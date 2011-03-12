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
$.urlParam = function(name){ var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href); if (!results) { return 0; } return results[1] || 0;}

$(document).ready(function() {
	var url = $.urlParam("loginUrl");
	if (url != undefined && url != null && url.length > 0) {
		// single signon process
		url = unescape("/client/api?"+url);
		$.ajax({
			url: url,
			dataType: "json",
			async: false,
			type: "POST",
			success: function(json) {
				login(json);
			},
			error: function() {
				logout();
			},
			beforeSend: function(XMLHttpRequest) {
				return true;
			}
		});
	} else {
		$("#loginmain").show();
		
		$("#login_submit").click("click", function(event) {
			var username = escape($("#login_username").val());
			var password = $.md5($("#login_password").val());
			var domain = escape("/"+username+"_domain");
			$.ajax({
				url: "/client/api?command=login&response=json&username="+username+"&password="+password+"&domain="+domain,
				dataType: "json",
				async: false,
				type: "POST",
				success: function(json) {
				    $("#login_error").hide();
					login(json);
				},
				error: function(XMLHttpRequest) {				
					$("#login_password").val("");
					$("#login_error").show();
					$("#login_username").focus();
				},
				beforeSend: function(XMLHttpRequest) {
					return true;
				}
			});
			return false;
		});
			
		$("#login_form").keypress(function(event) {			
			if(event.keyCode == keycode_Enter) {
				$("#login_submit").click();
			}
		});		
	}
});