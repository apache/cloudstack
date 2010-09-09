function loadAccountToRigntPanelFn($rightPanelContent) {   
    var jsonObj = $rightPanelContent.data("jsonObj");
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