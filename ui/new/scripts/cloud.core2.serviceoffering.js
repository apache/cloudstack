function afterLoadServiceOfferingJSP() {

}

function serviceOfferingToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", ("midmenuItem_"+jsonObj.id));  
    $midmenuItem1.data("jsonObj", jsonObj); 
        
    //var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    //$iconContainer.find("#icon").attr("src", "images/midmenuicon_storage_volume.png");	
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.cpunumber + " x " + convertHz(jsonObj.cpuspeed));  
}

function serviceOfferingToRigntPanel($midmenuItem) {
    var jsonObj = $midmenuItem.data("jsonObj");
    serviceOfferingJsonToDetailsTab(jsonObj);   
}

function serviceOfferingJsonToDetailsTab(jsonObj) { 
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);      
    $detailsTab.find("#id").text(jsonObj.id);
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#displaytext").text(fromdb(jsonObj.displaytext));
    $detailsTab.find("#storagetype").text(jsonObj.storagetype);
    $detailsTab.find("#cpu").text(jsonObj.cpunumber + " x " + convertHz(jsonObj.cpuspeed));
    $detailsTab.find("#memory").text(convertBytes(parseInt(jsonObj.memory)*1024*1024));
    $detailsTab.find("#offerha").text(toBooleanText(jsonObj.offerha));
    $detailsTab.find("#networktype").text(toNetworkType(jsonObj.usevirtualnetwork));
    $detailsTab.find("#tags").text(fromdb(jsonObj.tags));   
    setDateField(jsonObj.created, $detailsTab.find("#created"));	
}