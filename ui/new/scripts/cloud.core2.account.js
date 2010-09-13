function afterLoadAccountJSP() {

}

function accountToMidmenu(jsonObj, $midmenuItem1, toRightPanelFn) {  
    $midmenuItem1.attr("id", ("midmenuItem_"+jsonObj.id));  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    var iconContainer = $midmenuItem1.find("#icon_container").show();   
    if (jsonObj.accounttype == roleTypeUser) 
        iconContainer.find("#icon").attr("src", "images/midmenuicon_account_user.png");		
	else if (jsonObj.accounttype == roleTypeAdmin) 
	    iconContainer.find("#icon").attr("src", "images/midmenuicon_account_admin.png");		
	else if (jsonObj.accounttype == roleTypeDomainAdmin) 
	    iconContainer.find("#icon").attr("src", "images/midmenuicon_account_domain.png");	
    
    $midmenuItem1.find("#first_row").text(jsonObj.name.substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.domain.substring(0,25));           
    $midmenuItem1.data("toRightPanelFn", toRightPanelFn);
}

function accountToRigntPanel($midmenuItem) {      
    var jsonObj = $midmenuItem.data("jsonObj");
    
    var $rightPanelContent = $("#right_panel_content");
    $rightPanelContent.find("#role").text(toRole(jsonObj.accounttype));
    $rightPanelContent.find("#account").text(fromdb(jsonObj.name));
    $rightPanelContent.find("#domain").text(fromdb(jsonObj.domain));
    $rightPanelContent.find("#vm_total").text(jsonObj.vmtotal);
    $rightPanelContent.find("#ip_total").text(jsonObj.iptotal);
    $rightPanelContent.find("#bytes_received").text(jsonObj.receivedbytes);
    $rightPanelContent.find("#bytes_sent").text(jsonObj.sentbytes);
    $rightPanelContent.find("#state").text(jsonObj.state);
}