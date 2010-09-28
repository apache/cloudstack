$(document).ready(function() {
	// Prevent the UI from being iframed if the iframe isn't from the same domain.
	try {
		if ( top != self && self.location.hostname != top.location.hostname) {
			// leaving the code here in the oft change an older browser is being used that does not have
			// cross-site scripting prevention.
			alert("Detected a frame (" + top.location.hostname + ") not from the same domain (" + self.location.hostname + ").  Moving app to top of browser to prevent any security tampering.");
			top.location.href = window.location.href;
		}
	} catch (err) {
		// This means the domains are different because the browser is preventing access to the parent's domain.
		alert("Detected a frame not from the same domain (" + self.location.hostname + ").  Moving app to top of browser to prevent any security tampering.");
		top.location.href = window.location.href;
	}

	// We don't support IE6 at the moment, so let's just inform customers it won't work
	var IE6 = false /*@cc_on || @_jscript_version < 5.7 @*/;
	var gteIE7 = false /*@cc_on || @_jscript_version >= 5.7 @*/;

	// Disable IE6 browsers as UI does not support it
	if (IE6 == true) {
		alert("Only IE7, IE8, FireFox 3.x, Chrome, and Safari browsers are supported at this time.");
		return;
	}
	
	initializeTestTool();
	
	// We will be dropping all the main tab content into this container
	mainContainer = $("#maincontentarea");

	// Tab Links, dashboard is the initial active tab
	mainContainer.load("jsp/tab_dashboard.jsp");
	
	// Default AJAX Setup
	$.ajaxSetup({
		url: "/client/api",
		dataType: "json",
		cache: false,
		error: function(XMLHttpResponse) {
			handleError(XMLHttpResponse);
		},
		beforeSend: function(XMLHttpRequest) {
			if (g_mySession == $.cookie("JSESSIONID")) {
				return true;
			} else {
				$("#dialog_session_expired").dialog("open");
				return false;
			}
		}		
	});
	
	// LOGIN/LOGOUT
	// 'Enter' Key in any login form element = Submit click
	$("#logoutpage #loginForm").keypress(function(event) {
		var formId = $(event.target).attr("id");
		if(event.keyCode == keycode_Enter && formId != "loginbutton") {
			login();
		}
	});
	
	$("#logoutpage .loginbutton").bind("click", function(event) {
		login();
		return false;
	});
	
	$("#logoutaccount_link").bind("click", function(event) {
		$.ajax({
			data: "command=logout&response=json",
			dataType: "json",
			success: function(json) {
				logout(true);
			},
			error: function() {
				logout(true);
			},
			beforeSend : function(XMLHTTP) {
				return true;
			}
		});
	});
	
	// FUNCTION: logs the user out
	var activeTab = null;
	function logout(refresh) {
		g_mySession = null;
		g_username = null;	
		g_account = null;
		g_domainid = null;	
		g_timezoneoffset = null;
		g_timezone = null;
		
		$.cookie('JSESSIONID', null);
		$.cookie('username', null);
		$.cookie('account', null);
		$.cookie('domainid', null);
		$.cookie('role', null);
		$.cookie('networktype', null); 
		$.cookie('timezoneoffset', null);
		$.cookie('timezone', null);
		
		$("body").stopTime();
		
		// default is to redisplay the login page
		if (onLogoutCallback()) {
			if (refresh) {
				location.replace('/client');
				return false;
			}
			$("#account_password").val("");
			$(".loginbutton_box p").hide();
			$("#logoutpage").show();
			$("body").css("background", "#4e4e4e url(images/logout_bg.gif) repeat-x top left");
			mainContainer.empty();
			$("#mainmaster").hide();
			$("#overlay_black").hide();
			
			var menuOnClass = "menutab_on";
			var menuOffClass = "menutab_off";
			var tab = null;
			if (isAdmin()) {
				tab = $("#menutab_dashboard_root");
				menuOnClass = "admin_menutab_on";
				menuOffClass = "admin_menutab_off";
			} else if (isDomainAdmin()) {
				tab = $("#menutab_dashboard_domain");
				menuOnClass = "admin_menutab_on";
				menuOffClass = "admin_menutab_off";
			} else if (isUser()) {
				tab = $("#menutab_dashboard_user");
				menuOnClass = "menutab_on";
				menuOffClass = "menutab_off";
			}
			if (activeTab != null) {
				activeTab.removeClass(menuOnClass).addClass(menuOffClass);
				activeTab = null;
			}
			if (tab != null) {
				tab.removeClass(menuOffClass).addClass(menuOnClass);
			}
			g_role = null;
			$("#account_username").focus();
		}
	}
	
	// FUNCTION: logs the user in
	function login() {
		var array1 = [];
		var username = encodeURIComponent($("#account_username").val());
		array1.push("&username="+username);
		
		var password = $.md5(encodeURIComponent($("#account_password").val()));
		array1.push("&password="+password);
		
		var domain = encodeURIComponent($("#account_domain").val());
		if(domain != null && domain.length > 0)
		    array1.push("&domain="+domain);
		
		$.ajax({
			type: "POST",
			data: "command=login&response=json" + array1.join(""),
			dataType: "json",
			async: false,
			success: function(json) {
				g_mySession = $.cookie('JSESSIONID');
				g_role = json.loginresponse.type;
				g_username = json.loginresponse.username;	
				g_account = json.loginresponse.account;
				g_domainid = json.loginresponse.domainid;	
				g_timezone = json.loginresponse.timezone;								
				g_timezoneoffset = json.loginresponse.timezoneoffset;					
				if (json.loginresponse.networktype != null) 
					g_networkType = json.loginresponse.networktype;				
				if (json.loginresponse.hypervisortype != null) 
					g_hypervisorType = json.loginresponse.hypervisortype;				
				if (json.loginresponse.directattachnetworkgroupsenabled != null) 
					g_directattachnetworkgroupsenabled = json.loginresponse.directattachnetworkgroupsenabled;
				if (json.loginresponse.directattacheduntaggedenabled != null) 
					g_directAttachedUntaggedEnabled = json.loginresponse.directattacheduntaggedenabled;

				$.cookie('networktype', g_networkType, { expires: 1});
				$.cookie('hypervisortype', g_hypervisorType, { expires: 1});
				$.cookie('username', g_username, { expires: 1});	
				$.cookie('account', g_account, { expires: 1});	
				$.cookie('domainid', g_domainid, { expires: 1});				
				$.cookie('role', g_role, { expires: 1});
				$.cookie('timezoneoffset', g_timezoneoffset, { expires: 1});  
				$.cookie('timezone', g_timezone, { expires: 1});  
				$.cookie('directattachnetworkgroupsenabled', g_directattachnetworkgroupsenabled, { expires: 1}); 
				$.cookie('directattacheduntaggedenabled', g_directAttachedUntaggedEnabled, { expires: 1}); 
				
				// Set Role
				if (isUser()) {
					$(".loginbutton_box p").text("").hide();			
					$("#menutab_role_user #menutab_dashboard_user").click();
				} else if (isAdmin()) {
					$(".loginbutton_box p").text("").hide();			
					$("#menutab_role_root #menutab_dashboard_root").click();
				} else if (isDomainAdmin()) {
					$(".loginbutton_box p").text("").hide();			
					$("#menutab_role_domain #menutab_dashboard_domain").click();
				} else {
				    $(".loginbutton_box p").text("Account type of '" + username + "' is neither user nor admin.").show();
				    return;
				}				
				
				$("#logoutpage").hide();
				$("body").css("background", "#FFF repeat top left");
				$("#mainmaster").show();	
			},
			error: function() {
				$("#account_password").val("");
				$("#logoutpage").show();				
				$(".loginbutton_box p").text("Your username/password does not match our records.").show();
				$("#account_username").focus();
			},
			beforeSend: function(XMLHttpRequest) {
				return true;
			}
		});
	}
	
	// Dialogs
	$("#dialog_confirmation").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000
	});
	
	$("#dialog_info").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000,
		buttons: { "OK": function() { $(this).dialog("close"); } }
	});
	
	$("#dialog_alert").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000,
		buttons: { "OK": function() { $(this).dialog("close"); } }
	});
	$("#dialog_alert").siblings(".ui-widget-header").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	$("#dialog_alert").siblings(".ui-dialog-buttonpane").find(".ui-state-default").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	
	$("#dialog_error").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000,
		buttons: { "Close": function() { $(this).dialog("close"); } }
	});
	$("#dialog_error").siblings(".ui-widget-header").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	$("#dialog_error").siblings(".ui-dialog-buttonpane").find(".ui-state-default").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	
	$("#dialog_session_expired").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000,
		buttons: { "OK": function() { logout(true); $(this).dialog("close"); } }
	});
	$("#dialog_session_expired").siblings(".ui-widget-header").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	$("#dialog_session_expired").siblings(".ui-dialog-buttonpane").find(".ui-state-default").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	
	$("#dialog_server_error").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000,
		buttons: { "OK": function() { $(this).dialog("close"); } }
	});
	$("#dialog_server_error").siblings(".ui-widget-header").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	$("#dialog_server_error").siblings(".ui-dialog-buttonpane").find(".ui-state-default").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	
	// Menu Tabs
	$("#global_nav").bind("click", function(event) {
		var tab = $(event.target);
		var tabId = tab.attr("id");
		var menuOnClass = "menutab_on";
		var menuOffClass = "menutab_off";
		if (tabId == "menutab_dashboard_user" || tabId == "menutab_dashboard_root" || tabId == "menutab_dashboard_domain") {
			showDashboardTab();
		} else if (tabId == "menutab_vm") {
		    mainContainer.load("jsp/tab_instances.jsp", function() {
			    showInstancesTab(tab.data("domainId"), tab.data("account"));
			});		
		} else if (tabId == "menutab_networking") {
		    mainContainer.load("jsp/tab_networking.jsp", function() {		
			    showNetworkingTab(tab.data("domainId"), tab.data("account"));
			});
		} else if (tabId == "menutab_templates") {
		    mainContainer.load("jsp/tab_templates.jsp", function() {
			    showTemplatesTab();
			});
		} else if (tabId == "menutab_events") {
		    mainContainer.load("jsp/tab_events.jsp", function() {			   
			    showEventsTab(tab.data("showEvents"));
			});
		} else if (tabId == "menutab_hosts") {
		    mainContainer.load("jsp/tab_hosts.jsp", function() {	
			    showHostsTab();
			});
	    } else if (tabId == "menutab_storage") {
	        mainContainer.load("jsp/tab_storage.jsp", function() {		
			    showStorageTab(tab.data("domainId"), tab.data("targetTab"));
			});
		} else if (tabId == "menutab_accounts") {
		    mainContainer.load("jsp/tab_accounts.jsp", function() {	
			    showAccountsTab(tab.data("domainId"));
			});
		} else if (tabId == "menutab_domain") {
		    mainContainer.load("jsp/tab_domains.jsp", function() {	   
			    showDomainsTab();
			});
		} else if (tabId == "menutab_configuration") {
		    mainContainer.load("jsp/tab_configuration.jsp", function() {
			    showConfigurationTab();
			});
		} else {
			return false;
		}
		
		if (isAdmin() || isDomainAdmin()) {
			menuOnClass = "admin_menutab_on";
			menuOffClass = "admin_menutab_off";
		} else if (isUser()) {
			menuOnClass = "menutab_on";
			menuOffClass = "menutab_off";
		}
		if (activeTab != null) {
			activeTab.removeClass(menuOnClass).addClass(menuOffClass); 
		}
		tab.removeClass(menuOffClass).addClass(menuOnClass);
		activeTab = tab;
		removeDialogs();
		return false;
	});
	
	// Dashboard Tab
	function showDashboardTab() {
		mainContainer.load("jsp/tab_dashboard.jsp", function() {
			$(".header_topright #header_username").text($.cookie("username"));
			
			if (isAdmin()) {
				var sessionExpired = false;
				var zones = null;
				var noZones = false;
				var noPods = true;
				$("#menutab_dashboard_root, #menutab_vm, #menutab_networking_old, #menutab_networking, #menutab_templates, #menutab_events, #menutab_hosts, #menutab_storage, #menutab_accounts, #menutab_domain").hide();							
				$.ajax({
					data: "command=listZones&available=true&response=json",
					dataType: "json",
					async: false,
					success: function(json) {
						zones = json.listzonesresponse.zone;
						var zoneSelect = $("#capacity_zone_select").empty();	
						if (zones != null && zones.length > 0) {
							for (var i = 0; i < zones.length; i++) {
								zoneSelect.append("<option value='" + zones[i].id + "'>" + fromdb(zones[i].name) + "</option>"); 								
								if(noPods) {
								    $.ajax({
						                data: "command=listPods&zoneId="+zones[i].id+"&response=json",
						                dataType: "json",
						                async: false,
						                success: function(json) {
							                var pods = json.listpodsresponse.pod;						
							                if (pods != null && pods.length > 0) {
            							        noPods = false;
            							        $("#menutab_dashboard_root, #menutab_vm, #menutab_networking_old, #menutab_networking, #menutab_templates, #menutab_events, #menutab_hosts, #menutab_storage, #menutab_accounts, #menutab_domain").show();							
							                }							
						                }
					                });
								}
							}
						} else {							
							noZones = true;
						}
					},					
					beforeSend: function(XMLHttpRequest) {
						return true;
					}	
				});
				if (sessionExpired) return false;
				if (noZones || noPods) {
					$("#tab_dashboard_user").hide();
					$("#menutab_role_user").hide();
					$("#menutab_role_root").show();
					$("#menutab_configuration").click();
					return false;
				}
				
				var capacities = null;
				$.ajax({
					cache: false,
					async: false,
					data: "command=listCapacity&response=json",
					dataType: "json",
					success: function(json) {
						capacities = json.listcapacityresponse.capacity;
					}
				});
				
				$("#capacity_pod_select").bind("change", function(event) {
					// Reset to Defaults
					$("#public_ip_total, #storage_total, #storage_alloc_total, #sec_storage_total, #memory_total, #cpu_total, #private_ip_total").text("N/A");
					$("#public_ip_used, #storage_used, #storage_alloc, #sec_storage_used, #memory_used, #cpu_used, #private_ip_used,").attr("style", "width:50%").text("N/A");
					$(".db_bargraph_barbox_safezone").attr("style", "width:0%");
					$(".db_bargraph_barbox_unsafezone").attr("style", "width:0%");
					
					var selectedZone = $("#capacity_zone_select option:selected").text();
					var selectedPod = $("#capacity_pod_select").val();
					
					if (capacities != null && capacities.length > 0) {
						for (var i = 0; i < capacities.length; i++) {
							var capacity = capacities[i];
							if (capacity.zonename == selectedZone) {
								// Public IPs
								if (capacity.type == "4") {
									$("#public_ip_used").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + capacity.capacityused + " / " + capacity.percentused + "%");
									$("#public_ip_total").text("Total: " + capacity.capacitytotal);
									var usedPercentage = parseInt(capacity.percentused);
									if (usedPercentage > 70) {
										$("#capacity_public_ip .db_bargraph_barbox_safezone").attr("style", "width:70%");
										if(usedPercentage <= 100) 										
										    $("#capacity_public_ip .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
										else 
										    $("#capacity_public_ip .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
									} else {
										$("#capacity_public_ip .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
									    $("#capacity_public_ip .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
									}								
								// Secondary Storage
								} else if (capacity.type == "6") {
									$("#sec_storage_used").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + convertBytes(parseInt(capacity.capacityused)) + " / " + capacity.percentused + "%");
									$("#sec_storage_total").text("Total: " + convertBytes(parseInt(capacity.capacitytotal)));
									var usedPercentage = parseInt(capacity.percentused);
									if (usedPercentage > 70) {
										$("#capacity_sec_storage .db_bargraph_barbox_safezone").attr("style", "width:70%");
										if(usedPercentage <= 100) 
										    $("#capacity_sec_storage .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
										else
										    $("#capacity_sec_storage .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
									} else {
										$("#capacity_sec_storage .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
									    $("#capacity_sec_storage .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
									}
								} else {
									if (capacity.podname == selectedPod) {
										// Memory
										if (capacity.type == "0") {
											$("#memory_used").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + convertBytes(parseInt(capacity.capacityused)) + " / " + capacity.percentused + "%");
											$("#memory_total").text("Total: " + convertBytes(parseInt(capacity.capacitytotal)));
											var usedPercentage = parseInt(capacity.percentused);
											if (usedPercentage > 70) {
												$("#capacity_memory .db_bargraph_barbox_safezone").attr("style", "width:70%");
												if(usedPercentage <= 100) 
												    $("#capacity_memory .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
												else
												    $("#capacity_memory .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
											} else {
												$("#capacity_memory .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
											    $("#capacity_memory .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
											}
										// CPU
										} else if (capacity.type == "1") {
											$("#cpu_used").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + convertHz(parseInt(capacity.capacityused)) + " / " + capacity.percentused + "%");
											$("#cpu_total").text("Total: " + convertHz(parseInt(capacity.capacitytotal)));
											var usedPercentage = parseInt(capacity.percentused);
											if (usedPercentage > 70) {
												$("#capacity_cpu .db_bargraph_barbox_safezone").attr("style", "width:70%");
												if(usedPercentage <= 100) 
												    $("#capacity_cpu .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
												else
												    $("#capacity_cpu .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
											} else {
												$("#capacity_cpu .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
											    $("#capacity_cpu .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
											}
										// Storage Used
										} else if (capacity.type == "2") {
											$("#storage_used").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + convertBytes(parseInt(capacity.capacityused)) + " / " + capacity.percentused + "%");
											$("#storage_total").text("Total: " + convertBytes(parseInt(capacity.capacitytotal)));
											var usedPercentage = parseInt(capacity.percentused);									
											if (usedPercentage > 70) {
												$("#capacity_storage .db_bargraph_barbox_safezone").attr("style", "width:70%");
												if(usedPercentage <= 100) 
												    $("#capacity_storage .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
												else
												    $("#capacity_storage .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
											} else {
												$("#capacity_storage .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
											    $("#capacity_storage .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
											}
										// Storage Allocated
										} else if (capacity.type == "3") {
											$("#storage_alloc").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + convertBytes(parseInt(capacity.capacityused)) + " / " + capacity.percentused + "%");
											$("#storage_alloc_total").text("Total: " + convertBytes(parseInt(capacity.capacitytotal)));
											var usedPercentage = parseInt(capacity.percentused);
											if (usedPercentage > 70) {
												$("#capacity_storage_alloc .db_bargraph_barbox_safezone").attr("style", "width:70%");
												if(usedPercentage <= 100) 
												    $("#capacity_storage_alloc .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
												else
												    $("#capacity_storage_alloc .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
											} else {
												$("#capacity_storage_alloc .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
											    $("#capacity_storage_alloc .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
											}
										// Private IPs
										} else if (capacity.type == "5") {
											$("#private_ip_used").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + capacity.capacityused + " / " + capacity.percentused + "%");
											$("#private_ip_total").text("Total: " + capacity.capacitytotal);
											var usedPercentage = parseInt(capacity.percentused);
											if (usedPercentage > 70) {
												$("#capacity_private_ip .db_bargraph_barbox_safezone").attr("style", "width:70%");
												if(usedPercentage <= 100) 
												    $("#capacity_private_ip .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
												else
												    $("#capacity_private_ip .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
											} else {
												$("#capacity_private_ip .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
											    $("#capacity_private_ip .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
											}
										}
									}
								}
							}
						}
					}
				});
				
				$("#capacity_zone_select").bind("change", function(event) {
					var zoneId = $(this).val();
					$.ajax({
						data: "command=listPods&zoneId="+zoneId+"&response=json",
						dataType: "json",
						async: false,
						success: function(json) {
							var pods = json.listpodsresponse.pod;
							var podSelect = $("#capacity_pod_select").empty();	
							if (pods != null && pods.length > 0) {
								podSelect.append("<option value='All'>All</option>"); 
							    for (var i = 0; i < pods.length; i++) {
								    podSelect.append("<option value='" + pods[i].name + "'>" + fromdb(pods[i].name) + "</option>"); 
							    }
							}
							$("#capacity_pod_select").change();
						}
					});
				});
				$("#capacity_zone_select").change();
				
				// Show Recent Alerts
				$.ajax({
					data: "command=listAlerts&response=json",
					dataType: "json",
					success: function(json) {
						var alerts = json.listalertsresponse.alert;
						if (alerts != null && alerts.length > 0) {
							var alertGrid = $("#alert_grid_content").empty();
							var length = (alerts.length>=5) ? 5 : alerts.length;
							for (var i = 0; i < length; i++) {
								var errorTemplate = $("#recent_error_template").clone(true);
								errorTemplate.find("#db_error_type").text(toAlertType(alerts[i].type));
								errorTemplate.find("#db_error_msg").append(fromdb(alerts[i].description));											
								setDateField(alerts[i].sent, errorTemplate.find("#db_error_date"));															
								alertGrid.append(errorTemplate.show());
							}
						}
					}
				});
				
				// Show Host Alerts
				$.ajax({
					data: "command=listHosts&state=Alert&response=json",
					dataType: "json",
					success: function(json) {
						var alerts = json.listhostsresponse.host;
						if (alerts != null && alerts.length > 0) {
							var alertGrid = $("#host_alert_grid_content").empty();
							var length = (alerts.length>=4) ? 4 : alerts.length;
							for (var i = 0; i < length; i++) {
								var errorTemplate = $("#recent_error_template").clone(true);
								errorTemplate.find("#db_error_type").text("Host - Alert State");
								errorTemplate.find("#db_error_msg").append("Host - <b>" + fromdb(alerts[i].name) + "</b> has been detected in Alert state.");								
								setDateField(alerts[i].disconnected, errorTemplate.find("#db_error_date"));											
								alertGrid.append(errorTemplate.show());
							}
						}
					}
				});
				
				$("#alert_more").bind("click", function(event) {
					event.preventDefault();
					
					$("#menutab_role_root #menutab_events").data("showEvents", false).click();
				});
				$("#host_alert_more").bind("click", function(event) {
					event.preventDefault();
					$("#menutab_hosts").click();
				});
				
				$("#tab_dashboard_user, #tab_dashboard_domain, #loading_gridtable").hide();
				$("#tab_dashboard_root").show();
				$("#menutab_role_user").hide();
				$("#menutab_role_root").show();
				$("#menutab_role_domain").hide();
				$("#launch_test").show();
			} else if (isDomainAdmin()) {
				var thisTab = $("#tab_dashboard_domain");
				$("#tab_dashboard_user, #tab_dashboard_root, #loading_gridtable").hide();
				thisTab.show();
				$("#menutab_role_user").hide();
				$("#menutab_role_root").hide();
				$("#menutab_role_domain").show();
				$("#launch_test").hide();
				
				// Need to use/create better API for this as there is a limit of pageSize
				// to list count.
				$.ajax({
					data: "command=listVirtualMachines&pageSize=500&response=json",
					dataType: "json",
					success: function(json) {
						if (json.listvirtualmachinesresponse.virtualmachine != undefined)
							thisTab.find("#dashboard_instances").text(json.listvirtualmachinesresponse.virtualmachine.length);
					}
				});
				$.ajax({
					data: "command=listVolumes&pageSize=500&response=json",
					dataType: "json",
					success: function(json) {
						if (json.listvolumesresponse.volume)
							thisTab.find("#dashboard_volumes").text(json.listvolumesresponse.volume.length);
					}
				});
				$.ajax({
					data: "command=listSnapshots&pageSize=500&response=json",
					dataType: "json",
					success: function(json) {
						if (json.listsnapshotsresponse.snapshot)
							thisTab.find("#dashboard_snapshots").text(json.listsnapshotsresponse.snapshot.length);
					}
				});
				$.ajax({
					data: "command=listAccounts&pageSize=500&response=json",
					dataType: "json",
					success: function(json) {
						if (json.listaccountsresponse.account)
							thisTab.find("#dashboard_accounts").text(json.listaccountsresponse.account.length);
					}
				});
				$.ajax({
					data: "command=listEvents&level=ERROR&response=json",
					dataType: "json",
					success: function(json) {
						var events = json.listeventsresponse.event;
						if (events != null && events.length > 0) {
							var errorGrid = thisTab.find("#error_grid_content").empty();
							var length = (events.length>=3) ? 3 : events.length;
							for (var i = 0; i < length; i++) {
								var errorTemplate = $("#recent_error_template").clone(true);
								errorTemplate.find("#db_error_type").text(events[i].type);
								errorTemplate.find("#db_error_msg").text(fromdb(events[i].description));								
								setDateField(events[i].created, errorTemplate.find("#db_error_date"));																
								errorGrid.append(errorTemplate.show());
							}
						}
					}
				});
			} else if(isUser()) {			    
			    $("#launch_test").hide();
				$.ajax({
					cache: false,
					data: "command=listAccounts&response=json",
					dataType: "json",
					success: function(json) {
					    var accounts = json.listaccountsresponse.account;						
						if (accounts != null && accounts.length > 0) {
						    var statJSON = accounts[0];
						    var sent = parseInt(statJSON.sentbytes);
						    var rec = parseInt(statJSON.receivedbytes);
    						
    						if(sent==0 && rec==0)
    						    $("#network_bandwidth_panel").hide();
    						else
    						    $("#network_bandwidth_panel").show();
    						
						    $("#menutab_role_user").show();
						    $("#menutab_role_root").hide();
							$("#menutab_role_domain").hide();
						    $("#tab_dashboard_user").show();
						    $("#tab_dashboard_root, #tab_dashboard_domain, #loading_gridtable").hide();
							
						    // This is in bytes, so let's change to KB
						    sent = Math.round(sent / 1024);
						    rec = Math.round(rec / 1024);
						    $("#db_sent").text(sent + "KB");
						    $("#db_received").text(rec + "KB");
						    $("#db_available_public_ips").text(statJSON.ipavailable);
						    $("#db_owned_public_ips").text(statJSON.iptotal);
						    $("#db_running_vms").text(statJSON.vmrunning + " VM(s)");
						    $("#db_stopped_vms").text(statJSON.vmstopped + " VM(s)");
						    $("#db_total_vms").text(statJSON.vmtotal + " VM(s)");
						    $("#db_avail_vms").text(statJSON.vmavailable + " VM(s)");						   
						    $("#db_account_id").text(statJSON.id);
						    $("#db_account").text(statJSON.name);						    
						    $("#db_type").text(toRole(statJSON.accounttype));
						    $("#db_domain").text(statJSON.domain);						    			   
						}
						
						// Events
						$.ajax({
							data: "command=listEvents&level=ERROR&response=json",
							dataType: "json",
							success: function(json) {
								var events = json.listeventsresponse.event;
								if (events != null && events.length > 0) {
									var errorGrid = $("#error_grid_content").empty();
									var length = (events.length>=3) ? 3 : events.length;
									for (var i = 0; i < length; i++) {
										var errorTemplate = $("#recent_error_template").clone(true);
										errorTemplate.find("#db_error_type").text(events[i].type);
										errorTemplate.find("#db_error_msg").text(fromdb(events[i].description));										
										setDateField(events[i].created, errorTemplate.find("#db_error_date"));									
										errorGrid.append(errorTemplate.show());
									}
								}
							}
						});
					},					
					beforeSend: function(XMLHttpRequest) {
						return true;
					}	
				});
			} else { //no role 
			    logout(false);
			    return;
			}
		});
	}

	// Check whether the session is valid.
	g_mySession = $.cookie("JSESSIONID");
	g_role = $.cookie("role");
	g_username = $.cookie("username");
	g_account = $.cookie("account");
	g_domainid = $.cookie("domainid");
	g_networkType = $.cookie("networktype");
	g_hypervisorType = $.cookie("hypervisortype");
	g_timezone = $.cookie("timezone");
	g_directattachnetworkgroupsenabled = $.cookie("directattachnetworkgroupsenabled");
	g_directAttachedUntaggedEnabled = $.cookie("directattacheduntaggedenabled");
	
	if($.cookie("timezoneoffset") != null)
	    g_timezoneoffset = isNaN($.cookie("timezoneoffset"))?null: parseFloat($.cookie("timezoneoffset"));
	else
	    g_timezoneoffset = null;
	    
	if (!g_networkType || g_networkType.length == 0) 		
		g_networkType = "vnet";
	
	if (!g_hypervisorType || g_hypervisorType.length == 0) 		
		g_hypervisorType = "kvm";
	
	if (!g_directattachnetworkgroupsenabled || g_directattachnetworkgroupsenabled.length == 0) 		
		g_directattachnetworkgroupsenabled = "false";	
		
	if (!g_directAttachedUntaggedEnabled || g_directAttachedUntaggedEnabled.length == 0) 		
		g_directAttachedUntaggedEnabled = "false";		
		
	$.ajax({
		data: "command=listZones&available=true&response=json",
		dataType: "json",
		async: false,
		success: function(json) {
			// session is valid, continue
			if (isUser()) {
				$("#menutab_role_user #menutab_dashboard_user").click();
			} else if (isAdmin()) {
				$("#menutab_role_root #menutab_dashboard_root").click();
			} else if (isDomainAdmin()) {
				$("#menutab_role_domain #menutab_dashboard_domain").click();
			} else {
				logout(false);
			}
		},
		error: function(xmlHTTP) {
			logout(false);
		},
		beforeSend: function(xmlHTTP) {
			return true;
		}
	});
});
