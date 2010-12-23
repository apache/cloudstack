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

function eventGetSearchParams() {
    var moreCriteria = [];	

    var searchInput = $("#basic_search").find("#search_input").val();	 
    if (searchInput != null && searchInput.length > 0) {	           
        moreCriteria.push("&keyword="+todb(searchInput));	       
    }     

	var $advancedSearchPopup = $("#advanced_search_popup");
	if (lastSearchType == "advanced_search" && $advancedSearchPopup.length > 0) {		    
	    var type = $advancedSearchPopup.find("#adv_search_type").val();							
		if (type!=null && trim(type).length > 0) 
			moreCriteria.push("&type="+todb(type));		
		
		var level = $advancedSearchPopup.find("#adv_search_level").val();	
	    if (level!=null && level.length > 0) 
			moreCriteria.push("&level="+todb(level));	
		
		if ($advancedSearchPopup.find("#adv_search_domain_li").css("display") != "none") {
		    var domainId = $advancedSearchPopup.find("#adv_search_domain").val();		
		    if (domainId!=null && domainId.length > 0) 
			    moreCriteria.push("&domainid="+todb(domainId));	
	    }
    	
    	if ($advancedSearchPopup.find("#adv_search_account_li").css("display") != "none" 
    	    && $advancedSearchPopup.find("#adv_search_account").hasClass("textwatermark") == false) {	
		    var account = $advancedSearchPopup.find("#adv_search_account").val();					
		    if (account!=null && account.length > 0) 
			    moreCriteria.push("&account="+todb(account));	
		}
		
		var startdate = $advancedSearchPopup.find("#adv_search_startdate").val();						
		if ($advancedSearchPopup.find("#adv_search_startdate").hasClass("textwatermark") == false && startdate!=null && startdate.length > 0) 
			moreCriteria.push("&startdate="+todb(startdate));	
		
		var enddate = $advancedSearchPopup.find("#adv_search_enddate").val();			
		if ($advancedSearchPopup.find("#adv_search_enddate").hasClass("textwatermark") == false && enddate!=null && enddate.length > 0) 
			moreCriteria.push("&enddate="+todb(enddate));	
	} 
		
	return moreCriteria.join("");          
}

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
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.description).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(fromdb(jsonObj.type).substring(0,25));  
}

function eventToRightPanel($midmenuItem1) {  
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);
    eventJsonToDetailsTab();   
}

function eventJsonToDetailsTab() {   
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null)
        return;
    
    var jsonObj = $midmenuItem1.data("jsonObj");
    if(jsonObj == null)
        return;    
    
    var $thisTab = $("#right_panel_content #tab_content_details");  
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();    
         
    $thisTab.find("#id").text(fromdb(jsonObj.id));
    $thisTab.find("#username").text(fromdb(jsonObj.username));
    $thisTab.find("#account").text(fromdb(jsonObj.account));
    $thisTab.find("#type").text(fromdb(jsonObj.type));
    $thisTab.find("#level").text(fromdb(jsonObj.level));   
    $thisTab.find("#description").text(fromdb(jsonObj.description));  
    $thisTab.find("#state").text(fromdb(jsonObj.state));     
    setDateField(jsonObj.created, $thisTab.find("#created"));	
    
    $thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();   
}