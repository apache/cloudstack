function volumeGetSearchParams() {
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
		        
        if ($advancedSearchPopup.find("#adv_search_domain_li").css("display") != "none"
	        && $advancedSearchPopup.find("#domain").hasClass("textwatermark") == false) {
	        var domainName = $advancedSearchPopup.find("#domain").val();
	        if (domainName != null && domainName.length > 0) { 	
				var domainId;							    
			    if(autoCompleteDomains != null && autoCompleteDomains.length > 0) {									
				    for(var i=0; i < autoCompleteDomains.length; i++) {					        
				      if(fromdb(autoCompleteDomains[i].name).toLowerCase() == domainName.toLowerCase()) {
				          domainId = autoCompleteDomains[i].id;
				          break;	
				      }
			        } 					   			    
			    } 	     	
	            if(domainId == null) { 
			        showError(false, $advancedSearchPopup.find("#domain"), $advancedSearchPopup.find("#domain_errormsg"), g_dictionary["label.not.found"]);
			    }
			    else { //e.g. domainId == 5 (number)
			        showError(true, $advancedSearchPopup.find("#domain"), $advancedSearchPopup.find("#domain_errormsg"), null)
			        moreCriteria.push("&domainid="+todb(domainId));	
			    }
			}
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

function afterLoadVolumeJSP() {
    initDialog("dialog_create_template", 420); 
    initDialog("dialog_create_snapshot");        
    initDialog("dialog_recurring_snapshot", 420);	    
    initDialog("dialog_add_volume");	
    initDialog("dialog_attach_volume");	
    initDialog("dialog_add_volume_from_snapshot");	
    initDialog("dialog_create_template_from_snapshot", 450);    	
	initDialog("dialog_download_volume");
	
	if(isAdmin())
	    $("#dialog_create_template_from_snapshot").find("#isfeatured_container").show();
	else
	    $("#dialog_create_template_from_snapshot").find("#isfeatured_container").hide();		
	        
    $.ajax({
        data: createURL("command=listOsTypes"),
	    dataType: "json",
	    success: function(json) {
		    types = json.listostypesresponse.ostype;
		    if (types != null && types.length > 0) {
			    var osTypeField1 = $("#dialog_create_template #create_template_os_type").empty();
			    var osTypeField2 = $("#dialog_create_template_from_snapshot #os_type").empty();	
			    for (var i = 0; i < types.length; i++) {
				    osTypeField1.append("<option value='" + types[i].id + "'>" + types[i].description + "</option>");
				    osTypeField2.append("<option value='" + types[i].id + "'>" + types[i].description + "</option>");
			    }
		    }	
	    }
    });   
     
    $.ajax({
        data: createURL("command=listZones&available=true"),
	    dataType: "json",
	    success: function(json) {
		    var zones = json.listzonesresponse.zone;
		    var volumeZoneSelect = $("#dialog_add_volume").find("#volume_zone").empty();			
		    if (zones != null && zones.length > 0) {
		        for (var i = 0; i < zones.length; i++) {
			        volumeZoneSelect.append("<option value='" + zones[i].id + "'>" + fromdb(zones[i].name) + "</option>"); 
		        }
		    }				
	    }
	});	
	
	$.ajax({
        data: createURL("command=listDiskOfferings"),
	    dataType: "json",
	    success: function(json) {			    
	        var offerings = json.listdiskofferingsresponse.diskoffering;								
		    var volumeDiskOfferingSelect = $("#dialog_add_volume").find("#volume_diskoffering").empty();	
		    if (offerings != null && offerings.length > 0) {								
				for (var i = 0; i < offerings.length; i++) {		
					var $option = $("<option value='" + offerings[i].id + "'>" + fromdb(offerings[i].displaytext) + "</option>");	
					$option.data("jsonObj", offerings[i]);	
					volumeDiskOfferingSelect.append($option); 
				}	
				$("#dialog_add_volume").find("#volume_diskoffering").change();	
			}	
	    }
    });	 
    
    $("#dialog_add_volume").find("#volume_diskoffering").unbind("change").bind("change", function(event) {        
        var jsonObj = $(this).find("option:selected").data("jsonObj");
        if(jsonObj != null && jsonObj.iscustomized == true) {
            $("#dialog_add_volume").find("#size_container").show();
        }
        else {
            $("#dialog_add_volume").find("#size_container").hide();  
            $("#dialog_add_volume").find("#size").val("");
        }      
    });  
      
    //add volume button ***      
    $("#add_volume_button").unbind("click").bind("click", function(event) {   
        $("#dialog_add_volume")
	    .dialog('option', 'buttons', { 			    
		    "Add": function() { 
		        var thisDialog = $(this);
		    			            										
		        // validate values							
			    var isValid = true;									
			    isValid &= validateString("Name", thisDialog.find("#add_volume_name"), thisDialog.find("#add_volume_name_errormsg"));					    
			    if(thisDialog.find("#size_container").css("display") != "none")
			        isValid &= validateInteger("Size", thisDialog.find("#size"), thisDialog.find("#size_errormsg"));				    			
			    if (!isValid) return;
			    
			    thisDialog.dialog("close");		
				
				var array1 = [];
				
				var name = thisDialog.find("#add_volume_name").val();	
				array1.push("&name="+todb(name));
								
			    var zoneId = thisDialog.find("#volume_zone").val();	
			    array1.push("&zoneId="+zoneId);
			    				    				
			    var diskofferingId = thisDialog.find("#volume_diskoffering").val();	
			    array1.push("&diskOfferingId="+diskofferingId);
			    
			    if(thisDialog.find("#size_container").css("display") != "none") {
			        var size = thisDialog.find("#size").val()
			        array1.push("&size="+size);
			    }
				
				var $midmenuItem1 = beforeAddingMidMenuItem() ;
				    					
			    $.ajax({
				    data: createURL("command=createVolume"+array1.join("")), 
				    dataType: "json",
				    success: function(json) {						        
				        var jobId = json.createvolumeresponse.jobid;				        
				        var timerKey = "createVolumeJob_"+jobId;
							    
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
									    if (result.jobstatus == 1) {
										    // Succeeded										   
										    volumeToMidmenu(result.jobresult.volume, $midmenuItem1);
						                    bindClickToMidMenu($midmenuItem1, volumeToRightPanel, getMidmenuId);  
						                    afterAddingMidMenuItem($midmenuItem1, true);	         
									    } else if (result.jobstatus == 2) {
									        //afterAddingMidMenuItem($midmenuItem1, false, g_dictionary["label.adding.failed"]);	
									        afterAddingMidMenuItem($midmenuItem1, false, fromdb(result.jobresult.errortext));										        								   				    
									    }
								    }
							    },
							    error: function(XMLHttpResponse) {
								    $("body").stopTime(timerKey);
									handleError(XMLHttpResponse, function() {
										afterAddingMidMenuItem($midmenuItem1, false, parseXMLHttpResponse(XMLHttpResponse));
									});
							    }
						    });
					    }, 0);						    					
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
       
	$("#snapshot_interval").change(function(event) {
		var thisElement = $(this);
		var snapshotInterval = thisElement.val();
		var $snapshotIntervalOption = thisElement.find("#snapshot_interval_"+snapshotInterval);
		var jsonObj = $snapshotIntervalOption.data("jsonObj");
		var $dialog = $("#dialog_recurring_snapshot");
		if (jsonObj == undefined || jsonObj == null) {
			$dialog.find("#policy_enabled").text("Disabled");
		} else {
			$dialog.find("#policy_enabled").text("Enabled");
		}
		$dialog.find("#info_container").hide();
		switch (snapshotInterval) {
			case "0": 
	            $dialog.find("#edit_time_colon, #edit_hour_container, #edit_meridiem_container, #edit_day_of_week_container, #edit_day_of_month_container").hide(); 
	            $dialog.find("#edit_past_the_hour, #edit_minute_container").show();	
				if (jsonObj != null) {
					$dialog.find("#edit_minute").val(jsonObj.schedule);            
					$dialog.find("#edit_max").val(jsonObj.maxsnaps); 
					$dialog.find("#edit_timezone").val(jsonObj.timezone);
				} else {
					$dialog.find("#edit_minute").val("");            
					$dialog.find("#edit_max").val(""); 
					$dialog.find("#edit_timezone").val("");
				}
				$dialog.find("#snapshot_form").show();
	            break;
	        case "1":
	            $dialog.find("#edit_past_the_hour, #edit_day_of_week_container, #edit_day_of_month_container").hide(); 
	            $dialog.find("#edit_minute_container, #edit_hour_container, #edit_meridiem_container").show();	
				
				if (jsonObj != null) {
					var parts = jsonObj.schedule.split(":");
					var hour12, meridiem;
					var hour24 = parts[1];                                            
					if(hour24 < 12) {
						hour12 = hour24;
						meridiem = "AM";                                               
					}   
					else {
						hour12 = hour24 - 12;
						meridiem = "PM"
					}											
					if (hour12 < 10 && hour12.toString().length==1) 
						hour12 = "0"+hour12.toString();
									
					$dialog.find("#edit_minute").val(parts[0]);
					$dialog.find("#edit_hour").val(hour12); 
					$dialog.find("#edit_meridiem").val(meridiem);          
					$dialog.find("#edit_max").val(jsonObj.maxsnaps); 
					$dialog.find("#edit_timezone").val(jsonObj.timezone); 
				} else {
					$dialog.find("#edit_minute").val("");
					$dialog.find("#edit_hour").val(""); 
					$dialog.find("#edit_meridiem").val("");          
					$dialog.find("#edit_max").val(""); 
					$dialog.find("#edit_timezone").val(""); 
				}	
				$dialog.find("#snapshot_form").show();
	            break;
	        case "2":
	            $dialog.find("#edit_past_the_hour, #edit_day_of_month_container").hide(); 
	            $dialog.find("#edit_minute_container, #edit_hour_container, #edit_meridiem_container, #edit_day_of_week_container").show();		           
	            
				if (jsonObj != null) {
					var parts = jsonObj.schedule.split(":");
					var hour12, meridiem;
					var hour24 = parts[1];
					if(hour24 < 12) {
						hour12 = hour24;  
						meridiem = "AM";                                               
					}   
					else {
						hour12 = hour24 - 12;
						meridiem = "PM"
					}
					if (hour12 < 10 && hour12.toString().length==1) 
						hour12 = "0"+hour12.toString();
						
					$dialog.find("#edit_minute").val(parts[0]);
					$dialog.find("#edit_hour").val(hour12); 
					$dialog.find("#edit_meridiem").val(meridiem); 	
					$dialog.find("#edit_day_of_week").val(parts[2]);         
					$dialog.find("#edit_max").val(jsonObj.maxsnaps); 
					$dialog.find("#edit_timezone").val(jsonObj.timezone); 
				} else {
					$dialog.find("#edit_minute").val("");
					$dialog.find("#edit_hour").val(""); 
					$dialog.find("#edit_meridiem").val(""); 	
					$dialog.find("#edit_day_of_week").val("");         
					$dialog.find("#edit_max").val(""); 
					$dialog.find("#edit_timezone").val(""); 
				}
				$dialog.find("#snapshot_form").show();
	            break;
	        case "3":
	            $dialog.find("#edit_past_the_hour, #edit_day_of_week_container").hide(); 
	            $dialog.find("#edit_minute_container, #edit_hour_container, #edit_meridiem_container, #edit_day_of_month_container").show();		           
	            
				if (jsonObj != null) {
					var parts = jsonObj.schedule.split(":");
					var hour12, meridiem;
					var hour24 = parts[1];
					if(hour24 < 12) {
						hour12 = hour24;  
						meridiem = "AM";                                               
					}   
					else {
						hour12 = hour24 - 12;
						meridiem = "PM"
					}
					if (hour12 < 10 && hour12.toString().length==1) 
						hour12 = "0"+hour12.toString();
					$dialog.find("#edit_minute").val(parts[0]);
					$dialog.find("#edit_hour").val(hour12); 
					$dialog.find("#edit_meridiem").val(meridiem); 	
					$dialog.find("#edit_day_of_month").val(parts[2]);         
					$dialog.find("#edit_max").val(jsonObj.maxsnaps); 
					$dialog.find("#edit_timezone").val(jsonObj.timezone); 
				} else {
					$dialog.find("#edit_minute").val("");
					$dialog.find("#edit_hour").val(""); 
					$dialog.find("#edit_meridiem").val(""); 	
					$dialog.find("#edit_day_of_month").val("");         
					$dialog.find("#edit_max").val(""); 
					$dialog.find("#edit_timezone").val("");
				}
				$dialog.find("#snapshot_form").show();
	            break;
		}
	});
	// *** recurring snapshot dialog - event binding (end) ******************************	    
         
    //***** switch between different tabs (begin) ********************************************************************
    var tabArray = [$("#tab_details"), $("#tab_snapshot")];
    var tabContentArray = [$("#tab_content_details"), $("#tab_content_snapshot")];
    var afterSwitchFnArray = [volumeJsonToDetailsTab, volumeJsonToSnapshotTab];
    switchBetweenDifferentTabs(tabArray, tabContentArray, afterSwitchFnArray);       
    //***** switch between different tabs (end) **********************************************************************    
}

function volumeToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_storage_volume.png");		
   
    var firstRowText = fromdb(jsonObj.name);
    $midmenuItem1.find("#first_row").text(clippedText(firstRowText, midMenuFirstRowLength));     
    $midmenuItem1.find("#first_row_container").attr("title", firstRowText);   
    
    var secondRowText = fromdb(jsonObj.type);
    $midmenuItem1.find("#second_row").text(clippedText(secondRowText, midMenuSecondRowLength));
    $midmenuItem1.find("#second_row_container").attr("title", secondRowText);  
}

function volumeToRightPanel($midmenuItem1) {  
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);  
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);  
    $("#tab_details").click();   
}
 
function volumeJsonToDetailsTab(){  
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null) {
        volumeJsonClearDetailsTab();   
        return;
    }
    
    var jsonObj = $midmenuItem1.data("jsonObj");
    if(jsonObj == null) {
        volumeJsonClearDetailsTab();   
        return;
    }
     
    var $thisTab = $("#right_panel_content #tab_content_details");      
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   
    
    var id = jsonObj.id;
        
    $.ajax({
        data: createURL("command=listVolumes&id="+id),
        dataType: "json",
        async: false,
        success: function(json) {  
            var items = json.listvolumesresponse.volume;
            if(items != null && items.length > 0) {
                jsonObj = items[0];
                $midmenuItem1.data("jsonObj", jsonObj);                     
            }
        }
    });      
           
    $thisTab.find("#id").text(fromdb(jsonObj.id));
    $thisTab.find("#name").text(fromdb(jsonObj.name));    
    $thisTab.find("#zonename").text(fromdb(jsonObj.zonename));    
    $thisTab.find("#device_id").text(fromdb(jsonObj.deviceid));   
    $thisTab.find("#state").text(fromdb(jsonObj.state)); 
    $thisTab.find("#account").text(fromdb(jsonObj.account));  
	$thisTab.find("#domain").text(fromdb(jsonObj.domain));
    $thisTab.find("#type").text(fromdb(jsonObj.type) + " (" + fromdb(jsonObj.storagetype) + " storage)");
    $thisTab.find("#size").text((jsonObj.size == "0") ? "" : convertBytes(jsonObj.size));	    
    if (jsonObj.virtualmachineid == null) 
		$thisTab.find("#vm_name").text("detached");
	else 
		$thisTab.find("#vm_name").text(getVmName(jsonObj.vmname, jsonObj.vmdisplayname) + " (" + fromdb(jsonObj.vmstate) + ")");		
    setDateField(jsonObj.created, $thisTab.find("#created"));	
     
    if(isAdmin()) {
    	$thisTab.find("#storage").text(fromdb(jsonObj.storage));
    	$thisTab.find("#storage_container").show();
    }
    else {
    	$thisTab.find("#storage").text("");
    	$thisTab.find("#storage_container").hide();
    }
    
    
    //actions ***    
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    	
    if(jsonObj.hypervisor != "Ovm") {   
      buildActionLinkForTab("label.action.take.snapshot", volumeActionMap, $actionMenu, $midmenuItem1, $thisTab);	//show take snapshot
      buildActionLinkForTab("label.action.recurring.snapshot", volumeActionMap, $actionMenu, $midmenuItem1, $thisTab);	//show Recurring Snapshot
	}
    
    if(jsonObj.state != "Allocated")
        buildActionLinkForTab("label.action.download.volume", volumeActionMap, $actionMenu, $midmenuItem1, $thisTab);
    
    if(jsonObj.state != "Creating" && jsonObj.state != "Corrupted" && jsonObj.name != "attaching") {
        if(jsonObj.type=="ROOT") {
            if (jsonObj.vmstate == "Stopped") { 
                buildActionLinkForTab("label.action.create.template", volumeActionMap, $actionMenu, $midmenuItem1, $thisTab);
            }
        } 
        else { 
	        if (jsonObj.virtualmachineid != null) {
		        if (jsonObj.storagetype == "shared" && (jsonObj.vmstate == "Running" || jsonObj.vmstate == "Stopped" || jsonObj.vmstate == "Destroyed")) {
			        buildActionLinkForTab("label.action.detach.disk", volumeActionMap, $actionMenu, $midmenuItem1, $thisTab); 
		        }
	        } else {
		        // Disk not attached
		        if (jsonObj.storagetype == "shared") {
			        buildActionLinkForTab("label.action.attach.disk", volumeActionMap, $actionMenu, $midmenuItem1, $thisTab);   
    			    			  		    
			        if(jsonObj.vmname == null || jsonObj.vmname == "none")
			            buildActionLinkForTab("label.action.delete.volume", volumeActionMap, $actionMenu, $midmenuItem1, $thisTab); 
		        }
	        }
        }
    }
    
    $thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();     
} 

