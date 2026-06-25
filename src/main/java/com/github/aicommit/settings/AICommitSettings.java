package com.github.aicommit.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
    name = "com.github.aicommit.settings.AICommitSettings",
    storages = @Storage("ai_commit_settings.xml")
)
public class AICommitSettings implements PersistentStateComponent<AICommitSettings.State> {

    public static class State {
        public String provider = Provider.GEMINI.name();
        public String apiUrl = Provider.GEMINI.defaultUrl;
        public String modelName = Provider.GEMINI.defaultModel;
        public String promptTemplate = "Write a concise and professional Git commit message based on the provided diff.\n" +
                "Follow the conventional commits specification (e.g., feat: add login feature).\n" +
                "The commit message must be written in {language}.\n" +
                "Output ONLY the commit message itself, without any markdown formatting, backticks, quotes, or conversational filler.\n\n" +
                "Changes:\n" +
                "{diff}";
        public String language = "English";
    }

    public enum Provider {
        GEMINI("Gemini", "https://generativelanguage.googleapis.com", "gemini-1.5-flash"),
        OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini"),
        OLLAMA("Ollama (Local)", "http://localhost:11434", "llama3"),
        CUSTOM("Custom OpenAI Compatible", "", "");

        public final String displayName;
        public final String defaultUrl;
        public final String defaultModel;

        Provider(String displayName, String defaultUrl, String defaultModel) {
            this.displayName = displayName;
            this.defaultUrl = defaultUrl;
            this.defaultModel = defaultModel;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private State myState = new State();

    @Nullable
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.myState = state;
    }

    public static AICommitSettings getInstance() {
        return ApplicationManager.getApplication().getService(AICommitSettings.class);
    }
}
