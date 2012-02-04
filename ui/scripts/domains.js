(function(cloudStack) {
  cloudStack.sections.domains = {
    title: 'Domains',
    id: 'domains',

    // Domain tree
    treeView: {
      // Details
      detailView: {
        name: 'Domain details',
        viewAll: {
          label: 'Accounts',
          path: 'accounts'
        },

        // Detail actions
        actions: {
          'delete': {
            label: 'Delete domain',
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to delete this domain?'
              },
              notification: function(args) {
                return 'Domain is deleted';
              }
            },

            createForm: {
              title: 'Delete domain',
              createLabel: 'Delete',
              preFilter: function(args) {
                if(isAdmin()) {
                  args.$form.find('.form-item[rel=isForced]').css('display', 'inline-block');
                }
              },
              fields: {
                isForced: {
                  label: 'Force delete',
                  isBoolean: true,
                  isHidden: true
                }
              }
            },

            action: function(args) {
              var array1 = [];
              if(args.$form.find('.form-item[rel=isForced]').css("display") != "none") //uncomment after Brian fix it to include $form in args
                array1.push("&cleanup=" + (args.data.isForced == "on"));

              $.ajax({
                url: createURL("deleteDomain&id=" + args.context.domains[0].id + array1.join("")),
                dataType: "json",
                async: false,
                success: function(json) {
                  var jid = json.deletedomainresponse.jobid;
                  args.response.success(
                    {_custom:
                     {jobId: jid}
                    }
                  );

                  // Quick fix for proper UI reaction to delete domain
                  var $item = $('.name.selected').closest('li');
                  var $itemParent = $item.closest('li');
                  $itemParent.parent().parent().find('.name:first').click();
                  $item.remove();
                }
              });
            },
            notification: {
              poll: pollAsyncJobResult
            }
          },

          // Edit domain
          edit: {
            label: 'Edit domain details',
            messages: {
              notification: function(args) {
                return 'Edited domain: ' + args.name;
              }
            },
            action: function(args) {
              var domainObj;
              var array1 = [];
              array1.push("&name=" + todb(args.data.name));
              array1.push("&networkdomain=" + todb(args.data.networkdomain));
              $.ajax({
                url: createURL("updateDomain&id=" + args.context.domains[0].id + array1.join("")),
                async: false,
                dataType: "json",
                success: function(json) {
                  domainObj = json.updatedomainresponse.domain;
                }
              });

              $.ajax({
                url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=0&max=" + args.data.vmLimit),
                dataType: "json",
                async: false,
                success: function(json) {
                  domainObj["vmLimit"] = args.data.vmLimit;
                }
              });
              $.ajax({
                url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=1&max=" + args.data.ipLimit),
                dataType: "json",
                async: false,
                success: function(json) {
                  domainObj["ipLimit"] = args.data.ipLimit;
                }
              });
              $.ajax({
                url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=2&max=" + args.data.volumeLimit),
                dataType: "json",
                async: false,
                success: function(json) {
                  domainObj["volumeLimit"] = args.data.volumeLimit;
                }
              });
              $.ajax({
                url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=3&max=" + args.data.snapshotLimit),
                dataType: "json",
                async: false,
                success: function(json) {
                  domainObj["snapshotLimit"] = args.data.snapshotLimit;
                }
              });
              $.ajax({
                url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=4&max=" + args.data.templateLimit),
                dataType: "json",
                async: false,
                success: function(json) {
                  domainObj["templateLimit"] = args.data.templateLimit;
                }
              });

              args.response.success({data: domainObj});
            }
          },

          // Add domain
          create: {
            label: 'Add domain',

            action: function(args) {
              var array1 = [];
              array1.push("&parentdomainid=" + args.context.domains[0].id);
              array1.push("&name=" + todb(args.data.name));     
              if(args.data.networkdomain != null && args.data.networkdomain.length > 0)              
                array1.push("&networkdomain=" + todb(args.data.networkdomain));  
                
              $.ajax({
                url: createURL("createDomain" + array1.join("")),
                dataType: "json",
                async: false,
                success: function(json) {
                  var item = json.createdomainresponse.domain;
                  args.response.success({data: item});
                }
              });
            },

            messages: {
              notification: function(args) {
                return 'Created domain'
              }
            },

            createForm: {
              title: 'Add subdomain',
              desc: 'Please specify the subdomain you want to create under this domain',
              fields: {
                name: {
                  label: 'Name',
                  validation: { required: true }
                },
                networkdomain: {
                  label: 'Network Domain',
                  validation: { required: false }
                }
              }
            }
          }
        },
        tabs: {
          details: {
            title: 'Details',
            fields: [
              {
                name: { label: 'Name', isEditable: true }
              },
              {
                id: { label: 'ID' },
								
								path: { label: 'Full path'},
								
                networkdomain: { 
                  label: 'Network domain',
                  isEditable: true
                },
                vmLimit: {
                  label: 'Instance limits',
                  isEditable: true
                },
                ipLimit: {
                  label: 'Public IP limits',
                  isEditable: true
                },
                volumeLimit: {
                  label: 'Volume limits',
                  isEditable: true
                },
                snapshotLimit: {
                  label: 'Snapshot limits',
                  isEditable: true
                },
                templateLimit: {
                  label: 'Template limits',
                  isEditable: true
                },
                accountTotal: { label: 'Accounts' },
                vmTotal: { label: 'Instances' },
                volumeTotal: { label: 'Volumes' }
              }
            ],
            dataProvider: function(args) {
              var domainObj = args.context.domains[0];
              $.ajax({
                url: createURL("listAccounts&domainid=" + domainObj.id),
                async: false,
                dataType: "json",
                success: function(json) {
                  var items = json.listaccountsresponse.account;
                  var total;
                  if (items != null)
                    total = items.length;
                  else
                    total = 0;
                  domainObj["accountTotal"] = total;
                }
              });

              $.ajax({
                url: createURL("listVirtualMachines&domainid=" + domainObj.id),
                async: false,
                dataType: "json",
                success: function(json) {
                  var items = json.listvirtualmachinesresponse.virtualmachine;
                  var total;
                  if (items != null)
                    total = items.length;
                  else
                    total = 0;
                  domainObj["vmTotal"] = total;
                }
              });

              $.ajax({
                url: createURL("listVolumes&domainid=" + domainObj.id),
                async: false,
                dataType: "json",
                success: function(json) {
                  var items = json.listvolumesresponse.volume;
                  var total;
                  if (items != null)
                    total = items.length;
                  else
                    total = 0;
                  domainObj["volumeTotal"] = total;
                }
              });

              $.ajax({
                url: createURL("listResourceLimits&domainid=" + domainObj.id),
                async: false,
                dataType: "json",
                success: function(json) {
                  var limits = json.listresourcelimitsresponse.resourcelimit;
                  if (limits != null) {
                    for (var i = 0; i < limits.length; i++) {
                      var limit = limits[i];
                      switch (limit.resourcetype) {
                        case "0":
                          domainObj["vmLimit"] = limit.max;
                          break;
                        case "1":
                          domainObj["ipLimit"] = limit.max;
                          break;
                        case "2":
                          domainObj["volumeLimit"] = limit.max;
                          break;
                        case "3":
                          domainObj["snapshotLimit"] = limit.max;
                          break;
                        case "4":
                          domainObj["templateLimit"] = limit.max;
                          break;
                      }
                    }
                  }
                }
              });

              args.response.success({
                data: domainObj,
                actionFilter: domainActionfilter
              });
            }
          }
        }
      },
      labelField: 'name',
      dataProvider: function(args) {
        var parentDomain = args.context.parentDomain;
        if(parentDomain == null) { //draw root node
          $.ajax({
            url: createURL("listDomains&id=" + g_domainid + '&listAll=true'),
            dataType: "json",
            async: false,
            success: function(json) {
              var domainObjs = json.listdomainsresponse.domain;
              args.response.success({
                actionFilter: domainActionfilter,
                data: domainObjs
              });
            }
          });
        }
        else {
          $.ajax({
            url: createURL("listDomainChildren&id=" + parentDomain.id),
            dataType: "json",
            async: false,
            success: function(json) {
              var domainObjs = json.listdomainchildrenresponse.domain;
              args.response.success({
                actionFilter: domainActionfilter,
                data: domainObjs
              });
            }
          });
        }
      }
    }
  };

  var domainActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];
    if(isAdmin()) {
      allowedActions.push("create");     
			if(jsonObj.level != 0) { //ROOT domain (whose level is 0) is not allowed to edit or delete
        allowedActions.push("edit"); //merge updateResourceCount into edit
        allowedActions.push("delete");
      }
    }
    //allowedActions.push("updateResourceCount");
    return allowedActions;
  }

})(cloudStack);
