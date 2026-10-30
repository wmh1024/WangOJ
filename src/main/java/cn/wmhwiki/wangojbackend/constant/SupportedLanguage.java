package cn.wmhwiki.wangojbackend.constant;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 判题支持的语言白名单。
 *
 * <p>前端、后端与沙箱必须使用同一组标识；后端提前校验可以避免未知语言
 * 被沙箱拒绝后落成 System Error。</p>
 */
public final class SupportedLanguage {

    private static final Set<String> LANGUAGES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("c", "c++", "java", "python", "javascript", "golang")));

    private SupportedLanguage() {
    }

    /**
     * 统一别名并转小写；空白返回空字符串。
     */
    public static String normalize(String lang) {
        if (StrUtil.isBlank(lang)) {
            return "";
        }
        String normalized = lang.trim().toLowerCase();
        switch (normalized) {
            case "cpp":
                return "c++";
            case "js":
                return "javascript";
            case "go":
                return "golang";
            default:
                return normalized;
        }
    }

    public static boolean isSupported(String lang) {
        return LANGUAGES.contains(normalize(lang));
    }
}
