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
  
function afterLoadHostJSP($midmenuItem1) {
    initAddHostButton($("#midmenu_add_link"), "host_page"); 
    initAddPrimaryStorageButton($("#midmenu_add2_link"), "host_page");          

    //initDialog("dialog_add_host");
    //initDialog("dialog_add_pool");
    initDialog("dialog_confirmation_enable_maintenance");
    initDialog("dialog_confirmation_cancel_maintenance");
    initDialog("dialog_confirmation_force_reconnect");
    initDialog("dialog_confirmation_remove_host");
    initDialog("dialog_update_os");
         
    // switch between different tabs 
    var tabArray = [$("#tab_details"), $("#tab_statistics"), $("#tab_instance"), $("#tab_router"), $("#tab_systemvm")];
    var tabContentArray = [$("#tab_content_details"), $("#tab_content_statistics"), $("#tab_content_instance"), $("#tab_content_router"), $("#tab_content_systemvm")];
    var afterSwitchFnArray = [hostJsonToDetailsTab, hostJsonToStatisticsTab, hostJsonToInstanceTab, hostJsonToRouterTab, hostJsonToSystemvmTab];
    switchBetweenDifferentTabs(tabArray, tabContentArray, afterSwitchFnArray);         
}


function hostGetMidmenuId(jsonObj) {
    return "midmenuItem_host_" + jsonObj.id; 
}

function hostToMidmenu(jsonObj, $midmenuItem1) {    
    $midmenuItem1.attr("id", hostGetMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj);      
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show(); 
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_host.png");   
       
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.ipaddress.substring(0,25)); 
    
    updateHostStateInMidMenu(jsonObj, $midmenuItem1);   
}

function hostToRightPanel($midmenuItem1) {     
    resourceLoadPage("jsp/host.jsp", $midmenuItem1);   
}

