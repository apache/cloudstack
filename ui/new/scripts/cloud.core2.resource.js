function afterLoadResourceJSP() {
    var $rightPanelHeaderLabel = $("#right_panel_header").find("#label");

    var $rightPanelConent = $("#right_panel_content");
    var $zonePage = $rightPanelConent.find("#zone_page");
    var $podPage = $rightPanelConent.find("#pod_page");
    var $clusterPage = $rightPanelConent.find("#cluster_page");
    var $hostPage = $rightPanelConent.find("#host_page");
    var $primarystoragePage = $rightPanelConent.find("#primarystorage_page");
    var $systemvmPage = $rightPanelConent.find("#systemvm_page");
    
    var pageArray = [$zonePage, $podPage, $clusterPage, $hostPage, $primarystoragePage, $systemvmPage];
    var pageLabelArray = ["Zone", "Pod", "Cluster", "Host", "Primary Storage", "System VM"];
    
    function showPage($pageToShow, jsonObj) {        
        for(var i=0; i<pageArray.length; i++) {
            if(pageArray[i].attr("id") == $pageToShow.attr("id")) {
                $rightPanelHeaderLabel.text(pageLabelArray[i]);
                pageArray[i].show();
            }
            else {
                pageArray[i].hide();
            }
            $pageToShow.data("jsonObj", jsonObj);
        }   
        
        if($pageToShow.attr("id") == "zone_page") {
            //***** Add Pod (begin) *****
            $("#midmenu_add_link").find("#label").text("Add Pod");  
            $("#midmenu_add_link").data("jsonObj", jsonObj).show();   
            $("#midmenu_add_link").unbind("click").bind("click", function(event) {    
                var zoneObj = $(this).data("jsonObj");		
		        $("#dialog_add_pod").find("#add_pod_zone_name").text(fromdb(zoneObj.name));
		        $("#dialog_add_pod #add_pod_name, #dialog_add_pod #add_pod_cidr, #dialog_add_pod #add_pod_startip, #dialog_add_pod #add_pod_endip, #add_pod_gateway").val("");
        		
		        $("#dialog_add_pod")
		        .dialog('option', 'buttons', { 				
			        "Add": function() {		
			            var thisDialog = $(this);
        						
				        // validate values
				        var isValid = true;					
				        isValid &= validateString("Name", thisDialog.find("#add_pod_name"), thisDialog.find("#add_pod_name_errormsg"));
				        isValid &= validateCIDR("CIDR", thisDialog.find("#add_pod_cidr"), thisDialog.find("#add_pod_cidr_errormsg"));	
				        isValid &= validateIp("Start IP Range", thisDialog.find("#add_pod_startip"), thisDialog.find("#add_pod_startip_errormsg"));  //required
				        isValid &= validateIp("End IP Range", thisDialog.find("#add_pod_endip"), thisDialog.find("#add_pod_endip_errormsg"), true);  //optional
				        isValid &= validateIp("Gateway", thisDialog.find("#add_pod_gateway"), thisDialog.find("#add_pod_gateway_errormsg"));  //required when creating
				        if (!isValid) 
				            return;			
                        
                        thisDialog.dialog("close"); 
                          
                        var name = trim(thisDialog.find("#add_pod_name").val());
				        var cidr = trim(thisDialog.find("#add_pod_cidr").val());
				        var startip = trim(thisDialog.find("#add_pod_startip").val());
				        var endip = trim(thisDialog.find("#add_pod_endip").val());	    //optional
				        var gateway = trim(thisDialog.find("#add_pod_gateway").val());			

                        var array1 = [];
                        array1.push("&zoneId="+zoneObj.id);
                        array1.push("&name="+todb(name));
                        array1.push("&cidr="+encodeURIComponent(cidr));
                        array1.push("&startIp="+encodeURIComponent(startip));
                        if (endip != null && endip.length > 0)
                            array1.push("&endIp="+encodeURIComponent(endip));
                        array1.push("&gateway="+encodeURIComponent(gateway));			
        								    
				        var template = $("#pod_template").clone(true);
				        var loadingImg = template.find(".adding_loading");										
				        var row_container = template.find("#row_container");        				
				        $("#zone_" + zoneObj.id + " #zone_content").show();	
				        $("#zone_" + zoneObj.id + " #pods_container").prepend(template.show());						
				        $("#zone_" + zoneObj.id + " #zone_expand").removeClass().addClass("zonetree_openarrows");									            
	                    loadingImg.show();  
                        row_container.hide();             
		                template.fadeIn("slow");
        				
				        $.ajax({
				          data: createURL("command=createPod&response=json"+array1.join("")),
					        dataType: "json",
					        success: function(json) {
						        var item = json.createpodresponse; 	
					            podJSONToTreeNode(item, template);	
						        loadingImg.hide(); 								                            
                                row_container.show();
                                
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
					        },
				            error: function(XMLHttpResponse) {	
				                handleError(XMLHttpResponse);			    
					            template.slideUp("slow", function() {
							        $(this).remove();
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
            //***** Add Pod (end) *****
            
            $("#midmenu_add2_link").unbind("click").hide();   
        }
        else if($pageToShow.attr("id") == "pod_page") {
            //***** Add Host (begin) *****
            $("#midmenu_add_link").find("#label").text("Add Host");  
            $("#midmenu_add_link").data("jsonObj", jsonObj).show(); 
            $("#midmenu_add_link").unbind("click").bind("click", function(event) {
            
                return false;
            });
            
            //***** Add Host (end) *****
             
            //***** Add Primary Storage (begin) *****          
            $("#midmenu_add2_link").find("#label").text("Add Primary Storage"); 
            $("#midmenu_add2_link").data("jsonObj", jsonObj).show();   
            $("#midmenu_add2_link").unbind("click").bind("click", function(event) {
            
                return false;
            });     
            
            //***** Add Primary Storage (end) *****           
        }      
        else {
            $("#midmenu_add_link").unbind("click").hide();              
            $("#midmenu_add2_link").unbind("click").hide();   
        }
    }
   
    //***** build zone tree (begin) ***********************************************************************************************
    var forceLogout = true;  // We force a logout only if the user has first added a POD for the very first time 
    var $zoneetree1 = $("#zonetree").clone().attr("id", "zonetree1");  
    $("#midmenu_container").append($zoneetree1.show());
    
    $.ajax({
	    data: createURL("command=listZones&available=true"+maxPageSize),
		dataType: "json",
		success: function(json) {
			var items = json.listzonesresponse.zone;
			var container = $("#zonetree1").find("#zones_container").empty();
			if (items != null && items.length > 0) {					    
				for (var i = 0; i < items.length; i++) {
					var template = $("#zone_template").clone(true);
					zoneJSONToTreeNode(items[i], template);
					container.append(template.show());
				}
			}
		}
	});  

    function zoneJSONToTreeNode(json, template) {
        var zoneid = json.id;
        template.attr("id", "zone_" + zoneid);  
	    template.data("id", zoneid).data("name", fromdb(json.name));
	    var zoneName = template.find("#zone_name").text(fromdb(json.name));	    
	    zoneName.data("jsonObj", json);	    
    	
	    $.ajax({
	        data: createURL("command=listPods&zoneid="+zoneid+maxPageSize),
		    dataType: "json",
		    success: function(json) {
			    var items = json.listpodsresponse.pod;
			    var container = template.find("#pods_container").empty();
			    if (items != null && items.length > 0) {					    
				    for (var i = 0; i < items.length; i++) {
					    var podTemplate = $("#pod_template").clone(true);
					    podJSONToTreeNode(items[i], podTemplate);
					    container.append(podTemplate.show());
					    forceLogout = false;  // We don't force a logout if pod(s) exit.
				    }
			    }
		    }
	    });
	    	    
	    $.ajax({
	        data: createURL("command=listSystemVms&zoneid="+zoneid+maxPageSize),
		    dataType: "json",
		    success: function(json) {
			    var items = json.listsystemvmsresponse.systemvm;
			    var container = template.find("#systemvms_container").empty();
			    if (items != null && items.length > 0) {					    
				    for (var i = 0; i < items.length; i++) {
					    var systemvmTemplate = $("#systemvm_template").clone(true);
					    systemvmJSONToTreeNode(items[i], systemvmTemplate);
					    container.append(systemvmTemplate.show());
				    }
			    }
		    }
	    });
    }
    
    function podJSONToTreeNode(json, template) {	
        var podid = json.id;
        template.attr("id", "pod_" + podid);  
    	    
		var ipRange = getIpRange(json.startip, json.endip);			
		template.data("id", podid).data("name", fromdb(json.name));
		
		var podName = template.find("#pod_name").text(fromdb(json.name));
		podName.data("jsonObj", json);	    
			
	    $.ajax({
            data: createURL("command=listClusters&podid="+podid+maxPageSize),
	        dataType: "json",
	        success: function(json) {
		        var items = json.listclustersresponse.cluster;
		        var container = template.find("#clusters_container").empty();
		        if (items != null && items.length > 0) {					    
			        for (var i = 0; i < items.length; i++) {
				        var clusterTemplate = $("#cluster_template").clone(true);
				        clusterJSONToTreeNode(items[i], clusterTemplate);
				        container.append(clusterTemplate.show());
			        }
		        }
	        }
        });		
	}
		
	function systemvmJSONToTreeNode(json, template) {	
	    var systemvmid = json.id;	
	    template.attr("id", "systemvm_"+systemvmid);
	    template.data("id", systemvmid).data("name", json.name);	     
	    var systeymvmName = template.find("#systemvm_name").text(json.name);	    
	    systeymvmName.data("jsonObj", json);	    		
	}
			
	function clusterJSONToTreeNode(json, template) {
	    template.attr("id", "cluster_"+json.id);
	    template.data("id", json.id).data("name", fromdb(json.name));	    
	    var clusterName = template.find("#cluster_name").text(fromdb(json.name));
	    clusterName.data("jsonObj", json);	   
	    	   
	    $.ajax({
            data: createURL("command=listHosts&clusterid="+json.id+maxPageSize),
	        dataType: "json",
	        success: function(json) {
		        var items = json.listhostsresponse.host;
		        var container = template.find("#hosts_container").empty();
		        if (items != null && items.length > 0) {					    
			        for (var i = 0; i < items.length; i++) {
				        var hostTemplate = $("#host_template").clone(true);
				        hostJSONToTreeNode(items[i], hostTemplate);
				        container.append(hostTemplate.show());
			        }
		        }
	        }
        });		
        
        $.ajax({
            data: createURL("command=listStoragePools&clusterid="+json.id+maxPageSize),
	        dataType: "json",
	        success: function(json) {
		        var items = json.liststoragepoolsresponse.storagepool;
		        var container = template.find("#primarystorages_container").empty();
		        if (items != null && items.length > 0) {					    
			        for (var i = 0; i < items.length; i++) {
				        var primaryStorageTemplate = $("#primarystorage_template").clone(true);
				        primaryStorageJSONToTreeNode(items[i], primaryStorageTemplate);
				        container.append(primaryStorageTemplate.show());
			        }
		        }
	        }
        });		    
	}
	
	function hostJSONToTreeNode(json, template) {
	    template.attr("id", "host_"+json.id);
	    template.data("id", json.id).data("name", fromdb(json.name));	    
	    var hostName = template.find("#host_name").text(fromdb(json.name));
	    hostName.data("jsonObj", json);
	}
	
	function primaryStorageJSONToTreeNode(json, template) {
	    template.attr("id", "primary_storage_"+json.id);
	    template.data("id", json.id).data("name", fromdb(json.name));	    
	    var primaryStorageName = template.find("#primarystorage_name").text(fromdb(json.name));
	    primaryStorageName.data("jsonObj", json);
	}
	
	$("#zone_template").bind("click", function(event) {
		var template = $(this);
		var target = $(event.target);
		var action = target.attr("id");
		var id = template.data("id");
		var name = template.data("name");
		
		switch (action) {
			case "zone_expand" :			   
				if (target.hasClass("zonetree_closedarrows")) {						
					target.removeClass().addClass("zonetree_openarrows");					
					target.parent().parent().parent().find("#zone_content").show();	
				} else {					
					target.removeClass().addClass("zonetree_closedarrows");					
					target.parent().parent().parent().find("#zone_content").hide();									
				}
				break;	
			case "zone_name":	
			    $zoneetree1.find(".selected").removeClass("selected");
			    target.parent().parent().parent().addClass("selected");				    
			    var jsonObj = target.data("jsonObj");  
			    showPage($zonePage, jsonObj);
			    zoneJsonToDetailsTab(jsonObj);
			    zoneJsonToNetworkTab(jsonObj);							    		   			    
			    break;
			
			
			case "pod_expand" :				    	   
				if (target.hasClass("zonetree_closedarrows")) {									
					target.removeClass().addClass("zonetree_openarrows");
					target.parent().parent().siblings("#pod_content").show();	
				} else {					
					target.removeClass().addClass("zonetree_closedarrows");
					target.parent().parent().siblings("#pod_content").hide();
				}
				break;	
			case "pod_name" :			   
				$zoneetree1.find(".selected").removeClass("selected");
				target.parent().parent().parent().addClass("selected");
			    var jsonObj = target.data("jsonObj");
			    showPage($podPage, jsonObj);		
			    podJsonToDetailsTab(jsonObj);				
				break;
				
			
			case "cluster_expand" :			   
				if (target.hasClass("zonetree_closedarrows")) {
				    target.removeClass().addClass("zonetree_openarrows");
					target.parent().parent().siblings("#cluster_content").show();					
					
				} else {
				    target.removeClass().addClass("zonetree_closedarrows");
					target.parent().parent().siblings("#cluster_content").hide();					
				}
				break;		
			case "cluster_name" :			   
				$zoneetree1.find(".selected").removeClass("selected");
			    target.parent().parent().parent().addClass("selected");			    
			    var jsonObj = target.data("jsonObj");
			    showPage($clusterPage, jsonObj);
			    clusterJsonToDetailsTab(jsonObj);					
				break;	
				
				
			case "host_expand" :			   
				if (target.hasClass("zonetree_closedarrows")) {
				    target.removeClass().addClass("zonetree_openarrows");
					target.parent().parent().siblings("#host_content").show();					
					
				} else {
				    target.removeClass().addClass("zonetree_closedarrows");
					target.parent().parent().siblings("#host_content").hide();					
				}
				break;	
			case "host_name" :			   
				$zoneetree1.find(".selected").removeClass("selected");
			    target.parent().parent().parent().addClass("selected");			    
				var jsonObj = target.data("jsonObj");
				showPage($hostPage, jsonObj);
				hostJsonToDetailsTab(jsonObj);				
				break;	
			
			
			case "primarystorage_expand" :			   
				if (target.hasClass("zonetree_closedarrows")) {
				    target.removeClass().addClass("zonetree_openarrows");
					target.parent().parent().siblings("#primarystorage_content").show();					
					
				} else {
				    target.removeClass().addClass("zonetree_closedarrows");
					target.parent().parent().siblings("#primarystorage_content").hide();					
				}
				break;	
			case "primarystorage_name" :			   
				$zoneetree1.find(".selected").removeClass("selected");
			    target.parent().parent().parent().addClass("selected");			    
			    var jsonObj = target.data("jsonObj");
			    showPage($primarystoragePage, jsonObj);
				primarystorageJsonToDetailsTab(jsonObj);					
				break;
						
						
			case "systemvm_name" :			   
				$zoneetree1.find(".selected").removeClass("selected");			    		    
			    target.parent().parent().parent().addClass("selected");	
			    var jsonObj = target.data("jsonObj");	
			    showPage($systemvmPage, jsonObj);					
				systemvmJsonToDetailsTab(jsonObj);			
				break;
			
			
			default:
				break;
		}
		return false;
	});
    
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
	//***** build zone tree (end) *************************************************************************************************
	
	//***** zone page (begin) *****************************************************************************************************
	//switch between different tabs in zone page 
    var tabArray = [$zonePage.find("#tab_details"), $zonePage.find("#tab_network"), $zonePage.find("#tab_secondary_storage")];
    var tabContentArray = [$zonePage.find("#tab_content_details"), $zonePage.find("#tab_content_network"), $zonePage.find("#tab_content_secondary_storage")];
    switchBetweenDifferentTabs(tabArray, tabContentArray);       
  
    function zoneJsonToDetailsTab(jsonObj) {	    
	    var $detailsTab = $zonePage.find("#tab_content_details");   
        $detailsTab.data("jsonObj", jsonObj);           
        $detailsTab.find("#id").text(fromdb(jsonObj.id));
        $detailsTab.find("#name").text(fromdb(jsonObj.name));
        $detailsTab.find("#dns1").text(fromdb(jsonObj.dns1));
        $detailsTab.find("#dns2").text(fromdb(jsonObj.dns2));
        $detailsTab.find("#internaldns1").text(fromdb(jsonObj.internaldns1));
        $detailsTab.find("#internaldns2").text(fromdb(jsonObj.internaldns2));	
        $detailsTab.find("#vlan").text(fromdb(jsonObj.vlan));
        $detailsTab.find("#guestcidraddress").text(fromdb(jsonObj.guestcidraddress));     
	}	  
	
	function zoneJsonToNetworkTab(jsonObj) {	    
	    var $networkTab = $zonePage.find("#tab_content_network");  
	    $networkTab.find("#zone_cloud").find("#zone_name").text(fromdb(jsonObj.name));	 
	    $networkTab.find("#zone_vlan").text(jsonObj.vlan);   
                      
        $.ajax({
		  data: createURL("command=listVlanIpRanges&zoneId="+jsonObj.id),
			dataType: "json",
			success: function(json) {
				var items = json.listvlaniprangesresponse.vlaniprange;		
				var $vlanContainer = $networkTab.find("#vlan_container").empty();   					
				if (items != null && items.length > 0) {					    
					for (var i = 0; i < items.length; i++) {	
					    var item = items[i];
					    					   
					    var $template1;
					    if(item.forvirtualnetwork == "false") 
					        $template1 = $("#direct_vlan_template").clone(); 
					    else
					    	$template1 = $("#virtual_vlan_template").clone();  					    
					    
					    vlanjsontotemplate(item, $template1);
					    $vlanContainer.append($template1.show());											
					}
				}
			}
		});
	}	 
	
	function vlanjsontotemplate(jsonObj, $template1) {
	    $template1.data("jsonObj", jsonObj);
	    $template1.find("#vlan_id").text(jsonObj.vlan);
	    $template1.find("#ip_range").text(jsonObj.description);
	} 	
	
    //***** zone page (end) *******************************************************************************************************
    
    //***** pod page (begin) ******************************************************************************************************
    function podJsonToDetailsTab(jsonObj) {	    
	    var $detailsTab = $podPage.find("#tab_content_details");   
        $detailsTab.data("jsonObj", jsonObj);           
        $detailsTab.find("#id").text(fromdb(jsonObj.id));
        $detailsTab.find("#name").text(fromdb(jsonObj.name));
        $detailsTab.find("#cidr").text(fromdb(jsonObj.cidr));        
        $detailsTab.find("#ipRange").text(fromdb(jsonObj.ipRange));
        $detailsTab.find("#gateway").text(fromdb(jsonObj.gateway));  
        
        //if (getDirectAttachUntaggedEnabled() == "true") 
		//	$("#submenu_content_zones #action_add_directip_vlan").data("type", "pod").data("id", obj.id).data("name", obj.name).data("zoneid", obj.zoneid).show();		
	}	
	//***** pod page (end) ********************************************************************************************************
	
	//***** cluster page (bgein) **************************************************************************************************
	function clusterJsonToDetailsTab(jsonObj) {	    
	    var $detailsTab = $clusterPage.find("#tab_content_details");   
        $detailsTab.data("jsonObj", jsonObj);           
        $detailsTab.find("#id").text(fromdb(jsonObj.id));
        $detailsTab.find("#name").text(fromdb(jsonObj.name));
        $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));        
        $detailsTab.find("#podname").text(fromdb(jsonObj.podname));            
    }
    //***** cluster page (end) ****************************************************************************************************
	
	//***** host page (bgein) *****************************************************************************************************
	function hostJsonToDetailsTab(jsonObj) {	    
	    var $detailsTab = $hostPage.find("#tab_content_details");   
        $detailsTab.data("jsonObj", jsonObj);           
        $detailsTab.find("#id").text(fromdb(jsonObj.id));
        $detailsTab.find("#name").text(fromdb(jsonObj.name));
        $detailsTab.find("#state").text(fromdb(jsonObj.state));        
        $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename)); 
        $detailsTab.find("#podname").text(fromdb(jsonObj.podname));   
        $detailsTab.find("#clustername").text(fromdb(jsonObj.clustername));        
        $detailsTab.find("#ipaddress").text(fromdb(jsonObj.ipaddress)); 
        $detailsTab.find("#version").text(fromdb(jsonObj.version));  
        $detailsTab.find("#oscategoryname").text(fromdb(jsonObj.oscategoryname));        
        $detailsTab.find("#disconnected").text(fromdb(jsonObj.disconnected));        
    }
	//***** host page (end) *******************************************************************************************************
	
	//***** primary storage page (bgein) ******************************************************************************************
	function primarystorageJsonToDetailsTab(jsonObj) {	    
	    var $detailsTab = $primarystoragePage.find("#tab_content_details");   
        $detailsTab.data("jsonObj", jsonObj);           
        $detailsTab.find("#id").text(fromdb(jsonObj.id));
        $detailsTab.find("#name").text(fromdb(jsonObj.name));
        $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));
        $detailsTab.find("#podname").text(fromdb(jsonObj.podname));
        $detailsTab.find("#clustername").text(fromdb(jsonObj.clustername));
        $detailsTab.find("#type").text(fromdb(jsonObj.type));
        $detailsTab.find("#ipaddress").text(fromdb(jsonObj.ipaddress));
        $detailsTab.find("#path").text(fromdb(jsonObj.path));                
		$detailsTab.find("#disksizetotal").text(convertBytes(jsonObj.disksizetotal));
		$detailsTab.find("#disksizeallocated").text(convertBytes(jsonObj.disksizeallocated));
		$detailsTab.find("#tags").text(fromdb(jsonObj.tags));         
    }
	//***** primary storage page (end) *********************************************************************************************
	
	//***** systemVM page (begin) *************************************************************************************************
    function systemvmJsonToDetailsTab(jsonObj) {	   
	    var $detailsTab = $systemvmPage.find("#tab_content_details");   
        $detailsTab.data("jsonObj", jsonObj);   
        
        $detailsTab.find("#state").text(fromdb(jsonObj.state));     
        $detailsTab.find("#systemvmtype").text(toSystemVMTypeText(jsonObj.systemvmtype));    
        $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename)); 
        $detailsTab.find("#id").text(fromdb(jsonObj.id));  
        $detailsTab.find("#name").text(fromdb(jsonObj.name));   
        $detailsTab.find("#activeviewersessions").text(fromdb(jsonObj.activeviewersessions)); 
        $detailsTab.find("#publicip").text(fromdb(jsonObj.publicip)); 
        $detailsTab.find("#privateip").text(fromdb(jsonObj.privateip)); 
        $detailsTab.find("#hostname").text(fromdb(jsonObj.hostname));
        $detailsTab.find("#gateway").text(fromdb(jsonObj.gateway)); 
        $detailsTab.find("#created").text(fromdb(jsonObj.created));             
	}
	
	function toSystemVMTypeText(value) {
	    var text = "";
        if(value == "consoleproxy")
            text = "Console Proxy VM";
        else if(value == "secondarystoragevm")
            text = "Secondary Storage VM";
        return text;        
    }
	//***** systemVM page (end) ***************************************************************************************************
	
	//dialogs	
	initDialog("dialog_add_zone");
	initDialog("dialog_add_pod", 320);
	
	//add button ***
	$("#midmenu_add_link").find("#label").text("Add Zone");     
    $("#midmenu_add_link").show();     
    $("#midmenu_add_link").unbind("click").bind("click", function(event) {  
        $("#dialog_add_zone")
		.dialog('option', 'buttons', { 				
			"Add": function() { 
			    var thisDialog = $(this);
								
				// validate values
				var isValid = true;					
				isValid &= validateString("Name", thisDialog.find("#add_zone_name"), thisDialog.find("#add_zone_name_errormsg"));
				isValid &= validateIp("DNS 1", thisDialog.find("#add_zone_dns1"), thisDialog.find("#add_zone_dns1_errormsg"), false); //required
				isValid &= validateIp("DNS 2", thisDialog.find("#add_zone_dns2"), thisDialog.find("#add_zone_dns2_errormsg"), true);  //optional	
				isValid &= validateIp("Internal DNS 1", thisDialog.find("#add_zone_internaldns1"), thisDialog.find("#add_zone_internaldns1_errormsg"), false); //required
				isValid &= validateIp("Internal DNS 2", thisDialog.find("#add_zone_internaldns2"), thisDialog.find("#add_zone_internaldns2_errormsg"), true);  //optional	
				if (getNetworkType() != "vnet") {
					isValid &= validateString("Zone - Start VLAN Range", thisDialog.find("#add_zone_startvlan"), thisDialog.find("#add_zone_startvlan_errormsg"), false); //required
					isValid &= validateString("Zone - End VLAN Range", thisDialog.find("#add_zone_endvlan"), thisDialog.find("#add_zone_endvlan_errormsg"), true);        //optional
				}
				isValid &= validateCIDR("Guest CIDR", thisDialog.find("#add_zone_guestcidraddress"), thisDialog.find("#add_zone_guestcidraddress_errormsg"), false); //required
				if (!isValid) 
				    return;							
				
				thisDialog.dialog("close"); 
				
				var moreCriteria = [];	
				
				var name = trim(thisDialog.find("#add_zone_name").val());
				moreCriteria.push("&name="+todb(name));
				
				var dns1 = trim(thisDialog.find("#add_zone_dns1").val());
				moreCriteria.push("&dns1="+encodeURIComponent(dns1));
				
				var dns2 = trim(thisDialog.find("#add_zone_dns2").val());
				if (dns2 != null && dns2.length > 0) 
				    moreCriteria.push("&dns2="+encodeURIComponent(dns2));						
									
				var internaldns1 = trim(thisDialog.find("#add_zone_internaldns1").val());
				moreCriteria.push("&internaldns1="+encodeURIComponent(internaldns1));
				
				var internaldns2 = trim(thisDialog.find("#add_zone_internaldns2").val());
				if (internaldns2 != null && internaldns2.length > 0) 
				    moreCriteria.push("&internaldns2="+encodeURIComponent(internaldns2));						
				 											
				if (getNetworkType() != "vnet") {
					var vlanStart = trim(thisDialog.find("#add_zone_startvlan").val());	
					var vlanEnd = trim(thisDialog.find("#add_zone_endvlan").val());						
					if (vlanEnd != null && vlanEnd.length > 0) 
					    moreCriteria.push("&vlan=" + encodeURIComponent(vlanStart + "-" + vlanEnd));									
					else 							
						moreCriteria.push("&vlan=" + encodeURIComponent(vlanStart));		
				}					
				
				var guestcidraddress = trim(thisDialog.find("#add_zone_guestcidraddress").val());
				moreCriteria.push("&guestcidraddress="+encodeURIComponent(guestcidraddress));	
						
				
				var template = $("#zone_template").clone(true);
			    var loadingImg = template.find(".adding_loading");										
			    var row_container = template.find("#row_container");    			
			    $("#zonetree1").find("#zones_container").prepend(template);			    			            
                loadingImg.show();  
                row_container.hide();             
	            template.fadeIn("slow");
                $.ajax({
			        data: createURL("command=createZone"+moreCriteria.join("")),
				    dataType: "json",
				    success: function(json) {
					    var item = json.createzoneresponse;					    
					    zoneJSONToTreeNode(item, template);	
					    loadingImg.hide();	
					    row_container.show();		        
				    },
			        error: function(XMLHttpResponse) {
			            handleError(XMLHttpResponse);
			            template.slideUp("slow", function() {
						    $(this).remove();
					    });
			        }
			    });
			}, 
			"Cancel": function() { 
			    var thisDialog = $(this);
				thisDialog.dialog("close"); 
				cleanErrMsg(thisDialog.find("#add_zone_name"), thisDialog.find("#add_zone_name_errormsg"));
				cleanErrMsg(thisDialog.find("#add_zone_dns1"), thisDialog.find("#add_zone_dns1_errormsg"));
				cleanErrMsg(thisDialog.find("#add_zone_dns2"), thisDialog.find("#add_zone_dns2_errormsg"));
				cleanErrMsg(thisDialog.find("#add_zone_internaldns1"), thisDialog.find("#add_zone_internaldns1_errormsg"));
				cleanErrMsg(thisDialog.find("#add_zone_internaldns2"), thisDialog.find("#add_zone_internaldns2_errormsg"));
				cleanErrMsg(thisDialog.find("#add_zone_startvlan"), thisDialog.find("#add_zone_startvlan_errormsg"));
				cleanErrMsg(thisDialog.find("#add_zone_guestcidraddress"), thisDialog.find("#add_zone_guestcidraddress_errormsg"));
			} 
		}).dialog("open");        
        return false;
    });  
}

