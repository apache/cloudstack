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

function networkGetSearchParams() {
    var moreCriteria = [];	
    
	var searchInput = $("#basic_search").find("#search_input").val();	 
    if (searchInput != null && searchInput.length > 0) {	           
        moreCriteria.push("&keyword="+todb(searchInput));	       
    }     

	var $advancedSearchPopup = getAdvancedSearchPopupInSearchContainer();
	if ($advancedSearchPopup.length > 0 && $advancedSearchPopup.css("display") != "none" ) {			
        if ($advancedSearchPopup.find("#adv_search_domain_li").css("display") != "none") {		
		    var domainId = $advancedSearchPopup.find("#adv_search_domain").val();		
		    if (domainId!=null && domainId.length > 0) 
			    moreCriteria.push("&domainid="+domainId);	
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

var zoneObj;  
function afterLoadNetworkJSP($leftmenuItem1) {      
    var $topButtonContainer = clearButtonsOnTop();			    	       
	$("#top_buttons").appendTo($("#top_button_container").empty()); 
        
    //switch between different tabs - Public Network page
    var $publicNetworkPage = $("#public_network_page");
    var tabArray = [$publicNetworkPage.find("#tab_details"), $publicNetworkPage.find("#tab_ipallocation"), $publicNetworkPage.find("#tab_firewall"), $publicNetworkPage.find("#tab_loadbalancer")];
    var tabContentArray = [$publicNetworkPage.find("#tab_content_details"), $publicNetworkPage.find("#tab_content_ipallocation"), $publicNetworkPage.find("#tab_content_firewall"), $publicNetworkPage.find("#tab_content_loadbalancer")];
    var afterSwitchFnArray = [publicNetworkJsonToDetailsTab, publicNetworkJsonToIpAllocationTab, publicNetworkJsonToFirewallTab, publicNetworkJsonToLoadBalancerTab];
    switchBetweenDifferentTabs(tabArray, tabContentArray, afterSwitchFnArray);  
    
    //switch between different tabs - Direct Network page
    var $directNetworkPage = $("#direct_network_page");
    var tabArray = [$directNetworkPage.find("#tab_details"), $directNetworkPage.find("#tab_ipallocation")];
    var tabContentArray = [$directNetworkPage.find("#tab_content_details"), $directNetworkPage.find("#tab_content_ipallocation")];
    var afterSwitchFnArray = [directNetworkJsonToDetailsTab, directNetworkJsonToIpAllocationTab];
    switchBetweenDifferentTabs(tabArray, tabContentArray, afterSwitchFnArray);    
    
    //initialize dialog
    initDialog("dialog_add_iprange_to_publicnetwork");        
    initDialog("dialog_add_external_firewall");        
    initDialog("dialog_add_load_balancer");        
    initDialog("dialog_add_network_for_zone");
    initDialog("dialog_add_iprange_to_directnetwork");    
     
    networkPopulateMiddleMenu($leftmenuItem1);  
    bindAddNetworkButton($("#add_network_button"));     
}

function networkPopulateMiddleMenu($leftmenuItem1) {
    zoneObj = $leftmenuItem1.data("jsonObj");    
    if(zoneObj == null) 
	    return;	  
         
    //populate items into middle menu  
    var $midmenuContainer = $("#midmenu_container").empty();   

    //direct network
    listMidMenuItems2(("listNetworks&type=Direct&zoneId="+zoneObj.id), networkGetSearchParams, "listnetworksresponse", "network", directNetworkToMidmenu, directNetworkToRightPanel, directNetworkGetMidmenuId, false, 1);
    		    
    //public network   
    $midmenuContainer.find("#midmenu_container_no_items_available").remove();  //There is always at least one item (i.e. public network) in middle menu. So, "no items available" shouldn't be in middle menu even there is zero direct network item in middle menu.   
    $.ajax({
        data: createURL("command=listNetworks&trafficType=Public&isSystem=true&zoneId="+zoneObj.id),
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
                $midmenuContainer.prepend($midmenuItem1.show());    //prepend public network on the top of middle menu
                $midmenuItem1.click();  
            }
        }
    });           
}

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
    $midmenuItem1.find("#second_row").text("VLAN: Multiple");   
}

function publicNetworkToRightPanel($midmenuItem1) {      
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);  
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);  
            
    $("#public_network_page").show();    
    bindAddIpRangeToPublicNetworkButton($("#add_iprange_button"), $midmenuItem1);
    bindAddExternalFirewallButton($("#add_external_firewall_button"), $midmenuItem1);
    bindAddLoadBalancerButton($("#add_load_balancer_button"), $midmenuItem1);
    
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
	
	$thisTab.find("#grid_header_title").text(fromdb(jsonObj.networkofferingdisplaytext));	
		
	$thisTab.find("#id").text(fromdb(jsonObj.id));		
	$thisTab.find("#state").text(fromdb(jsonObj.state));		
	$thisTab.find("#traffictype").text(fromdb(jsonObj.traffictype));	
	$thisTab.find("#broadcastdomaintype").text(fromdb(jsonObj.broadcastdomaintype));	
	setBooleanReadField(jsonObj.isshared, $thisTab.find("#isshared"));
	setBooleanReadField(jsonObj.issystem, $thisTab.find("#issystem"));
	$thisTab.find("#networkofferingname").text(fromdb(jsonObj.networkofferingname));	
	$thisTab.find("#networkofferingdisplaytext").text(fromdb(jsonObj.networkofferingdisplaytext));	
	$thisTab.find("#networkofferingid").text(fromdb(jsonObj.networkofferingid));	
	$thisTab.find("#related").text(fromdb(jsonObj.related));	
	$thisTab.find("#zoneid").text(fromdb(jsonObj.zoneid));	
	$thisTab.find("#dns1").text(fromdb(jsonObj.dns1));	
	$thisTab.find("#dns2").text(fromdb(jsonObj.dns2));	
	$thisTab.find("#domainid").text(fromdb(jsonObj.domainid));	
	$thisTab.find("#account").text(fromdb(jsonObj.account));	
	
	//actions ***   
    var $actionLink = $thisTab.find("#action_link"); 
    bindActionLink($actionLink);
    /*
    $actionLink.bind("mouseover", function(event) {	    
        $(this).find("#action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {       
        $(this).find("#action_menu").hide();    
        return false;
    });	
    */
         
    var $actionMenu = $thisTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();   
	$actionMenu.find("#action_list").append($("#no_available_actions").clone().show());	   
			        
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
		data: createURL("command=listVlanIpRanges&zoneid="+ jsonObj.zoneid+"&forvirtualnetwork=true"), //don't need networkid because one zone has only one public network
		dataType: "json",		
		success: function(json) {		    
		    var items = json.listvlaniprangesresponse.vlaniprange;		    
		    var $container = $thisTab.find("#tab_container").empty();
		    var $template = $("#public_iprange_template");
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
    $template.data("jsonObj", jsonObj);
    $template.attr("id", "publicNetworkIprange_" + jsonObj.id);
    
    var ipRange = getIpRange(fromdb(jsonObj.startip), fromdb(jsonObj.endip));
    $template.find("#grid_header_title").text(ipRange);
    
    $template.find("#id").text(jsonObj.id);
    $template.find("#vlan").text(jsonObj.vlan);    
    $template.find("#gateway").text(jsonObj.gateway);
    $template.find("#netmask").text(jsonObj.netmask);    
    $template.find("#iprange").text(ipRange);     
    $template.find("#domain").text(jsonObj.domain);
    $template.find("#account").text(jsonObj.account);
   
    var $actionLink = $template.find("#action_link");	
    bindActionLink($actionLink);
    /*	
	$actionLink.bind("mouseover", function(event) {
        $(this).find("#action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {
        $(this).find("#action_menu").hide();    
        return false;
    });		
	*/
	
	var $actionMenu = $actionLink.find("#action_menu");
    $actionMenu.find("#action_list").empty();	
       
    buildActionLinkForSubgridItem("label.action.delete.IP.range", publicNetworkIpRangeActionMap, $actionMenu, $template);	
}

