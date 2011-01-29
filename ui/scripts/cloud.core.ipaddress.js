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

function ipGetSearchParams() {
    var moreCriteria = [];	

	var searchInput = $("#basic_search").find("#search_input").val();	 
    if (searchInput != null && searchInput.length > 0) {	           
        moreCriteria.push("&keyword="+todb(searchInput));	       
    }     

	var $advancedSearchPopup = getAdvancedSearchPopupInSearchContainer();
	if ($advancedSearchPopup.length > 0 && $advancedSearchPopup.css("display") != "none" ) {		

		var zone = $advancedSearchPopup.find("#adv_search_zone").val();	
	    if (zone!=null && zone.length > 0) 
			moreCriteria.push("&zoneId="+zone);	
				        
        if ($advancedSearchPopup.find("#adv_search_domain_li").css("display") != "none") {		
		    var domainId = $advancedSearchPopup.find("#adv_search_domain").val();		
		    if (domainId!=null && domainId.length > 0) 
			    moreCriteria.push("&domainid="+domainId);	
    	}	
    	
		if ($advancedSearchPopup.find("#adv_search_account_li").css("display") != "none" 
    	    && $advancedSearchPopup.find("#adv_search_account").hasClass("textwatermark") == false) {	
		    var account = $advancedSearchPopup.find("#adv_search_account").val();		
		    if (account!=null && account.length > 0) 
			    moreCriteria.push("&account="+account);		
		}	
	} 	
	
	return moreCriteria.join("");          
}

function afterLoadIpJSP() {
    //***** switch between different tabs (begin) ********************************************************************
    var tabArray = [$("#tab_details"), $("#tab_port_range"), $("#tab_port_forwarding"), $("#tab_load_balancer"), $("#tab_vpn")];
    var tabContentArray = [$("#tab_content_details"), $("#tab_content_port_range"), $("#tab_content_port_forwarding"), $("#tab_content_load_balancer"), $("#tab_content_vpn")];
    var afterSwitchFnArray = [ipJsonToDetailsTab, ipJsonToPortRangeTab, ipJsonToPortForwardingTab, ipJsonToLoadBalancerTab, ipJsonToVPNTab];
    switchBetweenDifferentTabs(tabArray, tabContentArray, afterSwitchFnArray);       
    //***** switch between different tabs (end) **********************************************************************
        
    //dialogs
    initDialog("dialog_acquire_public_ip", 325);
	initDialog("dialog_enable_vpn");
	initDialog("dialog_disable_vpn");
	initDialog("dialog_add_vpnuser");
	initDialog("dialog_confirmation_remove_vpnuser");
	initDialog("dialog_enable_static_NAT");
    
    //*** Acquire New IP (begin) ***
	$.ajax({
	    data: createURL("command=listZones&available=true"),
		dataType: "json",
		success: function(json) {
			var zones = json.listzonesresponse.zone;				
			var zoneSelect = $("#dialog_acquire_public_ip #acquire_zone").empty();	
			if (zones != null && zones.length > 0) {	
			    for (var i = 0; i < zones.length; i++) {
				    zoneSelect.append("<option value='" + zones[i].id + "'>" + fromdb(zones[i].name) + "</option>"); 
			    }
		    }
		}
	});
	    
    $("#acquire_new_ip_button").unbind("click").bind("click", function(event) {  
		var submenuContent = $("#submenu_content_network");
		$("#dialog_acquire_public_ip").dialog('option', 'buttons', {				
			"Acquire": function() { 
				var thisDialog = $(this);	
				thisDialog.dialog("close");
						
				var zoneid = thisDialog.find("#acquire_zone").val();				
				
				var $midmenuItem1 = beforeAddingMidMenuItem() ;	
				
				$.ajax({
				    data: createURL("command=associateIpAddress&zoneid="+zoneid),
					dataType: "json",
					success: function(json) {						   
					    var item = json.associateipaddressresponse.ipaddress;					 
					    ipToMidmenu(item, $midmenuItem1);
						bindClickToMidMenu($midmenuItem1, ipToRightPanel, ipGetMidmenuId);  
						afterAddingMidMenuItem($midmenuItem1, true);	
	            				
					},
					error: function(XMLHttpResponse) {
						handleError(XMLHttpResponse, function() {
							afterAddingMidMenuItem($midmenuItem1, false, parseXMLHttpResponse(XMLHttpResponse));
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
    //*** Acquire New IP (end) ***
    
    //*** Port Range tab (begin) ***
    var $createPortRangeRow = $("#tab_content_port_range").find("#create_port_range_row");     
  
    $createPortRangeRow.find("#add_link").bind("click", function(event){	        
		var isValid = true;							
		isValid &= validateInteger("Start Port", $createPortRangeRow.find("#start_port"), $createPortRangeRow.find("#start_port_errormsg"), 1, 65535);
		isValid &= validateInteger("End Port", $createPortRangeRow.find("#end_port"), $createPortRangeRow.find("#end_port_errormsg"), 1, 65535);				
		if (!isValid) 
		    return;			
	    
	    var $template = $("#port_range_template").clone();
	    $("#tab_content_port_range #grid_content").append($template.show());		
	    
	    var $spinningWheel = $template.find("#row_container").find("#spinning_wheel");	
	    //$spinningWheel.find("#description").text("Adding....");	
	    $spinningWheel.find("#description").text(g_dictionary["label.adding.processing"]);	
        $spinningWheel.show();   
	    	    
	    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");       
        var ipObj = $midmenuItem1.data("jsonObj");            	        
		var ipAddress = ipObj.ipaddress;
				
	    var startPort = $createPortRangeRow.find("#start_port").val();
	    var endPort = $createPortRangeRow.find("#end_port").val();
	    var protocol = $createPortRangeRow.find("#protocol").val();
	   	    
	    var array1 = [];
        array1.push("&ipaddress="+ipAddress);           
        array1.push("&startPort="+startPort);
        array1.push("&endPort="+endPort);
        array1.push("&protocol="+protocol);
                
        $.ajax({
            data: createURL("command=createIpForwardingRule"+array1.join("")),
            dataType: "json",           
            success: function(json) {                                    	                        
                var jobId = json.createipforwardingruleresponse.jobid;                  			                        
                var timerKey = "asyncJob_" + jobId;					                       
                $("body").everyTime(
                    10000,
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
	                                    var item = json.queryasyncjobresultresponse.jobresult.ipforwardingrule;		       	        	
	                                    portRangeJsonToTemplate(item, $template);
	                                    $spinningWheel.hide();   
	                                    refreshCreatePortRangeRow();			 
			                        } else if (result.jobstatus == 2) { // Failed			                            
				                        $template.slideUp("slow", function() {
					                        $(this).remove();
				                        });
				                        var errorMsg = "Create Port Fowarding Rule action failed. Reason: " + fromdb(result.jobresult.errortext);				
	                                    $("#dialog_error").text(errorMsg).dialog("open");
			                        }											                    
		                        }
	                        },
	                        error: function(XMLHttpResponse) {	                  
		                        $("body").stopTime(timerKey);	
								handleError(XMLHttpResponse, function() {
					                $template.slideUp("slow", function() {
						                $(this).remove();
					                });
					                var errorMsg = parseXMLHttpResponse(XMLHttpResponse);				
		                            $("#dialog_error").text(fromdb(errorMsg)).dialog("open");
				                });								
	                        }
                        });
                    },
                    0
                );
            },
            error: function(XMLHttpResponse) {	
                handleError(XMLHttpResponse, function() {
	                $template.slideUp("slow", function() {
		                $(this).remove();
	                });
	                var errorMsg = parseXMLHttpResponse(XMLHttpResponse);				
                    $("#dialog_error").text(fromdb(errorMsg)).dialog("open");
                });		
            }
        });    	    
	    
	    return false;
	});
    //*** Port Range tab (end) ***
   
    //*** Port Forwarding tab (begin) ***
    var $createPortForwardingRow = $("#tab_content_port_forwarding").find("#create_port_forwarding_row");     
    
    $createPortForwardingRow.find("#add_link").bind("click", function(event){	        
		var isValid = true;		
		isValid &= validateDropDownBox("Instance", $createPortForwardingRow.find("#vm"), $createPortForwardingRow.find("#vm_errormsg"));				
		isValid &= validateInteger("Public Port", $createPortForwardingRow.find("#public_port"), $createPortForwardingRow.find("#public_port_errormsg"), 1, 65535);
		isValid &= validateInteger("Private Port", $createPortForwardingRow.find("#private_port"), $createPortForwardingRow.find("#private_port_errormsg"), 1, 65535);				
		if (!isValid) 
		    return;			
	    
	    var $template = $("#port_forwarding_template").clone();
	    $("#tab_content_port_forwarding #grid_content").append($template.show());		
	    
	    var $spinningWheel = $template.find("#row_container").find("#spinning_wheel");	
	    $spinningWheel.find("#description").text(g_dictionary["label.adding.processing"]);	
        $spinningWheel.show();   
	    	    
	    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");       
        var ipObj = $midmenuItem1.data("jsonObj");            	        
		var ipAddress = ipObj.ipaddress;
				
	    var publicPort = $createPortForwardingRow.find("#public_port").val();
	    var privatePort = $createPortForwardingRow.find("#private_port").val();
	    var protocol = $createPortForwardingRow.find("#protocol").val();
	    var virtualMachineId = $createPortForwardingRow.find("#vm").val();		   
	    		    
	    var array1 = [];
        array1.push("&ipaddress="+ipAddress);           
        array1.push("&publicport="+publicPort);
        array1.push("&privateport="+privatePort);
        array1.push("&protocol="+protocol);
        array1.push("&virtualmachineid=" + virtualMachineId);
        
        $.ajax({
            data: createURL("command=createPortForwardingRule"+array1.join("")),
            dataType: "json",           
            success: function(json) {	                               	                        
                var jobId = json.createportforwardingruleresponse.jobid;                  			                        
                var timerKey = "asyncJob_" + jobId;					                       
                $("body").everyTime(
                    10000,
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
	                                    var item = json.queryasyncjobresultresponse.jobresult.portforwardingrule;		       	        	
	                                    portForwardingJsonToTemplate(item,$template);
	                                    $spinningWheel.hide();   
	                                    refreshCreatePortForwardingRow();			 
			                        } else if (result.jobstatus == 2) { // Failed			                            
				                        $template.slideUp("slow", function() {
					                        $(this).remove();
				                        });
				                        var errorMsg = "Create Port Fowarding Rule action failed. Reason: " + fromdb(result.jobresult.errortext);				
	                                    $("#dialog_error").text(errorMsg).dialog("open");
			                        }											                    
		                        }
	                        },
	                        error: function(XMLHttpResponse) {	                  
		                        $("body").stopTime(timerKey);	
								handleError(XMLHttpResponse, function() {
					                $template.slideUp("slow", function() {
						                $(this).remove();
					                });
					                var errorMsg = parseXMLHttpResponse(XMLHttpResponse);				
		                            $("#dialog_error").text(fromdb(errorMsg)).dialog("open");
				                });								
	                        }
                        });
                    },
                    0
                );
            },
            error: function(XMLHttpResponse) {	
                handleError(XMLHttpResponse, function() {
	                $template.slideUp("slow", function() {
		                $(this).remove();
	                });
	                var errorMsg = parseXMLHttpResponse(XMLHttpResponse);				
                    $("#dialog_error").text(fromdb(errorMsg)).dialog("open");
                });		
            }
        });    	    
	    
	    return false;
	});
    //*** Port Forwarding tab (end) ***
    
    //*** Load Balancer tab (begin) ***
    var createLoadBalancerRow = $("#tab_content_load_balancer #create_load_balancer_row");
    
    createLoadBalancerRow.find("#add_link").bind("click", function(event){		
	    // validate values		    
		var isValid = true;					
		isValid &= validateString("Name", createLoadBalancerRow.find("#name"), createLoadBalancerRow.find("#name_errormsg"));
		isValid &= validateInteger("Public Port", createLoadBalancerRow.find("#public_port"), createLoadBalancerRow.find("#public_port_errormsg"), 1, 65535);
		isValid &= validateInteger("Private Port", createLoadBalancerRow.find("#private_port"), createLoadBalancerRow.find("#private_port_errormsg"), 1, 65535);				
		if (!isValid) return;
		 
		var $template = $("#load_balancer_template").clone();	
		$("#tab_content_load_balancer #grid_content").append($template.show());		
		
		var $spinningWheel = $template.find("#row_container").find("#spinning_wheel");	
	    $spinningWheel.find("#description").text(g_dictionary["label.adding.processing"]);	
        $spinningWheel.show();            			 
		 			
		var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");        
        var ipObj = $midmenuItem1.data("jsonObj");
       
		var ipAddress = ipObj.ipaddress;
		 	 
	    var name = createLoadBalancerRow.find("#name").val();  
	    var publicPort = createLoadBalancerRow.find("#public_port").val();
	    var privatePort = createLoadBalancerRow.find("#private_port").val();
	    var algorithm = createLoadBalancerRow.find("#algorithm_select").val();  
	    		   
	    var array1 = [];
        array1.push("&publicip="+ipAddress);    
        array1.push("&name="+todb(name));              
        array1.push("&publicport="+publicPort);
        array1.push("&privateport="+privatePort);
        array1.push("&algorithm="+algorithm);
       
        $.ajax({
	        data: createURL("command=createLoadBalancerRule"+array1.join("")),
			dataType: "json",
			success: function(json) {	
				var item = json.createloadbalancerruleresponse.loadbalancer;				
	            loadBalancerJsonToTemplate(item, $template);
	            $spinningWheel.hide();   
	            refreshCreateLoadBalancerRow();	            	
			},
		    error: function(XMLHttpResponse) {				    
			    handleError(XMLHttpResponse, function() {
					$template.slideUp("slow", function() {
						$(this).remove();
					});
					var errorMsg = parseXMLHttpResponse(XMLHttpResponse);				
		            $("#dialog_error").text(fromdb(errorMsg)).dialog("open");
				});
		    }			
		});  	    
	    return false;
	});
	//*** Load Balancer tab (end) ***
}

