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

function diskOfferingGetSearchParams() {
    var moreCriteria = [];	

	var $advancedSearchPopup = $("#advanced_search_popup");
	if (lastSearchType == "advanced_search" && $advancedSearchPopup.length > 0) {
	    var name = $advancedSearchPopup.find("#adv_search_name").val();							
		if (name!=null && trim(name).length > 0) 
			moreCriteria.push("&name="+todb(name));	
        
        if ($advancedSearchPopup.find("#adv_search_domain_li").css("display") != "none") {		
		    var domainId = $advancedSearchPopup.find("#adv_search_domain").val();		
		    if (domainId!=null && domainId.length > 0) 
			    moreCriteria.push("&domainid="+domainId);	
    	}	    
	} 
	else {     			    		
	    var searchInput = $("#basic_search").find("#search_input").val();	 
        if (lastSearchType == "basic_search" && searchInput != null && searchInput.length > 0) {	           
            moreCriteria.push("&name="+todb(searchInput));	       
        }        
	}
	
	return moreCriteria.join("");          
}

function afterLoadDiskOfferingJSP() {    
    initAddDiskOfferingDialog();     
}

function initAddDiskOfferingDialog() { 
    //dialogs
    initDialog("dialog_add_disk");
    
    var $dialogAddDisk = $("#dialog_add_disk");
    $dialogAddDisk.find("#customized").bind("change", function(event) {     
        if($(this).val() == 'false') {
            $dialogAddDisk.find("#add_disk_disksize_container").show();
        }
        else {
            $dialogAddDisk.find("#add_disk_disksize_container").hide();   
            $dialogAddDisk.find("#add_disk_disksize").val(""); 
        }        
        return false;
    });
        
    $dialogAddDisk.find("#public_dropdown").unbind("change").bind("change", function(event) {        
        if($(this).val() == "true") {  //public zone
            $dialogAddDisk.find("#domain_dropdown_container").hide();  
        }
        else {  //private zone
            $dialogAddDisk.find("#domain_dropdown_container").show();  
        }
        return false;
    });
    
    $.ajax({
	  data: createURL("command=listDomains"),
		dataType: "json",
		async: false,
		success: function(json) {
		    var $domainDropdown1 = $dialogAddDisk.find("#domain_dropdown").empty();
		    var $domainDropdown2 = $("#tab_content_details").find("#domain_edit").empty();
			var domains = json.listdomainsresponse.domain;						
			if (domains != null && domains.length > 0) {
				for (var i = 0; i < domains.length; i++) {
					$domainDropdown1.append("<option value='" + fromdb(domains[i].id) + "'>" + fromdb(domains[i].name) + "</option>"); 
					$domainDropdown2.append("<option value='" + fromdb(domains[i].id) + "'>" + fromdb(domains[i].name) + "</option>"); 
				}
			} 
		}
	});   
    
    //add button ***
    $("#midmenu_add_link").find("#label").text("Add Disk Offering"); 
    $("#midmenu_add_link").show();     
    $("#midmenu_add_link").unbind("click").bind("click", function(event) {    
		$dialogAddDisk.find("#disk_name").val("");
		$dialogAddDisk.find("#disk_description").val("");
		$dialogAddDisk.find("#disk_disksize").val("");	
		var submenuContent = $("#submenu_content_disk");
				
		$dialogAddDisk
		.dialog('option', 'buttons', { 				
			"Add": function() { 
			    var thisDialog = $(this);
												    		
				// validate values
				var isValid = true;					
				isValid &= validateString("Name", thisDialog.find("#add_disk_name"), thisDialog.find("#add_disk_name_errormsg"));
				isValid &= validateString("Description", thisDialog.find("#add_disk_description"), thisDialog.find("#add_disk_description_errormsg"));
				
				if($("#add_disk_disksize_container").css("display") != "none")
				    isValid &= validateNumber("Disk size", thisDialog.find("#add_disk_disksize"), thisDialog.find("#add_disk_disksize_errormsg"), 0, null, false); //required
								
				isValid &= validateString("Tags", thisDialog.find("#add_disk_tags"), thisDialog.find("#add_disk_tags_errormsg"), true);	//optional	
				if (!isValid) 
				    return;		
				thisDialog.dialog("close");
				    
				var $midmenuItem1 = beforeAddingMidMenuItem() ;		
			
				var array1 = [];					
				var name = trim(thisDialog.find("#add_disk_name").val());
				array1.push("&name="+todb(name));
				
				var description = trim(thisDialog.find("#add_disk_description").val());	
				array1.push("&displaytext="+todb(description));
				
				var customized = thisDialog.find("#customized").val();				
				array1.push("&customized="+customized);
				
				if($("#add_disk_disksize_container").css("display") != "none") {		
				    var disksize = trim(thisDialog.find("#add_disk_disksize").val());
				    array1.push("&disksize="+disksize);
				}
				
				var tags = trim(thisDialog.find("#add_disk_tags").val());
				if(tags != null && tags.length > 0)
				    array1.push("&tags="+todb(tags));								
				
				if(thisDialog.find("#domain_dropdown_container").css("display") != "none") {
	                var domainId = thisDialog.find("#domain_dropdown").val();
	                array1.push("&domainid="+domainId);	
	            }
					
				$.ajax({
				  data: createURL("command=createDiskOffering&isMirrored=false" + array1.join("")),
					dataType: "json",
					success: function(json) {						    
					    var item = json.creatediskofferingresponse.diskoffering;							
						diskOfferingToMidmenu(item, $midmenuItem1);	
						bindClickToMidMenu($midmenuItem1, diskOfferingToRightPanel, getMidmenuId);  
						afterAddingMidMenuItem($midmenuItem1, true);						
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

function doEditDiskOffering($actionLink, $detailsTab, $midmenuItem1) {       
    var $readonlyFields  = $detailsTab.find("#name, #displaytext, #tags, #domain");
    var $editFields = $detailsTab.find("#name_edit, #displaytext_edit, #tags_edit, #domain_edit"); 
             
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
        doEditDiskOffering2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields);   
        return false;
    });   
}

function doEditDiskOffering2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields) {     
    var jsonObj = $midmenuItem1.data("jsonObj");
    var id = jsonObj.id;
    
    // validate values   
    var isValid = true;					
    isValid &= validateString("Name", $detailsTab.find("#name_edit"), $detailsTab.find("#name_edit_errormsg"), true);		
    isValid &= validateString("Display Text", $detailsTab.find("#displaytext_edit"), $detailsTab.find("#displaytext_edit_errormsg"), true);				
    if (!isValid) 
        return;	
     
    var array1 = [];    
    var name = $detailsTab.find("#name_edit").val();
    array1.push("&name="+todb(name));
    
    var displaytext = $detailsTab.find("#displaytext_edit").val();
    array1.push("&displayText="+todb(displaytext));
	
	var tags = $detailsTab.find("#tags_edit").val();
	array1.push("&tags="+todb(tags));	
	
	var domainid = $detailsTab.find("#domain_edit").val();
	array1.push("&domainid="+todb(domainid));	
	
	$.ajax({
	    data: createURL("command=updateDiskOffering&id="+id+array1.join("")),
		dataType: "json",
		success: function(json) {			    
		    var jsonObj = json.updatediskofferingresponse.diskoffering;   
		    diskOfferingToMidmenu(jsonObj, $midmenuItem1);
		    diskOfferingToRightPanel($midmenuItem1);	
		    
		    $editFields.hide();      
            $readonlyFields.show();       
            $("#save_button, #cancel_button").hide();     	  
		}
	});
}

function diskOfferingToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj); 
        
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_system_diskoffering.png");	
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(fromdb(jsonObj.displaytext).substring(0,25));  
}

function diskOfferingToRightPanel($midmenuItem1) {
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);
    diskOfferingJsonToDetailsTab();   
}

function diskOfferingJsonToDetailsTab() { 
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null)
        return;
    
    var jsonObj = $midmenuItem1.data("jsonObj");
    if(jsonObj == null)
        return;
     
    var $thisTab = $("#right_panel_content #tab_content_details");  
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   
    
    var id = jsonObj.id;
    
    var jsonObj;   
    $.ajax({
        data: createURL("command=listDiskOfferings&id="+id),
        dataType: "json",
        async: false,
        success: function(json) {  
            var items = json.listdiskofferingsresponse.diskoffering;
            if(items != null && items.length > 0) {
                jsonObj = items[0];
                $midmenuItem1.data("jsonObj", jsonObj);  
            }
        }
    });       
    
    $thisTab.find("#id").text(fromdb(jsonObj.id));
    
    $thisTab.find("#grid_header_title").text(fromdb(jsonObj.name));
    $thisTab.find("#name").text(fromdb(jsonObj.name));
    $thisTab.find("#name_edit").val(fromdb(jsonObj.name));
    
    $thisTab.find("#displaytext").text(fromdb(jsonObj.displaytext));
    $thisTab.find("#displaytext_edit").val(fromdb(jsonObj.displaytext));
    
    var diskSize = diskofferingGetDiskSize(jsonObj);   
    $thisTab.find("#disksize").text(diskSize);    
        
    $thisTab.find("#tags").text(fromdb(jsonObj.tags));    
    $thisTab.find("#tags_edit").val(fromdb(jsonObj.tags));    
      
    $thisTab.find("#domain").text(fromdb(jsonObj.domain));   
    $thisTab.find("#domain_edit").val(fromdb(jsonObj.domainid));   
    
    //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();    
    buildActionLinkForTab("Edit Disk Offering", diskOfferingActionMap, $actionMenu, $midmenuItem1, $thisTab);	  
    buildActionLinkForTab("Delete Disk Offering", diskOfferingActionMap, $actionMenu, $midmenuItem1, $thisTab);	  
    
    $thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();         
}

function diskofferingGetDiskSize(jsonObj) {
    var diskSize;
    if(jsonObj.disksize == 0 && jsonObj.isCustomized == true)
        diskSize = "custom size (during VM creation or volume creation)";
    else
        diskSize = convertBytes(jsonObj.disksize * 1024 * 1024);    //unit of jsonObj.disksize is MB.
    return diskSize;
}    

function diskOfferingClearRightPanel() {
    diskOfferingClearDetailsTab();
}

function diskOfferingClearDetailsTab() {
    var $thisTab = $("#right_panel_content").find("#tab_content_details");     
    $thisTab.find("#id").text("");    
    $thisTab.find("#grid_header_title").text("");
    $thisTab.find("#name").text("");
    $thisTab.find("#name_edit").val("");    
    $thisTab.find("#displaytext").text("");
    $thisTab.find("#displaytext_edit").val("");    
    $thisTab.find("#disksize").text("");
    $thisTab.find("#tags").text("");   
    $thisTab.find("#domain").text("");   
    
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty(); 
    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
}

var diskOfferingActionMap = {   
    "Edit Disk Offering": {
        dialogBeforeActionFn: doEditDiskOffering
    },   
    "Delete Disk Offering": {              
        api: "deleteDiskOffering",     
        isAsyncJob: false,           
        inProcessText: "Deleting disk offering....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {   
            $midmenuItem1.slideUp("slow", function() {
                $(this).remove();
            });    
            clearRightPanel();
            diskOfferingClearRightPanel();
        }
    }    
}  