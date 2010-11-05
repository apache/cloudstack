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

var $instanceSubMenuContainer;
function instanceBuildSubMenu() {    
    if (isAdmin() || isDomainAdmin()) {
		$("#leftmenu_instance_expandedbox").find("#leftmenu_instances_my_instances_container, #leftmenu_instances_all_instances_container, #leftmenu_instances_running_instances_container, #leftmenu_instances_stopped_instances_container, #leftmenu_instances_destroyed_instances_container ").show();
    } 	
    else if(isUser()) {	 
		$("#leftmenu_instance_expandedbox").find("#leftmenu_instances_all_instances_container, #leftmenu_instances_running_instances_container, #leftmenu_instances_stopped_instances_container").show();
        $.ajax({
            cache: false,
            data: createURL("command=listInstanceGroups"),	       
            dataType: "json",
            success: function(json) {	            
                var instancegroups = json.listinstancegroupsresponse.instancegroup;	        	
        	    if(instancegroups!=null && instancegroups.length>0) {           
	                for(var i=0; i < instancegroups.length; i++) {		                
	                    instanceBuildSubMenu2(instancegroups[i].name, ("listVirtualMachines&groupid="+instancegroups[i].id));   
	                }
	            }
            }
        });  
    }    
}

function instanceBuildSubMenu2(label, commandString) {   
    var $newSubMenu = $("#leftmenu_secondindent_template").clone();
    $newSubMenu.find("#label").text(label);    
    bindAndListMidMenuItems($newSubMenu, commandString, "listvirtualmachinesresponse", "virtualmachine", "jsp/instance.jsp", afterLoadInstanceJSP, vmToMidmenu, vmToRightPanel, getMidmenuId, true);
    $instanceSubMenuContainer.append($newSubMenu.show());
}

function afterLoadInstanceJSP() {
    //Add VM button
    $("#midmenu_add_link").find("#label").text("Add VM"); 
    $("#midmenu_add_link").show(); 
	
    if (isAdmin() || isDomainAdmin())
        $("#right_panel_content").find("#tab_router,#tab_router").show();
        
    initStartVMButton();    
    initStopVMButton(); 
    initRebootVMButton();    
    initDestroyVMButton();
    
    // switch between different tabs 
    var tabArray = [$("#tab_details"), $("#tab_volume"), $("#tab_statistics"), $("#tab_router")];
    var tabContentArray = [$("#tab_content_details"), $("#tab_content_volume"), $("#tab_content_statistics"), $("#tab_content_router")];
    var afterSwitchFnArray = [vmJsonToDetailsTab, vmJsonToVolumeTab, vmJsonToStatisticsTab, vmJsonToRouterTab];
    switchBetweenDifferentTabs(tabArray, tabContentArray, afterSwitchFnArray);       
    
    //initialize VM Wizard    
    initVMWizard();       
    
    // dialogs
    initDialog("dialog_detach_iso_from_vm");       	
   	initDialog("dialog_attach_iso");  
    initDialog("dialog_change_name"); 
    initDialog("dialog_change_group"); 
    initDialog("dialog_change_service_offering", 600); 
    initDialog("dialog_confirmation_change_root_password");
    initDialog("dialog_confirmation_enable_ha");  
    initDialog("dialog_confirmation_disable_ha");            
    initDialog("dialog_create_template", 400);  
    initDialog("dialog_confirmation_start_vm");
    initDialog("dialog_confirmation_stop_vm");
    initDialog("dialog_confirmation_reboot_vm");
    initDialog("dialog_confirmation_destroy_vm");
    initDialog("dialog_confirmation_restore_vm");    
    initDialog("dialog_confirmation_start_router");
    initDialog("dialog_confirmation_stop_router");
    initDialog("dialog_confirmation_reboot_router");
    
    vmPopulateDropdown();
}

function initStartVMButton() {
    $("#midmenu_startvm_link").show();   
    $("#midmenu_startvm_link").bind("click", function(event) {            
        var itemCounts = 0;
        for(var id in selectedItemsInMidMenu) {
            itemCounts ++;
        }
        if(itemCounts == 0) {
            $("#dialog_info_please_select_one_item_in_middle_menu").dialog("open");		
            return false;
        }        
                
        $("#dialog_confirmation_start_vm")	
	    .dialog('option', 'buttons', { 						
		    "Confirm": function() { 
			    $(this).dialog("close"); 			
			    
			    var apiInfo = {
                    label: "Start Instance",
                    isAsyncJob: true,
                    inProcessText: "Starting Instance....",
                    asyncJobResponse: "startvirtualmachineresponse",                  
                    afterActionSeccessFn: function(json, $midmenuItem1, id) {                    
                        var jsonObj = json.queryasyncjobresultresponse.jobresult.virtualmachine;      
                        
                        vmToMidmenu(jsonObj, $midmenuItem1);                     
                        if( ($("#right_panel_content #tab_content_details").length > 0)
                          && ($("#right_panel_content #tab_content_details").data("jsonObj") != null )
                          && (jsonObj.id == $("#right_panel_content #tab_content_details").data("jsonObj").id)) 
                            vmToRightPanel($midmenuItem1);                              
                    }
                }          
			                    
                for(var id in selectedItemsInMidMenu) {	
                    var apiCommand = "command=startVirtualMachine&id="+id;                                                
                    doActionToMidMenu(id, apiInfo, apiCommand); 	
                }  
                
                selectedItemsInMidMenu = {}; //clear selected items for action	                      					    
		    }, 
		    "Cancel": function() { 
			    $(this).dialog("close"); 
    			
		    } 
	    }).dialog("open");
                                                 
        return false;        
    }); 	
}

function initStopVMButton() {
    $("#midmenu_stopvm_link").show();   
    $("#midmenu_stopvm_link").bind("click", function(event) {            
        var itemCounts = 0;
        for(var id in selectedItemsInMidMenu) {
            itemCounts ++;
        }
        if(itemCounts == 0) {
            $("#dialog_info_please_select_one_item_in_middle_menu").dialog("open");		
            return false;
        }        
        
        $("#dialog_confirmation_stop_vm")	
	    .dialog('option', 'buttons', { 						
		    "Confirm": function() { 
			    $(this).dialog("close"); 			
			    
			    var apiInfo = {
                    label: "Stop Instance",
                    isAsyncJob: true,
                    inProcessText: "Stopping Instance....",
                    asyncJobResponse: "stopvirtualmachineresponse",                 
                    afterActionSeccessFn: function(json, $midmenuItem1, id) {                         
                        var jsonObj = json.queryasyncjobresultresponse.jobresult.virtualmachine;  
                        vmToMidmenu(jsonObj, $midmenuItem1);   
                        if( ($("#right_panel_content #tab_content_details").length > 0)
                          && ($("#right_panel_content #tab_content_details").data("jsonObj") != null )
                          && (jsonObj.id == $("#right_panel_content #tab_content_details").data("jsonObj").id)) 
                            vmToRightPanel($midmenuItem1);                                          
                    }
                }                      
			                    
                for(var id in selectedItemsInMidMenu) {	
                    var apiCommand = "command=stopVirtualMachine&id="+id;                                    
                    doActionToMidMenu(id, apiInfo, apiCommand); 	
                }  
                
                selectedItemsInMidMenu = {}; //clear selected items for action	                      					    
		    }, 
		    "Cancel": function() { 
			    $(this).dialog("close"); 
    			
		    } 
	    }).dialog("open");                        	                   
         
        return false;        
    }); 	
}

