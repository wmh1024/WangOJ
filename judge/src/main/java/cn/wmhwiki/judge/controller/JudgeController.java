/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.controller;

import cn.wmhwiki.judge.constant.Constants;
import cn.wmhwiki.judge.exception.JudgeException;
import cn.wmhwiki.judge.pojo.dto.JudgeRequest;
import cn.wmhwiki.judge.pojo.vo.JudgeResponse;
import cn.wmhwiki.judge.service.JudgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 判题协议 v1.0 入口。
 */
@RestController
public class JudgeController {

    @Autowired
    private JudgeService judgeService;

    /**
     * 健康检查，不需要鉴权。
     */
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "success");
        body.put("message", "PONG");
        body.put("protocolVersion", Constants.protocolVersion);
        return body;
    }

    /**
     * 判题 / 仅运行代码。
     */
    @PostMapping(value = "/api/v1/judge", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public JudgeResponse judge(@RequestBody JudgeRequest request) {
        if (request == null) {
            throw new JudgeException(400, null, "请求体不能为空");
        }
        return judgeService.judge(request);
    }
}
