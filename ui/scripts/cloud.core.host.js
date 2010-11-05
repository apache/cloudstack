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

    initDialog("dialog_add_host");
    initDialog("dialog_add_pool");
    initDialog("dialog_confirmation_enable_maintenance");
    initDialog("dialog_confirmation_cancel_maintenance");
    initDialog("dialog_confirmation_force_reconnect");
    initDialog("dialog_confirmation_remove_host");
    initDialog("dialog_update_os");
        
    hostJsonToDetailsTab($midmenuItem1);    
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
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);       
    resourceLoadPage("jsp/host.jsp", $midmenuItem1);
}

function hostJsonToDetailsTab($midmenuItem1) {
    var jsonObj = $midmenuItem1.data("jsonObj");	    
    var $detailsTab = $("#tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);           
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#grid_header_title").text(fromdb(jsonObj.name));
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
        
    setHostStateInRightPanel(fromdb(jsonObj.state), $detailsTab.find("#state"));
    
    $detailsTab.find("#type").text(fromdb(jsonObj.type));      
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename)); 
    $detailsTab.find("#podname").text(fromdb(jsonObj.podname));   
    $detailsTab.find("#clustername").text(fromdb(jsonObj.clustername));        
    $detailsTab.find("#ipaddress").text(fromdb(jsonObj.ipaddress)); 
    $detailsTab.find("#version").text(fromdb(jsonObj.version));  
    $detailsTab.find("#oscategoryname").text(fromdb(jsonObj.oscategoryname));        
    $detailsTab.find("#disconnected").text(fromdb(jsonObj.disconnected));  
    
    populateForUpdateOSDialog(jsonObj.oscategoryid);
    
    //actions ***   
    var $actionLink = $detailsTab.find("#action_link"); 
    $actionLink.bind("mouseover", function(event) {	    
        $(this).find("#action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {       
        $(this).find("#action_menu").hide();    
        return false;
    });	  
    var $actionMenu = $detailsTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    var noAvailableActions = true;
    
    if (jsonObj.state == 'Up' || jsonObj.state == "Connecting") {
		buildActionLinkForTab("Enable Maintenance Mode", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
	    buildActionLinkForTab("Force Reconnect", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);   
	    buildActionLinkForTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);   
	    noAvailableActions = false;
	} 
	else if(jsonObj.state == 'Down') {
	    buildActionLinkForTab("Enable Maintenance Mode", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
	    buildActionLinkForTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  	    
        buildActionLinkForTab("Remove Host", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
        noAvailableActions = false;
    }	
	else if(jsonObj.state == "Alert") {
	    buildActionLinkForTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  	
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
    
    //temporary for testing (begin) *****
    /*
    buildActionLinkForTab("Enable Maintenance Mode", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
    buildActionLinkForTab("Cancel Maintenance Mode", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
    buildActionLinkForTab("Force Reconnect", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
    buildActionLinkForTab("Remove Host", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
    buildActionLinkForTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);
    noAvailableActions = false; 	  
    */  
    //temporary for testing (begin) *****
    
    // no available actions 
	if(noAvailableActions == true) {
	    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
	}	       
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
            hostToMidmenu(json.queryasyncjobresultresponse.host[0], $midmenuItem1);
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
            hostToMidmenu(json.queryasyncjobresultresponse.host[0], $midmenuItem1);
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
            hostToMidmenu(json.queryasyncjobresultresponse.host[0], $midmenuItem1);
            hostToRightPanel($midmenuItem1);            
            $("#right_panel_content #after_action_info").text("We are actively reconnecting your host.  Please refresh periodically for an updated status."); 
        }
    },
    "Remove Host": {              
        isAsyncJob: false,        
        dialogBeforeActionFn: doRemoveHost,
        inProcessText: "Removing Host....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {    
            $midmenuItem1.remove();
            clearRightPanel();
            hostClearRightPanel();
        }
    },    
    "Update OS Preference": {              
        isAsyncJob: false,        
        dialogBeforeActionFn: doUpdateOSPreference,
        inProcessText: "Updating OS Preference....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {     
            //call listHosts API before bug 6650 ("updateHost API should return an embedded object like what listHosts API does") is fixed.
            $.ajax({
                data: createURL("command=listHosts&id="+id),
                dataType: "json",
                success: function(json) {  
                    hostToMidmenu(json.listhostsresponse.host[0], $midmenuItem1);      
                    hostToRightPanel($midmenuItem1);                     
                }
            });            
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

