function clickInstanceGroupHeader($arrowIcon) {   
    //***** VM Detail (begin) ******************************************************************************
    var $vmPopup
    var $rightPanelHeader;  
    var $rightPanelContent;   
    var $instanceGroupContainer = $("#leftmenu_instance_group_container");  
    var $instanceGroupTemplate = $("#leftmenu_instance_group_template");  
     
    var $actionLink = $("#action_link");
    var $actionMenu = $("#action_menu");
    var $actionList = $actionMenu.find("#action_list");
    var $midmenuContainer = $("#midmenu_container");
    var $midmenuItemVm = $("#midmenu_item_vm");    
    var $actionListItem = $("#action_list_item");
      
    var noGroupName = "(no group name)";             
    
    var listAPIMap = {
        listAPI: "listVirtualMachines",
        listAPIResponse: "listvirtualmachinesresponse",
        listAPIResponseObj: "virtualmachine"
    };           
      
    var actionMap = {        
        "Stop Instance": {
            api: "stopVirtualMachine",            
            isAsyncJob: true,
            asyncJobResponse: "stopvirtualmachineresponse",
            afterActionSeccessFn: setMidmenuItemVm
        },
        "Start Instance": {
            api: "startVirtualMachine",            
            isAsyncJob: true,
            asyncJobResponse: "startvirtualmachineresponse",
            afterActionSeccessFn: setMidmenuItemVm
        },
        "Reboot Instance": {
            api: "rebootVirtualMachine",           
            isAsyncJob: true,
            asyncJobResponse: "rebootvirtualmachineresponse",
            afterActionSeccessFn: setMidmenuItemVm
        },
        "Destroy Instance": {
            api: "destroyVirtualMachine",           
            isAsyncJob: true,
            asyncJobResponse: "destroyvirtualmachineresponse",
            afterActionSeccessFn: setMidmenuItemVm
        },
        "Restore Instance": {
            api: "recoverVirtualMachine",           
            isAsyncJob: false,
            afterActionSeccessFn: setMidmenuItemVm
        },
        "Attach ISO": {
            isAsyncJob: true,
            asyncJobResponse: "attachisoresponse",            
            dialogBeforeActionFn : doAttachISO,
            afterActionSeccessFn: setMidmenuItemVm   
        },
        "Detach ISO": {
            isAsyncJob: true,
            asyncJobResponse: "detachisoresponse",            
            dialogBeforeActionFn : doDetachISO,
            afterActionSeccessFn: setMidmenuItemVm   
        },
        "Reset Password": {
            isAsyncJob: true,
            asyncJobResponse: "resetpasswordforvirtualmachineresponse",            
            dialogBeforeActionFn : doResetPassword,
            afterActionSeccessFn: function(){}   
        },
        "Change Name": {
            isAsyncJob: false,            
            dialogBeforeActionFn : doChangeName,
            afterActionSeccessFn: setMidmenuItemVm
        },
        "Change Service": {
            isAsyncJob: true,
            asyncJobResponse: "changeserviceforvirtualmachineresponse",
            dialogBeforeActionFn : doChangeService,
            afterActionSeccessFn: setMidmenuItemVm
        }          
    }            
        
    function doAttachISO($t, selectedItemIds, listAPIMap) {   
        $.ajax({
		    data: createURL("command=listIsos&isReady=true"),
			dataType: "json",
			async: false,
			success: function(json) {
				var isos = json.listisosresponse.iso;
				var isoSelect = $("#dialog_attach_iso #attach_iso_select");
				if (isos != null && isos.length > 0) {
					isoSelect.empty();
					for (var i = 0; i < isos.length; i++) {
						isoSelect.append("<option value='"+isos[i].id+"'>"+sanitizeXSS(isos[i].displaytext)+"</option>");;
					}
				}
			}
		});
		
		$("#dialog_attach_iso")
		.dialog('option', 'buttons', { 						
			"Confirm": function() { 			    
				$(this).dialog("close");
				var isoId = $("#dialog_attach_iso #attach_iso_select").val();
				if (isoId == "none") {
					$("#dialog_alert").html("<p>There is no ISO file to attach to the virtual machine.</p>");
					$("#dialog_alert").dialog("open");
					return false;
				}	
				for(var id in selectedItemIds) {
				   var apiCommand = "command=attachIso&virtualmachineid="+id+"&id="+isoId;
				   doAction(id, $t, apiCommand, listAPIMap);	
				}			
			}, 
			"Cancel": function() { 
				$(this).dialog("close"); 
			} 
		}).dialog("open");
    }
   
    function doDetachISO($t, selectedItemIds, listAPIMap) {    
        $("#dialog_confirmation")
		.html("<p>Please confirm you want to detach an ISO from the virtual machine(s)</p>")
		.dialog('option', 'buttons', { 						
			"Confirm": function() { 
				$(this).dialog("close");				
				for(var id in selectedItemIds) {
				   var apiCommand = "command=detachIso&virtualmachineid="+id;
				   doAction(id, $t, apiCommand, listAPIMap);	
				}					
			}, 
			"Cancel": function() { 
				$(this).dialog("close"); 
			} 
		}).dialog("open");
    }
   
    function doResetPassword($t, selectedItemIds, listAPIMap) {   		
		$("#dialog_confirmation")
		.html("<p>Please confirm you want to change the ROOT password for your virtual machine(s)</p>")
		.dialog('option', 'buttons', { 						
			"Confirm": function() { 
				$(this).dialog("close"); 
				for(var id in selectedItemIds) {	
				    var $midMenuItem = selectedItemIds[id];
				    var jsonObj = $midMenuItem.data("jsonObj");
				    if(jsonObj.state != "Stopped") {				    
				        $midMenuItem.find("#info_icon").addClass("error").show();
                        $midMenuItem.data("afterActionInfo", ($t.data("label") + " action failed. Reason: This instance needs to be stopped before you can reset password"));  
			            continue;
		            }
		            if(jsonObj.passwordEnabled != "true") {
		                $midMenuItem.find("#info_icon").addClass("error").show();
                        $midMenuItem.data("afterActionInfo", ($t.data("label") + " action failed. Reason: This instance is not using a template that has the password reset feature enabled.  If you have forgotten your root password, please contact support."));  
			            continue;
		            }	
		            var apiCommand = "command=resetPasswordForVirtualMachine&id="+id;
		            doAction(id, $t, apiCommand, listAPIMap);	
		        }		
			}, 
			"Cancel": function() { 
				$(this).dialog("close"); 
			} 
		}).dialog("open");
	}
   
    function doChangeName($t, selectedItemIds, listAPIMap) { 
		$("#dialog_change_name")
		.dialog('option', 'buttons', { 						
			"Confirm": function() { 			    
			    var thisDialog = $(this);	
			    thisDialog.dialog("close"); 
			    											
				// validate values
		        var isValid = true;					
		        isValid &= validateString("Name", thisDialog.find("#change_instance_name"), thisDialog.find("#change_instance_name_errormsg"));								
		        if (!isValid) return;								
				
				var name = trim(thisDialog.find("#change_instance_name").val());
				
				for(var id in selectedItemIds) {
		           var apiCommand = "command=updateVirtualMachine&id="+id+"&displayName="+encodeURIComponent(escape(name));		     
		           doAction(id, $t, apiCommand, listAPIMap);
		        }	
			}, 
			"Cancel": function() { 
				$(this).dialog("close"); 
			} 
		}).dialog("open");
    }
   
    function doChangeService($t, selectedItemIds, listAPIMap) { 
		$.ajax({
		    //data: createURL("command=listServiceOfferings&VirtualMachineId="+vmId), //can not specifiy VirtualMachineId since we allow multiple-item-selection.
		    data: createURL("command=listServiceOfferings"), //can not specifiy VirtualMachineId since we support multiple-item-selection.
			dataType: "json",
			success: function(json) {
				var offerings = json.listserviceofferingsresponse.serviceoffering;
				var offeringSelect = $("#dialog_change_service_offering #change_service_offerings").empty();
				
				if (offerings != null && offerings.length > 0) {
					for (var i = 0; i < offerings.length; i++) {
						var option = $("<option value='" + offerings[i].id + "'>" + sanitizeXSS(unescape(offerings[i].displaytext)) + "</option>").data("name", sanitizeXSS(unescape(offerings[i].name)));
						offeringSelect.append(option); 
					}
				} 
			}
		});
		
		$("#dialog_change_service_offering")
		.dialog('option', 'buttons', { 						
			"Change": function() { 
			    var thisDialog = $(this);
				thisDialog.dialog("close"); 
				
				for(var id in selectedItemIds) {				
				    var $midMenuItem = selectedItemIds[id];
				    var jsonObj = $midMenuItem.data("jsonObj");				
				    if(jsonObj.state != "Stopped") {				    
				        $midMenuItem.find("#info_icon").addClass("error").show();
                        $midMenuItem.data("afterActionInfo", ($t.data("label") + " action failed. Reason: virtual instance needs to be stopped before you can change its service."));  
			            continue;
		            }
                    var apiCommand = "command=changeServiceForVirtualMachine&id="+id+"&serviceOfferingId="+thisDialog.find("#change_service_offerings").val();	     
                    doAction(id, $t, apiCommand, listAPIMap);
                }
			}, 
			"Cancel": function() { 
				$(this).dialog("close"); 
			} 
		}).dialog("open");
     }
   
   
    function updateVirtualMachineStateInRightPanel(state) {
        if(state == "Running")
            $rightPanelContent.find("#state").text(state).removeClass("red gray").addClass("green");
        else if(state == "Stopped")
            $rightPanelContent.find("#state").text(state).removeClass("green gray").addClass("red");
        else  //Destroyed, Creating, ~                                  
            $rightPanelContent.find("#state").text(state).removeClass("green red").addClass("gray");            			       
    }
    
    function updateVirtualMachineStateInMidMenu(jsonObj, midmenuItem) {         
        if(jsonObj.state == "Running")
            midmenuItem.find("#status_icon").attr("src", "images/status_green.png");
        else if(jsonObj.state == "Stopped")
            midmenuItem.find("#status_icon").attr("src", "images/status_red.png");
        else  //Destroyed, Creating, ~                                  
            midmenuItem.find("#status_icon").attr("src", "images/status_gray.png");
    }
      
    function setMidmenuItemVm(instance, $midmenuItemVm1) {  
        $midmenuItemVm1.data("jsonObj", instance);
        $midmenuItemVm1.data("toRightPanelFn", vmMidmenuItemToRightPanel);
        $midmenuItemVm1.attr("id", ("midmenuItemVm_"+instance.id));                             
        $midmenuItemVm1.data("id", instance.id);        
        var vmName = getVmName(instance.name, instance.displayname);
        $midmenuItemVm1.find("#vm_name").text(vmName);
        $midmenuItemVm1.find("#ip_address").text(instance.ipaddress);                                            
        updateVirtualMachineStateInMidMenu(instance, $midmenuItemVm1);        
        $midmenuItemVm1.bind("click", function(event) {  
            var $t = $(this);     
            vmMidmenuItemToRightPanel($t);	 
            return false;
        }); 
    }
    
    function vmClearRightPanel(jsonObj) {       
        $rightPanelHeader.find("#vm_name").text("");	
        updateVirtualMachineStateInRightPanel("");	
        $rightPanelContent.find("#ipAddress").text("");
        $rightPanelContent.find("#zoneName").text("");
        $rightPanelContent.find("#templateName").text("");
        $rightPanelContent.find("#serviceOfferingName").text("");		
        $rightPanelContent.find("#ha").hide();  
        $rightPanelContent.find("#created").text("");
        $rightPanelContent.find("#account").text("");
        $rightPanelContent.find("#domain").text("");
        $rightPanelContent.find("#hostName").text("");
        $rightPanelContent.find("#group").text("");	
        $rightPanelContent.find("#iso").hide();
    }
    
    function vmMidmenuItemToRightPanel($t) {
        if($t.find("#info_icon").css("display") != "none") {                
            $rightPanelContent.find("#after_action_info").text($t.data("afterActionInfo"));
            if($t.find("#info_icon").hasClass("error"))
                $rightPanelContent.find("#after_action_info_container").addClass("errorbox");
             else
                $rightPanelContent.find("#after_action_info_container").removeClass("errorbox");                                        
            $rightPanelContent.find("#after_action_info_container").show();                                         
        } 
        else {
            $rightPanelContent.find("#after_action_info").text("");
            $rightPanelContent.find("#after_action_info_container").hide();                
        }
        
        var jsonObj = $t.data("jsonObj");     
        var vmName = getVmName(jsonObj.name, jsonObj.displayname);        
        $rightPanelHeader.find("#vm_name").text(vmName);	
        updateVirtualMachineStateInRightPanel(jsonObj.state);	
        $rightPanelContent.find("#ipAddress").text(jsonObj.ipaddress);
        $rightPanelContent.find("#zoneName").text(jsonObj.zonename);
        $rightPanelContent.find("#templateName").text(jsonObj.templatename);
        $rightPanelContent.find("#serviceOfferingName").text(jsonObj.serviceofferingname);		                                			                                
        if(jsonObj.haenable == "true")
            $rightPanelContent.find("#ha").removeClass("cross_icon").addClass("tick_icon").show();
        else
            $rightPanelContent.find("#ha").removeClass("tick_icon").addClass("cross_icon").show();		                                
        $rightPanelContent.find("#created").text(jsonObj.created);
        $rightPanelContent.find("#account").text(jsonObj.account);
        $rightPanelContent.find("#domain").text(jsonObj.domain);
        $rightPanelContent.find("#hostName").text(jsonObj.hostname);
        $rightPanelContent.find("#group").text(jsonObj.group);	
        if(jsonObj.isoid != null && jsonObj.isoid.length > 0)
            $rightPanelContent.find("#iso").removeClass("cross_icon").addClass("tick_icon").show();
        else
            $rightPanelContent.find("#iso").removeClass("tick_icon").addClass("cross_icon").show();           
    }
    
    
  
    $("#add_link").show(); 
	if($arrowIcon.hasClass("close") == true) {
        $arrowIcon.removeClass("close").addClass("open");    
        $.ajax({
	        cache: false,
	        data: createURL("command=listVirtualMachines"),
	        dataType: "json",
	        success: function(json) {	
	            var instanceGroupMap = {};	       
	            var instanceGroupArray = [];						        		            
		        var instances = json.listvirtualmachinesresponse.virtualmachine;								
		        if (instances != null && instances.length > 0) {		
			        for (var i = 0; i < instances.length; i++) {
			            var group1 = instances[i].group;
			            if(group1 == null || group1.length == 0)
			                group1 = noGroupName;
			            if(group1 in instanceGroupMap) {
			                instanceGroupMap[group1].push(instances[i]);
			            }							        
			            else {
			                instanceGroupMap[group1] = [instances[i]];
			                instanceGroupArray.push(group1);
			            }	
			        }				    
		        }
		        for(var i=0; i < instanceGroupArray.length; i++) {
		            if(instanceGroupArray[i]!=null && instanceGroupArray[i].length>0) {
		        	    var $groupTemplate = $instanceGroupTemplate.clone().show();				        	            	
		                $groupTemplate.find("#group_name").text(instanceGroupArray[i]);
		                		                			                
		                $groupTemplate.bind("click", function(event) { 
		                    //$(this).removeClass("leftmenu_content").addClass("leftmenu_content_selected");			               
                            $("#midmenu_container").empty();
                            var groupName = $(this).find("#group_name").text();
                                                        
                            $.ajax({
	                            cache: false,
	                            data: createURL("command=listVirtualMachines"),
	                            dataType: "json",
	                            success: function(json) {		                                                             
	                                var instances = json.listvirtualmachinesresponse.virtualmachine;                               
                                    for(var i=0; i<instances.length;i++) {                
                                        var instance = instances[i];
                                        var $midmenuItemVm1 = $midmenuItemVm.clone();                                                                                                                               
                                        setMidmenuItemVm(instance, $midmenuItemVm1);  
                                        $("#midmenu_container").append($midmenuItemVm1.show());
                                    }    
	                            }
	                        });
                            
                            return false;
                        });			                
		                
		                $instanceGroupContainer.append($groupTemplate);
		            }
		        }
		        
		        //action menu			        
		        $("#action_link").show();
		        $actionList.empty();
		        for(var label in actionMap) {		
		            var apiInfo = actionMap[label];
		            var $listItem = $("#action_list_item").clone();
		            $actionList.append($listItem.show());
		            var $link = $listItem.find("#link").text(label);
		            $link.data("label", label);	  
		            $link.data("api", apiInfo.api);				                 
		            $link.data("isAsyncJob", apiInfo.isAsyncJob);
		            $link.data("asyncJobResponse", apiInfo.asyncJobResponse);		     
		            $link.data("afterActionSeccessFn", apiInfo.afterActionSeccessFn);
		            $link.data("dialogBeforeActionFn", apiInfo.dialogBeforeActionFn);
		            $link.bind("click", function(event) {	
		                $actionMenu.hide();    	 
		                var $t = $(this);   
		                var dialogBeforeActionFn = $t.data("dialogBeforeActionFn");
		                if(dialogBeforeActionFn == null) {		                   
		                    for(var id in selectedItemIds) {	
	                            var apiCommand = "command="+$t.data("api")+"&id="+id;                      
	                            doAction(id, $t, apiCommand, listAPIMap); 	
		                    }
		                }
		                else {
		                    dialogBeforeActionFn($t, selectedItemIds, listAPIMap);	
		                }
		                selectedItemIds = {}; //clear selected items for action	                          
		                return false;
		            });  
		        }
	        }
        });  
    }
    else if($arrowIcon.hasClass("open") == true) {
        $arrowIcon.removeClass("open").addClass("close");            
        $instanceGroupContainer.empty();   
    }	     
    //***** VM Detail (end) ********************************************************************************    
    $("#right_panel").load("jsp/tab_instance.jsp", function() {	
        $rightPanelHeader = $("#right_panel_header");			                                		                                
	    $rightPanelContent = $("#right_panel_content");	
	    
	    var $noDiskOfferingTemplate = $("#vm_popup_disk_offering_template_no");
        var $customDiskOfferingTemplate = $("#vm_popup_disk_offering_template_custom");
        var $existingDiskOfferingTemplate = $("#vm_popup_disk_offering_template_existing");
       	    	    
        activateDialog($("#dialog_attach_iso").dialog({ 
		    width: 600,
		    autoOpen: false,
		    modal: true,
		    zIndex: 2000
	    }));   
        
        activateDialog($("#dialog_change_name").dialog({ 
		    autoOpen: false,
		    modal: true,
		    zIndex: 2000
	    }));
        
        activateDialog($("#dialog_change_service_offering").dialog({ 
		    width: 600,
		    autoOpen: false,
		    modal: true,
		    zIndex: 2000
	    }));
        
        //***** VM Wizard (begin) ******************************************************************************
        $vmPopup = $("#vm_popup");
        var $serviceOfferingTemplate = $("#vm_popup_service_offering_template");
        var currentPageInTemplateGridInVmPopup =1;
	    var selectedTemplateTypeInVmPopup;  //selectedTemplateTypeInVmPopup will be set to "featured" when new VM dialog box opens
    	   	
	    $("#add_link").unbind("click").bind("click", function(event) {
            vmWizardOpen();			
		    $.ajax({
			    data: createURL("command=listZones&available=true"),
			    dataType: "json",
			    success: function(json) {
				    var zones = json.listzonesresponse.zone;					
				    var $zoneSelect = $vmPopup.find("#wizard_zone").empty();					
				    if (zones != null && zones.length > 0) {
					    for (var i = 0; i < zones.length; i++) {
						    $zoneSelect.append("<option value='" + zones[i].id + "'>" + sanitizeXSS(zones[i].name) + "</option>"); 
					    }
				    }				
				    listTemplatesInVmPopup();	
			    }
		    });
    		
		    $.ajax({
			    data: createURL("command=listServiceOfferings"),
			    dataType: "json",
			    async: false,
			    success: function(json) {
				    var offerings = json.listserviceofferingsresponse.serviceoffering;
				    var $container = $("#service_offering_container");
				    $container.empty();	
				    				    
				    //var checked = "checked";
				    if (offerings != null && offerings.length > 0) {						    
					    for (var i = 0; i < offerings.length; i++) {						    
						    //if (i != 0) 
						    //    checked = "";
						    
						    var $t = $serviceOfferingTemplate.clone();  						  
						    $t.find("input:radio[name=service_offering_radio]").val(offerings[i].id); 
						    $t.find("#name").text(sanitizeXSS(unescape(offerings[i].name)));
						    $t.find("#description").text(sanitizeXSS(unescape(offerings[i].displaytext))); 
						    
						    if (i > 0)
						        $t.find("input:radio[name=service_offering_radio]").removeAttr("checked");	
						
						    //if(i == 0)
						    //    $t.find("input:radio[name=service_offering_radio]").attr("checked", true);
						    //var listItem = $("<li><input class='radio' type='radio' name='service' id='service' value='"+offerings[i].id+"'" + checked + "/><label style='width:500px;font-size:11px;' for='service'>"+sanitizeXSS(unescape(offerings[i].displaytext))+"</label></li>");
						    $container.append($t.show());	
					    }
					    //Safari and Chrome are not smart enough to make checkbox checked if html markup is appended by JQuery.append(). So, the following 2 lines are added.		
				        var html_all = $container.html();        
                        $container.html(html_all); 
				    }
			    }
			});
			    			    
		
		    $.ajax({
			    data: createURL("command=listDiskOfferings&domainid=1"),
			    dataType: "json",
			    async: false,
			    success: function(json) {
				    var offerings = json.listdiskofferingsresponse.diskoffering;			
				    var $dataDiskOfferingContainer = $("#data_disk_offering_container").empty();
			        var $rootDiskOfferingContainer = $("#root_disk_offering_container").empty();
			        
			        //***** data disk offering: "no, thanks", "custom", existing disk offerings in database (begin) ****************************************************
			        //"no, thanks" radio button (default radio button in data disk offering)		               
		            var $t = $noDiskOfferingTemplate.clone(); 		            	     
		            $t.find("input:radio").attr("name","data_disk_offering_radio");  
		            $t.find("#name").text("no, thanks"); 		            
		            $dataDiskOfferingContainer.append($t.show()); 
			        
			        //"custom" radio button			        
			        var $t = $customDiskOfferingTemplate.clone();  			        
			        $t.find("input:radio").attr("name","data_disk_offering_radio").removeAttr("checked");	
			        $t.find("#name").text("custom:");	  			         
			        $dataDiskOfferingContainer.append($t.show());	
			        
			        //existing disk offerings in database
			        if (offerings != null && offerings.length > 0) {						    
				        for (var i = 0; i < offerings.length; i++) {	
					        var $t = $existingDiskOfferingTemplate.clone(); 
					        $t.find("input:radio").attr("name","data_disk_offering_radio").val(offerings[i].id).removeAttr("checked");	 	
					        $t.find("#name").text(sanitizeXSS(unescape(noNull(offerings[i].name))));
					        $t.find("#description").text(sanitizeXSS(noNull(unescape(offerings[i].displaytext)))); 	 
					        $dataDiskOfferingContainer.append($t.show());	
				        }
			        }
			        
			        //Safari and Chrome are not smart enough to make checkbox checked if html markup is appended by JQuery.append(). So, the following 2 lines are added.		
		            var html_all = $dataDiskOfferingContainer.html();        
                    $dataDiskOfferingContainer.html(html_all);                     
			        //***** data disk offering: "no, thanks", "custom", existing disk offerings in database (end) *******************************************************
			        		        	
			        //***** root disk offering: "custom", existing disk offerings in database (begin) *******************************************************************
			        //"custom" radio button			        
			        var $t = $customDiskOfferingTemplate.clone();  			        
			        $t.find("input:radio").attr("name","root_disk_offering_radio").val("custom");	
			        if (offerings != null && offerings.length > 0) //default is the 1st existing disk offering. If there is no existing disk offering, default to "custom" radio button
			            $t.find("input:radio").removeAttr("checked");	
			        $t.find("#name").text("custom:");	  			         
			        $rootDiskOfferingContainer.append($t.show());	
			        
			        //existing disk offerings in database
			        if (offerings != null && offerings.length > 0) {						    
				        for (var i = 0; i < offerings.length; i++) {	
					        var $t = $existingDiskOfferingTemplate.clone(); 
					        $t.find("input:radio").attr("name","root_disk_offering_radio").val(offerings[i].id);	 
					        if(i > 0) //default is the 1st existing disk offering. If there is no existing disk offering, default to "custom" radio button
					            $t.find("input:radio").removeAttr("checked");	 	
					        $t.find("#name").text(sanitizeXSS(unescape(noNull(offerings[i].name))));
					        $t.find("#description").text(sanitizeXSS(noNull(unescape(offerings[i].displaytext)))); 	 
					        $rootDiskOfferingContainer.append($t.show());	
				        }
			        }
				    
				    //Safari and Chrome are not smart enough to make checkbox checked if html markup is appended by JQuery.append(). So, the following 2 lines are added.		
		            var html_all = $rootDiskOfferingContainer.html();        
                    $rootDiskOfferingContainer.html(html_all);                         
				    //***** root disk offering: "custom", existing disk offerings in database (end) *********************************************************************
				    
				    
				    
				    
				    /*
				    $("#wizard_root_disk_offering, #wizard_data_disk_offering").empty();
												
			        var html = 
				    "<li>"
					    +"<input class='radio' type='radio' name='datadisk' id='datadisk' value='' checked/>"
					    +"<label style='width:500px;font-size:11px;' for='disk'>No disk offering</label>"
			       +"</li>";
				    $("#wizard_data_disk_offering").append(html);							
												
				    if (offerings != null && offerings.length > 0) {								    
					    for (var i = 0; i < offerings.length; i++) {	
						    var html = 
							    "<li>"
								    +"<input class='radio' type='radio' name='rootdisk' id='rootdisk' value='"+offerings[i].id+"'" + ((i==0)?"checked":"") + "/>"
								    +"<label style='width:500px;font-size:11px;' for='disk'>"+sanitizeXSS(unescape(offerings[i].displaytext))+"</label>"
						       +"</li>";
						    $("#wizard_root_disk_offering").append(html);
						
						    var html2 = 
						    "<li>"
							    +"<input class='radio' type='radio' name='datadisk' id='datadisk' value='"+offerings[i].id+"'" + "/>"
							    +"<label style='width:500px;font-size:11px;' for='disk'>"+sanitizeXSS(unescape(offerings[i].displaytext))+"</label>"
					       +"</li>";
						    $("#wizard_data_disk_offering").append(html2);																		
					    }
					    //Safari and Chrome are not smart enough to make checkbox checked if html markup is appended by JQuery.append(). So, the following 2 lines are added.		
		                var html_all = $("#wizard_root_disk_offering").html();        
                        $("#wizard_root_disk_offering").html(html_all); 
			            
		                var html_all2 = $("#wizard_data_disk_offering").html();        
                        $("#wizard_data_disk_offering").html(html_all2); 
				    }
				    */
				    
				    
			    }
		    });
		 
		    
		    $vmPopup.find("#wizard_service_offering").click();	      
            return false;
        });
        
        
	    function vmWizardCleanup() {
	        currentStepInVmPopup = 1;			
		    $vmPopup.find("#step1").show().nextAll().hide();
		    //$vmPopup.find("#prev_step").hide();
		    //$vmPopup.find("#next_step").show();
		    $vmPopup.find("#wizard_message").hide();
		    selectedTemplateTypeInVmPopup = "featured";				
		    $("#wiz_featured").removeClass().addClass("rev_wizmid_selectedtempbut");
		    $("#wiz_my, #wiz_community, #wiz_blank").removeClass().addClass("rev_wizmid_nonselectedtempbut");	
		    currentPageInTemplateGridInVmPopup = 1;	 	
	    }	
    	
	    function vmWizardOpen() {
            $("#overlay_black").show();
            $vmPopup.show();        
            vmWizardCleanup();	
        }     
                
        function vmWizardClose() {			
		    $vmPopup.hide();
		    $("#overlay_black").hide();					
	    }
    		    	
	    $vmPopup.find("#close_button").bind("click", function(event) {
		    vmWizardClose();
		    return false;
	    });
    			
	    $vmPopup.find("#step1 #wiz_message_continue").bind("click", function(event) {			    
		    $vmPopup.find("#step1 #wiz_message").hide();
		    return false;
	    });
    		
	    $vmPopup.find("#step2 #wiz_message_continue").bind("click", function(event) {			    
		    $vmPopup.find("#step2 #wiz_message").hide();
		    return false;
	    });
    	
	    function getIconForOS(osType) {
		    if (osType == null || osType.length == 0) {
			    return "";
		    } else {
			    if (osType.match("^CentOS") != null) {
				    return "rev_wiztemo_centosicons";
			    } else if (osType.match("^Windows") != null) {
				    return "rev_wiztemo_windowsicons";
			    } else {
				    return "rev_wiztemo_linuxicons";
			    }
		    }
	    }
    	
	    //vm wizard search and pagination
	    $vmPopup.find("#search_button").bind("click", function(event) {	              
            currentPageInTemplateGridInVmPopup = 1;           	        	
            listTemplatesInVmPopup();  
            return false;   //event.preventDefault() + event.stopPropagation() 
        });
    	
        $vmPopup.find("#search_input").bind("keypress", function(event) {		        
            if(event.keyCode == keycode_Enter) {                	        
                $vmPopup.find("#search_button").click();	
                return false;   //event.preventDefault() + event.stopPropagation() 		     
            }		    
        });   
    			
	    $vmPopup.find("#nextPage").bind("click", function(event){	            
            currentPageInTemplateGridInVmPopup++;        
            listTemplatesInVmPopup(); 
            return false;   //event.preventDefault() + event.stopPropagation() 
        });		
        
        $vmPopup.find("#prevPage").bind("click", function(event){	                 
            currentPageInTemplateGridInVmPopup--;	              	    
            listTemplatesInVmPopup(); 
            return false;   //event.preventDefault() + event.stopPropagation() 
        });	
    						
	    var vmPopupStep2PageSize = 11; //max number of templates each page in step2 of New VM wizard is 11 
	    function listTemplatesInVmPopup() {		
	        var zoneId = $vmPopup.find("#wizard_zone").val();
	        if(zoneId == null || zoneId.length == 0)
	            return;
    	
	        var container = $vmPopup.find("#template_container");	 		    	
    		   
	        var commandString;    		  	   
            var searchInput = $vmPopup.find("#search_input").val();   
            if (selectedTemplateTypeInVmPopup != "blank") {      
                if (searchInput != null && searchInput.length > 0)                 
                    commandString = "command=listTemplates&templatefilter="+selectedTemplateTypeInVmPopup+"&zoneid="+zoneId+"&keyword="+searchInput+"&page="+currentPageInTemplateGridInVmPopup; 
                else
                    commandString = "command=listTemplates&templatefilter="+selectedTemplateTypeInVmPopup+"&zoneid="+zoneId+"&page="+currentPageInTemplateGridInVmPopup;           		    		
		    } else {
		        if (searchInput != null && searchInput.length > 0)                 
                    commandString = "command=listIsos&isReady=true&bootable=true&zoneid="+zoneId+"&keyword="+searchInput+"&page="+currentPageInTemplateGridInVmPopup;  
                else
                    commandString = "command=listIsos&isReady=true&bootable=true&zoneid="+zoneId+"&page="+currentPageInTemplateGridInVmPopup;  
		    }
    		
		    var loading = $vmPopup.find("#wiz_template_loading").show();				
		    if(currentPageInTemplateGridInVmPopup==1)
                $vmPopup.find("#prevPage").hide();
            else 
                $vmPopup.find("#prevPage").show();  		
    		
		    $.ajax({
			    data: createURL(commandString),
			    dataType: "json",
			    async: false,
			    success: function(json) {
			        var items;		
			        if (selectedTemplateTypeInVmPopup != "blank")
				        items = json.listtemplatesresponse.template;
				    else
				        items = json.listisosresponse.iso;
				    loading.hide();
				    container.empty();
				    if (items != null && items.length > 0) {
					    var first = true;
					    for (var i = 0; i < items.length; i++) {
						    var divClass = "rev_wiztemplistbox";
						    if (first) {
							    divClass = "rev_wiztemplistbox_selected";
							    first = false;
						    }

						    var html = '<div class="'+divClass+'" id="'+items[i].id+'">'
									      +'<div class="'+getIconForOS(items[i].ostypename)+'"></div>'
									      +'<div class="rev_wiztemp_listtext">'+sanitizeXSS(items[i].displaytext)+'</div>'
									      +'<div class="rev_wiztemp_ownertext">'+sanitizeXSS(items[i].account)+'</div>'
								      +'</div>';
						    container.append(html);
					    }						
					    if(items.length < vmPopupStep2PageSize)
		                    $vmPopup.find("#nextPage").hide();
		                else
		                    $vmPopup.find("#nextPage").show();
    		        
				    } else {
				        var msg;
				        if (selectedTemplateTypeInVmPopup != "blank")
				            msg = "No templates available";
				        else
				            msg = "No ISOs available";					    
					    var html = '<div class="rev_wiztemplistbox" id="-2">'
								      +'<div></div>'
								      +'<div class="rev_wiztemp_listtext">'+msg+'</div>'
							      +'</div>';
					    container.append(html);						
					    $vmPopup.find("#nextPage").hide();
				    }
			    }
		    });
	    }
    			
	    $vmPopup.find("#template_container").bind("click", function(event) {
		    var container = $(this);
		    var target = $(event.target);
		    var parent = target.parent();
		    if (parent.hasClass("rev_wiztemplistbox_selected") || parent.hasClass("rev_wiztemplistbox")) {
			    target = parent;
		    }
		    if (target.attr("id") != "-2") {
			    if (target.hasClass("rev_wiztemplistbox")) {
				    container.find(".rev_wiztemplistbox_selected").removeClass().addClass("rev_wiztemplistbox");
				    target.removeClass().addClass("rev_wiztemplistbox_selected");
			    } else if (target.hasClass("rev_wiztemplistbox_selected")) {
				    target.removeClass().addClass("rev_wiztemplistbox");
			    }
		    }
	    });
                 
        $vmPopup.find("#wizard_zone").bind("change", function(event) {       
            var selectedZone = $(this).val();   
            if(selectedZone != null && selectedZone.length > 0)
                listTemplatesInVmPopup();         
            return false;
        });
        
       
        function displayDiskOffering(type) {
            if(type=="data") {
                $vmPopup.find("#wizard_data_disk_offering_title").show();
			    $vmPopup.find("#wizard_data_disk_offering").show();
			    $vmPopup.find("#wizard_root_disk_offering_title").hide();
			    $vmPopup.find("#wizard_root_disk_offering").hide();
            }
            else if(type=="root") {
                $vmPopup.find("#wizard_root_disk_offering_title").show();
			    $vmPopup.find("#wizard_root_disk_offering").show();
			    $vmPopup.find("#wizard_data_disk_offering_title").hide();	
			    $vmPopup.find("#wizard_data_disk_offering").hide();	
            }
        }
        displayDiskOffering("data");  //because default value of "#wiz_template_filter" is "wiz_featured"
      
        
        // Setup the left template filters	  	
	    $vmPopup.find("#wiz_template_filter").unbind("click").bind("click", function(event) {		 	    
		    var $container = $(this);
		    var target = $(event.target);
		    var targetId = target.attr("id");
		    selectedTemplateTypeInVmPopup = "featured";		
		    switch (targetId) {
			    case "wiz_featured":
			        $vmPopup.find("#search_input").val("");  
			        currentPageInTemplateGridInVmPopup = 1;
				    selectedTemplateTypeInVmPopup = "featured";
				    $container.find("#wiz_featured").removeClass().addClass("rev_wizmid_selectedtempbut");
				    $container.find("#wiz_my, #wiz_community, #wiz_blank").removeClass().addClass("rev_wizmid_nonselectedtempbut");
				    displayDiskOffering("data");
				    break;
			    case "wiz_my":
			        $vmPopup.find("#search_input").val("");  
			        currentPageInTemplateGridInVmPopup = 1;
				    $container.find("#wiz_my").removeClass().addClass("rev_wizmid_selectedtempbut");
				    $container.find("#wiz_featured, #wiz_community, #wiz_blank").removeClass().addClass("rev_wizmid_nonselectedtempbut");
				    selectedTemplateTypeInVmPopup = "selfexecutable";
				    displayDiskOffering("data");
				    break;	
			    case "wiz_community":
			        $vmPopup.find("#search_input").val("");  
			        currentPageInTemplateGridInVmPopup = 1;
				    $container.find("#wiz_community").removeClass().addClass("rev_wizmid_selectedtempbut");
				    $container.find("#wiz_my, #wiz_featured, #wiz_blank").removeClass().addClass("rev_wizmid_nonselectedtempbut");
				    selectedTemplateTypeInVmPopup = "community";					
				    displayDiskOffering("data");
				    break;
			    case "wiz_blank":
			        $vmPopup.find("#search_input").val("");  
			        currentPageInTemplateGridInVmPopup = 1;
				    $container.find("#wiz_blank").removeClass().addClass("rev_wizmid_selectedtempbut");
				    $container.find("#wiz_my, #wiz_community, #wiz_featured").removeClass().addClass("rev_wizmid_nonselectedtempbut");
				    selectedTemplateTypeInVmPopup = "blank";
				    displayDiskOffering("root");
				    break;
		    }
		    listTemplatesInVmPopup();
		    return false;
	    });  
    	
	    $vmPopup.find("#next_step").bind("click", function(event) {
		    event.preventDefault();
		    event.stopPropagation();	
		    var $thisPopup = $vmPopup;		    		
		    if (currentStepInVmPopup == 1) { //select a template		
		        // prevent a person from moving on if no templates are selected	    
//		        if($thisPopup.find("#step1 #template_container .rev_wiztemplistbox_selected").length == 0) {			        
//		            $thisPopup.find("#step1 #wiz_message").show();
//		            return false;
//		        }
                   	 
			    if ($thisPopup.find("#wiz_blank").hasClass("rev_wizmid_selectedtempbut")) {  //ISO
			        $thisPopup.find("#step3_label").text("Root Disk Offering");
			        $thisPopup.find("#root_disk_offering_container").show();
			        $thisPopup.find("#data_disk_offering_container").hide();			       
			    } else {  //template
			        $thisPopup.find("#step3_label").text("Data Disk Offering");
			        $thisPopup.find("#data_disk_offering_container").show();
			        $thisPopup.find("#root_disk_offering_container").hide();			       
			    }	
    			
    			$thisPopup.find("#wizard_review_zone").text($thisPopup.find("#wizard_zone option:selected").text());    	
    			$thisPopup.find("#wizard_review_template").text($thisPopup.find("#step1 .rev_wiztemplistbox_selected .rev_wiztemp_listtext").text());
    							
//			    $thisPopup.find("#wizard_review_service_offering").text($thisPopup.find("#wizard_service_offering input[name=service]:checked").next().text());
//			    $thisPopup.find("#wizard_review_zone").text($thisPopup.find("#wizard_zone option:selected").text());
//			    $thisPopup.find("#wizard_review_name").text($thisPopup.find("#wizard_vm_name").val());
//			    $thisPopup.find("#wizard_review_group").text($thisPopup.find("#wizard_vm_group").val());
//    			
//			    if($thisPopup.find("#wizard_network_groups_container").css("display") != "none" && $thisPopup.find("#wizard_network_groups").val() != null) {
//			        var networkGroupList = $thisPopup.find("#wizard_network_groups").val().join(",");
//			        $thisPopup.find("#wizard_review_network_groups_p").show();
//			        $thisPopup.find("#wizard_review_network_groups").text(networkGroupList);				    
//			    } else {
//			        $thisPopup.find("#wizard_review_network_groups_p").hide();
//			        $thisPopup.find("#wizard_review_network_groups").text("");
//			    }								


		    }			
    		
		    if (currentStepInVmPopup == 2) { //service offering
		        // prevent a person from moving on if no service offering is selected
//		        if($thisPopup.find("input:radio[name=service_offering_radio]:checked").length == 0) {
//		            $thisPopup.find("#step2 #wiz_message #wiz_message_text").text("Please select a service offering to continue");
//		            $thisPopup.find("#step2 #wiz_message").show();
//			        return false;
//			    }
               
                $thisPopup.find("#wizard_review_service_offering").text($thisPopup.find("input:radio[name=service_offering_radio]:checked").next().text());

		    }			
    		
		    if(currentStepInVmPopup ==3) { //disk offering
		        /*
		        // validate values
			    var isValid = true;		
			    isValid &= validateString("Name", $thisPopup.find("#wizard_vm_name"), $thisPopup.find("#wizard_vm_name_errormsg"), true);
			    isValid &= validateString("Group", $thisPopup.find("#wizard_vm_group"), $thisPopup.find("#wizard_vm_group_errormsg"), true);				
			    if (!isValid) return;	
			    */		
			    
			    /*
			    // prevent a person from moving on if no radio button is selected
		        if($thisPopup.find("input:radio[name=disk_offering_radio]:checked").length == 0) {
		            $thisPopup.find("#step2 #wiz_message #wiz_message_text").text("Please select a disk offering to continue");
		            $thisPopup.find("#step2 #wiz_message").show();
			        return false;
			    }	
			    */	   	
    		   
		        if($thisPopup.find("#wiz_blank").hasClass("rev_wizmid_selectedtempbut"))  { //ISO
		            $thisPopup.find("#wizard_review_disk_offering_label").text("Root Disk Offering:");
			        $thisPopup.find("#wizard_review_disk_offering").text($thisPopup.find("#root_disk_offering_container input[name=root_disk_offering_radio]:checked").next().text());	
			    }
			    else { //template
			        $thisPopup.find("#wizard_review_disk_offering_label").text("Data Disk Offering:");
			        $thisPopup.find("#wizard_review_disk_offering").text($thisPopup.find("#data_disk_offering_container input[name=data_disk_offering_radio]:checked").next().text());
			    }	
		    }	
		    	
		    if (currentStepInVmPopup == 4) { //network
		    
		    }	
		    
		    if (currentStepInVmPopup == 5) { //last step		        
		        // validate values							
			    var isValid = true;									
			    isValid &= validateString("Name", $thisPopup.find("#wizard_vm_name"), $thisPopup.find("#wizard_vm_name_errormsg"), true);	 //optional	
			    isValid &= validateString("Group", $thisPopup.find("#wizard_vm_group"), $thisPopup.find("#wizard_vm_group_errormsg"), true); //optional					
			    if (!isValid) return;		    
		    
			    // Create a new VM!!!!
			    var moreCriteria = [];								
			    moreCriteria.push("&zoneId="+$thisPopup.find("#wizard_zone").val());
    									
			    moreCriteria.push("&templateId="+$thisPopup.find("#step1 .rev_wiztemplistbox_selected").attr("id"));
    							
			    moreCriteria.push("&serviceOfferingId="+$thisPopup.find("input:radio[name=service_offering_radio]:checked").val());
    			
    			var diskOfferingId;    						
			    if ($thisPopup.find("#wiz_blank").hasClass("rev_wizmid_selectedtempbut"))  //ISO
			        diskOfferingId = $thisPopup.find("#root_disk_offering_container input[name=root_disk_offering_radio]:checked").val();	
			    else  //template
			        diskOfferingId = $thisPopup.find("#data_disk_offering_container input[name=data_disk_offering_radio]:checked").val();				       
		        			   
		        if(diskOfferingId != null && diskOfferingId != "" && diskOfferingId != "no" && diskOfferingId != "custom")
			        moreCriteria.push("&diskOfferingId="+diskOfferingId);						 
    			
    			var name = trim($thisPopup.find("#wizard_vm_name").val());
			    if (name != null && name.length > 0) 
				    moreCriteria.push("&displayname="+encodeURIComponent(name));	
    			
			    var group = trim($thisPopup.find("#wizard_vm_group").val());
			    if (group != null && group.length > 0) 
				    moreCriteria.push("&group="+encodeURIComponent(group));		
    						
			    vmWizardClose();
    			
    			var $t = $("#midmenu_item_vm").clone();
    			$t.find("#vm_name").text("Adding....");
    			$t.find("#ip_address_container #label").html("&nbsp;");
    			$t.find("#content").addClass("inaction"); 
    			$t.find("#spinning_wheel").show();
    			$("#midmenu_container").append($t.show());
    			
			    $.ajax({
				    data: createURL("command=deployVirtualMachine"+moreCriteria.join("")),
				    dataType: "json",
				    success: function(json) {
					    var jobId = json.deployvirtualmachineresponse.jobid;
					    $t.attr("id","vmNew"+jobId).data("jobId", jobId);
					    var timerKey = "vmNew"+jobId;
    					
					    // Process the async job
					    $("body").everyTime(
						    10000,
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
										    $t.find("#content").removeClass("inaction"); 
										    $t.find("#spinning_wheel").hide();		
										    if (result.jobstatus == 1) {
											    // Succeeded	
											    $t.find("#ip_address_container #label").text("IP Address:");
											    $t.find("#info_icon").removeClass("error").show();
						                        $t.data("afterActionInfo", ("Adding succeeded.")); 
						                        if("virtualmachine" in result)	
						                            setMidmenuItemVm(result.virtualmachine[0], $t);		
											    /*
											    vmJSONToTemplate(result.virtualmachine[0], vmInstance);
											    if (result.virtualmachine[0].passwordenabled == 'true') {
												    vmInstance.find(".loadingmessage_container .loadingmessage_top p").html("Your instance has been successfully created.  Your new password is : <b>" + result.virtualmachine[0].password + "</b> .  Please change it as soon as you log into your new instance");
											    } else {
												    vmInstance.find(".loadingmessage_container .loadingmessage_top p").html("Your instance has been successfully created.");
											    }
											    vmInstance.find(".loadingmessage_container").fadeIn("slow");
											    vmInstance.attr("id", "vm" + result.virtualmachine[0].id);
											    vmInstance.find("#vm_state_bar").removeClass("admin_vmred_arrow admin_vmgrey_arrow").addClass("admin_vmgreen_arrow");
											    vmInstance.find("#vm_state").text("Running").removeClass("grid_stoppedtitles grid_celltitles").addClass("grid_runningtitles");
											    changeGridRowsTotal($("#grid_rows_total"), 1); 
											    */
											    
										    } else if (result.jobstatus == 2) {
											    // Failed
											    $t.find("#vm_name").text("Adding failed");
											    $t.find("#info_icon").addClass("error").show();
						                        $t.data("afterActionInfo", ("Adding failed. Reason: " + sanitizeXSS(result.jobresult)));   	
						                        $t.bind("click", function(event) {  
                                                    $rightPanelContent.find("#after_action_info").text($(this).data("afterActionInfo"));
                                                    $rightPanelContent.find("#after_action_info_container").addClass("errorbox");
                                                    $rightPanelContent.find("#after_action_info_container").show();   
                                                    vmClearRightPanel();
                                                    return false;
                                                });			
						                        
											    /*
											    vmInstance.find(".loadingmessage_container .loadingmessage_top p").text("Unable to create your new instance due to the error: " + result.jobresult);
											    vmInstance.find(".loadingmessage_container").fadeIn("slow");
											    vmInstance.find(".continue_button").data("jobId", result.jobid).unbind("click").bind("click", function(event) {
												    event.preventDefault();
												    var deadVM = $("#vmNew"+$(this).data("jobId"));
												    deadVM.slideUp("slow", function() {
													    $(this).remove();
												    });
											    });
											    */
										    }
									    }
								    },
								    error: function(XMLHttpResponse) {
									    $("body").stopTime(timerKey);
									    $t.find("#info_icon").addClass("error").show();
									    $t.find("#vm_name").text("Adding failed");
									    handleError(XMLHttpResponse);
								    }
							    });
						    },
						    0
					    );
				    },
				    error: function(XMLHttpResponse) {					    
					    $t.find("#info_icon").addClass("error").show();		
					    $t.find("#vm_name").text("Adding failed");		    
				        handleError(XMLHttpResponse);
				    }					
			    });
		    } 		
    		
		    //since no error, move to next step
		    
		    $vmPopup.find("#step" + currentStepInVmPopup).hide().next().show();  //hide current step, show next step		    
		    currentStepInVmPopup++;					
	    });
    	
	    $vmPopup.find("#prev_step").bind("click", function(event) {		
		    var $prevStep = $vmPopup.find("#step" + currentStepInVmPopup).hide().prev().show(); //hide current step, show previous step
		    currentStepInVmPopup--;
//		    if (currentStepInVmPopup == 1) {
//			    $vmPopup.find("#prev_step").hide();
//		    }
		    return false; //event.preventDefault() + event.stopPropagation()
	    });
        //***** VM Wizard (end) ********************************************************************************
    });	
}  


	 