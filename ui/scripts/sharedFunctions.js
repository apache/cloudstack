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
  var urlString = clientApiUrl + "?" + "command=" + apiName +"&response=json&sessionkey=" + g_sessionKey;

  if (cloudStack.context && cloudStack.context.projects) {
    urlString = urlString + '&projectid=' + cloudStack.context.projects[0].id;
  }
  
  return urlString;
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

var roleTypeUser = "0";
var roleTypeAdmin = "1";
var roleTypeDomainAdmin = "2";

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
    if (type == roleTypeUser) {
      return "User";
    } else if (type == roleTypeAdmin) {
      return "Admin";
    } else if (type == roleTypeDomainAdmin) {
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

var timezoneMap = new Object();
timezoneMap['Etc/GMT+12']='[UTC-12:00] GMT-12:00';
timezoneMap['Etc/GMT+11']='[UTC-11:00] GMT-11:00';
timezoneMap['Pacific/Samoa']='[UTC-11:00] Samoa Standard Time';
timezoneMap['Pacific/Honolulu']='[UTC-10:00] Hawaii Standard Time';
timezoneMap['US/Alaska']='[UTC-09:00] Alaska Standard Time';
timezoneMap['America/Los_Angeles']='[UTC-08:00] Pacific Standard Time';
timezoneMap['Mexico/BajaNorte']='[UTC-08:00] Baja California';
timezoneMap['US/Arizona']='[UTC-07:00] Arizona';
timezoneMap['US/Mountain']='[UTC-07:00] Mountain Standard Time';
timezoneMap['America/Chihuahua']='[UTC-07:00] Chihuahua, La Paz';
timezoneMap['America/Chicago']='[UTC-06:00] Central Standard Time';
timezoneMap['America/Costa_Rica']='[UTC-06:00] Central America';
timezoneMap['America/Mexico_City']='[UTC-06:00] Mexico City, Monterrey';
timezoneMap['Canada/Saskatchewan']='[UTC-06:00] Saskatchewan';
timezoneMap['America/Bogota']='[UTC-05:00] Bogota, Lima';
timezoneMap['America/New_York']='[UTC-05:00] Eastern Standard Time';
timezoneMap['America/Caracas']='[UTC-04:00] Venezuela Time';
timezoneMap['America/Asuncion']='[UTC-04:00] Paraguay Time';
timezoneMap['America/Cuiaba']='[UTC-04:00] Amazon Time';
timezoneMap['America/Halifax']='[UTC-04:00] Atlantic Standard Time';
timezoneMap['America/La_Paz']='[UTC-04:00] Bolivia Time';
timezoneMap['America/Santiago']='[UTC-04:00] Chile Time';
timezoneMap['America/St_Johns']='[UTC-03:30] Newfoundland Standard Time';
timezoneMap['America/Araguaina']='[UTC-03:00] Brasilia Time';
timezoneMap['America/Argentina/Buenos_Aires']='[UTC-03:00] Argentine Time';
timezoneMap['America/Cayenne']='[UTC-03:00] French Guiana Time';
timezoneMap['America/Godthab']='[UTC-03:00] Greenland Time';
timezoneMap['America/Montevideo']='[UTC-03:00] Uruguay Time]';
timezoneMap['Etc/GMT+2']='[UTC-02:00] GMT-02:00';
timezoneMap['Atlantic/Azores']='[UTC-01:00] Azores Time';
timezoneMap['Atlantic/Cape_Verde']='[UTC-01:00] Cape Verde Time';
timezoneMap['Africa/Casablanca']='[UTC] Casablanca';
timezoneMap['Etc/UTC']='[UTC] Coordinated Universal Time';
timezoneMap['Atlantic/Reykjavik']='[UTC] Reykjavik';
timezoneMap['Europe/London']='[UTC] Western European Time';
timezoneMap['CET']='[UTC+01:00] Central European Time';
timezoneMap['Europe/Bucharest']='[UTC+02:00] Eastern European Time';
timezoneMap['Africa/Johannesburg']='[UTC+02:00] South Africa Standard Time';
timezoneMap['Asia/Beirut']='[UTC+02:00] Beirut';
timezoneMap['Africa/Cairo']='[UTC+02:00] Cairo';
timezoneMap['Asia/Jerusalem']='[UTC+02:00] Israel Standard Time';
timezoneMap['Europe/Minsk']='[UTC+02:00] Minsk';
timezoneMap['Europe/Moscow']='[UTC+03:00] Moscow Standard Time';
timezoneMap['Africa/Nairobi']='[UTC+03:00] Eastern African Time';
timezoneMap['Asia/Karachi']='[UTC+05:00] Pakistan Time';
timezoneMap['Asia/Kolkata']='[UTC+05:30] India Standard Time';
timezoneMap['Asia/Bangkok']='[UTC+05:30] Indochina Time';
timezoneMap['Asia/Shanghai']='[UTC+08:00] China Standard Time';
timezoneMap['Asia/Kuala_Lumpur']='[UTC+08:00] Malaysia Time';
timezoneMap['Australia/Perth']='[UTC+08:00] Western Standard Time (Australia)';
timezoneMap['Asia/Taipei']='[UTC+08:00] Taiwan';
timezoneMap['Asia/Tokyo']='[UTC+09:00] Japan Standard Time';
timezoneMap['Asia/Seoul']='[UTC+09:00] Korea Standard Time';
timezoneMap['Australia/Adelaide']='[UTC+09:30] Central Standard Time (South Australia)';
timezoneMap['Australia/Darwin']='[UTC+09:30] Central Standard Time (Northern Territory)';
timezoneMap['Australia/Brisbane']='[UTC+10:00] Eastern Standard Time (Queensland)';
timezoneMap['Australia/Canberra']='[UTC+10:00] Eastern Standard Time (New South Wales)';
timezoneMap['Pacific/Guam']='[UTC+10:00] Chamorro Standard Time';
timezoneMap['Pacific/Auckland']='[UTC+12:00] New Zealand Standard Time';
