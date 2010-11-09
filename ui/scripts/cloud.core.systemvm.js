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
 
 function afterLoadSystemVmJSP($midmenuItem1) {
    hideMiddleMenu();			
    systemvmJsonToRightPanel($midmenuItem1);		
    
    initDialog("dialog_confirmation_start_systemVM");
    initDialog("dialog_confirmation_stop_systemVM");
    initDialog("dialog_confirmation_reboot_systemVM");
}

function systemvmJsonToRightPanel($leftmenuItem1) {
    systemvmJsonToDetailsTab($leftmenuItem1);
}

function systemvmJsonToDetailsTab($leftmenuItem1) {	   
    var jsonObj = $leftmenuItem1.data("jsonObj"); 
    var $detailsTab = $("#tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);   
    $detailsTab.find("#grid_header_title").text(fromdb(jsonObj.name));  
     
    resetViewConsoleAction(jsonObj, $detailsTab);         
    setVmStateInRightPanel(jsonObj.state, $detailsTab.find("#state"));		
    $detailsTab.find("#ipAddress").text(jsonObj.publicip);
        
    $detailsTab.find("#state").text(jsonObj.state);     
    $detailsTab.find("#systemvmtype").text(toSystemVMTypeText(jsonObj.systemvmtype));    
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename)); 
    $detailsTab.find("#id").text(fromdb(jsonObj.id));  
    $detailsTab.find("#name").text(fromdb(jsonObj.name));   
    $detailsTab.find("#activeviewersessions").text(fromdb(jsonObj.activeviewersessions)); 
    $detailsTab.find("#publicip").text(fromdb(jsonObj.publicip)); 
    $detailsTab.find("#privateip").text(fromdb(jsonObj.privateip)); 
    $detailsTab.find("#hostname").text(fromdb(jsonObj.hostname));
    $detailsTab.find("#gateway").text(fromdb(jsonObj.gateway)); 
    $detailsTab.find("#created").text(fromdb(jsonObj.created));   
        
    //actions ***
    var $actionLink = $detailsTab.find("#action_link"); 
    $actionLink.bind("mouseover", function(event) {	    
        $(this).find("#action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {       
        $(this).find("#action_menu").hide();    
        return false;
    });	  
    var $actionMenu = $actionLink.find("#action_menu");
    $actionMenu.find("#action_list").empty();   
    		
	if (jsonObj.state == 'Running') {	//Show "Stop System VM", "Reboot System VM"
	    buildActionLinkForTab("Stop System VM", systemVmActionMap, $actionMenu, $leftmenuItem1, $detailsTab);     
        buildActionLinkForTab("Reboot System VM", systemVmActionMap, $actionMenu, $leftmenuItem1, $detailsTab);   
	} 
	else if (jsonObj.state == 'Stopped') { //show "Start System VM"	    
	    buildActionLinkForTab("Start System VM", systemVmActionMap, $actionMenu, $leftmenuItem1, $detailsTab); 
	}     
}

function toSystemVMTypeText(value) {
    var text = "";
    if(value == "consoleproxy")
        text = "Console Proxy VM";
    else if(value == "secondarystoragevm")
        text = "Secondary Storage VM";
    return text;        
}


//SystemVM 
var systemVmActionMap = {      
    "Start System VM": {             
        isAsyncJob: true,
        asyncJobResponse: "startsystemvmresponse",
        inProcessText: "Starting System VM....",
        dialogBeforeActionFn : doStartSystemVM,
        afterActionSeccessFn: function(json, $leftmenuItem1, id) {        
            var item = json.queryasyncjobresultresponse.jobresult.systemvm;   
            $leftmenuItem1.data("jsonObj", item);
            systemvmJsonToRightPanel($leftmenuItem1);            
        }
    },
    "Stop System VM": {            
        isAsyncJob: true,
        asyncJobResponse: "stopsystemvmresponse",
        inProcessText: "Stopping System VM....",
        dialogBeforeActionFn : doStopSystemVM,
        afterActionSeccessFn: function(json, $leftmenuItem1, id) {
            var item = json.queryasyncjobresultresponse.jobresult.systemvm;   
            $leftmenuItem1.data("jsonObj", item);
            systemvmJsonToRightPanel($leftmenuItem1);      
        }
    },
    "Reboot System VM": {        
        isAsyncJob: true,
        asyncJobResponse: "rebootsystemvmresponse",
        inProcessText: "Rebooting System VM....",
        dialogBeforeActionFn : doRebootSystemVM,
        afterActionSeccessFn: function(json, $leftmenuItem1, id) {
            var item = json.queryasyncjobresultresponse.jobresult.systemvm;   
            $leftmenuItem1.data("jsonObj", item);
            systemvmJsonToRightPanel($leftmenuItem1);      
        }
    }
}   

function doStartSystemVM($actionLink, $detailsTab, $midmenuItem1) {   
    $("#dialog_confirmation_start_systemVM")	
    .dialog('option', 'buttons', { 						
	    "Confirm": function() { 
		    $(this).dialog("close"); 			
		    
		    var jsonObj = $midmenuItem1.data("jsonObj");
		    var id = jsonObj.id;
		    var apiCommand = "command=startSystemVm&id="+id;              
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab); 			   			   	                         					    
	    }, 
	    "Cancel": function() { 
		    $(this).dialog("close"); 
			
	    } 
    }).dialog("open");
}   

function doStopSystemVM($actionLink, $detailsTab, $midmenuItem1) {     
    $("#dialog_confirmation_stop_systemVM")	
    .dialog('option', 'buttons', { 						
	    "Confirm": function() { 
		    $(this).dialog("close"); 			
		    
		    var jsonObj = $midmenuItem1.data("jsonObj");
		    var id = jsonObj.id;
		    var apiCommand = "command=stopSystemVm&id="+id;  
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab); 			   	                         					    
	    }, 
	    "Cancel": function() { 
		    $(this).dialog("close"); 
			
	    } 
    }).dialog("open");
}   
   
function doRebootSystemVM($actionLink, $detailsTab, $midmenuItem1) {   
    $("#dialog_confirmation_reboot_systemVM")	
    .dialog('option', 'buttons', { 						
	    "Confirm": function() { 
		    $(this).dialog("close"); 			
		    
		    var jsonObj = $midmenuItem1.data("jsonObj");
		    var id = jsonObj.id;
		    var apiCommand = "command=rebootSystemVm&id="+id;              
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab); 		   			   	                         					    
	    }, 
	    "Cancel": function() { 
		    $(this).dialog("close"); 
			
	    } 
    }).dialog("open");
}   

