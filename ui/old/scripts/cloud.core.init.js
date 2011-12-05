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
// Default password is MD5 hashed.  Set the following variable to false to disable this.
var md5Hashed = false;
 
$(document).ready(function() { 
	function initUI() {
		var context = $.urlParam('lp');
		if (context != null) { 
			if (context == 'instance') {
				$("#leftmenu_instances").click();
				$("#leftmenu_instances_my_instances").click();
			} else if (context == 'volume') {
				$("#leftmenu_storage").click();
				$("#leftmenu_volume").click();
			} else if (context == 'ip') {
				$("#leftmenu_network").click();
				$("#leftmenu_ip").click();
			} else {
				$("#leftmenu_dashboard").click();
			}
		} else {
			$("#leftmenu_dashboard").click();
		}
	}	
	
	// Setup custom theme
	var $currentTheme = null;
	if ($.cookie("theme") != null) {
		var theme = $.cookie("theme");
		$currentTheme = $("<link>").appendTo("head").attr({
			rel: "stylesheet",
			type: "text/css",
			href: "custom/"+theme+"/css/"+theme+".css"
		});
		$("#theme_button p").text($("#theme_button #theme_menu #"+theme).text());
	}
	$("#theme_button").click(function(event) {
		var $menu = $(this).find("#theme_menu");
		if ($menu.css("display") == "none") {
			$menu.slideDown(500);
		} else {
			$menu.slideUp(500);
		}
	});
	
	$("#theme_button #theme_menu").click(function(event) {
		var target = $(event.target);
		var id = target.attr("id");
		if ($currentTheme != null) {
			$currentTheme.remove();
			$currentTheme = null;
		}
		var name = g_dictionary["label.theme.default"];
		if (id != "theme_default") {
			$currentTheme = $("<link>").appendTo("head").attr({
				rel: "stylesheet",
				type: "text/css",
				href: "custom/"+id+"/css/"+id+".css"
			});
			name = target.text();
			$.cookie("theme", id);
		} else {
			if ($currentTheme != null) {
				$currentTheme.remove();
			}
			$.cookie("theme", null);
			name = g_dictionary["label.theme.default"];
		}
		$("#theme_button p").text(name);
		$(this).hide();
		return false;
	});
	
	// Setup Language option
	if ($.cookie("lang") != null) {
		$("#lang_button p").text($("#lang_button #lang_menu #"+$.cookie("lang")).text());
	}
	
	$("#lang_button").click(function(event) {
		var $menu = $(this).find("#lang_menu");
		if ($menu.css("display") == "none") {
			$menu.slideDown(500);
		} else {
			$menu.slideUp(500);
		}
	});
	
	$("#lang_button #lang_menu").click(function(event) {
		var target = $(event.target);
		var id = target.attr("id");
		$.cookie("lang", id);
		location.replace('/client');
		return false;
	});
	
	// Setup drag and slide for the main UI
	$("#west_panel").resizable({
		minWidth: 221,
		maxWidth: 421,
		ghost: true,
		stop: function(event, ui) { 
			var resized = ui.size.width - 1;
			$("#east_panel").attr("style", "margin-left:" + resized +"px;");
		}
	});

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
		var $dashboard = $(this);
	    selectLeftMenu($dashboard, false, function() {
			selectLeftSubMenu($dashboard);
			clearMiddleMenu();
			clearButtonsOnTop();
			hideMiddleMenu();
			
			$("#right_panel").data("onRefreshFn", function() {
				$("#leftmenu_dashboard").click();
			});
			
			$("#right_panel").load("jsp/dashboard.jsp", function(){
			    currentRightPanelJSP = "jsp/dashboard.jsp";
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
		bindAndListMidMenuItems($("#leftmenu_volume"), "listVolumes", volumeGetSearchParams, "listvolumesresponse", "volume", "jsp/volume.jsp", afterLoadVolumeJSP, volumeToMidmenu, volumeToRightPanel, getMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_snapshot"), "listSnapshots", snapshotGetSearchParams, "listsnapshotsresponse", "snapshot", "jsp/snapshot.jsp", afterLoadSnapshotJSP, snapshotToMidmenu, snapshotToRightPanel, getMidmenuId, false);
				
		if(g_supportELB == "guest")  //ips are allocated on guest network
			bindAndListMidMenuItems($("#leftmenu_ip"), "listPublicIpAddresses&forvirtualnetwork=false&forloadbalancing=true", ipGetSearchParams, "listpublicipaddressesresponse", "publicipaddress", "jsp/ipaddress.jsp", afterLoadIpJSP, ipToMidmenu, ipToRightPanel, ipGetMidmenuId, false);
		else if(g_supportELB == "public")  //ips are allocated on public network
			bindAndListMidMenuItems($("#leftmenu_ip"), "listPublicIpAddresses&forvirtualnetwork=true&forloadbalancing=true", ipGetSearchParams, "listpublicipaddressesresponse", "publicipaddress", "jsp/ipaddress.jsp", afterLoadIpJSP, ipToMidmenu, ipToRightPanel, ipGetMidmenuId, false);
		else			
		    bindAndListMidMenuItems($("#leftmenu_ip"), "listPublicIpAddresses", ipGetSearchParams, "listpublicipaddressesresponse", "publicipaddress", "jsp/ipaddress.jsp", afterLoadIpJSP, ipToMidmenu, ipToRightPanel, ipGetMidmenuId, false); //remove "&forvirtualnetwork=true" for advanced zone whose security group is enabled
		
		bindAndListMidMenuItems($("#leftmenu_security_group"), "listSecurityGroups", securityGroupGetSearchParams, "listsecuritygroupsresponse", "securitygroup", "jsp/securitygroup.jsp", afterLoadSecurityGroupJSP, securityGroupToMidmenu, securityGroupToRightPanel, getMidmenuId, false);
					 		  
		bindAndListMidMenuItems($("#leftmenu_submenu_my_template"), "listTemplates&templatefilter=self", templateGetSearchParams, "listtemplatesresponse", "template", "jsp/template.jsp", afterLoadTemplateJSP, templateToMidmenu, templateToRightPanel, templateGetMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_submenu_featured_template"), "listTemplates&templatefilter=featured", templateGetSearchParams, "listtemplatesresponse", "template", "jsp/template.jsp", afterLoadTemplateJSP, templateToMidmenu, templateToRightPanel, templateGetMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_submenu_community_template"), "listTemplates&templatefilter=community", templateGetSearchParams, "listtemplatesresponse", "template", "jsp/template.jsp", afterLoadTemplateJSP, templateToMidmenu, templateToRightPanel, templateGetMidmenuId, false);
		
		bindAndListMidMenuItems($("#leftmenu_submenu_my_iso"), "listIsos&isofilter=self", isoGetSearchParams, "listisosresponse", "iso", "jsp/iso.jsp", afterLoadIsoJSP, isoToMidmenu, isoToRightPanel, isoGetMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_submenu_featured_iso"), "listIsos&isofilter=featured", isoGetSearchParams, "listisosresponse", "iso", "jsp/iso.jsp", afterLoadIsoJSP, isoToMidmenu, isoToRightPanel, isoGetMidmenuId, false);
		bindAndListMidMenuItems($("#leftmenu_submenu_community_iso"), "listIsos&isofilter=community", isoGetSearchParams, "listisosresponse", "iso", "jsp/iso.jsp", afterLoadIsoJSP, isoToMidmenu, isoToRightPanel, isoGetMidmenuId, false);
		
		if (isAdmin() || isDomainAdmin()) {
		    bindAndListMidMenuItems($("#leftmenu_account_my_accounts"), "listAccounts&domainid="+g_domainid+"&name="+g_account, accountGetSearchParams, "listaccountsresponse", "account", "jsp/account.jsp", afterLoadAccountJSP, accountToMidmenu, accountToRightPanel, getMidmenuId, false);
		    bindAndListMidMenuItems($("#leftmenu_account_all_accounts"), "listAccounts", accountGetSearchParams, "listaccountsresponse", "account", "jsp/account.jsp", afterLoadAccountJSP, accountToMidmenu, accountToRightPanel, getMidmenuId, false);
    	}
    	
    	if (isAdmin()) {	    	   
    	    bindAndListMidMenuItems($("#leftmenu_alert"), "listAlerts", alertGetSearchParams, "listalertsresponse", "alert", "jsp/alert.jsp", afterLoadAlertJSP, alertToMidmenu, alertToRightPanel, getMidmenuId, false);
		    	
    	    //system
    	    bindAndListMidMenuItems($("#leftmenu_submenu_virtual_router"), "listRouters", routerGetSearchParams, "listroutersresponse", "router", "jsp/router.jsp", afterLoadRouterJSP, routerToMidmenu, routerToRightPanel, getMidmenuId, false);
		    bindAndListMidMenuItems($("#leftmenu_submenu_systemvm"), "listSystemVms", systemVmGetSearchParams, "listsystemvmsresponse", "systemvm", "jsp/systemvm.jsp", afterLoadSystemVmJSP, systemvmToMidmenu, systemvmToRightPanel, getMidmenuId, false);
		
			//configuration	
			bindAndListMidMenuItems($("#leftmenu_service_offering"), "listServiceOfferings&issystem=false", serviceOfferingGetSearchParams, "listserviceofferingsresponse", "serviceoffering", "jsp/serviceoffering.jsp", afterLoadServiceOfferingJSP, serviceOfferingToMidmenu, serviceOfferingToRightPanel, getMidmenuId, false); 
			bindAndListMidMenuItems($("#leftmenu_system_service_offering"), "listServiceOfferings&issystem=true", systemServiceOfferingGetSearchParams, "listserviceofferingsresponse", "serviceoffering", "jsp/systemserviceoffering.jsp", afterLoadSystemServiceOfferingJSP, systemServiceOfferingToMidmenu, systemServiceOfferingToRightPanel, getMidmenuId, false); 
			bindAndListMidMenuItems($("#leftmenu_disk_offering"), "listDiskOfferings", diskOfferingGetSearchParams, "listdiskofferingsresponse", "diskoffering", "jsp/diskoffering.jsp", afterLoadDiskOfferingJSP, diskOfferingToMidmenu, diskOfferingToRightPanel, getMidmenuId, false); 
			bindAndListMidMenuItems($("#leftmenu_network_offering"), "listNetworkOfferings&guestiptype=Virtual", networkOfferingGetSearchParams, "listnetworkofferingsresponse", "networkoffering", "jsp/networkoffering.jsp", afterLoadNetworkOfferingJSP, networkOfferingToMidmenu, networkOfferingToRightPanel, getMidmenuId, false);  
		}
	
		$("#leftmenu_global_setting").bind("click", function(event) {
		    selectLeftSubMenu($(this));		
		    hideMiddleMenu();			
	        clearMiddleMenu();
			clearButtonsOnTop();
		    	
		    $("#right_panel").data("onRefreshFn", function() {
		        $("#leftmenu_global_setting").click();
		    });
		    if (currentRightPanelJSP != "jsp/globalsetting.jsp") {
				$("#right_panel").load("jsp/globalsetting.jsp", function(){   
				    currentRightPanelJSP = "jsp/globalsetting.jsp";  												  
					afterLoadGlobalSettingJSP();					
				});    
			} else {
				populateGlobalSettingGrid();
			}
		    
		    return false;
		});
				
		$("#leftmenu_physical_resource").bind("click", function(event) {
		    var $target = $(event.target);
		    var targetId = $target.attr("id");
		    		    
			if(targetId == "physical_resource_arrow") {			    
			    if($target.hasClass("expanded_close") == true) {
		            $target.removeClass("expanded_close").addClass("expanded_open");
		            buildZoneTree();
	            } else {
		            $target.removeClass("expanded_open").addClass("expanded_close");
		            $("#leftmenu_zone_tree").find("#tree_container").empty();
	            }			    
			}
			else {			
		        if(currentRightPanelJSP != "jsp/resource.jsp") { 
				    removeDialogs();
    			
				    $("#right_panel").data("onRefreshFn", function() {
					    $("#leftmenu_physical_resource").click();
				    });                    
                    
                    $("#right_panel").load("jsp/resource.jsp", function(){     
                        currentRightPanelJSP = "jsp/resource.jsp";                                         	        
                        afterLoadResourceJSP(); 
                    });      
                } 
                else {
                    resourceCountTotal();	  
                }
            }
            					
			return false;
		});
				
		$("#leftmenu_template_filter_header, #leftmenu_iso_filter_header").unbind("click").bind("click", function(event) {	
		    var $arrowIcon = $(this).find("#arrow_icon");		    
		    var $subItemContainer = $(this).next();
		    if($arrowIcon.hasClass("expanded_open")) { 		        
		        $subItemContainer.hide();
		        $arrowIcon.removeClass("expanded_open").addClass("expanded_close");
		    }
		    else if($arrowIcon.hasClass("expanded_close")) {
		        $subItemContainer.show();
		        $arrowIcon.removeClass("expanded_close").addClass("expanded_open");
		    }
		    return false;
		});
		
		//Setup domain
		if (isAdmin() || isDomainAdmin()) {
		    bindEventHandlerToDomainTreeNode();			
		    drawRootNode(g_domainid);
		}
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
	
	//clear search
	$("#clear_search").unbind("click").bind("click", function(event) {
		if(searchParams.length > 0)  {
			if($selectedSubMenu != null)
				$selectedSubMenu.click();
			return false;
		}
	});
	
	//refresh mid search
	$("#refresh_mid").unbind("click").bind("click", function(event) {	    
	    var onRefreshFn = $("#right_panel").data("onRefreshFn");
		if(onRefreshFn != null)
		    onRefreshFn();
		return false;
	});
	
	//basic search	
	$("#basic_search").find("#search_input").unbind("keypress").bind("keypress", function(event) { 	 
	    event.stopPropagation();   
	    if(event.keyCode == keycode_Enter) { 
	        event.preventDefault();
	        var params = $("#middle_menu_pagination").data("params");
	        if(params == null)
	            return;	 
	        //lastSearchType = "basic_search";       	    
	        listMidMenuItems2(params.commandString, params.getSearchParamsFn, params.jsonResponse1, params.jsonResponse2, params.toMidmenuFn, params.toRightPanelFn, params.getMidmenuIdFn, params.isMultipleSelectionInMidMenu, 1);
	    }		    
	});
	
	//advanced search	   
	$("#advanced_search_icon").unbind("click").bind("click", function(event) {
	    if($(this).hasClass("up")) {  //clicking up-arrow          
	        getAdvancedSearchPopupInSearchContainer().slideUp("500");
	        $(this).removeClass("up");	//change arrow from up to down
	    }
	    else {  //clicking down-arrow 
	        $(this).addClass("up");	    //change arrow from down to up
	         	              
	        if(getAdvancedSearchPopupInSearchContainer().length > 0) {
	            getAdvancedSearchPopupInSearchContainer().slideDown("500");
	        }
	        else {	
	            var $advancedSearchPopup = getAdvancedSearchPopupInHiddenContainer();
	            $advancedSearchPopup.slideDown("500").appendTo($("#advanced_search_container"));
	            	                    	    
	            $advancedSearchPopup.unbind("click").bind("click", function(event) {
	                var $target = $(event.target);
	                var targetId = $target.attr("id");              	                
	                if($target.hasClass("textwatermark")) {
	                    $target.val("");
	                    $target.removeClass("textwatermark");    
	                }	                	              
	                return true;
	            });
        	    	
	            $advancedSearchPopup.unbind("keypress").bind("keypress", function(event) {	       
	                event.stopPropagation();   
	                if(event.keyCode == keycode_Enter) { 
	                    event.preventDefault();	  		                                      
	                    var params = $("#middle_menu_pagination").data("params");
	                    if(params == null)
	                        return;	        	    
	                    //(to-do: close auto-complete fields)	                
	                    listMidMenuItems2(params.commandString, params.getSearchParamsFn, params.jsonResponse1, params.jsonResponse2, params.toMidmenuFn, params.toRightPanelFn, params.getMidmenuIdFn, params.isMultipleSelectionInMidMenu, 1);    	                            
	                }	
	            });	
        	    	    
	            if(isAdmin() || isDomainAdmin())
	                $advancedSearchPopup.find("#adv_search_domain_li, #adv_search_account_li, #adv_search_pod_li").show();
	            else
	                $advancedSearchPopup.find("#adv_search_domain_li, #adv_search_account_li, #adv_search_pod_li").hide(); 
        	    
                var zoneSelect = $advancedSearchPopup.find("#adv_search_zone");	    
	            if(zoneSelect.length>0) {  //if zone dropdown is found on Advanced Search dialog 	    		
	                $.ajax({
		                data: createURL("command=listZones&available=true"),
		                dataType: "json",
		                success: function(json) {
			                var zones = json.listzonesresponse.zone;			   
			                zoneSelect.empty();					
			                zoneSelect.append("<option value=''>" + g_dictionary["label.by.zone"] + "</option>"); 
			                if (zones != null && zones.length > 0) {
			                    for (var i = 0; i < zones.length; i++) {
				                    zoneSelect.append("<option value='" + zones[i].id + "'>" + fromdb(zones[i].name) + "</option>"); 
			                    }
			                }
		                }
	                });
            		
	                var podSelect = $advancedSearchPopup.find("#adv_search_pod").empty();	
	                var podLabel = $advancedSearchPopup.find("#adv_search_pod_label");
	                if(podSelect.length>0 && $advancedSearchPopup.find("#adv_search_pod_li").css("display")!="none") {		        
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
				                        podSelect.append("<option value=''>" + g_dictionary["label.by.pod"] + "</option>"); 				                        		            
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
            	            	
            	applyAutoCompleteToDomainField($advancedSearchPopup.find("#domain")); 
            	   
	            $advancedSearchPopup.find("#adv_search_startdate, #adv_search_enddate").datepicker({dateFormat: 'yy-mm-dd'});	            
	        }
	    }
	    	   
	    return false;
	});
	
	//pagination
	$("#middle_menu_pagination").unbind("click").bind("click", function(event) {	
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
	$("#refresh_link").unbind("click").bind("click", function(event) {		
		if ($currentMidmenuItem != null) {			
			if($("#midmenu_container").find("#multiple_selection_sub_container").length == 0) //single-selection middle menu
	            $currentMidmenuItem.click();	    
	        else  //multiple-selection middle menu
	            clickItemInMultipleSelectionMidmenu($currentMidmenuItem); 
		} else {
			var onRefreshFn = $("#right_panel").data("onRefreshFn");
			if(onRefreshFn != null)
				onRefreshFn();
		}
		return false;
	});
	
	// Initialize help drop down dialog
	$("#help_link").unbind("click").bind("click", function(event) {
		$("#help_dropdown_dialog").show();
		$("#help_button").addClass("selected");
		return false;
	});
	
	$("#help_dropdown_close").unbind("click").bind("click", function(event) {
		$("#help_dropdown_dialog").hide();
		$("#help_button").removeClass("selected");
		return false;
	});
	
	initializeTestTool();
		
	// Default AJAX Setup
	$.ajaxSetup({
		url: clientApiUrl,
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
		g_supportELB = null;
		g_firewallRuleUiEnabled = null;
		
		$.cookie('JSESSIONID', null);
		$.cookie('sessionKey', null);
		$.cookie('username', null);
		$.cookie('account', null);
		$.cookie('domainid', null);
		$.cookie('role', null);
		$.cookie('networktype', null); 
		$.cookie('timezoneoffset', null);
		$.cookie('timezone', null);
		$.cookie('supportELB', null);
		$.cookie('firewallRuleUiEnabled', null);
		
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
		var username = $("#account_username").val();
		array1.push("&username="+encodeURIComponent(username));
		
		var password = $("#account_password").val();
		if (md5Hashed) {
			password = $.md5(password);
		} 
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
					
				$.cookie('sessionKey', g_sessionKey, { expires: 1});
				$.cookie('username', g_username, { expires: 1});	
				$.cookie('account', g_account, { expires: 1});	
				$.cookie('domainid', g_domainid, { expires: 1});				
				$.cookie('role', g_role, { expires: 1});
				$.cookie('timezoneoffset', g_timezoneoffset, { expires: 1});  
				$.cookie('timezone', g_timezone, { expires: 1});  				
								
				$.ajax({
					data: createURL("command=listCapabilities"),
					dataType: "json",
					async: false,
					success: function(json) {	
					    /* g_supportELB: "guest"   — ips are allocated on guest network (so use 'forvirtualnetwork' = false)
					     * g_supportELB: "public"  - ips are allocated on public network (so use 'forvirtualnetwork' = true)
					     * g_supportELB: "false"   – no ELB support
					     */
					    g_supportELB = json.listcapabilitiesresponse.capability.supportELB.toString(); //convert boolean to string if it's boolean				    
					    $.cookie('supportELB', g_supportELB, { expires: 1}); 
					    					    
					    g_firewallRuleUiEnabled = json.listcapabilitiesresponse.capability.firewallRuleUiEnabled.toString(); //convert boolean to string if it's boolean						    
					    $.cookie('firewallRuleUiEnabled', g_firewallRuleUiEnabled, { expires: 1}); 
					    			    
						if (json.listcapabilitiesresponse.capability.userpublictemplateenabled != null) {
							g_userPublicTemplateEnabled = json.listcapabilitiesresponse.capability.userpublictemplateenabled.toString(); //convert boolean to string if it's boolean
							$.cookie('userpublictemplateenabled', g_userPublicTemplateEnabled, { expires: 1});
						}
						
						if (json.listcapabilitiesresponse.capability.securitygroupsenabled != null) {
							g_directAttachSecurityGroupsEnabled = json.listcapabilitiesresponse.capability.securitygroupsenabled.toString(); //convert boolean to string if it's boolean
							$.cookie('directattachsecuritygroupsenabled', g_directAttachSecurityGroupsEnabled, { expires: 1});
						}
						
						buildSecondLevelNavigation();
						$("#main_username").text(g_username);
						$("#login_wrapper").hide();	
						showLeftNavigationBasedOnRole();
						initUI();
						periodicallyCheckNonCompleteAsyncJob();
						$("#main").show();
					},
					error: function(xmlHTTP) {
						logout(false);
					},
					beforeSend: function(xmlHTTP) {
						return true;
					}
				});
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
	initDialogWithOK("dialog_action_complete", 350, false);
	
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
	if (g_loginResponse == null) {
		g_mySession = $.cookie("JSESSIONID");
		g_sessionKey = $.cookie("sessionKey");
		g_role = $.cookie("role");
		g_username = $.cookie("username");
		g_account = $.cookie("account");
		g_domainid = $.cookie("domainid");
		g_timezone = $.cookie("timezone");
		g_directAttachSecurityGroupsEnabled = $.cookie("directattachsecuritygroupsenabled");
		g_userPublicTemplateEnabled = $.cookie("userpublictemplateenabled");		
				
		if($.cookie("timezoneoffset") != null)
			g_timezoneoffset = isNaN($.cookie("timezoneoffset"))?null: parseFloat($.cookie("timezoneoffset"));
		else
			g_timezoneoffset = null;
			
		if (g_directAttachSecurityGroupsEnabled == null || g_directAttachSecurityGroupsEnabled.length == 0) 		
			g_directAttachSecurityGroupsEnabled = "false";	
			
		if (g_userPublicTemplateEnabled == null || g_userPublicTemplateEnabled.length == 0) 		
			g_userPublicTemplateEnabled = "true";
	} else {
		g_mySession = $.cookie('JSESSIONID');
		g_sessionKey = encodeURIComponent(g_loginResponse.sessionkey);
		g_role = g_loginResponse.type;
		g_username = g_loginResponse.username;	
		g_account = g_loginResponse.account;
		g_domainid = g_loginResponse.domainid;	
		g_timezone = g_loginResponse.timezone;								
		g_timezoneoffset = g_loginResponse.timezoneoffset;
	}
	
	if(g_supportELB == null)
		g_supportELB = $.cookie("supportELB");
		
	if(g_firewallRuleUiEnabled == null)
		g_firewallRuleUiEnabled = $.cookie("firewallRuleUiEnabled");
	
	$.ajax({
	    data: createURL("command=listCapabilities"),
		dataType: "json",
		async: false,
		success: function(json) {		 
			/* g_supportELB: "guest"   — ips are allocated on guest network (so use 'forvirtualnetwork' = false)
		     * g_supportELB: "public"  - ips are allocated on public network (so use 'forvirtualnetwork' = true)
		     * g_supportELB: "false"   – no ELB support
		     */
		    g_supportELB = json.listcapabilitiesresponse.capability.supportELB.toString(); //convert boolean to string if it's boolean				    
		    $.cookie('supportELB', g_supportELB, { expires: 1}); 
		    					    
		    g_firewallRuleUiEnabled = json.listcapabilitiesresponse.capability.firewallRuleUiEnabled.toString(); //convert boolean to string if it's boolean						    
		    $.cookie('firewallRuleUiEnabled', g_firewallRuleUiEnabled, { expires: 1}); 
		    			    
			if (json.listcapabilitiesresponse.capability.userpublictemplateenabled != null) {
				g_userPublicTemplateEnabled = json.listcapabilitiesresponse.capability.userpublictemplateenabled.toString(); //convert boolean to string if it's boolean
				$.cookie('userpublictemplateenabled', g_userPublicTemplateEnabled, { expires: 1});
			}
			
			if (json.listcapabilitiesresponse.capability.securitygroupsenabled != null) {
				g_directAttachSecurityGroupsEnabled = json.listcapabilitiesresponse.capability.securitygroupsenabled.toString(); //convert boolean to string if it's boolean
				$.cookie('directattachsecuritygroupsenabled', g_directAttachSecurityGroupsEnabled, { expires: 1});
			}				    		
						
			buildSecondLevelNavigation();
			$("#main_username").text(g_username);
			showLeftNavigationBasedOnRole();
			initUI();
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

