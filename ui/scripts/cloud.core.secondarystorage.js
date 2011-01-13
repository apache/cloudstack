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

function afterLoadSecondaryStorageJSP($midmenuItem1) {
    var $topButtonContainer = clearButtonsOnTop();			    	       
	$("#top_buttons").appendTo($topButtonContainer); 
	initDialog("dialog_add_secondarystorage");   
    secondaryStorageRefreshDataBinding();    	
}

function secondaryStorageRefreshDataBinding() {      
    var $secondaryStorageNode = $selectedSubMenu;     
    secondaryStorageJsonToRightPanel($secondaryStorageNode);		  
}

function secondaryStorageJsonToRightPanel($midmenuItem1) {	    
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);  
    bindAddSecondaryStorageButton($midmenuItem1.data("zoneObj"));   
    secondaryStorageJsonToDetailsTab();    
}

function secondaryStorageJsonToDetailsTab() {	    
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null)
        return;
        
    var zoneObj = $midmenuItem1.data("zoneObj");    
    if(zoneObj == null) 
	    return;	
    
    var $thisTab = $("#right_panel_content").find("#tab_content_details");  
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();                   
     
    var jsonObj;
    $.ajax({
        data: createURL("command=listHosts&type=SecondaryStorage&zoneid="+zoneObj.id),
        dataType: "json",
        async: false,
        success: function(json) {              
            var items = json.listhostsresponse.host;		
			if(items != null && items.length > 0) {
                jsonObj = items[0];
                $midmenuItem1.data("jsonObj", jsonObj);                  
            }
        }
    });    
    
    if(jsonObj == null) {
        secondaryStorageJsonClearRightPanel();  
        $thisTab.find("#tab_spinning_wheel").hide();    
        $thisTab.find("#tab_container").show();      
        return;
    }
    
    $thisTab.find("#id").text(fromdb(jsonObj.id));
    $thisTab.find("#grid_header_title").text(fromdb(jsonObj.name));    
    $thisTab.find("#name").text(fromdb(jsonObj.name));
   
    $thisTab.find("#zonename").text(fromdb(jsonObj.zonename));	
	$thisTab.find("#type").text(jsonObj.type);	
    $thisTab.find("#ipaddress").text(jsonObj.ipaddress);
       
    setHostStateInRightPanel(fromdb(jsonObj.state), $thisTab.find("#state"))
    
    $thisTab.find("#version").text(jsonObj.version); 
    setDateField(jsonObj.disconnected, $thisTab.find("#disconnected"));
       
    //actions ***   
    var $actionLink = $thisTab.find("#action_link"); 
    $actionLink.bind("mouseover", function(event) {	    
        $(this).find("#action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {       
        $(this).find("#action_menu").hide();    
        return false;
    });	  
    var $actionMenu = $thisTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();          
    buildActionLinkForTab("Delete Secondary Storage", secondaryStorageActionMap, $actionMenu, $midmenuItem1, $thisTab);   
    
    $thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();      
}	  

function secondaryStorageJsonClearRightPanel() {
    secondaryStorageJsonClearDetailsTab();      
}

function secondaryStorageJsonClearDetailsTab() {	    
    var $thisTab = $("#right_panel_content").find("#tab_content_details");          
    $thisTab.find("#id").text("");
    $thisTab.find("#grid_header_title").text("");    
    $thisTab.find("#name").text("");   
    $thisTab.find("#zonename").text("");	
	$thisTab.find("#type").text("");	
    $thisTab.find("#ipaddress").text("");
    $thisTab.find("#state").text("");  
    $thisTab.find("#version").text("");    
    $thisTab.find("#disconnected").text("");        
        
    //actions ***   
    var $actionMenu = $thisTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();   
	$actionMenu.find("#action_list").append($("#no_available_actions").clone().show());		
}	

var secondaryStorageActionMap = {
    "Delete Secondary Storage": {   
        isAsyncJob: false,   
        dialogBeforeActionFn: doDeleteSecondaryStorage,       
        inProcessText: "Deleting Secondary Storage....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {                             
            if(id.toString() == $("#right_panel_content").find("#tab_content_details").find("#id").text()) {
                secondaryStorageJsonClearRightPanel();   
            }
        }
    } 
}

function doDeleteSecondaryStorage($actionLink, $detailsTab, $midmenuItem1) {     
    var jsonObj = $midmenuItem1.data("jsonObj");    
       
    $("#dialog_info")	
    .text("Please confirm you want to delete this secondary storage")
	.dialog('option', 'buttons', { 						
		"Confirm": function() { 
		    var $thisDialog = $(this);	
			$thisDialog.dialog("close");       	                                             
         
            var id = jsonObj.id;
            var apiCommand = "command=deleteHost&id="+id;  
    	    doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);						
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		} 
	}).dialog("open");
}