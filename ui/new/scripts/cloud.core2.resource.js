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

    function podJSONToTemplate(json, template) {}
}

