package com.github.jdami.aicommit.settings;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import javax.swing.*;

/**
 * UI component for Ollama settings
 */
public class OllamaSettingsComponent {

    private final JPanel mainPanel;
    private final JBTextField ollamaEndpointField = new JBTextField();
    private final JBTextField modelNameField = new JBTextField();
    private final JSpinner timeoutSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 300, 5));
    private final JTextArea systemPromptArea = new JTextArea(5, 40);

    public OllamaSettingsComponent() {
        systemPromptArea.setLineWrap(true);
        systemPromptArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(systemPromptArea);

        mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Ollama Endpoint: "), ollamaEndpointField, 1, false)
                .addLabeledComponent(new JBLabel("Model Name: "), modelNameField, 1, false)
                .addLabeledComponent(new JBLabel("Timeout (seconds): "), timeoutSpinner, 1, false)
                .addLabeledComponent(new JBLabel("System Prompt: "), scrollPane, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        mainPanel.setBorder(JBUI.Borders.empty(10));
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public String getOllamaEndpoint() {
        return ollamaEndpointField.getText() != null ? ollamaEndpointField.getText() : "";
    }

    public void setOllamaEndpoint(String endpoint) {
        ollamaEndpointField.setText(endpoint != null ? endpoint : "");
    }

    public String getModelName() {
        return modelNameField.getText() != null ? modelNameField.getText() : "";
    }

    public void setModelName(String modelName) {
        modelNameField.setText(modelName != null ? modelName : "");
    }

    public int getTimeout() {
        return (Integer) timeoutSpinner.getValue();
    }

    public void setTimeout(int timeout) {
        timeoutSpinner.setValue(timeout);
    }

    public String getSystemPrompt() {
        return systemPromptArea.getText() != null ? systemPromptArea.getText() : "";
    }

    public void setSystemPrompt(String prompt) {
        systemPromptArea.setText(prompt != null ? prompt : "");
    }
}
