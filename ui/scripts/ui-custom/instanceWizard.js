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
    /**
     * Instance wizard
     */
    cloudStack.uiCustom.instanceWizard = function(args) {
        return function(listViewArgs) {
            var context = listViewArgs.context;

            var instanceWizard = function(data) {
                var $wizard = $('#template').find('div.instance-wizard').clone();
                var $progress = $wizard.find('div.progress ul li');
                var $steps = $wizard.find('div.steps').children().hide();
                var $diagramParts = $wizard.find('div.diagram').children().hide();
                var $form = $wizard.find('form');

                $form.validate();

                // Close instance wizard
                var close = function() {
                    $wizard.dialog('destroy');
                    $('div.overlay').fadeOut(function() {
                        $('div.overlay').remove();
                    });
                };

                // Save instance and close wizard
                var completeAction = function() {
                    var data = cloudStack.serializeForm($form);
                    var $wizardLoading = $('<div>').addClass('loading-overlay').appendTo($wizard).css('z-index', '10000');

                    // Pass network IPs
                    data['my-network-ips'] = [];
                    $form.find('.my-networks .select .specify-ip input[type=text]').each(function() {
                        var $input = $(this);

                        if (!$input.closest('.select').find('input[type=checkbox]').is(':checked')) return true;

                        data['my-network-ips'].push(
                            $input.closest('.select').hasClass('advanced') ?
                                $input.val() : null
                        );
                    });
                    data['new-network-ip'] = $form.find('.new-network .select.advanced .specify-ip input[type=text]').val();

                    // Handle multi-disk service offerings
                    if ($form.find('.multi-disk-select-container').size()) {
                        data['disk-offerings-multi'] = [];

                        var $diskGroups = $form.find('.disk-select-group');
                        var $selectedDisks = $.grep($diskGroups, function (diskGroup) {
                            return $(diskGroup).find('input[type=checkbox]:checked').size();
                        });

                        $selectedDisks.map(function (disk) {
                            data['disk-offerings-multi'].push(
                                $.extend($(disk).data('json-obj'), {
                                    _diskOfferingId: $(disk).find('input[type=radio]:checked').val()
                                })
                            );
                        });
                    }

                    args.action({
                        // Populate data
                        context: context,
                        data: data,
                        $wizard: $wizard,
                        response: {
                            success: function(args) {
                                var $listView = $('.list-view.instances');

                                if ($listView.size()) {
                                    var $loading = $('.list-view.instances').listView('prependItem', {
                                        data: [{
                                            name: data.displayname ? data.displayname : _l('label.new.vm'),
                                            zonename: $wizard.find('select[name=zoneid] option').filter(function() {
                                                return $(this).val() == data.zoneid;
                                            }).html(),
                                            state: 'Creating'
                                        }],
                                        actionFilter: function(args) {
                                            return [];
                                        }
                                    });
                                }

                                listViewArgs.complete({
                                    _custom: args._custom,
                                    messageArgs: cloudStack.serializeForm($form),
                                    $item: $listView.size() ? $loading : $('<div>')
                                });

                                close();
                            },
                            error: function(message) {
                                $wizard.remove();
                                $('div.overlay').remove();

                                if (message) {
                                    cloudStack.dialog.notice({
                                        message: message
                                    });
                                }
                            }
                        }
                    });
                };

                var makeSelects = function(name, data, fields, options, selectedObj, selectedObjNonEditable) {
                    var $selects = $('<div>');
                    options = options ? options : {};

                    $(data).each(function() {
                        var item = this;
                        var id = item[fields.id];

                        var $select = $('<div>')
                            .addClass('select')
                            .append(
                                $('<input>')
                                .attr({
                                    type: (function(type) {
                                        return type ? type : 'radio';
                                    })(options ? options.type : null),
                                    name: name,
                                    'wizard-field': options['wizard-field']
                                })
                                .val(id)
                                .click(function() {
                                    var $select = $(this).closest('.select');
                                    var isSingleSelect = $select.hasClass('single-select');
                                    var $radio = $select.find('input[type=radio]');
                                    var $newNetwork = $(this).closest('.content').find('.select.new-network');
                                    var $otherSelects = $select.siblings().filter(':visible');
                                    var isCheckbox = $(this).attr('type') == 'checkbox';

                                    if (isCheckbox) {
                                        if (isSingleSelect) {
                                            $select.siblings('.single-select:visible').find('input[type=checkbox]')
                                                .attr('checked', false);

                                            var $checkedOtherSelect = $otherSelects.filter(function() {
                                                return $(this).not('.single-select') &&
                                                    $(this).find('input[type=checkbox]:checked').size() &&
                                                    $(this).find('input[type=radio]:checked').size();
                                            });

                                            if (!$checkedOtherSelect.size() &&
                                                !$('.new-network:visible input[type=radio]:checked').size()) {
                                                $(this).closest('.select').find('input[type=radio]').click();
                                            }
                                        }

                                        if ((!$otherSelects.size()) &&
                                            $newNetwork.find('input[type=checkbox]').is(':unchecked')) {
                                            // Set as default
                                            $(this).closest('.select').find('input[type=radio]').click();
                                        }
                                    }

                                    if ($radio.is(':checked') && !$(this).is(':checked')) {
                                        if (!$radio.closest('.select').index()) {
                                            return false;
                                        } else {
                                            $otherSelects.filter(':first')
                                                .find('input[type=radio]').click();
                                        }
                                    }

                                    return true;
                                })
                        )
                            .append(
                                $('<div>').addClass('select-desc')
                                .append($('<div>').addClass('name').html(_s(this[fields.name])))
                                .append($('<div>').addClass('desc').html(_s(this[fields.desc])))
                        )
                            .data('json-obj', this);

                        if (selectedObj != null && selectedObj.id == item.id) {
                            $select.find('input[type=checkbox]').attr('checked', 'checked');

                            if (selectedObjNonEditable) {
                                $select.find('input[type=checkbox]').attr('disabled', 'disabled');
                                $select.find('input[type=checkbox]').clone().attr({
                                    type: 'hidden',
                                    disabled: false
                                }).appendTo($selects);
                            }
                        }

                        $selects.append($select);

                        if (item._singleSelect) {
                            $select.addClass('single-select');
                        }

                        if (options.secondary) {
                            var $secondary = $('<div>').addClass('secondary-input').append(
                                $('<input>')
                                .attr({
                                    type: options.secondary.type,
                                    name: options.secondary.name,
                                    'wizard-field': options.secondary['wizard-field']
                                })
                                .val(id)
                                .click(function() {
                                    var $checkbox = $(this).closest('.select').find('input[type=checkbox]');

                                    if (!$checkbox.is(':checked')) {
                                        $checkbox.attr('checked', true);
                                    }

                                    if ($(this).closest('.select-container').hasClass('single-select')) {
                                        $(this).closest('.select').siblings().find('input[type=checkbox]')
                                            .attr('checked', false);
                                    }

                                    if ($select.hasClass('single-select')) {
                                        $select.siblings('.single-select:visible').find('input[type=checkbox]')
                                            .attr('checked', false);
                                    }
                                })
                                .after(
                                    $('<div>').addClass('name').html(options.secondary.desc)
                                )
                            ).appendTo($select);
                        }
                    });

                    cloudStack.evenOdd($selects, 'div.select', {
                        even: function($elem) {
                            $elem.addClass('even');
                        },
                        odd: function($elem) {
                            $elem.addClass('odd');
                        }
                    });

                    return $selects.children();
                };

                var dataProvider = function(step, providerArgs, callback) {
                    // Call appropriate data provider
                    args.steps[step - 1]($.extend(providerArgs, {
                        currentData: cloudStack.serializeForm($form),
                        initArgs: args,
                        context: context
                    }));
                };

                var dataGenerators = {
                    setup: function($step, formData) {
                        var originalValues = function(formData) {
                            $step.find('select').val(
                                formData.zoneid
                            );

                            $step.find('input[type=radio]').filter(function() {
                                return $(this).val() == formData['select-template'];
                            }).click();
                        };

                        return {
                            response: {
                                success: function(args) {
                                    // Zones
                                    $(args.data.zones).each(function() {
                                        $step.find('.select-zone select').append(
                                            $('<option>')
                                            .attr({
                                                value: this.id,
                                                'wizard-field': 'zone'
                                            })
                                            .html(this.name)
                                        );
                                    });

                                    originalValues(formData);
                                }
                            }
                        };
                    },

                    'select-iso': function($step, formData) {
                        $step.find('.section.custom-size').hide();

                        var originalValues = function(formData) {
                            var $inputs = $step.find('.wizard-step-conditional:visible')
                                .find('input[type=radio]');
                            var $selected = $inputs.filter(function() {
                                return $(this).val() == formData.templateid;
                            });

                            if (!$selected.size()) {
                                $inputs.filter(':first').click();
                            } else {
                                $selected.click();
                            }
                            $step.find('select[name=hypervisorid]:visible').val(
                                formData.hypervisorid
                            );
                        };

                        $step.find('.wizard-step-conditional').hide();

                        return {
                            response: {
                                success: function(args) {
                                    if (formData['select-template']) {
                                        $step.find('.wizard-step-conditional').filter(function() {
                                            return $(this).hasClass(formData['select-template']);
                                        }).show();
                                    } else {
                                        $step.find('.select-iso').show();
                                    }
                                    var makeIsos = function(type, append) {
                                        var $selects = makeSelects('templateid', args.data.templates[type], {
                                            name: 'name',
                                            desc: 'displaytext',
                                            id: 'id'
                                        }, {
                                            'wizard-field': 'template'
                                        });
                                        var $templateHypervisor = $step.find('input[type=hidden][wizard-field=hypervisor]');

                                        // Get hypervisor from template
                                        if (type == 'featuredtemplates' || type == 'communitytemplates' || type == 'mytemplates' || type == 'sharedtemplates') {
                                            $selects.each(function() {
                                                var $select = $(this);
                                                var template = $.grep(args.data.templates[type], function(tmpl, v) {
                                                    return tmpl.id == $select.find('input').val();
                                                })[0];

                                                $select.change(function() {
                                                    $templateHypervisor
                                                        .attr('disabled', false)
                                                        .val(template.hypervisor);
                                                });
                                            });
                                        } else {
                                            $templateHypervisor.attr('disabled', 'disabled');
                                        }

                                        if (type == 'featuredisos' || type == 'communityisos' || type == 'myisos' || type == 'sharedisos') {
                                            // Create hypervisor select
                                            $selects.find('input').bind('click', function() {
                                                var $select = $(this).closest('.select');

                                                //$select.siblings().removeClass('selected').find('.hypervisor').remove(); //SelectISO has 3 tabs now. This line only remove hypervisor div in the same tab, not enough. The following 3 lines will remove hypervisor div in all of 3 tabs.
                                                $("#instance-wizard-featured-isos .select-container div.selected").removeClass('selected').find('div.hypervisor').remove();
                                                $("#instance-wizard-community-isos .select-container div.selected").removeClass('selected').find('div.hypervisor').remove();
                                                $("#instance-wizard-my-isos .select-container div.selected").removeClass('selected').find('div.hypervisor').remove();
                                                $("#instance-wizard-shared-isos .select-container div.selected").removeClass('selected').find('div.hypervisor').remove();

                                                $select.addClass('selected').append(
                                                    $('<div>').addClass('hypervisor')
                                                    .append($('<label>').html('Hypervisor:'))
                                                    .append($('<select>').attr({
                                                        name: 'hypervisorid'
                                                    }))
                                                );

                                                // Get hypervisor data
                                                $(args.data.hypervisors).each(function() {
                                                    $select.find('select').append(
                                                        $('<option>').attr({
                                                            value: this[args.hypervisor.idField],
                                                            'wizard-field': 'hypervisor'
                                                        })
                                                        .html(this[args.hypervisor.nameField])
                                                    );
                                                });
                                            });
                                        }

                                        append($selects);
                                    };

                                    // Featured ISOs
                                    $(
                                        [
                                            // Templates
                                            ['featuredtemplates', 'instance-wizard-featured-templates'],
                                            ['communitytemplates', 'instance-wizard-community-templates'],
                                            ['mytemplates', 'instance-wizard-my-templates'],
                                            ['sharedtemplates', 'instance-wizard-shared-templates'],

                                            // ISOs
                                            ['featuredisos', 'instance-wizard-featured-isos'],
                                            ['communityisos', 'instance-wizard-community-isos'],
                                            ['myisos', 'instance-wizard-my-isos'],
                                            ['sharedisos', 'instance-wizard-shared-isos']
                                            //['isos', 'instance-wizard-all-isos']
                                        ]
                                    ).each(function() {
                                        var item = this;
                                        var $selectContainer = $wizard.find('#' + item[1]).find('.select-container');

                                        makeIsos(item[0], function($elem) {
                                            $selectContainer.append($elem);
                                        });
                                    });

                                    var custom = args.customHidden({
                                        context: context,
                                        data: args.data
                                    });

                                    $step.find('.custom-size-label').remove();

                                    if (custom) {
                                        $step.find('.section.custom-size').hide();
                                        $step.removeClass('custom-disk-size');
                                    }

                                    $step.find('input[type=radio]').bind('change', function() {
                                        var $target = $(this);
                                        var val = $target.val();
                                        var item = null;
                                        if (item == null) {
                                            item = $.grep(args.data.templates.featuredtemplates, function(elem) {
                                                return elem.id == val;
                                            })[0];
                                        }
                                        if (item == null) {
                                            item = $.grep(args.data.templates.communitytemplates, function(elem) {
                                                return elem.id == val;
                                            })[0];
                                        }
                                        if (item == null) {
                                            item = $.grep(args.data.templates.mytemplates, function(elem) {
                                                return elem.id == val;
                                            })[0];
                                        }
                                        if (item == null) {
                                            item = $.grep(args.data.templates.sharedtemplates, function(elem) {
                                                return elem.id == val;
                                            })[0];
                                        }

                                        if (!item) return true;

                                        var hypervisor = item['hypervisor'];
                                        if (hypervisor == 'KVM') {
                                            $step.find('.section.custom-size').show();
                                            $step.addClass('custom-disk-size');
                                        } else {
                                            $step.find('.section.custom-size').hide();
                                            $step.removeClass('custom-disk-size');
                                        }

                                        return true;
                                    });

                                    originalValues(formData);

                                }
                            }
                        };
                    },

                    'service-offering': function($step, formData) {
                        var originalValues = function(formData) {
                            if (formData.serviceofferingid) {
                                $step.find('input[type=radio]').filter(function() {
                                    return $(this).val() == formData.serviceofferingid;
                                }).click();
                            } else {
                                $step.find('input[type=radio]:first').click();
                            }
                        };

                        return {
                            response: {
                                success: function(args) {
                                    $step.find('.content .select-container').append(
                                        makeSelects('serviceofferingid', args.data.serviceOfferings, {
                                            name: 'name',
                                            desc: 'displaytext',
                                            id: 'id'
                                        }, {
                                            'wizard-field': 'service-offering'
                                        })
                                    );

                                    $step.find('input[type=radio]').bind('change', function() {
                                        var $target = $(this);
                                        var val = $target.val();
                                        var item = $.grep(args.data.serviceOfferings, function(elem) {
                                            return elem.id == val;
                                        })[0];

                                        if (!item) return true;

                                        var custom = item[args.customFlag];

                                        if (custom) {
                                            $step.addClass('custom-size');
                                        } else {
                                            $step.removeClass('custom-size');
                                        }

                                        var customIops = item[args.customIopsFlag];

                                        if (customIops && args.canShowCustomIops) {
                                            $step.addClass('custom-iops');
                                        } else {
                                            $step.removeClass('custom-iops');
                                        }

                                        return true;
                                    });

                                    originalValues(formData);
                                }
                            }
                        };
                    },

                    'data-disk-offering': function($step, formData) {
                        var originalValues = function(formData) {
                            var $targetInput = $step.find('input[type=radio]').filter(function() {
                                return $(this).val() == formData.diskofferingid;
                            }).click();

                            if (!$targetInput.size()) {
                                $step.find('input[type=radio]:visible').filter(':first').click();
                            }
                        };

                        $step.find('.section.custom-size').hide();

                        return {
                            response: {
                                success: function(args) {
                                    var multiDisk = args.multiDisk;

                                    $step.find('.multi-disk-select-container').remove();
                                    $step.removeClass('custom-disk-size');

                                    if (args.required) {
                                        $step.find('.section.no-thanks').hide();
                                        $step.addClass('required');
                                    } else {
                                        $step.find('.section.no-thanks').show();
                                        $step.removeClass('required');
                                    }

                                    var $selectContainer = $step.find('.content .select-container:not(.multi-disk)');

                                    if (multiDisk) { // Render as multiple groups for each disk
                                        var $multiDiskSelect = $('<div>').addClass('multi-disk-select-container');

                                        $(multiDisk).map(function(index, disk) {
                                            var $group = $('<div>').addClass('disk-select-group');
                                            var $header = $('<div>').addClass('disk-select-header').append(
                                                $('<div>').addClass('title').html(disk.label)
                                            ).appendTo($group);
                                            var $checkbox = $('<input>').addClass('multi-disk-select')
                                            .attr({
                                                type: 'checkbox',
                                                'disk-id': disk.id
                                            })
                                            .prependTo($header);
                                            var $multiSelectContainer = $selectContainer.clone().append(
                                                makeSelects('diskofferingid.' + disk.id, args.data.diskOfferings, {
                                                    id: 'id',
                                                    name: 'name',
                                                    desc: 'displaytext'
                                                }, {
                                                    'wizard-field': 'disk-offering'
                                                })
                                            ).appendTo($group).addClass('multi-disk');

                                            $group.appendTo($multiDiskSelect);
                                            $group.data('json-obj', disk);

                                            // Show-hide disk group selects
                                            $checkbox.click(function() {
                                                $group.toggleClass('selected');
                                                $group.find('.select:first input[type=radio]').click();

                                                if (!$multiDiskSelect.find('input[type=checkbox]:checked').size()) {
                                                    $step.find('.no-thanks input[type=radio]').click();
                                                } else {
                                                    $step.find('.no-thanks input[type=radio]').attr('checked', false);
                                                }
                                            });

                                            // Add custom disk size box
                                            $step.find('.section.custom-size').clone().hide().appendTo($group);
                                        });

                                        $multiDiskSelect.insertAfter($selectContainer);
                                        $selectContainer.hide();

                                        // Fix issue with containers always showing after reload
                                        $multiDiskSelect.find('.select-container').attr('style', null);
                                    } else {
                                        $selectContainer.show();
                                        $step.find('.content .select-container').append(
                                            makeSelects('diskofferingid', args.data.diskOfferings, {
                                                id: 'id',
                                                name: 'name',
                                                desc: 'displaytext'
                                            }, {
                                                'wizard-field': 'disk-offering'
                                            })
                                        );
                                    }

                                    $step.find('input[type=radio]').bind('change', function() {
                                        var $target = $(this);
                                        var val = $target.val();
                                        var item = $.grep(args.data.diskOfferings, function(elem) {
                                            return elem.id == val;
                                        })[0];
                                        var isMultiDisk = $step.find('.multi-disk-select').size();

                                        // Uncheck any multi-select groups
                                        if ($target.closest('.no-thanks').size() && isMultiDisk) {
                                            $step.find('.disk-select-group input[type=checkbox]:checked').click();
                                            $(this).attr('checked', true);

                                            return true;
                                        }

                                        if (!item) {
                                            if (isMultiDisk) {
                                                $(this).closest('.disk-select-group .section.custom-size').hide();
                                                $(this).closest('.disk-select-group').removeClass('custom-size');
                                            } else {
                                                // handle removal of custom size controls
                                                $step.find('.section.custom-size').hide();
                                                $step.removeClass('custom-disk-size');

                                                // handle removal of custom IOPS controls
                                                $step.removeClass('custom-iops-do');
                                            }

                                            return true;
                                        }

                                        var custom = item[args.customFlag];

                                        if (!isMultiDisk) $step.find('.custom-size-label').remove();

                                        if (custom && !isMultiDisk) {
                                            $target.parent().find('.name')
                                            .append(
                                                $('<span>').addClass('custom-size-label')
                                                .append(': ')
                                                .append(
                                                    $('<span>').addClass('custom-disk-size').html(
                                                        $step.find('.custom-size input[name=size]').val()
                                                )
                                                )
                                                .append(' GB')
                                            );
                                            $target.parent().find('.select-desc .desc')
                                            .append(
                                                $('<span>').addClass('custom-size-label')
                                                .append(', ')
                                                .append(
                                                    $('<span>').addClass('custom-disk-size').html(
                                                        $step.find('.custom-size input[name=size]').val()
                                                )
                                                )
                                                .append(' GB')
                                            );
                                            $step.find('.section.custom-size').show();
                                            $step.addClass('custom-disk-size');
                                            $target.closest('.select-container').scrollTop(
                                                $target.position().top
                                            );
                                        } else if (custom && isMultiDisk) {
                                            $(this).closest('.disk-select-group').addClass('custom-size');
                                        } else {
                                            if (isMultiDisk) {
                                                $(this).closest('.disk-select-group').removeClass('custom-size');
                                            } else {
                                                $step.find('.section.custom-size').hide();
                                                $step.removeClass('custom-disk-size');
                                            }
                                        }

                                        var customIops = item[args.customIopsDoFlag];

                                        if (customIops) {
                                            $step.addClass('custom-iops-do');
                                        } else {
                                            $step.removeClass('custom-iops-do');
                                        }

                                        return true;
                                    });

                                    originalValues(formData);
                                }
                            }
                        };
                    },

                    'affinity': function($step, formData) {
                        return {
                            response: {
                                success: function(args) {
                                    // Cleanup
                                    $step.find('.main-desc, p.no-affinity-groups').remove();

                                    if (args.data.affinityGroups && args.data.affinityGroups.length) {
                                        $step.prepend(
                                            $('<div>').addClass('main-desc').append(
                                                $('<p>').html(_l('message.select.affinity.groups'))
                                            )
                                        );
                                        $step.find('.select-container').append(
                                            makeSelects(
                                                'affinity-groups',
                                                args.data.affinityGroups, {
                                                    name: 'name',
                                                    desc: 'description',
                                                    id: 'id'
                                                }, {
                                                    type: 'checkbox',
                                                    'wizard-field': 'affinity-groups'
                                                },
                                                args.data.selectedObj,
                                                args.data.selectedObjNonEditable
                                            )
                                        );
                                    } else {
                                        $step.find('.select-container').append(
                                            $('<p>').addClass('no-affinity-groups').html(_l('message.no.affinity.groups'))
                                        );
                                    }
                                }
                            }
                        };
                    },

                    'sshkeyPairs': function($step, formData) {
                        var originalValues = function(formData) {
                            if (formData.sshkeypair) {
                                $step.find('input[type=radio]').filter(function() {
                                    return $(this).val() == formData.sshkeypair;
                                }).click();
                            } else {
                                $step.find('input[type=radio]:first').click();
                            }
                        };
                        return {
                            response: {
                                success: function(args) {
                                    $step.find('.main-desc, p.no-sshkey-pairs').remove();

                                    if (args.data.sshkeyPairs && args.data.sshkeyPairs.length) {
                                        $step.prepend(
                                            $('<div>').addClass('main-desc').append(
                                                $('<p>').html(_l('message.please.select.ssh.key.pair.use.with.this.vm'))
                                            )
                                        );
                                        $step.find('.section.no-thanks').show();
                                        $step.find('.select-container').append(
                                            makeSelects(
                                                'sshkeypair',
                                                args.data.sshkeyPairs, {
                                                    name: 'name',
                                                    id: 'name'
                                                }, {
                                                    'wizard-field': 'sshkey-pairs'
                                                }
                                            )
                                        );
                                        originalValues(formData); // if we can select only one.
                                    } else {
                                        $step.find('.section.no-thanks').hide();
                                        $step.find('.select-container').append(
                                            $('<p>').addClass('no-sshkey-pairs').html(_l('You do not have any ssh key pairs. Please continue to the next step.'))
                                        );
                                    }
                                }
                            }
                        };
                    },

                    'network': function($step, formData) {
                        var showAddNetwork = true;

                        var checkShowAddNetwork = function($newNetwork) {
                            if (!showAddNetwork) {
                                $newNetwork.hide();
                            } else {
                                $newNetwork.show();
                            }
                        };

                        var originalValues = function(formData) {
                            // Default networks
                            $step.find('input[type=radio]').filter(function() {
                                return $(this).val() == formData['defaultNetwork'];
                            }).click();

                            // Optional networks
                            var selectedOptionalNetworks = [];

                            if ($.isArray(formData['shared-networks']) != -1) {
                                $(formData['shared-networks']).each(function() {
                                    selectedOptionalNetworks.push(this);
                                });
                            } else {
                                selectedOptionalNetworks.push(formData['shared-networks']);
                            }

                            var $checkboxes = $step.find('input[name=shared-networks]');
                            $(selectedOptionalNetworks).each(function() {
                                var networkID = this;
                                $checkboxes.filter(function() {
                                    return $(this).val() == networkID;
                                }).attr('checked', 'checked');
                            });
                        };

                        var $newNetwork = $step.find('.new-network');
                        var $newNetworkCheckbox = $newNetwork.find('input[type=checkbox]');

                        // Setup new network field
                        $newNetworkCheckbox.unbind('click');
                        $newNetworkCheckbox.click(function() {
                            $newNetwork.toggleClass('unselected');

                            // Select another default if hiding field
                            if ($newNetwork.hasClass('unselected')) {
                                $step.find('input[type=radio]:visible:first').click();
                            } else {
                                $newNetwork.find('input[type=radio]').click();
                            }
                        });

                        setTimeout(function() {
                            var $checkbox = $step.find('.new-network input[type=checkbox]');
                            var $newNetwork = $checkbox.closest('.new-network');

                            if ($step.find('.select.my-networks .select-container .select:visible').size()) {
                                $checkbox.attr('checked', false);
                                $newNetwork.addClass('unselected');
                            } else {
                                $newNetwork.find('input[name=defaultNetwork]').filter('[value=new-network]').click();
                            }

                            $checkbox.change();
                        });

                        // Show relevant conditional sub-step if present
                        $step.find('.wizard-step-conditional').hide();

                        if ($.isFunction(args.showAddNetwork)) {
                            showAddNetwork = args.showAddNetwork({
                                data: formData,
                                context: context
                            });
                        }

                        // Filter network list by VPC ID
                        var filterNetworkList = function(vpcID) {
                            var $selects = $step.find('.my-networks .select-container .select');
                            var $visibleSelects = $($.grep($selects, function(select) {
                                var $select = $(select);

                                return args.vpcFilter($select.data('json-obj'), vpcID);
                            }));
                            var $addNetworkForm = $step.find('.select.new-network');
                            var $addNewNetworkCheck = $addNetworkForm.find('input[name=new-network]');

                            // VPC networks cannot be created via instance wizard
                            if (vpcID != -1) {
                                $step.find('.my-networks .select-container').addClass('single-select');
                                $addNetworkForm.hide();

                                if ($addNewNetworkCheck.is(':checked')) {
                                    $addNewNetworkCheck.click();
                                    $addNewNetworkCheck.attr('checked', false);
                                }
                            } else {
                                $step.find('.my-networks .select-container').removeClass('single-select');
                                $addNetworkForm.show();
                                checkShowAddNetwork($addNetworkForm);
                            }

                            $selects.find('input[type=checkbox]').attr('checked', false);
                            $selects.hide();
                            $visibleSelects.show();

                            // Select first visible item by default
                            $visibleSelects.filter(':first')
                                .find('input[type=radio]')
                                .click();

                            cloudStack.evenOdd($visibleSelects, 'div.select', {
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

                        var $vpcSelect = $step.find('select[name=vpc-filter]');

                        $vpcSelect.unbind('change');
                        $vpcSelect.change(function() {
                            filterNetworkList($vpcSelect.val());
                        });

                        return {
                            response: {
                                success: function(args) {
                                    var vpcs = args.data.vpcs;
                                    var addClass = args.addClass;
                                    var removeClass = args.removeClass;

                                    // Populate VPC drop-down
                                    $vpcSelect.html('');
                                    $(vpcs).map(function(index, vpc) {
                                        var $option = $('<option>');
                                        var id = vpc.id;
                                        var description = vpc.name;

                                        $option.attr('value', id);
                                        $option.html(description);
                                        $option.appendTo($vpcSelect);
                                    });

                                    // 'No VPC' option
                                    $('<option>').attr('value', '-1').html(_l('ui.listView.filters.all')).prependTo($vpcSelect);

                                    $vpcSelect.val(-1);

                                    // Populate network offering drop-down
                                    $(args.data.networkOfferings).each(function() {
                                        $('<option>')
                                            .val(this.id)
                                            .html(this.name)
                                            .appendTo($newNetwork.find('select'));
                                    });

                                    if (args.type) {
                                        $step.find('.wizard-step-conditional').filter(function() {
                                            return $(this).hasClass(args.type);
                                        }).show();
                                    } else {
                                        $step.find('.select-network').show();
                                    }

                                    // My networks
                                    $step.find('.my-networks .select-container').append(
                                        makeSelects('my-networks', args.data.networkObjs, {
                                            name: 'name',
                                            desc: 'type',
                                            id: 'id'
                                        }, {
                                            type: 'checkbox',
                                            'wizard-field': 'my-networks',
                                            secondary: {
                                                desc: 'Default',
                                                name: 'defaultNetwork',
                                                type: 'radio',
                                                'wizard-field': 'default-network'
                                            }
                                        })
                                    );

                                    // Add IP/advanced option fields
                                    $step.find('.my-networks .select-container .select, .select.new-network .select').each(function () {
                                        var $select = $(this);
                                        var $advancedLink = $('<div>').addClass('advanced-options hide-if-unselected');
                                        var $specifyIpField = $('<div>').addClass('specify-ip hide-if-unselected').append(
                                            $('<label>').html(_l('label.ip.address')),
                                            $('<input>').attr({ type: 'text' })
                                        );

                                        // Cleanup
                                        if ($select.closest('.new-network').size()) {
                                            $select.find('.advanced-options, .specify-ip').remove();
                                        }

                                        $select.append($advancedLink, $specifyIpField);
                                        $advancedLink.click(function() {
                                            $select.toggleClass('advanced');
                                        });
                                    });

                                    // Show non-VPC networks by default
                                    filterNetworkList(-1);

                                    // Security groups (alt. page)
                                    var $sgSelects = makeSelects('security-groups', args.data.securityGroups, {
                                        name: 'name',
                                        desc: 'description',
                                        id: 'id'
                                    }, {
                                        type: 'checkbox',
                                        'wizard-field': 'security-groups'
                                    });
                                    $step.find('.security-groups .select-container').append($sgSelects);

                                    //If there is only one security group and the only one is 'default', make it selected by default
                                    if ($sgSelects.length == 1) {
                                        var $firstCheckbox = $sgSelects.eq(0);
                                      if ($firstCheckbox.find('div .name').text() == 'default') {
                                        $firstCheckbox.find('input:checkbox').click();
                                      }
                                  }

                                    originalValues(formData);
                                    checkShowAddNetwork($newNetwork);
                                }
                            }
                        };
                    },

                    'review': function($step, formData) {
                        $step.find('[wizard-field]').each(function() {
                            var field = $(this).attr('wizard-field');
                            var fieldName;
                            var $input = $wizard.find('[wizard-field=' + field + ']').filter(function() {
                                return ($(this).is(':selected') ||
                                    $(this).is(':checked') ||
                                    $(this).attr('type') == 'hidden') &&
                                    $(this).is(':not(:disabled)');
                            });

                            if ($input.is('option')) {
                                fieldName = $input.html();
                            } else if ($input.is('input[type=radio]')) {
                                // Choosen New network as default
                                if ($input.parents('div.new-network').size()) {
                                    fieldName = $input.closest('div.new-network').find('input[name="new-network-name"]').val();
                                    // Choosen Network from existed
                                } else if ($input.parents('div.my-networks').size()) {
                                    fieldName = $input.closest('div.select').find('.select-desc .name').html();
                                } else {
                                    fieldName = $input.parent().find('.select-desc .name').html();
                                }
                            } else if ($input.eq(0).is('input[type=checkbox]')) {
                                fieldName = '';
                                $input.each(function(index) {
                                    if (index != 0) fieldName += '<br />';
                                    fieldName += $(this).next('div.select-desc').find('.name').html();
                                });
                            } else if ($input.is('input[type=hidden]')) {
                                fieldName = $input.val();
                            }

                            if (fieldName) {
                                $(this).html(fieldName);
                            } else {
                                $(this).html('(' + _l('label.none') + ')');
                            }

                            var conditionalFieldFrom = $(this).attr('conditional-field');
                            if (conditionalFieldFrom) {
                                if ($wizard.find('.' + conditionalFieldFrom).css('display') == 'block') {
                                    $(this).closest('div.select').show();
                                } else {
                                    $(this).closest('div.select').hide();
                                }
                            }
                        });
                    }
                };

                $wizard.bind('cloudStack.instanceWizard.showStep', function(e, args) {
                    showStep(args.index, { refresh: true });
                });

                // Go to specified step in wizard,
                // updating nav items and diagram
                var showStep = function(index, options) {
                    if (!options) options = {};
                    var targetIndex = index - 1;

                    if (index <= 1) targetIndex = 0;
                    if (targetIndex == $steps.size()) {
                        completeAction();
                        return;
                    }

                    var $targetStep = $($steps.hide()[targetIndex]).show();
                    var stepID = $targetStep.attr('wizard-step-id');
                    var formData = cloudStack.serializeForm($form);

                    if (options.refresh) {
                        $targetStep.removeClass('loaded');
                    }

                    if (!$targetStep.hasClass('loaded')) {
                        // Remove previous content
                        if (!$targetStep.hasClass('review')) { // Review row content is not autogenerated
                            $targetStep.find('.select-container:not(.fixed) div, option:not(:disabled)').remove();
                        }

                        dataProvider(
                            index,
                            dataGenerators[stepID](
                                $targetStep,
                                formData
                            )
                        );

                        if (!$targetStep.hasClass('repeat') && !$targetStep.hasClass('always-load')) $targetStep.addClass('loaded');
                    }

                    // Show launch vm button if last step
                    var $nextButton = $wizard.find('.button.next');
                    $nextButton.find('span').html(_l('label.next'));
                    $nextButton.removeClass('final');
                    if ($targetStep.hasClass('review')) {
                        $nextButton.find('span').html(_l('label.launch.vm'));
                        $nextButton.addClass('final');
                    }

                    // Hide previous button on first step
                    var $previousButton = $wizard.find('.button.previous');
                    if (index == 1) $previousButton.hide();
                    else $previousButton.show();

                    // Update progress bar
                    var $targetProgress = $progress.removeClass('active').filter(function() {
                        return $(this).index() <= targetIndex;
                    }).toggleClass('active');

                    // Update diagram; show/hide as necessary
                    $diagramParts.filter(function() {
                        return $(this).index() <= targetIndex;
                    }).fadeIn('slow');
                    $diagramParts.filter(function() {
                        return $(this).index() > targetIndex;
                    }).fadeOut('slow');

                    setTimeout(function() {
                        if (!$targetStep.find('input[type=radio]:checked').size()) {
                            $targetStep.find('input[type=radio]:first').click();
                        }
                    }, 50);
                };

                // Events
                $wizard.click(function(event) {
                    var $target = $(event.target);
                    var $activeStep = $form.find('.step:visible');

                    // Next button
                    if ($target.closest('div.button.next').size()) {
                        //step 2 - select template/ISO
                        if($activeStep.hasClass('select-iso')) {
                            if ($activeStep.find('.content:visible input:checked').size() == 0) {
                                cloudStack.dialog.notice({
                                    message: 'message.step.1.continue'
                                });
                                return false;
                            }
                            $(window).trigger("cloudStack.module.instanceWizard.clickNextButton", {
                                $form: $form,
                                currentStep: 2
                            });
                        }

                        //step 6 - select network
                        if ($activeStep.find('.wizard-step-conditional.select-network:visible').size() > 0) {
                            var data = $activeStep.data('my-networks');

                            if (!data) {
                                $activeStep.closest('form').data('my-networks', cloudStack.serializeForm(
                                    $activeStep.closest('form')
                                )['my-networks']);
                            }

                            if ($activeStep.find('input[type=checkbox]:checked').size() == 0) { //if no checkbox is checked
                                cloudStack.dialog.notice({
                                    message: 'message.step.4.continue'
                                });
                                return false;
                            }

                            if ($activeStep.hasClass('next-use-security-groups')) {
                                var advSGFilter = args.advSGFilter({
                                    data: cloudStack.serializeForm($form)
                                });

                                if (advSGFilter == 0) { //when total number of selected sg networks is 0, then 'Select Security Group' is skipped, go to step 6 directly
                                    showStep(6);
                                } else { //when total number of selected sg networks > 0
                                    if ($activeStep.find('input[type=checkbox]:checked').size() > 1) { //when total number of selected networks > 1
                                        cloudStack.dialog.notice({
                                            message: "Can't create a vm with multiple networks one of which is Security Group enabled"
                                        });
                                        return false;
                                    }
                                }
                            }
                        }

                        //step 6 - review (spcifiy displyname, group as well)
                        if ($activeStep.hasClass('review')) {
                            if ($activeStep.find('input[name=displayname]').size() > 0 && $activeStep.find('input[name=displayname]').val().length > 0) {
                                //validate
                                var b = cloudStack.validate.vmHostName($activeStep.find('input[name=displayname]').val());
                                if (b == false)
                                    return false;
                            }
                        }

                        if (!$form.valid()) {
                            if ($form.find('input.error:visible, select.error:visible').size()) {
                                return false;
                            }
                        }

                        if ($activeStep.hasClass('repeat')) {
                            showStep($steps.filter(':visible').index() + 1);
                        } else {
                            showStep($steps.filter(':visible').index() + 2);
                        }

                        return false;
                    }

                    // Previous button
                    if ($target.closest('div.button.previous').size()) {
                        var $step = $steps.filter(':visible');
                        var $networkStep = $steps.filter('.network');
                        var index = $step.index();

                        $networkStep.removeClass('next-use-security-groups');

                        if (index) {
                            if (index == $steps.size() - 1 && $networkStep.hasClass('next-use-security-groups')) {
                                showStep(5);
                            } else if ($activeStep.find('.select-security-group:visible').size() &&
                                $activeStep.find('.select-network.no-add-network').size()) {
                                showStep(5);
                            } else {
                                showStep(index);
                            }
                        }

                        return false;
                    }

                    // Close button
                    if ($target.closest('div.button.cancel').size()) {
                        close();

                        return false;
                    }

                    // Edit link
                    if ($target.closest('div.edit').size()) {
                        var $edit = $target.closest('div.edit');

                        showStep($edit.find('a').attr('href'));

                        return false;
                    }

                    return true;
                });

                showStep(1);

                $wizard.bind('change', function(event) {
                    var $target = $(event.target);
                    var $step = $target.closest('.step');
                    var $futureSteps = $step.siblings().filter(function() {
                        return $(this).index() > $step.index();
                    });

                    // Reset all fields in futher steps
                    $futureSteps.removeClass('loaded');
                });

                var minCustomDiskSize = args.minDiskOfferingSize ?
                                    args.minDiskOfferingSize() : 1;

                var maxCustomDiskSize = args.maxDiskOfferingSize ?
                    args.maxDiskOfferingSize() : 100;

                // Setup tabs and slider
                $wizard.find('.section.custom-size.custom-disk-size .size.min span').html(minCustomDiskSize);
                $wizard.find('.section.custom-size.custom-disk-size input[type=text]').val(minCustomDiskSize);
                $wizard.find('.section.custom-size.custom-disk-size .size.max span').html(maxCustomDiskSize);
                $wizard.find('.tab-view').tabs();
                $wizard.find('.slider').each(function() {
                   var $slider = $(this);

                    $slider.slider({
                        min: minCustomDiskSize,
                        max: maxCustomDiskSize,
                        start: function(event) {
                            $slider.closest('.section.custom-size').find('input[type=radio]').click();
                        },
                        slide: function(event, ui) {
                            $slider.closest('.section.custom-size').find('input[type=text]').val(
                                ui.value
                            );
                            $slider.closest('.step').find('span.custom-disk-size').html(
                                ui.value
                            );
                        }
                    });
                });

                $wizard.find('div.data-disk-offering div.custom-size input[type=text]').bind('change', function() {
                    var old = $wizard.find('div.data-disk-offering div.custom-size input[type=text]').val();
                    $wizard.find('div.data-disk-offering span.custom-disk-size').html(_s(old));
                });


                return $wizard.dialog({
                    title: _l('label.vm.add'),
                    width: 896,
                    height: 570,
                    closeOnEscape: false,
                    zIndex: 5000
                })
                    .closest('.ui-dialog').overlay();
            };

            instanceWizard(args);
        };
    };
})(jQuery, cloudStack);
