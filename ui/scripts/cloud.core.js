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

// Version: @VERSION@
var g_mySession = null;
var g_role = null; // roles - root, domain-admin, ro-admin, user
var g_username = null;
var g_account = null;
var g_domainid = null;
var g_enableLogging = false; 
var g_timezoneoffset = null;
var g_timezone = null;

// capabilities
var g_networkType = "vnet"; // vnet, vlan, direct
function getNetworkType() { return g_networkType; }

var g_hypervisorType = "kvm";
function getHypervisorType() { return g_hypervisorType; }

var g_directattachnetworkgroupsenabled = "false";
function getDirectAttachNetworkGroupsEnabled() { return g_directattachnetworkgroupsenabled; }

g_directAttachedUntaggedEnabled = "false"
function getDirectAttachUntaggedEnabled() { return g_directAttachedUntaggedEnabled; }

//keyboard keycode
var keycode_Enter = 13;

//XMLHttpResponse.status
var ERROR_ACCESS_DENIED_DUE_TO_UNAUTHORIZED = 401;
var ERROR_INTERNET_NAME_NOT_RESOLVED = 12007;
var ERROR_INTERNET_CANNOT_CONNECT = 12029;
var ERROR_VMOPS_ACCOUNT_ERROR = 531;

var g_logger = new Logger();
$(function() {
	if(g_enableLogging)
		g_logger.open();
});

// Test Tool.  Please comment this out or remove this when going production.
// This is intended to provide a simple test tool to create user accounts and
// domains.
function initializeTestTool() {
	$("#launch_test").click(function(event) {
		window.open('/client/test');
		return false;
	});
}

// Role Functions
function isAdmin() {
	return (g_role == 1);
}

function isUser() {
	return (g_role == 0);
}

function isDomainAdmin() {
	return (g_role == 2);
}

function setDateField(dateValue, dateField, htmlMarkup) {
    if (dateValue != null && dateValue.length > 0) {
	    var disconnected = new Date();
	    disconnected.setISO8601(dateValue);	
	    var showDate;			
	    if(g_timezoneoffset != null) 
	        showDate = disconnected.getTimePlusTimezoneOffset(g_timezoneoffset);
	    else 
	        showDate = disconnected.format("m/d/Y H:i:s");
	    if(htmlMarkup == null)
	        dateField.text(showDate);
	    else
	        dateField.html(htmlMarkup + showDate);
    }
}

//listItems() function takes care of loading image, pagination
function listItems(submenuContent, commandString, jsonResponse1, jsonResponse2, template, fnJSONToTemplate ) {            
    if(currentPage==1)
        submenuContent.find("#prevPage_div").hide();
    else 
	    submenuContent.find("#prevPage_div").show();     
        
    submenuContent.find("#loading_gridtable").show();     
    submenuContent.find("#pagination_panel").hide();
    	           
    index = 0;	    
    $.ajax({
	    data: commandString,
	    dataType: "json",
	    success: function(json) {			        
	        //IF jsonResponse1=="listaccountsresponse", jsonResponse2=="account", THEN json[jsonResponse1][jsonResponse2] == json.listaccountsresponse.account
		    var items = json[jsonResponse1][jsonResponse2]; 
		    
		    var grid = submenuContent.find("#grid_content").empty();		    	    		
		    if (items != null && items.length > 0) {				        			        
			    for (var i = 0; i < items.length; i++) {
				    var newTemplate = template.clone(true);
				    fnJSONToTemplate(items[i], newTemplate); 
				    grid.append(newTemplate.show());						   
			    }
			    setGridRowsTotal(submenuContent.find("#grid_rows_total"), items.length);
			    if(items.length < pageSize)
			        submenuContent.find("#nextPage_div").hide();
			    else
			        submenuContent.find("#nextPage_div").show();
		    } else {				        
	            setGridRowsTotal(submenuContent.find("#grid_rows_total"), null);
	            submenuContent.find("#nextPage_div").hide();
	        }	
	        submenuContent.find("#loading_gridtable").hide();     
            submenuContent.find("#pagination_panel").show();	      		    						
	    },
		error: function(XMLHttpResponse) {	
		    submenuContent.find("#loading_gridtable").hide();     						
			handleError(XMLHttpResponse, function() {			    
			    if(XMLHttpResponse.status == ERROR_VMOPS_ACCOUNT_ERROR) {
			        submenuContent.find("#grid_content").empty();
			        setGridRowsTotal(submenuContent.find("#grid_rows_total"), null);
	                submenuContent.find("#nextPage_div").hide();	                 
			    }
			    submenuContent.find("#loading_gridtable").hide();     
                submenuContent.find("#pagination_panel").show();	 
			});							
		}
    });
}


