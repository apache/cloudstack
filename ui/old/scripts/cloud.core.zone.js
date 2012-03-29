function afterLoadZoneJSP($leftmenuItem1) {
    hideMiddleMenu();  
        
    var $topButtonContainer = clearButtonsOnTop();			    	       
	$("#top_buttons").appendTo($topButtonContainer);     
        
    initDialog("dialog_add_external_cluster_in_zone_page", 320);
    initDialog("dialog_add_pod", 370);      
    initDialog("dialog_add_host_in_zone_page", 400); 
	initDialog("dialog_add_pool_in_zone_page", 400);
      
    $.ajax({
        data: createURL("command=listHypervisors"),
        dataType: "json",
        success: function(json) {            
            var items = json.listhypervisorsresponse.hypervisor;
            var $hypervisorDropdown = $("#dialog_add_external_cluster_in_zone_page").find("#cluster_hypervisor");
            if(items != null && items.length > 0) {                
                for(var i=0; i<items.length; i++) {                    
                    $hypervisorDropdown.append("<option value='"+fromdb(items[i].name)+"'>"+fromdb(items[i].name)+"</option>");
                }
            }
        }    
    });    
        
    //switch between different tabs in zone page    
    var tabArray = [$("#tab_details")];
    var tabContentArray = [$("#tab_content_details")];      
    var afterSwitchFnArray = [zoneJsonToDetailsTab];
    switchBetweenDifferentTabs(tabArray, tabContentArray, afterSwitchFnArray); 
         
    zoneRefreshDataBinding();    	
}

function zoneRefreshDataBinding() {    
    cancelEditMode($("#tab_content_details"));      
    var $zoneNode = $selectedSubMenu.parent();     
    zoneJsonToRightPanel($zoneNode);		  
}

function zoneJsonToRightPanel($leftmenuItem1) {	       
    $("#right_panel_content").data("$leftmenuItem1", $leftmenuItem1);      
           
    bindAddPodButton($("#add_pod_button"), $leftmenuItem1);   
          
    var pods;
    var zoneObj = $leftmenuItem1.data("jsonObj");
    var zoneId = zoneObj.id;
    var zoneName = zoneObj.name;
           
    $.ajax({
        data: createURL("command=listPods&zoneid="+zoneId),
        dataType: "json",
        async: false,
        success: function(json) {            
            pods = json.listpodsresponse.pod;            
        }        
    });
    
    bindAddClusterButtonOnZonePage($("#add_cluster_button"), zoneId, zoneName); 
    bindAddHostButtonOnZonePage($("#add_host_button"), zoneId, zoneName); 
    bindAddPrimaryStorageButtonOnZonePage($("#add_primarystorage_button"), zoneId, zoneName);      
    
    $("#right_panel_content").find("#tab_details").click();     
}

function zoneJsonClearRightPanel() {
    zoneClearDetailsTab();       
}

