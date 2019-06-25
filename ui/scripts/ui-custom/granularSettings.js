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
    cloudStack.uiCustom.granularSettings = function(args) {
        var dataProvider = args.dataProvider;
        var actions = args.actions;

        return function(args) {
            var context = args.context;

            var listView = {
                id: 'settings',
                fields: {
                    name: {
                        label: 'label.name'
                    },
                    description: {
                        label: 'label.description'
                    },
                    value: {
                        label: 'label.value',
                        editable: true
                    }
                },
                actions: {
                    edit: {
                        label: 'label.change.value',
                        action: actions.edit
                    }
                },
                dataProvider: dataProvider
            };

            var $listView = $('<div>').listView({
                context: context,
                listView: listView
            });

            return $listView;
        }
    };
    cloudStack.uiCustom.granularDetails = function(args) {
        var dataProvider = args.dataProvider;
        var actions = args.actions;
        var resourceType = args.resourceType;

        return function(args) {
            var context = args.context;

            var listView = {
                id: 'details',
                fields: {
                    name: {
                        label: 'label.name'
                    },
                    value: {
                        label: 'label.value',
                        editable: true
                    }
                },
                actions: {
                    edit: {
                        label: 'label.change.value',
                        action: actions.edit
                    },
                    remove: {
                        label: 'Remove Setting',
                        messages: {
                            confirm: function(args) {
                                return 'Delete Setting';
                            },
                            notification: function(args) {
                                return 'Setting deleted';
                            }
                        },
                        action: actions.remove,
                        notification: {
                            poll: function(args) {
                                args.complete();
                            }
                        }
                    },
                    add : {
                        label: 'Add Setting',
                        messages: {
                            confirm: function(args) {
                                return 'Add Setting';
                            },
                            notification: function(args) {
                                return 'Setting added';
                            }
                        },
                        preFilter: function(args) {
                            return true;
                        },
                        createForm: {
                            title: 'Add New Setting',
                            preFilter: function(args) {
                                var data = {
                                    resourcetype: resourceType
                                };
                                if (resourceType === 'UserVm') {
                                    data.resourceid = args.context.instances[0].id;
                                }
                                if (resourceType === 'Template') {
                                    data.resourceid = args.context.templates[0].id;
                                }

                                $.ajax({
                                    url: createURL("listDetailOptions"),
                                    data: data,
                                    dataType: "json",
                                    success: function(json) {
                                        var details = json.listdetailoptionsresponse.detailoptions.details;
                                        var keys = [];
                                        Object.keys(details).forEach(function(key,index) {
                                            keys.push(key);
                                        });
                                        $(args.$form.find('input[name="name"]')).blur();
                                        $(args.$form.find('input[name="name"]')).autocomplete({
                                            minLength: 0,
                                            delay: 0,
                                            source: keys.sort(),
                                            select: function(event, ui) {
                                                const key = ui.item.value;
                                                const options = details[key];
                                                $(args.$form.find('input[name="value"]')).autocomplete({
                                                    minLength: 0,
                                                    delay: 0,
                                                    autoFocus: true,
                                                    source: options.sort()
                                                }).focus(function() {
                                                    $(this).data("uiAutocomplete").search($(this).val());
                                                });
                                            }
                                        }).focus(function() {
                                            $(this).data("uiAutocomplete").search($(this).val());
                                        });
                                    }
                                });

                                return true;
                            },
                            fields: {
                                name: {
                                    label: 'label.name',
                                    validation: {
                                        required: true
                                    }
                                },
                                value: {
                                    label: 'label.value',
                                    validation: {
                                        required: true
                                    }
                                }
                            }
                        },
                        action: actions.add
                    }
                },
                dataProvider: function(args) {
                    var data = {
                        resourcetype: resourceType
                    };
                    if (resourceType === 'UserVm') {
                        data.resourceid = args.context.instances[0].id;
                    }
                    if (resourceType === 'Template') {
                        data.resourceid = args.context.templates[0].id;
                    }

                    $.ajax({
                        url: createURL("listDetailOptions"),
                        data: data,
                        dataType: "json",
                        success: function(json) {
                            var details = json.listdetailoptionsresponse.detailoptions.details;
                            var tbody = $.find('#details-tab-settings .data-table tbody');
                            $(tbody).on('DOMNodeInserted', "tr", function() {
                                $.each($.find('#details-tab-settings .data-table tbody tr'), function(idx, row) {
                                    const key = $(row).find('td.name').attr('title');
                                    const options = details[key];
                                    if (options) {
                                        $($(row).find('input.edit')).autocomplete({
                                            minLength: 0,
                                            delay: 0,
                                            autoFocus: true,
                                            source: options.sort()
                                        }).focus(function() {
                                            $(this).data("uiAutocomplete").search("");
                                        });
                                        $(row).find('input.edit').blur();
                                    }
                                });
                            });
                            dataProvider(args);
                        }
                    });
                }
            };

            var $listView = $('<div>').listView({
                context: context,
                listView: listView
            });

            return $listView;
        }
    };
}(jQuery, cloudStack));
