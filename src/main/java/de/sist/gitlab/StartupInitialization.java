package de.sist.gitlab;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import de.sist.gitlab.config.PipelineViewerConfig;
import de.sist.gitlab.notifier.NotifierService;
import org.jetbrains.annotations.NotNull;

public class StartupInitialization implements StartupActivity {

    Logger logger = Logger.getInstance(StartupInitialization.class);

    @Override
    public void runActivity(@NotNull Project project) {
        //Get service so it's initialized
        project.getService(NotifierService.class);

        PipelineViewerConfig config = PipelineViewerConfig.getInstance(project);
        config.initIfNeeded();

        project.getService(BackgroundUpdateService.class).startBackgroundTask();

    }
}
