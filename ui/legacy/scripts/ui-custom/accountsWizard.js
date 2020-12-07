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
    cloudStack.uiCustom.accountsWizard = function(args, isLdap) {
        return function(listViewArgs) {
            var context = listViewArgs.context;
            var ldapStatus = isLdap;
            var accountsWizard = function(data) {
                var $wizard = $('#template').find('div.accounts-wizard').clone();
                var $form = $wizard.find('form');

                var close = function() {
                    $wizard.dialog('destroy');
                    $wizard.remove();
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
                            isLdap: isLdap,
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
                                    isLdap: isLdap,
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
                                isLdap: isLdap,
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
                    if ($target.closest('button.next').length) {
                        $form.validate();
                        if ($form.valid()) {
                            completeAction();
                            $(window).trigger('cloudStack.fullRefresh');
                            close();
                            return true;
                        }
                    }

                    if ($target.closest('button.cancel').length) {
                        close();
                        return false;
                    }
                });

                if (ldapStatus) {
                    var userFilter = $wizard.find('#label_filterBy').val();
                    if (userFilter == null) {
                        userFilter = 'AnyDomain';
                    }
                    var domainId = $wizard.find('#label_domain').val();
                    if (domainId == null) {
                        domainId = $.cookie('domainid');
                    }
                    var $table = $wizard.find('.ldap-account-choice tbody');
                    $("#label_ldap_group_name").on("keypress", function(event) {
                        if ($table.find("#tr-groupname-message").length === 0) {
                            $("<tr id='tr-groupname-message'>").appendTo($table).append("<td colspan=\"4\">"+_l('message.ldap.group.import')+"</td>");
                        }
                        $table.find("tr").hide();
                        $table.find("#tr-groupname-message").show();
                    });
                    $("#label_ldap_group_name").on("blur", function(event) {
                        if (!$(this).val()) {
                            $table.find("tr").show();
                            $table.find("#tr-groupname-message").hide();
                        }
                    });
                    loadList = function() { $.ajax({
                        url: createURL("listLdapUsers&listtype=new&domainid=" + domainId + "&userfilter=" + userFilter),
                        dataType: "json",
                        async: false,
                        success: function(json) {

                            $table.find('tr').remove();
                            if (json.ldapuserresponse.count > 0) {
                                $(json.ldapuserresponse.LdapUser).each(function() {
                                    var $result = $('<tr>');

                                    $result.append(
                                        $('<td>').addClass('select').append(
                                            $('<input>').attr({
                                                type: 'checkbox', name: 'username', value: _s(this.username)
                                            })
                                        ),
                                        $('<td>').addClass('name').html(_s(this.firstname) + ' ' + _s(this.lastname))
                                            .attr('title', _s(this.firstname) + ' ' + _s(this.lastname)),
                                        $('<td>').addClass('username').html(_s(this.username))
                                            .attr('title', this.username),
                                        $('<td>').addClass('email').html(_s(this.email))
                                            .attr('title', _s(this.email)),
                                        $('<td>').addClass('email').html(_s(this.conflictingusersource))
                                            .attr('title', _s(this.conflictingusersource))
                                    )

                                    $table.append($result);
                                });
                            } else {
                                var $result = $('<tr>');

                                $result.append(
                                    $('<td>').attr('colspan', 4).html(_l('label.no.data'))
                                );

                                $table.append($result);
                            }
                        }
                    }) };
                    loadList();

                } else {
                    var informationWithinLdapFields = $.extend(true,{},args.informationWithinLdap);
                    // we are not in ldap so
                    delete informationWithinLdapFields.conflictingusersource;

                    var informationWithinLdap = cloudStack.dialog.createForm({
                        context: context,
                        noDialog: true,
                        form: {
                            title: '',
                            fields: informationWithinLdapFields
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

                var informationNotInLdap = $.extend(true,{},args.informationNotInLdap);

                if (!ldapStatus) {
                    delete informationNotInLdap.filter;
                    delete informationNotInLdap.ldapGroupName;
                }

                if (g_idpList == null) {
                    delete informationNotInLdap.samlEnable;
                    delete informationNotInLdap.samlEntity;
                }

                var informationNotInLdap = cloudStack.dialog.createForm({
                    context: context,
                    noDialog: true,
                    form: {
                        title: '',
                        fields: informationNotInLdap
                    }
                });

                var informationNotInLdapForm = informationNotInLdap.$formContainer.find('form .form-item');
                informationNotInLdapForm.find('.value #label_filterBy').addClass('required');
                informationNotInLdapForm.find('.value #label_filterBy').change(function() {
                    userFilter = $wizard.find('#label_filterBy').val();
                    loadList();
                });
                informationNotInLdapForm.find('.value #label_domain').addClass('required');
                informationNotInLdapForm.find('.value #label_domain').change(function() {
                    domainId = $wizard.find('#label_domain').val();
                    loadList();
                });
                informationNotInLdapForm.find('.value #label_type').addClass('required');
                if (!ldapStatus) {
                    informationNotInLdapForm.css('background', 'none');
                }
                $wizard.find('.manual-account-details').append(informationNotInLdapForm);

                if (g_idpList && g_appendIdpDomain && !ldapStatus) {
                    var samlChecked = false;
                    var idpUrl = $wizard.find('select[name=samlEntity]').children(':selected').val();
                    var appendDomainToUsername = function() {
                        if (!g_appendIdpDomain) {
                            return;
                        }
                        var username = $wizard.find('input[name=username]').val();
                        if (username) {
                            username = username.split('@')[0];
                        }
                        if (samlChecked) {
                            var link = document.createElement('a');
                            link.setAttribute('href', idpUrl);
                            $wizard.find('input[name=username]').val(username + "@" + link.host.split('.').splice(-2).join('.'));
                        } else {
                            $wizard.find('input[name=username]').val(username);
                        }
                    };
                    $wizard.find('select[name=samlEntity]').change(function() {
                        idpUrl = $(this).children(':selected').val();
                        appendDomainToUsername();
                    });
                    $wizard.find('input[name=samlEnable]').change(function() {
                        samlChecked = $(this).context.checked;
                        appendDomainToUsername();
                    });
                }

                var $dialog = $wizard.dialog({
                    title: ldapStatus ? _l('label.add.LDAP.account') : _l('label.add.account'),
                    width: ldapStatus ? 800 : 330,
                    height: ldapStatus ? 500 : 500,
                    closeOnEscape: false
                });
                
                return cloudStack.applyDefaultZindexAndOverlayOnJqueryDialogAndRemoveCloseButton($dialog);
            };

            accountsWizard(args);
        };
    };
})(jQuery, cloudStack);
