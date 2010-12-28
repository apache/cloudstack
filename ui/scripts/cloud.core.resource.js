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

function buildZoneTree() {     
    var forceLogout = true;  // We force a logout only if the user has first added a POD for the very first time     
    var $loading = $("#leftmenu_zone_tree").find("#loading_container").show();
    var $zoneTree = $("#leftmenu_zone_tree").find("#tree_container").hide();
  
    $.ajax({
	    data: createURL("command=listZones&available=true"),
		dataType: "json",		
		success: function(json) {
			var items = json.listzonesresponse.zone;
			var container = $zoneTree.empty();
			if (items != null && items.length > 0) {					    
				for (var i = 0; i < items.length; i++) {
					var $zoneNode = $("#leftmenu_zone_node_template").clone(true);
					zoneJSONToTreeNode(items[i],$zoneNode);
					container.append($zoneNode.show());
				}
			}	
			$loading.hide();
            $zoneTree.show();
		}
	});  
    
	$("#leftmenu_zone_node_template").unbind("click").bind("click", function(event) {
		var $zoneNode = $(this);
		var target = $(event.target);
		var action = target.attr("id");
		var id = $zoneNode.data("id");
		var name = $zoneNode.data("name");
		
		switch (action) {
			case "zone_arrow" :				    
			    var zoneObj = $zoneNode.data("jsonObj");
			    var $zoneContent = $zoneNode.find("#zone_content");				  	   
				if(target.hasClass("expanded_close")) {						
					target.removeClass("expanded_close").addClass("expanded_open");							
					$zoneContent.show();	
									
					$.ajax({
                        data: createURL("command=listPods&zoneid="+zoneObj.id),
	                    dataType: "json",
	                    async: false,
	                    success: function(json) {
		                    var items = json.listpodsresponse.pod;			    
		                    var $container = $zoneContent.find("#pods_container");		                    
		                    if (items != null && items.length > 0) {					    
			                    for (var i = 0; i < items.length; i++) {
				                    var $podNode = $("#leftmenu_pod_node_template").clone(true);
				                    podJSONToTreeNode(items[i], $podNode);
				                    $container.append($podNode.show());				                    
			                    }			                    
		                    }		    
	                    }
                    });                    
				} 
				else if(target.hasClass("expanded_open")) {					
					target.removeClass("expanded_open").addClass("expanded_close");					
					$zoneContent.hide();
					$zoneContent.find("#pods_container").empty();	
				}				
				break;					
			
			default:				    		    
			    selectRowInZoneTree($(this).find("#zone_header"));	
			    
			    if(currentRightPanelJSP != "jsp/zone.jsp") {                       
	                removeDialogs();
	                
	                var $thisNode = $(this);
                    $("#right_panel").load("jsp/zone.jsp", function(){     
                        currentRightPanelJSP = "jsp/zone.jsp";
                                        
                        $(this).data("onRefreshFn", function() {
	                        zoneJsonToDetailsTab();
	                    });  
            	        
                        afterLoadZoneJSP($thisNode);   
                    });      
                } 
                else {
                    zoneJsonToRightPanel($(this));			 
                }
			   	    		   				    		   			    
			    break;	
		}
		return false;
	});  
	
	$("#network_header").unbind("click").bind("click", function(event) {	   
	    selectRowInZoneTree($(this));	
	    
        if(currentRightPanelJSP != "jsp/network.jsp") {            
            removeDialogs();
            
            var $thisNode = $(this);    
            $("#right_panel").load("jsp/network.jsp", function(){     
                currentRightPanelJSP = "jsp/network.jsp";                 
                                      
                $(this).data("onRefreshFn", function() {		        
                    var zoneObj = $midmenuItem1.data("jsonObj");
                    if(zoneObj == null)
                        return;
                    $("#zone_"+zoneObj.id).find("#network_header").click();
                }); 
                afterLoadNetworkJSP($thisNode);                       
            });      
        } 
        else {
            networkPopulateMiddleMenu($(this));  		 
        }	    
	       
	    return false;
	});
	
	$("#leftmenu_pod_node_template").unbind("click").bind("click", function(event) {
	    var $podNode = $(this);
		var target = $(event.target);
		var action = target.attr("id");
		var id = $podNode.data("id");
		var name = $podNode.data("name");
		
		switch (action) {
			case "pod_arrow" :				    		    
			    var podObj = $podNode.data("jsonObj");
			    var $podContent = $podNode.find("#pod_content");					 	   
				if(target.hasClass("expanded_close")) {						
					target.removeClass("expanded_close").addClass("expanded_open");									
					$podContent.show();
					refreshClusterUnderPod($podNode); 
				} 
				else if(target.hasClass("expanded_open")) {					
					target.removeClass("expanded_open").addClass("expanded_close");	
					$podContent.hide();		
					$podContent.find("#clusters_container").empty();									
				}	
			    break;
			default:			
			    selectRowInZoneTree($(this).find("#pod_header"));	    
	            
	            if(currentRightPanelJSP != "jsp/pod.jsp") {                     
	                removeDialogs();
	                
	                var $thisNode = $(this); 
                    $("#right_panel").load("jsp/pod.jsp", function(){     
                        currentRightPanelJSP = "jsp/pod.jsp";
                                                    
                        $(this).data("onRefreshFn", function() {
		                    podJsonToDetailsTab();
		                });  
		                afterLoadPodJSP($thisNode);  
                    });      
                } 
                else {
                    podJsonToRightPanel($(this));    			 
                }
	            	            	
			    break;
	    }		    	    
	    return false;
	});  
	
	$("#leftmenu_cluster_node_template").unbind("click").bind("click", function(event) {
	    selectRowInZoneTree($(this).find("#cluster_header"));	    
	   	  
	   	hostClearRightPanel();    	    
	    var objCluster = $(this).data("jsonObj");
        listMidMenuItems(("listHosts&type=Routing&clusterid="+objCluster.id), hostGetSearchParams, "listhostsresponse", "host", "jsp/host.jsp", afterLoadHostJSP, hostToMidmenu, hostToRightPanel, getMidmenuId, false, ("cluster_"+objCluster.id));    
	    	    
	    return false;
	});  
}    

