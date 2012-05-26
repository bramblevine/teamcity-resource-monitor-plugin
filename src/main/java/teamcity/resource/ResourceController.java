package teamcity.resource;

import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.AjaxRequestProcessor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResourceController extends BaseController {

    private final WebControllerManager webControllerManager;

    private ResourceManager resourceManager;

    private ResourceMonitorPlugin plugin;

    private ResourceMonitor monitor;

    public ResourceController(SBuildServer buildServer, WebControllerManager webControllerManager,
                              ResourceManager resourceManager, ResourceMonitorPlugin plugin, ResourceMonitor monitor)
    {
        super(buildServer);
        this.webControllerManager = webControllerManager;
        this.resourceManager = resourceManager;
        this.plugin = plugin;
        this.monitor = monitor;
    }

    public void register() {
        webControllerManager.registerController("/resource.html", this);
    }

    protected ModelAndView doHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        new AjaxRequestProcessor().processRequest(request, response, new AjaxRequestProcessor.RequestHandler() {
            public void handleRequest(@NotNull final HttpServletRequest request,
                                      @NotNull final HttpServletResponse response,
                                      @NotNull final Element xmlResponse)
            {
                try {
                    doAction(request);
                } catch (InvalidNameException e) {
                    buildExceptionResponse("invalidName", e, xmlResponse);
                } catch (InvalidHostException e) {
                    buildExceptionResponse("invalidHost", e, xmlResponse);
                } catch (InvalidPortException e) {
                    buildExceptionResponse("invalidPort", e, xmlResponse);
                } catch (Exception e) {
                    Loggers.SERVER.warn(e);
                    buildExceptionResponse("resource", e, xmlResponse);
                }
            }
        });
        return null;
    }

    private void doAction(final HttpServletRequest request) throws Exception {
        Loggers.SERVER.debug("       method: [" + request.getMethod() + "]");
        Loggers.SERVER.debug("submit action: [" + request.getParameter("submitAction") + "]");
        Loggers.SERVER.debug("  resource id: [" + request.getParameter("resourceId") + "]");
        Loggers.SERVER.debug("resource name: [" + request.getParameter("resourceName") + "]");
        Loggers.SERVER.debug("resource host: [" + request.getParameter("resourceHost") + "]");
        Loggers.SERVER.debug("resource port: [" + request.getParameter("resourcePort") + "]");
        Loggers.SERVER.debug("build type id: [" + request.getParameter("buildTypeId") + "]");

        String action = request.getParameter("submitAction");
        if ("addResource".equals(action)) {
            String name = request.getParameter("resourceName");
            String host = request.getParameter("resourceHost");
            String port = request.getParameter("resourcePort");
            resourceManager.addResource(name, host, port);
            plugin.saveConfiguration();
        } else if ("updateResource".equals(action)) {
            String id = request.getParameter("resourceId");
            String name = request.getParameter("resourceName");
            String host = request.getParameter("resourceHost");
            String port = request.getParameter("resourcePort");
            resourceManager.updateResource(id, name, host, port);
            plugin.saveConfiguration();
        } else if ("removeResource".equals(action)) {
            String id = request.getParameter("resourceId");
            resourceManager.removeResource(id);
            plugin.saveConfiguration();
        } else if ("linkBuildType".equals(action)) {
            String id = request.getParameter("resourceId");
            String buildTypeId = request.getParameter("buildTypeId");
            resourceManager.linkBuildToResource(id, buildTypeId);
            plugin.saveConfiguration();
        } else if ("unlinkBuildType".equals(action)) {
            String id = request.getParameter("resourceId");
            String buildTypeId = request.getParameter("buildTypeId");
            resourceManager.unlinkBuildFromResource(id, buildTypeId);
            plugin.saveConfiguration();
        } else if ("enableResource".equals(action)) {
            String id = request.getParameter("resourceId");
            monitor.enableResource(resourceManager.getResourceById(id));
        } else if ("disableResource".equals(action)) {
            String id = request.getParameter("resourceId");
            monitor.disableResource(resourceManager.getResourceById(id));
        } else {
            throw new IllegalArgumentException("Invalid action: " + action);
        }
    }

    private void buildExceptionResponse(String name, Exception e, Element xmlResponse) {
        ActionErrors errors = new ActionErrors();
        errors.addError(name, getMessageWithNested(e));
        errors.serialize(xmlResponse);
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
