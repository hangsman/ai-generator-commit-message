package com.github.jdami.aicommit.util;

import com.intellij.openapi.diff.impl.patch.FilePatch;
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder;
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.annotations.NotNull;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for generating unified diff format from VCS changes.
 * Uses IntelliJ's native Patch API for standard and efficient diff generation.
 */
public class UnifiedDiffGenerator {

    /**
     * Generate unified diff from a Change object using IntelliJ's native API.
     * This generates a standard diff with 3 lines of context context by default.
     */
    public static String generateDiff(@NotNull Change change, @NotNull Project project) {
        try {
            // Use IntelliJ's native builder to create patches
            // This handles binary files, context lines, and diff format correctly
            Collection<Change> changes = Collections.singletonList(change);
            List<FilePatch> patches = IdeaTextPatchBuilder.buildPatch(project, changes, project.getBasePath(), false);

            if (patches.isEmpty()) {
                return "";
            }

            // Write the patches to a string in Unified Diff format
            StringWriter writer = new StringWriter();
            UnifiedDiffWriter.write(project, patches, writer, "\n", null);
            
            return writer.toString();
        } catch (VcsException e) {
            // Fallback to simple description if patch generation fails
            return "Error generating diff: " + e.getMessage();
        } catch (Exception e) {
            // Handle other potential issues (encoding, etc)
            return "Failed to generate diff for file: " + (change.getVirtualFile() != null ? change.getVirtualFile().getName() : "unknown");
        }
    }
}
