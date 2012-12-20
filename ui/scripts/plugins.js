(function($, cloudStack, require) {
  var pluginAPI = {
    addSection: function(section) {
      cloudStack.sections[section.id] = section;
    },
    extend: function(obj) {
      $.extend(true, cloudStack, obj);
    }
  };
  
  cloudStack.sections.plugins = {
    title: 'Plugins',
    show: cloudStack.uiCustom.plugins
  };

  // Load plugins
  $(cloudStack.plugins).map(function(index, pluginID) {
    var basePath = 'plugins/' + pluginID + '/';
    var pluginJS = basePath + pluginID + '.js';
    var configJS = basePath + 'config.js';

    require([pluginJS], function() {
      require([configJS]);

      // Execute plugin
      cloudStack.plugins[pluginID]({
        ui: pluginAPI
      });
    });
  });
}(jQuery, cloudStack, require));
