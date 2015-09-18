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

                        // Socket info
                        var $socketInfo = $dashboard.find('.socket-info ul');
                        $socketInfo.find('li').remove(); // Clean up
                        $(args.data.socketInfo).each(function() {
                            var item = this;
                            var name = item.name;
                            var hosts = item.hosts;
                            var sockets = item.sockets;

                            var $li = $('<li>').append(
                                $('<div>').addClass('name').html(name),
                                $('<div>').addClass('hosts').append(
                                    $('<div>').addClass('title').html(_l('label.hosts')),
                                    $('<div>').addClass('value').html(hosts)
                                ),
                                $('<div>').addClass('sockets').append(
                                    $('<div>').addClass('title').html(_l('label.sockets')),
                                    $('<div>').addClass('value').html(sockets)
                                )
                            );

                            $li.appendTo($socketInfo);
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
                        preFilter: function (args) {
                            var $form = args.$form;

                            // insert the "Add intermediate certificate" button
                            var $addButton = $('<div>')
                                .addClass('add ui-button')
                                .append(
                                    $('<span>').html(_l('label.add.intermediate.certificate'))
                                );
                            var $servercertificate = $form.find('.form-item[rel=certificate]');
                            $addButton.insertBefore($servercertificate);
                            var count = 0;
                            var $intermediatecertificate = $form.find('.form-item[rel=intermediatecertificate]');

                            $addButton.click(function() {
                                // clone the template intermediate certificate and make it visible
                                var $newcertificate = $intermediatecertificate.clone().attr('id','intermediate'+count);
                                $newcertificate.insertBefore($addButton);
                                $newcertificate.css('display', 'inline-block');
                                $newcertificate.addClass('sslcertificate');
                                count++;
                                // change label
                                var $label = $newcertificate.find('label');
                                $label.html($label.html().replace('{0}', count)); // 'Intermediate certificate ' + count + ':'
                            });
                        },
                        fields: {
                            rootcertificate: {
                                label: 'label.root.certificate',
                                isTextarea: true,
                                validation: { required: true }
                            },
                            intermediatecertificate: { // this is the template 'intermediate certificate', always hidden
                                label: 'label.intermediate.certificate',
                                isTextarea: true,
                                isHidden: true
                            },
                            certificate: {
                                label: 'label.certificate',
                                isTextarea: true,
                                validation: { required: true }
                            },
                            privatekey: {
                                label: 'label.privatekey',
                                isTextarea: true,
                                validation: { required: true }
                            },
                            domainsuffix: {
                                label: 'label.domain.suffix',
                                validation: { required: true }
                            }
                        }
                    },
                    after: function(args) {
                        var $loading = $('<div>').addClass('loading-overlay');
                        $('.system-dashboard-view:visible').prepend($loading);

                        // build a list with all certificates that need to be uploaded
                        var certificates = [];
                        certificates.push(args.data.rootcertificate);
                        if ($.isArray(args.data.intermediatecertificate))
                        {
                            $.merge(certificates, args.data.intermediatecertificate);
                        }
                        else
                        {
                            certificates.push(args.data.intermediatecertificate);
                        }
                        certificates.push(args.data.certificate);

                        // Recursively uploads certificates.
                        // When the upload succeeds, proceeds to uploading the next certificate.
                        // When the upload fails, stops and reports failure.
                        var uploadCertificate = function(index) {
                            if (index >=  certificates.length)
                            {
                                return;
                            }
                            if ( !$.trim(certificates[index])) // skip empty certificate
                            {
                                uploadCertificate(index + 1);
                                return;
                            }

                            // build certificate data
                            var certificateData = {
                                id: index + 1, // id start from 1
                                certificate: certificates[index],
                                domainsuffix: args.data.domainsuffix
                            };
                            switch (index) {
                                case (0): //first certificate is the root certificate
                                    certificateData['name'] = 'root';
                                    break;
                                case (certificates.length - 1): // last certificate is the server certificate
                                    certificateData['privatekey'] = args.data.privatekey;
                                    break;
                                default: // intermediate certificates
                                    certificateData['name'] = 'intermediate' + index;
                            }

                            $.ajax({
                                type: "POST",
                                url: createURL('uploadCustomCertificate'),
                                data:  certificateData,
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
                                                        if (index ==  certificates.length - 1) // last one, report success
                                                        {
                                                            cloudStack.dialog.notice({
                                                                message: 'message.update.ssl.succeeded'
                                                            });
                                                            $loading.remove();
                                                        }
                                                        else // upload next certificate
                                                        {
                                                            uploadCertificate(index + 1);
                                                        }
                                                    } else if (result.jobstatus == 2) {
                                                        cloudStack.dialog.notice({
                                                            message: _l('message.update.ssl.failed') + ' ' + _s(result.jobresult.errortext)
                                                        });
                                                        $loading.remove();
                                                    }
                                                }
                                            },
                                            error: function(XMLHttpResponse) {
                                                cloudStack.dialog.notice({
                                                    message: _l('message.update.ssl.failed') + ' ' + parseXMLHttpResponse(XMLHttpResponse)
                                                });
                                                $loading.remove();
                                            }
                                        });
                                    }, g_queryAsyncJobResultInterval);
                                },
                                error: function(XMLHttpResponse) {
                                    cloudStack.dialog.notice({
                                        message: _l('message.update.ssl.failed') + ' ' + parseXMLHttpResponse(XMLHttpResponse)
                                    });
                                    $loading.remove();
                                }
                            });
                            return;
                        };

                        // start uploading the certificates
                        uploadCertificate(0);
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
