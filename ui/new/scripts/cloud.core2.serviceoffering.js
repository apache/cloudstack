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

function afterLoadServiceOfferingJSP() {
    var $detailsTab = $("#right_panel_content #tab_content_details"); 

    //edit button ***
    var $readonlyFields  = $detailsTab.find("#name, #displaytext, #passwordenabled");
    var $editFields = $detailsTab.find("#name_edit, #displaytext_edit, #passwordenabled_edit"); 
    initializeEditFunction($readonlyFields, $editFields, doUpdateServiceOffering); 
     
    //dialogs
    initDialog("dialog_add_service");
     
    //add button ***
    $("#midmenu_add_link").find("#label").text("Add Service Offering"); 
    $("#midmenu_add_link").show();     
    $("#midmenu_add_link").unbind("click").bind("click", function(event) {      
        var dialogAddService = $("#dialog_add_service");
		
		dialogAddService.find("#add_service_name").val("");
		dialogAddService.find("#add_service_display").val("");
		dialogAddService.find("#add_service_cpucore").val("");
		dialogAddService.find("#add_service_cpu").val("");
		dialogAddService.find("#add_service_memory").val("");
		dialogAddService.find("#add_service_offerha").val("false");
			
		(g_hypervisorType == "kvm")? dialogAddService.find("#add_service_offerha_container").hide():dialogAddService.find("#add_service_offerha_container").show();            
				
		dialogAddService
		.dialog('option', 'buttons', { 				
			"Add": function() { 	
			    var thisDialog = $(this);
							
				// validate values
				var isValid = true;					
				isValid &= validateString("Name", thisDialog.find("#add_service_name"), thisDialog.find("#add_service_name_errormsg"));
				isValid &= validateString("Display Text", thisDialog.find("#add_service_display"), thisDialog.find("#add_service_display_errormsg"));
				isValid &= validateNumber("# of CPU Core", thisDialog.find("#add_service_cpucore"), thisDialog.find("#add_service_cpucore_errormsg"), 1, 1000);		
				isValid &= validateNumber("CPU", thisDialog.find("#add_service_cpu"), thisDialog.find("#add_service_cpu_errormsg"), 100, 100000);		
				isValid &= validateNumber("Memory", thisDialog.find("#add_service_memory"), thisDialog.find("#add_service_memory_errormsg"), 64, 1000000);	
				isValid &= validateString("Tags", thisDialog.find("#add_service_tags"), thisDialog.find("#add_service_tags_errormsg"), true);	//optional							
				if (!isValid) 
				    return;										
				thisDialog.dialog("close");
									
				var $midmenuItem1 = beforeAddingMidMenuItem() ;			
									
				var array1 = [];						
				var name = trim(thisDialog.find("#add_service_name").val());
				array1.push("&name="+todb(name));	
				
				var display = trim(thisDialog.find("#add_service_display").val());
				array1.push("&displayText="+todb(display));	
				
				var storagetype = trim(thisDialog.find("#add_service_storagetype").val());
				array1.push("&storageType="+storagetype);	
				
				var core = trim(thisDialog.find("#add_service_cpucore").val());
				array1.push("&cpuNumber="+core);	
				
				var cpu = trim(thisDialog.find("#add_service_cpu").val());
				array1.push("&cpuSpeed="+cpu);	
				
				var memory = trim(thisDialog.find("#add_service_memory").val());
				array1.push("&memory="+memory);	
					
				var offerha = thisDialog.find("#add_service_offerha").val();	
				array1.push("&offerha="+offerha);								
									
				var networkType = thisDialog.find("#add_service_networktype").val();
				var useVirtualNetwork = (networkType=="direct")? false:true;
				array1.push("&usevirtualnetwork="+useVirtualNetwork);		
				
				var tags = trim(thisDialog.find("#add_service_tags").val());
				if(tags != null && tags.length > 0)
				    array1.push("&tags="+todb(tags));	
				
				$.ajax({
				  data: createURL("command=createServiceOffering"+array1.join("")+"&response=json"),
					dataType: "json",
					success: function(json) {					    				
						var item = json.createserviceofferingresponse;							
						serviceOfferingToMidmenu(item, $midmenuItem1);	
						bindClickToMidMenu($midmenuItem1, serviceOfferingToRightPanel, getMidmenuId);  
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

function doUpdateServiceOffering() {
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
    var offerha = $detailsTab.find("#offerha_edit").val();   
    array1.push("&offerha="+offerha);		
	
	$.ajax({
	    data: createURL("command=updateServiceOffering&id="+id+array1.join("")),
		dataType: "json",
		success: function(json) {	  
		    var jsonObj = json.updateserviceofferingresponse;
		    var $midmenuItem1 = $("#"+getMidmenuId(jsonObj));		  
		    serviceOfferingToMidmenu(jsonObj, $midmenuItem1);
		    serviceOfferingToRightPanel($midmenuItem1);		  
		}
	});
}

function serviceOfferingToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj); 
        
    //var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    //$iconContainer.find("#icon").attr("src", "images/midmenuicon_storage_volume.png");	
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.cpunumber + " x " + convertHz(jsonObj.cpuspeed));  
}

function serviceOfferingToRightPanel($midmenuItem1) {
    serviceOfferingJsonToDetailsTab($midmenuItem1);   
}

function serviceOfferingJsonToDetailsTab($midmenuItem1) { 
    var jsonObj = $midmenuItem1.data("jsonObj");
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);      
    $detailsTab.find("#id").text(jsonObj.id);
    
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#name_edit").val(fromdb(jsonObj.name));
    
    $detailsTab.find("#displaytext").text(fromdb(jsonObj.displaytext));
    $detailsTab.find("#displaytext_edit").val(fromdb(jsonObj.displaytext));
    
    $detailsTab.find("#storagetype").text(jsonObj.storagetype);
    $detailsTab.find("#cpu").text(jsonObj.cpunumber + " x " + convertHz(jsonObj.cpuspeed));
    $detailsTab.find("#memory").text(convertBytes(parseInt(jsonObj.memory)*1024*1024));
    
    setBooleanField(jsonObj.offerha, $detailsTab.find("#offerha"));	
    $detailsTab.find("#offerha_edit").val(jsonObj.offerha);
    
    $detailsTab.find("#networktype").text(toNetworkType(jsonObj.usevirtualnetwork));
    $detailsTab.find("#tags").text(fromdb(jsonObj.tags));   
    setDateField(jsonObj.created, $detailsTab.find("#created"));	
    
    //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();      
    buildActionLinkForDetailsTab("Delete Service Offering", serviceOfferingActionMap, $actionMenu, $midmenuItem1);	
}

function serviceOfferingClearRightPanel() {
    serviceOfferingClearDetailsTab();
}

function serviceOfferingClearDetailsTab() {
    var $detailsTab = $("#right_panel_content #tab_content_details");    
    $detailsTab.find("#id").text("");    
    $detailsTab.find("#name").text("");
    $detailsTab.find("#name_edit").val("");    
    $detailsTab.find("#displaytext").text("");
    $detailsTab.find("#displaytext_edit").val("");    
    $detailsTab.find("#storagetype").text("");
    $detailsTab.find("#cpu").text("");
    $detailsTab.find("#memory").text("");    
    $detailsTab.find("#offerha").text("");
    $detailsTab.find("#offerha_edit").val("");    
    $detailsTab.find("#networktype").text("");
    $detailsTab.find("#tags").text("");  
    $detailsTab.find("#created").text(""); 
    
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty(); 
    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
}

var serviceOfferingActionMap = {     
    "Delete Service Offering": {              
        api: "deleteServiceOffering",     
        isAsyncJob: false,           
        inProcessText: "Deleting service offering....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {              
            $midmenuItem1.remove();
            clearRightPanel();
            serviceOfferingClearRightPanel();
        }
    }    
}  