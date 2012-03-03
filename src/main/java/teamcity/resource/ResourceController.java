package teamcity.resource;

import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.AjaxRequestProcessor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jdom.Element;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResourceController extends BaseController {

    private final WebControllerManager webControllerManager;

    private ResourceManager resourceManager;

    private ResourceMonitorPlugin plugin;

    public ResourceController(SBuildServer buildServer, WebControllerManager webControllerManager,
                              ResourceManager resourceManager, ResourceMonitorPlugin plugin)
    {
        super(buildServer);
        this.webControllerManager = webControllerManager;
        this.resourceManager = resourceManager;
        this.plugin = plugin;
    }

    public void register() {
        webControllerManager.registerController("/resource.html", this);
    }

    protected ModelAndView doHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        new AjaxRequestProcessor().processRequest(request, response, new AjaxRequestProcessor.RequestHandler() {
            public void handleRequest(final HttpServletRequest request, final HttpServletResponse response, final Element xmlResponse) {
                try {
                    doAction(request);
                } catch (Exception e) {
                    Loggers.SERVER.warn(e);
                    ActionErrors errors = new ActionErrors();
                    errors.addError("Resource", getMessageWithNested(e));
                    errors.serialize(xmlResponse);
                }
            }
        });
        return null;
    }

    private void doAction(final HttpServletRequest request) throws Exception {
        Loggers.SERVER.info("       method: [" + request.getMethod() + "]");
        Loggers.SERVER.info("submit action: [" + request.getParameter("submitAction") + "]");
        Loggers.SERVER.info("resource name: [" + request.getParameter("resourceName") + "]");
        Loggers.SERVER.info("resource host: [" + request.getParameter("resourceHost") + "]");
        Loggers.SERVER.info("resource port: [" + request.getParameter("resourcePort") + "]");
        Loggers.SERVER.info("build type id: [" + request.getParameter("buildTypeId") + "]");

        String action = request.getParameter("submitAction");
        if ("addResource".equals(action)) {
            String name = request.getParameter("resourceName");
            String host = request.getParameter("resourceHost");
            String port = request.getParameter("resourcePort");
            Resource resource = new Resource("1", name, host, Integer.valueOf(port));
            resourceManager.addResource(resource);
            plugin.saveConfiguration();
        } else if ("updateResource".equals(action)) {
            String name = request.getParameter("resourceName");
            String host = request.getParameter("resourceHost");
            String port = request.getParameter("resourcePort");
            resourceManager.updateResource(name, host, Integer.valueOf(port));
            plugin.saveConfiguration();
        } else if ("removeResource".equals(action)) {
            String name = request.getParameter("resourceName");
            resourceManager.removeResource(name);
            plugin.saveConfiguration();
        } else if ("enableResource".equals(action)) {
            String name = request.getParameter("resourceName");
            resourceManager.enableResource(name);
        } else if ("disableResource".equals(action)) {
            String name = request.getParameter("resourceName");
            resourceManager.disableResource(name);
        } else if ("linkBuildType".equals(action)) {
            String name = request.getParameter("resourceName");
            String buildTypeId = request.getParameter("buildTypeId");
            resourceManager.linkBuildToResource(name, buildTypeId);
            plugin.saveConfiguration();
        } else if ("unlinkBuildType".equals(action)) {
            String name = request.getParameter("resourceName");
            String buildTypeId = request.getParameter("buildTypeId");
            resourceManager.unlinkBuildFromResource(name, buildTypeId);
            plugin.saveConfiguration();
        }
    }

    private String getMessageWithNested(Throwable e) {
        String result = e.getMessage();
        Throwable cause = e.getCause();
        if (cause != null) {
            result += " Caused by: " + getMessageWithNested(cause);
        }
        return result;
    }
}
