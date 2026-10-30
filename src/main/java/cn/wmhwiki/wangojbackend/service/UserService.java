package cn.wmhwiki.wangojbackend.service;

import cn.wmhwiki.wangojbackend.dto.LoginDTO;
import cn.wmhwiki.wangojbackend.dto.PasswordUpdateDTO;
import cn.wmhwiki.wangojbackend.dto.UserDTO;
import cn.wmhwiki.wangojbackend.vo.PageVO;
import cn.wmhwiki.wangojbackend.vo.TokenVO;
import cn.wmhwiki.wangojbackend.vo.UserByAdminVO;
import cn.wmhwiki.wangojbackend.vo.UserVO;
import cn.wmhwiki.wangojbackend.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author WangOJ contributors
 * @description 针对表【user】的数据库操作Service
 * @createDate 2024-03-16 21:20:51
 */
public interface UserService extends IService<User> {

    TokenVO userLogin(LoginDTO loginDTO);

    TokenVO userRegister(UserDTO userDTO);

    String userLogout(String token);

    UserVO userStatus();

    User getUserById(Long id);

    String delUserById(Long id);

    String updateUser(Long id, PasswordUpdateDTO passwordUpdateDTO);

    PageVO<List<UserByAdminVO>> getUserList(Long current, Long pageSize);
}
