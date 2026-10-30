/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 协议 v1.0 中的单个测试点。
 *
 * <p>沙箱只负责执行、不负责判定答案。后端自 v1.0 起<b>不再下发</b>
 * {@code expectedOutput}；该字段仅为兼容早期客户端而保留，沙箱读取后立即忽略。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgeTestCase {

    // 标准输入
    private String input;

    /**
     * 已废弃：后端不再发送，沙箱也不使用。
     *
     * <p>保留字段只为让旧版客户端请求仍能反序列化成功（否则 Gson 会忽略未知字段，
     * 实际也不会报错，这里显式保留是为了文档与兼容性意图清晰）。</p>
     */
    @Deprecated
    private String expectedOutput;
}
