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

function showNetworkingTab(p_domainId, p_account) {   	
    //*** Network (begin) ****************************************************************************
    activateDialog($("#dialog_acquire_public_ip").dialog({ 
		width: 325,
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));
	
	//*** Acquire New IP (begin) ***
	$.ajax({
		data: "command=listZones&available=true&response=json",
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
		
	$(".add_publicipbutton").bind("click", function(event) {			
		var submenuContent = $("#submenu_content_network");
		$("#dialog_acquire_public_ip").dialog('option', 'buttons', {				
			"Acquire": function() { 
			    $("#submenu_content_network #overlay_white").show();
			    $("#submenu_content_network #loading_gridtable").find("#message").text("Acquiring New IP....");					    
			    $("#submenu_content_network #loading_gridtable").show();				    
			    
				var thisDialog = $(this);			
				var zoneid = thisDialog.find("#acquire_zone").val();
				thisDialog.dialog("close");
				
				$.ajax({
					data: "command=associateIpAddress&zoneid="+zoneid+"&response=json",
					dataType: "json",
					success: function(json) {						   
					    var items = json.associateipaddressresponse.publicipaddress;	
					    $("#dialog_info").html("<p>The IP address <b>"+items[0].ipaddress+"</b> has been assigned to your account</p>").dialog("open");			   
						
						$("#submenu_content_network #overlay_white").hide();
			            $("#submenu_content_network #loading_gridtable").hide();	
			            
			            if(isAdmin() || isDomainAdmin()) {
			                ipListContainer.empty(); //clear search result if there is.               
			                var template = ipTemplate.clone();
							ipJsonToPanel(items[0], template);
							ipListContainer.append(template.show());								
							showPfLbArea(items[0].ipaddress, items[0].domainid, items[0].account);				                
			            } else {
			                RefreshIpDropDown(items[0].ipaddress);		
			            }		            				
					},
					error: function(XMLHttpResponse) {
					    handleError(XMLHttpResponse);
					    $("#submenu_content_network #overlay_white").hide();
			            $("#submenu_content_network #loading_gridtable").hide();	
					}						
				});
			},
			"Cancel": function() { 
				$(this).dialog("close"); 
			}
		});
		$("#dialog_acquire_public_ip").dialog("open");			
		return false;
	});
    
    var ipListContainer = $("#submenu_content_network #ip_list_container");
	var ipTemplate = $("#ip_template");
	
    function refreshIpListContainer(strCmd) {
		$("#pf_lb_area").hide();
		$("#pf_lb_area_blank").hide();
		$("#show_last_search").hide();
        ipListContainer.empty();      				
	    $.ajax({
		    data: strCmd,				
		    dataType: "json",
		    success: function(json) {					   			    
			    var items = json.listpublicipaddressesresponse.publicipaddress;						
			    if(items != null && items.length > 0) {
					if (items.length > 1) {
						for(var i=0; i < items.length; i++) {
							var template = ipTemplate.clone();
							ipJsonToPanel(items[i], template);
							ipListContainer.append(template.show());
						}	
					} else {
						var template = ipTemplate.clone();
						ipJsonToPanel(items[0], template);
						ipListContainer.append(template.show());
						template.find("#ip_manage").hide();
						
						if(isIpManageable(items[0].domainid, items[0].account) == true) { 
							showPfLbArea(items[0].ipaddress, items[0].domainid, items[0].account);									
							listLoadBalancerRules();
							refreshCreateLoadBalancerRow();    					    
							listPortForwardingRules();
							refreshCreatePortForwardingRow();
							$("#create_port_forwarding_row #add_link").data("ip", items[0].ipaddress);
							$("#create_load_balancer_row #add_link").data("ip", items[0].ipaddress);
						} else {
							$("#pf_lb_area_blank p").text("This IP address is managed by the CloudStack for use with System VMs.");
							$("#pf_lb_area_blank").show();
						}
					}
			    } else {
					$("#pf_lb_area_blank p").text("Unable to find any IP Addresses.  Please try again.");
					$("#pf_lb_area_blank").show();
				}
		    },
			error : function(XMLHttpResponse) {
				$("#pf_lb_area_blank p").text("Unable to find any IP Addresses.  Please try again.");
				$("#pf_lb_area_blank").show();
	        }
	    });					    
    }
    
    function refreshIpListContainerByInputBox() {	  
        var ip = $("#submenu_content_network #admin_ip_search").val();	     
		$("#submenu_content_network #admin_ip_search").autocomplete("close");		
        if(ip != null && ip.length >0) {    
            refreshIpListContainer("command=listPublicIpAddresses&response=json&forvirtualnetwork=true&ipaddress=" + ip);	   
		}    
    }
    
	$("#submenu_content_network #admin_ip_search").autocomplete({
		source: function(request, response) {
			$.ajax({
				data: "command=listPublicIpAddresses&response=json&forvirtualnetwork=true&ipaddress=" + request.term,				
				dataType: "json",
				success: function(json) {		
					var items = json.listpublicipaddressesresponse.publicipaddress;		
					var ipArray = [];				
					if(items != null && items.length > 0) {									
						for(var i=0; i < items.length; i++) 					        
							ipArray.push(items[i].ipaddress);		   					   			    
					}
					response(ipArray);
				}
			});		
		},
		minLength: 2

	});
	
	$("#submenu_content_network #admin_ip_search").bind("keypress", function(event) {
		if(event.keyCode == keycode_Enter) {   	
			$("#submenu_content_network #ip_searchbutton1").click();
			return false;
		}
	});
    
    $("#submenu_content_network #search_by_account").bind("keypress", function(event) {
		if(event.keyCode == keycode_Enter) {   	
			$("#submenu_content_network #ip_searchbutton2").click();
			return false;
		}
	});
    
    //watermark (begin)
	$("#submenu_content_network #admin_ip_search").bind('focus', function(event){
	    if($(this).val() == "By Public IP Address") {
	        $(this).val("");
	        $(this).removeClass("ipwatermark_text");
	    }
	});		
    $("#submenu_content_network #admin_ip_search").bind('blur', function(event){
        if($(this).val() == "") {
            $(this).val("By Public IP Address");
            $(this).addClass("ipwatermark_text");
        }                
    });	
    
    $("#submenu_content_network #search_by_account").bind('focus', function(event){
	    if($(this).val() == "By Account") {
	        $(this).val("");
	        $(this).removeClass("ipwatermark_text");
	    }
	});		
    $("#submenu_content_network #search_by_account").bind('blur', function(event){
        if($(this).val() == "") {
            $(this).val("By Account");
            $(this).addClass("ipwatermark_text");
        }                
    });		 	
    //watermark (end)	
    
    $("#submenu_content_network #ip_searchbutton1").bind("click", refreshIpListContainerByInputBox);
    		  
    function populateDomainDropdown() {
        var domainSelect = $("#submenu_content_network #search_by_domain").empty();			
	    $.ajax({
		    data: "command=listDomains&available=true&response=json",
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
    
    $("#submenu_content_network #ip_searchbutton2").bind("click", function(event){
        var array1 = [];
        var account = $("#submenu_content_network #search_by_account").val();
        if(account != null && account.length > 0)
            array1.push("&account=" + account);
        var domainId = $("#submenu_content_network #search_by_domain").val();
        array1.push("&domainid=" + domainId);	        
        refreshIpListContainer("command=listPublicIpAddresses&response=json&forvirtualnetwork=true" + array1.join(""));	 	        
        return false;
    });
    		  
	function RefreshIpDropDown(ipAddress) {			
	    var array1 = [];	
        // "p_domainId!=null" and "p_account!=null" means redirected from "IPs" link in Accounts tab to here(new Network tab)  
        if (p_domainId!=null && p_domainId.length > 0) 
		    array1.push("&domainid="+p_domainId);		
        if (p_account!=null && p_account.length > 0) 
		    array1.push("&account="+p_account);	        
	    
	    $.ajax({
			data: "command=listPublicIpAddresses&response=json&forvirtualnetwork=true" + array1.join(""),				
			dataType: "json",
			success: function(json) {					   			    
				var items = json.listpublicipaddressesresponse.publicipaddress;						
				if(items != null && items.length > 0) {						    			    			    
				    var ipSelect = $("#submenu_content_network #ip_select").empty();
				    for(var i=0; i < items.length; i++) {
				        ipSelect.append("<option value='" + items[i].ipaddress + "'>" + items[i].ipaddress + "</option>");					        
				    }							  
				    if(ipAddress != null)
				       ipSelect.val(ipAddress);						    		    
				    ipSelect.change();						   		    
				}
			}
		});		    
	}	
		
	function ipJsonToPanel(json, panel) {
		panel.find("#ip_release").show();
		panel.attr("id", "ip"+json.ipaddress).data("ip", json.ipaddress).data("domainid", json.domainid).data("account", json.account);
		panel.find("#ip_manage").data("ip", json.ipaddress).data("domainid", json.domainid).data("account", json.account);
	    panel.data("ip_domainid", json.domainid).data("ip_account", json.account);
		panel.find("#ipaddress").text(json.ipaddress);
	    panel.find("#zonename").text(json.zonename);
		panel.find("#allocated").text(json.allocated);
		panel.find("#vlanname").text(json.vlanname);
		panel.find("#source_nat").text((json.issourcenat=="true")?"Yes":"No");	
		panel.find("#network_type").text((json.forvirtualnetwork=="true")? "Public":"Direct");
		panel.find("#domain").text(json.domain);
		panel.find("#account").text(json.account);					
		
		var ipAddress = json.ipaddress;				
		if (json.issourcenat != "true" && json.forvirtualnetwork =="true") {
			panel.find("#ip_release").data("ip", json.ipaddress).show();
		} else {
		    panel.find("#ip_release").hide();
		}
		
		if(isIpManageable(json.domainid, json.account) == true && !isUser()) 
		    panel.find("#ip_manage").show();				    
	}	
	
	var ipPanel = $("#submenu_content_network #network_container"); 				
	$("#submenu_content_network #ip_select").bind("change", function(event) {		    
	    var ipAddress = $(this).val();		
	    if(ipAddress != null && ipAddress.length > 0) {	            
	        $.ajax({
			    data: "command=listPublicIpAddresses&ipaddress="+ipAddress+"&response=json",				
			    dataType: "json",
			    success: function(json) {				        				   			    
				    var items = json.listpublicipaddressesresponse.publicipaddress;		
				    if(items != null && items.length > 0) {						        
				        var item =	items[0];		    
				        ipJsonToPanel(item, ipPanel);						    
				        showPfLbArea(ipAddress, item.domainid, item.account);	        
		            }			    			    
			    }
		    });		
		}		    		    
	    return false;
    });	
    
    function isIpManageable(domainid, account) {             
        if((g_domainid == domainid && g_account == account) || (isAdmin() && account!="system")) 
            return true;
        else
            return false;
    }
    
    function showPfLbArea(ipAddress, domainid, account) {
        //show portForwarding/loadBalancer if Ip is manageable
        if(isIpManageable(domainid, account) == true) {	  
			ipPanel.data("ip_domainid", domainid).data("ip_account", account).data("ip_address", ipAddress);
            listLoadBalancerRules();
            refreshCreateLoadBalancerRow();    					    
            listPortForwardingRules();
            refreshCreatePortForwardingRow();	
			$("#create_port_forwarding_row #add_link").data("ip", ipAddress);
			$("#create_load_balancer_row #add_link").data("ip", ipAddress);
            $("#pf_lb_area").show();	
            $("#pf_lb_area_blank").hide();
        } 
        //hide portForwarding/loadBalancer if IP is not manageable          
        else {
            $("#pf_lb_area").hide();
            $("#pf_lb_area_blank p").text("This IP address is managed by the CloudStack for use with System VMs.");
			$("#pf_lb_area_blank").show();
        }	         
    }
    
    function hidePfLbArea() {
        $("#pf_lb_area").hide();
        $("#pf_lb_area_blank").hide();
    }
    
    //*** Acquire New IP (end) ***
    
    //*** Port Forwarding (begin) ***    
    var createPortForwardingRow = $("#submenu_content_network #port_forwarding_panel #create_port_forwarding_row");
    var portForwardingGrid = $("#submenu_content_network #port_forwarding_panel #grid_content");        
           
	function listPortForwardingRules() {		    
	    var ipSelected = $("#submenu_content_network #ip_select").val();
		if (!isUser()) {
			ipSelected = ipPanel.data("ip_address");
		}
        if(ipSelected == null || ipSelected.length == 0)
            return;    		
        $.ajax({
            data: "command=listPortForwardingRules&ipaddress=" + ipSelected + "&response=json",
            dataType: "json",
            success: function(json) {	                                    
                var items = json.listportforwardingrulesresponse.portforwardingrule;   
                portForwardingGrid.empty();                          		    		      	    		
                if (items != null && items.length > 0) {				        			        
	                for (var i = 0; i < items.length; i++) {
		                var template = $("#port_forwarding_template").clone(true);
		                portForwardingJsonToTemplate(items[i], template); 
		                portForwardingGrid.append(template.show());						   
	                }			    
                } 	        	      		    						
            }
        });
    }        
		
	function refreshCreatePortForwardingRow() {            
	    createPortForwardingRow.find("#public_port").val("");
	    createPortForwardingRow.find("#private_port").val("");
	    createPortForwardingRow.find("#protocol").val("TCP");  		    

	    $.ajax({
		    data: "command=listVirtualMachines&response=json&domainid="+ipPanel.data("ip_domainid")+"&account="+ipPanel.data("ip_account"),
		    dataType: "json",
		    success: function(json) {			    
			    var instances = json.listvirtualmachinesresponse.virtualmachine;
			    var vmSelect = createPortForwardingRow.find("#vm").empty();							
			    if (instances != null && instances.length > 0) {
				    for (var i = 0; i < instances.length; i++) {								
				        var html = $("<option value='" + instances[i].id + "'>" +  getVmName(instances[i].name, instances[i].displayname) + "</option>");							        
			            vmSelect.append(html); 								
				    }
			    } 
		    }
	    });		    
    }	
		
	var portForwardingIndex = 0;	
	function portForwardingJsonToTemplate(json, template) {				        
	    (portForwardingIndex++ % 2 == 0)? template.find("#row_container").addClass("smallrow_even"): template.find("#row_container").addClass("smallrow_odd");		
	    
	    template.attr("id", "portForwarding_" + json.id).data("portForwardingId", json.id);	
	    		     
	    template.find("#row_container #public_port").text(json.publicport);
	    template.find("#row_container_edit #public_port").text(json.publicport);
	    
	    template.find("#row_container #private_port").text(json.privateport);
	    template.find("#row_container_edit #private_port").val(json.privateport);
	    
	    template.find("#row_container #protocol").text(json.protocol);
	    template.find("#row_container_edit #protocol").text(json.protocol);
	    
	    var vmName = getVmName(json.vmname, json.vmdisplayname); //json doesn't include vmdisplayname property(incorrect). Waiting for Bug 6241 to be fixed....
	    template.find("#row_container #vm_name").text(vmName);		    
	    var virtualMachineId = json.virtualmachineid;
	    		    
	    $.ajax({
		    data: "command=listVirtualMachines&response=json&domainid="+ipPanel.data("ip_domainid")+"&account="+ipPanel.data("ip_account"),
		    dataType: "json",
		    success: function(json) {			    
			    var instances = json.listvirtualmachinesresponse.virtualmachine;
			    var vmSelect = template.find("#row_container_edit #vm").empty();							
			    if (instances != null && instances.length > 0) {
				    for (var i = 0; i < instances.length; i++) {								
				        var html = $("<option value='" + instances[i].id + "'>" +  getVmName(instances[i].name, instances[i].displayname) + "</option>");							        
			            vmSelect.append(html); 								
				    }
				    vmSelect.val(virtualMachineId);
			    } 
		    }
	    });		    
	   		        	
	    var loadingImg = template.find(".adding_loading");		
        var rowContainer = template.find("#row_container");      
        var rowContainerEdit = template.find("#row_container_edit");    
        		    
	    template.find("#delete_link").unbind("click").bind("click", function(event){   		                    
            loadingImg.find(".adding_text").text("Deleting....");	
            loadingImg.show();  
            rowContainer.hide();                
	              
	        $.ajax({						
	            data: "command=deletePortForwardingRule&response=json&id="+json.id,
	            dataType: "json",
	            success: function(json) {             
	                template.slideUp("slow", function(){		                    
	                    $(this).remove();
	                });	   						
	            },
	            error: function(XMLHttpResponse) {
	                handleError(XMLHttpResponse);
	                loadingImg.hide(); 	   
	                rowContainer.show();	
	            }
            });	     
	        return false;
	    });
	    
	    template.find("#edit_link").unbind("click").bind("click", function(event){   		    
	        rowContainer.hide();
	        rowContainerEdit.show();
	    });
	    
	    template.find("#cancel_link").unbind("click").bind("click", function(event){   		    
	        rowContainer.show();
	        rowContainerEdit.hide();
	    });
	    
	    template.find("#save_link").unbind("click").bind("click", function(event){          		       
	        // validate values		    
		    var isValid = true;					    
		    isValid &= validateNumber("Private Port", rowContainerEdit.find("#private_port"), rowContainerEdit.find("#private_port_errormsg"), 1, 65535);				
		    if (!isValid) return;		    		        
		    
	        var loadingImg = template.find(".adding_loading");	                        
            loadingImg.find(".adding_text").text("Saving....");	
            loadingImg.show();  
            rowContainerEdit.hide();      
		    
	        var ipAddress = $("#submenu_content_network #ip_select").val();
			if (!isUser()) {
				ipAddress = ipPanel.data("ip_address");
			}
	        var publicPort = rowContainerEdit.find("#public_port").text();
	        var privatePort = rowContainerEdit.find("#private_port").val();
	        var protocol = rowContainerEdit.find("#protocol").text();
	        var virtualMachineId = rowContainerEdit.find("#vm").val();		   
		    		    
	        var array1 = [];
            array1.push("&publicip="+ipAddress);    
            array1.push("&privateport="+privatePort);
            array1.push("&publicport="+publicPort);
            array1.push("&protocol="+protocol);
            array1.push("&virtualmachineid=" + virtualMachineId);
                          
            $.ajax({
				 data: "command=updatePortForwardingRule&response=json"+array1.join(""),
				 dataType: "json",
				 success: function(json) {					    									 
					var jobId = json.updateportforwardingruleresponse.jobid;					        
			        var timerKey = "updateportforwardingruleJob"+jobId;
			        
                    $("body").everyTime(2000, timerKey, function() {
					    $.ajax({
						    data: "command=queryAsyncJobResult&jobId="+jobId+"&response=json",
						    dataType: "json",
						    success: function(json) {										       						   
							    var result = json.queryasyncjobresultresponse;									    
							    if (result.jobstatus == 0) {
								    return; //Job has not completed
							    } else {											    
								    $("body").stopTime(timerKey);
								    if (result.jobstatus == 1) { // Succeeded										        								    
									    var items = result.portforwardingrule;	            	
                                        portForwardingJsonToTemplate(items[0],template);
                                        loadingImg.hide(); 	   
                                        rowContainer.show();						                                                               
								    } else if (result.jobstatus == 2) { //Fail
								        loadingImg.hide(); 		
							            rowContainer.show(); 
									    $("#dialog_alert").html("<p>" + fromdb(result.jobresult) + "</p>").dialog("open");											    					    
								    }
							    }
						    },
						    error: function(XMLHttpResponse) {	
						        handleError(XMLHttpResponse);								        
							    $("body").stopTime(timerKey);
							    loadingImg.hide(); 		
							    rowContainer.show(); 									    								    
						    }
					    });
				    }, 0);							 
				 },
				 error: function(XMLHttpResponse) {
				     handleError(XMLHttpResponse);		
				     loadingImg.hide(); 		
					 rowContainer.show(); 							 
				 }
			 });                   
	    });
	}	
			
	createPortForwardingRow.find("#add_link").bind("click", function(event){		    
	    // validate values		    
		var isValid = true;				
		isValid &= validateNumber("Public Port", createPortForwardingRow.find("#public_port"), createPortForwardingRow.find("#public_port_errormsg"), 1, 65535);
		isValid &= validateNumber("Private Port", createPortForwardingRow.find("#private_port"), createPortForwardingRow.find("#private_port_errormsg"), 1, 65535);				
		if (!isValid) return;			
	    
	    var template = $("#port_forwarding_template").clone();
	    portForwardingGrid.append(template.show());		
	    
	    var loadingImg = template.find(".adding_loading");		
        var rowContainer = template.find("#row_container");                  
        loadingImg.find(".adding_text").text("Adding....");	
        loadingImg.show();  
        rowContainer.hide();      
	    
		var ipAddress = $(this).data("ip");
	    var publicPort = createPortForwardingRow.find("#public_port").val();
	    var privatePort = createPortForwardingRow.find("#private_port").val();
	    var protocol = createPortForwardingRow.find("#protocol").val();
	    var virtualMachineId = createPortForwardingRow.find("#vm").val();		   
	    		    
	    var array1 = [];
        array1.push("&ipaddress="+ipAddress);    
        array1.push("&privateport="+privatePort);
        array1.push("&publicport="+publicPort);
        array1.push("&protocol="+protocol);
        array1.push("&virtualmachineid=" + virtualMachineId);
        $.ajax({						
	        data: "command=createPortForwardingRule&response=json"+array1.join(""),
	        dataType: "json",
	        success: function(json) {			            
	            var items = json.createportforwardingruleresponse.portforwardingrule;	            	
	            portForwardingJsonToTemplate(items[0],template);
	            loadingImg.hide(); 	   
	            rowContainer.show();	
	            refreshCreatePortForwardingRow();			   						
	        },
		    error: function(XMLHttpResponse) {				    
			    handleError(XMLHttpResponse);
			    template.slideUp("slow", function() {
					$(this).remove();
				});
		    }	
        });	    
	    
	    return false;
	});
	//*** Port Forwarding (end) *** 
            
    //*** Load Balancer (begin) ***            
    var createLoadBalancerRow = $("#submenu_content_network #load_balancer_panel #create_load_balancer_row");
    var loadBalancerGrid = $("#submenu_content_network #load_balancer_panel #grid_content");        
    
    function listLoadBalancerRules() {            
        var ipSelected = $("#submenu_content_network #ip_select").val();
		if (!isUser()) {
			ipSelected = ipPanel.data("ip_address");
		}
        if(ipSelected == null || ipSelected.length == 0) 
            return;              
        $.ajax({
            data: "command=listLoadBalancerRules&publicip=" + ipSelected + "&response=json",
            dataType: "json",
            success: function(json) {		                    
                var items = json.listloadbalancerrulesresponse.loadbalancerrule;   
                loadBalancerGrid.empty();                          		    		      	    		
                if (items != null && items.length > 0) {				        			        
	                for (var i = 0; i < items.length; i++) {
		                var template = $("#load_balancer_template").clone(true);
		                loadBalancerJsonToTemplate(items[i], template); 
		                loadBalancerGrid.append(template.show());						   
	                }			    
                } 	        	      		    						
            }
        });
    }        
    
    function refreshCreateLoadBalancerRow() {
        createLoadBalancerRow.find("#name").val("");  
	    createLoadBalancerRow.find("#public_port").val("");
	    createLoadBalancerRow.find("#private_port").val("");
	    createLoadBalancerRow.find("#algorithm_select").val("roundrobin");  
    }
    
    var loadBalancerIndex = 0;
	function loadBalancerJsonToTemplate(json, template) {	
	    (loadBalancerIndex++ % 2 == 0)? template.find("#row_container").addClass("smallrow_even"): template.find("#row_container").addClass("smallrow_odd");		

	    var loadBalancerId = json.id;	    
	    template.attr("id", "loadBalancer_" + loadBalancerId).data("loadBalancerId", loadBalancerId);		    
	    
	    template.find("#row_container #name").text(json.name);
	    template.find("#row_container_edit #name").val(json.name);
	    
	    template.find("#row_container #public_port").text(json.publicport);
	    template.find("#row_container_edit #public_port").text(json.publicport);
	    
	    template.find("#row_container #private_port").text(json.privateport);
	    template.find("#row_container_edit #private_port").val(json.privateport);
	    
	    template.find("#row_container #algorithm").text(json.algorithm);	
	    template.find("#row_container_edit #algorithm").val(json.algorithm);			    	    
	    
	    template.find("#manage_link").unbind("click").bind("click", function(event){		        
	        var vmSubgrid = template.find("#vm_subgrid");
	        if(vmSubgrid.css("display") == "none") {
	            vmSubgrid.empty();         
	            $.ajax({
				    cache: false,
				    data: "command=listLoadBalancerRuleInstances&id="+loadBalancerId+"&response=json",
				    dataType: "json",
				    success: function(json) {					        
					    var instances = json.listloadbalancerruleinstancesresponse.loadbalancerruleinstance;						
					    if (instances != null && instances.length > 0) {							
						    for (var i = 0; i < instances.length; i++) {                                  
                                var lbVmTemplate = $("#load_balancer_vm_template").clone();	                                    									    											    											    
							    var obj = {"loadBalancerId": loadBalancerId, "vmId": instances[i].id, "vmName": getVmName(instances[i].name, instances[i].displayname), "vmPrivateIp": instances[i].privateip};	
							    lbVmObjToTemplate(obj, lbVmTemplate);		
							    template.find("#vm_subgrid").append(lbVmTemplate.show());	                                   
						    }
					    } 
				    }
			    });        
	            vmSubgrid.show();		           
	        }
	        else {
	            vmSubgrid.hide();
	        }	
	            
	        var addVmToLbRow = template.find("#add_vm_to_lb_row");
	        (addVmToLbRow.css("display") == "none")?addVmToLbRow.show():addVmToLbRow.hide();	
	            	        
	        return false;
	    });
	    
	    var loadingContainer = template.find("#loading_container");		
        var rowContainer = template.find("#row_container");      
        var rowContainerEdit = template.find("#row_container_edit");  
	    		    
	    template.find("#delete_link").unbind("click").bind("click", function(event){                            
            loadingContainer.find(".adding_text").text("Deleting....");	
            loadingContainer.show();  
            rowContainer.hide();                    
			$.ajax({
				data: "command=deleteLoadBalancerRule&id="+loadBalancerId+"&response=json",
				dataType: "json",
				success: function(json) {
					var lbJSON = json.deleteloadbalancerruleresponse;
					var timerKey = "deleteLoadBalancerRuleJob_"+lbJSON.jobid;
					$("body").everyTime(
						5000,
						timerKey,
						function() {
							$.ajax({
								data: "command=queryAsyncJobResult&jobId="+lbJSON.jobid+"&response=json",
								dataType: "json",
								success: function(json) {
									var result = json.queryasyncjobresultresponse;
									if (result.jobstatus == 0) {
										return; //Job has not completed
									} else {
										$("body").stopTime(timerKey);
										if (result.jobstatus == 1) { // Succeeded												
											template.slideUp("slow", function() {
												$(this).remove();													
											});
										} else if (result.jobstatus == 2) { // Failed
											loadingContainer.hide(); 	   
	                                        rowContainer.show();	
										}
									}
								},
								error: function(XMLHttpResponse) {										
									handleError(XMLHttpResponse);
									$("body").stopTime(timerKey);
									loadingContainer.hide(); 	   
	                                rowContainer.show();	
								}
							});
						},
						0
					);
				}
			});	     
	        return false;
	    });		
	    		    
        template.find("#edit_link").unbind("click").bind("click", function(event){   		    
	        rowContainer.hide();
	        rowContainerEdit.show();
	    });
	    
	    template.find("#cancel_link").unbind("click").bind("click", function(event){   		    
	        rowContainer.show();
	        rowContainerEdit.hide();
	    });
	    
	    template.find("#save_link").unbind("click").bind("click", function(event){          		       
	        // validate values			       
		    var isValid = true;		
		    isValid &= validateString("Name", rowContainerEdit.find("#name"), rowContainerEdit.find("#name_errormsg"));					    
		    isValid &= validateNumber("Private Port", rowContainerEdit.find("#private_port"), rowContainerEdit.find("#private_port_errormsg"), 1, 65535);				
		    if (!isValid) return;		    		        
		    
	        var loadingContainer = template.find(".adding_loading");	                        
            loadingContainer.find(".adding_text").text("Saving....");	
            loadingContainer.show();  
            rowContainerEdit.hide();      
		        		    	       
	        var name = rowContainerEdit.find("#name").val();  		        
	        var privatePort = rowContainerEdit.find("#private_port").val();
	        var algorithm = rowContainerEdit.find("#algorithm_select").val();  
		    		    
	        var array1 = [];
	        array1.push("&id=" + loadBalancerId);                
            array1.push("&name=" + name);                  
            array1.push("&privateport=" + privatePort);
            array1.push("&algorithm=" + algorithm);
                                                          
            $.ajax({
				data: "command=updateLoadBalancerRule&response=json"+array1.join(""),
				dataType: "json",
				success: function(json) {					    		   	    									 
					var jobId = json.updateloadbalancerruleresponse.jobid;					        
			        var timerKey = "updateloadbalancerruleJob"+jobId;
			        
                    $("body").everyTime(2000, timerKey, function() {
					    $.ajax({
						    data: "command=queryAsyncJobResult&jobId="+jobId+"&response=json",
						    dataType: "json",
						    success: function(json) {										       						   
							    var result = json.queryasyncjobresultresponse;									    
							    if (result.jobstatus == 0) {
								    return; //Job has not completed
							    } else {											    
								    $("body").stopTime(timerKey);
								    if (result.jobstatus == 1) { // Succeeded										        								        						        								    
									    var items = result.loadbalancer;											         	
                                        loadBalancerJsonToTemplate(items[0],template);
                                        loadingContainer.hide(); 	   
                                        rowContainer.show();						                                                               
								    } else if (result.jobstatus == 2) { //Fail
								        loadingContainer.hide(); 		
							            rowContainer.show(); 
									    $("#dialog_alert").html("<p>" + fromdb(result.jobresult) + "</p>").dialog("open");											    					    
								    }
							    }
						    },
						    error: function(XMLHttpResponse) {	
						        handleError(XMLHttpResponse);								        
							    $("body").stopTime(timerKey);
							    loadingContainer.hide(); 		
							    rowContainer.show(); 									    								    
						    }
					    });
				    }, 0);							 
				 },
				 error: function(XMLHttpResponse) {
				     handleError(XMLHttpResponse);		
				     loadingContainer.hide(); 		
					 rowContainer.show(); 							 
				 }
			 });                   
	    });	  		    
	    
	    refreshLbVmSelect(template, json.id);     
	    		   
	    template.find("#add_vm_to_lb_row #add_link").unbind("click").bind("click", function(event){		        
	        var vmOption =  template.find("#add_vm_to_lb_row #vm_select option:selected");
	        var vmId = vmOption.val();  		        
	        var vmName = vmOption.data("vmName");
	        var vmPrivateIp = vmOption.data("vmPrivateIp"); 
			if(vmId	== null || vmId.length == 0)
			    return;						    				
			var loading = template.find("#adding_loading").show();  
			var rowContainer =  template.find("#adding_row_container").hide();
			    	
			$.ajax({
				data: "command=assignToLoadBalancerRule&id="+loadBalancerId+"&virtualmachineid="+vmId+"&response=json",
				dataType: "json",
				success: function(json) {
					var lbInstanceJSON = json.assigntoloadbalancerruleresponse;
					var timerKey = "lbInstanceNew"+lbInstanceJSON.jobid;						
					$("body").everyTime(
						5000,
						timerKey,
						function() {
							$.ajax({
								data: "command=queryAsyncJobResult&jobId="+lbInstanceJSON.jobid+"&response=json",
								dataType: "json",
								success: function(json) {
									var result = json.queryasyncjobresultresponse;
									if (result.jobstatus == 0) {
										return; //Job has not completed
									} else {
										$("body").stopTime(timerKey);
										if (result.jobstatus == 1) { // Succeeded											    
										    var lbVmTemplate = $("#load_balancer_vm_template").clone();											    											    											    
										    var obj = {"loadBalancerId": loadBalancerId, "vmId": vmId, "vmName": vmName, "vmPrivateIp": vmPrivateIp};	
										    lbVmObjToTemplate(obj, lbVmTemplate);		
										    template.find("#vm_subgrid").append(lbVmTemplate.show());	
										    refreshLbVmSelect(template, loadBalancerId);											    
			                                loading.hide();  
			                                rowContainer.show(); 
										} else if (result.jobstatus == 2) { // Failed
											$("#dialog_error").html("<p style='color:red'><b>Operation error:</b></p><br/><p style='color:red'>"+ fromdb(result.jobresult)+"</p>").dialog("open");
											loading.hide();  
											rowContainer.show();  
										}
									}
								},
								error: function(XMLHttpResponse) {										
									handleError(XMLHttpResponse);
									$("body").stopTime(timerKey);
									loading.hide();   
									rowContainer.show(); 
								}
							});
						},
						0
					);
				},
				error: function(XMLHttpResponse) {
			        handleError(XMLHttpResponse);
			        loading.hide();  
			        rowContainer.show();  
				}
			});	        
	        return false;
	    });  
	}	
			
	function lbVmObjToTemplate(obj, template) {
	    template.find("#vm_name").text(obj.vmName);
		template.find("#vm_private_ip").text(obj.vmPrivateIp);			
		template.find("#delete_link").bind("click", function(event){				   
		    var loading = template.find("#deleting_loading").show();
		    var rowContainer = template.find("#deleting_row_container").hide();
		    						    		
	        $.ajax({
				data: "command=removeFromLoadBalancerRule&id="+obj.loadBalancerId+"&virtualmachineid="+obj.vmId+"&response=json",
				dataType: "json",
				success: function(json) {
					var lbJSON = json.removefromloadbalancerruleresponse;
					var timerKey = "removeVmFromLb"+obj.vmId;
					$("body").everyTime(
						5000,
						timerKey,
						function() {
							$.ajax({
								data: "command=queryAsyncJobResult&jobId="+lbJSON.jobid+"&response=json",
								dataType: "json",
								success: function(json) {
									var result = json.queryasyncjobresultresponse;
									if (result.jobstatus == 0) {
										return; //Job has not completed
									} else {
										$("body").stopTime(timerKey);
										if (result.jobstatus == 1) { // Succeeded											    
										    refreshLbVmSelect($("#loadBalancer_" + obj.loadBalancerId), obj.loadBalancerId);
											template.fadeOut("slow", function(event) {
												$(this).remove();
											});
										} else if (result.jobstatus == 2) { // Failed													
											$("#dialog_error").html("<p style='color:red'>We were unable to remove the Virtual Instance: "+vmName + " from your load balancer policy.  Please try again.").dialog("open");
											loading.hide();
											rowContainer.show();
										}
									}
								},
								error: function(XMLHttpResponse) {
									$("body").stopTime(timerKey);
									handleError(XMLHttpResponse);
									loading.hide();
									rowContainer.show();
								}
							});
						},
						0
					);
				},
				error: function(XMLHttpResponse) {
				    handleError(XMLHttpResponse);
				    loading.hide();
				    rowContainer.show();
				}
			});		
		    return false;
		});						
    }		
	
	function refreshLbVmSelect(template, loadBalancerId) {		
	    var vmSelect = template.find("#add_vm_to_lb_row #vm_select");		    	    
        // Load the select box with the VMs that haven't been applied a LB rule to.	        
	    $.ajax({
		    cache: false,
		    data: "command=listLoadBalancerRuleInstances&id="+loadBalancerId+"&applied=false&response=json",
		    dataType: "json",
		    success: function(json) {				        			        
			    var instances = json.listloadbalancerruleinstancesresponse.loadbalancerruleinstance;
			    vmSelect.empty();
			    if (instances != null && instances.length > 0) {
				    for (var i = 0; i < instances.length; i++) {
				        var vmName = getVmName(instances[i].name, instances[i].displayname);
					    html = $("<option value='" + instances[i].id + "'>" + vmName + "</option>")
					    html.data("vmPrivateIp", instances[i].privateip).data("vmName", vmName);
					    vmSelect.append(html); 
				    }
			    } else {
				    vmSelect.append("<option value=''>None Available</option>");
			    }
		    }
	    });			
	}
			
	createLoadBalancerRow.find("#add_link").bind("click", function(event){			    
	    // validate values		    
		var isValid = true;					
		isValid &= validateString("Name", createLoadBalancerRow.find("#name"), createLoadBalancerRow.find("#name_errormsg"));
		isValid &= validateNumber("Public Port", createLoadBalancerRow.find("#public_port"), createLoadBalancerRow.find("#public_port_errormsg"), 1, 65535);
		isValid &= validateNumber("Private Port", createLoadBalancerRow.find("#private_port"), createLoadBalancerRow.find("#private_port_errormsg"), 1, 65535);				
		if (!isValid) return;
		 
		var template = $("#load_balancer_template").clone();	
		loadBalancerGrid.append(template.show());		
		
		var loadingImg = template.find(".adding_loading");		
        var rowContainer = template.find("#row_container");                  
        loadingImg.find(".adding_text").text("Adding....");	
        loadingImg.show();  
        rowContainer.hide();            			 
		 
	    var ipAddress = $(this).data("ip");	    
	    var name = createLoadBalancerRow.find("#name").val();  
	    var publicPort = createLoadBalancerRow.find("#public_port").val();
	    var privatePort = createLoadBalancerRow.find("#private_port").val();
	    var algorithm = createLoadBalancerRow.find("#algorithm_select").val();  
	    		   
	    var array1 = [];
        array1.push("&publicip="+ipAddress);    
        array1.push("&name="+name);              
        array1.push("&publicport="+publicPort);
        array1.push("&privateport="+privatePort);
        array1.push("&algorithm="+algorithm);
       
        $.ajax({
			data: "command=createLoadBalancerRule&response=json"+array1.join(""),
			dataType: "json",
			success: function(json) {					    	    
				var items = json.createloadbalancerruleresponse.loadbalancerrule;						
	            loadBalancerJsonToTemplate(items[0],template);
	            loadingImg.hide(); 	   
	            rowContainer.show();	
	            refreshCreateLoadBalancerRow();	            	
			},
		    error: function(XMLHttpResponse) {				    
			    handleError(XMLHttpResponse);
			    template.slideUp("slow", function() {
					$(this).remove();
				});
		    }			
		});  
	    
	    return false;
	});
	//*** Load Balancer (begin) ***
        
   
    $("#submenu_network").bind("click", function(event) {		        
        currentSubMenu.removeClass().addClass("submenu_links_off");   
		currentSubMenu = $(this);
		currentSubMenu.removeClass().addClass("submenu_links_on");
		
		var submenuContent = $("#submenu_content_network").show();
		$("#submenu_content_network_groups").hide();			
	    
	    if(isAdmin() || isDomainAdmin()) {
	        submenuContent.find(".select_directipbg_admin").show();
	        submenuContent.find(".select_directipbg_user").hide();	
	        populateDomainDropdown();	    
	    } else {	        
	        submenuContent.find(".select_directipbg_admin").hide();
	        submenuContent.find(".select_directipbg_user").show();
	        RefreshIpDropDown();
	    }
	    		    
		return false;
	});

	var ipMid = $("#submenu_content_network #ip_descriptionbox_mid");
	
	$("#submenu_content_network #show_last_search").bind("click", function(event){
        $("#submenu_content_network").find("#show_last_search").hide();	
        hidePfLbArea(); 							
		var ips = ipListContainer.children();			
		for (var i = 0; i < ips.length; i++) {
			var ip = $(ips[i]);
			ip.show();
							
			if(isIpManageable(ip.data("domainid"), ip.data("account")) == true) 
			    ip.find("#ip_manage").show();	
		}        
        return false;
    });
	
	ipListContainer.bind("click", function(event) {
		var target = $(event.target);
		var id = target.attr("id");
		var targetIp = target.data("ip");
		var domainid = target.data("domainid");
		var account = target.data("account");	
		
		switch (id) {
			case "ip_manage" :
				var ips = ipListContainer.children();
				var ipSelected = null;
				var first = false;
				ipMid.css("height", ipMid.height());
				for (var i = 0; i < ips.length; i++) {
					var ip = $(ips[i]);
					if (ip.data("ip") != targetIp) {
						ip.fadeOut("fast");
					} else {
						if (i == 0) first = true;
						var ipPosition = ip.position();
						ipSelected = ip.css("position", "absolute").css("top", ipPosition.top);
					}
				}
				var animationSpeed = 2000;
				if (first) {
					animationSpeed = 500;
				}
				ipSelected.animate({top: '24px'}, animationSpeed, function() {
					$(this).css("position", "static").css("float", "left");
					ipMid.css("height", "auto");
					target.hide();
					$("#submenu_content_network").find("#show_last_search").show();
					showPfLbArea(targetIp, domainid, account);
				});
				break;
			case "ip_release" :
				$("#dialog_confirmation")
					.html("<p>Please confirm you want to release the IP address: <b>"+targetIp+"</b> from your account.</p>")
					.dialog('option', 'buttons', { 						
						"Confirm": function() { 
							$(this).dialog("close"); 
							
							$("#submenu_content_network #overlay_white").show();
			                $("#submenu_content_network #loading_gridtable").find("#message").text("Releasing IP....");					    
			                $("#submenu_content_network #loading_gridtable").show();	
			                								
							$.ajax({
								data: "command=disassociateIpAddress&ipAddress="+targetIp+"&response=json",
								dataType: "json",
								success: function(json) {				    		
									$("#dialog_info").html("<p>Your IP address <b>" + targetIp + "</b> has been released</p>").dialog("open");
									$("#submenu_content_network #overlay_white").hide();
			                        $("#submenu_content_network #loading_gridtable").hide();
									if (isUser()) {
										RefreshIpDropDown();
									} else {
										// Execute the codepath for showing last search result.											
										var ips = ipListContainer.children();	 
				                        if(ips != null && ips.length > 0) {					                            
				                            for (var i = 0; i < ips.length; i++) {
					                            var ip = $(ips[i]);
					                            if (ip.data("ip") == targetIp) {
						                            ip.fadeOut("fast", function(){							                                
						                                $(this).remove(); 
						                                							                                
						                                if(ipPanel.data("ip_address") == targetIp)
                                                            ipPanel.removeData("ip_address");
						                                 
						                                if(ipListContainer.children().length == 0) { 
					                                        $("#submenu_content_network #show_last_search").hide();
					                                    } else {  
					                                        $("#submenu_content_network #show_last_search").show();
					                                        $("#submenu_content_network #show_last_search").click();
					                                    }	                               
						                            });		
						                        }		                    			                        
				                            }                           
				                        }										
									}
								},
								error: function(XMLHttpResponse) {		                   
	                                handleError(XMLHttpResponse);	
								    $("#submenu_content_network #overlay_white").hide();
			                        $("#submenu_content_network #loading_gridtable").hide();
								}
							}); 	
						}, 
						"Cancel": function() { 
							$(this).dialog("close"); 
						} 
					}).dialog("open");
				break;
			default :
				break;
		}
		return false;
	});
	
	//*** Network (end) ******************************************************************************
	
	//*** Network Group (begin) **********************************************************************	   
    function networkGroupJSONToTemplate(json, template) {	       
        (index++ % 2 == 0)? template.addClass("smallrow_even"): template.addClass("smallrow_odd");	    
        template.attr("id", "networkGroup_"+json.id).data("networkGroupId", json.id).data("domainId", json.domainid).data("account",json.account).data("networkGroupName", fromdb(json.name));	      		    				   
	    template.find("#id").text(json.id);
	    template.find("#name").text(json.name);
	    template.find("#description").text(json.description);	      
	    template.find("#domain").text(json.domain); 
	    template.find("#account").text(json.account);  		 
	    
		// disable delete link from the default group
	    if(json.name == 'default' && json.description == 'Default Network Group') {
			template.find("#delete_link").hide();
		} else {
			if(json.ingressrule == null || json.ingressrule.length == 0)
				template.find("#delete_link").show();
			else
				template.find("#delete_link").hide();
		}
    }
    	    
    function listNetworkGroups() {       
        var submenuContent = $("#submenu_content_network_groups");                                    
	     
	    var commandString;            
		var advanced = submenuContent.find("#search_button").data("advanced");                    
		if (advanced != null && advanced) {		
		    var name = submenuContent.find("#advanced_search #adv_search_name").val();	   
		    var virtualMachineId = submenuContent.find("#advanced_search #adv_search_vm").val();				   
		    var domainId = submenuContent.find("#advanced_search #adv_search_domain").val();
		    var account = submenuContent.find("#advanced_search #adv_search_account").val();
		    var moreCriteria = [];								
			if (name!=null && trim(name).length > 0) 
				moreCriteria.push("&networkgroupname="+encodeURIComponent(trim(name)));						
			if (virtualMachineId!=null && virtualMachineId.length > 0) 
				moreCriteria.push("&virtualmachineid="+encodeURIComponent(virtualMachineId));	   
			if (domainId!=null && domainId.length > 0) 
				moreCriteria.push("&domainid="+domainId);		
			if (account!=null && account.length > 0) 
				moreCriteria.push("&account="+account);			
			commandString = "command=listNetworkGroups&page=" + currentPage + moreCriteria.join("") + "&response=json";		
		} else {    
		     var moreCriteria = [];		
		    if(domainId!=null)
		        moreCriteria.push("&domainid="+domainId);				   			  
            var searchInput = submenuContent.find("#search_input").val();            
            if (searchInput != null && searchInput.length > 0) 
                commandString = "command=listNetworkGroups&page=" + currentPage + moreCriteria.join("") + "&keyword=" + searchInput + "&response=json"
            else
                commandString = "command=listNetworkGroups&page=" + currentPage + moreCriteria.join("") + "&response=json";		
        }    	              
        
        //listItems(submenuContent, commandString, jsonResponse1, jsonResponse2, template, fnJSONToTemplate);         
        listItems(submenuContent, commandString, "listnetworkgroupsresponse", "networkgroup", $("#network_group_template"), networkGroupJSONToTemplate);          
    }	    
    
    submenuContentEventBinder($("#submenu_content_network_groups"), listNetworkGroups);	   
    
    $("#submenu_network_groups").bind("click", function(event) {		        
		currentSubMenu.removeClass().addClass("submenu_links_off");   
		currentSubMenu = $(this);
		currentSubMenu.removeClass().addClass("submenu_links_on");
		
		var submenuContent = $("#submenu_content_network_groups").show();
		$("#submenu_content_network").hide();	
					
		if (isAdmin())
	        submenuContent.find("#adv_search_domain_li, #adv_search_account_li").show();
				
		currentPage = 1;			
		listNetworkGroups();			
		return false;
	});	
	
	$("#network_group_template").bind("click", function(event) {
	    var template = $(this);		
	    var networkGroupId = template.data("networkGroupId");    		
	    var domainId = template.data("domainId");
	    var account = template.data("account");
	    var networkGroupId = template.data("networkGroupId");	
	    var networkGroupName = template.data("networkGroupName");
	    var link = $(event.target);	    
	    var submenuContent = $("#submenu_content_network_groups");		   
	    switch(event.target.id) {
	        case "delete_link":   
	            if(template.find("#ingress_rule_panel").css("display")=="block") //if network group's ingress rule grid is poped down, close it.
					template.find("#ingress_rule_grid").click();   
	            var loadingImg = template.find(".adding_loading");		
                var rowContainer = template.find("#row_container");                                                
                loadingImg.find(".adding_text").text("Deleting....");	
                loadingImg.show();  
                rowContainer.hide();                    	                                        
                                   
                var array1 = [];
                array1.push("&domainid="+domainId);
                array1.push("&account="+account);
                array1.push("&name="+networkGroupName);                    
                
                $.ajax({
                    data: "command=deleteNetworkGroup&response=json" + array1.join(""), //uncomment this line and delete the next line when deleteNetworkGroup API is available.
                    dataType: "json",
                    success: function(json) {		                        	                                               				        
                        template.slideUp("slow", function() { $(this).remove() });
						changeGridRowsTotal(submenuContent.find("#grid_rows_total"), -1);           		    					
                    },
                    error: function(XMLHttpResponse) {                                                			    
		                handleError(XMLHttpResponse);
		                loadingImg.hide();  
                        rowContainer.show();								
                    }
                });	 		        
	            break;	    
	        
	        case "ingress_rule_link":			                     		            		        
				var expanded = link.data("expanded");
				if (expanded == null || expanded == false) {						 					    			    
				    $.ajax({
	                    data: "command=listNetworkGroups"+"&domainid="+domainId+"&account="+account+"&networkgroupname="+networkGroupName+"&response=json",
	                    dataType: "json",		                    
	                    success: function(json) {			                        		                       
	                        var items = json.listnetworkgroupsresponse.networkgroup[0].ingressrule;                  
	                        var grid = template.find("#ingress_rule_grid");								
					        if(grid.find("#network_group_ingress_rule_add_row").length==0)											    
					            grid.append($("#network_group_ingress_rule_add_row").clone().show());						
					        if (items != null && items.length > 0) {									    
					            grid.empty();		
					            grid.append($("#network_group_ingress_rule_add_row").clone().show()); //need to append "add ingress rule" row again after emptying grid.							    				    															
						        for (var i = 0; i < items.length; i++) {			
						            var newTemplate = $("#network_group_ingress_rule_template").clone(true);
	                                ingressRuleJSONToTemplate(items[i], newTemplate).data("parentNetworkGroupId", networkGroupId).data("parentNetworkGroupDomainId", domainId).data("parentNetworkGroupAccount", account).data("parentNetworkGroupName",networkGroupName); 
	                                grid.append(newTemplate.show());																	
						        }					        
					        }
					        link.removeClass().addClass("vm_botactionslinks_up");
					        template.find("#ingress_rule_panel").slideDown("slow");						
					        link.data("expanded", true);                          			   
	                    }
                    });			
				} else {
					link.removeClass().addClass("vm_botactionslinks_down");
					template.find("#ingress_rule_panel").slideUp("slow");
					link.data("expanded", false);
				}
				break;	
				
		    case "network_group_ingress_rule_add_link":	       
		        dialogAddIngressRule.find("#start_port").val("");
		        cleanErrMsg(dialogAddIngressRule.find("#start_port"), dialogAddIngressRule.find("#start_port_errormsg"));
		        
		        dialogAddIngressRule.find("#end_port").val("");
		        cleanErrMsg(dialogAddIngressRule.find("#end_port"), dialogAddIngressRule.find("#end_port_errormsg"));
		        
		        dialogAddIngressRule.find("#protocol").val("TCP");
		        
		        dialogAddIngressRule.find("#cidr_container").empty();
		        dialogAddIngressRule.find("#add_more_cidr").click();
		        
		        dialogAddIngressRule.find("#account_networkgroup_container").empty();
		        dialogAddIngressRule.find("#add_more_account_networkgroup").click();
		        
		        $("#dialog_add_ingress_rule")
	            .dialog('option', 'buttons', { 			    
		            "Add": function() { 
		                var thisDialog = $(this);
    			    	
    			    	var moreCriteria = [];	
    			    	moreCriteria.push("&domainid="+domainId);
                        moreCriteria.push("&account="+account);
                        moreCriteria.push("&networkgroupname="+networkGroupName);        
    			    	
    			    	var protocol = thisDialog.find("#protocol").val();
			            if (protocol!=null && protocol.length > 0) 
				            moreCriteria.push("&protocol="+encodeURIComponent(protocol));	
				            	            										
		                // validate values (begin)							
			            var isValid = true;				
			            if(protocol == "ICMP") {					
			                isValid &= validateNumber("Type", thisDialog.find("#icmp_type"), thisDialog.find("#icmp_type_errormsg"), -1, 40, false);	//required	
			                isValid &= validateNumber("Code", thisDialog.find("#icmp_code"), thisDialog.find("#icmp_code_errormsg"), -1 , 15, false);	//required
			            }	
			            else {  //TCP, UDP
			                isValid &= validateNumber("Start Port", thisDialog.find("#start_port"), thisDialog.find("#start_port_errormsg"), 1, 65535, false);	//required	
			                isValid &= validateNumber("End Port", thisDialog.find("#end_port"), thisDialog.find("#end_port_errormsg"), 1, 65535, false);	//required
			            }
			            				          	
			            if(thisDialog.find("input[name='ingress_rule_type']:checked").val() == "cidr") {					                
			                isValid &= validateCIDR("CIDR", thisDialog.find(".cidr_template").eq(0).find("#cidr"), thisDialog.find(".cidr_template").eq(0).find("#cidr_errormsg"), false); //required                
	                        for(var i=1; i<thisDialog.find(".cidr_template").length; i++)
	                            isValid &= validateCIDR("CIDR", thisDialog.find(".cidr_template").eq(i).find("#cidr"), thisDialog.find(".cidr_template").eq(0).find("#cidr_errormsg"), true); //optional        
	                    }
	                    else if(thisDialog.find("input[name='ingress_rule_type']:checked").val() == "account_networkgroup") {			                        
	                        isValid &= validateString("Account", thisDialog.find(".account_networkgroup_template").eq(0).find("#account"), thisDialog.find(".account_networkgroup_template").eq(0).find("#account_networkgroup_template_errormsg"), false);	 //required                
	                        isValid &= validateString("Network Group", thisDialog.find(".account_networkgroup_template").eq(0).find("#networkgroup"), thisDialog.find(".account_networkgroup_template").eq(0).find("#account_networkgroup_template_errormsg"), false);	 //required             
	                        for(var i=1; i<thisDialog.find(".account_networkgroup_template").length; i++) {
	                            isValid &= validateString("Account", thisDialog.find(".account_networkgroup_template").eq(i).find("#account"), thisDialog.find(".account_networkgroup_template").eq(0).find("#account_networkgroup_template_errormsg"), true);	 //optional          
	                            isValid &= validateString("Network Group", thisDialog.find(".account_networkgroup_template").eq(i).find("#networkgroup"), thisDialog.find(".account_networkgroup_template").eq(0).find("#account_networkgroup_template_errormsg"), true);	 //optional     
	                        }
	                    }					            
			            if (!isValid) return;					
    					// validate values (end)
    					          							
    					if(protocol == "ICMP") {        					    
    					    var icmpType = thisDialog.find("#icmp_type").val();	
    					    if (icmpType!=null && icmpType.length > 0)         					        
				                moreCriteria.push("&icmptype="+encodeURIComponent(icmpType));						            
				            var icmpCode = thisDialog.find("#icmp_code").val();
			                if (icmpCode!=null && icmpCode.length > 0) 				                    
				                moreCriteria.push("&icmpcode="+encodeURIComponent(icmpCode)); 					              					    
    					}
    					else {  //TCP, UDP
    					    var startPort = thisDialog.find("#start_port").val();	
    					    if (startPort!=null && startPort.length > 0) 
				                moreCriteria.push("&startport="+encodeURIComponent(startPort));	
				            var endPort = thisDialog.find("#end_port").val();
			                if (endPort!=null && endPort.length > 0) 
				                moreCriteria.push("&endport="+encodeURIComponent(endPort));	
    					}        					            
			            				            				                                      
                        if(dialogAddIngressRule.find("input[name='ingress_rule_type']:checked").val() == "cidr") {	
                            var array1 = [];	        
	                        var cidrElementArray = dialogAddIngressRule.find(".cidr_template").find("#cidr");			                        
	                        for(var i=0; i<cidrElementArray.length; i++) {
	                            if(cidrElementArray[i].value.length >  0)
	                                array1.push(cidrElementArray[i].value);
	                        }
	                        if(array1.length > 0)
			                    moreCriteria.push("&cidrlist="+encodeURIComponent(array1.join(",")));	
	                    }	    
	                    else if(dialogAddIngressRule.find("input[name='ingress_rule_type']:checked").val() == "account_networkgroup") {			                          
	                        var accountElementArray = dialogAddIngressRule.find(".account_networkgroup_template").find("#account");	
	                        var networkgroupElementArray = dialogAddIngressRule.find(".account_networkgroup_template").find("#networkgroup");			                        
	                        for(var i=0; i<accountElementArray.length; i++)	{	  
	                            if(networkgroupElementArray[i].value.length > 0 && accountElementArray[i].value.length > 0)                        
	                                moreCriteria.push("&usernetworkgrouplist["+i+"].account="+accountElementArray[i].value+"&usernetworkgrouplist["+i+"].group="+networkgroupElementArray[i].value);
	                        }
	                    }	  		           
			            				            					            	   
			            thisDialog.dialog("close");		      
                                       
				        var ingressRuleTemplate = $("#network_group_ingress_rule_template").clone(true);											   
                        var loadingImg = ingressRuleTemplate.find(".adding_loading");		
                        var rowContainer = ingressRuleTemplate.find("#row_container");    	                               
                        loadingImg.find(".adding_text").text("Adding....");	
                        loadingImg.show();  
                        rowContainer.hide();                                
                        template.find("#ingress_rule_grid").append(ingressRuleTemplate.fadeIn("slow"));	
                        template.find("#ingress_rule_grid").find("#no_ingress_rule").hide();             
                        
                        $.ajax({
					        data: "command=authorizeNetworkGroupIngress"+moreCriteria.join("")+"&response=json",
					        dataType: "json",
					        success: function(json) {					            		            				
					            var jobId = json.authorizenetworkgroupingress.jobid; 						            	                   
			                    var timerKey = "ingressRuleJob_"+jobId;
			                    ingressRuleTemplate.attr("id","ingressRule_"+jobId); //temporary id until API call returns real id							        
						        $("body").everyTime(
							        5000,
							        timerKey,
							        function() {
								        $.ajax({
									        data: "command=queryAsyncJobResult&jobId="+jobId+"&response=json",
									        dataType: "json",
									        success: function(json) {
										        var result = json.queryasyncjobresultresponse;
										        if (result.jobstatus == 0) {
											        return; //Job has not completed
										        } else {
											        $("body").stopTime(timerKey);
											        if (result.jobstatus == 1) { // Succeeded													            							            
											            var items = result.networkgroup[0].ingressrule;			            
											            ingressRuleJSONToTemplate(items[0], ingressRuleTemplate).data("parentNetworkGroupId", networkGroupId).data("parentNetworkGroupDomainId", domainId).data("parentNetworkGroupAccount", account).data("parentNetworkGroupName",networkGroupName);													            
											            if(items.length > 1) {                               
                                                            for(var i=1; i<items.length; i++) {         
                                                                var ingressRuleTemplate2 = $("#network_group_ingress_rule_template").clone(true);                                                               
                                                                ingressRuleJSONToTemplate(items[i], ingressRuleTemplate2).data("parentNetworkGroupId", networkGroupId).data("parentNetworkGroupDomainId", domainId).data("parentNetworkGroupAccount", account).data("parentNetworkGroupName",networkGroupName);	   
                                                                template.find("#ingress_rule_grid").append(ingressRuleTemplate2.fadeIn("slow"));	                                                                 
                                                            }                                    
                                                        }  												        		
												        loadingImg.hide();  
                                                        rowContainer.show();                                                              
                                                        //hide delete link of network group. (network group is not allowed to delete if it is not empty. i.e. having ingress rule(s) 
                                                        template.find("#delete_link").hide();                                                                                                             												
											        } else if (result.jobstatus == 2) { // Failed
											            $("#dialog_alert").text("Unable to add ingress rule due to the error: " + result.jobresult).dialog("open");
												        ingressRuleTemplate.slideUp("slow", function() {
													        $(this).remove();
												        });
											        }
										        }
									        },
									        error: function(XMLHttpResponse) {
										        $("body").stopTime(timerKey);
										        handleError(XMLHttpResponse);										 
										        ingressRuleTemplate.slideUp("slow", function() {
											        $(this).remove();
										        });
									        }
								        });
							        },
							        0
						        );				        
					        },						
					        error: function(XMLHttpResponse) {							
						        handleError(XMLHttpResponse);										 
						        ingressRuleTemplate.slideUp("slow", function() {
							        $(this).remove();
						        });
					        }						
				        });				
			        }, 
		            "Cancel": function() { 				        				        
			            $(this).dialog("close"); 
		            } 
	            }).dialog("open");    
		        break;		           
	    }
	    		    
	    return false;
	});
	
	activateDialog($("#dialog_add_network_groups").dialog({ 			
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));	
	
	activateDialog($("#dialog_add_ingress_rule").dialog({ 
		width: 400,
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));				
	
	function ingressRuleJSONToTemplate(json, template) {
	    (index++ % 2 == 0)? template.addClass("smallrow_even"): template.addClass("smallrow_odd");		    
        template.attr("id", "ingressRule_"+json.id).data("ingressRuleId", json.ruleid).data("protocol", json.protocol);	 
        template.data("startPort", json.startport); 
        template.data("endPort", json.endport);           
        template.data("icmpType", json.icmptype);
        template.data("icmpCode", json.icmpcode);	    				   
	    template.find("#id").text(json.ruleid);
	    template.find("#protocol").text(json.protocol);
	    			    		    
	    var endpoint;		    
	    if(json.protocol == "icmp")
	        endpoint = "ICMP Type=" + ((json.icmptype!=null)?json.icmptype:"") + ", code=" + ((json.icmpcode!=null)?json.icmpcode:"");		        
	    else //tcp, udp
	        endpoint = "Port Range " + ((json.startport!=null)?json.startport:"") + "-" + ((json.endport!=null)?json.endport:"");		    
	    template.find("#endpoint").text(endpoint);		    
	    template.data("startPort", json.startport); 
        template.data("endPort", json.endport);           
        template.data("icmpType", json.icmptype);
        template.data("icmpCode", json.icmpcode);			    
	    
	    var cidrOrGroup;
	    if(json.cidr != null && json.cidr.length > 0)
	        cidrOrGroup = json.cidr;
	    else if (json.account != null && json.account.length > 0 &&  json.networkgroupname != null && json.networkgroupname.length > 0)
	        cidrOrGroup = json.account + "/" + json.networkgroupname;		    
	    template.find("#cidr").text(cidrOrGroup);		    		    
	    template.data("cidr", json.cidr);		    
	    template.data("account", json.account);		    
	    template.data("networkGroupName", json.networkgroupname);	
	    
	    return template;	   
    }
		
	$("#network_group_ingress_rule_template").bind("click", function(event) {
        var template = $(this);	
                   
        var parentNetworkGroupId = template.data("parentNetworkGroupId");
        var parentNeteworkGroupTemplate = $("#networkGroup_" + parentNetworkGroupId);
                    
        var moreCriteria = [];		   	
	    
	    var parentNetworkGroupDomainId = template.data("parentNetworkGroupDomainId");
	    moreCriteria.push("&domainid="+encodeURIComponent(parentNetworkGroupDomainId));
	    
	    var parentNetworkGroupAccount = template.data("parentNetworkGroupAccount");
	    moreCriteria.push("&account="+encodeURIComponent(parentNetworkGroupAccount));
	    		    
	    var parentNetworkGroupName = template.data("parentNetworkGroupName");
	    moreCriteria.push("&networkgroupname="+encodeURIComponent(parentNetworkGroupName));    
	    
	    var protocol = template.data("protocol");
	    moreCriteria.push("&protocol="+encodeURIComponent(protocol));		    	
	    
	    if(protocol == "icmp") {
	        var icmpType = template.data("icmpType");
	        if(icmpType != null && icmpType.length > 0)
	            moreCriteria.push("&icmptype="+encodeURIComponent(icmpType));
		    
	        var icmpCode = template.data("icmpCode");
	        if(icmpCode != null && icmpCode.length > 0)
	            moreCriteria.push("&icmpcode="+encodeURIComponent(icmpCode));
	    }
	    else {  //TCP, UDP
	        var startPort = template.data("startPort");
	        if(startPort != null && startPort.length > 0)
	            moreCriteria.push("&startport="+encodeURIComponent(startPort));
		    
	        var endPort = template.data("endPort");
	        if(endPort != null && endPort.length > 0)
	            moreCriteria.push("&endport="+encodeURIComponent(endPort));
	    }
	        
	    var cidr = template.data("cidr")
	    if(cidr != null && cidr.length > 0)
	        moreCriteria.push("&cidrlist="+encodeURIComponent(cidr));
						
	    var account = template.data("account");
	    var networkGroupName = template.data("networkGroupName"); 
	    if((account != null && account.length > 0) && (networkGroupName != null && networkGroupName.length > 0))                        
            moreCriteria.push("&usernetworkgrouplist[0].account="+account + "&usernetworkgrouplist[0].group="+networkGroupName);
			
	    var link = $(event.target);	 		    		        
	    switch(event.target.id) {
	        case "ingress_rule_delete_link":  	            		            
	            var loadingImg = template.find(".adding_loading");		
                var rowContainer = template.find("#row_container");                                                
                loadingImg.find(".adding_text").text("Deleting....");	
                loadingImg.show();  
                rowContainer.hide();              
                $.ajax({
                    data: "command=revokeNetworkGroupIngress"+moreCriteria.join("")+"&response=json", 
                    dataType: "json",
                    success: function(json) {		                                                                                            				        
                        var jobId = json.revokenetworkgroupingress.jobid;	                    
                        var timerKey = "revokeNetworkGroupIngressJob"+jobId;								    
                        $("body").everyTime(2000, timerKey, function() {
		                    $.ajax({
			                    data: "command=queryAsyncJobResult&jobId="+jobId+"&response=json", 
			                    dataType: "json",
			                    success: function(json) {					                        							       						   
				                    var result = json.queryasyncjobresultresponse;
				                    if (result.jobstatus == 0) {
					                    return; //Job has not completed
				                    } else {											    
					                    $("body").stopTime(timerKey);
					                    if (result.jobstatus == 1) { //success		
                                            template.slideUp("slow", function() { 
                                                $(this).remove();                                                                                                        
                                                //After deleting ingress rule successfully, check if this network group has any ingress rule(s) left. Show delete link of network group if no ingress rule(s) are left.
                                                $.ajax({
                                                    data:"command=listNetworkGroups&response=json&domainid="+parentNetworkGroupDomainId+"&account="+parentNetworkGroupAccount+"&networkgroupname="+parentNetworkGroupName,
                                                    dataType: "json",
                                                    success: function(json){                                                                                                                     
                                                        networkGroupJSONToTemplate(json.listnetworkgroupsresponse.networkgroup[0], parentNeteworkGroupTemplate);
                                                    }
                                                });                                                        
                                            });							                                                           
					                    } else if (result.jobstatus == 2) {										        
						                    $("#dialog_alert").html("<p>" + fromdb(result.jobresult) + "</p>").dialog("open");		
						                    loadingImg.hide();  
                                            rowContainer.show();                                                         									   					    
					                    }
				                    }
			                    },
			                    error: function(XMLHttpResponse) {				                        
				                    $("body").stopTime(timerKey);
				                    handleError(XMLHttpResponse);	
				                    loadingImg.hide();  
                                    rowContainer.show();									    
			                    }
		                    });
	                    }, 0);						    					
                    },
                    error: function(XMLHttpResponse) {		                                  			    
		                handleError(XMLHttpResponse);
		                loadingImg.hide();  
                        rowContainer.show();								
                    }
                });	 		        
	            break;			                      
	    }		    
	    return false;
	});	
	
	//*** event handler of dialog_add_ingress_rule (begin) ***
	var dialogAddIngressRule = $("#dialog_add_ingress_rule");
				
	dialogAddIngressRule.find("#add_more_cidr").bind("click", function(event){		    
        dialogAddIngressRule.find("#cidr_container").append($("#cidr_template").clone().show());
        return false;
    });	
	dialogAddIngressRule.find("#add_more_cidr").click();
	
		
	dialogAddIngressRule.find("#add_more_account_networkgroup").bind("click", function(event){		    
        dialogAddIngressRule.find("#account_networkgroup_container").append($("#account_networkgroup_template").clone().show());
        return false;
    });		
    dialogAddIngressRule.find("#add_more_account_networkgroup").click();
    
				
	dialogAddIngressRule.find("input[name='ingress_rule_type']").change(function(){		    
	    if(dialogAddIngressRule.find("input[name='ingress_rule_type']:checked").val() == "cidr") {	
	        //enable CIDR	        
	        dialogAddIngressRule.find(".cidr_template, #add_more_cidr").removeAttr("disabled");	 
	        	        
	        //disable Account/Network Group, clear up error fields 	        
	        dialogAddIngressRule.find(".account_networkgroup_template, #add_more_account_networkgroup").attr("disabled", "disabled"); 
	        cleanErrMsg(dialogAddIngressRule.find(".account_networkgroup_template").find("#account"), dialogAddIngressRule.find(".account_networkgroup_template").find("#account_networkgroup_template_errormsg")); 
    		cleanErrMsg(dialogAddIngressRule.find(".account_networkgroup_template").find("#networkgroup"), dialogAddIngressRule.find(".account_networkgroup_template").find("#account_networkgroup_template_errormsg")); 	        	        
	    }
	    else if(dialogAddIngressRule.find("input[name='ingress_rule_type']:checked").val() == "account_networkgroup") {
	        //enable Account/Network Group
	        dialogAddIngressRule.find(".account_networkgroup_template, #add_more_account_networkgroup").removeAttr("disabled");	
	        
	        //disable CIDR, clear up error fields
	        dialogAddIngressRule.find(".cidr_template, #add_more_cidr").attr("disabled", "disabled");   
	        cleanErrMsg(dialogAddIngressRule.find(".cidr_template").find("#cidr"), dialogAddIngressRule.find(".cidr_template").find("#cidr_errormsg")); 		              
	    }
	});
	dialogAddIngressRule.find("input[name='ingress_rule_type']").change();
	
	
	dialogAddIngressRule.find("#protocol").bind("change", function(event){		    
	    var thisDropDown = $(this);		   
	    if(thisDropDown.val() == "ICMP") {		        
	        dialogAddIngressRule.find("#icmp_type_container, #icmp_code_container").show();
	        dialogAddIngressRule.find("#icmp_type, #icmp_code").val("-1");
	        
	        dialogAddIngressRule.find("#start_port_container, #end_port_container").hide();		        
	        dialogAddIngressRule.find("#start_port, #end_port").val("");	
	        cleanErrMsg(dialogAddIngressRule.find("#start_port"), dialogAddIngressRule.find("#start_port_errormsg"));	  
	        cleanErrMsg(dialogAddIngressRule.find("#end_port"), dialogAddIngressRule.find("#end_port_errormsg"));	                
	    }
	    else {  //TCP, UDP
	        dialogAddIngressRule.find("#start_port_container, #end_port_container").show();	 
	        
	        dialogAddIngressRule.find("#icmp_type_container, #icmp_code_container").hide();
	        dialogAddIngressRule.find("#icmp_type, #icmp_code").val("");
	        cleanErrMsg(dialogAddIngressRule.find("#icmp_type"),dialogAddIngressRule.find("#icmp_type_errormsg"));	     
	        cleanErrMsg(dialogAddIngressRule.find("#icmp_code"),dialogAddIngressRule.find("#icmp_code_errormsg"));	
	    }		
	    return false;
	});
	dialogAddIngressRule.find("#protocol").change();
	//*** event handler of dialog_add_ingress_rule (end) ***
				
	$("#network_groups_action_new").bind("click", function(event){		    
	    $("#dialog_add_network_groups")
	    .dialog('option', 'buttons', {
	        "Create": function() {		            	          
	            var thisDialog = $(this);	
							
				// validate values
				var isValid = true;
				isValid &= validateString("Name", thisDialog.find("#name"), thisDialog.find("#name_errormsg"), false);  //required
				isValid &= validateString("Description", thisDialog.find("#description"), thisDialog.find("#description_errormsg"), true);	//optional				
				if (!isValid) return;	
				
				var submenuContent = $("#submenu_content_network_groups");
				
				var template = $("#network_group_template").clone(true);
				var loadingImg = template.find(".adding_loading");		
                var rowContainer = template.find("#row_container");    	                               
                loadingImg.find(".adding_text").text("Adding....");	
                loadingImg.show();  
                rowContainer.hide();                                   
                submenuContent.find("#grid_content").prepend(template.fadeIn("slow"));    					
				
				var name = trim(thisDialog.find("#name").val());
				var desc = trim(thisDialog.find("#description").val());
				
				thisDialog.dialog("close");
							
				$.ajax({						
					data: "command=createNetworkGroup&name="+encodeURIComponent(name)+"&description="+encodeURIComponent(desc)+"&response=json",
					dataType: "json",
					success: function(json) {						   
						var items = json.createnetworkgroupresponse.networkgroup;													
						networkGroupJSONToTemplate(items[0], template);							
						changeGridRowsTotal(submenuContent.find("#grid_rows_total"), 1);	
						loadingImg.hide();  
                        rowContainer.show();    						
					}, 								
                    error: function(XMLHttpResponse) {		                   
	                    handleError(XMLHttpResponse);	
	                    template.slideUp("slow", function(){ $(this).remove(); } );							    
                    }	
				});						            
	        },
	        "Cancel": function() {
	            $(this).dialog("close");
	        }
	    }).dialog("open");		    
	    return false;
	});	
				
	//*** Network Group (end) ************************************************************************	   		
	
	
	//initialize page
	if(getDirectAttachNetworkGroupsEnabled() != "true") 
	    $(".submenu_links, #submenu_content_network_groups").hide();		
	
	var currentSubMenu = $("#submenu_network");	
	currentSubMenu.click();	
}
