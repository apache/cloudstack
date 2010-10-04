var g_zoneIds = []; 
var g_zoneNames = [];	

function afterLoadTemplateJSP() {      
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    
    //add button ***
    var formatSelect = $("#dialog_add_template #add_template_format").empty();
	if (getHypervisorType() == "kvm") 
		formatSelect.append("<option value='QCOW2'>QCOW2</option>");
	else if (getHypervisorType() == "xenserver") 
		formatSelect.append("<option value='VHD'>VHD</option>");	    
		
	if(isAdmin())
	    $("#dialog_add_template #add_template_featured_container, #dialog_edit_template #edit_template_featured_container").show();
	else
	    $("#dialog_add_template #add_template_featured_container, #dialog_edit_template #edit_template_featured_container").hide();		
	
	//add button ***
    $("#midmenu_add_link").show();     
    $("#midmenu_add_link").unbind("click").bind("click", function(event) {        
        $("#dialog_add_template")
		.dialog('option', 'buttons', { 				
			"Create": function() { 		
			    var thisDialog = $(this);
													
				// validate values
				var isValid = true;					
				isValid &= validateString("Name", thisDialog.find("#add_template_name"), thisDialog.find("#add_template_name_errormsg"));
				isValid &= validateString("Display Text", thisDialog.find("#add_template_display_text"), thisDialog.find("#add_template_display_text_errormsg"));
				isValid &= validateString("URL", thisDialog.find("#add_template_url"), thisDialog.find("#add_template_url_errormsg"));			
				if (!isValid) return;		
				
				thisDialog.dialog("close");
										
				var name = trim(thisDialog.find("#add_template_name").val());
				var desc = trim(thisDialog.find("#add_template_display_text").val());
				var url = trim(thisDialog.find("#add_template_url").val());						
				var zoneId = thisDialog.find("#add_template_zone").val();												
				var format = thisDialog.find("#add_template_format").val();					
				var password = thisDialog.find("#add_template_password").val();		
				var isPublic = thisDialog.find("#add_template_public").val();	                    	
				var osType = thisDialog.find("#add_template_os_type").val();
				
				var moreCriteria = [];				
				if(thisDialog.find("#add_template_featured_container").css("display")!="none") {				
				    var isFeatured = thisDialog.find("#add_template_featured").val();						    	
                    moreCriteria.push("&isfeatured="+isFeatured);
                }					
				
				var $midmenuItem1 = beforeAddingMidMenuItem() ;
												
				$.ajax({
				    data: createURL("command=registerTemplate&name="+todb(name)+"&displayText="+todb(desc)+"&url="+encodeURIComponent(url)+"&zoneid="+zoneId+"&ispublic="+isPublic+moreCriteria.join("")+"&format="+format+"&passwordEnabled="+password+"&osTypeId="+osType+"&response=json"),
					dataType: "json",
					success: function(json) {	
						var items = json.registertemplateresponse.template;				       
				        templateToMidmenu(items[0], $midmenuItem1);
						bindClickToMidMenu($midmenuItem1, templateToRigntPanel, templateGetMidmenuId);  
						afterAddingMidMenuItem($midmenuItem1, true);
						                        
                        if(items.length > 1) {                               
                            for(var i=1; i<items.length; i++) {   
                                var $midmenuItem2 = $("#midmenu_item").clone();
                                templateToMidmenu(items[i], $midmenuItem2);
                                bindClickToMidMenu($midmenuItem2, templateToRigntPanel, templateGetMidmenuId); 
                                $("#midmenu_container").append($midmenuItem2.show());
                            }                                    
                        }  	                  			                  				
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
    
    //edit button ***
    var $readonlyFields  = $detailsTab.find("#name, #displaytext, #passwordenabled, #ispublic, #isfeatured, #ostypename");
    var $editFields = $detailsTab.find("#name_edit, #displaytext_edit, #passwordenabled_edit, #ispublic_edit, #isfeatured_edit, #ostypename_edit");     
    initializeEditFunction($readonlyFields, $editFields, doUpdateTemplate);      
    
    //populate dropdown ***   			
	var addTemplateZoneField = $("#dialog_add_template #add_template_zone");    	
	if (isAdmin())  
		addTemplateZoneField.append("<option value='-1'>All Zones</option>"); 	
    $.ajax({
        data: createURL("command=listZones&available=true"+maxPageSize),
	    dataType: "json",
	    success: function(json) {		        
		    var zones = json.listzonesresponse.zone;	 			     			    	
		    if (zones != null && zones.length > 0) {
		        for (var i = 0; i < zones.length; i++) {
			        addTemplateZoneField.append("<option value='" + zones[i].id + "'>" + sanitizeXSS(zones[i].name) + "</option>"); 			        
			        g_zoneIds.push(zones[i].id);
			        g_zoneNames.push(zones[i].name);			       
		        }
		    }				    			
	    }
	});	
    
    $.ajax({
	    data: createURL("command=listOsTypes&response=json"+maxPageSize),
		dataType: "json",
		success: function(json) {
			types = json.listostypesresponse.ostype;
			if (types != null && types.length > 0) {		
			    var osTypeDropdownAdd = $("#dialog_add_template #add_template_os_type");    
				var osTypeDropdownEdit = $detailsTab.find("#ostypename_edit").empty();
				if(types != null && types.length > 0) {
				    for(var i = 0; i < types.length; i++) {
					    var html = "<option value='" + types[i].id + "'>" + types[i].description + "</option>";
					    osTypeDropdownAdd.append(html);	
					    osTypeDropdownEdit.append(html);					
				    }
				}
			}	
		}
	});
	
	$.ajax({
	    data: createURL("command=listServiceOfferings&response=json"+maxPageSize),
	    dataType: "json",
	    success: function(json) {
	        var items = json.listserviceofferingsresponse.serviceoffering;
	        if(items != null && items.length > 0 ) {
	            var serviceOfferingField = $("#dialog_create_vm_from_template #service_offering").empty();
	            for(var i = 0; i < items.length; i++)		        
	                serviceOfferingField.append("<option value='" + items[i].id + "'>" + sanitizeXSS(items[i].name) + "</option>");
	        }		        
	    }
	});		
	
	$.ajax({
	    data: createURL("command=listDiskOfferings&response=json"+maxPageSize),
	    dataType: "json",
	    success: function(json) {
	        var items = json.listdiskofferingsresponse.diskoffering;
	        if(items != null && items.length > 0 ) {
	            var diskOfferingField = $("#dialog_create_vm_from_template #disk_offering").empty();
	            diskOfferingField.append("<option value=''>No disk offering</option>");
	            for(var i = 0; i < items.length; i++)		        
	                diskOfferingField.append("<option value='" + items[i].id + "'>" + sanitizeXSS(items[i].name) + "</option>");
	        }		  
	        
	    }
	});		
	
	//initialize dialog box ***
	initDialog("dialog_confirmation_delete_template_all_zones");
    initDialog("dialog_confirmation_delete_template");
    
	activateDialog($("#dialog_add_template").dialog({ 
		width:450,
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));
	
	activateDialog($("#dialog_copy_template").dialog({ 
		width:300,
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));
	
	activateDialog($("#dialog_create_vm_from_template").dialog({ 
		width:300,
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));
}

function templateGetMidmenuId(jsonObj) {
    return "midmenuItem_" + jsonObj.id + "_" + fromdb(jsonObj.zonename).replace(/\s/g, ""); //remove all spaces in zonename
}

function templateToMidmenu(jsonObj, $midmenuItem1) {    
    var id = templateGetMidmenuId(jsonObj);
    $midmenuItem1.attr("id", id);  
    $midmenuItem1.data("jsonObj", jsonObj); 
       
    var $iconContainer = $midmenuItem1.find("#icon_container").show();
    setIconByOsType(jsonObj.ostypename, $iconContainer.find("#icon"));
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(fromdb(jsonObj.zonename).substring(0,25));   
}

function templateAfterDetailsTabAction(jsonObj) {
    var $midmenuItem1 = $("#"+templateGetMidmenuId(jsonObj));
    $midmenuItem1.data("jsonObj", jsonObj);   
    templateToMidmenu(jsonObj, $midmenuItem1);
    templateJsonToDetailsTab(jsonObj);       
}

function templateToRigntPanel($midmenuItem) {       
    var jsonObj = $midmenuItem.data("jsonObj");
    templateJsonToDetailsTab(jsonObj);   
}

function templateJsonToDetailsTab(jsonObj) {   
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));
    
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#name_edit").val(fromdb(jsonObj.name));
    
    $detailsTab.find("#displaytext").text(fromdb(jsonObj.displaytext));
    $detailsTab.find("#displaytext_edit").val(fromdb(jsonObj.displaytext));
    
    var status = "Ready";
	if (jsonObj.isready == "false") 
		status = jsonObj.templatestatus;	
	$detailsTab.find("#status").text(status);    
    
    setBooleanField(jsonObj.passwordenabled, $detailsTab.find("#passwordenabled"));	
    $detailsTab.find("#passwordenabled_edit").val(jsonObj.passwordenabled);
    
    setBooleanField(jsonObj.ispublic, $detailsTab.find("#ispublic"));	
    $detailsTab.find("#ispublic_edit").val(jsonObj.ispublic);
    
    setBooleanField(jsonObj.isfeatured, $detailsTab.find("#isfeatured"));
    $detailsTab.find("#isfeatured_edit").val(jsonObj.isfeatured);
    
    setBooleanField(jsonObj.crossZones, $detailsTab.find("#crossZones"));
    
    $detailsTab.find("#ostypename").text(fromdb(jsonObj.ostypename));
    $detailsTab.find("#ostypename_edit").val(jsonObj.ostypeid);    
    
    $detailsTab.find("#account").text(fromdb(jsonObj.account));
    
    if(jsonObj.size != null)
	    $detailsTab.find("#size").text(convertBytes(parseInt(jsonObj.size)));        
    
    setDateField(jsonObj.created, $detailsTab.find("#created"));	
    
    //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    
    // action Edit, Copy, Create VM 			
	if ((isUser() && jsonObj.ispublic == "true" && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account)) || jsonObj.id==DomRTemplateId || jsonObj.isready == "false") {
		//template.find("#template_edit_container, #template_copy_container, #template_create_vm_container").hide(); 
		$("#edit_button").hide();		
    }
    else {
        $("#edit_button").show();
        buildActionLinkForDetailsTab("Copy Template", templateActionMap, $actionMenu, templateListAPIMap);			
        buildActionLinkForDetailsTab("Create VM", templateActionMap, $actionMenu, templateListAPIMap);			
    }
	
	// action Delete 			
	if (((isUser() && jsonObj.ispublic == "true" && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account)) || jsonObj.id==DomRTemplateId) || (jsonObj.isready == "false" && jsonObj.templatestatus != null && jsonObj.templatestatus.indexOf("% Downloaded") != -1)) {
		//template.find("#template_delete_container").hide();
    }
    else {
        buildActionLinkForDetailsTab("Delete Template", templateActionMap, $actionMenu, templateListAPIMap);	
    }
}

//setIconByOsType() is shared by template page and ISO page
function setIconByOsType(osType, $field) {
	if (osType == null || osType.length == 0)
		return; 	
	if (osType.match("^CentOS") != null)
		$field.attr("src", "images/midmenuicon_template_centos.png");
	else if (osType.match("^Windows") != null) 
		$field.attr("src", "images/midmenuicon_template_windows.png");
	else 
		$field.attr("src", "images/midmenuicon_template_linux.png");
}

function templateClearRightPanel() {       
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", null);
    $detailsTab.find("#id").text("");
    $detailsTab.find("#zonename").text("");
    
    $detailsTab.find("#name").text("");
    $detailsTab.find("#name_edit").val("");
    
    $detailsTab.find("#displaytext").text("");
    $detailsTab.find("#displaytext_edit").val("");
        
	$detailsTab.find("#status").text("");    
    
    $detailsTab.find("#passwordenabled").text("");
    $detailsTab.find("#passwordenabled_edit").val(null);
    
    $detailsTab.find("#ispublic").text("");
    $detailsTab.find("#ispublic_edit").val(null);
    
    $detailsTab.find("#isfeatured").text("");
    $detailsTab.find("#isfeatured_edit").val(null);
    
    $detailsTab.find("#crossZones").text("");    
    
    $detailsTab.find("#ostypename").text("");
    $detailsTab.find("#ostypename_edit").val(null);    
    
    $detailsTab.find("#account").text("");  
	$detailsTab.find("#size").text("");  
    $detailsTab.find("#created").text("");      
}

var templateActionMap = {  
    "Delete Template": {              
        isAsyncJob: true,
        asyncJobResponse: "deletetemplateresponse",
        dialogBeforeActionFn : doDeleteTemplate,
        inProcessText: "Deleting Template....",
        afterActionSeccessFn: function(jsonObj) {           
            var $midmenuItem1 = $("#"+templateGetMidmenuId(jsonObj)); 
            $midmenuItem1.remove();
            clearRightPanel();
            templateClearRightPanel();
        }
    },
    "Copy Template": {
        isAsyncJob: true,
        asyncJobResponse: "createtemplateresponse",            
        dialogBeforeActionFn : doCopyTemplate,
        inProcessText: "Copying Template....",
        afterActionSeccessFn: function(){}   
    }  
    ,
    "Create VM": {
        isAsyncJob: true,
        asyncJobResponse: "deployvirtualmachineresponse",            
        dialogBeforeActionFn : doCreateVMFromTemplate,
        inProcessText: "Creating VM....",
        afterActionSeccessFn: function(){}   
    }  
}   

var templateListAPIMap = {
    listAPI: "listTemplates&templatefilter=self",
    listAPIResponse: "listtemplatesresponse",
    listAPIResponseObj: "template"
}; 

var DomRTemplateId = 1;


function doUpdateTemplate() {    
    var $detailsTab = $("#right_panel_content #tab_content_details");  
            
    // validate values
    var isValid = true;					
    isValid &= validateString("Name", $detailsTab.find("#name_edit"), $detailsTab.find("#name_edit_errormsg"));
    isValid &= validateString("Display Text", $detailsTab.find("#displaytext_edit"), $detailsTab.find("#displaytext_edit_errormsg"));			
    if (!isValid) 
        return;					
	
	var jsonObj = $detailsTab.data("jsonObj"); 
	var id = jsonObj.id;
	
	//updateTemplate	
	var array1 = [];
	var oldName = jsonObj.name
	var newName = trim($detailsTab.find("#name_edit").val());
	if(newName != oldName)
	    array1.push("&name="+todb(newName));
	
	var oldDesc = jsonObj.displaytext;
	var newDesc = trim($detailsTab.find("#displaytext_edit").val());	
	if(newDesc != oldDesc)
	    array1.push("&displaytext="+todb(newDesc));
	    
	var oldPasswordEnabled = jsonObj.passwordenabled;	
	var newPasswordEnabled = $detailsTab.find("#passwordenabled_edit").val();     
	if(newPasswordEnabled != oldPasswordEnabled)
	    array1.push("&passwordenabled="+newPasswordEnabled);	
		
	var oldOsTypeId = jsonObj.ostypeid;
	var newOsTypeId = $detailsTab.find("#ostypename_edit").val();
	if(newOsTypeId != oldOsTypeId)
	    array1.push("&ostypeid="+newOsTypeId);
				
	if(array1.length > 0) {	
	    $.ajax({
		    data: createURL("command=updateTemplate&id="+id+array1.join("")),
		    dataType: "json",
		    async: false,
		    success: function(json) {		        
		        //embedded object (json.updatetemplateresponse) is returned, but the embedded object doesn't include all properties.(API needs to be fixed)		
		    }
	    });
	}
		
	//updateTemplatePermissions	
	var array2 = [];		
	var oldIsPublic = jsonObj.ispublic;
	var newIsPublic = $detailsTab.find("#ispublic_edit").val();        
	if(newIsPublic != oldIsPublic)
	    array2.push("&ispublic="+newIsPublic);
	    
	var oldIsFeatured = jsonObj.isfeatured;
	var newIsFeatured = $detailsTab.find("#isfeatured_edit").val();           
    if(newIsFeatured != oldIsFeatured)
        array2.push("&isfeatured="+newIsFeatured);											
								
	if(array2.length > 0) {	
	    $.ajax({
		    data: createURL("command=updateTemplatePermissions&id="+id+array2.join("")),
		    dataType: "json",
		    async: false,
		    success: function(json) {			        						       					    
		        //no embedded object is returned. (API needs to be fixed)		
    		}
	    });
	}	
	
	//since embedded object is not returned (updateTemplatePermissions API) or embedded object doesn't include all properties (updateTemplate API), call listTemplates API again.	
	$.ajax({
        data:createURL("command=listTemplates&templatefilter=self&id="+id),
        dataType: "json",
        success: function(json) {            
            templateAfterDetailsTabAction(json.listtemplatesresponse.template[0]);
        }
    });   
}

function doDeleteTemplate($actionLink, listAPIMap, $detailsTab) {   
    var $detailsTab = $("#right_panel_content #tab_content_details"); 
    var jsonObj = $detailsTab.data("jsonObj");
	var id = jsonObj.id;
	var name = jsonObj.name;			
	var zoneId = jsonObj.zoneid;

    var moreCriteria = [];						
	if (zoneId != null) 
		moreCriteria.push("&zoneid="+zoneId);	
	
	var $dialog1;
	if(jsonObj.crossZones == "true")
	    $dialog1 = $("#dialog_confirmation_delete_template_all_zones");
	else
	    $dialog1 = $("#dialog_confirmation_delete_template");	
	
	$dialog1		
	.dialog('option', 'buttons', { 					
		"Confirm": function() { 			
			$(this).dialog("close");			
			var apiCommand = "command=deleteTemplate&id="+id+moreCriteria.join("");
            doActionToDetailsTab(id, $actionLink, apiCommand, listAPIMap);	
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		}
	}).dialog("open");
}

function populateZoneFieldExcludeSourceZone(zoneField, excludeZoneId) {	  
    zoneField.empty();  
    if (g_zoneIds != null && g_zoneIds.length > 0) {
        for (var i = 0; i < g_zoneIds.length; i++) {
            if(g_zoneIds[i]	!= excludeZoneId)			            
	            zoneField.append("<option value='" + g_zoneIds[i] + "'>" + sanitizeXSS(g_zoneNames[i]) + "</option>"); 			        			       
        }
    }			    
}

function doCopyTemplate($actionLink, listAPIMap, $detailsTab) { 
	var jsonObj = $detailsTab.data("jsonObj");
	var id = jsonObj.id;
	var name = jsonObj.name;						
	var sourceZoneId = jsonObj.zoneid;				
		
	populateZoneFieldExcludeSourceZone($("#dialog_copy_template #copy_template_zone"), sourceZoneId);
	
	$("#dialog_copy_template #copy_template_name_text").text(name);
		
	var sourceZoneName = jsonObj.zonename;
	$("#dialog_copy_template #copy_template_source_zone_text").text(sourceZoneName);
		
	$("#dialog_copy_template")
	.dialog('option', 'buttons', {				    
	    "OK": function() {				       
	        var thisDialog = $(this);
	        thisDialog.dialog("close");		
	        				        
	        var isValid = true;	 
            isValid &= validateDropDownBox("Zone", thisDialog.find("#copy_template_zone"), thisDialog.find("#copy_template_zone_errormsg"), false);  //reset error text		         
	        if (!isValid) return;     
	        				        
	        var destZoneId = thisDialog.find("#copy_template_zone").val();	
	        
            var id = $detailsTab.data("jsonObj").id;			
	        var apiCommand = "command=copyTemplate&id="+id+"&sourcezoneid="+sourceZoneId+"&destzoneid="+destZoneId;
	        doActionToDetailsTab(id, $actionLink, apiCommand, listAPIMap);	
	    }, 
	    "Cancel": function() {				        
		    $(this).dialog("close");
	    }				
	}).dialog("open");			
}

function doCreateVMFromTemplate($actionLink, listAPIMap, $detailsTab) { 
    var jsonObj = $detailsTab.data("jsonObj");
	var id = jsonObj.id;		
	var name = jsonObj.name;				
	var zoneId = jsonObj.zoneid;		
					
	var createVmDialog = $("#dialog_create_vm_from_template");				
	createVmDialog.find("#p_name").text(name);
		
	createVmDialog
	.dialog('option', 'buttons', {			    
	    "Create": function() {
	        var thisDialog = $(this);	
	        thisDialog.dialog("close");
	        			        
	        // validate values
		    var isValid = true;		
		    isValid &= validateString("Name", thisDialog.find("#name"), thisDialog.find("#name_errormsg"), true);
		    isValid &= validateString("Group", thisDialog.find("#group"), thisDialog.find("#group_errormsg"), true);				
		    if (!isValid) return;	       
	        
	        var array1 = [];      
	        var name = trim(thisDialog.find("#name").val());	
	        array1.push("&displayname="+todb(name));
	        	
	        var group = trim(thisDialog.find("#group").val());	
	        array1.push("&group="+todb(group));
	        	
	        var serviceOfferingId = thisDialog.find("#service_offering").val();		
	        array1.push("&serviceOfferingId="+serviceOfferingId);
	        		               
	        var diskOfferingId = thisDialog.find("#disk_offering").val();
	        if(diskOfferingId != null && diskOfferingId.length > 0)
	            array1.push("&diskOfferingId="+diskOfferingId);	 		    	        
	        
		    var apiCommand = "command=deployVirtualMachine&zoneId="+zoneId+"&templateId="+id+array1.join("");
    	    doActionToDetailsTab(id, $actionLink, apiCommand, listAPIMap);		
	    }, 
	    "Cancel": function() {
	        $(this).dialog("close");
	    }
	}).dialog("open");			
}		