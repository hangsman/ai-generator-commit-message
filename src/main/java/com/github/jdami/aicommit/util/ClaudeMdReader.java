package com.github.jdami.aicommit.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Utility to read CLAUDE.md file from project root directory.
 * CLAUDE.md contains project-specific instructions and context for AI.
 */
public final class ClaudeMdReader {

    private static final String CLAUDE_MD_FILENAME = "CLAUDE.md";

    private ClaudeMdReader() {
    }

    /**
     * Read CLAUDE.md content from project root directory.
     *
     * @param project The current project
     * @return The content of CLAUDE.md, or null if not found or empty
     */
    @Nullable
    public static String readClaudeMd(@NotNull Project project) {
        VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
        if (projectDir == null) {
            return null;
        }

        VirtualFile claudeMdFile = projectDir.findChild(CLAUDE_MD_FILENAME);
        if (claudeMdFile == null || !claudeMdFile.exists()) {
            return null;
        }

        try {
            String content = new String(claudeMdFile.contentsToByteArray(), StandardCharsets.UTF_8);
            if (content == null || content.trim().isEmpty()) {
                return null;
            }
            return content.trim();
        } catch (IOException e) {
            System.err.println("Error reading CLAUDE.md: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if CLAUDE.md exists in project root.
     *
     * @param project The current project
     * @return true if CLAUDE.md exists
     */
    public static boolean exists(@NotNull Project project) {
        VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
        if (projectDir == null) {
            return false;
        }
        VirtualFile claudeMdFile = projectDir.findChild(CLAUDE_MD_FILENAME);
        return claudeMdFile != null && claudeMdFile.exists();
    }
}
