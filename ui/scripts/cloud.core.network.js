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
        
    //switch between different tabs - Public Network page
    var $publicNetworkPage = $("#public_network_page");
    var tabArray = [$publicNetworkPage.find("#tab_details"), $publicNetworkPage.find("#tab_ipallocation")];
    var tabContentArray = [$publicNetworkPage.find("#tab_content_details"), $publicNetworkPage.find("#tab_content_ipallocation")];
    var afterSwitchFnArray = [publicNetworkJsonToDetailsTab, publicNetworkJsonToIpAllocationTab];
    switchBetweenDifferentTabs(tabArray, tabContentArray, afterSwitchFnArray);  
         
    //populate items into middle menu  
    var $midmenuContainer = $("#midmenu_container").empty();   
        
    $.ajax({
        data: createURL("command=listNetworks&isSystem=true&zoneId="+zoneObj.id),
        dataType: "json",
        async: false,
        success: function(json) {       
            var items = json.listnetworksresponse.network;       
            if(items != null && items.length > 0) {
                var item = items[0];
                var $midmenuItem1 = $("#midmenu_item").clone();                      
                $midmenuItem1.data("toRightPanelFn", publicNetworkToRightPanel);                             
                publicNetworkToMidmenu(item, $midmenuItem1);    
                bindClickToMidMenu($midmenuItem1, publicNetworkToRightPanel, publicNetworkGetMidmenuId);   
                $midmenuContainer.append($midmenuItem1.show());    
                $midmenuItem1.click();  
            }
        }
    });   
    
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
                    bindClickToMidMenu($midmenuItem1, directNetworkToRightPanel, directNetworkGetMidmenuId);   
                    $midmenuContainer.append($midmenuItem1.show());  
				}
			}	
		}
	});    
}

//***** Direct Network (begin) ******************************************************************************************************
function directNetworkGetMidmenuId(jsonObj) {
    return "midmenuItem_directnetework_" + jsonObj.id;
}

function directNetworkToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", directNetworkGetMidmenuId(jsonObj)); 
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    /*
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_storage_snapshots.png");		
    */
         
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text("VLAN ID: " + fromdb(jsonObj.vlan));   
}

function directNetworkToRightPanel($midmenuItem1) {    
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);  
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);  
            
    $("#direct_network_page").show();
    $("#public_network_page").hide();
    
    $("#direct_network_page").find("#tab_details").click();     
}
	
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
    var ipRange = getIpRange(fromdb(jsonObj.startip), fromdb(jsonObj.endip));
    $thisTab.find("#iprange").text(ipRange);        
    
    $thisTab.find("#domain").text(fromdb(jsonObj.domain));      //might be null
    $thisTab.find("#account").text(fromdb(jsonObj.account));    //might be null
        
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
         
    $.ajax({
		data: createURL("command=listVlanIpRanges&zoneid="+ jsonObj.zoneid + "&networkid="+jsonObj.id),
		dataType: "json",		
		success: function(json) {
		    var items = json.listvlaniprangesresponse.vlaniprange;		    
		    var $container = $thisTab.find("#tab_container").empty();
		    var $template = $("#directnetwork_iprange_template");
		    if(items != null && items.length > 0) {		        
		        for(var i=0; i<items.length; i++) {
		            var $newTemplate = $template.clone();
		            directNetworkIprangeJsonToTemplate(items[i], $newTemplate);
		            $container.append($newTemplate.show());
		        }
		    }		    
		    $thisTab.find("#tab_container").show(); 
            $thisTab.find("#tab_spinning_wheel").hide();    
		}
    });  
}

function directNetworkIprangeJsonToTemplate(jsonObj, $template) {    
    $template.attr("id", "directNetworkIprange_" + jsonObj.id);
    
    var ipRange = getIpRange(fromdb(jsonObj.startip), fromdb(jsonObj.endip));
    $template.find("#grid_header_title").text(ipRange);
    
    $template.find("#vlan").text(jsonObj.vlan)
    $template.find("#startip").text(fromdb(jsonObj.startip));
    $template.find("#endip").text(fromdb(jsonObj.endip));
}
//***** Direct Network (end) ******************************************************************************************************


