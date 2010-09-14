function afterLoadIsoJSP() {

}

function isoToMidmenu(jsonObj, $midmenuItem1, toRightPanelFn) {    
    $midmenuItem1.attr("id", ("midmenuItem_"+jsonObj.id));  
    $midmenuItem1.data("jsonObj", jsonObj); 
        
    var $iconContainer = $midmenuItem1.find("#icon_container").show();
    setIconByOsType(jsonObj.ostypename, $iconContainer.find("#icon"));
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(fromdb(jsonObj.zonename).substring(0,25));           
    $midmenuItem1.data("toRightPanelFn", toRightPanelFn);
       
}

function isoToRigntPanel($midmenuItem) {       
    var jsonObj = $midmenuItem.data("jsonObj");
    
    var $rightPanelContent = $("#right_panel_content");
    $rightPanelContent.find("#id").text(fromdb(jsonObj.id));
    $rightPanelContent.find("#zonename").text(fromdb(jsonObj.zonename));
    $rightPanelContent.find("#name").text(fromdb(jsonObj.name));
    $rightPanelContent.find("#displaytext").text(fromdb(jsonObj.displaytext));
    $rightPanelContent.find("#account").text(fromdb(jsonObj.account));
    
    if(jsonObj.size != null)
	    $rightPanelContent.find("#size").text(convertBytes(parseInt(jsonObj.size)));       
    
    var status = "Ready";
	if (jsonObj.isready == "false")
		status = jsonObj.isostatus;	
	$rightPanelContent.find("#status").text(status); 
       
    setBooleanField(jsonObj.bootable, $rightPanelContent.find("#bootable"));	     
    setDateField(jsonObj.created, $rightPanelContent.find("#created"));	  
}