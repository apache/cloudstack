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

var xsToolsIsoId = 200;

var g_zoneIds = []; 
var g_zoneNames = [];	

function isoGetSearchParams() {
    var moreCriteria = [];	

	var $advancedSearchPopup = $("#advanced_search_popup");
	if (lastSearchType == "advanced_search" && $advancedSearchPopup.length > 0) {
	    var name = $advancedSearchPopup.find("#adv_search_name").val();							
		if (name!=null && trim(name).length > 0) 
			moreCriteria.push("&name="+todb(name));	
		
		var zone = $advancedSearchPopup.find("#adv_search_zone").val();	
	    if (zone!=null && zone.length > 0) 
			moreCriteria.push("&zoneId="+zone);	
				
        if ($advancedSearchPopup.find("#adv_search_domain_li").css("display") != "none") {		
		    var domainId = $advancedSearchPopup.find("#adv_search_domain").val();		
		    if (domainId!=null && domainId.length > 0) 
			    moreCriteria.push("&domainid="+domainId);	
    	}	
    	
		if ($advancedSearchPopup.find("#adv_search_account_li").css("display") != "none") {	
		    var account = $advancedSearchPopup.find("#adv_search_account").val();		
		    if (account!=null && account.length > 0) 
			    moreCriteria.push("&account="+account);		
		}	
	} 
	else {     			    		
	    var searchInput = $("#basic_search").find("#search_input").val();	 
        if (lastSearchType == "basic_search" && searchInput != null && searchInput.length > 0) {	           
            moreCriteria.push("&keyword="+todb(searchInput));	       
        }        
	}
	
	return moreCriteria.join("");          
}

function afterLoadIsoJSP() {    
    initDialog("dialog_confirmation_delete_iso_all_zones");
    initDialog("dialog_confirmation_delete_iso");
    initDialog("dialog_copy_iso", 300);    
    initDialog("dialog_download_ISO");
    
    initAddIsoDialog();
    initCreateVmFromIsoDialog();
}

