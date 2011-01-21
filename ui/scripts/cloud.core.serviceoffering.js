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

function serviceOfferingGetSearchParams() {
    var moreCriteria = [];	

	var searchInput = $("#basic_search").find("#search_input").val();	 
    if (searchInput != null && searchInput.length > 0) {	           
        moreCriteria.push("&keyword="+todb(searchInput));	       
    }     

	var $advancedSearchPopup = getAdvancedSearchPopupInSearchContainer();
	if ($advancedSearchPopup.length > 0 && $advancedSearchPopup.css("display") != "none" ) {
        if ($advancedSearchPopup.find("#adv_search_domain_li").css("display") != "none") {		
		    var domainId = $advancedSearchPopup.find("#adv_search_domain").val();		
		    if (domainId!=null && domainId.length > 0) 
			    moreCriteria.push("&domainid="+domainId);	
    	}	    
	} 
		
	return moreCriteria.join("");          
}

function afterLoadServiceOfferingJSP() {
    initAddServiceOfferingDialog();   
    
    $readonlyFields = $("#tab_content_details").find("#name, #displaytext, #offerha, #tags, #domain");    
    $editFields = $("#tab_content_details").find("#name_edit, #displaytext_edit, #offerha_edit, #domain_edit");         
}

function initAddServiceOfferingDialog() {
    initDialog("dialog_add_service");
    
    var $dialogAddService = $("#dialog_add_service");
    $dialogAddService.find("#public_dropdown").unbind("change").bind("change", function(event) {        
        if($(this).val() == "true") {  //public zone
            $dialogAddService.find("#domain_dropdown_container").hide();  
        }
        else {  //private zone
            $dialogAddService.find("#domain_dropdown_container").show();  
        }
        return false;
    });
           
	$.ajax({
	  data: createURL("command=listDomains"),
		dataType: "json",
		async: false,
		success: function(json) {
		    var $domainDropdown1 = $dialogAddService.find("#domain_dropdown").empty();
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
		         
    $("#add_serviceoffering_button").unbind("click").bind("click", function(event) {    
		$dialogAddService.find("#add_service_name").val("");
		$dialogAddService.find("#add_service_display").val("");
		$dialogAddService.find("#add_service_cpucore").val("");
		$dialogAddService.find("#add_service_cpu").val("");
		$dialogAddService.find("#add_service_memory").val("");
		$dialogAddService.find("#add_service_offerha").val("false");
			
		$dialogAddService
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
									
				var tags = thisDialog.find("#add_service_tags").val();
				if(tags != null && tags.length > 0)
				    array1.push("&tags="+todb(tags));	
				
				if(thisDialog.find("#domain_dropdown_container").css("display") != "none") {
	                var domainId = thisDialog.find("#domain_dropdown").val();
	                array1.push("&domainid="+domainId);	
	            }
								
				$.ajax({
				  data: createURL("command=createServiceOffering"+array1.join("")),
					dataType: "json",
					success: function(json) {					    				
						var item = json.createserviceofferingresponse.serviceoffering;							
						serviceOfferingToMidmenu(item, $midmenuItem1);	
						bindClickToMidMenu($midmenuItem1, serviceOfferingToRightPanel, getMidmenuId);  
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

function doEditServiceOffering($actionLink, $detailsTab, $midmenuItem1) {       
    $readonlyFields.hide();
    $editFields.show();  
    $detailsTab.find("#cancel_button, #save_button").show();
    
    $detailsTab.find("#cancel_button").unbind("click").bind("click", function(event){    
        cancelEditMode($detailsTab);         
        return false;
    });
    $detailsTab.find("#save_button").unbind("click").bind("click", function(event){        
        doEditServiceOffering2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields);                 
        return false;
    });   
}

function doEditServiceOffering2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields) {     
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
    var offerha = $detailsTab.find("#offerha_edit").val();   
    array1.push("&offerha="+offerha);		

	var tags = $detailsTab.find("#tags_edit").val();
	array1.push("&tags="+todb(tags));	
	
	var domainid = $detailsTab.find("#domain_edit").val();
	array1.push("&domainid="+todb(domainid));	
	
	$.ajax({
	    data: createURL("command=updateServiceOffering&id="+id+array1.join("")),
		dataType: "json",
		success: function(json) {	  
		    var jsonObj = json.updateserviceofferingresponse.serviceoffering;	
		    serviceOfferingToMidmenu(jsonObj, $midmenuItem1);
		    serviceOfferingToRightPanel($midmenuItem1);		
		    
		    $editFields.hide();      
            $readonlyFields.show();       
            $("#save_button, #cancel_button").hide();     
		}
	});
}

function serviceOfferingToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj); 
        
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_system_serviceoffering.png");	
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.cpunumber + " x " + convertHz(jsonObj.cpuspeed));  
}

function serviceOfferingToRightPanel($midmenuItem1) {
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);
    serviceOfferingJsonToDetailsTab();   
}

function serviceOfferingJsonToDetailsTab() { 
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
          
    $.ajax({
        data: createURL("command=listServiceOfferings&id="+id),
        dataType: "json",
        async: false,
        success: function(json) {  
            var items = json.listserviceofferingsresponse.serviceoffering;            
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
    
    $thisTab.find("#storagetype").text(fromdb(jsonObj.storagetype));
    $thisTab.find("#cpu").text(jsonObj.cpunumber + " x " + convertHz(jsonObj.cpuspeed));
    $thisTab.find("#memory").text(convertBytes(parseInt(jsonObj.memory)*1024*1024));
    
    setBooleanReadField(jsonObj.offerha, $thisTab.find("#offerha"));	
    setBooleanEditField(jsonObj.offerha, $thisTab.find("#offerha_edit"));
    
    $thisTab.find("#tags").text(fromdb(jsonObj.tags)); 
    $thisTab.find("#tags_edit").val(fromdb(jsonObj.tags));
    
    $thisTab.find("#domain").text(fromdb(jsonObj.domain)); 
    $thisTab.find("#domain_edit").val(fromdb(jsonObj.domainid));   
     
    setDateField(jsonObj.created, $thisTab.find("#created"));	
    
    //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();      
    buildActionLinkForTab("Edit Service Offering", serviceOfferingActionMap, $actionMenu, $midmenuItem1, $thisTab);	
    buildActionLinkForTab("Delete Service Offering", serviceOfferingActionMap, $actionMenu, $midmenuItem1, $thisTab);
    
    $thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();    	
}

function serviceOfferingClearRightPanel() {
    serviceOfferingClearDetailsTab();
}

function serviceOfferingClearDetailsTab() {
    var $thisTab = $("#right_panel_content #tab_content_details");    
    $thisTab.find("#id").text("");  
    $thisTab.find("#grid_header_title").text("");   
    $thisTab.find("#name").text("");
    $thisTab.find("#name_edit").val("");    
    $thisTab.find("#displaytext").text("");
    $thisTab.find("#displaytext_edit").val("");    
    $thisTab.find("#storagetype").text("");
    $thisTab.find("#cpu").text("");
    $thisTab.find("#memory").text("");    
    $thisTab.find("#offerha").text("");
    $thisTab.find("#offerha_edit").val("");    
    $thisTab.find("#tags").text("");  
    $thisTab.find("#created").text(""); 
    
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty(); 
    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
}

var serviceOfferingActionMap = {    
    "Edit Service Offering": {
        dialogBeforeActionFn: doEditServiceOffering
    }, 
    "Delete Service Offering": {              
        api: "deleteServiceOffering",     
        isAsyncJob: false,           
        inProcessText: "Deleting service offering....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {
            $midmenuItem1.slideUp("slow", function() {
                $(this).remove();                
                if(id.toString() == $("#right_panel_content").find("#tab_content_details").find("#id").text()) {
                    clearRightPanel();
                    serviceOfferingClearRightPanel();
                }                
            });    
        }
    }    
}  