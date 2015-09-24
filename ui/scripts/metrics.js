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
    cloudStack.sections.metrics = {
        title: 'label.metrics',
        listView: {
            id: 'metrics',
            fields: {
                name: {
                    label: 'metrics'
                }
            },
            refreshMetrics: function() {
                console.log("Refreshing metrics");
            },
            hideSearchBar: true
        }
    };

    // Cluster Metrics
    cloudStack.sections.metrics.cluster = {
        title: 'label.metrics',
        listView: {
            id: 'metrics',
            fields: {
                name: {
                    label: 'label.name'
                },
                state: {
                    label: 'label.state',
                    converter: function (str) {
                        // For localization
                        return str;
                    },
                    indicator: {
                        'Enabled': 'on',
                        'Unmanaged': 'off',
                        'Destroyed': 'off',
                        'Disabled': 'off'
                    }
                },
                hosts: {
                    label: 'label.hosts'
                },
                cpuused: {
                    label: 'label.cpu.usage',
                    collapsible: true,
                    columns: {
                        cpuusedavg: {
                            label: 'label.cpu.used.avg',
                        },
                        cpumaxdev: {
                            label: 'label.cpu.max.dev'
                        }
                    }
                },
                cpuallocated: {
                    label: 'label.cpu.allocated',
                    collapsible: true,
                    columns: {
                        cpuallocated: {
                            label: 'label.allocated'
                        },
                        cputotal: {
                            label: 'label.cpu.total.ghz'
                        }
                    }
                },
                memused: {
                    label: 'label.memory.usage',
                    collapsible: true,
                    columns: {
                        memusedavg: {
                            label: 'label.memory.used.avg'
                        },
                        memmaxdev: {
                            label: 'label.memory.max.dev'
                        }
                    }
                },
                memallocated: {
                    label: 'label.memory.allocated',
                    collapsible: true,
                    columns: {
                        memallocated: {
                            label: 'label.allocated'
                        },
                        memtotal: {
                            label: 'label.memory.total.gb'
                        }
                    }
                }
            },
            dataProvider: function(args) {
                var data = {};
                listViewDataProvider(args, data);
                $.ajax({
                    url: createURL('listClusters'),
                    data: data,
                    success: function(json) {
                        var items = json.listclustersresponse.cluster;
                        $.each(items, function(idx, cluster) {
                            items[idx].hosts = 0;
                            items[idx].cpuusedavg = 0.0;
                            items[idx].cpumaxdev = 0.0;
                            items[idx].cpuallocated = 0.0;
                            items[idx].maxCpuUsed = 0;
                            items[idx].memusedavg = 0.0;
                            items[idx].memmaxdev = 0.0;
                            items[idx].memallocated = 0.0;
                            items[idx].maxMemUsed = 0;

                            $.ajax({
                                url: createURL('listHosts'),
                                data: {clusterid: cluster.id},
                                success: function(json) {
                                    items[idx].hosts += parseInt(json.listhostsresponse.count);
                                    var maxCpuUsed = 0.0;
                                    var maxMemUsed = 0;
                                    $.each(json.listhostsresponse.host, function(i, host) {
                                        if (host.hasOwnProperty('cpuused')) {
                                            items[idx].cpuusedavg += host.cpuused;
                                            if (host.cpuused > items[idx].maxCpuUsed) {
                                                items[idx].maxCpuUsed = host.cpuused;
                                            }
                                        }

                                        if (host.hasOwnProperty('cpuallocated')) {
                                            items[idx].cpuallocated += parseFloat(host.cpuallocated.replace('%', ''));
                                        }

                                        if (host.hasOwnProperty('memused')) {
                                            items[idx].memusedavg += parseFloat(host.memused);
                                            if (host.memused > items[idx].maxMemUsed) {
                                                items[idx].maxMemUsed = host.memused;
                                            }
                                        }

                                        if (host.hasOwnProperty('memoryallocated')) {
                                            items[idx].memallocated += parseFloat(100.0 * parseFloat(host.memoryallocated)/parseFloat(host.memorytotal));
                                        }

                                    });

                                },
                                async: false
                            });

                            $.ajax({
                                url: createURL('listCapacity'),
                                data: {clusterid: cluster.id},
                                success: function(json) {
                                    $.each(json.listcapacityresponse.capacity, function(i, capacity) {
                                        // CPU
                                        if (capacity.type == 1) {
                                            items[idx].cputotal = (parseInt(capacity.capacitytotal)/(1000.0)).toFixed(2) + "Ghz";
                                        }
                                        // Memory
                                        if (capacity.type == 0) {
                                            items[idx].memtotal = (parseInt(capacity.capacitytotal)/(1024.0*1024.0*1024.0)).toFixed(2) + "GB";
                                        }
                                    });
                                },
                                async: false
                            });

                            items[idx].cpuusedavg = (100.0 * items[idx].cpuusedavg / items[idx].hosts);
                            items[idx].cpumaxdev = (items[idx].maxCpuUsed - items[idx].cpuusedavg);
                            items[idx].cpuallocated = (items[idx].cpuallocated / items[idx].hosts).toFixed(2) + "%";

                            items[idx].memusedavg = (100.0 * items[idx].memusedavg / items[idx].hosts);
                            items[idx].memmaxdev = (items[idx].maxMemUsed - items[idx].memusedavg);
                            items[idx].memallocated = (items[idx].memallocated / items[idx].hosts).toFixed(2) + "%";

                            items[idx].state = items[idx].allocationstate;
                            if (items[idx].managedstate == 'Unmanaged') {
                                items[idx].state = 'Unmanaged';
                            }

                            if (items[idx].managedstate == 'Managed' && items[idx].allocationstate == 'Enabled') {
                                items[idx].state = 'Enabled';
                            }

                            if (items[idx].managedstate == 'Managed' && items[idx].allocationstate == 'Disabled') {
                                items[idx].state = 'Disabled';
                            }
                        });
                        args.response.success({
                            data: items
                        });
                    }
                });
            },
            refreshMetrics: function() {
                console.log("Refreshing Cluster metrics");
            },
            hideSearchBar: true,
            needsRefresh: true,
            noSplit: true,
            horizontalOverflow: true,
            groupableColumns: true
        }
    };

})(cloudStack);