//event binder
var currentPage = 1;
var pageSize = 50;  //consistent with server-side
function submenuContentEventBinder(submenuContent, listFunction) {       
    submenuContent.find("#nextPage").bind("click", function(event){	
        event.preventDefault();          
        currentPage++;        
        listFunction(); 
    });		
    
    submenuContent.find("#prevPage").bind("click", function(event){	
        event.preventDefault();           
        currentPage--;	              	    
        listFunction(); 
    });				
		
    submenuContent.find("#refresh").bind("click", function(event){
        event.preventDefault();         
        currentPage=1;       
        listFunction(); 
    });    
        
    submenuContent.find("#search_button").bind("click", function(event) {	       
        event.preventDefault();   
        currentPage = 1;           	        	
        listFunction();                
    });
    
    submenuContent.find("#adv_search_button").bind("click", function(event) {	       
        event.preventDefault();   
        currentPage = 1;           	        	
        listFunction();         
        submenuContent.find("#search_button").data("advanced", false);
	    submenuContent.find("#advanced_search").hide();	
    });
	
	submenuContent.find("#search_input").bind("keypress", function(event) {		        
        if(event.keyCode == keycode_Enter) {           
            event.preventDefault();   		        
	        submenuContent.find("#search_button").click();			     
	    }		    
    });   	    

    submenuContent.find("#advanced_search").bind("keypress", function(event) {		        
        if(event.keyCode == keycode_Enter) {           
            event.preventDefault();   		        
	        submenuContent.find("#adv_search_button").click();			     
	    }		    
    });		   
     
    submenuContent.find("#advanced_search_close").bind("click", function(event) {	    
        event.preventDefault();               
	    submenuContent.find("#search_button").data("advanced", false);	
        submenuContent.find("#advanced_search").hide();
    });	 
        
    submenuContent.find("#advanced_search_link").bind("click", function(event) {	
        event.preventDefault();   
		submenuContent.find("#search_button").data("advanced", true);
		
		var zoneSelect = submenuContent.find("#advanced_search #adv_search_zone");	
		if(zoneSelect.length>0) {
		    var zoneSelect = zoneSelect.empty();			
		    $.ajax({
			    data: "command=listZones&available=true&response=json",
			    dataType: "json",
			    success: function(json) {
				    var zones = json.listzonesresponse.zone;					
				    zoneSelect.append("<option value=''></option>"); 
				    if (zones != null && zones.length > 0) {
				        for (var i = 0; i < zones.length; i++) {
					        zoneSelect.append("<option value='" + zones[i].id + "'>" + sanitizeXSS(zones[i].name) + "</option>"); 
				        }
				    }
			    }
		    });
    		
		    var podSelect = submenuContent.find("#advanced_search #adv_search_pod").empty();		
		    if(podSelect.length>0 && isAdmin()) {		    
		        podSelect.empty();	
		        zoneSelect.bind("change", function(event) {
		            podSelect.empty();
			        var zoneId = $(this).val();
			        if (zoneId.length == 0) return false;
			        $.ajax({
				        data: "command=listPods&zoneId="+zoneId+"&response=json",
				        dataType: "json",
				        async: false,
				        success: function(json) {
					        var pods = json.listpodsresponse.pod;						
					        podSelect.append("<option value=''></option>"); 
					        if (pods != null && pods.length > 0) {
					            for (var i = 0; i < pods.length; i++) {
						            podSelect.append("<option value='" + pods[i].id + "'>" + sanitizeXSS(pods[i].name) + "</option>"); 
					            }
					        }
				        }
			        });
		        });		
		    }
		}
    	
    	var domainSelect = submenuContent.find("#advanced_search #adv_search_domain");	
		if(domainSelect.length>0 && isAdmin()) {
		    var domainSelect = domainSelect.empty();			
		    $.ajax({
			    data: "command=listDomains&available=true&response=json",
			    dataType: "json",
			    success: function(json) {			        
				    var domains = json.listdomainsresponse.domain;			 
				    if (domains != null && domains.length > 0) {
				        for (var i = 0; i < domains.length; i++) {
					        domainSelect.append("<option value='" + domains[i].id + "'>" + sanitizeXSS(domains[i].name) + "</option>"); 
				        }
				    }
			    }
		    });		    
		} 	
    	    	
    	var vmSelect = submenuContent.find("#advanced_search").find("#adv_search_vm");	
		if(vmSelect.length>0) {		   
		    vmSelect.empty();		
		    vmSelect.append("<option value=''></option>"); 	
		    $.ajax({
			    data: "command=listVirtualMachines&response=json",
			    dataType: "json",
			    success: function(json) {			        
				    var items = json.listvirtualmachinesresponse.virtualmachine;		 
				    if (items != null && items.length > 0) {
				        for (var i = 0; i < items.length; i++) {
					        vmSelect.append("<option value='" + items[i].id + "'>" + sanitizeXSS(items[i].name) + "</option>"); 
				        }
				    }
			    }
		    });		    
		} 	
    	
        submenuContent.find("#advanced_search").show();
    });	  
}

// Validation functions
function showError(isValid, field, errMsgField, errMsg) {    
	if(isValid) {
	    errMsgField.text("").hide();
	    field.addClass("text").removeClass("error_text");
	}
	else {
	    errMsgField.text(errMsg).show();
	    field.removeClass("text").addClass("error_text");	
	}
}

function showError2(isValid, field, errMsgField, errMsg, appendErrMsg) {    
	if(isValid) {
	    errMsgField.text("").hide();
	    field.addClass("text2").removeClass("error_text2");
	}
	else {
	    if(appendErrMsg) //append text
	        errMsgField.text(errMsgField.text()+errMsg).show();  
	    else  //reset text
	        errMsgField.text(errMsg).show();  
	    field.removeClass("text2").addClass("error_text2");	
	}
}

function validateDropDownBox(label, field, errMsgField, appendErrMsg) {  
    var isValid = true;
    var errMsg = "";   
    var value = field.val();     
	if (value == null || value.length == 0) {	   
	    errMsg = label + " is a required value. ";	   
		isValid = false;		
	} 		
	showError2(isValid, field, errMsgField, errMsg, appendErrMsg);	
	return isValid;
}

function validateNumber(label, field, errMsgField, min, max, isOptional) {
    var isValid = true;
    var errMsg = "";
    var value = field.val();       
	if (value != null && value.length != 0) {
		if(isNaN(value)) {
			errMsg = label + " must be a number";
			isValid = false;
		} else {
			if (min != null && value < min) {
				errMsg = label + " must be a value greater than or equal to " + min;
				isValid = false;
			}
			if (max != null && value > max) {
				errMsg = label + " must be a value less than or equal to " + max;
				isValid = false;
			}
		}
	} else if(isOptional!=true){  //required field
		errMsg = label + " is a required value. ";
		isValid = false;
	}
	showError(isValid, field, errMsgField, errMsg);	
	return isValid;
}

function validateString(label, field, errMsgField, isOptional) {  
    var isValid = true;
    var errMsg = "";
    var value = field.val();     
	if (isOptional!=true && (value == null || value.length == 0)) {	 //required field   
	    errMsg = label + " is a required value. ";	   
		isValid = false;		
	} 	
	else if (value!=null && value.length >= 255) {	    
	    errMsg = label + " must be less than 255 characters";	   
		isValid = false;		
	} 	
	else if(value!=null && value.indexOf('"')!=-1) {
	    errMsg = "Double quotes are not allowed.";	   
		isValid = false;	
	}
	showError(isValid, field, errMsgField, errMsg);	
	return isValid;
}

function validateIp(label, field, errMsgField, isOptional) {  
    if(validateString(label, field, errMsgField, isOptional) == false)
        return;
    var isValid = true;
    var errMsg = "";
    var value = field.val();     		    
    if(value!=null && value.length>0) {
        myregexp = /^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/;	   
        var isMatch = myregexp.test(value);
        if(!isMatch) {
            errMsg = label + " should be like 75.52.126.11";	   
	        isValid = false;		
	    }
	}	 	
	showError(isValid, field, errMsgField, errMsg);	
	return isValid;
}

function validateCIDR(label, field, errMsgField, isOptional) {  
    if(validateString(label, field, errMsgField, isOptional) == false)
        return;        
    var isValid = true;
    var errMsg = "";
    var value = field.val();     
    if(value!=null && value.length>0) {
        myregexp = /^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\/\d{1,2}$/;	   
        var isMatch = myregexp.test(value);
        if(!isMatch) {
            errMsg = label + " should be like 10.1.1.0/24";	   
	        isValid = false;		
	    }
	}	
	showError(isValid, field, errMsgField, errMsg);	
	return isValid;
}

function validatePath(label, field, errMsgField, isOptional) {  
    if(validateString(label, field, errMsgField, isOptional) == false)
        return;
    var isValid = true;
    var errMsg = "";
    var value = field.val();    
    if(value!=null && value.length>0) {
        myregexp = /^\//;	   
        var isMatch = myregexp.test(value);
        if(!isMatch) {
            errMsg = label + " should be like /aaa/bbb/ccc";	   
	        isValid = false;		
	    }
	}	 	
	showError(isValid, field, errMsgField, errMsg);	
	return isValid;
}

function cleanErrMsg(field, errMsgField) {
    showError(true, field, errMsgField);
}	

