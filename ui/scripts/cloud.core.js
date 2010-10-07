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

// global variables
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

//dropdown field size 
var maxPageSize = "&pagesize=500"; 

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

function fromdb(val) {
    return sanitizeXSS(unescape(noNull(val)));
}

function todb(val) {
    return encodeURIComponent(escape(val));
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

function noNull(val) {
    if(val == null)
        return "";
    else
        return val;
}

// Prevent cross-site-script(XSS) attack. 
// used right before adding user input to the DOM tree. e.g. DOM_element.html(sanitizeXSS(user_input));  
function sanitizeXSS(val) {     
    if(val == null|| typeof(val) != "string")
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