function refreshClusterUnderPod($podNode, newClusterName, existingClusterId, noClicking) {  
    var podId = $podNode.data("podId");     
    if(podId == null)  //e.g. $podNode is not on the screen (when zone tree is hidden) ($podNode.length==0) 
        return;
    
    $.ajax({
        data: createURL("command=listClusters&podid="+podId),
        dataType: "json",
        async: false,
        success: function(json) {
            var items = json.listclustersresponse.cluster;  
            var container = $podNode.find("#clusters_container").empty();
            if (items != null && items.length > 0) {					    
                for (var i = 0; i < items.length; i++) {
                    var $clusterNode = $("#leftmenu_cluster_node_template").clone(true); 
                    var item = items[i];
                    clusterJSONToTreeNode(item, $clusterNode);
                    container.append($clusterNode.show());                                     
                    if(newClusterName != null && fromdb(item.name) == newClusterName && noClicking!=true) {   
                        $clusterNode.find("#cluster_name").click();                
                    }                 
                }                         
                $podNode.find("#pod_arrow").removeClass("white_nonexpanded_close").addClass("expanded_open");
                $podNode.find("#pod_content").show();                  
                
                if(existingClusterId != null && noClicking!=true) {
                    $("#cluster_"+existingClusterId).find("#cluster_name").click();	
                }                      
            }            
        }
    });	
}

function selectRowInZoneTree($rowToSelect) { 
    if($selectedSubMenu != null)
        $selectedSubMenu.removeClass("selected");    
    $rowToSelect.addClass("selected");
    $selectedSubMenu = $rowToSelect;
}

function selectTreeNodeInLeftMenu($menuToSelect, expandable) {	
	if($selectedLeftMenu != null)
		$selectedLeftMenu.removeClass("selected");  
	$menuToSelect.addClass("selected");
	$selectedLeftMenu = $menuToSelect; 	
}

function zoneJSONToTreeNode(jsonObj, $zoneNode) {
    var zoneid = jsonObj.id;
    $zoneNode.attr("id", "zone_" + zoneid);  
    $zoneNode.data("jsonObj", jsonObj);
    
   if(jsonObj.networktype == "Advanced") {
        $zoneNode.find("#network_header").show().data("jsonObj", jsonObj);		 
    }
        
    $zoneNode.data("id", zoneid).data("name", fromdb(jsonObj.name));
    var zoneName = $zoneNode.find("#zone_name").text(fromdb(jsonObj.name));	    
    zoneName.data("jsonObj", jsonObj);	    
		
	$.ajax({
        data: createURL("command=listPods&zoneid="+zoneid),
	    dataType: "json",
	    async: false,
	    success: function(json) {
		    var items = json.listpodsresponse.pod;	
		    if (items != null && items.length > 0) {					    
			    $zoneNode.find("#zone_arrow").removeClass("white_nonexpanded_close").addClass("expanded_close"); 
			    forceLogout = false;  // We don't force a logout if pod(s) exit.	
			}	    		    
	    }
    });	
}

function podJSONToTreeNode(json, $podNode) {	
    var podid = json.id;
    $podNode.attr("id", "pod_" + podid); 
    $podNode.data("jsonObj", json);     	
	$podNode.data("podId", podid).data("name", fromdb(json.name));
	
	var podName = $podNode.find("#pod_name").text(fromdb(json.name));
	podName.data("jsonObj", json);	    
		
    refreshClusterUnderPod($podNode);            
}
		
function clusterJSONToTreeNode(json, $clusterNode) {
    $clusterNode.attr("id", "cluster_"+json.id);
    $clusterNode.data("jsonObj", json);	  
    $clusterNode.data("id", json.id).data("name", fromdb(json.name));	    
    var clusterName = $clusterNode.find("#cluster_name").text(fromdb(json.name));
    clusterName.data("jsonObj", json);	   
}			

function afterLoadResourceJSP() {
    hideMiddleMenu();        
     
    var $topButtonContainer = clearButtonsOnTop();			    	       
	$("#top_buttons").appendTo($topButtonContainer);  
        
    initAddZoneWizard();  
	initAddZoneLinks();	 
    
	initUpdateConsoleCertButton($("#Update_SSL_Certificate_button"));
	
	initDialog("dialog_update_cert", 450);	
	initDialog("dialog_add_pod_in_resource_page", 370); 	
    initDialog("dialog_add_host_in_resource_page");  
    initDialog("dialog_add_pool_in_resource_page");
		
	initAddPodShortcut();
	initAddHostShortcut();
	initAddPrimaryStorageShortcut();
		
	resourceCountTotal();	  
}

