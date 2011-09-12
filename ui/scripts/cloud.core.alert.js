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

function alertGetSearchParams() {
    var moreCriteria = [];	

    var searchInput = $("#basic_search").find("#search_input").val();	 
    if (searchInput != null && searchInput.length > 0) {	           
        moreCriteria.push("&keyword="+todb(searchInput));	       
    }     

	var $advancedSearchPopup = getAdvancedSearchPopupInSearchContainer();
	if ($advancedSearchPopup.length > 0 && $advancedSearchPopup.css("display") != "none" ) {	
	    var typeid = $advancedSearchPopup.find("#adv_search_typeid").val();							
		if ($advancedSearchPopup.find("#adv_search_typeid").hasClass("textwatermark") == false && typeid!=null && typeid.length > 0) 
			moreCriteria.push("&type="+todb(typeid));  //"type" paramter in listAlerts refers to typeId, not typeText		
	} 	
	
	return moreCriteria.join("");          
}

function afterLoadAlertJSP() {

}

function alertToMidmenu(jsonObj, $midmenuItem1) {      
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj); 
        
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_alerts.png");	
        
    var firstRowText = fromdb(jsonObj.description);
    $midmenuItem1.find("#first_row").text(clippedText(firstRowText, midMenuFirstRowLength));     
    $midmenuItem1.find("#first_row_container").attr("title", firstRowText);   
    
    var secondRowText = toAlertType(jsonObj.type);
    $midmenuItem1.find("#second_row").text(clippedText(secondRowText, midMenuSecondRowLength));
    $midmenuItem1.find("#second_row_container").attr("title", secondRowText); 
}

function alertToRightPanel($midmenuItem1) {   
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);
    alertJsonToDetailsTab();   
}

function alertJsonToDetailsTab() {   
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null)
        return;
    
    var jsonObj = $midmenuItem1.data("jsonObj");
    if(jsonObj == null)
        return;
          
    var $thisTab = $("#right_panel_content").find("#tab_content_details");  
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();        
         
    $.ajax({
        data: createURL("command=listAlerts&id="+jsonObj.id),
        dataType: "json",
        async: false,
        success: function(json) {  
            var items = json.listalertsresponse.alert;                   
            if(items != null && items.length > 0) {
                jsonObj = items[0];
                $midmenuItem1.data("jsonObj", jsonObj);  
            }
        }
    });   
          
    $thisTab.find("#id").text(fromdb(jsonObj.id));      
    $thisTab.find("#type").text(toAlertType(jsonObj.type));
    $thisTab.find("#typeid").text(fromdb(jsonObj.type));
    $thisTab.find("#description").text(fromdb(jsonObj.description));    
    setDateField(jsonObj.sent, $thisTab.find("#sent"));	
    
    $thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();  
}