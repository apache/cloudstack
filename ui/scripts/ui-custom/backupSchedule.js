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
    cloudStack.uiCustom.backupSchedule = function(args) {
        var desc = args.desc;
        var selects = args.selects;
        var actions = args.actions;
        var dataProvider = args.dataProvider;

        return function(args) {
            var $backups = $('#template').find('.recurring-snapshots').clone();
            var context = args.context;

            // Update labels
            $backups.find('.forms ul li.hourly a').html(_l('label.hourly'));
            $backups.find('.forms ul li.daily a').html(_l('label.daily'));
            $backups.find('.forms ul li.weekly a').html(_l('label.weekly'));
            $backups.find('.forms ul li.monthly a').html(_l('label.monthly'));
            $backups.find('.field.timezone .name').html(_l('label.timezone'));
            $backups.find('.field.time .name').html(_l('label.time'));
            $backups.find('.field.time .value label').html(_l('label.minute.past.hour'));
            $backups.find('.field.maxsnaps').hide();
            $backups.find('.add-snapshot-action.add').html(_l('label.configure'));

            $backups.find('.desc').html(_l(desc));
            $backups.find('.forms').tabs();

            $backups.find('form select').each(function() {
                var $select = $(this);
                var selectData = selects[$select.attr('name')];

                if (selectData) {
                    selectData({
                        response: {
                            success: function(args) {
                                $(args.data).each(function() {
                                    var $option = $('<option>').appendTo($select);

                                    $option.val(this.id).html(_l(this.name));
                                });
                            }
                        }
                    });
                }
            });

            $backups.find('form').validate();
            $backups.find('.scheduled-snapshots p').html('Backup Schedule');
            $($.find('.scheduled-snapshots tr td.keep')).hide();

            $backups.find('.add-snapshot-action.add').click(function() {
                var $form = $backups.find('form:visible');
                if (!$form.valid()) return false;
                var formData = cloudStack.serializeForm($form);
                actions.add({
                    context: context,
                    snapshot: formData,
                    response: {
                        success: function(args) {
                            $backups.find('.scheduled-snapshots tr').hide();
                            var $backupScheduleRow = $backups.find('.scheduled-snapshots tr').filter(function() {
                                return $(this).index() == args.data.type;
                            }).addClass('active').show();

                            $backupScheduleRow.data('json-obj', args.data);

                            // Update fields
                            $backupScheduleRow.find('td.time span').html(args.data.time);
                            $backupScheduleRow.find('td.day-of-week span').html(_l(
                                args.data['day-of-week'] ?
                                $backups.find('select[name=day-of-week] option').filter(function() {
                                    return $(this).val() == args.data['day-of-week'];
                                }).html() :
                                args.data['day-of-month']
                            ));
                            $backupScheduleRow.find('td.timezone span').html(
                                $backups.find('select[name=timezone] option').filter(function() {
                                    return $(this).val() == args.data['timezone'];
                                }).html()
                            );
                            $backupScheduleRow.find('td.keep').hide();

                            $(':ui-dialog').dialog('option', 'position', 'center');

                        }
                    }
                });

                return true;
            });

            // Remove backup
            $backups.find('.action.destroy').click(function() {
                var $tr = $(this).closest('tr');
                actions.remove({
                    context: context,
                    snapshot: $tr.data('json-obj'),
                    response: {
                        success: function(args) {
                            $tr.hide().removeClass('active');
                            $(':ui-dialog').dialog('option', 'position', 'center');

                        }
                    }
                });
            });

            // Get existing data
            dataProvider({
                context: context,
                response: {
                    success: function(args) {
                        $(args.data).each(function() {
                            var backup = this;

                            // Get matching table row
                            var $tr = $backups.find('tr').filter(function() {
                                return $(this).index() == backup.type;
                            }).addClass('active').show();

                            $tr.data('json-obj', backup);

                            $tr.find('td.time span').html(backup.time);
                            $tr.find('td.timezone span').html(
                                $backups.find('select[name=timezone] option').filter(function() {
                                    return $(this).val() == backup['timezone'];
                                }).html()
                            );
                            $tr.find('td.day-of-week span').html(
                                backup['day-of-week'] ?
                                $backups.find('select[name=day-of-week] option').filter(function() {
                                    return $(this).val() == backup['day-of-week'];
                                }).html() :
                                backup['day-of-month']
                            );
                            $tr.find('td.keep').hide();
                        });

                    }
                }
            });

            // Create dialog
            var $dialog = $backups.dialog({
                title: _l('Backup Schedule'),
                dialogClass: 'recurring-snapshots',
                closeOnEscape: false,
                width: 600,
                buttons: [{
                    text: _l('label.close'),
                    'class': 'ok',
                    click: function() {
                        $dialog.fadeOut(function() {
                            $dialog.remove();
                        });

                        $('div.overlay').fadeOut(function() {
                            $('div.overlay').remove();
                        });
                    }
                }]
            });

            return cloudStack.applyDefaultZindexAndOverlayOnJqueryDialogAndRemoveCloseButton($dialog);
        };
    };
}(cloudStack, jQuery));
