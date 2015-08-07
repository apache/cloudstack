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
        // Region switcher
        var $regionList = $('<ul>');

        // Get region listing
        var refreshRegions = function() {
            $regionList.find('li').remove();
            cloudStack.sections.regions.regionSelector.dataProvider({
                response: {
                    success: function(args) {
                        var data = args.data;

                        var currentRegion = null;
                        $(data).each(function() {
                            var region = this;
                            var regionName = region.name;
                            var $li = $('<li>').append($('<span>').html(_s(region.name)));

                            $li.data('region-data', region);

                            if (document.location.href.indexOf(region.endpoint) != -1) {
                                currentRegion = region;
                                $li.addClass('active');
                            }

                            $regionList.append($li);
                        });

                        if (currentRegion != null) {
                            $regionSwitcherButton.find('.title').html(_s(currentRegion.name)).attr('title', _s(currentRegion.name));
                        } else {
                            $regionSwitcherButton.find('.title').html('').attr('title', '');
                        }
                    }
                }
            });
        };

        $(window).bind('cloudStack.refreshRegions', refreshRegions);

        var $regionSelector = $('<div>').addClass('region-selector')
            .append($('<div>').addClass('top-arrow'))
            .append($('<h2>').html(_l('label.menu.regions')))
            .append($regionList)
            .append(
                $('<div>').addClass('buttons')
                .append(
                    $('<div>').addClass('button close').append($('<span>').html(_l('label.close')))
                )
        )
            .hide();
        var $regionSwitcherButton = $('<div>').addClass('region-switcher')
            .attr('title', _l('label.select.region'))
            .append(
                $('<span>').addClass('icon').html('&nbsp;'),
                $('<span>').addClass('title').html('')
        );

        var closeRegionSelector = function(args) {
            $regionSwitcherButton.removeClass('active');
            $regionSelector.fadeOut(args ? args.complete : null);
            $('body > .overlay').fadeOut(function() {
                $('body > .overlay').remove()
            });
        };

        var switchRegion = function(url) {
            closeRegionSelector({
                complete: function() {
                    $('#container').prepend($('<div>').addClass('loading-overlay'));
                    document.location.href = url;
                }
            });
        };

        $regionList.click(function(event) {
            var $target = $(event.target);
            var $li = $target.closest('li');
            var region, url;

            if ($li.size() && !$li.hasClass('active')) {
                region = $li.data('region-data');
                url = region.endpoint;
                id = region.id;

                if (id != '-1') {
                    switchRegion(url);
                }
            }
        });

        $regionSwitcherButton.click(function() {
            if ($regionSwitcherButton.hasClass('active')) {
                closeRegionSelector();
            } else {
                $regionSwitcherButton.addClass('active');
                $regionSelector.fadeIn('fast').overlay({
                    closeAction: closeRegionSelector
                });
            }
        });

        $regionSelector.find('.button.close').click(function() {
            closeRegionSelector();
        });

        $('#header .controls .view-switcher.button:last').after($regionSwitcherButton, $regionSelector);
        refreshRegions();
    });
}(jQuery, cloudStack));
