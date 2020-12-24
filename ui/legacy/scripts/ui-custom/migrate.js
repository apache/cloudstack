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
(function(cloudStack, $) {
    cloudStack.uiCustom.migrate = function(args) {
        var listView = args.listView;
        var action = args.action;

        return function(args) {
            var context = args.context;

            var hostList = function(args) {
                var $listView;

                var hosts = $.extend(true, {}, args.listView, {
                    context: context,
                    uiCustom: true
                });

                hosts.listView.actions = {
                    select: {
                        label: _l('label.select.host'),
                        type: 'radio',
                        action: {
                            uiCustom: function(args) {
                                var $item = args.$item;
                                var $input = $item.find('td.actions input:visible');

                                if ($input.attr('type') == 'checkbox') {
                                    if ($input.is(':checked'))
                                        $item.addClass('multi-edit-selected');
                                    else
                                        $item.removeClass('multi-edit-selected');
                                } else {
                                    $item.siblings().removeClass('multi-edit-selected');
                                    $item.addClass('multi-edit-selected');
                                }
                            }
                        }
                    }
                };

                $listView = $('<div>').listView(hosts);

                // Change action label
                $listView.find('th.actions').html(_l('label.select'));

                return $listView;
            };

            var $dataList = hostList({
                listView: listView
            }).dialog({
                dialogClass: 'multi-edit-add-list panel migrate-vm-available-host-list',
                width: 825,
                draggable: false,
                title: _l('label.migrate.instance.to.host'),
                buttons: [{
                    text: _l('label.ok'),
                    'class': 'ok migrateok',
                    click: function() {
                        var complete = args.complete;
                        var selectedHostObj = $dataList.find('tr.multi-edit-selected').data('json-obj');
                        if(selectedHostObj != undefined) {
                            $dataList.fadeOut(function() {
                                action({
                                    context: $.extend(true, {}, context, {
                                        selectedHost: [
                                            selectedHostObj
                                        ]
                                    }),
                                    response: {
                                        success: function(args) {
                                            complete({
                                                _custom: args._custom,
                                                $item: $('<div>'),
                                            });
                                        },
                                        error: function(args) {
                                            cloudStack.dialog.notice({
                                                message: args
                                            });
                                        }
                                    }
                                });
                            });

                            $('div.overlay').fadeOut(function() {
                                $('div.overlay').remove();
                                $(':ui-dialog').dialog('destroy');
                            });
                        }
                        else {
                            cloudStack.dialog.notice({
                                message: _l('message.migrate.instance.select.host')
                            });
                        }
                    }
                }, {
                    text: _l('label.cancel'),
                    'class': 'cancel migratecancel',
                    click: function() {
                        $dataList.fadeOut(function() {
                            $dataList.remove();
                        });
                        $('div.overlay').fadeOut(function() {
                            $('div.overlay').remove();
                            $(':ui-dialog').dialog('destroy');
                        });
                    }
                }]
            });
            cloudStack.applyDefaultZindexAndOverlayOnJqueryDialogAndRemoveCloseButton($dataList);
        };
    };
}(cloudStack, jQuery));