function publicNetworkJsonToFirewallTab() {  
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null)
        return;
    
    var jsonObj = $midmenuItem1.data("jsonObj");    
    if(jsonObj == null) 
	    return;	  
	    
	var $thisTab = $("#right_panel_content #public_network_page #tab_content_firewall");      
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   
        
    $.ajax({
        data: createURL("command=listExternalFirewalls&zoneid="+zoneObj.id),
        dataType: "json",
        success: function(json) {            
            var items = json.listexternalfirewallsresponse.externalfirewall;
		    var $container = $thisTab.find("#tab_container").empty();
		    var $template = $("#externalfirewall_template");
		    if(items != null && items.length > 0) {		        
		        for(var i=0; i<items.length; i++) {
		            var $newTemplate = $template.clone();
		            publicNetworkFirewallJsonToTemplate(items[i], $newTemplate);
		            $container.append($newTemplate.show());
		        }
		    }		    
		    $thisTab.find("#tab_container").show(); 
            $thisTab.find("#tab_spinning_wheel").hide();   
        }
    });       
}

/*
function publicNetworkParseUrl(url, $template) {
    if(url == null) 
        return;
    
    var array1 = url.split("?");
    if(array1.length >= 2) {
        var $subTemplate = $("#grid_row_template");  
                 
        var ip = array1[0];    
        $template.find("#grid_header_title").text(ip);            
        var $newSubTemplate = $subTemplate.clone();              
        $newSubTemplate.attr("id", ("grid_row_template_ip")).addClass("even");
        $newSubTemplate.find("#label").text("IP: ");
        $newSubTemplate.find("#value").text(ip);                        
        $template.append($newSubTemplate.show());
                   
        var parameters = array1[1];
        if(parameters != null) {
            var array2 = parameters.split("&");                    
            for(var i=0; i < array2.length; i++) {                
                var array3 = array2[i].split("=");     
                if(array3.length >= 2) {
                    var $newSubTemplate = $subTemplate.clone();              
                    $newSubTemplate.attr("id", ("grid_row_template_"+i));
                    if(i%2 == 0)
                        $newSubTemplate.addClass("odd");
                    else
                        $newSubTemplate.addClass("even");
                    $newSubTemplate.find("#label").text(array3[0] + ": ");
                    $newSubTemplate.find("#value").text(array3[1]);                        
                    $template.append($newSubTemplate.show());
                }               
            }                         
        }
    }    
}
*/

function publicNetworkFirewallJsonToTemplate(jsonObj, $template) {    
    $template.data("jsonObj", jsonObj);
    $template.attr("id", "publicNetworkFirewall_" + jsonObj.id);  
    $template.find("#id").text(fromdb(jsonObj.id));          
    //publicNetworkParseUrl(jsonObj.url, $template);    
    $template.find("#ip").text(fromdb(jsonObj.ipaddress));        
    $template.find("#username").text(fromdb(jsonObj.username));      
    $template.find("#publicinterface").text(fromdb(jsonObj.publicinterface));      
    $template.find("#privateinterface").text(fromdb(jsonObj.privateinterface));      
    $template.find("#usageinterface").text(fromdb(jsonObj.usageinterface));      
    $template.find("#publiczone").text(fromdb(jsonObj.publiczone));      
    $template.find("#privatezone").text(fromdb(jsonObj.privatezone));         
    $template.find("#numretries").text(fromdb(jsonObj.numretries));
    $template.find("#timeout").text(fromdb(jsonObj.timeout));
          
    var $actionLink = $template.find("#action_link");	
    bindActionLink($actionLink);
    /*	
	$actionLink.bind("mouseover", function(event) {
        $(this).find("#action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {
        $(this).find("#action_menu").hide();    
        return false;
    });		
	*/
	
	var $actionMenu = $actionLink.find("#action_menu");
    $actionMenu.find("#action_list").empty();	
       
    buildActionLinkForSubgridItem("label.action.delete.firewall", publicNetworkFirewallActionMap, $actionMenu, $template);	
}

var publicNetworkFirewallActionMap = {     
    "label.action.delete.firewall": {              
        api: "deleteExternalFirewall",     
        isAsyncJob: false,      
        inProcessText: "label.action.delete.firewall.processing",
        afterActionSeccessFn: function(json, id, $subgridItem) {                 
            $subgridItem.slideUp("slow", function() {
                $(this).remove();
            });
        }
    }     
}  


