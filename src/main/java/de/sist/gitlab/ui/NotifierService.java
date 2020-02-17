package de.sist.gitlab.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.impl.NotificationsManagerImpl;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.BalloonImpl;
import com.intellij.ui.BalloonLayoutData;
import com.intellij.ui.awt.RelativePoint;
import de.sist.gitlab.DateTime;
import de.sist.gitlab.PipelineJobStatus;
import de.sist.gitlab.ReloadListener;
import org.jetbrains.annotations.NotNull;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class NotifierService {

    private static final Logger logger = Logger.getInstance(NotifierService.class);

    private static final List<String> KNOWN_STATUSES = Arrays.asList("pending", "running", "canceled", "failed", "success", "skipped");

    private final Project project;

    private Set<PipelineJobStatus> shownNotifications;

    private final Map<String, NotificationGroup> statusesToNotificationGroups = new HashMap<>();
    private final NotificationGroup errorNotificationGroup = new NotificationGroup("Gitlab Pipeline Viewer - Error", NotificationDisplayType.BALLOON, true,
            "Gitlab pipeline viewer", IconLoader.getIcon("/toolWindow/gitlab-icon.png"));

    public NotifierService(Project project) {
        this.project = project;

        KNOWN_STATUSES.forEach(x -> statusesToNotificationGroups.put(x, createNotificationGroup(x)));

        project.getMessageBus().connect().subscribe(ReloadListener.RELOAD, this::showNotifications);
    }

    private void showNotifications(List<PipelineJobStatus> statuses) {
        if (shownNotifications == null) {
            //Don't show notifications for pipeline statuses from before the program was started
            shownNotifications = new HashSet<>(statuses);


            return;
        }

        statuses.stream().filter(x ->
                !shownNotifications.contains(x)
        )
                .forEach(status -> {

                    NotificationGroup notificationGroup = statusesToNotificationGroups.get(status.result);

                    NotificationType notificationType;
                    String content;
                    if (Stream.of("failed", "canceled", "skipped").anyMatch(s -> status.result.equals(s))) {
                        notificationType = NotificationType.ERROR;
                    } else {
                        notificationType = NotificationType.INFORMATION;
                    }
                    content = status.branchName + ": <span style=\"color:" + getColorForStatus(status.result) + "\">" + status.result + "</span>"
                            + "<br>Created: " + DateTime.formatDateTime(status.creationTime)
                            + "<br>Last update: " + DateTime.formatDateTime(status.updateTime);

                    Notification notification = notificationGroup.createNotification("Gitlab branch status", null, content, notificationType);


                    notification.addAction(new NotificationAction("Open in Browser") {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                            com.intellij.ide.BrowserUtil.browse(status.pipelineLink);
                            notification.expire();
                        }
                    });

                    logger.debug("Showing notification for status " + status);
                    showBalloon(notification);
                    shownNotifications.add(status);

                });
    }

    private void showBalloon(Notification notification) {
        IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(project);
        if (ideFrame == null) {
            logger.error("ideFrame is null");
        } else {
            Rectangle bounds = ideFrame.getComponent().getBounds();
            Balloon balloon = NotificationsManagerImpl.createBalloon(ideFrame, notification, false, true, BalloonLayoutData.fullContent(), project);
            Dimension preferredSize = new Dimension(350, 100);
            ((BalloonImpl) balloon).getContent().setPreferredSize(preferredSize);
            Point pointForRelativePosition = new Point(bounds.x + bounds.width - 209, bounds.y + bounds.height - 111);
            balloon.show(new RelativePoint(ideFrame.getComponent(), pointForRelativePosition), Balloon.Position.above);
        }
    }

    private String getColorForStatus(String result) {
        switch (result) {
            case "running":
                return "orange";
            case "pending":
                return "grey";
            case "success":
                return "green";
            case "failed":
                return "red";
            case "skipped":
            case "canceled":
                return "blue";
        }
        return "";

    }

    @NotNull
    private NotificationGroup createNotificationGroup(String status) {
        return new NotificationGroup("Gitlab Pipeline Viewer - status " + status, NotificationDisplayType.BALLOON, true, "Gitlab pipeline viewer", IconLoader.getIcon("/toolWindow/gitlab-icon.png"));
    }


}
