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

function routerGetSearchParams() {
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
        
        if ($advancedSearchPopup.find("#adv_search_domain_li").css("display") != "none") {		
		    var domainId = $advancedSearchPopup.find("#adv_search_domain").val();		
		    if (domainId!=null && domainId.length > 0) 
			    moreCriteria.push("&domainid="+domainId);	
    	}	
    	
		if ($advancedSearchPopup.find("#adv_search_account_li").css("display") != "none") {	
		    var account = $advancedSearchPopup.find("#adv_search_account").val();		
		    if (account!=null && account.length > 0) 
			    moreCriteria.push("&account="+account);		
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

function afterLoadRouterJSP() {
    
}

function routerToMidmenu(jsonObj, $midmenuItem1) {
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    $midmenuItem1.find("#first_row").text(jsonObj.name.substring(0,25)); 
    $midmenuItem1.find("#second_row").text(fromdb(jsonObj.publicip).substring(0,25));
    updateVmStateInMidMenu(jsonObj, $midmenuItem1);       
}

function routerToRightPanel($midmenuItem1) { 
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);
    routerJsonToDetailsTab();   
}

function routerJsonToDetailsTab() {   
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
            
    setVmStateInRightPanel(fromdb(jsonObj.state), $thisTab.find("#state"));  
    $thisTab.find("#ipAddress").text(fromdb(jsonObj.publicip));
    $thisTab.find("#zonename").text(fromdb(jsonObj.zonename));
    $thisTab.find("#name").text(fromdb(jsonObj.name));
    $thisTab.find("#publicip").text(fromdb(jsonObj.publicip));
    $thisTab.find("#privateip").text(fromdb(jsonObj.privateip));
    $thisTab.find("#guestipaddress").text(fromdb(jsonObj.guestipaddress));
    $thisTab.find("#hostname").text(fromdb(jsonObj.hostname));
    $thisTab.find("#networkdomain").text(fromdb(jsonObj.networkdomain));
    $thisTab.find("#account").text(fromdb(jsonObj.account));  
    setDateField(jsonObj.created, $thisTab.find("#created"));	 
    
    resetViewConsoleAction(jsonObj, $thisTab);   
    
    //***** actions (begin) *****    
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    var noAvailableActions = true;
   
    if (jsonObj.state == 'Running') {   
        buildActionLinkForTab("Stop Router", routerActionMap, $actionMenu, $midmenuItem1, $thisTab);	
        buildActionLinkForTab("Reboot Router", routerActionMap, $actionMenu, $midmenuItem1, $thisTab);	  
        noAvailableActions = false;      
    }
    else if (jsonObj.state == 'Stopped') {        
        buildActionLinkForTab("Start Router", routerActionMap, $actionMenu, $midmenuItem1, $thisTab);	
        noAvailableActions = false;
    }  
    
    // no available actions 
	if(noAvailableActions == true) {
	    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
	}	   
    //***** actions (end) *****	
    
    $thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();     		    
}        
  
function doStopRouter($actionLink, $detailsTab, $midmenuItem1) {     
    $("#dialog_confirmation")
    .text("Please confirm you want to stop router")	
    .dialog('option', 'buttons', { 						
	    "Confirm": function() { 
		    $(this).dialog("close"); 			
		    
		    var jsonObj = $midmenuItem1.data("jsonObj");
		    var id = jsonObj.id;
		    var apiCommand = "command=stopRouter&id="+id;  
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab); 			   	                         					    
	    }, 
	    "Cancel": function() { 
		    $(this).dialog("close"); 
			
	    } 
    }).dialog("open");
}   
   
function doStartRouter($actionLink, $detailsTab, $midmenuItem1) {     
    $("#dialog_confirmation")
    .text("Please confirm you want to start router")	
    .dialog('option', 'buttons', { 						
	    "Confirm": function() { 
		    $(this).dialog("close"); 			
		    
		    var jsonObj = $midmenuItem1.data("jsonObj");
		    var id = jsonObj.id;
		    var apiCommand = "command=startRouter&id="+id;  
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab); 			   	                         					    
	    }, 
	    "Cancel": function() { 
		    $(this).dialog("close"); 
			
	    } 
    }).dialog("open");
}  

function doRebootRouter($actionLink, $detailsTab, $midmenuItem1) {     
    $("#dialog_confirmation")
    .text("Please confirm you want to reboot router")	
    .dialog('option', 'buttons', { 						
	    "Confirm": function() { 
		    $(this).dialog("close"); 			
		    
		    var jsonObj = $midmenuItem1.data("jsonObj");
		    var id = jsonObj.id;
		    var apiCommand = "command=rebootRouter&id="+id;  
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab); 			   	                         					    
	    }, 
	    "Cancel": function() { 
		    $(this).dialog("close"); 
			
	    } 
    }).dialog("open");
}     
  
var routerActionMap = {      
    "Start Router": {        
        isAsyncJob: true,
        asyncJobResponse: "startrouterresponse",
        inProcessText: "Starting Router....",
        dialogBeforeActionFn : doStartRouter,
        afterActionSeccessFn: function(json, $midmenuItem1, id) {
            var item = json.queryasyncjobresultresponse.jobresult.domainrouter;    
            routerToMidmenu(item, $midmenuItem1);  
            routerJsonToDetailsTab($midmenuItem1);   
        }
    },
    "Stop Router": {          
        isAsyncJob: true,
        asyncJobResponse: "stoprouterresponse",
        inProcessText: "Stopping Router....",
        dialogBeforeActionFn : doStopRouter,
        afterActionSeccessFn: function(json, $midmenuItem1, id) {
            var item = json.queryasyncjobresultresponse.jobresult.domainrouter;    
            routerToMidmenu(item, $midmenuItem1);  
            routerJsonToDetailsTab($midmenuItem1);   
        }
    },
    "Reboot Router": {           
        isAsyncJob: true,
        asyncJobResponse: "rebootrouterresponse",
        inProcessText: "Rebooting Router....",
        dialogBeforeActionFn : doRebootRouter,
        afterActionSeccessFn: function(json, $midmenuItem1, id) {
            var item = json.queryasyncjobresultresponse.jobresult.domainrouter;    
            routerToMidmenu(item, $midmenuItem1);  
            routerJsonToDetailsTab($midmenuItem1);    
        }
    }
}   