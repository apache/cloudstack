 /**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
 

function hostGetSearchParams() {
    var moreCriteria = [];	
   
	var searchInput = $("#basic_search").find("#search_input").val();	 
    if (searchInput != null && searchInput.length > 0) {	           
        moreCriteria.push("&keyword="+todb(searchInput));	       
    }     

	var $advancedSearchPopup = getAdvancedSearchPopupInSearchContainer();
	if ($advancedSearchPopup.length > 0 && $advancedSearchPopup.css("display") != "none" ) {		
 
        var state = $advancedSearchPopup.find("#adv_search_state").val();				
	    if (state!=null && state.length > 0) 
		    moreCriteria.push("&state="+todb(state));	
        
        var zone = $advancedSearchPopup.find("#adv_search_zone").val();	
	    if (zone!=null && zone.length > 0) 
			moreCriteria.push("&zoneId="+zone);	
		
		if ($advancedSearchPopup.find("#adv_search_pod_li").css("display") != "none") {	
		    var pod = $advancedSearchPopup.find("#adv_search_pod").val();		
	        if (pod!=null && pod.length > 0) 
			    moreCriteria.push("&podId="+pod);
        }
	} 
			
	return moreCriteria.join("");          
}

function afterLoadHostJSP() {    
    initDialog("dialog_add_host", 400);    
    initDialog("dialog_update_os");
    initDialog("dialog_confirmation_remove_host"); 
	
	var $dialogRemoveHost = $("#dialog_confirmation_remove_host");
	if(isAdmin()) {		
		$dialogRemoveHost.find("#force_remove_host_container").show();		   
	}  
    
    // switch between different tabs 
    var tabArray = [$("#tab_details"), $("#tab_instance"), $("#tab_router"), $("#tab_systemvm"), $("#tab_statistics")];
    var tabContentArray = [$("#tab_content_details"), $("#tab_content_instance"), $("#tab_content_router"), $("#tab_content_systemvm"), $("#tab_content_statistics")];
    var afterSwitchFnArray = [hostJsonToDetailsTab, hostJsonToInstanceTab, hostJsonToRouterTab, hostJsonToSystemvmTab, hostJsonToStatisticsTab];
    switchBetweenDifferentTabs(tabArray, tabContentArray, afterSwitchFnArray);  
  
    $readonlyFields  = $("#tab_content_details").find("#hosttags,#oscategoryname");
    $editFields = $("#tab_content_details").find("#hosttags_edit,#os_dropdown"); 
    
    $.ajax({
	    data: createURL("command=listOsCategories"),
		dataType: "json",
		success: function(json) {
			var categories = json.listoscategoriesresponse.oscategory;
			var $dropdown = $("#tab_content_details").find("#os_dropdown").empty();	
			$dropdown.append("<option value=''>None</option>"); 						
			if (categories != null && categories.length > 0) {
				for (var i = 0; i < categories.length; i++) {				   
					 $dropdown.append("<option value='" + categories[i].id + "'>" + fromdb(categories[i].name) + "</option>"); 						
		        }			    
			}
		}
	});
    
    hostRefreshDataBinding();  
}

function hostRefreshDataBinding() {
    var $hostNode = $selectedSubMenu.parent(); 
    bindAddHostButton($hostNode);      
}

function hostToMidmenu(jsonObj, $midmenuItem1) {    
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj);      
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show(); 
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_host.png");   
          
	var firstRowText = fromdb(jsonObj.name);
    $midmenuItem1.find("#first_row").text(clippedText(firstRowText, midMenuFirstRowLength));     
    $midmenuItem1.find("#first_row_container").attr("title", firstRowText);   
    
    var secondRowText = fromdb(jsonObj.ipaddress);
    $midmenuItem1.find("#second_row").text(clippedText(secondRowText, midMenuSecondRowLength));
    $midmenuItem1.find("#second_row_container").attr("title", secondRowText); 
    
    updateHostStateInMidMenu(jsonObj, $midmenuItem1);   
}

function hostToRightPanel($midmenuItem1) {       
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);  
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);        
    $("#tab_details").click();     
}

function hostJsonToDetailsTab() {  
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null) {
        hostClearDetailsTab();     
        return;
    }
    
    var jsonObj = $midmenuItem1.data("jsonObj");
    if(jsonObj == null) {
        hostClearDetailsTab();     
        return;    
    }
       
    $.ajax({
        data: createURL("command=listHosts&id="+jsonObj.id),
        dataType: "json",
        async: false,
        success: function(json) {            
            var items = json.listhostsresponse.host;
			if(items != null && items.length > 0) {
                jsonObj = items[0];
                $midmenuItem1.data("jsonObj", jsonObj);   
                updateHostStateInMidMenu(jsonObj, $midmenuItem1);                  
            }
        }
    });     
       
    var $thisTab = $("#right_panel_content #tab_content_details");  
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();        
                
    $thisTab.find("#id").text(fromdb(jsonObj.id));
    $thisTab.find("#grid_header_title").text(fromdb(jsonObj.name));
    $thisTab.find("#name").text(fromdb(jsonObj.name));
    $thisTab.find("#hosttags").text(fromdb(jsonObj.hosttags));    
    $thisTab.find("#hosttags_edit").val(fromdb(jsonObj.hosttags));    
    
    setHostStateInRightPanel(fromdb(jsonObj.state), $thisTab.find("#state"));
    
    
    //refresh status every 2 seconds until status is not changable any more 
	var timerKey = "refreshHostStatus";
	$("body").stopTime(timerKey);  //stop timer used by another middle menu item (i.e. stop timer when clicking on a different middle menu item)		
	if($midmenuItem1.find("#spinning_wheel").css("display") == "none") {
	    if(jsonObj.state in hostChangableStatus) {	    
	        $("body").everyTime(
                5000,
                timerKey,
                function() {              
                    $.ajax({
		                data: createURL("command=listHosts&id="+jsonObj.id),
		                dataType: "json",
		                async: false,
		                success: function(json) {  
			                var items = json.listhostsresponse.host;
			                if(items != null && items.length > 0) {
				                jsonObj = items[0]; //override jsonObj declared above				
				                $midmenuItem1.data("jsonObj", jsonObj); 				                            
				                if(!(jsonObj.state in hostChangableStatus)) {
				                    $("body").stopTime(timerKey);					                    
				                    updateHostStateInMidMenu(jsonObj, $midmenuItem1); 			                    
				                    if(jsonObj.id.toString() == $("#right_panel_content").find("#tab_content_details").find("#id").text()) {
				                        setHostStateInRightPanel(jsonObj.state, $thisTab.find("#state"));	
				                        hostBuildActionMenu(jsonObj, $thisTab, $midmenuItem1);	
				                    }					                    
				                }               
	                        }   
		                }
	                });                       	
                }
            );
	    }
	}
        
    
    $thisTab.find("#type").text(fromdb(jsonObj.type));      
    $thisTab.find("#zonename").text(fromdb(jsonObj.zonename)); 
    $thisTab.find("#podname").text(fromdb(jsonObj.podname));   
    $thisTab.find("#clustername").text(fromdb(jsonObj.clustername));        
    $thisTab.find("#ipaddress").text(fromdb(jsonObj.ipaddress)); 
    $thisTab.find("#version").text(fromdb(jsonObj.version));  
    $thisTab.find("#oscategoryname").text(fromdb(jsonObj.oscategoryname));        
    $thisTab.find("#disconnected").text(fromdb(jsonObj.disconnected));  
    
    populateForUpdateOSDialog(jsonObj.oscategoryid);
    
    // actions 
    hostBuildActionMenu(jsonObj, $thisTab, $midmenuItem1);
	
	$thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();               
}

var hostChangableStatus = {
    "PrepareForMaintenance": 1,
    "ErrorInMaintenance": 1,
    "Updating": 1,
    "Disconnected": 1,
    "Alert": 1,
    "Connecting": 1   
}

function hostBuildActionMenu(jsonObj, $thisTab, $midmenuItem1) {  
    var $actionLink = $thisTab.find("#action_link"); 
    bindActionLink($actionLink);
      
    var $actionMenu = $actionLink.find("#action_menu");
    $actionMenu.find("#action_list").empty();
    var noAvailableActions = true;
    
    if (jsonObj.state == 'Up' || jsonObj.state == "Connecting") {
    	buildActionLinkForTab("label.action.edit.host", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);   
		buildActionLinkForTab("label.action.enable.maintenance.mode", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);  
	    buildActionLinkForTab("label.action.force.reconnect", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);   	    
	    //buildActionLinkForTab("label.action.update.OS.preference", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);   //temp
	    noAvailableActions = false;
	} 
	else if(jsonObj.state == 'Down') {
		buildActionLinkForTab("label.action.edit.host", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);  
	    buildActionLinkForTab("label.action.enable.maintenance.mode", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);  	    	    
        buildActionLinkForTab("label.action.remove.host", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);  
        noAvailableActions = false;
    }	
	else if(jsonObj.state == "Alert") {
	    buildActionLinkForTab("label.action.edit.host", hostActionMap, $actionMenu, $midmenuItem1, $thisTab); 
	    buildActionLinkForTab("label.action.remove.host", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);   	
	    noAvailableActions = false;   
     
	}	
	else if (jsonObj.state == "ErrorInMaintenance") {
		buildActionLinkForTab("label.action.edit.host", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);  	 
	    buildActionLinkForTab("label.action.enable.maintenance.mode", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);  
        buildActionLinkForTab("label.action.cancel.maintenance.mode", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);  
        
        noAvailableActions = false;   
    }
	else if (jsonObj.state == "PrepareForMaintenance") {
		buildActionLinkForTab("label.action.edit.host", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);  	
	    buildActionLinkForTab("label.action.cancel.maintenance.mode", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);          
        noAvailableActions = false;    
    }
	else if (jsonObj.state == "Maintenance") {
		 buildActionLinkForTab("label.action.edit.host", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);  	
	    buildActionLinkForTab("label.action.cancel.maintenance.mode", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);             
        buildActionLinkForTab("label.action.remove.host", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);  
        noAvailableActions = false;
    }
	else if (jsonObj.state == "Disconnected"){
	    buildActionLinkForTab("label.action.edit.host", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);  	    
        buildActionLinkForTab("label.action.remove.host", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);  
        noAvailableActions = false;
    }
	else {
	    //alert("Unsupported Host State: " + jsonObj.state);
	} 
        
    // no available actions 
	if(noAvailableActions == true) {
	    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
	}	
}

function hostJsonToInstanceTab() {       	
	var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null) {
        hostClearInstanceTab();
        return;
    }
    
    var jsonObj = $midmenuItem1.data("jsonObj");
    if(jsonObj == null) {
        hostClearInstanceTab();
        return;
    }
    
    var $thisTab = $("#right_panel_content #tab_content_instance");
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show(); 
    	    
    $.ajax({
		cache: false,
		data: createURL("command=listVirtualMachines&hostid="+jsonObj.id),
		dataType: "json",
		success: function(json) {							    
			var items = json.listvirtualmachinesresponse.virtualmachine;	
			var $container = $thisTab.find("#tab_container").empty();																					
			if (items != null && items.length > 0) {			    
				var template = $("#instance_tab_template");				
				for (var i = 0; i < items.length; i++) {
					var newTemplate = template.clone(true);	               
	                hostInstanceJSONToTemplate(items[i], newTemplate); 
	                $container.append(newTemplate.show());	
				}			
			}	
			$thisTab.find("#tab_spinning_wheel").hide();    
            $thisTab.find("#tab_container").show();    			
		}
	});
} 

function hostClearInstanceTab() {
    var $thisTab = $("#right_panel_content").find("#tab_content_instance");
    $thisTab.find("#tab_container").empty();   
}

function hostInstanceJSONToTemplate(jsonObj, template) {
    template.data("jsonObj", jsonObj);     
    template.attr("id", "host_instance_"+jsonObj.id).data("hostInstanceId", jsonObj.id);    
    template.find("#grid_header_title").text(fromdb(jsonObj.name));			   
    template.find("#id").text(jsonObj.id);
    template.find("#name").text(getVmName(jsonObj.name, jsonObj.displayname));	  
    template.find("#ipaddress").text(fromdb(jsonObj.ipaddress));
    template.find("#serviceOfferingName").text(fromdb(jsonObj.serviceofferingname));	
    template.find("#account").text(fromdb(jsonObj.account));
    template.find("#domain").text(fromdb(jsonObj.domain));
    setDateField(jsonObj.created, template.find("#created"));	 		
} 

function hostJsonToRouterTab() {   
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null) {
        hostClearRouterTab();
        return;
    }
    
    var jsonObj = $midmenuItem1.data("jsonObj");
    if(jsonObj == null) {
        hostClearRouterTab();
        return;
    }
    
    var $thisTab = $("#right_panel_content #tab_content_router");
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show(); 
        
    $.ajax({
		cache: false,
		data: createURL("command=listRouters&hostid="+jsonObj.id),
		dataType: "json",
		success: function(json) {							    
			var items = json.listroutersresponse.router;	
			var $container = $thisTab.find("#tab_container").empty();																					
			if (items != null && items.length > 0) {			    
				var template = $("#router_tab_template");				
				for (var i = 0; i < items.length; i++) {
					var newTemplate = template.clone(true);	               
	                hostRouterJSONToTemplate(items[i], newTemplate); 
	                $container.append(newTemplate.show());	
				}			
			}	
			$thisTab.find("#tab_spinning_wheel").hide();    
            $thisTab.find("#tab_container").show();    			
		}
	});
} 

function hostClearRouterTab() {
    var $thisTab = $("#right_panel_content").find("#tab_content_router");
    $thisTab.find("#tab_container").empty(); 
}

function hostJsonToStatisticsTab() {    
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null) {
        hostClearStatisticsTab();
        return;
    }
    
    var jsonObj = $midmenuItem1.data("jsonObj");
    if(jsonObj == null) {
        hostClearStatisticsTab();
        return;
    }
    
    var $thisTab = $("#right_panel_content").find("#tab_content_statistics");  
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   
    
    /*
    var $barChartContainer = $thisTab.find("#cpu_barchart");
         
    var cpuNumber = ((jsonObj.cpunumber==null)? "":jsonObj.cpunumber.toString());
    $barChartContainer.find("#cpunumber").text(cpuNumber);
    
    var cpuSpeed = ((jsonObj.cpuspeed==null)? "":convertHz(jsonObj.cpuspeed)) ;
    $barChartContainer.find("#cpuspeed").text(cpuSpeed);
    
    $barChartContainer.find("#bar_chart").removeClass().addClass("db_barbox").css("width", "0%");    
    $barChartContainer.find("#percentused").text("");   
    if(jsonObj.cpuused!=null)
        drawBarChart($barChartContainer, jsonObj.cpuused);		
    */
    
    $thisTab.find("#cpunumber").text(fromdb(jsonObj.cpunumber));
    $thisTab.find("#cpuspeed").text(convertHz(jsonObj.cpuspeed));
    
    $thisTab.find("#percentused").text(jsonObj.cpuused); 
    
    var cpuAllocated = ((jsonObj.cpuallocated==null)? "":jsonObj.cpuallocated);
    $thisTab.find("#cpuallocated").text(cpuAllocated);    
    
    var memoryTotal = ((jsonObj.cpuallocated==null)? "":convertBytes(jsonObj.memorytotal));
    $thisTab.find("#memorytotal").text(memoryTotal);
    
    var memoryAllocated = ((jsonObj.cpuallocated==null)? "":convertBytes(jsonObj.memoryallocated));
    $thisTab.find("#memoryallocated").text(memoryAllocated);
    
    var memoryUsed = ((jsonObj.memoryused==null)? "":convertBytes(jsonObj.memoryused));
    $thisTab.find("#memoryused").text(memoryUsed);
        
    var networkKbsRead = ((jsonObj.networkkbsread==null)? "":convertBytes(jsonObj.networkkbsread * 1024));
    $thisTab.find("#networkkbsread").text(networkKbsRead);
    
    var networkKbsWrite = ((jsonObj.networkkbswrite==null)? "":convertBytes(jsonObj.networkkbswrite * 1024));
    $thisTab.find("#networkkbswrite").text(networkKbsWrite);
    
    $thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();  
}

