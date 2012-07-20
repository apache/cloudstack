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
  var zoneObjs, hypervisorObjs, featuredTemplateObjs, communityTemplateObjs, myTemplateObjs, featuredIsoObjs, community
  var selectedZoneObj, selectedTemplateObj, selectedHypervisor, selectedDiskOfferingObj; 
  var step5ContainerType = 'nothing-to-select'; //'nothing-to-select', 'select-network', 'select-security-group'

  cloudStack.instanceWizard = {
    maxDiskOfferingSize: function() {
      return g_capabilities.customdiskofferingmaxsize;
    },
    steps: [
      // Step 1: Setup
      function(args) {
      $.ajax({
        url: createURL("listZones&available=true"),
        dataType: "json",
        async: false,
        success: function(json) {
          zoneObjs = json.listzonesresponse.zone;
										
					var items;
					if(args.initArgs.pluginForm != null && args.initArgs.pluginForm.name == "vpcTierInstanceWizard") { //from VPC Tier chart
					  items = $.grep(zoneObjs, function(zoneObj) {						  
							return zoneObj.networktype == 'Advanced';
						});
					}
			    else { //from Instance page 
            items = zoneObjs;
          }		         			
					
          args.response.success({ data: {zones: items}});
        }
      });
    },

    // Step 2: Select template
    function(args) {
      $(zoneObjs).each(function(){
        if(this.id == args.currentData.zoneid) {
          selectedZoneObj = this;
          return false; //break the $.each() loop
        }
      });
      if(selectedZoneObj == null) {
        alert("error: can't find matched zone object");
        return;
      }

      $.ajax({
        url: createURL("listHypervisors&zoneid="+args.currentData.zoneid),
        dataType: "json",
        async: false,
        success: function(json) {
          hypervisorObjs = json.listhypervisorsresponse.hypervisor;
        }
      });

      //***** get templates/ISOs (begin) *****
      var selectedTemplate = args.currentData['select-template'];
      if (selectedTemplate == 'select-template') {
        var hypervisorArray = [];
        $(hypervisorObjs).each(function(index, item) {									 
          hypervisorArray.push(item.name);
        });

        $.ajax({
          url: createURL("listTemplates&templatefilter=featured&zoneid="+args.currentData.zoneid),
          dataType: "json",
          async: false,
          success: function(json) {										  
            featuredTemplateObjs = $.grep(json.listtemplatesresponse.template, function(item, index) {											  
              if($.inArray(item.hypervisor, hypervisorArray) > -1)
                return true;
            });	
          }
        });
        $.ajax({
          url: createURL("listTemplates&templatefilter=community&zoneid="+args.currentData.zoneid),
          dataType: "json",
          async: false,
          success: function(json) {
            communityTemplateObjs = $.grep(json.listtemplatesresponse.template, function(item, index) {											  
              if($.inArray(item.hypervisor, hypervisorArray) > -1)
                return true;
            });	
          }
        });
        $.ajax({
          url: createURL("listTemplates&templatefilter=selfexecutable&zoneid="+args.currentData.zoneid),
          dataType: "json",
          async: false,
          success: function(json) {
            myTemplateObjs = $.grep(json.listtemplatesresponse.template, function(item, index) {											  
              if($.inArray(item.hypervisor, hypervisorArray) > -1)
                return true;
            });	
          }
        });
      } else if (selectedTemplate == 'select-iso') {
        $.ajax({
          url: createURL("listIsos&isofilter=featured&zoneid=" + args.currentData.zoneid + "&bootable=true"),
          dataType: "json",
          async: false,
          success: function(json) {
            featuredIsoObjs = json.listisosresponse.iso;
          }
        });
        $.ajax({
          url: createURL("listIsos&isofilter=community&zoneid=" + args.currentData.zoneid + "&bootable=true"),
          dataType: "json",
          async: false,
          success: function(json) {
            communityIsoObjs = json.listisosresponse.iso;
          }
        });
        $.ajax({
          url: createURL("listIsos&isofilter=selfexecutable&zoneid=" + args.currentData.zoneid + "&bootable=true"),
          dataType: "json",
          async: false,
          success: function(json) {
            myIsoObjs = json.listisosresponse.iso;
          }
        });
      }
      //***** get templates/ISOs (end) *****


      var templatesObj = {};
      if (selectedTemplate == 'select-template') {
        templatesObj = {
          featuredtemplates: featuredTemplateObjs,
          communitytemplates: communityTemplateObjs,
          mytemplates: myTemplateObjs
        }
      } else if (selectedTemplate == 'select-iso') {
        templatesObj = {
          featuredisos: featuredIsoObjs,
          communityisos: communityIsoObjs,
          myisos: myIsoObjs
        }
      }
      args.response.success({
        hypervisor: {
          idField: 'name',
          nameField: 'name'
        },
        data: {
          templates: templatesObj,
          hypervisors: hypervisorObjs
        }
      });
    },

    // Step 3: Service offering
    function(args) {
      if(args.currentData["select-template"] == "select-template") {
        if(featuredTemplateObjs != null && featuredTemplateObjs.length > 0) {
          for(var i=0; i < featuredTemplateObjs.length; i++) {
            if(featuredTemplateObjs[i].id == args.currentData.templateid) {
              selectedTemplateObj = featuredTemplateObjs[i];
              break;
            }
          }
        }
        if(selectedTemplateObj == null) {
          if(communityTemplateObjs != null && communityTemplateObjs.length > 0) {
            for(var i=0; i < communityTemplateObjs.length; i++) {
              if(communityTemplateObjs[i].id == args.currentData.templateid) {
                selectedTemplateObj = communityTemplateObjs[i];
                break;
              }
            }
          }
        }
        if(selectedTemplateObj == null) {
          if(myTemplateObjs != null && myTemplateObjs.length > 0) {
            for(var i=0; i < myTemplateObjs.length; i++) {
              if(myTemplateObjs[i].id == args.currentData.templateid) {
                selectedTemplateObj = myTemplateObjs[i];
                break;
              }
            }
          }
        }
        if(selectedTemplateObj == null)
          alert("unable to find matched template object");
        else
          selectedHypervisor = selectedTemplateObj.hypervisor;
      }
      else { //(args.currentData["select-template"] == "select-iso"
        selectedHypervisor = args.currentData.hypervisorid;
      }

      $.ajax({
        url: createURL("listServiceOfferings&issystem=false"),
        dataType: "json",
        async: true,
        success: function(json) {
          serviceOfferingObjs = json.listserviceofferingsresponse.serviceoffering;
          args.response.success({
            data: {serviceOfferings: serviceOfferingObjs}
          });
        }
      });
    },

    // Step 4: Data disk offering
    function(args) {
      var isRequred = (args.currentData["select-template"] == "select-iso"? true: false);
      $.ajax({
        url: createURL("listDiskOfferings"),
        dataType: "json",
        async: true,
        success: function(json) {
          diskOfferingObjs = json.listdiskofferingsresponse.diskoffering;
          args.response.success({
            required: isRequred,
            customFlag: 'iscustomized', // Field determines if custom slider is shown
            data: {diskOfferings: diskOfferingObjs}
          });
        }
      });
    },

    // Step 5: Network
    function(args) {
      if(diskOfferingObjs != null && diskOfferingObjs.length > 0) {
        for(var i=0; i < diskOfferingObjs.length; i++) {
          if(diskOfferingObjs[i].id == args.currentData.diskofferingid) {
            selectedDiskOfferingObj = diskOfferingObjs[i];
            break;
          }
        }
      }

      if (selectedZoneObj.networktype == "Advanced") {  //Advanced zone. Show network list.	 
				var $networkStep = $(".step.network:visible .nothing-to-select");		
				if(args.initArgs.pluginForm != null && args.initArgs.pluginForm.name == "vpcTierInstanceWizard") { //from VPC Tier chart
				  step5ContainerType = 'nothing-to-select'; 					
					$networkStep.find("#from_instance_page_1").hide();		
          $networkStep.find("#from_instance_page_2").hide();					
					$networkStep.find("#from_vpc_tier").text("tier " + args.context.networks[0].name);					
					$networkStep.find("#from_vpc_tier").show();					
				}
			  else { //from Instance page 
          step5ContainerType = 'select-network';
					$networkStep.find("#from_instance_page_1").show();		
          $networkStep.find("#from_instance_page_2").show();
					$networkStep.find("#from_vpc_tier").text("");			
					$networkStep.find("#from_vpc_tier").hide();
				}
      }
      else { //Basic zone. Show securigy group list or nothing(when no SecurityGroup service in guest network)
        var includingSecurityGroupService = false;
        $.ajax({
          url: createURL("listNetworks&trafficType=Guest&zoneId=" + selectedZoneObj.id),
          dataType: "json",
          async: false,
          success: function(json) {
            //basic zone should have only one guest network returned in this API call
            var items = json.listnetworksresponse.network;
            if(items != null && items.length > 0) {
              var networkObj = items[0];    //basic zone has only one guest network
              var serviceObjArray = networkObj.service;
              for(var k = 0; k < serviceObjArray.length; k++) {
                if(serviceObjArray[k].name == "SecurityGroup") {
                  includingSecurityGroupService = true;
                  break;
                }
              }
            }
          }
        });

        if(includingSecurityGroupService == false || selectedHypervisor == "VMware") {
          step5ContainerType = 'nothing-to-select';
        }
        else {
          step5ContainerType = 'select-security-group';
        }
      }

      //step5ContainerType = 'nothing-to-select'; //for testing only, comment it out before checking in
      if(step5ContainerType == 'select-network') {
        var defaultNetworkArray = [], optionalNetworkArray = [];
        var networkData = {
          zoneId: args.currentData.zoneid
        };

        if (!(cloudStack.context.projects && cloudStack.context.projects[0])) {
          networkData.domainid = g_domainid;
          networkData.account = g_account;
        }

        var networkObjs;
        $.ajax({
          url: createURL('listNetworks'),
          data: networkData,
          dataType: "json",
          async: false,
          success: function(json) {
            networkObjs = json.listnetworksresponse.network ? json.listnetworksresponse.network : [];
          }
        });
                  
        $.ajax({
          url: createURL("listNetworkOfferings"), 
          dataType: "json",
					data: {
						forvpc: false, 
						zoneid: args.currentData.zoneid,
						guestiptype: 'Isolated',
						supportedServices: 'SourceNat',
						specifyvlan: false,
						state: 'Enabled'
					},
          async: false,
          success: function(json) {
            networkOfferingObjs  = json.listnetworkofferingsresponse.networkoffering;
          }
        });
        //get network offerings (end)	***			


        args.response.success({
          type: 'select-network',
          data: {
            myNetworks: [], //not used any more
            sharedNetworks: networkObjs,
            securityGroups: [],
            networkOfferings: networkOfferingObjs
          }
        });
      }

      else if(step5ContainerType == 'select-security-group') {
        var securityGroupArray = [];
        var data = {
          domainid: g_domainid,
          account: g_account
        };

        $.ajax({
          url: createURL("listSecurityGroups"),
          dataType: "json",
          async: false,
          data: cloudStack.context.projects ? {} : data,
          success: function(json) {
            var items = json.listsecuritygroupsresponse.securitygroup;
            if (items != null && items.length > 0) {
              for (var i = 0; i < items.length; i++) {
                if(items[i].name != "default") //exclude default security group because it is always applied
                  securityGroupArray.push(items[i]);
              }
            }
          }
        });
        args.response.success({
          type: 'select-security-group',
          data: {
            myNetworks: [], //not used any more
            sharedNetworks: [],
            securityGroups: securityGroupArray,
            networkOfferings: []
          }
        });
      }

      else if(step5ContainerType == 'nothing-to-select') {
        args.response.success({
          type: 'nothing-to-select',
          data: {
            myNetworks: [], //not used any more
            sharedNetworks: [],
            securityGroups: [],
            networkOfferings: []
          }
        });
      }

    },

    // Step 6: Review
    function(args) {
      return false;
    }
    ],
    action: function(args) {
      // Create a new VM!!!!
      var array1 = [];

      //step 1 : select zone
      array1.push("&zoneId=" + args.data.zoneid);

      //step 2: select template
      array1.push("&templateId=" + args.data.templateid);
      array1.push("&hypervisor=" + selectedHypervisor);

      //step 3: select service offering
      array1.push("&serviceOfferingId=" + args.data.serviceofferingid);

      //step 4: select disk offering
      if(args.data.diskofferingid != null && args.data.diskofferingid != "0") {
        array1.push("&diskOfferingId=" + args.data.diskofferingid);
        if(selectedDiskOfferingObj.iscustomized == true)
          array1.push("&size=" + args.data.size);
      }

      //step 5: select network
      if (step5ContainerType == 'select-network') {
        var array2 = [];
        var defaultNetworkId = args.data.defaultNetwork; //args.data.defaultNetwork might be equal to string "new-network" or a network ID

        var checkedNetworkIdArray;
        if(typeof(args.data["my-networks"]) == "object" && args.data["my-networks"].length != null) { //args.data["my-networks"] is an array of string, e.g. ["203", "202"],
          checkedNetworkIdArray = args.data["my-networks"];
        }
        else if(typeof(args.data["my-networks"]) == "string" && args.data["my-networks"].length > 0) { //args.data["my-networks"] is a string, e.g. "202"
          checkedNetworkIdArray = [];
          checkedNetworkIdArray.push(args.data["my-networks"]);
        }
        else { // typeof(args.data["my-networks"]) == null
          checkedNetworkIdArray = [];
        }

        //create new network starts here
        if(args.data["new-network"] == "create-new-network") {
          var isCreateNetworkSuccessful = true;
          $.ajax({
            url: createURL("createNetwork&networkOfferingId="+args.data["new-network-networkofferingid"]+"&name="+todb(args.data["new-network-name"])+"&displayText="+todb(args.data["new-network-name"])+"&zoneId="+selectedZoneObj.id),
            dataType: "json",
            async: false,
            success: function(json) {
              newNetwork = json.createnetworkresponse.network;
              checkedNetworkIdArray.push(newNetwork.id);
              if(defaultNetworkId == "new-network")
                defaultNetworkId = newNetwork.id;
            },
            error: function(XMLHttpResponse) {
              isCreateNetworkSuccessful = false;
              var errorMsg = "Failed to create new network, unable to proceed to deploy VM. Error: " + parseXMLHttpResponse(XMLHttpResponse);
              //alert(errorMsg);
              args.response.error(errorMsg);    //args.response.error(errorMsg) here doesn't show errorMsg. Waiting for Brian to fix it. use alert(errorMsg) to show errorMsg for now.
            }
          });
          if(isCreateNetworkSuccessful == false)
            return;
        }
        //create new network ends here

        //add default network first
        if(defaultNetworkId != null && defaultNetworkId.length > 0)
          array2.push(defaultNetworkId);

        //then, add other checked networks
        if(checkedNetworkIdArray.length > 0) {
          for(var i=0; i < checkedNetworkIdArray.length; i++) {
            if(checkedNetworkIdArray[i] != defaultNetworkId) //exclude defaultNetworkId that has been added to array2
              array2.push(checkedNetworkIdArray[i]);
          }
        }

        array1.push("&networkIds=" + array2.join(","));
      }
      else if (step5ContainerType == 'select-security-group') {
        var checkedSecurityGroupIdArray;
        if(typeof(args.data["security-groups"]) == "object" && args.data["security-groups"].length != null) { //args.data["security-groups"] is an array of string, e.g. ["2375f8cc-8a73-4b8d-9b26-50885a25ffe0", "27c60d2a-de7f-4bb7-96e5-a602cec681df","c6301d77-99b5-4e8a-85e2-3ea2ab31c342"],
          checkedSecurityGroupIdArray = args.data["security-groups"];
        }
        else if(typeof(args.data["security-groups"]) == "string" && args.data["security-groups"].length > 0) { //args.data["security-groups"] is a string, e.g. "2375f8cc-8a73-4b8d-9b26-50885a25ffe0"
          checkedSecurityGroupIdArray = [];
          checkedSecurityGroupIdArray.push(args.data["security-groups"]);
        }
        else { // typeof(args.data["security-groups"]) == null
          checkedSecurityGroupIdArray = [];
        }

        if(checkedSecurityGroupIdArray.length > 0)
          array1.push("&securitygroupids=" + checkedSecurityGroupIdArray.join(","));
      }
      else if (step5ContainerType == 'nothing-to-select') {		
				if(args.context.networks != null) //from VPC tier
				  array1.push("&networkIds=" + args.context.networks[0].id);
			}
			
      var displayname = args.data.displayname;
      if(displayname != null && displayname.length > 0) {
        array1.push("&displayname="+todb(displayname));
        array1.push("&name="+todb(displayname));
      }

      var group = args.data.groupname;
      if (group != null && group.length > 0)
        array1.push("&group="+todb(group));

      //array1.push("&startVm=false");	//for testing only, comment it out before checking in

      $.ajax({
        url: createURL("deployVirtualMachine"+array1.join("")),
        dataType: "json",
        success: function(json) {
          var jid = json.deployvirtualmachineresponse.jobid;
          var vmid = json.deployvirtualmachineresponse.id;
          args.response.success(
            {_custom:
              {jobId: jid,
                getUpdatedItem: function(json) {
                  var item = json.queryasyncjobresultresponse.jobresult.virtualmachine;
                  if (item.password != null)
                    alert("Password of new VM " + item.displayname + " is  " + item.password);
                  return item;
                },
                getActionFilter: function() {
                  return cloudStack.actionFilter.vmActionFilter;
                },
                getUpdatedItemWhenAsyncJobFails: function() {
                  var item;
                  $.ajax({
                    url: createURL("listVirtualMachines&id="+vmid),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      item = json.listvirtualmachinesresponse.virtualmachine[0];
                    }
                  });
                  return item;
                }
          }
          }
          );
        },
        error: function(XMLHttpResponse) {
          args.response.error(parseXMLHttpResponse(XMLHttpResponse)); //wait for Brian to implement
        }
      });
    }
  };
}(jQuery, cloudStack));
