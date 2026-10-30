/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.constant.enums;

import cn.wmhwiki.judge.pojo.lang.CContext;
import cn.wmhwiki.judge.pojo.lang.CppContext;
import cn.wmhwiki.judge.pojo.lang.GoContext;
import cn.wmhwiki.judge.pojo.lang.JavaContext;
import cn.wmhwiki.judge.pojo.lang.JsContext;
import cn.wmhwiki.judge.pojo.lang.LangContext;
import cn.wmhwiki.judge.pojo.lang.PythonContext;

public enum LangContextEnum {

    Java("java", new JavaContext()),
    Cpp("c++", new CppContext()),
    Python("python", new PythonContext()),
    Js("javascript", new JsContext()),
    Go("golang", new GoContext()),
    C("c", new CContext());

    private String lang;
    private LangContext langContext;

    LangContextEnum(String lang, LangContext langContext) {
        this.lang = lang;
        this.langContext = langContext;
    }

    public static LangContextEnum getByLang(String lang) {
        for (LangContextEnum item : LangContextEnum.values()) {
            if (item.lang.equals(lang)) {
                return item;
            }
        }
        return null;
    }

    public LangContext getLangContext() {
        return langContext;
    }
}
