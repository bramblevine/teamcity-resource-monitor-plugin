package teamcity.resource;

import static teamcity.resource.ResourceMonitorPlugin.log;

import jetbrains.buildServer.BuildAgent;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ResourceStartBuildPrecondition implements StartBuildPrecondition, ResourceMonitorListener {

    private ResourceManager manager;

    private Set<String> unavailableResources = new HashSet<String>();

    private Set<String> disabledResources = new HashSet<String>();

    public ResourceStartBuildPrecondition(final ResourceManager manager, final ResourceMonitor monitor) {
        this.manager = manager;
        monitor.addListener(this);
    }

    public WaitReason canStart(@NotNull QueuedBuildInfo queuedBuildInfo,
                               @NotNull Map<QueuedBuildInfo, BuildAgent> queuedBuildInfoBuildAgentMap,
                               @NotNull BuildDistributorInput buildDistributorInput,
                               boolean emulationMode)
    {
        String buildTypeId = queuedBuildInfo.getBuildConfiguration().getId();

        WaitReason waitReason = null;
        Resource resource = manager.findResourceByBuildTypeId(buildTypeId);
        if (resource != null) {
            boolean available = isAvailable(resource);
            boolean enabled = isEnabled(resource);
            log.trace("Resource: '" + resource.getName() + "', enabled: '" + enabled + "', available: '" + available + "'");
            if (!available || !enabled) {
                String state = enabled ? "available" : "enabled";
                waitReason = new SimpleWaitReason("Build cannot start until the required resource " + resource.getName() + " is " + state);
                log.trace(waitReason.getDescription());
            }
        }
        return waitReason;
    }

    public void resourceAvailable(Resource resource) {
        unavailableResources.remove(resource.getId());
    }

    public void resourceUnavailable(Resource resource) {
        unavailableResources.add(resource.getId());
    }

    public void resourceEnabled(Resource resource) {
        disabledResources.remove(resource.getId());
    }

    public void resourceDisabled(Resource resource) {
        disabledResources.add(resource.getId());
    }

    private boolean isAvailable(Resource resource) {
        return !unavailableResources.contains(resource.getId());
    }

    private boolean isEnabled(Resource resource) {
        return !disabledResources.contains(resource.getId());
    }
}
