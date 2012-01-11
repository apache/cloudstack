(function($, cloudStack, testData) {

  var zoneObjs, podObjs, clusterObjs, domainObjs, networkOfferingObjs;
  var selectedClusterObj, selectedZoneObj, selectedPublicNetworkObj, selectedManagementNetworkObj, selectedPhysicalNetworkObj, selectedGuestNetworkObj;
  var naasStatusMap = {};
  var nspMap = {};

  function virtualRouterProviderActionFilter(args) {
    var allowedActions = [];
    var jsonObj = nspMap["virtualRouter"];
    if(jsonObj.state == "Enabled")
      allowedActions.push("disable");
    else if(jsonObj.state == "Disabled")
      allowedActions.push("enable");
    return allowedActions;
  };

  cloudStack.sections.system = {
    title: 'System',
    id: 'system',

    // System dashboard
    dashboard: {
      dataProvider: function(args) {
        var dataFns = {
          zoneCount: function(data) {
            $.ajax({
              url: createURL('listZones'),
              success: function(json) {
                dataFns.podCount($.extend(data, {
                  zoneCount: json.listzonesresponse.count ?
                    json.listzonesresponse.count : 0
                }));
              }
            });
          },

          podCount: function(data) {
            $.ajax({
              url: createURL('listPods'),
              success: function(json) {
                dataFns.clusterCount($.extend(data, {
                  podCount: json.listpodsresponse.count ?
                    json.listpodsresponse.count : 0
                }));
              }
            });
          },

          clusterCount: function(data) {
            $.ajax({
              url: createURL('listClusters'),
              success: function(json) {
                dataFns.hostCount($.extend(data, {
                  clusterCount: json.listclustersresponse.count ?
                    json.listclustersresponse.count : 0
                }));
              }
            });
          },

          hostCount: function(data) {
            $.ajax({
              url: createURL('listHosts'),
              data: {
                type: 'routing'
              },
              success: function(json) {
                dataFns.capacity($.extend(data, {
                  hostCount: json.listhostsresponse.count ?
                    json.listhostsresponse.count : 0
                }));
              }
            });
          },

          capacity: function(data) {
            if (data.zoneCount) {
              $.ajax({
                url: createURL('listCapacity'),
                success: function(json) {
                  var capacities = json.listcapacityresponse.capacity;

                  var capacityTotal = function(id, converter) {
                    var capacity = $.grep(capacities, function(capacity) {
                      return capacity.type == id;
                    })[0];

                    var total = capacity ? capacity.capacitytotal : 0;

                    if (converter) {
                      return converter(total);
                    }

                    return total;
                  };

                  complete($.extend(data, {
                    cpuCapacityTotal: capacityTotal(1, cloudStack.converters.convertHz),
                    memCapacityTotal: capacityTotal(0, cloudStack.converters.convertBytes),
                    storageCapacityTotal: capacityTotal(2, cloudStack.converters.convertBytes)
                  }));
                }
              });
            } else {
              complete($.extend(data, {
                cpuCapacityTotal: cloudStack.converters.convertHz(0),
                memCapacityTotal: cloudStack.converters.convertBytes(0),
                storageCapacityTotal: cloudStack.converters.convertBytes(0)
              }));
            }
          }
        };

        var complete = function(data) {
          args.response.success({
            data: data
          });
        };

        dataFns.zoneCount({});
      }
    },

    // Network-as-a-service configuration
    naas: {
      mainNetworksPreFilter: function(args) {
        if (args.context.physicalResources[0].networktype == 'Basic') {
          return ['public'];
        }

        return [];
      },

      mainNetworks: {
        'public': {
          detailView: {
            actions: {},
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    traffictype: { label: 'Traffic type' },
                    broadcastdomaintype: { label: 'Broadcast domain type' }
                  }
                ],

                dataProvider: function(args) {
                  $.ajax({
                    url: createURL("listNetworks&listAll=true&trafficType=Public&isSystem=true&zoneId="+selectedZoneObj.id),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      var items = json.listnetworksresponse.network;
                      selectedPublicNetworkObj = items[0];
                      args.response.success({data: selectedPublicNetworkObj});
                    }
                  });
                }
              },

              ipAddresses: {
                title: 'IP Ranges',
                custom: function(args) {
                  return $('<div></div>').multiEdit({
                    context: args.context,
                    noSelect: true,
                    fields: {
                      'gateway': { edit: true, label: 'Gateway' },
                      'netmask': { edit: true, label: 'Netmask' },
                      'vlan': { edit: true, label: 'VLAN', isOptional: true },
                      'startip': { edit: true, label: 'Start IP' },
                      'endip': { edit: true, label: 'End IP' },
                      'add-rule': { label: 'Add', addButton: true }
                    },
                    add: {
                      label: 'Add',
                      action: function(args) {
                        var array1 = [];
                        array1.push("&zoneId=" + args.context.zones[0].id);

                        if (args.data.vlan != null && args.data.vlan.length > 0)
                          array1.push("&vlan=" + todb(args.data.vlan));
                        else
                          array1.push("&vlan=untagged");

                        array1.push("&gateway=" + args.data.gateway);
                        array1.push("&netmask=" + args.data.netmask);
                        array1.push("&startip=" + args.data.startip);
                        if(args.data.endip != null && args.data.endip.length > 0)
                          array1.push("&endip=" + args.data.endip);

                        if(args.context.zones[0].securitygroupsenabled == false)
                          array1.push("&forVirtualNetwork=true");
                        else
                          array1.push("&forVirtualNetwork=false");

                        $.ajax({
                          url: createURL("createVlanIpRange" + array1.join("")),
                          dataType: "json",
                          success: function(json) {
                            var item = json.createvlaniprangeresponse.vlan;
                            args.response.success({
                              data: item,
                              notification: {
                                label: 'IP range is added',
                                poll: function(args) {
                                  args.complete();
                                }
                              }
                            });
                          },
                          error: function(XMLHttpResponse) {
                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                            args.response.error(errorMsg);
                          }
                        });
                      }
                    },
                    actions: {
                      destroy: {
                        label: 'Delete',
                        action: function(args) {
                          $.ajax({
                            url: createURL('deleteVlanIpRange&id=' + args.context.multiRule[0].id),
                            dataType: 'json',
                            async: true,
                            success: function(json) {
                              args.response.success({
                                notification: {
                                  label: 'Remove IP range ' + args.context.multiRule[0].id,
                                  poll: function(args) {
                                    args.complete();
                                  }
                                }
                              });
                            }
                          });
                        }
                      }
                    },
                    dataProvider: function(args) {
                      $.ajax({
                        url: createURL("listVlanIpRanges&zoneid=" + args.context.zones[0].id + "&networkId=" + selectedPublicNetworkObj.id),
                        dataType: "json",
                        success: function(json) {
                          var items = json.listvlaniprangesresponse.vlaniprange;
                          args.response.success({data: items});
                        }
                      });
                    }
                  });
                }
              }
            }
          }
        },

        'management': {
          detailView: {  
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    traffictype: { label: 'Traffic type' },
                    broadcastdomaintype: { label: 'Broadcast domain type' }
                  }
                ],
                dataProvider: function(args) {                  
                  $.ajax({
                    url: createURL("listNetworks&listAll=true&issystem=true&trafficType=Management&zoneId=" + selectedZoneObj.id),
                    dataType: "json",
                    success: function(json) {                      
                      selectedManagementNetworkObj =json.listnetworksresponse.network[0];
                      args.response.success({ data: selectedManagementNetworkObj });                      
                    }
                  });                
                }
              },
              ipAddresses: { //read-only listView (no actions) filled with pod info (not VlanIpRange info)
                title: 'IP Ranges',               
								listView: {
									fields: {
										name: { label: 'Pod name' },
										gateway: { label: 'Gateway' },  //'Reserved system gateway' is too long and causes a visual format bug (2 lines overlay)
										netmask: { label: 'Netmask' },  //'Reserved system netmask' is too long and causes a visual format bug (2 lines overlay)
										startip: { label: 'Start IP' }, //'Reserved system start IP' is too long and causes a visual format bug (2 lines overlay)
										endip: { label: 'End IP' }      //'Reserved system end IP' is too long and causes a visual format bug (2 lines overlay)
									},
									dataProvider: function(args) {	                    
										var array1 = [];  
										if(args.filterBy != null) {          
											if(args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
												switch(args.filterBy.search.by) {
												case "name":
													if(args.filterBy.search.value.length > 0)
														array1.push("&keyword=" + args.filterBy.search.value);
													break;
												}
											}
										}
										$.ajax({
											url: createURL("listPods&zoneid=" + selectedZoneObj.id + "&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),  
											dataType: "json",
											async: true,
											success: function(json) {												
												var items = json.listpodsresponse.pod;
												args.response.success({ data:items });
											}
										});
									}
								}							
              }
            }
          }
        },

        'guest': {
          detailView: {
            actions: {
              edit: {
                label: 'Edit',
                action: function(args) {
                  var vlan;
                  if(args.data.endVlan == null || args.data.endVlan.length == 0)
                    vlan = args.data.startVlan;
                  else
                    vlan = args.data.startVlan + "-" + args.data.endVlan;
                  $.ajax({
                    url: createURL("updatePhysicalNetwork&id=" + selectedPhysicalNetworkObj.id + "&vlan=" + todb(vlan)),
                    dataType: "json",
                    success: function(json) {
                      var jobId = json.updatephysicalnetworkresponse.jobid;
                      args.response.success({ _custom: { jobId: jobId }});
                    }
                  });
                },
                notification: { poll: pollAsyncJobResult }
              }
            },

            tabFilter: function(args) {
              var hiddenTabs = [];       
              if (selectedZoneObj.networktype == 'Basic') 
                hiddenTabs.push("network");
              else //selectedZoneObj.networktype == 'Advanced'
                hiddenTabs.push("ipAddresses");                
              return hiddenTabs;
            },
            
            tabs: {
              details: {
                title: 'Details',
                preFilter: function(args) {                  
                  var hiddenFields = [];
                  if(selectedZoneObj.networktype == "Basic") {
                    hiddenFields.push("startVlan");
                    hiddenFields.push("endVlan");
                  }
                  return hiddenFields;
                },
                fields: [                  
                  {                    
                    state: { label: 'State' },
                    startVlan: {
                      label: 'Start Vlan',
                      isEditable: true
                    },
                    endVlan: {
                      label: 'End Vlan',
                      isEditable: true
                    },
                    broadcastdomainrange: { label: 'Broadcast domain range' }                    
                  }
                ],
                dataProvider: function(args) {                  
                  var startVlan, endVlan;
                  var vlan = selectedPhysicalNetworkObj.vlan;

                  if(vlan != null && vlan.length > 0) {
                    if(vlan.indexOf("-") != -1) {
                      var vlanArray = vlan.split("-");
                      startVlan = vlanArray[0];
                      endVlan = vlanArray[1];
                    }
                    else {
                      startVlan = vlan;
                    }
                    selectedPhysicalNetworkObj["startVlan"] = startVlan;
                    selectedPhysicalNetworkObj["endVlan"] = endVlan;
                  }

                  args.response.success({
                    actionFilter: function() {
                      var allowedActions = [];
                      if(selectedZoneObj.networktype == "Advanced")
                        allowedActions.push("edit");
                      return allowedActions;
                    },
                    data: selectedPhysicalNetworkObj
                  });
                }
              },

              ipAddresses: {
                title: 'IP Ranges',
                custom: function(args) {
                  return $('<div></div>').multiEdit({
                    context: args.context,
                    noSelect: true,
                    fields: {
										  'podid': { 
											  label: 'Pod',
												select: function(args) {												 
												  $.ajax({
													  url: createURL("listPods&zoneid=" + selectedZoneObj.id),
														dataType: "json",
														success: function(json) {														  
															var items = [];
															var pods = json.listpodsresponse.pod;
															$(pods).each(function(){
															  items.push({name: this.id, description: this.name}); //should be "{id: this.id, description: this.name}" (to be consistent with dropdown in createFrom and edit mode) (Brian will fix widget later)
															});
															args.response.success({	data: items });
														}
													});
												}											
											},
                      'gateway': { edit: true, label: 'Gateway' },
                      'netmask': { edit: true, label: 'Netmask' },                     
                      'startip': { edit: true, label: 'Start IP' },
                      'endip': { edit: true, label: 'End IP' },
                      'add-rule': { label: 'Add', addButton: true }
                    },
                    add: {
                      label: 'Add',
                      action: function(args) {											  
                        var array1 = [];												
                        array1.push("&podid=" + args.data.podid); 
												array1.push("&networkid=" + selectedGuestNetworkObj.id)
                        array1.push("&gateway=" + args.data.gateway);
                        array1.push("&netmask=" + args.data.netmask);
                        array1.push("&startip=" + args.data.startip);
                        if(args.data.endip != null && args.data.endip.length > 0)
                          array1.push("&endip=" + args.data.endip);
												array1.push("&forVirtualNetwork=false"); //indicates this new IP range is for guest network, not public network	
													
                        $.ajax({
                          url: createURL("createVlanIpRange" + array1.join("")),
                          dataType: "json",
                          success: function(json) {													 
                            var item = json.createvlaniprangeresponse.vlan;
                            args.response.success({
                              data: item,
                              notification: {
                                label: 'IP range is added',
                                poll: function(args) {
                                  args.complete();
                                }
                              }
                            });
                          },
                          error: function(XMLHttpResponse) {
                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                            args.response.error(errorMsg);
                          }
                        });
                      }
                    },
                    actions: {
                      destroy: {
                        label: 'Delete',
                        action: function(args) {
                          $.ajax({
                            url: createURL('deleteVlanIpRange&id=' + args.context.multiRule[0].id),
                            dataType: 'json',
                            async: true,
                            success: function(json) {
                              args.response.success({
                                notification: {
                                  label: 'Remove IP range ' + args.context.multiRule[0].id,
                                  poll: function(args) {
                                    args.complete();
                                  }
                                }
                              });
                            }
                          });
                        }
                      }
                    },
                    dataProvider: function(args) { //only basic zone has IP Range tab                      
                      selectedGuestNetworkObj = null;
                      $.ajax({
                        url: createURL("listNetworks&listAll=true&trafficType=Guest&zoneid=" + selectedZoneObj.id),
                        dataType: "json",
                        async: false,
                        success: function(json) {                         
                          var items = json.listnetworksresponse.network;
                          if(items != null && items.length > 0)
                            selectedGuestNetworkObj = json.listnetworksresponse.network[0];                          
                        }
                      });                      
                      if(selectedGuestNetworkObj == null) 
                        return;                      
                    
                      $.ajax({
                        url: createURL("listVlanIpRanges&zoneid=" + selectedZoneObj.id + "&networkId=" + selectedGuestNetworkObj.id), 
                        dataType: "json",
                        success: function(json) {
                          var items = json.listvlaniprangesresponse.vlaniprange;
                          args.response.success({data: items});
                        }
                      });
                    }
                  });
                }
              },

              network: {
                title: 'Network',
                listView: {
                  section: 'networks',
                  id: 'networks',
                  fields: {
                    name: { label: 'Name' },
                    type: { label: 'Type' },
                    vlan: { label: 'VLAN ID' },                    
                    cidr: { label: 'CIDR' },
                    scope: { label: 'Scope' }
                  },
                  actions: {
                    add: {
                      label: 'Create network',

                      messages: {
                        confirm: function(args) {
                          return 'Are you sure you want to create a network?';
                        },
                        success: function(args) {
                          return 'Your new network is being created.';
                        },
                        notification: function(args) {
                          return 'Creating new network';
                        },
                        complete: function(args) {
                          return 'Network has been created successfully!';
                        }
                      },

                      createForm: {
                        title: 'Create network',
                        preFilter: function(args) {
                          if(selectedZoneObj.networktype == "Basic") {    
                            args.$form.find('.form-item[rel=vlanId]').hide();
                            args.$form.find('.form-item[rel=scope]').hide();
                            args.$form.find('.form-item[rel=domainId]').hide();
                            args.$form.find('.form-item[rel=account]').hide();
                            args.$form.find('.form-item[rel=networkdomain]').hide();

                            args.$form.find('.form-item[rel=podId]').css('display', 'inline-block');
                          }
                          else {  //"Advanced"                           
                            args.$form.find('.form-item[rel=vlanId]').css('display', 'inline-block');
                            args.$form.find('.form-item[rel=scope]').css('display', 'inline-block');
                            //args.$form.find('.form-item[rel=domainId]').css('display', 'inline-block'); //depends on scope field
                            //args.$form.find('.form-item[rel=account]').css('display', 'inline-block');  //depends on scope field
                            args.$form.find('.form-item[rel=networkdomain]').css('display', 'inline-block');

                            args.$form.find('.form-item[rel=podId]').hide();
                          }
                        },
                        fields: {
                          name: {
                            label: 'Name',
                            validation: { required: true }
                          },
                          description: {
                            label: 'Description',
                            validation: { required: true }
                          },  
                          vlanId: { 
                            label: "VLAN ID" 
                          },
                          
                          scope: {
                            label: 'Scope',
                            select: function(args) {
                              var array1 = [];
                              if(selectedZoneObj.securitygroupsenabled) {
                                array1.push({id: 'account-specific', description: 'Account'});
                              }
                              else {
                                array1.push({id: 'zone-wide', description: 'All'});
                                array1.push({id: 'domain-specific', description: 'Domain'});
                                array1.push({id: 'account-specific', description: 'Account'});
                              }
                              args.response.success({data: array1});

                              args.$select.change(function() {
                                var $form = $(this).closest('form');
                                if($(this).val() == "zone-wide") {
                                  $form.find('.form-item[rel=domainId]').hide();
                                  $form.find('.form-item[rel=account]').hide();
                                }
                                else if ($(this).val() == "domain-specific") {
                                  $form.find('.form-item[rel=domainId]').css('display', 'inline-block');
                                  $form.find('.form-item[rel=account]').hide();
                                }
                                else if($(this).val() == "account-specific") {
                                  $form.find('.form-item[rel=domainId]').css('display', 'inline-block');
                                  $form.find('.form-item[rel=account]').css('display', 'inline-block');
                                }
                              });
                            }
                          },
                          domainId: {
                            label: 'Domain',
                            validation: { required: true },
                            select: function(args) {
                              var items = [];
                              if(selectedZoneObj.domainid != null) { //list only domains under selectedZoneObj.domainid
                                $.ajax({
                                  url: createURL("listDomainChildren&id=" + selectedZoneObj.domainid + "&isrecursive=true"),
                                  dataType: "json",
                                  async: false,
                                  success: function(json) {
                                    var domainObjs = json.listdomainchildrenresponse.domain;
                                    $(domainObjs).each(function() {
                                      items.push({id: this.id, description: this.name});
                                    });
                                  }
                                });
                                $.ajax({
                                  url: createURL("listDomains&id=" + selectedZoneObj.domainid),
                                  dataType: "json",
                                  async: false,
                                  success: function(json) {
                                    var domainObjs = json.listdomainsresponse.domain;
                                    $(domainObjs).each(function() {
                                      items.push({id: this.id, description: this.name});
                                    });
                                  }
                                });
                              }
                              else { //list all domains
                                $.ajax({
                                  url: createURL("listDomains&listAll=true"),
                                  dataType: "json",
                                  async: false,
                                  success: function(json) {
                                    var domainObjs = json.listdomainsresponse.domain;
                                    $(domainObjs).each(function() {
                                      items.push({id: this.id, description: this.name});
                                    });
                                  }
                                });
                              }
                              args.response.success({data: items});
                            }
                          },
                          account: { label: 'Account' },
                          
                          networkOfferingId: {
                            label: 'Network offering',
                            dependsOn: 'scope',
                            select: function(args) {
                              var array1 = [];
                              var apiCmd = "listNetworkOfferings&state=Enabled";
                              if(selectedZoneObj.networktype == "Advanced") {  //Advanced zone
                                if(args.scope == "zone-wide" || args.scope == "domain-specific")
                                  apiCmd += "&guestiptype=Shared";
                                else  //args.scope == "account-specific"
                                  apiCmd += "&guestiptype=Isolated&sourcenatsupported=false";
                              }
                              else {  //Basic zone
                                apiCmd += "&guestiptype=Shared";
                              }
                              $.ajax({
                                url: createURL(apiCmd),
                                dataType: "json",
                                async: false,
                                success: function(json) {
                                  var networkOfferings = json.listnetworkofferingsresponse.networkoffering;
                                  if (networkOfferings != null && networkOfferings.length > 0) {
                                    //for (var i = 0; i < networkOfferings.length; i++) {
                                    for (var i = (networkOfferings.length-1); i >= 0; i--) {
                                      if(nspMap["securityGroups"].state == "Disabled"){ //if security groups provider is disabled, exclude network offerings that has "SecurityGroupProvider" in service
                                        var includingSGP = false;
                                        var serviceObjArray = networkOfferings[i].service;
                                        for(var k = 0; k < serviceObjArray.length; k++) {
                                          if(serviceObjArray[k].name == "SecurityGroup") {
                                            includingSGP = true;
                                            break;
                                          }
                                        }
                                        if(includingSGP == true)
                                          continue; //skip to next network offering
                                      }
                                      array1.push({id: networkOfferings[i].id, description: networkOfferings[i].displaytext});
                                    }
                                  }
                                }
                              });
                              args.response.success({data: array1});
                            }
                          },

                          podId: {
                            label: 'Pod',
                            validation: { required: true },
                            select: function(args) {
                              var items = [];
                              if(selectedZoneObj.networktype == "Basic") {
                                $.ajax({
                                  url: createURL("listPods&zoneid=" + selectedZoneObj.id),
                                  dataType: "json",
                                  async: false,
                                  success: function(json) {
                                    var podObjs = json.listpodsresponse.pod;
                                    $(podObjs).each(function(){
                                      items.push({id: this.id, description: this.name});
                                    });
                                  }
                                });
                                items.push({id: 0, description: "(create new pod)"});
                              }
                              args.response.success({data: items});

                              args.$select.change(function() {
                                var $form = $(this).closest('form');
                                if($(this).val() == "0") {
                                  $form.find('.form-item[rel=podname]').css('display', 'inline-block');
                                  $form.find('.form-item[rel=reservedSystemGateway]').css('display', 'inline-block');
                                  $form.find('.form-item[rel=reservedSystemNetmask]').css('display', 'inline-block');
                                  $form.find('.form-item[rel=reservedSystemStartIp]').css('display', 'inline-block');
                                  $form.find('.form-item[rel=reservedSystemEndIp]').css('display', 'inline-block');
                                }
                                else {
                                  $form.find('.form-item[rel=podname]').hide();
                                  $form.find('.form-item[rel=reservedSystemGateway]').hide();
                                  $form.find('.form-item[rel=reservedSystemNetmask]').hide();
                                  $form.find('.form-item[rel=reservedSystemStartIp]').hide();
                                  $form.find('.form-item[rel=reservedSystemEndIp]').hide();
                                }
                              });
                            }
                          },

                          //create new pod fields start here
                          podname: {
                            label: 'Pod name',
                            validation: { required: true }
                          },
                          reservedSystemGateway: {
                            label: 'Reserved system gateway',
                            validation: { required: true }
                          },
                          reservedSystemNetmask: {
                            label: 'Reserved system netmask',
                            validation: { required: true }
                          },
                          reservedSystemStartIp: {
                            label: 'Start Reserved system IP',
                            validation: { required: true }
                          },
                          reservedSystemEndIp: {
                            label: 'End Reserved system IP',
                            validation: { required: false }
                          },
                          //create new pod fields ends here
                          
                          guestGateway: { label: 'Guest gateway' },
                          guestNetmask: { label: 'Guest netmask' },
                          guestStartIp: { label: 'Guest start IP' },
                          guestEndIp: { label: 'Guest end IP' },
                          networkdomain: { label: 'Network domain' }
                        }
                      },

                      action: function(args) {
                        var array1 = [];
                        array1.push("&zoneId=" + selectedZoneObj.id);
                        array1.push("&name=" + todb(args.data.name));
                        array1.push("&displayText=" + todb(args.data.description));
                        array1.push("&networkOfferingId=" + args.data.networkOfferingId);

                        if(selectedZoneObj.networktype == "Basic") {
                          array1.push("&vlan=untagged");
                        }
                        else {  //"Advanced"
                          array1.push("&vlan=" + todb(args.data.vlanId));                        

                          var $form = args.$form;
                          if($form.find('.form-item[rel=domainId]').css("display") != "none") {
                            if($form.find('.form-item[rel=account]').css("display") != "none") {  //account-specific
                              array1.push("&acltype=account");
                              array1.push("&domainId=" + args.data.domainId);
                              array1.push("&account=" + args.data.account);
                            }
                            else {  //domain-specific
                              array1.push("&acltype=domain");
                              array1.push("&domainId=" + args.data.domainId);
                            }
                          }
                          else { //zone-wide
                            array1.push("&acltype=domain"); //server-side will make it Root domain (i.e. domainid=1)
                          }
                         
                          array1.push("&gateway=" + args.data.guestGateway);
                          array1.push("&netmask=" + args.data.guestNetmask);
                          array1.push("&startip=" + args.data.guestStartIp);
                          array1.push("&endip=" + args.data.guestEndIp);

                          if(args.data.networkdomain != null && args.data.networkdomain.length > 0)
                            array1.push("&networkdomain=" + todb(args.data.networkdomain));
                        }

                        $.ajax({
                          url: createURL("createNetwork" + array1.join("")),
                          dataType: "json",
                          success: function(json) {
                            var item = json.createnetworkresponse.network;
                            args.response.success({data:item});

                            if(selectedZoneObj.networktype == "Basic") {
                              var array2 = [];

                              var podId;
                              if(args.data.podId != "0") {
                                podId = args.data.podId;
                              }
                              else { //args.data.podId==0, create pod first
                                var array1 = [];
                                array1.push("&zoneId=" + selectedZoneObj.id);
                                array1.push("&name=" + todb(args.data.podname));
                                array1.push("&gateway=" + todb(args.data.reservedSystemGateway));
                                array1.push("&netmask=" + todb(args.data.reservedSystemNetmask));
                                array1.push("&startIp=" + todb(args.data.reservedSystemStartIp));

                                var endip = args.data.reservedSystemEndIp;      //optional
                                if (endip != null && endip.length > 0)
                                  array1.push("&endIp=" + todb(endip));

                                $.ajax({
                                  url: createURL("createPod" + array1.join("")),
                                  dataType: "json",
                                  async: false,
                                  success: function(json) {
                                    var item = json.createpodresponse.pod;
                                    podId = item.id;
                                  },
                                  error: function(XMLHttpResponse) {
                                    //var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                    //args.response.error(errorMsg);
                                  }
                                });
                              }
                              if(podId == null)  {
                                alert("podId is null, so unable to create IP range on pod level");
                                return;
                              }
                              array2.push("&podId=" + podId);
                              array2.push("&vlan=untagged");
                              array2.push("&zoneid=" + selectedZoneObj.id);
                              array2.push("&forVirtualNetwork=false"); //direct VLAN
                              array2.push("&gateway=" + todb(args.data.guestGateway));
                              array2.push("&netmask=" + todb(args.data.guestNetmask));
                              array2.push("&startip=" + todb(args.data.guestStartIp));
                              var endip = args.data.guestEndIp;
                              if(endip != null && endip.length > 0)
                                array2.push("&endip=" + todb(endip));
                              $.ajax({
                                url: createURL("createVlanIpRange" + array2.join("")),
                                dataType: "json",
                                async: false,
                                success: function(json) {
                                  //var item = json.createvlaniprangeresponse.vlan;
                                },
                                error: function(XMLHttpResponse) {
                                  //var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                  //args.response.error(errorMsg);
                                }
                              });

                            }
                          },
                          error: function(XMLHttpResponse) {
                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                            args.response.error(errorMsg);
                          }
                        });
                      },

                      preAction: function(args) {
                        //var zone = $('.detail-view:last').data('view-args').context.zones[0]; //this line causes a bug when going back and forth between listView and detailView: "$(".detail-view:last").data("view-args").context.zones is undefined"
                        var zone = selectedZoneObj;
                        
                        var networksPresent = false;

                        // Only 1 guest network is allowed per basic zone,
                        // so don't show the dialog in this case
                        $.ajax({
                          url: createURL('listNetworks&listAll=true'),
                          data: {
                            trafficType: 'guest',
                            zoneId: zone.id
                          },
                          async: false,
                          success: function(json) {
                            if (json.listnetworksresponse.network) {
                              networksPresent = true;
                            }
                          }
                        });

                        if (zone.networktype == 'Basic' && networksPresent) {
                          cloudStack.dialog.notice({
                            message: 'Sorry, you can only have one guest network for a basic zone.'
                          });

                          return false;
                        }

                        return true;
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
											if(args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
												switch(args.filterBy.search.by) {
												case "name":
													if(args.filterBy.search.value.length > 0)
														array1.push("&keyword=" + args.filterBy.search.value);
													break;
												}
											}
										}
										
                    $.ajax({
                      url: createURL("listNetworks&listAll=true&trafficType=Guest&zoneId=" + selectedZoneObj.id + "&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
                      dataType: "json",
                      success: function(json) {
                        var items = json.listnetworksresponse.network;
                                                                      
                        $(items).each(function(){                          
                          this.networkdomaintext = this.networkdomain;
                          this.networkofferingidText = this.networkofferingid;
                                                    
                          if(this.acltype == "Domain") {
                            if(this.domainid == rootAccountId) 
                              this.scope = "All";                            
                            else 
                              this.scope = "Domain (" + this.domain + ")";                            
                          } 
                          else if (this.acltype == "Account"){
                            this.scope = "Account (" + this.domain + ", " + this.account + ")";      
                          }

                          if(this.vlan == null && this.broadcasturi != null)
                            this.vlan = this.broadcasturi.replace("vlan://", "");                          
                        });                        
                        
                        args.response.success({data: items});
                      }
                    });
                  },

                  detailView: {
                    name: 'Guest network details',
                    viewAll: { 
										  path: '_zone.guestIpRanges', 
											label: 'IP ranges',
                      preFilter: function(args) {                        
												if(selectedGuestNetworkObj.type == "Isolated") {												  
													var services = selectedGuestNetworkObj.service;
													if(services != null) {
														for(var i=0; i < services.length; i++) {
																var service = services[i];
																if(service.name == "SourceNat")
																	return false;
														}
													}  								  
												}
												return true;
                      }											
									  },
                    actions: {
                      edit: {
                        label: 'Edit',
                        messages: {
                          confirm: function(args) {
                            return 'Are you sure you want to edit network?';
                          },
                          success: function(args) {
                            return 'Network is being edited.';
                          },
                          notification: function(args) {
                            return 'Editing network';
                          },
                          complete: function(args) {
                            return 'Network has been edited.';
                          }
                        },
                        action: function(args) {
                          var array1 = [];
                          array1.push("&name=" + todb(args.data.name));
                          array1.push("&displaytext=" + todb(args.data.displaytext));

                          //args.data.networkofferingid is null when networkofferingid field is hidden
                          if(args.data.networkofferingid != null && args.data.networkofferingid != selectedGuestNetworkObj.networkofferingid)
                            array1.push("&networkofferingid=" + todb(args.data.networkofferingid));    

                          //args.data.networkdomain is null when networkdomain field is hidden
                          if(args.data.networkdomain != null && args.data.networkdomain != selectedGuestNetworkObj.networkdomain)
                            array1.push("&networkdomain=" + todb(args.data.networkdomain));

                          $.ajax({
                            url: createURL("updateNetwork&id=" + args.context.networks[0].id + array1.join("")),
                            dataType: "json",
                            success: function(json) {
                              var jid = json.updatenetworkresponse.jobid;
                              args.response.success(
                                {_custom:
                                 {jobId: jid,
                                  getUpdatedItem: function(json) {
                                    var item = json.queryasyncjobresultresponse.jobresult.network;
                                    return {data: item};
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
											
											'restart': { 											  
												label: 'Restart network',
												action: function(args) {												  
													$.ajax({
														url: createURL("restartNetwork&cleanup=true&id=" + args.context.networks[0].id),
														dataType: "json",
														async: true,
														success: function(json) {														  
															var jid = json.restartnetworkresponse.jobid;
															args.response.success(
																{_custom:
																 {jobId: jid,
																	getUpdatedItem: function(json) {																	  
																		return json.queryasyncjobresultresponse.jobresult.network;
																	}
																 }
																}
															);
														}
													});
												},
												messages: {
													confirm: function(args) {
														return 'Please confirm that you want to restart network';
													},
													success: function(args) {
														return 'Network is being restarted';
													},
													notification: function(args) {
														return 'Restarting network';
													},
													complete: function(args) {
														return 'Network has been restarted';
													}
												},
												notification: {
													poll: pollAsyncJobResult
												}												
											},
											
                      'delete': {
                        label: 'Delete network',
                        messages: {
                          confirm: function(args) {
                            return 'Are you sure you want to delete network ?';
                          },
                          success: function(args) {
                            return 'Network is being deleted.';
                          },
                          notification: function(args) {
                            return 'Deleting network';
                          },
                          complete: function(args) {
                            return 'Network has been deleted.';
                          }
                        },
                        action: function(args) {
                          $.ajax({
                            url: createURL("deleteNetwork&id=" + args.context.networks[0].id),
                            dataType: "json",
                            async: true,
                            success: function(json) {
                              var jid = json.deletenetworkresponse.jobid;
                              args.response.success(
                                {_custom:
                                 {jobId: jid,
                                  getUpdatedItem: function(json) {
                                    return {}; //nothing in this network needs to be updated, in fact, this whole template has being deleted
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
                      }
                    },
                    tabs: {
                      details: {
                        title: 'Details',
                        preFilter: function(args) {
                          var hiddenFields = [];
                          if(selectedZoneObj.networktype == "Basic") {
                            hiddenFields.push("account");
                            hiddenFields.push("gateway");
                            //hiddenFields.push("netmask");                            
                          }

                          if(selectedGuestNetworkObj.type == "Isolated") {
                            hiddenFields.push("networkofferingdisplaytext");
                            hiddenFields.push("networkdomaintext");
                            hiddenFields.push("gateway");
                            //hiddenFields.push("netmask");                            
                          }
                          else { //selectedGuestNetworkObj.type == "Shared"
                            hiddenFields.push("networkofferingid");
                            hiddenFields.push("networkdomain");
                          }
                          return hiddenFields;
                        },
                        fields: [
                          {
                            name: {
                              label: 'Name',
                              isEditable: true
                            }
                          },
                          {
                            id: { label: 'ID' },
                            displaytext: {
                              label: 'Description',
                              isEditable: true
                            },
                            type: {
                              label: 'Type'
                            },
                            state: {
                              label: 'State'
                            },                           
                            restartrequired: {
                              label: 'Restart required',
                              converter: function(booleanValue) {
                                if(booleanValue == true)
                                  return "<font color='red'>Yes</font>";
                                else if(booleanValue == false)
                                  return "No";
                              }
                            },                           
                            vlan: { label: 'VLAN ID' },
                            scope: { label: 'Scope' },
                            networkofferingdisplaytext: { label: 'Network offering' },
                            networkofferingid: {
                              label: 'Network offering',
                              isEditable: true,
                              select: function(args){
                                var items = [];
                                $.ajax({
                                  url: createURL("listNetworkOfferings&networkid=" + selectedGuestNetworkObj.id),
                                  dataType: "json",
                                  async: false,
                                  success: function(json) {
                                    var networkOfferingObjs = json.listnetworkofferingsresponse.networkoffering;
                                    $(networkOfferingObjs).each(function() {
                                      items.push({id: this.id, description: this.displaytext});
                                    });
                                  }
                                });
                                $.ajax({
                                  url: createURL("listNetworkOfferings&id=" + selectedGuestNetworkObj.networkofferingid),  //include currently selected network offeirng to dropdown
                                  dataType: "json",
                                  async: false,
                                  success: function(json) {
                                    var networkOfferingObjs = json.listnetworkofferingsresponse.networkoffering;
                                    $(networkOfferingObjs).each(function() {
                                      items.push({id: this.id, description: this.displaytext});
                                    });
                                  }
                                });
                                args.response.success({data: items});
                              }
                            },
                            
                            networkofferingidText: {
                              label: 'Network offering ID'
                            },
                          
                            gateway: { label: 'Gateway' },
                            //netmask: { label: 'Netmask' },                            
                            cidr: { label: 'CIDR' },
                            networkdomaintext: {
                              label: 'Network domain'
                            },
                            networkdomain: {
                              label: 'Network domain',
                              isEditable: true
                            }
                          }
                        ],
                        dataProvider: function(args) {    
                          selectedGuestNetworkObj = args.context.networks[0];                        
                          args.response.success({data: selectedGuestNetworkObj});
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      },
      networks: {
        actions: {
          add: {
            label: 'Add physical network',
            action: function(args) {
              args.response.success();
            }
          }
        },
        dataProvider: function(args) {
          setTimeout(function() {
            args.response.success({
              data: [
                { id: 1, name: 'Network 1' }
                //{ id: 2, name: 'Network 2' },
                //{ id: 3, name: 'Network 3' }
              ]
            });
          }, 500);
        }
      },

      networkProviders: {
        // Returns state of each network provider type
        statusCheck: function(args) {
          naasStatusMap = {
            virtualRouter: 'not-configured',
            netscaler: 'not-configured',
            f5: 'not-configured',
            srx: 'not-configured',
            securityGroups: 'not-configured'
          };

          selectedZoneObj = args.context.physicalResources[0];
          $.ajax({
            url: createURL("listPhysicalNetworks&zoneId=" + selectedZoneObj.id),
            dataType: "json",
            async: false,
            success: function(json) {
              var items = json.listphysicalnetworksresponse.physicalnetwork;
              selectedPhysicalNetworkObj = items[0];
            }
          });

          $.ajax({
            url: createURL("listNetworkServiceProviders&physicalnetworkid=" + selectedPhysicalNetworkObj.id),
            dataType: "json",
            async: false,
            success: function(json) {
              var items = json.listnetworkserviceprovidersresponse.networkserviceprovider;
              nspMap = {}; //empty the map
              for(var i = 0; i < items.length; i++) {
                switch(items[i].name) {
                  case "VirtualRouter":
                    nspMap["virtualRouter"] = items[i];
                    if(items[i].state == "Enabled") {
                      naasStatusMap["virtualRouter"] = "enabled";
                    }
                    else {
                      naasStatusMap["virtualRouter"] = "disabled";  //VirtualRouter provider is disabled
                    }
                    break;
                  case "Netscaler":
                    nspMap["netscaler"] = items[i];
                    if(items[i].state == "Enabled") {
                      naasStatusMap["netscaler"] = "enabled";
                    }
                    else { //items[i].state == "Disabled"
                      $.ajax({
                        url: createURL("listNetscalerLoadBalancers&physicalnetworkid=" + selectedPhysicalNetworkObj.id),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                          var items = json.listnetscalerloadbalancerresponse.netscalerloadbalancer;
                          if(items != null && items.length > 0) {
                            naasStatusMap["netscaler"] = "disabled";  //NetScaler provider is disabled with device(s)
                          }
                        }
                      });
                    }
                    break;
                  case "F5BigIp":
                    nspMap["f5"] = items[i];
                    if(items[i].state == "Enabled") {
                      naasStatusMap["f5"] = "enabled";
                    }
                    else { //items[i].state == "Disabled"
                      $.ajax({
                        url: createURL("listF5LoadBalancers&physicalnetworkid=" + selectedPhysicalNetworkObj.id),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                          var items = json.listf5loadbalancerresponse.f5loadbalancer;
                          if(items != null && items.length > 0) {
                            naasStatusMap["f5"] = "disabled";  //F5 provider is disabled with device(s)
                          }
                        }
                      });
                    }
                    break;
                  case "JuniperSRX":
                    nspMap["srx"] = items[i];
                    if(items[i].state == "Enabled") {
                      naasStatusMap["srx"] = "enabled";
                    }
                    else { //items[i].state == "Disabled"
                      $.ajax({
                        url: createURL("listSrxFirewalls&physicalnetworkid=" + selectedPhysicalNetworkObj.id),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                          var items = json.listsrxfirewallresponse.srxfirewall;
                          if(items != null && items.length > 0) {
                            naasStatusMap["srx"] = "disabled";  //SRX provider is disabled with device(s)
                          }
                        }
                      });
                    }
                    break;
                  case "SecurityGroupProvider":
                    nspMap["securityGroups"] = items[i];
                    if(items[i].state == "Enabled") {
                      naasStatusMap["securityGroups"] = "enabled";
                    }
                    else {
                      naasStatusMap["securityGroups"] = "disabled";  //SecurityGroup provider is disabled
                    }
                    break;
                }
              }
            }
          });
          return naasStatusMap;
        },

        statusLabels: {
          enabled: 'Enabled',             //having device, network service provider is enabled
          'not-configured': 'Not setup',  //no device
          disabled: 'Disabled'            //having device, network service provider is disabled
        },

        // Actions performed on entire net. provider type
        actions: {
          enable: function(args) {
            args.response.success();
          },

          disable: function(args) {
            args.response.success();
          }
        },

        types: {
          // Virtual router provider
          virtualRouter: {
            id: 'virtualRouterProviders',
            label: 'Virtual Router',
            type: 'detailView',
            fields: {
              name: { label: 'Name' },
              ipaddress: { label: 'IP Address' },
              state: { label: 'Status', indicator: { 'Enabled': 'on' } }
            },
            tabs: {
              network: {
                title: 'Network',
                fields: [
                  {
                    name: { label: 'Name' }
                  },
                  {
                    id: { label: 'ID' },
                    state: { label: 'State' },
                    physicalnetworkid: { label: 'Physical network ID' },
                    destinationphysicalnetworkid: { label: 'Destination physical networkID' }
                  },
                  {
                    Vpn: { label: 'VPN' },
                    Dhcp: { label: 'DHCP' },
                    Dns: { label: 'DNS' },
                    Gateway: { label: 'Gateway' },
                    Firewall: { label: 'Firewall' },
                    Lb: { label: 'Load Balancer' },
                    UserData: { label: 'UserData' },
                    SourceNat: { label: 'Source NAT' },
                    StaticNat: { label: 'Static NAT' },
                    PortForwarding: { label: 'Port Forwarding' }
                  }
                ],
                dataProvider: function(args) {
                  args.response.success({
                    actionFilter: virtualRouterProviderActionFilter,
                    data: $.extend(true, {}, nspMap["virtualRouter"], {
                      Vpn: 'On',
                      Dhcp: 'On',
                      Dns: 'On',
                      Gateway: 'On',
                      Firewall: 'On',
                      Lb: 'On',
                      UserData: 'On',
                      SourceNat: 'On',
                      StaticNat: 'On',
                      PortForwarding: 'On'
                    })
                  });
                }
              },

              instances: {
                title: 'Instances',
                listView: {
                  label: 'Virtual Appliances',
                  id: 'routers',
                  fields: {
                    name: { label: 'Name' },
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
                  dataProvider: function(args) {									 
									  var array1 = [];  
										if(args.filterBy != null) {          
											if(args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
												switch(args.filterBy.search.by) {
												case "name":
													if(args.filterBy.search.value.length > 0)
														array1.push("&keyword=" + args.filterBy.search.value);
													break;
												}
											}
										}
									
                    $.ajax({
                      url: createURL("listRouters&zoneid=" + selectedZoneObj.id + "&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
                      dataType: 'json',
                      async: true,
                      success: function(json) {
                        var items = json.listroutersresponse.router;
                        args.response.success({
                          actionFilter: routerActionfilter,
                          data: items
                        });
                      }
                    });
                  },
                  detailView: {
                    name: 'Virtual applicance details',
                    actions: {
                      start: {
                        label: 'Start router',
                        messages: {
                          confirm: function(args) {
                            return 'Are you sure you want to start router?';
                          },
                          notification: function(args) {
                            return 'Starting router';
                          }
                        },
                        action: function(args) {
                          $.ajax({
                            url: createURL('startRouter&id=' + args.context.routers[0].id),
                            dataType: 'json',
                            async: true,
                            success: function(json) {
                              var jid = json.startrouterresponse.jobid;
                              args.response.success({
                                _custom: {
                                  jobId: jid,
                                  getUpdatedItem: function(json) {
                                    return json.queryasyncjobresultresponse.jobresult.domainrouter;
                                  },
                                  getActionFilter: function() {
                                    return routerActionfilter;
                                  }
                                }
                              });
                            }
                          });
                        },
                        notification: {
                          poll: pollAsyncJobResult
                        }
                      },

                      stop: {
                        label: 'Stop router',
                        messages: {
                          confirm: function(args) {
                            return 'Are you sure you want to stop router?';
                          },
                          notification: function(args) {
                            return 'Stopping router';
                          }
                        },
                        action: function(args) {
                          $.ajax({
                            url: createURL('stopRouter&id=' + args.context.routers[0].id),
                            dataType: 'json',
                            async: true,
                            success: function(json) {
                              var jid = json.stoprouterresponse.jobid;
                              args.response.success({
                                _custom: {
                                  jobId: jid,
                                  getUpdatedItem: function(json) {
                                    return json.queryasyncjobresultresponse.jobresult.domainrouter;
                                  },
                                  getActionFilter: function() {
                                    return routerActionfilter;
                                  }
                                }
                              });
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
                            serviceOfferingId: {
                              label: 'Service offering',
                              select: function(args) {
                                $.ajax({
                                  url: createURL("listServiceOfferings&issystem=true&systemvmtype=domainrouter"),
                                  dataType: "json",
                                  async: true,
                                  success: function(json) {
                                    var serviceofferings = json.listserviceofferingsresponse.serviceoffering;
                                    var items = [];
                                    $(serviceofferings).each(function() {
                                      if(this.id != args.context.routers[0].serviceofferingid) {
                                        items.push({id: this.id, description: this.displaytext});
                                      }
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
                            url: createURL("changeServiceForRouter&id=" + args.context.routers[0].id + "&serviceofferingid=" + args.data.serviceOfferingId),
                            dataType: "json",
                            async: true,
                            success: function(json) {
                              var jsonObj = json.changeserviceforrouterresponse.domainrouter;
                              args.response.success({data: jsonObj});
                            },
                            error: function(XMLHttpResponse) {
                              var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                              args.response.error(errorMsg);
                            }
                          });
                        },
                        notification: {
                          poll: function(args) {
                            args.complete();
                          }
                        }
                      },

                      migrate: {
                        label: 'Migrate router',
                        messages: {
                          confirm: function(args) {
                            return 'Are you sure you want to migrate router?';
                          },
                          success: function(args) {
                            return 'Router is being migrated.';
                          },
                          notification: function(args) {
                            return 'Migrating router';
                          },
                          complete: function(args) {
                            return 'Router has been migrated.';
                          }
                        },
                        createForm: {
                          title: 'Migrate router',
                          desc: '',
                          fields: {
                            hostId: {
                              label: 'Host',
                              validation: { required: true },
                              select: function(args) {
                                $.ajax({
                                  url: createURL("listHosts&VirtualMachineId=" + args.context.routers[0].id),
                                  //url: createURL("listHosts"),	//for testing only, comment it out before checking in.
                                  dataType: "json",
                                  async: true,
                                  success: function(json) {
                                    var hostObjs = json.listhostsresponse.host;
                                    var items = [];
                                    $(hostObjs).each(function() {
                                      items.push({id: this.id, description: (this.name + ": " +(this.hasEnoughCapacity? "Available" : "Full"))});
                                    });
                                    args.response.success({data: items});
                                  }
                                });
                              },
                              error: function(XMLHttpResponse) {
                                var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                args.response.error(errorMsg);
                              }
                            }
                          }
                        },
                        action: function(args) {												                      
                          $.ajax({
                            url: createURL("migrateSystemVm&hostid=" + args.data.hostId + "&virtualmachineid=" + args.context.routers[0].id),
                            dataType: "json",
                            async: true,
                            success: function(json) {
                              var jid = json.migratesystemvmresponse.jobid;
                              args.response.success({
                                _custom: {
                                  jobId: jid,
                                  getUpdatedItem: function(json) {
                                    //return json.queryasyncjobresultresponse.jobresult.systemvminstance;    //not all properties returned in systemvminstance
                                    $.ajax({
                                      url: createURL("listRouters&id=" + json.queryasyncjobresultresponse.jobresult.systemvminstance.id),
                                      dataType: "json",
                                      async: false,
                                      success: function(json) {
                                        var items = json.listroutersresponse.router;
                                        if(items != null && items.length > 0) {
                                          return items[0];
                                        }
                                      }
                                    });
                                  },
                                  getActionFilter: function() {
                                    return routerActionfilter;
                                  }
                                }
                              });
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
                              return clientConsoleUrl + '?cmd=access&vm=' + args.context.routers[0].id;
                            },
                            title: function(args) {
                              return "console";  //can't have space in window name in window.open()
                            },
                            width: 820,
                            height: 640
                          }
                        }
                      }
                    },
                    tabs: {
                      details: {
                        title: 'Details',
                        fields: [
                          {
                            name: { label: 'Name' }
                          },
                          {
                            id: { label: 'ID' },
                            state: { label: 'State' },
                            publicip: { label: 'Public IP' },
                            guestipaddress: { label: 'Guest IP' },
                            linklocalip: { label: 'Link local IP' },
                            hostname: { label: 'Host' },
                            serviceofferingname: { label: 'Service offering' },
                            networkdomain: { label: 'Network domain' },
                            domain: { label: 'Domain' },
                            account: { label: 'Account' },
                            created: { label: 'Created', converter: cloudStack.converters.toLocalDate },
                            isredundantrouter: {
                              label: 'Redundant router',
                              converter: cloudStack.converters.toBooleanText
                            },
                            redundantRouterState: { label: 'Redundant state' }
                          }
                        ],
                        dataProvider: function(args) {
                          var item = args.context.routers[0];
                          if(item.isredundantrouter == true)
                            item["redundantRouterState"] = item.redundantstate;
                          else
                            item["redundantRouterState"] = "";
                          args.response.success({
                            actionFilter: routerActionfilter,
                            data: item
                          });
                        }
                      }
                    }
                  }
                }
              }
            },
            actions: {
              enable: {
                label: 'Enable provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["virtualRouter"].id + "&state=Enabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid
                          }
                        }
                      );

                      $(window).trigger('cloudStack.fullRefresh');
                    }
                  });
                },
                messages: {
                  notification: function() { return 'Provider is enabled'; }
                },
                notification: { poll: pollAsyncJobResult }
              },
              disable: {
                label: 'Disable provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["virtualRouter"].id + "&state=Disabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid
                          }
                        }
                      );

                      $(window).trigger('cloudStack.fullRefresh');
                    }
                  });
                },
                messages: {
                  notification: function() { return 'Provider is disabled'; }
                },
                notification: { poll: pollAsyncJobResult }
              }
            }
          },

          // NetScaler list view
          netscaler: {
            type: 'detailView',
            id: 'netscalerProviders',
            label: 'NetScaler',
            viewAll: { label: 'Providers', path: '_zone.netscalerProviders' },
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    name: { label: 'Name' }
                  },
                  {
                    id: { label: 'ID' }
                  }
                ],
                dataProvider: function(args) {
                  args.response.success({
                    data: selectedPhysicalNetworkObj,
                    actionFilter: function(args) {
                      var allowedActions = [];
                      var jsonObj = nspMap["netscaler"];
                      if(jsonObj.state == "Enabled")
                        allowedActions.push("disable");
                      else if(jsonObj.state == "Disabled")
                        allowedActions.push("enable");
                      allowedActions.push("destroy");
                      return allowedActions;
                    }
                  });
                }
              }
            },
            actions: {
              add: {
                label: 'Add new NetScaler',
                createForm: {
                  title: 'Add new NetScaler',
                  fields: {
                    ip: {
                      label: 'IP address'
                    },
                    username: {
                      label: 'Username'
                    },
                    password: {
                      label: 'Password',
                      isPassword: true
                    },
                    networkdevicetype: {
                      label: 'Type',
                      select: function(args) {
                        var items = [];
                        items.push({id: "NetscalerMPXLoadBalancer", description: "NetScaler MPX LoadBalancer"});
                        items.push({id: "NetscalerVPXLoadBalancer", description: "NetScaler VPX LoadBalancer"});
                        items.push({id: "NetscalerSDXLoadBalancer", description: "NetScaler SDX LoadBalancer"});
                        args.response.success({data: items});
                      }
                    },
                    publicinterface: {
                      label: 'Public interface'
                    },
                    privateinterface: {
                      label: 'Private interface'
                    },
                    numretries: {
                      label: 'Number of retries',
                      defaultValue: '2'
                    },
                    inline: {
                      label: 'Mode',
                      select: function(args) {
                        var items = [];
                        items.push({id: "false", description: "side by side"});
                        items.push({id: "true", description: "inline"});
                        args.response.success({data: items});
                      }
                    },
                    capacity: {
                      label: 'Capacity',
                      validation: { required: false, number: true }
                    },
                    dedicated: {
                      label: 'Dedicated',
                      isBoolean: true,
                      isChecked: false
                    }
                  }
                },
                action: function(args) {
                  if(nspMap["netscaler"] == null) {
                    $.ajax({
                      url: createURL("addNetworkServiceProvider&name=Netscaler&physicalnetworkid=" + selectedPhysicalNetworkObj.id),
                      dataType: "json",
                      async: true,
                      success: function(json) {
                        var jobId = json.addnetworkserviceproviderresponse.jobid;
                        var timerKey = "addNetworkServiceProviderJob_"+jobId;
                        $("body").everyTime(2000, timerKey, function() {
                          $.ajax({
                            url: createURL("queryAsyncJobResult&jobId="+jobId),
                            dataType: "json",
                            success: function(json) {
                              var result = json.queryasyncjobresultresponse;
                              if (result.jobstatus == 0) {
                                return; //Job has not completed
                              }
                              else {
                                $("body").stopTime(timerKey);
                                if (result.jobstatus == 1) {
                                  nspMap["netscaler"] = result.jobresult.networkserviceprovider;
                                  addExternalLoadBalancer(args, selectedPhysicalNetworkObj, "addNetscalerLoadBalancer", "addnetscalerloadbalancerresponse", "netscalerloadbalancer");
                                }
                                else if (result.jobstatus == 2) {
                                  alert("addNetworkServiceProvider&name=Netscaler failed. Error: " + fromdb(result.jobresult.errortext));
                                }
                              }
                            },
                            error: function(XMLHttpResponse) {
                              var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                              alert("addNetworkServiceProvider&name=Netscaler failed. Error: " + errorMsg);
                            }
                          });
                        });
                      }
                    });
                  }
                  else {
                    addExternalLoadBalancer(args, selectedPhysicalNetworkObj, "addNetscalerLoadBalancer", "addnetscalerloadbalancerresponse", "netscalerloadbalancer");
                  }
                },
                messages: {
                  notification: function(args) {
                    return 'Added new NetScaler';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },
              enable: {
                label: 'Enable provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["netscaler"].id + "&state=Enabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid
                          }
                        }
                      );

                      $(window).trigger('cloudStack.fullRefresh');
                    }
                  });
                },
                messages: {
                  notification: function() { return 'Provider is enabled'; }
                },
                notification: { poll: pollAsyncJobResult }
              },
              disable: {
                label: 'Disable provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["netscaler"].id + "&state=Disabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid
                          }
                        }
                      );

                      $(window).trigger('cloudStack.fullRefresh');
                    }
                  });
                },
                messages: {
                  notification: function() { return 'Provider is disabled'; }
                },
                notification: { poll: pollAsyncJobResult }
              },
              destroy: {
                label: 'Shutdown provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteNetworkServiceProvider&id=" + nspMap["netscaler"].id),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.deletenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid
                          }
                        }
                      );

                      $(window).trigger('cloudStack.fullRefresh');
                    }
                  });
                },
                messages: {
                  notification: function() { return 'Provider is shutdown'; }
                },
                notification: { poll: pollAsyncJobResult }
              }
            }
          },

          f5: {
            type: 'detailView',
            id: 'f5Providers',
            label: 'F5',
            viewAll: { label: 'Providers', path: '_zone.f5Providers' },
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    name: { label: 'Name' }
                  },
                  {
                    id: { label: 'ID' }
                  }
                ],
                dataProvider: function(args) {
                  args.response.success({
                    data: selectedPhysicalNetworkObj,
                    actionFilter: function(args) {
                      var allowedActions = [];
                      var jsonObj = nspMap["f5"];
                      if(jsonObj.state == "Enabled") {
                        allowedActions.push("disable");
                      }
                      else if(jsonObj.state == "Disabled") {
                        allowedActions.push("enable");
                      }
                      allowedActions.push("destroy");
                      return allowedActions;
                    }
                  });
                }
              }
            },
            actions: {
              add: {
                label: 'Add new F5',
                createForm: {
                  title: 'Add F5',
                  fields: {
                    ip: {
                      label: 'IP address'
                    },
                    username: {
                      label: 'Username'
                    },
                    password: {
                      label: 'Password',
                      isPassword: true
                    },
                    networkdevicetype: {
                      label: 'Type',
                      select: function(args) {
                        var items = [];
                        items.push({id: "F5BigIpLoadBalancer", description: "F5 Big Ip Load Balancer"});
                        args.response.success({data: items});
                      }
                    },
                    publicinterface: {
                      label: 'Public interface'
                    },
                    privateinterface: {
                      label: 'Private interface'
                    },
                    numretries: {
                      label: 'Number of retries',
                      defaultValue: '2'
                    },
                    inline: {
                      label: 'Mode',
                      select: function(args) {
                        var items = [];
                        items.push({id: "false", description: "side by side"});
                        items.push({id: "true", description: "inline"});
                        args.response.success({data: items});
                      }
                    },
                    capacity: {
                      label: 'Capacity',
                      validation: { required: false, number: true }
                    },
                    dedicated: {
                      label: 'Dedicated',
                      isBoolean: true,
                      isChecked: false
                    }
                  }
                },
                action: function(args) {
                  if(nspMap["f5"] == null) {
                    $.ajax({
                      url: createURL("addNetworkServiceProvider&name=F5BigIp&physicalnetworkid=" + selectedPhysicalNetworkObj.id),
                      dataType: "json",
                      async: true,
                      success: function(json) {
                        var jobId = json.addnetworkserviceproviderresponse.jobid;
                        var timerKey = "addNetworkServiceProviderJob_"+jobId;
                        $("body").everyTime(2000, timerKey, function() {
                          $.ajax({
                            url: createURL("queryAsyncJobResult&jobId="+jobId),
                            dataType: "json",
                            success: function(json) {
                              var result = json.queryasyncjobresultresponse;
                              if (result.jobstatus == 0) {
                                return; //Job has not completed
                              }
                              else {
                                $("body").stopTime(timerKey);
                                if (result.jobstatus == 1) {
                                  nspMap["f5"] = json.queryasyncjobresultresponse.jobresult.networkserviceprovider;
                                  addExternalLoadBalancer(args, selectedPhysicalNetworkObj, "addF5LoadBalancer", "addf5bigiploadbalancerresponse");
                                }
                                else if (result.jobstatus == 2) {
                                  alert("addNetworkServiceProvider&name=F5BigIp failed. Error: " + fromdb(result.jobresult.errortext));
                                }
                              }
                            },
                            error: function(XMLHttpResponse) {
                              var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                              alert("addNetworkServiceProvider&name=F5BigIpfailed. Error: " + errorMsg);
                            }
                          });
                        });
                      }
                    });
                  }
                  else {
                    addExternalLoadBalancer(args, selectedPhysicalNetworkObj, "addF5LoadBalancer", "addf5bigiploadbalancerresponse");
                  }
                },
                messages: {
                  notification: function(args) {
                    return 'Added new F5';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },
              enable: {
                label: 'Enable provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["f5"].id + "&state=Enabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                         {
                           jobId: jid
                         }
                        }
                      );

                      $(window).trigger('cloudStack.fullRefresh');
                    }
                  });
                },
                messages: {
                  notification: function() { return 'Provider is enabled'; }
                },
                notification: { poll: pollAsyncJobResult }
              },
              disable: {
                label: 'Disable provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["f5"].id + "&state=Disabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                         {
                           jobId: jid
                         }
                        }
                      );

                      $(window).trigger('cloudStack.fullRefresh');
                    }
                  });
                },
                messages: {
                  notification: function() { return 'Provider is disabled'; }
                },
                notification: { poll: pollAsyncJobResult }
              },
              destroy: {
                label: 'Shutdown provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteNetworkServiceProvider&id=" + nspMap["f5"].id),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.deletenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                         {
                           jobId: jid
                         }
                        }
                      );

                      $(window).trigger('cloudStack.fullRefresh');
                    }
                  });
                },
                messages: {
                  notification: function() { return 'F5 provider is shutdown'; }
                },
                notification: { poll: pollAsyncJobResult }
              }
            }
          },

          // SRX list view
          srx: {
            type: 'detailView',
            id: 'srxProviders',
            label: 'SRX',
            viewAll: { label: 'Providers', path: '_zone.srxProviders' },
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    name: { label: 'Name' }
                  },
                  {
                    id: { label: 'ID' }
                  }
                ],
                dataProvider: function(args) {
                  args.response.success({
                    data: selectedPhysicalNetworkObj,
                    actionFilter: function(args) {
                      var allowedActions = [];
                      var jsonObj = nspMap["srx"];
                      if(jsonObj.state == "Enabled") {
                        allowedActions.push("disable");
                      }
                      else if(jsonObj.state == "Disabled") {
                        allowedActions.push("enable");
                      }
                      allowedActions.push("destroy");
                      return allowedActions;
                    }
                  });
                }
              }
            },
            actions: {
              add: {
                label: 'Add new SRX',
                createForm: {
                  title: 'Add new SRX',
                  fields: {
                    ip: {
                      label: 'IP address'
                    },
                    username: {
                      label: 'Username'
                    },
                    password: {
                      label: 'Password',
                      isPassword: true
                    },
                    networkdevicetype: {
                      label: 'Type',
                      select: function(args) {
                        var items = [];
                        items.push({id: "JuniperSRXFirewall", description: "Juniper SRX Firewall"});
                        args.response.success({data: items});
                      }
                    },
                    publicinterface: {
                      label: 'Public interface'
                    },
                    privateinterface: {
                      label: 'Private interface'
                    },
                    usageinterface: {
                      label: 'Usage interface'
                    },
                    numretries: {
                      label: 'Number of retries',
                      defaultValue: '2'
                    },
                    timeout: {
                      label: 'Timeout',
                      defaultValue: '300'
                    },
                    inline: {
                      label: 'Mode',
                      select: function(args) {
                        var items = [];
                        items.push({id: "false", description: "side by side"});
                        items.push({id: "true", description: "inline"});
                        args.response.success({data: items});
                      }
                    },
                    publicnetwork: {
                      label: 'Public network',
                      defaultValue: 'untrusted'
                    },
                    privatenetwork: {
                      label: 'Private network',
                      defaultValue: 'trusted'
                    },
                    capacity: {
                      label: 'Capacity',
                      validation: { required: false, number: true }
                    },
                    dedicated: {
                      label: 'Dedicated',
                      isBoolean: true,
                      isChecked: false
                    }
                  }
                },
                action: function(args) {
                  if(nspMap["srx"] == null) {
                    $.ajax({
                      url: createURL("addNetworkServiceProvider&name=JuniperSRX&physicalnetworkid=" + selectedPhysicalNetworkObj.id),
                      dataType: "json",
                      async: true,
                      success: function(json) {
                        var jobId = json.addnetworkserviceproviderresponse.jobid;
                        var timerKey = "addNetworkServiceProviderJob_"+jobId;
                        $("body").everyTime(2000, timerKey, function() {
                          $.ajax({
                            url: createURL("queryAsyncJobResult&jobId="+jobId),
                            dataType: "json",
                            success: function(json) {
                              var result = json.queryasyncjobresultresponse;
                              if (result.jobstatus == 0) {
                                return; //Job has not completed
                              }
                              else {
                                $("body").stopTime(timerKey);
                                if (result.jobstatus == 1) {
                                  nspMap["srx"] = json.queryasyncjobresultresponse.jobresult.networkserviceprovider;
                                  addExternalFirewall(args, selectedPhysicalNetworkObj, "addSrxFirewall", "addsrxfirewallresponse", "srxfirewall");
                                }
                                else if (result.jobstatus == 2) {
                                  alert("addNetworkServiceProvider&name=JuniperSRX failed. Error: " + fromdb(result.jobresult.errortext));
                                }
                              }
                            },
                            error: function(XMLHttpResponse) {
                              var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                              alert("addNetworkServiceProvider&name=JuniperSRX failed. Error: " + errorMsg);
                            }
                          });
                        });
                      }
                    });
                  }
                  else {
                    addExternalFirewall(args, selectedPhysicalNetworkObj, "addSrxFirewall", "addsrxfirewallresponse", "srxfirewall");
                  }
                },
                messages: {
                  notification: function(args) {
                    return 'Added new SRX';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },
              enable: {
                label: 'Enable provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["srx"].id + "&state=Enabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid
                          }
                        }
                      );

                      $(window).trigger('cloudStack.fullRefresh');
                    }
                  });
                },
                messages: {
                  notification: function() { return 'Provider is enabled'; }
                },
                notification: { poll: pollAsyncJobResult }
              },
              disable: {
                label: 'Disable provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["srx"].id + "&state=Disabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid
                          }
                        }
                      );

                      $(window).trigger('cloudStack.fullRefresh');
                    }
                  });
                },
                messages: {
                  notification: function() { return 'Provider is disabled'; }
                },
                notification: { poll: pollAsyncJobResult }
              },
              destroy: {
                label: 'Shutdown provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteNetworkServiceProvider&id=" + nspMap["srx"].id),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.deletenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid
                          }
                        }
                      );

                      $(window).trigger('cloudStack.fullRefresh');
                    }
                  });
                },
                messages: {
                  notification: function() { return 'Provider is shutdown'; }
                },
                notification: { poll: pollAsyncJobResult }
              }
            }
          },

          // Security groups provider
          securityGroups: {
            id: 'securityGroup-providers',
            label: 'Security Groups',
            type: 'detailView',
            viewAll: { label: 'Security Groups', path: 'network.securityGroups' },
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    name: { label: 'Name' }
                  },
                  {
                    state: { label: 'State' },
                    id: { label: 'ID' },
                    physicalnetworkid: { label: 'Physical network ID' }
                  }
                ],
                dataProvider: function(args) {
                  args.response.success({
                    actionFilter: function(args) {
                      var allowedActions = [];
                      var jsonObj = nspMap["securityGroups"];
                      if(jsonObj.state == "Enabled")
                        allowedActions.push("disable");
                      else if(jsonObj.state == "Disabled")
                        allowedActions.push("enable");
                      return allowedActions;
                    },
                    data: nspMap["securityGroups"]
                  });
                }
              }
            },
            actions: {
              enable: {
                label: 'Enable provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["securityGroups"].id + "&state=Enabled"),
                    async: true,
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid
                          }
                        }
                      );

                      $(window).trigger('cloudStack.fullRefresh');
                    }
                  });
                },
                messages: {
                  confirm: function() { return 'Are you sure you want to enable security groups?'; },
                  notification: function() { return 'Provider is enabled'; }
                },
                notification: { poll: pollAsyncJobResult }
              },
              disable: {
                label: 'Disable provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["securityGroups"].id + "&state=Disabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid
                          }
                        }
                      );

                      $(window).trigger('cloudStack.fullRefresh');
                    }
                  });
                },
                messages: {
                  confirm: function() { return 'Are you sure you want to disable security groups?'; },
                  notification: function() { return 'Provider is disabled'; }
                },
                notification: { poll: pollAsyncJobResult }
              }
            },

            fields: {
              id: { label: 'ID' },
              name: { label: 'Name' }//,
              //state: { label: 'Status' } //comment it for now, since dataProvider below doesn't get called by widget code after action is done
            }
          }
        }
      }
    },
    show: cloudStack.uiCustom.physicalResources({
      sections: {
        physicalResources: {
          type: 'select',
          title: 'Physical Resources',
          listView: {
            id: 'physicalResources',
            label: 'Physical Resources',
            fields: {
              name: { label: 'Zone' },
              networktype: { label: 'Network Type' },
              domainid: {
                label: 'Public',
                converter: function(args) {
                  if(args == null)
                    return "Yes";
                  else
                    return "No";
                }
              },
              allocationstate: { label: 'Allocation State', indicator: { 
                'Enabled': 'on',
                'Disabled': 'off'
              } }
            },
            actions: {
              add: {
                label: 'Add zone',
                action: {
                  custom: cloudStack.zoneWizard({
                    steps: [
                      // Step 1: Setup
                      null,

                      // Step 2: Setup Zone
                      function(args) {
                        $.ajax({
                          url: createURL("listDomains&listAll=true"),
                          dataType: "json",
                          async: false,
                          success: function(json) {
                            domainObjs = json.listdomainsresponse.domain;
                          }
                        });

												var networkOfferingObjsWithSG = [];
												var networkOfferingObjsWithoutSG = [];
                        $.ajax({
                          url: createURL("listNetworkOfferings&state=Enabled&guestiptype=Shared"),
                          dataType: "json",
                          async: false,
                          success: function(json) {
                            networkOfferingObjs = json.listnetworkofferingsresponse.networkoffering;
																	
														$(networkOfferingObjs).each(function() {														 
															var includingSGP = false;
															var serviceObjArray = this.service;
															for(var k = 0; k < serviceObjArray.length; k++) {
																if(serviceObjArray[k].name == "SecurityGroup") {
																	includingSGP = true;
																	break;
																}
															}															
															if(includingSGP == false) //without SG
																networkOfferingObjsWithoutSG.push(this);
                              else //with SG
                                networkOfferingObjsWithSG.push(this);															
														});														
                          }
                        });
                        
                        args.response.success({
                          domains: domainObjs,

                          // Non-security-group-enabled offerings
                          networkOfferings: networkOfferingObjsWithoutSG,

                          // Security group-enabled offerings
                          securityGroupNetworkOfferings: networkOfferingObjsWithSG
                        });
                      },

                      // Step 3: Setup Pod
                      null,

                      // Step 4: Setup IP Range
                      function(args) {
                        args.response.success({domains: domainObjs});
                      }
                    ],

                    action: function(args) {
                      var array1 = [];

                      //var networktype = $thisWizard.find("#step1").find("input:radio[name=basic_advanced]:checked").val();  //"Basic", "Advanced"
                      var networktype = args.data["network-model"];
                      array1.push("&networktype=" + todb(networktype));

                      array1.push("&name=" + todb(args.data.name));

                      array1.push("&dns1=" + todb(args.data.dns1));

                      var dns2 = args.data.dns2;
                      if (dns2 != null && dns2.length > 0)
                        array1.push("&dns2=" + todb(dns2));

                      array1.push("&internaldns1="+todb(args.data.internaldns1));

                      var internaldns2 = args.data.internaldns2;
                      if (internaldns2 != null && internaldns2.length > 0)
                        array1.push("&internaldns2=" + todb(internaldns2));

                      if(networktype == "Advanced") {
                        array1.push("&securitygroupenabled=false");  
                      }

                      if(args.data["public"] == null) //public checkbox is unchecked
                        array1.push("&domainid=" + args.data["zone-domain"]);
                     
                      if(args.data.networkdomain != null && args.data.networkdomain.length > 0)  
                        array1.push("&domain=" + todb(args.data.networkdomain));
                      
                      var newZoneObj; 
                      $.ajax({
                        url: createURL("createZone" + array1.join("")),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                          var newZoneObj = json.createzoneresponse.zone;
                          args.response.success({
                            data: newZoneObj,
                            _custom: { zone: newZoneObj }
                          }); //spinning wheel appears from this moment
                          
                          //NaaS (begin)
                          var newPhysicalnetwork;													
													$.ajax({
													  url: createURL("createPhysicalNetwork&zoneid=" + newZoneObj.id + "&name=firstPhysicalNetwork"),
														dataType: "json",														
														success: function(json) {														 
															var jobId = json.createphysicalnetworkresponse.jobid;
															var timerKey = "createPhysicalNetworkJob_" + jobId;
															$("body").everyTime(2000, timerKey, function(){															  
																$.ajax({
																  url: createURL("queryAsyncJobResult&jobid=" + jobId),
																	dataType: "json",
																	success: function(json) {
																	  var result = json.queryasyncjobresultresponse;																		
																		if (result.jobstatus == 0) {
																			return; //Job has not completed
																		}
																		else {
																			$("body").stopTime(timerKey);
																			if (result.jobstatus == 1) {																				
																				newPhysicalnetwork = result.jobresult.physicalnetwork;
																				
																				var trafficTypeCount = 0;																				
																				$.ajax({
																				  url: createURL("addTrafficType&trafficType=Public&physicalnetworkid=" + newPhysicalnetwork.id),
																					dataType: "json",
																					success: function(json) {																					  
																						var jobId = json.addtraffictyperesponse.jobid;
																						var timerKey = "addTrafficTypeJob_" + jobId;
                                            $("body").everyTime(2000, timerKey, function() {																						 
																							$.ajax({
																							  url: createURL("queryAsyncJobResult&jobid=" + jobId),
																								dataType: "json", 
																								success: function(json) {
																								  var result = json.queryasyncjobresultresponse;
																									if (result.jobstatus == 0) {
																										return; //Job has not completed
																									}
																									else {
																										$("body").stopTime(timerKey);
																										if (result.jobstatus == 1) {
																										  //debugger;
																											trafficTypeCount++;
																											if(trafficTypeCount == 3) //3 traffic types(Public, Management, Guest) have been added to this physical network
																											  afterCreateZonePhysicalNetworkTrafficTypes(newZoneObj, newPhysicalnetwork);
																										}
																										else if (result.jobstatus == 2) {
																											alert("addTrafficType&trafficType=Public failed. Error: " + fromdb(result.jobresult.errortext));
																										}
																									}	
																								},																								
																								error: function(XMLHttpResponse) {
																									var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
																									alert("addTrafficType&trafficType=Public failed. Error: " + errorMsg);
																								}
																							});																							
																						});	
																					}
																				});			

                                        $.ajax({
																				  url: createURL("addTrafficType&trafficType=Management&physicalnetworkid=" + newPhysicalnetwork.id),
																					dataType: "json",
																					success: function(json) {																					  
																						var jobId = json.addtraffictyperesponse.jobid;
																						var timerKey = "addTrafficTypeJob_" + jobId;
                                            $("body").everyTime(2000, timerKey, function() {																						  
																							$.ajax({
																							  url: createURL("queryAsyncJobResult&jobid=" + jobId),
																								dataType: "json", 
																								success: function(json) {
																								  var result = json.queryasyncjobresultresponse;
																									if (result.jobstatus == 0) {
																										return; //Job has not completed
																									}
																									else {
																										$("body").stopTime(timerKey);
																										if (result.jobstatus == 1) {
																										  //debugger;
																											trafficTypeCount++;
																											if(trafficTypeCount == 3) //3 traffic types(Public, Management, Guest) have been added to this physical network
																											  afterCreateZonePhysicalNetworkTrafficTypes(newZoneObj, newPhysicalnetwork);
																										}
																										else if (result.jobstatus == 2) {
																											alert("addTrafficType&trafficType=Management failed. Error: " + fromdb(result.jobresult.errortext));
																										}
																									}	
																								},																								
																								error: function(XMLHttpResponse) {
																									var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
																									alert("addTrafficType&trafficType=Management failed. Error: " + errorMsg);
																								}
																							});																							
																						});	
																					}
																				});							

                                        $.ajax({
																				  url: createURL("addTrafficType&trafficType=Guest&physicalnetworkid=" + newPhysicalnetwork.id),
																					dataType: "json",
																					success: function(json) {																					  
																						var jobId = json.addtraffictyperesponse.jobid;
																						var timerKey = "addTrafficTypeJob_" + jobId;
                                            $("body").everyTime(2000, timerKey, function() {																						  
																							$.ajax({
																							  url: createURL("queryAsyncJobResult&jobid=" + jobId),
																								dataType: "json", 
																								success: function(json) {
																								  var result = json.queryasyncjobresultresponse;
																									if (result.jobstatus == 0) {
																										return; //Job has not completed
																									}
																									else {
																										$("body").stopTime(timerKey);
																										if (result.jobstatus == 1) {
																										  //debugger;
																											trafficTypeCount++;
																											if(trafficTypeCount == 3) //3 traffic types(Public, Management, Guest) have been added to this physical network
																											  afterCreateZonePhysicalNetworkTrafficTypes(newZoneObj, newPhysicalnetwork);
																										}
																										else if (result.jobstatus == 2) {
																											alert("addTrafficType&trafficType=Guest failed. Error: " + fromdb(result.jobresult.errortext));
																										}
																									}	
																								},																								
																								error: function(XMLHttpResponse) {
																									var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
																									alert("addTrafficType&trafficType=Guest failed. Error: " + errorMsg);
																								}
																							});																							
																						});	
																					}
																				});												
																			}
																			else if (result.jobstatus == 2) {																			  
																				alert("createPhysicalNetwork failed. Error: " + fromdb(result.jobresult.errortext));
																			}
																		}		
																	},																																		
																	error: function(XMLHttpResponse) {
																		var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
																		alert("createPhysicalNetwork failed. Error: " + errorMsg);
																	}																			
																});															
															});															
														}
													});
                          //NaaS (end)
                        },
                        error: function(XMLHttpResponse) {
                          var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                          args.response.error(errorMsg);
                        }
                      });
                    }
                  })
                },
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to add a zone?';
                  },
                  notification: function(args) {
                    return 'Created new zone';
                  }
                },
                notification: {
                  poll: function(args) {
                    args.complete({
                      actionFilter: zoneActionfilter,
                      data: args._custom.zone
                    });
                  }
                }
              },

              // Enable swift
              enableSwift: {
                label: 'Configure Swift',
                isHeader: true,
                preFilter: function(args) {
                  var swiftEnabled = false;
                  
                  $.ajax({
                    url: createURL('listConfigurations'),
                    data: {
                      name: 'swift.enable'
                    },
                    async: false,
                    success: function(json) {
                      swiftEnabled = json.listconfigurationsresponse.configuration[0].value == 'true' ?
                        true : false;
                    }
                  });
                  
                  return swiftEnabled;
                },
                messages: {
                  notification: function(args) {
                    return 'Enabled Swift support';
                  }
                },
                createForm: {
                  desc: 'Please fill in the following information to enable support for Swift',
                  fields: {
                    url: { label: 'URL', validation: { required: true } },
                    account: { label: 'Account' },
                    username: { label: 'Username' },
                    key: { label: 'Key' }
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL('addSwift'),
                    data: {
                      url: args.data.url,
                      account: args.data.account,
                      username: args.data.username,
                      key: args.data.key
                    },
                    success: function(json) {
										  havingSwift = true;
                      args.response.success();
                    },
                    error: function(json) {
                      args.response.error(parseXMLHttpResponse(json));
                    }
                  });
                }
              },

              enable: {
                label: 'Enable zone',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to enable this zone?';
                  },
                  success: function(args) {
                    return 'This zone is being enabled.';
                  },
                  notification: function(args) {
                    return 'Enabling zone';
                  },
                  complete: function(args) {
                    return 'Zone has been enabled.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("updateZone&id=" + args.context.physicalResources[0].id + "&allocationstate=Enabled"),  //embedded objects in listView is called physicalResources while embedded objects in detailView is called zones
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var item = json.updatezoneresponse.zone;
                      args.response.success({
                        actionFilter: zoneActionfilter,
                        data:item
                      });
                    }
                  });
                },
                notification: {
                  poll: function(args) {
                    args.complete();
                  }
                }
              },

              disable: {
                label: 'Disable zone',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to disable this zone?';
                  },
                  success: function(args) {
                    return 'This zone is being disabled.';
                  },
                  notification: function(args) {
                    return 'Disabling zone';
                  },
                  complete: function(args) {
                    return 'Zone has been disabled.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("updateZone&id=" + args.context.physicalResources[0].id + "&allocationstate=Disabled"),  //embedded objects in listView is called physicalResources while embedded objects in detailView is called zones
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var item = json.updatezoneresponse.zone;
                      args.response.success({
                        actionFilter: zoneActionfilter,
                        data:item
                      });
                    }
                  });
                },
                notification: {
                  poll: function(args) {
                    args.complete();
                  }
                }
              },

              'delete': {
                label: 'Delete' ,
                messages: {
                  confirm: function(args) {
                    return 'Please confirm that you want to delete this zone.';
                  },
                  success: function(args) {
                    return 'Zone is being deleted.';
                  },
                  notification: function(args) {
                    return 'Deleting zone';
                  },
                  complete: function(args) {
                    return 'Zone has been deleted.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteZone&id=" + args.context.physicalResources[0].id),  //embedded objects in listView is called physicalResources while embedded objects in detailView is called zones
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      args.response.success({data:{}});
                    }
                  });
                },
                notification: {
                  poll: function(args) { args.complete(); }
                }
              }

            },

            dataProvider: function(args) {						  
							var array1 = [];  
							if(args.filterBy != null) {          
								if(args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
									switch(args.filterBy.search.by) {
									case "name":
										if(args.filterBy.search.value.length > 0)
											array1.push("&keyword=" + args.filterBy.search.value);
										break;
									}
								}
							}
							
              $.ajax({
                url: createURL("listZones&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
                dataType: "json",
                async: true,
                success: function(json) {
                  zoneObjs = json.listzonesresponse.zone;
                  args.response.success({
                    actionFilter: zoneActionfilter,
                    data:zoneObjs
                  });
                }
              });
            },
            
            detailView: {
              isMaximized: true,
              viewAll: {
                path: 'instances',
                label: 'Configuration',
                custom: cloudStack.zoneChart()
              },
              actions: {
                edit: {
                  label: 'Edit',
                  action: function(args) {
                    var array1 = [];
                    array1.push("&name="  +todb(args.data.name));
                    array1.push("&dns1=" + todb(args.data.dns1));
                    array1.push("&dns2=" + todb(args.data.dns2));  //dns2 can be empty ("") when passed to API
                    array1.push("&internaldns1=" + todb(args.data.internaldns1));
                    array1.push("&internaldns2=" + todb(args.data.internaldns2));  //internaldns2 can be empty ("") when passed to API
                    array1.push("&domain=" + todb(args.data.domain));
                    $.ajax({
                      url: createURL("updateZone&id=" + args.context.physicalResources[0].id + array1.join("")),
                      dataType: "json",
                      async: false,
                      success: function(json) {
                        selectedZoneObj = json.updatezoneresponse.zone; //override selectedZoneObj after update zone
                        args.response.success({data: selectedZoneObj});
                      },
                      error: function(json) {
                        args.response.error('Could not edit zone information; please ensure all fields are valid.');
                      }
                    });
                  }
                }
              },
              tabs: {
                details: {
                  title: 'Details',
                  fields: [
                    {
                      name: { label: 'Zone', isEditable: true }
                    },
                    {
                      id: { label: 'ID' },
                      allocationstate: { label: 'Allocation State' },
                      dns1: { label: 'DNS 1', isEditable: true },
                      dns2: { label: 'DNS 2', isEditable: true },
                      internaldns1: { label: 'Internal DNS 1', isEditable: true },
                      internaldns2: { label: 'Internal DNS 2', isEditable: true },
                      networktype: { label: 'Network Type' },
                      securitygroupsenabled: {
                        label: 'Security Groups Enabled',
                        converter:cloudStack.converters.toBooleanText
                      },
                      domain: {
                        label: 'Network domain',
                        isEditable: true
                      }
                    }
                  ],
                  dataProvider: function(args) {
                    $.ajax({
                      url: createURL('listZones'),
                      data: {
                        id: args.context.physicalResources[0].id
                      },
                      success: function(json) {
                        args.response.success({ data: json.listzonesresponse.zone[0] });
                      }
                    });
                  }
                },
                systemVMs: {
                  title: 'System VMs',
                  listView: {
                    label: 'System VMs',
                    id: 'systemVMs',
                    fields: {
                      name: { label: 'Name' },
                      systemvmtype: {
                        label: 'Type',
                        converter: function(args) {
                          if(args == "consoleproxy")
                            return "Console Proxy VM";
                          else if(args == "secondarystoragevm")
                            return "Secondary Storage VM";
                        }
                      },
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
                    dataProvider: function(args) {										  
											var array1 = [];  
											if(args.filterBy != null) {          
												if(args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
													switch(args.filterBy.search.by) {
													case "name":
														if(args.filterBy.search.value.length > 0)
															array1.push("&keyword=" + args.filterBy.search.value);
														break;
													}
												}
											}
																					
                      var selectedZoneObj = args.context.physicalResources[0];
                      $.ajax({
                        url: createURL("listSystemVms&zoneid=" + selectedZoneObj.id + "&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
                        dataType: "json",
                        async: true,
                        success: function(json) {
                          var items = json.listsystemvmsresponse.systemvm;
                          args.response.success({
                            actionFilter: systemvmActionfilter,
                            data: items
                          });
                        }
                      });
                    },
                    
                    detailView: {
                      name: 'System VM details',
                      actions: {
                        start: {
                          label: 'Start system VM',
                          messages: {
                            confirm: function(args) {
                              return 'Are you sure you want to start system VM?';
                            },
                            notification: function(args) {
                              return 'Starting system VM';
                            }
                          },
                          action: function(args) {													
                            $.ajax({
                              url: createURL('startSystemVm&id=' + args.context.systemVMs[0].id),
                              dataType: 'json',
                              async: true,
                              success: function(json) {
                                var jid = json.startsystemvmresponse.jobid;
                                args.response.success({
                                  _custom: {
                                    jobId: jid,
                                    getUpdatedItem: function(json) {
                                      return json.queryasyncjobresultresponse.jobresult.systemvm;
                                    },
                                    getActionFilter: function() {
                                      return systemvmActionfilter;
                                    }
                                  }
                                });
                              }
                            });
                          },
                          notification: {
                            poll: pollAsyncJobResult
                          }
                        },

                        stop: {
                          label: 'Stop system VM',
                          messages: {
                            confirm: function(args) {
                              return 'Are you sure you want to stop system VM?';
                            },
                            notification: function(args) {
                              return 'Stopping system VM';
                            }
                          },
                          action: function(args) {												
                            $.ajax({
                              url: createURL('stopSystemVm&id=' + args.context.systemVMs[0].id),
                              dataType: 'json',
                              async: true,
                              success: function(json) {
                                var jid = json.stopsystemvmresponse.jobid;
                                args.response.success({
                                  _custom: {
                                    jobId: jid,
                                    getUpdatedItem: function(json) {
                                      return json.queryasyncjobresultresponse.jobresult.systemvm;
                                    },
                                    getActionFilter: function() {
                                      return systemvmActionfilter;
                                    }
                                  }
                                });
                              }
                            });
                          },
                          notification: {
                            poll: pollAsyncJobResult
                          }
                        },

                        restart: {
                          label: 'reboot system VM',
                          messages: {
                            confirm: function(args) {
                              return 'Are you sure you want to reboot system VM?';
                            },
                            notification: function(args) {
                              return 'rebooting system VM';
                            }
                          },
                          action: function(args) {													 
                            $.ajax({
                              url: createURL('rebootSystemVm&id=' + args.context.systemVMs[0].id),
                              dataType: 'json',
                              async: true,
                              success: function(json) {
                                var jid = json.rebootsystemvmresponse.jobid;
                                args.response.success({
                                  _custom: {
                                    jobId: jid,
                                    getUpdatedItem: function(json) {
                                      return json.queryasyncjobresultresponse.jobresult.systemvm;
                                    },
                                    getActionFilter: function() {
                                      return systemvmActionfilter;
                                    }
                                  }
                                });
                              }
                            });
                          },
                          notification: {
                            poll: pollAsyncJobResult
                          }
                        },

                        'delete': {
                          label: 'Destroy system VM',
                          messages: {
                            confirm: function(args) {
                              return 'Are you sure you want to destroy this system VM?';
                            },
                            notification: function(args) {
                              return 'Destroyping system VM';
                            }
                          },
                          action: function(args) {												
                            $.ajax({
                              url: createURL('destroySystemVm&id=' + args.context.systemVMs[0].id),
                              dataType: 'json',
                              async: true,
                              success: function(json) {
                                var jid = json.destroysystemvmresponse.jobid;
                                args.response.success({
                                  _custom: {
                                    jobId: jid,
                                    getUpdatedItem: function(json) {
                                      //return {}; //nothing in this systemVM needs to be updated, in fact, this whole systemVM has being destroyed
                                    },
                                    getActionFilter: function() {
                                      return systemvmActionfilter;
                                    }
                                  }
                                });
                              }
                            });
                          },
                          notification: {
                            poll: pollAsyncJobResult
                          }
                        },

                        migrate: {
                          label: 'Migrate system VM',
                          messages: {
                            confirm: function(args) {
                              return 'Are you sure you want to migrate system VM?';
                            },
                            success: function(args) {
                              return 'System VM is being migrated.';
                            },
                            notification: function(args) {
                              return 'Migrating system VM';
                            },
                            complete: function(args) {
                              return 'System VM has been migrated.';
                            }
                          },
                          createForm: {
                            title: 'Migrate system VM',
                            desc: '',
                            fields: {
                              hostId: {
                                label: 'Host',
                                validation: { required: true },
                                select: function(args) {
                                  $.ajax({
                                    url: createURL("listHosts&VirtualMachineId=" + args.context.systemVMs[0].id),
                                    //url: createURL("listHosts"),	//for testing only, comment it out before checking in.
                                    dataType: "json",
                                    async: true,
                                    success: function(json) {
                                      var hostObjs = json.listhostsresponse.host;
                                      var items = [];
                                      $(hostObjs).each(function() {
                                        items.push({id: this.id, description: (this.name + ": " +(this.hasEnoughCapacity? "Available" : "Full"))});
                                      });
                                      args.response.success({data: items});
                                    }
                                  });
                                },
                                error: function(XMLHttpResponse) {
                                  var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                  args.response.error(errorMsg);
                                }
                              }
                            }
                          },
                          action: function(args) {													                           
                            $.ajax({
                              url: createURL("migrateSystemVm&hostid=" + args.data.hostId + "&virtualmachineid=" + args.context.systemVMs[0].id),
                              dataType: "json",
                              async: true,
                              success: function(json) {
                                var jid = json.migratesystemvmresponse.jobid;
                                args.response.success({
                                  _custom: {
                                    jobId: jid,
                                    getUpdatedItem: function(json) {
                                      //return json.queryasyncjobresultresponse.jobresult.systemvminstance;    //not all properties returned in systemvminstance
                                      $.ajax({
                                        url: createURL("listSystemVms&id=" + json.queryasyncjobresultresponse.jobresult.systemvminstance.id),
                                        dataType: "json",
                                        async: false,
                                        success: function(json) {
                                          var items = json.listsystemvmsresponse.systemvm;
                                          if(items != null && items.length > 0) {
                                            return items[0];
                                          }
                                        }
                                      });
                                    },
                                    getActionFilter: function() {
                                      return systemvmActionfilter;
                                    }
                                  }
                                });
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
                                return clientConsoleUrl + '?cmd=access&vm=' + args.context.systemVMs[0].id;
                              },
                              title: function(args) {
                                return "console";  //can't have space in window name in window.open()
                              },
                              width: 820,
                              height: 640
                            }
                          }
                        }
                      },
                      tabs: {
                        details: {
                          title: 'Details',
                          fields: [
                            {
                              name: { label: 'Name' }
                            },
                            {
                              id: { label: 'ID' },
                              state: { label: 'State' },
                              systemvmtype: {
                                label: 'Type',
                                converter: function(args) {
                                  if(args == "consoleproxy")
                                    return "Console Proxy VM";
                                  else if(args == "secondarystoragevm")
                                    return "Secondary Storage VM";

                                  return false;
                                }
                              },
                              zonename: { label: 'Zone' },
                              publicip: { label: 'Public IP' },
                              privateip: { label: 'Private IP' },
                              linklocalip: { label: 'Link local IP' },
                              hostname: { label: 'Host' },
                              gateway: { label: 'Gateway' },
                              created: { label: 'Created', converter: cloudStack.converters.toLocalDate },
                              activeviewersessions: { label: 'Active sessions' }
                            }
                          ],
                          dataProvider: function(args) {
                            args.response.success({
                              actionFilter: systemvmActionfilter,
                              data: args.jsonObj
                            });
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }),
    subsections: {
      // Provider list views
      netscalerProviders: {
        id: 'netscalerProviders',
        title: 'NetScaler',
        listView: {
          id: 'netscalerProviders',
          fields: {
            ipaddress: { label: 'IP Address' },
            lbdevicestate: { label: 'Status' }
          },
          dataProvider: function(args) {
            $.ajax({
              url: createURL("listNetscalerLoadBalancers&physicalnetworkid=" + selectedPhysicalNetworkObj.id),
              dataType: "json",
              async: false,
              success: function(json) {
                var items = json.listnetscalerloadbalancerresponse.netscalerloadbalancer;
                args.response.success({data: items});
              }
            });
          },
          actions: {
            add: {
              label: 'Add new NetScaler',
              createForm: {
                title: 'Add new NetScaler',
                fields: {
                  ip: {
                    label: 'IP address'
                  },
                  username: {
                    label: 'Username'
                  },
                  password: {
                    label: 'Password',
                    isPassword: true
                  },
                  networkdevicetype: {
                    label: 'Type',
                    select: function(args) {
                      var items = [];
                      items.push({id: "NetscalerMPXLoadBalancer", description: "NetScaler MPX LoadBalancer"});
                      items.push({id: "NetscalerVPXLoadBalancer", description: "NetScaler VPX LoadBalancer"});
                      items.push({id: "NetscalerSDXLoadBalancer", description: "NetScaler SDX LoadBalancer"});
                      args.response.success({data: items});
                    }
                  },
                  publicinterface: {
                    label: 'Public interface'
                  },
                  privateinterface: {
                    label: 'Private interface'
                  },
                  numretries: {
                    label: 'Number of retries',
                    defaultValue: '2'
                  },
                  inline: {
                    label: 'Mode',
                    select: function(args) {
                      var items = [];
                      items.push({id: "false", description: "side by side"});
                      items.push({id: "true", description: "inline"});
                      args.response.success({data: items});
                    }
                  },
                  capacity: {
                    label: 'Capacity',
                    validation: { required: false, number: true }
                  },
                  dedicated: {
                    label: 'Dedicated',
                    isBoolean: true,
                    isChecked: false
                  }
                }
              },
              action: function(args) {
                if(nspMap["netscaler"] == null) {
                  $.ajax({
                    url: createURL("addNetworkServiceProvider&name=Netscaler&physicalnetworkid=" + selectedPhysicalNetworkObj.id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jobId = json.addnetworkserviceproviderresponse.jobid;
                      var timerKey = "addNetworkServiceProviderJob_"+jobId;
                      $("body").everyTime(2000, timerKey, function() {
                        $.ajax({
                          url: createURL("queryAsyncJobResult&jobId="+jobId),
                          dataType: "json",
                          success: function(json) {
                            var result = json.queryasyncjobresultresponse;
                            if (result.jobstatus == 0) {
                              return; //Job has not completed
                            }
                            else {
                              $("body").stopTime(timerKey);
                              if (result.jobstatus == 1) {
                                nspMap["netscaler"] = result.jobresult.networkserviceprovider;
                                addExternalLoadBalancer(args, selectedPhysicalNetworkObj, "addNetscalerLoadBalancer", "addnetscalerloadbalancerresponse", "netscalerloadbalancer");
                              }
                              else if (result.jobstatus == 2) {
                                alert("addNetworkServiceProvider&name=Netscaler failed. Error: " + fromdb(result.jobresult.errortext));
                              }
                            }
                          },
                          error: function(XMLHttpResponse) {
                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                            alert("addNetworkServiceProvider&name=Netscaler failed. Error: " + errorMsg);
                          }
                        });
                      });
                    }
                  });
                }
                else {
                  addExternalLoadBalancer(args, selectedPhysicalNetworkObj, "addNetscalerLoadBalancer", "addnetscalerloadbalancerresponse", "netscalerloadbalancer");
                }
              },
              messages: {
                notification: function(args) {
                  return 'Added new NetScaler';
                }
              },
              notification: {
                poll: pollAsyncJobResult
              }
            }
          },
          detailView: {
            name: 'NetScaler details',
            actions: {
              'delete': {
                label: 'Delete NetScaler',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to delete this NetScaler?';
                  },
                  success: function(args) {
                    return 'NetScaler is being deleted.';
                  },
                  notification: function(args) {
                    return 'Deleting NetScaler';
                  },
                  complete: function(args) {
                    return 'NetScaler has been deleted.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteNetscalerLoadBalancer&lbdeviceid=" + args.context.netscalerProviders[0].lbdeviceid),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.deletenetscalerloadbalancerresponse.jobid;
                      args.response.success(
                        {_custom:
                         {jobId: jid}
                        }
                      );
                    },

                    error: function(json) {
                      args.response.error(parseXMLHttpResponse(json));
                    }
                  });
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              }
            },
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    lbdeviceid: { label: 'ID' },
                    ipaddress: { label: 'IP Address' },
                    lbdevicestate: { label: 'Status' },
                    lbdevicename: { label: 'Type' },
                    lbdevicecapacity: { label: 'Capacity' },
                    lbdevicededicated: {
                      label: 'Dedicated',
                      converter: cloudStack.converters.toBooleanText
                    },
                    inline: {
                      label: 'Mode',
                      converter: function(args) {
                        if(args == false)
                          return "side by side";
                        else //args == true
                          return "inline";
                      }
                    }
                  }
                ],
                dataProvider: function(args) {
                  args.response.success({data: args.context.netscalerProviders[0]});
                }
              }
            }
          }
        }
      },

      f5Providers: {
        id: 'f5Providers',
        title: 'F5',
        listView: {
          id: 'f5Providers',
          fields: {
            ipaddress: { label: 'IP Address' },
            lbdevicestate: { label: 'Status' }
          },
          actions: {
            add: {
              label: 'Add new F5',
              createForm: {
                title: 'Add F5',
                fields: {
                  ip: {
                    label: 'IP address'
                  },
                  username: {
                    label: 'Username'
                  },
                  password: {
                    label: 'Password',
                    isPassword: true
                  },
                  networkdevicetype: {
                    label: 'Type',
                    select: function(args) {
                      var items = [];
                      items.push({id: "F5BigIpLoadBalancer", description: "F5 Big Ip Load Balancer"});
                      args.response.success({data: items});
                    }
                  },
                  publicinterface: {
                    label: 'Public interface'
                  },
                  privateinterface: {
                    label: 'Private interface'
                  },
                  numretries: {
                    label: 'Number of retries',
                    defaultValue: '2'
                  },
                  inline: {
                    label: 'Mode',
                    select: function(args) {
                      var items = [];
                      items.push({id: "false", description: "side by side"});
                      items.push({id: "true", description: "inline"});
                      args.response.success({data: items});
                    }
                  },
                  capacity: {
                    label: 'Capacity',
                    validation: { required: false, number: true }
                  },
                  dedicated: {
                    label: 'Dedicated',
                    isBoolean: true,
                    isChecked: false
                  }
                }
              },
              action: function(args) {
                if(nspMap["f5"] == null) {
                  $.ajax({
                    url: createURL("addNetworkServiceProvider&name=F5BigIp&physicalnetworkid=" + selectedPhysicalNetworkObj.id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jobId = json.addnetworkserviceproviderresponse.jobid;
                      var timerKey = "addNetworkServiceProviderJob_"+jobId;
                      $("body").everyTime(2000, timerKey, function() {
                        $.ajax({
                          url: createURL("queryAsyncJobResult&jobId="+jobId),
                          dataType: "json",
                          success: function(json) {
                            var result = json.queryasyncjobresultresponse;
                            if (result.jobstatus == 0) {
                              return; //Job has not completed
                            }
                            else {
                              $("body").stopTime(timerKey);
                              if (result.jobstatus == 1) {
                                nspMap["f5"] = json.queryasyncjobresultresponse.jobresult.networkserviceprovider;
                                addExternalLoadBalancer(args, selectedPhysicalNetworkObj, "addF5LoadBalancer", "addf5bigiploadbalancerresponse", "f5loadbalancer");
                              }
                              else if (result.jobstatus == 2) {
                                alert("addNetworkServiceProvider&name=F5BigIp failed. Error: " + fromdb(result.jobresult.errortext));
                              }
                            }
                          },
                          error: function(XMLHttpResponse) {
                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                            alert("addNetworkServiceProvider&name=F5BigIpfailed. Error: " + errorMsg);
                          }
                        });
                      });
                    }
                  });
                }
                else {
                  addExternalLoadBalancer(args, selectedPhysicalNetworkObj, "addF5LoadBalancer", "addf5bigiploadbalancerresponse", "f5loadbalancer");
                }
              },
              messages: {
                notification: function(args) {
                  return 'Added new F5';
                }
              },
              notification: {
                poll: pollAsyncJobResult
              }
            }
          },
          dataProvider: function(args) {
            $.ajax({
              url: createURL("listF5LoadBalancers&physicalnetworkid=" + selectedPhysicalNetworkObj.id),
              dataType: "json",
              async: false,
              success: function(json) {
                var items = json.listf5loadbalancerresponse.f5loadbalancer;
                args.response.success({data: items});
              }
            });
          },
          detailView: {
            name: 'F5 details',
            actions: {
              'delete': {
                label: 'Delete F5',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to delete this F5?';
                  },
                  success: function(args) {
                    return 'F5 is being deleted.';
                  },
                  notification: function(args) {
                    return 'Deleting F5';
                  },
                  complete: function(args) {
                    return 'F5 has been deleted.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteF5LoadBalancer&lbdeviceid=" + args.context.f5Providers[0].lbdeviceid),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.deletef5loadbalancerresponse.jobid;
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
              }
            },
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    lbdeviceid: { label: 'ID' },
                    ipaddress: { label: 'IP Address' },
                    lbdevicestate: { label: 'Status' },
                    lbdevicename: { label: 'Type' },
                    lbdevicecapacity: { label: 'Capacity' },
                    lbdevicededicated: {
                      label: 'Dedicated',
                      converter: cloudStack.converters.toBooleanText
                    },
                    inline: {
                      label: 'Mode',
                      converter: function(args) {
                        if(args == false)
                          return "side by side";
                        else //args == true
                          return "inline";
                      }
                    }
                  }
                ],
                dataProvider: function(args) {
                  args.response.success({data: args.context.f5Providers[0]});
                }
              }
            }
          }
        }
      },

      srxProviders: {
        id: 'srxProviders',
        title: 'SRX',
        listView: {
          id: 'srxProviders',
          fields: {
            ipaddress: { label: 'IP Address' },
            lbdevicestate: { label: 'Status' }
          },
          actions: {
            add: {
              label: 'Add new SRX',
              createForm: {
                title: 'Add new SRX',
                fields: {
                  ip: {
                    label: 'IP address'
                  },
                  username: {
                    label: 'Username'
                  },
                  password: {
                    label: 'Password',
                    isPassword: true
                  },
                  networkdevicetype: {
                    label: 'Type',
                    select: function(args) {
                      var items = [];
                      items.push({id: "JuniperSRXFirewall", description: "Juniper SRX Firewall"});
                      args.response.success({data: items});
                    }
                  },
                  publicinterface: {
                    label: 'Public interface'
                  },
                  privateinterface: {
                    label: 'Private interface'
                  },
                  usageinterface: {
                    label: 'Usage interface'
                  },
                  numretries: {
                    label: 'Number of retries',
                    defaultValue: '2'
                  },
                  timeout: {
                    label: 'Timeout',
                    defaultValue: '300'
                  },
                  inline: {
                    label: 'Mode',
                    select: function(args) {
                      var items = [];
                      items.push({id: "false", description: "side by side"});
                      items.push({id: "true", description: "inline"});
                      args.response.success({data: items});
                    }
                  },
                  publicnetwork: {
                    label: 'Public network',
                    defaultValue: 'untrusted'
                  },
                  privatenetwork: {
                    label: 'Private network',
                    defaultValue: 'trusted'
                  },
                  capacity: {
                    label: 'Capacity',
                    validation: { required: false, number: true }
                  },
                  dedicated: {
                    label: 'Dedicated',
                    isBoolean: true,
                    isChecked: false
                  }
                }
              },
              action: function(args) {
                if(nspMap["srx"] == null) {
                  $.ajax({
                    url: createURL("addNetworkServiceProvider&name=JuniperSRX&physicalnetworkid=" + selectedPhysicalNetworkObj.id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jobId = json.addnetworkserviceproviderresponse.jobid;
                      var timerKey = "addNetworkServiceProviderJob_"+jobId;
                      $("body").everyTime(2000, timerKey, function() {
                        $.ajax({
                          url: createURL("queryAsyncJobResult&jobId="+jobId),
                          dataType: "json",
                          success: function(json) {
                            var result = json.queryasyncjobresultresponse;
                            if (result.jobstatus == 0) {
                              return; //Job has not completed
                            }
                            else {
                              $("body").stopTime(timerKey);
                              if (result.jobstatus == 1) {
                                nspMap["srx"] = json.queryasyncjobresultresponse.jobresult.networkserviceprovider;
                                addExternalFirewall(args, selectedPhysicalNetworkObj, "addSrxFirewall", "addsrxfirewallresponse", "srxfirewall");
                              }
                              else if (result.jobstatus == 2) {
                                alert("addNetworkServiceProvider&name=JuniperSRX failed. Error: " + fromdb(result.jobresult.errortext));
                              }
                            }
                          },
                          error: function(XMLHttpResponse) {
                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                            alert("addNetworkServiceProvider&name=JuniperSRX failed. Error: " + errorMsg);
                          }
                        });
                      });
                    }
                  });
                }
                else {
                  addExternalFirewall(args, selectedPhysicalNetworkObj, "addSrxFirewall", "addsrxfirewallresponse", "srxfirewall");
                }
              },
              messages: {
                notification: function(args) {
                  return 'Added new SRX';
                }
              },
              notification: {
                poll: pollAsyncJobResult
              }
            }
          },
          dataProvider: function(args) {
            $.ajax({
              url: createURL("listSrxFirewalls&physicalnetworkid=" + selectedPhysicalNetworkObj.id),
              dataType: "json",
              async: false,
              success: function(json) {
                var items = json.listsrxfirewallresponse.srxfirewall;
                args.response.success({data: items});
              }
            });
          },
          detailView: {
            name: 'SRX details',
            actions: {
              'delete': {
                label: 'Delete SRX',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to delete this SRX?';
                  },
                  success: function(args) {
                    return 'SRX is being deleted.';
                  },
                  notification: function(args) {
                    return 'Deleting SRX';
                  },
                  complete: function(args) {
                    return 'SRX has been deleted.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteSrcFirewall&fwdeviceid=" + args.context.srxProviders[0].fwdeviceid),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.deletesrxfirewallresponse.jobid;
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
              }
            },
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    fwdeviceid: { label: 'ID' },
                    ipaddress: { label: 'IP Address' },
                    fwdevicestate: { label: 'Status' },
                    fwdevicename: { label: 'Type' },
                    fwdevicecapacity: { label: 'Capacity' },
                    timeout: { label: 'Timeout' }
                  }
                ],
                dataProvider: function(args) {
                  args.response.success({data: args.context.srxProviders[0]});
                }
              }
            }
          }
        }
      },
     
      pods: {
        title: 'Pods',
        listView: {
          id: 'pods',
          section: 'pods',
          fields: {
            name: { label: 'Name' },
            gateway: { label: 'Gateway' },
            netmask: { label: 'Netmask' },
            allocationstate: { label: 'Allocation Status' }
          },

          dataProvider: function(args) {					  
						var array1 = [];  
						if(args.filterBy != null) {          
							if(args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
								switch(args.filterBy.search.by) {
								case "name":
									if(args.filterBy.search.value.length > 0)
										array1.push("&keyword=" + args.filterBy.search.value);
									break;
								}
							}
						}
					
            $.ajax({
              url: createURL("listPods&zoneid=" + args.context.zones[0].id + "&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listpodsresponse.pod;
                args.response.success({
                  actionFilter: podActionfilter,
                  data:items
                });
              }
            });
          },

          actions: {
            add: {
              label: 'Add pod',

              createForm: {
                title: 'Add new pod',
                fields: {
                  podname: {
                    label: 'Pod name',
                    validation: { required: true }
                  },
                  reservedSystemGateway: {
                    label: 'Reserved system gateway',
                    validation: { required: true }
                  },
                  reservedSystemNetmask: {
                    label: 'Reserved system netmask',
                    validation: { required: true }
                  },
                  reservedSystemStartIp: {
                    label: 'Start Reserved system IP',
                    validation: { required: true }
                  },
                  reservedSystemEndIp: {
                    label: 'End Reserved system IP',
                    validation: { required: false }
                  }
                }
              },

              action: function(args) {
                var array1 = [];
                array1.push("&zoneId=" + args.context.zones[0].id);
                array1.push("&name=" + todb(args.data.podname));
                array1.push("&gateway=" + todb(args.data.reservedSystemGateway));
                array1.push("&netmask=" + todb(args.data.reservedSystemNetmask));
                array1.push("&startIp=" + todb(args.data.reservedSystemStartIp));

                var endip = args.data.reservedSystemEndIp;      //optional
                if (endip != null && endip.length > 0)
                  array1.push("&endIp=" + todb(endip));

                $.ajax({
                  url: createURL("createPod" + array1.join("")),
                  dataType: "json",
                  success: function(json) {
                    var item = json.createpodresponse.pod;										
                    args.response.success({
										  data:item
										});
                  },
                  error: function(XMLHttpResponse) {
                    var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                    args.response.error(errorMsg);
                  }
                });
              },

              notification: {
                poll: function(args) {
                  args.complete({
                    actionFilter: podActionfilter
                  });
                }
              },

              messages: {
                notification: function(args) {
                  return 'Added new pod';
                }
              }
            }    
          },

          detailView: {
            viewAll: { path: '_zone.clusters', label: 'Clusters' },
            tabFilter: function(args) {
              var hiddenTabs = [];
              if(selectedZoneObj.networktype == "Basic") { //basic-mode network (pod-wide VLAN)
                //$("#tab_ipallocation, #add_iprange_button, #tab_network_device, #add_network_device_button").show();
              }
              else if(selectedZoneObj.networktype == "Advanced") { //advanced-mode network (zone-wide VLAN)
                //$("#tab_ipallocation, #add_iprange_button, #tab_network_device, #add_network_device_button").hide();
                hiddenTabs.push("ipAllocations");
                //hiddenTabs.push("networkDevices"); //network devices tab is moved out of pod page at 3.0 UI. It will go to new network page.
              }
              return hiddenTabs;
            },
            actions: {
              edit: {
                label: 'Edit',
                action: function(args) {
                  var array1 = [];
                  array1.push("&name="  +todb(args.data.name));
                  array1.push("&netmask=" + todb(args.data.netmask));
                  array1.push("&startIp=" + todb(args.data.startip));
                  if(args.data.endip != null && args.data.endip.length > 0)
                    array1.push("&endIp=" + todb(args.data.endip));
                  if(args.data.gateway != null && args.data.gateway.length > 0)
                    array1.push("&gateway=" + todb(args.data.gateway));

                  $.ajax({
                    url: createURL("updatePod&id=" + args.context.pods[0].id + array1.join("")),
                    dataType: "json",
                    success: function(json) {
                      var item = json.updatepodresponse.pod;
                      args.response.success({
                        actionFilter: podActionfilter,
                        data:item
                      });
                    }
                  });
                }
              },

              enable: {
                label: 'Enable pod',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to enable this pod?';
                  },
                  success: function(args) {
                    return 'This pod is being enabled.';
                  },
                  notification: function(args) {
                    return 'Enabling pod';
                  },
                  complete: function(args) {
                    return 'Pod has been enabled.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("updatePod&id=" + args.context.pods[0].id + "&allocationstate=Enabled"),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var item = json.updatepodresponse.pod;
                      args.response.success({
                        actionFilter: podActionfilter,
                        data:item
                      });
                    }
                  });
                },
                notification: {
                  poll: function(args) {
                    args.complete();
                  }
                }
              },

              disable: {
                label: 'Disable pod',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to disable this pod?';
                  },
                  success: function(args) {
                    return 'This pod is being disabled.';
                  },
                  notification: function(args) {
                    return 'Disabling pod';
                  },
                  complete: function(args) {
                    return 'Pod has been disabled.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("updatePod&id=" + args.context.pods[0].id + "&allocationstate=Disabled"),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var item = json.updatepodresponse.pod;
                      args.response.success({
                        actionFilter: podActionfilter,
                        data:item
                      });
                    }
                  });
                },
                notification: {
                  poll: function(args) {
                    args.complete();
                  }
                }
              },

              'delete': {
                label: 'Delete' ,
                messages: {
                  confirm: function(args) {
                    return 'Please confirm that you want to delete this pod.';
                  },
                  success: function(args) {
                    return 'pod is being deleted.';
                  },
                  notification: function(args) {
                    return 'Deleting pod';
                  },
                  complete: function(args) {
                    return 'Pod has been deleted.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deletePod&id=" + args.context.pods[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      args.response.success({data:{}});
                    }
                  });
                },
                notification: {
                  poll: function(args) { args.complete(); }
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
                    netmask: { label: 'Netmask', isEditable: true },
                    startip: { label: 'Start IP Range', isEditable: true },
                    endip: { label: 'End IP Range', isEditable: true },
                    gateway: { label: 'Gateway', isEditable: true },
                    allocationstate: { label: 'Allocation Status' }
                  }
                ],

                dataProvider: function(args) {
                  args.response.success({
                    actionFilter: podActionfilter,
                    data: args.context.pods[0]
                  });
                }
              },

              ipAllocations: {
                title: 'IP Allocations',
                multiple: true,
                fields: [
                  {
                    id: { label: 'ID' },
                    gateway: { label: 'Gateway' },
                    netmask: { label: 'Netmask' },
                    startip: { label: 'Start IP range' },
                    endip: { label: 'End IP range' }
                  }
                ],
                dataProvider: function(args) {
                  $.ajax({
                    url: createURL("listVlanIpRanges&zoneid=" + args.context.zones[0].id + "&podid=" + args.context.pods[0].id),
                    dataType: "json",
                    success: function(json) {
                      var items = json.listvlaniprangesresponse.vlaniprange;
                      args.response.success({data: items});
                    }
                  });
                }
              }
            }
          }
        }
      },
      clusters: {
        title: 'Clusters',
        listView: {
          id: 'clusters',
          section: 'clusters',
          fields: {
            name: { label: 'Name' },
            podname: { label: 'Pod' },
            hypervisortype: { label: 'Hypervisor' },
            //allocationstate: { label: 'Allocation State' },
            //managedstate: { label: 'Managed State' },
						state: { label: 'State', indicator: { 'Enabled': 'on', 'Destroyed': 'off'} }
          },

          dataProvider: function(args) {
            var array1 = [];						
						if(args.filterBy != null) {          
							if(args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
								switch(args.filterBy.search.by) {
								case "name":
									if(args.filterBy.search.value.length > 0)
										array1.push("&keyword=" + args.filterBy.search.value);
									break;
								}
							}
						}						
            array1.push("&zoneid=" + args.context.zones[0].id);
            if("pods" in args.context)
              array1.push("&podid=" + args.context.pods[0].id);
            $.ajax({
              url: createURL("listClusters" + array1.join("") + "&page=" + args.page + "&pagesize=" + pageSize),
              dataType: "json",
              async: true,
              success: function(json) {                
                var items = json.listclustersresponse.cluster;								
								$(items).each(function(){								  
									if(this.managedstate == "Managed") {
									  this.state = this.allocationstate; //this.state == Enabled, Disabled
									}	
                  else {
                    this.state = this.managedstate; //this.state == Unmanaged, PrepareUnmanaged, PrepareUnmanagedError
                  }									
								});
																
                args.response.success({
                  actionFilter: clusterActionfilter,
                  data:items
                });
              }
            });
          },

          actions: {
            add: {
              label: 'Add cluster',

              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to add a cluster?';
                },
                success: function(args) {
                  return 'Your new cluster is being created.';
                },
                notification: function(args) {
                  return 'Creating new cluster';
                },
                complete: function(args) {
                  return 'Cluster has been created successfully!';
                }
              },

              createForm: {
                title: 'Add cluster',
                desc: 'Please fill in the following data to add a new cluster.',
                fields: {
                  hypervisor: {
                    label: 'Hypervisor',
                    select: function(args) {
                      $.ajax({
                        url: createURL("listHypervisors"),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                          var hypervisors = json.listhypervisorsresponse.hypervisor;
                          var items = [];
                          $(hypervisors).each(function() {
                            items.push({id: this.name, description: this.name})
                          });
                          args.response.success({data: items});
                        }
                      });

                      args.$select.bind("change", function(event) {
                        var $form = $(this).closest('form');
                        if($(this).val() == "VMware") {
                          //$('li[input_sub_group="external"]', $dialogAddCluster).show();
                          $form.find('.form-item[rel=vCenterHost]').css('display', 'inline-block');
                          $form.find('.form-item[rel=vCenterUsername]').css('display', 'inline-block');
                          $form.find('.form-item[rel=vCenterPassword]').css('display', 'inline-block');
                          $form.find('.form-item[rel=vCenterDatacenter]').css('display', 'inline-block');

                          //$("#cluster_name_label", $dialogAddCluster).text("vCenter Cluster:");
                        }
                        else {
                          //$('li[input_group="vmware"]', $dialogAddCluster).hide();
                          $form.find('.form-item[rel=vCenterHost]').css('display', 'none');
                          $form.find('.form-item[rel=vCenterUsername]').css('display', 'none');
                          $form.find('.form-item[rel=vCenterPassword]').css('display', 'none');
                          $form.find('.form-item[rel=vCenterDatacenter]').css('display', 'none');

                          //$("#cluster_name_label", $dialogAddCluster).text("Cluster:");
                        }
                      });
                    }
                  },
                  podId: {
                    label: 'Pod',
                    select: function(args) {
                      $.ajax({
                        url: createURL("listPods&zoneid=" + args.context.zones[0].id),
                        dataType: "json",
                        async: true,
                        success: function(json) {
                          var pods = json.listpodsresponse.pod;
                          var items = [];
                          $(pods).each(function() {													  
														if(("pods" in args.context) && (this.id == args.context.pods[0].id))
													    items.unshift({id: this.id, description: this.name});
													  else														
                              items.push({id: this.id, description: this.name});
                          });
                          args.response.success({data: items});
                        }
                      });
                    }
                  },
                  name: {
                    label: 'Cluster Name',
                    validation: { required: true }
                  },

                  //hypervisor==VMWare begins here
                  vCenterHost: {
                    label: 'vCenter Host',
                    validation: { required: true }
                  },
                  vCenterUsername: {
                    label: 'vCenter Username',
                    validation: { required: true }
                  },
                  vCenterPassword: {
                    label: 'vCenter Password',
                    validation: { required: true },
                    isPassword: true
                  },
                  vCenterDatacenter: {
                    label: 'vCenter Datacenter',
                    validation: { required: true }
                  }
                  //hypervisor==VMWare ends here
                }
              },

              action: function(args) {
                var array1 = [];
                array1.push("&zoneId=" + args.context.zones[0].id);
                array1.push("&hypervisor=" + args.data.hypervisor);

                var clusterType;
                if(args.data.hypervisor == "VMware")
                  clusterType="ExternalManaged";
                else
                  clusterType="CloudManaged";
                array1.push("&clustertype=" + clusterType);

                array1.push("&podId=" + args.data.podId);

                var clusterName = args.data.name;
                if(args.data.hypervisor == "VMware") {
                  array1.push("&username=" + todb(args.data.vCenterUsername));
                  array1.push("&password=" + todb(args.data.vCenterPassword));

                  var hostname = args.data.vCenterHost;
                  var dcName = args.data.vCenterDatacenter;

                  var url;
                  if(hostname.indexOf("http://") == -1)
                    url = "http://" + hostname;
                  else
                    url = hostname;
                  url += "/" + dcName + "/" + clusterName;
                  array1.push("&url=" + todb(url));

                  clusterName = hostname + "/" + dcName + "/" + clusterName; //override clusterName
                }
                array1.push("&clustername=" + todb(clusterName));

                $.ajax({
                  url: createURL("addCluster" + array1.join("")),
                  dataType: "json",
                  async: true,
                  success: function(json) {
                    var item = json.addclusterresponse.cluster[0];
                    args.response.success({
										  data: $.extend(item, { state: 'Enabled' })
										});  
                  },
                  error: function(XMLHttpResponse) {
                    var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                    args.response.error(errorMsg);
                  }
                });
              },

              notification: {
                poll: function(args) {
                  args.complete({
										actionFilter: clusterActionfilter,
                    data: { state: 'Enabled' }
                  });
                }
              }
            }
          },
					
          detailView: {
            viewAll: { path: '_zone.hosts', label: 'Hosts' },

            actions: {
              enable: {
                label: 'Enable cluster',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to enable this cluster?';
                  },
                  success: function(args) {
                    return 'This cluster is being enabled.';
                  },
                  notification: function(args) {
                    return 'Enabling cluster';
                  },
                  complete: function(args) {
                    return 'Cluster has been enabled.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("updateCluster&id=" + args.context.clusters[0].id + "&allocationstate=Enabled"),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var item = json.updateclusterresponse.cluster;
                      args.response.success({
                        actionFilter: clusterActionfilter,
                        data:item
                      });
                    }
                  });
                },
                notification: {
                  poll: function(args) {
                    args.complete();
                  }
                }
              },

              disable: {
                label: 'Disable cluster',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to disable this cluster?';
                  },
                  success: function(args) {
                    return 'This cluster is being disabled.';
                  },
                  notification: function(args) {
                    return 'Disabling cluster';
                  },
                  complete: function(args) {
                    return 'Cluster has been disabled.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("updateCluster&id=" + args.context.clusters[0].id + "&allocationstate=Disabled"),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var item = json.updateclusterresponse.cluster;
                      args.response.success({
                        actionFilter: clusterActionfilter,
                        data:item
                      });
                    }
                  });
                },
                notification: {
                  poll: function(args) {
                    args.complete();
                  }
                }
              },

              manage: {
                label: 'Manage cluster',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to manage this cluster?';
                  },
                  success: function(args) {
                    return 'This cluster is being managed.';
                  },
                  notification: function(args) {
                    return 'Managing cluster';
                  },
                  complete: function(args) {
                    return 'Cluster has been managed.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("updateCluster&id=" + args.context.clusters[0].id + "&managedstate=Managed"),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var item = json.updateclusterresponse.cluster;
                      args.response.success({
                        actionFilter: clusterActionfilter,
                        data:item
                      });
                    }
                  });
                },
                notification: {
                  poll: function(args) {
                    args.complete();
                  }
                }
              },

              unmanage: {
                label: 'Unmanage cluster',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to unmanage this cluster?';
                  },
                  success: function(args) {
                    return 'This cluster is being unmanaged.';
                  },
                  notification: function(args) {
                    return 'Unmanaging cluster';
                  },
                  complete: function(args) {
                    return 'Cluster has been unmanaged.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("updateCluster&id=" + args.context.clusters[0].id + "&managedstate=Unmanaged"),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var item = json.updateclusterresponse.cluster;
                      args.response.success({
                        actionFilter: clusterActionfilter,
                        data:item
                      });
                    }
                  });
                },
                notification: {
                  poll: function(args) {
                    args.complete();
                  }
                }
              },

              'delete': {
                label: 'Delete' ,
                messages: {
                  confirm: function(args) {
                    return 'Please confirm that you want to delete this cluster.';
                  },
                  success: function(args) {
                    return 'Cluster is being deleted.';
                  },
                  notification: function(args) {
                    return 'Deleting cluster';
                  },
                  complete: function(args) {
                    return 'Cluster has been deleted.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteCluster&id=" + args.context.clusters[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      args.response.success({data:{}});
                    }
                  });
                },
                notification: {
                  poll: function(args) { args.complete(); }
                }
              }
            },

            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    name: { label: 'Name' }
                  },
                  {
                    id: { label: 'ID' },
                    zonename: { label: 'Zone' },
                    podname: { label: 'Pod' },
                    hypervisortype: { label: 'Hypervisor' },
                    clustertype: { label: 'Cluster type' },
                    //allocationstate: { label: 'Allocation State' },
                    //managedstate: { label: 'Managed State' },
										state: { label: 'State' }
                  }
                ],                
                dataProvider: function(args) {
                  args.response.success({
                    actionFilter: clusterActionfilter,
                    data: args.context.clusters[0]
                  });
                }
              }
            }
          }
        }
      },
      hosts: {
        title: 'Hosts',
        id: 'hosts',
        listView: {
          section: 'hosts',
          id: 'hosts',
          fields: {
            name: { label: 'Name' },
            zonename: { label: 'Zone' },
            podname: { label: 'Pod' },
            clustername: { label: 'Cluster' }
          },

          dataProvider: function(args) {
            var array1 = [];						
						if(args.filterBy != null) {          
							if(args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
								switch(args.filterBy.search.by) {
								case "name":
									if(args.filterBy.search.value.length > 0)
										array1.push("&keyword=" + args.filterBy.search.value);
									break;
								}
							}
						}						
            array1.push("&zoneid=" + args.context.zones[0].id);
            if("pods" in args.context)
              array1.push("&podid=" + args.context.pods[0].id);
            if("clusters" in args.context)
              array1.push("&clusterid=" + args.context.clusters[0].id);
            $.ajax({
              url: createURL("listHosts&type=Routing" + array1.join("") + "&page=" + args.page + "&pagesize=" + pageSize),
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listhostsresponse.host;
                args.response.success({
                  actionFilter: hostActionfilter,
                  data: items
                });
              }
            });
          },

          actions: {
            add: {
              label: 'Add host',

              createForm: {
                title: 'Add new host',
                desc: 'Please fill in the following information to add a new host for the specified zone configuration.',
                fields: {
                  //always appear (begin)
                  podId: {
                    label: 'Pod',
                    validation: { required: true },
                    select: function(args) {
                      $.ajax({
                        url: createURL("listPods&zoneid=" + args.context.zones[0].id),
                        dataType: "json",
                        async: true,
                        success: function(json) {
                          var pods = json.listpodsresponse.pod;
                          var items = [];
                          $(pods).each(function() {													 
														if(("pods" in args.context) && (this.id == args.context.pods[0].id))
													    items.unshift({id: this.id, description: this.name});
													  else															
                              items.push({id: this.id, description: this.name});
                          });
                          args.response.success({data: items});
                        }
                      });
                    }
                  },

                  clusterId: {
                    label: 'Cluster',
                    validation: { required: true },
                    dependsOn: 'podId',
                    select: function(args) {
                      $.ajax({
                        url: createURL("listClusters&podid=" + args.podId),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                          clusterObjs = json.listclustersresponse.cluster;
                          var items = [];
                          $(clusterObjs).each(function() {													  
													  if(("clusters" in args.context) && (this.id == args.context.clusters[0].id))
													    items.unshift({id: this.id, description: this.name});
													  else
                              items.push({id: this.id, description: this.name});
                          });
                          args.response.success({data: items});
                        }
                      });

                      args.$select.change(function() {
                        var $form = $(this).closest('form');

                        var clusterId = $(this).val();
                        if(clusterId == null)
                          return;

                        var items = [];
                        $(clusterObjs).each(function(){
                          if(this.id == clusterId){
                            selectedClusterObj = this;
                            return false; //break the $.each() loop
                          }
                        });
                        if(selectedClusterObj == null)
                          return;

                        if(selectedClusterObj.hypervisortype == "VMware") {
                          //$('li[input_group="general"]', $dialogAddHost).hide();
                          $form.find('.form-item[rel=hostname]').hide();
                          $form.find('.form-item[rel=username]').hide();
                          $form.find('.form-item[rel=password]').hide();

                          //$('li[input_group="vmware"]', $dialogAddHost).show();
                          $form.find('.form-item[rel=vcenterHost]').css('display', 'inline-block');

                          //$('li[input_group="baremetal"]', $dialogAddHost).hide();
                          $form.find('.form-item[rel=baremetalCpuCores]').hide();
                          $form.find('.form-item[rel=baremetalCpu]').hide();
                          $form.find('.form-item[rel=baremetalMemory]').hide();
                          $form.find('.form-item[rel=baremetalMAC]').hide();

                          //$('li[input_group="Ovm"]', $dialogAddHost).hide();
                          $form.find('.form-item[rel=agentUsername]').hide();
                          $form.find('.form-item[rel=agentPassword]').hide();
                        }
                        else if (selectedClusterObj.hypervisortype == "BareMetal") {
                          //$('li[input_group="general"]', $dialogAddHost).show();
                          $form.find('.form-item[rel=hostname]').css('display', 'inline-block');
                          $form.find('.form-item[rel=username]').css('display', 'inline-block');
                          $form.find('.form-item[rel=password]').css('display', 'inline-block');

                          //$('li[input_group="baremetal"]', $dialogAddHost).show();
                          $form.find('.form-item[rel=baremetalCpuCores]').css('display', 'inline-block');
                          $form.find('.form-item[rel=baremetalCpu]').css('display', 'inline-block');
                          $form.find('.form-item[rel=baremetalMemory]').css('display', 'inline-block');
                          $form.find('.form-item[rel=baremetalMAC]').css('display', 'inline-block');

                          //$('li[input_group="vmware"]', $dialogAddHost).hide();
                          $form.find('.form-item[rel=vcenterHost]').hide();

                          //$('li[input_group="Ovm"]', $dialogAddHost).hide();
                          $form.find('.form-item[rel=agentUsername]').hide();
                          $form.find('.form-item[rel=agentPassword]').hide();
                        }
                        else if (selectedClusterObj.hypervisortype == "Ovm") {
                          //$('li[input_group="general"]', $dialogAddHost).show();
                          $form.find('.form-item[rel=hostname]').css('display', 'inline-block');
                          $form.find('.form-item[rel=username]').css('display', 'inline-block');
                          $form.find('.form-item[rel=password]').css('display', 'inline-block');

                          //$('li[input_group="vmware"]', $dialogAddHost).hide();
                          $form.find('.form-item[rel=vcenterHost]').hide();

                          //$('li[input_group="baremetal"]', $dialogAddHost).hide();
                          $form.find('.form-item[rel=baremetalCpuCores]').hide();
                          $form.find('.form-item[rel=baremetalCpu]').hide();
                          $form.find('.form-item[rel=baremetalMemory]').hide();
                          $form.find('.form-item[rel=baremetalMAC]').hide();

                          //$('li[input_group="Ovm"]', $dialogAddHost).show();
                          $form.find('.form-item[rel=agentUsername]').css('display', 'inline-block');
                          $form.find('.form-item[rel=agentUsername]').find('input').val("oracle");
                          $form.find('.form-item[rel=agentPassword]').css('display', 'inline-block');
                        }
                        else {
                          //$('li[input_group="general"]', $dialogAddHost).show();
                          $form.find('.form-item[rel=hostname]').css('display', 'inline-block');
                          $form.find('.form-item[rel=username]').css('display', 'inline-block');
                          $form.find('.form-item[rel=password]').css('display', 'inline-block');

                          //$('li[input_group="vmware"]', $dialogAddHost).hide();
                          $form.find('.form-item[rel=vcenterHost]').hide();

                          //$('li[input_group="baremetal"]', $dialogAddHost).hide();
                          $form.find('.form-item[rel=baremetalCpuCores]').hide();
                          $form.find('.form-item[rel=baremetalCpu]').hide();
                          $form.find('.form-item[rel=baremetalMemory]').hide();
                          $form.find('.form-item[rel=baremetalMAC]').hide();

                          //$('li[input_group="Ovm"]', $dialogAddHost).hide();
                          $form.find('.form-item[rel=agentUsername]').hide();
                          $form.find('.form-item[rel=agentPassword]').hide();
                        }
                      });

                      args.$select.trigger("change");
                    }
                  },
                  //always appear (end)

                  //input_group="general" starts here
                  hostname: {
                    label: 'Host name',
                    validation: { required: true },
                    isHidden: true
                  },

                  username: {
                    label: 'User name',
                    validation: { required: true },
                    isHidden: true
                  },

                  password: {
                    label: 'Password',
                    validation: { required: true },
                    isHidden: true,
                    isPassword: true
                  },
                  //input_group="general" ends here

                  //input_group="VMWare" starts here
                  vcenterHost: {
                    label: 'ESX/ESXi Host',
                    validation: { required: true },
                    isHidden: true
                  },
                  //input_group="VMWare" ends here

                  //input_group="BareMetal" starts here
                  baremetalCpuCores: {
                    label: '# of CPU Cores',
                    validation: { required: true },
                    isHidden: true
                  },
                  baremetalCpu: {
                    label: 'CPU (in MHz)',
                    validation: { required: true },
                    isHidden: true
                  },
                  baremetalMemory: {
                    label: 'Memory (in MB)',
                    validation: { required: true },
                    isHidden: true
                  },
                  baremetalMAC: {
                    label: 'Host MAC',
                    validation: { required: true },
                    isHidden: true
                  },
                  //input_group="BareMetal" ends here

                  //input_group="OVM" starts here
                  agentUsername: {
                    label: 'Agent Username',
                    validation: { required: false },
                    isHidden: true
                  },
                  agentPassword: {
                    label: 'Agent Password',
                    validation: { required: true },
                    isHidden: true,
                    isPassword: true
                  },
                  //input_group="OVM" ends here

                  //always appear (begin)
                  hosttags: {
                    label: 'Host tags',
                    validation: { required: false }
                  }
                  //always appear (end)
                }
              },

              action: function(args) {
                var array1 = [];
                array1.push("&zoneid=" + args.context.zones[0].id);
                array1.push("&podid=" + args.data.podId);
                array1.push("&clusterid=" + args.data.clusterId);
                array1.push("&hypervisor=" + todb(selectedClusterObj.hypervisortype));
                var clustertype = selectedClusterObj.clustertype;
                array1.push("&clustertype=" + todb(clustertype));
                array1.push("&hosttags=" + todb(args.data.hosttags));

                if(selectedClusterObj.hypervisortype == "VMware") {
                  array1.push("&username=");
                  array1.push("&password=");
                  var hostname = args.data.vcenterHost;
                  var url;
                  if(hostname.indexOf("http://")==-1)
                    url = "http://" + hostname;
                  else
                    url = hostname;
                  array1.push("&url=" + todb(url));
                }
                else {
                  array1.push("&username=" + todb(args.data.username));
                  array1.push("&password=" + todb(args.data.password));

                  var hostname = args.data.hostname;

                  var url;
                  if(hostname.indexOf("http://")==-1)
                    url = "http://" + hostname;
                  else
                    url = hostname;
                  array1.push("&url="+todb(url));

                  if (selectedClusterObj.hypervisortype == "BareMetal") {
                    array1.push("&cpunumber=" + todb(args.data.baremetalCpuCores));
                    array1.push("&cpuspeed=" + todb(args.data.baremetalCpu));
                    array1.push("&memory=" + todb(args.data.baremetalMemory));
                    array1.push("&hostmac=" + todb(args.data.baremetalMAC));
                  }
                  else if(selectedClusterObj.hypervisortype == "Ovm") {
                    array1.push("&agentusername=" + todb(args.data.agentUsername));
                    array1.push("&agentpassword=" + todb(args.data.agentPassword));
                  }
                }

                $.ajax({
                  url: createURL("addHost" + array1.join("")),
                  dataType: "json",
                  success: function(json) {
                    var item = json.addhostresponse.host[0];                   
                    args.response.success({
                      data: item
                    });
                  },
                  error: function(XMLHttpResponse) {
                    var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                    args.response.error(errorMsg);
                  }
                });
              },

              notification: {
                poll: function(args){
                  args.complete({
                    actionFilter: hostActionfilter
                  });
                }
              },

              messages: {
                notification: function(args) {
                  return 'Added new host';
                }
              }
            } 
          },
          detailView: {
            name: "Host details",												
						viewAll: {
							label: 'Instances',
							path: 'instances'
						},				
            actions: {
              edit: {
                label: 'Edit',
                action: function(args) {
                  var array1 = [];
                  array1.push("&hosttags=" + todb(args.data.hosttags));

                  if (args.data.oscategoryid != null)
                    array1.push("&osCategoryId=" + args.data.oscategoryid);
                  else //OS is none
                    array1.push("&osCategoryId=0");

                  $.ajax({
                    url: createURL("updateHost&id=" + args.context.hosts[0].id + array1.join("")),
                    dataType: "json",
                    success: function(json) {
                      var item = json.updatehostresponse.host;
                      args.response.success({
                        actionFilter: hostActionfilter,
                        data:item});
                    }
                  });
                }
              },

              enableMaintenanceMode: {
                label: 'Enable Maintenace' ,
                action: function(args) {
                  $.ajax({
                    url: createURL("prepareHostForMaintenance&id=" + args.context.hosts[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.preparehostformaintenanceresponse.jobid;
                      args.response.success(
                        {_custom:
                         {jobId: jid,
                          getUpdatedItem: function(json) {
                            return json.queryasyncjobresultresponse.jobresult.host;
                          },
                          getActionFilter: function() {
                            return hostActionfilter;
                          }
                         }
                        }
                      );
                    }
                  });
                },
                messages: {
                  confirm: function(args) {
                    return 'Enabling maintenance mode will cause a live migration of all running instances on this host to any available host.';
                  },
                  success: function(args) {
                    return 'Maintenance is being enabled.';
                  },
                  notification: function(args) {
                    return 'Enabling maintenance';
                  },
                  complete: function(args) {
                    return 'Maintenance has been enabled.';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },

              cancelMaintenanceMode: {
                label: 'Cancel Maintenace' ,
                action: function(args) {
                  $.ajax({
                    url: createURL("cancelHostMaintenance&id=" + args.context.hosts[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.cancelhostmaintenanceresponse.jobid;
                      args.response.success(
                        {_custom:
                         {jobId: jid,
                          getUpdatedItem: function(json) {
                            return json.queryasyncjobresultresponse.jobresult.host;
                          },
                          getActionFilter: function() {
                            return hostActionfilter;
                          }
                         }
                        }
                      );
                    }
                  });
                },
                messages: {
                  confirm: function(args) {
                    return 'Please confirm that you want to cancel this maintenance.';
                  },
                  success: function(args) {
                    return 'Maintenance is being cancelled.';
                  },
                  notification: function(args) {
                    return 'Cancelling maintenance';
                  },
                  complete: function(args) {
                    return 'Maintenance has been cancelled.';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },

              forceReconnect: {
                label: 'Force Reconnect' ,
                action: function(args) {
                  $.ajax({
                    url: createURL("reconnectHost&id=" + args.context.hosts[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.reconnecthostresponse.jobid;
                      args.response.success(
                        {_custom:
                         {jobId: jid,
                          getUpdatedItem: function(json) {
                            return json.queryasyncjobresultresponse.jobresult.host;
                          },
                          getActionFilter: function() {
                            return hostActionfilter;
                          }
                         }
                        }
                      );
                    }
                  });
                },
                messages: {
                  confirm: function(args) {
                    return 'Please confirm that you want to force reconnect this host.';
                  },
                  success: function(args) {
                    return 'Host is being force reconnected.';
                  },
                  notification: function(args) {
                    return 'Force reconnecting host';
                  },
                  complete: function(args) {
                    return 'Host has been force reconnected.';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },

              'delete': {
                label: 'Remove host' ,
                messages: {
                  confirm: function(args) {
                    return 'Please confirm that you want to remove this host.';
                  },
                  success: function(args) {
                    return 'Host is being removed.';
                  },
                  notification: function(args) {
                    return 'Removing host';
                  },
                  complete: function(args) {
                    return 'Host has been removed.';
                  }
                },
                preFilter: function(args) {
                  if(isAdmin()) {
                    args.$form.find('.form-item[rel=isForced]').css('display', 'inline-block');
                  }
                },
                createForm: {
                  title: 'Remove host',
                  fields: {
                    isForced: {
                      label: 'Force Remove',
                      isBoolean: true,
                      isHidden: true
                    }
                  }
                },
                action: function(args) {
                  var array1 = [];
                  //if(args.$form.find('.form-item[rel=isForced]').css("display") != "none") //uncomment after Brian fix it to include $form in args
                  array1.push("&forced=" + (args.data.isForced == "on"));

                  $.ajax({
                    url: createURL("deleteHost&id=" + args.context.hosts[0].id + array1.join("")),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      //{ "deletehostresponse" : { "success" : "true"}  }
                      args.response.success({data:{}});
                    }
                  });
                },
                notification: {
                  poll: function(args) { args.complete(); }
                }
              }

            },
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    name: { label: 'Name' }
                  },
                  {
                    id: { label: 'ID' },
                    resourcestate: { label: 'Resource state' },
                    state: { label: 'State' },
                    type: { label: 'Type' },
                    zonename: { label: 'Zone' },
                    podname: { label: 'Pod' },
                    clustername: { label: 'Cluster' },
                    ipaddress: { label: 'IP Address' },
                    version: { label: 'Version' },
                    hosttags: {
                      label: 'Host tags',
                      isEditable: true
                    },
                    oscategoryid: {
                      label: 'OS Preference',
                      isEditable: true,
                      select: function(args) {
                        $.ajax({
                          url: createURL("listOsCategories"),
                          dataType: "json",
                          async: true,
                          success: function(json) {
                            var oscategoryObjs = json.listoscategoriesresponse.oscategory;
                            var items = [];
                            $(oscategoryObjs).each(function() {
                              items.push({id: this.id, description: this.name});
                            });
                            args.response.success({data: items});
                          }
                        });
                      }
                    },
                    disconnected: { label: 'Last disconnected' }
                  }
                ],

                dataProvider: function(args) {
                  args.response.success({
                    actionFilter: hostActionfilter,
                    data: args.context.hosts[0]
                  });
                }

              }
            }
          }
        }
      },
      'primary-storage': {
        title: 'Primary Storage',
        id: 'primarystorages',
        listView: {
          section: 'primary-storage',
          fields: {
            name: { label: 'Name' },            
            podname: { label: 'Pod' },
						clustername: { label: 'Cluster' },
						path: { label: 'Path' }
          },

          dataProvider: function(args) {
            var array1 = [];				
						if(args.filterBy != null) {          
							if(args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
								switch(args.filterBy.search.by) {
								case "name":
									if(args.filterBy.search.value.length > 0)
										array1.push("&keyword=" + args.filterBy.search.value);
									break;
								}
							}
						}						
            array1.push("&zoneid=" + args.context.zones[0].id);
            if("pods" in args.context)
              array1.push("&podid=" + args.context.pods[0].id);
            if("clusters" in args.context)
              array1.push("&clusterid=" + args.context.clusters[0].id);
            $.ajax({
              url: createURL("listStoragePools&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.liststoragepoolsresponse.storagepool;
                args.response.success({
                  actionFilter: primarystorageActionfilter,
                  data:items
                });
              }
            });
          },

          actions: {
            add: {
              label: 'Add primary storage',

              createForm: {
                title: 'Add new primary storage',
                desc: 'Please fill in the following information to add a new primary storage',
                fields: {
                  //always appear (begin)
                  podId: {
                    label: 'Pod',
                    validation: { required: true },
                    select: function(args) {
                      $.ajax({
                        url: createURL("listPods&zoneid=" + args.context.zones[0].id),
                        dataType: "json",
                        async: true,
                        success: function(json) {
                          var pods = json.listpodsresponse.pod;
                          var items = [];
                          $(pods).each(function() {
                            items.push({id: this.id, description: this.name});
                          });
                          args.response.success({data: items});
                        }
                      });
                    }
                  },

                  clusterId: {
                    label: 'Cluster',
                    validation: { required: true },
                    dependsOn: 'podId',
                    select: function(args) {
                      $.ajax({
                        url: createURL("listClusters&podid=" + args.podId),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                          clusterObjs = json.listclustersresponse.cluster;
                          var items = [];
                          $(clusterObjs).each(function() {
                            items.push({id: this.id, description: this.name});
                          });
                          args.response.success({
                            actionFilter: clusterActionfilter,
                            data: items
                          });
                        }
                      });
                    }
                  },

                  name: {
                    label: 'Name',
                    validation: { required: true }
                  },

                  protocol: {
                    label: 'Protocol',
                    validation: { required: true },
                    dependsOn: 'clusterId',
                    select: function(args) {
                      var clusterId = args.clusterId;
                      if(clusterId == null)
                        return;
                      var items = [];
                      $(clusterObjs).each(function(){
                        if(this.id == clusterId){
                          selectedClusterObj = this;
                          return false; //break the $.each() loop
                        }
                      });
                      if(selectedClusterObj == null)
                        return;

                      if(selectedClusterObj.hypervisortype == "KVM") {
                        var items = [];
                        items.push({id: "nfs", description: "nfs"});
                        items.push({id: "SharedMountPoint", description: "SharedMountPoint"});
                        args.response.success({data: items});
                      }
                      else if(selectedClusterObj.hypervisortype == "XenServer") {
                        var items = [];
                        items.push({id: "nfs", description: "nfs"});
                        items.push({id: "PreSetup", description: "PreSetup"});
                        items.push({id: "iscsi", description: "iscsi"});
                        args.response.success({data: items});
                      }
                      else if(selectedClusterObj.hypervisortype == "VMware") {
                        var items = [];
                        items.push({id: "nfs", description: "nfs"});
                        items.push({id: "vmfs", description: "vmfs"});
                        args.response.success({data: items});
                      }
                      else if(selectedClusterObj.hypervisortype == "Ovm") {
                        var items = [];
                        items.push({id: "nfs", description: "nfs"});
                        items.push({id: "ocfs2", description: "ocfs2"});
                        args.response.success({data: items});
                      }
                      else {
                        args.response.success({data:[]});
                      }

                      args.$select.change(function() {
                        var $form = $(this).closest('form');

                        var protocol = $(this).val();
                        if(protocol == null)
                          return;

                        if(protocol == "nfs") {
                          //$("#add_pool_server_container", $dialogAddPool).show();
                          $form.find('.form-item[rel=server]').css('display', 'inline-block');
                          //$dialogAddPool.find("#add_pool_nfs_server").val("");
                          $form.find('.form-item[rel=server]').find(".value").find("input").val("");

                          //$('li[input_group="nfs"]', $dialogAddPool).show();
                          $form.find('.form-item[rel=path]').css('display', 'inline-block');
                          //$dialogAddPool.find("#add_pool_path_container").find("label").text(g_dictionary["label.path"]+":");
                          $form.find('.form-item[rel=path]').find(".name").find("label").text("Path:");

                          //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=iqn]').hide();
                          $form.find('.form-item[rel=lun]').hide();

                          //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=vCenterDataCenter]').hide();
                          $form.find('.form-item[rel=vCenterDataStore]').hide();
                        }
                        else if(protocol == "ocfs2") {//ocfs2 is the same as nfs, except no server field.
                          //$dialogAddPool.find("#add_pool_server_container").hide();
                          $form.find('.form-item[rel=server]').hide();
                          //$dialogAddPool.find("#add_pool_nfs_server").val("");
                          $form.find('.form-item[rel=server]').find(".value").find("input").val("");

                          //$('li[input_group="nfs"]', $dialogAddPool).show();
                          $form.find('.form-item[rel=path]').css('display', 'inline-block');
                          //$dialogAddPool.find("#add_pool_path_container").find("label").text(g_dictionary["label.path"]+":");
                          $form.find('.form-item[rel=path]').find(".name").find("label").text("Path:");

                          //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=iqn]').hide();
                          $form.find('.form-item[rel=lun]').hide();

                          //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=vCenterDataCenter]').hide();
                          $form.find('.form-item[rel=vCenterDataStore]').hide();
                        }
                        else if(protocol == "PreSetup") {
                          //$dialogAddPool.find("#add_pool_server_container").hide();
                          $form.find('.form-item[rel=server]').hide();
                          //$dialogAddPool.find("#add_pool_nfs_server").val("localhost");
                          $form.find('.form-item[rel=server]').find(".value").find("input").val("localhost");

                          //$('li[input_group="nfs"]', $dialogAddPool).show();
                          $form.find('.form-item[rel=path]').css('display', 'inline-block');
                          //$dialogAddPool.find("#add_pool_path_container").find("label").text(g_dictionary["label.SR.name"]+":");
                          $form.find('.form-item[rel=path]').find(".name").find("label").text("SR Name-Label:");

                          //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=iqn]').hide();
                          $form.find('.form-item[rel=lun]').hide();

                          //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=vCenterDataCenter]').hide();
                          $form.find('.form-item[rel=vCenterDataStore]').hide();
                        }
                        else if(protocol == "iscsi") {
                          //$dialogAddPool.find("#add_pool_server_container").show();
                          $form.find('.form-item[rel=server]').css('display', 'inline-block');
                          //$dialogAddPool.find("#add_pool_nfs_server").val("");
                          $form.find('.form-item[rel=server]').find(".value").find("input").val("");

                          //$('li[input_group="nfs"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=path]').hide();

                          //$('li[input_group="iscsi"]', $dialogAddPool).show();
                          $form.find('.form-item[rel=iqn]').css('display', 'inline-block');
                          $form.find('.form-item[rel=lun]').css('display', 'inline-block');

                          //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=vCenterDataCenter]').hide();
                          $form.find('.form-item[rel=vCenterDataStore]').hide();
                        }
                        else if(protocol == "vmfs") {
                          //$dialogAddPool.find("#add_pool_server_container").show();
                          $form.find('.form-item[rel=server]').css('display', 'inline-block');
                          //$dialogAddPool.find("#add_pool_nfs_server").val("");
                          $form.find('.form-item[rel=server]').find(".value").find("input").val("");

                          //$('li[input_group="nfs"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=path]').hide();

                          //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=iqn]').hide();
                          $form.find('.form-item[rel=lun]').hide();

                          //$('li[input_group="vmfs"]', $dialogAddPool).show();
                          $form.find('.form-item[rel=vCenterDataCenter]').css('display', 'inline-block');
                          $form.find('.form-item[rel=vCenterDataStore]').css('display', 'inline-block');
                        }
                        else if(protocol == "SharedMountPoint") {  //"SharedMountPoint" show the same fields as "nfs" does.
                          //$dialogAddPool.find("#add_pool_server_container").hide();
                          $form.find('.form-item[rel=server]').hide();
                          //$dialogAddPool.find("#add_pool_nfs_server").val("localhost");
                          $form.find('.form-item[rel=server]').find(".value").find("input").val("localhost");

                          //$('li[input_group="nfs"]', $dialogAddPool).show();
                          $form.find('.form-item[rel=path]').css('display', 'inline-block');
                          $form.find('.form-item[rel=path]').find(".name").find("label").text("Path:");

                          //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=iqn]').hide();
                          $form.find('.form-item[rel=lun]').hide();

                          //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=vCenterDataCenter]').hide();
                          $form.find('.form-item[rel=vCenterDataStore]').hide();
                        }
                        else {
                          //$dialogAddPool.find("#add_pool_server_container").show();
                          $form.find('.form-item[rel=server]').css('display', 'inline-block');
                          //$dialogAddPool.find("#add_pool_nfs_server").val("");
                          $form.find('.form-item[rel=server]').find(".value").find("input").val("");

                          //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=iqn]').hide();
                          $form.find('.form-item[rel=lun]').hide();

                          //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=vCenterDataCenter]').hide();
                          $form.find('.form-item[rel=vCenterDataStore]').hide();
                        }
                      });

                      args.$select.trigger("change");
                    }
                  },
                  //always appear (end)

                  server: {
                    label: 'Server',
                    validation: { required: true },
                    isHidden: true
                  },

                  //nfs
                  path: {
                    label: 'Path',
                    validation: { required: true },
                    isHidden: true
                  },

                  //iscsi
                  iqn: {
                    label: 'Target IQN',
                    validation: { required: true },
                    isHidden: true
                  },
                  lun: {
                    label: 'LUN #',
                    validation: { required: true },
                    isHidden: true
                  },

                  //vmfs
                  vCenterDataCenter: {
                    label: 'vCenter Datacenter',
                    validation: { required: true },
                    isHidden: true
                  },
                  vCenterDataStore: {
                    label: 'vCenter Datastore',
                    validation: { required: true },
                    isHidden: true
                  },

                  //always appear (begin)
                  storageTags: {
                    label: 'Storage Tags',
                    validation: { required: false }
                  }
                  //always appear (end)
                }
              },

              action: function(args) {
                var array1 = [];
                array1.push("&zoneid=" + args.context.zones[0].id);
                array1.push("&podId=" + args.data.podId);
                array1.push("&clusterid=" + args.data.clusterId);
                array1.push("&name=" + todb(args.data.name));

                var server = args.data.server;
                var url = null;
                if (args.data.protocol == "nfs") {
                  //var path = trim($thisDialog.find("#add_pool_path").val());
                  var path = args.data.path;

                  if(path.substring(0,1) != "/")
                    path = "/" + path;
                  url = nfsURL(server, path);
                }
                else if (args.data.protocol == "PreSetup") {
                  //var path = trim($thisDialog.find("#add_pool_path").val());
                  var path = args.data.path;

                  if(path.substring(0,1) != "/")
                    path = "/" + path;
                  url = presetupURL(server, path);
                }
                else if (args.data.protocol == "ocfs2") {
                  //var path = trim($thisDialog.find("#add_pool_path").val());
                  var path = args.data.path;

                  if(path.substring(0,1) != "/")
                    path = "/" + path;
                  url = ocfs2URL(server, path);
                }
                else if (args.data.protocol == "SharedMountPoint") {
                  //var path = trim($thisDialog.find("#add_pool_path").val());
                  var path = args.data.path;

                  if(path.substring(0,1) != "/")
                    path = "/" + path;
                  url = SharedMountPointURL(server, path);
                }
                else if (args.data.protocol == "vmfs") {
                  //var path = trim($thisDialog.find("#add_pool_vmfs_dc").val());
                  var path = args.data.vCenterDataCenter;

                  if(path.substring(0,1) != "/")
                    path = "/" + path;
                  path += "/" + args.data.vCenterDataStore;
                  url = vmfsURL("dummy", path);
                }
                else {
                  //var iqn = trim($thisDialog.find("#add_pool_iqn").val());
                  var iqn = args.data.iqn;

                  if(iqn.substring(0,1) != "/")
                    iqn = "/" + iqn;
                  var lun = args.data.lun;
                  url = iscsiURL(server, iqn, lun);
                }
                array1.push("&url=" + todb(url));

                if(args.data.storageTags != null && args.data.storageTags.length > 0)
                  array1.push("&tags=" + todb(args.data.storageTags));

                $.ajax({
                  url: createURL("createStoragePool" + array1.join("")),
                  dataType: "json",
                  success: function(json) {
                    var item = json.createstoragepoolresponse.storagepool[0];								
                    args.response.success({
										  data: item
										});
                  },
                  error: function(XMLHttpResponse) {
                    var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                    args.response.error(errorMsg);
                  }
                });
              },

              notification: {
                poll: function(args){
                  args.complete({
                    actionFilter: primarystorageActionfilter
                  });
                }
              },

              messages: {
                notification: function(args) {
                  return 'Added new primary storage';
                }
              }
            }   
          },

          detailView: {
            name: "Primary storage details",
            actions: {						 
							edit: {
                label: 'Edit',
                action: function(args) {
                  var array1 = [];							
                  array1.push("&tags=" + todb(args.data.tags));
                  
                  $.ajax({
                    url: createURL("updateStoragePool&id=" + args.context.primarystorages[0].id + array1.join("")),
                    dataType: "json",
                    success: function(json) {										  
                      var item = json.updatestoragepoolresponse.storagepool;
                      args.response.success({data: item});
                    },
                    error: function(XMLHttpResponse) {
                      args.response.error(parseXMLHttpResponse(XMLHttpResponse));
                    }
                  });
                }
              },							
													
              enableMaintenanceMode: {
                label: 'Enable Maintenace' ,
                action: function(args) {
                  $.ajax({
                    url: createURL("enableStorageMaintenance&id=" + args.context.primarystorages[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.prepareprimarystorageformaintenanceresponse.jobid;
                      args.response.success(
                        {_custom:
                         {jobId: jid,
                          getUpdatedItem: function(json) {
                            return json.queryasyncjobresultresponse.jobresult.storagepool;
                          },
                          getActionFilter: function() {
                            return primarystorageActionfilter;
                          }
                         }
                        }
                      );
                    }
                  });
                },
                messages: {
                  confirm: function(args) {
                    return 'Warning: placing the primary storage into maintenance mode will cause all VMs using volumes from it to be stopped.  Do you want to continue?';
                  },
                  success: function(args) {
                    return 'Maintenance is being enabled.';
                  },
                  notification: function(args) {
                    return 'Enabling maintenance';
                  },
                  complete: function(args) {
                    return 'Maintenance has been enabled.';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },

              cancelMaintenanceMode: {
                label: 'Cancel Maintenace' ,
                action: function(args) {
                  $.ajax({
                    url: createURL("cancelStorageMaintenance&id=" + args.context.primarystorages[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.cancelprimarystoragemaintenanceresponse.jobid;
                      args.response.success(
                        {_custom:
                         {jobId: jid,
                          getUpdatedItem: function(json) {
                            return json.queryasyncjobresultresponse.jobresult.storagepool;
                          },
                          getActionFilter: function() {
                            return primarystorageActionfilter;
                          }
                         }
                        }
                      );
                    }
                  });
                },
                messages: {
                  confirm: function(args) {
                    return 'Please confirm that you want to cancel this maintenance.';
                  },
                  success: function(args) {
                    return 'Maintenance is being cancelled.';
                  },
                  notification: function(args) {
                    return 'Cancelling maintenance';
                  },
                  complete: function(args) {
                    return 'Maintenance has been cancelled.';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },

              'delete': {
                label: 'Delete' ,
                messages: {
                  confirm: function(args) {
                    return 'Please confirm that you want to delete this primary storage.';
                  },
                  success: function(args) {
                    return 'Primary storage is being deleted.';
                  },
                  notification: function(args) {
                    return 'Deleting primary storage';
                  },
                  complete: function(args) {
                    return 'Primary storage has been deleted.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteStoragePool&id=" + args.context.primarystorages[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      args.response.success({data:{}});
                    }
                  });
                },
                notification: {
                  poll: function(args) { args.complete(); }
                }
              }

            },

            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    name: { label: 'Name' }
                  },
                  {
                    id: { label: 'ID' },
                    state: { label: 'State' },  
										tags: { 
										  label: 'Storage tags',
											isEditable: true
										},
										podname: { label: 'Pod' },
                    clustername: { label: 'Cluster' },
                    type: { label: 'Type' },
                    ipaddress: { label: 'IP Address' },
                    path: { label: 'Path' },
                    disksizetotal: {
                      label: 'Disk total',
                      converter: function(args) {
                        if (args == null || args == 0)
                          return "";
                        else
                          return cloudStack.converters.convertBytes(args);
                      }
                    },
                    disksizeallocated: {
                      label: 'Disk Allocated',
                      converter: function(args) {
                        if (args == null || args == 0)
                          return "";
                        else
                          return cloudStack.converters.convertBytes(args);
                      }
                    }                   
                  }
                ],
                
                dataProvider: function(args) {
                  args.response.success({
                    actionFilter: primarystorageActionfilter,
                    data: args.context.primarystorages[0]
                  });
                }

              }
            }
          }
        }
      },

      'secondary-storage': {
        title: 'Secondary Storage',
        id: 'secondarystorages',
        listView: {
          section: 'seconary-storage',
          fields: {
            name: { label: 'Name' },
						created: { label: 'Created', converter: cloudStack.converters.toLocalDate }
          },

          dataProvider: function(args) {
            var array1 = [];						
						if(args.filterBy != null) {          
							if(args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
								switch(args.filterBy.search.by) {
								case "name":
									if(args.filterBy.search.value.length > 0)
										array1.push("&keyword=" + args.filterBy.search.value);
									break;
								}
							}
						}						
            array1.push("&zoneid=" + args.context.zones[0].id);            
            $.ajax({
              url: createURL("listHosts&type=SecondaryStorage&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listhostsresponse.host;
                args.response.success({
                  actionFilter: secondarystorageActionfilter,
                  data:items
                });
              }
            });
          },

          actions: {
            add: {
              label: 'Add secondary storage',

              createForm: {
                title: 'Add new secondary storage',
                desc: 'Please fill in the following information to add a new secondary storage',
                fields: {
                  nfsServer: {
                    label: 'NFS Server',
                    validation: { required: true }
                  },
                  path: {
                    label: 'Path',
                    validation: { required: true }
                  }
                }
              },

              action: function(args) {
                var zoneId = args.context.zones[0].id;
                var nfs_server = args.data.nfsServer;
                var path = args.data.path;
                var url = nfsURL(nfs_server, path);

                $.ajax({
                  url: createURL("addSecondaryStorage&zoneId=" + zoneId + "&url=" + todb(url)),
                  dataType: "json",
                  success: function(json) {
                    var item = json.addsecondarystorageresponse.secondarystorage;						
                    args.response.success({
										  data:item
										});
                  },
                  error: function(XMLHttpResponse) {
                    var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                    args.response.error(errorMsg);
                  }
                });
              },

              notification: {
                poll: function(args) {
                  args.complete({
                    actionFilter: secondarystorageActionfilter
                  });
                }
              },

              messages: {
                notification: function(args) {
                  return 'Added new secondary storage';
                }
              }
            }
          },

          detailView: {
            name: 'Secondary storage details',
            actions: {
              'delete': {
                label: 'Delete' ,
                messages: {
                  confirm: function(args) {
                    return 'Please confirm that you want to delete this secondary storage.';
                  },
                  success: function(args) {
                    return 'Secondary storage is being deleted.';
                  },
                  notification: function(args) {
                    return 'Deleting secondary storage';
                  },
                  complete: function(args) {
                    return 'Secondary storage has been deleted.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteHost&id=" + args.context.secondarystorages[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      args.response.success({data:{}});
                    }
                  });
                },
                notification: {
                  poll: function(args) { args.complete(); }
                }
              }

            },
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    name: { label: 'Name' }
                  },
                  {
                    id: { label: 'ID' },
                    created: { label: 'Created', converter: cloudStack.converters.toLocalDate },
                  }
                ],
                
                dataProvider: function(args) {
                  args.response.success({
                    actionFilter: secondarystorageActionfilter,
                    data: args.context.secondarystorages[0]
                  });
                }
              }
            }
          }
        }
      },

      guestIpRanges: {
        title: 'Guest IP Range',
        id: 'guestIpRanges',
        listView: {
          section: 'guest-IP-range',
          fields: {
            //id: { label: 'ID' },
            //podname: { label: 'Pod' },
            //vlan: { label: 'VLAN' },
            startip: { label: 'Start IP' },
            endip: { label: 'End IP' }
          },

          dataProvider: function(args) {
            $.ajax({
              url: createURL("listVlanIpRanges&zoneid=" + selectedZoneObj.id + "&networkid=" + args.context.networks[0].id + "&page=" + args.page + "&pagesize=" + pageSize),
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listvlaniprangesresponse.vlaniprange;
                args.response.success({data: items});
              }
            });
          },

          actions: {
            add: {
              label: 'Add IP range',

              createForm: {
                title: 'Add IP range',

                preFilter: function(args) {
                  if(selectedZoneObj.networktype == "Basic") {
                    args.$form.find('.form-item[rel=podId]').css('display', 'inline-block');
                    args.$form.find('.form-item[rel=guestGateway]').css('display', 'inline-block');
                    args.$form.find('.form-item[rel=guestNetmask]').css('display', 'inline-block');
                  }
                  else {  //"Advanced"
                    args.$form.find('.form-item[rel=podId]').hide();
                    args.$form.find('.form-item[rel=guestGateway]').hide();
                    args.$form.find('.form-item[rel=guestNetmask]').hide();
                  }
                },

                fields: {
                  podId: {
                    label: 'Pod',
                    validation: { required: true },
                    select: function(args) {
                      var items = [];
                      if(selectedZoneObj.networktype == "Basic") {
                        $.ajax({
                          url: createURL("listPods&zoneid=" + selectedZoneObj.id),
                          dataType: "json",
                          async: false,
                          success: function(json) {
                            var podObjs = json.listpodsresponse.pod;
                            $(podObjs).each(function(){
                              items.push({id: this.id, description: this.name});
                            });
                          }
                        });
                        items.push({id: 0, description: "(create new pod)"});
                      }
                      args.response.success({data: items});

                      args.$select.change(function() {
                        var $form = $(this).closest('form');
                        if($(this).val() == "0") {
                          $form.find('.form-item[rel=podname]').css('display', 'inline-block');
                          $form.find('.form-item[rel=reservedSystemGateway]').css('display', 'inline-block');
                          $form.find('.form-item[rel=reservedSystemNetmask]').css('display', 'inline-block');
                          $form.find('.form-item[rel=reservedSystemStartIp]').css('display', 'inline-block');
                          $form.find('.form-item[rel=reservedSystemEndIp]').css('display', 'inline-block');
                        }
                        else {
                          $form.find('.form-item[rel=podname]').hide();
                          $form.find('.form-item[rel=reservedSystemGateway]').hide();
                          $form.find('.form-item[rel=reservedSystemNetmask]').hide();
                          $form.find('.form-item[rel=reservedSystemStartIp]').hide();
                          $form.find('.form-item[rel=reservedSystemEndIp]').hide();
                        }
                      });
                    }
                  },

                  //create new pod fields start here
                  podname: {
                    label: 'Pod name',
                    validation: { required: true }
                  },
                  reservedSystemGateway: {
                    label: 'Reserved system gateway',
                    validation: { required: true }
                  },
                  reservedSystemNetmask: {
                    label: 'Reserved system netmask',
                    validation: { required: true }
                  },
                  reservedSystemStartIp: {
                    label: 'Start Reserved system IP',
                    validation: { required: true }
                  },
                  reservedSystemEndIp: {
                    label: 'End Reserved system IP',
                    validation: { required: false }
                  },
                  //create new pod fields ends here

                  guestGateway: { label: 'Guest gateway' },
                  guestNetmask: { label: 'Guest netmask' },
                  guestStartIp: { label: 'Guest start IP' },
                  guestEndIp: { label: 'Guest end IP' }
                }
              },

              action: function(args) {		                
								var array2 = [];
								array2.push("&startip=" + args.data.guestStartIp);
								var endip = args.data.guestEndIp;
								if(endip != null && endip.length > 0)
									array2.push("&endip=" + endip);
								$.ajax({
									url: createURL("createVlanIpRange&forVirtualNetwork=false&networkid=" + args.context.networks[0].id + array2.join("")),
									dataType: "json",
									success: function(json) {
										var item = json.createvlaniprangeresponse.vlan;
										args.response.success({data:item});
									},
									error: function(XMLHttpResponse) {
										var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
										args.response.error(errorMsg);
									}
								});                
              },

              notification: {
                poll: function(args) {
                  args.complete();
                }
              },

              messages: {
                notification: function(args) {
                  return 'Added new IP range';
                }
              }
            },

            'delete': {
              label: 'Delete' ,
              messages: {
                confirm: function(args) {
                  return 'Please confirm that you want to delete this IP range.';
                },
                success: function(args) {
                  return 'IP range is being deleted.';
                },
                notification: function(args) {
                  return 'Deleting IP range';
                },
                complete: function(args) {
                  return 'IP range has been deleted.';
                }
              },
              action: function(args) {							
                $.ajax({
                  url: createURL("deleteVlanIpRange&id=" + args.data.id),
                  dataType: "json",
                  async: true,
                  success: function(json) {
                    args.response.success({data:{}});
                  }
                });
              },
              notification: {
                poll: function(args) { args.complete(); }
              }
            }
          }
        }
      }
    }
  };

  function nfsURL(server, path) {
    var url;
    if(server.indexOf("://")==-1)
      url = "nfs://" + server + path;
    else
      url = server + path;
    return url;
  }

  function presetupURL(server, path) {
    var url;
    if(server.indexOf("://")==-1)
      url = "presetup://" + server + path;
    else
      url = server + path;
    return url;
  }

  function ocfs2URL(server, path) {
    var url;
    if(server.indexOf("://")==-1)
      url = "ocfs2://" + server + path;
    else
      url = server + path;
    return url;
  }

  function SharedMountPointURL(server, path) {
    var url;
    if(server.indexOf("://")==-1)
      url = "SharedMountPoint://" + server + path;
    else
      url = server + path;
    return url;
  }

  function vmfsURL(server, path) {
    var url;
    if(server.indexOf("://")==-1)
      url = "vmfs://" + server + path;
    else
      url = server + path;
    return url;
  }

  function iscsiURL(server, iqn, lun) {
    var url;
    if(server.indexOf("://")==-1)
      url = "iscsi://" + server + iqn + "/" + lun;
    else
      url = server + iqn + "/" + lun;
    return url;
  }

  function addExternalLoadBalancer(args, physicalNetworkObj, apiCmd, apiCmdRes, apiCmdObj) {
    var array1 = [];
    array1.push("&physicalnetworkid=" + physicalNetworkObj.id);
    array1.push("&username=" + todb(args.data.username));
    array1.push("&password=" + todb(args.data.password));
    array1.push("&networkdevicetype=" + todb(args.data.networkdevicetype));

    //construct URL starts here
    var url = [];

    var ip = args.data.ip;
    url.push("https://" + ip);

    var isQuestionMarkAdded = false;

    var publicInterface = args.data.publicinterface;
    if(publicInterface != null && publicInterface.length > 0) {
        if(isQuestionMarkAdded == false) {
            url.push("?");
            isQuestionMarkAdded = true;
        }
        else {
            url.push("&");
        }
        url.push("publicinterface=" + publicInterface);
    }

    var privateInterface = args.data.privateinterface;
    if(privateInterface != null && privateInterface.length > 0) {
        if(isQuestionMarkAdded == false) {
            url.push("?");
            isQuestionMarkAdded = true;
        }
        else {
            url.push("&");
        }
        url.push("privateinterface=" + privateInterface);
    }

    var numretries = args.data.numretries;
    if(numretries != null && numretries.length > 0) {
        if(isQuestionMarkAdded == false) {
            url.push("?");
            isQuestionMarkAdded = true;
        }
        else {
            url.push("&");
        }
        url.push("numretries=" + numretries);
    }

    var isInline = args.data.inline;
    if(isInline != null && isInline.length > 0) {
        if(isQuestionMarkAdded == false) {
            url.push("?");
            isQuestionMarkAdded = true;
        }
        else {
            url.push("&");
        }
        url.push("inline=" + isInline);
    }

    var capacity = args.data.capacity;
    if(capacity != null && capacity.length > 0) {
        if(isQuestionMarkAdded == false) {
            url.push("?");
            isQuestionMarkAdded = true;
        }
        else {
            url.push("&");
        }
        url.push("lbdevicecapacity=" + capacity);
    }

    var dedicated = (args.data.dedicated == "on");	//boolean	(true/false)
    if(isQuestionMarkAdded == false) {
        url.push("?");
        isQuestionMarkAdded = true;
    }
    else {
        url.push("&");
    }
    url.push("lbdevicededicated=" + dedicated.toString());


    array1.push("&url=" + todb(url.join("")));
    //construct URL ends here

    $.ajax({
      url: createURL(apiCmd + array1.join("")),
      dataType: "json",
      success: function(json) {
        var jid = json[apiCmdRes].jobid;
        args.response.success(
          {_custom:
           {jobId: jid,
            getUpdatedItem: function(json) {
              var item = json.queryasyncjobresultresponse.jobresult[apiCmdObj];

              return item;
            }
           }
          }
        );
      }
    });
  }

  function addExternalFirewall(args, physicalNetworkObj, apiCmd, apiCmdRes, apiCmdObj){
    var array1 = [];
    array1.push("&physicalnetworkid=" + physicalNetworkObj.id);
    array1.push("&username=" + todb(args.data.username));
    array1.push("&password=" + todb(args.data.password));
    array1.push("&networkdevicetype=" + todb(args.data.networkdevicetype));

    //construct URL starts here
    var url = [];

    var ip = args.data.ip;
    url.push("https://" + ip);

    var isQuestionMarkAdded = false;

    var publicInterface = args.data.publicinterface;
    if(publicInterface != null && publicInterface.length > 0) {
        if(isQuestionMarkAdded == false) {
            url.push("?");
            isQuestionMarkAdded = true;
        }
        else {
            url.push("&");
        }
        url.push("publicinterface=" + publicInterface);
    }

    var privateInterface = args.data.privateinterface;
    if(privateInterface != null && privateInterface.length > 0) {
        if(isQuestionMarkAdded == false) {
            url.push("?");
            isQuestionMarkAdded = true;
        }
        else {
            url.push("&");
        }
        url.push("privateinterface=" + privateInterface);
    }

    var usageInterface = args.data.usageinterface;
    if(usageInterface != null && usageInterface.length > 0) {
        if(isQuestionMarkAdded == false) {
            url.push("?");
            isQuestionMarkAdded = true;
        }
        else {
            url.push("&");
        }
        url.push("usageinterface=" + usageInterface);
    }

    var numretries = args.data.numretries;
    if(numretries != null && numretries.length > 0) {
        if(isQuestionMarkAdded == false) {
            url.push("?");
            isQuestionMarkAdded = true;
        }
        else {
            url.push("&");
        }
        url.push("numretries=" + numretries);
    }

    var timeout = args.data.timeout;
    if(timeout != null && timeout.length > 0) {
        if(isQuestionMarkAdded == false) {
            url.push("?");
            isQuestionMarkAdded = true;
        }
        else {
            url.push("&");
        }
        url.push("timeout=" + timeout);
    }

    var isInline = args.data.inline;
    if(isInline != null && isInline.length > 0) {
        if(isQuestionMarkAdded == false) {
            url.push("?");
            isQuestionMarkAdded = true;
        }
        else {
            url.push("&");
        }
        url.push("inline=" + isInline);
    }

    var publicNetwork = args.data.publicnetwork;
    if(publicNetwork != null && publicNetwork.length > 0) {
        if(isQuestionMarkAdded == false) {
            url.push("?");
            isQuestionMarkAdded = true;
        }
        else {
            url.push("&");
        }
        url.push("publicnetwork=" + publicNetwork);
    }

    var privateNetwork = args.data.privatenetwork;
    if(privateNetwork != null && privateNetwork.length > 0) {
        if(isQuestionMarkAdded == false) {
            url.push("?");
            isQuestionMarkAdded = true;
        }
        else {
            url.push("&");
        }
        url.push("privatenetwork=" + privateNetwork);
    }

    var capacity = args.data.capacity;
    if(capacity != null && capacity.length > 0) {
        if(isQuestionMarkAdded == false) {
            url.push("?");
            isQuestionMarkAdded = true;
        }
        else {
            url.push("&");
        }
        url.push("fwdevicecapacity=" + capacity);
    }

    var dedicated = (args.data.dedicated == "on");	//boolean	(true/false)
    if(isQuestionMarkAdded == false) {
        url.push("?");
        isQuestionMarkAdded = true;
    }
    else {
        url.push("&");
    }
    url.push("fwdevicededicated=" + dedicated.toString());

    array1.push("&url=" + todb(url.join("")));
    //construct URL ends here

    $.ajax({
      url: createURL(apiCmd + array1.join("")),
      dataType: "json",
      success: function(json) {
        var jid = json[apiCmdRes].jobid;
        args.response.success(
          {_custom:
           {jobId: jid,
            getUpdatedItem: function(json) {
              var item = json.queryasyncjobresultresponse.jobresult[apiCmdObj];

              return item;
            }
           }
          }
        );
      }
    });
  }

	var afterCreateZonePhysicalNetworkTrafficTypes = function(newZoneObj, newPhysicalnetwork) {	  
		$.ajax({
			url: createURL("updatePhysicalNetwork&state=Enabled&id=" + newPhysicalnetwork.id),
			dataType: "json",
			success: function(json) {
				var jobId = json.updatephysicalnetworkresponse.jobid;
				var timerKey = "updatePhysicalNetworkJob_"+jobId;
				$("body").everyTime(2000, timerKey, function() {
					$.ajax({
						url: createURL("queryAsyncJobResult&jobId="+jobId),
						dataType: "json",
						success: function(json) {
							var result = json.queryasyncjobresultresponse;
							if (result.jobstatus == 0) {
								return; //Job has not completed
							}
							else {
								$("body").stopTime(timerKey);
								if (result.jobstatus == 1) {
									//alert("updatePhysicalNetwork succeeded.");

									// get network service provider ID of Virtual Router
									var virtualRouterProviderId;
									$.ajax({
										url: createURL("listNetworkServiceProviders&name=VirtualRouter&physicalNetworkId=" + newPhysicalnetwork.id),
										dataType: "json",
										async: false,
										success: function(json) {
											var items = json.listnetworkserviceprovidersresponse.networkserviceprovider;
											if(items != null && items.length > 0) {
												virtualRouterProviderId = items[0].id;
											}
										}
									});
									if(virtualRouterProviderId == null) {
										alert("error: listNetworkServiceProviders API doesn't return VirtualRouter provider ID");
										return;
									}

									var virtualRouterElementId;
									$.ajax({
										url: createURL("listVirtualRouterElements&nspid=" + virtualRouterProviderId),
										dataType: "json",
										async: false,
										success: function(json) {
											var items = json.listvirtualrouterelementsresponse.virtualrouterelement;
											if(items != null && items.length > 0) {
												virtualRouterElementId = items[0].id;
											}
										}
									});
									if(virtualRouterElementId == null) {
										alert("error: listVirtualRouterElements API doesn't return Virtual Router Element Id");
										return;
									}

									$.ajax({
										url: createURL("configureVirtualRouterElement&enabled=true&id=" + virtualRouterElementId),
										dataType: "json",
										async: false,
										success: function(json) {
											var jobId = json.configurevirtualrouterelementresponse.jobid;
											var timerKey = "configureVirtualRouterElementJob_"+jobId;
											$("body").everyTime(2000, timerKey, function() {
												$.ajax({
													url: createURL("queryAsyncJobResult&jobId="+jobId),
													dataType: "json",
													success: function(json) {
														var result = json.queryasyncjobresultresponse;
														if (result.jobstatus == 0) {
															return; //Job has not completed
														}
														else {
															$("body").stopTime(timerKey);
															if (result.jobstatus == 1) {
																//alert("configureVirtualRouterElement succeeded.");

																$.ajax({
																	url: createURL("updateNetworkServiceProvider&state=Enabled&id=" + virtualRouterProviderId),
																	dataType: "json",
																	async: false,
																	success: function(json) {
																		var jobId = json.updatenetworkserviceproviderresponse.jobid;
																		var timerKey = "updateNetworkServiceProviderJob_"+jobId;
																		$("body").everyTime(2000, timerKey, function() {
																			$.ajax({
																				url: createURL("queryAsyncJobResult&jobId="+jobId),
																				dataType: "json",
																				success: function(json) {
																					var result = json.queryasyncjobresultresponse;
																					if (result.jobstatus == 0) {
																						return; //Job has not completed
																					}
																					else {
																						$("body").stopTime(timerKey);
																						if (result.jobstatus == 1) {
																							//alert("Virtual Router Provider is enabled");
																							
																							if(newZoneObj.networktype == "Basic") {  
																								if(args.data["security-groups-enabled"] == "on") { //need to Enable security group provider first
																									// get network service provider ID of Security Group
																									var securityGroupProviderId;
																									$.ajax({
																										url: createURL("listNetworkServiceProviders&name=SecurityGroupProvider&physicalNetworkId=" + newPhysicalnetwork.id),
																										dataType: "json",
																										async: false,
																										success: function(json) {																																				
																											var items = json.listnetworkserviceprovidersresponse.networkserviceprovider;
																											if(items != null && items.length > 0) {
																												securityGroupProviderId = items[0].id;
																											}
																										}
																									});
																									if(securityGroupProviderId == null) {
																										alert("error: listNetworkServiceProviders API doesn't return security group provider ID");
																										return;
																									}                                      
																										
																									$.ajax({
																										url: createURL("updateNetworkServiceProvider&state=Enabled&id=" + securityGroupProviderId),
																										dataType: "json",
																										async: false,
																										success: function(json) {
																											var jobId = json.updatenetworkserviceproviderresponse.jobid;
																											var timerKey = "updateNetworkServiceProviderJob_"+jobId;
																											$("body").everyTime(2000, timerKey, function() {
																												$.ajax({
																													url: createURL("queryAsyncJobResult&jobId="+jobId),
																													dataType: "json",
																													success: function(json) {
																														var result = json.queryasyncjobresultresponse;
																														if (result.jobstatus == 0) {
																															return; //Job has not completed
																														}
																														else {																																										
																															$("body").stopTime(timerKey);
																															if (result.jobstatus == 1) {
																																//alert("Security group provider is enabled");

																																//create network (for basic zone only)
																																var array2 = [];
																																array2.push("&zoneid=" + newZoneObj.id);
																																array2.push("&name=guestNetworkForBasicZone");
																																array2.push("&displaytext=guestNetworkForBasicZone");
																																array2.push("&networkofferingid=" + args.data.networkOfferingId);
																																$.ajax({
																																	url: createURL("createNetwork" + array2.join("")),
																																	dataType: "json",
																																	async: false,
																																	success: function(json) {										  
																																		//create pod                                                                     
																																		var array3 = [];
																																		array3.push("&zoneId=" + newZoneObj.id);
																																		array3.push("&name=" + todb(args.data.podName));
																																		array3.push("&gateway=" + todb(args.data.podGateway));
																																		array3.push("&netmask=" + todb(args.data.podNetmask));
																																		array3.push("&startIp=" + todb(args.data.podStartIp));

																																		var endip = args.data.podEndIp;      //optional
																																		if (endip != null && endip.length > 0)
																																			array3.push("&endIp=" + todb(endip));

																																		$.ajax({
																																			url: createURL("createPod" + array3.join("")),
																																			dataType: "json",
																																			async: false,
																																			success: function(json) {
																																			
																																			},
																																			error: function(XMLHttpResponse) {
																																				var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
																																				alert("createPod failed. Error: " + errorMsg);
																																			}
																																		}); 	
																																	}
																																});
																															}
																															else if (result.jobstatus == 2) {
																																alert("failed to enable security group provider. Error: " + fromdb(result.jobresult.errortext));
																															}
																														}
																													},
																													error: function(XMLHttpResponse) {
																														var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
																														alert("updateNetworkServiceProvider failed. Error: " + errorMsg);
																													}
																												});
																											});
																										}
																									});
																								}					
																								else { 
																									//create network (for basic zone only)
																									var array2 = [];
																									array2.push("&zoneid=" + newZoneObj.id);
																									array2.push("&name=guestNetworkForBasicZone");
																									array2.push("&displaytext=guestNetworkForBasicZone");
																									array2.push("&networkofferingid=" + args.data.networkOfferingId);
																									$.ajax({
																										url: createURL("createNetwork" + array2.join("")),
																										dataType: "json",
																										async: false,
																										success: function(json) {				
																											//create pod                                                                     
																											var array3 = [];
																											array3.push("&zoneId=" + newZoneObj.id);
																											array3.push("&name=" + todb(args.data.podName));
																											array3.push("&gateway=" + todb(args.data.podGateway));
																											array3.push("&netmask=" + todb(args.data.podNetmask));
																											array3.push("&startIp=" + todb(args.data.podStartIp));

																											var endip = args.data.podEndIp;      //optional
																											if (endip != null && endip.length > 0)
																												array3.push("&endIp=" + todb(endip));

																											$.ajax({
																												url: createURL("createPod" + array3.join("")),
																												dataType: "json",
																												async: false,
																												success: function(json) {
																												
																												},
																												error: function(XMLHttpResponse) {
																													var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
																													alert("createPod failed. Error: " + errorMsg);
																												}
																											}); 				
																										}
																									});		
																								}	
																							}
																							else {  //Advanced zone
																								//create pod                                                                     
																								var array3 = [];
																								array3.push("&zoneId=" + newZoneObj.id);
																								array3.push("&name=" + todb(args.data.podName));
																								array3.push("&gateway=" + todb(args.data.podGateway));
																								array3.push("&netmask=" + todb(args.data.podNetmask));
																								array3.push("&startIp=" + todb(args.data.podStartIp));

																								var endip = args.data.podEndIp;      //optional
																								if (endip != null && endip.length > 0)
																									array3.push("&endIp=" + todb(endip));

																								$.ajax({
																									url: createURL("createPod" + array3.join("")),
																									dataType: "json",
																									async: false,
																									success: function(json) {
																									
																									},
																									error: function(XMLHttpResponse) {
																										var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
																										alert("createPod failed. Error: " + errorMsg);
																									}
																								}); 
																							}																																		  
																						}
																						else if (result.jobstatus == 2) {
																							alert("failed to enable Virtual Router Provider. Error: " + fromdb(result.jobresult.errortext));
																						}
																					}
																				},
																				error: function(XMLHttpResponse) {
																					var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
																					alert("updateNetworkServiceProvider failed. Error: " + errorMsg);
																				}
																			});
																		});
																	}
																});
															}
															else if (result.jobstatus == 2) {
																alert("configureVirtualRouterElement failed. Error: " + fromdb(result.jobresult.errortext));
															}
														}
													},
													error: function(XMLHttpResponse) {
														var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
														alert("configureVirtualRouterElement failed. Error: " + errorMsg);
													}
												});
											});
										}
									});
								}
								else if (result.jobstatus == 2) {
									alert("updatePhysicalNetwork failed. Error: " + fromdb(result.jobresult.errortext));
								}
							}
						},
						error: function(XMLHttpResponse) {
							var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
							alert("updatePhysicalNetwork failed. Error: " + errorMsg);
						}
					});
				});
			}
		});		
	}
	
  //action filters (begin)
  var zoneActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = ['enableSwift'];
    allowedActions.push("edit");
    if(jsonObj.allocationstate == "Disabled")
      allowedActions.push("enable");
    else if(jsonObj.allocationstate == "Enabled")
      allowedActions.push("disable");
    allowedActions.push("delete");
    return allowedActions;
  }

  var podActionfilter = function(args) {
    var podObj = args.context.item;
    var allowedActions = [];
    allowedActions.push("edit");
    if(podObj.allocationstate == "Disabled")
      allowedActions.push("enable");
    else if(podObj.allocationstate == "Enabled")
      allowedActions.push("disable");
    allowedActions.push("delete");

    /*
    var selectedZoneObj;
    $(zoneObjs).each(function(){
      if(this.id == podObj.zoneid) {
        selectedZoneObj = this;
        return false;  //break the $.each() loop
      }
    });
    */

    if(selectedZoneObj.networktype == "Basic") { //basic-mode network (pod-wide VLAN)
      //$("#tab_ipallocation, #add_iprange_button, #tab_network_device, #add_network_device_button").show();
      allowedActions.push("addIpRange");
      allowedActions.push("addNetworkDevice");
    }
    else if(selectedZoneObj.networktype == "Advanced") { //advanced-mode network (zone-wide VLAN)
      //$("#tab_ipallocation, #add_iprange_button, #tab_network_device, #add_network_device_button").hide();
    }

    return allowedActions;
  }

  var networkDeviceActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];
    return allowedActions;
  }

  var clusterActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];
		
    if(jsonObj.state == "Enabled") {//managed, allocation enabled
		  allowedActions.push("unmanage");
      allowedActions.push("disable");			
		}
		else if(jsonObj.state == "Disabled") { //managed, allocation disabled
		  allowedActions.push("unmanage");
      allowedActions.push("enable");			
		}
		else { //Unmanaged, PrepareUnmanaged , PrepareUnmanagedError
			allowedActions.push("manage");
		}

    allowedActions.push("delete");

    return allowedActions;
  }

  var hostActionfilter = function(args) {   
    var jsonObj = args.context.item;
    var allowedActions = [];

    if (jsonObj.resourcestate == "Enabled") {
      allowedActions.push("edit");
      allowedActions.push("enableMaintenanceMode");
      allowedActions.push("forceReconnect");
    }
    else if (jsonObj.resourcestate == "ErrorInMaintenance") {
      allowedActions.push("edit");
      allowedActions.push("enableMaintenanceMode");
      allowedActions.push("cancelMaintenanceMode");
    }
    else if (jsonObj.resourcestate == "PrepareForMaintenance") {
      allowedActions.push("edit");
      allowedActions.push("cancelMaintenanceMode");
    }
    else if (jsonObj.resourcestate == "Maintenance") {
      allowedActions.push("edit");
      allowedActions.push("cancelMaintenanceMode");
      allowedActions.push("delete");
    }
    else if (jsonObj.resourcestate == "Disabled"){
      allowedActions.push("edit");
      allowedActions.push("delete");
    }
    return allowedActions;
  }

  var primarystorageActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];

		allowedActions.push("edit");
		
    if (jsonObj.state == 'Up' || jsonObj.state == "Connecting") {
      allowedActions.push("enableMaintenanceMode");
    }
    else if(jsonObj.state == 'Down') {
      allowedActions.push("enableMaintenanceMode");
      allowedActions.push("delete");
    }
    else if(jsonObj.state == "Alert") {
      allowedActions.push("delete");
    }
    else if (jsonObj.state == "ErrorInMaintenance") {
      allowedActions.push("enableMaintenanceMode");
      allowedActions.push("cancelMaintenanceMode");
    }
    else if (jsonObj.state == "PrepareForMaintenance") {
      allowedActions.push("cancelMaintenanceMode");
    }
    else if (jsonObj.state == "Maintenance") {
      allowedActions.push("cancelMaintenanceMode");
      allowedActions.push("delete");
    }
    else if (jsonObj.state == "Disconnected"){
      allowedActions.push("delete");
    }
    return allowedActions;
  }

  var secondarystorageActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];
    allowedActions.push("delete");
    return allowedActions;
  }

  var publicNetworkActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];
    allowedActions.push("addIpRange");
    return allowedActions;
  }

  var directNetworkActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];
    allowedActions.push("addIpRange");
    allowedActions.push("edit");
    allowedActions.push("delete");
    return allowedActions;
  }

  var routerActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];

    if (jsonObj.state == 'Running') {
      allowedActions.push("stop");
      allowedActions.push("restart");
      allowedActions.push("changeService");
      allowedActions.push("viewConsole");
      if (isAdmin())
        allowedActions.push("migrate");
    }
    else if (jsonObj.state == 'Stopped') {
      allowedActions.push("start");
      allowedActions.push("changeService");
    }
    return allowedActions;
  }

  var systemvmActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];

    if (jsonObj.state == 'Running') {
      allowedActions.push("stop");
      allowedActions.push("restart");
      allowedActions.push("delete");  //destroy
      allowedActions.push("viewConsole");
      if (isAdmin())
        allowedActions.push("migrate");
    }
    else if (jsonObj.state == 'Stopped') {
      allowedActions.push("start");
      allowedActions.push("delete");  //destroy
    }
    else if (jsonObj.state == 'Error') {
      allowedActions.push("delete");  //destroy
    }
    return allowedActions;
  }
  //action filters (end)

})($, cloudStack, testData);
