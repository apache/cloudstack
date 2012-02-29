(function($) {
  test('Main widget', function() {
    var cloudStack = {
      sections: {
        home: {},
        sectionA: {},
        sectionB: {},
        sectionC: {}
      },

      home: 'home' 
    };     

    var $cloudStack = $('<div>');

    ok($cloudStack.cloudStack(cloudStack), 'Basic widget initialized');

    // Main containers/wrappers
    equal($cloudStack.find('[cloudStack-container]').size(), 1, 'Main sub-container present');
    equal($cloudStack.find('#main-area').size(), 1, 'Main area present');

    // Header
    var $header = $cloudStack.find('#header');
    equal($header.size(), 1, 'Header present');

    // Navigation
    var $navigation = $cloudStack.find('#navigation');
    equal($navigation.size(), 1, 'Navigation present');
    equal($navigation.find('li').size(), 4, 'Navigation has correct # of nav items');
  });
}(jQuery));