function hostClearStatisticsTab() {        
    var $thisTab = $("#right_panel_content").find("#tab_content_statistics");  	
    var $barChartContainer = $thisTab.find("#cpu_barchart");            
    $barChartContainer.find("#cpunumber").text("");        
    $barChartContainer.find("#cpuspeed").text("");    
    $barChartContainer.find("#bar_chart").removeClass().addClass("db_barbox").css("width", "0%");    
    $barChartContainer.find("#percentused").text("");               
    $thisTab.find("#cpuallocated").text("");            
    $thisTab.find("#memorytotal").text("");        
    $thisTab.find("#memoryallocated").text("");      
    $thisTab.find("#memoryused").text("");           
    $thisTab.find("#networkkbsread").text("");        
    $thisTab.find("#networkkbswrite").text("");       
}

function hostRouterJSONToTemplate(jsonObj, template) {
    template.data("jsonObj", jsonObj);     
    template.attr("id", "host_router_"+jsonObj.id).data("hostRouterId", jsonObj.id);    
    template.find("#grid_header_title").text(fromdb(jsonObj.name));			   
    template.find("#id").text(jsonObj.id);
    template.find("#name").text(fromdb(jsonObj.name));	  
    template.find("#publicip").text(fromdb(jsonObj.publicip));   
    template.find("#privateip").text(fromdb(jsonObj.linklocalip));
    template.find("#guestipaddress").text(fromdb(jsonObj.guestipaddress)); 
    template.find("#account").text(fromdb(jsonObj.account));
    template.find("#domain").text(fromdb(jsonObj.domain));
    setDateField(jsonObj.created, template.find("#created"));	 		
}  

