function primarystorageGetSearchParams() {
    var moreCriteria = [];	
       
	var searchInput = $("#basic_search").find("#search_input").val();	 
    if (searchInput != null && searchInput.length > 0) {	           
        moreCriteria.push("&keyword="+todb(searchInput));	       
    }     

	var $advancedSearchPopup = getAdvancedSearchPopupInSearchContainer();
	if ($advancedSearchPopup.length > 0 && $advancedSearchPopup.css("display") != "none" ) {	       
        var zone = $advancedSearchPopup.find("#adv_search_zone").val();	
	    if (zone!=null && zone.length > 0) 
			moreCriteria.push("&zoneId="+zone);	
		
		if ($advancedSearchPopup.find("#adv_search_pod_li").css("display") != "none") {	
		    var pod = $advancedSearchPopup.find("#adv_search_pod").val();		
	        if (pod!=null && pod.length > 0) 
			    moreCriteria.push("&podId="+pod);
        }
	} 
				
	return moreCriteria.join("");          
}

function primarystorageToMidmenu(jsonObj, $midmenuItem1) {    
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj);      
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show(); 
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_primarystorage.png");    
  
    var firstRowText = fromdb(jsonObj.name);
    $midmenuItem1.find("#first_row").text(clippedText(firstRowText, midMenuFirstRowLength));     
    $midmenuItem1.find("#first_row_container").attr("title", firstRowText);   
    
    var secondRowText = fromdb(jsonObj.ipaddress);
    $midmenuItem1.find("#second_row").text(clippedText(secondRowText, midMenuSecondRowLength));
    $midmenuItem1.find("#second_row_container").attr("title", secondRowText);  
     
    updateHostStateInMidMenu(jsonObj, $midmenuItem1);           
}

function primarystorageToRightPanel($midmenuItem1) {  
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);  
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);        
    primarystorageJsonToDetailsTab();  
}

function afterLoadPrimaryStorageJSP() {     
    //add pool dialog
    initDialog("dialog_add_pool", 400);
    bindEventHandlerToDialogAddPool($("#dialog_add_pool"));	         
    
    primaryStorageRefreshDataBinding();     
}

function primaryStorageRefreshDataBinding() {      
    var $primarystorageNode = $selectedSubMenu.parent(); 
    bindAddPrimaryStorageButton($primarystorageNode);    
}

function primarystorageJsonToDetailsTab() {	
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null) {
        primarystorageClearDetailsTab(); 
        return;
    }
    
    var jsonObj = $midmenuItem1.data("jsonObj");
    if(jsonObj == null) {
        primarystorageClearDetailsTab(); 
        return;         
    }      
    
    $.ajax({
        data: createURL("command=listStoragePools&id="+jsonObj.id),
        dataType: "json",
        async: false,
        success: function(json) {            
            var items = json.liststoragepoolsresponse.storagepool;	           
			if(items != null && items.length > 0) {
                jsonObj = items[0];
                $midmenuItem1.data("jsonObj", jsonObj);       
                updateHostStateInMidMenu(jsonObj, $midmenuItem1);             
            }
        }
    });     
    
    var $thisTab = $("#right_panel_content").find("#tab_content_details");  
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();        
                
    $thisTab.find("#id").text(fromdb(jsonObj.id));
    $thisTab.find("#grid_header_title").text(fromdb(jsonObj.name));
    $thisTab.find("#name").text(fromdb(jsonObj.name));
    
    setHostStateInRightPanel(fromdb(jsonObj.state), $thisTab.find("#state"));
    
     
    //refresh status every 2 seconds until status is not changable any more 
	var timerKey = "refreshPrimarystorageStatus";
	$("body").stopTime(timerKey);  //stop timer used by another middle menu item (i.e. stop timer when clicking on a different middle menu item)		
	if($midmenuItem1.find("#spinning_wheel").css("display") == "none") {
	    if(jsonObj.state in primarystorageChangableStatus) {	    
	        $("body").everyTime(
                5000,
                timerKey,
                function() {              
                    $.ajax({
		                data: createURL("command=listStoragePools&id="+jsonObj.id),
		                dataType: "json",
		                async: false,
		                success: function(json) {  
			                var items = json.liststoragepoolsresponse.storagepool;	   
			                if(items != null && items.length > 0) {
				                jsonObj = items[0]; //override jsonObj declared above				
				                $midmenuItem1.data("jsonObj", jsonObj); 				                            
				                if(!(jsonObj.state in primarystorageChangableStatus)) {
				                    $("body").stopTime(timerKey);					                    
				                    updateHostStateInMidMenu(jsonObj, $midmenuItem1); 			                    
				                    if(jsonObj.id.toString() == $("#right_panel_content").find("#tab_content_details").find("#id").text()) {
				                        setHostStateInRightPanel(jsonObj.state, $thisTab.find("#state"));	
				                        primarystorageBuildActionMenu(jsonObj, $thisTab, $midmenuItem1);	
				                    }					                    
				                }               
	                        }   
		                }
	                });                       	
                }
            );
	    }
	}
    
       
    $thisTab.find("#zonename").text(fromdb(jsonObj.zonename));
    $thisTab.find("#podname").text(fromdb(jsonObj.podname));
    $thisTab.find("#clustername").text(fromdb(jsonObj.clustername));
		
    $thisTab.find("#type").text(fromdb(jsonObj.type));
    
    $thisTab.find("#ipaddress").text(fromdb(jsonObj.ipaddress));
    $thisTab.find("#path").text(fromdb(jsonObj.path));                
	$thisTab.find("#disksizetotal").text(convertBytes(jsonObj.disksizetotal));
	$thisTab.find("#disksizeallocated").text(convertBytes(jsonObj.disksizeallocated));
	
	$thisTab.find("#tags").text(fromdb(jsonObj.tags));   
	$thisTab.find("#tags_edit").val(fromdb(jsonObj.tags));  
	 
	// actions  
    primarystorageBuildActionMenu(jsonObj, $thisTab, $midmenuItem1);
    
    $thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();         
}
    
