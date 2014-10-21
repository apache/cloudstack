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
                        cloudStack.dialog.notice({
                            message: args
                        });
                    }
                }
            });

            return false;
        });

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
