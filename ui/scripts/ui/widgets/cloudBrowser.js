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
    cloudStack.ui.widgets.browser = {};

    /**
     * Breadcrumb-related functions
     */
    var _breadcrumb = cloudStack.ui.widgets.browser.breadcrumb = {
        /**
         * Generate new breadcrumb
         */
        create: function($panel, title) {
            // Attach panel as ref for breadcrumb
            return cloudStack.ui.event.elem(
                'cloudBrowser', 'breadcrumb',
                $('<div>')
                .append(
                    $('<li>')
                    .attr({
                        title: title
                    })
                    .append(
                        $('<span>').html(title)
                    )
                )
                .append($('<div>').addClass('end'))
                .children(), {
                    panel: $panel
                }
            );
        },

        /**
         * Get breadcrumbs matching specified panels
         */
        filter: function($panels) {
            var $breadcrumbs = $('#breadcrumbs ul li');
            var $result = $([]);

            $panels.each(function() {
                var $panel = $(this);

                $.merge(
                    $result,
                    $.merge(
                        $breadcrumbs.filter(function() {
                            return $(this).index('#breadcrumbs ul li') == $panel.index();
                        }),

                        // Also include ends
                        $breadcrumbs.siblings('div.end').filter(function() {
                            return $(this).index('div.end') == $panel.index() + 1;
                        })
                    )
                );
            });

            return $result;
        }
    };

    /**
     * Container-related functions
     */
    var _container = cloudStack.ui.widgets.browser.container = {
        /**
         * Get all panels from container
         */
        panels: function($container) {
            return $container.find('div.panel');
        }
    };

    /**
     * Panel-related functions
     */
    var _panel = cloudStack.ui.widgets.browser.panel = {
        /**
         * Compute width of panel, relative to container
         */
        width: function($container, options) {
            options = options ? options : {};
            var width = $container.find('div.panel').size() < 1 || !options.partial ?
                $container.width() : $container.width() - $container.width() / 4;

            return width;
        },

        /**
         * Get left position
         */
        position: function($container, options) {
            return $container.find('div.panel').size() <= 1 || !options.partial ?
                0 : _panel.width($container, options) - _panel.width($container, options) / 1.5;
        },

        /**
         * Get the top panel z-index, for proper stacking
         */
        topIndex: function($container) {
            var base = 50; // Minimum z-index

            return Math.max.apply(
                null,
                $.map(
                    $container.find('div.panel'),
                    function(elem) {
                        return parseInt($(elem).css('z-index')) || base;
                    }
                )
            ) + 1;
        },

        /**
         * State when panel is outside container
         */
        initialState: function($container) {
            return {
                left: $container.width()
            };
        },

        /**
         * Get panel and breadcrumb behind specific panel
         */
        lower: function($container, $panel) {
            return _container.panels($container).filter(function() {
                return $(this).index() < $panel.index();
            });
        },

        /**
         * Get panel and breadcrumb stacked above specific panel
         */
        higher: function($container, $panel) {
            return _container.panels($container).filter(function() {
                return $(this).index() > $panel.index();
            });
        },

        /**
         * Generate new panel
         */
        create: function($container, options) {
            var $panel = $('<div>').addClass('panel').css({
                position: 'absolute',
                width: _panel.width($container, {
                    partial: options.partial
                }),
                zIndex: _panel.topIndex($container)
            }).append(
                // Shadow
                $('<div>').addClass('shadow')
            ).append(options.data);

            return $panel;
        }
    };

    /**
     * Browser -- jQuery widget
     */
    $.widget('cloudStack.cloudBrowser', {
        _init: function() {
            this.element.addClass('cloudStack-widget cloudBrowser');
            $('#breadcrumbs').append(
                $('<ul>')
            );
        },

        /**
         * Make target panel the top-most
         */
        selectPanel: function(args) {
            var $panel = args.panel;
            var $container = this.element;
            var $toShow = _panel.lower($container, $panel);
            var $toRemove = _panel.higher($container, $panel);
            var complete = args.complete;

            _breadcrumb.filter($toRemove).remove();
            _breadcrumb.filter($panel.siblings()).removeClass('active');
            _breadcrumb.filter($panel).addClass('active');
            _breadcrumb.filter($('div.panel')).find('span').css({
                opacity: 1
            });
            _breadcrumb.filter(
                $('div.panel.maximized')
                .removeClass('maximized')
                .addClass('reduced')
            ).removeClass('active maximized');

            $toRemove.remove();
            $toShow.show();
            $panel.css({
                left: _panel.position($container, {
                    maximized: $panel.hasClass('always-maximized')
                })
            });
            $panel.show().removeClass('reduced');
        },

        /**
         * Toggle selected panel as fully expanded, hiding/showing other panels
         */
        toggleMaximizePanel: function(args) {
            var $panel = args.panel;
            var $container = this.element;
            var $toHide = $panel.siblings(':not(.always-maximized)');
            var $shadow = $toHide.find('div.shadow');

            if (args.panel.hasClass('maximized')) {
                _breadcrumb.filter($panel).removeClass('maximized');
                $panel.removeClass('maximized');
                $panel.addClass('reduced');
                _breadcrumb.filter($panel.siblings()).find('span').css({
                    opacity: 1
                });
                $toHide.css({
                    left: _panel.position($container, {})
                });
                $shadow.show();
            } else {
                _breadcrumb.filter($panel).addClass('maximized');
                $panel.removeClass('reduced');
                $panel.addClass('maximized');
                $toHide.css(_panel.initialState($container));
                $shadow.hide();
            }
        },

        /**
         * Append new panel to end of container
         */
        addPanel: function(args) {
            var duration = args.duration ? args.duration : 500;
            var $container = this.element;
            var $parent = args.parent;
            var $panel, $reduced, targetPosition;

            // Create panel
            $panel = _panel.create(this.element, {
                partial: args.partial,
                data: args.data
            });

            // Remove existing panels from parent
            if ($parent) {
                // Cleanup transitioning panels -- prevent old complete actions from running
                $parent.siblings().stop();

                _breadcrumb.filter(
                    $('div.panel.maximized')
                    .removeClass('maximized')
                    .addClass('reduced')
                ).removeClass('active maximized');

                $parent.removeClass('maximized');
                _breadcrumb.filter($parent.next()).remove();
                $container.find($parent.next()).remove();
            }

            // Append panel
            $panel.appendTo($container);
            _breadcrumb.filter($panel.siblings()).removeClass('active');
            _breadcrumb.create($panel, args.title)
                .addClass('active')
                .appendTo('#breadcrumbs ul');

            // Reduced appearance for previous panels
            $panel.siblings().filter(function() {
                return $(this).index() < $panel.index();
            }).addClass('reduced');

            // Panel initial state
            if ($panel.index() == 0) $panel.addClass('always-maximized');
            $panel.css(
                _panel.initialState($container, $panel)
            );

            // Panel slide-in
            targetPosition = _panel.position($container, {
                maximized: args.maximizeIfSelected,
                partial: args.partial
            });
            if (!$panel.index()) {
                // Just show immediately if this is the first panel
                $panel.css({
                    left: targetPosition
                });
                if (args.complete) args.complete($panel, _breadcrumb.filter($panel));
            } else {
                // Animate slide-in
                $panel.css({
                    left: targetPosition
                });

                // Hide panels
                $panel.siblings().filter(function() {
                    return $(this).width() == $panel.width();
                });

                if ($panel.is(':visible') && args.complete) args.complete($panel);
            };

            return $panel;
        },

        removeLastPanel: function(args) {
            $('div.panel:last').stop(); // Prevent destroyed panels from animating
            this.element.find('div.panel:last').remove();
            this.element.find('div.panel:last').removeClass('reduced');
            $('#breadcrumbs').find('ul li:last').remove();
            $('#breadcrumbs').find('ul div.end').remove();
        },

        /**
         * Clear all panels
         */
        removeAllPanels: function(args) {
            $('div.panel').stop(); // Prevent destroyed panels from animating
            this.element.find('div.panel').remove();
            $('#breadcrumbs').find('ul li').remove();
            $('#breadcrumbs').find('ul div.end').remove();
        }
    });

    $('#breadcrumbs li').live('click', cloudStack.ui.event.bind(
        'cloudBrowser', {
            'breadcrumb': function($target, $browser, data) {

                if ($('#browser').hasClass('panel-highlight')) {
                    return false;
                }

                $browser.cloudBrowser('selectPanel', {
                    panel: data.panel
                });
            }
        }
    ));
})(jQuery, cloudStack);
