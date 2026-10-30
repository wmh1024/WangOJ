/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.pojo.lang;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class GoContext extends LangContext {
    // 编程语言
    private final String lang = "golang";

    // 保存文件名
    private final String sourceFileName = "main.go";

    // 编程语言对应的docker镜像
    private final String image = "golang:1.22-alpine";

    // 编程语言对应执行的编译命令
    private final String[] compileCmd = {
            "go", "build", "-o", "main", LangContext.SOURCE_PLACEHOLDER
    };

    // 编程语言对应执行的启动命令
    private final String[] runCmd = {"./main"};
}