function volumeJsonToSnapshotTab() {    
	var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");	
	if($midmenuItem1 == null) {
	    volumeClearSnapshotTab();
	    return;
	}
	
	var jsonObj = $midmenuItem1.data("jsonObj");	
	if(jsonObj == null) {
	    volumeClearSnapshotTab();
	    return;
	}
	
	var $thisTab = $("#right_panel_content").find("#tab_content_snapshot");	  	
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   
    
    $.ajax({
		cache: false,
		data: createURL("command=listSnapshots&volumeid="+fromdb(jsonObj.id)),
		dataType: "json",
		success: function(json) {	
		    var $container = $thisTab.find("#tab_container").empty();							    
			var items = json.listsnapshotsresponse.snapshot;																								
			if (items != null && items.length > 0) {			    
				var template = $("#snapshot_tab_template");				
				for (var i = 0; i < items.length; i++) {
					var newTemplate = template.clone(true);	               
	                volumeSnapshotJSONToTemplate(items[i], newTemplate); 
	                $container.append(newTemplate.show());	
				}			
			}	
			$thisTab.find("#tab_spinning_wheel").hide();    
            $thisTab.find("#tab_container").show();    			
		}
	});
} 
 
function volumeClearSnapshotTab() {
    var $thisTab = $("#right_panel_content").find("#tab_content_snapshot");	  	
    $thisTab.find("#tab_container").empty();
} 
 
