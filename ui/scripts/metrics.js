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
                allocationstate: {
                    label: 'label.state',
                    converter: function (str) {
                        // For localization
                        return str;
                    },
                    indicator: {
                        'Enabled': 'on',
                        'Destroyed': 'off'
                    }
                },
                hosts: {
                    label: 'label.hosts'
                },
                cpuused: {
                    label: 'Avg CPU Used'
                },
                cpumaxdev: {
                    label: 'CPU Used Max Dev'
                },
                cpuallocated: {
                    label: 'CPU Allocated'
                },
                cputotal: {
                    label: 'Total CPU Ghz'
                },
                memused: {
                    label: 'Avg Memory Used'
                },
                memmaxdev: {
                    label: 'Memory Used Max Dev'
                },
                memallocated: {
                    label: 'Memory Allocated'
                },
                memtotal: {
                    label: 'Total Memory GB'
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
                            $.ajax({
                                url: createURL('listHosts'),
                                data: {clusterid: cluster.id},
                                success: function(json) {
                                    items[idx].hosts = json.listhostsresponse.count;
                                    items[idx].cpuused = 0.0;
                                    items[idx].cpumaxdev = 0.0;
                                    items[idx].cpuallocated = 0.0;
                                    items[idx].cputotal = 0.0;
                                    items[idx].memused = 0.0;
                                    items[idx].memmaxdev = 0.0;
                                    items[idx].memallocated = 0.0;
                                    items[idx].memtotal = 0.0;
                                    var maxCpuUsed = 0.0;
                                    $.each(json.listhostsresponse.host, function(i, host) {
                                        if (host.hasOwnProperty('cpuused')) {
                                            items[idx].cpuused += host.cpuused;
                                            if (host.cpuused > maxCpuUsed) {
                                                maxCpuUsed = host.cpuused;
                                            }
                                        }

                                        if (host.hasOwnProperty('cpuallocated')) {
                                            items[idx].cpuallocated += parseFloat(host.cpuallocated.replace('%', ''));
                                        }

                                    });

                                    items[idx].cpuused = 100.0 * items[idx].cpuused / items[idx].hosts;
                                    items[idx].cpuallocated = (items[idx].cpuallocated / items[idx].hosts).toFixed(2);
                                },
                                async: false
                            });
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
            horizontalOverflow: true
        }
    };

})(cloudStack);
