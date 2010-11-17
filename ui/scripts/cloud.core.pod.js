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
 
 function afterLoadPodJSP($leftmenuItem1) {   
    hideMiddleMenu();	
          	
    initAddHostButton($("#midmenu_add_link"), "pod_page"); 
    initAddPrimaryStorageButton($("#midmenu_add2_link"), "pod_page");  
           
    initDialog("dialog_add_host");
    initDialog("dialog_add_pool");
    
    // if hypervisor is KVM, limit the server option to NFS for now
    if (getHypervisorType() == 'kvm') 
	    $("#dialog_add_pool").find("#add_pool_protocol").empty().html('<option value="nfs">NFS</option>');	
    bindEventHandlerToDialogAddPool();	 
    
    //switch between different tabs 
    var tabArray = [$("#tab_details"), $("#tab_network")];
    var tabContentArray = [$("#tab_content_details"), $("#tab_content_network")];
    var afterSwitchFnArray = [podJsonToDetailsTab, podJsonToNetworkTab];
    switchBetweenDifferentTabs(tabArray, tabContentArray, afterSwitchFnArray);       
   
	podJsonToRightPanel($leftmenuItem1);     	
}

function podJsonToRightPanel($leftmenuItem1) {	 
    $("#right_panel_content").data("$leftmenuItem1", $leftmenuItem1);  
    $("#tab_details").click();   
}

function podJsonToDetailsTab() {	
    var $thisTab = $("#right_panel_content #tab_content_details");  
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();        
    
    var $leftmenuItem1 = $("#right_panel_content").data("$leftmenuItem1");
    var jsonObj = $leftmenuItem1.data("jsonObj");
    $thisTab.data("jsonObj", jsonObj);  
     
    $thisTab.find("#id").text(noNull(jsonObj.id));
    $thisTab.find("#grid_header_title").text(fromdb(jsonObj.name));
    
    $thisTab.find("#name").text(fromdb(jsonObj.name));
    $thisTab.find("#name_edit").val(fromdb(jsonObj.name));
    
    $thisTab.find("#cidr").text(fromdb(jsonObj.cidr));   
    $thisTab.find("#cidr_edit").val(fromdb(jsonObj.cidr));   
         
    $thisTab.find("#ipRange").text(getIpRange(jsonObj.startip, jsonObj.endip));
    $thisTab.find("#startIpRange_edit").val(fromdb(jsonObj.startip));
    $thisTab.find("#endIpRange_edit").val(fromdb(jsonObj.endip));
    
    $thisTab.find("#gateway").text(fromdb(jsonObj.gateway));  
    $thisTab.find("#gateway_edit").val(fromdb(jsonObj.gateway));  
    
    
    // hide network tab upon zone vlan
    var zoneVlan;  
    $.ajax({
	    data: createURL("command=listZones&id="+noNull(jsonObj.zoneid)),
		dataType: "json",	
		async: false,	
		success: function(json) {
			var items = json.listzonesresponse.zone;						
			if (items != null && items.length > 0) {					    
				//zoneVlan = items[0].vlan;  //comment this one out until bug 7162 is fixed ("listZones API should take in id parameter")
				
				//temporary code before bug 7162 is fixed ********(begin)***********		
				for(var i=0; i<items.length; i++) {				   		    
				    if(items[i].id == jsonObj.zoneid) {
				        zoneVlan = items[i].vlan; 
				    }
				}	
				//temporary code before bug 7162 is fixed ********(end)*************	
			}				
		}
	});	
    if(zoneVlan == null) { //basic-mode network (pod-wide VLAN)
        $("#tab_network").show();  
        initAddPodVLANButton($("#midmenu_add3_link"));  
    }
    else { //advanced-mode network (zone-wide VLAN)
        $("#tab_network").hide();
        $("#midmenu_add3_link").unbind("click").hide();         
    }
    
    
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
    buildActionLinkForTab("Edit Pod", podActionMap, $actionMenu, $leftmenuItem1, $thisTab);  
    buildActionLinkForTab("Delete Pod", podActionMap, $actionMenu, $leftmenuItem1, $thisTab); 
    
    $thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();      
}	

