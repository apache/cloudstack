(function($, cloudStack, testData) {

  var zoneObjs, hypervisorObjs, featuredTemplateObjs, communityTemplateObjs, myTemplateObjs, isoObjs, serviceOfferingObjs, diskOfferingObjs, networkOfferingObjs;
  var selectedZoneObj, selectedTemplateObj, selectedHypervisor, selectedDiskOfferingObj; 
  var step5ContainerType = 'nothing-to-select'; //'nothing-to-select', 'select-network', 'select-security-group'

  cloudStack.sections.instances = {
    title: 'Instances',
    id: 'instances',
    listView: {
      section: 'instances',
      filters: {
	    all: { label: 'All' },
        mine: { label: 'Mine' },
        running: { label: 'Running' },
        stopped: { label: 'Stopped' },
        destroyed: { label: 'Destroyed' }
      },
      fields: {
        name: { label: 'Name', editable: true },
        displayname: { label: 'Display Name' },
        zonename: { label: 'Zone' },
        state: {
          label: 'Status',
          indicator: {
            'Running': 'on',
            'Stopped': 'off',
            'Error': 'off'
          }
        }
      },

      // List view actions
      actions: {
        // Add instance wizard
        add: {
          label: 'Add instance',

          action: {
            custom: cloudStack.instanceWizard({
              steps: [
                // Step 1: Setup
                function(args) {
                  $.ajax({
                    url: createURL("listZones&available=true"),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      zoneObjs = json.listzonesresponse.zone;
                      args.response.success({ data: {zones: zoneObjs}});
                    }
                  });
                },

                // Step 2: Select template
                function(args) {
                  $(zoneObjs).each(function(){
                    if(this.id == args.currentData.zoneid) {
                      selectedZoneObj = this;
                      return false; //break the $.each() loop
                    }
                  });
                  if(selectedZoneObj == null) {
                    alert("error: can't find matched zone object");
                    return;
                  }

                  $.ajax({
                    url: createURL("listHypervisors&zoneid="+args.currentData.zoneid),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      hypervisorObjs = json.listhypervisorsresponse.hypervisor;
                    }
                  });

                  $.ajax({
                    url: createURL("listTemplates&templatefilter=featured&zoneid="+args.currentData.zoneid),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      featuredTemplateObjs = json.listtemplatesresponse.template;
                    }
                  });

                  $.ajax({
                    url: createURL("listTemplates&templatefilter=community&zoneid="+args.currentData.zoneid),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      communityTemplateObjs = json.listtemplatesresponse.template;
                    }
                  });

                  $.ajax({
                    url: createURL("listTemplates&templatefilter=selfexecutable&zoneid="+args.currentData.zoneid),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      myTemplateObjs = json.listtemplatesresponse.template;
                    }
                  });

                  $.ajax({
                    url: createURL("listIsos&isReady=true&bootable=true&isofilter=executable&zoneid="+args.currentData.zoneid),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      isoObjs = json.listisosresponse.iso;
                    }
                  });

                  args.response.success({
                    hypervisor: {
                      idField: 'name',
                      nameField: 'name'
                    },
                    data: {
                      templates: {
                        featuredtemplates: featuredTemplateObjs,
                        communitytemplates: communityTemplateObjs,
                        mytemplates: myTemplateObjs,
                        isos: isoObjs
                      },
                      hypervisors: hypervisorObjs
                    }
                  });
                },

                // Step 3: Service offering
                function(args) {
                  if(args.currentData["select-template"] == "select-template") {
                    if(featuredTemplateObjs != null && featuredTemplateObjs.length > 0) {
                      for(var i=0; i < featuredTemplateObjs.length; i++) {
                        if(featuredTemplateObjs[i].id == args.currentData.templateid) {
                          selectedTemplateObj = featuredTemplateObjs[i];
                          break;
                        }
                      }
                    }
                    if(selectedTemplateObj == null) {
                      if(communityTemplateObjs != null && communityTemplateObjs.length > 0) {
                        for(var i=0; i < communityTemplateObjs.length; i++) {
                          if(communityTemplateObjs[i].id == args.currentData.templateid) {
                            selectedTemplateObj = communityTemplateObjs[i];
                            break;
                          }
                        }
                      }
                    }
                    if(selectedTemplateObj == null) {
                      if(myTemplateObjs != null && myTemplateObjs.length > 0) {
                        for(var i=0; i < myTemplateObjs.length; i++) {
                          if(myTemplateObjs[i].id == args.currentData.templateid) {
                            selectedTemplateObj = myTemplateObjs[i];
                            break;
                          }
                        }
                      }
                    }
                    if(selectedTemplateObj == null)
                      alert("unable to find matched template object");
                    else
                      selectedHypervisor = selectedTemplateObj.hypervisor;
                  }
                  else { //(args.currentData["select-template"] == "select-iso"
                    selectedHypervisor = args.currentData.hypervisorid;
                  }

                  $.ajax({
                    url: createURL("listServiceOfferings&issystem=false"),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      serviceOfferingObjs = json.listserviceofferingsresponse.serviceoffering;
                      args.response.success({
                        data: {serviceOfferings: serviceOfferingObjs}
                      });
                    }
                  });
                },

                // Step 4: Data disk offering
                function(args) {
                  var isRequred = (args.currentData["select-template"] == "select-iso"? true: false);
                  $.ajax({
                    url: createURL("listDiskOfferings"),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      diskOfferingObjs = json.listdiskofferingsresponse.diskoffering;
                      args.response.success({
                        required: isRequred,
                        customFlag: 'iscustomized', // Field determines if custom slider is shown
                        data: {diskOfferings: diskOfferingObjs}
                      });
                    }
                  });
                },

                // Step 5: Network
                function(args) {
                  if(diskOfferingObjs != null && diskOfferingObjs.length > 0) {
                    for(var i=0; i < diskOfferingObjs.length; i++) {
                      if(diskOfferingObjs[i].id == args.currentData.diskofferingid) {
                        selectedDiskOfferingObj = diskOfferingObjs[i];
                        break;
                      }
                    }
                  }

                  if (selectedZoneObj.securitygroupsenabled == false) {  //show network container                    
                    step5ContainerType = 'select-network';
                  }
                  else if (selectedZoneObj.securitygroupsenabled == true) {  // if security group is enabled  
                    var includingSecurityGroupService = false;
                    $.ajax({
                      url: createURL("listNetworks&trafficType=Guest&zoneId=" + selectedZoneObj.id),
                      dataType: "json",
                      async: false,
                      success: function(json) {                        
                        //basic zone should have only one guest network returned in this API call                        
                        var items = json.listnetworksresponse.network;
                        if(items != null && items.length > 0) {
                          var networkObj = items[0];                          
                          var serviceObjArray = networkObj.service;
                          for(var k = 0; k < serviceObjArray.length; k++) {                            
                            if(serviceObjArray[k].name == "SecurityGroup") {
                              includingSecurityGroupService = true;
                              break;
                            }
                          }                          
                        }                        
                      }
                    });
                                       
                    if(includingSecurityGroupService == false || selectedHypervisor == "VMware" || g_directAttachSecurityGroupsEnabled != "true") { 
                      step5ContainerType = 'nothing-to-select';
                    }
                    else {
                      step5ContainerType = 'select-security-group';    
                    }                      
                  }

                  //step5ContainerType = 'nothing-to-select'; //for testing only, comment it out before checking in
                  if(step5ContainerType == 'select-network') {
                    var defaultNetworkArray = [], optionalNetworkArray = [];
                    var networkData = {
                      zoneId: args.currentData.zoneid
                    };

                    if (!(cloudStack.context.projects && cloudStack.context.projects[0])) {
                      networkData.domainid = g_domainid;
                      networkData.account = g_account;
                    }
                    
                    $.ajax({
                      url: createURL('listNetworks'),
                      data: networkData,
                      dataType: "json",
                      async: false,
                      success: function(json) {
                        var networks = json.listnetworksresponse.network;

                        //***** check if there is an isolated network with sourceNAT (begin) *****
                        /*
                        var isolatedSourcenatNetwork = null;
                        if(selectedZoneObj.securitygroupsenabled == false) {
                          if (networks != null && networks.length > 0) {
                            for (var i = 0; i < networks.length; i++) {
                              if (networks[i].type == 'Isolated') {
                                //loop through
                                var sourceNatObj = ipFindNetworkServiceByName("SourceNat", networks[i]);
                                if(sourceNatObj != null) {
                                  isolatedSourcenatNetwork = networks[i];
                                  break;
                                }  
                              }
                            }
                          }
                          if (isolatedSourcenatNetwork == null) { //if there is no isolated network with sourceNat, create one.
                            $.ajax({
                              url: createURL("listNetworkOfferings&guestiptype=Isolated&supportedServices=sourceNat&state=Enabled"), //get the network offering for isolated network with sourceNat
                              dataType: "json",
                              async: false,
                              success: function(json) {
                                var networkOfferings = json.listnetworkofferingsresponse.networkoffering;
                                if (networkOfferings != null && networkOfferings.length > 0) {
                                  for (var i = 0; i < networkOfferings.length; i++) {
                                    if (networkOfferings[i].isdefault == true
                                        && (networkOfferings[i].availability == "Required" || networkOfferings[i].availability == "Optional")
                                       ) {
                                         // Create a isolated network
                                         var networkName = "Isolated Network";
                                         var networkDesc = "A dedicated isolated network for your account.  The broadcast domain is contained within a VLAN and all public network access is routed out by a virtual router.";
                                         $.ajax({
                                           url: createURL("createNetwork&networkOfferingId="+networkOfferings[i].id+"&name="+todb(networkName)+"&displayText="+todb(networkDesc)+"&zoneId="+args.currentData.zoneid),
                                           dataType: "json",
                                           async: false,
                                           success: function(json) {
                                             isolatedSourcenatNetwork = json.createnetworkresponse.network;
                                             defaultNetworkArray.push(isolatedSourcenatNetwork);
                                           }
                                         });
                                       }
                                  }
                                }
                              }
                            });
                          }                          
                        }
                        */
                        //***** check if there is an isolated network with sourceNAT (end) *****
                   

                        //***** populate all networks (begin) **********************************
                        //isolatedSourcenatNetwork is first radio button in default network section. Show isolatedSourcenatNetwork when its networkofferingavailability is 'Required' or'Optional'
                        /*
                        if (isolatedSourcenatNetwork.networkofferingavailability == 'Required' || isolatedSourcenatNetwork.networkofferingavailability == 'Optional') {
                          defaultNetworkArray.push(isolatedSourcenatNetwork);
                        }
                        */
                        
                        //default networks are in default network section 
                        //non-default networks are in additional network section                         
                        if (networks != null && networks.length > 0) {
                          for (var i = 0; i < networks.length; i++) {    
                            if (networks[i].isdefault) {     
                              defaultNetworkArray.push(networks[i]);                              
                            }
                            else {
                              optionalNetworkArray.push(networks[i]);
                            }
                          }
                        }
                        //***** populate all networks (end) ************************************
                      }
                    });                                      
                    
                    $.ajax({
                      url: createURL("listNetworkOfferings&guestiptype=Isolated&supportedServices=sourceNat&state=Enabled"), //get the network offering for isolated network with sourceNat
                      dataType: "json",
                      async: false,
                      success: function(json) {                        
                        networkOfferingObjs  = json.listnetworkofferingsresponse.networkoffering;                                             
                      }
                    });
                    
                    
                    args.response.success({
                      type: 'select-network',
                      data: {
                        myNetworks: defaultNetworkArray,
                        sharedNetworks: optionalNetworkArray,
                        securityGroups: [],
                        networkOfferings: networkOfferingObjs
                      }
                    });
                  }

                  else if(step5ContainerType == 'select-security-group') {
                    var securityGroupArray = [];
                    $.ajax({
                      url: createURL("listSecurityGroups"+"&domainid="+g_domainid+"&account="+g_account),
                      dataType: "json",
                      async: false,
                      success: function(json) {
                        var items = json.listsecuritygroupsresponse.securitygroup;
                        if (items != null && items.length > 0) {
                          for (var i = 0; i < items.length; i++) {
                            if(items[i].name != "default") //exclude default security group because it is always applied
                              securityGroupArray.push(items[i]);
                          }
                        }
                      }
                    });
                    args.response.success({
                      type: 'select-security-group',
                      data: {
                        defaultNetworks: [],
                        optionalNetworks: [],
                        securityGroups: securityGroupArray
                      }
                    });
                  }

                  else if(step5ContainerType == 'nothing-to-select') {
                    args.response.success({
                      type: 'nothing-to-select',
                      data: {
                        defaultNetworks: [],
                        optionalNetworks: [],
                        securityGroups: []                        
                      }
                    });
                  }

                },

                          // Step 6: Review
                          function(args) {
                            return false;
                          }
                          ],
              action: function(args) {
                /*
                 var isValid = true;
                 isValid &= validateString("Name", $thisPopup.find("#wizard_vm_name"), $thisPopup.find("#wizard_vm_name_errormsg"), true);   //optional
                 isValid &= validateString("Group", $thisPopup.find("#wizard_vm_group"), $thisPopup.find("#wizard_vm_group_errormsg"), true); //optional
                 if (!isValid)
                 return;
                 */

                // Create a new VM!!!!
                var array1 = [];

                //step 1 : select zone
                array1.push("&zoneId=" + args.data.zoneid);

                //step 2: select template
                array1.push("&templateId=" + args.data.templateid);
                array1.push("&hypervisor=" + selectedHypervisor);

                //step 3: select service offering
                array1.push("&serviceOfferingId=" + args.data.serviceofferingid);

                //step 4: select disk offering
                if(args.data.diskofferingid != null && args.data.diskofferingid != "0") {
                  array1.push("&diskOfferingId=" + args.data.diskofferingid);
                  if(selectedDiskOfferingObj.iscustomized == true)
                    array1.push("&size=" + args.data.size);
                }

                //step 5: select network      ???      
                if (step5ContainerType == 'select-network') {                
                  var array2 = [];                 
                  var defaultNetwork = args.data.defaultNetwork; //args.data.defaultNetwork might be equal to "new-network"                  
                  var checkedNetworks = args.data["my-networks"]; 
                
                  //create new network starts here                   
                  if(args.data["new-network"] == "create-new-network") {   
                    var networkName = "new Network";
                    $.ajax({
                      url: createURL("createNetwork&networkOfferingId="+args.data["new-network-networkofferingid"]+"&name="+todb(args.data["new-network-name"])+"&displayText="+todb(args.data["new-network-name"])+"&zoneId="+selectedZoneObj.id),
                      dataType: "json",
                      async: false,
                      success: function(json) {                      
                        newNetwork = json.createnetworkresponse.network;
                        checkedNetworks.push(newNetwork.id);
                        if(defaultNetwork == "new-network")
                          defaultNetwork = newNetwork.id;
                      }
                    });  
                  }                    
                  //create new network ends here
                  
                  //add default network first
                  if(defaultNetwork != null && defaultNetwork.length > 0)
                    array2.push(defaultNetwork);
                 
                  //then, add other checked networks                 
                  if(typeof(checkedNetworks) == "object" && checkedNetworks.length != null) { //checkedNetworks might be: (1) an array of string, e.g. ["203", "202"],
                    if(checkedNetworks != null && checkedNetworks.length > 0) {
                      for(var i=0; i < checkedNetworks.length; i++) {
                        if(checkedNetworks[i] != defaultNetwork) //exclude defaultNetwork that has been added to array2
                          array2.push(checkedNetworks[i]);
                      }
                    }
                  }                  
                  else if(typeof(checkedNetworks) == "string" && checkedNetworks.length > 0) { //checkedNetworks might be: (2) just an string, e.g. "202"
                    if(checkedNetworks != defaultNetwork) //exclude defaultNetwork that has been added to array2
                      array2.push(checkedNetworks);
                  }
                               
                  array1.push("&networkIds=" + array2.join(","));
                }
                else if (step5ContainerType == 'select-security-group') {
                  var securityGroupList;
                  var groups = args.data["security-groups"];
                  if(groups != null && groups.length > 0) {
                    for(var i=0; i < groups.length; i++) {
                      if(i == 0)
                        securityGroupList = groups[i];
                      else
                        securityGroupList += ("," + groups[i]);
                    }
                  }
                  if(securityGroupList != null)
                    array1.push("&securitygroupids=" + securityGroupList);
                }

                var displayname = args.data.displayname;
                if(displayname != null && displayname.length > 0)
                  array1.push("&displayname="+todb(displayname));

                var group = args.data.groupname;
                if (group != null && group.length > 0)
                  array1.push("&group="+todb(group));

                $.ajax({
                  url: createURL("deployVirtualMachine"+array1.join("")),
                  dataType: "json",
                  success: function(json) {
                    var jid = json.deployvirtualmachineresponse.jobid;
                    args.response.success(
                      {_custom:
                       {jobId: jid,
                        getUpdatedItem: function(json) {
                          return json.queryasyncjobresultresponse.jobresult.virtualmachine;
                        },
                        getActionFilter: function() {
                          return vmActionfilter;
                        }
                       }
                      }
                    );
                  },
                  error: function(XMLHttpResponse) {
                    //args.response.error(); //wait for Brian to implement
                    alert("Failed to deploy VM.");
                  }
                });
              }
            })
          },

          messages: {
            confirm: function(args) {  //never being called
              return 'Are you sure you want to deploy an instance?';
            },
            success: function(args) {  //never being called
              return args.name + ' is being created.';
            },
            notification: function(args) {
              //return 'Creating new VM: ' + args.name; //args.name is not available
              return 'Creating new VM';
            },
            complete: function(args) {  //never being called
              return args.name + ' has been created successfully!';
            }
          },
          notification: {
            poll: pollAsyncJobResult
          }
        },
        start: {
          label: 'Start instance' ,
          action: function(args) {
            $.ajax({
              url: createURL("startVirtualMachine&id=" + args.data.id),
              dataType: "json",
              async: true,
              success: function(json) {
                var jid = json.startvirtualmachineresponse.jobid;
                args.response.success(
                  {_custom:
                   {jobId: jid,
                    getUpdatedItem: function(json) {
                      return json.queryasyncjobresultresponse.jobresult.virtualmachine;
                    },
                    getActionFilter: function() {
                      return vmActionfilter;
                    }
                   }
                  }
                );
              }
            });
          },
          messages: {
            confirm: function(args) {
              return 'Are you sure you want to start ' + args.name + '?';
            },
            success: function(args) {
              return args.name + ' is being started.';
            },
            notification: function(args) {
              return 'Starting VM: ' + args.name;
            },
            complete: function(args) {
              return args.name + ' has been started.';
            }
          },
          notification: {
            poll: pollAsyncJobResult
          }
        },
        stop: {
          label: 'Stop instance',
          action: function(args) {
            $.ajax({
              url: createURL("stopVirtualMachine&id=" + args.data.id),
              dataType: "json",
              async: true,
              success: function(json) {
                var jid = json.stopvirtualmachineresponse.jobid;
                args.response.success(
                  {_custom:
                   {jobId: jid,
                    getUpdatedItem: function(json) {
                      return json.queryasyncjobresultresponse.jobresult.virtualmachine;
                    },
                    getActionFilter: function() {
                      return vmActionfilter;
                    }
                   }
                  }
                );
              }
            });
          },
          messages: {
            confirm: function(args) {
              return 'Are you sure you want to stop ' + args.name + '?';
            },
            success: function(args) {
              return args.name + ' is being stopped.';
            },
            notification: function(args) {
              return 'Stopping VM: ' + args.name;
            },
            complete: function(args) {
              return args.name + ' has been stopped.';
            }
          },
          notification: {
            //poll: testData.notifications.testPoll
            poll: pollAsyncJobResult
          }
        },
        restart: {
          label: 'Reboot instance',
          action: function(args) {
            $.ajax({
              url: createURL("rebootVirtualMachine&id=" + args.data.id),
              dataType: "json",
              async: true,
              success: function(json) {
                var jid = json.rebootvirtualmachineresponse.jobid;
                args.response.success(
                  {_custom:
                   {jobId: jid,
                    getUpdatedItem: function(json) {
                      return json.queryasyncjobresultresponse.jobresult.virtualmachine;
                    },
                    getActionFilter: function() {
                      return vmActionfilter;
                    }
                   }
                  }
                );
              }
            });
          },
          messages: {
            confirm: function(args) {
              return 'Are you sure you want to reboot ' + args.name + '?';
            },
            success: function(args) {
              return args.name + ' is being rebooted.';
            },
            notification: function(args) {
              return 'Rebooting VM: ' + args.name;
            },
            complete: function(args) {
              return args.name + ' has been rebooted successfully.';
            }
          },
          notification: {
            poll: pollAsyncJobResult
          }
        },
        destroy: {
          label: 'Destroy instance',
          messages: {
            confirm: function(args) {
              return 'Are you sure you want to destroy ' + args.name + '?';
            },
            success: function(args) {
              return args.name + ' is being destroyed.';
            },
            notification: function(args) {
              return 'Destroying VM: ' + args.name;
            },
            complete: function(args) {
              return args.name + ' has been destroyed.';
            }
          },
          action: function(args) {
            $.ajax({
              url: createURL("destroyVirtualMachine&id=" + args.data.id),
              dataType: "json",
              async: true,
              success: function(json) {
                var jid = json.destroyvirtualmachineresponse.jobid;
                args.response.success(
                  {_custom:
                   {jobId: jid,
                    getUpdatedItem: function(json) {
                      return json.queryasyncjobresultresponse.jobresult.virtualmachine;
                    },
                    getActionFilter: function() {
                      return vmActionfilter;
                    }
                   }
                  }
                );
              }
            });
          },
          notification: {
            poll: pollAsyncJobResult
          }
        },
        restore: {
          label: 'Restore instance',
          messages: {
            confirm: function(args) {
              return 'Are you sure you want to restore ' + args.name + '?';
            },
            success: function(args) {
              return args.name + ' is being restored.';
            },
            notification: function(args) {
              return 'Restoring VM: ' + args.name;
            },
            complete: function(args) {
              return args.name + ' has been restored.';
            }
          },
          action: function(args) {
            $.ajax({
              url: createURL("recoverVirtualMachine&id=" + args.data.id),
              dataType: "json",
              async: true,
              success: function(json) {
                var item = json.recovervirtualmachineresponse.virtualmachine;
                args.response.success({data:item});
              }
            });
          },
          notification: {
            poll: function(args) {
              args.complete();
            }
          }
        }
      },

      dataProvider: function(args) {
        var array1 = [];  
        if(args.filterBy != null) {
          if(args.filterBy.kind != null) {
            switch(args.filterBy.kind) {
            case "all":
              break;            
            case "mine":
              array1.push("&domainid=" + g_domainid + "&account=" + g_account);
              break;
            case "running":
              array1.push("&state=Running");
              break;
            case "stopped":
              array1.push("&state=Stopped");
              break;
            case "destroyed":
              array1.push("&state=Destroyed");
              break;
            }
          }
          if(args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
            switch(args.filterBy.search.by) {
            case "name":
              array1.push("&keyword=" + args.filterBy.search.value);
              break;
            }
          }
        }

        $.ajax({
          url: createURL("listVirtualMachines&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
          dataType: "json",
          async: true,
          success: function(json) {
            var items = json.listvirtualmachinesresponse.virtualmachine;

            args.response.success({
              actionFilter: vmActionfilter,
              data: items
            });
          }
        });
      },

      detailView: {
        name: 'Instance details',
        viewAll: { path: 'storage.volumes', label: 'Volumes' },
        tabFilter: function(args) {
          var hiddenTabs = [];                      
          if(g_directAttachSecurityGroupsEnabled != "true") 
             hiddenTabs.push("securityGroups");	         
          return hiddenTabs;
        },               
        actions: {
          start: {
            label: 'Start instance' ,
            action: function(args) {
              $.ajax({
                url: createURL("startVirtualMachine&id=" + args.data.id),
                dataType: "json",
                async: true,
                success: function(json) {
                  var jid = json.startvirtualmachineresponse.jobid;
                  args.response.success(
                    {_custom:
                     {jobId: jid,
                      getUpdatedItem: function(json) {
                        return json.queryasyncjobresultresponse.jobresult.virtualmachine;
                      },
                      getActionFilter: function() {
                        return vmActionfilter;
                      }
                     }
                    }
                  );
                }
              });
            },
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to start ' + args.name + '?';
              },
              success: function(args) {
                return args.name + ' is being started.';
              },
              notification: function(args) {
                return 'Starting VM: ' + args.name;
              },
              complete: function(args) {
                return args.name + ' has been started.';
              }
            },
            notification: {
              poll: pollAsyncJobResult
            }
          },
          stop: {
            label: 'Stop instance',
            action: function(args) {
              $.ajax({
                url: createURL("stopVirtualMachine&id=" + args.data.id),
                dataType: "json",
                async: true,
                success: function(json) {
                  var jid = json.stopvirtualmachineresponse.jobid;
                  args.response.success(
                    {_custom:
                     {jobId: jid,
                      getUpdatedItem: function(json) {
                        return json.queryasyncjobresultresponse.jobresult.virtualmachine;
                      },
                      getActionFilter: function() {
                        return vmActionfilter;
                      }
                     }
                    }
                  );
                }
              });
            },
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to stop ' + args.name + '?';
              },
              success: function(args) {
                return args.name + ' is being stopped.';
              },
              notification: function(args) {
                return 'Stopping VM: ' + args.name;
              },
              complete: function(args) {
                return args.name + ' has been stopped.';
              }
            },
            notification: {
              poll: pollAsyncJobResult
            }
          },
          restart: {
            label: 'Reboot instance',
            action: function(args) {
              $.ajax({
                url: createURL("rebootVirtualMachine&id=" + args.data.id),
                dataType: "json",
                async: true,
                success: function(json) {
                  var jid = json.rebootvirtualmachineresponse.jobid;
                  args.response.success(
                    {_custom:
                     {jobId: jid,
                      getUpdatedItem: function(json) {
                        return json.queryasyncjobresultresponse.jobresult.virtualmachine;
                      },
                      getActionFilter: function() {
                        return vmActionfilter;
                      }
                     }
                    }
                  );
                }
              });
            },
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to reboot ' + args.name + '?';
              },
              success: function(args) {
                return args.name + ' is being rebooted.';
              },
              notification: function(args) {
                return 'Rebooting VM: ' + args.name;
              },
              complete: function(args) {
                return args.name + ' has been rebooted successfully.';
              }
            },
            notification: {
              poll: pollAsyncJobResult
            }
          },
          destroy: {
            label: 'Destroy instance',
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to destroy ' + args.name + '?';
              },
              success: function(args) {
                return args.name + ' is being destroyed.';
              },
              notification: function(args) {
                return 'Destroying VM: ' + args.name;
              },
              complete: function(args) {
                return args.name + ' has been destroyed.';
              }
            },
            action: function(args) {
              $.ajax({
                url: createURL("destroyVirtualMachine&id=" + args.data.id),
                dataType: "json",
                async: true,
                success: function(json) {
                  var jid = json.destroyvirtualmachineresponse.jobid;
                  args.response.success(
                    {_custom:
                     {jobId: jid,
                      getUpdatedItem: function(json) {
                        return json.queryasyncjobresultresponse.jobresult.virtualmachine;
                      },
                      getActionFilter: function() {
                        return vmActionfilter;
                      }
                     }
                    }
                  );
                }
              });
            },
            notification: {
              poll: pollAsyncJobResult
            }
          },
          restore: {
            label: 'Restore instance',
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to restore ' + args.name + '?';
              },
              success: function(args) {
                return args.name + ' is being restored.';
              },
              notification: function(args) {
                return 'Restoring VM: ' + args.name;
              },
              complete: function(args) {
                return args.name + ' has been restored.';
              }
            },
            action: function(args) {
              $.ajax({
                url: createURL("recoverVirtualMachine&id=" + args.data.id),
                dataType: "json",
                async: true,
                success: function(json) {
                  var item = json.recovervirtualmachineresponse.virtualmachine;
                  args.response.success({data:item});
                }
              });
            },
            notification: {
              poll: function(args) {
                args.complete();
              }
            }
          },

          edit: {
            label: 'Edit',
            action: function(args) {
              var array1 = [];
              array1.push("&displayName=" + args.data.displayname);
              array1.push("&group=" + args.data.group);
              array1.push("&ostypeid=" + args.data.guestosid);
              //array1.push("&haenable=" + haenable);

              $.ajax({
                url: createURL("updateVirtualMachine&id=" + args.context.instances[0].id + array1.join("")),
                dataType: "json",
                success: function(json) {
                  var item = json.updatevirtualmachineresponse.virtualmachine;
                  args.response.success({data:item});
                }
              });
            }
          },

          attachISO: {
            label: 'Attach ISO',
            createForm: {
              title: 'Attach ISO',
              desc: 'Attach ISO to instance',
              fields: {
                iso: {
                  label: 'ISO',
                  select: function(args) {
                    $.ajax({
                      url: createURL("listIsos&isReady=true&isofilter=executable"),
                      dataType: "json",
                      async: true,
                      success: function(json) {
                        var isos = json.listisosresponse.iso;
                        var items = [];
                        $(isos).each(function() {
                          items.push({id: this.id, description: this.displaytext});
                        });
                        args.response.success({data: items});
                      }
                    });
                  }
                }
              }
            },
            action: function(args) {
              $.ajax({
                url: createURL("attachIso&virtualmachineid=" + args.context.instances[0].id + "&id=" + args.data.iso),
                dataType: "json",
                async: true,
                success: function(json) {
                  var jid = json.attachisoresponse.jobid;
                  args.response.success(
                    {_custom:
                     {jobId: jid,
                      getUpdatedItem: function(json) {
                        return json.queryasyncjobresultresponse.jobresult.virtualmachine;
                      },
                      getActionFilter: function() {
                        return vmActionfilter;
                      }
                     }
                    }
                  );
                }
              });
            },
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to attach ISO to instance ' + args.name + '?';
              },
              success: function(args) {
                return 'ISO is being attached to instance ' + args.name;
              },
              notification: function(args) {
                return 'Attaching ISO to instance ' + args.name;
              },
              complete: function(args) {
                return 'ISO has been attached to instance ' + args.name;
              }
            },
            notification: {
              poll: pollAsyncJobResult
            }
          },

          detachISO: {
            label: 'Detach instance',
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to detach ISO ?';
              },
              success: function(args) {
                return 'ISO is being detached.';
              },
              notification: function(args) {
                return 'Detaching ISO';
              },
              complete: function(args) {
                return 'ISO has been detached.';
              }
            },
            action: function(args) {
              $.ajax({
                url: createURL("detachIso&virtualmachineid=" + args.context.instances[0].id),
                dataType: "json",
                async: true,
                success: function(json) {
                  var jid = json.detachisoresponse.jobid;
                  args.response.success(
                    {_custom:
                     {jobId: jid,
                      getUpdatedItem: function(json) {
                        return json.queryasyncjobresultresponse.jobresult.virtualmachine;
                      },
                      getActionFilter: function() {
                        return vmActionfilter;
                      }
                     }
                    }
                  );
                }
              });
            },
            notification: {
              poll: pollAsyncJobResult
            }
          },

          resetPassword: {
            label: 'Reset password',
            messages: {
              confirm: function(args) {
                /*
                 if (jsonObj.passwordenabled == false) {
                 $("#dialog_info")
                 .text(dictionary["message.action.reset.password.off"])
                 .dialog('option', 'buttons', {
                 "OK": function() {
                 $(this).dialog("close");
                 }
                 }).dialog("open");
                 return;
                 } else if (jsonObj.state != 'Stopped') {
                 $("#dialog_info")
                 .text(dictionary["message.action.reset.password.warning"])
                 .dialog('option', 'buttons', {
                 "OK": function() {
                 $(this).dialog("close");
                 }
                 }).dialog("open");
                 return;
                 }
                 */
                return 'Are you sure you want to reset password?';
              },
              success: function(args) {
                return 'Password is being reset.';
              },
              notification: function(args) {
                return 'Resetting password';
              },
              complete: function(args) {
                return 'Password has been reset to ' + args.password;
              }
            },
            action: function(args) {
              $.ajax({
                url: createURL("resetPasswordForVirtualMachine&id=" + args.data.id),
                dataType: "json",
                async: true,
                success: function(json) {
                  var jid = json.resetpasswordforvirtualmachineresponse.jobid;
                  args.response.success(
                    {_custom:
                     {jobId: jid,
                      getUpdatedItem: function(json) {
                        return json.queryasyncjobresultresponse.jobresult.virtualmachine;
                      },
                      getActionFilter: function() {
                        return vmActionfilter;
                      }
                     }
                    }
                  );
                }
              });
            },
            notification: {
              poll: pollAsyncJobResult
            }
          },

          changeService: {
            label: 'Change service offering',
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to change service offering?';
              },
              success: function(args) {
                return 'Service offering is being changed.';
              },
              notification: function(args) {
                return 'Changing service offering';
              },
              complete: function(args) {
                return 'Service offering has been changed.';
              }
            },
            createForm: {
              title: 'Change Service Offering',
              desc: '',
              fields: {
                serviceOffering: {
                  label: 'Service offering',
                  select: function(args) {
                    $.ajax({
                      url: createURL("listServiceOfferings&VirtualMachineId=" + args.context.instances[0].id),
                      dataType: "json",
                      async: true,
                      success: function(json) {
                        var serviceofferings = json.listserviceofferingsresponse.serviceoffering;
                        var items = [];
                        $(serviceofferings).each(function() {
                          items.push({id: this.id, description: this.displaytext});
                        });
                        args.response.success({data: items});
                      }
                    });
                  }
                }
              }
            },
            action: function(args) {
              $.ajax({
                url: createURL("changeServiceForVirtualMachine&id=" + args.context.instances[0].id + "&serviceOfferingId=" + args.data.serviceOffering),
                dataType: "json",
                async: true,
                success: function(json) {
                  var jsonObj = json.changeserviceforvirtualmachineresponse.virtualmachine;
                  args.response.success({data: jsonObj});
                }
              });
            },
            notification: {
              poll: function(args) {
                args.complete();
              }
            }
          },

          createTemplate: {
            label: 'Create template',
            messages: {
              confirm: function(args) {
                /*
                 if (getUserPublicTemplateEnabled() == "true" || isAdmin()) {
                 $dialogCreateTemplate.find("#create_template_public_container").show();
                 }
                 */
                return 'Are you sure you want to create template?';
              },
              success: function(args) {
                return 'Template is being created.';
              },
              notification: function(args) {
                return 'Creating template';
              },
              complete: function(args) {
                return 'Template has been created.';
              }
            },
            createForm: {
              title: 'Create Template',
              desc: '',
              preFilter: cloudStack.preFilter.createTemplate,
              fields: {
                name: { label: 'Name', validation: { required: true }},
                displayText: { label: 'Description', validation: { required: true }},
                osTypeId: {
                  label: 'OS Type',
                  select: function(args) {
                    $.ajax({
                      url: createURL("listOsTypes"),
                      dataType: "json",
                      async: true,
                      success: function(json) {
                        var ostypes = json.listostypesresponse.ostype;
                        var items = [];
                        $(ostypes).each(function() {
                          items.push({id: this.id, description: this.description});
                        });
                        args.response.success({data: items});
                      }
                    });
                  }
                },
                isPublic: { label: 'Public', isBoolean: true },
                url: { label: 'Image directory', validation: { required: true } }
              }
            },
            action: function(args) {
              /*
               var isValid = true;
               isValid &= validateString("Name", $thisDialog.find("#create_template_name"), $thisDialog.find("#create_template_name_errormsg"));
               isValid &= validateString("Display Text", $thisDialog.find("#create_template_desc"), $thisDialog.find("#create_template_desc_errormsg"));
               isValid &= validateString("Image Directory", $thisDialog.find("#image_directory"), $thisDialog.find("#image_directory_errormsg"), false); //image directory is required when creating template from VM whose hypervisor is BareMetal
               if (!isValid)
               return;
               $thisDialog.dialog("close");
               */

              var array1 = [];
              array1.push("&name=" + todb(args.data.name));
              array1.push("&displayText=" + todb(args.data.displayText));
              array1.push("&osTypeId=" + args.data.osTypeId);

              //array1.push("&isPublic=" + args.data.isPublic);
              array1.push("&isPublic=" + (args.data.isPublic=="on"));  //temporary, before Brian fixes it.

              array1.push("&url=" + todb(args.data.url));

              $.ajax({
                url: createURL("createTemplate&virtualmachineid=" + args.context.instances[0].id + array1.join("")),
                dataType: "json",
                async: true,
                success: function(json) {
                  var jid = json.createtemplateresponse.jobid;
                  args.response.success(
                    {_custom:
                     {jobId: jid,
                      getUpdatedItem: function(json) {
                        return {}; //no properties in this VM needs to be updated
                      },
                      getActionFilter: function() {
                        return vmActionfilter;
                      }
                     }
                    }
                  );
                }
              });
            },
            notification: {
              poll: pollAsyncJobResult
            }
          },          

          migrate: {
            label: 'Migrate instance',
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to migrate instance?';
              },
              success: function(args) {
                return 'Instance is being migrated.';
              },
              notification: function(args) {
                return 'Migrating instance';
              },
              complete: function(args) {
                return 'Instance has been migrated.';
              }
            },
            createForm: {
              title: 'Migrate instance',
              desc: '',
              fields: {
                hostId: {
                  label: 'Host',
                  validation: { required: true },
                  select: function(args) {
                    $.ajax({
                      url: createURL("listHosts&VirtualMachineId=" + args.context.instances[0].id),
                      //url: createURL("listHosts"),	//for testing only, comment it out before checking in.
                      dataType: "json",
                      async: true,
                      success: function(json) {
                        var hosts = json.listhostsresponse.host;
                        var items = [];
                        $(hosts).each(function() {
                          items.push({id: this.id, description: this.name});
                        });
                        args.response.success({data: items});
                      }
                    });
                  }
                }
              }
            },
            action: function(args) {
              $.ajax({
                url: createURL("migrateVirtualMachine&hostid=" + args.data.hostId + "&virtualmachineid=" + args.context.instances[0].id),
                dataType: "json",
                async: true,
                success: function(json) {
                  var jid = json.migratevirtualmachineresponse.jobid;
                  args.response.success(
                    {_custom:
                     {jobId: jid,
                      getUpdatedItem: function(json) {
                        return json.queryasyncjobresultresponse.jobresult.virtualmachine;
                        /*
                         var vmObj;
                         $.ajax({
                         url: createURL("listVirtualMachines&id=" + args.context.instances[0].id),
                         dataType: "json",
                         async: false,
                         success: function(json) {
                         var items =  json.listvirtualmachinesresponse.virtualmachine;
                         if(items != null && items.length > 0) {
                         vmObj = items[0];
                         }
                         }
                         });
                         return vmObj;
                         */
                      },
                      getActionFilter: function() {
                        return vmActionfilter;
                      }
                     }
                    }
                  );
                }
              });
            },
            notification: {
              poll: pollAsyncJobResult
            }
          },          
          
          viewConsole: {
            label: 'View console',
            action: {
              externalLink: {
                url: function(args) {                
                  return clientConsoleUrl + '?cmd=access&vm=' + args.context.instances[0].id;                  
                },
                title: function(args) {                                 
                  return getVmName(args.context.instances[0].name, args.context.instances[0].displayname) + ' console';                  
                },
                width: 820,
                height: 640
              }
            }
          } 
        },
        tabs: {
          // Details tab
          details: {
            title: 'Details',
            fields: [
              {
                name: { label: 'Name', isEditable: false }
              },
              {
                id: { label: 'ID', isEditable: false },
                displayname: { label: 'Display Name', isEditable: true },
                state: { label: 'State', isEditable: false },
                zonename: { label: 'Zone', isEditable: false },
                hypervisor: { label: 'Hypervisor', isEditable: false },
                templatename: { label: 'Template', isEditable: false },
                templateid: {
                  label: 'Template type',
                  isEditable: false
                  /*
                   ,
                   select: function(args) {
                   var items = [];

                   $(testData.data.templates).each(function() {
                   items.push({ id: this.id, description: this.name });
                   });

                   args.response.success({ data: items });
                   }
                   */
                },
                guestosid: {
                  label: 'OS Type',
                  isEditable: true,
                  select: function(args) {
                    $.ajax({
                      url: createURL("listOsTypes"),
                      dataType: "json",
                      async: true,
                      success: function(json) {
                        var ostypes = json.listostypesresponse.ostype;
                        var items = [];
                        $(ostypes).each(function() {
                          items.push({id: this.id, description: this.description});
                        });
                        args.response.success({data: items});
                      }
                    });
                  }
                },

                serviceofferingname: { label: 'Service offering', isEditable: false },
                group: { label: 'Group', isEditable: true },
                hostname: { label: 'Host', isEditable: false},
                haenable: { label: 'HA Enable', isEditable: false, converter:cloudStack.converters.toBooleanText },
                isoid: {
                  label: 'Attached ISO',
                  isEditable: false,
                  converter: function(isoid) {
                    return cloudStack.converters.toBooleanText(isoid != null);
                  }
                },
                domain: { label: 'Domain', isEditable: false },
                account: { label: 'Account', isEditable: false },
                created: { label: 'Created', isEditable: false }
              }
            ],

            dataProvider: function(args) {
              args.response.success(
                {
                  actionFilter: vmActionfilter,
                  data: args.context.instances[0]
                }
              );
            }
          },

          /**
           * NICs tab
           */
          nics: {
            title: 'NICs',
            multiple: true,
            fields: [
              {
                id: { label: 'ID' },
                ipaddress: { label: 'IP Address' },
                type: { label: 'Type' },
                gateway: { label: 'Default gateway' },
                netmask: { label: 'Netmask' }
              }
            ],
            dataProvider: function(args) {              
              args.response.success({data: args.context.instances[0].nic});
            }
          },
                    
           /**
           * Security Groups tab
           */
          securityGroups: {
            title: 'Security groups',
            multiple: true,
            fields: [
              {
                id: { label: 'ID' },
                name: { label: 'Name' },
                description: { label: 'Description' }
              }
            ],
            dataProvider: function(args) {              
              args.response.success({data: args.context.instances[0].securitygroup});
            }
          },

          /**
           * Statistics tab
           */
          stats: {
            title: 'Statistics',
            fields: {
              totalCPU: { label: 'Total CPU' },
              cpuused: { label: 'CPU Utilized' },
              networkkbsread: { label: 'Network Read' },
              networkkbswrite: { label: 'Network Write' }
            },
            //dataProvider: testData.dataProvider.detailView('instances')
            dataProvider: function(args) {
              var jsonObj = args.context.instances[0];                          
              args.response.success({
                data: {
                  totalCPU: fromdb(jsonObj.cpunumber) + " x " + cloudStack.converters.convertHz(jsonObj.cpuspeed),
                  cpuused: jsonObj.cpuused,
                  networkkbsread: (jsonObj.networkkbsread == null || jsonObj.networkkbsread == 0)? "N/A": cloudStack.converters.convertBytes(jsonObj.networkkbsread * 1024),
                  networkkbswrite: (jsonObj.networkkbswrite == null || jsonObj.networkkbswrite == 0)? "N/A": cloudStack.converters.convertBytes(jsonObj.networkkbswrite * 1024)
                }
              });
            }
          }
        }
      }
    }
  };

  var vmActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];

    if (jsonObj.state == 'Destroyed') {
      if(isAdmin() || isDomainAdmin()) {
        allowedActions.push("restore");
      }
    }
    else if (jsonObj.state == 'Running') {
      allowedActions.push("edit");
      allowedActions.push("stop");
      allowedActions.push("restart");
      allowedActions.push("destroy");
      if (isAdmin()
          && (jsonObj.rootdevicetype == 'NetworkFilesystem' || jsonObj.rootdevicetype == 'IscsiLUN' || jsonObj.rootdevicetype == 'PreSetup'  || jsonObj.rootdevicetype == 'OCFS2')
          //&& (jsonObj.hypervisor == 'XenServer' || jsonObj.hypervisor == 'VMware')
          )
      {
        allowedActions.push("migrate");
      }

      if (jsonObj.isoid == null)	{
        allowedActions.push("attachISO");
      }
      else {
        allowedActions.push("detachISO");
      }

      allowedActions.push("resetPassword");
      allowedActions.push("changeService");

      if(jsonObj.hypervisor == "BareMetal") {
        allowedActions.push("createTemplate");
      }
      
      allowedActions.push("viewConsole");
    }
    else if (jsonObj.state == 'Stopped') {
      allowedActions.push("edit");
      allowedActions.push("start");
      allowedActions.push("destroy");
      if (jsonObj.isoid == null)	{
        allowedActions.push("attachISO");
      }
      else {
        allowedActions.push("detachISO");
      }
      allowedActions.push("resetPassword");
      allowedActions.push("changeService");
      if(jsonObj.hypervisor == "BareMetal") {
        allowedActions.push("createTemplate");
      }
    }
    else if (jsonObj.state == 'Starting') {
      allowedActions.push("stop");
    }
    else if (jsonObj.state == 'Error') {
      allowedActions.push("destroy");
    }
    return allowedActions;
  }

})(jQuery, cloudStack, testData);
