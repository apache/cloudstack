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

(function(cloudStack) {
    cloudStack.sections['global-settings'] = {
        title: 'label.menu.global.settings',
        id: 'global-settings',
        sectionSelect: {
            label: 'label.select-view'
        },
        sections: {
            globalSettings: {
                type: 'select',
                title: 'label.menu.global.settings',
                listView: {
                    label: 'label.menu.global.settings',
                    actions: {
                        edit: {
                            label: 'label.change.value',
                            action: function(args) {
                                var data = {
                                    name: args.data.jsonObj.name,
                                    value: args.data.value
                                };
                                $.ajax({
                                    url: createURL('updateConfiguration'),
                                    data: data,
                                    success: function(json) {
                                        var item = json.updateconfigurationresponse.configuration;
                                        if (item.category == "Usage")
                                            cloudStack.dialog.notice({
                                                message: _l('message.restart.mgmt.usage.server')
                                            });
                                        else
                                            cloudStack.dialog.notice({
                                                message: _l('message.restart.mgmt.server')
                                            });
                                        args.response.success({
                                            data: item
                                        });
                                    },
                                    error: function(json) {
                                        args.response.error(parseXMLHttpResponse(json));
                                    }
                                });
                            }
                        }
                    },
                    fields: {
                        name: {
                            label: 'label.name',
                            id: true
                        },
                        description: {
                            label: 'label.description'
                        },
                        value: {
                            label: 'label.value',
                            editable: true,
                            truncate: true
                        }
                    },
                    dataProvider: function(args) {
                        var data = {
                            page: args.page,
                            pagesize: pageSize
                        };

                        if (args.filterBy.search.value) {
                            data.name = args.filterBy.search.value;
                        }

                        $.ajax({
                            url: createURL('listConfigurations'),
                            data: data,
                            dataType: "json",
                            async: true,
                            success: function(json) {
                                var items = json.listconfigurationsresponse.configuration;
                                args.response.success({
                                    data: items
                                });
                            }
                        });
                    }
                }
            },
            ldapConfiguration: {
                type: 'select',
                title: 'LDAP Configuration',
                listView: {
                    id: 'ldap',
                    label: 'LDAP Configuration',
                    fields: {
                        hostname: {
                            label: 'Hostname'
                        },
                        port: {
                            label: 'LDAP Port'
                        }
                    },
                    dataProvider: function(args) {
                        var data = {};
                        listViewDataProvider(args, data);
                        $.ajax({
                            url: createURL('listLdapConfigurations'),
                            data: data,
                            success: function(json) {
                                var items = json.ldapconfigurationresponse.LdapConfiguration;
                                args.response.success({
                                    data: items
                                });
                            },
                            error: function(data) {
                                args.response.error(parseXMLHttpResponse(data));
                            }
                        });
                    },
                    detailView: {
                        name: 'label.details',
                        actions: {
                            remove: {
                                label: 'Remove LDAP',
                                messages: {
                                    notification: function(args) {
                                        return 'LDAP Configuration Deleted';
                                    },
                                    confirm: function() {
                                        return 'Are you sure you want to delete the LDAP configuration?';
                                    }
                                },
                                action: function(args) {
                                    $.ajax({
                                        url: createURL("deleteLdapConfiguration&hostname=" + args.context.ldapConfiguration[0].hostname),
                                        success: function(json) {
                                            args.response.success();
                                        }
                                    });
                                    $(window).trigger('cloudStack.fullRefresh');
                                }
                            }
                        },
                        tabs: {
                            details: {
                                title: 'LDAP Configuration Details',
                                fields: [{
                                    hostname: {
                                        label: 'Hostname'
                                    },
                                    port: {
                                        label: 'Port'
                                    }
                                }],
                                dataProvider: function(args) {
                                    var items = [];
                                    console.log(args);
                                    $.ajax({
                                        url: createURL("listLdapConfigurations&hostname=" + args.context.ldapConfiguration[0].hostname),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var item = json.ldapconfigurationresponse.LdapConfiguration;
                                            args.response.success({
                                                data: item[0]
                                            });
                                        }
                                    });
                                }
                            }
                        }
                    },
                    actions: {
                        add: {
                            label: 'Configure LDAP',
                            messages: {
                                confirm: function(args) {
                                    return 'Do you really want to configure LDAP ? ';
                                },
                                notification: function(args) {
                                    console.log(args);
                                    return 'Successfully added a new LDAP server';
                                }
                            },
                            createForm: {
                                title: 'Configure LDAP',
                                fields: {
                                    hostname: {
                                        label: 'Hostname',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    port: {
                                        label: 'Port',
                                        validation: {
                                            required: true
                                        }
                                    }
                                }
                            },
                            action: function(args) {
                                var array = [];
                                array.push("&hostname=" + todb(args.data.hostname));
                                array.push("&port=" + todb(args.data.port));;
                                $.ajax({
                                    url: createURL("addLdapConfiguration" + array.join("")),
                                    dataType: "json",
                                    async: true,
                                    success: function(json) {
                                        var items = json.ldapconfigurationresponse.LdapAddConfiguration;
                                        args.response.success({
                                            data: items
                                        });
                                    },
                                    error: function(json) {
                                        args.response.error(parseXMLHttpResponse(json));
                                    }
                                });
                            }
                        }
                    }
                }
            },
            hypervisorCapabilities: {
                type: 'select',
                title: 'label.hypervisor.capabilities',
                listView: {
                    id: 'hypervisorCapabilities',
                    label: 'label.hypervisor.capabilities',
                    fields: {
                        hypervisor: {
                            label: 'label.hypervisor'
                        },
                        hypervisorversion: {
                            label: 'label.hypervisor.version'
                        },
                        maxguestslimit: {
                            label: 'label.max.guest.limit'
                        }
                    },
                    dataProvider: function(args) {
                        var data = {};
                        listViewDataProvider(args, data);

                        $.ajax({
                            url: createURL('listHypervisorCapabilities'),
                            data: data,
                            success: function(json) {
                                var items = json.listhypervisorcapabilitiesresponse.hypervisorCapabilities;
                                args.response.success({
                                    data: items
                                });
                            },
                            error: function(data) {
                                args.response.error(parseXMLHttpResponse(data));
                            }
                        });
                    },

                    detailView: {
                        name: 'label.details',
                        actions: {
                            edit: {
                                label: 'label.edit',
                                action: function(args) {
                                    var data = {
                                        id: args.context.hypervisorCapabilities[0].id,
                                        maxguestslimit: args.data.maxguestslimit
                                    };

                                    $.ajax({
                                        url: createURL('updateHypervisorCapabilities'),
                                        data: data,
                                        success: function(json) {
                                            var item = json.updatehypervisorcapabilitiesresponse['null'];
                                            args.response.success({
                                                data: item
                                            });
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                }
                            }
                        },

                        tabs: {
                            details: {
                                title: 'label.details',
                                fields: [{
                                    id: {
                                        label: 'label.id'
                                    },
                                    hypervisor: {
                                        label: 'label.hypervisor'
                                    },
                                    hypervisorversion: {
                                        label: 'label.hypervisor.version'
                                    },
                                    maxguestslimit: {
                                        label: 'label.max.guest.limit',
                                        isEditable: true
                                    }
                                }],
                                dataProvider: function(args) {
                                    args.response.success({
                                        data: args.context.hypervisorCapabilities[0]
                                    });
                                }
                            }
                        }
                    }
                }
            }
        }
    };
})(cloudStack);
