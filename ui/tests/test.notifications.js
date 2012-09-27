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
  var $notifications, $notificationBox,
      $cloudStack, cloudStack;

  module('Notifications', {
    setup: function() {
      $.fx.off = true;

      cloudStack = {
        sections: {
          home: {
            show: function() { return $('<div>').addClass('test123'); }
          },
          sectionA: {
            show: function() { return $('<div>').addClass('notification123'); }
          }
        },

        home: 'home' 
      };     

      $cloudStack = $('<div>').appendTo($('#qunit-fixture'));
      ok($cloudStack.cloudStack(cloudStack), 'Basic widget initialized');

      // Need to cleanup here -- not handled by widget
      $('.notification-box').remove();
      
      $notifications = $('<div>').appendTo($cloudStack);
      ok($notifications.notifications(), 'Initialize notifications widget');
      $notificationBox = $('.notification-box');
    }
  });

  test('Widget setup', function() {
    ok($notifications.hasClass('notifications'), 'Correct styling assigned');
    equal($notificationBox.size(), 1, 'Notification box present');
  });

  test('Add notification via widget', function() {
    stop();
    $notifications.notifications('add', { // Basic notification
      desc: 'testNotification123',
      interval: 0,
      poll: function(args) {
        var $li = $notificationBox.find('li');
        
        start();
        equal($li.size(), 1, 'Notification added to list');
        equal($li.find('span').html(), 'testNotification123', 'Notification description correct');
        ok($li.hasClass('pending'), 'Notification item has pending state');
        ok($notificationBox.find('.button.clear-list').click(), 'Clear list button click');
        equal($notificationBox.find('li').size(), 1, 'Notification list still has correct number of items');
        args.complete();
        ok(!$li.hasClass('pending'), 'Notification item has non-pending (complete) state');

        stop();
        $notifications.notifications('add', { // More comprehensive notification
          desc: 'testNotification456',
          interval: 0,
          _custom: {
            attrA: '123',
            attrB: '456'
          },
          section: 'sectionA',
          poll: function(args) {
            var $li = $notificationBox.find('li');
            
            start();
            equal($li.size(), 2, 'Notification list is correct');
            ok($.isPlainObject(args._custom), '_custom present');
            equal(args._custom.attrA, '123', '_custom attr A correct');
            equal(args._custom.attrB, '456', '_custom attr B correct');
            ok($li.filter(':last').hasClass('pending'), 'New notification item has pending state');
            ok(!$li.filter(':first').hasClass('pending'), 'First notification item still has non-pending (complete) state');
            ok($notificationBox.find('.button.clear-list').click(), 'Clear list button click');
            ok(!$notificationBox.find('li:first').is(':visible'), 'First (completed) notification item cleared');
            args.complete();
            ok(!$li.hasClass('pending'), 'All notifications item has non-pending (complete) state');
            equal($li.filter(':last').data('notification-section'), 'sectionA', 'Section data is correct in last notification');
            equal($li.filter(':first').find('span').html(), 'testNotification123', 'First notification description correct');
            equal($li.filter(':last').find('span').html(), 'testNotification456', 'Second notification description correct');
            $li.filter(':last').find('span').click();
            equal($cloudStack.find('.notification123').size(), 1, 'Notification item text goes to correct section on click');
            ok($li.filter(':last').find('.remove').click(), 'Remove first item');
            equal($notificationBox.find('li').size(), 0, 'Notification list has no items anymore');
          }
        });
      }
    });
  });
}(jQuery)); 
