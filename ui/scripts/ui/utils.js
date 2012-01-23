(function($, cloudStack) {
  // General utils
  cloudStack.serializeForm = function($form, options) {
    if (!options) options = {};
    
    var data = {};

    $($form.serializeArray()).each(function() {
      var dataItem = data[this.name];
      var value = this.value;

      if (options.escapeSlashes) {
        value = value.replace(/\//g, '__forwardSlash__');
      }

      if (!dataItem) {
        data[this.name] = value;
      } else if (dataItem && !$(dataItem).size()) {
        data[this.name] = [dataItem, value];
      } else {
        dataItem.push(value);
      }
    });

    return data;
  };

  // Even/odd row handling
  cloudStack.evenOdd = function($container, itemSelector, args) {
    var even = false;

    $container.find(itemSelector).each(function() {
      var $elem = $(this);
      
      if (even) {
        even = false;
        args.odd($elem);
      } else {
        even = true;
        args.even($elem);
      }
    });
  };
})(jQuery, cloudStack);
