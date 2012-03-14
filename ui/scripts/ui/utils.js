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

  /**
   * Localization -- shortcut _l
   *
   * Takes string and runs through localization function -- if no code
   * exists or function isn't present, return string as-is
   */
  cloudStack.localize = window._l = function(str) {
    var localized = cloudStack.localizationFn ?
          cloudStack.localizationFn(str) : null;

    return localized ? localized : str;
  };

  /**
   * Sanitize user input -- shortcut _s
   * 
   * Strip unwanted characters from user-based input
   */
  cloudStack.sanitize = window._s = function(str) {
    if (!str) return '';
    
    var sanitized = str
          .replace(/&/g, "&amp;")
          .replace(/</g, "&lt;")
          .replace(/>/g, "&gt;");

    return sanitized;
  };
})(jQuery, cloudStack);
