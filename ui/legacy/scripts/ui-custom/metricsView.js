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
        return function(ctxArgs) {

            var metricsListView = cloudStack.sections.metrics.listView;
            var metricsLabel = _l('label.metrics');
            var context = {};
            if (ctxArgs && ctxArgs.hasOwnProperty('context')) {
                context = ctxArgs.context;
            }

            if (args.resource == 'zones') {
                metricsLabel = _l('label.zones') + ' ' + metricsLabel;
                metricsListView = cloudStack.sections.metrics.zones.listView;

            } else if (args.resource == 'clusters') {
                metricsLabel = _l('label.clusters') + ' ' + metricsLabel;
                metricsListView = cloudStack.sections.metrics.clusters.listView;

            } else if (args.resource == 'hosts') {
                metricsLabel = _l('label.hosts') + ' ' + metricsLabel;
                metricsListView = cloudStack.sections.metrics.hosts.listView;

                if (context && !context.filterBy) {
                    if (context.hasOwnProperty('clusters') && context.clusters[0]) {
                        context.filterBy = 'clusterid';
                        context.id = context.clusters[0].id;
                    }
                    if (context.hasOwnProperty('instances') && context.instances[0]) {
                        context.filterBy = 'virtualmachineid';
                        context.id = context.instances[0].id;
                    }
                }
            } else if (args.resource == 'storagepool') {
                metricsLabel = _l('label.primary.storage') + ' ' + metricsLabel;
                metricsListView = cloudStack.sections.metrics.storagepool.listView;

            } else if (args.resource == 'vms') {
                metricsLabel = _l('label.instances') + ' ' + metricsLabel;
                metricsListView = cloudStack.sections.metrics.instances.listView;
                metricsListView.advSearchFields = cloudStack.sections.instances.listView.advSearchFields;

                if (context && !context.filterBy) {
                    if (context.hasOwnProperty('hosts') && context.hosts[0]) {
                        context.filterBy = 'hostid';
                        context.id = context.hosts[0].id;
                    }
                }
            } else if (args.resource == 'volumes') {
                metricsLabel = _l('label.volumes') + ' ' + metricsLabel;
                metricsListView = cloudStack.sections.metrics.volumes.listView;
                metricsListView.advSearchFields = cloudStack.sections.storage.sections.volumes.listView.advSearchFields;
                metricsListView.groupableColumns = false;

                if (context && !context.filterBy) {
                    if (context.hasOwnProperty('instances') && context.instances[0]) {
                        context.filterBy = 'virtualmachineid';
                        context.id = context.instances[0].id;
                    }
                    if (context.hasOwnProperty('primarystorages') && context.primarystorages[0]) {
                        context.filterBy = 'storageid';
                        context.id = context.primarystorages[0].id;
                    }
                }
            }

            if (context.metricsFilterData) {
                delete context.metricsFilterData;
            }

            if (context.filterBy) {
                context.metricsFilterData = {
                    key: context.filterBy,
                    value: context.id
                };
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

            metricsListView.hideSearchBar = false;
            metricsListView.needsRefresh = true;
            metricsListView.noSplit = true;
            metricsListView.horizontalOverflow = true;
            metricsListView.groupableColumns = true;

            if (args.resource != 'vms' && args.resource != 'volumes' && args.resource != 'zones') {
                metricsListView.advSearchFields = {
                    name: {
                        label: 'label.name'
                    },
                    zoneid: {
                        label: 'label.zone',
                        select: function(args) {
                            $.ajax({
                                url: createURL('listZones'),
                                data: {
                                    listAll: true
                                },
                                success: function(json) {
                                    var zones = json.listzonesresponse.zone ? json.listzonesresponse.zone : [];

                                    args.response.success({
                                        data: $.map(zones, function(zone) {
                                            return {
                                                id: zone.id,
                                                description: zone.name
                                            };
                                        })
                                    });
                                }
                            });
                        }
                    }
                };
            }

            var $browser = $('#browser .container');
            return $browser.cloudBrowser('addPanel', {
                  title: metricsLabel,
                  maximizeIfSelected: true,
                  complete: function($newPanel) {
                      $newPanel.listView({
                          $browser: $browser,
                          context: context,
                          listView: metricsListView
                      });
                      // Make metrics tables horizontally scrollable
                      $newPanel.find('.list-view').css({'overflow-x': 'visible'});
                      // Refresh metrics when refresh button is clicked
                      $newPanel.find('.refreshMetrics').click(function() {
                          var sortedTh = $newPanel.find('table thead tr:last th.sorted');
                          var thIndex = sortedTh.index();
                          var thClassName = null;
                          var wasSorted = false;
                          var sortClassName = 'asc';
                          if (sortedTh && sortedTh.hasClass('sorted')) {
                              wasSorted = true;
                              var classes = sortedTh.attr('class').split(/\s+/);
                              thClassName = classes[0];
                              if (classes.indexOf('desc') > -1) {
                                  sortClassName = 'desc';
                              }
                          }
                          $browser.cloudBrowser('removeLastPanel', {});
                          var refreshedPanel = cloudStack.uiCustom.metricsView(args)(ctxArgs);
                          if (wasSorted && thClassName) {
                              refreshedPanel.find('th.' + thClassName).filter(function() {
                                  return $(this).index() == thIndex;
                              }).addClass('sorted').addClass(sortClassName);
                          }
                      });

                      var browseBy = metricsListView.browseBy;
                      if (browseBy) {
                          $newPanel.bind('click', function(event) {
                              event.stopPropagation();
                              var $target = $(event.target);
                              var id = $target.closest('tr').data('list-view-item-id');
                              var jsonObj = $target.closest('tr').data('jsonObj');
                              if (browseBy.filterKey && jsonObj) {
                                  if (jsonObj.hasOwnProperty(browseBy.filterKey)) {
                                      id = jsonObj[browseBy.filterKey];
                                  } else {
                                      return; // return if provided key is missing
                                  }
                              }
                              if (id && ($target.hasClass('first') || $target.parent().hasClass('first')) && ($target.is('td') || $target.parent().is('td'))) {
                                  context.id = id;
                                  context.filterBy = browseBy.filterBy;
                                  ctxArgs.context = context;
                                  cloudStack.uiCustom.metricsView({resource: browseBy.resource})(ctxArgs);
                              }
                          });
                      }
                  }
            });
        };
    };
})(jQuery, cloudStack);