function zoneJsonToDetailsTab() {	 
    var $leftmenuItem1 = $("#right_panel_content").data("$leftmenuItem1");
    if($leftmenuItem1 == null) {
        zoneClearDetailsTab();
        return;
    }
    
    var jsonObj = $leftmenuItem1.data("jsonObj");    
    if(jsonObj == null) { 
        zoneClearDetailsTab();
	    return;	
	}
       
    $.ajax({
        data: createURL("command=listZones&available=true&id="+jsonObj.id),
        dataType: "json",
        async: false,
        success: function(json) {            
            var items = json.listzonesresponse.zone;			
			if(items != null && items.length > 0) {
                jsonObj = items[0];
                $leftmenuItem1.data("jsonObj", jsonObj);                  
            }
        }
    });     
    
    var $thisTab = $("#right_panel_content").find("#tab_content_details");  
    if(jsonObj.domain == null) {
        $thisTab.find("#ispublic").text(g_dictionary["label.yes"]); 
        $thisTab.find("#ispublic_edit").val("true");             
        $readonlyFields  = $("#tab_content_details").find("#name, #dns1, #dns2, #internaldns1, #internaldns2, #vlan, #guestcidraddress");
        $editFields = $("#tab_content_details").find("#name_edit, #dns1_edit, #dns2_edit, #internaldns1_edit, #internaldns2_edit, #startvlan_edit, #endvlan_edit, #guestcidraddress_edit");    
    }
    else {
        $thisTab.find("#ispublic").text(g_dictionary["label.no"]);
        $thisTab.find("#ispublic_edit").val("false");
        $readonlyFields  = $("#tab_content_details").find("#name, #dns1, #dns2, #internaldns1, #internaldns2, #vlan, #guestcidraddress, #ispublic");
        $editFields = $("#tab_content_details").find("#name_edit, #dns1_edit, #dns2_edit, #internaldns1_edit, #internaldns2_edit, #startvlan_edit, #endvlan_edit, #guestcidraddress_edit, #ispublic_edit");    
    }
    
    if(jsonObj.networktype == "Basic") 
        $("#tab_network, #tab_content_details #vlan_container, #guestcidraddress_container").hide();    
    else if(jsonObj.networktype == "Advanced") 
        $("#tab_network, #tab_content_details #vlan_container, #guestcidraddress_container").show();            
  
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();                  
    $thisTab.find("#id").text(fromdb(jsonObj.id));
    $thisTab.find("#grid_header_title").text(fromdb(jsonObj.name));    
    $thisTab.find("#name").text(fromdb(jsonObj.name));
    $thisTab.find("#name_edit").val(fromdb(jsonObj.name));    
    $thisTab.find("#dns1").text(fromdb(jsonObj.dns1));
    $thisTab.find("#dns1_edit").val(fromdb(jsonObj.dns1));    
    $thisTab.find("#dns2").text(fromdb(jsonObj.dns2));
    $thisTab.find("#dns2_edit").val(fromdb(jsonObj.dns2));    
    $thisTab.find("#internaldns1").text(fromdb(jsonObj.internaldns1));
    $thisTab.find("#internaldns1_edit").val(fromdb(jsonObj.internaldns1));    
    $thisTab.find("#internaldns2").text(fromdb(jsonObj.internaldns2));
    $thisTab.find("#internaldns2_edit").val(fromdb(jsonObj.internaldns2));    
    $thisTab.find("#networktype").text(fromdb(jsonObj.networktype));  
    setBooleanReadField(jsonObj.securitygroupsenabled, $thisTab.find("#securitygroupsenabled"));	            
    $thisTab.find("#guestcidraddress").text(fromdb(jsonObj.guestcidraddress));   
    $thisTab.find("#guestcidraddress_edit").val(fromdb(jsonObj.guestcidraddress));           
    $thisTab.find("#domain").text(fromdb(jsonObj.domain));     
    $thisTab.find("#allocationstate").text(fromdb(jsonObj.allocationstate));
    if(jsonObj.networktype == "Advanced") {        
        var vlan = jsonObj.vlan; 
        $thisTab.find("#vlan").text(fromdb(vlan));      
        if(vlan != null) {           
		    if(vlan.indexOf("-") != -1) {  //e.g. vlan == "30-33"
			    var startVlan = vlan.substring(0, vlan.indexOf("-"));
			    var endVlan = vlan.substring((vlan.indexOf("-")+1));	
			    $thisTab.find("#startvlan_edit").val(startVlan);
			    $thisTab.find("#endvlan_edit").val(endVlan);			
		    }
		    else {  //e.g. vlan == "30"
		        $thisTab.find("#startvlan_edit").val(vlan);					        
		    }
	    } 
    }	
        
    //actions ***     
    zoneBuildActionMenu(jsonObj);  
    
    $thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();   
}	  

function zoneBuildActionMenu(jsonObj) {
	var $leftmenuItem1 = $("#right_panel_content").data("$leftmenuItem1");	
	var $thisTab = $("#right_panel_content").find("#tab_content_details");      

    var $actionLink = $thisTab.find("#action_link"); 
    bindActionLink($actionLink);
       
    var $actionMenu = $thisTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();      
    buildActionLinkForTab("label.action.edit.zone", zoneActionMap, $actionMenu, $leftmenuItem1, $thisTab);   
    
    if(jsonObj.allocationstate == "Disabled")
        buildActionLinkForTab("label.action.enable.zone", zoneActionMap, $actionMenu, $leftmenuItem1, $thisTab); 
    else if(jsonObj.allocationstate == "Enabled")  
        buildActionLinkForTab("label.action.disable.zone", zoneActionMap, $actionMenu, $leftmenuItem1, $thisTab); 
    
    buildActionLinkForTab("label.action.delete.zone", zoneActionMap, $actionMenu, $leftmenuItem1, $thisTab);          
}

function zoneClearDetailsTab() {	    
    var $thisTab = $("#right_panel_content").find("#tab_content_details");    
    $thisTab.find("#grid_header_title").text("");         
    $thisTab.find("#id").text("");
    
    $thisTab.find("#name").text("");
    $thisTab.find("#name_edit").val("");
    
    $thisTab.find("#dns1").text("");
    $thisTab.find("#dns1_edit").val("");
    
    $thisTab.find("#dns2").text("");
    $thisTab.find("#dns2_edit").val("");
    
    $thisTab.find("#internaldns1").text("");
    $thisTab.find("#internaldns1_edit").val("");
    
    $thisTab.find("#internaldns2").text("");
    $thisTab.find("#internaldns2_edit").val("");
    
    $thisTab.find("#networktype").text("");	
    
    $thisTab.find("#vlan").text("");
    $thisTab.find("#startvlan_edit").val("");
	$thisTab.find("#endvlan_edit").val("");	
    
    $thisTab.find("#guestcidraddress").text("");   
    $thisTab.find("#guestcidraddress_edit").val("");  
    
    $thisTab.find("#domain").text(""); 
    
    //actions ***   
    var $actionMenu = $thisTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();   
	$actionMenu.find("#action_list").append($("#no_available_actions").clone().show());		
}	