function hostJsonToDetailsTab() {    
    var $thisTab = $("#right_panel_content #tab_content_details");  
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();        
    
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    var jsonObj = $midmenuItem1.data("jsonObj");	    
    
    $thisTab.data("jsonObj", jsonObj);           
    $thisTab.find("#id").text(fromdb(jsonObj.id));
    $thisTab.find("#grid_header_title").text(fromdb(jsonObj.name));
    $thisTab.find("#name").text(fromdb(jsonObj.name));
        
    setHostStateInRightPanel(fromdb(jsonObj.state), $thisTab.find("#state"));
    
    $thisTab.find("#type").text(fromdb(jsonObj.type));      
    $thisTab.find("#zonename").text(fromdb(jsonObj.zonename)); 
    $thisTab.find("#podname").text(fromdb(jsonObj.podname));   
    $thisTab.find("#clustername").text(fromdb(jsonObj.clustername));        
    $thisTab.find("#ipaddress").text(fromdb(jsonObj.ipaddress)); 
    $thisTab.find("#version").text(fromdb(jsonObj.version));  
    $thisTab.find("#oscategoryname").text(fromdb(jsonObj.oscategoryname));        
    $thisTab.find("#disconnected").text(fromdb(jsonObj.disconnected));  
    
    populateForUpdateOSDialog(jsonObj.oscategoryid);
    
    //actions ***   
    var $actionLink = $thisTab.find("#action_link"); 
    $actionLink.bind("mouseover", function(event) {	    
        $(this).find("#action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {       
        $(this).find("#action_menu").hide();    
        return false;
    });	  
    var $actionMenu = $thisTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    var noAvailableActions = true;
    
    if (jsonObj.state == 'Up' || jsonObj.state == "Connecting") {
		buildActionLinkForTab("Enable Maintenance Mode", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);  
	    buildActionLinkForTab("Force Reconnect", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);   
	    buildActionLinkForTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);   
	    noAvailableActions = false;
	} 
	else if(jsonObj.state == 'Down') {
	    buildActionLinkForTab("Enable Maintenance Mode", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);  
	    buildActionLinkForTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);  	    
        buildActionLinkForTab("Remove Host", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);  
        noAvailableActions = false;
    }	
	else if(jsonObj.state == "Alert") {
	    buildActionLinkForTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab); 
	    buildActionLinkForTab("Remove Host", hostActionMap, $actionMenu, $midmenuItem1, $thisTab);   	
	    noAvailableActions = false;   
     
	}	
	else if (jsonObj.state == "ErrorInMaintenance") {
	    buildActionLinkForTab("Enable Maintenance Mode", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
        buildActionLinkForTab("Cancel Maintenance Mode", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
        buildActionLinkForTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  	 
        noAvailableActions = false;   
    }
	else if (jsonObj.state == "PrepareForMaintenance") {
	    buildActionLinkForTab("Cancel Maintenance Mode", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
        buildActionLinkForTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  	
        noAvailableActions = false;    
    }
	else if (jsonObj.state == "Maintenance") {
	    buildActionLinkForTab("Cancel Maintenance Mode", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
        buildActionLinkForTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  	    
        buildActionLinkForTab("Remove Host", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
        noAvailableActions = false;
    }
	else if (jsonObj.state == "Disconnected"){
	    buildActionLinkForTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  	    
        buildActionLinkForTab("Remove Host", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
        noAvailableActions = false;
    }
	else {
	    alert("Unsupported Host State: " + jsonObj.state);
	} 
        
    // no available actions 
	if(noAvailableActions == true) {
	    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
	}	       
}

function hostJsonToStatisticsTab() {
    var $thisTab = $("#right_panel_content #tab_content_statistics");  
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   

    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    var jsonObj = $midmenuItem1.data("jsonObj");
    
    var $barChartContainer = $thisTab.find("#cpu_barchart");
         
    var cpuNumber = ((jsonObj.cpunumber==null)? "":jsonObj.cpunumber.toString());
    $barChartContainer.find("#cpunumber").text(cpuNumber);
    
    var cpuSpeed = ((jsonObj.cpuspeed==null)? "":convertHz(jsonObj.cpuspeed)) ;
    $barChartContainer.find("#cpuspeed").text(cpuSpeed);
    
    $barChartContainer.find("#bar_chart").removeClass().addClass("db_barbox").css("width", "0%");    
    $barChartContainer.find("#percentused").text("");   
    if(jsonObj.cpuused!=null)
        drawBarChart($barChartContainer, jsonObj.cpuused);		
    
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

function hostJsonToInstanceTab() {   
    var $thisTab = $("#right_panel_content #tab_content_instance");
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show(); 
    		
	var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");	
	var jsonObj = $midmenuItem1.data("jsonObj");	
    
    $.ajax({
		cache: false,
		data: createURL("command=listVirtualMachines&hostid="+jsonObj.id),
		dataType: "json",
		success: function(json) {							    
			var items = json.listvirtualmachinesresponse.virtualmachine;																						
			if (items != null && items.length > 0) {
			    var $container = $thisTab.find("#tab_container").empty();
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

function hostInstanceJSONToTemplate(jsonObj, template) {
    template.data("jsonObj", jsonObj);     
    template.attr("id", "host_instance_"+jsonObj.id).data("hostInstanceId", jsonObj.id);    
    template.find("#grid_header_title").text(fromdb(jsonObj.name));			   
    template.find("#id").text(jsonObj.id);
    template.find("#name").text(fromdb(jsonObj.name));	  
    template.find("#ipaddress").text(fromdb(jsonObj.ipaddress));
    template.find("#serviceOfferingName").text(fromdb(jsonObj.serviceofferingname));	
    template.find("#account").text(fromdb(jsonObj.account));
    template.find("#domain").text(fromdb(jsonObj.domain));
    setDateField(jsonObj.created, template.find("#created"));	 		
} 

function hostJsonToRouterTab() {   
    var $thisTab = $("#right_panel_content #tab_content_router");
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show(); 
    		
	var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");	
	var jsonObj = $midmenuItem1.data("jsonObj");	
    
    $.ajax({
		cache: false,
		data: createURL("command=listRouters&hostid="+jsonObj.id),
		dataType: "json",
		success: function(json) {							    
			var items = json.listroutersresponse.router;																						
			if (items != null && items.length > 0) {
			    var $container = $thisTab.find("#tab_container").empty();
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

function hostRouterJSONToTemplate(jsonObj, template) {
    template.data("jsonObj", jsonObj);     
    template.attr("id", "host_router_"+jsonObj.id).data("hostRouterId", jsonObj.id);    
    template.find("#grid_header_title").text(fromdb(jsonObj.name));			   
    template.find("#id").text(jsonObj.id);
    template.find("#name").text(fromdb(jsonObj.name));	  
    template.find("#publicip").text(fromdb(jsonObj.publicip));   
    template.find("#privateip").text(fromdb(jsonObj.privateip));
    template.find("#guestipaddress").text(fromdb(jsonObj.guestipaddress)); 
    template.find("#account").text(fromdb(jsonObj.account));
    template.find("#domain").text(fromdb(jsonObj.domain));
    setDateField(jsonObj.created, template.find("#created"));	 		
}  

function hostJsonToSystemvmTab() {   
    var $thisTab = $("#right_panel_content #tab_content_systemvm");
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show(); 
    		
	var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");	
	var jsonObj = $midmenuItem1.data("jsonObj");	
    
    $.ajax({
		cache: false,
		data: createURL("command=listSystemVms&hostid="+jsonObj.id),
		dataType: "json",
		success: function(json) {							    
			var items = json.listsystemvmsresponse.systemvm;																						
			if (items != null && items.length > 0) {
			    var $container = $thisTab.find("#tab_container").empty();
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

function hostSystemvmJSONToTemplate(jsonObj, template) {
    template.data("jsonObj", jsonObj);     
    template.attr("id", "host_systemvm_"+jsonObj.id).data("hostSystemvmId", jsonObj.id);    
    template.find("#grid_header_title").text(fromdb(jsonObj.name));			   
    template.find("#id").text(jsonObj.id);
    template.find("#name").text(fromdb(jsonObj.name));	  
    template.find("#publicip").text(fromdb(jsonObj.publicip));    
    template.find("#privateip").text(fromdb(jsonObj.privateip));  
    setDateField(jsonObj.created, template.find("#created"));	 		
}  

function hostClearRightPanel() {
    hostClearDetailsTab(); 
}

function hostClearDetailsTab() {
    var $detailsTab = $("#right_panel_content").find("#tab_content_details");  
    $detailsTab.find("#id").text("");
    $detailsTab.find("#name").text("");
    $detailsTab.find("#state").text("");        
    $detailsTab.find("#zonename").text(""); 
    $detailsTab.find("#podname").text(""); 
    $detailsTab.find("#clustername").text(""); 
    $detailsTab.find("#ipaddress").text(""); 
    $detailsTab.find("#version").text(""); 
    $detailsTab.find("#oscategoryname").text("");       
    $detailsTab.find("#disconnected").text(""); 
    
    var $actionMenu = $detailsTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();   
	$actionMenu.find("#action_list").append($("#no_available_actions").clone().show());		    
}

function populateForUpdateOSDialog(oscategoryid) {	
	$.ajax({
	    data: createURL("command=listOsCategories"+maxPageSize),
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
    "Enable Maintenance Mode": {              
        isAsyncJob: true,
        asyncJobResponse: "preparehostformaintenanceresponse",
        dialogBeforeActionFn: doEnableMaintenanceMode,
        inProcessText: "Enabling Maintenance Mode....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {            
            var item = json.queryasyncjobresultresponse.jobresult.host;
            hostToMidmenu(item, $midmenuItem1);
            hostToRightPanel($midmenuItem1);            
            $("#right_panel_content #after_action_info").text("We are actively enabling maintenance on your host. Please refresh periodically for an updated status."); 
        }
    },
    "Cancel Maintenance Mode": {              
        isAsyncJob: true,
        asyncJobResponse: "cancelhostmaintenanceresponse",
        dialogBeforeActionFn: doCancelMaintenanceMode,
        inProcessText: "Cancelling Maintenance Mode....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {            
            var item = json.queryasyncjobresultresponse.jobresult.host;  
            hostToMidmenu(item, $midmenuItem1);  
            hostToRightPanel($midmenuItem1);            
            $("#right_panel_content #after_action_info").text("We are actively cancelling your scheduled maintenance.  Please refresh periodically for an updated status."); 
        }
    },
    "Force Reconnect": {              
        isAsyncJob: true,
        asyncJobResponse: "reconnecthostresponse",
        dialogBeforeActionFn: doForceReconnect,
        inProcessText: "Reconnecting....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {
            var item = json.queryasyncjobresultresponse.jobresult.host;
            hostToMidmenu(item, $midmenuItem1);  
            hostToRightPanel($midmenuItem1);            
            $("#right_panel_content #after_action_info").text("We are actively reconnecting your host.  Please refresh periodically for an updated status."); 
        }
    },
    "Remove Host": {              
        isAsyncJob: false,        
        dialogBeforeActionFn: doRemoveHost,
        inProcessText: "Removing Host....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {    
            $midmenuItem1.slideUp("slow", function() {
               $(this).remove();
            });   
            clearRightPanel();
            hostClearRightPanel();
        }
    },    
    "Update OS Preference": {              
        isAsyncJob: false,        
        dialogBeforeActionFn: doUpdateOSPreference,
        inProcessText: "Updating OS Preference....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {     
            var item = json.updatehostresponse.host;
            hostToMidmenu(item, $midmenuItem1);      
            hostToRightPanel($midmenuItem1);         
        }
    }          
} 

function doEnableMaintenanceMode($actionLink, $detailsTab, $midmenuItem1){ 
    var jsonObj = $detailsTab.data("jsonObj");
       
    $("#dialog_confirmation_enable_maintenance")
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
    var jsonObj = $detailsTab.data("jsonObj");
       
    $("#dialog_confirmation_cancel_maintenance")
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
    var jsonObj = $detailsTab.data("jsonObj");
       
    $("#dialog_confirmation_force_reconnect")
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
    var jsonObj = $detailsTab.data("jsonObj");
       
    $("#dialog_confirmation_remove_host")
    .dialog("option", "buttons", {	                    
         "OK": function() {
             $(this).dialog("close");      
             var id = jsonObj.id;
             var apiCommand = "command=deleteHost&id="+id;
    	     doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
         },
         "Cancel": function() {	                         
             $(this).dialog("close");
         }
    }).dialog("open");     
} 

function doUpdateOSPreference($actionLink, $detailsTab, $midmenuItem1){ 
    var jsonObj = $detailsTab.data("jsonObj");
       
    $("#dialog_update_os")
    .dialog("option", "buttons", {	                    
        "Update": function() {
            var $thisDialog = $(this);
            $thisDialog.dialog("close");
	        var osId = $thisDialog.find("#host_os").val();
	        var osName =$thisDialog.find("#host_os option:selected").text();	        
	        if (osId == null || osId.length == 0)
	            return;	        
	      
	        var id = jsonObj.id;    		    
            var apiCommand = "command=updateHost&id="+id+"&osCategoryId="+osId;
    	    doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
        },
        "Cancel": function() {	                         
            $(this).dialog("close");
        }
    }).dialog("open");     
} 

