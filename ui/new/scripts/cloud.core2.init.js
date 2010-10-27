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

$(document).ready(function() { 
	$(".leftmenu_content_flevel").hover(
		function() {
			$(this).find(".leftmenu_arrows_firstlevel_open").show();
		},
		function() {
			if ($selectedLeftMenu.attr("id") != $(this).attr("id")) {
				$(this).find(".leftmenu_arrows_firstlevel_open").hide();
			}
		}
	);

	// Setup first level navigation
	$("#leftmenu_dashboard").bind("click", function(event) {
		selectLeftMenu($(this));
		hideMiddleMenu();
		$("#right_panel").load("jsp/dashboard.jsp", function(){
			afterLoadDashboardJSP();        
		});
		return false;
	});	
	$("#leftmenu_storage").bind("click", function(event) {
		selectLeftMenu($(this), true);
		return false;
	});
	$("#leftmenu_network").bind("click", function(event) {
		selectLeftMenu($(this), true);
		return false;
	});
	$("#leftmenu_templates").bind("click", function(event) {
		selectLeftMenu($(this), true);
		return false;
	});
	$("#leftmenu_account").bind("click", function(event) {
		selectLeftMenu($(this));
		listMidMenuItems("listAccounts", "listaccountsresponse", "account", "jsp/account.jsp", afterLoadAccountJSP, accountToMidmenu, accountToRightPanel, getMidmenuId, false);
		return false;
	});	
	$("#leftmenu_events").bind("click", function(event) {
		selectLeftMenu($(this), true);
		return false;
	});
	$("#leftmenu_system").bind("click", function(event) {
		selectLeftMenu($(this), true);	
		if($("#leftmenu_resource").find("#resource_arrow").hasClass("expanded_open") == true)
		    $("#leftmenu_resource").click(); //if resource menu is open (i.e. zonetree is shown), empty zonetree and close resource menu.
		return false;
	});
	
	// Setup 2nd level navigation
	function buildSecondLevelNavigation() {
	
		// Instance sub menus
		bindAndListMidMenuItems($("#leftmenu_instances_my_instances"), "listVirtualMachines&domainid="+g_domainid+"&account="+g_account, "listvirtualmachinesresponse", "virtualmachine", "jsp/instance.jsp", afterLoadInstanceJSP, vmToMidmenu, vmToRightPanel, getMidmenuId, true);
		bindAndListMidMenuItems($("#leftmenu_instances_all_instances"), "listVirtualMachines", "listvirtualmachinesresponse", "virtualmachine", "jsp/instance.jsp", afterLoadInstanceJSP, vmToMidmenu, vmToRightPanel, getMidmenuId, true);
		bindAndListMidMenuItems($("#leftmenu_instances_running_instances"), "listVirtualMachines&state=Running", "listvirtualmachinesresponse", "virtualmachine", "jsp/instance.jsp", afterLoadInstanceJSP, vmToMidmenu, vmToRightPanel, getMidmenuId, true);
		bindAndListMidMenuItems($("#leftmenu_instances_stopped_instances"), "listVirtualMachines&state=Stopped", "listvirtualmachinesresponse", "virtualmachine", "jsp/instance.jsp", afterLoadInstanceJSP, vmToMidmenu, vmToRightPanel, getMidmenuId, true);
		bindAndListMidMenuItems($("#leftmenu_instances_destroyed_instances"), "listVirtualMachines&state=Destroyed", "listvirtualmachinesresponse", "virtualmachine", "jsp/instance.jsp", afterLoadInstanceJSP, vmToMidmenu, vmToRightPanel, getMidmenuId, true);
		
		bindAndListMidMenuItems($("#leftmenu_event"), "listEvents", "listeventsresponse", "event", "jsp/event.jsp", afterLoadEventJSP, eventToMidmenu, eventToRightPanel, getMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_alert"), "listAlerts", "listalertsresponse", "alert", "jsp/alert.jsp", afterLoadAlertJSP, alertToMidmenu, alertToRightPanel, getMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_volume"), "listVolumes", "listvolumesresponse", "volume", "jsp/volume.jsp", afterLoadVolumeJSP, volumeToMidmenu, volumeToRightPanel, getMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_snapshot"), "listSnapshots", "listsnapshotsresponse", "snapshot", "jsp/snapshot.jsp", afterLoadSnapshotJSP, snapshotToMidmenu, snapshotToRightPanel, getMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_ip"), "listPublicIpAddresses", "listpublicipaddressesresponse", "publicipaddress", "jsp/ipaddress.jsp", afterLoadIpJSP, ipToMidmenu, ipToRightPanel, ipGetMidmenuId, false);
		//bindAndListMidMenuItems("leftmenu_router", "listRouters", "listroutersresponse", "router", "jsp/router.jsp", afterLoadRouterJSP, routerToMidmenu, routerToRightPanel, getMidmenuId, false);
		  
		bindAndListMidMenuItems($("#leftmenu_submenu_my_template"), "listTemplates&templatefilter=self", "listtemplatesresponse", "template", "jsp/template.jsp", afterLoadTemplateJSP, templateToMidmenu, templateToRightPanel, templateGetMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_submenu_featured_template"), "listTemplates&templatefilter=featured", "listtemplatesresponse", "template", "jsp/template.jsp", afterLoadTemplateJSP, templateToMidmenu, templateToRightPanel, templateGetMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_submenu_community_template"), "listTemplates&templatefilter=community", "listtemplatesresponse", "template", "jsp/template.jsp", afterLoadTemplateJSP, templateToMidmenu, templateToRightPanel, templateGetMidmenuId, false);
		
		bindAndListMidMenuItems($("#leftmenu_submenu_my_iso"), "listIsos&isofilter=self", "listisosresponse", "iso", "jsp/iso.jsp", afterLoadIsoJSP, isoToMidmenu, isoToRightPanel, isoGetMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_submenu_featured_iso"), "listIsos&isofilter=featured", "listisosresponse", "iso", "jsp/iso.jsp", afterLoadIsoJSP, isoToMidmenu, isoToRightPanel, isoGetMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_submenu_community_iso"), "listIsos&isofilter=community", "listisosresponse", "iso", "jsp/iso.jsp", afterLoadIsoJSP, isoToMidmenu, isoToRightPanel, isoGetMidmenuId, false);
		
		bindAndListMidMenuItems($("#leftmenu_service_offering"), "listServiceOfferings", "listserviceofferingsresponse", "serviceoffering", "jsp/serviceoffering.jsp", afterLoadServiceOfferingJSP, serviceOfferingToMidmenu, serviceOfferingToRightPanel, getMidmenuId, false); 
		bindAndListMidMenuItems($("#leftmenu_disk_offering"), "listDiskOfferings", "listdiskofferingsresponse", "diskoffering", "jsp/diskoffering.jsp", afterLoadDiskOfferingJSP, diskOfferingToMidmenu, diskOfferingToRightPanel, getMidmenuId, false); 
		bindAndListMidMenuItems($("#leftmenu_global_setting"), "listConfigurations", "listconfigurationsresponse", "configuration", "jsp/globalsetting.jsp", afterLoadGlobalSettingJSP, globalSettingToMidmenu, globalSettingToRightPanel, globalSettingGetMidmenuId, false); 
		
		$("#leftmenu_instances").bind("click", function(event) {
			instanceBuildSubMenu();
			selectLeftMenu($(this), true);		
			return false;
		});	
			
		$("#leftmenu_domain").bind("click", function(event) {
			selectLeftMenu($(this), true);
			hideMiddleMenu();		
			disableMultipleSelectionInMidMenu();      
			clearMiddleMenu();
					
			bindEventHandlerToDomainTreeNode();		
			refreshWholeTree(g_domainid, defaultRootLevel); 
					
			return false;
		});  
			
		$("#leftmenu_resource").bind("click", function(event) {
			showMiddleMenu();
			disableMultipleSelectionInMidMenu();  
			clearMiddleMenu();
		   
			$arrowIcon = $(this).find("#resource_arrow");
			if($arrowIcon.hasClass("expanded_close") == true) {
				$arrowIcon.removeClass("expanded_close").addClass("expanded_open");
				buildZoneTree();
			} else {
				$arrowIcon.removeClass("expanded_open").addClass("expanded_close");
				$("#leftmenu_zone_tree").find("#tree_container").empty();
			}
			
			showPage($("#resource_page"), null);			
			return false;
		});
	}
               
    $("#midmenu_action_link").bind("mouseover", function(event) {
        $(this).find("#action_menu").show();    
        return false;
    });
    $("#midmenu_action_link").bind("mouseout", function(event) {
        $(this).find("#action_menu").hide();    
        return false;
    });
    
    
    
    
   
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
	
	// Initialize help drop down dialog
	$("#help_link").bind("click", function(event) {
		$("#help_dropdown_dialog").show();
		$("#help_button").addClass("selected");
		return false;
	});
	
	$("#help_dropdown_close").bind("click", function(event) {
		$("#help_dropdown_dialog").hide();
		$("#help_button").removeClass("selected");
		return false;
	});
	
	/*
	initializeTestTool();
	*/
		
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
	$("#login_wrapper #loginForm").keypress(function(event) {
		var formId = $(event.target).attr("id");
		if(event.keyCode == keycode_Enter && formId != "loginbutton") {
			login();
		}
	});
	
	$("#login_wrapper #loginbutton").bind("click", function(event) {
		login();
		return false;
	});
	
	$("#main_logout").bind("click", function(event) {
		$.ajax({
		    data: createURL("command=logout&response=json"),
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
                g_sessionKey = null;
		g_username = null;	
		g_account = null;
		g_domainid = null;	
		g_timezoneoffset = null;
		g_timezone = null;
		
		$.cookie('JSESSIONID', null);
		$.cookie('sessionKey', null);
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
				location.replace('/client/new');
				return false;
			}
			$("#account_password").val("");
			$("#login_wrapper #login_error").hide();
			$("#login_wrapper").show();
			$("#main").hide();
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
		
		var domain = $("#account_domain").val();
		if(domain != null && domain.length > 0) {
			if (domain.charAt(0) != "/") {
				domain = "/" + domain;
			}
		    array1.push("&domain="+encodeURIComponent(domain));
		} else {
			array1.push("&domain="+encodeURIComponent("/"));
		}
		
		$.ajax({
			type: "POST",
		        data: createURL("command=login&response=json" + array1.join("")),
			dataType: "json",
			async: false,
			success: function(json) {
				g_mySession = $.cookie('JSESSIONID');
				g_sessionKey = encodeURIComponent(json.loginresponse.sessionkey);
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
					g_directAttachNetworkGroupsEnabled = json.loginresponse.directattachnetworkgroupsenabled;
				if (json.loginresponse.directattacheduntaggedenabled != null) 
					g_directAttachedUntaggedEnabled = json.loginresponse.directattacheduntaggedenabled;
                if (json.loginresponse.systemvmuselocalstorage != null) 
					g_systemVmUseLocalStorage = json.loginresponse.systemvmuselocalstorage;
					
				$.cookie('sessionKey', g_sessionKey, { expires: 1});
				$.cookie('networktype', g_networkType, { expires: 1});
				$.cookie('hypervisortype', g_hypervisorType, { expires: 1});
				$.cookie('username', g_username, { expires: 1});	
				$.cookie('account', g_account, { expires: 1});	
				$.cookie('domainid', g_domainid, { expires: 1});				
				$.cookie('role', g_role, { expires: 1});
				$.cookie('timezoneoffset', g_timezoneoffset, { expires: 1});  
				$.cookie('timezone', g_timezone, { expires: 1});  
				$.cookie('directattachnetworkgroupsenabled', g_directAttachNetworkGroupsEnabled, { expires: 1}); 
				$.cookie('directattacheduntaggedenabled', g_directAttachedUntaggedEnabled, { expires: 1}); 
				$.cookie('systemvmuselocalstorage', g_systemVmUseLocalStorage, { expires: 1}); 
				
				buildSecondLevelNavigation();
				
				$("#main_username").text(g_username);
				$("#login_wrapper").hide();
				$("#main").show();	
				$("#leftmenu_dashboard").click();
			},
			error: function() {
				$("#account_password").val("");
				$("#login_wrapper #login_error").show();
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
			
	$("#dialog_info_please_select_one_item_in_middle_menu").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000,
		buttons: { "OK": function() { $(this).dialog("close"); } }
	});
			
	// Check whether the session is valid.
	g_mySession = $.cookie("JSESSIONID");
	g_sessionKey = $.cookie("sessionKey");
	g_role = $.cookie("role");
	g_username = $.cookie("username");
	g_account = $.cookie("account");
	g_domainid = $.cookie("domainid");
	g_networkType = $.cookie("networktype");
	g_hypervisorType = $.cookie("hypervisortype");
	g_timezone = $.cookie("timezone");
	g_directAttachNetworkGroupsEnabled = $.cookie("directattachnetworkgroupsenabled");
	g_directAttachedUntaggedEnabled = $.cookie("directattacheduntaggedenabled");
	g_systemVmUseLocalStorage = $.cookie("systemvmuselocalstorage");
	
	if($.cookie("timezoneoffset") != null)
	    g_timezoneoffset = isNaN($.cookie("timezoneoffset"))?null: parseFloat($.cookie("timezoneoffset"));
	else
	    g_timezoneoffset = null;
	    
	if (!g_networkType || g_networkType.length == 0) 		
		g_networkType = "vnet";
	
	if (!g_hypervisorType || g_hypervisorType.length == 0) 		
		g_hypervisorType = "kvm";
	
	if (!g_directAttachNetworkGroupsEnabled || g_directAttachNetworkGroupsEnabled.length == 0) 		
		g_directAttachNetworkGroupsEnabled = "false";	
		
	if (!g_directAttachedUntaggedEnabled || g_directAttachedUntaggedEnabled.length == 0) 		
		g_directAttachedUntaggedEnabled = "false";		
	
	if (!g_systemVmUseLocalStorage || g_systemVmUseLocalStorage.length == 0) 		
		g_systemVmUseLocalStorage = "false";
		
	$.ajax({
	    data: createURL("command=listZones&available=true&response=json"),
		dataType: "json",
		async: false,
		success: function(json) {
			buildSecondLevelNavigation();
			$("#main_username").text(g_username);
			$("#leftmenu_dashboard").click();
			$("#main").show();
		},
		error: function(xmlHTTP) {
			logout(false);
		},
		beforeSend: function(xmlHTTP) {
			return true;
		}
	});
});

