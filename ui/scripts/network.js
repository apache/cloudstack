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

(function(cloudStack, $) {
  var ingressEgressDataMap = function(elem) {
    var elemData = {
      id: elem.ruleid,
      protocol: elem.protocol,
      startport: elem.startport,
      endport: elem.endport,
      cidr: elem.cidr ? elem.cidr : ''.concat(elem.account, ' - ', elem.securitygroupname)
    };

    if (elemData.startport == 0 && elemData.endport) {
      elemData.startport = '0';
    } else if (elem.icmptype && elem.icmpcode) {
      elemData.startport = elem.icmptype;
      elemData.endport = elem.icmpcode;
    }

    return elemData;
  };

  var instanceSecondaryIPSubselect = function(args) {
    var instance = args.context.instances[0];
    var network = args.context.networks[0];
    var nic = $.grep(instance.nic, function(nic) {
      return nic.networkid == network.id;
    })[0];

    // Get NIC IPs
    $.ajax({
      url: createURL('listNics'),
      data: {
        virtualmachineid: instance.id,
        nicId: nic.id
      },
      success: function(json) {
        var nic = json.listnics.nic[0];
        var ips = nic.secondaryip ? nic.secondaryip : [];
        var ipSelection = [];

        // Add primary IP as default
        ipSelection.push({ id: -1, description: nic.ipaddress + ' (Primary)' });

        // Add secondary IPs
        $(ips).map(function(index, ip) {
          ipSelection.push({
            id: ip.ipaddress,
            description: ip.ipaddress
          });
        }); 


        args.response.success({
          data: ipSelection
        });
      }
    })
  };
  
  var ipChangeNotice = function() {
    cloudStack.dialog.confirm({
      message: 'message.ip.address.changed',
      action: function() {
        $('#browser .container').cloudBrowser('selectPanel', {
          panel: $('#browser .panel:last').prev(),
          complete: function() {
            $(window).trigger('cloudStack.fullRefresh');
          }
        });
      }
    });
  };

  var zoneObjs = [];

  var actionFilters = {
    ipAddress: function(args) {
      var allowedActions = args.context.actions;
      var disallowedActions = [];
      var item = args.context.item;
      var status = item.state;

      if (status == 'Destroyed' ||
          status == 'Releasing' ||
          status == 'Released' ||
          status == 'Creating' ||
          status == 'Allocating' ||
          item.account == 'system' ||
          item.issystem == true ) {
            return [];
          }

      if(item.networkOfferingConserveMode == false) {
        /*
         (1) If IP is SourceNat, no StaticNat/VPN/PortForwarding/LoadBalancer can be enabled/added.
         */
        if (item.issourcenat == true){
          disallowedActions.push('enableStaticNAT');
          disallowedActions.push('enableVPN');
        }

        /*
         (2) If IP is non-SourceNat, show StaticNat/VPN/PortForwarding/LoadBalancer at first.
         1. Once StaticNat is enabled, hide VPN/PortForwarding/LoadBalancer.
         2. Once VPN is enabled, hide StaticNat/PortForwarding/LoadBalancer.
         3. Once a PortForwarding rule is added, hide StaticNat/VPN/LoadBalancer.
         4. Once a LoadBalancer rule is added, hide StaticNat/VPN/PortForwarding.
         */
        else { //item.issourcenat == false
          if (item.isstaticnat) { //1. Once StaticNat is enabled, hide VPN/PortForwarding/LoadBalancer.
            disallowedActions.push('enableVPN');
          }
          if (item.vpnenabled) { //2. Once VPN is enabled, hide StaticNat/PortForwarding/LoadBalancer.
            disallowedActions.push('enableStaticNAT');
          }

          //3. Once a PortForwarding rule is added, hide StaticNat/VPN/LoadBalancer.
          $.ajax({
            url: createURL('listPortForwardingRules'),
            data: {
              ipaddressid: item.id,
              listAll: true
            },
            dataType: 'json',
            async: false,
            success: function(json) {
              var rules = json.listportforwardingrulesresponse.portforwardingrule;
              if(rules != null && rules.length > 0) {
                disallowedActions.push('enableVPN');
                disallowedActions.push('enableStaticNAT');
              }
            }
          });

          //4. Once a LoadBalancer rule is added, hide StaticNat/VPN/PortForwarding.
          $.ajax({
            url: createURL('listLoadBalancerRules'),
            data: {
              publicipid: item.id,
              listAll: true
            },
            dataType: 'json',
            async: false,
            success: function(json) {
              var rules = json.listloadbalancerrulesresponse.loadbalancerrule;
              if(rules != null && rules.length > 0) {
                disallowedActions.push('enableVPN');
                disallowedActions.push('enableStaticNAT');
              }
            }
          });
        }
      }
			if(item.networkOfferingConserveMode == false) {			 
				/*
				(1) If IP is SourceNat, no StaticNat/VPN/PortForwarding/LoadBalancer can be enabled/added. 
				*/
	      if (item.issourcenat == true){
					disallowedActions.push('enableStaticNAT');
					disallowedActions.push('enableVPN');
				}			
				
				/*
				(2) If IP is non-SourceNat, show StaticNat/VPN/PortForwarding/LoadBalancer at first.
				1. Once StaticNat is enabled, hide VPN/PortForwarding/LoadBalancer.
				2. Once VPN is enabled, hide StaticNat/PortForwarding/LoadBalancer.
				3. Once a PortForwarding rule is added, hide StaticNat/VPN/LoadBalancer.
				4. Once a LoadBalancer rule is added, hide StaticNat/VPN/PortForwarding.
				*/
				else { //item.issourcenat == false				   
					if (item.isstaticnat) { //1. Once StaticNat is enabled, hide VPN/PortForwarding/LoadBalancer.
					  disallowedActions.push('enableVPN');
					}
					if (item.vpnenabled) { //2. Once VPN is enabled, hide StaticNat/PortForwarding/LoadBalancer.
					  disallowedActions.push('enableStaticNAT');
					}
								 
					//3. Once a PortForwarding rule is added, hide StaticNat/VPN/LoadBalancer.
					$.ajax({
						url: createURL('listPortForwardingRules'),
						data: {
							ipaddressid: item.id,
							listAll: true
						},
						dataType: 'json',
						async: false,
						success: function(json) {							
							var rules = json.listportforwardingrulesresponse.portforwardingrule;
							if(rules != null && rules.length > 0) {
							  disallowedActions.push('enableVPN');
								disallowedActions.push('enableStaticNAT'); 
							}
						}
					});	
										
					//4. Once a LoadBalancer rule is added, hide StaticNat/VPN/PortForwarding.
					$.ajax({
						url: createURL('listLoadBalancerRules'),
						data: {
							publicipid: item.id,
							listAll: true
						},
						dataType: 'json',
						async: false,
						success: function(json) {
							var rules = json.listloadbalancerrulesresponse.loadbalancerrule;
							if(rules != null && rules.length > 0) {
							  disallowedActions.push('enableVPN');
								disallowedActions.push('enableStaticNAT'); 
							}
						}
					});		
				}			  				
			}			
			
      if (item.isstaticnat) {
        disallowedActions.push('enableStaticNAT');
      } else {
        disallowedActions.push('disableStaticNAT');
      }

			if(item.networkOfferingHavingVpnService == true) {      
        if (item.vpnenabled) {
          disallowedActions.push('enableVPN');
        } else {
          disallowedActions.push('disableVPN');
        }
      } else { //item.networkOfferingHavingVpnService == false
        disallowedActions.push('disableVPN');
        disallowedActions.push('enableVPN');
      }

      if (item.issourcenat){
        disallowedActions.push('enableStaticNAT');
        disallowedActions.push('disableStaticNAT');
        disallowedActions.push('remove');
      }

      allowedActions = $.grep(allowedActions, function(item) {
        return $.inArray(item, disallowedActions) == -1;
      });

      return allowedActions;
    },

    securityGroups: function(args) {
      var allowedActions = [];
      var isSecurityGroupOwner = isAdmin() || isDomainAdmin() ||
            args.context.item.account == args.context.users[0].account;

      if (isSecurityGroupOwner &&
          args.context.item.state != 'Destroyed' &&
          args.context.item.name != 'default') {
        allowedActions.push('remove');
      }

      return allowedActions;
    }
  };

	var networkOfferingObjs = [];
        var checkVpc=0; 	
  cloudStack.sections.network = {
    title: 'label.network',
    id: 'network',
    sectionSelect: {
      preFilter: function(args) {
        var havingSecurityGroupNetwork = false;
        var havingBasicZones = false;
        var havingAdvancedZones = true;

        // Get zone types
        $.ajax({
          url: createURL('listZones'),
          async: false,
          success: function(json) {
            var zones = json.listzonesresponse.zone ?
                  json.listzonesresponse.zone : [];
            var basicZones = $.grep(zones, function(zone) {
              return zone.networktype == 'Basic';
            });
            var advancedZones = $.grep(zones, function(zone) {
              return zone.networktype == 'Advanced';
            });

            
            havingBasicZones = basicZones.length ? true : false;
            havingAdvancedZones = advancedZones.length ? true : false;    
      }
        });
        
        $.ajax({
          url: createURL('listNetworks', { ignoreProject: true }),
          data: {
            supportedServices: 'SecurityGroup',
            listAll: true,
						details: 'min'
          },
          async: false,
          success: function(data) {
            if (data.listnetworksresponse.network != null && data.listnetworksresponse.network.length > 0) {
              havingSecurityGroupNetwork = true;
            }
          }
        });

        var sectionsToShow = ['networks'];

        if (havingAdvancedZones) {
          sectionsToShow.push('vpc');
          sectionsToShow.push('vpnCustomerGateway');
        }

        
        if(havingSecurityGroupNetwork == true)
          sectionsToShow.push('securityGroups');

        return sectionsToShow;
      },

      label: 'label.select-view'
    },
    sections: {
      networks: {
        id: 'networks',
        type: 'select',
        title: 'label.guest.networks',
        listView: {
          actions: {
            add: {
              label: 'label.add.guest.network',

              preFilter: function(args) {
                var advSgDisabledZones;
								$.ajax({
									url: createURL('listZones'),
									async: false,
									success: function(json) {									 
										advSgDisabledZones = $.grep(json.listzonesresponse.zone, function(zone) {
											return (zone.networktype == 'Advanced' && zone.securitygroupsenabled	!= true); //Isolated networks can only be created in Advanced SG-disabled zone (but not in Basic zone nor Advanced SG-enabled zone)
										});										
									}
								});								
								return (advSgDisabledZones != null && advSgDisabledZones.length > 0);							
              },

              createForm: {
                title: 'label.add.guest.network',
                desc: 'message.add.guest.network',
                fields: {
                  name: { label: 'label.name', validation: { required: true }, docID: 'helpGuestNetworkName' },
                  displayText: { label: 'label.display.text', validation: { required: true }, docID: 'helpGuestNetworkDisplayText'},
                  zoneId: {
                    label: 'label.zone',
                    validation: { required: true },
                    docID: 'helpGuestNetworkZone',

                    select: function(args) {
                      $.ajax({
                        url: createURL('listZones'),
                        success: function(json) {
                          var zones = $.grep(json.listzonesresponse.zone, function(zone) {
                            return (zone.networktype == 'Advanced' && zone.securitygroupsenabled	!= true); //Isolated networks can only be created in Advanced SG-disabled zone (but not in Basic zone nor Advanced SG-enabled zone)
                          });

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
                  networkOfferingId: {
                    label: 'label.network.offering',
                    validation: { required: true },
                    dependsOn: 'zoneId',
                    docID: 'helpGuestNetworkNetworkOffering',
                    select: function(args) {
                      $.ajax({
                        url: createURL('listVPCs'),
                        data: {
                          listAll: true
                        },
                        success: function(json) {
                          var items = json.listvpcsresponse.vpc;
                          var baseUrl = 'listNetworkOfferings&zoneid=' + args.zoneId;
                          var listUrl;
                          if(items != null && items.length > 0) 
                            listUrl = baseUrl;
                          else
                            listUrl = baseUrl + '&forVpc=false';
                          $.ajax({
                            url: createURL(listUrl),
                            data: {
                              guestiptype: 'Isolated',
                              supportedServices: 'SourceNat',
                              specifyvlan: false,
                              state: 'Enabled'
                            },
                            success: function(json) {
                              networkOfferingObjs = json.listnetworkofferingsresponse.networkoffering;
                              args.response.success({
                                data: $.map(networkOfferingObjs, function(zone) {
                                  return {
                                    id: zone.id,
                                    description: zone.name
                                  };
                                })
                              });
                            }
                          });                            
                        }
                      });
                    }
                  },

                  vpcid: {
                    label: 'label.vpc',
                    dependsOn: 'networkOfferingId',
                    select: function(args) {
                      var networkOfferingObj;
                      var $form = args.$select.closest('form');
                      $(networkOfferingObjs).each(function(key, value) {
                        if(value.id == args.networkOfferingId) {
                          networkOfferingObj = value;
                          return false; //break each loop
                        }
                      });
                      if(networkOfferingObj.forvpc == true) {
                        args.$select.closest('.form-item').css('display', 'inline-block');
                        $.ajax({
                          url: createURL('listVPCs'),
                          data: {
                            listAll: true,
														details: 'min'
                          },
                          success: function(json) {
                            var items = json.listvpcsresponse.vpc;
                            var data;
                            if(items != null && items.length > 0) {
                              data = $.map(items, function(item) {
                                return {
                                  id: item.id,
                                  description: item.name
                                }
                              });
                            }
                            args.response.success({ data: data });
                          }
                        });
                        $form.find('.form-item[rel=networkDomain]').hide();
                      }
                      else {
                        args.$select.closest('.form-item').hide();
                        $form.find('.form-item[rel=networkDomain]').show();
                        args.response.success({ data: null });
                      }
                    }
                  },

                  guestGateway: { label: 'label.guest.gateway', docID: 'helpGuestNetworkGateway' },
                  guestNetmask: { label: 'label.guest.netmask', docID: 'helpGuestNetworkNetmask' },
                  networkDomain: { label: 'label.network.domain' }
                }
              },
              action: function(args) {
                var dataObj = {
                  zoneId: args.data.zoneId,
                  name: args.data.name,
                  displayText: args.data.displayText,
                  networkOfferingId: args.data.networkOfferingId
                };		
		
                if(args.data.guestGateway != null && args.data.guestGateway.length > 0) {                  
                  $.extend(dataObj, {
                    gateway: args.data.guestGateway
                  });
                }								
                if(args.data.guestNetmask != null && args.data.guestNetmask.length > 0) {                  
                  $.extend(dataObj, {
                    netmask: args.data.guestNetmask
                  });									
                }								
                if(args.$form.find('.form-item[rel=vpcid]').css("display") != "none") {                 
                  $.extend(dataObj, {
                    vpcid: args.data.vpcid
                  });
                }
                if(args.data.networkDomain != null && args.data.networkDomain.length > 0 && args.$form.find('.form-item[rel=vpcid]').css("display") == "none") {
                  $.extend(dataObj, {
                    networkDomain: args.data.networkDomain
                  });                
                }
															
                $.ajax({
                  url: createURL('createNetwork'),
									data: dataObj,
                  success: function(json) {
                    args.response.success({
                      data: json.createnetworkresponse.network
                    });
                  },
                  error: function(json) {
                    args.response.error(parseXMLHttpResponse(json));
                  }
                });
              },
              messages: {
                notification: function() { return 'label.add.guest.network'; }
              }
            }
          },
          id: 'networks',
          fields: {
            name: { label: 'label.name' },
            account: { label: 'label.account' },            
            type: { label: 'label.type' },            
            cidr: { label: 'label.cidr' }           
          },
          
					advSearchFields: {					 
						zoneid: { 
						  label: 'Zone',							
              select: function(args) {							  					
								$.ajax({
									url: createURL('listZones'),
									data: {
									  listAll: true
									},
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
            						
						domainid: {					
							label: 'Domain',					
							select: function(args) {
								if(isAdmin() || isDomainAdmin()) {
									$.ajax({
										url: createURL('listDomains'),
										data: { 
											listAll: true,
											details: 'min'
										},
										success: function(json) {
											var array1 = [{id: '', description: ''}];
											var domains = json.listdomainsresponse.domain;
											if(domains != null && domains.length > 0) {
												for(var i = 0; i < domains.length; i++) {
													array1.push({id: domains[i].id, description: domains[i].path});
												}
											}
											args.response.success({
												data: array1
											});
										}
									});
								}
								else {
									args.response.success({
										data: null
									});
								}
							},
							isHidden: function(args) {
								if(isAdmin() || isDomainAdmin())
									return false;
								else
									return true;
							}
						},		
						
						account: { 
							label: 'Account',
							isHidden: function(args) {
								if(isAdmin() || isDomainAdmin())
									return false;
								else
									return true;
							}			
						},						
						tagKey: { label: 'Tag Key' },
						tagValue: { label: 'Tag Value' }						
					},
					
					dataProvider: function(args) {
            var data = {};
						listViewDataProvider(args, data);		
						
            $.ajax({
              url: createURL('listNetworks'),
              data: data,			
              async: false,
              success: function(data) {
                args.response.success({
                  data: data.listnetworksresponse.network
                });
              },
              error: function(data) {
                args.response.error(parseXMLHttpResponse(data));
              }
            });
          },

          detailView: {
            name: 'Guest network details',
            viewAll: {
              path: 'network.ipAddresses',
              label: 'label.menu.ipaddresses',
              preFilter: function(args) {
                if (args.context.networks[0].state == 'Destroyed')
                  return false;

                return true;
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
								  var data = {
									  id: args.context.networks[0].id,		
									  name: args.data.name,
										displaytext: args.data.displaytext
									};
								  
                  //args.data.networkdomain is null when networkdomain field is hidden
                  if(args.data.networkdomain != null && args.data.networkdomain != args.context.networks[0].networkdomain) {
									  $.extend(data, {
										  networkdomain: args.data.networkdomain
										});
									} 
                  
                  var oldcidr;
                  $.ajax({
                    url: createURL("listNetworks&id=" + args.context.networks[0].id ),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      oldcidr = json.listnetworksresponse.network[0].cidr;
                      
                    }
                  });


                  if(args.data.cidr !="" && args.data.cidr != oldcidr ){
                    $.extend(data, {
                      guestvmcidr: args.data.cidr
                    });
                  }

                  //args.data.networkofferingid is null when networkofferingid field is hidden
                  if(args.data.networkofferingid != null && args.data.networkofferingid != args.context.networks[0].networkofferingid) {
									  $.extend(data, {
										  networkofferingid: args.data.networkofferingid
										});

                    if(args.context.networks[0].type == "Isolated") { //Isolated network
                      cloudStack.dialog.confirm({
                        message: 'Do you want to keep the current guest network CIDR unchanged?',
                        action: function() { //"Yes"	button is clicked                          
													$.extend(data, {
													  changecidr: false
													});
													
                          $.ajax({
                            url: createURL('updateNetwork'),
                            data: data,
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
													$.extend(data, {
													  changecidr: true
													});
													
                          $.ajax({
                            url: createURL('updateNetwork'),
                            data: data,
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
                    url: createURL('updateNetwork'),
                    data: data,
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
                                        args.$form.find('.form-item[rel=cleanup]').find('input').attr('checked', 'checked'); //checked
                                        args.$form.find('.form-item[rel=cleanup]').css('display', 'inline-block'); //shown
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
              var hasNetworkACL = false;
              var hasSRXFirewall = false;
              var isVPC = false;
              var isAdvancedSGZone = false;
              var hiddenTabs = [];

              // Get network offering data
              $.ajax({
                url: createURL("listNetworkOfferings&id=" + args.context.networks[0].networkofferingid),
                dataType: "json",
                async: false,
                success: function(json) {
                  var networkoffering = json.listnetworkofferingsresponse.networkoffering[0];

                  if (networkoffering.forvpc) {
                    isVPC = true;
                  }

                  $(networkoffering.service).each(function(){
                    var thisService = this;

                    if (thisService.name == 'NetworkACL') {
                      hasNetworkACL = true;
                    } else if (thisService.name == "Lb") {
                      $(thisService.capability).each(function(){
                        if (this.name == "ElasticLb" && this.value == "true") {
                          networkOfferingHavingELB = true;
                        }
                      });
                    }

                    if (thisService.name == 'Firewall') {
                      $(thisService.provider).each(function() {
                        if (this.name == 'JuniperSRX') {
                          hasSRXFirewall = true;

                          return false;
                        }

                        return true;
                      });
                    }
                  });
                }
              });

              // Get zone data
              $.ajax({
                url: createURL('listZones'),
                data: {
                  id: args.context.networks[0].zoneid
                },
                async: false,
                success: function(json) {
                  var zone = json.listzonesresponse.zone[0];

                  isAdvancedSGZone = zone.securitygroupsenabled;
                }
              });

              if (!networkOfferingHavingELB) {
                hiddenTabs.push("addloadBalancer");
              }

              if (isVPC || isAdvancedSGZone || hasSRXFirewall) {
                hiddenTabs.push('egressRules');
               }
              
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
									
									if(!isAdmin()) {
									  hiddenFields.push("vlan");
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

                    ispersistent:{
                      label:'Persistent ',
                      converter:cloudStack.converters.toBooleanText

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

                    networkofferingid: {
                      label: 'label.network.offering',
                      isEditable: true,
                      select: function(args){
											  if (args.context.networks[0].type == 'Shared') { //Shared network is not allowed to upgrade to a different network offering
												  args.response.success({ data: [] });
                          return;
												}
											
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

                    //netmask: { label: 'label.netmask' },
                    cidr: { label: 'label.cidr', isEditable:true },

                    networkcidr:{label:'Network CIDR'},

                    reservediprange:{label:'Reserved IP Range'},


                    networkdomaintext: {
                      label: 'label.network.domain.text'
                    },
                    networkdomain: {
                      label: 'label.network.domain',
                      isEditable: true
                    },

                    domain: { label: 'label.domain' },
                    account: { label: 'label.account' },

                    vpcid: {
                      label: 'label.vpc.id',
                      converter: function(args) {
                        if(args != null)
												  return args;
												else
												  return 'N/A';
                      }											
										}
                  }
                ],

                tags: cloudStack.api.tags({ resourceType: 'Network', contextId: 'networks' }),


                dataProvider: function(args) {
                  $.ajax({
                    url: createURL("listNetworks&id=" + args.context.networks[0].id + "&listAll=true"), //pass "&listAll=true" to "listNetworks&id=xxxxxxxx" for now before API gets fixed.
                    data: { listAll: true },
										dataType: "json",
										async: true,
										success: function(json) {								  
											var jsonObj = json.listnetworksresponse.network[0];  
                      addExtraPropertiesToGuestNetworkObject(jsonObj);												
											args.response.success(
												{
													actionFilter: cloudStack.actionFilter.guestNetwork,
													data: jsonObj
												}
											);		
										}
									});			
                }
              }
            }
          }
        }
      },
      secondaryNicIps: {
        title: 'label.menu.ipaddresses',
        listView: {
          id: 'secondaryNicIps',
          label: 'label.ips',
          fields: {
            virtualmachinedisplayname: { label: 'label.vm.name' },
            ipaddress: {
              label: 'label.ips',
              converter: function(text, item) {
                if (item.issourcenat) {
                  return text + ' [' + _l('label.source.nat') + ']';
                }

                return text;
              }
            }
          },
          actions: {
            add: {
              label: 'label.acquire.new.ip',
              addRow: 'true',
              messages: {
                confirm: function(args) {
                  return 'message.acquire.new.ip';
                },
                notification: function(args) {
                  return 'label.acquire.new.ip';
                }
              },
              action: function(args) {
                var dataObj = {};

                $.ajax({
                  url: createURL('addIpToNic'),
                  data: {
                    nicId: args.context.nics[0].id
                  },
                  success: function(json) {
                    args.response.success({
                      _custom: {
                        getUpdatedItem: function(data) {
                          return $.extend(
                            data.queryasyncjobresultresponse.jobresult.nicsecondaryip,
                            {
                              zoneid: args.context.instances[0].zoneid,
                              virtualmachinedisplayname: args.context.instances[0].displayname
                            }
                          );
                        },
                        jobId: json.addiptovmnicresponse.jobid
                      }
                    });
                  }
                });
              },

              notification: {
                poll: pollAsyncJobResult
              }
            }
          },

          dataProvider: function(args) {
            var data = {};

            $.ajax({
              url: createURL('listNics'),
              data: {
                nicId: args.context.nics[0].id,
                virtualmachineid: args.context.instances[0].id
              },
              success: function(json) {
                var ips = json.listnics.nic ? json.listnics.nic[0].secondaryip : [];

                args.response.success({
                  data: $(ips).map(function(index, ip) {
                    return $.extend(ip, {
                      zoneid: args.context.instances[0].zoneid,
                      virtualmachinedisplayname: args.context.instances[0].displayname
                    });
                  })
                });
              }
            });
          },

          // Detail view
          detailView: {
            name: 'Secondary IP address detail',
            actions: {
              remove: {
                label: 'label.action.release.ip',
                action: function(args) {
                  $.ajax({
                    url: createURL('removeIpFromNic'),
                    data: {
                      id: args.context.secondaryNicIps[0].id
                    },
                    success: function(json) {
                      args.response.success({
                        _custom: { jobId: json.removeipfromnicresponse.jobid }
                      });
                    }
                  });
                },
                messages: {
                  confirm: function(args) {
                    return 'message.action.release.ip';
                  },
                  notification: function(args) {
                    return 'label.action.release.ip';
                  }
                },
                notification: { poll: pollAsyncJobResult }
              }
            },
            tabs: {
              details: {
                title: 'label.details',
                fields: [
                  {
                    ipaddress: { label: 'label.ip' }
                  },
                  {
                    id: { label: 'label.id' },
                    virtualmachinedisplayname: { label: 'label.vm.name' },
                    zonename: { label: 'label.zone.name' }
                  }
                ],

                dataProvider: function(args) {
                  $.ajax({
                    url: createURL('listNics'),
                    data: {
                      nicId: args.context.nics[0].id,
                      virtualmachineid: args.context.instances[0].id
                    },
                    success: function(json) {
                      var ips = json.listnics.nic[0].secondaryip

                      args.response.success({
                        data: $.grep($(ips).map(function(index, ip) {
                          return $.extend(ip, {
                            zonename: args.context.instances[0].zonename,
                            virtualmachinedisplayname: args.context.instances[0].displayname
                          });
                        }), function(ip) {
                          return ip.ipaddress == args.context.secondaryNicIps[0].ipaddress;
                        })[0]
                      });
                    }
                  });
                }
              }
            }
          }
        }
      },
      ipAddresses: {
        type: 'select',
        title: 'label.menu.ipaddresses',
        listView: {
          id: 'ipAddresses',
          label: 'label.ips',
          fields: {
            ipaddress: {
              label: 'label.ips',
              converter: function(text, item) {
                if (item.issourcenat) {
                  return text + ' [' + _l('label.source.nat') + ']';
                }

                return text;
              }
            },
            zonename: { label: 'label.zone' },            
            virtualmachinedisplayname: { label: 'label.vm.name' },
            state: {
              converter: function(str) {
                // For localization
                return str;
              },
              label: 'label.state', indicator: { 'Allocated': 'on', 'Released': 'off' }
            }
          },
          actions: {
            add: {
              label: 'label.acquire.new.ip',
              addRow: 'true',
              preFilter: function(args) {                
								var zoneObj;
 								var dataObj = {};
                
                if ('vpc' in args.context) { //from VPC section
                  $.extend(dataObj, {
                    id: args.context.vpc[0].zoneid
                  });
                } else if ('networks' in args.context) { //from Guest Network section
                  $.extend(dataObj, {
                    id: args.context.networks[0].zoneid
                  });
                }

								$.ajax({
								  url: createURL('listZones'),
									data: dataObj,
								  //	  id: args.context.networks[0].zoneid
								  //	},
									async: false,
									success: function(json) {									  
										zoneObj = json.listzonesresponse.zone[0];										
									}
								});

                if (zoneObj.networktype == 'Advanced' && zoneObj.securitygroupsenabled) {
                  return false;
                }
																							
								if (zoneObj.networktype == 'Basic') {
								  var havingEIP = false, havingELB = false;
								  $.ajax({
									  url: createURL('listNetworkOfferings'),
										data: {
										  id: args.context.networks[0].networkofferingid
										},
										async: false,
										success: function(json) {									  
											$(json.listnetworkofferingsresponse.networkoffering[0].service).each(function(){											 
												var thisService = this;														
												if (thisService.name == "StaticNat") {
													$(thisService.capability).each(function(){
														if (this.name == "ElasticIp" && this.value == "true") {
															havingEIP = true;
															return false; //break $.each() loop
														}
													});
												} else if (thisService.name == "Lb") {
													$(thisService.capability).each(function(){
														if (this.name == "ElasticLb" && this.value == "true") {
															havingELB = true;
															return false; //break $.each() loop
														}
													});
												}
											});			
										}
									});									                	               
									if(havingEIP != true || havingELB != true) { //not EIP-ELB 
										return false;  //acquire new IP is not allowed in non-EIP-ELB basic zone 
									}			
								}
																
								//*** from Guest Network section ***
								if('networks' in args.context) { 
                  if(args.context.networks[0].vpcid == null){ //Guest Network section > non-VPC network, show Acquire IP button
                    return true;
                  } 
									else { //Guest Network section > VPC network, hide Acquire IP button
                    return false;
                  }
                } 								
								//*** from VPC section ***
								else { //'vpc' in args.context
                  return true; //VPC section, show Acquire IP button
                }
              },
              messages: {
                confirm: function(args) {
                  if(args.context.vpc)
                    return 'message.acquire.new.ip.vpc';
                   else
                     return 'message.acquire.new.ip';
                },
                notification: function(args) {
                  return 'label.acquire.new.ip';
                }
              },	
              action: function(args) {                
								var dataObj = {};											
								if('vpc' in args.context) { //from VPC section
								  $.extend(dataObj, {
									  vpcid: args.context.vpc[0].id
									});
								}
								else if('networks' in args.context) { //from Guest Network section
								  $.extend(dataObj, {
									  networkid: args.context.networks[0].id
									});									
									
									if(args.context.networks[0].type == "Shared" && !args.context.projects) {
										$.extend(dataObj, {
											domainid: g_domainid,
											account: g_account
										});
									}									
								}

                $.ajax({
                  url: createURL('associateIpAddress'),
                  data: dataObj,
                  success: function(data) {
                    args.response.success({
                      _custom: {
                        jobId: data.associateipaddressresponse.jobid,
                        getUpdatedItem: function(data) {
                          var newIP = data.queryasyncjobresultresponse.jobresult.ipaddress;
                          return $.extend(newIP, {
                            state: 'Allocated'
                          });
                        },
                        getActionFilter: function() {
                          return actionFilters.ipAddress;
                        }
                      }
                    });
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

          dataProvider: function(args) {
            var data = {};
						listViewDataProvider(args, data);

            if (g_supportELB == "guest") // IPs are allocated on guest network
              $.extend(data, {
                forvirtualnetwork: false,
                forloadbalancing: true
              });
            else if(g_supportELB == "public") // IPs are allocated on public network
              $.extend(data, {
                forvirtualnetwork: true,
                forloadbalancing: true
              });

            if (args.context.networks) {
              $.extend(data, { associatedNetworkId: args.context.networks[0].id });

            }						
            
            if("vpc" in args.context) {
              $.extend(data, {
                vpcid: args.context.vpc[0].id
              });
            }

            $.ajax({
              url: createURL('listPublicIpAddresses'),
              data: data,
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listpublicipaddressesresponse.publicipaddress;

                $(items).each(function() {
                  getExtaPropertiesForIpObj(this, args);
                });

                args.response.success({
                  actionFilter: actionFilters.ipAddress,
                  data: items
                });
              },
              error: function(data) {
                args.response.error(parseXMLHttpResponse(data));
              }
            });
          },

          // Detail view
          detailView: {
            name: 'IP address detail',
            tabFilter: function(args) {
              var item = args.context.ipAddresses[0];

              // Get VPN data
              $.ajax({
                url: createURL('listRemoteAccessVpns'),
                data: {
                  listAll: true,
                  publicipid: item.id
                },
                dataType: 'json',
                async: false,
                success: function(vpnResponse) {
                  var isVPNEnabled = vpnResponse.listremoteaccessvpnsresponse.count;
                  if (isVPNEnabled) {
                    item.vpnenabled = true;
                    item.remoteaccessvpn = vpnResponse.listremoteaccessvpnsresponse.remoteaccessvpn[0];
                  };
                },
                error: function(data) {
                  args.response.error(parseXMLHttpResponse(data));
                }
              });

              var disabledTabs = [];
              var ipAddress = args.context.ipAddresses[0];
              var disableVpn = false, disableIpRules = false;

              if (!ipAddress.vpnenabled) {
                disableVpn = true;
              }
              
              if (ipAddress.issystem == true) {
                disableVpn = true;
                
                if (ipAddress.isstaticnat == true || ipAddress.virtualmachineid != null) {
                  disableIpRules = true;
                }
              }

              if (ipAddress.vpcid && ipAddress.issourcenat) {
                disableIpRules = true;
              }

              if (disableVpn) disabledTabs.push('vpn');
              if (disableIpRules) disabledTabs.push('ipRules');
              
              return disabledTabs;
            },
            actions: {
              enableVPN: {
                label: 'label.enable.vpn',
                action: function(args) {
                  $.ajax({
                    url: createURL('createRemoteAccessVpn'),
                    data: {
                      publicipid: args.context.ipAddresses[0].id,
                      domainid: args.context.ipAddresses[0].domainid,
                      account: args.context.ipAddresses[0].account
                    },
                    dataType: 'json',
                    async: true,
                    success: function(data) {
                      args.response.success({
                        _custom: {
                          getUpdatedItem: function(json) {
                            return {
                              vpn: json.queryasyncjobresultresponse.jobresult.remoteaccessvpn,
                              vpnenabled: true
                            };
                          },
                          getActionFilter: function() {
                            return actionFilters.ipAddress;
                          },
                          jobId: data.createremoteaccessvpnresponse.jobid
                        }
                      });
                    },
                    error: function(data) {
                      args.response.error(parseXMLHttpResponse(data));
                    }
                  });
                },
                messages: {
                  confirm: function(args) {
                    return 'message.enable.vpn';
                  },
                  notification: function(args) {
                    return 'label.enable.vpn';
                  },
                  complete: function(args) {
                    return _l('message.enabled.vpn') + ' ' + args.vpn.publicip + '.' + '<br/>'
                      + _l('message.enabled.vpn.ip.sec') + '<br/>'
                      + args.vpn.presharedkey;
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },
              disableVPN: {
                label: 'label.disable.vpn',
                action: function(args) {
                  $.ajax({
                    url: createURL('deleteRemoteAccessVpn'),
                    data: {
                      publicipid: args.context.ipAddresses[0].id,
                      domainid: args.context.ipAddresses[0].domainid
                    },
                    dataType: 'json',
                    async: true,
                    success: function(data) {
                      args.response.success({
                        _custom: {
                          getUpdatedItem: function(data) {
                            return {
                              vpnenabled: false
                            };
                          },
                          getActionFilter: function() { return actionFilters.ipAddress; },
                          jobId: data.deleteremoteaccessvpnresponse.jobid
                        }
                      });
                    },
                    error: function(data) {
                      args.response.error(parseXMLHttpResponse(data));
                    }
                  });
                },
                messages: {
                  confirm: function(args) {
                    return 'message.disable.vpn';
                  },
                  notification: function(args) {
                    return 'label.disable.vpn';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },
              enableStaticNAT: {
                label: 'label.action.enable.static.NAT',

                action: {
                  noAdd: true,
                  custom: cloudStack.uiCustom.enableStaticNAT({
                    tierSelect: function(args) {
                      if ('vpc' in args.context) { //from VPC section
                        args.$tierSelect.show(); //show tier dropdown

                        $.ajax({ //populate tier dropdown
                          url: createURL("listNetworks"),
                          async: false,
                          data: {
                            vpcid: args.context.vpc[0].id,
                            //listAll: true,  //do not pass listAll to listNetworks under VPC
														domainid: args.context.vpc[0].domainid,
						                account: args.context.vpc[0].account,
                            supportedservices: 'StaticNat'
                          },
                          success: function(json) {
                            var networks = json.listnetworksresponse.network;
                            var items = [{ id: -1, description: 'Please select a tier' }];
                            $(networks).each(function(){
                              items.push({id: this.id, description: this.displaytext});
                            });
                            args.response.success({ data: items });
                          }
                        });
                      }
                      else { //from Guest Network section
                        args.$tierSelect.hide();
                      }

                      args.$tierSelect.change(function() {
                        args.$tierSelect.closest('.list-view').listView('refresh');
                      });
                      args.$tierSelect.closest('.list-view').listView('refresh');
                    },

                    listView: $.extend(true, {}, cloudStack.sections.instances, {
                      listView: {
                        filters: false,
                        subselect: {
                          label: 'label.use.vm.ip',
                          dataProvider: instanceSecondaryIPSubselect
                        },
                        dataProvider: function(args) {
                          var data = {
                            page: args.page,
                            pageSize: pageSize,                            
                            listAll: true
                          };
                          
                          var $tierSelect = $(".ui-dialog-content").find('.tier-select select');
                          
                          // if $tierSelect is not initialized, return; tierSelect() will refresh listView and come back here later 
                          if($tierSelect.size() == 0){
                            args.response.success({ data: null });
                            return;             
                          }

                          if('vpc' in args.context) {
                            if($tierSelect.size() && $tierSelect.val() != '-1' ){ 
                              data.networkid = $tierSelect.val();
                            }                          
                            $.extend(data, {
                              vpcid: args.context.vpc[0].id
                            });
                          }
                          else if('networks' in args.context) {
                            $.extend(data, {
                              networkid: args.context.networks[0].id
                            });
                          }
													
                          if (!args.context.projects) {
                            $.extend(data, {
                              account: args.context.ipAddresses[0].account,
                              domainid: args.context.ipAddresses[0].domainid
                            });
                          }

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
                                      'Destroyed','Expunging'
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
                    action: function(args) {										  
											var data = {
												ipaddressid: args.context.ipAddresses[0].id,
												virtualmachineid: args.context.instances[0].id
											};

                      if (args._subselect && args._subselect != -1) {
                        data.vmguestip = args._subselect;
                      }
											
											if('vpc' in args.context) {
											  if(args.tierID == '-1') {
												  args.response.error('Tier is required');
												  return;
												}											
											  $.extend(data, {
												  networkid: args.tierID
												});
											}
										
                      $.ajax({
                        url: createURL('enableStaticNat'),
                        data: data,
                        dataType: 'json',
                        async: true,
                        success: function(data) {
                          args.response.success({});
                        },
                        error: function(data) {
                          args.response.error(parseXMLHttpResponse(data));
                        }
                      });
                    }
                  })
                },
                messages: {
                  notification: function(args) {
                    return 'label.action.enable.static.NAT';
                  }
                },
                notification: {
                  poll: function(args) {
                    args.complete({
                      data: {
                        isstaticnat: true
                      }
                    });

                    if (args._custom.$detailView.is(':visible')) {
                      ipChangeNotice();
                    }
                  }
                }
              },
              disableStaticNAT: {
                label: 'label.action.disable.static.NAT',
                action: function(args) {
                  $.ajax({
                    url: createURL('disableStaticNat'),
                    data: {
                      ipaddressid: args.context.ipAddresses[0].id
                    },
                    dataType: 'json',
                    async: true,
                    success: function(data) {
                      args.response.success({
                        _custom: {
                          jobId: data.disablestaticnatresponse.jobid,
                          getUpdatedItem: function() {
                            return {
                              isstaticnat: false,
                              virtualmachinedisplayname: ""
                            };
                          },
                          getActionFilter: function() {
                            return function(args) {
                              return ['enableStaticNAT'];
                            };
                          },
                          onComplete: function(args, _custom) {
                            if (_custom.$detailView.is(':visible')) {
                              ipChangeNotice();
                            }
                          }
                        }
                      });
                    },
                    error: function(data) {
                      args.response.error(parseXMLHttpResponse(data));
                    }
                  });
                },
                messages: {
                  confirm: function(args) {
                    return 'message.action.disable.static.NAT';
                  },
                  notification: function(args) {
                    return 'label.action.disable.static.NAT';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },
              remove: {
                label: 'label.action.release.ip',
                action: function(args) {
                  $.ajax({
                    url: createURL('disassociateIpAddress'),
                    data: {
                      id: args.context.ipAddresses[0].id
                    },
                    dataType: 'json',
                    async: true,
                    success: function(data) {
                      args.response.success({
                        _custom: {
                          jobId: data.disassociateipaddressresponse.jobid,
                          getActionFilter: function() {
                            return function(args) {
                              var allowedActions = ['enableStaticNAT'];

                              return allowedActions;
                            };
                          },
                          getUpdatedItem: function(args) {
                            return {
                              state: 'Released'
                            };
                          },
                          onComplete: function() {
                            $(window).trigger('cloudStack.fullRefresh');
                          }
                        }
                      });
                    },
                    error: function(data) {
                      args.response.error(parseXMLHttpResponse(data));
                    }
                  });
                },
                messages: {
                  confirm: function(args) {
                    return 'message.action.release.ip';
                  },
                  notification: function(args) {
                    return 'label.action.release.ip';
                  }
                },
                notification: { poll: pollAsyncJobResult }
              }
            },
            tabs: {
              details: {
                title: 'label.details',
                preFilter: function(args) {
                  var hiddenFields = [];
                  var zoneObj;
                  $.ajax({
                    url: createURL("listZones&id=" + args.context.ipAddresses[0].zoneid),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      zoneObj = json.listzonesresponse.zone[0];
                    }
                  });
                  if(zoneObj.networktype == "Advanced") {
                    hiddenFields.push("issystem");
                    hiddenFields.push("purpose");
                  }
									
									if(!isAdmin()) {                   
                    hiddenFields.push("vlanname");
                  }									
                  return hiddenFields;
                },
                fields: [
                  {
                    ipaddress: { label: 'label.ip' }
                  },
                  {
                    id: { label: 'label.id' },    
                    associatednetworkid: { label: 'label.associated.network.id' },
										networkname: { label: 'label.associated.network' },
                    state: { label: 'label.state' },
										networkid: { label: 'label.network.id' },
                    issourcenat: { label: 'label.source.nat', converter: cloudStack.converters.toBooleanText },
                    isstaticnat: { label: 'label.static.nat', converter: cloudStack.converters.toBooleanText },
                    issystem: { label: 'label.is.system', converter: cloudStack.converters.toBooleanText }, //(basic zone only)
                    purpose: { label: 'label.purpose' }, //(basic zone only) When an IP is system-generated, the purpose it serves can be Lb or static nat.
                    virtualmachinedisplayname: { label: 'label.vm.name' },
                    domain: { label: 'label.domain' },
                    account: { label: 'label.account' },
                    zonename: { label: 'label.zone' },
                    vlanname: { label: 'label.vlan' }
                  }
                ],

                tags: cloudStack.api.tags({ resourceType: 'PublicIpAddress', contextId: 'ipAddresses' }),

                dataProvider: function(args) {
                  var items = args.context.ipAddresses;

                  $.ajax({
                    url: createURL('listPublicIpAddresses'),
                    data: {
                      id: args.context.ipAddresses[0].id
                    },
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var ipObj = json.listpublicipaddressesresponse.publicipaddress[0];
                      getExtaPropertiesForIpObj(ipObj, args);

                      var network = $.grep(
                        args.context.vpc ?
                          args.context.vpc[0].network : args.context.networks,
                        function(network) {
                          return network.id = ipObj.associatednetworkid;
                        })[0];

                      args.response.success({
                        actionFilter: actionFilters.ipAddress,
                        data: $.extend(ipObj, {
                          networkname: network ? network.name : ''
                        })
                      });
                    },
                    error: function(data) {
                      args.response.error(parseXMLHttpResponse(data));
                    }
                  });
                }
              },

              ipRules: { //Configuration tab
                title: 'label.configuration',
                custom: cloudStack.ipRules({
                  preFilter: function(args) {							
                    var disallowedActions = [];
                    if (args.context.ipAddresses[0].isstaticnat)
                      disallowedActions.push("nonStaticNATChart");  //tell ipRules widget to show staticNAT chart instead of non-staticNAT chart.

                    var havingFirewallService = false;
                    var havingPortForwardingService = false;
                    var havingLbService = false;
										var havingVpnService = false;		
										
                    if('networks' in args.context && args.context.networks[0].vpcid == null) { //a non-VPC network from Guest Network section
                      $.ajax({
                        url: createURL('listNetworkOfferings'),
                        data: {
												  listAll: true,
													id: args.context.networks[0].networkofferingid
												},
                        async: false,
                        success: function(json) {
                          var networkoffering = json.listnetworkofferingsresponse.networkoffering[0];
                          $(networkoffering.service).each(function(){
                            var thisService = this;
                            if(thisService.name == "Firewall")
                              havingFirewallService = true;
                            if(thisService.name == "PortForwarding")
                              havingPortForwardingService = true;
                            if(thisService.name == "Lb")
                              havingLbService = true;
                            if(thisService.name == "Vpn")
                              havingVpnService = true;
                          });
                        }
                      });
                    }
                    else { //a VPC network from Guest Network section or from VPC section
                      // Firewall is not supported in IP from VPC section
                      // (because ACL has already supported in tier from VPC section)
                      havingFirewallService = false;
                      disallowedActions.push("firewall");

                      havingVpnService = false; //VPN is not supported in IP from VPC section

                      if(args.context.ipAddresses[0].associatednetworkid == null) { //IP is not associated with any tier yet
                        havingPortForwardingService = true;
                        havingLbService = true;
                      }
                      else { //IP is associated with a tier
                        $.ajax({
                          url: createURL('listNetworks'),
													data: {
													  listAll: true,
														id: args.context.ipAddresses[0].associatednetworkid
													},
                          async: false,
                          success: function(json) {
                            var networkObj = json.listnetworksresponse.network[0];
                            $.ajax({
                              url: createURL("listNetworkOfferings&id=" + networkObj.networkofferingid),
                              async: false,
                              success: function(json) {
                                var networkoffering = json.listnetworkofferingsresponse.networkoffering[0];
                                $(networkoffering.service).each(function(){
                                  var thisService = this;
                                  if(thisService.name == "PortForwarding")
                                    havingPortForwardingService = true;
                                  if(thisService.name == "Lb")
                                    havingLbService = true;
                                });
                              }
                            });
                          }
                        });
                      }
                    }
                    if(args.context.ipAddresses[0].networkOfferingConserveMode == false) {
                      /*
                       (1) If IP is SourceNat, no StaticNat/VPN/PortForwarding/LoadBalancer can be enabled/added.
                       */
                      if (args.context.ipAddresses[0].issourcenat){
                        if(havingFirewallService == false) { //firewall is not supported in IP from VPC section (because ACL has already supported in tier from VPC section)
                          disallowedActions.push("firewall");
                        }

                        disallowedActions.push("portForwarding");
                        disallowedActions.push("loadBalancing");
                      }

                      /*
                       (2) If IP is non-SourceNat, show StaticNat/VPN/PortForwarding/LoadBalancer at first.
                       1. Once StaticNat is enabled, hide VPN/PortForwarding/LoadBalancer.
                       2. If VPN service is supported (i.e. IP comes from Guest Network section, not from VPC section), once VPN is enabled, hide StaticNat/PortForwarding/LoadBalancer.
                       3. Once a PortForwarding rule is added, hide StaticNat/VPN/LoadBalancer.
                       4. Once a LoadBalancer rule is added, hide StaticNat/VPN/PortForwarding.
                       */
                      else { //args.context.ipAddresses[0].issourcenat == false
                        if(havingFirewallService == false)
                          disallowedActions.push("firewall");
                        if(havingPortForwardingService == false)
                          disallowedActions.push("portForwarding");
                        if(havingLbService == false)
                          disallowedActions.push("loadBalancing");

                        if (args.context.ipAddresses[0].isstaticnat) { //1. Once StaticNat is enabled, hide VPN/PortForwarding/LoadBalancer.
                          disallowedActions.push("portForwarding");
                          disallowedActions.push("loadBalancing");
                        }
                        if (havingVpnService && args.context.ipAddresses[0].vpnenabled) { //2. If VPN service is supported (i.e. IP comes from Guest Network section, not from VPC section), once VPN is enabled, hide StaticNat/PortForwarding/LoadBalancer.
                          disallowedActions.push("portForwarding");
                          disallowedActions.push("loadBalancing");
                        }

                        //3. Once a PortForwarding rule is added, hide StaticNat/VPN/LoadBalancer.
                        $.ajax({
                          url: createURL('listPortForwardingRules'),
                          data: {
                            ipaddressid: args.context.ipAddresses[0].id,
                            listAll: true
                          },
                          dataType: 'json',
                          async: false,
                          success: function(json) {
                            // Get instance
                            var rules = json.listportforwardingrulesresponse.portforwardingrule;
                            if(rules != null && rules.length > 0) {
                              disallowedActions.push("loadBalancing");
                            }
                          }
                        });

                        //4. Once a LoadBalancer rule is added, hide StaticNat/VPN/PortForwarding.
                        $.ajax({
                          url: createURL('listLoadBalancerRules'),
                          data: {
                            publicipid: args.context.ipAddresses[0].id,
                            listAll: true
                          },
                          dataType: 'json',
                          async: false,
                          success: function(json) {
                            var rules = json.listloadbalancerrulesresponse.loadbalancerrule;
                            if(rules != null && rules.length > 0) {
                              disallowedActions.push("portForwarding");
                            }
                          }
                        });
                      }
                    }

                    return disallowedActions;
                  },

                  // Firewall rules
                  firewall: {
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
                      'startport': { edit: true, label: 'label.start.port',isOptional: true },
                      'endport': { edit: true, label: 'label.end.port',isOptional: true },
                      'icmptype': { edit: true, label: 'ICMP.type', isDisabled: true },
                      'icmpcode': { edit: true, label: 'ICMP.code', isDisabled: true },
                      'add-rule': {
                        label: 'label.add.rule',
                        addButton: true
                      }
                    },

                    tags: cloudStack.api.tags({ resourceType: 'FirewallRule', contextId: 'multiRule' }),

                    add: {
                      label: 'label.add',
                      action: function(args) {
                        $.ajax({
                          url: createURL('createFirewallRule'),
                          data: $.extend(args.data, {
                            ipaddressid: args.context.ipAddresses[0].id
                          }),
                          dataType: 'json',
                          success: function(data) {
                            args.response.success({
                              _custom: {
                                jobId: data.createfirewallruleresponse.jobid
                              },
                              notification: {
                                label: 'label.add.firewall',
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
                        label: 'label.action.delete.firewall',
                        action: function(args) {
                          $.ajax({
                            url: createURL('deleteFirewallRule'),
                            data: {
                              id: args.context.multiRule[0].id
                            },
                            dataType: 'json',
                            async: true,
                            success: function(data) {
                              var jobID = data.deletefirewallruleresponse.jobid;

                              args.response.success({
                                _custom: {
                                  jobId: jobID
                                },
                                notification: {
                                  label: 'label.action.delete.firewall',
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
                        url: createURL('listFirewallRules'),
                        data: {
                          listAll: true,
                          ipaddressid: args.context.ipAddresses[0].id
                        },
                        dataType: 'json',
                        async: true,
                        success: function(data) {
                          args.response.success({
                            data: data.listfirewallrulesresponse.firewallrule
                          });
                        },
                        error: function(data) {
                          args.response.error(parseXMLHttpResponse(data));
                        }
                      });
                    }
                  },

                  staticNATDataProvider: function(args) {
                    $.ajax({
                      url: createURL('listPublicIpAddresses'),
                      data: {
                        id: args.context.ipAddresses[0].id,
												listAll: true
                      },
                      dataType: 'json',
                      async: true,
                      success: function(data) {											
                        var ipObj = data.listpublicipaddressesresponse.publicipaddress[0];
                        getExtaPropertiesForIpObj(ipObj, args);
												
                        args.response.success({
                          data: ipObj
                        });
                      },
                      error: function(data) {
                        args.response.error(parseXMLHttpResponse(data));
                      }
                    });
                  },

                  vmDataProvider: function(args) {
                    $.ajax({
                      url: createURL('listVirtualMachines'),
                      data: {
                        id: args.context.ipAddresses[0].virtualmachineid,
                        listAll: true
                      },
                      dataType: 'json',
                      async: true,
                      success: function(data) {
                        args.response.success({
                          data: data.listvirtualmachinesresponse.virtualmachine[0]
                        });
                      },
                      error: function(data) {
                        args.response.error(parseXMLHttpResponse(data));
                      }
                    });
                  },

                  vmDetails: cloudStack.sections.instances.listView.detailView,


                  //"NAT Port Range" multiEdit screen for StaticNAT is obsolete in cloudstack 3.0 because createIpForwardingRule/deleteIpForwardingRule/listIpForwardingRules API are obsolete in cloudstack 3.0.
                  //cloudstack 3.0 is using createFirewallRule/listFirewallRules/deleteFirewallRule API for both staticNAT and non-staticNAT .
                  /*
                   staticNAT: {
                   noSelect: true,
                   fields: {
                   'protocol': {
                   label: 'label.protocol',
                   select: function(args) {
                   args.response.success({
                   data: [
                   { name: 'tcp', description: 'TCP' },
                   { name: 'udp', description: 'UDP' }
                   ]
                   });
                   }
                   },
                   'startport': { edit: true, label: 'label.start.port' },
                   'endport': { edit: true, label: 'label.end.port' },
                   'add-rule': {
                   label: 'label.add.rule',
                   addButton: true
                   }
                   },
                   add: {
                   label: 'label.add',
                   action: function(args) {
                   $.ajax({
                   url: createURL('createIpForwardingRule'),
                   data: $.extend(args.data, {
                   ipaddressid: args.context.ipAddresses[0].id
                   }),
                   dataType: 'json',
                   success: function(data) {
                   args.response.success({
                   _custom: {
                   jobId: data.createipforwardingruleresponse.jobid
                   },
                   notification: {
                   label: 'label.add.static.nat.rule',
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
                   label: 'label.remove.rule',
                   action: function(args) {
                   $.ajax({
                   url: createURL('deleteIpForwardingRule'),
                   data: {
                   id: args.context.multiRule[0].id
                   },
                   dataType: 'json',
                   async: true,
                   success: function(data) {
                   var jobID = data.deleteipforwardingruleresponse.jobid;
                   args.response.success({
                   _custom: {
                   jobId: jobID
                   },
                   notification: {
                   label: 'label.remove.static.nat.rule',
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
                   setTimeout(function() {
                   $.ajax({
                   url: createURL('listIpForwardingRules'),
                   data: {
                   listAll: true,
                   ipaddressid: args.context.ipAddresses[0].id
                   },
                   dataType: 'json',
                   async: true,
                   success: function(data) {
                   args.response.success({
                   data: data.listipforwardingrulesresponse.ipforwardingrule
                   });
                   },
                   error: function(data) {
                   args.response.error(parseXMLHttpResponse(data));
                   }
                   });
                   }, 100);
                   }
                   },
                   */


                  // Load balancing rules
                  loadBalancing: {
                    listView: $.extend(true, {}, cloudStack.sections.instances, {
                      listView: {
                        fields: {
                          name: { label: 'label.name' },
                          displayname: { label: 'label.display.name' },
                          zonename: { label: 'label.zone.name' },
                          state: {
                            label: 'label.state',
                            indicator: {
                              'Running': 'on',
                              'Stopped': 'off',
                              'Destroyed': 'off',
                              'Error': 'off'
                            }
                          }
                        },
                        filters: false,
                        dataProvider: function(args) {
                          var itemData = $.isArray(args.context.multiRule) && args.context.multiRule[0]['_itemData'] ?
                            args.context.multiRule[0]['_itemData'] : [];
																											                    
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

                          if (!args.context.projects) {
                            $.extend(data, {
                              account: args.context.ipAddresses[0].account,
                              domainid: args.context.ipAddresses[0].domainid
                            });
                          }

                          $.ajax({
                            url: createURL('listVirtualMachines'),
                            data: data,
                            dataType: 'json',
                            async: true,
                            success: function(data) {
                              var vmData = $.grep(
                                data.listvirtualmachinesresponse.virtualmachine ?
                                  data.listvirtualmachinesresponse.virtualmachine : [],
                                function(instance) {
                                 //Hiding the autoScale VMs
                                 var nonAutoScale =0;
                                 if(instance.displayname == null)
                                    nonAutoScale = 1
                                 else {
                                 if(instance.displayname.match(/AutoScale-LB-/) == null)
                                       nonAutoScale = 1;
                                 else {
                                     if( instance.displayname.match(/AutoScale-LB-/).length)          
                                        nonAutoScale =0;
                                   }   
                                  }         
                                  var isActiveState = $.inArray(instance.state, ['Destroyed','Expunging']) == -1;
                                  var notExisting = !$.grep(itemData, function(item) {
                                    return item.id == instance.id;
                                  }).length;

                                  return nonAutoScale && isActiveState && notExisting;
                                }
                              );

                              args.response.success({
                                data: vmData
                              });
                            },
                            error: function(data) {
                              args.response.error(parseXMLHttpResponse(data));
                            }
                          });
                        }
                      }
                    }),
                    headerFields: {
                      tier: {
                        label: 'label.tier',
                        select: function(args) {
                          if('vpc' in args.context) {
                            var data = {
														  //listAll: true,  //do not pass listAll to listNetworks under VPC
															supportedservices: 'Lb'
														};
														if(args.context.ipAddresses[0].associatednetworkid == null) {
														  $.extend(data, {
															  vpcid: args.context.vpc[0].id,
																domainid: args.context.vpc[0].domainid,
						                    account: args.context.vpc[0].account
															});
														}
														else {
														  $.extend(data, {
															  id: args.context.ipAddresses[0].associatednetworkid
															});
														}			
													
														$.ajax({
															url: createURL("listNetworks"),															
															data: data,															
															success: function(json) {					  
																var networks = json.listnetworksresponse.network;	
																var items = [];
																$(networks).each(function(){																  
																	items.push({id: this.id, description: this.displaytext});
																});
																args.response.success({ data: items });																
															}
														});	 
													}																								
                        }
                      }
                    },
                    multipleAdd: true,

                    fieldPreFilter: function(args) {                        
											var hiddenFields = [];		
											if('vpc' in args.context) { //from VPC section
											  hiddenFields.push('autoScale'); //autoScale is not supported in VPC
											}		
                      else { //from Guest Network section 											  
                        $.ajax({
												  url: createURL('listNetworkOfferings'),
													data: {
													  id: args.context.networks[0].networkofferingid
													},
													async: false,
													success: function(json) {													  
														var serviceArray = json.listnetworkofferingsresponse.networkoffering[0].service;
														var lbProviderArrayIncludesNetscaler = false;
														for(var i = 0; i < serviceArray.length; i++) {
														  if(serviceArray[i].name == "Lb") {
															  var providerArray = serviceArray[i].provider;
																for(var k = 0; k < providerArray.length; k++) {
																  if(providerArray[k].name == "Netscaler") {
																	  lbProviderArrayIncludesNetscaler = true;
																		break;
																	}
																}																					
															  break;
															}															
														}														
														if(lbProviderArrayIncludesNetscaler == false) {
														  hiddenFields.push('autoScale'); //autoScale is not supported in a network that is not using Netscaler provider for LB service (CS-16459)
														}		
													}
												});											  
                      }											
                      return hiddenFields; // Returns fields to be hidden
                    },
                    fields: {
                      'name': { edit: true, label: 'label.name', isEditable: true },
                      'publicport': { edit: true, label: 'label.public.port' },
                      'privateport': { edit: true, label: 'label.private.port' },
                      'algorithm': {
                        label: 'label.algorithm',
                        isEditable: true,
                        select: function(args) {
                          args.response.success({
                            data: [
                              { id: 'roundrobin', name: 'roundrobin', description: _l('label.round.robin') },
                              { id: 'leastconn', name: 'leastconn', description: _l('label.least.connections') },
                              { id: 'source', name: 'source', description: _l('label.source') }
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

                     'health-check':{
                          label:'Health Check',
                          custom:{
                             requireValidation: true ,
                             buttonLabel:'Configure',
                             action:cloudStack.uiCustom.healthCheck()

                           }
                        },

                      'autoScale': {
                        label: 'AutoScale',
                        custom: {
                          requireValidation: true,
                          buttonLabel: 'label.configure',
                          action: cloudStack.uiCustom.autoscaler(cloudStack.autoscaler)
                        }
                      },

                      'add-vm': {
                        label: 'label.add.vms',
                        addButton: true
                      }
                    },

                    tags: cloudStack.api.tags({ resourceType: 'LoadBalancer', contextId: 'multiRule' }),

                    add: {
                      label: 'label.add.vms',
                      action: function(args) {  											  
												var networkid;												
											  if('vpc' in args.context) { //from VPC section
												  if(args.data.tier == null) {													  
														args.response.error('Tier is required');
													  return;
													}												
												  networkid = args.data.tier;		
												}
												else if('networks' in args.context) {	//from Guest Network section										  
													networkid = args.context.networks[0].id;													
												}											
                        var data = {
                          algorithm: args.data.algorithm,
                          name: args.data.name,
                          privateport: args.data.privateport,
                          publicport: args.data.publicport,
													openfirewall: false,	
                          networkid: networkid,													
													publicipid: args.context.ipAddresses[0].id
                        };
											
                        var stickyData = $.extend(true, {}, args.data.sticky);                     

                        $.ajax({
                          url: createURL('createLoadBalancerRule'),
                          data: data,
                          dataType: 'json',
                          async: true,
                          success: function(data) {
                            var itemData = args.itemData;
                            var jobID = data.createloadbalancerruleresponse.jobid;
                            var lbID = data.createloadbalancerruleresponse.id;

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
                                var jobID = data.assigntoloadbalancerruleresponse.jobid;
                                var lbStickyCreated = false;

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
                                        _custom: { jobId: jobID },
                                        complete: function(args) {
                                          if (lbStickyCreated) return;

                                          lbStickyCreated = true;

                                          // Create stickiness policy
                                          if (stickyData &&
                                              stickyData.methodname &&
                                              stickyData.methodname != 'None') {
                                            cloudStack.lbStickyPolicy.actions.add(lbID,
                                                                                  stickyData,
                                                                                  complete, error);
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
                    actions: {
                      edit: {
                        label: 'label.edit',
                        action: function(args) {
                          $.ajax({
                            url: createURL('updateLoadBalancerRule'),
                            data: $.extend(args.data, {
                              id: args.context.multiRule[0].id
                            }),
                            success: function(json) {
                              args.response.success({
                                _custom: { jobId: json.updateloadbalancerruleresponse.jobid },
                                notification: {
                                  label: 'label.edit.lb.rule',
                                  poll: pollAsyncJobResult
                                }
                              });
                            }
                          });
                        }
                      },
                      destroy:  {
                        label: 'label.action.delete.load.balancer',
                        action: function(args) {
                          $.ajax({
                            url: createURL('deleteLoadBalancerRule'),
                            data: {
                              id: args.context.multiRule[0].id
                            },
                            dataType: 'json',
                            async: true,
                            success: function(data) {
                              var jobID = data.deleteloadbalancerruleresponse.jobid;

                              args.response.success({
                                _custom: {
                                  jobId: jobID
                                },
                                notification: {
                                  label: 'label.action.delete.load.balancer',
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
                    itemActions: {
                      add: {
                        label: 'label.add.vms.to.lb',
                        action: function(args) {
                          $.ajax({
                            url: createURL('assignToLoadBalancerRule'),
                            data: {
                              id: args.multiRule.id,
                              virtualmachineids: $.map(args.data, function(elem) {
                                return elem.id;
                              }).join(',')
                            },
                            success: function(json) {
                              args.response.success({
                                notification: {
                                  _custom: {
                                    jobId: json.assigntoloadbalancerruleresponse.jobid
                                  },
                                  desc: 'label.add.vms.to.lb',
                                  poll: pollAsyncJobResult
                                }
                              });
                            },
                            error: function(json) {
                              args.response.error();
                              cloudStack.dialog.notice({ message: parseXMLHttpResponse(json) });
                            }
                          });
                        }
                      },
                      destroy: {
                        label: 'label.remove.vm.from.lb',
                        action: function(args) {
                          $.ajax({
                            url: createURL('removeFromLoadBalancerRule'),
                            data: {
                              id: args.multiRule.id,
                              virtualmachineids: args.item.id
                            },
                            success: function(json) {
                              args.response.success({
                                notification: {
                                  _custom: {
                                    jobId: json.removefromloadbalancerruleresponse.jobid
                                  },
                                  desc: 'label.remove.vm.from.lb',
                                  poll: pollAsyncJobResult
                                }
                              });
                            },
                            error: function(json) {
                              args.response.error();
                              cloudStack.dialog.notice({ message: parseXMLHttpResponse(json) });
                            }
                          });
                        }
                      }
                    },
                    dataProvider: function(args) {
                      var $multi = args.$multi;

                      $.ajax({
                        url: createURL('listLoadBalancerRules'),
                        data: {
                          publicipid: args.context.ipAddresses[0].id,
                          listAll: true
                        },
                        dataType: 'json',
                        async: true,
                        success: function(data) {
                          var loadBalancerData = data.listloadbalancerrulesresponse.loadbalancerrule;
                          var loadVMTotal = loadBalancerData ? loadBalancerData.length : 0;
                          var loadVMCurrent = 0;

                          $(loadBalancerData).each(function() {
                            loadVMCurrent++;
                            var item = this;
                            var stickyData = {};
                            var lbInstances = [];

                            // Passing _hideFields array will disable specified fields for this row
                            //item._hideFields = ['autoScale'];
                            
														$.ajax({
															url: createURL('listAutoScaleVmGroups'),
															data: {
																listAll: true,
																lbruleid: item.id
															},	
                              async: false,															
															success: function(json) {			
                                if(json.listautoscalevmgroupsresponse.autoscalevmgroup != null && json.listautoscalevmgroupsresponse.autoscalevmgroup.length > 0) { //from 'autoScale' button
																  item._hideFields = ['add-vm'];
																}
																else { //from 'add-vm' button
																  item._hideFields = ['autoScale'];
																} 
															}
														});
																												
														// Get sticky data
                            $.ajax({
                              url: createURL('listLBStickinessPolicies'),
                              async: false,
                              data: {
                                listAll: true,
                                lbruleid: item.id
                              },
                              success: function(json) {
                                var stickyPolicy = json.listlbstickinesspoliciesresponse.stickinesspolicies ?
                                      json.listlbstickinesspoliciesresponse.stickinesspolicies[0].stickinesspolicy : null;

                                if (stickyPolicy && stickyPolicy.length) {
                                  stickyPolicy = stickyPolicy[0];

                                  if (!stickyPolicy.methodname) stickyPolicy.methodname = 'None';

                                  stickyData = {
                                    _buttonLabel: stickyPolicy.methodname,
                                    methodname: stickyPolicy.methodname,
                                    stickyName: stickyPolicy.name,
                                    id: stickyPolicy.id,
                                    lbRuleID: item.id
                                  };
                                  $.extend(stickyData, stickyPolicy.params);
                                } else {
                                  stickyData = {
                                    lbRuleID: item.id
                                  };
                                }
                              },
                              error: function(json) {
                                cloudStack.dialog.notice({ message: parseXMLHttpResponse(json) });
                              }
                            });

                            // Get instances
                            $.ajax({
                              url: createURL('listLoadBalancerRuleInstances'),
                              dataType: 'json',
                              async: false,
                              data: {
                                listAll: true,
                                id: item.id
                              },
                              success: function(data) {
                                lbInstances = data.listloadbalancerruleinstancesresponse.loadbalancerruleinstance ?
                                  data.listloadbalancerruleinstancesresponse.loadbalancerruleinstance : [];
																																																	
                                $(lbInstances).each(function() {																  																
																	if(this.displayname.indexOf('AutoScale-LB-') > -1) //autoscale VM is not allowed to be deleted manually. So, hide destroy button
                                    this._hideActions = ['destroy'];	                                  																	
																});                                				
                              },
                              error: function(data) {
                                args.response.error(parseXMLHttpResponse(data));
                              }
                            });

                            $.extend(item, {
                              _itemName: 'displayname',
                              _itemData: lbInstances,
                              _maxLength: {
                                name: 7
                              },
                              sticky: stickyData,
                              autoScale: {
                                lbRuleID: item.id
                              }
                            });
                          });

                          args.response.success({
                            data: loadBalancerData
                          });
                        }
                      });

                      // Check if tiers are present; hide/show header drop-down (begin) ***   
                      //dataProvider() is called when a LB rule is added in multiEdit. However, adding a LB rule might change parent object (IP Address object). So, we have to force to refresh args.context.ipAddresses[0] here
										  $.ajax({
												url: createURL('listPublicIpAddresses'),
												data: {                      
													id: args.context.ipAddresses[0].id,
													listAll: true
												},
												success: function(json) {		                         											
													var ipObj = json.listpublicipaddressesresponse.publicipaddress[0];	
													getExtaPropertiesForIpObj(ipObj, args);
												
													args.context.ipAddresses.shift(); //remove the first element in args.context.ipAddresses										
													args.context.ipAddresses.push(ipObj);
																							
													var $headerFields = $multi.find('.header-fields');	                       												
													if ('vpc' in args.context) {
														if(args.context.ipAddresses[0].associatednetworkid == null) {
															$headerFields.show();
														}
														else {
															$headerFields.hide();
														}
													} 
													else if('networks' in args.context){
														$headerFields.hide();
													}																								
												}
											});											
                      // Check if tiers are present; hide/show header drop-down (end) ***   											
                    }
                  },

                  // Port forwarding rules
                  portForwarding: {
                    headerFields: {
                      tier: {
                        label: 'label.tier',
                        select: function(args) {
													if('vpc' in args.context) {		
                            var data = {
														  //listAll: true,  //do not pass listAll to listNetworks under VPC
															supportedservices: 'PortForwarding'
														};
														if(args.context.ipAddresses[0].associatednetworkid == null) {
														  $.extend(data, {
															  vpcid: args.context.vpc[0].id,
																domainid: args.context.vpc[0].domainid,
						                    account: args.context.vpc[0].account
															});
														}
														else {
														  $.extend(data, {
															  id: args.context.ipAddresses[0].associatednetworkid
															});
														}													
														$.ajax({
															url: createURL("listNetworks"),															
															data: data,															
															success: function(json) {					  
																var networks = json.listnetworksresponse.network;	
																var items = [];
																$(networks).each(function(){																  
																	items.push({id: this.id, description: this.displaytext});
																});
																args.response.success({ data: items });																
															}
														});	 
													}	
                        }
                      }
                    },
                    listView: $.extend(true, {}, cloudStack.sections.instances, {
                      listView: {
                        filters: false,
                        subselect: {
                          label: 'label.use.vm.ip',
                          dataProvider: instanceSecondaryIPSubselect
                        },
                        dataProvider: function(args) {
                          var networkid;
													if('vpc' in args.context) 
													  networkid = args.context.multiData.tier;													
													else 
													  networkid = args.context.ipAddresses[0].associatednetworkid;													
													
													var data = {
                            page: args.page,
                            pageSize: pageSize,
                            listAll: true,
                            networkid: networkid
                          };

                          if (!args.context.projects) {
                            $.extend(data, {
                              account: args.context.ipAddresses[0].account,
                              domainid: args.context.ipAddresses[0].domainid
                            });
                          }

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
                                      'Destroyed','Expunging'
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
                    fields: {
                      //'private-ports': {
                      privateport: {
                        edit: true,
                        label: 'label.private.port',
                        //range: ['privateport', 'privateendport']  //Bug 13427 - Don't allow port forwarding ranges in the CreatePortForwardingRule API
												range: ['privateport', 'privateendport']    //Bug 16344 (restore port range back) (http://bugs.cloudstack.org/browse/CS-16344)
                      },
                      //'public-ports': {
                      publicport: {
                        edit: true,
                        label: 'label.public.port',
                        //range: ['publicport', 'publicendport']  //Bug 13427 - Don't allow port forwarding ranges in the CreatePortForwardingRule API
												range: ['publicport', 'publicendport']    //Bug 16344 (restore port range back) (http://bugs.cloudstack.org/browse/CS-16344)
                      },
                      'protocol': {
                        label: 'label.protocol',
                        select: function(args) {
                          args.response.success({
                            data: [
                              { name: 'tcp', description: 'TCP' },
                              { name: 'udp', description: 'UDP' }
                            ]
                          });
                        }
                      },
                      'add-vm': {
                        label: 'label.add.vm',
                        addButton: true
                      }
                    },

                    tags: cloudStack.api.tags({ resourceType: 'PortForwardingRule', contextId: 'multiRule' }),

                    add: {
                      label: 'label.add.vm',

                      action: function(args) {
                        var data = {
                          ipaddressid: args.context.ipAddresses[0].id,
                          privateport: args.data.privateport,
													privateendport: args.data.privateendport,
                          publicport: args.data.publicport,
													publicendport: args.data.publicendport,
                          protocol: args.data.protocol,
                          virtualmachineid: args.itemData[0].id,
                          openfirewall: false
                        };

                        if (args.itemData[0]._subselect && args.itemData[0]._subselect != -1) {
                          data.vmguestip = args.itemData[0]._subselect;
                        }

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
											
                        $.ajax({
                          url: createURL('createPortForwardingRule'),
                          data: data,                        
                          success: function(data) {
                            args.response.success({
                              _custom: {
                                jobId: data.createportforwardingruleresponse.jobid
                              },
                              notification: {
                                label: 'label.add.port.forwarding.rule',
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
                        label: 'label.remove.pf',
                        action: function(args) {
                          $.ajax({
                            url: createURL('deletePortForwardingRule'),
                            data: {
                              id: args.context.multiRule[0].id
                            },
                            dataType: 'json',
                            async: true,
                            success: function(data) {
                              var jobID = data.deleteportforwardingruleresponse.jobid;

                              args.response.success({
                                _custom: {
                                  jobId: jobID
                                },
                                notification: {
                                  label: 'label.remove.pf',
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
                      var $multi = args.$multi;

                      $.ajax({
                        url: createURL('listPortForwardingRules'),
                        data: {
                          ipaddressid: args.context.ipAddresses[0].id,
                          listAll: true
                        },
                        dataType: 'json',
                        async: true,
                        success: function(data) {
                          // Get instance
                          var portForwardingData = data
                                .listportforwardingrulesresponse.portforwardingrule;
                          var loadTotal = portForwardingData ? portForwardingData.length : 0;
                          var loadCurrent = 0;

                          $(portForwardingData).each(function() {
                            var item = this;

                            item._itemName = '_displayName';

                            $.ajax({
                              url: createURL('listVirtualMachines'),
                              dataType: 'json',
                              async: true,
                              data: {
                                listAll: true,
                                id: item.virtualmachineid
                              },
                              success: function(data) {
                                loadCurrent++;
                                $.extend(item, {
                                  _itemData: $.map(data.listvirtualmachinesresponse.virtualmachine, function(vm) {
                                    return $.extend(vm, {
                                      _displayName: vm.id == vm.displayname ?
                                        (vm.instancename ? vm.instancename : vm.name)
                                      : vm.displayname
                                    });
                                  }),
                                  _context: {
                                    instances: data.listvirtualmachinesresponse.virtualmachine
                                  }
                                });

                                if (loadCurrent == loadTotal) {
                                  args.response.success({
                                    data: portForwardingData
                                  });
                                }
                              }
                            });
                          });
                 							
													// Check if tiers are present; hide/show header drop-down (begin) ***   
													//dataProvider() is called when a PF rule is added in multiEdit. However, adding a LB rule might change parent object (IP Address object). So, we have to force to refresh args.context.ipAddresses[0] here
													$.ajax({
														url: createURL('listPublicIpAddresses'),
														data: {                      
															id: args.context.ipAddresses[0].id,
															listAll: true
														},
														success: function(json) {												  
															var ipObj = json.listpublicipaddressesresponse.publicipaddress[0];														 
											        getExtaPropertiesForIpObj(ipObj, args);													 
													
															args.context.ipAddresses.shift(); //remove the first element in args.context.ipAddresses										
															args.context.ipAddresses.push(ipObj);
																									
															var $headerFields = $multi.find('.header-fields');													
															if ('vpc' in args.context) {
																if(args.context.ipAddresses[0].associatednetworkid == null) {
																	$headerFields.show();
																}
																else {
																	$headerFields.hide();
																}
															} 
															else if('networks' in args.context){
																$headerFields.hide();
															}																								
														}
													});											
													// Check if tiers are present; hide/show header drop-down (end) ***   	
                        },
                        error: function(data) {
                          args.response.error(parseXMLHttpResponse(data));
                        }
                      });
                    }
                  }
                })
              },
              vpn: {
                title: 'VPN',
                custom: function(args) {
                  var ipAddress = args.context.ipAddresses[0].ipaddress;
                  var psk = "";
                  if(args.context.ipAddresses[0].remoteaccessvpn != null)
                    psk = args.context.ipAddresses[0].remoteaccessvpn.presharedkey;

                  return $('<div>')
                    .append(
                      $('<ul>').addClass('info')
                        .append(
                          // VPN IP
                          $('<li>').addClass('ip').html(_l('message.enabled.vpn') + ' ')
                            .append($('<strong>').html(ipAddress))
                        )
                        .append(
                          // PSK
                          $('<li>').addClass('psk').html(_l('message.enabled.vpn.ip.sec') + ' ')
                            .append($('<strong>').html(psk))
                        )
                    ).multiEdit({
                      context: args.context,
                      noSelect: true,
                      fields: {
                        'username': { edit: true, label: 'label.username' },
                        'password': { edit: true, isPassword: true, label: 'label.password' },
                        'add-user': { addButton: true, label: 'label.add.user' }
                      },
                      add: {
                        label: 'label.add.user',
                        action: function(args) {
                          $.ajax({
                            url: createURL('addVpnUser'),
                            data: $.extend(args.data, {
                              domainid: args.context.ipAddresses[0].domainid,
                              account: args.context.ipAddresses[0].account
                            }),
                            dataType: 'json',
                            success: function(data) {
                              args.response.success({
                                _custom: {
                                  jobId: data.addvpnuserresponse.jobid
                                },
                                notification: {
                                  label: 'label.add.vpn.user',
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
                          label: 'label.action.delete.user',
                          action: function(args) {
                            $.ajax({
                              url: createURL('removeVpnUser'),
                              data: {
															  domainid: args.context.multiRule[0].domainid,
																account: args.context.multiRule[0].account,
                                username: args.context.multiRule[0].username    
                              },
                              dataType: 'json',
                              async: true,
                              success: function(data) {
                                var jobID = data.removevpnuserresponse.jobid;

                                args.response.success({
                                  _custom: {
                                    jobId: jobID
                                  },
                                  notification: {
                                    label: 'label.delete.vpn.user',
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
                          url: createURL('listVpnUsers'),
                          data: {
                            domainid: args.context.ipAddresses[0].domainid,
                            account: args.context.ipAddresses[0].account
                          },
                          dataType: 'json',
                          async: true,
                          success: function(data) {
                            args.response.success({
                              data: data.listvpnusersresponse.vpnuser
                            });
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
      securityGroups: {
        type: 'select',
        title: 'label.menu.security.groups',
        id: 'securityGroups',
        listView: {
          id: 'securityGroups',
          label: 'label.menu.security.groups',
          fields: {
            name: { label: 'label.name', editable: true },
            description: { label: 'label.description' },
            domain: { label: 'label.domain' },
            account: { label: 'label.account' }
          },
          actions: {
            add: {
              label: 'label.add.security.group',

              action: function(args) {
                $.ajax({
                  url: createURL('createSecurityGroup'),
                  data: {
                    name: args.data.name,
                    description: args.data.description
                  },
                  success: function(data) {
                    args.response.success({
                      data: data.createsecuritygroupresponse.securitygroup
                    });
                  },

                  error: function(data) {
                    args.response.error(parseXMLHttpResponse(data));
                  }
                });
              },

              notification: {
                poll: function(args) {
                  args.complete({
                    actionFilter: actionFilters.securityGroups
                  });
                }
              },

              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to add ' + args.name + '?';
                },
                notification: function(args) {
                  return 'label.add.security.group';
                }
              },

              createForm: {
                title: 'label.add.security.group',
                desc: 'label.add.security.group',
                fields: {
                  name: { label: 'label.name' },
                  description: { label: 'label.description' }
                }
              }
            }
          },

					advSearchFields: {					  					
						tagKey: { label: 'Tag Key' },
						tagValue: { label: 'Tag Value' }						
					},
					
          dataProvider: function(args) {
            var data = {};
						listViewDataProvider(args, data);						

            $.ajax({
              url: createURL('listSecurityGroups'),
              data: data,              
              success: function(json) {
                var items = json.listsecuritygroupsresponse.securitygroup;
                args.response.success({
                  actionFilter: actionFilters.securityGroups,
                  data: items
                });
              }
            });
          },

          detailView: {
            name: 'Security group details',
            tabs: {
              details: {
                title: 'label.details',
                fields: [
                  {
                    name: { label: 'label.name' }
                  },
                  {
                    id: { label: 'label.id' },
                    description: { label: 'label.description' },
                    domain: { label: 'label.domain' },
                    account: { label: 'label.account' }
                  }
                ],

                tags: cloudStack.api.tags({ resourceType: 'SecurityGroup', contextId: 'securityGroups' }),


                dataProvider: function(args) {
                  $.ajax({
                    url: createURL("listSecurityGroups&id="+args.id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var items = json.listsecuritygroupsresponse.securitygroup;
                      if(items != null && items.length > 0) {
                        args.response.success({
                          actionFilter: actionFilters.securityGroups,
                          data:items[0]
                        });
                      }
                    }
                  });
                }
              },
              ingressRules: {
                title: 'label.ingress.rule',
                custom: cloudStack.uiCustom.securityRules({
                  noSelect: true,
                  noHeaderActionsColumn: true,
                  fields: {
                    'protocol': {
                      label: 'label.protocol',
                      select: function(args) {
                        args.$select.change(function() {
                          var $inputs = args.$form.find('th, td');
                          var $icmpFields = $inputs.filter(function() {
                            var name = $(this).attr('rel');

                            return $.inArray(name, [
                              'icmptype',
                              'icmpcode'
                            ]) > -1;
                          });
                          var $otherFields = $inputs.filter(function() {
                            var name = $(this).attr('rel');

                            return name != 'icmptype' &&
                              name != 'icmpcode' &&
                              name != 'protocol' &&
                              name != 'add-rule' &&
                              name != 'cidr' &&
                              name != 'accountname' &&
                              name != 'securitygroup';
                          });

                          if ($(this).val() == 'icmp') {
                            $icmpFields.show();
                            $otherFields.hide();
                          } else {
                            $icmpFields.hide();
                            $otherFields.show();
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
                    'icmptype': { edit: true, label: 'ICMP.type', isHidden: true },
                    'icmpcode': { edit: true, label: 'ICMP.code', isHidden: true },
                    'cidr': { edit: true, label: 'label.cidr', isHidden: true },
                    'accountname': {
                      edit: true,
                      label: 'label.account.and.security.group',
                      isHidden: true,
                      range: ['accountname', 'securitygroup']
                    },
                    'add-rule': {
                      label: 'label.add',
                      addButton: true
                    }
                  },
                  add: {
                    label: 'label.add',
                    action: function(args) {
                      var data = {
                        securitygroupid: args.context.securityGroups[0].id,
                        protocol: args.data.protocol,
                        domainid: args.context.securityGroups[0].domainid,
                        account: args.context.securityGroups[0].account
                      };

                      // TCP / ICMP
                      if (args.data.icmptype && args.data.icmpcode) { // ICMP
                        $.extend(data, {
                          icmptype: args.data.icmptype,
                          icmpcode: args.data.icmpcode
                        });
                      } else { // TCP
                        $.extend(data, {
                          startport: args.data.startport,
                          endport: args.data.endport
                        });
                      }

                      // CIDR / account
                      if (args.data.cidr) {
                        data.cidrlist = args.data.cidr;
                      } else {
                        data['usersecuritygrouplist[0].account'] = args.data.accountname;
                        data['usersecuritygrouplist[0].group'] = args.data.securitygroup;
                      }

                      $.ajax({
                        url: createURL('authorizeSecurityGroupIngress'),
                        data: data,
                        dataType: 'json',
                        async: true,
                        success: function(data) {
                          var jobId = data.authorizesecuritygroupingressresponse.jobid;

                          args.response.success({
                            _custom: {
                              jobId: jobId
                            },
                            notification: {
                              label: 'label.add.ingress.rule',
                              poll: pollAsyncJobResult
                            }
                          });
                        }
                      });
                    }
                  },
                  actions: {
                    destroy: {
                      label: 'label.remove.rule',
                      action: function(args) {
                        $.ajax({
                          url: createURL('revokeSecurityGroupIngress'),
                          data: {
                            domainid: args.context.securityGroups[0].domainid,
                            account: args.context.securityGroups[0].account,
                            id: args.context.multiRule[0].id
                          },
                          dataType: 'json',
                          async: true,
                          success: function(data) {
                            var jobID = data.revokesecuritygroupingress.jobid;

                            args.response.success({
                              _custom: {
                                jobId: jobID
                              },
                              notification: {
                                label: 'label.remove.ingress.rule',
                                poll: pollAsyncJobResult
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
                  ignoreEmptyFields: true,
                  dataProvider: function(args) {
                    $.ajax({
                      url: createURL('listSecurityGroups'),
                      data: {
                        id: args.context.securityGroups[0].id
                      },
                      dataType: 'json',
                      async: true,
                      success: function(data) {
                        args.response.success({
                          data: $.map(
                            data.listsecuritygroupsresponse.securitygroup[0].ingressrule ?
                              data.listsecuritygroupsresponse.securitygroup[0].ingressrule : [],
                            ingressEgressDataMap
                          )
                        });
                      }
                    });
                  }
                })
              },
              
              egressRules: {
                title: 'label.egress.rule',
                custom: function(args) {
                  var context = args.context;

                  return $('<div>').multiEdit({
                    context: context,
                    noSelect: true,
                    noHeaderActionsColumn: true,
                    fields: {
                      'cidrlist': { edit: true, label: 'label.cidr' },
                      'protocol': {
                        label: 'label.protocol',
                        select: function(args) {
                          args.$select.change(function() {
                            var $inputs = args.$form.find('th, td');
                            var $icmpFields = $inputs.filter(function() {
                              var name = $(this).attr('rel');

                              return $.inArray(name, [
                                'icmptype',
                                'icmpcode'
                              ]) > -1;
                            });
                            var $otherFields = $inputs.filter(function() {
                              var name = $(this).attr('rel');

                              return name != 'cidrlist' &&
                                name != 'icmptype' &&
                                name != 'icmpcode' &&
                                name != 'protocol' &&
                                name != 'add-rule';
                            });

                            if ($(this).val() == 'icmp') {
                              $icmpFields.show();
                              $otherFields.hide();
                            } else {
                              $icmpFields.hide();
                              $otherFields.show();
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
                      'icmptype': { edit: true, label: 'ICMP.type', isHidden: true },
                      'icmpcode': { edit: true, label: 'ICMP.code', isHidden: true },
                      'add-rule': {
                        label: 'label.add',
                        addButton: true
                      }
                    },
                    add: {
                      label: 'label.add',
                      action: function(args) {
                        var data = {
                          protocol: args.data.protocol,
                          cidrlist: args.data.cidrlist,
                          trafficType: 'Egress'
                        };

                        if (args.data.icmptype && args.data.icmpcode) { // ICMP
                          $.extend(data, {
                            icmptype: args.data.icmptype,
                            icmpcode: args.data.icmpcode
                          });
                        } else { // TCP/UDP
                          $.extend(data, {
                            startport: args.data.startport,
                            endport: args.data.endport
                          });
                        }

                        // Get Source NAT IP
                        var sourceNATIP;

                        $.ajax({
                          url: createURL('listPublicIpAddresses'),
                          data: {
                            listAll: true,
                            associatednetworkid: args.context.networks[0].id
                          },
                          async: false,
                          success: function(json) {
                            var ipAddresses = json.listpublicipaddressesresponse.publicipaddress;
                            
                            sourceNATIP = $.grep(ipAddresses, function(ipAddress) {
                              return ipAddress.issourcenat;
                            })[0];
                          }
                        });

                        data.ipaddressid = sourceNATIP.id;

                        $.ajax({
                          url: createURL('createFirewallRule'),
                          data: data,
                          dataType: 'json',
                          async: true,
                          success: function(json) {
                            var jobId = json.createfirewallruleresponse.jobid;

                            args.response.success({
                              _custom: {
                                jobId: jobId
                              },
                              notification: {
                                label: 'label.add.egress.rule',
                                poll: pollAsyncJobResult
                              }
                            });
                          },
                          error: function(json) {
                            args.response.error(parseXMLHttpResponse(json));
                          }
                        });
                      }
                    },
                    actions: {
                      destroy: {
                        label: 'label.remove.rule',
                        action: function(args) {
                          $.ajax({
                            url: createURL('deleteFirewallRule'),
                            data: {
                              id: args.context.multiRule[0].id
                            },
                            dataType: 'json',
                            async: true,
                            success: function(data) {
                              var jobID = data.deletefirewallruleresponse.jobid;

                              args.response.success({
                                _custom: {
                                  jobId: jobID
                                },
                                notification: {
                                  label: 'label.remove.egress.rule',
                                  poll: pollAsyncJobResult
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
                    ignoreEmptyFields: true,
                    dataProvider: function(args) {
                      $.ajax({
                        url: createURL('listFirewallRules'),
                        data: {
                          listAll: true,
                          networkid: args.context.networks[0].id,
                          trafficType: 'Egress'
                        },
                        dataType: 'json',
                        async: true,
                        success: function(json) {
                          var response = json.listfirewallrulesresponse.firewallrule;
                          
                          args.response.success({
                            data: response
                          });
                        }
                      });
                    }
                  });
                }
              }
            },

            actions: {
              remove: {
                label: 'label.action.delete.security.group',
                messages: {
                  confirm: function(args) {
                    return 'message.action.delete.security.group';
                  },
                  notification: function(args) {
                    return 'label.action.delete.security.group';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL('deleteSecurityGroup'),
                    data: {
                      id: args.context.securityGroups[0].id
                    },
                    dataType: 'json',
                    async: true,
                    success: function(data) {
                      args.response.success();
                    },
                    error: function(json) {
                      args.response.error(parseXMLHttpResponse(json));
                    }
                  });
                },

                notification: {
                  poll: function(args) {
                    args.complete({
                      data: { state: 'Destroyed' },
                      actionFilter: actionFilters.securityGroups
                    });
                  }
                }
              }
            }
          }
        }
      },
      vpc: {
        type: 'select',
        title: 'label.vpc',
        id: 'vpc',
        listView: {
          id: 'vpc',
          label: 'label.vpc',
          fields: {
            name: { label: 'label.name' },
            displaytext: { label: 'label.description' },
            zonename: { label: 'label.zone' },
            cidr: { label: 'label.cidr' },
            state: {label: 'label.state', indicator: { 'Enabled': 'on', 'Disabled': 'off'}}
          },
										
					advSearchFields: {
					  name: { label: 'Name' },
						zoneid: { 
						  label: 'Zone',							
              select: function(args) {							  					
								$.ajax({
									url: createURL('listZones'),
									data: {
									  listAll: true
									},
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
            
						domainid: {					
							label: 'Domain',					
							select: function(args) {
								if(isAdmin() || isDomainAdmin()) {
									$.ajax({
										url: createURL('listDomains'),
										data: { 
											listAll: true,
											details: 'min'
										},
										success: function(json) {
											var array1 = [{id: '', description: ''}];
											var domains = json.listdomainsresponse.domain;
											if(domains != null && domains.length > 0) {
												for(var i = 0; i < domains.length; i++) {
													array1.push({id: domains[i].id, description: domains[i].path});
												}
											}
											args.response.success({
												data: array1
											});
										}
									});
								}
								else {
									args.response.success({
										data: null
									});
								}
							},
							isHidden: function(args) {
								if(isAdmin() || isDomainAdmin())
									return false;
								else
									return true;
							}
						},		
						
						account: { 
							label: 'Account',
							isHidden: function(args) {
								if(isAdmin() || isDomainAdmin())
									return false;
								else
									return true;
							}			
						},						
						tagKey: { label: 'Tag Key' },
						tagValue: { label: 'Tag Value' }						
					},					
					
          dataProvider: function(args) {
            var data = {};
						listViewDataProvider(args, data);			

            $.ajax({
              url: createURL('listVPCs'),
              data: data,              
              success: function(json) {
                var items = json.listvpcsresponse.vpc; 
                args.response.success({data:items});
              }
            });						
          },
          actions: {
            add: {
              label: 'label.add.vpc',
              messages: {
                notification: function(args) {
                  return 'label.add.vpc';
                }
              },
              createForm: {
                title: 'label.add.vpc',
                messages: {
                  notification: function(args) {
                    return 'label.add.vpc';
                  }
                },
                fields: {
                  name: {
                    label: 'label.name',
                    docID: 'helpVPCName',
                    validation: { required: true }
                  },
                  displaytext: {
                    label: 'label.description',
                    docID: 'helpVPCDescription',
                    validation: { required: true }
                  },
                  zoneid: {
                    label: 'label.zone',
                    docID: 'helpVPCZone',
                    validation: { required: true },
                    select: function(args) {
                      var data = { listAll: true };
                      $.ajax({
                        url: createURL('listZones'),
                        data: data,
                        success: function(json) {
                          var zones = json.listzonesresponse.zone;
                          var advZones = $.grep(zones, function(zone) {
                            return zone.networktype == 'Advanced' && ! zone.securitygroupsenabled;
                          });
                          args.response.success({
                            data: $.map(advZones, function(zone) {
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
                  cidr: {
                    label: 'label.super.cidr.for.guest.networks',
                    docID: 'helpVPCSuperCIDR',
                    validation: { required: true }
                  },
                  networkdomain: {
                    docID: 'helpVPCDomain',
                    label: 'label.DNS.domain.for.guest.networks'
                  }
                }
              },              
              action: function(args) {										
								var defaultvpcofferingid;
								$.ajax({
								  url: createURL("listVPCOfferings"),
									dataType: "json",
									data: {
									  isdefault: true
									},
								  async: false,
									success: function(json) {
									  defaultvpcofferingid = json.listvpcofferingsresponse.vpcoffering[0].id;
									}
								});
								
								var dataObj = {
									name: args.data.name,
									displaytext: args.data.displaytext,
									zoneid: args.data.zoneid,
									cidr: args.data.cidr,
									vpcofferingid: defaultvpcofferingid
								};
								
								if(args.data.networkdomain != null && args.data.networkdomain.length > 0)
								  $.extend(dataObj, { networkdomain: args.data.networkdomain });								
								
								$.ajax({
                  url: createURL("createVPC"),
                  dataType: "json",
									data: dataObj,
                  async: true,
                  success: function(json) {
                    var jid = json.createvpcresponse.jobid;
                    args.response.success(
                      {_custom:
                        {jobId: jid,
                          getUpdatedItem: function(json) {													  
                            return json.queryasyncjobresultresponse.jobresult.vpc;
                          }
                        }
                      });
                  },
                  error: function(data) {
                      args.response.error(parseXMLHttpResponse(data));
                    }
                });								
              },
             
							notification: {
                poll: pollAsyncJobResult
              }							
							
            },
            configureVpc: {
              label: 'label.configure.vpc',
              textLabel: 'label.configure',
              action: {
                custom: cloudStack.uiCustom.vpc(cloudStack.vpc)
              }
            }
          },									
					
					detailView: {
            name: 'label.details',											
						actions: {
              configureVpc: {
                label: 'label.configure',
                textLabel: 'label.configure',
                action: {
                  custom: cloudStack.uiCustom.vpc(cloudStack.vpc)
                },
                messages: { notification: function() { return ''; } }
              },
							
							edit: {
                label: 'label.edit',
                action: function(args) {            
                  $.ajax({
                    url: createURL('updateVPC'),
                    data: {
										  id: args.context.vpc[0].id,
											name: args.data.name,
											displaytext: args.data.displaytext
										},
                    success: function(json) {
                      var jid = json.updatevpcresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
												    jobId: jid,
                            getUpdatedItem: function(json) {														  
															return json.queryasyncjobresultresponse.jobresult.vpc;
														}
													}													 
                        }
                      );						
                    },
                    error: function(data) {
                      args.response.error(parseXMLHttpResponse(data));
                    }
                  });
                },								
								notification: {
                  poll: pollAsyncJobResult
                }								
              },

              restart: {
                label: 'label.restart.vpc',
                messages: {
                  confirm: function(args) {
                    return 'message.restart.vpc';
                  },
                  notification: function(args) {
                    return 'label.restart.vpc';
                  }
                },
                action: function(args) {								 
                  $.ajax({
                    url: createURL("restartVPC"),
                    data: {
										  id: args.context.vpc[0].id
										},                    
                    success: function(json) {                      
											var jid = json.restartvpcresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
												    jobId: jid,
														getUpdatedItem: function(json) {														  
															return json.queryasyncjobresultresponse.jobresult.vpc;
														}
                          }
                        }
                      );											
                    },
                    error: function(data) {
                      args.response.error(parseXMLHttpResponse(data));
                    }
                  });
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },							
							
              remove: {
                label: 'label.remove.vpc',
                messages: {
                  confirm: function(args) {
                    return 'message.remove.vpc';
                  },
                  notification: function(args) {
                    return 'label.remove.vpc';
                  }
                },
                action: function(args) {								 
                  $.ajax({
                    url: createURL("deleteVPC"),
                    data: {
										  id: args.context.vpc[0].id
										},                    
                    success: function(json) {                      
											var jid = json.deletevpcresponse.jobid;
                      args.response.success(
                        {_custom:
                          {
												    jobId: jid 
                          }
                        }
                      );											
                    },
                    error: function(data) {
                      args.response.error(parseXMLHttpResponse(data));
                    }
                  });
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              }
            },

            tabFilter:function(args) {
                var hiddenTabs=[];
                var isRouterOwner = isAdmin();
                if(!isRouterOwner)
                  hiddenTabs.push("router");
               return hiddenTabs;
            },
						
            tabs: {
              details: {
                title: 'label.details',
                fields: [
                  {				
                    name: { label: 'label.name', isEditable: true }
                  },
                  {
                    displaytext: { label: 'label.description', isEditable: true },
                    account: { label: 'label.account' },
                    domain: { label: 'label.domain' },
                    zonename: { label: 'label.zone' },
                    cidr: { label: 'label.cidr' },
                    networkdomain: { label: 'label.network.domain' },
                    state: { label: 'label.state' },
                    ispersistent:{
                      label:'Persistent ',
                      converter:cloudStack.converters.toBooleanText

                     },
                    restartrequired: {
                      label: 'label.restart.required',
                      converter: function(booleanValue) {
                        if (booleanValue == true) {
                          return "<font color='red'>Yes</font>";
                        }
                        
                        return "No";
                      }
                    },
                    id: { label: 'label.id' }
                  }
                ],

                tags: cloudStack.api.tags({ resourceType: 'Vpc', contextId: 'vpc' }),

                dataProvider: function(args) {		
									$.ajax({
										url: createURL("listVPCs"),
										dataType: "json",
										data: {
										  id: args.context.vpc[0].id
										},
										async: true,
										success: function(json) {
											var item = json.listvpcsresponse.vpc[0];
											args.response.success({data: item});
										}
									});									
								}
              },
              router: {
                title: 'VPC Router Details',
                fields:[
                  {
                    name: {label:'label.name'}
                  },
                  {
                    id:{ label:'label.id'},
                    zonename: { label: 'label.zone'},
                    dns1: {label: 'label.dns'},
                    gateway: {label:'label.gateway'},
                    publicip: {label: 'label.public.ip'},
                    guestipaddress:{ label: 'label.guest.ip'},
                    linklocalip: {label: 'label.linklocal.ip'},
                    state: { label:'label.state'},
                    serviceofferingname: {label:'label.service.offering'},
                    isredundantrouter:{
                      label: 'label.redundant.router',
                      converter: function(booleanValue) {
                        if (booleanValue == true) {
                          return "<font color='red'>Yes</font>";
                        }
                        return "No";
                      }
                    },
                    account: {label:'label.account'},
                    domain: {label: 'label.domain'}
                  }
                ],
                dataProvider: function(args) {
                  $.ajax({
                    url: createURL("listRouters&listAll=true&vpcid=" +args.context.vpc[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var item = json.listroutersresponse.router[0];
                      
                      args.response.success({
                        actionFilter: cloudStack.sections.system.routerActionFilter,
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
      
			vpnCustomerGateway: {
        type: 'select',
        title: 'label.vpn.customer.gateway',
        listView: {
          id: 'vpnCustomerGateway',
          label: 'label.vpn.customer.gateway',
          fields: {
					  name: { label: 'label.name' },
            gateway: { label: 'label.gateway' },
            cidrlist: { label: 'label.CIDR.list' },
            ipsecpsk: { label: 'label.IPsec.preshared.key' }
          },

          dataProvider: function(args) {
					  var data = {};
						listViewDataProvider(args, data);
					
            $.ajax({
              url: createURL('listVpnCustomerGateways'),
              data: data,
              async: true,
              success: function(json) {							  
                var items = json.listvpncustomergatewaysresponse.vpncustomergateway;
                args.response.success({data: items});
              }
            });
          },

          actions: {
            add: {
              label: 'label.add.vpn.customer.gateway',
              messages: {
                notification: function(args) {
                  return 'label.add.vpn.customer.gateway';
                }
              },
              createForm: {
                title: 'label.add.vpn.customer.gateway',
                fields: {
								  name: {
									  label: 'label.name',
                    docID: 'helpVPNGatewayName',
										validation: { required: true }
									},								
									gateway: { 
										label: 'label.gateway',
										validation: { required: true }
									}, 
									cidrlist: { 
										label: 'CIDR list',
										desc: 'Please enter a comma separated list of CIDRs if more than one',
										validation: { required: true }
									},
                  gateway: {
                    label: 'label.gateway',
                    docID: 'helpVPNGatewayGateway',
                    validation: { required: true }
                  },
                  cidrlist: {
                    label: 'label.CIDR.list',
                    desc:'Please enter a comma separated list of CIDRs if more than one',    
                    docID: 'helpVPNGatewayCIDRList',
                    validation: { required: true }
                  },
                  ipsecpsk: {
                    label: 'label.IPsec.preshared.key',
                    docID: 'helpVPNGatewayIPsecPresharedKey',
                    validation: { required: true }
                  },                 								
									
                  //IKE Policy									
									ikeEncryption: {
                    label: 'label.IKE.encryption',
                    docID: 'helpVPNGatewayIKEEncryption',
                    select: function(args) {
                      var items = [];
                      items.push({id: '3des', description: '3des'});
                      items.push({id: 'aes128', description: 'aes128'});
                      items.push({id: 'aes192', description: 'aes192'});
                      items.push({id: 'aes256', description: 'aes256'});             
                      args.response.success({data: items});
                    }
                  },									
									ikeHash: {
                    label: 'label.IKE.hash',
                    docID: 'helpVPNGatewayIKEHash',
                    select: function(args) {
                      var items = [];
                      items.push({id: 'md5', description: 'md5'});
                      items.push({id: 'sha1', description: 'sha1'});               
                      args.response.success({data: items});
                    }
                  },									
									ikeDh: {
                    label: 'label.IKE.DH',
                    docID: 'helpVPNGatewayIKEDH',
                    select: function(args) {
                      var items = [];
                      items.push({id: '', description: 'None'});
                      items.push({id: 'modp1024', description: 'Group 2(modp1024)'});
                      items.push({id: 'modp1536', description: 'Group 5(modp1536)'});										 
                      args.response.success({data: items});
                    }
                  },																
									
									//ESP Policy
                  espEncryption: {
                    label: 'label.ESP.encryption',
                    docID: 'helpVPNGatewayESPLifetime',
                    select: function(args) {
                      var items = [];
                      items.push({id: '3des', description: '3des'});
                      items.push({id: 'aes128', description: 'aes128'});
                      items.push({id: 'aes192', description: 'aes192'});
                      items.push({id: 'aes256', description: 'aes256'});             
                      args.response.success({data: items});
                    }
                  },									
									espHash: {
                    label: 'label.ESP.hash',
                    docID: 'helpVPNGatewayESPHash',
                    select: function(args) {
                      var items = [];
                      items.push({id: 'md5', description: 'md5'});
                      items.push({id: 'sha1', description: 'sha1'});               
                      args.response.success({data: items});
                    }
                  },									
									perfectForwardSecrecy: {
                    label: 'label.perfect.forward.secrecy',
                    docID: 'helpVPNGatewayPerfectForwardSecrecy',
                    select: function(args) {
                      var items = [];
                      items.push({id: '', description: 'None'});
                      items.push({id: 'modp1024', description: 'Group 2(modp1024)'});
                      items.push({id: 'modp1536', description: 'Group 5(modp1536)'});										 
                      args.response.success({data: items});
                    }
                  },																	
									
									ikelifetime: {
                    label: 'label.IKE.lifetime',
                    docID: 'helpVPNGatewayIKELifetime',
                    defaultValue: '86400',
                    validation: { required: false, number: true }
                  },
									esplifetime: {
                    label: 'label.ESP.lifetime',
                    docID: 'helpVPNGatewayESPLifetime',
                    defaultValue: '3600',
                    validation: { required: false, number: true }
                  },
									
									dpd: {
									  label: 'label.dead.peer.detection',
                    docID: 'helpVPNGatewayDeadPeerDetection',
										isBoolean: true,
										isChecked: false
									}                 
                }
              },
              action: function(args) {
							  var data = {
									name: args.data.name,
									gateway: args.data.gateway,
									cidrlist: args.data.cidrlist,
									ipsecpsk: args.data.ipsecpsk,									
									ikelifetime: args.data.ikelifetime,
									esplifetime: args.data.esplifetime,
									dpd: (args.data.dpd == "on")
								};
																
								var ikepolicy = args.data.ikeEncryption + '-' + args.data.ikeHash;
								if(args.data.ikeDh != null && args.data.ikeDh.length > 0)
								  ikepolicy += ';' + args.data.ikeDh;
								
								$.extend(data, {
								  ikepolicy: ikepolicy
								});																
								
								var esppolicy = args.data.espEncryption + '-' + args.data.espHash;
								if(args.data.perfectForwardSecrecy != null && args.data.perfectForwardSecrecy.length > 0)
								  esppolicy += ';' + args.data.perfectForwardSecrecy;
								
								$.extend(data, {
								  esppolicy: esppolicy
								});															
							
                $.ajax({
                  url: createURL('createVpnCustomerGateway'),
                  data: data,
                  dataType: 'json',
                  success: function(json) {
                    var jid = json.createvpncustomergatewayresponse.jobid;
                    args.response.success(
                      {_custom:
                       {
                         jobId: jid,
                         getUpdatedItem: function(json) {
                           return json.queryasyncjobresultresponse.jobresult.vpncustomergateway;
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

          detailView: {
            name: 'label.details',
            actions: {              
              edit: {
                label: 'label.edit',
                action: function(args) {									
									var data = {
									  id: args.context.vpnCustomerGateway[0].id,
										name: args.data.name,
										gateway: args.data.gateway,
										cidrlist: args.data.cidrlist,
										ipsecpsk: args.data.ipsecpsk,									
										ikelifetime: args.data.ikelifetime,
										esplifetime: args.data.esplifetime,
										dpd: (args.data.dpd == "on")
									};
																	
									var ikepolicy = args.data.ikeEncryption + '-' + args.data.ikeHash;
									if(args.data.ikeDh != null && args.data.ikeDh.length > 0)
										ikepolicy += ';' + args.data.ikeDh;
									
									$.extend(data, {
										ikepolicy: ikepolicy
									});																
									
									var esppolicy = args.data.espEncryption + '-' + args.data.espHash;
									if(args.data.perfectForwardSecrecy != null && args.data.perfectForwardSecrecy.length > 0)
										esppolicy += ';' + args.data.perfectForwardSecrecy;
									
									$.extend(data, {
										esppolicy: esppolicy
									});							
																	
                  $.ajax({
                    url: createURL('updateVpnCustomerGateway'),     
                    data: data,										
                    success: function(json) {										 
											var jobId = json.updatecustomergatewayresponse.jobid;
											args.response.success(
												{_custom:
													{
														jobId: jobId,
														getUpdatedItem: function(json) {														  
															var item = json.queryasyncjobresultresponse.jobresult.vpncustomergateway;
															args.response.success({ data: item });
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
                label: 'label.delete.VPN.customer.gateway',
                messages: {
                  confirm: function(args) {
                    return 'message.delete.VPN.customer.gateway';
                  },
                  notification: function(args) {
                    return 'label.delete.VPN.customer.gateway';
                  }
                },
                action: function(args) {								  
                  $.ajax({
                    url: createURL("deleteVpnCustomerGateway"),
                    data: {
										  id: args.context.vpnCustomerGateway[0].id
										},                  
                    success: function(json) {
                      var jid = json.deletevpncustomergatewayresponse.jobid;
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
									  name: { 
										  label: 'label.name',
										  isEditable: true,
					            validation: { required: true } 
										}                    
                  },
                  {
									  gateway: { 
										  label: 'label.gateway',
                      isEditable: true,
					            validation: { required: true }
										},
                    cidrlist: { 
										  label: 'label.CIDR.list',
                      isEditable: true,
					            validation: { required: true } 
										},
                    ipsecpsk: { 
										  label: 'label.IPsec.preshared.key',
                      isEditable: true,
					            validation: { required: true } 
										},   					
										
										//IKE Policy									
										ikeEncryption: {
											label: 'label.IKE.encryption',
											isEditable: true,
											select: function(args) {
												var items = [];
												items.push({id: '3des', description: '3des'});
												items.push({id: 'aes128', description: 'aes128'});
												items.push({id: 'aes192', description: 'aes192'});
												items.push({id: 'aes256', description: 'aes256'});             
												args.response.success({data: items});
											}
										},									
										ikeHash: {
											label: 'label.IKE.hash',
											isEditable: true,
											select: function(args) {
												var items = [];
												items.push({id: 'md5', description: 'md5'});
												items.push({id: 'sha1', description: 'sha1'});               
												args.response.success({data: items});
											}
										},									
										ikeDh: {
											label: 'label.IKE.DH',
											isEditable: true,
											select: function(args) {
												var items = [];
												items.push({id: '', description: 'None'});
												items.push({id: 'modp1024', description: 'Group 2(modp1024)'});
                        items.push({id: 'modp1536', description: 'Group 5(modp1536)'});												 
												args.response.success({data: items});
											}
										},						
										
										//ESP Policy
										espEncryption: {
											label: 'label.ESP.encryption',
											isEditable: true,
											select: function(args) {
												var items = [];
												items.push({id: '3des', description: '3des'});
												items.push({id: 'aes128', description: 'aes128'});
												items.push({id: 'aes192', description: 'aes192'});
												items.push({id: 'aes256', description: 'aes256'});             
												args.response.success({data: items});
											}
										},									
										espHash: {
											label: 'label.ESP.hash',
											isEditable: true,
											select: function(args) {
												var items = [];
												items.push({id: 'md5', description: 'md5'});
												items.push({id: 'sha1', description: 'sha1'});               
												args.response.success({data: items});
											}
										},									
										perfectForwardSecrecy: {
											label: 'label.perfect.forward.secrecy',
											isEditable: true,
											select: function(args) {
												var items = [];
												items.push({id: '', description: 'None'});
												items.push({id: 'modp1024', description: 'Group 2(modp1024)'});
                        items.push({id: 'modp1536', description: 'Group 5(modp1536)'});												 
												args.response.success({data: items});
											}
										},	           
									 
									 	ikelifetime: {
											label: 'label.IKE.lifetime',
											isEditable: true,											
											validation: { required: false, number: true }
										},
										esplifetime: {
											label: 'label.ESP.lifetime',
											isEditable: true,											
											validation: { required: false, number: true }
										},
										
										dpd: {
											label: 'label.dead.peer.detection',											
                      isBoolean: true,
                      isEditable: true,
                      converter:cloudStack.converters.toBooleanText
										},  									 
									 
										id: { label: 'label.id' },
                    domain: { label: 'label.domain' },
                    account: { label: 'label.account' }
                  }
                ],

                dataProvider: function(args) {
                  $.ajax({
                    url: createURL("listVpnCustomerGateways"),
                    data: {
                      id: args.context.vpnCustomerGateway[0].id
                    },
                    success: function(json) {
                      var item = json.listvpncustomergatewaysresponse.vpncustomergateway[0];
											
                      //IKE POlicy											
											var a1 = item.ikepolicy.split('-');  //e.g. item.ikepolicy == '3des-md5' or '3des-md5;modp1024'
											item.ikeEncryption = a1[0];
											if(a1[1].indexOf(';') == -1) {
											  item.ikeHash = a1[1];
											}
											else {
											  var a2 = a1[1].split(';');
												item.ikeHash = a2[0];
												item.ikeDh = a2[1];
											}
											
											//ESP Policy											
											var a1 = item.esppolicy.split('-');  //e.g. item.esppolicy == '3des-md5' or '3des-md5;modp1024'
											item.espEncryption = a1[0];
											if(a1[1].indexOf(';') == -1) {
											  item.espHash = a1[1];
											}
											else {
											  var a2 = a1[1].split(';');
												item.espHash = a2[0];
												item.perfectForwardSecrecy = a2[1];
											}
											
                      args.response.success({data: item});
                    }
                  });
                }
              }
            }
          }
        }
      }
    }
  };
	
  function getExtaPropertiesForIpObj(ipObj, args){	  
		if('networks' in args.context) { //from Guest Network section		
			//get ipObj.networkOfferingConserveMode and ipObj.networkOfferingHavingVpnService from guest network's network offering
			$.ajax({
				url: createURL('listNetworkOfferings'), 
				data: {
					id: args.context.networks[0].networkofferingid  
				},													
				async: false,
				success: function(json) {		
					var networkOfferingObj = json.listnetworkofferingsresponse.networkoffering[0];
					ipObj.networkOfferingConserveMode = networkOfferingObj.conservemode; 
																			
					$(networkOfferingObj.service).each(function(){
						var thisService = this;
						if(thisService.name == "Vpn")
							ipObj.networkOfferingHavingVpnService = true;                          
					});
																			
					if(ipObj.networkOfferingHavingVpnService == true) {														 
						$.ajax({
							url: createURL('listRemoteAccessVpns'), 
							data: {
								listAll: true,
								publicipid: ipObj.id
							},												
							async: false,
							success: function(vpnResponse) {
								var isVPNEnabled = vpnResponse.listremoteaccessvpnsresponse.count;
								if (isVPNEnabled) {
									ipObj.vpnenabled = true;
									ipObj.remoteaccessvpn = vpnResponse.listremoteaccessvpnsresponse.remoteaccessvpn[0];
								};													
							}
						});																	
					}																
				}
			});														
		}											
		else { //from VPC section 											  
			ipObj.networkOfferingConserveMode = false; //conserve mode of IP in VPC is always off, so hardcode it as false											
			ipObj.networkOfferingHavingVpnService = false; //VPN is not supported in IP in VPC, so hardcode it as false													
		}		
	}
	
})(cloudStack, jQuery);

