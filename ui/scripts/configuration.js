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

(function(cloudStack, $) {

    var requiredNetworkOfferingExists = false;
    var networkServiceObjs = [],
        serviceCheckboxNames = [];
    var serviceFields = [];

    cloudStack.sections.configuration = {
        title: 'label.menu.service.offerings',
        id: 'configuration',
        sectionSelect: {
            preFilter: function(args) {
               if(isAdmin())
                   return ["serviceOfferings", "systemServiceOfferings", "diskOfferings", "networkOfferings"];
               else if(isDomainAdmin())
                   return ["serviceOfferings", "diskOfferings"];
               else
                   return null;
            },
            label: 'label.select.offering'
        },
        sections: {
            serviceOfferings: {
                type: 'select',
                title: 'label.compute.offerings',
                listView: {
                    id: 'serviceOfferings',
                    label: 'label.menu.service.offerings',
                    fields: {
                        name: {
                            label: 'label.name',
                            editable: true
                        },
                        displaytext: {
                            label: 'label.description'
                        }
                    },

                    reorder: cloudStack.api.actions.sort('updateServiceOffering', 'serviceOfferings'),

                    actions: {
                        add: {
                            label: 'label.add.compute.offering',

                            messages: {
                                confirm: function(args) {
                                    return 'message.add.service.offering';
                                },
                                notification: function(args) {
                                    return 'label.add.compute.offering';
                                }
                            },

                            createForm: {
                                title: 'label.add.compute.offering',
                                preFilter: function(args) {
                                    if (isAdmin()) {
                                    } else {
                                        args.$form.find('.form-item[rel=isPublic]').hide();
                                        args.$form.find('.form-item[rel=domainId]').css('display', 'inline-block'); //shown
                                        args.$form.find('.form-item[rel=deploymentPlanner]').hide();
                                        args.$form.find('.form-item[rel=plannerMode]').hide();
                                        args.$form.find('.form-item[rel=storageTags]').hide();
                                        args.$form.find('.form-item[rel=hostTags]').hide();
                                    }
                                },
                                fields: {
                                    name: {
                                        label: 'label.name',
                                        docID: 'helpComputeOfferingName',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    description: {
                                        label: 'label.description',
                                        docID: 'helpComputeOfferingDescription',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    storageType: {
                                        label: 'label.storage.type',
                                        docID: 'helpComputeOfferingStorageType',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: 'shared',
                                                description: 'shared'
                                            });
                                            items.push({
                                                id: 'local',
                                                description: 'local'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    provisioningType: {
                                        label: 'label.disk.provisioningtype',
                                        docID: 'helpComputeOfferingProvisioningType',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: 'thin',
                                                description: 'thin'
                                            });
                                            items.push({
                                                id: 'sparse',
                                                description: 'sparse'
                                            });
                                            items.push({
                                                id: 'fat',
                                                description: 'fat'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    isCustomized: {
                                        label: 'label.custom',
                                        isBoolean: true,
                                        isReverse: true,
                                        isChecked: false
                                    },
                                    cpuNumber: {
                                        label: 'label.num.cpu.cores',
                                        dependsOn: 'isCustomized',
                                        docID: 'helpComputeOfferingCPUCores',
                                        validation: {
                                            required: true,
                                            number: true
                                        }
                                    },
                                    cpuSpeed: {
                                        label: 'label.cpu.mhz',
                                        dependsOn: 'isCustomized',
                                        docID: 'helpComputeOfferingCPUMHz',
                                        validation: {
                                            required: true,
                                            number: true
                                        }
                                    },
                                    memory: {
                                        label: 'label.memory.mb',
                                        dependsOn: 'isCustomized',
                                        docID: 'helpComputeOfferingMemory',
                                        validation: {
                                            required: true,
                                            number: true
                                        }
                                    },
                                    networkRate: {
                                        label: 'label.network.rate',
                                        docID: 'helpComputeOfferingNetworkRate',
                                        validation: {
                                            required: false, //optional
                                            number: true
                                        }
                                    },
                                    qosType: {
                                        label: 'label.qos.type',
                                        docID: 'helpDiskOfferingQoSType',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: '',
                                                description: ''
                                            });
                                            items.push({
                                                id: 'hypervisor',
                                                description: 'hypervisor'
                                            });
                                            items.push({
                                                id: 'storage',
                                                description: 'storage'
                                            });
                                            args.response.success({
                                                data: items
                                            });

                                            args.$select.change(function() {
                                                var $form = $(this).closest('form');
                                                var $isCustomizedIops = $form.find('.form-item[rel=isCustomizedIops]');
                                                var $minIops = $form.find('.form-item[rel=minIops]');
                                                var $maxIops = $form.find('.form-item[rel=maxIops]');
                                                var $hypervisorSnapshotReserve = $form.find('.form-item[rel=hypervisorSnapshotReserve]');
                                                var $diskBytesReadRate = $form.find('.form-item[rel=diskBytesReadRate]');
                                                var $diskBytesWriteRate = $form.find('.form-item[rel=diskBytesWriteRate]');
                                                var $diskIopsReadRate = $form.find('.form-item[rel=diskIopsReadRate]');
                                                var $diskIopsWriteRate = $form.find('.form-item[rel=diskIopsWriteRate]');

                                                var qosId = $(this).val();

                                                if (qosId == 'storage') { // Storage QoS
                                                    $diskBytesReadRate.hide();
                                                    $diskBytesWriteRate.hide();
                                                    $diskIopsReadRate.hide();
                                                    $diskIopsWriteRate.hide();

                                                    $isCustomizedIops.css('display', 'inline-block');

                                                    if ($isCustomizedIops.find('input[type=checkbox]').is(':checked')) {
                                                        $minIops.hide();
                                                        $maxIops.hide();
                                                    } else {
                                                        $minIops.css('display', 'inline-block');
                                                        $maxIops.css('display', 'inline-block');
                                                    }

                                                    $hypervisorSnapshotReserve.css('display', 'inline-block');
                                                } else if (qosId == 'hypervisor') { // Hypervisor Qos
                                                    $isCustomizedIops.hide();
                                                    $minIops.hide();
                                                    $maxIops.hide();
                                                    $hypervisorSnapshotReserve.hide();

                                                    $diskBytesReadRate.css('display', 'inline-block');
                                                    $diskBytesWriteRate.css('display', 'inline-block');
                                                    $diskIopsReadRate.css('display', 'inline-block');
                                                    $diskIopsWriteRate.css('display', 'inline-block');
                                                } else { // No Qos
                                                    $diskBytesReadRate.hide();
                                                    $diskBytesWriteRate.hide();
                                                    $diskIopsReadRate.hide();
                                                    $diskIopsWriteRate.hide();
                                                    $isCustomizedIops.hide();
                                                    $minIops.hide();
                                                    $maxIops.hide();
                                                    $hypervisorSnapshotReserve.hide();
                                                }
                                            });
                                        }
                                    },
                                    isCustomizedIops: {
                                        label: 'label.custom.disk.iops',
                                        docID: 'helpDiskOfferingCustomDiskIops',
                                        isBoolean: true,
                                        isReverse: true,
                                        isChecked: false
                                    },
                                    minIops: {
                                        label: 'label.disk.iops.min',
                                        docID: 'helpDiskOfferingDiskIopsMin',
                                        dependsOn: 'isCustomizedIops',
                                        validation: {
                                            required: false,
                                            number: true
                                        }
                                    },
                                    maxIops: {
                                        label: 'label.disk.iops.max',
                                        docID: 'helpDiskOfferingDiskIopsMax',
                                        dependsOn: 'isCustomizedIops',
                                        validation: {
                                            required: false,
                                            number: true
                                        }
                                    },
                                    hypervisorSnapshotReserve: {
                                        label: 'label.hypervisor.snapshot.reserve',
                                        docID: 'helpDiskOfferingHypervisorSnapshotReserve',
                                        validation: {
                                            required: false,
                                            number: true
                                        }
                                    },
                                    diskBytesReadRate: {
                                        label: 'label.disk.bytes.read.rate',
                                        docID: 'helpComputeOfferingDiskBytesReadRate',
                                        validation: {
                                            required: false, //optional
                                            number: true
                                        }
                                    },
                                    diskBytesWriteRate: {
                                        label: 'label.disk.bytes.write.rate',
                                        docID: 'helpComputeOfferingDiskBytesWriteRate',
                                        validation: {
                                            required: false, //optional
                                            number: true
                                        }
                                    },
                                    diskIopsReadRate: {
                                        label: 'label.disk.iops.read.rate',
                                        docID: 'helpComputeOfferingDiskIopsReadRate',
                                        validation: {
                                            required: false, //optional
                                            number: true
                                        }
                                    },
                                    diskIopsWriteRate: {
                                        label: 'label.disk.iops.write.rate',
                                        docID: 'helpComputeOfferingDiskIopsWriteRate',
                                        validation: {
                                            required: false, //optional
                                            number: true
                                        }
                                    },
                                    offerHA: {
                                        label: 'label.offer.ha',
                                        docID: 'helpComputeOfferingHA',
                                        isBoolean: true,
                                        isChecked: false
                                    },
                                    storageTags: {
                                        label: 'label.storage.tags',
                                        docID: 'helpComputeOfferingStorageType',
                                        isTokenInput: true,
                                        dataProvider: function(args) {
                                            $.ajax({
                                                url: createURL("listStorageTags"),
                                                dataType: "json",
                                                success: function(json) {
                                                    var item = json.liststoragetagsresponse.storagetag;
                                                    var tags = [];

                                                    if (item != null)
                                                    {
                                                        tags = $.map(item, function(tag) {
                                                            return {
                                                                       id: tag.name,
                                                                       name: tag.name
                                                                   };
                                                        });
                                                    }

                                                    args.response.success({
                                                        data: tags,
                                                        hintText: _l('hint.type.part.storage.tag'),
                                                        noResultsText: _l('hint.no.storage.tags')
                                                    });
                                                },
                                                error: function(XMLHttpResponse) {
                                                    var errorMsg = parseXMLHttpResponse(XMLHttpResponse);

                                                    args.response.error(errorMsg);
                                                }
                                            });
                                        }
                                    },
                                    hostTags: { //Only one single host tag is supported at server-side. Multiple host tags are NOT supported at server-side.
                                        label: 'label.host.tag',
                                        docID: 'helpComputeOfferingHostTags'
                                    },
                                    cpuCap: {
                                        label: 'label.CPU.cap',
                                        isBoolean: true,
                                        isChecked: false,
                                        docID: 'helpComputeOfferingCPUCap'
                                    },
                                    isPublic: {
                                        label: 'label.public',
                                        isBoolean: true,
                                        isReverse: true,
                                        isChecked: false,
                                        docID: 'helpComputeOfferingPublic'
                                    },

                                    isVolatile: {
                                        label: 'label.volatile',
                                        isBoolean: true,
                                        isChecked: false

                                    },

                                    deploymentPlanner: {
                                        label: 'label.deployment.planner',
                                        select: function(args) {
                                          if (isAdmin()) {
                                            $.ajax({
                                                url: createURL('listDeploymentPlanners'),
                                                dataType: 'json',
                                                success: function(json) {
                                                    var items = [{
                                                        id: '',
                                                        description: ''
                                                    }];
                                                    var plannerObjs = json.listdeploymentplannersresponse.deploymentPlanner;
                                                    $(plannerObjs).each(function() {
                                                        items.push({
                                                            id: this.name,
                                                            description: this.name
                                                        });
                                                    });
                                                    args.response.success({
                                                        data: items
                                                    });
                                                    args.$select.change(function() {
                                                        var $form = $(this).closest('form');
                                                        var $fields = $form.find('.field');
                                                        if ($(this).val() == "ImplicitDedicationPlanner") {
                                                            $form.find('[rel=plannerMode]').css('display', 'block');
                                                        } else {
                                                            $form.find('[rel=plannerMode]').hide();
                                                        }
                                                    });
                                                }
                                            });
                                          }
                                        }
                                    },

                                    // plannerKey:{label:'Planner Key' , docID:'helpImplicitPlannerKey'},
                                    plannerMode: {
                                        label: 'label.planner.mode',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: '',
                                                description: ''
                                            });
                                            items.push({
                                                id: 'Strict',
                                                description: 'Strict'
                                            });
                                            items.push({
                                                id: 'Preferred',
                                                description: 'Preferred'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },

                                    pciDevice: {
                                        label: 'label.gpu',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: '',
                                                description: ''
                                            });
                                            items.push({
                                                id: 'Group of NVIDIA Corporation GK107GL [GRID K1] GPUs',
                                                description: 'NVIDIA GRID K1'
                                            });
                                            items.push({
                                                id: 'Group of NVIDIA Corporation GK104GL [GRID K2] GPUs',
                                                description: 'NVIDIA GRID K2'
                                            });
                                            args.response.success({
                                                data: items
                                            });

                                            var vGpuMap = {};
                                            vGpuMap['Group of NVIDIA Corporation GK107GL [GRID K1] GPUs'] = ['passthrough', 'GRID K100', 'GRID K120Q', 'GRID K140Q', 'GRID K160Q', 'GRID K180Q'];
                                            vGpuMap['Group of NVIDIA Corporation GK104GL [GRID K2] GPUs'] = ['passthrough', 'GRID K200', 'GRID K220Q', 'GRID K240Q', 'GRID K260Q', 'GRID K280Q'];

                                            args.$select.change(function() {
                                                var gpu = $(this).val();

                                                if (gpu == '') {
                                                    $(this).closest('form').find('.form-item[rel=\"vgpuType\"]').hide();
                                                }
                                                else {
                                                    $(this).closest('form').find('.form-item[rel=\"vgpuType\"]').css('display', 'inline-block');

                                                    // enable/disable vGPU type options, depending on selected GPU
                                                    var $vGpuTypeSelect = $(this).closest('form').find('select[name=vgpuType]');
                                                    var $vGpuTypeOptions = $vGpuTypeSelect.find('option');
                                                    $vGpuTypeOptions.each(function(index) {
                                                        var vGpuTypeOption = $(this).val();
                                                        if (vGpuTypeOption == '' || (gpu in vGpuMap && $.inArray(vGpuTypeOption, vGpuMap[gpu]) > -1))
                                                            $(this).attr('disabled', false);
                                                        else
                                                            $(this).attr('disabled', true);
                                                    });

                                                    //if selected option is disabled, select the first enabled option instead
                                                    if ($vGpuTypeSelect.find('option:selected:disabled').length > 0) {
                                                        $vGpuTypeSelect.val($vGpuTypeSelect.find('option:enabled:first').val());
                                                    }
                                                }
                                            });
                                        }
                                    },

                                    vgpuType: {
                                        label: 'label.vgpu.type',
                                        isHidden: true,
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: '',
                                                description: ''
                                            });
                                            items.push({
                                                id: 'passthrough',
                                                description: 'passthrough'
                                            });
                                            items.push({
                                                id: 'GRID K100',
                                                description: 'GRID K100'
                                            });
                                            items.push({
                                                id: 'GRID K120Q',
                                                description: 'GRID K120Q'
                                            });
                                            items.push({
                                                id: 'GRID K140Q',
                                                description: 'GRID K140Q'
                                            });
                                            items.push({
                                                id: 'GRID K160Q',
                                                description: 'GRID K160Q'
                                            });
                                            items.push({
                                                id: 'GRID K180Q',
                                                description: 'GRID K180Q'
                                            });
                                            items.push({
                                                id: 'GRID K200',
                                                description: 'GRID K200'
                                            });
                                            items.push({
                                                id: 'GRID K220Q',
                                                description: 'GRID K220Q'
                                            });
                                            items.push({
                                                id: 'GRID K240Q',
                                                description: 'GRID K240Q'
                                            });
                                            items.push({
                                                id: 'GRID K260Q',
                                                description: 'GRID K260Q'
                                            });
                                            items.push({
                                                id: 'GRID K280Q',
                                                description: 'GRID K280Q'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },

                                    domainId: {
                                        label: 'label.domain',
                                        docID: 'helpComputeOfferingDomain',
                                        dependsOn: 'isPublic',
                                        select: function(args) {
                                            $.ajax({
                                                url: createURL("listDomains&listAll=true"),
                                                dataType: "json",
                                                async: false,
                                                success: function(json) {
                                                    var items = [];
                                                    var domainObjs = json.listdomainsresponse.domain;
                                                    $(domainObjs).each(function() {
                                                        items.push({
                                                            id: this.id,
                                                            description: this.path
                                                        });
                                                    });
                                                    items.sort(function(a, b) {
                                                        return a.description.localeCompare(b.description);
                                                    });
                                                    args.response.success({
                                                        data: items
                                                    });
                                                }
                                            });
                                        },
                                        isHidden: true
                                    }
                                }
                            },

                            action: function(args) {
                                var data = {
                                    issystem: false,
                                    name: args.data.name,
                                    displaytext: args.data.description,
                                    storageType: args.data.storageType,
                                    provisioningType :args.data.provisioningType,
                                    customized: (args.data.isCustomized == "on")
                                };

                                //custom fields (begin)
                                if (args.data.isCustomized != "on") {
                                    $.extend(data, {
                                        cpuNumber: args.data.cpuNumber
                                    });
                                    $.extend(data, {
                                        cpuSpeed: args.data.cpuSpeed
                                    });
                                    $.extend(data, {
                                        memory: args.data.memory
                                    });
                                }
                                //custom fields (end)

                                if (args.data.deploymentPlanner != null && args.data.deploymentPlanner.length > 0) {
                                    $.extend(data, {
                                        deploymentplanner: args.data.deploymentPlanner
                                    });
                                }

                                var array1 = [];
                                if (args.data.deploymentPlanner == "ImplicitDedicationPlanner" && args.data.plannerMode != "") {
                                    array1.push("&serviceofferingdetails[0].key" + "=" + "ImplicitDedicationMode");
                                    array1.push("&serviceofferingdetails[0].value" + "=" + args.data.plannerMode);
                                }

                                if (args.data.pciDevice != "") {
                                    array1.push("&serviceofferingdetails[1].key" + "=" + "pciDevice");
                                    array1.push("&serviceofferingdetails[1].value" + "=" + args.data.pciDevice);
                                }

                                if (args.data.vgpuType != "") {
                                    array1.push("&serviceofferingdetails[2].key" + "=" + "vgpuType");
                                    array1.push("&serviceofferingdetails[2].value" + "=" + args.data.vgpuType);
                                }

                                if (args.data.networkRate != null && args.data.networkRate.length > 0) {
                                    $.extend(data, {
                                        networkrate: args.data.networkRate
                                    });
                                }

                                if (args.data.qosType == 'storage') {
                                    var customIops = args.data.isCustomizedIops == "on";

                                    $.extend(data, {
                                        customizediops: customIops
                                    });

                                    if (!customIops) {
                                        if (args.data.minIops != null && args.data.minIops.length > 0) {
                                            $.extend(data, {
                                                miniops: args.data.minIops
                                            });
                                        }

                                        if (args.data.maxIops != null && args.data.maxIops.length > 0) {
                                            $.extend(data, {
                                                maxiops: args.data.maxIops
                                            });
                                        }
                                    }

                                    if (args.data.hypervisorSnapshotReserve != null && args.data.hypervisorSnapshotReserve.length > 0) {
                                        $.extend(data, {
                                            hypervisorsnapshotreserve: args.data.hypervisorSnapshotReserve
                                        });
                                    }
                                } else if (args.data.qosType == 'hypervisor') {
                                    if (args.data.diskBytesReadRate != null && args.data.diskBytesReadRate.length > 0) {
                                        $.extend(data, {
                                            bytesreadrate: args.data.diskBytesReadRate
                                        });
                                    }

                                    if (args.data.diskBytesWriteRate != null && args.data.diskBytesWriteRate.length > 0) {
                                        $.extend(data, {
                                            byteswriterate: args.data.diskBytesWriteRate
                                        });
                                    }

                                    if (args.data.diskIopsReadRate != null && args.data.diskIopsReadRate.length > 0) {
                                        $.extend(data, {
                                            iopsreadrate: args.data.diskIopsReadRate
                                        });
                                    }

                                    if (args.data.diskIopsWriteRate != null && args.data.diskIopsWriteRate.length > 0) {
                                        $.extend(data, {
                                            iopswriterate: args.data.diskIopsWriteRate
                                        });
                                    }
                                }

                                $.extend(data, {
                                    offerha: (args.data.offerHA == "on")
                                });

                                if (args.data.storageTags != null && args.data.storageTags.length > 0) {
                                    $.extend(data, {
                                        tags: args.data.storageTags
                                    });
                                }

                                if (args.data.hostTags != null && args.data.hostTags.length > 0) {
                                    $.extend(data, {
                                        hosttags: args.data.hostTags
                                    });
                                }

                                $.extend(data, {
                                    limitcpuuse: (args.data.cpuCap == "on")
                                });

                                $.extend(data, {
                                    isvolatile: (args.data.isVolatile == "on")
                                });

                                if (args.data.isPublic != "on") {
                                    $.extend(data, {
                                        domainid: args.data.domainId
                                    });
                                }

                                $.ajax({
                                    url: createURL('createServiceOffering' + array1.join("")),
                                    data: data,
                                    success: function(json) {
                                        var item = json.createserviceofferingresponse.serviceoffering;
                                        args.response.success({
                                            data: item
                                        });
                                    },
                                    error: function(data) {
                                        args.response.error(parseXMLHttpResponse(data));
                                    }
                                });
                            },

                            notification: {
                                poll: function(args) {
                                    args.complete();
                                }
                            }
                        }
                    },

                    dataProvider: function(args) {
                        var data = {};
                        listViewDataProvider(args, data);

                        $.extend(data, {
                            issystem: false
                        });

                        $.ajax({
                            url: createURL('listServiceOfferings&isrecursive=true'),
                            data: data,
                            success: function(json) {
                                var items = json.listserviceofferingsresponse.serviceoffering;
                                args.response.success({
                                    actionFilter: serviceOfferingActionfilter,
                                    data: items
                                });
                            },
                            error: function(data) {
                                args.response.error(parseXMLHttpResponse(data));
                            }
                        });
                    },

                    detailView: {
                        name: 'label.service.offering.details',
                        actions: {
                            edit: {
                                label: 'label.edit',
                                action: function(args) {
                                    var data = {
                                        id: args.context.serviceOfferings[0].id,
                                        name: args.data.name,
                                        displaytext: args.data.displaytext
                                    };
                                    $.ajax({
                                        url: createURL('updateServiceOffering'),
                                        data: data,
                                        success: function(json) {
                                            var item = json.updateserviceofferingresponse.serviceoffering;
                                            args.response.success({
                                                data: item
                                            });
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                }
                            },

                            remove: {
                                label: 'label.action.delete.service.offering',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.action.delete.service.offering';
                                    },
                                    notification: function(args) {
                                        return 'label.action.delete.service.offering';
                                    }
                                },
                                action: function(args) {
                                    var data = {
                                        id: args.context.serviceOfferings[0].id
                                    };
                                    $.ajax({
                                        url: createURL('deleteServiceOffering'),
                                        data: data,
                                        async: true,
                                        success: function(json) {
                                            args.response.success();
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                },
                                notification: {
                                    poll: function(args) {
                                        args.complete();
                                    }
                                }
                            }
                        },

                        tabs: {
                            details: {
                                title: 'label.details',

                                fields: [{
                                    name: {
                                        label: 'label.name',
                                        isEditable: true,
                                        validation: {
                                            required: true
                                        }
                                    }
                                }, {
                                    id: {
                                        label: 'label.id'
                                    },
                                    displaytext: {
                                        label: 'label.description',
                                        isEditable: true,
                                        validation: {
                                            required: true
                                        }
                                    },
                                    storagetype: {
                                        label: 'label.storage.type'
                                    },
                                    provisioningtype: {
                                        label: 'label.disk.provisioningtype'
                                    },
                                    cpunumber: {
                                        label: 'label.num.cpu.cores'
                                    },
                                    cpuspeed: {
                                        label: 'label.cpu.mhz',
                                        converter: function(args) {
                                            return cloudStack.converters.convertHz(args);
                                        }
                                    },
                                    memory: {
                                        label: 'label.memory.mb',
                                        converter: function(args) {
                                            if (args == undefined)
                                                return '';
                                            else
                                                return cloudStack.converters.convertBytes(args * 1024 * 1024);
                                        }
                                    },
                                    networkrate: {
                                        label: 'label.network.rate'
                                    },
                                    iscustomizediops: {
                                        label: 'label.custom.disk.iops',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    miniops: {
                                        label: 'label.disk.iops.min',
                                        converter: function(args) {
                                            if (args > 0)
                                                return args;
                                            else
                                                return "N/A";
                                        }
                                    },
                                    maxiops: {
                                        label: 'label.disk.iops.max',
                                        converter: function(args) {
                                            if (args > 0)
                                                return args;
                                            else
                                                return "N/A";
                                        }
                                    },
                                    hypervisorsnapshotreserve: {
                                        label: 'label.hypervisor.snapshot.reserve',
                                        converter: function(args) {
                                            if (args > 0)
                                                return args;
                                            else
                                                return "N/A";
                                        }
                                    },
                                    diskBytesReadRate: {
                                        label: 'label.disk.bytes.read.rate'
                                    },
                                    diskBytesWriteRate: {
                                        label: 'label.disk.bytes.write.rate'
                                    },
                                    diskIopsReadRate: {
                                        label: 'label.disk.iops.read.rate'
                                    },
                                    diskIopsWriteRate: {
                                        label: 'label.disk.iops.write.rate'
                                    },
                                    offerha: {
                                        label: 'label.offer.ha',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    limitcpuuse: {
                                        label: 'label.CPU.cap',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    isvolatile: {
                                        label: 'label.volatile',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    deploymentplanner: {
                                        label: 'label.deployment.planner'
                                    },
                                    plannerMode: {
                                        label: 'label.planner.mode'
                                    },
                                    pciDevice: {
                                        label: 'label.gpu'
                                    },
                                    vgpuType: {
                                        label: 'label.vgpu.type'
                                    },
                                    tags: {
                                        label: 'label.storage.tags'
                                    },
                                    hosttags: {
                                        label: 'label.host.tag'
                                    },
                                    domain: {
                                        label: 'label.domain'
                                    },
                                    created: {
                                        label: 'label.created',
                                        converter: cloudStack.converters.toLocalDate
                                    }
                                }],

                                dataProvider: function(args) {
                                    var data = {
                                        issystem: false,
                                        id: args.context.serviceOfferings[0].id
                                    };
                                    $.ajax({
                                        url: createURL('listServiceOfferings&isrecursive=true'),
                                        data: data,
                                        async: true,
                                        success: function(json) {
                                            var item = json.listserviceofferingsresponse.serviceoffering[0];

                                            if (item.deploymentplanner != null && item.serviceofferingdetails != null) {
                                                if (item.deploymentplanner == 'ImplicitDedicationPlanner' && item.serviceofferingdetails.ImplicitDedicationMode != null) {
                                                    item.plannerMode = item.serviceofferingdetails.ImplicitDedicationMode;
                                                }
                                            }

                                            if (item.serviceofferingdetails != null) {
                                                item.pciDevice = item.serviceofferingdetails.pciDevice;
                                                item.vgpuType = item.serviceofferingdetails.vgpuType;
                                            }

                                            args.response.success({
                                                actionFilter: serviceOfferingActionfilter,
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

            systemServiceOfferings: {
                type: 'select',
                title: 'label.menu.system.service.offerings',
                listView: {
                    id: 'systemServiceOfferings',
                    label: 'label.menu.system.service.offerings',
                    fields: {
                        name: {
                            label: 'label.name',
                            editable: true
                        },
                        displaytext: {
                            label: 'label.description'
                        }
                    },

                    reorder: cloudStack.api.actions.sort('updateServiceOffering', 'systemServiceOfferings'),

                    actions: {
                        add: {
                            label: 'label.add.system.service.offering',

                            messages: {
                                confirm: function(args) {
                                    return 'message.add.system.service.offering';
                                },
                                notification: function(args) {
                                    return 'label.add.system.service.offering';
                                }
                            },

                            createForm: {
                                title: 'label.add.system.service.offering',
                                preFilter: function(args) {
                                    if (isAdmin()) {
                                    } else {
                                        args.$form.find('.form-item[rel=isPublic]').hide();
                                        args.$form.find('.form-item[rel=domainId]').css('display', 'inline-block'); //shown
                                    }
                                },
                                fields: {
                                    name: {
                                        label: 'label.name',
                                        validation: {
                                            required: true
                                        },
                                        docID: 'helpSystemOfferingName'
                                    },
                                    description: {
                                        label: 'label.description',
                                        validation: {
                                            required: true
                                        },
                                        docID: 'helpSystemOfferingDescription'
                                    },
                                    systemvmtype: {
                                        label: 'label.system.vm.type',
                                        docID: 'helpSystemOfferingVMType',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: 'domainrouter',
                                                description: _l('label.domain.router')
                                            });
                                            items.push({
                                                id: 'consoleproxy',
                                                description: _l('label.console.proxy')
                                            });
                                            items.push({
                                                id: 'secondarystoragevm',
                                                description: _l('label.secondary.storage.vm')
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    storageType: {
                                        label: 'label.storage.type',
                                        docID: 'helpSystemOfferingStorageType',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: 'shared',
                                                description: 'shared'
                                            });
                                            items.push({
                                                id: 'local',
                                                description: 'local'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    provisioningType: {
                                        label: 'label.disk.provisioningtype',
                                        docID: 'helpDiskOfferingProvisioningType',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: 'thin',
                                                description: 'thin'
                                            });
                                            items.push({
                                                id: 'sparse',
                                                description: 'sparse'
                                            });
                                            items.push({
                                                id: 'fat',
                                                description: 'fat'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    cpuNumber: {
                                        label: 'label.num.cpu.cores',
                                        docID: 'helpSystemOfferingCPUCores',
                                        validation: {
                                            required: true,
                                            number: true
                                        }
                                    },
                                    cpuSpeed: {
                                        label: 'label.cpu.mhz',
                                        docID: 'helpSystemOfferingCPUMHz',
                                        validation: {
                                            required: true,
                                            number: true
                                        }
                                    },
                                    memory: {
                                        label: 'label.memory.mb',
                                        docID: 'helpSystemOfferingMemory',
                                        validation: {
                                            required: true,
                                            number: true
                                        }
                                    },
                                    networkRate: {
                                        label: 'label.network.rate',
                                        docID: 'helpSystemOfferingNetworkRate',
                                        validation: {
                                            required: false, //optional
                                            number: true
                                        }
                                    },
                                    diskBytesReadRate: {
                                        label: 'label.disk.bytes.read.rate',
                                        docID: 'helpSystemOfferingDiskBytesReadRate',
                                        validation: {
                                            required: false, //optional
                                            number: true
                                        }
                                    },
                                    diskBytesWriteRate: {
                                        label: 'label.disk.bytes.write.rate',
                                        docID: 'helpSystemOfferingDiskBytesWriteRate',
                                        validation: {
                                            required: false, //optional
                                            number: true
                                        }
                                    },
                                    diskIopsReadRate: {
                                        label: 'label.disk.iops.read.rate',
                                        docID: 'helpSystemOfferingDiskIopsReadRate',
                                        validation: {
                                            required: false, //optional
                                            number: true
                                        }
                                    },
                                    diskIopsWriteRate: {
                                        label: 'label.disk.iops.write.rate',
                                        docID: 'helpSystemOfferingDiskIopsWriteRate',
                                        validation: {
                                            required: false, //optional
                                            number: true
                                        }
                                    },
                                    offerHA: {
                                        label: 'label.offer.ha',
                                        docID: 'helpSystemOfferingHA',
                                        isBoolean: true,
                                        isChecked: false
                                    },
                                    storageTags: {
                                        label: 'label.storage.tags',
                                        docID: 'helpSystemOfferingStorageTags'
                                    },
                                    hostTags: {
                                        label: 'label.host.tags',
                                        docID: 'helpSystemOfferingHostTags'
                                    },
                                    cpuCap: {
                                        label: 'label.CPU.cap',
                                        isBoolean: true,
                                        isChecked: false,
                                        docID: 'helpSystemOfferingCPUCap'
                                    },
                                    isPublic: {
                                        label: 'label.public',
                                        isBoolean: true,
                                        isReverse: true,
                                        isChecked: false,
                                        docID: 'helpSystemOfferingPublic'
                                    },
                                    domainId: {
                                        label: 'label.domain',
                                        docID: 'helpSystemOfferingDomain',
                                        dependsOn: 'isPublic',
                                        select: function(args) {
                                            $.ajax({
                                                url: createURL("listDomains&listAll=true"),
                                                dataType: "json",
                                                async: false,
                                                success: function(json) {
                                                    var items = [];
                                                    var domainObjs = json.listdomainsresponse.domain;
                                                    $(domainObjs).each(function() {
                                                        items.push({
                                                            id: this.id,
                                                            description: this.path
                                                        });
                                                    });
                                                    items.sort(function(a, b) {
                                                        return a.description.localeCompare(b.description);
                                                    });
                                                    args.response.success({
                                                        data: items
                                                    });
                                                }
                                            });
                                        },
                                        isHidden: true
                                    }
                                }
                            },

                            action: function(args) {
                                var data = {
                                    issystem: true,
                                    name: args.data.name,
                                    displaytext: args.data.description,
                                    systemvmtype: args.data.systemvmtype,
                                    storageType: args.data.storageType,
                                    provisioningType: args.data.provisioningType,
                                    cpuNumber: args.data.cpuNumber,
                                    cpuSpeed: args.data.cpuSpeed,
                                    memory: args.data.memory
                                };

                                if (args.data.networkRate != null && args.data.networkRate.length > 0) {
                                    $.extend(data, {
                                        networkrate: args.data.networkRate
                                    });
                                }
                                if (args.data.diskBytesReadRate != null && args.data.diskBytesReadRate.length > 0) {
                                    $.extend(data, {
                                        bytesreadrate: args.data.diskBytesReadRate
                                    });
                                }
                                if (args.data.diskBytesWriteRate != null && args.data.diskBytesWriteRate.length > 0) {
                                    $.extend(data, {
                                        byteswriterate: args.data.diskBytesWriteRate
                                    });
                                }
                                if (args.data.diskIopsReadRate != null && args.data.diskIopsReadRate.length > 0) {
                                    $.extend(data, {
                                        iopsreadrate: args.data.diskIopsReadRate
                                    });
                                }
                                if (args.data.diskIopsWriteRate != null && args.data.diskIopsWriteRate.length > 0) {
                                    $.extend(data, {
                                        iopswriterate: args.data.diskIopsWriteRate
                                    });
                                }

                                $.extend(data, {
                                    offerha: (args.data.offerHA == "on")
                                });

                                if (args.data.storageTags != null && args.data.storageTags.length > 0) {
                                    $.extend(data, {
                                        tags: args.data.storageTags
                                    });
                                }

                                if (args.data.hostTags != null && args.data.hostTags.length > 0) {
                                    $.extend(data, {
                                        hosttags: args.data.hostTags
                                    });
                                }

                                $.extend(data, {
                                    limitcpuuse: (args.data.cpuCap == "on")
                                });

                                if (args.data.isPublic != "on") {
                                    $.extend(data, {
                                        domainid: args.data.domainId
                                    });
                                }

                                $.ajax({
                                    url: createURL('createServiceOffering'),
                                    data: data,
                                    success: function(json) {
                                        var item = json.createserviceofferingresponse.serviceoffering;
                                        args.response.success({
                                            data: item
                                        });
                                    },
                                    error: function(data) {
                                        args.response.error(parseXMLHttpResponse(data));
                                    }
                                });
                            },

                            notification: {
                                poll: function(args) {
                                    args.complete();
                                }
                            }
                        }
                    },

                    dataProvider: function(args) {
                        var data = {};
                        listViewDataProvider(args, data);

                        $.extend(data, {
                            issystem: true
                        });

                        $.ajax({
                            url: createURL('listServiceOfferings&isrecursive=true'),
                            data: data,
                            success: function(json) {
                                var items = json.listserviceofferingsresponse.serviceoffering;
                                args.response.success({
                                    data: items
                                });
                            },
                            error: function(data) {
                                args.response.error(parseXMLHttpResponse(data));
                            }
                        });
                    },

                    detailView: {
                        name: 'label.system.service.offering.details',
                        actions: {
                            edit: {
                                label: 'label.edit',
                                action: function(args) {
                                    var data = {
                                        id: args.context.systemServiceOfferings[0].id,
                                        name: args.data.name,
                                        displaytext: args.data.displaytext
                                    };
                                    $.ajax({
                                        url: createURL('updateServiceOffering'),
                                        data: data,
                                        success: function(json) {
                                            var item = json.updateserviceofferingresponse.serviceoffering;
                                            args.response.success({
                                                data: item
                                            });
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                }
                            },

                            remove: {
                                label: 'label.action.delete.system.service.offering',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.action.delete.system.service.offering';
                                    },
                                    notification: function(args) {
                                        return 'label.action.delete.system.service.offering';
                                    }
                                },
                                action: function(args) {
                                    var data = {
                                        id: args.context.systemServiceOfferings[0].id
                                    };
                                    $.ajax({
                                        url: createURL('deleteServiceOffering'),
                                        data: data,
                                        success: function(json) {
                                            args.response.success();
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                },
                                notification: {
                                    poll: function(args) {
                                        args.complete();
                                    }
                                }
                            }
                        },

                        tabs: {
                            details: {
                                title: 'label.details',

                                fields: [{
                                    name: {
                                        label: 'label.name',
                                        isEditable: true,
                                        validation: {
                                            required: true
                                        }
                                    }
                                }, {
                                    id: {
                                        label: 'label.id'
                                    },
                                    displaytext: {
                                        label: 'label.description',
                                        isEditable: true,
                                        validation: {
                                            required: true
                                        }
                                    },
                                    systemvmtype: {
                                        label: 'label.system.vm.type',
                                        converter: function(args) {
                                            var text = '';
                                            switch (args) {
                                                case 'domainrouter':
                                                    text = _l('label.domain.router');
                                                    break;
                                                case 'consoleproxy':
                                                    text = _l('label.console.proxy');
                                                    break;
                                                case 'secondarystoragevm':
                                                    text = _l('label.secondary.storage.vm');
                                                    break;
                                            }
                                            return text;
                                        }
                                    },
                                    storagetype: {
                                        label: 'label.storage.type'
                                    },
                                    provisioningtype: {
                                        label: 'label.disk.provisioningtype'
                                    },
                                    cpunumber: {
                                        label: 'label.num.cpu.cores'
                                    },
                                    cpuspeed: {
                                        label: 'label.cpu.mhz',
                                        converter: function(args) {
                                            return cloudStack.converters.convertHz(args);
                                        }
                                    },
                                    memory: {
                                        label: 'label.memory.mb',
                                        converter: function(args) {
                                            if (args == undefined)
                                                return '';
                                            else
                                                return cloudStack.converters.convertBytes(args * 1024 * 1024);
                                        }
                                    },
                                    networkrate: {
                                        label: 'label.network.rate'
                                    },
                                    diskBytesReadRate: {
                                        label: 'label.disk.bytes.read.rate'
                                    },
                                    diskBytesWriteRate: {
                                        label: 'label.disk.bytes.write.rate'
                                    },
                                    diskIopsReadRate: {
                                        label: 'label.disk.iops.read.rate'
                                    },
                                    diskIopsWriteRate: {
                                        label: 'label.disk.iops.write.rate'
                                    },
                                    offerha: {
                                        label: 'label.offer.ha',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    limitcpuuse: {
                                        label: 'label.CPU.cap',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    tags: {
                                        label: 'label.storage.tags'
                                    },
                                    hosttags: {
                                        label: 'label.host.tags'
                                    },
                                    domain: {
                                        label: 'label.domain'
                                    },
                                    created: {
                                        label: 'label.created',
                                        converter: cloudStack.converters.toLocalDate
                                    }
                                }],

                                dataProvider: function(args) {
                                    var data = {
                                        issystem: true,
                                        id: args.context.systemServiceOfferings[0].id
                                    };
                                    $.ajax({
                                        url: createURL('listServiceOfferings&isrecursive=true'),
                                        data: data,
                                        success: function(json) {
                                            var item = json.listserviceofferingsresponse.serviceoffering[0];
                                            args.response.success({
                                                actionFilter: systemServiceOfferingActionfilter,
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

            diskOfferings: {
                type: 'select',
                title: 'label.menu.disk.offerings',
                listView: {
                    id: 'diskOfferings',
                    label: 'label.menu.disk.offerings',
                    fields: {
                        name: {
                            label: 'label.name'
                        },
                        displaytext: {
                            label: 'label.description'
                        },
                        iscustomized: {
                            label: 'label.custom.disk.size',
                            converter: cloudStack.converters.toBooleanText
                        },
                        disksize: {
                            label: 'label.disk.size.gb',
                            converter: function(args) {
                                if (args != 0)
                                    return args;
                                else
                                    return "N/A";
                            }
                        }
                    },

                    reorder: cloudStack.api.actions.sort('updateDiskOffering', 'diskOfferings'),

                    dataProvider: function(args) {
                        var data = {};
                        listViewDataProvider(args, data);

                        $.ajax({
                            url: createURL('listDiskOfferings&isrecursive=true'),
                            data: data,
                            success: function(json) {
                                var items = json.listdiskofferingsresponse.diskoffering;
                                args.response.success({
                                    data: items
                                });
                            },
                            error: function(data) {
                                args.response.error(parseXMLHttpResponse(data));
                            }
                        });
                    },

                    actions: {
                        add: {
                            label: 'label.add.disk.offering',

                            messages: {
                                confirm: function(args) {
                                    return 'message.add.disk.offering';
                                },
                                notification: function(args) {
                                    return 'label.add.disk.offering';
                                }
                            },

                            createForm: {
                                title: 'label.add.disk.offering',
                                preFilter: function(args) {
                                    if (isAdmin()) {
                                    } else {
                                        args.$form.find('.form-item[rel=isPublic]').hide();
                                        args.$form.find('.form-item[rel=domainId]').css('display', 'inline-block'); //shown
                                        args.$form.find('.form-item[rel=tags]').hide();
                                    }
                                },
                                fields: {
                                    name: {
                                        label: 'label.name',
                                        docID: 'helpDiskOfferingName',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    description: {
                                        label: 'label.description',
                                        docID: 'helpDiskOfferingDescription',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    storageType: {
                                        label: 'label.storage.type',
                                        docID: 'helpDiskOfferingStorageType',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: 'shared',
                                                description: 'shared'
                                            });
                                            items.push({
                                                id: 'local',
                                                description: 'local'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    provisioningType: {
                                        label: 'label.disk.provisioningtype',
                                        docID: 'helpDiskOfferingProvisioningType',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: 'thin',
                                                description: 'thin'
                                            });
                                            items.push({
                                                id: 'sparse',
                                                description: 'sparse'
                                            });
                                            items.push({
                                                id: 'fat',
                                                description: 'fat'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    isCustomized: {
                                        label: 'label.custom.disk.size',
                                        docID: 'helpDiskOfferingCustomDiskSize',
                                        isBoolean: true,
                                        isReverse: true,
                                        isChecked: false
                                    },
                                    disksize: {
                                        label: 'label.disk.size.gb',
                                        docID: 'helpDiskOfferingDiskSize',
                                        dependsOn: 'isCustomized',
                                        validation: {
                                            required: true,
                                            number: true
                                        }
                                    },
                                    qosType: {
                                        label: 'label.qos.type',
                                        docID: 'helpDiskOfferingQoSType',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: '',
                                                description: ''
                                            });
                                            items.push({
                                                id: 'hypervisor',
                                                description: 'hypervisor'
                                            });
                                            items.push({
                                                id: 'storage',
                                                description: 'storage'
                                            });
                                            args.response.success({
                                                data: items
                                            });

                                            args.$select.change(function() {
                                                var $form = $(this).closest('form');
                                                var $isCustomizedIops = $form.find('.form-item[rel=isCustomizedIops]');
                                                var $minIops = $form.find('.form-item[rel=minIops]');
                                                var $maxIops = $form.find('.form-item[rel=maxIops]');
                                                var $hypervisorSnapshotReserve = $form.find('.form-item[rel=hypervisorSnapshotReserve]');
                                                var $diskBytesReadRate = $form.find('.form-item[rel=diskBytesReadRate]');
                                                var $diskBytesWriteRate = $form.find('.form-item[rel=diskBytesWriteRate]');
                                                var $diskIopsReadRate = $form.find('.form-item[rel=diskIopsReadRate]');
                                                var $diskIopsWriteRate = $form.find('.form-item[rel=diskIopsWriteRate]');

                                                var qosId = $(this).val();

                                                if (qosId == 'storage') { // Storage QoS
                                                    $diskBytesReadRate.hide();
                                                    $diskBytesWriteRate.hide();
                                                    $diskIopsReadRate.hide();
                                                    $diskIopsWriteRate.hide();

                                                    $isCustomizedIops.css('display', 'inline-block');

                                                    if ($isCustomizedIops.find('input[type=checkbox]').is(':checked')) {
                                                        $minIops.hide();
                                                        $maxIops.hide();
                                                    } else {
                                                        $minIops.css('display', 'inline-block');
                                                        $maxIops.css('display', 'inline-block');
                                                    }

                                                    $hypervisorSnapshotReserve.css('display', 'inline-block');
                                                } else if (qosId == 'hypervisor') { // Hypervisor Qos
                                                    $isCustomizedIops.hide();
                                                    $minIops.hide();
                                                    $maxIops.hide();
                                                    $hypervisorSnapshotReserve.hide();

                                                    $diskBytesReadRate.css('display', 'inline-block');
                                                    $diskBytesWriteRate.css('display', 'inline-block');
                                                    $diskIopsReadRate.css('display', 'inline-block');
                                                    $diskIopsWriteRate.css('display', 'inline-block');
                                                } else { // No Qos
                                                    $diskBytesReadRate.hide();
                                                    $diskBytesWriteRate.hide();
                                                    $diskIopsReadRate.hide();
                                                    $diskIopsWriteRate.hide();
                                                    $isCustomizedIops.hide();
                                                    $minIops.hide();
                                                    $maxIops.hide();
                                                    $hypervisorSnapshotReserve.hide();
                                                }
                                            });
                                        }
                                    },
                                    isCustomizedIops: {
                                        label: 'label.custom.disk.iops',
                                        docID: 'helpDiskOfferingCustomDiskIops',
                                        isBoolean: true,
                                        isReverse: true,
                                        isChecked: false
                                    },
                                    minIops: {
                                        label: 'label.disk.iops.min',
                                        docID: 'helpDiskOfferingDiskIopsMin',
                                        dependsOn: 'isCustomizedIops',
                                        validation: {
                                            required: false,
                                            number: true
                                        }
                                    },
                                    maxIops: {
                                        label: 'label.disk.iops.max',
                                        docID: 'helpDiskOfferingDiskIopsMax',
                                        dependsOn: 'isCustomizedIops',
                                        validation: {
                                            required: false,
                                            number: true
                                        }
                                    },
                                    hypervisorSnapshotReserve: {
                                        label: 'label.hypervisor.snapshot.reserve',
                                        docID: 'helpDiskOfferingHypervisorSnapshotReserve',
                                        validation: {
                                            required: false,
                                            number: true
                                        }
                                    },
                                    diskBytesReadRate: {
                                        label: 'label.disk.bytes.read.rate',
                                        docID: 'helpDiskOfferingDiskBytesReadRate',
                                        validation: {
                                            required: false, //optional
                                            number: true
                                        }
                                    },
                                    diskBytesWriteRate: {
                                        label: 'label.disk.bytes.write.rate',
                                        docID: 'helpDiskOfferingDiskBytesWriteRate',
                                        validation: {
                                            required: false, //optional
                                            number: true
                                        }
                                    },
                                    diskIopsReadRate: {
                                        label: 'label.disk.iops.read.rate',
                                        docID: 'helpDiskOfferingDiskIopsReadRate',
                                        validation: {
                                            required: false, //optional
                                            number: true
                                        }
                                    },
                                    diskIopsWriteRate: {
                                        label: 'label.disk.iops.write.rate',
                                        docID: 'helpDiskOfferingDiskIopsWriteRate',
                                        validation: {
                                            required: false, //optional
                                            number: true
                                        }
                                    },
                                    cacheMode: {
                                        label: 'label.cache.mode',
                                        docID: 'helpDiskOfferingCacheMode',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: 'none',
                                                description: 'No disk cache'
                                            });
                                            items.push({
                                                id: 'writeback',
                                                description: 'Write-back disk caching'
                                            });
                                            items.push({
                                                id: 'writethrough',
                                                description: 'Write-through disk caching'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    tags: {
                                        label: 'label.storage.tags',
                                        docID: 'helpDiskOfferingStorageTags',
                                        isTokenInput: true,
                                        dataProvider: function(args) {
                                            $.ajax({
                                                url: createURL("listStorageTags"),
                                                dataType: "json",
                                                success: function(json) {
                                                    var item = json.liststoragetagsresponse.storagetag;
                                                    var tags = [];

                                                    if (item != null)
                                                    {
                                                        tags = $.map(item, function(tag) {
                                                            return {
                                                                       id: tag.name,
                                                                       name: tag.name
                                                                   };
                                                        });
                                                    }

                                                    args.response.success({
                                                        data: tags,
                                                        hintText: _l('hint.type.part.storage.tag'),
                                                        noResultsText: _l('hint.no.storage.tags')
                                                    });
                                                },
                                                error: function(XMLHttpResponse) {
                                                    var errorMsg = parseXMLHttpResponse(XMLHttpResponse);

                                                    args.response.error(errorMsg);
                                                }
                                            });
                                        }
                                    },
                                    isPublic: {
                                        label: 'label.public',
                                        isBoolean: true,
                                        isReverse: true,
                                        isChecked: false,
                                        docID: 'helpDiskOfferingPublic'
                                    },
                                    domainId: {
                                        label: 'label.domain',
                                        docID: 'helpDiskOfferingDomain',
                                        dependsOn: 'isPublic',
                                        select: function(args) {
                                            $.ajax({
                                                url: createURL("listDomains&listAll=true"),
                                                dataType: "json",
                                                async: false,
                                                success: function(json) {
                                                    var items = [];
                                                    var domainObjs = json.listdomainsresponse.domain;
                                                    $(domainObjs).each(function() {
                                                        items.push({
                                                            id: this.id,
                                                            description: this.path
                                                        });
                                                    });
                                                    items.sort(function(a, b) {
                                                        return a.description.localeCompare(b.description);
                                                    });
                                                    args.response.success({
                                                        data: items
                                                    });
                                                }
                                            });
                                        },
                                        isHidden: true
                                    }
                                }
                            },

                            action: function(args) {
                                var data = {
                                    isMirrored: false,
                                    name: args.data.name,
                                    displaytext: args.data.description,
                                    storageType: args.data.storageType,
                                    cacheMode: args.data.cacheMode,
                                    provisioningType: args.data.provisioningType,
                                    customized: (args.data.isCustomized == "on")
                                };

                                if (args.data.isCustomized != "on") {
                                    $.extend(data, {
                                        disksize: args.data.disksize
                                    });
                                }

                                if (args.data.qosType == 'storage') {
                                    var customIops = args.data.isCustomizedIops == "on";

                                    $.extend(data, {
                                        customizediops: customIops
                                    });

                                    if (!customIops) {
                                        if (args.data.minIops != null && args.data.minIops.length > 0) {
                                            $.extend(data, {
                                                miniops: args.data.minIops
                                            });
                                        }

                                        if (args.data.maxIops != null && args.data.maxIops.length > 0) {
                                            $.extend(data, {
                                                maxiops: args.data.maxIops
                                            });
                                        }
                                    }

                                    if (args.data.hypervisorSnapshotReserve != null && args.data.hypervisorSnapshotReserve.length > 0) {
                                        $.extend(data, {
                                            hypervisorsnapshotreserve: args.data.hypervisorSnapshotReserve
                                        });
                                    }
                                } else if (args.data.qosType == 'hypervisor') {
                                    if (args.data.diskBytesReadRate != null && args.data.diskBytesReadRate.length > 0) {
                                        $.extend(data, {
                                            bytesreadrate: args.data.diskBytesReadRate
                                        });
                                    }

                                    if (args.data.diskBytesWriteRate != null && args.data.diskBytesWriteRate.length > 0) {
                                        $.extend(data, {
                                            byteswriterate: args.data.diskBytesWriteRate
                                        });
                                    }

                                    if (args.data.diskIopsReadRate != null && args.data.diskIopsReadRate.length > 0) {
                                        $.extend(data, {
                                            iopsreadrate: args.data.diskIopsReadRate
                                        });
                                    }

                                    if (args.data.diskIopsWriteRate != null && args.data.diskIopsWriteRate.length > 0) {
                                        $.extend(data, {
                                            iopswriterate: args.data.diskIopsWriteRate
                                        });
                                    }
                                }

                                if (args.data.tags != null && args.data.tags.length > 0) {
                                    $.extend(data, {
                                        tags: args.data.tags
                                    });
                                }

                                if (args.data.isPublic != "on") {
                                    $.extend(data, {
                                        domainid: args.data.domainId
                                    });
                                }

                                $.ajax({
                                    url: createURL('createDiskOffering'),
                                    data: data,
                                    success: function(json) {
                                        var item = json.creatediskofferingresponse.diskoffering;
                                        args.response.success({
                                            data: item
                                        });
                                    },
                                    error: function(data) {
                                        args.response.error(parseXMLHttpResponse(data));
                                    }
                                });
                            },

                            notification: {
                                poll: function(args) {
                                    args.complete();
                                }
                            }
                        }
                    },

                    detailView: {
                        name: 'label.disk.offering.details',
                        actions: {
                            edit: {
                                label: 'label.edit',
                                action: function(args) {
                                    var data = {
                                        id: args.context.diskOfferings[0].id,
                                        name: args.data.name,
                                        displaytext: args.data.displaytext
                                    };
                                    $.ajax({
                                        url: createURL('updateDiskOffering'),
                                        data: data,
                                        success: function(json) {
                                            var item = json.updatediskofferingresponse.diskoffering;
                                            args.response.success({
                                                data: item
                                            });
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                }
                            },

                            remove: {
                                label: 'label.action.delete.disk.offering',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.action.delete.disk.offering';
                                    },
                                    notification: function(args) {
                                        return 'label.action.delete.disk.offering';
                                    }
                                },
                                action: function(args) {
                                    var data = {
                                        id: args.context.diskOfferings[0].id
                                    };
                                    $.ajax({
                                        url: createURL('deleteDiskOffering'),
                                        data: data,
                                        success: function(json) {
                                            args.response.success();
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                },
                                notification: {
                                    poll: function(args) {
                                        args.complete();
                                    }
                                }
                            }
                        },

                        tabs: {
                            details: {
                                title: 'label.details',

                                fields: [{
                                    name: {
                                        label: 'label.name',
                                        isEditable: true,
                                        validation: {
                                            required: true
                                        }
                                    }
                                }, {
                                    id: {
                                        label: 'label.id'
                                    },
                                    displaytext: {
                                        label: 'label.description',
                                        isEditable: true,
                                        validation: {
                                            required: true
                                        }
                                    },
                                    iscustomized: {
                                        label: 'label.custom.disk.size',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    disksize: {
                                        label: 'label.disk.size.gb',
                                        converter: function(args) {
                                            if (args != 0)
                                                return args;
                                            else
                                                return "N/A";
                                        }
                                    },
                                    iscustomizediops: {
                                        label: 'label.custom.disk.iops',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    miniops: {
                                        label: 'label.disk.iops.min',
                                        converter: function(args) {
                                            if (args > 0)
                                                return args;
                                            else
                                                return "N/A";
                                        }
                                    },
                                    maxiops: {
                                        label: 'label.disk.iops.max',
                                        converter: function(args) {
                                            if (args > 0)
                                                return args;
                                            else
                                                return "N/A";
                                        }
                                    },
                                    hypervisorsnapshotreserve: {
                                        label: 'label.hypervisor.snapshot.reserve',
                                        converter: function(args) {
                                            if (args > 0)
                                                return args;
                                            else
                                                return "N/A";
                                        }
                                    },
                                    diskBytesReadRate: {
                                        label: 'label.disk.bytes.read.rate'
                                    },
                                    diskBytesWriteRate: {
                                        label: 'label.disk.bytes.write.rate'
                                    },
                                    diskIopsReadRate: {
                                        label: 'label.disk.iops.read.rate'
                                    },
                                    diskIopsWriteRate: {
                                        label: 'label.disk.iops.write.rate'
                                    },
                                    cacheMode: {
                                        label: 'label.cache.mode'
                                    },
                                    tags: {
                                        label: 'label.storage.tags'
                                    },
                                    domain: {
                                        label: 'label.domain'
                                    },
                                    storagetype: {
                                        label: 'label.storage.type'
                                    },
                                    provisioningtype: {
                                        label: 'label.disk.provisioningtype'
                                    }
                                }],

                                dataProvider: function(args) {
                                    var data = {
                                        id: args.context.diskOfferings[0].id
                                    };
                                    $.ajax({
                                        url: createURL('listDiskOfferings&isrecursive=true'),
                                        data: data,
                                        success: function(json) {
                                            var item = json.listdiskofferingsresponse.diskoffering[0];
                                            args.response.success({
                                                actionFilter: diskOfferingActionfilter,
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

            networkOfferings: {
                type: 'select',
                title: 'label.menu.network.offerings',
                listView: {
                    id: 'networkOfferings',
                    label: 'label.menu.network.offerings',
                    fields: {
                        name: {
                            label: 'label.name'
                        },
                        state: {
                            label: 'label.state',
                            indicator: {
                                'Enabled': 'on',
                                'Disabled': 'off',
                                'Destroyed': 'off'
                            }
                        }
                    },

                    dataProvider: function(args) {
                        var data = {};
                        listViewDataProvider(args, data);

                        $.ajax({
                            url: createURL('listNetworkOfferings'),
                            data: data,
                            success: function(json) {
                                var items = json.listnetworkofferingsresponse.networkoffering;

                                $(items).each(function() {
                                    if (this.availability == "Required") {
                                        requiredNetworkOfferingExists = true;
                                        return false; //break each loop
                                    }
                                });

                                args.response.success({
                                    actionFilter: networkOfferingActionfilter,
                                    data: items
                                });
                            },
                            error: function(data) {
                                args.response.error(parseXMLHttpResponse(data));
                            }
                        });
                    },

                    actions: {
                        add: {
                            label: 'label.add.network.offering',

                            createForm: {
                                title: 'label.add.network.offering',
                                preFilter: function(args) {
                                    var $availability = args.$form.find('.form-item[rel=availability]');
                                    var $lbType = args.$form.find('.form-item[rel=lbType]');
                                    var $serviceofferingid = args.$form.find('.form-item[rel=serviceofferingid]');
                                    var $conservemode = args.$form.find('.form-item[rel=conservemode]');
                                    var $supportsstrechedl2subnet = args.$form.find('.form-item[rel=supportsstrechedl2subnet]');
                                    var $serviceSourceNatRedundantRouterCapabilityCheckbox = args.$form.find('.form-item[rel="service.SourceNat.redundantRouterCapabilityCheckbox"]');
                                    var hasAdvancedZones = false;

                                    // Check whether there are any advanced zones
                                    $.ajax({
                                        url: createURL('listZones'),
                                        data: {},
                                        async: false,
                                        success: function(json) {
                                            var zones = json.listzonesresponse.zone;
                                            if (zones != null && zones.length > 0) {
                                                for (var i = 0; i < zones.length; i++) {
                                                    if (zones[i].networktype == "Advanced")
                                                        hasAdvancedZones = true;
                                                }
                                            }
                                        }
                                    });

                                    args.$form.bind('change', function() { //when any field in the dialog is changed
                                        //check whether to show or hide availability field
                                        var $sourceNATField = args.$form.find('input[name=\"service.SourceNat.isEnabled\"]');
                                        var $guestTypeField = args.$form.find('select[name=guestIpType]');

                                        //*** VPC checkbox ***
                                        var $useVpc = args.$form.find('.form-item[rel=\"useVpc\"]');
                                        var $useVpcCb = $useVpc.find("input[type=checkbox]");
                                        if ($guestTypeField.val() == 'Shared') { //Shared network offering
                                            $useVpc.hide();
                                            if ($useVpcCb.is(':checked')) { //if useVpc is checked,
                                                $useVpcCb.removeAttr("checked"); //remove "checked" attribute in useVpc
                                            }
                                        } else { //Isolated network offering
                                            $useVpc.css('display', 'inline-block');
                                        }
                                        var $providers = $useVpcCb.closest('form').find('.dynamic-input select[name!="service.Connectivity.provider"]');
                                        var $optionsOfProviders = $providers.find('option');
                                        //p.s. Netscaler is supported in both vpc and non-vpc
                                        if ($useVpc.is(':visible') && $useVpcCb.is(':checked')) { //*** vpc ***
                                            $optionsOfProviders.each(function(index) {
                                                if ($(this).val() == 'InternalLbVm' || $(this).val() == 'VpcVirtualRouter' || $(this).val() == 'Netscaler'  || $(this).val() == 'NuageVsp' || $(this).val() == 'NuageVspVpc' || $(this).val() == 'BigSwitchBcf') {
                                                    $(this).attr('disabled', false);
                                                } else {
                                                    $(this).attr('disabled', true);
                                                }
                                            });
                                        } else { //*** non-vpc ***
                                            $optionsOfProviders.each(function(index) {
                                                if ($(this).val() == 'InternalLbVm' || $(this).val() == 'VpcVirtualRouter') {
                                                    $(this).attr('disabled', true);
                                                } else {
                                                    $(this).attr('disabled', false);
                                                }
                                            });
                                        }
                                        $providers.each(function() {
                                            //if selected option is disabled, select the first enabled option instead
                                            if ($(this).find('option:selected:disabled').length > 0) {
                                                $(this).val($(this).find('option:first'));
                                            }
                                        });



                                        if (!requiredNetworkOfferingExists &&
                                            $sourceNATField.is(':checked') &&
                                            $guestTypeField.val() == 'Isolated') {
                                            $availability.css('display', 'inline-block');
                                        } else {
                                            $availability.hide();
                                        }


                                        //*** LB providers ***
                                        var $lbProvider = args.$form.find('.form-item[rel=\"service.Lb.provider\"]').find('select');
                                        var $lbProviderOptions = $lbProvider.find('option');
                                        //when useVpc is checked and service.Lb.isEnabled is checked
                                        if ($useVpcCb.is(':checked') && $("input[name='service.Lb.isEnabled']").is(":checked") == true) {
                                            $lbType.css('display', 'inline-block');

                                            if ($lbType.find('select').val() == 'publicLb') { //disable all providers except the ones in lbProviderMap.publicLb.vpc => ["VpcVirtualRouter", "Netscaler"]
                                                for (var i = 0; i < $lbProviderOptions.length; i++) {
                                                    var $option = $lbProviderOptions.eq(i);
                                                    var supportedProviders = lbProviderMap.publicLb.vpc;
                                                    var thisOpionIsSupported = false;
                                                    for (var k = 0; k < supportedProviders.length; k++) {
                                                        if ($option.val() == supportedProviders[k]) {
                                                            thisOpionIsSupported = true;
                                                            break;
                                                        }
                                                    }
                                                    if (thisOpionIsSupported == true) {
                                                        $option.attr('disabled', false);
                                                    } else {
                                                        $option.attr('disabled', true);
                                                    }
                                                }
                                            } else if ($lbType.find('select').val() == 'internalLb') { //disable all providers except the ones in lbProviderMap.internalLb.vpc => ["InternalLbVm"]
                                                for (var i = 0; i < $lbProviderOptions.length; i++) {
                                                    var $option = $lbProviderOptions.eq(i);
                                                    var supportedProviders = lbProviderMap.internalLb.vpc;
                                                    var thisOpionIsSupported = false;
                                                    for (var k = 0; k < supportedProviders.length; k++) {
                                                        if ($option.val() == supportedProviders[k]) {
                                                            thisOpionIsSupported = true;
                                                            break;
                                                        }
                                                    }
                                                    if (thisOpionIsSupported == true) {
                                                        $option.attr('disabled', false);
                                                    } else {
                                                        $option.attr('disabled', true);
                                                    }
                                                }
                                            }

                                            //if selected option is disabled, select the first enabled option instead
                                            if ($lbProvider.find('option:selected:disabled').length > 0) {
                                                $lbProvider.val($lbProvider.find('option:first'));
                                            }
                                        } else {
                                            $lbType.hide();
                                        }

                                        //when service(s) has Virtual Router as provider.....
                                        var havingVirtualRouterForAtLeastOneService = false;
                                        $(serviceCheckboxNames).each(function() {
                                            var checkboxName = this;
                                            if ($("input[name='" + checkboxName + "']").is(":checked") == true) {
                                                var providerFieldName = checkboxName.replace(".isEnabled", ".provider"); //either dropdown or input hidden field
                                                var providerName = $("[name='" + providerFieldName + "']").val();
                                                if (providerName == "VirtualRouter" || providerName == "VpcVirtualRouter") {
                                                    havingVirtualRouterForAtLeastOneService = true;
                                                    return false; //break each loop
                                                }
                                            }
                                        });
                                        if (havingVirtualRouterForAtLeastOneService == true) {
                                            $serviceofferingid.css('display', 'inline-block');
                                        } else {
                                            $serviceofferingid.hide();
                                        }


                                        /*
                                        when service(s) has VPC Virtual Router as provider:
                    (1) conserve mode is set to unchecked and grayed out.
                    (2) redundant router capability checkbox is set to unchecked and grayed out.
                    (3) remove Firewall service, SecurityGroup service.
                    */
                                        var havingVpcVirtualRouterForAtLeastOneService = false;
                                        $(serviceCheckboxNames).each(function() {
                                            var checkboxName = this;
                                            if ($("input[name='" + checkboxName + "']").is(":checked") == true) {
                                                var providerFieldName = checkboxName.replace(".isEnabled", ".provider"); //either dropdown or input hidden field
                                                var providerName = $("[name='" + providerFieldName + "']").val();
                                                if (providerName == "VpcVirtualRouter") {
                                                    havingVpcVirtualRouterForAtLeastOneService = true;
                                                    return false; //break each loop
                                                }
                                            }
                                        });
                                        if (havingVpcVirtualRouterForAtLeastOneService == true) {
                                            $conservemode.find("input[type=checkbox]").attr("disabled", "disabled");
                                            $conservemode.find("input[type=checkbox]").attr('checked', false);

                                            $serviceSourceNatRedundantRouterCapabilityCheckbox.find("input[type=checkbox]").attr("disabled", "disabled");
                                            $serviceSourceNatRedundantRouterCapabilityCheckbox.find("input[type=checkbox]").attr('checked', false);
                                        } else {
                                            $serviceSourceNatRedundantRouterCapabilityCheckbox.find("input[type=checkbox]").removeAttr("disabled");
                                            $conservemode.find("input[type=checkbox]").removeAttr("disabled");
                                        }

                                        $(':ui-dialog').dialog('option', 'position', 'center');

                                        //CS-16612 show all services regardless of guestIpType(Shared/Isolated)
                                        /*
                                        //hide/show service fields ***** (begin) *****
                                        var serviceFieldsToHide = [];
                                        if($guestTypeField.val() == 'Shared') { //Shared network offering
                                          serviceFieldsToHide = [
                                                'service.SourceNat.isEnabled',
                                                'service.PortForwarding.isEnabled',
                                                'service.Firewall.isEnabled',
                                                'service.Vpn.isEnabled'
                                            ];
                                            if(havingVpcVirtualRouterForAtLeastOneService == true) { //add SecurityGroup to to-hide-list
                                              serviceFieldsToHide.push('service.SecurityGroup.isEnabled');
                                            }
                                            else { //remove SecurityGroup from to-hide-list
                                              var temp = $.map(serviceFieldsToHide, function(item) {
                                                    if (item != 'service.SecurityGroup.isEnabled') {
                                                      return item;
                                                    }
                                                });
                                                serviceFieldsToHide = temp;
                                            }
                                        }
                                        else { //Isolated network offering
                                          serviceFieldsToHide = [
                                              'service.SecurityGroup.isEnabled'
                                            ];
                                            if(havingVpcVirtualRouterForAtLeastOneService == true) { //add firewall to to-hide-list
                                              serviceFieldsToHide.push('service.Firewall.isEnabled');
                                            }
                                            else { //remove firewall from to-hide-list
                                              var temp = $.map(serviceFieldsToHide, function(item) {
                                                    if (item != 'service.Firewall.isEnabled') {
                                                      return item;
                                                    }
                                                });
                                                serviceFieldsToHide = temp;
                                            }
                                        }
                    */


                                        //CS-16687: NetworkACL should be removed when the guest_type is SHARED
                                        //hide/show service fields ***** (begin) *****
                                        var serviceFieldsToHide = [];
                                        if ($guestTypeField.val() == 'Shared') { //Shared network offering
                                            serviceFieldsToHide = [
                                                'service.NetworkACL.isEnabled'
                                            ];
                                        } else { //Isolated network offering
                                            serviceFieldsToHide = [];
                                        }

                                        //hide service fields that are included in serviceFieldsToHide
                                        var $serviceCheckboxesToHide = args.$form.find('.form-item').filter(function() {
                                            if ($.inArray($(this).attr('rel'), serviceFieldsToHide) > -1) {
                                                return true;
                                            }
                                            return false;
                                        });
                                        $serviceCheckboxesToHide.hide();
                                        $serviceCheckboxesToHide.find('input[type=checkbox]').attr('checked', false);

                                        var $serviceProviderDropdownsToHide = args.$form.find('.form-item').filter(function() {
                                            if ($.inArray($(this).attr('depends-on'), serviceFieldsToHide) > -1) {
                                                return true;
                                            }
                                            return false;
                                        });
                                        $serviceProviderDropdownsToHide.hide();

                                        //show service fields that are not included in serviceFieldsToHide
                                        for (var i = 0; i < serviceFields.length; i++) {
                                            var serviceField = serviceFields[i];
                                            if ($.inArray(serviceField, serviceFieldsToHide) == -1) {
                                                if (args.$form.find('.form-item[rel=\"' + serviceField + '\"]').css('display') == 'none') {
                                                    args.$form.find('.form-item[rel=\"' + serviceField + '\"]').css('display', 'inline-block');
                                                }
                                            }
                                        }
                                        //hide/show service fields ***** (end) *****

                                        //show LB InlineMode dropdown only when (1)LB service is checked and LB service provider is F5BigIp (2)Firewall service is checked and Firewall service provider is JuniperSRX
                                        if ((args.$form.find('.form-item[rel=\"service.Lb.isEnabled\"]').find('input[type=checkbox]').is(':checked') == true) && (args.$form.find('.form-item[rel=\"service.Lb.provider\"]').find('select').val() == 'F5BigIp') &&
                                            (args.$form.find('.form-item[rel=\"service.Firewall.isEnabled\"]').find('input[type=checkbox]').is(':checked') == true) && (args.$form.find('.form-item[rel=\"service.Firewall.provider\"]').find('select').val() == 'JuniperSRX')) {
                                            args.$form.find('.form-item[rel=\"service.Lb.inlineModeDropdown\"]').css('display', 'inline-block');
                                        } else {
                                            args.$form.find('.form-item[rel=\"service.Lb.inlineModeDropdown\"]').hide();
                                        }

                                        if (args.$form.find('.form-item[rel=\"service.Firewall.isEnabled\"] input[type=checkbox]').is(':checked') == true) {
                                            args.$form.find('.form-item[rel=\"egressdefaultpolicy\"]').css('display', 'inline-block');
                                        } else {
                                            args.$form.find('.form-item[rel=\"egressdefaultpolicy\"]').css('display', 'none');
                                        }

                                        //show LB Isolation dropdown only when (1)LB Service is checked (2)Service Provider is Netscaler OR F5
                                        if ((args.$form.find('.form-item[rel=\"service.Lb.isEnabled\"]').find('input[type=checkbox]').is(':checked') == true) && (args.$form.find('.form-item[rel=\"service.Lb.provider\"]').find('select').val() == 'Netscaler' || args.$form.find('.form-item[rel=\"service.Lb.provider\"]').find('select').val() == 'F5BigIp')) {
                                            args.$form.find('.form-item[rel=\"service.Lb.lbIsolationDropdown\"]').css('display', 'inline-block');
                                        } else {
                                            args.$form.find('.form-item[rel=\"service.Lb.lbIsolationDropdown\"]').hide();
                                        }

                                        //show Elastic LB checkbox only when (1)LB Service is checked (2)Service Provider is Netscaler (3)Guest IP Type is Shared
                                        if ((args.$form.find('.form-item[rel=\"service.Lb.isEnabled\"]').find('input[type=checkbox]').is(':checked') == true) && (args.$form.find('.form-item[rel=\"service.Lb.provider\"]').find('select').val() == 'Netscaler') && (args.$form.find('.form-item[rel=\"guestIpType\"]').find('select').val() == 'Shared')) {
                                            args.$form.find('.form-item[rel=\"service.Lb.elasticLbCheckbox\"]').css('display', 'inline-block');
                                        } else {
                                            args.$form.find('.form-item[rel=\"service.Lb.elasticLbCheckbox\"]').hide();
                                            args.$form.find('.form-item[rel=\"service.Lb.elasticLbCheckbox\"]').find('input[type=checkbox]').attr('checked', false);
                                        }

                                        //show Elastic IP checkbox only when (1)StaticNat service is checked (2)StaticNat service provider is Netscaler
                                        if ((args.$form.find('.form-item[rel=\"service.StaticNat.isEnabled\"]').find('input[type=checkbox]').is(':checked') == true) && (args.$form.find('.form-item[rel=\"service.StaticNat.provider\"]').find('select').val() == 'Netscaler')) {
                                            args.$form.find('.form-item[rel=\"service.StaticNat.elasticIpCheckbox\"]').css('display', 'inline-block');
                                        } else {
                                            args.$form.find('.form-item[rel=\"service.StaticNat.elasticIpCheckbox\"]').hide();
                                            args.$form.find('.form-item[rel=\"service.StaticNat.elasticIpCheckbox\"]').find('input[type=checkbox]').attr('checked', false);
                                        }

                                        //show Associate Public IP checkbox only when (1)StaticNat Service is checked (2)Service Provider is Netscaler (3)Guest IP Type is Shared (4) Elastic IP checkbox is checked
                                        if ((args.$form.find('.form-item[rel=\"service.StaticNat.isEnabled\"]').find('input[type=checkbox]').is(':checked') == true) && (args.$form.find('.form-item[rel=\"service.StaticNat.provider\"]').find('select').val() == 'Netscaler') && (args.$form.find('.form-item[rel=\"guestIpType\"]').find('select').val() == 'Shared') && (args.$form.find('.form-item[rel=\"service.StaticNat.elasticIpCheckbox\"]').find('input[type=checkbox]').attr('checked') == "checked")) {
                                            args.$form.find('.form-item[rel=\"service.StaticNat.associatePublicIP\"]').css('display', 'inline-block');
                                        } else {
                                            args.$form.find('.form-item[rel=\"service.StaticNat.associatePublicIP\"]').hide();
                                            args.$form.find('.form-item[rel=\"service.StaticNat.associatePublicIP\"]').find('input[type=checkbox]').attr('checked', false);
                                        }

                                        //StretchedL2Subnet checkbox should be displayed only when 'Connectivity' service is checked
                                        if (args.$form.find('.form-item[rel=\"service.Connectivity.isEnabled\"]').find('input[type=checkbox]').is(':checked')) {
                                            $supportsstrechedl2subnet.css('display', 'inline-block');
                                        } else {
                                            $supportsstrechedl2subnet.hide();
                                        }
                                    });

                                    args.$form.change();
                                },
                                fields: {
                                    name: {
                                        label: 'label.name',
                                        validation: {
                                            required: true
                                        },
                                        docID: 'helpNetworkOfferingName'
                                    },

                                    displayText: {
                                        label: 'label.description',
                                        validation: {
                                            required: true
                                        },
                                        docID: 'helpNetworkOfferingDescription'
                                    },

                                    networkRate: {
                                        label: 'label.network.rate',
                                        docID: 'helpNetworkOfferingNetworkRate'
                                    },

                                    /*
                  trafficType: {
                    label: 'label.traffic.type', validation: { required: true },
                    select: function(args) {
                      args.response.success({
                        data: [
                          { id: 'GUEST', description: 'Guest' }
                        ]
                      });
                    }
                  },
                                    */

                                    guestIpType: {
                                        label: 'label.guest.type',
                                        docID: 'helpNetworkOfferingGuestType',
                                        select: function(args) {
                                            args.response.success({
                                                data: [{
                                                    id: 'Isolated',
                                                    description: 'Isolated'
                                                }, {
                                                    id: 'Shared',
                                                    description: 'Shared'
                                                }]
                                            });

                                            args.$select.change(function() {
                                                var $form = $(this).closest("form");

                                                if ($(this).val() == "Shared") {
                                                    $form.find('.form-item[rel=specifyVlan]').find('input[type=checkbox]').attr("disabled", "disabled"); //make it read-only
                                                    $form.find('.form-item[rel=specifyVlan]').find('input[type=checkbox]').attr('checked', true); //make it checked
                                                    $form.find('.form-item[rel=isPersistent]').find('input[type=checkbox]').attr("disabled", "disabled");


                                                } else { //$(this).val() == "Isolated"
                                                    $form.find('.form-item[rel=specifyVlan]').find('input[type=checkbox]').removeAttr("disabled"); //make it editable
                                                    $form.find('.form-item[rel=isPersistent]').find('input[type=checkbox]').removeAttr("disabled");

                                                }
                                            });
                                        }
                                    },

                                    isPersistent: {
                                        label: 'label.persistent',
                                        isBoolean: true,
                                        isChecked: false

                                    },


                                    specifyVlan: {
                                        label: 'label.specify.vlan',
                                        isBoolean: true,
                                        docID: 'helpNetworkOfferingSpecifyVLAN'
                                    },

                                    useVpc: {
                                        label: 'label.vpc',
                                        docID: 'helpNetworkOfferingVPC',
                                        isBoolean: true
                                    },

                                    lbType: { //only shown when VPC is checked and LB service is checked
                                        label: 'label.load.balancer.type',
                                        isHidden: true,
                                        select: function(args) {
                                            args.response.success({
                                                data: [{
                                                    id: 'publicLb',
                                                    description: 'Public LB'
                                                }, {
                                                    id: 'internalLb',
                                                    description: 'Internal LB'
                                                }]
                                            });
                                        }
                                    },

                                    supportedServices: {
                                        label: 'label.supported.services',

                                        dynamic: function(args) {
                                            $.ajax({
                                                url: createURL('listSupportedNetworkServices'),
                                                dataType: 'json',
                                                async: true,
                                                success: function(data) {
                                                    networkServiceObjs = data.listsupportednetworkservicesresponse.networkservice;
                                                    serviceFields = [];
                                                    var fields = {}, providerCanenableindividualserviceMap = {}, providerServicesMap = {}, providerDropdownsForciblyChangedTogether = {};
                                                    $(networkServiceObjs).each(function() {
                                                        var serviceName = this.name;
                                                        var providerObjs = this.provider;
                                                        var serviceDisplayName;

                                                        // Sanitize names
                                                        switch (serviceName) {
                                                            case 'Vpn':
                                                                serviceDisplayName = _l('label.vpn');
                                                                break;
                                                            case 'Dhcp':
                                                                serviceDisplayName = _l('label.dhcp');
                                                                break;
                                                            case 'Dns':
                                                                serviceDisplayName = _l('label.dns');
                                                                break;
                                                            case 'Lb':
                                                                serviceDisplayName = _l('label.load.balancer');
                                                                break;
                                                            case 'SourceNat':
                                                                serviceDisplayName = _l('label.source.nat');
                                                                break;
                                                            case 'StaticNat':
                                                                serviceDisplayName = _l('label.static.nat');
                                                                break;
                                                            case 'PortForwarding':
                                                                serviceDisplayName = _l('label.port.forwarding');
                                                                break;
                                                            case 'SecurityGroup':
                                                                serviceDisplayName = _l('label.security.groups');
                                                                break;
                                                            case 'UserData':
                                                                serviceDisplayName = _l('label.user.data');
                                                                break;
                                                            case 'Connectivity':
                                                                serviceDisplayName = _l('label.virtual.networking');
                                                                break;
                                                            default:
                                                                serviceDisplayName = serviceName;
                                                                break;
                                                        }

                                                        var id = {
                                                            isEnabled: 'service' + '.' + serviceName + '.' + 'isEnabled',
                                                            capabilities: 'service' + '.' + serviceName + '.' + 'capabilities',
                                                            provider: 'service' + '.' + serviceName + '.' + 'provider'
                                                        };

                                                        serviceCheckboxNames.push(id.isEnabled);

                                                        fields[id.isEnabled] = {
                                                            label: serviceDisplayName,
                                                            isBoolean: true
                                                        };
                                                        serviceFields.push(id.isEnabled);

                                                        if (providerObjs != null && providerObjs.length > 1) { //present provider dropdown when there are multiple providers for a service
                                                            fields[id.provider] = {
                                                                label: serviceDisplayName + ' Provider',
                                                                isHidden: true,
                                                                dependsOn: id.isEnabled,
                                                                select: function(args) {
                                                                    //Virtual Router needs to be the first choice in provider dropdown (Bug 12509)
                                                                    var items = [];
                                                                    $(providerObjs).each(function() {
                                                                        if (this.name == "VirtualRouter")
                                                                            items.unshift({
                                                                                id: this.name,
                                                                                description: this.name
                                                                            });
                                                                        else
                                                                            items.push({
                                                                                id: this.name,
                                                                                description: this.name
                                                                            });

                                                                        if (!(this.name in providerCanenableindividualserviceMap))
                                                                            providerCanenableindividualserviceMap[this.name] = this.canenableindividualservice;

                                                                        if (!(this.name in providerServicesMap))
                                                                            providerServicesMap[this.name] = [serviceName];
                                                                        else
                                                                            providerServicesMap[this.name].push(serviceName);
                                                                    });

                                                                    args.response.success({
                                                                        data: items
                                                                    });

                                                                    // Disable VPC virtual router by default
                                                                    args.$select.find('option[value=VpcVirtualRouter]').attr('disabled', true);

                                                                    args.$select.change(function() {
                                                                        var $thisProviderDropdown = $(this);
                                                                        var providerName = $(this).val();
                                                                        var canenableindividualservice = providerCanenableindividualserviceMap[providerName];
                                                                        if (canenableindividualservice == false) { //This provider can NOT enable individual service, therefore, force all services supported by this provider have this provider selected in provider dropdown
                                                                            var serviceNames = providerServicesMap[providerName];
                                                                            if (serviceNames != null && serviceNames.length > 1) {
                                                                                providerDropdownsForciblyChangedTogether = {}; //reset
                                                                                $(serviceNames).each(function() {
                                                                                    var providerDropdownId = 'service' + '.' + this + '.' + 'provider';
                                                                                    providerDropdownsForciblyChangedTogether[providerDropdownId] = 1;
                                                                                    $("select[name='" + providerDropdownId + "']").val(providerName);
                                                                                });
                                                                            }
                                                                        } else { //canenableindividualservice == true
                                                                            if ($thisProviderDropdown.context.name in providerDropdownsForciblyChangedTogether) { //if this provider dropdown is one of provider dropdowns forcibly changed together earlier, make other forcibly changed provider dropdowns restore default option (i.e. 1st option in dropdown)
                                                                                for (var key in providerDropdownsForciblyChangedTogether) {
                                                                                    if (key == $thisProviderDropdown.context.name)
                                                                                        continue; //skip to next item in for loop
                                                                                    else
                                                                                        $("select[name='" + key + "'] option:first").attr("selected", "selected");
                                                                                }
                                                                                providerDropdownsForciblyChangedTogether = {}; //reset
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            };
                                                        } else if (providerObjs != null && providerObjs.length == 1) { //present hidden field when there is only one provider for a service
                                                            fields[id.provider] = {
                                                                label: serviceDisplayName + ' Provider',
                                                                isHidden: true,
                                                                defaultValue: providerObjs[0].name
                                                            };
                                                        }
                                                    });

                                                    args.response.success({
                                                        fields: fields
                                                    });
                                                },
                                                error: function(data) {
                                                    args.response.error(parseXMLHttpResponse(data));
                                                }
                                            });
                                        }
                                    },

                                    //show or hide upon checked services and selected providers above (begin)
                                    serviceofferingid: {
                                        label: 'label.system.offering.for.router',
                                        isHidden: true,
                                        docID: 'helpNetworkOfferingSystemOffering',
                                        select: function(args) {
                                            $.ajax({
                                                url: createURL('listServiceOfferings&issystem=true&systemvmtype=domainrouter'),
                                                dataType: 'json',
                                                async: true,
                                                success: function(data) {
                                                    var serviceOfferings = data.listserviceofferingsresponse.serviceoffering;

                                                    args.response.success({
                                                        data: $.merge(
                                                            [{
                                                                id: null,
                                                                description: 'None'
                                                            }],
                                                            $.map(serviceOfferings, function(elem) {
                                                                return {
                                                                    id: elem.id,
                                                                    description: elem.name
                                                                };
                                                            })
                                                        )
                                                    });
                                                },
                                                error: function(data) {
                                                    args.response.error(parseXMLHttpResponse(data));
                                                }
                                            });
                                        }
                                    },

                                    "service.SourceNat.redundantRouterCapabilityCheckbox": {
                                        label: 'label.redundant.router.capability',
                                        isHidden: true,
                                        dependsOn: 'service.SourceNat.isEnabled',
                                        docID: 'helpNetworkOfferingRedundantRouterCapability',
                                        isBoolean: true
                                    },

                                    "service.SourceNat.sourceNatTypeDropdown": {
                                        label: 'label.supported.source.NAT.type',
                                        isHidden: true,
                                        dependsOn: 'service.SourceNat.isEnabled',
                                        select: function(args) {
                                            args.response.success({
                                                data: [{
                                                    id: 'peraccount',
                                                    description: 'Per account'
                                                }, {
                                                    id: 'perzone',
                                                    description: 'Per zone'
                                                }]
                                            });
                                        }
                                    },

                                    "service.Lb.elasticLbCheckbox": {
                                        label: "label.elastic.LB",
                                        isHidden: true,
                                        isBoolean: true
                                    },
                                    "service.Lb.lbIsolationDropdown": {
                                        label: 'label.LB.isolation',
                                        docID: 'helpNetworkOfferingLBIsolation',
                                        isHidden: true,
                                        select: function(args) {
                                            args.response.success({
                                                data: [{
                                                    id: 'dedicated',
                                                    description: 'Dedicated'
                                                }, {
                                                    id: 'shared',
                                                    description: 'Shared'
                                                }]
                                            })
                                        }
                                    },
                                    "service.Lb.inlineModeDropdown": {
                                        label: 'label.mode',
                                        docID: 'helpNetworkOfferingMode',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: "false",
                                                description: "side by side"
                                            });
                                            items.push({
                                                id: "true",
                                                description: "inline"
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },

                                    "service.StaticNat.elasticIpCheckbox": {
                                        label: "label.elastic.IP",
                                        isHidden: true,
                                        isBoolean: true
                                    },

                                    "service.StaticNat.associatePublicIP": {
                                        label: 'label.associate.public.ip',
                                        docID: 'helpNetworkOfferingAssociatePublicIP',
                                        isBoolean: true,
                                        isHidden: true
                                    },
                                    //show or hide upon checked services and selected providers above (end)

                                    supportsstrechedl2subnet: {
                                        label: 'label.supportsstrechedl2subnet',
                                        isBoolean: true,
                                        isChecked: false,
                                        isHidden: true
                                    },

                                    conservemode: {
                                        label: 'label.conserve.mode',
                                        isBoolean: true,
                                        isChecked: true,
                                        docID: 'helpNetworkOfferingConserveMode'
                                    },

                                    tags: {
                                        label: 'label.tags',
                                        docID: 'helpNetworkOfferingTags'
                                    },

                                    availability: {
                                        label: 'label.availability',
                                        isHidden: true,
                                        select: function(args) {
                                            args.response.success({
                                                data: [{
                                                    id: 'Optional',
                                                    description: 'label.optional'
                                                }, {
                                                    id: 'Required',
                                                    description: 'label.required'
                                                }]
                                            });
                                        }
                                    },

                                    egressdefaultpolicy: {
                                        label: 'label.default.egress.policy',
                                        isHidden: true,
                                        select: function(args) {
                                            args.response.success({
                                                data: [
                                                    { id: 'ALLOW', description: 'label.allow' },
                                                    { id: 'DENY', description: 'label.deny' }
                                                ]
                                            });
                                        }
                                    }
                                }
                            },

                            action: function(args) {
                                var formData = args.data;
                                var inputData = {};
                                var serviceProviderMap = {};
                                var serviceCapabilityIndex = 0;

                                $.each(formData, function(key, value) {
                                    var serviceData = key.split('.');

                                    if (serviceData.length > 1) {
                                        if (serviceData[0] == 'service' &&
                                            serviceData[2] == 'isEnabled' &&
                                            value == 'on') { // Services field

                                            serviceProviderMap[serviceData[1]] = formData[
                                                'service.' + serviceData[1] + '.provider'
                                            ];
                                        } else if ((key == 'service.SourceNat.redundantRouterCapabilityCheckbox') && ("SourceNat" in serviceProviderMap)) { //if checkbox is unchecked, it won't be included in formData in the first place. i.e. it won't fall into this section
                                            inputData['serviceCapabilityList[' + serviceCapabilityIndex + '].service'] = 'SourceNat';
                                            inputData['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilitytype'] = "RedundantRouter";
                                            inputData['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilityvalue'] = true; //because this checkbox's value == "on"
                                            serviceCapabilityIndex++;
                                        } else if ((key == 'service.SourceNat.sourceNatTypeDropdown') && ("SourceNat" in serviceProviderMap)) {
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'SourceNat';
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'SupportedSourceNatTypes';
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = value;
                                            serviceCapabilityIndex++;
                                        } else if ((key == 'service.Lb.elasticLbCheckbox') && ("Lb" in serviceProviderMap)) { //if checkbox is unchecked, it won't be included in formData in the first place. i.e. it won't fall into this section
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'lb';
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'ElasticLb';
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = true; //because this checkbox's value == "on"
                                            serviceCapabilityIndex++;
                                        } else if ((key == 'service.Lb.inlineModeDropdown') && ("Lb" in serviceProviderMap) && (serviceProviderMap.Lb == "F5BigIp")) {
                                            if (value == 'true') { //CS-16605 do not pass parameter if value is 'false'(side by side)
                                                inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'lb';
                                                inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'InlineMode';
                                                inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = value;
                                                serviceCapabilityIndex++;
                                            }
                                        } else if ((key == 'service.Lb.lbIsolationDropdown') && ("Lb" in serviceProviderMap)) {
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'lb';
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'SupportedLbIsolation';
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = value;
                                            serviceCapabilityIndex++;
                                        } else if ((key == 'service.StaticNat.elasticIpCheckbox') && ("StaticNat" in serviceProviderMap)) { //if checkbox is unchecked, it won't be included in formData in the first place. i.e. it won't fall into this section
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'StaticNat';
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'ElasticIp';
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = true; //because this checkbox's value == "on"
                                            serviceCapabilityIndex++;
                                        } else if ((key == 'service.StaticNat.associatePublicIP') && ("StaticNat" in serviceProviderMap)) { //if checkbox is unchecked, it won't be included in formData in the first place. i.e. it won't fall into this section
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'StaticNat';
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'associatePublicIP';
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = true; //because this checkbox's value == "on"
                                            serviceCapabilityIndex++;
                                        } else if ((key == 'service.Lb.provider') && ("Lb" in serviceProviderMap) && (serviceProviderMap.Lb == "InternalLbVm")) {
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'lb';
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'lbSchemes';
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = 'internal';
                                            serviceCapabilityIndex++;
                                        }
                                    } else if (value != '') { // normal data (serviceData.length ==1), e.g. "name", "displayText", "networkRate", "guestIpType", "lbType" (unwanted), "availability" (unwated when value is "Optional"), "egressdefaultpolicy", "state" (unwanted), "status" (unwanted), "allocationstate" (unwanted)
                                        if (!(key ==  "lbType"  || (key == "availability" && value == "Optional") || key == "state" || key == "status" || key == "allocationstate" || key == "useVpc" )) {
                                        inputData[key] = value;
                                    }
                                    }
                                });

                                for (var key1 in inputData) {
                                    /* When capability ElasticIp=true is passed to API, if capability associatePublicIP is not passed to API, cloudStack API will assume associatePublicIP=true.
                                    So, UI has to explicitly pass associatePublicIP=false to API if its checkbox is unchecked. */
                                    if (inputData[key1] == 'ElasticIp') { //ElasticIp checkbox is checked
                                        var associatePublicIPExists = false;
                                        for (var key2 in inputData) {
                                            if (inputData[key2] == 'associatePublicIP') {
                                                associatePublicIPExists = true;
                                                break; //break key2 for loop
                                            }
                                        }
                                        if (associatePublicIPExists == false) { //but associatePublicIP checkbox is unchecked
                                            //UI explicitly passes associatePublicIP=false to API
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'StaticNat';
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'associatePublicIP';
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = false; //associatePublicIP checkbox is unchecked
                                            serviceCapabilityIndex++;
                                        }
                                        break; //break key1 for loop
                                    }
                                }

                                //passing supportsstrechedl2subnet's value as capability
                                for (var k in inputData) {
                                    if (k == 'supportsstrechedl2subnet' && ("Connectivity" in serviceProviderMap)) {
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'Connectivity';
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'StretchedL2Subnet';
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = true;
                                            serviceCapabilityIndex++;
                                            break;
                                    }
                                }
                                //removing supportsstrechedl2subnet from parameters, it has been set as capability
                                delete inputData['supportsstrechedl2subnet'];

                                // Make supported services list
                                inputData['supportedServices'] = $.map(serviceProviderMap, function(value, key) {
                                    return key;
                                }).join(',');


                                if (inputData['guestIpType'] == "Shared") { //specifyVlan checkbox is disabled, so inputData won't include specifyVlan
                                    inputData['specifyVlan'] = true; //hardcode inputData['specifyVlan']
                                    inputData['specifyIpRanges'] = true;
                                    delete inputData.isPersistent; //if Persistent checkbox is unchecked, do not pass isPersistent parameter to API call since we need to keep API call's size as small as possible (p.s. isPersistent is defaulted as false at server-side)
                                } else if (inputData['guestIpType'] == "Isolated") { //specifyVlan checkbox is shown
                                    //inputData['specifyIpRanges'] = false;
                                    delete inputData.specifyIpRanges; //if specifyIpRanges should be false, do not pass specifyIpRanges parameter to API call since we need to keep API call's size as small as possible (p.s. specifyIpRanges is defaulted as false at server-side)

                                    if (inputData['specifyVlan'] == 'on') { //specifyVlan checkbox is checked
                                        inputData['specifyVlan'] = true;
                                    } else { //specifyVlan checkbox is unchecked
                                        delete inputData.specifyVlan; //if specifyVlan checkbox is unchecked, do not pass specifyVlan parameter to API call since we need to keep API call's size as small as possible (p.s. specifyVlan is defaulted as false at server-side)
                                    }

                                    if (inputData['isPersistent'] == 'on') { //It is a persistent network
                                        inputData['isPersistent'] = true;
                                    } else { //Isolated Network with Non-persistent network
                                        delete inputData.isPersistent; //if Persistent checkbox is unchecked, do not pass isPersistent parameter to API call since we need to keep API call's size as small as possible (p.s. isPersistent is defaulted as false at server-side)
                                    }
                                }


                                if (inputData['conservemode'] == 'on') {
                                    delete inputData.conservemode; //if ConserveMode checkbox is checked, do not pass conservemode parameter to API call since we need to keep API call's size as small as possible (p.s. conservemode is defaulted as true at server-side)
                                } else {
                                    inputData['conservemode'] = false;
                                }

                                // Make service provider map
                                var serviceProviderIndex = 0;
                                $.each(serviceProviderMap, function(key, value) {
                                    inputData['serviceProviderList[' + serviceProviderIndex + '].service'] = key;
                                    inputData['serviceProviderList[' + serviceProviderIndex + '].provider'] = value;
                                    serviceProviderIndex++;
                                });

                                if (args.$form.find('.form-item[rel=egressdefaultpolicy]').is(':visible')) {
                                    if (formData.egressdefaultpolicy === 'ALLOW') {
                                        delete inputData.egressdefaultpolicy; //do not pass egressdefaultpolicy to API call  since we need to keep API call's size as small as possible (p.s. egressdefaultpolicy is defaulted as true at server-side)
                                    } else {
                                        inputData['egressdefaultpolicy'] = false;
                                    }
                                } else {
                                    delete inputData.egressdefaultpolicy;
                                }

                                if (args.$form.find('.form-item[rel=serviceofferingid]').css("display") == "none")
                                    delete inputData.serviceofferingid;

                                inputData['traffictype'] = 'GUEST'; //traffic type dropdown has been removed since it has only one option ('Guest'). Hardcode traffic type value here.

                                $.ajax({
                                    url: createURL('createNetworkOffering'),
                                    data: inputData,
                                    type: "POST", //use POST instead of GET since the API call might be overlong and exceed size limit
                                    success: function(data) {
                                        var item = data.createnetworkofferingresponse.networkoffering;

                                        if (inputData['availability'] == "Required")
                                            requiredNetworkOfferingExists = true;

                                        args.response.success({
                                            data: item,
                                            actionFilter: networkOfferingActionfilter
                                        });
                                    },

                                    error: function(data) {
                                        args.response.error(parseXMLHttpResponse(data));
                                    }
                                });
                            },

                            notification: {
                                poll: function(args) {
                                    args.complete({
                                        actionFilter: networkOfferingActionfilter
                                    });
                                }
                            },

                            messages: {
                                notification: function(args) {
                                    return 'label.add.network.offering';
                                }
                            }
                        }
                    },

                    reorder: cloudStack.api.actions.sort('updateNetworkOffering', 'networkOfferings'),

                    detailView: {
                        name: 'label.network.offering.details',
                        actions: {
                            edit: {
                                label: 'label.edit',
                                action: function(args) {
                                    var data = {
                                        id: args.context.networkOfferings[0].id,
                                        name: args.data.name,
                                        displaytext: args.data.displaytext,
                                        availability: args.data.availability
                                    };

                                    $.ajax({
                                        url: createURL('updateNetworkOffering'),
                                        data: data,
                                        success: function(json) {
                                            //if availability is being updated from Required to Optional
                                            if (args.context.networkOfferings[0].availability == "Required" && args.data.availability == "Optional")
                                                requiredNetworkOfferingExists = false;
                                            //if availability is being updated from Optional to Required
                                            if (args.context.networkOfferings[0].availability == "Optional" && args.data.availability == "Required")
                                                requiredNetworkOfferingExists = true;

                                            var item = json.updatenetworkofferingresponse.networkoffering;
                                            args.response.success({
                                                data: item
                                            });
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                }
                            },

                            enable: {
                                label: 'label.enable.network.offering',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.confirm.enable.network.offering';
                                    },
                                    notification: function(args) {
                                        return 'message.enabling.network.offering';
                                    }
                                },
                                action: function(args) {
                                    $.ajax({
                                        url: createURL("updateNetworkOffering&id=" + args.context.networkOfferings[0].id + "&state=Enabled"),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var item = json.updatenetworkofferingresponse.networkoffering;
                                            args.response.success();
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                },
                                notification: {
                                    poll: function(args) {
                                        args.complete({
                                            data: {
                                                state: 'Enabled'
                                            }
                                        });
                                    }
                                }
                            },

                            disable: {
                                label: 'label.disable.network.offering',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.confirm.disable.network.offering';
                                    },
                                    notification: function(args) {
                                        return 'message.disabling.network.offering';
                                    }
                                },
                                action: function(args) {
                                    $.ajax({
                                        url: createURL("updateNetworkOffering&id=" + args.context.networkOfferings[0].id + "&state=Disabled"),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var item = json.updatenetworkofferingresponse.networkoffering;
                                            args.response.success();
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                },
                                notification: {
                                    poll: function(args) {
                                        args.complete({
                                            data: {
                                                state: 'Disabled'
                                            }
                                        });
                                    }
                                }
                            },

                            remove: {
                                label: 'label.remove.network.offering',
                                action: function(args) {
                                    $.ajax({
                                        url: createURL('deleteNetworkOffering'),
                                        data: {
                                            id: args.context.networkOfferings[0].id
                                        },
                                        success: function(json) {
                                            if (args.context.networkOfferings[0].availability == "Required")
                                                requiredNetworkOfferingExists = false; //since only one or zero Required network offering can exist

                                            args.response.success();
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                },
                                messages: {
                                    confirm: function() {
                                        return 'message.confirm.remove.network.offering';
                                    },
                                    notification: function() {
                                        return 'label.remove.network.offering';
                                    }
                                },
                                notification: {
                                    poll: function(args) {
                                        args.complete({
                                            data: {
                                                state: 'Destroyed'
                                            },
                                            actionFilter: networkOfferingActionfilter
                                        });
                                    }
                                }
                            }
                        },
                        tabs: {
                            details: {
                                title: 'label.details',

                                fields: [{
                                    name: {
                                        label: 'label.name',
                                        isEditable: true,
                                        validation: {
                                            required: true
                                        }
                                    }
                                }, {
                                    id: {
                                        label: 'label.id'
                                    },
                                    displaytext: {
                                        label: 'label.description',
                                        isEditable: true,
                                        validation: {
                                            required: true
                                        }
                                    },
                                    state: {
                                        label: 'label.state'
                                    },
                                    guestiptype: {
                                        label: 'label.guest.type'
                                    },

                                    ispersistent: {
                                        label: 'label.persistent',
                                        converter: cloudStack.converters.toBooleanText
                                    },

                                    egressdefaultpolicy: {
                                        label: 'label.egress.default.policy',
                                        converter: function(str) {
                                            return str === true ? 'Allow' : 'Deny';
                                        }
                                    },

                                    availability: {
                                        label: 'label.availability',
                                        isEditable: true,
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: 'Required',
                                                description: 'Required'
                                            });
                                            items.push({
                                                id: 'Optional',
                                                description: 'Optional'
                                            });
                                            //items.push({id: 'Unavailable', description: 'Unavailable'});
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    isdefault: { //created by system by default
                                        label: 'label.created.by.system',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    specifyvlan: {
                                        label: 'label.specify.vlan',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    specifyipranges: {
                                        label: 'label.specify.IP.ranges',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    conservemode: {
                                        label: 'label.conserve.mode',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    networkrate: {
                                        label: 'label.network.rate',
                                        converter: function(args) {
                                            var networkRate = args;
                                            if (args == null || args == -1) {
                                                return "Unlimited";
                                            } else {
                                                return _s(args) + " Mb/s";

                                            }
                                        }
                                    },
                                    traffictype: {
                                        label: 'label.traffic.type'
                                    },
                                    supportsstrechedl2subnet: {
                                        label: 'label.supportsstrechedl2subnet',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    supportedServices: {
                                        label: 'label.supported.services'
                                    },
                                    serviceCapabilities: {
                                        label: 'label.service.capabilities'
                                    },
                                    tags: {
                                        label: 'label.tags'
                                    }
                                }],

                                dataProvider: function(args) {
                                    $.ajax({
                                        url: createURL('listNetworkOfferings&id=' + args.context.networkOfferings[0].id),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var item = json.listnetworkofferingsresponse.networkoffering[0];
                                            args.response.success({
                                                actionFilter: networkOfferingActionfilter,
                                                data: $.extend(item, {
                                                    supportedServices: $.map(item.service, function(service) {
                                                        return service.name;
                                                    }).join(', '),

                                                    serviceCapabilities: $.map(item.service, function(service) {
                                                        return service.provider ? $.map(service.provider, function(capability) {
                                                            return service.name + ': ' + capability.name;
                                                        }).join(', ') : null;
                                                    }).join(', ')
                                                })
                                            });
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
            },

            vpcOfferings: {
                type: 'select',
                title: 'label.menu.vpc.offerings',
                listView: {
                    id: 'vpcOfferings',
                    label: 'label.menu.vpc.offerings',
                    fields: {
                        name: {
                            label: 'label.name'
                        },
                        state: {
                            label: 'label.state',
                            indicator: {
                                'Enabled': 'on',
                                'Disabled': 'off',
                                'Destroyed': 'off'
                            }
                        }
                    },

                    dataProvider: function(args) {
                        var data = {};
                        listViewDataProvider(args, data);

                        $.ajax({
                            url: createURL('listVPCOfferings'),
                            data: data,
                            success: function(json) {
                                var items = json.listvpcofferingsresponse.vpcoffering;
                                args.response.success({
                                    actionFilter: vpcOfferingActionfilter,
                                    data: items
                                });
                            },
                            error: function(data) {
                                args.response.error(parseXMLHttpResponse(data));
                            }
                        });
                    },

                    actions: {
                        add: {
                            label: 'label.add.vpc.offering',

                            createForm: {
                                title: 'label.add.vpc.offering',

                                preFilter: function(args) {
                                    var hasAdvancedZones = false;

                                    // Check whether there are any advanced zones
                                    $.ajax({
                                        url: createURL('listZones'),
                                        data: {},
                                        async: false,
                                        success: function(json) {
                                            var zones = json.listzonesresponse.zone;
                                            if (zones != null && zones.length > 0) {
                                                for (var i = 0; i < zones.length; i++) {
                                                    if (zones[i].networktype == "Advanced")
                                                        hasAdvancedZones = true;
                                                }
                                            }
                                        }
                                    });

                                    args.$form.bind('change', function() { //when any field in the dialog is changed

                                        var $providers = args.$form.find('.dynamic-input select');
                                        var $optionsOfProviders = $providers.find('option');

                                        $providers.each(function() {
                                            //if selected option is disabled, select the first enabled option instead
                                            if ($(this).find('option:selected:disabled').length > 0) {
                                                $(this).val($(this).find('option:first'));
                                            }
                                        });
                                    });

                                    args.$form.change();
                                },

                                fields: {
                                    name: {
                                        label: 'label.name',
                                        validation: {
                                            required: true
                                        },
                                        docID: 'helpVpcOfferingName'
                                    },

                                    displayText: {
                                        label: 'label.description',
                                        validation: {
                                            required: true
                                        },
                                        docID: 'helpVpcOfferingDescription'
                                    },

                                    supportedServices: {
                                        label: 'label.supported.services',

                                        dynamic: function(args) {
                                            var networkServiceObjs = [];
                                            networkServiceObjs.push({
                                                name: 'Dhcp',
                                                provider: [
                                                       {name: 'VpcVirtualRouter'},
                                                       {name: 'NuageVsp'}]
                                            });
                                            networkServiceObjs.push({
                                                name: 'Dns',
                                                provider: [{name: 'VpcVirtualRouter'}]
                                            });
                                            networkServiceObjs.push({
                                                name: 'Lb',
                                                provider: [{name: 'VpcVirtualRouter'}]
                                            });
                                            networkServiceObjs.push({
                                                name: 'Gateway',
                                                provider: [{name: 'VpcVirtualRouter'},
                                                           {name: 'BigSwitchBcf'}]
                                            });
                                            networkServiceObjs.push({
                                                name: 'StaticNat',
                                                provider: [
                                                       {name: 'VpcVirtualRouter'},
                                                       {name: 'NuageVsp'},
                                                       {name: 'BigSwitchBcf'}]
                                            });
                                            networkServiceObjs.push({
                                                name: 'SourceNat',
                                                provider: [
                                                       {name: 'VpcVirtualRouter'},
                                                       {name: 'NuageVsp'},
                                                       {name: 'BigSwitchBcf'}]
                                            });
                                            networkServiceObjs.push({
                                                name: 'NetworkACL',
                                                provider: [
                                                       {name: 'VpcVirtualRouter'},
                                                       {name: 'NuageVsp'},
                                                       {name: 'BigSwitchBcf'}]
                                            });
                                            networkServiceObjs.push({
                                                name: 'PortForwarding',
                                                provider: [{name: 'VpcVirtualRouter'}]
                                            });
                                            networkServiceObjs.push({
                                                name: 'UserData',
                                                provider: [{name: 'VpcVirtualRouter'}]
                                            });
                                            networkServiceObjs.push({
                                                name: 'Vpn',
                                                provider: [{name: 'VpcVirtualRouter'},
                                                           {name: 'BigSwitchBcf'}]
                                            });

                                            networkServiceObjs.push({
                                                name: 'Connectivity',
                                                provider: [
                                                    {name: 'BigSwitchBcf'},
                                                    {name: 'NiciraNvp'},
                                                    {name: 'Ovs'},
                                                    {name: 'JuniperContrailVpcRouter'},
                                                    {name: 'NuageVsp'}
                                                ]
                                            });

                                            serviceFields = [];
                                            var fields = {};
                                            $(networkServiceObjs).each(function() {
                                                var serviceName = this.name;
                                                var providerObjs = this.provider;
                                                var serviceDisplayName;

                                                // Sanitize names
                                                switch (serviceName) {
                                                    case 'Vpn':
                                                        serviceDisplayName = _l('label.vpn');
                                                        break;
                                                    case 'Dhcp':
                                                        serviceDisplayName = _l('label.dhcp');
                                                        break;
                                                    case 'Dns':
                                                        serviceDisplayName = _l('label.dns');
                                                        break;
                                                    case 'Lb':
                                                        serviceDisplayName = _l('label.load.balancer');
                                                        break;
                                                    case 'SourceNat':
                                                        serviceDisplayName = _l('label.source.nat');
                                                        break;
                                                    case 'StaticNat':
                                                        serviceDisplayName = _l('label.static.nat');
                                                        break;
                                                    case 'PortForwarding':
                                                        serviceDisplayName = _l('label.port.forwarding');
                                                        break;
                                                    case 'UserData':
                                                        serviceDisplayName = _l('label.user.data');
                                                        break;
                                                    default:
                                                        serviceDisplayName = serviceName;
                                                        break;
                                                }

                                                var id = {
                                                    isEnabled: 'service' + '.' + serviceName + '.' + 'isEnabled',
                                                    capabilities: 'service' + '.' + serviceName + '.' + 'capabilities',
                                                    provider: 'service' + '.' + serviceName + '.' + 'provider'
                                                };

                                                serviceCheckboxNames.push(id.isEnabled);

                                                fields[id.isEnabled] = {
                                                    label: serviceDisplayName,
                                                    isBoolean: true
                                                };

                                                serviceFields.push(id.isEnabled);

                                                fields[id.provider] = {
                                                    label: serviceDisplayName + ' Provider',
                                                    isHidden: true,
                                                    dependsOn: id.isEnabled,
                                                    select: function(args) {
                                                        var items = [];
                                                        $(providerObjs).each(function() {
                                                               items.push({
                                                                    id: this.name,
                                                                    description: this.name
                                                                });
                                                        });
                                                        args.response.success({
                                                            data: items
                                                        });
                                                    }
                                                }
                                            });

                                            args.response.success({
                                                fields: fields
                                            });
                                        }
                                    }, //end of supportedservices field

                                    "service.Connectivity.regionLevelVpcCapabilityCheckbox": {
                                        label: 'label.regionlevelvpc',
                                        isHidden: true,
                                        dependsOn: 'service.Connectivity.isEnabled',
                                        isBoolean: true
                                    },

                                    "service.Connectivity.distributedRouterCapabilityCheckbox": {
                                        label: 'label.distributedrouter',
                                        isHidden: true,
                                        dependsOn: 'service.Connectivity.isEnabled',
                                        isBoolean: true
                                    },

                                    "service.SourceNat.redundantRouterCapabilityCheckbox": {
                                        label: 'label.redundant.router.capability',
                                        isHidden: true,
                                        dependsOn: 'service.SourceNat.isEnabled',
                                        isBoolean: true
                                    }
                                }//end of fields
                            }, //end of createForm

                            action: function(args) {
                                var formData = args.data;
                                var inputData = {};
                                var serviceProviderMap = {};
                                var serviceCapabilityIndex = 0;
                                $.each(formData, function(key, value) {
                                    var serviceData = key.split('.');
                                    if (serviceData.length > 1) {
                                        if (serviceData[0] == 'service' &&
                                            serviceData[2] == 'isEnabled' &&
                                            value == 'on') { // Services field

                                            serviceProviderMap[serviceData[1]] = formData[
                                                'service.' + serviceData[1] + '.provider'
                                            ];
                                        } else if ((key == 'service.Connectivity.regionLevelVpcCapabilityCheckbox') && ("Connectivity" in serviceProviderMap)) {
                                            inputData['serviceCapabilityList[' + serviceCapabilityIndex + '].service'] = 'Connectivity';
                                            inputData['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilitytype'] = "RegionLevelVpc";
                                            inputData['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilityvalue'] = true;
                                            serviceCapabilityIndex++;
                                        } else if ((key == 'service.Connectivity.distributedRouterCapabilityCheckbox') && ("Connectivity" in serviceProviderMap)) {
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].service'] = 'Connectivity';
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilitytype'] = 'DistributedRouter';
                                            inputData['servicecapabilitylist[' + serviceCapabilityIndex + '].capabilityvalue'] = true;
                                            serviceCapabilityIndex++;
                                        } else if ((key == 'service.SourceNat.redundantRouterCapabilityCheckbox') && ("SourceNat" in serviceProviderMap)) {
                                            inputData['serviceCapabilityList[' + serviceCapabilityIndex + '].service'] = 'SourceNat';
                                            inputData['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilitytype'] = "RedundantRouter";
                                            inputData['serviceCapabilityList[' + serviceCapabilityIndex + '].capabilityvalue'] = true;
                                            serviceCapabilityIndex++;
                                        }

                                    } else if (value != '') { // Normal data
                                        inputData[key] = value;
                                    }
                                });

                                // Make supported services list
                                inputData['supportedServices'] = $.map(serviceProviderMap, function(value, key) {
                                    return key;
                                }).join(',');


                                // Make service provider map
                                var serviceProviderIndex = 0;
                                $.each(serviceProviderMap, function(key, value) {
                                    inputData['serviceProviderList[' + serviceProviderIndex + '].service'] = key;
                                    inputData['serviceProviderList[' + serviceProviderIndex + '].provider'] = value;
                                    serviceProviderIndex++;
                                });

                                $.ajax({
                                    url: createURL('createVPCOffering'),
                                    data: inputData,
                                    dataType: 'json',
                                    async: true,
                                    success: function(data) {
                                        var item = data.createvpcofferingresponse;

                                        args.response.success({
                                            data: item,
                                            actionFilter: vpcOfferingActionfilter
                                        });
                                    },

                                    error: function(data) {
                                        args.response.error(parseXMLHttpResponse(data));
                                    }
                                });
                            },

                            notification: {
                                poll: function(args) {
                                    args.complete({
                                        actionFilter: vpcOfferingActionfilter
                                    });
                                }
                            },

                            messages: {
                                notification: function(args) {
                                    return 'message.added.vpc.offering';
                                }
                            }
                        }
                    },

                    reorder: cloudStack.api.actions.sort('updateVPCOffering', 'vpcOfferings'),

                    detailView: {
                        name: 'label.vpc.offering.details',
                        actions: {
                            edit: {
                                label: 'label.edit',
                                action: function(args) {
                                    var data = {
                                        id: args.context.vpcOfferings[0].id,
                                        name: args.data.name,
                                        displaytext: args.data.displaytext,
                                        availability: args.data.availability
                                    };

                                    $.ajax({
                                        url: createURL('updateVPCOffering'),
                                        data: data,
                                        success: function(json) {
                                            var item = json.updatevpcofferingresponse.vpcoffering;
                                            args.response.success({
                                                data: item
                                            });
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                }
                            },

                            enable: {
                                label: 'label.enable.vpc.offering',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.confirm.enable.vpc.offering';
                                    },
                                    notification: function(args) {
                                        return 'message.enabling.vpc.offering';
                                    }
                                },
                                action: function(args) {
                                    $.ajax({
                                        url: createURL("updateVPCOffering&id=" + args.context.vpcOfferings[0].id + "&state=Enabled"),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var item = json.updatevpcofferingresponse.vpcoffering;
                                            args.response.success();
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                },
                                notification: {
                                    poll: function(args) {
                                        args.complete({
                                            data: {
                                                state: 'Enabled'
                                            }
                                        });
                                    }
                                }
                            },

                            disable: {
                                label: 'label.disable.vpc.offering',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.confirm.disable.vpc.offering';
                                    },
                                    notification: function(args) {
                                        return 'message.disabling.vpc.offering';
                                    }
                                },
                                action: function(args) {
                                    $.ajax({
                                        url: createURL("updateVPCOffering&id=" + args.context.vpcOfferings[0].id + "&state=Disabled"),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var item = json.updatevpcofferingresponse.vpcoffering;
                                            args.response.success();
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                },
                                notification: {
                                    poll: function(args) {
                                        args.complete({
                                            data: {
                                                state: 'Disabled'
                                            }
                                        });
                                    }
                                }
                            },

                            remove: {
                                label: 'label.remove.vpc.offering',
                                action: function(args) {
                                    $.ajax({
                                        url: createURL('deleteVPCOffering'),
                                        data: {
                                            id: args.context.vpcOfferings[0].id
                                        },
                                        success: function(json) {
                                            args.response.success();
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                },
                                messages: {
                                    confirm: function() {
                                        return 'message.confirm.remove.vpc.offering';
                                    },
                                    notification: function() {
                                        return 'label.remove.vpc.offering';
                                    }
                                },
                                notification: {
                                    poll: function(args) {
                                        args.complete({
                                            data: {
                                                state: 'Destroyed'
                                            },
                                            actionFilter: vpcOfferingActionfilter
                                        });
                                    }
                                }
                            }
                        },
                        tabs: {
                            details: {
                                title: 'label.details',

                                fields: [{
                                    name: {
                                        label: 'label.name',
                                        isEditable: true,
                                        validation: {
                                            required: true
                                        }
                                    }
                                }, {
                                    id: {
                                        label: 'label.id'
                                    },
                                    displaytext: {
                                        label: 'label.description',
                                        isEditable: true,
                                        validation: {
                                            required: true
                                        }
                                    },
                                    state: {
                                        label: 'label.state'
                                    },

                                    isdefault: { //created by system by default
                                        label: 'label.created.by.system',
                                        converter: cloudStack.converters.toBooleanText
                                    },

                                    supportedServices: {
                                        label: 'label.supported.services'
                                    },
                                    serviceCapabilities: {
                                        label: 'label.service.capabilities'
                                    },
                                    distributedvpcrouter: {
                                        label: 'label.vpc.distributedvpcrouter',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    supportsregionLevelvpc: {
                                        label: 'label.vpc.supportsregionlevelvpc',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    serviceCapabilities: {
                                        label: 'label.service.capabilities'
                                    },
                                    tags: {
                                        label: 'label.tags'
                                    }

                                }],

                                dataProvider: function(args) {
                                    $.ajax({
                                        url: createURL('listVPCOfferings&id=' + args.context.vpcOfferings[0].id),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var item = json.listvpcofferingsresponse.vpcoffering[0];
                                            args.response.success({
                                                actionFilter: vpcOfferingActionfilter,
                                                data: $.extend(item, {
                                                    supportedServices: $.map(item.service, function(service) {
                                                        return service.name;
                                                    }).join(', '),

                                                    serviceCapabilities: $.map(item.service, function(service) {
                                                        return service.provider ? $.map(service.provider, function(capability) {
                                                            return service.name + ': ' + capability.name;
                                                        }).join(', ') : null;
                                                    }).join(', ')
                                                })
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
}


    var serviceOfferingActionfilter = function(args) {
        var jsonObj = args.context.item;
        var allowedActions = [];
        allowedActions.push("edit");
        allowedActions.push("remove");
        return allowedActions;
    };

    var systemServiceOfferingActionfilter = function(args) {
        var jsonObj = args.context.item;
        var allowedActions = [];
        allowedActions.push("edit");
        allowedActions.push("remove");
        return allowedActions;
    };

    var diskOfferingActionfilter = function(args) {
        var jsonObj = args.context.item;
        var allowedActions = [];
        allowedActions.push("edit");
        allowedActions.push("remove");
        return allowedActions;
    };

    var networkOfferingActionfilter = function(args) {
        var jsonObj = args.context.item;

        if (jsonObj.state == 'Destroyed')
            return [];

        var allowedActions = [];
        allowedActions.push("edit");

        if (jsonObj.state == "Enabled")
            allowedActions.push("disable");
        else if (jsonObj.state == "Disabled")
            allowedActions.push("enable");

        if (jsonObj.isdefault == false)
            allowedActions.push("remove");

        return allowedActions;
    };

    var vpcOfferingActionfilter = function(args) {
        var jsonObj = args.context.item;

        if (jsonObj.state == 'Destroyed')
            return [];

        var allowedActions = [];
        allowedActions.push("edit");

        if (jsonObj.state == "Enabled")
            allowedActions.push("disable");
        else if (jsonObj.state == "Disabled")
            allowedActions.push("enable");

        if (jsonObj.isdefault == false)
            allowedActions.push("remove");

        return allowedActions;
    };

})(cloudStack, jQuery);
