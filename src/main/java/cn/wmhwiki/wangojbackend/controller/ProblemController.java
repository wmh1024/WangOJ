package cn.wmhwiki.wangojbackend.controller;

import cn.wmhwiki.wangojbackend.dto.query.ProblemQuery;
import cn.wmhwiki.wangojbackend.vo.PageVO;
import cn.wmhwiki.wangojbackend.vo.ProblemVO;
import cn.wmhwiki.wangojbackend.common.BaseResponse;
import cn.wmhwiki.wangojbackend.common.ResponseUtils;
import cn.wmhwiki.wangojbackend.service.ProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/problem")
@Tag(name = "题目控制器", description = "用于题目的控制器")
@Slf4j
public class ProblemController {
    @Resource
    private ProblemService problemService;

    @Operation(summary = "获取问题列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/list")
    public BaseResponse<PageVO<List<ProblemVO>>> getProblemDTOList(
            @Parameter(description = "当前页数") @RequestParam Integer current,
            @Parameter(description = "每页数量") @RequestParam Integer pageSize,
            @RequestBody ProblemQuery problemQuery
    ) {
        return ResponseUtils.success(problemService.getProblemDTOList(current, pageSize, problemQuery));
    }

    @Operation(summary = "根据ID获取问题内容")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/content")
    public BaseResponse<ProblemVO> getProblemContentById(
            @Parameter(description = "问题ID") Long id
    ) {
        return ResponseUtils.success(problemService.getProblemContent(id));
    }

    @GetMapping("/hot")
    public BaseResponse<List<ProblemVO>> getHotProblemList() {
        return ResponseUtils.success(problemService.getHotProblemList());
    }
}