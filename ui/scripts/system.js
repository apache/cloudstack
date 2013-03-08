// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

(function($, cloudStack) { 

  var zoneObjs, podObjs, clusterObjs, domainObjs, networkOfferingObjs, physicalNetworkObjs;
  var selectedClusterObj, selectedZoneObj, selectedPublicNetworkObj, selectedManagementNetworkObj, selectedPhysicalNetworkObj, selectedGuestNetworkObj; 
  var nspMap = {}; //from listNetworkServiceProviders API 
	var nspHardcodingArray = []; //for service providers listView (hardcoding, not from listNetworkServiceProviders API) 

  // Add router type to virtual router
  // -- can be either Project, VPC, or System (standard)
  var mapRouterType = function(index, router) {
    var routerType = _l('label.menu.system');

    if (router.projectid) routerType = _l('label.project');
    if (router.vpcid) routerType = 'VPC';

    return $.extend(router, {
      routerType: routerType
    });
  };
	
  cloudStack.publicIpRangeAccount = {
    dialog: function(args) {      
      return function(args) {
        var data = args.data ? args.data : {};
        var fields = {
          account: { label: 'Account', defaultValue: data.account },
          domainid: {
            label: 'Domain',
            defaultValue: data.domainid,
            select: function(args) {
              $.ajax({
                url: createURL('listDomains'),
                data: { listAll: true },
                success: function(json) {
                  args.response.success({
                    data: $.map(json.listdomainsresponse.domain, function(domain) {
                      return {
                        id: domain.id,
                        description: domain.path
                      };
                    })
                  });
                }
              });
            }
          }
        };
        var success = args.response.success;
        
        if (args.$item) { // Account data is read-only after creation
          $.ajax({
            url: createURL('listDomains'),
            data: { id: data.domainid, listAll: true },
            success: function(json) {
              var domain = json.listdomainsresponse.domain[0];
              
              cloudStack.dialog.notice({
                message: '<ul><li>' + _l('label.account') + ': ' + data.account + '</li>' +
                  '<li>' + _l('label.domain') + ': ' + domain.path + '</li></ul>'
              });
            }
          });
        } else {
          cloudStack.dialog.createForm({
            form: {
              title: 'label.add.account',
              desc: '(optional) Please specify an account to be associated with this IP range.',
              fields: fields
            },
            after: function(args) {
              var data = cloudStack.serializeForm(args.$form);
              
              success({ data: data });
            }
          });
        }
      };     
    }
  };
 
  var getTrafficType = function(physicalNetwork, typeID) {
    var trafficType = {};

    $.ajax({
      url: createURL('listTrafficTypes'),
      data: {
        physicalnetworkid: physicalNetwork.id
      },
      async: false,
      success: function(json) {
        trafficType = $.grep(
          json.listtraffictypesresponse.traffictype,
          function(trafficType) {
            return trafficType.traffictype == typeID;
          }
        )[0];
      }
    });

		if(trafficType.xennetworklabel == null || trafficType.xennetworklabel == 0)
		  trafficType.xennetworklabel = dictionary['label.network.label.display.for.blank.value'];
		if(trafficType.kvmnetworklabel == null || trafficType.kvmnetworklabel == 0)
		  trafficType.kvmnetworklabel = dictionary['label.network.label.display.for.blank.value'];
		if(trafficType.vmwarenetworklabel == null || trafficType.vmwarenetworklabel == 0)
		  trafficType.vmwarenetworklabel = dictionary['label.network.label.display.for.blank.value'];
	        if(trafficType.ovmnetworklabel == null || trafficType.ovmnetworklabel == 0)
                   trafficType.ovmnetworklabel = dictionary['label.network.label.display.for.blank.value'];	
    return trafficType;
  };

  var updateTrafficLabels = function(trafficType, labels, complete) {
    var array1 = [];
		if(labels.xennetworklabel != dictionary['label.network.label.display.for.blank.value'])
		  array1.push("&xennetworklabel=" + labels.xennetworklabel);
		if(labels.kvmnetworklabel != dictionary['label.network.label.display.for.blank.value'])
		  array1.push("&kvmnetworklabel=" + labels.kvmnetworklabel);
		if(labels.vmwarenetworklabel != dictionary['label.network.label.display.for.blank.value'])
		  array1.push("&vmwarenetworklabel=" + labels.vmwarenetworklabel);
	        if(labels.ovmnetworklabel != dictionary['label.network.label.display.for.blank.value'])
                  array1.push("&ovmnetworklabel=" + labels.ovmnetworklabel); 		
		$.ajax({
      url: createURL('updateTrafficType' + array1.join("")),
      data: {
        id: trafficType.id       
      },
      success: function(json) {
        var jobID = json.updatetraffictyperesponse.jobid;

        cloudStack.ui.notifications.add(
          {
            desc: 'Update traffic labels',
            poll: pollAsyncJobResult,
            section: 'System',
            _custom: { jobId: jobID }
          },
          complete ? complete : function() {}, {},
          function(data) {
            // Error
            cloudStack.dialog.notice({ message: parseXMLHttpResponse(data) });
          }, {}
        );
      }
    })
  };

  function virtualRouterProviderActionFilter(args) { 	  
    var allowedActions = [];    
		var jsonObj = args.context.item; //args.context.item == nspMap["virtualRouter"]
    if(jsonObj.state == "Enabled")
      allowedActions.push("disable");
    else if(jsonObj.state == "Disabled")
      allowedActions.push("enable");
    return allowedActions;
  };

  cloudStack.sections.system = {
    title: 'label.menu.infrastructure',
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
                    json.listzonesresponse.count : 0,
                  zones: json.listzonesresponse.zone
                }));
              }
            });
          },

          podCount: function(data) {
            $.ajax({
              url: createURL('listPods'),
							data: {
							  page: 1,
								pagesize: 1  //specifying pagesize as 1 because we don't need any embedded objects to be returned here. The only thing we need from API response is "count" property.
							},
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
							data: {
							  page: 1,
								pagesize: 1  //specifying pagesize as 1 because we don't need any embedded objects to be returned here. The only thing we need from API response is "count" property.
							},
              success: function(json) {                
								dataFns.hostCount($.extend(data, {
                  clusterCount: json.listclustersresponse.count ?
                    json.listclustersresponse.count : 0
                }));
																
								//comment the 4 lines above and uncomment the following 4 lines if listHosts API still responds slowly.
								
								/*
								dataFns.primaryStorageCount($.extend(data, {
                  clusterCount: json.listclustersresponse.count ?
                    json.listclustersresponse.count : 0
                }));
								*/
              }
            });
          },

          hostCount: function(data) {
            $.ajax({
              url: createURL('listHosts'),
              data: {
                type: 'routing',
								page: 1,
								pagesize: 1  //specifying pagesize as 1 because we don't need any embedded objects to be returned here. The only thing we need from API response is "count" property.
              },
              success: function(json) {
                dataFns.primaryStorageCount($.extend(data, {
                  hostCount: json.listhostsresponse.count ?
                    json.listhostsresponse.count : 0
                }));
              }
            });
          },

          primaryStorageCount: function(data) {
            $.ajax({
              url: createURL('listStoragePools'),
							data: {
							  page: 1,
								pagesize: 1  //specifying pagesize as 1 because we don't need any embedded objects to be returned here. The only thing we need from API response is "count" property.
							},
              success: function(json) {                
								dataFns.secondaryStorageCount($.extend(data, {
                  primaryStorageCount: json.liststoragepoolsresponse.count ?
                    json.liststoragepoolsresponse.count : 0
                }));
																
								//comment the 4 lines above and uncomment the following 4 lines if listHosts API still responds slowly.
								
								/*
								dataFns.systemVmCount($.extend(data, {
                  primaryStorageCount: json.liststoragepoolsresponse.count ?
                    json.liststoragepoolsresponse.count : 0
                }));
								*/
              }
            });
          },

          secondaryStorageCount: function(data) {
            $.ajax({
              url: createURL('listHosts'),
              data: {
                type: 'SecondaryStorage',
								page: 1,
								pagesize: 1  //specifying pagesize as 1 because we don't need any embedded objects to be returned here. The only thing we need from API response is "count" property.
              },
              success: function(json) {
                dataFns.systemVmCount($.extend(data, {
                  secondaryStorageCount: json.listhostsresponse.count ?
                    json.listhostsresponse.count : 0
                }));
              }
            });
          },

          systemVmCount: function(data) {
            $.ajax({
              url: createURL('listSystemVms'),
							data: {
							  page: 1,
								pagesize: 1  //specifying pagesize as 1 because we don't need any embedded objects to be returned here. The only thing we need from API response is "count" property.
							},
              success: function(json) {
                dataFns.virtualRouterCount($.extend(data, {
                  systemVmCount: json.listsystemvmsresponse.count ?
                    json.listsystemvmsresponse.count : 0
                }));
              }
            });
          },

          virtualRouterCount: function(data) {
            $.ajax({
              url: createURL('listRouters'),
              data: {
                projectid: -1,
								page: 1,
								pagesize: 1  //specifying pagesize as 1 because we don't need any embedded objects to be returned here. The only thing we need from API response is "count" property.
              },
              success: function(json) {
                var total1 = json.listroutersresponse.count ? json.listroutersresponse.count : 0;								
								$.ajax({
								  url: createURL('listRouters'),
									data: {
									  listAll: true,
										page: 1,
								    pagesize: 1  //specifying pagesize as 1 because we don't need any embedded objects to be returned here. The only thing we need from API response is "count" property.
									},
									success: function(json) {
									  var total2 = json.listroutersresponse.count ? json.listroutersresponse.count : 0;		
										dataFns.capacity($.extend(data, {
											virtualRouterCount: (total1 + total2)
										}));																	
									}									
								});										
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

    zoneDashboard: function(args) {
      $.ajax({
        url: createURL('listCapacity'),
        data: { zoneid: args.context.zones[0].id },
        success: function(json) {
          var capacities = json.listcapacityresponse.capacity;
          var data = {};

          $(capacities).each(function() {
            var capacity = this;

            data[capacity.type] = {
              used: cloudStack.converters.convertByType(capacity.type, capacity.capacityused),
              total: cloudStack.converters.convertByType(capacity.type, capacity.capacitytotal),
              percent: parseInt(capacity.percentused)
            };
          });

          args.response.success({
            data: data
          });
        }
      });
    },

    // Network-as-a-service configuration
    naas: {
      providerListView: {
        id: 'networkProviders',
        fields: {
          name: { label: 'label.name' },
          state: {
            label: 'label.state',
            converter: function(str) {
              // For localization
              return str;
            },
            indicator: { 'Enabled': 'on', 'Disabled': 'off' }
          }
        },
        dataProvider: function(args) {
					refreshNspData();
          args.response.success({
            data: nspHardcodingArray
          })
        },

        detailView: function(args) {
          return cloudStack.sections.system.naas.networkProviders.types[
            args.context.networkProviders[0].id
          ];
        }
      },
      mainNetworks: {
        'public': {
          detailView: {
            actions: {
              edit: {
                label: 'label.edit',
                action: function(args) {
                  var trafficType = getTrafficType(selectedPhysicalNetworkObj, 'Public');

                  updateTrafficLabels(trafficType, args.data, function () {
                    args.response.success();
                  });
                }
              }
            },
            tabs: {
              details: {
                title: 'label.details',
                fields: [
                  {
                    traffictype: { label: 'label.traffic.type' },
                    broadcastdomaintype: { label: 'label.broadcast.domain.type' }
                  },
                  {
                    xennetworklabel: { label: 'label.xen.traffic.label', isEditable: true },
                    kvmnetworklabel: { label: 'label.kvm.traffic.label', isEditable: true },
                    vmwarenetworklabel: { label: 'label.vmware.traffic.label', isEditable: true },
                    ovmnetworklabel: { label: 'OVM traffic label',isEditable: true }
                  }
                ],

                dataProvider: function(args) {
                  $.ajax({
                    url: createURL("listNetworks&listAll=true&trafficType=Public&isSystem=true&zoneId="+selectedZoneObj.id),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      var trafficType = getTrafficType(selectedPhysicalNetworkObj, 'Public');
                      var items = json.listnetworksresponse.network;

                      selectedPublicNetworkObj = items[0];

                      // Include traffic labels
                      selectedPublicNetworkObj.xennetworklabel = trafficType.xennetworklabel;
                      selectedPublicNetworkObj.kvmnetworklabel = trafficType.kvmnetworklabel;
                      selectedPublicNetworkObj.vmwarenetworklabel = trafficType.vmwarenetworklabel;
                      selectedPublicNetworkObj.ovmnetworklabel = trafficType.ovmnetworklabel;

                      args.response.success({data: selectedPublicNetworkObj});
                    }
                  });
                }
              },

              ipAddresses: {
                title: 'label.ip.ranges',
                custom: function(args) {
                  return $('<div></div>').multiEdit({
                    context: args.context,
                    noSelect: true,
                    fields: {
                      'gateway': { edit: true, label: 'label.gateway' },
                      'netmask': { edit: true, label: 'label.netmask' },
                      'vlan': { edit: true, label: 'label.vlan', isOptional: true },
                      'startip': { edit: true, label: 'label.start.IP' },
                      'endip': { edit: true, label: 'label.end.IP' },
                      'account': {
                        label: 'label.account',
                        custom: {
                          buttonLabel: 'label.add.account',
                          action: cloudStack.publicIpRangeAccount.dialog()
                        }
                      },
                      'add-rule': { label: 'label.add', addButton: true }
                    },
                    add: {
                      label: 'label.add',
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

                        if (args.data.account) {
                          array1.push("&account=" + args.data.account.account);
                          array1.push("&domainid=" + args.data.account.domainid);
                        }

                        array1.push("&forVirtualNetwork=true");  //indicates this new IP range is for public network, not guest network

                        $.ajax({
                          url: createURL("createVlanIpRange" + array1.join("")),
                          dataType: "json",
                          success: function(json) {
                            var item = json.createvlaniprangeresponse.vlan;
                            args.response.success({
                              data: item,
                              notification: {
                                label: 'label.add.ip.range',
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
                        label: 'label.remove.ip.range',
                        action: function(args) {
                          $.ajax({
                            url: createURL('deleteVlanIpRange&id=' + args.context.multiRule[0].id),
                            dataType: 'json',
                            async: true,
                            success: function(json) {
                              args.response.success({
                                notification: {
                                  label: 'label.remove.ip.range',
                                  poll: function(args) {
                                    args.complete();
                                  }
                                }
                              });
                            },
                            error: function(json) {
                              args.response.error(parseXMLHttpResponse(json));
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

                          args.response.success({
                            data: $.map(items, function(item) {
                              return $.extend(item, {
                                account: {
                                  _buttonLabel: item.account,
                                  account: item.account,
                                  domainid: item.domainid
                                }
                              });
                            })
                          });
                        }
                      });
                    }
                  });
                }
              }
            }
          }
        },

        'storage': {
          detailView: {
            actions: {
              edit: {
                label: 'label.edit',
                action: function(args) {
                  var trafficType = getTrafficType(selectedPhysicalNetworkObj, 'Storage');

                  updateTrafficLabels(trafficType, args.data, function () {
                    args.response.success();
                  });
                }
              }
            },
            tabs: {
              details: {
                title: 'label.details',
                fields: [
                  {
                    traffictype: { label: 'label.traffic.type' },
                    broadcastdomaintype: { label: 'label.broadcast.domain.type' }
                  },
                  {
                    xennetworklabel: { label: 'label.xen.traffic.label', isEditable: true },
                    kvmnetworklabel: { label: 'label.kvm.traffic.label', isEditable: true },
                    vmwarenetworklabel: { label: 'label.vmware.traffic.label', isEditable: true },
                    ovmnetworklabel: { label: 'OVM traffic label', isEditable: true }
                  }
                ],

                dataProvider: function(args) {
                  $.ajax({
                    url: createURL("listNetworks&listAll=true&trafficType=Storage&isSystem=true&zoneId="+selectedZoneObj.id),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      var items = json.listnetworksresponse.network;
                      var trafficType = getTrafficType(selectedPhysicalNetworkObj, 'Storage');
                      selectedPublicNetworkObj = items[0];

                      selectedPublicNetworkObj.xennetworklabel = trafficType.xennetworklabel;
                      selectedPublicNetworkObj.kvmnetworklabel = trafficType.kvmnetworklabel;
                      selectedPublicNetworkObj.vmwarenetworklabel = trafficType.vmwarenetworklabel;
                      selectedPublicNetworkObj.ovmnetworklabel = trafficType.ovmnetworklabel;

                      args.response.success({data: selectedPublicNetworkObj});
                    }
                  });
                }
              },

              ipAddresses: {
                title: 'label.ip.ranges',
                custom: function(args) {
                  return $('<div></div>').multiEdit({
                    context: args.context,
                    noSelect: true,
                    fields: {
											'podid': {
											  label: 'label.pod',
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
                      'gateway': { edit: true, label: 'label.gateway' },
                      'netmask': { edit: true, label: 'label.netmask' },
                      'vlan': { edit: true, label: 'label.vlan', isOptional: true },
                      'startip': { edit: true, label: 'label.start.IP' },
                      'endip': { edit: true, label: 'label.end.IP' },
                      'add-rule': { label: 'label.add', addButton: true }
                    },
                    add: {
                      label: 'label.add',
                      action: function(args) {
                        var array1 = [];
                        array1.push("&zoneId=" + args.context.zones[0].id);
												array1.push("&podid=" + args.data.podid);

												array1.push("&gateway=" + args.data.gateway);

                        if (args.data.vlan != null && args.data.vlan.length > 0)
                          array1.push("&vlan=" + todb(args.data.vlan));

                        array1.push("&netmask=" + args.data.netmask);
                        array1.push("&startip=" + args.data.startip);
                        if(args.data.endip != null && args.data.endip.length > 0)
                          array1.push("&endip=" + args.data.endip);

                        $.ajax({
                          url: createURL("createStorageNetworkIpRange" + array1.join("")),
                          dataType: "json",
                          success: function(json) {
														args.response.success({
															_custom: {
																jobId: json.createstoragenetworkiprangeresponse.jobid
															},
															notification: {
																label: 'label.add.ip.range',
																poll: pollAsyncJobResult
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
                        label: 'label.delete',
                        action: function(args) {
                          $.ajax({
                            url: createURL('deleteStorageNetworkIpRange&id=' + args.context.multiRule[0].id),
                            dataType: 'json',
                            async: true,
                            success: function(json) {
                              args.response.success({
                                notification: {
                                  label: 'label.remove.ip.range',
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
                        url: createURL("listStorageNetworkIpRange&zoneid=" + args.context.zones[0].id + "&networkId=" + selectedPublicNetworkObj.id),
                        dataType: "json",
                        success: function(json) {
                          var items = json.liststoragenetworkiprangeresponse.storagenetworkiprange;
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
            actions: {
              edit: {
                label: 'label.edit',
                action: function(args) {
                  var trafficType = getTrafficType(selectedPhysicalNetworkObj, 'Management');

                  updateTrafficLabels(trafficType, args.data, function () {
                    args.response.success();
                  });
                }
              }
            },
            tabs: {
              details: {
                title: 'label.details',
                fields: [
                  {
                    traffictype: { label: 'label.traffic.type' },
                    broadcastdomaintype: { label: 'label.broadcast.domain.type' }
                  },
                  {
                    xennetworklabel: { label: 'label.xen.traffic.label', isEditable: true },
                    kvmnetworklabel: { label: 'label.kvm.traffic.label', isEditable: true },
                    vmwarenetworklabel: { label: 'label.vmware.traffic.label', isEditable: true },
                    ovmnetworklabel: { label: 'OVM traffic label', isEditable: true } 
                  }
                ],
                dataProvider: function(args) {
                  $.ajax({
                    url: createURL("listNetworks&listAll=true&issystem=true&trafficType=Management&zoneId=" + selectedZoneObj.id),
                    dataType: "json",
                    success: function(json) {
                      selectedManagementNetworkObj =json.listnetworksresponse.network[0];

                      var trafficType = getTrafficType(selectedPhysicalNetworkObj, 'Management');

                      selectedManagementNetworkObj.xennetworklabel = trafficType.xennetworklabel;
                      selectedManagementNetworkObj.kvmnetworklabel = trafficType.kvmnetworklabel;
                      selectedManagementNetworkObj.vmwarenetworklabel = trafficType.vmwarenetworklabel;
                      selectedManagementNetworkObj.ovmnetworklabel = trafficType.ovmnetworklabel;
                      args.response.success({ data: selectedManagementNetworkObj });
                    }
                  });
                }
              },
              ipAddresses: { //read-only listView (no actions) filled with pod info (not VlanIpRange info)
                title: 'label.ip.ranges',
								listView: {
									fields: {
										name: { label: 'label.pod' }, //pod name
										gateway: { label: 'label.gateway' },  //'Reserved system gateway' is too long and causes a visual format bug (2 lines overlay)
										netmask: { label: 'label.netmask' },  //'Reserved system netmask' is too long and causes a visual format bug (2 lines overlay)
										startip: { label: 'label.start.IP' }, //'Reserved system start IP' is too long and causes a visual format bug (2 lines overlay)
										endip: { label: 'label.end.IP' }      //'Reserved system end IP' is too long and causes a visual format bug (2 lines overlay)
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

        'guest': { //physical network + Guest traffic type
          detailView: {
            actions: {
              edit: {
                label: 'label.edit',
                action: function(args) {
                  var vlan;
                  if(args.data.endVlan == null || args.data.endVlan.length == 0)
                    vlan = args.data.startVlan;
                  else
                    vlan = args.data.startVlan + "-" + args.data.endVlan;

									var array1 = [];
                  if(vlan != null && vlan.length > 0)
                    array1.push("&vlan=" + todb(vlan));
                  if(args.data.tags != null && args.data.tags.length > 0)
                    array1.push("&tags=" + todb(args.data.tags));

                  $.ajax({
                    url: createURL("updatePhysicalNetwork&id=" + selectedPhysicalNetworkObj.id + array1.join("")),
                    dataType: "json",
                    success: function(json) {
                      var jobId = json.updatephysicalnetworkresponse.jobid;

                      var trafficType = getTrafficType(selectedPhysicalNetworkObj, 'Guest');

                      updateTrafficLabels(trafficType, args.data, function() {
                        args.response.success({ _custom: { jobId: jobId }});
                      });
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
                title: 'label.details',
                preFilter: function(args) {
                  var hiddenFields = [];
                  if(selectedZoneObj.networktype == "Basic") {
                    hiddenFields.push("startVlan");
                    hiddenFields.push("endVlan");
                  }
                  return hiddenFields;
                },
                fields: [
                  { //updatePhysicalNetwork API
                    state: { label: 'label.state' },
                    startVlan: {
                      label: 'label.start.vlan',
                      isEditable: true
                    },
                    endVlan: {
                      label: 'label.end.vlan',
                      isEditable: true
                    },
										tags: { label: 'Tags', isEditable: true },
                    broadcastdomainrange: { label: 'label.broadcast.domain.range' }
                  },
                  { //updateTrafficType API
                    xennetworklabel: { label: 'label.xen.traffic.label', isEditable: true },
                    kvmnetworklabel: { label: 'label.kvm.traffic.label', isEditable: true },
                    vmwarenetworklabel: { label: 'label.vmware.traffic.label', isEditable: true },
                    ovmnetworklabel: { label: 'OVM traffic label', isEditable: true }
                  }
                ],
                dataProvider: function(args) { //physical network + Guest traffic type       
									//refresh physical network									
									$.ajax({
										url: createURL('listPhysicalNetworks'),
										data: {
											id: args.context.physicalNetworks[0].id
										},
										async: true,
										success: function(json) {										  
											selectedPhysicalNetworkObj = json.listphysicalnetworksresponse.physicalnetwork[0];			
											
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

											//traffic type
											var xentrafficlabel, kvmtrafficlabel, vmwaretrafficlabel;
											var trafficType = getTrafficType(selectedPhysicalNetworkObj, 'Guest'); //refresh Guest traffic type
											selectedPhysicalNetworkObj["xennetworklabel"] = trafficType.xennetworklabel;
											selectedPhysicalNetworkObj["kvmnetworklabel"] = trafficType.kvmnetworklabel;
											selectedPhysicalNetworkObj["vmwarenetworklabel"] = trafficType.vmwarenetworklabel;
                                                                                        selectedPhysicalNetworkObj["ovmnetworklabel"] = trafficType.ovmnetworklabel;

											args.response.success({
												actionFilter: function() {
													var allowedActions = ['edit'];
													return allowedActions;
												},
												data: selectedPhysicalNetworkObj
											});											
										}
									});	
                }
              },

              ipAddresses: {
                title: 'label.ip.ranges',
                custom: function(args) {
                  return $('<div></div>').multiEdit({
                    context: args.context,
                    noSelect: true,
                    fields: {
										  'podid': {
											  label: 'label.pod',
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
                      'gateway': { edit: true, label: 'label.gateway' },
                      'netmask': { edit: true, label: 'label.netmask' },
                      'startip': { edit: true, label: 'label.start.IP' },
                      'endip': { edit: true, label: 'label.end.IP' },
                      'add-rule': { label: 'label.add', addButton: true }
                    },
                    add: {
                      label: 'label.add',
                      action: function(args) {
                        var array1 = [];
                        array1.push("&podid=" + args.data.podid);
												array1.push("&networkid=" + selectedGuestNetworkObj.id);
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
                                label: 'label.add.ip.range',
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
                        label: 'label.remove.ip.range',
                        action: function(args) {
                          $.ajax({
                            url: createURL('deleteVlanIpRange&id=' + args.context.multiRule[0].id),
                            dataType: 'json',
                            async: true,
                            success: function(json) {
                              args.response.success({
                                notification: {
                                  label: 'label.remove.ip.range',
                                  poll: function(args) {
                                    args.complete();
                                  }
                                }
                              });
                            },
                            error: function(json) {
                              args.response.error(parseXMLHttpResponse(json));
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
                title: 'label.network',
                listView: {
                  section: 'networks',
                  id: 'networks',
                  fields: {
                    name: { label: 'label.name' },
                    type: { label: 'label.type' },
                    vlan: { label: 'label.vlan.id' },
                    cidr: { label: 'IPv4 CIDR' },
                    ip6cidr: { label: 'IPv6 CIDR'}
                    //scope: { label: 'label.scope' }
                  },
                  actions: {
                    add: {
                      label: 'label.add.guest.network',

                      messages: {
                        confirm: function(args) {
                          return 'message.add.guest.network';
                        },
                        notification: function(args) {
                          return 'label.add.guest.network';
                        }
                      },

                      createForm: {
                        title: 'label.add.guest.network',  //Add guest network in advanced zone

                        fields: {
                          name: {
                            docID: 'helpGuestNetworkZoneName',
                            label: 'label.name',
                            validation: { required: true }
                          },
                          description: {
                            label: 'label.description',
                            docID: 'helpGuestNetworkZoneDescription',
                            validation: { required: true }
                          },
                          vlanId: {
                            label: 'label.vlan.id',
                            docID: 'helpGuestNetworkZoneVLANID'
                          },

                          scope: {
                            label: 'label.scope',
                            docID: 'helpGuestNetworkZoneScope',
                            select: function(args) {
                              var array1 = [];															
															if(args.context.zones[0].networktype == "Advanced" && args.context.zones[0].securitygroupsenabled	== true) {															  
																array1.push({id: 'zone-wide', description: 'All'});
															}
															else {															
																array1.push({id: 'zone-wide', description: 'All'});
																array1.push({id: 'domain-specific', description: 'Domain'});
																array1.push({id: 'account-specific', description: 'Account'});
																array1.push({id: 'project-specific', description: 'Project'});
															}
                              args.response.success({data: array1});

                              args.$select.change(function() {
                                var $form = $(this).closest('form');
                                if($(this).val() == "zone-wide") {
                                  $form.find('.form-item[rel=domainId]').hide();
                                  $form.find('.form-item[rel=subdomainaccess]').hide();
                                  $form.find('.form-item[rel=account]').hide();
																	$form.find('.form-item[rel=projectId]').hide();
                                }
                                else if ($(this).val() == "domain-specific") {
                                  $form.find('.form-item[rel=domainId]').css('display', 'inline-block');
                                  $form.find('.form-item[rel=subdomainaccess]').css('display', 'inline-block');
                                  $form.find('.form-item[rel=account]').hide();
																	$form.find('.form-item[rel=projectId]').hide();
                                }
                                else if($(this).val() == "account-specific") {
                                  $form.find('.form-item[rel=domainId]').css('display', 'inline-block');
                                  $form.find('.form-item[rel=subdomainaccess]').hide();
                                  $form.find('.form-item[rel=account]').css('display', 'inline-block');
																	$form.find('.form-item[rel=projectId]').hide();
                                }
																else if($(this).val() == "project-specific") {
                                  $form.find('.form-item[rel=domainId]').css('display', 'inline-block');
                                  $form.find('.form-item[rel=subdomainaccess]').hide();
                                  $form.find('.form-item[rel=account]').hide();
																	$form.find('.form-item[rel=projectId]').css('display', 'inline-block');
                                }
                              });
                            }
                          },
                          domainId: {
                            label: 'label.domain',
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
                                      items.push({id: this.id, description: this.path});
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
                                      items.push({id: this.id, description: this.path});
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
                                      items.push({id: this.id, description: this.path});
                                    });
                                  }
                                });
                              }
                              args.response.success({data: items});
                            }
                          },
                          subdomainaccess: {
                            label: 'label.subdomain.access', isBoolean: true, isHidden: true,
                          },
                          account: { label: 'label.account' },

													projectId: {
                            label: 'label.project',
                            validation: { required: true },
                            select: function(args) {
                              var items = [];
                              $.ajax({
															  url: createURL("listProjects&listAll=true"),
																dataType: "json",
																async: false,
																success: function(json) {
																  projectObjs = json.listprojectsresponse.project;
																  $(projectObjs).each(function() {
                                    items.push({id: this.id, description: this.name});
                                  });
																}
															});
                              args.response.success({data: items});
                            }
                          },

                          networkOfferingId: {
                            label: 'label.network.offering',
                            docID: 'helpGuestNetworkZoneNetworkOffering',
                            dependsOn: 'scope',
                            select: function(args) {
															$.ajax({
																url: createURL('listPhysicalNetworks'),
																data: {
																	id: args.context.physicalNetworks[0].id
																},
																async: false,
																success: function(json) {
																	args.context.physicalNetworks[0] = json.listphysicalnetworksresponse.physicalnetwork[0];
																}
															});

                              var apiCmd = "listNetworkOfferings&state=Enabled&zoneid=" + selectedZoneObj.id;
															var array1 = [];

															if(physicalNetworkObjs.length > 1) { //multiple physical networks
															  var guestTrafficTypeTotal = 0;
															  for(var i = 0; i < physicalNetworkObjs.length; i++) {
																  if(guestTrafficTypeTotal > 1) //as long as guestTrafficTypeTotal > 1, break for loop, don't need to continue to count. It doesn't matter whether guestTrafficTypeTotal is 2 or 3 or 4 or 5 or more. We only care whether guestTrafficTypeTotal is greater than 1.
																	  break;
																  $.ajax({
																	  url: createURL("listTrafficTypes&physicalnetworkid=" + physicalNetworkObjs[i].id),
																		dataType: "json",
																		async: false,
																		success: function(json) {
																			var items = json.listtraffictypesresponse.traffictype;
																			for(var k = 0; k < items.length; k++) {
																			  if(items[k].traffictype == "Guest") {
																				  guestTrafficTypeTotal++;
																				  break;
																				}
																			}
																		}
																	});
																}

															  if(guestTrafficTypeTotal > 1) {
																	if(args.context.physicalNetworks[0].tags != null && args.context.physicalNetworks[0].tags.length > 0) {
																		array1.push("&tags=" + args.context.physicalNetworks[0].tags);
																	}
																	else {
																		alert(dictionary['error.please.specify.physical.network.tags']);
																		return;
																	}
																}
															}

                              //this tab (Network tab in guest network) only shows when it's under an Advanced zone
															if(args.scope == "zone-wide" || args.scope == "domain-specific") {
																array1.push("&guestiptype=Shared");
															}

															var networkOfferingArray = [];
															
                              $.ajax({
                                url: createURL(apiCmd + array1.join("")),
                                dataType: "json",
                                async: false,
                                success: function(json) {																  
                                  networkOfferingObjs = json.listnetworkofferingsresponse.networkoffering;
                                  if (networkOfferingObjs != null && networkOfferingObjs.length > 0) {
                                    for (var i = 0; i < networkOfferingObjs.length; i++) {    
																			//for zone-wide network in Advanced SG-enabled zone, list only SG network offerings 
																			if(args.context.zones[0].networktype == 'Advanced' && args.context.zones[0].securitygroupsenabled == true) {																		
																				if(args.scope == "zone-wide") { 
																					var includingSecurityGroup = false;
																					var serviceObjArray = networkOfferingObjs[i].service;
																					for(var k = 0; k < serviceObjArray.length; k++) {																					  
																						if(serviceObjArray[k].name == "SecurityGroup") {
																							includingSecurityGroup = true;
																							break;
																						}
																					}
																					if(includingSecurityGroup == false)
																						continue; //skip to next network offering
																				}
																			}
																			
                                      networkOfferingArray.push({id: networkOfferingObjs[i].id, description: networkOfferingObjs[i].displaytext});
                                    }
                                  }
                                }
                              });

                              args.response.success({data: networkOfferingArray});


															args.$select.change(function(){
															  var $form = $(this).closest("form");
																var selectedNetworkOfferingId = $(this).val();
																$(networkOfferingObjs).each(function(){
																  if(this.id == selectedNetworkOfferingId) {																	  
																		if(this.guestiptype == "Isolated") { //*** Isolated	***															  
																			if(this.specifyipranges == false) {
																				$form.find('.form-item[rel=startipv4]').hide();
																				$form.find('.form-item[rel=endipv4]').hide();
																			}
																			else {
																				$form.find('.form-item[rel=startipv4]').css('display', 'inline-block');
																				$form.find('.form-item[rel=endipv4]').css('display', 'inline-block');
																			}																					
                                      
																		  var includingSourceNat = false;
																			var serviceObjArray = this.service;
																			for(var k = 0; k < serviceObjArray.length; k++) {
																				if(serviceObjArray[k].name == "SourceNat") {
																					includingSourceNat = true;
																					break;
																				}
																			}																			
																			if(includingSourceNat == true) { //Isolated with SourceNat
																			  cloudStack.dialog.createFormField.validation.required.remove($form.find('.form-item[rel=ip4gateway]'));	//make ip4gateway optional 	                                      							
                                        cloudStack.dialog.createFormField.validation.required.remove($form.find('.form-item[rel=ip4Netmask]'));	//make ip4Netmask optional  		
																			}
																			else { //Isolated with no SourceNat
																			  cloudStack.dialog.createFormField.validation.required.add($form.find('.form-item[rel=ip4gateway]'));	  //make ip4gateway required		
																			  cloudStack.dialog.createFormField.validation.required.add($form.find('.form-item[rel=ip4Netmask]'));	  //make ip4Netmask required
																			}																
																		}
																		else {  //*** Shared ***
																			$form.find('.form-item[rel=startipv4]').css('display', 'inline-block');
																			$form.find('.form-item[rel=endipv4]').css('display', 'inline-block');																			
																			
                                      cloudStack.dialog.createFormField.validation.required.add($form.find('.form-item[rel=ip4gateway]'));	  //make ip4gateway required		
																			cloudStack.dialog.createFormField.validation.required.add($form.find('.form-item[rel=ip4Netmask]'));	  //make ip4Netmask required			
																		}
																		
																		if(this.specifyvlan == false) {
																		  $form.find('.form-item[rel=vlanId]').hide();
																			cloudStack.dialog.createFormField.validation.required.remove($form.find('.form-item[rel=vlanId]'));	//make vlanId optional 	 	
																		}
																		else {
																		  $form.find('.form-item[rel=vlanId]').css('display', 'inline-block');																			
																			cloudStack.dialog.createFormField.validation.required.add($form.find('.form-item[rel=vlanId]'));		//make vlanId required		
																		}
																		return false; //break each loop
																	}
																});
															});
                            }
                          },

                          //IPv4 (begin)
                          ip4gateway: {
                            label: 'IPv4 Gateway',
                            docID: 'helpGuestNetworkZoneGateway'
                          },
                          ip4Netmask: {
                            label: 'IPv4 Netmask',
                            docID: 'helpGuestNetworkZoneNetmask'
                          },
                          startipv4: { 
							label: 'IPv4 Start IP', 
							validation: { required: true },
                            docID: 'helpGuestNetworkZoneStartIP'
						  },
                          endipv4: { 
							label: 'IPv4 End IP', 
							validation: { required: true },
                            docID: 'helpGuestNetworkZoneEndIP'
						  },
						//IPv4 (end)
						  
						//IPv6 (begin)
                          ip6gateway: {
                            label: 'IPv6 Gateway',
                            docID: 'helpGuestNetworkZoneGateway'
                          },
                          ip6cidr: {
                            label: 'IPv6 CIDR'
                          },
                          startipv6: { 
							label: 'IPv6 Start IP', 
							validation: { required: true },
                            docID: 'helpGuestNetworkZoneStartIP'
						  },
                          endipv6: { 
							label: 'IPv6 End IP', 
							validation: { required: true },
                            docID: 'helpGuestNetworkZoneEndIP'
						  },
						//IPv6 (end)
						  
                          networkdomain: {
                            label: 'label.network.domain',
                            docID: 'helpGuestNetworkZoneNetworkDomain'
                          }
                        }
                      },

                      action: function(args) { //Add guest network in advanced zone
                        var $form = args.$form;

												var array1 = [];
                        array1.push("&zoneId=" + selectedZoneObj.id);
												array1.push("&networkOfferingId=" + args.data.networkOfferingId);

												//Pass physical network ID to createNetwork API only when network offering's guestiptype is Shared.
												var selectedNetworkOfferingObj;
												$(networkOfferingObjs).each(function(){
													if(this.id == args.data.networkOfferingId) {
													  selectedNetworkOfferingObj = this;
														return false; //break each loop
													}
												});
												if(selectedNetworkOfferingObj.guestiptype == "Shared")
												  array1.push("&physicalnetworkid=" + selectedPhysicalNetworkObj.id);

                        array1.push("&name=" + todb(args.data.name));
                        array1.push("&displayText=" + todb(args.data.description));

											  if(($form.find('.form-item[rel=vlanId]').css("display") != "none") && (args.data.vlanId != null && args.data.vlanId.length > 0))
												  array1.push("&vlan=" + todb(args.data.vlanId));

												if($form.find('.form-item[rel=domainId]').css("display") != "none") {
												  array1.push("&domainId=" + args.data.domainId);

													if($form.find('.form-item[rel=account]').css("display") != "none") {  //account-specific
														array1.push("&account=" + args.data.account);
														array1.push("&acltype=account");
													}
													else if($form.find('.form-item[rel=projectId]').css("display") != "none") {  //project-specific
														array1.push("&projectid=" + args.data.projectId);
														array1.push("&acltype=account");														
													}
													else {  //domain-specific
														array1.push("&acltype=domain");

                            if ($form.find('.form-item[rel=subdomainaccess]:visible input:checked').size())
															array1.push("&subdomainaccess=true");
														else
															array1.push("&subdomainaccess=false");
													}
												}
												else { //zone-wide
													array1.push("&acltype=domain"); //server-side will make it Root domain (i.e. domainid=1)
												}

												//IPv4 (begin)
											    if(args.data.ip4gateway != null && args.data.ip4gateway.length > 0)
												  array1.push("&gateway=" + args.data.ip4gateway);
												if(args.data.ip4Netmask != null && args.data.ip4Netmask.length > 0)
												  array1.push("&netmask=" + args.data.ip4Netmask);
												if(($form.find('.form-item[rel=startipv4]').css("display") != "none") && (args.data.startipv4 != null && args.data.startipv4.length > 0))
												  array1.push("&startip=" + args.data.startipv4);
												if(($form.find('.form-item[rel=endipv4]').css("display") != "none") && (args.data.endipv4 != null && args.data.endipv4.length > 0))
												  array1.push("&endip=" + args.data.endipv4);
												//IPv4 (end)
												
												//IPv6 (begin)
											    if(args.data.ip6gateway != null && args.data.ip6gateway.length > 0)
												  array1.push("&gateway=" + args.data.ip6gateway);
												if(args.data.ip6cidr != null && args.data.ip6cidr.length > 0)
												  array1.push("&netmask=" + args.data.ip6cidr);
												if(($form.find('.form-item[rel=startipv6]').css("display") != "none") && (args.data.startipv6 != null && args.data.startipv6.length > 0))
												  array1.push("&startip=" + args.data.startipv6);
												if(($form.find('.form-item[rel=endipv6]').css("display") != "none") && (args.data.endipv6 != null && args.data.endipv6.length > 0))
												  array1.push("&endip=" + args.data.endipv6);
												//IPv6 (end)
												
												if(args.data.networkdomain != null && args.data.networkdomain.length > 0)
													array1.push("&networkdomain=" + todb(args.data.networkdomain));

                        $.ajax({
                          url: createURL("createNetwork" + array1.join("")),
                          dataType: "json",
                          success: function(json) {
                            var item = json.createnetworkresponse.network;
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

										//need to make 2 listNetworks API call to get all guest networks from one physical network in Advanced zone
										var items = [];
										//"listNetworks&projectid=-1": list guest networks under all projects (no matter who the owner is)
										$.ajax({
                      url: createURL("listNetworks&projectid=-1&trafficType=Guest&zoneId=" + selectedZoneObj.id + "&physicalnetworkid=" + selectedPhysicalNetworkObj.id + "&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
                      dataType: "json",
											async: false,
                      success: function(json) {
											  if(json.listnetworksresponse.network != null && json.listnetworksresponse.network.length > 0)
												  items = json.listnetworksresponse.network;
                      }
                    });

										var networkCollectionMap = {};
										$(items).each(function() {
										  networkCollectionMap[this.id] = this.name;
										});

										//"listNetworks&listAll=true: list guest networks that are not under any project (no matter who the owner is)
										$.ajax({
                      url: createURL("listNetworks&listAll=true&trafficType=Guest&zoneId=" + selectedZoneObj.id + "&physicalnetworkid=" + selectedPhysicalNetworkObj.id + "&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
                      dataType: "json",
											async: false,
                      success: function(json) {
											  $(json.listnetworksresponse.network).each(function() {
													if((this.id in networkCollectionMap) == false)
													  items.push(this);
												});
                      }
                    });

										$(items).each(function(){ 										  
											addExtraPropertiesToGuestNetworkObject(this);																			
										});

										args.response.success({data: items});
                  },

                  detailView: {
                    name: 'Guest network details',
                    noCompact: true,
                    viewAll: {
										  path: '_zone.guestIpRanges',
											label: 'label.ip.ranges',
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
                        label: 'label.edit',
                        action: function(args) {
                          var array1 = [];
                          array1.push("&name=" + todb(args.data.name));
                          array1.push("&displaytext=" + todb(args.data.displaytext));

													//args.data.networkdomain is null when networkdomain field is hidden
                          if(args.data.networkdomain != null && args.data.networkdomain != selectedGuestNetworkObj.networkdomain)
                            array1.push("&networkdomain=" + todb(args.data.networkdomain));

				                  //args.data.networkofferingid is null when networkofferingid field is hidden
													if(args.data.networkofferingid != null && args.data.networkofferingid != args.context.networks[0].networkofferingid) {
														array1.push("&networkofferingid=" + todb(args.data.networkofferingid));

														if(args.context.networks[0].type == "Isolated") { //Isolated network
															cloudStack.dialog.confirm({
																message: 'Do you want to keep the current guest network CIDR unchanged?',
																action: function() { //"Yes"	button is clicked
																	array1.push("&changecidr=false");
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
																cancelAction: function() { //"Cancel" button is clicked
																	array1.push("&changecidr=true");
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
																}
															});
															return;
														}
													}

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
												label: 'label.restart.network',
												createForm: {
													title: 'label.restart.network',
													desc: 'message.restart.network',
													preFilter: function(args) {
														if(selectedZoneObj.networktype == "Basic") {
															args.$form.find('.form-item[rel=cleanup]').find('input').removeAttr('checked'); //unchecked
															args.$form.find('.form-item[rel=cleanup]').hide(); //hidden
														}
														else {
															args.$form.find('.form-item[rel=cleanup]').find('input').attr('checked', 'checked'); //checked
															args.$form.find('.form-item[rel=cleanup]').css('display', 'inline-block'); //shown
														}
													},
													fields: {
														cleanup: {
															label: 'label.clean.up',
															isBoolean: true
														}
													}
												},
												action: function(args) {
                          var array1 = [];
                          array1.push("&cleanup=" + (args.data.cleanup == "on"));
													$.ajax({
														url: createURL("restartNetwork&cleanup=true&id=" + args.context.networks[0].id + array1.join("")),
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
													notification: function(args) {
														return 'label.restart.network';
													}
												},
												notification: {
													poll: pollAsyncJobResult
												}
											},

                      'remove': {
                        label: 'label.action.delete.network',
                        messages: {
                          confirm: function(args) {
                            return 'message.action.delete.network';
                          },
                          notification: function(args) {
                            return 'label.action.delete.network';
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
                        title: 'label.details',
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
                              label: 'label.name',
                              isEditable: true
                            }
                          },
                          {
                            id: { label: 'label.id' },
                            displaytext: {
                              label: 'label.description',
                              isEditable: true
                            },
                            type: {
                              label: 'label.type'
                            },
                            state: {
                              label: 'label.state'
                            },
                            restartrequired: {
                              label: 'label.restart.required',
                              converter: function(booleanValue) {
                                if(booleanValue == true)
                                  return "<font color='red'>Yes</font>";
                                else if(booleanValue == false)
                                  return "No";
                              }
                            },
                            vlan: { label: 'label.vlan.id' },
                            scope: { label: 'label.scope' },
                            networkofferingdisplaytext: { label: 'label.network.offering' },
                            networkofferingid: {
                              label: 'label.network.offering',
                              isEditable: true,
                              select: function(args){
                                var items = [];
                                $.ajax({
                                  url: createURL("listNetworkOfferings&state=Enabled&networkid=" + selectedGuestNetworkObj.id + "&zoneid=" + selectedGuestNetworkObj.zoneid),
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
                              label: 'label.network.offering.id'
                            },

                            gateway: { label: 'IPv4 Gateway' },
                            //netmask: { label: 'label.netmask' },
                            cidr: { label: 'IPv4 CIDR' },
                            
                            ip6gateway: { label: 'IPv6 Gateway' },
                            ip6cidr: { label: 'IPv6 CIDR' },
                            
                            networkdomaintext: {
                              label: 'label.network.domain'
                            },
                            networkdomain: {
                              label: 'label.network.domain',
                              isEditable: true
                            },

														domain: { label: 'label.domain' },
                            subdomainaccess: {
														  label: 'label.subdomain.access',
															converter: function(data) {
																return data ? 'Yes' : 'No';
															}
														},
														account: { label: 'label.account' },
														project: { label: 'label.project' }
                          }
                        ],
                        dataProvider: function(args) {	                         
													var data = {
													  id: args.context.networks[0].id
													};
													if(args.context.networks[0].projectid != null) {
													  $.extend(data, {
														  projectid: -1
														});
													}
													else {
													  $.extend(data, {
														  listAll: true   //pass "&listAll=true" to "listNetworks&id=xxxxxxxx" for now before API gets fixed.
														});
													}
												
													$.ajax({
														url: createURL("listNetworks"), 
														data: data,
														async: false,
														success: function(json) {														 
															selectedGuestNetworkObj = json.listnetworksresponse.network[0];		
                              addExtraPropertiesToGuestNetworkObject(selectedGuestNetworkObj);	
															args.response.success({
																actionFilter: cloudStack.actionFilter.guestNetwork,
																data: selectedGuestNetworkObj
															});		
														}
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
      },

      networks: {
        listView: {
          id: 'physicalNetworks',
          hideToolbar: true,
          fields: {
            name: { label: 'label.name' },
            state: {
              converter: function(str) {
                // For localization
                return str;
              },
              label: 'label.state', indicator: { 'Enabled': 'on', 'Disabled': 'off' }
            },            
						isolationmethods: { label: 'label.isolation.method' }
          },

					actions: {
						remove: {
							label: 'label.action.delete.physical.network',
							messages: {
								confirm: function(args) {
									return 'message.action.delete.physical.network';
								},
								notification: function(args) {
									return 'label.action.delete.physical.network';
								}
							},
							action: function(args) {
								$.ajax({
									url: createURL("deletePhysicalNetwork&id=" + args.context.physicalNetworks[0].id),
									dataType: "json",
									async: true,
									success: function(json) {
										var jid = json.deletephysicalnetworkresponse.jobid;
										args.response.success(
											{_custom:
											 {jobId: jid
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
					}
        },
        dataProvider: function(args) {
          $.ajax({
            url: createURL('listPhysicalNetworks'),
						data: {
              zoneid: args.context.zones[0].id
						},
            success: function(json) {
						  physicalNetworkObjs = json.listphysicalnetworksresponse.physicalnetwork;
              args.response.success({
                actionFilter: cloudStack.actionFilter.physicalNetwork,
                data: json.listphysicalnetworksresponse.physicalnetwork
              });
            }
          });
        }
      },

      trafficTypes: {
        dataProvider: function(args) {
          selectedPhysicalNetworkObj = args.context.physicalNetworks[0];

          $.ajax({
            url: createURL('listTrafficTypes'),
            data: {
              physicalnetworkid: selectedPhysicalNetworkObj.id
            },
            success: function(json) {
              args.response.success({
                data: $.map(json.listtraffictypesresponse.traffictype, function(trafficType) {
                  return {
                    id: trafficType.id,
                    name: trafficType.traffictype
                  };
                })
              });
            }
          });
        }
      },

      networkProviders: {
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
          virtualRouter: {
            id: 'virtualRouterProviders',
            label: 'label.virtual.router',
            isMaximized: true,
            type: 'detailView',
            fields: {
              name: { label: 'label.name' },
              ipaddress: { label: 'label.ip.address' },
              state: { label: 'label.status', indicator: { 'Enabled': 'on' } }
            },
            tabs: {
              network: {
                title: 'label.network',
                fields: [
                  {
                    name: { label: 'label.name' }
                  },
                  {
                    id: { label: 'label.id' },
                    state: { label: 'label.state' },
                    physicalnetworkid: { label: 'label.physical.network.ID' },
                    destinationphysicalnetworkid: { label: 'label.destination.physical.network.id' },
										supportedServices: { label: 'label.supported.services' }
                  }
                ],
                dataProvider: function(args) { 		
                  refreshNspData("VirtualRouter"); 								  
									args.response.success({
										actionFilter: virtualRouterProviderActionFilter,
										data: $.extend(nspMap["virtualRouter"], {
											supportedServices: nspMap["virtualRouter"].servicelist.join(', ')
										})
									});		
                }
              },

              instances: {
                title: 'label.instances',
                listView: {
                  label: 'label.virtual.appliances',
                  id: 'routers',
                  fields: {
                    name: { label: 'label.name' },
                    zonename: { label: 'label.zone' },
                    routerType: {
                      label: 'label.type'
                    },
                    state: {
                      converter: function(str) {
                        // For localization
                        return str;
                      },
                      label: 'label.status',
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

                    var routers = [];
                    $.ajax({
                      url: createURL("listRouters&zoneid=" + selectedZoneObj.id + "&listAll=true&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
                      data: {
											  forvpc: false
											},
                      success: function(json) {
                        var items = json.listroutersresponse.router ?
                              json.listroutersresponse.router : [];

                        $(items).map(function(index, item) {
                          routers.push(item); 
                        });

                        // Get project routers
                        $.ajax({
                          url: createURL("listRouters&zoneid=" + selectedZoneObj.id + "&listAll=true&page=" + args.page + "&pagesize=" + pageSize + array1.join("") + "&projectid=-1"),
                          data: {
														forvpc: false
													},
                          success: function(json) {
                            var items = json.listroutersresponse.router ?
                                  json.listroutersresponse.router : [];
                            
                            $(items).map(function(index, item) {
                              routers.push(item); 
                            });
                            args.response.success({
                              actionFilter: routerActionfilter,
                              data: $(routers).map(mapRouterType)
                            });
                          }
                        });
                      }
                    });
                  },
                  detailView: {
                    name: 'Virtual applicance details',
                    actions: {
                      start: {
                        label: 'label.action.start.router',
                        messages: {
                          confirm: function(args) {
                            return 'message.action.start.router';
                          },
                          notification: function(args) {
                            return 'label.action.start.router';
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
                        label: 'label.action.stop.router',
												createForm: {
													title: 'label.action.stop.router',
													desc: 'message.action.stop.router',
													fields: {
														forced: {
															label: 'force.stop',
															isBoolean: true,
															isChecked: false
														}
													}
												},
                        messages: {
                          notification: function(args) {
                            return 'label.action.stop.router';
                          }
                        },
                        action: function(args) {
												  var array1 = [];
													array1.push("&forced=" + (args.data.forced == "on"));
                          $.ajax({
                            url: createURL('stopRouter&id=' + args.context.routers[0].id + array1.join("")),
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

											'remove': {
												label: 'label.destroy.router',
												messages: {
													confirm: function(args) {
														return 'message.confirm.destroy.router';
													},
													notification: function(args) {
														return 'label.destroy.router';
													}
												},
												action: function(args) {
													$.ajax({
														url: createURL("destroyRouter&id=" + args.context.routers[0].id),
														dataType: "json",
														async: true,
														success: function(json) {
															var jid = json.destroyrouterresponse.jobid;
															args.response.success({
																_custom: {
																	jobId: jid
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
                        label: 'label.action.migrate.router',
                        createForm: {
                          title: 'label.action.migrate.router',
                          desc: '',
                          fields: {
                            hostId: {
                              label: 'label.host',
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
                                      //items.push({id: this.id, description: (this.name + ": " +(this.hasEnoughCapacity? "Available" : "Full"))}); //listHosts API no longer returns hasEnoughCapacity proprety
																			items.push({id: this.id, description: this.name});
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
												messages: {
                          notification: function(args) {
                            return 'label.action.migrate.router';
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
                        label: 'label.view.console',
                        action: {
                          externalLink: {
                            url: function(args) {
                              return clientConsoleUrl + '?cmd=access&vm=' + args.context.routers[0].id;
                            },
                            title: function(args) {
															return args.context.routers[0].id.substr(0,8);  //title in window.open() can't have space nor longer than 8 characters. Otherwise, IE browser will have error.
														},
                            width: 820,
                            height: 640
                          }
                        }
                      }
                    },
                    tabs: {
                      details: {
                        title: 'label.details',
                        preFilter: function(args) {
												  var hiddenFields = [];
                          if (!args.context.routers[0].project) {
													  hiddenFields.push('project');
														hiddenFields.push('projectid');
                          }
													if(selectedZoneObj.networktype == 'Basic') {
													  hiddenFields.push('publicip'); //In Basic zone, guest IP is public IP. So, publicip is not returned by listRouters API. Only guestipaddress is returned by listRouters API.
											    }
                          return hiddenFields;
                        },
                        fields: [
                          {
                            name: { label: 'label.name' },
                            project: { label: 'label.project' }
                          },
                          {
                            id: { label: 'label.id' },
                            projectid: { label: 'label.project.id' },
                            state: { label: 'label.state' },
                            guestnetworkid: { label: 'label.network.id' },
                            publicip: { label: 'label.public.ip' },
                            guestipaddress: { label: 'label.guest.ip' },
                            linklocalip: { label: 'label.linklocal.ip' },
                            hostname: { label: 'label.host' },
                            serviceofferingname: { label: 'label.compute.offering' },
                            networkdomain: { label: 'label.network.domain' },
                            domain: { label: 'label.domain' },
                            account: { label: 'label.account' },
                            created: { label: 'label.created', converter: cloudStack.converters.toLocalDate },
                            isredundantrouter: {
                              label: 'label.redundant.router',
                              converter: cloudStack.converters.toBooleanText
                            },
                            redundantRouterState: { label: 'label.redundant.state' }
                          }
                        ],
                        dataProvider: function(args) {												  
													$.ajax({
														url: createURL("listRouters&id=" + args.context.routers[0].id),
														dataType: 'json',
														async: true,
														success: function(json) {
															var jsonObj = json.listroutersresponse.router[0];													
															addExtraPropertiesToRouterInstanceObject(jsonObj);															
															args.response.success({
																actionFilter: routerActionfilter,
																data: jsonObj
															});
														}
													});		
                        }
                      },
                      nics: {
                        title: 'label.nics',
                        multiple: true,
                        fields: [
                          {
                            name: { label: 'label.name', header: true },
                            type: { label: 'label.type' },
                            traffictype: { label: 'label.traffic.type' },
                            networkname: { label: 'label.network.name' },
                            netmask: { label: 'label.netmask' },
                            ipaddress: { label: 'label.ip.address' },
                            id: { label: 'label.id' },
                            networkid: { label: 'label.network.id' },
                            isolationuri: { label: 'label.isolation.uri' },
                            broadcasturi: { label: 'label.broadcast.uri' }
                          }
                        ],
                        dataProvider: function(args) {
                          $.ajax({
                            url: createURL("listRouters&id=" + args.context.routers[0].id),
                            dataType: 'json',
                            async: true,
                            success: function(json) {
                              var jsonObj = json.listroutersresponse.router[0].nic;

                              args.response.success({
                                actionFilter: routerActionfilter,
                                data: $.map(jsonObj, function(nic, index) {
                                  var name = 'NIC ' + (index + 1);                    
                                  if (nic.isdefault) {
                                    name += ' (' + _l('label.default') + ')';
                                  }
                                  return $.extend(nic, {
                                    name: name
                                  });
                                })
                              });
                            }
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
                label: 'label.enable.provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["virtualRouter"].id + "&state=Enabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
											args.response.success(
                        {_custom:
                          {
                            jobId: jid,
														getUpdatedItem: function(json) {
															$(window).trigger('cloudStack.fullRefresh');
														}
                          }
                        }
                      );
                    }
                  });
                },
                messages: {
								  confirm: function(args) {
									  return 'message.confirm.enable.provider';
									},
                  notification: function() {
									  return 'label.enable.provider';
									}
                },
                notification: { poll: pollAsyncJobResult }
              },
              disable: {
                label: 'label.disable.provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["virtualRouter"].id + "&state=Disabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
											args.response.success(
                        {_custom:
                          {
                            jobId: jid,
														getUpdatedItem: function(json) {
															$(window).trigger('cloudStack.fullRefresh');
														}
                          }
                        }
                      );
                    }
                  });
                },
                messages: {
								  confirm: function(args) {
									  return 'message.confirm.disable.provider';
									},
                  notification: function() {
									  return 'label.disable.provider';
									}
                },
                notification: { poll: pollAsyncJobResult }
              }
            }
          },
			
					vpcVirtualRouter: {
            id: 'vpcVirtualRouterProviders',
            label: 'VPC Virtual Router',
            isMaximized: true,
            type: 'detailView',
            fields: {
              name: { label: 'label.name' },
              ipaddress: { label: 'label.ip.address' },
              state: { label: 'label.status', indicator: { 'Enabled': 'on' } }
            },
            tabs: {
              network: {
                title: 'label.network',
                fields: [
                  {
                    name: { label: 'label.name' }
                  },
                  {
                    id: { label: 'label.id' },
                    state: { label: 'label.state' },
                    physicalnetworkid: { label: 'label.physical.network.ID' },
                    destinationphysicalnetworkid: { label: 'label.destination.physical.network.id' },
										supportedServices: { label: 'label.supported.services' }
                  }
                ],
                dataProvider: function(args) { 			
									refreshNspData("VpcVirtualRouter"); 								  
									args.response.success({
										actionFilter: virtualRouterProviderActionFilter,
										data: $.extend(nspMap["vpcVirtualRouter"], {
											supportedServices: nspMap["vpcVirtualRouter"].servicelist.join(', ')
										})
									});											
                }
              },

              instances: {
                title: 'label.instances',
                listView: {
                  label: 'label.virtual.appliances',
                  id: 'routers',
                  fields: {
                    name: { label: 'label.name' },
                    zonename: { label: 'label.zone' },
										routerType: {
											label: 'label.type'
										},
                    state: {
                      converter: function(str) {
                        // For localization
                        return str;
                      },
                      label: 'label.status',
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

										var routers = [];
                    $.ajax({
                      url: createURL("listRouters&zoneid=" + selectedZoneObj.id + "&listAll=true&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
                      dataType: 'json',
											data: {
											  forvpc: true
											},
                      async: true,
                      success: function(json) {
                        var items = json.listroutersresponse.router;												
												$(items).map(function(index, item) {
													routers.push(item); 
												});
												
												// Get project routers
												$.ajax({
													url: createURL("listRouters&zoneid=" + selectedZoneObj.id + "&listAll=true&page=" + args.page + "&pagesize=" + pageSize + array1.join("") + "&projectid=-1"),
													dataType: 'json',
													data: {
														forvpc: true
													},
													async: true,
													success: function(json) {
														var items = json.listroutersresponse.router;														
														$(items).map(function(index, item) {
															routers.push(item); 
														});														
														args.response.success({
															actionFilter: routerActionfilter,
															data: $(routers).map(mapRouterType)
														});
													}
												});												                    
                      }
                    });
                  },
                  detailView: {
                    name: 'Virtual applicance details',
                    actions: {
                      start: {
                        label: 'label.action.start.router',
                        messages: {
                          confirm: function(args) {
                            return 'message.action.start.router';
                          },
                          notification: function(args) {
                            return 'label.action.start.router';
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
                        label: 'label.action.stop.router',
												createForm: {
													title: 'label.action.stop.router',
													desc: 'message.action.stop.router',
													fields: {
														forced: {
															label: 'force.stop',
															isBoolean: true,
															isChecked: false
														}
													}
												},
                        messages: {
                          notification: function(args) {
                            return 'label.action.stop.router';
                          }
                        },
                        action: function(args) {
												  var array1 = [];
													array1.push("&forced=" + (args.data.forced == "on"));
                          $.ajax({
                            url: createURL('stopRouter&id=' + args.context.routers[0].id + array1.join("")),
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
											
											restart: {
												label: 'label.action.reboot.router',
												messages: {
													confirm: function(args) {
														return 'message.action.reboot.router';
													},
													notification: function(args) {
														return 'label.action.reboot.router';
													}
												},
												action: function(args) {
													$.ajax({
														url: createURL('rebootRouter&id=' + args.context.routers[0].id),
														dataType: 'json',
														async: true,
														success: function(json) {
															var jid = json.rebootrouterresponse.jobid;
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
												label: 'label.change.service.offering',
												createForm: {
													title: 'label.change.service.offering',
													desc: '',
													fields: {
														serviceOfferingId: {
															label: 'label.compute.offering',
															select: function(args) {																															
																$.ajax({
																	url: createURL('listServiceOfferings'),
																	data: {
																	  issystem: true,
																	  systemvmtype: 'domainrouter'
																	},
																	success: function(json) {																	 
																		var serviceofferings = json.listserviceofferingsresponse.serviceoffering;
																		var items = [];
																		$(serviceofferings).each(function() {		
																			if(this.id != args.context.routers[0].serviceofferingid) {
																				items.push({id: this.id, description: this.name});  //default one (i.e. "System Offering For Software Router") doesn't have displaytext property. So, got to use name property instead.
																			}
																		});																	
																		args.response.success({data: items});
																	}
																});
															}
														}
													}
												},
												messages: {
													notification: function(args) {
														return 'label.change.service.offering';
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
											
											'remove': {
												label: 'label.destroy.router',
												messages: {
													confirm: function(args) {
														return 'message.confirm.destroy.router';
													},
													notification: function(args) {
														return 'label.destroy.router';
													}
												},
												action: function(args) {
													$.ajax({
														url: createURL("destroyRouter&id=" + args.context.routers[0].id),
														dataType: "json",
														async: true,
														success: function(json) {
															var jid = json.destroyrouterresponse.jobid;
															args.response.success({
																_custom: {
																	jobId: jid
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
                        label: 'label.action.migrate.router',
                        createForm: {
                          title: 'label.action.migrate.router',
                          desc: '',
                          fields: {
                            hostId: {
                              label: 'label.host',
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
                                      //items.push({id: this.id, description: (this.name + ": " +(this.hasEnoughCapacity? "Available" : "Full"))}); //listHosts API no longer returns hasEnoughCapacity proprety
																			items.push({id: this.id, description: this.name});
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
												messages: {
                          notification: function(args) {
                            return 'label.action.migrate.router';
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
                        label: 'label.view.console',
                        action: {
                          externalLink: {
                            url: function(args) {
                              return clientConsoleUrl + '?cmd=access&vm=' + args.context.routers[0].id;
                            },
                            title: function(args) {
															return args.context.routers[0].id.substr(0,8);  //title in window.open() can't have space nor longer than 8 characters. Otherwise, IE browser will have error.
														},
                            width: 820,
                            height: 640
                          }
                        }
                      }
                    },
                    tabs: {
                      details: {
                        title: 'label.details',
                        preFilter: function(args) {
												  var hiddenFields = [];
                          if (!args.context.routers[0].project) {
													  hiddenFields.push('project');
														hiddenFields.push('projectid');
                          }
													if(selectedZoneObj.networktype == 'Basic') {
													  hiddenFields.push('publicip'); //In Basic zone, guest IP is public IP. So, publicip is not returned by listRouters API. Only guestipaddress is returned by listRouters API.
											    }
                          return hiddenFields;
                        },
                        fields: [
                          {
                            name: { label: 'label.name' },
                            project: { label: 'label.project' }
                          },
                          {
                            id: { label: 'label.id' },														
                            projectid: { label: 'label.project.id' },
                            state: { label: 'label.state' },
                            publicip: { label: 'label.public.ip' },
                            guestipaddress: { label: 'label.guest.ip' },
                            linklocalip: { label: 'label.linklocal.ip' },
                            hostname: { label: 'label.host' },
                            serviceofferingname: { label: 'label.compute.offering' },
                            networkdomain: { label: 'label.network.domain' },
                            domain: { label: 'label.domain' },
                            account: { label: 'label.account' },
                            created: { label: 'label.created', converter: cloudStack.converters.toLocalDate },
                            isredundantrouter: {
                              label: 'label.redundant.router',
                              converter: cloudStack.converters.toBooleanText
                            },
                            redundantRouterState: { label: 'label.redundant.state' },
														vpcid: { label: 'VPC ID' }
                          }
                        ],
                        dataProvider: function(args) {												  
													$.ajax({
														url: createURL("listRouters&id=" + args.context.routers[0].id),
														dataType: 'json',
														async: true,
														success: function(json) {
															var jsonObj = json.listroutersresponse.router[0];													
															addExtraPropertiesToRouterInstanceObject(jsonObj);															
															args.response.success({
																actionFilter: routerActionfilter,
																data: jsonObj
															});
														}
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
                label: 'label.enable.provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["vpcVirtualRouter"].id + "&state=Enabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
											args.response.success(
                        {_custom:
                          {
                            jobId: jid,
														getUpdatedItem: function(json) {
															$(window).trigger('cloudStack.fullRefresh');
														}
                          }
                        }
                      );
                    }
                  });
                },
                messages: {
								  confirm: function(args) {
									  return 'message.confirm.enable.provider';
									},
                  notification: function() {
									  return 'label.enable.provider';
									}
                },
                notification: { poll: pollAsyncJobResult }
              },
              disable: {
                label: 'label.disable.provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["vpcVirtualRouter"].id + "&state=Disabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
											args.response.success(
                        {_custom:
                          {
                            jobId: jid,
														getUpdatedItem: function(json) {
															$(window).trigger('cloudStack.fullRefresh');
														}
                          }
                        }
                      );
                    }
                  });
                },
                messages: {
								  confirm: function(args) {
									  return 'message.confirm.disable.provider';
									},
                  notification: function() {
									  return 'label.disable.provider';
									}
                },
                notification: { poll: pollAsyncJobResult }
              }
            }
          },
										
          // NetScaler provider detail view
          netscaler: {
            type: 'detailView',
            id: 'netscalerProvider',
            label: 'label.netScaler',
            viewAll: { label: 'label.devices', path: '_zone.netscalerDevices' },
            tabs: {
              details: {
                title: 'label.details',
                fields: [
                  {
                    name: { label: 'label.name' }
                  },
                  {
									  state: { label: 'label.state' }
                  }
                ],
                dataProvider: function(args) {
								  refreshNspData("Netscaler");
									var providerObj;
									$(nspHardcodingArray).each(function(){
										if(this.id == "netscaler") {
											providerObj = this;
											return false; //break each loop
										}
									});
                  args.response.success({
                    data: providerObj,
                    actionFilter: networkProviderActionFilter('netscaler')
                  });
                }
              }
            },
            actions: {
              add: {
                label: 'label.add.netScaler.device',
                createForm: {
                  title: 'label.add.netScaler.device',									
									preFilter: cloudStack.preFilter.addLoadBalancerDevice,	
                  fields: {
                    ip: {
                      label: 'label.ip.address',
                      docID: 'helpNetScalerIPAddress'
                    },
                    username: {
                      label: 'label.username',
                      docID: 'helpNetScalerUsername'
                    },
                    password: {
                      label: 'label.password',
                      isPassword: true,
                      docID: 'helpNetScalerPassword'
                    },
                    networkdevicetype: {
                      label: 'label.type',
                      docID: 'helpNetScalerType',
                      select: function(args) {
                        var items = [];
                        items.push({id: "NetscalerMPXLoadBalancer", description: "NetScaler MPX LoadBalancer"});
                        items.push({id: "NetscalerVPXLoadBalancer", description: "NetScaler VPX LoadBalancer"});
                        items.push({id: "NetscalerSDXLoadBalancer", description: "NetScaler SDX LoadBalancer"});
                        args.response.success({data: items});
                      }
                    },
                    publicinterface: {
                      label: 'label.public.interface',
                      docID: 'helpNetScalerPublicInterface'
                    },
                    privateinterface: {
                      label: 'label.private.interface',
                      docID: 'helpNetScalerPrivateInterface'
                    },
                    numretries: {
                      label: 'label.numretries',
                      defaultValue: '2',
                      docID: 'helpNetScalerRetries'
                    },
                    // inline: {
                    //   label: 'Mode',
                    //   select: function(args) {
                    //     var items = [];
                    //     items.push({id: "false", description: "side by side"});
                    //     items.push({id: "true", description: "inline"});
                    //     args.response.success({data: items});
                    //   }
                    // },                    
                    dedicated: {
                      label: 'label.dedicated',
                      isBoolean: true,
                      isChecked: false,
                      docID: 'helpNetScalerDedicated'
                    },
										capacity: {
                      label: 'label.capacity',											
                      validation: { required: false, number: true },
                      docID: 'helpNetScalerCapacity'
                    }
                  }
                },
								messages: {
                  notification: function(args) {
                    return 'label.add.netScaler.device';
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
												var addNetscalerProviderIntervalID = setInterval(function() { 	
                          $.ajax({
                            url: createURL("queryAsyncJobResult&jobId="+jobId),
                            dataType: "json",
                            success: function(json) {
                              var result = json.queryasyncjobresultresponse;
                              if (result.jobstatus == 0) {
                                return; //Job has not completed
                              }
                              else {
                                clearInterval(addNetscalerProviderIntervalID); 
                                if (result.jobstatus == 1) {
                                  nspMap["netscaler"] = result.jobresult.networkserviceprovider;
                                  addExternalLoadBalancer(args, selectedPhysicalNetworkObj, "addNetscalerLoadBalancer", "addnetscalerloadbalancerresponse", "netscalerloadbalancer");
                                }
                                else if (result.jobstatus == 2) {
                                  alert("addNetworkServiceProvider&name=Netscaler failed. Error: " + _s(result.jobresult.errortext));
                                }
                              }
                            },
                            error: function(XMLHttpResponse) {
                              var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                              alert("addNetworkServiceProvider&name=Netscaler failed. Error: " + errorMsg);
                            }
                          });
                        }, g_queryAsyncJobResultInterval); 		
                      }
                    });
                  }
                  else {
                    addExternalLoadBalancer(args, selectedPhysicalNetworkObj, "addNetscalerLoadBalancer", "addnetscalerloadbalancerresponse", "netscalerloadbalancer");
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },
              enable: {
                label: 'label.enable.provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["netscaler"].id + "&state=Enabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid,
														getUpdatedItem: function(json) {
															$(window).trigger('cloudStack.fullRefresh');
														}
                          }
                        }
                      );
                    }
                  });
                },
                messages: {
								  confirm: function(args) {
									  return 'message.confirm.enable.provider';
									},
                  notification: function() {
									  return 'label.enable.provider';
								  }
                },
                notification: { poll: pollAsyncJobResult }
              },
              disable: {
                label: 'label.disable.provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["netscaler"].id + "&state=Disabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid,
														getUpdatedItem: function(json) {
															$(window).trigger('cloudStack.fullRefresh');
														}
                          }
                        }
                      );
                    }
                  });
                },
                messages: {
								  confirm: function(args) {
									  return 'message.confirm.disable.provider';
									},
                  notification: function() {
									  return 'label.disable.provider';
									}
                },
                notification: { poll: pollAsyncJobResult }
              },
              destroy: {
                label: 'label.shutdown.provider',
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
								  confirm: function(args) {
									  return 'message.confirm.shutdown.provider';
									},
                  notification: function(args) {
									  return 'label.shutdown.provider';
									}
                },
                notification: { poll: pollAsyncJobResult }
              }
            }
          },

		      //f5 provider detail view
          f5: {
            type: 'detailView',
            id: 'f5Provider',
            label: 'label.f5',
            viewAll: { label: 'label.devices', path: '_zone.f5Devices' },
            tabs: {
              details: {
                title: 'label.details',
                fields: [
                  {
                    name: { label: 'label.name' }
                  },
                  {
									  state: { label: 'label.state' }
                  }
                ],
                dataProvider: function(args) {
								  refreshNspData("F5BigIp");
									var providerObj;
									$(nspHardcodingArray).each(function(){
										if(this.id == "f5") {
											providerObj = this;
											return false; //break each loop
										}
									});
                  args.response.success({
                    data: providerObj,
                    actionFilter: networkProviderActionFilter('f5')
                  });
                }
              }
            },
            actions: {
              add: {
                label: 'label.add.F5.device',
                createForm: {
                  title: 'label.add.F5.device',
									preFilter: cloudStack.preFilter.addLoadBalancerDevice,	
                  fields: {
                    ip: {
                      label: 'label.ip.address',
                      docID: 'helpF5IPAddress'
                    },
                    username: {
                      label: 'label.username',
                      docID: 'helpF5Username'
                    },
                    password: {
                      label: 'label.password',
                      docID: 'helpF5Password',
                      isPassword: true
                    },
                    networkdevicetype: {
                      label: 'label.type',
                      docID: 'helpF5Type',
                      select: function(args) {
                        var items = [];
                        items.push({id: "F5BigIpLoadBalancer", description: "F5 Big Ip Load Balancer"});
                        args.response.success({data: items});
                      }
                    },
                    publicinterface: {
                      label: 'label.public.interface',
                      docID: 'helpF5PublicInterface'
                    },
                    privateinterface: {
                      label: 'label.private.interface',
                      docID: 'helpF5PrivateInterface'
                    },
                    numretries: {
                      label: 'label.numretries',
                      docID: 'helpF5Retries',
                      defaultValue: '2'
                    },
										//Inline Mode has been moved from Add F5 Device to Create Network Offering (both backend and UI)
										/*
                    inline: {
                      label: 'Mode',
                      docID: 'helpF5Mode',
                      select: function(args) {
                        var items = [];
                        items.push({id: "false", description: "side by side"});
                        items.push({id: "true", description: "inline"});
                        args.response.success({data: items});
                      }
                    },    
                    */										
                    dedicated: {
                      label: 'label.dedicated',
                      docID: 'helpF5Dedicated',
                      isBoolean: true,
                      isChecked: false
                    },
										capacity: {
                      label: 'label.capacity',
                      docID: 'helpF5Capacity',
                      validation: { required: false, number: true }
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
												var addF5ProviderIntervalID = setInterval(function() { 	
                          $.ajax({
                            url: createURL("queryAsyncJobResult&jobId="+jobId),
                            dataType: "json",
                            success: function(json) {
                              var result = json.queryasyncjobresultresponse;
                              if (result.jobstatus == 0) {
                                return; //Job has not completed
                              }
                              else {
                                clearInterval(addF5ProviderIntervalID); 
                                if (result.jobstatus == 1) {
                                  nspMap["f5"] = json.queryasyncjobresultresponse.jobresult.networkserviceprovider;
                                  addExternalLoadBalancer(args, selectedPhysicalNetworkObj, "addF5LoadBalancer", "addf5bigiploadbalancerresponse");
                                }
                                else if (result.jobstatus == 2) {
                                  alert("addNetworkServiceProvider&name=F5BigIp failed. Error: " + _s(result.jobresult.errortext));
                                }
                              }
                            },
                            error: function(XMLHttpResponse) {
                              var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                              alert("addNetworkServiceProvider&name=F5BigIpfailed. Error: " + errorMsg);
                            }
                          });
                        }, g_queryAsyncJobResultInterval); 		
                      }
                    });
                  }
                  else {
                    addExternalLoadBalancer(args, selectedPhysicalNetworkObj, "addF5LoadBalancer", "addf5bigiploadbalancerresponse");
                  }
                },
                messages: {
                  notification: function(args) {
                    return 'label.add.F5.device';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },
              enable: {
                label: 'label.enable.provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["f5"].id + "&state=Enabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid,
														getUpdatedItem: function(json) {
															$(window).trigger('cloudStack.fullRefresh');
														}
                          }
                        }
                      );
                    }
                  });
                },
                messages: {
								  confirm: function(args) {
									  return 'message.confirm.enable.provider';
									},
                  notification: function() {
									  return 'label.enable.provider';
								  }
                },
                notification: { poll: pollAsyncJobResult }
              },
              disable: {
                label: 'label.disable.provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["f5"].id + "&state=Disabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid,
														getUpdatedItem: function(json) {
															$(window).trigger('cloudStack.fullRefresh');
														}
                          }
                        }
                      );
                    }
                  });
                },
                messages: {
								  confirm: function(args) {
									  return 'message.confirm.disable.provider';
									},
                  notification: function() {
									  return 'label.disable.provider';
									}
                },
                notification: { poll: pollAsyncJobResult }
              },
              destroy: {
                label: 'label.shutdown.provider',
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
								  confirm: function(args) {
									  return 'message.confirm.shutdown.provider';
									},
                  notification: function(args) {
									  return 'label.shutdown.provider';
									}
                },
                notification: { poll: pollAsyncJobResult }
              }
            }
          },

          // SRX provider detailView
          srx: {
            type: 'detailView',
            id: 'srxProvider',
            label: 'label.srx',
            viewAll: { label: 'label.devices', path: '_zone.srxDevices' },
            tabs: {
              details: {
                title: 'label.details',
                fields: [
                  {
                    name: { label: 'label.name' }
                  },
                  {
									  state: { label: 'label.state' }
                  }
                ],
                dataProvider: function(args) {            
								  refreshNspData("JuniperSRX");
									var providerObj;
									$(nspHardcodingArray).each(function(){
										if(this.id == "srx") {
											providerObj = this;
											return false; //break each loop
										}
									});
                  args.response.success({
                    data: providerObj,
                    actionFilter: networkProviderActionFilter('srx')
                  });
                }
              }
            },
            actions: {
              add: {
                label: 'label.add.SRX.device',
                createForm: {
                  title: 'label.add.SRX.device',
                  fields: {
                    ip: {
                      label: 'label.ip.address',
                      docID: 'helpSRXIPAddress'
                    },
                    username: {
                      label: 'label.username',
                      docID: 'helpSRXUsername'
                    },
                    password: {
                      label: 'label.password',
                      isPassword: true,
                      docID: 'helpSRXPassword'
                    },
                    networkdevicetype: {
                      label: 'label.type',
                      docID: 'helpSRXType',
                      select: function(args) {
                        var items = [];
                        items.push({id: "JuniperSRXFirewall", description: "Juniper SRX Firewall"});
                        args.response.success({data: items});
                      }
                    },
                    publicinterface: {
                      label: 'label.public.interface',
                      docID: 'helpSRXPublicInterface'
                    },
                    privateinterface: {
                      label: 'label.private.interface',
                      docID: 'helpSRXPrivateInterface'
                    },
                    usageinterface: {
                      label: 'Usage interface',
                      docID: 'helpSRXUsageInterface'
                    },
                    numretries: {
                      label: 'label.numretries',
                      defaultValue: '2',
                      docID: 'helpSRXRetries'
                    },
                    timeout: {
                      label: 'label.timeout',
                      defaultValue: '300',
                      docID: 'helpSRXTimeout'
                    },
                    // inline: {
                    //   label: 'Mode',
                    //   docID: 'helpSRXMode',
                    //   select: function(args) {
                    //     var items = [];
                    //     items.push({id: "false", description: "side by side"});
                    //     items.push({id: "true", description: "inline"});
                    //     args.response.success({data: items});
                    //   }
                    // },
                    publicnetwork: {
                      label: 'label.public.network',
                      defaultValue: 'untrusted',
                      docID: 'helpSRXPublicNetwork',
                      isDisabled:true
                    },
                    privatenetwork: {
                      label: 'label.private.network',
                      defaultValue: 'trusted',
                      docID: 'helpSRXPrivateNetwork',
                      isDisabled:true
                    },
                    capacity: {
                      label: 'label.capacity',
                      validation: { required: false, number: true },
                      docID: 'helpSRXCapacity'
                    },
                    dedicated: {
                      label: 'label.dedicated',
                      isBoolean: true,
                      isChecked: false,
                      docID: 'helpSRXDedicated'
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
                        var addJuniperSRXProviderIntervalID = setInterval(function() { 	
                          $.ajax({
                            url: createURL("queryAsyncJobResult&jobId="+jobId),
                            dataType: "json",
                            success: function(json) {
                              var result = json.queryasyncjobresultresponse;
                              if (result.jobstatus == 0) {
                                return; //Job has not completed
                              }
                              else {
                                clearInterval(addJuniperSRXProviderIntervalID); 
                                if (result.jobstatus == 1) {
                                  nspMap["srx"] = json.queryasyncjobresultresponse.jobresult.networkserviceprovider;
                                  addExternalFirewall(args, selectedPhysicalNetworkObj, "addSrxFirewall", "addsrxfirewallresponse", "srxfirewall");
                                }
                                else if (result.jobstatus == 2) {
                                  alert("addNetworkServiceProvider&name=JuniperSRX failed. Error: " + _s(result.jobresult.errortext));
                                }
                              }
                            },
                            error: function(XMLHttpResponse) {
                              var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                              alert("addNetworkServiceProvider&name=JuniperSRX failed. Error: " + errorMsg);
                            }
                          });
                        }, g_queryAsyncJobResultInterval); 		
                      }
                    });
                  }
                  else {
                    addExternalFirewall(args, selectedPhysicalNetworkObj, "addSrxFirewall", "addsrxfirewallresponse", "srxfirewall");
                  }
                },
                messages: {
                  notification: function(args) {
                    return 'label.add.SRX.device';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },
              enable: {
                label: 'label.enable.provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["srx"].id + "&state=Enabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid,
														getUpdatedItem: function(json) {
															$(window).trigger('cloudStack.fullRefresh');
														}
                          }
                        }
                      );
                    }
                  });
                },
                messages: {
								  confirm: function(args) {
									  return 'message.confirm.enable.provider';
									},
                  notification: function() {
									  return 'label.enable.provider';
									}
                },
                notification: { poll: pollAsyncJobResult }
              },
              disable: {
                label: 'label.disable.provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["srx"].id + "&state=Disabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid,
														getUpdatedItem: function(json) {
															$(window).trigger('cloudStack.fullRefresh');
														}
                          }
                        }
                      );
                    }
                  });
                },
                messages: {
								  confirm: function(args) {
									  return 'message.confirm.disable.provider';
									},
                  notification: function() {
									  return 'label.disable.provider';
									}
                },
                notification: { poll: pollAsyncJobResult }
              },
              destroy: {
                label: 'label.shutdown.provider',
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
								  confirm: function(args) {
									  return 'message.confirm.shutdown.provider';
									},
                  notification: function(args) {
									  return 'label.shutdown.provider';
									}
                },
                notification: { poll: pollAsyncJobResult }
              }
            }
          },

          // Security groups detail view
          securityGroups: {
            id: 'securityGroup-providers',
            label: 'Security Groups',
            type: 'detailView',
            viewAll: { label: 'label.rules', path: 'network.securityGroups' },
            tabs: {
              details: {
                title: 'label.details',
                fields: [
                  {
                    name: { label: 'label.name' }
                  },
                  {
                    state: { label: 'label.state' }
                  }
                ],
                dataProvider: function(args) {
								  refreshNspData("SecurityGroupProvider");
									var providerObj;
									$(nspHardcodingArray).each(function(){
										if(this.id == "securityGroups") {
											providerObj = this;
											return false; //break each loop
										}
									});
                  args.response.success({
                    actionFilter: function(args) {
                      var allowedActions = [];
                      var jsonObj = providerObj;
                      if(jsonObj.state == "Enabled")
                        allowedActions.push("disable");
                      else if(jsonObj.state == "Disabled")
                        allowedActions.push("enable");
                      return allowedActions;
                    },
                    data: providerObj
                  });
                }
              }
            },
            actions: {
              enable: {
                label: 'label.enable.provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["securityGroups"].id + "&state=Enabled"),
                    async: true,
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid,
														getUpdatedItem: function(json) {
															$(window).trigger('cloudStack.fullRefresh');
														}
                          }
                        }
                      );
                    }
                  });
                },
                messages: {
                  confirm: function(args) {
									  return 'message.confirm.enable.provider';
									},
                  notification: function() {
									  return 'label.enable.provider';
									}
                },
                notification: { poll: pollAsyncJobResult }
              },
              disable: {
                label: 'label.disable.provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["securityGroups"].id + "&state=Disabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid,
														getUpdatedItem: function(json) {
															$(window).trigger('cloudStack.fullRefresh');
														}
                          }
                        }
                      );
                    }
                  });
                },
                messages: {
								  confirm: function(args) {
									  return 'message.confirm.disable.provider';
									},
                  notification: function() {
									  return 'label.disable.provider';
									}
                },
                notification: { poll: pollAsyncJobResult }
              }
            },

            fields: {
              id: { label: 'label.id' },
              name: { label: 'label.name' }//,
              //state: { label: 'label.status' } //comment it for now, since dataProvider below doesn't get called by widget code after action is done
            }
          },
          // Nicira Nvp provider detail view
          niciraNvp: {
            type: 'detailView',
            id: 'niciraNvpProvider',
            label: 'label.niciraNvp',
            viewAll: { label: 'label.devices', path: '_zone.niciraNvpDevices' },
            tabs: {
              details: {
                title: 'label.details',
                fields: [
                  {
                    name: { label: 'label.name' }
                  },
                  {
                    state: { label: 'label.state' }
                  }
                ],
                dataProvider: function(args) {
                                  refreshNspData("NiciraNvp");
                                    var providerObj;
                                    $(nspHardcodingArray).each(function(){
                                        if(this.id == "niciraNvp") {
                                            providerObj = this;
                                            return false; //break each loop
                                        }
                                    });
                  args.response.success({
                    data: providerObj,
                    actionFilter: networkProviderActionFilter('niciraNvp')
                  });
                }
              }
            },
            actions: {
              add: {
                label: 'label.add.NiciraNvp.device',
                createForm: {
                  title: 'label.add.NiciraNvp.device',
                  preFilter: function(args) {  },   // TODO What is this?  
                  fields: {
                    host: {
                      label: 'label.ip.address'
                    },
                    username: {
                      label: 'label.username'
                    },
                    password: {
                      label: 'label.password',
                      isPassword: true
                    },
                    numretries: {
                      label: 'label.numretries',
                      defaultValue: '2'
                    },
                    transportzoneuuid: {
                      label: 'label.nicira.transportzoneuuid'
                    },
                    l3gatewayserviceuuid: {
                      label: 'label.nicira.l3gatewayserviceuuid'
                    }
                  }
                },
                action: function(args) {
                  if(nspMap["niciraNvp"] == null) {
                    $.ajax({
                      url: createURL("addNetworkServiceProvider&name=NiciraNvp&physicalnetworkid=" + selectedPhysicalNetworkObj.id),
                      dataType: "json",
                      async: true,
                      success: function(json) {
                        var jobId = json.addnetworkserviceproviderresponse.jobid;                        
                        var addNiciraNvpProviderIntervalID = setInterval(function() {  
                          $.ajax({
                            url: createURL("queryAsyncJobResult&jobId="+jobId),
                            dataType: "json",
                            success: function(json) {
                              var result = json.queryasyncjobresultresponse;
                              if (result.jobstatus == 0) {
                                return; //Job has not completed
                              }
                              else {
                                clearInterval(addNiciraNvpProviderIntervalID); 
                                if (result.jobstatus == 1) {
                                  nspMap["niciraNvp"] = json.queryasyncjobresultresponse.jobresult.networkserviceprovider;
                                  addNiciraNvpDevice(args, selectedPhysicalNetworkObj, "addNiciraNvpDevice", "addniciranvpdeviceresponse", "niciranvpdevice")
                                }
                                else if (result.jobstatus == 2) {
                                  alert("addNetworkServiceProvider&name=NiciraNvp failed. Error: " + _s(result.jobresult.errortext));
                                }
                              }
                            },
                            error: function(XMLHttpResponse) {
                              var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                              alert("addNetworkServiceProvider&name=NiciraNvp failed. Error: " + errorMsg);
                            }
                          });
                        }, g_queryAsyncJobResultInterval);       
                      }
                    });
                  }
                  else {
                      addNiciraNvpDevice(args, selectedPhysicalNetworkObj, "addNiciraNvpDevice", "addniciranvpdeviceresponse", "niciranvpdevice")
                  }
                },
                messages: {
                  notification: function(args) {
                    return 'label.add.NiciraNvp.device';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },
              enable: {
                label: 'label.enable.provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["niciraNvp"].id + "&state=Enabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid,
                                                        getUpdatedItem: function(json) {
                                                            $(window).trigger('cloudStack.fullRefresh');
                                                        }
                          }
                        }
                      );
                    }
                  });
                },
                messages: {
                                  confirm: function(args) {
                                      return 'message.confirm.enable.provider';
                                    },
                  notification: function() {
                                      return 'label.enable.provider';
                                  }
                },
                notification: { poll: pollAsyncJobResult }
              },
              disable: {
                label: 'label.disable.provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkServiceProvider&id=" + nspMap["niciraNvp"].id + "&state=Disabled"),
                    dataType: "json",
                    success: function(json) {
                      var jid = json.updatenetworkserviceproviderresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
                            jobId: jid,
                                   getUpdatedItem: function(json) {
                                     $(window).trigger('cloudStack.fullRefresh');
                                   }
                          }
                        }
                      );
                    }
                  });
                },
                messages: {
                                  confirm: function(args) {
                                      return 'message.confirm.disable.provider';
                                    },
                  notification: function() {
                                      return 'label.disable.provider';
                                    }
                },
                notification: { poll: pollAsyncJobResult }
              },
              destroy: {
                label: 'label.shutdown.provider',
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteNetworkServiceProvider&id=" + nspMap["niciraNvp"].id),
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
                                  confirm: function(args) {
                                      return 'message.confirm.shutdown.provider';
                                    },
                  notification: function(args) {
                                      return 'label.shutdown.provider';
                                    }
                },
                notification: { poll: pollAsyncJobResult }
              }
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
            zones: {
              id: 'physicalResources',
              label: 'label.menu.physical.resources',
              fields: {
                name: { label: 'label.zone' },
                networktype: { label: 'label.network.type' },
                domainid: {
                  label: 'label.public',
                  converter: function(args) {
                    if(args == null)
                      return "Yes";
                    else
                      return "No";
                  }
                },
                allocationstate: {
                  label: 'label.allocation.state',
                  converter: function(str) {
                    // For localization
                    return str;
                  },
                  indicator: {
                    'Enabled': 'on',
                    'Disabled': 'off'
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

              actions: {
                add: {
                  label: 'label.add.zone',
                  action: {
                    custom: cloudStack.uiCustom.zoneWizard(
                      cloudStack.zoneWizard
                    )
                  },
                  messages: {
                    notification: function(args) {
                      return 'label.add.zone';
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
                  label: 'label.enable.swift',
                  isHeader: true,
                  addRow: false,
                  preFilter: function(args) {
                    var swiftEnabled = false;
                    $.ajax({
                      url: createURL('listConfigurations'),
                      data: {
                        name: 'swift.enable'
                      },
                      async: false,
                      success: function(json) {
                        swiftEnabled = json.listconfigurationsresponse.configuration[0].value == 'true' && !havingSwift ?
                          true : false;
                      },

                      error: function(json) {
                        cloudStack.dialog.notice({ message: parseXMLHttpResponse(json) });
                      }
                    });

                    return swiftEnabled;
                  },
                  messages: {
                    notification: function(args) {
                      return 'label.enable.swift';
                    }
                  },
                  createForm: {
                    desc: 'confirm.enable.swift',
                    fields: {
                      url: { label: 'label.url', validation: { required: true } },
                      account: { label: 'label.account' },
                      username: { label: 'label.username' },
                      key: { label: 'label.key' }
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

                        cloudStack.dialog.notice({
                          message: 'message.after.enable.swift'
                        });
                      },
                      error: function(json) {
                        args.response.error(parseXMLHttpResponse(json));
                      }
                    });
                  }
                },

              enableS3: {
                label: 'label.enable.s3',
                isHeader: true,
                addRow: false,

                preFilter: function(args) {
                  var s3Enabled = false;
                  $.ajax({
                    url: createURL('listConfigurations'),
                    data: {
                      name: 's3.enable'
                    },
                    async: false,
                    success: function(json) {
                      s3Enabled = json.listconfigurationsresponse.configuration[0].value == 'true' && !havingS3 ?
                      true : false;
                    },
                    error: function(json) {
                      cloudStack.dialog.notice({ message: parseXMLHttpResponse(json) });
                    }
                 });

                 return s3Enabled;
              },

              messages: {
                notification: function(args) {
                  return 'label.enable.s3';
                }
              },

              createForm: {
                desc: 'confirm.enable.s3',
                fields: {
                  accesskey: { label: 'label.s3.access_key', validation: { required: true } },
                  secretkey: { label: 'label.s3.secret_key', validation: { required: true} },
                  bucket: { label: 'label.s3.bucket', validation: { required: true} },
                  endpoint: { label: 'label.s3.endpoint' },
                  usehttps: { 
                    label: 'label.s3.use_https', 
                    isEditable: true,
                    isBoolean: true,
                    isChecked: true,
                    converter:cloudStack.converters.toBooleanText 
                  },
                  connectiontimeout: { label: 'label.s3.connection_timeout' },
                  maxerrorretry: { label: 'label.s3.max_error_retry' },
                  sockettimeout: { label: 'label.s3.socket_timeout' }
                }
              },
              action: function(args) {
                $.ajax({
                  url: createURL('addS3'),
                  data: {
                        accesskey: args.data.accesskey,
                        secretkey: args.data.secretkey,
                        bucket: args.data.bucket,
                        endpoint: args.data.endpoint,
                        usehttps: (args.data.usehttps != null && args.data.usehttps == 'on' ? 'true' : 'false'),
                        connectiontimeout: args.data.connectiontimeout,
                        maxerrorretry: args.data.maxerrorretry,
                        sockettimeout: args.data.sockettimeout
                      },
                      success: function(json) {
                        havingS3 = true;
                        args.response.success();

                        cloudStack.dialog.notice({
                          message: 'message.after.enable.s3'
                        });
                      },
                      error: function(json) {
                        args.response.error(parseXMLHttpResponse(json));
                      }
                    });
                  }
                }
              },

              detailView: {
                isMaximized: true,
                actions: {
                  enable: {
                    label: 'label.action.enable.zone',
                    messages: {
                      confirm: function(args) {
                        return 'message.action.enable.zone';
                      },
                      notification: function(args) {
                        return 'label.action.enable.zone';
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
                    label: 'label.action.disable.zone',
                    messages: {
                      confirm: function(args) {
                        return 'message.action.disable.zone';
                      },
                      notification: function(args) {
                        return 'label.action.disable.zone';
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

                  'remove': {
                    label: 'label.action.delete.zone',
                    messages: {
                      confirm: function(args) {
                        return 'message.action.delete.zone';
                      },
                      notification: function(args) {
                        return 'label.action.delete.zone';
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
                  },
                  edit: {
                    label: 'label.edit',
                    action: function(args) {
                      var array1 = [];
                      array1.push("&name="  +todb(args.data.name));
                      array1.push("&dns1=" + todb(args.data.dns1));
                      array1.push("&dns2=" + todb(args.data.dns2));  //dns2 can be empty ("") when passed to API
                      
                      if (selectedZoneObj.networktype == "Advanced" && args.data.guestcidraddress) {
                        array1.push("&guestcidraddress=" + todb(args.data.guestcidraddress));
                      }
                      
                      array1.push("&internaldns1=" + todb(args.data.internaldns1));
                      array1.push("&internaldns2=" + todb(args.data.internaldns2));  //internaldns2 can be empty ("") when passed to API
                      array1.push("&domain=" + todb(args.data.domain));
                      array1.push("&localstorageenabled=" + (args.data.localstorageenabled == 'on'));
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
                    title: 'label.details',

                    preFilter: function(args) {
                      var hiddenFields = [];
                      if(selectedZoneObj.networktype == "Basic")
                        hiddenFields.push("guestcidraddress");
                      return hiddenFields;
                    },

                    fields: [
                      {
                        name: { label: 'label.zone', isEditable: true, validation: { required: true } }
                      },
                      {
                        id: { label: 'label.id' },
                        allocationstate: { label: 'label.allocation.state' },
                        dns1: { label: 'label.dns.1', isEditable: true, validation: { required: true } },
                        dns2: { label: 'label.dns.2', isEditable: true },
                        internaldns1: { label: 'label.internal.dns.1', isEditable: true, validation: { required: true } },
                        internaldns2: { label: 'label.internal.dns.2', isEditable: true },
                        domainname: { label: 'label.domain' },
                        networktype: { label: 'label.network.type' },
                        guestcidraddress : { label: 'label.guest.cidr', isEditable:true },
                        domain: {
                          label: 'label.network.domain',
                          isEditable: true
                        },
                        localstorageenabled: {
                          label: 'label.local.storage.enabled',
                          isBoolean: true,
													isEditable: true,
													converter:cloudStack.converters.toBooleanText
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
                          selectedZoneObj = json.listzonesresponse.zone[0];
                          args.response.success({
                            data: json.listzonesresponse.zone[0],
                            actionFilter: zoneActionfilter
                          });
                        }
                      });
                    }
                  },

                  compute: {
                    title: 'label.compute.and.storage',
                    custom: cloudStack.uiCustom.systemChart('compute')
                  },
                  network: {
                    title: 'label.physical.network',
                    custom: cloudStack.uiCustom.systemChart('network')
                  },
                  resources: {
                    title: 'label.resources',
                    custom: cloudStack.uiCustom.systemChart('resources')
                  },

                  systemVMs: {
                    title: 'label.system.vms',
                    listView: {
                      label: 'label.system.vms',
                      id: 'systemVMs',
                      fields: {
                        name: { label: 'label.name' },
                        systemvmtype: {
                          label: 'label.type',
                          converter: function(args) {
                            if(args == "consoleproxy")
                              return "Console Proxy VM";
                            else if(args == "secondarystoragevm")
                              return "Secondary Storage VM";
                            else
                              return args;
                          }
                        },
                        zonename: { label: 'label.zone' },
                        state: {
                          label: 'label.status',
                          converter: function(str) {
                            // For localization
                            return str;
                          },
                          indicator: {
                            'Running': 'on',
                            'Stopped': 'off',
                            'Error': 'off',
                            'Destroyed': 'off'
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
                        noCompact: true,
                        name: 'System VM details',
                        actions: {
                          start: {
                            label: 'label.action.start.systemvm',
                            messages: {
                              confirm: function(args) {
                                return 'message.action.start.systemvm';
                              },
                              notification: function(args) {
                                return 'label.action.start.systemvm';
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
                            label: 'label.action.stop.systemvm',
                            messages: {
                              confirm: function(args) {
                                return 'message.action.stop.systemvm';
                              },
                              notification: function(args) {
                                return 'label.action.stop.systemvm';
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
                            label: 'label.action.reboot.systemvm',
                            messages: {
                              confirm: function(args) {
                                return 'message.action.reboot.systemvm';
                              },
                              notification: function(args) {
                                return 'label.action.reboot.systemvm';
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

                          changeService: {
                            label: 'label.change.service.offering',
                            createForm: {
                              title: 'label.change.service.offering',
                              desc: '',
                              fields: {
                                serviceOfferingId: {
                                  label: 'label.compute.offering',
                                  select: function(args) {
                                    var apiCmd = "listServiceOfferings&issystem=true";
                                    if(args.context.systemVMs[0].systemvmtype == "secondarystoragevm")
                                      apiCmd += "&systemvmtype=secondarystoragevm";
                                    else if(args.context.systemVMs[0].systemvmtype == "consoleproxy")
                                      apiCmd += "&systemvmtype=consoleproxy";
                                    $.ajax({
                                      url: createURL(apiCmd),
                                      dataType: "json",
                                      async: true,
                                      success: function(json) {
                                        var serviceofferings = json.listserviceofferingsresponse.serviceoffering;
                                        var items = [];
                                        $(serviceofferings).each(function() {
                                          if(this.id != args.context.systemVMs[0].serviceofferingid) {
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
                            messages: {
                              notification: function(args) {
                                return 'label.change.service.offering';
                              }
                            },
                            action: function(args) {
                              $.ajax({
                                url: createURL("changeServiceForSystemVm&id=" + args.context.systemVMs[0].id + "&serviceofferingid=" + args.data.serviceOfferingId),
                                dataType: "json",
                                async: true,
                                success: function(json) {
                                  var jsonObj = json.changeserviceforsystemvmresponse.systemvm;
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

                          remove: {
                            label: 'label.action.destroy.systemvm',
                            messages: {
                              confirm: function(args) {
                                return 'message.action.destroy.systemvm';
                              },
                              notification: function(args) {
                                return 'label.action.destroy.systemvm';
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
                                      getUpdatedItem: function() {
                                        return { state: 'Destroyed' };
                                      },
                                      jobId: jid
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
                            label: 'label.action.migrate.systemvm',
                            messages: {
                              notification: function(args) {
                                return 'label.action.migrate.systemvm';
                              }
                            },
                            createForm: {
                              title: 'label.action.migrate.systemvm',
                              desc: '',
                              fields: {
                                hostId: {
                                  label: 'label.host',
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
                                          //items.push({id: this.id, description: (this.name + ": " +(this.hasEnoughCapacity? "Available" : "Full"))}); //listHosts API no longer returns hasEnoughCapacity proprety
																					items.push({id: this.id, description: this.name});
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
                            label: 'label.view.console',
                            action: {
                              externalLink: {
                                url: function(args) {
                                  return clientConsoleUrl + '?cmd=access&vm=' + args.context.systemVMs[0].id;
                                },
                                title: function(args) {
                                  return args.context.systemVMs[0].id.substr(0,8);  //title in window.open() can't have space nor longer than 8 characters. Otherwise, IE browser will have error.
                                },
                                width: 820,
                                height: 640
                              }
                            }
                          }
                        },
                        tabs: {
                          details: {
                            title: 'label.details',
                            fields: [
                              {
                                name: { label: 'label.name' }
                              },
                              {
                                id: { label: 'label.id' },
                                state: { label: 'label.state' },
                                systemvmtype: {
                                  label: 'label.type',
                                  converter: function(args) {
                                    if(args == "consoleproxy")
                                      return "Console Proxy VM";
                                    else if(args == "secondarystoragevm")
                                      return "Secondary Storage VM";
                                    else
                                      return args;
                                  }
                                },
                                zonename: { label: 'label.zone' },
                                publicip: { label: 'label.public.ip' },
                                privateip: { label: 'label.private.ip' },
                                linklocalip: { label: 'label.linklocal.ip' },
                                hostname: { label: 'label.host' },
                                gateway: { label: 'label.gateway' },
                                created: { label: 'label.created', converter: cloudStack.converters.toLocalDate },
                                activeviewersessions: { label: 'label.active.sessions' }
                              }
                            ],
                            dataProvider: function(args) {
                              $.ajax({
                                url: createURL("listSystemVms&id=" + args.context.systemVMs[0].id),
                                dataType: "json",
                                async: true,
                                success: function(json) {
                                  args.response.success({
                                    actionFilter: systemvmActionfilter,
                                    data: json.listsystemvmsresponse.systemvm[0]
                                  });
                                }
                              });
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            },
            pods: function() {
              var listView = $.extend(true, {}, cloudStack.sections.system.subsections.pods.listView, {
                dataProvider: function (args) {
                  var searchByArgs = args.filterBy.search.value.length ?
                    '&name=' + args.filterBy.search.value : '';

                  $.ajax({
                    url: createURL('listPods' + searchByArgs),
                    data: { page: args.page, pageSize: pageSize, listAll: true },
                    success: function (json) {
                      args.response.success({ data: json.listpodsresponse.pod });
                    },
                    error: function (json) {
                      args.response.error(parseXMLHttpResponse(json));
                    }
                  });
                },

                detailView: {
                  updateContext: function (args) {
                    var zone;

                    $.ajax({
                      url: createURL('listZones'),
                      data: { id: args.context.pods[0].zoneid },
                      async: false,
                      success: function (json) {
                        zone = json.listzonesresponse.zone[0];
                      }
                    });

                    selectedZoneObj = zone;

                    return {
                      zones: [zone]
                    };
                  }
                }
              });

              return listView;
            },
            clusters: function() {
              var listView = $.extend(true, {}, cloudStack.sections.system.subsections.clusters.listView, {
                dataProvider: function (args) {
                  var searchByArgs = args.filterBy.search.value.length ?
                    '&name=' + args.filterBy.search.value : '';

                  $.ajax({
                    url: createURL('listClusters' + searchByArgs),
                    data: { page: args.page, pageSize: pageSize, listAll: true },
                    success: function (json) {
                      args.response.success({ data: json.listclustersresponse.cluster });
                    },
                    error: function (json) {
                      args.response.error(parseXMLHttpResponse(json));
                    }
                  });
                },

                detailView: {
                  updateContext: function (args) {
                    var zone;

                    $.ajax({
                      url: createURL('listZones'),
                      data: { id: args.context.clusters[0].zoneid },
                      async: false,
                      success: function (json) {
                        zone = json.listzonesresponse.zone[0];
                      }
                    });

                    selectedZoneObj = zone;

                    return {
                      zones: [zone]
                    };
                  }
                }
              });

              return listView;
            },
            hosts: function() {
              var listView = $.extend(true, {}, cloudStack.sections.system.subsections.hosts.listView, {
                dataProvider: function (args) {
                  var searchByArgs = args.filterBy.search.value.length ?
                    '&name=' + args.filterBy.search.value : '';

                  $.ajax({
                    url: createURL('listHosts' + searchByArgs),
                    data: { page: args.page, pageSize: pageSize, type: 'routing', listAll: true },
                    success: function (json) {
                      args.response.success({ data: json.listhostsresponse.host });
                    },
                    error: function (json) {
                      args.response.error(parseXMLHttpResponse(json));
                    }
                  });
                },

                detailView: {
                  updateContext: function (args) {
                    var zone;

                    $.ajax({
                      url: createURL('listZones'),
                      data: { id: args.context.hosts[0].zoneid },
                      async: false,
                      success: function (json) {
                        zone = json.listzonesresponse.zone[0];
                      }
                    });

                    selectedZoneObj = zone;

                    return {
                      zones: [zone]
                    };
                  }
                }
              });

              return listView;
            },
            primaryStorage: function() {
              var listView = $.extend(true, {}, cloudStack.sections.system.subsections['primary-storage'].listView, {
                dataProvider: function (args) {
                  var searchByArgs = args.filterBy.search.value.length ?
                    '&name=' + args.filterBy.search.value : '';

                  $.ajax({
                    url: createURL('listStoragePools' + searchByArgs),
                    data: { page: args.page, pageSize: pageSize, listAll: true },
                    success: function (json) {
                      args.response.success({ data: json.liststoragepoolsresponse.storagepool });
                    },
                    error: function (json) {
                      args.response.error(parseXMLHttpResponse(json));
                    }
                  });
                },

                detailView: {
                  updateContext: function (args) {
                    var zone;

                    $.ajax({
                      url: createURL('listZones'),
                      data: { id: args.context.primarystorages[0].zoneid },
                      async: false,
                      success: function (json) {
                        zone = json.listzonesresponse.zone[0];
                      }
                    });

                    selectedZoneObj = zone;

                    return {
                      zones: [zone]
                    };
                  }
                }
              });

              return listView;
            },
            secondaryStorage: function() {
              var listView = $.extend(true, {}, cloudStack.sections.system.subsections['secondary-storage'].listView, {
                dataProvider: function (args) {
                  var searchByArgs = args.filterBy.search.value.length ?
                    '&name=' + args.filterBy.search.value : '';

                  $.ajax({
                    url: createURL('listHosts' + searchByArgs),
                    data: { type: 'SecondaryStorage', page: args.page, pageSize: pageSize, listAll: true },
                    success: function (json) {
                      args.response.success({ data: json.listhostsresponse.host });
                    },
                    error: function (json) {
                      args.response.error(parseXMLHttpResponse(json));
                    }
                  });
                },

                detailView: {
                  updateContext: function (args) {
                    var zone;

                    $.ajax({
                      url: createURL('listZones'),
                      data: { id: args.context.secondarystorages[0].zoneid },
                      async: false,
                      success: function (json) {
                        zone = json.listzonesresponse.zone[0];
                      }
                    });

                    selectedZoneObj = zone;

                    return {
                      zones: [zone]
                    };
                  }
                }
              });

              return listView;
            },
            systemVms: function() {
              var listView = $.extend(true, {}, cloudStack.sections.system.subsections.systemVms.listView, {
                dataProvider: function (args) {
                  var searchByArgs = args.filterBy.search.value.length ?
                    '&name=' + args.filterBy.search.value : '';

                  $.ajax({
                    url: createURL('listSystemVms' + searchByArgs),
                    data: { page: args.page, pageSize: pageSize, listAll: true },
                    success: function (json) {
                      args.response.success({ data: json.listsystemvmsresponse.systemvm });
                    },
                    error: function (json) {
                      args.response.error(parseXMLHttpResponse(json));
                    }
                  });
                },

                detailView: {
                  updateContext: function (args) {
                    var zone;

                    $.ajax({
                      url: createURL('listZones'),
                      data: { id: args.context.systemVMs[0].zoneid },
                      async: false,
                      success: function (json) {
                        zone = json.listzonesresponse.zone[0];
                      }
                    });

                    selectedZoneObj = zone;

                    return {
                      zones: [zone]
                    };
                  }
                }
              });

              return listView;
            },
            virtualRouters: function() {
              var listView = $.extend(true, {}, cloudStack.sections.system.subsections.virtualRouters.listView, {
                dataProvider: function (args) {
                  var searchByArgs = args.filterBy.search.value.length ?
                    '&name=' + args.filterBy.search.value : '';

                  var routers = [];
                  $.ajax({
                    url: createURL("listRouters&listAll=true&page=" + args.page + "&pagesize=" + pageSize + searchByArgs),
                    dataType: 'json',
                    async: true,
                    success: function(json) {
                      var items = json.listroutersresponse.router ?
                            json.listroutersresponse.router : [];

                      $(items).map(function(index, item) {
                        routers.push(item); 
                      });
                      
                      // Get project routers
                      $.ajax({
                        url: createURL("listRouters&listAll=true&page=" + args.page + "&pagesize=" + pageSize + "&projectid=-1"),
                        dataType: 'json',
                        async: true,
                        success: function(json) {
                          var items = json.listroutersresponse.router ?
                                json.listroutersresponse.router : [];
                          
                          $(items).map(function(index, item) {
                            routers.push(item); 
                          });
                          args.response.success({
                            actionFilter: routerActionfilter,
                            data: $(routers).map(mapRouterType)
                          });
                        }
                      });
                    }
                  });
                },

                detailView: {
                  updateContext: function (args) {
                    var zone;

                    $.ajax({
                      url: createURL('listZones'),
                      data: { id: args.context.routers[0].zoneid },
                      async: false,
                      success: function (json) {
                        zone = json.listzonesresponse.zone[0];
                      }
                    });

                    selectedZoneObj = zone;

                    return {
                      zones: [zone]
                    };
                  }
                }
              });

              return listView;
            }
          }
        }
      }
    }),
    subsections: {
      virtualRouters: {
        listView: {
          label: 'label.virtual.appliances',
          id: 'routers',
          fields: {
            name: { label: 'label.name' },
            zonename: { label: 'label.zone' },
            routerType: {
              label: 'label.type'
            },
            state: {
              converter: function(str) {
                // For localization
                return str;
              },
              label: 'label.status',
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

            var routers = [];
            $.ajax({
              url: createURL("listRouters&zoneid=" + selectedZoneObj.id + "&listAll=true&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
              data: {
							  forvpc: false
							},
              success: function(json) {
                var items = json.listroutersresponse.router ?
                      json.listroutersresponse.router : [];

                $(items).map(function(index, item) {
                  routers.push(item); 
                });
                // Get project routers
                $.ajax({
                  url: createURL("listRouters&zoneid=" + selectedZoneObj.id + "&listAll=true&page=" + args.page + "&pagesize=" + pageSize + array1.join("") + "&projectid=-1"),
                  data: {
										forvpc: false
									},
                  success: function(json) {
                    var items = json.listroutersresponse.router ?
                          json.listroutersresponse.router : [];
                    
                    $(items).map(function(index, item) {
                      routers.push(item); 
                    });
                    args.response.success({
                      actionFilter: routerActionfilter,
                      data: $(routers).map(mapRouterType)
                    });
                  }
                });
              }
            });
          },
          detailView: {
            name: 'Virtual applicance details',
            actions: {
              start: {
                label: 'label.action.start.router',
                messages: {
                  confirm: function(args) {
                    return 'message.action.start.router';
                  },
                  notification: function(args) {
                    return 'label.action.start.router';
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
                label: 'label.action.stop.router',
                createForm: {
                  title: 'label.action.stop.router',
                  desc: 'message.action.stop.router',
                  fields: {
                    forced: {
                      label: 'force.stop',
                      isBoolean: true,
                      isChecked: false
                    }
                  }
                },
                messages: {
                  notification: function(args) {
                    return 'label.action.stop.router';
                  }
                },
                action: function(args) {
                  var array1 = [];
                  array1.push("&forced=" + (args.data.forced == "on"));
                  $.ajax({
                    url: createURL('stopRouter&id=' + args.context.routers[0].id + array1.join("")),
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

              'remove': {
                label: 'label.destroy.router',
                messages: {
                  confirm: function(args) {
                    return 'message.confirm.destroy.router';
                  },
                  notification: function(args) {
                    return 'label.destroy.router';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("destroyRouter&id=" + args.context.routers[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.destroyrouterresponse.jobid;
                      args.response.success({
                        _custom: {
                          jobId: jid
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
								label: 'label.action.reboot.router',
								messages: {
									confirm: function(args) {
										return 'message.action.reboot.router';
									},
									notification: function(args) {
										return 'label.action.reboot.router';
									}
								},
								action: function(args) {
									$.ajax({
										url: createURL('rebootRouter&id=' + args.context.routers[0].id),
										dataType: 'json',
										async: true,
										success: function(json) {
											var jid = json.rebootrouterresponse.jobid;
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
								label: 'label.change.service.offering',
								createForm: {
									title: 'label.change.service.offering',
									desc: '',
									fields: {
										serviceOfferingId: {
											label: 'label.compute.offering',
											select: function(args) {																															
												$.ajax({
													url: createURL('listServiceOfferings'),
													data: {
														issystem: true,
														systemvmtype: 'domainrouter'
													},
													success: function(json) {
														var serviceofferings = json.listserviceofferingsresponse.serviceoffering;
														var items = [];
														$(serviceofferings).each(function() {																		
															if(this.id != args.context.routers[0].serviceofferingid) {
																items.push({id: this.id, description: this.name});  //default one (i.e. "System Offering For Software Router") doesn't have displaytext property. So, got to use name property instead.
															}
														});
														args.response.success({data: items});
													}
												});
											}
										}
									}
								},
								messages: {
									notification: function(args) {
										return 'label.change.service.offering';
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
                label: 'label.action.migrate.router',
                createForm: {
                  title: 'label.action.migrate.router',
                  desc: '',
                  fields: {
                    hostId: {
                      label: 'label.host',
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
                              //items.push({id: this.id, description: (this.name + ": " +(this.hasEnoughCapacity? "Available" : "Full"))}); //listHosts API no longer returns hasEnoughCapacity proprety
															items.push({id: this.id, description: this.name});
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
                messages: {
                  notification: function(args) {
                    return 'label.action.migrate.router';
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
                label: 'label.view.console',
                action: {
                  externalLink: {
                    url: function(args) {
                      return clientConsoleUrl + '?cmd=access&vm=' + args.context.routers[0].id;
                    },
                    title: function(args) {
                      return args.context.routers[0].id.substr(0,8);  //title in window.open() can't have space nor longer than 8 characters. Otherwise, IE browser will have error.
                    },
                    width: 820,
                    height: 640
                  }
                }
              }
            },
            tabs: {
              details: {
                title: 'label.details',
                preFilter: function(args) {
                  var hiddenFields = [];
                  if (!args.context.routers[0].project) {
                    hiddenFields.push('project');
                    hiddenFields.push('projectid');
                  }
                  if(selectedZoneObj.networktype == 'Basic') {
                    hiddenFields.push('publicip'); //In Basic zone, guest IP is public IP. So, publicip is not returned by listRouters API. Only guestipaddress is returned by listRouters API.
                  }
                  return hiddenFields;
                },
                fields: [
                  {
                    name: { label: 'label.name' },
                    project: { label: 'label.project' }
                  },
                  {
                    id: { label: 'label.id' },
                    projectid: { label: 'label.project.id' },
                    state: { label: 'label.state' },
                    guestnetworkid: { label: 'label.network.id' },
                    publicip: { label: 'label.public.ip' },
                    guestipaddress: { label: 'label.guest.ip' },
                    linklocalip: { label: 'label.linklocal.ip' },
                    hostname: { label: 'label.host' },
                    serviceofferingname: { label: 'label.compute.offering' },
                    networkdomain: { label: 'label.network.domain' },
                    domain: { label: 'label.domain' },
                    account: { label: 'label.account' },
                    created: { label: 'label.created', converter: cloudStack.converters.toLocalDate },
                    isredundantrouter: {
                      label: 'label.redundant.router',
                      converter: cloudStack.converters.toBooleanText
                    },
                    redundantRouterState: { label: 'label.redundant.state' },
										vpcid: { label: 'VPC ID' }
                  }
                ],
                dataProvider: function(args) {
                  $.ajax({
                    url: createURL("listRouters&id=" + args.context.routers[0].id),
                    dataType: 'json',
                    async: true,
                    success: function(json) {
                      var jsonObj = json.listroutersresponse.router[0];
                      addExtraPropertiesToRouterInstanceObject(jsonObj);
                      args.response.success({
                        actionFilter: routerActionfilter,
                        data: jsonObj
                      });
                    }
                  });
                }
              },
              nics: {
                title: 'label.nics',
                multiple: true,
                fields: [
                  {
                    name: { label: 'label.name', header: true },
                    type: { label: 'label.type' },
                    traffictype: { label: 'label.traffic.type' },
                    networkname: { label: 'label.network.name' },
                    netmask: { label: 'label.netmask' },
                    ipaddress: { label: 'label.ip.address' },
                    id: { label: 'label.id' },
                    networkid: { label: 'label.network.id' },
                    isolationuri: { label: 'label.isolation.uri' },
                    broadcasturi: { label: 'label.broadcast.uri' }
                  }
                ],
                dataProvider: function(args) {
                  $.ajax({
                    url: createURL("listRouters&id=" + args.context.routers[0].id),
                    dataType: 'json',
                    async: true,
                    success: function(json) {
                      var jsonObj = json.listroutersresponse.router[0].nic;

                      args.response.success({
                        actionFilter: routerActionfilter,
                        data: $.map(jsonObj, function(nic, index) {
                          var name = 'NIC ' + (index + 1);                    
                          if (nic.isdefault) {
                            name += ' (' + _l('label.default') + ')';
                          }
                          return $.extend(nic, {
                            name: name
                          });
                        })
                      });
                    }
                  });
                }
              }
            }
          }
        }
      },
      systemVms: {
        listView: {
          label: 'label.system.vms',
          id: 'systemVMs',
          fields: {
            name: { label: 'label.name' },
            systemvmtype: {
              label: 'label.type',
              converter: function(args) {
                if(args == "consoleproxy")
                  return "Console Proxy VM";
                else if(args == "secondarystoragevm")
                  return "Secondary Storage VM";
                else
                  return args;
              }
            },
            zonename: { label: 'label.zone' },
            state: {
              label: 'label.status',
              converter: function(str) {
                // For localization
                return str;
              },
              indicator: {
                'Running': 'on',
                'Stopped': 'off',
                'Error': 'off',
                'Destroyed': 'off'
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
                label: 'label.action.start.systemvm',
                messages: {
                  confirm: function(args) {
                    return 'message.action.start.systemvm';
                  },
                  notification: function(args) {
                    return 'label.action.start.systemvm';
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
                label: 'label.action.stop.systemvm',
                messages: {
                  confirm: function(args) {
                    return 'message.action.stop.systemvm';
                  },
                  notification: function(args) {
                    return 'label.action.stop.systemvm';
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
                label: 'label.action.reboot.systemvm',
                messages: {
                  confirm: function(args) {
                    return 'message.action.reboot.systemvm';
                  },
                  notification: function(args) {
                    return 'label.action.reboot.systemvm';
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

              changeService: {
                label: 'label.change.service.offering',
                createForm: {
                  title: 'label.change.service.offering',
                  desc: '',
                  fields: {
                    serviceOfferingId: {
                      label: 'label.compute.offering',
                      select: function(args) {
                        var apiCmd = "listServiceOfferings&issystem=true";
                        if(args.context.systemVMs[0].systemvmtype == "secondarystoragevm")
                          apiCmd += "&systemvmtype=secondarystoragevm";
                        else if(args.context.systemVMs[0].systemvmtype == "consoleproxy")
                          apiCmd += "&systemvmtype=consoleproxy";
                        $.ajax({
                          url: createURL(apiCmd),
                          dataType: "json",
                          async: true,
                          success: function(json) {
                            var serviceofferings = json.listserviceofferingsresponse.serviceoffering;
                            var items = [];
                            $(serviceofferings).each(function() {
                              if(this.id != args.context.systemVMs[0].serviceofferingid) {
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
                messages: {
                  notification: function(args) {
                    return 'label.change.service.offering';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("changeServiceForSystemVm&id=" + args.context.systemVMs[0].id + "&serviceofferingid=" + args.data.serviceOfferingId),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jsonObj = json.changeserviceforsystemvmresponse.systemvm;
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

              remove: {
                label: 'label.action.destroy.systemvm',
                messages: {
                  confirm: function(args) {
                    return 'message.action.destroy.systemvm';
                  },
                  notification: function(args) {
                    return 'label.action.destroy.systemvm';
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
                          getUpdatedItem: function() {
                            return { state: 'Destroyed' };
                          },
                          jobId: jid
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
                label: 'label.action.migrate.systemvm',
                messages: {
                  notification: function(args) {
                    return 'label.action.migrate.systemvm';
                  }
                },
                createForm: {
                  title: 'label.action.migrate.systemvm',
                  desc: '',
                  fields: {
                    hostId: {
                      label: 'label.host',
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
                              //items.push({id: this.id, description: (this.name + ": " +(this.hasEnoughCapacity? "Available" : "Full"))}); //listHosts API no longer returns hasEnoughCapacity proprety
															items.push({id: this.id, description: this.name});
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
                label: 'label.view.console',
                action: {
                  externalLink: {
                    url: function(args) {
                      return clientConsoleUrl + '?cmd=access&vm=' + args.context.systemVMs[0].id;
                    },
                    title: function(args) {
                      return args.context.systemVMs[0].id.substr(0,8);  //title in window.open() can't have space nor longer than 8 characters. Otherwise, IE browser will have error.
                    },
                    width: 820,
                    height: 640
                  }
                }
              }
            },
            tabs: {
              details: {
                title: 'label.details',
                fields: [
                  {
                    name: { label: 'label.name' }
                  },
                  {
                    id: { label: 'label.id' },
                    state: { label: 'label.state' },
                    systemvmtype: {
                      label: 'label.type',
                      converter: function(args) {
                        if(args == "consoleproxy")
                          return "Console Proxy VM";
                        else if(args == "secondarystoragevm")
                          return "Secondary Storage VM";
                        else
                          return args;
                      }
                    },
                    zonename: { label: 'label.zone' },
                    publicip: { label: 'label.public.ip' },
                    privateip: { label: 'label.private.ip' },
                    linklocalip: { label: 'label.linklocal.ip' },
                    hostname: { label: 'label.host' },
                    gateway: { label: 'label.gateway' },
                    created: { label: 'label.created', converter: cloudStack.converters.toLocalDate },
                    activeviewersessions: { label: 'label.active.sessions' }
                  }
                ],
                dataProvider: function(args) {
                  $.ajax({
                    url: createURL("listSystemVms&id=" + args.context.systemVMs[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      args.response.success({
                        actionFilter: systemvmActionfilter,
                        data: json.listsystemvmsresponse.systemvm[0]
                      });
                    }
                  });
                }
              }
            }
          }
        }
      },
  // netscaler devices listView
      netscalerDevices: {
        id: 'netscalerDevices',
        title: 'label.devices',
        listView: {
          id: 'netscalerDevices',
          fields: {
            ipaddress: { label: 'label.ip.address' },
            lbdevicestate: {
              converter: function(str) {
                // For localization
                return str;
              },
              label: 'label.status'
            }
          },
          dataProvider: function(args) {
            $.ajax({
              url: createURL("listNetscalerLoadBalancers&physicalnetworkid=" + selectedPhysicalNetworkObj.id),
              data: { page: args.page, pageSize: pageSize },
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
              label: 'label.add.netScaler.device',
              createForm: {
                title: 'label.add.netScaler.device',
								preFilter: cloudStack.preFilter.addLoadBalancerDevice,	
                fields: {
                  ip: {
                    label: 'label.ip.address'
                  },
                  username: {
                    label: 'label.username'
                  },
                  password: {
                    label: 'label.password',
                    isPassword: true
                  },
                  networkdevicetype: {
                    label: 'label.type',
                    select: function(args) {
                      var items = [];
                      items.push({id: "NetscalerMPXLoadBalancer", description: "NetScaler MPX LoadBalancer"});
                      items.push({id: "NetscalerVPXLoadBalancer", description: "NetScaler VPX LoadBalancer"});
                      items.push({id: "NetscalerSDXLoadBalancer", description: "NetScaler SDX LoadBalancer"});
                      args.response.success({data: items});
                    }
                  },
                  publicinterface: {
                    label: 'label.public.interface'
                  },
                  privateinterface: {
                    label: 'label.private.interface'
                  },
                  numretries: {
                    label: 'label.numretries',
                    defaultValue: '2'
                  },
                 /* inline: {
                    label: 'Mode',
                    select: function(args) {
                      var items = [];
                      items.push({id: "false", description: "side by side"});
                      items.push({id: "true", description: "inline"});
                      args.response.success({data: items});
                    }
                  },*/
                  dedicated: {
                    label: 'label.dedicated',
                    isBoolean: true,
                    isChecked: false
                  },
									capacity: {
                    label: 'label.capacity',
                    validation: { required: false, number: true }
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
											var addNetscalerProviderIntervalID = setInterval(function() { 	
                        $.ajax({
                          url: createURL("queryAsyncJobResult&jobId="+jobId),
                          dataType: "json",
                          success: function(json) {
                            var result = json.queryasyncjobresultresponse;
                            if (result.jobstatus == 0) {
                              return; //Job has not completed
                            }
                            else {
                              clearInterval(addNetscalerProviderIntervalID); 
                              if (result.jobstatus == 1) {
                                nspMap["netscaler"] = result.jobresult.networkserviceprovider;
                                addExternalLoadBalancer(args, selectedPhysicalNetworkObj, "addNetscalerLoadBalancer", "addnetscalerloadbalancerresponse", "netscalerloadbalancer");
                              }
                              else if (result.jobstatus == 2) {
                                alert("addNetworkServiceProvider&name=Netscaler failed. Error: " + _s(result.jobresult.errortext));
                              }
                            }
                          },
                          error: function(XMLHttpResponse) {
                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                            alert("addNetworkServiceProvider&name=Netscaler failed. Error: " + errorMsg);
                          }
                        });
                      }, g_queryAsyncJobResultInterval); 		
                    }
                  });
                }
                else {
                  addExternalLoadBalancer(args, selectedPhysicalNetworkObj, "addNetscalerLoadBalancer", "addnetscalerloadbalancerresponse", "netscalerloadbalancer");
                }
              },
              messages: {
                notification: function(args) {
                  return 'label.add.netScaler.device';
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
              'remove': {
                label: 'label.delete.NetScaler',
                messages: {
                  confirm: function(args) {
                    return 'message.confirm.delete.NetScaler';
                  },
                  notification: function(args) {
                    return 'label.delete.NetScaler';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteNetscalerLoadBalancer&lbdeviceid=" + args.context.netscalerDevices[0].lbdeviceid),
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
                title: 'label.details',
                fields: [
                  {
                    lbdeviceid: { label: 'label.id' },
                    ipaddress: { label: 'label.ip.address' },
                    lbdevicestate: { label: 'label.status' },
                    lbdevicename: { label: 'label.type' },
                    lbdevicecapacity: { label: 'label.capacity' },
                    lbdevicededicated: {
                      label: 'label.dedicated',
                      converter: cloudStack.converters.toBooleanText
                    }
                  }
                ],
                dataProvider: function(args) {								  
									$.ajax({
										url: createURL("listNetscalerLoadBalancers&lbdeviceid=" + args.context.netscalerDevices[0].lbdeviceid),									
										dataType: "json",
										async: true,
										success: function(json) {										 
											var item = json.listnetscalerloadbalancerresponse.netscalerloadbalancer[0];
											args.response.success({data: item});
										}
									});									             
                }
              }
            }
          }
        }
      },

			// F5 devices listView
      f5Devices: {
        id: 'f5Devices',
        title: 'label.devices',
        listView: {
          id: 'f5Devices',
          fields: {
            ipaddress: { label: 'label.ip.address' },
            lbdevicestate: {
              converter: function(str) {
                // For localization
                return str;
              },
              label: 'label.status'
            }
          },
          actions: {
            add: {
              label: 'label.add.F5.device',
              createForm: {
                title: 'label.add.F5.device',
								preFilter: cloudStack.preFilter.addLoadBalancerDevice,	
                fields: {
                  ip: {
                    label: 'label.ip.address'
                  },
                  username: {
                    label: 'label.username'
                  },
                  password: {
                    label: 'label.password',
                    isPassword: true
                  },
                  networkdevicetype: {
                    label: 'label.type',
                    select: function(args) {
                      var items = [];
                      items.push({id: "F5BigIpLoadBalancer", description: "F5 Big Ip Load Balancer"});
                      args.response.success({data: items});
                    }
                  },
                  publicinterface: {
                    label: 'label.public.interface'
                  },
                  privateinterface: {
                    label: 'label.private.interface'
                  },
                  numretries: {
                    label: 'label.numretries',
                    defaultValue: '2'
                  },
									//Inline Mode has been moved from Add F5 Device to Create Network Offering (both backend and UI)
									/*
                  inline: {
                    label: 'Mode',
                    select: function(args) {
                      var items = [];
                      items.push({id: "false", description: "side by side"});
                      items.push({id: "true", description: "inline"});
                      args.response.success({data: items});
                    }
                  },  
                  */									
                  dedicated: {
                    label: 'label.dedicated',
                    isBoolean: true,
                    isChecked: false
                  },
									capacity: {
                    label: 'label.capacity',
                    validation: { required: false, number: true }
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
                      var addF5ProviderIntervalID = setInterval(function() { 	
                        $.ajax({
                          url: createURL("queryAsyncJobResult&jobId="+jobId),
                          dataType: "json",
                          success: function(json) {
                            var result = json.queryasyncjobresultresponse;
                            if (result.jobstatus == 0) {
                              return; //Job has not completed
                            }
                            else {
                              clearInterval(addF5ProviderIntervalID); 
                              if (result.jobstatus == 1) {
                                nspMap["f5"] = json.queryasyncjobresultresponse.jobresult.networkserviceprovider;
                                addExternalLoadBalancer(args, selectedPhysicalNetworkObj, "addF5LoadBalancer", "addf5bigiploadbalancerresponse", "f5loadbalancer");
                              }
                              else if (result.jobstatus == 2) {
                                alert("addNetworkServiceProvider&name=F5BigIp failed. Error: " + _s(result.jobresult.errortext));
                              }
                            }
                          },
                          error: function(XMLHttpResponse) {
                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                            alert("addNetworkServiceProvider&name=F5BigIpfailed. Error: " + errorMsg);
                          }
                        });
                      }, g_queryAsyncJobResultInterval); 		
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
              data: { page: args.page, pageSize: pageSize },
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
              'remove': {
                label: 'label.delete.F5',
                messages: {
                  confirm: function(args) {
                    return 'message.confirm.delete.F5';
                  },
                  notification: function(args) {
                    return 'label.delete.F5';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteF5LoadBalancer&lbdeviceid=" + args.context.f5Devices[0].lbdeviceid),
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
                title: 'label.details',
                fields: [
                  {
                    lbdeviceid: { label: 'label.id' },
                    ipaddress: { label: 'label.ip.address' },
                    lbdevicestate: { label: 'label.status' },
                    lbdevicename: { label: 'label.type' },
                    lbdevicecapacity: { label: 'label.capacity' },
                    lbdevicededicated: {
                      label: 'label.dedicated',
                      converter: cloudStack.converters.toBooleanText
                    }
                  }
                ],
                dataProvider: function(args) {								  
									$.ajax({
										url: createURL("listF5LoadBalancers&lbdeviceid=" + args.context.f5Devices[0].lbdeviceid),										
										dataType: "json",
										async: true,
										success: function(json) {										  
											var item = json.listf5loadbalancerresponse.f5loadbalancer[0];
											args.response.success({data: item});
										}
									});									
                }
              }
            }
          }
        }
      },

	  //SRX devices listView
      srxDevices: {
        id: 'srxDevices',
        title: 'label.devices',
        listView: {
          id: 'srxDevices',
          fields: {
            ipaddress: { label: 'label.ip.address' },
            fwdevicestate: { label: 'label.status' },
            fwdevicename: { label: 'label.type' }
          },
          actions: {
            add: {
              label: 'label.add.SRX.device',
              createForm: {
                title: 'label.add.SRX.device',
                fields: {
                  ip: {
                    label: 'label.ip.address'
                  },
                  username: {
                    label: 'label.username'
                  },
                  password: {
                    label: 'label.password',
                    isPassword: true
                  },
                  networkdevicetype: {
                    label: 'label.type',
                    select: function(args) {
                      var items = [];
                      items.push({id: "JuniperSRXFirewall", description: "Juniper SRX Firewall"});
                      args.response.success({data: items});
                    }
                  },
                  publicinterface: {
                    label: 'label.public.interface'
                  },
                  privateinterface: {
                    label: 'label.private.interface'
                  },
                  usageinterface: {
                    label: 'label.usage.interface'
                  },
                  numretries: {
                    label: 'label.numretries',
                    defaultValue: '2'
                  },
                  timeout: {
                    label: 'label.timeout',
                    defaultValue: '300'
                  },
                  // inline: {
                  //   label: 'Mode',
                  //   select: function(args) {
                  //     var items = [];
                  //     items.push({id: "false", description: "side by side"});
                  //     items.push({id: "true", description: "inline"});
                  //     args.response.success({data: items});
                  //   }
                  // },
                  publicnetwork: {
                    label: 'label.public.network',
                    defaultValue: 'untrusted',
                    isDisabled:true
                  },
                  privatenetwork: {
                    label: 'label.private.network',
                    defaultValue: 'trusted',
                    isDisabled:true
                  },
                  capacity: {
                    label: 'label.capacity',
                    validation: { required: false, number: true }
                  },
                  dedicated: {
                    label: 'label.dedicated',
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
                      var addJuniperSRXProviderIntervalID = setInterval(function() { 	
                        $.ajax({
                          url: createURL("queryAsyncJobResult&jobId="+jobId),
                          dataType: "json",
                          success: function(json) {
                            var result = json.queryasyncjobresultresponse;
                            if (result.jobstatus == 0) {
                              return; //Job has not completed
                            }
                            else {
                              clearInterval(addJuniperSRXProviderIntervalID); 
                              if (result.jobstatus == 1) {
                                nspMap["srx"] = json.queryasyncjobresultresponse.jobresult.networkserviceprovider;
                                addExternalFirewall(args, selectedPhysicalNetworkObj, "addSrxFirewall", "addsrxfirewallresponse", "srxfirewall");
                              }
                              else if (result.jobstatus == 2) {
                                alert("addNetworkServiceProvider&name=JuniperSRX failed. Error: " + _s(result.jobresult.errortext));
                              }
                            }
                          },
                          error: function(XMLHttpResponse) {
                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                            alert("addNetworkServiceProvider&name=JuniperSRX failed. Error: " + errorMsg);
                          }
                        });
                      }, g_queryAsyncJobResultInterval); 		
                    }
                  });
                }
                else {
                  addExternalFirewall(args, selectedPhysicalNetworkObj, "addSrxFirewall", "addsrxfirewallresponse", "srxfirewall");
                }
              },
              messages: {
                notification: function(args) {
                  return 'label.add.SRX.device';
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
              data: { page: args.page, pageSize: pageSize },
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
              'remove': {
                label: 'label.delete.SRX',
                messages: {
                  confirm: function(args) {
                    return 'message.confirm.delete.SRX';
                  },
                  notification: function(args) {
                    return 'label.delete.SRX';
                  }
                },
                action: function(args) {                
                  $.ajax({
                    url: createURL("deleteSrxFirewall&fwdeviceid=" + args.context.srxDevices[0].fwdeviceid),
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
                title: 'label.details',
                fields: [
                  {
                    fwdeviceid: { label: 'label.id' },
                    ipaddress: { label: 'label.ip.address' },
                    fwdevicestate: { label: 'label.status' },
                    fwdevicename: { label: 'label.type' },
                    fwdevicecapacity: { label: 'label.capacity' },
                    timeout: { label: 'label.timeout' }
                  }
                ],
                dataProvider: function(args) {	
									$.ajax({
										url: createURL("listSrxFirewalls&fwdeviceid=" + args.context.srxDevices[0].fwdeviceid),										
										dataType: "json",
										async: true,
										success: function(json) {										  
											var item = json.listsrxfirewallresponse.srxfirewall[0];
											args.response.success({data: item});
										}
									});											
                }
              }
            }
          }
        }
      },
      // FIXME convert to nicira detailview
      // NiciraNvp devices listView
      niciraNvpDevices: {
        id: 'niciraNvpDevices',
        title: 'label.devices',
        listView: {
          id: 'niciraNvpDevices',
          fields: {
            hostname: { label: 'label.nicira.controller.address' },
            transportzoneuuid: { label: 'label.nicira.transportzoneuuid'},
            l3gatewayserviceuuid: { label: 'label.nicira.l3gatewayserviceuuid' }
          },
          actions: {
        	  add: {
                label: 'label.add.NiciraNvp.device',
                createForm: {
                  title: 'label.add.NiciraNvp.device',
                  preFilter: function(args) {  },   // TODO What is this?
                  fields: {
                    host: {
                      label: 'label.ip.address'
                    },
                    username: {
                      label: 'label.username'
                    },
                    password: {
                      label: 'label.password',
                      isPassword: true
                    },
                    numretries: {
                      label: 'label.numretries',
                      defaultValue: '2'
                    },
                    transportzoneuuid: {
                      label: 'label.nicira.transportzoneuuid'
                    },
                    l3gatewayserviceuuid: {
                      label: 'label.nicira.l3gatewayserviceuuid'
                    }
                  }
                },
                action: function(args) {
                  if(nspMap["niciraNvp"] == null) {
                    $.ajax({
                      url: createURL("addNetworkServiceProvider&name=NiciraNvp&physicalnetworkid=" + selectedPhysicalNetworkObj.id),
                      dataType: "json",
                      async: true,
                      success: function(json) {
                        var jobId = json.addnetworkserviceproviderresponse.jobid;                        
                        var addNiciraNvpProviderIntervalID = setInterval(function() {  
                          $.ajax({
                            url: createURL("queryAsyncJobResult&jobId="+jobId),
                            dataType: "json",
                            success: function(json) {
                              var result = json.queryasyncjobresultresponse;
                              if (result.jobstatus == 0) {
                                return; // Job has not completed
                              }
                              else {
                                clearInterval(addNiciraNvpProviderIntervalID); 
                                if (result.jobstatus == 1) {
                                  nspMap["niciraNvp"] = json.queryasyncjobresultresponse.jobresult.networkserviceprovider;
                                  addNiciraNvpDevice(args, selectedPhysicalNetworkObj, "addNiciraNvpDevice", "addniciranvpdeviceresponse", "niciranvpdevice")
                                }
                                else if (result.jobstatus == 2) {
                                  alert("addNetworkServiceProvider&name=NiciraNvp failed. Error: " + _s(result.jobresult.errortext));
                                }
                              }
                            },
                            error: function(XMLHttpResponse) {
                              var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                              alert("addNetworkServiceProvider&name=NiciraNvp failed. Error: " + errorMsg);
                            }
                          });
                        }, g_queryAsyncJobResultInterval);       
                      }
                    });
                  }
                  else {
                      addNiciraNvpDevice(args, selectedPhysicalNetworkObj, "addNiciraNvpDevice", "addniciranvpdeviceresponse", "niciranvpdevice")
                  }
                },
        	  
              messages: {
                notification: function(args) {
                  return 'Added new Nicira Nvp Controller';
                }
              },
              notification: {
                poll: pollAsyncJobResult
              }
            }
          },
          dataProvider: function(args) {
            $.ajax({
              url: createURL("listNiciraNvpDevices&physicalnetworkid=" + selectedPhysicalNetworkObj.id),
              data: { page: args.page, pageSize: pageSize },
              dataType: "json",
              async: false,
              success: function(json) {
                var items = json.listniciranvpdeviceresponse.niciranvpdevice;
                args.response.success({data: items});
              }
            });
          },   
          detailView: {
            name: 'Nicira Nvp details',
            actions: {
              'remove': {
                label: 'label.delete.NiciaNvp',
                messages: {
                  confirm: function(args) {
                    return 'message.confirm.delete.NiciraNvp';
                  },
                  notification: function(args) {
                    return 'label.delete.NiciraNvp';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteNiciraNvpDevice&nvpdeviceid=" + args.context.niciraNvpDevices[0].nvpdeviceid),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.deleteniciranvpdeviceresponse.jobid;
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
                title: 'label.details',
                fields: [
                  {
                    nvpdeviceid: { label: 'label.id' },
                    hostname: { label: 'label.ip.address' },
                    transportzoneuuid: { label: 'label.nicira.transportzoneuuid' },
                    l3gatewayserviceuuid: { label: 'label.nicira.l3gatewayserviceuuid' }
                  }
                ],
                dataProvider: function(args) {                                
                                    $.ajax({
                                        url: createURL("listNiciraNvpDevices&nvpdeviceid=" + args.context.niciraNvpDevices[0].nvpdeviceid),                                       
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {                                         
                                            var item = json.listniciranvpdeviceresponse.niciranvpdevice[0];
                                            args.response.success({data: item});
                                        }
                                    });                                 
                }
              }
            }
          }
        }
      },
      pods: {
        title: 'label.pods',
        listView: {
          id: 'pods',
          section: 'pods',
          fields: {
            name: { label: 'label.name' },
            gateway: { label: 'label.gateway' },
            netmask: { label: 'label.netmask' },
            allocationstate: {
              converter: function(str) {
                // For localization
                return str;
              },
              label: 'label.allocation.state'
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
              label: 'label.add.pod',

              createForm: {
                title: 'label.add.pod',
                fields: {
                  zoneid: {
                    label: 'Zone',
                    docID: 'helpPodZone',
                    validation: { required: true },
                    select: function(args) {
                      var data = args.context.zones ?
                            { id: args.context.zones[0].id } : { listAll: true };

                      $.ajax({
                        url: createURL('listZones'),
                        data: data,
                        success: function(json) {
                          var zones = json.listzonesresponse.zone;

                          args.response.success({
                            data: $.map(zones, function(zone) {
                              return {
                                id: zone.id,
                                description: zone.name
                              };
                            })
                          });
                        }
                      });
                    }
                  },
                  podname: {
                    label: 'label.pod.name',
                    docID: 'helpPodName',
                    validation: { required: true }
                  },
                  reservedSystemGateway: {
                    label: 'label.reserved.system.gateway',
                    docID: 'helpPodGateway',
                    validation: { required: true }
                  },
                  reservedSystemNetmask: {
                    label: 'label.reserved.system.netmask',
                    docID: 'helpPodNetmask',
                    validation: { required: true }
                  },
                  reservedSystemStartIp: {
                    label: 'label.start.reserved.system.IP',
                    docID: 'helpPodStartIP',
                    validation: { required: true }
                  },
                  reservedSystemEndIp: {
                    label: 'label.end.reserved.system.IP',
                    docID: 'helpPodEndIP',
                    validation: { required: false }
                  }
                }
              },

              action: function(args) {
                var array1 = [];
                
                array1.push("&zoneId=" + args.data.zoneid);
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
                  return 'label.add.pod';
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
                label: 'label.edit',
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
                     },
                      error: function(data) {
                          args.response.error(parseXMLHttpResponse(data));
                          }
                   });
                }
              },

              enable: {
                label: 'label.action.enable.pod',
                messages: {
                  confirm: function(args) {
                    return 'message.action.enable.pod';
                  },
                  notification: function(args) {
                    return 'label.action.enable.pod';
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
                label: 'label.action.disable.pod',
                messages: {
                  confirm: function(args) {
                    return 'message.action.disable.pod';
                  },
                  notification: function(args) {
                    return 'label.action.disable.pod';
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

              'remove': {
                label: 'label.delete' ,
                messages: {
                  confirm: function(args) {
                    return 'message.action.delete.pod';
                  },
                  notification: function(args) {
                    return 'label.action.delete.pod';
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
                title: 'label.details',
                fields: [
                  {
                    name: { label: 'label.name', isEditable: true, validation: { required: true } }
                  },
                  {
                    id: { label: 'label.id' },
                    netmask: { label: 'label.netmask', isEditable: true, validation: { required: true } },
                    startip: { label: 'label.start.IP', isEditable: true, validation: { required: true } },
                    endip: { label: 'label.end.IP', isEditable: true },
                    gateway: { label: 'label.gateway', isEditable: true, validation: { required: true } },
                    allocationstate: {
                      converter: function(str) {
                        // For localization
                        return str;
                      },
                      label: 'label.allocation.state'
                    }
                  }
                ],

                dataProvider: function(args) {								  
									$.ajax({
										url: createURL("listPods&id=" + args.context.pods[0].id),
										dataType: "json",
										async: true,
										success: function(json) {										  
											var item = json.listpodsresponse.pod[0];
											args.response.success({
												actionFilter: podActionfilter,
												data:item
											});
										}
									});									              
                }
              },

              ipAllocations: {
                title: 'label.ip.allocations',
                multiple: true,
                fields: [
                  {
                    id: { label: 'label.id' },
                    gateway: { label: 'label.gateway' },
                    netmask: { label: 'label.netmask' },
                    startip: { label: 'label.start.IP' },
                    endip: { label: 'label.end.IP' }
                  }
                ],
                dataProvider: function(args) {
                  $.ajax({
                    url: createURL("listVlanIpRanges&zoneid=" + args.context.zones[0].id + "&podid=" + args.context.pods[0].id),
                    dataType: "json",
                    success: function(json) {
                      var items = json.listvlaniprangesresponse.vlaniprange;
                      args.response.success({ data: items });
                    }
                  });
                }
              }
            }
          }
        }
      },
      clusters: {
        title: 'label.clusters',
        listView: {
          id: 'clusters',
          section: 'clusters',
          fields: {
            name: { label: 'label.name' },
            podname: { label: 'label.pod' },
            hypervisortype: { label: 'label.hypervisor' },
            //allocationstate: { label: 'label.allocation.state' },
            //managedstate: { label: 'Managed State' },
		allocationstate: {
              converter: function(str) {
                // For localization
                return str;
              },
              label: 'label.state',
              indicator: { 'Enabled': 'on', 'Destroyed': 'off'}
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
                  addExtraPropertiesToClusterObject(this);
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
              label: 'label.add.cluster',
              messages: {
                notification: function(args) {
                  return 'label.add.cluster';
                }
              },
              createForm: {
                title: 'label.add.cluster',
                fields: {
                  zoneid: {
                    label: 'Zone Name',
                    docID: 'helpClusterZone',
                    validation: { required: true },
                    select: function(args) {
                      var data = args.context.zones ?
                            { id: args.context.zones[0].id } : { listAll: true };

                      $.ajax({
                        url: createURL('listZones'),
                        data: data,
                        success: function(json) {
                          var zones = json.listzonesresponse.zone;

                          args.response.success({
                            data: $.map(zones, function(zone) {
                              return {
                                id: zone.id,
                                description: zone.name
                              };
                            })
                          });
                        }
                      });
                    }
                  },
                  hypervisor: {
                    label: 'label.hypervisor',
                    docID: 'helpClusterHypervisor',
                    select: function(args) {
                      var vSwitchEnabled = false;
                      var dvSwitchEnabled = false;

                      $.ajax({
                        url: createURL("listHypervisors"),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                          var hypervisors = json.listhypervisorsresponse.hypervisor;
                          var items = [];
                          $(hypervisors).each(function() {
                            items.push({id: this.name, description: this.name});
                          });
                          args.response.success({data: items});
                        }
                      });

                      // Check whether vSwitch capability is enabled
                      $.ajax({
                        url: createURL('listConfigurations'),
                        data: {
                          name: 'vmware.use.nexus.vswitch'
                        },
                        async: false,
                        success: function(json) {
                          if (json.listconfigurationsresponse.configuration[0].value == 'true') {
                            vSwitchEnabled = true;
                          }
                        }
                      });
                      
                      //Check whether dvSwitch is enabled or not
                      $.ajax({
                        url: createURL('listConfigurations'),
                        data: {
                           name: 'vmware.use.dvswitch'
                              },
                        async: false,
                        success: function(json) {
                          if (json.listconfigurationsresponse.configuration[0].value == 'true') {
                                dvSwitchEnabled = true;
                          }
                        }
                      });


                      args.$select.bind("change", function(event) {
                        var $form = $(this).closest('form');
                        var $vsmFields = $form.find('.form-item').filter(function() {
                          var vsmFields = [
                            'vsmipaddress',
                            'vsmusername',
                            'vsmpassword'
                          ];

                          return $.inArray($(this).attr('rel'), vsmFields) > -1;
                        });

                        if ($(this).val() == "VMware") {
                          //$('li[input_sub_group="external"]', $dialogAddCluster).show();

                          if(dvSwitchEnabled){
                        // $form.find('.form-item[rel=vSwitchPublicType]').css('display', 'inline-block');
                         // $form.find('.form-item[rel=vSwitchGuestType]').css('display', 'inline-block');
                         // $form.find('.form-item[rel=vSwitchPublicName]').css('display','inline-block');
                          //$form.find('.form-item[rel=vSwitchGuestName]').css('display','inline-block');
                          $form.find('.form-item[rel=overridepublictraffic]').css('display','inline-block');
                          $form.find('.form-item[rel=overridepublictraffic]').find('input[type=checkbox]').removeAttr('checked');
                          
                          $form.find('.form-item[rel=overrideguesttraffic]').css('display','inline-block');
                          $form.find('.form-item[rel=overrideguesttraffic]').find('input[type=checkbox]').removeAttr('checked');

 

                         }
                          else {
                                //  $form.find('.form-item[rel=vSwitchPublicType]').css('display', 'none');
                                //  $form.find('.form-item[rel=vSwitchGuestType]').css('display', 'none');
                                //  $form.find('.form-item[rel=vSwitchPublicName]').css('display','none');
                                 // $form.find('.form-item[rel=vSwitchGuestName]').css('display','none');
                                  $form.find('.form-item[rel=overridepublictraffic]').css('display','none');
                                  $form.find('.form-item[rel=overrideguesttraffic]').css('display','none');


                          } 
                          $form.find('.form-item[rel=vCenterHost]').css('display', 'inline-block');
                          $form.find('.form-item[rel=vCenterUsername]').css('display', 'inline-block');
                          $form.find('.form-item[rel=vCenterPassword]').css('display', 'inline-block');
                          $form.find('.form-item[rel=vCenterDatacenter]').css('display', 'inline-block');

                          if (vSwitchEnabled) {
                            $vsmFields.css('display', 'inline-block');
                          } else {
                            $vsmFields.css('display', 'none'); 
                          }
                        } else {
                          
                 
                           $form.find('.form-item[rel=overridepublictraffic]').css('display', 'none');
                           $form.find('.form-item[rel=overrideguesttraffic]').css('display', 'none');
                           $form.find('.form-item[rel=vSwitchPublicType]').css('display', 'none');
                           $form.find('.form-item[rel=vSwitchGuestType]').css('display', 'none');
                           $form.find('.form-item[rel=vSwitchPublicName]').css('display','none');
                           $form.find('.form-item[rel=vSwitchGuestName]').css('display','none');


                          $form.find('.form-item[rel=vCenterHost]').css('display', 'none');
                          $form.find('.form-item[rel=vCenterUsername]').css('display', 'none');
                          $form.find('.form-item[rel=vCenterPassword]').css('display', 'none');
                          $form.find('.form-item[rel=vCenterDatacenter]').css('display', 'none');
                          $form.find('.form-item[rel=enableNexusVswitch]').css('display', 'none');
                          $vsmFields.css('display', 'none');
                        }
                      });
                    }
                  },
                  podId: {
                    label: 'Pod Name',
                    docID: 'helpClusterPod',
                    dependsOn: 'zoneid',
                    select: function(args) {
                      $.ajax({
                        url: createURL("listPods&zoneid=" + args.zoneid),
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
                    label: 'label.cluster.name',
                    docID: 'helpClusterName',
                    validation: { required: true }
                  },

                  cpuovercommit:{
                     label: 'CPU overcommit ratio',
                     defaultValue:'1'

                  },

                  memoryovercommit:{
                     label: 'RAM overcommit ratio',
                     defaultValue:'1'

                   },

                  //hypervisor==VMWare begins here

                  vCenterHost: {
                    label: 'label.vcenter.host',
                    docID: 'helpClustervCenterHost',
                    validation: { required: true }
                  },
                  vCenterUsername: {
                    label: 'label.vcenter.username',
                    docID: 'helpClustervCenterUsername',
                    validation: { required: true }
                  },
                  vCenterPassword: {
                    label: 'label.vcenter.password',
                    docID: 'helpClustervCenterPassword',
                    validation: { required: true },
                    isPassword: true
                  },
                  vCenterDatacenter: {
                    label: 'label.vcenter.datacenter',
                    docID: 'helpClustervCenterDatacenter',
                    validation: { required: true }
                  },

                    overridepublictraffic:{
                     label:'Override Public-Traffic',
                     isBoolean:true,
                     isHidden:true,
                     isChecked:false,
                     docID:'helpOverridePublicNetwork'

                  },


                  vSwitchPublicType:{
                       label: 'Public Traffic vSwitch Type',
                        select: function(args) {
                            var vSwitchEnabled = false;                 
                            var items = []
                             $.ajax({
                        url: createURL('listConfigurations'),
                        data: {
                          name: 'vmware.use.nexus.vswitch'
                        },
                        async: false,
                        success: function(json) {
                          if (json.listconfigurationsresponse.configuration[0].value == 'true') {
                            vSwitchEnabled = true;
                          }
                        }
                      });

                            if(vSwitchEnabled) {
                  
                              items.push({ id:" nexusdvs" , description: "Cisco Nexus 1000v Distributed Virtual Switch"});
                              items.push({id: "vmwaresvs", description: "VMware vNetwork Standard Virtual Switch"});
                              items.push({id: "vmwaredvs", description: "VMware vNetwork Distributed Virtual Switch"});




                              }                         

                             // items.push({id: "" , description:" " });
                            else{
                              items.push({id: "vmwaredvs", description: "VMware vNetwork Distributed Virtual Switch"});
                              items.push({id: "vmwaresvs", description: "VMware vNetwork Standard Virtual Switch"});
                              items.push({ id:" nexusdvs" , description: "Cisco Nexus 1000v Distributed Virtual Switch"});
                           }

                              args.response.success({data: items});
                           },
                        isHidden:true,
                        dependsOn:'overridepublictraffic'
                      },

                   vSwitchPublicName:{
                        label:'Public Traffic vSwitch Name',
                        dependsOn:'overridepublictraffic',
                        isHidden:true


                     },

                 overrideguesttraffic:{
                     label:'Override Guest-Traffic',
                     isBoolean:true,
                     isHidden:true,
                     isChecked:false,
                     docID:'helpOverrideGuestNetwork'

                  },


                 vSwitchGuestType:{
                        label: 'Guest Traffic vSwitch Type',
                        select: function(args) {
                        var items = []
                      //  items.push({id: "" , description:" " });

                         var vSwitchEnabled = false;
                             $.ajax({
                        url: createURL('listConfigurations'),
                        data: {
                          name: 'vmware.use.nexus.vswitch'
                        },
                        async: false,
                        success: function(json) {
                          if (json.listconfigurationsresponse.configuration[0].value == 'true') {
                            vSwitchEnabled = true;
                          }
                        }
                      });


                       if(vSwitchEnabled){
                        items.push({ id:"nexusdvs" , description: "Cisco Nexus 1000v Distributed Virtual Switch"});
                        items.push({id: "vmwaresvs", description: "VMware vNetwork Standard Virtual Switch"});
                        items.push({id: "vmwaredvs", description: "VMware vNetwork Distributed Virtual Switch"}); 

                       }


                       else{
                           items.push({id: "vmwaredvs", description: "VMware vNetwork Distributed Virtual Switch"});
                           items.push({id: "vmwaresvs", description: "VMware vNetwork Standard Virtual Switch"});
                           items.push({ id:"nexusdvs" , description: "Cisco Nexus 1000v Distributed Virtual Switch"});

 
                         }
                        args.response.success({data: items});
                        },
                        isHidden:true,
                        dependsOn:'overrideguesttraffic'

                        },

                   vSwitchGuestName:{
                        label:' Guest Traffic vSwitch Name',
                        dependsOn:'overrideguesttraffic',
                        isHidden:true


                     },

                 
                  vsmipaddress: {
                    label: 'Nexus 1000v IP Address',
                    validation: { required: true },
                    isHidden: false
                  },
                  vsmusername: {
                    label: 'Nexus 1000v Username',
                    validation: { required: true },
                    isHidden: false
                  },
                  vsmpassword: {
                    label: 'Nexus 1000v Password',
                    validation: { required: true },
                    isPassword: true,
                    isHidden: false
                  }
                  //hypervisor==VMWare ends here
                }
              },

              action: function(args) {
                var array1 = [];
                array1.push("&zoneId=" + args.data.zoneid);
                array1.push("&hypervisor=" + args.data.hypervisor);

                var clusterType;
                if(args.data.hypervisor == "VMware")
                  clusterType="ExternalManaged";
                else
                  clusterType="CloudManaged";
                array1.push("&clustertype=" + clusterType);

                array1.push("&podId=" + args.data.podId);

                var clusterName = args.data.name;

                if(args.data.cpuovercommit != "" && args.data.cpuovercommit > 0 ){
                   
                     array1.push("&cpuovercommitratio=" + todb(args.data.cpuovercommit));

                   }

                 if(args.data.memoryovercommit != "" && args.data.memoryovercommit > 0)
                    array1.push("&memoryovercommitratio=" + todb(args.data.memoryovercommit));

                if(args.data.hypervisor == "VMware") {
                  array1.push("&username=" + todb(args.data.vCenterUsername));
                  array1.push("&password=" + todb(args.data.vCenterPassword));
            
                  //vSwitch Public Type
                 if(args.data.vSwitchPublicType != "")
                  array1.push("&publicvswitchtype=" + args.data.vSwitchPublicType);

                 if(args.data.vSwitchPublicName != "")
                  array1.push("&publicvswitchname=" +args.data.vSwitchPublicName);

                 
                  //vSwitch Guest Type
                 if(args.data.vSwitchGuestType !=  "")
                  array1.push("&guestvswitchtype=" + args.data.vSwitchGuestType);
                
                 if(args.data.vSwitchGuestName !="")
                  array1.push("&guestvswitchname=" +args.data.vSwitchGuestName);

                  if (args.data.vsmipaddress) {
                    array1.push('&vsmipaddress=' + args.data.vsmipaddress);
                    array1.push('&vsmusername=' + args.data.vsmusername);
                    array1.push('&vsmpassword=' + args.data.vsmpassword);
                  }

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
            viewAll: { path: '_zone.hosts', label: 'label.hosts' },
            isMaximized:true,
            tabFilter: function(args) {
              var vSwichConfigEnabled, vSwitchPresentOnCluster;
              $.ajax({
                url: createURL('listConfigurations'),
                data: { name: 'vmware.use.nexus.vswitch' },
                async: false,
                success: function(json) {
                  vSwichConfigEnabled = json.listconfigurationsresponse.configuration[0].value;
                }
              });

              var hypervisorType = args.context.clusters[0].hypervisortype;
              if(vSwichConfigEnabled != "true" || hypervisorType != 'VMware') {
                return ['nexusVswitch'];
              }
              return [];
            },

            actions: {

               edit: {
                label: 'label.edit',
                action: function(args) {
                  var array1 = [];

                  if (args.data.cpuovercommitratio != "" && args.data.cpuovercommitratio > 0)
                    array1.push("&cpuovercommitratio=" + args.data.cpuovercommitratio);

                  if (args.data.memoryovercommitratio != "" && args.data.memoryovercommitratio > 0)
                    array1.push("&memoryovercommitratio=" + args.data.memoryovercommitratio);

                  $.ajax({

                    url: createURL("updateCluster&id=" + args.context.clusters[0].id + array1.join("")),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var item = json.updateclusterresponse.cluster;
                      args.context.clusters[0].cpuovercommitratio = item.cpuovercommitratio;
                      args.context.clusters[0].memoryovercommitratio = item.memoryovercommitratio;
                      addExtraPropertiesToClusterObject(item);
                      args.response.success({
                        actionFilter: clusterActionfilter,
                        data:item
                       });

                    }
                  });
                }
              },
              
              enable: {
                label: 'label.action.enable.cluster',
                messages: {
                  confirm: function(args) {
                    return 'message.action.enable.cluster';
                  },
                  notification: function(args) {
                    return 'label.action.enable.cluster';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("updateCluster&id=" + args.context.clusters[0].id + "&allocationstate=Enabled"),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var item = json.updateclusterresponse.cluster;
                      args.context.clusters[0].state = item.allocationstate;
											addExtraPropertiesToClusterObject(item);																				
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
                label: 'label.action.disable.cluster',
                messages: {
                  confirm: function(args) {
                    return 'message.action.disable.cluster';
                  },
                  notification: function(args) {
                    return 'label.action.disable.cluster';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("updateCluster&id=" + args.context.clusters[0].id + "&allocationstate=Disabled"),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var item = json.updateclusterresponse.cluster;
                      args.context.clusters[0].state = item.allocationstate;
											addExtraPropertiesToClusterObject(item);												
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
                label: 'label.action.manage.cluster',
                messages: {
                  confirm: function(args) {
                    return 'message.action.manage.cluster';
                  },
                  notification: function(args) {
                    return 'label.action.manage.cluster';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("updateCluster&id=" + args.context.clusters[0].id + "&managedstate=Managed"),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var item = json.updateclusterresponse.cluster;
											addExtraPropertiesToClusterObject(item);
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
                label: 'label.action.unmanage.cluster',
                messages: {
                  confirm: function(args) {
                    return 'message.action.unmanage.cluster';
                  },
                  notification: function(args) {
                    return 'label.action.unmanage.cluster';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("updateCluster&id=" + args.context.clusters[0].id + "&managedstate=Unmanaged"),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var item = json.updateclusterresponse.cluster;
											addExtraPropertiesToClusterObject(item);
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

              'remove': {
                label: 'label.action.delete.cluster' ,
                messages: {
                  confirm: function(args) {
                    return 'message.action.delete.cluster';
                  },
                  notification: function(args) {
                    return 'label.action.delete.cluster';
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
                title: 'label.details',
                fields: [
                  {
                    name: { label: 'label.name' }
                  },
                  {
                    id: { label: 'label.id' },
                    zonename: { label: 'label.zone' },
                    podname: { label: 'label.pod' },
                    hypervisortype: { label: 'label.hypervisor' },
                    clustertype: { label: 'label.cluster.type' },
                    cpuovercommitratio:{ label: 'CPU overcommit Ratio', isEditable:true},
                    memoryovercommitratio:{ label: 'Memory overcommit Ratio', isEditable:true},
                    //allocationstate: { label: 'label.allocation.state' },
                    //managedstate: { label: 'Managed State' },
										state: { label: 'label.state' }
                  }
                ],
                dataProvider: function(args) {
									$.ajax({
										url: createURL("listClusters&id=" + args.context.clusters[0].id),
										dataType: "json",
										success: function(json) {
											var item = json.listclustersresponse.cluster[0];
											addExtraPropertiesToClusterObject(item);
											args.response.success({
												actionFilter: clusterActionfilter,
												data: item
											});
										}
									});
                }
              },
              nexusVswitch: {
                title:'label.nexusVswitch',
                listView: {
                  id: 'vSwitches',
                  fields: {
                    vsmdeviceid: { label: 'label.name' },
                    vsmdevicestate: { label: 'label.state',indicator:{ 'Enabled': 'on' } }
                  },
                  detailView: {
                    actions: {
                      enable: {
                        label: 'label.action.enable.nexusVswitch',
                        messages: {
                          confirm: function(args) {
                            return 'message.action.enable.nexusVswitch';
                          },
                          notification: function(args) {
                            return 'label.action.enable.nexusVswitch';
			                    }
                        },
                        action: function(args) {
                          $.ajax({
                            url: createURL("enableCiscoNexusVSM&id=" + args.context.vSwitches[0].vsmdeviceid),
                            dataType: "json",
                            async: true,
                            success: function(json) {
                              var jid = json.enablecisconexusvsmresponse.jobid;
                              args.response.success(
                                {_custom:
                                  {jobId: jid}
                                }
                              );
                              //args.context.vSwitches[0].vsmdevicestate = item.allocationstate;
                              //addExtraPropertiesToClusterObject(item);
                              args.response.success({
                                actionFilter: nexusActionfilter,
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
                        label: 'label.action.disable.nexusVswitch',
                        messages: {
                          confirm: function(args) {
                            return 'message.action.disable.nexusVswitch';
                          },
                          notification: function(args) {
                            return 'label.action.disable.nexusVswitch';
                          }
                        },
                        action: function(args) {
                          $.ajax({
                            url: createURL("disableCiscoNexusVSM&id=" + args.context.vSwitches[0].vsmdeviceid ),
                            dataType: "json",
                            async: true,
                            success: function(json) {
                              var jid = json.disablecisconexusvsmresponse.jobid; 
                              args.response.success(
                                {_custom:
                                  {jobId: jid}
                                }
                              );
                              //args.context.vSwitches[0].vsmdevicestate = item.allocationstate;
                              //addExtraPropertiesToClusterObject(item);
                              args.response.success({
                                actionFilter: nexusActionfilter,
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
                      }

                      /*   'remove': {
                        label: 'label.action.delete.nexusVswitch' ,
                        messages: {
                          confirm: function(args) {
                            return 'message.action.delete.nexusVswitch';
                          },
                          notification: function(args) {
                            return 'label.action.delete.nexusVswitch';
                          }
                        },
                        action: function(args) {
                          $.ajax({
                            url: createURL("deleteCiscoNexusVSM&id=" + args.context.vSwitches[0].vsmdeviceid),
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
                      }*/
                    },

                    tabs: {
                      details: {
                        title: 'label.details',
                        fields: {
                          vsmdeviceid: { label: 'label.name' },
                          ipaddress: { label: 'label.ipaddress' },
                          vsmctrlvlanid: { label: 'label.vsmctrlvlanid' },
                          vsmpktvlanid: { label: 'label.vsmpktvlanid' },
                          vsmstoragevlanid: { label: 'label.vsmstoragevlanid' },
                          vsmdevicestate: { label: 'label.state', indicator: { 'Enabled': 'on' } }
                        },
                        
                        dataProvider: function(args) {
                          $.ajax({
                            url: createURL("listCiscoNexusVSMs&clusterid=" + args.context.clusters[0].id),
                            dataType: "json",
                            success: function(json) {
                              var item = json.listcisconexusvsmscmdresponse.cisconexusvsm[0];
                              addExtraPropertiesToClusterObject(item);
                              args.response.success({
                                actionFilter: nexusActionfilter,
                                data: item
                              });
                            },
                            error: function(json) {
                              args.response.error(parseXMLHttpResponse(json));
                            }
                          });
                        }
                      }
                    }
                  },

                  dataProvider: function(args) {
                    $.ajax({
                      url: createURL("listCiscoNexusVSMs&clusterid=" + args.context.clusters[0].id),
                      dataType: "json",
                      success: function(json) {
                        var item = json.listcisconexusvsmscmdresponse.cisconexusvsm;
                        args.response.success({
                          actionFilter: nexusActionfilter,
                          data: item
                         
                        });
                      },
                      error: function(json) {
                        // Not generally a real error; means vSwitch still needs setup
                        args.response.success({ data: [] });
                      }
                    });
                  }
                }
              }
            }
          }
        }
      },
      hosts: {
        title: 'label.hosts',
        id: 'hosts',
        listView: {
          section: 'hosts',
          id: 'hosts',
          fields: {
            name: { label: 'label.name' },
            zonename: { label: 'label.zone' },
            podname: { label: 'label.pod' },
            clustername: { label: 'label.cluster' },					
						state: {
							label: 'label.state',							
							indicator: {
								'Up': 'on',
								'Down': 'off',
								'Disconnected': 'off',
								'Alert': 'off',
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

            if (!args.context.instances) {
              array1.push("&zoneid=" + args.context.zones[0].id);
              if("pods" in args.context)
                array1.push("&podid=" + args.context.pods[0].id);
              if("clusters" in args.context)
                array1.push("&clusterid=" + args.context.clusters[0].id);
            } else {
              array1.push("&hostid=" + args.context.instances[0].hostid);
            }

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
              label: 'label.add.host',

              createForm: {
                title: 'label.add.host',
                fields: {
                  zoneid: {
                    docID: 'helpHostZone',
                    label: 'Zone',
                    validation: { required: true },
                    select: function(args) {
                      var data = args.context.zones ?
                        { id: args.context.zones[0].id } : { listAll: true };

                      $.ajax({
                        url: createURL('listZones'),
                        data: data,
                        success: function(json) {
                          var zones = json.listzonesresponse.zone;

                          args.response.success({
                            data: $.map(zones, function(zone) {
                              return {
                                id: zone.id,
                                description: zone.name
                              };
                            })
                          });
                        }
                      });
                    }
                  },

                  //always appear (begin)
                  podId: {
                    label: 'label.pod',
                    docID: 'helpHostPod',
                    validation: { required: true },
                    dependsOn: 'zoneid',
                    select: function(args) {
                      $.ajax({
                        url: createURL("listPods&zoneid=" + args.zoneid),
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
                    label: 'label.cluster',
                    docID: 'helpHostCluster',
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
                    label: 'label.host.name',
                    docID: 'helpHostName',
                    validation: { required: true },
                    isHidden: true
                  },

                  username: {
                    label: 'label.username',
                    docID: 'helpHostUsername',
                    validation: { required: true },
                    isHidden: true
                  },

                  password: {
                    label: 'label.password',
                    docID: 'helpHostPassword',
                    validation: { required: true },
                    isHidden: true,
                    isPassword: true
                  },
                  //input_group="general" ends here

                  //input_group="VMWare" starts here
                  vcenterHost: {
                    label: 'label.esx.host',
                    validation: { required: true },
                    isHidden: true
                  },
                  //input_group="VMWare" ends here

                  //input_group="BareMetal" starts here
                  baremetalCpuCores: {
                    label: 'label.num.cpu.cores',
                    validation: { required: true },
                    isHidden: true
                  },
                  baremetalCpu: {
                    label: 'label.cpu.mhz',
                    validation: { required: true },
                    isHidden: true
                  },
                  baremetalMemory: {
                    label: 'label.memory.mb',
                    validation: { required: true },
                    isHidden: true
                  },
                  baremetalMAC: {
                    label: 'label.host.MAC',
                    validation: { required: true },
                    isHidden: true
                  },
                  //input_group="BareMetal" ends here

                  //input_group="OVM" starts here
                  agentUsername: {
                    label: 'label.agent.username',
                    validation: { required: false },
                    isHidden: true
                  },
                  agentPassword: {
                    label: 'label.agent.password',
                    validation: { required: true },
                    isHidden: true,
                    isPassword: true
                  },
                  //input_group="OVM" ends here

                  //always appear (begin)
                  hosttags: {
                    label: 'label.host.tags',
                    docID: 'helpHostTags',
                    validation: { required: false }
                  }
                  //always appear (end)
                }
              },

              action: function(args) {
                var data = {
								  zoneid: args.data.zoneid,
									podid: args.data.podId,
									clusterid: args.data.clusterId,
									hypervisor: selectedClusterObj.hypervisortype,
									clustertype: selectedClusterObj.clustertype,
									hosttags: args.data.hosttags
								};								               

                if(selectedClusterObj.hypervisortype == "VMware") {
								  $.extend(data,{
									  username: '',
										password: ''										
									});
								                  
                  var hostname = args.data.vcenterHost;
                  var url;
                  if(hostname.indexOf("http://")==-1)
                    url = "http://" + hostname;
                  else
                    url = hostname;
										
									$.extend(data, {
									  url: url
									});	                  
                }
                else {
								  $.extend(data, {
									  username: args.data.username,
										password: args.data.password
									});
								
                  var hostname = args.data.hostname;
                  var url;
                  if(hostname.indexOf("http://")==-1)
                    url = "http://" + hostname;
                  else
                    url = hostname;
										
									$.extend(data, {
                    url: url
                  });									
                 
                  if (selectedClusterObj.hypervisortype == "BareMetal") {
									  $.extend(data, {
										  cpunumber: args.data.baremetalCpuCores,
											cpuspeed: args.data.baremetalCpu,
											memory: args.data.baremetalMemory,
											hostmac: args.data.baremetalMAC
										});									
                  }
                  else if(selectedClusterObj.hypervisortype == "Ovm") {
									  $.extend(data, {
										  agentusername: args.data.agentUsername,
											agentpassword: args.data.agentPassword
										});									
                  }
                }

                $.ajax({
                  url: createURL("addHost"),
                  type: "POST",
									data: data,
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
                  return 'label.add.host';
                }
              }
            }
          },
          detailView: {
            name: "Host details",
						viewAll: {
							label: 'label.instances',
							path: 'instances'
						},
            actions: {
              edit: {
                label: 'label.edit',
                action: function(args) {
                  var array1 = [];
                  array1.push("&hosttags=" + todb(args.data.hosttags));

                  if (args.data.oscategoryid != null && args.data.oscategoryid.length > 0)
                    array1.push("&osCategoryId=" + args.data.oscategoryid);

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
                label: 'label.action.enable.maintenance.mode',
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
                    return 'message.action.host.enable.maintenance.mode';
                  },
                  notification: function(args) {
                    return 'label.action.enable.maintenance.mode';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },

              cancelMaintenanceMode: {
                label: 'label.action.cancel.maintenance.mode' ,
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
                    return 'message.action.cancel.maintenance.mode';
                  },
                  notification: function(args) {
                    return 'label.action.cancel.maintenance.mode';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },

              forceReconnect: {
                label: 'label.action.force.reconnect',
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
                    return 'message.confirm.action.force.reconnect';
                  },
                  notification: function(args) {
                    return 'label.action.force.reconnect';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },

              'remove': {
                label: 'label.action.remove.host' ,
                messages: {
                  notification: function(args) {
                    return 'label.action.remove.host';
                  }
                },
                preFilter: function(args) {
                  if(isAdmin()) {
                    args.$form.find('.form-item[rel=isForced]').css('display', 'inline-block');
                  }
                },
                createForm: {
                  title: 'label.action.remove.host',
                  desc: 'message.action.remove.host',
                  fields: {
                    isForced: {
                      label: 'force.remove',
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
                title: 'label.details',
								
								preFilter: function(args) {								  
                  var hiddenFields = [];
                  $.ajax({
									  url: createURL('listConfigurations&name=ha.tag'),
										dataType: 'json',		
                    async: false,										
										success: function(json) {										  
											if(json.listconfigurationsresponse.configuration == null || json.listconfigurationsresponse.configuration[0].value == null || json.listconfigurationsresponse.configuration[0].value.length == 0) {
											  hiddenFields.push('hahost');
											}
										}
									});														
                  return hiddenFields;
                },
								
                fields: [
                  {
                    name: { label: 'label.name' }
                  },
                  {
                    id: { label: 'label.id' },
                    resourcestate: { label: 'label.resource.state' },
                    state: { label: 'label.state' },
                    type: { label: 'label.type' },                    
                    hosttags: {
                      label: 'label.host.tags',
                      isEditable: true
                    },
										hahost: {
										  label: 'label.ha.enabled',
											converter: cloudStack.converters.toBooleanText
										},
                    oscategoryid: {
                      label: 'label.os.preference',
                      isEditable: true,
                      select: function(args) {
                        $.ajax({
                          url: createURL("listOsCategories"),
                          dataType: "json",
                          async: true,
                          success: function(json) {
                            var oscategoryObjs = json.listoscategoriesresponse.oscategory;
                            var items = [
                              { id: '', description: _l('label.none') }
                            ];
                            $(oscategoryObjs).each(function() {
                              items.push({id: this.id, description: this.name});
                            });														
                            args.response.success({data: items});
                          }
                        });
                      }
                    },
										zonename: { label: 'label.zone' },
                    podname: { label: 'label.pod' },
                    clustername: { label: 'label.cluster' },
                    ipaddress: { label: 'label.ip.address' },
                    version: { label: 'label.version' },
                    disconnected: { label: 'label.last.disconnected' }
                  }
                ],

                dataProvider: function(args) {								  
									$.ajax({
										url: createURL("listHosts&id=" + args.context.hosts[0].id),
										dataType: "json",
										async: true,
										success: function(json) {										  
											var item = json.listhostsresponse.host[0];
											args.response.success({
												actionFilter: hostActionfilter,
												data: item
											});
										}
									});											
                }
              },

							stats: {
								title: 'label.statistics',
								fields: {
									totalCPU: { label: 'label.total.cpu' },
									cpuused: { label: 'label.cpu.utilized' },
									cpuallocated: { label: 'label.cpu.allocated.for.VMs' },
									memorytotal: { label: 'label.memory.total' },
									memoryallocated: { label: 'label.memory.allocated' },
									memoryused: { label: 'label.memory.used' },
									networkkbsread: { label: 'label.network.read' },
									networkkbswrite: { label: 'label.network.write' }
								},
								dataProvider: function(args) {								  
									$.ajax({
										url: createURL("listHosts&id=" + args.context.hosts[0].id),
										dataType: "json",
										async: true,
										success: function(json) {										  
											var jsonObj = json.listhostsresponse.host[0];			                     									
											args.response.success({
												data: {
											    totalCPU: jsonObj.cpunumber + " x " + cloudStack.converters.convertHz(jsonObj.cpuspeed),
											    cpuused: jsonObj.cpuused,				
											    cpuallocated: (jsonObj.cpuallocated == null || jsonObj.cpuallocated == 0)? "N/A": jsonObj.cpuallocated,
											    memorytotal: (jsonObj.memorytotal == null || jsonObj.memorytotal == 0)? "N/A": cloudStack.converters.convertBytes(jsonObj.memorytotal),
											    memoryallocated: (jsonObj.memoryallocated == null || jsonObj.memoryallocated == 0)? "N/A": cloudStack.converters.convertBytes(jsonObj.memoryallocated ),
											    memoryused: (jsonObj.memoryused == null || jsonObj.memoryused == 0)? "N/A": cloudStack.converters.convertBytes(jsonObj.memoryused),												
											    networkkbsread: (jsonObj.networkkbsread == null)? "N/A": cloudStack.converters.convertBytes(jsonObj.networkkbsread * 1024),
											    networkkbswrite: (jsonObj.networkkbswrite == null)? "N/A": cloudStack.converters.convertBytes(jsonObj.networkkbswrite * 1024)
												}
											});		
										}
									});		
								}
							}
            }
          }
        }
      },
      'primary-storage': {
        title: 'label.primary.storage',
        id: 'primarystorages',
        listView: {
          id: 'primarystorages',
          section: 'primary-storage',
          fields: {
            name: { label: 'label.name' },
            ipaddress: { label: 'label.server' },
						path: { label: 'label.path' },
						clustername: { label: 'label.cluster'},
            scope:{label:'Scope'}
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
              label: 'label.add.primary.storage',

              createForm: {
                title: 'label.add.primary.storage',
                fields: {
                  scope: {
                    label: 'label.scope',
                    select: function(args) {
                      var scope = [
                        { id: 'zone', description: _l('label.zone.wide') },
                        { id: 'cluster', description: _l('label.cluster') },
                        { id: 'host', description: _l('label.host') }
                      ];

                      args.response.success({
                        data: scope
                      });

                      args.$select.change(function() {
                        var $form = $(this).closest('form');
                        var scope = $(this).val();

                        if(scope == 'zone'){
                            $form.find('.form-item[rel=podId]').hide();
                            $form.find('.form-item[rel=clusterId]').hide();
                            $form.find('.form-item[rel=hostId]').hide();


                         }

                        else if(scope == 'cluster'){

                             $form.find('.form-item[rel=hostId]').hide();
                             $form.find('.form-item[rel=podId]').css('display', 'inline-block');
                             $form.find('.form-item[rel=clusterId]').css('display', 'inline-block');


                         }

                         else if(scope == 'host'){
                            $form.find('.form-item[rel=podId]').css('display', 'inline-block');
                            $form.find('.form-item[rel=clusterId]').css('display', 'inline-block');
                            $form.find('.form-item[rel=hostId]').css('display', 'inline-block');

                         }

                        })

                    }
                  },
                  zoneid: {
                    label: 'Zone',
                    docID: 'helpPrimaryStorageZone',
                    validation: { required: true },
                    select: function(args) {
                      var data = args.context.zones ?
                      { id: args.context.zones[0].id } : { listAll: true };

                      $.ajax({
                        url: createURL('listZones'),
                        data: data,
                        success: function(json) {
                          var zones = json.listzonesresponse.zone;

                          args.response.success({
                            data: $.map(zones, function(zone) {
                              return {
                                id: zone.id,
                                description: zone.name
                              };
                            })
                          });
                        }
                      });
                    }
                  },
                  podId: {
                    label: 'label.pod',
                    dependsOn: 'zoneid',
                    docID: 'helpPrimaryStoragePod',
                    validation: { required: true },
                    select: function(args) {
                      $.ajax({
                        url: createURL("listPods&zoneid=" + args.zoneid),
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
                    label: 'label.cluster',
                    docID: 'helpPrimaryStorageCluster',
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

                  hostId: {
                    label: 'label.host',
                    validation: { required: true },
                    dependsOn: 'clusterId',
                    select: function(args) {
                      $.ajax({
                        url: createURL('listHosts'),
                        data: {
                          clusterid: args.clusterId
                        },
                        success: function(json) {
                          var hosts = json.listhostsresponse.host ?
                              json.listhostsresponse.host : [] 
                          args.response.success({
                            data: $.map(hosts, function(host) {
                              return { id: host.id, description: host.name }
                            })
                          });
                        }
                      });
                    }
                  },

                  name: {
                    label: 'label.name',
                    docID: 'helpPrimaryStorageName',
                    validation: { required: true }
                  },

                  protocol: {
                    label: 'label.protocol',
                    docID: 'helpPrimaryStorageProtocol',
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
                        items.push({id: "rbd", description: "RBD"});
                        items.push({id: "clvm", description: "CLVM"});
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
                          var $required = $form.find('.form-item[rel=path]').find(".name").find("label span");
                          $form.find('.form-item[rel=path]').find(".name").find("label").text("Path:").prepend($required);

                          //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=iqn]').hide();
                          $form.find('.form-item[rel=lun]').hide();

                          //$('li[input_group="clvm"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=volumegroup]').hide();

                          //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=vCenterDataCenter]').hide();
                          $form.find('.form-item[rel=vCenterDataStore]').hide();

                          $form.find('.form-item[rel=rbdmonitor]').hide();
                          $form.find('.form-item[rel=rbdpool]').hide();
                          $form.find('.form-item[rel=rbdid]').hide();
                          $form.find('.form-item[rel=rbdsecret]').hide();
                        }
                        else if(protocol == "ocfs2") {//ocfs2 is the same as nfs, except no server field.
                          //$dialogAddPool.find("#add_pool_server_container").hide();
                          $form.find('.form-item[rel=server]').hide();
                          //$dialogAddPool.find("#add_pool_nfs_server").val("");
                          $form.find('.form-item[rel=server]').find(".value").find("input").val("");

                          //$('li[input_group="nfs"]', $dialogAddPool).show();
                          $form.find('.form-item[rel=path]').css('display', 'inline-block');
                          //$dialogAddPool.find("#add_pool_path_container").find("label").text(g_dictionary["label.path"]+":");
                          var $required = $form.find('.form-item[rel=path]').find(".name").find("label span");
                          $form.find('.form-item[rel=path]').find(".name").find("label").text("Path:").prepend($required);

                          //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=iqn]').hide();
                          $form.find('.form-item[rel=lun]').hide();

                          //$('li[input_group="clvm"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=volumegroup]').hide();

                          //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=vCenterDataCenter]').hide();
                          $form.find('.form-item[rel=vCenterDataStore]').hide();

                          $form.find('.form-item[rel=rbdmonitor]').hide();
                          $form.find('.form-item[rel=rbdpool]').hide();
                          $form.find('.form-item[rel=rbdid]').hide();
                          $form.find('.form-item[rel=rbdsecret]').hide();
                        }
                        else if(protocol == "PreSetup") {
                          //$dialogAddPool.find("#add_pool_server_container").hide();
                          $form.find('.form-item[rel=server]').hide();
                          //$dialogAddPool.find("#add_pool_nfs_server").val("localhost");
                          $form.find('.form-item[rel=server]').find(".value").find("input").val("localhost");

                          //$('li[input_group="nfs"]', $dialogAddPool).show();
                          $form.find('.form-item[rel=path]').css('display', 'inline-block');
                          //$dialogAddPool.find("#add_pool_path_container").find("label").text(g_dictionary["label.SR.name"]+":");                          
                          var $required = $form.find('.form-item[rel=path]').find(".name").find("label span");
                          $form.find('.form-item[rel=path]').find(".name").find("label").text("SR Name-Label:").prepend($required);

                          //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=iqn]').hide();
                          $form.find('.form-item[rel=lun]').hide();

                          //$('li[input_group="clvm"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=volumegroup]').hide();

                          //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=vCenterDataCenter]').hide();
                          $form.find('.form-item[rel=vCenterDataStore]').hide();

                          $form.find('.form-item[rel=rbdmonitor]').hide();
                          $form.find('.form-item[rel=rbdpool]').hide();
                          $form.find('.form-item[rel=rbdid]').hide();
                          $form.find('.form-item[rel=rbdsecret]').hide();
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

                          //$('li[input_group="clvm"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=volumegroup]').hide();

                          //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=vCenterDataCenter]').hide();
                          $form.find('.form-item[rel=vCenterDataStore]').hide();

                          $form.find('.form-item[rel=rbdmonitor]').hide();
                          $form.find('.form-item[rel=rbdpool]').hide();
                          $form.find('.form-item[rel=rbdid]').hide();
                          $form.find('.form-item[rel=rbdsecret]').hide();
                        }
                        else if($(this).val() == "clvm") {
                          //$("#add_pool_server_container", $dialogAddPool).hide();
                          $form.find('.form-item[rel=server]').hide();
                          //$dialogAddPool.find("#add_pool_nfs_server").val("localhost");
                          $form.find('.form-item[rel=server]').find(".value").find("input").val("localhost");

                          //$('li[input_group="nfs"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=path]').hide();

                          //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=iqn]').hide();
                          $form.find('.form-item[rel=lun]').hide();

                          //$('li[input_group="clvm"]', $dialogAddPool).show();
                          $form.find('.form-item[rel=volumegroup]').css('display', 'inline-block');

                          //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=vCenterDataCenter]').hide();
                          $form.find('.form-item[rel=vCenterDataStore]').hide();

                          $form.find('.form-item[rel=rbdmonitor]').hide();
                          $form.find('.form-item[rel=rbdpool]').hide();
                          $form.find('.form-item[rel=rbdid]').hide();
                          $form.find('.form-item[rel=rbdsecret]').hide();
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

                          //$('li[input_group="clvm"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=volumegroup]').hide();

                          //$('li[input_group="vmfs"]', $dialogAddPool).show();
                          $form.find('.form-item[rel=vCenterDataCenter]').css('display', 'inline-block');
                          $form.find('.form-item[rel=vCenterDataStore]').css('display', 'inline-block');

                          $form.find('.form-item[rel=rbdmonitor]').hide();
                          $form.find('.form-item[rel=rbdpool]').hide();
                          $form.find('.form-item[rel=rbdid]').hide();
                          $form.find('.form-item[rel=rbdsecret]').hide();
                        }
                        else if(protocol == "SharedMountPoint") {  //"SharedMountPoint" show the same fields as "nfs" does.
                          //$dialogAddPool.find("#add_pool_server_container").hide();
                          $form.find('.form-item[rel=server]').hide();
                          //$dialogAddPool.find("#add_pool_nfs_server").val("localhost");
                          $form.find('.form-item[rel=server]').find(".value").find("input").val("localhost");

                          //$('li[input_group="nfs"]', $dialogAddPool).show();
                          $form.find('.form-item[rel=path]').css('display', 'inline-block');
                          var $required = $form.find('.form-item[rel=path]').find(".name").find("label span");
                          $form.find('.form-item[rel=path]').find(".name").find("label").text("Path:").prepend($required);

                          //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=iqn]').hide();
                          $form.find('.form-item[rel=lun]').hide();

                          //$('li[input_group="clvm"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=volumegroup]').hide();

                          //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=vCenterDataCenter]').hide();
                          $form.find('.form-item[rel=vCenterDataStore]').hide();

                          $form.find('.form-item[rel=rbdmonitor]').hide();
                          $form.find('.form-item[rel=rbdpool]').hide();
                          $form.find('.form-item[rel=rbdid]').hide();
                          $form.find('.form-item[rel=rbdsecret]').hide();
                        }
                        else if(protocol == "rbd") {
                          $form.find('.form-item[rel=rbdmonitor]').css('display', 'inline-block');
                          $form.find('.form-item[rel=rbdmonitor]').find(".name").find("label").text("RADOS Monitor:");

                          $form.find('.form-item[rel=rbdpool]').css('display', 'inline-block');
                          $form.find('.form-item[rel=rbdpool]').find(".name").find("label").text("RADOS Pool:");

                          $form.find('.form-item[rel=rbdid]').css('display', 'inline-block');
                          $form.find('.form-item[rel=rbdid]').find(".name").find("label").text("RADOS User:");

                          $form.find('.form-item[rel=rbdsecret]').css('display', 'inline-block');
                          $form.find('.form-item[rel=rbdsecret]').find(".name").find("label").text("RADOS Secret:");

                          $form.find('.form-item[rel=server]').hide();
                          $form.find('.form-item[rel=iqn]').hide();
                          $form.find('.form-item[rel=lun]').hide();
                          $form.find('.form-item[rel=volumegroup]').hide();
                          $form.find('.form-item[rel=path]').hide();
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

                          //$('li[input_group="clvm"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=volumegroup]').hide();

                          //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                          $form.find('.form-item[rel=vCenterDataCenter]').hide();
                          $form.find('.form-item[rel=vCenterDataStore]').hide();

                          $form.find('.form-item[rel=rbdmonitor]').hide();
                          $form.find('.form-item[rel=rbdpool]').hide();
                          $form.find('.form-item[rel=rbdid]').hide();
                          $form.find('.form-item[rel=rbdsecret]').hide();
                        }
                      });

                      args.$select.trigger("change");
                    }
                  },
                  //always appear (end)

                  server: {
                    label: 'label.server',
                    docID: 'helpPrimaryStorageServer',
                    validation: { required: true },
                    isHidden: true
                  },

                  //nfs
                  path: {
                    label: 'label.path',
                    docID: 'helpPrimaryStoragePath',
                    validation: { required: true },
                    isHidden: true
                  },

                  //iscsi
                  iqn: {
                    label: 'label.target.iqn',
                    docID: 'helpPrimaryStorageTargetIQN',
                    validation: { required: true },
                    isHidden: true
                  },
                  lun: {
                    label: 'label.LUN.number',
                    docID: 'helpPrimaryStorageLun',
                    validation: { required: true },
                    isHidden: true
                  },

									//clvm
									volumegroup: {
                    label: 'label.volgroup',
                    validation: { required: true },
                    isHidden: true
                  },

                  //vmfs
                  vCenterDataCenter: {
                    label: 'label.vcenter.datacenter',
                    validation: { required: true },
                    isHidden: true
                  },
                  vCenterDataStore: {
                    label: 'label.vcenter.datastore',
                    validation: { required: true },
                    isHidden: true
                  },

                  // RBD
                  rbdmonitor: {
                    label: 'label.rbd.monitor',
                    validation: { required: true },
                    isHidden: true
                  },
                  rbdpool: {
                    label: 'label.rbd.pool',
                    validation: { required: true },
                    isHidden: true
                  },
                   rbdid: {
                    label: 'label.rbd.id',
                    validation: { required: false },
                    isHidden: true
                  },
                   rbdsecret: {
                    label: 'label.rbd.secret',
                    validation: { required: false },
                    isHidden: true
                  },

                  //always appear (begin)
                  storageTags: {
                    label: 'label.storage.tags',
                    docID: 'helpPrimaryStorageTags',
                    validation: { required: false }
                  }
                  //always appear (end)
                }
              },

              action: function(args) {
                var array1 = [];
                array1.push("&scope=" + todb(args.data.scope));

                array1.push("&zoneid=" + args.data.zoneid);
            
               if(args.data.scope == 'cluster'){

                array1.push("&podid=" + args.data.podId);
                array1.push("&clusterid=" + args.data.clusterId);

               }

                if(args.data.scope == 'host'){
                array1.push("&podid=" + args.data.podId);
                array1.push("&clusterid=" + args.data.clusterId);
                array1.push("&hostid=" + args.data.hostId);

               }

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
								else if (args.data.protocol == "clvm") {
									//var vg = trim($thisDialog.find("#add_pool_clvm_vg").val());
									var vg = args.data.volumegroup;

									if(vg.substring(0,1) != "/")
                    vg = "/" + vg;
									url = clvmURL(vg);
								}
                else if (args.data.protocol == "rbd") {
                  var rbdmonitor = args.data.rbdmonitor;
                  var rbdpool = args.data.rbdpool;
                  var rbdid = args.data.rbdid;
                  var rbdsecret = args.data.rbdsecret;

                  url = rbdURL(rbdmonitor, rbdpool, rbdid, rbdsecret);
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
                  return 'label.add.primary.storage';
                }
              }
            }
          },

          detailView: {
            name: "Primary storage details",
            actions: {
							edit: {
                label: 'label.edit',
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
                label: 'label.action.enable.maintenance.mode' ,
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
                    return 'message.action.primarystorage.enable.maintenance.mode';
                  },
                  notification: function(args) {
                    return 'label.action.enable.maintenance.mode';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },

              cancelMaintenanceMode: {
                label: 'label.action.cancel.maintenance.mode' ,
								messages: {
                  confirm: function(args) {
                    return 'message.action.cancel.maintenance.mode';
                  },
                  notification: function(args) {
                    return 'label.action.cancel.maintenance.mode';
                  }
                },
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
                notification: {
                  poll: pollAsyncJobResult
                }
              },

              'remove': {
                label: 'label.action.delete.primary.storage' ,
                messages: {                  
                  notification: function(args) {
                    return 'label.action.delete.primary.storage';
                  }
                },		
                createForm: {
                  title: 'label.action.delete.primary.storage',                 
                  fields: {
                    isForced: {
                      label: 'force.remove',
                      isBoolean: true                      
                    }
                  }
                },									
                action: function(args) {								
								  var array1 = [];                  
                  array1.push("&forced=" + (args.data.isForced == "on"));								
                  $.ajax({
                    url: createURL("deleteStoragePool&id=" + args.context.primarystorages[0].id + array1.join("")),
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
                title: 'label.details',
                fields: [
                  {
                    name: { label: 'label.name' }
                  },
                  {
                    id: { label: 'label.id' },
                    state: { label: 'label.state' },
										tags: {
										  label: 'label.storage.tags',
											isEditable: true
										},
										podname: { label: 'label.pod' },
                    clustername: { label: 'label.cluster' },
                    type: { label: 'label.type' },
                    ipaddress: { label: 'label.ip.address' },
                    path: { label: 'label.path' },
                    disksizetotal: {
                      label: 'label.disk.total',
                      converter: function(args) {
                        if (args == null || args == 0)
                          return "";
                        else
                          return cloudStack.converters.convertBytes(args);
                      }
                    },
                    disksizeallocated: {
                      label: 'label.disk.allocated',
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
									$.ajax({
										url: createURL("listStoragePools&id=" + args.context.primarystorages[0].id),
										dataType: "json",
										async: true,
										success: function(json) {										  
											var item = json.liststoragepoolsresponse.storagepool[0];
											args.response.success({
												actionFilter: primarystorageActionfilter,
												data:item
											});
										}
									});														
                }
              }
            }
          }
        }
      },

      'secondary-storage': {
        title: 'label.secondary.storage',
        id: 'secondarystorages',
        listView: {
          id: 'secondarystorages',
          section: 'seconary-storage',
          fields: {
            name: { label: 'label.name' },
						created: { label: 'label.created', converter: cloudStack.converters.toLocalDate },
            resourcestate: {
              label: 'label.state',
              indicator: {
                'Enabled': 'on',
                'Disabled': 'off',
                'Destroyed': 'off'
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
              label: 'label.add.secondary.storage',

              createForm: {
                title: 'label.add.secondary.storage',
                fields: {
                  zoneid: {
                    label: 'Zone',
                    docID: 'helpSecondaryStorageZone',
                    validation: { required: true },
                    select: function(args) {
                      var data = args.context.zones ?
                      { id: args.context.zones[0].id } : { listAll: true };

                      $.ajax({
                        url: createURL('listZones'),
                        data: data,
                        success: function(json) {
                          var zones = json.listzonesresponse.zone;

                          args.response.success({
                            data: $.map(zones, function(zone) {
                              return {
                                id: zone.id,
                                description: zone.name
                              };
                            })
                          });
                        }
                      });
                    }
                  },
                  nfsServer: {
                    label: 'label.nfs.server',
                    docID: 'helpSecondaryStorageNFSServer',
                    validation: { required: true }
                  },
                  path: {
                    label: 'label.path',
                    docID: 'helpSecondaryStoragePath',
                    validation: { required: true }
                  }
                }
              },

              action: function(args) {
                var zoneId = args.data.zoneid;
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
                  return 'label.add.secondary.storage';
                }
              }
            }
          },

          detailView: {
            name: 'Secondary storage details',
            actions: {
              remove: {
                label: 'label.action.delete.secondary.storage' ,
                messages: {
                  confirm: function(args) {
                    return 'message.action.delete.secondary.storage';
                  },
                  notification: function(args) {
                    return 'label.action.delete.secondary.storage';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteHost&id=" + args.context.secondarystorages[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      args.response.success();
                    }
                  });
                },
                notification: {
                  poll: function(args) { args.complete({ data: { resourcestate: 'Destroyed' } }); }
                }
              }

            },
            tabs: {
              details: {
                title: 'label.details',
                fields: [
                  {
                    name: { label: 'label.name' }
                  },
                  {
                    id: { label: 'label.id' },
                    zonename: { label: 'label.zone' },
                    created: { label: 'label.created', converter: cloudStack.converters.toLocalDate }
                  }
                ],

                dataProvider: function(args) {								  
									$.ajax({
										url: createURL("listHosts&type=SecondaryStorage&id=" + args.context.secondarystorages[0].id),
										dataType: "json",
										async: true,
										success: function(json) {										  
											var item = json.listhostsresponse.host[0];
											args.response.success({
												actionFilter: secondarystorageActionfilter,
												data:item
											});
										}
									});										
                }
              }
            }
          }
        }
      },

      guestIpRanges: { //Advanced zone - Guest traffic type - Network tab - Network detailView - View IP Ranges
        title: 'label.guest.ip.range',
        id: 'guestIpRanges',
        listView: {
          section: 'guest-IP-range',
          fields: {
            startip: { label: 'IPv4 Start IP' },
            endip: { label: 'IPv4 End IP' },
            startipv6: { label: 'IPv6 Start IP' },
            endipv6: { label: 'IPv6 End IP' }
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
              label: 'label.add.ip.range',
              createForm: {
                title: 'label.add.ip.range',
                fields: {
                  startipv4: { label: 'IPv4 Start IP' },
                  endipv4: { label: 'IPv4 End IP' },
                  startipv6: { label: 'IPv6 Start IP' },
                  endipv6: { label: 'IPv6 End IP' }
                }
              },
              action: function(args) {
                var array2 = [];
                if(args.data.startipv4 != null && args.data.startipv4.length > 0)
                  array2.push("&startip=" + args.data.startipv4);                
                if(args.data.endipv4 != null && args.data.endipv4.length > 0)
                  array2.push("&endip=" + args.data.endipv4);
                
                if(args.data.startipv6 != null && args.data.startipv6.length > 0)
                    array2.push("&startipv6=" + args.data.startipv6);                
                  if(args.data.endipv6 != null && args.data.endipv6.length > 0)
                    array2.push("&endipv6=" + args.data.endipv6);
                
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
                  return 'label.add.ip.range';
                }
              }
            },

            'remove': {
              label: 'label.remove.ip.range' ,
              messages: {
                confirm: function(args) {
                  return 'message.confirm.remove.IP.range';
                },
                notification: function(args) {
                  return 'label.remove.ip.range';
                }
              },
              action: function(args) {
                $.ajax({
                  url: createURL("deleteVlanIpRange&id=" + args.data.id),
                  dataType: "json",
                  async: true,
                  success: function(json) {
                    args.response.success({data:{}});
                  },
                  error: function(json) {
                    args.response.error(parseXMLHttpResponse(json));
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

  function addNiciraNvpDevice(args, physicalNetworkObj, apiCmd, apiCmdRes, apiCmdObj) {
    var array1 = [];
    array1.push("&physicalnetworkid=" + physicalNetworkObj.id);
    array1.push("&username=" + todb(args.data.username));
    array1.push("&password=" + todb(args.data.password));
    array1.push("&hostname=" + todb(args.data.host));
    array1.push("&transportzoneuuid=" + todb(args.data.transportzoneuuid));

    var l3GatewayServiceUuid = args.data.l3gatewayserviceuuid;
    if(l3GatewayServiceUuid != null && l3GatewayServiceUuid.length > 0) {
        array1.push("&l3gatewayserviceuuid=" + todb(args.data.l3gatewayserviceuuid));
    }
    
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


	var afterCreateZonePhysicalNetworkTrafficTypes = function(args, newZoneObj, newPhysicalnetwork) {
		$.ajax({
			url: createURL("updatePhysicalNetwork&state=Enabled&id=" + newPhysicalnetwork.id),
			dataType: "json",
			success: function(json) {
				var jobId = json.updatephysicalnetworkresponse.jobid;
				var enablePhysicalNetworkIntervalID = setInterval(function() { 	
					$.ajax({
						url: createURL("queryAsyncJobResult&jobId="+jobId),
						dataType: "json",
						success: function(json) {
							var result = json.queryasyncjobresultresponse;
							if (result.jobstatus == 0) {
								return; //Job has not completed
							}
							else {
								clearInterval(enablePhysicalNetworkIntervalID); 
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
											var enableVirtualRouterElementIntervalID = setInterval(function() { 	
												$.ajax({
													url: createURL("queryAsyncJobResult&jobId="+jobId),
													dataType: "json",
													success: function(json) {
														var result = json.queryasyncjobresultresponse;
														if (result.jobstatus == 0) {
															return; //Job has not completed
														}
														else {
															clearInterval(enableVirtualRouterElementIntervalID); 
															if (result.jobstatus == 1) {
																//alert("configureVirtualRouterElement succeeded.");

																$.ajax({
																	url: createURL("updateNetworkServiceProvider&state=Enabled&id=" + virtualRouterProviderId),
																	dataType: "json",
																	async: false,
																	success: function(json) {
																		var jobId = json.updatenetworkserviceproviderresponse.jobid;
																		var enableVirtualRouterProviderIntervalID = setInterval(function() { 	
																			$.ajax({
																				url: createURL("queryAsyncJobResult&jobId="+jobId),
																				dataType: "json",
																				success: function(json) {
																					var result = json.queryasyncjobresultresponse;
																					if (result.jobstatus == 0) {
																						return; //Job has not completed
																					}
																					else {
																						clearInterval(enableVirtualRouterProviderIntervalID); 
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
																											var enableSecurityGroupProviderIntervalID = setInterval(function() { 	
																												$.ajax({
																													url: createURL("queryAsyncJobResult&jobId="+jobId),
																													dataType: "json",
																													success: function(json) {
																														var result = json.queryasyncjobresultresponse;
																														if (result.jobstatus == 0) {
																															return; //Job has not completed
																														}
																														else {
																															clearInterval(enableSecurityGroupProviderIntervalID); 
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
																																alert("failed to enable security group provider. Error: " + _s(result.jobresult.errortext));
																															}
																														}
																													},
																													error: function(XMLHttpResponse) {
																														var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
																														alert("updateNetworkServiceProvider failed. Error: " + errorMsg);
																													}
																												});
																											}, g_queryAsyncJobResultInterval); 		
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
																							alert("failed to enable Virtual Router Provider. Error: " + _s(result.jobresult.errortext));
																						}
																					}
																				},
																				error: function(XMLHttpResponse) {
																					var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
																					alert("updateNetworkServiceProvider failed. Error: " + errorMsg);
																				}
																			});
																		}, g_queryAsyncJobResultInterval); 		
																	}
																});
															}
															else if (result.jobstatus == 2) {
																alert("configureVirtualRouterElement failed. Error: " + _s(result.jobresult.errortext));
															}
														}
													},
													error: function(XMLHttpResponse) {
														var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
														alert("configureVirtualRouterElement failed. Error: " + errorMsg);
													}
												});
											}, g_queryAsyncJobResultInterval); 		
										}
									});
								}
								else if (result.jobstatus == 2) {
									alert("updatePhysicalNetwork failed. Error: " + _s(result.jobresult.errortext));
								}
							}
						},
						error: function(XMLHttpResponse) {
							var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
							alert("updatePhysicalNetwork failed. Error: " + errorMsg);
						}
					});
				}, g_queryAsyncJobResultInterval); 		
			}
		});
	};

  //action filters (begin)
  var zoneActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = ['enableSwift'];
    allowedActions.push("edit");
    if(jsonObj.allocationstate == "Disabled")
      allowedActions.push("enable");
    else if(jsonObj.allocationstate == "Enabled")
      allowedActions.push("disable");
    allowedActions.push("remove");
    return allowedActions;
  }
   

   var nexusActionfilter = function(args) {
    var nexusObj = args.context.item;
    var allowedActions = [ ];
    allowedActions.push("edit");
    if(nexusObj.vsmdevicestate == "Disabled")
      allowedActions.push("enable");
    else if(nexusObj.vsmdevicestate == "Enabled")
      allowedActions.push("disable");
    allowedActions.push("remove");
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
    allowedActions.push("remove");

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
      allowedActions.push("edit");

		}
		else if(jsonObj.state == "Disabled") { //managed, allocation disabled
		  allowedActions.push("unmanage");
      allowedActions.push("enable");
      allowedActions.push("edit");

		}
		else { //Unmanaged, PrepareUnmanaged , PrepareUnmanagedError
			allowedActions.push("manage");
		}

    allowedActions.push("remove");

    return allowedActions;
  }

  var hostActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];

    if (jsonObj.resourcestate == "Enabled") {
      allowedActions.push("edit");
      allowedActions.push("enableMaintenanceMode");
      
			if(jsonObj.state != "Disconnected")
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
      allowedActions.push("remove");
    }
    else if (jsonObj.resourcestate == "Disabled"){
      allowedActions.push("edit");
      allowedActions.push("remove");
    }

		if((jsonObj.state == "Down" || jsonObj.state == "Alert" || jsonObj.state == "Disconnected") && ($.inArray("remove", allowedActions) == -1)) {
		  allowedActions.push("remove");
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
      allowedActions.push("remove");
    }
    else if(jsonObj.state == "Alert") {
      allowedActions.push("remove");
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
      allowedActions.push("remove");
    }
    else if (jsonObj.state == "Disconnected"){
      allowedActions.push("remove");
    }
    return allowedActions;
  }

  var secondarystorageActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];
    allowedActions.push("remove");
    return allowedActions;
  }

  var routerActionfilter = cloudStack.sections.system.routerActionFilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];

    if (jsonObj.state == 'Running') {
      allowedActions.push("stop");
      			
		//	if(jsonObj.vpcid != null) 
        allowedActions.push("restart");
				
      allowedActions.push("viewConsole");
      if (isAdmin())
        allowedActions.push("migrate");
    }
    else if (jsonObj.state == 'Stopped') {
      allowedActions.push("start");
	    allowedActions.push("remove");
			
      if(jsonObj.vpcid != null)
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
      allowedActions.push("remove");  
      allowedActions.push("viewConsole");
      if (isAdmin())
        allowedActions.push("migrate");
    }
    else if (jsonObj.state == 'Stopped') {
      allowedActions.push("start");
			allowedActions.push("changeService");
      allowedActions.push("remove");
    }
    else if (jsonObj.state == 'Error') {
      allowedActions.push("remove");
    }
    return allowedActions;
  }
  //action filters (end)

  var networkProviderActionFilter = function(id) {
    return function(args) {
      var allowedActions = [];
      var jsonObj = nspMap[id] ?
        nspMap[id] : {};

      if (jsonObj.state) {
        if (jsonObj.state == "Enabled")
          allowedActions.push("disable");
        else if (jsonObj.state == "Disabled")
          allowedActions.push("enable");
        allowedActions.push("destroy");
      }

      allowedActions.push('add');

      return allowedActions;
    }
  };

	var addExtraPropertiesToClusterObject = function(jsonObj) {
		if(jsonObj.managedstate == "Managed") {
			jsonObj.state = jsonObj.allocationstate; //jsonObj.state == Enabled, Disabled
		}
		else {
			jsonObj.state = jsonObj.managedstate; //jsonObj.state == Unmanaged, PrepareUnmanaged, PrepareUnmanagedError
		}
  }
	
	var addExtraPropertiesToRouterInstanceObject = function(jsonObj) {  		
		if(jsonObj.isredundantrouter == true)
			jsonObj["redundantRouterState"] = jsonObj.redundantstate;
		else
			jsonObj["redundantRouterState"] = "";				
  }	
	
	var refreshNspData = function(nspName) {	  
		var array1 = [];
		if(nspName != null)
		  array1.push("&name=" + nspName);
		
		$.ajax({
			url: createURL("listNetworkServiceProviders&physicalnetworkid=" + selectedPhysicalNetworkObj.id + array1.join("")),
			dataType: "json",
			async: false,
			success: function(json) {
			  nspMap = {}; //reset 
			
				var items = json.listnetworkserviceprovidersresponse.networkserviceprovider;	        
        if(items != null) {				
					for(var i = 0; i < items.length; i++) {
						switch(items[i].name) {
							case "VirtualRouter":
								nspMap["virtualRouter"] = items[i];
								break;
							case "VpcVirtualRouter":
							  nspMap["vpcVirtualRouter"] = items[i];
							  break;
							case "Netscaler":
								nspMap["netscaler"] = items[i];
								break;
							case "F5BigIp":
								nspMap["f5"] = items[i];
								break;
							case "JuniperSRX":
								nspMap["srx"] = items[i];
								break;
							case "SecurityGroupProvider":
								nspMap["securityGroups"] = items[i];
								break;
                            case "NiciraNvp":
                                nspMap["niciraNvp"] = items[i];
                                break;
						}
					}
				}
			}
		});
   
		nspHardcodingArray = [
			{
				id: 'netscaler',
				name: 'NetScaler',
				state: nspMap.netscaler? nspMap.netscaler.state : 'Disabled'
			},
			{
				id: 'virtualRouter',
				name: 'Virtual Router',
				state: nspMap.virtualRouter ? nspMap.virtualRouter.state : 'Disabled'
			},
            {
                id: 'niciraNvp',
                name: 'Nicira Nvp',
                state: nspMap.niciraNvp ? nspMap.niciraNvp.state : 'Disabled'
            }
		];

		if(selectedZoneObj.networktype == "Basic") {
			nspHardcodingArray.push(
				{
					id: 'securityGroups',
					name: 'Security Groups',
					state: nspMap.securityGroups ? nspMap.securityGroups.state : 'Disabled'
				}
			);
		}
		else if(selectedZoneObj.networktype == "Advanced"){
		  nspHardcodingArray.push(
				{
					id: 'vpcVirtualRouter',
					name: 'VPC Virtual Router',
					state: nspMap.vpcVirtualRouter ? nspMap.vpcVirtualRouter.state : 'Disabled'
				}
			);		
			nspHardcodingArray.push(
				{
					id: 'f5',
					name: 'F5',
					state: nspMap.f5 ? nspMap.f5.state : 'Disabled'
				}
			);
			nspHardcodingArray.push(
				{
					id: 'srx',
					name: 'SRX',
					state: nspMap.srx ? nspMap.srx.state : 'Disabled'
				}
			);
		}
	};

	cloudStack.actionFilter.physicalNetwork = function(args) {
    var state = args.context.item.state;

    if (state != 'Destroyed') {
      return ['remove'];
    }
    
    return [];
  };
})($, cloudStack);
