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
    cloudStack.uiCustom.dashboard = function() {
        /**
         * Retrieve chart data
         */
        var getData = function() {
            // Populate data
            $dashboard.find('[data-item]').hide();
            cloudStack.sections.dashboard[dashboardType].dataProvider({
                response: {
                    success: function(args) {
                        var $browser = $dashboard.closest('#browser .container');
                        var data = args.data;

                        // Iterate over data; populate corresponding DOM elements
                        $.each(data, function(key, value) {
                            var $elem = $dashboard.find('[data-item=' + key + ']');

                            // This assumes an array of data
                            if ($elem.is('ul')) {
                                $elem.show();
                                var $liTmpl = $elem.find('li').remove();
                                $(value).each(function() {
                                    var item = this;
                                    var $li = $liTmpl.clone().appendTo($elem).hide();

                                    if ($li.is('.zone-stats li')) {
                                        $li.click(function() {
                                            $browser.cloudBrowser('addPanel', {
                                                title: _l('label.zone.details'),
                                                parent: $dashboard.closest('.panel'),
                                                maximizeIfSelected: true,
                                                complete: function($newPanel) {
                                                    $newPanel.detailView($.extend(true, {},
                                                        cloudStack.sections.dashboard.admin.zoneDetailView, {
                                                            $browser: $browser,
                                                            context: $.extend(true, {}, cloudStack.context, {
                                                                physicalResources: [{
                                                                    id: item.zoneID
                                                                }]
                                                            })
                                                        }
                                                    ));
                                                }
                                            });
                                        });
                                    }

                                    $.each(item, function(arrayKey, arrayValue) {
                                        if (!arrayValue) arrayValue = '';

                                        var $arrayElem = $li.find('[data-list-item=' + arrayKey + ']');

                                        $arrayElem.each(function() {
                                            var $arrayElem = $(this);

                                            if ($arrayElem.hasClass('pie-chart')) {
                                                // Generate pie chart
                                                // -- values above 80 have a red color
                                                setTimeout(function() {
                                                    pieChart($arrayElem, [{
                                                        data: [
                                                            [1, 100 - arrayValue]
                                                        ],
                                                        color: '#54697e'
                                                    }, {
                                                        data: [
                                                            [1, arrayValue]
                                                        ],
                                                        color: arrayValue < 80 ? 'orange' : 'red'
                                                    }]);
                                                });
                                            } else {
                                                if ($li.attr('concat-value')) {
                                                    var val = $(_l(arrayValue).toString().split(', ')).map(function() {
                                                        var val = _s(this.toString());
                                                        var concatValue = parseInt($li.attr('concat-value'));

                                                        return val.length >= concatValue ?
                                                            val.substring(0, concatValue).concat('...') : val;
                                                    }).toArray().join('<br />');

                                                    $arrayElem.html(val);
                                                } else {
                                                    $arrayElem.html(_s(_l(arrayValue)));
                                                }

                                                $arrayElem.attr('title', _l(arrayValue).toString().replace('<br/>', ', '));
                                            }
                                        });
                                    });

                                    $li.attr({
                                        title: _s(_l(item.description))
                                    });

                                    $li.fadeIn();
                                });
                            } else {
                                $elem.each(function() {
                                    var $item = $(this);
                                    if ($item.hasClass('chart-line')) {
                                        $item.show().animate({
                                            width: value + '%'
                                        });
                                    } else {
                                        $item.hide().html(_s(value)).fadeIn();
                                    }
                                });
                            }
                        });
                    }
                }
            });
        };

        /**
         * Render circular pie chart, without labels
         */
        var pieChart = function($container, data) {
            $container.css({
                width: 70,
                height: 66
            });
            $.plot($container, data, {
                width: 100,
                height: 100,
                series: {
                    pie: {
                        innerRadius: 0.7,
                        show: true,
                        label: {
                            show: false
                        }
                    }
                },
                legend: {
                    show: false
                }
            });
        };

        // Determine if user or admin dashboard should be shown
        var dashboardType = cloudStack.sections.dashboard.adminCheck({
            context: cloudStack.context
        }) ? 'admin' : 'user';

        // Get dashboard layout
        var $dashboard = $('#template').find('div.dashboard.' + dashboardType).clone();

        // Update text
        $dashboard.find('.button.view-all').html(_l('label.view.all'));
        $dashboard.find('.dashboard-container.sub.alerts.first .top .title span').html(_l('label.general.alerts'));
        $dashboard.find('.dashboard-container.sub.alerts.last .top .title span').html(_l('label.host.alerts'));
        $dashboard.find('.dashboard-container.head .top .title span').html(_l('label.system.capacity'));

        // View all action
        $dashboard.find('.view-all').click(function() {
            var $browser = $('#browser .container');

            if ($(this).hasClass('network')) $('#navigation li.network').click();
            else {
                $browser.cloudBrowser('addPanel', {
                    title: $dashboard.hasClass('admin') ? 'Alerts' : 'Events',
                    maximizeIfSelected: true,
                    complete: function($newPanel) {
                        $newPanel.listView({
                            $browser: $browser,
                            context: cloudStack.context,
                            listView: $dashboard.hasClass('admin') ? cloudStack.sections.events.sections.alerts.listView : cloudStack.sections.events.sections.events.listView // Users cannot see events
                        });
                    }
                });
            };
        });

        //Fetch Latest action
        $dashboard.find('.fetch-latest').click(function() {
            window.fetchLatestflag = 1;
            var $browser = $('#browser .container');

            if ($(this).hasClass('fetch-latest')) $('#navigation li.dashboard').click();
        });

        getData();

        return $dashboard;
    };
}(jQuery, cloudStack));
