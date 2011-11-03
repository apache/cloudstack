var g_mySession = null;
var g_sessionKey = null;
var g_role = null; // roles - root, domain-admin, ro-admin, user
var g_username = null;
var g_account = null;
var g_domainid = null;
var g_enableLogging = false;
var g_timezoneoffset = null;
var g_timezone = null;
var g_supportELB = null;
var g_firewallRuleUiEnabled = null; //true or false
var g_directAttachSecurityGroupsEnabled = "false";
var g_userPublicTemplateEnabled = "true";

//keyboard keycode
var keycode_Enter = 13;

//XMLHttpResponse.status
var ERROR_ACCESS_DENIED_DUE_TO_UNAUTHORIZED = 401;
var ERROR_INTERNET_NAME_NOT_RESOLVED = 12007;
var ERROR_INTERNET_CANNOT_CONNECT = 12029;
var ERROR_VMOPS_ACCOUNT_ERROR = 531;

// Default password is MD5 hashed.  Set the following variable to false to disable this.
var md5Hashed = true;

//page size for API call (e.g."listXXXXXXX&pagesize=N" )
var pageSize = 50;

//async action
var pollAsyncJobResult = function(args) {
  $.ajax({
    url: createURL("queryAsyncJobResult&jobId=" + args._custom.jobId),
    dataType: "json",
    async: false,
    success: function(json) {
      var result = json.queryasyncjobresultresponse;
      if (result.jobstatus == 0) {
        return; //Job has not completed
      } else {
        if (result.jobstatus == 1) { // Succeeded
          //debugger;
          if(args._custom.getUpdatedItem != null && args._custom.getActionFilter != null) {
            args.complete({
              data: args._custom.getUpdatedItem(json),
              actionFilter: args._custom.getActionFilter()
            });
          }
          else {
            args.complete();
          }
        }
        else if (result.jobstatus == 2) { // Failed
          args.error({message:result.jobresult.errortext});
        }
      }
    },
    error: function(XMLHttpResponse) {
      args.error();
    }
  });
}

//API calls
function createURL(apiName) {
  return clientApiUrl + "?" + "command=" + apiName +"&response=json&sessionkey=" + g_sessionKey;
}

function fromdb(val) {
  return sanitizeXSS(noNull(val));
}

function todb(val) {
  return encodeURIComponent(val);
}

function noNull(val) {
  if(val == null)
    return "";
  else
    return val;
}

function sanitizeXSS(val) {  // Prevent cross-site-script(XSS) attack
  if(val == null || typeof(val) != "string")
    return val;
  val = val.replace(/</g, "&lt;");  //replace < whose unicode is \u003c
  val = val.replace(/>/g, "&gt;");  //replace > whose unicode is \u003e
  return unescape(val);
}

// Role Functions
function isAdmin() {
  return (g_role == 1);
}

function isDomainAdmin() {
  return (g_role == 2);
}

function isUser() {
  return (g_role == 0);
}

// FUNCTION: Handles AJAX error callbacks.  You can pass in an optional function to
// handle errors that are not already handled by this method.
function handleError(XMLHttpResponse, handleErrorCallback) {
  // User Not authenticated
  if (XMLHttpResponse.status == ERROR_ACCESS_DENIED_DUE_TO_UNAUTHORIZED) {
    $("#dialog_session_expired").dialog("open");
  }
  else if (XMLHttpResponse.status == ERROR_INTERNET_NAME_NOT_RESOLVED) {
    $("#dialog_error_internet_not_resolved").dialog("open");
  }
  else if (XMLHttpResponse.status == ERROR_INTERNET_CANNOT_CONNECT) {
    $("#dialog_error_management_server_not_accessible").dialog("open");
  }
  else if (XMLHttpResponse.status == ERROR_VMOPS_ACCOUNT_ERROR && handleErrorCallback != undefined) {
    handleErrorCallback();
  }
  else if (handleErrorCallback != undefined) {
    handleErrorCallback();
  }
  else {
    var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
    $("#dialog_error").text(fromdb(errorMsg)).dialog("open");
  }
}