function volumeSnapshotJSONToTemplate(jsonObj, template) {
    template.data("jsonObj", jsonObj);     
    template.attr("id", "volume_snapshot_"+fromdb(jsonObj.id)).data("volumeSnapshotId", fromdb(jsonObj.id));    
    template.find("#grid_header_title").text(fromdb(jsonObj.name));			   
    template.find("#id").text(fromdb(jsonObj.id));
    template.find("#name").text(fromdb(jsonObj.name));			      
    template.find("#volumename").text(fromdb(jsonObj.volumename));	    
    template.find("#state").text(fromdb(jsonObj.state));       
    template.find("#intervaltype").text(fromdb(jsonObj.intervaltype));	    		   
    template.find("#account").text(fromdb(jsonObj.account));
    template.find("#domain").text(fromdb(jsonObj.domain));    
    setDateField(jsonObj.created, template.find("#created"));	 
	
	var $actionLink = template.find("#action_link");		
	bindActionLink($actionLink);
	
	var $actionMenu = $actionLink.find("#action_menu");
    $actionMenu.find("#action_list").empty();	
    
    if(jsonObj.state == "BackedUp") {
	    buildActionLinkForSubgridItem("label.action.create.volume", volumeSnapshotActionMap, $actionMenu, template);		    
	    buildActionLinkForSubgridItem("label.action.create.template", volumeSnapshotActionMap, $actionMenu, template);	
    }
    buildActionLinkForSubgridItem("label.action.delete.snapshot", volumeSnapshotActionMap, $actionMenu, template);	
} 
 
