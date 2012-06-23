package teamcity.resource;

import jetbrains.buildServer.BuildAgent;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ResourceBuildLimitStartPrecondition extends BuildServerAdapter implements StartBuildPrecondition {

    private ResourceManager manager;

    private Map<String, ResourceBuildCount> resourceBuildCounts = new HashMap<String, ResourceBuildCount>();

    ResourceBuildLimitStartPrecondition(SBuildServer buildServer, final ResourceManager manager) {
        buildServer.addListener(this);
        this.manager = manager;
    }

    public WaitReason canStart(@NotNull QueuedBuildInfo queuedBuildInfo,
                               @NotNull Map<QueuedBuildInfo, BuildAgent> queuedBuildInfoBuildAgentMap,
                               @NotNull BuildDistributorInput buildDistributorInput,
                               boolean emulationMode)
    {
        String buildTypeId = queuedBuildInfo.getBuildConfiguration().getId();
        Loggers.SERVER.debug("Build canStart check for '" + buildTypeId + "'");

        WaitReason waitReason = null;
        Resource resource = manager.findResourceByBuildTypeId(buildTypeId);
        if (resource != null) {
            int buildLimit = resource.getBuildLimit();
            if (buildLimit > 0) {
                int currentBuilds = getResourceBuildCount(resource.getId()).value;

                if (currentBuilds >= buildLimit) {
                    waitReason = new SimpleWaitReason("Build cannot start until the number of builds using the resource "
                            + resource.getName() + " is below the limit of " + buildLimit);
                    Loggers.SERVER.debug(waitReason.getDescription());
                }
            }
        }
        return waitReason;
    }

    @Override
    public void buildStarted(SRunningBuild build) {
        String buildTypeId = build.getBuildTypeId();
        Resource resource = manager.findResourceByBuildTypeId(buildTypeId);
        if (resource != null) {
            ResourceBuildCount resourceBuildCount = getResourceBuildCount(resource.getId());
            resourceBuildCount.value++;
        }
    }

    @Override
    public void buildFinished(SRunningBuild build) {
        String buildTypeId = build.getBuildTypeId();
        Resource resource = manager.findResourceByBuildTypeId(buildTypeId);
        if (resource != null) {
            ResourceBuildCount resourceBuildCount = getResourceBuildCount(resource.getId());
            resourceBuildCount.value--;
        }
    }

    private ResourceBuildCount getResourceBuildCount(String id) {
        ResourceBuildCount buildCount = resourceBuildCounts.get(id);
        if (buildCount == null) {
            buildCount = new ResourceBuildCount();
            resourceBuildCounts.put(id, buildCount);
        }
        return buildCount;
    }

    private static class ResourceBuildCount {
        int value = 0;
    }
}
