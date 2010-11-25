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
 
function primarystorageGetMidmenuId(jsonObj) {
    return "midmenuItem_primarystorage_" + jsonObj.id; 
}

function primarystorageToMidmenu(jsonObj, $midmenuItem1) {    
    $midmenuItem1.attr("id", primarystorageGetMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj);      
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show(); 
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_primarystorage.png");    
      
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.ipaddress.substring(0,25));  
     
    updateHostStateInMidMenu(jsonObj, $midmenuItem1);           
}

function primarystorageToRightPanel($midmenuItem1) {  
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);         
    resourceLoadPage("jsp/primarystorage.jsp", $midmenuItem1);  //after reloading "jsp/primarystorage.jsp", afterLoadPrimaryStorageJSP() will be called.
}

 function afterLoadPrimaryStorageJSP($midmenuItem1) {
    initAddHostButton($("#midmenu_add_host_button"), "primarystorage_page", $midmenuItem1); 
    initAddPrimaryStorageButton($("#midmenu_add_primarystorage_button"), "primarystorage_page", $midmenuItem1);  
    
    initDialog("dialog_add_host");
    initDialog("dialog_add_pool");
    bindEventHandlerToDialogAddPool($("#dialog_add_pool"));	     
    initDialog("dialog_confirmation_delete_primarystorage");
     
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);    
    primarystorageJsonToDetailsTab(); 
}

function primarystorageJsonToDetailsTab() {	
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null)
        return;
    
    var jsonObj = $midmenuItem1.data("jsonObj");
    if(jsonObj == null)
        return; 
    
    var $thisTab = $("#right_panel_content").find("#tab_content_details");  
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();        
                
    $thisTab.find("#id").text(noNull(jsonObj.id));
    $thisTab.find("#grid_header_title").text(fromdb(jsonObj.name));
    $thisTab.find("#name").text(fromdb(jsonObj.name));
    
    setHostStateInRightPanel(fromdb(jsonObj.state), $thisTab.find("#state"));
    
    $thisTab.find("#zonename").text(fromdb(jsonObj.zonename));
    $thisTab.find("#podname").text(fromdb(jsonObj.podname));
    $thisTab.find("#clustername").text(fromdb(jsonObj.clustername));
	var storageType = "ISCSI Share";
	if (jsonObj.type == 'NetworkFilesystem') 
	    storageType = "NFS Share";
    $thisTab.find("#type").text(fromdb(storageType));
    $thisTab.find("#ipaddress").text(noNull(jsonObj.ipaddress));
    $thisTab.find("#path").text(fromdb(jsonObj.path));                
	$thisTab.find("#disksizetotal").text(convertBytes(jsonObj.disksizetotal));
	$thisTab.find("#disksizeallocated").text(convertBytes(jsonObj.disksizeallocated));
	
	$thisTab.find("#tags").text(fromdb(jsonObj.tags));   
	$thisTab.find("#tags_edit").val(fromdb(jsonObj.tags));  
	 
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
    buildActionLinkForTab("Edit Primary Storage", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab);     
      
    if (jsonObj.state == 'Up' || jsonObj.state == "Connecting") {
		buildActionLinkForTab("Enable Maintenance Mode", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab);  
	} 
	else if(jsonObj.state == 'Down') {
	    buildActionLinkForTab("Enable Maintenance Mode", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab); 
        buildActionLinkForTab("Delete Primary Storage", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab);  
        
    }	
	else if(jsonObj.state == "Alert") {	     
	    buildActionLinkForTab("Delete Primary Storage", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab);   
	}	
	else if (jsonObj.state == "ErrorInMaintenance") {
	    buildActionLinkForTab("Enable Maintenance Mode", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab);  
        buildActionLinkForTab("Cancel Maintenance Mode", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab);   
    }
	else if (jsonObj.state == "PrepareForMaintenance") {
	    buildActionLinkForTab("Cancel Maintenance Mode", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab); 
    }
	else if (jsonObj.state == "Maintenance") {
	    buildActionLinkForTab("Cancel Maintenance Mode", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab);            	    
        buildActionLinkForTab("Delete Primary Storage", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab);          
    }
	else if (jsonObj.state == "Disconnected"){	      	    
        buildActionLinkForTab("Delete Primary Storage", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab);          
    }
	else {
	    alert("Unsupported Host State: " + jsonObj.state);
	}      
    
    $thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();         
}
       
function primarystorageClearRightPanel() {  
    primarystorageJsonClearDetailsTab();  
}

function primarystorageJsonClearDetailsTab() {	    
    var $thisTab = $("#right_panel_content").find("#tab_content_details");   
    $thisTab.find("#id").text("");
    $thisTab.find("#name").text("");
	$thisTab.find("#state").text("");
    $thisTab.find("#zonename").text("");
    $thisTab.find("#podname").text("");
    $thisTab.find("#clustername").text("");
    $thisTab.find("#type").text("");
    $thisTab.find("#ipaddress").text("");
    $thisTab.find("#path").text("");                
	$thisTab.find("#disksizetotal").text("");
	$thisTab.find("#disksizeallocated").text("");
	$thisTab.find("#tags").text("");         
}

