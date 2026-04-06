package com.github.jdami.aicommit.service.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility to build prompts for commit message generation.
 */
public final class PromptBuilder {

    private PromptBuilder() {
    }

    /**
     * Build prompt with diff content only (backward compatible).
     */
    public static String buildPrompt(@NotNull String diffContent) {
        return buildPrompt(diffContent, null);
    }

    /**
     * Build prompt with diff content and optional project context from CLAUDE.md.
     *
     * @param diffContent     The git diff content
     * @param projectContext  Optional project context from CLAUDE.md (can be null)
     * @return The complete prompt for AI
     */
    public static String buildPrompt(@NotNull String diffContent, @Nullable String projectContext) {
        // First try to compress by removing context lines if oversized
        String compressedDiff = DiffCompressor.compress(diffContent);
        
        // Then apply truncation if still too large (to prevent AI model 400 errors)
        String processedDiff = DiffTruncator.truncate(compressedDiff);
        
        // Log truncation statistics
        if (processedDiff.length() != diffContent.length()) {
            System.out.println("=== Diff Truncation Applied ===");
            System.out.println("Original: " + DiffTruncator.getStats(diffContent));
            System.out.println("Truncated to: " + DiffTruncator.getStats(processedDiff));
            System.out.println("================================");
        }

        // Build project context section if available
        String projectContextSection = "";
        if (projectContext != null && !projectContext.isEmpty()) {
            projectContextSection = "## 项目上下文（来自 CLAUDE.md）\n\n" +
                    "以下是项目的上下文信息，请参考这些信息来更好地理解项目结构和约定：\n\n" +
                    "```\n" + projectContext + "\n```\n\n" +
                    "---\n\n";
        }

        return "请分析以下代码变更（Diff）并生成专业的 commit message。\n\n" +
                projectContextSection +
                "## 分析步骤\n\n" +
                "1. **识别变更类型**\n" +
                "   - 检查是否有新增文件（new file mode）→ 可能是 feat\n" +
                "   - 检查是否修复了问题或错误 → 可能是 fix\n" +
                "   - 检查是否重构了代码结构 → 可能是 refactor\n" +
                "   - 检查是否优化了性能 → 可能是 perf\n" +
                "   - 根据系统指令中的规则选择最合适的类型\n\n" +
                "2. **提取核心逻辑**\n" +
                "   - 识别主要改动的文件和方法\n" +
                "   - 理解改动的业务目的\n" +
                "   - 总结关键的逻辑变化\n\n" +
                "3. **确定 scope**\n" +
                "   - 基于主要改动的模块/组件/文件确定 scope\n" +
                "   - 保持简短精确（如：api, service, config, ui 等）\n\n" +
                "4. **生成 commit message**\n" +
                "   - 第一行：type(scope): 简明总结\n" +
                "   - 空一行\n" +
                "   - 详细变更点：2-5 个要点，每行一个，使用 `-` 开头\n\n" +
                "---\n\n" +
                "## 代码变更内容\n\n" +
                "```diff\n" +
                processedDiff + "\n" +
                "```\n\n" +
                "---\n\n" +
                "现在请直接输出 commit message（不要任何解释或前缀）：";
    }
}