function ipGetMidmenuId(jsonObj) {   
    return ipGetMidmenuId2(jsonObj.ipaddress);
}

function ipGetMidmenuId2(ipaddress) {  
    return "midmenuItem_" + ipaddress.replace(/\./g, "_");   //e.g. "192.168.33.108" => "192_168_33_108"
}

function ipToMidmenu(jsonObj, $midmenuItem1) {    
    var id = ipGetMidmenuId(jsonObj);
    $midmenuItem1.attr("id", id);  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show();
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_network_networkgroup.png");
    
	var firstRow = jsonObj.ipaddress.substring(0,25);
	if (jsonObj.issourcenat == true) {
		firstRow+="[source nat]";
	}
    $midmenuItem1.find("#first_row").text(firstRow); 
    $midmenuItem1.find("#second_row").text("owned by: "+fromdb(jsonObj.account).substring(0,25));    
}

function isIpManageable(domainid, account) {             
    if(((g_domainid == domainid && g_account == account) || (g_domainid == domainid && isDomainAdmin()) || (isAdmin())) 
        && account!="system") 
        return true;
    else
        return false;
}    

function ipToRightPanel($midmenuItem1) {       
    var ipObj = $midmenuItem1.data("jsonObj");
    
    var networkObj;
    if(ipObj.networkid != null) {  
        $.ajax({            
            data: createURL("command=listNetworks&id="+ipObj.networkid+"&isSystem=true"),
            dataType: "json",
            async: false,
            success: function(json) {                          
                var items = json.listnetworksresponse.network;
                if(items != null && items.length > 0) {  
                    networkObj = items[0];                
                    $midmenuItem1.data("networkObj", networkObj);    
                }
            }
        });       
    }
    
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);
    
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);
    $("#tab_details").click();        
   
    if(ipObj.isstaticnat == true) {
        $("#tab_port_range").show();	
        $("#tab_port_forwarding, #tab_load_balancer, #tab_vpn").hide();	
    }
    else { //ipObj.isstaticnat == false  
        $("#tab_port_range").hide();
        if(ipObj.forvirtualnetwork == true) { //(public network)            
            if(isIpManageable(ipObj.domainid, ipObj.account) == true) {           
	            //Port Forwarding tab
	            if(networkObj != null) {
	                var firewallServiceObj = ipFindNetworkServiceByName("Firewall", networkObj);
	                if(firewallServiceObj != null) {
			            var portForwardingCapabilityObj = ipFindCapabilityByName("PortForwarding", firewallServiceObj);
			            if(portForwardingCapabilityObj != null) {
			                if(portForwardingCapabilityObj.value == "true") 
			                    $("#tab_port_forwarding").show();  			                
			                else
			                    $("#tab_port_forwarding").hide();    
			            }
			            else {
			                $("#tab_port_forwarding").hide();    
			            }	
	                }
	                else {
	                    $("#tab_port_forwarding").hide();    
	                }
	            }
	            else {
	                $("#tab_port_forwarding").hide();    
	            }
	            
	            //Load Balancer tab	  
	            if(networkObj != null) {
	                var lbServiceObj = ipFindNetworkServiceByName("Lb", networkObj);			    	                      
	                if(lbServiceObj != null)
	                    $("#tab_load_balancer").show();
	                else
	                    $("#tab_load_balancer").hide();	     
	            }     
	            else {
	                $("#tab_load_balancer").hide();	   
	            }       	            
		        
		        //VPN tab
		        var vpnServiceObj = ipFindNetworkServiceByName("Vpn", networkObj);	      
		        if (ipObj.issourcenat == true) {
		            if(vpnServiceObj != null)
			            $("#tab_vpn").show();
			        else
			            $("#tab_vpn").hide();
		        }   
		        else {
		            $("#tab_vpn").hide();
		        }          
            } 
            else { 
	            $("#tab_port_forwarding, #tab_load_balancer, #tab_vpn").hide();	    
            }
        }
        else { //ipObj.forvirtualnetwork == false (direct network)
            $("#tab_port_forwarding, #tab_load_balancer, #tab_vpn").hide();	    
        }            
    }        
}

