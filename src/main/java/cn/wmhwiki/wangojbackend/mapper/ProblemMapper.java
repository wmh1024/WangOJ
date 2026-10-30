package cn.wmhwiki.wangojbackend.mapper;

import cn.wmhwiki.wangojbackend.domain.Problem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * @author WangOJ contributors
 * @description 针对表【problem】的数据库操作Mapper
 * @createDate 2024-03-17 17:54:01
 * @Entity cn.wmhwiki.wangojbackend.domain.Problem
 */
public interface ProblemMapper extends BaseMapper<Problem> {
    @Update("UPDATE problem SET commitNum = commitNum + 1 WHERE id = #{id} AND isDeleted = 0")
    void plusCommitNum(@Param("id") Long id);

    @Update("UPDATE problem SET acceptNum = acceptNum + 1, commitNum = commitNum + 1 "
            + "WHERE id = #{id} AND isDeleted = 0")
    void plusAcceptNum(@Param("id") Long id);
}




