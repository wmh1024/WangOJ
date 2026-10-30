package cn.wmhwiki.wangojbackend.util;

import cn.wmhwiki.wangojbackend.constant.JudgeStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * 输出比较工具。
 *
 * <p>规则：</p>
 * <ul>
 *   <li>统一 CRLF / CR 为 LF；</li>
 *   <li>去掉每行行尾空白；</li>
 *   <li>去掉末尾多余空行；</li>
 *   <li>默认区分大小写、保留行首空白；</li>
 *   <li>按测试点逐点比较，不忽略顺序和重复次数。</li>
 * </ul>
 *
 * @author WangOJ contributors
 */
public final class JudgeOutputUtils {

    private static final String ACCEPTED = "ACCEPTED";

    private JudgeOutputUtils() {
    }

    /**
     * 规范化输出，用于比较。
     */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String unified = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = unified.split("\n", -1);
        List<String> normalized = new ArrayList<>(lines.length);
        for (String line : lines) {
            normalized.add(stripTrailingWhitespace(line));
        }
        // 去掉末尾空行
        int end = normalized.size();
        while (end > 0 && normalized.get(end - 1).isEmpty()) {
            end--;
        }
        return String.join("\n", normalized.subList(0, end));
    }

    /**
     * 判断实际输出是否与标准输出一致。
     */
    public static boolean isAccepted(String expectedOutput, String actualOutput) {
        return normalize(expectedOutput).equals(normalize(actualOutput));
    }

    /**
     * 根据沙箱返回的单点状态和输出，给出该测试点的最终 verdict。
     *
     * @param sandboxVerdict 沙箱 verdict，可为空
     * @param expected       标准输出
     * @param actual         实际输出
     * @param timeMs         耗时（沙箱未返回时为 null，视为未知，不参与超时判定）
     * @param memoryKb       内存（沙箱未返回时为 null，视为未知，不参与超限判定）
     * @param timeLimitMs    时间限制
     * @param memoryLimitKb  内存限制
     */
    public static String resolveCaseVerdict(String sandboxVerdict,
                                            String expected,
                                            String actual,
                                            Long timeMs,
                                            Long memoryKb,
                                            long timeLimitMs,
                                            long memoryLimitKb) {
        if (ACCEPTED.equals(sandboxVerdict)) {
            // 沙箱声称执行成功但缺少耗时/内存：无法判定，按系统错误处理，避免"未知即通过"
            if (timeMs == null || memoryKb == null) {
                return JudgeStatus.CASE_SYSTEM_ERROR;
            }
        }
        if (JudgeStatus.CASE_COMPILE_ERROR.equals(sandboxVerdict)) {
            return JudgeStatus.CASE_COMPILE_ERROR;
        }
        if (JudgeStatus.CASE_TIME_LIMIT_EXCEEDED.equals(sandboxVerdict)) {
            return JudgeStatus.CASE_TIME_LIMIT_EXCEEDED;
        }
        if (JudgeStatus.CASE_MEMORY_LIMIT_EXCEEDED.equals(sandboxVerdict)) {
            return JudgeStatus.CASE_MEMORY_LIMIT_EXCEEDED;
        }
        if (JudgeStatus.CASE_RUNTIME_ERROR.equals(sandboxVerdict)
                || JudgeStatus.CASE_SYSTEM_ERROR.equals(sandboxVerdict)) {
            return sandboxVerdict;
        }
        if (timeMs != null && timeMs > timeLimitMs) {
            return JudgeStatus.CASE_TIME_LIMIT_EXCEEDED;
        }
        if (memoryKb != null && memoryLimitKb > 0 && memoryKb > memoryLimitKb) {
            return JudgeStatus.CASE_MEMORY_LIMIT_EXCEEDED;
        }
        if (isAccepted(expected, actual)) {
            return JudgeStatus.CASE_ACCEPTED;
        }
        return JudgeStatus.CASE_WRONG_ANSWER;
    }

    /**
     * 将单点 verdict 汇总为提交状态。优先级：系统错误 > 编译错误 > 超时 > 内存 > 运行时 > WA > AC。
     */
    public static String aggregateVerdict(List<String> caseVerdicts) {
        if (caseVerdicts == null || caseVerdicts.isEmpty()) {
            return JudgeStatus.SYSTEM_ERROR;
        }
        String result = JudgeStatus.ACCEPT;
        boolean sawVerdict = false;
        for (String verdict : caseVerdicts) {
            if (verdict == null) {
                continue;
            }
            sawVerdict = true;
            switch (verdict) {
                case JudgeStatus.CASE_SYSTEM_ERROR:
                    return JudgeStatus.SYSTEM_ERROR;
                case JudgeStatus.CASE_COMPILE_ERROR:
                    return JudgeStatus.COMPILE_ERROR;
                case JudgeStatus.CASE_TIME_LIMIT_EXCEEDED:
                    result = JudgeStatus.TIME_LIMIT_EXCEEDED;
                    break;
                case JudgeStatus.CASE_MEMORY_LIMIT_EXCEEDED:
                    if (!JudgeStatus.TIME_LIMIT_EXCEEDED.equals(result)) {
                        result = JudgeStatus.MEMORY_LIMIT_EXCEEDED;
                    }
                    break;
                case JudgeStatus.CASE_RUNTIME_ERROR:
                    if (JudgeStatus.ACCEPT.equals(result)) {
                        result = JudgeStatus.RUNTIME_ERROR;
                    }
                    break;
                case JudgeStatus.CASE_WRONG_ANSWER:
                    if (JudgeStatus.ACCEPT.equals(result)) {
                        result = JudgeStatus.WRONG_ANSWER;
                    }
                    break;
                case JudgeStatus.CASE_ACCEPTED:
                    break;
                default:
                    // 未知状态不能被当作 AC
                    return JudgeStatus.SYSTEM_ERROR;
            }
        }
        return sawVerdict ? result : JudgeStatus.SYSTEM_ERROR;
    }

    private static String stripTrailingWhitespace(String line) {
        int end = line.length();
        while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) {
            end--;
        }
        return line.substring(0, end);
    }
}
