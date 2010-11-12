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
	    data: createURL("command=listZones&available=true"+maxPageSize),
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
		var template = $(this);
		var target = $(event.target);
		var action = target.attr("id");
		var id = template.data("id");
		var name = template.data("name");
		
		switch (action) {
			case "zone_arrow" :		
			    selectRowInZoneTree(target.parent().parent());
			    var $zoneNode = target.parent().parent().parent().parent();			   
			    var zoneObj = $zoneNode.data("jsonObj");
			    var $zoneContent = $zoneNode.find("#zone_content");				  	   
				if(target.hasClass("expanded_close")) {						
					target.removeClass("expanded_close").addClass("expanded_open");							
					$zoneContent.show();	
									
					$.ajax({
                        data: createURL("command=listPods&zoneid="+zoneObj.id+maxPageSize),
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
											    
			case "pod_arrow" :	
			    selectRowInZoneTree(target.parent().parent());
			    var $podNode = target.parent().parent().parent().parent();
			    var podObj = $podNode.data("jsonObj");
			    var $podContent = $podNode.find("#pod_content");					 	   
				if(target.hasClass("expanded_close")) {						
					target.removeClass("expanded_close").addClass("expanded_open");		
					$podContent.show();				
					target.parent().parent().siblings("#pod_content").show();
					refreshClusterUnderPod($podNode); 
				} 
				else if(target.hasClass("expanded_open")) {					
					target.removeClass("expanded_open").addClass("expanded_close");		
					$podContent.hide();			
					target.parent().parent().siblings("#pod_content").hide();		
					$podContent.find("#clusters_container").empty();									
				}
				break;	
				
			
			case "zone_name_label":	
			case "zone_name":	
			    target.siblings("#zone_arrow").click();			    
			    selectRowInZoneTree(target.parent().parent());			    
			    var $leftmenuItem1 = target.parent().parent().parent().parent();	
			    resourceLoadPage("jsp/zone.jsp", $leftmenuItem1);			    		   				    		   			    
			    break;		
			    	
			case "pod_name_label" :	
			case "pod_name" :	
			    target.siblings("#pod_arrow").click();
			    selectRowInZoneTree(target.parent().parent());
			    var $leftmenuItem1 = target.parent().parent().parent().parent();
			    resourceLoadPage("jsp/pod.jsp", $leftmenuItem1);				   			
				break;		
				    
			case "cluster_name_label" :	
			case "cluster_name" :	
			    selectRowInZoneTree(target.parent().parent());
			    var $leftmenuItem1 = target.parent().parent().parent().parent();	
			    resourceLoadPage("jsp/cluster.jsp", $leftmenuItem1);
			    break;	
			    
			default:
				break;
		}
		return false;
	});  
}    

