package cn.wmhwiki.wangojbackend.service;

import cn.wmhwiki.wangojbackend.dto.CommitlogDTO;
import cn.wmhwiki.wangojbackend.pojo.RankItem;
import cn.wmhwiki.wangojbackend.vo.CommitlogVO;
import cn.wmhwiki.wangojbackend.vo.PageVO;
import cn.wmhwiki.wangojbackend.domain.Commitlog;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author WangOJ contributors
 * @description 针对表【commitLog】的数据库操作Service
 * @createDate 2024-03-19 18:46:59
 */
public interface CommitlogService extends IService<Commitlog> {

    String commitCode(CommitlogDTO commitlogDTO);

    /**
     * 分页查询提交记录。
     *
     * @param current   页码
     * @param pageSize  页大小
     * @param problemId 可选：按题目过滤
     * @param userId    可选：按用户过滤
     */
    PageVO<List<CommitlogVO>> getCommitVOList(Integer current, Integer pageSize, Long problemId, Long userId);

    CommitlogVO getCommitlogById(Long id);

    List<RankItem> getRankList();
}
