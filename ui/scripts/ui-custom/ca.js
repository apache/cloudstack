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
        var caCert = "";
        var downloadCaCert = function() {
            var blob = new Blob([caCert], {type: 'application/x-x509-ca-cert'});
            var filename = "cloud-ca.pem";
            if(window.navigator.msSaveOrOpenBlob) {
                window.navigator.msSaveBlob(blob, filename);
            } else{
                var elem = window.document.createElement('a');
                elem.href = window.URL.createObjectURL(blob);
                elem.download = filename;
                document.body.appendChild(elem)
                elem.click();
                document.body.removeChild(elem);
            }
        };

        $.ajax({
            url: createURL('listCaCertificate'),
            success: function(json) {
                caCert = json.listcacertificateresponse.cacertificates.certificate;
                if (caCert) {
                    var $caCertDownloadButton = $('<div>').addClass('cacert-download');
                    $caCertDownloadButton.append($('<span>').addClass('icon').html('&nbsp;').attr('title', 'Download CA Certificate'));
                    $caCertDownloadButton.click(function() {
                        downloadCaCert();
                    });
                    $('#header .controls .view-switcher:last').after($caCertDownloadButton);
                }
            },
            error: function(data) {
            }
        });
    });
}(jQuery, cloudStack));
