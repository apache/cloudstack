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

function showEventsTab(showEvents) {
    var currentSubMenu = $("#submenu_events");
    
    var initializeEventTab = function(isAdmin) {   
        var eIndex = 0;		        
        function eventJSONToTemplate(json, template) {           
            if (eIndex++ % 2 == 0) {
			    template.addClass("smallrow_odd");
		    } else {
			    template.addClass("smallrow_even");
		    }		    
		    template.find("#event_account").text(json.account);
		    template.find("#event_username").text(json.username);
		    template.find("#event_type").text(json.type);
		    template.find("#event_level").text(json.level);
		    template.find("#event_desc").text(json.description);  
		    template.find("#event_state").text(json.state);  
		    
		    setDateField(json.created, template.find("#event_date"));		   			    
        }
      
        function listEvents() {      
            var submenuContent = $("#submenu_content_events");  
            
            var commandString;            
			var advanced = submenuContent.find("#search_button").data("advanced");                    
			if (advanced != null && advanced) {		
			    var type = submenuContent.find("#advanced_search #adv_search_type").val();	
			    var level = submenuContent.find("#advanced_search #adv_search_level").val();
			    var domainId = submenuContent.find("#advanced_search #adv_search_domain").val();	
			    var account = submenuContent.find("#advanced_search #adv_search_account").val();
			    var startdate = submenuContent.find("#advanced_search #adv_search_startdate").val();	
			    var enddate = submenuContent.find("#advanced_search #adv_search_enddate").val();	
			    var moreCriteria = [];								
				if (type!=null && trim(type).length > 0) 
					moreCriteria.push("&type="+encodeURIComponent(trim(type)));		
			    if (level!=null && level.length > 0) 
					moreCriteria.push("&level="+encodeURIComponent(trim(level)));	
				if (domainId!=null && domainId.length > 0) 
					moreCriteria.push("&domainid="+domainId);					
				if (account!=null && account.length > 0) 
					moreCriteria.push("&account="+account);					
				if (startdate!=null && startdate.length > 0) 
					moreCriteria.push("&startdate="+encodeURIComponent(startdate));		
				if (enddate!=null && enddate.length > 0) 
					moreCriteria.push("&enddate="+encodeURIComponent(enddate));		
				commandString = "command=listEvents&page="+currentPage+moreCriteria.join("")+"&response=json";   
			} else {          	 
                var searchInput = submenuContent.find("#search_input").val();            
                if (searchInput != null && searchInput.length > 0) 
                    commandString = "command=listEvents&page="+currentPage+"&keyword="+searchInput+"&response=json"
                else
                    commandString = "command=listEvents&page="+currentPage+"&response=json";	
            } 
            
            //listItems(submenuContent, commandString, jsonResponse1, jsonResponse2, template, fnJSONToTemplate);         
            listItems(submenuContent, commandString, "listeventsresponse", "event", $("#event_template"), eventJSONToTemplate);    
		}	
	    
	    submenuContentEventBinder($("#submenu_content_events"), listEvents);
	    	    
	    if(isAdmin) {	        
	        $("#submenu_events").bind("click", function(event) {
	            event.preventDefault();
	            
				$(this).removeClass().addClass("submenu_links_on");
				currentSubMenu.removeClass().toggleClass("submenu_links_off");
			    currentSubMenu = $(this);
			    
				var submenuContent = $("#submenu_content_events").show();
				$("#submenu_content_alerts").hide();				
				
				submenuContent.find("#adv_search_domain_li, #adv_search_account_li").show();  
					
				currentPage = 1;			
				listEvents();    
	        });	   
	         
	        $(".submenu_links, #submenu_content_alerts, #alert_template").show(); 
	        $("#event_account_header, #event_account_container").show();	 	
	                    
	        if (showEvents == null || showEvents) {
				currentSubMenu = $("#submenu_alerts");
				$("#submenu_events").click();  //Default tab is Events when login as admin	  
			} else {
				currentSubMenu = $("#submenu_events");
				$("#submenu_alerts").click();
			}
	    }
	    else {
	        $(".submenu_links, #submenu_content_alerts, #alert_template").hide();
	        $("#event_account_header, #event_account_container").hide();	 
			$("#submenu_content_events").show();
	        listEvents();    
	    }   
    }
    

	// Manage Events 	
	var advancedSearch = $("#advanced_search");
	advancedSearch.find("#adv_search_startdate, #adv_search_enddate").datepicker({dateFormat: 'yy-mm-dd'});
		
    if (isAdmin()) {				
		// *** Alerts (begin) ***
		var alertIndex = 0;
		function alertJSONToTemplate(json, template) {           
            if (alertIndex++ % 2 == 0) {
		        template.addClass("smallrow_odd");
	        } else {
		        template.addClass("smallrow_even");
	        }		    
		   		    
	        template.find("#alert_type").text((toAlertType(json.type)));
		    template.find("#alert_desc").text(json.description);
			
			setDateField(json.sent, template.find("#alert_sent"));			    					    
        }
		
		function listAlerts() {		
		    var submenuContent = $("#submenu_content_alerts");
        	   
        	var commandString;            
		    var advanced = submenuContent.find("#search_button").data("advanced");                    
		    if (advanced != null && advanced) {		
		        var type = submenuContent.find("#advanced_search #adv_search_type").val();				       
		        var moreCriteria = [];								
			    if (type!=null && trim(type).length > 0) 
				    moreCriteria.push("&type="+encodeURIComponent(trim(type)));			   
			    commandString = "command=listAlerts&page="+currentPage+moreCriteria.join("")+"&response=json";     
		    } else {            
        	    var searchInput = submenuContent.find("#search_input").val();            
                if (searchInput != null && searchInput.length > 0) 
                    commandString = "command=listAlerts&page="+currentPage+"&keyword="+searchInput+"&response=json"
                else
                    commandString = "command=listAlerts&page="+currentPage+"&response=json";    
            }
        	
        	//listItems(submenuContent, commandString, jsonResponse1, jsonResponse2, template, fnJSONToTemplate);         
            listItems(submenuContent, commandString, "listalertsresponse", "alert", $("#alert_template"), alertJSONToTemplate);              	
		}			
		
		submenuContentEventBinder($("#submenu_content_alerts"), listAlerts);
			
		$("#submenu_alerts").bind("click", function(event) {
		    event.preventDefault();         			   			
			
			$(this).removeClass().addClass("submenu_links_on");
			currentSubMenu.removeClass().toggleClass("submenu_links_off");
			currentSubMenu = $(this);
			
			var submenuContent = $("#submenu_content_alerts").show();
			$("#submenu_content_events").hide();
			
			currentPage = 1;
			listAlerts();
		});					
		// *** Alerts (end) ***
		
		// *** Events (begin) ***
		initializeEventTab(true);	
		// *** Events (end) ***
		
	
    } else {
	   
        // *** Events (begin) ***	    
        initializeEventTab(false);	
        // *** Events (end) ***	    
	    
    }    
}