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
        var filename = "kube.conf";
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
    var minCpu = 0;
    var minRamSize = 0;
    cloudStack.plugins.cks = function(plugin) {
        plugin.ui.addSection({
            id: 'cks',
            title: 'label.kubernetes.service',
            preFilter: function(args) {
                var pluginEnabled = false;
                $.ajax({
                    url: createURL('listCapabilities'),
                    async: false,
                    success: function(json) {
                        pluginEnabled = json.listcapabilitiesresponse.capability.kubernetesserviceenabled;
                    },
                    error: function(XMLHttpResponse) {
                        pluginEnabled = false;
                    }
                });
                return pluginEnabled;
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
                    title: "label.clusters",
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
                                label: 'label.add.kubernetes.cluster',
                                createForm: {
                                    title: 'label.add.kubernetes.cluster',
                                    preFilter: function(args) {
                                        args.$form.find('.form-item[rel=masternodes]').find('input[name=masternodes]').val('2');
                                        args.$form.find('.form-item[rel=size]').find('input[name=size]').val('1');
                                        var experimentalFeaturesEnabled = false;
                                        $.ajax({
                                            url: createURL('listCapabilities'),
                                            async: false,
                                            success: function(json) {
                                                experimentalFeaturesEnabled = json.listcapabilitiesresponse.capability.kubernetesclusterexperimentalfeaturesenabled;
                                            }
                                        });
                                        if (experimentalFeaturesEnabled == true) {
                                            args.$form.find('.form-item[rel=supportPrivateRegistry]').css('display', 'inline-block');
                                        }
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
                                                        args.response.success({
                                                            data: items
                                                        });
                                                    }
                                                });
                                            }
                                        },
                                        kubernetesversion: {
                                            label: 'label.kubernetes.version',
                                            dependsOn: ['zone'],
                                            //docID: 'helpKubernetesClusterZone',
                                            validation: {
                                                required: true
                                            },
                                            select: function(args) {
                                                var versionObjs;
                                                var filterData = { zoneid: args.zone };
                                                $.ajax({
                                                    url: createURL("listKubernetesSupportedVersions"),
                                                    data: filterData,
                                                    dataType: "json",
                                                    async: true,
                                                    success: function(json) {
                                                        var items = [];
                                                        versionObjs = json.listkubernetessupportedversionsresponse.kubernetessupportedversion;
                                                        if (versionObjs != null) {
                                                            for (var i = 0; i < versionObjs.length; i++) {
                                                                if (versionObjs[i].state == 'Enabled' && versionObjs[i].isostate == 'Ready') {
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

                                                args.$select.change(function() {
                                                    var $form = $(this).closest("form");
                                                    $form.find('.form-item[rel=multimaster]').find('input[name=multimaster]').prop('checked', false);
                                                    $form.find('.form-item[rel=multimaster]').hide();
                                                    $form.find('.form-item[rel=masternodes]').hide();
                                                    var currentVersionId = $(this).val();
                                                    if (currentVersionId != null  && versionObjs != null) {
                                                        for (var i = 0; i < versionObjs.length; i++) {
                                                            if (currentVersionId == versionObjs[i].id) {
                                                                if (versionObjs[i].supportsha === true) {
                                                                    $form.find('.form-item[rel=multimaster]').css('display', 'inline-block');
                                                                }
                                                                minCpu = 0;
                                                                if (versionObjs[i].mincpunumber != null && versionObjs[i].mincpunumber != undefined) {
                                                                    minCpu = versionObjs[i].mincpunumber;
                                                                }
                                                                minRamSize = 0;
                                                                if (versionObjs[i].minmemory != null && versionObjs[i].minmemory != undefined) {
                                                                    minRamSize = versionObjs[i].minmemory;
                                                                }
                                                                break;
                                                            }
                                                        }
                                                    }
                                                });
                                            }
                                        },
                                        serviceoffering: {
                                            label: 'label.menu.service.offerings',
                                            dependsOn: ['kubernetesversion'],
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
                                                                if (items[i].iscustomized == false &&
                                                                    items[i].cpunumber >= minCpu && items[i].memory >= minRamSize) {
                                                                    offeringObjs.push({
                                                                        id: items[i].id,
                                                                        description: items[i].name
                                                                    });
                                                                }
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
                                            label: 'label.node.root.disk.size.gb',
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
                                            label: "label.ha.enabled",
                                            dependsOn: 'kubernetesversion',
                                            isBoolean: true,
                                            isChecked: false,
                                        },
                                        masternodes: {
                                            label: 'label.master.nodes',
                                            //docID: 'helpKubernetesClusterSize',
                                            validation: {
                                                required: true,
                                                multiplecountnumber: true
                                            },
                                            dependsOn: "multimaster",
                                            isHidden: true,
                                        },
                                        externalloadbalanceripaddress: {
                                            label: 'label.external.loadbalancer.ip.address',
                                            validation: {
                                                ipv4AndIpv6AddressValidator: true
                                            },
                                            dependsOn: "multimaster",
                                            isHidden: true,
                                        },
                                        size: {
                                            label: 'label.cluster.size.worker.nodes',
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
                                            label: 'label.private.registry',
                                            isBoolean: true,
                                            isChecked: false,
                                            isHidden: true
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
                                        if (args.data.externalloadbalanceripaddress != null && args.data.externalloadbalanceripaddress != "") {
                                            $.extend(data, {
                                                externalloadbalanceripaddress: args.data.externalloadbalanceripaddress
                                            });
                                        }
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
                            name: 'label.kubernetes.cluster.details',
                            isMaximized: true,
                            actions: {
                                start: {
                                    label: 'label.start.kuberentes.cluster',
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
                                            return 'message.confirm.start.kubernetes.cluster';
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
                                    label: 'label.stop.kuberentes.cluster',
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
                                            return 'message.confirm.stop.kubernetes.cluster';
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
                                    label: 'label.destroy.kubernetes.cluster',
                                    compactLabel: 'label.destroy',
                                    createForm: {
                                        title: 'label.destroy.kubernetes.cluster',
                                        desc: 'label.destroy.kubernetes.cluster',
                                        isWarning: true,
                                        fields: {
                                        }
                                    },
                                    messages: {
                                        confirm: function(args) {
                                            return 'message.confirm.destroy.kubernetes.cluster';
                                        },
                                        notification: function(args) {
                                            return 'Destroyed Kubernetes cluster.';
                                        }
                                    },
                                    action: function(args) {
                                        var data = {
                                            id: args.context.kubernetesclusters[0].id
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
                                    label: 'label.download.kubernetes.cluster.config',
                                    messages: {
                                        notification: function(args) {
                                            return 'label.download.kubernetes.cluster.config';
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
                                                    clusterKubeConfig = jsonObj.configdata;
                                                    downloadClusterKubeConfig();
                                                    args.response.success({});
                                                } else {
                                                    args.response.error("Unable to retrieve Kubernetes cluster config");
                                                }
                                            },
                                            error: function(XMLHttpResponse) {
                                                var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                args.response.error(errorMsg);
                                            }
                                        });
                                    },
                                    notification: {
                                        poll: function(args) {
                                            args.complete();
                                        }
                                    }
                                },
                                scaleKubernetesCluster: {
                                    label: 'label.scale.kubernetes.cluster',
                                    messages: {
                                        notification: function(args) {
                                            return 'label.scale.kubernetes.cluster';
                                        }
                                    },
                                    createForm: {
                                        title: 'label.scale.kubernetes.cluster',
                                        desc: '',
                                        preFilter: function(args) {
                                            var options = args.$form.find('.form-item[rel=serviceoffering]').find('option');
                                            $.each(options, function(optionIndex, option) {
                                                if ($(option).val() === args.context.kubernetesclusters[0].serviceofferingid) {
                                                    $(option).attr('selected','selected');
                                                }
                                            });
                                            args.$form.find('.form-item[rel=size]').find('input[name=size]').val(args.context.kubernetesclusters[0].size);
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
                                                        url: createURL("listKubernetesSupportedVersions"),
                                                        data: {id: args.context.kubernetesclusters[0].kubernetesversionid},
                                                        dataType: "json",
                                                        async: false,
                                                        success: function(json) {
                                                            var versionObjs = json.listkubernetessupportedversionsresponse.kubernetessupportedversion;
                                                            if (versionObjs != null && versionObjs.length > 0) {
                                                                minCpu = 0;
                                                                if (versionObjs[0].mincpunumber != null && versionObjs[0].mincpunumber != undefined) {
                                                                    minCpu = versionObjs[0].mincpunumber;
                                                                }
                                                                minRamSize = 0;
                                                                if (versionObjs[0].minmemory != null && versionObjs[0].minmemory != undefined) {
                                                                    minRamSize = versionObjs[0].minmemory;
                                                                }
                                                            }
                                                        }
                                                    });
                                                    $.ajax({
                                                        url: createURL("listServiceOfferings"),
                                                        dataType: "json",
                                                        async: true,
                                                        success: function(json) {
                                                            var offeringObjs = [];
                                                            var items = json.listserviceofferingsresponse.serviceoffering;
                                                            if (items != null) {
                                                                for (var i = 0; i < items.length; i++) {
                                                                    if (items[i].iscustomized == false &&
                                                                        items[i].cpunumber >= minCpu && items[i].memory >= minRamSize) {
                                                                        offeringObjs.push({
                                                                            id: items[i].id,
                                                                            description: items[i].name
                                                                        });
                                                                    }
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
                                                label: 'label.cluster.size',
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
                                },
                                upgradeKubernetesCluster: {
                                    label: 'label.upgrade.kubernetes.cluster',
                                    messages: {
                                        notification: function(args) {
                                            return 'label.upgrade.kubernetes.cluster';
                                        }
                                    },
                                    createForm: {
                                        title: 'label.upgrade.kubernetes.cluster',
                                        desc: '',
                                        preFilter: function(args) {},
                                        fields: {
                                            kubernetesversion: {
                                                label: 'label.kubernetes.version',
                                                //docID: 'helpKubernetesClusterZone',
                                                validation: {
                                                    required: true
                                                },
                                                select: function(args) {
                                                    var filterData = { minimumkubernetesversionid: args.context.kubernetesclusters[0].kubernetesversionid };
                                                    $.ajax({
                                                        url: createURL("listKubernetesSupportedVersions"),
                                                        data: filterData,
                                                        dataType: "json",
                                                        async: true,
                                                        success: function(json) {
                                                            var items = [];
                                                            var versionObjs = json.listkubernetessupportedversionsresponse.kubernetessupportedversion;
                                                            if (versionObjs != null) {
                                                                var clusterVersion = null;
                                                                for (var j = 0; j < versionObjs.length; j++) {
                                                                    if (versionObjs[j].id == args.context.kubernetesclusters[0].kubernetesversionid) {
                                                                        clusterVersion = versionObjs[j];
                                                                        break;
                                                                    }
                                                                }
                                                                for (var i = 0; i < versionObjs.length; i++) {
                                                                    if (versionObjs[i].id != args.context.kubernetesclusters[0].kubernetesversionid &&
                                                                        (clusterVersion == null || (clusterVersion != null && versionObjs[i].semanticversion != clusterVersion.semanticversion)) &&
                                                                        versionObjs[i].state == 'Enabled' && versionObjs[i].isostate == 'Ready') {
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
                                        }
                                    },
                                    action: function(args) {
                                        var data = {
                                            id: args.context.kubernetesclusters[0].id,
                                            kubernetesversionid: args.data.kubernetesversion
                                        };
                                        $.ajax({
                                            url: createURL('upgradeKubernetesCluster'),
                                            data: data,
                                            dataType: "json",
                                            success: function (json) {
                                                var jid = json.upgradekubernetesclusterresponse.jobid;
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
                                },
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
                                        kubernetesversionname: {
                                            label: 'label.kubernetes.version'
                                        },
                                        masternodes : {
                                            label: 'label.master.nodes'
                                        },
                                        size : {
                                            label: 'label.cluster.size'
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
                                clusteraccess: {
                                    title: 'label.access',
                                    custom : function (args) {
                                        var showAccess = function() {
                                            var state = args.context.kubernetesclusters[0].state;
                                            if (state == "Created") { // Created
                                                return jQuery('<br><p>').html("Kubernetes cluster setup is under progress, please check again in few minutes.");
                                            } else if (state == "Error") { // Error
                                                return jQuery('<br><p>').html("Kubernetes cluster is in error state, it cannot be accessed.");
                                            } else if (state == "Destroying") { // Destroying
                                                return jQuery('<br><p>').html("Kubernetes cluster is in destroying state, it cannot be accessed.");
                                            } else if (state == "Destroyed") { // Destroyed
                                                return jQuery('<br><p>').html("Kubernetes cluster is already destroyed, it cannot be accessed.");
                                            }
                                            var data = {
                                                id: args.context.kubernetesclusters[0].kubernetesversionid
                                            }
                                            var version = '';
                                            $.ajax({
                                                url: createURL("listKubernetesSupportedVersions"),
                                                dataType: "json",
                                                data: data,
                                                async: false,
                                                success: function(json) {
                                                    var jsonObj;
                                                    if (json.listkubernetessupportedversionsresponse.kubernetessupportedversion != null) {
                                                        version = json.listkubernetessupportedversionsresponse.kubernetessupportedversion[0].semanticversion;
                                                    }
                                                }
                                            });
                                            return jQuery('<br><p>').html("Access Kubernetes cluster<br><br>Download cluster's kubeconfig file using action from Details tab.<br>Download kubectl tool for cluster's Kubernetes version from,<br>Linux: <a href='https://storage.googleapis.com/kubernetes-release/release/v" + version + "/bin/linux/amd64/kubectl'>https://storage.googleapis.com/kubernetes-release/release/v" + version + "/bin/linux/amd64/kubectl</a><br>MacOS: <a href='https://storage.googleapis.com/kubernetes-release/release/v" + version + "/bin/darwin/amd64/kubectl'>https://storage.googleapis.com/kubernetes-release/release/v" + version + "/bin/darwin/amd64/kubectl</a><br>Windows: <a href='https://storage.googleapis.com/kubernetes-release/release/v" + version + "/bin/windows/amd64/kubectl.exe'>https://storage.googleapis.com/kubernetes-release/release/v" + version + "/bin/windows/amd64/kubectl.exe</a><br><br>Using kubectl and kubeconfig file to access cluster<br><code>kubectl --kubeconfig /custom/path/kube.conf {COMMAND}</code><br><br>List pods<br><code>kubectl --kubeconfig /custom/path/kube.conf get pods --all-namespaces</code><br>List nodes<br><code>kubectl --kubeconfig /custom/path/kube.conf get nodes --all-namespaces</code><br>List services<br><code>kubectl --kubeconfig /custom/path/kube.conf get services --all-namespaces</code><br><br>Access dashboard web UI<br>Run proxy locally<br><code>kubectl --kubeconfig /custom/path/kube.conf proxy</code><br>Open URL in browser<br><code><a href='http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/'>http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/</a></code><br><br>Token for dashboard login can be retrieved using following command<br><code>kubectl --kubeconfig /custom/path/kube.conf describe secret $(kubectl --kubeconfig /custom/path/kube.conf get secrets -n kubernetes-dashboard | grep kubernetes-dashboard-token | awk '{print $1}') -n kubernetes-dashboard</code><br><br>More about accessing dashboard UI, https://kubernetes.io/docs/tasks/access-application-cluster/web-ui-dashboard/#accessing-the-dashboard-ui");
                                        };
                                        return showAccess();
                                    }
                                },
                                clusterinstances: {
                                    title: 'label.instances',
                                    listView: {
                                        section: 'clusterinstances',
                                        preFilter: function(args) {
                                            var hiddenFields = [];
                                            if (!isAdmin()) {
                                                hiddenFields.push('instancename');
                                            }
                                            return hiddenFields;
                                        },
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

                                                    if (items && items.length > 0 && items[0].projectid != null &&
                                                        items[0].projectid != undefined && items[0].projectid.length > 0) {
                                                        $.extend(data, {
                                                            projectid: items[0].projectid
                                                        });
                                                    }

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
                                        var data = {
                                            id: args.context.kubernetesclusters[0].networkid,
                                            listAll: true
                                        }
                                        if (args.context.kubernetesclusters[0].projectid != null &&
                                            args.context.kubernetesclusters[0].projectid != undefined &&
                                            args.context.kubernetesclusters[0].projectid.length > 0) {
                                            $.extend(data, {
                                                projectid: args.context.kubernetesclusters[0].projectid
                                            });
                                            $.extend(args.context, {"projectid": args.context.kubernetesclusters[0].projectid});
                                        }
                                        $.ajax({
                                            url: createURL('listNetworks'),
                                            data: data,
                                            async: false,
                                            dataType: "json",
                                            success: function(json) {
                                                var network = json.listnetworksresponse.network;
                                                $.extend(args.context, {"networks": [network]});
                                            }
                                        });
                                        data = {
                                            associatedNetworkId: args.context.kubernetesclusters[0].networkid,
                                            listAll: true,
                                            forvirtualnetwork: true
                                        }
                                        if (args.context.kubernetesclusters[0].projectid != null &&
                                            args.context.kubernetesclusters[0].projectid != undefined &&
                                            args.context.kubernetesclusters[0].projectid.length > 0) {
                                            $.extend(data, {
                                                projectid: args.context.kubernetesclusters[0].projectid
                                            });
                                        }
                                        $.ajax({
                                            url: createURL('listPublicIpAddresses'),
                                            data: data,
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
                    title: "label.versions",
                    listView: {
                        fields: {
                            name: {
                                label: 'label.name'
                            },
                            semanticversion: {
                                label: 'label.kubernetes.version'
                            },
                            zonename: {
                                label: 'label.zone.name'
                            },
                            isoname: {
                                label: 'label.iso.name'
                            },
                            isostate: {
                                label: 'label.iso.state'
                            },
                            mincpunumber: {
                                label: 'label.min.cpu.cores'
                            },
                            minmemory: {
                                label: 'label.memory.minimum.mb'
                            },
                            state: {
                                label: 'label.state',
                                indicator: {
                                    'Enabled': 'on',
                                    'Disabled': 'off'
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
                                label: 'label.add.kubernetes.version',
                                preFilter: function(args) { return isAdmin(); },
                                createForm: {
                                    title: 'label.add.kubernetes.version',
                                    preFilter: cloudStack.preFilter.createTemplate,
                                    fields: {
                                        version: {
                                            label: 'label.semantic.version',
                                            //docID: 'Name of the cluster',
                                            validation: {
                                                required: true
                                            }
                                        },
                                        name: {
                                            label: 'label.name',
                                            //docID: 'Name of the cluster',
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
                                                            description: 'label.all.zones'
                                                        });
                                                        args.response.success({
                                                            data: items
                                                        });
                                                    }
                                                });
                                            }
                                        },
                                        isourl: {
                                            label: 'label.url',
                                            //docID: 'Name of the cluster',
                                            validation: {
                                                required: true
                                            }
                                        },
                                        isochecksum: {
                                            label: 'label.checksum',
                                            //docID: 'Name of the cluster',
                                        },
                                        mincpunumber: {
                                            label: 'label.min.cpu.cores',
                                            validation: {
                                                required: true,
                                                number: true
                                            },
                                        },
                                        minmemory: {
                                            label: 'label.memory.minimum.mb',
                                            validation: {
                                                required: true,
                                                number: true
                                            }
                                        }
                                    }
                                },

                                action: function(args) {
                                    var data = {
                                        name: args.data.name,
                                        semanticversion: args.data.version,
                                        url: args.data.isourl,
                                        checksum: args.data.isochecksum
                                    };
                                    if (args.data.zone != null && args.data.zone != -1) {
                                        $.extend(data, {
                                            zoneid: args.data.zone
                                        });
                                    }
                                    if (args.data.mincpunumber != null && args.data.mincpunumber != "" && args.data.mincpunumber > 0) {
                                        $.extend(data, {
                                            mincpunumber: args.data.mincpunumber
                                        });
                                    }
                                    if (args.data.minmemory != null && args.data.minmemory != "" && args.data.minmemory > 0) {
                                        $.extend(data, {
                                            minmemory: args.data.minmemory
                                        });
                                    }
                                    $.ajax({
                                        url: createURL('addKubernetesSupportedVersion'),
                                        data: data,
                                        success: function(json) {
                                            var version = json.addkubernetessupportedversionresponse.kubernetessupportedversion;
                                            args.response.success({
                                                data: version
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
                            name: 'label.kubernetes.version.details',
                            isMaximized: true,
                            actions: {
                                update: {
                                    label: 'label.edit',
                                    messages: {
                                        notification: function(args) {
                                            return 'label.update.kubernetes.version';
                                        }
                                    },
                                    createForm: {
                                        title: 'label.update.kubernetes.version',
                                        desc: '',
                                        preFilter: function(args) {
                                            var formVersion = args.context.kubernetesversions[0];
                                            $.ajax({
                                                url: createURL('listKubernetesSupportedVersions'),
                                                data: {
                                                    id: args.context.kubernetesversions[0].id
                                                },
                                                dataType: "json",
                                                async: false,
                                                success: function (json) {
                                                    if (json.listkubernetessupportedversionsresponse.kubernetessupportedversion != null &&
                                                        json.listkubernetessupportedversionsresponse.kubernetessupportedversion.length > 0) {
                                                        formVersion = json.listkubernetessupportedversionsresponse.kubernetessupportedversion[0];
                                                    }
                                                }
                                            });
                                            if (formVersion.state != null) {
                                                var options = args.$form.find('.form-item[rel=state]').find('option');
                                                $.each(options, function(optionIndex, option) {
                                                    if ($(option).val() === formVersion.state) {
                                                        $(option).attr('selected','selected');
                                                    }
                                                });
                                            }
                                        },
                                        fields: {
                                            state: {
                                                label: 'label.state',
                                                //docID: 'helpKubernetesClusterZone',
                                                validation: {
                                                    required: true
                                                },
                                                select: function(args) {
                                                    var items = [];
                                                    items.push({
                                                        id: 'Enabled',
                                                        description: 'state.Enabled'
                                                    }, {
                                                        id: 'Disabled',
                                                        description: 'state.Disabled'
                                                    });
                                                    args.response.success({
                                                        data: items
                                                    });
                                                }
                                            },
                                        }
                                    },
                                    action: function(args) {
                                        var data = {
                                            id: args.context.kubernetesversions[0].id,
                                            state: args.data.state
                                        };
                                        $.ajax({
                                            url: createURL('updateKubernetesSupportedVersion'),
                                            data: data,
                                            dataType: "json",
                                            success: function (json) {
                                                var jsonObj;
                                                if (json.updatekubernetessupportedversionresponse.kubernetessupportedversion != null) {
                                                    jsonObj = json.updatekubernetessupportedversionresponse.kubernetessupportedversion;
                                                }
                                                args.response.success({
                                                    data: jsonObj
                                                });
                                            },
                                            error: function(XMLHttpResponse) {
                                                var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                args.response.error(errorMsg);
                                            }
                                        }); //end ajax
                                    }
                                },
                                destroy: {
                                    label: 'label.delete.kubernetes.version',
                                    compactLabel: 'label.delete',
                                    preFilter: function(args) { return isAdmin(); },
                                    createForm: {
                                        title: 'label.delete.kubernetes.version',
                                        desc: 'label.delete.kubernetes.version',
                                        isWarning: true,
                                        fields: {}
                                    },
                                    messages: {
                                        confirm: function(args) {
                                            return 'message.confirm.delete.kubernetes.version';
                                        },
                                        notification: function(args) {
                                            return 'Deleted Kubernetes version.';
                                        }
                                    },
                                    action: function(args) {
                                        var data = {
                                            id: args.context.kubernetesversions[0].id
                                        };
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
                                            label: 'label.iso.id'
                                        },
                                        isoname: {
                                            label: 'label.iso.name'
                                        },
                                        isostate: {
                                            label: 'label.iso.name'
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
                allowedActions.push("upgradeKubernetesCluster");
            }
            allowedActions.push("destroy");
        }
        return allowedActions;
    }

}(cloudStack));
