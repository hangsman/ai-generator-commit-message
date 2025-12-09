package com.github.jdami.aicommit.settings;

import com.github.jdami.aicommit.settings.OllamaSettingsState.Provider;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable for Ollama settings UI
 */
public class OllamaSettingsConfigurable implements Configurable {

    private OllamaSettingsComponent settingsComponent;

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "AI Commit Message Generator";
    }

    @Override
    public @Nullable JComponent createComponent() {
        settingsComponent = new OllamaSettingsComponent();
        // Load current settings into UI
        reset();
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        if (settingsComponent == null) {
            return false;
        }
        
        OllamaSettingsState settings = OllamaSettingsState.getInstance();
        
        Provider currentProvider = settingsComponent.getProvider();
        String currentEndpoint = settingsComponent.getOllamaEndpoint();
        String currentOllamaModel = settingsComponent.getOllamaModel();
        String currentOpenAiEndpoint = settingsComponent.getOpenAiEndpoint();
        String currentOpenAiModel = settingsComponent.getOpenAiModel();
        String currentOpenAiApiKey = settingsComponent.getOpenAiApiKey();
        int currentTimeout = settingsComponent.getTimeout();
        String currentSystemPrompt = settingsComponent.getSystemPrompt();
        
        return currentProvider != settings.provider
                || !currentEndpoint.equals(settings.ollamaEndpoint)
                || !currentOllamaModel.equals(settings.ollamaModel)
                || !currentOpenAiEndpoint.equals(settings.openAiEndpoint)
                || !currentOpenAiModel.equals(settings.openAiModel)
                || !currentOpenAiApiKey.equals(settings.openAiApiKey)
                || currentTimeout != settings.timeout
                || !currentSystemPrompt.equals(settings.systemPrompt);
    }

    @Override
    public void apply() throws ConfigurationException {
        if (settingsComponent == null) {
            return;
        }
        
        OllamaSettingsState settings = OllamaSettingsState.getInstance();
        
        // Validate inputs
        Provider provider = settingsComponent.getProvider();
        String endpoint = settingsComponent.getOllamaEndpoint().trim();
        String ollamaModel = settingsComponent.getOllamaModel().trim();
        String systemPrompt = settingsComponent.getSystemPrompt().trim();
        String openAiEndpoint = settingsComponent.getOpenAiEndpoint().trim();
        String openAiModel = settingsComponent.getOpenAiModel().trim();
        String openAiApiKey = settingsComponent.getOpenAiApiKey().trim();
        
        if (systemPrompt.isEmpty()) {
            throw new ConfigurationException("System prompt cannot be empty");
        }
        if (provider == Provider.OLLAMA) {
            if (endpoint.isEmpty()) {
                throw new ConfigurationException("Ollama endpoint cannot be empty");
            }
            if (ollamaModel.isEmpty()) {
                throw new ConfigurationException("Ollama model cannot be empty");
            }
        } else if (provider == Provider.OPENAI) {
            if (openAiEndpoint.isEmpty()) {
                throw new ConfigurationException("OpenAI API base cannot be empty");
            }
            if (openAiModel.isEmpty()) {
                throw new ConfigurationException("OpenAI model cannot be empty");
            }
            if (openAiApiKey.isEmpty()) {
                throw new ConfigurationException("OpenAI API key cannot be empty");
            }
        }
        
        // Apply settings
        settings.provider = provider != null ? provider : Provider.OLLAMA;
        settings.ollamaEndpoint = endpoint;
        settings.ollamaModel = ollamaModel;
        settings.modelName = ollamaModel; // legacy field update
        settings.openAiEndpoint = openAiEndpoint;
        settings.openAiModel = openAiModel;
        settings.openAiApiKey = openAiApiKey;
        settings.timeout = settingsComponent.getTimeout();
        settings.systemPrompt = systemPrompt;
        
        // Force state persistence
        settings.loadState(settings);
    }

    @Override
    public void reset() {
        if (settingsComponent == null) {
            return;
        }
        
        OllamaSettingsState settings = OllamaSettingsState.getInstance();
        settingsComponent.setProvider(settings.provider != null ? settings.provider : Provider.OLLAMA);
        settingsComponent.setOllamaEndpoint(settings.ollamaEndpoint != null ? settings.ollamaEndpoint : "http://localhost:11434");
        settingsComponent.setOllamaModel(settings.ollamaModel != null ? settings.ollamaModel : "qwen3:8b");
        settingsComponent.setOpenAiEndpoint(settings.openAiEndpoint != null ? settings.openAiEndpoint : "https://api.openai.com");
        settingsComponent.setOpenAiModel(settings.openAiModel != null ? settings.openAiModel : "gpt-4o-mini");
        settingsComponent.setOpenAiApiKey(settings.openAiApiKey != null ? settings.openAiApiKey : "");
        settingsComponent.setTimeout(settings.timeout);
        settingsComponent.setSystemPrompt(settings.systemPrompt != null ? settings.systemPrompt : getDefaultSystemPrompt());
    }
    
    private String getDefaultSystemPrompt() {
        // Create a new instance to get the default value
        OllamaSettingsState defaultState = new OllamaSettingsState();
        return defaultState.systemPrompt;
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}
