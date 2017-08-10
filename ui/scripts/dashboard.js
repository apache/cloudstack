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
    // Admin dashboard
    cloudStack.sections.dashboard = {
        title: 'label.menu.dashboard',
        show: cloudStack.uiCustom.dashboard,

        adminCheck: function(args) {
            return isAdmin() ? true : false;
        },

        // User dashboard
        user: {
            dataProvider: function(args) {
                var dataFns = {
                    instances: function(data) {
                        var totalInstanceCount = 0;
                        $.ajax({
                            url: createURL("listVirtualMachines"),
                            data: {
                                listAll: true,
                                page: 1,
                                pageSize: 1
                            },
                            async: false,
                            success: function(json) {
                                if (json.listvirtualmachinesresponse.count != undefined) {
                                    totalInstanceCount = json.listvirtualmachinesresponse.count;
                                }
                            }
                        });

                        var RunningInstanceCount = 0;
                        $.ajax({
                            url: createURL("listVirtualMachines"),
                            data: {
                                listAll: true,
                                page: 1,
                                pageSize: 1,
                                state: "Running"
                            },
                            async: false,
                            success: function(json) {
                                if (json.listvirtualmachinesresponse.count != undefined) {
                                    RunningInstanceCount = json.listvirtualmachinesresponse.count;
                                }
                            }
                        });

                        var stoppedInstanceCount = 0;
                        $.ajax({
                            url: createURL("listVirtualMachines"),
                            data: {
                                listAll: true,
                                page: 1,
                                pageSize: 1,
                                state: "Stopped"
                            },
                            async: false,
                            success: function(json) {
                                if (json.listvirtualmachinesresponse.count != undefined) {
                                    stoppedInstanceCount = json.listvirtualmachinesresponse.count;
                                }
                            }
                        });

                        dataFns.account($.extend(data, {
                            runningInstances: RunningInstanceCount,
                            stoppedInstances: stoppedInstanceCount,
                            totalInstances: totalInstanceCount
                        }));
                    },

                    account: function(data) {
                        var user = cloudStack.context.users[0];
                        dataFns.events($.extend(data, {
                            accountID: user.userid,
                            accountName: user.account,
                            userName: user.username,
                            accountType: user.role,
                            accountDomainID: user.domainid
                        }));
                    },

                    events: function(data) {
                        $.ajax({
                            url: createURL('listEvents'),
                            data: {
                                listAll: true,
                                page: 1,
                                pageSize: (pageSize > 4? 4: pageSize) //if default.page.size > 4, show 4 items only (since space on dashboard is limited)
                                //pageSize: 1 //for testing only
                            },
                            success: function(json) {
                                dataFns.ipAddresses($.extend(data, {
                                    events: json.listeventsresponse.event ? json.listeventsresponse.event : []
                                }));
                            }
                        });
                    },

                    ipAddresses: function(data) {
                        $.ajax({
                            url: createURL('listNetworks'),
                            data: {
                                listAll: true,
                                page: 1,
                                pageSize: 1,
                                type: 'isolated',
                                supportedServices: 'SourceNat'
                            },
                            success: function(json) {
                                var netTotal = json.listnetworksresponse.count ?
                                    json.listnetworksresponse.count : 0;

                                $.ajax({
                                    url: createURL('listPublicIpAddresses'),
                                    data: {
                                        page: 1,
                                        pageSize: 1
                                    },
                                    success: function(json) {
                                        var ipTotal = json.listpublicipaddressesresponse.count ?
                                            json.listpublicipaddressesresponse.count : 0;

                                        complete($.extend(data, {
                                            netTotal: netTotal,
                                            ipTotal: ipTotal
                                        }));
                                    }
                                });
                            }
                        });
                    }
                };

                var complete = function(data) {
                    args.response.success({
                        data: data
                    });
                };

                dataFns.instances({});
            }
        },

        // Admin dashboard
        admin: {
            zoneDetailView: {
                tabs: {
                    resources: {
                        title: 'label.resources',
                        custom: cloudStack.uiCustom.systemChart('resources')
                    }
                }
            },

            dataProvider: function(args) {
                var dataFns = {
                    zones: function(data) {
                        $.ajax({
                            url: createURL('listZones'),
                            success: function(json) {
                                dataFns.capacity({
                                    zones: json.listzonesresponse.zone
                                });
                            }
                        });
                    },
                    capacity: function(data) {
                        if (window.fetchLatestflag == 1) {
                            data.fetchLatest = true;
                        } else {
                            data.fetchLatest = false;
                        }
                        window.fetchLatestflag = 0;
                        dataFns.alerts(data);
                    },

                    alerts: function(data) {
                        $.ajax({
                            url: createURL('listAlerts'),
                            data: {
                                page: 1,
                                pageSize: (pageSize > 4? 4: pageSize) //if default.page.size > 4, show 4 items only (since space on dashboard is limited)
                            },
                            success: function(json) {
                                var alerts = json.listalertsresponse.alert ?
                                    json.listalertsresponse.alert : [];

                                dataFns.hostAlerts($.extend(data, {
                                    alerts: $.map(alerts, function(alert) {
                                        return {
                                            name: cloudStack.converters.toAlertType(alert.type),
                                            description: alert.description,
                                            sent: cloudStack.converters.toLocalDate(alert.sent)
                                        };
                                    })
                                }));
                            }
                        });
                    },

                    hostAlerts: function(data) {
                        $.ajax({
                            url: createURL('listHosts'),
                            data: {
                                state: 'Alert',
                                page: 1,
                                pageSize: (pageSize > 4? 4: pageSize) //if default.page.size > 4, show 4 items only (since space on dashboard is limited)
                            },
                            success: function(json) {
                                var hosts = json.listhostsresponse.host ?
                                    json.listhostsresponse.host : [];

                                dataFns.zoneCapacity($.extend(data, {
                                    hostAlerts: $.map(hosts, function(host) {
                                        return {
                                            name: host.name,
                                            description: 'message.alert.state.detected'
                                        };
                                    })
                                }));
                            }
                        });
                    },

                    zoneCapacity: function(data) {
                        $.ajax({
                            url: createURL('listCapacity'),
                            data: {
                                fetchLatest: data.fetchLatest,
                                sortBy: 'usage',
                            },
                            success: function(json) {
                                var capacities = json.listcapacityresponse.capacity ?
                                    json.listcapacityresponse.capacity : [];

                                complete($.extend(data, {
                                    zoneCapacities: $.map(capacities, function(capacity) {
                                        if (capacity.podname) {
                                            capacity.zonename = capacity.zonename.concat(', ' + _l('label.pod') + ': ' + capacity.podname);
                                        }

                                        if (capacity.clustername) {
                                            capacity.zonename = capacity.zonename.concat(', ' + _l('label.cluster') + ': ' + capacity.clustername);
                                        }

                                        capacity.zonename.replace('Zone:', _l('label.zone') + ':');

                                        return {
                                            zoneID: capacity.zoneid, // Temporary fix for dashboard
                                            zoneName: capacity.zonename,
                                            type: cloudStack.converters.toCapacityCountType(capacity.type),
                                            percent: parseInt(capacity.percentused),
                                            used: cloudStack.converters.convertByType(capacity.type, capacity.capacityused),
                                            total: cloudStack.converters.convertByType(capacity.type, capacity.capacitytotal)
                                        };
                                    })
                                }));
                            }
                        });
                    }
                };

                var complete = function(data) {
                    args.response.success({
                        data: data
                    });
                };

                dataFns.zones({});
            }
        }
    };
})(jQuery, cloudStack);
