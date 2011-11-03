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

function snapshotGetSearchParams() {
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
    	
		if ($advancedSearchPopup.find("#adv_search_account_li").css("display") != "none" 
    	    && $advancedSearchPopup.find("#adv_search_account").hasClass("textwatermark") == false) {
		    var account = $advancedSearchPopup.find("#adv_search_account").val();		
		    if (account!=null && account.length > 0) 
			    moreCriteria.push("&account="+account);		
		}	
	} 
		
	return moreCriteria.join("");          
}

function afterLoadSnapshotJSP() {    
    initDialog("dialog_add_volume_from_snapshot");  
    
    /*
    $.ajax({
        data: createURL("command=listDiskOfferings"),
	    dataType: "json",
	    success: function(json) {			    
	        var offerings = json.listdiskofferingsresponse.diskoffering;								
		    var diskOfferingDropdown = $("#dialog_add_volume_from_snapshot").find("#diskoffering_dropdown").empty();	
		    if (offerings != null && offerings.length > 0) {								
				for (var i = 0; i < offerings.length; i++) {
				    if(offerings[i].iscustomized ==	true) {
					    var $option = $("<option value='" + offerings[i].id + "'>" + fromdb(offerings[i].displaytext) + "</option>");	
					    $option.data("jsonObj", offerings[i]);	
					    diskOfferingDropdown.append($option); 
					}
				}					
			}	
	    }
    });	 
    */
    
    initCreateTemplateFromSnapshotDialog();
}

function initCreateTemplateFromSnapshotDialog() {
	if (getUserPublicTemplateEnabled() == "true" || isAdmin()) {
		$("#dialog_create_template_from_snapshot #create_template_public_container").show();
	}
	
    initDialog("dialog_create_template_from_snapshot", 450);  
    
    var $dialogCreateTemplateFromSnapshot = $("#dialog_create_template_from_snapshot");
        
    $.ajax({
        data: createURL("command=listOsTypes"),
	    dataType: "json",
	    success: function(json) {
		    types = json.listostypesresponse.ostype;
		    if (types != null && types.length > 0) {
			    var osTypeField = $dialogCreateTemplateFromSnapshot.find("#os_type").empty();	
			    for (var i = 0; i < types.length; i++) {
				    var html = "<option value='" + types[i].id + "'>" + types[i].description + "</option>";
				    osTypeField.append(html);						
			    }
		    }	
	    }
    });	
    
    if(isAdmin())
	    $dialogCreateTemplateFromSnapshot.find("#isfeatured_container").show();
	else
	    $dialogCreateTemplateFromSnapshot.find("#isfeatured_container").hide();		
}

function snapshotToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", getMidmenuId(jsonObj)); 
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_storage_snapshots.png");		
   
    var firstRowText = fromdb(jsonObj.name);
    $midmenuItem1.find("#first_row").text(clippedText(firstRowText, midMenuFirstRowLength));     
    $midmenuItem1.find("#first_row_container").attr("title", firstRowText);   
    
    var secondRowText = fromdb(jsonObj.volumename);
    $midmenuItem1.find("#second_row").text(clippedText(secondRowText, midMenuSecondRowLength));
    $midmenuItem1.find("#second_row_container").attr("title", secondRowText);   
}

function snapshotToRightPanel($midmenuItem1) {    
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);
    snapshotJsonToDetailsTab();   
}

function snapshotJsonToDetailsTab() { 
    var $thisTab = $("#right_panel_content").find("#tab_content_details");             
    
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null)
        return;
        
    var jsonObj = $midmenuItem1.data("jsonObj");
    if(jsonObj == null)
        return;
    
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show(); 
    
    var id = jsonObj.id;
        
    $.ajax({
        data: createURL("command=listSnapshots&id="+id),
        dataType: "json",
        async: false,
        success: function(json) {  
            var items = json.listsnapshotsresponse.snapshot;
            if(items != null && items.length > 0) {
                jsonObj = items[0];
                $midmenuItem1.data("jsonObj", jsonObj);                  
            }
        }
    });        
     
    $thisTab.find("#id").text(fromdb(jsonObj.id));
    $thisTab.find("#name").text(fromdb(jsonObj.name));     
    $thisTab.find("#volume_name").text(fromdb(jsonObj.volumename));
    $thisTab.find("#state").text(fromdb(jsonObj.state));   
    $thisTab.find("#interval_type").text(fromdb(jsonObj.intervaltype));
    $thisTab.find("#account").text(fromdb(jsonObj.account));
    $thisTab.find("#domain").text(fromdb(jsonObj.domain));      
    setDateField(jsonObj.created, $thisTab.find("#created"));	
    
    //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();  
    
    if(jsonObj.state == "BackedUp") {
	    buildActionLinkForTab("label.action.create.volume"  , snapshotActionMap, $actionMenu, $midmenuItem1, $thisTab);	    
	    buildActionLinkForTab("label.action.create.template", snapshotActionMap, $actionMenu, $midmenuItem1, $thisTab);	
    }
    buildActionLinkForTab("label.action.delete.snapshot", snapshotActionMap, $actionMenu, $midmenuItem1, $thisTab);	
    
    $thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();     				
}

function snapshotClearRightPanel() {
    snapshotClearDetailsTab();
}

