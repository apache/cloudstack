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

function afterLoadEventJSP() {

}

function eventToMidmenu(jsonObj, $midmenuItem1) {
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show();
    if(jsonObj.level == "INFO")
        $iconContainer.find("#icon").attr("src", "images/midmenuicon_events_info.png");
    else if(jsonObj.level == "ERROR")
        $iconContainer.find("#icon").attr("src", "images/midmenuicon_events_error.png");
    else if(jsonObj.level == "WARN")
        $iconContainer.find("#icon").attr("src", "images/midmenuicon_events_warning.png");
    
    $midmenuItem1.find("#first_row").text(jsonObj.description.substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.type.substring(0,25));  
}

function eventToRightPanel($midmenuItem1) {  
    eventJsonToDetailsTab($midmenuItem1);   
}

function eventJsonToDetailsTab($midmenuItem1) {   
    var jsonObj = $midmenuItem1.data("jsonObj");
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);  
      
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#username").text(fromdb(jsonObj.username));
    $detailsTab.find("#account").text(fromdb(jsonObj.account));
    $detailsTab.find("#type").text(jsonObj.type);
    $detailsTab.find("#level").text(jsonObj.level);   
    $detailsTab.find("#description").text(fromdb(jsonObj.description));  
    $detailsTab.find("#state").text(jsonObj.state);     
    setDateField(jsonObj.created, $detailsTab.find("#created"));	
}