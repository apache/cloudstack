function diskOfferingGetSearchParams() {
    var moreCriteria = [];	

	var searchInput = $("#basic_search").find("#search_input").val();	 
    if (searchInput != null && searchInput.length > 0) {	           
        moreCriteria.push("&keyword="+todb(searchInput));	       
    }     

	var $advancedSearchPopup = getAdvancedSearchPopupInSearchContainer();
	if ($advancedSearchPopup.length > 0 && $advancedSearchPopup.css("display") != "none" ) {	        
        if ($advancedSearchPopup.find("#adv_search_domain_li").css("display") != "none"
	        && $advancedSearchPopup.find("#domain").hasClass("textwatermark") == false) {
	        var domainName = $advancedSearchPopup.find("#domain").val();
	        if (domainName != null && domainName.length > 0) { 	
				var domainId;							    
			    if(autoCompleteDomains != null && autoCompleteDomains.length > 0) {									
				    for(var i=0; i < autoCompleteDomains.length; i++) {					        
				      if(fromdb(autoCompleteDomains[i].name).toLowerCase() == domainName.toLowerCase()) {
				          domainId = autoCompleteDomains[i].id;
				          break;	
				      }
			        } 					   			    
			    } 	     	
	            if(domainId == null) { 
			        showError(false, $advancedSearchPopup.find("#domain"), $advancedSearchPopup.find("#domain_errormsg"), g_dictionary["label.not.found"]);
			    }
			    else { //e.g. domainId == 5 (number)
			        showError(true, $advancedSearchPopup.find("#domain"), $advancedSearchPopup.find("#domain_errormsg"), null)
			        moreCriteria.push("&domainid="+todb(domainId));	
			    }
			}
	    }           
	} 
		
	return moreCriteria.join("");          
}