function podJsonToNetworkTab() {   
    var $thisTab = $("#right_panel_content #tab_content_network");
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   
		
	var $leftmenuItem1 = $("#right_panel_content").data("$leftmenuItem1");	
	var jsonObj = $leftmenuItem1.data("jsonObj");	
        
    $.ajax({
		data: createURL("command=listVlanIpRanges&zoneid="+noNull(jsonObj.zoneid)+"&podid="+noNull(jsonObj.id)),
		dataType: "json",
		success: function(json) {			       
			var items = json.listvlaniprangesresponse.vlaniprange;
			var $container = $thisTab.find("#tab_container").empty();
			var template = $("#network_tab_template");	
			if (items != null && items.length > 0) {					    
				for (var i = 0; i < items.length; i++) {	
				    var newTemplate = template.clone(true);	               
	                podNetworkJsonToTemplate(items[i], newTemplate); 
	                $container.append(newTemplate.show());	
				}
			}
			$thisTab.find("#tab_spinning_wheel").hide();    
            $thisTab.find("#tab_container").show();    	
		}			
	});			
} 

function podNetworkJsonToTemplate(jsonObj, template) {
    template.data("jsonObj", jsonObj);     
    template.attr("id", "pod_VLAN_"+noNull(jsonObj.id)).data("podVLANId", noNull(jsonObj.id));    
    template.find("#grid_header_title").text(fromdb(jsonObj.description));			   
    template.find("#id").text(noNull(jsonObj.id));    
    template.find("#iprange").text(fromdb(jsonObj.description));
    template.find("#netmask").text(noNull(jsonObj.netmask));
    template.find("#gateway").text(noNull(jsonObj.gateway));
    template.find("#podname").text(fromdb(jsonObj.podname)); 
   
    var $actionLink = template.find("#network_action_link");		
	$actionLink.bind("mouseover", function(event) {
        $(this).find("#network_action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {
        $(this).find("#network_action_menu").hide();    
        return false;
    });		
	
	var $actionMenu = $actionLink.find("#network_action_menu");
    $actionMenu.find("#action_list").empty();	
    
    buildActionLinkForSubgridItem("Delete VLAN", podNetworkActionMap, $actionMenu, template);	
}

var podNetworkActionMap = {  
    "Delete VLAN": {              
        api: "deleteVlanIpRange",     
        isAsyncJob: false,   
        inProcessText: "Deleting VLAN....",
        afterActionSeccessFn: function(json, id, $subgridItem) {                 
            $subgridItem.slideUp("slow", function() {
                $(this).remove();
            });
        }
    } 
}  

function podJsonClearRightPanel(jsonObj) {	 
    podJsonClearDetailsTab(jsonObj);
}

function podJsonClearDetailsTab(jsonObj) {	    
    var $thisTab = $("#right_panel_content #tab_content_details");  
    $thisTab.find("#id").text("");
    
    $thisTab.find("#name").text("");
    $thisTab.find("#name_edit").val("");
    
    $thisTab.find("#cidr").text("");  
    $thisTab.find("#cidr_edit").val("");
          
    $thisTab.find("#ipRange").text("");
    $thisTab.find("#startIpRange_edit").val("");
    $thisTab.find("#endIpRange_edit").val("");
    
    $thisTab.find("#gateway").text(""); 
    $thisTab.find("#gateway_edit").val(""); 
    
    //if (getDirectAttachUntaggedEnabled() == "true") 
	//	$("#submenu_content_zones #action_add_directip_vlan").data("type", "pod").data("id", obj.id).data("name", obj.name).data("zoneid", obj.zoneid).show();		
}
	
function getIpRange(startip, endip) {
    var ipRange = "";
	if (startip != null && startip.length > 0) {
		ipRange = startip;
	}
	if (endip != null && endip.length > 0) {
		ipRange = ipRange + " - " + endip;
	}		
	return ipRange;
}	

function refreshClsuterFieldInAddHostDialog(dialogAddHost, podId, clusterId) {                         
    $.ajax({
        data: createURL("command=listClusters&podid="+podId),
        dataType: "json",
        success: function(json) {			            
            var items = json.listclustersresponse.cluster;
            var clusterSelect = dialogAddHost.find("#cluster_select").empty();		
            if(items != null && items.length > 0) {		                                        
                for(var i=0; i<items.length; i++) {	
                    if(clusterId != null && items[i].id == clusterId)
                        clusterSelect.append("<option value='" + noNull(items[i].id) + "' selected>" + fromdb(items[i].name) + "</option>");	
                    else               
                        clusterSelect.append("<option value='" + noNull(items[i].id) + "'>" + fromdb(items[i].name) + "</option>");		
                }                             
                dialogAddHost.find("input[value=existing_cluster_radio]").attr("checked", true);
            }
            else {
			    clusterSelect.append("<option value='-1'>None Available</option>");
                dialogAddHost.find("input[value=new_cluster_radio]").attr("checked", true);
            }
        }
    });     
}      

function initAddHostButton($midmenuAddLink1, currentPageInRightPanel) {
    $midmenuAddLink1.find("#label").text("Add Host"); 
    $midmenuAddLink1.show();
    $midmenuAddLink1.unbind("click").bind("click", function(event) {     
        dialogAddHost = $("#dialog_add_host");      
        dialogAddHost.find("#info_container").hide();    
        dialogAddHost.find("#new_cluster_name").val("");
        
        var zoneId, podId, clusterId;               
        if(currentPageInRightPanel == "pod_page") {
            var podObj = $("#tab_content_details").data("jsonObj");   
            zoneId = podObj.zoneid;
            podId = podObj.id;
            dialogAddHost.find("#zone_name").text(fromdb(podObj.zonename));  
            dialogAddHost.find("#pod_name").text(fromdb(podObj.name)); 
        }
        else if(currentPageInRightPanel == "host_page") {
            var hostObj = $("#tab_content_details").data("jsonObj");  
            zoneId = hostObj.zoneid;
            podId = hostObj.podid; 
            clusterId = hostObj.clusterid;   
            dialogAddHost.find("#zone_name").text(fromdb(hostObj.zonename));  
            dialogAddHost.find("#pod_name").text(fromdb(hostObj.podname)); 
        }
        else if(currentPageInRightPanel == "primarystorage_page") {
            var primarystorageObj = $("#tab_content_details").data("jsonObj");   
            zoneId = primarystorageObj.zoneid;
            podId = primarystorageObj.podid;    
            clusterId = primarystorageObj.clusterid;          
            dialogAddHost.find("#zone_name").text(fromdb(primarystorageObj.zonename));  
            dialogAddHost.find("#pod_name").text(fromdb(primarystorageObj.podname)); 
        }
          
        refreshClsuterFieldInAddHostDialog(dialogAddHost, podId, clusterId);
	        	    
        dialogAddHost
        .dialog('option', 'buttons', { 				
	        "Add": function() { 
	            var $thisDialog = $(this);		            
	            			   
		        var clusterRadio = $thisDialog.find("input[name=cluster]:checked").val();				
			
		        // validate values
		        var isValid = true;									
		        isValid &= validateString("Host name", $thisDialog.find("#host_hostname"), $thisDialog.find("#host_hostname_errormsg"));
		        isValid &= validateString("User name", $thisDialog.find("#host_username"), $thisDialog.find("#host_username_errormsg"));
		        isValid &= validateString("Password", $thisDialog.find("#host_password"), $thisDialog.find("#host_password_errormsg"));	
				if(clusterRadio == "new_cluster_radio") {
					isValid &= validateString("Cluster Name", $thisDialog.find("#new_cluster_name"), $thisDialog.find("#new_cluster_name_errormsg"));
				}
		        if (!isValid) 
		            return;
		            				
				$thisDialog.find("#spinning_wheel").show(); 				
				
		        var array1 = [];    
		        array1.push("&zoneId="+zoneId);
		        array1.push("&podId="+podId);
						      
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
					
					    showMiddleMenu();
					    
					    /*
					    var $midmenuItem1 = $("#midmenu_item").clone();
                        $("#midmenu_container").append($midmenuItem1.fadeIn("slow"));
                        var items = json.addhostresponse.host;				            			      										   
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
							clickClusterNodeAfterAddHost(clusterRadio, podId, newClusterName, existingClusterId, $thisDialog);  
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

function clickClusterNodeAfterAddHost(clusterRadio, podId, newClusterName, existingClusterId, $thisDialog) {
    if(clusterRadio == "new_cluster_radio") {    //*** new cluster ***                         
        refreshClusterUnderPod($("#pod_" + podId), newClusterName);  //this function will click the new cluster node                         
        $thisDialog.find("#new_cluster_name").val("");   
    }        
    else if(clusterRadio == "existing_cluster_radio") { //*** existing cluster ***     
        if (existingClusterId != null && existingClusterId != '-1') {
            $("#cluster_"+existingClusterId).find("#cluster_name").click();
        }    
    }         
}

function initAddPrimaryStorageButton($midmenuAddLink2, currentPageInRightPanel) {
    $midmenuAddLink2.find("#label").text("Add Primary Storage"); 
    $midmenuAddLink2.show();   
    $midmenuAddLink2.unbind("click").bind("click", function(event) {   
        dialogAddPool = $("#dialog_add_pool");  
        dialogAddPool.find("#info_container").hide();	
             
        var zoneId, podId, sourceClusterId;        
        if(currentPageInRightPanel == "pod_page") {
            var podObj = $("#tab_content_details").data("jsonObj");  
            zoneId = podObj.zoneid;
            podId = podObj.id;
            dialogAddPool.find("#zone_name").text(fromdb(podObj.zonename));  
            dialogAddPool.find("#pod_name").text(fromdb(podObj.name)); 
        }
        else if(currentPageInRightPanel == "host_page") {
            var hostObj = $("#tab_content_details").data("jsonObj");  
            zoneId = hostObj.zoneid;
            podId = hostObj.podid; 
            sourceClusterId = hostObj.clusterid;            
            dialogAddPool.find("#zone_name").text(fromdb(hostObj.zonename));  
            dialogAddPool.find("#pod_name").text(fromdb(hostObj.podname)); 
        }
        else if(currentPageInRightPanel == "primarystorage_page") {
            var primarystorageObj = $("#tab_content_details").data("jsonObj");   
            zoneId = primarystorageObj.zoneid;
            podId = primarystorageObj.podid;  
            sourceClusterId = primarystorageObj.clusterid;   
            dialogAddPool.find("#zone_name").text(fromdb(primarystorageObj.zonename));  
            dialogAddPool.find("#pod_name").text(fromdb(primarystorageObj.podname)); 
        }
                                             
        var clusterSelect = $("#dialog_add_pool").find("#pool_cluster").empty();			            
	    $.ajax({
		    data: createURL("command=listClusters&podid=" + podId),
	        dataType: "json",
	        success: function(json) {				                        
	            var items = json.listclustersresponse.cluster;
	            if(items != null && items.length > 0) {				                		                
	                for(var i=0; i<items.length; i++) {	
	                    if(sourceClusterId != null && items[i].id == sourceClusterId)
	                        clusterSelect.append("<option value='" + items[i].id + "' selected>" + fromdb(items[i].name) + "</option>");	
	                    else               
	                        clusterSelect.append("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>");		
	                }                
	            }			            
	        }
	    });		   
        
        $("#dialog_add_pool")
	    .dialog('option', 'buttons', { 				    
		    "Add": function() { 	
		    	var $thisDialog = $(this);
		    	
			    // validate values
				var protocol = $thisDialog.find("#add_pool_protocol").val();
				
			    var isValid = true;						    
			    isValid &= validateDropDownBox("Cluster", $thisDialog.find("#pool_cluster"), $thisDialog.find("#pool_cluster_errormsg"), false);  //required, reset error text					    				
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
				array1.push("&zoneId="+zoneId);
		        array1.push("&podId="+podId);
				
				var clusterId = $thisDialog.find("#pool_cluster").val();
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
				    array1.push("&tags="+encodeURIComponent(tags));				    
			    
			    $.ajax({
				    data: createURL("command=createStoragePool" + array1.join("")),
				    dataType: "json",
				    success: function(json) {
				        $thisDialog.find("#spinning_wheel").hide();					       
				        $thisDialog.dialog("close");					
						            
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

function initAddPodVLANButton($button) {
    initDialog("dialog_add_vlan_for_pod");

    $button.find("#label").text("Add Direct IP Range"); 
    $button.show();   
    $button.unbind("click").bind("click", function(event) {   
        if($("#tab_content_network").css("display") == "none")
            $("#tab_network").click();    
            
        var podObj = $("#tab_content_details").data("jsonObj");               
        var zoneId = podObj.zoneid;        
        var podId = podObj.id;
        var podName = podObj.name;      
                
        $("#dialog_add_vlan_for_pod").find("#pod_name_label").text(podName);
                
        $("#dialog_add_vlan_for_pod")
	    .dialog('option', 'buttons', {
	        "Add": function() {             
	            var $thisDialog = $(this);		
			   				
				// validate values
				var isValid = true;						
				isValid &= validateIp("Gateway", $thisDialog.find("#gateway"), $thisDialog.find("#gateway_errormsg"));
				isValid &= validateIp("Netmask", $thisDialog.find("#netmask"), $thisDialog.find("#netmask_errormsg"));
				isValid &= validateIp("Start IP Range", $thisDialog.find("#startip"), $thisDialog.find("#startip_errormsg"));   //required
				isValid &= validateIp("End IP Range", $thisDialog.find("#endip"), $thisDialog.find("#endip_errormsg"), true);  //optional
				if (!isValid) 
				    return;							
				
				$thisDialog.find("#spinning_wheel").show(); 
												
				var gateway = trim($thisDialog.find("#gateway").val());
				var netmask = trim($thisDialog.find("#netmask").val());
				var startip = trim($thisDialog.find("#startip").val());
				var endip = trim($thisDialog.find("#endip").val());		
				
				var array1 = [];
				array1.push("&vlan=untagged");	
				array1.push("&zoneid=" + zoneId);
				array1.push("&podId=" + podId);	
				array1.push("&forVirtualNetwork=false"); //direct VLAN	
				array1.push("&gateway="+encodeURIComponent(gateway));
				array1.push("&netmask="+encodeURIComponent(netmask));	
				array1.push("&startip="+encodeURIComponent(startip));
				if(endip != null && endip.length > 0)
				    array1.push("&endip="+encodeURIComponent(endip));	
				
				$.ajax({
				  data: createURL("command=createVlanIpRange" + array1.join("")),
					dataType: "json",
					success: function(json) {					    
					    $thisDialog.find("#spinning_wheel").hide();				        
				        $thisDialog.dialog("close");
					    
					    var item = json.createvlaniprangeresponse.vlan;
					    var $subgridItem = $("#network_tab_template").clone(true);
					    podNetworkJsonToTemplate(item, $subgridItem); 	
					    $subgridItem.find("#after_action_info").text("Direct VLAN was added successfully.");
                        $subgridItem.find("#after_action_info_container").removeClass("error").addClass("success").show();  				                        
	                    $("#tab_content_network").find("#tab_container").append($subgridItem.fadeIn("slow"));	
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

function nfsURL(server, path) {
    var url;
    if(server.indexOf("://")==-1)
	    url = "nfs://" + server + path;
	else
	    url = server + path;
	return url;
}

function iscsiURL(server, iqn, lun) {
    var url;
    if(server.indexOf("://")==-1)
	    url = "iscsi://" + server + iqn + "/" + lun;
	else
	    url = server + iqn + "/" + lun;
	return url;
}

function bindEventHandlerToDialogAddPool() {    
    $("#dialog_add_pool").find("#add_pool_protocol").change(function(event) {
		if ($(this).val() == "iscsi") {
			$("#dialog_add_pool #add_pool_path_container").hide();
			$("#dialog_add_pool #add_pool_iqn_container, #dialog_add_pool #add_pool_lun_container").show();
		} else {
			$("#dialog_add_pool #add_pool_path_container").show();
			$("#dialog_add_pool #add_pool_iqn_container, #dialog_add_pool #add_pool_lun_container").hide();
		}
	});		
}

var podActionMap = {
    "Edit Pod": {
        dialogBeforeActionFn: doEditPod  
    },
    "Delete Pod": {  
        api: "deletePod",            
        isAsyncJob: false,        
        inProcessText: "Deleting Pod....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {       
            $midmenuItem1.slideUp("slow", function() {
                $(this).remove();
            });
            clearRightPanel();
            podJsonClearRightPanel();
        }
    }
}

function doEditPod($actionLink, $detailsTab, $midmenuItem1) {       
    var $readonlyFields  = $detailsTab.find("#name, #cidr, #ipRange, #gateway");
    var $editFields = $detailsTab.find("#name_edit, #cidr_edit, #startIpRange_edit, #endIpRange_edit, #gateway_edit");
           
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
        doEditPod2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields);              
        return false;
    });   
}

function doEditPod2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields) {    
    var jsonObj = $detailsTab.data("jsonObj");
    var id = jsonObj.id;		
	var zoneid = jsonObj.zoneid;				
	var oldName = jsonObj.name;	
	var oldCidr = jsonObj.cidr;	
	var oldStartip = jsonObj.startip;					
	var oldEndip = jsonObj.endip;	
	var oldGateway = jsonObj.gateway;
	
    // validate values
	var isValid = true;			
	isValid &= validateString("Name", $detailsTab.find("#name_edit"), $detailsTab.find("#name_edit_errormsg"));
	isValid &= validateCIDR("CIDR", $detailsTab.find("#cidr_edit"), $detailsTab.find("#cidr_edit_errormsg"));	
	isValid &= validateIp("Start IP Range", $detailsTab.find("#startIpRange_edit"), $detailsTab.find("#startIpRange_edit_errormsg"));  //required
	isValid &= validateIp("End IP Range", $detailsTab.find("#endIpRange_edit"), $detailsTab.find("#endIpRange_edit_errormsg"), true);  //optional
	isValid &= validateIp("Gateway", $detailsTab.find("#gateway_edit"), $detailsTab.find("#gateway_edit_errormsg"), true);  //optional when editing	
	if (!isValid) 
	    return;			
  
    var newName = trim($detailsTab.find("#name_edit").val());
	var newCidr = trim($detailsTab.find("#cidr_edit").val());
	var newStartip = trim($detailsTab.find("#startIpRange_edit").val());
	var newEndip = trim($detailsTab.find("#endIpRange_edit").val());	
	var newIpRange = getIpRange(newStartip, newEndip);	
	var newGateway = trim($detailsTab.find("#gateway_edit").val());				
        
    var array1 = [];	
    array1.push("&id="+id);
    if(newName != oldName)
        array1.push("&name="+todb(newName));
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
	
	$.ajax({
	  data: createURL("command=updatePod"+array1.join("")),
		dataType: "json",
		success: function(json) {		   	   				    
		    var item = json.updatepodresponse.pod;	
		    $midmenuItem1.data("jsonObj", item);
		    podJsonToRightPanel($midmenuItem1);			    
		    
		    $editFields.hide();      
            $readonlyFields.show();       
            $("#save_button, #cancel_button").hide();      			
		}
	});	   
}
