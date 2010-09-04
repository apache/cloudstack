function loadSnapshotToRigntPanelFn($rightPanelContent) {   
    var jsonObj = $rightPanelContent.data("jsonObj");
    
    var $rightPanelContent = $("#right_panel_content");
    
    $rightPanelContent.find("#id").text(jsonObj.id);
    $rightPanelContent.find("#name").text(jsonObj.name);
    $rightPanelContent.find("#volume_name").text(jsonObj.volumename);
    $rightPanelContent.find("#interval_type").text(jsonObj.intervaltype);
    $rightPanelContent.find("#account").text(jsonObj.account);
    $rightPanelContent.find("#domain").text(jsonObj.domain);   
    
    setDateField(jsonObj.created, $rightPanelContent.find("#created"));	
}