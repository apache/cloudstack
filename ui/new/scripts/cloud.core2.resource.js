function afterLoadResourceJSP() {
    //***** switch between different tabs (begin) ********************************************************************
    var tabArray = ["tab_details", "tab_network", "tab_secondary_storage"];
    var tabContentArray = ["tab_content_details", "tab_content_network", "tab_content_secondary_storage"];
    switchBetweenDifferentTabs(tabArray, tabContentArray);       
    //***** switch between different tabs (end) **********************************************************************
  
    var forceLogout = true;  // We force a logout only if the user has first added a POD for the very first time   
    $("#midmenu_container").append($("#zonetree").clone().attr("id", "zonetree1").show());
    
     $.ajax({
	  data: createURL("command=listZones&available=true&response=json"+maxPageSize),
		dataType: "json",
		success: function(json) {
			var zones = json.listzonesresponse.zone;
			var grid = $("#zonetree1 #zones_container").empty();
			if (zones != null && zones.length > 0) {					    
				for (var i = 0; i < zones.length; i++) {
					var template = $("#zone_template").clone(true).attr("id", "zone_"+zones[i].id);
					zoneJSONToTemplate(zones[i], template);
					grid.append(template.show());
				}
			}
		}
	});  

    function zoneJSONToTemplate(json, template) {
        var zoneid = json.id;
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
	      data: createURL("command=listPods&zoneid="+zoneid+"&response=json"),
		    dataType: "json",
		    success: function(json) {
			    var pods = json.listpodsresponse.pod;
			    var grid = template.find("#pods_container").empty();
			    if (pods != null && pods.length > 0) {					    
				    for (var i = 0; i < pods.length; i++) {
					    var podTemplate = $("#pod_template").clone(true).attr("id", "pod_"+pods[i].id);
					    podJSONToTemplate(pods[i], podTemplate);
					    grid.append(podTemplate.show());
					    forceLogout = false;
				    }
			    }
		    }
	    });
    }

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
	
    function podJSONToTemplate(json, template) {		    
		var ipRange = getIpRange(json.startip, json.endip);			
		template.data("id", json.id).data("name", json.name);
		
		var podName = template.find("#pod_name").text(json.name);
		podName.data("id", json.id);
		podName.data("zoneid", json.zoneid);
		podName.data("name", json.name);
		podName.data("cidr", json.cidr);
		podName.data("startip", json.startip);
		podName.data("endip", json.endip);
		podName.data("ipRange", ipRange);		
		podName.data("gateway", json.gateway);				
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
					$("#zone_"+id+" #zone_content").show();
					target.removeClass().addClass("zonetree_openarrows");
				} else {
					$("#zone_"+id+" #zone_content").hide();
					target.removeClass().addClass("zonetree_closedarrows");
				}
				break;
			case "zone_name" :
				$(".zonetree_firstlevel_selected").removeClass().addClass("zonetree_firstlevel");
				$(".zonetree_secondlevel_selected").removeClass().addClass("zonetree_secondlevel");
				template.find(".zonetree_firstlevel").removeClass().addClass("zonetree_firstlevel_selected");
									
				var obj = {"id": target.data("id"), "name": target.data("name"), "dns1": target.data("dns1"), "dns2": target.data("dns2"), "internaldns1": target.data("internaldns1"), "internaldns2": target.data("internaldns2"), "vlan": target.data("vlan"), "guestcidraddress": target.data("guestcidraddress")};
				//zoneObjectToRightPanel(obj);					
				
				break;
				
			case "pod_name" :
				$(".zonetree_firstlevel_selected").removeClass().addClass("zonetree_firstlevel");
				$(".zonetree_secondlevel_selected").removeClass().addClass("zonetree_secondlevel");
				target.parent(".zonetree_secondlevel").removeClass().addClass("zonetree_secondlevel_selected");
									
				var obj = {"id": target.data("id"), "zoneid": target.data("zoneid"), "name": target.data("name"), "cidr": target.data("cidr"), "startip": target.data("startip"), "endip": target.data("endip"), "ipRange": target.data("ipRange"), "gateway": target.data("gateway")};
				//podObjectToRightPanel(obj);
				
				break;
			
			default:
				break;
		}
		return false;
	});
	
}

