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
var g_mySession = null;
var g_sessionKey = null;
var g_role = null; // roles - root, domain-admin, ro-admin, user
var g_username = null;
var g_account = null;
var g_domainid = null;
var g_loginCmdText = null;
var g_enableLogging = false;
var g_timezoneoffset = null;
var g_timezone = null;
var g_supportELB = null;
var g_userPublicTemplateEnabled = "true";
var g_cloudstackversion = null;
var g_queryAsyncJobResultInterval = 3000;

//keyboard keycode
var keycode_Enter = 13;

//XMLHttpResponse.status
var ERROR_ACCESS_DENIED_DUE_TO_UNAUTHORIZED = 401;
var ERROR_INTERNET_NAME_NOT_RESOLVED = 12007;
var ERROR_INTERNET_CANNOT_CONNECT = 12029;
var ERROR_VMOPS_ACCOUNT_ERROR = 531;

// Default password is MD5 hashed.  Set the following variable to false to disable this.
var md5Hashed = false;
var md5HashedLogin = false;

//page size for API call (e.g."listXXXXXXX&pagesize=N" )
var pageSize = 20;

var rootAccountId = 1;
var havingSwift = false;
var havingS3 = false;

//async action
var pollAsyncJobResult = function(args) {
  $.ajax({
    url: createURL("queryAsyncJobResult&jobId=" + args._custom.jobId),
    dataType: "json",
    async: false,
    success: function(json) {
      var result = json.queryasyncjobresultresponse;
      if (result.jobstatus == 0) {
        return; //Job has not completed
      } 
      else {
        if (result.jobstatus == 1) { // Succeeded
          if(args._custom.getUpdatedItem != null && args._custom.getActionFilter != null) {
            args.complete({
              data: args._custom.getUpdatedItem(json),
              actionFilter: args._custom.getActionFilter()
            });
          }
          else if(args._custom.getUpdatedItem != null && args._custom.getActionFilter == null) {
            args.complete({
              data: args._custom.getUpdatedItem(json)
            });
          }
          else {
            args.complete({ data: json.queryasyncjobresultresponse.jobresult });
          }
										
					if(args._custom.fullRefreshAfterComplete == true) {
						setTimeout(function() {
							$(window).trigger('cloudStack.fullRefresh');
						}, 500);
					}

          if (args._custom.onComplete) {
            args._custom.onComplete(json, args._custom);
          }
        }
        else if (result.jobstatus == 2) { // Failed          
          var msg = (result.jobresult.errortext == null)? "": result.jobresult.errortext;
					if (args._custom.getUpdatedItemWhenAsyncJobFails != null && args._custom.getActionFilter != null) {
					  args.error({message: msg, updatedData: args._custom.getUpdatedItemWhenAsyncJobFails(), actionFilter: args._custom.getActionFilter()});
					} else if (args._custom.getUpdatedItemWhenAsyncJobFails != null && args._custom.getActionFilter == null) {
					  args.error({message: msg, updatedData: args._custom.getUpdatedItemWhenAsyncJobFails()});
					}
					else {
					  args.error({message: msg});
					}
        }
      }
    },
    error: function(XMLHttpResponse) {
      args.error();
    }
  });
}

//API calls
function createURL(apiName, options) {
  if (!options) options = {};
  var urlString = clientApiUrl + "?" + "command=" + apiName +"&response=json&sessionkey=" + g_sessionKey;

  if (cloudStack.context && cloudStack.context.projects && !options.ignoreProject) {
    urlString = urlString + '&projectid=' + cloudStack.context.projects[0].id;
  }
   
  return urlString;
}

function todb(val) {
  return encodeURIComponent(val);
}

//LB provider map
var lbProviderMap = {
  "publicLb": {
    "non-vpc": ["VirtualRouter", "Netscaler", "F5"],
    "vpc": ["VpcVirtualRouter", "Netscaler"]
  },
  "internalLb": {
    "non-vpc": [],
    "vpc": ["InternalLbVm"]
  }
};

