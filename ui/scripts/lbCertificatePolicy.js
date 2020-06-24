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
    cloudStack.lbCertificatePolicy = {
        dialog: function(args) {
            return function(args) {
                var success = args.response.success;
                var context = args.context;

                var certid = {
                    certificate: {
                        label: 'label.certificate.name',
                        required: true,
                        select: function(args) {
                            var data = {};
                            var item = {};

                            if (context != null) {
                                if (context.networks != null) {
                                    $.extend(data, {account: context.networks[0].account});
                                    $.extend(data, {domain: context.networks[0].domain});
                                }
                            }

                            $.extend(data, {
                                details: 'min'
                            });

                            $.ajax({
                                url: createURL('listAccounts'),
                                async: false,
                                data: data,
                                success: function(json) {
                                    var items = json.listaccountsresponse.account;
                                    $.extend(item, {accountid: items[0].id});
                                }
                            });

                            $.ajax({
                                url: createURL('listSslCerts'),
                                async: false,
                                data: item,
                                success: function(json) {
                                    if(!json.listsslcertsresponse || !json.listsslcertsresponse.sslcert){
                                        args.response.success({
                                            data: {id: 'No certificates ID', 
                                                    description: 'No certificates found'}
                                        });
                                        return;
                                    }
                                    var items = json.listsslcertsresponse.sslcert;
                                    var data = [{
                                        id: 'None',
                                        description: '---None---'
                                    }];
                                    $.map(items, function(item) {
                                        data.push({
                                            id: item.id,
                                            description: item.name
                                        });
                                    });
                                    args.response.success({
                                        data: data
                                    });
                                }
                            });
                        }
                    },
                    forced: {
                       label: 'Force Assign',
                       isBoolean: true,
                       isChecked: false
                    }
                };

                var $item = args.$item;

                cloudStack.dialog.createForm({
                    form: {
                        title: 'Configure Certificate',
                        desc: 'Please complete the following fields',
                        fields: certid,
                        preFilter: function(args) {
                            if ($item) {
                                var certId = $item.data('multi-custom-data').id;
                                var $cert = args.$form.find('select[name=certificate]');
                                if (certId) {
                                    $cert.val(certId);
                                } else {
                                    $cert.val("None");
                                }
                            }
                        }
                    },
                    after: function(args) {
                        // Remove fields not applicable to certificate
                        args.$form.find('.form-item:hidden').remove();

                        var data = cloudStack.serializeForm(args.$form);

                        /* $item indicates that this is an existing certificate;
                           remove it and assign new certificate */
                        if ($item) {
                            var $loading = $('<div>').addClass('loading-overlay');

                            $loading.prependTo($item);
                            cloudStack.lbCertificatePolicy.actions.recreate(
                                $item.data('multi-custom-data').id,
                                $item.data('multi-custom-data').lbRuleID,
                                data,
                                function() { // Complete
                                    $(window).trigger('cloudStack.fullRefresh');
                                },
                                function(error) { // Error
                                    $(window).trigger('cloudStack.fullRefresh');
                                }
                            );
                        } else {
                            success({
                                data: data
                            });
                        }
                    }
                });
            };
        },

        actions: {
            add: function(lbRuleID, data, complete, error) {

                $.ajax({
                    url: createURL('assignCertToLoadBalancer'),
                    data: {certid: data.certificate, lbruleid: lbRuleID, forced: data.forced == "on"},
                    success: function(json) {
                        cloudStack.ui.notifications.add({
                                desc: 'Add new LB Certificate',
                                section: 'Network',
                                poll: pollAsyncJobResult,
                                _custom: {
                                    jobId: json.assigncerttoloadbalancerresponse.jobid
                                }
                            },
                            complete, {},
                            error, {}
                        );
                    },
                    error: function(json) {
                        complete();
                        cloudStack.dialog.notice({
                            message: parseXMLHttpResponse(json)
                        });
                    }
                });
            },
            'delete': function(lbRuleID, complete, error) {
                $.ajax({
                    url: createURL('removeCertFromLoadBalancer'),
                    data: {
                        lbruleid: lbRuleID
                    },
                    success: function(json) {
                        cloudStack.ui.notifications.add({
                                desc: 'Remove previous LB Certificate',
                                section: 'Network',
                                poll: pollAsyncJobResult,
                                _custom: {
                                    jobId: json.removecertfromloadbalancerresponse.jobid
                                }
                            },
                            complete, {},
                            error, {}
                        );
                    },
                    error: function(json) {
                        complete();
                        cloudStack.dialog.notice({
                            message: parseXMLHttpResponse(json)
                        });
                    }
                });
            },
            recreate: function(certId, lbRuleID, data, complete, error) {
                var addCertificatePolicy = function() {
                    cloudStack.lbCertificatePolicy.actions.add(
                        lbRuleID,
                        data,
                        complete,
                        error
                    );
                };

                if (certId && certId == data.certificate) {
                    cloudStack.dialog.notice({
                      message: _l('Certificate is not changed')
                    });
                    $(window).trigger('cloudStack.fullRefresh');
                } else if (certId == null && data.certificate == 'None') {
                    cloudStack.dialog.notice({
                      message: _l('Certificate is not assigned')
                    });
                    $(window).trigger('cloudStack.fullRefresh');
                } else if (certId && data.certificate == 'None') {
                    // Delete existing certificate
                    cloudStack.lbCertificatePolicy.actions['delete'](lbRuleID, complete, error);
                } else {
                    addCertificatePolicy();
                }
            }
        }
    };
}(jQuery, cloudStack));