function initRebootVMButton() {
    $("#midmenu_rebootvm_link").show();   
    $("#midmenu_rebootvm_link").bind("click", function(event) {            
        var itemCounts = 0;
        for(var id in selectedItemsInMidMenu) {
            itemCounts ++;
        }
        if(itemCounts == 0) {
            $("#dialog_info_please_select_one_item_in_middle_menu").dialog("open");		
            return false;
        }        
               
        $("#dialog_confirmation_reboot_vm")	
	    .dialog('option', 'buttons', { 						
		    "Confirm": function() { 
			    $(this).dialog("close"); 			
			    
			    var apiInfo = {
                    label: "Reboot Instance",
                    isAsyncJob: true,
                    inProcessText: "Rebooting Instance....",
                    asyncJobResponse: "rebootvirtualmachineresponse",                  
                    afterActionSeccessFn: function(json, $midmenuItem1, id) {  
                        var jsonObj = json.queryasyncjobresultresponse.jobresult.virtualmachine;  
                        vmToMidmenu(jsonObj, $midmenuItem1);    
                        if( ($("#right_panel_content #tab_content_details").length > 0)
                          && ($("#right_panel_content #tab_content_details").data("jsonObj") != null )
                          && (jsonObj.id == $("#right_panel_content #tab_content_details").data("jsonObj").id)) 
                            vmToRightPanel($midmenuItem1);                                         
                    }
                }                       
			                    
                for(var id in selectedItemsInMidMenu) {	
                    var apiCommand = "command=rebootVirtualMachine&id="+id;                                               
                    doActionToMidMenu(id, apiInfo, apiCommand); 	
                }  
                
                selectedItemsInMidMenu = {}; //clear selected items for action	                      					    
		    }, 
		    "Cancel": function() { 
			    $(this).dialog("close"); 
    			
		    } 
	    }).dialog("open");
                                          
        return false;        
    }); 
}

function initDestroyVMButton() {
    $("#midmenu_destroyvm_link").show();   
    $("#midmenu_destroyvm_link").bind("click", function(event) {            
        var itemCounts = 0;
        for(var id in selectedItemsInMidMenu) {
            itemCounts ++;
        }
        if(itemCounts == 0) {
            $("#dialog_info_please_select_one_item_in_middle_menu").dialog("open");		
            return false;
        }        
                
        $("#dialog_confirmation_destroy_vm")	
	    .dialog('option', 'buttons', { 						
		    "Confirm": function() { 
			    $(this).dialog("close"); 			
			    
			    var apiInfo = {
                    label: "Destroy Instance",
                    isAsyncJob: true,
                    inProcessText: "Destroying Instance....",
                    asyncJobResponse: "destroyvirtualmachineresponse",                 
                    afterActionSeccessFn: function(json, $midmenuItem1, id) {  
                        var jsonObj = json.queryasyncjobresultresponse.jobresult.virtualmachine; 
                        vmToMidmenu(jsonObj, $midmenuItem1);  
                        if( ($("#right_panel_content #tab_content_details").length > 0)
                          && ($("#right_panel_content #tab_content_details").data("jsonObj") != null )
                          && (jsonObj.id == $("#right_panel_content #tab_content_details").data("jsonObj").id)) 
                            vmToRightPanel($midmenuItem1);                                           
                    }
                }                            
			                    
                for(var id in selectedItemsInMidMenu) {	
                    var apiCommand = "command=destroyVirtualMachine&id="+id;                                       
                    doActionToMidMenu(id, apiInfo, apiCommand); 	
                }  
                
                selectedItemsInMidMenu = {}; //clear selected items for action	                      					    
		    }, 
		    "Cancel": function() { 
			    $(this).dialog("close"); 
    			
		    } 
	    }).dialog("open");                 
             
        return false;        
    }); 	
}