// setter 
function setGridRowsTotal(field, gridRowsTotal) {   
    if(gridRowsTotal==null) {
        field.text("");
        return;
    }
 
    if(gridRowsTotal==1)
	    field.text(gridRowsTotal + " item");
	else
	    field.text(gridRowsTotal + " items");
} 

function changeGridRowsTotal(field, difference) {   
    var t = field.text();
    var oldTotal = 0;
    if(t.length>0 && t.indexOf(" item")!=-1) {      
        var s = t.substring(0, t.indexOf(" item"));
        if(!isNaN(s))
            oldTotal = parseInt(s);
    }
    var newTotal = oldTotal + difference;
    setGridRowsTotal(field, newTotal);
}


// others
function trim(val) {
    if(val == null)
        return null;
    return val.replace(/^\s*/, "").replace(/\s*$/, "");
}

// Prevent cross-site-script(XSS) attack. 
// used right before adding user input to the DOM tree. e.g. DOM_element.html(sanitizeXSS(user_input));  
function sanitizeXSS(val) {     
    if(val == null)
        return val; 
    val = val.replace(/</g, "&lt;");  //replace < whose unicode is \u003c     
    val = val.replace(/>/g, "&gt;");  //replace > whose unicode is \u003e  
    return val;
}

function getVmName(p_vmName, p_vmDisplayname) {
    if(p_vmDisplayname == null)
        return sanitizeXSS(p_vmName);
    var vmName = null;
	if (isAdmin()) {
		if (p_vmDisplayname != p_vmName) {
			vmName = p_vmName + "(" + sanitizeXSS(p_vmDisplayname) + ")";
		} else {
			vmName = p_vmName;
		}
	} else {
		vmName = sanitizeXSS(p_vmDisplayname);
	}
	return vmName;
}

// FUNCTION: Handles AJAX error callbacks.  You can pass in an optional function to 
// handle errors that are not already handled by this method.  
function handleError(xmlHttp, handleErrorCallback) {
	// User Not authenticated
	if (xmlHttp.status == ERROR_ACCESS_DENIED_DUE_TO_UNAUTHORIZED) {
		$("#dialog_session_expired").dialog("open");
	} 	
	else if (xmlHttp.status == ERROR_INTERNET_NAME_NOT_RESOLVED) {
		$("#dialog_error").text("Internet name can not be resolved").dialog("open");
	} 
	else if (xmlHttp.status == ERROR_INTERNET_CANNOT_CONNECT) {
		$("#dialog_error").text("Management server is not accessible").dialog("open");
	} 
	else if (xmlHttp.status == ERROR_VMOPS_ACCOUNT_ERROR && handleErrorCallback != undefined) {
		handleErrorCallback();
	} 
	else if (handleErrorCallback != undefined) {
		handleErrorCallback();
	}
	else {	   
		var start = xmlHttp.responseText.indexOf("h1") + 3;
		var end = xmlHttp.responseText.indexOf("</h1");
		var errorMsg = xmlHttp.responseText.substring(start, end);		
		$("#dialog_error").html("<p><b>Encountered an error:</b></p><br/><p>"+sanitizeXSS(errorMsg)+"</p>").dialog("open");
	}
}

// FUNCTION: Adds a Dialog to the list of active Dialogs so that
// when you shift from one tab to another, we clean out the dialogs
var activeDialogs = new Array();
function activateDialog(dialog) {
	activeDialogs[activeDialogs.length] = dialog;
	
	//bind Enter-Key-pressing event handler to the dialog 	
	dialog.keypress(function(event) {
	    if(event.keyCode == keycode_Enter) 	        
	        $('[aria-labelledby$='+dialog.attr("id")+']').find(":button:first").click();	    
	});
}
function removeDialogs() {
	for (var i = 0; i < activeDialogs.length; i++) {
		activeDialogs[i].remove();
	}
	activeDialogs = new Array();
}

function convertBytes(bytes) {
	if (bytes < 1024 * 1024) {
		return (bytes / 1024).toFixed(2) + " KB";
	} else if (bytes < 1024 * 1024 * 1024) {
		return (bytes / 1024 / 1024).toFixed(2) + " MB";
	} else if (bytes < 1024 * 1024 * 1024 * 1024) {
		return (bytes / 1024 / 1024 / 1024).toFixed(2) + " GB";
	} else {
		return (bytes / 1024 / 1024 / 1024 / 1024).toFixed(2) + " TB";
	}
}

function convertHz(hz) {
	if (hz < 1000) {
		return hz + " MHZ";
	} else {
		return (hz / 1000).toFixed(2) + " GHZ";
	} 
}

function toDayOfMonthDesp(dayOfMonth) {
    return "Day "+dayOfMonth +" of Month";
}

function toDayOfWeekDesp(dayOfWeek) {
    if (dayOfWeek == "1")
        return "Sunday";
    else if (dayOfWeek == "2")
        return "Monday";
    else if (dayOfWeek == "3")
        return "Tuesday";
    else if (dayOfWeek == "4")
        return "Wednesday";
    else if (dayOfWeek == "5")
        return "Thursday"
    else if (dayOfWeek == "6")
        return "Friday";
    else if (dayOfWeek == "7")
        return "Saturday";    
}

function toBooleanText(booleanValue) {
    if(booleanValue == "true")
        return "Yes";
    else if(booleanValue == "false")
        return "No";
}

function toBooleanValue(booleanText) {
    if(booleanText == "Yes")
        return "true";
    else if(booleanText == "No")
        return "false";
}

var roleTypeUser = "0";
var roleTypeAdmin = "1";
var roleTypeDomainAdmin = "2";
function toRole(type) {
	if (type == roleTypeUser) {
		return "User";
	} else if (type == roleTypeAdmin) {
		return "Admin";
	} else if (type == roleTypeDomainAdmin) {
		return "Domain-Admin";
	}
}

function toAlertType(alertCode) {
	switch (alertCode) {
		case "0" : return "Capacity Threshold - Memory";
		case "1" : return "Capacity Threshold - CPU";
		case "2" : return "Capacity Threshold - Storage Used";
		case "3" : return "Capacity Threshold - Storage Allocated";
		case "4" : return "Capacity Threshold - Public IP";
		case "5" : return "Capacity Threshold - Private IP";
		case "6" : return "Monitoring - Host";
		case "7" : return "Monitoring - VM";
		case "8" : return "Monitoring - Domain Router";
		case "9" : return "Monitoring - Console Proxy";
		case "10" : return "Monitoring - Routing Host";
		case "11" : return "Monitoring - Storage";
		case "12" : return "Monitoring - Usage Server";
		case "13" : return "Monitoring - Management Server";
		case "14" : return "Migration - Domain Router";
		case "15" : return "Migration - Console Proxy";
		case "16" : return "Migration - User VM";
		case "17" : return "VLAN";
		case "18" : return "Monitoring - Secondary Storage VM";
	}
}

