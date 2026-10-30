package cn.wmhwiki.wangojbackend.controller.admin;

import cn.wmhwiki.wangojbackend.dto.ProblemDTO;
import cn.wmhwiki.wangojbackend.common.BaseResponse;
import cn.wmhwiki.wangojbackend.common.ResponseUtils;
import cn.wmhwiki.wangojbackend.service.ProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/problem")
@Tag(name = "题目管理控制器", description = "用于管理题目的控制器")
public class ProblemAdminController {
    @Resource
    private ProblemService problemService;

    @Operation(summary = "获取题目详情")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/{id}")
    public BaseResponse<ProblemDTO> getProblemVOById(@PathVariable Long id) {
        return ResponseUtils.success(problemService.getProblemByAdmin(id));
    }

    @Operation(summary = "添加题目")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/add")
    public BaseResponse<String> addProblem(@Valid @RequestBody ProblemDTO problemDTO) {
        return ResponseUtils.success(problemService.addProblem(problemDTO));
    }

    @Operation(summary = "删除题目")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @DeleteMapping("/del")
    public BaseResponse<Long> delProblem(Long id) {
        return ResponseUtils.success(problemService.delProblem(id));
    }

    @Operation(summary = "修改题目")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PutMapping("/change")
    public BaseResponse<Long> changeProblem(@Valid @RequestBody ProblemDTO problemDTO) {
        return ResponseUtils.success(problemService.changeProblem(problemDTO));
    }
}