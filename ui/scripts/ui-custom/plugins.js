(function($, cloudStack) {
  var elems = {
    pluginItem: function(args) {
      var id = args.id;
      var title = args.title;
      var desc = args.desc;
      var $pluginItem = $('<li>').addClass('plugin-item').addClass(id);
      var $title = $('<span>').addClass('title').html(title);
      var $desc = $('<span>').addClass('desc').html(desc);
      var $icon = $('<span>').addClass('icon');

      $pluginItem.append(
        $icon, $title, $desc
      );

      return $pluginItem;
    },
    pluginListing: function(args) {
      var plugins = args.plugins;
      var $plugins = $('<ul>');
      var $pluginsListing = $('<div>').addClass('plugins-listing');

      $(plugins).each(function() {
        var plugin = this;

        elems.pluginItem({
          id: plugin.id,
          title: plugin.title,
          desc: plugin.desc
        }).appendTo($plugins);
      });

      $pluginsListing.append($plugins);

      return $pluginsListing;
    }
  };

  cloudStack.uiCustom.plugins = function() {
    var plugins = cloudStack.plugins.loaded;

    return elems.pluginListing({
      plugins: $(plugins).map(function(index, pluginID) {
        var plugin = cloudStack.plugins.registry[pluginID];

        return {
          id: plugin.id,
          title: plugin.title,
          desc: plugin.desc
        };
      })
    });
  };
}(jQuery, cloudStack)); 
