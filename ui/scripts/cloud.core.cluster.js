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
  
function afterLoadClusterJSP($leftmenuItem1) {
    var objCluster = $leftmenuItem1.data("jsonObj");
    listMidMenuItems(("listHosts&type=Routing&clusterid="+objCluster.id), hostGetSearchParams, "listhostsresponse", "host", "jsp/host.jsp", afterLoadHostJSP, hostToMidmenu, hostToRightPanel, getMidmenuId, false, ("cluster_"+objCluster.id));    

    /*
    clearButtonsOnTop();
   
    initDialog("dialog_add_host");
    initDialog("dialog_add_pool");    
    bindEventHandlerToDialogAddPool($("#dialog_add_pool"));	       
    
	clusterJsonToRightPanel($leftmenuItem1);	
	*/
}

/*
function clusterJsonToRightPanel($leftmenuItem1) {
    var objCluster = $leftmenuItem1.data("jsonObj");
    
    clearButtonsOnTop();
    if(objCluster.clustertype == "CloudManaged")
    	initAddHostButton($("#midmenu_add_host_button"), "cluster_page", $leftmenuItem1);
    else 
    	$("#midmenu_add_host_button").hide();
    
    initAddPrimaryStorageButton($("#midmenu_add_primarystorage_button"), "cluster_page", $leftmenuItem1);  
    
    listMidMenuItems(("listHosts&type=Routing&clusterid="+objCluster.id), hostGetSearchParams, "listhostsresponse", "host", "jsp/host.jsp", afterLoadHostJSP, hostToMidmenu, hostToRightPanel, getMidmenuId, false, ("cluster_"+objCluster.id));    

    $("#right_panel_content").data("$leftmenuItem1", $leftmenuItem1);
    clusterJsonToDetailsTab();    
}
*/

/*
function clusterJsonToDetailsTab() {	
    var $leftmenuItem1 = $("#right_panel_content").data("$leftmenuItem1");
    if($leftmenuItem1 == null)
        return;
    
    var jsonObj = $leftmenuItem1.data("jsonObj");    
    if(jsonObj == null) 
	    return;	
        
    $.ajax({
        data: createURL("command=listClusters&id="+jsonObj.id),
        dataType: "json",
        async: false,
        success: function(json) {            
            var items = json.listclustersresponse.cluster;	           
			if(items != null && items.length > 0) {
                jsonObj = items[0];
                $leftmenuItem1.data("jsonObj", jsonObj);                  
            }
        }
    });     
     
    var $detailsTab = $("#right_panel_content").find("#tab_content_details");   
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));        
    $detailsTab.find("#podname").text(fromdb(jsonObj.podname));            
}
*/
