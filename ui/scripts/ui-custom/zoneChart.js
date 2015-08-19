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
    /**
     * Zone details chart
     */
    cloudStack.uiCustom.systemChart = function(chartID, chartFunc) {
        /**
         * Make view all button
         */
        var viewAllButton = function(args) {
            var $viewAll = $('<div>').addClass('button view-all');
            var $label = $('<span>').addClass('view-all-label').html(args.label ? args.label : _l('label.view.all'));
            var $browser = args.$browser;
            var action = args.action;
            // Launch a list view
            //var $multiple-click=$viewAll.data('multiple-click',false);
            $viewAll.click(function() {
                if ($viewAll.data('multiple-click')) return false;
                //@pranav-handling the multiple clicks by using a flag variable
                $viewAll.data('multiple-click', true);
                $browser.cloudBrowser('addPanel', {
                    title: args.title,
                    maximizeIfSelected: true,
                    complete: function($newPanel) {
                        $viewAll.data('multiple-click', false);
                        action({
                            $panel: $newPanel
                        });
                    }
                });
            });

            $viewAll.append($label);

            return $viewAll;
        };

        /**
         * Chart button action generators
         */
        var actions = {
            /**
             * Makes a list view from given zone sub-section
             */
            listView: function(targetID, context) {
                return function(args) {
                    var $elem = args.$panel;
                    var listView = cloudStack.sections.system.subsections[targetID];

                    $elem.listView($.extend(true, {}, listView, {
                        context: context
                    }));
                };
            },

            providerListView: function(context) {
                return function(args) {
                    var $elem = args.$panel;
                    var listViewArgs = cloudStack.sections.system.naas.providerListView;

                    $elem.listView({
                        context: context,
                        listView: listViewArgs
                    });
                };
            },

            /**
             * Makes details for a given traffic type
             */
            trafficTypeDetails: function(targetID, context) {
                return function(args) {
                    var $elem = args.$panel;
                    var detailViewArgs = cloudStack.sections.system.naas.mainNetworks[targetID].detailView;

                    $elem.detailView($.extend(true, {}, detailViewArgs, {
                        $browser: $('#browser .container'),
                        context: context
                    }));
                };
            }
        };

        /**
         * Chart generators
         */
        var charts = {
            /**
             * Compute tab
             */
            compute: function(args) {
                var $chart = $('<div>');
                var $browser = $('#browser .container');
                var context = args.context;

                // Resource items
                var computeResources = {
                    zone: {
                        label: 'label.zone'
                    },

                    pods: {
                        label: 'label.pods',
                        viewAll: {
                            action: actions.listView('pods', context)
                        }
                    },

                    clusters: {
                        label: 'label.clusters',
                        viewAll: {
                            action: actions.listView('clusters', context)
                        }
                    },

                    hosts: {
                        label: 'label.hosts',
                        viewAll: {
                            action: actions.listView('hosts', context)
                        }
                    },

                    primaryStorage: {
                        label: 'label.primary.storage',
                        viewAll: {
                            action: actions.listView('primary-storage', context)
                        }
                    },

                    ucs: {
                        label: 'label.ucs',
                        viewAll: {
                            action: actions.listView('ucs', context)
                        }
                    },

                    secondaryStorage: {
                        label: 'label.secondary.storage',
                        viewAll: {
                            action: actions.listView('secondary-storage', context)
                        }
                    }
                };


                var $computeResources = $('<ul>').addClass('resources');

                // Make resource items
                $.each(computeResources, function(id, resource) {
                    var $li = $('<li>');
                    var $label = $('<span>').addClass('label');

                    $li.addClass(id);
                    $label.html(_l(resource.label));
                    $label.appendTo($li);

                    // View all
                    if (resource.viewAll) {
                        viewAllButton($.extend(resource.viewAll, {
                            title: _l(resource.label),
                            $browser: $browser,
                            context: context
                        })).appendTo($li);
                    }

                    $li.appendTo($computeResources);
                });

                $chart.append($computeResources);

                return $chart;
            },

            network: function(args) {
                var $chart = $('<div>');
                var $browser = $('#browser .container');
                var $loading = $('<div>').addClass('loading-overlay');
                var context = args.context;
                var networkDataProvider = cloudStack.sections.system.naas.networks.dataProvider;
                var trafficTypeDataProvider = cloudStack.sections.system.naas.trafficTypes.dataProvider;

                $loading.appendTo($chart);

                var renderChart = function(args) {
                    var $targetChart = args.$chart ? args.$chart : $chart;
                    var targetContext = $.extend(true, {}, context, {
                        physicalNetworks: [args.data]
                    });

                    // Get traffic type data
                    trafficTypeDataProvider({
                        context: targetContext,
                        response: {
                            success: function(args) {
                                var $networkChart = $('<div>').addClass('system-network-chart');
                                var $trafficTypes = $('<ul>').addClass('resources traffic-types');

                                $loading.remove();

                                var trafficTypes = {
                                    'public': {
                                        label: _l('label.public'),
                                        configure: {
                                            action: actions.trafficTypeDetails('public', targetContext)
                                        }
                                    },

                                    'guest': {
                                        label: _l('label.guest'),
                                        configure: {
                                            action: actions.trafficTypeDetails('guest', targetContext)
                                        }
                                    },

                                    'management': {
                                        label: _l('label.management'),
                                        configure: {
                                            action: actions.trafficTypeDetails('management', targetContext)
                                        }
                                    },

                                    'storage': {
                                        label: _l('label.storage'),
                                        configure: {
                                            action: actions.trafficTypeDetails('storage', targetContext)
                                        }
                                    },

                                    'providers': {
                                        label: _l('label.network.service.providers'),
                                        ignoreChart: true,
                                        dependsOn: 'guest',
                                        configure: {
                                            action: actions.providerListView(targetContext)
                                        }
                                    }
                                };

                                var validTrafficTypes = $.map(args.data, function(trafficType) {
                                    return trafficType.name.toLowerCase();
                                });

                                // Make traffic type elems
                                $.each(trafficTypes, function(id, trafficType) {
                                    if ($.inArray(id, validTrafficTypes) == -1) { //if it is not a valid traffic type
                                        if (trafficType.dependsOn != null && trafficType.dependsOn.length > 0) { //if it has dependsOn
                                            if ($.inArray(trafficType.dependsOn, validTrafficTypes) == -1) { //if its dependsOn is not a valid traffic type, either
                                                return true; //skip this item
                                            }
                                            //else, if its dependsOn is a valid traffic type, continue to Make list item    (e.g. providers.dependsOn is 'guest')
                                        } else {
                                            return true; //if it doesn't have dependsOn, skip this item
                                        }
                                    }

                                    // Make list item
                                    var $li = $('<li>').addClass(id);
                                    var $label = $('<span>').addClass('label').html(trafficType.label);
                                    var $configureButton = viewAllButton($.extend(trafficType.configure, {
                                        label: _l('label.configure'),
                                        title: trafficType.label,
                                        $browser: $browser,
                                        targetContext: targetContext
                                    }));

                                    $li.append($label, $configureButton);
                                    $li.appendTo($trafficTypes);

                                    // Make chart
                                    if (trafficType.ignoreChart)
                                        return true;

                                    var $targetChartItem = $('<div>').addClass('network-chart-item').addClass(id);
                                    $targetChartItem.appendTo($networkChart);
                                });

                                var $switchIcon = $('<div>').addClass('network-switch-icon').append(
                                    $('<span>').html('L2/L3 switch')
                                );
                                var $circleIcon = $('<div>').addClass('base-circle-icon');

                                $targetChart.append($trafficTypes, $switchIcon, $networkChart, $circleIcon);
                            }
                        }
                    });
                };

                // Get network data
                networkDataProvider({
                    context: context,
                    response: {
                        success: function(args) {
                            var data = args.data;
                            var actionFilter = args.actionFilter;

                            $chart.listView({
                                listView: $.extend(true, {}, cloudStack.sections.system.naas.networks.listView, {
                                    dataProvider: function(args) {
                                        args.response.success({
                                            actionFilter: actionFilter,
                                            data: data
                                        });
                                    },
                                    detailView: {
                                        noCompact: true,
                                        tabs: {
                                            network: {
                                                title: 'Network',
                                                custom: function(args) {
                                                    var $chart = $('<div>').addClass('system-chart network');

                                                    renderChart({
                                                        $chart: $chart,
                                                        data: args.context.physicalNetworks[0]
                                                    });

                                                    return $chart;
                                                }
                                            }
                                        }
                                    }
                                })
                            });
                            $loading.remove();
                        }
                    }
                });

                return $chart;
            },

            resources: function(args) {
                var $chart = $('<div>').addClass('dashboard admin');
                var $chartItems = $('<ul>');
                var $stats = $('<div>').addClass('stats');
                var $container = $('<div>').addClass('dashboard-container head');
                var $top = $('<div>').addClass('top');
                var $title = $('<div>').addClass('title').append($('<span>').html(_l('label.system.wide.capacity')));

                var chartItems = {
                    // The keys are based on the internal type ID associated with each capacity
                    0: {
                        name: _l('label.memory.allocated')
                    },
                    1: {
                        name: _l('label.cpu.allocated')
                    },
                    2: {
                        name: _l('label.primary.used')
                    },
                    3: {
                        name: _l('label.primary.allocated')
                    },
                    6: {
                        name: _l('label.secondary.storage')
                    },
                    9: {
                        name: _l('label.local.storage')
                    },
                    4: {
                        name: _l('label.public.ips')
                    },
                    5: {
                        name: _l('label.management.ips')
                    },
                    8: {
                        name: _l('label.direct.ips')
                    },
                    7: {
                        name: _l('label.vlan')
                    },
                    19: {
                        name: _l('GPU')
                    }
                };

                $top.append($title);
                $container.append($top, $stats.append($chartItems));
                $chart.append($container);
                var $loading = $('<div>').addClass('loading-overlay').prependTo($chart);

                cloudStack.sections.system.zoneDashboard({
                    context: args.context,
                    response: {
                        success: function(args) {
                            $loading.remove();
                            $.each(chartItems, function(id, chartItem) {
                                var data = args.data[id] ? args.data[id] : {
                                    used: 0,
                                    total: 0,
                                    percent: 0
                                };
                                var $item = $('<li>');
                                var $name = $('<div>').addClass('name').html(chartItem.name);
                                var $value = $('<div>').addClass('value');
                                var $content = $('<div>').addClass('content').html('Allocated: ');
                                var $allocatedValue = $('<span>').addClass('allocated').html(data.used);
                                var $totalValue = $('<span>').addClass('total').html(data.total);
                                var $chart = $('<div>').addClass('chart');
                                var $chartLine = $('<div>').addClass('chart-line')
                                    .css({
                                        width: '0%'
                                    })
                                    .animate({
                                        width: data.percent + '%'
                                    });
                                var $percent = $('<div>').addClass('percentage');
                                var $percentValue = $('<soan>').addClass('value').html(data.percent);

                                $chartItems.append(
                                    $item.append(
                                        $name,
                                        $value.append(
                                            $content.append(
                                                $allocatedValue,
                                                ' / ',
                                                $totalValue
                                            )
                                        ),
                                        $chart.append($chartLine),
                                        $percent.append($percentValue, '%')
                                    )
                                );
                            });
                        }
                    }
                });

                return $chart;
            }
        };

        return function(args) {
            // Fix zone context naming
            args.context.zones = args.context.physicalResources;

            if (chartFunc == null)
                chartFunc = charts[chartID];

            var $chart = chartFunc(args).addClass('system-chart').addClass(chartID);

            return $chart;
        };
    };
})(jQuery, cloudStack);
