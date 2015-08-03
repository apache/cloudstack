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
            $(window).trigger('cloudStack.init');

            return;
        }

        $login.appendTo('html body');
        $('html body').addClass('login');

        // Remove label if field was auto filled
        $.each($form.find('label'), function() {
            var $label = $(this);
            var $input = $form.find('input').filter(function() {
                return $(this).attr('name') == $label.attr('for');
            });
            if ($input.val()) {
                $label.hide();
            }
        });

        // Form validation
        $form.validate();

        // Form label behavior
        $inputs.bind('keydown focus click blur', function(event) {
            var $target = $(event.target);
            var $label = $form.find('label').filter(function() {
                return $(this).attr('for') == $target.attr('name');
            });

            if (event.type == 'keydown') {
                $label.hide();

                return true;
            } else if (event.type == 'blur') {
                if ($target.hasClass('first-input')) {
                    $target.removeClass('first-input');
                }
                if (!$(this).val()) {
                    $label.show();
                }
            } else {
                if (!$target.hasClass('first-input')) {
                    $label.hide();
                }
            }

            return true;
        });

        if (!args.hasLogo) $login.addClass('nologo');

        // Labels cause related input to be focused
        $login.find('label').click(function() {
            var $input = $inputs.filter('[name=' + $(this).attr('for') + ']');
            var $label = $(this);

            $input.focus();
            $label.hide();
        });

        $inputs.filter(':first').addClass('first-input').focus();

        // Login action
        var selectedLogin = 'cloudstack';
        $login.find('#login-submit').click(function() {
            if (selectedLogin === 'cloudstack') {
                // CloudStack Local Login
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
                            cloudStack.dialog.notice({
                                message: args
                            });
                        }
                    }
                });
            } else if (selectedLogin === 'saml') {
                // SAML
                args.samlLoginAction({
                    data: {'idpid': $login.find('#login-options').find(':selected').val()}
                });
            }
            return false;
        });


        var toggleLoginView = function (selectedOption) {
            $login.find('#login-submit').show();
            if (selectedOption === '') {
                    $login.find('#cloudstack-login').hide();
                    $login.find('#login-submit').hide();
                    selectedLogin = 'none';
            } else if (selectedOption === 'cloudstack-login') {
                    $login.find('#cloudstack-login').show();
                    selectedLogin = 'cloudstack';
            } else {
                    $login.find('#cloudstack-login').hide();
                    selectedLogin = 'saml';
            }
        };

        $login.find('#login-options').change(function() {
            var selectedOption = $login.find('#login-options').find(':selected').val();
            toggleLoginView(selectedOption);
            if (selectedOption && selectedOption !== '') {
                $.cookie('login-option', selectedOption);
            }
        });

        // By Default hide login option dropdown
        $login.find('#login-dropdown').hide();
        $login.find('#login-submit').show();
        $login.find('#cloudstack-login').show();

        // If any IdP servers were set, SAML is enabled
        if (g_idpList && g_idpList.length > 0) {
            $login.find('#login-dropdown').show();
            $login.find('#login-submit').hide();
            $login.find('#cloudstack-login').hide();

            $login.find('#login-options')
                .append($('<option>', {
                    value: '',
                    text: '--- Select Identity Provider -- ',
                    selected: true
                }));

            $.each(g_idpList, function(index, idp) {
                $login.find('#login-options')
                    .append($('<option>', {
                        value: idp.id,
                        text: idp.orgName
                    }));
            });

            var loginOption = $.cookie('login-option');
            if (loginOption) {
                var option = $login.find('#login-options option[value="' + loginOption + '"]');
                if (option.length > 0) {
                    option.prop('selected', true);
                    toggleLoginView(loginOption);
                }
            }
        }

        // Select language
        var $languageSelect = $login.find('select[name=language]');
        $languageSelect.change(function() {
            if ($(this).val() != '') //language dropdown is not blank
                $.cookie('lang', $(this).val()); //the selected option in language dropdown will be used (instead of browser's default language)
            else //language dropdown is blank
                $.cookie('lang', null); //null $.cookie('lang'), so browser's default language will be used.
            document.location.reload();
        });

        $languageSelect.val($.cookie('lang'));

        // Hide login screen, mainly for SSO
        if (args.hideLoginScreen) {
            $login.children().hide();
            $login.append($('<div>').addClass('loading-overlay').append(
                $('<span>').html(
                    // _l is not set yet, so localize directly to dictionary
                    // [should fix in future]
                    dictionary['label.loading'] + '...'
                )
            ));
        }

        $(window).trigger('cloudStack.init');
    };
})(jQuery, cloudStack);
