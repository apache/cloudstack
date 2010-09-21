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
       
    //actions ***    
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    
    buildActionLinkForDetailsTab("Take Snapshot", volumeActionMap, $actionMenu, volumeListAPIMap);	//show take snapshot
    //buildActionLinkForDetailsTab("Recurring Snapshot", volumeActionMap, $actionMenu, volumeListAPIMap);	//show Recurring Snapshot
    
    if(jsonObj.state != "Creating" && jsonObj.state != "Corrupted" && jsonObj.name != "attaching") {
        if(jsonObj.type=="ROOT") {
            if (jsonObj.vmstate == "Stopped")  
                buildActionLinkForDetailsTab("Create Template", volumeActionMap, $actionMenu, volumeListAPIMap);	//show create template
        } 
        else { 
	        if (jsonObj.virtualmachineid != null) {
		        if (jsonObj.storagetype == "shared" && (jsonObj.vmstate == "Running" || jsonObj.vmstate == "Stopped")) {
			        buildActionLinkForDetailsTab("Detach Disk", volumeActionMap, $actionMenu, volumeListAPIMap); //show detach disk
		        }
	        } else {
		        // Disk not attached
		        if (jsonObj.storagetype == "shared") {
			        buildActionLinkForDetailsTab("Detach Disk", volumeActionMap, $actionMenu, volumeListAPIMap);   //show attach disk
    			    			  		    
			        if(jsonObj.vmname == null || jsonObj.vmname == "none")
			            buildActionLinkForDetailsTab("Delete Volume", volumeActionMap, $actionMenu, volumeListAPIMap); //show delete volume
		        }
	        }
        }
    }
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
    },
    "Delete Volume": {
        api: "deleteVolume",            
        isAsyncJob: false,        
        inProcessText: "Deleting volume....",
        afterActionSeccessFn: function(id) {     
            var $midmenuItem1 = $("#midmenuItem_"+id); 
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
        afterActionSeccessFn: function(){}   
    }  
}   

var volumeListAPIMap = {
    listAPI: "listVolumes",
    listAPIResponse: "listvolumesresponse",
    listAPIResponseObj: "volume"
}; 

function doCreateTemplateFromVolume($actionLink, listAPIMap, $detailsTab) {       
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
			var apiCommand = "command=createTemplate&volumeId="+id+"&name="+encodeURIComponent(name)+"&displayText="+encodeURIComponent(desc)+"&osTypeId="+osType+"&isPublic="+isPublic+"&passwordEnabled="+password;
	    	doActionToDetailsTab(id, $actionLink, apiCommand, listAPIMap);					
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		} 
	}).dialog("open");
}   

function doTakeSnapshot($actionLink, listAPIMap, $detailsTab) {   
    $("#dialog_create_snapshot")					
    .dialog('option', 'buttons', { 					    
	    "Confirm": function() { 	
	        $(this).dialog("close");	
	    	
            var id = $detailsTab.data("jsonObj").id;	
			var apiCommand = "command=createSnapshot&volumeid="+id;
	    	doActionToDetailsTab(id, $actionLink, apiCommand, listAPIMap);	
	    },
	    "Cancel": function() { 					        
		    $(this).dialog("close"); 
	    } 
    }).dialog("open");	  
}		