function initAddIsoDialog() {
    initDialog("dialog_add_iso", 450);   

    var $dialogAddIso = $("#dialog_add_iso");
    var $detailsTab = $("#right_panel_content").find("#tab_content_details");             
    
    if(isAdmin())
	    $dialogAddIso.find("#isfeatured_container").show();
	else
	    $dialogAddIso.find("#isfeatured_container").hide();		
    
    var addIsoZoneField = $dialogAddIso.find("#add_iso_zone");    	
	if (isAdmin())  
		addIsoZoneField.append("<option value='-1'>All Zones</option>"); 	
    $.ajax({
        data: createURL("command=listZones&available=true"),
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
	    data: createURL("command=listOsTypes"),
		dataType: "json",
		success: function(json) {
			types = json.listostypesresponse.ostype;
			if (types != null && types.length > 0) {
				var osTypeDropDownAdd = $dialogAddIso.find("#add_iso_os_type").empty();
				var osTypeDropdownEdit = $detailsTab.find("#ostypename_edit").empty();
				for (var i = 0; i < types.length; i++) {
					var html = "<option value='" + types[i].id + "'>" + fromdb(types[i].description) + "</option>";
					osTypeDropDownAdd.append(html);			
					osTypeDropdownEdit.append(html);					
				}
			}	
		}
	});
    
    //add button ***     
    $("#add_iso_button").unbind("click").bind("click", function(event) {     
        $dialogAddIso
	    .dialog('option', 'buttons', { 				
		    "Create": function() { 	
		        var thisDialog = $(this);
    				
			    // validate values
			    var isValid = true;	
			    isValid &= validateString("Name", thisDialog.find("#add_iso_name"), thisDialog.find("#add_iso_name_errormsg"));				
			    //isValid &= validateFilename("Name", thisDialog.find("#add_iso_name"), thisDialog.find("#add_iso_name_errormsg"));
			    isValid &= validateString("Display Text", thisDialog.find("#add_iso_display_text"), thisDialog.find("#add_iso_display_text_errormsg"));
			    isValid &= validateString("URL", thisDialog.find("#add_iso_url"), thisDialog.find("#add_iso_url_errormsg"));			
			    if (!isValid) 
			        return;		
			        
			    thisDialog.dialog("close");	
			    
			    var array1 = [];
			    var name = trim(thisDialog.find("#add_iso_name").val());
			    array1.push("&name="+todb(name));
			    
			    var desc = trim(thisDialog.find("#add_iso_display_text").val());
			    array1.push("&displayText="+todb(desc));
			    
			    var url = trim(thisDialog.find("#add_iso_url").val());	
			    array1.push("&url="+todb(url));
			    					
			    var zoneId = thisDialog.find("#add_iso_zone").val();
			    array1.push("&zoneId="+zoneId);	
			    
			    var isPublic = thisDialog.find("#add_iso_public").val();
			    array1.push("&isPublic="+isPublic);	
			    		
			    var osType = thisDialog.find("#add_iso_os_type").val();
			    array1.push("&osTypeId="+osType);
			    
			    var bootable = thisDialog.find("#add_iso_bootable").val();	
			    array1.push("&bootable="+bootable);
			    
			    if(thisDialog.find("#isfeatured_container").css("display") != "none") {				
				    var isFeatured = thisDialog.find("#isfeatured").val();						    	
                    array1.push("&isfeatured="+isFeatured);
                }			
    		    				    
		        var $midmenuItem1 = beforeAddingMidMenuItem() ;				    
    		       		    				
			    $.ajax({
			        data: createURL("command=registerIso"+array1.join("")),
				    dataType: "json",
				    success: function(json) {					
				        var items = json.registerisoresponse.iso;				       
				        isoToMidmenu(items[0], $midmenuItem1);
						bindClickToMidMenu($midmenuItem1, isoToRightPanel, isoGetMidmenuId);  
						afterAddingMidMenuItem($midmenuItem1, true);
						                        
                        if(items.length > 1) {                               
                            for(var i=1; i<items.length; i++) {   
                                var $midmenuItem2 = $("#midmenu_item").clone();
                                isoToMidmenu(items[i], $midmenuItem2);
                                bindClickToMidMenu($midmenuItem2, isoToRightPanel, isoGetMidmenuId); 
                                $("#midmenu_container").append($midmenuItem2.show());
                            }                                    
                        }  						
				    }, 
					error: function(XMLHttpResponse) {
						handleError(XMLHttpResponse, function() {
							afterAddingMidMenuItem($midmenuItem1, false, parseXMLHttpResponse(XMLHttpResponse));
						});
					}				
			    });
		    },
		    "Cancel": function() { 
			    $(this).dialog("close"); 
		    } 
	    }).dialog("open");
        return false;
    });
}

function initCreateVmFromIsoDialog() {
    initDialog("dialog_create_vm_from_iso", 450);    
    
    var $dialogCreateVmFromIso = $("#dialog_create_vm_from_iso");
    
    $.ajax({
	    data: createURL("command=listServiceOfferings"),
	    dataType: "json",
	    success: function(json) {
	        var items = json.listserviceofferingsresponse.serviceoffering;
	        if(items != null && items.length > 0 ) {
	            var serviceOfferingField = $dialogCreateVmFromIso.find("#service_offering").empty();
	            for(var i = 0; i < items.length; i++)		        
	                serviceOfferingField.append("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>");
	        }		        
	    }
	});		
	
	$.ajax({
	    data: createURL("command=listDiskOfferings"),
	    dataType: "json",
	    success: function(json) {
	        var items = json.listdiskofferingsresponse.diskoffering;
	        if(items != null && items.length > 0 ) {
	            var diskOfferingField = $dialogCreateVmFromIso.find("#disk_offering").empty();
	            for(var i = 0; i < items.length; i++) {		  
	                var $option = $("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>");	
		            $option.data("jsonObj", items[i]);			                              
	                diskOfferingField.append($option);	            
	            }
	            $dialogCreateVmFromIso.find("#disk_offering").change();
	        }		  
	        
	    }
	});		
    
    $dialogCreateVmFromIso.find("#disk_offering").bind("change", function(event) {  	         
        var jsonObj = $(this).find("option:selected").data("jsonObj");
        if(jsonObj != null && jsonObj.isCustomized == true) { //jsonObj is null when "<option value=''>No disk offering</option>" is selected
            $dialogCreateVmFromIso.find("#size_container").show();
        }
        else {
            $dialogCreateVmFromIso.find("#size_container").hide();  
            $dialogCreateVmFromIso.find("#size").val("");
        }      
    });    
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

function isoToRightPanel($midmenuItem1) {  
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1); 
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);
    isoJsonToDetailsTab();   
}