function ipJsonToPortRangeTab() {   
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
   
    if($midmenuItem1 == null) {
        ipClearPortRangeTab();
        return;    
    }
        
    var ipObj = $midmenuItem1.data("jsonObj");
    if(ipObj == null) {
        ipClearPortRangeTab();
        return;   
    }
        
    var networkObj = $midmenuItem1.data("networkObj");
      
    var ipAddress = ipObj.ipaddress;
    if(ipAddress == null || ipAddress.length == 0)
        return;    
   
    var $thisTab = $("#right_panel_content #tab_content_port_range");  
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   		

    if(networkObj != null) {
        var firewallServiceObj = ipFindNetworkServiceByName("Firewall", networkObj);
        if(firewallServiceObj != null) {
	        var supportedProtocolsCapabilityObj = ipFindCapabilityByName("SupportedProtocols", firewallServiceObj);    
            if(supportedProtocolsCapabilityObj != null) {
                var protocols = supportedProtocolsCapabilityObj.value.toUpperCase();  //e.g. "tcp,udp" => "TCP,UDP"         
                var array1 = protocols.split(",");
                var $protocolField = $("#create_port_range_row").find("#protocol").empty();
                for(var i=0; i<array1.length; i++)
                    $protocolField.append("<option value='"+array1[i]+"'>"+array1[i]+"</option>")
            }  
        }
    }  
    
    refreshCreatePortRangeRow();         
           		
    $.ajax({
        data: createURL("command=listIpForwardingRules&ipaddress=" + ipAddress),
        dataType: "json",        
        success: function(json) {	                                       
            var items = json.listipforwardingrulesresponse.ipforwardingrule;              
            var $PortRangeGrid = $thisTab.find("#grid_content");            
            $PortRangeGrid.empty();                       		    		      	    		
            if (items != null && items.length > 0) {				        			        
                for (var i = 0; i < items.length; i++) {
	                var $template = $("#port_range_template").clone(true);
	                portRangeJsonToTemplate(items[i], $template); 
	                $PortRangeGrid.append($template.show());						   
                }			    
            } 	
            $thisTab.find("#tab_spinning_wheel").hide();    
            $thisTab.find("#tab_container").show();           	      		    						
        }
    });      
}

function ipJsonToPortForwardingTab() {   
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null) {
        ipClearPortForwardingTab();
        return;    
    }
        
    var ipObj = $midmenuItem1.data("jsonObj");
    if(ipObj == null) {
        ipClearPortForwardingTab();
        return;   
    }
        
    var networkObj = $midmenuItem1.data("networkObj");
      
    var ipAddress = ipObj.ipaddress;
    if(ipAddress == null || ipAddress.length == 0)
        return;    
   
    var $thisTab = $("#right_panel_content #tab_content_port_forwarding");  
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   		

    if(networkObj != null) {
        var firewallServiceObj = ipFindNetworkServiceByName("Firewall", networkObj);
        if(firewallServiceObj != null) {
	        var supportedProtocolsCapabilityObj = ipFindCapabilityByName("SupportedProtocols", firewallServiceObj);    
            if(supportedProtocolsCapabilityObj != null) {
                var protocols = supportedProtocolsCapabilityObj.value.toUpperCase();  //e.g. "tcp,udp" => "TCP,UDP"         
                var array1 = protocols.split(",");
                var $protocolField = $("#create_port_forwarding_row").find("#protocol").empty();
                for(var i=0; i<array1.length; i++)
                    $protocolField.append("<option value='"+array1[i]+"'>"+array1[i]+"</option>")
            }  
        }
    }  
    
    refreshCreatePortForwardingRow();         
           		
    $.ajax({
        data: createURL("command=listPortForwardingRules&ipaddress=" + ipAddress),
        dataType: "json",        
        success: function(json) {	                                    
            var items = json.listportforwardingrulesresponse.portforwardingrule;              
            var $portForwardingGrid = $thisTab.find("#grid_content");            
            $portForwardingGrid.empty();                       		    		      	    		
            if (items != null && items.length > 0) {				        			        
                for (var i = 0; i < items.length; i++) {
	                var $template = $("#port_forwarding_template").clone(true);
	                portForwardingJsonToTemplate(items[i], $template); 
	                $portForwardingGrid.append($template.show());						   
                }			    
            } 	
            $thisTab.find("#tab_spinning_wheel").hide();    
            $thisTab.find("#tab_container").show();           	      		    						
        }
    });   
}

function ipJsonToLoadBalancerTab() {
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null) {
        ipClearLoadBalancerTab();
        return;
    }
    
    var ipObj = $midmenuItem1.data("jsonObj");
    if(ipObj == null) {
        ipClearLoadBalancerTab();
        return;
    }
   
    var networkObj = $midmenuItem1.data("networkObj");
   
    var ipAddress = ipObj.ipaddress;
    if(ipAddress == null || ipAddress.length == 0)
        return;          
    
    var $thisTab = $("#right_panel_content #tab_content_load_balancer");  
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   
		
    if(networkObj != null) {		
	    var lbServiceObj = ipFindNetworkServiceByName("Lb", networkObj);
	    if(lbServiceObj != null) {
	        var supportedLbAlgorithmsCapabilityObj = ipFindCapabilityByName("SupportedLbAlgorithms", lbServiceObj);		
	        if(supportedLbAlgorithmsCapabilityObj != null) {
               var algorithms = supportedLbAlgorithmsCapabilityObj.value;  //e.g. "roundrobin,leastconn,sourceip"        
               var array1 = algorithms.split(",");
               var $algorithmField1 = $("#create_load_balancer_row").find("#algorithm_select").empty();
               var $algorithmField2 = $("#load_balancer_template").find("#row_container_edit").find("#algorithm_select").empty();
               for(var i=0; i<array1.length; i++) {
                   $algorithmField1.append("<option value='"+array1[i]+"'>"+array1[i]+"</option>");
                   $algorithmField2.append("<option value='"+array1[i]+"'>"+array1[i]+"</option>");
               }
            }  	
        }
    }
        
    refreshCreateLoadBalancerRow();            
        
    $.ajax({
        data: createURL("command=listLoadBalancerRules&publicip="+ipAddress),
        dataType: "json",
        success: function(json) {		                    
            var items = json.listloadbalancerrulesresponse.loadbalancerrule;  
            var loadBalancerGrid = $thisTab.find("#grid_content");      
            loadBalancerGrid.empty();                         		    		      	    		
            if (items != null && items.length > 0) {				        			        
                for (var i = 0; i < items.length; i++) {
	                var $template = $("#load_balancer_template").clone(true);
	                loadBalancerJsonToTemplate(items[i], $template); 
	                loadBalancerGrid.append($template.show());						   
                }			    
            } 	 
            $thisTab.find("#tab_spinning_wheel").hide();    
            $thisTab.find("#tab_container").show();    	       	      		    						
        }
    });    
}

