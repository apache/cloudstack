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
    $instanceSubMenuContainer = $("#leftmenu_instance_expandedbox").empty();    
  
    if (isAdmin() || isDomainAdmin()) {
        instanceBuildSubMenu2("My Instances", ("listVirtualMachines&domainid="+g_domainid+"&account="+g_account));
        instanceBuildSubMenu2("All Instances", "listVirtualMachines");           
        instanceBuildSubMenu2("Running Instances", "listVirtualMachines&state=Running");
        instanceBuildSubMenu2("Stopped Instances", "listVirtualMachines&state=Stopped");
        instanceBuildSubMenu2("Destoryed Instances", "listVirtualMachines&state=Destroyed");
    } 	
    else if(isUser()) {	        
        instanceBuildSubMenu2("All Instances", "listVirtualMachines");           
        instanceBuildSubMenu2("Running Instances", "listVirtualMachines&state=Running");
        instanceBuildSubMenu2("Stopped Instances", "listVirtualMachines&state=Stopped");        
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

var vmActionMapForMidMenu = {};

function afterLoadInstanceJSP() {
    //Add VM button
    $("#midmenu_add_link").find("#label").text("Add VM"); 
    $("#midmenu_add_link").show(); 
		        
    //action menu	        
    $("#midmenu_action_link").show();
    $("#action_menu #action_list").empty();		        
    for(var label in vmActionMapForMidMenu) 				            
        buildActionLinkForMidMenu(label, vmActionMapForMidMenu, $("#action_menu"));	
    
    if (isAdmin() || isDomainAdmin())
        $("#right_panel_content").find("#tab_router,#tab_router").show();
    
    var $noDiskOfferingTemplate = $("#vm_popup_disk_offering_template_no");
    var $customDiskOfferingTemplate = $("#vm_popup_disk_offering_template_custom");
    var $existingDiskOfferingTemplate = $("#vm_popup_disk_offering_template_existing");
   	   
   	initDialog("dialog_detach_iso_from_vm");       	
   	initDialog("dialog_attach_iso");  
    initDialog("dialog_change_name"); 
    initDialog("dialog_change_group"); 
    initDialog("dialog_change_service_offering", 600); 
    initDialog("dialog_confirmation_change_root_password");
    initDialog("dialog_confirmation_enable_ha");  
    initDialog("dialog_confirmation_disable_ha");            
    initDialog("dialog_create_template", 400);  

    //***** switch between different tabs (begin) ********************************************************************
    var tabArray = [$("#tab_details"), $("#tab_volume"), $("#tab_statistics"), $("#tab_router")];
    var tabContentArray = [$("#tab_content_details"), $("#tab_content_volume"), $("#tab_content_statistics"), $("#tab_content_router")];
    switchBetweenDifferentTabs(tabArray, tabContentArray);       
    //***** switch between different tabs (end) **********************************************************************
    
    //***** VM Wizard (begin) ******************************************************************************
    $vmPopup = $("#vm_popup");
    var $serviceOfferingTemplate = $("#vm_popup_service_offering_template");
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
					    var $t = $serviceOfferingTemplate.clone();  						  
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
		   
        var commandString;    		  	   
        var searchInput = $vmPopup.find("#search_input").val();   
        if (selectedTemplateTypeInVmPopup != "blank") {  //template   
            var hypervisor = $vmPopup.find("#wizard_hypervisor").val();	                
            if (searchInput != null && searchInput.length > 0)                 
                commandString = "command=listTemplates&templatefilter="+selectedTemplateTypeInVmPopup+"&zoneid="+zoneId+"&hypervisor="+hypervisor+"&keyword="+searchInput+"&page="+currentPageInTemplateGridInVmPopup; 
            else
                commandString = "command=listTemplates&templatefilter="+selectedTemplateTypeInVmPopup+"&zoneid="+zoneId+"&hypervisor="+hypervisor+"&page="+currentPageInTemplateGridInVmPopup;           		    		
	    } 
	    else {  //ISO
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
								      +'<div class="rev_wiztemp_listtext">'+fromdb(items[i].displaytext)+'</div>'
								      +'<div class="rev_wiztemp_ownertext">'+fromdb(items[i].account)+'</div>'
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
            
    $vmPopup.find("#wizard_hypervisor").bind("change", function(event) {       
        var selectedHypervisor = $(this).val();
        if(selectedHypervisor != null && selectedHypervisor.length > 0)
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
			$thisPopup.find("#wizard_review_hypervisor").text($thisPopup.find("#wizard_hypervisor option:selected").text());   	
			$thisPopup.find("#wizard_review_template").text($thisPopup.find("#step1 .rev_wiztemplistbox_selected .rev_wiztemp_listtext").text()); 
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
	        if($thisPopup.find("#wiz_blank").hasClass("rev_wizmid_selectedtempbut"))  { //ISO
	            $thisPopup.find("#wizard_review_disk_offering_label").text("Root Disk Offering:");
		        $thisPopup.find("#wizard_review_disk_offering").text($thisPopup.find("#root_disk_offering_container input[name=root_disk_offering_radio]:checked").next().text());	
		    }
		    else { //template
		        var checkedRadioButton = $thisPopup.find("#data_disk_offering_container input[name=data_disk_offering_radio]:checked");			        
		        			    
		        // validate values
		        var isValid = true;		
		        if(checkedRadioButton.parent().attr("id") == "vm_popup_disk_offering_template_custom")			    
		            isValid &= validateNumber("Disk Size", $thisPopup.find("#custom_disk_size"), $thisPopup.find("#custom_disk_size_errormsg"), null, null, false);	//required	
		        else
		            isValid &= validateNumber("Disk Size", $thisPopup.find("#custom_disk_size"), $thisPopup.find("#custom_disk_size_errormsg"), null, null, true);	//optional		    		
		        if (!isValid) return;
		        
		        $thisPopup.find("#wizard_review_disk_offering_label").text("Data Disk Offering:");
		        
		        var diskOfferingName = checkedRadioButton.next().text();
		        if(checkedRadioButton.parent().attr("id") == "vm_popup_disk_offering_template_custom")
		            diskOfferingName += (" " + $thisPopup.find("#data_disk_offering_container input[name=data_disk_offering_radio]:checked").next().next().next().val() + " MB");
		        $thisPopup.find("#wizard_review_disk_offering").text(diskOfferingName);
		    }	
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
			moreCriteria.push("&hypervisor="+$thisPopup.find("#wizard_hypervisor").val());	    								
		    moreCriteria.push("&templateId="+$thisPopup.find("#step1 .rev_wiztemplistbox_selected").attr("id"));    							
		    moreCriteria.push("&serviceOfferingId="+$thisPopup.find("input:radio[name=service_offering_radio]:checked").val());
			
			var diskOfferingId;    						
		    if ($thisPopup.find("#wiz_blank").hasClass("rev_wizmid_selectedtempbut"))  //ISO
		        diskOfferingId = $thisPopup.find("#root_disk_offering_container input[name=root_disk_offering_radio]:checked").val();	
		    else  //template
		        diskOfferingId = $thisPopup.find("#data_disk_offering_container input[name=data_disk_offering_radio]:checked").val();	
	        if(diskOfferingId != null && diskOfferingId != "" && diskOfferingId != "no" && diskOfferingId != "custom")
		        moreCriteria.push("&diskOfferingId="+diskOfferingId);						 
			    			
			var customDiskSize = $thisPopup.find("#custom_disk_size").val(); //unit is MB
			if(customDiskSize != null && customDiskSize.length > 0)
			    moreCriteria.push("&size="+customDiskSize);	    
			
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
				                            vmToMidmenu(result.virtualmachine[0], $midmenuItem1);
				                            bindClickToMidMenu($midmenuItem1, vmToRightPanel, getMidmenuId);  
				                            if (result.virtualmachine[0].passwordenabled == 'true') {							                                									        
										        var extraMessage = "New password: " + result.virtualmachine[0].password;
										        afterAddingMidMenuItem($midmenuItem1, true, extraMessage);
										        var afterActionInfo = "Your instance has been successfully created.  Your new password is : " + result.virtualmachine[0].password;
										        $midmenuItem1.data("afterActionInfo", afterActionInfo); 
									        } 	
									        else {
									            afterAddingMidMenuItem($midmenuItem1, true);
									        }							                        
									    } else if (result.jobstatus == 2) {
										    // Failed
										    afterAddingMidMenuItem($midmenuItem1, false, fromdb(result.jobresult));		
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
    //***** VM Wizard (end) ********************************************************************************
    
    //***** Volume tab (begin) *****************************************************************************         
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

//***** VM Detail (begin) ******************************************************************************
      
var vmActionMap = {        
    "Stop Instance": {
        api: "stopVirtualMachine",            
        isAsyncJob: true,
        asyncJobResponse: "stopvirtualmachineresponse",
        inProcessText: "Stopping Instance....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) { 
            //call listVirtualMachine to get embedded object until bug 6486 ("StopVirtualMachine API should return an embedded object on success") is fixed.
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
    "Start Instance": {
        api: "startVirtualMachine",            
        isAsyncJob: true,
        asyncJobResponse: "startvirtualmachineresponse",
        inProcessText: "Starting Instance....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) { 
            var jsonObj = json.queryasyncjobresultresponse.jobresult.startvirtualmachineresponse;      
            vmToMidmenu(jsonObj, $midmenuItem1);
            vmToRightPanel($midmenuItem1);
        }
    },
    "Reboot Instance": {
        api: "rebootVirtualMachine",           
        isAsyncJob: true,
        asyncJobResponse: "rebootvirtualmachineresponse",
        inProcessText: "Rebooting Instance....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) { 
            var jsonObj = json.queryasyncjobresultresponse.virtualmachine[0];      
            vmToMidmenu(jsonObj, $midmenuItem1);
            vmToRightPanel($midmenuItem1);
        }
    },
    "Destroy Instance": {
        api: "destroyVirtualMachine",           
        isAsyncJob: true,
        asyncJobResponse: "destroyvirtualmachineresponse",
        inProcessText: "Destroying Instance....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) { 
            //call listVirtualMachine to get embedded object until bug 6041 ("DestroyVirtualMachine API should return an embedded object on success") is fixed.
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
    "Restore Instance": {
        api: "recoverVirtualMachine",           
        isAsyncJob: false,
        inProcessText: "Restoring Instance....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) { 
            //call listVirtualMachine to get embedded object until bug 6037 ("RecoverVirtualMachine API should return an embedded object on success") is fixed.
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
    "Change Name": {
        isAsyncJob: false,     
        inProcessText: "Changing Name....",       
        dialogBeforeActionFn : doChangeName,
        afterActionSeccessFn: function(json, $midmenuItem1, id) { 
            //call listVirtualMachine to get embedded object until bug 6489 ("updateVirtualMachine API should return an embedded object on success") is fixed.
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
    "Change Group": {
        isAsyncJob: false,      
        inProcessText: "Changing Group....",      
        dialogBeforeActionFn : doChangeGroup,
        afterActionSeccessFn: function(json, $midmenuItem1, id) { 
            //call listVirtualMachine to get embedded object until bug 6489 ("updateVirtualMachine API should return an embedded object on success") is fixed.
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
    },
    "Enable HA": {
        isAsyncJob: false,     
        inProcessText: "Enabling HA....",       
        dialogBeforeActionFn : doEnableHA,
        afterActionSeccessFn: function(json, $midmenuItem1, id) { 
            //call listVirtualMachine to get embedded object until bug 6489 ("updateVirtualMachine API should return an embedded object on success") is fixed.
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
    "Disable HA": {
        isAsyncJob: false,   
        inProcessText: "Disabling HA....",         
        dialogBeforeActionFn : doDisableHA,
        afterActionSeccessFn: function(json, $midmenuItem1, id) { 
            //call listVirtualMachine to get embedded object until bug 6489 ("updateVirtualMachine API should return an embedded object on success") is fixed.
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
    }                 
}                      
   
function doAttachISO($actionLink, $detailsTab, $midmenuItem1) {    
//function doAttachISO($actionLink, selectedItemsInMidMenu) {   
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
            doActionToDetailsTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);						
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 			
		} 
	}).dialog("open");
}

function doDetachISO($actionLink, $detailsTab, $midmenuItem1) {
//function doDetachISO($actionLink, selectedItemsInMidMenu) {    
    $("#dialog_detach_iso_from_vm")	
	.dialog('option', 'buttons', { 						
		"OK": function() { 
			$(this).dialog("close");	
			
			var jsonObj = $midmenuItem1.data("jsonObj");
			var id = jsonObj.id;
			var apiCommand = "command=detachIso&virtualmachineid="+id;
            doActionToDetailsTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);							
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 			
		} 
	}).dialog("open");
}

function doResetPassword($actionLink, $detailsTab, $midmenuItem1) {  
//function doResetPassword($actionLink, selectedItemsInMidMenu) {   		
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
            doActionToDetailsTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);				
		}, 
		"No": function() { 
			$(this).dialog("close"); 			
		} 
	}).dialog("open");
}

function doChangeName($actionLink, $detailsTab, $midmenuItem1) {  
//function doChangeName($actionLink, selectedItemsInMidMenu) { 
    var jsonObj = $midmenuItem1.data("jsonObj");
    $("#dialog_change_name").find("#change_instance_name").val(fromdb(jsonObj.displayname));
    
	$("#dialog_change_name")
	.dialog('option', 'buttons', { 						
		"OK": function() { 			    
		    var thisDialog = $(this);			    
		    											
			// validate values
	        var isValid = true;					
	        isValid &= validateString("Name", thisDialog.find("#change_instance_name"), thisDialog.find("#change_instance_name_errormsg"));								
	        if (!isValid) 
	            return;	
	        
	        thisDialog.dialog("close"); 							
			
			var name = trim(thisDialog.find("#change_instance_name").val());
			
			var id = jsonObj.id;
			var apiCommand = "command=updateVirtualMachine&id="+id+"&displayName="+todb(name);		
            doActionToDetailsTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
			
		} 
	}).dialog("open");
}

function doChangeGroup($actionLink, $detailsTab, $midmenuItem1) {  
//function doChangeGroup($actionLink, selectedItemsInMidMenu) { 
	var jsonObj = $midmenuItem1.data("jsonObj");
    $("#dialog_change_group").find("#change_group_name").val(fromdb(jsonObj.group));
		
	$("#dialog_change_group")
	.dialog('option', 'buttons', { 						
		"OK": function() { 	
		    var thisDialog = $(this);	        
											
			// validate values
	        var isValid = true;					
	        isValid &= validateString("Group", thisDialog.find("#change_group_name"), thisDialog.find("#change_group_name_errormsg"), true); //group name is optional								
	        if (!isValid) 
	            return;
	        
	        thisDialog.dialog("close"); 
	        
	        var group = trim(thisDialog.find("#change_group_name").val());
	        
			var id = jsonObj.id;
			var apiCommand = "command=updateVirtualMachine&id="+id+"&group="+todb(group);  
            doActionToDetailsTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
			
		} 
	}).dialog("open");	
}

function doChangeService($actionLink, $detailsTab, $midmenuItem1) {  
//function doChangeService($actionLink, selectedItemsInMidMenu) {    
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
            doActionToDetailsTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);				
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 			
		} 
	}).dialog("open");
}