function publicNetworkJsonToLoadBalancerTab() {  
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null)
        return;
    
    var jsonObj = $midmenuItem1.data("jsonObj");    
    if(jsonObj == null) 
	    return;	  
	    
	var $thisTab = $("#right_panel_content #public_network_page #tab_content_loadbalancer");      
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   
        
    $.ajax({
        data: createURL("command=listExternalLoadBalancers&zoneid="+zoneObj.id),
        dataType: "json",
        success: function(json) {                 
            var items = json.listexternalloadbalancersresponse.externalloadbalancer;
		    var $container = $thisTab.find("#tab_container").empty();
		    var $template = $("#loadbalancer_template");
		    if(items != null && items.length > 0) {		        
		        for(var i=0; i<items.length; i++) {
		            var $newTemplate = $template.clone();
		            publicNetworkLoadBalancerJsonToTemplate(items[i], $newTemplate);
		            $container.append($newTemplate.show());
		        }
		    }		    
		    $thisTab.find("#tab_container").show(); 
            $thisTab.find("#tab_spinning_wheel").hide();   
        }
    });       
}

function publicNetworkLoadBalancerJsonToTemplate(jsonObj, $template) {    
    $template.data("jsonObj", jsonObj);
    $template.attr("id", "publicNetworkLoadBalancer_" + jsonObj.id);
        
    $template.find("#grid_header_title").text(fromdb(jsonObj.url));    
    $template.find("#id").text(fromdb(jsonObj.id));   
    //publicNetworkParseUrl(jsonObj.url, $template);      
    $template.find("#ip").text(fromdb(jsonObj.ipaddress));        
    $template.find("#username").text(fromdb(jsonObj.username));      
    $template.find("#publicinterface").text(fromdb(jsonObj.publicinterface));      
    $template.find("#privateinterface").text(fromdb(jsonObj.privateinterface));      
    $template.find("#numretries").text(fromdb(jsonObj.numretries));
      
    var $actionLink = $template.find("#action_link");	
    bindActionLink($actionLink);
    /*	
	$actionLink.bind("mouseover", function(event) {
        $(this).find("#action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {
        $(this).find("#action_menu").hide();    
        return false;
    });		
	*/
	
	var $actionMenu = $actionLink.find("#action_menu");
    $actionMenu.find("#action_list").empty();	       
    buildActionLinkForSubgridItem("label.action.delete.load.balancer", publicNetworkLoadBalancerActionMap, $actionMenu, $template);	
}

var publicNetworkLoadBalancerActionMap = {     
    "label.action.delete.load.balancer": {              
        api: "deleteExternalLoadBalancer",     
        isAsyncJob: false,      
        inProcessText: "label.action.delete.load.balancer.processing",
        afterActionSeccessFn: function(json, id, $subgridItem) {                 
            $subgridItem.slideUp("slow", function() {
                $(this).remove();
            });
        }
    }     
}  

