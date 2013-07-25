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
                    args.action({
                        context: context,
                        data: data,
                        response: {
                            success: function(args) {
                                $('.list-view').listView('refresh');
                                close();
                            },
                            error: function(message) {
                                close();
                                if(message) {
                                    cloudStack.dialog.notice({
                                        message: message
                                    });
                                }
                            }
                        }
                    });
                }

                $wizard.click(function(event) {
                    var $target = $(event.target);
                    if ($target.closest('div.button.next').size()) {
                        $form.validate();
                        if ($form.valid()) {
                            completeAction();
                            return true;
                        } else {
                            return false;
                        }
                    }

                    if ($target.closest('div.button.cancel').size()) {
                        close();
                        return false;
                    }
                });

                var form = cloudStack.dialog.createForm({
                    context: context,
                    noDialog: true,
                    form: {
                        title: '',
                        fields: args.manuallyInputtedAccountInformation
                    }
                });

                var $manualDetails = form.$formContainer.find('form .form-item');
                $wizard.find('.manual-account-details').append($manualDetails);

                var $table = $wizard.find('.ldap-account-choice tbody');

                $.ajax({
                    url: createURL("listAllLdapUsers"),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                        $(json.ldapuserresponse.LdapUser).each(function() {
                            var result = $("<tr>");
                            result.append("<td><input type=\"radio\" class=\"required\" name=\"username\" value=\"" + this.username + "\"></td>");
                            result.append("<td>" + this.firstname + " " + this.lastname + "</td>");
                            result.append("<td>" + this.username + "</td>");
                            result.append("<td>" + this.email + "</td>");
                            $table.append(result);
                        })
                    }
                });

                return $wizard.dialog({
                    title: _l('label.add.account'),
                    width: 800,
                    height: 500,
                    closeOnEscape: false,
                    zIndex: 5000
                }).closest('.ui-dialog').overlay();
            }

            accountsWizard(args);
        };
    };
})(jQuery, cloudStack);
