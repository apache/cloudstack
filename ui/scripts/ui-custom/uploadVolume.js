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
    cloudStack.uiCustom.uploadVolume = function(args) {
        var listView = args.listView;
        var action = args.action;

        var validate = function($uploadVolume) {
            if (!$uploadVolume.find('input[type=text]').val()) {
                cloudStack.dialog.notice({
                    message: _l('message.specify.url')
                });

                return false;
            }

            if (!$uploadVolume.find(
                'input[type=radio]:checked, input[type=checkbox]:checked'
            ).size()) {
                cloudStack.dialog.notice({
                    message: _l('message.select.instance')
                });

                return false;
            }

            return true;
        };

        return function(args) {
            var $uploadVolume = $('<div>').addClass('upload-volume');
            var context = args.context;
            var topFields = function() {
                var $form = $('<form>').addClass('top-fields');
                var $urlLabel = $('<label>').html(_l('label.url') + ':');
                var $urlField = $('<div>').addClass('field url');
                var $nameLabel = $('<label>').html(_l('label.name') + ':');
                var $nameField = $('<div>').addClass('field name');
                var $urlInput = $('<input>').attr({
                    type: 'text',
                    name: 'url'
                }).addClass('required');
                var $nameInput = $('<input>').attr({
                    type: 'text',
                    name: 'name'
                }).addClass('required');

                $urlField.append($urlLabel, $urlInput);
                $nameField.append($nameLabel, $nameInput);
                $form.append($nameField, $urlField);

                return $form;
            };
            var vmList = function(args) {
                // Create a listing of instances, based on limited information
                // from main instances list view
                var $listView;
                var instances = $.extend(true, {}, args.listView, {
                    context: context,
                    uiCustom: true
                });

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

            $uploadVolume.append(
                topFields,
                $('<div>').addClass('desc').html(_l('label.select.instance.to.attach.volume.to') + ':'),
                $('<div>').addClass('listView-container').append(
                    vmList({
                        listView: listView
                    })
                )
            );
            $uploadVolume.dialog({
                dialogClass: 'multi-edit-add-list panel',
                width: 900,
                title: _l('label.upload.volume'),
                buttons: [{
                    text: _l('label.upload'),
                    'class': 'ok',
                    click: function() {
                        if (!validate($uploadVolume)) return false;

                        var complete = args.complete;
                        var $loading = $('<div>').addClass('loading-overlay');

                        $loading.appendTo($uploadVolume);
                        action({
                            data: cloudStack.serializeForm($uploadVolume.find('form')),
                            context: $.extend(true, {}, context, {
                                instances: [
                                    $uploadVolume.find('tr.multi-edit-selected').data('json-obj')
                                ]
                            }),
                            response: {
                                success: function(args) {
                                    $('.ui-dialog').fadeOut(function() {
                                        $('.ui-dialog').remove();
                                        $(window).trigger('cloudStack.fullRefresh');
                                    });
                                    $('div.overlay').fadeOut(function() {
                                        $('div.overlay').remove();
                                    });
                                    complete({
                                        $item: $('<div>'),
                                        _custom: args._custom
                                    });
                                },
                                error: function(args) {
                                    $loading.remove();
                                    cloudStack.dialog.notice({
                                        message: args
                                    });
                                }
                            }
                        });
                    }
                }, {
                    text: _l('label.cancel'),
                    'class': 'cancel',
                    click: function() {
                        $('.ui-dialog').fadeOut(function() {
                            $('.ui-dialog').remove();
                        });
                        $('div.overlay').fadeOut(function() {
                            $('div.overlay').remove();
                        });
                    }
                }]
            }).closest('.ui-dialog').overlay();
        };
    };
}(cloudStack, jQuery));