function volumeClearRightPanel() {       
    volumeJsonClearDetailsTab();   
} 
  
function volumeJsonClearDetailsTab(){   
    var $thisTab = $("#right_panel_content").find("#tab_content_details");  
    $thisTab.find("#id").text("");
    $thisTab.find("#name").text("");    
    $thisTab.find("#zonename").text("");    
    $thisTab.find("#device_id").text("");   
    $thisTab.find("#state").text("");    
    $thisTab.find("#storage").text("");
    $thisTab.find("#account").text(""); 
    $thisTab.find("#type").text("");
    $thisTab.find("#size").text("");		
    $thisTab.find("#vm_name").text("");
    $thisTab.find("#created").text("");
    $thisTab.find("#domain").text("");
}
   
var volumeActionMap = {  
    "label.action.attach.disk": {
        isAsyncJob: true,
        asyncJobResponse: "attachvolumeresponse",            
        dialogBeforeActionFn : doAttachDisk,
        inProcessText: "label.action.attach.disk.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {                
            var jsonObj = json.queryasyncjobresultresponse.jobresult.volume;  
            volumeToMidmenu(jsonObj, $midmenuItem1);            
        }
    },
    "label.action.detach.disk": {
        api: "detachVolume",            
        isAsyncJob: true,
        asyncJobResponse: "detachvolumeresponse",
        inProcessText: "label.action.detach.disk.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id){   
            var jsonObj = json.queryasyncjobresultresponse.jobresult.volume;     
            volumeToMidmenu(jsonObj,  $midmenuItem1);            
        }
    },
    "label.action.create.template": {
        isAsyncJob: true,
        asyncJobResponse: "createtemplateresponse",            
        dialogBeforeActionFn : doCreateTemplateFromVolume,
        inProcessText: "label.action.create.template.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {}   
    },
    "label.action.delete.volume": {
        api: "deleteVolume",            
        isAsyncJob: false,  
        dialogBeforeActionFn : doDeleteVolume,      
        inProcessText: "label.action.delete.volume.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {  
    	    $midmenuItem1.remove();
            if(id.toString() == $("#right_panel_content").find("#tab_content_details").find("#id").text()) {
                clearRightPanel();
                volumeClearRightPanel();
            }                      
        }
    },
    "label.action.take.snapshot": {
        isAsyncJob: true,
        asyncJobResponse: "createsnapshotresponse",            
        dialogBeforeActionFn : doTakeSnapshot,
        inProcessText: "label.action.take.snapshot.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {}   
    },
    "label.action.recurring.snapshot": {                 
        dialogBeforeActionFn : doRecurringSnapshot 
    },
    "label.action.download.volume": {               
        dialogBeforeActionFn : doDownloadVolume        
    }        
}   

