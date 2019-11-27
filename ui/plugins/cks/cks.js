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
(function (cloudStack) {

    var rootCaCert = "";
    var downloadCaCert = function() {
        var blob = new Blob([rootCaCert], {type: 'application/x-x509-ca-cert'});
        var filename = "cloudstack-ca.pem";
        if(window.navigator.msSaveOrOpenBlob) {
            window.navigator.msSaveBlob(blob, filename);
        } else{
            var elem = window.document.createElement('a');
            elem.href = window.URL.createObjectURL(blob);
            elem.download = filename;
            document.body.appendChild(elem)
            elem.click();
            document.body.removeChild(elem);
        }
    };
    var clusterKubeConfig = "";
    var downloadClusterKubeConfig = function() {
        var blob = new Blob([clusterKubeConfig], {type: 'text/plain'});
        var filename = "admin.conf";
        if(window.navigator.msSaveOrOpenBlob) {
            window.navigator.msSaveBlob(blob, filename);
        } else{
            var elem = window.document.createElement('a');
            elem.href = window.URL.createObjectURL(blob);
            elem.download = filename;
            document.body.appendChild(elem)
            elem.click();
            document.body.removeChild(elem);
        }
    };
    cloudStack.plugins.cks = function(plugin) {
        plugin.ui.addSection({
            id: 'cks',
            title: 'Kubernetes Service',
            preFilter: function(args) {
                return true;
            },
            showOnNavigation: true,
            sectionSelect: {
                label: 'label.select-view',
                preFilter: function() {
                    return ['kubernetesclusters', 'kubernetesversions'];
                }
            },
            sections: {
                kubernetesclusters: {
                    id: 'kubernetesclusters',
                    type: 'select',
                    title: "Clusters",
                    listView: {
                        filters: {
                            all: {
                                label: 'ui.listView.filters.all'
                            },
                            running: {
                                label: 'state.Running'
                            },
                            stopped: {
                                label: 'state.Stopped'
                            },
                            destroyed: {
                                label: 'state.Destroyed'
                            }
                        },
                        fields: {
                            name: {
                                label: 'label.name'
                            },
                            zonename: {
                                label: 'label.zone.name'
                            },
                            size : {
                                label: 'label.size'
                            },
                            cpunumber: {
                                label: 'label.num.cpu.cores'
                            },
                            memory: {
                                label: 'label.memory.mb'
                            },
                            state: {
                                label: 'label.state',
                                indicator: {
                                    'Running': 'on',
                                    'Stopped': 'off',
                                    'Destroyed': 'off',
                                    'Error': 'off'
                                }
                            }
                        },
                        advSearchFields: {
                            name: {
                                label: 'label.name'
                            },
                            zoneid: {
                                label: 'label.zone',
                                select: function(args) {
                                    $.ajax({
                                        url: createURL('listZones'),
                                        data: {
                                            listAll: true
                                        },
                                        success: function(json) {
                                            var zones = json.listzonesresponse.zone ? json.listzonesresponse.zone : [];

                                            args.response.success({
                                                data: $.map(zones, function(zone) {
                                                    return {
                                                        id: zone.id,
                                                        description: zone.name
                                                    };
                                                })
                                            });
                                        }
                                    });
                                }
                            },
                        },
                        // List view actions
                        actions: {
                            add: {
                                label: 'Add Kubernetes cluster',
                                createForm: {
                                    title: 'Add Kubernetes cluster',
                                    preFilter: function(args) {
                                        args.$form.find('.form-item[rel=masternodes]').find('input[name=masternodes]').val('1');
                                        args.$form.find('.form-item[rel=size]').find('input[name=size]').val('1');
                                    },
                                    fields: {
                                        name: {
                                            label: 'label.name',
                                            //docID: 'Name of the cluster',
                                            validation: {
                                                required: true
                                            }
                                        },
                                        description: {
                                            label: 'label.description',
                                            //docID: 'helpKubernetesClusterDesc',
                                        },
                                        zone: {
                                            label: 'label.zone',
                                            //docID: 'helpKubernetesClusterZone',
                                            validation: {
                                                required: true
                                            },
                                            select: function(args) {
                                                $.ajax({
                                                    url: createURL("listZones&available=true"),
                                                    dataType: "json",
                                                    async: true,
                                                    success: function(json) {
                                                        var items = [];
                                                        var zoneObjs = json.listzonesresponse.zone;
                                                        if (zoneObjs != null) {
                                                            for (var i = 0; i < zoneObjs.length; i++) {
                                                                items.push({
                                                                    id: zoneObjs[i].id,
                                                                    description: zoneObjs[i].name
                                                                });
                                                            }
                                                        }
                                                        args.response.success({
                                                            data: items
                                                        });
                                                    }
                                                });
                                            }
                                        },
                                        kubernetesversion: {
                                            label: 'Kubernetes version',
                                            dependsOn: ['zone'],
                                            //docID: 'helpKubernetesClusterZone',
                                            validation: {
                                                required: true
                                            },
                                            select: function(args) {
                                                var filterData = { zoneid: args.zone };
                                                $.ajax({
                                                    url: createURL("listKubernetesSupportedVersions"),
                                                    data: filterData,
                                                    dataType: "json",
                                                    async: true,
                                                    url: createURL("listKubernetesSupportedVersions"),
                                                    dataType: "json",
                                                    async: true,
                                                    success: function(json) {
                                                        var items = [];
                                                        var versionObjs = json.listkubernetessupportedversionsresponse.kubernetessupportedversion;
                                                        if (versionObjs != null) {
                                                            for (var i = 0; i < versionObjs.length; i++) {
                                                                if (versionObjs[i].isostate == 'Active') {
                                                                    items.push({
                                                                        id: versionObjs[i].id,
                                                                        description: versionObjs[i].name
                                                                    });
                                                                }
                                                            }
                                                        }
                                                        args.response.success({
                                                            data: items
                                                        });
                                                    }
                                                });
                                            }
                                        },
                                        serviceoffering: {
                                            label: 'label.menu.service.offerings',
                                            //docID: 'helpKubernetesClusterServiceOffering',
                                            validation: {
                                                required: true
                                            },
                                            select: function(args) {
                                                $.ajax({
                                                    url: createURL("listServiceOfferings"),
                                                    dataType: "json",
                                                    async: true,
                                                    success: function(json) {
                                                        var offeringObjs = [];
                                                        var items = json.listserviceofferingsresponse.serviceoffering;
                                                        if (items != null) {
                                                            for (var i = 0; i < items.length; i++) {
                                                                offeringObjs.push({
                                                                    id: items[i].id,
                                                                    description: items[i].name
                                                                });
                                                            }
                                                        }
                                                        args.response.success({
                                                            data: offeringObjs
                                                        });
                                                    }
                                                });
                                            }
                                        },
                                        noderootdisksize: {
                                            label: 'Node root disk size (in GB)',
                                            //docID: 'helpKubernetesClusterNodeRootDiskSize',
                                            validation: {
                                                number: true
                                            }
                                        },
                                        network: {
                                            label: 'label.network',
                                            //docID: 'helpKubernetesClusterNetwork',
                                            select: function(args) {
                                                $.ajax({
                                                    url: createURL("listNetworks"),
                                                    dataType: "json",
                                                    async: true,
                                                    success: function(json) {
                                                        var networkObjs = [];
                                                        networkObjs.push({
                                                            id: "",
                                                            description: ""
                                                        });
                                                        var items = json.listnetworksresponse.network;
                                                        if (items != null) {
                                                            for (var i = 0; i < items.length; i++) {
                                                                networkObjs.push({
                                                                    id: items[i].id,
                                                                    description: items[i].name
                                                                });
                                                            }
                                                        }
                                                        args.response.success({
                                                            data: networkObjs
                                                        });
                                                    }
                                                });
                                            }
                                        },
                                        multimaster: {
                                            label: "HA (Multi-master)",
                                            isBoolean: true,
                                            isChecked: false,
                                        },
                                        masternodes: {
                                            label: 'Master nodes',
                                            //docID: 'helpKubernetesClusterSize',
                                            validation: {
                                                required: true,
                                                naturalnumber: true
                                            },
                                            dependsOn: "multimaster",
                                            isHidden: true,
                                        },
                                        size: {
                                            label: 'Cluster size (Worker nodes)',
                                            //docID: 'helpKubernetesClusterSize',
                                            validation: {
                                                required: true,
                                                naturalnumber: true
                                            },
                                        },
                                        sshkeypair: {
                                            label: 'label.ssh.key.pair',
                                            //docID: 'helpKubernetesClusterSSH',
                                            select: function(args) {
                                                $.ajax({
                                                    url: createURL("listSSHKeyPairs"),
                                                    dataType: "json",
                                                    async: true,
                                                    success: function(json) {
                                                        var keypairObjs = [];
                                                        keypairObjs.push({
                                                            id: "",
                                                            description: ""
                                                        });
                                                        var items = json.listsshkeypairsresponse.sshkeypair;
                                                        if (items != null) {
                                                            for (var i = 0; i < items.length; i++) {
                                                                keypairObjs.push({
                                                                    id: items[i].name,
                                                                    description: items[i].name
                                                                });
                                                            }
                                                        }
                                                        args.response.success({
                                                            data: keypairObjs
                                                        });
                                                    }
                                                });
                                            }
                                        },
                                        supportPrivateRegistry: {
                                            label: 'Private Registry',
                                            isBoolean: true,
                                            isChecked: false,
                                        },
                                        username: {
                                            label: 'label.username',
                                            dependsOn: 'supportPrivateRegistry',
                                            validation: {
                                                required: true
                                            },
                                            isHidden: true
                                        },
                                        password: {
                                            label: 'label.password',
                                            dependsOn: 'supportPrivateRegistry',
                                            validation: {
                                                required: true
                                            },
                                            isHidden: true,
                                            isPassword: true
                                        },
                                        url: {
                                            label: 'label.url',
                                            dependsOn: 'supportPrivateRegistry',
                                            validation: {
                                                required: true
                                            },
                                            isHidden: true,
                                        },
                                        email: {
                                            label: 'label.email',
                                            dependsOn: 'supportPrivateRegistry',
                                            validation: {
                                                required: true
                                            },
                                            isHidden: true,
                                        }
                                    }
                                },

                                action: function(args) {
                                    var data = {
                                        name: args.data.name,
                                        description: args.data.description,
                                        zoneid: args.data.zone,
                                        kubernetesversionid: args.data.kubernetesversion,
                                        serviceofferingid: args.data.serviceoffering,
                                        size: args.data.size,
                                        keypair: args.data.sshkeypair
                                    };

                                    if (args.data.noderootdisksize != null && args.data.noderootdisksize != "" && args.data.noderootdisksize > 0) {
                                        $.extend(data, {
                                            noderootdisksize: args.data.noderootdisksize
                                        });
                                    }

                                    var masterNodes = 1;
                                    if (args.data.multimaster === 'on') {
                                        masterNodes = args.data.masternodes;
                                    }
                                    $.extend(data, {
                                        masternodes: masterNodes
                                    });

                                    if (args.data.supportPrivateRegistry) {
                                        $.extend(data, {
                                            dockerregistryusername: args.data.username,
                                            dockerregistrypassword: args.data.password,
                                            dockerregistryurl: args.data.url,
                                            dockerregistryemail: args.data.email
                                        });
                                    }

                                    if (args.data.network != null && args.data.network.length > 0) {
                                        $.extend(data, {
                                            networkid: args.data.network
                                        });
                                    }
                                    $.ajax({
                                        url: createURL('createKubernetesCluster'),
                                        data: data,
                                        success: function(json) {
                                            var jid = json.createkubernetesclusterresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid
                                                }
                                            });
                                        },
                                        error: function(XMLHttpResponse) {
                                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                            args.response.error(errorMsg);
                                        }
                                    });
                                },


                                messages: {
                                    notification: function(args) {
                                        return 'Kubernetes Cluster Add';
                                    }
                                },
                                notification: {
                                    poll: pollAsyncJobResult
                                }
                            }
                        },
                        dataProvider: function(args) {
                            var data = {
                                    page: args.page,
                                    pagesize: pageSize
                                };
                            listViewDataProvider(args, data);
                            if (args.filterBy != null) { //filter dropdown
                                if (args.filterBy.kind != null) {
                                    switch (args.filterBy.kind) {
                                        case "all":
                                        break;
                                        case "running":
                                        $.extend(data, {
                                            state: 'Running'
                                        });
                                        break;
                                        case "stopped":
                                        $.extend(data, {
                                            state: 'Stopped'
                                        });
                                        break;
                                        case "destroyed":
                                        $.extend(data, {
                                            state: 'Destroyed'
                                        });
                                        break;
                                    }
                                }
                            }

                            $.ajax({
                                url: createURL("listKubernetesClusters"),
                                data: data,
                                dataType: "json",
                                sync: true,
                                success: function(json) {
                                    var items = [];
                                    if (json.listkubernetesclustersresponse.kubernetescluster != null) {
                                        items = json.listkubernetesclustersresponse.kubernetescluster;
                                    }
                                    args.response.success({
                                        actionFilter: cksActionfilter,
                                        data: items
                                    });
                                }
                            });
                        },

                        detailView: {
                            name: 'Kubernetes cluster details',
                            isMaximized: true,
                            actions: {
                                start: {
                                    label: 'Start Kubernetes Cluster',
                                    action: function(args) {
                                        $.ajax({
                                            url: createURL("startKubernetesCluster"),
                                            data: {"id": args.context.kubernetesclusters[0].id},
                                            dataType: "json",
                                            async: true,
                                            success: function(json) {
                                                var jid = json.startkubernetesclusterresponse.jobid;
                                                args.response.success({
                                                    _custom: {
                                                        jobId: jid
                                                    }
                                                });
                                            }
                                        });
                                    },
                                    messages: {
                                        confirm: function(args) {
                                            return 'Please confirm that you want to start this Kubernetes cluster.';
                                        },
                                        notification: function(args) {
                                            return 'Started Kubernetes cluster.';
                                        }
                                    },
                                    notification: {
                                        poll: pollAsyncJobResult
                                    }
                                },
                                stop: {
                                    label: 'Stop Kubernetes Cluster',
                                    action: function(args) {
                                        $.ajax({
                                            url: createURL("stopKubernetesCluster"),
                                            data: {"id": args.context.kubernetesclusters[0].id},
                                            dataType: "json",
                                            async: true,
                                            success: function(json) {
                                                var jid = json.stopkubernetesclusterresponse.jobid;
                                                args.response.success({
                                                    _custom: {
                                                        jobId: jid
                                                    }
                                                });
                                            }
                                        });
                                    },
                                    messages: {
                                        confirm: function(args) {
                                            return 'Please confirm that you want to stop this Kubernetes cluster.';
                                        },
                                        notification: function(args) {
                                            return 'Stopped Kubernetes cluster.';
                                        }
                                    },
                                    notification: {
                                        poll: pollAsyncJobResult
                                    }
                                },
                                destroy: {
                                    label: 'Destroy Cluster',
                                    compactLabel: 'label.destroy',
                                    createForm: {
                                        title: 'Destroy Kubernetes Cluster',
                                        desc: 'Destroy Kubernetes Cluster',
                                        isWarning: true,
                                        fields: {
                                        }
                                    },
                                    messages: {
                                        confirm: function(args) {
                                            return 'Please confirm that you want to destroy this Kubernetes cluster.';
                                        },
                                        notification: function(args) {
                                            return 'Destroyed Kubernetes cluster.';
                                        }
                                    },
                                    action: function(args) {
                                        var data = {
                                            id: args.context.kubernetesclusters[0].id,
                                            expunge: true
                                        };
                                        $.ajax({
                                            url: createURL('deleteKubernetesCluster'),
                                            data: data,
                                            dataType: "json",
                                            async: true,
                                            success: function(json) {
                                                args.response.success({
                                                    _custom: {
                                                        jobId: json.deletekubernetesclusterresponse.jobid,
                                                        getUpdatedItem: function(json) {
                                                            return { 'toRemove': true };
                                                        }
                                                    }
                                                });
                                            }
                                        });
                                    },
                                    notification: {
                                        poll: pollAsyncJobResult
                                    }
                                },
                                downloadKubernetesClusterKubeConfig: {
                                    label: 'Download Kubernetes Cluster Config',
                                    messages: {
                                        notification: function(args) {
                                            return 'Download Kubernetes Cluster Config';
                                        }
                                    },
                                    action: function(args) {
                                        var data = {
                                            id: args.context.kubernetesclusters[0].id
                                        }
                                        $.ajax({
                                            url: createURL("getKubernetesClusterConfig"),
                                            dataType: "json",
                                            data: data,
                                            async: false,
                                            success: function(json) {
                                                var jsonObj;
                                                if (json.getkubernetesclusterconfigresponse.clusterconfig != null &&
                                                    json.getkubernetesclusterconfigresponse.clusterconfig.configdata != null ) {
                                                    jsonObj = json.getkubernetesclusterconfigresponse.clusterconfig;
                                                    clusterKubeConfig = jsonObj.configdata ;
                                                }
                                            }
                                        });
                                        downloadClusterKubeConfig();
                                        args.response.success({});
                                    },
                                    notification: {
                                        poll: function(args) {
                                            args.complete();
                                        }
                                    }
                                },
                                scaleKubernetesCluster: {
                                    label: 'Scale Kubernetes Cluster',
                                    messages: {
                                        notification: function(args) {
                                            return 'Scale Kubernetes Cluster';
                                        }
                                    },
                                    createForm: {
                                        title: 'Scale Kubernetes Cluster',
                                        desc: '',
                                        preFilter: function(args) {
                                            var options = args.$form.find('.form-item[rel=serviceoffering]').find('option');
                                            $.each(options, function(optionIndex, option) {
                                                if ($(option).val() === args.context.kubernetesclusters[0].serviceofferingid) {
                                                    $(option).attr('selected','selected');
                                                }
                                            });
                                        },
                                        fields: {
                                            serviceoffering: {
                                                label: 'label.menu.service.offerings',
                                                //docID: 'helpKubernetesClusterServiceOffering',
                                                validation: {
                                                    required: true
                                                },
                                                select: function(args) {
                                                    $.ajax({
                                                        url: createURL("listServiceOfferings"),
                                                        dataType: "json",
                                                        async: true,
                                                        success: function(json) {
                                                            var offeringObjs = [];
                                                            var items = json.listserviceofferingsresponse.serviceoffering;
                                                            if (items != null) {
                                                                for (var i = 0; i < items.length; i++) {
                                                                    offeringObjs.push({
                                                                        id: items[i].id,
                                                                        description: items[i].name
                                                                    });
                                                                }
                                                            }
                                                            args.response.success({
                                                                data: offeringObjs
                                                            });
                                                        }
                                                    });
                                                }
                                            },
                                            size: {
                                                label: 'Cluster size',
                                                //docID: 'helpKubernetesClusterSize',
                                                validation: {
                                                    required: true,
                                                    number: true
                                                },
                                            }
                                        }
                                    },
                                    action: function(args) {
                                        var data = {
                                            id: args.context.kubernetesclusters[0].id,
                                            serviceofferingid: args.data.serviceoffering,
                                            size: args.data.size
                                        };
                                        $.ajax({
                                            url: createURL('scaleKubernetesCluster'),
                                            data: data,
                                            dataType: "json",
                                            success: function (json) {
                                                var jid = json.scalekubernetesclusterresponse.jobid;
                                                args.response.success({
                                                    _custom: {
                                                        jobId: jid,
                                                        getActionFilter: function() {
                                                            return cksActionfilter;
                                                        }
                                                    }
                                                });
                                            }
                                        }); //end ajax
                                    },
                                    notification: {
                                        poll: pollAsyncJobResult
                                    }
                                }
                            },
                            tabs: {
                                // Details tab
                                details: {
                                    title: 'label.details',
                                    fields: [{
                                        id: {
                                            label: 'label.id'
                                        },
                                        name: {
                                            label: 'label.name'
                                        },
                                        zonename: {
                                            label: 'label.zone.name'
                                        },
                                        masternodes : {
                                            label: 'Master nodes'
                                        },
                                        size : {
                                            label: 'Cluster Size'
                                        },
                                        cpunumber: {
                                            label: 'label.num.cpu.cores'
                                        },
                                        memory: {
                                            label: 'label.memory.mb'
                                        },
                                        state: {
                                            label: 'label.state',
                                        },
                                        serviceofferingname: {
                                            label: 'label.compute.offering'
                                        },
                                        associatednetworkname: {
                                            label: 'label.network'
                                        },
                                        keypair: {
                                            label: 'label.ssh.key.pair'
                                        },
                                        endpoint: {
                                            label: 'API endpoint',
                                            isCopyPaste: true
                                        },
                                        consoleendpoint: {
                                            label: 'Dashboard endpoint',
                                            isCopyPaste: true
                                        },
                                        username: {
                                            label: 'label.username',
                                            isCopyPaste: true
                                        },
                                        password: {
                                            label: 'label.password',
                                            isCopyPaste: true
                                        }
                                    }],

                                    dataProvider: function(args) {
                                        $.ajax({
                                            url: createURL("listKubernetesClusters&id=" + args.context.kubernetesclusters[0].id),
                                            dataType: "json",
                                            async: true,
                                            success: function(json) {
                                                var jsonObj;
                                                if (json.listkubernetesclustersresponse.kubernetescluster != null && json.listkubernetesclustersresponse.kubernetescluster.length > 0) {
                                                    jsonObj = json.listkubernetesclustersresponse.kubernetescluster[0];
                                                }
                                                args.response.success({
                                                    actionFilter: cksActionfilter,
                                                    data: jsonObj
                                                });
                                            }
                                        });
                                    }
                                },
                                console : {
                                    title: 'Access',
                                    custom : function (args) {
                                        var showDashboard = function() {
                                            var state = args.context.kubernetesclusters[0].state;

                                            if (state == "Created" || state == "Starting") { // Starting
                                                return jQuery('<br><p>').html("Kubernetes cluster setup is under progress, please check again in few minutes.");
                                            }

                                            if (state == "Stopped" || state == "Stopping") { // Starting
                                                return jQuery('<br><p>').html("Kubernetes cluster setup is under progress, please check again in few minutes.");
                                            }

                                            if (state == "Running") { // Running
                                                var data = {
                                                    id: args.context.kubernetesclusters[0].id
                                                }
                                                $.ajax({
                                                    url: createURL("getKubernetesClusterConfig"),
                                                    dataType: "json",
                                                    data: data,
                                                    async: true,
                                                    success: function(json) {
                                                        var jsonObj;
                                                        if (json.getkubernetesclusterconfigresponse.clusterconfig != null &&
                                                            json.getkubernetesclusterconfigresponse.clusterconfig.configdata != null ) {
                                                            jsonObj = json.getkubernetesclusterconfigresponse.clusterconfig;
                                                            clusterKubeConfig = jsonObj.configdata ;
                                                            args.response.success({
                                                                data: jsonObj
                                                            });
                                                        }
                                                    }
                                                });
                                                return jQuery('<br><p>').html("Access Kubernetes cluster<br>Download Config File<br><br>Use kubectl<br><code>kubectl --kubeconfig /custom/path/kube.config {COMMAND}</code><br><br>List pods<br><code>kubectl --kubeconfig /custom/path/kube.config get pods --all-namespaces</code><br>Access dashboard web UI<br>Run proxy locally<br><code>kubectl --kubeconfig /custom/path/kube.config proxy</code><br>Open URL in browser<br><code>http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/</code>");
                                                // return jQuery('<br><p>').html("Access Kubernetes cluster<br>Download Config File<br><br>How to do this<br><code>kubectl --kubeconfig /custom/path/kube.config get pods</code>");
                                            }

                                            return jQuery('<br><p>').html("Kubernetes cluster is not in a stable state, please check again in few minutes.");
                                        };
                                        return showDashboard();
                                    }
                                },
                                clusterinstances: {
                                    title: 'Instances',
                                    listView: {
                                        section: 'clusterinstances',
                                        fields: {
                                            name: {
                                                label: 'label.name',
                                                truncate: true
                                            },
                                            instancename: {
                                                label: 'label.internal.name'
                                            },
                                            displayname: {
                                                label: 'label.display.name',
                                                truncate: true
                                            },
                                            ipaddress: {
                                                label: 'label.ip.address'
                                            },
                                            zonename: {
                                                label: 'label.zone.name'
                                            },
                                            state: {
                                                label: 'label.state',
                                                indicator: {
                                                    'Running': 'on',
                                                    'Stopped': 'off',
                                                    'Destroyed': 'off',
                                                    'Error': 'off'
                                                }
                                            }
                                        },
                                        dataProvider: function(args) {
                                            var data = {};
                                            listViewDataProvider(args, data);

                                            $.ajax({
                                                url: createURL("listKubernetesClusters"),
                                                data: {"id": args.context.kubernetesclusters[0].id},
                                                success: function(json) {
                                                    var items = json.listkubernetesclustersresponse.kubernetescluster;

                                                    var vmlist = [];
                                                    $.each(items, function(idx, item) {
                                                        if ("virtualmachineids" in item) {
                                                            vmlist = vmlist.concat(item.virtualmachineids);
                                                        }
                                                    });

                                                    $.extend(data, {
                                                        ids: vmlist.join()
                                                    });

                                                    if (data.ids.length == 0) {
                                                        args.response.success({
                                                            data: []
                                                        });
                                                    } else {
                                                        $.ajax({
                                                            url: createURL('listVirtualMachines'),
                                                            data: data,
                                                            success: function(json) {
                                                                var items = json.listvirtualmachinesresponse.virtualmachine;
                                                                if (items) {
                                                                    $.each(items, function(idx, vm) {
                                                                        if (vm.nic && vm.nic.length > 0 && vm.nic[0].ipaddress) {
                                                                            items[idx].ipaddress = vm.nic[0].ipaddress;
                                                                        }
                                                                    });
                                                                }
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
                                                    }
                                                }
                                            });
                                       },
                                    }
                                },
                                firewall: {
                                    title: 'label.firewall',
                                    custom: function(args) {
                                        $.ajax({
                                            url: createURL('listNetworks'),
                                            data: {id: args.context.kubernetesclusters[0].networkid, listAll: true},
                                            async: false,
                                            dataType: "json",
                                            success: function(json) {
                                                var network = json.listnetworksresponse.network;
                                                $.extend(args.context, {"networks": [network]});
                                            }
                                        });

                                        $.ajax({
                                            url: createURL('listPublicIpAddresses'),
                                            data: {associatedNetworkId: args.context.kubernetesclusters[0].networkid, listAll: true, forvirtualnetwork: true},
                                            async: false,
                                            dataType: "json",
                                            success: function(json) {
                                                var ips = json.listpublicipaddressesresponse.publicipaddress;
                                                var fwip = ips[0];
                                                $.each(ips, function(idx, ip) {
                                                    if (ip.issourcenat || ip.isstaticnat) {
                                                        fwip = ip;
                                                        return false;
                                                    }
                                                });
                                                $.extend(args.context, {"ipAddresses": [fwip]});
                                            }
                                        });
                                        return cloudStack.sections.network.sections.ipAddresses.listView.detailView.tabs.ipRules.custom(args);
                                    },
                                },
                            }
                        }
                    }
                },
                kubernetesversions: {
                    id: 'kubernetesversions',
                    type: 'select',
                    title: "Versions",
                    listView: {
                        fields: {
                            name: {
                                label: 'label.name'
                            },
                            kubernetesversion: {
                                label: 'Kubernetes version'
                            },
                            zonename: {
                                label: 'label.zone.name'
                            },
                            isoname: {
                                label: 'ISO Name'
                            },
                            isostate: {
                                label: 'ISO State'
                            }
                        },
                        advSearchFields: {
                            name: {
                                label: 'label.name'
                            },
                            zoneid: {
                                label: 'label.zone',
                                select: function(args) {
                                    $.ajax({
                                        url: createURL('listZones'),
                                        data: {
                                            listAll: true
                                        },
                                        success: function(json) {
                                            var zones = json.listzonesresponse.zone ? json.listzonesresponse.zone : [];

                                            args.response.success({
                                                data: $.map(zones, function(zone) {
                                                    return {
                                                        id: zone.id,
                                                        description: zone.name
                                                    };
                                                })
                                            });
                                        }
                                    });
                                }
                            },
                        },
                        // List view actions
                        actions: {
                            add: {
                                label: 'Add Kubernetes version',
                                preFilter: function(args) { return isAdmin(); },
                                createForm: {
                                    title: 'Add Kubernetes version',
                                    preFilter: cloudStack.preFilter.createTemplate,
                                    fields: {
                                        name: {
                                            label: 'label.name',
                                            //docID: 'Name of the cluster',
                                            validation: {
                                                required: true
                                            }
                                        },
                                        version: {
                                            label: 'Semantic version',
                                            //docID: 'Name of the cluster',
                                            validation: {
                                                required: true
                                            }
                                        },
                                        zone: {
                                            label: 'label.zone',
                                            //docID: 'helpKubernetesClusterZone',
                                            validation: {
                                                required: true
                                            },
                                            select: function(args) {
                                                $.ajax({
                                                    url: createURL("listZones&available=true"),
                                                    dataType: "json",
                                                    async: true,
                                                    success: function(json) {
                                                        var items = [];
                                                        var zoneObjs = json.listzonesresponse.zone;
                                                        if (zoneObjs != null) {
                                                            for (var i = 0; i < zoneObjs.length; i++) {
                                                                items.push({
                                                                    id: zoneObjs[i].id,
                                                                    description: zoneObjs[i].name
                                                                });
                                                            }
                                                        }
                                                        items.sort(function(a, b) {
                                                            return a.description.localeCompare(b.description);
                                                        });
                                                        items.unshift({
                                                            id: -1,
                                                            description: "All Zones"
                                                        });
                                                        args.response.success({
                                                            data: items
                                                        });
                                                    }
                                                });
                                            }
                                        },
                                        isoid: {
                                            label: 'label.iso',
                                            //docID: 'helpKubernetesClusterZone',
                                            validation: {
                                                required: true
                                            },
                                            select: function(args) {
                                                $.ajax({
                                                    url: createURL("listIsos&ispublic=true&bootable=false"),
                                                    data: { zoneid: args.zone },
                                                    dataType: "json",
                                                    async: true,
                                                    success: function(json) {
                                                        var items = [];
                                                        var isoObjs = json.listisosresponse.iso;;
                                                        if (isoObjs != null) {
                                                            for (var i = 0; i < isoObjs.length; i++) {
                                                                items.push({
                                                                    id: isoObjs[i].id,
                                                                    description: isoObjs[i].name
                                                                });
                                                            }
                                                        }
                                                        items.sort(function(a, b) {
                                                            return a.description.localeCompare(b.description);
                                                        });
                                                        items.unshift({
                                                            id: -1,
                                                            description: "Add new ISO"
                                                        });
                                                        args.response.success({
                                                            data: items
                                                        });
                                                    }
                                                });

                                                args.$select.change(function() {
                                                    var $form = $(this).closest('form');
                                                    var currentIsoId = $(this).val();
                                                    if (currentIsoId < 0) {
                                                        $form.find('.form-item[rel=isourl]').css('display', 'inline-block');
                                                        $form.find('.form-item[rel=isochecksum]').css('display', 'inline-block');
                                                    } else {
                                                        $form.find('.form-item[rel=isourl]').hide();
                                                        $form.find('.form-item[rel=isochecksum]').hide();
                                                    }
                                                });
                                            }
                                        },
                                        isourl: {
                                            label: 'label.url',
                                            //docID: 'Name of the cluster',
                                            isHidden: true
                                        },
                                        isochecksum: {
                                            label: 'label.checksum',
                                            //docID: 'Name of the cluster',
                                            isHidden: true
                                        },
                                    }
                                },

                                action: function(args) {
                                    var data = {
                                        name: args.data.name,
                                        kubernetesversion: args.data.version,
                                    };
                                    if (args.data.zone > 0) {
                                        $.extend(data, {
                                            zoneid: args.data.zone
                                        });
                                    }
                                    if (args.data.isoid < 0) {
                                        if (args.data.isourl == null || args.data.isourl == '') {
                                            cloudStack.dialog.notice({
                                                message: 'ISO URL is required to a new ISO' //_l('')
                                            });
                                            return;
                                        }
                                        $.extend(data, {
                                            url: args.data.isourl,
                                            checksum: args.data.isochecksum
                                        });
                                    } else {
                                        $.extend(data, {
                                            isoid: args.data.isoid
                                        });
                                    }
                                    $.ajax({
                                        url: createURL('addKubernetesSupportedVersion'),
                                        data: data,
                                        success: function(json) {
                                            var jid = json.addKubernetesSupportedVersion.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid
                                                }
                                            });
                                        },
                                        error: function(XMLHttpResponse) {
                                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                            args.response.error(errorMsg);
                                        }
                                    });
                                },

                                messages: {
                                    notification: function(args) {
                                        return 'Kubernetes Supported Version Add';
                                    }
                                },
                                notification: {
                                    poll: pollAsyncJobResult
                                }
                            }
                        },
                        dataProvider: function(args) {
                            var data = {
                                    page: args.page,
                                    pagesize: pageSize
                                };
                            listViewDataProvider(args, data);
                            $.ajax({
                                url: createURL("listKubernetesSupportedVersions"),
                                data: data,
                                dataType: "json",
                                sync: true,
                                success: function(json) {
                                    var items = [];
                                    if (json.listkubernetessupportedversionsresponse.kubernetessupportedversion != null) {
                                        items = json.listkubernetessupportedversionsresponse.kubernetessupportedversion;
                                    }
                                    args.response.success({
                                        data: items
                                    });
                                }
                            });
                        },

                        detailView: {
                            name: 'Kubernetes version details',
                            isMaximized: true,
                            actions: {
                                destroy: {
                                    label: 'Delete Version',
                                    compactLabel: 'label.delete',
                                    preFilter: function(args) { return isAdmin(); },
                                    createForm: {
                                        title: 'Delete Kubernetes Version',
                                        desc: 'Delete Kubernetes Version',
                                        isWarning: true,
                                        fields: {
                                            deleteiso: {
                                                label: 'Delete ISO',
                                                isBoolean: true,
                                                isChecked: false
                                            },
                                        }
                                    },
                                    messages: {
                                        confirm: function(args) {
                                            return 'Please confirm that you want to delete this Kubernetes version.';
                                        },
                                        notification: function(args) {
                                            return 'Deleted Kubernetes version.';
                                        }
                                    },
                                    action: function(args) {
                                        var data = {
                                            id: args.context.kubernetesversions[0].id
                                        };
                                        if (args.data.deleteiso === 'on') {
                                            $.extend(data, {
                                                deleteiso: true
                                            });
                                        }
                                        $.ajax({
                                            url: createURL('deleteKubernetesSupportedVersion'),
                                            data: data,
                                            dataType: "json",
                                            async: true,
                                            success: function(json) {
                                                args.response.success({
                                                    _custom: {
                                                        jobId: json.deletekubernetessupportedversionresponse.jobid,
                                                        getUpdatedItem: function(json) {
                                                            return { 'toRemove': true };
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
                            tabs: {
                                // Details tab
                                details: {
                                    title: 'label.details',
                                    fields: [{
                                        id: {
                                            label: 'label.id'
                                        },
                                        name: {
                                            label: 'label.name'
                                        },
                                        zonename: {
                                            label: 'label.zone.name'
                                        },
                                        isoid: {
                                            label: 'ISO ID'
                                        },
                                        isoname: {
                                            label: 'ISO Name'
                                        },
                                        isostate: {
                                            label: 'ISO State'
                                        }
                                    }],

                                    dataProvider: function(args) {
                                        $.ajax({
                                            url: createURL("listKubernetesSupportedVersions&id=" + args.context.kubernetesversions[0].id),
                                            dataType: "json",
                                            async: true,
                                            success: function(json) {
                                                var jsonObj;
                                                if (json.listkubernetessupportedversionsresponse.kubernetessupportedversion != null && json.listkubernetessupportedversionsresponse.kubernetessupportedversion.length > 0) {
                                                    jsonObj = json.listkubernetessupportedversionsresponse.kubernetessupportedversion[0];
                                                }
                                                args.response.success({
                                                    data: jsonObj
                                                });
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    }
                },
            }
        });
    };

    var cksActionfilter = cloudStack.actionFilter.cksActionfilter = function(args) {
        var jsonObj = args.context.item;
        var allowedActions = [];
        if (jsonObj.state != "Destroyed" && jsonObj.state != "Destroying") {
            if (jsonObj.state == "Stopped") {
                allowedActions.push("start");
            } else {
                allowedActions.push("downloadKubernetesClusterKubeConfig");
                allowedActions.push("stop");
            }
            if (jsonObj.state == "Created" || jsonObj.state == "Running") {
                allowedActions.push("scaleKubernetesCluster");
            }
            allowedActions.push("destroy");
        }
        return allowedActions;
    }

}(cloudStack));
