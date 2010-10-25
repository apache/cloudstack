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

function afterLoadVolumeJSP() {
    activateDialog($("#dialog_create_template").dialog({
        width: 400,
        autoOpen: false,
        modal: true,
        zIndex: 2000
    }));
    activateDialog($("#dialog_create_snapshot").dialog({ 
	    autoOpen: false,
	    modal: true,
	    zIndex: 2000
    }));
	activateDialog($("#dialog_recurring_snapshot").dialog({ 
	    width: 735,
	    autoOpen: false,
	    modal: true,
	    zIndex: 2000
    }));
	activateDialog($("#dialog_add_volume").dialog({ 
	    autoOpen: false,
	    modal: true,
	    zIndex: 2000
    }));	
	activateDialog($("#dialog_attach_volume").dialog({ 
	    autoOpen: false,
	    modal: true,
	    zIndex: 2000
    }));	
	activateDialog($("#dialog_add_volume_from_snapshot").dialog({ 
	    autoOpen: false,
	    modal: true,
	    zIndex: 2000
    }));
    activateDialog($("#dialog_create_template_from_snapshot").dialog({ 
        width: 400,
        autoOpen: false,
        modal: true,
        zIndex: 2000
    }));    
	        
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
        data: createURL("command=listZones&available=true"+maxPageSize),
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
		        if (offerings != null && offerings.length > 0) {
		            for (var i = 0; i < offerings.length; i++) 				
			            volumeDiskOfferingSelect.append("<option value='" + offerings[i].id + "'>" + fromdb(offerings[i].displaytext) + "</option>"); 		
			    }	
			}	
	    }
    });	   
      
    //add button ***
    $("#midmenu_add_link").find("#label").text("Add Volume"); 
    $("#midmenu_add_link").show();     
    $("#midmenu_add_link").unbind("click").bind("click", function(event) {   
        $("#dialog_add_volume")
	    .dialog('option', 'buttons', { 			    
		    "Add": function() { 
		        var thisDialog = $(this);
		    			            										
		        // validate values							
			    var isValid = true;									
			    isValid &= validateString("Name", thisDialog.find("#add_volume_name"), thisDialog.find("#add_volume_name_errormsg"));					
			    if (!isValid) return;
			    
			    thisDialog.dialog("close");		
				
				var name = trim(thisDialog.find("#add_volume_name").val());					
			    var zoneId = thisDialog.find("#volume_zone").val();					    				
			    var diskofferingId = thisDialog.find("#volume_diskoffering").val();	
				
				var $midmenuItem1 = beforeAddingMidMenuItem() ;
				    					
			    $.ajax({
				    data: createURL("command=createVolume&zoneId="+zoneId+"&name="+todb(name)+"&diskOfferingId="+diskofferingId+"&accountId="+"1"), 
				    dataType: "json",
				    success: function(json) {						        
				        var jobId = json.createvolumeresponse.jobid;				        
				        var timerKey = "createVolumeJob_"+jobId;
							    
				        $("body").everyTime(2000, timerKey, function() {
						    $.ajax({
							    data: createURL("command=queryAsyncJobResult&jobId="+json.createvolumeresponse.jobid),
							    dataType: "json",
							    success: function(json) {										       						   
								    var result = json.queryasyncjobresultresponse;
								    if (result.jobstatus == 0) {
									    return; //Job has not completed
								    } else {											    
									    $("body").stopTime(timerKey);
									    if (result.jobstatus == 1) {
										    // Succeeded										   
										    volumeToMidmenu(result.volume[0], $midmenuItem1);
						                    bindClickToMidMenu($midmenuItem1, volumeToRightPanel, getMidmenuId);  
						                    afterAddingMidMenuItem($midmenuItem1, true);	         
									    } else if (result.jobstatus == 2) {
									        handleAsyncJobFailInMidMenu(result.jobresult, $midmenuItem1);											   				    
									    }
								    }
							    },
							    error: function(XMLHttpResponse) {
								    $("body").stopTime(timerKey);
								    handleErrorInMidMenu(XMLHttpResponse, $midmenuItem1);	
							    }
						    });
					    }, 0);						    					
				    },
				    error: function(XMLHttpResponse) {							    
						handleErrorInMidMenu(XMLHttpResponse, $midmenuItem1);	
				    }
			    });
		    }, 
		    "Cancel": function() { 				        				        
			    $(this).dialog("close"); 
		    } 
	    }).dialog("open");
	    
        return false;
    });  
       
    // *** recurring snapshot dialog - event binding (begin) ******************************		
	$("#dialog_recurring_snapshot").bind("click", function(event) {		
	    event.preventDefault();
	    event.stopPropagation();
	    
	    var target = event.target;
	    var targetId = target.id;
	    var thisDialog = $(this);		   
	    var volumeId = thisDialog.data("volumeId");
	    var topPanel = thisDialog.find("#dialog_snapshotleft");
		var bottomPanel = thisDialog.find("#dialog_snapshotright");
			    
	    if(targetId.indexOf("_edit_link")!=-1) {
			clearBottomPanel();						
			bottomPanel.animate({
				height: 200
				}, 1000, function() {}
		    );	
	    }	
	    else if(targetId.indexOf("_delete_link")!=-1) {  		       
	        clearBottomPanel();
	        var snapshotPolicyId = $("#"+targetId).data("snapshotPolicyId");			                 
	        if(snapshotPolicyId == null || snapshotPolicyId.length==0)
	            return;
            $.ajax({
	            data: createURL("command=deleteSnapshotPolicies&id="+snapshotPolicyId),
                dataType: "json",                        
                success: function(json) {                              
                    clearTopPanel($("#"+targetId).data("intervalType"));                        
                },
                error: function(XMLHttpResponse) {                                                   					
                    handleError(XMLHttpResponse);					
                }
            });	              
	    }
	    
	    var thisLink;
	    switch(targetId) {
	        case "hourly_edit_link": 
	            $("#edit_interval_type").text("Hourly");
	            $("#edit_time_colon, #edit_hour_container, #edit_meridiem_container, #edit_day_of_week_container, #edit_day_of_month_container").hide(); 
	            $("#edit_past_the_hour, #edit_minute_container").show();		            	
	            thisLink = thisDialog.find("#hourly_edit_link");           
	            thisDialog.find("#edit_minute").val(thisLink.data("minute"));            
	            thisDialog.find("#edit_max").val(thisLink.data("max")); 
	            thisDialog.find("#edit_timezone").val(thisLink.data("timezone")); 
	            break;
	        case "daily_edit_link":
	            $("#edit_interval_type").text("Daily");
	            $("#edit_past_the_hour, #edit_day_of_week_container, #edit_day_of_month_container").hide(); 
	            $("#edit_minute_container, #edit_hour_container, #edit_meridiem_container").show();		           
	            thisLink = thisDialog.find("#daily_edit_link");           
	            thisDialog.find("#edit_minute").val(thisLink.data("minute"));
	            thisDialog.find("#edit_hour").val(thisLink.data("hour12")); 
	            thisDialog.find("#edit_meridiem").val(thisLink.data("meridiem"));          
	            thisDialog.find("#edit_max").val(thisLink.data("max")); 
	            thisDialog.find("#edit_timezone").val(thisLink.data("timezone")); 
	            break;
	        case "weekly_edit_link":
	            $("#edit_interval_type").text("Weekly");
	            $("#edit_past_the_hour, #edit_day_of_month_container").hide(); 
	            $("#edit_minute_container, #edit_hour_container, #edit_meridiem_container, #edit_day_of_week_container").show();		           
	            thisLink = thisDialog.find("#weekly_edit_link");           
	            thisDialog.find("#edit_minute").val(thisLink.data("minute"));
	            thisDialog.find("#edit_hour").val(thisLink.data("hour12")); 
	            thisDialog.find("#edit_meridiem").val(thisLink.data("meridiem")); 	
	            thisDialog.find("#edit_day_of_week").val(thisLink.data("dayOfWeek"));         
	            thisDialog.find("#edit_max").val(thisLink.data("max")); 
	            thisDialog.find("#edit_timezone").val(thisLink.data("timezone")); 
	            break;
	        case "monthly_edit_link":
	            $("#edit_interval_type").text("Monthly");
	            $("#edit_past_the_hour, #edit_day_of_week_container").hide(); 
	            $("#edit_minute_container, #edit_hour_container, #edit_meridiem_container, #edit_day_of_month_container").show();		           
	            thisLink = thisDialog.find("#monthly_edit_link");           
	            thisDialog.find("#edit_minute").val(thisLink.data("minute"));
	            thisDialog.find("#edit_hour").val(thisLink.data("hour12")); 
	            thisDialog.find("#edit_meridiem").val(thisLink.data("meridiem")); 	
	            thisDialog.find("#edit_day_of_month").val(thisLink.data("dayOfMonth"));         
	            thisDialog.find("#edit_max").val(thisLink.data("max")); 
	            thisDialog.find("#edit_timezone").val(thisLink.data("timezone")); 
	            break;  
	        case "apply_button":		            
	            var intervalType = bottomPanel.find("#edit_interval_type").text().toLowerCase();
	            var minute, hour12, hour24, meridiem, dayOfWeek, dayOfWeekString, dayOfMonth, schedule, max, timezone;   			                   
	            switch(intervalType) {
	                 case "hourly":
	                     var isValid = true;	 
	                     isValid &= validateNumber("Keep # of snapshots", bottomPanel.find("#edit_max"), bottomPanel.find("#edit_max_errormsg"));	    	
				         if (!isValid) return;
	                 
	                     minute = bottomPanel.find("#edit_minute").val();		                     
	                     schedule = minute;		                    
	                     max = bottomPanel.find("#edit_max").val();	
	                     timezone = bottomPanel.find("#edit_timezone").val();			                                                      
	                     break;
	                     
	                 case "daily":
	                     var isValid = true;	 
	                     isValid &= validateNumber("Keep # of snapshots", bottomPanel.find("#edit_max"), bottomPanel.find("#edit_max_errormsg"));	    	
				         if (!isValid) return;
	                     
	                     minute = bottomPanel.find("#edit_minute").val();		
	                     hour12 = bottomPanel.find("#edit_hour").val();
	                     meridiem = bottomPanel.find("#edit_meridiem").val();			                    
	                     if(meridiem=="AM")	 
	                         hour24 = hour12;
	                     else //meridiem=="PM"	 
	                         hour24 = (parseInt(hour12)+12).toString();                
	                     schedule = minute + ":" + hour24;		                    
	                     max = bottomPanel.find("#edit_max").val();	
	                     timezone = bottomPanel.find("#edit_timezone").val();		
	                     break;
	                     
	                 case "weekly":
	                     var isValid = true;	 
	                     isValid &= validateNumber("Keep # of snapshots", bottomPanel.find("#edit_max"), bottomPanel.find("#edit_max_errormsg"));	    	
				         if (!isValid) return;
	                 
	                     minute = bottomPanel.find("#edit_minute").val();		
	                     hour12 = bottomPanel.find("#edit_hour").val();
	                     meridiem = bottomPanel.find("#edit_meridiem").val();			                    
	                     if(meridiem=="AM")	 
	                         hour24 = hour12;
	                     else //meridiem=="PM"	 
	                         hour24 = (parseInt(hour12)+12).toString();    
	                     dayOfWeek = bottomPanel.find("#edit_day_of_week").val();  
	                     dayOfWeekString = bottomPanel.find("#edit_day_of_week option:selected").text();
	                     schedule = minute + ":" + hour24 + ":" + dayOfWeek;		                    
	                     max = bottomPanel.find("#edit_max").val();	
	                     timezone = bottomPanel.find("#edit_timezone").val();	
	                     break;
	                     
	                 case "monthly":
	                     var isValid = true;	 
	                     isValid &= validateNumber("Keep # of snapshots", bottomPanel.find("#edit_max"), bottomPanel.find("#edit_max_errormsg"));	    	
				         if (!isValid) return;
				         
	                     minute = bottomPanel.find("#edit_minute").val();		
	                     hour12 = bottomPanel.find("#edit_hour").val();
	                     meridiem = bottomPanel.find("#edit_meridiem").val();			                    
	                     if(meridiem=="AM")	 
	                         hour24 = hour12;
	                     else //meridiem=="PM"	 
	                         hour24 = (parseInt(hour12)+12).toString();    
	                     dayOfMonth = bottomPanel.find("#edit_day_of_month").val();  		                     
	                     schedule = minute + ":" + hour24 + ":" + dayOfMonth;		                    
	                     max = bottomPanel.find("#edit_max").val();	
	                     timezone = bottomPanel.find("#edit_timezone").val();			                    
	                     break;		                
	            }	
	            
	            var thisLink;
	            $.ajax({
		            data: createURL("command=createSnapshotPolicy&intervaltype="+intervalType+"&schedule="+schedule+"&volumeid="+volumeId+"&maxsnaps="+max+"&timezone="+encodeURIComponent(timezone)),
                    dataType: "json",                        
                    success: function(json) {	                                                                              
                        switch(intervalType) {
	                        case "hourly":
								topPanel.find("#dialog_snapshot_hourly_info_unset").hide();
								topPanel.find("#dialog_snapshot_hourly_info_set").show();
	                            topPanel.find("#read_hourly_minute").text(minute);
								topPanel.find("#read_hourly_timezone").text("("+timezones[timezone]+")");
                                topPanel.find("#read_hourly_max").text(max);                                                                        
                                topPanel.find("#hourly_edit_link, #hourly_delete_link").data("intervalType", "hourly").data("snapshotPolicyId", json.createsnapshotpolicyresponse.id).data("max",max).data("timezone",timezone).data("minute", minute);                                                                   
	                            break;
	                        case "daily":
								topPanel.find("#dialog_snapshot_daily_info_unset").hide();
								topPanel.find("#dialog_snapshot_daily_info_set").show();
	                            topPanel.find("#read_daily_minute").text(minute);
	                            topPanel.find("#read_daily_hour").text(hour12);
	                            topPanel.find("#read_daily_meridiem").text(meridiem);
								topPanel.find("#read_daily_timezone").text("("+timezones[timezone]+")");
                                topPanel.find("#read_daily_max").text(max);                                                                       
                                topPanel.find("#daily_edit_link, #daily_delete_link").data("intervalType", "daily").data("snapshotPolicyId", json.createsnapshotpolicyresponse.id).data("max",max).data("timezone",timezone).data("minute", minute).data("hour12", hour12).data("meridiem", meridiem);                                 
                                break;
	                        case "weekly":
								topPanel.find("#dialog_snapshot_weekly_info_unset").hide();
								topPanel.find("#dialog_snapshot_weekly_info_set").show();
	                            topPanel.find("#read_weekly_minute").text(minute);
	                            topPanel.find("#read_weekly_hour").text(hour12);
	                            topPanel.find("#read_weekly_meridiem").text(meridiem);
								topPanel.find("#read_weekly_timezone").text("("+timezones[timezone]+")");
	                            topPanel.find("#read_weekly_day_of_week").text(dayOfWeekString);
                                topPanel.find("#read_weekly_max").text(max);	                                                                         
                                topPanel.find("#weekly_edit_link, #weekly_delete_link").data("intervalType", "weekly").data("snapshotPolicyId", json.createsnapshotpolicyresponse.id).data("max",max).data("timezone",timezone).data("minute", minute).data("hour12", hour12).data("meridiem", meridiem).data("dayOfWeek",dayOfWeek);                                       
	                            break;
	                        case "monthly":
								topPanel.find("#dialog_snapshot_monthly_info_unset").hide();
								topPanel.find("#dialog_snapshot_monthly_info_set").show();
	                            topPanel.find("#read_monthly_minute").text(minute);
	                            topPanel.find("#read_monthly_hour").text(hour12);
	                            topPanel.find("#read_monthly_meridiem").text(meridiem);
								topPanel.find("#read_monthly_timezone").text("("+timezones[timezone]+")");
	                            topPanel.find("#read_monthly_day_of_month").text(toDayOfMonthDesp(dayOfMonth));
                                topPanel.find("#read_monthly_max").text(max);	                                                                          
                                topPanel.find("#monthly_edit_link, #monthly_delete_link").data("intervalType", "monthly").data("snapshotPolicyId", json.createsnapshotpolicyresponse.id).data("max",max).data("timezone",timezone).data("minute", minute).data("hour12", hour12).data("meridiem", meridiem).data("dayOfMonth",dayOfMonth);                                         
	                            break;
	                    }	                      
                        	    						
                    },
                    error: function(XMLHttpResponse) {                            					
                        handleError(XMLHttpResponse);					
                    }
                });	           
	                        
	            break;		            
	       
	    }		    
	});	
	// *** recurring snapshot dialog - event binding (end) ******************************	    
         
    //***** switch between different tabs (begin) ********************************************************************
    var tabArray = [$("#tab_details"), $("#tab_snapshot")];
    var tabContentArray = [$("#tab_content_details"), $("#tab_content_snapshot")];
    switchBetweenDifferentTabs(tabArray, tabContentArray);       
    //***** switch between different tabs (end) **********************************************************************    
}

function volumeToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_storage_volume.png");		
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.type.substring(0,25));  
}

function volumeToRightPanel($midmenuItem1) {  
    copyAfterActionInfoToRightPanel($midmenuItem1); 
    volumeJsonToDetailsTab($midmenuItem1);  
    
    var jsonObj = $midmenuItem1.data("jsonObj");  
    volumeJsonToSnapshotTab(jsonObj);
}
 
function volumeJsonToDetailsTab($midmenuItem1){
    var jsonObj = $midmenuItem1.data("jsonObj"); 
    var $detailsTab = $("#right_panel_content #tab_content_details");  
    $detailsTab.data("jsonObj", jsonObj);   
    $detailsTab.find("#id").text(jsonObj.id);
    $detailsTab.find("#name").text(fromdb(jsonObj.name));    
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));    
    $detailsTab.find("#device_id").text(jsonObj.deviceid);   
    $detailsTab.find("#state").text(jsonObj.state);    
    $detailsTab.find("#storage").text(fromdb(jsonObj.storage));
    $detailsTab.find("#account").text(fromdb(jsonObj.account)); 
    
    $detailsTab.find("#type").text(jsonObj.type + " (" + jsonObj.storagetype + " storage)");
    $detailsTab.find("#size").text((jsonObj.size == "0") ? "" : convertBytes(jsonObj.size));		
    
    if (jsonObj.virtualmachineid == null) 
		$detailsTab.find("#vm_name").text("detached");
	else 
		$detailsTab.find("#vm_name").text(getVmName(jsonObj.vmname, jsonObj.vmdisplayname) + " (" + jsonObj.vmstate + ")");
		
    setDateField(jsonObj.created, $detailsTab.find("#created"));	
       
    //actions ***    
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();
        
    buildActionLinkForDetailsTab("Take Snapshot", volumeActionMap, $actionMenu, $midmenuItem1, $detailsTab);	//show take snapshot
    buildActionLinkForDetailsTab("Recurring Snapshot", volumeActionMap, $actionMenu, $midmenuItem1, $detailsTab);	//show Recurring Snapshot
    
    if(jsonObj.state != "Creating" && jsonObj.state != "Corrupted" && jsonObj.name != "attaching") {
        if(jsonObj.type=="ROOT") {
            if (jsonObj.vmstate == "Stopped") { 
                //buildActionLinkForDetailsTab("Create Template", volumeActionMap, $actionMenu, $midmenuItem1, $detailsTab);	//backend of CreateTemplateFromVolume is not working. Hide the option from UI until backend is fixed.
            }
        } 
        else { 
	        if (jsonObj.virtualmachineid != null) {
		        if (jsonObj.storagetype == "shared" && (jsonObj.vmstate == "Running" || jsonObj.vmstate == "Stopped")) {
			        buildActionLinkForDetailsTab("Detach Disk", volumeActionMap, $actionMenu, $midmenuItem1, $detailsTab); //show detach disk
		        }
	        } else {
		        // Disk not attached
		        if (jsonObj.storagetype == "shared") {
			        buildActionLinkForDetailsTab("Attach Disk", volumeActionMap, $actionMenu, $midmenuItem1, $detailsTab);   //show attach disk
    			    			  		    
			        if(jsonObj.vmname == null || jsonObj.vmname == "none")
			            buildActionLinkForDetailsTab("Delete Volume", volumeActionMap, $actionMenu, $midmenuItem1, $detailsTab); //show delete volume
		        }
	        }
        }
    }
} 

