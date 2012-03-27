package teamcity.resource;

import jetbrains.buildServer.BuildAgent;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ResourceBuildLimitStartPrecondition extends BuildServerAdapter implements StartBuildPrecondition {

    private ResourceManager manager;

    private Map<String, Integer> resourceBuildCounts = new HashMap<String, Integer>();

    ResourceBuildLimitStartPrecondition(final ResourceManager manager) {
        this.manager = manager;
    }

    public WaitReason canStart(@NotNull QueuedBuildInfo queuedBuildInfo,
                               @NotNull Map<QueuedBuildInfo, BuildAgent> queuedBuildInfoBuildAgentMap,
                               @NotNull BuildDistributorInput buildDistributorInput,
                               boolean emulationMode)
    {
        String buildTypeId = queuedBuildInfo.getBuildConfiguration().getId();
        Loggers.SERVER.info("Build canStart check for '" + buildTypeId + "'");

        WaitReason waitReason = null;
        Resource resource = manager.findResourceByBuildTypeId(buildTypeId);
        if (resource != null) {
            int buildLimit = resource.getBuildLimit();
            if (buildLimit > 0) {
                int currentBuilds = getBuildCount(resource.getId());

//            Loggers.SERVER.info("Resource: '" + resource.getName() + "', enabled: '" + enabled + "', available: '" + available + "'");
                if (currentBuilds >= buildLimit) {
                    waitReason = new SimpleWaitReason("Build cannot start until the required resource " + resource.getName() + " build limit is " + buildLimit);
                    Loggers.SERVER.info(waitReason.getDescription());
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
            Integer buildCount = resourceBuildCounts.get(resource.getId());
            if (buildCount == null) {
                buildCount = 1;
            } else {
                buildCount = buildCount + 1;
            }
            resourceBuildCounts.put(resource.getId(), buildCount);
        }
    }

    @Override
    public void buildFinished(SRunningBuild build) {
        String buildTypeId = build.getBuildTypeId();
        Resource resource = manager.findResourceByBuildTypeId(buildTypeId);
        if (resource != null) {
            Integer buildCount = resourceBuildCounts.get(resource.getId());
            if (buildCount == null) {
                buildCount = 0;
            } else {
                buildCount = buildCount - 1;
            }
            resourceBuildCounts.put(resource.getId(), buildCount);
        }
    }

    private int getBuildCount(String id) {
        Integer buildCount = resourceBuildCounts.get(id);
        return (buildCount == null) ? 0 : buildCount;
    }
}
