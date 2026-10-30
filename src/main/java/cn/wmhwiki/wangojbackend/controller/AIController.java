package cn.wmhwiki.wangojbackend.controller;

import cn.wmhwiki.wangojbackend.dto.AiCodeDTO;
import cn.wmhwiki.wangojbackend.vo.UserVO;
import cn.wmhwiki.wangojbackend.common.BaseResponse;
import cn.wmhwiki.wangojbackend.common.ResponseUtils;
import cn.wmhwiki.wangojbackend.exception.BusinessException;
import cn.wmhwiki.wangojbackend.service.AIService;
import cn.wmhwiki.wangojbackend.service.impl.RateLimiter;
import cn.wmhwiki.wangojbackend.util.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 相关接口。
 *
 * <p>需要登录，并按用户限流，避免被刷爆第三方额度。</p>
 */
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI 控制器", description = "用于AI操作的控制器")
public class AIController {

    @Resource
    private AIService aiService;

    @Resource
    private RateLimiter rateLimiter;

    @Operation(summary = "解释代码", description = "解释提供的代码")
    @ApiResponse(responseCode = "200", description = "成功的响应")
    @PostMapping("/code")
    public BaseResponse<String> codeExplain(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "AI代码请求体")
            @Valid @RequestBody AiCodeDTO aiCodeDTO) {
        UserVO userVO = UserHolder.getUserVO();
        if (userVO == null) {
            throw new BusinessException("请先登录");
        }
        if (aiCodeDTO == null) {
            throw new BusinessException("请求体不能为空");
        }
        if (!rateLimiter.allowAiCall(userVO.getId())) {
            throw new BusinessException("调用过于频繁，请稍后再试");
        }
        return ResponseUtils.success(
                aiService.codeExplain(aiCodeDTO.getCode(), aiCodeDTO.getProblemId(), aiCodeDTO.getAiPrompt()));
    }
}
