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
    cloudStack.uiCustom.enableStaticNAT = function(args) {
        var listView = args.listView;
        var action = args.action;
        var tierSelect = args.tierSelect;

        return function(args) {
            var context = args.context;
            var $instanceRow = args.$instanceRow;

            var vmList = function(args) {
                // Create a listing of instances, based on limited information
                // from main instances list view
                var $listView;
                var instances = $.extend(true, {}, args.listView, {
                    context: context,
                    uiCustom: true
                });

                instances.listView.multiSelect = false;

                instances.listView.actions = {
                    select: {
                        label: _l('label.select.instance'),
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

                $listView = $('<div>').listView(instances);

                // Change action label
                $listView.find('th.actions').html(_l('label.select'));

                return $listView;
            };

            var $dataList = vmList({
                listView: listView
            }).dialog({
                dialogClass: 'multi-edit-add-list panel',
                width: 825,
                title: _l('label.select.vm.for.static.nat'),
                buttons: [{
                    text: _l('label.apply'),
                    'class': 'ok',
                    click: function() {
                        if ($dataList.find('.tier-select select').val() == -1) {
                            cloudStack.dialog.notice({
                                message: ('Please select a tier')
                            });
                            return false;
                        }

                        if (!$dataList.find(
                            'input[type=radio]:checked, input[type=checkbox]:checked'
                        ).size()) {
                            cloudStack.dialog.notice({
                                message: _l('message.select.instance')
                            });

                            return false;
                        }

                        var complete = args.complete;
                        var start = args.start;

                        start();
                        $dataList.fadeOut(function() {
                            action({
                                tierID: $dataList.find('.tier-select select').val(),
                                _subselect: $dataList.find('tr.multi-edit-selected .subselect select').val(),
                                context: $.extend(true, {}, context, {
                                    instances: [
                                        $dataList.find('tr.multi-edit-selected').data('json-obj')
                                    ]
                                }),
                                response: {
                                    success: function(args) {
                                        complete({
                                            $item: $instanceRow
                                        });
                                    },
                                    error: function(args) {
                                        cloudStack.dialog.notice({
                                            message: args
                                        });
                                    }
                                }
                            });
                            $dataList.remove();
                        });

                        $('div.overlay').fadeOut(function() {
                            $('div.overlay').remove();
                        });
                    }
                }, {
                    text: _l('label.cancel'),
                    'class': 'cancel',
                    click: function() {
                        $dataList.fadeOut(function() {
                            $dataList.remove();
                        });
                        $('div.overlay').fadeOut(function() {
                            $('div.overlay').remove();
                        });
                    }
                }]
            }).parent('.ui-dialog').overlay();

            // Add tier select dialog
            if (tierSelect) {
                var $toolbar = $dataList.find('.toolbar');
                var $tierSelect = $('<div>').addClass('filters tier-select').prependTo($toolbar);
                var $tierSelectLabel = $('<label>').html('Select tier').appendTo($tierSelect);
                var $tierSelectInput = $('<select>').appendTo($tierSelect);

                // Get tier data
                tierSelect({
                    context: context,
                    $tierSelect: $tierSelect,
                    response: {
                        success: function(args) {
                            var data = args.data;

                            $(data).map(function(index, item) {
                                var $option = $('<option>');

                                $option.attr('value', item.id);
                                $option.html(item.description);
                                $option.appendTo($tierSelectInput);
                            });
                        },
                        error: function(message) {
                            cloudStack.dialog.notice({
                                message: message ? message : 'Could not retrieve VPC tiers'
                            });
                        }
                    }
                });
            }
        };
    };
}(cloudStack, jQuery));
