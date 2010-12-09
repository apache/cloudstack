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

var zoneObj;  
function afterLoadNetworkJSP($leftmenuItem1) {    
    zoneObj = $leftmenuItem1.data("jsonObj");    
    if(zoneObj == null) 
	    return;	  
    
    showMiddleMenu();
    disableMultipleSelectionInMidMenu();     
    
    initAddNetworkButton($("#midmenu_add_network_button"));
    
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
         
    //populate items into middle menu  
    var $midmenuContainer = $("#midmenu_container").empty();   
    
    //public network  
    if(zoneObj.networktype == "Advanced") {
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
    }
    
    //direct network
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
    initAddIpRangeToPublicNetworkButton($("#midmenu_add_iprange_button"), $midmenuItem1);
    
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
		    var $template = $("#iprange_template");
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
    
         
     
    $thisTab.find("#tab_container").show(); 
    $thisTab.find("#tab_spinning_wheel").hide();   
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
    
         
     
    $thisTab.find("#tab_container").show(); 
    $thisTab.find("#tab_spinning_wheel").hide();   
}

function initAddIpRangeToPublicNetworkButton($button, $midmenuItem1) {   
    var jsonObj = $midmenuItem1.data("jsonObj");      
    
    initDialog("dialog_add_iprange_to_publicnetwork");    
    
    var $dialogAddIpRangeToPublicNetwork = $("#dialog_add_iprange_to_publicnetwork"); 
         
    //***** binding Event Handler (begin) ******      	
	$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_cidr_container").hide();	
	  
    $dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_tagged").change();
		
	// default value of "#add_publicip_vlan_scope" is "zone-wide". Calling change() will hide "#add_publicip_vlan_domain_container", "#add_publicip_vlan_account_container". 
	$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_scope").change(); 	
       
	if (zoneObj.networktype == "Advanced") {
		$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_tagged").change(function(event) {	
			if ($(this).val() == "tagged") {
				$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_vlan_container").show();
				$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_pod_container").hide();
								
				$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_scope").empty().append('<option value="zone-wide">zone-wide</option>').append('<option value="account-specific">account-specific</option>');
			} 
			else if($(this).val() == "untagged") {  
				$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_vlan_container").hide();
				$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_pod_container").hide();
				
				$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_scope").empty().append('<option value="zone-wide">zone-wide</option>');				
			}			
			
			// default value of "#add_publicip_vlan_scope" is "zone-wide". Calling change() will hide "#add_publicip_vlan_domain_container", "#add_publicip_vlan_account_container". 
			$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_scope").change(); 	
			
			return false;
		});
		
		$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_tagged").change();		
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
        $("#public_network_page").find("#tab_ipallocation").click();
    
        $dialogAddIpRangeToPublicNetwork.find("#info_container").hide();
        $dialogAddIpRangeToPublicNetwork.find("#zone_name").text(fromdb(zoneObj.name));         
		$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_vlan_container, #add_publicip_vlan_domain_container, #add_publicip_vlan_account_container").hide();
		$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_tagged, #add_publicip_vlan_vlan, #add_publicip_vlan_gateway, #add_publicip_vlan_netmask, #add_publicip_vlan_startip, #add_publicip_vlan_endip, #add_publicip_vlan_account").val("");
		
		
		$dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_pod_container").show();	
					
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
					isValid &= validateNumber("VLAN", $thisDialog.find("#add_publicip_vlan_vlan"), $thisDialog.find("#add_publicip_vlan_vlan_errormsg"), 2, 4095);
				}
				
				isValid &= validateIp("Gateway", $thisDialog.find("#add_publicip_vlan_gateway"), $thisDialog.find("#add_publicip_vlan_gateway_errormsg"));
				isValid &= validateIp("Netmask", $thisDialog.find("#add_publicip_vlan_netmask"), $thisDialog.find("#add_publicip_vlan_netmask_errormsg"));
				isValid &= validateIp("Start IP Range", $thisDialog.find("#add_publicip_vlan_startip"), $thisDialog.find("#add_publicip_vlan_startip_errormsg"));   //required
				isValid &= validateIp("End IP Range", $thisDialog.find("#add_publicip_vlan_endip"), $thisDialog.find("#add_publicip_vlan_endip_errormsg"), true);  //optional
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
				if($dialogAddIpRangeToPublicNetwork.find("#add_publicip_vlan_scope").val()=="account-specific") {
				    scopeParams = "&domainId="+trim($thisDialog.find("#add_publicip_vlan_domain").val())+"&account="+trim($thisDialog.find("#add_publicip_vlan_account").val());  
				} else if (isDirect) {
					scopeParams = "&isshared=true";
				}
										
				var gateway = trim($thisDialog.find("#add_publicip_vlan_gateway").val());
				var netmask = trim($thisDialog.find("#add_publicip_vlan_netmask").val());
				var startip = trim($thisDialog.find("#add_publicip_vlan_startip").val());
				var endip = trim($thisDialog.find("#add_publicip_vlan_endip").val());					
									
				
				// Allocating ip ranges on a vlan for virtual networking
				$.ajax({
					data: createURL("command=createVlanIpRange&forVirtualNetwork=true"+"&zoneId="+zoneObj.id+vlan+scopeParams+"&gateway="+todb(gateway)+"&netmask="+todb(netmask)+"&startip="+todb(startip)+"&endip="+todb(endip)),
					dataType: "json",
					success: function(json) {	
						$thisDialog.find("#spinning_wheel").hide();
						$thisDialog.dialog("close");
					
					    var item = json.createvlaniprangeresponse.vlan;						    
					    var $newTemplate = $("#iprange_template").clone();
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
    initAddIpRangeToDirectNetworkButton($("#midmenu_add_iprange_button"), $midmenuItem1);
    
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
		    var $template = $("#iprange_template");
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

function initAddNetworkButton($button) {   
    if(zoneObj == null)
        return;
    
    initDialog("dialog_add_network_for_zone");
    
    var $dialogAddNetworkForZone = $("#dialog_add_network_for_zone"); 
     
    //***** binding Event Handler (begin) ******    
    //direct VLAN shows only "tagged" option while public VLAN shows both "tagged" and "untagged" option. 
	$dialogAddNetworkForZone.find("#add_publicip_vlan_type").change(function(event) {
	    var addPublicipVlanTagged = $dialogAddNetworkForZone.find("#add_publicip_vlan_tagged").empty();
	   		
		if ($(this).val() == "false") { //direct VLAN (only tagged option)		
			addPublicipVlanTagged.append('<option value="tagged">tagged</option>');
			$dialogAddNetworkForZone.find("#add_publicip_vlan_network_name_container").show();	
			$dialogAddNetworkForZone.find("#add_publicip_vlan_network_desc_container").show();	
			$dialogAddNetworkForZone.find("#add_publicip_vlan_vlan_container").show();			
			$dialogAddNetworkForZone.find("#add_publicip_vlan_pod_container").hide();
			
		} 
		else if ($(this).val() == "true") { //public VLAN	
			$dialogAddNetworkForZone.find("#add_publicip_vlan_network_name_container").hide();	
			$dialogAddNetworkForZone.find("#add_publicip_vlan_network_desc_container").hide();	
			$dialogAddNetworkForZone.find("#add_publicip_vlan_cidr_container").hide();	
			addPublicipVlanTagged.append('<option value="untagged">untagged</option>').append('<option value="tagged">tagged</option>');	
		} 
		
		$dialogAddNetworkForZone.find("#add_publicip_vlan_tagged").change();
		
		// default value of "#add_publicip_vlan_scope" is "zone-wide". Calling change() will hide "#add_publicip_vlan_domain_container", "#add_publicip_vlan_account_container". 
		$dialogAddNetworkForZone.find("#add_publicip_vlan_scope").change(); 			
		
		return false;
	});
	
	if (zoneObj.networktype == "Advanced") {
		$dialogAddNetworkForZone.find("#add_publicip_vlan_tagged").change(function(event) {	
			if ($(this).val() == "tagged") {
				$dialogAddNetworkForZone.find("#add_publicip_vlan_vlan_container").show();
				$dialogAddNetworkForZone.find("#add_publicip_vlan_pod_container").hide();
								
				$dialogAddNetworkForZone.find("#add_publicip_vlan_scope").empty().append('<option value="zone-wide">zone-wide</option>').append('<option value="account-specific">account-specific</option>');
			} 
			else if($(this).val() == "untagged") {  
				$dialogAddNetworkForZone.find("#add_publicip_vlan_vlan_container").hide();
				$dialogAddNetworkForZone.find("#add_publicip_vlan_pod_container").hide();
				
				$dialogAddNetworkForZone.find("#add_publicip_vlan_scope").empty().append('<option value="zone-wide">zone-wide</option>');				
			}			
			
			// default value of "#add_publicip_vlan_scope" is "zone-wide". Calling change() will hide "#add_publicip_vlan_domain_container", "#add_publicip_vlan_account_container". 
			$dialogAddNetworkForZone.find("#add_publicip_vlan_scope").change(); 	
			
			return false;
		});
		
		$dialogAddNetworkForZone.find("#add_publicip_vlan_tagged").change();		
	} 
	else {
		$dialogAddNetworkForZone.find("#add_publicip_vlan_container").hide();
	}
	
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
	//***** binding Event Handler (end) ******   
 
    $button.show();   
    $button.unbind("click").bind("click", function(event) {  
        $dialogAddNetworkForZone.find("#info_container").hide();
        $dialogAddNetworkForZone.find("#zone_name").text(fromdb(zoneObj.name));         
		$dialogAddNetworkForZone.find("#add_publicip_vlan_vlan_container, #add_publicip_vlan_domain_container, #add_publicip_vlan_account_container").hide();
		$dialogAddNetworkForZone.find("#add_publicip_vlan_tagged, #add_publicip_vlan_vlan, #add_publicip_vlan_gateway, #add_publicip_vlan_netmask, #add_publicip_vlan_startip, #add_publicip_vlan_endip, #add_publicip_vlan_account").val("");
						
		if (zoneObj.networktype == 'Basic') {
			$dialogAddNetworkForZone.find("#add_publicip_vlan_type_container").hide();
		} else {	
			$dialogAddNetworkForZone.find("#add_publicip_vlan_pod_container").show();	
			$dialogAddNetworkForZone.find("#add_publicip_vlan_type").change();
			$dialogAddNetworkForZone.find("#add_publicip_vlan_type_container").show();
			var podSelect = $dialogAddNetworkForZone.find("#add_publicip_vlan_pod").empty();		
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
				var isTagged = $thisDialog.find("#add_publicip_vlan_tagged").val() == "tagged";
				var isDirect = $thisDialog.find("#add_publicip_vlan_type").val() == "false";
				
				isValid &= validateString("Account", $thisDialog.find("#add_publicip_vlan_account"), $thisDialog.find("#add_publicip_vlan_account_errormsg"), true); //optional
				
				if (isTagged) {
					isValid &= validateNumber("VLAN", $thisDialog.find("#add_publicip_vlan_vlan"), $thisDialog.find("#add_publicip_vlan_vlan_errormsg"), 2, 4095);
				}
				if (isDirect) {
					isValid &= validateString("Network Name", $thisDialog.find("#add_publicip_vlan_network_name"), $thisDialog.find("#add_publicip_vlan_network_name_errormsg"));
					isValid &= validateString("Network Description", $thisDialog.find("#add_publicip_vlan_network_desc"), $thisDialog.find("#add_publicip_vlan_network_desc_errormsg"));
				}
				isValid &= validateIp("Gateway", $thisDialog.find("#add_publicip_vlan_gateway"), $thisDialog.find("#add_publicip_vlan_gateway_errormsg"));
				isValid &= validateIp("Netmask", $thisDialog.find("#add_publicip_vlan_netmask"), $thisDialog.find("#add_publicip_vlan_netmask_errormsg"));
				isValid &= validateIp("Start IP Range", $thisDialog.find("#add_publicip_vlan_startip"), $thisDialog.find("#add_publicip_vlan_startip_errormsg"));   //required
				isValid &= validateIp("End IP Range", $thisDialog.find("#add_publicip_vlan_endip"), $thisDialog.find("#add_publicip_vlan_endip_errormsg"), true);  //optional
				if (!isValid) 
				    return;		
				    
				//$thisDialog.dialog("close"); 		//only close dialog when this action succeeds		
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
								
				var type = trim($thisDialog.find("#add_publicip_vlan_type").val());
				var gateway = trim($thisDialog.find("#add_publicip_vlan_gateway").val());
				var netmask = trim($thisDialog.find("#add_publicip_vlan_netmask").val());
				var startip = trim($thisDialog.find("#add_publicip_vlan_startip").val());
				var endip = trim($thisDialog.find("#add_publicip_vlan_endip").val());					
							
				if (!isDirect) {
					// Allocating ip ranges on a vlan for virtual networking
					$.ajax({
						data: createURL("command=createVlanIpRange&forVirtualNetwork="+type+"&zoneId="+zoneObj.id+vlan+scopeParams+"&gateway="+todb(gateway)+"&netmask="+todb(netmask)+"&startip="+todb(startip)+"&endip="+todb(endip)),
						dataType: "json",
						success: function(json) {	
							$thisDialog.find("#spinning_wheel").hide();
							$thisDialog.dialog("close");
						
						    /*
							var $template1 = $("#vlan_template").clone(); 							   
							$template1.find("#vlan_type_icon").removeClass("direct").addClass("virtual");	
							
							var item = json.createvlaniprangeresponse.vlan;
							vlanJsonToTemplate(item, $template1, false);	        				
							$vlanContainer.prepend($template1);	
							$template1.fadeIn("slow");
							*/
						},
						error: function(XMLHttpResponse) {
							handleError(XMLHttpResponse, function() {
								handleErrorInDialog(XMLHttpResponse, $thisDialog);	
							});
						}
					});
				} 
				else {
					// Creating network for the direct networking
					var name = todb($thisDialog.find("#add_publicip_vlan_network_name").val());
					var desc = todb($thisDialog.find("#add_publicip_vlan_network_desc").val());
					$.ajax({
						data: createURL("command=listNetworkOfferings"),
						dataType: "json",
						async: false,
						success: function(json) {
							var networkOfferings = json.listnetworkofferingsresponse.networkoffering;
							if (networkOfferings != null && networkOfferings.length > 0) {
								for (var i = 0; i < networkOfferings.length; i++) {
									if (networkOfferings[i].type == "Direct" && networkOfferings[i].isdefault) {
										// Create a network from this.
										$.ajax({
											data: createURL("command=createNetwork&name="+name+"&displayText="+desc+"&networkOfferingId="+networkOfferings[i].id+"&zoneId="+zoneObj.id+vlan+scopeParams+"&gateway="+todb(gateway)+"&netmask="+todb(netmask)+"&startip="+todb(startip)+"&endip="+todb(endip)),
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
				}
				
			}, 
			"Cancel": function() { 
				$(this).dialog("close"); 
			} 
		}).dialog("open");           
        return false;
    });
}

function initAddIpRangeToDirectNetworkButton($button, $midmenuItem1) {   
    var jsonObj = $midmenuItem1.data("jsonObj");       
    initDialog("dialog_add_iprange_to_directnetwork");        
    var $dialogAddIpRangeToDirectNetwork = $("#dialog_add_iprange_to_directnetwork");     
  
    $dialogAddIpRangeToDirectNetwork.find("#directnetwork_name").text(fromdb(jsonObj.name));
    $dialogAddIpRangeToDirectNetwork.find("#zone_name").text(fromdb(zoneObj.name));
    
    $button.show();   
    $button.unbind("click").bind("click", function(event) {           
        $("#direct_network_page").find("#tab_ipallocation").click();    
        //$dialogAddIpRangeToDirectNetwork.find("#add_publicip_vlan_startip, #add_publicip_vlan_endip").val("");
        
		$dialogAddIpRangeToDirectNetwork
		.dialog('option', 'buttons', { 	
			"Add": function() { 	
			    var $thisDialog = $(this);		
			    					
				// validate values
				var isValid = true;		
				isValid &= validateIp("Start IP Range", $thisDialog.find("#add_publicip_vlan_startip"), $thisDialog.find("#add_publicip_vlan_startip_errormsg"));   //required
				isValid &= validateIp("End IP Range", $thisDialog.find("#add_publicip_vlan_endip"), $thisDialog.find("#add_publicip_vlan_endip_errormsg"), true);  //optional
				if (!isValid) 
				    return;						    
				
				$thisDialog.find("#spinning_wheel").show()
											
				var startip = trim($thisDialog.find("#add_publicip_vlan_startip").val());
				var endip = trim($thisDialog.find("#add_publicip_vlan_endip").val());										
						
				$.ajax({
					data: createURL("command=createVlanIpRange&forVirtualNetwork=false&networkid="+todb(jsonObj.id)+"&startip="+todb(startip)+"&endip="+todb(endip)),
					dataType: "json",
					success: function(json) {	
						$thisDialog.find("#spinning_wheel").hide();
						$thisDialog.dialog("close");
					
					    var item = json.createvlaniprangeresponse.vlan;	
					    var $newTemplate = $("#iprange_template").clone();
		                directNetworkIprangeJsonToTemplate(item, $newTemplate);
		                $("#right_panel_content #direct_network_page #tab_content_ipallocation").find("#tab_container").append($newTemplate.show());					    			   
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

//***** Direct Network (end) ******************************************************************************************************