function vmPopulateDropdown() {         
    $.ajax({
        data: createURL("command=listOsTypes&response=json"),
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
}

function initVMWizard() {
    $vmPopup = $("#vm_popup");  
    var currentPageInTemplateGridInVmPopup =1;
    var selectedTemplateTypeInVmPopup;  //selectedTemplateTypeInVmPopup will be set to "featured" when new VM dialog box opens
	   	
    $("#midmenu_add_link").unbind("click").bind("click", function(event) {
        vmWizardOpen();			
	    $.ajax({
		    data: createURL("command=listZones&available=true"),
		    dataType: "json",
		    success: function(json) {
			    var zones = json.listzonesresponse.zone;					
			    var $zoneSelect = $vmPopup.find("#wizard_zone").empty();					
			    if (zones != null && zones.length > 0) {
				    for (var i = 0; i < zones.length; i++) {
					    $zoneSelect.append("<option value='" + zones[i].id + "'>" + fromdb(zones[i].name) + "</option>"); 
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
			    if (offerings != null && offerings.length > 0) {						    
				    for (var i = 0; i < offerings.length; i++) {	
					    var $t = $("#vm_popup_service_offering_template").clone();  						  
					    $t.find("input:radio[name=service_offering_radio]").val(offerings[i].id); 
					    $t.find("#name").text(fromdb(offerings[i].name));
					    $t.find("#description").text(fromdb(offerings[i].displaytext)); 						    
					    if (i > 0)
					        $t.find("input:radio[name=service_offering_radio]").removeAttr("checked");							
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
	            var $t = $("#vm_popup_disk_offering_template_no").clone(); 		            	     
	            $t.find("input:radio").attr("name","data_disk_offering_radio");  
	            $t.find("#name").text("no, thanks"); 		            
	            $dataDiskOfferingContainer.append($t.show()); 
		        		        
		        //disk offerings from database
		        if (offerings != null && offerings.length > 0) {						    
			        for (var i = 0; i < offerings.length; i++) {	
			            var $t;
			            if(offerings[i].isCustomized == true) 			                       
		                    $t = $("#vm_popup_disk_offering_template_custom").clone();  			            
			            else 
				            $t = $("#vm_popup_disk_offering_template_existing").clone(); 	
				        
				        $t.data("jsonObj", offerings[i]);				        
				        $t.find("input:radio").attr("name","data_disk_offering_radio").val(fromdb(offerings[i].id)).removeAttr("checked");	 	
			            $t.find("#name").text(fromdb(offerings[i].name));
			            $t.find("#description").text(fromdb(offerings[i].displaytext)); 	 
			            $dataDiskOfferingContainer.append($t.show());	
			        }
		        }
		        
		        //Safari and Chrome are not smart enough to make checkbox checked if html markup is appended by JQuery.append(). So, the following 2 lines are added.		
	            var html_all = $dataDiskOfferingContainer.html();        
                $dataDiskOfferingContainer.html(html_all);                     
		        //***** data disk offering: "no, thanks", "custom", existing disk offerings in database (end) *******************************************************
		        		        	
		        //***** root disk offering: "custom", existing disk offerings in database (begin) *******************************************************************
		        		        
		        //disk offerings from database
		        if (offerings != null && offerings.length > 0) {						    
			        for (var i = 0; i < offerings.length; i++) {	
			            var $t;
			            if(offerings[i].isCustomized == true) 
			                $t = $("#vm_popup_disk_offering_template_custom").clone();  
			            else 
				            $t = $("#vm_popup_disk_offering_template_existing").clone(); 	
				        
				        $t.data("jsonObj", offerings[i]);	
				        $t.find("input:radio").attr("name","root_disk_offering_radio").val(offerings[i].id);	 
				        if(i > 0) //default is the 1st existing disk offering. If there is no existing disk offering, default to "custom" radio button
				            $t.find("input:radio").removeAttr("checked");	 	
				        $t.find("#name").text(fromdb(offerings[i].name));
				        $t.find("#description").text(fromdb(offerings[i].displaytext)); 	 
				        $rootDiskOfferingContainer.append($t.show());	
			        }
		        }
			    
			    //Safari and Chrome are not smart enough to make checkbox checked if html markup is appended by JQuery.append(). So, the following 2 lines are added.		
	            var html_all = $rootDiskOfferingContainer.html();        
                $rootDiskOfferingContainer.html(html_all);                         
			    //***** root disk offering: "custom", existing disk offerings in database (end) *********************************************************************				    
		    }
	    });	 
	    
	    $vmPopup.find("#wizard_service_offering").click();	      
        return false;
    });
        
    function vmWizardCleanup() {
        currentStepInVmPopup = 1;			
	    $vmPopup.find("#step1").show().nextAll().hide();		   
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
		   
        var commandString, templateType;    		  	   
        var searchInput = $vmPopup.find("#search_input").val();   
        if (selectedTemplateTypeInVmPopup != "blank") {  //*** template ***  
            templateType = "template";
            if (searchInput != null && searchInput.length > 0)                 
                commandString = "command=listTemplates&templatefilter="+selectedTemplateTypeInVmPopup+"&zoneid="+zoneId+"&keyword="+searchInput+"&page="+currentPageInTemplateGridInVmPopup; 
            else
                commandString = "command=listTemplates&templatefilter="+selectedTemplateTypeInVmPopup+"&zoneid="+zoneId+"&page="+currentPageInTemplateGridInVmPopup;           		    		
	    } 
	    else {  //*** ISO ***
	        templateType = "ISO";
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
		        var items, $vmTemplateInWizard;		
		        if (templateType == "template") {
			        items = json.listtemplatesresponse.template;
			        $vmTemplateInWizard = $("#vmtemplate_in_vmwizard");
			    }
			    else if (templateType == "ISO") {
			        items = json.listisosresponse.iso;
			        $vmTemplateInWizard = $("#vmiso_in_vmwizard");
			    }
			        
			    loading.hide();
			    container.empty(); 
			    if (items != null && items.length > 0) {	
				    for (var i = 0; i < items.length; i++) {
				        var $newTemplate = $vmTemplateInWizard.clone();				        
				        vmWizardTemplateJsonToTemplate(items[i], $newTemplate, templateType, i);
				        container.append($newTemplate.show());				       
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
		
	var $selectedVmWizardTemplate;		
	function vmWizardTemplateJsonToTemplate(jsonObj, $template, templateType, i) {	 
	    $template.attr("id", ("vmWizardTemplate_"+jsonObj.id));
	    $template.data("templateId", jsonObj.id);
	    $template.data("templateType", templateType);
	    $template.data("templateName", fromdb(jsonObj.displaytext));
	
        $template.find("#icon").removeClass().addClass(getIconForOS(jsonObj.ostypename));
        $template.find("#name").text(fromdb(jsonObj.displaytext));	
        
        if(templateType == "template") {
            $template.find("#hypervisor_text").text(fromdb(jsonObj.hypervisor));	
            //$template.find("#hypervisor_text").text("XenServer");  //This line is for testing only. Comment this line and uncomment the line above before checkin.
        }
        				    
        $template.find("#submitted_by").text(fromdb(jsonObj.account));				      
                
        if(i == 0) { //select the 1st one
            $selectedVmWizardTemplate = $template;
            $template.addClass("rev_wiztemplistbox_selected");
        }
        else {
            $template.addClass("rev_wiztemplistbox");
        }
            
        $template.bind("click", function(event) {
            if($selectedVmWizardTemplate != null)
                $selectedVmWizardTemplate.removeClass("rev_wiztemplistbox_selected").addClass("rev_wiztemplistbox");          
             
            $(this).removeClass("rev_wiztemplistbox").addClass("rev_wiztemplistbox_selected");  
            $selectedVmWizardTemplate = $(this);
            return false;
        });
	}		
	
	/*		
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
    */
             
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
	    if (currentStepInVmPopup == 1) { //select a template/ISO		    		
	        // prevent a person from moving on if no templates are selected	  
	        if($thisPopup.find("#step1 #template_container .rev_wiztemplistbox_selected").length == 0) {			        
	            $thisPopup.find("#step1 #wiz_message").show();
	            return false;
	        }
               	 
		    if ($thisPopup.find("#wiz_blank").hasClass("rev_wizmid_selectedtempbut")) {  //ISO
		        $thisPopup.find("#step3_label").text("Root Disk Offering");
		        $thisPopup.find("#root_disk_offering_container").show();
		        $thisPopup.find("#data_disk_offering_container").hide();			       
		    } 
		    else {  //template
		        $thisPopup.find("#step3_label").text("Data Disk Offering");
		        $thisPopup.find("#data_disk_offering_container").show();
		        $thisPopup.find("#root_disk_offering_container").hide();			       
		    }	
			
			$thisPopup.find("#wizard_review_zone").text($thisPopup.find("#wizard_zone option:selected").text());    
			
			// This is taking from the selected template but need to change this to the dropdown that supports ISO.		
			if($selectedVmWizardTemplate.data("templateType") == "template")
			    $selectedVmWizardTemplate.data("hypervisor", $selectedVmWizardTemplate.find("#hypervisor_text").text());
			else 
			    $selectedVmWizardTemplate.data("hypervisor", $selectedVmWizardTemplate.find("#hypervisor_select").val());			
			$thisPopup.find("#wizard_review_hypervisor").text($selectedVmWizardTemplate.data("hypervisor"));   	
						
			$thisPopup.find("#wizard_review_template").text($selectedVmWizardTemplate.data("templateName")); 
	    }			
		
	    if (currentStepInVmPopup == 2) { //service offering
	        // prevent a person from moving on if no service offering is selected
	        if($thisPopup.find("input:radio[name=service_offering_radio]:checked").length == 0) {
	            $thisPopup.find("#step2 #wiz_message #wiz_message_text").text("Please select a service offering to continue");
	            $thisPopup.find("#step2 #wiz_message").show();
		        return false;
		    }               
            $thisPopup.find("#wizard_review_service_offering").text($thisPopup.find("input:radio[name=service_offering_radio]:checked").next().text());
	    }			
		
	    if(currentStepInVmPopup ==3) { //disk offering	 	        
	        if($selectedVmWizardTemplate.data("templateType") == "template") {	//*** template ***            
	            $thisPopup.find("#wizard_review_disk_offering_label").text("Data Disk Offering:");
	            var checkedRadioButton = $thisPopup.find("#data_disk_offering_container input[name=data_disk_offering_radio]:checked");	
	        }
	        else {  //*** ISO ***
	            $thisPopup.find("#wizard_review_disk_offering_label").text("Root Disk Offering:");
	            var checkedRadioButton = $thisPopup.find("#root_disk_offering_container input[name=root_disk_offering_radio]:checked");	
		    }
		          		        
	        var $diskOfferingElement = checkedRadioButton.parent();	        	    
	        
	        var isValid = true;		
	        if($diskOfferingElement.find("#custom_disk_size").length > 0) 	    
	            isValid &= validateNumber("Disk Size", $diskOfferingElement.find("#custom_disk_size"), $diskOfferingElement.find("#custom_disk_size_errormsg"), null, null, false);	//required	
	        else
	            isValid &= validateNumber("Disk Size", $diskOfferingElement.find("#custom_disk_size"), $diskOfferingElement.find("#custom_disk_size_errormsg"), null, null, true);	//optional		    		
	        if (!isValid) 
	            return;        
	        
	        var diskOfferingName = $diskOfferingElement.find("#name").text();
	        if(checkedRadioButton.parent().attr("id") == "vm_popup_disk_offering_template_custom")
	            diskOfferingName += (" (Disk Size: " + $diskOfferingElement.find("#custom_disk_size").val() + " MB)");
	        $thisPopup.find("#wizard_review_disk_offering").text(diskOfferingName);  
	    }	
	    	
	    if (currentStepInVmPopup == 4) { //network
	    
	    }	
	    
	    if (currentStepInVmPopup == 5) { //last step		        
	        // validate values							
		    var isValid = true;									
		    isValid &= validateString("Name", $thisPopup.find("#wizard_vm_name"), $thisPopup.find("#wizard_vm_name_errormsg"), true);	 //optional	
		    isValid &= validateString("Group", $thisPopup.find("#wizard_vm_group"), $thisPopup.find("#wizard_vm_group_errormsg"), true); //optional					
		    if (!isValid) 
		        return;		    
	        vmWizardClose();
	        
		    // Create a new VM!!!!
		    var moreCriteria = [];								
		    moreCriteria.push("&zoneId="+$thisPopup.find("#wizard_zone").val());			    
			moreCriteria.push("&hypervisor="+$selectedVmWizardTemplate.data("hypervisor"));	    								
		    moreCriteria.push("&templateId="+$selectedVmWizardTemplate.data("templateId"));    							
		    moreCriteria.push("&serviceOfferingId="+$thisPopup.find("input:radio[name=service_offering_radio]:checked").val());
			
			var diskOfferingId, $diskOfferingElement;    						
		    if ($thisPopup.find("#wiz_blank").hasClass("rev_wizmid_selectedtempbut")) {  //ISO
		        diskOfferingId = $thisPopup.find("#root_disk_offering_container input[name=root_disk_offering_radio]:checked").val();	
		        $diskOfferingElement = $thisPopup.find("#root_disk_offering_container input[name=root_disk_offering_radio]:checked").parent();
		    }
		    else { //template
		        diskOfferingId = $thisPopup.find("#data_disk_offering_container input[name=data_disk_offering_radio]:checked").val();	
		        $diskOfferingElement = $thisPopup.find("#data_disk_offering_container input[name=data_disk_offering_radio]:checked").parent();
		    }
	        if(diskOfferingId != null && diskOfferingId != "" && diskOfferingId != "no")
		        moreCriteria.push("&diskOfferingId="+diskOfferingId);						 
								
			if($diskOfferingElement.find("#custom_disk_size").length > 0) {    			
			    var customDiskSize = $diskOfferingElement.find("#custom_disk_size").val(); //unit is MB
			    if(customDiskSize != null && customDiskSize.length > 0)
			        moreCriteria.push("&size="+customDiskSize);	    
			}
			
			var name = trim($thisPopup.find("#wizard_vm_name").val());
		    if (name != null && name.length > 0) 
			    moreCriteria.push("&displayname="+todb(name));	
			
		    var group = trim($thisPopup.find("#wizard_vm_group").val());
		    if (group != null && group.length > 0) 
			    moreCriteria.push("&group="+todb(group));	
			    		
			var $midmenuItem1 = beforeAddingMidMenuItem() ;
			    			
		    $.ajax({
			    data: createURL("command=deployVirtualMachine"+moreCriteria.join("")),
			    dataType: "json",
			    success: function(json) {
				    var jobId = json.deployvirtualmachineresponse.jobid;					   
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
									    if (result.jobstatus == 1) {
										    // Succeeded	
										    var item = result.jobresult.virtualmachine;					                        
				                            vmToMidmenu(item, $midmenuItem1);
				                            bindClickToMidMenu($midmenuItem1, vmToRightPanel, getMidmenuId);  
				                            if (item.passwordenabled == 'true') {							                                									        
										        var extraMessage = "New password: " + item.password;
										        afterAddingMidMenuItem($midmenuItem1, true, extraMessage);
										        var afterActionInfo = "Your instance has been successfully created.  Your new password is : " + item.password;
										        $midmenuItem1.data("afterActionInfo", afterActionInfo); 
									        } 	
									        else {
									            afterAddingMidMenuItem($midmenuItem1, true);
									        }							                        
									    } else if (result.jobstatus == 2) {
										    // Failed										    
										    afterAddingMidMenuItem($midmenuItem1, false, fromdb(result.jobresult.errortext));		
									    }
								    }
							    },
							    error: function(XMLHttpResponse) {
								    $("body").stopTime(timerKey);									    
								    afterAddingMidMenuItem($midmenuItem1, false);									    						    
								    handleError(XMLHttpResponse);
							    }
						    });
					    },
					    0
				    );
			    },
			    error: function(XMLHttpResponse) {					        				    
				    afterAddingMidMenuItem($midmenuItem1, false);	
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
	    return false; //event.preventDefault() + event.stopPropagation()
    });
}


//***** VM Detail (begin) ******************************************************************************
      
var vmActionMap = {    
    "Start Instance": {        
        isAsyncJob: true,
        asyncJobResponse: "startvirtualmachineresponse",
        inProcessText: "Starting Instance....",
        dialogBeforeActionFn : doStartVM,
        afterActionSeccessFn: function(json, $midmenuItem1, id) { 
            var jsonObj = json.queryasyncjobresultresponse.jobresult.virtualmachine;      
            vmToMidmenu(jsonObj, $midmenuItem1);
            vmToRightPanel($midmenuItem1);
        }
    },
    "Stop Instance": {             
        isAsyncJob: true,
        asyncJobResponse: "stopvirtualmachineresponse",
        inProcessText: "Stopping Instance....",
        dialogBeforeActionFn : doStopVM,
        afterActionSeccessFn: function(json, $midmenuItem1, id) { 
            var jsonObj = json.queryasyncjobresultresponse.jobresult.virtualmachine;            
            vmToMidmenu(jsonObj, $midmenuItem1);
            vmToRightPanel($midmenuItem1);
        }
    },
    "Reboot Instance": {        
        isAsyncJob: true,
        asyncJobResponse: "rebootvirtualmachineresponse",
        inProcessText: "Rebooting Instance....",
        dialogBeforeActionFn : doRebootVM,
        afterActionSeccessFn: function(json, $midmenuItem1, id) { 
            var jsonObj = json.queryasyncjobresultresponse.jobresult.virtualmachine;       
            vmToMidmenu(jsonObj, $midmenuItem1);
            vmToRightPanel($midmenuItem1);
        }
    },
    "Destroy Instance": {        
        isAsyncJob: true,
        asyncJobResponse: "destroyvirtualmachineresponse",
        inProcessText: "Destroying Instance....",
        dialogBeforeActionFn : doDestroyVM,
        afterActionSeccessFn: function(json, $midmenuItem1, id) {             
            var jsonObj = json.queryasyncjobresultresponse.jobresult.virtualmachine; 
            vmToMidmenu(jsonObj, $midmenuItem1);
            vmToRightPanel($midmenuItem1);
        }
    },
    "Restore Instance": {          
        isAsyncJob: false,
        inProcessText: "Restoring Instance....",
        dialogBeforeActionFn : doRestoreVM,
        afterActionSeccessFn: function(json, $midmenuItem1, id) { 
            var jsonObj = json.queryasyncjobresultresponse.jobresult.virtualmachine; 
            vmToMidmenu(jsonObj, $midmenuItem1);
            vmToRightPanel($midmenuItem1);
        }
    },
    "Edit Instance": {
        dialogBeforeActionFn: doEditVM  
    },
    "Attach ISO": {
        isAsyncJob: true,
        asyncJobResponse: "attachisoresponse",    
        inProcessText: "Attaching ISO....",        
        dialogBeforeActionFn : doAttachISO,
        afterActionSeccessFn: function(json, $midmenuItem1, id) { 
            //call listVirtualMachine to get embedded object until bug 6487 ("AttachISO API should return an embedded object on success") is fixed.
            var id = $midmenuItem1.data("jsonObj").id; 
            var jsonObj;
            $.ajax({
                data: createURL("command=listVirtualMachines&id="+id),
                dataType: "json",
                async: false,
                success: function(json) {                    
                    jsonObj = json.listvirtualmachinesresponse.virtualmachine[0];                    
                }
            });
            
            vmToMidmenu(jsonObj, $midmenuItem1);
            vmToRightPanel($midmenuItem1);
        }   
    },
    "Detach ISO": {
        isAsyncJob: true,
        asyncJobResponse: "detachisoresponse",     
        inProcessText: "Detaching ISO....",       
        dialogBeforeActionFn : doDetachISO,
        afterActionSeccessFn: function(json, $midmenuItem1, id) { 
             //call listVirtualMachine to get embedded object until bug 6488 ("Detach ISO API should return an embedded object on success") is fixed.
            var id = $midmenuItem1.data("jsonObj").id; 
            var jsonObj;
            $.ajax({
                data: createURL("command=listVirtualMachines&id="+id),
                dataType: "json",
                async: false,
                success: function(json) {                    
                    jsonObj = json.listvirtualmachinesresponse.virtualmachine[0];                    
                }
            });
            
            vmToMidmenu(jsonObj, $midmenuItem1);
            vmToRightPanel($midmenuItem1);
        }   
    },
    "Reset Password": {                
        isAsyncJob: true,  
        asyncJobResponse: "resetpasswordforvirtualmachineresponse", 
        inProcessText: "Resetting Password....",  
        dialogBeforeActionFn : doResetPassword,
        afterActionSeccessFn: function(json, $midmenuItem1, id) { 
            var item = json.queryasyncjobresultresponse.jobresult.resetpasswordforvirtualmachineresponse;            
            var $afterActionInfoContainer = $("#right_panel_content #after_action_info_container_on_top");
		    $afterActionInfoContainer.find("#after_action_info").html("New password is <b>" + item.password + "</b>");  
		    $afterActionInfoContainer.removeClass("errorbox").show();            
        }
    },       
    "Change Service": {
        isAsyncJob: true,
        asyncJobResponse: "changeserviceforvirtualmachineresponse",
        inProcessText: "Changing Service....",
        dialogBeforeActionFn : doChangeService,
        afterActionSeccessFn: function(json, $midmenuItem1, id) { 
            var jsonObj = json.queryasyncjobresultresponse.virtualmachine[0];
            vmToMidmenu(jsonObj, $midmenuItem1);
            vmToRightPanel($midmenuItem1);
        }
    }      
}                      
     
function doStartVM($actionLink, $detailsTab, $midmenuItem1) {       
    $("#dialog_confirmation_start_vm")	
    .dialog('option', 'buttons', { 						
	    "Confirm": function() { 
		    $(this).dialog("close"); 			
		    
		    var jsonObj = $midmenuItem1.data("jsonObj");
		    var id = jsonObj.id;
		    var apiCommand = "command=startVirtualMachine&id="+id;  
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);				   	                         					    
	    }, 
	    "Cancel": function() { 
		    $(this).dialog("close"); 
			
	    } 
    }).dialog("open");
}   

function doStopVM($actionLink, $detailsTab, $midmenuItem1) {   
    $("#dialog_confirmation_stop_vm")	
    .dialog('option', 'buttons', { 						
	    "Confirm": function() { 
		    $(this).dialog("close"); 			
		    
		    var jsonObj = $midmenuItem1.data("jsonObj");
		    var id = jsonObj.id;
		    var apiCommand = "command=stopVirtualMachine&id="+id;  
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);				   	                         					    
	    }, 
	    "Cancel": function() { 
		    $(this).dialog("close"); 
			
	    } 
    }).dialog("open");
}   
   
function doRebootVM($actionLink, $detailsTab, $midmenuItem1) {   
    $("#dialog_confirmation_reboot_vm")	
    .dialog('option', 'buttons', { 						
	    "Confirm": function() { 
		    $(this).dialog("close"); 			
		    
		    var jsonObj = $midmenuItem1.data("jsonObj");
		    var id = jsonObj.id;
		    var apiCommand = "command=rebootVirtualMachine&id="+id;  
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);				   	                         					    
	    }, 
	    "Cancel": function() { 
		    $(this).dialog("close"); 
			
	    } 
    }).dialog("open");
}   
  
function doDestroyVM($actionLink, $detailsTab, $midmenuItem1) {   
    $("#dialog_confirmation_destroy_vm")	
    .dialog('option', 'buttons', { 						
	    "Confirm": function() { 
		    $(this).dialog("close"); 			
		    
		    var jsonObj = $midmenuItem1.data("jsonObj");
		    var id = jsonObj.id;
		    var apiCommand = "command=destroyVirtualMachine&id="+id;  
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);				   	                         					    
	    }, 
	    "Cancel": function() { 
		    $(this).dialog("close"); 
			
	    } 
    }).dialog("open");
}   
  
function doRestoreVM($actionLink, $detailsTab, $midmenuItem1) {   
    $("#dialog_confirmation_restore_vm")	
    .dialog('option', 'buttons', { 						
	    "Confirm": function() { 
		    $(this).dialog("close"); 			
		    
		    var jsonObj = $midmenuItem1.data("jsonObj");
		    var id = jsonObj.id;
		    var apiCommand = "command=recoverVirtualMachine&id="+id;  
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);				   	                         					    
	    }, 
	    "Cancel": function() { 
		    $(this).dialog("close"); 
			
	    } 
    }).dialog("open");
}   
 
function doEditVM($actionLink, $detailsTab, $midmenuItem1) {       
    var $readonlyFields  = $detailsTab.find("#vmname, #group, #haenable");
    var $editFields = $detailsTab.find("#vmname_edit, #group_edit, #haenable_edit"); 
           
    $readonlyFields.hide();
    $editFields.show();  
    $detailsTab.find("#cancel_button, #save_button").show();
    
    $detailsTab.find("#cancel_button").unbind("click").bind("click", function(event){    
        $editFields.hide();
        $readonlyFields.show();   
        $("#save_button, #cancel_button").hide();       
        return false;
    });
    $detailsTab.find("#save_button").unbind("click").bind("click", function(event){        
        doEditVM2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields);   
        return false;
    });   
}

