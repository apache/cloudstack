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
	$("#leftmenu_configuration").bind("click", function(event) {
		selectLeftMenu($(this), true);		
		return false;
	});	
	
	$("#leftmenu_system").bind("click", function(event) {
		selectLeftMenu($(this), true);
		return false;
	});		
				
	$("#leftmenu_domain").bind("click", function(event) {
		selectLeftMenu($(this), true);	
		return false;
	});	
	$("#leftmenu_account").bind("click", function(event) {
		selectLeftMenu($(this), true);
		return false;
	});	
	
	
	$("#leftmenu_dashboard").bind("click", function(event) {
	    selectLeftMenu($(this), false, function() {
			clearMiddleMenu();
			hideMiddleMenu();
			
			$("#right_panel").data("onRefreshFn", function() {
				$("#leftmenu_dashboard").click();
			});
			
			$("#right_panel").load("jsp/dashboard.jsp", function(){
				afterLoadDashboardJSP();        
			});
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
	$("#leftmenu_events").bind("click", function(event) {
		selectLeftMenu($(this), true);
		return false;
	});
	
	$("#leftmenu_instances").bind("click", function(event) {
		instanceBuildSubMenu();
		selectLeftMenu($(this), true);		
		return false;
	});	
	
	
	// Setup 2nd level navigation
	function buildSecondLevelNavigation() {
	
		// Instance sub menus
		bindAndListMidMenuItems($("#leftmenu_instances_my_instances"), "listVirtualMachines&domainid="+g_domainid+"&account="+g_account, vmGetSearchParams, "listvirtualmachinesresponse", "virtualmachine", "jsp/instance.jsp", afterLoadInstanceJSP, vmToMidmenu, vmToRightPanel, getMidmenuId, true);
		bindAndListMidMenuItems($("#leftmenu_instances_all_instances"), "listVirtualMachines", vmGetSearchParams, "listvirtualmachinesresponse", "virtualmachine", "jsp/instance.jsp", afterLoadInstanceJSP, vmToMidmenu, vmToRightPanel, getMidmenuId, true);
		bindAndListMidMenuItems($("#leftmenu_instances_running_instances"), "listVirtualMachines&state=Running", vmGetSearchParams, "listvirtualmachinesresponse", "virtualmachine", "jsp/instance.jsp", afterLoadInstanceJSP, vmToMidmenu, vmToRightPanel, getMidmenuId, true);
		bindAndListMidMenuItems($("#leftmenu_instances_stopped_instances"), "listVirtualMachines&state=Stopped", vmGetSearchParams, "listvirtualmachinesresponse", "virtualmachine", "jsp/instance.jsp", afterLoadInstanceJSP, vmToMidmenu, vmToRightPanel, getMidmenuId, true);
		bindAndListMidMenuItems($("#leftmenu_instances_destroyed_instances"), "listVirtualMachines&state=Destroyed", vmGetSearchParams, "listvirtualmachinesresponse", "virtualmachine", "jsp/instance.jsp", afterLoadInstanceJSP, vmToMidmenu, vmToRightPanel, getMidmenuId, true);
		
		bindAndListMidMenuItems($("#leftmenu_event"), "listEvents", eventGetSearchParams, "listeventsresponse", "event", "jsp/event.jsp", afterLoadEventJSP, eventToMidmenu, eventToRightPanel, getMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_alert"), "listAlerts", alertGetSearchParams, "listalertsresponse", "alert", "jsp/alert.jsp", afterLoadAlertJSP, alertToMidmenu, alertToRightPanel, getMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_volume"), "listVolumes", volumeGetSearchParams, "listvolumesresponse", "volume", "jsp/volume.jsp", afterLoadVolumeJSP, volumeToMidmenu, volumeToRightPanel, getMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_snapshot"), "listSnapshots", snapshotGetSearchParams, "listsnapshotsresponse", "snapshot", "jsp/snapshot.jsp", afterLoadSnapshotJSP, snapshotToMidmenu, snapshotToRightPanel, getMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_ip"), "listPublicIpAddresses", ipGetSearchParams, "listpublicipaddressesresponse", "publicipaddress", "jsp/ipaddress.jsp", afterLoadIpJSP, ipToMidmenu, ipToRightPanel, ipGetMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_security_group"), "listNetworkGroups", securityGroupGetSearchParams, "listnetworkgroupsresponse", "securitygroup", "jsp/securitygroup.jsp", afterLoadSecurityGroupJSP, securityGroupToMidmenu, securityGroupToRightPanel, getMidmenuId, false);
					 		  
		bindAndListMidMenuItems($("#leftmenu_submenu_my_template"), "listTemplates&templatefilter=self", templateGetSearchParams, "listtemplatesresponse", "template", "jsp/template.jsp", afterLoadTemplateJSP, templateToMidmenu, templateToRightPanel, templateGetMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_submenu_featured_template"), "listTemplates&templatefilter=featured", templateGetSearchParams, "listtemplatesresponse", "template", "jsp/template.jsp", afterLoadTemplateJSP, templateToMidmenu, templateToRightPanel, templateGetMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_submenu_community_template"), "listTemplates&templatefilter=community", templateGetSearchParams, "listtemplatesresponse", "template", "jsp/template.jsp", afterLoadTemplateJSP, templateToMidmenu, templateToRightPanel, templateGetMidmenuId, false);
		
		bindAndListMidMenuItems($("#leftmenu_submenu_my_iso"), "listIsos&isofilter=self", isoGetSearchParams, "listisosresponse", "iso", "jsp/iso.jsp", afterLoadIsoJSP, isoToMidmenu, isoToRightPanel, isoGetMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_submenu_featured_iso"), "listIsos&isofilter=featured", isoGetSearchParams, "listisosresponse", "iso", "jsp/iso.jsp", afterLoadIsoJSP, isoToMidmenu, isoToRightPanel, isoGetMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_submenu_community_iso"), "listIsos&isofilter=community", isoGetSearchParams, "listisosresponse", "iso", "jsp/iso.jsp", afterLoadIsoJSP, isoToMidmenu, isoToRightPanel, isoGetMidmenuId, false);
		
		bindAndListMidMenuItems($("#leftmenu_account_my_accounts"), "listAccounts&domainid="+g_domainid+"&name="+g_account, accountGetSearchParams, "listaccountsresponse", "account", "jsp/account.jsp", afterLoadAccountJSP, accountToMidmenu, accountToRightPanel, getMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_account_all_accounts"), "listAccounts", accountGetSearchParams, "listaccountsresponse", "account", "jsp/account.jsp", afterLoadAccountJSP, accountToMidmenu, accountToRightPanel, getMidmenuId, false);
		
		bindAndListMidMenuItems($("#leftmenu_service_offering"), "listServiceOfferings", serviceOfferingGetSearchParams, "listserviceofferingsresponse", "serviceoffering", "jsp/serviceoffering.jsp", afterLoadServiceOfferingJSP, serviceOfferingToMidmenu, serviceOfferingToRightPanel, getMidmenuId, false); 
		bindAndListMidMenuItems($("#leftmenu_disk_offering"), "listDiskOfferings", diskOfferingGetSearchParams, "listdiskofferingsresponse", "diskoffering", "jsp/diskoffering.jsp", afterLoadDiskOfferingJSP, diskOfferingToMidmenu, diskOfferingToRightPanel, getMidmenuId, false); 
		
		bindAndListMidMenuItems($("#leftmenu_submenu_virtual_router"), "listRouters", routerGetSearchParams, "listroutersresponse", "router", "jsp/router.jsp", afterLoadRouterJSP, routerToMidmenu, routerToRightPanel, getMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_submenu_systemvm"), "listSystemVms", systemVmGetSearchParams, "listsystemvmsresponse", "systemvm", "jsp/systemvm.jsp", afterLoadSystemVmJSP, systemvmToMidmenu, systemvmToRightPanel, getMidmenuId, false);
		
		$("#leftmenu_global_setting").bind("click", function(event) {
		    selectLeftSubMenu($(this));		
		    hideMiddleMenu();	
	        clearMiddleMenu();
		    	
		    $("#right_panel").data("onRefreshFn", function() {
		        $("#leftmenu_global_setting").click();
		    });
		    		    
		    $("#right_panel").load("jsp/globalsetting.jsp", function(){     
		        var $actionLink = $("#right_panel_content #tab_content_details #action_link");
		        $actionLink.bind("mouseover", function(event) {	    
			        $(this).find("#action_menu").show();    
			        return false;
		        });
		        $actionLink.bind("mouseout", function(event) {       
			        $(this).find("#action_menu").hide();    
			        return false;
		        });	   
        					  
		        afterLoadGlobalSettingJSP();   
	        });    
		    
		    return false;
		});
		
		$("#leftmenu_physical_resource").bind("click", function(event) {
			showMiddleMenu();
			clearMiddleMenu();
		   
			expandOrCollapseZoneTree();
			
			$("#right_panel").data("onRefreshFn", function() {
		        $("#leftmenu_physical_resource").click();
		    });
			
			resourceLoadPage("jsp/resource.jsp", null);			
			return false;
		});
		
		//Setup domain
		bindEventHandlerToDomainTreeNode();			
		drawRootNode(g_domainid);
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
	
	//basic search	
	$("#basic_search").find("#search_input").unbind("keypress").bind("keypress", function(event) { 	 
	    event.stopPropagation();   
	    if(event.keyCode == keycode_Enter) { 
	        event.preventDefault();
	        var params = $("#middle_menu_pagination").data("params");
	        if(params == null)
	            return;	 
	        lastSearchType = "basic_search";       	    
	        listMidMenuItems2(params.commandString, params.getSearchParamsFn, params.jsonResponse1, params.jsonResponse2, params.toMidmenuFn, params.toRightPanelFn, params.getMidmenuIdFn, params.isMultipleSelectionInMidMenu, 1);
	    }		    
	});
	
	//advanced search	
	$("#advanced_search_icon").unbind("click").bind("click", function(event) {
	    var $advancedSearch = $("#advanced_search_template").clone().attr("id", "advanced_search_popup");
	    
	    $advancedSearch.unbind("click").bind("click", function(event) {
	        var $target = $(event.target);
	        var targetId = $target.attr("id");	        
	        if(targetId == "advanced_search_close") {
	            $(this).hide();
	            return false;
	        }
	        else if(targetId == "adv_search_button") {    	        
    	        var params = $("#middle_menu_pagination").data("params");
	            if(params == null)
	                return;	        	    
	            lastSearchType = "advanced_search";  
	            $("#basic_search").find("#search_input").val("");
	            listMidMenuItems2(params.commandString, params.getSearchParamsFn, params.jsonResponse1, params.jsonResponse2, params.toMidmenuFn, params.toRightPanelFn, params.getMidmenuIdFn, params.isMultipleSelectionInMidMenu, 1);
    	        $(this).hide();
	            return false;
	        }
	        return true;
	    });
	    	
	    $advancedSearch.unbind("keypress").bind("keypress", function(event) {	       
	        event.stopPropagation();   
	        if(event.keyCode == keycode_Enter) { 
	            event.preventDefault();
	            $(this).find("#adv_search_button").click();
	        }	
	    });	
	    	    
	    if(isAdmin() || isDomainAdmin())
	        $advancedSearch.find("#adv_search_domain_li, #adv_search_account_li, #adv_search_pod_li").show();
	    else
	        $advancedSearch.find("#adv_search_domain_li, #adv_search_account_li, #adv_search_pod_li").hide(); 
	    
        var zoneSelect = $advancedSearch.find("#adv_search_zone");	    
	    if(zoneSelect.length>0) {  //if zone dropdown is found on Advanced Search dialog 	    		
	        $.ajax({
		        data: createURL("command=listZones&available=true"),
		        dataType: "json",
		        success: function(json) {
			        var zones = json.listzonesresponse.zone;			   
			        zoneSelect.empty();					
			        zoneSelect.append("<option value=''></option>"); 
			        if (zones != null && zones.length > 0) {
			            for (var i = 0; i < zones.length; i++) {
				            zoneSelect.append("<option value='" + zones[i].id + "'>" + fromdb(zones[i].name) + "</option>"); 
			            }
			        }
		        }
	        });
    		
	        var podSelect = $advancedSearch.find("#adv_search_pod").empty();	
	        var podLabel = $advancedSearch.find("#adv_search_pod_label");
	        if(podSelect.length>0 && $advancedSearch.find("#adv_search_pod_li").css("display")!="none") {		        
	            zoneSelect.bind("change", function(event) { 	            
		            var zoneId = $(this).val();
		            if (zoneId == null || zoneId.length == 0) {			            
		                podLabel.css("color", "gray");	
		                podSelect.attr("disabled", "disabled");	 
		                podSelect.empty();	        
		            } else {		            
		                podLabel.css("color", "black");	
		                podSelect.removeAttr("disabled");
		                $.ajax({
				        data: createURL("command=listPods&zoneId="+zoneId+""),
			                dataType: "json",
			                async: false,
			                success: function(json) {
				                var pods = json.listpodsresponse.pod;	
				                podSelect.empty();			            
				                if (pods != null && pods.length > 0) {
				                    for (var i = 0; i < pods.length; i++) {
					                    podSelect.append("<option value='" + pods[i].id + "'>" + fromdb(pods[i].name) + "</option>"); 
				                    }
				                }
			                }
		                });
		            }
		            return false;		        
	            });		
    	        
	            zoneSelect.change();
	        }
	    }
    	
	    var domainSelect = $advancedSearch.find("#adv_search_domain");	
	    if(domainSelect.length>0 && $advancedSearch.find("#adv_search_domain_li").css("display")!="none") {
	        var domainSelect = domainSelect.empty();			
	        $.ajax({
		        data: createURL("command=listDomains&available=true"),
		        dataType: "json",
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
    	    	
	    var vmSelect = $advancedSearch.find("#adv_search_vm");	
	    if(vmSelect.length>0) {		   
	        vmSelect.empty();		
	        vmSelect.append("<option value=''></option>"); 	
	        $.ajax({
		        data: createURL("command=listVirtualMachines"),
		        dataType: "json",
		        success: function(json) {			        
			        var items = json.listvirtualmachinesresponse.virtualmachine;		 
			        if (items != null && items.length > 0) {
			            for (var i = 0; i < items.length; i++) {
				            vmSelect.append("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>"); 
			            }
			        }
		        }
	        });		    
	    } 	  
	    	      
	    $advancedSearch.find("#adv_search_startdate, #adv_search_enddate").datepicker({dateFormat: 'yy-mm-dd'});
	    	    	    
	    $("#advanced_search_container").empty().append($advancedSearch.show());	 
	    	   
	    return false;
	});
	
	//pagination
	$("#middle_menu_pagination").unbind("clik").bind("click", function(event) {	
	    var params = $(this).data("params");
	    if(params == null)
	        return;	    
	    
	    var $target = $(event.target);
	    var targetId = $target.attr("id");
	    
	    if(targetId == "midmenu_prevbutton") {
	        listMidMenuItems2(params.commandString, params.getSearchParamsFn, params.jsonResponse1, params.jsonResponse2, params.toMidmenuFn, params.toRightPanelFn, params.getMidmenuIdFn, params.isMultipleSelectionInMidMenu, (params.page-1));
	    }	        
	    else if(targetId == "midmenu_nextbutton") {	        
	        listMidMenuItems2(params.commandString, params.getSearchParamsFn, params.jsonResponse1, params.jsonResponse2, params.toMidmenuFn, params.toRightPanelFn, params.getMidmenuIdFn, params.isMultipleSelectionInMidMenu, (params.page+1));
	    }	
	    
	    return false;    
	});
	
	// refresh button
	$("#refresh_link").unbind("clik").bind("click", function(event) {		
		var onRefreshFn = $("#right_panel").data("onRefreshFn");
		if(onRefreshFn != null)
		    onRefreshFn();
		return false;
	});
	
	// Initialize help drop down dialog
	$("#help_link").unbind("clik").bind("click", function(event) {
		$("#help_dropdown_dialog").show();
		$("#help_button").addClass("selected");
		return false;
	});
	
	$("#help_dropdown_close").unbind("clik").bind("click", function(event) {
		$("#help_dropdown_dialog").hide();
		$("#help_button").removeClass("selected");
		return false;
	});
	
	initializeTestTool();
		
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
				location.replace('/client');
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
				if (json.loginresponse.hypervisortype != null) 
					g_hypervisorType = json.loginresponse.hypervisortype;				
				if (json.loginresponse.directattachnetworkgroupsenabled != null) 
					g_directAttachNetworkGroupsEnabled = json.loginresponse.directattachnetworkgroupsenabled;
				if (json.loginresponse.directattacheduntaggedenabled != null) 
					g_directAttachedUntaggedEnabled = json.loginresponse.directattacheduntaggedenabled;
                if (json.loginresponse.systemvmuselocalstorage != null) 
					g_systemVmUseLocalStorage = json.loginresponse.systemvmuselocalstorage;
					
				$.cookie('sessionKey', g_sessionKey, { expires: 1});
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
				showLeftNavigationBasedOnRole();				
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
	initDialog("dialog_confirmation", 350, false);	
	initDialogWithOK("dialog_info", 350, false);
	
	initDialogWithOK("dialog_alert", 350, false);
	$("#dialog_alert").siblings(".ui-widget-header").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	$("#dialog_alert").siblings(".ui-dialog-buttonpane").find(".ui-state-default").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	
	initDialogWithOK("dialog_error", 350, false);	
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
		
	initDialogWithOK("dialog_info_please_select_one_item_in_middle_menu", 350, false);		
				
	// Check whether the session is valid.
	g_mySession = $.cookie("JSESSIONID");
	g_sessionKey = $.cookie("sessionKey");
	g_role = $.cookie("role");
	g_username = $.cookie("username");
	g_account = $.cookie("account");
	g_domainid = $.cookie("domainid");
	g_hypervisorType = $.cookie("hypervisortype");
	g_timezone = $.cookie("timezone");
	g_directAttachNetworkGroupsEnabled = $.cookie("directattachnetworkgroupsenabled");
	g_directAttachedUntaggedEnabled = $.cookie("directattacheduntaggedenabled");
	g_systemVmUseLocalStorage = $.cookie("systemvmuselocalstorage");
	
	if($.cookie("timezoneoffset") != null)
	    g_timezoneoffset = isNaN($.cookie("timezoneoffset"))?null: parseFloat($.cookie("timezoneoffset"));
	else
	    g_timezoneoffset = null;
	    
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
			showLeftNavigationBasedOnRole();												
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