function isoJsonToDetailsTab() {     
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null)
        return;
    
    var jsonObj = $midmenuItem1.data("jsonObj");
    if(jsonObj == null)
        return;          
    
    var $thisTab = $("#right_panel_content #tab_content_details");  
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();  
         
    $thisTab.find("#grid_header_title").text(fromdb(jsonObj.name));
        
    $thisTab.find("#id").text(fromdb(jsonObj.id));
    $thisTab.find("#zonename").text(fromdb(jsonObj.zonename));
    
    $thisTab.find("#name").text(fromdb(jsonObj.name));
    $thisTab.find("#name_edit").val(fromdb(jsonObj.name));
    
    $thisTab.find("#displaytext").text(fromdb(jsonObj.displaytext));
    $thisTab.find("#displaytext_edit").val(fromdb(jsonObj.displaytext));
    $thisTab.find("#ostypename").text(fromdb(jsonObj.ostypename));
    $thisTab.find("#ostypename_edit").val(fromdb(jsonObj.ostypeid));    
    $thisTab.find("#account").text(fromdb(jsonObj.account));
	$thisTab.find("#domain").text(fromdb(jsonObj.domain));
                      
    var status = "Ready";
	if (jsonObj.isready == false)
		status = fromdb(jsonObj.status);		
	setTemplateStateInRightPanel(status, $thisTab.find("#status"));
	
	if(jsonObj.size != null)
	    $thisTab.find("#size").text(convertBytes(parseInt(jsonObj.size)));  
	else
	    $thisTab.find("#size").text("");    
              
    setBooleanReadField(jsonObj.bootable, $thisTab.find("#bootable"));	
    
    setBooleanReadField(jsonObj.ispublic, $thisTab.find("#ispublic"));	
    setBooleanEditField(jsonObj.ispublic, $thisTab.find("#ispublic_edit"));
    
    setBooleanReadField(jsonObj.isfeatured, $thisTab.find("#isfeatured"));
    setBooleanEditField(jsonObj.isfeatured, $thisTab.find("#isfeatured_edit"));
    
    setBooleanReadField(jsonObj.crossZones, $thisTab.find("#crossZones"));	     
    setDateField(jsonObj.created, $thisTab.find("#created"));	  
    
    
    //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    var noAvailableActions = true;

    // "Edit", "Copy", "Create VM" 
	if ((isUser() && jsonObj.ispublic == true && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account)) || jsonObj.isready == false) {		
		//nothing happens
    }
    else {        
        buildActionLinkForTab("Edit ISO", isoActionMap, $actionMenu, $midmenuItem1, $thisTab);	
        noAvailableActions = false;	
        
        if(jsonObj.id != xsToolsIsoId)
            buildActionLinkForTab("Copy ISO", isoActionMap, $actionMenu, $midmenuItem1, $thisTab);	        
    }
		
	// "Create VM" 
	// Commenting this out for Beta2 as it does not support the new network.
	/*
	if (((isUser() && jsonObj.ispublic == true && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account)) || jsonObj.isready == false) || (jsonObj.bootable == false)) {
	}
    else {        
        buildActionLinkForTab("Create VM", isoActionMap, $actionMenu, $midmenuItem1, $thisTab);	
        noAvailableActions = false;
    }
	*/
    
	// "Delete" 
	if (((isUser() && jsonObj.ispublic == true && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account))) || (jsonObj.isready == false && jsonObj.isostatus != null && jsonObj.isostatus.indexOf("% Downloaded") != -1)) {
	}
	else {	    
	    buildActionLinkForTab("Delete ISO", isoActionMap, $actionMenu, $midmenuItem1, $thisTab);	
	    noAvailableActions = false;
	}    
	
	buildActionLinkForTab("Download ISO", isoActionMap, $actionMenu, $midmenuItem1, $thisTab);	
    noAvailableActions = false;	
	
	// no available actions 
	if(noAvailableActions == true) {
	    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
	}	 
	
	$thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();     
}