function  volumeJsonToSnapshotTab(jsonObj) {
    $.ajax({
		cache: false,
		data: createURL("command=listSnapshots&volumeid="+jsonObj.id+maxPageSize),
		dataType: "json",
		success: function(json) {							    
			var items = json.listsnapshotsresponse.snapshot;																						
			if (items != null && items.length > 0) {
			    var container = $("#right_panel_content #tab_content_snapshot").empty();
				var template = $("#snapshot_tab_template");				
				for (var i = 0; i < items.length; i++) {
					var newTemplate = template.clone(true);	               
	                volumeSnapshotJSONToTemplate(items[i], newTemplate); 
	                container.append(newTemplate.show());	
				}			
			}			
		}
	});
} 
 
function volumeSnapshotJSONToTemplate(jsonObj, template) {
    template.data("jsonObj", jsonObj);     
    template.attr("id", "volume_snapshot_"+jsonObj.id).data("volumeSnapshotId", jsonObj.id);    
    template.find("#title").text(fromdb(jsonObj.name));			   
    template.find("#id").text(jsonObj.id);
    template.find("#name").text(fromdb(jsonObj.name));			      
    template.find("#volumename").text(fromdb(jsonObj.volumename));	
    template.find("#intervaltype").text(jsonObj.intervaltype);	    		   
    template.find("#account").text(fromdb(jsonObj.account));
    template.find("#domain").text(fromdb(jsonObj.domain));    
    setDateField(jsonObj.created, template.find("#created"));	 
	
	var $actionLink = template.find("#snapshot_action_link");		
	$actionLink.bind("mouseover", function(event) {
        $(this).find("#snapshot_action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {
        $(this).find("#snapshot_action_menu").hide();    
        return false;
    });		
	
	var $actionMenu = $actionLink.find("#snapshot_action_menu");
    $actionMenu.find("#action_list").empty();	
    
    buildActionLinkForSubgridItem("Create Volume", volumeSnapshotActionMap, $actionMenu, template);	
    buildActionLinkForSubgridItem("Delete Snapshot", volumeSnapshotActionMap, $actionMenu, template);	
    buildActionLinkForSubgridItem("Create Template", volumeSnapshotActionMap, $actionMenu, template);	
} 
 
function volumeClearRightPanel() {       
    var $detailsTab = $("#right_panel_content #tab_content_details");  
    $detailsTab.find("#id").text("");
    $detailsTab.find("#name").text("");    
    $detailsTab.find("#zonename").text("");    
    $detailsTab.find("#device_id").text("");   
    $detailsTab.find("#state").text("");    
    $detailsTab.find("#storage").text("");
    $detailsTab.find("#account").text(""); 
    $detailsTab.find("#type").text("");
    $detailsTab.find("#size").text("");		
    $detailsTab.find("#vm_name").text("");
    $detailsTab.find("#created").text("");
} 
   
var volumeActionMap = {  
    "Attach Disk": {
        isAsyncJob: true,
        asyncJobResponse: "attachvolumeresponse",            
        dialogBeforeActionFn : doAttachDisk,
        inProcessText: "Attaching disk....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {              
            //var jsonObj = json.queryasyncjobresultresponse.virtualmachine[0];
            //Get embedded object from lsitVolume API until Bug 6481(embedded object returned by attachVolume API should include "type" property) is fixed.
            var jsonObj;           
            $.ajax({
                data: createURL("command=listVolumes&id="+id),
                dataType: "json",
                async: false,
                success: function(json) {                    
                    jsonObj = json.listvolumesresponse.volume[0];
                }            
            });           
            volumeToMidmenu(jsonObj, $midmenuItem1);
            volumeJsonToDetailsTab($midmenuItem1);   
        }
    },
    "Detach Disk": {
        api: "detachVolume",            
        isAsyncJob: true,
        asyncJobResponse: "detachvolumeresponse",
        inProcessText: "Detaching disk....",
        afterActionSeccessFn: function(json, $midmenuItem1, id){                
            //var jsonObj = json.queryasyncjobresultresponse.virtualmachine[0];
            //Get embedded object from lsitVolume API until Bug 6480(detachVolume API should return embedded object, like attachVolume API does.) is fixed.
            var jsonObj;            
            $.ajax({
                data: createURL("command=listVolumes&id="+id),
                dataType: "json",
                async: false,
                success: function(json) {                    
                    jsonObj = json.listvolumesresponse.volume[0];
                }            
            });            
            volumeToMidmenu(jsonObj,  $midmenuItem1);
            volumeJsonToDetailsTab($midmenuItem1);   
        }
    },
    "Create Template": {
        isAsyncJob: true,
        asyncJobResponse: "createtemplateresponse",            
        dialogBeforeActionFn : doCreateTemplateFromVolume,
        inProcessText: "Creating template....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {}   
    },
    "Delete Volume": {
        api: "deleteVolume",            
        isAsyncJob: false,        
        inProcessText: "Deleting volume....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {                 
            $midmenuItem1.remove();
            clearRightPanel();
            volumeClearRightPanel();
        }
    },
    "Take Snapshot": {
        isAsyncJob: true,
        asyncJobResponse: "createsnapshotresponse",            
        dialogBeforeActionFn : doTakeSnapshot,
        inProcessText: "Taking Snapshot....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {}   
    },
    "Recurring Snapshot": {                 
        dialogBeforeActionFn : doRecurringSnapshot 
    }   
}   

