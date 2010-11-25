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
 
 function afterLoadZoneJSP($leftmenuItem1) {
    hideMiddleMenu();  
                 
    initAddPodButton($("#midmenu_add_pod_button"), $leftmenuItem1);                  
    initAddVLANButton($("#midmenu_add_vlan_button"), $leftmenuItem1);
    initAddSecondaryStorageButton($("#midmenu_add_secondarystorage_button"), $leftmenuItem1);
          
    var pods;
    var zoneObj = $leftmenuItem1.data("jsonObj");
    var zoneId = zoneObj.id;
    var zoneName = zoneObj.name;
    $.ajax({
        data: createURL("command=listPods&zoneid="+zoneId),
        dataType: "json",
        async: false,
        success: function(json) {            
            pods = json.listpodsresponse.pod;            
        }        
    });
    if(pods != null && pods.length > 0) {
        initAddHostButtonOnZonePage($("#midmenu_add_host_button"), zoneId, zoneName); 
        initAddPrimaryStorageButtonOnZonePage($("#midmenu_add_primarystorage_button"), zoneId, zoneName);  
    }    
   
    initDialog("dialog_add_pod", 320); 
    initDialog("dialog_add_vlan_for_zone");
    initDialog("dialog_add_secondarystorage"); 
    initDialog("dialog_confirmation_delete_secondarystorage"); 
    
    // If the network type is vnet, don't show any vlan stuff.
    if (getNetworkType() == "vnet") 		
	    $("#dialog_add_vlan_for_zone").attr("title", "Add Public IP Range");		
    bindEventHandlerToDialogAddVlanForZone();	        
            
    //switch between different tabs in zone page    
    var tabArray = [$("#tab_details"), $("#tab_secondarystorage"), $("#tab_network")];
    var tabContentArray = [$("#tab_content_details"), $("#tab_content_secondarystorage"), $("#tab_content_network")];      
    var afterSwitchFnArray = [zoneJsonToDetailsTab, zoneJsonToSecondaryStorageTab, zoneJsonToNetworkTab];
    switchBetweenDifferentTabs(tabArray, tabContentArray, afterSwitchFnArray); 
                 
	zoneJsonToRightPanel($leftmenuItem1);		  
}

function zoneJsonToRightPanel($leftmenuItem1) {	 
    $("#right_panel_content").data("$leftmenuItem1", $leftmenuItem1);      
    $("#right_panel_content").find("#tab_details").click();     
}

function zoneJsonClearRightPanel() {
    zoneJsonClearDetailsTab();   
    zoneJsonClearNetworkTab();				    
    zoneJsonClearSecondaryStorageTab();
}

function zoneJsonToDetailsTab() {	 
    var $leftmenuItem1 = $("#right_panel_content").data("$leftmenuItem1");
    if($leftmenuItem1 == null)
        return;
    
    var jsonObj = $leftmenuItem1.data("jsonObj");    
    if(jsonObj == null) 
	    return;	
    
    var $thisTab = $("#right_panel_content").find("#tab_content_details");  
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();        
                 
    $thisTab.find("#id").text(noNull(jsonObj.id));
    $thisTab.find("#grid_header_title").text(fromdb(jsonObj.name));
    
    $thisTab.find("#name").text(fromdb(jsonObj.name));
    $thisTab.find("#name_edit").val(fromdb(jsonObj.name));
    
    $thisTab.find("#dns1").text(fromdb(jsonObj.dns1));
    $thisTab.find("#dns1_edit").val(fromdb(jsonObj.dns1));
    
    $thisTab.find("#dns2").text(fromdb(jsonObj.dns2));
    $thisTab.find("#dns2_edit").val(fromdb(jsonObj.dns2));
    
    $thisTab.find("#internaldns1").text(fromdb(jsonObj.internaldns1));
    $thisTab.find("#internaldns1_edit").val(fromdb(jsonObj.internaldns1));
    
    $thisTab.find("#internaldns2").text(fromdb(jsonObj.internaldns2));
    $thisTab.find("#internaldns2_edit").val(fromdb(jsonObj.internaldns2));
    
    $thisTab.find("#networktype").text(fromdb(jsonObj.networktype));
    if(jsonObj.networktype == "Basic") {
        $("#midmenu_add_vlan_button, #tab_network, #tab_content_details #vlan_container").hide();
    }
    else if(jsonObj.networktype == "Advanced") {
        $("#midmenu_add_vlan_button, #tab_network, #tab_content_details #vlan_container").show();           
        
        var vlan = jsonObj.vlan; 
        $thisTab.find("#vlan").text(fromdb(vlan));      
        if(vlan != null) {           
		    if(vlan.indexOf("-") != -1) {  //e.g. vlan == "30-33"
			    var startVlan = vlan.substring(0, vlan.indexOf("-"));
			    var endVlan = vlan.substring((vlan.indexOf("-")+1));	
			    $thisTab.find("#startvlan_edit").val(startVlan);
			    $thisTab.find("#endvlan_edit").val(endVlan);			
		    }
		    else {  //e.g. vlan == "30"
		        $thisTab.find("#startvlan_edit").val(vlan);					        
		    }
	    } 
    }	
        
    $thisTab.find("#guestcidraddress").text(fromdb(jsonObj.guestcidraddress));   
    $thisTab.find("#guestcidraddress_edit").val(fromdb(jsonObj.guestcidraddress));   
        
    $thisTab.find("#domain").text(fromdb(jsonObj.domain)); 
        
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
    buildActionLinkForTab("Edit Zone", zoneActionMap, $actionMenu, $leftmenuItem1, $thisTab);    
    buildActionLinkForTab("Delete Zone", zoneActionMap, $actionMenu, $leftmenuItem1, $thisTab);   
    
    $thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();      
}	  

