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
            }
        }
    };


    // Zones Metrics
    cloudStack.sections.metrics.zones = {
        title: 'label.metrics',
        listView: {
            id: 'physicalResources',
            fields: {
                name: {
                    label: 'label.metrics.name'
                },
                state: {
                    label: 'label.metrics.state',
                    converter: function (str) {
                        // For localization
                        return str;
                    },
                    indicator: {
                        'Enabled': 'on',
                        'Disabled': 'off'
                    },
                    compact: true
                },
                clusters : {
                    label: 'label.metrics.clusters'
                },
                cpuused: {
                    label: 'label.metrics.cpu.usage',
                    collapsible: true,
                    columns: {
                        cpuusedavg: {
                            label: 'label.metrics.cpu.used.avg',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'cpunotificationthreshold',
                                disable: 'cpudisablethreshold'
                            }
                        },
                        cpumaxdev: {
                            label: 'label.metrics.cpu.max.dev'
                        }
                    }
                },
                cpuallocated: {
                    label: 'label.metrics.cpu.allocated',
                    collapsible: true,
                    columns: {
                        cpuallocated: {
                            label: 'label.metrics.allocated',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'cpunotificationthreshold',
                                disable: 'cpudisablethreshold'
                            }
                        },
                        cputotal: {
                            label: 'label.metrics.cpu.total'
                        }
                    }
                },
                memused: {
                    label: 'label.metrics.memory.usage',
                    collapsible: true,
                    columns: {
                        memusedavg: {
                            label: 'label.metrics.memory.used.avg',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'memnotificationthreshold',
                                disable: 'memdisablethreshold'
                            }
                        },
                        memmaxdev: {
                            label: 'label.metrics.memory.max.dev'
                        }
                    }
                },
                memallocated: {
                    label: 'label.metrics.memory.allocated',
                    collapsible: true,
                    columns: {
                        memallocated: {
                            label: 'label.metrics.allocated',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'memnotificationthreshold',
                                disable: 'memdisablethreshold'
                            }
                        },
                        memtotal: {
                            label: 'label.metrics.memory.total'
                        }
                    }
                }
            },
            dataProvider: function(args) {
                var data = {};
                listViewDataProvider(args, data);
                $.ajax({
                    url: createURL('listZones'),
                    data: data,
                    success: function(json) {
                        var items = json.listzonesresponse.zone;
                        if (items) {
                            $.each(items, function(idx, zone) {
                                items[idx].clusters = 0;
                                items[idx].clustersUp = 0;
                                items[idx].hosts = 0;
                                items[idx].cpuusedavg = 0.0;
                                items[idx].cpumaxdev = 0.0;
                                items[idx].cpuallocated = 0.0;
                                items[idx].cputotal = 0.0;
                                items[idx].maxCpuUsed = 0.0;
                                items[idx].memusedavg = 0.0;
                                items[idx].memmaxdev = 0.0;
                                items[idx].memallocated = 0.0;
                                items[idx].memtotal = 0.0;
                                items[idx].maxMemUsed = 0.0;

                                // Threshold color coding
                                items[idx].cpunotificationthreshold = 75.0;
                                items[idx].cpudisablethreshold = 95.0;
                                items[idx].memnotificationthreshold = 75.0;
                                items[idx].memdisablethreshold = 95.0;

                                $.ajax({
                                    url: createURL('listClusters'),
                                    data: {zoneid: zone.id},
                                    success: function(json) {
                                        if (json && json.listclustersresponse && json.listclustersresponse.cluster && json.listclustersresponse.count) {
                                            items[idx].clusters += parseInt(json.listclustersresponse.count);
                                            $.each(json.listclustersresponse.cluster, function(i, cluster) {
                                                if (cluster.allocationstate == 'Enabled' && cluster.managedstate == 'Managed') {
                                                    items[idx].clustersUp += 1;
                                                }
                                                $.ajax({
                                                    url: createURL('listHosts'),
                                                    data: {clusterid: cluster.id, type: 'routing'},
                                                    success: function(json) {
                                                        if (json && json.listhostsresponse && json.listhostsresponse.host && json.listhostsresponse.count) {
                                                            items[idx].hosts += parseInt(json.listhostsresponse.count);
                                                            $.each(json.listhostsresponse.host, function(i, host) {
                                                                if (host.hasOwnProperty('cpuused')) {
                                                                    var hostCpuUsage = parseFloat(host.cpuused);
                                                                    items[idx].cpuusedavg += hostCpuUsage;
                                                                    if (hostCpuUsage > items[idx].maxCpuUsed) {
                                                                        items[idx].maxCpuUsed = hostCpuUsage;
                                                                    }
                                                                }

                                                                if (host.hasOwnProperty('cpuallocated')) {
                                                                    items[idx].cpuallocated += parseFloat(host.cpuallocated.replace('%', ''));
                                                                }

                                                                if (host.hasOwnProperty('memoryused')) {
                                                                    var hostMemoryUsage = 100.0 * parseFloat(host.memoryused) /  parseFloat(host.memorytotal);
                                                                    items[idx].memusedavg += hostMemoryUsage;
                                                                    if (hostMemoryUsage > items[idx].maxMemUsed) {
                                                                        items[idx].maxMemUsed = hostMemoryUsage;
                                                                    }
                                                                }

                                                                if (host.hasOwnProperty('memoryallocated')) {
                                                                    items[idx].memallocated += parseFloat(100.0 * parseFloat(host.memoryallocated)/parseFloat(host.memorytotal));
                                                                }
                                                            });
                                                        }
                                                    },
                                                    async: false
                                                });
                                            });
                                        }
                                    },
                                    async: false
                                });

                                $.ajax({
                                    url: createURL('listCapacity'),
                                    data: {zoneid: zone.id},
                                    success: function(json) {
                                        if (json && json.listcapacityresponse && json.listcapacityresponse.capacity) {
                                            $.each(json.listcapacityresponse.capacity, function(i, capacity) {
                                                // CPU
                                                if (capacity.type == 1) {
                                                    items[idx].cputotal = parseInt(capacity.capacitytotal)/1000.0;
                                                }
                                                // Memory
                                                if (capacity.type == 0) {
                                                    items[idx].memtotal = parseInt(capacity.capacitytotal)/(1024.0*1024.0*1024.0);
                                                }
                                            });
                                        }
                                    },
                                    async: false
                                });

                                if (items[idx].hosts != 0) {
                                    items[idx].cpuusedavg = (items[idx].cpuusedavg / items[idx].hosts);
                                    items[idx].cpumaxdev = (items[idx].maxCpuUsed - items[idx].cpuusedavg);
                                    items[idx].cpuallocated = (items[idx].cpuallocated / items[idx].hosts);

                                    items[idx].memusedavg = (items[idx].memusedavg / items[idx].hosts);
                                    items[idx].memmaxdev = (items[idx].maxMemUsed - items[idx].memusedavg);
                                    items[idx].memallocated = (items[idx].memallocated / items[idx].hosts);
                                }
                                // Format data
                                items[idx].cpuusedavg = (items[idx].cpuusedavg).toFixed(2) + "%";
                                items[idx].cpumaxdev = (items[idx].cpumaxdev).toFixed(2) + "%";
                                items[idx].cpuallocated = (items[idx].cpuallocated).toFixed(2) + "%";
                                items[idx].cputotal = (items[idx].cputotal).toFixed(2) + " Ghz";

                                items[idx].memusedavg = (items[idx].memusedavg).toFixed(2) + "%";
                                items[idx].memmaxdev = (items[idx].memmaxdev).toFixed(2) + "%";
                                items[idx].memallocated = (items[idx].memallocated).toFixed(2) + "%";
                                items[idx].memtotal = (items[idx].memtotal).toFixed(2) + " GB";

                                items[idx].clusters = items[idx].clustersUp + ' / ' + items[idx].clusters;
                                items[idx].state = items[idx].allocationstate;
                            });
                        }
                        args.response.success({
                            data: items
                        });
                    }
                });
            },
            browseBy: {
                filterBy: 'zoneid',
                resource: 'clusters'
            },
            detailView: cloudStack.sections.system.physicalResourceSection.sections.physicalResources.listView.zones.detailView
        }
    };


    // Clusters Metrics
    cloudStack.sections.metrics.clusters = {
        title: 'label.metrics',
        listView: {
            id: 'clusters',
            fields: {
                name: {
                    label: 'label.metrics.name'
                },
                state: {
                    label: 'label.metrics.state',
                    converter: function (str) {
                        // For localization
                        return str;
                    },
                    indicator: {
                        'Enabled': 'on',
                        'Unmanaged': 'warning',
                        'Disabled': 'off'
                    },
                    compact: true
                },
                hosts: {
                    label: 'label.metrics.hosts'
                },
                cpuused: {
                    label: 'label.metrics.cpu.usage',
                    collapsible: true,
                    columns: {
                        cpuusedavg: {
                            label: 'label.metrics.cpu.used.avg',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'cpunotificationthreshold',
                                disable: 'cpudisablethreshold'
                            }
                        },
                        cpumaxdev: {
                            label: 'label.metrics.cpu.max.dev'
                        }
                    }
                },
                cpuallocated: {
                    label: 'label.metrics.cpu.allocated',
                    collapsible: true,
                    columns: {
                        cpuallocated: {
                            label: 'label.metrics.allocated',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'cpunotificationthreshold',
                                disable: 'cpudisablethreshold'
                            }
                        },
                        cputotal: {
                            label: 'label.metrics.cpu.total'
                        }
                    }
                },
                memused: {
                    label: 'label.metrics.memory.usage',
                    collapsible: true,
                    columns: {
                        memusedavg: {
                            label: 'label.metrics.memory.used.avg',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'memnotificationthreshold',
                                disable: 'memdisablethreshold'
                            }
                        },
                        memmaxdev: {
                            label: 'label.metrics.memory.max.dev'
                        }
                    }
                },
                memallocated: {
                    label: 'label.metrics.memory.allocated',
                    collapsible: true,
                    columns: {
                        memallocated: {
                            label: 'label.metrics.allocated',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'memnotificationthreshold',
                                disable: 'memdisablethreshold'
                            }
                        },
                        memtotal: {
                            label: 'label.metrics.memory.total'
                        }
                    }
                }
            },
            dataProvider: function(args) {
                var data = {};
                listViewDataProvider(args, data);
                if (args.context.metricsFilterData && args.context.metricsFilterData.key && args.context.metricsFilterData.value) {
                    data[args.context.metricsFilterData.key] = args.context.metricsFilterData.value;
                }
                $.ajax({
                    url: createURL('listClusters'),
                    data: data,
                    success: function(json) {
                        var items = json.listclustersresponse.cluster;
                        if (items) {
                            $.each(items, function(idx, cluster) {
                                items[idx].hosts = 0;
                                items[idx].hostsUp = 0;
                                items[idx].cpuusedavg = 0.0;
                                items[idx].cpumaxdev = 0.0;
                                items[idx].cpuallocated = 0.0;
                                items[idx].cputotal = 0.0;
                                items[idx].maxCpuUsed = 0;
                                items[idx].memusedavg = 0.0;
                                items[idx].memmaxdev = 0.0;
                                items[idx].memallocated = 0.0;
                                items[idx].memtotal = 0.0;
                                items[idx].maxMemUsed = 0.0;

                                // Threshold color coding
                                items[idx].cpunotificationthreshold = 75.0;
                                items[idx].cpudisablethreshold = 95.0;
                                items[idx].memnotificationthreshold = 75.0;
                                items[idx].memdisablethreshold = 95.0;

                                $.ajax({
                                    url: createURL('listConfigurations'),
                                    data: {clusterid: cluster.id, listAll: true},
                                    success: function(json) {
                                        if (json.listconfigurationsresponse && json.listconfigurationsresponse.configuration) {
                                            $.each(json.listconfigurationsresponse.configuration, function(i, config) {
                                                switch (config.name) {
                                                    case 'cluster.cpu.allocated.capacity.disablethreshold':
                                                        items[idx].cpudisablethreshold = 100 * parseFloat(config.value);
                                                        break;
                                                    case 'cluster.cpu.allocated.capacity.notificationthreshold':
                                                        items[idx].cpunotificationthreshold = 100 * parseFloat(config.value);
                                                        break;
                                                    case 'cluster.memory.allocated.capacity.disablethreshold':
                                                        items[idx].memdisablethreshold = 100 * parseFloat(config.value);
                                                        break;
                                                    case 'cluster.memory.allocated.capacity.notificationthreshold':
                                                        items[idx].memnotificationthreshold = 100 * parseFloat(config.value);
                                                        break;
                                                }
                                            });
                                        }
                                    },
                                    async: false
                                });

                                $.ajax({
                                    url: createURL('listHosts'),
                                    data: {clusterid: cluster.id, type: 'routing'},
                                    success: function(json) {
                                        if (json && json.listhostsresponse && json.listhostsresponse.host && json.listhostsresponse.count) {
                                            items[idx].hosts += parseInt(json.listhostsresponse.count);
                                            $.each(json.listhostsresponse.host, function(i, host) {
                                                if (host.state == 'Up') {
                                                    items[idx].hostsUp += 1;
                                                }
                                                if (host.hasOwnProperty('cpuused')) {
                                                    var hostCpuUsage = parseFloat(host.cpuused);
                                                    items[idx].cpuusedavg += hostCpuUsage;
                                                    if (hostCpuUsage > items[idx].maxCpuUsed) {
                                                        items[idx].maxCpuUsed = hostCpuUsage;
                                                    }
                                                }

                                                if (host.hasOwnProperty('cpuallocated')) {
                                                    items[idx].cpuallocated += parseFloat(host.cpuallocated.replace('%', ''));
                                                }

                                                if (host.hasOwnProperty('memoryused')) {
                                                    var hostMemoryUsage = 100.0 * parseFloat(host.memoryused) /  parseFloat(host.memorytotal);
                                                    items[idx].memusedavg += hostMemoryUsage;
                                                    if (hostMemoryUsage > items[idx].maxMemUsed) {
                                                        items[idx].maxMemUsed = hostMemoryUsage;
                                                    }
                                                }

                                                if (host.hasOwnProperty('memoryallocated')) {
                                                    items[idx].memallocated += parseFloat(100.0 * parseFloat(host.memoryallocated)/parseFloat(host.memorytotal));
                                                }
                                            });
                                        }
                                    },
                                    async: false
                                });

                                $.ajax({
                                    url: createURL('listCapacity'),
                                    data: {clusterid: cluster.id},
                                    success: function(json) {
                                        if (json && json.listcapacityresponse && json.listcapacityresponse.capacity) {
                                            $.each(json.listcapacityresponse.capacity, function(i, capacity) {
                                                // CPU
                                                if (capacity.type == 1) {
                                                    items[idx].cputotal = parseInt(capacity.capacitytotal)/1000.0;
                                                }
                                                // Memory
                                                if (capacity.type == 0) {
                                                    items[idx].memtotal = parseInt(capacity.capacitytotal)/(1024.0*1024.0*1024.0);
                                                }
                                            });
                                        }
                                    },
                                    async: false
                                });

                                if (items[idx].hosts != 0) {
                                    items[idx].cpuusedavg = (items[idx].cpuusedavg / items[idx].hosts);
                                    items[idx].cpumaxdev = (items[idx].maxCpuUsed - items[idx].cpuusedavg);
                                    items[idx].cpuallocated = (items[idx].cpuallocated / items[idx].hosts);

                                    items[idx].memusedavg = (items[idx].memusedavg / items[idx].hosts);
                                    items[idx].memmaxdev = (items[idx].maxMemUsed - items[idx].memusedavg);
                                    items[idx].memallocated = (items[idx].memallocated / items[idx].hosts);
                                }

                                // Format data
                                items[idx].cpuusedavg = (items[idx].cpuusedavg).toFixed(2) + "%";
                                items[idx].cpumaxdev = (items[idx].cpumaxdev).toFixed(2) + "%";
                                items[idx].cpuallocated = (items[idx].cpuallocated).toFixed(2) + "%";
                                items[idx].cputotal = (items[idx].cputotal).toFixed(2) + " Ghz";

                                items[idx].memusedavg = (items[idx].memusedavg).toFixed(2) + "%";
                                items[idx].memmaxdev = (items[idx].memmaxdev).toFixed(2) + "%";
                                items[idx].memallocated = (items[idx].memallocated).toFixed(2) + "%";
                                items[idx].memtotal = (items[idx].memtotal).toFixed(2) + " GB";
                                items[idx].hosts = items[idx].hostsUp + ' / ' + items[idx].hosts;

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
                        }
                        args.response.success({
                            data: items
                        });
                    }
                });
            },
            browseBy: {
                filterBy: 'clusterid',
                resource: 'hosts'
            },
            detailView: cloudStack.sections.system.subsections.clusters.listView.detailView
        }
    };


    // Hosts Metrics
    cloudStack.sections.metrics.hosts = {
        title: 'label.metrics',
        listView: {
            id: 'hosts',
            fields: {
                name: {
                    label: 'label.metrics.name'
                },
                state: {
                    label: 'label.metrics.state',
                    converter: function (str) {
                        // For localization
                        return str;
                    },
                    indicator: {
                        'Up': 'on',
                        'Down': 'off',
                        'Disconnected': 'off',
                        'Removed': 'off',
                        'Error': 'off',
                        'Connecting': 'transition',
                        'Rebalancing': 'transition',
                        'Alert': 'warning'
                    },
                    compact: true
                },
                instances: {
                    label: 'label.instances'
                },
                cpuused: {
                    label: 'label.metrics.cpu.usage',
                    collapsible: true,
                    columns: {
                        cores: {
                            label: 'label.metrics.num.cpu.cores'
                        },
                        cputotal: {
                            label: 'label.metrics.cpu.total'
                        },
                        cpuusedavg: {
                            label: 'label.metrics.cpu.used.avg',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'cpunotificationthreshold',
                                disable: 'cpudisablethreshold'
                            }
                        },
                        cpuallocated: {
                            label: 'label.metrics.allocated',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'cpuallocatednotificationthreshold',
                                disable: 'cpuallocateddisablethreshold'
                            }
                        }
                    }
                },
                memused: {
                    label: 'label.metrics.memory.usage',
                    collapsible: true,
                    columns: {
                        memtotal: {
                            label: 'label.metrics.memory.total'
                        },
                        memusedavg: {
                            label: 'label.metrics.memory.used.avg',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'memnotificationthreshold',
                                disable: 'memdisablethreshold'
                            }
                        },
                        memallocated: {
                            label: 'label.metrics.allocated',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'memallocatednotificationthreshold',
                                disable: 'memallocateddisablethreshold'
                            }
                        }
                    }
                },
                network: {
                    label: 'label.metrics.network.usage',
                    collapsible: true,
                    columns: {
                        networkread: {
                            label: 'label.metrics.network.read'
                        },
                        networkwrite: {
                            label: 'label.metrics.network.write'
                        }
                    }
                }
            },
            dataProvider: function(args) {
                var data = {};
                data.type = 'routing';
                listViewDataProvider(args, data);
                if (args.context.metricsFilterData && args.context.metricsFilterData.key && args.context.metricsFilterData.value) {
                    data[args.context.metricsFilterData.key] = args.context.metricsFilterData.value;
                }
                $.ajax({
                    url: createURL('listHosts'),
                    data: data,
                    success: function(json) {
                        var items = json.listhostsresponse.host;
                        if (items) {
                            $.each(items, function(idx, host) {
                                items[idx].cores = host.cpunumber;
                                items[idx].cputotal = (parseFloat(host.cpunumber) * parseFloat(host.cpuspeed) / 1000.0).toFixed(2);
                                if (host.cpuused) {
                                    items[idx].cpuusedavg = (parseFloat(host.cpuused) * items[idx].cputotal / 100.0).toFixed(2) + ' Ghz';
                                } else {
                                    items[idx].cpuusedavg = '';
                                }
                                items[idx].cpuallocated = (parseFloat(host.cpuallocated) * items[idx].cputotal / 100.0).toFixed(2) + ' Ghz';
                                items[idx].memtotal = (parseFloat(host.memorytotal)/(1024.0*1024.0*1024.0)).toFixed(2) + ' GB';
                                items[idx].memallocated = (parseFloat(host.memoryallocated)/(1024.0*1024.0*1024.0)).toFixed(2) + ' GB';
                                if (host.memoryused) {
                                    items[idx].memusedavg = (parseFloat(host.memoryused)/(1024.0*1024.0*1024.0)).toFixed(2) + ' GB';
                                } else {
                                    items[idx].memusedavg = '';
                                }
                                if (host.networkkbsread && host.networkkbswrite) {
                                    items[idx].networkread = (parseFloat(host.networkkbsread)/(1024.0*1024.0)).toFixed(2) + ' GB';
                                    items[idx].networkwrite = (parseFloat(host.networkkbswrite)/(1024.0*1024.0)).toFixed(2) + ' GB';
                                } else {
                                    items[idx].networkread = '';
                                    items[idx].networkwrite = '';
                                }

                                var cpuOverCommit = 1.0;
                                var memOverCommit = 1.0;
                                $.ajax({
                                    url: createURL('listClusters'),
                                    data: {clusterid: host.clusterid, listAll: true},
                                    success: function(json) {
                                        if (json.listclustersresponse && json.listclustersresponse.cluster) {
                                            var cluster = json.listclustersresponse.cluster[0];
                                            cpuOverCommit = parseFloat(cluster.cpuovercommitratio);
                                            memOverCommit = parseFloat(cluster.memoryovercommitratio);
                                        }
                                    },
                                    async: false
                                });

                                // Threshold color coding
                                items[idx].cpunotificationthreshold = 0.75 * parseFloat(items[idx].cputotal);
                                items[idx].cpudisablethreshold = 0.95 * parseFloat(items[idx].cputotal);
                                items[idx].cpuallocatednotificationthreshold = 0.75 * cpuOverCommit * parseFloat(items[idx].cputotal);
                                items[idx].cpuallocateddisablethreshold = 0.95 * cpuOverCommit * parseFloat(items[idx].cputotal);

                                items[idx].memnotificationthreshold = 0.75 * parseFloat(items[idx].memtotal);
                                items[idx].memdisablethreshold = 0.95 * parseFloat(items[idx].memtotal);
                                items[idx].memallocatednotificationthreshold = 0.75 * memOverCommit * parseFloat(items[idx].memtotal);
                                items[idx].memallocateddisablethreshold = 0.95 * memOverCommit * parseFloat(items[idx].memtotal);

                                $.ajax({
                                    url: createURL('listConfigurations'),
                                    data: {clusterid: host.clusterid, listAll: true},
                                    success: function(json) {
                                        if (json.listconfigurationsresponse && json.listconfigurationsresponse.configuration) {
                                            $.each(json.listconfigurationsresponse.configuration, function(i, config) {
                                                switch (config.name) {
                                                    case 'cluster.cpu.allocated.capacity.disablethreshold':
                                                        items[idx].cpudisablethreshold = parseFloat(config.value) * parseFloat(items[idx].cputotal);
                                                        items[idx].cpuallocateddisablethreshold = parseFloat(config.value) * cpuOverCommit * parseFloat(items[idx].cputotal);
                                                        break;
                                                    case 'cluster.cpu.allocated.capacity.notificationthreshold':
                                                        items[idx].cpunotificationthreshold = parseFloat(config.value) * parseFloat(items[idx].cputotal);
                                                        items[idx].cpuallocatednotificationthreshold = parseFloat(config.value) * cpuOverCommit * parseFloat(items[idx].cputotal);
                                                        break;
                                                    case 'cluster.memory.allocated.capacity.disablethreshold':
                                                        items[idx].memdisablethreshold = parseFloat(config.value) * parseFloat(items[idx].memtotal);
                                                        items[idx].memallocateddisablethreshold = parseFloat(config.value) * memOverCommit * parseFloat(items[idx].memtotal);
                                                        break;
                                                    case 'cluster.memory.allocated.capacity.notificationthreshold':
                                                        items[idx].memnotificationthreshold = parseFloat(config.value) * parseFloat(items[idx].memtotal);
                                                        items[idx].memallocatednotificationthreshold = parseFloat(config.value) * memOverCommit * parseFloat(items[idx].memtotal);
                                                        break;
                                                }
                                            });
                                        }
                                    },
                                    async: false
                                });


                                items[idx].cputotal = items[idx].cputotal + ' Ghz (x' + cpuOverCommit + ')';
                                items[idx].memtotal = items[idx].memtotal + ' (x' + memOverCommit + ')';

                                items[idx].instances = 0;
                                items[idx].instancesUp = 0;
                                $.ajax({
                                    url: createURL('listVirtualMachines'),
                                    data: {hostid: host.id, listAll: true},
                                    success: function(json) {
                                        if (json && json.listvirtualmachinesresponse && json.listvirtualmachinesresponse.virtualmachine) {
                                            var vms = json.listvirtualmachinesresponse.virtualmachine;
                                            if (vms) {
                                                $.each(vms, function(_, vm) {
                                                    items[idx].instances += 1;
                                                    if (vm.state == 'Running') {
                                                        items[idx].instancesUp += 1;
                                                    }
                                                });
                                            }
                                        }
                                    },
                                    async: false
                                });
                                items[idx].instances = items[idx].instancesUp + ' / ' + items[idx].instances;
                            });
                        }
                        args.response.success({
                            data: items
                        });
                    }
                });
            },
            browseBy: {
                filterBy: 'hostid',
                resource: 'vms'
            },
            detailView: cloudStack.sections.system.subsections.hosts.listView.detailView
        }
    };


    // VMs Metrics
    cloudStack.sections.metrics.instances = {
        title: 'label.metrics',
        listView: {
            id: 'instances',
            fields: {
                name: {
                    label: 'label.metrics.name'
                },
                state: {
                    label: 'label.metrics.state',
                    converter: function (str) {
                        // For localization
                        return str;
                    },
                    indicator: {
                        'Running': 'on',
                        'Stopped': 'off',
                        'Error': 'off',
                        'Destroyed': 'off',
                        'Expunging': 'off',
                        'Stopping': 'transition',
                        'Starting': 'transition',
                        'Migrating': 'transition',
                        'Shutdowned': 'warning'
                    },
                    compact: true
                },
                ipaddress: {
                    label: 'label.ip.address'
                },
                zonename: {
                    label: 'label.zone'
                },
                cpuused: {
                    label: 'label.metrics.cpu.usage',
                    collapsible: true,
                    columns: {
                        cores: {
                            label: 'label.metrics.num.cpu.cores'
                        },
                        cputotal: {
                            label: 'label.metrics.cpu.total'
                        },
                        cpuused: {
                            label: 'label.metrics.cpu.used.avg'
                        }
                    }
                },
                memused: {
                    label: 'label.metrics.memory.usage',
                    collapsible: true,
                    columns: {
                        memallocated: {
                            label: 'label.metrics.allocated'
                        }
                    }
                },
                network: {
                    label: 'label.metrics.network.usage',
                    collapsible: true,
                    columns: {
                        networkread: {
                            label: 'label.metrics.network.read'
                        },
                        networkwrite: {
                            label: 'label.metrics.network.write'
                        }
                    }
                },
                disk: {
                    label: 'label.metrics.disk.usage',
                    collapsible: true,
                    columns: {
                        diskread: {
                            label: 'label.metrics.disk.read'
                        },
                        diskwrite: {
                            label: 'label.metrics.disk.write'
                        },
                        diskiopstotal: {
                            label: 'label.metrics.disk.iops.total'
                        }
                    }
                }
            },
            dataProvider: function(args) {
                var data = {};
                listViewDataProvider(args, data);
                if (args.context.metricsFilterData && args.context.metricsFilterData.key && args.context.metricsFilterData.value) {
                    data[args.context.metricsFilterData.key] = args.context.metricsFilterData.value;
                }
                $.ajax({
                    url: createURL('listVirtualMachines'),
                    data: data,
                    success: function(json) {
                        var items = [];
                        if (json && json.listvirtualmachinesresponse && json.listvirtualmachinesresponse.virtualmachine) {
                            items = json.listvirtualmachinesresponse.virtualmachine;
                            $.each(items, function(idx, vm) {
                                items[idx].cores = vm.cpunumber;
                                items[idx].cputotal = (parseFloat(vm.cpunumber) * parseFloat(vm.cpuspeed) / 1000.0).toFixed(1) + ' Ghz';
                                items[idx].cpuusedavg = vm.cpuused;
                                items[idx].cpuallocated = vm.cpuallocated;
                                items[idx].memallocated = (parseFloat(vm.memory)/1024.0).toFixed(2) + ' GB';
                                items[idx].networkread = (parseFloat(vm.networkkbsread)/(1024.0)).toFixed(2) + ' MB';
                                items[idx].networkwrite = (parseFloat(vm.networkkbswrite)/(1024.0)).toFixed(2) + ' MB';
                                items[idx].diskread = (parseFloat(vm.diskkbsread)/(1024.0)).toFixed(2) + ' MB';
                                items[idx].diskwrite = (parseFloat(vm.diskkbswrite)/(1024.0)).toFixed(2) + ' MB';
                                items[idx].diskiopstotal = parseFloat(vm.diskioread) + parseFloat(vm.diskiowrite);
                                if (vm.nic && vm.nic.length > 0 && vm.nic[0].ipaddress) {
                                    items[idx].ipaddress = vm.nic[0].ipaddress;
                                }

                                var keys = [{'cpuused': 'cpuusedavg'},
                                            {'networkkbsread': 'networkread'},
                                            {'networkkbswrite': 'networkwrite'},
                                            {'diskkbsread': 'diskread'},
                                            {'diskkbswrite': 'diskwrite'},
                                            {'diskioread': 'diskiopstotal'}];
                                for (keyIdx in keys) {
                                    var map = keys[keyIdx];
                                    var key = Object.keys(map)[0];
                                    var uiKey = map[key];
                                    if (!vm.hasOwnProperty(key)) {
                                        items[idx][uiKey] = '';
                                    }
                                }
                            });
                        }
                        args.response.success({
                            data: items
                        });
                    }
                });
            },
            browseBy: {
                filterBy: 'virtualmachineid',
                resource: 'volumes'
            },
            detailView: cloudStack.sections.instances.listView.detailView
        }
    };


    // Volumes Metrics
    cloudStack.sections.metrics.volumes = {
        title: 'label.metrics',
        listView: {
            id: 'volumes',
            fields: {
                name: {
                    label: 'label.metrics.name'
                },
                state: {
                    label: 'label.metrics.state',
                    converter: function (str) {
                        // For localization
                        return str;
                    },
                    indicator: {
                        'Allocated': 'transition',
                        'Creating': 'transition',
                        'Ready': 'on',
                        'Destroy': 'off',
                        'Expunging': 'off',
                        'Migrating': 'warning',
                        'UploadOp': 'transition',
                        'Snapshotting': 'warning'
                    },
                    compact: true
                },
                vmname: {
                    label: 'label.metrics.vm.name'
                },
                disksize: {
                    label: 'label.metrics.disk.size'
                },
                storagetype: {
                    label: 'label.metrics.disk.storagetype'
                },
                storagepool: {
                    label: 'label.metrics.storagepool'
                }
            },
            dataProvider: function(args) {
                var data = {listAll: true};
                listViewDataProvider(args, data);
                if (args.context.metricsFilterData && args.context.metricsFilterData.key && args.context.metricsFilterData.value) {
                    data[args.context.metricsFilterData.key] = args.context.metricsFilterData.value;
                }
                $.ajax({
                    url: createURL('listVolumes'),
                    data: data,
                    success: function(json) {
                        var items = [];
                        if (json && json.listvolumesresponse && json.listvolumesresponse.volume) {
                            items = json.listvolumesresponse.volume;
                            $.each(items, function(idx, volume) {
                                items[idx].name = volume.name;
                                items[idx].state = volume.state;
                                items[idx].vmname = volume.vmname;
                                items[idx].disksize = parseFloat(volume.size)/(1024.0*1024.0*1024.0) + ' GB';
                                items[idx].storagetype = volume.storagetype.replace(/\w\S*/g, function(txt){return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();}) + ' (' + volume.type + ')';
                                if (volume.storage) {
                                    items[idx].storagepool = volume.storage;
                                }
                            });
                        }
                        args.response.success({
                            data: items
                        });
                    }
                });
            },
            detailView: cloudStack.sections.storage.sections.volumes.listView.detailView
        }
    };


    // Storage Pool Metrics
    cloudStack.sections.metrics.storagepool = {
        title: 'label.metrics',
        listView: {
            id: 'primarystorages',
            fields: {
                name: {
                    label: 'label.metrics.name'
                },
                property: {
                    label: 'label.metrics.property',
                    collapsible: true,
                    columns: {
                        state: {
                            label: 'label.metrics.state',
                            converter: function (str) {
                                // For localization
                                return str;
                            },
                            indicator: {
                                'Up': 'on',
                                'Down': 'off',
                                'Removed': 'off',
                                'ErrorInMaintenance': 'off',
                                'PrepareForMaintenance': 'transition',
                                'CancelMaintenance': 'warning',
                                'Maintenance': 'warning'
                            },
                            compact: true
                        },
                        scope: {
                            label: 'label.metrics.scope'
                        },
                        type: {
                            label: 'label.metrics.disk.storagetype'
                        }
                    }
                },
                disk: {
                    label: 'label.metrics.disk',
                    collapsible: true,
                    columns: {
                        disksizeused: {
                            label: 'label.metrics.disk.used',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'storagenotificationthreshold',
                                disable: 'storagedisablethreshold'
                            }
                        },
                        disksizetotal: {
                            label: 'label.metrics.disk.total'
                        },
                        disksizeallocated: {
                            label: 'label.metrics.disk.allocated',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'storageallocatednotificationthreshold',
                                disable: 'storageallocateddisablethreshold'
                            }
                        },
                        disksizeunallocated: {
                            label: 'label.metrics.disk.unallocated'
                        }
                    }
                }
            },
            dataProvider: function(args) {
                var data = {};
                listViewDataProvider(args, data);
                if (args.context.metricsFilterData && args.context.metricsFilterData.key && args.context.metricsFilterData.value) {
                    data[args.context.metricsFilterData.key] = args.context.metricsFilterData.value;
                }
                $.ajax({
                    url: createURL('listStoragePools'),
                    data: data,
                    success: function(json) {
                        var items = [];
                        if (json && json.liststoragepoolsresponse && json.liststoragepoolsresponse.storagepool) {
                            items = json.liststoragepoolsresponse.storagepool;
                            $.each(items, function(idx, pool) {
                                items[idx].name = pool.name;
                                items[idx].state = pool.state;
                                items[idx].scope = pool.scope;
                                items[idx].type = pool.type;
                                items[idx].overprovisionfactor = parseFloat(pool.overprovisionfactor);
                                if (pool.disksizeused) {
                                    items[idx].disksizeused = (parseFloat(pool.disksizeused)/(1024.0*1024.0*1024.0)).toFixed(2) + ' GB';
                                } else {
                                    items[idx].disksizeused = '';
                                }
                                items[idx].disksizetotal = parseFloat(pool.disksizetotal);
                                items[idx].disksizeallocated = parseFloat(pool.disksizeallocated);
                                items[idx].disksizeunallocated = (items[idx].overprovisionfactor * items[idx].disksizetotal) - items[idx].disksizeallocated;

                                // Format presentation
                                items[idx].disksizetotal = (items[idx].disksizetotal/(1024.0*1024.0*1024.0)).toFixed(2) + ' GB (x' + items[idx].overprovisionfactor + ')';
                                items[idx].disksizeallocated = (items[idx].disksizeallocated/(1024.0*1024.0*1024.0)).toFixed(2) + ' GB';
                                items[idx].disksizeunallocated = (items[idx].disksizeunallocated/(1024.0*1024.0*1024.0)).toFixed(2) + ' GB';

                                // Threshold color coding
                                items[idx].storagenotificationthreshold = 0.75 * parseFloat(items[idx].disksizetotal);
                                items[idx].storagedisablethreshold = 0.95 * parseFloat(items[idx].disksizetotal);
                                items[idx].storageallocatednotificationthreshold = 0.75 * parseFloat(items[idx].disksizetotal) * items[idx].overprovisionfactor;
                                items[idx].storageallocateddisablethreshold = 0.95 * parseFloat(items[idx].disksizetotal) * items[idx].overprovisionfactor;


                                var getThresholds = function(data, items, idx) {
                                    data.listAll = true;
                                    $.ajax({
                                        url: createURL('listConfigurations'),
                                        data: data,
                                        success: function(json) {
                                            if (json.listconfigurationsresponse && json.listconfigurationsresponse.configuration) {
                                                $.each(json.listconfigurationsresponse.configuration, function(i, config) {
                                                    switch (config.name) {
                                                        case 'cluster.storage.allocated.capacity.notificationthreshold':
                                                            items[idx].storageallocatednotificationthreshold = parseFloat(config.value) * items[idx].overprovisionfactor * parseFloat(items[idx].disksizetotal);
                                                            break;
                                                        case 'cluster.storage.capacity.notificationthreshold':
                                                            items[idx].storagenotificationthreshold = parseFloat(config.value) * parseFloat(items[idx].disksizetotal);
                                                            break;
                                                        case 'pool.storage.allocated.capacity.disablethreshold':
                                                            items[idx].storageallocateddisablethreshold = parseFloat(config.value) * items[idx].overprovisionfactor * parseFloat(items[idx].disksizetotal);
                                                            break;
                                                        case 'pool.storage.capacity.disablethreshold':
                                                            items[idx].storagedisablethreshold = parseFloat(config.value) * parseFloat(items[idx].disksizetotal);
                                                            break;
                                                    }
                                                });
                                            }
                                        },
                                        async: false
                                    });
                                };
                                // Update global and cluster level thresholds
                                getThresholds({}, items, idx);
                                getThresholds({clusterid: pool.clusterid}, items, idx);
                            });
                        }
                        args.response.success({
                            data: items
                        });
                    }
                });
            },
            browseBy: {
                filterBy: 'storageid',
                resource: 'volumes'
            },
            detailView: cloudStack.sections.system.subsections['primary-storage'].listView.detailView
        }
    };

})(cloudStack);
