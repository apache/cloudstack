(function($) {
  var $browser, $breadcrumbs, $browserContainer;

  module('Browser', {
    setup: function() {
      $.fx.off = true;
      $browser = $('<div>').addClass('browser-test').appendTo('#qunit-fixture');
      $breadcrumbs = $('<div>').attr('id', 'breadcrumbs').appendTo($browser);
      $browserContainer = $('<div>').addClass('container').appendTo($browser);
      ok($browserContainer.cloudBrowser(), 'Browser initialized');
      equal($breadcrumbs.find('ul').size(), 1, 'Breadcrumbs initialized');
    }
  });

  // Browser tests
  test('Add panel', function() {
    ok($browserContainer.cloudBrowser('addPanel', { title: 'testPanel123' }), 'Add panel');
    equal($browser.find('.panel').size(), 1, 'Browser has 1 panel');
    equal($breadcrumbs.find('ul li').size(), 1, 'Browser has 1 breadcrumb');
    equal($breadcrumbs.find('ul li:first span').html(), 'testPanel123', 'Panel has correct title');
  });

  test('Add a second panel', function() {
    ok($browserContainer.cloudBrowser('addPanel', { title: 'testPanel123' }), 'Add first panel');
    ok($browserContainer.cloudBrowser('addPanel', { title: 'testPanel456' }), 'Add second panel');
    equal($browser.find('.panel').size(), 2, 'Browser has 2 panels');
    equal($breadcrumbs.find('ul li').size(), 2, 'Browser has 2 breadcrumbs');
    equal($breadcrumbs.find('ul li:last span').html(), 'testPanel456', 'New panel has correct title');
    equal($breadcrumbs.find('ul li:first span').html(), 'testPanel123', 'First panel still has correct title');
  });

  test('Select panel', function() {
    ok($browserContainer.cloudBrowser('addPanel', { title: 'testPanel123' }), 'Add first panel');
    ok($browserContainer.cloudBrowser('addPanel', { title: 'testPanel456' }), 'Add second panel');
    ok($browserContainer.cloudBrowser('addPanel', { title: 'testPanel789' }), 'Add third panel');

    stop();
    $browserContainer.cloudBrowser('selectPanel', {
      panel: $browser.find('.panel:first').next(),
      complete: function() {
        start();
        ok(true, 'Select second panel');
      }
    });  

    equal($browser.find('.panel').size(), 2, 'Browser has 2 panels');
    equal($breadcrumbs.find('ul li:first span').html(), 'testPanel123', 'First panel still has correct title');
    equal($breadcrumbs.find('ul li:last span').html(), 'testPanel456', 'Second panel still has correct title');
    equal($breadcrumbs.find('ul li').size(), 2, 'Browser has 2 breadcrumbs');

    stop();
    $browserContainer.cloudBrowser('selectPanel', {
      panel: $browser.find('.panel:first'),
      complete: function() {
        start();
        ok(true, 'Select first panel');
      }
    });  

    equal($browser.find('.panel').size(), 1, 'Browser has 1 panel');
    equal($breadcrumbs.find('ul li:first span').html(), 'testPanel123', 'First panel still has correct title');
    equal($breadcrumbs.find('ul li').size(), 1, 'Browser has 1 breadcrumb');
  });
}(jQuery)); 