function showEnableVPNDialog($thisTab) {
	$("#dialog_enable_vpn")	
	.dialog('option', 'buttons', { 						
		"Enable": function() { 
			var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");	
			var ipObj = $midmenuItem1.data("jsonObj");
			var $thisDialog = $(this);
			$spinningWheel = $thisDialog.find("#spinning_wheel").show();
			$.ajax({
				data: createURL("command=createRemoteAccessVpn&publicip="+ipObj.ipaddress+"&account="+ipObj.account+"&domainid="+ipObj.domainid+"&zoneid="+ipObj.zoneid),
				dataType: "json",
				success: function(json) {
					var jobId = json.createremoteaccessvpnresponse.jobid;
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
																																	 
										if (result.jobstatus == 1) { // Succeeded
											showVpnUsers(result.jobresult.remoteaccessvpn.presharedkey, result.jobresult.remoteaccessvpn.publicip);
											$thisDialog.dialog("close");
											$spinningWheel.hide();
											$thisTab.find("#tab_container").show();
											$thisTab.find("#vpn_disabled_msg").hide();
										} else if (result.jobstatus == 2) { // Failed	
											$spinningWheel.hide(); 
											var errorMsg = "We were unable to enable VPN access.  Please contact support.";
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
			$thisTab.find("#tab_container").hide();
			$thisTab.find("#vpn_disabled_msg").show();
			$(this).dialog("close"); 
			$thisTab.find("#enable_vpn_link").unbind("click").bind("click", function(event) {
				showEnableVPNDialog($thisTab);
			});
		} 
	}).dialog("open");
}

function ipJsonToVPNTab() {
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null) {
        ipClearVPNTab();
        return;
    }
    
    var ipObj = $midmenuItem1.data("jsonObj");
    if(ipObj == null) {
        ipClearVPNTab();
        return;
    }
	
	var ipAddress = ipObj.ipaddress;
	if(ipAddress == null || ipAddress.length == 0) {
	    ipClearVPNTab();
	    return;
	}
	
	var $thisTab = $("#right_panel_content").find("#tab_content_vpn");  	
	$thisTab.find("#tab_spinning_wheel").show();    
    $thisTab.find("#tab_container").hide();   
		
	$.ajax({
        data: createURL("command=listRemoteAccessVpns&publicip="+ipAddress),
        dataType: "json",
        success: function(json) {		                    
            var items = json.listremoteaccessvpnsresponse.remoteaccessvpn;  
            if (items != null && items.length > 0) {
				showVpnUsers(items[0].presharedkey, items[0].publicip);
            } else {
				showEnableVPNDialog($thisTab);
			}
            $thisTab.find("#tab_spinning_wheel").hide();    
            $thisTab.find("#tab_container").show();    	
			$thisTab.find("#vpn_disabled_msg").hide();
        }
    });    
}

function ipClearVPNTab() {
	var $thisTab = $("#right_panel_content").find("#tab_content_vpn");  	
	showEnableVPNDialog($thisTab);	 	
	$thisTab.find("#vpn_disabled_msg").hide();
}

function showVpnUsers(presharedkey, publicip) {
	var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");	
	var ipObj = $midmenuItem1.data("jsonObj");
	var $vpnTab = $("#right_panel_content #tab_content_vpn");
	var $actionMenu = $vpnTab.find("#vpn_action_menu");
    $actionMenu.find("#action_list").empty();
	
	$vpnTab.find("#vpn_key").text(presharedkey);
	$vpnTab.find("#vpn_ip").text(publicip);
	
	var $listItemTemplate = $("#action_list_item");
	var $listItem = $listItemTemplate.clone();
	$listItem.find("#link").text("Disable VPN");
	$listItem.bind("click", function(event) {
		$actionMenu.hide();  
		$("#dialog_disable_vpn")	
		.dialog('option', 'buttons', { 						
			"Disable": function() { 
				var $thisDialog = $(this);
				$spinningWheel = $thisDialog.find("#spinning_wheel").show();
				$.ajax({
					data: createURL("command=deleteRemoteAccessVpn&publicip="+ipObj.ipaddress+"&account="+ipObj.account+"&domainid="+ipObj.domainid+"&zoneid="+ipObj.zoneid),
					dataType: "json",
					success: function(json) {
						var jobId = json.deleteremoteaccessvpnresponse.jobid;
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
												$vpnTab.find("#vpn_help").hide();
												$vpnTab.find("#enable_vpn_link").unbind("click").bind("click", function(event) {
													showEnableVPNDialog($vpnTab);
												});
												$vpnTab.find("#tab_container").hide();
												$vpnTab.find("#vpn_disabled_msg").show();
											} else if (result.jobstatus == 2) { // Failed	
												var errorMsg = "We were unable to disable VPN access.  Please contact support.";
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
				$(this).dialog("close"); 
			} 
		}).dialog("open");
		return false;
	});
	$actionMenu.find("#action_list").append($listItem.show()); 
	
	$listItem = $listItemTemplate.clone();
	$listItem.find("#link").text("Add VPN User");
	$listItem.bind("click", function(event) {
		$actionMenu.hide();
		$vpnDialog = $("#dialog_add_vpnuser");
		$vpnDialog.find("#username").val("");
		$vpnDialog.find("#password").val("");
		$("#dialog_add_vpnuser")	
		.dialog('option', 'buttons', { 						
			"Add": function() { 
				var $thisDialog = $(this);
				$thisDialog.find("#info_container").hide();
				var isValid = true;		
				isValid &= validateString("Username", $thisDialog.find("#username"), $thisDialog.find("#username_errormsg"));					    
				isValid &= validateString("Password", $thisDialog.find("#password"), $thisDialog.find("#password_errormsg"));				
				if (!isValid) return;	
				
				var username = todb($thisDialog.find("#username").val());
				var password = todb($thisDialog.find("#password").val());
				
				$spinningWheel = $thisDialog.find("#spinning_wheel").show();
				$.ajax({
					data: createURL("command=addVpnUser&username="+username+"&password="+password),
					dataType: "json",
					success: function(json) {
						var jobId = json.addvpnuserresponse.jobid;
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
												$("#tab_content_vpn #grid_content").append(vpnUserJsonToTemplate(result.jobresult.vpnuser).fadeIn());
											} else if (result.jobstatus == 2) { // Failed	
												var errorMsg = "We were unable to add user access to your VPN.  Please contact support.";
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
				$(this).dialog("close"); 
			} 
		}).dialog("open");
		return false;
	});
	$actionMenu.find("#action_list").append($listItem.show()); 
	
	// Enable action menu for vpn
	var $actionLink = $vpnTab.find("#vpn_action_link");		
	$actionLink.unbind("mouseover").bind("mouseover", function(event) {
		$(this).find("#vpn_action_menu").show();    
		return false;
	});
	$actionLink.unbind("mouseout").bind("mouseout", function(event) {
		$(this).find("#vpn_action_menu").hide();    
		return false;
	});		
	
	$vpnTab.find("#vpn_help").show();
	enableDeleteUser();
	// List users
	$.ajax({
        data: createURL("command=listVpnUsers&account="+ipObj.account+"&domainid="+ipObj.domainid),
        dataType: "json",
        async: false,
        success: function(json) {  
            var items = json.listvpnusersresponse.vpnuser;
            if(items != null && items.length > 0) {
				var $gridContent = $("#tab_content_vpn #grid_content").empty();
				for (var i = 0; i < items.length; i++) {
					$gridContent.append(vpnUserJsonToTemplate(items[i]).show());
				}
				
				//Enable delete user
				
			}
        }
    });  
}

function enableDeleteUser() {
	$("#tab_content_vpn #grid_content").unbind("click").bind("click", function(event) {
		var target = $(event.target);
		var targetId = target.attr("id");
		if (targetId == "vpn_delete_user") {
			var id = target.data("id");
			var username = target.data("username");
			var account = target.data("account");
			var domainId = target.data("domainid");
			var params = [];
			params.push("&username="+username);
			params.push("&account="+account);
			params.push("&domainid="+domainId);
			var $thisDialog = $("#dialog_confirmation_remove_vpnuser");
			$thisDialog.find("#username").text(target.data("username"));
			$thisDialog.dialog('option', 'buttons', { 						
				"Ok": function() { 
					$spinningWheel = $thisDialog.find("#spinning_wheel").show();
					$.ajax({
						data: createURL("command=removeVpnUser"+params.join("")),
						dataType: "json",
						success: function(json) {
							var jobId = json.removevpnuserresponse.jobid;
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
													
													//remove user from grid
													$("#right_panel_content #tab_content_vpn").find("#vpnuser"+id).slideUp();
												} else if (result.jobstatus == 2) { // Failed	
													var errorMsg = "We were unable to add user access to your VPN.  Please contact support.";
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
					$(this).dialog("close"); 
				} 
			}).dialog("open");
			
		}
		return false;
	});
}


var vpnItem = 1;
function vpnUserJsonToTemplate(json) {
	var $template = $("#vpn_template").clone();
	if (vpnItem++ % 2 == 0) $template.removeClass("odd").addClass("even");
	$template.find("#username").text(json.username);
	$template.attr("id", "vpnuser"+json.id);
	$template.find("#vpn_delete_user").data("id", json.id).data("username", json.username).data("account", json.account).data("domainid", json.domainid);
	return $template;
}

function ipClearRightPanel() { 
    ipClearDetailsTab();   
    ipClearPortForwardingTab();
    ipClearLoadBalancerTab(); 
}

//***** Details tab (begin) ****************************************************************************************************************
function ipJsonToDetailsTab() {  
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null) {
        ipClearDetailsTab()
        return;
    }
    
    var ipObj = $midmenuItem1.data("jsonObj");
    if(ipObj == null) {
        ipClearDetailsTab()
        return;
    }
    
    var networkObj = $midmenuItem1.data("networkObj");
    
    var ipaddress = ipObj.ipaddress;   
    if(ipaddress == null || ipaddress.length == 0)
        return;
    
    var $thisTab = $("#right_panel_content").find("#tab_content_details");  
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   
         
    $.ajax({
        data: createURL("command=listPublicIpAddresses&ipaddress="+ipaddress),
        dataType: "json",
        async: false,
        success: function(json) {  
            var items = json.listpublicipaddressesresponse.publicipaddress;
            if(items != null && items.length > 0) {
                ipObj = items[0];
                $midmenuItem1.data("jsonObj", ipObj);    
            }
        }
    });        
   
    $thisTab.find("#grid_header_title").text(fromdb(ipObj.ipaddress));       
    $thisTab.find("#ipaddress").text(fromdb(ipObj.ipaddress));
    $thisTab.find("#zonename").text(fromdb(ipObj.zonename));
    $thisTab.find("#vlanname").text(fromdb(ipObj.vlanname));    
    setBooleanReadField(ipObj.issourcenat, $thisTab.find("#source_nat")); 
    setNetworkTypeField(ipObj.forvirtualnetwork, $thisTab.find("#network_type"));  
    $thisTab.find("#networkid").text(fromdb(ipObj.networkid));  
    $thisTab.find("#associatednetworkid").text(fromdb(ipObj.associatednetworkid));
    $thisTab.find("#domain").text(fromdb(ipObj.domain));
    $thisTab.find("#account").text(fromdb(ipObj.account));
    $thisTab.find("#allocated").text(fromdb(ipObj.allocated));
    
    setBooleanReadField(ipObj.isstaticnat, $thisTab.find("#static_nat")); 
    
    if(ipObj.isstaticnat == true) {    
	    $thisTab.find("#vm_of_static_nat").text(getVmName(ipObj.virtualmachinename, ipObj.virtualmachinedisplayname));
	    $thisTab.find("#vm_of_static_nat_container").show();	        
    }
    else {
        $thisTab.find("#vm_of_static_nat").text("");
        $thisTab.find("#vm_of_static_nat_container").hide();
    }
    
    //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    var noAvailableActions = true;
    
    if(isIpManageable(ipObj.domainid, ipObj.account) == true) {    
        ipPopulateVMarray(ipObj); //popoulate VM array    
        var $vmSelect = $("#dialog_enable_static_NAT").find("#vm_dropdown").empty();		
        ipPopulateVMDropdown($vmSelect);
     
        if(ipObj.isstaticnat == true) {        
            buildActionLinkForTab("label.action.disable.static.NAT", ipActionMap, $actionMenu, $midmenuItem1, $thisTab);	
			noAvailableActions = false;
        } else {  
			if(ipObj.issourcenat != true) {   	
			    if(networkObj != null) {			    
			        var firewallServiceObj = ipFindNetworkServiceByName("Firewall", networkObj);
			        if(firewallServiceObj != null) {
			            var staticNatCapabilityObj = ipFindCapabilityByName("StaticNat", firewallServiceObj);
			            if(staticNatCapabilityObj != null) {
			                if(staticNatCapabilityObj.value == "true")
				                buildActionLinkForTab("label.action.enable.static.NAT", ipActionMap, $actionMenu, $midmenuItem1, $thisTab);
				        }
				    }
				}    				
    				    
				buildActionLinkForTab("label.action.release.ip", ipActionMap, $actionMenu, $midmenuItem1, $thisTab);
				noAvailableActions = false;
			}
        }  
    }
       
    // no available actions 
	if(noAvailableActions == true) {
	    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
	}	    	    
	
	$thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();    
}

function ipFindNetworkServiceByName(pName, networkObj) {    
    if(networkObj == null)
        return null;
    for(var i=0; i<networkObj.service.length; i++) {
        var networkServiceObj = networkObj.service[i];
        if(networkServiceObj.name == pName)
            return networkServiceObj;
    }
    return null;
}

function ipFindCapabilityByName(pName, networkServiceObj) {  
    if(networkServiceObj == null)
        return null;  
    for(var i=0; i<networkServiceObj.capability.length; i++) {
        var capabilityObj = networkServiceObj.capability[i];
        if(capabilityObj.name == pName)
            return capabilityObj;
    }
    return null;
}

function ipClearDetailsTab() {
    var $thisTab = $("#right_panel_content #tab_content_details");   
       
    $thisTab.find("#grid_header_title").text("");    
    $thisTab.find("#ipaddress").text("");
    $thisTab.find("#zonename").text("");
    $thisTab.find("#vlanname").text("");   
    $thisTab.find("#source_nat").text("");
    $thisTab.find("#network_type").text("");    
    $thisTab.find("#networkid").text("");  
    $thisTab.find("#associatednetworkid").text("");    
    $thisTab.find("#domain").text("");
    $thisTab.find("#account").text("");
    $thisTab.find("#allocated").text("");    
    $thisTab.find("#static_nat").text("");
    $thisTab.find("#vm_of_static_nat").text("");
    
    
    //actions ***  
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");  
    $actionMenu.find("#action_list").empty();
    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());		 
}

function setNetworkTypeField(value, $field) {  
    if(value == true)
        $field.text("Public");
    else if(value == false)
        $field.text("Direct");
    else
        $field.text("");
}

var ipActionMap = {  
    "label.action.release.ip": {                  
        isAsyncJob: false,        
        dialogBeforeActionFn : doReleaseIp,
        inProcessText: "label.action.release.ip.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {   
            $midmenuItem1.slideUp("slow", function(){
                $(this).remove();                    
                if(id.toString() == $("#right_panel_content").find("#tab_content_details").find("#ipaddress").text()) {
                    clearRightPanel();
                    ipClearRightPanel();
                }
            });  
            
        }
    },
    "label.action.enable.static.NAT": {                      
        isAsyncJob: false,        
        dialogBeforeActionFn: doEnableStaticNAT,
        inProcessText: "label.action.enable.static.NAT.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {  //id is ipaddress         
            if(id.toString() == $("#right_panel_content").find("#tab_content_details").find("#ipaddress").text()) {  //id is ipaddress          
                 $.ajax({
                    data: createURL("command=listPublicIpAddresses&ipaddress="+id), //id is ipaddress  
                    dataType: "json",
                    async: false,
                    success: function(json) {  
                        var items = json.listpublicipaddressesresponse.publicipaddress;
                        if(items != null && items.length > 0) {                                              
                            ipToMidmenu(items[0], $midmenuItem1); 
                        }
                    }
                });                
	        }   
        }        
    },
    "label.action.disable.static.NAT": {                      
        isAsyncJob: true,
        asyncJobResponse: "disablestaticnatresponse",
        dialogBeforeActionFn: doDisableStaticNAT,
        inProcessText: "label.action.disable.static.NAT.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {  //id is ipaddress        
            if(id.toString() == $("#right_panel_content").find("#tab_content_details").find("#ipaddress").text()) {  //id is ipaddress  
                $.ajax({
                    data: createURL("command=listPublicIpAddresses&ipaddress="+id), //id is ipaddress  
                    dataType: "json",
                    async: false,
                    success: function(json) {  
                        var items = json.listpublicipaddressesresponse.publicipaddress;
                        if(items != null && items.length > 0) {                                              
                            ipToMidmenu(items[0], $midmenuItem1); 
                        }
                    }
                });         
            }      
        }        
    }
}   

function doReleaseIp($actionLink, $detailsTab, $midmenuItem1) {      
    var jsonObj = $midmenuItem1.data("jsonObj");
    var ipaddress = jsonObj.ipaddress;
    
    $("#dialog_confirmation")
    .text(dictionary["message.action.release.ip"])	
	.dialog('option', 'buttons', { 						
		"Confirm": function() { 
		    $(this).dialog("close");			
			var apiCommand = "command=disassociateIpAddress&ipaddress="+ipaddress;
            doActionToTab(ipaddress, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		} 
	}).dialog("open");
}

function doEnableStaticNAT($actionLink, $detailsTab, $midmenuItem1) {    
    var jsonObj = $midmenuItem1.data("jsonObj");
    var ipaddress = jsonObj.ipaddress;
    
    $("#dialog_enable_static_NAT")    
	.dialog('option', 'buttons', { 						
		"Confirm": function() { 
		    var $thisDialog = $(this);
		
		    //validate
		    var vmId = $thisDialog.find("#vm_dropdown").val();
		    if(vmId == null || vmId.length == 0)
		        return;
		
		    $thisDialog.dialog("close");	
		    
			var apiCommand = "command=enableStaticNat&ipaddress="+ipaddress+"&virtualmachineid="+vmId;
            doActionToTab(ipaddress, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		} 
	}).dialog("open");
}

function doDisableStaticNAT($actionLink, $detailsTab, $midmenuItem1) {  
    var jsonObj = $midmenuItem1.data("jsonObj");
    var ipaddress = jsonObj.ipaddress;
    
    $("#dialog_info")
    .text(dictionary["message.action.disable.static.NAT"])    
	.dialog('option', 'buttons', { 						
		"Confirm": function() { 		    
		    $(this).dialog("close");	
		    
			var apiCommand = "command=disableStaticNat&ipaddress="+ipaddress;
            doActionToTab(ipaddress, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		} 
	}).dialog("open");
}
//***** Details tab (end) ******************************************************************************************************************

function ipSetRuleState(stateValue, $stateField) {
    $stateField.text(stateValue);    
    if(stateValue == "Active")  //green      
        $stateField.removeClass("status_red status_gray").addClass("status_green");
    else if(stateValue == "Deleting") //red
        $stateField.removeClass("status_green status_gray").addClass("status_red");
    else //gray
        $stateField.removeClass("status_red status_green").addClass("status_gray");
}

//***** Port Range tab (begin) ********************************************************************************************************
function ipClearPortRangeTab() {
   $("#tab_content_port_range #grid_content").empty(); 
    refreshCreatePortRangeRow(); 
}    

function portRangeJsonToTemplate(jsonObj, $template) {	     
    $template.attr("id", "PortRange_" + fromdb(jsonObj.id)).data("PortRangeId", fromdb(jsonObj.id));    		     
    $template.find("#row_container #start_port").text(fromdb(jsonObj.startport));        
    $template.find("#row_container #end_port").text(fromdb(jsonObj.endport));        
    $template.find("#row_container #protocol").text(fromdb(jsonObj.protocol));      
    ipSetRuleState(fromdb(jsonObj.state), $template.find("#row_container #state"));
                
    $template.find("#delete_link").unbind("click").bind("click", function(event){   		                    
        var $spinningWheel = $template.find("#row_container").find("#spinning_wheel");		
        $spinningWheel.find("#description").text(g_dictionary["label.deleting.processing"]);	
        $spinningWheel.show();   
             
        $.ajax({
            data: createURL("command=deleteIpForwardingRule&id="+fromdb(jsonObj.id)),
            dataType: "json",           
            success: function(json) {	                                  	                        
                var jobId = json.deleteipforwardingruleresponse.jobid;                  			                        
                var timerKey = "asyncJob_" + jobId;					                       
                $("body").everyTime(
                    10000,
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
										$template.slideUp("slow", function() {
											$(this).remove();													
										});									
									} else if (result.jobstatus == 2) { // Failed		
									    $("#dialog_error").text(g_dictionary["label.deleting.failed"]).dialog("open");											
									}							                    
		                        }
	                        },
	                        error: function(XMLHttpResponse) {	 
		                        $("body").stopTime(timerKey);
								$spinningWheel.hide();   
								handleError(XMLHttpResponse);		         
	                        }
                        });
                    },
                    0
                );
            },
            error: function(XMLHttpResponse) {				
				$spinningWheel.hide();   
				handleError(XMLHttpResponse);				
            }
        });    
                 
        return false;
    }); 
}	  

function refreshCreatePortRangeRow() {    
    var $createPortRangeRow = $("#create_port_range_row");      
    $createPortRangeRow.find("#start_port").val("");
    $createPortRangeRow.find("#end_port").val("");    
}

//***** Port Range tab (end) **********************************************************************************************************

//***** Port Forwarding tab (begin) ********************************************************************************************************
function ipClearPortForwardingTab() {
   $("#tab_content_port_forwarding #grid_content").empty(); 
    refreshCreatePortForwardingRow(); 
}    

function portForwardingJsonToTemplate(jsonObj, $template) {				        
    $template.attr("id", "portForwarding_" + fromdb(jsonObj.id)).data("portForwardingId", fromdb(jsonObj.id));	
    		     
    $template.find("#row_container #public_port").text(fromdb(jsonObj.publicport));
    $template.find("#row_container_edit #public_port").text(fromdb(jsonObj.publicport));
    
    $template.find("#row_container #private_port").text(fromdb(jsonObj.privateport));
    $template.find("#row_container_edit #private_port").val(fromdb(jsonObj.privateport));
    
    $template.find("#row_container #protocol").text(fromdb(jsonObj.protocol));
    $template.find("#row_container_edit #protocol").text(fromdb(jsonObj.protocol));
       
    var vmName = getVmName(jsonObj.virtualmachinename, jsonObj.virtualmachinedisplayname); 
    $template.find("#row_container #vm_name").text(vmName);		    
    var virtualMachineId = fromdb(jsonObj.virtualmachineid);
   
    ipSetRuleState(fromdb(jsonObj.state), $template.find("#row_container #state"));
    
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null)
        return;    
    var ipObj = $midmenuItem1.data("jsonObj");
    if(ipObj == null)
        return;    
    var ipAddress = fromdb(ipObj.ipaddress);  
   
    var $vmSelect = $template.find("#row_container_edit #vm").empty();			    
    ipPopulateVMDropdown($vmSelect);
    $vmSelect.val(virtualMachineId);    
   	   	    	   
    var $rowContainer = $template.find("#row_container");      
    var $rowContainerEdit = $template.find("#row_container_edit");    
    		    
    $template.find("#delete_link").unbind("click").bind("click", function(event){   		                    
        var $spinningWheel = $rowContainer.find("#spinning_wheel");		
        $spinningWheel.find("#description").text(g_dictionary["label.deleting.processing"]);	
        $spinningWheel.show();   
             
        $.ajax({
            data: createURL("command=deletePortForwardingRule&id="+fromdb(jsonObj.id)),
            dataType: "json",           
            success: function(json) {	                                  	                        
                var jobId = json.deleteportforwardingruleresponse.jobid;                  			                        
                var timerKey = "asyncJob_" + jobId;					                       
                $("body").everyTime(
                    10000,
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
										$template.slideUp("slow", function() {
											$(this).remove();													
										});									
									} else if (result.jobstatus == 2) { // Failed		
									    $("#dialog_error").text(g_dictionary["label.deleting.failed"]).dialog("open");											
									}							                    
		                        }
	                        },
	                        error: function(XMLHttpResponse) {	 
		                        $("body").stopTime(timerKey);
								$spinningWheel.hide();   
								handleError(XMLHttpResponse);		         
	                        }
                        });
                    },
                    0
                );
            },
            error: function(XMLHttpResponse) {				
				$spinningWheel.hide();   
				handleError(XMLHttpResponse);				
            }
        });    
                 
        return false;
    });
    
    /*
    $template.find("#edit_link").unbind("click").bind("click", function(event){   		    
        $rowContainer.hide();
        $rowContainerEdit.show();
    });
    */
    
    /*
    $template.find("#cancel_link").unbind("click").bind("click", function(event){   		    
        $rowContainer.show();
        $rowContainerEdit.hide();
    });
    */
    
    /*
    $template.find("#save_link").unbind("click").bind("click", function(event){          		       
        // validate values		    
	    var isValid = true;					    
	    isValid &= validateInteger("Private Port", $rowContainerEdit.find("#private_port"), $rowContainerEdit.find("#private_port_errormsg"), 1, 65535);				
	    if (!isValid) return;		    		        
	    
        var $spinningWheel = $rowContainerEdit.find("#spinning_wheel");	                     
        $spinningWheel.find("#description").text(g_dictionary["label.saving.processing"]);	
        $spinningWheel.show();  
	    
        var publicPort = $rowContainerEdit.find("#public_port").text();
        var privatePort = $rowContainerEdit.find("#private_port").val();
        var protocol = $rowContainerEdit.find("#protocol").text();
        var virtualMachineId = $rowContainerEdit.find("#vm").val();		   
	    		    
        var array1 = [];
        array1.push("&ipaddress="+ipAddress);    
        array1.push("&privateport="+privatePort);
        array1.push("&publicport="+publicPort);
        array1.push("&protocol="+protocol);
        array1.push("&virtualmachineid=" + virtualMachineId);
                      
        $.ajax({
             data: createURL("command=updatePortForwardingRule"+array1.join("")),
			 dataType: "json",
			 success: function(json) {					    									 
				var jobId = json.updateportforwardingruleresponse.jobid;					        
		        var timerKey = "updateportforwardingruleJob"+jobId;
		        
                $("body").everyTime(2000, timerKey, function() {
				    $.ajax({
					   data: createURL("command=queryAsyncJobResult&jobId="+jobId),
					    dataType: "json",
					    success: function(json) {										       						   
						    var result = json.queryasyncjobresultresponse;									    
						    if (result.jobstatus == 0) {
							    return; //Job has not completed
						    } else {											    
							    $("body").stopTime(timerKey);
							    if (result.jobstatus == 1) { // Succeeded							
							        var item = result.jobresult.portforwardingrule;    	
                                    portForwardingJsonToTemplate(item,$template);
                                    $spinningWheel.hide(); 	     
                                    $rowContainerEdit.hide();
                                    $rowContainer.show();                                                      
							    } else if (result.jobstatus == 2) { //Fail
							        $spinningWheel.hide(); 		
						            $("#dialog_alert").text(fromdb(result.jobresult.errortext)).dialog("open");											    					    
							    }
						    }
					    },
					    error: function(XMLHttpResponse) {	
					        handleError(XMLHttpResponse);								        
						    $("body").stopTime(timerKey);
						    $spinningWheel.hide(); 									    								    
					    }
				    });
			    }, 0);							 
			 },
			 error: function(XMLHttpResponse) {
			     handleError(XMLHttpResponse);		
			     $spinningWheel.hide(); 						 
			 }
		 });                   
    });   
    */
}	  

