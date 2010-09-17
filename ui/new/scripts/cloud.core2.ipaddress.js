function afterLoadIpJSP() {

}

function ipGetMidmenuId(jsonObj) {   
    return "midmenuItem_" + jsonObj.ipaddress.replace(/\./g, "_");   //e.g. "192.168.33.108" => "192_168_33_108"
}

function ipToMidmenu(jsonObj, $midmenuItem1) {    
    var id = ipGetMidmenuId(jsonObj);
    $midmenuItem1.attr("id", id);  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show();
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_network_networkgroup.png");
    
    $midmenuItem1.find("#first_row").text(jsonObj.ipaddress.substring(0,25)); 
    $midmenuItem1.find("#second_row").text(fromdb(jsonObj.account).substring(0,25));    
}

function ipToRigntPanel($midmenuItem1) {       
    var jsonObj = $midmenuItem1.data("jsonObj");
    ipJsonToDetailsTab(jsonObj);   
}

function ipJsonToDetailsTab(jsonObj) {   
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);      
    
    $detailsTab.find("#ipaddress").text(fromdb(jsonObj.ipaddress));
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));
    $detailsTab.find("#vlanname").text(fromdb(jsonObj.vlanname));    
    setSourceNatField(jsonObj.issourcenat, $detailsTab.find("#source_nat")); 
    setNetworkTypeField(jsonObj.forvirtualnetwork, $detailsTab.find("#network_type"));    
    
    $detailsTab.find("#domain").text(fromdb(jsonObj.domain));
    $detailsTab.find("#account").text(fromdb(jsonObj.account));
    $detailsTab.find("#allocated").text(fromdb(jsonObj.allocated));
}

function setSourceNatField(value, $field) {
    if(value == "true")
        $field.text("Yes");
    else if(value == "false")
        $field.text("No");
    else
        $field.text("");
}

function setNetworkTypeField(value, $field) {
    if(value == "true")
        $field.text("Public");
    else if(value == "false")
        $field.text("Direct");
    else
        $field.text("");
}