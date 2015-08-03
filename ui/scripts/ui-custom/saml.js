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
    $(window).bind('cloudStack.ready', function() {
        var showSamlDomainSwitcher = false;
        if (g_idpList) {
            showSamlDomainSwitcher = true;
        }
        if (!showSamlDomainSwitcher) {
            return;
        }

        var $label = $('<label>').html('Domain:');
        var $header = $('#header .controls');
        var $domainSwitcher = $('<div>').addClass('domain-switcher');
        var $domainSelect = $('<select>');
        $domainSwitcher.append($label, $domainSelect);

        var switchAccount = function(userId, domainId) {
            var toReload = true;
            $.ajax({
                url: createURL('listAndSwitchSamlAccount'),
                type: 'POST',
                async: false,
                data: {
                    userid: userId,
                    domainid: domainId
                },
                success: function(data, textStatus) {
                    document.location.reload(true);
                },
                error: function(data) {
                    cloudStack.dialog.notice({
                        message: parseXMLHttpResponse(data)
                    });
                    if (data.status !== 200) {
                        toReload = false;
                    }
                },
                complete: function() {
                    if (toReload) {
                        document.location.reload(true);
                    }
                    toReload = true;
                }
            });
        };

        $domainSelect.change(function() {
            var selectedOption = $domainSelect.val();
            var userId = selectedOption.split('/')[0];
            var domainId = selectedOption.split('/')[1];
            switchAccount(userId, domainId);
        });

        $.ajax({
            url: createURL('listAndSwitchSamlAccount'),
            success: function(json) {
                var accounts = json.listandswitchsamlaccountresponse.samluseraccount;
                if (accounts.length < 2) {
                    return;
                };
                $domainSelect.empty();
                for (var i = 0; i < accounts.length; i++) {
                    var option = $('<option>');
                    option.data("userId", accounts[i].userId);
                    option.data("domainId", accounts[i].domainId);
                    option.val(accounts[i].userId + '/' + accounts[i].domainId);
                    option.html(accounts[i].accountName + "/" + accounts[i].domainName);
                    option.appendTo($domainSelect);
                }
                var currentAccountDomain = g_userid + '/' + g_domainid;
                $domainSelect.find('option[value="' + currentAccountDomain + '"]').attr("selected", "selected");
                $domainSwitcher.insertAfter($header.find('.region-switcher'));
            },
            error: function(data) {
                // if call fails, the logged in user in not a SAML authenticated user
            }
        });
    });
}(jQuery, cloudStack));