function refreshCreatePortForwardingRow() {       
    var $createPortForwardingRow = $("#create_port_forwarding_row");      
    $createPortForwardingRow.find("#public_port").val("");
    $createPortForwardingRow.find("#private_port").val("");
    
    var $vmSelect = $createPortForwardingRow.find("#vm").empty();		
    ipPopulateVMDropdown($vmSelect);
}	

var runningVMs, stoppedVMs;
function ipPopulateVMarray(ipObj) {    
    var commandString = "command=listVirtualMachines&domainid="+ipObj.domainid+"&account="+ipObj.account+"&networkid="+ipObj.associatednetworkid;
    
    $.ajax({
	    data: createURL(commandString+"&state=Running"),
	    dataType: "json",
	    async: false,
	    success: function(json) {			    
		    runningVMs = json.listvirtualmachinesresponse.virtualmachine;
	    }
    });	
    
    $.ajax({
	    data: createURL(commandString+"&state=Stopped"),
	    dataType: "json",
	    async: false,
	    success: function(json) {			    
		    stoppedVMs = json.listvirtualmachinesresponse.virtualmachine;		    
	    }
    });	
}   
      
function ipPopulateVMDropdown($vmSelect) {    
    if (runningVMs != null && runningVMs.length > 0) {
	    for (var i = 0; i < runningVMs.length; i++) {								
	        var html = $("<option value='" + fromdb(runningVMs[i].id) + "'>" + getVmName(runningVMs[i].name, runningVMs[i].displayname) + "</option>");							        
            $vmSelect.append(html); 								
	    }			    
    } 
    if (stoppedVMs != null && stoppedVMs.length > 0) {
	    for (var i = 0; i < stoppedVMs.length; i++) {								
	        var html = $("<option value='" + fromdb(stoppedVMs[i].id) + "'>" + getVmName(stoppedVMs[i].name, stoppedVMs[i].displayname) + "</option>");							        
            $vmSelect.append(html); 								
	    }			    
    } 	
}	    
//***** Port Forwarding tab (end) **********************************************************************************************************


