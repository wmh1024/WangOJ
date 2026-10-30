package cn.wmhwiki.wangojbackend.service;

public interface AIService {
    String codeExplain(String code, Long problemId, String aiPrompt);
}