function refreshClusterUnderPod($podNode, newClusterName, existingClusterId) {  
    var podId = $podNode.data("podId"); 
    $.ajax({
        data: createURL("command=listClusters&podid="+podId+maxPageSize),
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
                                        
                    if(newClusterName != null && fromdb(item.name) == newClusterName) {   
                        $clusterNode.find("#cluster_name").click();                
                    }                 
                }                         
                $podNode.find("#pod_arrow").removeClass("white_nonexpanded_close").addClass("expanded_open");
                $podNode.find("#pod_content").show();                  
                
                if(existingClusterId != null) {
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

function zoneJSONToTreeNode(json, $zoneNode) {
    var zoneid = json.id;
    $zoneNode.attr("id", "zone_" + zoneid);  
    $zoneNode.data("jsonObj", json);	 
    $zoneNode.data("id", zoneid).data("name", fromdb(json.name));
    var zoneName = $zoneNode.find("#zone_name").text(fromdb(json.name));	    
    zoneName.data("jsonObj", json);	    
		
	$.ajax({
        data: createURL("command=listPods&zoneid="+zoneid+maxPageSize),
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

function resourceLoadPage(pageToShow, $midmenuItem1) {   //$midmenuItem1 is either $leftmenuItem1 or $midmenuItem1    
    clearAddButtonsOnTop();  
    $("#right_panel").load(pageToShow, function(){       
	    if(pageToShow == "jsp/resource.jsp") {
            afterLoadResourceJSP($midmenuItem1); 
        }
        else if(pageToShow == "jsp/zone.jsp") {
            afterLoadZoneJSP($midmenuItem1); 
        }
        else if(pageToShow == "jsp/pod.jsp") {
            afterLoadPodJSP($midmenuItem1); 
        }
        else if(pageToShow == "jsp/cluster.jsp") {
            afterLoadClusterJSP($midmenuItem1); 
        }
        else if(pageToShow == "jsp/host.jsp") {
            afterLoadHostJSP($midmenuItem1); 
            copyActionInfoFromMidMenuToRightPanel($midmenuItem1);                   
            $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);
            $("#tab_details").click();     
        }
        else if(pageToShow == "jsp/primarystorage.jsp") {
            afterLoadPrimaryStorageJSP($midmenuItem1);    
        }         
    });    
}

function afterLoadResourceJSP($midmenuItem1) {
    hideMiddleMenu();        
    initAddZoneButton($("#midmenu_add_link")); 
	initUpdateConsoleCertButton($("#midmenu_add2_link"));
    initDialog("dialog_add_zone");
	initDialog("dialog_update_cert", 450);	
	resourceCountTotal();
	initAddZoneShortcut();     
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

function initAddZoneShortcut() {    
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
	  data: createURL("command=listDomains"+maxPageSize),
		dataType: "json",
		async: false,
		success: function(json) {
			var domains = json.listdomainsresponse.domain;						
			if (domains != null && domains.length > 0) {
				for (var i = 0; i < domains.length; i++) {
					domainDropdown.append("<option value='" + domains[i].id + "'>" + fromdb(domains[i].name) + "</option>"); 
				}
			} 
		}
	});    
    
    $("#add_zone_shortcut").unbind("click").bind("click", function(event) {              
        if($("#leftmenu_physical_resource").find("#physical_resource_arrow").hasClass("expanded_close") == true)
			expandOrCollapseZoneTree(); //if Physical Resource arrow shows closed (i.e. zonetree is hidden), expand and show zonetree.    
       
        openAddZoneWizard();
        return false;
    });    
          
    $addZoneWizard.unbind("click").bind("click", function(event) {  
        var $thisWizard = $(this);
        var $target = $(event.target);
    
        switch($target.attr("id")) {
            case "close_button":
                closeAddZoneWizard();
                break;
            
            case "basic_mode":  //create VLAN in pod-level      
                //hide Zone VLAN Range in Add Zone(step 2), show Guest IP Range in Add Pod(step3)                 
                $thisWizard.find("#step2").find("#add_zone_vlan_container").hide();
                $thisWizard.find("#step3").find("#guestip_container, #guestnetmask_container").show();
                return true;
                break;
                
            case "advanced_mode":  //create VLAN in zone-level 
                //show Zone VLAN Range in Add Zone(step 2), hide Guest IP Range in Add Pod(step3) 
                $thisWizard.find("#step2").find("#add_zone_vlan_container").show();  
                $thisWizard.find("#step3").find("#guestip_container, #guestnetmask_container").hide();   
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
}

function addZoneWizardValidateZond($thisWizard) {    
	var isValid = true;					
	isValid &= validateString("Name", $thisWizard.find("#add_zone_name"), $thisWizard.find("#add_zone_name_errormsg"));
	isValid &= validateIp("DNS 1", $thisWizard.find("#add_zone_dns1"), $thisWizard.find("#add_zone_dns1_errormsg"), false); //required
	isValid &= validateIp("DNS 2", $thisWizard.find("#add_zone_dns2"), $thisWizard.find("#add_zone_dns2_errormsg"), true);  //optional	
	isValid &= validateIp("Internal DNS 1", $thisWizard.find("#add_zone_internaldns1"), $thisWizard.find("#add_zone_internaldns1_errormsg"), false); //required
	isValid &= validateIp("Internal DNS 2", $thisWizard.find("#add_zone_internaldns2"), $thisWizard.find("#add_zone_internaldns2_errormsg"), true);  //optional	
	if($thisWizard.find("#step2").find("#add_zone_vlan_container").css("display") != "none") {
		isValid &= validateString("VLAN Range", $thisWizard.find("#add_zone_startvlan"), $thisWizard.find("#add_zone_startvlan_errormsg"), false); //required
		isValid &= validateString("VLAN Range", $thisWizard.find("#add_zone_endvlan"), $thisWizard.find("#add_zone_endvlan_errormsg"), true);        //optional
	}	
	isValid &= validateCIDR("Guest CIDR", $thisWizard.find("#add_zone_guestcidraddress"), $thisWizard.find("#add_zone_guestcidraddress_errormsg"), false); //required
	return isValid;
}

function addZoneWizardValidatePod($thisWizard) {   
    var isValid = true;					
    isValid &= validateString("Name", $thisWizard.find("#add_pod_name"), $thisWizard.find("#add_pod_name_errormsg"));
    isValid &= validateCIDR("CIDR", $thisWizard.find("#add_pod_cidr"), $thisWizard.find("#add_pod_cidr_errormsg"));	
    isValid &= validateIp("Reserved System IP", $thisWizard.find("#add_pod_startip"), $thisWizard.find("#add_pod_startip_errormsg"));  //required
    isValid &= validateIp("Reserved System IP", $thisWizard.find("#add_pod_endip"), $thisWizard.find("#add_pod_endip_errormsg"), true);  //optional    
    return isValid;			
}

function addZoneWizardValidateGuestIPRange($thisWizard) {   
    var isValid = true;	
    isValid &= validateIp("Guest IP Range", $thisWizard.find("#startguestip"), $thisWizard.find("#startguestip_errormsg"));  //required
    isValid &= validateIp("Guest IP Range", $thisWizard.find("#endguestip"), $thisWizard.find("#endguestip_errormsg"), true);  //optional
    isValid &= validateIp("Guest Gateway", $thisWizard.find("#guestnetmask"), $thisWizard.find("#guestnetmask_errormsg"));  //required when creating
    return isValid;			
}

function addZoneWizardSubmit($thisWizard) {
	$thisWizard.find("#spinning_wheel").show();
	
	var moreCriteria = [];	
	
	var name = trim($thisWizard.find("#add_zone_name").val());
	moreCriteria.push("&name="+todb(name));
	
	var dns1 = trim($thisWizard.find("#add_zone_dns1").val());
	moreCriteria.push("&dns1="+encodeURIComponent(dns1));
	
	var dns2 = trim($thisWizard.find("#add_zone_dns2").val());
	if (dns2 != null && dns2.length > 0) 
	    moreCriteria.push("&dns2="+encodeURIComponent(dns2));						
						
	var internaldns1 = trim($thisWizard.find("#add_zone_internaldns1").val());
	moreCriteria.push("&internaldns1="+encodeURIComponent(internaldns1));
	
	var internaldns2 = trim($thisWizard.find("#add_zone_internaldns2").val());
	if (internaldns2 != null && internaldns2.length > 0) 
	    moreCriteria.push("&internaldns2="+encodeURIComponent(internaldns2));						
	 											
    if($thisWizard.find("#step2").find("#add_zone_vlan_container").css("display") != "none") {
		var vlanStart = trim($thisWizard.find("#add_zone_startvlan").val());	
		var vlanEnd = trim($thisWizard.find("#add_zone_endvlan").val());						
		if (vlanEnd != null && vlanEnd.length > 0) 
		    moreCriteria.push("&vlan=" + encodeURIComponent(vlanStart + "-" + vlanEnd));									
		else 							
			moreCriteria.push("&vlan=" + encodeURIComponent(vlanStart));		
	}	
	
	var guestcidraddress = trim($thisWizard.find("#add_zone_guestcidraddress").val());
	moreCriteria.push("&guestcidraddress="+encodeURIComponent(guestcidraddress));	
					
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
	        afterActionMsg += "Zone was created successfully<br><br>";	     	 
	       	        	        			        
	        $zoneNode = $("#leftmenu_zone_node_template").clone(true); 			            			   
            var $zoneTree = $("#leftmenu_zone_tree").find("#tree_container");		     			
            $zoneTree.prepend($zoneNode);	
            $zoneNode.fadeIn("slow");				        
	    
		    var item = json.createzoneresponse.zone;					    
		    zoneJSONToTreeNode(item, $zoneNode);		
		    
		    zoneId = item.id;			           
	    },
        error: function(XMLHttpResponse) {            
			handleError(XMLHttpResponse, function() {
				afterActionMsg += ("Failed to create zone. " + parseXMLHttpResponse(XMLHttpResponse) + "<br><br>");				
			});
        }
    });
    
    if(zoneId != null) {        
        var name = trim($thisWizard.find("#add_pod_name").val());
        var cidr = trim($thisWizard.find("#add_pod_cidr").val());
        var startip = trim($thisWizard.find("#add_pod_startip").val());
        var endip = trim($thisWizard.find("#add_pod_endip").val());	    //optional
        gateway = trim($thisWizard.find("#add_pod_gateway").val());			

        var array1 = [];
        array1.push("&zoneId="+zoneId);
        array1.push("&name="+todb(name));
        array1.push("&cidr="+encodeURIComponent(cidr));
        array1.push("&startIp="+encodeURIComponent(startip));
        if (endip != null && endip.length > 0)
            array1.push("&endIp="+encodeURIComponent(endip));
        array1.push("&gateway="+encodeURIComponent(gateway));			
						
        $.ajax({
            data: createURL("command=createPod"+array1.join("")), 
	        dataType: "json",
	        async: false,
	        success: function(json) {
	            afterActionMsg += "Pod was created successfully<br><br>";	            
	            	            
	            var item = json.createpodresponse.pod; 	
	            podId = item.id;		            		            				    
                $podNode = $("#leftmenu_pod_node_template").clone(true);
                podJSONToTreeNode(item, $podNode);                                				
                $zoneNode.find("#zone_content").show();	
                $zoneNode.find("#pods_container").prepend($podNode.show());						
                $zoneNode.find("#zone_arrow").removeClass("white_nonexpanded_close").addClass("expanded_open");	
                $podNode.fadeIn("slow");
	                                    
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
					afterActionMsg += ("Failed to create Pod. " + parseXMLHttpResponse(XMLHttpResponse) + "<br><br>");					
				});
            }
        });	
    } 
    
    if(podId != null && $thisWizard.find("#step3").find("#guestip_container").css("display") != "none") {        
		var netmask = $thisWizard.find("#step3").find("#guestnetmask").val();
		var startip = $thisWizard.find("#step3").find("#startguestip").val();
		var endip = $thisWizard.find("#step3").find("#endguestip").val();	
				
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
			async: false,
			success: function(json) { 
			    afterActionMsg += "Guest IP range was created successfully<br><br>";   
				var item = json.createvlaniprangeresponse.vlan;
				vlanId = item.id;				
			},		   
		    error: function(XMLHttpResponse) {	
				handleError(XMLHttpResponse, function() {
					afterActionMsg += ("Failed to create Guest IP range. " + parseXMLHttpResponse(XMLHttpResponse) + "<br><br>");					
				});
            }
		});		
    }
    
    $thisWizard.find("#spinning_wheel").hide();    
    $thisWizard.find("#after_action_message").html(afterActionMsg);	
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
			        data: createURL("command=uploadCustomCertificate&certificate="+encodeURIComponent(cert)),
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

function initAddZoneButton($midmenuAddLink1) {
    $midmenuAddLink1.find("#label").text("Add Zone");     
    $midmenuAddLink1.show(); 
    initAddZoneDialog($("#dialog_add_zone"), $midmenuAddLink1); 
}

function initAddZoneDialog($addZoneDialog, $addZoneLink) { 
    $addZoneDialog.find("#add_zone_public").unbind("change").bind("change", function(event) {        
        if($(this).val() == "true") {  //public zone
            $addZoneDialog.find("#domain_dropdown_container").hide();  
        }
        else {  //private zone
            $addZoneDialog.find("#domain_dropdown_container").show();  
        }
        return false;
    });
         
    var domainDropdown = $addZoneDialog.find("#domain_dropdown").empty();	
	$.ajax({
	  data: createURL("command=listDomains"+maxPageSize),
		dataType: "json",
		async: false,
		success: function(json) {
			var domains = json.listdomainsresponse.domain;						
			if (domains != null && domains.length > 0) {
				for (var i = 0; i < domains.length; i++) {
					domainDropdown.append("<option value='" + domains[i].id + "'>" + fromdb(domains[i].name) + "</option>"); 
				}
			} 
		}
	});   
       
    $addZoneLink.unbind("click").bind("click", function(event) {         
        if($("#leftmenu_physical_resource").find("#physical_resource_arrow").hasClass("expanded_close") == true)
			$("#leftmenu_physical_resource").click(); //if Physical Resource arrow shows closed (i.e. zonetree is hidden), expand and show zonetree.     
        
        $addZoneDialog.find("#info_container").hide();				
    
        $addZoneDialog
		.dialog('option', 'buttons', { 				
			"Add": function() { 
			    var $thisDialog = $(this);
								
				// validate values
				var isValid = true;					
				isValid &= validateString("Name", $thisDialog.find("#add_zone_name"), $thisDialog.find("#add_zone_name_errormsg"));
				isValid &= validateIp("DNS 1", $thisDialog.find("#add_zone_dns1"), $thisDialog.find("#add_zone_dns1_errormsg"), false); //required
				isValid &= validateIp("DNS 2", $thisDialog.find("#add_zone_dns2"), $thisDialog.find("#add_zone_dns2_errormsg"), true);  //optional	
				isValid &= validateIp("Internal DNS 1", $thisDialog.find("#add_zone_internaldns1"), $thisDialog.find("#add_zone_internaldns1_errormsg"), false); //required
				isValid &= validateIp("Internal DNS 2", $thisDialog.find("#add_zone_internaldns2"), $thisDialog.find("#add_zone_internaldns2_errormsg"), true);  //optional	
				if (getNetworkType() != "vnet") {
					isValid &= validateString("Zone - Start VLAN Range", $thisDialog.find("#add_zone_startvlan"), $thisDialog.find("#add_zone_startvlan_errormsg"), false); //required
					isValid &= validateString("Zone - End VLAN Range", $thisDialog.find("#add_zone_endvlan"), $thisDialog.find("#add_zone_endvlan_errormsg"), true);        //optional
				}
				isValid &= validateCIDR("Guest CIDR", $thisDialog.find("#add_zone_guestcidraddress"), $thisDialog.find("#add_zone_guestcidraddress_errormsg"), false); //required
				if (!isValid) 
				    return;							
				
				$thisDialog.find("#spinning_wheel").show();
				
				var moreCriteria = [];	
				
				var name = trim($thisDialog.find("#add_zone_name").val());
				moreCriteria.push("&name="+todb(name));
				
				var dns1 = trim($thisDialog.find("#add_zone_dns1").val());
				moreCriteria.push("&dns1="+encodeURIComponent(dns1));
				
				var dns2 = trim($thisDialog.find("#add_zone_dns2").val());
				if (dns2 != null && dns2.length > 0) 
				    moreCriteria.push("&dns2="+encodeURIComponent(dns2));						
									
				var internaldns1 = trim($thisDialog.find("#add_zone_internaldns1").val());
				moreCriteria.push("&internaldns1="+encodeURIComponent(internaldns1));
				
				var internaldns2 = trim($thisDialog.find("#add_zone_internaldns2").val());
				if (internaldns2 != null && internaldns2.length > 0) 
				    moreCriteria.push("&internaldns2="+encodeURIComponent(internaldns2));						
				 											
				if (getNetworkType() != "vnet") {
					var vlanStart = trim($thisDialog.find("#add_zone_startvlan").val());	
					var vlanEnd = trim($thisDialog.find("#add_zone_endvlan").val());						
					if (vlanEnd != null && vlanEnd.length > 0) 
					    moreCriteria.push("&vlan=" + encodeURIComponent(vlanStart + "-" + vlanEnd));									
					else 							
						moreCriteria.push("&vlan=" + encodeURIComponent(vlanStart));		
				}					
				
				var guestcidraddress = trim($thisDialog.find("#add_zone_guestcidraddress").val());
				moreCriteria.push("&guestcidraddress="+encodeURIComponent(guestcidraddress));	
								
				if($thisDialog.find("#domain_dropdown_container").css("display") != "none") {
				    var domainId = trim($thisDialog.find("#domain_dropdown").val());
				    moreCriteria.push("&domainid="+domainId);	
				}
														
                $.ajax({
			        data: createURL("command=createZone"+moreCriteria.join("")),
				    dataType: "json",
				    success: function(json) {				       
				        $thisDialog.find("#spinning_wheel").hide();
				        $thisDialog.dialog("close");
				        			        
				        var template = $("#leftmenu_zone_node_template").clone(true); 			            			   
			            var $zoneTree = $("#leftmenu_zone_tree").find("#tree_container");		     			
			            $zoneTree.prepend(template);	
	                    template.fadeIn("slow");				        
				    
					    var item = json.createzoneresponse.zone;					    
					    zoneJSONToTreeNode(item, template);						    	        
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
				/*
				cleanErrMsg($thisDialog.find("#add_zone_name"), $thisDialog.find("#add_zone_name_errormsg"));
				cleanErrMsg($thisDialog.find("#add_zone_dns1"), $thisDialog.find("#add_zone_dns1_errormsg"));
				cleanErrMsg($thisDialog.find("#add_zone_dns2"), $thisDialog.find("#add_zone_dns2_errormsg"));
				cleanErrMsg($thisDialog.find("#add_zone_internaldns1"), $thisDialog.find("#add_zone_internaldns1_errormsg"));
				cleanErrMsg($thisDialog.find("#add_zone_internaldns2"), $thisDialog.find("#add_zone_internaldns2_errormsg"));
				cleanErrMsg($thisDialog.find("#add_zone_startvlan"), $thisDialog.find("#add_zone_startvlan_errormsg"));
				cleanErrMsg($thisDialog.find("#add_zone_guestcidraddress"), $thisDialog.find("#add_zone_guestcidraddress_errormsg"));
				*/
			} 
		}).dialog("open");        
        return false;
    });     
}