var g_zoneIds = []; 
var g_zoneNames = [];	

function afterLoadIsoJSP() {
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    
    
    //populate dropdown ***
    $.ajax({
	    data: createURL("command=listOsTypes&response=json"+maxPageSize),
		dataType: "json",
		success: function(json) {
			types = json.listostypesresponse.ostype;
			if (types != null && types.length > 0) {
				var osTypeDropdown = $detailsTab.find("#ostypename_edit").empty();
				for (var i = 0; i < types.length; i++) {
					var html = "<option value='" + types[i].id + "'>" + types[i].description + "</option>";
					osTypeDropdown.append(html);					
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
	            var serviceOfferingField = $("#dialog_create_vm_from_iso #service_offering").empty();
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
	            var diskOfferingField = $("#dialog_create_vm_from_iso #disk_offering").empty();
	            for(var i = 0; i < items.length; i++)		        
	                diskOfferingField.append("<option value='" + items[i].id + "'>" + sanitizeXSS(items[i].name) + "</option>");
	        }		  
	        
	    }
	});		
    
    //initialize dialog box ***
	activateDialog($("#dialog_copy_iso").dialog({ 
		width:300,
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));
	
	activateDialog($("#dialog_create_vm_from_iso").dialog({ 
		width:300,
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));
    
    //populate zone dropdown excluding source zone ***				
	var addTemplateZoneField = $("#dialog_add_template #add_template_zone");
    
	// Add default zone
	if (isAdmin()) {
		addTemplateZoneField.append("<option value='-1'>All Zones</option>"); 		
	}
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
}

function isoToMidmenu(jsonObj, $midmenuItem1) {    
    $midmenuItem1.attr("id", ("midmenuItem_"+jsonObj.id));  
    $midmenuItem1.data("jsonObj", jsonObj); 
        
    var $iconContainer = $midmenuItem1.find("#icon_container").show();
    setIconByOsType(jsonObj.ostypename, $iconContainer.find("#icon"));
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(fromdb(jsonObj.zonename).substring(0,25));  
}

function isoToRigntPanel($midmenuItem) {       
    var jsonObj = $midmenuItem.data("jsonObj");
    isoJsonToDetailsTab(jsonObj);   
}

function isoJsonToDetailsTab(jsonObj) {   
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);      
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#displaytext").text(fromdb(jsonObj.displaytext));
    $detailsTab.find("#account").text(fromdb(jsonObj.account));
    
    if(jsonObj.size != null)
	    $detailsTab.find("#size").text(convertBytes(parseInt(jsonObj.size)));       
    
    var status = "Ready";
	if (jsonObj.isready == "false")
		status = jsonObj.isostatus;	
	$detailsTab.find("#status").text(status); 
       
    setBooleanField(jsonObj.bootable, $detailsTab.find("#bootable"));	     
    setDateField(jsonObj.created, $detailsTab.find("#created"));	  
    
    
    //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    
    // "Edit", "Copy", "Create VM" 
	if ((isUser() && jsonObj.ispublic == "true" && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account)) || jsonObj.isready == "false") {
		//template.find("#iso_edit_container, #iso_copy_container").hide();
		$("#edit_button").hide();
    }
    else {
        //template.find("#iso_edit_container, #iso_copy_container").show();
        $("#edit_button").show();
        buildActionLinkForDetailsTab("Copy ISO", isoActionMap, $actionMenu, isoListAPIMap);		
    }
		
	// "Create VM" 
	if (((isUser() && jsonObj.ispublic == "true" && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account)) || jsonObj.isready == "false") || (jsonObj.bootable == "false")) {
		//template.find("#iso_create_vm_container").hide();
    }
    else {
        //template.find("#iso_create_vm_container").show();
        buildActionLinkForDetailsTab("Create VM", isoActionMap, $actionMenu, isoListAPIMap);	
    }
    
	// "Delete" 
	if (((isUser() && jsonObj.ispublic == "true" && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account))) || (jsonObj.isready == "false" && jsonObj.isostatus != null && jsonObj.isostatus.indexOf("% Downloaded") != -1)) {
		//template.find("#iso_delete_container").hide();	
	}
	else {
	    //template.find("#iso_delete_container").show();	
	     buildActionLinkForDetailsTab("Delete ISO", isoActionMap, $actionMenu, isoListAPIMap);	
	}    
}

function isoClearRightPanel() {

}

