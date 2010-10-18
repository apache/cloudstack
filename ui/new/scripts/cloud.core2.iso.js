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

// Version: @VERSION@

var g_zoneIds = []; 
var g_zoneNames = [];	

function afterLoadIsoJSP() {
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    
    //add button ***
    $("#midmenu_add_link").find("#label").text("Add ISO"); 
    $("#midmenu_add_link").show();     
    $("#midmenu_add_link").unbind("click").bind("click", function(event) {     
        $("#dialog_add_iso")
	    .dialog('option', 'buttons', { 				
		    "Create": function() { 	
		        var thisDialog = $(this);
    				
			    // validate values
			    var isValid = true;					
			    isValid &= validateString("Name", thisDialog.find("#add_iso_name"), thisDialog.find("#add_iso_name_errormsg"));
			    isValid &= validateString("Display Text", thisDialog.find("#add_iso_display_text"), thisDialog.find("#add_iso_display_text_errormsg"));
			    isValid &= validateString("URL", thisDialog.find("#add_iso_url"), thisDialog.find("#add_iso_url_errormsg"));			
			    if (!isValid) 
			        return;		
			        
			    thisDialog.dialog("close");	
			    
			    var name = trim(thisDialog.find("#add_iso_name").val());
			    var desc = trim(thisDialog.find("#add_iso_display_text").val());
			    var url = trim(thisDialog.find("#add_iso_url").val());						
			    var zoneId = thisDialog.find("#add_iso_zone").val();	
			    //var isPublic = thisDialog.find("#add_iso_public").val();
			    var isPublic = "false"; //default to private for now
			    var osType = thisDialog.find("#add_iso_os_type").val();
			    var bootable = thisDialog.find("#add_iso_bootable").val();			
    		    				    
		        var $midmenuItem1 = beforeAddingMidMenuItem() ;				    
    		       		    				
			    $.ajax({
			        data: createURL("command=registerIso&name="+todb(name)+"&displayText="+todb(desc)+"&url="+encodeURIComponent(url)+"&zoneId="+zoneId+"&isPublic="+isPublic+"&osTypeId="+osType+"&bootable="+bootable+"&response=json"),
				    dataType: "json",
				    success: function(json) {					
				        var items = json.registerisoresponse.iso;				       
				        isoToMidmenu(items[0], $midmenuItem1);
						bindClickToMidMenu($midmenuItem1, isoToRigntPanel, isoGetMidmenuId);  
						afterAddingMidMenuItem($midmenuItem1, true);
						                        
                        if(items.length > 1) {                               
                            for(var i=1; i<items.length; i++) {   
                                var $midmenuItem2 = $("#midmenu_item").clone();
                                isoToMidmenu(items[i], $midmenuItem2);
                                bindClickToMidMenu($midmenuItem2, isoToRigntPanel, isoGetMidmenuId); 
                                $("#midmenu_container").append($midmenuItem2.show());
                            }                                    
                        }  						
				    }, 
					error: function(XMLHttpResponse) {					    
					    handleErrorInMidMenu(XMLHttpResponse, $midmenuItem1);					  
					}				
			    });
		    },
		    "Cancel": function() { 
			    $(this).dialog("close"); 
		    } 
	    }).dialog("open");
        return false;
    });
    
    //edit button ***
    var $readonlyFields  = $detailsTab.find("#name, #displaytext");
    var $editFields = $detailsTab.find("#name_edit, #displaytext_edit"); 
    initializeEditFunction($readonlyFields, $editFields, doUpdateIso); 
       
    //populate dropdown ***
    var addIsoZoneField = $("#dialog_add_iso #add_iso_zone");    	
	if (isAdmin())  
		addIsoZoneField.append("<option value='-1'>All Zones</option>"); 	
    $.ajax({
        data: createURL("command=listZones&available=true"+maxPageSize),
	    dataType: "json",
	    success: function(json) {		        
		    var zones = json.listzonesresponse.zone;	 			     			    	
		    if (zones != null && zones.length > 0) {
		        for (var i = 0; i < zones.length; i++) {
			        addIsoZoneField.append("<option value='" + zones[i].id + "'>" + fromdb(zones[i].name) + "</option>"); 			        
			        g_zoneIds.push(zones[i].id);
			        g_zoneNames.push(zones[i].name);			       
		        }
		    }				    			
	    }
	});	
    
    $.ajax({
	    data: createURL("command=listOsTypes&response=json"+maxPageSize),
		dataType: "json",
		success: function(json) {
			types = json.listostypesresponse.ostype;
			if (types != null && types.length > 0) {
				var osTypeDropDownAdd = $("#dialog_add_iso #add_iso_os_type").empty();
				var osTypeDropdownEdit = $detailsTab.find("#ostypename_edit").empty();
				for (var i = 0; i < types.length; i++) {
					var html = "<option value='" + types[i].id + "'>" + fromdb(types[i].description) + "</option>";
					osTypeDropDownAdd.append(html);			
					osTypeDropdownEdit.append(html);					
				}
			}	
		}
	});
	
	$.ajax({
	    data: createURL("command=listServiceOfferings&response=json"+maxPageSize),
	    dataType: "json",
	    success: function(json) {
	        var items = json.listserviceofferingsresponse.serviceoffering;
	        if(items != null && items.length > 0 ) {
	            var serviceOfferingField = $("#dialog_create_vm_from_iso #service_offering").empty();
	            for(var i = 0; i < items.length; i++)		        
	                serviceOfferingField.append("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>");
	        }		        
	    }
	});		
	
	$.ajax({
	    data: createURL("command=listDiskOfferings&response=json"+maxPageSize),
	    dataType: "json",
	    success: function(json) {
	        var items = json.listdiskofferingsresponse.diskoffering;
	        if(items != null && items.length > 0 ) {
	            var diskOfferingField = $("#dialog_create_vm_from_iso #disk_offering").empty();
	            for(var i = 0; i < items.length; i++)		        
	                diskOfferingField.append("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>");
	        }		  
	        
	    }
	});		
    
    //initialize dialog box ***
    initDialog("dialog_confirmation_delete_iso_all_zones");
    initDialog("dialog_confirmation_delete_iso");
    initDialog("dialog_copy_iso", 300);
    initDialog("dialog_create_vm_from_iso", 450);
    initDialog("dialog_add_iso", 450);   
}

