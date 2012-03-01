(function($) {
  test('Main widget', function() {
    var cloudStack = {
      sections: {
        home: {
          show: function() { return $('<div>').addClass('test123'); }
        },
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
    var $userOptions = $cloudStack.find('#user-options');
    equal($header.size(), 1, 'Header present');
    equal($userOptions.size(), 1, 'User options present');
    equal($userOptions.find('a').size(), 2, 'User options has correct # of options');

    // Navigation
    var $navigation = $cloudStack.find('#navigation');
    equal($navigation.size(), 1, 'Navigation present');
    equal($navigation.find('li').size(), 4, 'Navigation has correct # of nav items');

    // Browser / page generation
    var $browser = $cloudStack.find('#browser .container');
    var $homePage = $browser.find('.panel div.test123');
    equal($browser.size(), 1, 'Browser intialized');
    equal($homePage.size(), 1, 'Home page is visible');
  });
}(jQuery));
