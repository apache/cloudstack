function afterLoadVolumeJSP() {
    activateDialog($("#dialog_create_template").dialog({
        width: 400,
        autoOpen: false,
        modal: true,
        zIndex: 2000
    }));
    
    
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

function volumeToMidmenu(jsonObj, $midmenuItem1, toRightPanelFn) {  
    $midmenuItem1.attr("id", ("midmenuItem_"+jsonObj.id));  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_storage_volume.png");		
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.type.substring(0,25));           
    $midmenuItem1.data("toRightPanelFn", toRightPanelFn);
}

function volumeToRigntPanel($midmenuItem) {       
    var json = $midmenuItem.data("jsonObj");
        
    var $rightPanelContent = $("#right_panel_content");    
    $rightPanelContent.data("jsonObj", json);   
    $rightPanelContent.find("#id").text(json.id);
    $rightPanelContent.find("#name").text(fromdb(json.name));    
    $rightPanelContent.find("#zonename").text(fromdb(json.zonename));    
    $rightPanelContent.find("#device_id").text(json.deviceid);   
    $rightPanelContent.find("#state").text(json.state);    
    $rightPanelContent.find("#storage").text(fromdb(json.storage));
    $rightPanelContent.find("#account").text(fromdb(json.account)); 
    
    $rightPanelContent.find("#type").text(json.type + " (" + json.storagetype + " storage)");
    $rightPanelContent.find("#size").text((json.size == "0") ? "" : convertBytes(json.size));		
    
    if (json.virtualmachineid == null) 
		$rightPanelContent.find("#vm_name").text("detached");
	else 
		$rightPanelContent.find("#vm_name").text(getVmName(json.vmname, json.vmdisplayname) + " (" + json.vmstate + ")");
		
    setDateField(json.created, $rightPanelContent.find("#created"));	
    
    var $actionLink = $rightPanelContent.find("#volume_action_link");
	$actionLink.bind("mouseover", function(event) {	    
        $(this).find("#volume_action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {       
        $(this).find("#volume_action_menu").hide();    
        return false;
    });			
        
    var $actionMenu = $actionLink.find("#volume_action_menu");
    $actionMenu.find("#action_list").empty();
    if(json.type=="ROOT") { //"create template" is allowed(when stopped), "detach disk" is disallowed.
		if (json.vmstate == "Stopped") 
		    buildActionLinkForSingleObject("Create Template", volumeActionMap, $actionMenu, volumeListAPIMap, $rightPanelContent);	
	} 
	else { //json.type=="DATADISK": "detach disk" is allowed, "create template" is disallowed.			
		buildActionLinkForSingleObject("Detach Disk", volumeActionMap, $actionMenu, volumeListAPIMap, $rightPanelContent);				
	}	
}
       
var volumeListAPIMap = {
    listAPI: "listVolumes",
    listAPIResponse: "listvolumesresponse",
    listAPIResponseObj: "volume"
};           
  
var volumeActionMap = {  
    "Detach Disk": {
        api: "detachVolume",            
        isAsyncJob: true,
        asyncJobResponse: "detachvolumeresponse",
        inProcessText: "Detaching disk....",
        afterActionSeccessFn: function(){}
    },
    "Create Template": {
        isAsyncJob: true,
        asyncJobResponse: "createtemplateresponse",            
        dialogBeforeActionFn : doCreateTemplate,
        inProcessText: "Creating template....",
        afterActionSeccessFn: function(){}   
    }  
}   

function doCreateTemplate($actionLink, listAPIMap, $singleObject) {       
    var jsonObj = $singleObject.data("jsonObj");
    $("#dialog_create_template").find("#volume_name").text(jsonObj.name);
    
	$("#dialog_create_template")
	.dialog('option', 'buttons', { 						
		"Create": function() { 
		    //debugger;
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
			
			var id = $singleObject.data("jsonObj").id;			
			var apiCommand = "command=createTemplate&volumeId="+id+"&name="+encodeURIComponent(name)+"&displayText="+encodeURIComponent(desc)+"&osTypeId="+osType+"&isPublic="+isPublic+"&passwordEnabled="+password;
	    	doActionToSingleObject(id, $actionLink, apiCommand, listAPIMap, $singleObject);					
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		} 
	}).dialog("open");
}   