function hostJsonToSystemvmTab() {   
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null) {
        hostClearSystemvmTab();
        return;
    }
    
    var jsonObj = $midmenuItem1.data("jsonObj");
    if(jsonObj == null) {
        hostClearSystemvmTab();
        return;
    }
    
    var $thisTab = $("#right_panel_content #tab_content_systemvm");
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show(); 
        
    $.ajax({
		cache: false,
		data: createURL("command=listSystemVms&hostid="+jsonObj.id),
		dataType: "json",
		success: function(json) {							    
			var items = json.listsystemvmsresponse.systemvm;	
			var $container = $thisTab.find("#tab_container").empty();																					
			if (items != null && items.length > 0) {			    
				var template = $("#systemvm_tab_template");				
				for (var i = 0; i < items.length; i++) {
					var newTemplate = template.clone(true);	               
	                hostSystemvmJSONToTemplate(items[i], newTemplate); 
	                $container.append(newTemplate.show());	
				}			
			}	
			$thisTab.find("#tab_spinning_wheel").hide();    
            $thisTab.find("#tab_container").show();    			
		}
	});
} 

function hostClearSystemvmTab() {
    var $thisTab = $("#right_panel_content").find("#tab_content_systemvm");
    $thisTab.find("#tab_container").empty(); 
}