function doEditVM2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields) {   
    // validate values
    var isValid = true;					
    isValid &= validateString("Name", $detailsTab.find("#name_edit"), $detailsTab.find("#name_edit_errormsg"), true);  //optional
    isValid &= validateString("Display Text", $detailsTab.find("#group_edit"), $detailsTab.find("#group_edit_errormsg"), true);	//optional	
    if (!isValid) 
        return;
       
    var jsonObj = $detailsTab.data("jsonObj"); 
	var id = jsonObj.id;	
	
	var array1 = [];						
	var name = trim($detailsTab.find("#vmname_edit").val());
	array1.push("&displayName="+todb(name));	
	
	var group = trim($detailsTab.find("#group_edit").val());
	array1.push("&group="+todb(group));
	
	var haenable = $detailsTab.find("#haenable_edit").val();     
	array1.push("&haenable="+haenable);   
	
	$.ajax({
	    data: createURL("command=updateVirtualMachine&id="+id+array1.join("")),
		dataType: "json",
		success: function(json) {	
		    //call listVirtualMachine to get embedded object until bug 6489 ("updateVirtualMachine API should return an embedded object on success") is fixed.
            var jsonObj;         
            $.ajax({
                data: createURL("command=listVirtualMachines&id="+id),
                dataType: "json",
                async: false,
                success: function(json) {                                
                    jsonObj = json.listvirtualmachinesresponse.virtualmachine[0];                    
                }
            });
         
            vmToMidmenu(jsonObj, $midmenuItem1);
            vmToRightPanel($midmenuItem1);	
            
            $editFields.hide();      
            $readonlyFields.show();       
            $("#save_button, #cancel_button").hide();          					
		}
	});
} 
         
