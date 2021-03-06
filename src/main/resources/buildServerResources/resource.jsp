<%@ include file="/include.jsp" %>

<jsp:useBean id="resources" scope="request" type="java.util.List"/>
<jsp:useBean id="buildTypes" scope="request" type="java.util.Map"/>
<jsp:useBean id="availableBuildTypes" scope="request" type="java.util.List"/>

<style type="text/css">
    @import "<c:url value='/css/forms.css'/>";
    @import "<c:url value='/css/admin/adminMain.css'/>";
    @import "<c:url value='/css/admin/projectConfig.css'/>";
</style>

<script type="text/javascript">
    BS.ResourceMonitor.start("<c:url value='/resourceStatus.html'/>");
</script>

<script type="text/javascript">
    function addDependency(selector, id) {
        var buildTypeId = selector.options[selector.selectedIndex].value;
        BS.Resource.linkBuildType(id, buildTypeId);
    }
</script>
<script type="text/javascript">
    function toggleResourceBuildTypes(id) {
        jQuery('.buildConfigurationRow.resource_' + id).toggle();
    }
</script>

<div id="container">
    <a class="btn" href="javascript://" onclick="BS.EditResourceDialog.showDialog('','','','','')">
        <span class="addNew">Create resource</span>
    </a>
    <div class="editResourcesPage">
        <div class="descr"></div>
        <c:if test="${not empty resources}">
            <div class="expand_collapse">
                <bs:collapseExpand collapseAction="BS.ResourceUI.collapseAllBuildTypes()"
                                   expandAction="BS.ResourceUI.expandAllBuildTypes()"/>
            </div>
            <c:forEach items="${resources}" var="resource">
                <table id="resource" class="resourceTable">
                    <tbody>
                        <tr class="resource">
                            <td class="name highlight"
                                onclick="toggleResourceBuildTypes('${resource.id}')"
                                title="Click to collapse or expand build types">
                                ${resource.name}
                            </td>
                            <td class="edit highlight">
                                <a href="javascript://"
                                   onclick="BS.Resource.enableResource('${resource.id}', ${resource.enabled})">
                                    <c:choose>
                                        <c:when test="${resource.enabled}">Enabled</c:when>
                                        <c:otherwise>Disabled</c:otherwise>
                                    </c:choose>
                                </a>
                            </td>
                            <td id="resourceStatus_${resource.id}" class="edit highlight">
                                <c:choose>
                                    <c:when test="${resource.available}">Available</c:when>
                                    <c:otherwise>Unavailable</c:otherwise>
                                </c:choose>
                            </td>
                            <td id="resourceUsage_${resource.id}" class="edit highlight">0</td>
                            <td class="host highlight">${resource.host}</td>
                            <td class="port highlight">${resource.port}</td>
                            <td class="edit highlight"
                                onclick="BS.EditResourceDialog.showDialog('${resource.id}', '${resource.name}', '${resource.host}', '${resource.port}', '${resource.buildLimit}');"
                                title="Click to edit resource">
                                <a href="javascript://"
                                   onclick="BS.EditResourceDialog.showDialog('${resource.id}', '${resource.name}', '${resource.host}', '${resource.port}', '${resource.buildLimit}');">
                                    edit
                                </a>
                            </td>
                            <td class="edit">
                                <a href="javascript://" onclick="BS.EditResourceForm.removeResource('${resource.id}');">delete</a>
                            </td>
                        </tr>
                        <c:forEach items="${resource.buildTypes}" var="buildType">
                            <c:if test="${not empty buildTypes[buildType]}">
                                <tr class="buildConfigurationRow resource_${resource.id}">
                                    <td class="buildConfiguration" colspan="7">
                                        <a href="<c:url value='/viewType.html?buildTypeId=${buildTypes[buildType].externalId}&tab=buildTypeStatusDiv'/>">
                                            <c:out value="${buildTypes[buildType].fullName}"/>
                                        </a>
                                    </td>
                                    <td class="edit">
                                        <a href="javascript://" onclick="BS.Resource.unlinkBuildType('${resource.id}', '${buildType}');">delete</a>
                                    </td>
                                </tr>
                            </c:if>
                        </c:forEach>
                        <c:forEach items="${resource.matchedBuildTypes}" var="buildType">
                            <c:if test="${not empty buildTypes[buildType]}">
                                <tr class="buildConfigurationRow">
                                    <td class="buildConfiguration" colspan="7">
                                        <a href="<c:url value='/viewType.html?buildTypeId=${buildTypes[buildType].externalId}&tab=buildTypeStatusDiv'/>">
                                            <c:out value="${buildTypes[buildType].fullName}"/>
                                        </a>
                                    </td>
                                    <td class="edit">
                                    </td>
                                </tr>
                            </c:if>
                        </c:forEach>
                    </tbody>
                </table>
                <p class="addNew">Add dependency:
                    <select name="buildType" onchange="addDependency(this, '${resource.id}')">
                        <option value="">-- Please select a build configuration --</option>
                        <c:forEach items="${availableBuildTypes}" var="buildType">
                            <option value="${buildType}"><c:out value="${buildTypes[buildType].fullName}"/></option>
                        </c:forEach>
                    </select>
                </p>
                <br/>
            </c:forEach>
        </c:if>
    </div>
</div>

<div id="editResourceDialog" class="editResourceDialog modalDialog">
    <div class="dialogHeader">
        <div class="closeWindow">
            <a title="Close dialog window" href="javascript://" onclick="BS.EditResourceDialog.cancelDialog()">close</a>
        </div>
        <h3 id="resourceDialogTitle" class="dialogTitle"></h3>
    </div>

    <div class="modalDialogBody">
        <form id="editResourceForm" action="<c:url value='/resource.html?'/>" method="post"
              onsubmit="return BS.EditResourceForm.saveResource();">
            <label class="resourceNameLabel" for="resourceName">Name: <l:star/></label>
            <forms:textField name="resourceName" maxlength="50" style="width: 22em;"/>
            <span class="error" id="error_resourceName" style="margin-left: 5.5em;"></span>

            <div class="clr" style="height:3px;"></div>
            <label for="resourceHost">Host:</label>
            <forms:textField name="resourceHost" maxlength="50" style="width: 22em;" onchange=""/>
            <span class="error" id="error_resourceHost" style="margin-left: 5.5em;"></span>

            <div class="clr" style="height:3px;"></div>
            <label for="resourcePort">Port: <l:star/></label>
            <forms:textField name="resourcePort" maxlength="5" style="width: 5em;" onchange=""/>
            <span class="error" id="error_resourcePort" style="margin-left: 5.5em;"></span>

            <div class="clr" style="height:3px;"></div>
            <label for="resourceLimit">Build limit (0 - unlimited):</label>
            <forms:textField name="resourceLimit" maxlength="5" style="width: 5em;" onchange=""/>

            <div class="popupSaveButtonsBlock">
                <a href="javascript://" onclick="BS.EditResourceDialog.cancelDialog()" class="cancel">Cancel</a>
                <input class="submitButton" type="submit" value="Save"/>
                <br clear="all"/>
            </div>

            <input id="resourceId" type="hidden" name="resourceId" value=""/>
            <input id="submitAction" type="hidden" name="submitAction" value=""/>
        </form>
    </div>
</div>
