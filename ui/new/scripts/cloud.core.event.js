function loadEventToRigntPanelFn($rightPanelContent) {   
    var jsonObj = $rightPanelContent.data("jsonObj");
    var $rightPanelContent = $("#right_panel_content");
    $rightPanelContent.find("#username").text(jsonObj.username);
    $rightPanelContent.find("#account").text(jsonObj.account);
    $rightPanelContent.find("#type").text(jsonObj.type);
    $rightPanelContent.find("#level").text(jsonObj.level);   
    $rightPanelContent.find("#description").text(jsonObj.description);  
    $rightPanelContent.find("#state").text(jsonObj.state);     
    setDateField(jsonObj.created, $rightPanelContent.find("#created"));	
}