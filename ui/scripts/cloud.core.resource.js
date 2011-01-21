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
    var $loadingContainer = $("#leftmenu_physical_resource").find("#loading_container").show();
    var $arrowIcon = $("#leftmenu_physical_resource").find("#physical_resource_arrow").hide();
    
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
			$zoneTree.show();
			
			$loadingContainer.hide();
			$arrowIcon.show();            
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
			    var $loadingContainer = $zoneNode.find("#zone_loading_container").show();
                var $zoneArrow = $zoneNode.find("#zone_arrow").hide();			
			    
			    var zoneObj = $zoneNode.data("jsonObj");
			    var $zoneContent = $zoneNode.find("#zone_content");				  	   
				if(target.hasClass("expanded_close")) {										
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
                    target.removeClass("expanded_close").addClass("expanded_open");							
					$zoneContent.show();					                   
				} 
				else if(target.hasClass("expanded_open")) {	
				    $zoneContent.find("#pods_container").empty();					
					target.removeClass("expanded_open").addClass("expanded_close");					
					$zoneContent.hide();					
				}	
				
				$loadingContainer.hide();
                $zoneArrow.show();				
							
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
                    zoneRefreshDataBinding();                     	 
                }
			   	    		   				    		   			    
			    break;	
		}
		return false;
	});  
	
	$("#secondarystorage_header").unbind("click").bind("click", function(event) {	   
	    selectRowInZoneTree($(this));	
	    
	    clearMiddleMenu();
	    hideMiddleMenu();	
	    
        if(currentRightPanelJSP != "jsp/secondarystorage.jsp") {            
            removeDialogs();
            
            var $thisNode = $(this);    
            $("#right_panel").load("jsp/secondarystorage.jsp", function(){     
                currentRightPanelJSP = "jsp/secondarystorage.jsp";                 
                 
                /*                      
                $(this).data("onRefreshFn", function() {		        
                    var zoneObj = $midmenuItem1.data("jsonObj");
                    if(zoneObj == null)
                        return;
                    $("#zone_"+zoneObj.id).find("#secondarystorage_header").click();
                }); 
                */
                
                afterLoadSecondaryStorageJSP($thisNode);                       
            });      
        } 
        else {
            secondaryStorageRefreshDataBinding(); 
        }	    
	       
	    return false;
	});
	
	$("#network_header").unbind("click").bind("click", function(event) {	   
	    selectRowInZoneTree($(this));	
	    
	    clearMiddleMenu();
	    showMiddleMenu();	
	    
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
			    var $loadingContainer = $podNode.find("#pod_loading_container").show();
                var $podArrow = $podNode.find("#pod_arrow").hide();
						    		    
			    var podObj = $podNode.data("jsonObj");
			    var $podContent = $podNode.find("#pod_content");					 	   
				if(target.hasClass("expanded_close")) {						
					target.removeClass("expanded_close").addClass("expanded_open");									
					$podContent.show();					
					$.ajax({
                        data: createURL("command=listClusters&podid="+podObj.id),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                            var items = json.listclustersresponse.cluster;  
                            var container = $podContent.find("#clusters_container").empty();
                            if (items != null && items.length > 0) {	                                				    
                                for (var i = 0; i < items.length; i++) {
                                    $clusterNode = $("#leftmenu_cluster_node_template").clone(true); 
                                    var item = items[i];
                                    clusterJSONToTreeNode(item, $clusterNode);
                                    container.append($clusterNode.show());  
                                }  
                            }            
                        }
                    });	
				} 
				else if(target.hasClass("expanded_open")) {					
					target.removeClass("expanded_open").addClass("expanded_close");	
					$podContent.hide();		
					$podContent.find("#clusters_container").empty();									
				}
				
				$loadingContainer.hide();
                $podArrow.show();
					
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
		
	$("#leftmenu_host_node_template").unbind("click").bind("click", function(event) {
	    selectRowInZoneTree($(this).find("#host_header"));	    
	   	  
	   	hostClearRightPanel();    	    
	    var clusterObj = $(this).data("clusterObj");
        listMidMenuItems(("listHosts&type=Routing&clusterid="+clusterObj.id), hostGetSearchParams, "listhostsresponse", "host", "jsp/host.jsp", afterLoadHostJSP, hostToMidmenu, hostToRightPanel, getMidmenuId, false, ("cluster_"+clusterObj.id+"_host"), hostRefreshDataBinding);    
	    	    
	    return false;
	});  
	
	$("#leftmenu_primarystorage_node_template").unbind("click").bind("click", function(event) {
	    selectRowInZoneTree($(this).find("#primarystorage_header"));	    
	   	  
	   	primarystorageClearRightPanel();    	    
	    var clusterObj = $(this).data("clusterObj");
        listMidMenuItems(("listStoragePools&clusterid="+clusterObj.id), primarystorageGetSearchParams, "liststoragepoolsresponse", "storagepool", "jsp/primarystorage.jsp", afterLoadPrimaryStorageJSP, primarystorageToMidmenu, primarystorageToRightPanel, getMidmenuId, false, ("cluster_"+clusterObj.id+"_primarystorage"), primaryStorageRefreshDataBinding);    
	    	    
	    return false;
	});
	
	$("#leftmenu_cluster_node_template").unbind("click").bind("click", function(event) {
		var $thisNode = $(this);
		var $target = $(event.target);
		var targetId = $target.attr("id");	
		
		switch (targetId) {
			case "cluster_arrow" :				    
			    var $loadingContainer = $thisNode.find("#cluster_loading_container").show();
                var $clusterArrow = $thisNode.find("#cluster_arrow").hide();	
			    var clusterObj = $thisNode.data("jsonObj");
			    var $clusterContent = $thisNode.find("#cluster_content");				  	   
				if($target.hasClass("expanded_close")) {						  
                    $target.removeClass("expanded_close").addClass("expanded_open");							
					$clusterContent.show();					                   
				} 
				else if($target.hasClass("expanded_open")) {	
				    $clusterContent.find("#pods_container").empty();					
					$target.removeClass("expanded_open").addClass("expanded_close");					
					$clusterContent.hide();					
				}	
				
				$loadingContainer.hide();
                $clusterArrow.show();				
							
				break;					
			
			default:			   			    		    
			    selectRowInZoneTree($thisNode.find("#cluster_header"));	
			    
			    if(currentRightPanelJSP != "jsp/cluster.jsp") {                       
	                removeDialogs();
	                	                               
                    $("#right_panel").load("jsp/cluster.jsp", function(){                            
                        currentRightPanelJSP = "jsp/cluster.jsp";
                            
                        var $topButtonContainer = clearButtonsOnTop();			    	       
		                $("#top_buttons").appendTo($topButtonContainer);       
                                        
                        $thisNode.data("onRefreshFn", function() {
	                        clusterJsonToDetailsTab();
	                    });  
            	        
                        afterLoadClusterJSP($thisNode);   
                        clusterToRightPanel($thisNode);	
                    });      
                } 
                else {
                    clusterToRightPanel($thisNode);			 
                }                
			   	    		   				    		   			    
			    break;	
		}
		return false;
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
    $zoneNode.find("#secondarystorage_header").data("zoneObj", jsonObj);    
    
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
		
	$.ajax({
        data: createURL("command=listClusters&podid="+podid),
	    dataType: "json",
	    async: false,
	    success: function(json) {
		    var items = json.listclustersresponse.cluster;	
		    if (items != null && items.length > 0) {					    
			    $podNode.find("#pod_arrow").removeClass("white_nonexpanded_close").addClass("expanded_close"); 			    
			}	    		    
	    }
    });		
}
		
function clusterJSONToTreeNode(json, $clusterNode) {
    $clusterNode.attr("id", "cluster_"+json.id);
    $clusterNode.data("jsonObj", json);	 
    $clusterNode.find("#leftmenu_host_node_template").data("clusterObj", json).attr("id",("cluster_"+json.id+"_host"));	  
    $clusterNode.find("#leftmenu_primarystorage_node_template").data("clusterObj", json).attr("id",("cluster_"+json.id+"_primarystorage"));	  
    $clusterNode.data("id", json.id).data("name", fromdb(json.name));	    
    var clusterName = $clusterNode.find("#cluster_name").text(fromdb(json.name));
    clusterName.data("jsonObj", json);	 
}			

function hostJSONToTreeNode(json, $node) {
    $node.attr("id", "host_"+json.id);
    $node.data("jsonObj", json);	
    var hostName = $node.find("#host_name").text(fromdb(json.name));    
}	

function primarystorageJSONToTreeNode(json, $node) {
    $node.attr("id", "primarystorage_"+json.id);
    $node.data("jsonObj", json);	
    var primarystorageName = $node.find("#primarystorage_name").text(fromdb(json.name));    
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
	initDialog("dialog_add_external_cluster_in_resource_page", 320);
    initDialog("dialog_add_host_in_resource_page");  
    initDialog("dialog_add_pool_in_resource_page");
	
	listZonesUpdate();
		
	initAddPodShortcut();
	initAddClusterShortcut();
	initAddHostShortcut();
	initAddPrimaryStorageShortcut();
		
	resourceCountTotal();	  
}

function listZonesUpdate() {
    var $dialogs = $("#dialog_add_pod_in_resource_page,#dialog_add_external_cluster_in_resource_page,#dialog_add_host_in_resource_page,#dialog_add_pool_in_resource_page");
       
    $.ajax({
	    data: createURL("command=listZones&available=true"),
	    dataType: "json",
	    async: false,
	    success: function(json) {
	        var items = json.listzonesresponse.zone;			
			if (items != null && items.length > 0) {
			    $("#zone_total").text(items.length.toString());			   
			    for(var i=0; i<items.length; i++) {		   			    
			        $dialogs.find("#zone_dropdown").append("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>");
			    }			         
			}	
	    }
	}); 
}

function initAddPodShortcut() {
    var $dialogAddPod = $("#dialog_add_pod_in_resource_page");

    var $zoneDropdown = $dialogAddPod.find("#zone_dropdown");
    /*
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
    */
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

function initAddClusterShortcut() {
    var $dialogAddCluster = $("#dialog_add_external_cluster_in_resource_page");

    var $zoneDropdown = $dialogAddCluster.find("#zone_dropdown");
    var $podDropdown = $dialogAddCluster.find("#pod_dropdown");    	
    
    /*
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
    */
        
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
                    $dialogAddCluster.find("#guestip_container, #guestnetmask_container, #guestgateway_container").show();
                }
                else if(zoneObj.networktype == "Advanced") { //advanced-mode network (zone-wide VLAN)
                    $dialogAddCluster.find("#guestip_container, #guestnetmask_container, #guestgateway_container").hide();     
                }  	                    
	        }
	    });		    	   
	    $.ajax({
            data: createURL("command=listPods&zoneid="+zoneId),
            dataType: "json",
            async: false,
            success: function(json) {            
                var pods = json.listpodsresponse.pod;   
                $podDropdown.empty(); 
                if(pods != null && pods.length > 0) {
                    for(var i=0; i<pods.length; i++)
                        $podDropdown.append("<option value='" + pods[i].id + "'>" + fromdb(pods[i].name) + "</option>"); 	
                }                 
            }        
        });  	    
    });        
  
    var $hypervisorDropdown = $dialogAddCluster.find("#cluster_hypervisor");    
    $hypervisorDropdown.change(function() {
        if($(this).val() == "VmWare") {
    		$('li[input_group="vmware"]', $dialogAddCluster).show();
    		$dialogAddCluster.find("#type_dropdown").change();
    	} else {
    		$('li[input_group="vmware"]', $dialogAddCluster).hide();
    		$("#cluster_name_label", $dialogAddCluster).text("Cluster:");
    	}
    });
    
    $dialogAddCluster.find("#type_dropdown").change(function() {
    	if($(this).val() == "ExternalManaged") {
    		$('li[input_sub_group="external"]', $dialogAddCluster).show();
    		$("#cluster_name_label", $dialogAddCluster).text("vCenter Cluster:");
    	} else {
    		$('li[input_sub_group="external"]', $dialogAddCluster).hide();
    		$("#cluster_name_label", $dialogAddCluster).text("Cluster:");
    	}
    });
            
    $("#add_cluster_shortcut").unbind("click").bind("click", function(event) {          
        $dialogAddCluster.find("#info_container").hide();          
         
        $zoneDropdown.change();          
        $hypervisorDropdown.change();         
                
        $dialogAddCluster.dialog('option', 'buttons', { 				
	        "Add": function() { 
	            var $thisDialog = $(this);		            
	            			   
		        // validate values
			    var hypervisor = $thisDialog.find("#cluster_hypervisor").val();
			    var clusterType="CloudManaged";
			    if(hypervisor == "VmWare")
			    	clusterType = $thisDialog.find("#type_dropdown").val();
	            
		        var isValid = true;		        
		        isValid &= validateDropDownBox("Zone", $thisDialog.find("#zone_dropdown"), $thisDialog.find("#zone_dropdown_errormsg"));	
		        isValid &= validateDropDownBox("Pod", $thisDialog.find("#pod_dropdown"), $thisDialog.find("#pod_dropdown_errormsg"));	       
		        if(hypervisor == "VmWare" && clusterType != "CloudManaged") {
			        isValid &= validateString("vCenter Server", $thisDialog.find("#cluster_hostname"), $thisDialog.find("#cluster_hostname_errormsg"));
			        isValid &= validateString("vCenter user", $thisDialog.find("#cluster_username"), $thisDialog.find("#cluster_username_errormsg"));
			        isValid &= validateString("Password", $thisDialog.find("#cluster_password"), $thisDialog.find("#cluster_password_errormsg"));	
			        isValid &= validateString("Datacenter", $thisDialog.find("#cluster_datacenter"), $thisDialog.find("#cluster_datacenter_errormsg"));	
		        }
		        isValid &= validateString("Cluster name", $thisDialog.find("#cluster_name"), $thisDialog.find("#cluster_name_errormsg"));	
		        if (!isValid) 
		            return;
		            				
				$thisDialog.find("#spinning_wheel").show(); 				
				
		        var array1 = [];
			    array1.push("&hypervisor="+hypervisor);
			    array1.push("&clustertype=" + clusterType);
			    
			    var zoneId = $thisDialog.find("#zone_dropdown").val();;
		        array1.push("&zoneId="+zoneId);
		        
		        //expand zone in left menu tree (to show pod, cluster under the zone) 
				var $zoneNode = $("#leftmenu_zone_tree").find("#tree_container").find("#zone_" + zoneId);							
				if($zoneNode.find("#zone_arrow").hasClass("expanded_close"))
				    $zoneNode.find("#zone_arrow").click();
										             
		        var podId = $thisDialog.find("#pod_dropdown").val();
		        array1.push("&podId="+podId);

		        var clusterName = trim($thisDialog.find("#cluster_name").val());
		        if(hypervisor == "VmWare" && clusterType != "CloudManaged") {
			        var username = trim($thisDialog.find("#cluster_username").val());
			        array1.push("&username="+todb(username));
					
			        var password = trim($thisDialog.find("#cluster_password").val());
			        array1.push("&password="+todb(password));
			        
			        var hostname = trim($thisDialog.find("#cluster_hostname").val());
			        var dcName = trim($thisDialog.find("#cluster_datacenter").val());
			        var url;					
			        if(hostname.indexOf("http://")==-1)
			            url = "http://" + todb(hostname);
			        else
			            url = hostname;
			        url += "/" + todb(dcName) + "/" + todb(clusterName);
			        array1.push("&url=" + todb(url));
			        
			        clusterName = hostname + "/" + dcName + "/" + clusterName
		        } 
		        
		        array1.push("&clustername=" + todb(clusterName));
									
		        $.ajax({
			       data: createURL("command=addCluster" + array1.join("")),
			        dataType: "json",
			        success: function(json) {
			            $thisDialog.find("#spinning_wheel").hide();
			            $thisDialog.dialog("close");
					
					    var item = json.addclusterresponse.cluster[0];                                                                   
                        var $podNode = $("#pod_" + podId);
                        if($podNode.length > 0 && $podNode.css("display") != "none") {
                            if($podNode.find("#pod_arrow").hasClass("white_nonexpanded_close")) {
                                $podNode.find("#pod_arrow").removeClass("white_nonexpanded_close").addClass("expanded_close");    
                            }
                            
                            if($podNode.find("#pod_arrow").hasClass("expanded_close")) { //if pod node is closed
                                $podNode.find("#pod_arrow").click(); //expand pod node
                                var $clusterNode = $podNode.find("#cluster_"+item.id);
                                $clusterNode.find("#cluster_arrow").click(); //expand cluster node to see host node and storage node   
                                $clusterNode.find("#cluster_name").click();  //click cluster node to show cluster info
                            }
                            else { //if pod node is expanded                                
                                var $clusterNode = $("#leftmenu_cluster_node_template").clone(true);  
                                clusterJSONToTreeNode(item, $clusterNode);
                                $podNode.find("#clusters_container").append($clusterNode.show());   
                                $clusterNode.find("#cluster_arrow").click(); //expand cluster node to see host node and storage node   
                                $clusterNode.find("#cluster_name").click();  //click cluster node to show cluster info                                                            
                            }                                      
                        }
					
					    var clusterTotal = parseInt($("#cluster_total").text());
		                clusterTotal++;
		                $("#cluster_total").text(clusterTotal.toString());                                            
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
    var $podSelect = $dialogAddHost.find("#pod_dropdown");
    
    /*
    $dialogAddHost.find("#host_hypervisor").change(function() {
        if($(this).val() == "VmWare") {
    		$('li[input_group="general"]', $dialogAddHost).hide();
    		$('li[input_group="vmware"]', $dialogAddHost).show();
    	} else {
    		$('li[input_group="vmware"]', $dialogAddHost).hide();
    		$('li[input_group="general"]', $dialogAddHost).show();
    	}
    	
        refreshClsuterFieldInAddHostDialog($dialogAddHost, $podSelect.val(), null, $(this).val());        
    }); 
    */
        
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
	    
    $dialogAddHost.find("#pod_dropdown").unbind("change").bind("change", function(event) {    	   
        refreshClsuterFieldInAddHostDialog($dialogAddHost, $dialogAddHost.find("#pod_dropdown").val(), null);
        $dialogAddHost.find("#cluster_select").change();       
    });  
    
    $dialogAddHost.find("#cluster_select").unbind("change").bind("change", function(event) {        
        var clusterId = $(this).val();            
        if(clusterId == null)
            return;        
        var clusterObj = clustersUnderOnePod[clusterId];                        
    	if(clusterObj.hypervisortype == "VmWare") {
    		$('li[input_group="vmware"]', $dialogAddHost).show();
    		$('li[input_group="general"]', $dialogAddHost).hide();
    	} else {
    		$('li[input_group="vmware"]', $dialogAddHost).hide();
    		$('li[input_group="general"]', $dialogAddHost).show();
    	}   
    });
        
    $("#add_host_shortcut").unbind("click").bind("click", function(event) {               
        $dialogAddHost.find("#info_container").hide();         
        $dialogAddHost.find("#zone_dropdown").change(); 
       
        $dialogAddHost
        .dialog('option', 'buttons', { 				
	        "Add": function() { 
	            var $thisDialog = $(this);		            
	            			   
		        //var clusterRadio = $thisDialog.find("input[name=cluster]:checked").val();				
			
		        // validate values
		        var isValid = true;	
		        isValid &= validateDropDownBox("Zone", $thisDialog.find("#zone_dropdown"), $thisDialog.find("#zone_dropdown_errormsg"));	
		        isValid &= validateDropDownBox("Pod", $thisDialog.find("#pod_dropdown"), $thisDialog.find("#pod_dropdown_errormsg"));	
		        isValid &= validateDropDownBox("Cluster", $thisDialog.find("#cluster_select"), $thisDialog.find("#cluster_select_errormsg"), false);  //required, reset error text					    				
		        
		        var clusterId = $thisDialog.find("#cluster_select").val();	
				var clusterObj, hypervisor;
				if(clusterId != null) {
				    clusterObj = clustersUnderOnePod[clusterId];    
                    hypervisor = clusterObj.hypervisortype;  		        
		            if(hypervisor == "VmWare") {
			            isValid &= validateString("vCenter Address", $thisDialog.find("#host_vcenter_address"), $thisDialog.find("#host_vcenter_address_errormsg"));
			            isValid &= validateString("vCenter User", $thisDialog.find("#host_vcenter_username"), $thisDialog.find("#host_vcenter_username_errormsg"));
			            isValid &= validateString("vCenter Password", $thisDialog.find("#host_vcenter_password"), $thisDialog.find("#host_vcenter_password_errormsg"));	
			            isValid &= validateString("vCenter Datacenter", $thisDialog.find("#host_vcenter_dc"), $thisDialog.find("#host_vcenter_dc_errormsg"));	
			            isValid &= validateString("vCenter Host", $thisDialog.find("#host_vcenter_host"), $thisDialog.find("#host_vcenter_host_errormsg"));	
		            } else {
			            isValid &= validateString("Host name", $thisDialog.find("#host_hostname"), $thisDialog.find("#host_hostname_errormsg"));
			            isValid &= validateString("User name", $thisDialog.find("#host_username"), $thisDialog.find("#host_username_errormsg"));
			            isValid &= validateString("Password", $thisDialog.find("#host_password"), $thisDialog.find("#host_password_errormsg"));	
		            }	
		        }        
		        if (!isValid) 
		            return;		            			
					
				$thisDialog.find("#spinning_wheel").show() 				
				
		        var array1 = [];
				
				/*
			    var hypervisor = $thisDialog.find("#host_hypervisor").val();
			    if(hypervisor.length > 0)
				    array1.push("&hypervisor="+hypervisor);
		        */
		        
		        var zoneId = $thisDialog.find("#zone_dropdown").val();
		        array1.push("&zoneid="+zoneId);
		        
		        //expand zone in left menu tree (to show pod, cluster under the zone) 
				var $zoneNode = $("#leftmenu_zone_tree").find("#tree_container").find("#zone_" + zoneId);							
				if($zoneNode.find("#zone_arrow").hasClass("expanded_close"))
				    $zoneNode.find("#zone_arrow").click();
								        
		        var podId = $thisDialog.find("#pod_dropdown").val();
		        array1.push("&podid="+podId);
						      
	            var clusterId = $thisDialog.find("#cluster_select").val();			    
			    array1.push("&clusterid="+clusterId);			    		        			
                 
			    array1.push("&hypervisor="+hypervisor);			    
			    var clustertype = clusterObj.clustertype;
                array1.push("&clustertype=" + clustertype);				    

			    if(hypervisor == "VmWare") {
			        var username = trim($thisDialog.find("#host_vcenter_username").val());
			        array1.push("&username="+todb(username));
					
			        var password = trim($thisDialog.find("#host_vcenter_password").val());
			        array1.push("&password="+todb(password));
				    
			        var hostname = trim($thisDialog.find("#host_vcenter_address").val());
			        hostname += "/" + trim($thisDialog.find("#host_vcenter_dc").val());
			        hostname += "/" + trim($thisDialog.find("#host_vcenter_host").val());
			        
			        var url;					
			        if(hostname.indexOf("http://")==-1)
			            url = "http://" + todb(hostname);
			        else
			            url = hostname;
			        array1.push("&url="+todb(url));
			    	
			    } else {
			        var username = trim($thisDialog.find("#host_username").val());
			        array1.push("&username="+todb(username));
					
			        var password = trim($thisDialog.find("#host_password").val());
			        array1.push("&password="+todb(password));
				    
			        var hostname = trim($thisDialog.find("#host_hostname").val());
			        var url;					
			        if(hostname.indexOf("http://")==-1)
			            url = "http://" + todb(hostname);
			        else
			            url = hostname;
			        array1.push("&url="+todb(url));
			    }
				      
		        $.ajax({
			       data: createURL("command=addHost" + array1.join("")),
			        dataType: "json",
			        success: function(json) {
			            $thisDialog.find("#spinning_wheel").hide();
			            $thisDialog.dialog("close");
								    
					    var items = json.addhostresponse.host;			  
		                var hostTotal = parseInt($("#host_total").text());
		                hostTotal = hostTotal + items.length;
		                $("#host_total").text(hostTotal.toString());		                
                                                
			        },			
                    error: function(XMLHttpResponse) {	
						handleError(XMLHttpResponse, function() {							 
							refreshClsuterFieldInAddHostDialog($thisDialog, podId, null);                     
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

function initAddZoneLinks() {     
    $("#add_zone_shortcut,#add_zone_button").unbind("click").bind("click", function(event) {              
        var $arrowIcon = $("#leftmenu_physical_resource").find("#physical_resource_arrow");
        if($arrowIcon.hasClass("expanded_close") == true)
			$arrowIcon.click(); //if Physical Resource arrow shows closed (i.e. zonetree is hidden), expand and show zonetree.    
			       
        openAddZoneWizard();
        return false;
    });            
}

function resourceCountTotal() {		
	/*
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
	*/
	
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
	    data: createURL("command=listClusters"),
	    dataType: "json",
	    success: function(json) {
	        var items = json.listclustersresponse.cluster;		    			
			if (items != null) {			   			    
			    $("#cluster_total").text(items.length.toString());
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
    $addZoneWizard.find("#step2, #step3, #step4, #after_submit_screen").hide();
    $addZoneWizard.find("#step1").show();
    
    $addZoneWizard.find("#step4").find("#add_publicip_vlan_tagged").change();            		
	$addZoneWizard.find("#step4").find("#add_publicip_vlan_scope").change(); // default value of "#add_publicip_vlan_scope" is "zone-wide". Calling change() will hide "#add_publicip_vlan_domain_container", "#add_publicip_vlan_account_container". 	
     
    $addZoneWizard.find("#after_submit_screen").find("#spinning_wheel").show();
	            
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
         
    $addZoneWizard.find("#step4").find("#add_publicip_vlan_tagged").unbind("change").bind("change", function(event) {	
		if ($(this).val() == "tagged") {
			$addZoneWizard.find("#step4").find("#add_publicip_vlan_vlan_container").show();
			$addZoneWizard.find("#step4").find("#add_publicip_vlan_pod_container").hide();
							
			$addZoneWizard.find("#step4").find("#add_publicip_vlan_scope").empty().append('<option value="zone-wide">zone-wide</option>').append('<option value="account-specific">account-specific</option>');
		} 
		else if($(this).val() == "untagged") {  
			$addZoneWizard.find("#step4").find("#add_publicip_vlan_vlan_container").hide();
			$addZoneWizard.find("#step4").find("#add_publicip_vlan_pod_container").hide();
			
			$addZoneWizard.find("#step4").find("#add_publicip_vlan_scope").empty().append('<option value="zone-wide">zone-wide</option>');				
		}	
		
		$addZoneWizard.find("#step4").find("#add_publicip_vlan_scope").change();		
		return false;
	});
	
	$addZoneWizard.find("#step4").find("#add_publicip_vlan_scope").change(function(event) {	   
	    if($(this).val() == "zone-wide") {
	        $addZoneWizard.find("#step4").find("#add_publicip_vlan_domain_container").hide();
			$addZoneWizard.find("#step4").find("#add_publicip_vlan_account_container").hide();    
	    } 
	    else if($(this).val() == "account-specific") { 
	        $addZoneWizard.find("#step4").find("#add_publicip_vlan_domain_container").show();
			$addZoneWizard.find("#step4").find("#add_publicip_vlan_account_container").show();    
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
                $thisWizard.find("#step2").find("#add_zone_vlan_container, #add_zone_guestcidraddress_container").hide();
                
                //$thisWizard.find("#step3").find("#guestip_container, #guestnetmask_container, #guestgateway_container").show();     
                $thisWizard.find("#step4").find("#guestip_list").show();
                $thisWizard.find("#step4").find("#publicip_list").hide();           
                return true;
                break;
                
            case "Advanced":  //create VLAN in zone-level 
                //show Zone VLAN Range in Add Zone(step 2), hide Guest IP Range in Add Pod(step3) 
                $thisWizard.find("#step2").find("#add_zone_vlan_container, #add_zone_guestcidraddress_container").show();  
                
                //$thisWizard.find("#step3").find("#guestip_container, #guestnetmask_container, #guestgateway_container").hide();                  
                $thisWizard.find("#step4").find("#guestip_list").hide();
                $thisWizard.find("#step4").find("#publicip_list").show();                   
		        $addZoneWizard.find("#step4").find("#add_publicip_vlan_scope").change(); 		
                
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
                              
                var $IpRangeDomainSelect = $thisWizard.find("#step4").find("#add_publicip_vlan_domain").empty();	                
		        if($thisWizard.find("#step2").find("#domain_dropdown_container").css("display") != "none") { //list only domains under zoneDomain		
		            var zoneDomainId = $thisWizard.find("#step2").find("#domain_dropdown").val();  
		            var zoneDomainName = $thisWizard.find("#step2").find("#domain_dropdown option:selected").text();      		    
		            $IpRangeDomainSelect.append("<option value='" + zoneDomainId + "'>" + zoneDomainName + "</option>"); 	
                    function populateDomainDropdown(parentDomainId) {
                        $.ajax({
                            data: createURL("command=listDomainChildren&id="+parentDomainId),
                            dataType: "json",
                            async: false,
                            success: function(json) {					        
                                var domains = json.listdomainchildrenresponse.domain;		                  		        	    
                                if (domains != null && domains.length > 0) {					    
	                                for (var i = 0; i < domains.length; i++) {	
		                                $IpRangeDomainSelect.append("<option value='" + domains[i].id + "'>" + fromdb(domains[i].name) + "</option>"); 	
		                                if(domains[i].haschild == true) 
                                            populateDomainDropdown(domains[i].id);				   
	                                }
                                }				
                            }
                        }); 
                    }
                    populateDomainDropdown(zoneDomainId);
                }
                else { //list all domains            
                     $.ajax({
                        data: createURL("command=listDomains"),
                        dataType: "json",
                        success: function(json) {           
                            var items = json.listdomainsresponse.domain;
                            if(items != null && items.length > 0) {
                                for(var i=0; i<items.length; i++) {
                                    $IpRangeDomainSelect.append("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>"); 
                                }		
                            }
                        }    
                    });  
                }                  
                break;  
                           
            case "go_to_step_4": //step 3 => step 4                   
                var isValid = addZoneWizardValidatePod($thisWizard);                
                if (!isValid) 
	                return;               
                          
                $thisWizard.find("#step3").hide();
                $thisWizard.find("#step4").show();                
                break;  
           
            case "back_to_step_3": //step 4 => step 3
                $thisWizard.find("#step4").hide();
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
                        
            case "submit": //step 4 => make API call  
                var isValid = true;	          
                if($thisWizard.find("#step4").find("#guestip_list").css("display") != "none")
                    isValid = addZoneWizardValidateGuestIPRange($thisWizard); 
                if($thisWizard.find("#step4").find("#publicip_list").css("display") != "none")
                    isValid &= addZoneWizardValidatePublicIPRange($thisWizard);        
                if (!isValid) 
	                return;	  	                  
                
                $thisWizard.find("#step4").hide();
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
	if($thisWizard.find("#add_zone_guestcidraddress_container").css("display") != "none") {
	    isValid &= validateCIDR("Guest CIDR", $thisWizard.find("#add_zone_guestcidraddress"), $thisWizard.find("#add_zone_guestcidraddress_errormsg"), false); //required
	}
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

function addZoneWizardValidatePublicIPRange($thisWizard) {   
    var isValid = true;					
	var isTagged = $thisWizard.find("#step4").find("#add_publicip_vlan_tagged").val() == "tagged";	
	
	isValid &= validateString("Account", $thisWizard.find("#step4").find("#add_publicip_vlan_account"), $thisWizard.find("#step4").find("#add_publicip_vlan_account_errormsg"), true); //optional
	
	if (isTagged) {
		isValid &= validateInteger("VLAN", $thisWizard.find("#step4").find("#add_publicip_vlan_vlan"), $thisWizard.find("#step4").find("#add_publicip_vlan_vlan_errormsg"), 2, 4095);
	}
	
	isValid &= validateIp("Gateway", $thisWizard.find("#step4").find("#add_publicip_vlan_gateway"), $thisWizard.find("#step4").find("#add_publicip_vlan_gateway_errormsg"), false); //required
	isValid &= validateIp("Netmask", $thisWizard.find("#step4").find("#add_publicip_vlan_netmask"), $thisWizard.find("#step4").find("#add_publicip_vlan_netmask_errormsg"), false); //required
	isValid &= validateIp("Start IP Range", $thisWizard.find("#step4").find("#add_publicip_vlan_startip"), $thisWizard.find("#step4").find("#add_publicip_vlan_startip_errormsg"), false); //required
	isValid &= validateIp("End IP Range", $thisWizard.find("#step4").find("#add_publicip_vlan_endip"), $thisWizard.find("#step4").find("#add_publicip_vlan_endip_errormsg"), true); //optional
	
    return isValid;			
}

function addZoneWizardSubmit($thisWizard) {	
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
	
	if($thisWizard.find("#add_zone_guestcidraddress_container").css("display") != "none") {
	    var guestcidraddress = trim($thisWizard.find("#add_zone_guestcidraddress").val());
	    moreCriteria.push("&guestcidraddress="+todb(guestcidraddress));	
	}
					
	if($thisWizard.find("#step2").find("#domain_dropdown_container").css("display") != "none") {
	    var domainId = trim($thisWizard.find("#step2").find("#domain_dropdown").val());
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
		    
		    listZonesUpdate();
		    /*
		    var zoneTotal = parseInt($("#zone_total").text());
		    zoneTotal++;
		    $("#zone_total").text(zoneTotal.toString());	
		    */	           
	    },
        error: function(XMLHttpResponse) {            
			handleError(XMLHttpResponse, function() {
			    $thisWizard.find("#after_submit_screen").find("#add_zone_tick_cross").removeClass().addClass("zonepopup_reviewcross");
	            $thisWizard.find("#after_submit_screen").find("#add_zone_message").removeClass().addClass("error").text(("Failed to create zone: " + parseXMLHttpResponse(XMLHttpResponse)));					
			});
        }
    });
    
    if(zoneId != null) {   
        // create pod (begin) 
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
	        },
            error: function(XMLHttpResponse) {	
				handleError(XMLHttpResponse, function() {				    
				    $thisWizard.find("#after_submit_screen").find("#add_pod_tick_cross").removeClass().addClass("zonepopup_reviewcross");
	                $thisWizard.find("#after_submit_screen").find("#add_pod_message").removeClass().addClass("error").text(("Failed to create pod: " + parseXMLHttpResponse(XMLHttpResponse)));			
				});
            }
        });	
        // create pod (end) 
        
        // add guest IP range to basic zone (begin) 
        if($thisWizard.find("#step4").find("#guestip_list").css("display") != "none") {
            var netmask = $thisWizard.find("#step4").find("#guestip_list").find("#guestnetmask").val();
		    var startip = $thisWizard.find("#step4").find("#guestip_list").find("#startguestip").val();
		    var endip = $thisWizard.find("#step4").find("#guestip_list").find("#endguestip").val();	
		    var guestgateway = $thisWizard.find("#step4").find("#guestip_list").find("#guestgateway").val();
    				
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
			        $thisWizard.find("#after_submit_screen").find("#add_iprange_tick_cross").removeClass().addClass("zonepopup_reviewtick");
	                $thisWizard.find("#after_submit_screen").find("#add_iprange_message").removeClass().text("Guest IP range was created successfully");	    
    			    
				    var item = json.createvlaniprangeresponse.vlan;
				    vlanId = item.id;				
			    },		   
		        error: function(XMLHttpResponse) {	
				    handleError(XMLHttpResponse, function() {			
				        $thisWizard.find("#after_submit_screen").find("#add_iprange_tick_cross").removeClass().addClass("zonepopup_reviewcross");
	                    $thisWizard.find("#after_submit_screen").find("#add_iprange_message").removeClass().addClass("error").text(("Failed to create Guest IP range: " + parseXMLHttpResponse(XMLHttpResponse)));			
			        });
                }
		    });		            
        }
        // add guest IP range to basic zone (end) 
        
        // add public IP range to basic zone (begin) 
        if($thisWizard.find("#step4").find("#publicip_list").css("display") != "none") {   
            var isDirect = false;
			var isTagged = $thisWizard.find("#step4").find("#add_publicip_vlan_tagged").val() == "tagged";
			
			var vlan = trim($thisWizard.find("#step4").find("#add_publicip_vlan_vlan").val());
			if (isTagged) {
				vlan = "&vlan="+vlan;
			} else {
				vlan = "&vlan=untagged";
			}
							
			var scopeParams = "";
			if($thisWizard.find("#step4").find("#add_publicip_vlan_scope").val() == "account-specific") {
			    scopeParams = "&domainId="+trim($thisWizard.find("#step4").find("#add_publicip_vlan_domain").val())+"&account="+trim($thisWizard.find("#step4").find("#add_publicip_vlan_account").val());  
			} else if (isDirect) {
				scopeParams = "&isshared=true";
			}
			
			var array1 = [];						
			var gateway = $thisWizard.find("#step4").find("#add_publicip_vlan_gateway").val();
			array1.push("&gateway="+todb(gateway));
			
			var netmask = $thisWizard.find("#step4").find("#add_publicip_vlan_netmask").val();
			array1.push("&netmask="+todb(netmask));
			
			var startip = $thisWizard.find("#step4").find("#add_publicip_vlan_startip").val();
			array1.push("&startip="+todb(startip));
			
			var endip = $thisWizard.find("#step4").find("#add_publicip_vlan_endip").val();	//optional field (might be empty)
			if(endip != null && endip.length > 0)
			    array1.push("&endip="+todb(endip));										
			
			$.ajax({
				data: createURL("command=createVlanIpRange&forVirtualNetwork=true&zoneId="+zoneId+vlan+scopeParams+array1.join("")),
				dataType: "json",
				success: function(json) {			    
				    $thisWizard.find("#after_submit_screen").find("#add_iprange_tick_cross").removeClass().addClass("zonepopup_reviewtick");
	                $thisWizard.find("#after_submit_screen").find("#add_iprange_message").removeClass().text("Public IP range was created successfully");	
    			    
    			    var item = json.createvlaniprangeresponse.vlan;						    
				    vlanId = item.id;					    	    				   
				},
				error: function(XMLHttpResponse) {				    
					handleError(XMLHttpResponse, function() {
						$thisWizard.find("#after_submit_screen").find("#add_iprange_tick_cross").removeClass().addClass("zonepopup_reviewcross");
	                    $thisWizard.find("#after_submit_screen").find("#add_iprange_message").removeClass().addClass("error").text(("Failed to create public IP range: " + parseXMLHttpResponse(XMLHttpResponse)));			
					});
				}
			});            
        }
        // add public IP range to basic zone (end)  
    } 
        
    $thisWizard.find("#after_submit_screen").find("#spinning_wheel").hide();   
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
	// TODO: Fix this to use the hypervisor from the cluster
    //if (getHypervisorType() == 'kvm') 
	//    $dialogAddPool.find("#add_pool_protocol").empty().html('<option value="nfs">NFS</option>');	
    bindEventHandlerToDialogAddPool($dialogAddPool);	
    
    $dialogAddPool.find("#zone_dropdown").bind("change", function(event) {
	    var zoneId = $(this).val();
	    if(zoneId == null)
	        return;
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
    	
    	if(objCluster == null)
    	    return;
    	
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