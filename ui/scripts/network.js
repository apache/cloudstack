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
	
  cloudStack.sections.network = {
    title: 'label.network',
    id: 'network',
    sectionSelect: {
      preFilter: function(args) {
        var havingSecurityGroupNetwork = false;
        var havingBasicZones = false;

        // Get basic zones
        $.ajax({
          url: createURL('listZones'),
          async: false,
          success: function(json) {
            var zones = json.listzonesresponse.zone ?
                  json.listzonesresponse.zone : [];
            var basicZones = $.grep(zones, function(zone) {
              return zone.networktype == 'Basic';
            });
            
            havingBasicZones = basicZones.length ? true : false;
          }
        });
        
        $.ajax({
          url: createURL('listNetworks', { ignoreProject: true }),
          data: {
            supportedServices: 'SecurityGroup',
            listAll: true
          },
          async: false,
          success: function(data) {
            if (data.listnetworksresponse.network != null && data.listnetworksresponse.network.length > 0) {
              havingSecurityGroupNetwork = true;
            }
          }
        });

        var sectionsToShow = ['networks', 'vpnCustomerGateway'];

        if (!havingBasicZones) {
          sectionsToShow.push('vpc');
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
                var basicZoneExists = true; //Modifying the logic behind displaying the tabs depending on the networktype
                $.ajax({
                  url: createURL("listZones"),
                  dataType: "json",
                  async: false,
                  success: function(json) {
                    if(json.listzonesresponse.zone != null && json.listzonesresponse.zone.length > 0) {
                      zoneObjs = json.listzonesresponse.zone;
                      $(zoneObjs).each(function() {
                        if(this.networktype == "Advanced") {
                          basicZoneExists = false; // For any occurence of an Advanced zone with any combination of basic zone , the add guest network tab will be displayed
                          return false; //break each loop
                        }
                      });
                    }
                  }
                })
                return !basicZoneExists; //hide Add guest network button if any basic zone exists
              },

              createForm: {
                title: 'label.add.guest.network',
                desc: 'message.add.guest.network',
                fields: {
                  name: { label: 'label.name', validation: { required: true } },
                  displayText: { label: 'label.display.text', validation: { required: true }},
                  zoneId: {
                    label: 'label.zone',
                    validation: { required: true },


                    select: function(args) {
                      $.ajax({
                        url: createURL('listZones'),
                        success: function(json) {
                          var zones = $.grep(json.listzonesresponse.zone, function(zone) {
                            return zone.networktype == 'Advanced';
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
                    select: function(args) {										 
                      $.ajax({
                        url: createURL('listNetworkOfferings&zoneid=' + args.zoneId),
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
                  },
									vpcid: {
										label: 'VPC',
										dependsOn: 'networkOfferingId',
										select: function(args) {										  
											var networkOfferingObj;
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
													  listAll: true
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
											}
											else {
											  args.$select.closest('.form-item').hide();
											  args.response.success({ data: null });
											}			
										}
									},
                  guestGateway: { label: 'label.guest.gateway' },
                  guestNetmask: { label: 'label.guest.netmask' }
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
            //zonename: { label: 'Zone' },
            type: { label: 'label.type' },
            vlan: { label: 'label.vlan' },
            cidr: { label: 'label.cidr' }
            /*
            state: {
              label: 'State',
                indicator: {
                'Implemented': 'on',
                'Setup': 'on',
                'Allocated': 'on',
                'Destroyed': 'off'
              }
            }
            */
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
              url: createURL("listNetworks&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
              data: {
                listAll: true
              },
              dataType: 'json',
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
                    account: { label: 'label.account' },
										
										vpcid: { 
										  label: 'VPC ID',	
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
                          filters: false,
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
          }
        }
      },
      ipAddresses: {
        type: 'select',
        title: 'label.menu.ipaddresses',
        listView: {
          id: 'ipAddresses',
          label: 'IPs',          
          fields: {
            ipaddress: {
              label: 'IP',
              converter: function(text, item) {
                if (item.issourcenat) {
                  return text + ' [' + _l('label.source.nat') + ']';
                }

                return text;
              }
            },
            zonename: { label: 'label.zone' },
            //vlanname: { label: 'VLAN' },   					  
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
							  if('networks' in args.context) { //from Guest Network section
									if(args.context.networks[0].vpcid == null) //if it's a non-VPC network, show Acquire IP button
										return true;
									else //if it's a VPC network, hide Acquire IP button
										return false;
								}
								else { //from VPC section
								  return true; //show Acquire IP button
								}
              },							
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
            var data = {
              page: args.page,
              pageSize: pageSize
            };

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
											
						if("vpc" in args.context) {
						  $.extend(data, {
							  vpcid: args.context.vpc[0].id
							});									
            }							
						
            $.ajax({
              url: createURL("listPublicIpAddresses&listAll=true&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
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
                        dataProvider: function(args) {
                          var $listView = args.$listView;
                          var data = {
                            page: args.page,
                            pageSize: pageSize,                            
                            listAll: true
                          };

                          // See if tier is selected
                          var $tierSelect = $listView.find('.tier-select select');
                          
                          if ($tierSelect.size() && $tierSelect.val() != '-1') {
                            data.networkid = $tierSelect.val();
                          }
													else {
													  args.response.success({ data: null });
														return;
													}

													if('vpc' in args.context) {
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
                    action: function(args) {										  
											var data = {
												ipaddressid: args.context.ipAddresses[0].id,
												virtualmachineid: args.context.instances[0].id
											};
											
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
									return hiddenFields;								
								},
								
                fields: [
                  {
                    ipaddress: { label: 'IP' }
                  },
                  {
                    id: { label: 'label.id' },    
                    networkid: { label: 'label.network.id' },
                    associatednetworkid: { label: 'label.associated.network.id' },
                    state: { label: 'label.state' },
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

                  // Get network data
                  $.ajax({
                    url: createURL('listPublicIpAddresses'),
                    data: {                      
                      id: args.context.ipAddresses[0].id,
											listAll: true
                    },
                    dataType: "json",
                    async: true,
                    success: function(json) {										  
                      var ipObj = json.listpublicipaddressesresponse.publicipaddress[0];	                      									
											getExtaPropertiesForIpObj(ipObj, args);	
								
                      args.response.success({
												actionFilter: actionFilters.ipAddress,
												data: ipObj
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
												url: createURL("listNetworkOfferings&id=" + args.context.networks[0].networkofferingid),
												dataType: "json",
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
												  url: createURL("listNetworks&id=" + args.context.ipAddresses[0].associatednetworkid),
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
                      'startport': { edit: true, label: 'label.start.port' },
                      'endport': { edit: true, label: 'label.end.port' },
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
                      edit: {
                        label: 'label.edit',

                        // Blank -- edit is just for tags right now
                        action: function(args) {
                          args.response.success({
                            notification: {
                              label: 'Edit firewall rule',
                              poll: function(args) { args.complete(); }
                            }
                          });
                        }
                      },
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
                                  var isActiveState = $.inArray(instance.state, ['Destroyed']) == -1;
                                  var notExisting = !$.grep(itemData, function(item) {
                                    return item.id == instance.id;
                                  }).length;

                                  return isActiveState && notExisting;
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
                        label: 'Tier',
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
                              },
                              error: function(data) {
                                args.response.error(parseXMLHttpResponse(data));
                              }
                            });

                            $.extend(item, {
                              _itemName: '_displayName',
                              _itemData: $.map(lbInstances, function(vm) {
                                return $.extend(vm, {
                                  _displayName: vm.id == vm.displayname ?
                                    (vm.instancename ? vm.instancename : vm.name)
                                    : vm.displayname
                                });
                              }),
                              _maxLength: {
                                name: 7
                              },
                              sticky: stickyData
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
                        label: 'Tier',
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
                    fields: {
                      //'private-ports': {
                      privateport: {
                        edit: true,
                        label: 'label.private.port'
                        //range: ['privateport', 'privateendport']  //Bug 13427 - Don't allow port forwarding ranges in the CreatePortForwardingRule API
                      },
                      //'public-ports': {
                      publicport: {
                        edit: true,
                        label: 'label.public.port'
                        //range: ['publicport', 'publicendport']  //Bug 13427 - Don't allow port forwarding ranges in the CreatePortForwardingRule API
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
													publicport: args.data.publicport,
													protocol: args.data.protocol,		
													virtualmachineid: args.itemData[0].id,
                          openfirewall: false													
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
											
                        $.ajax({
                          url: createURL('createPortForwardingRule'),
                          data: data,                        
                          success: function(data) {
                            args.response.success({
                              _custom: {
                                jobId: data.createportforwardingruleresponse.jobid
                              },
                              notification: {
                                label: 'Add port forwarding rule',
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
                      edit: {
                        label: 'label.edit',

                        // Blank -- edit is just for tags right now
                        action: function(args) {
                          args.response.success({
                            notification: {
                              label: 'label.edit.pf',
                              poll: function(args) { args.complete(); }
                            }
                          });
                        }
                      },
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
                                username: args.context.multiRule[0].username,
                                id: args.context.multiRule[0].domainid
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
              url: createURL("listSecurityGroups&listAll=true&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
              dataType: "json",
              async: true,
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
                    id: { label: 'ID' },
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
                    'cidr': { edit: true, label: 'CIDR', isHidden: true },
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
                            function(elem) {
                              return {
                                id: elem.ruleid,
                                protocol: elem.protocol,
                                startport: elem.startport ? elem.startport : elem.icmptype,
                                endport: elem.endport ? elem.endport : elem.icmpcode,
                                cidr: elem.cidr ? elem.cidr : ''.concat(elem.account, ' - ', elem.securitygroupname)
                              };
                            }
                          )
                        });
                      }
                    });
                  }
                })
              },
              egressRules: {
                title: 'label.egress.rule',
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
                    'cidr': { edit: true, label: 'CIDR', isHidden: true },
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
                        url: createURL('authorizeSecurityGroupEgress'),
                        data: data,
                        dataType: 'json',
                        async: true,
                        success: function(data) {
                          var jobId = data.authorizesecuritygroupegressresponse.jobid;

                          args.response.success({
                            _custom: {
                              jobId: jobId
                            },
                            notification: {
                              label: 'label.add.egress.rule',
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
                          url: createURL('revokeSecurityGroupEgress'),
                          data: {
                            domainid: args.context.securityGroups[0].domainid,
                            account: args.context.securityGroups[0].account,
                            id: args.context.multiRule[0].id
                          },
                          dataType: 'json',
                          async: true,
                          success: function(data) {
                            var jobID = data.revokesecuritygroupegress.jobid;

                            args.response.success({
                              _custom: {
                                jobId: jobID
                              },
                              notification: {
                                label: 'label.remove.egress.rule',
                                poll: pollAsyncJobResult
                              }
                            });
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
                            data.listsecuritygroupsresponse.securitygroup[0].egressrule ? 
                              data.listsecuritygroupsresponse.securitygroup[0].egressrule : [],
                            function(elem) {
                              return {
                                id: elem.ruleid,
                                protocol: elem.protocol,
                                startport: elem.startport ? elem.startport : elem.icmptype,
                                endport: elem.endport ? elem.endport : elem.icmpcode,
                                cidr: elem.cidr ? elem.cidr : ''.concat(elem.account, ' - ', elem.securitygroupname)
                              };
                            }
                          )
                        });
                      }
                    });
                  }
                })
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
        title: 'VPC',
        id: 'vpc',
        listView: {
          id: 'vpc',
          label: 'VPC',
          fields: {
            name: { label: 'label.name' },                  
						displaytext: { label: 'label.description' },										
						zonename: { label: 'label.zone' },
						cidr: { label: 'label.cidr' }						
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
              url: createURL("listVPCs&listAll=true&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listvpcsresponse.vpc; 
                args.response.success({data:items});
              }
            });						
          },
          actions: {
            add: {
              label: 'Add VPC',							
							messages: {								
								notification: function(args) {
									return 'Add VPC';
								}
							},
              createForm: {
                title: 'Add VPC',
								messages: {
									notification: function(args) { 
										return 'Add VPC'; 
									}
								},
                fields: {
                  name: { 
									  label: 'label.name', 
										validation: { required: true } 
									},   
                  displaytext: {
                    label: 'label.description',
										validation: { required: true } 
                  },									
									zoneid: {
                    label: 'Zone',
                    validation: { required: true },
                    select: function(args) {
                      var data = { listAll: true };
                      $.ajax({
                        url: createURL('listZones'),
                        data: data,
                        success: function(json) {
                          var zones = json.listzonesresponse.zone;													
													var advZones = $.grep(zones, function(zone) {													  
													  return zone.networktype == 'Advanced';
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
									  label: 'label.cidr',
										validation: { required: true } 
									},		
									networkdomain: { 
									  label: 'label.network.domain'
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
                      }
                    );
                  }
                });								
              },
             
							notification: {
                poll: pollAsyncJobResult
              }							
							
            },
            configureVpc: {
              label: 'Configure VPC',
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
                label: 'Edit VPC',
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
                label: 'restart VPC',
                messages: {
                  confirm: function(args) {
                    return 'Please confirm that you want to restart the VPC';
                  },
                  notification: function(args) {
                    return 'restart VPC';
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
                label: 'remove VPC',
                messages: {
                  confirm: function(args) {
                    return 'Please confirm that you want to delete the VPC';
                  },
                  notification: function(args) {
                    return 'remove VPC';
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
              }
            }
          }								
        }
      },	
      
			vpnCustomerGateway: {
        type: 'select',
        title: 'VPN Customer Gateway',
        listView: {
          id: 'vpnCustomerGateway',
          label: 'VPN Customer Gateway',
          fields: {
            name: { label: 'label.name' },
						gateway: { label: 'label.gateway' },
            cidrlist: { label: 'CIDR list' },
						ipsecpsk: { label: 'IPsec Preshared-Key' }
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
              url: createURL("listVpnCustomerGateways&listAll=true&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
              dataType: "json",
              async: true,
              success: function(json) {							  
                var items = json.listvpncustomergatewaysresponse.vpncustomergateway;
                args.response.success({data: items});
              }
            });
          },
										
					actions: {
						add: {
							label: 'add VPN Customer Gateway',
							messages: {                
								notification: function(args) {
									return 'add VPN Customer Gateway';
								}
							},
							createForm: {
								title: 'add VPN Customer Gateway',
								fields: {		
                  name: {
									  label: 'label.name',
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
											items.push({id: '3des-sha1', description: '3des-sha1'});
											items.push({id: 'aes-sha1', description: 'aes-sha1'});
											items.push({id: 'aes128-sha1', description: 'aes128-sha1'});											
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
											items.push({id: '3des-sha1', description: '3des-sha1'});
											items.push({id: 'aes-sha1', description: 'aes-sha1'});
											items.push({id: 'aes128-sha1', description: 'aes128-sha1'});											
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
								$.ajax({
									url: createURL('createVpnCustomerGateway'),
									data: {
									  name: args.data.name,
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
							remove: {
                label: 'delete VPN Customer Gateway',
                messages: {
                  confirm: function(args) {
                    return 'Please confirm that you want to delete this VPN Customer Gateway';
                  },
                  notification: function(args) {
                    return 'delete VPN Customer Gateway';
                  }
                },
                action: function(args) {								  
                  $.ajax({
                    url: createURL("deleteVpnCustomerGateway"),
                    data: {
										  id: args.context.vpnCustomerGateway[0].id
										},                  
                    success: function(json) {
                      var jid = json.deletecustomergatewayresponse.jobid;
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
                    gateway: { label: 'label.gateway' }
									},
									{
										cidrlist: { label: 'CIDR list' },
										ipsecpsk: { label: 'IPsec Preshared-Key' }, 										
										id: { label: 'label.id' },
                                                                                ikepolicy: { label: 'IKE policy'},
                                                                                esppolicy:{ label: 'ESP policy'},
                                                                                lifetime :{label: 'Lifetime (second)'},
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

