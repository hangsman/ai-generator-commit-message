package com.github.jdami.aicommit.actions;

import com.github.jdami.aicommit.service.AiService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.ui.Refreshable;
import com.intellij.vcsUtil.VcsUtil;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Action to generate commit message using Ollama AI
 */
public class GenerateCommitMessageAction extends AnAction {

    private AiService aiService;
    private volatile boolean isGenerating;
    private volatile boolean wasCancelled;
    private volatile ProgressIndicator currentIndicator;
    private final Object stateLock = new Object();

    private AiService getAiService() {
        if (aiService == null) {
            aiService = new AiService();
        }
        return aiService;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        if (isGenerating) {
            cancelGeneration();
            return;
        }

        // Get the commit panel
        Refreshable data = Refreshable.PANEL_KEY.getData(e.getDataContext());
        if (!(data instanceof CheckinProjectPanel)) {
            Messages.showErrorDialog(project, "Unable to access commit panel", "Error");
            return;
        }

        CheckinProjectPanel checkinPanel = (CheckinProjectPanel) data;
        CommitMessageI commitMessageI = (CommitMessageI) checkinPanel;

        // Get the changes to be committed
        Collection<Change> changes = checkinPanel.getSelectedChanges();
        
        // Also get unversioned files (new files not yet added to git)
        List<FilePath> unversionedFiles = checkinPanel.getVirtualFiles().stream()
            .filter(vf -> {
                // Check if this file is not already in the changes collection
                String vfPath = vf.getPath();
                return changes.stream().noneMatch(c -> 
                    (c.getAfterRevision() != null && c.getAfterRevision().getFile().getPath().equals(vfPath)) ||
                    (c.getBeforeRevision() != null && c.getBeforeRevision().getFile().getPath().equals(vfPath))
                );
            })
            .map(VcsUtil::getFilePath)
            .collect(Collectors.toList());
        
        if (changes.isEmpty() && unversionedFiles.isEmpty()) {
            Messages.showWarningDialog(project, "No changes selected for commit", "Warning");
            return;
        }

        // Generate diff content in background
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating commit message...", true) {
            private String generatedMessage;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                markGenerating(indicator);
                try {
                    indicator.setText("Analyzing changes...");
                    indicator.setIndeterminate(false);
                    indicator.setFraction(0.3);
                    indicator.checkCanceled();

                    // Get diff content
                    String diffContent = getDiffContent(project, changes, unversionedFiles);

                    if (diffContent == null || diffContent.trim().isEmpty()) {
                        ApplicationManager.getApplication().invokeLater(
                                () -> Messages.showWarningDialog(project, "No diff content found", "Warning"));
                        return;
                    }

                    indicator.setText("Calling Ai service...");
                    indicator.setFraction(0.6);
                    indicator.checkCanceled();

                    // Call Ai service
                    generatedMessage = getAiService().generateCommitMessage(diffContent, indicator);

                    indicator.setFraction(1.0);

                } catch (ProcessCanceledException canceled) {
                    wasCancelled = true;
                    System.out.println("Commit message generation canceled by user.");
                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(project,
                            "Failed to generate commit message: " + ex.getMessage(),
                            "Error"));
                }
            }

            @Override
            public void onSuccess() {
                if (generatedMessage != null && !generatedMessage.isEmpty() && !wasCancelled) {
                    ApplicationManager.getApplication()
                            .invokeLater(() -> commitMessageI.setCommitMessage(generatedMessage));
                }
            }

            @Override
            public void onCancel() {
                wasCancelled = true;
                System.out.println("Commit message generation canceled.");
            }

            @Override
            public void onFinished() {
                markIdle();
            }
        });
    }

    /**
     * Normalize path for git diff format (always use forward slashes)
     * This ensures Windows paths work correctly in git diffs
     */
    private String normalizePathForGit(String path) {
        // Replace backslashes with forward slashes for Windows compatibility
        return path.replace('\\', '/');
    }

    /**
     * Get diff content from changes using git diff command
     */
    private String getDiffContent(Project project, Collection<Change> changes, List<FilePath> unversionedFiles) {
        try {
            GitRepository repository = GitUtil.getRepositoryManager(project).getRepositories().stream()
                    .findFirst()
                    .orElse(null);

            if (repository == null) {
                System.err.println("No Git repository found");
                return null;
            }

            String repoPath = repository.getRoot().getPath();
            StringBuilder diffBuilder = new StringBuilder();

            // Use git diff to get actual diff content
            for (Change change : changes) {
                String absolutePath = null;
                boolean isNewFile = change.getBeforeRevision() == null;
                boolean isDeletedFile = change.getAfterRevision() == null;

                if (change.getAfterRevision() != null) {
                    absolutePath = change.getAfterRevision().getFile().getPath();
                } else if (change.getBeforeRevision() != null) {
                    absolutePath = change.getBeforeRevision().getFile().getPath();
                }

                if (absolutePath != null) {
                    // Convert to relative path
                    String relativePath = absolutePath;
                    if (absolutePath.startsWith(repoPath)) {
                        relativePath = absolutePath.substring(repoPath.length());
                        if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
                            relativePath = relativePath.substring(1);
                        }
                    }
                    
                    // Normalize path for git (convert backslashes to forward slashes for Windows)
                    relativePath = normalizePathForGit(relativePath);

                    String changeType = isNewFile ? "NEW" : (isDeletedFile ? "DELETED" : "MODIFIED");
                    System.out.println("Processing file: " + relativePath + " [" + changeType + "]");

                    try {
                        String fileDiff = null;

                        if (isNewFile) {
                            // For new files, try different strategies
                            // Try 1: Staged new file
                            fileDiff = executeGitDiff(repoPath, "diff", "--cached", "HEAD", "--", relativePath);
                            
                            // Try 2: If not staged, check if it's added to index
                            if (fileDiff.isEmpty()) {
                                fileDiff = executeGitDiff(repoPath, "diff", "--cached", "--", relativePath);
                            }
                            
                            // Try 3: For completely new untracked files, show the content as added
                            if (fileDiff.isEmpty()) {
                                // Check if file exists and show it as new content
                                String showContent = executeGitDiff(repoPath, "show", ":" + relativePath);
                                if (!showContent.isEmpty()) {
                                    // Split on both Unix and Windows line endings
                                    String[] showLines = showContent.split("\\r?\\n");
                                    // Format as diff
                                    fileDiff = "diff --git a/" + relativePath + " b/" + relativePath + "\n" +
                                              "new file mode 100644\n" +
                                              "--- /dev/null\n" +
                                              "+++ b/" + relativePath + "\n" +
                                              "@@ -0,0 +1," + showLines.length + " @@\n";
                                    for (String line : showLines) {
                                        fileDiff += "+" + line + "\n";
                                    }
                                }
                            }
                        } else if (isDeletedFile) {
                            // For deleted files
                            fileDiff = executeGitDiff(repoPath, "diff", "--cached", "--", relativePath);
                            
                            if (fileDiff.isEmpty()) {
                                fileDiff = executeGitDiff(repoPath, "diff", "HEAD", "--", relativePath);
                            }
                        } else {
                            // For modified files
                            // Try 1: Staged changes (git diff --cached)
                            fileDiff = executeGitDiff(repoPath, "diff", "--cached", "--", relativePath);

                            // Try 2: Unstaged changes (git diff)
                            if (fileDiff.isEmpty()) {
                                fileDiff = executeGitDiff(repoPath, "diff", "--", relativePath);
                            }

                            // Try 3: Compare with HEAD
                            if (fileDiff.isEmpty()) {
                                fileDiff = executeGitDiff(repoPath, "diff", "HEAD", "--", relativePath);
                            }
                        }

                        if (!fileDiff.isEmpty()) {
                            diffBuilder.append(fileDiff);
                            System.out.println("Successfully got diff for: " + relativePath + " (" + fileDiff.length() + " chars)");
                        } else {
                            System.err.println("No diff found for file: " + relativePath + " [" + changeType + "]");
                        }
                    } catch (Exception e) {
                        System.err.println("Error getting diff for file " + relativePath + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

            // Process unversioned files (new files not yet added to git)
            for (FilePath filePath : unversionedFiles) {
                try {
                    String absolutePath = filePath.getPath();
                    String relativePath = absolutePath;
                    
                    if (absolutePath.startsWith(repoPath)) {
                        relativePath = absolutePath.substring(repoPath.length());
                        if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
                            relativePath = relativePath.substring(1);
                        }
                    }
                    
                    // Normalize path for git (convert backslashes to forward slashes for Windows)
                    relativePath = normalizePathForGit(relativePath);
                    
                    System.out.println("Processing unversioned file: " + relativePath);
                    
                    // Read file content directly from filesystem
                    File file = new File(absolutePath);
                    if (file.exists() && file.isFile()) {
                        // Read with explicit UTF-8 encoding for cross-platform compatibility
                        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                        // Split on both Unix (\n) and Windows (\r\n) line endings
                        String[] lines = content.split("\\r?\\n", -1); // -1 to preserve trailing empty strings
                        
                        // Format as diff (all lines are additions)
                        StringBuilder fileDiff = new StringBuilder();
                        fileDiff.append("diff --git a/").append(relativePath).append(" b/").append(relativePath).append("\n");
                        fileDiff.append("new file mode 100644\n");
                        fileDiff.append("--- /dev/null\n");
                        fileDiff.append("+++ b/").append(relativePath).append("\n");
                        fileDiff.append("@@ -0,0 +1,").append(lines.length).append(" @@\n");
                        
                        for (String line : lines) {
                            fileDiff.append("+").append(line).append("\n");
                        }
                        
                        diffBuilder.append(fileDiff);
                        System.out.println("Successfully got diff for unversioned file: " + relativePath + " (" + fileDiff.length() + " chars)");
                    } else {
                        System.err.println("Unversioned file not found or not a file: " + absolutePath);
                    }
                } catch (Exception e) {
                    System.err.println("Error reading unversioned file " + filePath.getPath() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            String finalDiff = diffBuilder.toString();
            System.out.println("=== Git Diff Content ===");
            System.out.println("Diff length: " + finalDiff.length() + " characters");
            System.out.println(finalDiff);
            System.out.println("========================");

            return finalDiff.isEmpty() ? null : finalDiff;
        } catch (Exception e) {
            System.err.println("Error getting diff content: " + e.getMessage());
            e.printStackTrace();
            return "Unable to get diff content: " + e.getMessage();
        }
    }

    /**
     * Execute git diff command and return output
     */
    private String executeGitDiff(String repoPath, String... args) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(args);
        processBuilder.directory(new java.io.File(repoPath));
        processBuilder.redirectErrorStream(true);

        // Prepend "git" to the command
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add("git");
        command.addAll(java.util.Arrays.asList(args));
        processBuilder.command(command);

        System.out.println("Executing: " + String.join(" ", command));

        Process process = processBuilder.start();
        // Read git command output with explicit UTF-8 encoding for cross-platform compatibility
        java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        System.out.println("Exit code: " + exitCode + ", Output length: " + output.length());

        if (exitCode != 0) {
            System.err.println("Git command failed: " + output.toString());
            return "";
        }

        return output.toString();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
        e.getPresentation().setIcon(IconLoader.getIcon(
                isGenerating ? "/icons/aiCommitStop.svg" : "/icons/aiCommit.svg",
                GenerateCommitMessageAction.class));
        e.getPresentation().setText(isGenerating ? "停止生成" : "Git助手");
        e.getPresentation().setDescription(isGenerating ? "停止生成 commit message" : "Generate commit message using AI");
    }

    private void markGenerating(ProgressIndicator indicator) {
        synchronized (stateLock) {
            isGenerating = true;
            wasCancelled = false;
            currentIndicator = indicator;
        }
    }

    private void markIdle() {
        synchronized (stateLock) {
            isGenerating = false;
            currentIndicator = null;
        }
        getAiService().cancelOngoingCall();
    }

    private void cancelGeneration() {
        synchronized (stateLock) {
            if (!isGenerating) {
                return;
            }
            if (currentIndicator != null) {
                currentIndicator.cancel();
            }
            wasCancelled = true;
        }
        getAiService().cancelOngoingCall();
    }
}
