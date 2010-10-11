function afterLoadEventJSP() {

}

function eventToMidmenu(jsonObj, $midmenuItem1) {
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show();
    if(jsonObj.level == "INFO")
        $iconContainer.find("#icon").attr("src", "images/midmenuicon_events_info.png");
    else if(jsonObj.level == "ERROR")
        $iconContainer.find("#icon").attr("src", "images/midmenuicon_events_error.png");
    else if(jsonObj.level == "WARN")
        $iconContainer.find("#icon").attr("src", "images/midmenuicon_events_warning.png");
    
    $midmenuItem1.find("#first_row").text(jsonObj.description.substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.type.substring(0,25));  
}

function eventToRigntPanel($midmenuItem) {      
    var jsonObj = $midmenuItem.data("jsonObj");
    eventJsonToDetailsTab(jsonObj);   
}

function eventJsonToDetailsTab(jsonObj) {   
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);  
      
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#username").text(fromdb(jsonObj.username));
    $detailsTab.find("#account").text(fromdb(jsonObj.account));
    $detailsTab.find("#type").text(jsonObj.type);
    $detailsTab.find("#level").text(jsonObj.level);   
    $detailsTab.find("#description").text(fromdb(jsonObj.description));  
    $detailsTab.find("#state").text(jsonObj.state);     
    setDateField(jsonObj.created, $detailsTab.find("#created"));	
}