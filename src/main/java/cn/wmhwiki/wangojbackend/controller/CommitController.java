package cn.wmhwiki.wangojbackend.controller;

import cn.wmhwiki.wangojbackend.dto.CommitlogDTO;
import cn.wmhwiki.wangojbackend.vo.CommitlogVO;
import cn.wmhwiki.wangojbackend.vo.PageVO;
import cn.wmhwiki.wangojbackend.common.BaseResponse;
import cn.wmhwiki.wangojbackend.common.ResponseUtils;
import cn.wmhwiki.wangojbackend.service.CommitlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author WangOJ contributors
 */
@RestController
@RequestMapping("/api/commit")
@Tag(name = "提交记录控制器", description = "用于提交记录操作的控制器")
public class CommitController {
    @Resource
    private CommitlogService commitlogService;

    @Operation(summary = "提交代码", description = "提交请求体中提供的代码")
    @ApiResponse(responseCode = "200", description = "成功的响应")
    @PostMapping("/code")
    public BaseResponse<String> commitCode(@Valid @RequestBody CommitlogDTO commitlogDTO) {
        return ResponseUtils.success(commitlogService.commitCode(commitlogDTO));
    }

    @Operation(summary = "获取提交列表", description = "获取带分页的提交记录列表，可按题目或用户过滤")
    @ApiResponse(responseCode = "200", description = "成功的响应")
    @GetMapping("/list")
    public BaseResponse<PageVO<List<CommitlogVO>>> listCommits(
            @RequestParam Integer current,
            @RequestParam Integer pageSize,
            @RequestParam(required = false) Long problemId,
            @RequestParam(required = false) Long userId) {
        return ResponseUtils.success(commitlogService.getCommitVOList(current, pageSize, problemId, userId));
    }
}