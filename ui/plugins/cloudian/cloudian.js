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

(function (cloudStack) {
  cloudStack.plugins.cloudian = function(plugin) {

    plugin.ui.addSection({
      id: 'cloudian',
      title: 'Cloudian Storage',
      showOnNavigation: true,
      preFilter: function(args) {
        var pluginEnabled = false;
        $.ajax({
            url: createURL('cloudianIsEnabled'),
            async: false,
            success: function(json) {
                var response = json.cloudianisenabledresponse.cloudianisenabled;
                pluginEnabled = response.enabled;
                if (pluginEnabled) {
                    var cloudianLogoutUrl = response.url + "logout.htm?";
                    onLogoutCallback = function() {
                        g_loginResponse = null;
                        var csUrl = window.location.href;
                        var redirect = "redirect=" + encodeURIComponent(csUrl);
                        window.location.replace(cloudianLogoutUrl + redirect);
                        return false;
                    };
                }
            }
        });
        return pluginEnabled;
      },

      show: function() {
        var description = 'Cloudian Management Console should open in another window.';
        $.ajax({
            url: createURL('cloudianSsoLogin'),
            async: false,
            success: function(json) {
                var response = json.cloudianssologinresponse.cloudianssologin;
                var cmcWindow = window.open(response.url, "CMCWindow");
                cmcWindow.focus();
            },
            error: function(data) {
                description = 'Single-Sign-On failed for Cloudian Management Console. Please ask your administrator to fix integration issues.';
            }
        });
        return $('<div style="margin: 20px;">').html(description);
      }
    });
  };
}(cloudStack));
