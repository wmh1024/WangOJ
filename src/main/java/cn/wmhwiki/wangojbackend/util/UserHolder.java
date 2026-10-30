package cn.wmhwiki.wangojbackend.util;

import cn.hutool.core.bean.BeanUtil;
import cn.wmhwiki.wangojbackend.vo.UserVO;
import cn.wmhwiki.wangojbackend.domain.User;

/**
 * 当前登录用户容器（基于 ThreadLocal）。
 *
 * <p>用户信息由 {@code TokenInterceptor} 在每次请求时<b>回源数据库</b>后写入，
 * 因此管理员降权、改密、删号会立即生效，不会因为 Redis 中的旧副本而残留权限。</p>
 */
public final class UserHolder {

    private static final ThreadLocal<UserVO> TL = new ThreadLocal<>();

    private UserHolder() {
    }

    public static void saveUserVO(UserVO userVO) {
        TL.set(userVO);
    }

    public static UserVO getUserVO() {
        return TL.get();
    }

    /**
     * 当前登录用户 ID，未登录返回 null。
     */
    public static Long getUserId() {
        UserVO userVO = TL.get();
        return userVO == null ? null : userVO.getId();
    }

    public static void removeUserVO() {
        TL.remove();
    }

    /**
     * 领域对象转脱敏 VO（不含密码哈希）。
     */
    public static UserVO toUserVO(User user) {
        if (user == null) {
            return null;
        }
        return BeanUtil.copyProperties(user, UserVO.class);
    }
}
