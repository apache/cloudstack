(function($, cloudStack) {
  cloudStack.ui.event = {
    // Attach element to specific event type
    elem: function(widget, elem, $elem, extraData) {
      // Setup DOM metadata
      var data = { cloudStack: {} };
      data.cloudStack[widget] = {
        elem: elem
      };
      if (extraData) $.extend(data.cloudStack[widget], extraData);

      return $elem
        .addClass('cloudStack-elem')
        .addClass(widget)
        .data(data);
    },

    // Create widget-based event
    bind: function(widget, events) {
      return function(event) {
        var $target = $(event.target);
        var $widget, $elem;
        var data, elem;

        $elem = $target.closest('.cloudStack-elem.' + widget);
        if (!$elem.size())
          return true;

        $widget = $('.cloudStack-widget.' + widget);
        data = $elem.data('cloudStack')[widget];
        elem = data.elem;

        events[elem]($elem, $widget, data);

        return false;
      };
    }
  };
})(jQuery, cloudStack);
