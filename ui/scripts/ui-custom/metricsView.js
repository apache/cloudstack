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

    cloudStack.uiCustom.metricsView = function(args) {
        return function() {
            var metricsListView = cloudStack.sections.metrics.listView;

            if (args.resource == 'zones') {
            } else if (args.resource == 'clusters') {
                metricsListView = cloudStack.sections.metrics.cluster.listView;
            } else if (args.resource == 'hosts') {
            } else if (args.resource == 'primarystorage') {
            } else if (args.resource == 'vms') {
            } else if (args.resource == 'volumes') {
            }

            var $browser = $('#browser .container');
            return $browser.cloudBrowser('addPanel', {
                  title: 'Metrics',
                  maximizeIfSelected: true,
                  complete: function($newPanel) {
                      $newPanel.listView({
                          $browser: $browser,
                          context: cloudStack.context,
                          listView: metricsListView
                      });
                  }
            });
        };
    };
})(jQuery, cloudStack);
