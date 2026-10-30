package cn.wmhwiki.wangojbackend.controller;

import cn.wmhwiki.wangojbackend.vo.CommitlogVO;
import cn.wmhwiki.wangojbackend.common.BaseResponse;
import cn.wmhwiki.wangojbackend.common.ResponseUtils;
import cn.wmhwiki.wangojbackend.service.CommitlogService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/log")
public class CommitlogController {
    @Resource
    private CommitlogService commitlogService;

    @GetMapping("/{id}")
    public BaseResponse<CommitlogVO> getCommitlogById(@PathVariable Long id) {
        return ResponseUtils.success(commitlogService.getCommitlogById(id));
    }
}
