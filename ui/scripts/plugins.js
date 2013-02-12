(function($, cloudStack, require) {
  var loadCSS = function(path) {
    var $link = $('<link>');

    $link.attr({
      rel: 'stylesheet',
      type: 'text/css',
      href: path
    });

    $('head').append($link);
  };

  var pluginAPI = {
    apiCall: function(command, args) {
      $.ajax({
        url: createURL(command),
        success: args.success,
        error: function(json) {
          args.error(parseXMLHttpResponse(json));
        }
      })
    },
    addSection: function(section) {
      cloudStack.sections[section.id] = $.extend(section, {
        customIcon: 'plugins/' + section.id + '/icon.png'
      });
    },
    extend: function(obj) {
      $.extend(true, cloudStack, obj);
    }
  };
  
  cloudStack.sections.plugins = {
    title: 'label.plugins',
    show: cloudStack.uiCustom.plugins
  };

  // Load plugins
  $(cloudStack.plugins).map(function(index, pluginID) {
    var basePath = 'plugins/' + pluginID + '/';
    var pluginJS = basePath + pluginID + '.js';
    var configJS = basePath + 'config.js';
    var pluginCSS = basePath + pluginID + '.css';

    require([pluginJS], function() {
      require([configJS]);
      loadCSS(pluginCSS);

      // Execute plugin
      cloudStack.plugins[pluginID]({
        ui: pluginAPI
      });
    });

    // Load CSS
  });
}(jQuery, cloudStack, require));