function bindAddIpRangeToPublicNetworkButton($button, $midmenuItem1) {   
    var jsonObj = $midmenuItem1.data("jsonObj");      
    
    var $dialogAddIpRangeToPublicNetwork = $("#dialog_add_iprange_to_publicnetwork"); 
         
    //***** binding Event Handler (begin) ******   
	if (zoneObj.networktype == "Advanced") {
		$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_tagged").change(function(event) {	
			if ($(this).val() == "tagged") {
				$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_vlan_container").show();
				//$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_pod_container").hide();
								
				$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_scope").empty().append('<option value="zone-wide">zone-wide</option>').append('<option value="account-specific">account-specific</option>');
			} 
			else if($(this).val() == "untagged") {  
				$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_vlan_container").hide();
				//$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_pod_container").hide();
				
				$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_scope").empty().append('<option value="zone-wide">zone-wide</option>');				
			}			
			
			// default value of "#add_publicip_vlan_scope" is "zone-wide". Calling change() will hide "#add_publicip_vlan_domain_container", "#add_publicip_vlan_account_container". 
			$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_scope").change(); 	
			
			return false;
		});
	} 
	
	$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_scope").change(function(event) {	   
	    if($(this).val() == "zone-wide") {
	        $dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_domain_container").hide();
			$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_account_container").hide();    
	    } 
	    else if($(this).val() == "account-specific") { 
	        $dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_domain_container").show();
			$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_account_container").show();    
	    }		    
	    return false;
	});
	//***** binding Event Handler (end) ******   
    $button.show();   
    $button.unbind("click").bind("click", function(event) {  
        if($("#public_network_page").find("#tab_content_ipallocation").css("display") == "none")         
            $("#public_network_page").find("#tab_ipallocation").click();
         
        /*                  
        $dialogAddIpRangeToPublicNetwork.find("#info_container").hide();
        $dialogAddIpRangeToPublicNetwork.find("#zone_name").text(fromdb(zoneObj.name));         
		$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_vlan_container, #add_publicip_vlan_domain_container, #add_publicip_vlan_account_container").hide();
		$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_tagged, #add_publicip_vlan_vlan, #add_publicip_vlan_gateway, #add_publicip_vlan_netmask, #add_publicip_vlan_startip, #add_publicip_vlan_endip, #add_publicip_vlan_account").val("");
		*/
		//$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_pod_container").show();	
		
		$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_tagged").change();            		
	    $dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_scope").change(); // default value of "#add_publicip_vlan_scope" is "zone-wide". Calling change() will hide "#add_publicip_vlan_domain_container", "#add_publicip_vlan_account_container". 	
        
        /*    					
		var podSelect = $dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_pod").empty();		
		$.ajax({
		    data: createURL("command=listPods&zoneId="+zoneObj.id),
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
		*/
		
		var domainSelect = $dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_domain").empty();	
		if(zoneObj.domainid != null) { //list only domains under zoneObj.domainid
		    domainSelect.append("<option value='" + zoneObj.domainid + "'>" + fromdb(zoneObj.domain) + "</option>"); 	
		        									
		    function populateDomainDropdown(id) {					        
                $.ajax({
                    data: createURL("command=listDomainChildren&id="+id),
                    dataType: "json",
                    async: false,
                    success: function(json) {					        
                        var domains = json.listdomainchildrenresponse.domain;		                  		        	    
	                    if (domains != null && domains.length > 0) {					    
		                    for (var i = 0; i < domains.length; i++) {	
			                    domainSelect.append("<option value='" + domains[i].id + "'>" + fromdb(domains[i].name) + "</option>"); 	
			                    if(domains[i].haschild == true) 
	                                populateDomainDropdown(domains[i].id);				   
		                    }
	                    }				
                    }
                }); 
            }	
            
            populateDomainDropdown(zoneObj.domainid);
        }
        else { //list all domains            
             $.ajax({
                data: createURL("command=listDomains"),
                dataType: "json",
                success: function(json) {           
                    var items = json.listdomainsresponse.domain;
                    if(items != null && items.length > 0) {
                        for(var i=0; i<items.length; i++) {
                            domainSelect.append("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>"); 
                        }		
                    }
                }    
            });  
        }   
		
		$dialogAddIpRangeToPublicNetwork
		.dialog('option', 'buttons', { 	
			"Add": function() { 	
			    var $thisDialog = $(this);		
			    					
				// validate values
				var isValid = true;					
				var isTagged = $thisDialog.find("#add_publicip_vlan_tagged").val() == "tagged";
				
				
				isValid &= validateString("Account", $thisDialog.find("#add_publicip_vlan_account"), $thisDialog.find("#add_publicip_vlan_account_errormsg"), true); //optional
				
				if (isTagged) {
					isValid &= validateNumber("VLAN", $thisDialog.find("#add_publicip_vlan_vlan"), $thisDialog.find("#add_publicip_vlan_vlan_errormsg"), 1, 4095);
				}
				
				isValid &= validateIp("Gateway", $thisDialog.find("#add_publicip_vlan_gateway"), $thisDialog.find("#add_publicip_vlan_gateway_errormsg"), false); //required
				isValid &= validateIp("Netmask", $thisDialog.find("#add_publicip_vlan_netmask"), $thisDialog.find("#add_publicip_vlan_netmask_errormsg"), false); //required
				isValid &= validateIp("Start IP Range", $thisDialog.find("#add_publicip_vlan_startip"), $thisDialog.find("#add_publicip_vlan_startip_errormsg"), false); //required
				isValid &= validateIp("End IP Range", $thisDialog.find("#add_publicip_vlan_endip"), $thisDialog.find("#add_publicip_vlan_endip_errormsg"), true); //optional
				if (!isValid) 
				    return;		
				 
				var isDirect = false;
				    						
				$thisDialog.find("#spinning_wheel").show()
				
				var vlan = trim($thisDialog.find("#add_publicip_vlan_vlan").val());
				if (isTagged) {
					vlan = "&vlan="+vlan;
				} else {
					vlan = "&vlan=untagged";
				}
								
				var scopeParams = "";
				if($dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_scope").val() == "account-specific") {
				    scopeParams = "&domainId="+trim($thisDialog.find("#add_publicip_vlan_domain").val())+"&account="+trim($thisDialog.find("#add_publicip_vlan_account").val());  
				} else if (isDirect) {
					scopeParams = "&isshared=true";
				}
				
				var array1 = [];						
				var gateway = $thisDialog.find("#add_publicip_vlan_gateway").val();
				array1.push("&gateway="+todb(gateway));
				
				var netmask = $thisDialog.find("#add_publicip_vlan_netmask").val();
				array1.push("&netmask="+todb(netmask));
				
				var startip = $thisDialog.find("#add_publicip_vlan_startip").val();
				array1.push("&startip="+todb(startip));
				
				var endip = $thisDialog.find("#add_publicip_vlan_endip").val();	//optional field (might be empty)
				if(endip != null && endip.length > 0)
				    array1.push("&endip="+todb(endip));			
													
				// Add IP Range to public network
				$.ajax({
					data: createURL("command=createVlanIpRange&forVirtualNetwork=true&zoneId="+zoneObj.id+vlan+scopeParams+array1.join("")),
					dataType: "json",
					success: function(json) {	
						$thisDialog.find("#spinning_wheel").hide();
						$thisDialog.dialog("close");
					
					    var item = json.createvlaniprangeresponse.vlan;						    
					    var $newTemplate = $("#public_iprange_template").clone();
	                    publicNetworkIprangeJsonToTemplate(item, $newTemplate);
	                    $("#public_network_page").find("#tab_content_ipallocation").find("#tab_container").prepend($newTemplate.show());						   
					},
					error: function(XMLHttpResponse) {
						handleError(XMLHttpResponse, function() {
							handleErrorInDialog(XMLHttpResponse, $thisDialog);	
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

function bindAddExternalFirewallButton($button, $midmenuItem1) {         
    var jsonObj = $midmenuItem1.data("jsonObj");      
    
    var $dialogAddExternalFirewall = $("#dialog_add_external_firewall"); 
          
    $button.show();   
    $button.unbind("click").bind("click", function(event) {         
        if($("#public_network_page").find("#tab_content_firewall").css("display") == "none")         
            $("#public_network_page").find("#tab_firewall").click();
                          
        $dialogAddExternalFirewall.find("#info_container").hide();
        $dialogAddExternalFirewall.find("#zone_name").text(fromdb(zoneObj.name));         
					
		$dialogAddExternalFirewall
		.dialog('option', 'buttons', { 	
			"Add": function() { 	
			    var $thisDialog = $(this);		
			    			
				// validate values
				var isValid = true;		
				isValid &= validateString("IP", $thisDialog.find("#ip"), $thisDialog.find("#ip_errormsg"), false); //required
				isValid &= validateString("User Name", $thisDialog.find("#username"), $thisDialog.find("#username_errormsg"), false); //required
				isValid &= validateString("Password", $thisDialog.find("#password"), $thisDialog.find("#password_errormsg"), false);  //required				
				isValid &= validateString("Public Interface", $thisDialog.find("#public_interface"), $thisDialog.find("#public_interface_errormsg"), true);  //optinal
				isValid &= validateString("Private Interface", $thisDialog.find("#private_interface"), $thisDialog.find("#private_interface_errormsg"), true);  //optinal				
				isValid &= validateString("Usage Interface", $thisDialog.find("#usage_interface"), $thisDialog.find("#usage_interface_errormsg"), true);  //optinal				
				isValid &= validateString("Public Zone", $thisDialog.find("#public_zone"), $thisDialog.find("#public_zone_errormsg"), true);  //optinal
				isValid &= validateString("Private Zone", $thisDialog.find("#private_zone"), $thisDialog.find("#private_zone_errormsg"), true);  //optinal				
				isValid &= validateInteger("Number of Retries", $thisDialog.find("#numretries"), $thisDialog.find("#numretries_errormsg"), null, null, true);  //optinal
				isValid &= validateInteger(" Timeout(seconds)", $thisDialog.find("#timeout"), $thisDialog.find("#timeout_errormsg"), null, null, true);  //optinal
				if (!isValid) 
				    return;		
				 			    						
				$thisDialog.find("#spinning_wheel").show()
				
				var array1 = [];
			
				array1.push("&zoneid=" + zoneObj.id);
											
				var username = $thisDialog.find("#username").val();
				array1.push("&username="+username);
				
				var password = $thisDialog.find("#password").val();
				array1.push("&password="+password);
				
				//*** construct URL (begin)	***	
				var url = [];
				
				var ip = $thisDialog.find("#ip").val();
		        if(ip.indexOf("http://")==-1)
		            url.push("http://"+ip);		            
		        else
		            url.push(ip);		                   
				
				var isQuestionMarkAdded = false;
				
				var publicInterface = $thisDialog.find("#public_interface").val();
				if(publicInterface != null && publicInterface.length > 0) {
				    if(isQuestionMarkAdded == false) {
				        url.push("?");
				        isQuestionMarkAdded = true;
				    }
				    else {
				        url.push("&");
				    }  				    
				    url.push("publicInterface="+publicInterface); 
				}
				    
				var privateInterface = $thisDialog.find("#private_interface").val();
				if(privateInterface != null && privateInterface.length > 0) {
				    if(isQuestionMarkAdded == false) {
				        url.push("?");
				        isQuestionMarkAdded = true;
				    }
				    else {
				        url.push("&");
				    }  		
				    url.push("privateInterface="+privateInterface); 
				}
				 
				var usageInterface = $thisDialog.find("#usage_interface").val();
				if(usageInterface != null && usageInterface.length > 0) {
				    if(isQuestionMarkAdded == false) {
				        url.push("?");
				        isQuestionMarkAdded = true;
				    }
				    else {
				        url.push("&");
				    }  				    
				    url.push("usageInterface="+usageInterface); 
				} 
				    
				var publicZone = $thisDialog.find("#public_zone").val();
				if(publicZone != null && publicZone.length > 0) {
				    if(isQuestionMarkAdded == false) {
				        url.push("?");
				        isQuestionMarkAdded = true;
				    }
				    else {
				        url.push("&");
				    }  		
				    url.push("publicZone="+publicZone); 
				}
				
				var privateZone = $thisDialog.find("#private_zone").val();
				if(privateZone != null && privateZone.length > 0) {
				    if(isQuestionMarkAdded == false) {
				        url.push("?");
				        isQuestionMarkAdded = true;
				    }
				    else {
				        url.push("&");
				    }  		
				    url.push("privateZone="+privateZone); 	
				}			
				
				var numretries = $thisDialog.find("#numretries").val();
				if(numretries != null && numretries.length > 0) {
				    if(isQuestionMarkAdded == false) {
				        url.push("?");
				        isQuestionMarkAdded = true;
				    }
				    else {
				        url.push("&");
				    }  		
				    url.push("numretries="+numretries); 	
				}		
				
				var timeout = $thisDialog.find("#timeout").val();
				if(timeout != null && timeout.length > 0) {
				    if(isQuestionMarkAdded == false) {
				        url.push("?");
				        isQuestionMarkAdded = true;
				    }
				    else {
				        url.push("&");
				    }  		
				    url.push("timeout="+timeout); 	
				}		
				
				array1.push("&url="+todb(url.join("")));		
				//*** construct URL (end)	***					
										
				$.ajax({
					data: createURL("command=addExternalFirewall"+array1.join("")),
					dataType: "json",
					success: function(json) {	
						$thisDialog.find("#spinning_wheel").hide();
						$thisDialog.dialog("close");
					    					    
					    var item = json.addexternalfirewallresponse.externalfirewall;
					    var $newTemplate = $("#externalfirewall_template").clone();
		                publicNetworkFirewallJsonToTemplate(item, $newTemplate);
		                $("#right_panel_content #public_network_page #tab_content_firewall").find("#tab_container").append($newTemplate.show());					    	               				   
					},
					error: function(XMLHttpResponse) {
						handleError(XMLHttpResponse, function() {
							handleErrorInDialog(XMLHttpResponse, $thisDialog);	
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

var publicNetworkIpRangeActionMap = {     
    "label.action.delete.IP.range": {              
        api: "deleteVlanIpRange",     
        isAsyncJob: false,   
        inProcessText: "label.action.delete.IP.range.processing",
        afterActionSeccessFn: function(json, id, $subgridItem) {                 
            $subgridItem.slideUp("slow", function() {
                $(this).remove();
            });
        }
    }    
}  


function bindAddLoadBalancerButton($button, $midmenuItem1) {         
    var jsonObj = $midmenuItem1.data("jsonObj");      
    
    var $dialogAddLoadBalancer = $("#dialog_add_load_balancer"); 
          
    $button.show();   
    $button.unbind("click").bind("click", function(event) {         
        if($("#public_network_page").find("#tab_content_loadbalancer").css("display") == "none")         
            $("#public_network_page").find("#tab_loadbalancer").click();
                          
        $dialogAddLoadBalancer.find("#info_container").hide();
        $dialogAddLoadBalancer.find("#zone_name").text(fromdb(zoneObj.name));         
					
		$dialogAddLoadBalancer
		.dialog('option', 'buttons', { 	
			"Add": function() { 	
			    var $thisDialog = $(this);		
			    			
				// validate values
				var isValid = true;		
				isValid &= validateString("IP", $thisDialog.find("#ip"), $thisDialog.find("#ip_errormsg"), false); //required
				isValid &= validateString("User Name", $thisDialog.find("#username"), $thisDialog.find("#username_errormsg"), false); //required
				isValid &= validateString("Password", $thisDialog.find("#password"), $thisDialog.find("#password_errormsg"), false);  //required				
				isValid &= validateString("Public Interface", $thisDialog.find("#public_interface"), $thisDialog.find("#public_interface_errormsg"), true);  //optinal
				isValid &= validateString("Private Interface", $thisDialog.find("#private_interface"), $thisDialog.find("#private_interface_errormsg"), true);  //optinal
				isValid &= validateInteger("Number of Retries", $thisDialog.find("#numretries"), $thisDialog.find("#numretries_errormsg"), null, null, true);  //optinal
				if (!isValid) 
				    return;		
				 			    						
				$thisDialog.find("#spinning_wheel").show()
				
				var array1 = [];
			
				array1.push("&zoneid=" + zoneObj.id);
											
				var username = $thisDialog.find("#username").val();
				array1.push("&username="+username);
				
				var password = $thisDialog.find("#password").val();
				array1.push("&password="+password);
				
				//*** construct URL (begin)	***	
				var url = [];
				
				var ip = $thisDialog.find("#ip").val();
		        if(ip.indexOf("http://")==-1)
		            url.push("http://"+ip);		            
		        else
		            url.push(ip);		                   
				
				var isQuestionMarkAdded = false;
				
				var publicInterface = $thisDialog.find("#public_interface").val();
				if(publicInterface != null && publicInterface.length > 0) {
				    if(isQuestionMarkAdded == false) {
				        url.push("?");
				        isQuestionMarkAdded = true;
				    }
				    else {
				        url.push("&");
				    }  				    
				    url.push("publicInterface="+publicInterface); 
				}
				    
				var privateInterface = $thisDialog.find("#private_interface").val();
				if(privateInterface != null && privateInterface.length > 0) {
				    if(isQuestionMarkAdded == false) {
				        url.push("?");
				        isQuestionMarkAdded = true;
				    }
				    else {
				        url.push("&");
				    }  		
				    url.push("privateInterface="+privateInterface); 
				}
				
				var numretries = $thisDialog.find("#numretries").val();
				if(numretries != null && numretries.length > 0) {
				    if(isQuestionMarkAdded == false) {
				        url.push("?");
				        isQuestionMarkAdded = true;
				    }
				    else {
				        url.push("&");
				    }  		
				    url.push("numretries="+numretries); 	
				}		
				  				
				array1.push("&url="+todb(url.join("")));		
				//*** construct URL (end)	***					
								
				$.ajax({
					data: createURL("command=addExternalLoadBalancer"+array1.join("")),
					dataType: "json",
					success: function(json) {	
						$thisDialog.find("#spinning_wheel").hide();
						$thisDialog.dialog("close");
					   	
					   	var item = json.addexternalloadbalancerresponse.externalloadbalancer;			    
					    var $newTemplate = $("#loadbalancer_template").clone();
		                publicNetworkLoadBalancerJsonToTemplate(item, $newTemplate);
		                $("#right_panel_content #public_network_page #tab_content_loadbalancer").find("#tab_container").append($newTemplate.show());
					},
					error: function(XMLHttpResponse) {
						handleError(XMLHttpResponse, function() {
							handleErrorInDialog(XMLHttpResponse, $thisDialog);	
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

//***** Public Network (end) ******************************************************************************************************


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
    $midmenuItem1.find("#second_row").text("VLAN : " + fromdb(jsonObj.vlan));   
}

function directNetworkToRightPanel($midmenuItem1) {    
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);  
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);  
            
    $("#direct_network_page").show();
    bindAddIpRangeToDirectNetworkButton($("#add_iprange_button"), $midmenuItem1);
    $("#add_external_firewall_button").unbind("click").hide(); 
    $("#add_load_balancer_button").unbind("click").hide(); 
    
    $("#public_network_page").hide();
    
    $("#direct_network_page").find("#tab_details").click();     
}
	
function directNetworkClearRightPanel() {
    directNetworkJsonClearDetailsTab();
    directNetworkJsonClearIpAllocationTab();
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
	$thisTab.find("#default").text((jsonObj.isdefault) ? "Yes" : "No"); 	
    $thisTab.find("#vlan").text(fromdb(jsonObj.vlan));
    $thisTab.find("#gateway").text(fromdb(jsonObj.gateway));
    $thisTab.find("#netmask").text(fromdb(jsonObj.netmask));
        
    $thisTab.find("#domain").text(fromdb(jsonObj.domain));      //might be null
    $thisTab.find("#account").text(fromdb(jsonObj.account));    //might be null
        
    //actions ***   
    var $actionLink = $thisTab.find("#action_link"); 
    bindActionLink($actionLink);
    /*
    $actionLink.bind("mouseover", function(event) {	    
        $(this).find("#action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {       
        $(this).find("#action_menu").hide();    
        return false;
    });	  
    */
    
    var $actionMenu = $actionLink.find("#action_menu");
    $actionMenu.find("#action_list").empty();        
    buildActionLinkForTab("label.action.delete.network", directNetworkActionMap, $actionMenu, $midmenuItem1, $thisTab);	      
        
    $thisTab.find("#tab_container").show(); 
    $thisTab.find("#tab_spinning_wheel").hide();   
}

function directNetworkJsonClearDetailsTab() {
    var $thisTab = $("#right_panel_content #direct_network_page #tab_content_details");    
	$thisTab.find("#grid_header_title").text("");			
	$thisTab.find("#id").text("");				
	$thisTab.find("#name").text("");	
	$thisTab.find("#displaytext").text("");	 
	$thisTab.find("#default").text(""); 	 	
    $thisTab.find("#vlan").text("");
    $thisTab.find("#gateway").text("");
    $thisTab.find("#netmask").text("");        
    $thisTab.find("#domain").text("");      
    $thisTab.find("#account").text("");       
       
    //actions ***  
    var $actionMenu = $thisTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();   
	$actionMenu.find("#action_list").append($("#no_available_actions").clone().show());	   
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
		    var $template = $("#direct_iprange_template");
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

function directNetworkJsonClearIpAllocationTab() {
    var $thisTab = $("#right_panel_content #direct_network_page #tab_content_ipallocation"); 
    $thisTab.find("#tab_container").empty();     
}

function directNetworkIprangeJsonToTemplate(jsonObj, $template) {    
    $template.data("jsonObj", jsonObj);
    $template.attr("id", "directNetworkIprange_" + jsonObj.id);
    
    var ipRange = getIpRange(fromdb(jsonObj.startip), fromdb(jsonObj.endip));
    $template.find("#grid_header_title").text(ipRange);
    
    $template.find("#id").text(jsonObj.id)
    $template.find("#vlan").text(jsonObj.vlan);
    $template.find("#iprange").text(ipRange);
        
    var $actionLink = $template.find("#action_link");	
    bindActionLink($actionLink);
    /*	
	$actionLink.bind("mouseover", function(event) {
        $(this).find("#action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {
        $(this).find("#action_menu").hide();    
        return false;
    });		
    */
	
	var $actionMenu = $actionLink.find("#action_menu");
    $actionMenu.find("#action_list").empty();	
       
    buildActionLinkForSubgridItem("label.action.delete.IP.range", directNetworkIpRangeActionMap, $actionMenu, $template);
}

var directNetworkIpRangeActionMap = {     
    "label.action.delete.IP.range": {              
        api: "deleteVlanIpRange",     
        isAsyncJob: false,   
        inProcessText: "label.action.delete.IP.range.processing",
        afterActionSeccessFn: function(json, id, $subgridItem) {                 
            $subgridItem.slideUp("slow", function() {
                $(this).remove();
            });
        }
    }    
}  

function bindAddNetworkButton($button) {   
    if(zoneObj == null)
        return;
    
    var $dialogAddNetworkForZone = $("#dialog_add_network_for_zone"); 
     
	$dialogAddNetworkForZone.find("#add_publicip_vlan_scope").change(function(event) {		    
	    if($(this).val() == "zone-wide") {
	        $dialogAddNetworkForZone.find("#add_publicip_vlan_domain_container").hide();
			$dialogAddNetworkForZone.find("#add_publicip_vlan_account_container").hide();    
	    } 
	    else if($(this).val() == "account-specific") { 
	        $dialogAddNetworkForZone.find("#add_publicip_vlan_domain_container").show();
			$dialogAddNetworkForZone.find("#add_publicip_vlan_account_container").show();    
	    }		    
	    return false;
	});
	    
    $button.unbind("click").bind("click", function(event) {   
	    $dialogAddNetworkForZone.find("#info_container").hide();
        $dialogAddNetworkForZone.find("#zone_name").text(fromdb(zoneObj.name));  
		$dialogAddNetworkForZone.find("#add_publicip_vlan_vlan, #add_publicip_vlan_gateway, #add_publicip_vlan_netmask, #add_publicip_vlan_startip, #add_publicip_vlan_endip, #add_publicip_vlan_account").val("");
		$dialogAddNetworkForZone.find("#add_publicip_vlan_scope").change();  	                    
        				
		if (zoneObj.networktype == 'Basic') {
			
		} 
		else {	
			var domainSelect = $dialogAddNetworkForZone.find("#add_publicip_vlan_domain").empty();	
			if(zoneObj.domainid != null) { //list only domains under zoneObj.domainid
			    domainSelect.append("<option value='" + zoneObj.domainid + "'>" + fromdb(zoneObj.domain) + "</option>"); 	
			        									
			    function populateDomainDropdown(id) {					        
                    $.ajax({
	                    data: createURL("command=listDomainChildren&id="+id),
	                    dataType: "json",
	                    async: false,
	                    success: function(json) {					        
	                        var domains = json.listdomainchildrenresponse.domain;		                  		        	    
		                    if (domains != null && domains.length > 0) {					    
			                    for (var i = 0; i < domains.length; i++) {	
				                    domainSelect.append("<option value='" + domains[i].id + "'>" + fromdb(domains[i].name) + "</option>"); 	
				                    if(domains[i].haschild == true) 
		                                populateDomainDropdown(domains[i].id);				   
			                    }
		                    }				
	                    }
                    }); 
                }	
                
                populateDomainDropdown(zoneObj.domainid);
            }
            else { //list all domains            
                 $.ajax({
                    data: createURL("command=listDomains"),
                    dataType: "json",
                    success: function(json) {           
                        var items = json.listdomainsresponse.domain;
                        if(items != null && items.length > 0) {
                            for(var i=0; i<items.length; i++) {
                                domainSelect.append("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>"); 
                            }		
                        }
                    }    
                });  
            }   
		}

		$dialogAddNetworkForZone
		.dialog('option', 'buttons', { 	
			"Add": function() { 	
			    var $thisDialog = $(this);		
			    					
				// validate values
				var isValid = true;					
				var isTagged = true;
				var isDirect = true;
				
				isValid &= validateString("Account", $thisDialog.find("#add_publicip_vlan_account"), $thisDialog.find("#add_publicip_vlan_account_errormsg"), true); //optional
				isValid &= validateInteger("VLAN", $thisDialog.find("#add_publicip_vlan_vlan"), $thisDialog.find("#add_publicip_vlan_vlan_errormsg"), 1, 4095);
				isValid &= validateString("Network Name", $thisDialog.find("#add_publicip_vlan_network_name"), $thisDialog.find("#add_publicip_vlan_network_name_errormsg"));
				isValid &= validateString("Network Description", $thisDialog.find("#add_publicip_vlan_network_desc"), $thisDialog.find("#add_publicip_vlan_network_desc_errormsg"));			
				isValid &= validateIp("Gateway", $thisDialog.find("#add_publicip_vlan_gateway"), $thisDialog.find("#add_publicip_vlan_gateway_errormsg"));
				isValid &= validateIp("Netmask", $thisDialog.find("#add_publicip_vlan_netmask"), $thisDialog.find("#add_publicip_vlan_netmask_errormsg"));
				isValid &= validateIp("Start IP Range", $thisDialog.find("#add_publicip_vlan_startip"), $thisDialog.find("#add_publicip_vlan_startip_errormsg"));   //required
				isValid &= validateIp("End IP Range", $thisDialog.find("#add_publicip_vlan_endip"), $thisDialog.find("#add_publicip_vlan_endip_errormsg"), true);  //optional
				if (!isValid) 
				    return;		
				    				
				$thisDialog.find("#spinning_wheel").show()
				
				var vlan = trim($thisDialog.find("#add_publicip_vlan_vlan").val());
				if (isTagged) {
					vlan = "&vlan="+vlan;
				} else {
					vlan = "&vlan=untagged";
				}
								
				var scopeParams = "";
				if($dialogAddNetworkForZone.find("#add_publicip_vlan_scope").val()=="account-specific") {
				    scopeParams = "&domainId="+trim($thisDialog.find("#add_publicip_vlan_domain").val())+"&account="+trim($thisDialog.find("#add_publicip_vlan_account").val());  
				} else if (isDirect) {
					scopeParams = "&isshared=true";
				}
				
				var isDefault = trim($thisDialog.find("#add_publicip_vlan_default").val());
				var gateway = trim($thisDialog.find("#add_publicip_vlan_gateway").val());
				var netmask = trim($thisDialog.find("#add_publicip_vlan_netmask").val());
				var startip = trim($thisDialog.find("#add_publicip_vlan_startip").val());
				var endip = trim($thisDialog.find("#add_publicip_vlan_endip").val());					
										
				// Creating network for the direct networking
				var name = todb($thisDialog.find("#add_publicip_vlan_network_name").val());
				var desc = todb($thisDialog.find("#add_publicip_vlan_network_desc").val());
				$.ajax({
					data: createURL("command=listNetworkOfferings&traffictype=Public"),
					dataType: "json",
					async: false,
					success: function(json) {
						var networkOfferings = json.listnetworkofferingsresponse.networkoffering;
						if (networkOfferings != null && networkOfferings.length > 0) {
							for (var i = 0; i < networkOfferings.length; i++) {
								if (networkOfferings[i].isdefault) {
									// Create a network from this.
									$.ajax({
										data: createURL("command=createNetwork&isDefault="+isDefault+"&name="+name+"&displayText="+desc+"&networkOfferingId="+networkOfferings[i].id+"&zoneId="+zoneObj.id+vlan+scopeParams+"&gateway="+todb(gateway)+"&netmask="+todb(netmask)+"&startip="+todb(startip)+"&endip="+todb(endip)),
										dataType: "json",
										success: function(json) {	
											$thisDialog.find("#spinning_wheel").hide();
											$thisDialog.dialog("close");
										
										    var item = json.createnetworkresponse.network;
										    var $midmenuItem1 = $("#midmenu_item").clone();                      
                                            $midmenuItem1.data("toRightPanelFn", directNetworkToRightPanel);                             
                                            directNetworkToMidmenu(item, $midmenuItem1);    
                                            bindClickToMidMenu($midmenuItem1, directNetworkToRightPanel, directNetworkGetMidmenuId);   
                                            $("#midmenu_container").append($midmenuItem1.show());  											    
										},
										error: function(XMLHttpResponse) {
											handleError(XMLHttpResponse, function() {
												handleErrorInDialog(XMLHttpResponse, $thisDialog);	
											});
										}
									});
								}
							}
						}
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

function bindAddIpRangeToDirectNetworkButton($button, $midmenuItem1) {   
    var jsonObj = $midmenuItem1.data("jsonObj");       
        
    var $dialogAddIpRangeToDirectNetwork = $("#dialog_add_iprange_to_directnetwork");     
  
    $dialogAddIpRangeToDirectNetwork.find("#directnetwork_name").text(fromdb(jsonObj.name));
    $dialogAddIpRangeToDirectNetwork.find("#zone_name").text(fromdb(zoneObj.name));
    
    $button.show();   
    $button.unbind("click").bind("click", function(event) {    
        if($("#direct_network_page").find("#tab_content_ipallocation").css("display") == "none")       
            $("#direct_network_page").find("#tab_ipallocation").click();    
        
		$dialogAddIpRangeToDirectNetwork
		.dialog('option', 'buttons', { 	
			"Add": function() { 	
			    var $thisDialog = $(this);		
			    					
				// validate values
				var isValid = true;		
				isValid &= validateIp("Start IP Range", $thisDialog.find("#add_publicip_vlan_startip"), $thisDialog.find("#add_publicip_vlan_startip_errormsg"), false);   //required
				isValid &= validateIp("End IP Range", $thisDialog.find("#add_publicip_vlan_endip"), $thisDialog.find("#add_publicip_vlan_endip_errormsg"), true);  //optional
				if (!isValid) 
				    return;						    
				
				$thisDialog.find("#spinning_wheel").show()
						
				var array1 = [];							
				var startip = $thisDialog.find("#add_publicip_vlan_startip").val();
				array1.push("&startip="+todb(startip));
				
				var endip = $thisDialog.find("#add_publicip_vlan_endip").val();	
				if(endip != null && endip.length > 0)
				    array1.push("&endip="+todb(endip));								
						
				$.ajax({
					data: createURL("command=createVlanIpRange&forVirtualNetwork=false&networkid="+todb(jsonObj.id)+array1.join("")),
					dataType: "json",
					success: function(json) {	
						$thisDialog.find("#spinning_wheel").hide();
						$thisDialog.dialog("close");
					
					    var item = json.createvlaniprangeresponse.vlan;	
					    var $newTemplate = $("#direct_iprange_template").clone();
		                directNetworkIprangeJsonToTemplate(item, $newTemplate);
		                $("#right_panel_content #direct_network_page #tab_content_ipallocation").find("#tab_container").prepend($newTemplate.show());					    			   
					},
					error: function(XMLHttpResponse) {
						handleError(XMLHttpResponse, function() {
							handleErrorInDialog(XMLHttpResponse, $thisDialog);	
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

var directNetworkActionMap = {       
    "label.action.delete.network": {              
        isAsyncJob: false,    
        dialogBeforeActionFn : doDeleteNetwork,        
        inProcessText: "label.action.delete.network.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {   
            $midmenuItem1.slideUp("slow", function() {
                $(this).remove();                
                if(id.toString() == $("#right_panel_content").find("#direct_network_page").find("#tab_content_details").find("#id").text()) {
                    clearRightPanel();
                    directNetworkClearRightPanel();
                }                
            });              
        }
    }    
}  

function doDeleteNetwork($actionLink, $detailsTab, $midmenuItem1) {       
    var jsonObj = $midmenuItem1.data("jsonObj");
	var id = jsonObj.id;
		
	$("#dialog_confirmation")
	.text(dictionary["message.action.delete.network"])
	.dialog('option', 'buttons', { 					
		"Confirm": function() { 			
			$(this).dialog("close");			
			var apiCommand = "command=deleteNetwork&id="+id;
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		}
	}).dialog("open");
}
//***** Direct Network (end) ******************************************************************************************************