//***** Load Balancer tab (begin) **********************************************************************************************************
function ipClearLoadBalancerTab() {  
    $("#tab_content_load_balancer #grid_content").empty();   
    refreshCreateLoadBalancerRow();   
}

function loadBalancerJsonToTemplate(jsonObj, $template) {	
    var loadBalancerId = fromdb(jsonObj.id);	    
    $template.attr("id", "loadBalancer_" + loadBalancerId).data("loadBalancerId", loadBalancerId);		    
    
    $template.find("#row_container #name").text(fromdb(jsonObj.name));
    $template.find("#row_container_edit #name").val(fromdb(jsonObj.name));
    
    $template.find("#row_container #public_port").text(fromdb(jsonObj.publicport));
    $template.find("#row_container_edit #public_port").text(fromdb(jsonObj.publicport));
    
    $template.find("#row_container #private_port").text(fromdb(jsonObj.privateport));
    $template.find("#row_container_edit #private_port").text(fromdb(jsonObj.privateport));
    
    $template.find("#row_container #algorithm").text(fromdb(jsonObj.algorithm));	
    $template.find("#row_container_edit #algorithm_select").val(fromdb(jsonObj.algorithm));			    	    
     
    ipSetRuleState(fromdb(jsonObj.state), $template.find("#row_container #state")); 
            
    $template.find("#manage_link").unbind("click").bind("click", function(event){	
        var $managementArea = $template.find("#management_area");
        var $vmSubgrid = $managementArea.find("#subgrid_content");
        if($managementArea.css("display") == "none") {
            $vmSubgrid.empty();         
            $.ajax({
			    cache: false,
		        data: createURL("command=listLoadBalancerRuleInstances&id="+loadBalancerId+"&applied=true"),
			    dataType: "json",
			    success: function(json) {					        
				    var instances = json.listloadbalancerruleinstancesresponse.loadbalancerruleinstance;						
				    if (instances != null && instances.length > 0) {							
					    for (var i = 0; i < instances.length; i++) {                                  
                            var $lbVmTemplate = $("#load_balancer_vm_template").clone();    											    											    
						    var obj = {"loadBalancerId": loadBalancerId, "vmId": instances[i].id, "vmName": getVmName(instances[i].name, instances[i].displayname), "vmPrivateIp": instances[i].ipaddress};	
						    lbVmObjToTemplate(obj, $lbVmTemplate);		
						    $vmSubgrid.append($lbVmTemplate.show());	                                   
					    }
				    } 
			    }
		    });        
            $managementArea.show();		           
        }
        else {
            $managementArea.hide();
        }		        
        return false;
    });
          
    var $rowContainer = $template.find("#row_container");      
    var $rowContainerEdit = $template.find("#row_container_edit");  
    		    
    $template.find("#delete_link").unbind("click").bind("click", function(event){    
        var $managementArea = $template.find("#management_area");
        if($managementArea.css("display") != "none")
            $managementArea.hide();
        
        var $spinningWheel = $template.find("#row_container").find("#spinning_wheel");	
	    $spinningWheel.find("#description").text(g_dictionary["label.deleting.processing"]);	
        $spinningWheel.show();           
                    
		$.ajax({
		    data: createURL("command=deleteLoadBalancerRule&id="+loadBalancerId),
			dataType: "json",
			success: function(json) {				
				var jobId = json.deleteloadbalancerruleresponse.jobid;
				var timerKey = "deleteLoadBalancerRuleJob_"+jobId;
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
										$template.slideUp("slow", function() {
											$(this).remove();													
										});
									} else if (result.jobstatus == 2) { // Failed										
										$("#dialog_error").text(g_dictionary["label.deleting.failed"]).dialog("open");
									}
								}
							},
							error: function(XMLHttpResponse) {	
								$("body").stopTime(timerKey);
								$spinningWheel.hide();   
								handleError(XMLHttpResponse);
							}
						});
					},
					0
				);
			}
			,
			error: function(XMLHttpResponse) {
			    $spinningWheel.hide();   
				handleError(XMLHttpResponse);
			}
		});	     
        return false;
    });		
    		    
    $template.find("#edit_link").unbind("click").bind("click", function(event){   		    
        $rowContainer.hide();
        $rowContainerEdit.show();
    });
    
    $template.find("#cancel_link").unbind("click").bind("click", function(event){   		    
        $rowContainer.show();
        $rowContainerEdit.hide();
    });
    
    $template.find("#save_link").unbind("click").bind("click", function(event){   
	    var isValid = true;		
	    isValid &= validateString("Name", $rowContainerEdit.find("#name"), $rowContainerEdit.find("#name_errormsg"));	
	    if (!isValid) 
	        return;		    		        
	    
	    var $spinningWheel = $template.find("#row_container_edit").find("#spinning_wheel");	
	    $spinningWheel.find("#description").text(g_dictionary["label.saving.processing"]);	
        $spinningWheel.show();     
	        		    	       
        var name = $rowContainerEdit.find("#name").val();  	
        var algorithm = $rowContainerEdit.find("#algorithm_select").val();  
	    		    
        var array1 = [];
        array1.push("&id=" + loadBalancerId);                
        array1.push("&name=" + name);   
        array1.push("&algorithm=" + algorithm);
                                                      
        $.ajax({
            data: createURL("command=updateLoadBalancerRule"+array1.join("")),
			dataType: "json",
			success: function(json) {					    		   	    									 
				var jobId = json.updateloadbalancerruleresponse.jobid;					        
		        var timerKey = "updateloadbalancerruleJob"+jobId;
		        
                $("body").everyTime(2000, timerKey, function() {
				    $.ajax({
					   data: createURL("command=queryAsyncJobResult&jobId="+jobId),
					    dataType: "json",
					    success: function(json) {										       						   
						    var result = json.queryasyncjobresultresponse;									    
						    if (result.jobstatus == 0) {
							    return; //Job has not completed
						    } else {											    
							    $("body").stopTime(timerKey);
							    if (result.jobstatus == 1) { // Succeeded														        								        						        								    
								    var item = result.jobresult.loadbalancer;			         	
                                    loadBalancerJsonToTemplate(item,$template); 
                                    $spinningWheel.hide();                                   
                                    $rowContainerEdit.hide();  
                                    $rowContainer.show();                                                  
							    } else if (result.jobstatus == 2) { //Fail
							        $spinningWheel.hide();                                   
                                    $rowContainerEdit.hide();  
                                    $rowContainer.show(); 
								    $("#dialog_alert").text(fromdb(result.jobresult.errortext)).dialog("open");											    					    
							    }
						    }
					    },
					    error: function(XMLHttpResponse) {	   
						    $("body").stopTime(timerKey);
						    $spinningWheel.hide();                                   
                            $rowContainerEdit.hide();  
                            $rowContainer.show(); 	
                            handleError(XMLHttpResponse);									    								    
					    }
				    });
			    }, 0);							 
			 },
			 error: function(XMLHttpResponse) {
			     handleError(XMLHttpResponse);		
			     $spinningWheel.hide();                                   
                 $rowContainerEdit.hide();  
                 $rowContainer.show(); 					 
			 }
		 });                   
    });	  		    
    
    refreshLbVmSelect($template, jsonObj.id);     
    		   
    $template.find("#add_vm_to_lb_row #assign_link").unbind("click").bind("click", function(event){		
        var vmOption =  $template.find("#add_vm_to_lb_row #vm_select option:selected");
        var vmId = vmOption.val();  		        
        var vmName = vmOption.data("vmName");
        var vmPrivateIp = vmOption.data("vmPrivateIp"); 
		if(vmId	== null || vmId.length == 0)
		    return;						    				
				
		var $spinningWheel = $template.find("#add_vm_to_lb_row #spinning_wheel");    
        $spinningWheel.show(); 			
		
		$.ajax({
		   data: createURL("command=assignToLoadBalancerRule&id="+loadBalancerId+"&virtualmachineids="+vmId),
			dataType: "json",
			success: function(json) {
				var lbInstanceJSON = json.assigntoloadbalancerruleresponse;
				var jobId = lbInstanceJSON.jobid;
				var timerKey = "assignToLoadBalancerRuleJob_"+jobId;						
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
									if (result.jobstatus == 1) { // Succeeded																		    
									    var $lbVmTemplate = $("#load_balancer_vm_template").clone();											    											    											    
									    var obj = {"loadBalancerId": loadBalancerId, "vmId": vmId, "vmName": vmName, "vmPrivateIp": vmPrivateIp};	
									    lbVmObjToTemplate(obj, $lbVmTemplate);		
									    $template.find("#management_area #subgrid_content").append($lbVmTemplate.show());	
									    refreshLbVmSelect($template, loadBalancerId);											    
		                                $spinningWheel.hide();   
									} else if (result.jobstatus == 2) { // Failed										
										$("#dialog_error").text(fromdb(result.jobresult.errortext)).dialog("open");  																
										$spinningWheel.hide();   
									}
								}
							},
							error: function(XMLHttpResponse) {										
								handleError(XMLHttpResponse);
								$("body").stopTime(timerKey);
								$spinningWheel.hide();   
							}
						});
					},
					0
				);
			},
			error: function(XMLHttpResponse) {
		        handleError(XMLHttpResponse);
		        $spinningWheel.hide();   
			}
		});	        
        return false;
    });       
}	

