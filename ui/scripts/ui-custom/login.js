(function($, cloudStack) {
  /**
   * Login process
   */
  cloudStack.uiCustom.login = function(args) {
    var $container = args.$container;
    var $login = $('#template').find('.login').clone();
    var $form = $login.find('form');
    var $inputs = $form.find('input[type=text], input[type=password]');
    var complete = args.complete;
    var bypass = args.bypassLoginCheck && args.bypassLoginCheck();

    // Check to see if we can bypass login screen
    if (bypass) {
      complete({
        user: bypass.user
      });

      return;
    }

    $login.appendTo('html body');
    $('html body').addClass('login');

    // Form validation
    $form.validate();

    // Form label behavior
    $inputs.bind('focus blur', function(event) {
      var $target = $(event.target);
      var $label = $form.find('label').filter(function() {
        return $(this).attr('for') == $target.attr('name') && !$(this).hasClass('error');
      });
      var isEmpty = !$target.val();
      var isFocus = event.type == 'focus';
      var isBlur = event.type == 'blur';

      if (isFocus) {
        $label.hide();
      } else if (isBlur && isEmpty) {
        $label.show();
      }
    });

    $inputs.focus().blur();

    // Login action
    $login.find('input[type=submit]').click(function() {
      if (!$form.valid()) return false;

      var data = cloudStack.serializeForm($form);

      args.loginAction({
        data: data,
        response: {
          success: function(args) {
            $login.remove();
            $('html body').removeClass('login');
            complete({
              user: args.data.user
            });
          },
          error: function(args) {
            cloudStack.dialog.notice({ message: 'Invalid username and/or password.' });
          }
        }
      });

      return false;
    });
  };
})(jQuery, cloudStack);