function doAttachISO($actionLink, $detailsTab, $midmenuItem1) {   
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
					isoSelect.append("<option value='"+isos[i].id+"'>"+fromdb(isos[i].displaytext)+"</option>");;
				}
			}
		}
	});
	
	$("#dialog_attach_iso")
	.dialog('option', 'buttons', { 						
		"OK": function() { 	
		    var $thisDialog = $(this);
		    				
			var isValid = true;				
			isValid &= validateDropDownBox("ISO", $thisDialog.find("#attach_iso_select"), $thisDialog.find("#attach_iso_select_errormsg"));	
			if (!isValid) 
			    return;
			    
			$thisDialog.dialog("close");		
				
			var isoId = $("#dialog_attach_iso #attach_iso_select").val();	
			
			var jsonObj = $midmenuItem1.data("jsonObj");
			var id = jsonObj.id;
			var apiCommand = "command=attachIso&virtualmachineid="+id+"&id="+isoId;
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);						
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 			
		} 
	}).dialog("open");
}

function doDetachISO($actionLink, $detailsTab, $midmenuItem1) {  
    $("#dialog_detach_iso_from_vm")	
	.dialog('option', 'buttons', { 						
		"OK": function() { 
			$(this).dialog("close");	
			
			var jsonObj = $midmenuItem1.data("jsonObj");
			var id = jsonObj.id;
			var apiCommand = "command=detachIso&virtualmachineid="+id;
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);							
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 			
		} 
	}).dialog("open");
}

function doResetPassword($actionLink, $detailsTab, $midmenuItem1) {   		
	$("#dialog_confirmation_change_root_password")	
	.dialog('option', 'buttons', { 						
		"Yes": function() { 
			$(this).dialog("close"); 
						
			var jsonObj = $midmenuItem1.data("jsonObj");				
			if(jsonObj.passwordenabled != true) {
			    var $afterActionInfoContainer = $("#right_panel_content #after_action_info_container_on_top");
			    $afterActionInfoContainer.find("#after_action_info").text("Reset password failed. Reason: This instance is not using a template that has the password reset feature enabled.  If you have forgotten your root password, please contact support.");  
			    $afterActionInfoContainer.addClass("errorbox").show();
			    return;
			}			
			
			var id = jsonObj.id;
			var apiCommand = "command=resetPasswordForVirtualMachine&id="+id;    
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);				
		}, 
		"No": function() { 
			$(this).dialog("close"); 			
		} 
	}).dialog("open");
}