function initAddPodShortcut() {
    var $dialogAddPod = $("#dialog_add_pod_in_resource_page");

    var $zoneDropdown = $dialogAddPod.find("#zone_dropdown");
    $.ajax({
	    data: createURL("command=listZones&available=true"),
	    dataType: "json",
	    async: false,
	    success: function(json) {
	        var items = json.listzonesresponse.zone;			
			if (items != null && items.length > 0) {	
			    for(var i=0; i<items.length; i++)		   			    
			        $zoneDropdown.append("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>");
			}	
	    }
	});    
    
    $zoneDropdown.bind("change", function(event) {
	    var zoneId = $(this).val();	    
	    if(zoneId == null)
	        return;
	    $.ajax({
	        data: createURL("command=listZones&id="+zoneId),
	        dataType: "json",	        
	        success: function(json) {	            
	            var zoneObj = json.listzonesresponse.zone[0];		           
	            if(zoneObj.networktype == "Basic") { //basic-mode network (pod-wide VLAN)
                    $dialogAddPod.find("#guestip_container, #guestnetmask_container, #guestgateway_container").show();
                }
                else if(zoneObj.networktype == "Advanced") { //advanced-mode network (zone-wide VLAN)
                    $dialogAddPod.find("#guestip_container, #guestnetmask_container, #guestgateway_container").hide();     
                }  	                    
	        }
	    });	    
    });    
    $zoneDropdown.change();
  
    $("#add_pod_shortcut").unbind("click").bind("click", function(event) {           
        $dialogAddPod.find("#info_container").hide();	
        //$dialogAddPod.find("#add_pod_name, #dd_pod_gateway,#add_pod_netmask,#add_pod_startip,add_pod_endip").val("");
        		
        $dialogAddPod
        .dialog('option', 'buttons', { 				
	        "Add": function() {		
	            var $thisDialog = $(this);
						
		        // validate values
		        var isValid = true;	
		        isValid &= validateDropDownBox("Zone", $thisDialog.find("#zone_dropdown"), $thisDialog.find("#zone_dropdown_errormsg"));			
		        isValid &= validateString("Name", $thisDialog.find("#add_pod_name"), $thisDialog.find("#add_pod_name_errormsg"));
		        isValid &= validateIp("Netmask", $thisDialog.find("#add_pod_netmask"), $thisDialog.find("#add_pod_netmask_errormsg"));	
		        isValid &= validateIp("Start IP Range", $thisDialog.find("#add_pod_startip"), $thisDialog.find("#add_pod_startip_errormsg"));  //required
		        isValid &= validateIp("End IP Range", $thisDialog.find("#add_pod_endip"), $thisDialog.find("#add_pod_endip_errormsg"), true);  //optional
		        isValid &= validateIp("Gateway", $thisDialog.find("#add_pod_gateway"), $thisDialog.find("#add_pod_gateway_errormsg"));  //required when creating
		        
		        if($thisDialog.find("#guestip_container").css("display") != "none")
                    isValid &= addZoneWizardValidateGuestIPRange($thisDialog);
		        
		        if (!isValid) 
		            return;			
                
                $thisDialog.find("#spinning_wheel").show()
                 
                var zoneId = $thisDialog.find("#zone_dropdown").val(); 
                var name = trim($thisDialog.find("#add_pod_name").val());
		        var netmask = trim($thisDialog.find("#add_pod_netmask").val());
		        var startip = trim($thisDialog.find("#add_pod_startip").val());
		        var endip = trim($thisDialog.find("#add_pod_endip").val());	    //optional
		        var gateway = trim($thisDialog.find("#add_pod_gateway").val());			

                var array1 = [];
                array1.push("&zoneId="+zoneId);
                array1.push("&name="+todb(name));
                array1.push("&netmask="+todb(netmask));
                array1.push("&startIp="+todb(startip));
                if (endip != null && endip.length > 0)
                    array1.push("&endIp="+todb(endip));
                array1.push("&gateway="+todb(gateway));			
								
		        $.ajax({
		          data: createURL("command=createPod"+array1.join("")), 
			        dataType: "json",
			        success: function(json) {			            
			            $thisDialog.find("#spinning_wheel").hide();
			            $thisDialog.dialog("close");
			            
			            var item = json.createpodresponse.pod; 			
		                var $zoneNode = $("#leftmenu_zone_tree").find("#tree_container").find("#zone_" + zoneId);			                		                		                
				        if($zoneNode.find("#zone_arrow").hasClass("expanded_open")) {
				            var template = $("#leftmenu_pod_node_template").clone(true);
		                    podJSONToTreeNode(item, template);	     
				            $zoneNode.find("#pods_container").prepend(template.fadeIn("slow"));		
				        }
				        else {	
		                    $zoneNode.find("#zone_arrow").click();  //expand zone node to show the newly added pod
		                }		
			            			           			             
			            var podTotal = parseInt($("#pod_total").text());
		                podTotal++;
		                $("#pod_total").text(podTotal.toString());  
			                                    
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
				            
				        //Create IP Range 
                        if($thisDialog.find("#guestip_container").css("display") != "none") {       
		                    var netmask = $thisDialog.find("#guestnetmask").val();
		                    var startip = $thisDialog.find("#startguestip").val();
		                    var endip = $thisDialog.find("#endguestip").val();	
		                    var guestgateway = $thisDialog.find("#guestgateway").val();
                    				
		                    var array1 = [];
		                    array1.push("&vlan=untagged");	
		                    array1.push("&zoneid=" + zoneId);
		                    array1.push("&podId=" + item.id);	
		                    array1.push("&forVirtualNetwork=false"); //direct VLAN	
		                    array1.push("&gateway="+todb(guestgateway));
		                    array1.push("&netmask="+todb(netmask));	
		                    array1.push("&startip="+todb(startip));
		                    if(endip != null && endip.length > 0)
		                        array1.push("&endip="+todb(endip));
                            
                            $.ajax({
		                        data: createURL("command=createVlanIpRange" + array1.join("")),
			                    dataType: "json",
			                    async: false,
			                    success: function(json) { 	                    			                			    
				                    //var item = json.createvlaniprangeresponse.vlan;				                    			
			                    },		   
		                        error: function(XMLHttpResponse) {					                    
				                    handleError(XMLHttpResponse, function() {
					                    handleErrorInDialog(XMLHttpResponse, $thisDialog);	
				                    });				                    			
                                }
		                    });		
                        }	  
				        
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

function initAddHostShortcut() {
    var $dialogAddHost = $("#dialog_add_host_in_resource_page");    
    
    $.ajax({
        data: createURL("command=listZones&available=true"),
	    dataType: "json",
	    success: function(json) {
		    var zones = json.listzonesresponse.zone;
		    var zoneSelect = $dialogAddHost.find("#zone_dropdown").empty();								
		    if (zones != null && zones.length > 0) {
			    for (var i = 0; i < zones.length; i++) 
				    zoneSelect.append("<option value='" + zones[i].id + "'>" + fromdb(zones[i].name) + "</option>"); 				    
		    }	
		    //$dialogAddHost.find("#zone_dropdown").change();	//comment out to avoid race condition, do it before dialog box pops up	    
	    }
    });
	
    $dialogAddHost.find("#zone_dropdown").bind("change", function(event) {
	    var zoneId = $(this).val();
	    if(zoneId == null)
	        return;
	    $.ajax({
	        data: createURL("command=listPods&zoneId="+zoneId),
		    dataType: "json",
		    async: false,
		    success: function(json) {
			    var pods = json.listpodsresponse.pod;
			    var podSelect = $dialogAddHost.find("#pod_dropdown").empty();	
			    if (pods != null && pods.length > 0) {
				    for (var i = 0; i < pods.length; i++) {
					    podSelect.append("<option value='" + pods[i].id + "'>" + fromdb(pods[i].name) + "</option>"); 
				    }
			    }
			    $dialogAddHost.find("#pod_dropdown").change();
		    }
	    });
    });
	
    $dialogAddHost.find("#pod_dropdown").bind("change", function(event) {			   
        var podId = $(this).val();
        if(podId == null || podId.length == 0)
            return;        
        refreshClsuterFieldInAddHostDialog($dialogAddHost, podId, null);        
    });                 	        	    
        
    $("#add_host_shortcut").unbind("click").bind("click", function(event) {   
        $dialogAddHost.find("#zone_dropdown").change(); //refresh cluster dropdown (do it here to avoid race condition)        
        $dialogAddHost.find("#info_container").hide();    
        $dialogAddHost.find("#new_cluster_name").val("");
        
        $dialogAddHost
        .dialog('option', 'buttons', { 				
	        "Add": function() { 
	            var $thisDialog = $(this);		            
	            			   
		        var clusterRadio = $thisDialog.find("input[name=cluster]:checked").val();				
			
		        // validate values
		        var isValid = true;	
		        isValid &= validateDropDownBox("Zone", $thisDialog.find("#zone_dropdown"), $thisDialog.find("#zone_dropdown_errormsg"));	
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
				
			    var hypervisor = $thisDialog.find("#host_hypervisor").val();
			    if(hypervisor.length > 0)
				    array1.push("&hypervisor="+hypervisor);
		        
		        var zoneId = $thisDialog.find("#zone_dropdown").val();
		        array1.push("&zoneid="+zoneId);
		        
		        //expand zone in left menu tree (to show pod, cluster under the zone) 
				var $zoneNode = $("#leftmenu_zone_tree").find("#tree_container").find("#zone_" + zoneId);							
				if($zoneNode.find("#zone_arrow").hasClass("expanded_close"))
				    $zoneNode.find("#zone_arrow").click();
										             
		        var podId = $thisDialog.find("#pod_dropdown").val();
		        array1.push("&podid="+podId);
						      
		        var username = trim($thisDialog.find("#host_username").val());
		        array1.push("&username="+todb(username));
				
		        var password = trim($thisDialog.find("#host_password").val());
		        array1.push("&password="+todb(password));
					
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
		        array1.push("&url="+todb(url));
									
		        //var $midmenuItem1 = beforeAddingMidMenuItem() ;    				
		        
		        $.ajax({
			       data: createURL("command=addHost" + array1.join("")),
			        dataType: "json",
			        success: function(json) {
			            $thisDialog.find("#spinning_wheel").hide();
			            $thisDialog.dialog("close");
					
					    showMiddleMenu();
					    
					    var items = json.addhostresponse.host;			  
		                var hostTotal = parseInt($("#host_total").text());
		                hostTotal = hostTotal + items.length;
		                $("#host_total").text(hostTotal.toString());
		                
					    /*
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

function initAddZoneLinks() {     
    $("#add_zone_shortcut,#add_zone_button").unbind("click").bind("click", function(event) {              
        if($("#leftmenu_physical_resource").find("#physical_resource_arrow").hasClass("expanded_close") == true)
			expandOrCollapseZoneTree(); //if Physical Resource arrow shows closed (i.e. zonetree is hidden), expand and show zonetree.    
			       
        openAddZoneWizard();
        return false;
    });            
}

function resourceCountTotal() {		
	$.ajax({
	    data: createURL("command=listZones&available=true"),
	    dataType: "json",
	    success: function(json) {
	        var items = json.listzonesresponse.zone;			
			if (items != null) {			   			    
			    $("#zone_total").text(items.length.toString());
			}	
	    }
	});
	
	$.ajax({
	    data: createURL("command=listPods&available=true"),
	    dataType: "json",
	    success: function(json) {
	        var items = json.listpodsresponse.pod;			    			
			if (items != null) {			   			    
			    $("#pod_total").text(items.length.toString());
			}	
	    }
	});
	
	$.ajax({
        data: createURL("command=listHosts&type=Routing"),
        dataType: "json",
        async: false,
        success: function(json) {
            var items = json.listhostsresponse.host;	
            if (items != null) {					    
                $("#host_total").text(items.length.toString());		                    
            }		    
        }
    });  
    
    $.ajax({
        data: createURL("command=listStoragePools"),
        dataType: "json",
        async: false,
        success: function(json) {
            var items = json.liststoragepoolsresponse.storagepool;	
            if (items != null) {					    
                $("#primarystorage_total").text(items.length.toString());		                    
            }		    
        }
    });  
}

function refreshAddZoneWizard() {
    var $addZoneWizard = $("#add_zone_wizard");
    $addZoneWizard.find("#step2, #step3, #after_submit_screen").hide();
    $addZoneWizard.find("#step1").show();
    $addZoneWizard.find("#basic_mode").click();
}

function openAddZoneWizard() {
    refreshAddZoneWizard();
    $("#add_zone_wizard").show();
    $("#wizard_overlay").show();
}

function closeAddZoneWizard() {
    $("#add_zone_wizard").hide();
    $("#wizard_overlay").hide();
}

function initAddZoneWizard() {    
    var $addZoneWizard = $("#add_zone_wizard");
    $addZoneWizard.find("#add_zone_public").unbind("change").bind("change", function(event) {        
        if($(this).val() == "true") {  //public zone
            $addZoneWizard.find("#domain_dropdown_container").hide();  
        }
        else {  //private zone
            $addZoneWizard.find("#domain_dropdown_container").show();  
        }
        return false;
    });
         
    var domainDropdown = $addZoneWizard.find("#domain_dropdown").empty();	
	$.ajax({
	  data: createURL("command=listDomains"),
		dataType: "json",
		async: false,
		success: function(json) {
			var domains = json.listdomainsresponse.domain;						
			if (domains != null && domains.length > 0) {
				for (var i = 0; i < domains.length; i++) {
					domainDropdown.append("<option value='" + fromdb(domains[i].id) + "'>" + fromdb(domains[i].name) + "</option>"); 
				}
			} 
		}
	});    
     
    $addZoneWizard.unbind("click").bind("click", function(event) {  
        var $thisWizard = $(this);
        var $target = $(event.target);
    
        switch($target.attr("id")) {
            case "close_button":
                closeAddZoneWizard();
                break;
            
            case "Basic":  //create VLAN in pod-level      
                //hide Zone VLAN Range in Add Zone(step 2), show Guest IP Range in Add Pod(step3)                 
                $thisWizard.find("#step2").find("#add_zone_vlan_container").hide();
                $thisWizard.find("#step3").find("#guestip_container, #guestnetmask_container, #guestgateway_container").show();
                return true;
                break;
                
            case "Advanced":  //create VLAN in zone-level 
                //show Zone VLAN Range in Add Zone(step 2), hide Guest IP Range in Add Pod(step3) 
                $thisWizard.find("#step2").find("#add_zone_vlan_container").show();  
                $thisWizard.find("#step3").find("#guestip_container, #guestnetmask_container, #guestgateway_container").hide();   
                return true;
                break;
            
            case "go_to_step_2": //step 1 => step 2   
                $thisWizard.find("#step1").hide();
                $thisWizard.find("#step2").show();
                break;    
                
            case "go_to_step_3": //step 2 => step 3
                var isValid = addZoneWizardValidateZond($thisWizard);
                if (!isValid) 
	                return;	
                $thisWizard.find("#step2").hide();
                $thisWizard.find("#step3").show();
                break;   
           
            case "back_to_step_2": //step 3 => step 2
                $thisWizard.find("#step3").hide();
                $thisWizard.find("#step2").show();
                break;    
                
            case "back_to_step_1": //step 2 => step 1
                $thisWizard.find("#step2").hide();
                $thisWizard.find("#step1").show();
                break; 
                
            case "submit_button": //step 3 => make API call
                var isValid = addZoneWizardValidatePod($thisWizard);
                if($thisWizard.find("#step3").find("#guestip_container").css("display") != "none")
                    isValid &= addZoneWizardValidateGuestIPRange($thisWizard);
                if (!isValid) 
	                return;	
            
                $thisWizard.find("#step3").hide();
                $thisWizard.find("#after_submit_screen").show();
                addZoneWizardSubmit($thisWizard);
                break;       
        }           
        return false;
    }); 
    
    $addZoneWizard.find("#step1").find("#Basic").click();
}

function addZoneWizardValidateZond($thisWizard) {    
	var isValid = true;					
	isValid &= validateString("Name", $thisWizard.find("#add_zone_name"), $thisWizard.find("#add_zone_name_errormsg"));
	isValid &= validateIp("DNS 1", $thisWizard.find("#add_zone_dns1"), $thisWizard.find("#add_zone_dns1_errormsg"), false); //required
	isValid &= validateIp("DNS 2", $thisWizard.find("#add_zone_dns2"), $thisWizard.find("#add_zone_dns2_errormsg"), true);  //optional	
	isValid &= validateIp("Internal DNS 1", $thisWizard.find("#add_zone_internaldns1"), $thisWizard.find("#add_zone_internaldns1_errormsg"), false); //required
	isValid &= validateIp("Internal DNS 2", $thisWizard.find("#add_zone_internaldns2"), $thisWizard.find("#add_zone_internaldns2_errormsg"), true);  //optional	
	if($thisWizard.find("#step2").find("#add_zone_vlan_container").css("display") != "none") {
		isValid &= validateString("VLAN Range", $thisWizard.find("#add_zone_startvlan"), $thisWizard.find("#add_zone_startvlan_errormsg"), true);    //optional
		isValid &= validateString("VLAN Range", $thisWizard.find("#add_zone_endvlan"), $thisWizard.find("#add_zone_endvlan_errormsg"), true);        //optional
	}	
	isValid &= validateCIDR("Guest CIDR", $thisWizard.find("#add_zone_guestcidraddress"), $thisWizard.find("#add_zone_guestcidraddress_errormsg"), false); //required
	return isValid;
}

function addZoneWizardValidatePod($thisWizard) {   
    var isValid = true;					
    isValid &= validateString("Name", $thisWizard.find("#add_pod_name"), $thisWizard.find("#add_pod_name_errormsg"));    
    isValid &= validateIp("Gateway", $thisWizard.find("#add_pod_gateway"), $thisWizard.find("#add_pod_gateway_errormsg"));     
    isValid &= validateIp("Netmask", $thisWizard.find("#add_pod_netmask"), $thisWizard.find("#add_pod_netmask_errormsg"));	
    isValid &= validateIp("Reserved System IP", $thisWizard.find("#add_pod_startip"), $thisWizard.find("#add_pod_startip_errormsg"));  //required
    isValid &= validateIp("Reserved System IP", $thisWizard.find("#add_pod_endip"), $thisWizard.find("#add_pod_endip_errormsg"), true);  //optional    
    return isValid;			
}

function addZoneWizardValidateGuestIPRange($thisWizard) {   
    var isValid = true;	
    isValid &= validateIp("Guest IP Range", $thisWizard.find("#startguestip"), $thisWizard.find("#startguestip_errormsg"));  //required
    isValid &= validateIp("Guest IP Range", $thisWizard.find("#endguestip"), $thisWizard.find("#endguestip_errormsg"), true);  //optional
    isValid &= validateIp("Guest Netmask", $thisWizard.find("#guestnetmask"), $thisWizard.find("#guestnetmask_errormsg"));  //required when creating
    isValid &= validateIp("Guest Gateway", $thisWizard.find("#guestgateway"), $thisWizard.find("#guestgateway_errormsg"));  
    return isValid;			
}

function addZoneWizardSubmit($thisWizard) {
	$thisWizard.find("#spinning_wheel").show();
	
	var moreCriteria = [];	
	
	var networktype = $thisWizard.find("#step1").find("input:radio[name=basic_advanced]:checked").val();  //"Basic", "Advanced"
	moreCriteria.push("&networktype="+todb(networktype));
	
	var name = trim($thisWizard.find("#add_zone_name").val());
	moreCriteria.push("&name="+todb(name));
	
	var dns1 = trim($thisWizard.find("#add_zone_dns1").val());
	moreCriteria.push("&dns1="+todb(dns1));
	
	var dns2 = trim($thisWizard.find("#add_zone_dns2").val());
	if (dns2 != null && dns2.length > 0) 
	    moreCriteria.push("&dns2="+todb(dns2));						
						
	var internaldns1 = trim($thisWizard.find("#add_zone_internaldns1").val());
	moreCriteria.push("&internaldns1="+todb(internaldns1));
	
	var internaldns2 = trim($thisWizard.find("#add_zone_internaldns2").val());
	if (internaldns2 != null && internaldns2.length > 0) 
	    moreCriteria.push("&internaldns2="+todb(internaldns2));						
	 											
    if($thisWizard.find("#step2").find("#add_zone_vlan_container").css("display") != "none") {
		var vlanStart = $thisWizard.find("#add_zone_startvlan").val();
		if(vlanStart != null && vlanStart.length > 0) {	
		    var vlanEnd = $thisWizard.find("#add_zone_endvlan").val();						
		    if (vlanEnd != null && vlanEnd.length > 0) 
		        moreCriteria.push("&vlan=" + todb(vlanStart + "-" + vlanEnd));									
		    else 							
			    moreCriteria.push("&vlan=" + todb(vlanStart));		
        }
	}	
	
	var guestcidraddress = trim($thisWizard.find("#add_zone_guestcidraddress").val());
	moreCriteria.push("&guestcidraddress="+todb(guestcidraddress));	
					
	if($thisWizard.find("#domain_dropdown_container").css("display") != "none") {
	    var domainId = trim($thisWizard.find("#domain_dropdown").val());
	    moreCriteria.push("&domainid="+domainId);	
	}
	
	var zoneId, podId, vlanId, $zoneNode, $podNode, gateway;	
	var afterActionMsg = "";						
    $.ajax({
        data: createURL("command=createZone"+moreCriteria.join("")),
	    dataType: "json",
	    async: false,
	    success: function(json) {	
	        $thisWizard.find("#after_submit_screen").find("#add_zone_tick_cross").removeClass().addClass("zonepopup_reviewtick");
	        $thisWizard.find("#after_submit_screen").find("#add_zone_message").removeClass().text("Zone was created successfully");	         
	       	        	        			        
	        $zoneNode = $("#leftmenu_zone_node_template").clone(true); 			            			   
            var $zoneTree = $("#leftmenu_zone_tree").find("#tree_container");		     			
            $zoneTree.prepend($zoneNode);	
            $zoneNode.fadeIn("slow");				        
	    
		    var item = json.createzoneresponse.zone;					    
		    zoneJSONToTreeNode(item, $zoneNode);		
		    
		    zoneId = item.id;	
		    
		    var zoneTotal = parseInt($("#zone_total").text());
		    zoneTotal++;
		    $("#zone_total").text(zoneTotal.toString());		           
	    },
        error: function(XMLHttpResponse) {            
			handleError(XMLHttpResponse, function() {
			    $thisWizard.find("#after_submit_screen").find("#add_zone_tick_cross").removeClass().addClass("zonepopup_reviewcross");
	            $thisWizard.find("#after_submit_screen").find("#add_zone_message").removeClass().addClass("error").text(("Failed to create zone: " + parseXMLHttpResponse(XMLHttpResponse)));					
			});
        }
    });
    
    if(zoneId != null) {        
        var name = trim($thisWizard.find("#add_pod_name").val());
        var netmask = trim($thisWizard.find("#add_pod_netmask").val());
        var startip = trim($thisWizard.find("#add_pod_startip").val());
        var endip = trim($thisWizard.find("#add_pod_endip").val());	    //optional
        gateway = trim($thisWizard.find("#add_pod_gateway").val());			

        var array1 = [];
        array1.push("&zoneId="+zoneId);
        array1.push("&name="+todb(name));
        array1.push("&netmask="+todb(netmask));
        array1.push("&startIp="+todb(startip));
        if (endip != null && endip.length > 0)
            array1.push("&endIp="+todb(endip));
        array1.push("&gateway="+todb(gateway));			
						
        $.ajax({
            data: createURL("command=createPod"+array1.join("")), 
	        dataType: "json",
	        async: false,
	        success: function(json) {	            
	            $thisWizard.find("#after_submit_screen").find("#add_pod_tick_cross").removeClass().addClass("zonepopup_reviewtick");
	            $thisWizard.find("#after_submit_screen").find("#add_pod_message").removeClass().text("Pod was created successfully");	    
	                      
	            var item = json.createpodresponse.pod; 	
	            podId = item.id;		            		            				    
                $podNode = $("#leftmenu_pod_node_template").clone(true);
                podJSONToTreeNode(item, $podNode);                                				
                $zoneNode.find("#zone_content").show();	
                $zoneNode.find("#pods_container").prepend($podNode.show());						
                $zoneNode.find("#zone_arrow").removeClass("white_nonexpanded_close").addClass("expanded_open");	
                $podNode.fadeIn("slow");
	             
	            var podTotal = parseInt($("#pod_total").text());
		        podTotal++;
		        $("#pod_total").text(podTotal.toString()); 
	                                    
                forceLogout = false;  // We don't force a logout if pod(s) exit.
		        if (forceLogout) {
			        $("#dialog_confirmation")
				        .html("<p>You have successfully added your first Zone and Pod.  After clicking 'OK', this UI will automatically refresh to give you access to the rest of cloud features.</p>")
				        .dialog('option', 'buttons', { 
					        "OK": function() { 	
						        window.location.reload();
					        } 
				        }).dialog("open");
		        }
	        },
            error: function(XMLHttpResponse) {	
				handleError(XMLHttpResponse, function() {				    
				    $thisWizard.find("#after_submit_screen").find("#add_pod_tick_cross").removeClass().addClass("zonepopup_reviewcross");
	                $thisWizard.find("#after_submit_screen").find("#add_pod_message").removeClass().addClass("error").text(("Failed to create pod: " + parseXMLHttpResponse(XMLHttpResponse)));			
				});
            }
        });	
    } 
    
    if(podId != null && $thisWizard.find("#step3").find("#guestip_container").css("display") != "none") {     
        $thisWizard.find("#after_submit_screen").find("#add_guestiprange_message_container").show();
       
		var netmask = $thisWizard.find("#step3").find("#guestnetmask").val();
		var startip = $thisWizard.find("#step3").find("#startguestip").val();
		var endip = $thisWizard.find("#step3").find("#endguestip").val();	
		var guestgateway = $thisWizard.find("#step3").find("#guestgateway").val();
				
		var array1 = [];
		array1.push("&vlan=untagged");	
		array1.push("&zoneid=" + zoneId);
		array1.push("&podId=" + podId);	
		array1.push("&forVirtualNetwork=false"); //direct VLAN	
		array1.push("&gateway="+todb(guestgateway));
		array1.push("&netmask="+todb(netmask));	
		array1.push("&startip="+todb(startip));
		if(endip != null && endip.length > 0)
		    array1.push("&endip="+todb(endip));
        
        $.ajax({
		    data: createURL("command=createVlanIpRange" + array1.join("")),
			dataType: "json",
			async: false,
			success: function(json) { 			    
			    $thisWizard.find("#after_submit_screen").find("#add_guestiprange_tick_cross").removeClass().addClass("zonepopup_reviewtick");
	            $thisWizard.find("#after_submit_screen").find("#add_guestiprange_message").removeClass().text("Guest IP range was created successfully");	    
			    
				var item = json.createvlaniprangeresponse.vlan;
				vlanId = item.id;				
			},		   
		    error: function(XMLHttpResponse) {	
				handleError(XMLHttpResponse, function() {			
				    $thisWizard.find("#after_submit_screen").find("#add_guestiprange_tick_cross").removeClass().addClass("zonepopup_reviewcross");
	                $thisWizard.find("#after_submit_screen").find("#add_guestiprange_message").removeClass().addClass("error").text(("Failed to create Guest IP range: " + parseXMLHttpResponse(XMLHttpResponse)));			
			    });
            }
		});		
    }
    else {
         $thisWizard.find("#after_submit_screen").find("#add_guestiprange_message_container").hide();
    }
    
    $thisWizard.find("#spinning_wheel").hide();   
}

function initUpdateConsoleCertButton($midMenuAddLink2) {
	$midMenuAddLink2.find("#label").text("Update SSL Certificate");
	$midMenuAddLink2.show();   
	$midMenuAddLink2.unbind("click").bind("click", function(event) { 
		var $certDialog = $("#dialog_update_cert");
		$certDialog.find("#info_container").hide();
		$certDialog
		.dialog('option', 'buttons', {
			"Add": function() {
				var $thisDialog = $(this);
				var isValid = true;					
				isValid &= validateString("SSL Certificate", $thisDialog.find("#update_cert"), $thisDialog.find("#update_cert_errormsg"), false, 4096);
				if (!isValid) return;	

				$spinningWheel = $thisDialog.find("#spinning_wheel").show();
				
				var cert = trim($thisDialog.find("#update_cert").val());
				
				$.ajax({
			        data: createURL("command=uploadCustomCertificate&certificate="+todb(cert)),
				    dataType: "json",
				    success: function(json) {
						var jobId = json.uploadcustomcertificateresponse.jobid;
						var timerKey = "asyncJob_" + jobId;					                       
						$("body").everyTime(
							5000,
							timerKey,
							function() {
								$.ajax({
									data: createURL("command=queryAsyncJobResult&jobId="+jobId),
									dataType: "json",									                    					                    
									success: function(json) {		                                                     							                       
										var result = json.queryasyncjobresultresponse;										                   
										if (result.jobstatus == 0) {
											return; //Job has not completed
										} else {											                    
											$("body").stopTime(timerKey);				                        
											$spinningWheel.hide(); 
																																		 
											if (result.jobstatus == 1) { // Succeeded 	
												$thisDialog.dialog("close");
												// TODO: Add a confirmation message
											} else if (result.jobstatus == 2) { // Failed	
												var errorMsg = result.jobresult.errortext;
												$thisDialog.find("#info_container").text(errorMsg).show();
											}	
										}
									},
									error: function(XMLHttpResponse) {	                            
										$("body").stopTime(timerKey);
										handleError(XMLHttpResponse, function() {
											handleErrorInDialog(XMLHttpResponse, $thisDialog); 	
										});
									}
								});
							},
							0
						);
				    },
			        error: function(XMLHttpResponse) {
						handleError(XMLHttpResponse, function() {
							handleErrorInDialog(XMLHttpResponse, $thisDialog);	
						});
			        }
			    });
			},
			"Cancel": function() { 
				var $thisDialog = $(this);
				$thisDialog.dialog("close"); 
			}
		}).dialog("open"); 
		return false;
	});
}


function initAddPrimaryStorageShortcut($midmenuAddLink2, currentPageInRightPanel) { 
	var $dialogAddPool = $("#dialog_add_pool_in_resource_page");    
	
    // if hypervisor is KVM, limit the server option to NFS for now
    if (getHypervisorType() == 'kvm') 
	    $dialogAddPool.find("#add_pool_protocol").empty().html('<option value="nfs">NFS</option>');	
    bindEventHandlerToDialogAddPool($dialogAddPool);	
    
    $.ajax({
        data: createURL("command=listZones&available=true"),
	    dataType: "json",
	    success: function(json) {
		    var zones = json.listzonesresponse.zone;
		    var zoneSelect = $dialogAddPool.find("#zone_dropdown").empty();								
		    if (zones != null && zones.length > 0) {
			    for (var i = 0; i < zones.length; i++) 
				    zoneSelect.append("<option value='" + zones[i].id + "'>" + fromdb(zones[i].name) + "</option>"); 				    
		    }	
		    //$dialogAddPool.find("#zone_dropdown").change();	//comment out to avoid race condition, do it before dialog box pops up	    
	    }
    });
	
    $dialogAddPool.find("#zone_dropdown").bind("change", function(event) {
	    var zoneId = $(this).val();
	    $.ajax({
	        data: createURL("command=listPods&zoneId="+zoneId),
		    dataType: "json",
		    async: false,
		    success: function(json) {
			    var pods = json.listpodsresponse.pod;
			    var podSelect = $dialogAddPool.find("#pod_dropdown").empty();	
			    if (pods != null && pods.length > 0) {
				    for (var i = 0; i < pods.length; i++) {
					    podSelect.append("<option value='" + pods[i].id + "'>" + fromdb(pods[i].name) + "</option>"); 
				    }
			    }
			    $dialogAddPool.find("#pod_dropdown").change();
		    }
	    });
    });

    var mapClusters = {};
    $dialogAddPool.find("#pod_dropdown").bind("change", function(event) {			   
        var podId = $(this).val();
        if(podId == null || podId.length == 0)
            return;
        var $clusterSelect = $dialogAddPool.find("#cluster_select").empty();		        
        $.ajax({
	       data: createURL("command=listClusters&podid=" + podId),
            dataType: "json",
            async: false,
            success: function(json) {			            
        		mapClusters = {};
                var items = json.listclustersresponse.cluster;
                if(items != null && items.length > 0) {			                
                    for(var i=0; i<items.length; i++) {
	                	mapClusters["cluster_"+items[i].id] = items[i];
                        $clusterSelect.append("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>");	
                    }
                    
	                if(!$clusterSelect.val())
	                	$("option", $clusterSelect)[0].attr("selected", "selected");
	                $clusterSelect.change();
	                
                    $dialogAddPool.find("input[value=existing_cluster_radio]").attr("checked", true);
                }
                else {
				    $clusterSelect.append("<option value='-1'>None Available</option>");
                    $dialogAddPool.find("input[value=new_cluster_radio]").attr("checked", true);
                }
            }
        });
    });        
    
    $dialogAddPool.find("#cluster_select").change(function() {
    	var curOption = $(this).val();
    	if(!curOption)
    		return false;
    	
    	var $protocolSelector = $("#add_pool_protocol", $dialogAddPool);
    	var objCluster = mapClusters['cluster_'+curOption];
    	
    	if(objCluster.hypervisortype == "KVM") {
    		$protocolSelector.empty();
    		$protocolSelector.append('<option value="nfs">NFS</option>');
    	} else if(objCluster.hypervisortype == "XenServer") {
    		$protocolSelector.empty();
			$protocolSelector.append('<option value="nfs">NFS</option>');
			$protocolSelector.append('<option value="iscsi">ISCSI</option>');
    	} else if(objCluster.hypervisortype == "VmWare") {
    		$protocolSelector.empty();
			$protocolSelector.append('<option value="nfs">NFS</option>');
			$protocolSelector.append('<option value="vmfs">VMFS datastore</option>');
    	}
    	
    	$protocolSelector.change();
    }).change();
    
       
    $("#add_primarystorage_shortcut").unbind("click").bind("click", function(event) { 
        $dialogAddPool.find("#zone_dropdown").change(); //refresh cluster dropdown (do it here to avoid race condition)     
        $dialogAddPool.find("#info_container").hide();	
                       
        $("#dialog_add_pool_in_resource_page")
	    .dialog('option', 'buttons', { 				    
		    "Add": function() { 	
		    	var $thisDialog = $(this);
		    	
			    // validate values
				var protocol = $thisDialog.find("#add_pool_protocol").val();
				
			    var isValid = true;		
			    isValid &= validateDropDownBox("Zone", $thisDialog.find("#zone_dropdown"), $thisDialog.find("#zone_dropdown_errormsg"));	
		        isValid &= validateDropDownBox("Pod", $thisDialog.find("#pod_dropdown"), $thisDialog.find("#pod_dropdown_errormsg"));						    
			    isValid &= validateDropDownBox("Cluster", $thisDialog.find("#cluster_select"), $thisDialog.find("#cluster_select_errormsg"), false);  //required, reset error text					    				
			    isValid &= validateString("Name", $thisDialog.find("#add_pool_name"), $thisDialog.find("#add_pool_name_errormsg"));
				if (protocol == "nfs") {
				    isValid &= validateString("Server", $thisDialog.find("#add_pool_nfs_server"), $thisDialog.find("#add_pool_nfs_server_errormsg"));	
					isValid &= validateString("Path", $thisDialog.find("#add_pool_path"), $thisDialog.find("#add_pool_path_errormsg"));	
				} else if(protocol == "iscsi") {
				    isValid &= validateString("Server", $thisDialog.find("#add_pool_nfs_server"), $thisDialog.find("#add_pool_nfs_server_errormsg"));	
					isValid &= validateString("Target IQN", $thisDialog.find("#add_pool_iqn"), $thisDialog.find("#add_pool_iqn_errormsg"));	
					isValid &= validateString("LUN #", $thisDialog.find("#add_pool_lun"), $thisDialog.find("#add_pool_lun_errormsg"));	
				} else if(protocol == "vmfs") {
					isValid &= validateString("vCenter Datacenter", $thisDialog.find("#add_pool_vmfs_dc"), $thisDialog.find("#add_pool_vmfs_dc_errormsg"));	
					isValid &= validateString("vCenter Datastore", $thisDialog.find("#add_pool_vmfs_ds"), $thisDialog.find("#add_pool_vmfs_ds_errormsg"));	
				}
				isValid &= validateString("Tags", $thisDialog.find("#add_pool_tags"), $thisDialog.find("#add_pool_tags_errormsg"), true);	//optional
			    if (!isValid) 
			        return;
			        			    
				$thisDialog.find("#spinning_wheel").show()  
							
				var array1 = [];
				
				var zoneId = $thisDialog.find("#zone_dropdown").val();
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
				} else if(protocol == "vmfs") {
					var path = trim($thisDialog.find("#add_pool_vmfs_dc").val());
					if(path.substring(0,1)!="/")
						path = "/" + path; 
					path += "/" + trim($thisDialog.find("#add_pool_vmfs_ds").val())
					url = vmfsURL("dummy", path);
				} else {
					var iqn = trim($thisDialog.find("#add_pool_iqn").val());
					if(iqn.substring(0,1)!="/")
						iqn = "/" + iqn; 
					var lun = trim($thisDialog.find("#add_pool_lun").val());
					url = iscsiURL(server, iqn, lun);
				}
				array1.push("&url="+todb(url));
				
			    var tags = trim($thisDialog.find("#add_pool_tags").val());
				if(tags != null && tags.length > 0)
				    array1.push("&tags="+todb(tags));				    
			    
			    $.ajax({
				    data: createURL("command=createStoragePool" + array1.join("")),
				    dataType: "json",
				    success: function(json) {	
				        var $clusterNode = $("#cluster_"+clusterId);
				        if($clusterNode.length > 0)
				  	        $("#cluster_"+clusterId).find("#cluster_name").click();		
				  	    else  //pod node is close. Expand pod node.	
				  	        refreshClusterUnderPod($("#pod_" + podId), null, clusterId);					    
					   	                    
	                    var primarystorageTotal = parseInt($("#primarystorage_total").text());
		                primarystorageTotal++;
		                $("#primarystorage_total").text(primarystorageTotal.toString());				    	
	                    
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