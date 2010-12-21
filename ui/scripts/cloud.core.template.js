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

//var DomRTemplateId = 1;

var g_zoneIds = []; 
var g_zoneNames = [];	

function templateGetSearchParams() {
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

function afterLoadTemplateJSP() {      
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    
    //add button ***   
	$("#dialog_add_template #add_template_hypervisor").bind("change", function(event) {	      
	    var formatSelect = $("#dialog_add_template #add_template_format").empty();	     
	    var selectedHypervisorType = $(this).val();
	    
	    if(selectedHypervisorType == "XenServer")
	        formatSelect.append("<option value='VHD'>VHD</option>");	    
	    else if(selectedHypervisorType == "VmWare")
	        formatSelect.append("<option value='OVA'>OVA</option>");
	    else if(selectedHypervisorType == "KVM")
	        formatSelect.append("<option value='QCOW2'>QCOW2</option>");
	        
	    return false;
	});		
	$("#dialog_add_template #add_template_hypervisor").change();	
			    
		
	if(isAdmin())
	    $("#dialog_add_template #add_template_featured_container, #dialog_edit_template #edit_template_featured_container").show();
	else
	    $("#dialog_add_template #add_template_featured_container, #dialog_edit_template #edit_template_featured_container").hide();		
	
	//add button ***	   
    $("#add_template_button").unbind("click").bind("click", function(event) {        
        $("#dialog_add_template")
		.dialog('option', 'buttons', { 				
			"Create": function() { 		
			    var thisDialog = $(this);
													
				// validate values
				var isValid = true;					
				isValid &= validateString("Name", thisDialog.find("#add_template_name"), thisDialog.find("#add_template_name_errormsg"));
				//isValid &= validateFilename("Name", thisDialog.find("#add_template_name"), thisDialog.find("#add_template_name_errormsg"));
				isValid &= validateString("Display Text", thisDialog.find("#add_template_display_text"), thisDialog.find("#add_template_display_text_errormsg"));
				isValid &= validateString("URL", thisDialog.find("#add_template_url"), thisDialog.find("#add_template_url_errormsg"));			
				if (!isValid) return;		
				
				thisDialog.dialog("close");
										
				var name = trim(thisDialog.find("#add_template_name").val());
				var desc = trim(thisDialog.find("#add_template_display_text").val());
				var url = trim(thisDialog.find("#add_template_url").val());						
				var zoneId = thisDialog.find("#add_template_zone").val();												
				var format = thisDialog.find("#add_template_format").val();					
				var password = thisDialog.find("#add_template_password").val();		
				var isPublic = thisDialog.find("#add_template_public").val();	                    	
				var osType = thisDialog.find("#add_template_os_type").val();
				var hypervisor = thisDialog.find("#add_template_hypervisor").val();
				
				var moreCriteria = [];				
				if(thisDialog.find("#add_template_featured_container").css("display")!="none") {				
				    var isFeatured = thisDialog.find("#add_template_featured").val();						    	
                    moreCriteria.push("&isfeatured="+isFeatured);
                }					
				
				var $midmenuItem1 = beforeAddingMidMenuItem() ;
												
				$.ajax({
				    data: createURL("command=registerTemplate&name="+todb(name)+"&displayText="+todb(desc)+"&url="+todb(url)+"&zoneid="+zoneId+"&ispublic="+isPublic+moreCriteria.join("")+"&format="+format+"&passwordEnabled="+password+"&osTypeId="+osType+"&hypervisor="+hypervisor+"&response=json"),
					dataType: "json",
					success: function(json) {	
						var items = json.registertemplateresponse.template;				       
				        templateToMidmenu(items[0], $midmenuItem1);
						bindClickToMidMenu($midmenuItem1, templateToRightPanel, templateGetMidmenuId);  
						afterAddingMidMenuItem($midmenuItem1, true);
						                        
                        if(items.length > 1) {                               
                            for(var i=1; i<items.length; i++) {   
                                var $midmenuItem2 = $("#midmenu_item").clone();
                                templateToMidmenu(items[i], $midmenuItem2);
                                bindClickToMidMenu($midmenuItem2, templateToRightPanel, templateGetMidmenuId); 
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
    
    //populate dropdown ***   			
	var addTemplateZoneField = $("#dialog_add_template").find("#add_template_zone");    	
	if (isAdmin())  
		addTemplateZoneField.append("<option value='-1'>All Zones</option>"); 	
    $.ajax({
        data: createURL("command=listZones&available=true"),
	    dataType: "json",
	    success: function(json) {		        
		    var zones = json.listzonesresponse.zone;	 			     			    	
		    if (zones != null && zones.length > 0) {
		        for (var i = 0; i < zones.length; i++) {
			        addTemplateZoneField.append("<option value='" + zones[i].id + "'>" + fromdb(zones[i].name) + "</option>"); 			        
			        g_zoneIds.push(zones[i].id);
			        g_zoneNames.push(zones[i].name);			       
		        }
		    }				    			
	    }
	});	
    
    $.ajax({
	    data: createURL("command=listOsTypes&response=json"),
		dataType: "json",
		success: function(json) {
			types = json.listostypesresponse.ostype;
			if (types != null && types.length > 0) {		
			    var osTypeDropdownAdd = $("#dialog_add_template").find("#add_template_os_type");    
				var osTypeDropdownEdit = $detailsTab.find("#ostypename_edit").empty();
				if(types != null && types.length > 0) {
				    for(var i = 0; i < types.length; i++) {
					    var html = "<option value='" + types[i].id + "'>" + types[i].description + "</option>";
					    osTypeDropdownAdd.append(html);	
					    osTypeDropdownEdit.append(html);					
				    }
				}
			}	
		}
	});
	
	$.ajax({
	    data: createURL("command=listServiceOfferings&response=json"),
	    dataType: "json",
	    success: function(json) {
	        var items = json.listserviceofferingsresponse.serviceoffering;
	        if(items != null && items.length > 0 ) {
	            var serviceOfferingField = $("#dialog_create_vm_from_template").find("#service_offering").empty();
	            for(var i = 0; i < items.length; i++)		        
	                serviceOfferingField.append("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>");
	        }		        
	    }
	});		
	
	$.ajax({
	    data: createURL("command=listDiskOfferings&response=json"),
	    dataType: "json",
	    success: function(json) {
	        var items = json.listdiskofferingsresponse.diskoffering;
	        if(items != null && items.length > 0 ) {
	            var diskOfferingField = $("#dialog_create_vm_from_template").find("#disk_offering").empty();
	            diskOfferingField.append("<option value=''>No disk offering</option>");
	            for(var i = 0; i < items.length; i++) {		
	                var $option = $("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>");	
		            $option.data("jsonObj", items[i]);			                              
	                diskOfferingField.append($option);
	            }
	            $("#dialog_create_vm_from_template").find("#disk_offering").change();
	        }		  
	        
	    }
	});		
		
	$("#dialog_create_vm_from_template").find("#disk_offering").bind("change", function(event) {  	         
        var jsonObj = $(this).find("option:selected").data("jsonObj");
        if(jsonObj != null && jsonObj.isCustomized == true) { //jsonObj is null when "<option value=''>No disk offering</option>" is selected
            $("#dialog_create_vm_from_template").find("#size_container").show();
        }
        else {
            $("#dialog_create_vm_from_template").find("#size_container").hide();  
            $("#dialog_create_vm_from_template").find("#size").val("");
        }      
    });
	
	//initialize dialog box ***
	initDialog("dialog_confirmation_delete_template_all_zones");
    initDialog("dialog_confirmation_delete_template");    
    initDialog("dialog_add_template", 450);	
	initDialog("dialog_copy_template", 300);	
	initDialog("dialog_create_vm_from_template", 300);	
	initDialog("dialog_download_template");
}

function templateGetMidmenuId(jsonObj) {
    return "midmenuItem_" + jsonObj.id + "_" + fromdb(jsonObj.zonename).replace(/\s/g, ""); //remove all spaces in zonename
}

function templateToMidmenu(jsonObj, $midmenuItem1) {    
    var id = templateGetMidmenuId(jsonObj);
    $midmenuItem1.attr("id", id);  
    $midmenuItem1.data("jsonObj", jsonObj); 
       
    var $iconContainer = $midmenuItem1.find("#icon_container").show();
    setIconByOsType(jsonObj.ostypename, $iconContainer.find("#icon"));
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(fromdb(jsonObj.zonename).substring(0,25));   
}

function templateToRightPanel($midmenuItem1) {  
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1); 
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);
    templateJsonToDetailsTab();   
}

function templateJsonToDetailsTab() {   
     var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null)
        return;
    
    var jsonObj = $midmenuItem1.data("jsonObj");
    if(jsonObj == null)
        return;      
    
    var $thisTab = $("#right_panel_content").find("#tab_content_details");  
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();        
             
    $thisTab.find("#grid_header_title").text(fromdb(jsonObj.name)); 
     
    $thisTab.find("#id").text(fromdb(jsonObj.id));
    $thisTab.find("#zonename").text(fromdb(jsonObj.zonename));
    
    $thisTab.find("#name").text(fromdb(jsonObj.name));
    $thisTab.find("#name_edit").val(fromdb(jsonObj.name));
    
    $thisTab.find("#displaytext").text(fromdb(jsonObj.displaytext));
    $thisTab.find("#displaytext_edit").val(fromdb(jsonObj.displaytext));
    
    var status = "Ready";
	if (jsonObj.isready == false) 
		status = fromdb(jsonObj.status);	 
    setTemplateStateInRightPanel(status, $thisTab.find("#status"));
    
    if(jsonObj.size != null)
	    $thisTab.find("#size").text(convertBytes(parseInt(jsonObj.size))); 
	else
	    $thisTab.find("#size").text(""); 
    
    setBooleanReadField(jsonObj.passwordenabled, $thisTab.find("#passwordenabled"));	
    setBooleanEditField(jsonObj.passwordenabled, $thisTab.find("#passwordenabled_edit"));
   
    setBooleanReadField(jsonObj.ispublic, $thisTab.find("#ispublic"));	
    setBooleanEditField(jsonObj.ispublic, $thisTab.find("#ispublic_edit"));
    
    setBooleanReadField(jsonObj.isfeatured, $thisTab.find("#isfeatured"));
    setBooleanEditField(jsonObj.isfeatured, $thisTab.find("#isfeatured_edit"));
    
    setBooleanReadField(jsonObj.crossZones, $thisTab.find("#crossZones"));
    
    $thisTab.find("#ostypename").text(fromdb(jsonObj.ostypename));
    $thisTab.find("#ostypename_edit").val(jsonObj.ostypeid);    
    
    $thisTab.find("#hypervisor").text(fromdb(jsonObj.hypervisor));
    
    $thisTab.find("#account").text(fromdb(jsonObj.account));   
    $thisTab.find("#domain").text(fromdb(jsonObj.domain)); 
    setDateField(jsonObj.created, $thisTab.find("#created"));	
    
    //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    var noAvailableActions = true;
    
    // "Edit Template", "Copy Template", "Create VM"
	if ((isUser() && jsonObj.ispublic == true && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account)) || jsonObj.templatetype == "SYSTEM" || jsonObj.isready == false) {
	    //do nothing	
    }
    else {
        buildActionLinkForTab("Edit Template", templateActionMap, $actionMenu, $midmenuItem1, $thisTab);      
        
        buildActionLinkForTab("Copy Template", templateActionMap, $actionMenu, $midmenuItem1, $thisTab);			
        
        // For Beta2, this simply doesn't work without a network.
		//buildActionLinkForTab("Create VM", templateActionMap, $actionMenu, $midmenuItem1, $thisTab);	 		       
        
        noAvailableActions = false;		
    }
	
	// "Download Template", "Delete Template"	
	if (((isUser() && jsonObj.ispublic == true && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account))) 
		|| (jsonObj.isready == false && jsonObj.templatestatus != null && jsonObj.templatestatus.indexOf("% Downloaded") != -1) || jsonObj.templatetype == "SYSTEM") {
	    //do nothing	
    }
    else {
        buildActionLinkForTab("Download Template", templateActionMap, $actionMenu, $midmenuItem1, $thisTab);
        buildActionLinkForTab("Delete Template", templateActionMap, $actionMenu, $midmenuItem1, $thisTab);
        noAvailableActions = false;	
    }
            
    // no available actions 
	if(noAvailableActions == true) {
	    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
	}	 
	
	$thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();      
}

//setIconByOsType() is shared by template page and ISO page
function setIconByOsType(osType, $field) {
	if (osType == null || osType.length == 0)
		return; 	
	if (osType.match("^CentOS") != null)
		$field.attr("src", "images/midmenuicon_template_centos.png");
	else if (osType.match("^Windows") != null) 
		$field.attr("src", "images/midmenuicon_template_windows.png");
	else 
		$field.attr("src", "images/midmenuicon_template_linux.png");
}

function templateClearRightPanel() {       
    templateClearDetailsTab();
}

function templateClearDetailsTab() {
    var $thisTab = $("#right_panel_content").find("#tab_content_details");   
   
    $thisTab.find("#id").text("");
    $thisTab.find("#zonename").text("");
    
    $thisTab.find("#name").text("");
    $thisTab.find("#name_edit").val("");
    
    $thisTab.find("#displaytext").text("");
    $thisTab.find("#displaytext_edit").val("");
        
	$thisTab.find("#status").text("");    
    
    $thisTab.find("#passwordenabled").text("");
    $thisTab.find("#passwordenabled_edit").val(null);
    
    $thisTab.find("#ispublic").text("");
    $thisTab.find("#ispublic_edit").val(null);
    
    $thisTab.find("#isfeatured").text("");
    $thisTab.find("#isfeatured_edit").val(null);
    
    $thisTab.find("#crossZones").text("");    
    
    $thisTab.find("#ostypename").text("");
    $thisTab.find("#ostypename_edit").val(null);    
    
    $thisTab.find("#account").text("");  
    $thisTab.find("#domain").text(""); 
    
	$thisTab.find("#size").text("");  
    $thisTab.find("#created").text("");      
}

var templateActionMap = {  
    "Edit Template": {
        dialogBeforeActionFn : doEditTemplate  
    },
    "Delete Template": {              
        isAsyncJob: true,
        asyncJobResponse: "deletetemplateresponse",
        dialogBeforeActionFn : doDeleteTemplate,
        inProcessText: "Deleting Template....",
        afterActionSeccessFn: function(json, $midmenuItem1, id){  
            $midmenuItem1.slideUp("slow", function() {
                $(this).remove();
            });              
            clearRightPanel();
            templateClearRightPanel();
        }
    },
    "Copy Template": {
        isAsyncJob: true,
        asyncJobResponse: "copytemplateresponse",            
        dialogBeforeActionFn : doCopyTemplate,
        inProcessText: "Copying Template....",
        afterActionSeccessFn: function(json, $midmenuItem1, id){}   
    }  
    ,
    "Create VM": {
        isAsyncJob: true,
        asyncJobResponse: "deployvirtualmachineresponse",            
        dialogBeforeActionFn : doCreateVMFromTemplate,
        inProcessText: "Creating VM....",
        afterActionSeccessFn: function(json, $midmenuItem1, id){}   
    },
    "Download Template": {               
        dialogBeforeActionFn : doDownloadTemplate        
    }   
}   

function doEditTemplate($actionLink, $detailsTab, $midmenuItem1) {   
    //var $detailsTab = $("#right_panel_content #tab_content_details");  
    var $readonlyFields  = $detailsTab.find("#name, #displaytext, #passwordenabled, #ispublic, #isfeatured, #ostypename");
    var $editFields = $detailsTab.find("#name_edit, #displaytext_edit, #passwordenabled_edit, #ispublic_edit, #isfeatured_edit, #ostypename_edit");    
        
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
        doEditTemplate2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields);   
        return false;
    });   
}

function doEditTemplate2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields) {               
    // validate values
    var isValid = true;					
    isValid &= validateString("Name", $detailsTab.find("#name_edit"), $detailsTab.find("#name_edit_errormsg"));
    isValid &= validateString("Display Text", $detailsTab.find("#displaytext_edit"), $detailsTab.find("#displaytext_edit_errormsg"));			
    if (!isValid) 
        return;					
	
	var jsonObj = $midmenuItem1.data("jsonObj"); 
	var id = jsonObj.id;
	var midmenuId = templateGetMidmenuId(jsonObj);
		
	var array1 = [];
	var oldName = jsonObj.name
	var newName = trim($detailsTab.find("#name_edit").val());
	if(newName != oldName)
	    array1.push("&name="+todb(newName));
	
	var oldDesc = jsonObj.displaytext;
	var newDesc = trim($detailsTab.find("#displaytext_edit").val());	
	if(newDesc != oldDesc)
	    array1.push("&displaytext="+todb(newDesc));
	    
	var oldPasswordEnabled = jsonObj.passwordenabled.toString();	
	var newPasswordEnabled = $detailsTab.find("#passwordenabled_edit").val();     
	if(newPasswordEnabled != oldPasswordEnabled)
	    array1.push("&passwordenabled="+newPasswordEnabled);	
		
	var oldOsTypeId = jsonObj.ostypeid;
	var newOsTypeId = $detailsTab.find("#ostypename_edit").val();
	if(newOsTypeId != oldOsTypeId)
	    array1.push("&ostypeid="+newOsTypeId);
				
	if(array1.length > 0) {	
	    $.ajax({
		    data: createURL("command=updateTemplate&id="+id+array1.join("")),
		    dataType: "json",
		    async: false,
		    success: function(json) {				       
		        $detailsTab.find("#name").text(newName);
		        $midmenuItem1.find("#first_row").text(newName.substring(0,25)); 		        
		        $detailsTab.find("#displaytext").text(newDesc);
		        setBooleanReadField(newPasswordEnabled, $detailsTab.find("#passwordenabled"));		        
		        $detailsTab.find("#ostypename").text($detailsTab.find("#ostypename_edit option:selected").text());		
		        
		        jsonObj.name = newName;
		        jsonObj.displaytext = newDesc;		      
		        jsonObj.passwordenabled = (newPasswordEnabled == "true"); 
		        jsonObj.ostypeid = parseInt(newOsTypeId);
		        jsonObj.ostypename = $detailsTab.find("#ostypename_edit option:selected").text();     
		    }
	    });
	}
		
	//updateTemplatePermissions	
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
		    data: createURL("command=updateTemplatePermissions&id="+id+array2.join("")),
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

function doDeleteTemplate($actionLink, $detailsTab, $midmenuItem1) {   
    var jsonObj = $midmenuItem1.data("jsonObj");
	var id = jsonObj.id;
	var name = jsonObj.name;			
	var zoneId = jsonObj.zoneid;

    var moreCriteria = [];						
	if (zoneId != null) 
		moreCriteria.push("&zoneid="+zoneId);	
	
	var $dialog1;
	if(jsonObj.crossZones == true)
	    $dialog1 = $("#dialog_confirmation_delete_template_all_zones");
	else
	    $dialog1 = $("#dialog_confirmation_delete_template");	
	
	$dialog1		
	.dialog('option', 'buttons', { 					
		"Confirm": function() { 			
			$(this).dialog("close");			
			var apiCommand = "command=deleteTemplate&id="+id+moreCriteria.join("");
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

function doCopyTemplate($actionLink, $detailsTab, $midmenuItem1) { 
	var jsonObj = $midmenuItem1.data("jsonObj");
	var id = jsonObj.id;
	var name = jsonObj.name;						
	var sourceZoneId = jsonObj.zoneid;				
		
	populateZoneFieldExcludeSourceZone($("#dialog_copy_template #copy_template_zone"), sourceZoneId);
	
	$("#dialog_copy_template #copy_template_name_text").text(name);
		
	var sourceZoneName = jsonObj.zonename;
	$("#dialog_copy_template #copy_template_source_zone_text").text(sourceZoneName);
		
	$("#dialog_copy_template")
	.dialog('option', 'buttons', {				    
	    "OK": function() {				       
	        var thisDialog = $(this);
	        thisDialog.dialog("close");		
	        				        
	        var isValid = true;	 
            isValid &= validateDropDownBox("Zone", thisDialog.find("#copy_template_zone"), thisDialog.find("#copy_template_zone_errormsg"), false);  //reset error text		         
	        if (!isValid) return;     
	        				        
	        var destZoneId = thisDialog.find("#copy_template_zone").val();	
	                    		
	        var apiCommand = "command=copyTemplate&id="+id+"&sourcezoneid="+sourceZoneId+"&destzoneid="+destZoneId;
	        doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	
	    }, 
	    "Cancel": function() {				        
		    $(this).dialog("close");
	    }				
	}).dialog("open");			
}

function doCreateVMFromTemplate($actionLink, $detailsTab, $midmenuItem1) { 
    var jsonObj = $midmenuItem1.data("jsonObj");
	var id = jsonObj.id;		
	var name = jsonObj.name;				
	var zoneId = jsonObj.zoneid;		
					
	var createVmDialog = $("#dialog_create_vm_from_template");				
	createVmDialog.find("#p_name").text(name);
		
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
	        if(diskOfferingId != null && diskOfferingId.length > 0)
	            array1.push("&diskOfferingId="+diskOfferingId);	 		    	        
	        
	        if(thisDialog.find("#size_container").css("display") != "none") {
	            var size = thisDialog.find("#size").val()
			    array1.push("&size="+size);
	        }
	        
		    var apiCommand = "command=deployVirtualMachine&zoneId="+zoneId+"&templateId="+id+array1.join("");
    	    doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
	    }, 
	    "Cancel": function() {
	        $(this).dialog("close");
	    }
	}).dialog("open");			
}		

function doDownloadTemplate($actionLink, $detailsTab, $midmenuItem1) { 
	var jsonObj = $midmenuItem1.data("jsonObj");
	var id = jsonObj.id;						
	var zoneId = jsonObj.zoneid;	
	
    var apiCommand = "command=extractTemplate&id="+id+"&zoneid="+zoneId+"&mode=HTTP_DOWNLOAD";
    
    var $dialogDownloadTemplate = $("#dialog_download_template");
    $spinningWheel = $dialogDownloadTemplate.find("#spinning_wheel");
    $spinningWheel.show();
    var $infoContainer = $dialogDownloadTemplate.find("#info_container");
    $infoContainer.hide();	
    
    $dialogDownloadTemplate
	.dialog('option', 'buttons', {	
	    "Close": function() {				        
		    $(this).dialog("close");
	    }				
	}).dialog("open");	
			  
    $.ajax({
        data: createURL(apiCommand),
        dataType: "json",           
        success: function(json) {	                       	                        
            var jobId = json.extracttemplateresponse.jobid;                  			                        
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
		                            var url = decodeURIComponent(json.queryasyncjobresultresponse.jobresult.template.url);	
		                            var htmlMsg = "Please click <a href='" + url + "'>" + url + "</a>" + " to download template";                          
		                            $infoContainer.find("#info").html(htmlMsg);
		                            $infoContainer.show();		                        
		                        } else if (result.jobstatus == 2) { // Failed	
		                            handleErrorInDialog2(fromdb(result.jobresult.errortext), $dialogDownloadTemplate);		                        
		                        }											                    
	                        }
                        },
                        error: function(XMLHttpResponse) {	                            
	                        $("body").stopTime(timerKey);	
							handleError(XMLHttpResponse, function() {
							    handleErrorInDialog(XMLHttpResponse, $dialogDownloadTemplate);									
							});
                        }
                    });
                },
                0
            );
        },
        error: function(XMLHttpResponse) {
			handleError(XMLHttpResponse, function() {
				handleErrorInDialog(XMLHttpResponse, $dialogDownloadTemplate);			
			});
        }
    });    
    //???						
}