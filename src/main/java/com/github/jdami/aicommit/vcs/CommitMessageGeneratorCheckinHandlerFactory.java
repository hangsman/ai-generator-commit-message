package com.github.jdami.aicommit.vcs;

import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Factory for creating checkin handlers that add UI to commit dialog
 */
public class CommitMessageGeneratorCheckinHandlerFactory extends CheckinHandlerFactory {

    @NotNull
    @Override
    public CheckinHandler createHandler(@NotNull CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
        return new CheckinHandler() {
            @Override
            public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
                return new RefreshableOnComponent() {
                    private JPanel panel;

                    @Override
                    public JComponent getComponent() {
                        if (panel == null) {
                            panel = new JPanel();
                            // The actual button is registered as an action in plugin.xml
                            // This handler is here to ensure proper integration with VCS
                        }
                        return panel;
                    }

                    @Override
                    public void refresh() {
                        // No-op
                    }

                    @Override
                    public void saveState() {
                        // No-op
                    }

                    @Override
                    public void restoreState() {
                        // No-op
                    }
                };
            }
        };
    }
}