function zoneJsonClearDetailsTab() {	    
    var $thisTab = $("#right_panel_content").find("#tab_content_details");    
    $thisTab.find("#grid_header_title").text("");         
    $thisTab.find("#id").text("");
    
    $thisTab.find("#name").text("");
    $thisTab.find("#name_edit").val("");
    
    $thisTab.find("#dns1").text("");
    $thisTab.find("#dns1_edit").val("");
    
    $thisTab.find("#dns2").text("");
    $thisTab.find("#dns2_edit").val("");
    
    $thisTab.find("#internaldns1").text("");
    $thisTab.find("#internaldns1_edit").val("");
    
    $thisTab.find("#internaldns2").text("");
    $thisTab.find("#internaldns2_edit").val("");
    
    $thisTab.find("#networktype").text("");	
    
    $thisTab.find("#vlan").text("");
    $thisTab.find("#startvlan_edit").val("");
	$thisTab.find("#endvlan_edit").val("");	
    
    $thisTab.find("#guestcidraddress").text("");   
    $thisTab.find("#guestcidraddress_edit").val("");  
    
    $thisTab.find("#domain").text(""); 
    
    //actions ***   
    var $actionMenu = $thisTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();   
	$actionMenu.find("#action_list").append($("#no_available_actions").clone().show());		
}	

function zoneJsonToSecondaryStorageTab() {      	
	var $leftmenuItem1 = $("#right_panel_content").data("$leftmenuItem1");
    if($leftmenuItem1 == null)
        return;
    
    var jsonObj = $leftmenuItem1.data("jsonObj");    
    if(jsonObj == null) 
	    return;		
      
    var $thisTab = $("#right_panel_content").find("#tab_content_secondarystorage");
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   
 	
    $.ajax({
		cache: false,
		data: createURL("command=listHosts&type=SecondaryStorage&zoneid="+jsonObj.id),
		dataType: "json",
		success: function(json) {			   			    
			var items = json.listhostsresponse.host;	
			var $container = $thisTab.find("#tab_container").empty();																					
			if (items != null && items.length > 0) {			    
				var $template = $("#secondary_storage_tab_template");				
				for (var i = 0; i < items.length; i++) {
					var $newTemplate = $template.clone(true);	               
	                secondaryStorageJSONToTemplate(items[i], $newTemplate); 
	                $container.append($newTemplate.show());	
				}			
			}	
			$thisTab.find("#tab_spinning_wheel").hide();    
            $thisTab.find("#tab_container").show();    		
		}
	});     
}

function zoneJsonClearSecondaryStorageTab() {   
    $("#right_panel_content").find("#tab_content_secondarystorage").empty();	
}

var $vlanContainer;
function zoneJsonToNetworkTab(jsonObj) {	
    var $leftmenuItem1 = $("#right_panel_content").data("$leftmenuItem1");	
    if($leftmenuItem1 == null)
        return;
        
	var jsonObj = $leftmenuItem1.data("jsonObj");	    
    if(jsonObj == null) 	    
	    return;	
    
    var $thisTab = $("#right_panel_content").find("#tab_content_network");
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();  		
        
    $thisTab.find("#zone_cloud").find("#zone_name").text(fromdb(jsonObj.name));	 
    $thisTab.find("#zone_vlan").text(jsonObj.vlan);   
                  
    $.ajax({
	  data: createURL("command=listVlanIpRanges&zoneId="+jsonObj.id),
		dataType: "json",
		success: function(json) {
			var items = json.listvlaniprangesresponse.vlaniprange;		
			$vlanContainer = $thisTab.find("#vlan_container").empty();   					
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
			$thisTab.find("#tab_spinning_wheel").hide();    
            $thisTab.find("#tab_container").show();    
		}
	});
}	 

function zoneJsonClearNetworkTab() {	    
    var $thisTab = $("#right_panel_content").find("#tab_content_network");      
    $thisTab.find("#zone_cloud").find("#zone_name").text("");	 
    $thisTab.find("#zone_vlan").text("");   
    $thisTab.find("#vlan_container").empty();    
}	

