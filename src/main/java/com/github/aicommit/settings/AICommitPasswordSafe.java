package com.github.aicommit.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;

public class AICommitPasswordSafe {
    private static final String SUBSYSTEM_NAME = "AICommitMessage";
    private static final CredentialAttributes credentialAttributes = new CredentialAttributes(
        "AICommitMessageApiKey"
    );

    public static String getApiKey() {
        return PasswordSafe.getInstance().getPassword(credentialAttributes);
    }

    public static void setApiKey(String apiKey) {
        PasswordSafe.getInstance().setPassword(credentialAttributes, apiKey);
    }
}
