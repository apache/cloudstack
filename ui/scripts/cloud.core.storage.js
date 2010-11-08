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

function showStorageTab(domainId, targetTab) {      
    var currentSubMenu;
       		
    var populateZoneField = function(isAdmin) {         
        $.ajax({
		    data: "command=listZones&available=true&response=json",
		    dataType: "json",
		    success: function(json) {
			    var zones = json.listzonesresponse.zone;					    
			    if(isAdmin) {	
			        var poolZoneSelect = $("#dialog_add_pool").find("#pool_zone").empty();		
			        var hostZoneSelect = $("#dialog_add_host").find("#storage_zone").empty();	
			    }
			    var volumeZoneSelect = $("#dialog_add_volume").find("#volume_zone").empty();			
			    if (zones != null && zones.length > 0) {
			        for (var i = 0; i < zones.length; i++) {	
			            if(isAdmin) {			
				            poolZoneSelect.append("<option value='" + zones[i].id + "'>" + fromdb(zones[i].name) + "</option>"); 
				            hostZoneSelect.append("<option value='" + zones[i].id + "'>" + fromdb(zones[i].name) + "</option>"); 
				        }
				        volumeZoneSelect.append("<option value='" + zones[i].id + "'>" + fromdb(zones[i].name) + "</option>"); 
			        }
			    }
				if (isAdmin) {
					poolZoneSelect.change();
				}
		    }
		});	
    }   
    
    var populateDiskOfferingField = function() {        
        $.ajax({
		    data: "command=listDiskOfferings&response=json",
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
    }
    
    var populateVirtualMachineField = function(domainId, account, zoneId) {        
	    $.ajax({
		    cache: false,
		    data: "command=listVirtualMachines&state=Running&zoneid="+zoneId+"&domainid="+domainId+"&account="+account+"&response=json",
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
					data: "command=listVirtualMachines&state=Stopped&zoneid="+zoneId+"&domainid="+domainId+"&account="+account+"&response=json",
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
    
    var populateOSTypeField = function() {     
        $.ajax({
		    data: "command=listOsTypes&response=json",
		    dataType: "json",
		    success: function(json) {
			    types = json.listostypesresponse.ostype;
			    if (types != null && types.length > 0) {
				    var osTypeField = $("#dialog_create_template_from_snapshot").find("#os_type").empty();	
				    for (var i = 0; i < types.length; i++) {
					    var html = "<option value='" + types[i].id + "'>" + types[i].description + "</option>";
					    osTypeField.append(html);						
				    }
			    }	
		    }
	    });
	}
    
    var initializeVolumeTab = function(isAdmin) {          
        // Add Volume Dialog (begin)
	    activateDialog($("#dialog_add_volume").dialog({ 
		    autoOpen: false,
		    modal: true,
		    zIndex: 2000
	    }));	
	    
	    $("#storage_action_new_volume").bind("click", function(event) {
		    $("#dialog_add_volume")
		    .dialog('option', 'buttons', { 			    
			    "Add": function() { 
			        var thisDialog = $(this);
			    			            										
			        // validate values							
				    var isValid = true;									
				    isValid &= validateString("Name", thisDialog.find("#add_volume_name"), thisDialog.find("#add_volume_name_errormsg"));					
				    if (!isValid) return;
					
					var name = trim(thisDialog.find("#add_volume_name").val());					
				    var zoneId = thisDialog.find("#volume_zone").val();					    				
				    var diskofferingId = thisDialog.find("#volume_diskoffering").val();	
				    thisDialog.dialog("close");		
				    
				    var submenuContent = $("#submenu_content_volume");						
				    var template = $("#volume_template").clone(true);	
				    var loadingImg = template.find(".adding_loading");		
	                var rowContainer = template.find("#row_container");                  
                    loadingImg.find(".adding_text").text("Adding....");	
                    loadingImg.show();  
                    rowContainer.hide();	                  
                    submenuContent.find("#grid_content").prepend(template);	 
                    template.fadeIn("slow");	           									
					    					
				    $.ajax({
					    data: "command=createVolume&zoneId="+zoneId+"&name="+encodeURIComponent(name)+"&diskOfferingId="+diskofferingId+"&accountId="+"1"+"&response=json", 
					    dataType: "json",
					    success: function(json) {						        
					        var jobId = json.createvolumeresponse.jobid;
					        template.attr("id","volumeNew"+jobId).data("jobId", jobId);
					        var timerKey = "volume"+jobId;
								    
					        $("body").everyTime(2000, timerKey, function() {
							    $.ajax({
								    data: "command=queryAsyncJobResult&jobId="+json.createvolumeresponse.jobid+"&response=json",
								    dataType: "json",
								    success: function(json) {										       						   
									    var result = json.queryasyncjobresultresponse;
									    if (result.jobstatus == 0) {
										    return; //Job has not completed
									    } else {											    
										    $("body").stopTime(timerKey);
										    if (result.jobstatus == 1) {
											    // Succeeded	
											    volumeJSONToTemplate(result.volume[0], template);												    
											    changeGridRowsTotal(submenuContent.find("#grid_rows_total"), 1);  											
											                                                                                              
                                                loadingImg.hide(); 	                                                                                    
                                                var createdSuccessfullyImg = template.find("#created_successfully").show();	
                                                createdSuccessfullyImg.find("#close_button").bind("click", function() {
                                                    createdSuccessfullyImg.hide();
                                                    rowContainer.show(); 
                                                });	
											                                                              
										    } else if (result.jobstatus == 2) {
											    $("#dialog_alert").html("<p>" + fromdb(result.jobresult) + "</p>").dialog("open");
											    template.slideUp("slow", function() {
													$(this).remove();
												});						    
										    }
									    }
								    },
								    error: function(XMLHttpResponse) {
									    $("body").stopTime(timerKey);
									    handleError(XMLHttpResponse);
									    template.slideUp("slow", function() {
											$(this).remove();
										});
								    }
							    });
						    }, 0);						    					
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
	    // Add Volume Dialog (end)
              
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
    	       
        function hideShowDetachAttachLinks(vmname, template) {              
	        var detachLink = template.find("#volume_action_detach_span");
	        var attachLink = template.find("#volume_action_attach_span");
	        
	        if (vmname=="none"||vmname==""||vmname==null)  {  //if NOT attached to a virtual machine, hide "detach" link, show "attach" link.  
	            detachLink.hide();
	            attachLink.show();	            
	        }        
	        else  { //if attached to a virtual machine, hide "attach" link, show "ditach" link. 
	            attachLink.hide();
	            detachLink.show();
	        }
        }  
    
        // FUNCTION: volume JSON to Template
	    function volumeJSONToTemplate(json, template) {		
			if (getHypervisorType() == "kvm") {
				//template.find("#volume_action_create_template_span").show();  //create template from volume doesn't work. Hide it from UI on 2.1.x 
			}
	        template.attr("id", "volume"+json.id);   
		    if (index++ % 2 == 0) {
			    template.addClass("smallrow_even");
		    } else {
			    template.addClass("smallrow_odd");
		    }
		    template.data("volumeId", json.id);
		    template.data("vmname", getVmName(json.vmname, json.vmdisplayname));	
			template.data("vmstate", json.vmstate);
		    template.data("domainId", json.domainid);	
		    template.data("account", fromdb(json.account));	
			template.data("volumeName", fromdb(json.name));
			template.data("vmid", json.virtualmachineid);
			template.data("zoneId", json.zoneid);
		    
		    template.find("#volume_id").text(json.id);
		    template.find("#volume_name").text(json.name);
			template.find("#volume_zone").text(json.zonename);
		    template.find("#volume_account").text(json.account);
		    template.find("#volume_domain").text(json.domain);
		    template.find("#volume_hostname").text(json.storage);
		    template.find("#volume_path").text(json.path);
		    template.find("#volume_state").text(json.state);
		    template.find("#volume_size").text((json.size == "0") ? "" : convertBytes(json.size));		    
		    template.find("#volume_type").text(json.type + " (" + json.storagetype + " storage)");
			if (json.virtualmachineid == undefined) {
				template.find("#volume_vmname").text("detached");
			} else {
				template.find("#volume_vmname").text(getVmName(json.vmname, json.vmdisplayname) + " (" + json.vmstate + ")");
			}
			
			setDateField(json.created, template.find("#volume_created"));			
		   		    		
			if(json.type=="ROOT") {
				if (json.virtualmachineid != undefined && json.vmstate == "Stopped" && getHypervisorType() == "kvm") {
					//template.find("#volume_action_create_template_span").show();  //create template from volume doesn't work. Hide it from UI on 2.1.x
				}
			} else {
				// DataDisk
				if (json.virtualmachineid != undefined) {
					if (json.storagetype == "shared" && (json.vmstate == "Running" || json.vmstate == "Stopped")) {
						template.find("#volume_action_detach_span").show();
					}
					if (json.vmstate == "Stopped" && getHypervisorType() == "kvm") {
						//template.find("#volume_action_create_template_span").show();  //create template from volume doesn't work. Hide it from UI on 2.1.x
					}
				} else {
					// Disk not attached
					if (getHypervisorType() == "kvm") {
						//template.find("#volume_action_create_template_span").show();  //create template from volume doesn't work. Hide it from UI on 2.1.x
					}
					if (json.storagetype == "shared") {
						template.find("#volume_action_attach_span, #volume_action_delete_span").show();
					}
				}
			}
			
			if(json.state == "Creating" || json.state == "Corrupted" || json.name == "attaching") 
			    template.find("#grid_links_container").hide();
			else
			    template.find("#grid_links_container").show();
	    }
	    	  
	    function listVolumes() {	 
	        var submenuContent = $("#submenu_content_volume");
	         
            var commandString;            
			var advanced = submenuContent.find("#search_button").data("advanced");                    
			if (advanced != null && advanced) {		
			    var name = submenuContent.find("#advanced_search #adv_search_name").val();	    
			    var zone = submenuContent.find("#advanced_search #adv_search_zone").val();
			    var pod = submenuContent.find("#advanced_search #adv_search_pod").val();
			    var domainId = submenuContent.find("#advanced_search #adv_search_domain").val();
			    var account = submenuContent.find("#advanced_search #adv_search_account").val();
			    var moreCriteria = [];								
				if (name!=null && trim(name).length > 0) 
					moreCriteria.push("&name="+encodeURIComponent(trim(name)));										
			    if (zone!=null && zone.length > 0) 
					moreCriteria.push("&zoneId="+zone);		
			    if (pod!=null && pod.length > 0) 
					moreCriteria.push("&podId="+pod);	
				if (domainId!=null && domainId.length > 0) 
					moreCriteria.push("&domainid="+domainId);		
				if (account!=null && account.length > 0) 
					moreCriteria.push("&account="+account);			
				commandString = "command=listVolumes&page=" + currentPage + moreCriteria.join("") + "&response=json";		
			} else {    
			     var moreCriteria = [];		
			    if(domainId!=null)
			        moreCriteria.push("&domainid="+domainId);				   			  
                var searchInput = submenuContent.find("#search_input").val();            
                if (searchInput != null && searchInput.length > 0) 
                    commandString = "command=listVolumes&page=" + currentPage + moreCriteria.join("") + "&keyword=" + searchInput + "&response=json"
                else
                    commandString = "command=listVolumes&page=" + currentPage + moreCriteria.join("") + "&response=json";		
            }
            	
            //listItems(submenuContent, commandString, jsonResponse1, jsonResponse2, template, fnJSONToTemplate);         
            listItems(submenuContent, commandString, "listvolumesresponse", "volume", $("#volume_template"), volumeJSONToTemplate);  
	    } 
	   
	    submenuContentEventBinder($("#submenu_content_volume"), listVolumes);
	   
	    
	    $("#submenu_volume").bind("click", function(event) {			        
		    event.preventDefault();
		  
		    currentSubMenu.addClass("submenu_links_off").removeClass("submenu_links_on");  		
		    $(this).addClass("submenu_links_on").removeClass("submenu_links_off");			    	    
		    currentSubMenu = $(this);
		    
		    $("#submenu_content_volume").show();
		    $("#submenu_content_pool").hide();
		    $("#submenu_content_storage").hide();  
		    $("#submenu_content_snapshot").hide(); 
		    
		    var submenuContent = $("#submenu_content_volume");		    
		    if (isAdmin)
		        submenuContent.find("#adv_search_pod_li, #adv_search_domain_li, #adv_search_account_li").show();   
			     
		    currentPage = 1;  			
		    listVolumes();
	    });   
		 		  
        function listSnapshots() {      
            var submenuContent = $("#submenu_content_snapshot");
            
            var commandString;            
			var advanced = submenuContent.find("#search_button").data("advanced");             
			if (advanced != null && advanced) {					    
			    var domainId = submenuContent.find("#advanced_search #adv_search_domain").val();	    
			    var account = submenuContent.find("#advanced_search #adv_search_account").val();
			    var moreCriteria = [];	
			    if (domainId!=null && domainId.length > 0) 
					moreCriteria.push("&domainid="+domainId);			
				if (account!=null && account.length > 0) 
					moreCriteria.push("&account="+account);			    
				commandString = "command=listSnapshots&page="+currentPage+moreCriteria.join("")+"&response=json";  
			} else {     
			    var moreCriteria = [];		
			    if(domainId!=null)
			        moreCriteria.push("&domainid="+domainId);			   
			    var searchInput = submenuContent.find("#search_input").val();         
                if (searchInput != null && searchInput.length > 0) 
                    commandString = "command=listSnapshots&page="+currentPage+moreCriteria.join("")+"&keyword="+searchInput+"&response=json"
                else
                    commandString = "command=listSnapshots&page="+currentPage+moreCriteria.join("")+"&response=json";          
            }   
            
            //listItems(submenuContent, commandString, jsonResponse1, jsonResponse2, template, fnJSONToTemplate);         
            listItems(submenuContent, commandString, "listsnapshotsresponse", "snapshot", $("#snapshot_template"), snapshotJSONToTemplate);    
	    }
	    
	    submenuContentEventBinder($("#submenu_content_snapshot"), listSnapshots);	   
	     
	    $("#snapshot_template").bind("click", function(event) {
	        event.preventDefault();
	        event.stopPropagation();
	        
	        var template = $(this);
	        var snapshotId = template.data("snapshotId");
	       	        
	        var target = event.target.id;
	        switch(target) {
	             case "snapshot_action_create_volume":	       
	                 $("#dialog_add_volume_from_snapshot")
	                 .dialog("option", "buttons", {	                    
	                     "Add": function() {	
	                         var thisDialog = $(this);	 
	                         thisDialog.dialog("close");
	                                               
	                         var isValid = true;					
					         isValid &= validateString("Name", thisDialog.find("#name"), thisDialog.find("#name_errormsg"));					          		
					         if (!isValid) return;          	                                             
	                         
	                         var name = thisDialog.find("#name").val();	                
	                         
	                         var loadingImg = template.find(".adding_loading");		
	                         var rowContainer = template.find("#row_container");                           
                             loadingImg.find(".adding_text").text("Creating volume....");	
                             loadingImg.show();  
                             rowContainer.hide();	
	                         	                                               
	                         $.ajax({
						         data: "command=createVolume&snapshotid="+snapshotId+"&name="+name+"&response=json",
						         dataType: "json",
						         success: function(json) {							           								 
							        var jobId = json.createvolumeresponse.jobid;					        
					                var timerKey = "createVolumeJob"+jobId;        					        
                                    $("body").everyTime(2000, timerKey, function() {
							            $.ajax({
								            data: "command=queryAsyncJobResult&jobId="+json.createvolumeresponse.jobid+"&response=json",
								            dataType: "json",
								            success: function(json) {										       						   
									            var result = json.queryasyncjobresultresponse;									           
									            if (result.jobstatus == 0) {
										            return; //Job has not completed
									            } else {											    
										            $("body").stopTime(timerKey);
										            if (result.jobstatus == 1) {
											            // Succeeded		
											            loadingImg.hide(); 		
											            rowContainer.show(); 									            
											            $("#dialog_info").html("<p>Volume was created successfully</p>").dialog("open");											                                   
										            } else if (result.jobstatus == 2) {
										                loadingImg.hide(); 		
											            rowContainer.show(); 	
											            $("#dialog_alert").html("<p>" + fromdb(result.jobresult) + "</p>").dialog("open");											    				    
										            }
									            }
								            },
								            error: function(XMLHttpResponse) {								                
									            $("body").stopTime(timerKey);
									            loadingImg.hide(); 		
											    rowContainer.show(); 	
									            handleError(XMLHttpResponse);									    
								            }
							            });
						            }, 0);							 
						         },
						         error: function(XMLHttpResponse) {
						             loadingImg.hide(); 		
									 rowContainer.show(); 	
									 handleError(XMLHttpResponse);		
						         }
					         });                      
	                     },
	                     "Cancel": function() {	                         
	                         $(this).dialog("close");
	                     }
	                 }).dialog("open");            
	                 break;
	                 
	             case "snapshot_action_delete":	                             
	                 var loadingImg = template.find(".adding_loading");		
                     var rowContainer = template.find("#row_container");                           
                     loadingImg.find(".adding_text").text("Deleting snapshot....");	
                     loadingImg.show();  
                     rowContainer.hide();	
	             	                       
	                 $.ajax({
						 data: "command=deleteSnapshot&id="+snapshotId+"&response=json",
						 dataType: "json",
						 success: function(json) {											 
							var jobId = json.deletesnapshotresponse.jobid;					        
					        var timerKey = "deleteSnapshotJob"+jobId;
					        
                            $("body").everyTime(2000, timerKey, function() {
							    $.ajax({
								    data: "command=queryAsyncJobResult&jobId="+json.deletesnapshotresponse.jobid+"&response=json",
								    dataType: "json",
								    success: function(json) {										       						   
									    var result = json.queryasyncjobresultresponse;									    
									    if (result.jobstatus == 0) {
										    return; //Job has not completed
									    } else {											    
										    $("body").stopTime(timerKey);
										    if (result.jobstatus == 1) {
											    // Succeeded
											    loadingImg.hide(); 		
									            rowContainer.show(); 	
											    template.slideUp("slow", function() {
													$(this).remove();
												});		                                                           
										    } else if (result.jobstatus == 2) {
										        loadingImg.hide(); 		
									            rowContainer.show(); 
											    $("#dialog_alert").html("<p>" + fromdb(result.jobresult) + "</p>").dialog("open");											    					    
										    }
									    }
								    },
								    error: function(XMLHttpResponse) {								        
									    $("body").stopTime(timerKey);
									    loadingImg.hide(); 		
									    rowContainer.show(); 
									    handleError(XMLHttpResponse);									    
								    }
							    });
						    }, 0);							 
						 },
						 error: function(XMLHttpResponse) {
						     loadingImg.hide(); 		
							 rowContainer.show(); 
							 handleError(XMLHttpResponse);		
						 }
					 });             
	                 break;	                 
	                 
	             case "snapshot_action_create_template":
	                 $("#dialog_create_template_from_snapshot")
	                 .dialog("option", "buttons", {
	                     "Add": function() {	
	                         var thisDialog = $(this);	 	                                                                        
	                         var isValid = true;					
					         isValid &= validateString("Name", thisDialog.find("#name"), thisDialog.find("#name_errormsg"), false);		
					         isValid &= validateString("Display Text", thisDialog.find("#display_text"), thisDialog.find("#display_text_errormsg"), false);				         		          		
					         if (!isValid) return;                  	                                             
	                         
	                         var name = thisDialog.find("#name").val();	 
	                         var displayText = thisDialog.find("#display_text").val();	 
	                         var osTypeId = thisDialog.find("#os_type").val(); 	 
	                         var password = thisDialog.find("#password").val();		                                          
	                         thisDialog.dialog("close");	
	                         		     	                                                         	                                                  						
							 var loadingImg = template.find(".adding_loading");							
							 var rowContainer = template.find("#row_container");
							 loadingImg.find(".adding_text").text("Creating template....");				            
							 loadingImg.fadeIn("slow");
				             rowContainer.hide(); 	                                  
	                                                    
	                         $.ajax({
						         data: "command=createTemplate&snapshotid="+snapshotId+"&name="+name+"&displaytext="+displayText+"&ostypeid="+osTypeId+"&passwordEnabled="+password+"&response=json",
						         dataType: "json",
						         success: function(json) {							            					           								 
							        var jobId = json.createtemplateresponse.jobid;					        
					                var timerKey = "createTemplateJob"+jobId;        					        
                                    $("body").everyTime(2000, timerKey, function() {
							            $.ajax({
								            data: "command=queryAsyncJobResult&jobId="+json.createtemplateresponse.jobid+"&response=json",
								            dataType: "json",
								            success: function(json) {									                							       						   
									            var result = json.queryasyncjobresultresponse;									           
									            if (result.jobstatus == 0) {
										            return; //Job has not completed
									            } else {											    
										            $("body").stopTime(timerKey);
										            if (result.jobstatus == 1) {
											            // Succeeded	
											            loadingImg.hide();
														rowContainer.show(); 
														$("#dialog_info").html("<p>Template was created successfully</p>").dialog("open");	                                                                
                                                    } else if (result.jobstatus == 2) {		                                                    
                                                        loadingImg.hide();
														rowContainer.show(); 
														$("#dialog_alert").html("<p>" + fromdb(result.jobresult) + "</p>").dialog("open");            											            										    				    
										            }
									            }
								            },
								            error: function(XMLHttpResponse) {								                
									            $("body").stopTime(timerKey);
									            loadingImg.hide();
												rowContainer.show(); 									           
									            handleError(XMLHttpResponse);           								            								    
								            }
							            });
						            }, 0);							 
						         },
						         error: function(XMLHttpResponse) {								         
						             loadingImg.hide();
									 rowContainer.show(); 					            
									 handleError(XMLHttpResponse);								 								 
						         }
					         });                      
	                     },
	                     "Cancel": function() {	                         
	                         $(this).dialog("close");
	                     }	                     
	                 }).dialog("open");	                 
	                 break;              
	        }
	    }); 
	    
		if (getHypervisorType() == "xenserver") {
			$("#volume_action_snapshot_grid, #volume_action_take_snapshot_container, #volume_action_recurring_snapshot_container").show();
			$("#submenu_snapshot").show().bind("click", function(event) {			        
				event.preventDefault();
			  
				currentSubMenu.addClass("submenu_links_off").removeClass("submenu_links_on");  	
				$(this).addClass("submenu_links_on").removeClass("submenu_links_off");			    		    
				currentSubMenu = $(this);
				
				$("#submenu_content_snapshot").show();
				$("#submenu_content_pool").hide();
				$("#submenu_content_storage").hide();  
				$("#submenu_content_volume").hide(); 
				
				var submenuContent = $("#submenu_content_snapshot");			
				if (isAdmin)
				    submenuContent.find("#adv_search_domain_li, #adv_search_account_li").show();  
				else  //There are no fields in Advanced Search Dialog Box for non-admin user. So, hide Advanced Search Link.
				    submenuContent.find("#advanced_search_link").hide(); 
							 
				currentPage = 1;  			
				listSnapshots();
			});  
		}
		else if (getHypervisorType() == "kvm") {
		    $("#dialog_add_pool #pool_cluster_container").hide();
		}
			
	    activateDialog($("#dialog_detach_volume").dialog({ 
		    autoOpen: false,
		    modal: true,
		    zIndex: 2000
	    }));	
		
	    activateDialog($("#dialog_attach_volume").dialog({ 
		    autoOpen: false,
		    modal: true,
		    zIndex: 2000
	    }));	
		
	    activateDialog($("#dialog_delete_volume").dialog({ 
		    autoOpen: false,
		    modal: true,
		    zIndex: 2000
	    }));	
		
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
		
		$.ajax({
			data: "command=listOsTypes&response=json",
			dataType: "json",
			success: function(json) {
				types = json.listostypesresponse.ostype;
				if (types != null && types.length > 0) {
					var select = $("#dialog_create_template #create_template_os_type").empty();
					for (var i = 0; i < types.length; i++) {
						select.append("<option value='" + types[i].id + "'>" + types[i].description + "</option>");
					}
				}	
			}
		});
			
		// *** recurring snapshot dialog - event binding (begin) ******************************	
		var dialogRecurringSnapshot = $("#dialog_recurring_snapshot");
		
		function clearTopPanel(target) { // "target == null" means target at all (hourly + daily + weekly + monthly)
	        var dialogBox = dialogRecurringSnapshot;
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
	        var dialogBox = dialogRecurringSnapshot;
	    		    
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
					}, 1000, function() {
						//$(this).fadeIn("fast");
					// Animation complete.
				});				
				//bottomPanel.show("slide", { direction: "left" }, 1000);	      
		    }	
		    else if(targetId.indexOf("_delete_link")!=-1) {  		       
		        clearBottomPanel();
		        var snapshotPolicyId = $("#"+targetId).data("snapshotPolicyId");			                 
		        if(snapshotPolicyId == null || snapshotPolicyId.length==0)
		            return;
	            $.ajax({
                    data: "command=deleteSnapshotPolicies&id="+snapshotPolicyId+"&response=json",
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
                        data: "command=createSnapshotPolicy&intervaltype="+intervalType+"&schedule="+schedule+"&volumeid="+volumeId+"&maxsnaps="+max+"&timezone="+encodeURIComponent(timezone)+"&response=json",
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
			
		// *** volume template - event binding (begin) **************************************	
	    $("#volume_template").bind("click", function(event) {			      
		    var template = $(this);
		    var link = $(event.target);
		    var linkAction = link.attr("id");
		    var volumeId = template.data("volumeId");
			var vmId = template.data("vmid");
		    var vmname = template.data("vmname");	
			var vmState = template.data("vmstate");
		    var domainId = template.data("domainId");
		    var account = template.data("account");
		    var volumeName = template.data("volumeName");
			var zoneId = template.data("zoneId");
			var timerKey = "volume"+volumeId;	
			var submenuContent = $("#submenu_content_volume");		
					        
		    switch (linkAction) {						
			    case "volume_action_delete" : 	
                    //check if this volume is attached to a virtual machine. If yes, can't be deleted.						        		    		    		    			    
			        if(vmname != null && (vmname != "" || vmname != "none")) {  
				        $("#dialog_alert").html("<p>This volume is attached to virtual machine " + vmname + " and can't be deleted.</p>")
                        $("#dialog_alert").dialog("open");		        		        
			            return;
			        }					       		
   				        
				    $("#dialog_delete_volume")					
				    .dialog('option', 'buttons', { 					    
					    "Confirm": function() { 				    					            					            					            				        
							var volumeTemplate = $("#volume"+volumeId);	
							var loadingImg = volumeTemplate.find(".adding_loading");
							var rowContainer = volumeTemplate.find("#row_container");
							loadingImg.find(".adding_text").text("Deleting....");	
						    $(this).dialog("close");						    
						    if(volumeTemplate.find("#volume_snapshot_detail_panel").css("display")=="block") //if volume's snapshot grid is poped down, close it.
							    volumeTemplate.find("#volume_action_snapshot_grid").click();							    	
				            loadingImg.fadeIn("slow");
				            rowContainer.hide(); 
				                					            					        
						    $.ajax({
								data: "command=deleteVolume&id="+volumeId+"&response=json",
								dataType: "json",
								success: function(json) {							                    					                    				                				                
									volumeTemplate.slideUp("slow", function(){
									    $(this).remove();
									});
								},
								error: function(XMLHttpResponse) {	
								    handleError(XMLHttpResponse);						                    			                    
									loadingImg.hide(); 								                            
									rowContainer.show(); 								
								}
							});						
					    }, 
					    "Cancel": function() { 					        
						    $(this).dialog("close"); 
					    } 
				    }).dialog("open");
				    break;	
				    				
			    case "volume_action_detach" : 		   				        
				    $("#dialog_detach_volume")					
				    .dialog('option', 'buttons', { 					   
					    "Confirm": function() { 				    					            					            					            				        
							var loadingImg = template.find(".adding_loading");
							var rowContainer = template.find("#row_container");
							loadingImg.find(".adding_text").text("Detaching....");	
						    $(this).dialog("close");							    
						    if(template.find("#volume_snapshot_detail_panel").css("display")=="block") //if volume's snapshot grid is poped down, close it.
							    template.find("#volume_action_snapshot_grid").click();							    
				            loadingImg.show();  
				            rowContainer.hide();
				            					            					        
						    $.ajax({
								data: "command=detachVolume&id="+volumeId+"&response=json",
								dataType: "json",
								success: function(json) {							                    					                    				                				                
									$("body").everyTime(5000, timerKey, function() {
										$.ajax({
											data: "command=queryAsyncJobResult&jobId="+json.detachvolumeresponse.jobid+"&response=json",
											dataType: "json",
											success: function(json) {									                
												var result = json.queryasyncjobresultresponse;										           
												if (result.jobstatus == 0) {										                    
													return; //Job has not completed
												} else {
													$("body").stopTime(timerKey);
													if (result.jobstatus == 1) {
														// Succeeded		
														//template.find("#volume_action_attach_span, #volume_action_delete_span, #volume_action_create_template_span").show();  //create template from volume doesn't work. Hide it from UI on 2.1.x
														template.find("#volume_action_attach_span, #volume_action_delete_span").show();  //delete line after createTemplateFromVolume API is fixed.
														
														template.find("#volume_action_detach_span").hide();																
														template.find("#volume_vmname").text("detached");
														template.data("vmid", null).data("vmname", null);
														loadingImg.hide(); 								                            
														rowContainer.show();   
													} else if (result.jobstatus == 2) {
														// Failed	
														loadingImg.hide(); 								                            
														rowContainer.show(); 	
														$("#dialog_alert").html("<p>" + fromdb(result.jobresult) + "</p>").dialog("open");
													}
												}
											},
											error: function(XMLHttpResponse) {
												$("body").stopTime(timerKey);										                
												loadingImg.hide(); 								                            
												rowContainer.show(); 
												handleError(XMLHttpResponse);
											}
										});
									}, 0);
								},
								error: function(XMLHttpResponse) {							                    			                    
									loadingImg.hide(); 								                            
									rowContainer.show(); 
									handleError(XMLHttpResponse);
								}
							});						
					    },
					    "Cancel": function() { 					        
						    $(this).dialog("close"); 
					    } 
				    }).dialog("open");
				    break;				
				    
			    case "volume_action_attach" : 			
			        populateVirtualMachineField(domainId, account, zoneId);
			     		   				        
				    $("#dialog_attach_volume")					
				    .dialog('option', 'buttons', { 					    
					    "Confirm": function() { 
					        var virtualMachineId = $("#dialog_attach_volume #volume_vm").val();		
					        if(virtualMachineId==null)  {
					            $(this).dialog("close"); 
					            $("#dialog_alert").html("<p>Please attach volume to a valid virtual machine</p>").dialog("open");
					            return;					            
					        }		       
					    				    					            					            					            				        
							var loadingImg = template.find(".adding_loading");
							var rowContainer = template.find("#row_container");
							loadingImg.find(".adding_text").text("Attaching....");	
						    $(this).dialog("close");							    
						    if(template.find("#volume_snapshot_detail_panel").css("display")=="block") //if volume's snapshot grid is poped down, close it.
							    template.find("#volume_action_snapshot_grid").click();	 
				            loadingImg.show();  
				            rowContainer.hide();	            
				      
				            var virtualMachineId = $("#dialog_attach_volume #volume_vm").val();		
						    $.ajax({
								data: "command=attachVolume&id="+volumeId+'&virtualMachineId='+virtualMachineId+"&response=json",
								dataType: "json",
								success: function(json) {							                    					                    				                				                
									$("body").everyTime(5000, timerKey, function() {
										$.ajax({
											data: "command=queryAsyncJobResult&jobId="+json.attachvolumeresponse.jobid+"&response=json",
											dataType: "json",
											success: function(json) {									                
												var result = json.queryasyncjobresultresponse;										           
												if (result.jobstatus == 0) {										                    
													return; //Job has not completed
												} else {
													$("body").stopTime(timerKey);
													if (result.jobstatus == 1) {
														// Succeeded
														if (result.virtualmachine[0].vmstate == "Stopped") {
															template.find("#volume_action_attach_span, #volume_action_delete_span").hide();	
															//template.find("#volume_action_detach_span, #volume_action_create_template_span").show();  //create template from volume doesn't work. Hide it from UI on 2.1.x
															template.find("#volume_action_detach_span").show();  //delete line after createTemplateFromVolume API is fixed.
														} else {
															template.find("#volume_action_attach_span, #volume_action_delete_span, #volume_action_create_template_span").hide();
															template.find("#volume_action_detach_span").show();
														}
														template.find("#volume_vmname").text(getVmName(result.virtualmachine[0].vmname, result.virtualmachine[0].vmdisplayname) + " (" + result.virtualmachine[0].vmstate + ")");
														template.data("vmid", virtualMachineId).data("vmname", getVmName(result.virtualmachine[0].vmname, result.virtualmachine[0].vmdisplayname));
														loadingImg.hide(); 								                            								                           							                            
														rowContainer.show(); 					                                           
													} else if (result.jobstatus == 2) {
														// Failed		
														loadingImg.hide(); 								                            
														rowContainer.show(); 												               										                
														$("#dialog_alert").html("<p>" + fromdb(result.jobresult) + "</p>").dialog("open");
													}
												}
											},
											error: function(XMLHttpResponse) {
												$("body").stopTime(timerKey);	
												loadingImg.hide(); 								                            
												rowContainer.show(); 
												handleError(XMLHttpResponse);
											}
										});
									}, 0);
								},
								error: function(XMLHttpResponse) {							                    			                    
									loadingImg.hide(); 								                            
									rowContainer.show(); 
									handleError(XMLHttpResponse);
								}
							});						
					    }, 
					    "Cancel": function() { 					        
						    $(this).dialog("close"); 
					    } 
				    }).dialog("open");
				    break;
				    
				case "volume_action_create_template" :
					if(vmId != null && vmState != "Stopped") {
						$("#dialog_alert").html("<p><b>"+vmname+"</b> needs to be stopped before you can create a template of this disk volume.</p>")
						$("#dialog_alert").dialog("open");
						return false;
					}
					$("#dialog_create_template").find("#volume_name").text(volumeName);
					$("#dialog_create_template")
					.dialog('option', 'buttons', { 						
						"Create": function() { 							
							// validate values
					        var isValid = true;					
					        isValid &= validateString("Name", $("#create_template_name"), $("#create_template_name_errormsg"));
        					isValid &= validateString("Display Text", $("#create_template_desc"), $("#create_template_desc_errormsg"));			
					        if (!isValid) return;		
					        
					        var name = trim($("#create_template_name").val());
							var desc = trim($("#create_template_desc").val());
							var osType = $("#create_template_os_type").val();					
							var isPublic = $("#create_template_public").val();
                            var password = $("#create_template_password").val();
                            
							$(this).dialog("close"); 
							if(template.find("#volume_snapshot_detail_panel").css("display")=="block") //if volume's snapshot grid is poped down, close it.
							    template.find("#volume_action_snapshot_grid").click();								
							template.find(".adding_loading .adding_text").text("Creating Template...");
							template.find(".adding_loading").show();
							template.find("#row_container").hide();
							
							$.ajax({
								data: "command=createTemplate&volumeId="+volumeId+"&name="+encodeURIComponent(name)+"&displayText="+encodeURIComponent(desc)+"&osTypeId="+osType+"&isPublic="+isPublic+"&passwordEnabled="+password+"&response=json",
								dataType: "json",
								success: function(json) {
									$("body").everyTime(
										30000, // This is templates..it could take hours
										timerKey,
										function() {
											$.ajax({
												data: "command=queryAsyncJobResult&jobId="+json.createtemplateresponse.jobid+"&response=json",
												dataType: "json",
												success: function(json) {
													var result = json.queryasyncjobresultresponse;
													if (result.jobstatus == 0) {
														return; //Job has not completed
													} else {
														$("body").stopTime(timerKey);
														template.find(".adding_loading").hide();
														template.find("#row_container").show();
														if (result.jobstatus == 1) {
															$("#dialog_info").html("<p>" + ((isPublic=="true")? "Public":"Private") + " template: " + name + " has been successfully created</p>").dialog("open");
														} else if (result.jobstatus == 2) {
															$("#dialog_alert").html("<p>" + fromdb(result.jobresult) + "</p>").dialog("open");
														}
													}
												},
												error: function(XMLHttpResponse) {
													template.find(".adding_loading").hide();
													template.find("#row_container").show();
													$("body").stopTime(timerKey);
													handleError(XMLHttpResponse);
												}
											});
										},
										0
									);
								},
								error: function(XMLHttpResponse) {
									template.find(".adding_loading").hide();
									template.find("#row_container").show();
									handleError(XMLHttpResponse);
								}
							});
						},
						"Cancel": function() { 
							$(this).dialog("close"); 
						} 
					}).dialog("open");
					break;
					
			    case "volume_action_take_snapshot":	      	        
			        $("#dialog_create_snapshot")					
				    .dialog('option', 'buttons', { 					    
					    "Confirm": function() { 					        		    					            					            					            				        
							var volumeTemplate = $("#volume"+volumeId);								
							var loadingImg = volumeTemplate.find(".adding_loading");							
							var rowContainer = volumeTemplate.find("#row_container");
							loadingImg.find(".adding_text").text("Taking snapshot....");	
						    $(this).dialog("close");					            			            
				            if(template.find("#volume_snapshot_detail_panel").css("display")=="block") //if volume's snapshot grid is poped down, close it.
							    template.find("#volume_action_snapshot_grid").click();		
							loadingImg.fadeIn("slow");
				            rowContainer.hide(); 									              					            					        
						    $.ajax({
								data: "command=createSnapshot&volumeid="+volumeId+"&response=json",
								dataType: "json",
								success: function(json) {							                    					                    				                				                
									$("body").everyTime(5000, timerKey, function() {									    
										$.ajax({
											data: "command=queryAsyncJobResult&jobId="+json.createsnapshotresponse.jobid+"&response=json",
											dataType: "json",
											success: function(json) {												    							                
												var result = json.queryasyncjobresultresponse;										           
												if (result.jobstatus == 0) {
												    if(result.jobprocstatus == 1) 
												        loadingImg.find(".adding_text").text("Backing up....");													    																						                    
													return; //Job has not completed
												} else {
													$("body").stopTime(timerKey);													
													if (result.jobstatus == 1) {
														//Succeeded													
														template.find("#volume_action_snapshot_grid").click(); //pop down volume's snapshot grid																								
														loadingImg.hide();
														rowContainer.show(); 
														$("#dialog_info").html("<p>Snapshot was created successfully</p>").dialog("open");																		                                												            
													} else if (result.jobstatus == 2) {
														loadingImg.hide();
														rowContainer.show(); 
														$("#dialog_alert").html("<p>" + fromdb(result.jobresult) + "</p>").dialog("open");
													}
												}
											},
											error: function(XMLHttpResponse) {											    
												$("body").stopTime(timerKey);										                
												loadingImg.hide(); 								                            
												rowContainer.show(); 
												handleError(XMLHttpResponse);
											}
										});
									}, 0);
								},
								error: function(XMLHttpResponse) {									   			                    			                    
									loadingImg.hide(); 								                            
									rowContainer.show(); 
									handleError(XMLHttpResponse);
								}
							});						
					    },
					    "Cancel": function() { 					        
						    $(this).dialog("close"); 
					    } 
				    }).dialog("open");	       
			        break;     
			   
			   case "volume_action_recurring_snapshot": 
			        var dialogBox = $("#dialog_recurring_snapshot"); 					
					clearTopPanel();
					$.ajax({
	                    data: "command=listSnapshotPolicies&volumeid="+volumeId+"&response=json",
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
			        break;
			      
			   case "volume_action_snapshot_grid" :			        
					var expanded = link.data("expanded");
					if (expanded == null || expanded == false) {										
						$.ajax({
							cache: false,
							data: "command=listSnapshots&volumeid="+volumeId+"&response=json",
							dataType: "json",
							success: function(json) {							    
								var items = json.listsnapshotsresponse.snapshot;																						
								if (items != null && items.length > 0) {									    
								    var grid = template.find("#volume_snapshot_grid").empty();																	
									for (var i = 0; i < items.length; i++) {			
									    var newTemplate = $("#volume_snapshot_detail_template").clone(true);
				                        volumeSnapshotJSONToTemplate(items[i], newTemplate); 
				                        grid.append(newTemplate.show());																	
									}
								}
								link.removeClass().addClass("vm_botactionslinks_up");
								template.find("#volume_snapshot_detail_panel").slideDown("slow");
								
								link.data("expanded", true);
							}
						});
					} else {
						link.removeClass().addClass("vm_botactionslinks_down");
						template.find("#volume_snapshot_detail_panel").slideUp("slow");
						link.data("expanded", false);
					}
					break;			        
			        
			    default :
				    break;
		    }
		    return false;
	    });		
	    // *** volume template - event binding (end) ****************************************
			
		// *** volume's snapshot template - event binding (begin) ***************************		
		$("#volume_snapshot_detail_template").bind("click", function(event) {  
		    event.preventDefault();
	        event.stopPropagation();
	 
	        var template = $(this);
	        var snapshotId = template.data("snapshotId");	         
	        var target = event.target.id;
	        switch(target) {
	             case "volume_snapshot_action_create_volume":
	                 $("#dialog_add_volume_from_snapshot")
	                 .dialog("option", "buttons", {
	                     "Add": function() {	
	                         var thisDialog = $(this);	 
	                         	                                               
	                         var isValid = true;					
					         isValid &= validateString("Name", thisDialog.find("#name"), thisDialog.find("#name_errormsg"));					          		
					         if (!isValid) return;                  	                                             
	                         
	                         var name = thisDialog.find("#name").val();	                       
	                         thisDialog.dialog("close");	
	                         	                         
	                         var submenuContent = $("#submenu_content_volume");						
				             var template = $("#volume_template").clone(true);					             
				             var loadingImg = template.find(".adding_loading");		
	                         var rowContainer = template.find("#row_container");                        
                             loadingImg.find(".adding_text").text("Adding....");	
                             loadingImg.show();  
                             rowContainer.hide();	                      
                             submenuContent.find("#grid_content").prepend(template);	 
                             template.fadeIn("slow");	                                  
	                                                
	                         $.ajax({
						         data: "command=createVolume&snapshotid="+snapshotId+"&name="+name+"&response=json",
						         dataType: "json",
						         success: function(json) {							           								 
							        var jobId = json.createvolumeresponse.jobid;					        
					                var timerKey = "createVolumeJob"+jobId;        					        
                                    $("body").everyTime(2000, timerKey, function() {
							            $.ajax({
								            data: "command=queryAsyncJobResult&jobId="+json.createvolumeresponse.jobid+"&response=json",
								            dataType: "json",
								            success: function(json) {										       						   
									            var result = json.queryasyncjobresultresponse;									           
									            if (result.jobstatus == 0) {
										            return; //Job has not completed
									            } else {											    
										            $("body").stopTime(timerKey);
										            if (result.jobstatus == 1) {
											            // Succeeded	
											            volumeJSONToTemplate(result.volume[0], template);												    
											            changeGridRowsTotal(submenuContent.find("#grid_rows_total"), 1);   
											            		                                                               
                                                        loadingImg.hide(); 	                                                                                          
                                                        var createdSuccessfullyImg = template.find("#created_successfully").show();	
                                                        createdSuccessfullyImg.find("#close_button").bind("click", function() {
                                                            createdSuccessfullyImg.hide();
                                                            rowContainer.show(); 
                                                        });	                                                                     											                                                               
										            } else if (result.jobstatus == 2) {										                
											            $("#dialog_alert").html("<p>" + fromdb(result.jobresult) + "</p>").dialog("open");												            
											            template.slideUp("slow", function() {
													        $(this).remove();
												        });												            										    				    
										            }
									            }
								            },
								            error: function(XMLHttpResponse) {								                
									            $("body").stopTime(timerKey);									           
									            handleError(XMLHttpResponse);										            								            
									            template.slideUp("slow", function() {
											        $(this).remove();
										        });									            								    
								            }
							            });
						            }, 0);							 
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
	                 
	                 break;
	                 
	             case "volume_snapshot_action_delete_snapshot":	   	 
	                 var thisDialog = $(this);	 
	                 thisDialog.dialog("close");
	             
	                 var loadingImg = template.find(".adding_loading");		
                     var rowContainer = template.find("#row_container");                           
                     loadingImg.find(".adding_text").text("Deleting snapshot....");	
                     loadingImg.show();  
                     rowContainer.hide();	   
	                         
	                 $.ajax({
						 data: "command=deleteSnapshot&id="+snapshotId+"&response=json",
						 dataType: "json",
						 success: function(json) {											 
							var jobId = json.deletesnapshotresponse.jobid;					        
					        var timerKey = "deleteSnapshotJob"+jobId;
					        
                            $("body").everyTime(2000, timerKey, function() {
							    $.ajax({
								    data: "command=queryAsyncJobResult&jobId="+json.deletesnapshotresponse.jobid+"&response=json",
								    dataType: "json",
								    success: function(json) {										       						   
									    var result = json.queryasyncjobresultresponse;									    
									    if (result.jobstatus == 0) {
										    return; //Job has not completed
									    } else {											    
										    $("body").stopTime(timerKey);
										    if (result.jobstatus == 1) {
											    // Succeeded										    
											    loadingImg.hide(); 		
									            rowContainer.show(); 											    											   
											    template.slideUp("slow", function() {
													$(this).remove();
												});		                                                           
										    } else if (result.jobstatus == 2) {
										        loadingImg.hide(); 		
									            rowContainer.show(); 
											    $("#dialog_alert").html("<p>" + fromdb(result.jobresult) + "</p>").dialog("open");											    					    
										    }
									    }
								    },
								    error: function(XMLHttpResponse) {								        
									    $("body").stopTime(timerKey);
									    loadingImg.hide(); 		
									    rowContainer.show(); 
									    handleError(XMLHttpResponse);									    
								    }
							    });
						    }, 0);							 
						 },
						 error: function(XMLHttpResponse) {
						     loadingImg.hide(); 		
							 rowContainer.show(); 
							 handleError(XMLHttpResponse);	
						 }
					 });                   
	                 break;
	                 
	             case "volume_snapshot_action_create_template":
	                 $("#dialog_create_template_from_snapshot")
	                 .dialog("option", "buttons", {
	                     "Add": function() {	
	                         var thisDialog = $(this);	 	                                                                        
	                         var isValid = true;					
					         isValid &= validateString("Name", thisDialog.find("#name"), thisDialog.find("#name_errormsg"), false);		
					         isValid &= validateString("Display Text", thisDialog.find("#display_text"), thisDialog.find("#display_text_errormsg"), false);				         		          		
					         if (!isValid) return;                  	                                             
	                         
	                         var name = thisDialog.find("#name").val();	 
	                         var displayText = thisDialog.find("#display_text").val();	 
	                         var osTypeId = thisDialog.find("#os_type").val(); 	    
							 var password = thisDialog.find("#password").val();
	                         thisDialog.dialog("close");	
	                         		     	                                                         	                                                  						
							 var loadingImg = template.find(".adding_loading");							
							 var rowContainer = template.find("#row_container");
							 loadingImg.find(".adding_text").text("Creating template....");				            
							 loadingImg.fadeIn("slow");
				             rowContainer.hide(); 	                                  
	                                                    
	                         $.ajax({
						         data: "command=createTemplate&snapshotid="+snapshotId+"&name="+name+"&displaytext="+displayText+"&ostypeid="+osTypeId+"&passwordEnabled="+password+"&response=json",
						         dataType: "json",
						         success: function(json) {							            					           								 
							        var jobId = json.createtemplateresponse.jobid;					        
					                var timerKey = "createTemplateJob"+jobId;        					        
                                    $("body").everyTime(2000, timerKey, function() {
							            $.ajax({
								            data: "command=queryAsyncJobResult&jobId="+json.createtemplateresponse.jobid+"&response=json",
								            dataType: "json",
								            success: function(json) {									                							       						   
									            var result = json.queryasyncjobresultresponse;									           
									            if (result.jobstatus == 0) {
										            return; //Job has not completed
									            } else {											    
										            $("body").stopTime(timerKey);
										            if (result.jobstatus == 1) {
											            // Succeeded	
											            loadingImg.hide();
														rowContainer.show(); 
														$("#dialog_info").html("<p>Template was created successfully</p>").dialog("open");	                                                                
                                                    } else if (result.jobstatus == 2) {		                                                    
                                                        loadingImg.hide();
														rowContainer.show(); 
														$("#dialog_alert").html("<p>" + fromdb(result.jobresult) + "</p>").dialog("open");            											            										    				    
										            }
									            }
								            },
								            error: function(XMLHttpResponse) {								                
									            $("body").stopTime(timerKey);
									            loadingImg.hide();
												rowContainer.show(); 									           
									            handleError(XMLHttpResponse);           								            								    
								            }
							            });
						            }, 0);							 
						         },
						         error: function(XMLHttpResponse) {								         
						             loadingImg.hide();
									 rowContainer.show(); 					            
									 handleError(XMLHttpResponse);								 								 
						         }
					         });                      
	                     },
	                     "Cancel": function() {	                         
	                         $(this).dialog("close");
	                     }	                     
	                 }).dialog("open");	                 
	                 break;                 
	        }   
		});		
		// *** volume's snapshot template - event binding (end) *****************************	
				
		function volumeSnapshotJSONToTemplate(json, template) {			   
		    template.addClass("smallrow_even");		 
			      		    	    		    
		    template.attr("id", "volume_snapshot_"+json.id).data("snapshotId", json.id).data("volumeId", json.volumeid);	   
		    template.find("#id").text(json.id);
		    template.find("#name").text(json.name);
		    template.find("#volume").text(json.volumename);	
		    template.find("#interval_type").text(json.intervaltype);
		    template.find("#account").text(json.account);
		    template.find("#domain").text(json.domain);		   		    
		       
		    setDateField(json.created, template.find("#created"));  		   
		}	
		
		function snapshotJSONToTemplate(json, template) {   	             
	        (index++ % 2 == 0)? template.addClass("smallrow_even"): template.addClass("smallrow_odd");		
		   			      		    	    		    
		    template.attr("id", "snapshot_"+json.id).data("snapshotId", json.id);	   
		    template.find("#id").text(json.id);
		    template.find("#name").text(json.name);			      
		    template.find("#volume").text(json.volumename);	
		    template.find("#interval_type").text(json.intervaltype);	    		   
		    template.find("#account").text(json.account);
		    template.find("#domain").text(json.domain);
		    
		    setDateField(json.created, template.find("#created"));		    
	    }	
    }
    
    if (isAdmin()) {  	   
        populateZoneField(true);
        populateDiskOfferingField();  
        populateOSTypeField();  		
		
	    // *** Primary Storage (begin) ***
	    
	    function poolJSONToTemplate(json, template) {
	        template.attr("id", "pool"+json.id);
	    
		    if (index++ % 2 == 0) {
			    template.find("#row_container").addClass("smallrow_even");
		    } else {
			    template.find("#row_container").addClass("smallrow_odd");
		    }
	
		    template.data("id", json.id).data("name", fromdb(json.name));
		    template.find("#pool_id").text(json.id);
		    template.find("#pool_name").text(json.name);
		    template.find("#pool_zone").text(json.zonename);
		    template.find("#pool_pod").text(json.podname);
		    template.find("#pool_cluster").text(json.clustername);
		    template.find("#pool_type").text(json.type);
		    template.find("#pool_ip").text(json.ipaddress);
		    template.find("#pool_path").text(json.path);
		    template.find("#pool_tags").text(json.tags);
		    
		    var statHtml = "<strong> Disk Total:</strong> " +convertBytes(json.disksizetotal)+" | <strong>Disk Allocated:</strong> " + convertBytes(json.disksizeallocated);
		    template.find("#pool_statistics").html(statHtml); 			
			    						        
		    /*
		    var statHtml = "<div class='hostcpu_icon'></div><p><strong> Disk Total:</strong> " +convertBytes(json.disksizetotal)+" | <strong>Disk Allocated:</strong> " + json.disksizeallocated + "</p>";
		    template.find("#storage_disk_stat").html(statHtml);
			
		    // State
		    if (json.state == 'Up') {
			    template.find("#storage_state_bar").removeClass("yellow_statusbar grey_statusbar red_statusbar").addClass("green_statusbar ");
			    template.find("#storage_state").text(json.state).removeClass("grid_celltitles grid_stoppedtitles").addClass("grid_runningtitles");
			    template.find(".grid_links").find("#storage_action_cancel_maint_container, #storage_action_remove_container").hide();
		    } else if (json.state == 'Down' || json.state == "Alert") {
			    template.find("#storage_state_bar").removeClass("yellow_statusbar grey_statusbar green_statusbar").addClass("red_statusbar");
			    template.find("#storage_state").text(json.state).removeClass("grid_celltitles grid_runningtitles").addClass("grid_stoppedtitles");
				
			    if (json.state == "Alert") {
				    template.find(".grid_links").find("#storage_action_reconnect_container, #storage_action_enable_maint_container, #storage_action_cancel_maint_container, #storage_action_remove_container").hide();
			    } else {
				    template.find(".grid_links").find("#storage_action_reconnect_container, #storage_action_cancel_maint_container, #storage_action_remove_container").hide();
			    }
		    } else {
			    template.find("#storage_state_bar").removeClass("yellow_statusbar green_statusbar red_statusbar").addClass("grey_statusbar");
			    template.find("#storage_state").text(json.state).removeClass("grid_runningtitles grid_stoppedtitles").addClass("grid_celltitles ");
				
			    if (json.state == "ErrorInMaintenance") {
				    template.find(".grid_links").find("#storage_action_reconnect_container, #storage_action_remove_container").hide();
			    } else if (json.state == "PrepareForMaintenance") {
				    template.find(".grid_links").find("#storage_action_reconnect_container, #storage_action_enable_maint_container, #storage_action_remove_container").hide();
			    } else if (json.state == "Maintenance") {
				    template.find(".grid_links").find("#storage_action_reconnect_container, #storage_action_enable_maint_container, #storage_action_cancel_maint_container").hide();
			    } else if (json.state == "Disconnected") {
				    template.find(".grid_links").find("#storage_action_reconnect_container, #storage_action_enable_maint_container, #storage_action_cancel_maint_container, #storage_action_remove_container").hide();
			    } else {
				    alert("Unsupported Host State: " + json.state);
			    }
		    } */
	    }
		
	    // Dialog Setup
	    activateDialog($("#dialog_add_pool").dialog({ 
		    autoOpen: false,
		    modal: true,
		    zIndex: 2000
	    }));
		
		// if hypervisor is KVM, limit the server option to NFS for now
		if (getHypervisorType() == 'kvm') {
			$("#dialog_add_pool").find("#add_pool_protocol").empty().html('<option value="nfs">NFS</option>');
		}
		
		$("#dialog_add_pool").find("#pool_zone").bind("change", function(event) {
			var zoneId = $(this).val();
			$.ajax({
				data: "command=listPods&zoneId="+zoneId+"&response=json",
				dataType: "json",
				async: false,
				success: function(json) {
					var pods = json.listpodsresponse.pod;
					var podSelect = $("#dialog_add_pool").find("#pool_pod").empty();	
					if (pods != null && pods.length > 0) {
					    for (var i = 0; i < pods.length; i++) {
						    podSelect.append("<option value='" + pods[i].id + "'>" + fromdb(pods[i].name) + "</option>"); 
					    }
					}
					$("#dialog_add_pool").find("#pool_pod").change();
				}
			});
		});
		
		$("#dialog_add_pool").find("#pool_pod").bind("change", function(event) {			     
		    var podId = $(this).val();
		    if(podId == null || podId.length == 0)
		        return;		
		    var clusterSelect = $("#dialog_add_pool").find("#pool_cluster").empty();			            
		    $.ajax({
		        data: "command=listClusters&response=json&podid=" + podId,
		        dataType: "json",
		        success: function(json) {				                        
		            var items = json.listclustersresponse.cluster;
		            if(items != null && items.length > 0) {				                		                
		                for(var i=0; i<items.length; i++) 			                    
		                    clusterSelect.append("<option value='" + items[i].id + "'>" + items[i].name + "</option>");		                
		            }			            
		        }
		    });		    
		});
		
		function nfsURL(server, path) {
		    var url;
		    if(server.indexOf("://")==-1)
			    url = "nfs://" + server + path;
			else
			    url = server + path;
			return url;
		}
		
		function iscsiURL(server, iqn, lun) {
		    var url;
		    if(server.indexOf("://")==-1)
			    url = "iscsi://" + server + iqn + "/" + lun;
			else
			    url = server + iqn + "/" + lun;
			return url;
		}
		
	    // Add New Primary Storage
		
		$("#dialog_add_pool #add_pool_protocol").change(function(event) {
			if ($(this).val() == "iscsi") {
				$("#dialog_add_pool #add_pool_path_container").hide();
				$("#dialog_add_pool #add_pool_iqn_container, #dialog_add_pool #add_pool_lun_container").show();
			} else {
				$("#dialog_add_pool #add_pool_path_container").show();
				$("#dialog_add_pool #add_pool_iqn_container, #dialog_add_pool #add_pool_lun_container").hide();
			}
		});
		
		$("#pool_template").bind("click", function(event) {		                  
			var template = $(this);				
			var id = template.data("id");	
			var name = template.data("name");
			var submenuContent = $("#submenu_content_pool");	
			switch(event.target.id) {
			    case "delete_link": 
					$("#dialog_confirmation")
					.html("Please confirm the deletion of your primary storage: " + name)
					.dialog('option', 'buttons', { 						
						"Confirm": function() { 
							$(this).dialog("close"); 
							$.ajax({
								data: "command=deleteStoragePool&id="+id+"&response=json",
								dataType: "json",
								success: function(json) {							   
									template.slideUp("slow", function() { 				   
										$(this).remove(); 
										changeGridRowsTotal(submenuContent.find("#grid_rows_total"), -1);
									});
								}
							});   
						}, 
						"Cancel": function() { 
							$(this).dialog("close"); 
						} 
					}).dialog("open");
					break;
			}
			return false;  //event.preventDefault() + event.stopPropogation()
		});  
		
	    $("#storage_action_new_pool").bind("click", function(event) {
		    $("#dialog_add_pool")
		    .dialog('option', 'buttons', { 				    
			    "Add": function() { 	
			    	var thisDialog = $(this);
			    	
				    // validate values
					var protocol = thisDialog.find("#add_pool_protocol").val();
					
				    var isValid = true;						    
				    if($("#dialog_add_pool #pool_cluster_container").css("display") != "none")	//if HypervisorType is "kvm", cluster field is hidden. Then, shouldn't validate it.				    
				        isValid &= validateDropDownBox("Cluster", thisDialog.find("#pool_cluster"), thisDialog.find("#pool_cluster_errormsg"), false);  //required, reset error text					    				
				    isValid &= validateString("Name", thisDialog.find("#add_pool_name"), thisDialog.find("#add_pool_name_errormsg"));
				    isValid &= validateString("Server", thisDialog.find("#add_pool_nfs_server"), thisDialog.find("#add_pool_nfs_server_errormsg"));	
					if (protocol == "nfs") {
						isValid &= validateString("Path", thisDialog.find("#add_pool_path"), thisDialog.find("#add_pool_path_errormsg"));	
					} else {
						isValid &= validateString("Target IQN", thisDialog.find("#add_pool_iqn"), thisDialog.find("#add_pool_iqn_errormsg"));	
						isValid &= validateString("LUN #", thisDialog.find("#add_pool_lun"), thisDialog.find("#add_pool_lun_errormsg"));	
					}
					isValid &= validateString("Tags", thisDialog.find("#add_pool_tags"), thisDialog.find("#add_pool_tags_errormsg"), true);	//optional
				    if (!isValid) return;
					    					
					var submenuContent = $("#submenu_content_pool");    					    					
					var template = $("#pool_template").clone(true).attr("id", "pool"+(new Date().getTime()));  //set a temporary Id to make the template unique before it gets a real Id.	
				    var loadingImg = template.find(".adding_loading");		
                    var rowContainer = template.find("#row_container");    	                               
                    loadingImg.find(".adding_text").text("Adding....");	
                    loadingImg.show();  
                    rowContainer.hide();                                   
                    submenuContent.find("#grid_content").prepend(template.fadeIn("slow"));       					
					
					var array1 = [];
				    var name = trim(thisDialog.find("#add_pool_name").val());
				    array1.push("&name="+encodeURIComponent(name));
				    
				    var server = trim(thisDialog.find("#add_pool_nfs_server").val());	
				    	
				    var zoneId = thisDialog.find("#pool_zone").val();	
				    array1.push("&zoneId="+zoneId);
				    
				    //if HypervisorType is "kvm", cluster field is hidden.				    
				    if($("#dialog_add_pool #pool_cluster_container").css("display") != "none") { 
				        var clusterId = thisDialog.find("#pool_cluster").val();
				        array1.push("&clusterid="+clusterId);
				    }
				    
					var podId = thisDialog.find("#pool_pod").val();
					array1.push("&podId="+podId);
					
					var url = null;
					if (protocol == "nfs") {
						var path = trim(thisDialog.find("#add_pool_path").val());
						if(path.substring(0,1)!="/")
							path = "/" + path; 
						url = nfsURL(server, path);
					} else {
						var iqn = trim(thisDialog.find("#add_pool_iqn").val());
						if(iqn.substring(0,1)!="/")
							iqn = "/" + iqn; 
						var lun = trim(thisDialog.find("#add_pool_lun").val());
						url = iscsiURL(server, iqn, lun);
					}
					array1.push("&url="+encodeURIComponent(url));
					
				    var tags = trim(thisDialog.find("#add_pool_tags").val());
					if(tags != null && tags.length > 0)
					    array1.push("&tags="+encodeURIComponent(tags));						
										    
				    thisDialog.dialog("close");
				    
				    $.ajax({
					    data: "command=createStoragePool&response=json" + array1.join(""),
					    dataType: "json",
					    success: function(json) {
						    var json = json.createstoragepoolresponse;							    
						    poolJSONToTemplate(json.storagepool[0], template);							    							    
						    changeGridRowsTotal(submenuContent.find("#grid_rows_total"), 1);
						    loadingImg.hide();  
                            rowContainer.show();    
					    },			
                        error: function(XMLHttpResponse) {		                   
	                        handleError(XMLHttpResponse);	
	                        template.slideUp("slow", function(){ $(this).remove(); } );							    
                        }							    
				    });
			    }, 
			    "Cancel": function() { 
				    $(this).dialog("close"); 
			    } 
		    }).dialog("open");
		    return false;
	    });
		    			
		function listStoragePools() {   
		    var submenuContent = $("#submenu_content_pool");
		     
           	var commandString;            
		    var advanced = submenuContent.find("#search_button").data("advanced");                    
		    if (advanced != null && advanced) {		
		        var name = submenuContent.find("#advanced_search #adv_search_name").val();				     
		        var zone = submenuContent.find("#advanced_search #adv_search_zone").val();	
		        var ip = submenuContent.find("#advanced_search #adv_search_ip").val();		
		        var path = submenuContent.find("#advanced_search #adv_search_path").val();				      
		        var moreCriteria = [];								
			    if (name!=null && trim(name).length > 0) 
				    moreCriteria.push("&name="+encodeURIComponent(trim(name)));					   
		        if (zone!=null && zone.length > 0) 
				    moreCriteria.push("&zoneId="+zone);	
				if (ip!=null && trim(ip).length > 0) 
				    moreCriteria.push("&ipaddress="+encodeURIComponent(trim(ip)));		
				if (path!=null && trim(path).length > 0) 
				    moreCriteria.push("&path="+encodeURIComponent(trim(path)));						       	
			    commandString = "command=listStoragePools&page="+currentPage+moreCriteria.join("")+"&response=json";
		    } else {          			
                var searchInput = submenuContent.find("#search_input").val();            
                if (searchInput != null && searchInput.length > 0) 
                    commandString = "command=listStoragePools&page="+currentPage+"&keyword="+searchInput+"&response=json"
                else
                    commandString = "command=listStoragePools&page="+currentPage+"&response=json";
            }
           	
           	//listItems(submenuContent, commandString, jsonResponse1, jsonResponse2, template, fnJSONToTemplate);         
            listItems(submenuContent, commandString, "liststoragepoolsresponse", "storagepool", $("#pool_template"), poolJSONToTemplate);               
		}
			
		submenuContentEventBinder($("#submenu_content_pool"), listStoragePools);	
			
	    $("#submenu_pool").bind("click", function(event) {  		       
		    event.preventDefault();
		    
		    currentSubMenu.addClass("submenu_links_off").removeClass("submenu_links_on");		
		    $(this).addClass("submenu_links_on").removeClass("submenu_links_off");
		    currentSubMenu = $(this); 
		    
		    $("#submenu_content_pool").show();
		    $("#submenu_content_storage").hide();
		    $("#submenu_content_volume").hide();
		    $("#submenu_content_snapshot").hide();
            
            currentPage = 1;
		    listStoragePools();
	    });	    		
	    // *** Primary Storage (end) ***
		
		
		
		
		
			
	    // *** Secondary Storage (begin) ***				
	    // Add Secondary Storage Dialog (begin)
	    activateDialog($("#dialog_add_host").dialog({ 
		    autoOpen: false,
		    modal: true,
		    zIndex: 2000
	    }));		
	    $("#storage_action_new_host").bind("click", function(event) {
		    $("#dialog_add_host")
		    .dialog('option', 'buttons', { 				    
			    "Add": function() { 
			        var thisDialog = $(this);
			    
				    // validate values					
				    var isValid = true;							    
				    isValid &= validateString("NFS Server", thisDialog.find("#add_storage_nfs_server"), thisDialog.find("#add_storage_nfs_server_errormsg"));	
				    isValid &= validatePath("Path", thisDialog.find("#add_storage_path"), thisDialog.find("#add_storage_path_errormsg"));					
				    if (!isValid) return;
						
					var submenuContent = $("#submenu_content_storage");	  
					var template = $("#storage_template").clone(true);		
				    var loadingImg = template.find(".adding_loading");		
                    var rowContainer = template.find("#row_container");    	                               
                    loadingImg.find(".adding_text").text("Adding....");	
                    loadingImg.show();  
                    rowContainer.hide();                                   
                    submenuContent.find("#grid_content").prepend(template.fadeIn("slow"));    
					     					  								            				
				    var zoneId = thisDialog.find("#storage_zone").val();		
				    var nfs_server = trim(thisDialog.find("#add_storage_nfs_server").val());		
				    var path = trim(thisDialog.find("#add_storage_path").val());	    					    				    					   					
					var url = nfsURL(nfs_server, path);    					   					
				    
				    thisDialog.dialog("close");					  
				    $.ajax({
					    data: "command=addSecondaryStorage&zoneId="+zoneId+"&url="+encodeURIComponent(url)+"&response=json",
					    dataType: "json",
					    success: function(json) {								    						    
						    var secondaryStorage = json.addsecondarystorageresponse.secondarystorage[0];							  
						    storageJSONToTemplate(secondaryStorage, template);							    
						    changeGridRowsTotal(submenuContent.find("#grid_rows_total"), 1) ;
						    loadingImg.hide();  
                            rowContainer.show();    
					    },			
                        error: function(XMLHttpResponse) {		                   
	                        handleError(XMLHttpResponse);	
	                        template.slideUp("slow", function(){ $(this).remove(); } );							    
                        }					    			    
				    });
			    }, 
			    "Cancel": function() { 
				    $(this).dialog("close"); 
			    } 
		    }).dialog("open");
		    return false;
	    });
	    // Add Secondary Storage Dialog (end)
				
	    // FUNCTION: Storage JSON to Template
	    function storageJSONToTemplate(json, template) {
	        template.attr("id", "secondaryStorage_"+json.id).data("secondaryStorageId", json.id);
		    if (index++ % 2 == 0) {
			    template.find("#row_container").addClass("smallrow_even");
		    } else {
			    template.find("#row_container").addClass("smallrow_odd");
		    }
		    template.data("hostName", fromdb(json.name));
			template.find("#storage_type").text(json.type);
		    template.find("#storage_name").text(json.name);
			template.find("#storage_zone").text(json.zonename);
		    template.find("#storage_ip").text(json.ipaddress);
		    template.find("#storage_version").text(json.version);
		    
		    setDateField(json.disconnected, template.find("#storage_disconnected"));			   
	    }    		    	
		
		function listSecondaryStorage() {    	
		    var submenuContent = $("#submenu_content_storage");    		
			
        	var commandString;            
		    var advanced = submenuContent.find("#search_button").data("advanced");                    
		    if (advanced != null && advanced) {		
		        var name = submenuContent.find("#advanced_search #adv_search_name").val();	
		        //var state = submenuContent.find("#advanced_search #adv_search_state").val();
		        var zone = submenuContent.find("#advanced_search #adv_search_zone").val();			        
		        //var pod = submenuContent.find("#advanced_search #adv_search_pod").val();
		        var domainId = submenuContent.find("#advanced_search #adv_search_domain").val();
		        var moreCriteria = [];								
			    if (name!=null && trim(name).length > 0) 
				    moreCriteria.push("&name="+encodeURIComponent(trim(name)));				
			    //if (state!=null && state.length > 0) 
				//    moreCriteria.push("&state="+state);		
		        if (zone!=null && zone.length > 0) 
				    moreCriteria.push("&zoneId="+zone);		
		        //if (pod!=null && pod.length > 0) 
				//    moreCriteria.push("&podId="+pod);
				if (domainId!=null && domainId.length > 0) 
				    moreCriteria.push("&domainid="+domainId);				
			    commandString = "command=listHosts&type=SecondaryStorage&page="+currentPage+moreCriteria.join("")+"&response=json"; 
		    } else {    
                var searchInput = $("#submenu_content_storage #search_input").val();              
                if (searchInput != null && searchInput.length > 0) 
                    commandString = "command=listHosts&type=SecondaryStorage&page="+currentPage+"&keyword="+searchInput+"&response=json"
                else
                    commandString = "command=listHosts&type=SecondaryStorage&page="+currentPage+"&response=json";    
            }
            	
            //listItems(submenuContent, commandString, jsonResponse1, jsonResponse2, template, fnJSONToTemplate);         
            listItems(submenuContent, commandString, "listhostsresponse", "host", $("#storage_template"), storageJSONToTemplate);    	    
		}
			
		submenuContentEventBinder($("#submenu_content_storage"), listSecondaryStorage);	
						
	    $("#submenu_storage").bind("click", function(event) {   		
		    event.preventDefault();
		    
		    $(this).addClass("submenu_links_on").removeClass("submenu_links_off");
		    currentSubMenu.addClass("submenu_links_off").removeClass("submenu_links_on");
		    currentSubMenu = $(this);
		    
		    $("#submenu_content_storage").show();
		    $("#submenu_content_pool").hide();
		    $("#submenu_content_volume").hide();
		    $("#submenu_content_snapshot").hide();
			
			currentPage = 1;
		    listSecondaryStorage();
	    });
		
		$("#storage_template").bind("click", function(event) {		                  
			var template = $(this);				
			var id = template.data("secondaryStorageId");	
			var name = template.data("hostName");
			var submenuContent = $("#submenu_content_storage");	
			switch(event.target.id) {
			    case "delete_link":  
					$("#dialog_confirmation")
					.html("Please confirm the deletion of your secondary storage: " + name)
					.dialog('option', 'buttons', { 						
						"Confirm": function() { 
							$(this).dialog("close"); 
							$.ajax({
								data: "command=deleteHost&id="+id+"&response=json",
								dataType: "json",
								success: function(json) {							   
									template.slideUp("slow", function() { 				   
										$(this).remove(); 
										changeGridRowsTotal(submenuContent.find("#grid_rows_total"), -1);
									});
								}
							});    
						}, 
						"Cancel": function() { 
							$(this).dialog("close"); 
						} 
					}).dialog("open");
					break;
			}
			return false;  //event.preventDefault() + event.stopPropogation()
		});      			
	    // *** Secondary Storage (end) ***	
		    		
		
	    // *** Volume (begin) ***				
	    initializeVolumeTab(true);  
	    $("#volume_hostname_header, #volume_hostname_container, #volume_account_header, #volume_account_container, #snapshot_account_header, #snapshot_account_container, #snapshot_domain_header, #snapshot_domain_container, #volume_snapshot_account_header, #volume_snapshot_account_container, #volume_snapshot_domain_header, #volume_snapshot_domain_container").show();	   	    		
	    // *** Volume (end) ***	      		
		    	
		if(targetTab==null)  {  	
		    currentSubMenu = $("#submenu_pool");	
	        $("#submenu_pool").click();	  //default tab is Primary Storage page
	    }
	    else {
	        currentSubMenu = $("#"+targetTab);	
	        $("#"+targetTab).click(); 	   
	    }  
       
    } else {  //*** isAdmin()==false              
        $("#submenu_content_pool, #pool_template, #dialog_add_pool, #submenu_content_storage, #storage_template, #dialog_add_host, #submenu_pool, #submenu_storage").hide(); //hide Primary Storage tab, Secondary Storage tab 
                                                       	  	
        populateZoneField(false);    		
        populateDiskOfferingField();
        populateOSTypeField();  
            		 		
        // *** Volume (begin) ***				
        initializeVolumeTab(false);	 
        $("#volume_hostname_header, #volume_hostname_container, #volume_account_header, #volume_account_container, #snapshot_account_header, #snapshot_account_container, #snapshot_domain_header, #snapshot_domain_container, #volume_snapshot_account_header, #volume_snapshot_account_container, #volume_snapshot_domain_header, #volume_snapshot_domain_container").hide();	
        $("#volume_created_header, #volume_created_container").css("width", "30%");			
        // *** Volume (end) ***		        
        
        currentSubMenu = $("#submenu_volume"); //default tab is volume
	    $("#submenu_volume").click(); 	    	    
    }   
}