function vlanJsonToTemplate(jsonObj, $template1) {
    $template1.data("jsonObj", jsonObj);
    $template1.find("#vlan_id").text(jsonObj.vlan);
    $template1.find("#ip_range").text(jsonObj.description);
    $template1.unbind("click").bind("click", function(event) {        
        var $target = $(event.target);
        var targetId = $target.attr("id");     
        switch(targetId) {
            case "info_icon": //show info dropdown 
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
                
            case "close_link":  //hide info dropdown              
                $target.parent().parent().hide();
                break;
            
            case "delete_vlan": //delete VLAN               
                $.ajax({
                    data: createURL("command=deleteVlanIpRange&id="+jsonObj.id),
                    dataType: "json",
                    success: function(json) {                        
                        $template1.slideUp("slow", function() {
                            $(this).remove();
                        });
                    }
                });
                break;
        }
        
        return false;
    });
} 	


function initAddVLANButton($button, $leftmenuItem1) {    
    $button.show();   
    $button.unbind("click").bind("click", function(event) {  
        if($("#tab_content_network").css("display") == "none")
            $("#tab_network").click();      
            
        var zoneObj = $leftmenuItem1.data("jsonObj");  
        var dialogAddVlanForZone = $("#dialog_add_vlan_for_zone");       
        dialogAddVlanForZone.find("#info_container").hide();
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
			if(zoneObj.domainid != null) { //list only domains under zoneObj.domainid
			    domainSelect.append("<option value='" + zoneObj.domainid + "'>" + fromdb(zoneObj.domain) + "</option>"); 	
			        									
			    function populateDomainDropdown(id) {					        
                    $.ajax({
	                    data: createURL("command=listDomainChildren&id="+id+"&pageSize=-1"),
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

		dialogAddVlanForZone
		.dialog('option', 'buttons', { 	
			"Add": function() { 	
			    var $thisDialog = $(this);		
			    					
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
				$thisDialog.find("#spinning_wheel").show()
				
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
					    $thisDialog.find("#spinning_wheel").hide();
					    $thisDialog.dialog("close");
					
					    var $template1 = $("#vlan_template").clone(); 							   
				        if(type == "false") //direct  
				            $template1.find("#vlan_type_icon").removeClass("virtual").addClass("direct");
				        else  //virtual
				  	        $template1.find("#vlan_type_icon").removeClass("direct").addClass("virtual");	
        				
        				var item = json.createvlaniprangeresponse.vlan;
       				    vlanJsonToTemplate(item, $template1);	        				
				        $vlanContainer.prepend($template1);	
				        $template1.fadeIn("slow");
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


function initAddSecondaryStorageButton($button, $leftmenuItem1) {    
    $button.show();      
    $button.unbind("click").bind("click", function(event) {
        if($("#tab_content_secondarystorage").css("display") == "none")
            $("#tab_secondarystorage").click();    
        
        var zoneObj = $leftmenuItem1.data("jsonObj");    
        $("#dialog_add_secondarystorage").find("#zone_name").text(fromdb(zoneObj.name));   
        $("#dialog_add_secondarystorage").find("#info_container").hide();		    
   
        $("#dialog_add_secondarystorage")
	    .dialog('option', 'buttons', { 				    
		    "Add": function() { 
		        var $thisDialog = $(this);	
	            
			    // validate values					
			    var isValid = true;							    
			    isValid &= validateString("NFS Server", $thisDialog.find("#nfs_server"), $thisDialog.find("#nfs_server_errormsg"));	
			    isValid &= validatePath("Path", $thisDialog.find("#path"), $thisDialog.find("#path_errormsg"));					
			    if (!isValid) 
			        return;
			    
				$thisDialog.find("#spinning_wheel").show()
								     					  								            				
			    var zoneId = zoneObj.id;		
			    var nfs_server = trim($thisDialog.find("#nfs_server").val());		
			    var path = trim($thisDialog.find("#path").val());	    					    				    					   					
				var url = nfsURL(nfs_server, path);  
			    				  
			    $.ajax({
				    data: createURL("command=addSecondaryStorage&zoneId="+zoneId+"&url="+encodeURIComponent(url)),
				    dataType: "json",
				    success: function(json) {	
				        $thisDialog.find("#spinning_wheel").hide();				        
				        $thisDialog.dialog("close");
					
					    var $subgridItem = $("#secondary_storage_tab_template").clone(true);	                        
				        secondaryStorageJSONToTemplate(json.addsecondarystorageresponse.secondarystorage, $subgridItem);	
	                    $subgridItem.find("#after_action_info").text("Secondary storage was added successfully.");
                        $subgridItem.find("#after_action_info_container").removeClass("error").addClass("success").show();  
                        $("#tab_content_secondarystorage").append($subgridItem.show());  
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

function initAddPodButton($button, $leftmenuItem1) {
    $button.show();     
    $button.unbind("click").bind("click", function(event) {   
        var zoneObj = $leftmenuItem1.data("jsonObj"); 
        $("#dialog_add_pod").find("#info_container").hide();				  	
        $("#dialog_add_pod").find("#add_pod_zone_name").text(fromdb(zoneObj.name));
        $("#dialog_add_pod #add_pod_name, #dialog_add_pod #add_pod_cidr, #dialog_add_pod #add_pod_startip, #dialog_add_pod #add_pod_endip, #add_pod_gateway").val("");
		
        $("#dialog_add_pod")
        .dialog('option', 'buttons', { 				
	        "Add": function() {		
	            var $thisDialog = $(this);
						
		        // validate values
		        var isValid = true;					
		        isValid &= validateString("Name", $thisDialog.find("#add_pod_name"), $thisDialog.find("#add_pod_name_errormsg"));
		        isValid &= validateCIDR("CIDR", $thisDialog.find("#add_pod_cidr"), $thisDialog.find("#add_pod_cidr_errormsg"));	
		        isValid &= validateIp("Start IP Range", $thisDialog.find("#add_pod_startip"), $thisDialog.find("#add_pod_startip_errormsg"));  //required
		        isValid &= validateIp("End IP Range", $thisDialog.find("#add_pod_endip"), $thisDialog.find("#add_pod_endip_errormsg"), true);  //optional
		        isValid &= validateIp("Gateway", $thisDialog.find("#add_pod_gateway"), $thisDialog.find("#add_pod_gateway_errormsg"));  //required when creating
		        if (!isValid) 
		            return;			
                
                $thisDialog.find("#spinning_wheel").show()
                  
                var name = trim($thisDialog.find("#add_pod_name").val());
		        var cidr = trim($thisDialog.find("#add_pod_cidr").val());
		        var startip = trim($thisDialog.find("#add_pod_startip").val());
		        var endip = trim($thisDialog.find("#add_pod_endip").val());	    //optional
		        var gateway = trim($thisDialog.find("#add_pod_gateway").val());			

                var array1 = [];
                array1.push("&zoneId="+zoneObj.id);
                array1.push("&name="+todb(name));
                array1.push("&cidr="+encodeURIComponent(cidr));
                array1.push("&startIp="+encodeURIComponent(startip));
                if (endip != null && endip.length > 0)
                    array1.push("&endIp="+encodeURIComponent(endip));
                array1.push("&gateway="+encodeURIComponent(gateway));			
								
		        $.ajax({
		          data: createURL("command=createPod"+array1.join("")), 
			        dataType: "json",
			        success: function(json) {
			            $thisDialog.find("#spinning_wheel").hide();
			            $thisDialog.dialog("close");
			            
			            var item = json.createpodresponse.pod; 			            		            				    
		                var template = $("#leftmenu_pod_node_template").clone(true);
		                podJSONToTreeNode(item, template);	
		                var $zoneNode = $("#leftmenu_zone_tree").find("#tree_container").find("#zone_" + zoneObj.id);	 
		                $zoneNode.find("#pods_container").prepend(template.show());			
		                template.fadeIn("slow");
		                    			                                    
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
				        
				        //expand zone node to show the newly added pod
				        if($zoneNode.find("#zone_arrow").hasClass("expanded_close"))
				            $zoneNode.find("#zone_arrow").click();  
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

function secondaryStorageJSONToTemplate(json, template) {
    template.data("jsonObj", json);
    template.attr("id", "secondaryStorage_"+json.id).data("secondaryStorageId", json.id);   	
   	template.find("#id").text(json.id);
   	template.find("#title").text(fromdb(json.name));
   	template.find("#name").text(fromdb(json.name));
   	template.find("#zonename").text(fromdb(json.zonename));	
	template.find("#type").text(json.type);	
    template.find("#ipaddress").text(json.ipaddress);
       
    setHostStateInRightPanel(fromdb(json.state), template.find("#state"))
    
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


function bindEventHandlerToDialogAddVlanForZone() {
    //direct VLAN shows only "tagged" option while public VLAN shows both "tagged" and "untagged" option. 		
	var dialogAddVlanForZone = $("#dialog_add_vlan_for_zone");
			
	dialogAddVlanForZone.find("#add_publicip_vlan_type").change(function(event) {
	    var addPublicipVlanTagged = dialogAddVlanForZone.find("#add_publicip_vlan_tagged").empty();
	   		
		if ($(this).val() == "false") { //direct VLAN (only tagged option)		
			addPublicipVlanTagged.append('<option value="tagged">tagged</option>');
							
			dialogAddVlanForZone.find("#add_publicip_vlan_vlan_container").show();			
			dialogAddVlanForZone.find("#add_publicip_vlan_pod_container").hide();
			
		} 
		else if ($(this).val() == "true") { //public VLAN	
			addPublicipVlanTagged.append('<option value="untagged">untagged</option>').append('<option value="tagged">tagged</option>');	
		} 
		
		dialogAddVlanForZone.find("#add_publicip_vlan_tagged").change();
		
		// default value of "#add_publicip_vlan_scope" is "zone-wide". Calling change() will hide "#add_publicip_vlan_domain_container", "#add_publicip_vlan_account_container". 
		dialogAddVlanForZone.find("#add_publicip_vlan_scope").change(); 			
		
		return false;
	});
			
	if (getNetworkType() != "vnet") {
		dialogAddVlanForZone.find("#add_publicip_vlan_tagged").change(function(event) {	
			if ($(this).val() == "tagged") {
				dialogAddVlanForZone.find("#add_publicip_vlan_vlan_container").show();
				dialogAddVlanForZone.find("#add_publicip_vlan_pod_container").hide();
								
				dialogAddVlanForZone.find("#add_publicip_vlan_scope").empty().append('<option value="zone-wide">zone-wide</option>').append('<option value="account-specific">account-specific</option>');
			} 
			else if($(this).val() == "untagged") {  
				dialogAddVlanForZone.find("#add_publicip_vlan_vlan_container").hide();
				dialogAddVlanForZone.find("#add_publicip_vlan_pod_container").hide();
				
				dialogAddVlanForZone.find("#add_publicip_vlan_scope").empty().append('<option value="zone-wide">zone-wide</option>');				
			}			
			
			// default value of "#add_publicip_vlan_scope" is "zone-wide". Calling change() will hide "#add_publicip_vlan_domain_container", "#add_publicip_vlan_account_container". 
			dialogAddVlanForZone.find("#add_publicip_vlan_scope").change(); 	
			
			return false;
		});
		
		dialogAddVlanForZone.find("#add_publicip_vlan_tagged").change();		
	} 
	else {
		dialogAddVlanForZone.find("#add_publicip_vlan_container").hide();
	}
	
	dialogAddVlanForZone.find("#add_publicip_vlan_scope").change(function(event) {	   
	    if($(this).val() == "zone-wide") {
	        dialogAddVlanForZone.find("#add_publicip_vlan_domain_container").hide();
			dialogAddVlanForZone.find("#add_publicip_vlan_account_container").hide();    
	    } 
	    else if($(this).val() == "account-specific") { 
	        dialogAddVlanForZone.find("#add_publicip_vlan_domain_container").show();
			dialogAddVlanForZone.find("#add_publicip_vlan_account_container").show();    
	    }		    
	    return false;
	});
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
		    var $thisDialog = $(this);	
			$thisDialog.dialog("close");       	                                             
         
            var name = $thisDialog.find("#name").val();	                
             
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
    "Edit Zone": {
        dialogBeforeActionFn: doEditZone  
    },
    "Delete Zone": {  
        api: "deleteZone",            
        isAsyncJob: false,        
        inProcessText: "Deleting Zone....",
        afterActionSeccessFn: function(json, $leftmenuItem1, id) {   
            $leftmenuItem1.slideUp(function() {
                $(this).remove();
            });
            clearRightPanel();
            zoneJsonClearRightPanel();
        }
    }
}


function doEditZone($actionLink, $detailsTab, $leftmenuItem1) {       
    var $readonlyFields  = $detailsTab.find("#name, #dns1, #dns2, #internaldns1, #internaldns2, #vlan, #guestcidraddress");
    var $editFields = $detailsTab.find("#name_edit, #dns1_edit, #dns2_edit, #internaldns1_edit, #internaldns2_edit, #startvlan_edit, #endvlan_edit, #guestcidraddress_edit");
           
    $readonlyFields.hide();
    $editFields.show();  
    $detailsTab.find("#cancel_button, #save_button").show();
    
    $detailsTab.find("#cancel_button").unbind("click").bind("click", function(event){    
        $editFields.hide();
        $readonlyFields.show();   
        $("#save_button, #cancel_button").hide();       
        return false;
    });
    $detailsTab.find("#save_button").unbind("click").bind("click", function(event){        
        doEditZone2($actionLink, $detailsTab, $leftmenuItem1, $readonlyFields, $editFields);   
        return false;
    });   
}

function doEditZone2($actionLink, $detailsTab, $leftmenuItem1, $readonlyFields, $editFields) {    
    // validate values
	var isValid = true;			
	isValid &= validateString("Name", $detailsTab.find("#name_edit"), $detailsTab.find("#name_edit_errormsg"));
	isValid &= validateIp("DNS 1", $detailsTab.find("#dns1_edit"), $detailsTab.find("#dns1_edit_errormsg"), false);	//required
	isValid &= validateIp("DNS 2", $detailsTab.find("#dns2_edit"), $detailsTab.find("#dns2_edit_errormsg"), true);	//optional	
	isValid &= validateIp("Internal DNS 1", $detailsTab.find("#internaldns1_edit"), $detailsTab.find("#internaldns1_edit_errormsg"), false);	//required
	isValid &= validateIp("Internal DNS 2", $detailsTab.find("#internaldns2_edit"), $detailsTab.find("#internaldns2_edit_errormsg"), true);	//optional						
	if ($("#tab_content_details #vlan_container").css("display") != "none") {
		isValid &= validateString("Start VLAN Range", $detailsTab.find("#startvlan_edit"), $detailsTab.find("#startvlan_edit_errormsg"), true); //optional (Bug 5730 requested to change VLAN to be optional when updating zone)
		isValid &= validateString("End VLAN Range", $detailsTab.find("#endvlan_edit"), $detailsTab.find("#endvlan_edit_errormsg"), true);  //optional
	}
	isValid &= validateCIDR("Guest CIDR", $detailsTab.find("#guestcidraddress_edit"), $detailsTab.find("#guestcidraddress_edit_errormsg"), false);	//required					
	if (!isValid) 
	    return;							
	
	var moreCriteria = [];	
	
	var jsonObj = $leftmenuItem1.data("jsonObj"); 
	
	var oldDns1 = jsonObj.dns1;
	var oldDns2 = jsonObj.dns2;	
	
	var name = $detailsTab.find("#name_edit").val();
	if(name != jsonObj.name)
	    moreCriteria.push("&name="+todb(name));
	
	var dns1 = $detailsTab.find("#dns1_edit").val();
	if(dns1 != jsonObj.dns1)
	    moreCriteria.push("&dns1="+encodeURIComponent(dns1));
	
	var dns2 = $detailsTab.find("#dns2_edit").val();
	if (dns2 != null && dns2.length > 0 && dns2 != jsonObj.dns2) 
		moreCriteria.push("&dns2="+encodeURIComponent(dns2));	
	
	var internaldns1 = $detailsTab.find("#internaldns1_edit").val();
	if(internaldns1 != jsonObj.internaldns1)
	    moreCriteria.push("&internaldns1="+encodeURIComponent(internaldns1));
	
	var internaldns2 = $detailsTab.find("#internaldns2_edit").val();	
	if (internaldns2 != null && internaldns2.length > 0 && internaldns2 != jsonObj.internaldns2) 
		moreCriteria.push("&internaldns2="+encodeURIComponent(internaldns2));						
	
	var vlan;				
	if ($("#tab_content_details #vlan_container").css("display") != "none") {
		var vlanStart = $detailsTab.find("#startvlan_edit").val();	
		if(vlanStart != null && vlanStart.length > 0) {
		    var vlanEnd = $detailsTab.find("#endvlan_edit").val();						
		    if (vlanEnd != null && vlanEnd.length > 0) 
		        vlan = vlanStart + "-" + vlanEnd;						    							
		    else 	
		        vlan = vlanStart;							
                      
            if(vlan != jsonObj.vlan)
               moreCriteria.push("&vlan=" + encodeURIComponent(vlan));	
        }
	}				
	
	var guestcidraddress = $detailsTab.find("#guestcidraddress_edit").val();
	if(guestcidraddress != jsonObj.guestcidraddress)
	    moreCriteria.push("&guestcidraddress="+encodeURIComponent(guestcidraddress));				    		 
	 
	if(moreCriteria.length > 0) { 	        	
	    $.ajax({
	      data: createURL("command=updateZone&id="+jsonObj.id+moreCriteria.join("")),
		    dataType: "json",
		    success: function(json) {		   
		        var item = json.updatezoneresponse.zone;		  
		        $leftmenuItem1.data("jsonObj", item);
		        $leftmenuItem1.find("#zone_name").text(item.name);
		        zoneJsonToRightPanel($leftmenuItem1);	
    		    
		        $editFields.hide();      
                $readonlyFields.show();       
                $("#save_button, #cancel_button").hide();  
                           
                if(item.dns1 != oldDns1 || item.dns2 != oldDns2) {
                    $("#dialog_info")
                    .text("DNS update will not take effect until all virtual routers are stopped and then started")
                    .dialog("open"); 
                }               	    
		    }
	    }); 
	}  
	else {
	    $editFields.hide();      
        $readonlyFields.show();       
        $("#save_button, #cancel_button").hide();  
	}
}

function initAddHostButtonOnZonePage($button, zoneId, zoneName) {
    $button.show();
    
    initDialog("dialog_add_host_in_zone_page");    
    var $dialogAddHost = $("#dialog_add_host_in_zone_page");    
    
    var $podSelect = $dialogAddHost.find("#pod_dropdown");
        
    $podSelect.bind("change", function(event) {	        	   
        var podId = $(this).val();
        if(podId == null || podId.length == 0)
            return;        
        refreshClsuterFieldInAddHostDialog($dialogAddHost, podId, null);        
    });   
    
    $button.unbind("click").bind("click", function(event) { 
        $dialogAddHost.find("#zone_name").text(zoneName);             
        $dialogAddHost.find("#info_container").hide();    
        $dialogAddHost.find("#new_cluster_name").val("");
        
        $.ajax({
            data: createURL("command=listPods&zoneid="+zoneId),
            dataType: "json",
            async: false,
            success: function(json) {            
                var pods = json.listpodsresponse.pod;   
                $podSelect.empty(); 
                if(pods != null && pods.length > 0) {
                    for(var i=0; i<pods.length; i++)
                        $podSelect.append("<option value='" + pods[i].id + "'>" + fromdb(pods[i].name) + "</option>"); 	
                }  
                $podSelect.change();        
            }        
        });    
	    
        $dialogAddHost
        .dialog('option', 'buttons', { 				
	        "Add": function() { 
	            var $thisDialog = $(this);		            
	    			   
		        var clusterRadio = $thisDialog.find("input[name=cluster]:checked").val();				
			
		        // validate values
		        var isValid = true;			       	
		        isValid &= validateDropDownBox("Pod", $thisDialog.find("#pod_dropdown"), $thisDialog.find("#pod_dropdown_errormsg"));									
		        isValid &= validateString("Host name", $thisDialog.find("#host_hostname"), $thisDialog.find("#host_hostname_errormsg"));
		        isValid &= validateString("User name", $thisDialog.find("#host_username"), $thisDialog.find("#host_username_errormsg"));
		        isValid &= validateString("Password", $thisDialog.find("#host_password"), $thisDialog.find("#host_password_errormsg"));	
				if(clusterRadio == "new_cluster_radio") {
					isValid &= validateString("Cluster Name", $thisDialog.find("#new_cluster_name"), $thisDialog.find("#new_cluster_name_errormsg"));
				}
		        if (!isValid) 
		            return;		            			
					
				$thisDialog.find("#spinning_wheel").show() 				
				
		        var array1 = [];    
		        		    
		        array1.push("&zoneid="+zoneId);
		        
		        //expand zone in left menu tree (to show pod, cluster under the zone) 
				var $zoneNode = $("#leftmenu_zone_tree").find("#tree_container").find("#zone_" + zoneId);							
				if($zoneNode.find("#zone_arrow").hasClass("expanded_close"))
				    $zoneNode.find("#zone_arrow").click();
										             
		        var podId = $thisDialog.find("#pod_dropdown").val();
		        array1.push("&podid="+podId);
						      
		        var username = trim($thisDialog.find("#host_username").val());
		        array1.push("&username="+encodeURIComponent(username));
				
		        var password = trim($thisDialog.find("#host_password").val());
		        array1.push("&password="+encodeURIComponent(password));
					
				var newClusterName, existingClusterId;							
			    if(clusterRadio == "new_cluster_radio") {
		            newClusterName = trim($thisDialog.find("#new_cluster_name").val());
		            array1.push("&clustername="+todb(newClusterName));				    
		        }
		        else if(clusterRadio == "existing_cluster_radio") {			            
		            existingClusterId = $thisDialog.find("#cluster_select").val();
				    // We will default to no cluster if someone selects Join Cluster with no cluster available.
				    if (existingClusterId != '-1') {
					    array1.push("&clusterid="+existingClusterId);
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
			            $thisDialog.find("#spinning_wheel").hide();
			            $thisDialog.dialog("close");
					
					    //showMiddleMenu();
					    		                
					    /*
					    var items = json.addhostresponse.host;	
					    var $midmenuItem1 = $("#midmenu_item").clone();
                        $("#midmenu_container").append($midmenuItem1.fadeIn("slow"));                        			            			      										   
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
                        */                             
                        
                        clickClusterNodeAfterAddHost(clusterRadio, podId, newClusterName, existingClusterId, $thisDialog);                                  
			        },			
                    error: function(XMLHttpResponse) {	            
						handleError(XMLHttpResponse, function() {						  
							refreshClsuterFieldInAddHostDialog($thisDialog, podId, null);                     
							handleErrorInDialog(XMLHttpResponse, $thisDialog);
							if(clusterRadio == "new_cluster_radio") {    //*** new cluster ***                         
                               refreshClusterUnderPod($("#pod_" + podId), newClusterName, null, true);  //refresh clusters under pod, but no clicking at any cluster                        
                               $thisDialog.find("#new_cluster_name").val("");   
                           }   
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

function initAddPrimaryStorageButtonOnZonePage($button, zoneId, zoneName) {
    $button.show();

	initDialog("dialog_add_pool_in_zone_page");
	var $dialogAddPool = $("#dialog_add_pool_in_zone_page");    
	
    // if hypervisor is KVM, limit the server option to NFS for now
    if (getHypervisorType() == 'kvm') 
	    $dialogAddPool.find("#add_pool_protocol").empty().html('<option value="nfs">NFS</option>');	
    bindEventHandlerToDialogAddPool($dialogAddPool);	
       
	var $podSelect = $dialogAddPool.find("#pod_dropdown");
    
    $podSelect.bind("change", function(event) {			   
        var podId = $(this).val();
        if(podId == null || podId.length == 0)
            return;
        var $clusterSelect = $dialogAddPool.find("#cluster_select").empty();		        
        $.ajax({
	       data: createURL("command=listClusters&podid=" + podId),
            dataType: "json",
            success: function(json) {			            
                var items = json.listclustersresponse.cluster;
                if(items != null && items.length > 0) {			                
                    for(var i=0; i<items.length; i++) 			                    
                        $clusterSelect.append("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>");		      
                    $dialogAddPool.find("input[value=existing_cluster_radio]").attr("checked", true);
                }
                else {
				    $clusterSelect.append("<option value='-1'>None Available</option>");
                    $dialogAddPool.find("input[value=new_cluster_radio]").attr("checked", true);
                }
            }
        });
    });        
    
    $button.unbind("click").bind("click", function(event) { 
        $dialogAddPool.find("#zone_name").text(zoneName);        
        $dialogAddPool.find("#zone_dropdown").change(); //refresh cluster dropdown (do it here to avoid race condition)     
        $dialogAddPool.find("#info_container").hide();	
        
        $.ajax({
            data: createURL("command=listPods&zoneid="+zoneId),
            dataType: "json",
            async: false,
            success: function(json) {            
                var pods = json.listpodsresponse.pod;   
                $podSelect.empty(); 
                if(pods != null && pods.length > 0) {
                    for(var i=0; i<pods.length; i++)
                        $podSelect.append("<option value='" + pods[i].id + "'>" + fromdb(pods[i].name) + "</option>"); 	
                }   
                $podSelect.change();    
            }        
        });          
                       
        $("#dialog_add_pool_in_zone_page")
	    .dialog('option', 'buttons', { 				    
		    "Add": function() { 	
		    	var $thisDialog = $(this);
		    	
			    // validate values
				var protocol = $thisDialog.find("#add_pool_protocol").val();
				
			    var isValid = true;					    
		        isValid &= validateDropDownBox("Pod", $thisDialog.find("#pod_dropdown"), $thisDialog.find("#pod_dropdown_errormsg"));						    
			    isValid &= validateDropDownBox("Cluster", $thisDialog.find("#cluster_select"), $thisDialog.find("#cluster_select_errormsg"), false);  //required, reset error text					    				
			    isValid &= validateString("Name", $thisDialog.find("#add_pool_name"), $thisDialog.find("#add_pool_name_errormsg"));
			    isValid &= validateString("Server", $thisDialog.find("#add_pool_nfs_server"), $thisDialog.find("#add_pool_nfs_server_errormsg"));	
				if (protocol == "nfs") {
					isValid &= validateString("Path", $thisDialog.find("#add_pool_path"), $thisDialog.find("#add_pool_path_errormsg"));	
				} else {
					isValid &= validateString("Target IQN", $thisDialog.find("#add_pool_iqn"), $thisDialog.find("#add_pool_iqn_errormsg"));	
					isValid &= validateString("LUN #", $thisDialog.find("#add_pool_lun"), $thisDialog.find("#add_pool_lun_errormsg"));	
				}
				isValid &= validateString("Tags", $thisDialog.find("#add_pool_tags"), $thisDialog.find("#add_pool_tags_errormsg"), true);	//optional
			    if (!isValid) 
			        return;
			        			    
				$thisDialog.find("#spinning_wheel").show()  
							
				var array1 = [];
								
		        array1.push("&zoneid="+zoneId);
		        
		        //expand zone in left menu tree (to show pod, cluster under the zone) 
				var $zoneNode = $("#leftmenu_zone_tree").find("#tree_container").find("#zone_" + zoneId);							
				if($zoneNode.find("#zone_arrow").hasClass("expanded_close"))
				    $zoneNode.find("#zone_arrow").click();
				
				var podId = $thisDialog.find("#pod_dropdown").val();
		        array1.push("&podId="+podId);
				
				var clusterId = $thisDialog.find("#cluster_select").val();
			    array1.push("&clusterid="+clusterId);	
				
			    var name = trim($thisDialog.find("#add_pool_name").val());
			    array1.push("&name="+todb(name));
			    
			    var server = trim($thisDialog.find("#add_pool_nfs_server").val());						
				
				var url = null;
				if (protocol == "nfs") {
					var path = trim($thisDialog.find("#add_pool_path").val());
					if(path.substring(0,1)!="/")
						path = "/" + path; 
					url = nfsURL(server, path);
				} else {
					var iqn = trim($thisDialog.find("#add_pool_iqn").val());
					if(iqn.substring(0,1)!="/")
						iqn = "/" + iqn; 
					var lun = trim($thisDialog.find("#add_pool_lun").val());
					url = iscsiURL(server, iqn, lun);
				}
				array1.push("&url="+encodeURIComponent(url));
				
			    var tags = trim($thisDialog.find("#add_pool_tags").val());
				if(tags != null && tags.length > 0)
				    array1.push("&tags="+todb(tags));				    
			    
			    $.ajax({
				    data: createURL("command=createStoragePool" + array1.join("")),
				    dataType: "json",
				    success: function(json) {				       			               
					    if(isMiddleMenuShown() == false) { //not on cluster node (still on pod node, so middle menu is hidden)
					        var $clusterNode = $("#cluster_"+clusterId);
					        if($clusterNode.length > 0)
					  	        $("#cluster_"+clusterId).find("#cluster_name").click();		
					  	    else  //pod node is close. Expand pod node.	
					  	        refreshClusterUnderPod($("#pod_" + podId), null, clusterId);
					    }
					    else {	
					        var $container = $("#midmenu_container").find("#midmenu_primarystorage_container");
					        var $noItemsAvailable = $container.siblings("#midmenu_container_no_items_available");
					        if($noItemsAvailable.length > 0) {
					            $noItemsAvailable.slideUp("slow", function() {
					                $(this).remove();
					            });
					        }					            
					        
					        var $midmenuItem1 = $("#midmenu_item").clone();
                            $container.append($midmenuItem1.fadeIn("slow"));
				            var item = json.createstoragepoolresponse.storagepool;				            			      										   
					        primarystorageToMidmenu(item, $midmenuItem1);
	                        bindClickToMidMenu($midmenuItem1, primarystorageToRightPanel, primarystorageGetMidmenuId);  
	                    }	                                  	    	
	                    
	                    $thisDialog.find("#spinning_wheel").hide();					       
				        $thisDialog.dialog("close");	                                                                 
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