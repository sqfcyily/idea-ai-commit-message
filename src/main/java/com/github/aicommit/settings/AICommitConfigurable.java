package com.github.aicommit.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class AICommitConfigurable implements Configurable {

    private JPanel mainPanel;
    private JComboBox<AICommitSettings.Provider> providerComboBox;
    private JBTextField apiUrlField;
    private JPasswordField apiKeyField;
    private JBTextField modelNameField;
    private JComboBox<String> languageComboBox;
    private JBTextArea promptTextArea;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "AI Commit Message";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;

        // Provider
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        mainPanel.add(new JBLabel("LLM Provider:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        providerComboBox = new JComboBox<>(AICommitSettings.Provider.values());
        mainPanel.add(providerComboBox, gbc);

        // API URL
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        mainPanel.add(new JBLabel("API Base URL:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        apiUrlField = new JBTextField();
        mainPanel.add(apiUrlField, gbc);

        // API Key
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        mainPanel.add(new JBLabel("API Key:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        apiKeyField = new JPasswordField();
        mainPanel.add(apiKeyField, gbc);

        // Model Name
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0;
        mainPanel.add(new JBLabel("Model Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        modelNameField = new JBTextField();
        mainPanel.add(modelNameField, gbc);

        // Language
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0;
        mainPanel.add(new JBLabel("Language:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        languageComboBox = new JComboBox<>(new String[]{"English", "Chinese", "Japanese", "Spanish", "German", "French"});
        mainPanel.add(languageComboBox, gbc);

        // Prompt
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.0;
        mainPanel.add(new JBLabel("Prompt Template:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        promptTextArea = new JBTextArea(8, 40);
        promptTextArea.setLineWrap(true);
        promptTextArea.setWrapStyleWord(true);
        mainPanel.add(new JScrollPane(promptTextArea), gbc);

        // Spacer to push everything to the top
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; gbc.weighty = 1.0;
        mainPanel.add(new JPanel(), gbc);

        // Add action listener to combobox to set default values
        providerComboBox.addActionListener(e -> {
            AICommitSettings.Provider selected = (AICommitSettings.Provider) providerComboBox.getSelectedItem();
            if (selected != null && selected != AICommitSettings.Provider.CUSTOM) {
                apiUrlField.setText(selected.defaultUrl);
                modelNameField.setText(selected.defaultModel);
            }
        });

        return mainPanel;
    }

    @Override
    public boolean isModified() {
        AICommitSettings settings = AICommitSettings.getInstance();
        AICommitSettings.State state = settings.getState();
        if (state == null) return false;

        boolean providerModified = !state.provider.equals(((AICommitSettings.Provider) providerComboBox.getSelectedItem()).name());
        boolean apiUrlModified = !state.apiUrl.equals(apiUrlField.getText());
        boolean apiKeyModified = !new String(apiKeyField.getPassword()).equals(AICommitPasswordSafe.getApiKey() != null ? AICommitPasswordSafe.getApiKey() : "");
        boolean modelNameModified = !state.modelName.equals(modelNameField.getText());
        boolean languageModified = !state.language.equals(languageComboBox.getSelectedItem());
        boolean promptModified = !state.promptTemplate.equals(promptTextArea.getText());

        return providerModified || apiUrlModified || apiKeyModified || modelNameModified || languageModified || promptModified;
    }

    @Override
    public void apply() throws ConfigurationException {
        AICommitSettings settings = AICommitSettings.getInstance();
        AICommitSettings.State state = settings.getState();
        if (state == null) return;

        state.provider = ((AICommitSettings.Provider) providerComboBox.getSelectedItem()).name();
        state.apiUrl = apiUrlField.getText();
        state.modelName = modelNameField.getText();
        state.language = (String) languageComboBox.getSelectedItem();
        state.promptTemplate = promptTextArea.getText();

        AICommitPasswordSafe.setApiKey(new String(apiKeyField.getPassword()));
    }

    @Override
    public void reset() {
        AICommitSettings settings = AICommitSettings.getInstance();
        AICommitSettings.State state = settings.getState();
        if (state == null) return;

        providerComboBox.setSelectedItem(AICommitSettings.Provider.valueOf(state.provider));
        apiUrlField.setText(state.apiUrl);
        apiKeyField.setText(AICommitPasswordSafe.getApiKey() != null ? AICommitPasswordSafe.getApiKey() : "");
        modelNameField.setText(state.modelName);
        languageComboBox.setSelectedItem(state.language);
        promptTextArea.setText(state.promptTemplate);
    }
}
