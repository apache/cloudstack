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
  
function afterLoadClusterJSP($midmenuItem1) {
    hideMiddleMenu();  
    initDialog("dialog_add_host");
    
    //add pool dialog
    initDialog("dialog_add_pool");   
    bindEventHandlerToDialogAddPool($("#dialog_add_pool"));	 
}


function clusterToRightPanel($midmenuItem1) {  
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);        
    clusterJsonToDetailsTab(); 
}

function clusterClearRightPanel() {
    clusterClearDetailsTab();
}

function clusterJsonToDetailsTab() {	   
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null)
        return;
    
    var jsonObj = $midmenuItem1.data("jsonObj");    
    if(jsonObj == null) 
	    return;	
    
    bindAddHostButton($midmenuItem1); 
    bindAddPrimaryStorageButton($midmenuItem1);  
        
    $.ajax({
        data: createURL("command=listClusters&id="+jsonObj.id),
        dataType: "json",
        async: false,
        success: function(json) {            
            var items = json.listclustersresponse.cluster;	           
			if(items != null && items.length > 0) {
                jsonObj = items[0];
                $midmenuItem1.data("jsonObj", jsonObj);                  
            }
        }
    });     
     
    var $thisTab = $("#right_panel_content").find("#tab_content_details");   
    $thisTab.find("#grid_header_title").text(fromdb(jsonObj.name));
    $thisTab.find("#id").text(fromdb(jsonObj.id));
    $thisTab.find("#name").text(fromdb(jsonObj.name));
    $thisTab.find("#zonename").text(fromdb(jsonObj.zonename));        
    $thisTab.find("#podname").text(fromdb(jsonObj.podname));     
    
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
    buildActionLinkForTab("Delete Cluster", clusterActionMap, $actionMenu, $midmenuItem1, $thisTab);        
}

function clusterClearDetailsTab() {	   
    var $thisTab = $("#right_panel_content").find("#tab_content_details");   
    $thisTab.find("#grid_header_title").text("");
    $thisTab.find("#id").text("");
    $thisTab.find("#name").text("");
    $thisTab.find("#zonename").text("");        
    $thisTab.find("#podname").text("");     
    
    //actions ***   
    var $actionMenu = $thisTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();   
	$actionMenu.find("#action_list").append($("#no_available_actions").clone().show());	       
}

var clusterActionMap = {   
    "Delete Cluster": {  
        api: "deleteCluster",            
        isAsyncJob: false,        
        inProcessText: "Deleting Cluster....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {     
            $midmenuItem1.slideUp("slow", function() {
                $(this).remove();
            });
            if(id.toString() == $("#right_panel_content").find("#tab_content_details").find("#id").text()) {
                clearRightPanel();
                clusterClearRightPanel();
            }
        }
    }
}
