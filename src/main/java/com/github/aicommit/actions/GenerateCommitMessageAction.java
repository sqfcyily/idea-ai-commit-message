package com.github.aicommit.actions;

import com.github.aicommit.service.LLMService;
import com.github.aicommit.settings.AICommitPasswordSafe;
import com.github.aicommit.settings.AICommitSettings;
import com.intellij.diff.comparison.ComparisonManager;
import com.intellij.diff.comparison.ComparisonPolicy;
import com.intellij.diff.fragments.LineFragment;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.vcs.commit.CommitWorkflowUi;
import com.intellij.openapi.vcs.CommitMessageI;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GenerateCommitMessageAction extends AnAction {

    private static volatile boolean isGenerating = false;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        if (isGenerating) return;

        AICommitSettings settings = AICommitSettings.getInstance();
        AICommitSettings.State state = settings.getState();
        if (state == null) return;

        String apiKey = AICommitPasswordSafe.getApiKey();
        AICommitSettings.Provider provider = AICommitSettings.Provider.valueOf(state.provider);

        if ((apiKey == null || apiKey.trim().isEmpty()) && provider != AICommitSettings.Provider.OLLAMA) {
            Messages.showErrorDialog(
                    project,
                    "API Key is not configured. Please configure it in Settings -> Tools -> AI Commit Message.",
                    "AI Commit Message"
            );
            return;
        }

        CommitMessageI commitMessageControl = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);
        CommitWorkflowUi commitWorkflowUi = e.getData(VcsDataKeys.COMMIT_WORKFLOW_UI);

        if (commitMessageControl == null) {
            Messages.showWarningDialog(
                    project,
                    "Cannot find commit message editor. Please make sure the Commit tool window is open.",
                    "AI Commit Message"
            );
            return;
        }

        List<Change> changes = new ArrayList<>();
        if (commitWorkflowUi != null) {
            changes.addAll(commitWorkflowUi.getIncludedChanges());
        } else if (e.getData(VcsDataKeys.CHANGES) != null) {
            changes.addAll(Arrays.asList(e.getData(VcsDataKeys.CHANGES)));
        } else if (e.getData(VcsDataKeys.SELECTED_CHANGES) != null) {
            changes.addAll(Arrays.asList(e.getData(VcsDataKeys.SELECTED_CHANGES)));
        }

        if (changes.isEmpty()) {
            Messages.showInfoMessage(
                    project,
                    "No changes selected for commit. Please check/select the files you want to commit.",
                    "AI Commit Message"
            );
            return;
        }

        isGenerating = true;
        com.intellij.ide.ActivityTracker.getInstance().inc(); // Instantly update toolbar state

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating commit message...", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("Retrieving changes diff...");
                    String diffText = buildDiffText(changes, indicator);

                    if (diffText.trim().isEmpty()) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            Messages.showWarningDialog(
                                    project,
                                    "No text changes found in the selected files.",
                                    "AI Commit Message"
                             );
                        });
                        return;
                    }

                    indicator.setText("Requesting AI response...");
                    String generatedMessage = LLMService.generateCommitMessage(diffText);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        commitMessageControl.setCommitMessage(generatedMessage);
                    });
                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(
                                project,
                                "Failed to generate commit message:\n" + ex.getMessage(),
                                "AI Commit Message Error"
                        );
                    });
                }
            }

            @Override
            public void onFinished() {
                isGenerating = false;
                com.intellij.ide.ActivityTracker.getInstance().inc(); // Refresh toolbar back to normal
            }
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        CommitMessageI commitMessageControl = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);

        boolean visible = project != null && commitMessageControl != null;
        e.getPresentation().setVisible(visible);
        e.getPresentation().setEnabled(visible && !isGenerating);

        if (isGenerating) {
            e.getPresentation().setText("Generating Commit Message...");
            e.getPresentation().setIcon(com.intellij.icons.AllIcons.Process.Step_1);
        } else {
            e.getPresentation().setText("Generate Commit Message with AI");
            e.getPresentation().setIcon(com.intellij.icons.AllIcons.Actions.Lightning);
        }
    }

    private String buildDiffText(List<Change> changes, ProgressIndicator indicator) throws Exception {
        StringBuilder diffBuilder = new StringBuilder();

        for (Change change : changes) {
            if (indicator.isCanceled()) break;

            ContentRevision beforeRevision = change.getBeforeRevision();
            ContentRevision afterRevision = change.getAfterRevision();

            boolean isBinary = (beforeRevision != null && beforeRevision.getFile().getFileType().isBinary()) ||
                               (afterRevision != null && afterRevision.getFile().getFileType().isBinary());
            if (isBinary) continue;

            String filePath = beforeRevision != null ? beforeRevision.getFile().getPath() :
                    (afterRevision != null ? afterRevision.getFile().getPath() : "unknown");
            diffBuilder.append("File: ").append(filePath).append("\n");

            if (beforeRevision == null && afterRevision != null) {
                diffBuilder.append("Type: ADDED\n");
                String content = afterRevision.getContent();
                if (content == null) content = "";
                diffBuilder.append("Content:\n").append(takeChars(content, 1000)).append("\n");
            } else if (beforeRevision != null && afterRevision == null) {
                diffBuilder.append("Type: DELETED\n");
                String content = beforeRevision.getContent();
                if (content == null) content = "";
                diffBuilder.append("Content:\n").append(takeChars(content, 1000)).append("\n");
            } else if (beforeRevision != null && afterRevision != null) {
                diffBuilder.append("Type: MODIFIED\n");
                String before = beforeRevision.getContent();
                String after = afterRevision.getContent();
                if (before == null) before = "";
                if (after == null) after = "";
                String lineDiff = getUnifiedDiff(before, after);
                diffBuilder.append("Diff:\n").append(takeChars(lineDiff, 2000)).append("\n");
            }
            diffBuilder.append("\n---\n");
        }
        return diffBuilder.toString();
    }

    private String takeChars(String text, int max) {
        if (text.length() <= max) return text;
        return text.substring(0, max);
    }

    private String getUnifiedDiff(String before, String after) {
        List<String> lines1 = Arrays.asList(before.split("\n"));
        List<String> lines2 = Arrays.asList(after.split("\n"));

        List<LineFragment> fragments = ComparisonManager.getInstance().compareLines(
                before, after, ComparisonPolicy.DEFAULT, new EmptyProgressIndicator()
        );

        StringBuilder diff = new StringBuilder();
        int lastLine1 = 0;

        for (LineFragment fragment : fragments) {
            int startLine1 = fragment.getStartLine1();
            int endLine1 = fragment.getEndLine1();
            int startLine2 = fragment.getStartLine2();
            int endLine2 = fragment.getEndLine2();

            int contextStart = Math.max(lastLine1, startLine1 - 2);
            for (int i = contextStart; i < startLine1; i++) {
                if (i < lines1.size()) {
                    diff.append("  ").append(lines1.get(i)).append("\n");
                }
            }

            for (int i = startLine1; i < endLine1; i++) {
                if (i < lines1.size()) {
                    diff.append("- ").append(lines1.get(i)).append("\n");
                }
            }

            for (int i = startLine2; i < endLine2; i++) {
                if (i < lines2.size()) {
                    diff.append("+ ").append(lines2.get(i)).append("\n");
                }
            }

            lastLine1 = endLine1;
        }

        int contextEnd = Math.min(lines1.size(), lastLine1 + 2);
        for (int i = lastLine1; i < contextEnd; i++) {
            diff.append("  ").append(lines1.get(i)).append("\n");
        }

        return diff.toString();
    }
}
