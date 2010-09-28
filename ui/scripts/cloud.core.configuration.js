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

// Version: @VERSION@

function showConfigurationTab() {
	var forceLogout = true;  // We force a logout only if the user has first added a POD for the very first time

	// Manage Configuration
	var currentSubMenu = $("#submenu_global");
	
	activateDialog($("#dialog_edit_global").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));
	
	$("#global_template").bind("click", function(event) {
		var template = $(this);
		var link = $(event.target);
		var linkAction = link.attr("id");
		var name = template.data("name");
		switch (linkAction) {
			case "global_action_edit" :
				$("#edit_global_name").text(name);
				$("#edit_global_value").val(template.find("#global_value").text());
									
				$("#dialog_edit_global")
				.dialog('option', 'buttons', { 						
					"Confirm": function() {		
					    var thisDialog = $(this);
									
					    // validate values
			            var isValid = true;					
			            isValid &= validateString("Value", thisDialog.find("#edit_global_value"), thisDialog.find("#edit_global_value_errormsg"));					
			            if (!isValid) return;
						
					    var value = trim(thisDialog.find("#edit_global_value").val());
						
					    thisDialog.dialog("close");
					    $.ajax({
						    data: "command=updateConfiguration&name="+encodeURIComponent(name)+"&value="+encodeURIComponent(value)+"&response=json",
						    dataType: "json",
						    success: function(json) {
							    template.find("#global_value").text(value);
							    $("#dialog_alert").html("<p><b>PLEASE RESTART YOUR MGMT SERVER!!</b><br/><b>PLEASE RESTART YOUR MGMT SERVER!!</b><br/><br/>You have successfully change a global configuration value.  Please <b>RESTART</b> your management server for your new settings to take effect.  Refer to the install guide for instructions on how to restart the mgmt server.</p>").dialog("open");
						    }
					    });		
					},
					"Cancel": function() { 
						$(this).dialog("close"); 
					}						
				}).dialog("open");									   
		}
		return false;
	});
	
	function globalJSONToTemplate(json, template) {
	    template.data("name", fromdb(json.name)).attr("id", "global_"+json.name);
	    (index++ % 2 == 0)? template.addClass("smallrow_even"): template.addClass("smallrow_odd");		
		template.find("#global_name").text(json.name);
		template.find("#global_value").text(json.value);
		template.find("#global_desc").text(json.description);
	}
					
	function listConfigurations() {		 
	    var submenuContent = $("#submenu_content_global");
	   
    	var commandString;            
		var advanced = submenuContent.find("#search_button").data("advanced");                    
		if (advanced != null && advanced) {		
		    var name = submenuContent.find("#advanced_search #adv_search_name").val();				   
		    var moreCriteria = [];								
			if (name!=null && trim(name).length > 0) 
				moreCriteria.push("&name="+encodeURIComponent(trim(name)));					
			commandString = "command=listConfigurations&page="+currentPage+moreCriteria.join("")+"&response=json";  
		} else {          
            var searchInput = submenuContent.find("#search_input").val();            
            if (searchInput != null && searchInput.length > 0) 
                commandString = "command=listConfigurations&page="+currentPage+"&keyword="+searchInput+"&response=json";
            else
                commandString = "command=listConfigurations&page="+currentPage+"&response=json";
        }
    	 
    	//listItems(submenuContent, commandString, jsonResponse1, jsonResponse2, template, fnJSONToTemplate);         
        listItems(submenuContent, commandString, "listconfigurationsresponse", "configuration", $("#global_template"), globalJSONToTemplate);     
	}
	
	submenuContentEventBinder($("#submenu_content_global"), listConfigurations);
	
	$("#submenu_global").bind("click", function(event) {			        
	    event.preventDefault();	            
		
		$(this).toggleClass("submenu_links_on").toggleClass("submenu_links_off");
		currentSubMenu.toggleClass("submenu_links_off").toggleClass("submenu_links_on");
		currentSubMenu = $(this);
		var submenuContent = $("#submenu_content_global").show();
		$("#submenu_content_zones, #submenu_content_service, #submenu_content_disk").hide();			
		
		currentPage = 1;
		listConfigurations();
	});
	
	
	//zone	
	var rightPanel = $("#submenu_content_zones #right_panel_detail_title");
	var rightContent = $("#submenu_content_zones #right_panel_detail_content");
		
	function clearRightPanel() {
	    rightPanel.empty();
	    rightContent.empty();	
	    $("#submenu_content_zones").find("#action_edit_zone, #action_add_pod, #action_edit_pod, #action_add_publicip_vlan, #action_add_directip_vlan, #action_delete").hide();				
	}	
				
	function zoneObjectToRightPanel(obj) {
        rightPanel.html("<strong>Zone:</strong> "+fromdb(obj.name));					
		var rightContentHtml = 
			"<p><span>ZONE:</span> "+fromdb(obj.name)+"</p>"
			+ "<p><span>DNS 1:</span> "+obj.dns1+"</p>"
			+ "<p><span>DNS 2:</span> "+((obj.dns2 == null) ? "" : obj.dns2) +"</p>"
			+ "<p><span>Internal DNS 1:</span> "+obj.internaldns1+"</p>"
			+ "<p><span>Internal DNS 2:</span> "+((obj.internaldns2 == null) ? "" : obj.internaldns2) +"</p>";			
		if (getNetworkType() != "vnet") 
			rightContentHtml += "<p><span>VLAN:</span> "+((obj.vlan == null) ? "" : obj.vlan) +"</p>";			
		rightContentHtml += "<p><span>Guest CIDR:</span> "+obj.guestcidraddress+"</p>";			
		
		rightContent.data("id", obj.id).html(rightContentHtml);		
		
		$("#submenu_content_zones").find("#action_edit_pod, #action_add_directip_vlan").hide();			
		
		var buttons = $("#submenu_content_zones #action_delete, #submenu_content_zones #action_edit_zone, #submenu_content_zones #action_add_pod, #submenu_content_zones #action_add_publicip_vlan").data("type", "zone").show();			
		buttons.data("id", obj.id);			
		buttons.data("name", obj.name);		    
	    buttons.data("dns1", obj.dns1);
	    buttons.data("dns2", obj.dns2);
	    buttons.data("internaldns1", obj.internaldns1);
	    buttons.data("internaldns2", obj.internaldns2);
	    buttons.data("vlan", obj.vlan);		
	    buttons.data("guestcidraddress", obj.guestcidraddress);		   
	}	
	
	function podObjectToRightPanel(obj) {		    
		rightPanel.html("<strong>Pod:</strong> " + fromdb(obj.name));
					
		var rightContentHtml = 
			"<p><span>POD:</span> "+fromdb(obj.name)+"</p>"
			+ "<p><span>Private CIDR:</span> "+obj.cidr+"</p>"
			+ "<p><span>Private IP Range:</span> "+obj.ipRange+"</p>"
			+ "<p><span>Gateway:</span> "+obj.gateway+"</p>";
							
		rightContent.data("id", obj.id).html(rightContentHtml);
		
		$("#submenu_content_zones").find("#action_edit_zone, #action_add_pod, #action_add_publicip_vlan").hide();
		var buttons = $("#submenu_content_zones").find("#action_delete, #action_edit_pod").data("type", "pod").show();
		buttons.data("id", obj.id);		
		buttons.data("zoneid", obj.zoneid);
		buttons.data("name", obj.name);
		buttons.data("cidr", obj.cidr);
		buttons.data("startip", obj.startip);	
		buttons.data("endip", obj.endip);	
		buttons.data("ipRange", obj.ipRange);		
		buttons.data("gateway", obj.gateway);	
		if (getDirectAttachUntaggedEnabled() == "true") {
			$("#submenu_content_zones #action_add_directip_vlan").data("type", "pod").data("id", obj.id).data("name", obj.name).data("zoneid", obj.zoneid).show();
		}
	}
	
	$("#submenu_content_zones #action_delete").bind("click", function(event) {
		var deleteButton = $(this);
	
		var confirmMessage = null;
		var id = deleteButton.data("id");
		var type = deleteButton.data("type");
		var command = null;
		if (type == "zone") {
			confirmMessage = "Please confirm you want to delete the zone : <b>" + deleteButton.data("name") +"</b>";
			command = "deleteZone";
		} else if (type == "pod") {
			confirmMessage = "Please confirm you want to delete the pod : <b>" + deleteButton.data("name") + "</b>";
			command = "deletePod"
		} else {
			confirmMessage = "Please confirm you want to delete the public vlan IP range : <b>" + deleteButton.data("name") + "</b>";
			command = "deleteVlanIpRange";
		}
	
		$("#dialog_confirmation")
		.html(confirmMessage)
		.dialog('option', 'buttons', { 				
			"Confirm": function() { 
				$(this).dialog("close"); 
				
				$.ajax({
					data: "command="+command+"&id="+id+"&response=json",
					dataType: "json",
					success: function(json) {
						var target = null;
						if (type == "zone") {
							target = $("#submenu_content_zones #zone_" + id);
						} else if (type == "pod") {
							target = $("#submenu_content_zones #pod_" + id);
						} else {
							target = $("#submenu_content_zones #publicip_range_" + id);
						}
						target.fadeOut("slow", function() {
							$(this).remove();
						});
						rightPanel.empty();
						rightContent.empty();
						$("#submenu_content_zones #action_delete").hide();
					}
				});
				
			}, 
			"Cancel": function() { 
				$(this).dialog("close"); 
			} 
		}).dialog("open");
	});
	
	$("#submenu_content_zones #action_edit_zone").bind("click", function(event) {            
		var id = $(this).data("id");
		
		var dialogEditZone = $("#dialog_edit_zone");			
		dialogEditZone.find("#edit_zone_name").val($(this).data("name"));
		dialogEditZone.find("#edit_zone_dns1").val($(this).data("dns1"));
		dialogEditZone.find("#edit_zone_dns2").val($(this).data("dns2"));
		dialogEditZone.find("#edit_zone_internaldns1").val($(this).data("internaldns1"));
		dialogEditZone.find("#edit_zone_internaldns2").val($(this).data("internaldns2"));
		dialogEditZone.find("#edit_zone_guestcidraddress").val($(this).data("guestcidraddress"));
		var guestcidraddress = $(this).data("guestcidraddress");
		
		// If the network type is vnet, don't show any vlan stuff.
		if (getNetworkType() != "vnet") {
			dialogEditZone.find("#edit_zone_startvlan").val("");
			dialogEditZone.find("#edit_zone_endvlan").val("");
			var vlan = $(this).data("vlan");
			if(vlan != null) {
				if(vlan.indexOf("-")!==-1) {
					var startVlan = vlan.substring(0, vlan.indexOf("-"));
					var endVlan = vlan.substring((vlan.indexOf("-")+1));		    
					dialogEditZone.find("#edit_zone_startvlan").val(startVlan);
					dialogEditZone.find("#edit_zone_endvlan").val(endVlan);
				}
				else {
					dialogEditZone.find("#edit_zone_startvlan").val(vlan);			        
				}
			}
		}
		
		dialogEditZone
		.dialog('option', 'buttons', { 				
			"Change": function() { 		
			    var thisDialog = $(this);
			 			
				// validate values
				var isValid = true;					
				isValid &= validateString("Name", thisDialog.find("#edit_zone_name"), thisDialog.find("#edit_zone_name_errormsg"));
				isValid &= validateIp("DNS 1", thisDialog.find("#edit_zone_dns1"), thisDialog.find("#edit_zone_dns1_errormsg"), false);	//required
				isValid &= validateIp("DNS 2", thisDialog.find("#edit_zone_dns2"), thisDialog.find("#edit_zone_dns2_errormsg"), true);	//optional	
				isValid &= validateIp("Internal DNS 1", thisDialog.find("#edit_zone_internaldns1"), thisDialog.find("#edit_zone_internaldns1_errormsg"), false);	//required
				isValid &= validateIp("Internal DNS 2", thisDialog.find("#edit_zone_internaldns2"), thisDialog.find("#edit_zone_internaldns2_errormsg"), true);	//optional						
				if (getNetworkType() != "vnet") {
					isValid &= validateString("Zone - Start VLAN Range", thisDialog.find("#edit_zone_startvlan"), thisDialog.find("#edit_zone_startvlan_errormsg"), false); //required
					isValid &= validateString("Zone - End VLAN Range", thisDialog.find("#edit_zone_endvlan"), thisDialog.find("#edit_zone_endvlan_errormsg"), true);  //optional
				}
				isValid &= validateCIDR("Guest CIDR", thisDialog.find("#edit_zone_guestcidraddress"), thisDialog.find("#edit_zone_guestcidraddress_errormsg"), false);	//required					
				if (!isValid) return;							
				
				var moreCriteria = [];	
				
				var name = trim(thisDialog.find("#edit_zone_name").val());
				moreCriteria.push("&name="+encodeURIComponent(name));
				
				var dns1 = trim(thisDialog.find("#edit_zone_dns1").val());
				moreCriteria.push("&dns1="+encodeURIComponent(dns1));
				
				var dns2 = trim(thisDialog.find("#edit_zone_dns2").val());
				if (dns2 != null & dns2.length > 0) 
					moreCriteria.push("&dns2="+encodeURIComponent(dns2));	
				
				var internaldns1 = trim(thisDialog.find("#edit_zone_internaldns1").val());
				moreCriteria.push("&internaldns1="+encodeURIComponent(internaldns1));
				
				var internaldns2 = trim(thisDialog.find("#edit_zone_internaldns2").val());	
				if (internaldns2 != null & internaldns2.length > 0) 
					moreCriteria.push("&internaldns2="+encodeURIComponent(internaldns2));						
				
				var vlan;				
				if (getNetworkType() != "vnet") {
					var vlanStart = trim(thisDialog.find("#edit_zone_startvlan").val());	
					var vlanEnd = trim(thisDialog.find("#edit_zone_endvlan").val());						
					if (vlanEnd != null && vlanEnd.length > 0) 
					    vlan = vlanStart + "-" + vlanEnd;						    							
					else 	
					    vlan = vlanStart;							
			       moreCriteria.push("&vlan=" + encodeURIComponent(vlan));	
				}				
				
				var guestcidraddress = trim(thisDialog.find("#edit_zone_guestcidraddress").val());
				moreCriteria.push("&guestcidraddress="+encodeURIComponent(guestcidraddress));				    		 
				
				thisDialog.dialog("close"); 
				
				var template = $("#zone_"+id); 
				var loadingImg = template.find(".adding_loading").find(".adding_text").text("Updating zone....");										
				var row_container = template.find("#row_container");									            
	            loadingImg.show();  
                row_container.hide();             
				
				$.ajax({
					data: "command=updateZone&id="+id+moreCriteria.join("")+"&response=json",
					dataType: "json",
					success: function(json) {	
					    var obj = {"id": id, "name": name, "dns1": dns1, "dns2": dns2, "internaldns1": internaldns1, "internaldns2": internaldns2, "vlan": vlan, "guestcidraddress": guestcidraddress };
				        zoneObjectToRightPanel(obj);						
						var zoneName = $("#zone_"+id).find("#zone_name").text(name);		
						zoneName.data("id", id).data("name", fromdb(name)).data("dns1", dns1).data("internaldns1", internaldns1).data("guestcidraddress", guestcidraddress);							
						if (dns2 != "") 
							zoneName.data("dns2", dns2);
						if (internaldns2 != "") 
							zoneName.data("internaldns2", internaldns2);
						if (vlan != "") 
							zoneName.data("vlan", vlan);
							
						loadingImg.hide(); 								                            
                        row_container.show();      
					},
				    error: function(XMLHttpResponse) {
				        handleError(XMLHttpResponse);
						loadingImg.hide(); 								                            
                        row_container.show();
				    }
				});
			}, 
			"Cancel": function() { 
				$(this).dialog("close"); 
			} 
		}).dialog("open");			
		
	});
	
	$("#submenu_content_zones #action_add_pod").bind("click", function(event) {
		var id = $(this).data("id");
		
		$("#dialog_add_pod").find("#add_pod_zone_name").text($(this).data("name"));
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
				if (!isValid) return;			

                var name = trim(thisDialog.find("#add_pod_name").val());
				var cidr = trim(thisDialog.find("#add_pod_cidr").val());
				var startip = trim(thisDialog.find("#add_pod_startip").val());
				var endip = trim(thisDialog.find("#add_pod_endip").val());	    //optional
				var gateway = trim(thisDialog.find("#add_pod_gateway").val());			

                var array1 = [];
                array1.push("&zoneId="+id);
                array1.push("&name="+encodeURIComponent(name));
                array1.push("&cidr="+encodeURIComponent(cidr));
                array1.push("&startIp="+encodeURIComponent(startip));
                if (endip != null && endip.length > 0)
                    array1.push("&endIp="+encodeURIComponent(endip));
                array1.push("&gateway="+encodeURIComponent(gateway));			
				
				thisDialog.dialog("close"); 
				
				var template = $("#pod_template").clone(true);
				var loadingImg = template.find(".adding_loading");										
				var row_container = template.find("#row_container");
				
				$("#zone_"+id+" #zone_content").show();	
				$("#zone_" + id + " #pods_container").prepend(template.show());						
				$("#zone_" + id + " #zone_expand").removeClass().addClass("zonetree_openarrows");									            
	            loadingImg.show();  
                row_container.hide();             
		        template.fadeIn("slow");
				
				$.ajax({
					data: "command=createPod&response=json"+array1.join(""),
					dataType: "json",
					success: function(json) {
						var pod = json.createpodresponse;
						template.attr("id", "pod_"+pod.id);
						podJSONToTemplate(pod, template);
						loadingImg.hide(); 								                            
                        row_container.show();
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
	});
	
	$("#submenu_content_zones #action_add_directip_vlan").bind("click", function(event) {  
	    var thisLink = $(this);		    
	    var podid = thisLink.data("id");
	    var podname = thisLink.data("name");
	    var zoneid = thisLink.data("zoneid");
	    		 
	    $("#dialog_add_vlan_for_pod").find("#pod_name_label").text(podname);
	    
	    $("#dialog_add_vlan_for_pod")
	    .dialog('option', 'buttons', {
	        "Add": function() {             
	            var thisDialog = $(this);		
			    					
				// validate values
				var isValid = true;						
				isValid &= validateIp("Gateway", thisDialog.find("#gateway"), thisDialog.find("#gateway_errormsg"));
				isValid &= validateIp("Netmask", thisDialog.find("#netmask"), thisDialog.find("#netmask_errormsg"));
				isValid &= validateIp("Start IP Range", thisDialog.find("#startip"), thisDialog.find("#startip_errormsg"));   //required
				isValid &= validateIp("End IP Range", thisDialog.find("#endip"), thisDialog.find("#endip_errormsg"), true);  //optional
				if (!isValid) return;							
												
				var gateway = trim(thisDialog.find("#gateway").val());
				var netmask = trim(thisDialog.find("#netmask").val());
				var startip = trim(thisDialog.find("#startip").val());
				var endip = trim(thisDialog.find("#endip").val());		
				
				var array1 = [];
				array1.push("&vlan=untagged");	
				array1.push("&zoneid=" + zoneid);
				array1.push("&podId=" + podid);	
				array1.push("&forVirtualNetwork=false"); //direct VLAN	
				array1.push("&gateway="+encodeURIComponent(gateway));
				array1.push("&netmask="+encodeURIComponent(netmask));	
				array1.push("&startip="+encodeURIComponent(startip));
				if(endip != null && endip.length > 0)
				    array1.push("&endip="+encodeURIComponent(endip));
									
				thisDialog.dialog("close"); 
									
				var template = $("#vlan_ip_range_template").clone(true);					
				//direct untagged VLAN is under pod(2nd level). So, make direct untagged VLAN 3rd level.
				template.find("#row_container .zonetree_secondlevel").removeClass().addClass("zonetree_thirdlevel");
				
				var loadingImg = template.find(".adding_loading");	
				loadingImg.find(".adding_text").text("Adding a direct IP range....");										
				var row_container = template.find("#row_container");
													
				$("#zone_" + zoneid).find("#pod_" + podid).find("#directip_ranges_container").prepend(template.show());						
																            
	            loadingImg.show();  
                row_container.hide();             
		        template.fadeIn("slow");					
				
				$.ajax({
					data: "command=createVlanIpRange&response=json" + array1.join(""),
					dataType: "json",
					success: function(json) {						    
						var vlan = json.createvlaniprangeresponse;
						template.attr("id", "publicip_range_"+vlan.id);
						vlanIpRangeJSONToTemplate(vlan, template);
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
	            $(this).dialog("close");
	        }		    
	    }).dialog("open");
	    
	    return false;
	});
	
	$("#submenu_content_zones #action_edit_pod").bind("click", function(event) {             
		var id = $(this).data("id");		
		var zoneid = $(this).data("zoneid");
		var dialogEditPod = $("#dialog_edit_pod");	
					
		var oldName = $(this).data("name");				
		dialogEditPod.find("#edit_pod_name").val(oldName);
		
		var oldCidr = $(this).data("cidr");
		dialogEditPod.find("#edit_pod_cidr").val(oldCidr);	
		
		var oldStartip = $(this).data("startip");							
		dialogEditPod.find("#edit_pod_startip").val(oldStartip); 
		
		var oldEndip = $(this).data("endip");
		dialogEditPod.find("#edit_pod_endip").val(oldEndip);  
		
		var oldGateway = $(this).data("gateway"); 
		dialogEditPod.find("#edit_pod_gateway").val(oldGateway);
					
		dialogEditPod
		.dialog('option', 'buttons', { 				
			"Change": function() { 	
			    var thisDialog = $(this);
							    				
			    // validate values
				var isValid = true;					
				isValid &= validateString("Name", thisDialog.find("#edit_pod_name"), thisDialog.find("#edit_pod_name_errormsg"));
				isValid &= validateCIDR("CIDR", thisDialog.find("#edit_pod_cidr"), thisDialog.find("#edit_pod_cidr_errormsg"));	
				isValid &= validateIp("Start IP Range", thisDialog.find("#edit_pod_startip"), dialogEditPod.find("#edit_pod_startip_errormsg"));  //required
				isValid &= validateIp("End IP Range", dialogEditPod.find("#edit_pod_endip"), thisDialog.find("#edit_pod_endip_errormsg"), true);  //optional
				isValid &= validateIp("Gateway", thisDialog.find("#edit_pod_gateway"), thisDialog.find("#edit_pod_gateway_errormsg"), true);  //optional when editing
				if (!isValid) return;			
              
                var newName = trim(thisDialog.find("#edit_pod_name").val());
				var newCidr = trim(thisDialog.find("#edit_pod_cidr").val());
				var newStartip = trim(thisDialog.find("#edit_pod_startip").val());
				var newEndip = trim(thisDialog.find("#edit_pod_endip").val());	
				var newIpRange = getIpRange(newStartip, newEndip);	
				var newGateway = trim(thisDialog.find("#edit_pod_gateway").val());				
                    
                var array1 = [];	
                array1.push("&id="+id);
                if(newName != oldName)
                    array1.push("&name="+encodeURIComponent(newName));
                if(newCidr != oldCidr)
                    array1.push("&cidr="+encodeURIComponent(newCidr));
                if(newStartip != oldStartip)
                    array1.push("&startIp="+encodeURIComponent(newStartip));    
                if(newEndip != oldEndip && newEndip != null && newEndip.length > 0) { 
                    if(newStartip == oldStartip) {
                        array1.push("&startIp="+encodeURIComponent(newStartip));  //startIp needs to be passed to updatePod API when endIp is passed to updatePod API.
                    }
					array1.push("&endIp="+encodeURIComponent(newEndip));	
			    }
				if(newGateway != oldGateway && newGateway != null && newGateway.length > 0)				             
				    array1.push("&gateway="+encodeURIComponent(newGateway)); 
				
				$(this).dialog("close"); 
				
				var template = $("#pod_"+id); 
				var loadingImg = template.find(".adding_loading");	
				loadingImg.find(".adding_text").text("Updating a pod....");									
				var row_container = template.find("#row_container");
												            
	            loadingImg.show();  
                row_container.hide();             
		        template.fadeIn("slow");
				
				$.ajax({
					data: "command=updatePod&response=json"+array1.join(""),
					dataType: "json",
					success: function(json) {						   				    
					    var newIpRange = getIpRange(newStartip, newEndip);											   
						var obj = {"id": id, "zoneid": zoneid, "name": newName, "cidr": newCidr, "startip": newStartip, "endip": newEndip, "ipRange": newIpRange, "gateway": newGateway};  
				        podObjectToRightPanel(obj);					
						var podName = $("#pod_"+id).find("#pod_name").text(newName);
						podName.data("id", id).data("name", fromdb(newName)).data("cidr", newCidr).data("startip", newStartip).data("endip", newEndip).data("ipRange", newIpRange).data("gateway", newGateway);	
						loadingImg.hide(); 								                            
                        row_container.show();							
					},
				    error: function(XMLHttpResponse) {	
				        loadingImg.hide();  
                        row_container.show();   
				        handleError(XMLHttpResponse);							    
				    }
				});	
			}, 
			"Cancel": function() { 
				$(this).dialog("close"); 
			}
		}).dialog("open");			
	});
			
	$("#submenu_content_zones #action_add_publicip_vlan").bind("click", function(event) {			
		var id = $(this).data("id");
		
		// reset dialog
		dialogAddVlanForZone.find("#add_publicip_vlan_vlan_container, #add_publicip_vlan_domain_container, #add_publicip_vlan_account_container").hide();
		dialogAddVlanForZone.find("#add_publicip_vlan_tagged, #add_publicip_vlan_vlan, #add_publicip_vlan_gateway, #add_publicip_vlan_netmask, #add_publicip_vlan_startip, #add_publicip_vlan_endip, #add_publicip_vlan_account").val("");
		dialogAddVlanForZone.find("#add_publicip_vlan_zone_name").text($(this).data("name"));
				
		if (getNetworkType() == 'vnet') {
			$("#add_publicip_vlan_type_container").hide();
		} else {	
			dialogAddVlanForZone.find("#add_publicip_vlan_pod_container").show();	
			dialogAddVlanForZone.find("#add_publicip_vlan_type").change();
			$("#add_publicip_vlan_type_container").show();
			var podSelect = dialogAddVlanForZone.find("#add_publicip_vlan_pod").empty();		
			$.ajax({
				data: "command=listPods&zoneId="+id+"&response=json",
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
				data: "command=listDomains&response=json",
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
			    var thisDialog = $(this);							
				// validate values
				var isValid = true;					
				var isTagged = false;
				var isDirect = false;
				if (getNetworkType() == "vlan") {
					isDirect = thisDialog.find("#add_publicip_vlan_type").val() == "false";
					isTagged = thisDialog.find("#add_publicip_vlan_tagged").val() == "tagged";
				}
				
				isValid &= validateString("Account", thisDialog.find("#add_publicip_vlan_account"), thisDialog.find("#add_publicip_vlan_account_errormsg"), true); //optional
				
				if (isTagged) {
					isValid &= validateNumber("VLAN", thisDialog.find("#add_publicip_vlan_vlan"), thisDialog.find("#add_publicip_vlan_vlan_errormsg"), 2, 4095);
				}
				isValid &= validateIp("Gateway", thisDialog.find("#add_publicip_vlan_gateway"), thisDialog.find("#add_publicip_vlan_gateway_errormsg"));
				isValid &= validateIp("Netmask", thisDialog.find("#add_publicip_vlan_netmask"), thisDialog.find("#add_publicip_vlan_netmask_errormsg"));
				isValid &= validateIp("Start IP Range", thisDialog.find("#add_publicip_vlan_startip"), thisDialog.find("#add_publicip_vlan_startip_errormsg"));   //required
				isValid &= validateIp("End IP Range", thisDialog.find("#add_publicip_vlan_endip"), thisDialog.find("#add_publicip_vlan_endip_errormsg"), true);  //optional
				if (!isValid) return;							
				
				var vlan = trim(thisDialog.find("#add_publicip_vlan_vlan").val());
				if (isTagged) {
					vlan = "&vlan="+vlan;
				} else {
					vlan = "&vlan=untagged";
				}
				
				var scopeParams = "";
				if(dialogAddVlanForZone.find("#add_publicip_vlan_scope").val()=="account-specific")
				    scopeParams = "&domainId="+trim(thisDialog.find("#add_publicip_vlan_domain").val())+"&account="+trim(thisDialog.find("#add_publicip_vlan_account").val());    
				
				var type = "true";
				if (getNetworkType() == "vlan") type = trim(thisDialog.find("#add_publicip_vlan_type").val());
				var gateway = trim(thisDialog.find("#add_publicip_vlan_gateway").val());
				var netmask = trim(thisDialog.find("#add_publicip_vlan_netmask").val());
				var startip = trim(thisDialog.find("#add_publicip_vlan_startip").val());
				var endip = trim(thisDialog.find("#add_publicip_vlan_endip").val());					
				
				thisDialog.dialog("close"); 
									
				var template = $("#vlan_ip_range_template").clone(true);
				var loadingImg = template.find(".adding_loading");										
				var row_container = template.find("#row_container");
				
				$("#zone_" + id + " #zone_content").show();	
				$("#zone_" + id + " #publicip_ranges_container").prepend(template.show());						
				$("#zone_" + id + " #zone_expand").removeClass().addClass("zonetree_openarrows");									            
	            loadingImg.show();  
                row_container.hide();             
		        template.fadeIn("slow");					
				
				$.ajax({
					data: "command=createVlanIpRange&forVirtualNetwork="+type+"&zoneId="+id+vlan+scopeParams+"&gateway="+encodeURIComponent(gateway)+"&netmask="+encodeURIComponent(netmask)+"&startip="+encodeURIComponent(startip)+"&endip="+encodeURIComponent(endip)+"&response=json",
					dataType: "json",
					success: function(json) {
						var vlan = json.createvlaniprangeresponse;
						template.attr("id", "publicip_range_"+vlan.id);
						vlanIpRangeJSONToTemplate(vlan, template);
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
				$(this).dialog("close"); 
			} 
		}).dialog("open");
	});
	
	$("#zone_template").bind("click", function(event) {
		var template = $(this);
		var target = $(event.target);
		var action = target.attr("id");
		var id = template.data("id");
		var name = template.data("name");
		
		switch (action) {
			case "zone_expand" :
				if (target.hasClass("zonetree_closedarrows")) {
					$("#zone_"+id+" #zone_content").show();
					target.removeClass().addClass("zonetree_openarrows");
				} else {
					$("#zone_"+id+" #zone_content").hide();
					target.removeClass().addClass("zonetree_closedarrows");
				}
				break;
			case "zone_name" :
				$("#submenu_content_zones .zonetree_firstlevel_selected").removeClass().addClass("zonetree_firstlevel");
				$("#submenu_content_zones .zonetree_secondlevel_selected").removeClass().addClass("zonetree_secondlevel");
				template.find(".zonetree_firstlevel").removeClass().addClass("zonetree_firstlevel_selected");
									
				var obj = {"id": target.data("id"), "name": target.data("name"), "dns1": target.data("dns1"), "dns2": target.data("dns2"), "internaldns1": target.data("internaldns1"), "internaldns2": target.data("internaldns2"), "vlan": target.data("vlan"), "guestcidraddress": target.data("guestcidraddress")};
				zoneObjectToRightPanel(obj);					
				
				break;
			case "pod_name" :
				$("#submenu_content_zones .zonetree_firstlevel_selected").removeClass().addClass("zonetree_firstlevel");
				$("#submenu_content_zones .zonetree_secondlevel_selected").removeClass().addClass("zonetree_secondlevel");
				target.parent(".zonetree_secondlevel").removeClass().addClass("zonetree_secondlevel_selected");
									
				var obj = {"id": target.data("id"), "zoneid": target.data("zoneid"), "name": target.data("name"), "cidr": target.data("cidr"), "startip": target.data("startip"), "endip": target.data("endip"), "ipRange": target.data("ipRange"), "gateway": target.data("gateway")};
				podObjectToRightPanel(obj);
				
				break;
			case "vlan_ip_range_name" :
				$("#submenu_content_zones .zonetree_firstlevel_selected").removeClass().addClass("zonetree_firstlevel");
				$("#submenu_content_zones .zonetree_secondlevel_selected").removeClass().addClass("zonetree_secondlevel");
				target.parent(".zonetree_secondlevel").removeClass().addClass("zonetree_secondlevel_selected");
				
				var title = "<strong>Public VLAN IP Range</strong>";
				var isDirect = target.data("forVirtualNetwork") == "false";
				var isTagged = target.data("vlan") != "untagged";
				if (isDirect) {
					title = "<strong>Direct VLAN IP Range</strong>";
				}				
				rightPanel.html(title);
				
				var rightContentHtml = 
					"<p><span>VLAN ID:</span> "+target.data("vlan")+"</p>"				
					+ "<p><span>Gateway:</span> "+target.data("gateway")+"</p>"
					+ "<p><span>Netmask:</span> "+target.data("netmask")+"</p>"
					+ "<p><span>IP Range:</span> "+target.data("name")+"</p>";
				if(target.data("domainId")!=null) 
					rightContentHtml += "<p><span>Domain ID:</span> "+target.data("domainId")+"</p>";	
				if(target.data("domain")!=null) 
					rightContentHtml += "<p><span>Domain:</span> "+target.data("domain")+"</p>";	
		        if(target.data("account")!=null) 
					rightContentHtml += "<p><span>Account:</span> "+target.data("account")+"</p>";	
			    if(target.data("podname")!=null) 
					rightContentHtml += "<p><span>Pod:</span> "+target.data("podname")+"</p>";						
				rightContent.data("id", target.data("id")).html(rightContentHtml);
				
				$("#submenu_content_zones").find("#action_edit_zone, #action_add_pod, #action_edit_pod, #action_add_publicip_vlan, #action_add_directip_vlan").hide();
				$("#submenu_content_zones #action_delete").data("id", target.data("id")).data("name", target.data("name")).data("type", "publicip_range").show();
				
				break;
			default:
				break;
		}
		return false;
	});
	
	function vlanIpRangeJSONToTemplate(json, template) {
		template.data("id", json.id);
		var vlanName = json.id;
		var vlanDisplayName = vlanName;
		if (json.description != undefined) {
			if (json.description.indexOf("-") == -1) {
				vlanName = json.description;
				vlanDisplayName = vlanName;
			} else {
				var ranges = json.description.split("-");
				vlanName = ranges[0] + " -" + ranges[1];
				vlanDisplayName = ranges[0] + " - " + ranges[1];
			}
		}
		var isDirect = json.forvirtualnetwork == "false";
		var isTagged = json.vlan != "untagged";
		if (isDirect) {
			template.find(".zonetree_ipicon").removeClass().addClass("zonetree_directipicon");
			template.find("#vlan_ip_range_type").text("Direct IP Range:");
		}
		else {
		    template.find("#vlan_ip_range_type").text("Public IP Range:");
		}
		template.find("#vlan_ip_range_name")
			.html(vlanName)
			.data("id", json.id)
			.data("name", vlanDisplayName)
			.data("vlan", json.vlan)
			.data("forVirtualNetwork", json.forvirtualnetwork)
			.data("gateway", json.gateway)
			.data("netmask", json.netmask);
			
		template.find("#vlan_ip_range_name").data("domainId", json.domainid); //json.domainid might be null.
		template.find("#vlan_ip_range_name").data("domain", json.domain);     //json.domain might be null.
		template.find("#vlan_ip_range_name").data("account", json.account);   //json.account might be null.	
		template.find("#vlan_ip_range_name").data("podname", json.podname);   //json.podname might be null.			
	}
	
	function getIpRange(startip, endip) {
	    var ipRange = "";
		if (startip != null && startip.length > 0) {
			ipRange = startip;
		}
		if (endip != null && endip.length > 0) {
			ipRange = ipRange + "-" + endip;
		}		
		return ipRange;
	}
	
	function podJSONToTemplate(json, template) {		    
		var ipRange = getIpRange(json.startip, json.endip);			
		template.data("id", json.id).data("name", json.name);
		
		var podName = template.find("#pod_name").text(json.name);
		podName.data("id", json.id);
		podName.data("zoneid", json.zoneid);
		podName.data("name", json.name);
		podName.data("cidr", json.cidr);
		podName.data("startip", json.startip);
		podName.data("endip", json.endip);
		podName.data("ipRange", ipRange);		
		podName.data("gateway", json.gateway);
							
		$.ajax({
			data: "command=listVlanIpRanges&zoneid="+json.zoneid+"&podid="+json.id+"&response=json",
			dataType: "json",
			success: function(json) {				    
				var ranges = json.listvlaniprangesresponse.vlaniprange;
				var grid = template.find("#directip_ranges_container").empty();
				if (ranges != null && ranges.length > 0) {					    
					for (var i = 0; i < ranges.length; i++) {	
					    if(ranges[i].forvirtualnetwork == "false" && ranges[i].vlan == "untagged") { //direct untagged VLAN should be under pod, instead of under zone.						    		    
						    var rangeTemplate = $("#vlan_ip_range_template").clone(true).attr("id", "publicip_range_"+ranges[i].id);
						    vlanIpRangeJSONToTemplate(ranges[i], rangeTemplate);
						    grid.append(rangeTemplate.show());
						    							    					    
						    //direct untagged VLAN is under pod(2nd level). So, make direct untagged VLAN 3rd level.
						    rangeTemplate.find("#row_container .zonetree_secondlevel").removeClass().addClass("zonetree_thirdlevel");
						}
					}
				}
			}
		});						
	}
	
	function zoneJSONToTemplate(json, template) {
	    var zoneid = json.id;
		template.data("id", zoneid).data("name", fromdb(json.name));
		template.find("#zone_name")
			.text(json.name)
			.data("id", zoneid)
			.data("name", fromdb(json.name))
			.data("dns1", json.dns1)
			.data("internaldns1", json.internaldns1)
			.data("guestcidraddress", json.guestcidraddress);
			
		if (json.dns2 != undefined) {
			template.find("#zone_name").data("dns2", json.dns2);
		}
		if (json.internaldns2 != undefined) {
			template.find("#zone_name").data("internaldns2", json.internaldns2);
		}
		if (json.vlan != undefined) {
			template.find("#zone_name").data("vlan", json.vlan);
		}	
		
		$.ajax({
			data: "command=listPods&zoneid="+zoneid+"&response=json",
			dataType: "json",
			success: function(json) {
				var pods = json.listpodsresponse.pod;
				var grid = template.find("#pods_container").empty();
				if (pods != null && pods.length > 0) {					    
					for (var i = 0; i < pods.length; i++) {
						var podTemplate = $("#pod_template").clone(true).attr("id", "pod_"+pods[i].id);
						podJSONToTemplate(pods[i], podTemplate);
						grid.append(podTemplate.show());
						forceLogout = false;
					}
				}
			}
		});
		
		$.ajax({
			data: "command=listVlanIpRanges&zoneId="+zoneid+"&response=json",
			dataType: "json",
			success: function(json) {
				var ranges = json.listvlaniprangesresponse.vlaniprange;
				var grid = template.find("#publicip_ranges_container").empty();
				if (ranges != null && ranges.length > 0) {					    
					for (var i = 0; i < ranges.length; i++) {						    
					    if(ranges[i].forvirtualnetwork == "false" && ranges[i].vlan == "untagged") //direct untagged VLAN should be under pod, instead of under zone.
					        continue;
						var rangeTemplate = $("#vlan_ip_range_template").clone(true).attr("id", "publicip_range_"+ranges[i].id);
						vlanIpRangeJSONToTemplate(ranges[i], rangeTemplate);
						grid.append(rangeTemplate.show());
					}
				}
			}
		});
		
	}
	// If the network type is vnet, don't show any vlan stuff.
	if (getNetworkType() == "vnet") {
		$("#action_add_publicip_vlan").removeClass().addClass("zonedetails_addpublicipbutton");
		$("#dialog_add_vlan_for_zone").attr("title", "Add Public IP Range");
		$("#dialog_edit_zone #edit_zone_container, #dialog_add_zone #add_zone_container").hide();
	}
	activateDialog($("#dialog_add_zone").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));
	
	activateDialog($("#dialog_edit_zone").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));
	
	activateDialog($("#dialog_add_pod").dialog({ 
		autoOpen: false,
		modal: true,
		width:320,
		zIndex: 2000
	}));
	
	activateDialog($("#dialog_edit_pod").dialog({ 
		autoOpen: false,
		width: 320,
		modal: true,
		zIndex: 2000
	}));
	
	activateDialog($("#dialog_add_vlan_for_zone").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));
		
	activateDialog($("#dialog_add_vlan_for_pod").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));	
					
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
			
	$("#action_add_zone").bind("click", function(event) {		    
	    var thisDialog = $(this);
		thisDialog.find("#add_zone_name, #add_zone_dns1, #add_zone_dns2, #add_zone_internaldns1, #add_zone_internaldns2, #add_zone_startvlan, #add_zone_endvlan, #add_zone_guestcidraddress").val("");
		
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
				if (!isValid) return;							
				
				var moreCriteria = [];	
				
				var name = trim(thisDialog.find("#add_zone_name").val());
				moreCriteria.push("&name="+encodeURIComponent(name));
				
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
				
				thisDialog.dialog("close"); 
				
				var template = $("#zone_template").clone(true);
				var loadingImg = template.find(".adding_loading");										
				var row_container = template.find("#row_container");
				
				$("#submenu_content_zones #zones_container").prepend(template.show());					            
	            loadingImg.show();  
                row_container.hide();             
		        template.fadeIn("slow");				        		
				
				$.ajax({
					data: "command=createZone"+moreCriteria.join("")+"&response=json",
					dataType: "json",
					success: function(json) {
						var zone = json.createzoneresponse;
						template.attr("id", "zone_"+zone.id);
						zoneJSONToTemplate(zone, template);							
						loadingImg.hide(); 								                            
                        row_container.show();      
					},
				    error: function(XMLHttpResponse) {
				        handleError(XMLHttpResponse);				    
					    template.slideUp("slow", function() {
							thisDialog.remove();
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
	});
	
	$("#submenu_zones").bind("click", function(event) {
		event.preventDefault();
		$(this).toggleClass("submenu_links_on").toggleClass("submenu_links_off");
		currentSubMenu.toggleClass("submenu_links_off").toggleClass("submenu_links_on");
		currentSubMenu = $(this);
		var container = $("#submenu_content_zones").show();
		$("#submenu_content_global, #submenu_content_service, #submenu_content_disk").hide();
		clearRightPanel();
		$.ajax({
			data: "command=listZones&available=true&response=json",
			dataType: "json",
			success: function(json) {
				var zones = json.listzonesresponse.zone;
				var grid = $("#submenu_content_zones #zones_container").empty();
				if (zones != null && zones.length > 0) {					    
					for (var i = 0; i < zones.length; i++) {
						var template = $("#zone_template").clone(true).attr("id", "zone_"+zones[i].id);
						zoneJSONToTemplate(zones[i], template);
						grid.append(template.show());
					}
				}
			}
		});
	});
	
	$("#service_template").bind("click", function(event) {
		var template = $(this);
		var link = $(event.target);
		var linkAction = link.attr("id");
		var svcId = template.data("svcId");
		var svcName = template.data("svcName");
		var submenuContent = $("#submenu_content_service");
		switch (linkAction) {
			case "service_action_edit" :
				var dialogEditService = $("#dialog_edit_service");
				
				dialogEditService.find("#service_name").text(svcName);
				dialogEditService.find("#edit_service_name").val(svcName);
				dialogEditService.find("#edit_service_display").val(template.find("#service_display").text());
				dialogEditService.find("#edit_service_offerha").val(toBooleanValue(template.find("#service_offerha").text()));					
				
				dialogEditService
				.dialog('option', 'buttons', { 						
					"Confirm": function() { 
					    var thisDialog = $(this);	
												
						// validate values
				        var isValid = true;					
				        isValid &= validateString("Name", thisDialog.find("#edit_service_name"), thisDialog.find("#edit_service_name_errormsg"));
				        isValid &= validateString("Display Text", thisDialog.find("#edit_service_display"), thisDialog.find("#edit_service_display_errormsg"));											
				        if (!isValid) return;	
				
				        var moreCriteria = [];	
				        var name = trim(thisDialog.find("#edit_service_name").val());
				        moreCriteria.push("&name="+encodeURIComponent(escape(name)));						        
						var display = trim(thisDialog.find("#edit_service_display").val());
						moreCriteria.push("&displayText="+encodeURIComponent(escape(display)));								
						var offerha = trim(thisDialog.find("#edit_service_offerha").val());
						moreCriteria.push("&offerha="+offerha);								
										
						thisDialog.dialog("close");
						
						$.ajax({
							data: "command=updateServiceOffering&id="+svcId+moreCriteria.join("")+"&response=json",
							dataType: "json",
							success: function(json) {
								template.find("#service_display").text(display);
								template.find("#service_name").text(name);
								template.find("#service_offerha").text(toBooleanText(offerha));
								template.data("svcName", name);
							}
						});
					}, 
					"Cancel": function() { 
						$(this).dialog("close"); 
					} 
				}).dialog("open");
				break;
			case "service_action_delete" :
				$("#dialog_confirmation")
				.html("<p>Please confirm you want to remove the service offering: <b>"+svcName+"</b> from the management server. </p>")
				.dialog('option', 'buttons', { 						
					"Confirm": function() { 
						var dialogBox = $(this);
						$.ajax({
							data: "command=deleteServiceOffering&id="+svcId+"&response=json",
							dataType: "json",
							success: function(json) {
								dialogBox.dialog("close");
								template.slideUp("slow", function() {
									$(this).remove();																			
									changeGridRowsTotal(submenuContent.find("#grid_rows_total"), -1);
								});
							}
						});
					}, 
					"Cancel": function() { 
						$(this).dialog("close"); 
					} 
				}).dialog("open");
				break;
			default :
				break;
		}
		return false;
	});
			
	function serviceJSONToTemplate(json, template) {	
	    template.attr("id", "service_"+json.id);	   
		(index++ % 2 == 0)? template.addClass("smallrow_even"): template.addClass("smallrow_odd");	
		template.data("svcId", json.id).data("svcName", fromdb(json.name));
		
		template.find("#service_id").text(json.id);
		template.find("#service_name").text(fromdb(json.name));
		template.find("#service_display").text(fromdb(json.displaytext));
		template.find("#service_storagetype").text(json.storagetype);
		template.find("#service_cpu").text(json.cpunumber + " x " + convertHz(json.cpuspeed));
		template.find("#service_memory").text(convertBytes(parseInt(json.memory)*1024*1024));			
		template.find("#service_offerha").text(toBooleanText(json.offerha));
		template.find("#service_networktype").text((json.usevirtualnetwork=="true")? "Public":"Direct");
		template.find("#service_tags").text(fromdb(json.tags));
		
		setDateField(json.created, template.find("#service_created"));			
	}
	
	activateDialog($("#dialog_add_service").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));
	
	activateDialog($("#dialog_edit_service").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));
	
	activateDialog($("#dialog_edit_disk").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));
	
	$("#service_add_service").bind("click", function(event) {
		var dialogAddService = $("#dialog_add_service");
		
		dialogAddService.find("#add_service_name").val("");
		dialogAddService.find("#add_service_display").val("");
		dialogAddService.find("#add_service_cpucore").val("");
		dialogAddService.find("#add_service_cpu").val("");
		dialogAddService.find("#add_service_memory").val("");
		dialogAddService.find("#add_service_offerha").val("false");
			
		(g_hypervisorType == "kvm")? dialogAddService.find("#add_service_offerha_container").hide():dialogAddService.find("#add_service_offerha_container").show();            
		
		var submenuContent = $("#submenu_content_service");
		
		dialogAddService
		.dialog('option', 'buttons', { 				
			"Add": function() { 	
			    var thisDialog = $(this);
							
				// validate values
				var isValid = true;					
				isValid &= validateString("Name", thisDialog.find("#add_service_name"), thisDialog.find("#add_service_name_errormsg"));
				isValid &= validateString("Display Text", thisDialog.find("#add_service_display"), thisDialog.find("#add_service_display_errormsg"));
				isValid &= validateNumber("# of CPU Core", thisDialog.find("#add_service_cpucore"), thisDialog.find("#add_service_cpucore_errormsg"), 1, 1000);		
				isValid &= validateNumber("CPU", thisDialog.find("#add_service_cpu"), thisDialog.find("#add_service_cpu_errormsg"), 100, 100000);		
				isValid &= validateNumber("Memory", thisDialog.find("#add_service_memory"), thisDialog.find("#add_service_memory_errormsg"), 64, 1000000);	
				isValid &= validateString("Tags", thisDialog.find("#add_service_tags"), thisDialog.find("#add_service_tags_errormsg"), true);	//optional							
				if (!isValid) return;										
									
				var submenuContent = $("#submenu_content_service");                  
                var template = $("#service_template").clone(true);		
				var loadingImg = template.find(".adding_loading");		
                var rowContainer = template.find("#row_container");    	                               
                loadingImg.find(".adding_text").text("Adding....");	
                loadingImg.show();  
                rowContainer.hide();                                   
                submenuContent.find("#grid_content").prepend(template.fadeIn("slow"));    
									
				var array1 = [];						
				var name = trim(thisDialog.find("#add_service_name").val());
				array1.push("&name="+encodeURIComponent(escape(name)));	
				
				var display = trim(thisDialog.find("#add_service_display").val());
				array1.push("&displayText="+encodeURIComponent(escape(display)));	
				
				var storagetype = trim(thisDialog.find("#add_service_storagetype").val());
				array1.push("&storageType="+storagetype);	
				
				var core = trim(thisDialog.find("#add_service_cpucore").val());
				array1.push("&cpuNumber="+core);	
				
				var cpu = trim(thisDialog.find("#add_service_cpu").val());
				array1.push("&cpuSpeed="+cpu);	
				
				var memory = trim(thisDialog.find("#add_service_memory").val());
				array1.push("&memory="+memory);	
					
				var offerha = thisDialog.find("#add_service_offerha").val();	
				array1.push("&offerha="+offerha);								
									
				var networkType = thisDialog.find("#add_service_networktype").val();
				var useVirtualNetwork = (networkType=="direct")? false:true;
				array1.push("&usevirtualnetwork="+useVirtualNetwork);		
				
				var tags = trim(thisDialog.find("#add_service_tags").val());
				if(tags != null && tags.length > 0)
				    array1.push("&tags="+encodeURIComponent(escape(tags)));		
				
				thisDialog.dialog("close");
				$.ajax({
					data: "command=createServiceOffering"+array1.join("")+"&response=json",
					dataType: "json",
					success: function(json) {
						var offering = json.createserviceofferingresponse;							
						serviceJSONToTemplate(offering, template);							
						changeGridRowsTotal(submenuContent.find("#grid_rows_total"), 1); 
						loadingImg.hide();  
                        rowContainer.show();    
					},			
                    error: function(XMLHttpResponse) {		                   
	                    handleError(XMLHttpResponse);	
	                    template.slideUp("slow", function(){ $(this).remove(); } );							    
                    }							
				});
			}, 
			"Cancel": function() { 
				$(this).dialog("close"); 
			} 
		}).dialog("open");
		return false;
	});
	
	//add a new disk offering
	activateDialog($("#dialog_add_disk").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));
	
	$("#disk_add_disk").bind("click", function(event) {			   
		var dialogAddDisk = $("#dialog_add_disk");
		dialogAddDisk.find("#disk_name").val("");
		dialogAddDisk.find("#disk_description").val("");
		dialogAddDisk.find("#disk_disksize").val("");	
		var submenuContent = $("#submenu_content_disk");
				
		dialogAddDisk
		.dialog('option', 'buttons', { 				
			"Add": function() { 
			    var thisDialog = $(this);
								    		
				// validate values
				var isValid = true;					
				isValid &= validateString("Name", thisDialog.find("#add_disk_name"), thisDialog.find("#add_disk_name_errormsg"));
				isValid &= validateString("Description", thisDialog.find("#add_disk_description"), thisDialog.find("#add_disk_description_errormsg"));
				isValid &= validateNumber("Disk size", thisDialog.find("#add_disk_disksize"), thisDialog.find("#add_disk_disksize_errormsg"), 1, null); 
				isValid &= validateString("Tags", thisDialog.find("#add_disk_tags"), thisDialog.find("#add_disk_tags_errormsg"), true);	//optional	
				if (!isValid) return;		
				
				var submenuContent = $("#submenu_content_disk");                  
                var template = $("#disk_template").clone(true);		
				var loadingImg = template.find(".adding_loading");		
                var rowContainer = template.find("#row_container");    	                               
                loadingImg.find(".adding_text").text("Adding....");	
                loadingImg.show();  
                rowContainer.hide();                                   
                submenuContent.find("#grid_content").prepend(template.fadeIn("slow"));    		
					
				var array1 = [];					
				var name = trim(thisDialog.find("#add_disk_name").val());
				array1.push("&name="+encodeURIComponent(escape(name)));
				
				var description = trim(thisDialog.find("#add_disk_description").val());	
				array1.push("&displaytext="+encodeURIComponent(escape(description)));
							
				var disksize = trim(thisDialog.find("#add_disk_disksize").val());
				array1.push("&disksize="+disksize);
				
				var tags = trim(thisDialog.find("#add_disk_tags").val());
				if(tags != null && tags.length > 0)
				    array1.push("&tags="+encodeURIComponent(escape(tags)));		
						
				thisDialog.dialog("close");
				$.ajax({
					data: "command=createDiskOffering&isMirrored=false&response=json" + array1.join(""),
					dataType: "json",
					success: function(json) {						   
						var offering = json.creatediskofferingresponse;							
						diskJSONToTemplate(offering, template);							
						changeGridRowsTotal(submenuContent.find("#grid_rows_total"), 1);
						loadingImg.hide();  
                        rowContainer.show();    
					},			
                    error: function(XMLHttpResponse) {		                   
	                    handleError(XMLHttpResponse);	
	                    template.slideUp("slow", function(){ $(this).remove(); } );							    
                    }	
				});
			}, 
			"Cancel": function() { 
				$(this).dialog("close"); 
			} 
		}).dialog("open");			
		return false;
	});
			
	function listServiceOfferings() {	
	    var submenuContent = $("#submenu_content_service");
		
    	var commandString;            
		var advanced = submenuContent.find("#search_button").data("advanced");                    
		if (advanced != null && advanced) {		
		    var name = submenuContent.find("#advanced_search #adv_search_name").val();				   
		    var moreCriteria = [];								
			if (name!=null && trim(name).length > 0) 
				moreCriteria.push("&name="+encodeURIComponent(trim(name)));				
			commandString = "command=listServiceOfferings&page="+currentPage+moreCriteria.join("")+"&response=json";    
		} else {              
    	    var searchInput = submenuContent.find("#search_input").val();           	   
            if (searchInput != null && searchInput.length > 0) 
                commandString = "command=listServiceOfferings&page="+currentPage +"&keyword="+searchInput+"&response=json";
            else
                commandString = "command=listServiceOfferings&page="+currentPage+"&response=json"; 
        }   
    	
    	//listItems(submenuContent, commandString, jsonResponse1, jsonResponse2, template, fnJSONToTemplate);         
        listItems(submenuContent, commandString, "listserviceofferingsresponse", "serviceoffering", $("#service_template"), serviceJSONToTemplate);          	
	}
		
	submenuContentEventBinder($("#submenu_content_service"), listServiceOfferings);	
			
	$("#submenu_service").bind("click", function(event) {
	    event.preventDefault();					   
		
		$(this).toggleClass("submenu_links_on").toggleClass("submenu_links_off");
		currentSubMenu.toggleClass("submenu_links_off").toggleClass("submenu_links_on");
		currentSubMenu = $(this);
		
		var submenuContent = $("#submenu_content_service").show();
		$("#submenu_content_zones, #submenu_content_global, #submenu_content_disk").hide();	
		
		currentPage = 1;							
		listServiceOfferings();   	
	});		
	
	//Disk Offering
	$("#disk_template").bind("click", function(event) {		   
		var template = $(this);
		var link = $(event.target);
		var linkAction = link.attr("id");
		var diskId = template.data("diskId");
		var diskName = template.data("diskName");
		var submenuContent = $("#submenu_content_disk");
		
		switch (linkAction) {	
		    case "disk_action_edit" :	
		        var dialogEditDisk = $("#dialog_edit_disk");		        
				dialogEditDisk.find("#edit_disk_name").val(template.find("#disk_name").text());
				dialogEditDisk.find("#edit_disk_display").val(template.find("#disk_description").text());		
							
				dialogEditDisk
				.dialog('option', 'buttons', { 						
					"Confirm": function() { 							
						// validate values
				        var isValid = true;						        			
				        isValid &= validateString("Name", dialogEditDisk.find("#edit_disk_name"), dialogEditDisk.find("#edit_disk_name_errormsg"));
				        isValid &= validateString("Display Text", dialogEditDisk.find("#edit_disk_display"), dialogEditDisk.find("#edit_disk_display_errormsg"));											
				        if (!isValid) return;	
				
				        var name = trim(dialogEditDisk.find("#edit_disk_name").val());
						var display = trim(dialogEditDisk.find("#edit_disk_display").val());
						
						var dialogBox = $(this);					
						dialogBox.dialog("close");
						$.ajax({
							data: "command=updateDiskOffering&name="+encodeURIComponent(escape(name))+"&displayText="+encodeURIComponent(escape(display))+"&id="+diskId+"&response=json",
							dataType: "json",
							success: function(json) {									   				    
								template.find("#disk_description").text(display);
								template.find("#disk_name").text(name);
								template.data("diskName", name);
							}
						});
					}, 
					"Cancel": function() { 
						$(this).dialog("close"); 
					} 
				}).dialog("open");
				break;	
			case "disk_action_delete" :
				$("#dialog_confirmation")
				.html("<p>Please confirm you want to remove the disk offering: <b>"+diskName+"</b> from the management server. </p>")
				.dialog('option', 'buttons', { 						
					"Confirm": function() { 
						var dialogBox = $(this);
						$.ajax({
							data: "command=deleteDiskOffering&id="+diskId+"&response=json",
							dataType: "json",
							success: function(json) {
								dialogBox.dialog("close");
								template.slideUp("slow", function() {
									$(this).remove();										
									changeGridRowsTotal(submenuContent.find("#grid_rows_total"), -1);
								});
							}
						});
					},
					"Cancel": function() { 
						$(this).dialog("close"); 
					} 
				}).dialog("open");
				break;
			default :
				break;
		}
		return false;
	});
				
	function diskJSONToTemplate(json, template) {	
	    template.attr("id", "disk_"+json.id);	    
		if (index++ % 2 == 0) {
			template.addClass("smallrow_even");
		} else {
			template.addClass("smallrow_odd");
		}
		template.data("diskId", json.id).data("diskName", fromdb(json.name));	
				
		template.find("#disk_id").text(json.id);			
		template.find("#disk_name").text(fromdb(json.name));
		template.find("#disk_description").text(fromdb(json.displaytext));
	    template.find("#disk_disksize").text(convertBytes(json.disksize));
	    template.find("#disk_tags").text(fromdb(json.tags));
		template.find("#disk_domain").text(fromdb(json.domain)); 	
	}
		
	function listDiskOfferings() {		  
	    var submenuContent = $("#submenu_content_disk");
	
    	var commandString;            
		var advanced = submenuContent.find("#search_button").data("advanced");                    
		if (advanced != null && advanced) {		
		    var name = submenuContent.find("#advanced_search #adv_search_name").val();				   
		    var moreCriteria = [];								
			if (name!=null && trim(name).length > 0) 
				moreCriteria.push("&name="+encodeURIComponent(trim(name)));						
			commandString = "command=listDiskOfferings&page="+currentPage+moreCriteria.join("")+"&response=json";      //moreCriteria.join("")
		} else {              
    	    var searchInput = submenuContent.find("#search_input").val();            
            if (searchInput != null && searchInput.length > 0) 
                commandString = "command=listDiskOfferings&page="+currentPage+"&keyword="+searchInput+"&response=json";                                
            else
                commandString = "command=listDiskOfferings&page="+currentPage+"&response=json";    
        }
    	  
    	//listItems(submenuContent, commandString, jsonResponse1, jsonResponse2, template, fnJSONToTemplate);         
        listItems(submenuContent, commandString, "listdiskofferingsresponse", "diskoffering", $("#disk_template"), diskJSONToTemplate);              	
	}		
	
	submenuContentEventBinder($("#submenu_content_disk"), listDiskOfferings);	
				
	$("#submenu_disk").bind("click", function(event) {	
	    event.preventDefault();	
	    	
		$(this).toggleClass("submenu_links_on").toggleClass("submenu_links_off");
		currentSubMenu.toggleClass("submenu_links_off").toggleClass("submenu_links_on");
		currentSubMenu = $(this);
		
		var submenuContent = $("#submenu_content_disk").show();
		$("#submenu_content_zones, #submenu_content_service, #submenu_content_global").hide();			
		
		currentPage=1;
		listDiskOfferings();
	});
			
	$("#submenu_global").click();
}