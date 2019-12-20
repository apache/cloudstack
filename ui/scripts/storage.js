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
    var migrateVolumeCreateFormAction = {
            title: 'label.migrate.volume',
            fields: {
                storagePool: {
                    label: 'label.storage.pool',
                    validation: {
                        required: true
                    },
                    select: function(args) {
                        var mapStoragePoolsByUuid = new Map();
                        var volumeId = args.context.volumes[0].id;
                        var volumeBeingMigrated = undefined;
                        $.ajax({
                            url: createURL("listVolumes&id=" + volumeId),
                            dataType: "json",
                            async: false,
                            success: function(json){
                                volumeBeingMigrated = json.listvolumesresponse.volume[0]; 
                            }
                        });
                        var currentStoragePool = undefined;
                        $.ajax({
                            url: createURL("listStoragePools&id=" + volumeBeingMigrated.storageid),
                            dataType: "json",
                            async: false,
                            success: function(json){
                                currentStoragePool = json.liststoragepoolsresponse.storagepool[0]; 
                            }
                        });
                        var isVolumeNotAttachedToVm = volumeBeingMigrated.virtualmachineid == undefined;
                        var urlToRetrieveStoragePools = "findStoragePoolsForMigration&id=" + args.context.volumes[0].id;
                        if(isVolumeNotAttachedToVm){
                            urlToRetrieveStoragePools = "listStoragePools&zoneid=" + args.context.volumes[0].zoneid;
                        }
                        $.ajax({
                            url: createURL(urlToRetrieveStoragePools),
                            dataType: "json",
                            async: true,
                            success: function(json) {
                                var pools = undefined;
                                if(isVolumeNotAttachedToVm){
                                    pools = json.liststoragepoolsresponse.storagepool;
                                }else{
                                    pools = json.findstoragepoolsformigrationresponse.storagepool;
                                }
                                var items = [];
                                $(pools).each(function() {
                                    mapStoragePoolsByUuid.set(this.id, this);
                                    var description = this.name;
                                    if(!isVolumeNotAttachedToVm){
                                        description = description + " (" + (this.suitableformigration ? "Suitable" : "Not Suitable") + ")";
                                    }
                                    items.push({
                                        id: this.id,
                                        description: description
                                    });
                                });
                                args.response.success({
                                    data: items
                                });
                                var diskOfferings = cloudStack.listDiskOfferings({listAll: true});
                                $('select[name=storagePool]').change(function(){
                                    var uuidOfStoragePoolSelected = $(this).val();
                                    var storagePoolSelected = mapStoragePoolsByUuid.get(uuidOfStoragePoolSelected);
                                    if(currentStoragePool.scope === storagePoolSelected.scope){
                                        $('div[rel=newDiskOffering],div[rel=useNewDiskOffering]').hide();
                                    }else{
                                        $('div[rel=newDiskOffering],div[rel=useNewDiskOffering]').show();
                                    }
                                    var storageType = 'shared';
                                    if(storagePoolSelected.scope == 'HOST'){
                                        storageType = 'local';
                                    }
                                    $(diskOfferings).each(function(){
                                        var diskOfferingOption = $('option[value=' + this.id + ']');
                                        if(this.storagetype == storageType){
                                            diskOfferingOption.show();
                                        }else{
                                            diskOfferingOption.hide();
                                        }
                                    });
                                    var firstAvailableDiskOfferingForStorageType = $('select#label_disk_newOffering').children('option:visible').first().attr('value');
                                    $('select#label_disk_newOffering').attr('value', firstAvailableDiskOfferingForStorageType);
                                });
                                var functionHideShowNewDiskOfferint = function(){
                                    if($('div[rel=useNewDiskOffering] input[type=checkbox]').is(':checked')){
                                        $('div[rel=newDiskOffering]').show();
                                    }else{
                                        $('div[rel=newDiskOffering]').hide();
                                    }  
                                };
                                $('div[rel=useNewDiskOffering] input[type=checkbox]').click(functionHideShowNewDiskOfferint);
                                $('select[name=storagePool]').change();
                                functionHideShowNewDiskOfferint();
                            }
                        });
                    }
                },
            useNewDiskOffering:{
                label: 'label.migrate.volume.newDiskOffering',
                desc: 'label.migrate.volume.newDiskOffering.desc',
                validation: {
                    required: false
                   },
                isEditable: true, 
                isBoolean: true,
                defaultValue: 'Yes'
            },
            newDiskOffering: {
                label: 'label.disk.newOffering',
                desc: 'label.disk.newOffering.description',
                validation: {
                    required: false
                   },
                select: function(args){
                    var diskOfferings = cloudStack.listDiskOfferings({listAll: true});
                    var items = [];
                    $(diskOfferings).each(function() {
                        items.push({
                            id: this.id,
                            description: this.name
                        });
                    });
                    args.response.success({
                        data: items
                    });
                   }
               }
           }
        };
    var functionMigrateVolume = function(args) {
        var volumeBeingMigrated = args.context.volumes[0];
        var isLiveMigrate = volumeBeingMigrated.vmstate == 'Running';
        var migrateVolumeUrl = "migrateVolume&livemigrate="+ isLiveMigrate +"&storageid=" + args.data.storagePool + "&volumeid=" + volumeBeingMigrated.id;
        if($('div[rel=useNewDiskOffering] input[name=useNewDiskOffering]:checkbox').is(':checked')){
            migrateVolumeUrl = migrateVolumeUrl + '&newdiskofferingid=' + $('div[rel=newDiskOffering] select').val();
        }
        $.ajax({
            url: createURL(migrateVolumeUrl),
            dataType: "json",
            async: true,
            success: function(json) {
                $(window).trigger('cloudStack.fullRefresh');
                var jid = json.migratevolumeresponse.jobid;
                args.response.success({
                    _custom: {
                        jobId: jid
                    }
                });
            }
        });
    }

    var diskOfferingsObjList, selectedDiskOfferingObj = null;

    cloudStack.sections.storage = {
        title: 'label.storage',
        id: 'storage',
        sectionSelect: {
            label: 'label.select-view'
        },
        sections: {
            /**
             * Volumes
             */
            volumes: {
                type: 'select',
                title: 'label.volumes',
                listView: {
                    id: 'volumes',
                    label: 'label.volumes',
                    preFilter: function(args) {
                        var hiddenFields = [];
                        if (isAdmin() != true) {
                            hiddenFields.push('hypervisor');
                            hiddenFields.push('account');
                        }
                        return hiddenFields;
                    },
                    fields: {
                        name: {
                            label: 'label.name'
                        },
                        type: {
                            label: 'label.type'
                        },
                        vmdisplayname: {
                            label: 'label.vm.display.name'
                        },
                        hypervisor: {
                            label: 'label.hypervisor'
                        },
                        account: {
                            label: 'label.account'
                        },
                        zonename: {
                            label: 'label.zone'
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
                            }
                        }
                    },

                    // List view actions
                    actions: {
                        // Add volume
                        add: {
                            label: 'label.add',

                            preFilter: function(args) {
                                return !args.context.instances;
                            },

                            messages: {
                                confirm: function(args) {
                                    return 'message.add.volume';
                                },
                                notification: function(args) {
                                    return 'label.add.volume';
                                }
                            },

                            createForm: {
                                title: 'label.add.volume',
                                desc: 'message.add.volume',
                                fields: {
                                    name: {
                                        docID: 'helpVolumeName',
                                        label: 'label.name'
                                    },
                                    availabilityZone: {
                                        label: 'label.availability.zone',
                                        docID: 'helpVolumeAvailabilityZone',
                                        select: function(args) {
                                            $.ajax({
                                                url: createURL("listZones&available=true"),
                                                dataType: "json",
                                                async: true,
                                                success: function(json) {
                                                    var zoneObjs = json.listzonesresponse.zone;
                                                    args.response.success({
                                                        descriptionField: 'name',
                                                        data: zoneObjs
                                                    });
                                                }
                                            });
                                            args.$select.change(function() {
                                                var diskOfferingSelect = $(this).closest('form').find('select[name=diskOffering]');
                                                if(diskOfferingSelect) {
                                                    $(diskOfferingSelect).find('option').remove().end();
                                                    var data = {
                                                        zoneid: $(this).val(),
                                                    };
                                                    console.log(data);
                                                    var diskOfferings = cloudStack.listDiskOfferings({ data: data });
                                                    diskOfferingsObjList = diskOfferings;
                                                    $(diskOfferings).each(function() {
                                                        $(diskOfferingSelect).append(new Option(this.displaytext, this.id));
                                                    });
                                                }
                                            });
                                        }
                                    },
                                    diskOffering: {
                                        label: 'label.disk.offering',
                                        docID: 'helpVolumeDiskOffering',
                                        select: function(args) {
                                            var diskOfferings = cloudStack.listDiskOfferings({});
                                            diskOfferingsObjList = diskOfferings;
                                            var items = [];
                                            $(diskOfferings).each(function() {
                                                items.push({
                                                    id: this.id,
                                                    description: this.displaytext
                                                });
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                            args.$select.change(function() {
                                                var diskOfferingId = $(this).val();
                                                $(diskOfferingsObjList).each(function() {
                                                    if (this.id == diskOfferingId) {
                                                        selectedDiskOfferingObj = this;
                                                        return false; //break the $.each() loop
                                                    }
                                                });
                                                if (selectedDiskOfferingObj == null){
                                                    return;
                                                }
                                                var $form = $(this).closest('form');
                                                var $diskSize = $form.find('.form-item[rel=diskSize]');
                                                if (selectedDiskOfferingObj.iscustomized == true) {
                                                    $diskSize.css('display', 'inline-block');
                                                } else {
                                                    $diskSize.hide();
                                                }
                                                var $minIops = $form.find('.form-item[rel=minIops]');
                                                var $maxIops = $form.find('.form-item[rel=maxIops]');
                                                if (selectedDiskOfferingObj.iscustomizediops == true) {
                                                    $minIops.css('display', 'inline-block');
                                                    $maxIops.css('display', 'inline-block');
                                                } else {
                                                    $minIops.hide();
                                                    $maxIops.hide();
                                                }
                                            });
                                        }
                                    }

                                    ,
                                    diskSize: {
                                        label: 'label.disk.size.gb',
                                        docID: 'helpVolumeSizeGb',
                                        validation: {
                                            required: true,
                                            number: true
                                        },
                                        isHidden: true
                                    },

                                    minIops: {
                                        label: 'label.disk.iops.min',
                                        validation: {
                                            required: false,
                                            number: true
                                        },
                                        isHidden: true
                                    },

                                    maxIops: {
                                        label: 'label.disk.iops.max',
                                        validation: {
                                            required: false,
                                            number: true
                                        },
                                        isHidden: true
                                    }

                                }
                            },

                            action: function(args) {
                                var data = {
                                    name: args.data.name,
                                    zoneId: args.data.availabilityZone,
                                    diskOfferingId: args.data.diskOffering
                                };

                                // if(thisDialog.find("#size_container").css("display") != "none") { //wait for Brian to include $form in args
                                if (selectedDiskOfferingObj.iscustomized == true) {
                                    $.extend(data, {
                                        size: args.data.diskSize
                                    });
                                }

                                if (selectedDiskOfferingObj.iscustomizediops == true) {
                                    if (args.data.minIops != "" && args.data.minIops > 0) {
                                        $.extend(data, {
                                            miniops: args.data.minIops
                                        });
                                    }

                                    if (args.data.maxIops != "" && args.data.maxIops > 0) {
                                        $.extend(data, {
                                            maxiops: args.data.maxIops
                                        });
                                    }
                                }

                                $.ajax({
                                    url: createURL('createVolume'),
                                    data: data,
                                    success: function(json) {
                                        var jid = json.createvolumeresponse.jobid;
                                        args.response.success({
                                            _custom: {
                                                jobId: jid,
                                                getUpdatedItem: function(json) {
                                                    return json.queryasyncjobresultresponse.jobresult.volume;
                                                },
                                                getActionFilter: function() {
                                                    return volumeActionfilter;
                                                }
                                            }
                                        });
                                    },
                                    error: function(json) {
                                        args.response.error(parseXMLHttpResponse(json));
                                    }
                                });
                            },

                            notification: {
                                poll: pollAsyncJobResult
                            }
                        },

                        viewMetrics: {
                            label: 'label.metrics',
                            isHeader: true,
                            addRow: false,
                            action: {
                                custom: cloudStack.uiCustom.metricsView({resource: 'volumes'})
                            },
                            messages: {
                                notification: function (args) {
                                    return 'label.metrics';
                                }
                            }
                        },

                        uploadVolume: {
                            isHeader: true,
                            label: 'label.upload',
                            preFilter: function(args) {
                                return !args.context.instances;
                            },
                            messages: {
                                notification: function() {
                                    return 'label.upload.volume.from.url';
                                }
                            },
                            createForm: {
                                title: 'label.upload.volume.from.url',
                                fields: {
                                    url: {
                                        label: 'label.url',
                                        docID: 'helpUploadVolumeURL',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    name: {
                                        label: 'label.name',
                                        validation: {
                                            required: true
                                        },
                                        docID: 'helpUploadVolumeName'
                                    },
                                    availabilityZone: {
                                        label: 'label.availability.zone',
                                        docID: 'helpUploadVolumeZone',
                                        select: function(args) {
                                            $.ajax({
                                                url: createURL("listZones&available=true"),
                                                dataType: "json",
                                                async: true,
                                                success: function(json) {
                                                    var zoneObjs = json.listzonesresponse.zone;
                                                    args.response.success({
                                                        descriptionField: 'name',
                                                        data: zoneObjs
                                                    });
                                                }
                                            });
                                        }
                                    },
                                    format: {
                                        label: 'label.format',
                                        docID: 'helpUploadVolumeFormat',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: 'RAW',
                                                description: 'RAW'
                                            });
                                            items.push({
                                                id: 'VHD',
                                                description: 'VHD'
                                            });
                                            items.push({
                                                id: 'VHDX',
                                                description: 'VHDX'
                                            });
                                            items.push({
                                                id: 'OVA',
                                                description: 'OVA'
                                            });
                                            items.push({
                                                id: 'QCOW2',
                                                description: 'QCOW2'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }

                                    },
                                    diskOffering: {
                                        label: 'label.custom.disk.offering',
                                        docID: 'helpVolumeDiskOffering',
                                        select: function(args) {
                                            var diskOfferings = cloudStack.listDiskOfferings({});
                                            var items = [{
                                                id: '',
                                                description: ''
                                            }];
                                            $(diskOfferings).each(function() {
                                                if (this.iscustomized == true) {
                                                    items.push({
                                                        id: this.id,
                                                        description: this.name
                                                    });
                                                }
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    checksum: {
                                        docID: 'helpUploadVolumeChecksum',
                                        label: 'label.md5.checksum'
                                    }
                                }
                            },

                            action: function(args) {
                                var data = {
                                    name: args.data.name,
                                    zoneId: args.data.availabilityZone,
                                    format: args.data.format,
                                    url: args.data.url
                                };

                                if (args.data.diskOffering != '' && args.data.diskOffering.length > 0) {
                                    $.extend(data, {
                                        diskofferingid: args.data.diskOffering
                                    });
                                }

                                if (args.data.checksum != null && args.data.checksum.length > 0) {
                                    $.extend(data, {
                                        checksum: args.data.checksum
                                    });
                                }

                                $.ajax({
                                    url: createURL('uploadVolume'),
                                    data: data,
                                    success: function(json) {
                                        var jid = json.uploadvolumeresponse.jobid;
                                        args.response.success({
                                            _custom: {
                                                jobId: jid,
                                                getUpdatedItem: function(json) {
                                                    return json.queryasyncjobresultresponse.jobresult.volume;
                                                },
                                                getActionFilter: function() {
                                                    return volumeActionfilter;
                                                }
                                            }
                                        });
                                    },
                                    error: function(json) {
                                        args.response.error(parseXMLHttpResponse(json));
                                    }
                                });
                            },

                            notification: {
                                poll: pollAsyncJobResult
                            }
                        },

                        uploadVolumefromLocal: {
                            isHeader: true,
                            label: 'label.upload.from.local',
                            preFilter: function(args) {
                                return !args.context.instances;
                            },
                            messages: {
                                notification: function() {
                                    return 'label.upload.volume.from.local';
                                }
                            },
                            createForm: {
                                title: 'label.upload.volume.from.local',
                                fileUpload: {
                                    getURL: function(args) {
                                        args.data = args.formData;

                                        var data = {
                                            name: args.data.name,
                                            zoneId: args.data.availabilityZone,
                                            format: args.data.format,
                                            url: args.data.url
                                        };

                                        if (args.data.checksum != null && args.data.checksum.length > 0) {
                                            $.extend(data, {
                                                checksum: args.data.checksum
                                            });
                                        }

                                        $.ajax({
                                            url: createURL('getUploadParamsForVolume'),
                                            data: data,
                                            async: false,
                                            success: function(json) {
                                                var uploadparams = json.postuploadvolumeresponse.getuploadparams; //son.postuploadvolumeresponse.getuploadparams is an object, not an array of object.
                                                var volumeId = uploadparams.id;

                                                args.response.success({
                                                    url: uploadparams.postURL,
                                                    ajaxPost: true,
                                                    data: {
                                                        'X-signature': uploadparams.signature,
                                                        'X-expires': uploadparams.expires,
                                                        'X-metadata': uploadparams.metadata
                                                    }
                                                });
                                            }
                                        });
                                    },
                                    postUpload: function(args) {
                                        if(args.error) {
                                            args.response.error(args.errorMsg);
                                        } else {
                                            cloudStack.dialog.notice({
                                                message: "This volume file has been uploaded. Please check its status at Stroage menu > Volumes > " + args.data.name + " > Status field."
                                            });
                                            args.response.success();
                                        }
                                    }
                                },
                                fields: {
                                    volumeFileUpload: {
                                        label: 'label.local.file',
                                        isFileUpload: true,
                                        validation: {
                                            required: true
                                        }
                                    },
                                    name: {
                                        label: 'label.name',
                                        validation: {
                                            required: true
                                        },
                                        docID: 'helpUploadVolumeName'
                                    },
                                    availabilityZone: {
                                        label: 'label.availability.zone',
                                        docID: 'helpUploadVolumeZone',
                                        select: function(args) {
                                            $.ajax({
                                                url: createURL("listZones&available=true"),
                                                dataType: "json",
                                                async: true,
                                                success: function(json) {
                                                    var zoneObjs = json.listzonesresponse.zone;
                                                    args.response.success({
                                                        descriptionField: 'name',
                                                        data: zoneObjs
                                                    });
                                                }
                                            });
                                        }
                                    },
                                    format: {
                                        label: 'label.format',
                                        docID: 'helpUploadVolumeFormat',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: 'RAW',
                                                description: 'RAW'
                                            });
                                            items.push({
                                                id: 'VHD',
                                                description: 'VHD'
                                            });
                                            items.push({
                                                id: 'VHDX',
                                                description: 'VHDX'
                                            });
                                            items.push({
                                                id: 'OVA',
                                                description: 'OVA'
                                            });
                                            items.push({
                                                id: 'QCOW2',
                                                description: 'QCOW2'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    checksum: {
                                        docID: 'helpUploadVolumeChecksum',
                                        label: 'label.md5.checksum'
                                    }
                                }
                            },

                            action: function(args) {
                                return; //createForm.fileUpload.getURL() has executed the whole action. Therefore, nothing needs to be done here.
                            },

                            notification: {
                                poll: pollAsyncJobResult
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
                                    data: {},
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

                        domainid: {
                            label: 'label.domain',
                            select: function(args) {
                                if (isAdmin() || isDomainAdmin()) {
                                    $.ajax({
                                        url: createURL('listDomains'),
                                        data: {
                                            listAll: true,
                                            details: 'min'
                                        },
                                        success: function(json) {
                                            var array1 = [{
                                                id: '',
                                                description: ''
                                            }];
                                            var domains = json.listdomainsresponse.domain;
                                            if (domains != null && domains.length > 0) {
                                                for (var i = 0; i < domains.length; i++) {
                                                    array1.push({
                                                        id: domains[i].id,
                                                        description: domains[i].path
                                                    });
                                                }
                                            }
                                            array1.sort(function(a, b) {
                                                return a.description.localeCompare(b.description);
                                            });
                                            args.response.success({
                                                data: array1
                                            });
                                        }
                                    });
                                } else {
                                    args.response.success({
                                        data: null
                                    });
                                }
                            },
                            isHidden: function(args) {
                                if (isAdmin() || isDomainAdmin())
                                    return false;
                                else
                                    return true;
                            }
                        },

                        account: {
                            label: 'label.account',
                            isHidden: function(args) {
                                if (isAdmin() || isDomainAdmin())
                                    return false;
                                else
                                    return true;
                            }
                        },

                        tagKey: {
                            label: 'label.tag.key'
                        },
                        tagValue: {
                            label: 'label.tag.value'
                        }
                    },

                    dataProvider: function(args) {
                        var data = {};
                        listViewDataProvider(args, data);

                        if (args.context != null) {
                            if ("instances" in args.context) {
                                $.extend(data, {
                                    virtualMachineId: args.context.instances[0].id
                                });
                            }
                            if ("primarystorages" in args.context) {
                                $.extend(data, {
                                    storageid: args.context.primarystorages[0].id
                                });
                            }
                        }

                        $.ajax({
                            url: createURL('listVolumes'),
                            data: data,
                            success: function(json) {
                                var items = json.listvolumesresponse.volume;
                                args.response.success({
                                    actionFilter: volumeActionfilter,
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
                    },

                    detailView: {
                        name: 'label.volume.details',
                        viewAll: {
                            path: 'storage.snapshots',
                            label: 'label.snapshots'
                        },
                        actions: {
                            migrateVolume: {
                                label: 'label.migrate.volume',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.confirm.migrate.volume';
                                    },
                                    notification: function(args) {
                                        return 'label.volume.migrated';
                                    }
                                },

                                createForm: migrateVolumeCreateFormAction,

                                action: functionMigrateVolume,
                                notification: {
                                    poll: pollAsyncJobResult
                                }

                            },

                            takeSnapshot: {
                                label: 'label.action.take.snapshot',
                                messages: {
                                    notification: function(args) {
                                        return 'label.action.take.snapshot';
                                    }
                                },
                                createForm: {
                                    title: 'label.action.take.snapshot',
                                    desc: 'message.action.take.snapshot',
                                    fields: {
                                        quiescevm: {
                                            label: 'label.quiesce.vm',
                                            isBoolean: true,
                                            isHidden: function(args) {
                                                if (args.context.volumes[0].quiescevm == true)
                                                    return false;
                                                        else
                                                    return true;
                                            }
                                        },
                                        name: {
                                            label: 'label.name'
                                        },
                                        asyncBackup: {
                                            label: 'label.async.backup',
                                            isBoolean: true
                                        },
                                        tags: {
                                            label: 'label.tags',
                                            tagger: true
                                        }
                                    }
                                },
                                action: function(args) {
                                    var data = {
                                        volumeId: args.context.volumes[0].id,
                                        quiescevm: (args.data.quiescevm == 'on' ? true: false),
                                        asyncBackup: (args.data.asyncBackup == 'on' ? true: false)
                                    };
                                    if (args.data.name != null && args.data.name.length > 0) {
                                        $.extend(data, {
                                            name: args.data.name
                                        });
                                    }
                                    if (!$.isEmptyObject(args.data.tags)) {
                                        $(args.data.tags).each(function(idx, tagData) {
                                            var formattedTagData = {};
                                            formattedTagData["tags[" + _s(idx) + "].key"] = _s(tagData.key);
                                            formattedTagData["tags[" + _s(idx) + "].value"] = _s(tagData.value);
                                            $.extend(data, formattedTagData);
                                        });
                                    }

                                    $.ajax({
                                        url: createURL("createSnapshot"),
                                        data: data,
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var jid = json.createsnapshotresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid, //take snapshot from a volume doesn't change any property in this volume. So, don't need to specify getUpdatedItem() to return updated volume. Besides, createSnapshot API doesn't return updated volume.
                                                    onComplete: function(json, customData) {
                                                        var volumeId = json.queryasyncjobresultresponse.jobresult.snapshot.volumeid;
                                                        var snapshotId = json.queryasyncjobresultresponse.jobresult.snapshot.id;
                                                        cloudStack.dialog.notice({
                                                            message: "Created snapshot for volume " + volumeId + " with snapshot ID " + snapshotId
                                                        });
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

                            recurringSnapshot: {
                                label: 'label.snapshot.schedule',
                                action: {
                                    custom: cloudStack.uiCustom.recurringSnapshots({
                                        desc: 'message.snapshot.schedule',
                                        dataProvider: function(args) {
                                            $.ajax({
                                                url: createURL('listSnapshotPolicies'),
                                                data: {
                                                    volumeid: args.context.volumes[0].id
                                                },
                                                async: true,
                                                dataType: 'json',
                                                success: function(data) {
                                                    args.response.success({
                                                        data: $.map(
                                                            data.listsnapshotpoliciesresponse.snapshotpolicy ? data.listsnapshotpoliciesresponse.snapshotpolicy : [],
                                                            function(snapshot, index) {
                                                                return {
                                                                    id: snapshot.id,
                                                                    type: snapshot.intervaltype,
                                                                    time: snapshot.intervaltype > 0 ? snapshot.schedule.split(':')[1] + ':' + snapshot.schedule.split(':')[0] : snapshot.schedule,
                                                                    timezone: snapshot.timezone,
                                                                    keep: snapshot.maxsnaps,
                                                                    'day-of-week': snapshot.intervaltype == 2 ? snapshot.schedule.split(':')[2] : null,
                                                                    'day-of-month': snapshot.intervaltype == 3 ? snapshot.schedule.split(':')[2] : null
                                                                };
                                                            }
                                                        )
                                                    });
                                                }
                                            });
                                        },
                                        actions: {
                                            add: function(args) {
                                                var snap = args.snapshot;

                                                var data = {
                                                    volumeid: args.context.volumes[0].id,
                                                    intervaltype: snap['snapshot-type'],
                                                    maxsnaps: snap.maxsnaps,
                                                    timezone: snap.timezone
                                                };

                                                var convertTime = function(minute, hour, meridiem, extra) {
                                                    var convertedHour = meridiem == 'PM' ?
                                                        (hour != 12 ? parseInt(hour) + 12 : 12) : (hour != 12 ? hour : '00');
                                                    var time = minute + ':' + convertedHour;
                                                    if (extra) time += ':' + extra;

                                                    return time;
                                                };

                                                switch (snap['snapshot-type']) {
                                                    case 'hourly': // Hourly
                                                        $.extend(data, {
                                                            schedule: snap.schedule
                                                        });
                                                        break;

                                                    case 'daily': // Daily
                                                        $.extend(data, {
                                                            schedule: convertTime(
                                                                snap['time-minute'],
                                                                snap['time-hour'],
                                                                snap['time-meridiem']
                                                            )
                                                        });
                                                        break;

                                                    case 'weekly': // Weekly
                                                        $.extend(data, {
                                                            schedule: convertTime(
                                                                snap['time-minute'],
                                                                snap['time-hour'],
                                                                snap['time-meridiem'],
                                                                snap['day-of-week']
                                                            )
                                                        });
                                                        break;

                                                    case 'monthly': // Monthly
                                                        $.extend(data, {
                                                            schedule: convertTime(
                                                                snap['time-minute'],
                                                                snap['time-hour'],
                                                                snap['time-meridiem'],
                                                                snap['day-of-month']
                                                            )
                                                        });
                                                        break;
                                                }

                                                if (!$.isEmptyObject(snap.tags)) {
                                                    $(snap.tags).each(function(idx, tagData) {
                                                        var formattedTagData = {};
                                                        formattedTagData["tags[" + _s(idx) + "].key"] = _s(tagData.key);
                                                        formattedTagData["tags[" + _s(idx) + "].value"] = _s(tagData.value);
                                                        $.extend(data, formattedTagData);
                                                    });
                                                }

                                                $.ajax({
                                                    url: createURL('createSnapshotPolicy'),
                                                    data: data,
                                                    dataType: 'json',
                                                    async: true,
                                                    success: function(successData) {
                                                        var snapshot = successData.createsnapshotpolicyresponse.snapshotpolicy;

                                                        args.response.success({
                                                            data: {
                                                                id: snapshot.id,
                                                                type: snapshot.intervaltype,
                                                                time: snapshot.intervaltype > 0 ? snapshot.schedule.split(':')[1] + ':' + snapshot.schedule.split(':')[0] : snapshot.schedule,
                                                                timezone: snapshot.timezone,
                                                                keep: snapshot.maxsnaps,
                                                                'day-of-week': snapshot.intervaltype == 2 ? snapshot.schedule.split(':')[2] : null,
                                                                'day-of-month': snapshot.intervaltype == 3 ? snapshot.schedule.split(':')[2] : null
                                                            }
                                                        });
                                                    }
                                                });
                                            },
                                            remove: function(args) {
                                                $.ajax({
                                                    url: createURL('deleteSnapshotPolicies'),
                                                    data: {
                                                        id: args.snapshot.id
                                                    },
                                                    dataType: 'json',
                                                    async: true,
                                                    success: function(data) {
                                                        args.response.success();
                                                    }
                                                });
                                            }
                                        },

                                        // Select data
                                        selects: {
                                            schedule: function(args) {
                                                var time = [];

                                                for (var i = 1; i <= 59; i++) {
                                                    time.push({
                                                        id: i,
                                                        name: i
                                                    });
                                                }

                                                args.response.success({
                                                    data: time
                                                });
                                            },
                                            timezone: function(args) {
                                                args.response.success({
                                                    data: $.map(timezoneMap, function(value, key) {
                                                        return {
                                                            id: key,
                                                            name: value
                                                        };
                                                    })
                                                });
                                            },
                                            'day-of-week': function(args) {
                                                args.response.success({
                                                    data: [{
                                                        id: 1,
                                                        name: 'label.sunday'
                                                    }, {
                                                        id: 2,
                                                        name: 'label.monday'
                                                    }, {
                                                        id: 3,
                                                        name: 'label.tuesday'
                                                    }, {
                                                        id: 4,
                                                        name: 'label.wednesday'
                                                    }, {
                                                        id: 5,
                                                        name: 'label.thursday'
                                                    }, {
                                                        id: 6,
                                                        name: 'label.friday'
                                                    }, {
                                                        id: 7,
                                                        name: 'label.saturday'
                                                    }]
                                                });
                                            },

                                            'day-of-month': function(args) {
                                                var time = [];

                                                for (var i = 1; i <= 28; i++) {
                                                    time.push({
                                                        id: i,
                                                        name: i
                                                    });
                                                }

                                                args.response.success({
                                                    data: time
                                                });
                                            },

                                            'time-hour': function(args) {
                                                var time = [];

                                                for (var i = 1; i <= 12; i++) {
                                                    time.push({
                                                        id: i,
                                                        name: i
                                                    });
                                                }

                                                args.response.success({
                                                    data: time
                                                });
                                            },

                                            'time-minute': function(args) {
                                                var time = [];

                                                for (var i = 0; i <= 59; i++) {
                                                    time.push({
                                                        id: i < 10 ? '0' + i : i,
                                                        name: i < 10 ? '0' + i : i
                                                    });
                                                }

                                                args.response.success({
                                                    data: time
                                                });
                                            },

                                            'time-meridiem': function(args) {
                                                args.response.success({
                                                    data: [{
                                                        id: 'AM',
                                                        name: 'AM'
                                                    }, {
                                                        id: 'PM',
                                                        name: 'PM'
                                                    }]
                                                });
                                            }
                                        }
                                    })
                                },
                                messages: {
                                    notification: function(args) {
                                        return 'label.snapshot.schedule';
                                    }
                                }
                            },

                            attachDisk: {
                                addRow: 'false',
                                label: 'label.action.attach.disk',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.confirm.attach.disk';
                                    },
                                    notification: function(args) {
                                        return 'label.action.attach.disk';
                                    }
                                },
                                createForm: {
                                    title: 'label.action.attach.disk',
                                    desc: 'label.action.attach.disk',
                                    fields: {
                                        virtualMachineId: {
                                            label: 'label.instance',
                                            select: function(args) {
                                                var zoneid = args.context.volumes[0].zoneid;
                                                var items = [];
                                                var data;

                                                if (!args.context.projects) {
                                                    data = {
                                                        zoneid: zoneid,
                                                        domainid: args.context.volumes[0].domainid,
                                                        account: args.context.volumes[0].account
                                                    };
                                                } else {
                                                    data = {
                                                        zoneid: zoneid,
                                                        projectid: args.context.projects[0].id
                                                    };
                                                }

                                                if (args.context.volumes[0].hypervisor != null && args.context.volumes[0].hypervisor.length > 0 && args.context.volumes[0].hypervisor != 'None') {
                                                    data = $.extend(data, {
                                                        hypervisor: args.context.volumes[0].hypervisor
                                                    });
                                                }

                                                $(['Running', 'Stopped']).each(function() {
                                                    $.ajax({
                                                        url: createURL('listVirtualMachines'),
                                                        data: $.extend(data, {
                                                            state: this.toString()
                                                        }),
                                                        async: false,
                                                        success: function(json) {
                                                            var instanceObjs = json.listvirtualmachinesresponse.virtualmachine;
                                                            $(instanceObjs).each(function() {
                                                                items.push({
                                                                    id: this.id,
                                                                    description: this.displayname ? this.displayname : this.name
                                                                });
                                                            });
                                                        }
                                                    });
                                                });

                                                args.response.success({
                                                    data: items
                                                });
                                            }
                                        }
                                    }
                                },
                                action: function(args) {
                                    $.ajax({
                                        url: createURL("attachVolume&id=" + args.context.volumes[0].id + '&virtualMachineId=' + args.data.virtualMachineId),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var jid = json.attachvolumeresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid,
                                                    getUpdatedItem: function(json) {
                                                        return json.queryasyncjobresultresponse.jobresult.volume;
                                                    },
                                                    getActionFilter: function() {
                                                        return volumeActionfilter;
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
                            detachDisk: {
                                label: 'label.action.detach.disk',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.detach.disk';
                                    },
                                    notification: function(args) {
                                        return 'label.action.detach.disk';
                                    }
                                },
                                action: function(args) {
                                    $.ajax({
                                        url: createURL("detachVolume&id=" + args.context.volumes[0].id),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var jid = json.detachvolumeresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid,
                                                    getUpdatedItem: function(json) {
                                                        return {
                                                            virtualmachineid: null,
                                                            vmdisplayname: null
                                                        };
                                                    },
                                                    getActionFilter: function() {
                                                        return volumeActionfilter;
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

                            downloadVolume: {
                                label: 'label.action.download.volume',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.download.volume.confirm';
                                    },
                                    notification: function(args) {
                                        return 'label.action.download.volume';
                                    },
                                    complete: function(args) {
                                        var url = args.url;
                                        var htmlMsg = _l('message.download.volume');
                                        var htmlMsg2 = htmlMsg.replace(/#/, url).replace(/00000/, url);
                                        //$infoContainer.find("#info").html(htmlMsg2);
                                        return htmlMsg2;
                                    }
                                },
                                action: function(args) {
                                    $.ajax({
                                        url: createURL("extractVolume&id=" + args.context.volumes[0].id + "&zoneid=" + args.context.volumes[0].zoneid + "&mode=HTTP_DOWNLOAD"),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var jid = json.extractvolumeresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid,
                                                    getUpdatedItem: function(json) {
                                                        return json.queryasyncjobresultresponse.jobresult.volume;
                                                    },
                                                    getActionFilter: function() {
                                                        return volumeActionfilter;
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

                            createTemplate: cloudStack.createTemplateMethod(false),
                            migrateToAnotherStorage: {
                                label: 'label.migrate.volume.to.primary.storage',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.migrate.volume';
                                    },
                                    notification: function(args) {
                                        return 'label.migrate.volume.to.primary.storage';
                                    }
                                },
                                createForm: $.extend({}, migrateVolumeCreateFormAction, {title: 'label.migrate.volume.to.primary.storage'}),
                                action: functionMigrateVolume,
                                notification: {
                                    poll: pollAsyncJobResult
                                }
                            },

                            remove: {
                                label: 'label.action.delete.volume',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.action.delete.volume';
                                    },
                                    notification: function(args) {
                                        return 'label.action.delete.volume';
                                    }
                                },
                                action: function(args) {
                                    $.ajax({
                                        url: createURL("deleteVolume&id=" + args.context.volumes[0].id),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            args.response.success();
                                        }
                                    });
                                },
                                notification: {
                                    poll: function(args) {
                                        args.complete();
                                    }
                                }
                            },

                            resize: {
                                label: 'label.action.resize.volume',
                                messages: {
                                    notification: function(args) {
                                        return 'label.action.resize.volume';
                                    }
                                },
                                createForm: {
                                    title: 'label.action.resize.volume',
                                    preFilter: function(args) {
                                        var vol;
                                        if (args.context.volumes != null) vol = args.context.volumes[0];
                                        if (vol.type == "ROOT" && (vol.hypervisor == "XenServer" || vol.hypervisor == "KVM" || vol.hypervisor == "VMware")) {
                                            args.$form.find('.form-item[rel=newdiskoffering]').hide();
                                            args.$form.find('.form-item[rel=newsize]').css('display', 'inline-block');
                                        } else {
                                            args.$form.find('.form-item[rel=newdiskoffering]').css('display', 'inline-block');
                                            args.$form.find('.form-item[rel=newsize]').hide();
                                        }
                                    },
                                    fields: {
                                        newdiskoffering: {
                                            label: 'label.resize.new.offering.id',
                                            isHidden: true,
                                            select: function(args) {
                                                if (args.context.volumes != null && args.context.volumes[0].type == 'ROOT') {
                                                    args.response.success({
                                                        data: []
                                                    });
                                                    return;
                                                }
                                                var diskOfferings = cloudStack.listDiskOfferings({});
                                                var items = [];
                                                $(diskOfferings).each(function() {
                                                    items.push({
                                                        id: this.id,
                                                        description: this.displaytext
                                                    });
                                                });
                                                args.response.success({
                                                    data: items
                                                });
                                                args.$select.change(function() {
                                                    if(args.context.volumes[0].type == "ROOT") {
                                                        selectedDiskOfferingObj = null;
                                                        return;
                                                    }

                                                    var diskOfferingId = $(this).val();
                                                    $(diskOfferings).each(function() {
                                                        if (this.id == diskOfferingId) {
                                                            selectedDiskOfferingObj = this;
                                                            return false; //break the $.each() loop
                                                        }
                                                    });
                                                    if (selectedDiskOfferingObj == null){
                                                        return;
                                                    }
                                                    var $form = $(this).closest('form');

                                                    var $shrinkok = $form.find('.form-item[rel=shrinkok]');
                                                    //unit of args.context.volumes[0].size is "byte"
                                                    //unit of selectedDiskOfferingObj.disksize is "gigabyte" ("GB"), so transfer it into "byte" by multiply (1024 * 1024 * 1024)
                                                    if (args.context.volumes[0].size > selectedDiskOfferingObj.disksize * (1024 * 1024 * 1024)) { //if original disk size  > new disk size
                                                        $shrinkok.css('display', 'inline-block');
                                                    } else {
                                                        $shrinkok.hide();
                                                    }

                                                    var $newsize = $form.find('.form-item[rel=newsize]');
                                                    if (selectedDiskOfferingObj.iscustomized == true) {
                                                        $newsize.css('display', 'inline-block');
                                                    } else {
                                                        $newsize.hide();
                                                    }

                                                    var $minIops = $form.find('.form-item[rel=minIops]');
                                                    var $maxIops = $form.find('.form-item[rel=maxIops]');
                                                    if (selectedDiskOfferingObj.iscustomizediops == true) {
                                                        $minIops.css('display', 'inline-block');
                                                        $maxIops.css('display', 'inline-block');
                                                    } else {
                                                        $minIops.hide();
                                                        $maxIops.hide();
                                                    }
                                                });
                                            }
                                        },
                                        newsize: {
                                            label: 'label.resize.new.size',
                                            validation: {
                                                required: true,
                                                number: true
                                            }
                                        },
                                        shrinkok: {
                                            label: 'label.resize.shrink.ok',
                                            isBoolean: true,
                                            isChecked: false,
                                            isHidden: true
                                        },
                                        minIops: {
                                            label: 'label.disk.iops.min',
                                            validation: {
                                                required: false,
                                                number: true
                                            },
                                            isHidden: true
                                        },
                                        maxIops: {
                                            label: 'label.disk.iops.max',
                                            validation: {
                                                required: false,
                                                number: true
                                            },
                                            isHidden: true
                                        }
                                    }
                                },
                                action: function(args) {
                                    var array1 = [];
                                    if(args.$form.find('.form-item[rel=shrinkok]').css("display") != "none") {
                                        array1.push("&shrinkok=" + (args.data.shrinkok == "on"));
                                    }

                                    var newDiskOffering = args.data.newdiskoffering;
                                    if (newDiskOffering != null && newDiskOffering.length > 0) {
                                        array1.push("&diskofferingid=" + encodeURIComponent(newDiskOffering));
                                    }
                                    if (selectedDiskOfferingObj.iscustomized == true) {
                                        cloudStack.addNewSizeToCommandUrlParameterArrayIfItIsNotNullAndHigherThanZero(array1, args.data.newsize);
                                    }

                                    var minIops;
                                    var maxIops
                                    if (selectedDiskOfferingObj.iscustomizediops == true) {
                                        minIops = args.data.minIops;
                                        maxIops = args.data.maxIops;
                                    }

                                    if (minIops != null && minIops.length > 0) {
                                        array1.push("&miniops=" + encodeURIComponent(minIops));
                                    }

                                    if (maxIops != null && maxIops.length > 0) {
                                        array1.push("&maxiops=" + encodeURIComponent(maxIops));
                                    }
                                    //if original disk size  > new disk size
                                    if ((args.context.volumes[0].type == "ROOT")
                                    && (args.context.volumes[0].size > (newSize * (1024 * 1024 * 1024)))) {
                                        return args.response.error('message.volume.root.shrink.disk.size');
                                    }


                                    $.ajax({
                                        url: createURL("resizeVolume&id=" + args.context.volumes[0].id + array1.join("")),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var jid = json.resizevolumeresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid,
                                                    getUpdatedItem: function(json) {
                                                        return json.queryasyncjobresultresponse.jobresult.volume;
                                                    },
                                                    getActionFilter: function() {
                                                        return volumeActionfilter;
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
                            details: {
                                title: 'label.details',

                                preFilter: function(args) {
                                    var hiddenFields;
                                    if (isAdmin()) {
                                        hiddenFields = [];
                                    } else {
                                        hiddenFields = ['storage', 'hypervisor', 'virtualsize', 'physicalsize', 'utilization', 'clusterid', 'clustername'];
                                    }
                                    return hiddenFields;
                                },

                                fields: [{
                                    name: {
                                        label: 'label.name',
                                        isEditable: true
                                    }
                                }, {
                                    id: {
                                        label: 'label.id'
                                    },
                                    zonename: {
                                        label: 'label.zone'
                                    },
                                    state: {
                                        label: 'label.state',
                                        pollAgainIfValueIsIn: {
                                            'UploadNotStarted': 1
                                        },
                                        pollAgainFn: function(context) {
                                            var toClearInterval = false;
                                            $.ajax({
                                                url: createURL("listVolumes&id=" + context.volumes[0].id),
                                                dataType: "json",
                                                async: false,
                                                success: function(json) {
                                                    var jsonObj = json.listvolumesresponse.volume[0];
                                                    if (jsonObj.state != context.volumes[0].state) {
                                                        toClearInterval = true; //to clear interval
                                                    }
                                                }
                                            });
                                            return toClearInterval;
                                        }
                                    },
                                    status: {
                                        label: 'label.status'
                                    },
                                    diskofferingdisplaytext: {
                                        label: 'label.disk.offering'
                                    },
                                    type: {
                                        label: 'label.type'
                                    },
                                    storagetype: {
                                        label: 'label.storage.type'
                                    },
                                    provisioningtype: {
                                        label: 'label.disk.provisioningtype'
                                    },
                                    hypervisor: {
                                        label: 'label.hypervisor'
                                    },
                                    size: {
                                        label: 'label.size',
                                        converter: function(args) {
                                            if (args == null || args == 0)
                                                return "";
                                            else
                                                return cloudStack.converters.convertBytes(args);
                                        }
                                    },
                                    clusterid: {
                                        label: 'label.cluster'
                                    },
                                    clustername: {
                                        label: 'label.cluster.name'
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
                                    virtualsize: {
                                        label: 'label.disk.virtualsize',
                                        converter: function(args) {
                                            if (args == null || args == 0)
                                                return "";
                                            else
                                                return cloudStack.converters.convertBytes(args);
                                        }
                                    },
                                    miniops: {
                                        label: 'label.disk.iops.min',
                                        converter: function(args) {
                                            if (args == null || args == 0)
                                                return "";
                                            else
                                                return args;
                                        }
                                    },
                                    maxiops: {
                                        label: 'label.disk.iops.max',
                                        converter: function(args) {
                                            if (args == null || args == 0)
                                                return "";
                                            else
                                                return args;
                                        }
                                    },
                                    virtualmachineid: {
                                        label: 'label.vm.id',
                                        converter: function(args) {
                                            if (args == null)
                                                return _l('state.detached');
                                            else
                                                return args;
                                        }
                                    },
                                    //vmname: { label: 'label.vm.name' },
                                    vmdisplayname: {
                                        label: 'label.vm.display.name'
                                    },
                                    vmstate: {
                                        label: 'label.vm.state'
                                    },
                                    deviceid: {
                                        label: 'label.device.id'
                                    },
                                    storage: {
                                        label: 'label.storage'
                                    },
                                    created: {
                                        label: 'label.created',
                                        converter: cloudStack.converters.toLocalDate
                                    },
                                    domain: {
                                        label: 'label.domain'
                                    },
                                    account: {
                                        label: 'label.account'
                                    }
                                }],

                                tags: cloudStack.api.tags({
                                    resourceType: 'Volume',
                                    contextId: 'volumes'
                                }),


                                dataProvider: function(args) {
                                    $.ajax({
                                        url: createURL("listVolumes&id=" + args.context.volumes[0].id),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var jsonObj = json.listvolumesresponse.volume[0];

                                            $(window).trigger('cloudStack.module.sharedFunctions.addExtraProperties', {
                                                obj: jsonObj,
                                                objType: "Volume"
                                            });

                                            args.response.success({
                                                actionFilter: volumeActionfilter,
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

            /**
             * Snapshots
             */
            snapshots: {
                type: 'select',
                title: 'label.snapshots',
                listView: {
                    id: 'snapshots',
                    label: 'label.snapshots',
                    fields: {
                        volumename: {
                            label: 'label.volume'
                        },
                        name: {
                            label: 'label.name'
                        },
                        intervaltype: {
                            label: 'label.interval.type'
                        },
                        created: {
                            label: 'label.created',
                            converter: cloudStack.converters.toLocalDate
                        },
                        state: {
                            label: 'label.state',
                            indicator: {
                                'BackedUp': 'on',
                                'Destroyed': 'off'
                            }
                        }
                    },

                    advSearchFields: {
                        name: {
                            label: 'label.name'
                        },

                        domainid: {
                            label: 'label.domain',
                            select: function(args) {
                                if (isAdmin() || isDomainAdmin()) {
                                    $.ajax({
                                        url: createURL('listDomains'),
                                        data: {
                                            listAll: true,
                                            details: 'min'
                                        },
                                        success: function(json) {
                                            var array1 = [{
                                                id: '',
                                                description: ''
                                            }];
                                            var domains = json.listdomainsresponse.domain;
                                            if (domains != null && domains.length > 0) {
                                                for (var i = 0; i < domains.length; i++) {
                                                    array1.push({
                                                        id: domains[i].id,
                                                        description: domains[i].path
                                                    });
                                                }
                                            }
                                            array1.sort(function(a, b) {
                                                return a.description.localeCompare(b.description);
                                            });
                                            args.response.success({
                                                data: array1
                                            });
                                        }
                                    });
                                } else {
                                    args.response.success({
                                        data: null
                                    });
                                }
                            },
                            isHidden: function(args) {
                                if (isAdmin() || isDomainAdmin())
                                    return false;
                                else
                                    return true;
                            }
                        },

                        account: {
                            label: 'label.account',
                            isHidden: function(args) {
                                if (isAdmin() || isDomainAdmin())
                                    return false;
                                else
                                    return true;
                            }
                        },
                        tagKey: {
                            label: 'label.tag.key'
                        },
                        tagValue: {
                            label: 'label.tag.value'
                        }
                    },

                    dataProvider: function(args) {
                        var data = {};
                        var instanceVolumeIds = [];
                        listViewDataProvider(args, data);

                        if (args.context != null) {
                            if ("volumes" in args.context) {
                                $.extend(data, {
                                    volumeid: args.context.volumes[0].id
                                });
                            } else if (args.context.instances) {
                                $.ajax({
                                    url: createURL('listVolumes'),
                                    data: {
                                        virtualmachineid: args.context.instances[0].id,
                                        listAll: true
                                    },
                                    async: false,
                                    success: function(json) {
                                        instanceVolumeIds = $.map(json.listvolumesresponse.volume, function(volume) {
                                            return volume.id;
                                        })
                                    }
                                });
                                data.volumeid = instanceVolumeIds.join(',');
                            }
                        }

                        $.ajax({
                            url: createURL('listSnapshots'),
                            data: data,
                            success: function(json) {
                                var items = json.listsnapshotsresponse.snapshot;
                                args.response.success({
                                    actionFilter: snapshotActionfilter,
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
                    },

                    detailView: {
                        name: 'Snapshot detail',
                        actions: {
                            createTemplate: cloudStack.createTemplateFromSnapshotMethod(),

                            createVolume: {
                                label: 'label.action.create.volume',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.confirm.create.volume';
                                    },
                                    notification: function(args) {
                                        return 'label.action.create.volume';
                                    }
                                },
                                createForm: {
                                    title: 'label.action.create.volume',
                                    desc: '',
                                    preFilter: function(args) {
                                        if (g_regionsecondaryenabled == true) {
                                            args.$form.find('.form-item[rel=zoneid]').css('display', 'inline-block');
                                        } else {
                                            args.$form.find('.form-item[rel=zoneid]').hide();
                                        }
                                        if(args.context.snapshots[0].volumetype!='ROOT') {
                                            args.$form.find('.form-item[rel=diskOffering]').hide();
                                        }
                                    },
                                    fields: {
                                        name: {
                                            label: 'label.name',
                                            validation: {
                                                required: true
                                            }
                                        },
                                        zoneid: {
                                            label: 'label.availability.zone',
                                            isHidden: true,
                                            select: function(args) {
                                                $.ajax({
                                                    url: createURL("listZones&available=true"),
                                                    dataType: "json",
                                                    async: true,
                                                    success: function(json) {
                                                        var zoneObjs = json.listzonesresponse.zone;
                                                        var items = [{
                                                            id: '',
                                                            description: ''
                                                        }];
                                                        if (zoneObjs != null) {
                                                            for (i = 0; i < zoneObjs.length; i++) {
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
                                        diskOffering: {
                                            label: 'label.disk.offering',
                                            docID: 'helpVolumeDiskOffering',
                                            select: function(args) {
                                                var snapshotSizeInGB = Math.floor(args.context.snapshots[0].virtualsize/(1024 * 1024 * 1024))
                                                $.ajax({
                                                    url: createURL("listDiskOfferings"),
                                                    dataType: "json",
                                                    async: false,
                                                    success: function(json) {
                                                        diskofferingObjs = json.listdiskofferingsresponse.diskoffering;
                                                        var items = [];
                                                        // Sort offerings list with size and keep custom offerings at end
                                                        for(var i=0;i<diskofferingObjs.length;i++) {
                                                            for(var j=i+1;j<diskofferingObjs.length;j++) {
                                                                if((diskofferingObjs[i].disksize>diskofferingObjs[j].disksize &&
                                                                    diskofferingObjs[j].disksize!=0) ||
                                                                    (diskofferingObjs[i].disksize==0 &&
                                                                        diskofferingObjs[j].disksize!=0)) {
                                                                    var temp = diskofferingObjs[i];
                                                                    diskofferingObjs[i] = diskofferingObjs[j];
                                                                    diskofferingObjs[j] = temp;
                                                                }
                                                            }
                                                        }
                                                        $(diskofferingObjs).each(function() {
                                                            if(this.disksize==0 || this.disksize>=snapshotSizeInGB) {
                                                                items.push({
                                                                    id: this.id,
                                                                    description: this.displaytext
                                                                });
                                                            }
                                                        });
                                                        args.response.success({
                                                            data: items
                                                        });
                                                    }
                                                });

                                                args.$select.change(function() {
                                                    var diskOfferingId = $(this).val();
                                                    selectedDiskOfferingObj = null;
                                                    $(diskofferingObjs).each(function() {
                                                        if (this.id == diskOfferingId) {
                                                            selectedDiskOfferingObj = this;
                                                            return false;
                                                        }
                                                    });

                                                    if (selectedDiskOfferingObj == null) return;

                                                    var $form = $(this).closest('form');
                                                    var $diskSize = $form.find('.form-item[rel=diskSize]');
                                                    if (selectedDiskOfferingObj.iscustomized == true) {
                                                        $diskSize.css('display', 'inline-block');
                                                        $form.find('input[name=diskSize]').val(''+snapshotSizeInGB);
                                                    } else {
                                                        $diskSize.hide();
                                                    }

                                                    var $minIops = $form.find('.form-item[rel=minIops]');
                                                    var $maxIops = $form.find('.form-item[rel=maxIops]');
                                                    if (selectedDiskOfferingObj.iscustomizediops == true) {
                                                        $minIops.css('display', 'inline-block');
                                                        $maxIops.css('display', 'inline-block');
                                                    } else {
                                                        $minIops.hide();
                                                        $maxIops.hide();
                                                    }
                                                });
                                            }
                                        },
                                        diskSize: {
                                            label: 'label.disk.size.gb',
                                            docID: 'helpVolumeSizeGb',
                                            validation: {
                                                required: true,
                                                number: true
                                            },
                                            isHidden: true
                                        },
                                        minIops: {
                                            label: 'label.disk.iops.min',
                                            validation: {
                                                required: false,
                                                number: true
                                            },
                                            isHidden: true
                                        },
                                        maxIops: {
                                            label: 'label.disk.iops.max',
                                            validation: {
                                                required: false,
                                                number: true
                                            },
                                            isHidden: true
                                        }
                                    }
                                },
                                action: function(args) {
                                    var data = {
                                        name: args.data.name,
                                        snapshotid: args.context.snapshots[0].id
                                    };

                                    if (args.$form.find('.form-item[rel=zoneid]').css("display") != "none" && args.data.zoneid != '') {
                                        $.extend(data, {
                                            zoneId: args.data.zoneid
                                        });
                                    }

                                    if (args.$form.find('.form-item[rel=diskOffering]').css("display") != "none") {
                                        if (args.data.diskOffering) {
                                            $.extend(data, {
                                                diskofferingid: args.data.diskOffering
                                            });
                                        }
                                        if (selectedDiskOfferingObj) {
                                            if(selectedDiskOfferingObj.iscustomized == true) {
                                                $.extend(data, {
                                                    size: args.data.diskSize
                                                });
                                            }

                                            if (selectedDiskOfferingObj.iscustomizediops == true) {
                                                if (args.data.minIops != "" && args.data.minIops > 0) {
                                                    $.extend(data, {
                                                        miniops: args.data.minIops
                                                    });
                                                }

                                                if (args.data.maxIops != "" && args.data.maxIops > 0) {
                                                    $.extend(data, {
                                                        maxiops: args.data.maxIops
                                                    });
                                                }
                                            }
                                        }
                                    }

                                    $.ajax({
                                        url: createURL('createVolume'),
                                        data: data,
                                        success: function(json) {
                                            var jid = json.createvolumeresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid,
                                                    getUpdatedItem: function(json) {
                                                        return {}; //nothing in this snapshot needs to be updated
                                                    },
                                                    getActionFilter: function() {
                                                        return snapshotActionfilter;
                                                    }
                                                }
                                            });
                                        },
                                        error: function(json) {
                                            args.response.error(parseXMLHttpResponse(json));
                                        }
                                    });
                                },
                                notification: {
                                    poll: pollAsyncJobResult
                                }
                            },
                            revertSnapshot: {
                                label: 'label.action.revert.snapshot',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.action.revert.snapshot';
                                    },
                                    notification: function(args) {
                                        return 'label.action.revert.snapshot';
                                    }
                                },
                                action: function(args) {
                                    $.ajax({
                                        url: createURL("revertSnapshot&id="+args.context.snapshots[0].id),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var jid = json.revertsnapshotresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid
                                                }
                                            });
                                        }
                                    });
                                },
                                notification: {
                                    poll: pollAsyncJobResult
                                }
                            },

                            remove: {
                                label: 'label.action.delete.snapshot',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.action.delete.snapshot';
                                    },
                                    notification: function(args) {
                                        return 'label.action.delete.snapshot';
                                    }
                                },
                                action: function(args) {
                                    $.ajax({
                                        url: createURL("deleteSnapshot&id=" + args.context.snapshots[0].id),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var jid = json.deletesnapshotresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid
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
                            details: {
                                title: 'label.details',
                                fields: [{
                                    name: {
                                        label: 'label.name'
                                    }
                                }, {
                                    id: {
                                        label: 'label.id'
                                    },
                                    volumename: {
                                        label: 'label.volume.name'
                                    },
                                    state: {
                                        label: 'label.state'
                                    },
                                    intervaltype: {
                                        label: 'label.interval.type'
                                    },
                                    domain: {
                                        label: 'label.domain'
                                    },
                                    account: {
                                        label: 'label.account'
                                    },
                                    created: {
                                        label: 'label.created',
                                        converter: cloudStack.converters.toLocalDate
                                    }
                                }],

                                tags: cloudStack.api.tags({
                                    resourceType: 'Snapshot',
                                    contextId: 'snapshots'
                                }),

                                dataProvider: function(args) {
                                    $.ajax({
                                        url: createURL("listSnapshots&id=" + args.context.snapshots[0].id),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var jsonObj = json.listsnapshotsresponse.snapshot[0];
                                            args.response.success({
                                                actionFilter: snapshotActionfilter,
                                                data: jsonObj
                                            });
                                        }
                                    });
                                }
                            }
                        }
                    }
                },
            },

            /**
             * VM Snapshots
             */
            vmsnapshots: {
                type: 'select',
                title: 'label.vmsnapshot',
                listView: {
                    id: 'vmsnapshots',
                    isMaximized: true,
                    fields: {
                        displayname: {
                            label: 'label.name'
                        },
                        state: {
                            label: 'label.state',
                            indicator: {
                                'Ready': 'on',
                                'Error': 'off'
                            }
                        },
                        type: {
                            label: 'label.vmsnapshot.type'
                        },
                        current: {
                            label: 'label.vmsnapshot.current',
                            converter: cloudStack.converters.toBooleanText
                        },
                        parentName: {
                            label: 'label.vmsnapshot.parentname'
                        },
                        created: {
                            label: 'label.date',
                            converter: cloudStack.converters.toLocalDate
                        }
                    },

                    advSearchFields: {
                        name: {
                            label: 'label.name'
                        },

                        domainid: {
                            label: 'label.domain',
                            select: function(args) {
                                if (isAdmin() || isDomainAdmin()) {
                                    $.ajax({
                                        url: createURL('listDomains'),
                                        data: {
                                            listAll: true,
                                            details: 'min'
                                        },
                                        success: function(json) {
                                            var array1 = [{
                                                id: '',
                                                description: ''
                                            }];
                                            var domains = json.listdomainsresponse.domain;
                                            if (domains != null && domains.length > 0) {
                                                for (var i = 0; i < domains.length; i++) {
                                                    array1.push({
                                                        id: domains[i].id,
                                                        description: domains[i].path
                                                    });
                                                }
                                            }
                                            array1.sort(function(a, b) {
                                                return a.description.localeCompare(b.description);
                                            });
                                            args.response.success({
                                                data: array1
                                            });
                                        }
                                    });
                                } else {
                                    args.response.success({
                                        data: null
                                    });
                                }
                            },
                            isHidden: function(args) {
                                if (isAdmin() || isDomainAdmin())
                                    return false;
                                else
                                    return true;
                            }
                        },

                        account: {
                            label: 'label.account',
                            isHidden: function(args) {
                                if (isAdmin() || isDomainAdmin())
                                    return false;
                                else
                                    return true;
                            }
                        },
                        tagKey: {
                            label: 'label.tag.key'
                        },
                        tagValue: {
                            label: 'label.tag.value'
                        }
                    },

                    dataProvider: function(args) {
                        var data = {
                            listAll: true
                        };
                                listViewDataProvider(args, data);

                        if (args.context != null) {
                            if ("instances" in args.context) {
                                        $.extend(data, {
                                            virtualMachineId: args.context.instances[0].id
                                        });
                            }
                        }
                        $.ajax({
                            url: createURL('listVMSnapshot'),
                                    data: data,
                            dataType: "json",
                            async: true,
                            success: function(json) {
                                var jsonObj;
                                jsonObj = json.listvmsnapshotresponse.vmSnapshot;
                                args.response.success({
                                            actionFilter: vmSnapshotActionfilter,
                                    data: jsonObj
                                });
                            }
                        });
                    },
                    //dataProvider end
                    detailView: {
                        tabs: {
                            details: {
                                title: 'label.details',
                                fields: {
                                    id: {
                                        label: 'label.id'
                                    },
                                    name: {
                                        label: 'label.name'
                                    },
                                    displayname: {
                                        label: 'label.display.name'
                                    },
                                    type: {
                                        label: 'label.vmsnapshot.type'
                                    },
                                    description: {
                                        label: 'label.description'
                                    },
                                    state: {
                                        label: 'label.state',
                                        indicator: {
                                            'Ready': 'on',
                                            'Error': 'off'
                                        }
                                    },
                                    current: {
                                        label: 'label.vmsnapshot.current',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    parentName: {
                                        label: 'label.vmsnapshot.parentname'
                                    },
                                            domain: {
                                                label: 'label.domain'
                                            },
                                            account: {
                                                label: 'label.account'
                                            },
                                            virtualmachineid: {
                                                label: 'label.vm.id'
                                            },
                                    created: {
                                        label: 'label.date',
                                        converter: cloudStack.converters.toLocalDate
                                    }
                                },
                                dataProvider: function(args) {
                                    $.ajax({
                                        url: createURL("listVMSnapshot&listAll=true&vmsnapshotid=" + args.context.vmsnapshots[0].id),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var jsonObj;
                                            jsonObj = json.listvmsnapshotresponse.vmSnapshot[0];
                                            args.response.success({
                                                        actionFilter: vmSnapshotActionfilter,
                                                data: jsonObj
                                            });
                                        }
                                    });
                                },
                                tags: cloudStack.api.tags({
                                    resourceType: 'VMSnapshot',
                                    contextId: 'vmsnapshots'
                                })
                            }
                        },
                        actions: {
                            //delete a snapshot
                            remove: {
                                label: 'label.action.vmsnapshot.delete',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.action.vmsnapshot.delete';
                                    },
                                    notification: function(args) {
                                        return 'label.action.vmsnapshot.delete';
                                    }
                                },
                                action: function(args) {
                                    $.ajax({
                                        url: createURL("deleteVMSnapshot&vmsnapshotid=" + args.context.vmsnapshots[0].id),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var jid = json.deletevmsnapshotresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid
                                                }
                                            });
                                        }
                                    });
                                },
                                notification: {
                                    poll: pollAsyncJobResult
                                }
                            },
                            revertToVMSnapshot: {
                                label: 'label.action.vmsnapshot.revert',
                                messages: {
                                    confirm: function(args) {
                                        return 'label.action.vmsnapshot.revert';
                                    },
                                    notification: function(args) {
                                        return 'message.action.vmsnapshot.revert';
                                    }
                                },
                                action: function(args) {
                                    $.ajax({
                                        url: createURL("revertToVMSnapshot&vmsnapshotid=" + args.context.vmsnapshots[0].id),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var jid = json.reverttovmsnapshotresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid
                                                }
                                            });
                                        }
                                    });

                                },
                                notification: {
                                    poll: pollAsyncJobResult
                                }
                            },
                            takeSnapshot: {
                                label: 'Create Snapshot From VM Snapshot',
                                messages: {
                                    confirm: function(args) {
                                        return 'Please confirm that you want to create a volume snapshot from the vm snapshot.';
                                    },
                                    notification: function(args) {
                                        return 'Volume snapshot is created from vm snapshot';
                                    }
                                },
                                createForm: {
                                    title: 'label.action.take.snapshot',
                                    desc: 'message.action.take.snapshot',
                                    fields: {
                                        name: {
                                            label: 'label.name',
                                        },
                                                volume: {
                                                    label: 'label.volume',
                                                    validation: {
                                                        required: true
                                                    },
                                                    select: function(args) {
                                                        $.ajax({
                                                            url: createURL("listVolumes&virtualMachineId=" + args.context.vmsnapshots[0].virtualmachineid),
                                                            dataType: "json",
                                                            async: true,
                                                            success: function(json) {
                                                                var volumes = json.listvolumesresponse.volume;
                                                                var items = [];
                                                                $(volumes).each(function() {
                                                                    items.push({
                                                                        id: this.id,
                                                                        description: this.name
                                                                    });
                                                                });
                                                                args.response.success({
                                                                    data: items
                                                                });

                                                            }
                                                        });
                                                    }
                                                }
                                    }
                                },
                                action: function(args) {
                                    var data = {
                                                volumeid: args.data.volume,
                                        vmsnapshotid: args.context.vmsnapshots[0].id
                                    };
                                    if (args.data.name != null && args.data.name.length > 0) {
                                        $.extend(data, {
                                            name: args.data.name
                                        });
                                    }
                                    $.ajax({
                                        url: createURL("createSnapshotFromVMSnapshot"),
                                        data: data,
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var jid = json.createsnapshotfromvmsnapshotresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid
                                                }
                                            });
                                        }
                                    });

                                },
                                notification: {
                                    poll: pollAsyncJobResult
                                }
                            }
                        }
                    }
                    //detailview end
                }
            }
        }
    };


    var volumeActionfilter = cloudStack.actionFilter.volumeActionfilter = function(args) {
        var jsonObj = args.context.item;
        var allowedActions = [];


        if (jsonObj.state == 'Destroyed' || jsonObj.state == 'Migrating' || jsonObj.state == 'Uploading') {
            return [];
        }
        if (jsonObj.state == 'UploadError') {
            return ["remove"];
        }

        if (jsonObj.hypervisor != "Ovm" && jsonObj.state == "Ready") {
            if (jsonObj.hypervisor == 'KVM') {
                if (jsonObj.vmstate == 'Running') {
                    if (g_kvmsnapshotenabled == true) { //"kvm.snapshot.enabled" flag should be taken to account only when snapshot is being created for Running vm (CLOUDSTACK-4428)
                        allowedActions.push("takeSnapshot");
                        allowedActions.push("recurringSnapshot");
                    }
                } else {
                    allowedActions.push("takeSnapshot");
                    allowedActions.push("recurringSnapshot");
                }
            } else {
                allowedActions.push("takeSnapshot");
                allowedActions.push("recurringSnapshot");
            }
        }

        if ((jsonObj.type == "DATADISK"  || jsonObj.type == "ROOT") && (jsonObj.state == "Ready" || jsonObj.state == "Allocated")) {
            allowedActions.push("resize");
        }

        if (jsonObj.state != "Allocated") {
            if ((jsonObj.vmstate == "Stopped" || jsonObj.virtualmachineid == null) && jsonObj.state == "Ready") {
                allowedActions.push("downloadVolume");
            }
        }



        if (jsonObj.type == "ROOT" || jsonObj.type == "DATADISK") {
            if (jsonObj.state == "Ready" && isAdmin() && jsonObj.virtualmachineid != null) {
                allowedActions.push("migrateVolume");
            }
        }

        if (jsonObj.state != "Creating") {
            if (jsonObj.type == "ROOT") {
                if (jsonObj.vmstate == "Stopped") {
                    allowedActions.push("createTemplate");
                }
            } else { //jsonObj.type == "DATADISK"
                if (jsonObj.virtualmachineid != null) {
                    if (jsonObj.vmstate == "Running" || jsonObj.vmstate == "Stopped" || jsonObj.vmstate == "Destroyed") {
                        allowedActions.push("detachDisk");
                    }
                } else { // Disk not attached
                    allowedActions.push("remove");
                    if (jsonObj.state == "Ready" && isAdmin()) {
                        allowedActions.push("migrateToAnotherStorage");
                    }
                    allowedActions.push("attachDisk");
                }
            }
        }

        return allowedActions;
    };

    var snapshotActionfilter = cloudStack.actionFilter.snapshotActionfilter = function(args) {
        var jsonObj = args.context.item;

        if (jsonObj.state == 'Destroyed') {
            return [];
        }

        var allowedActions = [];
        if (jsonObj.state == "BackedUp") {
            allowedActions.push("createTemplate");
            allowedActions.push("createVolume");

            if (jsonObj.revertable) {
                allowedActions.push("revertSnapshot");
            }
        }
        allowedActions.push("remove");

        return allowedActions;
    };

    var vmSnapshotActionfilter = cloudStack.actionFilter.vmSnapshotActionfilter = function(args) {
        var jsonObj = args.context.item;

        if (jsonObj.state == 'Error') {
            return ["remove"];
        }

        var allowedActions = [];
        if (jsonObj.state == "Ready") {
            allowedActions.push("remove");
            allowedActions.push("revertToVMSnapshot");

            if (args && args.context && args.context.instances && args.context.instances[0].hypervisor && args.context.instances[0].hypervisor === "KVM") {
                allowedActions.push("takeSnapshot");
            }
        }

        return allowedActions;
    }

})(cloudStack);