function doEnableHA($actionLink, $detailsTab, $midmenuItem1) {  
//function doEnableHA($actionLink, selectedItemsInMidMenu) {            
	$("#dialog_confirmation_enable_ha")	
	.dialog('option', 'buttons', { 						
		"Confirm": function() { 
			$(this).dialog("close"); 
			var jsonObj = $midmenuItem1.data("jsonObj");
			var id = jsonObj.id;
			var apiCommand = "command=updateVirtualMachine&id="+id+"&haenable=true";        
            doActionToDetailsTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);								    
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
			
		} 
	}).dialog("open");
}

function doDisableHA($actionLink, $detailsTab, $midmenuItem1) {  
//function doDisableHA($actionLink, selectedItemsInMidMenu) {       
    $("#dialog_confirmation_disable_ha")	
	.dialog('option', 'buttons', { 						
		"Confirm": function() { 
			$(this).dialog("close"); 
			var jsonObj = $midmenuItem1.data("jsonObj");
			var id = jsonObj.id;
			var apiCommand = "command=updateVirtualMachine&id="+id+"&haenable=false";        
            doActionToDetailsTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);							    
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
    updateStateInMidMenu(jsonObj, $midmenuItem1);     
    
    $midmenuItem1.data("toRightPanelFn", vmToRightPanel);   
}

function vmToRightPanel($midmenuItem1) {
    var jsonObj = $midmenuItem1.data("jsonObj");          
    
    var vmName = getVmName(jsonObj.name, jsonObj.displayname);        
    $("right_panel_header").find("#vm_name").text(vmName);	
    
    //Comment the following code which is for middle menu action, not details tab action.
    /*
    var $rightPanelContent = $("#right_panel_content");        
    if($midmenuItem1.find("#info_icon").css("display") != "none") {                
        $rightPanelContent.find("#after_action_info").text($midmenuItem1.data("afterActionInfo"));
        if($midmenuItem1.find("#info_icon").hasClass("error"))
            $rightPanelContent.find("#after_action_info_container").addClass("errorbox");
         else
            $rightPanelContent.find("#after_action_info_container").removeClass("errorbox");                                        
        $rightPanelContent.find("#after_action_info_container").show();                                         
    } 
    else {
        $rightPanelContent.find("#after_action_info").text("");
        $rightPanelContent.find("#after_action_info_container").hide();                
    }
    */
    
    vmJsonToDetailsTab($midmenuItem1);   
    vmJsonToVolumeTab(jsonObj);
    
    if (isAdmin() || isDomainAdmin())
        vmJsonToRouterTab(jsonObj);
}
 
function vmJsonToDetailsTab($midmenuItem1){
    var jsonObj = $midmenuItem1.data("jsonObj");
    var $detailsTab = $("#right_panel_content #tab_content_details");  
    $detailsTab.data("jsonObj", jsonObj);  

    resetViewConsoleAction(jsonObj, $detailsTab);      
    setVmStateInRightPanel(jsonObj.state, $detailsTab.find("#state"));		
    $detailsTab.find("#ipAddress").text(jsonObj.ipaddress);
    
    $detailsTab.find("#zoneName").text(fromdb(jsonObj.zonename));
           
    var vmName = getVmName(jsonObj.name, jsonObj.displayname);        
    $detailsTab.find("#title").text(vmName);
    $detailsTab.find("#vmname").text(vmName);
    $detailsTab.find("#ipaddress").text(fromdb(jsonObj.ipaddress));
    
    $detailsTab.find("#templateName").text(fromdb(jsonObj.templatename));
    $detailsTab.find("#serviceOfferingName").text(fromdb(jsonObj.serviceofferingname));		
    $detailsTab.find("#created").text(jsonObj.created);
    $detailsTab.find("#account").text(fromdb(jsonObj.account));
    $detailsTab.find("#domain").text(fromdb(jsonObj.domain));
    $detailsTab.find("#hostName").text(fromdb(jsonObj.hostname));
    $detailsTab.find("#group").text(fromdb(jsonObj.group));	
    
    setBooleanField(jsonObj.haenable, $detailsTab.find("#haenable"));	
    setBooleanField((jsonObj.isoid != null), $detailsTab.find("#iso"));	
      
    //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();              
	           
    // Show State of the VM
	if (jsonObj.state == 'Destroyed') {
		buildActionLinkForDetailsTab("Restore Instance"  , vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);
		//to hide view console in details tab....(to-do)
		//to hide volume tab....(to-do)	
	} 
	else if (jsonObj.state == 'Running') {
		//instanceTemplate.find("#vm_action_start, #vm_action_reset_password, #vm_action_change_service").removeClass().addClass("vmaction_links_off");
		buildActionLinkForDetailsTab("Stop Instance", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);
		buildActionLinkForDetailsTab("Reboot Instance", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);
		buildActionLinkForDetailsTab("Destroy Instance", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);
		
		if (jsonObj.isoid == null)	
	        buildActionLinkForDetailsTab("Attach ISO", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);
	    else 		
	       buildActionLinkForDetailsTab("Detach ISO", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);		
		
		buildActionLinkForDetailsTab("Change Name", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);
		buildActionLinkForDetailsTab("Change Group", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);						
			
	    if (jsonObj.haenable == true) 	
            buildActionLinkForDetailsTab("Disable HA", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);
	    else 		
		    buildActionLinkForDetailsTab("Enable HA", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);	
	} 
	else {	    
		if (jsonObj.state == 'Stopped') {
			//instanceTemplate.find("#vm_action_stop, #vm_action_reboot").removeClass().addClass("vmaction_links_off");
			buildActionLinkForDetailsTab("Start Instance", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);		    
		    buildActionLinkForDetailsTab("Destroy Instance", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);
		    
		    if (jsonObj.isoid == null)	
		        buildActionLinkForDetailsTab("Attach ISO", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);
		    else 		
		       buildActionLinkForDetailsTab("Detach ISO", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);				    
		    
		    buildActionLinkForDetailsTab("Reset Password", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);
		    buildActionLinkForDetailsTab("Change Name", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);
		    buildActionLinkForDetailsTab("Change Service", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);	
		    buildActionLinkForDetailsTab("Change Group", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);	
		    
		    if (jsonObj.haenable == true) 	
                buildActionLinkForDetailsTab("Disable HA", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);
	        else 		
		        buildActionLinkForDetailsTab("Enable HA", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);					
		} 
		else { //jsonObj.state == "Starting", "Creating", ~~~ 	
			if(jsonObj.state != 'Creating')
			    buildActionLinkForDetailsTab("Destroy Instance", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);
			    
			//instanceTemplate.find("#vm_action_start, #vm_action_stop, #vm_action_reboot, #vm_action_attach_iso, #vm_action_detach_iso, #vm_action_reset_password, #vm_action_change_service").removeClass().addClass("vmaction_links_off");
			buildActionLinkForDetailsTab("Change Name", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);
		    buildActionLinkForDetailsTab("Change Group", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);
									
			if (jsonObj.haenable == true ) 	
                buildActionLinkForDetailsTab("Disable HA", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);
	        else 		
		        buildActionLinkForDetailsTab("Enable HA", vmActionMap, $actionMenu, $midmenuItem1, $detailsTab);	
		}
		//to hide view console in details tab....(to-do)
	}       
}

function vmJsonToVolumeTab(jsonObj) {
    //volume tab
    //if (getHypervisorType() == "kvm") 
		//detail.find("#volume_action_create_template").show();		

    $.ajax({
		cache: false,
		data: createURL("command=listVolumes&virtualMachineId="+jsonObj.id+maxPageSize),
		dataType: "json",
		success: function(json) {			    
			var items = json.listvolumesresponse.volume;
			if (items != null && items.length > 0) {
				var container = $("#right_panel_content #tab_content_volume").empty();
				var template = $("#volume_tab_template");				
				for (var i = 0; i < items.length; i++) {
					var newTemplate = template.clone(true);
	                vmVolumeJSONToTemplate(items[i], newTemplate); 
	                container.append(newTemplate.show());	
				}
			}						
		}
	});          
}
    
function vmJsonToRouterTab(jsonObj) {   
    $.ajax({
		cache: false,
		data: createURL("command=listRouters&domainid="+jsonObj.domainid+"&account="+jsonObj.account+maxPageSize),
		dataType: "json",
		success: function(json) {				      
			var items = json.listroutersresponse.router;
			if (items != null && items.length > 0) {
				var container = $("#right_panel_content #tab_content_router").empty();
				var template = $("#router_tab_template");				
				for (var i = 0; i < items.length; i++) {
					var newTemplate = template.clone(true);
	                vmRouterJSONToTemplate(items[i], newTemplate); 
	                container.append(newTemplate.show());	
				}
			}						
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
    
	if(json.type=="ROOT") { //"create template" is allowed(when stopped), "detach disk" is disallowed.
		if (json.vmstate == "Stopped") {
		    buildActionLinkForSubgridItem("Create Template", vmVolumeActionMap, $actionMenu, $template);	
		    noAvailableActions = false;		
		}
	} 
	else { //json.type=="DATADISK": "detach disk" is allowed, "create template" is disallowed.			
		buildActionLinkForSubgridItem("Detach Disk", vmVolumeActionMap, $actionMenu, $template);		
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

function routerAfterSubgridItemAction(json, id, $subgridItem) {        
    var jsonObj = json.queryasyncjobresultresponse.router[0];  
    vmRouterJSONToTemplate(jsonObj, $subgridItem);
}     
  
var vmRouterActionMap = {  
    "Stop Router": {
        api: "stopRouter",            
        isAsyncJob: true,
        asyncJobResponse: "stoprouterresponse",
        inProcessText: "Stopping Router....",
        afterActionSeccessFn: routerAfterSubgridItemAction
    },
    "Start Router": {
        api: "startRouter",            
        isAsyncJob: true,
        asyncJobResponse: "startrouterresponse",
        inProcessText: "Starting Router....",
        afterActionSeccessFn: routerAfterSubgridItemAction
    },
    "Reboot Router": {
        api: "rebootRouter",           
        isAsyncJob: true,
        asyncJobResponse: "rebootrouterresponse",
        inProcessText: "Rebooting Router....",
        afterActionSeccessFn: routerAfterSubgridItemAction
    }
}   
//***** Routers tab (end) ***************************************************************************************