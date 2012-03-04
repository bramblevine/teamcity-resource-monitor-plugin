
BS.EditResourceForm = OO.extend(BS.AbstractWebForm, {
    formElement : function() {
        return $('editResourceForm');
    },

    saveResource : function() {
        BS.FormSaver.save(this, this.formElement().action, OO.extend(BS.ErrorsAwareListener, {

          onCompleteSave : function(form, responseXML, err) {
            form.enable();
            if (!err) {
              BS.EditResourceDialog.close();
            }
          }
        }), false);

        return false;
    },

    removeResource : function(id) {
        if (!confirm("Are you sure you want to remove this resource?")) return;

        var url = this.formElement().action + "&submitAction=removeResource&resourceId=" + id;
        BS.ajaxRequest(url, {
          onComplete: function() {
            BS.EditResourceDialog.close();
          }
        });
    }
});

BS.EditResourceDialog = OO.extend(BS.AbstractModalDialog, {
    getContainer : function() {
        return $('editResourceDialog');
    },

    showDialog : function(id, name, host, port) {
        $('resourceId').value = id;
        $('resourceName').value = name;
        $('resourceHost').value = host;
        $('resourcePort').value = port;

        var title = name.length == 0 ? 'Add New Resource' : 'Edit Resource';
        $('resourceDialogTitle').innerHTML = title;
        var action = name.length == 0 ? 'addResource' : 'updateResource';
        $('submitAction').value = action;

        this.showCentered();
        $('resourceName').focus();
    },

    cancelDialog : function() {
        this.close();
    }
});

BS.Resource = {
    enableResource: function(id, enable) {
        var url = "/resource.html?submitAction=";
        url = url + ((enable) ? "disableResource" : "enableResource");
        url = url + "&resourceId=" + id;
        BS.ajaxRequest(url, {
            onSuccess: function(transport) {
                document.location.reload();
            },
            onFailure: function() {
                alert('Unable to enable/disable resource');
            }
        });
    },

    linkBuildType: function(id, buildTypeId) {
        var url = "/resource.html?submitAction=linkBuildType&resourceId=" + id + "&buildTypeId=" + buildTypeId;
        BS.ajaxRequest(url, {
            onSuccess: function(transport) {
                document.location.reload();
            },
            onFailure: function() {
                alert('Unable to link dependency');
            }
        });
    },

    unlinkBuildType : function(id, buildTypeId) {
        if (!confirm("Are you sure you want to remove this build configuration?")) return;

        var url = "/resource.html?submitAction=unlinkBuildType&resourceId=" + id + "&buildTypeId=" + buildTypeId;
        BS.ajaxRequest(url, {
          onComplete: function() {
              document.location.reload();
          },
          onFailure: function() {
              alert('Unable to unlink dependency');
          }
        });
    }
};
