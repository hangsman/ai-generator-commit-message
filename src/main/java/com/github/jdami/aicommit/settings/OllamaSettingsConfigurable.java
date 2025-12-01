package com.github.jdami.aicommit.settings;

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
        
        String currentEndpoint = settingsComponent.getOllamaEndpoint();
        String currentModelName = settingsComponent.getModelName();
        int currentTimeout = settingsComponent.getTimeout();
        String currentSystemPrompt = settingsComponent.getSystemPrompt();
        
        return !currentEndpoint.equals(settings.ollamaEndpoint)
                || !currentModelName.equals(settings.modelName)
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
        String endpoint = settingsComponent.getOllamaEndpoint().trim();
        String modelName = settingsComponent.getModelName().trim();
        String systemPrompt = settingsComponent.getSystemPrompt().trim();
        
        if (endpoint.isEmpty()) {
            throw new ConfigurationException("Ollama endpoint cannot be empty");
        }
        if (modelName.isEmpty()) {
            throw new ConfigurationException("Model name cannot be empty");
        }
        if (systemPrompt.isEmpty()) {
            throw new ConfigurationException("System prompt cannot be empty");
        }
        
        // Apply settings
        settings.ollamaEndpoint = endpoint;
        settings.modelName = modelName;
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
        settingsComponent.setOllamaEndpoint(settings.ollamaEndpoint != null ? settings.ollamaEndpoint : "http://localhost:11434");
        settingsComponent.setModelName(settings.modelName != null ? settings.modelName : "qwen3:8b");
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
