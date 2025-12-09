package com.github.jdami.aicommit.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent state for Ollama settings
 */
@State(name = "com.github.jdami.aicommit.settings.OllamaSettingsState", storages = @Storage("OllamaSettings.xml"))
public class OllamaSettingsState implements PersistentStateComponent<OllamaSettingsState> {

    public enum Provider {
        OLLAMA,
        OPENAI
    }

    public Provider provider = Provider.OLLAMA;

    public String ollamaEndpoint = "http://localhost:11434";
    public String ollamaModel = "qwen3:8b";
    // Deprecated legacy field kept for migration compatibility
    public String modelName = "qwen3:8b";

    public String openAiEndpoint = "https://api.openai.com";
    public String openAiApiKey = "";
    public String openAiModel = "gpt-4o-mini";

    public int timeout = 30;
    public String systemPrompt = "CRITICAL: You are a commit message generator. You MUST output ONLY the commit message in the exact format below. NO explanations, NO analysis, NO extra text, NO markdown.\n\n"
            +
            "MANDATORY OUTPUT FORMAT:\n" +
            "type(scope): <SUMMARY OF ALL CHANGES>\n" +
            "\n" +
            "- detailed change point 1\n" +
            "- detailed change point 2\n" +
            "- detailed change point 3\n\n" +
            "ABSOLUTE RULES - VIOLATION WILL FAIL:\n" +
            "1. Output ONLY the commit message - NOTHING ELSE\n" +
            "2. The first line description MUST be a concise summary of ALL changes\n" +
            "3. NO explanations like 'Based on the git diff' or 'Here's the analysis'\n" +
            "4. NO markdown (```), NO code blocks, NO formatting\n" +
            "5. NO sentences like 'This message:', 'You can use this', etc.\n" +
            "6. Use Chinese for descriptions\n" +
            "7. type: feat, fix, docs, style, refactor, test, chore\n" +
            "8. Start immediately with the commit message\n\n" +
            "CORRECT OUTPUT:\n" +
            "fix(weather): 修复城市搜索接口日志输出错误\n" +
            "\n" +
            "- 修正了城市搜索接口返回结果的日志输出格式\n" +
            "- 移除了日志中的无效字符\n" +
            "- 确保日志能正确显示响应内容\n\n" +
            "FORBIDDEN OUTPUTS (NEVER DO THIS):\n" +
            "- 'Based on the git diff...'\n" +
            "- 'Here's the analysis...'\n" +
            "- 'This message:'\n" +
            "- Any explanation or commentary\n" +
            "- Markdown formatting\n\n" +
            "START YOUR RESPONSE IMMEDIATELY WITH: type(scope):";

    public String pluginVersion = "";

    public static OllamaSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(OllamaSettingsState.class);
    }

    @Nullable
    @Override
    public OllamaSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull OllamaSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
        if (this.ollamaModel == null || this.ollamaModel.isEmpty()) {
            this.ollamaModel = this.modelName;
        }
    }

    /**
     * Reset settings to default values
     */
    public void resetToDefaults() {
        this.provider = Provider.OLLAMA;
        this.ollamaEndpoint = "http://localhost:11434";
        this.ollamaModel = "qwen3:8b";
        this.modelName = "qwen3:8b";
        this.openAiEndpoint = "https://api.openai.com";
        this.openAiApiKey = "";
        this.openAiModel = "gpt-4o-mini";
        this.timeout = 30;
        this.systemPrompt = "CRITICAL: You are a commit message generator. You MUST output ONLY the commit message in the exact format below. NO explanations, NO analysis, NO extra text, NO markdown.\n\n"
                +
                "MANDATORY OUTPUT FORMAT:\n" +
                "type(scope): <SUMMARY OF ALL CHANGES>\n" +
                "\n" +
                "- detailed change point 1\n" +
                "- detailed change point 2\n" +
                "- detailed change point 3\n\n" +
                "ABSOLUTE RULES - VIOLATION WILL FAIL:\n" +
                "1. Output ONLY the commit message - NOTHING ELSE\n" +
                "2. The first line description MUST be a concise summary of ALL changes\n" +
                "3. NO explanations like 'Based on the git diff' or 'Here's the analysis'\n" +
                "4. NO markdown (```), NO code blocks, NO formatting\n" +
                "5. NO sentences like 'This message:', 'You can use this', etc.\n" +
                "6. Use Chinese for descriptions\n" +
                "7. type: feat, fix, docs, style, refactor, test, chore\n" +
                "8. Start immediately with the commit message\n\n" +
                "CORRECT OUTPUT:\n" +
                "fix(weather): 修复城市搜索接口日志输出错误\n" +
                "\n" +
                "- 修正了城市搜索接口返回结果的日志输出格式\n" +
                "- 移除了日志中的无效字符\n" +
                "- 确保日志能正确显示响应内容\n\n" +
                "FORBIDDEN OUTPUTS (NEVER DO THIS):\n" +
                "- 'Based on the git diff...'\n" +
                "- 'Here's the analysis...'\n" +
                "- 'This message:'\n" +
                "- Any explanation or commentary\n" +
                "- Markdown formatting\n\n" +
                "START YOUR RESPONSE IMMEDIATELY WITH: type(scope):";
    }
}
