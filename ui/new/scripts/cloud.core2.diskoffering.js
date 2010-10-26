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

function afterLoadDiskOfferingJSP() {
    var $detailsTab = $("#right_panel_content #tab_content_details"); 

    //edit button ***
    var $readonlyFields  = $detailsTab.find("#name, #displaytext");
    var $editFields = $detailsTab.find("#name_edit, #displaytext_edit"); 
    initializeEditFunction($readonlyFields, $editFields, doUpdateDiskOffering);   
    
    //dialogs
    initDialog("dialog_add_disk");
    
    //add button ***
    $("#midmenu_add_link").find("#label").text("Add Disk Offering"); 
    $("#midmenu_add_link").show();     
    $("#midmenu_add_link").unbind("click").bind("click", function(event) {    
		var dialogAddDisk = $("#dialog_add_disk");
		dialogAddDisk.find("#disk_name").val("");
		dialogAddDisk.find("#disk_description").val("");
		dialogAddDisk.find("#disk_disksize").val("");	
		var submenuContent = $("#submenu_content_disk");
				
		dialogAddDisk
		.dialog('option', 'buttons', { 				
			"Add": function() { 
			    var thisDialog = $(this);
								    		
				// validate values
				var isValid = true;					
				isValid &= validateString("Name", thisDialog.find("#add_disk_name"), thisDialog.find("#add_disk_name_errormsg"));
				isValid &= validateString("Description", thisDialog.find("#add_disk_description"), thisDialog.find("#add_disk_description_errormsg"));
				isValid &= validateNumber("Disk size", thisDialog.find("#add_disk_disksize"), thisDialog.find("#add_disk_disksize_errormsg"), 0, null); 
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
							
				var disksize = trim(thisDialog.find("#add_disk_disksize").val());
				array1.push("&disksize="+disksize);
				
				var tags = trim(thisDialog.find("#add_disk_tags").val());
				if(tags != null && tags.length > 0)
				    array1.push("&tags="+todb(tags));								
				
				$.ajax({
				  data: createURL("command=createDiskOffering&isMirrored=false&response=json" + array1.join("")),
					dataType: "json",
					success: function(json) {						    
					    var item = json.creatediskofferingresponse;							
						diskOfferingToMidmenu(item, $midmenuItem1);	
						bindClickToMidMenu($midmenuItem1, diskOfferingToRightPanel, getMidmenuId);  
						afterAddingMidMenuItem($midmenuItem1, true);						
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
}

function doUpdateDiskOffering() {
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    var jsonObj = $detailsTab.data("jsonObj");
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
	
	$.ajax({
	    data: createURL("command=updateDiskOffering&id="+id+array1.join("")),
		dataType: "json",
		success: function(json) {	
		    //call listDiskOffering before bug 6502(What updateDiskOffering API returns should include an embedded object) is fixed.
		    var jsonObj;		   
		    $.ajax({
		        data: createURL("command=listDiskOfferings&id="+id),
		        dataType: "json",
		        async: false,
		        success: function(json) {		            
		            jsonObj = json.listdiskofferingsresponse.diskoffering[0];
		        }
		    });		   
		    var $midmenuItem1 = $("#"+getMidmenuId(jsonObj));		   
		    diskOfferingToMidmenu(jsonObj, $midmenuItem1);
		    diskOfferingToRightPanel($midmenuItem1);		  
		}
	});
}

function diskOfferingToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj); 
        
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_system_diskoffering.png");	
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(convertBytes(jsonObj.disksize));  
}

function diskOfferingToRightPanel($midmenuItem1) {
    copyAfterActionInfoToRightPanel($midmenuItem1);
    diskOfferingJsonToDetailsTab($midmenuItem1);   
}

function diskOfferingJsonToDetailsTab($midmenuItem1) { 
    var jsonObj = $midmenuItem1.data("jsonObj");
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);      
    $detailsTab.find("#id").text(jsonObj.id);
    
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#name_edit").val(fromdb(jsonObj.name));
    
    $detailsTab.find("#displaytext").text(fromdb(jsonObj.displaytext));
    $detailsTab.find("#displaytext_edit").val(fromdb(jsonObj.displaytext));
    
    $detailsTab.find("#disksize").text(convertBytes(jsonObj.disksize));
    $detailsTab.find("#tags").text(fromdb(jsonObj.tags));   
    $detailsTab.find("#domain").text(fromdb(jsonObj.domain));   
    
    //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();    
    buildActionLinkForDetailsTab("Delete Disk Offering", diskOfferingActionMap, $actionMenu, $midmenuItem1, $detailsTab);	    
}

function diskOfferingClearRightPanel() {
    diskOfferingClearDetailsTab();
}

function diskOfferingClearDetailsTab() {
    var $detailsTab = $("#right_panel_content #tab_content_details");     
    $detailsTab.find("#id").text("");    
    $detailsTab.find("#name").text("");
    $detailsTab.find("#name_edit").val("");    
    $detailsTab.find("#displaytext").text("");
    $detailsTab.find("#displaytext_edit").val("");    
    $detailsTab.find("#disksize").text("");
    $detailsTab.find("#tags").text("");   
    $detailsTab.find("#domain").text("");   
    
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty(); 
    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
}

var diskOfferingActionMap = {     
    "Delete Disk Offering": {              
        api: "deleteDiskOffering",     
        isAsyncJob: false,           
        inProcessText: "Deleting disk offering....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {        
            $midmenuItem1.remove();
            clearRightPanel();
            diskOfferingClearRightPanel();
        }
    }    
}  