package com.github.jdami.aicommit.settings;

import com.github.jdami.aicommit.service.model.GenerationInputs;
import com.github.jdami.aicommit.service.provider.OllamaProviderClient;
import com.github.jdami.aicommit.service.provider.OpenAiProviderClient;
import com.github.jdami.aicommit.service.provider.OpenRouterProviderClient;
import com.github.jdami.aicommit.settings.AiSettingsState.Provider;
import com.github.jdami.aicommit.settings.model.ProviderSettings;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * UI component for Ollama settings
 */
public class AiSettingsComponent {

    private final JPanel mainPanel;
    private final JComboBox<Provider> providerCombo = new JComboBox<>(Provider.values());
    private final JPanel providerCards = new JPanel(new CardLayout());
    private final JBTextField ollamaEndpointField = new JBTextField();
    private final JBTextField ollamaModelField = new JBTextField();
    private final JBTextField openAiEndpointField = new JBTextField();
    private final JBTextField openAiModelField = new JBTextField();
    private final JBPasswordField openAiApiKeyField = new JBPasswordField();
    private final JBTextField openRouterEndpointField = new JBTextField();
    private final JBTextField openRouterModelField = new JBTextField();
    private final JBPasswordField openRouterApiKeyField = new JBPasswordField();
    private final JSpinner timeoutSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 300, 5));
    private final JTextArea systemPromptArea = new JTextArea(5, 40);

    private JBLabel createLabel(String text) {
        JBLabel label = new JBLabel(text);
        // Set a consistent width to ensure alignment across different FormBuilders
        // We must use the label's natural height, passing -1 is invalid for Dimension
        Dimension naturalSize = label.getPreferredSize();
        label.setPreferredSize(new Dimension(JBUI.scale(120), naturalSize.height));
        return label;
    }

    public AiSettingsComponent() {
        systemPromptArea.setLineWrap(true);
        systemPromptArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new com.intellij.ui.components.JBScrollPane(systemPromptArea);

        providerCards.add(buildOllamaPanel(), Provider.OLLAMA.name());
        providerCards.add(buildOpenAiPanel(), Provider.OPENAI.name());
        providerCards.add(buildOpenRouterPanel(), Provider.OPENROUTER.name());

        // Action Panel: Spinner + Test Button
        // We use a panel to hold them together
        JPanel timeoutAndTestPanel = new JPanel(new BorderLayout());
        JPanel spinnerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        spinnerPanel.add(timeoutSpinner);
        
        JButton testButton = new JButton("Test Connection");
        testButton.addActionListener(e -> testConnection(getProvider())); // Dynamically get current provider

        timeoutAndTestPanel.add(spinnerPanel, BorderLayout.WEST);
        timeoutAndTestPanel.add(testButton, BorderLayout.EAST);

        // Create release link panel
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel linkLabel = new JLabel("<html>插件发布地址: <a href='https://linux.do/t/topic/1415731/65'>LINUX.DO</a></html>");
        linkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        linkLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new java.net.URI("https://linux.do/t/topic/1415731/65"));
                } catch (Exception ex) {
                    Messages.showErrorDialog("无法打开链接: " + ex.getMessage(), "错误");
                }
            }
        });
        linkPanel.add(linkLabel);

        mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(createLabel("AI Provider: "), providerCombo, 1, false)
                .addVerticalGap(5)
                .addComponent(new com.intellij.ui.TitledSeparator("Provider Settings"))
                .addComponent(providerCards)
                .addLabeledComponent(createLabel("Timeout(s): "), timeoutAndTestPanel, 1, false)
                .addVerticalGap(5)
                .addComponent(new com.intellij.ui.TitledSeparator("Generation Parameters"))
                .addLabeledComponent(createLabel("System Prompt: "), scrollPane, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .addComponent(linkPanel)
                .getPanel();

        mainPanel.setBorder(JBUI.Borders.empty(10));

        providerCombo.addActionListener(e -> switchProviderCard());
    }

    private JPanel createApiKeyPanel(JBPasswordField apiKeyField) {
        // Fix Expansion: Set columns to limit preferred width
        apiKeyField.setColumns(30);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(apiKeyField, BorderLayout.CENTER);
        
        JCheckBox showPassword = new JCheckBox("Show");
        showPassword.addActionListener(e -> {
            char echoChar = showPassword.isSelected() ? 0 : '•';
            apiKeyField.setEchoChar(echoChar);
        });
        panel.add(showPassword, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildOllamaPanel() {
        JBLabel hintLabel = new JBLabel("提示: 如果 URL 以 # 结尾，将直接使用该地址作为完整请求 URL (不拼接 /api/generate)");
        hintLabel.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        hintLabel.setFont(JBUI.Fonts.smallFont());

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(createLabel("Endpoint URL: "), ollamaEndpointField, 1, false)
                .addComponentToRightColumn(hintLabel)
                .addLabeledComponent(createLabel("Model Name: "), ollamaModelField, 1, false)
                .getPanel();
    }

    private JPanel buildOpenAiPanel() {
        JBLabel hintLabel = new JBLabel("提示: 如果 URL 以 # 结尾，将直接使用该地址作为完整请求 URL (不拼接 /v1/chat/completions)");
        hintLabel.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        hintLabel.setFont(JBUI.Fonts.smallFont());

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(createLabel("Endpoint URL: "), openAiEndpointField, 1, false)
                .addComponentToRightColumn(hintLabel)
                .addLabeledComponent(createLabel("Model Name: "), openAiModelField, 1, false)
                .addLabeledComponent(createLabel("API Key: "), createApiKeyPanel(openAiApiKeyField), 1, false)
                .getPanel();
    }

    private JPanel buildOpenRouterPanel() {
        JBLabel hintLabel = new JBLabel("提示: 如果 URL 以 # 结尾，将直接使用该地址作为完整请求 URL (不拼接 /v1/chat/completions)");
        hintLabel.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        hintLabel.setFont(JBUI.Fonts.smallFont());

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(createLabel("Endpoint URL: "), openRouterEndpointField, 1, false)
                .addComponentToRightColumn(hintLabel)
                .addLabeledComponent(createLabel("Model Name: "), openRouterModelField, 1, false)
                .addLabeledComponent(createLabel("API Key: "), createApiKeyPanel(openRouterApiKeyField), 1, false)
                .getPanel();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public String getOllamaEndpoint() {
        return ollamaEndpointField.getText() != null ? ollamaEndpointField.getText() : "";
    }

    public Provider getProvider() {
        return (Provider) providerCombo.getSelectedItem();
    }

    public void setProvider(Provider provider) {
        providerCombo.setSelectedItem(provider);
        switchProviderCard();
    }

    public void setProviders(ProviderSettings providers) {
        if (providers == null) {
            return;
        }
        setOllamaEndpoint(providers.ollama != null ? providers.ollama.endpoint : "");
        setOllamaModel(providers.ollama != null ? providers.ollama.model : "");
        setOpenAiEndpoint(providers.openAi != null ? providers.openAi.endpoint : "");
        setOpenAiModel(providers.openAi != null ? providers.openAi.model : "");
        setOpenAiApiKey(providers.openAi != null ? providers.openAi.apiKey : "");
        setOpenRouterEndpoint(providers.openRouter != null ? providers.openRouter.endpoint : "");
        setOpenRouterModel(providers.openRouter != null ? providers.openRouter.model : "");
        setOpenRouterApiKey(providers.openRouter != null ? providers.openRouter.apiKey : "");
    }

    public void setOllamaEndpoint(String endpoint) {
        ollamaEndpointField.setText(endpoint != null ? endpoint : "");
    }

    public String getOllamaModel() {
        return ollamaModelField.getText() != null ? ollamaModelField.getText() : "";
    }

    public void setOllamaModel(String modelName) {
        ollamaModelField.setText(modelName != null ? modelName : "");
    }

    public int getTimeout() {
        return (Integer) timeoutSpinner.getValue();
    }

    public void setTimeout(int timeout) {
        timeoutSpinner.setValue(timeout);
    }

    public String getOpenAiEndpoint() {
        return openAiEndpointField.getText() != null ? openAiEndpointField.getText() : "";
    }

    public void setOpenAiEndpoint(String endpoint) {
        openAiEndpointField.setText(endpoint != null ? endpoint : "");
    }

    public String getOpenAiModel() {
        return openAiModelField.getText() != null ? openAiModelField.getText() : "";
    }

    public void setOpenAiModel(String model) {
        openAiModelField.setText(model != null ? model : "");
    }

    public String getOpenAiApiKey() {
        return openAiApiKeyField.getPassword() != null ? String.valueOf(openAiApiKeyField.getPassword()) : "";
    }

    public void setOpenAiApiKey(String apiKey) {
        openAiApiKeyField.setText(apiKey != null ? apiKey : "");
    }

    public String getSystemPrompt() {
        return systemPromptArea.getText() != null ? systemPromptArea.getText() : "";
    }

    public void setSystemPrompt(String prompt) {
        systemPromptArea.setText(prompt != null ? prompt : "");
    }

    public String getOpenRouterEndpoint() {
        return openRouterEndpointField.getText() != null ? openRouterEndpointField.getText() : "";
    }

    public void setOpenRouterEndpoint(String endpoint) {
        openRouterEndpointField.setText(endpoint != null ? endpoint : "");
    }

    public String getOpenRouterModel() {
        return openRouterModelField.getText() != null ? openRouterModelField.getText() : "";
    }

    public void setOpenRouterModel(String model) {
        openRouterModelField.setText(model != null ? model : "");
    }

    public String getOpenRouterApiKey() {
        return openRouterApiKeyField.getPassword() != null ? String.valueOf(openRouterApiKeyField.getPassword()) : "";
    }

    public void setOpenRouterApiKey(String apiKey) {
        openRouterApiKeyField.setText(apiKey != null ? apiKey : "");
    }

    private void switchProviderCard() {
        CardLayout layout = (CardLayout) providerCards.getLayout();
        Provider provider = getProvider() != null ? getProvider() : Provider.OLLAMA;
        layout.show(providerCards, provider.name());
    }

    private void testConnection(Provider provider) {
        // Get configuration based on provider
        final String endpoint;
        final String model;
        final String apiKey;
        
        switch (provider) {
            case OLLAMA:
                endpoint = getOllamaEndpoint().trim();
                model = getOllamaModel().trim();
                apiKey = "";
                break;
            case OPENAI:
                endpoint = getOpenAiEndpoint().trim();
                model = getOpenAiModel().trim();
                apiKey = getOpenAiApiKey().trim();
                break;
            case OPENROUTER:
                endpoint = getOpenRouterEndpoint().trim();
                model = getOpenRouterModel().trim();
                apiKey = getOpenRouterApiKey().trim();
                break;
            default:
                throw new IllegalStateException("Unknown provider: " + provider);
        }
        
        // Validate inputs
        if (endpoint.isEmpty()) {
            Messages.showErrorDialog("Endpoint cannot be empty", "Test Connection Failed");
            return;
        }
        if (model.isEmpty()) {
            Messages.showErrorDialog("Model cannot be empty", "Test Connection Failed");
            return;
        }
        if ((provider == Provider.OPENAI || provider == Provider.OPENROUTER) && apiKey.isEmpty()) {
            Messages.showErrorDialog("API Key cannot be empty", "Test Connection Failed");
            return;
        }
        
        // Test connection in background
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                // Create test inputs
                GenerationInputs inputs = new GenerationInputs(
                        "Test connection",
                        "You are a test assistant. Reply with 'OK' if you receive this message.",
                        endpoint,
                        model,
                        apiKey,
                        getTimeout()
                );
                
                // Select the appropriate client and test
                String response;
                switch (provider) {
                    case OLLAMA:
                        response = new OllamaProviderClient().generate(inputs, null);
                        break;
                    case OPENAI:
                        response = new OpenAiProviderClient().generate(inputs, null);
                        break;
                    case OPENROUTER:
                        response = new OpenRouterProviderClient().generate(inputs, null);
                        break;
                    default:
                        throw new IllegalStateException("Unknown provider: " + provider);
                }
                
                // Show success message
                String finalResponse = response;
                SwingUtilities.invokeLater(() -> 
                    Messages.showInfoMessage(
                        "Connection successful!\n\nProvider: " + provider + "\nModel: " + model + "\nResponse: " + finalResponse.substring(0, Math.min(100, finalResponse.length())) + "...",
                        "Test Connection Successful"
                    )
                );
                
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> 
                    Messages.showErrorDialog(
                        "Connection failed: " + ex.getMessage(),
                        "Test Connection Failed"
                    )
                );
            }
        }, "Testing Connection...", true, null);
    }
}
