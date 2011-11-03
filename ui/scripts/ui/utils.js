(function($, cloudStack) {
  // General utils
  cloudStack.serializeForm = function($form) {
    var data = {};
    $($form.serializeArray()).each(function() {
      var dataItem = data[this.name];

      if (!dataItem)
        data[this.name] = this.value;
      else if (dataItem && !$(dataItem).size())
        data[this.name] = [dataItem, this.value];
      else
        dataItem.push(this.value);
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
