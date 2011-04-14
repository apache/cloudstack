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

(function($) {
	jQuery.fn.tabSetup = function() {
		var tab = $(this);
		
		// initial setup
		$(".content_tabs", tab).each(function(i, val) {
			if($(val).hasClass("on"))
				$("#" + $(val).attr("cloud_binding")).show();
			else
				$("#" + $(val).attr("cloud_binding")).hide();
		});
		
		// event handling
		tab.click(function(e) {
			if($(e.target).hasClass("content_tabs")) {
				$(".content_tabs", tab).each(function(i, val) {
					$(val).removeClass("on").removeClass("off").addClass("off");
					$("#" + $(val).attr("cloud_binding")).hide();
				});
				
				$(e.target).removeClass("off").removeClass("on").addClass("on");
				$("#" + $(e.target).attr("cloud_binding")).show();
				return false;
			}
			return true;
		});
	};
})(jQuery);
