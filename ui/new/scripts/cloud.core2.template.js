function afterLoadTemplateJSP() {

}

function templateToMidmenu(jsonObj, $midmenuItem1, toRightPanelFn) {    
    $midmenuItem1.attr("id", ("midmenuItem_"+jsonObj.id));  
    $midmenuItem1.data("jsonObj", jsonObj); 
       
    var $iconContainer = $midmenuItem1.find("#icon_container").show();
    setIconByOsType(jsonObj.ostypename, $iconContainer.find("#icon"));
    
    if(jsonObj.level == "INFO")
        iconContainer.find("#icon").attr("src", "images/midmenuicon_events_info.png");
    else if(jsonObj.level == "ERROR")
        iconContainer.find("#icon").attr("src", "images/midmenuicon_events_error.png");
    else if(jsonObj.level == "WARN")
        iconContainer.find("#icon").attr("src", "images/midmenuicon_events_warning.png");    
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(fromdb(jsonObj.zonename).substring(0,25));           
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
    
    setBooleanField(jsonObj.passwordenabled, $rightPanelContent.find("#passwordenabled"));	
    setBooleanField(jsonObj.ispublic, $rightPanelContent.find("#ispublic"));	
    setBooleanField(jsonObj.isfeatured, $rightPanelContent.find("#isfeatured"));
    setBooleanField(jsonObj.crossZones, $rightPanelContent.find("#crossZones"));
    
    $rightPanelContent.find("#ostypename").text(fromdb(jsonObj.ostypename));
    $rightPanelContent.find("#account").text(fromdb(jsonObj.account));
    
    if(jsonObj.size != null)
	    $rightPanelContent.find("#size").text(convertBytes(parseInt(jsonObj.size)));        
    
    setDateField(jsonObj.created, $rightPanelContent.find("#created"));	    
}

//setIconByOsType() is shared by template page and ISO page
function setIconByOsType(osType, $field) {
	if (osType == null || osType.length == 0)
		return; 	
	if (osType.match("^CentOS") != null)
		$field.attr("src", "images/midmenuicon_template_centos.png");
	else if (osType.match("^Windows") != null) 
		$field.attr("src", "images/midmenuicon_template_windows.png");
	else 
		$field.attr("src", "images/midmenuicon_template_linux.png");
}