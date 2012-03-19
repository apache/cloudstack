(function(cloudStack, $) {
  $.extend(cloudStack, {
    home: 'dashboard',

    sectionPreFilter: function(args) {
      if(isAdmin()) {
        return ["dashboard", "instances", "storage", "network", "templates", "accounts", "domains", "events", "system", "global-settings", "configuration", "projects"];
      }
      else if(isDomainAdmin()) {
        return ["dashboard", "instances", "storage", "network", "templates", "accounts", "domains", "events", "projects"];
      }
      else if (g_userProjectsEnabled) {
        return ["dashboard", "instances", "storage", "network", "templates", "accounts", "events", "projects"];
      }
      else { //normal user
        return ["dashboard", "instances", "storage", "network", "templates", "accounts", "events"];
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
      events: {},
      accounts: {},
      domains: {},
      system: {},
      projects: {},
      'global-settings': {},
      configuration: {}
    }
  });

  $(function() {
    // Get language
    g_lang = $.cookie('lang') ? $.cookie('lang') : 'en';

    /**
     * Generic error handling
     */
		
    $.ajaxSetup({
		  url: clientApiUrl,
      async: true,
      dataType: 'json',
      cache: false,
      error: function(data) {
        cloudStack.dialog.notice({ message: parseXMLHttpResponse(data) });
      },
			beforeSend: function(XMLHttpRequest) {		
				if (g_mySession == $.cookie("JSESSIONID")) {
					return true;
				} 
				else {
					cloudStack.dialog.notice({ message: _l('label.session.expired') });
					return false;
				}
			}	
    });

    var $container = $('#cloudStack3-container');

    var loginArgs = {
      $container: $container,

      // Use this for checking the session, to bypass login screen
      bypassLoginCheck: function(args) { //before login screen
			  if (g_loginResponse == null) { //not single-sign-on
					g_mySession = $.cookie('JSESSIONID');
					g_sessionKey = $.cookie('sessionKey');
					g_role = $.cookie('role');        
					g_username = $.cookie('username');
					g_userid = $.cookie('userid');
					g_account = $.cookie('account');
					g_domainid = $.cookie('domainid');
					g_userfullname = $.cookie('userfullname');		
					g_timezone = $.cookie('timezone');  
					if($.cookie('timezoneoffset') != null)
						g_timezoneoffset = isNaN($.cookie('timezoneoffset'))? null: parseFloat($.cookie('timezoneoffset'));
					else
						g_timezoneoffset = null;   
        }
				else { //single-sign-on		
					g_mySession = $.cookie('JSESSIONID');
					g_sessionKey = encodeURIComponent(g_loginResponse.sessionkey);
					g_role = g_loginResponse.type;            
					g_username = g_loginResponse.username;
					g_userid = g_loginResponse.userid;
					g_account = g_loginResponse.account;
					g_domainid = g_loginResponse.domainid;
					g_userfullname = g_loginResponse.firstname + ' ' + g_loginResponse.lastname;
					g_timezone = g_loginResponse.timezone;										
					if(g_loginResponse.timezoneoffset != null)
						g_timezoneoffset = isNaN(g_loginResponse.timezoneoffset)? null: parseFloat(g_loginResponse.timezoneoffset);
					else
						g_timezoneoffset = null;   					
				}
								
        var userValid = false;
        $.ajax({
          url: createURL("listCapabilities"),
          dataType: "json",
          async: false,
          success: function(json) {
            g_capabilities = json.listcapabilitiesresponse.capability;
						$.cookie('capabilities', g_capabilities, { expires: 1});
						
            g_supportELB = json.listcapabilitiesresponse.capability.supportELB.toString(); //convert boolean to string if it's boolean
            $.cookie('supportELB', g_supportELB, { expires: 1});
                     
            if (json.listcapabilitiesresponse.capability.userpublictemplateenabled != null) {
              g_userPublicTemplateEnabled = json.listcapabilitiesresponse.capability.userpublictemplateenabled.toString(); //convert boolean to string if it's boolean
              $.cookie('userpublictemplateenabled', g_userPublicTemplateEnabled, { expires: 1});
            }  
						
						g_userProjectsEnabled = json.listcapabilitiesresponse.capability.allowusercreateprojects;
            $.cookie('userProjectsEnabled', g_userProjectsEnabled, { expires: 1 });
						
            userValid = true;
          },
          error: function(xmlHTTP) {
            logout(false);
          },
          beforeSend : function(XMLHttpResponse) {					  
						return true;
					}	
        });

        if (userValid && isAdmin()) {
          $.ajax({
            url: createURL("listSwifts"),
            dataType: "json",
            async: false,
            success: function(json) {
              var items = json.listswiftsresponse.swift;
              if(items != null && items.length > 0)
                havingSwift = true;
            }
          });
        } else {
          havingSwift = false;
        }

        return userValid ? {
          user: {
            userid: g_userid,
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
        if (md5HashedLogin)
          password = $.md5(args.data.password);
        else
          password = todb(args.data.password);
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
          type: "POST",
          data: "command=login" + array1.join("") + "&response=json",					
          dataType: "json",
          async: false,
          success: function(json) {			
            var loginresponse = json.loginresponse;

            g_mySession = $.cookie('JSESSIONID');
            g_sessionKey = encodeURIComponent(loginresponse.sessionkey);
            g_role = loginresponse.type;            
            g_username = loginresponse.username;
            g_userid = loginresponse.userid;
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
            $.cookie('userid', g_userid, { expires: 1 });

            $.ajax({
              url: createURL("listCapabilities"),              
              dataType: "json",
              async: false,
              success: function(json) {
                g_capabilities = json.listcapabilitiesresponse.capability;
								$.cookie('capabilities', g_capabilities, { expires: 1});
								
                g_supportELB = json.listcapabilitiesresponse.capability.supportELB.toString(); //convert boolean to string if it's boolean
                $.cookie('supportELB', g_supportELB, { expires: 1});

                if (json.listcapabilitiesresponse.capability.userpublictemplateenabled != null) {
                  g_userPublicTemplateEnabled = json.listcapabilitiesresponse.capability.userpublictemplateenabled.toString(); //convert boolean to string if it's boolean
                  $.cookie('userpublictemplateenabled', g_userPublicTemplateEnabled, { expires: 1});
                }

                g_userProjectsEnabled = json.listcapabilitiesresponse.capability.allowusercreateprojects;
                $.cookie('userProjectsEnabled', g_userProjectsEnabled, { expires: 1 });

                args.response.success({
                  data: {
                    user: $.extend(true, {}, loginresponse, {
                      name: loginresponse.firstname + ' ' + loginresponse.lastname,
                      role: loginresponse.type == 1 ? 'admin' : 'user',
                      type: loginresponse.type
                    })
                  }
                });
              },
              error: function(xmlHTTP) {
                args.response.error();
              }				
            });

            if (isAdmin()) {
              $.ajax({
                url: createURL("listSwifts"),
                dataType: "json",
                async: false,
                success: function(json) {
                  var items = json.listswiftsresponse.swift;
                  if(items != null && items.length > 0)
                    havingSwift = true;
                }
              });
            } else {
              havingSwift = false;
            }

            // Get project configuration
            // TEMPORARY -- replace w/ output of capability response, etc., once implemented
            window.g_projectsInviteRequired = false;
          },
          error: function(XMLHttpRequest) {					  
						var errorMsg = parseXMLHttpResponse(XMLHttpRequest);							
            if(errorMsg.length == 0 && XMLHttpRequest.status == 0) 		
						  errorMsg = dictionary['error.unable.to.reach.management.server'];	 
            else 
              errorMsg = _l('error.invalid.username.password'); //override error message             						
            args.response.error(errorMsg);			
          },										
					beforeSend : function(XMLHttpResponse) {				
						return true;
					}	
        });
      },

      logoutAction: function(args) {
        $.ajax({
          url: createURL('logout'),
          async: false,
          success: function() {					  
						g_mySession = null;
						g_sessionKey = null;
						g_username = null;	
						g_account = null;
						g_domainid = null;	
						g_timezoneoffset = null;
						g_timezone = null;
						g_supportELB = null;
								
						$.cookie('JSESSIONID', null);
						$.cookie('sessionKey', null);
						$.cookie('username', null);
						$.cookie('account', null);
						$.cookie('domainid', null);
						$.cookie('role', null);
						$.cookie('networktype', null); 
						$.cookie('timezoneoffset', null);
						$.cookie('timezone', null);
						$.cookie('supportELB', null);
						
            document.location.reload();
          },
          error: function() {
            document.location.reload();
          },			
					beforeSend : function(XMLHttpResponse) {
						return true;
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
              if (args.doInstall && isAdmin()) {
                var initInstallWizard = function(eulaHTML) {
                  cloudStack.uiCustom.installWizard({
                    $container: $container,
                    context: context,
                    eula: eulaHTML,
                    complete: function() {
                      // Show cloudStack main UI
                      $container.cloudStack(cloudStackArgs);
                    }
                  });
                };

                initInstallWizard(loginArgs.eula);
              } else {
                // Show cloudStack main UI
                $container.cloudStack($.extend(cloudStackArgs, { hasLogo: loginArgs.eula }));
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
		
    // EULA check
    $.ajax({
      url: 'eula.' + g_lang + '.html',
      dataType: 'html',
      success: function(html) {
        cloudStack.uiCustom.login($.extend(loginArgs, { eula: html, hasLogo: true }));
      },
      error: function() {
        cloudStack.uiCustom.login(loginArgs);
      },	
			beforeSend : function(XMLHttpResponse) {
				return true;
			}		
    });

    // Localization
    cloudStack.localizationFn = function(str) {
      return dictionary[str];
    };
  });
})(cloudStack, jQuery);
