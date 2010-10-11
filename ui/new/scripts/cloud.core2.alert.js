function afterLoadAlertJSP() {

}

function alertToMidmenu(jsonObj, $midmenuItem1) {      
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj); 
    $midmenuItem1.find("#first_row").text(jsonObj.description.substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.type.substring(0,25)); 
}

function alertToRigntPanel($midmenuItem) {      
    var jsonObj = $midmenuItem.data("jsonObj");
    alertJsonToDetailsTab(jsonObj);   
}

function alertJsonToDetailsTab(jsonObj) {   
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);  
        
    $detailsTab.find("#type").text(jsonObj.type);
    $detailsTab.find("#description").text(jsonObj.description);    
    setDateField(jsonObj.sent, $detailsTab.find("#sent"));	
}