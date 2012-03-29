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
	// Returning true means the LOGIN page will be show.  If you wish to redirect the user
	// to different login page, this is where you would do that.
	g_loginResponse = null;
	return true;
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
	
	var url = $.urlParam("loginUrl");
	if (url != undefined && url != null && url.length > 0) {
		url = unescape(clientApiUrl+"?"+url);
		$.ajax({
			url: url,
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


