(function(cloudStack, testData) {
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

            preFilter: function(args) {
              if(isAdmin()) {
                args.$form.find('.form-item[rel=isForced]').css('display', 'inline-block');
              }
            },

            createForm: {
              title: 'Delete domain',
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
              debugger;
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
              var array1 = [];
              array1.push("&name=" + todb(args.data.name));              
              $.ajax({
                url: createURL("updateDomain&id=" + args.context.domains[0].id + array1.join("")),
                dataType: "json",
                success: function(json) {
                  debugger;                  
                  args.response.success({data: json.updatedomainresponse.domain});
                }
              });   
            }
          },

          // Add domain
          create: {
            label: 'Add domain',

            action: function(args) {
              var array1 = [];
              array1.push("&name=" + todb(args.data.name));
              array1.push("&parentdomainid=" + args.context.domains[0].id);
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
                }
              }
            },

            notification: {
              poll: testData.notifications.testPoll
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
                accounts: { label: 'Accounts' },
                instances: { label: 'Instances' },
                volumes: { label: 'Volumes' }
              }
            ],
            dataProvider: function(args) {
              args.response.success({
                data: args.context.domains[0]
              });
            }
          },
          adminAccounts: {
            title: 'Admin Accounts',
            multiple: true,
            fields: [
              {
                name: { label: 'Name' },
                vmtotal: { label: 'VMs' },
                iptotal: { label: 'IPs' },
                receivedbytes: { label: 'Bytes received' },
                sentbytes: { label: 'Bytes sent' },
                state: { label: 'State' }
              }
            ],
            dataProvider: function(args) {
              args.response.success({
                data: $.grep(testData.data.accounts, function(item, index) {
                  return item.domain === 'ROOT' && index <= 5;
                })
              });
            }
          },
          resourceLimits: {
            title: 'Resource Limits',
            fields: {
              vmlimit: { label: 'Instance Limit' },
              iplimit: { label: 'Public IP Limit' },
              volumelimit: { label: 'Volume Limit' },
              snapshotlimit: { label: 'Snapshot Limit' },
              templatelimit: { label: 'Template Limit' }
            },
            dataProvider: function(args) {
              args.response.success({
                data: testData.data.accounts[4]
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
            url: createURL("listDomains&id=" + g_domainid),
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
      allowedActions.push("add");    
    	if(jsonObj.id != 1) { //"ROOT" domain is not allowed to edit or delete
        allowedActions.push("edit"); //merge updateResourceCount into edit
	      allowedActions.push("delete");	        
    	}    	
    }   
	  //allowedActions.push("updateResourceCount");
    return allowedActions;
  }  
  
})(cloudStack, testData);
