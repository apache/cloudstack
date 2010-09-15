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

function routerToRigntPanel($midmenuItem) {      
    var jsonObj = $midmenuItem.data("jsonObj");
    
    var $rightPanelContent = $("#right_panel_content");
    
    $rightPanelContent.find("#state").text(fromdb(jsonObj.state));
    $rightPanelContent.find("#zonename").text(fromdb(jsonObj.zonename));
    $rightPanelContent.find("#name").text(fromdb(jsonObj.name));
    $rightPanelContent.find("#publicip").text(fromdb(jsonObj.publicip));
    $rightPanelContent.find("#privateip").text(fromdb(jsonObj.privateip));
    $rightPanelContent.find("#guestipaddress").text(fromdb(jsonObj.guestipaddress));
    $rightPanelContent.find("#hostname").text(fromdb(jsonObj.hostname));
    $rightPanelContent.find("#networkdomain").text(fromdb(jsonObj.networkdomain));
    $rightPanelContent.find("#account").text(fromdb(jsonObj.account));  
    setDateField(jsonObj.created, $rightPanelContent.find("#created"));	    
}