var isoActionMap = {  
    "Delete ISO": {
        api: "deleteIso",            
        isAsyncJob: true,
        asyncJobResponse: "deleteisosresponse",
        inProcessText: "Deleting ISO....",
        afterActionSeccessFn: function(jsonObj) {           
            var $midmenuItem1 = $("#midmenuItem_"+jsonObj.id);
            $midmenuItem1.remove();
            clearRightPanel();
            isoClearRightPanel();
        }
    },
    "Copy ISO": {
        isAsyncJob: true,
        asyncJobResponse: "copyisoresponse",            
        dialogBeforeActionFn : doCopyIso,
        inProcessText: "Copying ISO....",
        afterActionSeccessFn: function(){}   
    }  
    ,
    "Create VM": {
        isAsyncJob: true,
        asyncJobResponse: "deployvirtualmachineresponse",            
        dialogBeforeActionFn : doCreateVMFromIso,
        inProcessText: "Creating VM....",
        afterActionSeccessFn: function(){}   
    }  
}   

var isoListAPIMap = {
    listAPI: "listisos&isofilter=self",
    listAPIResponse: "listisosresponse",
    listAPIResponseObj: "iso"
}; 

function populateZoneFieldExcludeSourceZone(zoneField, excludeZoneId) {	  
    zoneField.empty();  
    if (g_zoneIds != null && g_zoneIds.length > 0) {
        for (var i = 0; i < g_zoneIds.length; i++) {
            if(g_zoneIds[i]	!= excludeZoneId)			            
	            zoneField.append("<option value='" + g_zoneIds[i] + "'>" + sanitizeXSS(g_zoneNames[i]) + "</option>"); 			        			       
        }
    }			    
}

function doCopyIso($actionLink, listAPIMap, $detailsTab) {   
	var jsonObj = $detailsTab.data("jsonObj");
	var id = jsonObj.id;
	var name = jsonObj.name;			
	var sourceZoneId = jsonObj.zoneid;
				
	populateZoneFieldExcludeSourceZone($("#dialog_copy_iso #copy_iso_zone"), sourceZoneId);
	
	$("#dialog_copy_iso #copy_iso_name_text").text(name);  //ISO name
	
	var sourceZoneName = jsonObj.zonename;
	$("#dialog_copy_iso #copy_iso_source_zone_text").text(sourceZoneName); // source zone
		
	$("#dialog_copy_iso")
	.dialog('option', 'buttons', {				    
	    "OK": function() {				       
	        var thisDialog = $(this);
	        thisDialog.dialog("close");
	        	        
	        var isValid = true;	 
            isValid &= validateDropDownBox("Zone", thisDialog.find("#copy_iso_zone"), thisDialog.find("#copy_iso_zone_errormsg"), false);  //reset error text		         
	        if (!isValid) return;     
	        				        
	        var destZoneId = thisDialog.find("#copy_iso_zone").val();				        				        
	        thisDialog.dialog("close");		        
	          				        
	        
            var apiCommand = "command=copyIso&id="+id+"&sourcezoneid="+sourceZoneId+"&destzoneid="+destZoneId;
            doActionToDetailsTab(id, $actionLink, apiCommand, listAPIMap);	 
	    }, 
	    "Cancel": function() {				        
		    $(this).dialog("close");
	    }				
	}).dialog("open");	
}	

function doCreateVMFromIso($actionLink, listAPIMap, $detailsTab) { 
    var jsonObj = $detailsTab.data("jsonObj");	
	var id = jsonObj.id;		
	var name = jsonObj.name;				
	var zoneId = jsonObj.zoneid;		
					
	var createVmDialog = $("#dialog_create_vm_from_iso");				
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
	                
	        var name = trim(thisDialog.find("#name").val());		
	        var group = trim(thisDialog.find("#group").val());		
	        var serviceOfferingId = thisDialog.find("#service_offering").val();				        
	        var diskOfferingId = thisDialog.find("#disk_offering").val();	        		        
	                         
		    var apiCommand = "command=deployVirtualMachine&zoneId="+zoneId+"&serviceOfferingId="+serviceOfferingId+"&diskOfferingId="+diskOfferingId+"&templateId="+id+"&group="+encodeURIComponent(group)+"&displayname="+encodeURIComponent(name);
    	    doActionToDetailsTab(id, $actionLink, apiCommand, listAPIMap);		
	    }, 
	    "Cancel": function() {
	        $(this).dialog("close");
	    }
	}).dialog("open");			
}	