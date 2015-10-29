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
            var metricsLabel = _l('label.metrics');

            if (args.resource == 'zones') {
                metricsListView = cloudStack.sections.metrics.zones.listView;
                metricsLabel = _l('label.zones') + ' ' + metricsLabel;
            } else if (args.resource == 'clusters') {
                metricsListView = cloudStack.sections.metrics.clusters.listView;
                metricsLabel = _l('label.clusters') + ' ' + metricsLabel;
            } else if (args.resource == 'hosts') {
                metricsListView = cloudStack.sections.metrics.hosts.listView;
                metricsLabel = _l('label.hosts') + ' ' + metricsLabel;
            } else if (args.resource == 'storagepool') {
                metricsListView = cloudStack.sections.metrics.storagepool.listView;
                metricsLabel = _l('label.primary.storage') + ' ' + metricsLabel;
            } else if (args.resource == 'vms') {
                metricsListView = cloudStack.sections.metrics.instances.listView;
                metricsLabel = _l('label.instances') + ' ' + metricsLabel;
            } else if (args.resource == 'volumes') {
                metricsListView = cloudStack.sections.metrics.volumes.listView;
                metricsLabel = _l('label.volumes') + ' ' + metricsLabel;
            }

            // list view refresh button
            metricsListView.actions = {
                refreshMetrics: {
                    label: 'label.refresh',
                    isHeader: true,
                    addRow: true,
                    action: {
                        custom: function (args) {
                            return function() {
                            };
                        }
                    }
                }
            };

            metricsListView.hideSearchBar = true;
            metricsListView.needsRefresh = true;
            metricsListView.noSplit = true;
            metricsListView.horizontalOverflow = true;
            metricsListView.groupableColumns = true;

            if (args.resource == 'volumes') {
                metricsListView.groupableColumns = false;
            }

            var metricsContext = cloudStack.context;
            if (metricsContext.metricsFilterData) {
                delete metricsContext.metricsFilterData;
            }
            if (args.filterBy) {
                metricsContext.metricsFilterData = {
                    key: args.filterBy,
                    value: args.id
                };
            }

            var $browser = $('#browser .container');
            return $browser.cloudBrowser('addPanel', {
                  title: metricsLabel,
                  maximizeIfSelected: true,
                  complete: function($newPanel) {
                      $newPanel.listView({
                          $browser: $browser,
                          context: metricsContext,
                          listView: metricsListView
                      });
                      // Make metrics tables horizontally scrollable
                      $newPanel.find('.list-view').css({'overflow-x': 'visible'});
                      // Refresh metrics when refresh button is clicked
                      $newPanel.find('.refreshMetrics').click(function() {
                          $browser.cloudBrowser('removeLastPanel', {});
                          cloudStack.uiCustom.metricsView(args)();
                      });

                      var filterMetricView = metricsListView.browseBy;
                      if (filterMetricView) {
                          $newPanel.bind('click', function(event) {
                              event.stopPropagation();
                              var $target = $(event.target);
                              var id = $target.closest('tr').data('list-view-item-id');
                              var jsonObj = $target.closest('tr').data('jsonObj');
                              if (filterMetricView.filterKey && jsonObj) {
                                  if (jsonObj.hasOwnProperty(filterMetricView.filterKey)) {
                                      id = jsonObj[filterMetricView.filterKey];
                                  } else {
                                      return; // return if provided key is missing
                                  }
                              }
                              if (id && ($target.hasClass('first') || $target.parent().hasClass('first')) && ($target.is('td') || $target.parent().is('td'))) {
                                  filterMetricView.id = id;
                                  cloudStack.uiCustom.metricsView(filterMetricView)();
                              }
                          });
                      }
                  }
            });
        };
    };
})(jQuery, cloudStack);