function doDownloadVolume($actionLink, $detailsTab, $midmenuItem1) { 
	var jsonObj = $midmenuItem1.data("jsonObj");
	var id = jsonObj.id;
	var zoneId = jsonObj.zoneid;							
	
    var apiCommand = "command=extractVolume&id="+id+"&zoneid="+zoneId+"&mode=HTTP_DOWNLOAD";
   
    var $dialogDownloadVolume = $("#dialog_download_volume");
    $spinningWheel = $dialogDownloadVolume.find("#spinning_wheel");
    $spinningWheel.show();
    var $infoContainer = $dialogDownloadVolume.find("#info_container");
    $infoContainer.hide();	
   
    $dialogDownloadVolume
	.dialog('option', 'buttons', {	
	    "Close": function() {				        
		    $(this).dialog("close");
	    }				
	}).dialog("open");	
			  
    $.ajax({
        data: createURL(apiCommand),
        dataType: "json",           
        success: function(json) {	                           	                        
            var jobId = json.extractvolumeresponse.jobid;                  			                        
            var timerKey = "asyncJob_" + jobId;					                       
            $("body").everyTime(
                2000,  //this API returns fast. So, set 2 seconds instead of 10 seconds.
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
		                            $infoContainer.removeClass("error");
		                            $infoContainer.find("#icon,#info").removeClass("error");		                      
		                            var url = decodeURIComponent(json.queryasyncjobresultresponse.jobresult.volume.url);		                          
		                            var htmlMsg = dictionary["message.download.volume"];		                            
		                            var htmlMsg2 = htmlMsg.replace(/#/, url).replace(/00000/, url);                        
		                            $infoContainer.find("#info").html(htmlMsg2);		                            
		                            $infoContainer.show();		                        
		                        } else if (result.jobstatus == 2) { // Failed	
		                            //var errorMsg = g_dictionary["label.failed"] + " - " + g_dictionary["label.error.code"] + " " + fromdb(result.jobresult.errorcode);
			                        var errorMsg = g_dictionary["label.failed"] + " - " + fromdb(result.jobresult.errortext);
			                        handleErrorInDialog2(errorMsg, $dialogDownloadVolume);		                        
		                        }											                    
	                        }
                        },
                        error: function(XMLHttpResponse) {	                            
	                        $("body").stopTime(timerKey);	
							handleError(XMLHttpResponse, function() {
							    handleErrorInDialog(XMLHttpResponse, $dialogDownloadVolume);									
							});
                        }
                    });
                },
                0
            );
        },
        error: function(XMLHttpResponse) {
			handleError(XMLHttpResponse, function() {
				handleErrorInDialog(XMLHttpResponse, $dialogDownloadVolume);			
			});
        }
    });  	
}

function doCreateTemplateFromVolume($actionLink, $detailsTab, $midmenuItem1) {  
	if (getUserPublicTemplateEnabled() == "true" || isAdmin()) {
		$("#dialog_create_template #create_template_public_container").show();
	}
   
    var jsonObj = $midmenuItem1.data("jsonObj");
    $("#dialog_create_template").find("#volume_name").text(jsonObj.name);
    
	$("#dialog_create_template")
	.dialog('option', 'buttons', { 						
		"Create": function() { 		   
		    var thisDialog = $(this);
		    thisDialog.dialog("close"); 
									
			// validate values
	        var isValid = true;					
	        isValid &= validateString("Name", thisDialog.find("#create_template_name"), thisDialog.find("#create_template_name_errormsg"));
			isValid &= validateString("Display Text", thisDialog.find("#create_template_desc"), thisDialog.find("#create_template_desc_errormsg"));			
	        if (!isValid) return;		
	        
	        var name = trim(thisDialog.find("#create_template_name").val());
			var desc = trim(thisDialog.find("#create_template_desc").val());
			var osType = thisDialog.find("#create_template_os_type").val();					
			var isPublic = thisDialog.find("#create_template_public").val();
            var password = thisDialog.find("#create_template_password").val();				
			
			var id = $midmenuItem1.data("jsonObj").id;			
			var apiCommand = "command=createTemplate&volumeId="+id+"&name="+todb(name)+"&displayText="+todb(desc)+"&osTypeId="+osType+"&isPublic="+isPublic+"&passwordEnabled="+password;
	    	doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);					
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		} 
	}).dialog("open");
}   

function doDeleteVolume($actionLink, $detailsTab, $midmenuItem1) {       
    var jsonObj = $midmenuItem1.data("jsonObj");
	var id = jsonObj.id;
		
	$("#dialog_confirmation")
	.text(dictionary["message.action.delete.volume"])
	.dialog('option', 'buttons', { 					
		"Confirm": function() { 			
			$(this).dialog("close");			
			var apiCommand = "command=deleteVolume&id="+id;
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		}
	}).dialog("open");
}

function doTakeSnapshot($actionLink, $detailsTab, $midmenuItem1) {      
    $("#dialog_confirmation")
    .text(dictionary["message.action.take.snapshot"])				
    .dialog('option', 'buttons', { 					    
	    "Confirm": function() { 	
	        $(this).dialog("close");	
	    	
            var id = $midmenuItem1.data("jsonObj").id;	
			var apiCommand = "command=createSnapshot&volumeid="+id;
	    	doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	
	    },
	    "Cancel": function() { 					        
		    $(this).dialog("close"); 
	    } 
    }).dialog("open");	  
}		
	
