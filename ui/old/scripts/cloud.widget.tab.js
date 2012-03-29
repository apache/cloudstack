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
