(function(cloudStack, $, testData) {
  var actionFilters = {
    ipAddress: function(args) {
      var allowedActions = args.context.actions;
      var disallowedActions = [];
      var item = args.context.item;
      var status = item.state;

      if (status == 'Released') {
        return [];
      }

      if (status == 'Destroyed' ||
          status == 'Releasing' ||
          status == 'Released' ||
          status == 'Creating' ||
          status == 'Allocating' ||
          item.account == 'system' ||
					item.iselastic == true ) {
        disallowedActions = allowedActions;
      }

      if (item.isstaticnat) {
        disallowedActions.push('enableStaticNAT');
      } else {
        disallowedActions.push('disableStaticNAT');
      }

      if (item.vpnenabled) {
        disallowedActions.push('enableVPN');
      } else {
        disallowedActions.push('disableVPN');
      }

      if (item.issourcenat){
        disallowedActions.push('enableStaticNAT');
        disallowedActions.push('disableStaticNAT');
        disallowedActions.push('destroy');
      } else {
        disallowedActions.push('enableVPN');
        disallowedActions.push('disableVPN');
      }

      allowedActions = $.grep(allowedActions, function(item) {
        return $.inArray(item, disallowedActions) == -1;
      });

      return allowedActions;
    },

    securityGroups: function(args) {
      var allowedActions = [];
      var isSecurityGroupOwner = isAdmin() ||
        args.context.item.account == args.context.users[0].account;

      if (isSecurityGroupOwner &&
          args.context.item.state != 'Destroyed' &&
          args.context.item.name != 'default') {
        allowedActions.push('destroy');
      }

      return allowedActions;
    }
  };

	var securityGroupNetworkObjs = [];
	var isolatedSourcenatNetworkObjs = [];	
	var sharedLbStaticnatSgNetworkObjs = [];
	
  cloudStack.sections.network = {
    title: 'Network',
    id: 'network',
    sectionSelect: {
      preFilter: function(args) {       
        $.ajax({
          url: createURL('listNetworks'),
          data: {
            supportedServices: 'SecurityGroup',
						listAll: true
          },
          async: false,
          success: function(data) {            
            if (data.listnetworksresponse.network && data.listnetworksresponse.network.length) {
              securityGroupNetworkObjs = data.listnetworksresponse.network;
            }
          }
        });
				
				$.ajax({
					url: createURL('listNetworks'),
					data: {
						type: 'isolated',
						supportedServices: 'SourceNat',
						listAll: true
					},
					async: false,
					success: function(data) {					 
						if (data.listnetworksresponse.network != null && data.listnetworksresponse.network.length > 0) {
							isolatedSourcenatNetworkObjs = data.listnetworksresponse.network;
						}
					}
				});
				
        $.ajax({
					url: createURL('listNetworks'),
					data: {
						type: 'shared',
						supportedServices: 'Lb,StaticNat,SecurityGroup',
						listAll: true
					},
					async: false,
					success: function(data) {					 
						if (data.listnetworksresponse.network != null && data.listnetworksresponse.network.length > 0) {
							sharedLbStaticnatSgNetworkObjs = data.listnetworksresponse.network;
						}
					}
				});				
						
				var sectionsToShow = [];
				if(securityGroupNetworkObjs.length > 0) //if there is securityGroup networks
				  sectionsToShow.push('securityGroups'); //show securityGroup section
					
				if(isolatedSourcenatNetworkObjs.length > 0 || sharedLbStaticnatSgNetworkObjs.length > 0) //if there is isolatedSourceNat networks or sharedLbStaticnatSg networks
				  sectionsToShow.push('networks');  //show network section
					
				if(sectionsToShow.length == 0) //if no securityGroup networks, nor isolatedSourceNat networks, nor sharedLbStaticnatSg networks
				  sectionsToShow.push('networks'); // still show network section (no networks in the grid though)
				return sectionsToShow;					
      },
			
      label: 'Select view'
    },
    sections: {
      networks: {
        id: 'networks',
        type: 'select',
        title: 'Guest Networks',
        listView: {
          actions: {
            add: {
              label: 'Add guest network',
              createForm: {
                title: 'Add new guest network',
                desc: 'Please specify name and zone for this network; note that network will be isolated and source NAT-enabled.',
                fields: {
                  name: { label: 'Name', validation: { required: true } },
                  displayText: { label: 'Display Text', validation: { required: true }},
                  zoneId: {
                    label: 'Zone',
                    validation: { required: true },
                    select: function(args) {
                      $.ajax({
                        url: createURL('listZones'),
                        data: {
                          type: 'Advanced'
                        },
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
                    label: 'Network Offering',
                    validation: { required: true },
                    select: function(args) {
                      $.ajax({
                        url: createURL('listNetworkOfferings'),
                        data: {
                          supportedServices: 'SourceNat',
                          type: 'isolated',
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
                  }
                }
              },
              action: function(args) {
                $.ajax({
                  url: createURL('createNetwork'),
                  data: args.data,
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
                notification: function() { return 'Added guest network'; }
              }
            }
          },
          id: 'networks',
          fields: {
            name: { label: 'Name' },
            accounts: { label: 'Account' },
            zonename: { label: 'Zone' },
            type: { label: 'Type' },
            vlan: { label: 'VLAN' },
            cidr: { label: 'CIDR' },
            state: { label: 'State', indicator: {
              'Implemented': 'on', 'Setup': 'on', 'Allocated': 'on',
              'Destroyed': 'off'
            } }
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

            //temporary until Alena fixes listNetworks API to return both isolated and shared networks in one call		
						var networkObjs1 = [];
						$.ajax({
              url: createURL("listNetworks&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
              data: {
                type: 'isolated',
                supportedServices: 'SourceNat',
								listAll: true
              },
              dataType: 'json',
              async: false,
              success: function(data) {
							  if(data.listnetworksresponse.network != null && data.listnetworksresponse.network.length > 0)
                  networkObjs1 = data.listnetworksresponse.network;  
              },
              error: function(data) {
                args.response.error(parseXMLHttpResponse(data));
              }
            });							
						var networkObjs2 = [];
						$.ajax({
							url: createURL("listNetworks&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
							data: {
								type: 'shared',
								supportedServices: 'Lb,StaticNat,SecurityGroup',
								listAll: true
							},
							async: false,
							success: function(data) {			
                if(data.listnetworksresponse.network != null && data.listnetworksresponse.network.length > 0)							
								  networkObjs2 = data.listnetworksresponse.network; 								
							}
						});				
							
						var networkObjs = $.extend(networkObjs1, networkObjs2);						
						args.response.success({
							data: networkObjs
						});		
          },

          detailView: {
            name: 'Guest network details',
            viewAll: {
							path: 'network.ipAddresses',
							label: 'IP addresses',
              preFilter: function(args) {
                if (args.context.networks[0].state == 'Destroyed') return false;

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
                  if(args.data.networkofferingid != null && args.data.networkofferingid != args.context.networks[0].networkofferingid)
                    array1.push("&networkofferingid=" + todb(args.data.networkofferingid));

                  //args.data.networkdomain is null when networkdomain field is hidden
                  if(args.data.networkdomain != null && args.data.networkdomain != args.context.networks[0].networkdomain)
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
								createForm: {
									title: 'Restart network',
									desc: 'Please confirm that you want to restart network',
									fields: {                 
										cleanup: {
											label: 'Clean up',
											isBoolean: true,                   
											isChecked: false
										}                  
									}
								},										
								messages: {
									confirm: function(args) {
										return 'Please confirm that you want to restart network';
									},
									notification: function(args) {
										return 'Restarting network';
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

              destroy: {
                label: 'Delete network',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to delete network ?';
                  },
                  notification: function(args) {
                    return 'Deleting network';
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
                            return { state: 'Destroyed' }; //nothing in this network needs to be updated, in fact, this whole template has being deleted
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
						
						tabFilter: function(args) {		
							var showLbTab = false;
							$(args.context.networks[0].service).each(function(){			
								if(this.name == "Lb") {
								  $(this.capability).each(function() {									  
										if(this.name == "ElasticLb") {										  
											showLbTab = true;
											return false; //break $.each loop
										}										
									});		
									return false; //break $.each loop
                }									
							});		
							
              var hiddenTabs = [];  							
							if(showLbTab == false)
							  hiddenTabs.push("loadBalancer"); 							
              return hiddenTabs;
            },

            isMaximized: true,
            tabs: {
              details: {
                title: 'Details',
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
                    //hiddenFields.push("netmask");
                  }

                  if(args.context.networks[0].type == "Isolated") {
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
                    //networkofferingdisplaytext: { label: 'Network offering' },
                    networkofferingid: {
                      label: 'Network offering',
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
                  args.response.success({
                    actionFilter: function(args) {
                      if (args.context.networks[0].state == 'Destroyed')
                        return [];

                      return args.context.actions;
                    },
                    data: args.context.networks[0]
                  });
                }
              },

              loadBalancer: {
                title: 'Load Balancer',
                custom: function(args) {
                  var context = args.context;
                  
                  return $('<div>').multiEdit(
                    {
                      context: context,
                      listView: $.extend(true, {}, cloudStack.sections.instances, {
                        listView: {
                          dataProvider: function(args) {
                            $.ajax({
                              url: createURL('listVirtualMachines'),
                              data: {
                                networkid: args.context.networks[0].id
                              },
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
                        'name': { edit: true, label: 'Name' },
                        'publicport': { edit: true, label: 'Public Port' },
                        'privateport': { edit: true, label: 'Private Port' },
                        'algorithm': {
                          label: 'Algorithm',
                          select: function(args) {
                            args.response.success({
                              data: [
                                { name: 'roundrobin', description: 'Round-robin' },
                                { name: 'leastconn', description: 'Least connections' },
                                { name: 'source', description: 'Source' }
                              ]
                            });
                          }
                        },
                        'sticky': {
                          label: 'Sticky Policy',
                          custom: {
                            buttonLabel: 'Configure',
                            action: cloudStack.lbStickyPolicy()
                          }
                        },
                        'add-vm': {
                          label: 'Add VMs',
                          addButton: true
                        }
                      },
                      add: {
                        label: 'Add VMs',
                        action: function(args) {
                          var openFirewall = g_firewallRuleUiEnabled == "true" ? false : true;
                          var data = {
                            algorithm: args.data.algorithm,
                            name: args.data.name,
                            privateport: args.data.privateport,
                            publicport: args.data.publicport
                          };
                          var stickyData = $.extend(true, {}, args.data.sticky);
													
												  var apiCmd = "createLoadBalancerRule";		
												  //if(args.context.networks[0].type == "Shared") 
												  apiCmd += "&domainid=" + g_domainid + "&account=" + g_account;
												  //else //args.context.networks[0].type == "Isolated"
												  //apiCmd += "&account=" + args.context.users[0].account;
                          //apiCmd += '&domainid=' + args.context.users[0].domainid;
												  
                          $.ajax({
                            url: createURL(apiCmd),
                            data: $.extend(data, {
                              openfirewall: openFirewall,                            
                              networkid: args.context.networks[0].id
                            }),
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
																    fullRefresh: true,
                                    _custom: {
                                      jobId: jobID
                                    },
                                    notification: {
                                      label: 'Add load balancer rule',
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
                                            
                                            // Create stickiness policy
                                            if (stickyData &&
                                                stickyData.methodname &&
                                                stickyData.methodname != 'none') {
                                              var stickyURLData = '';
                                              var stickyParams;

                                              switch (stickyData.methodname) {
                                              case 'LbCookie':
                                                stickyParams = ['name', 'mode', 'nocache', 'indirect', 'postonly', 'domain'];
                                                break;
                                              case 'AppCookie':
                                                stickyParams = ['name', 'length', 'holdtime', 'request-learn', 'prefix', 'mode'];
                                                break;
                                              case 'SourceBased':
                                                stickyParams = ['tablesize', 'expire'];
                                                break;
                                              }

                                              $(stickyParams).each(function(index) {
                                                var param = '&param[' + index + ']';
                                                var name = this;
                                                var value = stickyData[name];

                                                if (!value) return true;
                                                if (value == 'on') value = true;
                                                
                                                stickyURLData += param + '.name=' + name + param + '.value=' + value;
                                              });

                                              $.ajax({
                                                url: createURL('createLBStickinessPolicy' + stickyURLData),
                                                data: {
                                                  lbruleid: args.data.loadbalancer.id,
                                                  name: stickyData.name,
                                                  methodname: stickyData.methodname
                                                },
                                                success: function(json) {
                                                  var addStickyCheck = setInterval(function() {
                                                    pollAsyncJobResult({
                                                      _custom: {
                                                        jobId: json.createLBStickinessPolicy.jobid,
                                                      },
                                                      complete: function(args) {
                                                        complete();
                                                        clearInterval(addStickyCheck);
                                                      },
                                                      error: function(args) {
                                                        complete();
                                                        cloudStack.dialog.notice({ message: args.message });
                                                        clearInterval(addStickyCheck);
                                                      }
                                                    });                                                  
                                                  }, 1000);
                                                },
                                                error: function(json) {
                                                  complete();
                                                  cloudStack.dialog.notice({ message: parseXMLHttpResponse(json) });
                                                }
                                              });
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
                        destroy:  {
                          label: 'Remove load balancer rule',
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
                                    label: 'Remove load balancer rule ' + args.context.multiRule[0].id,
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
										    var apiCmd = "listLoadBalancerRules";												  									
											  //if(args.context.networks[0].type == "Shared") 
											  //	apiCmd += "&domainid=" + g_domainid + "&account=" + g_account;
											  //else //args.context.networks[0].type == "Isolated"
												apiCmd += "&networkid=" + args.context.networks[0].id;
												
                        $.ajax({
                          url: createURL(apiCmd),                        
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
                                  lbruleid: item.id
                                },
                                success: function(json) {
                                  var stickyPolicy = json.listlbstickinesspoliciesresponse.stickinesspolicies ?
                                    json.listlbstickinesspoliciesresponse.stickinesspolicies[0].stickinesspolicy : null

                                  if (stickyPolicy && stickyPolicy.length) {
                                    stickyPolicy = stickyPolicy[0];
                                    
                                    stickyData = {
                                      _buttonLabel: 'lb'.toUpperCase(),
                                      method: 'lb',
                                      name: 'StickyTest',
                                      mode: '123',
                                      nocache: true,
                                      indirect: false,
                                      postonly: true,
                                      domain: false
                                    };
                                  } else {
                                    stickyData = {};
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
                                  id: item.id
                                },
                                success: function(data) {
                                  lbInstances = data.listloadbalancerruleinstancesresponse.loadbalancerruleinstance;
                                },
                                error: function(data) {
                                  args.response.error(parseXMLHttpResponse(data));
                                }
                              });

                              $.extend(item, {
                                _itemData: lbInstances,
                                sticky: stickyData
                              });
                            });

                            args.response.success({
                              data: loadBalancerData
                            });
                          }
                        });
                      }
                    }
                  )
                }
              }
            }
          }
        }
      },
      ipAddresses: {
        type: 'select',
        title: 'IP Addresses',
        listView: {
          id: 'ipAddresses',
          label: 'IPs',
          filters: {
            allocated: { label: 'Allocated ' },
            mine: { label: 'My network' }
          },
          fields: {
            ipaddress: {
              label: 'IP',
              converter: function(text, item) {
                if (item.issourcenat) {
                  return text + ' [Source NAT]';
                }

                return text;
              }
            },
            zonename: { label: 'Zone' },
            //vlanname: { label: 'VLAN' },
						iselastic: { label: 'Elastic', converter: cloudStack.converters.toBooleanText },
            account: { label: 'Account' },
            state: { label: 'State', indicator: { 'Allocated': 'on', 'Released': 'off' } }
          },
          actions: {
            add: {
              label: 'Acquire new IP',
              addRow: 'true',
              action: function(args) {					
								var apiCmd = "associateIpAddress";
								if(args.context.networks[0].type == "Shared") 
								  apiCmd += "&domainid=" + g_domainid + "&account=" + g_account;											
                $.ajax({
                  url: createURL(apiCmd),
                  data: {
                    networkId: args.context.networks[0].id
                  },
                  dataType: 'json',
                  async: true,
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

              messages: {
                confirm: function(args) {
                  return 'Please confirm that you would like to acquire a net IP for this network.';
                },
                notification: function(args) {
                  return 'Allocated IP';
                }
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
            $.ajax({
              url: createURL("listPublicIpAddresses&listAll=true&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
              data: data,
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listpublicipaddressesresponse.publicipaddress;
                var processedItems = 0;

                if (!items) {
                  args.response.success({
                    data: []
                  });
                  return;
                }

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
              if (!ipAddress.issourcenat ||
                  (ipAddress.issourcenat && !ipAddress.vpnenabled)) {
                disabledTabs.push('vpn');
              }							
							if(ipAddress.iselastic == true) {
							  disabledTabs.push('vpn');
														
                if(ipAddress.isstaticnat == true || ipAddress.virtualmachineid != null)								
								  disabledTabs.push('ipRules');								
							}			
              return disabledTabs;
            },
            actions: {
              enableVPN: {
                label: 'Enable VPN',
                action: function(args) {
                  $.ajax({
                    url: createURL('createRemoteAccessVpn'),
                    data: {
                      publicipid: args.context.ipAddresses[0].id,
                      domainid: args.context.ipAddresses[0].domainid
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
                    return 'Please confirm that you want VPN enabled for this IP address.';
                  },
                  notification: function(args) {
                    return 'Enabled VPN';
                  },
                  complete: function(args) {
                    return 'VPN is now enabled for IP ' + args.vpn.publicip + '.'
                      + '<br/>Your IPsec pre-shared key is:<br/>' + args.vpn.presharedkey;
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },
              disableVPN: {
                label: 'Disable VPN',
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
                    return 'Are you sure you want to disable VPN?';
                  },
                  notification: function(args) {
                    return 'Disabled VPN';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },
              enableStaticNAT: {
                label: 'Enable static NAT',
                action: {
                  noAdd: true,
                  custom: cloudStack.uiCustom.enableStaticNAT({
                    listView: $.extend(true, {}, cloudStack.sections.instances, {
                      listView: {
                        dataProvider: function(args) {
                          $.ajax({
                            url: createURL('listVirtualMachines'),
                            data: {
                              networkid: args.context.networks[0].id
                            },
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
                      $.ajax({
                        url: createURL('enableStaticNat'),
                        data: {
                          ipaddressid: args.context.ipAddresses[0].id,
                          virtualmachineid: args.context.instances[0].id
                        },
                        dataType: 'json',
                        async: true,
                        success: function(data) {
                          args.response.success({ fullRefresh: true });											
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
                    return 'Enabled Static NAT';
                  }
                },
                notification: {
                  poll: function(args) {
                    args.complete({
                      data: {
                        isstaticnat: true
                      }
                    });

                    setTimeout(function() {
                      $(window).trigger('cloudStack.fullRefresh');
                    }, 500);
                  }
                }
              },
              disableStaticNAT: {
                label: 'Disable static NAT',
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
                              isstaticnat: false
                            };
                          },
                          getActionFilter: function() {
                            return function(args) {
                              return ['enableStaticNAT'];
                            };
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
                    return 'Are you sure you want to disable static NAT?';
                  },
                  notification: function(args) {
                    return 'Disable Static NAT';
                  }
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },
              destroy: {
                label: 'Release IP',
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
                    return 'Are you sure you want to release this IP?';
                  },
                  notification: function(args) {
                    return 'Release IP';
                  }
                },
                notification: { poll: pollAsyncJobResult }
              }
            },
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    ipaddress: { label: 'IP' }
                  },
                  {
                    id: { label: 'id' },
                    networkname: { label: 'Network' },
                    networktype: { label: 'Network Type' },
                    networkid: { label: 'Network ID' },
                    associatednetworkid: { label: 'Assoc. Network ID' },
                    state: { label: 'State' },
										issourcenat: { label: 'Source NAT', converter: cloudStack.converters.toBooleanText },
                    isstaticnat: { label: 'Static NAT', converter: cloudStack.converters.toBooleanText },
										iselastic: { label: 'Elastic', converter: cloudStack.converters.toBooleanText },																	
										virtualmachinedisplayname: { label: 'Virtual machine' },			
                    domain: { label: 'Domain' },
                    account: { label: 'Account' },
                    zonename: { label: 'Zone' },
                    vlanname: { label: 'VLAN' }                   							
                  }
                ],

                //dataProvider: testData.dataProvider.detailView('network')
                dataProvider: function(args) {
                  var items = args.context.ipAddresses;

                  // Get network data
                  $.ajax({
                    url: createURL("listPublicIpAddresses&id="+args.id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var item = items[0];
                      $.ajax({
                        url: createURL('listNetworks'),
                        data: {
                          networkid: this.associatednetworkid
                        },
                        dataType: 'json',
                        async: true,
                        success: function(data) {
                          // Get VPN data
                          $.ajax({
                            url: createURL('listRemoteAccessVpns'),
                            data: {
                              publicipid: item.id
                            },
                            dataType: 'json',
                            async: true,
                            success: function(vpnResponse) {
                              var isVPNEnabled = vpnResponse.listremoteaccessvpnsresponse.count;
                              if (isVPNEnabled) {
                                item.vpnenabled = true;
                                item.remoteaccessvpn = vpnResponse.listremoteaccessvpnsresponse.remoteaccessvpn[0];
                              };

                              // Check if data retrieval complete
                              item.network = data.listnetworksresponse.network[0];
                              item.networkname = item.network.name;
                              item.networktype = item.network.type;

                              args.response.success({
                                actionFilter: actionFilters.ipAddress,
                                data: item
                              });
                            }
                          });
                        }
                      });
                    },
                    error: function(data) {
                      args.response.error(parseXMLHttpResponse(data));
                    }
                  });
                }
              },
              ipRules: {
                title: 'Configuration',
                custom: cloudStack.ipRules({
                  preFilter: function(args) {			
										var disallowedActions = [];
										if (args.context.ipAddresses[0].isstaticnat)  
										  disallowedActions.push("nonStaticNATChart");  //tell ipRules widget to show staticNAT chart instead of non-staticNAT chart.									
										                    
										var networkHavingFirewallService = false;										
										var networkHavingPortForwardingService = false;
										var networkHavingLbService = false;																				
										$(args.context.networks[0].service).each(function(){												 
											if(this.name == "Firewall") 
												networkHavingFirewallService = true;		
											if(this.name == "PortForwarding")
											  networkHavingPortForwardingService = true;
											if(this.name == "Lb")
											  networkHavingLbService = true;											
										});		
										if(networkHavingFirewallService == false) 
										  disallowedActions.push("firewall"); 	
										if(networkHavingPortForwardingService == false) 
										  disallowedActions.push("portForwarding");
										if(networkHavingLbService == false) 
										  disallowedActions.push("loadBalancing");																					
									
                    return disallowedActions;
                  },

                  // Firewall rules
                  firewall: {
                    noSelect: true,
                    fields: {
                      'cidrlist': { edit: true, label: 'Source CIDR' },
                      'protocol': {
                        label: 'Protocol',
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
                              $icmpFields.attr('disabled', false);
                              $otherFields.attr('disabled', 'disabled');
                            } else {
                              $otherFields.attr('disabled', false);
                              $icmpFields.attr('disabled', 'disabled');
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
                      'startport': { edit: true, label: 'Start Port' },
                      'endport': { edit: true, label: 'End Port' },
                      'icmptype': { edit: true, label: 'ICMP Type', isDisabled: true },
                      'icmpcode': { edit: true, label: 'ICMP Code', isDisabled: true },
                      'add-rule': {
                        label: 'Add Rule',
                        addButton: true
                      }
                    },
                    add: {
                      label: 'Add',
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
                                label: 'Add firewall rule',
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
                        label: 'Remove Rule',
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
                                  label: 'Remove firewall rule ' + args.context.multiRule[0].id,
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
                        id: args.context.ipAddresses[0].id
                      },
                      dataType: 'json',
                      async: true,
                      success: function(data) {
                        var ipAddress = data.listpublicipaddressesresponse.publicipaddress[0];

                        args.response.success({
                          data: ipAddress
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
                        id: args.context.ipAddresses[0].virtualmachineid
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

                  staticNAT: {
                    noSelect: true,
                    fields: {
                      'protocol': {
                        label: 'Protocol',
                        select: function(args) {
                          args.response.success({
                            data: [
                              { name: 'tcp', description: 'TCP' },
                              { name: 'udp', description: 'UDP' }
                            ]
                          });
                        }
                      },
                      'startport': { edit: true, label: 'Start Port' },
                      'endport': { edit: true, label: 'End Port' },
                      'add-rule': {
                        label: 'Add Rule',
                        addButton: true
                      }
                    },
                    add: {
                      label: 'Add',
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
                                label: 'Added static NAT rule',
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
                        label: 'Remove Rule',
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
                                  label: 'Removed static NAT rule',
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

                  // Load balancing rules
                  loadBalancing: {
                    listView: $.extend(true, {}, cloudStack.sections.instances, {
                      listView: {
                        dataProvider: function(args) {
                          $.ajax({
                            url: createURL('listVirtualMachines'),
                            data: {
                              networkid: args.context.ipAddresses[0].associatednetworkid
                            },
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
                      'name': { edit: true, label: 'Name' },
                      'publicport': { edit: true, label: 'Public Port' },
                      'privateport': { edit: true, label: 'Private Port' },
                      'algorithm': {
                        label: 'Algorithm',
                        select: function(args) {
                          args.response.success({
                            data: [
                              { name: 'roundrobin', description: 'Round-robin' },
                              { name: 'leastconn', description: 'Least connections' },
                              { name: 'source', description: 'Source' }
                            ]
                          });
                        }
                      },
                      'sticky': {
                        label: 'Sticky Policy',
                        custom: {
                          buttonLabel: 'Configure',
                          action: cloudStack.lbStickyPolicy()
                        }
                      },
                      'add-vm': {
                        label: 'Add VMs',
                        addButton: true
                      }
                    },
                    add: {
                      label: 'Add VMs',
                      action: function(args) {
                        var openFirewall = g_firewallRuleUiEnabled == "true" ? false : true;
                        var data = {
                          algorithm: args.data.algorithm,
                          name: args.data.name,
                          privateport: args.data.privateport,
                          publicport: args.data.publicport
                        };
                        var stickyData = $.extend(true, {}, args.data.sticky);
																			
												var apiCmd = "createLoadBalancerRule";		
												//if(args.context.networks[0].type == "Shared") 
												  //apiCmd += "&domainid=" + g_domainid + "&account=" + g_account;
												//else //args.context.networks[0].type == "Isolated"
												  apiCmd += "&publicipid=" + args.context.ipAddresses[0].id;
												
                        $.ajax({
                          url: createURL(apiCmd),
                          data: $.extend(data, {
                            openfirewall: openFirewall,                            
                            networkid: args.context.networks[0].id
                          }),
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
																  fullRefresh: true,
                                  _custom: {
                                    jobId: jobID
                                  },
                                  notification: {
                                    label: 'Add load balancer rule',
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
                                          
                                          // Create stickiness policy
                                          if (stickyData &&
                                              stickyData.methodname &&
                                              stickyData.methodname != 'None') {
                                            var stickyURLData = '';
                                            var stickyParams;

                                            switch (stickyData.methodname) {
                                              case 'LbCookie':
                                              stickyParams = ['name', 'mode', 'nocache', 'indirect', 'postonly', 'domain'];
                                              break;
                                              case 'AppCookie':
                                              stickyParams = ['name', 'length', 'holdtime', 'request-learn', 'prefix', 'mode'];
                                              break;
                                              case 'SourceBased':
                                              stickyParams = ['tablesize', 'expire'];
                                              break;
                                            }

                                            $(stickyParams).each(function(index) {
                                              var param = '&param[' + index + ']';
                                              var name = this;
                                              var value = stickyData[name];

                                              if (!value) return true;
                                              if (value == 'on') value = true;
                                              
                                              stickyURLData += param + '.name=' + name + param + '.value=' + value;
                                            });

                                            $.ajax({
                                              url: createURL('createLBStickinessPolicy' + stickyURLData),
                                              data: {
                                                lbruleid: args.data.loadbalancer.id,
                                                name: stickyData.name,
                                                methodname: stickyData.methodname
                                              },
                                              success: function(json) {
                                                var addStickyCheck = setInterval(function() {
                                                  pollAsyncJobResult({
                                                    _custom: {
                                                      jobId: json.createLBStickinessPolicy.jobid,
                                                    },
                                                    complete: function(args) {
                                                      complete();
                                                      clearInterval(addStickyCheck);
                                                    },
                                                    error: function(args) {
                                                      complete();
                                                      cloudStack.dialog.notice({ message: args.message });
                                                      clearInterval(addStickyCheck);
                                                    }
                                                  });                                                  
                                                }, 1000);
                                              },
                                              error: function(json) {
                                                complete();
                                                cloudStack.dialog.notice({ message: parseXMLHttpResponse(json) });
                                              }
                                            });
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
                      destroy:  {
                        label: 'Remove load balancer rule',
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
                                  label: 'Remove load balancer rule ' + args.context.multiRule[0].id,
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
										  var apiCmd = "listLoadBalancerRules";												  									
											//if(args.context.networks[0].type == "Shared") 
											//	apiCmd += "&domainid=" + g_domainid + "&account=" + g_account;
											//else //args.context.networks[0].type == "Isolated"
												apiCmd += "&publicipid=" + args.context.ipAddresses[0].id;
																				
                      $.ajax({
                        url: createURL(apiCmd),                        
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
                                lbruleid: item.id
                              },
                              success: function(json) {
                                var stickyPolicy = json.listlbstickinesspoliciesresponse.stickinesspolicies ?
                                  json.listlbstickinesspoliciesresponse.stickinesspolicies[0].stickinesspolicy : null

                                if (stickyPolicy && stickyPolicy.length) {
                                  stickyPolicy = stickyPolicy[0];

                                  if (!stickyPolicy.methodname) stickyPolicy.methodname = 'None';

                                  stickyData = {
                                    _buttonLabel: stickyPolicy.methodname,
                                    methodname: stickyPolicy.methodname,
                                    id: stickyPolicy.id
                                  };
                                  $.extend(stickyData, stickyPolicy.params);
                                } else {
                                  stickyData = {};
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
                                id: item.id
                              },
                              success: function(data) {
                                lbInstances = data.listloadbalancerruleinstancesresponse.loadbalancerruleinstance;
                              },
                              error: function(data) {
                                args.response.error(parseXMLHttpResponse(data));
                              }
                            });

                            $.extend(item, {
                              _itemData: lbInstances,
                              sticky: stickyData
                            });
                          });

                          args.response.success({
                            data: loadBalancerData
                          });
                        }
                      });
                    }
                  },

                  // Port forwarding rules
                  portForwarding: {
                    listView: $.extend(true, {}, cloudStack.sections.instances, {
                      listView: {
                        dataProvider: function(args) {
                          $.ajax({
                            url: createURL('listVirtualMachines'),
                            data: {
                              networkid: args.context.ipAddresses[0].associatednetworkid
                            },
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
                      'private-ports': {
                        edit: true,
                        label: 'Private Ports',
                        range: ['privateport', 'privateendport']
                      },
                      'public-ports': {
                        edit: true,
                        label: 'Public Ports',
                        range: ['publicport', 'publicendport']
                      },
                      'protocol': {
                        label: 'Protocol',
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
                        label: 'Add VM',
                        addButton: true
                      }
                    },
                    add: {
                      label: 'Add VM',
                      action: function(args) {
                        var openFirewall = g_firewallRuleUiEnabled == "true" ? false : true;

                        $.ajax({
                          url: createURL('createPortForwardingRule'),

                          data: $.extend(args.data, {
                            openfirewall: openFirewall,
                            ipaddressid: args.context.ipAddresses[0].id,
                            virtualmachineid: args.itemData[0].id
                          }),
                          dataType: 'json',
                          async: true,
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
                      destroy: {
                        label: 'Remove port forwarding rule',
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
                                  label: 'Remove port forwarding rule ' + args.context.multiRule[0].id,
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
                        url: createURL('listPortForwardingRules'),
                        data: {
                          ipaddressid: args.context.ipAddresses[0].id
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

                            $.ajax({
                              url: createURL('listVirtualMachines'),
                              dataType: 'json',
                              async: true,
                              data: {
                                id: item.virtualmachineid
                              },
                              success: function(data) {
                                loadCurrent++;
                                $.extend(item, {
                                  _itemData: data.listvirtualmachinesresponse.virtualmachine,
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
                  var psk = args.context.ipAddresses[0].remoteaccessvpn.presharedkey;

                  return $('<div>')
                    .append(
                      $('<ul>').addClass('info')
                        .append(
                          // VPN IP
                          $('<li>').addClass('ip').html('Your VPN access is currently enabled and can be accessed via the IP: ')
                            .append($('<strong>').html(ipAddress))
                        )
                        .append(
                          // PSK
                          $('<li>').addClass('psk').html('Your IPSec pre-shared key is: ')
                            .append($('<strong>').html(psk))
                        )
                    ).multiEdit({
                      context: args.context,
                      noSelect: true,
                      fields: {
                        'username': { edit: true, label: 'Username' },
                        'password': { edit: true, isPassword: true, label: 'Password' },
                        'add-user': { addButton: true, label: 'Add user' }
                      },
                      add: {
                        label: 'Add user',
                        action: function(args) {
                          $.ajax({
                            url: createURL('addVpnUser'),
                            data: args.data,
                            dataType: 'json',
                            success: function(data) {
                              args.response.success({
                                _custom: {
                                  jobId: data.addvpnuserresponse.jobid
                                },
                                notification: {
                                  label: 'Added VPN user',
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
                          label: 'Remove user',
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
                                    label: 'Removed VPN user',
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
                            domainid: args.context.ipAddresses[0].domainid
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
        title: 'Security Groups',
        id: 'securityGroups',
        listView: {
          id: 'securityGroups',
          label: 'Security Groups',
          fields: {
            name: { label: 'Name', editable: true },
            description: { label: 'Description' },
            domain: { label: 'Domain' },
            account: { label: 'Account' }
          },
          actions: {
            add: {
              label: 'Add security group',

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
                  })
                }
              },

              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to add ' + args.name + '?';
                },
                notification: function(args) {
                  return 'Created security group';
                }
              },

              createForm: {
                title: 'New security group',
                desc: 'Please name your security group.',
                fields: {
                  name: { label: 'Name' },
                  description: { label: 'Description' }
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
                title: 'Details',
                fields: [
                  {
                    name: { label: 'Name' }
                  },
                  {
									  id: { label: 'ID' },
										description: { label: 'Description' },									
                    domain: { label: 'Domain' },
                    account: { label: 'Account' }
                  }
                ],
                
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
                title: 'Ingress Rules',
                custom: cloudStack.uiCustom.securityRules({
                  noSelect: true,
                  noHeaderActionsColumn: true,
                  fields: {
                    'protocol': {
                      label: 'Protocol',
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
                    'startport': { edit: true, label: 'Start Port' },
                    'endport': { edit: true, label: 'End Port' },
                    'icmptype': { edit: true, label: 'ICMP Type', isHidden: true },
                    'icmpcode': { edit: true, label: 'ICMP Code', isHidden: true },
                    'cidr': { edit: true, label: 'CIDR', isHidden: true },
                    'accountname': {
                      edit: true,
                      label: 'Account, Security Group',
                      isHidden: true,
                      range: ['accountname', 'securitygroup']
                    },
                    'add-rule': {
                      label: 'Add',
                      addButton: true
                    }
                  },
                  add: {
                    label: 'Add',
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
                              label: 'Add new ingress rule',
                              poll: pollAsyncJobResult
                            }
                          });
                        }
                      });
                    }
                  },
                  actions: {
                    destroy: {
                      label: 'Remove Rule',
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
                                label: 'Revoke ingress rule',
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
                            data.listsecuritygroupsresponse.securitygroup[0].ingressrule,
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
                title: 'Egress Rules',
                custom: cloudStack.uiCustom.securityRules({
                  noSelect: true,
                  noHeaderActionsColumn: true,
                  fields: {
                    'protocol': {
                      label: 'Protocol',
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
                    'startport': { edit: true, label: 'Start Port' },
                    'endport': { edit: true, label: 'End Port' },
                    'icmptype': { edit: true, label: 'ICMP Type', isHidden: true },
                    'icmpcode': { edit: true, label: 'ICMP Code', isHidden: true },
                    'cidr': { edit: true, label: 'CIDR', isHidden: true },
                    'accountname': {
                      edit: true,
                      label: 'Account, Security Group',
                      isHidden: true,
                      range: ['accountname', 'securitygroup']
                    },
                    'add-rule': {
                      label: 'Add',
                      addButton: true
                    }
                  },
                  add: {
                    label: 'Add',
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
                              label: 'Add new egress rule',
                              poll: pollAsyncJobResult
                            }
                          });
                        }
                      });
                    }
                  },
                  actions: {
                    destroy: {
                      label: 'Remove Rule',
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
                                label: 'Revoke egress rule',
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
                            data.listsecuritygroupsresponse.securitygroup[0].egressrule,
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
              destroy: {
                label: 'Delete security group',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to delete ' + args.name + '?';
                  },
                  notification: function(args) {
                    return 'Deleted security group: ' + args.name;
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
              },
            }
          }
        }
      }
    }
  };
})(cloudStack, jQuery, testData);
