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
                        cpuused: {
                            label: 'label.metrics.cpu.used.avg',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'cputhreshold',
                                disable: 'cpudisablethreshold'
                            }
                        },
                        cpumaxdeviation: {
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
                                notification: 'cpuallocatedthreshold',
                                disable: 'cpuallocateddisablethreshold'
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
                        memoryused: {
                            label: 'label.metrics.memory.used.avg',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'memorythreshold',
                                disable: 'memorydisablethreshold'
                            }
                        },
                        memorymaxdeviation: {
                            label: 'label.metrics.memory.max.dev'
                        }
                    }
                },
                memallocated: {
                    label: 'label.metrics.memory.allocated',
                    collapsible: true,
                    columns: {
                        memoryallocated: {
                            label: 'label.metrics.allocated',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'memoryallocatedthreshold',
                                disable: 'memoryallocateddisablethreshold'
                            }
                        },
                        memorytotal: {
                            label: 'label.metrics.memory.total'
                        }
                    }
                }
            },
            dataProvider: function(args) {
                var data = {};
                listViewDataProvider(args, data);
                $.ajax({
                    url: createURL('listZonesMetrics'),
                    data: data,
                    success: function(json) {
                        args.response.success({
                            data: json.listzonesmetricsresponse.zone
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
                        cpuused: {
                            label: 'label.metrics.cpu.used.avg',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'cputhreshold',
                                disable: 'cpudisablethreshold'
                            }
                        },
                        cpumaxdeviation: {
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
                                notification: 'cpuallocatedthreshold',
                                disable: 'cpuallocateddisablethreshold'
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
                        memoryused: {
                            label: 'label.metrics.memory.used.avg',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'memorythreshold',
                                disable: 'memorydisablethreshold'
                            }
                        },
                        memorymaxdeviation: {
                            label: 'label.metrics.memory.max.dev'
                        }
                    }
                },
                memallocated: {
                    label: 'label.metrics.memory.allocated',
                    collapsible: true,
                    columns: {
                        memoryallocated: {
                            label: 'label.metrics.allocated',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'memoryallocatedthreshold',
                                disable: 'memoryallocateddisablethreshold'
                            }
                        },
                        memorytotal: {
                            label: 'label.metrics.memory.total'
                        }
                    }
                }
            },
            dataProvider: function(args) {
                var data = {};
                listViewDataProvider(args, data);

                if ("zones" in args.context && args.context.zones[0]) {
                    data['zoneid'] = args.context.zones[0].id;
                }

                if ("pods" in args.context && args.context.pods[0]) {
                    data['podid'] = args.context.pods[0].id;
                }

                if (args.context.metricsFilterData && args.context.metricsFilterData.key && args.context.metricsFilterData.value) {
                    data[args.context.metricsFilterData.key] = args.context.metricsFilterData.value;
                }

                $.ajax({
                    url: createURL('listClustersMetrics'),
                    data: data,
                    success: function(json) {
                        args.response.success({
                            data: json.listclustersmetricsresponse.cluster
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
                        'Alert': 'warning'
                    },
                    compact: true
                },
                powerstate: {
                    label: 'label.metrics.outofbandmanagementpowerstate',
                    converter: function (str) {
                        // For localization
                        return str;
                    },
                    indicator: {
                        'On': 'on',
                        'Off': 'off',
                        'Unknown': 'warning'
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
                        cpunumber: {
                            label: 'label.metrics.num.cpu.cores',
                        },
                        cputotalghz: {
                            label: 'label.metrics.cpu.total'
                        },
                        cpuusedghz: {
                            label: 'label.metrics.cpu.used.avg',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'cputhreshold',
                                disable: 'cpudisablethreshold'
                            }
                        },
                        cpuallocatedghz: {
                            label: 'label.metrics.allocated',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'cpuallocatedthreshold',
                                disable: 'cpuallocateddisablethreshold'
                            }
                        }
                    }
                },
                memused: {
                    label: 'label.metrics.memory.usage',
                    collapsible: true,
                    columns: {
                        memorytotalgb: {
                            label: 'label.metrics.memory.total'
                        },
                        memoryusedgb: {
                            label: 'label.metrics.memory.used.avg',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'memorythreshold',
                                disable: 'memorydisablethreshold'
                            }
                        },
                        memoryallocatedgb: {
                            label: 'label.metrics.allocated',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'memoryallocatedthreshold',
                                disable: 'memoryallocateddisablethreshold'
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

                if (!args.context.instances) {
                    if ("zones" in args.context && args.context.zones[0]) {
                        data['zoneid'] = args.context.zones[0].id;
                    }

                    if ("pods" in args.context && args.context.pods[0]) {
                        data['podid'] = args.context.pods[0].id;
                    }

                    if ("clusters" in args.context && args.context.clusters[0]) {
                        data['clusterid'] = args.context.clusters[0].id;
                    }
                } else {
                    if (args.context.instances[0]) {
                        data['id'] = args.context.instances[0].hostid;
                    }
                }

                if (args.context.metricsFilterData && args.context.metricsFilterData.key && args.context.metricsFilterData.value) {
                    data[args.context.metricsFilterData.key] = args.context.metricsFilterData.value;
                }

                $.ajax({
                    url: createURL('listHostsMetrics'),
                    data: data,
                    success: function(json) {
                        args.response.success({
                            data: json.listhostsmetricsresponse.host
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
                        'Stopping': 'warning',
                        'Shutdown': 'warning'
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
                        cpunumber: {
                            label: 'label.metrics.num.cpu.cores',
                        },
                        cputotal: {
                            label: 'label.metrics.cpu.total'
                        },
                        cpuused: {
                            label: 'label.metrics.cpu.used.avg',
                        }
                    }
                },
                memused: {
                    label: 'label.metrics.memory.usage',
                    collapsible: true,
                    columns: {
                        memorytotal: {
                            label: 'label.metrics.allocated'
                        },
                        memoryused: {
                            label: 'label.metrics.memory.used.avg'
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
                        diskioread: {
                            label: 'label.metrics.disk.read'
                        },
                        diskiowrite: {
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

                if ("hosts" in args.context && args.context.hosts[0]) {
                    data['hostid'] = args.context.hosts[0].id;
                }

                if (args.context.metricsFilterData && args.context.metricsFilterData.key && args.context.metricsFilterData.value) {
                    data[args.context.metricsFilterData.key] = args.context.metricsFilterData.value;
                }

                $.ajax({
                    url: createURL('listVirtualMachinesMetrics'),
                    data: data,
                    success: function(json) {
                        json.listvirtualmachinesmetricsresponse.virtualmachine.forEach(function(vm) {
                            var memUsedPercent = (vm.memorykbs && vm.memoryintfreekbs) ? (Math.round((vm.memorykbs - vm.memoryintfreekbs) * 10000 / vm.memorykbs) / 100).toString() + "%" : "";
                            $.extend(vm,{
                                memoryused: memUsedPercent
                            })
                        });
                        args.response.success({
                            data: json.listvirtualmachinesmetricsresponse.virtualmachine
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
            preFilter: function(args) {
                var hiddenFields = [];
                if (!isAdmin()) {
                    hiddenFields.push('physicalsize');
                    hiddenFields.push('storage');
                    hiddenFields.push('storagetype');
                }
                return hiddenFields;
            },
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
                        'Allocated': 'on',
                        'Ready': 'on',
                        'Destroy': 'off',
                        'Expunging': 'off',
                        'Migrating': 'warning',
                        'UploadOp': 'warning',
                        'Snapshotting': 'warning',
                    },
                    compact: true
                },
                vmname: {
                    label: 'label.metrics.vm.name'
                },
                sizegb: {
                    label: 'label.metrics.disk.size'
                },
                physicalsize: {
                    label: 'label.disk.physicalsize',
                    converter: function(args) {
                        if (args == null || args == 0)
                            return "";
                        else
                            return cloudStack.converters.convertBytes(args);
                    }
                },
                utilization: {
                    label: 'label.disk.utilisation'
                },
                storagetype: {
                    label: 'label.metrics.disk.storagetype'
                },
                storage: {
                    label: 'label.metrics.storagepool'
                },
                disk: {
                    label: 'label.metrics.disk.usage',
                    collapsible: true,
                    columns: {
                        diskioread: {
                            label: 'label.metrics.disk.read'
                        },
                        diskiowrite: {
                            label: 'label.metrics.disk.write'
                        },
                        diskiopstotal: {
                            label: 'label.metrics.disk.iops.total'
                        }
                    }
                }
            },
            dataProvider: function(args) {
                var data = {listAll: true};
                listViewDataProvider(args, data);

                if ("instances" in args.context && args.context.instances[0]) {
                    data['virtualmachineid'] = args.context.instances[0].id;
                }

                if ("primarystorages" in args.context && args.context.primarystorages[0]) {
                    data['storageid'] = args.context.primarystorages[0].id;
                }

                if (args.context.metricsFilterData && args.context.metricsFilterData.key && args.context.metricsFilterData.value) {
                    data[args.context.metricsFilterData.key] = args.context.metricsFilterData.value;
                }

                $.ajax({
                    url: createURL('listVolumesMetrics'),
                    data: data,
                    success: function(json) {
                        args.response.success({
                            data: json.listvolumesmetricsresponse.volume
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
                                'ErrorInPrepareForMaintenance': 'warning',
                                'PrepareForMaintenance': 'warning',
                                'CancelMaintenance': 'warning',
                                'Maintenance': 'warning',
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
                        disksizeusedgb: {
                            label: 'label.metrics.disk.used',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'storageusagethreshold',
                                disable: 'storageusagedisablethreshold'
                            }
                        },
                        disksizetotalgb: {
                            label: 'label.metrics.disk.total'
                        },
                        disksizeallocatedgb: {
                            label: 'label.metrics.disk.allocated',
                            thresholdcolor: true,
                            thresholds: {
                                notification: 'storageallocatedthreshold',
                                disable: 'storageallocateddisablethreshold'
                            }
                        },
                        disksizeunallocatedgb: {
                            label: 'label.metrics.disk.unallocated'
                        }
                    }
                }
            },
            dataProvider: function(args) {
                var data = {};
                listViewDataProvider(args, data);

                if ("zones" in args.context && args.context.zones[0]) {
                    data['zoneid'] = args.context.zones[0].id;
                }

                if ("pods" in args.context && args.context.pods[0]) {
                    data['podid'] = args.context.pods[0].id;
                }

                if ("clusters" in args.context && args.context.clusters[0]) {
                    data['clusterid'] = args.context.clusters[0].id;
                }

                if (args.context.metricsFilterData && args.context.metricsFilterData.key && args.context.metricsFilterData.value) {
                    data[args.context.metricsFilterData.key] = args.context.metricsFilterData.value;
                }

                $.ajax({
                    url: createURL('listStoragePoolsMetrics'),
                    data: data,
                    success: function(json) {
                        args.response.success({
                            data: json.liststoragepoolsmetricsresponse.storagepool
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
