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
(function($, cloudStack, require) {
  var loadCSS = function(path) {
    var $link = $('<link>');

    $link.attr({
      rel: 'stylesheet',
      type: 'text/css',
      href: path
    });

    $('head').append($link);
  };

  var pluginAPI = {
    pollAsyncJob: pollAsyncJobResult,
    apiCall: function(command, args) {
      $.ajax({
        url: createURL(command),
        data: args.data,
        success: args.success,
        error: function(json) {
          args.error(parseXMLHttpResponse(json));
        }
      })
    },
    addSection: function(section) {
      cloudStack.sections[section.id] = $.extend(section, {
        customIcon: 'plugins/' + section.id + '/icon.png'
      });
    },
    extend: function(obj) {
      $.extend(true, cloudStack, obj);
    }
  };
  
  cloudStack.sections.plugins = {
    title: 'label.plugins',
    show: cloudStack.uiCustom.plugins
  };

  // Load plugins
  $(cloudStack.plugins).map(function(index, pluginID) {
    var basePath = 'plugins/' + pluginID + '/';
    var pluginJS = basePath + pluginID + '.js';
    var configJS = basePath + 'config.js';
    var pluginCSS = basePath + pluginID + '.css';

    require([pluginJS], function() {
      require([configJS]);
      loadCSS(pluginCSS);

      // Execute plugin
      cloudStack.plugins[pluginID]({
        ui: pluginAPI
      });
    });

    // Load CSS
  });
}(jQuery, cloudStack, require));
