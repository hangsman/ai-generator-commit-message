package com.github.jdami.aicommit.service;

import com.github.jdami.aicommit.service.model.GenerationInputs;
import com.github.jdami.aicommit.service.provider.OllamaProviderClient;
import com.github.jdami.aicommit.service.provider.OpenAiProviderClient;
import com.github.jdami.aicommit.service.provider.OpenRouterProviderClient;
import com.github.jdami.aicommit.service.util.PromptBuilder;
import com.github.jdami.aicommit.settings.AiSettingsState;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Facade to route commit message generation to the configured provider.
 */
public class AiService {

    private final AiProviderClient ollamaClient = new OllamaProviderClient();
    private final AiProviderClient openAiClient = new OpenAiProviderClient();
    private final AiProviderClient openRouterClient = new OpenRouterProviderClient();
    private volatile AiProviderClient activeClient;
    private final Object lock = new Object();

    /**
     * Generate commit message with diff content only (backward compatible).
     */
    public String generateCommitMessage(@NotNull String diffContent, @Nullable ProgressIndicator indicator) throws IOException {
        return generateCommitMessage(diffContent, null, indicator);
    }

    /**
     * Generate commit message with diff content and optional project context from CLAUDE.md.
     */
    public String generateCommitMessage(@NotNull String diffContent, @Nullable String projectContext, @Nullable ProgressIndicator indicator) throws IOException {
        AiSettingsState settings = AiSettingsState.getInstance();

        // Only include project context if enabled in settings
        String effectiveProjectContext = settings.includeClaudeMd ? projectContext : null;
        String prompt = PromptBuilder.buildPrompt(diffContent, effectiveProjectContext);
        GenerationInputs inputs = buildInputs(prompt, settings);
        AiProviderClient client = selectProvider(settings);

        synchronized (lock) {
            activeClient = client;
        }
        try {
            return client.generate(inputs, indicator);
        } finally {
            synchronized (lock) {
                activeClient = null;
            }
        }
    }

    public void cancelOngoingCall() {
        AiProviderClient client;
        synchronized (lock) {
            client = activeClient;
        }
        if (client != null) {
            client.cancel();
        }
    }

    private AiProviderClient selectProvider(AiSettingsState settings) {
        if (settings.provider == AiSettingsState.Provider.OPENAI) {
            return openAiClient;
        } else if (settings.provider == AiSettingsState.Provider.OPENROUTER) {
            return openRouterClient;
        }
        return ollamaClient;
    }

    private GenerationInputs buildInputs(String prompt, AiSettingsState settings) {
        if (settings.provider == AiSettingsState.Provider.OPENAI) {
            return new GenerationInputs(
                    prompt,
                    settings.systemPrompt,
                    settings.providers.openAi.endpoint,
                    settings.providers.openAi.model,
                    settings.providers.openAi.apiKey,
                    settings.timeout
            );
        } else if (settings.provider == AiSettingsState.Provider.OPENROUTER) {
            return new GenerationInputs(
                    prompt,
                    settings.systemPrompt,
                    settings.providers.openRouter.endpoint,
                    settings.providers.openRouter.model,
                    settings.providers.openRouter.apiKey,
                    settings.timeout
            );
        }

        return new GenerationInputs(
                prompt,
                settings.systemPrompt,
                settings.providers.ollama.endpoint,
                settings.providers.ollama.model,
                "",
                settings.timeout
        );
    }
}
