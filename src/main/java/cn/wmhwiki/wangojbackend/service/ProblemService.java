package cn.wmhwiki.wangojbackend.service;

import cn.wmhwiki.wangojbackend.dto.ProblemDTO;
import cn.wmhwiki.wangojbackend.dto.query.ProblemQuery;
import cn.wmhwiki.wangojbackend.vo.PageVO;
import cn.wmhwiki.wangojbackend.vo.ProblemVO;
import cn.wmhwiki.wangojbackend.domain.Problem;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author WangOJ contributors
 * @description 针对表【problem】的数据库操作Service
 * @createDate 2024-03-17 17:54:01
 */
public interface ProblemService extends IService<Problem> {

    PageVO<List<ProblemVO>> getProblemDTOList(Integer current, Integer pageSize, ProblemQuery problemQuery);

    ProblemVO getProblemContent(Long id);

    String addProblem(ProblemDTO problemDTO);

    Long delProblem(Long id);

    Long changeProblem(ProblemDTO problemDTO);

    ProblemDTO getProblemByAdmin(Long id);

    List<ProblemVO> getHotProblemList();
}