function snapshotClearDetailsTab() {
    var $thisTab = $("#right_panel_content #tab_content_details");   
    $thisTab.find("#id").text("");
    $thisTab.find("#name").text("");
    $thisTab.find("#volume_name").text("");    
    $thisTab.find("#state").text("");
    $thisTab.find("#interval_type").text("");
    $thisTab.find("#account").text("");
    $thisTab.find("#domain").text("");      
    $thisTab.find("#created").text("");   
}

var snapshotActionMap = {  
    "label.action.create.volume": {              
        isAsyncJob: true,
        asyncJobResponse: "createvolumeresponse",
        dialogBeforeActionFn : doCreateVolumeFromSnapshotInSnapshotPage,
        inProcessText: "label.action.create.volume.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id){}
    }   
    , 
    "label.action.delete.snapshot": {      
        isAsyncJob: true,
        asyncJobResponse: "deletesnapshotresponse",    
		dialogBeforeActionFn : doSnapshotDelete,
        inProcessText: "label.action.delete.snapshot.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id){   
    		$midmenuItem1.remove();                                   
            if(id.toString() == $("#right_panel_content").find("#tab_content_details").find("#id").text()) {
                clearRightPanel();
                snapshotClearRightPanel();
            }                      
        }
    } 
    ,
    "label.action.create.template": {              
        isAsyncJob: true,
        asyncJobResponse: "createtemplateresponse",
        dialogBeforeActionFn : doCreateTemplateFromSnapshotInSnapshotPage,
        inProcessText: "label.action.create.template.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id){}
    }
}   

function doSnapshotDelete($actionLink, $thisTab, $midmenuItem1) {
	$("#dialog_confirmation")	
	.text(dictionary["message.action.delete.snapshot"])
    .dialog('option', 'buttons', { 						
	    "Confirm": function() { 
		    $(this).dialog("close"); 	
			var id = $midmenuItem1.data("jsonObj").id;
			var apiCommand = "command=deleteSnapshot&id="+id;                      
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $thisTab); 
	    }, 
	    "Cancel": function() { 
		    $(this).dialog("close"); 
			
	    } 
    }).dialog("open");
}

function doCreateVolumeFromSnapshotInSnapshotPage($actionLink, $detailsTab, $midmenuItem1){ 
    var jsonObj = $midmenuItem1.data("jsonObj");
       
    $("#dialog_add_volume_from_snapshot")
    .dialog("option", "buttons", {	                    
     "Add": function() {	
         var $thisDialog = $(this);	 
                                        
         var isValid = true;					
         isValid &= validateString("Name", $thisDialog.find("#name"), $thisDialog.find("#name_errormsg"));	
         //isValid &= validateDropDownBox("Disk Offering", $thisDialog.find("#diskoffering_dropdown"), $thisDialog.find("#diskoffering_dropdown_errormsg"));					          		
         if (!isValid) return;   
         
         $thisDialog.dialog("close");       	                                             
         
         var name = $thisDialog.find("#name").val();	  
         //var diskofferingId = $thisDialog.find("#diskoffering_dropdown").val();	
		    
         var id = jsonObj.id;
         //var apiCommand = "command=createVolume&snapshotid="+id+"&name="+name+"&diskOfferingId="+diskofferingId;
         var apiCommand = "command=createVolume&snapshotid="+id+"&name="+name;
    	 doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
     },
     "Cancel": function() {	                         
         $(this).dialog("close");
     }
    }).dialog("open");     
}

function doCreateTemplateFromSnapshotInSnapshotPage($actionLink, $detailsTab, $midmenuItem1){ 
    var jsonObj = $midmenuItem1.data("jsonObj");
       
    $("#dialog_create_template_from_snapshot")
    .dialog("option", "buttons", {
     "Add": function() {	
         var thisDialog = $(this);	 	                                                                        
         var isValid = true;					
         isValid &= validateString("Name", thisDialog.find("#name"), thisDialog.find("#name_errormsg"), false);		
         isValid &= validateString("Display Text", thisDialog.find("#display_text"), thisDialog.find("#display_text_errormsg"), false);				         		          		
         if (!isValid) return;                  	                                             
         
         thisDialog.dialog("close");	
         
         var array1 = [];
         var name = thisDialog.find("#name").val();	 
         array1.push("&name="+name);
         
         var displayText = thisDialog.find("#display_text").val();	 
         array1.push("&displaytext="+displayText);
         
         var osTypeId = thisDialog.find("#os_type").val(); 	  
         array1.push("&ostypeid="+osTypeId);
         
         var isPublic = thisDialog.find("#ispublic").val();
         array1.push("&isPublic="+isPublic);
         
         var password = thisDialog.find("#password").val();	       
         array1.push("&passwordEnabled="+password);                                 
     
         if(thisDialog.find("#isfeatured_container").css("display")!="none") {				
		    var isFeatured = thisDialog.find("#isfeatured").val();						    	
            array1.push("&isfeatured="+isFeatured);
        }	
       
         var id = jsonObj.id;
         var apiCommand = "command=createTemplate&snapshotid="+id+array1.join("");
    	 doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
     },
     "Cancel": function() {	                         
         $(this).dialog("close");
     }	                     
    }).dialog("open");	     
}