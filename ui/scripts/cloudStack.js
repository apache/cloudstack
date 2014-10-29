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
(function(cloudStack, $) {
    $.extend(cloudStack, {
        home: 'dashboard',

        sectionPreFilter: function(args) {
            var sections = [];

            if (isAdmin()) {
                sections = ["dashboard", "instances", "storage", "network", "templates", "accounts", "domains", "events", "system", "global-settings", "configuration", "projects", "regions", "affinityGroups"];
            } else if (isDomainAdmin()) {
                sections = ["dashboard", "instances", "storage", "network", "templates", "accounts", "domains", "events", "projects", "regions", "affinityGroups"];
            } else if (g_userProjectsEnabled) {
                sections = ["dashboard", "instances", "storage", "network", "templates", "accounts", "events", "projects", "regions", "affinityGroups"];
            } else { //normal user
                sections = ["dashboard", "instances", "storage", "network", "templates", "accounts", "events", "regions", "affinityGroups"];
            }

            if (cloudStack.plugins.length) {
                sections.push('plugins');
            }

            return sections;
        },
        sections: {
            /**
             * Dashboard
             */
            dashboard: {},
            instances: {},
            affinityGroups: {},
            storage: {},
            network: {},
            templates: {},
            events: {},
            projects: {},
            accounts: {},

            domains: {}, //domain-admin and root-admin only

            regions: {}, //root-admin only
            system: {}, //root-admin only
            'global-settings': {}, //root-admin only
            configuration: {}, //root-admin only
            plugins: {}
        }
    });

    $(window).bind('cloudStack.pluginReady', function() {
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
                var clickAction = false;
                if (isValidJsonString(data.responseText)) {
                    var json = JSON.parse(data.responseText);
                    if (json != null) {
                        var property;
                        for (property in json) {}
                        var errorObj = json[property];
                        if (errorObj.errorcode == 401 && errorObj.errortext == "unable to verify user credentials and/or request signature") {
                            clickAction = function() {
                                $('#user-options a').eq(0).trigger('click');
                            };
                        }
                    }
                }
                cloudStack.dialog.notice({
                    message: parseXMLHttpResponse(data),
                    clickAction: clickAction
                });
            },
            beforeSend: function(XMLHttpRequest) {
                if (g_mySession == $.cookie("JSESSIONID")) {
                    return true;
                } else {
                    var clickAction = function() {
                        $('#user-options a').eq(0).trigger('click');
                    };

                    if ($('.notification-box:visible').size()) {
                        $('.notification-box, div.overlay:first').remove();
                    }

                    cloudStack.dialog.notice({
                        message: _l('label.session.expired'),
                        clickAction: clickAction
                    }).closest('.ui-dialog').overlay();

                    return false;
                }
            }
        });

        var $container = $('#cloudStack3-container');

        var loginArgs = {
            $container: $container,

            // Use this for checking the session, to bypass login screen
            bypassLoginCheck: function(args) { //determine to show or bypass login screen
                if (g_loginResponse == null) { //show login screen
                    /*
           but if this is a 2nd browser window (of the same domain), login screen still won't show because $.cookie('sessionKey') is valid for 2nd browser window (of the same domain) as well.
           i.e. calling listCapabilities API with g_sessionKey from $.cookie('sessionKey') will succeed,
           then userValid will be set to true, then an user object (instead of "false") will be returned, then login screen will be bypassed.
           */
                    var unBoxCookieValue = function (cookieName) {
                        var cookieValue = $.cookie(cookieName);
                        if (cookieValue && cookieValue.length > 2 && cookieValue[0] === '"' && cookieValue[cookieValue.length-1] === '"') {
                            cookieValue = cookieValue.slice(1, cookieValue.length-1);
                            $.cookie(cookieName, cookieValue, { expires: 1 });
                        }
                        return cookieValue;
                    };
                    g_mySession = $.cookie('JSESSIONID');
                    g_sessionKey = unBoxCookieValue('sessionKey');
                    g_role = unBoxCookieValue('role');
                    g_userid = unBoxCookieValue('userid');
                    g_domainid = unBoxCookieValue('domainid');
                    g_account = unBoxCookieValue('account');
                    g_username = unBoxCookieValue('username');
                    g_userfullname = unBoxCookieValue('userfullname');
                    g_timezone = unBoxCookieValue('timezone');                    
                } else { //single-sign-on	(bypass login screen)
                    g_mySession = $.cookie('JSESSIONID');
                    g_sessionKey = encodeURIComponent(g_loginResponse.sessionkey);
                    g_role = g_loginResponse.type;
                    g_username = g_loginResponse.username;
                    g_userid = g_loginResponse.userid;
                    g_account = g_loginResponse.account;
                    g_domainid = g_loginResponse.domainid;
                    g_userfullname = g_loginResponse.firstname + ' ' + g_loginResponse.lastname;
                    g_timezone = g_loginResponse.timezone;                    
                }

                var userValid = false;
                $.ajax({
                    url: createURL("listCapabilities"),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                        g_capabilities = json.listcapabilitiesresponse.capability;                        
                        g_supportELB = json.listcapabilitiesresponse.capability.supportELB.toString(); //convert boolean to string if it's boolean   
                        g_kvmsnapshotenabled = json.listcapabilitiesresponse.capability.kvmsnapshotenabled; //boolean          
                        g_regionsecondaryenabled = json.listcapabilitiesresponse.capability.regionsecondaryenabled; //boolean    
                        if (json.listcapabilitiesresponse.capability.userpublictemplateenabled != null) {
                            g_userPublicTemplateEnabled = json.listcapabilitiesresponse.capability.userpublictemplateenabled.toString(); //convert boolean to string if it's boolean                            
                        }
                        g_userProjectsEnabled = json.listcapabilitiesresponse.capability.allowusercreateprojects;
                       

                        g_cloudstackversion = json.listcapabilitiesresponse.capability.cloudstackversion;

                        if (json.listcapabilitiesresponse.capability.apilimitinterval != null && json.listcapabilitiesresponse.capability.apilimitmax != null) {
                            var intervalLimit = ((json.listcapabilitiesresponse.capability.apilimitinterval * 1000) / json.listcapabilitiesresponse.capability.apilimitmax) * 3; //multiply 3 to be on safe side
                            //intervalLimit = 9999; //this line is for testing only, comment it before check in
                            if (intervalLimit > g_queryAsyncJobResultInterval)
                                g_queryAsyncJobResultInterval = intervalLimit;
                        }

                        userValid = true;
                    },
                    error: function(xmlHTTP) { //override default error handling, do nothing instead of showing error "unable to verify user credentials" on login screen
                    },
                    beforeSend: function(XMLHttpResponse) {
                        return true;
                    }
                });
               
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
                if (args.data.domain != null && args.data.domain.length > 0) {
                    if (args.data.domain.charAt(0) != "/")
                        domain = "/" + args.data.domain;
                    else
                        domain = args.data.domain;
                    array1.push("&domain=" + encodeURIComponent(domain));
                } else {
                    array1.push("&domain=" + encodeURIComponent("/"));
                }

                var loginCmdText = array1.join("");

                $.ajax({
                    type: "POST",
                    data: "command=login" + loginCmdText + "&response=json",
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
                        g_userfullname = loginresponse.firstname + ' ' + loginresponse.lastname;

                        $.cookie('sessionKey', g_sessionKey, {
                            expires: 1
                        });
                        $.cookie('username', g_username, {
                            expires: 1
                        });
                        $.cookie('account', g_account, {
                            expires: 1
                        });
                        $.cookie('domainid', g_domainid, {
                            expires: 1
                        });
                        $.cookie('role', g_role, {
                            expires: 1
                        });                        
                        $.cookie('timezone', g_timezone, {
                            expires: 1
                        });
                        $.cookie('userfullname', g_userfullname, {
                            expires: 1
                        });
                        $.cookie('userid', g_userid, {
                            expires: 1
                        });

                        $.ajax({
                            url: createURL("listCapabilities"),
                            dataType: "json",
                            async: false,
                            success: function(json) {
                                g_capabilities = json.listcapabilitiesresponse.capability;
                                g_supportELB = json.listcapabilitiesresponse.capability.supportELB.toString(); //convert boolean to string if it's boolean      
                                g_kvmsnapshotenabled = json.listcapabilitiesresponse.capability.kvmsnapshotenabled; //boolean           
                                g_regionsecondaryenabled = json.listcapabilitiesresponse.capability.regionsecondaryenabled; //boolean                               
                                if (json.listcapabilitiesresponse.capability.userpublictemplateenabled != null) {
                                    g_userPublicTemplateEnabled = json.listcapabilitiesresponse.capability.userpublictemplateenabled.toString(); //convert boolean to string if it's boolean                                    
                                }
                                g_userProjectsEnabled = json.listcapabilitiesresponse.capability.allowusercreateprojects;
                                

                                g_cloudstackversion = json.listcapabilitiesresponse.capability.cloudstackversion;

                                if (json.listcapabilitiesresponse.capability.apilimitinterval != null && json.listcapabilitiesresponse.capability.apilimitmax != null) {
                                    var intervalLimit = ((json.listcapabilitiesresponse.capability.apilimitinterval * 1000) / json.listcapabilitiesresponse.capability.apilimitmax) * 3; //multiply 3 to be on safe side
                                    //intervalLimit = 8888; //this line is for testing only, comment it before check in
                                    if (intervalLimit > g_queryAsyncJobResultInterval)
                                        g_queryAsyncJobResultInterval = intervalLimit;
                                }

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
                       
                        // Get project configuration
                        // TEMPORARY -- replace w/ output of capability response, etc., once implemented
                        window.g_projectsInviteRequired = false;
                    },
                    error: function(XMLHttpRequest) {
                        var errorMsg = parseXMLHttpResponse(XMLHttpRequest);
                        if (errorMsg.length == 0 && XMLHttpRequest.status == 0)
                            errorMsg = dictionary['error.unable.to.reach.management.server'];
                        else
                            errorMsg = _l('error.invalid.username.password'); //override error message
                        args.response.error(errorMsg);
                    },
                    beforeSend: function(XMLHttpResponse) {
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
                        g_kvmsnapshotenabled = null;
                        g_regionsecondaryenabled = null;
                        g_loginCmdText = null;

                        $.cookie('JSESSIONID', null);
                        $.cookie('sessionKey', null);
                        $.cookie('username', null);
                        $.cookie('account', null);
                        $.cookie('domainid', null);
                        $.cookie('role', null);  
                        $.cookie('timezone', null);
                        
                        if (onLogoutCallback()) { //onLogoutCallback() will set g_loginResponse(single-sign-on variable) to null, then bypassLoginCheck() will show login screen.
                            document.location.reload(); //when onLogoutCallback() returns true, reload the current document.
                        }
                    },
                    error: function() {
                        if (onLogoutCallback()) { //onLogoutCallback() will set g_loginResponse(single-sign-on variable) to null, then bypassLoginCheck() will show login screen.
                            document.location.reload(); //when onLogoutCallback() returns true, reload the current document.
                        }
                    },
                    beforeSend: function(XMLHttpResponse) {
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
                                var initInstallWizard = function() {
                                    cloudStack.uiCustom.installWizard({
                                        $container: $container,
                                        context: context,
                                        complete: function() {
                                            // Show cloudStack main UI
                                            $container.cloudStack($.extend(cloudStackArgs, {
                                                hasLogo: false
                                            }));
                                        }
                                    });
                                };

                                initInstallWizard();
                            } else {
                                // Show cloudStack main UI
                                $container.cloudStack($.extend(cloudStackArgs, {
                                    hasLogo: false
                                }));
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

                window._reloadUI = function() {
                    $('#container').html('');
                    $('#container').cloudStack(window.cloudStack);
                };
            }
        };

        if ($.urlParam('loginUrl') != 0) {
            // SSO
            loginArgs.hideLoginScreen = true;
        }

        // Localization
        if (!$.isFunction(cloudStack.localizationFn)) { // i.e., localize is overridden by a plugin/module
            cloudStack.localizationFn = function(str) {
                return dictionary[str];
            };
        }
        
        //added for dictionary split up
        if (dictionary != undefined && dictionary2 != undefined) {
            $.extend(dictionary,dictionary2);
        }

        // Localize validation messages
        cloudStack.localizeValidatorMessages();

        cloudStack.uiCustom.login(loginArgs);

        document.title = _l('label.app.name');            
    });
})(cloudStack, jQuery);