function hostSystemvmJSONToTemplate(jsonObj, template) {
    template.data("jsonObj", jsonObj);     
    template.attr("id", "host_systemvm_"+jsonObj.id).data("hostSystemvmId", jsonObj.id);    
    template.find("#grid_header_title").text(fromdb(jsonObj.name));			   
    template.find("#id").text(jsonObj.id);
    template.find("#name").text(fromdb(jsonObj.name));	      
    template.find("#systemvmtype").text(toSystemVMTypeText(jsonObj.systemvmtype));  
    template.find("#publicip").text(fromdb(jsonObj.publicip));    
    template.find("#privateip").text(fromdb(jsonObj.privateip));
	template.find("#linklocalip").text(fromdb(jsonObj.linklocalip));  
    setDateField(jsonObj.created, template.find("#created"));	 		
}  

function hostClearRightPanel() {
    hostClearDetailsTab();     
    hostClearInstanceTab();
    hostClearRouterTab();
    hostClearSystemvmTab();
    hostClearStatisticsTab();
}

function hostClearDetailsTab() {
    var $thisTab = $("#right_panel_content").find("#tab_content_details");  
    $thisTab.find("#grid_header_title").text("");
    $thisTab.find("#id").text("");
    $thisTab.find("#name").text("");
    $thisTab.find("#state").text("");  
    $thisTab.find("#type").text("");        
    $thisTab.find("#zonename").text(""); 
    $thisTab.find("#podname").text(""); 
    $thisTab.find("#clustername").text(""); 
    $thisTab.find("#ipaddress").text(""); 
    $thisTab.find("#version").text(""); 
    $thisTab.find("#oscategoryname").text("");       
    $thisTab.find("#disconnected").text(""); 
    
    var $actionMenu = $thisTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();   
	$actionMenu.find("#action_list").append($("#no_available_actions").clone().show());		    
}