function isoClearRightPanel() {
    isoClearDetailsTab(); 
}

function isoClearDetailsTab() {
    var $thisTab = $("#right_panel_content #tab_content_details");   
    
    $thisTab.find("#grid_header_title").text("");
    
    $thisTab.find("#id").text("");
    $thisTab.find("#zonename").text("");
    
    $thisTab.find("#name").text("");
    $thisTab.find("#name_edit").val("");
    
    $thisTab.find("#displaytext").text("");
    $thisTab.find("#displaytext_edit").val("");
    
    $thisTab.find("#ostypename").text("");
    
    $thisTab.find("#account").text("");  
	$thisTab.find("#domain").text("");
	$thisTab.find("#ostypename_edit").val(null);   
    $thisTab.find("#size").text("");  
	$thisTab.find("#status").text(""); 
	$thisTab.find("#bootable").text("");
	$thisTab.find("#ispublic").text("");	
	$thisTab.find("#isfeatured").text("");  
	$thisTab.find("#crossZones").text("");
    $thisTab.find("#created").text("");  
    
     //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
}

var isoActionMap = {  
    "Edit ISO": {
        dialogBeforeActionFn: doEditISO  
    },
    "Delete ISO": {                  
        isAsyncJob: true,
        asyncJobResponse: "deleteisosresponse",
        dialogBeforeActionFn: doDeleteIso,
        inProcessText: "Deleting ISO....",
        afterActionSeccessFn: function(json, $midmenuItem1, id){    
            $midmenuItem1.slideUp("slow", function() {
                $(this).remove();
            });            
            clearRightPanel();
            isoClearRightPanel();
        }        
    },
    "Copy ISO": {
        isAsyncJob: true,
        asyncJobResponse: "copyisoresponse",            
        dialogBeforeActionFn: doCopyIso,
        inProcessText: "Copying ISO....",
        afterActionSeccessFn: function(json, $midmenuItem1, id){}   
    }  
    ,
    "Create VM": {
        isAsyncJob: true,
        asyncJobResponse: "deployvirtualmachineresponse",            
        dialogBeforeActionFn: doCreateVMFromIso,
        inProcessText: "Creating VM....",
        afterActionSeccessFn: function(json, $midmenuItem1, id){}   
    },
    "Download ISO": {               
        dialogBeforeActionFn : doDownloadISO        
    }     
}   

function doEditISO($actionLink, $detailsTab, $midmenuItem1) {     
    var $readonlyFields, $editFields;
  
    if(isAdmin()) {
        $readonlyFields  = $detailsTab.find("#name, #displaytext, #ispublic, #ostypename, #isfeatured");
        $editFields = $detailsTab.find("#name_edit, #displaytext_edit, #ispublic_edit, #ostypename_edit, #isfeatured_edit"); 
    }
    else {  
        $readonlyFields  = $detailsTab.find("#name, #displaytext, #ispublic, #ostypename");
        $editFields = $detailsTab.find("#name_edit, #displaytext_edit, #ispublic_edit, #ostypename_edit"); 
    }
           
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
        doEditISO2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields);   
        return false;
    });   
}

