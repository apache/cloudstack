(function(cloudStack, $, testData) {
  $.extend(cloudStack, testData, {
    home: 'dashboard',

    sectionPreFilter: function(args) {
      var user = args.context.users[0];

      if (user.role == 'admin')
        return args.context.sections;

      return ['dashboard', 'instances', 'storage', 'templates', 'events'];
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
      'global-settings': {},
      configuration: {}
    }
  });

  $(function() {
    var $container = $('#cloudStack3-container');
    
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

      // Show cloudStack main UI widget
      complete: function(args) {
        $container.cloudStack($.extend(cloudStack, {
          context: {
            users: [args.user]
          }
        }));
      }
    });
  });
})(cloudStack, jQuery, testData);