var primarystorageActionMap = {    
    "Edit Primary Storage": {
        dialogBeforeActionFn: doEditPrimaryStorage
    },           
    "Enable Maintenance Mode": {              
        isAsyncJob: true,
        asyncJobResponse: "prepareprimarystorageformaintenanceresponse",
        dialogBeforeActionFn: doEnableMaintenanceModeForPrimaryStorage,
        inProcessText: "Enabling Maintenance Mode....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {             
            var item = json.queryasyncjobresultresponse.jobresult.storagepool; 
            primarystorageToMidmenu(item, $midmenuItem1);
            primarystorageToRightPanel($midmenuItem1);                        
            $("#right_panel_content #after_action_info").text("We are actively enabling maintenance. Please refresh periodically for an updated status."); 
        }
    },
    "Cancel Maintenance Mode": {              
        isAsyncJob: true,
        asyncJobResponse: "cancelprimarystoragemaintenanceresponse",
        dialogBeforeActionFn: doCancelMaintenanceModeForPrimaryStorage,
        inProcessText: "Cancelling Maintenance Mode....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {       
            var item = json.queryasyncjobresultresponse.jobresult.storagepool;    
            primarystorageToMidmenu(item, $midmenuItem1);
            primarystorageToRightPanel($midmenuItem1);            
            $("#right_panel_content #after_action_info").text("We are actively cancelling your scheduled maintenance.  Please refresh periodically for an updated status."); 
        }
    },
    "Delete Primary Storage": {              
        isAsyncJob: false,        
        dialogBeforeActionFn: doDeletePrimaryStorage,
        inProcessText: "Deleting Primary Storage....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {   
            $midmenuItem1.slideUp("slow", function() {
                $(this).remove();
            });   
            clearRightPanel();
            primarystorageClearRightPanel();
        }
    }
}

function doEditPrimaryStorage($actionLink, $detailsTab, $midmenuItem1) {       
    var $readonlyFields  = $detailsTab.find("#tags");
    var $editFields = $detailsTab.find("#tags_edit"); 
             
    $readonlyFields.hide();
    $editFields.show();  
    $detailsTab.find("#cancel_button, #save_button").show();
    
    $detailsTab.find("#cancel_button").unbind("click").bind("click", function(event){    
        $editFields.hide();
        $readonlyFields.show();   
        $("#save_button, #cancel_button").hide();       
        return false;
    });
    $detailsTab.find("#save_button").unbind("click").bind("click", function(event){        
        doEditPrimaryStorage2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields);   
        return false;
    });   
}

function doEditPrimaryStorage2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields) {       
    var jsonObj = $midmenuItem1.data("jsonObj");
    var id = jsonObj.id;
    
    // validate values   
    var isValid = true;					
    isValid &= validateString("Tags", $detailsTab.find("#tags_edit"), $detailsTab.find("#tags_edit_errormsg"), true);	//optional
    if (!isValid) 
        return;	
     
    var array1 = [];       
	
	var tags = $detailsTab.find("#tags_edit").val();
	array1.push("&tags="+encodeURIComponent(tags));	
	
	if(array1.length == 0)
	    return;
	
	$.ajax({
	    data: createURL("command=updateStoragePool&id="+id+array1.join("")),
		dataType: "json",
		success: function(json) {			       
		    primarystorageToMidmenu(jsonObj, $midmenuItem1);
            primarystorageToRightPanel($midmenuItem1);	
		    		    
		    $editFields.hide();      
            $readonlyFields.show();       
            $("#save_button, #cancel_button").hide();     	  
		}
	});
}

function doEnableMaintenanceModeForPrimaryStorage($actionLink, $detailsTab, $midmenuItem1){ 
    var jsonObj = $midmenuItem1.data("jsonObj");
       
    $("#dialog_confirmation")
    .text("Warning: placing the primary storage into maintenance mode will cause all VMs using volumes from it to be stopped.  Do you want to continue?")
    .dialog("option", "buttons", {	                    
         "OK": function() {
             $(this).dialog("close");      
             var id = jsonObj.id;
             var apiCommand = "command=enableStorageMaintenance&id="+id;
    	     doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
         },
         "Cancel": function() {	                         
             $(this).dialog("close");
         }
    }).dialog("open");     
} 

function doCancelMaintenanceModeForPrimaryStorage($actionLink, $detailsTab, $midmenuItem1){ 
    var jsonObj = $midmenuItem1.data("jsonObj");
       
    $("#dialog_confirmation")
    .text("Please confirm you want to cancel maintenace")
    .dialog("option", "buttons", {	                    
         "OK": function() {
             $(this).dialog("close");      
             var id = jsonObj.id;
             var apiCommand = "command=cancelStorageMaintenance&id="+id;
    	     doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
         },
         "Cancel": function() {	                         
             $(this).dialog("close");
         }
    }).dialog("open");     
} 

function doDeletePrimaryStorage($actionLink, $detailsTab, $midmenuItem1){ 
    var jsonObj = $midmenuItem1.data("jsonObj");
       
    $("#dialog_confirmation_delete_primarystorage")
    .dialog("option", "buttons", {	                    
         "OK": function() {
             $(this).dialog("close");      
             var id = jsonObj.id;
             var apiCommand = "command=deleteStoragePool&id="+id;
    	     doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
         },
         "Cancel": function() {	                         
             $(this).dialog("close");
         }
    }).dialog("open");     
} 

