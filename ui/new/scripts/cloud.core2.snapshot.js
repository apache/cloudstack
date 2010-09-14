function afterLoadSnapshotJSP() {

}

function snapshotToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", ("midmenuItem_"+jsonObj.id)); 
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_storage_snapshots.png");		
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(fromdb(jsonObj.volumename).substring(0,25));    
}

function snapshotToRigntPanel($midmenuItem) {      
    var jsonObj = $midmenuItem.data("jsonObj");
    
    var $rightPanelContent = $("#right_panel_content");    
    $rightPanelContent.find("#id").text(jsonObj.id);
    $rightPanelContent.find("#name").text(fromdb(jsonObj.name));
    $rightPanelContent.find("#volume_name").text(fromdb(jsonObj.volumename));
    $rightPanelContent.find("#interval_type").text(jsonObj.intervaltype);
    $rightPanelContent.find("#account").text(fromdb(jsonObj.account));
    $rightPanelContent.find("#domain").text(fromdb(jsonObj.domain));      
    setDateField(jsonObj.created, $rightPanelContent.find("#created"));	
}