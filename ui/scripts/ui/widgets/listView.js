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
/**
 * Create dynamic list view based on data callbacks
 */
(function($, cloudStack, _l, _s) {
    var uiActions = {
        standard: function($instanceRow, args, additional) {
            var isAddAction = args.action.isAdd;

            var listViewArgs = $instanceRow.closest('div.list-view').data('view-args');
            var notification = args.action.notification ? args.action.notification : {};
            var messages = args.action ? args.action.messages : {};
            var preAction = args.action ? args.action.preAction : {};
            var action = args.action ? args.action.action : {};
            var multiSelect = args.action.isMultiSelectAction;
            var needsRefresh = args.action.needsRefresh;
            var section;
            var data;
            var messageArgs;
            if (multiSelect) {
                data = {
                    id: $.map($instanceRow, function(elem) { return $(elem).data('list-view-item-id'); }),
                    jsonObj: $.map($instanceRow, function(elem) { return $(elem).data('jsonObj'); })
                };
                messageArgs = {
                    name: $.map($instanceRow, function(elem) { return $(elem).find('td.name span').html() })
                };
            } else {
                data = {
                    id: $instanceRow.data('list-view-item-id'),
                    jsonObj: $instanceRow.data('jsonObj')
                };
                messageArgs = {
                    name: $instanceRow.find('td.name span').html()
                };
            }
            var $listView = $instanceRow.closest('.list-view');

            if (args.data) $.extend(true, data, args.data);
            if (listViewArgs) section = listViewArgs.section;

            notification.desc = messages ?
                messages.notification(messageArgs) : null;

            if (listViewArgs)
                notification.section = listViewArgs.id;

            // Handle pre-action (occurs before any other behavior happens)
            if (preAction) {
                var preActionContext = $.extend(true, {}, listViewArgs.context);

                preActionContext[
                    listViewArgs.activeSection
                ] = (multiSelect ? data.jsonObj : [data.jsonObj]);

                if (!preAction({
                    context: preActionContext
                })) return false;
            }

            var performAction = function(data, options) {
                if (!options) options = {};

                var $form = options.$form;
                var isHeader = options.isHeader;

                $instanceRow = options.$item ? options.$item : $instanceRow;
                var $item = options.$item;
                var context = $.extend(true, {}, listViewArgs.context);
                context[
                    listViewArgs.activeSection
                ] = (multiSelect ? $.map($instanceRow, function(elem) { return $(elem).data('jsonObj'); }) : [$instanceRow.data('jsonObj')]);

                // Make sure the master checkbox is unselected
                if (multiSelect) {
                    $instanceRow.closest('.list-view').find('input.multiSelectMasterCheckbox').attr('checked', false);
                }

                var externalLinkAction = action.externalLink;
                if (externalLinkAction) {
                    // Show popup immediately, do not proceed through normal action process
                    window.open(
                        // URL
                        externalLinkAction.url({
                            context: context
                        }),

                        // Title
                        externalLinkAction.title({
                            context: context
                        }),

                        // Window options
                        'menubar=0,resizable=0,' + 'width=' + externalLinkAction.width + ',' + 'height=' + externalLinkAction.height
                    );
                } else if (action.custom && !action.noAdd) {
                    action.custom({
                        data: data,
                        ref: options.ref,
                        context: context,
                        $instanceRow: $instanceRow,
                        complete: function(args) {
                            args = args ? args : {};

                            var $item = args.$item;

                            notification.desc = messages.notification(args.messageArgs);
                            notification._custom = args._custom;

                            cloudStack.ui.notifications.add(
                                notification,
                                function(args) {
                                    if (listViewArgs.onActionComplete) {
                                        listViewArgs.onActionComplete();
                                    }

                                    if ($item.is(':visible') && !isHeader) {
                                        replaceItem(
                                            $item,
                                            args.data,
                                            args.actionFilter ?
                                            args.actionFilter : $instanceRow.next().data('list-view-action-filter')
                                        );
                                    }
                                },

                                {},

                                // Error

                                function(args) {
                                    if (args && args.updatedData) {
                                        if ($item.is(':visible') && !isHeader) {
                                            replaceItem(
                                                $item,
                                                args.updatedData,
                                                args.actionFilter
                                            );
                                        }
                                    } else {
                                        $item.remove();
                                    }
                                }
                            );
                        }
                    });
                } else if (action.uiCustom) {
                    action.uiCustom({
                        $item: $instanceRow
                    });
                } else {
                    if (needsRefresh) {
                        var $loading = $('<div>').addClass('loading-overlay');

                        $listView.prepend($loading);
                    }

                    var actionArgs = {
                        data: data,
                        ref: options.ref,
                        context: options.context,
                        $form: $form,
                        response: {
                            success: function(args) {
                                args = args ? args : {};

                                var $prevRow, $newRow;

                                // Make copy of previous row, in case data is needed
                                $prevRow = $instanceRow.clone();
                                if (multiSelect) {
                                    $prevRow.find('.quick-view').addClass('loading-overlay');
                                    $.each($prevRow, function(index, elem) {
                                        $(elem).data($($instanceRow[index]).data());
                                    });
                                } else {
                                    $prevRow.data($instanceRow.data());
                                }

                                // Set loading appearance
                                if (args.data && (!isHeader || multiSelect)) {
                                    if (multiSelect) {
                                        $instanceRow = $.map($instanceRow, function(elem, index) {
                                            return replaceItem(
                                                $(elem),
                                                $.extend($(elem).data('json-obj'), args.data[index]),
                                                $(elem).data('list-view-action-filter')
                                            )[0];
                                        });
                                    } else {
                                        $instanceRow = replaceItem(
                                            $instanceRow,
                                            $.extend($instanceRow.data('json-obj'), args.data),
                                            $instanceRow.data('list-view-action-filter')
                                        );
                                    }
                                }

                                if (multiSelect) {
                                    $.each($instanceRow, function(index, elem) {
                                        $(elem).find('td:last').children().remove();
                                        $(elem).find('td:last').append($('<div>').addClass('loading'));
                                        $(elem).addClass('loading');

                                        if (options.$item) $(elem).data('list-view-new-item', true);

                                        // Disable any clicking/actions for row
                                        $(elem).bind('click', function() {
                                            return false;
                                        });
                                    });
                                } else {
                                    $instanceRow.find('td:last').children().remove();
                                    $instanceRow.find('td:last').append($('<div>').addClass('loading'));
                                    $instanceRow.addClass('loading');

                                    if (options.$item) $instanceRow.data('list-view-new-item', true);

                                    // Disable any clicking/actions for row
                                    $instanceRow.bind('click', function() {
                                        return false;
                                    });
                                }

                                if(args.notification) notification = args.notification;

                                notification._custom = args._custom;

                                if (additional && additional.success) additional.success(args);

                                if (listViewArgs.onActionComplete == true) {
                                    listViewArgs.onActionComplete();
                                }

                                cloudStack.ui.notifications.add(
                                    notification,

                                    // Success

                                    function(args) {
                                        if (!args) args = {};

                                        var actionFilter = args.actionFilter ?
                                            args.actionFilter : (multiSelect ?
                                                $.map($instanceRow, function(elem) { $(elem).data('list-view-action-filter') }) :
                                                $instanceRow.data('list-view-action-filter'));

                                        if (!isHeader || multiSelect) {
                                            var visible = (multiSelect ? $($instanceRow[0]).is(':visible') : $instanceRow.is(':visible'));
                                            if (visible) {
                                                if (args.data) {
                                                    if (multiSelect) {
                                                        $newRow = [];
                                                        $.each($instanceRow, function(index, elem) {
                                                            $newRow.push(
                                                                replaceItem($(elem),
                                                                    args.data, //$.extend($(elem).data('json-obj'), args.data[index]),
                                                                    actionFilter)
                                                            );
                                                        });
                                                    } else {
                                                        $newRow = replaceItem($instanceRow,
                                                            args.data,
                                                            actionFilter);
                                                    }
                                                } else {
                                                    // Nothing new, so just put in existing data
                                                    if (multiSelect) {
                                                        $instanceRow = $.map($instanceRow, function(elem) {
                                                            replaceItem($(elem),
                                                                $(elem).data('json-obj'),
                                                                actionFilter)[0]
                                                        });
                                                    } else {
                                                        $newRow = replaceItem($instanceRow,
                                                            $instanceRow.data('json-obj'),
                                                            actionFilter);
                                                    }
                                                }

                                                if (needsRefresh) {
                                                    if ($listView.closest('.detail-view').size()) {
                                                        $('.detail-view:last .button.refresh').click();
                                                    } else {
                                                        $loading.remove();
                                                        $listView.listView('refresh');
                                                    }
                                                }
                                            }

                                            if (additional && additional.complete)
                                                additional.complete(args, $newRow);
                                        }

                                        if (messages.complete) {
                                            cloudStack.dialog.notice({
                                                message: messages.complete(args.data)
                                            });
                                        }

                                        if (options.complete) {
                                            options.complete(args);
                                        }

                                        if (listViewArgs.onActionComplete) {
                                            listViewArgs.onActionComplete();
                                        }
                                    },

                                    {},

                                    // Error

                                    function(errorArgs) {
                                        if (!isHeader) {
                                            if (isAddAction == true && $instanceRow.data('list-view-new-item')) {
                                                // For create forms
                                                $instanceRow.remove();
                                            } else {
                                                // For standard actions
                                                if(!args.notification) {
                                                    if (multiSelect) {
                                                        $.each($instanceRow, function(index, elem) {
                                                            replaceItem(
                                                                $(elem),
                                                                $.extend($(elem).data('json-obj'), errorArgs.data),
                                                                errorArgs.actionFilter ?
                                                                errorArgs.actionFilter :
                                                                $(elem).data('list-view-action-filter')
                                                            );
                                                        });
                                                    } else {
                                                        replaceItem(
                                                            $instanceRow,
                                                            $.extend($instanceRow.data('json-obj'), errorArgs.data),
                                                            errorArgs.actionFilter ?
                                                            errorArgs.actionFilter :
                                                            $instanceRow.data('list-view-action-filter')
                                                        );
                                                    }
                                                }
                                            }
                                        }

                                        if (options.error) {
                                            options.error(errorArgs);
                                        }
                                    }
                                );
                            },
                            error: function(message) {
                                $instanceRow.removeClass('loading');
                                $instanceRow.find('td.quick-view').removeClass('loading-overlay');

                                if (!isHeader) {
                                    if (($.isPlainObject(args.action.createForm) && args.action.addRow != 'false') ||
                                        (!args.action.createForm && args.action.addRow == 'true')) {
                                        $instanceRow.remove();
                                    }
                                }

                                if (needsRefresh) {
                                    if (!$listView.closest('.detail-view').size()) {
                                        $loading.remove();
                                    }
                                }

                                if (options.error) options.error(message);

                                if (message) cloudStack.dialog.notice({
                                    message: message
                                });
                            }
                        }
                    };

                    if (action.custom && action.noAdd) {
                        action.custom({
                            data: data,
                            ref: options.ref,
                            context: context,
                            $instanceRow: $instanceRow,
                            complete: actionArgs.response.success
                        });
                    } else {
                        action(actionArgs);
                    }
                }
            };

            var context = $.extend({}, listViewArgs.context);

            context[
                listViewArgs.activeSection
            ] = (multiSelect ? $.map($instanceRow, function(elem) { return $(elem).data('jsonObj'); }) : [$instanceRow.data('jsonObj')]);

            messageArgs.context = context;

            if (!args.action.action.externalLink && !args.action.createForm &&
                args.action.addRow != 'true' && !action.custom && !action.uiCustom && !args.action.listView) {
                cloudStack.dialog.confirm({
                    message: messages.confirm(messageArgs),
                    action: function() {
                        performAction(data, {
                            context: context,
                            isMultiSelectAction: multiSelect,
                            isHeader: args.action.isHeader
                        });
                    }
                });
            } else if (action.custom || action.uiCustom) {
                performAction();
            } else if (args.action.listView) {
                cloudStack.dialog.listView({
                    context: context,
                    listView: args.action.listView,
                    after: function(args) {
                        performAction(null, {
                            context: args.context
                        });
                    }
                });
            } else {
                var addRow = args.action.addRow == "false" ? false : true;
                var isHeader = args.action.isHeader;
                var createFormContext = $.extend({}, context);

                var externalLinkAction = action.externalLink;
                if (externalLinkAction) {
                    // Show popup immediately, do not proceed through normal action process
                    window.open(
                        // URL
                        externalLinkAction.url({
                            context: context
                        }),

                        // Title
                        externalLinkAction.title({
                            context: context
                        }),

                        // Window options
                        'menubar=0,resizable=0,' + 'width=' + externalLinkAction.width + ',' + 'height=' + externalLinkAction.height
                    );
                } else if (args.action.createForm) {
                    cloudStack.dialog.createForm({
                        form: args.action.createForm,
                        after: function(args) {
                            var $newItem;

                            if (!isHeader) {
                                if (addRow != false) {
                                    $newItem = $listView.listView('prependItem', {
                                        data: [
                                            $.extend(args.data, {
                                                state: 'Creating',
                                                status: 'state.Creating',
                                                allocationstate: 'Creating'
                                            })
                                        ]
                                    });
                                } else {
                                    $newItem = $instanceRow;
                                }

                                performAction(args.data, {
                                    ref: args.ref,
                                    context: createFormContext,
                                    $item: $newItem,
                                    $form: args.$form
                                });
                            } else {
                                var $loading = $('<div>').addClass('loading-overlay');

                                $loading.appendTo($listView);
                                performAction(args.data, {
                                    ref: args.ref,
                                    context: createFormContext,
                                    $form: args.$form,
                                    isHeader: isHeader,
                                    complete: function(args) {
                                        $loading.remove();
                                        $listView.listView('refresh');
                                    },
                                    error: function(args) {
                                        $loading.remove();
                                    }
                                });
                            }
                        },
                        ref: listViewArgs.ref,
                        context: createFormContext
                    });
                } else {
                    cloudStack.dialog.confirm({
                        message: messages.confirm(messageArgs),
                        action: function() {
                            var $newItem;
                            if (addRow && !action.isHeader) {
                                $newItem = $listView.listView('prependItem', {
                                    data: [
                                        $.extend(args.data, {
                                            state: 'Creating',
                                            status: 'state.Creating',
                                            allocationstate: 'Creating'
                                        })
                                    ]
                                });
                            } else if (action.isHeader) {
                                $newItem = $('<div>');
                            } else {
                                $newItem = $instanceRow;
                            }

                            performAction(args.data, {
                                ref: args.ref,
                                context: createFormContext,
                                $item: $newItem,
                                $form: args.$form
                            });
                        }
                    });
                }
            }
        },

        remove: function($instanceRow, args) {
            uiActions.standard($instanceRow, args, {
                complete: function(args, $newRow) {
                    $newRow.remove();
                }
            });
        },

        edit: function($instanceRow, args) {
            var $td = $instanceRow.find('td.editable');
            var $edit = $td.find('div.edit');
            var $editInput = $edit.find('input');
            var $label = $td.find('span');
            var $listView = $instanceRow.closest('.list-view');
            var listViewArgs = $listView.data('view-args');

            // Hide label, show edit field
            var showEditField = function() {
                $edit.css({
                    opacity: 1
                });
                $label.fadeOut('fast', function() {
                    $edit.fadeIn();
                    $editInput.focus();
                    $instanceRow.closest('div.data-table').dataTable('refresh');
                });
            };

            // Hide edit field, validate and save changes
            var showLabel = function(val, options) {
                if (!options) options = {};

                var oldVal = $label.html();

                if (val != null)
                    $label.html(_s(val));

                var data = {
                    id: $instanceRow.data('list-view-item-id'),
                    jsonObj: $instanceRow.data('jsonObj')
                };

                data[$td.data('list-view-item-field')] = $editInput.val();

                var context = $.extend({}, listViewArgs.context);
                context[
                    listViewArgs.activeSection
                ] = $instanceRow.data('jsonObj');

                args.callback({
                    data: data,
                    context: context,
                    response: {
                        success: function(args) {
                            $edit.hide();
                            $label.fadeIn();
                            $instanceRow.closest('div.data-table').dataTable('refresh');

                            if (options.success) options.success(args);
                        },
                        error: function(message) {
                            if (message) {
                                cloudStack.dialog.notice({
                                    message: message
                                });
                                $edit.hide(),
                                $label.html(_s(oldVal)).fadeIn();
                                $instanceRow.closest('div.data-table').dataTable('refresh');

                                if (options.error) options.error(args);
                            }
                        }
                    }
                });
            };

            if (args.cancel) { //click Cancel button
                // showLabel();
                var oldVal = $label.html();
                $edit.hide();
                $label.fadeIn();
                $instanceRow.closest('div.data-table').dataTable('refresh');
                $editInput.val(_s(oldVal));
                return false;
            }

            if (!$editInput.is(':visible') || !(typeof(args.action) == 'undefined')) { //click Edit button
                showEditField();
            } else if ($editInput.val() != $label.html()) { //click Save button with changed value
                if ($editInput.val().match(/<|>/)) {
                    cloudStack.dialog.notice({ message: 'message.validate.invalid.characters' });
                    return false;
                }

                $edit.animate({
                    opacity: 0.5
                });

                var originalName = $label.html();
                var newName = $editInput.val();
                showLabel(newName, {
                    success: function() {
                        cloudStack.ui.notifications.add({
                                section: $instanceRow.closest('div.view').data('view-args').id,
                                desc: newName ? _l('Set value of') + ' ' + $instanceRow.find('td.name span').html() + ' ' + _l('to') + ' ' + _s(newName) : _l('Unset value for') + ' ' + $instanceRow.find('td.name span').html()
                            },
                            function(args) {}, [{
                                name: newName
                            }]
                        );
                    }
                });
            } else { //click Save button with unchanged value
                showLabel();
            }

            return $instanceRow;
        }
    };

    var rowActions = {
        _std: function($tr, action) {
            action();

            $tr.closest('.data-table').dataTable('refresh');

            setTimeout(function() {
                $tr.closest('.data-table').dataTable('selectRow', $tr.index());
            }, 0);
        },

        moveTop: function($tr) {
            rowActions._std($tr, function() {
                $tr.closest('tbody').prepend($tr);
                $tr.closest('.list-view').animate({
                    scrollTop: 0
                });
            });
        },

        moveBottom: function($tr) {
            rowActions._std($tr, function() {
                $tr.closest('tbody').append($tr);
                $tr.closest('.list-view').animate({
                    scrollTop: 0
                });
            });
        },

        moveUp: function($tr) {
            rowActions._std($tr, function() {
                $tr.prev().before($tr);
            });
        },

        moveDown: function($tr) {
            rowActions._std($tr, function() {
                $tr.next().after($tr);
            });
        },

        moveTo: function($tr, index, after) {
            rowActions._std($tr, function() {
                var $target = $tr.closest('tbody').find('tr').filter(function() {
                    return $(this).index() == index;
                });

                //    if ($target.index() > $tr.index()) $target.after($tr);
                //  else $target.before($tr);

                $tr.closest('.list-view').scrollTop($tr.position().top - $tr.height() * 2);

                if (after)
                    setTimeout(function() {
                        after();
                    });
            });
        }
    };

    /**
     * Edit field text
     *
     * @param $td {jQuery} <td> to put input field into
     */
    var createEditField = function($td) {
        $td.addClass('editable');

        // Put <td> label into a span
        var sanitizedValue = $td.html();
        $('<span></span>').html(sanitizedValue).appendTo($td.html(''));

        var $editArea = $('<div></div>').addClass('edit');
        var $editField = $('<input />').addClass('edit').attr({
            type: 'text',
            value: cloudStack.sanitizeReverse(sanitizedValue)
        });
        var $actionButton = $('<div></div>').addClass('action');
        var $saveButton = $actionButton.clone().addClass('save').attr({
            'title': _l('Save')
        });
        var $cancelButton = $actionButton.clone().addClass('cancel').attr({
            'title': _l('Cancel edit')
        });

        $([$editField, $saveButton, $cancelButton]).each(function() {
            this.appendTo($editArea);
        });

        return $editArea.hide();
    };

    var renderActionCol = function(actions) {
        return $.grep(
            $.map(actions, function(value, key) {
                return key;
            }),
            function(elem) {
                return elem != 'add';
            }
        ).length;
    };

    var createHeader = function(preFilter, fields, $table, actions, options) {
        if (!options) options = {};

        var $tr = $('<tr>');
        var $thead = $('<thead>').prependTo($table).append($tr);
        var reorder = options.reorder;
        var detailView = options.detailView;
        var multiSelect = options.multiSelect;
        var groupableColumns = options.groupableColumns;
        var viewArgs = $table.closest('.list-view').data('view-args');
        var uiCustom = viewArgs.uiCustom;
        var hiddenFields = [];

        if (preFilter != null)
            hiddenFields = preFilter();

        var addColumnToTr = function($tr, key, colspan, label, needsCollapsibleColumn) {
            var trText = _l(label);
            var $th = $('<th>').addClass(key).attr('colspan', colspan).appendTo($tr);
            if ($th.index()) $th.addClass('reduced-hide');
            $th.css({'border-right': '1px solid #C6C3C3', 'border-left': '1px solid #C6C3C3'});
            if (needsCollapsibleColumn) {
                var karetLeft = $('<span>').css({'margin-right': '10px'});
                karetLeft.attr('title', trText);
                karetLeft.appendTo($th);
                $('<span>').html('&laquo').css({'font-size': '15px', 'float': 'right'}).appendTo(karetLeft);
                $('<span>').html(trText).appendTo(karetLeft);

                $th.click(function(event) {
                    event.stopPropagation();
                    var $th = $(this);
                    var startIndex = 0;
                    $th.prevAll('th').each(function() {
                        startIndex += parseInt($(this).attr('colspan'));
                    });
                    var endIndex = startIndex + parseInt($th.attr('colspan'));
                    // Hide Column group
                    $th.hide();
                    $th.closest('table').find('tbody td').filter(function() {
                        return $(this).index() >= startIndex && $(this).index() < endIndex;
                    }).hide();
                    $th.closest('table').find('thead tr:last th').filter(function() {
                        return $(this).index() >= startIndex && $(this).index() < endIndex;
                    }).hide();
                    // Show collapsible column with blank cells
                    $th.next('th').show();
                    $th.closest('table').find('tbody td').filter(function() {
                        return $(this).index() == endIndex;
                    }).show();
                    $th.closest('table').find('thead tr:last th').filter(function() {
                        return $(this).index() == endIndex;
                    }).show();
                    // Refresh list view
                    $tr.closest('.list-view').find('.no-split').dataTable('refresh');
                });

                var karetRight = addColumnToTr($tr, 'collapsible-column', 1, '');
                $('<span>').html(trText.substring(0,3)).appendTo(karetRight);
                $('<span>').css({'font-size': '15px'}).html(' &raquo').appendTo(karetRight);
                karetRight.attr('title', trText);
                karetRight.css({'border-right': '1px solid #C6C3C3', 'border-left': '1px solid #C6C3C3', 'min-width': '10px', 'width': '10px', 'max-width': '45px', 'padding': '2px'});
                karetRight.hide();
                karetRight.click(function(event) {
                    event.stopPropagation();
                    var prevTh = $(this).prev('th');
                    var startIndex = 0;
                    prevTh.prevAll('th').each(function() {
                        startIndex += parseInt($(this).attr('colspan'));
                    });
                    var endIndex = startIndex + parseInt(prevTh.attr('colspan'));

                    prevTh.show();
                    prevTh.closest('table').find('tbody td').filter(function() {
                        return $(this).index() >= startIndex && $(this).index() < endIndex;
                    }).show();
                    prevTh.closest('table').find('thead tr:last th').filter(function() {
                        return $(this).index() >= startIndex && $(this).index() < endIndex;
                    }).show();

                    prevTh.next('th').hide();
                    prevTh.closest('table').find('tbody td').filter(function() {
                        return $(this).index() == endIndex;
                    }).hide();
                    prevTh.closest('table').find('thead tr:last th').filter(function() {
                        return $(this).index() == endIndex;
                    }).hide();

                    $tr.closest('.list-view').find('.no-split').dataTable('refresh');
                });
            } else {
                $th.html(trText);
            }
            return $th;
        };

        if (groupableColumns) {
            $tr.addClass('groupable-header-columns').addClass('groupable-header');
            $.each(fields, function(key) {
                var field = this;
                if (field.columns) {
                    var colspan = Object.keys(field.columns).length;
                    addColumnToTr($tr, key, colspan, field.label, true);
                } else {
                    var label = '';
                    if (key == 'name') {
                        label = 'label.resources';
                    }
                    addColumnToTr($tr, key, 1, label);
                }
                return true;
            });
            if (detailView && !$.isFunction(detailView) && !detailView.noCompact && !uiCustom) {
                addColumnToTr($tr, 'quick-view', 1, '');
            }
            $tr = $('<tr>').appendTo($thead);
            $tr.addClass('groupable-header');
        }

        if (multiSelect) {
            var $th = $('<th>').addClass('multiselect').appendTo($tr);
            var content = $('<input>')
                .attr('type', 'checkbox')
                .addClass('multiSelectMasterCheckbox')
                .appendTo($th);

            content.click(function() {
                var checked = $(this).is(':checked');
                $('.multiSelectCheckbox').attr('checked', checked);
                toggleMultiSelectActions($table.closest('.list-view'), checked);
            });
        }

        $.each(fields, function(key) {
            if ($.inArray(key, hiddenFields) != -1)
                return true;
            var field = this;
            if (field.columns) {
                $.each(field.columns, function(idx) {
                    var subfield = this;
                    addColumnToTr($tr, key, 1, subfield.label);
                    return true;
                });
                var blankCell = addColumnToTr($tr, 'collapsible-column', 1, '');
                blankCell.css({'min-width': '10px', 'width': '10px'});
                blankCell.hide();
            } else {
                addColumnToTr($tr, key, 1, field.label);
            }
            return true;
        });

        // Re-order row buttons
        if (reorder) {
            $tr.append(
                $('<th>').html(_l('label.order')).addClass('reorder-actions reduced-hide')
            );
        }

        // Actions column
        var actionsArray = actions ? $.map(actions, function(v, k) {
            if (k == 'add' || k == 'rootAdminAddGuestNetwork') {
                v.isAdd = true;
            }

            return v;
        }) : [];
        var headerActionsArray = $.grep(
            actionsArray,
            function(action) {
                return action.isHeader || action.isAdd;
            }
        );

        if (actions && !options.noActionCol && renderActionCol(actions) && actionsArray.length != headerActionsArray.length) {
            $tr.append(
                $('<th></th>')
                .html(_l('label.actions'))
                .addClass('actions reduced-hide')
            );
        }

        // Quick view
        if (detailView && !$.isFunction(detailView) && !detailView.noCompact && !uiCustom) {
            $tr.append(
                $('<th></th>')
                .html(_l('label.quickview'))
                .addClass('quick-view reduced-hide')
            );
        }

        return $thead;
    };

    var createFilters = function($toolbar, filters) {
        if (!filters) return false;

        var $filters = $('<div></div>').addClass('filters reduced-hide');
        $filters.append($('<label>').html(_l('label.filterBy')));

        var $filterSelect = $('<select id="filterBy"></select>').appendTo($filters);

        if (filters)
            $.each(filters, function(key) {
                if (this.preFilter != null && this.preFilter() == false) {
                    return true; //skip to next item in each loop
                }
                var $option = $('<option>').attr({
                    value: key
                }).html(_l(this.label));

                $option.appendTo($filterSelect);

                return true;
            });

        return $filters.appendTo($toolbar);
    };

    var createSearchBar = function($toolbar, listViewData) {
        var $search = $('<div></div>').addClass('text-search reduced-hide');
        var $searchBar = $('<div></div>').addClass('search-bar reduced hide').appendTo($search);
        $searchBar.append('<input type="text" />');
        $search.append('<div id="basic_search" class="button search"></div>');

        if (listViewData.advSearchFields != null) {
            $search.append(
                $('<div>').attr({
                    id: 'advanced_search'
                })
                .addClass('button search advanced-search')
                .append($('<div>').addClass('icon'))
            );
        }

        return $search.appendTo($toolbar);
    };

    /**
     * Makes set of icons from data, in the for of a table cell
     */
    var makeActionIcons = function($td, actions, options) {
        options = options ? options : {};
        var allowedActions = options.allowedActions;
        var $tr = $td.closest('tr');
        var data = $tr && $tr.data('json-obj') ? $tr.data('json-obj') : null;

        $.each(actions, function(actionName, action) {
            if (actionName == 'add' || action.isHeader)
                return true;

            if (action.type == 'radio') {
                $td.closest('.list-view').addClass('list-view-select');
                $td.append(
                    $('<div></div>')
                    .addClass('action')
                    .addClass(actionName)
                    .append(
                        $('<input>').attr({
                            type: 'radio',
                            name: actionName
                        })
                    )
                    .attr({
                        alt: _l(action.label),
                        title: _l(action.label)
                    })
                    .data('list-view-action-id', actionName)
                );

                return true;
            } else if (action.type == 'checkbox') {
                $td.closest('.list-view').addClass('list-view-select');
                $td.append(
                    $('<div></div>')
                    .addClass('action')
                    .addClass(actionName)
                    .append(
                        $('<input>').attr({
                            type: 'checkbox',
                            name: actionName,
                            checked: data && data._isSelected ? 'checked' : false
                        })
                    )
                    .attr({
                        alt: _l(action.label),
                        title: _l(action.label)
                    })
                    .data('list-view-action-id', actionName)
                );

                if ($td.find('input[type=checkbox]').is(':checked')) {
                    $tr.addClass('multi-edit-selected');
                }

                return true;
            }

            var $action = $('<div></div>')
                .addClass('action')
                .addClass(actionName)
                .append($('<span>').addClass('icon').html('&nbsp;'))
                .attr({
                    alt: _l(action.label),
                    title: _l(action.label)
                })
                .data('list-view-action-id', actionName);

            if (action.textLabel) {
                $action
                    .addClass('text')
                    .prepend(
                        $('<span>').addClass('label').html(_l(action.textLabel))
                );
            }

            // Disabled appearance/behavior for filtered actions
            if (allowedActions && $.inArray(actionName, allowedActions) == -1) {
                $action.addClass('disabled');
            }

            $td.append($action);

            return true;
        });
    };

    /**
     * Initialize detail view for specific ID from list view
     */
    var createDetailView = function(args, complete, $row, options) {
        var $panel = args.$panel;
        var title = args.title;
        var id = args.id;
        var data = $.extend(true, {}, args.data, {
            $browser: $('#browser .container'),
            id: id,
            jsonObj: args.jsonObj,
            section: args.section,
            context: args.context,
            $listViewRow: $row,
            compact: options ? options.compact : false
        });

        var noPanel = options ? options.noPanel : false;
        var $detailView, $detailsPanel;
        var panelArgs = {
            title: title,
            parent: $panel,
            maximizeIfSelected: data.isMaximized,
            complete: function($newPanel) {
                // Make detail view element
                if (!args.pageGenerator && !data.isMaximized)
                    $detailView = $('<div>').addClass('detail-view').detailView(data).appendTo($newPanel);
                else if (!args.pageGenerator && data.isMaximized)
                    $detailView = $newPanel.detailView(data);
                else
                    $detailView = args.pageGenerator(data).appendTo($newPanel);

                if (complete) complete($detailView);

                return $detailView;
            }
        };

        if (noPanel) {
            return $('<div>').detailView(data);
        } else {
            $detailsPanel = data.$browser.cloudBrowser('addPanel', panelArgs);
        }
    };

    var addTableRows = function(preFilter, fields, data, $tbody, actions, options) {
        if (!options) options = {};
        var rows = [];
        var reorder = options.reorder;
        var detailView = options.detailView;
        var multiSelect = options.multiSelect;
        var $listView = $tbody.closest('.list-view');
        var listViewArgs = $listView.data('view-args');
        var uiCustom = listViewArgs.uiCustom;
        var subselect = uiCustom ? listViewArgs.listView.subselect : null;
        var hasCollapsibleColumn = false;

        if (!(data && data.length)) {
            $listView.data('end-of-table', true);
            if (!$tbody.find('tr').size()) {
                return [
                    $('<tr>').addClass('empty last').append(
                        $('<td>').html(_l('label.no.data'))
                    ).appendTo($tbody)
                ];
            }
        }

        $tbody.find('tr.empty').remove();

        $(data).each(function() {
            var dataItem = this;
            var id = dataItem.id;
            var $quickView;

            var $tr = $('<tr>');
            rows.push($tr);

            if (options.prepend) {
                $tr.prependTo($tbody);
            } else {
                $tr.appendTo($tbody);
            }

            var hiddenFields = [];
            if (preFilter != null)
                hiddenFields = preFilter();

            if (multiSelect) {
                var $td = $('<td>')
                        .addClass('multiselect')
                        .appendTo($tr);
                var content = $('<input>')
                    .attr('type', 'checkbox')
                    .addClass('multiSelectCheckbox')
                    .click(function() {
                        var checked = $(this).is(':checked');
                        var numRows = $(this).parents('tbody').find('input.multiSelectCheckbox').size();
                        var numRowsChecked = $(this).parents('tbody').find('input.multiSelectCheckbox:checked').size();
                        var enabled = checked || (numRowsChecked > 0);

                        toggleMultiSelectActions($td.closest('.list-view'), enabled);

                        $td.closest('.list-view').find('input.multiSelectMasterCheckbox').attr('checked', (numRows === numRowsChecked));
                    });

                $td.append(
                    $('<span></span>').html(content)
                );
            }

            var reducedFields = {};
            var idx = 0;
            $.each(fields, function(key) {
                var field = this;
                if (field.columns) {
                    $.each(field.columns, function(innerKey) {
                        reducedFields[innerKey] = this;
                    });
                    reducedFields['blank-cell-' + idx] = {blankCell: true};
                    idx += 1;
                    hasCollapsibleColumn = true;
                } else {
                    reducedFields[key] = this;
                }
                return true;
            });

            // Add field data
            $.each(reducedFields, function(key) {
                if ($.inArray(key, hiddenFields) != -1)
                    return true;
                var field = this;
                var $td = $('<td>')
                    .addClass(key)
                    .data('list-view-item-field', key)
                    .appendTo($tr);
                var content = dataItem[key];

                if (field.truncate) {
                    $td.addClass('truncated');
                }

                if (field.blankCell) {
                    $td.css({'min-width': '10px', 'width': '10px'});
                    $td.hide();
                }

                if (field.indicator) {
                    $td.addClass('state').addClass(field.indicator[content]);

                    // Disabling indicator for now per new design
                    //$tr.find('td:first').addClass('item-state-' + field.indicator[content]);
                }

                if (field.thresholdcolor && field.thresholds) {
                    if ((field.thresholds.disable in dataItem) && (field.thresholds.notification in dataItem)) {
                        var disableThreshold = parseFloat(dataItem[field.thresholds.disable]);
                        var notificationThreshold = parseFloat(dataItem[field.thresholds.notification]);
                        var value = parseFloat(content);
                        if (value >= disableThreshold) {
                            $td.addClass('alert-disable-threshold');
                        } else if (value >= notificationThreshold) {
                            $td.addClass('alert-notification-threshold');
                        }
                    }
                }


                if (field.limitcolor && field.limits) {
                    if ((field.limits.lowerlimit in dataItem) && (field.limits.upperlimit in dataItem)) {
                        var upperlimit = parseFloat(dataItem[field.limits.upperlimit]);
                        var lowerlimit = parseFloat(dataItem[field.limits.lowerlimit ]);
                        var value = parseFloat(content);
                        if (value <= lowerlimit) {
                            $td.addClass('alert-disable-threshold');
                        } else if (value <= upperlimit) {
                            $td.addClass('alert-notification-threshold');
                        }
                    }
                }

                if (field.id == true) id = field.id;
                if ($td.index()) $td.addClass('reduced-hide');
                if (field.action) {
                    $td.data('list-view-action', key);
                }
                if (field.converter) {
                    content = _l(field.converter(content, dataItem));
                }
                if (field.editable) {
                    $td.html(_s(content));
                    createEditField($td).appendTo($td);
                } else {
                    $td.html('');

                    if ($.isArray(content)) {
                        var $ul = $('<ul>');

                        $(content).map(function (index, contentItem) {
                            var $li = $('<li>');

                            $li.append('<span>').text(contentItem.toString());
                            $li.appendTo($ul);
                        });

                        $ul.appendTo($td);
                    } else if (field.span == false) {
                        $td.append(
                            $('<pre>').html(_s(content))
                        );
                    } else {
                        var span = $('<span>').html(_s(content));
                        if (field.compact) {
                            span.addClass('compact');
                            span.html('');
                        }
                        $td.append(span);
                    }
                }

                $td.attr('title', _s(content));
            });

            var $first = $tr.find('td:first');
            if (multiSelect)
                $first = $first.next();
            $first.addClass('first');

            // Add reorder actions
            if (reorder) {
                var sort = function($tr, action) {
                    var $listView = $tr.closest('.list-view');
                    var viewArgs = $listView.data('view-args');
                    var context = $.extend(
                        true, {},
                        $tr.closest('.list-view').data('view-args').context
                    );
                    var rowIndex = $tr.closest('tbody').find('tr').size() - ($tr.index());

                    context[viewArgs.activeSection] = $tr.data('json-obj');

                    action.action({
                        context: context,
                        index: rowIndex,
                        response: {
                            success: function(args) {},
                            error: function(args) {
                                // Move back to previous position
                                rowActions.moveTo($tr, rowIndex);
                            }
                        }
                    });
                };

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
                                var map1 = {};
                                $tr.closest('tbody').find('tr').each(function() {
                                    /*
                                     * fire only one sorting API call(updateXXXXXXX&sortKey=n&id=UUID) for items who have the same UUID.
                                     * e.g. An Template/ISO of multiple zones have the same UUID.
                                     */
                                    var objId = $(this).data('json-obj').id;
                                    if(!(objId in map1)) {
                                    sort($(this), action);
                                        map1[objId] = 1;
                                    }
                                });
                                $tr.closest('.data-table').dataTable('selectRow', $tr.index());

                                return false;
                            });
                    });
                });

                // Draggable action
                var initDraggable = function($tr) {
                    var originalIndex;

                    return $tr.closest('tbody').sortable({
                        handle: '.action.moveDrag',
                        start: function(event, ui) {
                            originalIndex = ui.item.index();
                        },
                        stop: function(event, ui) {
                            rowActions._std($tr, function() {});
                            var map1 = {};
                            $tr.closest('tbody').find('tr').each(function() {
                                /*
                                 * fire only one sorting API call(updateXXXXXXX&sortKey=n&id=UUID) for items who have the same UUID.
                                 * e.g. An Template/ISO of multiple zones have the same UUID.
                                 */
                                var objId = $(this).data('json-obj').id;
                                if(!(objId in map1)) {
                                sort($(this), reorder.moveDrag);
                                    map1[objId] = 1;
                                }
                            });
                        }
                    });
                };

                if (reorder && reorder.moveDrag) {
                    initDraggable($tr);
                }
            }

            // Add action data
            $tr.data('list-view-item-id', id);
            $tr.data('jsonObj', dataItem);
            $tr.data('list-view-action-filter', options.actionFilter);

            var actionsArray = actions ? $.map(actions, function(v, k) {
                if (k == 'add') {
                    v.isAdd = true;
                }

                return v;
            }) : [];
            var headerActionsArray = $.grep(
                actionsArray,
                function(action) {
                    return action.isHeader || action.isAdd;
                }
            );

            if (actions && !options.noActionCol && renderActionCol(actions) && actionsArray.length != headerActionsArray.length) {
                var allowedActions = $.map(actions, function(value, key) {
                    return key;
                });

                var $listView = $tr.closest('.list-view');
                var isUICustom = $listView.data('view-args') ?
                    $tr.closest('.list-view').data('view-args').uiCustom : false;

                if ($.isFunction(options.actionFilter) && !isUICustom) {
                    allowedActions = options.actionFilter({
                        context: $.extend(true, {}, options.context, {
                            actions: allowedActions,
                            item: dataItem
                        })
                    });
                }

                makeActionIcons(
                    $('<td></td>').addClass('actions reduced-hide')
                    .appendTo($tr),
                    actions, {
                        allowedActions: allowedActions
                    }
                );
            }

          $tr.closest('.list-view').trigger('cloudStack.listView.addRow', {
            $tr: $tr
          });

          // Add sub-select
            if (subselect) {
                var $td = $tr.find('td.first');
                var $select = $('<div></div>').addClass('subselect').append(
                    $('<span>').html(_l(subselect.label))
                ).hide();
                var $selectionArea = $tr.find('td:last').find('input');

                if (subselect.isMultiple) {
                    $select.append(
                        $('<select multiple>'),
                        $('<span>').addClass('info').html(_l('message.listView.subselect.multi'))
                    );
                } else {
                  $select.append($('<select>'));
                }

                $td.append($select);

                // Show and populate selection
                $selectionArea.change(function() {
                    if ($(this).is(':checked')) {
                        // Populate data
                        subselect.dataProvider({
                            context: $.extend(true, {},
                                ($listView && $listView.data('view-args') ?
                                    $.extend(true, {}, $listView.data('view-args').context, options.context) :
                                    options.context), {
                                instances: [$tr.data('json-obj')]
                            }),
                            response: {
                                success: function(args) {
                                    var data = args.data;

                                    if (data.length) {
                                        $(data).map(function(index, item) {
                                            var $option = $('<option>');

                                            $option.attr('value', item.id);
                                            $option.attr('title', item.description);
                                            $option.append(item.description);
                                            $option.appendTo($select.find('select'));
                                        });
                                        $select.show();
                                    } else {
                                        $select.hide();
                                    }

                                    $select.find('option:first').attr('selected', 'selected');
                                    $listView.find('.data-table').dataTable('refresh');
                                }
                            }
                        });

                        if ($(this).is('input[type=radio]')) {
                            $(this).closest('tr').siblings().find('input[type=radio]').change();
                        }
                    } else {
                        $select.find('option').remove();
                        $select.hide();
                    }
                });
            }

            // Add quick view
            if (detailView && !$.isFunction(detailView) && !detailView.noCompact && !uiCustom) {
                $quickView = $('<td>').addClass('quick-view reduced-hide')
                    .append(
                        $('<span>').addClass('icon').html('&nbsp;')
                )
                    .appendTo($tr);
                $quickView.mouseover(
                    // Show quick view
                    function() {
                        var $quickView = $(this);
                        var $quickViewTooltip = $('<div>').addClass('quick-view-tooltip hovered-elem');
                        var $tr = $quickView.closest('tr');
                        var $listView = $tr.closest('.list-view');
                        var $title = $('<div>').addClass('title');
                        var $detailsContainer = $('<div>').addClass('container').appendTo($quickViewTooltip);
                        var context = $.extend(true, {}, $listView.data('view-args').context);
                        var activeSection = $listView.data('view-args').activeSection;
                        var itemID = $tr.data('list-view-item-id');
                        var jsonObj = $tr.data('json-obj');
                        var $loading = $('<div>').addClass('loading-overlay').appendTo($detailsContainer);
                        var listViewArgs = $listView.data('view-args');
                        var listViewActiveSection = activeSection;
                        var targetSection;

                        if ($tr.hasClass('loading')) return;

                        if (listViewActiveSection != '_zone') {
                            if (listViewArgs.sections &&
                                listViewArgs.sections[listViewActiveSection] &&
                                listViewArgs.sections[listViewActiveSection].id) {
                                targetSection = listViewArgs.sections[listViewActiveSection].id;
                            } else {
                                targetSection = listViewActiveSection;
                            }
                        } else {
                            targetSection = detailViewArgs.section;
                        }

                        // Title
                        $title.append(
                            $('<span>').html(_l('label.quickview') + ': '),
                            $('<span>').addClass('title').html(
                                cloudStack.concat(
                                    $tr.find('td:first span').html(), 30
                                )
                            ).attr({
                                title: $tr.find('td:first span').html()
                            }),
                            $('<span>').addClass('icon').html('&nbsp;')
                        );
                        $quickViewTooltip.append($title);
                        $('.quick-view-tooltip').remove();
                        // Setup positioning
                        $quickViewTooltip.hide().appendTo('#container').fadeIn(200, function() {
                            if (!$quickViewTooltip.is(':visible')) return;

                            // Init detail view
                            context[targetSection] = [jsonObj];
                            createDetailView({
                                    data: $.extend(true, {}, detailView, {
                                        onLoad: function($detailView) {
                                            $loading.remove();
                                            $detailView.slideToggle('fast');
                                        },
                                        onPerformAction: function() {
                                            $tr.addClass('loading').find('td:last').prepend($('<div>').addClass('loading'));
                                            $quickViewTooltip.detach();
                                        },
                                        onActionComplete: function() {
                                            if (listViewArgs.onActionComplete) {
                                                listViewArgs.onActionComplete();
                                            }

                                            $tr.removeClass('loading').find('td:last .loading').remove();
                                            $quickViewTooltip.remove();
                                        }
                                    }),
                                    id: itemID,
                                    jsonObj: jsonObj,
                                    section: activeSection,
                                    context: context,
                                    $listViewRow: $tr
                                },
                                function($detailView) { //complete(), callback funcion
                                    $detailView.data('list-view', $listView);
                                }, $tr, {
                                    compact: true,
                                    noPanel: true
                                }
                            ).appendTo($detailsContainer).hide();
                        });
                        $quickViewTooltip.css({
                            position: 'absolute',
                            left: $quickView.offset().left + $quickView.outerWidth() - $quickViewTooltip.width() - 2*(parseInt($quickView.css('border-left-width')) + parseInt($quickView.css('border-right-width'))),
                            top: $quickView.offset().top,
                            zIndex: $tr.closest('.panel').zIndex() + 1
                        });

                        $quickViewTooltip.mouseleave(function() {
                            if (!$('.overlay:visible').size()) {
                                $quickViewTooltip.remove();
                            }
                        });
                    }
                );
            }
        });

        // Toggle collapsible column to fix alignment of hidden/shown cells
        if (hasCollapsibleColumn) {
            $tbody.closest('table').find('tr:first th.collapsible-column:visible').prev('th').click();
        }

        // Re-sort table if a column was previously sorted
        $listView.find('thead tr:last th.sorted').click().click();

        return rows;
    };

    var setLoading = function($table, completeFn) {
        var $loading = $('<tr>')
            .addClass('loading')
            .appendTo($table.find('tbody'))
            .append(
                $('<td>')
                .addClass('loading icon')
                .attr({
                    'colspan': $table.find('th').size()
                })
        );

        $table.closest('div.list-view').scrollTop($table.height() + 100);

        return completeFn({
            loadingCompleted: function() {
                $loading.remove();
            }
        });
    };

    var loadBody = function($table, dataProvider, preFilter, fields, append, loadArgs, actions, options) {
        if (!options) options = {};
        var context = options.context;
        var reorder = options.reorder;
        var multiSelect = options.multiSelect;
        var $tbody = $table.find('tbody');
        var $listView = $table.closest('.list-view');

        if (!loadArgs) loadArgs = {
            page: 1,
            filterBy: {
                search: {},
                kind: 'all',
                page: 1
            }
        };

        if (options.clearEndTable) {
            $listView.data('page', 1);
            $table.closest('.list-view').data('end-of-table', false);
        }

        if (!append) {
            if (!append) $table.find('tbody tr').remove();
        }

        var viewArgs = $listView.data('view-args');
        var uiCustom = viewArgs.listView ? viewArgs.listView.uiCustom : false;

        setLoading($table, function(setLoadingArgs) {
            $table.dataTable();
            $.extend(loadArgs, {
                context: options.context,
                response: {
                    success: function(args) {
                        setLoadingArgs.loadingCompleted();

                        addTableRows(preFilter, fields, args.data, $tbody, actions, {
                            actionFilter: args.actionFilter,
                            context: context,
                            reorder: reorder,
                            detailView: options.detailView,
                            'multiSelect': options.multiSelect,
                            noActionCol: options.noActionCol
                        });
                        $table.dataTable(null, {
                            noSelect: uiCustom
                        });

                        setTimeout(function() {
                            $table.dataTable('refresh');
                        });
                    },
                    error: function(args) {
                        setLoadingArgs.loadingCompleted();
                        addTableRows(preFilter, fields, [], $tbody, actions);
                        $table.find('td:first').html(_l('ERROR'));
                        $table.dataTable(null, {
                            noSelect: uiCustom
                        });
                    }
                }
            });
        });

        return dataProvider(loadArgs);
    };

    /**
     * Make 'switcher' buttons for sections
     */
    var createSectionSwitcher = function(args) {
        var sections = args.sections;
        var $switcher = $('<div>').addClass('section-switcher reduced-hide');
        var $sectionSelect = $('<select></select>')
            .appendTo(
                $('<div></div>')
                .addClass('section-select')
                .appendTo($switcher)
        );
        var sectionPreFilter;

        if (args.sectionSelect) {
            $('<label>')
                .prependTo($sectionSelect.parent())
                .html(_l(args.sectionSelect.label) + ':');

            sectionPreFilter = args.sectionSelect.preFilter ?
                args.sectionSelect.preFilter({
                    context: $.extend({}, cloudStack.context, args.context)
                }) : null;
        } else {
            $sectionSelect.hide();
        }

        // No need to display switcher if only one entry is present
        if (sectionPreFilter && sectionPreFilter.length == 1) {
            $switcher.find('select').hide();
            $switcher.find('label').html(
                _l('label.viewing') + ' ' + _l(sections[sectionPreFilter[0]].title)
            );
        }

        $.each(sections, function(key) {
            if (sectionPreFilter && $.inArray(key, sectionPreFilter) == -1) {
                return true;
            }

            var $sectionButton;

            if (!this.type || this.type == 'button') {
                $sectionButton = $('<div>')
                    .addClass('section')
                    .append(
                        $('<a>')
                        .addClass(key)
                        .attr({
                            href: '#'
                        })
                        .data('list-view-section-id', key)
                        .html(_l(this.title))
                );

                $sectionButton.appendTo($switcher);
            } else if (this.type == 'select') {
                $sectionSelect.append(
                    $('<option></option>')
                    .attr('value', key)
                    .html(_l(this.title))
                );
            }

            return true;
        });

        $switcher.find('div.section:first').addClass('first');
        $switcher.find('div.section:last').addClass('last');

        return $switcher;
    };

    /**
     * Generate/reset entire list view elements
     *
     * @param $container Container to place list view inside
     * @param args List view setup data
     * @param section If section, reset list view to specified section
     */
    var makeListView = function($container, args, section) {
        args.activeSection = section ? section : (
            args.listView.id ? args.listView.id : args.id
        );

        // Clear out any existing list view
        var $existingListView = $container.find('div.list-view');
        if ($existingListView.size()) {
            $existingListView.remove();
        }

        var listViewData = args.listView;

        if (section) {
            listViewData = args.sections[section].listView;

            var sectionTitle = _l(args.title);
            var subsectionTitle = _l(args.sections[section].title);

            // Show subsection in breadcrumb
            if (args.$breadcrumb) {
                if ((sectionTitle && subsectionTitle) &&
                    (sectionTitle != subsectionTitle)) {
                    args.$breadcrumb.find('span.subsection').html(' - ' + subsectionTitle);
                    args.$breadcrumb.attr('title', sectionTitle + ' - ' + subsectionTitle);
                } else {
                    args.$breadcrumb.find('span.subsection').html('');
                    args.$breadcrumb.attr('title', sectionTitle);
                }
            }
        }

        // Create table and other elems
        var $listView = $('<div></div>')
            .addClass('view list-view')
            .addClass(listViewData.section);

        $listView.data('view-args', args);

        var $toolbar = $('<div>').addClass('toolbar').appendTo($listView);
        var $table = $('<table>').appendTo($listView);
        var infScrollTimer;
        var actions = listViewData.actions;
        var reorder = listViewData.reorder;
        var multiSelect = listViewData.multiSelect;
        var tableHeight = $table.height();

        $listView.data('end-of-table', false);
        $listView.data('page', 1);

        var $switcher;
        if (args.sections) {
            $switcher = createSectionSwitcher(args);
            if (section) {
                $switcher
                    .appendTo($toolbar)
                    .find('a.' + section).addClass('active');
                $switcher.find('div.section-select select').val(section);
            }
        }

        if ($switcher && $switcher.find('option').size() == 1) {
            listViewData = args.sections[
                $switcher.find('select').val()
            ].listView;

            args.activeSection = listViewData.id;
        }

        if (listViewData.hideToolbar) {
            $toolbar.hide();
        }

        // Add panel controls
        $('<div class="panel-controls">').append($('<div class="control expand">').attr({
            'ui-id': 'toggle-expand-panel'
        })).appendTo($toolbar);

        if (listViewData.actions && listViewData.actions.add) {
            var showAdd = listViewData.actions.add.preFilter ?
                listViewData.actions.add.preFilter({
                    context: listViewData.context ? listViewData.context : args.context
                }) : true;

            if (showAdd) {
                $toolbar
                    .append(
                        $('<div>')
                        .addClass('button action add reduced-hide')
                        .data('list-view-action-id', 'add')
                        .append(
                            $('<span>').html(_l(listViewData.actions.add.label))
                        )
                );
            }
        }

        // List view header actions
        if (listViewData.actions) {
            $.each(listViewData.actions, function(actionName, action) {
                var preFilter = function(extendContext) {
                    var context = $.extend(true, {},
                        $listView.data('view-args').context ? $listView.data('view-args').context : cloudStack.context);

                    if (extendContext) {
                        $.extend(context, extendContext);
                    }

                    return action.preFilter ? action.preFilter({
                        id: listViewData.id,
                        context: context
                    }) : null;
                }

                if (!action.isHeader || (action.preFilter && !preFilter())) return true;

                var $action = $('<div>')
                    .addClass('button action main-action reduced-hide').addClass(actionName)
                    .data('list-view-action-id', actionName)
                    .append($('<span>').addClass('icon'))
                    .append($('<span>').html(_l(action.label)));

                if (action.isMultiSelectAction) {
                    $action.addClass('multiSelectAction');
                    $action.hide();

                    if (action.preFilter) {
                        $action.data('list-view-action-prefilter', preFilter);
                    }
                }

                $toolbar.append($action)

                return true;
            });
        }

        $('<tbody>').appendTo($table);

        createHeader(listViewData.preFilter,
            listViewData.fields,
            $table,
            listViewData.actions, {
                reorder: reorder,
                detailView: listViewData.detailView,
                'multiSelect': multiSelect,
                noActionCol: listViewData.noActionCol,
                groupableColumns: listViewData.groupableColumns
            });

        if (listViewData.noSplit) {
            $table.addClass('no-split');
        }

        if (listViewData.horizontalOverflow) {
            $table.addClass('horizontal-overflow');
            $table.parent().css({'overflow-x': 'auto'});
        }

        createFilters($toolbar, listViewData.filters);

        if (listViewData.hideSearchBar != true) {
        createSearchBar($toolbar, listViewData);
        }

        loadBody(
            $table,
            listViewData.dataProvider,
            listViewData.preFilter,
            listViewData.fields,
            false, {
                page: $listView.data('page'),
                filterBy: {
                    kind: $listView.find('select[id=filterBy]').val(),
                    search: {
                        value: $listView.find('input[type=text]').val(),
                        by: 'name'
                    }
                },
                ref: args.ref
            },
            listViewData.actions, {
                context: args.context,
                reorder: reorder,
                detailView: listViewData.detailView,
                'multiSelect': multiSelect,
                noActionCol: listViewData.noActionCol
            }
        );

        // Keyboard events
        $listView.bind('keypress', function(event) {
            var code = (event.keyCode ? event.keyCode : event.which);
            var $input = $listView.find('input:focus');

            if ($input.size() && $input.hasClass('edit') && code === 13) {
                uiActions.edit($input.closest('tr'), {
                    callback: listViewData.actions.edit.action
                });
            }
        });

        // Setup item events
        $listView.find('tbody').bind('click', function(event) {
            var $target = $(event.target);
            var listViewAction = $target.data('list-view-action');

            if (!listViewAction) return true;

            listViewData.fields[listViewAction].action();

            return true;
        });

        //basic search
        var basicSearch = function() {
            $listView.removeData('advSearch');
            advancedSearchData = {};

            $listView.data('page', 1);
            loadBody(
                $table,
                listViewData.dataProvider,
                listViewData.preFilter,
                listViewData.fields,
                false, {
                    page: $listView.data('page'),
                    filterBy: {
                        kind: $listView.find('select[id=filterBy]').val(),
                        search: {
                            value: $listView.find('input[type=text]').val(),
                            by: 'name'
                        }
                    }
                },
                listViewData.actions, {
                    context: $listView.data('view-args').context,
                    reorder: listViewData.reorder,
                    detailView: listViewData.detailView,
                    'multiSelect': multiSelect,
                    noActionCol: listViewData.noActionCol
                }
            );
        };

        $listView.find('.search-bar input[type=text]').keyup(function(event) {
            if (event.keyCode == 13) //13 is keycode of Enter key
                basicSearch();
            return true;
        });
        $listView.find('.button.search#basic_search').bind('click', function(event) {
            basicSearch();
            return true;
        });
        $listView.find('select').bind('change', function(event) {
            if ($(event.target).closest('.section-select').size()) return true;
            if ((event.type == 'click' ||
                    event.type == 'mouseup') &&
                ($(event.target).is('select') ||
                    $(event.target).is('option') ||
                    $(event.target).is('input')))
                return true;

            basicSearch();

            return true;
        });

        //advanced search
        var advancedSearch = function(args) {
            $listView.data('advSearch', args.data);
            $listView.data('page', 1);
            loadBody(
                $table,
                listViewData.dataProvider,
                listViewData.preFilter,
                listViewData.fields,
                false, {
                    page: $listView.data('page'),
                    filterBy: {
                        kind: $listView.find('select[id=filterBy]').val(),
                        advSearch: args.data
                    }
                },
                listViewData.actions, {
                    context: $listView.data('view-args').context,
                    reorder: listViewData.reorder,
                    detailView: listViewData.detailView,
                    'multiSelect': multiSelect,
                    noActionCol: listViewData.noActionCol
                }
            );
        };

        var advancedSearchData = {};

        var closeAdvancedSearch = function() {
            $listView.find('.advanced-search .form-container:visible').remove();
        };

        $listView.find('.advanced-search .icon').bind('click', function(event) {
            if ($listView.find('.advanced-search .form-container:visible').size()) {
                closeAdvancedSearch();

                return false;
            }

            // Setup advanced search default values, when existing data is present
            $.each(listViewData.advSearchFields, function(fieldID, field) {
                field.defaultValue = advancedSearchData[fieldID];
            });

            var form = cloudStack.dialog.createForm({
                noDialog: true,
                form: {
                    title: 'label.advanced.search',
                    fields: listViewData.advSearchFields
                },
                after: function(args) {
                    advancedSearch(args);
                    advancedSearchData = args.data;
                    $listView.find('.button.search#basic_search').siblings('.search-bar').find('input').val(''); //clear basic search input field to avoid confusion of search result
                    closeAdvancedSearch();
                }
            });
            var $formContainer = form.$formContainer;
            var $form = $formContainer.find('form');

            $formContainer.hide().appendTo($listView.find('.advanced-search')).show();
            $form.find('.form-item:first input').focus();
            $form.find('input[type=submit]')
                .show()
                .appendTo($form)
                .val(_l('label.search'));

            // Cancel button
            $form.append(
                $('<div>').addClass('button cancel').html(_l('label.cancel'))
                .click(function() {
                    closeAdvancedSearch();
                })
            );

            $form.submit(function() {
                form.completeAction($formContainer);
            });

            return false;
        });


        // Infinite scrolling event
        $listView.bind('scroll', function(event) {
            var listView = args.listView;
            if (!listView && args.sections && args.sections.hasOwnProperty(args.activeSection)) {
                listView = args.sections[args.activeSection].listView;
            }
            if (listView && listView.disableInfiniteScrolling) return false;
            if ($listView.find('tr.last, td.loading:visible').size()) return false;

            clearTimeout(infScrollTimer);
            infScrollTimer = setTimeout(function() {
                var loadMoreData = $listView.scrollTop() >= ($table.height() - $listView.height()) - $listView.height() / 4;
                var context = $listView.data('view-args').context;

                if (loadMoreData && !$listView.data('end-of-table')) {
                    $listView.data('page', $listView.data('page') + 1);

                    var filterBy = {
                        kind: $listView.find('select[id=filterBy]').length > 0 ? $listView.find('select[id=filterBy]').val() : 'all'
                    };
                    if ($listView.data('advSearch') == null) {
                        filterBy.search = {
                            value: $listView.find('input[type=text]').length > 0 ? $listView.find('input[type=text]').val() : '',
                            by: 'name'
                        };
                    } else {
                        filterBy.advSearch = $listView.data('advSearch');
                    }

                    loadBody(
                        $table,
                        listViewData.dataProvider,
                        listViewData.preFilter,
                        listViewData.fields, true, {
                            context: context,
                            page: $listView.data('page'),
                            filterBy: filterBy
                        }, actions, {
                            reorder: listViewData.reorder,
                            detailView: listViewData.detailView,
                            'multiSelect': multiSelect,
                            noActionCol: listViewData.noActionCol
                        });
                    $table.height() == tableHeight ? endTable = true : tableHeight = $table.height();
                }
            }, 500);

            return true;
        });

        // Action events
        $(window).bind('cloudstack.view-item-action', function(event, data) {
            var actionName = data.actionName;
            var $tr = $listView.find('tr').filter(function() {
                return $(this).data('list-view-item-id') == data.id;
            });

            if (actionName == 'destroy') {
                $tr.animate({
                    opacity: 0.5
                });
                $tr.bind('click', function() {
                    return false;
                });
            }
        });

        $listView.bind('click change', function(event) {
            var $target = $(event.target);
            var id = $target.closest('tr').data('list-view-item-id');
            var jsonObj = $target.closest('tr').data('jsonObj');
            var detailViewArgs;
            var detailViewPresent = ($target.closest('div.data-table tr td.first').size() &&
                listViewData.detailView && !$target.closest('div.edit').size()) && !listViewData.detailView.noPanelView;
            var uiCustom = args.uiCustom == true ? true : false;

            // Click on first item will trigger detail view (if present)
            if (detailViewPresent && !uiCustom && !$target.closest('.empty, .loading').size()) {
                var $loading = $('<div>').addClass('loading-overlay');
                $target.closest('div.data-table').prepend($loading); //overlay the whole listView, so users can't click another row until click-handling for this row is done (e.g. API response is back)

                listViewData.detailView.$browser = args.$browser;
                detailViewArgs = {
                    $panel: $target.closest('div.panel'),
                    data: listViewData.detailView,
                    title: $target.closest('td').find('span').html(),
                    id: id,
                    jsonObj: jsonObj,
                    ref: jsonObj,
                    context: $.extend(true, {}, $listView.data('view-args').context)
                };

                // Populate context object w/ instance data
                var listViewActiveSection = $listView.data('view-args').activeSection;

                // Create custom-generated detail view
                if (listViewData.detailView.pageGenerator) {
                    detailViewArgs.pageGenerator = listViewData.detailView.pageGenerator;
                }

                var listViewArgs = $listView.data('view-args');

                var targetSection;

                detailViewArgs.section = listViewArgs.activeSection ?
                    listViewArgs.activeSection : listViewArgs.id;

                if (listViewActiveSection != '_zone') {
                    if (listViewArgs.sections &&
                        listViewArgs.sections[listViewActiveSection] &&
                        listViewArgs.sections[listViewActiveSection].id) {
                        targetSection = listViewArgs.sections[listViewActiveSection].id;
                    } else {
                        targetSection = listViewActiveSection;
                    }
                } else {
                    targetSection = detailViewArgs.section;
                }

                detailViewArgs.context[targetSection] = [jsonObj];

                if ($.isFunction(detailViewArgs.data)) {
                    detailViewArgs.data = detailViewArgs.data({
                        context: detailViewArgs.context
                    });
                }

                detailViewArgs.data.onActionComplete = listViewArgs.onActionComplete;

                createDetailView(
                    detailViewArgs,
                    function($detailView) { //complete(), callback funcion
                        $detailView.data('list-view', $listView);
                        $loading.remove();
                    },
                    $target.closest('tr')
                );

                return false;
            }

            // Action icons
            if (!$target.closest('td.actions').hasClass('reorder') &&
                ($target.closest('td.actions').size() ||
                    $target.closest('.action.add').size() ||
                    $target.closest('.action.main-action').size())) {
                var actionID = $target.closest('.action').data('list-view-action-id');
                var $tr;

                if ($target.closest('.action').is('.disabled')) {
                    return false;
                }

                if ($target.closest('.action.add').size() ||
                    $target.closest('.action.main-action:not(.multiSelectAction)').size()) {
                    $tr = $target.closest('div.list-view').find('tr:first'); // Dummy row
                } else {
                    if (listViewData.actions[actionID].isMultiSelectAction) {
                        $tr = $listView.find('input.multiSelectCheckbox:checked').parents('tr');
                    } else {
                        $tr = $target.closest('tr');
                    }
                }

                var uiCallback = uiActions[actionID];

                if (!uiCallback)
                    uiCallback = uiActions['standard'];

                uiCallback($tr, {
                    action: listViewData.actions[actionID]
                });

                return true;
            }

            // Edit field action icons
            if ($target.hasClass('action') && $target.parent().is('div.edit')) {
                uiActions.edit($target.closest('tr'), {
                    callback: listViewData.actions.edit.action,
                    cancel: $target.hasClass('cancel')
                });
                return false;
            }

            // Section switcher
            if ($target.is('a') && $target.closest('div.section-switcher').size()) {
                makeListView($container, args, $target.data('list-view-section-id'));

                return false;
            }

            if ($target.is('div.section-switcher select') && event.type == 'change') {
                makeListView($container, args, $target.val());

                return false;
            }

            return true;
        });

        return $listView.appendTo($container);
    };

    var prependItem = function(listView, data, actionFilter, options) {
        if (!options) options = {};

        var viewArgs = listView.data('view-args');
        var listViewArgs = $.isPlainObject(viewArgs.listView) ? viewArgs.listView : viewArgs;
        var targetArgs = listViewArgs.activeSection ? listViewArgs.sections[
            listViewArgs.activeSection
        ].listView : listViewArgs;
        var reorder = targetArgs.reorder;
        var $tr = addTableRows(
            targetArgs.preFilter,
            targetArgs.fields,
            data,
            listView.find('table tbody'),
            targetArgs.actions, {
                prepend: true,
                actionFilter: actionFilter,
                reorder: reorder,
                detailView: targetArgs.detailView,
                'multiSelect': targetArgs.multiSelect,
                noActionCol: targetArgs.noActionCol
            }
        )[0];
        listView.find('table').dataTable('refresh');

        $tr.addClass('loading').find('td:last').prepend($('<div>').addClass('loading'));
        $tr.find('.action').remove();

        return $tr;
    };

    var replaceItem = function($row, data, actionFilter, after) {
        var $newRow;
        var $listView = $row.closest('.list-view');
        var viewArgs = $listView.data('view-args');
        var listViewArgs = $.isPlainObject(viewArgs.listView) ? viewArgs.listView : viewArgs;
        var targetArgs = listViewArgs.activeSection ? listViewArgs.sections[
            listViewArgs.activeSection
        ].listView : listViewArgs;
        var reorder = targetArgs.reorder;
        var multiSelect = targetArgs.multiSelect;
        var $table = $row.closest('table');
        var defaultActionFilter = $row.data('list-view-action-filter');

        $newRow = addTableRows(
            targetArgs.preFilter,
            targetArgs.fields,
            data,
            $listView.find('table tbody'),
            targetArgs.actions, {
                actionFilter: actionFilter ? actionFilter : defaultActionFilter,
                reorder: reorder,
                detailView: targetArgs.detailView,
                'multiSelect': multiSelect,
                noActionCol: targetArgs.noActionCol
            }
        )[0];

        $newRow.data('json-obj', data);
        $row.replaceWith($newRow);
        $table.dataTable('refresh');

        if (after) after($newRow);

        return $newRow;
    };

    var toggleMultiSelectActions = function($listView, enabled) {
        var $multiSelectActions = $listView.find('div.main-action.multiSelectAction');

        $listView.find('div.action.add')[enabled ? 'hide' : 'show']();
        $listView.find('div.main-action:not(.multiSelectAction)')[enabled ? 'hide' : 'show']();
        $multiSelectActions.hide();

        if (enabled) {
            $multiSelectActions.filter(function() {
                var preFilter = $(this).data('list-view-action-prefilter');
                var $selectedVMs;
                var context = {};

                if (preFilter) {
                    $selectedVMs = $listView.find('tbody tr').filter(function() {
                        return $(this).find('td.multiselect input[type=checkbox]:checked').size()
                    });
                    context[$listView.data('view-args').activeSection] = $selectedVMs.map(function(index, item) {
                        return $(item).data('json-obj');
                    });

                    return preFilter(context);
                }

                return true;
            }).show();
        }
    }

    $.fn.listView = function(args, options) {
        if (!options) options = {};
        if (args == 'prependItem') {
            return prependItem(this, options.data, options.actionFilter);
        } else if (args == 'replaceItem') {
            replaceItem(options.$row, options.data, options.actionFilter, options.after);
        } else if (args.sections) {
            var targetSection;
            $.each(args.sections, function(key) {
                targetSection = key;
                return false;
            });
            makeListView(this, $.extend(true, {}, args, {
                context: options.context
            }), targetSection);
        } else if (args == 'refresh') {
            var activeSection = this.data('view-args').activeSection;
            var listViewArgs = this.data('view-args').sections ?
                this.data('view-args').sections[activeSection].listView :
                this.data('view-args').listView;

            loadBody(
                this.find('table:last'),
                listViewArgs.dataProvider,
                listViewArgs.preFilter,
                listViewArgs.fields,
                false,
                null,
                listViewArgs.actions, {
                    clearEndTable: true,
                    multiSelect: listViewArgs.multiSelect,
                    context: this.data('view-args').context,
                    detailView: listViewArgs.detailView
                }
            );
        } else {
            makeListView(
                this,
                $.extend(true, {}, args, {
                    context: options.context ? options.context : cloudStack.context
                }));
        }

        return this;
    };

    // List view refresh handler
    $(window).bind('cloudStack.fullRefresh', function() {
        var $listViews = $('.list-view');

        $listViews.each(function() {
            var $listView = $(this);

            $listView.listView('refresh');
        });
    });
})(window.jQuery, window.cloudStack, window._l, window._s);
