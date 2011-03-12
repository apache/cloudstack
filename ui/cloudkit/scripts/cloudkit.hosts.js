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
 
function convertBytes(bytes) {
	if (bytes < 1024 * 1024) {
		return (bytes / 1024).toFixed(2) + " KB";
	} else if (bytes < 1024 * 1024 * 1024) {
		return (bytes / 1024 / 1024).toFixed(2) + " MB";
	} else if (bytes < 1024 * 1024 * 1024 * 1024) {
		return (bytes / 1024 / 1024 / 1024).toFixed(2) + " GB";
	} else {
		return (bytes / 1024 / 1024 / 1024 / 1024).toFixed(2) + " TB";
	}
}

function convertHz(hz) {
    if (hz == null)
        return "";

	if (hz < 1000) {
		return hz + " MHZ";
	} else {
		return (hz / 1000).toFixed(2) + " GHZ";
	} 
}
 
$(document).ready(function() {
	if (g_loginResponse == null) {
		logout();
		return;
	}
	
	// setup dialog
	var dialog = $("#dialog_overlay");
	dialog.find("#dialog_cancel, #dialog_ok").bind("click", function(event) {
		dialog.hide();
	});
	dialog.find("#dialog_confirm").bind("click", function(event) {
		var id = $(this).data("hostid");
		$.ajax({
			data: createURL("command=deleteHost&id="+id),				
			success: function(json) {
				$("#host_"+id).slideUp("slow", function() {
					$(this).remove();
				});
			}
		});	
		dialog.hide();
	});
	
	// setup host template and container
	var hostTemplate = $("#host_template");
	var hostContainer = $("#host_container");
	
	hostContainer.bind("click", function(event) {
		var $container = $(this);
	    var target = $(event.target);
	    var targetId = target.attr("id");
		
		switch (targetId) {
			case "host_details" :
				var details = dialog.find("#dialog_host_details").show();
				dialog.find("#dialog_delete_host").hide();
				var jsonObj = target.data("jsonObj");
				
				details.find("#host_id").text(jsonObj.id);
				details.find("#host_cpu_total").text((jsonObj.cpuspeed == null)? "": (jsonObj.cpunumber + "x" + convertHz(jsonObj.cpuspeed)));
				details.find("#host_cpu_allocated").text((jsonObj.cpuallocated == null)? "": jsonObj.cpuallocated);
				details.find("#host_cpu_used").text((jsonObj.cpuused == null)? "": jsonObj.cpuused);
				details.find("#host_mem_total").text((jsonObj.memorytotal == null)? "": convertBytes(jsonObj.memorytotal));
				details.find("#host_mem_allocated").text((jsonObj.memoryallocated == null)? "": convertBytes(jsonObj.memoryallocated));
				details.find("#host_mem_used").text((jsonObj.memeoryused == null)? "": convertBytes(jsonObj.memeoryused));
				details.find("#host_net_read").text((jsonObj.networkkbsread == null)? "": convertBytes(jsonObj.networkkbsread * 1024));
				details.find("#host_net_sent").text((jsonObj.networkkbswrite == null)? "": convertBytes(jsonObj.networkkbswrite *1024));
				
				if (jsonObj.created != null) {
					var created = new Date();
					created.setISO8601(jsonObj.created);
					details.find("#host_added").text(created.format("m/d/Y H:i:s"));
				}
				else {
					details.find("#host_added").text("");
				}
				dialog.show();
				break;
			case "host_delete" :
				dialog.find("#dialog_host_details").hide();
				dialog.find("#dialog_delete_host").show();
				var jsonObj = target.data("jsonObj");
				dialog.find("#hostname").text(jsonObj.name);
				dialog.find("#dialog_confirm").data("hostid", jsonObj.id);
				dialog.show();
				break;
		}
	});
	

	var page = 1;
	var midmenuItemCount = 13;
	function listHosts() {
		if(page > 1)
            $("#prev_page_button").show();
		else
			$("#prev_page_button").hide();
				
		var cmdString = "command=listHosts&type=Routing"+"&pagesize="+midmenuItemCount+"&page="+page;				
		var searchInput = $("#search_panel").find("#search_input").val();	 
	    if (searchInput != null && searchInput.length > 0) 
	    	cmdString += ("&keyword="+searchInput);		      	
		
		$.ajax({
			data: createURL(cmdString),				
			success: function(json) {	
				hostContainer.empty();				
				$("#page_number").text(page);				
				var hosts = json.listhostsresponse.host;
				if (hosts != null && hosts.length >0) {
					if(hosts.length >= midmenuItemCount)
                        $("#next_page_button").show();
					else
						$("#next_page_button").hide();
					
					for (var i = 0; i < hosts.length; i++) {
						var host = hosts[i];
						var template = hostTemplate.clone(true).attr("id", "host_"+host.id);
						template.find("#host_details").data("jsonObj", host);
						template.find("#host_delete").data("jsonObj", host);
						template.find("#hostname").text(host.name);
						template.find("#ip").text(host.ipaddress);
						template.find("#version").text(host.version);

						if (host.disconnected != null) {
							var disconnected = new Date();
							disconnected.setISO8601(host.disconnected);
							template.find("#disconnected").text(disconnected.format("m/d/Y H:i:s"));
						}
						var state = host.state;
						template.find("#state").text(state);
						if (state == 'Up') {
							template.find("#state").removeClass("red").addClass("green");
						} 
						else {
							template.find("#state").removeClass("green").addClass("red");
						}
						hostContainer.append(template.show());
					}
				}
				else {
					$("#next_page_button").hide();	
				}
			}
		});	
	}
	
	
	// *** Setup tab clicks (begin) ***
	$("#tab_hosts").bind("click", function(event) {
		$(this).removeClass("off").addClass("on");
		$("#tab_docs").removeClass("on").addClass("off");
		$("#tab_docs_content").hide();
		$("#tab_hosts_content").show();
		$("#search_input").val("");
		$("#search_panel").show();					
		page = 1; //reset pagination to the first page
		listHosts(); 
		return false;
	});	
	
	$("#tab_docs").bind("click", function(event) {
		$(this).removeClass("off").addClass("on");
		$("#tab_hosts").removeClass("on").addClass("off");
		$("#tab_hosts_content").hide();
		$("#tab_docs_content").show();
		$("#search_panel").hide();
				
		$.ajax({
			data: createURL("command=listZones&domainid="+g_loginResponse.domainid),	// Setup zone key			
			success: function(json) {	
				var zones = json.listzonesresponse.zone;
				if (zones != null && zones.length >0) {
					$("#zone_token").text(zones[0].zonetoken);
				}
			}
		});	
		
		return false;
	});	
	// *** Setup tab clicks (end) ***
	
	// *** Pagination (begin) ***
	$("#tab_hosts_content").find("#prev_page_button").bind("click", function(event){
		page--
		listHosts();
		return false;
	});
	
	$("#tab_hosts_content").find("#next_page_button").bind("click", function(event){
		page++
		listHosts();
		return false;
	});
	// *** Pagination (begin) ***
	
	// *** Search (begin) ***
	$("#search_input").bind("keypress", function(event) {
		if(event.keyCode == keycode_Enter) {
			page = 1; //reset pagination to the first page
			listHosts();
		}
		else {
		    $("#clear_search_button").show();	
		}
		return true;
	});	
	$("#clear_search_button").bind("click", function(event){
		$("#search_input").val("");					
		page = 1; //reset pagination to the first page
		listHosts(); 
		$(this).hide();
		return false;
	});
	// *** Search (end) ***
	
	// *** Refresh button (begin) ***
	$("#refresh_button").bind("click", function(event){
		listHosts();
		return false;
	});
	// *** Refresh button (end) ***
	
	var oneHostUp = false;
	var atLeastOneHost = false;	
	$.ajax({
		data: createURL("command=listHosts&type=Routing"),				
		success: function(json) {	
			hostContainer.empty();
			var hosts = json.listhostsresponse.host;
			if (hosts != null && hosts.length >0) {				
				atLeastOneHost = true;
				for (var i = 0; i < hosts.length; i++) {
					var host = hosts[i];					
					if (host.state == 'Up') {
						oneHostUp = true;
					} 			
				}
			}
		}
	});	
	
	if (g_loginResponse.registered == "false") {
		if (!atLeastOneHost) {
			$("#tab_docs").click();
		} 
		else if (oneHostUp) {
			$("#registration_complete_link").attr("href","https://my.rightscale.com/cloud_registrations/my_cloud/cloud_stack/new?callback_url="+encodeURIComponent("http://216.38.159.3:8080/client/cloudkit/complete?token="+g_loginResponse.registrationtoken));
			$("#registration_complete_container").show();
			$("#tab_hosts").click();	
		}
		else {
			$("#tab_hosts").click();
		}
	} 
	else {
		$("#registration_complete_container").hide();
		$("#tab_hosts").click();	
	}
		
	$("#registration_complete_doc_link").attr("href","https://my.rightscale.com/cloud_registrations/my_cloud/cloud_stack/new?callback_url="+encodeURIComponent("http://216.38.159.3:8080/client/cloudkit/complete?token="+g_loginResponse.registrationtoken));			
	$("#main").show();
});


