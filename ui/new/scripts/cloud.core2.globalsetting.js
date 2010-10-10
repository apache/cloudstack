function afterLoadGlobalSettingJSP() {
    var $detailsTab = $("#right_panel_content #tab_content_details"); 

    //edit button ***
    var $readonlyFields  = $detailsTab.find("#value");
    var $editFields = $detailsTab.find("#value_edit"); 
    initializeEditFunction($readonlyFields, $editFields, doUpdateGlobalSetting);   
    
    //initialize dialogs
    initDialogWithOK("dialog_alert_restart_management_server");   
}

function doUpdateGlobalSetting() {
    // validate values
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    
    var isValid = true;					
    isValid &= validateString("Value", $detailsTab.find("#value_edit"), $detailsTab.find("#value_edit_errormsg"), true);					
    if (!isValid) 
        return;						
	
	var jsonObj = $detailsTab.data("jsonObj");		
	var name = jsonObj.name;
    var value = trim($detailsTab.find("#value_edit").val());

    $.ajax({
      data: createURL("command=updateConfiguration&name="+todb(name)+"&value="+todb(value)+"&response=json"),
	    dataType: "json",
	    success: function(json) {	        
		    $detailsTab.find("#value").text(value);
		    
		    //no embedded object returned, so....		   
		    jsonObj.value = value;
		    		    
		    globalSettingToMidmenu(jsonObj, $("#"+globalSettingGetMidmenuId(jsonObj)));		    
		    $("#dialog_alert_restart_management_server").dialog("open");
	    }
    });		
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
    $detailsTab.find("#value_edit").val(fromdb(jsonObj.value));
    $detailsTab.find("#description").text(fromdb(jsonObj.description));   
    $detailsTab.find("#category").text(fromdb(jsonObj.category)); 
}