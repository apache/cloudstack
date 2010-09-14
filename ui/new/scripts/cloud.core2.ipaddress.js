function afterLoadIpJSP() {

}

function ipToMidmenu(jsonObj, $midmenuItem1, toRightPanelFn) {    
    $midmenuItem1.attr("id", ("midmenuItem_"+jsonObj.id));  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show();
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_network_networkgroup.png");
    
    $midmenuItem1.find("#first_row").text(jsonObj.ipaddress.substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.account.substring(0,25));           
    $midmenuItem1.data("toRightPanelFn", toRightPanelFn);
}

function ipToRigntPanel($midmenuItem) {   
    var jsonObj = $midmenuItem.data("jsonObj");
    
    var $rightPanelContent = $("#right_panel_content");    
    //$rightPanelContent.find("#type").text(jsonObj.type);
    //$rightPanelContent.find("#description").text(jsonObj.description);    
    //setDateField(jsonObj.sent, $rightPanelContent.find("#sent"));	
}