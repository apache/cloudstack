(function($, cloudStack) {
  cloudStack.plugin = function(args) {
    var id = args.id;
    var title = args.title;
    var desc = args.desc;

    cloudStack.plugins.registry[id] = {
      title: title,
      desc: desc
    };
  };

  cloudStack.plugins = {
    loaded: [], // Lists loaded plugins by ID
    registry: {}, // Stores metadata for plugins

    // Loads/executes script
    load: function(plugins) {
      $(plugins).map(function(index, pluginID) {
        var path = '/client/plugins/' + pluginID + '/' + pluginID + '.js';

        require([path], function() {
          cloudStack.plugins.loaded.push(pluginID);
        });

        return path;
      });
    }
  };

  cloudStack.sections.plugins = {
    title: 'Plugins',
    show: cloudStack.uiCustom.plugins
  }
}(jQuery, cloudStack));