function doCreateTemplateFromVolume($actionLink, $detailsTab, $midmenuItem1) {       
    var jsonObj = $detailsTab.data("jsonObj");
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
			
			var id = $detailsTab.data("jsonObj").id;			
			var apiCommand = "command=createTemplate&volumeId="+id+"&name="+todb(name)+"&displayText="+todb(desc)+"&osTypeId="+osType+"&isPublic="+isPublic+"&passwordEnabled="+password;
	    	doActionToDetailsTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);					
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		} 
	}).dialog("open");
}   

function doTakeSnapshot($actionLink, $detailsTab, $midmenuItem1) {   
    $("#dialog_create_snapshot")					
    .dialog('option', 'buttons', { 					    
	    "Confirm": function() { 	
	        $(this).dialog("close");	
	    	
            var id = $detailsTab.data("jsonObj").id;	
			var apiCommand = "command=createSnapshot&volumeid="+id;
	    	doActionToDetailsTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	
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
    var $detailsTab = $("#right_panel_content #tab_content_details");  
	var volumeId = $detailsTab.data("jsonObj").id;
	
	var dialogBox = $("#dialog_recurring_snapshot"); 
	clearTopPanel();
	
	$.ajax({
        data: createURL("command=listSnapshotPolicies&volumeid="+volumeId),
        dataType: "json",
        async: false,
        success: function(json) {								
            var items = json.listsnapshotpoliciesresponse.snapshotpolicy;
            if(items!=null && items.length>0) {
                for(var i=0; i<items.length; i++) {
                    var item = items[i];                           
                    switch(item.intervaltype) {
                        case "0": //hourly    
							dialogBox.find("#dialog_snapshot_hourly_info_unset").hide();
							dialogBox.find("#dialog_snapshot_hourly_info_set").show();
                            dialogBox.find("#read_hourly_max").text(item.maxsnaps);
                            dialogBox.find("#read_hourly_minute").text(item.schedule);
							dialogBox.find("#read_hourly_timezone").text("("+timezones[item.timezone]+")");
                            dialogBox.find("#hourly_edit_link, #hourly_delete_link").data("intervalType", "hourly").data("snapshotPolicyId", item.id).data("max",item.maxsnaps).data("timezone",item.timezone).data("minute", item.schedule); 
                            break;
                        case "1": //daily
							dialogBox.find("#dialog_snapshot_daily_info_unset").hide();
							dialogBox.find("#dialog_snapshot_daily_info_set").show();
                            dialogBox.find("#read_daily_max").text(item.maxsnaps);
                            var parts = item.schedule.split(":");
                            dialogBox.find("#read_daily_minute").text(parts[0]);
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
                            dialogBox.find("#read_daily_hour").text(hour12);       
                            dialogBox.find("#read_daily_meridiem").text(meridiem);
							dialogBox.find("#read_daily_timezone").text("("+timezones[item.timezone]+")");
                            dialogBox.find("#daily_edit_link, #daily_delete_link").data("intervalType", "daily").data("snapshotPolicyId", item.id).data("max",item.maxsnaps).data("timezone",item.timezone).data("minute", parts[0]).data("hour12", hour12).data("meridiem", meridiem);                                   
                            break;
                        case "2": //weekly
							dialogBox.find("#dialog_snapshot_weekly_info_unset").hide();
							dialogBox.find("#dialog_snapshot_weekly_info_set").show();
                            dialogBox.find("#read_weekly_max").text(item.maxsnaps);
                            var parts = item.schedule.split(":");
                            dialogBox.find("#read_weekly_minute").text(parts[0]);
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
                            dialogBox.find("#read_weekly_hour").text(hour12);       
                            dialogBox.find("#read_weekly_meridiem").text(meridiem);    
							dialogBox.find("#read_weekly_timezone").text("("+timezones[item.timezone]+")");
                            dialogBox.find("#read_weekly_day_of_week").text(toDayOfWeekDesp(parts[2]));  
                            dialogBox.find("#weekly_edit_link, #weekly_delete_link").data("intervalType", "weekly").data("snapshotPolicyId", item.id).data("max",item.maxsnaps).data("timezone",item.timezone).data("minute", parts[0]).data("hour12", hour12).data("meridiem", meridiem).data("dayOfWeek",parts[2]);     
                            break;
                        case "3": //monthly
							dialogBox.find("#dialog_snapshot_monthly_info_unset").hide();
							dialogBox.find("#dialog_snapshot_monthly_info_set").show();
                            dialogBox.find("#read_monthly_max").text(item.maxsnaps);                                           
                            var parts = item.schedule.split(":");
                            dialogBox.find("#read_monthly_minute").text(parts[0]);
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
                            dialogBox.find("#read_monthly_hour").text(hour12);       
                            dialogBox.find("#read_monthly_meridiem").text(meridiem);  
							dialogBox.find("#read_monthly_timezone").text("("+timezones[item.timezone]+")");
                            dialogBox.find("#read_monthly_day_of_month").text(toDayOfMonthDesp(parts[2])); 
                            dialogBox.find("#monthly_edit_link, #monthly_delete_link").data("intervalType", "monthly").data("snapshotPolicyId", item.id).data("max",item.maxsnaps).data("timezone",item.timezone).data("minute", parts[0]).data("hour12", hour12).data("meridiem", meridiem).data("dayOfMonth",parts[2]);     
                            break;
                    }
                }    
            }                                 		    						
        },
        error: function(XMLHttpResponse) {			                   					
            handleError(XMLHttpResponse);					
        }
    });   	    
   	           			        
    dialogBox
	.dialog('option', 'buttons', { 
		"Close": function() { 
			$("#dialog_snapshotright").hide(0, function() { $(this).height("0px");});
			$(this).dialog("close"); 
		}
	}).dialog("open").data("volumeId", volumeId);
}	

function populateVirtualMachineField(domainId, account, zoneId) {        
    $.ajax({
	    cache: false,
	    data: createURL("command=listVirtualMachines&state=Running&zoneid="+zoneId+"&domainid="+domainId+"&account="+account+maxPageSize),
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
				data: createURL("command=listVirtualMachines&state=Stopped&zoneid="+zoneId+"&domainid="+domainId+"&account="+account+maxPageSize),
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
    var jsonObj = $detailsTab.data("jsonObj");    
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
	    	doActionToDetailsTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
	    }, 
	    "Cancel": function() { 					        
		    $(this).dialog("close"); 
	    } 
    }).dialog("open");
}	

//Snapshot tab actions
var volumeSnapshotActionMap = {  
    "Create Volume": {              
        isAsyncJob: true,
        asyncJobResponse: "createvolumeresponse",
        dialogBeforeActionFn : doCreateVolumeFromSnapshotInVolumePage,
        inProcessText: "Creating Volume....",
        afterActionSeccessFn: function(json, id, $subgridItem) {           
            //var jsonObj = ???  
            /*              
            var $midmenuItem1 = $("#midmenu_item").clone();
            $("#midmenu_container").append($midmenuItem1.show());
            volumeToMidmenu(jsonObj, $midmenuItem1);
			bindClickToMidMenu($midmenuItem1, volumeToRightPanel);  
			*/
        }
    }   
    , 
    "Delete Snapshot": {              
        api: "deleteSnapshot",     
        isAsyncJob: true,
        asyncJobResponse: "deletesnapshotresponse",        
        inProcessText: "Deleting snapshot....",
        afterActionSeccessFn: function(json, id, $subgridItem) {                 
            $subgridItem.slideUp("slow", function() {
                $(this).remove();
            });
        }
    } 
    ,
    "Create Template": {              
        isAsyncJob: true,
        asyncJobResponse: "createtemplateresponse",
        dialogBeforeActionFn : doCreateTemplateFromSnapshotInVolumePage,
        inProcessText: "Creating Template....",
        afterActionSeccessFn: function(json, id, $subgridItem) {}            
    }
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
    var jsonObj = $subgridItem.data("jsonObj");
       
    $("#dialog_create_template_from_snapshot")
    .dialog("option", "buttons", {
     "Add": function() {	
         var thisDialog = $(this);	 	                                                                        
         var isValid = true;					
         isValid &= validateString("Name", thisDialog.find("#name"), thisDialog.find("#name_errormsg"), false);		
         isValid &= validateString("Display Text", thisDialog.find("#display_text"), thisDialog.find("#display_text_errormsg"), false);				         		          		
         if (!isValid) return;                  	                                             
         
         thisDialog.dialog("close");	
         
         var name = thisDialog.find("#name").val();	 
         var displayText = thisDialog.find("#display_text").val();	 
         var osTypeId = thisDialog.find("#os_type").val(); 	  
         var password = thisDialog.find("#password").val();	                                         
       
         var id = jsonObj.id;
         var apiCommand = "command=createTemplate&snapshotid="+id+"&name="+todb(name)+"&displaytext="+todb(displayText)+"&ostypeid="+osTypeId+"&passwordEnabled="+password;    	 
    	 doActionToSubgridItem(id, $actionLink, apiCommand, $subgridItem);				
     },
     "Cancel": function() {	                         
         $(this).dialog("close");
     }	                     
    }).dialog("open");	     
}
