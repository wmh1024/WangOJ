package cn.wmhwiki.wangojbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.wmhwiki.wangojbackend.dto.LoginDTO;
import cn.wmhwiki.wangojbackend.dto.PasswordUpdateDTO;
import cn.wmhwiki.wangojbackend.dto.UserDTO;
import cn.wmhwiki.wangojbackend.vo.PageVO;
import cn.wmhwiki.wangojbackend.vo.TokenVO;
import cn.wmhwiki.wangojbackend.vo.UserByAdminVO;
import cn.wmhwiki.wangojbackend.vo.UserVO;
import cn.wmhwiki.wangojbackend.domain.User;
import cn.wmhwiki.wangojbackend.exception.BusinessException;
import cn.wmhwiki.wangojbackend.exception.PasswordPolicy;
import cn.wmhwiki.wangojbackend.util.StringUtils;
import cn.wmhwiki.wangojbackend.interceptor.SessionStore;
import cn.wmhwiki.wangojbackend.mapper.UserMapper;
import cn.wmhwiki.wangojbackend.service.UserService;
import cn.wmhwiki.wangojbackend.util.PageUtils;
import cn.wmhwiki.wangojbackend.util.PasswordUtils;
import cn.wmhwiki.wangojbackend.util.UserHolder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author WangOJ contributors
 * @description 针对表【user】的数据库操作Service实现
 * @createDate 2024-03-16 21:20:51
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String DEFAULT_ROLE = "user";

    @Resource
    private UserMapper userMapper;

    @Resource
    private PasswordUtils passwordUtils;

    @Resource
    private PasswordPolicy passwordPolicy;

    @Resource
    private LoginAttemptGuard loginAttemptGuard;

    @Resource
    private SessionStore sessionStore;

    /**
     * 用户登录
     *
     * @param userDTO 用户的登录表单
     * @return Token
     */
    @Override
    public TokenVO userLogin(LoginDTO loginDTO) {
        // 1、参数校验：统一提示，避免账号枚举
        if (loginDTO == null) {
            throw new BusinessException("用户名或密码错误");
        }
        String userAccount = StrUtil.trimToEmpty(loginDTO.getUserAccount());
        String userPassword = loginDTO.getUserPassword();
        if (userAccount.isEmpty() || StrUtil.isBlank(userPassword)) {
            throw new BusinessException("用户名或密码错误");
        }
        if (userAccount.length() > PasswordPolicy.MAX_ACCOUNT_LENGTH
                || userPassword.length() > PasswordPolicy.MAX_PASSWORD_LENGTH) {
            throw new BusinessException("用户名或密码错误");
        }
        // 2、失败次数限流（按账号计数，Redis 异常时不阻断登录）
        if (loginAttemptGuard.isLocked(userAccount)) {
            long remaining = loginAttemptGuard.remainingSeconds(userAccount);
            throw new BusinessException(429, "登录失败次数过多，请 " + Math.max(remaining, 1) + " 秒后再试");
        }
        // 3、查询数据库
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("userAccount", userAccount));
        // 4、校验密码（兼容历史 MD5，校验成功后自动升级为 PBKDF2）
        if (user == null || !passwordUtils.matches(userPassword, user.getUserPassword())) {
            loginAttemptGuard.recordFailure(userAccount);
            throw new BusinessException("用户名或密码错误");
        }
        loginAttemptGuard.reset(userAccount);
        if (passwordUtils.needsUpgrade(user.getUserPassword())) {
            user.setUserPassword(passwordUtils.encode(userPassword));
        }
        // 刷新最近登录时间（顺手把可能升级过的哈希一起写回）
        user.setLastLoginTime(new Date());
        userMapper.updateById(user);

        return issueToken(user);
    }

    /**
     * 用户注册
     *
     * @param userDTO 注册信息
     * @return Token
     */
    @Override
    public TokenVO userRegister(UserDTO userDTO) {
        if (userDTO == null) {
            throw new BusinessException("用户名或密码错误");
        }
        String userAccount = StrUtil.trimToEmpty(userDTO.getUserAccount());
        String userPassword = userDTO.getUserPassword();
        passwordPolicy.validateAccount(userAccount);
        passwordPolicy.validatePassword(userPassword);
        // 查询账号是否已存在
        Long cnt = userMapper.selectCount(new QueryWrapper<User>().eq("userAccount", userAccount));
        if (cnt != null && cnt != 0) {
            throw new BusinessException("用户名已存在");
        }
        User user = User.builder()
                .userRole(DEFAULT_ROLE)
                .userAccount(userAccount)
                .userPassword(passwordUtils.encode(userPassword))
                .build();
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 唯一索引兜底：并发注册同一账号时不再抛 500
            throw new BusinessException("用户名已存在");
        }
        user = userMapper.selectById(user.getId());
        return issueToken(user);
    }

    /**
     * 用户注销：吊销当前 Token，并清掉该用户对应的索引。
     *
     * @param token Authorization 请求头（Bearer &lt;uuid&gt;）
     * @return OK
     */
    @Override
    public String userLogout(String token) {
        // 只允许注销当前请求自己的 Token：避免带别人的 Token 强制踢人下线
        String current = sessionStore.currentToken();
        if (current == null) {
            return "未登录";
        }
        String provided = StringUtils.resolveBearerToken(token);
        if (provided != null && !provided.equals(current)) {
            throw BusinessException.forbidden("只能注销当前登录的会话");
        }
        sessionStore.revoke(current);
        return "ok";
    }

    /**
     * 查询当前登录用户信息
     *
     * @return UserVO（每次请求由 TokenInterceptor 回源，保证角色/账号是最新的）
     */
    @Override
    public UserVO userStatus() {
        UserVO userVO = UserHolder.getUserVO();
        if (userVO == null) {
            throw BusinessException.unauthorized("请登录");
        }
        return userVO;
    }

    @Override
    public User getUserById(Long id) {
        if (id == null) {
            throw new BusinessException("用户ID不能为空");
        }
        User user = getById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        // 不返回密码哈希
        user.setUserPassword(null);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String delUserById(Long id) {
        if (id == null) {
            throw new BusinessException("用户ID不能为空");
        }
        if (getById(id) == null) {
            throw BusinessException.notFound("用户不存在");
        }
        removeById(id);
        // 删号后立即终止其所有会话
        sessionStore.revokeAllOfUser(id, null);
        return "操作成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateUser(Long id, PasswordUpdateDTO passwordUpdateDTO) {
        if (id == null) {
            throw new BusinessException("用户ID不能为空");
        }
        if (passwordUpdateDTO == null) {
            throw new BusinessException("密码不能为空");
        }
        passwordPolicy.validatePassword(passwordUpdateDTO.getUserPassword());
        if (getById(id) == null) {
            throw BusinessException.notFound("用户不存在");
        }
        User user = new User();
        user.setId(id);
        user.setUserPassword(passwordUtils.encode(passwordUpdateDTO.getUserPassword()));
        userMapper.updateById(user);
        // 改密后吊销该用户其它会话：管理员操作时保留发起者（若非本人则全部吊销）
        Long currentUserId = UserHolder.getUserId();
        String keepToken = id.equals(currentUserId) ? sessionStore.currentToken() : null;
        int revoked = sessionStore.revokeAllOfUser(id, keepToken);
        log.info("用户密码已重置, userId={}, 吊销会话数={}", id, revoked);
        return "操作成功";
    }

    @Override
    public PageVO<List<UserByAdminVO>> getUserList(Long current, Long pageSize) {
        int pageCurrent = PageUtils.page(current);
        int pageSizeValue = PageUtils.pageSize(pageSize);
        Page<User> page = new Page<>(pageCurrent, pageSizeValue);
        page = userMapper.selectPage(page, new QueryWrapper<User>().orderByDesc("id"));
        List<UserByAdminVO> userDTOList = new ArrayList<>();
        for (User user : page.getRecords()) {
            UserByAdminVO dto = new UserByAdminVO();
            dto.setId(user.getId());
            dto.setUserAccount(user.getUserAccount());
            dto.setCreatedTime(formatTime(user.getCreatedTime()));
            dto.setLastLoginTime(formatTime(user.getLastLoginTime()));
            dto.setUserRole(user.getUserRole());
            userDTOList.add(dto);
        }

        PageVO<List<UserByAdminVO>> pageVO = new PageVO<>();
        pageVO.setTotal(page.getTotal());
        pageVO.setSize(page.getSize());
        pageVO.setCurrent((long) pageCurrent);
        pageVO.setT(userDTOList);
        return pageVO;
    }

    /**
     * 签发 Token 并返回脱敏后的用户信息。
     */
    private TokenVO issueToken(User user) {
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        String token = sessionStore.issue(user.getId());
        return new TokenVO(token, userVO);
    }

    private static String formatTime(Date date) {
        if (date == null) {
            return "";
        }
        return TIME_FORMATTER.format(LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault()));
    }
}
