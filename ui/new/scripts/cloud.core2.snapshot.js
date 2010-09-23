function afterLoadSnapshotJSP() {
    activateDialog($("#dialog_add_volume_from_snapshot").dialog({ 
	    autoOpen: false,
	    modal: true,
	    zIndex: 2000
    }));	
}

function snapshotToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", ("midmenuItem_"+jsonObj.id)); 
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_storage_snapshots.png");		
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(fromdb(jsonObj.volumename).substring(0,25));    
}

function snapshotToRigntPanel($midmenuItem) {
    var jsonObj = $midmenuItem.data("jsonObj");
    snapshotJsonToDetailsTab(jsonObj);   
}

function snapshotJsonToDetailsTab(jsonObj) {   
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);         
    
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.find("#id").text(jsonObj.id);
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#volume_name").text(fromdb(jsonObj.volumename));
    $detailsTab.find("#interval_type").text(jsonObj.intervaltype);
    $detailsTab.find("#account").text(fromdb(jsonObj.account));
    $detailsTab.find("#domain").text(fromdb(jsonObj.domain));      
    setDateField(jsonObj.created, $detailsTab.find("#created"));	
    
    //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    buildActionLinkForDetailsTab("Create Volume", snapshotActionMap, $actionMenu, snapshotListAPIMap);			
}

var snapshotActionMap = {  
    "Create Volume": {              
        isAsyncJob: true,
        asyncJobResponse: "createvolumeresponse",
        dialogBeforeActionFn : doCreateVolumeFromSnapshotInSnapshotPage,
        inProcessText: "Creating Volume....",
        afterActionSeccessFn: function(jsonObj) {           

        }
    }    
}   

var snapshotListAPIMap = {
    listAPI: "listSnapshots",
    listAPIResponse: "listsnapshotsresponse",
    listAPIResponseObj: "snapshot"
}; 

function doCreateVolumeFromSnapshotInSnapshotPage($actionLink, listAPIMap, $detailsTab) { 
    var jsonObj = $detailsTab.data("jsonObj");
       
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
         var apiCommand = "command=createVolume&snapshotid="+id+"&name="+name;
    	 doActionToDetailsTab(id, $actionLink, apiCommand, listAPIMap);		
     },
     "Cancel": function() {	                         
         $(this).dialog("close");
     }
    }).dialog("open");     
}