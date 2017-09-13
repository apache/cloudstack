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
                console.log(json);
                pluginEnabled = (json.cloudianisenabledresponse.success == 'true');
            }
        });
        return pluginEnabled;
      },
      show: function() {
        var ssoUrl = '';
        var description = 'Cloudian Storage should open in another window.';
        $.ajax({
            url: createURL('cloudianSsoLogin'),
            async: false,
            success: function(json) {
                console.log(json);
                ssoUrl = json.cloudianssologinresponse.cloudianssologin.url;
                //FIXME: post? maybe submit using a form?
                var cmcWindow = window.open(ssoUrl, "CMCWindow");
                cmcWindow.focus();
            },
            error: function(data) {
                description = 'Single-Sign-On failed for Cloudian Storage.';
            }
        });
        return $('<div>').html(description);
      }
    });
  };
}(cloudStack));