function doChangeService($actionLink, $detailsTab, $midmenuItem1) {    
    var jsonObj = $midmenuItem1.data("jsonObj");
	var id = jsonObj.id;
	
	$.ajax({	   
	    data: createURL("command=listServiceOfferings&VirtualMachineId="+id), 
		dataType: "json",
		async: false,
		success: function(json) {
			var offerings = json.listserviceofferingsresponse.serviceoffering;
			var offeringSelect = $("#dialog_change_service_offering #change_service_offerings").empty();
			
			if (offerings != null && offerings.length > 0) {
				for (var i = 0; i < offerings.length; i++) {
					var option = $("<option value='" + offerings[i].id + "'>" + fromdb(offerings[i].displaytext) + "</option>").data("name", fromdb(offerings[i].name));
					offeringSelect.append(option); 
				}
			} 
		}
	});
	
	$("#dialog_change_service_offering")
	.dialog('option', 'buttons', { 						
		"OK": function() { 
		    var $thisDialog = $(this);
		    		   
		    var isValid = true;				
			isValid &= validateDropDownBox("Service Offering", $thisDialog.find("#change_service_offerings"), $thisDialog.find("#change_service_offerings_errormsg"));	
			if (!isValid) 
			    return;
		    
			$thisDialog.dialog("close"); 
			var serviceOfferingId = $thisDialog.find("#change_service_offerings").val();
						
			if(jsonObj.state != "Stopped") {				    
		        $midmenuItem1.find("#info_icon").addClass("error").show();
                $midmenuItem1.data("afterActionInfo", ($actionLink.data("label") + " action failed. Reason: virtual instance needs to be stopped before you can change its service."));  
	        }
            var apiCommand = "command=changeServiceForVirtualMachine&id="+id+"&serviceOfferingId="+serviceOfferingId;	     
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);				
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 			
		} 
	}).dialog("open");
}

function vmToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.data("jsonObj", jsonObj);
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));   
      
    var vmName = getVmName(jsonObj.name, jsonObj.displayname);
    $midmenuItem1.find("#first_row").text(vmName);        
    $midmenuItem1.find("#second_row").text(jsonObj.ipaddress); 
    updateVmStateInMidMenu(jsonObj, $midmenuItem1);     
    
    $midmenuItem1.data("toRightPanelFn", vmToRightPanel);   
}

function vmToRightPanel($midmenuItem1) {
    var jsonObj = $midmenuItem1.data("jsonObj");          
    
    var vmName = getVmName(jsonObj.name, jsonObj.displayname);        
    $("right_panel_header").find("#vm_name").text(vmName);	
     
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1); 
     
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);
    $("#tab_details").click();   
}
  
function vmJsonToDetailsTab(){  
    var $thisTab = $("#right_panel_content #tab_content_details");  
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();        
    
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    var id = $midmenuItem1.data("jsonObj").id;
    
    var jsonObj;   
    $.ajax({
        data: createURL("command=listVirtualMachines&id="+id),
        dataType: "json",
        async: false,
        success: function(json) {  
            var items = json.listvirtualmachinesresponse.virtualmachine;
            if(items != null && items.length > 0)
                jsonObj = items[0];
        }
    });        
    $thisTab.data("jsonObj", jsonObj);    
    $midmenuItem1.data("jsonObj", jsonObj);    

    resetViewConsoleAction(jsonObj, $thisTab);      
    setVmStateInRightPanel(jsonObj.state, $thisTab.find("#state"));		
    $thisTab.find("#ipAddress").text(jsonObj.ipaddress);
    
    $thisTab.find("#zoneName").text(fromdb(jsonObj.zonename));
           
    var vmName = getVmName(jsonObj.name, jsonObj.displayname);        
    $thisTab.find("#title").text(vmName);
    
    $thisTab.find("#vmname").text(vmName);
    $thisTab.find("#vmname_edit").val(fromdb(jsonObj.displayname));
    
    $thisTab.find("#ipaddress").text(fromdb(jsonObj.ipaddress));
    
    $thisTab.find("#templateName").text(fromdb(jsonObj.templatename));
    $thisTab.find("#serviceOfferingName").text(fromdb(jsonObj.serviceofferingname));		
    $thisTab.find("#created").text(jsonObj.created);
    $thisTab.find("#account").text(fromdb(jsonObj.account));
    $thisTab.find("#domain").text(fromdb(jsonObj.domain));
    $thisTab.find("#hostName").text(fromdb(jsonObj.hostname));
    
    $thisTab.find("#group").text(fromdb(jsonObj.group));	
    $thisTab.find("#group_edit").val(fromdb(jsonObj.group));	
    
    setBooleanReadField(jsonObj.haenable, $thisTab.find("#haenable"));
    setBooleanEditField(jsonObj.haenable, $thisTab.find("#haenable_edit"));
    //$thisTab.find("#haenable_edit").val(jsonObj.haenable);
    	
    setBooleanReadField((jsonObj.isoid != null), $thisTab.find("#iso"));	
      
    //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();              
	  
	buildActionLinkForTab("Edit Instance", vmActionMap, $actionMenu, $midmenuItem1, $thisTab); 
	           
    // Show State of the VM
	if (jsonObj.state == 'Destroyed') {
		buildActionLinkForTab("Restore Instance", vmActionMap, $actionMenu, $midmenuItem1, $thisTab);
		//to hide view console in details tab....(to-do)
		//to hide volume tab....(to-do)	
	} 
	else if (jsonObj.state == 'Running') {
		//instanceTemplate.find("#vm_action_start, #vm_action_reset_password, #vm_action_change_service").removeClass().addClass("vmaction_links_off");
		buildActionLinkForTab("Stop Instance", vmActionMap, $actionMenu, $midmenuItem1, $thisTab);
		buildActionLinkForTab("Reboot Instance", vmActionMap, $actionMenu, $midmenuItem1, $thisTab);
		buildActionLinkForTab("Destroy Instance", vmActionMap, $actionMenu, $midmenuItem1, $thisTab);
		
		if (jsonObj.isoid == null)	
	        buildActionLinkForTab("Attach ISO", vmActionMap, $actionMenu, $midmenuItem1, $thisTab);
	    else 		
	        buildActionLinkForTab("Detach ISO", vmActionMap, $actionMenu, $midmenuItem1, $thisTab);	
	} 
	else {	    
		if (jsonObj.state == 'Stopped') {
			//instanceTemplate.find("#vm_action_stop, #vm_action_reboot").removeClass().addClass("vmaction_links_off");
			buildActionLinkForTab("Start Instance", vmActionMap, $actionMenu, $midmenuItem1, $thisTab);		    
		    buildActionLinkForTab("Destroy Instance", vmActionMap, $actionMenu, $midmenuItem1, $thisTab);
		    
		    if (jsonObj.isoid == null)	
		        buildActionLinkForTab("Attach ISO", vmActionMap, $actionMenu, $midmenuItem1, $thisTab);
		    else 		
		       buildActionLinkForTab("Detach ISO", vmActionMap, $actionMenu, $midmenuItem1, $thisTab);				    
		    
		    buildActionLinkForTab("Reset Password", vmActionMap, $actionMenu, $midmenuItem1, $thisTab);
		    buildActionLinkForTab("Change Service", vmActionMap, $actionMenu, $midmenuItem1, $thisTab);			    					
		} 
		else { //jsonObj.state == "Starting", "Creating", ~~~ 	
			if(jsonObj.state != 'Creating')
			    buildActionLinkForTab("Destroy Instance", vmActionMap, $actionMenu, $midmenuItem1, $thisTab);
			    
			//instanceTemplate.find("#vm_action_start, #vm_action_stop, #vm_action_reboot, #vm_action_attach_iso, #vm_action_detach_iso, #vm_action_reset_password, #vm_action_change_service").removeClass().addClass("vmaction_links_off");
	    }
		//to hide view console in details tab....(to-do)
	}   
	
	$thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();       
}

