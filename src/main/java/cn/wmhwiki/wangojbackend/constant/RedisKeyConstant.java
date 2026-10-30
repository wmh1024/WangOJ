package cn.wmhwiki.wangojbackend.constant;

/**
 * Redis key 前缀。
 *
 * <p>约定：{@code wangoj:<类型>:<标识>}。</p>
 *
 * @author WangOJ contributors
 */
public final class RedisKeyConstant {

    private static final String PROJECT_PREFIX = "wangoj:";

    /**
     * 登录 Token：{@code wangoj:token:<uuid>} -> userId（只存 ID，用户信息每次回源）
     */
    public static final String TOKEN = PROJECT_PREFIX + "token:";

    /**
     * 某个用户当前有效的 Token 集合：{@code wangoj:user_tokens:<userId>}，用于改密/降权/删号时批量吊销
     */
    public static final String USER_TOKENS = PROJECT_PREFIX + "user_tokens:";

    /**
     * 题目信息缓存：{@code wangoj:problem_content:<problemId>}
     */
    public static final String PROBLEM_CONTENT = PROJECT_PREFIX + "problem_content:";

    /**
     * 排行榜缓存
     */
    public static final String RANK_LIST = PROJECT_PREFIX + "rank";

    /**
     * 登录失败计数（按账号）
     */
    public static final String LOGIN_FAIL = PROJECT_PREFIX + "login_fail:";

    /**
     * AI 接口频率限制（按用户）
     */
    public static final String AI_RATE_LIMIT = PROJECT_PREFIX + "ai_rate:";

    private RedisKeyConstant() {
    }
}
