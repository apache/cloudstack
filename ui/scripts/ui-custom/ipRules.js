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
    cloudStack.ipRules = function(args) {
        return function(detailArgs) {
            var context = detailArgs.context;

            var portMultiEdit = function(args) {
                return $('<div>').multiEdit(args);
            };

            var makeMultiEditPanel = function($item) {
                if ($item.closest('li').hasClass('disabled'))
                    return false;

                var targetId = $item.attr('net-target');
                var targetName = $item.parent().find('.name').find('span').html();
                var target = args[targetId];

                var $browser = $item.closest('.detail-view').data('view-args').$browser;

                $browser.cloudBrowser('addPanel', {
                    title: targetName,
                    maximizeIfSelected: true,
                    complete: function($newPanel) {
                        $newPanel.detailView({
                            $browser: $browser,
                            name: targetId,
                            context: context,
                            tabs: {
                                network: {
                                    title: targetName,
                                    custom: function(args) {
                                        return portMultiEdit($.extend(target, {
                                            context: context
                                        }));
                                    }
                                }
                            }
                        });
                    }
                });

                return true;
            };

            var staticNATChart = function(args, includingFirewall) {
                var $chart = $('#template').find('.network-chart.static-nat').clone();
                var $vmName = $chart.find('li.static-nat-enabled .vmname');
                var $browser = $('#browser .container');
                var vmDataProvider = args.vmDataProvider;
                var vmDetails = args.vmDetails;

                args.staticNATDataProvider({
                    context: context,
                    response: {
                        success: function(args) {
                            var vmID = args.data.virtualmachineid;
                            var vmIP = args.data.vmipaddress;
                            var vmName = args.data.virtualmachinename;

                            $vmName.append(
                                $('<span>').html('VM: ' + _s(vmName)),
                                $('<span>').html('<br/>VM IP: ' + vmIP)
                            );

                            $vmName.click(function() {
                                $browser.cloudBrowser('addPanel', {
                                    title: _l('label.static.nat.vm.details'),
                                    complete: function($newPanel) {
                                        vmDataProvider({
                                            context: context,
                                            response: {
                                                success: function(args) {
                                                    var instance = args.data;
                                                    var detailViewArgs = $.extend(true, {}, vmDetails, {
                                                        $browser: $browser,
                                                        context: $.extend(true, {}, context, {
                                                            instances: [instance]
                                                        }),
                                                        jsonObj: instance,
                                                        id: instance.id
                                                    });

                                                    // No actions available
                                                    detailViewArgs.actions = {};

                                                    $newPanel.detailView(detailViewArgs);
                                                }
                                            }
                                        });
                                    }
                                });
                            });
                        }
                    }
                });

                if (includingFirewall == true) {
                    $chart.find('li.firewall .view-details').click(function() {
                        //makeMultiEditPanel($(this), { title: _l('label.nat.port.range')});
                        makeMultiEditPanel($(this));
                    });
                } else {
                    $chart.find('li.firewall').hide();
                }

                return $chart;
            };

            var netChart = function(args) {

                var $chart = $('#template').find('.network-chart.normal').clone();
                var preFilter = args.preFilter ? args.preFilter({
                    items: ['firewall', 'portForwarding', 'loadBalancing'],
                    context: context
                }) : [];

                // 1. choose between staticNAT chart and non-staticNAT chart  2. filter disabled tabs
                if (preFilter.length) {
                    if ($.inArray('nonStaticNATChart', preFilter) != -1) { //choose static NAT chart
                        if ($.inArray('firewall', preFilter) == -1) {
                            return staticNATChart(args, true); //static NAT including Firewall
                        } else {
                            return staticNATChart(args, false); //static NAT excluding Firewall
                        }
                    } else { //choose non-static NAT chart
                        $(preFilter).each(function() {
                            var id = this;

                            var $li = $chart.find('li').filter(function() {
                                return $(this).hasClass(id);
                            }).addClass('disabled');
                        });
                    }
                }

                $chart.find('.view-details').click(function() {
                    makeMultiEditPanel($(this));
                    return false;
                });

                return $chart;
            };

            return netChart(args);
        };
    };
})(jQuery, cloudStack);