function vmJsonToVolumeTab() {       	
	var $thisTab = $("#right_panel_content #tab_content_volume");  
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   
		
	var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");	
	var jsonObj = $midmenuItem1.data("jsonObj");	

    $.ajax({
		cache: false,
		data: createURL("command=listVolumes&virtualMachineId="+jsonObj.id+maxPageSize),
		dataType: "json",
		success: function(json) {			    
			var items = json.listvolumesresponse.volume;
			if (items != null && items.length > 0) {
				var $container = $thisTab.find("#tab_container").empty();
				var template = $("#volume_tab_template");				
				for (var i = 0; i < items.length; i++) {
					var newTemplate = template.clone(true);
	                vmVolumeJSONToTemplate(items[i], newTemplate); 
	                $container.append(newTemplate.show());	
				}
			}	
			$thisTab.find("#tab_spinning_wheel").hide();    
            $thisTab.find("#tab_container").show();    						
		}
	});          
}
 
function vmJsonToStatisticsTab() {
    var $thisTab = $("#right_panel_content #tab_content_statistics");  
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   

    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    var jsonObj = $midmenuItem1.data("jsonObj");
    
    var $barChartContainer = $thisTab.find("#cpu_barchart");
         
    var cpuNumber = ((jsonObj.cpunumber==null)? "":jsonObj.cpunumber.toString());
    $barChartContainer.find("#cpunumber").text(cpuNumber);
    
    var cpuSpeed = ((jsonObj.cpuspeed==null)? "":convertHz(jsonObj.cpuspeed)) ;
    $barChartContainer.find("#cpuspeed").text(cpuSpeed);
    
    $barChartContainer.find("#bar_chart").removeClass().addClass("db_barbox").css("width", "0%");    
    $barChartContainer.find("#percentused").text("");   
    if(jsonObj.cpuused!=null)
        drawBarChart($barChartContainer, jsonObj.cpuused);		
    
    var networkKbsRead = ((jsonObj.networkkbsread==null)? "":convertBytes(jsonObj.networkkbsread * 1024));
    $thisTab.find("#networkkbsread").text(networkKbsRead);
    
    var networkKbsWrite = ((jsonObj.networkkbswrite==null)? "":convertBytes(jsonObj.networkkbswrite * 1024));
    $thisTab.find("#networkkbswrite").text(networkKbsWrite);
    
    $thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();  
}
   
function drawBarChart($capacity, percentused) { //percentused == "0.01%" (having % inside)    
    $capacity.find("#percentused").text(percentused);
    
    var percentusedFloat; 
    if(percentused.indexOf("%") != -1) {
        percentused = percentused.replace("%", "");
        percentusedFloat = parseFloat(percentused);
        percentusedFloat = percentusedFloat * 0.01;   //because % is removed.  percentusedFloat == 0.0001
    }
    else {
        percentusedFloat = parseFloat(percentused);
    }
      
    if (percentusedFloat <= 0.6)
        $capacity.find("#bar_chart").removeClass().addClass("db_barbox low").css("width", percentused); 
    else if (percentusedFloat > 0.6 && percentusedFloat <= 0.8 )
        $capacity.find("#bar_chart").removeClass().addClass("db_barbox mid").css("width", percentused);
    else if (percentusedFloat > 0.8 )
        $capacity.find("#bar_chart").removeClass().addClass("db_barbox high").css("width", percentused);
}   
    
function vmJsonToRouterTab() {   
    var $thisTab = $("#right_panel_content #tab_content_router");  
	$thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   

    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    var vmObj = $midmenuItem1.data("jsonObj");

    $.ajax({
		cache: false,
		data: createURL("command=listRouters&domainid="+vmObj.domainid+"&account="+vmObj.account+maxPageSize),
		dataType: "json",
		success: function(json) {				      
			var items = json.listroutersresponse.router;
			if (items != null && items.length > 0) {
				var $container = $thisTab.find("#tab_container").empty();
				var template = $("#router_tab_template");				
				for (var i = 0; i < items.length; i++) {
					var newTemplate = template.clone(true);
	                vmRouterJSONToTemplate(items[i], newTemplate); 
	                $container.append(newTemplate.show());	
				}
			}
			$thisTab.find("#tab_spinning_wheel").hide();    
            $thisTab.find("#tab_container").show();    				
		}
	});          
}    
    
