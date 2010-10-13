function afterLoadResourceJSP() {
    //***** switch between different tabs (begin) ********************************************************************
    var tabArray = ["tab_details", "tab_network", "tab_secondary_storage"];
    var tabContentArray = ["tab_content_details", "tab_content_network", "tab_content_secondary_storage"];
    switchBetweenDifferentTabs(tabArray, tabContentArray);       
    //***** switch between different tabs (end) **********************************************************************
  
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
					zoneJSONToTemplate(items[i], template);
					container.append(template.show());
				}
			}
		}
	});  

    function zoneJSONToTemplate(json, template) {
        var zoneid = json.id;
        template.attr("id", "zone_" + zoneid);  
	    template.data("id", zoneid).data("name", fromdb(json.name));
	    template.find("#zone_name")
		    .text(fromdb(json.name))
		    .data("id", zoneid)
		    .data("name", fromdb(json.name))
		    .data("dns1", json.dns1)
		    .data("internaldns1", json.internaldns1)
		    .data("guestcidraddress", json.guestcidraddress);		
	    if (json.dns2 != null) 
		    template.find("#zone_name").data("dns2", json.dns2);	
	    if (json.internaldns2 != null) 
		    template.find("#zone_name").data("internaldns2", json.internaldns2);	
	    if (json.vlan != null) 
		    template.find("#zone_name").data("vlan", json.vlan);		
    	
	    $.ajax({
	        data: createURL("command=listPods&zoneid="+zoneid+maxPageSize),
		    dataType: "json",
		    success: function(json) {
			    var items = json.listpodsresponse.pod;
			    var container = template.find("#pods_container").empty();
			    if (items != null && items.length > 0) {					    
				    for (var i = 0; i < items.length; i++) {
					    var podTemplate = $("#pod_template").clone(true).attr("id", "pod_"+items[i].id);
					    podJSONToTemplate(items[i], podTemplate);
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
					    systemvmJSONToTemplate(items[i], systemvmTemplate);
					    container.append(systemvmTemplate.show());
				    }
			    }
		    }
	    });
    }
    
    function podJSONToTemplate(json, template) {	
        var podid = json.id;
        template.attr("id", "pod_" + podid);  //???
    	    
		var ipRange = getIpRange(json.startip, json.endip);			
		template.data("id", podid).data("name", json.name);
		
		var podName = template.find("#pod_name").text(json.name);
		podName.data("id", podid);
		podName.data("zoneid", json.zoneid);
		podName.data("name", json.name);
		podName.data("cidr", json.cidr);
		podName.data("startip", json.startip);
		podName.data("endip", json.endip);
		podName.data("ipRange", ipRange);		
		podName.data("gateway", json.gateway);		
		
	    $.ajax({
            data: createURL("command=listClusters&podid="+podid+maxPageSize),
	        dataType: "json",
	        success: function(json) {
		        var items = json.listclustersresponse.cluster;
		        var container = template.find("#clusters_container").empty();
		        if (items != null && items.length > 0) {					    
			        for (var i = 0; i < items.length; i++) {
				        var clusterTemplate = $("#cluster_template").clone(true).attr("id", "cluster_"+items[i].id);
				        clusterJSONToTemplate(items[i], clusterTemplate);
				        container.append(clusterTemplate.show());
			        }
		        }
	        }
        });		
	}
		
	function systemvmJSONToTemplate(json, template) {	
	    var systemvmid = json.id;	
	    template.attr("id", "systemvm_"+systemvmid);
	    template.data("id", systemvmid).data("name", json.name);	     
	    var systeymvmName = template.find("#systemvm_name").text(json.name);
		systeymvmName.data("systemvmtype", json.systemvmtype);
		systeymvmName.data("name", json.name);	
		systeymvmName.data("zonename", json.zonename);
		systeymvmName.data("activeviewersessions", json.activeviewersessions);	
		systeymvmName.data("publicip", json.publicip);
		systeymvmName.data("privateip", json.privateip);	
		systeymvmName.data("hostname", json.hostname);
		systeymvmName.data("gateway", json.gateway);	
		systeymvmName.data("created", json.created);
		systeymvmName.data("state", json.state);	
	}
			
	function clusterJSONToTemplate(json, template) {
	    template.data("id", json.id).data("name", fromdb(json.name));
	    
	    var systeymvmName = template.find("#cluster_name").text(fromdb(json.name));
	    	   
	    $.ajax({
            data: createURL("command=listHosts&clusterid="+json.id+maxPageSize),
	        dataType: "json",
	        success: function(json) {
		        var items = json.listhostsresponse.host;
		        var container = template.find("#hosts_container").empty();
		        if (items != null && items.length > 0) {					    
			        for (var i = 0; i < items.length; i++) {
				        var hostTemplate = $("#host_template").clone(true).attr("id", "host_"+items[i].id);
				        hostJSONToTemplate(items[i], hostTemplate);
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
				        primaryStorageJSONToTemplate(items[i], primaryStorageTemplate);
				        container.append(primaryStorageTemplate.show());
			        }
		        }
	        }
        });		    
	}
	
	function hostJSONToTemplate(json, template) {
	    template.data("id", json.id).data("name", fromdb(json.name));
	    
	    var hostName = template.find("#host_name").text(fromdb(json.name));
	}
	
	function primaryStorageJSONToTemplate(json, template) {
	    template.data("id", json.id).data("name", fromdb(json.name));
	    
	    var primaryStorageName = template.find("#primarystorage_name").text(fromdb(json.name));
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
					$("#zone_"+id).find("#zone_content").show();					
					target.removeClass().addClass("zonetree_openarrows");
				} else {
					$("#zone_"+id).find("#zone_content").hide();
					target.removeClass().addClass("zonetree_closedarrows");
				}
				break;					
			case "zone_icon":
			case "zone_label":
			case "zone_name":	
			    $zoneetree1.find(".selected").removeClass("selected");
			    template.find("#zone_node").addClass("selected");	
			    var obj = {"id": target.data("id"), "name": target.data("name"), "dns1": target.data("dns1"), "dns2": target.data("dns2"), "internaldns1": target.data("internaldns1"), "internaldns2": target.data("internaldns2"), "vlan": target.data("vlan"), "guestcidraddress": target.data("guestcidraddress")};
				//zoneObjectToRightPanel(obj);				    		   			    
			    break;
			
			
			case "pod_expand" :			   
				if (target.hasClass("zonetree_closedarrows")) {
					$("#zone_"+id).find("#pod_content").show();					
					target.removeClass().addClass("zonetree_openarrows");
				} else {
					$("#zone_"+id).find("#pod_content").hide();
					target.removeClass().addClass("zonetree_closedarrows");
				}
				break;			
			case "pod_icon":
			case "pod_label":	
			case "pod_name" :			   
				$zoneetree1.find(".selected").removeClass("selected");
			    template.find("#pod_node").addClass("selected");
				var obj = {"id": target.data("id"), "zoneid": target.data("zoneid"), "name": target.data("name"), "cidr": target.data("cidr"), "startip": target.data("startip"), "endip": target.data("endip"), "ipRange": target.data("ipRange"), "gateway": target.data("gateway")};
				//podObjectToRightPanel(obj);				
				break;
				
			
			case "cluster_expand" :			   
				if (target.hasClass("zonetree_closedarrows")) {
					$("#zone_"+id).find("#cluster_content").show();					
					target.removeClass().addClass("zonetree_openarrows");
				} else {
					$("#zone_"+id).find("#cluster_content").hide();
					target.removeClass().addClass("zonetree_closedarrows");
				}
				break;			
			case "cluster_icon":
			case "cluster_label":	
			case "cluster_name" :			   
				$zoneetree1.find(".selected").removeClass("selected");
			    template.find("#cluster_node").addClass("selected");
				var obj = {"id": target.data("id"), "zoneid": target.data("zoneid"), "name": target.data("name"), "cidr": target.data("cidr"), "startip": target.data("startip"), "endip": target.data("endip"), "ipRange": target.data("ipRange"), "gateway": target.data("gateway")};
				//clusterObjectToRightPanel(obj);				
				break;	
				
				
			case "host_expand" :			   
				if (target.hasClass("zonetree_closedarrows")) {
					$("#zone_"+id).find("#host_content").show();					
					target.removeClass().addClass("zonetree_openarrows");
				} else {
					$("#zone_"+id).find("#host_content").hide();
					target.removeClass().addClass("zonetree_closedarrows");
				}
				break;			
			case "host_icon":
			case "host_label":	
			case "host_name" :			   
				$zoneetree1.find(".selected").removeClass("selected");
			    template.find("#host_node").addClass("selected");
				var obj = {"id": target.data("id"), "zoneid": target.data("zoneid"), "name": target.data("name"), "cidr": target.data("cidr"), "startip": target.data("startip"), "endip": target.data("endip"), "ipRange": target.data("ipRange"), "gateway": target.data("gateway")};
				//hostObjectToRightPanel(obj);				
				break;	
			
			
			case "primarystorage_expand" :			   
				if (target.hasClass("zonetree_closedarrows")) {
					$("#zone_"+id).find("#primarystorage_content").show();					
					target.removeClass().addClass("zonetree_openarrows");
				} else {
					$("#zone_"+id).find("#primarystorage_content").hide();
					target.removeClass().addClass("zonetree_closedarrows");
				}
				break;			
			case "primarystorage_icon":
			case "primarystorage_label":	
			case "primarystorage_name" :			   
				$zoneetree1.find(".selected").removeClass("selected");
			    template.find("#primarystorage_node").addClass("selected");
				var obj = {"id": target.data("id"), "zoneid": target.data("zoneid"), "name": target.data("name"), "cidr": target.data("cidr"), "startip": target.data("startip"), "endip": target.data("endip"), "ipRange": target.data("ipRange"), "gateway": target.data("gateway")};
				//primarystorageObjectToRightPanel(obj);				
				break;
						
			
			case "systemvm_icon":
			case "systemvm_label":	
			case "systemvm_name" :			   
				$zoneetree1.find(".selected").removeClass("selected");
			    template.find("#systemvm_node").addClass("selected");
				var obj = {"id": target.data("id"), "zoneid": target.data("zoneid"), "name": target.data("name"), "cidr": target.data("cidr"), "startip": target.data("startip"), "endip": target.data("endip"), "ipRange": target.data("ipRange"), "gateway": target.data("gateway")};
				//systemvmObjectToRightPanel(obj);				
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
			ipRange = ipRange + "-" + endip;
		}		
		return ipRange;
	}	
}

