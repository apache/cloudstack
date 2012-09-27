// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
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

  test('Add maximized panel', function() {
    var $maximizedPanel, $normalPanel;
    
    ok($browserContainer.cloudBrowser('addPanel', { title: 'testPanel123' }, 'Add first panel'));
    ok($browserContainer.cloudBrowser('addPanel', { title: 'testPanel456' }, 'Add normal-sized-panel'));
    ok($browserContainer.cloudBrowser('addPanel', { title: 'testPanel789', maximizeIfSelected: true }), 'Add maximized panel');

    $maximizedPanel = $browserContainer.find('.panel:last');
    $normalPanel = $browserContainer.find('.panel:first').next();
   
    ok($maximizedPanel.hasClass('always-maximized'), 'Maximized panel has maximized class');
    ok(!$normalPanel.hasClass('always-maximized'), 'Normal panel has maximized class');
    equal($maximizedPanel.width(), $browserContainer.width(), 'Maximized panel covers full width of browser container');
    notEqual($normalPanel.width(), $browserContainer.width(), 'Normal panel doesn\'t have maximized appearance');
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

  test('Remove all panels', function() {
    ok($browserContainer.cloudBrowser('addPanel', { title: 'testPanel123' }), 'Add first panel');
    ok($browserContainer.cloudBrowser('addPanel', { title: 'testPanel456' }), 'Add second panel');
    ok($browserContainer.cloudBrowser('addPanel', { title: 'testPanel789' }), 'Add third panel');
    equal($browserContainer.find('.panel').size(), 3, 'Correct # of panels');
    ok($browserContainer.cloudBrowser('removeAllPanels'), 'Remove all panels');
    equal($browserContainer.find('.panel').size(), 0, 'All panels removed');
    ok($browserContainer.cloudBrowser('addPanel', { title: 'testPanel123' }), 'Add 1 panel');
    equal($browserContainer.find('.panel').size(), 1, 'Correct # of panels');
  });

  test('Maximize panel', function() {
    ok($browserContainer.cloudBrowser('addPanel', { title: 'testPanel123' }), 'Add first panel');
    ok($browserContainer.cloudBrowser('addPanel', { title: 'testPanel456' }), 'Add second panel');
    equal($browserContainer.find('.panel').size(), 2, 'Correct # of panels');
    ok($browserContainer.cloudBrowser('toggleMaximizePanel', { panel: $browserContainer.find('.panel:first')}), 'Maximize first panel');
    ok($browserContainer.find('.panel:first').hasClass('maximized'), 'First panel has maximized style');
    ok(!$browserContainer.find('.panel:last').hasClass('maximized'), 'Last panel has correct style');
  });
}(jQuery)); 
