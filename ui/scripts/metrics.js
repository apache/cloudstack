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
        }
    };


    // Zones Metrics
    cloudStack.sections.metrics.zones = {
        title: 'label.metrics',
        listView: {
            id: 'zones',
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
                clusters : {
                    label: 'label.clusters'
                },
                cpuused: {
                    label: 'label.cpu.usage',
                    collapsible: true,
                    columns: {
                        cpuusedavg: {
                            label: 'label.cpu.used.avg',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'cpunotificationthreshold',
                                disable: 'cpudisablethreshold'
                            }
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
                            label: 'label.allocated',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'cpunotificationthreshold',
                                disable: 'cpudisablethreshold'
                            }
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
                            label: 'label.memory.used.avg',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'memnotificationthreshold',
                                disable: 'memdisablethreshold'
                            }
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
                            label: 'label.allocated',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'memnotificationthreshold',
                                disable: 'memdisablethreshold'
                            }
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
                    url: createURL('listZones'),
                    data: data,
                    success: function(json) {
                        var items = json.listzonesresponse.zone;
                        if (items) {
                            $.each(items, function(idx, zone) {
                                items[idx].clusters = 0;
                                items[idx].hosts = 0;
                                items[idx].cpuusedavg = 0.0;
                                items[idx].cpumaxdev = 0.0;
                                items[idx].cpuallocated = 0.0;
                                items[idx].maxCpuUsed = 0.0;
                                items[idx].memusedavg = 0.0;
                                items[idx].memmaxdev = 0.0;
                                items[idx].memallocated = 0.0;
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
                                        items[idx].clusters += parseInt(json.listclustersresponse.count);
                                        var maxCpuUsed = 0.0;
                                        var maxMemUsed = 0;
                                        $.each(json.listclustersresponse.cluster, function(i, cluster) {
                                            $.ajax({
                                                url: createURL('listHosts'),
                                                data: {clusterid: cluster.id, type: 'routing'},
                                                success: function(json) {
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
                                                },
                                                async: false
                                            });
                                        });
                                    },
                                    async: false
                                });

                                $.ajax({
                                    url: createURL('listCapacity'),
                                    data: {zoneid: zone.id},
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

                                items[idx].cpuusedavg = (items[idx].cpuusedavg / items[idx].hosts);
                                items[idx].cpumaxdev = (items[idx].maxCpuUsed - items[idx].cpuusedavg);
                                items[idx].cpuallocated = (items[idx].cpuallocated / items[idx].hosts);

                                items[idx].memusedavg = (items[idx].memusedavg / items[idx].hosts);
                                items[idx].memmaxdev = (items[idx].maxMemUsed - items[idx].memusedavg);
                                items[idx].memallocated = (items[idx].memallocated / items[idx].hosts);

                                // Format data
                                items[idx].cpuusedavg = (items[idx].cpuusedavg).toFixed(2) + "%";
                                items[idx].cpumaxdev = (items[idx].cpumaxdev).toFixed(2) + "%";
                                items[idx].cpuallocated = (items[idx].cpuallocated).toFixed(2) + "%";

                                items[idx].memusedavg = (items[idx].memusedavg).toFixed(2) + "%";
                                items[idx].memmaxdev = (items[idx].memmaxdev).toFixed(2) + "%";
                                items[idx].memallocated = (items[idx].memallocated).toFixed(2) + "%";


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
                filterBy: 'zoneid',
                resource: 'clusters'
            }
        }
    };


    // Clusters Metrics
    cloudStack.sections.metrics.clusters = {
        title: 'label.metrics',
        listView: {
            id: 'clusters',
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
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'cpunotificationthreshold',
                                disable: 'cpudisablethreshold'
                            }
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
                            label: 'label.allocated',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'cpunotificationthreshold',
                                disable: 'cpudisablethreshold'
                            }
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
                            label: 'label.memory.used.avg',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'memnotificationthreshold',
                                disable: 'memdisablethreshold'
                            }
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
                            label: 'label.allocated',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'memnotificationthreshold',
                                disable: 'memdisablethreshold'
                            }
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
                                items[idx].cpuusedavg = 0.0;
                                items[idx].cpumaxdev = 0.0;
                                items[idx].cpuallocated = 0.0;
                                items[idx].maxCpuUsed = 0;
                                items[idx].memusedavg = 0.0;
                                items[idx].memmaxdev = 0.0;
                                items[idx].memallocated = 0.0;
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

                                items[idx].cpuusedavg = (items[idx].cpuusedavg / items[idx].hosts);
                                items[idx].cpumaxdev = (items[idx].maxCpuUsed - items[idx].cpuusedavg);
                                items[idx].cpuallocated = (items[idx].cpuallocated / items[idx].hosts);

                                items[idx].memusedavg = (items[idx].memusedavg / items[idx].hosts);
                                items[idx].memmaxdev = (items[idx].maxMemUsed - items[idx].memusedavg);
                                items[idx].memallocated = (items[idx].memallocated / items[idx].hosts);

                                // Format data
                                items[idx].cpuusedavg = (items[idx].cpuusedavg).toFixed(2) + "%";
                                items[idx].cpumaxdev = (items[idx].cpumaxdev).toFixed(2) + "%";
                                items[idx].cpuallocated = (items[idx].cpuallocated).toFixed(2) + "%";

                                items[idx].memusedavg = (items[idx].memusedavg).toFixed(2) + "%";
                                items[idx].memmaxdev = (items[idx].memmaxdev).toFixed(2) + "%";
                                items[idx].memallocated = (items[idx].memallocated).toFixed(2) + "%";

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
                        'Up': 'on',
                        'Down': 'off',
                        'Unmanaged': 'off',
                        'Destroyed': 'off',
                        'Disabled': 'off'
                    }
                },
                cpuused: {
                    label: 'label.cpu.usage',
                    collapsible: true,
                    columns: {
                        cores: {
                            label: 'label.num.cpu.cores',
                        },
                        cputotal: {
                            label: 'label.cpu.total.ghz'
                        },
                        cpuusedavg: {
                            label: 'label.cpu.used.avg',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'cpunotificationthreshold',
                                disable: 'cpudisablethreshold'
                            }
                        },
                        cpuallocated: {
                            label: 'label.allocated',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'cpunotificationthreshold',
                                disable: 'cpudisablethreshold'
                            }
                        }
                    }
                },
                memused: {
                    label: 'label.memory.usage',
                    collapsible: true,
                    columns: {
                        memtotal: {
                            label: 'label.memory.total.gb'
                        },
                        memallocated: {
                            label: 'label.allocated',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'memnotificationthreshold',
                                disable: 'memdisablethreshold'
                            }
                        },
                        memusedavg: {
                            label: 'label.memory.used.avg',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'memnotificationthreshold',
                                disable: 'memdisablethreshold'
                            }
                        }
                    }
                },
                network: {
                    label: 'label.network',
                    collapsible: true,
                    columns: {
                        networkread: {
                            label: 'label.network.read'
                        },
                        networkwrite: {
                            label: 'label.network.write'
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
                                items[idx].cpuusedavg = host.cpuused;
                                items[idx].cpuallocated = host.cpuallocated;
                                items[idx].memtotal = (parseFloat(host.memorytotal)/(1024.0*1024.0*1024.0)).toFixed(2) + 'GB';
                                items[idx].memallocated = (parseFloat(host.memoryallocated)/(1024.0*1024.0*1024.0)).toFixed(2) + 'GB';
                                items[idx].memusedavg = (parseFloat(host.memoryused)/(1024.0*1024.0*1024.0)).toFixed(2) + 'GB';
                                items[idx].networkread = (parseFloat(host.networkkbsread)/(1024.0*1024.0)).toFixed(2) + 'GB';
                                items[idx].networkwrite = (parseFloat(host.networkkbswrite)/(1024.0*1024.0)).toFixed(2) + 'GB';

                                // Threshold color coding
                                items[idx].cpunotificationthreshold = 75.0;
                                items[idx].cpudisablethreshold = 95.0;
                                items[idx].memnotificationthreshold = 75.0;
                                items[idx].memdisablethreshold = 95.0;

                                $.ajax({
                                    url: createURL('listConfigurations'),
                                    data: {clusterid: host.clusterid, listAll: true},
                                    success: function(json) {
                                        if (json.listconfigurationsresponse && json.listconfigurationsresponse.configuration) {
                                            $.each(json.listconfigurationsresponse.configuration, function(i, config) {
                                                switch (config.name) {
                                                    case 'cluster.cpu.allocated.capacity.disablethreshold':
                                                        items[idx].cpudisablethreshold = parseFloat(config.value) * items[idx].cputotal;
                                                        break;
                                                    case 'cluster.cpu.allocated.capacity.notificationthreshold':
                                                        items[idx].cpunotificationthreshold = parseFloat(config.value) * items[idx].cputotal;
                                                        break;
                                                    case 'cluster.memory.allocated.capacity.disablethreshold':
                                                        items[idx].memdisablethreshold = parseFloat(config.value) * parseFloat(items[idx].memtotal);
                                                        break;
                                                    case 'cluster.memory.allocated.capacity.notificationthreshold':
                                                        items[idx].memnotificationthreshold = parseFloat(config.value) * parseFloat(items[idx].memtotal);
                                                        break;
                                                }
                                            });
                                        }
                                    },
                                    async: false
                                });

                                var cpuOverCommit = 1.0;
                                var memOverCommit = 1.0;
                                $.ajax({
                                    url: createURL('listClusters'),
                                    data: {clusterid: host.clusterid, listAll: true},
                                    success: function(json) {
                                        if (json.listclustersresponse && json.listclustersresponse.cluster) {
                                            var cluster = json.listclustersresponse.cluster[0];
                                            cpuOverCommit = cluster.cpuovercommitratio;
                                            memOverCommit = cluster.memoryovercommitratio;
                                        }
                                    },
                                    async: false
                                });

                                items[idx].cputotal = items[idx].cputotal + ' (x' + cpuOverCommit + ')';
                                items[idx].memtotal = items[idx].memtotal + ' (x' + memOverCommit + ')';
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
                cpuused: {
                    label: 'label.cpu.usage',
                    collapsible: true,
                    columns: {
                        cores: {
                            label: 'label.num.cpu.cores',
                        },
                        cputotal: {
                            label: 'label.cpu.total.ghz'
                        },
                        cpuused: {
                            label: 'label.cpu.used.avg',
                        }
                    }
                },
                memused: {
                    label: 'label.memory.usage',
                    collapsible: true,
                    columns: {
                        memallocated: {
                            label: 'label.allocated'
                        },
                        memused: {
                            label: 'label.memory.used.avg'
                        }
                    }
                },
                network: {
                    label: 'label.network',
                    collapsible: true,
                    columns: {
                        networkread: {
                            label: 'label.network.read'
                        },
                        networkwrite: {
                            label: 'label.network.write'
                        }
                    }
                },
                disk: {
                    label: 'label.disk.volume',
                    collapsible: true,
                    columns: {
                        diskread: {
                            label: 'label.disk.read.bytes'
                        },
                        diskwrite: {
                            label: 'label.disk.write.bytes'
                        },
                        diskiops: {
                            label: 'label.disk.iops.total'
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
                        var items = json.listvirtualmachinesresponse.virtualmachine;
                        if (items) {
                            $.each(items, function(idx, host) {
                                items[idx].cores = 0;
                                items[idx].cputotal = 0;
                                items[idx].cpuused = 0.0;
                                items[idx].memallocated = 0.0;
                                items[idx].memused = 0.0;
                                items[idx].networkread = 0.0;
                                items[idx].networkwrite = 0.0;
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


})(cloudStack);
