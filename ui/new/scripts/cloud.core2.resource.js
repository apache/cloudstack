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

function buildZoneTree() {      
    //***** build zone tree (begin) ***********************************************************************************************
    var forceLogout = true;  // We force a logout only if the user has first added a POD for the very first time     
    var $loading = $("#leftmenu_zone_tree").find("#loading_container").show();
    var $zoneTree = $("#leftmenu_zone_tree").find("#tree_container").hide();
  
    $.ajax({
	    data: createURL("command=listZones&available=true"+maxPageSize),
		dataType: "json",		
		success: function(json) {
			var items = json.listzonesresponse.zone;
			var container = $zoneTree.empty();
			if (items != null && items.length > 0) {					    
				for (var i = 0; i < items.length; i++) {
					var $zoneNode = $("#leftmenu_zone_node_template").clone(true);
					zoneJSONToTreeNode(items[i],$zoneNode);
					container.append($zoneNode.show());
				}
			}	
			$loading.hide();
            $zoneTree.show();
		}
	});  
    
	$("#leftmenu_zone_node_template").unbind("click").bind("click", function(event) {
		var template = $(this);
		var target = $(event.target);
		var action = target.attr("id");
		var id = template.data("id");
		var name = template.data("name");
		
		switch (action) {
			case "zone_arrow" :				  	   
				if(target.hasClass("expanded_close")) {						
					target.removeClass("expanded_close").addClass("expanded_open");					
					target.parent().parent().siblings("#zone_content").show();	
				} 
				else if(target.hasClass("expanded_open")) {					
					target.removeClass("expanded_open").addClass("expanded_close");					
					target.parent().parent().siblings("#zone_content").hide();									
				}
				break;
											    
			case "pod_arrow" :		    	   
				if(target.hasClass("expanded_close")) {						
					target.removeClass("expanded_close").addClass("expanded_open");					
					target.parent().parent().siblings("#pod_content").show();	
				} 
				else if(target.hasClass("expanded_open")) {					
					target.removeClass("expanded_open").addClass("expanded_close");					
					target.parent().parent().siblings("#pod_content").hide();									
				}
				break;	
				
			
			case "zone_name_label":	
			case "zone_name":	
			    selectTreeNodeInLeftMenu(target.parent().parent().parent());	
			    var $leftmenuItem1 = target.parent().parent().parent().parent();	
			    showPage($("#zone_page"), $leftmenuItem1);			    		   				    		   			    
			    break;		
			    	
			case "pod_name_label" :	
			case "pod_name" :	
			    selectTreeNodeInLeftMenu(target.parent().parent().parent());
			    var $leftmenuItem1 = target.parent().parent().parent().parent();
			    showPage($("#pod_page"), $leftmenuItem1);				   			
				break;		
				    
			case "cluster_name_label" :	
			case "cluster_name" :	
			    selectTreeNodeInLeftMenu(target.parent().parent().parent());
			    var $leftmenuItem1 = target.parent().parent().parent().parent();	
			    showPage($("#cluster_page"), $leftmenuItem1);
			    break;								
						
			case "systemvm_name_label" :
			case "systemvm_name" :		
			    selectTreeNodeInLeftMenu(target.parent().parent().parent());	
			    var $leftmenuItem1 = target.parent().parent().parent().parent();	
			    showPage($("#systemvm_page"), $leftmenuItem1);
				break;			
			
			default:
				break;
		}
		return false;
	});    	
	//***** build zone tree (end) *************************************************************************************************       
}    

function selectTreeNodeInLeftMenu($menuToSelect, expandable) {	
	if($selectedLeftMenu != null)
		$selectedLeftMenu.removeClass("selected");  
	$menuToSelect.addClass("selected");
	$selectedLeftMenu = $menuToSelect; 	
}

function zoneJSONToTreeNode(json, $zoneNode) {
    var zoneid = json.id;
    $zoneNode.attr("id", "zone_" + zoneid);  
    $zoneNode.data("jsonObj", json);	 
    $zoneNode.data("id", zoneid).data("name", fromdb(json.name));
    var zoneName = $zoneNode.find("#zone_name").text(fromdb(json.name));	    
    zoneName.data("jsonObj", json);	    
	
    $.ajax({
        data: createURL("command=listPods&zoneid="+zoneid+maxPageSize),
	    dataType: "json",
	    async: false,
	    success: function(json) {
		    var items = json.listpodsresponse.pod;			    
		    var container = $zoneNode.find("#pods_container");
		    if (items != null && items.length > 0) {					    
			    for (var i = 0; i < items.length; i++) {
				    var $podNode = $("#leftmenu_pod_node_template").clone(true);
				    podJSONToTreeNode(items[i], $podNode);
				    container.append($podNode.show());
				    forceLogout = false;  // We don't force a logout if pod(s) exit.
			    }
		    }
	    }
    });
    	    
    $.ajax({
        data: createURL("command=listSystemVms&zoneid="+zoneid+maxPageSize),
	    dataType: "json",
	    async: false,
	    success: function(json) {
		    var items = json.listsystemvmsresponse.systemvm;
		    var container = $zoneNode.find("#systemvms_container").empty();
		    if (items != null && items.length > 0) {					    
			    for (var i = 0; i < items.length; i++) {
				    var $systemvmNode = $("#leftmenu_systemvm_node_template").clone(true);
				    systemvmJSONToTreeNode(items[i], $systemvmNode);
				    container.append($systemvmNode.show());
			    }
		    }
	    }
    });
}

function podJSONToTreeNode(json, $podNode) {	
    var podid = json.id;
    $podNode.attr("id", "pod_" + podid); 
    $podNode.data("jsonObj", json);     	
	$podNode.data("id", podid).data("name", fromdb(json.name));
	
	var podName = $podNode.find("#pod_name").text(fromdb(json.name));
	podName.data("jsonObj", json);	    
		
    $.ajax({
        data: createURL("command=listClusters&podid="+podid+maxPageSize),
        dataType: "json",
        async: false,
        success: function(json) {
	        var items = json.listclustersresponse.cluster;
	        var container = $podNode.find("#clusters_container").empty();
	        if (items != null && items.length > 0) {					    
		        for (var i = 0; i < items.length; i++) {
			        var clusterTemplate = $("#leftmenu_cluster_node_template").clone(true); 
			        clusterJSONToTreeNode(items[i], clusterTemplate);
			        container.append(clusterTemplate.show());
		        }
	        }
        }
    });		
}
	
function systemvmJSONToTreeNode(json, $systemvmNode) {	
    var systemvmid = json.id;	
    $systemvmNode.attr("id", "systemvm_"+systemvmid);
    $systemvmNode.data("jsonObj", json);	    
    $systemvmNode.data("id", systemvmid).data("name", json.name);	     
    var systeymvmName = $systemvmNode.find("#systemvm_name").text(json.name);	    
    systeymvmName.data("jsonObj", json);	    		
}
		
function clusterJSONToTreeNode(json, $clusterNode) {
    $clusterNode.attr("id", "cluster_"+json.id);
    $clusterNode.data("jsonObj", json);	  
    $clusterNode.data("id", json.id).data("name", fromdb(json.name));	    
    var clusterName = $clusterNode.find("#cluster_name").text(fromdb(json.name));
    clusterName.data("jsonObj", json);	   
}			

//$menuItem1 is either $leftmenuItem1 or $midmenuItem1
function showPage($pageToShow, $menuItem1) { 
    clearMiddleMenu();     
    if($pageToShow.length == 0) { //resource.jsp is not loaded in right panel      
        $("#right_panel").load("jsp/resource.jsp", function(){  
			showPage2($($pageToShow.selector), $menuItem1); //$pageToShow is still empty (i.e. $pageToShow.length == 0), So, select the element again.
		});        
    }
    else {        
        showPage2($pageToShow, $menuItem1); 
    }
}