function doEditISO2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields) {     
    // validate values
    var isValid = true;					
    isValid &= validateString("Name", $detailsTab.find("#name_edit"), $detailsTab.find("#name_edit_errormsg"));
    isValid &= validateString("Display Text", $detailsTab.find("#displaytext_edit"), $detailsTab.find("#displaytext_edit_errormsg"));			
    if (!isValid) 
        return;
       
    var jsonObj = $midmenuItem1.data("jsonObj"); 
	var id = jsonObj.id;
	
	var array1 = [];
	array1.push("&id="+id);
							
	var name = $detailsTab.find("#name_edit").val();
	array1.push("&name="+todb(name));
		
	var displaytext = $detailsTab.find("#displaytext_edit").val();
	array1.push("&displayText="+todb(displaytext));
	
	var oldOsTypeId = jsonObj.ostypeid;
	var newOsTypeId = $detailsTab.find("#ostypename_edit").val();
	if(newOsTypeId != oldOsTypeId)
	    array1.push("&ostypeid="+newOsTypeId);
		
	$.ajax({
	    data: createURL("command=updateIso"+array1.join("")),
		dataType: "json",
		async: false,
		success: function(json) {	
		    $detailsTab.find("#name").text(name);
		    $midmenuItem1.find("#first_row").text(name.substring(0,25)); 
		    $detailsTab.find("#displaytext").text(displaytext);		           
		    $detailsTab.find("#ostypename").text($detailsTab.find("#ostypename_edit option:selected").text());				    
		    jsonObj.name = name;
		    jsonObj.displaytext = displaytext;		    
		    jsonObj.ostypeid = parseInt(newOsTypeId);
		    jsonObj.ostypename = $detailsTab.find("#ostypename_edit option:selected").text();    		
		}
	});
	
	//updateIsoPermissions
	var array2 = [];	
	var oldIsPublic = jsonObj.ispublic.toString();	
	var newIsPublic = $detailsTab.find("#ispublic_edit").val();       
	if(newIsPublic != oldIsPublic)
	    array2.push("&ispublic="+newIsPublic);	    						
		    
	var oldIsFeatured = jsonObj.isfeatured.toString();	
	var newIsFeatured = $detailsTab.find("#isfeatured_edit").val();           
    if(newIsFeatured != oldIsFeatured)
        array2.push("&isfeatured="+newIsFeatured);		
								
	if(array2.length > 0) {	
	    $.ajax({
		    data: createURL("command=updateIsoPermissions&id="+id+array2.join("")),
		    dataType: "json",
		    async: false,
		    success: function(json) {			    	        						       					    
		        setBooleanReadField(newIsPublic, $detailsTab.find("#ispublic"));
		        setBooleanReadField(newIsFeatured, $detailsTab.find("#isfeatured"));		       
		        jsonObj.ispublic = (newIsPublic == "true");
		        jsonObj.isfeatured = (newIsFeatured == "true");
    		}
	    });
	}	
	
	$editFields.hide();      
    $readonlyFields.show();       
    $("#save_button, #cancel_button").hide();       
}

function doDeleteIso($actionLink, $detailsTab, $midmenuItem1) {       
    var jsonObj = $midmenuItem1.data("jsonObj");
	var id = jsonObj.id;
	var name = jsonObj.name;			
	var zoneId = jsonObj.zoneid;

    var moreCriteria = [];						
	if (zoneId != null) 
		moreCriteria.push("&zoneid="+zoneId);	
	
	var $dialog1;
	if(jsonObj.crossZones == true)
	    $dialog1 = $("#dialog_confirmation_delete_iso_all_zones");
	else
	    $dialog1 = $("#dialog_confirmation_delete_iso");	
	
	$dialog1	
	.dialog('option', 'buttons', { 					
		"Confirm": function() { 			
			$(this).dialog("close");			
			var apiCommand = "command=deleteIso&id="+id+moreCriteria.join("");
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	
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

function doCopyIso($actionLink, $detailsTab, $midmenuItem1) {   
	var jsonObj = $midmenuItem1.data("jsonObj");
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
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	 
	    }, 
	    "Cancel": function() {				        
		    $(this).dialog("close");
	    }				
	}).dialog("open");	
}	

function doCreateVMFromIso($actionLink, $detailsTab, $midmenuItem1) { 
    var jsonObj = $midmenuItem1.data("jsonObj");	
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
		     if(thisDialog.find("#size_container").css("display") != "none")
			    isValid &= validateNumber("Size", thisDialog.find("#size"), thisDialog.find("#size_errormsg"));				
		    if (!isValid) 
		        return;	       
	           
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
	        
	        if(thisDialog.find("#size_container").css("display") != "none") {
	            var size = thisDialog.find("#size").val()
			    array1.push("&size="+size);
	        }
	        
	        var hypervisor = thisDialog.find("#hypervisor").val();	
	        array1.push("&hypervisor="+hypervisor);	
	                         
		    var apiCommand = "command=deployVirtualMachine&zoneId="+zoneId+"&templateId="+id+array1.join("");
    	    doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
	    }, 
	    "Cancel": function() {
	        $(this).dialog("close");
	    }
	}).dialog("open");			
}	

