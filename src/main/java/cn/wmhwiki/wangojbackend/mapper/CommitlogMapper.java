package cn.wmhwiki.wangojbackend.mapper;

import cn.wmhwiki.wangojbackend.pojo.RankItem;
import cn.wmhwiki.wangojbackend.domain.Commitlog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author WangOJ contributors
 * @description 针对表【commitLog】的数据库操作Mapper
 * @createDate 2024-03-19 18:46:59
 * @Entity cn.wmhwiki.wangojbackend.domain.Commitlog
 */
public interface CommitlogMapper extends BaseMapper<Commitlog> {
    /**
     * 排行榜：ac = 通过的去重题目数，ans = 通过次数。
     *
     * <p>两者都只统计 {@code status = 'Accept'} 的提交，因此 "ans" 表示"AC 次数"而非"提交次数"。</p>
     */
    @Select("SELECT u.id as id, u.userAccount as name, "
            + "COUNT(DISTINCT c.problemId) as ac, COUNT(c.id) as ans "
            + "FROM commitLog c "
            + "JOIN user u ON c.userId = u.id "
            + "WHERE c.status = 'Accept' "
            + "AND c.isDeleted = 0 "
            + "AND u.isDeleted = 0 "
            + "GROUP BY u.id, u.userAccount "
            + "ORDER BY ac DESC, ans ASC, u.id ASC "
            + "LIMIT 10")
    List<RankItem> getRankList();
}




