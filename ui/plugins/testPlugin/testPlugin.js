(function (cloudStack) {
  cloudStack.plugins.testPlugin = function(plugin) {
    plugin.ui.addSection({
      id: 'testPlugin',
      title: 'TestPlugin',
      preFilter: function(args) {
        return isAdmin();
      },
      show: function() {
        return $('<div>').html('Test plugin section');
      }
    });
  };
}(cloudStack));