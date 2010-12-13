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
 
 function systemVmGetSearchParams() {
     var moreCriteria = [];	

	var $advancedSearchPopup = $("#advanced_search_popup");
	if (lastSearchType == "advanced_search" && $advancedSearchPopup.length > 0) {
	    var name = $advancedSearchPopup.find("#adv_search_name").val();							
		if (name!=null && trim(name).length > 0) 
			moreCriteria.push("&name="+todb(name));	
		
		var state = $advancedSearchPopup.find("#adv_search_state").val();
		if (state!=null && state.length > 0) 
			moreCriteria.push("&state="+todb(state));		
				
		var zone = $advancedSearchPopup.find("#adv_search_zone").val();	
	    if (zone!=null && zone.length > 0) 
			moreCriteria.push("&zoneId="+zone);	
		
		if ($advancedSearchPopup.find("#adv_search_pod_li").css("display") != "none") {	
		    var pod = $advancedSearchPopup.find("#adv_search_pod").val();		
	        if (pod!=null && pod.length > 0) 
			    moreCriteria.push("&podId="+pod);
        }       
	} 
	else {     			    		
	    var searchInput = $("#basic_search").find("#search_input").val();	 
        if (lastSearchType == "basic_search" && searchInput != null && searchInput.length > 0) {	           
            moreCriteria.push("&keyword="+todb(searchInput));	       
        }        
	}
	
	return moreCriteria.join("");          
 }
 
 function afterLoadSystemVmJSP($midmenuItem1) {
    //hideMiddleMenu();			
    //systemvmToRightPanel($midmenuItem1);		
    
    initDialog("dialog_confirmation_start_systemVM");
    initDialog("dialog_confirmation_stop_systemVM");
    initDialog("dialog_confirmation_reboot_systemVM");
}

function systemvmToMidmenu(jsonObj, $midmenuItem1) {
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_resource_systemvm.png");		
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(fromdb(jsonObj.publicip));  
    
    updateVmStateInMidMenu(jsonObj, $midmenuItem1);      
}

function systemvmToRightPanel($midmenuItem1) {    
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);
    systemvmJsonToDetailsTab();
}

function systemvmJsonToDetailsTab() {
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null)
        return;
    
    var jsonObj = $midmenuItem1.data("jsonObj");
    if(jsonObj == null)
        return;
     
    var $thisTab = $("#right_panel_content").find("#tab_content_details");  
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();   
       
    $thisTab.find("#grid_header_title").text(fromdb(jsonObj.name));
       
    resetViewConsoleAction(jsonObj, $thisTab);         
    setVmStateInRightPanel(fromdb(jsonObj.state), $thisTab.find("#state"));		
    $thisTab.find("#ipAddress").text(fromdb(jsonObj.publicip));
        
    $thisTab.find("#state").text(fromdb(jsonObj.state));     
    $thisTab.find("#systemvmtype").text(toSystemVMTypeText(jsonObj.systemvmtype));    
    $thisTab.find("#zonename").text(fromdb(jsonObj.zonename)); 
    $thisTab.find("#id").text(fromdb(jsonObj.id));  
    $thisTab.find("#name").text(fromdb(jsonObj.name));     
    $thisTab.find("#publicip").text(fromdb(jsonObj.publicip)); 
    $thisTab.find("#privateip").text(fromdb(jsonObj.privateip)); 
    $thisTab.find("#hostname").text(fromdb(jsonObj.hostname));
    $thisTab.find("#gateway").text(fromdb(jsonObj.gateway)); 
    $thisTab.find("#created").text(fromdb(jsonObj.created));   
    
    if(jsonObj.systemvmtype == "consoleproxy") {
        $thisTab.find("#activeviewersessions").text(fromdb(jsonObj.activeviewersessions)); 
        $thisTab.find("#activeviewersessions_container").show();
    }
    else {  //jsonObj.systemvmtype == "secondarystoragevm"
        $thisTab.find("#activeviewersessions").text(""); 
        $thisTab.find("#activeviewersessions_container").hide();
    }    
        
    //actions ***
    var $actionLink = $thisTab.find("#action_link"); 
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
	    buildActionLinkForTab("Stop System VM", systemVmActionMap, $actionMenu, $midmenuItem1, $thisTab);     
        buildActionLinkForTab("Reboot System VM", systemVmActionMap, $actionMenu, $midmenuItem1, $thisTab);   
	} 
	else if (jsonObj.state == 'Stopped') { //show "Start System VM"	    
	    buildActionLinkForTab("Start System VM", systemVmActionMap, $actionMenu, $midmenuItem1, $thisTab); 
	}  
	
	$thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();      
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
        afterActionSeccessFn: function(json, $midmenuItem1, id) {            
            var jsonObj = json.queryasyncjobresultresponse.jobresult.systemvm;  
            systemvmToMidmenu(jsonObj, $midmenuItem1);
            systemvmToRightPanel($midmenuItem1);            
        }
    },
    "Stop System VM": {            
        isAsyncJob: true,
        asyncJobResponse: "stopsystemvmresponse",
        inProcessText: "Stopping System VM....",
        dialogBeforeActionFn : doStopSystemVM,
        afterActionSeccessFn: function(json, $midmenuItem1, id) {           
            var jsonObj = json.queryasyncjobresultresponse.jobresult.systemvm;                  	
            systemvmToMidmenu(jsonObj, $midmenuItem1);
            systemvmToRightPanel($midmenuItem1);      
        }
    },
    "Reboot System VM": {        
        isAsyncJob: true,
        asyncJobResponse: "rebootsystemvmresponse",
        inProcessText: "Rebooting System VM....",
        dialogBeforeActionFn : doRebootSystemVM,
        afterActionSeccessFn: function(json, $midmenuItem1, id) {            
            var jsonObj = json.queryasyncjobresultresponse.jobresult.systemvm;              
            systemvmToMidmenu(jsonObj, $midmenuItem1);
            systemvmToRightPanel($midmenuItem1);      
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