//Add Guest Network in Advanced zone (for root-admin only)
var addGuestNetworkDialog = {
  zoneObjs: [],   
  physicalNetworkObjs: [],
  networkOfferingObjs: [],
  def: {
    label: 'label.add.guest.network',

    messages: {     
      notification: function(args) {
        return 'label.add.guest.network';
      }
    },

    preFilter: function(args) {      
      if(isAdmin()) 
        return true;     
      else
        return false;      
    },
    
    createForm: {
      title: 'label.add.guest.network',  //Add Shared Network in advanced zone

      preFilter: function(args) {   
        if('zones' in args.context) {//Infrastructure menu > zone detail > guest traffic type > network tab (only shown in advanced zone) > add guest network dialog
          args.$form.find('.form-item[rel=zoneId]').hide();
          args.$form.find('.form-item[rel=physicalNetworkId]').hide();
        }
        else {//Network menu > guest network section > add guest network dialog
          args.$form.find('.form-item[rel=zoneId]').css('display', 'inline-block');
          args.$form.find('.form-item[rel=physicalNetworkId]').css('display', 'inline-block');
        }  
      },
      
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
        
        zoneId: {
          label: 'label.zone',
          validation: { required: true },
          docID: 'helpGuestNetworkZone',
          select: function(args) {   
            if('zones' in args.context) {//Infrastructure menu > zone detail > guest traffic type > network tab (only shown in advanced zone) > add guest network dialog
              addGuestNetworkDialog.zoneObjs = args.context.zones; //i.e. only one zone entry
            }
            else {//Network menu > guest network section > add guest network dialog
              $.ajax({
                url: createURL('listZones'),
                async: false,
                success: function(json) {
                  addGuestNetworkDialog.zoneObjs = []; //reset                   
                  var items = json.listzonesresponse.zone;
                  if(items != null) {
                    for(var i = 0; i < items.length; i++) {
                      if(items[i].networktype == 'Advanced') {
                        addGuestNetworkDialog.zoneObjs.push(items[i]);
                      }
                    }
                  }                     
                }
              });
            }              
            args.response.success({
              data: $.map(addGuestNetworkDialog.zoneObjs, function(zone) {
                return {
                  id: zone.id,
                  description: zone.name
                };
              })
            });    
          },
          isHidden: true
        },   
        
        physicalNetworkId: {
          label: 'label.physical.network',
          dependsOn: 'zoneId',
          select: function(args) {   
            if('physicalNetworks' in args.context) {
              addGuestNetworkDialog.physicalNetworkObjs = args.context.physicalNetworks;
            }
            else {
              var selectedZoneId = args.$form.find('.form-item[rel=zoneId]').find('select').val();
              $.ajax({
                url: createURL('listPhysicalNetworks'),
                data: {
                  zoneid: selectedZoneId
                },
                async: false,
                success: function(json) {               
                  addGuestNetworkDialog.physicalNetworkObjs = json.listphysicalnetworksresponse.physicalnetwork;                  
                }
              });
            }        
            var items = [];
            if(addGuestNetworkDialog.physicalNetworkObjs != null) {
              for(var i = 0; i < addGuestNetworkDialog.physicalNetworkObjs.length; i++) {
                items.push({ id: addGuestNetworkDialog.physicalNetworkObjs[i].id, description: addGuestNetworkDialog.physicalNetworkObjs[i].name });
              }
            }
            args.response.success({data: items});            
          },
          isHidden: true
        },
        
        vlanId: {
          label: 'label.vlan.id',
          docID: 'helpGuestNetworkZoneVLANID'
        },
        isolatedpvlanId: {
          label: 'Secondary Isolated VLAN ID'                           
        },
        
        scope: {
          label: 'label.scope',
          docID: 'helpGuestNetworkZoneScope',
          select: function(args) {
            var selectedZoneId = args.$form.find('.form-item[rel=zoneId]').find('select').val();
            var selectedZoneObj = {};
            if(addGuestNetworkDialog.zoneObjs != null && selectedZoneId != "") {
              for(var i = 0; i < addGuestNetworkDialog.zoneObjs.length; i++) {
                if(addGuestNetworkDialog.zoneObjs[i].id == selectedZoneId) {
                  selectedZoneObj = addGuestNetworkDialog.zoneObjs[i];
                  break;
                }
              }
            }      
                        
            var array1 = [];                              
            if(selectedZoneObj.networktype == "Advanced" && selectedZoneObj.securitygroupsenabled == true) {                                
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
            var selectedZoneId = args.$form.find('.form-item[rel=zoneId]').find('select').val();
            var selectedZoneObj = {};
            if(addGuestNetworkDialog.zoneObjs != null && selectedZoneId != "") {
              for(var i = 0; i < addGuestNetworkDialog.zoneObjs.length; i++) {
                if(addGuestNetworkDialog.zoneObjs[i].id == selectedZoneId) {
                  selectedZoneObj = addGuestNetworkDialog.zoneObjs[i];
                  break;
                }
              }
            }            
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
            var data = {
              state: 'Enabled',
              zoneid: args.$form.find('.form-item[rel=zoneId]').find('select').val()
            };
                        
            var selectedPhysicalNetworkObj = [];
            var selectedPhysicalNetworkId = args.$form.find('.form-item[rel=physicalNetworkId]').find('select').val();
            if(addGuestNetworkDialog.physicalNetworkObjs != null) {
              for(var i = 0; i < addGuestNetworkDialog.physicalNetworkObjs.length; i++) {
                if(addGuestNetworkDialog.physicalNetworkObjs[i].id == selectedPhysicalNetworkId) {
                  selectedPhysicalNetworkObj = addGuestNetworkDialog.physicalNetworkObjs[i];
                  break;
                }
              }
            }     
            if(selectedPhysicalNetworkObj.tags != null && selectedPhysicalNetworkObj.tags.length > 0) {
              $.extend(data, {
                tags: selectedPhysicalNetworkObj.tags
              });              
            }            
            
            //Network tab in Guest Traffic Type in Infrastructure menu is only available when it's under Advanced zone.
            //zone dropdown in add guest network dialog includes only Advanced zones.  
            if(args.scope == "zone-wide" || args.scope == "domain-specific") {
              $.extend(data, {
                guestiptype: 'Shared'
              });
            }

            var items= [];             
            $.ajax({
              url: createURL('listNetworkOfferings'),
              data: data,
              async: false,
              success: function(json) {        
                addGuestNetworkDialog.networkOfferingObjs = json.listnetworkofferingsresponse.networkoffering;
                if (addGuestNetworkDialog.networkOfferingObjs != null && addGuestNetworkDialog.networkOfferingObjs.length > 0) {                  
                  var selectedZoneId = args.$form.find('.form-item[rel=zoneId]').find('select').val();
                  var selectedZoneObj = {};
                  if(addGuestNetworkDialog.zoneObjs != null && selectedZoneId != "") {
                    for(var i = 0; i < addGuestNetworkDialog.zoneObjs.length; i++) {
                      if(addGuestNetworkDialog.zoneObjs[i].id == selectedZoneId) {
                        selectedZoneObj = addGuestNetworkDialog.zoneObjs[i];
                        break;
                      }
                    }
                  }     
                  for (var i = 0; i < addGuestNetworkDialog.networkOfferingObjs.length; i++) {    
                    //for zone-wide network in Advanced SG-enabled zone, list only SG network offerings 
                    if(selectedZoneObj.networktype == 'Advanced' && selectedZoneObj.securitygroupsenabled == true) {                                    
                      if(args.scope == "zone-wide") { 
                        var includingSecurityGroup = false;
                        var serviceObjArray = addGuestNetworkDialog.networkOfferingObjs[i].service;
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
                    items.push({id: addGuestNetworkDialog.networkOfferingObjs[i].id, description: addGuestNetworkDialog.networkOfferingObjs[i].displaytext});
                  }
                }
              }
            });            
            args.response.success({data: items});

            args.$select.change(function(){
              var $form = $(this).closest("form");
              var selectedNetworkOfferingId = $(this).val();
              $(addGuestNetworkDialog.networkOfferingObjs).each(function(){
                if(this.id == selectedNetworkOfferingId) {                                    
                  if(this.specifyvlan == false) {
                    $form.find('.form-item[rel=vlanId]').hide();
                    cloudStack.dialog.createFormField.validation.required.remove($form.find('.form-item[rel=vlanId]')); //make vlanId optional  
                    
                    $form.find('.form-item[rel=isolatedpvlanId]').hide();
                  }
                  else {
                    $form.find('.form-item[rel=vlanId]').css('display', 'inline-block');                                      
                    cloudStack.dialog.createFormField.validation.required.add($form.find('.form-item[rel=vlanId]'));    //make vlanId required  
                    
                    $form.find('.form-item[rel=isolatedpvlanId]').css('display', 'inline-block');             
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
          docID: 'helpGuestNetworkZoneStartIP'
        },
        endipv4: { 
          label: 'IPv4 End IP',               
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
          docID: 'helpGuestNetworkZoneStartIP'
        },
        endipv6: { 
          label: 'IPv6 End IP',               
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
      if (
        ((args.data.ip4gateway.length == 0) && (args.data.ip4Netmask.length == 0) && (args.data.startipv4.length == 0) && (args.data.endipv4.length == 0))
        && 
        ((args.data.ip6gateway.length == 0) && (args.data.ip6cidr.length == 0) && (args.data.startipv6.length == 0) && (args.data.endipv6.length == 0))
      )
      {
        args.response.error("Either IPv4 fields or IPv6 fields need to be filled when adding a guest network");
        return;
      }
    
      var $form = args.$form;

      var array1 = [];  
      array1.push("&zoneId=" + args.data.zoneId);
      array1.push("&networkOfferingId=" + args.data.networkOfferingId);

      //Pass physical network ID to createNetwork API only when network offering's guestiptype is Shared.
      var selectedNetworkOfferingObj;
      if(addGuestNetworkDialog.networkOfferingObjs != null) {
        for(var i = 0; i < addGuestNetworkDialog.networkOfferingObjs.length; i++) {
          if(addGuestNetworkDialog.networkOfferingObjs[i].id == args.data.networkOfferingId) {
            selectedNetworkOfferingObj = addGuestNetworkDialog.networkOfferingObjs[i]
            break;
          }
        }
      }
      
      if(selectedNetworkOfferingObj.guestiptype == "Shared")
        array1.push("&physicalnetworkid=" + args.data.physicalNetworkId);

      array1.push("&name=" + todb(args.data.name));
      array1.push("&displayText=" + todb(args.data.description));

      if(($form.find('.form-item[rel=vlanId]').css("display") != "none") && (args.data.vlanId != null && args.data.vlanId.length > 0))
        array1.push("&vlan=" + todb(args.data.vlanId));
      
      if(($form.find('.form-item[rel=isolatedpvlanId]').css("display") != "none") && (args.data.isolatedpvlanId != null && args.data.isolatedpvlanId.length > 0))
        array1.push("&isolatedpvlan=" + todb(args.data.isolatedpvlanId));
                              
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
        array1.push("&ip6gateway=" + args.data.ip6gateway);
      if(args.data.ip6cidr != null && args.data.ip6cidr.length > 0)
        array1.push("&ip6cidr=" + args.data.ip6cidr);
      if(($form.find('.form-item[rel=startipv6]').css("display") != "none") && (args.data.startipv6 != null && args.data.startipv6.length > 0))
        array1.push("&startipv6=" + args.data.startipv6);
      if(($form.find('.form-item[rel=endipv6]').css("display") != "none") && (args.data.endipv6 != null && args.data.endipv6.length > 0))
        array1.push("&endipv6=" + args.data.endipv6);
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
}
  

// Role Functions
function isAdmin() {
  return (g_role == 1);
}

function isDomainAdmin() {
  return (g_role == 2);
}

function isUser() {
  return (g_role == 0);
}

function isSelfOrChildDomainUser(username, useraccounttype, userdomainid, iscallerchilddomain) {
	if(username == g_username) { //is self
        return true;
    } else if(isDomainAdmin()
        && iscallerchilddomain
        && (useraccounttype == 0)) { //domain admin to user
        return true;
	} else if(isDomainAdmin()
        && iscallerchilddomain 
		&& (userdomainid != g_domainid) ) { //domain admin to subdomain admin and user
        return true;
    } else {
        return false;
    } 
}

// FUNCTION: Handles AJAX error callbacks.  You can pass in an optional function to
// handle errors that are not already handled by this method.
function handleError(XMLHttpResponse, handleErrorCallback) {
  // User Not authenticated
  if (XMLHttpResponse.status == ERROR_ACCESS_DENIED_DUE_TO_UNAUTHORIZED) {
    $("#dialog_session_expired").dialog("open");
  }
  else if (XMLHttpResponse.status == ERROR_INTERNET_NAME_NOT_RESOLVED) {
    $("#dialog_error_internet_not_resolved").dialog("open");
  }
  else if (XMLHttpResponse.status == ERROR_INTERNET_CANNOT_CONNECT) {
    $("#dialog_error_management_server_not_accessible").dialog("open");
  }
  else if (XMLHttpResponse.status == ERROR_VMOPS_ACCOUNT_ERROR && handleErrorCallback != undefined) {
    handleErrorCallback();
  }
  else if (handleErrorCallback != undefined) {
    handleErrorCallback();
  }
  else {
    var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
    $("#dialog_error").text(_s(errorMsg)).dialog("open");
  }
}

function parseXMLHttpResponse(XMLHttpResponse) {
  if(isValidJsonString(XMLHttpResponse.responseText) == false) {
    return "";
  }

  //var json = jQuery.parseJSON(XMLHttpResponse.responseText);
  var json = JSON.parse(XMLHttpResponse.responseText);
  if (json != null) {
    var property;
    for(property in json) {
    var errorObj = json[property];		
		if(errorObj.errorcode == 401 && errorObj.errortext == "unable to verify user credentials and/or request signature")
		  return _l('label.session.expired');
		else
      return _s(errorObj.errortext);
     }
  } 
	else {
    return "";
  }
}

function isValidJsonString(str) {
  try {
    JSON.parse(str);
  }
  catch (e) {
    return false;
  }
  return true;
}

cloudStack.validate = {
  vmHostName: function(args) {	  	
		// 1 ~ 63 characters long 
		// ASCII letters 'a' through 'z', 'A' through 'Z', digits '0' through '9', hyphen ('-') 
		// must start with a letter 
		// must end with a letter or a digit (must not end with a hyphen)
		var regexp = /^[a-zA-Z]{1}[a-zA-Z0-9\-]{0,61}[a-zA-Z0-9]{0,1}$/;
    var b = regexp.test(args); //true or false		
		if(b == false)
	    cloudStack.dialog.notice({ message: 'message.validate.instance.name' });	
	  return b;
	}
}

cloudStack.preFilter = {
  createTemplate: function(args) {
    if(isAdmin()) {
      args.$form.find('.form-item[rel=isPublic]').css('display', 'inline-block');
      args.$form.find('.form-item[rel=isFeatured]').css('display', 'inline-block');
    }
    else {
      if (g_userPublicTemplateEnabled == "true") {
        args.$form.find('.form-item[rel=isPublic]').css('display', 'inline-block');
      }
      else {
        args.$form.find('.form-item[rel=isPublic]').hide();
      }
      args.$form.find('.form-item[rel=isFeatured]').hide();
    }
  },
	addLoadBalancerDevice: function(args) { //add netscaler device OR add F5 device	  
		args.$form.find('.form-item[rel=dedicated]').bind('change', function() { 		  
			var $dedicated = args.$form.find('.form-item[rel=dedicated]');
			var $capacity = args.$form.find('.form-item[rel=capacity]');											
			if($dedicated.find('input[type=checkbox]:checked').length > 0) {												
				$capacity.hide();
				$capacity.find('input[type=text]').val('1');
			}
			else if($dedicated.find('input[type=checkbox]:unchecked').length > 0) {
				$capacity.css('display', 'inline-block');
				$capacity.find('input[type=text]').val('');												
			}			
		});			
		args.$form.change();		
	}	
}

cloudStack.actionFilter = {
  guestNetwork: function(args) {    
    var jsonObj = args.context.item;
		var allowedActions = [];
                allowedActions.push('replaceacllist');
		if(jsonObj.type == 'Isolated') {
		  allowedActions.push('edit');		//only Isolated network is allowed to upgrade to a different network offering (Shared network is not allowed to)
			allowedActions.push('restart');   
		  allowedActions.push('remove');
		}
		else if(jsonObj.type == 'Shared') {
		  if(isAdmin()) {
				allowedActions.push('restart');   
				allowedActions.push('remove');
			}
		}		
		return allowedActions;
	}
}

var roleTypeUser = "0";
var roleTypeAdmin = "1";
var roleTypeDomainAdmin = "2";

cloudStack.converters = {
  convertBytes: function(bytes) {
    if (bytes < 1024 * 1024) {
      return (bytes / 1024).toFixed(2) + " KB";
    } else if (bytes < 1024 * 1024 * 1024) {
      return (bytes / 1024 / 1024).toFixed(2) + " MB";
    } else if (bytes < 1024 * 1024 * 1024 * 1024) {
      return (bytes / 1024 / 1024 / 1024).toFixed(2) + " GB";
    } else {
      return (bytes / 1024 / 1024 / 1024 / 1024).toFixed(2) + " TB";
    }
  },
	toLocalDate: function(UtcDate) {	 
		var localDate = "";		
		if (UtcDate != null && UtcDate.length > 0) {
	    var disconnected = new Date();
	    disconnected.setISO8601(UtcDate);	
	    	
	    if(g_timezoneoffset != null) 
	      localDate = disconnected.getTimePlusTimezoneOffset(g_timezoneoffset);
	    else 
	      localDate = disconnected.toUTCString();
             // localDate = disconnected.getTimePlusTimezoneOffset(0);	 
    }
		return localDate; 		
	},
  toBooleanText: function(booleanValue) {
    if(booleanValue == true)
      return "Yes";

    return "No";
  },
  convertHz: function(hz) {
    if (hz == null)
      return "";

    if (hz < 1000) {
      return hz + " MHz";
    } else {
      return (hz / 1000).toFixed(2) + " GHz";
    }
  },
  toDayOfWeekDesp: function(dayOfWeek) {
    if (dayOfWeek == "1")
      return "Sunday";
    else if (dayOfWeek == "2")
      return "Monday";
    else if (dayOfWeek == "3")
      return "Tuesday";
    else if (dayOfWeek == "4")
      return "Wednesday";
    else if (dayOfWeek == "5")
      return "Thursday"
    else if (dayOfWeek == "6")
      return "Friday";
    else if (dayOfWeek == "7")
      return "Saturday";
  },
  toDayOfWeekDesp: function(dayOfWeek) {
    if (dayOfWeek == "1")
      return "Sunday";
    else if (dayOfWeek == "2")
      return "Monday";
    else if (dayOfWeek == "3")
      return "Tuesday";
    else if (dayOfWeek == "4")
      return "Wednesday";
    else if (dayOfWeek == "5")
      return "Thursday"
    else if (dayOfWeek == "6")
      return "Friday";
    else if (dayOfWeek == "7")
      return "Saturday";
  },
  toNetworkType: function(usevirtualnetwork) {
    if(usevirtualnetwork == true || usevirtualnetwork == "true")
      return "Public";
    else
      return "Direct";
  },
  toRole: function(type) {
    if (type == roleTypeUser) {
      return "User";
    } else if (type == roleTypeAdmin) {
      return "Admin";
    } else if (type == roleTypeDomainAdmin) {
      return "Domain-Admin";
    }
  },
  toAlertType: function(alertCode) {
    switch (alertCode) {
    case 0 : return _l('label.memory');
    case 1 : return _l('label.cpu');
    case 2 : return _l('label.storage');
    case 3 : return _l('label.primary.storage');
    case 4 : return _l('label.public.ips');
    case 5 : return _l('label.management.ips');
    case 6 : return _l('label.secondary.storage');
    case 7 : return _l('label.host');
    case 9 : return _l('label.domain.router');
    case 10 : return _l('label.console.proxy');

    // These are old values -- can be removed in the future
    case 8 : return "User VM";
    case 11 : return "Routing Host";
    case 12 : return "Storage";
    case 13 : return "Usage Server";
    case 14 : return "Management Server";
    case 15 : return "Domain Router";
    case 16 : return "Console Proxy";
    case 17 : return "User VM";
    case 18 : return "VLAN";
    case 19 : return "Secondary Storage VM";
    case 20 : return "Usage Server";
    case 21 : return "Storage";
    case 22 : return "Update Resource Count";
    case 23 : return "Usage Sanity Result";
    case 24 : return "Direct Attached Public IP";
    case 25 : return "Local Storage";
    case 26 : return "Resource Limit Exceeded";
    }
  },

  toCapacityCountType:function(capacityCode){
   switch(capacityCode){
    case 0 : return _l('label.memory');
    case 1 : return _l('label.cpu');
    case 2 : return _l('label.storage');
    case 3 : return _l('label.primary.storage');
    case 4 : return _l('label.public.ips');
    case 5 : return _l('label.management.ips');
    case 6 : return _l('label.secondary.storage');
    case 7 : return _l('label.vlan');
    case 8 : return _l('label.direct.ips');
    case 9 : return _l('label.local.storage');
    case 10 : return "Routing Host";
    case 11 : return "Storage";
    case 12 : return "Usage Server";
    case 13 : return "Management Server";
    case 14 : return "Domain Router";
    case 15 : return "Console Proxy";
    case 16 : return "User VM";
    case 17 : return "VLAN";
    case 18 : return "Secondary Storage VM";
      }
    },

  convertByType: function(alertCode, value) {
    switch(alertCode) {
      case 0: return cloudStack.converters.convertBytes(value);
      case 1: return cloudStack.converters.convertHz(value);
      case 2: return cloudStack.converters.convertBytes(value);
      case 3: return cloudStack.converters.convertBytes(value);
      case 6: return cloudStack.converters.convertBytes(value);
      case 9: return cloudStack.converters.convertBytes(value);
      case 11: return cloudStack.converters.convertBytes(value);
    }

    return value;
  }
}

//data parameter passed to API call in listView
function listViewDataProvider(args, data) {   
  //search 
	if(args.filterBy != null) {				    
		if(args.filterBy.advSearch != null && typeof(args.filterBy.advSearch) == "object") { //advanced search		  
			for(var key in args.filterBy.advSearch) {	
				if(key == 'tagKey' && args.filterBy.advSearch[key].length > 0) {
				  $.extend(data, {
					  'tags[0].key': args.filterBy.advSearch[key]
					});
				}	
				else if(key == 'tagValue' && args.filterBy.advSearch[key].length > 0) {
				  $.extend(data, {
					  'tags[0].value': args.filterBy.advSearch[key]
					});					
				}	
				else if(args.filterBy.advSearch[key] != null && args.filterBy.advSearch[key].length > 0) {				  
					data[key] = args.filterBy.advSearch[key]; //do NOT use  $.extend(data, { key: args.filterBy.advSearch[key] }); which will treat key variable as "key" string 
        }					
			}			
		}		
		else if(args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) { //basic search
			switch(args.filterBy.search.by) {
			  case "name":
				  if(args.filterBy.search.value.length > 0) {
				    $.extend(data, {
					    keyword: args.filterBy.search.value
					  });					
				  }
				  break;
			}
		}		
	}

	//pagination
	$.extend(data, {
	  listAll: true,		
		page: args.page,
		pagesize: pageSize		
	});	
}

//used by infrastruct page and network page	
var addExtraPropertiesToGuestNetworkObject = function(jsonObj) {  
	jsonObj.networkdomaintext = jsonObj.networkdomain;
	jsonObj.networkofferingidText = jsonObj.networkofferingid;

	if(jsonObj.acltype == "Domain") {
		if(jsonObj.domainid == rootAccountId)
			jsonObj.scope = "All";
		else
			jsonObj.scope = "Domain (" + jsonObj.domain + ")";
	}
	else if (jsonObj.acltype == "Account"){
		if(jsonObj.project != null)
			jsonObj.scope = "Account (" + jsonObj.domain + ", " + jsonObj.project + ")";
		else
			jsonObj.scope = "Account (" + jsonObj.domain + ", " + jsonObj.account + ")";
	}

	if(jsonObj.vlan == null && jsonObj.broadcasturi != null) {
		jsonObj.vlan = jsonObj.broadcasturi.replace("vlan://", "");   	
	}
}	

//find service object in network object
function ipFindNetworkServiceByName(pName, networkObj) {    
    if(networkObj == null)
        return null;
    if(networkObj.service != null) {
	    for(var i=0; i<networkObj.service.length; i++) {
	        var networkServiceObj = networkObj.service[i];
	        if(networkServiceObj.name == pName)
	            return networkServiceObj;
	    }
    }    
    return null;
}
//find capability object in service object in network object
function ipFindCapabilityByName(pName, networkServiceObj) {  
    if(networkServiceObj == null)
        return null;  
    if(networkServiceObj.capability != null) {
	    for(var i=0; i<networkServiceObj.capability.length; i++) {
	        var capabilityObj = networkServiceObj.capability[i];
	        if(capabilityObj.name == pName)
	            return capabilityObj;
	    }
    }    
    return null;
}

//compose URL for adding primary storage
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

function rbdURL(monitor, pool, id, secret) {
	var url;

	/*
	Replace the + and / symbols by - and _ to have URL-safe base64 going to the API
	It's hacky, but otherwise we'll confuse java.net.URI which splits the incoming URI
	*/
	secret = secret.replace("+", "-");
	secret = secret.replace("/", "_");

	if (id != null && secret != null) {
		monitor = id + ":" + secret + "@" + monitor;
	}

	if(pool.substring(0,1) != "/")
		pool = "/" + pool;

	if(monitor.indexOf("://")==-1)
		url = "rbd://" + monitor + pool;
	else
		url = monitor + pool;

	return url;
}

function clvmURL(vgname) {
	var url;
	if(vgname.indexOf("://")==-1)
		url = "clvm://localhost/" + vgname;
	else
		url = vgname;
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


//VM Instance
function getVmName(p_vmName, p_vmDisplayname) {
  if(p_vmDisplayname == null)
    return _s(p_vmName);

  var vmName = null;
  if (p_vmDisplayname != p_vmName) {
    vmName = _s(p_vmName) + " (" + _s(p_vmDisplayname) + ")";
  } else {
    vmName = _s(p_vmName);
  }
  return vmName;
}

var timezoneMap = new Object();
timezoneMap['Etc/GMT+12']='[UTC-12:00] GMT-12:00';
timezoneMap['Etc/GMT+11']='[UTC-11:00] GMT-11:00';
timezoneMap['Pacific/Samoa']='[UTC-11:00] Samoa Standard Time';
timezoneMap['Pacific/Honolulu']='[UTC-10:00] Hawaii Standard Time';
timezoneMap['US/Alaska']='[UTC-09:00] Alaska Standard Time';
timezoneMap['America/Los_Angeles']='[UTC-08:00] Pacific Standard Time';
timezoneMap['Mexico/BajaNorte']='[UTC-08:00] Baja California';
timezoneMap['US/Arizona']='[UTC-07:00] Arizona';
timezoneMap['US/Mountain']='[UTC-07:00] Mountain Standard Time';
timezoneMap['America/Chihuahua']='[UTC-07:00] Chihuahua, La Paz';
timezoneMap['America/Chicago']='[UTC-06:00] Central Standard Time';
timezoneMap['America/Costa_Rica']='[UTC-06:00] Central America';
timezoneMap['America/Mexico_City']='[UTC-06:00] Mexico City, Monterrey';
timezoneMap['Canada/Saskatchewan']='[UTC-06:00] Saskatchewan';
timezoneMap['America/Bogota']='[UTC-05:00] Bogota, Lima';
timezoneMap['America/New_York']='[UTC-05:00] Eastern Standard Time';
timezoneMap['America/Caracas']='[UTC-04:00] Venezuela Time';
timezoneMap['America/Asuncion']='[UTC-04:00] Paraguay Time';
timezoneMap['America/Cuiaba']='[UTC-04:00] Amazon Time';
timezoneMap['America/Halifax']='[UTC-04:00] Atlantic Standard Time';
timezoneMap['America/La_Paz']='[UTC-04:00] Bolivia Time';
timezoneMap['America/Santiago']='[UTC-04:00] Chile Time';
timezoneMap['America/St_Johns']='[UTC-03:30] Newfoundland Standard Time';
timezoneMap['America/Araguaina']='[UTC-03:00] Brasilia Time';
timezoneMap['America/Argentina/Buenos_Aires']='[UTC-03:00] Argentine Time';
timezoneMap['America/Cayenne']='[UTC-03:00] French Guiana Time';
timezoneMap['America/Godthab']='[UTC-03:00] Greenland Time';
timezoneMap['America/Montevideo']='[UTC-03:00] Uruguay Time]';
timezoneMap['Etc/GMT+2']='[UTC-02:00] GMT-02:00';
timezoneMap['Atlantic/Azores']='[UTC-01:00] Azores Time';
timezoneMap['Atlantic/Cape_Verde']='[UTC-01:00] Cape Verde Time';
timezoneMap['Africa/Casablanca']='[UTC] Casablanca';
timezoneMap['Etc/UTC']='[UTC] Coordinated Universal Time';
timezoneMap['Atlantic/Reykjavik']='[UTC] Reykjavik';
timezoneMap['Europe/London']='[UTC] Western European Time';
timezoneMap['CET']='[UTC+01:00] Central European Time';
timezoneMap['Europe/Bucharest']='[UTC+02:00] Eastern European Time';
timezoneMap['Africa/Johannesburg']='[UTC+02:00] South Africa Standard Time';
timezoneMap['Asia/Beirut']='[UTC+02:00] Beirut';
timezoneMap['Africa/Cairo']='[UTC+02:00] Cairo';
timezoneMap['Asia/Jerusalem']='[UTC+02:00] Israel Standard Time';
timezoneMap['Europe/Minsk']='[UTC+02:00] Minsk';
timezoneMap['Europe/Moscow']='[UTC+03:00] Moscow Standard Time';
timezoneMap['Africa/Nairobi']='[UTC+03:00] Eastern African Time';
timezoneMap['Asia/Karachi']='[UTC+05:00] Pakistan Time';
timezoneMap['Asia/Kolkata']='[UTC+05:30] India Standard Time';
timezoneMap['Asia/Bangkok']='[UTC+05:30] Indochina Time';
timezoneMap['Asia/Shanghai']='[UTC+08:00] China Standard Time';
timezoneMap['Asia/Kuala_Lumpur']='[UTC+08:00] Malaysia Time';
timezoneMap['Australia/Perth']='[UTC+08:00] Western Standard Time (Australia)';
timezoneMap['Asia/Taipei']='[UTC+08:00] Taiwan';
timezoneMap['Asia/Tokyo']='[UTC+09:00] Japan Standard Time';
timezoneMap['Asia/Seoul']='[UTC+09:00] Korea Standard Time';
timezoneMap['Australia/Adelaide']='[UTC+09:30] Central Standard Time (South Australia)';
timezoneMap['Australia/Darwin']='[UTC+09:30] Central Standard Time (Northern Territory)';
timezoneMap['Australia/Brisbane']='[UTC+10:00] Eastern Standard Time (Queensland)';
timezoneMap['Australia/Canberra']='[UTC+10:00] Eastern Standard Time (New South Wales)';
timezoneMap['Pacific/Guam']='[UTC+10:00] Chamorro Standard Time';
timezoneMap['Pacific/Auckland']='[UTC+12:00] New Zealand Standard Time';

// CloudStack common API helpers
cloudStack.api = {
  actions: {
    sort: function(updateCommand, objType) {
      var action = function(args) {
        $.ajax({
          url: createURL(updateCommand),
          data: {
            id: args.context[objType].id,
            sortKey: args.index
          },
          success: function(json) {
            args.response.success();
          },
          error: function(json) {
            args.response.error(parseXMLHttpResponse(json));
          }
        });

      };

      return {
        moveTop: {
          action: action
        },
        moveBottom: {
          action: action
        },
        moveUp: {
          action: action
        },
        moveDown: {
          action: action
        },
        moveDrag: {
          action: action
        }
      }
    }
  },

  tags: function(args) {
    var resourceType = args.resourceType;
    var contextId = args.contextId;
    
    return {
      actions: {
        add: function(args) {
          var data = args.data;
          var resourceId = args.context[contextId][0].id;

          $.ajax({
            url: createURL('createTags'),
            data: {
						  'tags[0].key': data.key,
							'tags[0].value': data.value,						
              resourceIds: resourceId,
              resourceType: resourceType
            },
            success: function(json) {
              args.response.success({
                _custom: { jobId: json.createtagsresponse.jobid },
                notification: {
                  desc: 'Add tag for ' + resourceType,
                  poll: pollAsyncJobResult
                }
              });
            }
          });
        },

        remove: function(args) {
          var data = args.context.tagItems[0];
          var resourceId = args.context[contextId][0].id;

          $.ajax({
            url: createURL('deleteTags'),
            data: {
						  'tags[0].key': data.key,
							'tags[0].value': data.value,						
              resourceIds: resourceId,
              resourceType: resourceType
            },
            success: function(json) {
              args.response.success({
                _custom: { jobId: json.deletetagsresponse.jobid },
                notification: {
                  desc: 'Remove tag for ' + resourceType,
                  poll: pollAsyncJobResult
                }
              });
            }
          });
        }
      },
      dataProvider: function(args) {
        var resourceId = args.context[contextId][0].id;
        var data = {
          resourceId: resourceId,
          resourceType: resourceType
        };

        if (isAdmin() || isDomainAdmin()) {
          data.listAll = true;
        }

        if (args.context.projects) {
          data.projectid=args.context.projects[0].id;
        }
        
				if(args.jsonObj != null && args.jsonObj.projectid != null && data.projectid == null) {
				  data.projectid = args.jsonObj.projectid;
				}
				
        $.ajax({
          url: createURL('listTags'),
          data: data,
          success: function(json) {
            args.response.success({
              data: json.listtagsresponse ?
                json.listtagsresponse.tag : []
            });
          },
          error: function(json) {
            args.response.error(parseXMLHttpResponse(json));
          }
        });
      }
    };
  }
};