function isoGetMidmenuId(jsonObj) {
    return "midmenuItem_" + jsonObj.id + "_" + fromdb(jsonObj.zonename).replace(/\s/g, ""); //remove all spaces in zonename
}

function isoToMidmenu(jsonObj, $midmenuItem1) {    
    var id = isoGetMidmenuId(jsonObj);
    $midmenuItem1.attr("id", id);   
    $midmenuItem1.data("jsonObj", jsonObj); 
        
    var $iconContainer = $midmenuItem1.find("#icon_container").show();
    setIconByOsType(jsonObj.ostypename, $iconContainer.find("#icon"));
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(fromdb(jsonObj.zonename).substring(0,25));  
}

function isoToRigntPanel($midmenuItem) {       
    var jsonObj = $midmenuItem.data("jsonObj");
    isoJsonToDetailsTab(jsonObj);   
}

function isoJsonToDetailsTab(jsonObj) {   
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);      
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));
    
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#name_edit").val(fromdb(jsonObj.name));
    
    $detailsTab.find("#displaytext").text(fromdb(jsonObj.displaytext));
    $detailsTab.find("#displaytext_edit").val(fromdb(jsonObj.displaytext));
    
    $detailsTab.find("#account").text(fromdb(jsonObj.account));
                      
    var status = "Ready";
	if (jsonObj.isready == "false")
		status = fromdb(jsonObj.isostatus);	
	$detailsTab.find("#status").text(status); 
	
	if(jsonObj.size != null)
	    $detailsTab.find("#size").text(convertBytes(parseInt(jsonObj.size)));  
	else
	    $detailsTab.find("#size").text("");    
              
    setBooleanField(jsonObj.bootable, $detailsTab.find("#bootable"));	
    setBooleanField(jsonObj.crossZones, $detailsTab.find("#crossZones"));	     
    setDateField(jsonObj.created, $detailsTab.find("#created"));	  
    
    
    //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    var noAvailableActions = true;
    
    var midmenuId = isoGetMidmenuId(jsonObj);
    
    // "Edit", "Copy", "Create VM" 
	if ((isUser() && jsonObj.ispublic == "true" && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account)) || jsonObj.isready == "false") {		
		$("#edit_button").hide();
    }
    else {        
        $("#edit_button").show();
        buildActionLinkForDetailsTab("Copy ISO", isoActionMap, $actionMenu, midmenuId);		
        noAvailableActions = false;
    }
		
	// "Create VM" 
	if (((isUser() && jsonObj.ispublic == "true" && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account)) || jsonObj.isready == "false") || (jsonObj.bootable == "false")) {
	}
    else {        
        buildActionLinkForDetailsTab("Create VM", isoActionMap, $actionMenu, midmenuId);	
        noAvailableActions = false;
    }
    
	// "Delete" 
	if (((isUser() && jsonObj.ispublic == "true" && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account))) || (jsonObj.isready == "false" && jsonObj.isostatus != null && jsonObj.isostatus.indexOf("% Downloaded") != -1)) {
	}
	else {	    
	    buildActionLinkForDetailsTab("Delete ISO", isoActionMap, $actionMenu, midmenuId);	
	    noAvailableActions = false;
	}    
	
	// no available actions 
	if(noAvailableActions == true) {
	    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
	}	    
}

