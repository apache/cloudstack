function loadEventToRigntPanelFn($rightPanelContent) {   
    var jsonObj = $rightPanelContent.data("jsonObj");
    var $rightPanelContent = $("#right_panel_content");
    $rightPanelContent.find("#username").text(fromdb(jsonObj.username));
    $rightPanelContent.find("#account").text(fromdb(jsonObj.account));
    $rightPanelContent.find("#type").text(jsonObj.type);
    $rightPanelContent.find("#level").text(jsonObj.level);   
    $rightPanelContent.find("#description").text(fromdb(jsonObj.description));  
    $rightPanelContent.find("#state").text(jsonObj.state);     
    setDateField(jsonObj.created, $rightPanelContent.find("#created"));	
}