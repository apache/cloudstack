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
    var isFormValid = function($form) {
        var key = $form.find('input[name=key]').val();
        var value = $form.find('input[name=value]').val();

        if (!key || !value) {
            cloudStack.dialog.notice({
                message: 'message.specifiy.tag.key.value'
            });
            return false;
        }

        if ($form.find('div.field.key').find('label.error').css('display') == 'block' || $form.find('div.field.value').find('label.error').css('display') == 'block')
            return false;

        return true;
    };

    var elems = {
        inputArea: function(args) {
            var $form = $('<form>').addClass('tag-input');
            var $keyField = $('<div>').addClass('field key');
            var $keyLabel = $('<label>').attr('for', 'key').html(_l('label.key') + ':');
            var $key = $('<input>').addClass('key disallowSpecialCharacters').attr('name', 'key');
            var $valueField = $('<div>').addClass('field value');
            var $valueLabel = $('<label>').attr('for', 'value').html(_l('label.value') + ':');
            var $value = $('<input>').addClass('value disallowSpecialCharacters').attr('name', 'value');
            var $submit = $('<input>').attr('type', 'submit').val(_l('label.add'));

            $keyField.append($keyLabel, $key);
            $valueField.append($valueLabel, $value);
            $form.append(
                $keyField, $valueField,
                $submit
            );

            $form.validate();

            $form.submit(
                args.onSubmit ?
                function() {
                    if (!isFormValid($form)) return false;

                    args.onSubmit({
                        data: cloudStack.serializeForm($form),
                        response: {
                            success: function() {
                                // Restore editing of input
                                $key.attr('disabled', false);
                                $value.attr('disabled', false);

                                // Clear out old data
                                $key.val('');
                                $value.val('');
                                $key.focus();
                            },
                            error: function() {
                                // Restore editing of input
                                $key.attr('disabled', false);
                                $value.attr('disabled', false);
                                $key.focus();
                            }
                        }
                    });

                    // Prevent input during submission
                    $key.attr('disabled', 'disabled');
                    $value.attr('disabled', 'disabled');

                    return false;
                } :
                function() {
                    return false;
                }
            );

            return $form;
        },
        tagItem: function(title, onRemove, data) {
            var $li = $('<li>');
            var $label = $('<span>').addClass('label');
            var $remove = $('<span>').addClass('remove').html('X');
            var $key = $('<span>').addClass('key').html(_s(data.key));
            var $value = $('<span>').addClass('value').html(_s(data.value));

            $label.append($key, '<span>=</span>', $value);
            $label.attr('title', title);
            $remove.click(function() {
                if (onRemove) onRemove($li, data);
            });

            $li.append($remove, $label);

            return $li;
        },

        info: function(text) {
            var $info = $('<div>').addClass('tag-info');
            var $text = $('<span>').html(text);

            $text.appendTo($info);

            return $info;
        }
    };

    $.widget('cloudStack.tagger', {
        _init: function(args) {
            var context = this.options.context;
            var jsonObj = this.options.jsonObj;
            var dataProvider = this.options.dataProvider;
            var actions = this.options.actions;
            var $container = this.element.addClass('tagger');
            var $tagArea = $('<ul>').addClass('tags');
            var $title = elems.info(_l('label.tags')).addClass('title');
            var $loading = $('<div>').addClass('loading-overlay');

            var onRemoveItem = function($item, data) {
                $loading.appendTo($container);
                actions.remove({
                    context: $.extend(true, {}, context, {
                        tagItems: [data]
                    }),
                    response: {
                        success: function(args) {
                            var notification = $.extend(true, {}, args.notification, {
                                interval: 500,
                                _custom: args._custom
                            });

                            cloudStack.ui.notifications.add(
                                notification,

                                // Success

                                function() {
                                    $loading.remove();
                                    $item.remove();
                                }, {},

                                // Error

                                function() {
                                    $loading.remove();
                                }, {}
                            );
                        },
                        error: function(message) {
                            $loading.remove();
                            cloudStack.dialog.notice({
                                message: message
                            });
                        }
                    }
                });
            };

            var $inputArea = elems.inputArea({
                onSubmit: function(args) {
                    var data = args.data;
                    var success = args.response.success;
                    var error = args.response.error;
                    var title = data.key + ' = ' + data.value;

                    $loading.appendTo($container);
                    actions.add({
                        data: data,
                        context: context,
                        response: {
                            success: function(args) {
                                var notification = $.extend(true, {}, args.notification, {
                                    interval: 500,
                                    _custom: args._custom
                                });

                                cloudStack.ui.notifications.add(
                                    notification,

                                    // Success

                                    function() {
                                        $loading.remove();
                                        elems.tagItem(title, onRemoveItem, data).appendTo($tagArea);
                                        success();
                                    }, {},

                                    // Error

                                    function() {
                                        $loading.remove();
                                        error();
                                    }, {}
                                );
                            },
                            error: function(message) {
                                $loading.remove();
                                error();
                                cloudStack.dialog.notice({
                                    message: message
                                });
                            }
                        }
                    });
                }
            });

            $container.append($title, $inputArea, $tagArea);

            // Get data
            $loading.appendTo($container);
            dataProvider({
                context: context,
                jsonObj: jsonObj,
                response: {
                    success: function(args) {
                        var data = args.data;

                        $loading.remove();
                        $(data).map(function(index, item) {
                            var key = item.key;
                            var value = item.value;
                            var data = {
                                key: key,
                                value: value
                            };

                            elems.tagItem(key + ' = ' + value, onRemoveItem, data).appendTo($tagArea);
                        });
                    },
                    error: function(message) {
                        $loading.remove();
                        $container.find('ul').html(message);
                    }
                }
            });
        }
    });
}(jQuery, cloudStack));
