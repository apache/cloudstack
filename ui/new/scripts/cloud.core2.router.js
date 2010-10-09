function afterLoadRouterJSP() {
    
}

function routerToMidmenu(jsonObj, $midmenuItem1) {
    $midmenuItem1.attr("id", ("midmenuItem_"+jsonObj.id));  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    $midmenuItem1.find("#first_row").text(jsonObj.name.substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.publicip.substring(0,25));
    updateStateInMidMenu(jsonObj, $midmenuItem1);       
}

function routerAfterDetailsTabAction(json, id, midmenuItemId) {        
    var jsonObj = json.queryasyncjobresultresponse.router[0];    
    routerToMidmenu(jsonObj, $("#"+midmenuItemId));  
    routerJsonToDetailsTab(jsonObj);   
}

function routerToRigntPanel($midmenuItem) {      
    var jsonObj = $midmenuItem.data("jsonObj");
    routerJsonToDetailsTab(jsonObj);   
}

function routerJsonToDetailsTab(jsonObj) {    
    var $detailsTab = $("#right_panel_content #tab_content_details");    
    $detailsTab.data("jsonObj", jsonObj);         
    setVmStateInRightPanel(jsonObj.state, $detailsTab.find("#state"));  
    $detailsTab.find("#ipAddress").text(jsonObj.publicip);
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#publicip").text(fromdb(jsonObj.publicip));
    $detailsTab.find("#privateip").text(fromdb(jsonObj.privateip));
    $detailsTab.find("#guestipaddress").text(fromdb(jsonObj.guestipaddress));
    $detailsTab.find("#hostname").text(fromdb(jsonObj.hostname));
    $detailsTab.find("#networkdomain").text(fromdb(jsonObj.networkdomain));
    $detailsTab.find("#account").text(fromdb(jsonObj.account));  
    setDateField(jsonObj.created, $detailsTab.find("#created"));	 
    
    resetViewConsoleAction(jsonObj, $detailsTab);   
    
    //***** actions (begin) *****    
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    var noAvailableActions = true;
    var midmenuId = getMidmenuId(jsonObj);
    
    if (jsonObj.state == 'Running') {   
        buildActionLinkForDetailsTab("Stop Router", routerActionMap, $actionMenu, midmenuId);	
        buildActionLinkForDetailsTab("Reboot Router", routerActionMap, $actionMenu, midmenuId);	  
        noAvailableActions = false;      
    }
    else if (jsonObj.state == 'Stopped') {        
        buildActionLinkForDetailsTab("Start Router", routerActionMap, $actionMenu, midmenuId);	
        noAvailableActions = false;
    }  
    
    // no available actions 
	if(noAvailableActions == true) {
	    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
	}	   
    //***** actions (end) *****		    
}        
  
var routerActionMap = {  
    "Stop Router": {
        api: "stopRouter",            
        isAsyncJob: true,
        asyncJobResponse: "stoprouterresponse",
        inProcessText: "Stopping Router....",
        afterActionSeccessFn: routerAfterDetailsTabAction
    },
    "Start Router": {
        api: "startRouter",            
        isAsyncJob: true,
        asyncJobResponse: "startrouterresponse",
        inProcessText: "Starting Router....",
        afterActionSeccessFn: routerAfterDetailsTabAction
    },
    "Reboot Router": {
        api: "rebootRouter",           
        isAsyncJob: true,
        asyncJobResponse: "rebootrouterresponse",
        inProcessText: "Rebooting Router....",
        afterActionSeccessFn: routerAfterDetailsTabAction
    }
}   