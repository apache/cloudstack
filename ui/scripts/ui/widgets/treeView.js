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
(function($, cloudStack, _s) {
    /**
     * Make <ul> of tree items
     */
    var makeTreeList = function(args) {
        var $treeList = $('<ul>');
        var $treeView = args.$treeView;

        args.dataProvider({
            context: $.extend(args.context, {
                parentDomain: args.parent
            }),
            response: {
                success: function(successArgs) {
                    $(successArgs.data).each(function() {
                        var itemData = this;

                        var $li = $('<li>')
                                .data('tree-view-item-id', this.id)
                                .data('tree-view-item-obj', this)
                                .append(
                                    $('<div>')
                                        .addClass('expand')
                                )
                                .append(
                                    $('<div>').addClass('name')
                                        .html(_s(this.name))
                                )
                                .appendTo($treeList);

                        $treeView.trigger('cloudStack.treeView.addItem', {
                            $li: $li,
                            itemData: itemData
                        });
                    });
                }
            }
        });

        return $treeList;
    };

    /**
     * Define an infinite 'tree' list
     */
    $.fn.treeView = function(args) {
        var $treeView = $('<div>')
            .appendTo(this)
            .addClass('view tree-view');
        var $toolbar = $('<div>')
            .addClass('toolbar')
            .append(
                $('<div>')
                .addClass('text-search')
                .append(
                    $('<div>')
                    .addClass('search-bar').attr('style', 'display:none') //no place to show search result in a tree, so hide it for now
                    .append(
                        $('<input>').attr('type', 'text')
                    )
                )
                .append(
                    $('<div>').addClass('button search').attr('style', 'display:none') //no place to show search result in a tree, so hide it for now
                )
        )
            .prependTo($treeView);
        var treeViewArgs = args.treeView;
        var $browser = args.$browser;

        if(treeViewArgs.overflowScroll) {
            $treeView.addClass('overflowScroll');
        }

        makeTreeList({
            $treeView: $treeView,
            parent: null,
            dataProvider: treeViewArgs.dataProvider,
            context: args.context
        }).appendTo($treeView);

        setTimeout(function() {
            $treeView.find('li:first div.name').click();
        }, 100);

        this.click(function(event) {
            var $target = $(event.target);
            var $li = $target.closest('li');

            if ($target.is('li div.expand') && $li.data('tree-view-item-obj')) {
                if ($li.find('ul').size()) {
                    $li.find('ul').remove();
                    $li.removeClass('expanded');

                    $treeView.trigger('cloudStack.treeView.removeItem', {
                        $li: $li
                    });

                    return false;
                }

                makeTreeList({
                    $treeView: $treeView,
                    parent: $li.data('tree-view-item-obj'),
                    dataProvider: treeViewArgs.dataProvider
                }).appendTo($li);
                $li.addClass('expanded');

                return false;
            }

            if ($target.is('li .name')) {
                $treeView.find('li .name').removeClass('selected');
                $target.addClass('selected');

                if ($browser && $browser.size()) {
                    $browser.cloudBrowser('addPanel', {
                        partial: true,
                        title: $target.html(),
                        data: '',
                        parent: $treeView.closest('div.panel'),
                        complete: function($panel) {
                            $panel.detailView($.extend(treeViewArgs.detailView, {
                                id: $li.data('tree-view-item-id'),
                                $browser: $browser,
                                context: {
                                    domains: [$li.data('tree-view-item-obj')]
                                }
                            }));
                        }
                    });
                }
            }

            return true;
        });

        // Action events
        $(window).bind('cloudstack.view-item-action', function(event, data) {
            var actionName = data.actionName;
            var $li = $treeView.find('li').filter(function() {
                return $(this).data('tree-view-item-id') == data.id;
            });

            if (actionName == 'destroy') {
                $li.animate({
                    opacity: 0.5
                });
                $li.bind('click', function() {
                    return false;
                });
            }
        });

        return this;
    };
})(jQuery, cloudStack, cloudStack.sanitize);
