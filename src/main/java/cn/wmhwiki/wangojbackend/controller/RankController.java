package cn.wmhwiki.wangojbackend.controller;

import cn.wmhwiki.wangojbackend.pojo.RankItem;
import cn.wmhwiki.wangojbackend.common.BaseResponse;
import cn.wmhwiki.wangojbackend.common.ResponseUtils;
import cn.wmhwiki.wangojbackend.service.CommitlogService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 排行榜
 *
 * @author WangOJ contributors
 */
@RestController
@RequestMapping("/api/rank")
public class RankController {

    @Resource
    private CommitlogService commitlogService;

    @GetMapping({"", "/"})
    public BaseResponse<List<RankItem>> getRankList() {
        return ResponseUtils.success(commitlogService.getRankList());
    }
}
