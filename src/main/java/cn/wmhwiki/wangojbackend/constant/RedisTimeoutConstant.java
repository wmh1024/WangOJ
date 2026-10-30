package cn.wmhwiki.wangojbackend.constant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Redis 缓存过期时间（分钟）。
 *
 * <p>通过配置项注入，便于按部署规模调整；默认值与历史版本保持一致。</p>
 */
@Component
public class RedisTimeoutConstant {

    /**
     * 登录 Token 有效期（分钟），每次请求滑动续期
     */
    private final long token;

    /**
     * 题目详情缓存（小时）
     */
    private final long problemContent;

    /**
     * 排行榜缓存（分钟）
     */
    private final long rankList;

    public RedisTimeoutConstant(@Value("${app.token-timeout-minutes:10}") long token,
                                @Value("${app.problem-cache-hours:12}") long problemContent,
                                @Value("${app.rank-cache-minutes:5}") long rankList) {
        this.token = token;
        this.problemContent = problemContent;
        this.rankList = rankList;
    }

    public long getToken() {
        return token;
    }

    public long getProblemContent() {
        return problemContent;
    }

    public long getRankList() {
        return rankList;
    }
}
