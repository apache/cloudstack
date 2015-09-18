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
                cpuavg: {
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
                memavg: {
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
                console.log(args);

                var data = {};
                listViewDataProvider(args, data);
                $.ajax({
                    url: createURL('listClusters'),
                    data: data,
                    success: function(json) {
                        var items = json.listclustersresponse.cluster;
                        console.log(items);
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
            needsRefresh: true
        }
    };

})(cloudStack);