function isoClearRightPanel() {
    isoClearDetailsTab(); 
}

function isoClearDetailsTab() {
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    
    $detailsTab.find("#id").text("");
    $detailsTab.find("#zonename").text("");
    
    $detailsTab.find("#name").text("");
    $detailsTab.find("#name_edit").val("");
    
    $detailsTab.find("#displaytext").text("");
    $detailsTab.find("#displaytext_edit").val("");
    
    $detailsTab.find("#account").text("");    
    $detailsTab.find("#size").text("");  
	$detailsTab.find("#status").text(""); 
	$detailsTab.find("#bootable").text("");
	$detailsTab.find("#crossZones").text("");
    $detailsTab.find("#created").text("");   
}

var isoActionMap = {  
    "Delete ISO": {                  
        isAsyncJob: true,
        asyncJobResponse: "deleteisosresponse",
        dialogBeforeActionFn : doDeleteIso,
        inProcessText: "Deleting ISO....",
        afterActionSeccessFn: function(json, id, midmenuItemId){                    
            var $midmenuItem1 = $("#"+midmenuItemId); 
            $midmenuItem1.remove();
            clearRightPanel();
            isoClearRightPanel();
        }        
    },
    "Copy ISO": {
        isAsyncJob: true,
        asyncJobResponse: "copyisoresponse",            
        dialogBeforeActionFn : doCopyIso,
        inProcessText: "Copying ISO....",
        afterActionSeccessFn: function(json, id, midmenuItemId){}   
    }  
    ,
    "Create VM": {
        isAsyncJob: true,
        asyncJobResponse: "deployvirtualmachineresponse",            
        dialogBeforeActionFn : doCreateVMFromIso,
        inProcessText: "Creating VM....",
        afterActionSeccessFn: function(json, id, midmenuItemId){}   
    }  
}   

function doUpdateIso() { 
    var $detailsTab = $("#right_panel_content #tab_content_details");      
    
    // validate values
    var isValid = true;					
    isValid &= validateString("Name", $detailsTab.find("#name_edit"), $detailsTab.find("#name_edit_errormsg"));
    isValid &= validateString("Display Text", $detailsTab.find("#displaytext_edit"), $detailsTab.find("#displaytext_edit_errormsg"));			
    if (!isValid) 
        return;
       
    var jsonObj = $detailsTab.data("jsonObj"); 
	var id = jsonObj.id;
	var midmenuId = isoGetMidmenuId(jsonObj);
							
	var name = trim($detailsTab.find("#name_edit").val());
	var displaytext = trim($detailsTab.find("#displaytext_edit").val());
	
	$.ajax({
	    data: createURL("command=updateIso&id="+id+"&name="+todb(name)+"&displayText="+todb(displaytext)),
		dataType: "json",
		success: function(json) {	
		    var jsonObj = json.updateisoresponse;		    
		    isoToMidmenu(jsonObj, $("#"+midmenuId)); 
            isoJsonToDetailsTab(jsonObj);	    					
		}
	});
}