function vmClearRightPanel(jsonObj) {       
    $("#right_panel_header").find("#vm_name").text("");	
    setVmStateInRightPanel("");	
    
    var $rightPanelContent = $("#right_panel_content"); 
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

//***** declaration for volume tab (begin) *********************************************************
var vmVolumeActionMap = {  
    "Detach Disk": {
        api: "detachVolume",            
        isAsyncJob: true,
        asyncJobResponse: "detachvolumeresponse",
        inProcessText: "Detaching disk....",
        afterActionSeccessFn: function(json, id, $subgridItem) {         
            $subgridItem.slideUp("slow", function(){                   
                $(this).remove();
            });
        }
    },
    "Create Template": {
        isAsyncJob: true,
        asyncJobResponse: "createtemplateresponse",            
        dialogBeforeActionFn : doCreateTemplateFromVmVolume,
        inProcessText: "Creating template....",
        afterActionSeccessFn: function(json, id, $subgridItem) {}         
    }  
}     

function vmVolumeJSONToTemplate(json, $template) {
    $template.attr("id","vm_volume_"+json.id);	        
    $template.data("jsonObj", json);    
    $template.find("#title").text(fromdb(json.name));    
	$template.find("#id").text(json.id);	
	$template.find("#name").text(fromdb(json.name));
	if (json.storagetype == "shared") 
		$template.find("#type").text(json.type + " (shared storage)");
	else 
		$template.find("#type").text(json.type + " (local storage)");
			
	$template.find("#size").text((json.size == "0") ? "" : convertBytes(json.size));										
	setDateField(json.created, $template.find("#created"));
	
	//***** actions (begin) *****
	var $actionLink = $template.find("#volume_action_link");		
	$actionLink.unbind("mouseover").bind("mouseover", function(event) {
        $(this).find("#volume_action_menu").show();    
        return false;
    });
    $actionLink.unbind("mouseout").bind("mouseout", function(event) {
        $(this).find("#volume_action_menu").hide();    
        return false;
    });		
	
	var $actionMenu = $actionLink.find("#volume_action_menu");
    $actionMenu.find("#action_list").empty();
    var noAvailableActions = true;
     
    var hasCreateTemplate = false;        
	if(json.type=="ROOT") { //"create template" is allowed(when stopped), "detach disk" is disallowed.
		if (json.vmstate == "Stopped") {
		    buildActionLinkForSubgridItem("Create Template", vmVolumeActionMap, $actionMenu, $template);	
		    hasCreateTemplate = true;
		    noAvailableActions = false;		
		}
	} 
	else { //json.type=="DATADISK": "detach disk" is allowed, "create template" is disallowed.			
		buildActionLinkForSubgridItem("Detach Disk", vmVolumeActionMap, $actionMenu, $template);		
		noAvailableActions = false;				
	}	
	
	if (getHypervisorType() == "kvm" && hasCreateTemplate == false) {
	    buildActionLinkForSubgridItem("Create Template", vmVolumeActionMap, $actionMenu, $template);	
	    noAvailableActions = false;	
	}
	
	// no available actions 
	if(noAvailableActions == true) {	    
	    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
	}	  
	//***** actions (end) *****		
}
	
function vmRouterJSONToTemplate(jsonObj, $template) {	
    $template.data("jsonObj", jsonObj);            
    $template.find("#title").text(fromdb(jsonObj.name));    
     
    resetViewConsoleAction(jsonObj, $template);   
    setVmStateInRightPanel(jsonObj.state, $template.find("#state"));
    $template.find("#ipAddress").text(jsonObj.publicip);
    
    $template.find("#zonename").text(fromdb(jsonObj.zonename));
    $template.find("#name").text(fromdb(jsonObj.name));
    $template.find("#publicip").text(fromdb(jsonObj.publicip));
    $template.find("#privateip").text(fromdb(jsonObj.privateip));
    $template.find("#guestipaddress").text(fromdb(jsonObj.guestipaddress));
    $template.find("#hostname").text(fromdb(jsonObj.hostname));
    $template.find("#networkdomain").text(fromdb(jsonObj.networkdomain));
    $template.find("#account").text(fromdb(jsonObj.account));  
    setDateField(jsonObj.created, $template.find("#created"));	
    
    //***** actions (begin) *****
	var $actionLink = $template.find("#router_action_link");	
	$actionLink.unbind("mouseover").bind("mouseover", function(event) {
        $(this).find("#router_action_menu").show();    
        return false;
    });       
    $actionLink.unbind("mouseout").bind("mouseout", function(event) {
        $(this).find("#router_action_menu").hide();    
        return false;
    });		
	
	var $actionMenu = $actionLink.find("#router_action_menu");
    $actionMenu.find("#action_list").empty();
    var noAvailableActions = true;
    
	 if (jsonObj.state == 'Running') {
	    buildActionLinkForSubgridItem("Stop Router", vmRouterActionMap, $actionMenu, $template);
	    buildActionLinkForSubgridItem("Reboot Router", vmRouterActionMap, $actionMenu, $template);
	    noAvailableActions = false;		
    }
    else if (jsonObj.state == 'Stopped') {     
        buildActionLinkForSubgridItem("Start Router", vmRouterActionMap, $actionMenu, $template);  
        noAvailableActions = false;		 
    }          
    
    // no available actions 
	if(noAvailableActions == true) {
	    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
	}	        
	//***** actions (end) *****		
}	



//***** declaration for volume tab (end) *********************************************************

function appendInstanceGroup(groupId, groupName) {
    var $leftmenuSubmenuTemplate = $("#leftmenu_submenu_template").clone().show();			        	    
    $leftmenuSubmenuTemplate.attr("id", ("leftmenu_instance_group_"+groupId));		
    $leftmenuSubmenuTemplate.data("groupId", groupId)	        	            	
    $leftmenuSubmenuTemplate.find("#submenu_name").text(groupName);
    $leftmenuSubmenuTemplate.find("#icon").attr("src", "images/instance_leftmenuicon.png").show();
     		                			                
    $leftmenuSubmenuTemplate.bind("click", function(event) { 
        $("#midmenu_container").empty();
        selectedItemsInMidMenu = {};
                                    
        var groupId = $(this).data("groupId");                                   
        $.ajax({
            cache: false,
            data: createURL("command=listVirtualMachines&groupid="+groupId+"&pagesize="+midmenuItemCount),
            dataType: "json",
            success: function(json) {		                                                             
                var instances = json.listvirtualmachinesresponse.virtualmachine;    
                if (instances != null && instances.length > 0) {
                    var $template = $("#midmenu_item"); 	                           
                    for(var i=0; i<instances.length;i++) {  
                        var $midmenuItem1 = $template.clone();                                                                                                                                              
                        vmToMidmenu(instances[i], $midmenuItem1); 
                        bindClickToMidMenu($midmenuItem1, vmToRightPanel, getMidmenuId);  
                        $("#midmenu_container").append($midmenuItem1.show()); 
                        if(i == 0) {  //click the 1st item in middle menu as default  
                            $midmenuItem1.click();                               
                            $midmenuItem1.addClass("ui-selected");  //because instance page is using JQuery selectable widget to do multiple-selection
                            selectedItemsInMidMenu[instances[i].id] = $midmenuItem1; //because instance page is using JQuery selectable widget to do multiple-selection
                        }                    
                    }  
                }  
            }
        });                            
        return false;
    });	
    $("#leftmenu_instance_group_container").append($leftmenuSubmenuTemplate);
}	

function doCreateTemplateFromVmVolume($actionLink, $subgridItem) {       
    var jsonObj = $subgridItem.data("jsonObj");
    
	$("#dialog_create_template")
	.dialog('option', 'buttons', { 						
		"OK": function() { 		
		    var thisDialog = $(this);		    
									
			// validate values
	        var isValid = true;					
	        isValid &= validateString("Name", thisDialog.find("#create_template_name"), thisDialog.find("#create_template_name_errormsg"));
			isValid &= validateString("Display Text", thisDialog.find("#create_template_desc"), thisDialog.find("#create_template_desc_errormsg"));			
	        if (!isValid) 
	            return;		
	        
	        thisDialog.dialog("close"); 
	        
	        var name = trim(thisDialog.find("#create_template_name").val());
			var desc = trim(thisDialog.find("#create_template_desc").val());
			var osType = thisDialog.find("#create_template_os_type").val();					
			var isPublic = thisDialog.find("#create_template_public").val();
            var password = thisDialog.find("#create_template_password").val();				
			
			var id = $subgridItem.data("jsonObj").id;			
			var apiCommand = "command=createTemplate&volumeId="+id+"&name="+todb(name)+"&displayText="+todb(desc)+"&osTypeId="+osType+"&isPublic="+isPublic+"&passwordEnabled="+password;
	    	doActionToSubgridItem(id, $actionLink, apiCommand, $subgridItem);					
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		} 
	}).dialog("open");
}   

//***** Routers tab (begin) ***************************************************************************************

function vmRouterAfterSubgridItemAction(json, id, $subgridItem) {        
    //var jsonObj = json.queryasyncjobresultresponse.router[0];  
    //vmRouterJSONToTemplate(jsonObj, $subgridItem);
    
    //This is a temporary fix until bug 6787("RebootRouter API should return an embedded object on success") is fixed.
    var $detailsTab = $("#right_panel_content #tab_content_details");  
    var vmObj = $detailsTab.data("jsonObj");  
    vmJsonToRouterTab(vmObj);   
}     
  
var vmRouterActionMap = {      
    "Start Router": {                
        isAsyncJob: true,
        asyncJobResponse: "startrouterresponse",
        inProcessText: "Starting Router....",
        dialogBeforeActionFn : doStartVmRouter,
        afterActionSeccessFn: vmRouterAfterSubgridItemAction
    },
    "Stop Router": {        
        isAsyncJob: true,
        asyncJobResponse: "stoprouterresponse",
        inProcessText: "Stopping Router....",
        dialogBeforeActionFn : doStopVmRouter,
        afterActionSeccessFn: vmRouterAfterSubgridItemAction
    },
    "Reboot Router": {        
        isAsyncJob: true,
        asyncJobResponse: "rebootrouterresponse",
        inProcessText: "Rebooting Router....",
        dialogBeforeActionFn : doRebootVmRouter,
        afterActionSeccessFn: vmRouterAfterSubgridItemAction
    }
}   

function doStartVmRouter($actionLink, $subgridItem) {
    $("#dialog_confirmation_start_router")	
    .dialog('option', 'buttons', { 						
	    "Confirm": function() { 
		    $(this).dialog("close"); 			
		    
		    var jsonObj = $subgridItem.data("jsonObj");
		    var id = jsonObj.id;
		    var apiCommand = "command=startRouter&id="+id;  
            doActionToSubgridItem(id, $actionLink, apiCommand, $subgridItem); 			   			   	                         					    
	    }, 
	    "Cancel": function() { 
		    $(this).dialog("close"); 
			
	    } 
    }).dialog("open");
}   

function doStopVmRouter($actionLink, $subgridItem) {
    $("#dialog_confirmation_stop_router")	
    .dialog('option', 'buttons', { 						
	    "Confirm": function() { 
		    $(this).dialog("close"); 			
		    
		    var jsonObj = $subgridItem.data("jsonObj");
		    var id = jsonObj.id;
		    var apiCommand = "command=stopRouter&id="+id;              
            doActionToSubgridItem(id, $actionLink, apiCommand, $subgridItem); 			   	                         					    
	    }, 
	    "Cancel": function() { 
		    $(this).dialog("close"); 
			
	    } 
    }).dialog("open");
}   
   
function doRebootVmRouter($actionLink, $subgridItem) {
    $("#dialog_confirmation_reboot_router")	
    .dialog('option', 'buttons', { 						
	    "Confirm": function() { 
		    $(this).dialog("close"); 			
		    
		    var jsonObj = $subgridItem.data("jsonObj");
		    var id = jsonObj.id;
		    var apiCommand = "command=rebootRouter&id="+id;  
            doActionToSubgridItem(id, $actionLink, apiCommand, $subgridItem); 			   			   	                         					    
	    }, 
	    "Cancel": function() { 
		    $(this).dialog("close"); 
			
	    } 
    }).dialog("open");
}   
//***** Routers tab (end) ***************************************************************************************