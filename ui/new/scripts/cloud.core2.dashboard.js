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
		
		//???		
		if (sessionExpired) 
		    return false;
		    
		if (noZones || noPods) {
//			$("#tab_dashboard_user").hide();
//			$("#menutab_role_user").hide();
//			$("#menutab_role_root").show();
//			$("#menutab_configuration").click();
			return false;
		}
		
		var capacities = null;
		$.ajax({
			cache: false,
			async: false,
			data: createURL("command=listCapacity"),
			dataType: "json",
			success: function(json) {
				capacities = json.listcapacityresponse.capacity;
			}
		});
		
		$("#capacity_pod_select").bind("change", function(event) {		    
		    event.stopPropagation();		    
		    var selectedZone = $("#capacity_zone_select option:selected").text();
			var selectedPod = $("#capacity_pod_select").val();
			
			// Reset to Defaults			
			var $capacityContainer = $("#system_wide_capacity_container");
			//var $allSections = $capacityContainer.find("#public_ip_address, #secondary_storage_used, #memory_allocated, #cpu, #primary_storage_used, #primary_storage_allocated, #private_ip_address");
		    $capacityContainer.find("#capacityused").text("N");
		    $capacityContainer.find("#capacitytotal").text("A");
		    $capacityContainer.find("#percentused").text("");		
		    $capacityContainer.find("#bar_chart").removeClass().addClass("db_barbox").css("width", "0%");    
			/*
			$(".db_bargraph_barbox_safezone").attr("style", "width:0%");
			$(".db_bargraph_barbox_unsafezone").attr("style", "width:0%");
			*/	
			
			if (capacities != null && capacities.length > 0) {
				for (var i = 0; i < capacities.length; i++) {
					var capacity = capacities[i];
					if (capacity.zonename == selectedZone) {
										
						// ***** Public IPs Addresses *****
						if (capacity.type == "4") {
						    var $c = $capacityContainer.find("#public_ip_address");
						    $c.find("#capacityused").text(capacity.capacityused);
						    $c.find("#capacitytotal").text(capacity.capacitytotal);						    
						    capacityBarChart($c, capacity.percentused);
							/*
							$("#public_ip_used").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + capacity.capacityused + " / " + capacity.percentused + "%");
							$("#public_ip_total").text("Total: " + capacity.capacitytotal);
							var usedPercentage = parseInt(capacity.percentused);
							if (usedPercentage > 70) {
								$("#capacity_public_ip .db_bargraph_barbox_safezone").attr("style", "width:70%");
								if(usedPercentage <= 100) 										
								    $("#capacity_public_ip .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
								else 
								    $("#capacity_public_ip .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
							} else {
								$("#capacity_public_ip .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
							    $("#capacity_public_ip .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
							}	
							*/
						} 						
						
						// ***** Secondary Storage Used *****
						else if (capacity.type == "6") {
						    var $c = $capacityContainer.find("#secondary_storage_used");
						    $c.find("#capacityused").text(convertBytes(parseInt(capacity.capacityused)));
						    $c.find("#capacitytotal").text(convertBytes(parseInt(capacity.capacitytotal)));						    
						    capacityBarChart($c, capacity.percentused);
						    /*
							$("#sec_storage_used").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + convertBytes(parseInt(capacity.capacityused)) + " / " + capacity.percentused + "%");
							$("#sec_storage_total").text("Total: " + convertBytes(parseInt(capacity.capacitytotal)));
							var usedPercentage = parseInt(capacity.percentused);
							if (usedPercentage > 70) {
								$("#capacity_sec_storage .db_bargraph_barbox_safezone").attr("style", "width:70%");
								if(usedPercentage <= 100) 
								    $("#capacity_sec_storage .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
								else
								    $("#capacity_sec_storage .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
							} else {
								$("#capacity_sec_storage .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
							    $("#capacity_sec_storage .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
							}
							*/
						} 
						
						else {						    
							if (capacity.podname == selectedPod) {							    
								// ***** Memory Allocated *****
								if (capacity.type == "0") {
								    var $c = $capacityContainer.find("#memory_allocated");
						            $c.find("#capacityused").text(convertBytes(parseInt(capacity.capacityused)));
						            $c.find("#capacitytotal").text(convertBytes(parseInt(capacity.capacitytotal)));						            
								    capacityBarChart($c, capacity.percentused);
								    /*
									$("#memory_used").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + convertBytes(parseInt(capacity.capacityused)) + " / " + capacity.percentused + "%");
									$("#memory_total").text("Total: " + convertBytes(parseInt(capacity.capacitytotal)));
									var usedPercentage = parseInt(capacity.percentused);
									if (usedPercentage > 70) {
										$("#capacity_memory .db_bargraph_barbox_safezone").attr("style", "width:70%");
										if(usedPercentage <= 100) 
										    $("#capacity_memory .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
										else
										    $("#capacity_memory .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
									} else {
										$("#capacity_memory .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
									    $("#capacity_memory .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
									}
									*/
								} 
																
								// ***** CPU *****
								else if (capacity.type == "1") {
								    var $c = $capacityContainer.find("#cpu");
						            $c.find("#capacityused").text(convertHz(parseInt(capacity.capacityused)));
						            $c.find("#capacitytotal").text(convertHz(parseInt(capacity.capacitytotal)));						            
								    capacityBarChart($c, capacity.percentused);
								    /*
									$("#cpu_used").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + convertHz(parseInt(capacity.capacityused)) + " / " + capacity.percentused + "%");
									$("#cpu_total").text("Total: " + convertHz(parseInt(capacity.capacitytotal)));
									var usedPercentage = parseInt(capacity.percentused);
									if (usedPercentage > 70) {
										$("#capacity_cpu .db_bargraph_barbox_safezone").attr("style", "width:70%");
										if(usedPercentage <= 100) 
										    $("#capacity_cpu .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
										else
										    $("#capacity_cpu .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
									} else {
										$("#capacity_cpu .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
									    $("#capacity_cpu .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
									}
									*/									
								} 
																
								// ***** Primary Storage Used *****
								else if (capacity.type == "2") {
								    var $c = $capacityContainer.find("#primary_storage_used");
						            $c.find("#capacityused").text(convertBytes(parseInt(capacity.capacityused)));
						            $c.find("#capacitytotal").text(convertBytes(parseInt(capacity.capacitytotal)));						            
						            capacityBarChart($c, capacity.percentused);
								    /*
									$("#storage_used").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + convertBytes(parseInt(capacity.capacityused)) + " / " + capacity.percentused + "%");
									$("#storage_total").text("Total: " + convertBytes(parseInt(capacity.capacitytotal)));
									var usedPercentage = parseInt(capacity.percentused);									
									if (usedPercentage > 70) {
										$("#capacity_storage .db_bargraph_barbox_safezone").attr("style", "width:70%");
										if(usedPercentage <= 100) 
										    $("#capacity_storage .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
										else
										    $("#capacity_storage .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
									} else {
										$("#capacity_storage .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
									    $("#capacity_storage .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
									}
									*/
								} 
																
								// ***** Primary Storage Allocated *****
								else if (capacity.type == "3") {
								    var $c = $capacityContainer.find("#primary_storage_allocated");
						            $c.find("#capacityused").text(convertBytes(parseInt(capacity.capacityused)));
						            $c.find("#capacitytotal").text(convertBytes(parseInt(capacity.capacitytotal)));						            
						            capacityBarChart($c, capacity.percentused);
								    /*
									$("#storage_alloc").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + convertBytes(parseInt(capacity.capacityused)) + " / " + capacity.percentused + "%");
									$("#storage_alloc_total").text("Total: " + convertBytes(parseInt(capacity.capacitytotal)));
									var usedPercentage = parseInt(capacity.percentused);
									if (usedPercentage > 70) {
										$("#capacity_storage_alloc .db_bargraph_barbox_safezone").attr("style", "width:70%");
										if(usedPercentage <= 100) 
										    $("#capacity_storage_alloc .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
										else
										    $("#capacity_storage_alloc .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
									} else {
										$("#capacity_storage_alloc .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
									    $("#capacity_storage_alloc .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
									}
									*/
								} 
																
								// ***** Private IP Addresses *****
								else if (capacity.type == "5") {								
								    var $c = $capacityContainer.find("#private_ip_address");
						            $c.find("#capacityused").text(capacity.capacityused);
						            $c.find("#capacitytotal").text(capacity.capacitytotal);						            
								    capacityBarChart($c, capacity.percentused);
								    /*
									$("#private_ip_used").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + capacity.capacityused + " / " + capacity.percentused + "%");
									$("#private_ip_total").text("Total: " + capacity.capacitytotal);
									var usedPercentage = parseInt(capacity.percentused);
									if (usedPercentage > 70) {
										$("#capacity_private_ip .db_bargraph_barbox_safezone").attr("style", "width:70%");
										if(usedPercentage <= 100) 
										    $("#capacity_private_ip .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
										else
										    $("#capacity_private_ip .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
									} else {
										$("#capacity_private_ip .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
									    $("#capacity_private_ip .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
									}
									*/									
								}								
								
							}
						}
					}
				}
			}
		});	
		//???
		
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

function capacityBarChart($capacity, percentused) {
    var percentused2 = (percentused + "%");
    $capacity.find("#percentused").text(percentused2);

    if (percentused <= 60)
        $capacity.find("#bar_chart").removeClass().addClass("db_barbox low").css("width", percentused2); 
    else if (percentused > 60 && percentused <= 80 )
        $capacity.find("#bar_chart").removeClass().addClass("db_barbox mid").css("width", percentused2);
    else if (percentused > 80 )
        $capacity.find("#bar_chart").removeClass().addClass("db_barbox high").css("width", percentused2);
}