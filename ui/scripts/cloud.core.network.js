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
  
function afterLoadNetworkJSP($leftmenuItem1) {    
    var zoneObj = $leftmenuItem1.data("jsonObj");    
    if(zoneObj == null) 
	    return;	  
    
    showMiddleMenu();
    disableMultipleSelectionInMidMenu();     
    
    //switch between different tabs - Direct Network page
    var $directNetworkPage = $("#direct_network_page");
    var tabArray = [$directNetworkPage.find("#tab_details"), $directNetworkPage.find("#tab_ipallocation")];
    var tabContentArray = [$directNetworkPage.find("#tab_content_details"), $directNetworkPage.find("#tab_content_ipallocation")];
    var afterSwitchFnArray = [directNetworkJsonToDetailsTab, directNetworkJsonToIpAllocationTab];
    switchBetweenDifferentTabs(tabArray, tabContentArray, afterSwitchFnArray);  
     
    //populate items into middle menu  
    var $midmenuContainer = $("#midmenu_container").empty();      
    var $container_directNetwork = $("<div id='midmenu_directNetwork_container'></div>"); 
    $midmenuContainer.append($container_directNetwork);        
    var $header1 = $("#midmenu_itemheader_without_margin").clone().attr("id", "#midmenu_itemheader_without_margin_clone").show();  //without margin on top
    $header1.find("#name").text("Direct Network");
    $container_directNetwork.append($header1);    
    $.ajax({
		data: createURL("command=listNetworks&type=Direct&zoneId="+zoneObj.id),
		dataType: "json",
		success: function(json) {		    
			var items = json.listnetworksresponse.network;		
			if (items != null && items.length > 0) {					    
				for (var i = 0; i < items.length; i++) {					
					var $midmenuItem1 = $("#midmenu_item").clone();                      
                    $midmenuItem1.data("toRightPanelFn", directNetworkToRightPanel);                             
                    directNetworkToMidmenu(items[i], $midmenuItem1);    
                    bindClickToMidMenu($midmenuItem1, directNetworkToRightPanel, getMidmenuId);             
                   
                    $container_directNetwork.append($midmenuItem1.show());   
                    if(i == 0)  { //click the 1st item in middle menu as default                        
                        $midmenuItem1.click();   
                    }    									
				}
			}			
			else {
                $container_directNetwork.append($("#midmenu_container_no_items_available").clone().attr("id","midmenu_container_no_items_available_clone").show());  
            }  		
		}
	});    
}

function directNetworkToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", getMidmenuId(jsonObj)); 
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    /*
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_storage_snapshots.png");		
    */
         
    $midmenuItem1.find("#first_row").text("VLAN " + fromdb(jsonObj.vlan)); 
    $midmenuItem1.find("#second_row").text(fromdb(getIpRange(jsonObj.startip, jsonObj.endip)));   
    
}

function directNetworkToRightPanel($midmenuItem1) {    
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);  
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);  
    
    $("#right_panel_header").find("#page_title").text("Direct Network");        
    $("#direct_network_page").show();
    $("#public_network_page").hide();
    
    $("#direct_network_page").find("#tab_details").click();     
}

//function vlanJsonToTemplate(item, $template1, true) {			
function directNetworkJsonToDetailsTab() {	    
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null)
        return;
    
    var jsonObj = $midmenuItem1.data("jsonObj");    
    if(jsonObj == null) 
	    return;	  
	
	var $thisTab = $("#right_panel_content #direct_network_page #tab_content_details");      
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   
		
	$thisTab.find("#grid_header_title").text(fromdb(jsonObj.name));	
		
	$thisTab.find("#id").text(fromdb(jsonObj.id));				
	$thisTab.find("#name").text(fromdb(jsonObj.name));	
	$thisTab.find("#displaytext").text(fromdb(jsonObj.displaytext));
	  	
    $thisTab.find("#vlan").text(fromdb(jsonObj.vlan));
    $thisTab.find("#gateway").text(fromdb(jsonObj.gateway));
    $thisTab.find("#netmask").text(fromdb(jsonObj.netmask));
    $thisTab.find("#iprange").text(fromdb(getIpRange(jsonObj.startip, jsonObj.endip)));        
    
    $thisTab.find("#domain").text(fromdb(jsonObj.domain));      //might be null
    $thisTab.find("#account").text(fromdb(jsonObj.account));    //might be null
    $thisTab.find("#podname").text(fromdb(jsonObj.podname));    //might be null  	
    
    $thisTab.find("#tab_container").show(); 
    $thisTab.find("#tab_spinning_wheel").hide();   
}

function directNetworkJsonToIpAllocationTab() {	    
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null)
        return;
    
    var jsonObj = $midmenuItem1.data("jsonObj");    
    if(jsonObj == null) 
	    return;	  
	    
	var $thisTab = $("#right_panel_content #direct_network_page #tab_content_ipallocation");      
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   
    
     
    
    
    
    $thisTab.find("#tab_container").show(); 
    $thisTab.find("#tab_spinning_wheel").hide();    
}