var primarystorageChangableStatus = {
    "PrepareForMaintenance": 1,
    "CancelMaintenance": 1
}

function primarystorageBuildActionMenu(jsonObj, $thisTab, $midmenuItem1) {  
    var $actionLink = $thisTab.find("#action_link"); 
    bindActionLink($actionLink);
       
    var $actionMenu = $actionLink.find("#action_menu");
    $actionMenu.find("#action_list").empty(); 
    var noAvailableActions = true;
    
    //buildActionLinkForTab("label.action.edit.primary.storage", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab);  //because updateStoragePool API is commented out.
      
    if (jsonObj.state == 'Up' || jsonObj.state == "Connecting") {
		buildActionLinkForTab("label.action.enable.maintenance.mode", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab);  
		noAvailableActions = false;	
	} 
	else if(jsonObj.state == 'Down') {
	    buildActionLinkForTab("label.action.enable.maintenance.mode", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab); 
        buildActionLinkForTab("label.action.delete.primary.storage", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab);  
        noAvailableActions = false;	
        
    }	
	else if(jsonObj.state == "Alert") {	     
	    buildActionLinkForTab("label.action.delete.primary.storage", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab);  
	    noAvailableActions = false;	 
	}	
	else if (jsonObj.state == "ErrorInMaintenance") {
	    buildActionLinkForTab("label.action.enable.maintenance.mode", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab);  
        buildActionLinkForTab("label.action.cancel.maintenance.mode", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab); 
        noAvailableActions = false;	  
    }
	else if (jsonObj.state == "PrepareForMaintenance") {
	    buildActionLinkForTab("label.action.cancel.maintenance.mode", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab); 
	    noAvailableActions = false;	
    }
	else if (jsonObj.state == "Maintenance") {
	    buildActionLinkForTab("label.action.cancel.maintenance.mode", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab);            	    
        buildActionLinkForTab("label.action.delete.primary.storage", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab);   
        noAvailableActions = false;	       
    }
	else if (jsonObj.state == "Disconnected"){	      	    
        buildActionLinkForTab("label.action.delete.primary.storage", primarystorageActionMap, $actionMenu, $midmenuItem1, $thisTab);  
        noAvailableActions = false;	        
    }
	else {
	    //alert("Unsupported Host State: " + jsonObj.state);
	} 
	
	// no available actions 
	if(noAvailableActions == true) {
	    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
	}	      
}    
       
function primarystorageClearRightPanel() {  
    primarystorageClearDetailsTab();  
}

function primarystorageClearDetailsTab() {	    
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
    "label.action.edit.primary.storage": {
        dialogBeforeActionFn: doEditPrimaryStorage
    },           
    "label.action.enable.maintenance.mode": {              
        isAsyncJob: true,
        asyncJobResponse: "prepareprimarystorageformaintenanceresponse",
        dialogBeforeActionFn: doEnableMaintenanceModeForPrimaryStorage,
        inProcessText: "label.action.enable.maintenance.mode.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {             
            var item = json.queryasyncjobresultresponse.jobresult.storagepool; 
            primarystorageToMidmenu(item, $midmenuItem1);           
        }
    },
    "label.action.cancel.maintenance.mode": {              
        isAsyncJob: true,
        asyncJobResponse: "cancelprimarystoragemaintenanceresponse",
        dialogBeforeActionFn: doCancelMaintenanceModeForPrimaryStorage,
        inProcessText: "label.action.cancel.maintenance.mode.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {       
            var item = json.queryasyncjobresultresponse.jobresult.storagepool;    
            primarystorageToMidmenu(item, $midmenuItem1);        
        }
    },
    "label.action.delete.primary.storage": {              
        isAsyncJob: false,        
        dialogBeforeActionFn: doDeletePrimaryStorage,
        inProcessText: "label.action.delete.primary.storage.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {   
            $midmenuItem1.remove();                             
            if(id.toString() == $("#right_panel_content").find("#tab_content_details").find("#id").text()) {
                clearRightPanel();
                primarystorageClearRightPanel();
            }
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
	array1.push("&tags="+todb(tags));	
	
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
    .text(dictionary["message.action.primarystorage.enable.maintenance.mode"])
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
    .text(dictionary["message.action.cancel.maintenance.mode"])
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
       
    $("#dialog_confirmation")
    .text(dictionary["message.action.delete.primary.storage"])
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

