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
    var getMultiData = function($multi) {
        return cloudStack.serializeForm($multi.find('form'));
    };

    var _medit = cloudStack.ui.widgets.multiEdit = {
        /**
         * Append item to list
         */
        addItem: function(data, fields, $multi, itemData, actions, options) {
            if (!options) options = {};

            var $tr;
            var $item = $('<div>').addClass('data-item');
            var multiRule = data;
            var reorder = options.reorder;

            $item.append($('<table>').append($('<tbody>')));
            $tr = $('<tr>').appendTo($item.find('tbody'));
            $item.data('json-obj', multiRule);

            if (itemData) {
                $tr.data('multi-edit-data', itemData);
            }

            // Add reorder actions
            if (reorder) {
                $('<td>').addClass('actions reorder').appendTo($tr).append(function() {
                    var $td = $(this);

                    $.each(reorder, function(actionName, action) {
                        var fnLabel = {
                            moveTop: _l('label.move.to.top'),
                            moveBottom: _l('label.move.to.bottom'),
                            moveUp: _l('label.move.up.row'),
                            moveDown: _l('label.move.down.row'),
                            moveDrag: _l('label.drag.new.position')
                        };

                        $('<div>')
                            .addClass('action reorder')
                            .addClass(actionName)
                            .append(
                                $('<span>').addClass('icon').html('&nbsp;')
                        )
                            .attr({
                                title: _l(fnLabel[actionName])
                            })
                            .appendTo($td)
                            .click(function() {
                                if (actionName == 'moveDrag') return false;

                                rowActions[actionName]($tr);
                                $tr.closest('.data-body').find('.data-item').each(function() {
                                    sort($(this), action);
                                });

                                return false;
                            });
                    });
                });
            }


            // Setup columns
            $.each(fields, function(fieldName, field) {
                if (!field || (options.ignoreEmptyFields && !data[fieldName])) {
                    return true;
                }

                var isHidden = $multi.find('th.' + fieldName).hasClass('always-hide');

                if (isHidden) {
                    return true;
                }

                var $td = $('<td>').addClass(fieldName).appendTo($tr);
                var $input, val;
                var $addButton = $multi.find('form .button.add-vm:not(.custom-action)').clone();
                var newItemRows = [];
                var addItemAction = function(data) {
                    var $loading = $('<div>').addClass('loading-overlay');
                    var complete = function(args) {
                        var $tbody = $item.find('.expandable-listing tbody');

                        $loading.remove();
                        $(data).each(function() {
                            var item = this;
                            var $itemRow = _medit.multiItem.itemRow(item, options.itemActions, multiRule, $tbody);

                            $itemRow.appendTo($tbody);
                            newItemRows.push($itemRow);

                            cloudStack.evenOdd($tbody, 'tr:visible', {
                                even: function($elem) {
                                    $elem.removeClass('odd');
                                    $elem.addClass('even');
                                },
                                odd: function($elem) {
                                    $elem.removeClass('even');
                                    $elem.addClass('odd');
                                }
                            });
                        });
                    };
                    var error = function() {
                        $(newItemRows).each(function() {
                            var $itemRow = this;

                            $itemRow.remove();
                        });
                        $loading.remove();
                    };

                    $loading.prependTo($item);
                    options.itemActions.add.action({
                        context: options.context,
                        data: data,
                        multiRule: multiRule,
                        response: {
                            success: function(args) {
                                var notificationError = function(args) {
                                    error();
                                };

                                cloudStack.ui.notifications.add(args.notification,
                                    complete, {},
                                    notificationError, {});
                            },
                            error: error
                        }
                    });
                };

                if (!itemData) itemData = [{}];

                if (!options.noSelect &&
                    $multi.find('th,td').filter(function() {
                        return $(this).attr('rel') == fieldName;
                    }).is(':hidden')) {
                    return true;
                }

                if (!field.isPassword) {
                    if (field.edit) {
                        // Edit fields append value of data
                        if (field.range) {
                            var start = _s(data[field.range[0]]);
                            var end = _s(data[field.range[1]]);

                            $td.append($('<span>').html(start + ' - ' + end));
                        } else {
                            var maxLengths = data['_maxLength'];

                            if (maxLengths &&
                                maxLengths[fieldName] &&
                                data[fieldName].length >= maxLengths[fieldName]) {
                                $td.append($('<span>').html(_s(data[fieldName].toString().substr(0, maxLengths[fieldName] - 3).concat('...'))));
                            } else {
                                $td.append($('<span>').html(_s(data[fieldName])));
                            }
                            $td.attr('title', data[fieldName]);
                        }
                    } else if (field.select) {
                        // Get matching option text
                        var $matchingSelect = $multi.find('select')
                            .filter(function() {
                                return $(this).attr('name') == fieldName;
                            });
                        var $matchingOption = $matchingSelect.find('option')
                            .filter(function() {
                                return $(this).val() == data[fieldName];
                            });

                        var matchingValue = $matchingOption.size() ?
                            $matchingOption.html() : data[fieldName];

                        $td.append($('<span>').html(_s(matchingValue)));
                    } else if (field.addButton && !options.noSelect) {
                        if (options.multipleAdd) {
                            $addButton.click(function() {
                                var context = $.extend(true, {}, options.context);

                                if ($td.hasClass('disabled')) return false;

                                var $subItems = $td.closest('.data-item').find('.expandable-listing tr');

                                if ($subItems.size()) {
                                    context.subItemData = $subItems.map(function() {
                                        return $(this).data('json-obj');
                                    });
                                }

                                _medit.vmList($multi,
                                    options.listView,
                                    context,
                                    options.multipleAdd, _l('label.add.vms'),
                                    addItemAction, {
                                        multiRule: multiRule
                                    });

                                return true;
                            });
                            $td.append($addButton);
                        } else {
                            // Show VM data
                            var itemName = data._itemName ? itemData[0][data._itemName] : itemData[0].name;
                            $td.html(options.multipleAdd ?
                                itemData.length + ' VMs' : itemName);
                            $td.click(function() {
                                var $browser = $(this).closest('.detail-view').data('view-args').$browser;

                                if (options.multipleAdd) {
                                    _medit.multiItem.details(itemData, $browser);
                                } else {
                                    _medit.details(itemData[0], $browser, {
                                        context: options.context,
                                        itemName: itemName
                                    });
                                }
                            });
                        }
                    } else if (field.custom) {
                        var $button = $('<div>').addClass('button add-vm custom-action');

                        $td.data('multi-custom-data', data[fieldName]);
                        $button.html(data && data[fieldName] && data[fieldName]['_buttonLabel'] ?
                            _l(data[fieldName]['_buttonLabel']) : _l(field.custom.buttonLabel));
                        $button.click(function() {
                            if ($td.hasClass('disabled')) return false;

                            var $button = $(this);
                            var context = $.extend(true, {},
                                options.context ?
                                options.context : cloudStack.context, {
                                    multiRules: [data]
                                });

                            field.custom.action({
                                context: context,
                                data: $td.data('multi-custom-data'),
                                $item: $td,
                                response: {
                                    success: function(args) {
                                        if (args.data['_buttonLabel']) {
                                            $button.html(_l(args.data['_buttonLabel']));
                                        }
                                        $td.data('multi-custom-data', args.data);
                                    }
                                }
                            });

                            return true;
                        });
                        $button.appendTo($td);
                    }
                }

                // Add blank styling for empty fields
                if ($td.html() == '') {
                    $td.addClass('blank');
                }

                if (data._hideFields &&
                    $.inArray(fieldName, data._hideFields) > -1) {
                    $td.addClass('disabled');
                }

                return true;
            });

            // Actions column
            var $actions = $('<td>').addClass('multi-actions').appendTo($item.find('tr'));

            // Align action column width
            $actions.width($multi.find('th.multi-actions').width() + 4);

            // Action filter
            var allowedActions = options.preFilter ? options.preFilter({
                actions: $.map(actions, function(value, key) {
                    return key;
                }),
                context: $.extend(true, {}, options.context, {
                    multiRule: [data],
                    actions: $.map(actions, function(value, key) {
                        return key;
                    })
                })
            }) : null;

            // Append actions
            $.each(actions, function(actionID, action) {
                if (allowedActions && $.inArray(actionID, allowedActions) == -1) return true;

                $actions.append(
                    $('<div>').addClass('action')
                    .addClass(actionID)
                    .append($('<span>').addClass('icon'))
                    .attr({
                        title: _l(action.label)
                    })
                    .click(function() {
                        var performAction = function(actionOptions) {
                            if (!actionOptions) actionOptions = {};

                            action.action({
                                context: $.extend(true, {}, options.context, {
                                    multiRule: [data]
                                }),
                                data: actionOptions.data,
                                response: {
                                    success: function(args) {
                                        var notification = args ? args.notification : null;
                                        var _custom = args ? args._custom : null;
                                        if (notification) {
                                            $('.notifications').notifications('add', {
                                                section: 'network',
                                                desc: notification.label,
                                                interval: 3000,
                                                _custom: _custom,
                                                poll: function(args) {
                                                    var complete = args.complete;
                                                    var error = args.error;

                                                    notification.poll({
                                                        _custom: args._custom,
                                                        complete: function(args) {
                                                            if (isDestroy) {
                                                                $loading.remove();
                                                                $dataItem.remove();
                                                            } else {
                                                                $multi.trigger('refresh');
                                                            }

                                                            complete();

                                                            if (actionOptions.complete) actionOptions.complete();
                                                        },
                                                        error: function(args) {
                                                            error(args);
                                                            $multi.trigger('refresh');

                                                            return cloudStack.dialog.error;
                                                        }
                                                    });
                                                }
                                            });
                                        } else {
                                            $loading.remove();
                                            if (isDestroy) {
                                                $dataItem.remove();
                                            }
                                        }
                                    },
                                    error: function(message) {
                                        cloudStack.dialog.notice({
                                            message: message
                                        });
                                        $item.show();
                                        $dataItem.find('.loading-overlay').remove();
                                    }
                                }
                            });
                        };

                        var $target = $(this);
                        var $dataItem = $target.closest('.data-item');
                        var $expandable = $dataItem.find('.expandable-listing');
                        var isDestroy = $target.hasClass('destroy');
                        var isEdit = $target.hasClass('edit');
                        var createForm = action.createForm;
                        var reorder = options.reorder;

                        if (isDestroy) {
                            var $loading = _medit.loadingItem($multi, _l('label.removing') + '...');

                            if ($expandable.is(':visible')) {
                                $expandable.slideToggle(function() {
                                    $dataItem.hide();
                                    $dataItem.after($loading);
                                });
                            } else {
                                // Loading appearance
                                $dataItem.hide();
                                $dataItem.after($loading);
                            }
                        }

                        if (!isEdit) {
                            if (createForm) {
                                cloudStack.dialog.createForm({
                                    form: createForm,
                                    after: function(args) {
                                        var $loading = $('<div>').addClass('loading-overlay').prependTo($dataItem);
                                        performAction({
                                            data: args.data,
                                            complete: function() {
                                                $multi.trigger('refresh');
                                            }
                                        });
                                    }
                                });
                            } else {
                                performAction();
                            }
                        } else {
                            // Get editable fields
                            var editableFields = {};

                            $.each(fields, function(key, field) {
                                field.isDisabled = false;

                                if (field && field.isEditable) editableFields[key] = $.extend(true, {}, field, {
                                    defaultValue: data[key]
                                });
                            });

                            cloudStack.dialog.createForm({
                                form: {
                                    title: 'label.edit.rule',
                                    desc: '',
                                    fields: editableFields
                                },
                                after: function(args) {
                                    var $loading = $('<div>').addClass('loading-overlay').prependTo($dataItem);
                                    performAction({
                                        data: args.data,
                                        complete: function() {
                                            $multi.trigger('refresh');
                                        }
                                    });
                                }
                            });
                        }
                    })
                );
            });

            // Add tagger action
            if (options.tags) {
                $actions.prepend(
                    $('<div></div>')
                    .addClass('action editTags')
                    .attr('title', _l('label.edit.tags'))
                    .append($('<span></span>').addClass('icon'))
                    .click(function() {
                        $('<div>')
                            .dialog({
                                dialogClass: 'editTags',
                                title: _l('label.edit.tags'),
                                width: 400,
                                buttons: [{
                                    text: _l('label.done'),
                                    'class': 'ok',
                                    click: function() {
                                        $(this).dialog('destroy');
                                        $('div.overlay:last').remove();

                                        return true;
                                    }
                                }]
                            })
                            .append(
                                $('<div></div>').addClass('multi-edit-tags').tagger($.extend(true, {}, options.tags, {
                                    context: $.extend(true, {}, options.context, {
                                        multiRule: [multiRule]
                                    })
                                }))
                        )
                            .closest('.ui-dialog').overlay();

                        return false;
                    })
                )
            }

            // Add expandable listing, for multiple-item
            if (options.multipleAdd) {
                // Create expandable box
                _medit.multiItem.expandable($item.find('tr').data('multi-edit-data'),
                    options.itemActions,
                    multiRule).appendTo($item);

                // Expandable icon/action
                $item.find('td:first').prepend(
                    $('<div>').addClass('expand').click(function() {
                        $item.closest('.data-item').find('.expandable-listing').slideToggle();
                    }));
            }

            return $item;
        },

        vmList: function($multi, listView, context, isMultipleAdd, label, complete, options) {
            if (!options) options = {};

            // Create a listing of instances, based on limited information
            // from main instances list view
            var $listView;
            var instances = $.extend(true, {}, listView, {
                context: $.extend(true, {}, context, {
                    multiData: getMultiData($multi),
                    multiRule: options.multiRule ? [options.multiRule] : null
                }),
                uiCustom: true
            });

            instances.listView.multiSelect = false;

            instances.listView.actions = {
                select: {
                    label: 'label.select.instance',
                    type: isMultipleAdd ? 'checkbox' : 'radio',
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
            $listView.find('th.actions').html(_l('Select'));

            var $dataList = $listView.addClass('multi-edit-add-list').dialog({
                dialogClass: 'multi-edit-add-list panel',
                width: 825,
                title: label,
                buttons: [{
                    text: _l('label.apply'),
                    'class': 'ok',
                    click: function() {
                        if (!$listView.find('input[type=radio]:checked, input[type=checkbox]:checked').size()) {
                            cloudStack.dialog.notice({
                                message: _l('message.select.item')
                            });

                            return false;
                        }

                        $dataList.fadeOut(function() {
                            complete($.map(
                                $listView.find('tr.multi-edit-selected'),

                                // Attach VM data to row

                                function(elem) {
                                    var itemData = $(elem).data('json-obj');
                                    var $subselect = $(elem).find('.subselect select');

                                    // Include subselect data
                                    if ($subselect && $subselect.val()) {
                                        return $.extend(itemData, {
                                            _subselect: $subselect.val()
                                        });
                                    }

                                    return itemData;
                                }
                            ));
                            $dataList.remove();
                        });

                        $('div.overlay').fadeOut(function() {
                            $('div.overlay').remove();
                        });

                        return true;
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
        },

        /**
         * Align width of each data row to main header
         */
        refreshItemWidths: function($multi) {
            $multi.find('.data-body').width(
                $multi.find('form > table.multi-edit').width()
            );

            $multi.find('.data tr').filter(function() {
                return !$(this).closest('.expandable-listing').size();
            }).each(function() {
                var $tr = $(this);

                $tr.find('td').each(function() {
                    var $td = $(this);

                    $td.width($($multi.find('th:visible')[$td.index()]).width() + 5);
                });
            });
        },

        /**
         * Create a fake 'loading' item box
         */
        loadingItem: function($multi, label) {
            var $loading = $('<div>').addClass('data-item loading');

            // Align height with existing items
            var $row = $multi.find('.data-item:first');

            // Set label
            if (label) {
                $loading.append(
                    $('<div>').addClass('label').append(
                        $('<span>').html(_l(label))
                    )
                );
            }

            return $loading;
        },
        details: function(data, $browser, options) {
            if (!options) options = {};

            var detailViewArgs, $detailView;

            detailViewArgs = $.extend(true, {}, cloudStack.sections.instances.listView.detailView);
            detailViewArgs.actions = null;
            detailViewArgs.$browser = $browser;
            detailViewArgs.id = data.id;
            detailViewArgs.jsonObj = data;
            detailViewArgs.context = options.context;

            $browser.cloudBrowser('addPanel', {
                title: options.itemName ? options.itemName : data.name,
                maximizeIfSelected: true,
                complete: function($newPanel) {
                    $newPanel.detailView(detailViewArgs);
                }
            });
        },
        multiItem: {
            /**
             * Show listing of load balanced VMs
             */
            details: function(data, $browser) {
                var listViewArgs, $listView;

                // Setup list view
                listViewArgs = $.extend(true, {}, cloudStack.sections.instances);
                listViewArgs.listView.actions = null;
                listViewArgs.listView.filters = null;
                listViewArgs.$browser = $browser;
                listViewArgs.listView.detailView.actions = null;
                listViewArgs.listView.dataProvider = function(args) {
                    setTimeout(function() {
                        args.response.success({
                            data: data
                        });
                    }, 50);
                };
                $listView = $('<div>').listView(listViewArgs);

                // Show list view of selected VMs
                $browser.cloudBrowser('addPanel', {
                    title: _l('label.item.listing'),
                    data: '',
                    noSelectPanel: true,
                    maximizeIfSelected: true,
                    complete: function($newPanel) {
                        return $newPanel.listView(listViewArgs);
                    }
                });
            },

            itemRow: function(item, itemActions, multiRule, $tbody) {
                var $tr = $('<tr>');

                var itemName = multiRule._itemName ? item[multiRule._itemName] : item.name;
                var $itemName = $('<span>').html(_s(itemName));

                $tr.append($('<td>').addClass('name').appendTo($tr).append($itemName));

                $itemName.click(function() {
                    _medit.details(item, $('#browser .container'), {
                        itemName: itemName,
                        context: {
                            instances: [item]
                        }
                    });
                });


                var itemIp = multiRule._itemIp ? item[multiRule._itemIp] : null;
                if (itemIp != null) {
                     var $itemIp = $('<span>').html(_s(itemIp));
                     $tr.append($('<td>').addClass('state').appendTo($tr).append($itemIp));
                }


                var itemState = item._itemState ? item._itemState : item.state;

                $tr.append($('<td>').addClass('state').appendTo($tr).append(
                    $('<span>').text(
                        item._itemStateLabel ? _l(item._itemStateLabel) + ' - ' + itemState :
                            _l('label.state') + ' - ' + itemState
                    )
                ));


                if (itemActions) {
                    var $itemActions = $('<td>').addClass('actions item-actions');

                    $.each(itemActions, function(itemActionID, itemAction) {
                        if (itemActionID == 'add')
                            return true;

                        if (item._hideActions != null && $.inArray(itemActionID, item._hideActions) > -1)
                            return true;

                        var $itemAction = $('<div>').addClass('action').addClass(itemActionID);

                        $itemAction.click(function() {
                            itemAction.action({
                                item: item,
                                multiRule: multiRule,
                                response: {
                                    success: function(args) {
                                        if (itemActionID == 'destroy') {
                                            var notification = args.notification;
                                            var success = function(args) {
                                                $tr.remove();
                                            };
                                            var successArgs = {};
                                            var error = function(args) {
                                                $tr.show();
                                                cloudStack.evenOdd($tbody, 'tr:visible', {
                                                    even: function($elem) {
                                                        $elem.removeClass('odd');
                                                        $elem.addClass('even');
                                                    },
                                                    odd: function($elem) {
                                                        $elem.removeClass('even');
                                                        $elem.addClass('odd');
                                                    }
                                                });
                                            };
                                            var errorArgs = {};

                                            $tr.hide();
                                            cloudStack.evenOdd($tbody, 'tr:visible', {
                                                even: function($elem) {
                                                    $elem.removeClass('odd');
                                                    $elem.addClass('even');
                                                },
                                                odd: function($elem) {
                                                    $elem.removeClass('even');
                                                    $elem.addClass('odd');
                                                }
                                            });
                                            cloudStack.ui.notifications.add(notification,
                                                success, successArgs,
                                                error, errorArgs);
                                        }
                                    },
                                    error: function(message) {
                                        if (message) {
                                            cloudStack.dialog.notice({
                                                message: message
                                            });
                                        }
                                    }
                                }
                            });
                        });
                        $itemAction.append($('<span>').addClass('icon'));
                        $itemAction.appendTo($itemActions);

                        return true;
                    });

                    $itemActions.appendTo($tr);
                }

                return $tr;
            },

            expandable: function(data, itemActions, multiRule) {
                var $expandable = $('<div>').addClass('expandable-listing');
                var $tbody = $('<tbody>').appendTo($('<table>').appendTo($expandable));

                $(data).each(function() {
                    var field = this;
                    var $tr = _medit.multiItem.itemRow(field, itemActions, multiRule, $tbody).appendTo($tbody);

                    $tr.data('json-obj', field);

                    cloudStack.evenOdd($tbody, 'tr', {
                        even: function($elem) {
                            $elem.addClass('even');
                        },
                        odd: function($elem) {
                            $elem.addClass('odd');
                        }
                    });
                });

                return $expandable.hide();
            }
        }
    };

    $.fn.multiEdit = function(args) {
        var dataProvider = args.dataProvider;
        var multipleAdd = args.multipleAdd;
        var tags = args.tags;
        var $multi = $('<div>').addClass('multi-edit').appendTo(this);
        var $multiForm = $('<form>').appendTo($multi);
        var $inputTable = $('<table>').addClass('multi-edit').appendTo($multiForm);
        var $dataTable = $('<div>').addClass('data').appendTo($multi);
        var $addVM;
        var fields = args.fields;
        var actions = args.actions;
        var itemActions = multipleAdd ? args.itemActions : null;
        var noSelect = args.noSelect;
        var context = args.context;
        var ignoreEmptyFields = args.ignoreEmptyFields;
        var actionPreFilter = args.actionPreFilter;
        var readOnlyCheck = args.readOnlyCheck;
        var reorder = args.reorder;

        var $thead = $('<tr>').appendTo(
            $('<thead>').appendTo($inputTable)
        );
        var $inputForm = $('<tr>').appendTo(
            $('<tbody>').appendTo($inputTable)
        );
        var $dataBody = $('<div>').addClass('data-body').appendTo($dataTable);

        // Setup input table headers

        if (reorder) {
            $('<th>').addClass('reorder').appendTo($thead);
            $('<td>').addClass('reorder').appendTo($inputForm);
            $multi.find('.data-body').sortable({
                handle: '.action.moveDrag',

                update: function(event, ui) {
                    var $loading = $('<div>').addClass('loading-overlay');

                    $loading.prependTo($multi);
                    reorder.moveDrag.action({
                        targetIndex: ui.item.index(),
                        nextItem: ui.item.next().size() ? ui.item.next().data('json-obj') : null,
                        prevItem: ui.item.prev().size() ? ui.item.prev().data('json-obj') : null,
                        context: $.extend(true, {}, context, {
                            // Passes all rules, so that each index can be updated
                            multiRule: [ui.item.data('json-obj')]
                        }),
                        response: {
                            success: function(args) {
                                $multi.trigger('refresh');
                                $loading.remove();
                            },
                            error: function(msg) {
                                $multi.trigger('refresh');
                                cloudStack.dialog.notice(msg);
                                $loading.remove();
                            }
                        }
                    });
                }
            });
        }

        $.each(args.fields, function(fieldName, field) {
            if (!field) return true;

            var $th = $('<th>').addClass(fieldName).html(_l(field.label.toString()));
            $th.attr('rel', fieldName);
            $th.appendTo($thead);
            var $td = $('<td>').addClass(fieldName);
            $td.attr('rel', fieldName);
            $td.appendTo($inputForm);

            var isHidden = $.isFunction(field.isHidden) ?
                    field.isHidden({ context: context }) : field.isHidden;

            if (isHidden) {
                // return true == hide only header and form column
                // return 2 == hide header and form, as well as returned item column
                if (isHidden === 2) {
                    $th.addClass('always-hide');
                }

                $th.hide();
                $td.hide();
            }

            if (field.select) {
                var $select = $('<select>');

                $select.attr({
                    name: fieldName
                });
                $select.appendTo($td);
                field.select({
                    context: context,
                    $select: $select,
                    $form: $multiForm,
                    response: {
                        success: function(args) {
                            $(args.data).each(function() {
                                $('<option>').val(this.name).html(_l(_s(this.description)))
                                    .appendTo($select);
                            });
                            _medit.refreshItemWidths($multi);
                        },

                        error: function(args) {}
                    }
                });
            } else if (field.edit && field.edit != 'ignore') {
                if (field.range) {
                    var $range = $('<div>').addClass('range').appendTo($td);

                    $(field.range).each(function() { //e.g. field.range = ['privateport', 'privateendport'];
                        var $input = $('<input>')
                            .addClass('disallowSpecialCharacters')
                            .attr({
                                name: this,
                                type: 'text'
                            })
                        //.addClass(!field.isOptional ? 'required' : null)          //field.range[0] might be required while field.range[1] is optional (e.g. private start port is required while private end port is optional), so "isOptional" property should be on field.range level instead of field level.
                        //.attr('disabled', field.isDisabled ? 'disabled' : false)  //field.range[0] might be enabled while field.range[1] is disabled  (e.g. private start port is enabled while private end port is disabled),  so "isDisabled" property should be on field.range level instead of field level.
                        .appendTo(
                            $('<div>').addClass('range-item').appendTo($range)
                        );

                        if (field.isDisabled) $input.hide();

                        if (field.defaultValue) {
                            $input.val(field.defaultValue);
                            $input.data('multi-default-value', field.defaultValue);
                        }
                    });
                } else {
                    var $input = $('<input>')
                        .attr({
                            name: fieldName,
                            type: field.isPassword ? 'password' : 'text'
                        })
                            .addClass(!field.isOptional ? 'required' : null)
                            .addClass('disallowSpecialCharacters')
                        .attr('disabled', field.isDisabled ? 'disabled' : false)
                        .appendTo($td);

                    if (field.validation) {
                        $td.find('input').first().data("validation-settings",  field.validation );
                    }

                    if (field.isDisabled) $input.hide();
                    if (field.defaultValue) {
                        $input.val(field.defaultValue);
                        $input.data('multi-default-value', field.defaultValue);
                    }
                }
            } else if (field.custom) {
                $('<div>').addClass('button add-vm custom-action')
                    .html(_l(field.custom.buttonLabel))
                    .click(function() {
                        if (field.custom.requireValidation && !$multiForm.valid()) return false;

                        var formData = getMultiData($multi);

                        field.custom.action({
                            formData: formData,
                            context: context,
                            data: $td.data('multi-custom-data'),
                            response: {
                                success: function(args) {
                                    $td.data('multi-custom-data', args.data);
                                }
                            }
                        });

                        return false;
                    }).appendTo($td);
            } else if (field.addButton) {
                $addVM = $('<div>').addClass('button add-vm').html(
                    _l('label.add')
                ).appendTo($td);
            }

            if (field.desc) $input.attr('title', field.desc);
        });

        // Setup header fields
        var showHeaderFields = args.headerFields ? true : false;
        var headerForm = showHeaderFields ? cloudStack.dialog.createForm({
            context: context,
            noDialog: true,
            form: {
                fields: args.headerFields
            },
            after: function(args) {
                // Form fields are handled by main 'add' action
            }
        }) : null;
        var $headerFields = $('<div>').addClass('header-fields').hide(); //make headerFields hidden as default

        if (headerForm) {
            $headerFields.append(headerForm.$formContainer)
                .prependTo($multi);
        }

        if (args.actions && !args.noHeaderActionsColumn) {
            $thead.append($('<th></th>').html(_l('label.actions')).addClass('multi-actions'));
            $inputForm.append($('<td></td>').addClass('multi-actions'));
        }

        $addVM.bind('click', function() {
            // Validate form first
            if (!$multiForm.valid()) {
                if ($multiForm.find('input.error:visible').size()) {
                    return false;
                }
            }

            var $dataList;
            var addItem = function(itemData) {
                var data = {};

                $.each(getMultiData($multi), function(key, value) {
                    if (value != '') {
                        data[key] = value;
                    }
                });

                // Append custom data
                var $customFields = $multi.find('tbody td').filter(function() {
                    return $(this).data('multi-custom-data');
                });

                $customFields.each(function() {
                    var $field = $(this);
                    var fieldID = $field.attr('rel');
                    var fieldData = $field.data('multi-custom-data');

                    data[fieldID] = fieldData;
                });

                // Loading appearance
                var $loading = _medit.loadingItem($multi, _l('label.adding') + '...');
                $dataBody.prepend($loading);

                // Clear out fields
                $multi.find('input').each(function() {
                    var $input = $(this);

                    if ($input.data('multi-default-value')) {
                        $input.val($input.data('multi-default-value'));
                    } else {
                        $input.val('');
                    }
                });
                $multi.find('tbody td').each(function() {
                    var $item = $(this);

                    if ($item.data('multi-custom-data')) {
                        $item.data('multi-custom-data', null);
                    }
                });

                // Apply action
                args.add.action({
                    context: context,
                    data: data,
                    itemData: itemData,
                    $multi: $multi,
                    response: {
                        success: function(successArgs) {
                            var notification = successArgs ? successArgs.notification : null;
                            if (notification) {
                                $('.notifications').notifications('add', {
                                    section: 'network',
                                    desc: notification.label,
                                    interval: 3000,
                                    _custom: successArgs._custom,
                                    poll: function(pollArgs) {
                                        var complete = pollArgs.complete;
                                        var error = pollArgs.error;

                                        notification.poll({
                                            _custom: pollArgs._custom,
                                            complete: function(completeArgs) {
                                                complete(args);
                                                $loading.remove();
                                                getData();
                                            },

                                            error: function(args) {
                                                error(args);
                                                $loading.remove();

                                                return cloudStack.dialog.error(args);
                                            }
                                        });
                                    }
                                });
                            } else {
                                $loading.remove();
                                getData();
                            }
                        },

                        error: cloudStack.dialog.error(function() {
                            $loading.remove();
                        })
                    }
                });
            };

            if (args.noSelect) {
                // Don't append instance data
                addItem([]);

                return true;
            }

            _medit.vmList($multi,
                args.listView,
                args.context,
                multipleAdd, _l('label.add.vms'),
                addItem);

            return true;
        });

        var listView = args.listView;
        var getData = function() {
            dataProvider({
                context: context,
                $multi: $multi,
                response: {
                    success: function(args) {
                        $multi.find('.data-item').remove();
                        $(args.data).each(function() {
                            var data = this;
                            var itemData = this._itemData;

                            _medit.addItem(
                                data,
                                fields,
                                $multi,
                                itemData,
                                actions, {
                                    multipleAdd: multipleAdd,
                                    itemActions: itemActions,
                                    noSelect: noSelect,
                                    context: $.extend(true, {}, context, this._context),
                                    ignoreEmptyFields: ignoreEmptyFields,
                                    preFilter: actionPreFilter,
                                    listView: listView,
                                    tags: tags,
                                    reorder: reorder
                                }
                            ).appendTo($dataBody);
                        });

                        if (readOnlyCheck && !readOnlyCheck(args)) {
                            $multi.find('th.add-user, td.add-user').detach();
                            $multiForm.find('tbody').detach();
                        }
                        if (args.hideFields) {
                            $(args.hideFields).each(function() {
                                $multi.find('th.' + this + ',td.' + this).hide();
                            });
                        }

                        _medit.refreshItemWidths($multi);
                    },
                    error: cloudStack.dialog.error
                }
            });
        };

        if (args.hideForm && args.hideForm()) {
            $multiForm.find('tbody').detach();
        }

        // Get existing data
        setTimeout(function() {
            getData();
        });

        var fullRefreshEvent = function(event) {
            if ($multi.is(':visible')) {
                getData();
            } else {
                $(window).unbind('cloudStack.fullRefresh', fullRefreshEvent);
            }
        };
        $(window).bind('cloudStack.fullRefresh', fullRefreshEvent);
        $multi.bind('refresh', fullRefreshEvent);

        $multi.bind('change select', function() {
            _medit.refreshItemWidths($multi);
        });

        $multiForm.validate();

        var inputs = $multiForm.find('input');
        $.each(inputs, function() {
            if ($(this).data && $(this).data('validation-settings'))
                $(this).rules('add', $(this).data('validation-settings'));
        });
        return this;
    };

})(jQuery, cloudStack);