function clearTopPanel(target) { // "target == null" means target at all (hourly + daily + weekly + monthly)
    var dialogBox = $("#dialog_recurring_snapshot");
    if(target == "hourly" || target == null) {
        dialogBox.find("#dialog_snapshot_hourly_info_unset").show();
	    dialogBox.find("#dialog_snapshot_hourly_info_set").hide();   
	    dialogBox.find("#read_hourly_max, #read_hourly_minute").text("N/A"); 	                  
        dialogBox.find("#hourly_edit_link, #hourly_delete_link").data("intervalType", "hourly").data("max", "").data("timezone", (g_timezone==null)?"Etc/GMT+12":g_timezone).data("minute", "00"); 
    }                
    if(target == "daily" || target == null) {   
        dialogBox.find("#dialog_snapshot_daily_info_unset").show();
	    dialogBox.find("#dialog_snapshot_daily_info_set").hide();
	    dialogBox.find("#read_daily_max, #read_daily_minute, #read_daily_hour, #read_daily_meridiem").text("N/A");  
        dialogBox.find("#daily_edit_link, #daily_delete_link").data("intervalType", "daily").data("max", "").data("timezone", (g_timezone==null)?"Etc/GMT+12":g_timezone).data("minute", "00").data("hour12", "00").data("meridiem", "AM");                                   
    }                
    if(target == "weekly" || target == null) {    
        dialogBox.find("#dialog_snapshot_weekly_info_unset").show();
	    dialogBox.find("#dialog_snapshot_weekly_info_set").hide();
	    dialogBox.find("#read_weekly_max, #read_weekly_minute, #read_weekly_hour, #read_weekly_meridiem, #read_weekly_day_of_week").text("N/A");     
        dialogBox.find("#weekly_edit_link, #weekly_delete_link").data("intervalType", "weekly").data("max", "").data("timezone", (g_timezone==null)?"Etc/GMT+12":g_timezone).data("minute", "00").data("hour12", "00").data("meridiem", "AM").data("dayOfWeek", "1");     
    }                
    if(target == "monthly" || target == null) {    
        dialogBox.find("#dialog_snapshot_monthly_info_unset").show();
	    dialogBox.find("#dialog_snapshot_monthly_info_set").hide();
	    dialogBox.find("#read_monthly_max, #read_monthly_minute, #read_monthly_hour, #read_monthly_meridiem, #read_monthly_day_of_month").text("N/A");  
        dialogBox.find("#monthly_edit_link, #monthly_delete_link").data("intervalType", "monthly").data("max", "").data("timezone", (g_timezone==null)?"Etc/GMT+12":g_timezone).data("minute", "00").data("hour12", "00").data("meridiem", "AM").data("dayOfMonth", "1");                                                                
    }
}

function clearBottomPanel() {	
    var dialogBox = $("#dialog_recurring_snapshot");
		    
    dialogBox.find("#edit_hour").val("00");
    cleanErrMsg(dialogBox.find("#edit_hour"), dialogBox.find("#edit_time_errormsg"));
    
    dialogBox.find("#edit_minute").val("00");
    cleanErrMsg(dialogBox.find("#edit_minute"), dialogBox.find("#edit_time_errormsg"));
    
    dialogBox.find("#edit_meridiem").val("AM");
    		        
    dialogBox.find("#edit_max").val("");	
    cleanErrMsg(dialogBox.find("#edit_max"), dialogBox.find("#edit_max_errormsg"));
    
    dialogBox.find("#edit_timezone").val((g_timezone==null)?"Etc/GMT+12":g_timezone); 
    cleanErrMsg(dialogBox.find("#edit_timezone"), dialogBox.find("#edit_timezone_errormsg"));
    	        
    dialogBox.find("#edit_day_of_week").val("1");
    cleanErrMsg(dialogBox.find("#edit_day_of_week"), dialogBox.find("#edit_day_of_week_errormsg"));
    
    dialogBox.find("#edit_day_of_month").val("1");
    cleanErrMsg(dialogBox.find("#edit_day_of_month"), dialogBox.find("#edit_day_of_month_errormsg"));
}	   
	