function parseXMLHttpResponse(XMLHttpResponse) {
  if(isValidJsonString(XMLHttpResponse.responseText) == false) {
    return "";
  }

  //var json = jQuery.parseJSON(XMLHttpResponse.responseText);
  var json = JSON.parse(XMLHttpResponse.responseText);
  if (json != null) {
    var property;
    for(property in json) {}
    var errorObj = json[property];
    return fromdb(errorObj.errortext);
  } else {
    return "";
  }
}

function isValidJsonString(str) {
  try {
    JSON.parse(str);
  }
  catch (e) {
    return false;
  }
  return true;
}

cloudStack.preFilter = {
  createTemplate: function(args) {
    if(isAdmin()) {
      args.$form.find('.form-item[rel=isPublic]').css('display', 'inline-block');
      args.$form.find('.form-item[rel=isFeatured]').css('display', 'inline-block');
    }
    else {
      if (g_userPublicTemplateEnabled == "true") {
        args.$form.find('.form-item[rel=isPublic]').css('display', 'inline-block');
      }
      else {
        args.$form.find('.form-item[rel=isPublic]').hide();
      }
      args.$form.find('.form-item[rel=isFeatured]').hide();
    }
  }
}

cloudStack.converters = {
  convertBytes: function(bytes) {
    if (bytes < 1024 * 1024) {
      return (bytes / 1024).toFixed(2) + " KB";
    } else if (bytes < 1024 * 1024 * 1024) {
      return (bytes / 1024 / 1024).toFixed(2) + " MB";
    } else if (bytes < 1024 * 1024 * 1024 * 1024) {
      return (bytes / 1024 / 1024 / 1024).toFixed(2) + " GB";
    } else {
      return (bytes / 1024 / 1024 / 1024 / 1024).toFixed(2) + " TB";
    }
  },
  toBooleanText: function(booleanValue) {
    if(booleanValue == true)
      return "Yes";
    else if(booleanValue == false)
      return "No";
  },
  convertHz: function(hz) {
    if (hz == null)
      return "";

    if (hz < 1000) {
      return hz + " MHZ";
    } else {
      return (hz / 1000).toFixed(2) + " GHZ";
    }
  },
  toDayOfWeekDesp: function(dayOfWeek) {
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
  },
  toDayOfWeekDesp: function(dayOfWeek) {
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
  },
  toNetworkType: function(usevirtualnetwork) {
    if(usevirtualnetwork == true || usevirtualnetwork == "true")
      return "Public";
    else
      return "Direct";
  },
  toRole: function(type) {
    if (type == "0") {
      return "User";
    } else if (type == "1") {
      return "Admin";
    } else if (type == "2") {
      return "Domain-Admin";
    }
  },
  toAlertType: function(alertCode) {
    switch (alertCode) {
    case 0 : return "Capacity Threshold - Memory";
    case 1 : return "Capacity Threshold - CPU";
    case 2 : return "Capacity Threshold - Storage Used";
    case 3 : return "Capacity Threshold - Storage Allocated";
    case 4 : return "Capacity Threshold - Public IP";
    case 5 : return "Capacity Threshold - Private IP";
    case 6 : return "Monitoring - Host";
    case 7 : return "Monitoring - VM";
    case 8 : return "Monitoring - Domain Router";
    case 9 : return "Monitoring - Console Proxy";
    case 10 : return "Monitoring - Routing Host";
    case 11 : return "Monitoring - Storage";
    case 12 : return "Monitoring - Usage Server";
    case 13 : return "Monitoring - Management Server";
    case 14 : return "Migration - Domain Router";
    case 15 : return "Migration - Console Proxy";
    case 16 : return "Migration - User VM";
    case 17 : return "VLAN";
    case 18 : return "Monitoring - Secondary Storage VM";
    }
  }
}

//VM Instance
function getVmName(p_vmName, p_vmDisplayname) {
  if(p_vmDisplayname == null)
    return fromdb(p_vmName);

  var vmName = null;
  if (p_vmDisplayname != p_vmName) {
    vmName = fromdb(p_vmName) + " (" + fromdb(p_vmDisplayname) + ")";
  } else {
    vmName = fromdb(p_vmName);
  }
  return vmName;
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