// Timezones
var timezones = new Object();
timezones['Etc/GMT+12']='[UTC-12:00] GMT-12:00';
timezones['Etc/GMT+11']='[UTC-11:00] GMT-11:00';
timezones['Pacific/Samoa']='[UTC-11:00] Samoa Standard Time';
timezones['Pacific/Honolulu']='[UTC-10:00] Hawaii Standard Time';
timezones['US/Alaska']='[UTC-09:00] Alaska Standard Time';
timezones['America/Los_Angeles']='[UTC-08:00] Pacific Standard Time';
timezones['Mexico/BajaNorte']='[UTC-08:00] Baja California';
timezones['US/Arizona']='[UTC-07:00] Arizona';
timezones['US/Mountain']='[UTC-07:00] Mountain Standard Time';
timezones['America/Chihuahua']='[UTC-07:00] Chihuahua, La Paz';
timezones['America/Chicago']='[UTC-06:00] Central Standard Time';
timezones['America/Costa_Rica']='[UTC-06:00] Central America';
timezones['America/Mexico_City']='[UTC-06:00] Mexico City, Monterrey';
timezones['Canada/Saskatchewan']='[UTC-06:00] Saskatchewan';
timezones['America/Bogota']='[UTC-05:00] Bogota, Lima';
timezones['America/New_York']='[UTC-05:00] Eastern Standard Time';
timezones['America/Caracas']='[UTC-04:00] Venezuela Time';
timezones['America/Asuncion']='[UTC-04:00] Paraguay Time';
timezones['America/Cuiaba']='[UTC-04:00] Amazon Time';
timezones['America/Halifax']='[UTC-04:00] Atlantic Standard Time';
timezones['America/La_Paz']='[UTC-04:00] Bolivia Time';
timezones['America/Santiago']='[UTC-04:00] Chile Time';
timezones['America/St_Johns']='[UTC-03:30] Newfoundland Standard Time';
timezones['America/Araguaina']='[UTC-03:00] Brasilia Time';
timezones['America/Argentina/Buenos_Aires']='[UTC-03:00] Argentine Time';
timezones['America/Cayenne']='[UTC-03:00] French Guiana Time';
timezones['America/Godthab']='[UTC-03:00] Greenland Time';
timezones['America/Montevideo']='[UTC-03:00] Uruguay Time]';
timezones['Etc/GMT+2']='[UTC-02:00] GMT-02:00';
timezones['Atlantic/Azores']='[UTC-01:00] Azores Time';
timezones['Atlantic/Cape_Verde']='[UTC-01:00] Cape Verde Time';
timezones['Africa/Casablanca']='[UTC] Casablanca';
timezones['Etc/UTC']='[UTC] Coordinated Universal Time';
timezones['Atlantic/Reykjavik']='[UTC] Reykjavik';
timezones['Europe/London']='[UTC] Western European Time';
timezones['CET']='[UTC+01:00] Central European Time';
timezones['Europe/Bucharest']='[UTC+02:00] Eastern European Time';
timezones['Africa/Johannesburg']='[UTC+02:00] South Africa Standard Time';
timezones['Asia/Beirut']='[UTC+02:00] Beirut';
timezones['Africa/Cairo']='[UTC+02:00] Cairo';
timezones['Asia/Jerusalem']='[UTC+02:00] Israel Standard Time';
timezones['Europe/Minsk']='[UTC+02:00] Minsk';
timezones['Europe/Moscow']='[UTC+03:00] Moscow Standard Time';
timezones['Africa/Nairobi']='[UTC+03:00] Eastern African Time';
timezones['Asia/Karachi']='[UTC+05:00] Pakistan Time';
timezones['Asia/Kolkata']='[UTC+05:30] India Standard Time';
timezones['Asia/Bangkok']='[UTC+05:30] Indochina Time';
timezones['Asia/Shanghai']='[UTC+08:00] China Standard Time';
timezones['Asia/Kuala_Lumpur']='[UTC+08:00] Malaysia Time';
timezones['Australia/Perth']='[UTC+08:00] Western Standard Time (Australia)';
timezones['Asia/Taipei']='[UTC+08:00] Taiwan';
timezones['Asia/Tokyo']='[UTC+09:00] Japan Standard Time';
timezones['Asia/Seoul']='[UTC+09:00] Korea Standard Time';
timezones['Australia/Adelaide']='[UTC+09:30] Central Standard Time (South Australia)';
timezones['Australia/Darwin']='[UTC+09:30] Central Standard Time (Northern Territory)';
timezones['Australia/Brisbane']='[UTC+10:00] Eastern Standard Time (Queensland)';
timezones['Australia/Canberra']='[UTC+10:00] Eastern Standard Time (New South Wales)';
timezones['Pacific/Guam']='[UTC+10:00] Chamorro Standard Time';
timezones['Pacific/Auckland']='[UTC+12:00] New Zealand Standard Time';