function doDownloadISO($actionLink, $detailsTab, $midmenuItem1) { 
	var jsonObj = $midmenuItem1.data("jsonObj");
	var id = jsonObj.id;						
	var zoneId = jsonObj.zoneid;	
	
   var apiCommand = "command=extractIso&id="+id+"&zoneid="+zoneId+"&mode=HTTP_DOWNLOAD";
   
   var $dialogDownloadISO = $("#dialog_download_ISO");
   $spinningWheel = $dialogDownloadISO.find("#spinning_wheel");
   $spinningWheel.show();
   var $infoContainer = $dialogDownloadISO.find("#info_container");
   $infoContainer.hide();	
   
   $dialogDownloadISO
	.dialog('option', 'buttons', {	
	    "Close": function() {				        
		    $(this).dialog("close");
	    }				
	}).dialog("open");	
			  
   $.ajax({
       data: createURL(apiCommand),
       dataType: "json",           
       success: function(json) {	                       	                        
           var jobId = json.extractisoresponse.jobid;                  			                        
           var timerKey = "asyncJob_" + jobId;					                       
           $("body").everyTime(
               2000,  //this API returns fast. So, set 2 seconds instead of 10 seconds.
               timerKey,
               function() {
                   $.ajax({
                       data: createURL("command=queryAsyncJobResult&jobId="+jobId),
                       dataType: "json",									                    					                    
                       success: function(json) {		                                                     							                       
	                        var result = json.queryasyncjobresultresponse;										                   
	                        if (result.jobstatus == 0) {
		                        return; //Job has not completed
	                        } else {											                    
		                        $("body").stopTime(timerKey);				                        
		                        $spinningWheel.hide(); 		                 		                          			                                             
		                        if (result.jobstatus == 1) { // Succeeded 			                            
		                            $infoContainer.removeClass("error");
		                            $infoContainer.find("#icon,#info").removeClass("error");		                      
		                            var url = fromdb(json.queryasyncjobresultresponse.jobresult.iso.url);	
		                            var htmlMsg = "Please click <a href='" + url + "'>" + url + "</a>" + " to download ISO";                          
		                            $infoContainer.find("#info").html(htmlMsg);
		                            $infoContainer.show();		                        
		                        } else if (result.jobstatus == 2) { // Failed	
		                            handleErrorInDialog2(fromdb(result.jobresult.errortext), $dialogDownloadISO);		                        
		                        }											                    
	                        }
                       },
                       error: function(XMLHttpResponse) {	                            
	                        $("body").stopTime(timerKey);	
							handleError(XMLHttpResponse, function() {
							    handleErrorInDialog(XMLHttpResponse, $dialogDownloadISO);									
							});
                       }
                   });
               },
               0
           );
       },
       error: function(XMLHttpResponse) {
			handleError(XMLHttpResponse, function() {
				handleErrorInDialog(XMLHttpResponse, $dialogDownloadISO);			
			});
       }
   });  	
}