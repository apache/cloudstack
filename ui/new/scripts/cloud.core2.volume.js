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
 
    $("#right_panel_content #tab_content_details #action_message_box #close_button").bind("click", function(event){    
        $(this).parent().hide();
        return false;
    });
}

function volumeAfterDetailsTabAction(jsonObj) {
    $("#midmenuItem_"+jsonObj.id).data("jsonObj", jsonObj);   
    volumeJsonToDetailsTab(jsonObj);   
}

function volumeToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", ("midmenuItem_"+jsonObj.id));  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_storage_volume.png");		
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.type.substring(0,25));  
}

function volumeToRigntPanel($midmenuItem) {       
    var json = $midmenuItem.data("jsonObj");     
    volumeJsonToDetailsTab(json);   
}
 
function volumeJsonToDetailsTab(jsonObj){
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
    
    var $actionLink = $detailsTab.find("#volume_action_link");
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
    if(jsonObj.type=="ROOT") { //"create template" is allowed(when stopped), "detach disk" is disallowed.
		if (jsonObj.vmstate == "Stopped") 
		    buildActionLinkForDetailsTab("Create Template", volumeActionMap, $actionMenu, volumeListAPIMap);	
	} 
	else { //jsonObj.type=="DATADISK": "detach disk" is allowed, "create template" is disallowed.			
		buildActionLinkForDetailsTab("Detach Disk", volumeActionMap, $actionMenu, volumeListAPIMap);				
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
        afterActionSeccessFn: volumeAfterDetailsTabAction
    },
    "Create Template": {
        isAsyncJob: true,
        asyncJobResponse: "createtemplateresponse",            
        dialogBeforeActionFn : doCreateTemplateFromVolume,
        inProcessText: "Creating template....",
        afterActionSeccessFn: function(){}   
    }  
}   

function doCreateTemplateFromVolume($actionLink, listAPIMap, $detailsTab) {       
    var jsonObj = $detailsTab.data("jsonObj");
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
			
			var id = $detailsTab.data("jsonObj").id;			
			var apiCommand = "command=createTemplate&volumeId="+id+"&name="+encodeURIComponent(name)+"&displayText="+encodeURIComponent(desc)+"&osTypeId="+osType+"&isPublic="+isPublic+"&passwordEnabled="+password;
	    	doActionToDetailsTab(id, $actionLink, apiCommand, listAPIMap);					
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		} 
	}).dialog("open");
}   