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

function showHostsTab() {
    var rIndex = 0;	
	var sIndex = 0;
	var pIndex = 0;
	
	// Dialog Setup
	if (getHypervisorType() != "kvm") { //"xenserver"
		$("#host_action_new_routing").show();
		activateDialog($("#dialog_add_routing").dialog({ 
			autoOpen: false,
			modal: true,
			zIndex: 2000
		}));
		
		var dialogAddRouting = $("#dialog_add_routing");
					
		$.ajax({
			data: "command=listZones&available=true&response=json",
			dataType: "json",
			success: function(json) {
				var zones = json.listzonesresponse.zone;
				var zoneSelect = dialogAddRouting.find("#host_zone").empty();								
				if (zones != null && zones.length > 0) {
					for (var i = 0; i < zones.length; i++) 
						zoneSelect.append("<option value='" + zones[i].id + "'>" + fromdb(zones[i].name) + "</option>"); 				    
				}
				//dialogAddRouting.find("#host_zone").change();
			}
		});
		
		dialogAddRouting.find("#host_zone").bind("change", function(event) {
			var zoneId = $(this).val();
			$.ajax({
				data: "command=listPods&zoneId="+zoneId+"&response=json",
				dataType: "json",
				async: false,
				success: function(json) {
					var pods = json.listpodsresponse.pod;
					var podSelect = dialogAddRouting.find("#host_pod").empty();	
					if (pods != null && pods.length > 0) {
						for (var i = 0; i < pods.length; i++) {
							podSelect.append("<option value='" + pods[i].id + "'>" + fromdb(pods[i].name) + "</option>"); 
						}
					}
					dialogAddRouting.find("#host_pod").change();
				}
			});
		});
		
		dialogAddRouting.find("#host_pod").bind("change", function(event) {			   
		    var podId = $(this).val();
		    if(podId == null || podId.length == 0)
		        return;
		    var clusterSelect = dialogAddRouting.find("#cluster_select").empty();		        
		    $.ajax({
		        data: "command=listClusters&response=json&podid=" + podId,
		        dataType: "json",
		        success: function(json) {			            
		            var items = json.listclustersresponse.cluster;
		            if(items != null && items.length > 0) {			                
		                for(var i=0; i<items.length; i++) 			                    
		                    clusterSelect.append("<option value='" + items[i].id + "'>" + items[i].name + "</option>");		      
	                    dialogAddRouting.find("input[value=existing_cluster_radio]").attr("checked", true);
		            }
		            else {
						clusterSelect.append("<option value='-1'>None Available</option>");
		                dialogAddRouting.find("input[value=new_cluster_radio]").attr("checked", true);
		            }
		        }
		    });
		});
	}
	activateDialog($("#dialog_update_os").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));
	$.ajax({
		data: "command=listOsCategories&response=json",
		dataType: "json",
		success: function(json) {
			var categories = json.listoscategoriesresponse.oscategory;
			var select = $("#dialog_update_os #host_os");								
			if (categories != null && categories.length > 0) {
				for (var i = 0; i < categories.length; i++) 
					select.append("<option value='" + categories[i].id + "'>" + categories[i].name + "</option>"); 				    
			}
		}
	});
	
	// Routing Template Setup
	var routingTemplate = $("#routing_template");
	routingTemplate.bind("mouseenter", function(event) {
		$(this).find("#grid_links_container").show();
		return false;
	});
	routingTemplate.bind("mouseleave", function(event) {
		$(this).find("#grid_links_container").hide();
		return false;
	});
	
	function vmJSONToTemplate(json, template, type) {
        var template = template.attr("id","vm"+json.id);
        if (index++ % 2 == 0) {
	        template.addClass("hostadmin_showdetails_row_even");
        } else {
	        template.addClass("hostadmin_showdetails_row_odd");
        }
        template.find("#detail_type").text(type);
        template.find("#detail_name").text(getVmName(json.name, json.displayname));
        
        if(type == "Instance")
            template.find("#detail_ip").text(json.ipaddress);
        else //Router, System
            template.find("#detail_ip").text(json.privateip);
            
        template.find("#detail_service").text(json.serviceofferingname);
        
        if(json.account == null && type == "System")
            template.find("#detail_owner").text("system");
        else
            template.find("#detail_owner").text(json.account);	        
        
        setDateField(json.created, template.find("#detail_created"));	
    }
	
	routingTemplate.bind("click", function(event) {
		var template = $(this);
		var link = $(event.target);
		var linkAction = link.attr("id");
		var hostId = template.data("hostId");
		var hostName = template.data("hostName");
		var submenuContent = $("#submenu_content_routing");
		switch (linkAction) {
			case "host_action_details" :
				var expanded = link.data("expanded");
				if (expanded == null || expanded == false) {																	                                         
                    var itemTotal = 0;                        
                    var vms, routers, systemVms;
                    $.ajax({
						cache: false,
						data: "command=listVirtualMachines&hostid="+hostId+"&response=json",
						dataType: "json",
						async: false,
						success: function(json) {							    					    
							vms = json.listvirtualmachinesresponse.virtualmachine;
							if(vms != null)
							    itemTotal += vms.length;																					
						}
					});								
                    $.ajax({
						cache: false,
						data: "command=listRouters&hostid="+hostId+"&response=json",
						dataType: "json",
						async: false,
						success: function(json) {								    					    					    
							routers = json.listroutersresponse.router;
							if(routers != null)
							    itemTotal += routers.length;																					
						}
					});												
					$.ajax({
						cache: false,
						data: "command=listSystemVms&hostid="+hostId+"&response=json",
						dataType: "json",
						async: false,
						success: function(json) {								    					    					    					    
							systemVms = json.listsystemvmsresponse.systemvm;
							if(systemVms != null)
							    itemTotal += systemVms.length;																					
						}
					});	
																																			
					if(itemTotal > 0) {
					    var detailGrid = template.find("#detail_container").empty();
					    var detailTemplate = $("#routing_detail_template");  							    
					    if (vms != null && vms.length > 0) {						
							for (var i = 0; i < vms.length; i++) {									    
							    var newDetailTemplate = detailTemplate.clone(true);
		                        vmJSONToTemplate(vms[i], newDetailTemplate, "Instance"); 
		                        detailGrid.append(newDetailTemplate.show());											
							}
						}				
						if (routers != null && routers.length > 0) {				
							for (var i = 0; i < routers.length; i++) {									    
							    var newDetailTemplate = detailTemplate.clone(true);
		                        vmJSONToTemplate(routers[i], newDetailTemplate, "Router"); 
		                        detailGrid.append(newDetailTemplate.show());											
							}
						}								
						if (systemVms != null && systemVms.length > 0) {						
							for (var i = 0; i < systemVms.length; i++) {									    
							    var newDetailTemplate = detailTemplate.clone(true);
		                        vmJSONToTemplate(systemVms[i], newDetailTemplate, "System"); 
		                        detailGrid.append(newDetailTemplate.show());											
							}
						}												    
					}							
				    template.find("#host_action_details_container img").attr("src", "images/details_uparrow.jpg");
					template.find("#host_action_details_container a").text("Hide Details");
					template.find("#host_detail_panel").slideDown("slow");
					link.data("expanded", true);	
				} else {
					template.find("#host_action_details_container img").attr("src", "images/details_downarrow.jpg");
					template.find("#host_action_details_container a").text("Show Details");
					template.find("#host_detail_panel").slideUp("slow");
					link.data("expanded", false);
				}
				break;
			case "host_action_enable_maint" :
				$("#dialog_confirmation")
				.html("<p>Please confirm you enable maintenance for host: <b>"+fromdb(hostName)+"</b>.  Enabling maintenance mode will cause a live migration of all running instances on this host to any available host.  An alert will be sent to the admin when this process has been completed.</p>")
				.dialog('option', 'buttons', { 						
					"Confirm": function() { 
						var dialogBox = $(this);
						$.ajax({
							data: "command=prepareHostForMaintenance&id="+hostId+"&response=json",
							dataType: "json",
							success: function(json) {
								dialogBox.dialog("close");
								
								template.find(".row_loading").show();
								template.find(".loading_animationcontainer .loading_animationtext").text("Preparing...");
								template.find(".loading_animationcontainer").show();
								template.fadeIn("slow");
								var that = template;
								template.find(".continue_button").data("hostId", hostId).unbind("click").bind("click", function(event) {										
									that.find(".loading_animationcontainer").hide();
									that.find(".loadingmessage_container").fadeOut("slow");
									that.find(".row_loading").fadeOut("slow");										
									
									// Host status is likely to change at this point. So, refresh the row now.
									$.ajax({
			                            data: "command=listHosts&id="+hostId+"&response=json",
			                            dataType: "json",
			                            success: function(json) {                            				   
                        				    routingJSONToTemplate(json.listhostsresponse.host[0], that);                            				    
			                            }
		                            });				                            
		                            return false;										
								});
								var timerKey = "host"+hostId;
								$("body").everyTime(
									15000, // Migration could possibly take a while
									timerKey,
									function() {
										$.ajax({
											data: "command=queryAsyncJobResult&jobId="+json.preparehostformaintenanceresponse.jobid+"&response=json",
											dataType: "json",
											success: function(json) {
												var result = json.queryasyncjobresultresponse;
												if (result.jobstatus == 0) {
													return; //Job has not completed
												} else {
													$("body").stopTime(timerKey);
													if (result.jobstatus == 1) {
														// Succeeded
														routingStateToTemplate(result.host[0].state, template);																
														template.find("#routing_disconnected").text(result.host[0].disconnected);
														template.find(".loadingmessage_container .loadingmessage_top p").html("We are actively enabling maintenance on your host.  Please refresh periodically for an updated status.");
														template.find(".loadingmessage_container").fadeIn("slow");															
													} else if (result.jobstatus == 2) {
														// Failed
														routingStateToTemplate(result.host[0].state, template);																
														template.find(".loadingmessage_container .loadingmessage_top p").text("We were unable to successfully prepare your host for maintenance.  Please check your logs for more info.");
														template.find(".loadingmessage_container").fadeIn("slow");															
													}
												}
											},
											error: function(XMLHttpResponse) {
												$("body").stopTime(timerKey);
												handleError(XMLHttpResponse);
											}
										});
									},
									0
								);
							}
						});
					}, 
					"Cancel": function() { 
						$(this).dialog("close"); 
					} 
				}).dialog("open");
				break;
			case "host_action_cancel_maint" :
				$("#dialog_confirmation")
				.html("<p>Please confirm you want to cancel maintenance for host: <b>"+fromdb(hostName)+"</b>. </p>")
				.dialog('option', 'buttons', { 						
					"Confirm": function() { 
						var dialogBox = $(this);
						$.ajax({
							data: "command=cancelHostMaintenance&id="+hostId+"&response=json",
							dataType: "json",
							success: function(json) {
								dialogBox.dialog("close");
								
								template.find(".row_loading").show();
								template.find(".loading_animationcontainer .loading_animationtext").text("Cancelling...");
								template.find(".loading_animationcontainer").show();
								template.fadeIn("slow");
								var that = template;
								template.find(".continue_button").data("hostId", hostId).unbind("click").bind("click", function(event) {																			
									that.find(".loading_animationcontainer").hide();
									that.find(".loadingmessage_container").fadeOut("slow");
									that.find(".row_loading").fadeOut("slow");
									
									// Host status is likely to change at this point. So, refresh the row now.
									$.ajax({
			                            data: "command=listHosts&id="+hostId+"&response=json",
			                            dataType: "json",
			                            success: function(json) {                            				   
                        				    routingJSONToTemplate(json.listhostsresponse.host[0], that);                            				    
			                            }
		                            });											
									return false;
								});
								var timerKey = "host"+hostId;
								$("body").everyTime(
									5000,
									timerKey,
									function() {
										$.ajax({
											data: "command=queryAsyncJobResult&jobId="+json.cancelhostmaintenanceresponse.jobid+"&response=json",
											dataType: "json",
											success: function(json) {
												var result = json.queryasyncjobresultresponse;
												if (result.jobstatus == 0) {
													return; //Job has not completed
												} else {
													$("body").stopTime(timerKey);
													if (result.jobstatus == 1) {
														// Succeeded
														routingStateToTemplate(result.host[0].state, template);	//result.host[0].status == "ErrorInMaintenance"														
														template.find("#routing_disconnected").text(result.host[0].disconnected);
														template.find(".loadingmessage_container .loadingmessage_top p").html("We are actively cancelling your scheduled maintenance.  Please refresh periodically for an updated status.");
														template.find(".loadingmessage_container").fadeIn("slow");
													} else if (result.jobstatus == 2) {
														// Failed
														routingStateToTemplate(result.host[0].state, template);															
														template.find(".loadingmessage_container .loadingmessage_top p").text("We were unable to cancel your maintenance process.  Please try again.");
														template.find(".loadingmessage_container").fadeIn("slow");
													}														
												}
											},
											error: function(XMLHttpResponse) {
												$("body").stopTime(timerKey);
												template.find(".loading_animationcontainer").hide();
												handleError(XMLHttpResponse);
											}
										});
									},
									0
								);
							}
						});
					}, 
					"Cancel": function() { 
						$(this).dialog("close"); 
					} 
				}).dialog("open");
				break;
			case "host_action_reconnect" :
				$("#dialog_confirmation")
				.html("<p>Please confirm you want to force a reconnection for host: <b>"+fromdb(hostName)+"</b>. </p>")
				.dialog('option', 'buttons', { 						
					"Confirm": function() { 
						var dialogBox = $(this);
						$.ajax({
							data: "command=reconnectHost&id="+hostId+"&response=json",
							dataType: "json",
							success: function(json) {
								dialogBox.dialog("close");
								
								template.find(".row_loading").show();
								template.find(".loading_animationcontainer .loading_animationtext").text("Reconnecting...");
								template.find(".loading_animationcontainer").show();
								template.fadeIn("slow");
								var that = template;
								template.find(".continue_button").data("hostId", hostId).unbind("click").bind("click", function(event) {										
									that.find(".loading_animationcontainer").hide();
									that.find(".loadingmessage_container").fadeOut("slow");
									that.find(".row_loading").fadeOut("slow");
									
								    // Host status is likely to change at this point. So, refresh the row now.
									$.ajax({
			                            data: "command=listHosts&id="+hostId+"&response=json",
			                            dataType: "json",
			                            success: function(json) {                            				   
                        				    routingJSONToTemplate(json.listhostsresponse.host[0], that);                            				    
			                            }
		                            });	
		                            return false;	
								});
								var timerKey = "host"+hostId;
								$("body").everyTime(
									5000,
									timerKey,
									function() {
										$.ajax({
											data: "command=queryAsyncJobResult&jobId="+json.reconnecthostresponse.jobid+"&response=json",
											dataType: "json",
											success: function(json) {
												var result = json.queryasyncjobresultresponse;
												if (result.jobstatus == 0) {
													return; //Job has not completed
												} else {
													$("body").stopTime(timerKey);
													if (result.jobstatus == 1) {
														// Succeeded
														routingStateToTemplate(result.host[0].state, template);										
														template.find("#routing_disconnected").text(result.host[0].disconnected);
														template.find(".loadingmessage_container .loadingmessage_top p").html("We are actively reconnecting your host.  Please refresh periodically for an updated status.");
														template.find(".loadingmessage_container").fadeIn("slow");						
														
													} else if (result.jobstatus == 2) {
														// Failed
														routingStateToTemplate(result.host[0].state, template);								
														template.find(".loadingmessage_container .loadingmessage_top p").text("We were unable to reconnect your host.  Please try again.");
														template.find(".loadingmessage_container").fadeIn("slow");		
													}
												}
											},
											error: function(XMLHttpResponse) {
												$("body").stopTime(timerKey);
												handleError(XMLHttpResponse);
											}
										});
									},
									0
								);
							}
						});
					}, 
					"Cancel": function() { 
						$(this).dialog("close"); 
					} 
				}).dialog("open");
				break;
			case "host_action_remove" :
				$("#dialog_confirmation")
				.html("<p>Please confirm you want to remove this host: <b>"+fromdb(hostName)+"</b> from the management server. </p>")
				.dialog('option', 'buttons', { 						
					"Confirm": function() { 
						$(this).dialog("close"); 
						$.ajax({
							data: "command=deleteHost&id="+hostId+"&response=json",
							dataType: "json",
							success: function(json) {									
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
			case "host_action_update_os" :
				$("#dialog_update_os #host_os").val(template.data("osId"));
				$("#dialog_update_os")
				.dialog('option', 'buttons', { 						
					"Update": function() { 
						var dialogBox = $(this);
						var osId = $("#dialog_update_os #host_os").val();
						var osName = $("#dialog_update_os #host_os option:selected").text();
						var category = "";
						if (osId.length > 0) {
							category = "&osCategoryId="+osId;
						}
						$.ajax({
							data: "command=updateHost&id="+hostId+category+"&response=json",
							dataType: "json",
							success: function(json) {
								template.find("#routing_os").text(osName);
								template.data("osId", osId);
								dialogBox.dialog("close");
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

	// FUNCTION: Routing JSON to Template
	function routingJSONToTemplate(json, template) {
	    template.attr("id", "host"+json.id);
	
		if (index++ % 2 == 0) {
			template.find("#row_container").addClass("row_even");
		} else {
			template.find("#row_container").addClass("row_odd");
		}
		template.data("hostId", json.id).data("hostName", fromdb(json.name));
				
		template.find("#routing_zone").text(json.zonename);
		template.find("#routing_pod").text(json.podname);
		template.find("#routing_cluster").text(json.clustername);
		
		template.find("#routing_name").text(json.name);
		template.find("#routing_ipaddress").text(json.ipaddress);
		template.find("#routing_version").text(json.version);
		template.find("#routing_os").text(json.oscategoryname);
		template.data("osId", json.oscategoryid);
					
		setDateField(json.disconnected, template.find("#routing_disconnected"));
		
		var spaceCharacter = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"			
		var statHtml = "<div class='hostcpu_icon'></div><p><strong> CPU Total:</strong> " + ((json.cpunumber==null)? spaceCharacter:json.cpunumber) + " x " + ((json.cpuspeed==null)? spaceCharacter:convertHz(json.cpuspeed))+" | <strong>CPU Allocated:</strong> " + ((json.cpuallocated==null)? spaceCharacter:json.cpuallocated) + " | <span class='host_statisticspanel_green'> <strong>CPU Used:</strong> " + ((json.cpuused==null)? spaceCharacter:json.cpuused) + "</span></p>";
		template.find("#host_cpu_stat").html(statHtml);
		statHtml = "<div class='hostmemory_icon'></div><p><strong> MEM Total:</strong> " + ((json.memorytotal==null)? spaceCharacter:convertBytes(json.memorytotal))+" | <strong>MEM Allocated:</strong> " + ((json.memoryallocated==null)? spaceCharacter:convertBytes(json.memoryallocated)) + " | <span class='host_statisticspanel_green'> <strong>MEM Used:</strong> " + ((json.memoryused==null)? spaceCharacter:convertBytes(json.memoryused)) + "</span></p>";
		template.find("#host_mem_stat").html(statHtml);				
		statHtml = "<div class='hostnetwork_icon'></div><p><strong> Network Read:</strong> " + ((json.networkkbsread==null)? spaceCharacter:convertBytes(json.networkkbsread * 1024))+" | <strong>Network Write:</strong> " + ((json.networkkbswrite==null)? spaceCharacter:convertBytes(json.networkkbswrite * 1024)) + "</p>";
		template.find("#host_network_stat").html(statHtml);			
		
		routingStateToTemplate(json.state, template);
	}
	
    function routingStateToTemplate(state, template) {	
        template.find(".grid_links").find("#host_action_reconnect_container, #host_action_enable_maint_container, #host_action_cancel_maint_container, #host_action_remove_container, #host_action_update_os_container").show();
    	    
		if (state == 'Up' || state == "Connecting") {
			template.find("#host_state_bar").removeClass("yellow_statusbar grey_statusbar red_statusbar").addClass("green_statusbar ");
			template.find("#routing_state").text(state).removeClass("grid_celltitles grid_stoppedtitles").addClass("grid_runningtitles");
			template.find(".grid_links").find("#host_action_cancel_maint_container, #host_action_remove_container").hide();
		} else if (state == 'Down' || state == "Alert") {
			template.find("#host_state_bar").removeClass("yellow_statusbar grey_statusbar green_statusbar").addClass("red_statusbar");
			template.find("#routing_state").text(state).removeClass("grid_celltitles grid_runningtitles").addClass("grid_stoppedtitles");
			
			if (state == "Alert") {
				template.find(".grid_links").find("#host_action_reconnect_container, #host_action_enable_maint_container, #host_action_cancel_maint_container, #host_action_remove_container").hide();
			} else {
				template.find(".grid_links").find("#host_action_reconnect_container, #host_action_cancel_maint_container").hide();
			}
		} else {
			template.find("#host_state_bar").removeClass("yellow_statusbar green_statusbar red_statusbar").addClass("grey_statusbar");
			template.find("#routing_state").text(state).removeClass("grid_runningtitles grid_stoppedtitles").addClass("grid_celltitles ");
			
			if (state == "ErrorInMaintenance") {
				template.find(".grid_links").find("#host_action_reconnect_container, #host_action_remove_container").hide();
			} else if (state == "PrepareForMaintenance") {
				template.find(".grid_links").find("#host_action_reconnect_container, #host_action_enable_maint_container, #host_action_remove_container").hide();
			} else if (state == "Maintenance") {
				template.find(".grid_links").find("#host_action_reconnect_container, #host_action_enable_maint_container").hide();
			} else if (state == "Disconnected") {
				template.find(".grid_links").find("#host_action_reconnect_container, #host_action_enable_maint_container, #host_action_cancel_maint_container").hide();
			} else {
				alert("Unsupported Host State: " + state);
			}
		} 
	}
	
	var submenuContent = $("#submenu_content_routing");
	
	// Add New Routing Host
	if (getHypervisorType() != "kvm") {
		$("#host_action_new_routing").bind("click", function(event) {
			dialogAddRouting.find("#new_cluster_name").val("");
			dialogAddRouting.find("#host_zone").change(); //refresh cluster dropdown
		    
		    dialogAddRouting
		    .dialog('option', 'buttons', { 				
			    "Add": function() { 
			        var dialogBox = $(this);				   
				    var clusterRadio = dialogBox.find("input[name=cluster]:checked").val();				
				
				    // validate values
				    var isValid = true;									
				    isValid &= validateString("Host name", dialogBox.find("#host_hostname"), dialogBox.find("#host_hostname_errormsg"));
				    isValid &= validateString("User name", dialogBox.find("#host_username"), dialogBox.find("#host_username_errormsg"));
				    isValid &= validateString("Password", dialogBox.find("#host_password"), dialogBox.find("#host_password_errormsg"));						
				    if(clusterRadio == "new_cluster_radio")
				        isValid &= validateString("Cluster name", dialogBox.find("#new_cluster_name"), dialogBox.find("#new_cluster_name_errormsg"));					
				    if (!isValid) return;
					
				    var array1 = [];
					
				    var zoneId = dialogBox.find("#host_zone").val();
				    array1.push("&zoneId="+zoneId);
					
				    var podId = dialogBox.find("#host_pod").val();	
				    array1.push("&podId="+podId);
							      
				    var username = trim(dialogBox.find("#host_username").val());
				    array1.push("&username="+encodeURIComponent(username));
					
				    var password = trim(dialogBox.find("#host_password").val());
				    array1.push("&password="+encodeURIComponent(password));
					
				    if(clusterRadio == "new_cluster_radio") {
				        var newClusterName = trim(dialogBox.find("#new_cluster_name").val());
				        array1.push("&clustername="+encodeURIComponent(newClusterName));				    
				    }
				    else if(clusterRadio == "existing_cluster_radio") {
				        var clusterId = dialogBox.find("#cluster_select").val();
						// We will default to no cluster if someone selects Join Cluster with no cluster available.
						if (clusterId != '-1') {
							array1.push("&clusterid="+clusterId);
						}
				    }
					
				    var hostname = trim(dialogBox.find("#host_hostname").val());
				    var url;					
				    if(hostname.indexOf("http://")==-1)
				        url = "http://" + hostname;
				    else
				        url = hostname;
				    array1.push("&url="+encodeURIComponent(url));
										
				    var template = $("#routing_template").clone(true);		
				    var loadingImg = template.find(".adding_loading");		
                    var rowContainer = template.find("#row_container");    	                               
                    loadingImg.find(".adding_text").text("Adding....");	
                    loadingImg.show();  
                    rowContainer.hide();                                                                                     					
				    submenuContent.find("#grid_content").append(template.fadeIn("slow")); 
					
				    dialogBox.dialog("close");
				    $.ajax({
					    data: "command=addHost&response=json" + array1.join(""),
					    dataType: "json",
					    success: function(json) {						    
						    var items = json.addhostresponse.host;
						    routingJSONToTemplate(items[0], template);							
						    loadingImg.hide();  
                            rowContainer.show();  
                            changeGridRowsTotal(submenuContent.find("#grid_rows_total"), 1); 
                                                            
                            if(items.length > 1) { 
                                for(var i=1; i<items.length; i++) { 
                                    var anotherNewTemplate = $("#routing_template").clone(true);	
                                    routingJSONToTemplate(items[i], anotherNewTemplate);	
                                    submenuContent.find("#grid_content").append(anotherNewTemplate.fadeIn("slow")); 
                                    changeGridRowsTotal(submenuContent.find("#grid_rows_total"), 1); 
                                }	
                            }                                
                            
                            if(clusterRadio == "new_cluster_radio")
                                dialogBox.find("#new_cluster_name").val("");
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
	}
			
	function listHosts() {			
	    var submenuContent = $("#submenu_content_routing");			    
	               			
        var commandString;            
		var advanced = submenuContent.find("#search_button").data("advanced");                    
		if (advanced != null && advanced) {		
		    var name = submenuContent.find("#advanced_search #adv_search_name").val();	
		    var state = submenuContent.find("#advanced_search #adv_search_state").val();
		    var zone = submenuContent.find("#advanced_search #adv_search_zone").val();
		    var pod = submenuContent.find("#advanced_search #adv_search_pod").val();			    
		    var moreCriteria = [];								
			if (name!=null && trim(name).length > 0) 
				moreCriteria.push("&name="+encodeURIComponent(trim(name)));				
			if (state!=null && state.length > 0) 
				moreCriteria.push("&state="+state);		
		    if (zone!=null && zone.length > 0) 
				moreCriteria.push("&zoneId="+zone);		
		    if (pod!=null && pod.length > 0) 
				moreCriteria.push("&podId="+pod);					
			commandString = "command=listHosts&page=" + currentPage + moreCriteria.join("") + "&type=Routing&response=json";   //moreCriteria.join("")
		} else {          
			var searchInput = submenuContent.find("#search_input").val();            
			if (searchInput != null && searchInput.length > 0) 
				commandString = "command=listHosts&page=" + currentPage + "&keyword=" + searchInput + "&type=Routing&response=json";
			else
				commandString = "command=listHosts&page=" + currentPage + "&type=Routing&response=json";
		}
        
         //listItems(submenuContent, commandString, jsonResponse1, jsonResponse2, template, fnJSONToTemplate);         
        listItems(submenuContent, commandString, "listhostsresponse", "host", $("#routing_template"), routingJSONToTemplate);         
	};				
	
	submenuContentEventBinder($("#submenu_content_routing"), listHosts);
	
	currentPage = 1;	
	listHosts();
}