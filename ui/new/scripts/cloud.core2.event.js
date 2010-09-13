function afterLoadEventJSP() {

}

function eventToMidmenu(jsonObj, $midmenuItem1, toRightPanelFn) {
    $midmenuItem1.attr("id", ("midmenuItem_"+jsonObj.id));  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    var iconContainer = $midmenuItem1.find("#icon_container").show();
    if(jsonObj.level == "INFO")
        iconContainer.find("#icon").attr("src", "images/midmenuicon_events_info.png");
    else if(jsonObj.level == "ERROR")
        iconContainer.find("#icon").attr("src", "images/midmenuicon_events_error.png");
    else if(jsonObj.level == "WARN")
        iconContainer.find("#icon").attr("src", "images/midmenuicon_events_warning.png");
    
    $midmenuItem1.find("#first_row").text(jsonObj.description.substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.type.substring(0,25));           
    $midmenuItem1.data("toRightPanelFn", toRightPanelFn);
}

function eventToRigntPanel($midmenuItem) {      
    var jsonObj = $midmenuItem.data("jsonObj");
    
    var $rightPanelContent = $("#right_panel_content");
    $rightPanelContent.find("#id").text(fromdb(jsonObj.id));
    $rightPanelContent.find("#username").text(fromdb(jsonObj.username));
    $rightPanelContent.find("#account").text(fromdb(jsonObj.account));
    $rightPanelContent.find("#type").text(jsonObj.type);
    $rightPanelContent.find("#level").text(jsonObj.level);   
    $rightPanelContent.find("#description").text(fromdb(jsonObj.description));  
    $rightPanelContent.find("#state").text(jsonObj.state);     
    setDateField(jsonObj.created, $rightPanelContent.find("#created"));	
}