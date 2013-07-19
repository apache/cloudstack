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
    cloudStack.uiCustom.physicalResources = function(args) {
        var listView = function(targetID) {
            var target = args.sections.physicalResources.listView[targetID];
            var listViewArgs = $.isFunction(target) ? target() : target;

            return $('<div>').listView(
                (listViewArgs.listView || listViewArgs.sections) ? listViewArgs : {
                    listView: listViewArgs
                }
            );
        };
        var $dashboard = $('#template').find('.system-dashboard-view').clone();
        var getData = function() {
            // Populate data
            $dashboard.find('[data-item]').hide();
            cloudStack.sections.system.dashboard.dataProvider({
                response: {
                    success: function(args) {
                        var data = args.data;
                        $.each(data, function(key, value) {
                            var $elem = $dashboard.find('[data-item=' + key + ']');
                            $elem.hide().html(value).fadeIn();
                        });
                    }
                }
            });
        };
        var resourceChart = function(args) {
            getData();
            return $dashboard
                .click(function(event) {
                    var $target = $(event.target);
                    if ($target.closest('[view-all-target]').size()) {
                        var targetID = $target.closest('[view-all-target]').attr('view-all-target');
                        args.$browser.cloudBrowser('addPanel', {
                            title: $target.closest('[view-all-title]').attr('view-all-title'),
                            data: '',
                            noSelectPanel: true,
                            maximizeIfSelected: true,
                            complete: function($newPanel) {
                                listView(targetID).appendTo($newPanel);
                            }
                        });
                    }
                });
        };
        $(window).bind('cloudStack.fullRefresh cloudStack.updateResources', function() {
            if ($dashboard.is(':visible')) {
                getData();
            }
        });
        return function(args) {
            $dashboard.find('#update_ssl_button').click(function() {
                cloudStack.dialog.createForm({
                    form: {
                        title: 'label.update.ssl',
                        desc: 'message.update.ssl',
                        fields: {
                            certificate: {
                                label: 'label.certificate',
                                isTextarea: true
                            },
                            privatekey: {
                                label: 'label.privatekey',
                                isTextarea: true
                            },
                            domainsuffix: {
                                label: 'label.domain.suffix'
                            }
                        }
                    },
                    after: function(args) {
                        var $loading = $('<div>').addClass('loading-overlay');
                        $('.system-dashboard-view:visible').prepend($loading);
                        $.ajax({
                            type: "POST",
                            url: createURL('uploadCustomCertificate'),
                            data: {
                                certificate: encodeURIComponent(args.data.certificate),
                                privatekey: encodeURIComponent(args.data.privatekey),
                                domainsuffix: args.data.domainsuffix
                            },
                            dataType: 'json',
                            success: function(json) {
                                var jid = json.uploadcustomcertificateresponse.jobid;
                                var uploadCustomCertificateIntervalID = setInterval(function() {
                                    $.ajax({
                                        url: createURL("queryAsyncJobResult&jobId=" + jid),
                                        dataType: "json",
                                        success: function(json) {
                                            var result = json.queryasyncjobresultresponse;
                                            if (result.jobstatus == 0) {
                                                return; //Job has not completed
                                            } else {
                                                clearInterval(uploadCustomCertificateIntervalID);
                                                if (result.jobstatus == 1) {
                                                    cloudStack.dialog.notice({
                                                        message: 'Update SSL Certiciate succeeded'
                                                    });
                                                } else if (result.jobstatus == 2) {
                                                    cloudStack.dialog.notice({
                                                        message: 'Failed to update SSL Certificate. ' + _s(result.jobresult.errortext)
                                                    });
                                                }
                                                $loading.remove();
                                            }
                                        },
                                        error: function(XMLHttpResponse) {
                                            cloudStack.dialog.notice({
                                                message: 'Failed to update SSL Certificate. ' + parseXMLHttpResponse(XMLHttpResponse)
                                            });
                                            $loading.remove();
                                        }
                                    });
                                }, g_queryAsyncJobResultInterval);
                            },
                            error: function(XMLHttpResponse) {
                                cloudStack.dialog.notice({
                                    message: 'Failed to update SSL Certificate. ' + parseXMLHttpResponse(XMLHttpResponse)
                                });
                                $loading.remove();
                            }
                        });
                    },
                    context: {}
                });
                return false;
            });
            $dashboard.find('#refresh_button').click(function() {
                getData();
                return false;
            });
            return resourceChart(args);
        };
    };
}(cloudStack, jQuery));
