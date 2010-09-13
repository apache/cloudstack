function afterLoadTemplateJSP() {

}

function templateToMidmenu(jsonObj, $midmenuItem1, toRightPanelFn) {    
    $midmenuItem1.attr("id", ("midmenuItem_"+jsonObj.id));  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    /*
    var iconContainer = $midmenuItem1.find("#icon_container").show();
    if(jsonObj.level == "INFO")
        iconContainer.find("#icon").attr("src", "images/midmenuicon_events_info.png");
    else if(jsonObj.level == "ERROR")
        iconContainer.find("#icon").attr("src", "images/midmenuicon_events_error.png");
    else if(jsonObj.level == "WARN")
        iconContainer.find("#icon").attr("src", "images/midmenuicon_events_warning.png");
    */
    
    $midmenuItem1.find("#first_row").text(jsonObj.name.substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.zonename.substring(0,25));           
    $midmenuItem1.data("toRightPanelFn", toRightPanelFn);
       
}

function templateToRigntPanel($midmenuItem) {       
    var jsonObj = $midmenuItem.data("jsonObj");
    
    var $rightPanelContent = $("#right_panel_content");
    $rightPanelContent.find("#id").text(fromdb(jsonObj.id));
    $rightPanelContent.find("#zonename").text(fromdb(jsonObj.zonename));
    $rightPanelContent.find("#name").text(fromdb(jsonObj.name));
    $rightPanelContent.find("#displaytext").text(fromdb(jsonObj.displaytext));
    
    var status = "Ready";
	if (jsonObj.isready == "false") 
		status = jsonObj.templatestatus;	
	$rightPanelContent.find("#status").text(status);    
    
    $rightPanelContent.find("#passwordenabled").text(fromdb(jsonObj.passwordenabled));
    $rightPanelContent.find("#ispublic").text(fromdb(jsonObj.ispublic));   
    $rightPanelContent.find("#isfeatured").text(fromdb(jsonObj.isfeatured));
    $rightPanelContent.find("#crossZones").text(fromdb(jsonObj.crossZones));
    $rightPanelContent.find("#ostypename").text(fromdb(jsonObj.ostypename));
    $rightPanelContent.find("#account").text(fromdb(jsonObj.account));
    
    if(jsonObj.size != null)
	    $rightPanelContent.find("#size").text(convertBytes(parseInt(jsonObj.size)));        
    
    setDateField(jsonObj.created, $rightPanelContent.find("#created"));	    
}