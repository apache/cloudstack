(function(cloudStack, $, testData) {
  $.extend(cloudStack, testData, {
    home: 'dashboard',

    sectionPreFilter: function(args) {
      var user = args.context.users[0];

      if (user.role == 'admin')
        return args.context.sections;

      return ['dashboard', 'instances', 'storage', 'templates', 'events', 'projects'];
    },
    sections: {
      dashboard: {},
      instances: {},
      storage: {},
      network: {},
      templates: {},
      accounts: {},
      domains: {},
      events: {},
      system: {},
      projects: {},
      'global-settings': {},
      configuration: {}
    }
  });

  $(function() {
    var $container = $('#cloudStack3-container');

    // Login
    cloudStack.uiCustom.login({
      $container: $container,

      // Use this for checking the session, to bypass login screen
      bypassLoginCheck: function(args) {
        var disabledLogin = document.location.href.split('?')[1] == 'login=disabled';

        if (disabledLogin)
          return {
            user: {
              login: 'wchan',
              name: 'Will Chan',
              role: 'admin'
            }
          };
        else
          return false;
      },

      // Actual login process, via form
      loginAction: function(args) {
        if (args.data.username != 'invalid'){
          return args.response.success({
            data: {
              user: {
                username: args.data.username,
                name: args.data.name ? args.data.name : args.data.username,
                role: args.data.username == 'jdoe' ? 'user' : 'admin'
              }
            }
          });
        }

        return args.response.error();
      },

      complete: function(args) {
        var context = {
          users: [args.user]
        };
        var cloudStackArgs = $.extend(cloudStack, {
          context: context
        });

        // Check to invoke install wizard
        cloudStack.installWizard.check({
          context: context,
          response: {
            success: function(args) {
              if (args.doInstall) {
                cloudStack.uiCustom.installWizard({
                  $container: $container,
                  context: context,
                  complete: function() {
                    $container.cloudStack(cloudStackArgs);
                  }
                });
              } else {
                // Show cloudStack main UI
                $container.cloudStack(cloudStackArgs);
              }
            }
          }
        });
      }
    });
  });
})(cloudStack, jQuery, testData);