function doDeleteIso($actionLink, $detailsTab, midmenuItemId) {   
    var $detailsTab = $("#right_panel_content #tab_content_details"); 
    var jsonObj = $detailsTab.data("jsonObj");
	var id = jsonObj.id;
	var name = jsonObj.name;			
	var zoneId = jsonObj.zoneid;

    var moreCriteria = [];						
	if (zoneId != null) 
		moreCriteria.push("&zoneid="+zoneId);	
	
	var $dialog1;
	if(jsonObj.crossZones == "true")
	    $dialog1 = $("#dialog_confirmation_delete_iso_all_zones");
	else
	    $dialog1 = $("#dialog_confirmation_delete_iso");	
	
	$dialog1	
	.dialog('option', 'buttons', { 					
		"Confirm": function() { 			
			$(this).dialog("close");			
			var apiCommand = "command=deleteIso&id="+id+moreCriteria.join("");
            doActionToDetailsTab(id, $actionLink, apiCommand, midmenuItemId);	
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		}
	}).dialog("open");
}

function populateZoneFieldExcludeSourceZone(zoneField, excludeZoneId) {	  
    zoneField.empty();  
    if (g_zoneIds != null && g_zoneIds.length > 0) {
        for (var i = 0; i < g_zoneIds.length; i++) {
            if(g_zoneIds[i]	!= excludeZoneId)			            
	            zoneField.append("<option value='" + g_zoneIds[i] + "'>" + fromdb(g_zoneNames[i]) + "</option>"); 			        			       
        }
    }			    
}

function doCopyIso($actionLink, $detailsTab, midmenuItemId) {   
	var jsonObj = $detailsTab.data("jsonObj");
	var id = jsonObj.id;
	var name = jsonObj.name;			
	var sourceZoneId = jsonObj.zoneid;				
	populateZoneFieldExcludeSourceZone($("#dialog_copy_iso #copy_iso_zone"), sourceZoneId);		
			
	$("#dialog_copy_iso")
	.dialog('option', 'buttons', {				    
	    "OK": function() {				       
	        var thisDialog = $(this);
	        	        	        
	        var isValid = true;	 
            isValid &= validateDropDownBox("Zone", thisDialog.find("#copy_iso_zone"), thisDialog.find("#copy_iso_zone_errormsg"), false);  //reset error text		         
	        if (!isValid) return;     
	        
	        thisDialog.dialog("close");
	        				        
	        var destZoneId = thisDialog.find("#copy_iso_zone").val();	
            var apiCommand = "command=copyIso&id="+id+"&sourcezoneid="+sourceZoneId+"&destzoneid="+destZoneId;
            doActionToDetailsTab(id, $actionLink, apiCommand, midmenuItemId);	 
	    }, 
	    "Cancel": function() {				        
		    $(this).dialog("close");
	    }				
	}).dialog("open");	
}	

function doCreateVMFromIso($actionLink, $detailsTab, midmenuItemId) { 
    var jsonObj = $detailsTab.data("jsonObj");	
	var id = jsonObj.id;		
	var name = jsonObj.name;				
	var zoneId = jsonObj.zoneid;
	var createVmDialog = $("#dialog_create_vm_from_iso");				
			
	createVmDialog
	.dialog('option', 'buttons', {			    
	    "Create": function() {
	        var thisDialog = $(this);	
	      
	        // validate values
		    var isValid = true;		
		    isValid &= validateString("Name", thisDialog.find("#name"), thisDialog.find("#name_errormsg"), true);
		    isValid &= validateString("Group", thisDialog.find("#group"), thisDialog.find("#group_errormsg"), true);				
		    if (!isValid) return;	       
	           
	        thisDialog.dialog("close");   
	        
	        var array1 = [];
	        var name = trim(thisDialog.find("#name").val());
	        array1.push("&displayname="+todb(name));		
	        var group = trim(thisDialog.find("#group").val());	
	        array1.push("&group="+todb(group));	
	        var serviceOfferingId = thisDialog.find("#service_offering").val();	
	        array1.push("&serviceOfferingId="+serviceOfferingId);			        
	        var diskOfferingId = thisDialog.find("#disk_offering").val();	
	        array1.push("&diskOfferingId="+diskOfferingId);
	        var hypervisor = thisDialog.find("#hypervisor").val();	
	        array1.push("&hypervisor="+hypervisor);	
	                         
		    var apiCommand = "command=deployVirtualMachine&zoneId="+zoneId+"&templateId="+id+array1.join("");
    	    doActionToDetailsTab(id, $actionLink, apiCommand, midmenuItemId);		
	    }, 
	    "Cancel": function() {
	        $(this).dialog("close");
	    }
	}).dialog("open");			
}	