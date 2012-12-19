(function (cloudStack) {
  var testPlugin1 = function(plugin) {
    // Plugin code goes here
  };

  cloudStack.plugin({
    id: 'testPlugin1',
    title: 'Test Plugin 1',
    desc: 'Sample plugin 1',
    load: testPlugin1
  });
}(cloudStack));