function populateForUpdateOSDialog(oscategoryid) {	
	$.ajax({
	    data: createURL("command=listOsCategories"),
		dataType: "json",
		success: function(json) {
			var categories = json.listoscategoriesresponse.oscategory;
			var select = $("#dialog_update_os #host_os").empty();	
			select.append("<option value=''>None</option>"); 						
			if (categories != null && categories.length > 0) {
				for (var i = 0; i < categories.length; i++) {
				    if(categories[i].id == oscategoryid) {				       
				        select.append("<option value='" + categories[i].id + "' selected >" + categories[i].name + "</option>"); 	
				    }    
				    else {
					    select.append("<option value='" + categories[i].id + "'>" + categories[i].name + "</option>"); 	
					}
		        }			    
			}
		}
	});
}


var hostActionMap = {  
    "label.action.edit.host": {
        dialogBeforeActionFn: doEditHost  
    },
    "label.action.enable.maintenance.mode": {              
        isAsyncJob: true,
        asyncJobResponse: "preparehostformaintenanceresponse",
        dialogBeforeActionFn: doEnableMaintenanceMode,
        inProcessText: "label.action.enable.maintenance.mode.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {            
            var item = json.queryasyncjobresultresponse.jobresult.host;
            hostToMidmenu(item, $midmenuItem1);    
			return dictionary["message.action.enable.maintenance"];
        }
    },
    "label.action.cancel.maintenance.mode": {              
        isAsyncJob: true,
        asyncJobResponse: "cancelhostmaintenanceresponse",
        dialogBeforeActionFn: doCancelMaintenanceMode,
        inProcessText: "label.action.cancel.maintenance.mode.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {            
            var item = json.queryasyncjobresultresponse.jobresult.host;  
            hostToMidmenu(item, $midmenuItem1);
			return dictionary["message.action.cancel.maintenance"];
        }
    },
    "label.action.force.reconnect": {              
        isAsyncJob: true,
        asyncJobResponse: "reconnecthostresponse",
        dialogBeforeActionFn: doForceReconnect,
        inProcessText: "label.action.force.reconnect.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {
            var item = json.queryasyncjobresultresponse.jobresult.host;
            hostToMidmenu(item, $midmenuItem1);  
			return dictionary["message.action.force.reconnect"];
        }
    },
    "label.action.remove.host": {              
        isAsyncJob: false,        
        dialogBeforeActionFn: doRemoveHost,
        inProcessText: "label.action.remove.host.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {    
            $midmenuItem1.remove();               
            if(id.toString() == $("#right_panel_content").find("#tab_content_details").find("#id").text()) {
                clearRightPanel();
                hostClearRightPanel();
            }                         
        }
    }
    /*
    ,    
    "label.action.update.OS.preference": {              
        isAsyncJob: false,        
        dialogBeforeActionFn: doUpdateOSPreference,
        inProcessText: "label.action.update.OS.preference.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {     
            var item = json.updatehostresponse.host;
            hostToMidmenu(item, $midmenuItem1);              
        }
    } 
    */         
} 

