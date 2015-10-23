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
    cloudStack.sections.events = {
        title: 'label.menu.events',
        id: 'events',
        sectionSelect: {
            preFilter: function(args) {
                if (isAdmin())
                    return ["events", "alerts"];
                else
                    return ["events"];
            },
            label: 'label.select-view'
        },
        sections: {
            events: {
                type: 'select',
                title: 'label.menu.events',
                listView: {
                    id: 'events',
                    label: 'label.menu.events',
                    multiSelect: true,
                    fields: {
                        description: {
                            label: 'label.description'
                        },
                        level: {
                            label: 'label.level'
                        },
                        type: {
                            label: 'label.type',
                            truncate: true
                        },
                        domain: {
                            label: 'label.domain'
                        },
                        account: {
                            label: 'label.account'
                        },
                        created: {
                            label: 'label.date',
                            converter: cloudStack.converters.toLocalDate
                        }
                    },

                    actions: {
                        // Remove multiple events
                        removeMulti: {
                            label: 'label.delete.events',
                            isHeader: true,
                            addRow: false,
                            isMultiSelectAction: true,
                            messages: {
                                confirm: function(args) {
                                    return 'message.confirm.remove.selected.events';
                                },
                                notification: function(args) {
                                    return 'label.delete.events';
                                }
                            },
                            action: function(args) {
                                var events = args.context.events;

                                $.ajax({
                                    url: createURL("deleteEvents"),
                                    data: {
                                        ids: $(events).map(function(index, event) {
                                            return event.id;
                                        }).toArray().join(',')
                                    },
                                    success: function(data) {
                                        args.response.success();
                                        $(window).trigger('cloudStack.fullRefresh');
                                    },
                                    error:function(data) {
                                        args.response.error(parseXMLHttpResponse(data));
                                    }
                                });
                            }
                        },

                        remove: {
                            label: 'label.delete.events',
                            isHeader: true,
                            addRow: false,
                            messages: {
                                notification: function(args) {
                                    return 'label.delete.events';
                                }
                            },
                            createForm: {
                                title: 'label.delete.events',
                                desc: '',
                                fields: {
                                    type: {
                                        label: 'label.by.event.type',
                                        docID: 'helpEventsDeleteType'
                                    },
                                    startdate: {
                                        label: 'label.by.date.start',
                                        docID: 'helpEventsDeleteDate',
                                        isDatepicker: true
                                    },
                                    enddate: {
                                        label: 'label.by.date.end',
                                        docID: 'helpEventsDeleteDate',
                                        isDatepicker: true
                                    }
                                }
                            },
                            action: function(args) {

                                var data = {};

                                if (args.data.type != "")
                                    $.extend(data, {
                                        type: args.data.type
                                    });

                                if (args.data.startdate != "")
                                    $.extend(data, {
                                        startdate: args.data.startdate
                                    });

                                if (args.data.enddate != "")
                                    $.extend(data, {
                                        enddate: args.data.enddate
                                    });

                                $.ajax({
                                    url: createURL("deleteEvents"),
                                    data: data,
                                    success: function(data) {
                                        args.response.success();
                                    },
                                    error:function(data) {
                                        args.response.error(parseXMLHttpResponse(data));
                                    }
                                });
                            }
                        },

                        archive: {
                            label: 'label.archive.events',
                            isHeader: true,
                            addRow: false,
                            messages: {
                                notification: function(args) {
                                    return 'label.archive.events';
                                }
                            },
                            createForm: {
                                title: 'label.archive.events',
                                desc: '',
                                fields: {
                                    type: {
                                        label: 'label.by.event.type',
                                        docID: 'helpEventsArchiveType'
                                    },
                                    startdate: {
                                        label: 'label.by.date.start',
                                        docID: 'helpEventsArchiveDate',
                                        isDatepicker: true
                                    },
                                    enddate: {
                                        label: 'label.by.date.end',
                                        docID: 'helpEventsArchiveDate',
                                        isDatepicker: true
                                    }
                                }
                            },
                            action: function(args) {
                                var data = {};

                                if (args.data.type != "")
                                    $.extend(data, {
                                        type: args.data.type
                                    });

                                if (args.data.startdate != "")
                                    $.extend(data, {
                                        startdate: args.data.startdate
                                    });

                                if (args.data.enddate != "")
                                    $.extend(data, {
                                        enddate: args.data.enddate
                                    });

                                $.ajax({
                                    url: createURL("archiveEvents"),
                                    data: data,
                                    dataType: 'json',
                                    async: false,

                                    success: function(data) {
                                        args.response.success();
                                    }
                                });

                            }
                        },

                        // Archive multiple events
                        archiveMulti: {
                            label: 'label.archive.events',
                            isHeader: true,
                            addRow: false,
                            isMultiSelectAction: true,
                            messages: {
                                confirm: function(args) {
                                    return 'message.confirm.archive.selected.events';
                                },
                                notification: function(args) {
                                    return 'label.archive.events';
                                }
                            },
                            action: function(args) {
                                var events = args.context.events;

                                $.ajax({
                                    url: createURL("archiveEvents"),
                                    data: {
                                        ids: $(events).map(function(index, event) {
                                            return event.id;
                                        }).toArray().join(',')
                                    },
                                    success: function(data) {
                                        args.response.success();
                                        $(window).trigger('cloudStack.fullRefresh');
                                    },
                                    error:function(data) {
                                        args.response.error(parseXMLHttpResponse(data));
                                    }
                                });
                            }
                        }

                    },



                    advSearchFields: {
                        level: {
                            label: 'label.level',
                            select: function(args) {
                                args.response.success({
                                    data: [{
                                        id: '',
                                        description: ''
                                    }, {
                                        id: 'INFO',
                                        description: 'label.info.upper'
                                    }, {
                                        id: 'WARN',
                                        description: 'label.warn.upper'
                                    }, {
                                        id: 'ERROR',
                                        description: 'label.error.upper'
                                    }]
                                });
                            }
                        },

                        domainid: {
                            label: 'label.domain',
                            select: function(args) {
                                if (isAdmin() || isDomainAdmin()) {
                                    $.ajax({
                                        url: createURL('listDomains'),
                                        data: {
                                            listAll: true,
                                            details: 'min'
                                        },
                                        success: function(json) {
                                            var array1 = [{
                                                id: '',
                                                description: ''
                                            }];
                                            var domains = json.listdomainsresponse.domain;
                                            if (domains != null && domains.length > 0) {
                                                for (var i = 0; i < domains.length; i++) {
                                                    array1.push({
                                                        id: domains[i].id,
                                                        description: domains[i].path
                                                    });
                                                }
                                            }
                                            array1.sort(function(a, b) {
                                                return a.description.localeCompare(b.description);
                                            });
                                            args.response.success({
                                                data: array1
                                            });
                                        }
                                    });
                                } else {
                                    args.response.success({
                                        data: null
                                    });
                                }
                            },
                            isHidden: function(args) {
                                if (isAdmin() || isDomainAdmin())
                                    return false;
                                else
                                    return true;
                            }
                        },

                        account: {
                            label: 'label.account',
                            isHidden: function(args) {
                                if (isAdmin() || isDomainAdmin())
                                    return false;
                                else
                                    return true;
                            }
                        }
                    },

                    dataProvider: function(args) {
                        var data = {};
                        listViewDataProvider(args, data);

                        $.ajax({
                            url: createURL('listEvents'),
                            data: data,
                            success: function(json) {
                                var items = json.listeventsresponse.event;
                                args.response.success({
                                    data: items
                                });
                            },
                            error: function(XMLHttpResponse) {
                                cloudStack.dialog.notice({
                                    message: parseXMLHttpResponse(XMLHttpResponse)
                                });
                                args.response.error();
                             }
                        });
                    },
                    detailView: {
                        name: 'label.details',
                        actions: {

                            // Remove single event
                            remove: {
                                label: 'label.delete',
                                messages: {
                                    notification: function(args) {
                                        return 'label.event.deleted';
                                    },
                                    confirm: function() {
                                        return 'message.confirm.remove.event';
                                    }
                                },
                                action: function(args) {

                                    $.ajax({
                                        url: createURL("deleteEvents&ids=" + args.context.events[0].id),
                                        success: function(json) {
                                            args.response.success();
                                            $(window).trigger('cloudStack.fullRefresh');
                                        }

                                    });
                                }
                            },

                            // Archive single event
                            archive: {
                                label: 'label.archive',
                                messages: {
                                    notification: function(args) {
                                        return 'label.event.archived';
                                    },
                                    confirm: function() {
                                        return 'message.confirm.archive.event';
                                    }
                                },
                                action: function(args) {

                                    $.ajax({
                                        url: createURL("archiveEvents&ids=" + args.context.events[0].id),
                                        success: function(json) {
                                            args.response.success();
                                            $(window).trigger('cloudStack.fullRefresh');
                                        }
                                    });
                                }
                            }
                        },
                        tabs: {
                            details: {
                                title: 'label.details',
                                fields: [{
                                    description: {
                                        label: 'label.description'
                                    },
                                    state: {
                                        label: 'label.state'
                                    },
                                    level: {
                                        label: 'label.level'
                                    },
                                    type: {
                                        label: 'label.type'
                                    },
                                    domain: {
                                        label: 'label.domain'
                                    },
                                    account: {
                                        label: 'label.account'
                                    },
                                    username: {
                                        label: 'label.initiated.by'
                                    },
                                    created: {
                                        label: 'label.date',
                                        converter: cloudStack.converters.toLocalDate
                                    },
                                    id: {
                                        label: 'label.id'
                                    }
                                }],
                                dataProvider: function(args) {
                                    $.ajax({
                                        url: createURL("listEvents&id=" + args.context.events[0].id),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var item = json.listeventsresponse.event[0];
                                            args.response.success({
                                                data: item
                                            });
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
            },
            alerts: {
                type: 'select',
                title: 'label.menu.alerts',
                listView: {
                    id: 'alerts',
                    label: 'label.menu.alerts',
                    multiSelect: true,
                    fields: {
                        description: {
                            label: 'label.description'
                        },
                        type: {
                            label: 'label.type'
                        },
                        sent: {
                            label: 'label.date',
                            converter: cloudStack.converters.toLocalDate
                        }
                    },

                    actions: {
                        // Remove multiple Alerts
                        removeMulti: {
                            label: 'label.delete.alerts',
                            isHeader: true,
                            addRow: false,
                            isMultiSelectAction: true,
                            messages: {
                                confirm: function(args) {
                                    return 'message.confirm.remove.selected.alerts';
                                },
                                notification: function(args) {
                                    return 'label.delete.alerts';
                                }
                            },
                            action: function(args) {
                                var events = args.context.alerts;

                                $.ajax({
                                    url: createURL("deleteAlerts"),
                                    data: {
                                        ids: $(events).map(function(index, event) {
                                            return event.id;
                                        }).toArray().join(',')
                                    },
                                    success: function(data) {
                                        args.response.success();
                                        $(window).trigger('cloudStack.fullRefresh');
                                    },
                                    error:function(data) {
                                        args.response.error(parseXMLHttpResponse(data));
                                    }
                                });
                            }
                        },

                        remove: {
                            label: 'label.delete.alerts',
                            isHeader: true,
                            addRow: false,
                            messages: {
                                notification: function(args) {
                                    return 'label.delete.alerts';
                                }
                            },
                            createForm: {
                                title: 'label.delete.alerts',
                                desc: '',
                                fields: {
                                    type: {
                                        label: 'label.by.alert.type',
                                        docID: 'helpAlertsDeleteType'
                                    },
                                    startdate: {
                                        label: 'label.by.date.start',
                                        docID: 'helpAlertsDeleteDate',
                                        isDatepicker: true
                                    },
                                    enddate: {
                                        label: 'label.by.date.end',
                                        docID: 'helpAlertsDeleteDate',
                                        isDatepicker: true
                                    }
                                }
                            },
                            action: function(args) {

                                var data = {};

                                if (args.data.type != "")
                                    $.extend(data, {
                                        type: args.data.type
                                    });

                                if (args.data.startdate != "")
                                    $.extend(data, {
                                        startdate: args.data.startdate
                                    });

                                if (args.data.enddate != "")
                                    $.extend(data, {
                                        enddate: args.data.enddate
                                    });

                                $.ajax({
                                    url: createURL("deleteAlerts"),
                                    data: data,
                                    dataType: 'json',
                                    async: false,

                                    success: function(data) {
                                        args.response.success();
                                    },
                                    error:function(data) {
                                        args.response.error(parseXMLHttpResponse(data));
                                    }
                                });
                            }
                        },

                        // Archive multiple Alerts
                        archiveMulti: {
                            label: 'label.archive.alerts',
                            isHeader: true,
                            addRow: false,
                            isMultiSelectAction: true,
                            messages: {
                                confirm: function(args) {
                                    return 'message.confirm.archive.selected.alerts';
                                },
                                notification: function(args) {
                                    return 'label.archive.alerts';
                                }
                            },
                            action: function(args) {
                                var events = args.context.alerts;

                                $.ajax({
                                    url: createURL("archiveAlerts"),
                                    data: {
                                        ids: $(events).map(function(index, event) {
                                            return event.id;
                                        }).toArray().join(',')
                                    },
                                    success: function(data) {
                                        args.response.success();
                                        $(window).trigger('cloudStack.fullRefresh');
                                    },
                                    error:function(data) {
                                        args.response.error(parseXMLHttpResponse(data));
                                    }
                                });
                            }
                        },

                        archive: {
                            label: 'label.archive.alerts',
                            isHeader: true,
                            addRow: false,
                            messages: {
                                notification: function(args) {
                                    return 'label.archive.alerts';
                                }
                            },
                            createForm: {
                                title: 'label.archive.alerts',
                                desc: '',
                                fields: {
                                    type: {
                                        label: 'label.by.alert.type',
                                        docID: 'helpAlertsArchiveType'
                                    },
                                    startdate: {
                                        label: 'label.by.date.start',
                                        docID: 'helpAlertsArchiveDate',
                                        isDatepicker: true
                                    },
                                    enddate: {
                                        label: 'label.by.date.end',
                                        docID: 'helpAlertsArchiveDate',
                                        isDatepicker: true
                                    }

                                }
                            },
                            action: function(args) {
                                var data = {};

                                if (args.data.type != "")
                                    $.extend(data, {
                                        type: args.data.type
                                    });

                                if (args.data.startdate != "")
                                    $.extend(data, {
                                        startdate: args.data.startdate
                                    });

                                if (args.data.enddate != "")
                                    $.extend(data, {
                                        enddate: args.data.enddate
                                    });

                                $.ajax({
                                    url: createURL("archiveAlerts"),
                                    data: data,
                                    dataType: 'json',
                                    async: false,

                                    success: function(data) {
                                        args.response.success();
                                    }
                                });
                            }
                        }
                    },

                    dataProvider: function(args) {
                        var data = {};
                        listViewDataProvider(args, data);

                        $.ajax({
                            url: createURL('listAlerts'),
                            data: data,
                            async: true,
                            success: function(json) {
                                var items = json.listalertsresponse.alert;
                                args.response.success({
                                    data: items
                                });
                            }
                        });
                    },
                    detailView: {
                        name: 'label.alert.details',
                        actions: {

                            // Remove single Alert
                            remove: {
                                label: 'label.delete',
                                messages: {
                                    notification: function(args) {
                                        return 'label.alert.deleted';
                                    },
                                    confirm: function() {
                                        return 'message.confirm.delete.alert';
                                    }
                                },
                                action: function(args) {

                                    $.ajax({
                                        url: createURL("deleteAlerts&ids=" + args.context.alerts[0].id),
                                        success: function(json) {
                                            args.response.success();
                                            $(window).trigger('cloudStack.fullRefresh');
                                        }
                                    });

                                }
                            },

                            archive: {
                                label: 'label.archive',
                                messages: {
                                    notification: function(args) {
                                        return 'label.alert.archived';
                                    },
                                    confirm: function() {
                                        return 'message.confirm.archive.alert';
                                    }
                                },
                                action: function(args) {

                                    $.ajax({
                                        url: createURL("archiveAlerts&ids=" + args.context.alerts[0].id),
                                        success: function(json) {
                                            args.response.success();
                                            $(window).trigger('cloudStack.fullRefresh');
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
                                    description: {
                                        label: 'label.description'
                                    },
                                    sent: {
                                        label: 'label.date',
                                        converter: cloudStack.converters.toLocalDate
                                    }
                                }],
                                dataProvider: function(args) {
                                    $.ajax({
                                        url: createURL("listAlerts&id=" + args.context.alerts[0].id),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var item = json.listalertsresponse.alert[0];
                                            args.response.success({
                                                data: item
                                            });
                                        }
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
