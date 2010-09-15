function afterLoadRouterJSP() {
    
}

function routerToMidmenu(jsonObj, $midmenuItem1) {
    $midmenuItem1.attr("id", ("midmenuItem_"+jsonObj.id));  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    /*
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_routers_info.png");    
    */ 
    
    $midmenuItem1.find("#first_row").text(jsonObj.name.substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.publicip.substring(0,25));     
}

function routerAfterDetailsTabAction(jsonObj) {
    $("#midmenuItem_"+jsonObj.id).data("jsonObj", jsonObj);   
    routerJsonToDetailsTab(jsonObj);   
}

function routerToRigntPanel($midmenuItem) {      
    var jsonObj = $midmenuItem.data("jsonObj");
    routerJsonToDetailsTab(jsonObj);   
}

function routerJsonToDetailsTab(jsonObj) {    
    var $detailsTab = $("#right_panel_content #tab_content_details");    
    $detailsTab.data("jsonObj", jsonObj);     
    $detailsTab.find("#state").text(fromdb(jsonObj.state));
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#publicip").text(fromdb(jsonObj.publicip));
    $detailsTab.find("#privateip").text(fromdb(jsonObj.privateip));
    $detailsTab.find("#guestipaddress").text(fromdb(jsonObj.guestipaddress));
    $detailsTab.find("#hostname").text(fromdb(jsonObj.hostname));
    $detailsTab.find("#networkdomain").text(fromdb(jsonObj.networkdomain));
    $detailsTab.find("#account").text(fromdb(jsonObj.account));  
    setDateField(jsonObj.created, $detailsTab.find("#created"));	  
    
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
    
    if (jsonObj.state == 'Running') {
        //template.find(".grid_links").find("#router_action_stop_container, #router_action_reboot_container, #router_action_view_console_container").show();	
        buildActionLinkForDetailsTab("Stop Router", routerActionMap, $actionMenu, routerListAPIMap);	
        buildActionLinkForDetailsTab("Reboot Router", routerActionMap, $actionMenu, routerListAPIMap);	
        //buildActionLinkForDetailsTab("View Console", routerActionMap, $actionMenu, routerListAPIMap);	
    }
    else if (jsonObj.state == 'Stopped') {
        //template.find(".grid_links").find("#router_action_start_container").show();
        buildActionLinkForDetailsTab("Start Router", routerActionMap, $actionMenu, routerListAPIMap);	
    }   
}

var routerListAPIMap = {
    listAPI: "listRouters",
    listAPIResponse: "listroutersresponse",
    listAPIResponseObj: "router"
};           
  
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