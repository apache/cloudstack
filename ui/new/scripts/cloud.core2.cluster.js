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
    showMiddleMenu();
        
    $("#midmenu_add_link").unbind("click").hide();              
    $("#midmenu_add2_link").unbind("click").hide();   
    $("#midmenu_add3_link").unbind("click").hide();          
    
	clusterJsonToRightPanel($midmenuItem1);
	
	var clusterId = $midmenuItem1.data("jsonObj").id;     
        
    var $midmenuContainer = $("#midmenu_container").empty();	    
    var $header1 = $("#midmenu_itemheader_without_margin").clone();  //without margin on top
    $header1.find("#name").text("Host");
    $midmenuContainer.append($header1);
    var count1 = listMidMenuItems2(("listHosts&type=Routing&clusterid="+clusterId), "listhostsresponse", "host", hostToMidmenu, hostToRightPanel, hostGetMidmenuId, false, true); 					
    if(count1 > 0)
	    $header1.show();
	    
	var $header2 = $("#midmenu_itemheader_with_margin").clone(); //with margin on top
    $header2.find("#name").text("Primary Storage");
    $midmenuContainer.append($header2);
	var count2 = listMidMenuItems2(("listStoragePools&clusterid="+clusterId), "liststoragepoolsresponse", "storagepool", primarystorageToMidmenu, primarystorageToRightPanel, primarystorageGetMidmenuId, false, false); 			
    if(count2 > 0)
	    $header2.show();
}

function clusterJsonToRightPanel($leftmenuItem1) {
    clusterJsonToDetailsTab($leftmenuItem1);
}

function clusterJsonToDetailsTab($leftmenuItem1) {	 
    var jsonObj = $leftmenuItem1.data("jsonObj");    
    var $detailsTab = $("#tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);           
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));        
    $detailsTab.find("#podname").text(fromdb(jsonObj.podname));            
}

