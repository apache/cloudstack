function networkOfferingGetSearchParams() {
    var moreCriteria = [];	
   
	var searchInput = $("#basic_search").find("#search_input").val();	 
    if (searchInput != null && searchInput.length > 0) {	           
        moreCriteria.push("&keyword="+todb(searchInput));	       
    }     

	var $advancedSearchPopup = getAdvancedSearchPopupInSearchContainer();
	if ($advancedSearchPopup.length > 0 && $advancedSearchPopup.css("display") != "none" ) {	
        var availability = $advancedSearchPopup.find("#adv_search_availability").val();				
	    if (availability!=null && availability.length > 0) 
		    moreCriteria.push("&availability="+todb(availability));	
		
		var traffictype = $advancedSearchPopup.find("#adv_search_traffictype").val();				
	    if (traffictype!=null && traffictype.length > 0) 
		    moreCriteria.push("&traffictype="+todb(traffictype));	        
       
	} 	
		
	return moreCriteria.join("");         
}

function afterLoadNetworkOfferingJSP() {   
    $readonlyFields  = $("#tab_content_details").find("#displaytext, #availability");
    $editFields = $("#tab_content_details").find("#displaytext_edit, #availability_edit");     
}

function doEditNetworkOffering($actionLink, $detailsTab, $midmenuItem1) {       
    $readonlyFields.hide();
    $editFields.show();  
    $detailsTab.find("#cancel_button, #save_button").show();
    
    $detailsTab.find("#cancel_button").unbind("click").bind("click", function(event){    
        cancelEditMode($detailsTab);     
        return false;
    });
    $detailsTab.find("#save_button").unbind("click").bind("click", function(event){        
        doEditNetworkOffering2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields);   
        return false;
    });   
}

function doEditNetworkOffering2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields) {     
    var jsonObj = $midmenuItem1.data("jsonObj");
    var id = jsonObj.id;
    
    // validate values   
    var isValid = true;					
    isValid &= validateString("Display Text", $detailsTab.find("#displaytext_edit"), $detailsTab.find("#displaytext_edit_errormsg"), true);				
    if (!isValid) 
        return;	
     
    var array1 = [];    
       
    var displaytext = $detailsTab.find("#displaytext_edit").val();
    array1.push("&displayText="+todb(displaytext));
	
	var availability = $detailsTab.find("#availability_edit").val();
    array1.push("&availability="+todb(availability));	
	    
	$.ajax({
	    data: createURL("command=updateNetworkOffering&id="+id+array1.join("")),
		dataType: "json",
		success: function(json) {			    
		    var jsonObj = json.updatenetworkofferingresponse.networkoffering;   		    
		    networkOfferingToMidmenu(jsonObj, $midmenuItem1);
		    networkOfferingToRightPanel($midmenuItem1);	
		    
		    $editFields.hide();      
            $readonlyFields.show();       
            $("#save_button, #cancel_button").hide();     	  
		}
	});
}

function networkOfferingToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj); 
     
    /*    
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_system_networkOffering.png");	
    */
   
    var firstRowText = fromdb(jsonObj.name);
    $midmenuItem1.find("#first_row").text(clippedText(firstRowText, midMenuFirstRowLength));     
    $midmenuItem1.find("#first_row_container").attr("title", firstRowText);   
    
    var secondRowText = fromdb(jsonObj.availability);
    $midmenuItem1.find("#second_row").text(clippedText(secondRowText, midMenuSecondRowLength));
    $midmenuItem1.find("#second_row_container").attr("title", secondRowText); 
}

function networkOfferingToRightPanel($midmenuItem1) {
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);
    networkOfferingJsonToDetailsTab();   
}

function networkOfferingJsonToDetailsTab() { 
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null) {
        networkOfferingClearDetailsTab();
        return;
    }
    
    var jsonObj = $midmenuItem1.data("jsonObj");
    if(jsonObj == null) {
        networkOfferingClearDetailsTab();
        return;
    }
     
    var $thisTab = $("#right_panel_content #tab_content_details");  
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   
    
    var id = jsonObj.id;
    
    var jsonObj;   
    $.ajax({
        data: createURL("command=listNetworkOfferings&id="+id),
        dataType: "json",
        async: false,
        success: function(json) {  
            var items = json.listnetworkofferingsresponse.networkoffering;
            if(items != null && items.length > 0) {
                jsonObj = items[0];
                $midmenuItem1.data("jsonObj", jsonObj);  
            }
        }
    });       
    
    $thisTab.find("#id").text(fromdb(jsonObj.id));
        
    $thisTab.find("#grid_header_title").text(fromdb(jsonObj.name));
    $thisTab.find("#name").text(fromdb(jsonObj.name));
        
    $thisTab.find("#displaytext").text(fromdb(jsonObj.displaytext));
    $thisTab.find("#displaytext_edit").val(fromdb(jsonObj.displaytext));
     
    $thisTab.find("#availability").text(fromdb(jsonObj.availability));     
    $thisTab.find("#availability_edit").val(fromdb(jsonObj.availability)); 
         
    setBooleanReadField(jsonObj.redundantrouter, $thisTab.find("#redundantrouter"));	    
    setBooleanReadField(jsonObj.isdefault, $thisTab.find("#isdefault"));
    setBooleanReadField(jsonObj.specifyvlan, $thisTab.find("#specifyvlan"));
      
	var networkRate = jsonObj.networkrate;
	if (networkRate == undefined || networkRate == -1) {
		$thisTab.find("#rate").text(dictionary["label.unlimited"]);
	} else {
		$thisTab.find("#rate").text(fromdb(networkRate) + " Mb/s");
	}
    $thisTab.find("#traffictype").text(fromdb(jsonObj.traffictype));
   
    //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();    
    buildActionLinkForTab("label.action.edit.network.offering", networkOfferingActionMap, $actionMenu, $midmenuItem1, $thisTab);	  
    
    $thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();         
}

function networkOfferingClearRightPanel() {
    networkOfferingClearDetailsTab();
}

function networkOfferingClearDetailsTab() {
    var $thisTab = $("#right_panel_content").find("#tab_content_details");     
    $thisTab.find("#id").text("");    
    $thisTab.find("#grid_header_title").text("");
    $thisTab.find("#name").text("");   
    $thisTab.find("#displaytext").text("");
    $thisTab.find("#displaytext_edit").val("");   
    $thisTab.find("#redundantrouter").text("");
    $thisTab.find("#disksize").text("");
    $thisTab.find("#tags").text("");   
    $thisTab.find("#domain").text("");   
    
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty(); 
    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
}

var networkOfferingActionMap = {   
    "label.action.edit.network.offering": {
        dialogBeforeActionFn: doEditNetworkOffering
    }
}  
