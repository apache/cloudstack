// Copyright 2012 Citrix Systems, Inc. Licensed under the
// Apache License, Version 2.0 (the "License"); you may not use this
// file except in compliance with the License.  Citrix Systems, Inc.
// reserves all rights not expressly granted by the License.
// You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License. 
(function($, cloudStack) {
  var aclMultiEdit = {   
		noSelect: true,
		fields: {
			'cidrlist': { edit: true, label: 'label.cidr.list' },
			'protocol': {
				label: 'label.protocol',
				select: function(args) {
					args.$select.change(function() {
						var $inputs = args.$form.find('input');
						var $icmpFields = $inputs.filter(function() {
							var name = $(this).attr('name');

							return $.inArray(name, [
								'icmptype',
								'icmpcode'
							]) > -1;
						});
						var $otherFields = $inputs.filter(function() {
							var name = $(this).attr('name');

							return name != 'icmptype' && name != 'icmpcode' && name != 'cidrlist';
						});

						if ($(this).val() == 'icmp') {
							$icmpFields.show();
							$icmpFields.attr('disabled', false);
							$otherFields.attr('disabled', 'disabled');
							$otherFields.hide();
							$otherFields.parent().find('label.error').hide();
						} else {
							$otherFields.show();
							$otherFields.parent().find('label.error').hide();
							$otherFields.attr('disabled', false);
							$icmpFields.attr('disabled', 'disabled');
							$icmpFields.hide();
							$icmpFields.parent().find('label.error').hide();
						}
					});

					args.response.success({
						data: [
							{ name: 'tcp', description: 'TCP' },
							{ name: 'udp', description: 'UDP' },
							{ name: 'icmp', description: 'ICMP' }
						]
					});
				}
			},
			'startport': { edit: true, label: 'label.start.port' },
			'endport': { edit: true, label: 'label.end.port' },
			'icmptype': { edit: true, label: 'ICMP.type', isDisabled: true },
			'icmpcode': { edit: true, label: 'ICMP.code', isDisabled: true },
			'add-rule': {
				label: 'label.add.rule',
				addButton: true
			}
		},  
    add: {
      label: 'Add ACL',
      action: function(args) {        
				$.ajax({
					url: createURL('createNetworkACL'),
					data: $.extend(args.data, {
						networkid: args.context.tiers[0].id
					}),
					dataType: 'json',
					success: function(data) {
						args.response.success({
							_custom: {
								jobId: data.createnetworkaclresponse.jobid
							},
							notification: {
								label: 'Add ACL',
								poll: pollAsyncJobResult
							}
						});
					},
					error: function(data) {
						args.response.error(parseXMLHttpResponse(data));
					}
				});
      }
    },
    actions: {
      destroy: {
        label: 'Remove ACL',
        action: function(args) {     
					$.ajax({
						url: createURL('deleteNetworkACL'),
						data: {
							id: args.context.multiRule[0].id
						},
						dataType: 'json',
						async: true,
						success: function(data) {
							var jobID = data.deletenetworkaclresponse.jobid;
							args.response.success({
								_custom: {
									jobId: jobID
								},
								notification: {
									label: 'Remove ACL',
									poll: pollAsyncJobResult
								}
							});
						},
						error: function(data) {
							args.response.error(parseXMLHttpResponse(data));
						}
					});					
        }
      }
    },
    dataProvider: function(args) {						
			$.ajax({
				url: createURL('listNetworkACLs'),
				data: {
					listAll: true,
					networkid: args.context.tiers[0].id
				},
				dataType: 'json',
				async: true,
				success: function(json) {					
					args.response.success({
						data: json.listnetworkaclsresponse.networkacl
					});
				},
				error: function(XMLHttpResponse) {
					args.response.error(parseXMLHttpResponse(XMLHttpResponse));
				}
			});						
    }
  };
  
  cloudStack.vpc = {
    vmListView: {
      id: 'vpcTierInstances',
      listView: {
        filters: {
          mine: { label: 'My instances' },
          all: { label: 'All instances' },
          running: { label: 'Running instances' },
          destroyed: { label: 'Destroyed instances' }
        },
        fields: {
          name: { label: 'Name', editable: true },
          account: { label: 'Account' },
          zonename: { label: 'Zone' },
          state: {
            label: 'Status',
            indicator: {
              'Running': 'on',
              'Stopped': 'off',
              'Destroyed': 'off'
            }
          }
        },

        // List view actions
        actions: {          
					start: {
						label: 'label.action.start.instance' ,
						action: function(args) {						  
							$.ajax({
								url: createURL("startVirtualMachine&id=" + args.context.vpcTierInstances[0].id),
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
												return cloudStack.actionFilter.vmActionFilter;
											}
										 }
										}
									);
								}
							});
						},
						messages: {
							confirm: function(args) {
								return 'message.action.start.instance';
							},
							notification: function(args) {
								return 'label.action.start.instance';
							},
							complete: function(args) {						  
								if(args.password != null) {
									alert('Password of the VM is ' + args.password);
								}
								return 'label.action.start.instance';
							}			
						},
						notification: {
							poll: pollAsyncJobResult
						}
					},
					stop: {
						label: 'label.action.stop.instance',
						addRow: 'false',
						createForm: {
							title: 'label.action.stop.instance',
							desc: 'message.action.stop.instance',
							fields: {
								forced: {
									label: 'force.stop',
									isBoolean: true,
									isChecked: false
								}
							}
						},
						action: function(args) {
							var array1 = [];
							array1.push("&forced=" + (args.data.forced == "on"));
							$.ajax({
								url: createURL("stopVirtualMachine&id=" + args.context.vpcTierInstances[0].id + array1.join("")),
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
												return cloudStack.actionFilter.vmActionFilter;
											}
										 }
										}
									);
								}
							});
						},
						messages: {
							confirm: function(args) {
								return 'message.action.stop.instance';
							},

							notification: function(args) {
								return 'label.action.stop.instance';
							}
						},
						notification: {
							poll: pollAsyncJobResult
						}
					},
					restart: {
						label: 'instances.actions.reboot.label',
						action: function(args) {
							$.ajax({
								url: createURL("rebootVirtualMachine&id=" + args.context.vpcTierInstances[0].id),
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
												return cloudStack.actionFilter.vmActionFilter;
											}
										 }
										}
									);
								}
							});
						},
						messages: {
							confirm: function(args) {
								return 'message.action.reboot.instance';
							},
							notification: function(args) {
								return 'instances.actions.reboot.label';
							}
						},
						notification: {
							poll: pollAsyncJobResult
						}
					},
					destroy: {
						label: 'label.action.destroy.instance',
						messages: {
							confirm: function(args) {
								return 'message.action.destroy.instance';
							},            
							notification: function(args) {
								return 'label.action.destroy.instance';
							}
						},
						action: function(args) {
							$.ajax({
								url: createURL("destroyVirtualMachine&id=" + args.context.vpcTierInstances[0].id),
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
												return cloudStack.actionFilter.vmActionFilter;
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
						label: 'label.action.restore.instance',
						messages: {
							confirm: function(args) {
								return 'message.action.restore.instance';
							},
							notification: function(args) {
								return 'label.action.restore.instance';
							}
						},					
						action: function(args) {
							$.ajax({
								url: createURL("recoverVirtualMachine&id=" + args.context.vpcTierInstances[0].id),
								dataType: "json",
								async: true,
								success: function(json) {
									var item = json.recovervirtualmachineresponse.virtualmachine;
									args.response.success({data:item});
								}
							});
						}
					}					
        },
        dataProvider: function(args) {	          
					var array1 = [];
					if(args.filterBy != null) {
						if(args.filterBy.kind != null) {
							switch(args.filterBy.kind) {
							case "all":
								array1.push("&listAll=true");
								break;
							case "mine":
								if (!args.context.projects) array1.push("&domainid=" + g_domainid + "&account=" + g_account);
								break;
							case "running":
								array1.push("&listAll=true&state=Running");
								break;
							case "stopped":
								array1.push("&listAll=true&state=Stopped");
								break;
							case "destroyed":
								array1.push("&listAll=true&state=Destroyed");
								break;
							}
						}
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
            url: createURL('listVirtualMachines' + array1.join("")),
						data: {
						  networkid: args.context.tiers[0].id
						},
            success: function(json) {
              args.response.success({ 
							  data: json.listvirtualmachinesresponse.virtualmachine,
								actionFilter: cloudStack.actionFilter.vmActionFilter
							});
            }
          });
        }
      }
    },
		ipAddresses: {		 
		  listView: function() {
		    return cloudStack.sections.network.sections.ipAddresses;
			}
		},
		gateways: {
		
		},
    siteToSiteVPN: {
      listView: function() { 
        return cloudStack.sections.network.sections.siteToSiteVpn;
      }
    },
    tiers: {
      actionPreFilter: function(args) {
        var tier = args.context.tiers[0];
        var state = tier.state;

        return state == 'Running' ? ['start'] : ['stop'];
      },
      actions: {        
        add: {
					label: 'Add new tier',     
					createForm: {
						title: 'Add new tier',               
						fields: {
							name: { label: 'label.name', 
								validation: { required: true } 
							},                                
							networkOfferingId: {
								label: 'label.network.offering',
								validation: { required: true },
								dependsOn: 'zoneId',
								select: function(args) {										 
									$.ajax({
										url: createURL('listNetworkOfferings'),
										data: {
											forvpc: true,
											zoneid: args.zoneId,
											guestiptype: 'Isolated',
											supportedServices: 'SourceNat',
											specifyvlan: false,
											state: 'Enabled'
										},
										success: function(json) {
											var networkOfferings = json.listnetworkofferingsresponse.networkoffering;
											args.response.success({
												data: $.map(networkOfferings, function(zone) {
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
							gateway: { 
								label: 'label.gateway', 
								validation: { required: true } 
							},        
							netmask: { 
								label: 'label.netmask', 
								validation: { required: true } 
							},        
						}
					},
					action: function(args) {	
						var dataObj = {
						  vpcid: args.context.vpc[0].id,
							zoneId: args.context.vpc[0].zoneid,
							networkOfferingId: args.data.networkOfferingId,
							name: args.data.name,
							displayText: args.data.name,
							gateway: args.data.gateway, 
							netmask: args.data.netmask
						};
					
						$.ajax({
							url: createURL('createNetwork'),
							dataType: 'json',
							data: dataObj,
							success: function(json) {		                				
								args.response.success({
									data: json.createnetworkresponse.network
								});
							},
							error: function(XMLHttpResponse) {                				
								args.response.error(parseXMLHttpResponse(XMLHttpResponse));
							}
						});
					},
					messages: {
						notification: function() { return 'Add new tier'; }
					}
				},
				
				/*
        start: {
          label: 'Start tier',
          shortLabel: 'Start',
          action: function(args) {
            args.response.success();
          },
          notification: {
            poll: function(args) { args.complete({ data: { state: 'Running' } }); }
          }
        },
				*/
				
				/*
        stop: {
          label: 'Stop tier',
          shortLabel: 'Stop',
          action: function(args) {
            args.response.success();
          },
          notification: {
            poll: function(args) { args.complete({ data: { state: 'Stopped' } }); }
          }
        },
				*/
				
        addVM: {
          label: 'Add VM to tier',
          shortLabel: 'Add VM',
          action: cloudStack.uiCustom.instanceWizard(						
						$.extend(true, {}, cloudStack.instanceWizard, {
						  pluginForm: {
								name: 'vpcTierInstanceWizard'
							}
						})	
          ),
          notification: {
            poll: pollAsyncJobResult
          }
        },
				
        acl: {
          label: 'Configure ACL for tier',
          shortLabel: 'ACL',
          multiEdit: aclMultiEdit
        },
				
        remove: {
          label: 'Remove tier',
          action: function(args) {					 
						$.ajax({
						  url: createURL('deleteNetwork'),
							dataType: "json",
							data: {
							  id: args.context.tiers[0].id
							},
							success: function(json) {							  
								var jid = json.deletenetworkresponse.jobid;
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
      },

      // Get tiers
      dataProvider: function(args) {		
				$.ajax({
					url: createURL("listNetworks"),
					dataType: "json",
					data: {
					  vpcid: args.context.vpc[0].id,
						listAll: true
					},
					async: true,
					success: function(json) {					  
						var networks = json.listnetworksresponse.network;												
						if(networks != null && networks.length > 0) {
						  for(var i = 0; i < networks.length; i++) {							 
							  $.ajax({
								  url: createURL("listVirtualMachines"),
									dataType: "json",
									data: {
									  networkid: networks[i].id,
										listAll: true
									},
									async: false,
									success: function(json) {									  
									  networks[i].virtualMachines = json.listvirtualmachinesresponse.virtualmachine;										
									}
								});								
							}
						}		
						args.response.success({ tiers: networks });
					}
				});	 
      }
    }
  };
}(jQuery, cloudStack));