//$menuItem1 is either $leftmenuItem1 or $midmenuItem1
function showPage2($pageToShow, $menuItem1) {   
    var jsonObj;
    if($menuItem1 != null)
        jsonObj = $menuItem1.data("jsonObj");  
        
    var pageArray = [$("#resource_page"), $("#zone_page"), $("#pod_page"), $("#cluster_page"), $("#host_page"), $("#primarystorage_page"), $("#systemvm_page")];
    var pageLabelArray = ["Resource", "Zone", "Pod", "Cluster", "Host", "Primary Storage", "System VM"];       
   
    for(var i=0; i<pageArray.length; i++) {
        if(pageArray[i].attr("id") == $pageToShow.attr("id")) {
            $("#right_panel_header").find("#label").text(pageLabelArray[i]);
            pageArray[i].show();
        }
        else {
            pageArray[i].hide();
        }
        $pageToShow.data("jsonObj", jsonObj);
    }   
    
    if($pageToShow.attr("id") == "resource_page") { 
        initAddZoneButton($("#midmenu_add_link")); 
        initDialog("dialog_add_zone");         
    }
    else if($pageToShow.attr("id") == "zone_page") {               
        initAddPodButton($("#midmenu_add_link"));                  
        initAddVLANButton($("#midmenu_add2_link"));
        initAddSecondaryStorageButton($("#midmenu_add3_link"));
       
        initDialog("dialog_add_pod", 320); 
        initDialog("dialog_add_vlan_for_zone");
        initDialog("dialog_add_secondarystorage"); 
        initDialog("dialog_confirmation_delete_secondarystorage"); 
        
        // If the network type is vnet, don't show any vlan stuff.
	    if (getNetworkType() == "vnet") 		
		    $("#dialog_add_vlan_for_zone").attr("title", "Add Public IP Range");		
	    bindEventHandlerToDialogAddVlanForZone();	        
                
        //switch between different tabs in zone page 
	    var $zonePage = $pageToShow;
        var tabArray = [$zonePage.find("#tab_details"), $zonePage.find("#tab_network"), $zonePage.find("#tab_secondarystorage")];
        var tabContentArray = [$zonePage.find("#tab_content_details"), $zonePage.find("#tab_content_network"), $zonePage.find("#tab_content_secondarystorage")];
        //var afterSwitchFnArray = [afterSwitchToDetailsTab, afterSwitchToNetworkTab, afterSwitchToSecondaryStorageTab];
        switchBetweenDifferentTabs(tabArray, tabContentArray);    
        $zonePage.find("#tab_details").click();   
        
        hideMiddleMenu();
		zoneJsonToRightPanel($menuItem1);		  
    }
    else if($pageToShow.attr("id") == "pod_page") {        	
        initAddHostButton($("#midmenu_add_link")); 
        initAddPrimaryStorageButton($("#midmenu_add2_link"));  
               
        initDialog("dialog_add_host");
        initDialog("dialog_add_pool");
        
        // if hypervisor is KVM, limit the server option to NFS for now
	    if (getHypervisorType() == 'kvm') 
		    $("#dialog_add_pool").find("#add_pool_protocol").empty().html('<option value="nfs">NFS</option>');	
	    bindEventHandlerToDialogAddPool();	 
	    
	    showMiddleMenu();	
		podJsonToRightPanel($menuItem1);     
		
		var podId = jsonObj.id;
	    $("#midmenu_container").empty();
	    listMidMenuItems2(("listHosts&type=Routing&podid="+podId), "listhostsresponse", "host", hostToMidmenu, hostToRightPanel, hostGetMidmenuId, false, false); 					
		listMidMenuItems2(("listStoragePools&podid="+podId), "liststoragepoolsresponse", "storagepool", primarystorageToMidmenu, primarystorageToRightPanel, primarystorageGetMidmenuId, false, false); 	
    }  
    else if($pageToShow.attr("id") == "cluster_page") {
        $("#midmenu_add_link").unbind("click").hide();              
        $("#midmenu_add2_link").unbind("click").hide();   
        $("#midmenu_add3_link").unbind("click").hide();  
        
        showMiddleMenu();
		clusterJsonToRightPanel($menuItem1);
		
	    var clusterId = jsonObj.id;
	    $("#midmenu_container").empty();
	    listMidMenuItems2(("listHosts&type=Routing&clusterid="+clusterId), "listhostsresponse", "host", hostToMidmenu, hostToRightPanel, hostGetMidmenuId, false, true); 					
		listMidMenuItems2(("listStoragePools&clusterid="+clusterId), "liststoragepoolsresponse", "storagepool", primarystorageToMidmenu, primarystorageToRightPanel, primarystorageGetMidmenuId, false, false); 			
    }
    else if($pageToShow.attr("id") == "host_page") {
        initAddHostButton($("#midmenu_add_link")); 
        initAddPrimaryStorageButton($("#midmenu_add2_link"));          
    
        initDialog("dialog_confirmation_enable_maintenance");
	    initDialog("dialog_confirmation_cancel_maintenance");
	    initDialog("dialog_confirmation_force_reconnect");
	    initDialog("dialog_confirmation_remove_host");
	    initDialog("dialog_update_os");
    }  
    else if($pageToShow.attr("id") == "primarystorage_page") {
        initAddHostButton($("#midmenu_add_link")); 
        initAddPrimaryStorageButton($("#midmenu_add2_link"));  
        
        initDialog("dialog_confirmation_delete_primarystorage");
    }  
    else if($pageToShow.attr("id") == "systemvm_page") {  
        hideMiddleMenu();			
	    systemvmJsonToRightPanel($menuItem1);		
    }    
}

//***** zone page (begin) *****************************************************************************************************	
function zoneGetLeftmenuId(jsonObj) {
    return "zone_" + jsonObj.id;
}

function zoneJsonToRightPanel($leftmenuItem1) {
    zoneJsonToDetailsTab($leftmenuItem1);
    var jsonObj = $leftmenuItem1.data("jsonObj");  
    zoneJsonToNetworkTab(jsonObj);				    
    zoneJsonToSecondaryStorageTab(jsonObj);
}

function zoneJsonClearRightPanel($leftmenuItem1) {
    zoneJsonClearDetailsTab($leftmenuItem1);
    var jsonObj = $leftmenuItem1.data("jsonObj");  
    zoneJsonClearNetworkTab(jsonObj);				    
    zoneJsonClearSecondaryStorageTab(jsonObj);
}

