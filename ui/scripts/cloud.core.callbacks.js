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
	// Returning true means the LOGIN page will be show.  If you wish to redirect the user
	// to different login page, this is where you would do that.
	g_loginResponse = null;
	return true;
}

var g_loginResponse = null;

/*
For single signon purposes, you just need to make sure that after a successful login, you set the
global variable "g_loginResponse"

Below is a sample login attempt
*/

/*
$(document).ready(function() {
	//var username = encodeURIComponent($("#account_username").val());
	//var password = encodeURIComponent($("#account_password").val());
	//var domain = encodeURIComponent($("#account_domain").val());
	//var url = "/client/api?command=login&username="+username+"&password="+password+"&domain="+domain+"&response=json";
	
	// Test URL
	var url = "/client/api?command=login&username=admin&password=5f4dcc3b5aa765d61d8327deb882cf99&domain=%2F&response=json";
	$.ajax({
		url: url,
		dataType: "json",
		async: false,
		success: function(json) {
			g_loginResponse = json.loginresponse;
		},
		error: function() {
			// This means the login failed.  You should redirect to your login page.
		},
		beforeSend: function(XMLHttpRequest) {
			return true;
		}
	});
});
*/


