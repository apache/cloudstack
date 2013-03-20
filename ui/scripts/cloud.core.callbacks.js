// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
$.urlParam = function(name){ var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href); if (!results) { return 0; } return results[1] || 0;}
 
/*
This file is meant to help with implementing single signon integration.  If you are using the
cloud.com default UI, there is no need to touch this file.
*/

/*
This callback function is called when either the session has timed out for the user,
the session ID has been changed (i.e. another user logging into the UI via a different tab), 
or it's the first time the user has come to this page.
*/
function onLogoutCallback() {
  g_loginResponse = null; //clear single signon variable g_loginResponse
	
	
	return true;  // return true means the login page will show 	
	/*
	window.location.replace("http://www.google.com"); //redirect to a different location
  return false;	//return false means it will stay in the location window.location.replace() sets it to (i.e. "http://www.google.com") 
	*/
}

var g_loginResponse = null;

/*
For single signon purposes, you just need to make sure that after a successful login, you set the
global variable "g_loginResponse"

You can also pass in a special param called loginUrl that is pregenerated and sent to the CloudStack, it will
automatically log you in.

Below is a sample login attempt
*/

var clientApiUrl = "/client/api";
var clientConsoleUrl   = "/client/console";

$(document).ready(function() {		
  /*
	condition 1: If window.location.href contains parameter 'loginUrl', save the parameter's value to a cookie, then reload the page without any URL parameter.
	(After the page is reloaded without any URL parameter, it will fall in condition 2.) 
	*/
	if ($.urlParam('loginUrl') != 0) {	 
		$.cookie('loginUrl', $.urlParam('loginUrl'), { expires: 1});
	  document.location.href = '/client/';	
	}
	
	/* 
	condition 2: If window.location.href does not contain parameter 'loginUrl' but cookie 'loginUrl' exists, 
	save the cookie's value to g_regionUrlParam (a global variable for switching regions), 
	then call login API to set g_loginResponse (a global variable for single-sign-on).
	*/
	else if($.cookie('loginUrl') != null) {	  
	  g_regionUrlParam = '?loginUrl=' + $.cookie('loginUrl');		
		$.ajax({
			url: unescape(clientApiUrl + "?" + $.cookie('loginUrl')),
			dataType: "json",
			async: false,
			success: function(json) {
				g_loginResponse = json.loginresponse;				
			},
			error: function() {
				onLogoutCallback();
				// This means the login failed.  You should redirect to your login page.
			},
			beforeSend: function(XMLHttpRequest) {
				return true;
			}
		});
	}
	
});


