function afterLoadAlertJSP() {

}

function alertToMidmenu(jsonObj, $midmenuItem1, toRightPanelFn) {      
    $midmenuItem1.attr("id", ("midmenuItem_"+jsonObj.id));                             
    $midmenuItem1.data("id", jsonObj.id); 
    $midmenuItem1.data("jsonObj", jsonObj); 
    $midmenuItem1.find("#first_row").text(jsonObj.description.substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.type.substring(0,25));           
    $midmenuItem1.data("toRightPanelFn", toRightPanelFn);
}

function alertToRigntPanel($midmenuItem) {      
    var jsonObj = $midmenuItem.data("jsonObj");
    
    var $rightPanelContent = $("#right_panel_content");    
    $rightPanelContent.find("#type").text(jsonObj.type);
    $rightPanelContent.find("#description").text(jsonObj.description);    
    setDateField(jsonObj.sent, $rightPanelContent.find("#sent"));	
}