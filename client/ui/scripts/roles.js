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
    var apiList = [];
    var rolePermissions = [];
    cloudStack.sections.roles = {
        title: 'label.roles',
        id: 'roles',
        listView: {
            id: 'roles',
            fields: {
                name: {
                    label: 'label.name'
                },
                type: {
                    label: 'label.type'
                },
                description: {
                    label: 'label.description'
                }
            },
            disableInfiniteScrolling: true,
            dataProvider: function(args) {
                var data = {};
                if (args.filterBy.search && args.filterBy.search.value) {
                    data['name'] = args.filterBy.search.value;
                }
                $.ajax({
                    url: createURL("listRoles"),
                    data: data,
                    dataType: "json",
                    async: true,
                    success: function(json) {
                        var jsonObj;
                        jsonObj = json.listrolesresponse.role;
                        args.response.success({
                            data: jsonObj
                        });
                    }
                });
            },
            actions: {
                add: {
                    label: 'label.add.role',
                    preFilter: function(args) {
                        if (isAdmin())
                            return true;
                    },
                    messages: {
                        notification: function() {
                            return 'label.add.role';
                        }
                    },
                    createForm: {
                        title: 'label.add.role',
                        fields: {
                            name: {
                                label: 'label.name',
                                validation: {
                                    required: true
                                }
                            },
                            description: {
                                label: 'label.description',
                            },
                            type: {
                                label: 'label.type',
                                validation: {
                                    required: true
                                },
                                select: function(args) {
                                    var items = [];
                                    items.push({
                                        id: "Admin",
                                        description: "Admin"
                                    });
                                    items.push({
                                        id: "DomainAdmin",
                                        description: "Domain Admin"
                                    });
                                    items.push({
                                        id: "User",
                                        description: "User"
                                    });
                                    args.response.success({
                                        data: items
                                    });
                                }
                            }
                        }
                    },
                    action: function(args) {
                        $.ajax({
                            url: createURL('createRole'),
                            data: args.data,
                            success: function(json) {
                                var item = json.createroleresponse.role;
                                args.response.success({
                                    data: item
                                });
                            },
                            error: function(json) {
                                args.response.error(parseXMLHttpResponse(json));
                            }
                        });
                    },
                    notification: {
                        poll: function(args) {
                            args.complete();
                        }
                    }
                }
            },
            detailView: {
                tabs: {
                    details: {
                        title: 'label.details',
                        fields: {
                            id: {
                                label: 'label.id'
                            },
                            name: {
                                label: 'label.name',
                                isEditable: true,
                                validation: {
                                    required: true
                                }
                            },
                            type: {
                                label: 'label.type'
                            },
                            description: {
                                label: 'label.description',
                                isEditable: true
                            }
                        },
                        dataProvider: function(args) {
                            $.ajax({
                                url: createURL("listRoles&id=" + args.context.roles[0].id),
                                dataType: "json",
                                async: true,
                                success: function(json) {
                                    var response = json.listrolesresponse.role[0];
                                    args.response.success({
                                        data: response
                                    });
                                }
                            });
                        }
                    },
                    rules: {
                        title: 'label.rules',
                        custom: function(args) {
                            var context = args.context;

                            return $('<div>').multiEdit({
                                context: context,
                                noSelect: true,
                                noHeaderActionsColumn: true,
                                selectPermission: {
                                    action: function(args){
                                        $.ajax({
                                            url: createURL("updateRolePermission"),
                                            data: {
                                                roleid: args.roleid,
                                                ruleid: args.ruleid,
                                                permission: args.permission
                                            },
                                            dataType: "json",
                                            async: true,
                                            success: function(json) {
                                                $(window).trigger('cloudStack.fullRefresh');
                                            },
                                            error: function(json) {
                                                cloudStack.dialog.notice({
                                                    message: 'message.role.update.fail'
                                                });
                                            }
                                        });
                                    }
                                },
                                reorder: {
                                    moveDrag: {
                                        action: function(args) {
                                            var rule = args.context.multiRule[0];
                                            var prevItemId = args.prevItem ? args.prevItem.id : 0;

                                            var ruleOrder = [];
                                            $.each(rolePermissions, function(idx, item) {
                                                var itemId = item.id;
                                                if (idx == 0 && prevItemId == 0) {
                                                    ruleOrder.push(rule.id);
                                                }
                                                if (itemId == rule.id) {
                                                    return true;
                                                }
                                                ruleOrder.push(item.id);
                                                if (prevItemId == itemId) {
                                                    ruleOrder.push(rule.id);
                                                }
                                            });

                                            $.ajax({
                                            	type: 'POST',
                                                url: createURL('updateRolePermission'),
                                                data: {
                                                    roleid: rule.roleid,
                                                    ruleorder: ruleOrder.join()
                                                },
                                                success: function(json) {
                                                    args.response.success();
                                                    $(window).trigger('cloudStack.fullRefresh');
                                                },
                                                error: function(json) {
                                                    cloudStack.dialog.notice({
                                                        message: 'message.role.ordering.fail'
                                                    });
                                                }
                                            });
                                        }
                                    }
                                },
                                fields: {
                                    'rule': {
                                        edit: true,
                                        label: 'label.rule',
                                        isOptional: false
                                    },
                                    'permission': {
                                        label: 'label.permission',
                                        select: function(args) {
                                            args.response.success({
                                                data: [{
                                                    name: 'allow',
                                                    description: 'Allow'
                                                }, {
                                                    name: 'deny',
                                                    description: 'Deny'
                                                }]
                                            });
                                        }
                                    },
                                    'description': {
                                        edit: true,
                                        label: 'label.description',
                                        isOptional: true
                                    },
                                    'always-hide': {
                                        label: 'label.action',
                                        addButton: true
                                    }
                                },
                                add: {
                                    label: 'label.add',
                                    action: function(args) {
                                        var data = {
                                            rule: args.data.rule,
                                            permission: args.data.permission,
                                            description: args.data.description,
                                            roleid: args.context.roles[0].id
                                        };

                                        $.ajax({
                                            url: createURL('createRolePermission'),
                                            data: data,
                                            dataType: 'json',
                                            success: function(json) {
                                                var response = json.createrolepermissionresponse.rolepermission;
                                                args.response.success({
                                                    data: response
                                                });
                                            },
                                            error: function(json) {
                                                args.response.error(parseXMLHttpResponse(json));
                                            }
                                        });
                                    }
                                },
                                actions: {
                                    destroy: {
                                        label: 'label.remove.rule',
                                        action: function(args) {
                                            $.ajax({
                                                url: createURL('deleteRolePermission'),
                                                data: {
                                                    id: args.context.multiRule[0].id
                                                },
                                                dataType: 'json',
                                                success: function(data) {
                                                    args.response.success();
                                                },
                                                error: function(json) {
                                                    args.response.error(parseXMLHttpResponse(json));
                                                }
                                            });
                                        }
                                    }
                                },
                                dataProvider: function(args) {
                                    $.ajax({
                                        url: createURL('listRolePermissions'),
                                        data: {
                                            roleid: args.context.roles[0].id
                                        },
                                        dataType: 'json',
                                        success: function(json) {
                                            var rules = json.listrolepermissionsresponse.rolepermission;
                                            if (rules) {
                                                rolePermissions = rules;
                                            }
                                            args.response.success({
                                                data: rules
                                            });
                                        }
                                    });
                                    var setupAutocompletion = function() {
                                        var $target = $($.find('input[name="rule"]'));
                                        if ($target.hasClass('ui-autocomplete')) {
                                            $target.autocomplete('destroy');
                                        }
                                        $($.find('input[name="rule"]')).autocomplete({
                                            source: apiList,
                                            autoFocus:true
                                        });
                                    };
                                    if (apiList.length == 0) {
                                        $.ajax({
                                            url: createURL("listApis"),
                                            dataType: "json",
                                            success: function(json) {
                                                var response = json.listapisresponse.api;
                                                $.each(response, function(idx, api) {
                                                    apiList.push(api.name);
                                                });
                                                setupAutocompletion();
                                            }
                                        });
                                    } else {
                                        setupAutocompletion();
                                    }
                                }
                            });
                        }
                    }
                },
                actions: {
                    edit: {
                        label: 'label.edit.role',
                        action: function(args) {
                            var data = {
                                id: args.context.roles[0].id,
                                name: args.data.name,
                                description: args.data.description
                            };

                            $.ajax({
                                url: createURL('updateRole'),
                                data: data,
                                success: function(json) {
                                    args.response.success();
                                },
                                error: function(json) {
                                    args.response.error(parseXMLHttpResponse(json));
                                }
                            });
                        }
                    },
                    remove: {
                        label: 'label.delete.role',
                        messages: {
                            confirm: function(args) {
                                return 'label.delete.role';
                            },
                            notification: function(args) {
                                return 'label.delete.role';
                            }
                        },
                        action: function(args) {
                            $.ajax({
                                url: createURL("deleteRole&id=" + args.context.roles[0].id),
                                dataType: "json",
                                success: function(json) {
                                    var response = json.deleteroleresponse;
                                    args.response.success({
                                        data: response
                                    });
                                }
                            });
                        },
                        notification: {
                            poll: function(args) {
                                args.complete();
                            }
                        }
                    }
                }
            }
        }
    }
})(jQuery, cloudStack);
