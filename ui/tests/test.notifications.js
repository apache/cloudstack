(function($) {
  var $notifications;

  module('Notifications', {
    setup: function() {
      $.fx.off = true;
      
      $notifications = $('<div>');
      ok($notifications.notifications(), 'Initialize notifications widget');
    }
  });

  test('Widget setup', function() {
    var $notificationBox = $('html body > .notification-box');

    ok($notifications.hasClass('notifications'), 'Correct styling assigned');
    equal($notificationBox.size(), 1, 'Notification box present');
  });
}(jQuery)); 
