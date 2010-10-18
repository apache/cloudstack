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

// Version: @VERSION@

function afterLoadSnapshotJSP() {
    //initialize dialog
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
    
    //populate dropdown
    $.ajax({
        data: createURL("command=listOsTypes"),
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

function snapshotToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", getMidmenuId(jsonObj)); 
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
    var midmenuItemId = getMidmenuId(jsonObj);
    buildActionLinkForDetailsTab("Create Volume", snapshotActionMap, $actionMenu, midmenuItemId);		
    buildActionLinkForDetailsTab("Delete Snapshot", snapshotActionMap, $actionMenu, midmenuItemId);	
    buildActionLinkForDetailsTab("Create Template", snapshotActionMap, $actionMenu, midmenuItemId);					
}

function snapshotClearRightPanel() {
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.find("#id").text("");
    $detailsTab.find("#name").text("");
    $detailsTab.find("#volume_name").text("");
    $detailsTab.find("#interval_type").text("");
    $detailsTab.find("#account").text("");
    $detailsTab.find("#domain").text("");      
    $detailsTab.find("#created").text("");   
}

var snapshotActionMap = {  
    "Create Volume": {              
        isAsyncJob: true,
        asyncJobResponse: "createvolumeresponse",
        dialogBeforeActionFn : doCreateVolumeFromSnapshotInSnapshotPage,
        inProcessText: "Creating Volume....",
        afterActionSeccessFn: function(json, id, midmenuItemId) {}
    }   
    , 
    "Delete Snapshot": {              
        api: "deleteSnapshot",     
        isAsyncJob: true,
        asyncJobResponse: "deletesnapshotresponse",        
        inProcessText: "Deleting snapshot....",
        afterActionSeccessFn: function(json, id, midmenuItemId) {            
            $("#"+midmenuItemId).remove();
            clearRightPanel();
            snapshotClearRightPanel();
        }
    } 
    ,
    "Create Template": {              
        isAsyncJob: true,
        asyncJobResponse: "createtemplateresponse",
        dialogBeforeActionFn : doCreateTemplateFromSnapshotInSnapshotPage,
        inProcessText: "Creating Template....",
        afterActionSeccessFn: function(json, id, midmenuItemId) {}
    }
}   

function doCreateVolumeFromSnapshotInSnapshotPage($actionLink, $detailsTab, midmenuItemId){ 
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
    	 doActionToDetailsTab(id, $actionLink, apiCommand, midmenuItemId);		
     },
     "Cancel": function() {	                         
         $(this).dialog("close");
     }
    }).dialog("open");     
}

function doCreateTemplateFromSnapshotInSnapshotPage($actionLink, $detailsTab, midmenuItemId){ 
    var jsonObj = $detailsTab.data("jsonObj");
       
    $("#dialog_create_template_from_snapshot")
    .dialog("option", "buttons", {
     "Add": function() {	
         var thisDialog = $(this);	 	                                                                        
         var isValid = true;					
         isValid &= validateString("Name", thisDialog.find("#name"), thisDialog.find("#name_errormsg"), false);		
         isValid &= validateString("Display Text", thisDialog.find("#display_text"), thisDialog.find("#display_text_errormsg"), false);				         		          		
         if (!isValid) return;                  	                                             
         
         thisDialog.dialog("close");	
         
         var name = thisDialog.find("#name").val();	 
         var displayText = thisDialog.find("#display_text").val();	 
         var osTypeId = thisDialog.find("#os_type").val(); 	  
         var password = thisDialog.find("#password").val();	                                         
       
         var id = jsonObj.id;
         var apiCommand = "command=createTemplate&snapshotid="+id+"&name="+name+"&displaytext="+displayText+"&ostypeid="+osTypeId+"&passwordEnabled="+password;
    	 doActionToDetailsTab(id, $actionLink, apiCommand, midmenuItemId);		
     },
     "Cancel": function() {	                         
         $(this).dialog("close");
     }	                     
    }).dialog("open");	     
}