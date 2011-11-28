(function(cloudStack, $, testData) {
  $.extend(window.cloudStack, testData, {
    home: 'dashboard',

    sectionPreFilter: function(args) {
      if(isAdmin()) {
        return ["dashboard", "instances", "storage", "network", "templates", "accounts", "domains", "events", "system", "global-settings", "configuration", "projects"];
      }
      else if(isDomainAdmin()) {
        return ["dashboard", "instances", "storage", "network", "templates", "accounts", "domains", "events", "projects"];
      }
      else { //normal user
        return ["dashboard", "instances", "storage", "network", "templates", "events", "projects"];
      }
    },
    sections: {
      /**
       * Dashboard
       */
      dashboard: {},
      //'dashboard-user': {},
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

    var loginArgs = {
      $container: $container,

      // Use this for checking the session, to bypass login screen
      bypassLoginCheck: function(args) {
        g_mySession = $.cookie("JSESSIONID");
        g_sessionKey = $.cookie("sessionKey");
        g_role = $.cookie("role");
        g_username = $.cookie("username");
        g_account = $.cookie("account");
        g_domainid = $.cookie("domainid");
        g_timezone = $.cookie("timezone");
        g_directAttachSecurityGroupsEnabled = $.cookie("directattachsecuritygroupsenabled");
        g_userPublicTemplateEnabled = $.cookie("userpublictemplateenabled");
        g_userfullname = $.cookie('userfullname');

        if($.cookie("timezoneoffset") != null)
          g_timezoneoffset = isNaN($.cookie("timezoneoffset"))?null: parseFloat($.cookie("timezoneoffset"));
        else
          g_timezoneoffset = null;

        if (g_directAttachSecurityGroupsEnabled == null || g_directAttachSecurityGroupsEnabled.length == 0)
          g_directAttachSecurityGroupsEnabled = "false";

        if (g_userPublicTemplateEnabled == null || g_userPublicTemplateEnabled.length == 0)
          g_userPublicTemplateEnabled = "true";

        if(g_supportELB == null)
          g_supportELB = $.cookie("supportELB");

        if(g_firewallRuleUiEnabled == null)
          g_firewallRuleUiEnabled = $.cookie("firewallRuleUiEnabled");

        var userValid = false;

        $.ajax({
          url: createURL("listCapabilities"),
          dataType: "json",
          async: false,
          success: function(json) {
            /* g_supportELB: "guest"   â€” ips are allocated on guest network (so use 'forvirtualnetwork' = false)
             * g_supportELB: "public"  - ips are allocated on public network (so use 'forvirtualnetwork' = true)
             * g_supportELB: "false"   â€“ no ELB support
             */
            g_supportELB = json.listcapabilitiesresponse.capability.supportELB.toString(); //convert boolean to string if it's boolean
            $.cookie('supportELB', g_supportELB, { expires: 1});

            g_firewallRuleUiEnabled = json.listcapabilitiesresponse.capability.firewallRuleUiEnabled.toString(); //convert boolean to string if it's boolean
            $.cookie('firewallRuleUiEnabled', g_firewallRuleUiEnabled, { expires: 1});

            if (json.listcapabilitiesresponse.capability.userpublictemplateenabled != null) {
              g_userPublicTemplateEnabled = json.listcapabilitiesresponse.capability.userpublictemplateenabled.toString(); //convert boolean to string if it's boolean
              $.cookie('userpublictemplateenabled', g_userPublicTemplateEnabled, { expires: 1});
            }

            if (json.listcapabilitiesresponse.capability.securitygroupsenabled != null) {
              g_directAttachSecurityGroupsEnabled = json.listcapabilitiesresponse.capability.securitygroupsenabled.toString(); //convert boolean to string if it's boolean
              $.cookie('directattachsecuritygroupsenabled', g_directAttachSecurityGroupsEnabled, { expires: 1});
            }

            userValid = true;
          },
          error: function(xmlHTTP) {
            logout(false);
          },
          beforeSend: function(xmlHTTP) {
            return true;
          }
        });

        return userValid ? {
          user: {
            username: g_username,
            account: g_account,
            name: g_userfullname,
            role: g_role,
            domainid: g_domainid
          }
        } : false;

        return testAddUser;
      },

      // Actual login process, via form
      loginAction: function(args) {
        var array1 = [];
        array1.push("&username=" + encodeURIComponent(args.data.username));

        var password;
        if (md5Hashed)
          password = $.md5(args.data.password);
        else
          password = args.data.password;
        array1.push("&password=" + password);

        var domain;
        if(args.data.domain != null && args.data.domain.length > 0) {
          if (args.data.domain.charAt(0) != "/")
            domain = "/" + args.data.domain;
          else
            domain = args.data.domain;
          array1.push("&domain=" + encodeURIComponent(domain));
        }
        else {
          array1.push("&domain=" + encodeURIComponent("/"));
        }

        $.ajax({
          //type: "POST",
          url: createURL("login") + array1.join(""),
          dataType: "json",
          async: false,
          success: function(json) {
            var loginresponse = json.loginresponse;

            g_mySession = $.cookie('JSESSIONID');
            g_sessionKey = encodeURIComponent(loginresponse.sessionkey);
            g_role = loginresponse.type;
            g_username = loginresponse.username;
            g_account = loginresponse.account;
            g_domainid = loginresponse.domainid;
            g_timezone = loginresponse.timezone;
            g_timezoneoffset = loginresponse.timezoneoffset;
            g_userfullname = loginresponse.firstname + ' ' + loginresponse.lastname;

            $.cookie('sessionKey', g_sessionKey, { expires: 1});
            $.cookie('username', g_username, { expires: 1});
            $.cookie('account', g_account, { expires: 1});
            $.cookie('domainid', g_domainid, { expires: 1});
            $.cookie('role', g_role, { expires: 1});
            $.cookie('timezoneoffset', g_timezoneoffset, { expires: 1});
            $.cookie('timezone', g_timezone, { expires: 1});
            $.cookie('userfullname', g_userfullname, { expires: 1 });

            $.ajax({
              url: createURL("listCapabilities"),
              //url: "command=/client/api?listCapabilities&sessionkey="+g_sessionKey,
              dataType: "json",
              async: false,
              success: function(json) {
                /* g_supportELB: "guest"   — ips are allocated on guest network (so use 'forvirtualnetwork' = false)
                 * g_supportELB: "public"  - ips are allocated on public network (so use 'forvirtualnetwork' = true)
                 * g_supportELB: "false"   – no ELB support
                 */
                g_supportELB = json.listcapabilitiesresponse.capability.supportELB.toString(); //convert boolean to string if it's boolean
                $.cookie('supportELB', g_supportELB, { expires: 1});

                g_firewallRuleUiEnabled = json.listcapabilitiesresponse.capability.firewallRuleUiEnabled.toString(); //convert boolean to string if it's boolean
                $.cookie('firewallRuleUiEnabled', g_firewallRuleUiEnabled, { expires: 1});

                if (json.listcapabilitiesresponse.capability.userpublictemplateenabled != null) {
                  g_userPublicTemplateEnabled = json.listcapabilitiesresponse.capability.userpublictemplateenabled.toString(); //convert boolean to string if it's boolean
                  $.cookie('userpublictemplateenabled', g_userPublicTemplateEnabled, { expires: 1});
                }

                if (json.listcapabilitiesresponse.capability.securitygroupsenabled != null) {
                  g_directAttachSecurityGroupsEnabled = json.listcapabilitiesresponse.capability.securitygroupsenabled.toString(); //convert boolean to string if it's boolean
                  $.cookie('directattachsecuritygroupsenabled', g_directAttachSecurityGroupsEnabled, { expires: 1});
                }

                args.response.success({
                  data: {
                    user: $.extend(true, {}, loginresponse, {
                      name: loginresponse.firstname + ' ' + loginresponse.lastname,
                      role: loginresponse.type == 1 ? 'admin' : 'user'
                    })
                  }
                });
              },
              error: function(xmlHTTP) {
                args.response.error();
              }
            });

            // Get project configuration
            // TEMPORARY -- replace w/ output of capability response, etc., once implemented
            window.g_projectsInviteRequired = false;
          },
          error: function() {
            args.response.error();
          }
        });
      },

      logoutAction: function(args) {
        $.ajax({
          url: createURL('logout'),
          async: false,
          success: function() {
            document.location.reload();
          },
          error: function() {
            document.location.reload();
          }
        });

      },

      // Show cloudStack main UI widget
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
                    // Show cloudStack main UI
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

        // Logout action
        $('#user-options a').live('click', function() {
          loginArgs.logoutAction({
            context: cloudStack.context
          });
        });
      }
    };

    cloudStack.uiCustom.login(loginArgs);
  });
})(cloudStack, jQuery, testData);