function doRecurringSnapshot($actionLink, $detailsTab, $midmenuItem1) {     
	var volumeId = $midmenuItem1.data("jsonObj").id;
	
	var dialogBox = $("#dialog_recurring_snapshot"); 
	clearTopPanel();
	
	$.ajax({
        data: createURL("command=listSnapshotPolicies&volumeid="+volumeId),
        dataType: "json",
        async: false,
        success: function(json) {								
            var items = json.listsnapshotpoliciesresponse.snapshotpolicy;
			var $snapInterval = dialogBox.find("#snapshot_interval");            
			$snapInterval.find("#snapshot_interval_0,#snapshot_interval_1,#snapshot_interval_2,#snapshot_interval_3").data("jsonObj", null);						
			if(items!=null && items.length>0) {
				for (var i = 0; i < items.length; i++) {
					var item = items[i];
					$snapInterval.find("#snapshot_interval_"+item.intervaltype).data("jsonObj", item);
				}
            } 
			clearBottomPanel();
			$snapInterval.val("0"); //default to hourly
			$snapInterval.change();
			
			dialogBox.dialog('option', 'buttons', { 
				"Apply": function() {
					var thisDialog = $(this);		   
					var volumeId = thisDialog.data("volumeId");
					var bottomPanel = thisDialog.find("#dialog_snapshotright");
				
					var intervalType = thisDialog.find("#snapshot_interval").val();
					var minute, hour12, hour24, meridiem, dayOfWeek, dayOfWeekString, dayOfMonth, schedule, max, timezone;   			                   
					switch(intervalType) {
						 case "0":
							 var isValid = true;	 
							 isValid &= validateInteger("Keep # of snapshots", bottomPanel.find("#edit_max"), bottomPanel.find("#edit_max_errormsg"));	    	
							 if (!isValid) return;
							 intervalType = "hourly";
							 minute = bottomPanel.find("#edit_minute").val();		                     
							 schedule = minute;		                    
							 max = bottomPanel.find("#edit_max").val();	
							 timezone = bottomPanel.find("#edit_timezone").val();			                                                      
							 break;
							 
						 case "1":
							 var isValid = true;	 
							 isValid &= validateInteger("Keep # of snapshots", bottomPanel.find("#edit_max"), bottomPanel.find("#edit_max_errormsg"));	    	
							 if (!isValid) return;
							 intervalType = "daily";
							 minute = bottomPanel.find("#edit_minute").val();		
							 hour12 = bottomPanel.find("#edit_hour").val();
							 meridiem = bottomPanel.find("#edit_meridiem").val();			                    
							 if(meridiem=="AM")	 
								 hour24 = hour12;
							 else //meridiem=="PM"	 
								 hour24 = (parseInt(hour12,10)+12).toString();  //specify number base to be 10. Otherwise, "08" and "09" will be treated as octal numbers (base 8).              
							 schedule = minute + ":" + hour24;		                    
							 max = bottomPanel.find("#edit_max").val();	
							 timezone = bottomPanel.find("#edit_timezone").val();		
							 break;
							 
						 case "2":
							 var isValid = true;	 
							 isValid &= validateInteger("Keep # of snapshots", bottomPanel.find("#edit_max"), bottomPanel.find("#edit_max_errormsg"));	    	
							 if (!isValid) return;
							 intervalType = "weekly";
							 minute = bottomPanel.find("#edit_minute").val();		
							 hour12 = bottomPanel.find("#edit_hour").val();
							 meridiem = bottomPanel.find("#edit_meridiem").val();			                    
							 if(meridiem=="AM")	 
								 hour24 = hour12;
							 else //meridiem=="PM"	 
								 hour24 = (parseInt(hour12,10)+12).toString();  //specify number base to be 10. Otherwise, "08" and "09" will be treated as octal numbers (base 8).  
							 dayOfWeek = bottomPanel.find("#edit_day_of_week").val();  
							 dayOfWeekString = bottomPanel.find("#edit_day_of_week option:selected").text();
							 schedule = minute + ":" + hour24 + ":" + dayOfWeek;		                    
							 max = bottomPanel.find("#edit_max").val();	
							 timezone = bottomPanel.find("#edit_timezone").val();	
							 break;
							 
						 case "3":
							 var isValid = true;	 
							 isValid &= validateInteger("Keep # of snapshots", bottomPanel.find("#edit_max"), bottomPanel.find("#edit_max_errormsg"));	    	
							 if (!isValid) return;
							 intervalType = "monthly";
							 minute = bottomPanel.find("#edit_minute").val();		
							 hour12 = bottomPanel.find("#edit_hour").val();
							 meridiem = bottomPanel.find("#edit_meridiem").val();			                    
							 if(meridiem=="AM")	 
								 hour24 = hour12;
							 else //meridiem=="PM"	 
								 hour24 = (parseInt(hour12,10)+12).toString();  //specify number base to be 10. Otherwise, "08" and "09" will be treated as octal numbers (base 8).   
							 dayOfMonth = bottomPanel.find("#edit_day_of_month").val();  		                     
							 schedule = minute + ":" + hour24 + ":" + dayOfMonth;		                    
							 max = bottomPanel.find("#edit_max").val();	
							 timezone = bottomPanel.find("#edit_timezone").val();			                    
							 break;		                
					}	
					var thisLink;
					var $snapshotInterval = thisDialog.find("#snapshot_interval");
					var $snapshotIntervalOption = thisDialog.find("#snapshot_interval_"+$snapshotInterval.val());
					$.ajax({
						data: createURL("command=createSnapshotPolicy&intervaltype="+intervalType+"&schedule="+schedule+"&volumeid="+volumeId+"&maxsnaps="+max+"&timezone="+todb(timezone)),
						dataType: "json",                        
						success: function(json) {	
							$snapshotIntervalOption.data("jsonObj", json.createsnapshotpolicyresponse.snapshotpolicy);
							$snapshotInterval.change();
							thisDialog.find("#info").text(dictionary["message.apply.snapshot.policy"]);
							thisDialog.find("#info_container").show();
						},
						error: function(XMLHttpResponse) {                            					
							handleError(XMLHttpResponse);					
						}
					});	 
				},
				"Disable": function() {
					var thisDialog = $(this);
					var $snapshotInterval = thisDialog.find("#snapshot_interval");
					var $snapshotIntervalOption = thisDialog.find("#snapshot_interval_"+$snapshotInterval.val());
					var jsonObj = $snapshotIntervalOption.data("jsonObj");                 
					if(jsonObj != null) {
						$.ajax({
							data: createURL("command=deleteSnapshotPolicies&id="+jsonObj.id),
							dataType: "json",                        
							success: function(json) {      
								$snapshotIntervalOption.data("jsonObj", null);
								$snapshotInterval.change();
								thisDialog.find("#info").text(dictionary["message.disable.snapshot.policy"]);
								thisDialog.find("#info_container").show();
							},
							error: function(XMLHttpResponse) {                                                   					
								handleError(XMLHttpResponse);					
							}
						});	 
					}
				},
				"Close": function() { 
					$(this).dialog("close"); 
				}
			}).dialog("open").data("volumeId", volumeId);
        },
        error: function(XMLHttpResponse) {			                   					
            handleError(XMLHttpResponse);					
        }
    });   	    
}	

function populateVirtualMachineField(domainId, account, zoneId) {        
    $.ajax({
	    cache: false,
	    data: createURL("command=listVirtualMachines&state=Running&zoneid="+zoneId+"&domainid="+domainId+"&account="+account),
	    dataType: "json",
	    success: function(json) {			    
		    var instances = json.listvirtualmachinesresponse.virtualmachine;				
		    var volumeVmSelect = $("#dialog_attach_volume").find("#volume_vm").empty();					
		    if (instances != null && instances.length > 0) {
			    for (var i = 0; i < instances.length; i++) {
				    volumeVmSelect.append("<option value='" + instances[i].id + "'>" + getVmName(instances[i].name, instances[i].displayname) + "</option>"); 
			    }				    
		    }
			$.ajax({
				cache: false,
				data: createURL("command=listVirtualMachines&state=Stopped&zoneid="+zoneId+"&domainid="+domainId+"&account="+account),
				dataType: "json",
				success: function(json) {			    
					var instances = json.listvirtualmachinesresponse.virtualmachine;								
					if (instances != null && instances.length > 0) {
						for (var i = 0; i < instances.length; i++) {
							volumeVmSelect.append("<option value='" + instances[i].id + "'>" + getVmName(instances[i].name, instances[i].displayname) + "</option>");
						}				    
					}
				}
			});
	    }
    });
}		

