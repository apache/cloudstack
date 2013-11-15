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
    cloudStack.uiCustom.accountsWizard = function(args) {
        return function(listViewArgs) {
            var context = listViewArgs.context;
            var ldapStatus = isLdapEnabled();
            var accountsWizard = function(data) {
                var $wizard = $('#template').find('div.accounts-wizard').clone();
                var $form = $wizard.find('form');

                var close = function() {
                    $wizard.dialog('destroy');
                    $('div.overlay').fadeOut(function() {
                        $('div.overlay').remove();
                    });
                };

                var completeAction = function() {
                    var data = cloudStack.serializeForm($form);
                    var groupname = $.trim(data.ldapGroupName);
                    if (groupname) {
                        args.action({
                            context: context,
                            data: data,
                            groupname: groupname,
                            response: {
                                error: function(message) {
                                    if (message) {
                                        cloudStack.dialog.notice({
                                            message: message
                                        });
                                    }
                                }
                            }
                        });
                    } else {
                        var username = data.username;
                        var bulkAdd = (username instanceof Array);
                        if (bulkAdd) {
                            for (var i = 0; i < username.length; i++) {
                                args.action({
                                    context: context,
                                    data: data,
                                    username: username[i],
                                    response: {
                                        error: function(message) {
                                            if (message) {
                                                cloudStack.dialog.notice({
                                                    message: message
                                                });
                                            }
                                        }
                                    }
                                });
                            }
                        } else {
                            args.action({
                                context: context,
                                data: data,
                                username: username,
                                response: {
                                    error: function(message) {
                                        if (message) {
                                            cloudStack.dialog.notice({
                                                message: message
                                            });
                                        }
                                    }
                                }
                            });
                        }
                    }
                };

                $wizard.click(function(event) {
                    var $target = $(event.target);
                    if ($target.closest('button.next').size()) {
                        $form.validate();
                        if ($form.valid()) {
                            completeAction();
                            $(window).trigger('cloudStack.fullRefresh');
                            close();
                            return true;
                        }
                    }

                    if ($target.closest('button.cancel').size()) {
                        close();
                        return false;
                    }
                });

                if (ldapStatus) {
                    var $table = $wizard.find('.ldap-account-choice tbody');
                    $("#label_ldap_group_name").live("keypress", function(event) {
                        if ($table.find("#tr-groupname-message").length === 0) {
                            $("<tr id='tr-groupname-message'>").appendTo($table).append("<td colspan=\"4\">All The users from the given group name will be imported</td>");
                        }
                        $table.find("tr").hide();
                        $table.find("#tr-groupname-message").show();
                    });
                    $("#label_ldap_group_name").live("blur", function(event) {
                        if (!$(this).val()) {
                            $table.find("tr").show();
                            $table.find("#tr-groupname-message").hide();
                        }
                    });
                    $.ajax({
                        url: createURL("listLdapUsers&listtype=new"),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                            if (json.ldapuserresponse.count > 0) {
                                $(json.ldapuserresponse.LdapUser).each(function() {
                                    var result = $("<tr>");
                                    result.append("<td><input type=\"checkbox\" name=\"username\" value=\"" + this.username + "\"></td>");
                                    result.append("<td>" + this.firstname + " " + this.lastname + "</td>");
                                    result.append("<td>" + this.username + "</td>");
                                    result.append("<td>" + this.email + "</td>");
                                    $table.append(result);
                                });
                            } else {
                                var result = $("<tr>");
                                result.append("<td colspan=\"4\">No data to show</td>");
                                $table.append(result);
                            }
                        }
                    });
                } else {
                    var informationWithinLdap = cloudStack.dialog.createForm({
                        context: context,
                        noDialog: true,
                        form: {
                            title: '',
                            fields: args.informationWithinLdap
                        }
                    });

                    var informationWithinLdapForm = informationWithinLdap.$formContainer.find('form .form-item');
                    informationWithinLdapForm.find('.value #label_username').addClass('required');
                    informationWithinLdapForm.find('.value #password').addClass('required');
                    informationWithinLdapForm.find('.value #label_confirm_password').addClass('required');
                    informationWithinLdapForm.find('.value #label_confirm_password').attr('equalTo', '#password');
                    informationWithinLdapForm.find('.value #label_email').addClass('required');
                    informationWithinLdapForm.find('.value #label_first_name').addClass('required');
                    informationWithinLdapForm.find('.value #label_last_name').addClass('required');
                    $wizard.find('.manual-account-details').append(informationWithinLdapForm).children().css('background', 'none');
                    $wizard.find('.ldap-account-choice').css('display', 'none');
                    $wizard.removeClass('multi-wizard');
                }

                if (!ldapStatus) {
                    delete args.informationNotInLdap.ldapGroupName;
                }

                var informationNotInLdap = cloudStack.dialog.createForm({
                    context: context,
                    noDialog: true,
                    form: {
                        title: '',
                        fields: args.informationNotInLdap
                    }
                });

                var informationNotInLdapForm = informationNotInLdap.$formContainer.find('form .form-item');
                informationNotInLdapForm.find('.value #label_domain').addClass('required');
                informationNotInLdapForm.find('.value #label_type').addClass('required');
                if (!ldapStatus) {
                    informationNotInLdapForm.css('background', 'none');
                }
                $wizard.find('.manual-account-details').append(informationNotInLdapForm);

                return $wizard.dialog({
                    title: _l('label.add.account'),
                    width: ldapStatus ? 800 : 330,
                    height: ldapStatus ? 500 : 500,
                    closeOnEscape: false,
                    zIndex: 5000
                }).closest('.ui-dialog').overlay();
            };

            accountsWizard(args);
        };
    };
})(jQuery, cloudStack);