function bindAddPodButton($button, $leftmenuItem1) {       
    $button.unbind("click").bind("click", function(event) {   
        var zoneObj = $leftmenuItem1.data("jsonObj"); 
        
        var $dialogAddPod = $("#dialog_add_pod");
        
        if(zoneObj.networktype == "Basic") { //basic-mode network (pod-wide VLAN)
            $dialogAddPod.find("#guestip_container, #guestnetmask_container, #guestgateway_container").show();
        }
        else if(zoneObj.networktype == "Advanced") { //advanced-mode network (zone-wide VLAN)
            $dialogAddPod.find("#guestip_container, #guestnetmask_container, #guestgateway_container").hide();     
        }       
                
        $dialogAddPod.find("#info_container").hide();				  	
        $dialogAddPod.find("#add_pod_zone_name").text(fromdb(zoneObj.name));
        //$dialogAddPod.find("#add_pod_name, #add_pod_netmask, #add_pod_startip, #add_pod_endip, #add_pod_gateway").val("");
        
        $dialogAddPod
        .dialog('option', 'buttons', { 				
	        "Add": function() {		
	            var $thisDialog = $(this);
				$thisDialog.find("#info_container").hide();  
						
		        // validate values
		        var isValid = true;					
		        isValid &= validateString("Name", $thisDialog.find("#add_pod_name"), $thisDialog.find("#add_pod_name_errormsg"));
		        isValid &= validateNetmask("Netmask", $thisDialog.find("#add_pod_netmask"), $thisDialog.find("#add_pod_netmask_errormsg"));	
		        isValid &= validateIp("Start IP Range", $thisDialog.find("#add_pod_startip"), $thisDialog.find("#add_pod_startip_errormsg"));  //required
		        isValid &= validateIp("End IP Range", $thisDialog.find("#add_pod_endip"), $thisDialog.find("#add_pod_endip_errormsg"), true);  //optional
		        isValid &= validateIp("Gateway", $thisDialog.find("#add_pod_gateway"), $thisDialog.find("#add_pod_gateway_errormsg"));  //required when creating
		        		        
		        if($thisDialog.find("#guestip_container").css("display") != "none") {
                    isValid &= validateIp("Guest IP Range", $thisDialog.find("#startguestip"), $thisDialog.find("#startguestip_errormsg"));  //required
					isValid &= validateIp("Guest IP Range", $thisDialog.find("#endguestip"), $thisDialog.find("#endguestip_errormsg"), true);  //optional
					isValid &= validateNetmask("Guest Netmask", $thisDialog.find("#guestnetmask"), $thisDialog.find("#guestnetmask_errormsg"));  //required when creating
					isValid &= validateIp("Guest Gateway", $thisDialog.find("#guestgateway"), $thisDialog.find("#guestgateway_errormsg")); 
				}
		        		        
		        if (!isValid) 
		            return;			
                
                $thisDialog.find("#spinning_wheel").show()
                  
                var name = trim($thisDialog.find("#add_pod_name").val());
		        var netmask = trim($thisDialog.find("#add_pod_netmask").val());
		        var startip = trim($thisDialog.find("#add_pod_startip").val());
		        var endip = trim($thisDialog.find("#add_pod_endip").val());	    //optional
		        var gateway = trim($thisDialog.find("#add_pod_gateway").val());			

                var array1 = [];
                array1.push("&zoneId="+zoneObj.id);
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
		                var $zoneNode = $("#leftmenu_zone_tree").find("#tree_container").find("#zone_" + zoneObj.id);			                		                		                
				        if($zoneNode.find("#zone_arrow").hasClass("expanded_open")) {
				            var template = $("#leftmenu_pod_node_template").clone(true);
		                    podJSONToTreeNode(item, template);	     
				            $zoneNode.find("#pods_container").prepend(template.fadeIn("slow"));		
				        }
				        else {	
		                    $zoneNode.find("#zone_arrow").click();  //expand zone node to show the newly added pod
		                }			               
		                    			                                    
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
		                    array1.push("&zoneid=" + zoneObj.id);
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

var zoneActionMap = {
    "label.action.edit.zone": {
        dialogBeforeActionFn: doEditZone  
    },  
    "label.action.enable.zone": {                  
        isAsyncJob: false,       
        dialogBeforeActionFn : doEnableZone,  
        inProcessText: "label.action.enable.zone.processing",
        afterActionSeccessFn: function(json, $leftmenuItem1, id) {   
    		var jsonObj = json.updatezoneresponse.zone;
    		$("#right_panel_content").find("#tab_content_details").find("#allocationstate").text(fromdb(jsonObj.allocationstate));
    		zoneBuildActionMenu(jsonObj);        
        }
    },
    "label.action.disable.zone": {                  
        isAsyncJob: false,       
        dialogBeforeActionFn : doDisableZone,  
        inProcessText: "label.action.disable.zone.processing",
        afterActionSeccessFn: function(json, $leftmenuItem1, id) {   
	    	var jsonObj = json.updatezoneresponse.zone;
	    	$("#right_panel_content").find("#tab_content_details").find("#allocationstate").text(fromdb(jsonObj.allocationstate));
	    	zoneBuildActionMenu(jsonObj);    
        }
    },   
    "label.action.delete.zone": {                  
        isAsyncJob: false,       
        dialogBeforeActionFn : doDeleteZone,  
        inProcessText: "label.action.delete.zone.processing",
        afterActionSeccessFn: function(json, $leftmenuItem1, id) {   
            $leftmenuItem1.remove();                                         
            if(id.toString() == $("#right_panel_content").find("#tab_content_details").find("#id").text()) {
                clearRightPanel();
                zoneJsonClearRightPanel();
            }          
        }
    }
}

function doEditZone($actionLink, $detailsTab, $leftmenuItem1) {       
    $readonlyFields.hide();
    $editFields.show();  
    $detailsTab.find("#cancel_button, #save_button").show();
    
    $detailsTab.find("#cancel_button").unbind("click").bind("click", function(event){    
        cancelEditMode($detailsTab);        
        return false;
    });
    $detailsTab.find("#save_button").unbind("click").bind("click", function(event){        
        doEditZone2($actionLink, $detailsTab, $leftmenuItem1, $readonlyFields, $editFields);   
        return false;
    });   
}

function doEditZone2($actionLink, $detailsTab, $leftmenuItem1, $readonlyFields, $editFields) {    
    // validate values
	var isValid = true;			
	isValid &= validateString("Name", $detailsTab.find("#name_edit"), $detailsTab.find("#name_edit_errormsg"));
	isValid &= validateIp("DNS 1", $detailsTab.find("#dns1_edit"), $detailsTab.find("#dns1_edit_errormsg"), false);	//required
	isValid &= validateIp("DNS 2", $detailsTab.find("#dns2_edit"), $detailsTab.find("#dns2_edit_errormsg"), true);	//optional	
	isValid &= validateIp("Internal DNS 1", $detailsTab.find("#internaldns1_edit"), $detailsTab.find("#internaldns1_edit_errormsg"), false);	//required
	isValid &= validateIp("Internal DNS 2", $detailsTab.find("#internaldns2_edit"), $detailsTab.find("#internaldns2_edit_errormsg"), true);	//optional						
	if ($("#tab_content_details #vlan_container").css("display") != "none") {
		isValid &= validateString("Start VLAN Range", $detailsTab.find("#startvlan_edit"), $detailsTab.find("#startvlan_edit_errormsg"), true); //optional (Bug 5730 requested to change VLAN to be optional when updating zone)
		isValid &= validateString("End VLAN Range", $detailsTab.find("#endvlan_edit"), $detailsTab.find("#endvlan_edit_errormsg"), true);  //optional
	}
	if ($("#tab_content_details #guestcidraddress_container").css("display") != "none") {
	    isValid &= validateCIDR("Guest CIDR", $detailsTab.find("#guestcidraddress_edit"), $detailsTab.find("#guestcidraddress_edit_errormsg"), false);	//required
	}					
	if (!isValid) 
	    return;							
	
	var moreCriteria = [];	
	
	var jsonObj = $leftmenuItem1.data("jsonObj"); 
	
	var oldDns1 = jsonObj.dns1;
	var oldDns2 = jsonObj.dns2;	
	
	var name = $detailsTab.find("#name_edit").val();
	if(name != jsonObj.name)
	    moreCriteria.push("&name="+todb(name));
	
	var dns1 = $detailsTab.find("#dns1_edit").val();
	if(dns1 != jsonObj.dns1)
	    moreCriteria.push("&dns1="+todb(dns1));
	
	var dns2 = $detailsTab.find("#dns2_edit").val();
	if (dns2 != jsonObj.dns2) //dns2 can be an empty string
		moreCriteria.push("&dns2="+todb(dns2));	
	
	var internaldns1 = $detailsTab.find("#internaldns1_edit").val();
	if(internaldns1 != jsonObj.internaldns1)
	    moreCriteria.push("&internaldns1="+todb(internaldns1));
	
	var internaldns2 = $detailsTab.find("#internaldns2_edit").val();	
	if (internaldns2 != jsonObj.internaldns2) //internaldns2 can be an empty string
		moreCriteria.push("&internaldns2="+todb(internaldns2));						
	
	var vlan;				
	if ($("#tab_content_details #vlan_container").css("display") != "none") {
		var vlanStart = $detailsTab.find("#startvlan_edit").val();	
		if(vlanStart != null && vlanStart.length > 0) {
		    var vlanEnd = $detailsTab.find("#endvlan_edit").val();						
		    if (vlanEnd != null && vlanEnd.length > 0) 
		        vlan = vlanStart + "-" + vlanEnd;						    							
		    else 	
		        vlan = vlanStart;							
                      
            if(vlan != jsonObj.vlan)
               moreCriteria.push("&vlan=" + todb(vlan));	
        }
	}				
	
	if ($("#tab_content_details #guestcidraddress_container").css("display") != "none") {
	    var guestcidraddress = $detailsTab.find("#guestcidraddress_edit").val();
	    if(guestcidraddress != jsonObj.guestcidraddress)
	        moreCriteria.push("&guestcidraddress="+todb(guestcidraddress));				    		 
	}
	 
	if($("#ispublic_edit").css("display") != "none") {
	    var ispublic = $detailsTab.find("#ispublic_edit").val();
	    moreCriteria.push("&ispublic="+todb(ispublic));				     
	} 
	 
	if(moreCriteria.length > 0) { 	        	
	    $.ajax({
	      data: createURL("command=updateZone&id="+jsonObj.id+moreCriteria.join("")),
		    dataType: "json",
		    success: function(json) {		   
		        var item = json.updatezoneresponse.zone;		  
		        $leftmenuItem1.data("jsonObj", item);
		        $leftmenuItem1.find("#zone_name").text(item.name);
		        zoneJsonToRightPanel($leftmenuItem1);	
    		    
		        $editFields.hide();      
                $readonlyFields.show();       
                $("#save_button, #cancel_button").hide();  
                           
                if(item.dns1 != oldDns1 || item.dns2 != oldDns2) {
                    $("#dialog_info")
                    .text("DNS update will not take effect until all virtual routers and system vms are stopped and then started")
                    .dialog("open"); 
                }               	    
		    }
	    }); 
	}  
	else {
	    $editFields.hide();      
        $readonlyFields.show();       
        $("#save_button, #cancel_button").hide();  
	}
}

function doEnableZone($actionLink, $detailsTab, $midmenuItem1) {       
    var jsonObj = $midmenuItem1.data("jsonObj");
	var id = jsonObj.id;
		
	$("#dialog_confirmation")
	.text(dictionary["message.action.enable.zone"])
	.dialog('option', 'buttons', { 					
		"Confirm": function() { 			
			$(this).dialog("close");			
			var apiCommand = "command=updateZone&id="+id+"&allocationstate=Enabled";
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		}
	}).dialog("open");
}

function doDisableZone($actionLink, $detailsTab, $midmenuItem1) {       
    var jsonObj = $midmenuItem1.data("jsonObj");
	var id = jsonObj.id;
		
	$("#dialog_confirmation")
	.text(dictionary["message.action.disable.zone"])
	.dialog('option', 'buttons', { 					
		"Confirm": function() { 			
			$(this).dialog("close");			
			var apiCommand = "command=updateZone&id="+id+"&allocationstate=Disabled";
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		}
	}).dialog("open");
}

function doDeleteZone($actionLink, $detailsTab, $midmenuItem1) {       
    var jsonObj = $midmenuItem1.data("jsonObj");
	var id = jsonObj.id;
		
	$("#dialog_confirmation")
	.text(dictionary["message.action.delete.zone"])
	.dialog('option', 'buttons', { 					
		"Confirm": function() { 			
			$(this).dialog("close");			
			var apiCommand = "command=deleteZone&id="+id;
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		}
	}).dialog("open");
}

function bindAddClusterButtonOnZonePage($button, zoneId, zoneName) {    
    $button.unbind("click").bind("click", function(event) {
        $dialogAddCluster = $("#dialog_add_external_cluster_in_zone_page");      
        $dialogAddCluster.find("#info_container").hide();          
        $dialogAddCluster.find("#zone_name").text(zoneName);
         
        var $podSelect = $dialogAddCluster.find("#pod_dropdown");    	
        $.ajax({
            data: createURL("command=listPods&zoneid="+zoneId),
            dataType: "json",
            async: false,
            success: function(json) {            
                var pods = json.listpodsresponse.pod;   
                $podSelect.empty(); 
                if(pods != null && pods.length > 0) {
                    for(var i=0; i<pods.length; i++)
                        $podSelect.append("<option value='" + pods[i].id + "'>" + fromdb(pods[i].name) + "</option>"); 	
                }  
                $podSelect.change();        
            }        
        });    
        
        $dialogAddCluster.find("#cluster_hypervisor").change(function() {
        	if($(this).val() == "VMware") {
        		$('li[input_group="vmware"]', $dialogAddCluster).show();
        		// $dialogAddCluster.find("#type_dropdown").change();
        		$('li[input_sub_group="external"]', $dialogAddCluster).show();
        		$("#cluster_name_label", $dialogAddCluster).text("vCenter Cluster:");
        	} else {
        		$('li[input_group="vmware"]', $dialogAddCluster).hide();
        		$("#cluster_name_label", $dialogAddCluster).text("Cluster:");
        	}
        }).change();

/*        
        $dialogAddCluster.find("#type_dropdown").change(function() {
        	if($(this).val() == "ExternalManaged") {
        		$('li[input_sub_group="external"]', $dialogAddCluster).show();
        		$("#cluster_name_label", $dialogAddCluster).text("vCenter Cluster:");
        	} else {
        		$('li[input_sub_group="external"]', $dialogAddCluster).hide();
        		$("#cluster_name_label", $dialogAddCluster).text("Cluster:");
        	}
        });
*/        
        $dialogAddCluster.dialog('option', 'buttons', { 				
	        "Add": function() { 
	            var $thisDialog = $(this);		            
	            $thisDialog.find("#info_container").hide();  
	            			   
		        // validate values
			    var hypervisor = $thisDialog.find("#cluster_hypervisor").val();
			    var clusterType="CloudManaged";
			    if(hypervisor == "VMware") {
			    	// clusterType = $thisDialog.find("#type_dropdown").val();
				    clusterType="ExternalManaged";
			    }
			    
		        var isValid = true;
		        isValid &= validateDropDownBox("Pod", $thisDialog.find("#pod_dropdown"), $thisDialog.find("#pod_dropdown_errormsg"));	
		        if(hypervisor == "VMware" && clusterType != "CloudManaged") {
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
		        array1.push("&zoneId="+zoneId);
		        
		        //expand zone in left menu tree (to show pod, cluster under the zone) 
				var $zoneNode = $("#leftmenu_zone_tree").find("#tree_container").find("#zone_" + zoneId);							
				if($zoneNode.find("#zone_arrow").hasClass("expanded_close"))
				    $zoneNode.find("#zone_arrow").click();
										             
		        var podId = $thisDialog.find("#pod_dropdown").val();
		        array1.push("&podId="+podId);

		        var clusterName = trim($thisDialog.find("#cluster_name").val());
		        if(hypervisor == "VMware" && clusterType != "CloudManaged") {
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


function bindAddHostButtonOnZonePage($button, zoneId, zoneName) {
    var $dialogAddHost = $("#dialog_add_host_in_zone_page");   
    $dialogAddHost.find("#zone_name").text(zoneName); 
    
    var $podSelect = $dialogAddHost.find("#pod_dropdown");     
    $.ajax({
        data: createURL("command=listPods&zoneid="+zoneId),
        dataType: "json",
        async: false,
        success: function(json) {            
            var pods = json.listpodsresponse.pod;   
            $podSelect.empty(); 
            if(pods != null && pods.length > 0) {
                for(var i=0; i<pods.length; i++)
                    $podSelect.append("<option value='" + pods[i].id + "'>" + fromdb(pods[i].name) + "</option>"); 	
            }                
        }        
    });                   
                 
    $dialogAddHost.find("#pod_dropdown").unbind("change").bind("change", function(event) {    	   
        $dialogAddHost.find("#cluster_select").change();       
    });  
        
    $dialogAddHost.find("#cluster_select").unbind("change").change(function() {        
        var clusterId = $(this).val();
        if(clusterId == null)
            return;        
        var clusterObj = clustersUnderOnePod[clusterId];                    
    	
        if(clusterObj.hypervisortype == "VMware") {
    		$('li[input_group="vmware"]', $dialogAddHost).show();
    		$('li[input_group="general"]', $dialogAddHost).hide();
			$('li[input_group="baremetal"]', $dialogAddHost).hide();
			$('li[input_group="Ovm"]', $dialogAddHost).hide();
    	} 
    	else if (clusterObj.hypervisortype == "BareMetal") {
    		$('li[input_group="baremetal"]', $dialogAddHost).show();
    		$('li[input_group="general"]', $dialogAddHost).show();
			$('li[input_group="vmware"]', $dialogAddHost).hide();
			$('li[input_group="Ovm"]', $dialogAddHost).hide();			
		} 
    	else if (clusterObj.hypervisortype == "Ovm") {
    		$('li[input_group="Ovm"]', $dialogAddHost).show();
    		$('li[input_group="general"]', $dialogAddHost).show();    		
			$('li[input_group="vmware"]', $dialogAddHost).hide();    		
			$('li[input_group="baremetal"]', $dialogAddHost).hide();			
		} 
    	else {    		
    		$('li[input_group="general"]', $dialogAddHost).show();
    		$('li[input_group="vmware"]', $dialogAddHost).hide();
			$('li[input_group="baremetal"]', $dialogAddHost).hide();
			$('li[input_group="Ovm"]', $dialogAddHost).hide();	
    	}               
    });
     
    $button.unbind("click").bind("click", function(event) {              
        $dialogAddHost.find("#info_container").hide();             
        //$dialogAddHost.find("#host_hypervisor").change();	    
	    refreshClsuterFieldInAddHostDialog($dialogAddHost, $dialogAddHost.find("#pod_dropdown").val(), null);
	    $dialogAddHost.find("#pod_dropdown").change();
	    
        $dialogAddHost
        .dialog('option', 'buttons', { 				
	        "Add": function() { 
	            var $thisDialog = $(this);		            
	    		$thisDialog.find("#info_container").hide();  
	    		
		        // validate values
		        var isValid = true;			       	
		        isValid &= validateDropDownBox("Pod", $thisDialog.find("#pod_dropdown"), $thisDialog.find("#pod_dropdown_errormsg"));	
		        isValid &= validateDropDownBox("Cluster", $thisDialog.find("#cluster_select"), $thisDialog.find("#cluster_select_errormsg"), false);  //required, reset error text					    				
		        
		        var clusterId = $thisDialog.find("#cluster_select").val();	
				var clusterObj, hypervisor;
				if(clusterId != null) {
				    clusterObj = clustersUnderOnePod[clusterId];    
                    hypervisor = clusterObj.hypervisortype;  		        
		            if(hypervisor == "VMware") {
/*		            	
			            isValid &= validateString("vCenter Address", $thisDialog.find("#host_vcenter_address"), $thisDialog.find("#host_vcenter_address_errormsg"));
			            isValid &= validateString("vCenter User", $thisDialog.find("#host_vcenter_username"), $thisDialog.find("#host_vcenter_username_errormsg"));
			            isValid &= validateString("vCenter Password", $thisDialog.find("#host_vcenter_password"), $thisDialog.find("#host_vcenter_password_errormsg"));	
			            isValid &= validateString("vCenter Datacenter", $thisDialog.find("#host_vcenter_dc"), $thisDialog.find("#host_vcenter_dc_errormsg"));	
*/	
			            isValid &= validateString("vCenter Host", $thisDialog.find("#host_vcenter_host"), $thisDialog.find("#host_vcenter_host_errormsg"));	
		            } 
		            else {						
						isValid &= validateString("Host name", $thisDialog.find("#host_hostname"), $thisDialog.find("#host_hostname_errormsg"));
						isValid &= validateString("User name", $thisDialog.find("#host_username"), $thisDialog.find("#host_username_errormsg"));
						isValid &= validateString("Password", $thisDialog.find("#host_password"), $thisDialog.find("#host_password_errormsg"));	
					
						if (hypervisor == "BareMetal") {
							isValid &= validateString("CPU Cores", $thisDialog.find("#host_baremetal_cpucores"), $thisDialog.find("#host_baremetal_cpucores_errormsg"));
							isValid &= validateString("CPU", $thisDialog.find("#host_baremetal_cpu"), $thisDialog.find("#host_baremetal_cpu_errormsg"));
							isValid &= validateString("Memory", $thisDialog.find("#host_baremetal_memory"), $thisDialog.find("#host_baremetal_memory_errormsg"));
							isValid &= validateString("MAC", $thisDialog.find("#host_baremetal_mac"), $thisDialog.find("#host_baremetal_mac_errormsg"));	
						} 
						else if(hypervisor == "Ovm") {
							isValid &= validateString("Agent Username", $thisDialog.find("#agent_username"), $thisDialog.find("#agent_username_errormsg"), true);  //optional
							isValid &= validateString("Agent Password", $thisDialog.find("#agent_password"), $thisDialog.find("#agent_password_errormsg"), false); //required	
						}
		            }
		        }	        
		        if (!isValid) 
		            return;		            			
					
				$thisDialog.find("#spinning_wheel").show() 				
				
		        var array1 = [];
				
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
				array1.push("&hosttags=" + todb(trim($thisDialog.find("#host_tags").val())));			    

			    if(hypervisor == "VMware") {
/*
			    	var username = trim($thisDialog.find("#host_vcenter_username").val());
			        array1.push("&username="+todb(username));
					
			        var password = trim($thisDialog.find("#host_vcenter_password").val());
			        array1.push("&password="+todb(password));
				    
			        var hostname = trim($thisDialog.find("#host_vcenter_address").val());
			        hostname += "/" + trim($thisDialog.find("#host_vcenter_dc").val());
			        hostname += "/" + trim($thisDialog.find("#host_vcenter_host").val());
*/
			        array1.push("&username=");
			        array1.push("&password=");
			        var hostname = trim($thisDialog.find("#host_vcenter_host").val());
			    	
			        var url;					
			        if(hostname.indexOf("http://")==-1)
			            url = "http://" + todb(hostname);
			        else
			            url = hostname;
			        array1.push("&url="+todb(url));
			    	
			    } 
			    else {
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
			        			        
			        if (hypervisor == "BareMetal") {
						var cpuCores = trim($thisDialog.find("#host_baremetal_cpucores").val());
						array1.push("&cpunumber="+todb(cpuCores));
						
						var cpuSpeed = trim($thisDialog.find("#host_baremetal_cpu").val());
						array1.push("&cpuspeed="+todb(cpuSpeed));
						
						var memory = trim($thisDialog.find("#host_baremetal_memory").val());
						array1.push("&memory="+todb(memory));
						
						var mac = trim($thisDialog.find("#host_baremetal_mac").val());
						array1.push("&hostmac="+todb(mac));
					}
			        else if(hypervisor == "Ovm") {
			        	var agentUsername = $thisDialog.find("#agent_username").val();
			        	array1.push("&agentusername="+todb(agentUsername)); 
							
					    var agentPassword = $thisDialog.find("#agent_password").val();
					    array1.push("&agentpassword="+todb(agentPassword));			        	
					}
			    }
							        
		        $.ajax({
			       data: createURL("command=addHost" + array1.join("")),
			        dataType: "json",
			        success: function(json) {			        
			            $thisDialog.find("#spinning_wheel").hide();
			            $thisDialog.dialog("close");		
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

function bindAddPrimaryStorageButtonOnZonePage($button, zoneId, zoneName) {
    var $dialogAddPool = $("#dialog_add_pool_in_zone_page");    
	$dialogAddPool.find("#zone_name").text(zoneName);  
	
    bindEventHandlerToDialogAddPool($dialogAddPool);	
    
    var $podSelect = $dialogAddPool.find("#pod_dropdown");    
    $.ajax({
        data: createURL("command=listPods&zoneid="+zoneId),
        dataType: "json",
        async: false,
        success: function(json) {            
            var pods = json.listpodsresponse.pod;   
            $podSelect.empty(); 
            if(pods != null && pods.length > 0) {
                for(var i=0; i<pods.length; i++)
                    $podSelect.append("<option value='" + pods[i].id + "'>" + fromdb(pods[i].name) + "</option>"); 	
            }              
        }        
    });          
    
    $podSelect.unbind("change").bind("change", function(event) {	        
        var podId = $(this).val();        
        populateClusterFieldInAddPoolDialog($dialogAddPool, podId, null);   
    });
          
    $button.unbind("click").bind("click", function(event) {         
        $dialogAddPool.find("#info_container").hide();	
        $podSelect.change();       
                   
        $("#dialog_add_pool_in_zone_page")
	    .dialog('option', 'buttons', { 				    
		    "Add": function() { 	
		    	var $thisDialog = $(this);
		    	$thisDialog.find("#info_container").hide();  
		    	
			    // validate values
				var protocol = $thisDialog.find("#add_pool_protocol").val();
				
			    var isValid = true;					    
		        isValid &= validateDropDownBox("Pod", $thisDialog.find("#pod_dropdown"), $thisDialog.find("#pod_dropdown_errormsg"));						    
			    isValid &= validateDropDownBox("Cluster", $thisDialog.find("#pool_cluster"), $thisDialog.find("#pool_cluster_errormsg"), false);  //required, reset error text					    				
			    isValid &= validateString("Name", $thisDialog.find("#add_pool_name"), $thisDialog.find("#add_pool_name_errormsg"));
				if (protocol == "nfs" || protocol == "PreSetup" || protocol == "SharedMountPoint") {
				    isValid &= validateString("Server", $thisDialog.find("#add_pool_nfs_server"), $thisDialog.find("#add_pool_nfs_server_errormsg"));	
					isValid &= validateString("Path", $thisDialog.find("#add_pool_path"), $thisDialog.find("#add_pool_path_errormsg"));	
				} else if(protocol == "ocfs2") {
					isValid &= validateString("Path", $thisDialog.find("#add_pool_path"), $thisDialog.find("#add_pool_path_errormsg"));			
				} else if(protocol == "iscsi") {
				    isValid &= validateString("Server", $thisDialog.find("#add_pool_nfs_server"), $thisDialog.find("#add_pool_nfs_server_errormsg"));	
					isValid &= validateString("Target IQN", $thisDialog.find("#add_pool_iqn"), $thisDialog.find("#add_pool_iqn_errormsg"));	
					isValid &= validateString("LUN #", $thisDialog.find("#add_pool_lun"), $thisDialog.find("#add_pool_lun_errormsg"));	
				} else if(protocol == "clvm") {
					isValid &= validateString("Volume Group", $thisDialog.find("#add_pool_clvm_vg"), $thisDialog.find("#add_pool_clvm_vg_errormsg"));
				} else if(protocol == "vmfs") {
					isValid &= validateString("vCenter Datacenter", $thisDialog.find("#add_pool_vmfs_dc"), $thisDialog.find("#add_pool_vmfs_dc_errormsg"));	
					isValid &= validateString("vCenter Datastore", $thisDialog.find("#add_pool_vmfs_ds"), $thisDialog.find("#add_pool_vmfs_ds_errormsg"));	
				}
				isValid &= validateString("Tags", $thisDialog.find("#add_pool_tags"), $thisDialog.find("#add_pool_tags_errormsg"), true);	//optional
			    if (!isValid) 
			        return;
			        			    
				$thisDialog.find("#spinning_wheel").show()  
							
				var array1 = [];
								
		        array1.push("&zoneid="+zoneId);
		        
		        //expand zone in left menu tree (to show pod, cluster under the zone) 
				var $zoneNode = $("#leftmenu_zone_tree").find("#tree_container").find("#zone_" + zoneId);							
				if($zoneNode.find("#zone_arrow").hasClass("expanded_close"))
				    $zoneNode.find("#zone_arrow").click();
				
				var podId = $thisDialog.find("#pod_dropdown").val();
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
				} 
				else if (protocol == "PreSetup") {
					var path = trim($thisDialog.find("#add_pool_path").val());
					if(path.substring(0,1)!="/")
						path = "/" + path; 
					url = presetupURL(server, path);
				} 
				else if (protocol == "ocfs2") {
					var path = trim($thisDialog.find("#add_pool_path").val());
					if(path.substring(0,1)!="/")
						path = "/" + path; 
					url = ocfs2URL(server, path);
				} 
				else if (protocol == "SharedMountPoint") {
					var path = trim($thisDialog.find("#add_pool_path").val());
					if(path.substring(0,1)!="/")
						path = "/" + path; 
					url = SharedMountPointURL(server, path);
				} 
				else if (protocol == "clvm") {
					var vg = trim($thisDialog.find("#add_pool_clvm_vg").val());
					url = clvmURL(vg);
				}
				else if (protocol == "vmfs") {
					var path = trim($thisDialog.find("#add_pool_vmfs_dc").val());
					if(path.substring(0,1)!="/")
						path = "/" + todb(path); 
					path += "/" + todb(trim($thisDialog.find("#add_pool_vmfs_ds").val()));
					url = vmfsURL("dummy", path);
				} 
				else {
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