function doAttachDisk($actionLink, $detailsTab, $midmenuItem1) {       
    var jsonObj = $midmenuItem1.data("jsonObj");    
    populateVirtualMachineField(jsonObj.domainid, jsonObj.account, jsonObj.zoneid);
	    
    $("#dialog_attach_volume")					
    .dialog('option', 'buttons', { 					    
	    "OK": function() { 	
	        var $thisDialog = $(this);
		    				
			var isValid = true;				
			isValid &= validateDropDownBox("Virtual Machine", $thisDialog.find("#volume_vm"), $thisDialog.find("#volume_vm_errormsg"));	
			if (!isValid) 
			    return;
			    
			$thisDialog.dialog("close");	     
	        
	        var virtualMachineId = $thisDialog.find("#volume_vm").val();		
	        	    	
	    	var id = jsonObj.id;			
			var apiCommand = "command=attachVolume&id="+id+'&virtualMachineId='+virtualMachineId;
	    	doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
	    }, 
	    "Cancel": function() { 					        
		    $(this).dialog("close"); 
	    } 
    }).dialog("open");
}	

//Snapshot tab actions
var volumeSnapshotActionMap = {  
    "label.action.create.volume": {              
        isAsyncJob: true,
        asyncJobResponse: "createvolumeresponse",
        dialogBeforeActionFn : doCreateVolumeFromSnapshotInVolumePage,
        inProcessText: "label.action.create.volume.processing",
        afterActionSeccessFn: function(json, id, $subgridItem) {   
            var $midmenuItem1 = $("#midmenu_item").clone();		        
            var item = json.queryasyncjobresultresponse.jobresult.volume;		   
			volumeToMidmenu(item, $midmenuItem1);
			bindClickToMidMenu($midmenuItem1, volumeToRightPanel, getMidmenuId);  						                    
			$midmenuItem1.find("#info_icon").removeClass("error").show();
	        $midmenuItem1.data("afterActionInfo", ("Creating volume from snapshot succeeded.")); 	
            $("#midmenu_container").append($midmenuItem1.fadeIn("slow"));	           
        }
    }   
    , 
    "label.action.delete.snapshot": {              
        api: "deleteSnapshot",     
        isAsyncJob: true,
        asyncJobResponse: "deletesnapshotresponse",
		dialogBeforeActionFn : doSnapshotDelete,
        inProcessText: "label.action.delete.snapshot.processing",
        afterActionSeccessFn: function(json, id, $subgridItem) {                 
            $subgridItem.slideUp("slow", function() {
                $(this).remove();
            });
        }
    } 
    ,
    "label.action.create.template": {              
        isAsyncJob: true,
        asyncJobResponse: "createtemplateresponse",
        dialogBeforeActionFn : doCreateTemplateFromSnapshotInVolumePage,
        inProcessText: "label.action.create.template.processing",
        afterActionSeccessFn: function(json, id, $subgridItem) {}            
    }
}  

function doSnapshotDelete($actionLink, $subgridItem) {
	$("#dialog_confirmation")	
	.text(dictionary["message.action.delete.snapshot"])
    .dialog('option', 'buttons', { 						
	    "Confirm": function() { 
		    $(this).dialog("close"); 	
			var id = $subgridItem.data("jsonObj").id;
			var apiCommand = "command=deleteSnapshot&id="+id;                      
            doActionToSubgridItem(id, $actionLink, apiCommand, $subgridItem); 
	    }, 
	    "Cancel": function() { 
		    $(this).dialog("close"); 
			
	    } 
    }).dialog("open");
}
                                              
function doCreateVolumeFromSnapshotInVolumePage($actionLink, $subgridItem) { 
    var jsonObj = $subgridItem.data("jsonObj");
       
    $("#dialog_add_volume_from_snapshot")
    .dialog("option", "buttons", {	                    
     "Add": function() {	
         var thisDialog = $(this);	 
                                        
         var isValid = true;					
         isValid &= validateString("Name", thisDialog.find("#name"), thisDialog.find("#name_errormsg"));					          		
         if (!isValid) return;   
         
         thisDialog.dialog("close");       	                                             
         
         var name = thisDialog.find("#name").val();	                
         
         var id = jsonObj.id;
         var apiCommand = "command=createVolume&snapshotid="+id+"&name="+fromdb(name);    	
    	 doActionToSubgridItem(id, $actionLink, apiCommand, $subgridItem);			
     },
     "Cancel": function() {	                         
         $(this).dialog("close");
     }
    }).dialog("open");     
}

function doCreateTemplateFromSnapshotInVolumePage($actionLink, $subgridItem) { 
	if (getUserPublicTemplateEnabled() == "true" || isAdmin()) {
		$("#dialog_create_template_from_snapshot #create_template_public_container").show();
	}
	
    var jsonObj = $subgridItem.data("jsonObj");
       
    $("#dialog_create_template_from_snapshot")
    .dialog("option", "buttons", {
     "Add": function() {	
         var thisDialog = $(this);	 	                                                                        
         var isValid = true;					
         isValid &= validateString("Name", thisDialog.find("#name"), thisDialog.find("#name_errormsg"), false);		
         isValid &= validateString("Display Text", thisDialog.find("#display_text"), thisDialog.find("#display_text_errormsg"), false);				         		          		
         if (!isValid) 
             return;                  	                                             
         
         thisDialog.dialog("close");	
         
         var array1 = [];
         var name = thisDialog.find("#name").val();	 
         array1.push("&name="+todb(name));
         
         var displayText = thisDialog.find("#display_text").val();	 
         array1.push("&displaytext="+todb(displayText));
         
         var osTypeId = thisDialog.find("#os_type").val(); 	  
         array1.push("&ostypeid="+osTypeId);
         
         var isPublic = thisDialog.find("#ispublic").val();
         array1.push("&isPublic="+isPublic);
         
         var password = thisDialog.find("#password").val();	
         array1.push("&passwordEnabled="+password);                                         
       
         if(thisDialog.find("#isfeatured_container").css("display")!="none") {				
		     var isFeatured = thisDialog.find("#isfeatured").val();						    	
             array1.push("&isfeatured="+isFeatured);
         }	
       
         var id = jsonObj.id;
         var apiCommand = "command=createTemplate&snapshotid="+id+array1.join("");    	 
    	 doActionToSubgridItem(id, $actionLink, apiCommand, $subgridItem);				
     },
     "Cancel": function() {	                         
         $(this).dialog("close");
     }	                     
    }).dialog("open");	     
}
