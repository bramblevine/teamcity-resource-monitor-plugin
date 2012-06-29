package teamcity.resource;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.SimpleCustomTab;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class ResourceConfigExtension extends SimpleCustomTab {

    private ProjectManager projectManager;

    private ResourceManager resourceManager;

    private ResourceMonitor resourceMonitor;

    public ResourceConfigExtension(PagePlaces pagePlaces, ProjectManager projectManager, ResourceManager resourceManager, ResourceMonitor resourceMonitor) {
        super(pagePlaces);
        this.projectManager = projectManager;
        this.resourceManager = resourceManager;
        this.resourceMonitor = resourceMonitor;
    }

    @NotNull
    public String getTabId() {
        return "resourceConfig";
    }

    @NotNull
    public String getTabTitle() {
        return "Resources";
    }

    @NotNull
    public List<String> getCssPaths() {
        List<String> cssPaths = new ArrayList<String>();
        cssPaths.add("resource.css");
        return cssPaths;
    }

    @NotNull
    public List<String> getJsPaths() {
        List<String> jsPaths = new ArrayList<String>();
        jsPaths.add("resource.js");
        return jsPaths;
    }

    public boolean isVisible() {
        return true;
    }

    public boolean isAvailable(@NotNull HttpServletRequest request) {
        return true;
    }

    public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
        model.put("resources", getResources());
        model.put("buildTypes", getBuildTypes());
        model.put("availableBuildTypes", getAvailableBuildTypes());
    }

    private List<ResourceState> getResources() {
        List<ResourceState> resources = new ArrayList<ResourceState>();
        for (Resource resource : resourceManager.getResources()) {
            boolean available = isAvailable(resource);
            boolean enabled = isEnabled(resource);
            resources.add(new ResourceState(resource, available, enabled));
        }
        Collections.sort(resources, new Comparator<ResourceState>() {
            public int compare(ResourceState o1, ResourceState o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return resources;
    }

    private boolean isAvailable(Resource resource) {
        return resourceMonitor.isAvailable(resource);
    }

    private boolean isEnabled(Resource resource) {
        return resourceMonitor.isEnabled(resource);
    }

    private Map getBuildTypes() {
        List<SBuildType> buildTypes = projectManager.getAllBuildTypes();
        Map<String, SBuildType> buildTypesMap = new HashMap<String, SBuildType>();
        for (SBuildType buildType : buildTypes) {
            buildTypesMap.put(buildType.getBuildTypeId(), buildType);
        }
        return buildTypesMap;
    }

    private List<String> getAvailableBuildTypes() {
        List<String> availableBuildTypes = new ArrayList<String>();
        for (SBuildType buildType : projectManager.getAllBuildTypes()) {
            availableBuildTypes.add(buildType.getBuildTypeId());
        }
        for (Resource resource : resourceManager.getResources()) {
            List<String> usedBuildTypes = resource.getBuildTypes();
            availableBuildTypes.removeAll(usedBuildTypes);
        }
        return availableBuildTypes;
    }
}
