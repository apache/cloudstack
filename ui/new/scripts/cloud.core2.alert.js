function loadAlertToRigntPanelFn($rightPanelContent) {   
    var jsonObj = $rightPanelContent.data("jsonObj");
    
    var $rightPanelContent = $("#right_panel_content");
    
    $rightPanelContent.find("#type").text(jsonObj.type);
    $rightPanelContent.find("#description").text(jsonObj.description);
    
    setDateField(jsonObj.sent, $rightPanelContent.find("#sent"));	
}