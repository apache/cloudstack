(function($, cloudStack, testData) {

  var zoneObjs, podObjs, clusterObjs, domainObjs;
  var selectedClusterObj;
  var publicNetworkObj;

  cloudStack.sections.system = {
    title: 'System',
    id: 'system',
    // Network-as-a-service configuration
    naas: {
      mainNetworks: {
        'public': {
          detailView: {
            actions: {},
            tabs: {
              details: {
                title: 'Details',                
                fields: [
                  {
                    networkofferingdisplaytext:  { label: "Network offering description" }
                  },
                  {
                    id: { label: "ID" },
                    broadcastdomaintype: { label: 'Broadcast domain type' },
                    traffictype: { label: 'Traffic type' },
                    gateway: { label: 'Gateway' },
                    netmask: { label: 'Netmask' },
                    startip: { label: 'Start IP' },
                    endip: { label: 'End IP' },
                    zoneid: { label: 'Zone ID' },
                    networkofferingid: { label: 'Network offering ID' },
                    networkofferingname: { label: 'Network offering name' },
                    networkofferingavailability: { label: 'network offering availability' },
                    isshared: {
                      label: 'Shared',
                      converter: cloudStack.converters.toBooleanText
                    },
                    issystem: {
                      label: 'System',
                      converter: cloudStack.converters.toBooleanText
                    },
                    isdefault: {
                      label: 'Default',
                      converter: cloudStack.converters.toBooleanText
                    },
                    securitygroupenabled: {
                      label: 'Security group enabled',
                      converter: cloudStack.converters.toBooleanText
                    },
                    state: { label: 'State' },
                    related: { label: 'Related' },
                    dns1: { label: 'DNS 1' },
                    dns2: { label: 'DNS 2' },
                    vlan: { label: 'VLAN' },
                    domainid: { label: 'Domain ID' },
                    account: { label: 'Account' }
                  }
                ],
                                
                dataProvider: function(args) {                    
                  var showPublicNetwork = true;
                  var zoneObj = args.context.zones[0];
                  if(zoneObj.networktype == "Basic") {
                    //$("#add_network_button").hide();
                    $.ajax({
                      url: createURL("listExternalFirewalls&zoneid=" + zoneObj.id),
                      dataType: "json",
                      async: false,
                      success: function(json) {
                        var items = json.listexternalfirewallsresponse.externalfirewall;
                        if(items != null && items.length > 0) {
                          showPublicNetwork = true;
                          //$("#add_iprange_button,#tab_ipallocation").show();
                        }
                        else {
                          showPublicNetwork = false;
                          //$("#add_iprange_button,#tab_ipallocation").hide();
                        }
                      }
                    });
                  }
                  else { // Advanced zone
                    showPublicNetwork = true;
                    //$("#add_network_button,#add_iprange_button,#tab_ipallocation").show();
                    //listMidMenuItems2(("listNetworks&type=Direct&zoneId="+zoneObj.id), networkGetSearchParams, "listnetworksresponse", "network", directNetworkToMidmenu, directNetworkToRightPanel, directNetworkGetMidmenuId, false, 1);
                  }

                  //var publicNetworkObj;
                  if(showPublicNetwork == true && zoneObj.securitygroupsenabled == false) { //public network
                    $.ajax({
                      url: createURL("listNetworks&trafficType=Public&isSystem=true&zoneId="+zoneObj.id),
                      dataType: "json",
                      async: false,
                      success: function(json) {
                        var items = json.listnetworksresponse.network;
                        publicNetworkObj = items[0];                        
                      }
                    });
                  }
                  else if (showPublicNetwork == true && zoneObj.securitygroupsenabled == true){
                    $.ajax({
                      url: createURL("listNetworks&type=Direct&trafficType=Guest&isSystem=true&zoneId="+zoneObj.id),
                      dataType: "json",
                      async: false,
                      success: function(json) {
                        var items = json.listnetworksresponse.network;
                        publicNetworkObj = items[0]; 
                      }
                    });
                  }
                                    
                  args.response.success({data: publicNetworkObj});
                  
                }               
              },
                           
              ipAddresses: {
                title: 'IP Addresses',
                custom: function(args) {                  
                  return $('<div></div>').multiEdit({
                    context: args.context,
                    noSelect: true,
                    fields: {
                      'gateway': { edit: true, label: 'Gateway' },
                      'netmask': { edit: true, label: 'Netmask' },
                      'vlanid': { edit: true, label: 'VLAN', isOptional: true },
                      'startip': { edit: true, label: 'Start IP' },
                      'endip': { edit: true, label: 'End IP' },
                      'add-rule': { label: 'Add', addButton: true }
                    },
                    add: {
                      label: 'Add',
                      action: function(args) {                       
                        var array1 = [];
                        array1.push("&zoneId=" + args.context.zones[0].id);

                        if (args.data.vlanid != null && args.data.vlanid.length > 0)
                          array1.push("&vlan=" + todb(args.data.vlanid));
                        else
                          array1.push("&vlan=untagged");

                          
                        //array1.push("&isshared=true"); //temporary, will add scope, domain, account field, then uncommment the following section later.
                        /*
                        if($form.find('.form-item[rel=domainId]').css("display") != "none") {
                          if($form.find('.form-item[rel=account]').css("display") != "none") {  //account-specific
                            array1.push("&domainId=" + args.data.domainId);
                            array1.push("&account=" + args.data.account);
                          }
                          else {  //domain-specific
                            array1.push("&domainId=" + args.data.domainId);
                            array1.push("&isshared=true");
                          }
                        }
                        else { //zone-wide
                          array1.push("&isshared=true");
                        }
                        */
                        
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
                                label: 'Added IP address',
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
                            url: createURL('deleteVlanIpRange'),
                            data: {
                              id: args.context.multiRule[0].id
                            },
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
                        url: createURL("listVlanIpRanges&zoneid=" + args.context.zones[0].id + "&networkId=" + publicNetworkObj.id),
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
              //???  
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
                    name: { label: 'Name' }
                  }
                ],
                dataProvider: function(args) {
                  args.response.success({ data: testData.data.networks[0] });
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
                  $.ajax({
                    url: createURL("updatePhysicalNetwork&id=" + args._custom.physicalNetworkObj.id + "&state=Enabled&vlan=" + todb(args.data.vlan)),                    
                    dataType: "json",
                    success: function(json) {                      
                      
                    }
                  });
                  
                  $.ajax({
                    url: createURL("updateZone&id=" + args.context.zones[0].id + "&guestcidraddress=" + todb(args.data.guestcidraddress)),
                    dataType: "json",
                    success: function(json) {
                    
                    }
                  });
                  
                  //args.response.success({data: item});                  
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
                    vlan: { 
                      label: 'VLAN',
                      isEditable: true                      
                    },
                    broadcastdomainrange: { label: 'Broadcast domain range' },                  
                    zoneid: { label: 'Zone ID' },
                    guestcidraddress: { 
                      label: 'CIDR',
                      isEditable: true                             
                    }                    
                  }
                ],
                dataProvider: function(args) { 
                  var physicalNetworkObj = [];
                  $.ajax({
                    url: createURL("listPhysicalNetworks&zoneId=" + args.context.zones[0].id),
                    dataType: "json",
                    async: false,
                    success: function(json) {                      
                      var items = json.listphysicalnetworksresponse.physicalnetwork;
                      physicalNetworkObj = items[0];                      
                    }
                  });  
                  
                  physicalNetworkObj["guestcidraddress"] = args.context.zones[0].guestcidraddress;
                  
                  args.response.success({
                    _custom: {"physicalNetworkObj": physicalNetworkObj},
                    data: physicalNetworkObj
                  });                  
                }
              },
			  //???
			  network: {
                title: 'Network',
                listView: {
                  section: 'networks',
                  id: 'networks',
                  fields: {
                    name: { label: 'Name' },
                    startip: { label: 'Start IP' },
                    endip: { label: 'End IP' },
                    vlan: { label: 'VLAN' }
                  },
                  actions: {
                    add: {
                      label: 'Add network',
                      createForm: {
                        title: 'Add network',
                        desc: 'Please fill in the following to add a guest network',
                        fields: {
                          vlan: {
                            label: 'VLAN ID',
                            validation: { required: true }
                          },
                          gateway: {
                            label: 'Gateway',
                            validation: { required: true }
                          },
                          netmask: {
                            label: 'Netmask',
                            validation: { required: true }
                          },
                          startip: {
                            label: 'Start IP',
                            validation: { required: true }
                          },
                          endip: {
                            label: 'Start IP',
                            validation: { required: true }
                          }
                        }
                      },

                      action: function(args) {
                        args.response.success();
                      },

                      messages: {
                        notification: function(args) {
                          return 'Added guest network';
                        }
                      },
                      notification: { poll: testData.notifications.testPoll }
                    }
                  },
                  dataProvider: function(args) {
				    return args.response.success({data: {}});
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
            label: 'Add Network',
            action: function(args) {
              args.response.success();
            }
          }
        },
        dataProvider: function(args) {
          setTimeout(function() {
            args.response.success({
              data: [
                { id: 1, name: 'Network A' },
                { id: 2, name: 'Network B' },
                { id: 3, name: 'Network C' }
              ]
            });
          }, 500);
        }
      },

      networkProviders: {
        // Returns state of each network provider type
        statusCheck: function(args) {
          return {
            virtualRouter: 'enabled',
            netscaler: 'disabled',
            f5: 'shutdown',
            srx: 'enabled',
            securityGroups: 'enabled'
          };
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
          // Virtual router
          virtualRouter: {
            label: 'Virtual Router',
            fields: {
              name: { label: 'Name' },
              ipaddress: { label: 'IP Address' },
              state: { label: 'Status' }
            },
            dataProvider: function(args) {
              args.response.success({
                data: [
                  {
                    name: 'Router0001S',
                    ipaddress: '192.168.1.1',
                    state: 'Enabled'
                  },
                  {
                    name: 'Router0001B',
                    ipaddress: '192.168.1.155',
                    state: 'Enabled'
                  },
                  {
                    name: 'Router0002',
                    ipaddress: '192.168.1.13',
                    state: 'Enabled'
                  }
                ]
              });
            }
          },

          // NetScaler
          netscaler: {
            label: 'NetScaler',
            fields: {
              name: { label: 'Name' },
              ipaddress: { label: 'IP Address' },
              state: { label: 'Status' }
            },
            dataProvider: function(args) {
              args.response.success({
                data: [
                  {
                    name: 'Router0001S',
                    ipaddress: '192.168.1.1',
                    state: 'Enabled'
                  },
                  {
                    name: 'Router0001B',
                    ipaddress: '192.168.1.155',
                    state: 'Enabled'
                  },
                  {
                    name: 'Router0002',
                    ipaddress: '192.168.1.13',
                    state: 'Enabled'
                  }
                ]
              });
            }
          },

          // F5
          f5: {
            label: 'F5',
            fields: {
              name: { label: 'Name' },
              ipaddress: { label: 'IP Address' },
              state: { label: 'Status' }
            },
            dataProvider: function(args) {
              args.response.success({
                data: [
                  {
                    name: 'Router0001S',
                    ipaddress: '192.168.1.1',
                    state: 'Enabled'
                  },
                  {
                    name: 'Router0001B',
                    ipaddress: '192.168.1.155',
                    state: 'Enabled'
                  },
                  {
                    name: 'Router0002',
                    ipaddress: '192.168.1.13',
                    state: 'Enabled'
                  }
                ]
              });
            }
          },

          // SRX
          srx: {
            label: 'SRX',
            fields: {
              name: { label: 'Name' },
              ipaddress: { label: 'IP Address' },
              state: { label: 'Status' }
            },
            dataProvider: function(args) {
              args.response.success({
                data: [
                  {
                    name: 'Router0001S',
                    ipaddress: '192.168.1.1',
                    state: 'Enabled'
                  },
                  {
                    name: 'Router0001B',
                    ipaddress: '192.168.1.155',
                    state: 'Enabled'
                  },
                  {
                    name: 'Router0002',
                    ipaddress: '192.168.1.13',
                    state: 'Enabled'
                  }
                ]
              });
            }
          },

          // Security groups
          securityGroups: {
            label: 'Security Groups',
            fields: {
              name: { label: 'Name' },
              ipaddress: { label: 'IP Address' },
              state: { label: 'Status' }
            },
            dataProvider: function(args) {
              args.response.success({
                data: [
                  {
                    name: 'Router0001S',
                    ipaddress: '192.168.1.1',
                    state: 'Enabled'
                  },
                  {
                    name: 'Router0001B',
                    ipaddress: '192.168.1.155',
                    state: 'Enabled'
                  },
                  {
                    name: 'Router0002',
                    ipaddress: '192.168.1.13',
                    state: 'Enabled'
                  }
                ]
              });
            }
          }
        }
      }
    },
    show: cloudStack.uiCustom.physicalResources({
      sectionSelect: {
        label: 'Select view'
      },
      sections: {
        physicalResources: {
          type: 'select',
          title: 'Physical Resources',
          listView: {
            id: 'zones',
            label: 'Physical Resources',
            fields: {
              name: { label: 'Zone' },
              networktype: { label: 'Network Type' },
              allocationstate: { label: 'Allocation State' }
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
                          url: createURL("listDomains"),
                          dataType: "json",
                          async: false,
                          success: function(json) {
                            domainObjs = json.listdomainsresponse.domain;
                          }
                        });
                        args.response.success({domains: domainObjs});
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
                        //if(args.data["isolation-mode"] == "security-groups") {
                        //  array1.push("&securitygroupenabled=true");
                        //}
                        //else { //args.data["isolation-mode"] == "vlan"
                          array1.push("&securitygroupenabled=false");

                          var guestcidraddress = args.data["guest-cidr"];
                          if(guestcidraddress != null && guestcidraddress.length > 0) {
                            array1.push("&guestcidraddress="+todb(guestcidraddress));
                          }
                        //}
                      }

                      if(args.data["public"] == null) //public checkbox is unchecked
                        array1.push("&domainid=" + args.data["zone-domain"]);

                      var zoneId;
                      $.ajax({
                        url: createURL("createZone" + array1.join("")),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                          var item = json.createzoneresponse.zone;
                          args.response.success({data:item});

                          zoneId = item.id;
                          
                          //NaaS (begin)                          
                          var physicalNetworkId;
                          $.ajax({
                            url: createURL("listPhysicalNetworks&zoneId=" + zoneId),
                            dataType: "json",
                            async: false,
                            success: function(json) {
                              var items = json.listphysicalnetworksresponse.physicalnetwork;
                              if(items != null && items.length > 0)
                                physicalNetworkId = items[0].id
                            }
                          });
                          
                          if(physicalNetworkId != null) {                            
                            var networkServiceProviderId;
                            $.ajax({
                              url: createURL("listNetworkServiceProviders&physicalNetworkId=" + physicalNetworkId),
                              dataType: "json",
                              async: false,
                              success: function(json) {
                                var items = json.listnetworkserviceprovidersresponse.networkserviceprovider;
                                if(items != null && items.length > 0)
                                  networkServiceProviderId = items[0].id
                              }
                            });
                            
                            if(networkServiceProviderId != null) {   
                              var virtualRouterElementId;
                              $.ajax({
                                url: createURL("listVirtualRouterElements&nspid=" + networkServiceProviderId),
                                dataType: "json",
                                async: false,
                                success: function(json) {
                                  var items = json.listvirtualrouterelementsresponse.virtualrouterelement;
                                  if(items != null && items.length > 0)
                                    virtualRouterElementId = items[0].id
                                }
                              });
                              if(virtualRouterElementId != null) {                               
                                $.ajax({
                                  url: createURL("configureVirtualRouterElement&id=" + virtualRouterElementId + "&enabled=true"),
                                  dataType: "json",
                                  async: false,
                                  success: function(json) {
                                    var jid = json.configurevirtualrouterelementresponse.jobid;                               
                                    $.ajax({
                                      url: createURL("queryAsyncJobResult&jobId=" + jid),
                                      dataType: "json",
                                      async: false,
                                      success: function(json) {
                                        var result = json.queryasyncjobresultresponse;
                                        if (result.jobstatus == 0) {
                                          return; //Job has not completed
                                        } else {
                                          if (result.jobstatus == 1) { // Succeeded                                        
                                            //args.complete();                                        
                                          }
                                          else if (result.jobstatus == 2) { // Failed
                                            //args.error({message:result.jobresult.errortext});
                                            alert(fromdb(result.jobresult.errortext));
                                          }
                                        }
                                      },
                                      error: function(XMLHttpResponse) {
                                        //args.error();
                                        var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                        alert(errorMsg);                                        
                                      }
                                    });                            
                                  }
                                });                                                                 
                              }      
                            
                              $.ajax({
                                url: createURL("updateNetworkServiceProvider&id=" + networkServiceProviderId + "&state=Enabled"),
                                dataType: "json",
                                async: false,
                                success: function(json) {
                                  var jid = json.updatephysicalnetworkresponse.jobid;                               
                                  $.ajax({
                                    url: createURL("queryAsyncJobResult&jobId=" + jid),
                                    dataType: "json",
                                    async: false,
                                    success: function(json) {
                                      var result = json.queryasyncjobresultresponse;
                                      if (result.jobstatus == 0) {
                                        return; //Job has not completed
                                      } else {
                                        if (result.jobstatus == 1) { // Succeeded                                        
                                          //args.complete();                                        
                                        }
                                        else if (result.jobstatus == 2) { // Failed
                                          //args.error({message:result.jobresult.errortext});
                                          alert(fromdb(result.jobresult.errortext));
                                        }
                                      }
                                    },
                                    error: function(XMLHttpResponse) {
                                      //args.error();
                                      var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                      alert(errorMsg);
                                    }
                                  });                            
                                }
                              });   
                            }                                                        
                          }
                          //NaaS (end) 

                          $.ajax({
                            url: createURL("listCapabilities"),
                            dataType: "json",
                            async: false,
                            success: function(json) {
                              /* g_supportELB: "guest"   � ips are allocated on guest network (so use 'forvirtualnetwork' = false)
                               * g_supportELB: "public"  - ips are allocated on public network (so use 'forvirtualnetwork' = true)
                               * g_supportELB: "false"   � no ELB support
                               */
                              g_supportELB = json.listcapabilitiesresponse.capability.supportELB.toString(); //convert boolean to string if it's boolean
                              $.cookie('supportELB', g_supportELB, { expires: 1});

                              g_firewallRuleUiEnabled = json.listcapabilitiesresponse.capability.firewallRuleUiEnabled.toString(); //convert boolean to string if it's boolean
                              $.cookie('firewallRuleUiEnabled', g_firewallRuleUiEnabled, { expires: 1});

                              if (json.listcapabilitiesresponse.capability.userpublictemplateenabled != null) {
                                g_userPublicTemplateEnabled = json.listcapabilitiesresponse.capability.userpublictemplateenabled.toString(); //convert boolean to string if it's boolean
                                $.cookie('userpublictemplateenabled', g_userPublicTemplateEnabled, { expires: 1});
                              }

                              if (json.listcapabilitiesresponse.capability.securitygroupsenabled != null) {
                                g_directAttachSecurityGroupsEnabled = json.listcapabilitiesresponse.capability.securitygroupsenabled.toString(); //convert boolean to string if it's boolean
                                $.cookie('directattachsecuritygroupsenabled', g_directAttachSecurityGroupsEnabled, { expires: 1});
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
                  poll: testData.notifications.testPoll
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
              $.ajax({
                url: createURL("listZones&page=" + args.page + "&pagesize=" + pageSize),
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
              pageGenerator: cloudStack.zoneChart({
                dataProvider: function(args) {
                  args.response.success({data:args.jsonObj});
                },
                detailView: {
                  name: 'Zone details',
                  viewAll: { path: '_zone.pods', label: 'Pods' },
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

                        if(args.context.zones[0].networktype == "Advanced") {  //remove this after Brian fixes it to include $form in args
                          var vlan;
                          //if(args.$form.find('.form-item[rel=startVlan]').css("display") != "none") {  //comment out until Brian fixes it to include $form in args
                          var vlanStart = args.data.startVlan;
                          if(vlanStart != null && vlanStart.length > 0) {
                            var vlanEnd = args.data.endVlan;
                            if (vlanEnd != null && vlanEnd.length > 0)
                              vlan = vlanStart + "-" + vlanEnd;
                            else
                              vlan = vlanStart;
                            array1.push("&vlan=" + todb(vlan));
                          }
                          //}

                          //if(args.$form.find('.form-item[rel=guestcidraddress]').css("display") != "none") {  //comment out until Brian fixes it to include $form in args
                          array1.push("&guestcidraddress=" + todb(args.data.guestcidraddress));
                          //}
                        }  //remove this after Brian fixes it to include $form in args

                        $.ajax({
                          url: createURL("updateZone&id=" + args.context.zones[0].id + array1.join("")),
                          dataType: "json",
                          success: function(json) {
                            var item = json.updatezoneresponse.zone;
                            args.response.success({
                              actionFilter: zoneActionfilter,
                              data:item
                            });
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
                          url: createURL("updateZone&id=" + args.context.zones[0].id + "&allocationstate=Enabled"),
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
                          url: createURL("updateZone&id=" + args.context.zones[0].id + "&allocationstate=Disabled"),
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
                          url: createURL("deleteZone&id=" + args.context.zones[0].id),
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

                      preFilter: function(args) {
                        /*
                         var hiddenFields;
                         if(args.context.zones[0].networktype == "Basic")
                         hiddenFields = ["guestcidraddress", "startVlan", "endVlan"];
                         else if(args.context.zones[0].networktype == "Advanced")
                         hiddenFields = [];
                         return hiddenFields;
                         */
                        //comment out the above section until Brian fix it to include context in args
                        return [];
                      },

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
                          domain: { label: 'Domain' },

                          //only advanced zones have VLAN and CIDR Address
                          guestcidraddress: { label: 'Guest CIDR Address', isEditable: true },
                          vlan: { label: 'Vlan' },
                          startVlan: { label: 'Start Vlan', isEditable: true },
                          endVlan: { label: 'End Vlan', isEditable: true }
                        }
                      ],

                      dataProvider: function(args) {
                        args.response.success({
                          actionFilter: zoneActionfilter,
                          data: args.context.zones[0]
                        });
                      }
                    }
                  }
                }
              })
            }
          }
        },
        routers: {
          type: 'select',
          title: 'Virtual Appliances',
          id: 'routers',
          listView: {
            label: 'Virtual Appliances',
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

              restart: {
                label: 'reboot router',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to reboot router?';
                  },
                  notification: function(args) {
                    return 'rebooting router';
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
                  if(args.data.hostId == null) {
                    args.response.error("Host field is required");
                    return;
                  }
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
              }
            },
            dataProvider: function(args) {
              $.ajax({
                url: createURL('listRouters&page=' + args.page + '&pagesize=' + pageSize),
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
            }
          }
        },
        systemvms: {
          type: 'select',
          title: 'System VMs',
          listView: {
            label: 'System VMs',
            fields: {
              name: { label: 'Name' },
              systemvmtype: { label: 'Type' },
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
              $.ajax({
                url: createURL("listSystemVms&page=" + args.page + "&pagesize=" + pageSize),
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
                    url: createURL('startSystemVm&id=' + args.context.systemvms[0].id),
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
                    url: createURL('stopSystemVm&id=' + args.context.systemvms[0].id),
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
                    url: createURL('rebootSystemVm&id=' + args.context.systemvms[0].id),
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
                    url: createURL('destroySystemVm&id=' + args.context.systemvms[0].id),
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
                          url: createURL("listHosts&VirtualMachineId=" + args.context.systemvms[0].id),
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
                  if(args.data.hostId == null) {
                    args.response.error("Host field is required");
                    return;
                  }
                  $.ajax({
                    url: createURL("migrateSystemVm&hostid=" + args.data.hostId + "&virtualmachineid=" + args.context.systemvms[0].id),
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
              }
            }
          }
        }
      }
    }),
    subsections: {
      networks: {
        sectionSelect: { label: 'Network type' },
        sections: {
          publicNetworks: {
            type: 'select',
            title: 'Public network',
            listView: {
              section: 'networks',
              id: 'networks',
              fields: {
                //id: { label: "ID" },
                traffictype:  { label: "Traffic type" },
                broadcastdomaintype:  { label: "Broadcast domain type" }
              },

              dataProvider: function(args) {  //public network
                var showPublicNetwork = true;
                var zoneObj = args.context.zones[0];
                if(zoneObj.networktype == "Basic") {
                  //$("#add_network_button").hide();
                  $.ajax({
                    url: createURL("listExternalFirewalls&zoneid=" + zoneObj.id),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      var items = json.listexternalfirewallsresponse.externalfirewall;
                      if(items != null && items.length > 0) {
                        showPublicNetwork = true;
                        //$("#add_iprange_button,#tab_ipallocation").show();
                      }
                      else {
                        showPublicNetwork = false;
                        //$("#add_iprange_button,#tab_ipallocation").hide();
                      }
                    }
                  });
                }
                else { // Advanced zone
                  showPublicNetwork = true;
                  //$("#add_network_button,#add_iprange_button,#tab_ipallocation").show();
                  //listMidMenuItems2(("listNetworks&type=Direct&zoneId="+zoneObj.id), networkGetSearchParams, "listnetworksresponse", "network", directNetworkToMidmenu, directNetworkToRightPanel, directNetworkGetMidmenuId, false, 1);
                }

                if(showPublicNetwork == true && zoneObj.securitygroupsenabled == false) { //public network
                  $.ajax({
                    url: createURL("listNetworks&trafficType=Public&isSystem=true&zoneId="+zoneObj.id),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      var items = json.listnetworksresponse.network;
                      args.response.success({data: items});
                    }
                  });
                }
                else if (showPublicNetwork == true && zoneObj.securitygroupsenabled == true){
                  $.ajax({
                    url: createURL("listNetworks&type=Direct&trafficType=Guest&isSystem=true&zoneId="+zoneObj.id),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      var items = json.listnetworksresponse.network;
                      args.response.success({data: items});
                    }
                  });
                }
                else {
                  args.response.success({data: null});
                }
              }	,

              detailView: {
                //viewAll: { label: 'Hosts', path: 'instances' },

                actions: {
                  addIpRange: {
                    label: 'Add IP range',
                    messages: {
                      confirm: function(args) {
                        return 'Are you sure you want to add IP range?';
                      },
                      success: function(args) {
                        return 'IP range is being added.';
                      },
                      notification: function(args) {
                        return 'Adding IP range';
                      },
                      complete: function(args) {
                        return 'IP range has been added.';
                      }
                    },
                    createForm: {
                      title: 'Add IP range',
                      fields: {
                        vlanTagged: {
                          label: 'VLAN',
                          dependsOn: 'isBootable',
                          select: function(args) {
                            var items = [];
                            items.push({id: "untagged", description: "untagged"});
                            items.push({id: "tagged", description: "tagged"});
                            args.response.success({data: items});
                            args.$select.change(function() {
                              var $form = $(this).closest('form');
                              if($(this).val() == "tagged")
                                $form.find('.form-item[rel=vlanId]').css('display', 'inline-block');
                              else //"untagged"
                                $form.find('.form-item[rel=vlanId]').hide();
                            });
                          }
                        },
                        vlanId: { label: "VLAN ID" },

                        scope: {
                          label: 'Scope',
                          select: function(args) {
                            var zoneObj = args.context.zones[0];
                            var array1 = [];
                            if(zoneObj.securitygroupsenabled) {
                              array1.push({id: 'account-specific', description: 'account-specific'});
                            }
                            else {
                              array1.push({id: 'zone-wide', description: 'zone-wide'});
                              array1.push({id: 'domain-specific', description: 'domain-specific'});
                              array1.push({id: 'account-specific', description: 'account-specific'});
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
                            var zoneObj = args.context.zones[0];
                            if(zoneObj.domainid != null) { //list only domains under zoneObj.domainid
                              $.ajax({
                                url: createURL("listDomainChildren&id=" + zoneObj.domainid + "&isrecursive=true"),
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
                                url: createURL("listDomains&id=" + zoneObj.domainid),
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
                                url: createURL("listDomains"),
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

                        gateway: { label: 'Gateway' },
                        netmask: { label: 'Netmask' },
                        startip: { label: 'Start IP' },
                        endip: { label: 'End IP' }
                      }
                    },
                    action: function(args) {
                      var $form = args.$form;
                      var array1 = [];
                      array1.push("&zoneId=" + args.context.zones[0].id);

                      if (args.data.vlanTagged == "tagged")
                        array1.push("&vlan=" + todb(args.data.vlanId));
                      else
                        array1.push("&vlan=untagged");

                      if($form.find('.form-item[rel=domainId]').css("display") != "none") {
                        if($form.find('.form-item[rel=account]').css("display") != "none") {  //account-specific
                          array1.push("&domainId=" + args.data.domainId);
                          array1.push("&account=" + args.data.account);
                        }
                        else {  //domain-specific
                          array1.push("&domainId=" + args.data.domainId);
                          array1.push("&isshared=true");
                        }
                      }
                      else { //zone-wide
                        array1.push("&isshared=true");
                      }

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
                          args.response.success({data: item});
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

                tabs: {
                  details: {
                    title: 'Details',
                    fields: [
                      {
                        networkofferingdisplaytext:  { label: "Network offering description" }
                      },
                      {
                        id: { label: "ID" },
                        broadcastdomaintype: { label: 'Broadcast domain type' },
                        traffictype: { label: 'Traffic type' },
                        gateway: { label: 'Gateway' },
                        netmask: { label: 'Netmask' },
                        startip: { label: 'Start IP' },
                        endip: { label: 'End IP' },
                        zoneid: { label: 'Zone ID' },
                        networkofferingid: { label: 'Network offering ID' },
                        networkofferingname: { label: 'Network offering name' },
                        networkofferingavailability: { label: 'network offering availability' },
                        isshared: {
                          label: 'Shared',
                          converter: cloudStack.converters.toBooleanText
                        },
                        issystem: {
                          label: 'System',
                          converter: cloudStack.converters.toBooleanText
                        },
                        isdefault: {
                          label: 'Default',
                          converter: cloudStack.converters.toBooleanText
                        },
                        securitygroupenabled: {
                          label: 'Security group enabled',
                          converter: cloudStack.converters.toBooleanText
                        },
                        state: { label: 'State' },
                        related: { label: 'Related' },
                        dns1: { label: 'DNS 1' },
                        dns2: { label: 'DNS 2' },
                        vlan: { label: 'VLAN' },
                        domainid: { label: 'Domain ID' },
                        account: { label: 'Account' }
                      }
                    ],
                    dataProvider: function(args) {
                      args.response.success({data: args.context.publicNetworks[0]});
                    }
                  },
                  ipAllocations: {
                    title: 'IP Allocations',
                    multiple: true,
                    fields: [
                      {
                        id: { label: 'ID' },
                        vlan: { label: 'VLAN' },
                        gateway: { label: 'Gateway' },
                        netmask: { label: 'Netmask' },
                        startip: { label: 'Start IP' },
                        endip: { label: 'End IP' },
                        domain: { label: 'Domain' },
                        account: { label: 'Account' }
                      }
                    ],
                    dataProvider: function(args) {
                      $.ajax({
                        url: createURL("listVlanIpRanges&zoneid=" + args.context.zones[0].id + "&networkId=" + args.context.publicNetworks[0].id),
                        dataType: "json",
                        success: function(json) {
                          var items = json.listvlaniprangesresponse.vlaniprange;
                          args.response.success({
                            actionFilter: publicNetworkActionfilter,
                            data: items});
                        }
                      });
                    }
                  }
                }
              }
            }
          },
          directNetworks: {
            title: 'Direct network',
            type: 'select',
            listView: {
              section: 'networks',
              id: 'networks',
              fields: {
                //id: { label: "ID" },
                vlan:  { label: "VLAN ID" },
                traffictype:  { label: "Traffic type" }
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
                    fields: {
                      name: {
                        label: 'Name',
                        validation: { required: true }
                      },
                      description: {
                        label: 'Description',
                        validation: { required: true }
                      },
                      isDefault: {
                        label: "Default",
                        isBoolean: true
                      },
                      vlanTagged: {
                        label: 'VLAN',
                        select: function(args) {
                          args.response.success({data: {id: "tagged", description: "tagged"}});
                        }
                      },
                      vlanId: { label: "VLAN ID" },
                      scope: {
                        label: 'Scope',
                        select: function(args) {
                          var zoneObj = args.context.zones[0];
                          var array1 = [];
                          if(zoneObj.securitygroupsenabled) {
                            array1.push({id: 'account-specific', description: 'account-specific'});
                          }
                          else {
                            array1.push({id: 'zone-wide', description: 'zone-wide'});
                            array1.push({id: 'domain-specific', description: 'domain-specific'});
                            array1.push({id: 'account-specific', description: 'account-specific'});
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
                          var zoneObj = args.context.zones[0];
                          if(zoneObj.domainid != null) { //list only domains under zoneObj.domainid
                            $.ajax({
                              url: createURL("listDomainChildren&id=" + zoneObj.domainid + "&isrecursive=true"),
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
                              url: createURL("listDomains&id=" + zoneObj.domainid),
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
                              url: createURL("listDomains"),
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
                      gateway: { label: 'Gateway' },
                      netmask: { label: 'Netmask' },
                      startip: { label: 'Start IP' },
                      endip: { label: 'End IP' },
                      networkdomain: { label: 'Network domain' },
                      tags: { label: 'Tags' }
                    }
                  },

                  action: function(args) {
                    var array1 = [];
                    array1.push("&zoneId=" + args.context.zones[0].id);
                    array1.push("&name=" + todb(args.data.name));
                    array1.push("&displayText=" + todb(args.data.description));

                    if (args.data.vlanTagged == "tagged")
                      array1.push("&vlan=" + todb(args.data.vlanId));
                    else
                      array1.push("&vlan=untagged");

                    var $form = args.$form;

                    if($form.find('.form-item[rel=domainId]').css("display") != "none") {
                      if($form.find('.form-item[rel=account]').css("display") != "none") {  //account-specific
                        array1.push("&domainId=" + args.data.domainId);
                        array1.push("&account=" + args.data.account);
                      }
                      else {  //domain-specific
                        array1.push("&domainId=" + args.data.domainId);
                        array1.push("&isshared=true");
                      }
                    }
                    else { //zone-wide
                      array1.push("&isshared=true");
                    }

                    array1.push("&isDefault=" + (args.data.isDefault=="on"));
                    array1.push("&gateway=" + args.data.gateway);
                    array1.push("&netmask=" + args.data.netmask);
                    array1.push("&startip=" + args.data.startip);
                    array1.push("&endip=" + args.data.endip);

                    if(args.data.networkdomain != null && args.data.networkdomain.length > 0)
                      array1.push("&networkdomain=" + todb(args.data.networkdomain));

                    if(args.data.tags != null && args.data.tags.length > 0)
                      array1.push("&tags=" + todb(args.data.tags));

                    //get network offering of direct network
                    $.ajax({
                      url: createURL("listNetworkOfferings&guestiptype=Direct"),
                      dataType: "json",
                      async: false,
                      success: function(json) {
                        var networkOfferings = json.listnetworkofferingsresponse.networkoffering;
                        if (networkOfferings != null && networkOfferings.length > 0) {
                          for (var i = 0; i < networkOfferings.length; i++) {
                            if (networkOfferings[i].isdefault) {
                              array1.push("&networkOfferingId=" + networkOfferings[i].id);

                              // Create a network from this.
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
                            }
                          }
                        }
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
              dataProvider: function(args) { //direct netwoerk
                var zoneObj = args.context.zones[0];
                if(zoneObj.networktype == "Basic") {
                  args.response.success({data: null});
                }
                else { // Advanced zone
                  //$("#add_network_button,#add_iprange_button,#tab_ipallocation").show();
                  $.ajax({
                    url: createURL("listNetworks&type=Direct&zoneId="+zoneObj.id),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      var items = json.listnetworksresponse.network;
                      args.response.success({
                        actionFilter: directNetworkActionfilter,
                        data: items});
                    }
                  });
                }
              }	,

              detailView: {
                //viewAll: { label: 'Hosts', path: 'instances' },
                actions: {
                  addIpRange: {
                    label: 'Add IP range',
                    messages: {
                      confirm: function(args) {
                        return 'Are you sure you want to add IP range?';
                      },
                      success: function(args) {
                        return 'IP range is being added.';
                      },
                      notification: function(args) {
                        return 'Adding IP range';
                      },
                      complete: function(args) {
                        return 'IP range has been added.';
                      }
                    },
                    createForm: {
                      title: 'Add IP range',
                      preFilter: function(args) {
                        if(args.context.zones[0].securitygroupsenabled) {
                          args.$form.find('.form-item[rel=vlanId]').css('display', 'inline-block');
                          args.$form.find('.form-item[rel=gateway]').css('display', 'inline-block');
                          args.$form.find('.form-item[rel=netmask]').css('display', 'inline-block');
                        }
                        else {
                          args.$form.find('.form-item[rel=vlanId]').hide();
                          args.$form.find('.form-item[rel=gateway]').hide();
                          args.$form.find('.form-item[rel=netmask]').hide();
                        }
                      },
                      fields: {
                        vlanId: { label: 'VLAN ID' },
                        gateway: { label: 'Gateway' },
                        netmask: { label: 'Netmask' },
                        startip: { label: 'Start IP' },
                        endip: { label: 'End IP' }
                      }
                    },
                    action: function(args) {
                      var $form = args.$form;
                      var array1 = [];
                      if($form.find('.form-item[rel=vlanId]').css("display") != "none") {
                        array1.push("&vlan="+todb(args.data.vlanId));
                      }
                      //else {   //Bug 8950 (don't specify "vlan=untagged" if vlanId is not available when Adding Ip Range to Direct Network)
                      //  array1.push("&vlan=untagged");
                      //}

                      if($form.find('.form-item[rel=gateway]').css("display") != "none") {
                        array1.push("&gateway=" + todb(args.data.gateway));
                      }

                      if($form.find('.form-item[rel=netmask]').css("display") != "none") {
                        array1.push("&netmask=" + todb(args.data.netmask));
                      }

                      array1.push("&startip=" + todb(args.data.startip));

                      if(args.data.endip != null && args.data.endip.length > 0)
                        array1.push("&endip=" + todb(args.data.endip));


                      $.ajax({
                        url: createURL("createVlanIpRange&forVirtualNetwork=false&networkid=" + args.context.directNetworks[0].id + array1.join("")),
                        dataType: "json",
                        success: function(json) {
                          var item = json.createvlaniprangeresponse.vlan;
                          args.response.success({data: item});
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
                      array1.push("&displayText=" + todb(args.data.displaytext));
                      array1.push("&networkdomain=" + todb(args.data.networkdomain));
                      array1.push("&tags=" + todb(args.data.tags));

                      $.ajax({
                        url: createURL("updateNetwork&id=" + args.context.directNetworks[0].id + array1.join("")),
                        dataType: "json",
                        success: function(json) {
                          var jid = json.updatenetworkresponse.jobid;
                          args.response.success(
                            {_custom:
                             {jobId: jid,
                              getUpdatedItem: function(json) {
                                var item = json.queryasyncjobresultresponse.jobresult.network;
                                return {data: item};
                              },
                              getActionFilter: function() {
                                return directNetworkActionfilter;
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
                        url: createURL("deleteNetwork&id=" + args.context.directNetworks[0].id),
                        dataType: "json",
                        async: true,
                        success: function(json) {
                          var jid = json.deletenetworkresponse.jobid;
                          args.response.success(
                            {_custom:
                             {jobId: jid,
                              getUpdatedItem: function(json) {
                                return {}; //nothing in this template needs to be updated, in fact, this whole template has being deleted
                              },
                              getActionFilter: function() {
                                return directNetworkActionfilter;
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
                    title: 'Details', //direct network details
                    fields: [
                      {
                        name:  {
                          label: 'Name',
                          isEditable: true
                        }
                      },
                      {
                        id: { label: "ID" },
                        displaytext: {
                          label: 'Description',
                          isEditable: true
                        },
                        isdefault: {
                          label: 'Default',
                          converter: cloudStack.converters.toBooleanText
                        },
                        vlan: { label: 'VLAN ID' },
                        domain: { label: 'Domain' },
                        account: { label: 'Account' },
                        gateway: { label: 'Gateway' },
                        netmask: { label: 'Netmask' },
                        startip: { label: 'Start IP' },
                        endip: { label: 'End IP' },
                        networkdomain: {
                          label: 'Network domain',
                          isEditable: true
                        },
                        tags: {
                          label: 'Tags',
                          isEditable: true
                        }
                      }
                    ],
                    dataProvider: function(args) {
                      args.response.success({data: args.context.directNetworks[0]});
                    }
                  },
                  ipAllocations: {
                    title: 'IP Allocations',
                    multiple: true,
                    fields: [
                      {
                        id: { label: 'ID' },
                        vlan: { label: 'VLAN' },
                        startip: { label: 'Start IP' },
                        endip: { label: 'End IP' }
                      }
                    ],
                    dataProvider: function(args) {
                      $.ajax({
                        url: createURL("listVlanIpRanges&zoneid="+ args.context.zones[0].id + "&networkid="+ args.context.directNetworks[0].id),
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
            $.ajax({
              url: createURL("listPods&zoneid=" + args.ref.zoneID + "&page=" + args.page + "&pagesize=" + pageSize),
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
                desc: 'Please fill in the following information to add a new pod',

                preFilter: function(args) {
                  var $guestFields = args.$form.find('.form-item[rel=guestGateway], .form-item[rel=guestNetmask], .form-item[rel=startGuestIp], .form-item[rel=endGuestIp]');
                  if (args.context.zones[0].networktype == "Basic") {
                    $guestFields.css('display', 'inline-block');
                  }
                  else if(args.context.zones[0].networktype == "Advanced") { //advanced-mode network (zone-wide VLAN)
                    $guestFields.hide();
                  }
                },

                fields: {
                  name: {
                    label: 'Name',
                    validation: { required: true }
                  },
                  gateway: {
                    label: 'Gateway',
                    validation: { required: true }
                  },
                  netmask: {
                    label: 'Netmask',
                    validation: { required: true }
                  },
                  startip: {
                    label: 'Start IP',
                    validation: { required: true }
                  },
                  endip: {
                    label: 'End IP',
                    validation: { required: false }
                  },

                  //only basic zones show guest fields (begin)
                  guestGateway: {
                    label: 'Guest Gateway',
                    validation: { required: true },
                    isHidden: true
                  },
                  guestNetmask: {
                    label: 'Guest Netmask',
                    validation: { required: true },
                    isHidden: true
                  },
                  startGuestIp: {
                    label: 'Start Guest IP',
                    validation: { required: true },
                    isHidden: true
                  },
                  endGuestIp: {
                    label: 'End Guest IP',
                    validation: { required: false },
                    isHidden: true
                  }
                  //only basic zones show guest fields (end)
                }
              },

              action: function(args) {
                var array1 = [];
                array1.push("&zoneId=" + args.context.zones[0].id);
                array1.push("&name=" + todb(args.data.name));
                array1.push("&netmask=" + todb(args.data.netmask));
                array1.push("&startIp=" + todb(args.data.startip));

                var endip = args.data.endip;      //optional
                if (endip != null && endip.length > 0)
                  array1.push("&endIp=" + todb(endip));

                array1.push("&gateway=" + todb(args.data.gateway));

                $.ajax({
                  url: createURL("createPod" + array1.join("")),
                  dataType: "json",
                  success: function(json) {
                    var item = json.createpodresponse.pod;
                    args.response.success({data:item});
                    var podId = item.id;

                    //Create IP Range
                    //if($thisDialog.find("#guestip_container").css("display") != "none") {
                    if(args.context.zones[0].networktype == "Basic") {
                      var array1 = [];
                      array1.push("&vlan=untagged");
                      array1.push("&zoneid=" + args.context.zones[0].id);
                      array1.push("&podId=" + podId);
                      array1.push("&forVirtualNetwork=false"); //direct VLAN
                      array1.push("&gateway=" + todb(args.data.guestGateway));
                      array1.push("&netmask=" + todb(args.data.guestNetmask));
                      array1.push("&startip=" + todb(args.data.startGuestIp));

                      var endip = args.data.endGuestIp;
                      if(endip != null && endip.length > 0)
                        array1.push("&endip=" + todb(endip));

                      $.ajax({
                        url: createURL("createVlanIpRange" + array1.join("")),
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

              notification: {
                poll: function(args) {
                  args.complete();
                }
              },

              messages: {
                notification: function(args) {
                  return 'Added new pod';
                }
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

          detailView: {
            viewAll: { path: '_zone.clusters', label: 'Clusters' },
            tabFilter: function(args) {
              var hiddenTabs = [];
              var selectedZoneObj = args.context.zones[0];
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
              },

              addIpRange: {
                label: 'Add IP range' ,
                messages: {
                  confirm: function(args) {
                    return 'Please confirm that you want to add IP range to this pod';
                  },
                  success: function(args) {
                    return 'IP range is being added.';
                  },
                  notification: function(args) {
                    return 'Adding IP range';
                  },
                  complete: function(args) {
                    return 'IP range has been added.';
                  }
                },

                createForm: {
                  title: 'Add IP range',
                  fields: {
                    gateway: { label: 'Guest gateway' },
                    netmask: { label: 'Guest netmask' },
                    startip: { label: 'Guest start IP' },
                    endip: { label: 'Guest end IP' }
                  }
                },

                action: function(args) {
                  var array1 = [];
                  array1.push("&vlan=untagged");
                  array1.push("&zoneid=" + args.context.zones[0].id);
                  array1.push("&podId=" + args.context.pods[0].id);
                  array1.push("&forVirtualNetwork=false"); //direct VLAN
                  array1.push("&gateway=" + todb(args.data.gateway));
                  array1.push("&netmask=" + todb(args.data.netmask));
                  array1.push("&startip=" + todb(args.data.startip));
                  if(args.data.endip != null && args.data.endip.length > 0)
                    array1.push("&endip=" + todb(args.data.endip));

                  $.ajax({
                    url: createURL("createVlanIpRange" + array1.join("")),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      var item = json.createvlaniprangeresponse.vlan;
                      args.response.success({data: item});
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
                    name: { label: 'Name', isEditable: true },
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
                    description: { label: 'Description' },
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
              },

              //network devices tab is moved out of pod page at 3.0 UI. It will go to new network page.
              /*
              networkDevices: {
                title: 'Network devices',
                multiple: true,

                //preFilter won't work until Brian fix Bug #155
                preFilter: function(args) {
                  var networkDeviceObj = args.context.networkDevices[0];

                  var hiddenFields;
                  if(networkDeviceObj.pingstorageserverip == null || networkDeviceObj.pingstorageserverip.length == 0)
                    hiddenFields.push("pingstorageserverip");
                  if(networkDeviceObj.pingdir == null || networkDeviceObj.pingdir.length == 0)
                    hiddenFields.push("pingdir");
                  if(networkDeviceObj.tftpdir == null || networkDeviceObj.tftpdir.length == 0)
                    hiddenFields.push("tftpdir");

                  return hiddenFields;
                },


                fields: [
                  {
                    id: { label: 'ID' },
                    url: { label: 'URL' },
                    type: { label: 'Type' },
                    pingstorageserverip: { label: 'PING storage IP' },
                    pingdir: { label: 'PING directory' },
                    tftpdir: { label: 'TFTP directory' }
                  }
                ],
                dataProvider: function(args) {
                  var array1 = [];
                  array1.push("&networkdeviceparameterlist[0].zoneid=" + args.context.pods[0].zoneid);
	                array1.push("&networkdeviceparameterlist[0].podid=" + args.context.pods[0].id);
                  $.ajax({
                    url: createURL("listNetworkDevice" + array1.join("")),
                    dataType: "json",
                    success: function(json) {
                      var items = json.listnetworkdevice.networkdevice;
                      args.response.success({
                        actionFilter: networkDeviceActionfilter,
                        data: items
                      });
                    }
                  });
                }
              }
              */
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
            allocationstate: { label: 'Allocation State' },
            managedstate: { label: 'Managed State' }
          },

          //dataProvider: testData.dataProvider.listView('clusters'),
          dataProvider: function(args) {
            $.ajax({
              url: createURL("listClusters&zoneid=" + args.ref.zoneID + "&page=" + args.page + "&pagesize=" + pageSize),
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listclustersresponse.cluster;
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
                array1.push("&clustertype=CloudManaged");
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
                    args.response.success({data: item});
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
                    name: { label: 'Name' },
                  },
                  {
                    id: { label: 'ID' },
                    zonename: { label: 'Zone' },
                    podname: { label: 'Pod' },
                    hypervisortype: { label: 'Hypervisor' },
                    clustertype: { label: 'Clulster type' },
                    allocationstate: { label: 'Allocation State' },
                    managedstate: { label: 'Managed State' }
                  }
                ],

                //dataProvider: testData.dataProvider.detailView('clusters')
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
          fields: {
            name: { label: 'Name' },
            zonename: { label: 'Zone' },
            podname: { label: 'Pod' },
            clustername: { label: 'Cluster' }
          },

          //dataProvider: testData.dataProvider.listView('hosts'),
          dataProvider: function(args) {
            $.ajax({
              url: createURL("listHosts&type=Routing&zoneid=" + args.ref.zoneID + "&page=" + args.page + "&pagesize=" + pageSize),
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
                desc: 'Please fill in the following information to add a new host fro the specified zone configuration.',
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
                    args.response.success({data: item});
                  },
                  error: function(XMLHttpResponse) {
                    var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                    args.response.error(errorMsg);
                  }
                });
              },

              notification: {
                poll: function(args){
                  args.complete();
                }
              },

              messages: {
                notification: function(args) {
                  return 'Added new host';
                }
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
          detailView: {
            name: "Host details",
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
                    name: { label: 'Name' },
                  },
                  {
                    id: { label: 'ID' },
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

              },
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
            zonename: { label: 'Zone' },
            podname: { label: 'Pod' }
          },

          //dataProvider: testData.dataProvider.listView('primaryStorage'),
          dataProvider: function(args) {
            $.ajax({
              url: createURL("listStoragePools&zoneid=" + args.ref.zoneID + "&page=" + args.page + "&pagesize=" + pageSize),
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
                    args.response.success({data: item});
                  },
                  error: function(XMLHttpResponse) {
                    var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                    args.response.error(errorMsg);
                  }
                });
              },

              notification: {
                poll: function(args){
                  args.complete();
                }
              },

              messages: {
                notification: function(args) {
                  return 'Added new primary storage';
                }
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

          detailView: {
            name: "Primary storage details",
            actions: {
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
                    name: { label: 'Name' },
                  },
                  {
                    id: { label: 'ID' },
                    state: { label: 'State' },
                    zonename: { label: 'Zone' },
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
                    },
                    tags: { label: 'Primary tags' }
                  }
                ],

                //dataProvider: testData.dataProvider.detailView('primaryStorage')
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
            zonename: { label: 'Zone' }
          },

          //dataProvider: testData.dataProvider.listView('clusters'),
          dataProvider: function(args) {
            $.ajax({
              url: createURL("listHosts&type=SecondaryStorage&zoneid=" + args.ref.zoneID + "&page=" + args.page + "&pagesize=" + pageSize),
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
                  return 'Added new secondary storage';
                }
              }
            },

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
                    name: { label: 'Name' },
                  },
                  {
                    id: { label: 'ID' },
                    zonename: { label: 'Zone' },
                    type: { label: 'Type' },
                    ipaddress: { label: 'IP Address' }
                  }
                ],

                //dataProvider: testData.dataProvider.detailView('secondaryStorage')
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

  //action filters (begin)
  var zoneActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];
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

    var selectedZoneObj;
    $(zoneObjs).each(function(){
      if(this.id == podObj.zoneid) {
        selectedZoneObj = this;
        return false;  //break the $.each() loop
      }
    });

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

    if(jsonObj.allocationstate == "Disabled")
      allowedActions.push("enable");
    else if(jsonObj.allocationstate == "Enabled")
      allowedActions.push("disable");

    if(jsonObj.managedstate == "Managed")
      allowedActions.push("unmanage");
    else //PrepareUnmanaged , PrepareUnmanagedError, Unmanaged
      allowedActions.push("manage");

    allowedActions.push("delete");

    return allowedActions;
  }

  var hostActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];

    if (jsonObj.state == 'Up' || jsonObj.state == "Connecting") {
      allowedActions.push("edit");
      allowedActions.push("enableMaintenanceMode");
      allowedActions.push("forceReconnect");
    }
    else if(jsonObj.state == 'Down') {
      allowedActions.push("edit");
      allowedActions.push("enableMaintenanceMode");
      allowedActions.push("delete");
    }
    else if(jsonObj.state == "Alert") {
      allowedActions.push("edit");
      allowedActions.push("delete");
    }
    else if (jsonObj.state == "ErrorInMaintenance") {
      allowedActions.push("edit");
      allowedActions.push("enableMaintenanceMode");
      allowedActions.push("cancelMaintenanceMode");
    }
    else if (jsonObj.state == "PrepareForMaintenance") {
      allowedActions.push("edit");
      allowedActions.push("cancelMaintenanceMode");
    }
    else if (jsonObj.state == "Maintenance") {
      allowedActions.push("edit");
      allowedActions.push("cancelMaintenanceMode");
      allowedActions.push("delete");
    }
    else if (jsonObj.state == "Disconnected"){
      allowedActions.push("edit");
      allowedActions.push("delete");
    }
    return allowedActions;
  }

  var primarystorageActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];

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
