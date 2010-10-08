function afterLoadGlobalSettingJSP() {

}

function globalSettingGetMidmenuId(jsonObj) {
    return "midmenuItem_" + fromdb(jsonObj.name).replace(/\./g, "_").replace(/\s/g, ""); //remove all spaces in jsonObj.name
}

function globalSettingToMidmenu(jsonObj, $midmenuItem1) {    
    var id = globalSettingGetMidmenuId(jsonObj);
    $midmenuItem1.attr("id", id);   
       
    $midmenuItem1.data("jsonObj", jsonObj); 
        
    //var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    //$iconContainer.find("#icon").attr("src", "images/midmenuicon_storage_volume.png");	
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(fromdb(jsonObj.value).substring(0,25));  
}

function globalSettingToRigntPanel($midmenuItem) {
    var jsonObj = $midmenuItem.data("jsonObj");
    globalSettingJsonToDetailsTab(jsonObj);   
}

function globalSettingJsonToDetailsTab(jsonObj) { 
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);          
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#value").text(fromdb(jsonObj.value));
    $detailsTab.find("#description").text(fromdb(jsonObj.description));   
}