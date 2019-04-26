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
    cloudStack.sections.affinityGroups = {
        title: 'label.affinity.groups',
        listView: {
            id: 'affinityGroups',
            fields: {
                name: {
                    label: 'label.name'
                },
                type: {
                    label: 'label.type'
                }
            },
            dataProvider: function(args) {
                var data = {};
                listViewDataProvider(args, data);
                if (args.context != null) {
                    if ("instances" in args.context) {
                        $.extend(data, {
                            virtualmachineid: args.context.instances[0].id
                        });
                    }
                }
                $.ajax({
                    url: createURL('listAffinityGroups'),
                    data: data,
                    success: function(json) {
                        var items = json.listaffinitygroupsresponse.affinitygroup;
                        args.response.success({
                            data: items
                        });
                    }
                });
            },
            actions: {
                add: {
                    label: 'label.add.affinity.group',

                    messages: {
                        notification: function(args) {
                            return 'label.add.affinity.group';
                        }
                    },

                    createForm: {
                        title: 'label.add.affinity.group',
                        fields: {
                            name: {
                                label: 'label.name',
                                validation: {
                                    required: true
                                }
                            },
                            description: {
                                label: 'label.description'
                            },
                            type: {
                                label: 'label.type',
                                select: function(args) {
                                    $.ajax({
                                        url: createURL('listAffinityGroupTypes'),
                                        success: function(json) {
                                            var types = [];
                                            var items = json.listaffinitygrouptypesresponse.affinityGroupType;
                                            if (items != null) {
                                                for (var i = 0; i < items.length; i++) {
                                                    types.push({
                                                        id: items[i].type,
                                                        description: items[i].type
                                                    });
                                                }
                                            }
                                            args.response.success({
                                                data: types
                                            })
                                        }
                                    });
                                }
                            }
                        }
                    },

                    action: function(args) {
                        var data = {
                            name: args.data.name,
                            type: args.data.type
                        };
                        if (args.data.description != null && args.data.description.length > 0)
                            $.extend(data, {
                                description: args.data.description
                            });

                        $.ajax({
                            url: createURL('createAffinityGroup'),
                            data: data,
                            success: function(json) {
                                var jid = json.createaffinitygroupresponse.jobid;
                                args.response.success({
                                    _custom: {
                                        jobId: jid,
                                        getUpdatedItem: function(json) {
                                            return json.queryasyncjobresultresponse.jobresult.affinitygroup;
                                        }
                                    }
                                });
                            }
                        });
                    },

                    notification: {
                        poll: pollAsyncJobResult
                    }
                }
            },
            detailView: {
                actions: {
                    remove: {
                        label: 'label.delete.affinity.group',
                        messages: {
                            confirm: function(args) {
                                return 'message.delete.affinity.group';
                            },
                            notification: function(args) {
                                return 'label.delete.affinity.group';
                            }
                        },
                        action: function(args) {
                            $.ajax({
                                url: createURL('deleteAffinityGroup'),
                                data: {
                                    id: args.context.affinityGroups[0].id
                                },
                                success: function(json) {
                                    var jid = json.deleteaffinitygroupresponse.jobid;
                                    args.response.success({
                                        _custom: {
                                            jobId: jid
                                        }
                                    });
                                }
                            });
                        },
                        notification: {
                            poll: pollAsyncJobResult
                        }
                    }
                },

                viewAll: {
                    path: 'instances',
                    label: 'label.instances'
                },

                tabs: {
                    details: {
                        title: 'label.details',
                        fields: [{
                            name: {
                                label: 'label.name'
                            }
                        }, {
                            description: {
                                label: 'label.description'
                            },
                            type: {
                                label: 'label.type'
                            },
                            id: {
                                label: 'label.id'
                            }
                        }],

                        dataProvider: function(args) {
                            $.ajax({
                                url: createURL('listAffinityGroups'),
                                data: {
                                    id: args.context.affinityGroups[0].id
                                },
                                success: function(json) {
                                    var item = json.listaffinitygroupsresponse.affinitygroup[0];
                                    args.response.success({
                                        actionFilter: affinitygroupActionfilter,
                                        data: item
                                    });
                                }
                            });
                        }
                    }
                }
            }
        }
    };

    var affinitygroupActionfilter = cloudStack.actionFilter.affinitygroupActionfilter = function(args) {
        var jsonObj = args.context.item;
        var allowedActions = [];
        if (jsonObj.type != 'ExplicitDedication' || isAdmin()) {
            allowedActions.push("remove");
        }
        return allowedActions;
    }

})(cloudStack);