function zoneJsonToDetailsTab($leftmenuItem1) {	 
    var jsonObj = $leftmenuItem1.data("jsonObj");     
    var $detailsTab = $("#zone_page").find("#tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);           
    $detailsTab.find("#id").text(jsonObj.id);
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#dns1").text(fromdb(jsonObj.dns1));
    $detailsTab.find("#dns2").text(fromdb(jsonObj.dns2));
    $detailsTab.find("#internaldns1").text(fromdb(jsonObj.internaldns1));
    $detailsTab.find("#internaldns2").text(fromdb(jsonObj.internaldns2));	
    $detailsTab.find("#vlan").text(fromdb(jsonObj.vlan));
    $detailsTab.find("#guestcidraddress").text(fromdb(jsonObj.guestcidraddress));   
    
    //actions ***   
    var $actionLink = $detailsTab.find("#action_link"); 
    $actionLink.bind("mouseover", function(event) {	    
        $(this).find("#action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {       
        $(this).find("#action_menu").hide();    
        return false;
    });	  
    var $actionMenu = $detailsTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();        
    buildActionLinkForDetailsTab("Delete Zone", zoneActionMap, $actionMenu, $leftmenuItem1, $detailsTab);     
}	  

function zoneJsonClearDetailsTab(jsonObj) {	    
    var $detailsTab = $("#zone_page").find("#tab_content_details");             
    $detailsTab.find("#id").text("");
    $detailsTab.find("#name").text("");
    $detailsTab.find("#dns1").text("");
    $detailsTab.find("#dns2").text("");
    $detailsTab.find("#internaldns1").text("");
    $detailsTab.find("#internaldns2").text("");	
    $detailsTab.find("#vlan").text("");
    $detailsTab.find("#guestcidraddress").text("");   
    
    //actions ***   
    var $actionMenu = $detailsTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();   
	$actionMenu.find("#action_list").append($("#no_available_actions").clone().show());		
}	

var $vlanContainer;
function zoneJsonToNetworkTab(jsonObj) {	    
    var $networkTab = $("#zone_page").find("#tab_content_network");      
    $networkTab.find("#zone_cloud").find("#zone_name").text(fromdb(jsonObj.name));	 
    $networkTab.find("#zone_vlan").text(jsonObj.vlan);   
                  
    $.ajax({
	  data: createURL("command=listVlanIpRanges&zoneId="+jsonObj.id),
		dataType: "json",
		success: function(json) {
			var items = json.listvlaniprangesresponse.vlaniprange;		
			$vlanContainer = $networkTab.find("#vlan_container").empty();   					
			if (items != null && items.length > 0) {					    
				for (var i = 0; i < items.length; i++) {	
				    var item = items[i];
				    					   
				    var $template1 = $("#vlan_template").clone(); 							   
				    if(item.forvirtualnetwork == false)  //direct
				        $template1.find("#vlan_type_icon").removeClass("virtual").addClass("direct");
				    else  //virtual
				    	$template1.find("#vlan_type_icon").removeClass("direct").addClass("virtual");				    
				    
				    vlanJsonToTemplate(item, $template1);
				    $vlanContainer.append($template1.show());											
				}
			}
		}
	});
}	 

function zoneJsonClearNetworkTab(jsonObj) {	    
    var $networkTab = $("#zone_page").find("#tab_content_network");      
    $networkTab.find("#zone_cloud").find("#zone_name").text("");	 
    $networkTab.find("#zone_vlan").text("");   
    $networkTab.find("#vlan_container").empty();    
}	 

function zoneJsonToSecondaryStorageTab(jsonObj) {   
    var zoneObj =  $("#zone_page").find("#tab_content_details").data("jsonObj");  
    $.ajax({
		cache: false,
		data: createURL("command=listHosts&type=SecondaryStorage&zoneid="+zoneObj.id+maxPageSize),
		dataType: "json",
		success: function(json) {			   			    
			var items = json.listhostsresponse.host;	
			var container = $("#zone_page").find("#tab_content_secondarystorage").empty();																					
			if (items != null && items.length > 0) {			    
				var template = $("#secondary_storage_tab_template");				
				for (var i = 0; i < items.length; i++) {
					var newTemplate = template.clone(true);	               
	                secondaryStorageJSONToTemplate(items[i], newTemplate); 
	                container.append(newTemplate.show());	
				}			
			}			
		}
	});     
}

function zoneJsonClearSecondaryStorageTab(jsonObj) {   
    $("#zone_page").find("#tab_content_secondarystorage").empty();	
}

function vlanJsonToTemplate(jsonObj, $template1) {
    $template1.data("jsonObj", jsonObj);
    $template1.find("#vlan_id").text(jsonObj.vlan);
    $template1.find("#ip_range").text(jsonObj.description);
    $template1.unbind("click").bind("click", function(event) {        
        var $target = $(event.target);
        var targetId = $target.attr("id");     
        switch(targetId) {
            case "info_icon":   
                var vlanName = jsonObj.id;
		        var vlanDisplayName = vlanName;
		        if (jsonObj.description != null) {
			        if (jsonObj.description.indexOf("-") == -1) {
				        vlanName = jsonObj.description;
				        vlanDisplayName = vlanName;
			        } else {
				        var ranges = jsonObj.description.split("-");
				        vlanName = ranges[0] + " -" + ranges[1];
				        vlanDisplayName = ranges[0] + " - " + ranges[1];
			        }
		        }
                                
                var $infoDropdown = $target.siblings("#info_dropdown");               
                $infoDropdown.find("#vlan").text(fromdb(jsonObj.vlan));
                $infoDropdown.find("#gateway").text(fromdb(jsonObj.gateway));
                $infoDropdown.find("#netmask").text(fromdb(jsonObj.netmask));
                $infoDropdown.find("#iprange").text(fromdb(vlanDisplayName));
                if(jsonObj.domainid != null) {
                    var $container = $infoDropdown.find("#domainid_container").show();
                    $container.find("#domainid").text(fromdb(jsonObj.domainid));               
                }                
                if(jsonObj.domain != null) {
                     var $container = $infoDropdown.find("#domain_container").show();
                    $container.find("#domain").text(fromdb(jsonObj.domain));        
                }
                if(jsonObj.account != null) {
                    var $container = $infoDropdown.find("#account_container").show();
                    $container.find("#account").text(fromdb(jsonObj.account));
                }
                if(jsonObj.podname != null) {
                    var $container = $infoDropdown.find("#podname_container").show();
                    $container.find("#podname").text(fromdb(jsonObj.podname));
                }     
                $infoDropdown.show();
                break;
                
            case "close_link":                
                $target.parent().parent().hide();
                break;
        }
        
        return false;
    });
} 	

//***** zone page (end) *******************************************************************************************************

//***** pod page (begin) ******************************************************************************************************
function podGetLeftmenuId(jsonObj) {
    return "pod_" + jsonObj.id;
}

function podJsonToRightPanel($leftmenuItem1) {	 
    podJsonToDetailsTab($leftmenuItem1);
}

function podJsonToDetailsTab($leftmenuItem1) {	
    var jsonObj = $leftmenuItem1.data("jsonObj");     
    var $detailsTab = $("#pod_page").find("#tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);           
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#cidr").text(fromdb(jsonObj.cidr));        
    $detailsTab.find("#ipRange").text(getIpRange(jsonObj.startip, jsonObj.endip));
    $detailsTab.find("#gateway").text(fromdb(jsonObj.gateway));  
    
    //actions ***   
    var $actionLink = $detailsTab.find("#action_link"); 
    $actionLink.bind("mouseover", function(event) {	    
        $(this).find("#action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {       
        $(this).find("#action_menu").hide();    
        return false;
    });	  
    var $actionMenu = $detailsTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();   
    buildActionLinkForDetailsTab("Delete Pod", podActionMap, $actionMenu, $leftmenuItem1, $detailsTab);  
}	

function podJsonClearRightPanel(jsonObj) {	 
    podJsonClearDetailsTab(jsonObj);
}

function podJsonClearDetailsTab(jsonObj) {	    
    var $detailsTab = $("#pod_page").find("#tab_content_details");       
    $detailsTab.find("#id").text("");
    $detailsTab.find("#name").text("");
    $detailsTab.find("#cidr").text("");        
    $detailsTab.find("#ipRange").text("");
    $detailsTab.find("#gateway").text("");  
    
    //if (getDirectAttachUntaggedEnabled() == "true") 
	//	$("#submenu_content_zones #action_add_directip_vlan").data("type", "pod").data("id", obj.id).data("name", obj.name).data("zoneid", obj.zoneid).show();		
}
	
function getIpRange(startip, endip) {
    var ipRange = "";
	if (startip != null && startip.length > 0) {
		ipRange = startip;
	}
	if (endip != null && endip.length > 0) {
		ipRange = ipRange + " - " + endip;
	}		
	return ipRange;
}	

//***** pod page (end) ********************************************************************************************************

//***** cluster page (bgein) **************************************************************************************************
function clusterJsonToRightPanel($leftmenuItem1) {
    clusterJsonToDetailsTab($leftmenuItem1);
}

function clusterJsonToDetailsTab($leftmenuItem1) {	 
    var jsonObj = $leftmenuItem1.data("jsonObj");    
    var $detailsTab = $("#cluster_page").find("#tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);           
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));        
    $detailsTab.find("#podname").text(fromdb(jsonObj.podname));            
}
//***** cluster page (end) ****************************************************************************************************

//***** host page (bgein) *****************************************************************************************************

function hostGetMidmenuId(jsonObj) {
    return "midmenuItem_host_" + jsonObj.id; 
}

function hostToMidmenu(jsonObj, $midmenuItem1) {    
    $midmenuItem1.attr("id", hostGetMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj);      
    //$iconContainer.find("#icon").attr("src", "images/midmenuicon_host.png");      
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.ipaddress.substring(0,25)); 
}

function hostToRightPanel($midmenuItem1) { 
    copyAfterActionInfoToRightPanel($midmenuItem1);
    hostJsonToDetailsTab($midmenuItem1);       
    showPage($("#host_page"), $midmenuItem1);
}

function hostJsonToDetailsTab($midmenuItem1) {
    var jsonObj = $midmenuItem1.data("jsonObj");	    
    var $detailsTab = $("#host_page").find("#tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);           
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#state").text(fromdb(jsonObj.state));        
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename)); 
    $detailsTab.find("#podname").text(fromdb(jsonObj.podname));   
    $detailsTab.find("#clustername").text(fromdb(jsonObj.clustername));        
    $detailsTab.find("#ipaddress").text(fromdb(jsonObj.ipaddress)); 
    $detailsTab.find("#version").text(fromdb(jsonObj.version));  
    $detailsTab.find("#oscategoryname").text(fromdb(jsonObj.oscategoryname));        
    $detailsTab.find("#disconnected").text(fromdb(jsonObj.disconnected));  
    
    populateForUpdateOSDialog(jsonObj.oscategoryid);
    
    //actions ***   
    var $actionLink = $detailsTab.find("#action_link"); 
    $actionLink.bind("mouseover", function(event) {	    
        $(this).find("#action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {       
        $(this).find("#action_menu").hide();    
        return false;
    });	  
    var $actionMenu = $detailsTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    var noAvailableActions = true;
    
    if (jsonObj.state == 'Up' || jsonObj.state == "Connecting") {
		buildActionLinkForDetailsTab("Enable Maintenance Mode", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
	    buildActionLinkForDetailsTab("Force Reconnect", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);   
	    buildActionLinkForDetailsTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);   
	    noAvailableActions = false;
	} 
	else if(jsonObj.state == 'Down') {
	    buildActionLinkForDetailsTab("Enable Maintenance Mode", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
	    buildActionLinkForDetailsTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  	    
        buildActionLinkForDetailsTab("Remove Host", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
        noAvailableActions = false;
    }	
	else if(jsonObj.state == "Alert") {
	    buildActionLinkForDetailsTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  	
	    noAvailableActions = false;   
     
	}	
	else if (jsonObj.state == "ErrorInMaintenance") {
	    buildActionLinkForDetailsTab("Enable Maintenance Mode", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
        buildActionLinkForDetailsTab("Cancel Maintenance Mode", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
        buildActionLinkForDetailsTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  	 
        noAvailableActions = false;   
    }
	else if (jsonObj.state == "PrepareForMaintenance") {
	    buildActionLinkForDetailsTab("Cancel Maintenance Mode", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
        buildActionLinkForDetailsTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  	
        noAvailableActions = false;    
    }
	else if (jsonObj.state == "Maintenance") {
	    buildActionLinkForDetailsTab("Cancel Maintenance Mode", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
        buildActionLinkForDetailsTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  	    
        buildActionLinkForDetailsTab("Remove Host", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
        noAvailableActions = false;
    }
	else if (jsonObj.state == "Disconnected"){
	    buildActionLinkForDetailsTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  	    
        buildActionLinkForDetailsTab("Remove Host", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
        noAvailableActions = false;
    }
	else {
	    alert("Unsupported Host State: " + jsonObj.state);
	} 
    
    //temporary for testing (begin) *****
    /*
    buildActionLinkForDetailsTab("Enable Maintenance Mode", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
    buildActionLinkForDetailsTab("Cancel Maintenance Mode", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
    buildActionLinkForDetailsTab("Force Reconnect", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
    buildActionLinkForDetailsTab("Remove Host", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
    buildActionLinkForDetailsTab("Update OS Preference", hostActionMap, $actionMenu, $midmenuItem1, $detailsTab);
    noAvailableActions = false; 	  
    */  
    //temporary for testing (begin) *****
    
    // no available actions 
	if(noAvailableActions == true) {
	    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
	}	       
}


function hostClearRightPanel() {
    hostClearDetailsTab(); 
}

function hostClearDetailsTab() {
    var $detailsTab = $("#right_panel_content #host_page #tab_content_details");  
    $detailsTab.find("#id").text("");
    $detailsTab.find("#name").text("");
    $detailsTab.find("#state").text("");        
    $detailsTab.find("#zonename").text(""); 
    $detailsTab.find("#podname").text(""); 
    $detailsTab.find("#clustername").text(""); 
    $detailsTab.find("#ipaddress").text(""); 
    $detailsTab.find("#version").text(""); 
    $detailsTab.find("#oscategoryname").text("");       
    $detailsTab.find("#disconnected").text(""); 
    
    var $actionMenu = $detailsTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();   
	$actionMenu.find("#action_list").append($("#no_available_actions").clone().show());		    
}

//***** host page (end) *******************************************************************************************************

//***** primary storage page (bgein) ******************************************************************************************
function primarystorageGetMidmenuId(jsonObj) {
    return "midmenuItem_primarystorage_" + jsonObj.id; 
}

function primarystorageToMidmenu(jsonObj, $midmenuItem1) {    
    $midmenuItem1.attr("id", primarystorageGetMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj);      
    //$iconContainer.find("#icon").attr("src", "images/midmenuicon_primarystorage.png");      
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.ipaddress.substring(0,25));          
}

function primarystorageToRightPanel($midmenuItem1) {  
    copyAfterActionInfoToRightPanel($midmenuItem1);
    primarystorageJsonToDetailsTab($midmenuItem1);      
    showPage($("#primarystorage_page"), $midmenuItem1);
}

function primarystorageJsonToDetailsTab($midmenuItem1) {	
    var jsonObj = $midmenuItem1.data("jsonObj");    
    var $detailsTab = $("#primarystorage_page").find("#tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);           
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));
    $detailsTab.find("#podname").text(fromdb(jsonObj.podname));
    $detailsTab.find("#clustername").text(fromdb(jsonObj.clustername));
    $detailsTab.find("#type").text(fromdb(jsonObj.type));
    $detailsTab.find("#ipaddress").text(fromdb(jsonObj.ipaddress));
    $detailsTab.find("#path").text(fromdb(jsonObj.path));                
	$detailsTab.find("#disksizetotal").text(convertBytes(jsonObj.disksizetotal));
	$detailsTab.find("#disksizeallocated").text(convertBytes(jsonObj.disksizeallocated));
	$detailsTab.find("#tags").text(fromdb(jsonObj.tags));   
	
	//actions ***   
    var $actionLink = $detailsTab.find("#action_link"); 
    $actionLink.bind("mouseover", function(event) {	    
        $(this).find("#action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {       
        $(this).find("#action_menu").hide();    
        return false;
    });	  
    var $actionMenu = $detailsTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty(); 
    buildActionLinkForDetailsTab("Delete Primary Storage", primarystorageActionMap, $actionMenu, $midmenuItem1, $detailsTab);        
}
       
function primarystorageClearRigntPanel() {  
    primarystorageJsonClearDetailsTab(jsonObj);  
}

function primarystorageJsonClearDetailsTab() {	    
    var $detailsTab = $("#primarystorage_page").find("#tab_content_details");   
    $detailsTab.find("#id").text("");
    $detailsTab.find("#name").text("");
    $detailsTab.find("#zonename").text("");
    $detailsTab.find("#podname").text("");
    $detailsTab.find("#clustername").text("");
    $detailsTab.find("#type").text("");
    $detailsTab.find("#ipaddress").text("");
    $detailsTab.find("#path").text("");                
	$detailsTab.find("#disksizetotal").text("");
	$detailsTab.find("#disksizeallocated").text("");
	$detailsTab.find("#tags").text("");         
}
//***** primary storage page (end) *********************************************************************************************

//***** systemVM page (begin) *************************************************************************************************
function systemvmJsonToRightPanel($leftmenuItem1) {
    systemvmJsonToDetailsTab($leftmenuItem1);
}

function systemvmJsonToDetailsTab($leftmenuItem1) {	   
    var jsonObj = $leftmenuItem1.data("jsonObj"); 
    var $detailsTab = $("#systemvm_page").find("#tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);   
     
    resetViewConsoleAction(jsonObj, $detailsTab);         
    setVmStateInRightPanel(jsonObj.state, $detailsTab.find("#state"));		
    $detailsTab.find("#ipAddress").text(jsonObj.publicip);
        
    $detailsTab.find("#state").text(jsonObj.state);     
    $detailsTab.find("#systemvmtype").text(toSystemVMTypeText(jsonObj.systemvmtype));    
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename)); 
    $detailsTab.find("#id").text(fromdb(jsonObj.id));  
    $detailsTab.find("#name").text(fromdb(jsonObj.name));   
    $detailsTab.find("#activeviewersessions").text(fromdb(jsonObj.activeviewersessions)); 
    $detailsTab.find("#publicip").text(fromdb(jsonObj.publicip)); 
    $detailsTab.find("#privateip").text(fromdb(jsonObj.privateip)); 
    $detailsTab.find("#hostname").text(fromdb(jsonObj.hostname));
    $detailsTab.find("#gateway").text(fromdb(jsonObj.gateway)); 
    $detailsTab.find("#created").text(fromdb(jsonObj.created));             
}

function toSystemVMTypeText(value) {
    var text = "";
    if(value == "consoleproxy")
        text = "Console Proxy VM";
    else if(value == "secondarystoragevm")
        text = "Secondary Storage VM";
    return text;        
}
//***** systemVM page (end) ***************************************************************************************************

function initAddVLANButton($addButton) {
    $addButton.find("#label").text("Add VLAN");      
    $addButton.show();   
    $addButton.unbind("click").bind("click", function(event) {  
        $("#zone_page").find("#tab_network").click();      
        var zoneObj = $("#zone_page").find("#tab_content_details").data("jsonObj");       
        var dialogAddVlanForZone = $("#dialog_add_vlan_for_zone");       
        dialogAddVlanForZone.find("#zone_name").text(fromdb(zoneObj.name));         
		dialogAddVlanForZone.find("#add_publicip_vlan_vlan_container, #add_publicip_vlan_domain_container, #add_publicip_vlan_account_container").hide();
		dialogAddVlanForZone.find("#add_publicip_vlan_tagged, #add_publicip_vlan_vlan, #add_publicip_vlan_gateway, #add_publicip_vlan_netmask, #add_publicip_vlan_startip, #add_publicip_vlan_endip, #add_publicip_vlan_account").val("");
		
				
		if (getNetworkType() == 'vnet') {
			dialogAddVlanForZone.find("#add_publicip_vlan_type_container").hide();
		} else {	
			dialogAddVlanForZone.find("#add_publicip_vlan_pod_container").show();	
			dialogAddVlanForZone.find("#add_publicip_vlan_type").change();
			dialogAddVlanForZone.find("#add_publicip_vlan_type_container").show();
			var podSelect = dialogAddVlanForZone.find("#add_publicip_vlan_pod").empty();		
			$.ajax({
			    data: createURL("command=listPods&zoneId="+zoneObj.id+maxPageSize),
				dataType: "json",
				async: false,
				success: function(json) {
					var pods = json.listpodsresponse.pod;						
					if (pods != null && pods.length > 0) {
						for (var i = 0; i < pods.length; i++) {
							podSelect.append("<option value='" + pods[i].id + "'>" + fromdb(pods[i].name) + "</option>"); 
						}
					} else {
						podSelect.append("<option value=''>No available pods</option>"); 
					}
				}
			});
			
			var domainSelect = dialogAddVlanForZone.find("#add_publicip_vlan_domain").empty();	
			$.ajax({
			    data: createURL("command=listDomains"+maxPageSize),
				dataType: "json",
				async: false,
				success: function(json) {
					var domains = json.listdomainsresponse.domain;						
					if (domains != null && domains.length > 0) {
						for (var i = 0; i < domains.length; i++) {
							domainSelect.append("<option value='" + domains[i].id + "'>" + fromdb(domains[i].name) + "</option>"); 
						}
					} 
				}
			});
		}

		dialogAddVlanForZone
		.dialog('option', 'buttons', { 	
			"Add": function() { 	
			    var $thisDialog = $(this);		
			    $thisDialog.find("#info_container").hide();
			    					
				// validate values
				var isValid = true;					
				var isTagged = false;
				var isDirect = false;
				if (getNetworkType() == "vlan") {
					isDirect = $thisDialog.find("#add_publicip_vlan_type").val() == "false";
					isTagged = $thisDialog.find("#add_publicip_vlan_tagged").val() == "tagged";
				}
				
				isValid &= validateString("Account", $thisDialog.find("#add_publicip_vlan_account"), $thisDialog.find("#add_publicip_vlan_account_errormsg"), true); //optional
				
				if (isTagged) {
					isValid &= validateNumber("VLAN", $thisDialog.find("#add_publicip_vlan_vlan"), $thisDialog.find("#add_publicip_vlan_vlan_errormsg"), 2, 4095);
				}
				isValid &= validateIp("Gateway", $thisDialog.find("#add_publicip_vlan_gateway"), $thisDialog.find("#add_publicip_vlan_gateway_errormsg"));
				isValid &= validateIp("Netmask", $thisDialog.find("#add_publicip_vlan_netmask"), $thisDialog.find("#add_publicip_vlan_netmask_errormsg"));
				isValid &= validateIp("Start IP Range", $thisDialog.find("#add_publicip_vlan_startip"), $thisDialog.find("#add_publicip_vlan_startip_errormsg"));   //required
				isValid &= validateIp("End IP Range", $thisDialog.find("#add_publicip_vlan_endip"), $thisDialog.find("#add_publicip_vlan_endip_errormsg"), true);  //optional
				if (!isValid) 
				    return;		
				    
				//$thisDialog.dialog("close"); 		//only close dialog when this action succeeds		
				$thisDialog.find("#spinning_wheel").fadeIn("slow");
				
				var vlan = trim($thisDialog.find("#add_publicip_vlan_vlan").val());
				if (isTagged) {
					vlan = "&vlan="+vlan;
				} else {
					vlan = "&vlan=untagged";
				}
								
				var scopeParams = "";
				if(dialogAddVlanForZone.find("#add_publicip_vlan_scope").val()=="account-specific")
				    scopeParams = "&domainId="+trim($thisDialog.find("#add_publicip_vlan_domain").val())+"&account="+trim($thisDialog.find("#add_publicip_vlan_account").val());    
								
				var type = "true";
				if (getNetworkType() == "vlan") 
				    type = trim($thisDialog.find("#add_publicip_vlan_type").val());
				    
				var gateway = trim($thisDialog.find("#add_publicip_vlan_gateway").val());
				var netmask = trim($thisDialog.find("#add_publicip_vlan_netmask").val());
				var startip = trim($thisDialog.find("#add_publicip_vlan_startip").val());
				var endip = trim($thisDialog.find("#add_publicip_vlan_endip").val());													
																				
				$.ajax({
				    data: createURL("command=createVlanIpRange&forVirtualNetwork="+type+"&zoneId="+zoneObj.id+vlan+scopeParams+"&gateway="+encodeURIComponent(gateway)+"&netmask="+encodeURIComponent(netmask)+"&startip="+encodeURIComponent(startip)+"&endip="+encodeURIComponent(endip)),
					dataType: "json",
					success: function(json) {	
					    $thisDialog.dialog("close");
					
					    var $template1 = $("#vlan_template").clone(); 							   
				        if(type == "false") //direct  
				            $template1.find("#vlan_type_icon").removeClass("virtual").addClass("direct");
				        else  //virtual
				  	        $template1.find("#vlan_type_icon").removeClass("direct").addClass("virtual");	
        				
        				vlanJsonToTemplate(json.createvlaniprangeresponse, $template1);	
				        $vlanContainer.prepend($template1);	
				        $template1.fadeIn("slow");
					},
				    error: function(XMLHttpResponse) {
				        handleErrorInDialog(XMLHttpResponse, $thisDialog);					         
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

function initAddSecondaryStorageButton($addButton) {
    $addButton.find("#label").text("Add Secondary Storage");
    $addButton.show();      
    $addButton.unbind("click").bind("click", function(event) {
        $("#zone_page").find("#tab_secondarystorage").click();    
        var zoneObj = $("#zone_page").find("#tab_content_details").data("jsonObj");       
        $("#dialog_add_secondarystorage").find("#zone_name").text(fromdb(zoneObj.name));   
   
        $("#dialog_add_secondarystorage")
	    .dialog('option', 'buttons', { 				    
		    "Add": function() { 
		        var thisDialog = $(this);
		    
			    // validate values					
			    var isValid = true;							    
			    isValid &= validateString("NFS Server", thisDialog.find("#nfs_server"), thisDialog.find("#nfs_server_errormsg"));	
			    isValid &= validatePath("Path", thisDialog.find("#path"), thisDialog.find("#path_errormsg"));					
			    if (!isValid) 
			        return;
			        
				thisDialog.dialog("close");	
								
				var $subgridItem = $("#secondary_storage_tab_template").clone(true);	
	            var $spinningWheel = $subgridItem.find("#spinning_wheel");
                $spinningWheel.find("#description").text("Adding Secondary Storage....");  
                $spinningWheel.show();  
                $subgridItem.find("#after_action_info_container").removeClass("error").addClass("success").hide();  
                $("#zone_page").find("#tab_content_secondarystorage").append($subgridItem.show());    
				     					  								            				
			    var zoneId = zoneObj.id;		
			    var nfs_server = trim(thisDialog.find("#nfs_server").val());		
			    var path = trim(thisDialog.find("#path").val());	    					    				    					   					
				var url = nfsURL(nfs_server, path);  
			    				  
			    $.ajax({
				    data: createURL("command=addSecondaryStorage&zoneId="+zoneId+"&url="+encodeURIComponent(url)),
				    dataType: "json",
				    success: function(json) {						        
				        secondaryStorageJSONToTemplate(json.addsecondarystorageresponse.secondarystorage[0], $subgridItem);
				        $spinningWheel.hide();   
	                    $subgridItem.find("#after_action_info").text("Secondary storage was added successfully.");
                        $subgridItem.find("#after_action_info_container").removeClass("error").addClass("success").show();  
				    },			
                    error: function(XMLHttpResponse) {	
                        handleErrorInSubgridItem(XMLHttpResponse, $subgridItem, "Add Secondary Storage");    
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

function populateForUpdateOSDialog(oscategoryid) {	
	$.ajax({
	    data: createURL("command=listOsCategories"+maxPageSize),
		dataType: "json",
		success: function(json) {
			var categories = json.listoscategoriesresponse.oscategory;
			var select = $("#dialog_update_os #host_os");								
			if (categories != null && categories.length > 0) {
				for (var i = 0; i < categories.length; i++) {
				    if(categories[i].id == oscategoryid) {				       
				        select.append("<option value='" + categories[i].id + "' selected >" + categories[i].name + "</option>"); 	
				    }    
				    else {
					    select.append("<option value='" + categories[i].id + "'>" + categories[i].name + "</option>"); 	
					}
		        }			    
			}
		}
	});
}

function nfsURL(server, path) {
    var url;
    if(server.indexOf("://")==-1)
	    url = "nfs://" + server + path;
	else
	    url = server + path;
	return url;
}

function iscsiURL(server, iqn, lun) {
    var url;
    if(server.indexOf("://")==-1)
	    url = "iscsi://" + server + iqn + "/" + lun;
	else
	    url = server + iqn + "/" + lun;
	return url;
}

function initAddZoneButton($midmenuAddLink1) {
    $midmenuAddLink1.find("#label").text("Add Zone");     
    $midmenuAddLink1.show();     
    $midmenuAddLink1.unbind("click").bind("click", function(event) {  
        $("#dialog_add_zone")
		.dialog('option', 'buttons', { 				
			"Add": function() { 
			    var thisDialog = $(this);
								
				// validate values
				var isValid = true;					
				isValid &= validateString("Name", thisDialog.find("#add_zone_name"), thisDialog.find("#add_zone_name_errormsg"));
				isValid &= validateIp("DNS 1", thisDialog.find("#add_zone_dns1"), thisDialog.find("#add_zone_dns1_errormsg"), false); //required
				isValid &= validateIp("DNS 2", thisDialog.find("#add_zone_dns2"), thisDialog.find("#add_zone_dns2_errormsg"), true);  //optional	
				isValid &= validateIp("Internal DNS 1", thisDialog.find("#add_zone_internaldns1"), thisDialog.find("#add_zone_internaldns1_errormsg"), false); //required
				isValid &= validateIp("Internal DNS 2", thisDialog.find("#add_zone_internaldns2"), thisDialog.find("#add_zone_internaldns2_errormsg"), true);  //optional	
				if (getNetworkType() != "vnet") {
					isValid &= validateString("Zone - Start VLAN Range", thisDialog.find("#add_zone_startvlan"), thisDialog.find("#add_zone_startvlan_errormsg"), false); //required
					isValid &= validateString("Zone - End VLAN Range", thisDialog.find("#add_zone_endvlan"), thisDialog.find("#add_zone_endvlan_errormsg"), true);        //optional
				}
				isValid &= validateCIDR("Guest CIDR", thisDialog.find("#add_zone_guestcidraddress"), thisDialog.find("#add_zone_guestcidraddress_errormsg"), false); //required
				if (!isValid) 
				    return;							
				
				thisDialog.dialog("close"); 
				
				var moreCriteria = [];	
				
				var name = trim(thisDialog.find("#add_zone_name").val());
				moreCriteria.push("&name="+todb(name));
				
				var dns1 = trim(thisDialog.find("#add_zone_dns1").val());
				moreCriteria.push("&dns1="+encodeURIComponent(dns1));
				
				var dns2 = trim(thisDialog.find("#add_zone_dns2").val());
				if (dns2 != null && dns2.length > 0) 
				    moreCriteria.push("&dns2="+encodeURIComponent(dns2));						
									
				var internaldns1 = trim(thisDialog.find("#add_zone_internaldns1").val());
				moreCriteria.push("&internaldns1="+encodeURIComponent(internaldns1));
				
				var internaldns2 = trim(thisDialog.find("#add_zone_internaldns2").val());
				if (internaldns2 != null && internaldns2.length > 0) 
				    moreCriteria.push("&internaldns2="+encodeURIComponent(internaldns2));						
				 											
				if (getNetworkType() != "vnet") {
					var vlanStart = trim(thisDialog.find("#add_zone_startvlan").val());	
					var vlanEnd = trim(thisDialog.find("#add_zone_endvlan").val());						
					if (vlanEnd != null && vlanEnd.length > 0) 
					    moreCriteria.push("&vlan=" + encodeURIComponent(vlanStart + "-" + vlanEnd));									
					else 							
						moreCriteria.push("&vlan=" + encodeURIComponent(vlanStart));		
				}					
				
				var guestcidraddress = trim(thisDialog.find("#add_zone_guestcidraddress").val());
				moreCriteria.push("&guestcidraddress="+encodeURIComponent(guestcidraddress));	
						
				
				var template = $("#leftmenu_zone_node_template").clone(true); 
			    var loadingImg = template.find(".adding_loading");										
			    var row_container = template.find("#row_container");  			   
			    var $zoneTree = $("#leftmenu_zone_tree").find("#tree_container");		     			
			    $zoneTree.prepend(template);						            
                loadingImg.show();  
                row_container.hide();             
	            template.fadeIn("slow");
                $.ajax({
			        data: createURL("command=createZone"+moreCriteria.join("")),
				    dataType: "json",
				    success: function(json) {
					    var item = json.createzoneresponse;					    
					    zoneJSONToTreeNode(item, template);	
					    loadingImg.hide();	
					    row_container.show();		        
				    },
			        error: function(XMLHttpResponse) {
			            handleError(XMLHttpResponse);
			            template.slideUp("slow", function() {
						    $(this).remove();
					    });
			        }
			    });
			}, 
			"Cancel": function() { 
			    var thisDialog = $(this);
				thisDialog.dialog("close"); 
				cleanErrMsg(thisDialog.find("#add_zone_name"), thisDialog.find("#add_zone_name_errormsg"));
				cleanErrMsg(thisDialog.find("#add_zone_dns1"), thisDialog.find("#add_zone_dns1_errormsg"));
				cleanErrMsg(thisDialog.find("#add_zone_dns2"), thisDialog.find("#add_zone_dns2_errormsg"));
				cleanErrMsg(thisDialog.find("#add_zone_internaldns1"), thisDialog.find("#add_zone_internaldns1_errormsg"));
				cleanErrMsg(thisDialog.find("#add_zone_internaldns2"), thisDialog.find("#add_zone_internaldns2_errormsg"));
				cleanErrMsg(thisDialog.find("#add_zone_startvlan"), thisDialog.find("#add_zone_startvlan_errormsg"));
				cleanErrMsg(thisDialog.find("#add_zone_guestcidraddress"), thisDialog.find("#add_zone_guestcidraddress_errormsg"));
			} 
		}).dialog("open");        
        return false;
    });     
}

function initAddPodButton($midmenuAddLink1) {
    $midmenuAddLink1.find("#label").text("Add Pod"); 
    $midmenuAddLink1.show();     
    $midmenuAddLink1.unbind("click").bind("click", function(event) {   
        var zoneObj = $("#zone_page").find("#tab_content_details").data("jsonObj");   	
        $("#dialog_add_pod").find("#add_pod_zone_name").text(fromdb(zoneObj.name));
        $("#dialog_add_pod #add_pod_name, #dialog_add_pod #add_pod_cidr, #dialog_add_pod #add_pod_startip, #dialog_add_pod #add_pod_endip, #add_pod_gateway").val("");
		
        $("#dialog_add_pod")
        .dialog('option', 'buttons', { 				
	        "Add": function() {		
	            var thisDialog = $(this);
						
		        // validate values
		        var isValid = true;					
		        isValid &= validateString("Name", thisDialog.find("#add_pod_name"), thisDialog.find("#add_pod_name_errormsg"));
		        isValid &= validateCIDR("CIDR", thisDialog.find("#add_pod_cidr"), thisDialog.find("#add_pod_cidr_errormsg"));	
		        isValid &= validateIp("Start IP Range", thisDialog.find("#add_pod_startip"), thisDialog.find("#add_pod_startip_errormsg"));  //required
		        isValid &= validateIp("End IP Range", thisDialog.find("#add_pod_endip"), thisDialog.find("#add_pod_endip_errormsg"), true);  //optional
		        isValid &= validateIp("Gateway", thisDialog.find("#add_pod_gateway"), thisDialog.find("#add_pod_gateway_errormsg"));  //required when creating
		        if (!isValid) 
		            return;			
                
                thisDialog.dialog("close"); 
                  
                var name = trim(thisDialog.find("#add_pod_name").val());
		        var cidr = trim(thisDialog.find("#add_pod_cidr").val());
		        var startip = trim(thisDialog.find("#add_pod_startip").val());
		        var endip = trim(thisDialog.find("#add_pod_endip").val());	    //optional
		        var gateway = trim(thisDialog.find("#add_pod_gateway").val());			

                var array1 = [];
                array1.push("&zoneId="+zoneObj.id);
                array1.push("&name="+todb(name));
                array1.push("&cidr="+encodeURIComponent(cidr));
                array1.push("&startIp="+encodeURIComponent(startip));
                if (endip != null && endip.length > 0)
                    array1.push("&endIp="+encodeURIComponent(endip));
                array1.push("&gateway="+encodeURIComponent(gateway));			
								    
		        var template = $("#leftmenu_pod_node_template").clone(true);
		        var loadingImg = template.find(".adding_loading");										
		        var row_container = template.find("#row_container");        				
		        $("#zone_" + zoneObj.id + " #zone_content").show();	
		        $("#zone_" + zoneObj.id + " #pods_container").prepend(template.show());						
		        $("#zone_" + zoneObj.id + " #zone_expand").removeClass().addClass("zonetree_openarrows");									            
                loadingImg.show();  
                row_container.hide();             
                template.fadeIn("slow");
				
		        $.ajax({
		          data: createURL("command=createPod"+array1.join("")), 
			        dataType: "json",
			        success: function(json) {
				        var item = json.createpodresponse; 	
			            podJSONToTreeNode(item, template);	
				        loadingImg.hide(); 								                            
                        row_container.show();
                        
                        forceLogout = false;  // We don't force a logout if pod(s) exit.
				        if (forceLogout) {
					        $("#dialog_confirmation")
						        .html("<p>You have successfully added your first Zone and Pod.  After clicking 'OK', this UI will automatically refresh to give you access to the rest of cloud features.</p>")
						        .dialog('option', 'buttons', { 
							        "OK": function() { 											
								        $(this).dialog("close");
								        window.location.reload();
							        } 
						        }).dialog("open");
				        }
			        },
		            error: function(XMLHttpResponse) {	
		                handleError(XMLHttpResponse);			    
			            template.slideUp("slow", function() {
					        $(this).remove();
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

function initAddHostButton($midmenuAddLink1) {
    $midmenuAddLink1.find("#label").text("Add Host"); 
    $midmenuAddLink1.show();
    $midmenuAddLink1.unbind("click").bind("click", function(event) {     
        dialogAddHost = $("#dialog_add_host");          
        var podObj = $("#pod_page").find("#tab_content_details").data("jsonObj");   
        dialogAddHost.find("#zone_name").text(fromdb(podObj.zonename));  
        dialogAddHost.find("#pod_name").text(fromdb(podObj.name)); 
        dialogAddHost.find("#new_cluster_name").val("");
                          
        $.ajax({
	       data: createURL("command=listClusters&podid="+podObj.id+maxPageSize),
            dataType: "json",
            success: function(json) {			            
                var items = json.listclustersresponse.cluster;
                var clusterSelect = dialogAddHost.find("#cluster_select").empty();		
                if(items != null && items.length > 0) {			                
                    for(var i=0; i<items.length; i++) 			                    
                        clusterSelect.append("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>");		      
                    dialogAddHost.find("input[value=existing_cluster_radio]").attr("checked", true);
                }
                else {
				    clusterSelect.append("<option value='-1'>None Available</option>");
                    dialogAddHost.find("input[value=new_cluster_radio]").attr("checked", true);
                }
            }
        });           
	        	    
        dialogAddHost
        .dialog('option', 'buttons', { 				
	        "Add": function() { 
	            var $thisDialog = $(this);	
	            $thisDialog.find("#info_container").hide();
	            			   
		        var clusterRadio = $thisDialog.find("input[name=cluster]:checked").val();				
			
		        // validate values
		        var isValid = true;									
		        isValid &= validateString("Host name", $thisDialog.find("#host_hostname"), $thisDialog.find("#host_hostname_errormsg"));
		        isValid &= validateString("User name", $thisDialog.find("#host_username"), $thisDialog.find("#host_username_errormsg"));
		        isValid &= validateString("Password", $thisDialog.find("#host_password"), $thisDialog.find("#host_password_errormsg"));						
		        if (!isValid) 
		            return;
		            
				//$thisDialog.dialog("close");   //only close dialog when this action succeeds		
				$thisDialog.find("#spinning_wheel").fadeIn("slow"); 				
				
		        var array1 = [];    
		        array1.push("&zoneId="+podObj.zoneid);
		        array1.push("&podId="+podObj.id);
						      
		        var username = trim($thisDialog.find("#host_username").val());
		        array1.push("&username="+encodeURIComponent(username));
				
		        var password = trim($thisDialog.find("#host_password").val());
		        array1.push("&password="+encodeURIComponent(password));
											
			    if(clusterRadio == "new_cluster_radio") {
		            var newClusterName = trim($thisDialog.find("#new_cluster_name").val());
		            array1.push("&clustername="+todb(newClusterName));				    
		        }
		        else if(clusterRadio == "existing_cluster_radio") {			            
		            var clusterId = $thisDialog.find("#cluster_select").val();
				    // We will default to no cluster if someone selects Join Cluster with no cluster available.
				    if (clusterId != '-1') {
					    array1.push("&clusterid="+clusterId);
				    }
		        }				
				
		        var hostname = trim($thisDialog.find("#host_hostname").val());
		        var url;					
		        if(hostname.indexOf("http://")==-1)
		            url = "http://" + todb(hostname);
		        else
		            url = hostname;
		        array1.push("&url="+encodeURIComponent(url));
									
		        //var $midmenuItem1 = beforeAddingMidMenuItem() ;    				
		        
		        $.ajax({
			       data: createURL("command=addHost" + array1.join("")),
			        dataType: "json",
			        success: function(json) {
			            $thisDialog.dialog("close");
					
					    var $midmenuItem1 = $("#midmenu_item").clone();
                        $("#midmenu_container").append($midmenuItem1.fadeIn("slow"));
                        var items = json.addhostresponse.host;				            			      										   
					    hostToMidmenu(items[0], $midmenuItem1);
	                    bindClickToMidMenu($midmenuItem1, hostToRightPanel, hostGetMidmenuId); 
			           
                        if(items.length > 1) { 
                            for(var i=1; i<items.length; i++) {                                    
                                var $midmenuItem2 = $("#midmenu_item").clone();
                                hostToMidmenu(items[i], $midmenuItem2);
                                bindClickToMidMenu($midmenuItem2, hostToRightPanel, hostGetMidmenuId); 
                                $("#midmenu_container").append($midmenuItem2.fadeIn("slow"));                                   
                            }	
                        }                                
                        
                        if(clusterRadio == "new_cluster_radio")
                            $thisDialog.find("#new_cluster_name").val("");
			        },			
                    error: function(XMLHttpResponse) {		                   
                        handleErrorInDialog(XMLHttpResponse, $thisDialog);					    
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

function initAddPrimaryStorageButton($midmenuAddLink2) {
    $midmenuAddLink2.find("#label").text("Add Primary Storage"); 
    $midmenuAddLink2.show();   
    $midmenuAddLink2.unbind("click").bind("click", function(event) {   
        var podObj = $("#pod_page").find("#tab_content_details").data("jsonObj");
        dialogAddPool = $("#dialog_add_pool");  
        dialogAddPool.find("#zone_name").text(fromdb(podObj.zonename));  
        dialogAddPool.find("#pod_name").text(fromdb(podObj.name)); 
                                
        var clusterSelect = $("#dialog_add_pool").find("#pool_cluster").empty();			            
	    $.ajax({
		    data: createURL("command=listClusters&podid=" + podObj.id),
	        dataType: "json",
	        success: function(json) {				                        
	            var items = json.listclustersresponse.cluster;
	            if(items != null && items.length > 0) {				                		                
	                for(var i=0; i<items.length; i++) 			                    
	                    clusterSelect.append("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>");		                
	            }			            
	        }
	    });		   
        
        $("#dialog_add_pool")
	    .dialog('option', 'buttons', { 				    
		    "Add": function() { 	
		    	var thisDialog = $(this);
		    	
			    // validate values
				var protocol = thisDialog.find("#add_pool_protocol").val();
				
			    var isValid = true;						    
			    isValid &= validateDropDownBox("Cluster", thisDialog.find("#pool_cluster"), thisDialog.find("#pool_cluster_errormsg"), false);  //required, reset error text					    				
			    isValid &= validateString("Name", thisDialog.find("#add_pool_name"), thisDialog.find("#add_pool_name_errormsg"));
			    isValid &= validateString("Server", thisDialog.find("#add_pool_nfs_server"), thisDialog.find("#add_pool_nfs_server_errormsg"));	
				if (protocol == "nfs") {
					isValid &= validateString("Path", thisDialog.find("#add_pool_path"), thisDialog.find("#add_pool_path_errormsg"));	
				} else {
					isValid &= validateString("Target IQN", thisDialog.find("#add_pool_iqn"), thisDialog.find("#add_pool_iqn_errormsg"));	
					isValid &= validateString("LUN #", thisDialog.find("#add_pool_lun"), thisDialog.find("#add_pool_lun_errormsg"));	
				}
				isValid &= validateString("Tags", thisDialog.find("#add_pool_tags"), thisDialog.find("#add_pool_tags_errormsg"), true);	//optional
			    if (!isValid) 
			        return;
			        
			    thisDialog.dialog("close");    
				
				var $midmenuItem1 = beforeAddingMidMenuItem() ;    	
				
				var array1 = [];
				array1.push("&zoneId="+podObj.zoneid);
		        array1.push("&podId="+podObj.id);
				
				var clusterId = thisDialog.find("#pool_cluster").val();
			    array1.push("&clusterid="+clusterId);	
				
			    var name = trim(thisDialog.find("#add_pool_name").val());
			    array1.push("&name="+todb(name));
			    
			    var server = trim(thisDialog.find("#add_pool_nfs_server").val());						
				
				var url = null;
				if (protocol == "nfs") {
					var path = trim(thisDialog.find("#add_pool_path").val());
					if(path.substring(0,1)!="/")
						path = "/" + path; 
					url = nfsURL(server, path);
				} else {
					var iqn = trim(thisDialog.find("#add_pool_iqn").val());
					if(iqn.substring(0,1)!="/")
						iqn = "/" + iqn; 
					var lun = trim(thisDialog.find("#add_pool_lun").val());
					url = iscsiURL(server, iqn, lun);
				}
				array1.push("&url="+encodeURIComponent(url));
				
			    var tags = trim(thisDialog.find("#add_pool_tags").val());
				if(tags != null && tags.length > 0)
				    array1.push("&tags="+todb(tags));				    
			    
			    $.ajax({
				    data: createURL("command=createStoragePool" + array1.join("")),
				    dataType: "json",
				    success: function(json) {					        
				        var item = json.createstoragepoolresponse;				            			      										   
					    primarystorageToMidmenu(item, $midmenuItem1);
	                    bindClickToMidMenu($midmenuItem1, primarystorageToRightPanel, primarystorageGetMidmenuId);  
	                    afterAddingMidMenuItem($midmenuItem1, true);
				    },			
                    error: function(XMLHttpResponse) {	
                        handleErrorInMidMenu(XMLHttpResponse, $midmenuItem1);			                        					    
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

function secondaryStorageJSONToTemplate(json, template) {
    template.data("jsonObj", json);
    template.attr("id", "secondaryStorage_"+json.id).data("secondaryStorageId", json.id);   	
   	template.find("#id").text(json.id);
   	template.find("#title").text(fromdb(json.name));
   	template.find("#name").text(fromdb(json.name));
   	template.find("#zonename").text(fromdb(json.zonename));	
	template.find("#type").text(json.type);	
    template.find("#ipaddress").text(json.ipaddress);
    template.find("#state").text(json.state);
    template.find("#version").text(json.version); 
    setDateField(json.disconnected, template.find("#disconnected"));
    
    var $actionLink = template.find("#secondarystorage_action_link");		
	$actionLink.bind("mouseover", function(event) {
        $(this).find("#secondarystorage_action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {
        $(this).find("#secondarystorage_action_menu").hide();    
        return false;
    });		
	
	var $actionMenu = $actionLink.find("#secondarystorage_action_menu");
    $actionMenu.find("#action_list").empty();	
    
    buildActionLinkForSubgridItem("Delete Secondary Storage", secondarystorageActionMap, $actionMenu, template);	    
}   

function bindEventHandlerToDialogAddPool() {    
    $("#dialog_add_pool").find("#add_pool_protocol").change(function(event) {
		if ($(this).val() == "iscsi") {
			$("#dialog_add_pool #add_pool_path_container").hide();
			$("#dialog_add_pool #add_pool_iqn_container, #dialog_add_pool #add_pool_lun_container").show();
		} else {
			$("#dialog_add_pool #add_pool_path_container").show();
			$("#dialog_add_pool #add_pool_iqn_container, #dialog_add_pool #add_pool_lun_container").hide();
		}
	});		
}

function bindEventHandlerToDialogAddVlanForZone() {
    //direct VLAN shows only "tagged" option while public VLAN shows both "tagged" and "untagged" option. 		
	var dialogAddVlanForZone = $("#dialog_add_vlan_for_zone");
			
	dialogAddVlanForZone.find("#add_publicip_vlan_type").change(function(event) {
	    var addPublicipVlanTagged = dialogAddVlanForZone.find("#add_publicip_vlan_tagged").empty();
	   	
		// default value of "#add_publicip_vlan_scope" is "zone-wide". Calling change() will hide "#add_publicip_vlan_domain_container", "#add_publicip_vlan_account_container". 
		dialogAddVlanForZone.find("#add_publicip_vlan_scope").change(); 
		
		if ($(this).val() == "false") { //direct VLAN (only tagged option)		
			addPublicipVlanTagged.append('<option value="tagged">tagged</option>');
							
			dialogAddVlanForZone.find("#add_publicip_vlan_vlan_container").show();			
			dialogAddVlanForZone.find("#add_publicip_vlan_pod_container").hide();
			
		} else { //public VLAN	
			addPublicipVlanTagged.append('<option value="untagged">untagged</option>').append('<option value="tagged">tagged</option>');	
			
			if (dialogAddVlanForZone.find("#add_publicip_vlan_tagged") == "tagged") {
				dialogAddVlanForZone.find("#add_publicip_vlan_vlan_container").show();
				dialogAddVlanForZone.find("#add_publicip_vlan_pod_container").hide();
			} else {
				dialogAddVlanForZone.find("#add_publicip_vlan_vlan_container").hide();
				dialogAddVlanForZone.find("#add_publicip_vlan_pod_container").hide();
			}
		} 
		return false;
	});
			
	if (getNetworkType() != "vnet") {
		dialogAddVlanForZone.find("#add_publicip_vlan_tagged").change(function(event) {			
			// default value of "#add_publicip_vlan_scope" is "zone-wide". Calling change() will hide "#add_publicip_vlan_domain_container", "#add_publicip_vlan_account_container". 
			dialogAddVlanForZone.find("#add_publicip_vlan_scope").change(); 	
			
			if (dialogAddVlanForZone.find("#add_publicip_vlan_type").val() == "false") { //direct VLAN (only tagged option)						
				dialogAddVlanForZone.find("#add_publicip_vlan_vlan_container").show();				
				dialogAddVlanForZone.find("#add_publicip_vlan_pod_container").hide();	
			} else { //public VLAN		
				if ($(this).val() == "tagged") {
					dialogAddVlanForZone.find("#add_publicip_vlan_vlan_container").show();
					dialogAddVlanForZone.find("#add_publicip_vlan_pod_container").hide();
				} else {
					dialogAddVlanForZone.find("#add_publicip_vlan_vlan_container").hide();
					dialogAddVlanForZone.find("#add_publicip_vlan_pod_container").hide();
				}
			}
			return false;
		});
	} else {
		dialogAddVlanForZone.find("#add_publicip_vlan_container").hide();
	}
	
	dialogAddVlanForZone.find("#add_publicip_vlan_scope").change(function(event) {	   
	    if($(this).val() == "zone-wide") {
	        dialogAddVlanForZone.find("#add_publicip_vlan_domain_container").hide();
			dialogAddVlanForZone.find("#add_publicip_vlan_account_container").hide();    
	    } else { // account-specific
	        dialogAddVlanForZone.find("#add_publicip_vlan_domain_container").show();
			dialogAddVlanForZone.find("#add_publicip_vlan_account_container").show();    
	    }		    
	    return false;
	});
}

var hostActionMap = {  
    "Enable Maintenance Mode": {              
        isAsyncJob: true,
        asyncJobResponse: "preparehostformaintenanceresponse",
        dialogBeforeActionFn: doEnableMaintenanceMode,
        inProcessText: "Enabling Maintenance Mode....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {            
            hostToMidmenu(json.queryasyncjobresultresponse.host[0], $midmenuItem1);
            hostToRightPanel($midmenuItem1);            
            $("#right_panel_content #after_action_info").text("We are actively enabling maintenance on your host. Please refresh periodically for an updated status."); 
        }
    },
    "Cancel Maintenance Mode": {              
        isAsyncJob: true,
        asyncJobResponse: "cancelhostmaintenanceresponse",
        dialogBeforeActionFn: doCancelMaintenanceMode,
        inProcessText: "Cancelling Maintenance Mode....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {            
            hostToMidmenu(json.queryasyncjobresultresponse.host[0], $midmenuItem1);
            hostToRightPanel($midmenuItem1);            
            $("#right_panel_content #after_action_info").text("We are actively cancelling your scheduled maintenance.  Please refresh periodically for an updated status."); 
        }
    },
    "Force Reconnect": {              
        isAsyncJob: true,
        asyncJobResponse: "reconnecthostresponse",
        dialogBeforeActionFn: doForceReconnect,
        inProcessText: "Reconnecting....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {
            hostToMidmenu(json.queryasyncjobresultresponse.host[0], $midmenuItem1);
            hostToRightPanel($midmenuItem1);            
            $("#right_panel_content #after_action_info").text("We are actively reconnecting your host.  Please refresh periodically for an updated status."); 
        }
    },
    "Remove Host": {              
        isAsyncJob: false,        
        dialogBeforeActionFn: doRemoveHost,
        inProcessText: "Removing Host....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {    
            $midmenuItem1.remove();
            clearRightPanel();
            hostClearRightPanel();
        }
    },    
    "Update OS Preference": {              
        isAsyncJob: false,        
        dialogBeforeActionFn: doUpdateOSPreference,
        inProcessText: "Updating OS Preference....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {     
            //call listHosts API before bug 6650 ("updateHost API should return an embedded object like what listHosts API does") is fixed.
            $.ajax({
                data: createURL("command=listHosts&id="+id),
                dataType: "json",
                success: function(json) {  
                    hostToMidmenu(json.listhostsresponse.host[0], $midmenuItem1);      
                    hostToRightPanel($midmenuItem1);                     
                }
            });            
        }
    },          
} 

function doEnableMaintenanceMode($actionLink, $detailsTab, $midmenuItem1){ 
    var jsonObj = $detailsTab.data("jsonObj");
       
    $("#dialog_confirmation_enable_maintenance")
    .dialog("option", "buttons", {	                    
         "OK": function() {
             $(this).dialog("close");      
             var id = jsonObj.id;
             var apiCommand = "command=prepareHostForMaintenance&id="+id;
    	     doActionToDetailsTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
         },
         "Cancel": function() {	                         
             $(this).dialog("close");
         }
    }).dialog("open");     
} 

function doCancelMaintenanceMode($actionLink, $detailsTab, $midmenuItem1){ 
    var jsonObj = $detailsTab.data("jsonObj");
       
    $("#dialog_confirmation_cancel_maintenance")
    .dialog("option", "buttons", {	                    
         "OK": function() {
             $(this).dialog("close");      
             var id = jsonObj.id;
             var apiCommand = "command=cancelHostMaintenance&id="+id;
    	     doActionToDetailsTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
         },
         "Cancel": function() {	                         
             $(this).dialog("close");
         }
    }).dialog("open");     
} 

function doForceReconnect($actionLink, $detailsTab, $midmenuItem1){ 
    var jsonObj = $detailsTab.data("jsonObj");
       
    $("#dialog_confirmation_force_reconnect")
    .dialog("option", "buttons", {	                    
         "OK": function() {
             $(this).dialog("close");      
             var id = jsonObj.id;
             var apiCommand = "command=reconnectHost&id="+id;
    	     doActionToDetailsTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
         },
         "Cancel": function() {	                         
             $(this).dialog("close");
         }
    }).dialog("open");     
} 

function doRemoveHost($actionLink, $detailsTab, $midmenuItem1){ 
    var jsonObj = $detailsTab.data("jsonObj");
       
    $("#dialog_confirmation_remove_host")
    .dialog("option", "buttons", {	                    
         "OK": function() {
             $(this).dialog("close");      
             var id = jsonObj.id;
             var apiCommand = "command=deleteHost&id="+id;
    	     doActionToDetailsTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
         },
         "Cancel": function() {	                         
             $(this).dialog("close");
         }
    }).dialog("open");     
} 

function doUpdateOSPreference($actionLink, $detailsTab, $midmenuItem1){ 
    var jsonObj = $detailsTab.data("jsonObj");
       
    $("#dialog_update_os")
    .dialog("option", "buttons", {	                    
        "Update": function() {
            $(this).dialog("close");
	        var osId = $("#dialog_update_os #host_os").val();
	        var osName = $("#dialog_update_os #host_os option:selected").text();
	        var category = "";
	        if (osId.length > 0) {
		        category = "&osCategoryId="+osId;
	        }
	        var id = jsonObj.id;
    		    
            var apiCommand = "command=updateHost&id="+id+category;
    	    doActionToDetailsTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
        },
        "Cancel": function() {	                         
            $(this).dialog("close");
        }
    }).dialog("open");     
} 

var primarystorageActionMap = {
    "Delete Primary Storage": {              
        isAsyncJob: false,        
        dialogBeforeActionFn: doDeletePrimaryStorage,
        inProcessText: "Deleting Primary Storage....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {   
            $midmenuItem1.remove();
            clearRightPanel();
            primarystorageClearRightPanel();
        }
    }
}

function doDeletePrimaryStorage($actionLink, $detailsTab, $midmenuItem1){ 
    var jsonObj = $detailsTab.data("jsonObj");
       
    $("#dialog_confirmation_delete_primarystorage")
    .dialog("option", "buttons", {	                    
         "OK": function() {
             $(this).dialog("close");      
             var id = jsonObj.id;
             var apiCommand = "command=deleteStoragePool&id="+id;
    	     doActionToDetailsTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
         },
         "Cancel": function() {	                         
             $(this).dialog("close");
         }
    }).dialog("open");     
} 

var secondarystorageActionMap = {
    "Delete Secondary Storage": {   
        isAsyncJob: false,   
        dialogBeforeActionFn: doDeleteSecondaryStorage,       
        inProcessText: "Deleting Secondary Storaget....",
        afterActionSeccessFn: function(json, id, $subgridItem) {                        
            $subgridItem.slideUp("slow", function() {
                $(this).remove();
            });
        }
    } 
}

function doDeleteSecondaryStorage($actionLink, $subgridItem) { 
    var jsonObj = $subgridItem.data("jsonObj");
       
    $("#dialog_confirmation_delete_secondarystorage")	
	.dialog('option', 'buttons', { 						
		"Confirm": function() { 
		    var thisDialog = $(this);	
			thisDialog.dialog("close");       	                                             
         
            var name = thisDialog.find("#name").val();	                
             
            var id = jsonObj.id;
            var apiCommand = "command=deleteHost&id="+id;    	
    	    doActionToSubgridItem(id, $actionLink, apiCommand, $subgridItem);				
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		} 
	}).dialog("open");
}

var zoneActionMap = {
    "Delete Zone": {  
        api: "deleteZone",            
        isAsyncJob: false,        
        inProcessText: "Deleting Zone....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {   
            $midmenuItem1.slideUp(function() {
                $(this).remove();
            });
            clearRightPanel();
            zoneJsonClearRightPanel();
        }
    }
}

var podActionMap = {
    "Delete Pod": {  
        api: "deletePod",            
        isAsyncJob: false,        
        inProcessText: "Deleting Pod....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {       
            $midmenuItem1.slideUp(function() {
                $(this).remove();
            });
            clearRightPanel();
            podJsonClearRightPanel();
        }
    }
}

