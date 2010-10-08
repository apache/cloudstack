function afterLoadDiskOfferingJSP() {

}

function diskOfferingToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", ("midmenuItem_"+jsonObj.id));  
    $midmenuItem1.data("jsonObj", jsonObj); 
        
    //var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    //$iconContainer.find("#icon").attr("src", "images/midmenuicon_storage_volume.png");	
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(convertBytes(jsonObj.disksize));  
}

function diskOfferingToRigntPanel($midmenuItem) {
    var jsonObj = $midmenuItem.data("jsonObj");
    diskOfferingJsonToDetailsTab(jsonObj);   
}

function diskOfferingJsonToDetailsTab(jsonObj) { 
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);      
    $detailsTab.find("#id").text(jsonObj.id);
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#displaytext").text(fromdb(jsonObj.displaytext));
    $detailsTab.find("#disksize").text(convertBytes(jsonObj.disksize));
    $detailsTab.find("#tags").text(fromdb(jsonObj.tags));   
    $detailsTab.find("#domain").text(fromdb(jsonObj.domain));    
}