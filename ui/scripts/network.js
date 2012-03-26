
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

      if (status == 'Released') {
        return [];
      }

      if (status == 'Destroyed' ||
          status == 'Releasing' ||
          status == 'Released' ||
          status == 'Creating' ||
          status == 'Allocating' ||
          item.account == 'system' ||
          item.issystem == true ) {
        disallowedActions = allowedActions;
      }

      if (item.isstaticnat) {
        disallowedActions.push('enableStaticNAT');
      } else {
        disallowedActions.push('disableStaticNAT');
      }

      if ($.inArray('Vpn', $.map(args.context.networks[0].service,
                                 function(service) { return service.name; })) > -1) {
        if (item.vpnenabled) {
          disallowedActions.push('enableVPN');
        } else {
          disallowedActions.push('disableVPN');
        }
      } else {
        disallowedActions.push('disableVPN');
        disallowedActions.push('enableVPN');
      }

      if (item.issourcenat){
        disallowedActions.push('enableStaticNAT');
        disallowedActions.push('disableStaticNAT');
        disallowedActions.push('destroy');
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
        allowedActions.push('destroy');
      }

      return allowedActions;
    }
  };

  cloudStack.sections.network = {
    title: 'label.network',
    id: 'network',
    sectionSelect: {
      preFilter: function(args) {
        var havingSecurityGroupNetwork = false;
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

        var sectionsToShow = ['networks'];
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
                var basicZoneExists = false;
                $.ajax({
                  url: createURL("listZones"),
                  dataType: "json",
                  async: false,
                  success: function(json) {
                    if(json.listzonesresponse.zone != null && json.listzonesresponse.zone.length > 0) {
                      zoneObjs = json.listzonesresponse.zone;
                      $(zoneObjs).each(function() {
                        if(this.networktype == "Basic") {
                          basicZoneExists = true;
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
                  guestGateway: { label: 'label.guest.gateway' },
                  guestNetmask: { label: 'label.guest.netmask' }
                }
              },
              action: function(args) {
                var array1 = [];
                array1.push("&zoneId=" + args.data.zoneId);
                array1.push("&name=" + todb(args.data.name));
                array1.push("&displayText=" + todb(args.data.displayText));
                array1.push("&networkOfferingId=" + args.data.networkOfferingId);

                if(args.data.guestGateway != null && args.data.guestGateway.length > 0)
                  array1.push("&gateway=" + args.data.guestGateway);
                if(args.data.guestNetmask != null && args.data.guestNetmask.length > 0)
                  array1.push("&netmask=" + args.data.guestNetmask);

                $.ajax({
                  url: createURL('createNetwork' + array1.join("")),
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
                    account: { label: 'label.account' }
                  }
                ],
                dataProvider: function(args) {								 					
								  $.ajax({
										url: createURL("listNetworks&id=" + args.context.networks[0].id + "&listAll=true"), //pass "listAll=true" for now until API is fixed.
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
                            $.ajax({
                              url: createURL('listVirtualMachines'),
                              data: {
                                networkid: args.context.networks[0].id,
                                listAll: true
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
                          var openFirewall = false;
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
          filters: {
            allocated: { label: 'label.allocated' },
            mine: { label: 'label.my.network' }
          },
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
                  return 'message.acquire.new.ip';
                },
                notification: function(args) {
                  return 'label.acquire.new.ip';
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
              if (!ipAddress.vpnenabled) {
                disabledTabs.push('vpn');
              }
              if(ipAddress.issystem == true) {
                disabledTabs.push('vpn');

                if(ipAddress.isstaticnat == true || ipAddress.virtualmachineid != null)
                  disabledTabs.push('ipRules');
              }
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
                    listView: $.extend(true, {}, cloudStack.sections.instances, {
                      listView: {
                        dataProvider: function(args) {
                          $.ajax({
                            url: createURL('listVirtualMachines'),
                            data: {
                              networkid: args.context.networks[0].id,
                              listAll: true
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

                dataProvider: function(args) {
                  var items = args.context.ipAddresses;

                  // Get network data
                  $.ajax({
                    url: createURL('listPublicIpAddresses'),
                    data: {                      
                      id: args.id
                    },
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var item = items[0];
                      // Get VPN data
                      $.ajax({
                        url: createURL('listRemoteAccessVpns'),
                        data: {
                          listAll: true,
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
                          args.response.success({
                            actionFilter: actionFilters.ipAddress,
                            data: item
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
                title: 'label.configuration',
                custom: cloudStack.ipRules({
                  preFilter: function(args) {
                    var disallowedActions = [];
                    if (args.context.ipAddresses[0].isstaticnat)
                      disallowedActions.push("nonStaticNATChart");  //tell ipRules widget to show staticNAT chart instead of non-staticNAT chart.


                    var networkOfferingHavingFirewallService = false;
                    var networkOfferingHavingPortForwardingService = false;
                    var networkOfferingHavingLbService = false;
                    $.ajax({
                      url: createURL("listNetworkOfferings&id=" + args.context.networks[0].networkofferingid),
                      dataType: "json",
                      async: false,
                      success: function(json) {
                        var networkoffering = json.listnetworkofferingsresponse.networkoffering[0];
                        $(networkoffering.service).each(function(){
                          var thisService = this;
                          if(thisService.name == "Firewall")
                            networkOfferingHavingFirewallService = true;
                          if(thisService.name == "PortForwarding")
                            networkOfferingHavingPortForwardingService = true;
                          if(thisService.name == "Lb")
                            networkOfferingHavingLbService = true;
                        });
                      }
                    });
                    if(networkOfferingHavingFirewallService == false)
                      disallowedActions.push("firewall");
                    if(networkOfferingHavingPortForwardingService == false)
                      disallowedActions.push("portForwarding");
                    if(networkOfferingHavingLbService == false)
                      disallowedActions.push("loadBalancing");

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

                  // Load balancing rules
                  loadBalancing: {
                    listView: $.extend(true, {}, cloudStack.sections.instances, {
                      listView: {
                        dataProvider: function(args) {
                          var itemData = $.isArray(args.context.multiRule) && args.context.multiRule[0]['_itemData'] ?
                            args.context.multiRule[0]['_itemData'] : [];

                          $.ajax({
                            url: createURL('listVirtualMachines'),
                            data: {
                              networkid: args.context.ipAddresses[0].associatednetworkid,
                              listAll: true
                            },
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
                    add: {
                      label: 'label.add.vms',
                      action: function(args) {
                        var openFirewall = false;
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
                      var apiCmd = "listLoadBalancerRules&listAll=true";
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
                              _itemName: 'displayname',
                              _itemData: lbInstances,
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
                              listAll: true,
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
                    add: {
                      label: 'label.add.vm',
                      action: function(args) {
                        var openFirewall = false;

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

                            item._itemName = 'displayname';

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
                            data: args.data,
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
                      label: _l('label.account') + ', ' + _l('label.security.group'),
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
                      label: _l('label.account') + ', ' + _l('label.security.group'),
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
              destroy: {
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
      }
    }
  };
		
})(cloudStack, jQuery);