function doEditHost($actionLink, $detailsTab, $midmenuItem1) {  
    var jsonObj = $midmenuItem1.data("jsonObj"); 
    $detailsTab.find("#os_dropdown").val(jsonObj.oscategoryid);
    
    $readonlyFields.hide();
    $editFields.show();  
    $detailsTab.find("#cancel_button, #save_button").show();
    
    $detailsTab.find("#cancel_button").unbind("click").bind("click", function(event){    
        cancelEditMode($detailsTab);    
        return false;
    });
    $detailsTab.find("#save_button").unbind("click").bind("click", function(event){        
        doEditHost2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields);   
        return false;
    });   
}

function doEditHost2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields) {   
    var isValid = true;					
    isValid &= validateString("Host Tags", $detailsTab.find("#hosttags_edit"), $detailsTab.find("#hosttags_edit_errormsg"), true); //optional    		
    if (!isValid) 
        return;
       
    var jsonObj = $midmenuItem1.data("jsonObj"); 
	var id = jsonObj.id;
	
	var array1 = [];
	array1.push("&id="+id);
							
	var hosttags = $detailsTab.find("#hosttags_edit").val();
	array1.push("&hosttags="+todb(hosttags));
		
	var osCategoryId = $detailsTab.find("#os_dropdown").val();
    if (osCategoryId != null && osCategoryId.length > 0)
	    array1.push("&osCategoryId="+osCategoryId);
    else //OS is none
    	array1.push("&osCategoryId=0");

	$.ajax({
	    data: createURL("command=updateHost"+array1.join("")),
		dataType: "json",
		async: false,
		success: function(json) {			
		    var jsonObj = json.updatehostresponse.host;		
            hostToMidmenu(jsonObj, $midmenuItem1);
            hostToRightPanel($midmenuItem1);	
		    
		    $editFields.hide();      
            $readonlyFields.show();       
            $("#save_button, #cancel_button").hide(); 		    		
		}
	});
}