//***** Public Network (begin) ******************************************************************************************************
function publicNetworkGetMidmenuId(jsonObj) {    
    return "midmenuItem_publicnetework_" + jsonObj.id;
}

function publicNetworkToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", publicNetworkGetMidmenuId(jsonObj)); 
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    /*
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_storage_snapshots.png");		
    */
         
    $midmenuItem1.find("#first_row").text("Public Network"); 
    $midmenuItem1.find("#second_row").text("Network ID: " + fromdb(jsonObj.id));   
}

function publicNetworkToRightPanel($midmenuItem1) {      
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);  
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);  
            
    $("#public_network_page").show();
    $("#direct_network_page").hide();
    
    $("#public_network_page").find("#tab_details").click();     
}
	
function publicNetworkJsonToDetailsTab() {	 
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null)
        return;
    
    var jsonObj = $midmenuItem1.data("jsonObj");    
    if(jsonObj == null) 
	    return;	  
	
	var $thisTab = $("#right_panel_content #public_network_page #tab_content_details");      
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   
		
	$thisTab.find("#grid_header_title").text(fromdb(jsonObj.name));	
		
	$thisTab.find("#id").text(fromdb(jsonObj.id));				
	$thisTab.find("#name").text(fromdb(jsonObj.name));	
	$thisTab.find("#displaytext").text(fromdb(jsonObj.displaytext));
	  	
    $thisTab.find("#vlan").text(fromdb(jsonObj.vlan));
    $thisTab.find("#gateway").text(fromdb(jsonObj.gateway));
    $thisTab.find("#netmask").text(fromdb(jsonObj.netmask));
    var ipRange = getIpRange(fromdb(jsonObj.startip), fromdb(jsonObj.endip));
    $thisTab.find("#iprange").text(ipRange);        
    
    $thisTab.find("#domain").text(fromdb(jsonObj.domain));      //might be null
    $thisTab.find("#account").text(fromdb(jsonObj.account));    //might be null
        
    $thisTab.find("#tab_container").show(); 
    $thisTab.find("#tab_spinning_wheel").hide();   
}

function publicNetworkJsonToIpAllocationTab() {  
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null)
        return;
    
    var jsonObj = $midmenuItem1.data("jsonObj");    
    if(jsonObj == null) 
	    return;	  
	    
	var $thisTab = $("#right_panel_content #public_network_page #tab_content_ipallocation");      
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   
         
    $.ajax({
		data: createURL("command=listVlanIpRanges&zoneid="+ jsonObj.zoneid + "&networkid="+jsonObj.id),
		dataType: "json",		
		success: function(json) {		    
		    var items = json.listvlaniprangesresponse.vlaniprange;		    
		    var $container = $thisTab.find("#tab_container").empty();
		    var $template = $("#directnetwork_iprange_template");
		    if(items != null && items.length > 0) {		        
		        for(var i=0; i<items.length; i++) {
		            var $newTemplate = $template.clone();
		            publicNetworkIprangeJsonToTemplate(items[i], $newTemplate);
		            $container.append($newTemplate.show());
		        }
		    }		    
		    $thisTab.find("#tab_container").show(); 
            $thisTab.find("#tab_spinning_wheel").hide();    
		}
    });  
}

function publicNetworkIprangeJsonToTemplate(jsonObj, $template) {    
    $template.attr("id", "publicNetworkIprange_" + jsonObj.id);
    
    var ipRange = getIpRange(fromdb(jsonObj.startip), fromdb(jsonObj.endip));
    $template.find("#grid_header_title").text(ipRange);
    
    $template.find("#vlan").text(jsonObj.vlan)
    $template.find("#startip").text(fromdb(jsonObj.startip));
    $template.find("#endip").text(fromdb(jsonObj.endip));
}
//***** Public Network (end) ******************************************************************************************************