$(document).ready(function() {
	// Prevent the UI from being iframed if the iframe isn't from the same domain.
	try {
		if ( top != self && self.location.hostname != top.location.hostname) {
			// leaving the code here in the oft change an older browser is being used that does not have
			// cross-site scripting prevention.
			alert("Detected a frame (" + top.location.hostname + ") not from the same domain (" + self.location.hostname + ").  Moving app to top of browser to prevent any security tampering.");
			top.location.href = window.location.href;
		}
	} catch (err) {
		// This means the domains are different because the browser is preventing access to the parent's domain.
		alert("Detected a frame not from the same domain (" + self.location.hostname + ").  Moving app to top of browser to prevent any security tampering.");
		top.location.href = window.location.href;
	}

	// We don't support IE6 at the moment, so let's just inform customers it won't work
	var IE6 = false /*@cc_on || @_jscript_version < 5.7 @*/;
	var gteIE7 = false /*@cc_on || @_jscript_version >= 5.7 @*/;

	// Disable IE6 browsers as UI does not support it
	if (IE6 == true) {
		alert("Only IE7, IE8, FireFox 3.x, Chrome, and Safari browsers are supported at this time.");
		return;
	}
	
	initializeTestTool();
	
	// We will be dropping all the main tab content into this container
	mainContainer = $("#maincontentarea");

	// Tab Links, dashboard is the initial active tab
	mainContainer.load("content/tab_dashboard.html");
	
	// Default AJAX Setup
	$.ajaxSetup({
		url: "/client/api",
		dataType: "json",
		cache: false,
		error: function(XMLHttpResponse) {
			handleError(XMLHttpResponse);
		},
		beforeSend: function(XMLHttpRequest) {
			if (g_mySession == $.cookie("JSESSIONID")) {
				return true;
			} else {
				$("#dialog_session_expired").dialog("open");
				return false;
			}
		}		
	});
	
	// LOGIN/LOGOUT
	// 'Enter' Key in any login form element = Submit click
	$("#logoutpage #loginForm").keypress(function(event) {
		var formId = $(event.target).attr("id");
		if(event.keyCode == keycode_Enter && formId != "loginbutton") {
			login();
		}
	});
	
	$("#logoutpage .loginbutton").bind("click", function(event) {
		login();
		return false;
	});
	
	$("#logoutaccount_link").bind("click", function(event) {
		$.ajax({
			data: "command=logout&response=json",
			dataType: "json",
			success: function(json) {
				logout(true);
			},
			error: function() {
				logout(true);
			},
			beforeSend : function(XMLHTTP) {
				return true;
			}
		});
	});
	
	// FUNCTION: logs the user out
	var activeTab = null;
	function logout(refresh) {
		g_mySession = null;
		g_username = null;	
		g_account = null;
		g_domainid = null;	
		g_timezoneoffset = null;
		g_timezone = null;
		
		$.cookie('JSESSIONID', null);
		$.cookie('username', null);
		$.cookie('account', null);
		$.cookie('domainid', null);
		$.cookie('role', null);
		$.cookie('networktype', null); 
		$.cookie('timezoneoffset', null);
		$.cookie('timezone', null);
		
		$("body").stopTime();
		
		// default is to redisplay the login page
		if (onLogoutCallback()) {
			if (refresh) {
				location.replace('/client');
				return false;
			}
			$("#account_password").val("");
			$(".loginbutton_box p").hide();
			$("#logoutpage").show();
			$("body").css("background", "#4e4e4e url(images/logout_bg.gif) repeat-x top left");
			mainContainer.empty();
			$("#mainmaster").hide();
			$("#overlay_black").hide();
			
			var menuOnClass = "menutab_on";
			var menuOffClass = "menutab_off";
			var tab = null;
			if (isAdmin()) {
				tab = $("#menutab_dashboard_root");
				menuOnClass = "admin_menutab_on";
				menuOffClass = "admin_menutab_off";
			} else if (isDomainAdmin()) {
				tab = $("#menutab_dashboard_domain");
				menuOnClass = "admin_menutab_on";
				menuOffClass = "admin_menutab_off";
			} else if (isUser()) {
				tab = $("#menutab_dashboard_user");
				menuOnClass = "menutab_on";
				menuOffClass = "menutab_off";
			}
			if (activeTab != null) {
				activeTab.removeClass(menuOnClass).addClass(menuOffClass);
				activeTab = null;
			}
			if (tab != null) {
				tab.removeClass(menuOffClass).addClass(menuOnClass);
			}
			g_role = null;
			$("#account_username").focus();
		}
	}
	
	// FUNCTION: logs the user in
	function login() {
		var array1 = [];
		var username = encodeURIComponent($("#account_username").val());
		array1.push("&username="+username);
		
		var password = $.md5(encodeURIComponent($("#account_password").val()));
		array1.push("&password="+password);
		
		var domain = encodeURIComponent($("#account_domain").val());
		if(domain != null && domain.length > 0)
		    array1.push("&domain="+domain);
		
		$.ajax({
			type: "POST",
			data: "command=login&response=json" + array1.join(""),
			dataType: "json",
			async: false,
			success: function(json) {
				g_mySession = $.cookie('JSESSIONID');
				g_role = json.loginresponse.type;
				g_username = json.loginresponse.username;	
				g_account = json.loginresponse.account;
				g_domainid = json.loginresponse.domainid;	
				g_timezone = json.loginresponse.timezone;								
				g_timezoneoffset = json.loginresponse.timezoneoffset;					
				if (json.loginresponse.networktype != null) 
					g_networkType = json.loginresponse.networktype;				
				if (json.loginresponse.hypervisortype != null) 
					g_hypervisorType = json.loginresponse.hypervisortype;				
				if (json.loginresponse.directattachnetworkgroupsenabled != null) 
					g_directattachnetworkgroupsenabled = json.loginresponse.directattachnetworkgroupsenabled;
				if (json.loginresponse.directattacheduntaggedenabled != null) 
					g_directAttachedUntaggedEnabled = json.loginresponse.directattacheduntaggedenabled;

				$.cookie('networktype', g_networkType, { expires: 1});
				$.cookie('hypervisortype', g_hypervisorType, { expires: 1});
				$.cookie('username', g_username, { expires: 1});	
				$.cookie('account', g_account, { expires: 1});	
				$.cookie('domainid', g_domainid, { expires: 1});				
				$.cookie('role', g_role, { expires: 1});
				$.cookie('timezoneoffset', g_timezoneoffset, { expires: 1});  
				$.cookie('timezone', g_timezone, { expires: 1});  
				$.cookie('directattachnetworkgroupsenabled', g_directattachnetworkgroupsenabled, { expires: 1}); 
				$.cookie('directattacheduntaggedenabled', g_directAttachedUntaggedEnabled, { expires: 1}); 
				
				// Set Role
				if (isUser()) {
					$(".loginbutton_box p").text("").hide();			
					$("#menutab_role_user #menutab_dashboard_user").click();
				} else if (isAdmin()) {
					$(".loginbutton_box p").text("").hide();			
					$("#menutab_role_root #menutab_dashboard_root").click();
				} else if (isDomainAdmin()) {
					$(".loginbutton_box p").text("").hide();			
					$("#menutab_role_domain #menutab_dashboard_domain").click();
				} else {
				    $(".loginbutton_box p").text("Account type of '" + username + "' is neither user nor admin.").show();
				    return;
				}				
				
				$("#logoutpage").hide();
				$("body").css("background", "#FFF repeat top left");
				$("#mainmaster").show();	
			},
			error: function() {
				$("#account_password").val("");
				$("#logoutpage").show();				
				$(".loginbutton_box p").text("Your username/password does not match our records.").show();
				$("#account_username").focus();
			},
			beforeSend: function(XMLHttpRequest) {
				return true;
			}
		});
	}
	
	// Dialogs
	$("#dialog_confirmation").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000
	});
	
	$("#dialog_info").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000,
		buttons: { "OK": function() { $(this).dialog("close"); } }
	});
	
	$("#dialog_alert").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000,
		buttons: { "OK": function() { $(this).dialog("close"); } }
	});
	$("#dialog_alert").siblings(".ui-widget-header").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	$("#dialog_alert").siblings(".ui-dialog-buttonpane").find(".ui-state-default").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	
	$("#dialog_error").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000,
		buttons: { "Close": function() { $(this).dialog("close"); } }
	});
	$("#dialog_error").siblings(".ui-widget-header").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	$("#dialog_error").siblings(".ui-dialog-buttonpane").find(".ui-state-default").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	
	$("#dialog_session_expired").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000,
		buttons: { "OK": function() { logout(true); $(this).dialog("close"); } }
	});
	$("#dialog_session_expired").siblings(".ui-widget-header").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	$("#dialog_session_expired").siblings(".ui-dialog-buttonpane").find(".ui-state-default").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	
	$("#dialog_server_error").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000,
		buttons: { "OK": function() { $(this).dialog("close"); } }
	});
	$("#dialog_server_error").siblings(".ui-widget-header").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	$("#dialog_server_error").siblings(".ui-dialog-buttonpane").find(".ui-state-default").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	
	// Menu Tabs
	$("#global_nav").bind("click", function(event) {
		var tab = $(event.target);
		var tabId = tab.attr("id");
		var menuOnClass = "menutab_on";
		var menuOffClass = "menutab_off";
		if (tabId == "menutab_dashboard_user" || tabId == "menutab_dashboard_root" || tabId == "menutab_dashboard_domain") {
			showDashboardTab();
		} else if (tabId == "menutab_vm") {
		    mainContainer.load("content/tab_instances.html", function() {
			    showInstancesTab(tab.data("domainId"), tab.data("account"));
			});		
		} else if (tabId == "menutab_networking") {
		    mainContainer.load("content/tab_networking.html", function() {		
			    showNetworkingTab(tab.data("domainId"), tab.data("account"));
			});
		} else if (tabId == "menutab_templates") {
		    mainContainer.load("content/tab_templates.html", function() {
			    showTemplatesTab();
			});
		} else if (tabId == "menutab_events") {
		    mainContainer.load("content/tab_events.html", function() {			   
			    showEventsTab(tab.data("showEvents"));
			});
		} else if (tabId == "menutab_hosts") {
		    mainContainer.load("content/tab_hosts.html", function() {	
			    showHostsTab();
			});
	    } else if (tabId == "menutab_storage") {
	        mainContainer.load("content/tab_storage.html", function() {		
			    showStorageTab(tab.data("domainId"), tab.data("targetTab"));
			});
		} else if (tabId == "menutab_accounts") {
		    mainContainer.load("content/tab_accounts.html", function() {	
			    showAccountsTab(tab.data("domainId"));
			});
		} else if (tabId == "menutab_domain") {
		    mainContainer.load("content/tab_domains.html", function() {	   
			    showDomainsTab();
			});
		} else if (tabId == "menutab_configuration") {
		    mainContainer.load("content/tab_configuration.html", function() {
			    showConfigurationTab();
			});
		} else {
			return false;
		}
		
		if (isAdmin() || isDomainAdmin()) {
			menuOnClass = "admin_menutab_on";
			menuOffClass = "admin_menutab_off";
		} else if (isUser()) {
			menuOnClass = "menutab_on";
			menuOffClass = "menutab_off";
		}
		if (activeTab != null) {
			activeTab.removeClass(menuOnClass).addClass(menuOffClass); 
		}
		tab.removeClass(menuOffClass).addClass(menuOnClass);
		activeTab = tab;
		removeDialogs();
		return false;
	});
	
	// Dashboard Tab
	function showDashboardTab() {
		mainContainer.load("content/tab_dashboard.html", function() {
			$(".header_topright #header_username").text($.cookie("username"));
			
			if (isAdmin()) {
				var sessionExpired = false;
				var zones = null;
				var noZones = false;
				var noPods = true;
				$("#menutab_dashboard_root, #menutab_vm, #menutab_networking_old, #menutab_networking, #menutab_templates, #menutab_events, #menutab_hosts, #menutab_storage, #menutab_accounts, #menutab_domain").hide();							
				$.ajax({
					data: "command=listZones&available=true&response=json",
					dataType: "json",
					async: false,
					success: function(json) {
						zones = json.listzonesresponse.zone;
						var zoneSelect = $("#capacity_zone_select").empty();	
						if (zones != null && zones.length > 0) {
							for (var i = 0; i < zones.length; i++) {
								zoneSelect.append("<option value='" + zones[i].id + "'>" + sanitizeXSS(zones[i].name) + "</option>"); 								
								if(noPods) {
								    $.ajax({
						                data: "command=listPods&zoneId="+zones[i].id+"&response=json",
						                dataType: "json",
						                async: false,
						                success: function(json) {
							                var pods = json.listpodsresponse.pod;						
							                if (pods != null && pods.length > 0) {
            							        noPods = false;
            							        $("#menutab_dashboard_root, #menutab_vm, #menutab_networking_old, #menutab_networking, #menutab_templates, #menutab_events, #menutab_hosts, #menutab_storage, #menutab_accounts, #menutab_domain").show();							
							                }							
						                }
					                });
								}
							}
						} else {							
							noZones = true;
						}
					},					
					beforeSend: function(XMLHttpRequest) {
						return true;
					}	
				});
				if (sessionExpired) return false;
				if (noZones || noPods) {
					$("#tab_dashboard_user").hide();
					$("#menutab_role_user").hide();
					$("#menutab_role_root").show();
					$("#menutab_configuration").click();
					return false;
				}
				
				var capacities = null;
				$.ajax({
					cache: false,
					async: false,
					data: "command=listCapacity&response=json",
					dataType: "json",
					success: function(json) {
						capacities = json.listcapacityresponse.capacity;
					}
				});
				
				$("#capacity_pod_select").bind("change", function(event) {
					// Reset to Defaults
					$("#public_ip_total, #storage_total, #storage_alloc_total, #sec_storage_total, #memory_total, #cpu_total, #private_ip_total").text("N/A");
					$("#public_ip_used, #storage_used, #storage_alloc, #sec_storage_used, #memory_used, #cpu_used, #private_ip_used,").attr("style", "width:50%").text("N/A");
					$(".db_bargraph_barbox_safezone").attr("style", "width:0%");
					$(".db_bargraph_barbox_unsafezone").attr("style", "width:0%");
					
					var selectedZone = $("#capacity_zone_select option:selected").text();
					var selectedPod = $("#capacity_pod_select").val();
					
					if (capacities != null && capacities.length > 0) {
						for (var i = 0; i < capacities.length; i++) {
							var capacity = capacities[i];
							if (capacity.zonename == selectedZone) {
								// Public IPs
								if (capacity.type == "4") {
									$("#public_ip_used").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + capacity.capacityused + " / " + capacity.percentused + "%");
									$("#public_ip_total").text("Total: " + capacity.capacitytotal);
									var usedPercentage = parseInt(capacity.percentused);
									if (usedPercentage > 70) {
										$("#capacity_public_ip .db_bargraph_barbox_safezone").attr("style", "width:70%");
										if(usedPercentage <= 100) 										
										    $("#capacity_public_ip .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
										else 
										    $("#capacity_public_ip .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
									} else {
										$("#capacity_public_ip .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
									    $("#capacity_public_ip .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
									}								
								// Secondary Storage
								} else if (capacity.type == "6") {
									$("#sec_storage_used").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + convertBytes(parseInt(capacity.capacityused)) + " / " + capacity.percentused + "%");
									$("#sec_storage_total").text("Total: " + convertBytes(parseInt(capacity.capacitytotal)));
									var usedPercentage = parseInt(capacity.percentused);
									if (usedPercentage > 70) {
										$("#capacity_sec_storage .db_bargraph_barbox_safezone").attr("style", "width:70%");
										if(usedPercentage <= 100) 
										    $("#capacity_sec_storage .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
										else
										    $("#capacity_sec_storage .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
									} else {
										$("#capacity_sec_storage .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
									    $("#capacity_sec_storage .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
									}
								} else {
									if (capacity.podname == selectedPod) {
										// Memory
										if (capacity.type == "0") {
											$("#memory_used").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + convertBytes(parseInt(capacity.capacityused)) + " / " + capacity.percentused + "%");
											$("#memory_total").text("Total: " + convertBytes(parseInt(capacity.capacitytotal)));
											var usedPercentage = parseInt(capacity.percentused);
											if (usedPercentage > 70) {
												$("#capacity_memory .db_bargraph_barbox_safezone").attr("style", "width:70%");
												if(usedPercentage <= 100) 
												    $("#capacity_memory .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
												else
												    $("#capacity_memory .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
											} else {
												$("#capacity_memory .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
											    $("#capacity_memory .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
											}
										// CPU
										} else if (capacity.type == "1") {
											$("#cpu_used").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + convertHz(parseInt(capacity.capacityused)) + " / " + capacity.percentused + "%");
											$("#cpu_total").text("Total: " + convertHz(parseInt(capacity.capacitytotal)));
											var usedPercentage = parseInt(capacity.percentused);
											if (usedPercentage > 70) {
												$("#capacity_cpu .db_bargraph_barbox_safezone").attr("style", "width:70%");
												if(usedPercentage <= 100) 
												    $("#capacity_cpu .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
												else
												    $("#capacity_cpu .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
											} else {
												$("#capacity_cpu .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
											    $("#capacity_cpu .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
											}
										// Storage Used
										} else if (capacity.type == "2") {
											$("#storage_used").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + convertBytes(parseInt(capacity.capacityused)) + " / " + capacity.percentused + "%");
											$("#storage_total").text("Total: " + convertBytes(parseInt(capacity.capacitytotal)));
											var usedPercentage = parseInt(capacity.percentused);									
											if (usedPercentage > 70) {
												$("#capacity_storage .db_bargraph_barbox_safezone").attr("style", "width:70%");
												if(usedPercentage <= 100) 
												    $("#capacity_storage .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
												else
												    $("#capacity_storage .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
											} else {
												$("#capacity_storage .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
											    $("#capacity_storage .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
											}
										// Storage Allocated
										} else if (capacity.type == "3") {
											$("#storage_alloc").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + convertBytes(parseInt(capacity.capacityused)) + " / " + capacity.percentused + "%");
											$("#storage_alloc_total").text("Total: " + convertBytes(parseInt(capacity.capacitytotal)));
											var usedPercentage = parseInt(capacity.percentused);
											if (usedPercentage > 70) {
												$("#capacity_storage_alloc .db_bargraph_barbox_safezone").attr("style", "width:70%");
												if(usedPercentage <= 100) 
												    $("#capacity_storage_alloc .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
												else
												    $("#capacity_storage_alloc .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
											} else {
												$("#capacity_storage_alloc .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
											    $("#capacity_storage_alloc .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
											}
										// Private IPs
										} else if (capacity.type == "5") {
											$("#private_ip_used").attr("style", "width: " + ((parseFloat(capacity.percentused) < 50) ? "50%" : capacity.percentused + "%")).text("Used: " + capacity.capacityused + " / " + capacity.percentused + "%");
											$("#private_ip_total").text("Total: " + capacity.capacitytotal);
											var usedPercentage = parseInt(capacity.percentused);
											if (usedPercentage > 70) {
												$("#capacity_private_ip .db_bargraph_barbox_safezone").attr("style", "width:70%");
												if(usedPercentage <= 100) 
												    $("#capacity_private_ip .db_bargraph_barbox_unsafezone").attr("style", "width:"+(usedPercentage - 70)+"%");
												else
												    $("#capacity_private_ip .db_bargraph_barbox_unsafezone").attr("style", "width:30%");
											} else {
												$("#capacity_private_ip .db_bargraph_barbox_safezone").attr("style", "width:"+usedPercentage+"%");
											    $("#capacity_private_ip .db_bargraph_barbox_unsafezone").attr("style", "width:0%");
											}
										}
									}
								}
							}
						}
					}
				});
				
				$("#capacity_zone_select").bind("change", function(event) {
					var zoneId = $(this).val();
					$.ajax({
						data: "command=listPods&zoneId="+zoneId+"&response=json",
						dataType: "json",
						async: false,
						success: function(json) {
							var pods = json.listpodsresponse.pod;
							var podSelect = $("#capacity_pod_select").empty();	
							if (pods != null && pods.length > 0) {
								podSelect.append("<option value='All'>All</option>"); 
							    for (var i = 0; i < pods.length; i++) {
								    podSelect.append("<option value='" + pods[i].name + "'>" + sanitizeXSS(pods[i].name) + "</option>"); 
							    }
							}
							$("#capacity_pod_select").change();
						}
					});
				});
				$("#capacity_zone_select").change();
				
				// Show Recent Alerts
				$.ajax({
					data: "command=listAlerts&response=json",
					dataType: "json",
					success: function(json) {
						var alerts = json.listalertsresponse.alert;
						if (alerts != null && alerts.length > 0) {
							var alertGrid = $("#alert_grid_content").empty();
							var length = (alerts.length>=5) ? 5 : alerts.length;
							for (var i = 0; i < length; i++) {
								var errorTemplate = $("#recent_error_template").clone(true);
								errorTemplate.find("#db_error_type").text(toAlertType(alerts[i].type));
								errorTemplate.find("#db_error_msg").append(sanitizeXSS(alerts[i].description));											
								setDateField(alerts[i].sent, errorTemplate.find("#db_error_date"));															
								alertGrid.append(errorTemplate.show());
							}
						}
					}
				});
				
				// Show Host Alerts
				$.ajax({
					data: "command=listHosts&state=Alert&response=json",
					dataType: "json",
					success: function(json) {
						var alerts = json.listhostsresponse.host;
						if (alerts != null && alerts.length > 0) {
							var alertGrid = $("#host_alert_grid_content").empty();
							var length = (alerts.length>=4) ? 4 : alerts.length;
							for (var i = 0; i < length; i++) {
								var errorTemplate = $("#recent_error_template").clone(true);
								errorTemplate.find("#db_error_type").text("Host - Alert State");
								errorTemplate.find("#db_error_msg").append("Host - <b>" + sanitizeXSS(alerts[i].name) + "</b> has been detected in Alert state.");								
								setDateField(alerts[i].disconnected, errorTemplate.find("#db_error_date"));											
								alertGrid.append(errorTemplate.show());
							}
						}
					}
				});
				
				$("#alert_more").bind("click", function(event) {
					event.preventDefault();
					
					$("#menutab_role_root #menutab_events").data("showEvents", false).click();
				});
				$("#host_alert_more").bind("click", function(event) {
					event.preventDefault();
					$("#menutab_hosts").click();
				});
				
				$("#tab_dashboard_user, #tab_dashboard_domain, #loading_gridtable").hide();
				$("#tab_dashboard_root").show();
				$("#menutab_role_user").hide();
				$("#menutab_role_root").show();
				$("#menutab_role_domain").hide();
				$("#launch_test").show();
			} else if (isDomainAdmin()) {
				var thisTab = $("#tab_dashboard_domain");
				$("#tab_dashboard_user, #tab_dashboard_root, #loading_gridtable").hide();
				thisTab.show();
				$("#menutab_role_user").hide();
				$("#menutab_role_root").hide();
				$("#menutab_role_domain").show();
				$("#launch_test").hide();
				
				// Need to use/create better API for this as there is a limit of pageSize
				// to list count.
				$.ajax({
					data: "command=listVirtualMachines&pageSize=500&response=json",
					dataType: "json",
					success: function(json) {
						if (json.listvirtualmachinesresponse.virtualmachine != undefined)
							thisTab.find("#dashboard_instances").text(json.listvirtualmachinesresponse.virtualmachine.length);
					}
				});
				$.ajax({
					data: "command=listVolumes&pageSize=500&response=json",
					dataType: "json",
					success: function(json) {
						if (json.listvolumesresponse.volume)
							thisTab.find("#dashboard_volumes").text(json.listvolumesresponse.volume.length);
					}
				});
				$.ajax({
					data: "command=listSnapshots&pageSize=500&response=json",
					dataType: "json",
					success: function(json) {
						if (json.listsnapshotsresponse.snapshot)
							thisTab.find("#dashboard_snapshots").text(json.listsnapshotsresponse.snapshot.length);
					}
				});
				$.ajax({
					data: "command=listAccounts&pageSize=500&response=json",
					dataType: "json",
					success: function(json) {
						if (json.listaccountsresponse.account)
							thisTab.find("#dashboard_accounts").text(json.listaccountsresponse.account.length);
					}
				});
				$.ajax({
					data: "command=listEvents&level=ERROR&response=json",
					dataType: "json",
					success: function(json) {
						var events = json.listeventsresponse.event;
						if (events != null && events.length > 0) {
							var errorGrid = thisTab.find("#error_grid_content").empty();
							var length = (events.length>=3) ? 3 : events.length;
							for (var i = 0; i < length; i++) {
								var errorTemplate = $("#recent_error_template").clone(true);
								errorTemplate.find("#db_error_type").text(events[i].type);
								errorTemplate.find("#db_error_msg").text(sanitizeXSS(events[i].description));								
								setDateField(events[i].created, errorTemplate.find("#db_error_date"));																
								errorGrid.append(errorTemplate.show());
							}
						}
					}
				});
			} else if(isUser()) {			    
			    $("#launch_test").hide();
				$.ajax({
					cache: false,
					data: "command=listAccounts&response=json",
					dataType: "json",
					success: function(json) {
					    var accounts = json.listaccountsresponse.account;						
						if (accounts != null && accounts.length > 0) {
						    var statJSON = accounts[0];
						    var sent = parseInt(statJSON.sentbytes);
						    var rec = parseInt(statJSON.receivedbytes);
    						
    						if(sent==0 && rec==0)
    						    $("#network_bandwidth_panel").hide();
    						else
    						    $("#network_bandwidth_panel").show();
    						
						    $("#menutab_role_user").show();
						    $("#menutab_role_root").hide();
							$("#menutab_role_domain").hide();
						    $("#tab_dashboard_user").show();
						    $("#tab_dashboard_root, #tab_dashboard_domain, #loading_gridtable").hide();
							
						    // This is in bytes, so let's change to KB
						    sent = Math.round(sent / 1024);
						    rec = Math.round(rec / 1024);
						    $("#db_sent").text(sent + "KB");
						    $("#db_received").text(rec + "KB");
						    $("#db_available_public_ips").text(statJSON.ipavailable);
						    $("#db_owned_public_ips").text(statJSON.iptotal);
						    $("#db_running_vms").text(statJSON.vmrunning + " VM(s)");
						    $("#db_stopped_vms").text(statJSON.vmstopped + " VM(s)");
						    $("#db_total_vms").text(statJSON.vmtotal + " VM(s)");
						    $("#db_avail_vms").text(statJSON.vmavailable + " VM(s)");						   
						    $("#db_account_id").text(statJSON.id);
						    $("#db_account").text(statJSON.name);						    
						    $("#db_type").text(toRole(statJSON.accounttype));
						    $("#db_domain").text(statJSON.domain);						    			   
						}
						
						// Events
						$.ajax({
							data: "command=listEvents&level=ERROR&response=json",
							dataType: "json",
							success: function(json) {
								var events = json.listeventsresponse.event;
								if (events != null && events.length > 0) {
									var errorGrid = $("#error_grid_content").empty();
									var length = (events.length>=3) ? 3 : events.length;
									for (var i = 0; i < length; i++) {
										var errorTemplate = $("#recent_error_template").clone(true);
										errorTemplate.find("#db_error_type").text(events[i].type);
										errorTemplate.find("#db_error_msg").text(sanitizeXSS(events[i].description));										
										setDateField(events[i].created, errorTemplate.find("#db_error_date"));									
										errorGrid.append(errorTemplate.show());
									}
								}
							}
						});
					},					
					beforeSend: function(XMLHttpRequest) {
						return true;
					}	
				});
			} else { //no role 
			    logout(false);
			    return;
			}
		});
	}

	// Check whether the session is valid.
	g_mySession = $.cookie("JSESSIONID");
	g_role = $.cookie("role");
	g_username = $.cookie("username");
	g_account = $.cookie("account");
	g_domainid = $.cookie("domainid");
	g_networkType = $.cookie("networktype");
	g_hypervisorType = $.cookie("hypervisortype");
	g_timezone = $.cookie("timezone");
	g_directattachnetworkgroupsenabled = $.cookie("directattachnetworkgroupsenabled");
	g_directAttachedUntaggedEnabled = $.cookie("directattacheduntaggedenabled");
	
	if($.cookie("timezoneoffset") != null)
	    g_timezoneoffset = isNaN($.cookie("timezoneoffset"))?null: parseFloat($.cookie("timezoneoffset"));
	else
	    g_timezoneoffset = null;
	    
	if (!g_networkType || g_networkType.length == 0) 		
		g_networkType = "vnet";
	
	if (!g_hypervisorType || g_hypervisorType.length == 0) 		
		g_hypervisorType = "kvm";
	
	if (!g_directattachnetworkgroupsenabled || g_directattachnetworkgroupsenabled.length == 0) 		
		g_directattachnetworkgroupsenabled = "false";	
		
	if (!g_directAttachedUntaggedEnabled || g_directAttachedUntaggedEnabled.length == 0) 		
		g_directAttachedUntaggedEnabled = "false";		
		
	$.ajax({
		data: "command=listZones&available=true&response=json",
		dataType: "json",
		async: false,
		success: function(json) {
			// session is valid, continue
			if (isUser()) {
				$("#menutab_role_user #menutab_dashboard_user").click();
			} else if (isAdmin()) {
				$("#menutab_role_root #menutab_dashboard_root").click();
			} else if (isDomainAdmin()) {
				$("#menutab_role_domain #menutab_dashboard_domain").click();
			} else {
				logout(false);
			}
		},
		error: function(xmlHTTP) {
			logout(false);
		},
		beforeSend: function(xmlHTTP) {
			return true;
		}
	});
});

