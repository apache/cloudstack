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
                    
                    $.ajax({
                        data: createURL("command=listSystemVms&zoneid="+zoneObj.id+maxPageSize),
	                    dataType: "json",
	                    async: false,
	                    success: function(json) {
		                    var items = json.listsystemvmsresponse.systemvm;
		                    var $container = $zoneContent.find("#systemvms_container").empty();		                    
		                    if (items != null && items.length > 0) {					    
			                    for (var i = 0; i < items.length; i++) {
				                    var $systemvmNode = $("#leftmenu_systemvm_node_template").clone(true);
				                    systemvmJSONToTreeNode(items[i], $systemvmNode);
				                    $container.append($systemvmNode.show());
			                    }
		                    }
	                    }
                    });					
				} 
				else if(target.hasClass("expanded_open")) {					
					target.removeClass("expanded_open").addClass("expanded_close");					
					$zoneContent.hide();
					$zoneContent.find("#pods_container").empty();		
					$zoneContent.find("#systemvms_container").empty();							
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
						
			case "systemvm_name_label" :
			case "systemvm_name" :		
			    selectRowInZoneTree(target.parent().parent());	
			    var $leftmenuItem1 = target.parent().parent().parent().parent();	
			    resourceLoadPage("jsp/systemvm.jsp", $leftmenuItem1);
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
	
	var zoneArrowExpandable = false;
	$.ajax({
        data: createURL("command=listPods&zoneid="+zoneid+maxPageSize),
	    dataType: "json",
	    async: false,
	    success: function(json) {
		    var items = json.listpodsresponse.pod;	
		    if (items != null && items.length > 0) {					    
			    zoneArrowExpandable = true;  
			    forceLogout = false;  // We don't force a logout if pod(s) exit.	
			}	    		    
	    }
    });
	
	if(zoneArrowExpandable == false) {
	    $.ajax({
            data: createURL("command=listSystemVms&zoneid="+zoneid+maxPageSize),
	        dataType: "json",
	        async: false,
	        success: function(json) {
		        var items = json.listsystemvmsresponse.systemvm;		        
		        if (items != null && items.length > 0) {				    
			        zoneArrowExpandable = true;  
			    } 		        
	        }
        });
	}
	
	if(zoneArrowExpandable == true) {
	    $zoneNode.find("#zone_arrow").removeClass("white_nonexpanded_close").addClass("expanded_close");
	}
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
	
function systemvmJSONToTreeNode(json, $systemvmNode) {	
    var systemvmid = json.id;	
    $systemvmNode.attr("id", "systemvm_"+systemvmid);
    $systemvmNode.data("jsonObj", json);	    
    $systemvmNode.data("id", systemvmid).data("name", json.name);	     
    var systeymvmName = $systemvmNode.find("#systemvm_name").text(json.name);	    
    systeymvmName.data("jsonObj", json);	    		
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
	    if(pageToShow == "jsp/resource.jsp")
            afterLoadResourceJSP($midmenuItem1); 
        else if(pageToShow == "jsp/zone.jsp")
            afterLoadZoneJSP($midmenuItem1); 
        else if(pageToShow == "jsp/pod.jsp")
            afterLoadPodJSP($midmenuItem1); 
        else if(pageToShow == "jsp/cluster.jsp")
            afterLoadClusterJSP($midmenuItem1); 
        else if(pageToShow == "jsp/host.jsp")
            afterLoadHostJSP($midmenuItem1); 
        else if(pageToShow == "jsp/primarystorage.jsp")
            afterLoadPrimaryStorageJSP($midmenuItem1); 
        else if(pageToShow == "jsp/systemvm.jsp")
            afterLoadSystemVmJSP($midmenuItem1); 	    
    });    
}

function afterLoadResourceJSP($midmenuItem1) {
    hideMiddleMenu();        
    initAddZoneButton($("#midmenu_add_link")); 
	initUpdateConsoleCertButton($("#midmenu_add2_link"));
    initDialog("dialog_add_zone");
	initDialog("dialog_update_cert", 450);
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
        
    var $dialogAddZone = $("#dialog_add_zone");
    $dialogAddZone.find("#add_zone_public").unbind("change").bind("change", function(event) {        
        if($(this).val() == "true") {  //public zone
            $dialogAddZone.find("#domain_dropdown_container").hide();  
        }
        else {  //private zone
            $dialogAddZone.find("#domain_dropdown_container").show();  
        }
        return false;
    });
         
    var domainDropdown = $dialogAddZone.find("#domain_dropdown").empty();	
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
       
    $midmenuAddLink1.unbind("click").bind("click", function(event) {  
        $("#dialog_add_zone").find("#info_container").hide();				
    
        $("#dialog_add_zone")
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