function afterLoadDiskOfferingJSP() {    
    initAddDiskOfferingDialog();   
    
    $readonlyFields  = $("#tab_content_details").find("#name, #displaytext");
    $editFields = $("#tab_content_details").find("#name_edit, #displaytext_edit");       
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
            $dialogAddDisk.find("#domain_container").hide();  
        }
        else {  //private zone
            $dialogAddDisk.find("#domain_container").show();  
        }
        return false;
    });
    
    applyAutoCompleteToDomainField($dialogAddDisk.find("#domain"));   
           
    $("#add_diskoffering_button").unbind("click").bind("click", function(event) {    
		$dialogAddDisk.find("#disk_name").val("");
		$dialogAddDisk.find("#disk_description").val("");
		$dialogAddDisk.find("#disk_disksize").val("");	
		var submenuContent = $("#submenu_content_disk");
				
		$dialogAddDisk
		.dialog('option', 'buttons', { 				
			"Add": function() { 
			    var $thisDialog = $(this);
												    		
				// validate values
				var isValid = true;					
				isValid &= validateString("Name", $thisDialog.find("#add_disk_name"), $thisDialog.find("#add_disk_name_errormsg"));
				isValid &= validateString("Description", $thisDialog.find("#add_disk_description"), $thisDialog.find("#add_disk_description_errormsg"));
				
				if($("#add_disk_disksize_container").css("display") != "none")
				    isValid &= validateInteger("Disk size", $thisDialog.find("#add_disk_disksize"), $thisDialog.find("#add_disk_disksize_errormsg"), 0, null, false); //required
				
				if($thisDialog.find("#domain_container").css("display") != "none") {
				    isValid &= validateString("Domain", $thisDialog.find("#domain"), $thisDialog.find("#domain_errormsg"), false);                             //required	
				    var domainName = $thisDialog.find("#domain").val();
				    var domainId;
				    if(domainName != null && domainName.length > 0) { 				    
				        if(autoCompleteDomains != null && autoCompleteDomains.length > 0) {									
					        for(var i=0; i < autoCompleteDomains.length; i++) {					        
					          if(fromdb(autoCompleteDomains[i].name).toLowerCase() == domainName.toLowerCase()) {
					              domainId = autoCompleteDomains[i].id;
					              break;	
					          }
				            } 					   			    
				        }					    				    
				        if(domainId == null) {
				            showError(false, $thisDialog.find("#domain"), $thisDialog.find("#domain_errormsg"), g_dictionary["label.not.found"]);
				            isValid &= false;
				        }				    
				    }				
				}
								
				isValid &= validateString("Tags", $thisDialog.find("#add_disk_tags"), $thisDialog.find("#add_disk_tags_errormsg"), true);	//optional	
				if (!isValid) 
				    return;		
				$thisDialog.dialog("close");
				    
				var $midmenuItem1 = beforeAddingMidMenuItem() ;		
			
				var array1 = [];					
				var name = $thisDialog.find("#add_disk_name").val();
				array1.push("&name="+todb(name));
				
				var description = $thisDialog.find("#add_disk_description").val();	
				array1.push("&displaytext="+todb(description));
				
				var customized = $thisDialog.find("#customized").val();				
				array1.push("&customized="+customized);
				
				if($("#add_disk_disksize_container").css("display") != "none") {		
				    var disksize = $thisDialog.find("#add_disk_disksize").val();
				    array1.push("&disksize="+disksize);
				}
				
				var tags = $thisDialog.find("#add_disk_tags").val();
				if(tags != null && tags.length > 0)
				    array1.push("&tags="+todb(tags));								
				
				if($thisDialog.find("#domain_container").css("display") != "none") {                
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
    $readonlyFields.hide();
    $editFields.show();  
    $detailsTab.find("#cancel_button, #save_button").show();
    
    $detailsTab.find("#cancel_button").unbind("click").bind("click", function(event){    
        cancelEditMode($detailsTab);       
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

function doDeleteDiskOffering($actionLink, $detailsTab, $midmenuItem1) {       
    var jsonObj = $midmenuItem1.data("jsonObj");
	var id = jsonObj.id;
		
	$("#dialog_confirmation")
	.text(dictionary["message.action.delete.disk.offering"])
	.dialog('option', 'buttons', { 					
		"Confirm": function() { 			
			$(this).dialog("close");			
			var apiCommand = "command=deleteDiskOffering&id="+id;
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		}
	}).dialog("open");
}

function diskOfferingToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj); 
        
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_system_diskoffering.png");	
    
    var firstRowText = fromdb(jsonObj.name);
    $midmenuItem1.find("#first_row").text(clippedText(firstRowText, midMenuFirstRowLength));     
    $midmenuItem1.find("#first_row_container").attr("title", firstRowText);   
    
    var secondRowText = fromdb(jsonObj.displaytext);
    $midmenuItem1.find("#second_row").text(clippedText(secondRowText, midMenuSecondRowLength));
    $midmenuItem1.find("#second_row_container").attr("title", secondRowText); 
}

function diskOfferingToRightPanel($midmenuItem1) {
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);
    diskOfferingJsonToDetailsTab();   
}

function diskOfferingJsonToDetailsTab() { 
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null) {
        diskOfferingClearDetailsTab();
        return;
    }
    
    var jsonObj = $midmenuItem1.data("jsonObj");
    if(jsonObj == null) {
        diskOfferingClearDetailsTab();
        return;
    }
     
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
    buildActionLinkForTab("label.action.edit.disk.offering", diskOfferingActionMap, $actionMenu, $midmenuItem1, $thisTab);	  
    buildActionLinkForTab("label.action.delete.disk.offering", diskOfferingActionMap, $actionMenu, $midmenuItem1, $thisTab);	  
    
    $thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();         
}

function diskofferingGetDiskSize(jsonObj) {
    var diskSize;
    if(jsonObj.disksize == 0 && jsonObj.isCustomized == true)
        diskSize = "custom size (during VM creation or volume creation)";
    else
        diskSize = jsonObj.disksize + " GB";
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
    "label.action.edit.disk.offering": {
        dialogBeforeActionFn: doEditDiskOffering
    },   
    "label.action.delete.disk.offering": {              
        api: "deleteDiskOffering",     
        isAsyncJob: false,   
        dialogBeforeActionFn : doDeleteDiskOffering,              
        inProcessText: "label.action.delete.disk.offering.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {   
    		$midmenuItem1.remove();   
            if(id.toString() == $("#right_panel_content").find("#tab_content_details").find("#id").text()) {
                clearRightPanel();
                diskOfferingClearRightPanel();
            }      
        }
    }    
}  
