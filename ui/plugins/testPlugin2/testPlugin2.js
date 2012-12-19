(function (cloudStack) {
  var testPlugin2 = function(plugin) {
    // Plugin code goes here
  };

  cloudStack.plugin({
    id: 'testPlugin2',
    title: 'Test Plugin 2',
    desc: 'Sample plugin 2',
    load: testPlugin2
  });
}(cloudStack));