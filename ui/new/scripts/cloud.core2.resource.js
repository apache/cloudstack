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
    
    function showPage($pageToShow) {        
        for(var i=0; i<pageArray.length; i++) {
            if(pageArray[i].attr("id") == $pageToShow.attr("id")) {
                $rightPanelHeaderLabel.text(pageLabelArray[i]);
                pageArray[i].show();
            }
            else {
                pageArray[i].hide();
            }
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
					var template = $("#zone_template").clone(true).attr("id", "zone_"+items[i].id);
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
					    var podTemplate = $("#pod_template").clone(true).attr("id", "pod_"+items[i].id);
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
					    var systemvmTemplate = $("#systemvm_template").clone(true).attr("id", "systemvm_"+items[i].id);
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
		template.data("id", podid).data("name", json.name);
		
		var podName = template.find("#pod_name").text(json.name);
		podName.data("jsonObj", json);	    
			
	    $.ajax({
            data: createURL("command=listClusters&podid="+podid+maxPageSize),
	        dataType: "json",
	        success: function(json) {
		        var items = json.listclustersresponse.cluster;
		        var container = template.find("#clusters_container").empty();
		        if (items != null && items.length > 0) {					    
			        for (var i = 0; i < items.length; i++) {
				        var clusterTemplate = $("#cluster_template").clone(true).attr("id", "cluster_"+items[i].id);
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
				        var hostTemplate = $("#host_template").clone(true).attr("id", "host_"+items[i].id);
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
				        var primaryStorageTemplate = $("#primarystorage_template").clone(true).attr("id", "primary_storage_"+items[i].id);
				        primaryStorageJSONToTreeNode(items[i], primaryStorageTemplate);
				        container.append(primaryStorageTemplate.show());
			        }
		        }
	        }
        });		    
	}
	
	function hostJSONToTreeNode(json, template) {
	    template.data("id", json.id).data("name", fromdb(json.name));	    
	    var hostName = template.find("#host_name").text(fromdb(json.name));
	    hostName.data("jsonObj", json);
	}
	
	function primaryStorageJSONToTreeNode(json, template) {
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
			    showPage($zonePage);				    
			    var jsonObj = target.data("jsonObj");    
			    zoneJsonToDetailsTab(jsonObj);							    		   			    
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
				showPage($podPage);			    
			    var jsonObj = target.data("jsonObj");
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
			    showPage($clusterPage);
			    var jsonObj = target.data("jsonObj");
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
			    showPage($hostPage);
				var jsonObj = target.data("jsonObj");
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
			    showPage($primarystoragePage);
			    var jsonObj = target.data("jsonObj");
				primarystorageJsonToDetailsTab(jsonObj);					
				break;
						
						
			case "systemvm_name" :			   
				$zoneetree1.find(".selected").removeClass("selected");			    		    
			    target.parent().parent().parent().addClass("selected");		
			    showPage($systemvmPage);
			    var jsonObj = target.data("jsonObj");						
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
}

