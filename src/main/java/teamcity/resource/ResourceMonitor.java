package teamcity.resource;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.comments.Comment;
import jetbrains.buildServer.users.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ResourceMonitor implements Runnable {

    private static final String PLUGIN_NAME = "ResourceMonitorPlugin";

    private static final int INITIAL_DELAY = 1;

    private static final Logger log = Loggers.SERVER;

    private SBuildServer server;

    private ProjectManager projectManager;

    private ResourceManager resourceManager;

    private AvailabilityChecker checker;

    private ScheduledFuture<?> future;

    public ResourceMonitor() {
        log.info("ResourceMonitor() default constructor");
    }

    public ResourceMonitor(@NotNull SBuildServer server, ProjectManager projectManager, ResourceManager resourceManager, AvailabilityChecker checker) {
        log.info("ResourceMonitor(SBuildServer, ProjectManager, ResourceManager) constructor");
        this.server = server;
        this.projectManager = projectManager;
        this.resourceManager = resourceManager;
        this.checker = checker;
    }

    public void scheduleMonitor() {
        int interval = resourceManager.getInterval();
        log.info(PLUGIN_NAME + ": monitor check interval set to " + interval + "seconds");
        if (future != null) {
            future.cancel(false);
        }
        ScheduledExecutorService executor = server.getExecutor();
        future = executor.scheduleAtFixedRate(this, INITIAL_DELAY, interval, TimeUnit.SECONDS);
    }

    public void run() {
        int enabled = 0;
        int available = 0;
        for (Resource resource : getResources().values()) {
            if (isEnabled(resource)) {
                enabled++;
            }
            if (checker.isAvailable(resource)) {
                resourceAvailable(resource);
                available++;
            } else {
                resourceUnavailable(resource);
            }
        }
        log.info("Monitored resources: " + getResources().size() + ", enabled: " + enabled + ", available: " + available);
    }

    private boolean isEnabled(Resource resource) {
        return resource.isEnabled();
    }

    public void resourceAvailable(Resource resource) {
        List<String> buildTypes = resource.getBuildTypes();
        for (String buildTypeId : buildTypes) {
            SBuildType buildType = projectManager.findBuildTypeById(buildTypeId);
            if (buildType != null) {
                if (canActivate(buildType)) {
                    User user = getUser();
                    String comment = "Resource " + resource.getName() + " is available, build activated by " + PLUGIN_NAME;
                    buildType.setPaused(false, user, comment);
                    String message = "Resource " + resource.getName() + " is available, build '" + buildType.getFullName() + "' activated by " + PLUGIN_NAME;
                    log.info(message);
                }
            }
        }
    }

    private boolean canActivate(SBuildType buildType) {
        boolean result = false;
        if (buildType.isPaused()) {
            Comment comment = buildType.getPauseComment();
            if (comment != null) {
                String commentText = comment.getComment();
                result = commentText != null && commentText.contains(PLUGIN_NAME);
            }
        }
        return result;
    }

    public void resourceUnavailable(Resource resource) {
        List<String> buildTypes = resource.getBuildTypes();
        for (String buildTypeId : buildTypes) {
            SBuildType buildType = projectManager.findBuildTypeById(buildTypeId);
            if (buildType != null) {
                if (!buildType.isPaused()) {
                    User user = getUser();
                    String comment = "Resource " + resource.getName() + " is unavailable, build de-activated by " + PLUGIN_NAME;
                    buildType.setPaused(true, user, comment);
                    String message = "Resource " + resource.getName() + " is unavailable, build '" + buildType.getFullName() + "' de-activated by " + PLUGIN_NAME;
                    log.info(message);
                }
            } else {
                log.warn("Resource '" + resource.getName() + "' is linked to build id '" + buildTypeId + "' that doesn't exist");
            }
        }
    }

    private User getUser() {
        return new ResourceUser();
    }

    private Map<String,Resource> getResources() {
        return resourceManager.getResources();
    }
}
