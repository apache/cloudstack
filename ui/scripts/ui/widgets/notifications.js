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
(function($, cloudStack, _l) {
    /**
     * Notification handling
     */
    var notifications = {
        activeTasks: [],
        cornerAlert: function(args, options) {
            if (!options) options = {};

            var $container = $('#main-area'); // Put in main container box
            var $cornerAlert = $('<div>').addClass('notification corner-alert')
                .hide()
                .appendTo('html body')
                .append(
                    $('<div>').addClass('top-arrow'),
                    $('<div>').addClass('title').append(
                        $('<span>').html(
                            options.error ? options.error : _l('label.task.completed')
                        )
                    )
            )
                .append(
                    $('<div>').addClass('message')
                    .append(
                        $('<span>').html(_l(args.message))
                    )
            );

            if (options.error) {
                $cornerAlert.addClass('error');
            }

            return $cornerAlert
                .css({
                    opacity: 0,
                    position: 'absolute',
                    top: $('#header .notifications').offset().top,
                    left: $('#header .notifications').offset().left
                })
                .animate({
                    opacity: 1
                }, {
                    complete: function() {
                        setTimeout(function() {
                            $cornerAlert.fadeOut('fast', function() {
                                $cornerAlert.remove();
                            });
                        }, 3000);
                    }
                })
                .show();
        },
        add: function(args, $popup, $total) {
            var currentTotal = parseInt($total.html());
            var newTotal = currentTotal + 1;
            var desc = args.desc;
            var section = $('html body')
                .find('[cloudStack-container]')
                .data('cloudStack-args')
                .sections[args.section];
            var _custom = args._custom;

            var $item = $('<li>')
                .append(
                    $('<span>').html(_l(args.desc))
            )
                .append(
                    $('<div>').addClass('remove')
            );
            var additionalComplete = args.complete;

            // Get information for specified section path
            $item.data('notification-section', args.section);
            $item.data('notification-custom', _custom);
            $popup.find('ul').append($item);
            $total.html(newTotal);
            $total.parent().addClass('pending');
            $item.addClass('pending');

            // Setup timer
            var pollTimer = setInterval(function() {
                args.poll({
                    _custom: _custom,
                    pollTimer: pollTimer,
                    complete: function(args) {
                        clearInterval(pollTimer);

                        notifications.cornerAlert({
                            message: $item.html()
                        });
                        notifications.activeTasks.pop(pollTimer);
                        $item.removeClass('pending');

                        if (additionalComplete) additionalComplete();
                    },
                    incomplete: function(args) {},
                    error: function(args) {
                        if (args && args.message) {
                            cloudStack.dialog.notice({
                                message: _s(args.message)
                            });
                        }

                        clearInterval(pollTimer);
                        notifications.activeTasks.pop(pollTimer);
                        notifications.cornerAlert({
                            message: $item.html()
                        }, {
                            error: _l('label.error')
                        });
                        $item.removeClass('pending').addClass('error').append(
                            $('<div>').addClass('subtitle').html(args && args.message ?
                                                                 args.message : _l('label.error'))
                        );
                        $item.attr('title', args && args.message ? args.message : _l('label.error'));

                        if (additionalComplete) additionalComplete();
                    }
                });
            }, args.interval);
            notifications.activeTasks.push(pollTimer);

            return $total;
        },

        /**
         * Set total to 0
         */
        resetTotal: function($popup) {
            var $total = $popup.data('notifications-attach-to').find('div.total span');
            var $items = $popup.find('ul li');
            var total = $items.size();
            var completed = $items.filter(':not(.pending)').size();
            var newTotal = total - completed;

            if (newTotal < 0) newTotal = completed;

            $total.html(newTotal);

            if (!newTotal)
                $total.parent().removeClass('pending');
        },

        /**
         * Remove item from notification list
         */
        removeItem: function($popup, $item) {
            if ($item.closest('li').hasClass('pending')) return false;

            $item.remove();

            return true;
        },

        /**
         * Remove all completed notifications
         */
        clear: function($popup) {
            $popup.find('ul li').each(function() {
                var $item = $(this);

                if (!$item.hasClass('pending')) {
                    notifications.removeItem($popup, $item);
                }
            });
        },
        popup: {
            create: function($attachTo) {
                var $popup = $('<div>')
                    .addClass('notification-box')
                    .append(
                        // Header
                        $('<h3>').html(_l('label.notifications'))
                )
                    .append(
                        // Container
                        $('<div>').addClass('container')
                        .append(
                            // Notification list
                            $('<ul>')
                        )
                )
                    .append(
                        // Buttons
                        $('<div>').addClass('buttons')
                        .append(
                            // Clear list
                            $('<div>').addClass('button clear-list')
                            .append(
                                $('<span>').html(_l('label.clear.list'))
                            )
                        )
                        .append(
                            $('<div>').addClass('button close')
                            .append(
                                $('<span>').html(_l('label.close'))
                            )
                        )
                )
                    .css({
                        position: 'absolute'
                    })
                    .data('notifications-attach-to', $attachTo)
                    .hide();

                if (!$attachTo.hasClass('notifications')) $attachTo.addClass('notifications');
                $attachTo.data('notifications-popup', $popup);

                return $popup;
            },
            show: function($popup, $attachTo) {
                notifications.resetTotal($popup);
                return notifications.popup.reposition($popup, $attachTo)
                    .overlay({
                        closeAction: function() {
                            notifications.popup.hide($popup);
                        }
                    })
                    .fadeIn();
            },
            hide: function($popup) {
                $popup.fadeOut();
            },
            reposition: function($popup, $attachTo) {
                return $popup
                    .css({
                        zIndex: 10000,
                        top: $attachTo.offset().top + $attachTo.height() + 10,
                        left: $attachTo.offset().left - $attachTo.width()
                    });
            }
        }
    };

    /**
     * Define notification widget -- this is basically represented in a
     * notifications icon, that contains a pop-up list of notifications
     */
    $.fn.notifications = function(method, args) {
        var $attachTo = this;
        var $total = $attachTo.find('div.total span');
        var $popup;

        var init = function() {
            $popup = notifications.popup.create($attachTo).appendTo('html body');
        };

        if (method == 'add')
            notifications.add(args, $attachTo.data('notifications-popup'), $total);
        else
            init();

        return this;
    };

    /**
     * Notifications UI helpers
     */
    cloudStack.ui.notifications = {
        add: function(notification, success, successArgs, error, errorArgs) {
            if (!notification) {
                success(successArgs);

                return false;
            };

            var $notifications = $('div.notifications');

            if (!notification.poll) {
                cloudStack.ui.event.call('addNotification', {
                    section: notification.section,
                    desc: notification.desc,
                    interval: 0,
                    poll: function(args) {
                        success(successArgs);
                        args.complete();
                    }
                });
            } else {
                cloudStack.ui.event.call('addNotification', {
                    section: notification.section,
                    desc: notification.desc,
                    interval: notification.interval ? notification.interval : g_queryAsyncJobResultInterval,
                    _custom: notification._custom,
                    poll: function(args) {
                        var complete = args.complete;
                        var notificationError = args.error;

                        notification.poll({
                            _custom: args._custom,
                            complete: function(args) {
                                success($.extend(successArgs, args));
                                complete(args);
                            },
                            error: function(args) {
                                error($.extend(errorArgs, args));
                                notificationError(args);
                            }
                        });
                    }
                });
            }

            return true;
        }
    };

    // Setup notification listener -- accepts same args as
    $(window).bind('cloudStack.addNotification', function(event, data) {
        $('.notifications').notifications('add', data);
    });

    $(document).click(function(event) {
        var $target = $(event.target);
        var $attachTo, $popup;

        // Notifications header area
        if ($target.closest('.notifications').size()) {
            $attachTo = $target.closest('.notifications');
            $popup = $attachTo.data('notifications-popup');
            notifications.popup.show($popup, $attachTo);

            return false;
        }

        // Notification item
        if ($target.is('.notification-box li span')) {
            var $li = $target.closest('.notification-box li');

            $('#navigation ul li').filter(function() {
                return $(this).hasClass($li.data('notification-section'));
            }).click();
            $('div.overlay').click();

            return false;
        }

        // Popup
        if ($target.closest('div.notification-box').size()) {
            $popup = $target.closest('div.notification-box');

            // Clear list
            if ($target.closest('.button.clear-list').size()) {
                notifications.clear($popup);
            }

            // Remove instance item
            else if ($target.hasClass('remove')) {
                notifications.removeItem($popup, $target.closest('li'));
            }

            // Close button
            else if ($target.closest('.button.close')) {
                $('div.overlay').click();
            }

            return false;
        }

        return true;
    });

    $(window).resize(function(event) {
        var $popup = $('div.notification-box:visible');

        if ($popup.size())
            notifications.popup.reposition($popup, $popup.data('notifications-attach-to'));
    });
})(window.jQuery, window.cloudStack, window._l);
