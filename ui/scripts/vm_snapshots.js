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
    cloudStack.sections.vmsnapshots = {
        title: 'label.vmsnapshot',
        id: 'vmsnapshots',
        listView: {
            id: 'vmsnapshots',
            isMaximized: true,
            fields: {
                displayname: {
                    label: 'label.name'
                },
                state: {
                    label: 'label.state',
                    indicator: {
                        'Ready': 'on',
                        'Error': 'off'
                    }
                },
                type: {
                    label: 'label.vmsnapshot.type'
                },
                current: {
                    label: 'label.vmsnapshot.current',
                    converter: cloudStack.converters.toBooleanText
                },
                parentName: {
                    label: 'label.vmsnapshot.parentname'
                },
                created: {
                    label: 'label.date',
                    converter: cloudStack.converters.toLocalDate
                }
            },

            dataProvider: function(args) {
                var apiCmd = "listVMSnapshot&listAll=true";
                if (args.context != null) {
                    if ("instances" in args.context) {
                        apiCmd += "&virtualmachineid=" + args.context.instances[0].id;
                    }
                }
                $.ajax({
                    url: createURL(apiCmd),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                        var jsonObj;
                        jsonObj = json.listvmsnapshotresponse.vmSnapshot;
                        args.response.success({
                            data: jsonObj
                        });
                    }
                });
            },
            //dataProvider end
            detailView: {
                tabs: {
                    details: {
                        title: 'label.details',
                        fields: {
                            id: {
                                label: 'label.id'
                            },
                            name: {
                                label: 'label.name'
                            },
                            displayname: {
                                label: 'label.display.name'
                            },
                            type: {
                                label: 'label.vmsnapshot.type'
                            },
                            description: {
                                label: 'label.description'
                            },
                            state: {
                                label: 'label.state',
                                indicator: {
                                    'Ready': 'on',
                                    'Error': 'off'
                                }
                            },
                            current: {
                                label: 'label.vmsnapshot.current',
                                converter: cloudStack.converters.toBooleanText
                            },
                            parentName: {
                                label: 'label.vmsnapshot.parentname'
                            },
                            created: {
                                label: 'label.date',
                                converter: cloudStack.converters.toLocalDate
                            }
                        },
                        dataProvider: function(args) {
                            $.ajax({
                                url: createURL("listVMSnapshot&listAll=true&vmsnapshotid=" + args.context.vmsnapshots[0].id),
                                dataType: "json",
                                async: true,
                                success: function(json) {
                                    var jsonObj;
                                    jsonObj = json.listvmsnapshotresponse.vmSnapshot[0];
                                    args.response.success({
                                        //actionFilter: vmActionfilter,
                                        data: jsonObj
                                    });
                                }
                            });
                        },
                        tags: cloudStack.api.tags({
                            resourceType: 'VMSnapshot',
                            contextId: 'vmsnapshots'
                        })
                    }
                },
                actions: {
                    //delete a snapshot
                    remove: {
                        label: 'label.action.vmsnapshot.delete',
                        messages: {
                            confirm: function(args) {
                                return 'message.action.vmsnapshot.delete';
                            },
                            notification: function(args) {
                                return 'label.action.vmsnapshot.delete';
                            }
                        },
                        action: function(args) {
                            $.ajax({
                                url: createURL("deleteVMSnapshot&vmsnapshotid=" + args.context.vmsnapshots[0].id),
                                dataType: "json",
                                async: true,
                                success: function(json) {
                                    var jid = json.deletevmsnapshotresponse.jobid;
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
                    },
                    restart: {
                        label: 'label.action.vmsnapshot.revert',
                        messages: {
                            confirm: function(args) {
                                return 'label.action.vmsnapshot.revert';
                            },
                            notification: function(args) {
                                return 'message.action.vmsnapshot.revert';
                            }
                        },
                        action: function(args) {
                            $.ajax({
                                url: createURL("revertToVMSnapshot&vmsnapshotid=" + args.context.vmsnapshots[0].id),
                                dataType: "json",
                                async: true,
                                success: function(json) {
                                    var jid = json.reverttovmsnapshotresponse.jobid;
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
                }
            }
            //detailview end
        }
    }
})(jQuery, cloudStack);
