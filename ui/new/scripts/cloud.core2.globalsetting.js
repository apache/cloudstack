 /**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

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
	        //call listConfigurations before bug 6506("What updateConfiguration API returns should include an embedded object") is fixed.
		    var jsonObj;		   
		    $.ajax({
		        data: createURL("command=listConfigurations&name="+name),
		        dataType: "json",
		        async: false,
		        success: function(json) {			                      
		            jsonObj = json.listconfigurationsresponse.configuration[0];
		        }
		    });		   
		    var $midmenuItem1 = $("#"+globalSettingGetMidmenuId(jsonObj));		   
		    globalSettingToMidmenu(jsonObj, $midmenuItem1);
		    globalSettingToRigntPanel($midmenuItem1);		        
		    	    
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

function globalSettingToRigntPanel($midmenuItem1) {
    globalSettingJsonToDetailsTab($midmenuItem1);   
}

function globalSettingJsonToDetailsTab($midmenuItem1) { 
    var jsonObj = $midmenuItem1.data("jsonObj");
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);          
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#value").text(fromdb(jsonObj.value));
    $detailsTab.find("#value_edit").val(fromdb(jsonObj.value));
    $detailsTab.find("#description").text(fromdb(jsonObj.description));   
    $detailsTab.find("#category").text(fromdb(jsonObj.category)); 
}