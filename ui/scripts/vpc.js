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
			'traffictype' : { 
			  label: 'Traffic type',
				select: function(args) {				  
					args.response.success({
						data: [
							{ name: 'Ingress', description: 'Ingress' },
							{ name: 'Egress', description: 'Egress' }
						]
					});		
				}
			},
			'add-rule': {
				label: 'label.add.rule',
				addButton: true
			}
		},  
    add: {
      label: 'Add',
      action: function(args) {        
				$.ajax({
					url: createURL('createNetworkACL'),
					data: $.extend(args.data, {
						networkid: args.context.networks[0].id
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
					networkid: args.context.networks[0].id
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
						  networkid: args.context.networks[0].id
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
		  listView: function() {
		    return {
          listView: {
            id: 'vpcGateways',
            fields: {						 
						  ipaddress: { label: 'label.ip.address', validation: { required: true }},
							gateway: { label: 'label.gateway', validation: { required: true }},
							netmask: { label: 'label.netmask', validation: { required: true }}, 									
							vlan: { label: 'label.vlan', validation: { required: true }}  
            },
            actions: {
              add: {
                label: 'Add new gateway',															
								preFilter: function(args) {
									if(isAdmin())
									  return true;
									else
									  return false;
								},															
                createForm: {
                  title: 'Add new gateway',
                  desc: 'Please specify the information to add a new gateway to this VPC.',
                  fields: {
									  ipaddress: { label: 'label.ip.address', validation: { required: true }},
										gateway: { label: 'label.gateway', validation: { required: true }},
										netmask: { label: 'label.netmask', validation: { required: true }}, 									
                    vlan: { label: 'label.vlan', validation: { required: true }}                    
                  }
                },
                action: function(args) {								 
									$.ajax({
									  url: createURL('createPrivateGateway'),
										data: {
										  vpcid: args.context.vpc[0].id,
											ipaddress: args.data.ipaddress,
											gateway: args.data.gateway,
											netmask: args.data.netmask,
											vlan: args.data.vlan
										},
										success: function(json) {										 							
											var jid = json.createprivategatewayresponse.jobid;
											args.response.success(
												{_custom:
												 {jobId: jid,
													getUpdatedItem: function(json) {													 
														return json.queryasyncjobresultresponse.jobresult.privategateway;
													}
												 }
												}
											);																					
										},
										error: function(json) {
                      args.response.error(parseXMLHttpResponse(json));
                    }										
									});		
                },
                messages: {
                  notification: function() { return 'Add gateway to VPC'; }
                }
              }
            },
            dataProvider: function(args) {						 
							$.ajax({
							  url: createURL('listPrivateGateways'),
								data: {
								  vpcid: args.context.vpc[0].id,
									listAll: true
								},
								success: function(json) {
								  var items = json.listprivategatewaysresponse.privategateway;
									args.response.success({ data: items });									
								}
							});						
            },
            detailView: {
              name: 'Gateway details',
							actions: {							  
								remove: {
									label: 'delete gateway',
									messages: {
										confirm: function(args) {
											return 'Please confirm you want to delete the gateway';
										},
										notification: function(args) {
											return 'delete gateway';
										}
									},
									action: function(args) {
										$.ajax({
											url: createURL("deletePrivateGateway&id=" + args.context.vpcGateways[0].id),
											dataType: "json",
											async: true,
											success: function(json) {
												var jid = json.deleteprivategatewayresponse.jobid;
												args.response.success(
													{_custom:
													  {
														  jobId: jid
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
                  fields: [
                    {
                      ipaddress: { label: 'label.ip.address' }
                    },
                    {  	
											gateway: { label: 'label.gateway' },
											netmask: { label: 'label.netmask'}, 									
											vlan: { label: 'label.vlan' },  
                      state: { label: 'label.state' },											
											id: { label: 'label.id' },
											zonename: { label: 'label.zone' },
											domain: { label: 'label.domain' },
											account: { label: 'label.account' }											
                    }
                  ],
                  dataProvider: function(args) {										  
										$.ajax({
											url: createURL('listPrivateGateways'),
											data: {
												id: args.context.vpcGateways[0].id
											},
											success: function(json) {
												var item = json.listprivategatewaysresponse.privategateway[0];
												args.response.success({ data: item });									
											}
										});	
                  }
                },
                staticRoutes: {
                  title: 'Static Routes',
                  custom: function(args) {
                    return $('<div>').multiEdit({
                      noSelect: true,
                      context: args.context,
                      fields: {
                        cidr: { edit: true, label: 'label.cidr' },
                        'add-rule': {
				                  label: 'Add route',
				                  addButton: true
			                  }
                      },
                      add: {
                        label: 'Add',
                        action: function(args) {												  
												  $.ajax({
													  url: createURL('createStaticRoute'),
														data: {
														  gatewayid: args.context.vpcGateways[0].id,
															cidr: args.data.cidr
														},																
                            success: function(data) {
                              args.response.success({
                                _custom: {
                                  jobId: data.createstaticrouteresponse.jobid
                                },
                                notification: {
                                  label: 'Add static route',
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
                          label: 'Remove static route',
													action: function(args) {													  
                            $.ajax({
                              url: createURL('deleteStaticRoute'),
                              data: {                                
                                id: args.context.multiRule[0].id
                              },
                              dataType: 'json',
                              async: true,
                              success: function(data) {
                                var jobID = data.deletestaticrouteresponse.jobid;

                                args.response.success({
                                  _custom: {
                                    jobId: jobID
                                  },
                                  notification: {
                                    label: 'Remove static route',
                                    poll: pollAsyncJobResult
                                  }
                                });
                              }
                            });
                          }													
                        }
                      },
                      dataProvider: function(args) {											  
												$.ajax({
												  url: createURL('listStaticRoutes'),
													data: {
													  gatewayid: args.context.vpcGateways[0].id
													},
													success: function(json) {													 
														var items = json.liststaticroutesresponse.staticroute;
														args.response.success({ data: items });
													}
												});												
                      }
                    });
                  }
                }
              }
            }
          }
        };
			}		
		},
    siteToSiteVPN: {//start siteToSiteVPN
			type: 'select',
			title: 'site-to-site VPN',
			listView: {
				id: 'siteToSiteVpn',
				label: 'site-to-site VPN',
				fields: {			
					publicip: { label: 'label.ip.address' },				
					gateway: { label: 'label.gateway' },
					cidrlist: { label: 'CIDR list' },
					ipsecpsk: { label: 'IPsec Preshared-Key' },
					ikepolicy: { label: 'IKE policy' },
					esppolicy: { label: 'ESP policy' }								
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
						url: createURL("listVpnConnections&listAll=true&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
						data: {
						  vpcid: args.context.vpc[0].id
						},						
						success: function(json) {							
							var items = json.listvpnconnectionsresponse.vpnconnection;
							args.response.success({data:items});
						}
					});
				},
				
				actions: {
					add: {
						label: 'add site-to-site VPN',
						messages: {                
							notification: function(args) {
								return 'add site-to-site VPN';
							}
						},
						createForm: {
							title: 'add site-to-site VPN',
							fields: {
								publicipid: {
									label: 'label.ip.address',
									select: function(args) {	
										$.ajax({
											url: createURL('listPublicIpAddresses'),												
											dataType: 'json',
											data: {
											  vpcid: args.context.vpc[0].id
											},
											async: true,
											success: function(json) {				
												var items = [];												
												var objs = json.listpublicipaddressesresponse.publicipaddress;
												if(objs != null && objs.length > 0) {
													for(var i = 0; i < objs.length; i++) {
														items.push({id: objs[i].id, description: objs[i].ipaddress});
													}
												}													
												args.response.success({data: items});													
											}
										});											
									},
									validation: { required: true }
								},
								gateway: { 
									label: 'label.gateway',
									validation: { required: true }
								}, 
								cidrlist: { 
									label: 'CIDR list',
									validation: { required: true }
								},
								ipsecpsk: { 
									label: 'IPsec Preshared-Key',
									validation: { required: true }
								},
								ikepolicy: { 
									label: 'IKE policy',
									select: function(args) {
										var items = [];
										items.push({id: '3des-md5', description: '3des-md5'});
										items.push({id: 'aes-md5', description: 'aes-md5'});
										items.push({id: 'aes128-md5', description: 'aes128-md5'});
										items.push({id: 'des-md5', description: 'des-md5'});											
										items.push({id: '3des-sha1', description: '3des-sha1'});
										items.push({id: 'aes-sha1', description: 'aes-sha1'});
										items.push({id: 'aes128-sha1', description: 'aes128-sha1'});
										items.push({id: 'des-sha1', description: 'des-sha1'});
										args.response.success({data: items});
									}
								},
								esppolicy: { 
									label: 'ESP policy',
									select: function(args) {
										var items = [];
										items.push({id: '3des-md5', description: '3des-md5'});
										items.push({id: 'aes-md5', description: 'aes-md5'});
										items.push({id: 'aes128-md5', description: 'aes128-md5'});
										items.push({id: 'des-md5', description: 'des-md5'});											
										items.push({id: '3des-sha1', description: '3des-sha1'});
										items.push({id: 'aes-sha1', description: 'aes-sha1'});
										items.push({id: 'aes128-sha1', description: 'aes128-sha1'});
										items.push({id: 'des-sha1', description: 'des-sha1'});
										args.response.success({data: items});
									}
								},
								lifetime: { 
									label: 'Lifetime (second)',
									defaultValue: '86400',
									validation: { required: false, number: true }
								}		
							}
						},
						action: function(args) {	
							var createVpnCustomerGatewayAndVpnConnection = function(vpngatewayid) {							 
								$.ajax({
									url: createURL('createVpnCustomerGateway'),
									data: {
										gateway: args.data.gateway,
										cidrlist: args.data.cidrlist,
										ipsecpsk: args.data.ipsecpsk,
										ikepolicy: args.data.ikepolicy,
										esppolicy: args.data.esppolicy,
										lifetime: args.data.lifetime
									},
									dataType: 'json',									
									success: function(json) {
										var jid = json.createvpncustomergatewayresponse.jobid;                          
										var createvpncustomergatewayIntervalID = setInterval(function() { 																
											$.ajax({
												url: createURL("queryAsyncJobResult&jobid=" + jid),
												dataType: "json",
												success: function(json) {													
													var result = json.queryasyncjobresultresponse;	
													if (result.jobstatus == 0) {
														return; //Job has not completed
													}
													else {                                      
														clearInterval(createvpncustomergatewayIntervalID); 														
														if (result.jobstatus == 1) {															
															var obj = result.jobresult.vpncustomergateway;
															var vpncustomergatewayid = obj.id;	
																	
															$.ajax({
																url: createURL('createVpnConnection'),
																data: {
																	s2svpngatewayid: vpngatewayid,
																	s2scustomergatewayid: vpncustomergatewayid
																},
																dataType: 'json',									
																success: function(json) {
																	var jid = json.createvpnconnectionresponse.jobid;                          
																	var createvpnconnectionIntervalID = setInterval(function() { 																
																		$.ajax({
																			url: createURL("queryAsyncJobResult&jobid=" + jid),
																			dataType: "json",
																			success: function(json) {													
																				var result = json.queryasyncjobresultresponse;	
																				if (result.jobstatus == 0) {
																					return; //Job has not completed
																				}
																				else {                                      
																					clearInterval(createvpnconnectionIntervalID); 
																																															
																					if (result.jobstatus == 1) {																														
																						//remove loading image on table row																													
																						var $listviewTable = $("div.list-view  div.data-table table.body tbody");														
																						var $tr1 = $listviewTable.find("tr.loading").removeClass("loading");
																						$tr1.find("td div.loading").removeClass("loading");

																						var item = result.jobresult.vpnconnection;	                                                         	
																						$tr1.find("td.publicip span").text(item.publicip);																													
																						
																						cloudStack.dialog.notice({ message: "site-to-site VPN is created successfully." });		
																					}
																					else if (result.jobstatus == 2) {
																						$.removeTableRowInAction();
																						cloudStack.dialog.notice({ message: _s(result.jobresult.errortext) });																														
																					}
																				}
																			},
																			error: function(XMLHttpResponse) {
																				$.removeTableRowInAction();
																				cloudStack.dialog.notice({ message: parseXMLHttpResponse(XMLHttpResponse) });	
																			}
																		});                              
																	}, 3000); 																
																}
															});																		
														}
														else if (result.jobstatus == 2) {
															$.removeTableRowInAction();
															cloudStack.dialog.notice({ message: _s(result.jobresult.errortext) });																									
														}
													}
												},
												error: function(XMLHttpResponse) {
													$.removeTableRowInAction();
													cloudStack.dialog.notice({ message: parseXMLHttpResponse(XMLHttpResponse) });																				
												}
											});                              
										}, 3000); 																
									}
								});			
							}							
						  							
							var vpngatewayid;
							$.ajax({
							  url: createURL('listVpnGateways'),
								data: {
								  vpcid: args.context.vpc[0].id
								},
								async: false,
								success: function(json) {								  
								  var items = json.listvpngatewaysresponse.vpngateway;
									if(items != null && items.length > 0) {
									  vpngatewayid = items[0].id;
									}
								}
							});
						 
						  if(vpngatewayid == null) {						
								$.ajax({
									url: createURL('createVpnGateway'),
									data: {
										publicipid: args.data.publicipid
									},														
									success: function(json) {
										var jid = json.createvpngatewayresponse.jobid;                          
										var createvpngatewayIntervalID = setInterval(function() { 																
											$.ajax({
												url: createURL("queryAsyncJobResult&jobid=" + jid),
												dataType: "json",
												success: function(json) {													
													var result = json.queryasyncjobresultresponse;												
													if (result.jobstatus == 0) {
														return; //Job has not completed
													}
													else {                                      
														clearInterval(createvpngatewayIntervalID); 														
														if (result.jobstatus == 1) {															
															var obj = result.jobresult.vpngateway;
															vpngatewayid = obj.id;														
															createVpnCustomerGatewayAndVpnConnection(vpngatewayid); 
														}
														else if (result.jobstatus == 2) {		
															$.removeTableRowInAction();
															cloudStack.dialog.notice({ message: _s(result.jobresult.errortext) });																
														}
													}
												},
												error: function(XMLHttpResponse) {													
													$.removeTableRowInAction();
													cloudStack.dialog.notice({ message: parseXMLHttpResponse(XMLHttpResponse) });	
												}
											});                              
										}, 3000); 																
									}
								});				
              }
              else { //vpngatewayid != null							
							  createVpnCustomerGatewayAndVpnConnection(vpngatewayid); 
							}								
						}              
					}
				},
				
				detailView: {
					name: 'label.details',
					tabs: {
						details: {
							title: 'label.details',
							fields: [
								{                   
									id: { label: 'label.id' },
									
									//s2svpngatewayid: { label: 'VPN gateway ID' },
									publicip: { label: 'label.ip.address' },
									
									//s2scustomergatewayid: { label: 'Customer gateway ID' }, 
									gateway: { label: 'label.gateway' },
									cidrlist: { label: 'CIDR list' },
									ipsecpsk: { label: 'IPsec Preshared-Key' },
									ikepolicy: { label: 'IKE policy' },
									esppolicy: { label: 'ESP policy' },
									lifetime: { label: 'Lifetime (second)' },
																		 
									created: { label: 'label.date', converter: cloudStack.converters.toLocalDate }										
								}
							],
							dataProvider: function(args) {								  
								$.ajax({
									url: createURL("listVpnConnections&id=" + args.context.siteToSiteVpn[0].id),
									dataType: "json",
									async: true,
									success: function(json) {							
										var item = json.listvpnconnectionsresponse.vpnconnection[0];
										args.response.success({data: item});
									}
								});									
							}
						}
					},
					actions: {                 
						restart: {
							label: 'Reset VPN connection',
							messages: {
								confirm: function(args) {
									return 'Please confirm that you want to reset VPN connection' ;
								},
								notification: function(args) {
									return 'Reset VPN connection';
								}
							},
							action: function(args) {
								$.ajax({
									url: createURL("resetVpnConnection"),										
									data: {
										id: args.context.siteToSiteVpn[0].id
									},										
									dataType: "json",
									async: true,
									success: function(json) {
										var jid = json.resetvpnconnectionresponse.jobid;
										args.response.success(
											{_custom:
												{
													jobId: jid,
													getUpdatedItem: function(json) {														  
														return json.queryasyncjobresultresponse.jobresult.vpnconnection;
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
												
						remove: {
							label: 'delete site-to-site VPN',
							messages: {
								confirm: function(args) {
									return 'Please confirm that you want to delete this site-to-site VPN';
								},
								notification: function(args) {
									return 'delete site-to-site VPN';
								}
							},
							action: function(args) {
								$.ajax({
									url: createURL("deleteVpnConnection"),
									dataType: "json",
									data: {
										id: args.context.siteToSiteVpn[0].id
									},
									async: true,
									success: function(json) {		
										var jid = json.deletevpnconnectionresponse.jobid;										
										var deleteVpnConnectionIntervalID = setInterval(function() { 	
											$.ajax({
												url: createURL("queryAsyncJobResult&jobId=" + jid),
												dataType: "json",
												success: function(json) {
													var result = json.queryasyncjobresultresponse;
													if (result.jobstatus == 0) {
														return; //Job has not completed
													}
													else {
														clearInterval(deleteVpnConnectionIntervalID); 
														if (result.jobstatus == 1) {																
                              $.ajax({
																url: createURL("deleteVpnCustomerGateway"),
																dataType: "json",
																data: {
																	id: args.context.siteToSiteVpn[0].s2scustomergatewayid
																},
																async: true,
																success: function(json) {	
																	var jid = json.deletecustomergatewayresponse.jobid;			
																	var deleteVpnCustomerGatewayIntervalID = setInterval(function() { 	
																		$.ajax({
																			url: createURL("queryAsyncJobResult&jobId=" + jid),
																			dataType: "json",
																			success: function(json) {
																				var result = json.queryasyncjobresultresponse;
																				if (result.jobstatus == 0) {
																					return; //Job has not completed
																				}
																				else {
																					clearInterval(deleteVpnCustomerGatewayIntervalID); 
																					if (result.jobstatus == 1) {	
																						$("div.detail-view div.loading-overlay").remove();
																						cloudStack.dialog.notice({ message: "site-to-site VPN has been deleted." });																															
																						$.removeDetailViewAndTableRow();																														
																					}
																					else if (result.jobstatus == 2) {
																						$("div.detail-view div.loading-overlay").remove();
																						cloudStack.dialog.notice({ message: _s(result.jobresult.errortext) });																										
																					}
																				}
																			},
																			error: function(XMLHttpResponse) {
																				$("div.detail-view div.loading-overlay").remove();
																				cloudStack.dialog.notice({ message: parseXMLHttpResponse(XMLHttpResponse) });																												
																			}
																		});
																	}, 3000);		
																}
															});										
														}
														else if (result.jobstatus == 2) {
															$("div.detail-view div.loading-overlay").remove();
															cloudStack.dialog.notice({ message: _s(result.jobresult.errortext) });	
														}
													}
												},
												error: function(XMLHttpResponse) {
													$("div.detail-view div.loading-overlay").remove();
													cloudStack.dialog.notice({ message: parseXMLHttpResponse(XMLHttpResponse) });	
												}
											});
										}, 3000);		
									}
								});										
							}								
						}									
					}							
				}
			}	     
    },
		
    tiers: {
      detailView: { //duplicate from cloudStack.sections.network.sections.networks.listView.detailView (begin)			 
				name: 'Guest network details',
				viewAll: {
					path: 'network.ipAddresses',
					label: 'label.menu.ipaddresses',
					preFilter: function(args) {					  
						if (args.context.networks[0].state == 'Destroyed')
							return false;

						var services = args.context.networks[0].service;
						if(services == null)
							return false;

						if(args.context.networks[0].type == "Isolated") {
							for(var i=0; i < services.length; i++) {
								var service = services[i];
								if(service.name == "SourceNat") {
									return true;
								}
							}
						}
						else if(args.context.networks[0].type == "Shared") {
							var havingSecurityGroupService = false;
							var havingElasticIpCapability = false;
							var havingElasticLbCapability = false;

							for(var i=0; i < services.length; i++) {
								var service = services[i];
								if(service.name == "SecurityGroup") {
									havingSecurityGroupService = true;
								}
								else if(service.name == "StaticNat") {
									$(service.capability).each(function(){
										if(this.name == "ElasticIp" && this.value == "true") {
											havingElasticIpCapability = true;
											return false; //break $.each() loop
										}
									});
								}
								else if(service.name == "Lb") {
									$(service.capability).each(function(){
										if(this.name == "ElasticLb" && this.value == "true") {
											havingElasticLbCapability = true;
											return false; //break $.each() loop
										}
									});
								}
							}

							if(havingSecurityGroupService == true && havingElasticIpCapability == true && havingElasticLbCapability == true)
								return true;
							else
								return false;
						}

						return false;
					}
				},
				actions: {
					edit: {
						label: 'label.edit',
						messages: {
							notification: function(args) {
								return 'label.edit.network.details';
							}
						},
						action: function(args) {
							var array1 = [];
							array1.push("&name=" + todb(args.data.name));
							array1.push("&displaytext=" + todb(args.data.displaytext));

							//args.data.networkdomain is null when networkdomain field is hidden
							if(args.data.networkdomain != null && args.data.networkdomain != args.context.networks[0].networkdomain)
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
								var zoneObj;
								$.ajax({
									url: createURL("listZones&id=" + args.context.networks[0].zoneid),
									dataType: "json",
									async: false,
									success: function(json){											  
										zoneObj = json.listzonesresponse.zone[0];												
									}
								});																				
								if(zoneObj.networktype == "Basic") {										  								
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
						messages: {
							notification: function(args) {
								return 'label.restart.network';
							}
						},
						action: function(args) {
							var array1 = [];									
							array1.push("&cleanup=" + (args.data.cleanup == "on"));
							$.ajax({
								url: createURL("restartNetwork&id=" + args.context.networks[0].id + array1.join("")),
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
						notification: {
							poll: pollAsyncJobResult
						}
					},

					remove: {
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

				tabFilter: function(args) {
					var networkOfferingHavingELB = false;
					$.ajax({
						url: createURL("listNetworkOfferings&id=" + args.context.networks[0].networkofferingid),
						dataType: "json",
						async: false,
						success: function(json) {
							var networkoffering = json.listnetworkofferingsresponse.networkoffering[0];
							$(networkoffering.service).each(function(){
								var thisService = this;
								if(thisService.name == "Lb") {
									$(thisService.capability).each(function(){
										if(this.name == "ElasticLb" && this.value == "true") {
											networkOfferingHavingELB = true;
											return false; //break $.each() loop
										}
									});
									return false; //break $.each() loop
								}
							});
						}
					});

					var hiddenTabs = [];
					if(networkOfferingHavingELB == false)
						hiddenTabs.push("addloadBalancer");
					return hiddenTabs;
				},

				isMaximized: true,
				tabs: {
					details: {
						title: 'label.details',
						preFilter: function(args) {
							var hiddenFields = [];
							var zone;

							$.ajax({
								url: createURL('listZones'),
								data: {
									id: args.context.networks[0].zoneid
								},
								async: false,
								success: function(json) {
									zone = json.listzonesresponse.zone[0];
								}
							});

							if(zone.networktype == "Basic") {
								hiddenFields.push("account");
								hiddenFields.push("gateway");
								hiddenFields.push("vlan");
								hiddenFields.push("cidr");
								//hiddenFields.push("netmask");
							}

							if(args.context.networks[0].type == "Isolated") {
								hiddenFields.push("networkofferingdisplaytext");
								hiddenFields.push("networkdomaintext");
								hiddenFields.push("gateway");
								hiddenFields.push("networkofferingname");
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
								zonename: { label: 'label.zone' },
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
								vlan: { label: 'VLAN ID' },

								networkofferingname: { label: 'label.network.offering' },

								networkofferingid: {
									label: 'label.network.offering',
									isEditable: true,
									select: function(args){
										if (args.context.networks[0].state == 'Destroyed') {
											args.response.success({ data: [] });
											return;
										}

										var items = [];
										$.ajax({
											url: createURL("listNetworkOfferings&networkid=" + args.context.networks[0].id),
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
											url: createURL("listNetworkOfferings&id=" + args.context.networks[0].networkofferingid),  //include currently selected network offeirng to dropdown
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

								gateway: { label: 'label.gateway' },

								//netmask: { label: 'Netmask' },
								cidr: { label: 'CIDR' },

								networkdomaintext: {
									label: 'label.network.domain.text'
								},
								networkdomain: {
									label: 'label.network.domain',
									isEditable: true
								},

								domain: { label: 'label.domain' },
								account: { label: 'label.account' }
							}
						],
						dataProvider: function(args) {								 					
							$.ajax({
								url: createURL("listNetworks&id=" + args.context.networks[0].id + "&listAll=true"), //pass "&listAll=true" to "listNetworks&id=xxxxxxxx" for now before API gets fixed.
								data: { listAll: true },
								dataType: "json",
								async: true,
								success: function(json) {								  
									var jsonObj = json.listnetworksresponse.network[0];   
									args.response.success(
										{
											actionFilter: cloudStack.actionFilter.guestNetwork,
											data: jsonObj
										}
									);		
								}
							});			
						}
					},

					addloadBalancer: {
						title: 'label.add.load.balancer',
						custom: function(args) {
							var context = args.context;

							return $('<div>').multiEdit(
								{
									context: context,
									listView: $.extend(true, {}, cloudStack.sections.instances, {
										listView: {
											dataProvider: function(args) {                           
												var networkid;
												if('vpc' in args.context) 
													networkid = args.context.multiData.tier;														
												else 
													networkid = args.context.ipAddresses[0].associatednetworkid;
												
												var data = {
													page: args.page,
													pageSize: pageSize,
													networkid: networkid,
													listAll: true
												};
												
												$.ajax({
													url: createURL('listVirtualMachines'),
													data: data,
													dataType: 'json',
													async: true,
													success: function(data) {
														args.response.success({
															data: $.grep(
																data.listvirtualmachinesresponse.virtualmachine ?
																	data.listvirtualmachinesresponse.virtualmachine : [],
																function(instance) {
																	return $.inArray(instance.state, [
																		'Destroyed'
																	]) == -1;
																}
															)
														});
													},
													error: function(data) {
														args.response.error(parseXMLHttpResponse(data));
													}
												});
											}
										}
									}),
									multipleAdd: true,
									fields: {
										'name': { edit: true, label: 'label.name' },
										'publicport': { edit: true, label: 'label.public.port' },
										'privateport': { edit: true, label: 'label.private.port' },
										'algorithm': {
											label: 'label.algorithm',
											select: function(args) {
												args.response.success({
													data: [
														{ name: 'roundrobin', description: _l('label.round.robin') },
														{ name: 'leastconn', description: _l('label.least.connections') },
														{ name: 'source', description: _l('label.source') }
													]
												});
											}
										},
										'sticky': {
											label: 'label.stickiness',
											custom: {
												buttonLabel: 'label.configure',
												action: cloudStack.lbStickyPolicy.dialog()
											}
										},
										'add-vm': {
											label: 'label.add.vms',
											addButton: true
										}
									},
									add: {
										label: 'label.add.vms',
										action: function(args) {                         
											var data = {
												algorithm: args.data.algorithm,
												name: args.data.name,
												privateport: args.data.privateport,
												publicport: args.data.publicport,
												openfirewall: false,
												domainid: g_domainid,
												account: g_account
											};
											
											if('vpc' in args.context) { //from VPC section
												if(args.data.tier == null) {													  
													args.response.error('Tier is required');
													return;
												}			
												$.extend(data, {
													networkid: args.data.tier		
												});	
											}
											else {  //from Guest Network section
												$.extend(data, {
													networkid: args.context.networks[0].id
												});	
											}
											
											var stickyData = $.extend(true, {}, args.data.sticky);
										 
											$.ajax({
												url: createURL('createLoadBalancerRule'),
												data: data,
												dataType: 'json',
												async: true,
												success: function(data) {
													var itemData = args.itemData;
													var jobID = data.createloadbalancerruleresponse.jobid;

													$.ajax({
														url: createURL('assignToLoadBalancerRule'),
														data: {
															id: data.createloadbalancerruleresponse.id,
															virtualmachineids: $.map(itemData, function(elem) {
																return elem.id;
															}).join(',')
														},
														dataType: 'json',
														async: true,
														success: function(data) {
															var lbCreationComplete = false;

															args.response.success({
																_custom: {
																	jobId: jobID
																},
																notification: {
																	label: 'label.add.load.balancer',
																	poll: function(args) {
																		var complete = args.complete;
																		var error = args.error;

																		pollAsyncJobResult({
																			_custom: args._custom,
																			complete: function(args) {
																				if (lbCreationComplete) {
																					return;
																				}

																				lbCreationComplete = true;
																				cloudStack.dialog.notice({
																					message: _l('message.add.load.balancer.under.ip') +
																						args.data.loadbalancer.publicip
																				});

																				if (stickyData &&
																						stickyData.methodname &&
																						stickyData.methodname != 'None') {
																					cloudStack.lbStickyPolicy.actions.add(
																						args.data.loadbalancer.id,
																						stickyData,
																						complete, // Complete
																						complete // Error
																					);
																				} else {
																					complete();
																				}
																			},
																			error: error
																		});
																	}
																}
															});
														},
														error: function(data) {
															args.response.error(parseXMLHttpResponse(data));
														}
													});
												},
												error: function(data) {
													args.response.error(parseXMLHttpResponse(data));
												}
											});
										}
									},
									dataProvider: function(args) {
										args.response.success({ //no LB listing in AddLoadBalancer tab
											data: []
										});
									}
								}
							);
						}
					}
				}
				//???
			
			  /*
        name: 'Tier details',
        tabs: {
          details: {
            title: 'Details',
            fields: [
              { id: { label: 'ID' }},
              {
                name: { label: 'Name' },
                cidr: { label: 'CIDR' }
              }
            ],
            dataProvider: function(args) {
              args.response.success({
                data: args.context.networks[0]
              });
            }
          }
        }
				*/				
      }, //duplicate from cloudStack.sections.network.sections.networks.listView.detailView (begin)	
			
      actionPreFilter: function(args) {
        var tier = args.context.networks[0];
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
									var networkSupportingLbExists = false;									
									$.ajax({
									  url: createURL('listNetworks'),
										data: {
										  vpcid: args.context.vpc[0].id,
										  supportedservices: 'LB'
										},										
										success: function(json) {		
                      var networkSupportingLbExists;										
											if(json.listnetworksresponse.network != null && json.listnetworksresponse.network.length > 0) 
											  networkSupportingLbExists = true;
											else
                        networkSupportingLbExists = false;											
											
											//???
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
																										
													var items;
													if(networkSupportingLbExists == true) {
													  items = $.grep(networkOfferings, function(networkOffering) {
															var includingLbService = false;
															$(networkOffering.service).each(function(){
																var thisService = this;
																if(thisService.name == "Lb") {
																	includingLbService = true;																	
																	return false; //break $.each() loop
																}
															});	
                              return !includingLbService;															
														});
													}
													else {
													  items = networkOfferings;
													}
																										
													args.response.success({
														data: $.map(items, function(item) {
															return {
																id: item.id,
																description: item.name
															};
														})
													});
												}
											});
											//???											
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
							}
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
							  id: args.context.networks[0].id
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
