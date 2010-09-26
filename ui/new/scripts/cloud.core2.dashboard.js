function afterLoadDashboardJSP() {
        if (isAdmin()) {
		var sessionExpired = false;
		var zones = null;
		var noZones = false;
		var noPods = true;
		//$("#menutab_dashboard_root, #menutab_vm, #menutab_networking_old, #menutab_networking, #menutab_templates, #menutab_events, #menutab_hosts, #menutab_storage, #menutab_accounts, #menutab_domain").hide();							
   
        $.ajax({
		    data: createURL("command=listZones&available=true"+maxPageSize),
			dataType: "json",
			async: false,
			success: function(json) {
				zones = json.listzonesresponse.zone;
				var zoneSelect = $("#capacity_zone_select").empty();	
				if (zones != null && zones.length > 0) {
					for (var i = 0; i < zones.length; i++) {
						zoneSelect.append("<option value='" + zones[i].id + "'>" + fromdb(zones[i].name) + "</option>"); 								
						if(noPods == true) {
						    $.ajax({
						        data: createURL("command=listPods&zoneId="+zones[i].id),
				                dataType: "json",
				                async: false,
				                success: function(json) {
					                var pods = json.listpodsresponse.pod;						
					                if (pods != null && pods.length > 0) {
    							        noPods = false;
    							        //$("#menutab_dashboard_root, #menutab_vm, #menutab_networking_old, #menutab_networking, #menutab_templates, #menutab_events, #menutab_hosts, #menutab_storage, #menutab_accounts, #menutab_domain").show();							
					                }							
				                }
			                });
						}
					}
				} else {							
					noZones = true;
				}
			}
		});
		
		
        $("#capacity_zone_select").bind("change", function(event) {
			var zoneId = $(this).val();
			$.ajax({
			    data: createURL("command=listPods&zoneId="+zoneId+maxPageSize),
				dataType: "json",
				async: false,
				success: function(json) {
					var pods = json.listpodsresponse.pod;
					var podSelect = $("#capacity_pod_select").empty();	
					if (pods != null && pods.length > 0) {
						podSelect.append("<option value='All'>All pods</option>"); 
					    for (var i = 0; i < pods.length; i++) {
						    podSelect.append("<option value='" + pods[i].name + "'>" + fromdb(pods[i].name) + "</option>"); 
					    }
					}
					$("#capacity_pod_select").change();
				}
			});
		});
		$("#capacity_zone_select").change();
	} 
	else if (isDomainAdmin()) {
	
	} 
	else if(isUser()) {	
	
	} 
	else { //no role 
	    logout(false);
	    return;
	}
}