function doEnableMaintenanceMode($actionLink, $detailsTab, $midmenuItem1){ 
    var jsonObj = $midmenuItem1.data("jsonObj");
       
    $("#dialog_confirmation")
    .text(dictionary["message.action.host.enable.maintenance.mode"])
    .dialog("option", "buttons", {	                    
         "OK": function() {
             $(this).dialog("close");      
             var id = jsonObj.id;
             var apiCommand = "command=prepareHostForMaintenance&id="+id;
    	     doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
         },
         "Cancel": function() {	                         
             $(this).dialog("close");
         }
    }).dialog("open");     
} 

function doCancelMaintenanceMode($actionLink, $detailsTab, $midmenuItem1){ 
    var jsonObj = $midmenuItem1.data("jsonObj");
       
    $("#dialog_confirmation")
    .text(dictionary["message.action.cancel.maintenance.mode"])
    .dialog("option", "buttons", {	                    
         "OK": function() {
             $(this).dialog("close");      
             var id = jsonObj.id;
             var apiCommand = "command=cancelHostMaintenance&id="+id;
    	     doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
         },
         "Cancel": function() {	                         
             $(this).dialog("close");
         }
    }).dialog("open");     
} 

function doForceReconnect($actionLink, $detailsTab, $midmenuItem1){ 
    var jsonObj = $midmenuItem1.data("jsonObj");
       
    $("#dialog_confirmation")
    .text(dictionary["message.action.force.reconnect"])
    .dialog("option", "buttons", {	                    
         "OK": function() {
             $(this).dialog("close");      
             var id = jsonObj.id;
             var apiCommand = "command=reconnectHost&id="+id;
    	     doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
         },
         "Cancel": function() {	                         
             $(this).dialog("close");
         }
    }).dialog("open");     
} 

function doRemoveHost($actionLink, $detailsTab, $midmenuItem1){ 
    var jsonObj = $midmenuItem1.data("jsonObj");
       
    $("#dialog_confirmation_remove_host")    
    .dialog("option", "buttons", {	                    
         "OK": function() {
             $(this).dialog("close");      
             var id = jsonObj.id;
             var isForced = $("#dialog_confirmation_remove_host").find("#force_remove_host").attr("checked").toString();
             var apiCommand = "command=deleteHost&id="+id+"&forced="+isForced;                
    	     doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
         },
         "Cancel": function() {	                         
             $(this).dialog("close");
         }
    }).dialog("open");     
} 

/*
function doUpdateOSPreference($actionLink, $detailsTab, $midmenuItem1){ 
    var jsonObj = $midmenuItem1.data("jsonObj");
       
    $("#dialog_update_os")
    .dialog("option", "buttons", {	                    
        "Update": function() {
            var $thisDialog = $(this);
            $thisDialog.dialog("close");
	        var osId = $thisDialog.find("#host_os").val();
	        var osName =$thisDialog.find("#host_os option:selected").text();	        
	        if (osId == null || osId.length == 0) //OS is none
	            osId =0;    	    
	      
	        var id = jsonObj.id;    		    
            var apiCommand = "command=updateHost&id="+id+"&osCategoryId="+osId;
    	    doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
        },
        "Cancel": function() {	                         
            $(this).dialog("close");
        }
    }).dialog("open");     
} 
*/