function refreshCreateLoadBalancerRow() {
    var createLoadBalancerRow = $("#tab_content_load_balancer #create_load_balancer_row");
    createLoadBalancerRow.find("#name").val("");  
    createLoadBalancerRow.find("#public_port").val("");
    createLoadBalancerRow.find("#private_port").val("");
    createLoadBalancerRow.find("#algorithm_select").val("roundrobin");  
}
    

function lbVmObjToTemplate(obj, $template) {
    $template.find("#vm_name").text(obj.vmName);
	$template.find("#vm_private_ip").text(fromdb(obj.vmPrivateIp));		
		
	$template.find("#remove_link").bind("click", function(event){	
	    var $spinningWheel = $template.find("#spinning_wheel");		    
        $spinningWheel.show();   	   			    		
        $.ajax({
	       data: createURL("command=removeFromLoadBalancerRule&id="+fromdb(obj.loadBalancerId)+"&virtualmachineids="+fromdb(obj.vmId)),
			dataType: "json",
			success: function(json) {
				var lbJSON = json.removefromloadbalancerruleresponse;
				var jobId = lbJSON.jobid;
				var timerKey = "removeFromLoadBalancerRuleJob_"+jobId;
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
									if (result.jobstatus == 1) { // Succeeded											    
									    refreshLbVmSelect($("#loadBalancer_" + obj.loadBalancerId), obj.loadBalancerId);
										$template.fadeOut("slow", function(event) {
											$(this).remove();
										});
									} else if (result.jobstatus == 2) { // Failed													
										$("#dialog_error").text(fromdb(result.jobresult.errortext)).dialog("open");
										$spinningWheel.hide();   										
									}
								}
							},
							error: function(XMLHttpResponse) {
								$("body").stopTime(timerKey);
								handleError(XMLHttpResponse);
								$spinningWheel.hide(); 
							}
						});
					},
					0
				);
			},
			error: function(XMLHttpResponse) {
			    handleError(XMLHttpResponse);
			    $spinningWheel.hide(); 
			}
		});		
	    return false;
	});						
}		

function refreshLbVmSelect($template, loadBalancerId) {		
    var vmSelect = $template.find("#add_vm_to_lb_row #vm_select");		    	    
    // Load the select box with the VMs that haven't been applied a LB rule to.	        
    $.ajax({
	    cache: false,
	    data: createURL("command=listLoadBalancerRuleInstances&id="+loadBalancerId+"&applied=false"),
	    dataType: "json",
	    success: function(json) {				        			        
		    var instances = json.listloadbalancerruleinstancesresponse.loadbalancerruleinstance;
		    vmSelect.empty();
		    if (instances != null && instances.length > 0) {
			    for (var i = 0; i < instances.length; i++) {
			        var vmName = getVmName(instances[i].name, instances[i].displayname);
				    html = $("<option value='" + instances[i].id + "'>" + vmName + "</option>");				  
				    html.data("vmPrivateIp", instances[i].ipaddress);
				    html.data("vmName", vmName);
				    vmSelect.append(html); 
			    }
		    } else {
			    vmSelect.append("<option value=''>None Available</option>");
		    }
	    }
    });			
}

//***** Load Balancer tab (end) ************************************************************************************************************
