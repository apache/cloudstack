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
    var ostypeObjs;
    var previousCollection = [];
    var previousFilterType = null;

    cloudStack.sections.templates = {
        title: 'label.menu.templates',
        id: 'templates',
        sectionSelect: {
            label: 'label.select-view'
        },
        sections: {
            templates: {
                type: 'select',
                title: 'label.menu.templates',
                listView: {
                    id: 'templates',
                    label: 'label.menu.templates',
                    filters: {
                        all: {
                            preFilter: function(args) {
                                if (isAdmin()|| isDomainAdmin()) //"listTemplates&templatefilter=all" only for root-admin and domain-admin. Domain-admin is able to see all templates in his domain.
                                    return true;
                                else
                                    return false;
                            },
                            label: 'ui.listView.filters.all'
                        },
                        mine: {
                            label: 'ui.listView.filters.mine'
                        },
                        shared: {
                            label: 'label.shared'
                        },
                        featured: {
                            label: 'label.featured'
                        },
                        community: {
                            label: 'label.community'
                        }
                    },
                    preFilter: function() {
                        if (isAdmin()||isDomainAdmin()) {
                            return []
                        }
                        return ['account']
                    },
                    fields: {
                        name: {
                            label: 'label.name'
                        },
                        hypervisor: {
                            label: 'label.hypervisor'
                        },
                        ostypename: {
                            label: 'label.os.type'
                        },
                        account: {
                            label: 'label.account'
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
                        tagKey: {
                            label: 'label.tag.key'
                        },
                        tagValue: {
                            label: 'label.tag.value'
                        }
                    },

                    reorder: cloudStack.api.actions.sort('updateTemplate', 'templates'),
                    actions: {
                        add: {
                            label: 'label.add',
                            messages: {
                                notification: function(args) {
                                    return 'label.action.register.template';
                                }
                            },
                            createForm: {
                                title: 'label.action.register.template',
                                docID: 'helpNetworkOfferingName',
                                preFilter: cloudStack.preFilter.createTemplate,
                                fields: {
                                    url: {
                                        label: 'label.url',
                                        docID: 'helpRegisterTemplateURL',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    name: {
                                        label: 'label.name',
                                        docID: 'helpRegisterTemplateName',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    description: {
                                        label: 'label.description',
                                        docID: 'helpRegisterTemplateDescription',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    zone: {
                                        label: 'label.zone',
                                        docID: 'helpRegisterTemplateZone',
                                        isMultiple: true,
                                        validation: {
                                            allzonesonly: true
                                        },
                                        select: function(args) {
                                            if(g_regionsecondaryenabled == true) {
                                                args.response.success({
                                                    data: [{
                                                        id: -1,
                                                        description: "All Zones"
                                                    }]
                                                });
                                            } else {
                                                $.ajax({
                                                    url: createURL("listZones&available=true"),
                                                    dataType: "json",
                                                    async: true,
                                                    success: function(json) {
                                                        var zoneObjs = [];
                                                        var items = json.listzonesresponse.zone;
                                                        if (items != null) {
                                                            for (var i = 0; i < items.length; i++) {
                                                                zoneObjs.push({
                                                                    id: items[i].id,
                                                                    description: items[i].name
                                                                });
                                                            }
                                                        }
                                                        if (isAdmin() && !(cloudStack.context.projects && cloudStack.context.projects[0])) {
                                                            zoneObjs.unshift({
                                                                id: -1,
                                                                description: "All Zones"
                                                            });
                                                        }
                                                        args.response.success({
                                                            data: zoneObjs
                                                        });
                                                    }
                                                });
                                            }
                                        }
                                    },
                                    hypervisor: {
                                        label: 'label.hypervisor',
                                        docID: 'helpRegisterTemplateHypervisor',
                                        dependsOn: 'zone',
                                        select: function(args) {
                                            if (args.zone == null)
                                                return;
                                            // We want only distinct Hypervisor entries to be visible to the user
                                            var items = [];
                                            var distinctHVNames = [];
                                            var length = 1;
                                            // When only one zone is selected, args.zone is NOT an array.
                                            if (Object.prototype.toString.call( args.zone ) === '[object Array]')
                                                length = args.zone.length;
                                            for (var index = 0; index < length; index++)
                                            {
                                                var zoneId;
                                                if (length == 1)
                                                        zoneId = args.zone;
                                                else
                                                        zoneId = args.zone[index];

                                                var apiCmd;
                                                if (zoneId == -1) { //All Zones
                                                    apiCmd = "listHypervisors";
                                                }
                                                else {
                                                    apiCmd = "listHypervisors&zoneid=" + zoneId;
                                                }

                                                $.ajax({
                                                    url: createURL(apiCmd),
                                                    dataType: "json",
                                                    async: false,
                                                    success: function(json) {
                                                        var hypervisorObjs = json.listhypervisorsresponse.hypervisor;

                                                        $(hypervisorObjs).each(function() {
                                                        // Only if this hypervisor isn't already part of this
                                                        // list, then add to the drop down
                                                           if (distinctHVNames.indexOf(this.name) < 0 ){
                                                               distinctHVNames.push(this.name);
                                                               items.push({
                                                                   id: this.name,
                                                                   description: this.name
                                                               });
                                                           }
                                                        });
                                                    }
                                                });
                                            }
                                            args.$select.change(function() {
                                                var $form = $(this).closest('form');
                                                if ($(this).val() == "VMware") {
                                                    $form.find('.form-item[rel=rootDiskControllerType]').css('display', 'inline-block');
                                                    $form.find('.form-item[rel=nicAdapterType]').css('display', 'inline-block');
                                                    $form.find('.form-item[rel=keyboardType]').css('display', 'inline-block');
                                                    $form.find('.form-item[rel=xenserverToolsVersion61plus]').hide();
                                                    $form.find('.form-item[rel=rootDiskControllerTypeKVM]').hide();
                                                    $form.find('.form-item[rel=directdownload]').hide();
                                                    $form.find('.form-item[rel=requireshvm]').hide();
                                                } else if ($(this).val() == "XenServer") {
                                                    $form.find('.form-item[rel=rootDiskControllerType]').hide();
                                                    $form.find('.form-item[rel=nicAdapterType]').hide();
                                                    $form.find('.form-item[rel=keyboardType]').hide();
                                                    $form.find('.form-item[rel=rootDiskControllerTypeKVM]').hide();
                                                    $form.find('.form-item[rel=directdownload]').hide();
                                                    $form.find('.form-item[rel=requireshvm]').css('display', 'inline-block');

                                                    if (isAdmin()) {
                                                        $form.find('.form-item[rel=xenserverToolsVersion61plus]').css('display', 'inline-block');
                                                    }
                                                } else if ($(this).val() == "KVM") {
                                                    $form.find('.form-item[rel=rootDiskControllerType]').hide();
                                                    $form.find('.form-item[rel=nicAdapterType]').hide();
                                                    $form.find('.form-item[rel=keyboardType]').hide();
                                                    $form.find('.form-item[rel=xenserverToolsVersion61plus]').hide();
                                                    $form.find('.form-item[rel=rootDiskControllerTypeKVM]').css('display', 'inline-block');
                                                    $('#label_root_disk_controller').prop('selectedIndex', 2);
                                                    $form.find('.form-item[rel=requireshvm]').css('display', 'inline-block');
                                                    if (isAdmin()) {
                                                      $form.find('.form-item[rel=directdownload]').css('display', 'inline-block');
                                                    }
                                                } else {
                                                    $form.find('.form-item[rel=rootDiskControllerType]').hide();
                                                    $form.find('.form-item[rel=nicAdapterType]').hide();
                                                    $form.find('.form-item[rel=keyboardType]').hide();
                                                    $form.find('.form-item[rel=xenserverToolsVersion61plus]').hide();
                                                    $form.find('.form-item[rel=rootDiskControllerTypeKVM]').hide();
                                                    $form.find('.form-item[rel=directdownload]').hide();
                                                    $form.find('.form-item[rel=requireshvm]').css('display', 'inline-block');
                                                }
                                            });

                                            items.push({
                                                id: "Any",
                                                description: "Any"
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                            args.$select.trigger('change');

                                        }
                                    },

                                    // fields for hypervisor == XenServer (starts here)
                                    xenserverToolsVersion61plus: {
                                        label: 'label.xenserver.tools.version.61.plus',
                                        isBoolean: true,
                                        isChecked: function (args) {
                                             var b = true;
                                            if (isAdmin()) {
                                                $.ajax({
                                                    url: createURL('listConfigurations'),
                                                    data: {
                                                        name: 'xenserver.pvdriver.version'
                                                    },
                                                    async: false,
                                                    success: function (json) {
                                                        if (json.listconfigurationsresponse.configuration != null && json.listconfigurationsresponse.configuration[0].value != 'xenserver61') {
                                                            b = false;
                                                        }
                                                    }
                                                });
                                            }
                                            return b;
                                        },
                                        isHidden: true
                                    },
                                    // fields for hypervisor == XenServer (ends here)

                                    // fields for hypervisor == "KVM" (starts here)
                                    // Direct Download
                                    directdownload : {
                                        label: 'label.direct.download',
                                        docID: 'helpRegisterTemplateDirectDownload',
                                        isBoolean: true,
                                        dependsOn: 'hypervisor',
                                        isHidden: true
                                    },
                                    checksum: {
                                        label: 'label.checksum',
                                        dependsOn: 'directdownload',
                                        isHidden: true
                                    },
                                    // Direct Download - End
                                    rootDiskControllerTypeKVM: {
                                        label: 'label.root.disk.controller',
                                        isHidden: true,
                                        select: function(args) {
                                            var items = []
                                            items.push({
                                                id: "",
                                                description: ""
                                            });
                                            items.push({
                                                id: "ide",
                                                description: "ide"
                                            });
                                            items.push({
                                                id: "osdefault",
                                                description: "osdefault"
                                            });
                                            items.push({
                                                id: "scsi",
                                                description: "virtio-scsi"
                                            });
                                            items.push({
                                                id: "virtio",
                                                description: "virtio"
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    // fields for hypervisor == "KVM" (ends here)

                                    // fields for hypervisor == "VMware" (starts here)
                                    rootDiskControllerType: {
                                        label: 'label.root.disk.controller',
                                        isHidden: true,
                                        select: function(args) {
                                            var items = []
                                            items.push({
                                                id: "",
                                                description: ""
                                            });
                                            items.push({
                                                id: "scsi",
                                                description: "scsi"
                                            });
                                            items.push({
                                                id: "ide",
                                                description: "ide"
                                            });
                                            items.push({
                                                id: "osdefault",
                                                description: "osdefault"
                                            });
                                            items.push({
                                                id: "pvscsi",
                                                description: "pvscsi"
                                            });
                                            items.push({
                                                id: "lsilogic",
                                                description: "lsilogic"
                                            });
                                            items.push({
                                                id: "lsisas1068",
                                                description: "lsilogicsas"
                                            });
                                            items.push({
                                                id: "buslogic",
                                                description: "buslogic"
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    nicAdapterType: {
                                        label: 'label.nic.adapter.type',
                                        isHidden: true,
                                        select: function(args) {
                                            var items = []
                                            items.push({
                                                id: "",
                                                description: ""
                                            });
                                            items.push({
                                                id: "E1000",
                                                description: "E1000"
                                            });
                                            items.push({
                                                id: "PCNet32",
                                                description: "PCNet32"
                                            });
                                            items.push({
                                                id: "Vmxnet2",
                                                description: "Vmxnet2"
                                            });
                                            items.push({
                                                id: "Vmxnet3",
                                                description: "Vmxnet3"
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    keyboardType: {
                                        label: 'label.keyboard.type',
                                        isHidden: true,
                                        select: function(args) {
                                            var items = []
                                            items.push({
                                                id: "",
                                                description: ""
                                            });
                                            for (var key in cloudStackOptions.keyboardOptions) {
                                                items.push({
                                                    id: key,
                                                    description: _l(cloudStackOptions.keyboardOptions[key])
                                                });
                                            }
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    // fields for hypervisor == "VMware" (ends here)

                                    format: {
                                        label: 'label.format',
                                        docID: 'helpRegisterTemplateFormat',
                                        dependsOn: 'hypervisor',
                                        select: function(args) {
                                            var items = [];
                                            if (args.hypervisor == "XenServer") {
                                                //formatSelect.append("<option value='VHD'>VHD</option>");
                                                items.push({
                                                    id: 'VHD',
                                                    description: 'VHD'
                                                });
                                            } else if (args.hypervisor == "VMware") {
                                                //formatSelect.append("<option value='OVA'>OVA</option>");
                                                items.push({
                                                    id: 'OVA',
                                                    description: 'OVA'
                                                });
                                            } else if (args.hypervisor == "KVM") {
                                                //formatSelect.append("<option value='QCOW2'>QCOW2</option>");
                                                items.push({
                                                    id: 'QCOW2',
                                                    description: 'QCOW2'
                                                });
                                                items.push({
                                                    id: 'RAW',
                                                    description: 'RAW'
                                                });
                                                items.push({
                                                    id: 'VHD',
                                                    description: 'VHD'
                                                });
                                                items.push({
                                                    id: 'VMDK',
                                                    description: 'VMDK'
                                                });
                                            } else if (args.hypervisor == "BareMetal") {
                                                //formatSelect.append("<option value='BareMetal'>BareMetal</option>");
                                                items.push({
                                                    id: 'BareMetal',
                                                    description: 'BareMetal'
                                                });
                                            } else if (args.hypervisor == "Ovm") {
                                                //formatSelect.append("<option value='RAW'>RAW</option>");
                                                items.push({
                                                    id: 'RAW',
                                                    description: 'RAW'
                                                });
                                            } else if (args.hypervisor == "LXC") {
                                                //formatSelect.append("<option value='TAR'>TAR</option>");
                                                items.push({
                                                    id: 'TAR',
                                                    description: 'TAR'
                                                });
                                            } else if (args.hypervisor == "Hyperv") {
                                                items.push({
                                                    id: 'VHD',
                                                    description: 'VHD'
                                                });
                                                items.push({
                                                    id: 'VHDX',
                                                    description: 'VHDX'
                                                });
                                            }
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },

                                    osTypeId: {
                                        label: 'label.os.type',
                                        docID: 'helpRegisterTemplateOSType',
                                        select: function(args) {
                                            $.ajax({
                                                url: createURL("listOsTypes"),
                                                dataType: "json",
                                                async: true,
                                                success: function(json) {
                                                    var ostypeObjs = json.listostypesresponse.ostype;
                                                    args.response.success({
                                                        data: ostypeObjs
                                                    });
                                                }
                                            });
                                        }
                                    },

                                    isExtractable: {
                                        label: "label.extractable",
                                        docID: 'helpRegisterTemplateExtractable',
                                        isBoolean: true
                                    },

                                    isPasswordEnabled: {
                                        label: "label.password.enabled",
                                        docID: 'helpRegisterTemplatePasswordEnabled',
                                        isBoolean: true
                                    },

                                    isdynamicallyscalable: {
                                        label: "label.dynamically.scalable",
                                        docID: 'helpRegisterTemplateDynamicallyScalable',
                                        isBoolean: true
                                    },

                                    isPublic: {
                                        label: "label.public",
                                        docID: 'helpRegisterTemplatePublic',
                                        isBoolean: true,
                                        isHidden: true
                                    },

                                    isFeatured: {
                                        label: "label.featured",
                                        docID: 'helpRegisterTemplateFeatured',
                                        isBoolean: true,
                                        isHidden: true
                                    },
                                    isrouting: {
                                        label: 'label.routing',
                                        docID: 'helpRegisterTemplateRouting',
                                        isBoolean: true,
                                        isHidden: true
                                    },
                                    requireshvm: {
                                        label: 'label.hvm',
                                        docID: 'helpRegisterTemplateHvm',
                                        isBoolean: true,
                                        isHidden: false,
                                        isChecked: true
                                    }
                                }
                            },

                            action: function(args) {
                                var zones = "";
                                if (Object.prototype.toString.call( args.data.zone ) === '[object Array]'){
                                    zones = args.data.zone.join(",");
                                }
                                else
                                    zones = args.data.zone;
                                var data = {
                                    name: args.data.name,
                                    displayText: args.data.description,
                                    url: args.data.url,
                                    zoneids: zones,
                                    format: args.data.format,
                                    isextractable: (args.data.isExtractable == "on"),
                                    passwordEnabled: (args.data.isPasswordEnabled == "on"),
                                    isdynamicallyscalable: (args.data.isdynamicallyscalable == "on"),
                                    osTypeId: args.data.osTypeId,
                                    hypervisor: args.data.hypervisor
                                };

                                if (args.$form.find('.form-item[rel=isPublic]').css("display") != "none") {
                                    $.extend(data, {
                                        ispublic: (args.data.isPublic == "on")
                                    });
                                }

                                if (args.$form.find('.form-item[rel=requireshvm]').css("display") != "none") {
                                    $.extend(data, {
                                        requireshvm: (args.data.requireshvm == "on")
                                    });
                                }

                                if (args.$form.find('.form-item[rel=isFeatured]').css("display") != "none") {
                                    $.extend(data, {
                                        isfeatured: (args.data.isFeatured == "on")
                                    });
                                }

                                if (args.$form.find('.form-item[rel=isrouting]').is(':visible')) {
                                    $.extend(data, {
                                        isrouting: (args.data.isrouting === 'on')
                                    });
                                }

                                // for hypervisor == XenServer (starts here)
                                if (args.$form.find('.form-item[rel=xenserverToolsVersion61plus]').css("display") != "none") {
                                    $.extend(data, {
                                        'details[0].hypervisortoolsversion': (args.data.xenserverToolsVersion61plus == "on") ? "xenserver61" : "xenserver56"
                                    });
                                }
                                // for hypervisor == XenServer (ends here)

                                // for hypervisor == KVM (starts here)
                                if (args.$form.find('.form-item[rel=rootDiskControllerTypeKVM]').css("display") != "none" && args.data.rootDiskControllerTypeKVM != "") {
                                    $.extend(data, {
                                        'details[0].rootDiskController': args.data.rootDiskControllerTypeKVM
                                    });
                                }

                                if (args.$form.find('.form-item[rel=directdownload]').css("display") != "none" && args.data.directdownload != "") {
                                    $.extend(data, {
                                        'directdownload': (args.data.directdownload == "on") ? "true" : "false",
                                        'checksum': args.data.checksum
                                    });
                                }
                                // for hypervisor == KVM (ends here)

                                // for hypervisor == VMware (starts here)
                                if (args.$form.find('.form-item[rel=rootDiskControllerType]').css("display") != "none" && args.data.rootDiskControllerType != "") {
                                    $.extend(data, {
                                        'details[0].rootDiskController': args.data.rootDiskControllerType
                                    });
                                }
                                if (args.$form.find('.form-item[rel=nicAdapterType]').css("display") != "none" && args.data.nicAdapterType != "") {
                                    $.extend(data, {
                                        'details[0].nicAdapter': args.data.nicAdapterType
                                    });
                                }
                                if (args.$form.find('.form-item[rel=keyboardType]').css("display") != "none" && args.data.keyboardType != "") {
                                    $.extend(data, {
                                        'details[0].keyboard': args.data.keyboardType
                                    });
                                }
                                // for hypervisor == VMware (ends here)

                                $.ajax({
                                    url: createURL('registerTemplate'),
                                    data: data,
                                    success: function(json) {
                                        var items = json.registertemplateresponse.template; //items might have more than one array element if it's create templates for all zones.
                                        args.response.success({
                                            data: items[0]
                                        });
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

                        uploadTemplateFromLocal: {
                            isHeader: true,
                            label: 'label.upload.from.local',
                            messages: {
                                notification: function(args) {
                                    return 'label.upload.template.from.local';
                                }
                            },
                            createForm: {
                                title: 'label.upload.template.from.local',
                                preFilter: cloudStack.preFilter.createTemplate,
                                fileUpload: {
                                    getURL: function(args) {
                                        args.data = args.formData;

                                        var data = {
                                            name: args.data.name,
                                            displayText: args.data.description,
                                            zoneid: args.data.zone,
                                            format: args.data.format,
                                            isextractable: (args.data.isExtractable == "on"),
                                            passwordEnabled: (args.data.isPasswordEnabled == "on"),
                                            isdynamicallyscalable: (args.data.isdynamicallyscalable == "on"),
                                            osTypeId: args.data.osTypeId,
                                            hypervisor: args.data.hypervisor
                                        };

                                        // for hypervisor == XenServer (starts here)
                                        if (args.$form.find('.form-item[rel=xenserverToolsVersion61plus]').css("display") != "none") {
                                            $.extend(data, {
                                                'details[0].hypervisortoolsversion': (args.data.xenserverToolsVersion61plus == "on") ? "xenserver61" : "xenserver56"
                                            });
                                        }
                                        // for hypervisor == XenServer (ends here)

                                        // for hypervisor == KVM (starts here)
                                        if (args.$form.find('.form-item[rel=rootDiskControllerTypeKVM]').css("display") != "none" && args.data.rootDiskControllerTypeKVM != "") {
                                            $.extend(data, {
                                                'details[0].rootDiskController': args.data.rootDiskControllerTypeKVM
                                            });
                                        }
                                        // for hypervisor == KVM (ends here)

                                        // for hypervisor == VMware (starts here)
                                        if (args.$form.find('.form-item[rel=rootDiskControllerType]').css("display") != "none" && args.data.rootDiskControllerType != "") {
                                            $.extend(data, {
                                                'details[0].rootDiskController': args.data.rootDiskControllerType
                                            });
                                        }
                                        if (args.$form.find('.form-item[rel=nicAdapterType]').css("display") != "none" && args.data.nicAdapterType != "") {
                                            $.extend(data, {
                                                'details[0].nicAdapter': args.data.nicAdapterType
                                            });
                                        }
                                        if (args.$form.find('.form-item[rel=keyboardType]').css("display") != "none" && args.data.keyboardType != "") {
                                            $.extend(data, {
                                                'details[0].keyboard': args.data.keyboardType
                                            });
                                        }
                                        // for hypervisor == VMware (ends here)

                                        if (args.$form.find('.form-item[rel=isPublic]').css("display") != "none") {
                                            $.extend(data, {
                                                ispublic: (args.data.isPublic == "on")
                                            });
                                        }

                                        if (args.$form.find('.form-item[rel=requireshvm]').css("display") != "none") {
                                            $.extend(data, {
                                                requireshvm: (args.data.requireshvm == "on")
                                            });
                                        }

                                        if (args.$form.find('.form-item[rel=isFeatured]').css("display") != "none") {
                                            $.extend(data, {
                                                isfeatured: (args.data.isFeatured == "on")
                                            });
                                        }

                                        if (args.$form.find('.form-item[rel=isrouting]').is(':visible')) {
                                            $.extend(data, {
                                                isrouting: (args.data.isrouting === 'on')
                                            });
                                        }

                                        $.ajax({
                                            url: createURL('getUploadParamsForTemplate'),
                                            data: data,
                                            async: false,
                                            success: function(json) {
                                                var uploadparams = json.postuploadtemplateresponse.getuploadparams;
                                                var templateId = uploadparams.id;

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
                                                message: "This template file has been uploaded. Please check its status at Templates menu > " + args.data.name + " > Zones tab > click a zone > Status field and Ready field."
                                            });
                                            args.response.success();
                                        }
                                    }
                                },
                                fields: {
                                    templateFileUpload: {
                                        label: 'label.local.file',
                                        isFileUpload: true,
                                        validation: {
                                            required: true
                                        }
                                    },

                                    name: {
                                        label: 'label.name',
                                        docID: 'helpRegisterTemplateName',
                                        validation: {
                                            required: true
                                        }
                                    },

                                    description: {
                                        label: 'label.description',
                                        docID: 'helpRegisterTemplateDescription',
                                        validation: {
                                            required: true
                                        }
                                    },

                                    zone: {
                                        label: 'label.zone',
                                        docID: 'helpRegisterTemplateZone',
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

                                    hypervisor: {
                                        label: 'label.hypervisor',
                                        docID: 'helpRegisterTemplateHypervisor',
                                        dependsOn: 'zone',
                                        select: function(args) {
                                            if (args.zone == null)
                                                return;

                                            var apiCmd;
                                            if (args.zone == -1) { //All Zones
                                                //apiCmd = "listHypervisors&zoneid=-1"; //"listHypervisors&zoneid=-1" has been changed to return only hypervisors available in all zones (bug 8809)
                                                apiCmd = "listHypervisors";
                                            } else {
                                                apiCmd = "listHypervisors&zoneid=" + args.zone;
                                            }

                                            $.ajax({
                                                url: createURL(apiCmd),
                                                dataType: "json",
                                                async: false,
                                                success: function(json) {
                                                    var hypervisorObjs = json.listhypervisorsresponse.hypervisor;
                                                    var items = [];
                                                    $(hypervisorObjs).each(function() {
                                                        items.push({
                                                            id: this.name,
                                                            description: this.name
                                                        });
                                                    });
                                                    args.response.success({
                                                        data: items
                                                    });
                                                }
                                            });
                                            args.$select.change(function() {
                                                var $form = $(this).closest('form');
                                                if ($(this).val() == "VMware") {
                                                    $form.find('.form-item[rel=rootDiskControllerType]').css('display', 'inline-block');
                                                    $form.find('.form-item[rel=nicAdapterType]').css('display', 'inline-block');
                                                    $form.find('.form-item[rel=keyboardType]').css('display', 'inline-block');
                                                    $form.find('.form-item[rel=xenserverToolsVersion61plus]').hide();
                                                    $form.find('.form-item[rel=rootDiskControllerTypeKVM]').hide();
                                                    $form.find('.form-item[rel=requireshvm]').hide();
                                                } else if ($(this).val() == "XenServer") {
                                                    $form.find('.form-item[rel=rootDiskControllerType]').hide();
                                                    $form.find('.form-item[rel=nicAdapterType]').hide();
                                                    $form.find('.form-item[rel=keyboardType]').hide();
                                                    $form.find('.form-item[rel=rootDiskControllerTypeKVM]').hide();
                                                    $form.find('.form-item[rel=requireshvm]').css('display', 'inline-block');
                                                    if (isAdmin()) {
                                                        $form.find('.form-item[rel=xenserverToolsVersion61plus]').css('display', 'inline-block');
                                                    }
                                                } else if ($(this).val() == "KVM") {
                                                    $form.find('.form-item[rel=rootDiskControllerType]').hide();
                                                    $form.find('.form-item[rel=nicAdapterType]').hide();
                                                    $form.find('.form-item[rel=keyboardType]').hide();
                                                    $form.find('.form-item[rel=xenserverToolsVersion61plus]').hide();
                                                    $form.find('.form-item[rel=rootDiskControllerTypeKVM]').css('display', 'inline-block');
                                                    $('#label_root_disk_controller').prop('selectedIndex', 2);
                                                    $form.find('.form-item[rel=requireshvm]').css('display', 'inline-block');
                                                } else {
                                                    $form.find('.form-item[rel=rootDiskControllerType]').hide();
                                                    $form.find('.form-item[rel=nicAdapterType]').hide();
                                                    $form.find('.form-item[rel=keyboardType]').hide();
                                                    $form.find('.form-item[rel=xenserverToolsVersion61plus]').hide();
                                                    $form.find('.form-item[rel=rootDiskControllerTypeKVM]').hide();
                                                    $form.find('.form-item[rel=requireshvm]').css('display', 'inline-block');
                                                }
                                            });
                                            args.$select.trigger('change');
                                        }
                                    },

                                    // fields for hypervisor == XenServer (starts here)
                                    xenserverToolsVersion61plus: {
                                        label: 'label.xenserver.tools.version.61.plus',
                                        isBoolean: true,
                                        isChecked: function (args) {
                                             var b = true;
                                            if (isAdmin()) {
                                                $.ajax({
                                                    url: createURL('listConfigurations'),
                                                    data: {
                                                        name: 'xenserver.pvdriver.version'
                                                    },
                                                    async: false,
                                                    success: function (json) {
                                                        if (json.listconfigurationsresponse.configuration != null && json.listconfigurationsresponse.configuration[0].value != 'xenserver61') {
                                                            b = false;
                                                        }
                                                    }
                                                });
                                            }
                                            return b;
                                        },
                                        isHidden: true
                                    },
                                    // fields for hypervisor == XenServer (ends here)

                                    // fields for hypervisor == "KVM" (starts here)
                                    rootDiskControllerTypeKVM: {
                                        label: 'label.root.disk.controller',
                                        isHidden: true,
                                        select: function(args) {
                                            var items = []
                                            items.push({
                                                id: "",
                                                description: ""
                                            });
                                            items.push({
                                                id: "ide",
                                                description: "ide"
                                            });
                                            items.push({
                                                id: "osdefault",
                                                description: "osdefault"
                                            });
                                            items.push({
                                                id: "scsi",
                                                description: "virtio-scsi"
                                            });
                                            items.push({
                                                id: "virtio",
                                                description: "virtio"
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    // fields for hypervisor == "KVM" (ends here)

                                    // fields for hypervisor == "VMware" (starts here)
                                    rootDiskControllerType: {
                                        label: 'label.root.disk.controller',
                                        isHidden: true,
                                        select: function(args) {
                                            var items = []
                                            items.push({
                                                id: "",
                                                description: ""
                                            });
                                            items.push({
                                                id: "scsi",
                                                description: "scsi"
                                            });
                                            items.push({
                                                id: "ide",
                                                description: "ide"
                                            });
                                            items.push({
                                                id: "osdefault",
                                                description: "osdefault"
                                            });
                                            items.push({
                                                id: "pvscsi",
                                                description: "pvscsi"
                                            });
                                            items.push({
                                                id: "lsilogic",
                                                description: "lsilogic"
                                            });
                                            items.push({
                                                id: "lsisas1068",
                                                description: "lsilogicsas"
                                            });
                                            items.push({
                                                id: "buslogic",
                                                description: "buslogic"
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    nicAdapterType: {
                                        label: 'label.nic.adapter.type',
                                        isHidden: true,
                                        select: function(args) {
                                            var items = []
                                            items.push({
                                                id: "",
                                                description: ""
                                            });
                                            items.push({
                                                id: "E1000",
                                                description: "E1000"
                                            });
                                            items.push({
                                                id: "PCNet32",
                                                description: "PCNet32"
                                            });
                                            items.push({
                                                id: "Vmxnet2",
                                                description: "Vmxnet2"
                                            });
                                            items.push({
                                                id: "Vmxnet3",
                                                description: "Vmxnet3"
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    keyboardType: {
                                        label: 'label.keyboard.type',
                                        isHidden: true,
                                        select: function(args) {
                                            var items = []
                                            items.push({
                                                id: "",
                                                description: ""
                                            });
                                            for (var key in cloudStackOptions.keyboardOptions) {
                                                items.push({
                                                    id: key,
                                                    description: _l(cloudStackOptions.keyboardOptions[key])
                                                });
                                            }
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    // fields for hypervisor == "VMware" (ends here)

                                    format: {
                                        label: 'label.format',
                                        docID: 'helpRegisterTemplateFormat',
                                        dependsOn: 'hypervisor',
                                        select: function(args) {
                                            var items = [];
                                            if (args.hypervisor == "XenServer") {
                                                items.push({
                                                    id: 'VHD',
                                                    description: 'VHD'
                                                });
                                            } else if (args.hypervisor == "VMware") {
                                                items.push({
                                                    id: 'OVA',
                                                    description: 'OVA'
                                                });
                                            } else if (args.hypervisor == "KVM") {
                                                items.push({
                                                    id: 'QCOW2',
                                                    description: 'QCOW2'
                                                });
                                                items.push({
                                                    id: 'RAW',
                                                    description: 'RAW'
                                                });
                                                items.push({
                                                    id: 'VHD',
                                                    description: 'VHD'
                                                });
                                                items.push({
                                                    id: 'VMDK',
                                                    description: 'VMDK'
                                                });
                                            } else if (args.hypervisor == "BareMetal") {
                                                items.push({
                                                    id: 'BareMetal',
                                                    description: 'BareMetal'
                                                });
                                            } else if (args.hypervisor == "Ovm") {
                                                items.push({
                                                    id: 'RAW',
                                                    description: 'RAW'
                                                });
                                            } else if (args.hypervisor == "LXC") {
                                                items.push({
                                                    id: 'TAR',
                                                    description: 'TAR'
                                                });
                                            } else if (args.hypervisor == "Hyperv") {
                                                items.push({
                                                    id: 'VHD',
                                                    description: 'VHD'
                                                });
                                                items.push({
                                                    id: 'VHDX',
                                                    description: 'VHDX'
                                                });
                                            }
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },

                                    osTypeId: {
                                        label: 'label.os.type',
                                        docID: 'helpRegisterTemplateOSType',
                                        select: function(args) {
                                            $.ajax({
                                                url: createURL("listOsTypes"),
                                                dataType: "json",
                                                async: true,
                                                success: function(json) {
                                                    var ostypeObjs = json.listostypesresponse.ostype;
                                                    args.response.success({
                                                        data: ostypeObjs
                                                    });
                                                }
                                            });
                                        }
                                    },

                                    isExtractable: {
                                        label: "label.extractable",
                                        docID: 'helpRegisterTemplateExtractable',
                                        isBoolean: true
                                    },

                                    isPasswordEnabled: {
                                        label: "label.password.enabled",
                                        docID: 'helpRegisterTemplatePasswordEnabled',
                                        isBoolean: true
                                    },

                                    isdynamicallyscalable: {
                                        label: "label.dynamically.scalable",
                                        docID: 'helpRegisterTemplateDynamicallyScalable',
                                        isBoolean: true
                                    },

                                    isPublic: {
                                        label: "label.public",
                                        docID: 'helpRegisterTemplatePublic',
                                        isBoolean: true,
                                        isHidden: true
                                    },

                                    isFeatured: {
                                        label: "label.featured",
                                        docID: 'helpRegisterTemplateFeatured',
                                        isBoolean: true,
                                        isHidden: true
                                    },

                                    isrouting: {
                                        label: 'label.routing',
                                        docID: 'helpRegisterTemplateRouting',
                                        isBoolean: true,
                                        isHidden: true
                                    },

                                    requireshvm: {
                                        label: 'label.hvm',
                                        docID: 'helpRegisterTemplateHvm',
                                        isBoolean: true,
                                        isHidden: false,
                                        isChecked: true
                                    }
                                }
                            },

                            action: function(args) {
                                return; //createForm.fileUpload.getURL() has executed the whole action. Therefore, nothing needs to be done here.
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
                        // Due to zonal grouping, low pagesize can result lower
                        // aggregated items, resulting in no scroll shown
                        // So, use maximum pagesize
                        data.pagesize = 200;

                        var ignoreProject = false;
                        if (args.filterBy != null) { //filter dropdown
                            if (args.filterBy.kind != null) {
                                if (previousFilterType != args.filterBy.kind || args.page == 1) {
                                    previousFilterType = args.filterBy.kind;
                                    previousCollection = [];
                                }
                                switch (args.filterBy.kind) {
                                    case "all":
                                        $.extend(data, {
                                            templatefilter: 'all'
                                        });
                                        break;
                                    case "mine":
                                        $.extend(data, {
                                            templatefilter: 'self'
                                        });
                                        break;
                                    case "shared":
                                        $.extend(data, {
                                            templatefilter: 'shared'
                                        });
                                        break;
                                    case "featured":
                                        ignoreProject = true;
                                        $.extend(data, {
                                            templatefilter: 'featured'
                                        });
                                        break;
                                    case "community":
                                        ignoreProject = true;
                                        $.extend(data, {
                                            templatefilter: 'community'
                                        });
                                        break;
                                }
                            }
                        }

                        $.ajax({
                            url: createURL('listTemplates', {
                                ignoreProject: ignoreProject
                            }),
                            data: data,
                            success: function(json) {
                                var items = json.listtemplatesresponse.template;
                                var itemsView = [];

                                $(items).each(function(index, item) {
                                    var existing = $.grep(previousCollection, function(it){
                                        return it != null && it.id !=null && it.id == item.id;
                                    });

                                    if (existing.length > 0) {
                                        return true; // skip adding this entry
                                    } else {
                                        var templateItem = $.extend(item, {
                                            zones: item.zonename,
                                            zoneids: [item.zoneid]
                                        });
                                        itemsView.push(templateItem);
                                        previousCollection.push(templateItem);
                                    }
                                });

                                args.response.success({
                                    actionFilter: templateActionfilter,
                                    data: itemsView
                                });
                            }
                        });
                    },

                    detailView: {
                        name: 'Template details',
                        viewAll: {
                            label: 'label.instances',
                            path: 'instances'
                        },
                        actions: {
                            edit: {
                                label: 'label.edit',
                                action: function(args) {
                                    //***** updateTemplate *****
                                    var data = {
                                        id: args.context.templates[0].id,
                                        //zoneid: args.context.templates[0].zoneid, //can't update template/ISO in only one zone. It always get updated in all zones.
                                        name: args.data.name,
                                        displaytext: args.data.displaytext,
                                        ostypeid: args.data.ostypeid,
                                        passwordenabled: (args.data.passwordenabled == "on"),
                                        isdynamicallyscalable: (args.data.isdynamicallyscalable == "on")
                                    };
                                    $.ajax({
                                        url: createURL('updateTemplate'),
                                        data: data,
                                        async: false,
                                        success: function(json) {
                                            //API returns an incomplete embedded object  (some properties are missing in the embedded template object)
                                        }
                                    });


                                    //***** updateTemplatePermissions *****
                                    var data = {
                                        id: args.context.templates[0].id
                                        //zoneid: args.context.templates[0].zoneid //can't update template/ISO in only one zone. It always get updated in all zones.
                                    };

                                    //if args.data.ispublic is undefined(i.e. checkbox is hidden), do not pass ispublic to API call.
                                    if (args.data.ispublic == "on") {
                                        $.extend(data, {
                                            ispublic: true
                                        });
                                    } else if (args.data.ispublic == "off") {
                                        $.extend(data, {
                                            ispublic: false
                                        });
                                    }
                                    //if args.data.isfeatured is undefined(i.e. checkbox is hidden), do not pass isfeatured to API call.
                                    if (args.data.isfeatured == "on") {
                                        $.extend(data, {
                                            isfeatured: true
                                        });
                                    } else if (args.data.isfeatured == "off") {
                                        $.extend(data, {
                                            isfeatured: false
                                        });
                                    }
                                    //if args.data.isextractable is undefined(i.e. checkbox is hidden), do not pass isextractable to API call.
                                    if (args.data.isextractable == "on") {
                                        $.extend(data, {
                                            isextractable: true
                                        });
                                    } else if (args.data.isextractable == "off") {
                                        $.extend(data, {
                                            isextractable: false
                                        });
                                    }
                                    $.ajax({
                                        url: createURL('updateTemplatePermissions'),
                                        data: data,
                                        async: false,
                                        success: function(json) {
                                            //API doesn't return an embedded object
                                        }
                                    });


                                    //***** addResourceDetail *****
                                    //XenServer only (starts here)
                                      if(args.$detailView.find('form').find('div .detail-group').find('.xenserverToolsVersion61plus').length > 0) {
                                          $.ajax({
                                              url: createURL('addResourceDetail'),
                                              data: {
                                                  resourceType: 'template',
                                                  resourceId: args.context.templates[0].id,
                                                  'details[0].key': 'hypervisortoolsversion',
                                                  'details[0].value': (args.data.xenserverToolsVersion61plus == "on") ? 'xenserver61' : 'xenserver56'
                                              },
                                              success: function(json) {
                                                   var jobId = json.addResourceDetailresponse.jobid;
                                                   var addResourceDetailIntervalID = setInterval(function() {
                                                       $.ajax({
                                                           url: createURL("queryAsyncJobResult&jobid=" + jobId),
                                                           dataType: "json",
                                                           success: function(json) {
                                                               var result = json.queryasyncjobresultresponse;

                                                               if (result.jobstatus == 0) {
                                                                   return; //Job has not completed
                                                               } else {
                                                                   clearInterval(addResourceDetailIntervalID);

                                                                   if (result.jobstatus == 1) {
                                                                       //do nothing
                                                                   } else if (result.jobstatus == 2) {
                                                                       cloudStack.dialog.notice({
                                                                           message: "message.XSTools61plus.update.failed" + " " + _s(result.jobresult.errortext)
                                                                       });
                                                                   }
                                                               }
                                                           },
                                                           error: function(XMLHttpResponse) {
                                                               cloudStack.dialog.notice({
                                                                   message: "message.XSTools61plus.update.failed" + " " + parseXMLHttpResponse(XMLHttpResponse)
                                                               });
                                                           }
                                                       });
                                                   }, g_queryAsyncJobResultInterval);
                                              }
                                          });
                                      }
                                      //XenServer only (ends here)


                                    //***** listTemplates *****
                                    //So, we call listTemplates API to get a complete template object
                                    var data = {
                                        id: args.context.templates[0].id,
                                        zoneid: args.context.templates[0].zoneid,
                                        templatefilter: 'self'
                                    };
                                    $.ajax({
                                        url: createURL('listTemplates'),
                                        data: data,
                                        async: false,
                                        success: function(json) {
                                            var item = json.listtemplatesresponse.template;
                                            args.response.success({
                                                data: item
                                            });
                                        }
                                    });
                                }
                            },

                            downloadTemplate: {
                                label: 'label.action.download.template',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.action.download.template';
                                    },
                                    notification: function(args) {
                                        return 'message.action.downloading.template';
                                    },
                                    complete: function(args) {
                                        var url = args.url;
                                        var htmlMsg = _l('message.download.template');
                                        var htmlMsg2 = htmlMsg.replace(/#/, url).replace(/00000/, url);
                                        return htmlMsg2;
                                    }
                                },
                                action: function(args) {
                                    var apiCmd = "extractTemplate&mode=HTTP_DOWNLOAD&id=" + args.context.templates[0].id;
                                    if (args.context.templates[0].zoneid != null)
                                        apiCmd += "&zoneid=" + args.context.templates[0].zoneid;

                                    $.ajax({
                                        url: createURL(apiCmd),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var jid = json.extracttemplateresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid,
                                                    getUpdatedItem: function(json) {
                                                        return json.queryasyncjobresultresponse.jobresult.template;
                                                    },
                                                    getActionFilter: function() {
                                                        return templateActionfilter;
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
                            // Share template
                            shareTemplate: {
                                label: 'label.action.share.template',
                                messages: {
                                    notification: function (args) {
                                        return 'label.action.share.template';
                                    }
                                },

                                createForm: {
                                    title: 'label.action.share.template',
                                    desc: '',
                                    fields: {
                                        operation: {
                                            label: 'label.operation',
                                            docID: 'helpUpdateTemplateOperation',
                                            validation: {
                                                required: true
                                            },
                                            select: function (args) {
                                                var items = [];
                                                items.push({
                                                    id: "add",
                                                    description: "Add"
                                                });
                                                items.push({
                                                    id: "remove",
                                                    description: "Remove"
                                                });
                                                items.push({
                                                    id: "reset",
                                                    description: "Reset"
                                                });

                                                args.response.success({
                                                    data: items
                                                });

                                                // Select change
                                                args.$select.change(function () {
                                                    var $form = $(this).closest('form');
                                                    var selectedOperation = $(this).val();
                                                    if (selectedOperation === "reset") {
                                                        $form.find('[rel=projects]').hide();
                                                        $form.find('[rel=sharewith]').hide();
                                                        $form.find('[rel=accounts]').hide();
                                                        $form.find('[rel=accountlist]').hide();
                                                    } else {
                                                        // allow.user.view.domain.accounts = true
                                                        // Populate List of accounts in domain as dropdown multiselect
                                                        $form.find('[rel=sharewith]').css('display', 'inline-block');
                                                        if (!isUser() || g_allowUserViewAllDomainAccounts === true) {
                                                            $form.find('[rel=projects]').css('display', 'inline-block');
                                                            $form.find('[rel=accounts]').css('display', 'inline-block');
                                                            $form.find('[rel=accountlist]').hide();
                                                        } else {
                                                            // If users are not allowed to see accounts in the domain, show input text field for Accounts
                                                            // Projects will always be shown as dropdown multiselect
                                                            $form.find('[rel=projects]').css('display', 'inline-block');
                                                            $form.find('[rel=accountslist]').css('display', 'inline-block');
                                                            $form.find('[rel=accounts]').hide();
                                                        }
                                                    }
                                                });
                                            }
                                        },
                                        shareWith: {
                                            label: 'label.share.with',
                                            docID: 'helpUpdateTemplateShareWith',
                                            validation: {
                                                required: true
                                            },
                                            dependsOn: 'operation',
                                            select: function (args) {
                                                var items = [];
                                                items.push({
                                                    id: "account",
                                                    description: "Account"
                                                });
                                                items.push({
                                                    id: "project",
                                                    description: "Project"
                                                });

                                                args.response.success({ data: items });

                                                // Select change
                                                args.$select.change(function () {
                                                    var $form = $(this).closest('form');
                                                    var sharedWith = $(this).val();
                                                    if (args.operation !== "reset") {
                                                        if (sharedWith === "project") {
                                                            $form.find('[rel=accounts]').hide();
                                                            $form.find('[rel=accountlist]').hide();
                                                            $form.find('[rel=projects]').css('display', 'inline-block');
                                                        } else {
                                                            // allow.user.view.domain.accounts = true
                                                            // Populate List of accounts in domain as dropdown multiselect
                                                            if (!isUser() || g_allowUserViewAllDomainAccounts === true) {
                                                                $form.find('[rel=projects]').hide();
                                                                $form.find('[rel=accountlist]').hide();
                                                                $form.find('[rel=accounts]').css('display', 'inline-block');
                                                            } else {
                                                                // If users are not allowed to see accounts in the domain, show input text field for Accounts
                                                                // Projects will always be shown as dropdown multiselect
                                                                $form.find('[rel=projects]').hide();
                                                                $form.find('[rel=accounts]').hide();
                                                                $form.find('[rel=accountlist]').css('display', 'inline-block');
                                                            }
                                                        }
                                                    }
                                                });
                                            }
                                        },

                                        accountlist: {
                                            label: 'label.accounts',
                                            docID: 'helpUpdateTemplateAccountList'
                                        },

                                        accounts: {
                                            label: 'label.accounts',
                                            docID: 'helpUpdateTemplateAccounts',
                                            dependsOn: 'shareWith',
                                            isMultiple: true,
                                            select: function (args) {
                                                var operation = args.operation;
                                                if (operation !== "reset") {
                                                    $.ajax({
                                                        url: createURL("listAccounts&listall=true"),
                                                        dataType: "json",
                                                        async: true,
                                                        success: function (jsonAccounts) {
                                                            var accountByName = {};
                                                            $.each(jsonAccounts.listaccountsresponse.account, function(idx, account) {
                                                                // Only add current domain's accounts for add as update template permissions supports that
                                                                if (account.domainid === g_domainid && operation === "add") {
                                                                    accountByName[account.name] = {
                                                                        projName: account.name,
                                                                        hasPermission: false
                                                                    };
                                                                }
                                                            });
                                                            $.ajax({
                                                                url: createURL('listTemplatePermissions&id=' + args.context.templates[0].id),
                                                                dataType: "json",
                                                                async: true,
                                                                success: function (json) {
                                                                    items = json.listtemplatepermissionsresponse.templatepermission.account;
                                                                    $.each(items, function(idx, accountName) {
                                                                        if (accountByName[accountName]) {
                                                                            accountByName[accountName].hasPermission = true;
                                                                        }
                                                                    });

                                                                    var accountObjs = [];
                                                                    if (operation === "add") {
                                                                        // Skip already permitted accounts
                                                                        $.each(Object.keys(accountByName), function(idx, accountName) {
                                                                            if (accountByName[accountName].hasPermission == false) {
                                                                                accountObjs.push({
                                                                                    name: accountName,
                                                                                    description: accountName
                                                                                });
                                                                            }
                                                                        });
                                                                    } else if (items != null) {
                                                                        $.each(items, function(idx, accountName) {
                                                                            if (accountName !== g_account) {
                                                                                accountObjs.push({
                                                                                    name: accountName,
                                                                                    description: accountName
                                                                                });
                                                                            }
                                                                        });
                                                                    }
                                                                    args.$select.html('');
                                                                    args.response.success({data: accountObjs});
                                                                }
                                                            });
                                                        }
                                                    });
                                                }
                                            }
                                        },

                                        projects: {
                                            label: 'label.projects',
                                            docID: 'helpUpdateTemplateProjectIds',
                                            dependsOn: 'shareWith',
                                            isMultiple: true,
                                            select: function (args) {
                                                var operation = args.operation;
                                                if (operation !== "reset") {
                                                    $.ajax({
                                                        url: createURL("listProjects&listall=true"),
                                                        dataType: "json",
                                                        async: true,
                                                        success: function (jsonProjects) {
                                                            var projectsByIds = {};
                                                            $.each(jsonProjects.listprojectsresponse.project, function(idx, project) {
                                                                // Only add current domain's projects for add operation as update template permissions supports that
                                                                if ((project.domainid === g_domainid && operation === "add") || operation === "remove") {
                                                                    projectsByIds[project.id] = {
                                                                        projName: project.name,
                                                                        hasPermission: false
                                                                    };
                                                                }
                                                            });

                                                            $.ajax({
                                                                url: createURL('listTemplatePermissions&id=' + args.context.templates[0].id),
                                                                dataType: "json",
                                                                async: true,
                                                                success: function (json) {
                                                                    items = json.listtemplatepermissionsresponse.templatepermission.projectids;
                                                                    $.each(items, function(idx, projectId) {
                                                                        if (projectsByIds[projectId]) {
                                                                            projectsByIds[projectId].hasPermission = true;
                                                                        }
                                                                    });

                                                                    var projectObjs = [];
                                                                    if (operation === "add") {
                                                                        // Skip already permitted accounts
                                                                        $.each(Object.keys(projectsByIds), function(idx, projectId) {
                                                                            if (projectsByIds[projectId].hasPermission == false) {
                                                                                projectObjs.push({
                                                                                    id: projectId,
                                                                                    description: projectsByIds[projectId].projName
                                                                                });
                                                                            }
                                                                        });
                                                                    } else if (items != null) {
                                                                        $.each(items, function(idx, projectId) {
                                                                            if (projectId !== g_account) {
                                                                                projectObjs.push({
                                                                                    id: projectId,
                                                                                    description: projectsByIds[projectId] ? projectsByIds[projectId].projName : projectId
                                                                                });
                                                                            }
                                                                        });
                                                                    }
                                                                    args.$select.html('');
                                                                    args.response.success({data: projectObjs});
                                                                }
                                                            });
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    }
                                },

                                action: function (args) {
                                    // Load data from form
                                    var data = {
                                        id: args.context.templates[0].id,
                                        op: args.data.operation
                                    };
                                    var selectedOperation = args.data.operation;
                                    if (selectedOperation === "reset") {
                                        // Do not append Project ID or Account to data object
                                    } else {
                                        var projects = args.data.projects;
                                        var accounts = args.data.accounts;
                                        var accountList = args.data.accountlist;

                                        if (accounts !== undefined || (accountList !== undefined && accountList.length > 0)) {
                                            var accountNames = "";
                                            if (accountList !== undefined && accounts === undefined) {
                                                accountNames = accountList;
                                            } else {
                                                if (Object.prototype.toString.call(accounts) === '[object Array]') {
                                                    accountNames = accounts.join(",");
                                                } else {
                                                    accountNames = accounts;
                                                }
                                            }
                                            $.extend(data, {
                                                accounts: accountNames
                                            });
                                        }

                                        if (projects !== undefined) {
                                            var projectIds = "";
                                            if (Object.prototype.toString.call(projects) === '[object Array]') {
                                                projectIds = projects.join(",");
                                            } else {
                                                projectIds = projects;
                                            }

                                            $.extend(data, {
                                                projectids: projectIds
                                            });
                                        }
                                    }

                                    $.ajax({
                                        url: createURL('updateTemplatePermissions'),
                                        data: data,
                                        dataType: "json",
                                        async: false,
                                        success: function (json) {
                                            var item = json.updatetemplatepermissionsresponse.success;
                                            args.response.success({
                                                data: item
                                            });
                                        }
                                    }); //end ajax
                                }
                            }
                        },
                        tabFilter: function (args) {
                            $.ajax({
                                url: createURL("listTemplateOvfProperties&id=" + args.context.templates[0].id),
                                dataType: "json",
                                async: false,
                                success: function(json) {
                                    ovfprops = json.listtemplateovfpropertiesresponse.ovfproperty;
                                }
                            });
                            var hiddenTabs = [];
                            if (ovfprops == null || ovfprops.length === 0) {
                                hiddenTabs.push("ovfpropertiestab");
                            }
                            return hiddenTabs;
                        },
                        tabs: {
                            details: {
                                title: 'label.details',

                                preFilter: function(args) {
                                    var hiddenFields;
                                    if (isAdmin()) {
                                        hiddenFields = [];
                                    } else {
                                        hiddenFields = ["hypervisor", 'xenserverToolsVersion61plus'];
                                    }

                                    if ('templates' in args.context && args.context.templates[0].hypervisor != 'XenServer') {
                                        hiddenFields.push('xenserverToolsVersion61plus');
                                    }

                                    if ('templates' in args.context && args.context.templates[0].ostypeid != undefined) {
                                        var ostypeObjs;
                                        $.ajax({
                                            url: createURL("listOsTypes"),
                                            dataType: "json",
                                            async: false,
                                            success: function(json) {
                                                var ostypeObjs = json.listostypesresponse.ostype;
                                            }
                                        });

                                        if (ostypeObjs != undefined) {
                                            var ostypeName;
                                            for (var i = 0; i < ostypeObjs.length; i++) {
                                                if (ostypeObjs[i].id == args.context.templates[0].ostypeid) {
                                                    ostypeName = ostypeObjs[i].description;
                                                    break;
                                                }
                                            }
                                            if (ostypeName == undefined || ostypeName.indexOf("Win") == -1) {
                                                hiddenFields.push('xenserverToolsVersion61plus');
                                            }
                                        }
                                    }

                                    return hiddenFields;
                                },

                                fields: [{
                                    name: {
                                        label: 'label.name',
                                        isEditable: true,
                                        validation: {
                                            required: true
                                        }
                                    }
                                }, {
                                    hypervisor: {
                                        label: 'label.hypervisor'
                                    },
                                    xenserverToolsVersion61plus: {
                                        label: 'label.xenserver.tools.version.61.plus',
                                        isBoolean: true,
                                        isEditable: function () {
                                            if (isAdmin())
                                                return true;
                                            else
                                                return false;
                                        },
                                        converter: cloudStack.converters.toBooleanText
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
                                    directdownload: {
                                        label: 'label.direct.download',
                                        isBoolean: true,
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    isextractable: {
                                        label: 'label.extractable.lower',
                                        isBoolean: true,
                                        isEditable: true,
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    passwordenabled: {
                                        label: 'label.password.enabled',
                                        isBoolean: true,
                                        isEditable: true,
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    isdynamicallyscalable: {
                                        label: 'label.dynamically.scalable',
                                        isBoolean: true,
                                        isEditable: true,
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    ispublic: {
                                        label: 'label.public',
                                        isBoolean: true,
                                        isEditable: function() {
                                            if (isAdmin()) {
                                                return true;
                                            } else {
                                                if (g_userPublicTemplateEnabled == "true")
                                                    return true;
                                                else
                                                    return false;
                                            }
                                        },
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    isfeatured: {
                                        label: 'label.featured',
                                        isBoolean: true,
                                        isEditable: function() {
                                            if (isAdmin())
                                                return true;
                                            else
                                                return false;
                                        },
                                        converter: cloudStack.converters.toBooleanText
                                    },

                                    ostypeid: {
                                        label: 'label.os.type',
                                        isEditable: true,
                                        select: function(args) {
                                            var ostypeObjs;
                                            $.ajax({
                                                url: createURL("listOsTypes"),
                                                dataType: "json",
                                                async: false,
                                                success: function(json) {
                                                    ostypeObjs = json.listostypesresponse.ostype;
                                                }
                                            });

                                            var items = [];
                                            $(ostypeObjs).each(function() {
                                                items.push({
                                                    id: this.id,
                                                    description: this.description
                                                });
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },

                                    crossZones: {
                                        label: 'label.cross.zones',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    displaytext: {
                                        label: 'label.description',
                                        isEditable: true,
                                        validation: {
                                            required: true
                                        }
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
                                    },

                                    templatetype: {
                                        label: 'label.type'
                                    },

                                    id: {
                                        label: 'label.id'
                                    }
                                }],

                                tags: cloudStack.api.tags({
                                    resourceType: 'Template',
                                    contextId: 'templates'
                                }),


                                dataProvider: function(args) {  // UI > Templates menu (listing) > select a template from listing > Details tab
                                    $.ajax({
                                        url: createURL("listTemplates"),
                                        data: {
                                            templatefilter: "self",
                                            id: args.context.templates[0].id
                                        },
                                        success: function(json) {
                                            var jsonObj = json.listtemplatesresponse.template[0];

                                            if ('details' in jsonObj && 'hypervisortoolsversion' in jsonObj.details) {
                                                if (jsonObj.details.hypervisortoolsversion == 'xenserver61')
                                                    jsonObj.xenserverToolsVersion61plus = true;
                                                else
                                                    jsonObj.xenserverToolsVersion61plus = false;
                                            }

                                            args.response.success({
                                                actionFilter: templateActionfilter,
                                                data: jsonObj
                                            });
                                        }
                                    });
                                }
                            },

                            zones: {
                                title: 'label.zones',
                                listView: {
                                    id: 'zones',
                                    fields: {
                                        zonename: {
                                            label: 'label.name'
                                        },
                                        status: {
                                            label: 'label.status'
                                        },
                                        isready: {
                                            label: 'state.Ready',
                                            converter: cloudStack.converters.toBooleanText
                                        }
                                    },
                                    hideSearchBar: true,


                                    dataProvider: function(args) {  // UI > Templates menu (listing) > select a template from listing > Details tab > Zones tab (listing)
                                        var data = { templatefilter: "self",
                                                     id: args.context.templates[0].id
                                                   };
                                        listViewDataProvider(args, data);
                                        $.ajax({
                                            url: createURL("listTemplates"),
                                            data: data,
                                            success: function(json) {
                                                var jsonObjs = json.listtemplatesresponse.template;

                                                if (jsonObjs != undefined) {
                                                    for (var i = 0; i < jsonObjs.length; i++) {
                                                        var jsonObj = jsonObjs[i];
                                                        if ('details' in jsonObj && 'hypervisortoolsversion' in jsonObj.details) {
                                                            if (jsonObj.details.hypervisortoolsversion == 'xenserver61')
                                                                jsonObj.xenserverToolsVersion61plus = true;
                                                            else
                                                                jsonObj.xenserverToolsVersion61plus = false;
                                                        }
                                                    }
                                                    if (!'zonename' in jsonObj) {
                                                        jsonObj.zonename = 'All Zones';
                                                    }
                                                }

                                                args.response.success({
                                                    actionFilter: templateActionfilter,
                                                    data: jsonObjs
                                                });
                                            }
                                        });
                                    },

                                    detailView: {
                                        noCompact: true,
                                        actions: {
                                             remove: {
                                                 label: 'label.action.delete.template',
                                                 createForm: {
                                                    title: 'label.action.delete.template',
                                                    desc: function(args) {
                                                       if(args.context.templates[0].crossZones == true) {
                                                          return 'message.action.delete.template.for.all.zones';
                                                       } else {
                                                          return 'message.action.delete.template';
                                                       }
                                                      },
                                                    fields: {
                                                        forced: {
                                                            label: 'force.delete',
                                                            isBoolean: true,
                                                            isChecked: false
                                                        }
                                                    }
                                                 },
                                                 messages: {
                                                     notification: function(args) {
                                                         return 'label.action.delete.template';
                                                     }
                                                 },
                                                 action: function(args) {
                                                     var queryParams = "deleteTemplate&id=" + args.context.templates[0].id;
                                                     if (!args.context.templates[0].crossZones){
                                                        queryParams += "&zoneid=" + args.context.zones[0].zoneid;
                                                     }
                                                     $.ajax({
                                                         url: createURL(queryParams + "&forced=" + (args.data.forced == "on")),
                                                         dataType: "json",
                                                         async: true,
                                                         success: function(json) {
                                                             var jid = json.deletetemplateresponse.jobid;
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
                                             copyTemplate: {
                                                 label: 'label.action.copy.template',
                                                 messages: {
                                                     success: function(args) {
                                                         return 'message.template.copying';
                                                     },
                                                     notification: function(args) {
                                                         return 'label.action.copy.template';
                                                     }
                                                 },
                                                action: {
                                                    custom: cloudStack.uiCustom.copyTemplate({
                                                        listView: {
                                                            listView: {
                                                                id: 'destinationZones',
                                                                fields: {
                                                                    destinationZoneName: {
                                                                        label: 'label.name'
                                                                    }
                                                                },
                                                                dataProvider: function(args) {
                                                                     var data = {
                                                                        page: args.page,
                                                                        pagesize: pageSize
                                                                    };
                                                                    if (args.filterBy.search.value) {
                                                                        data.keyword = args.filterBy.search.value;
                                                                    }
                                                                     $.ajax({
                                                                             url: createURL("listZones&available=true"),
                                                                             dataType: "json",
                                                                             data: data,
                                                                             async: true,
                                                                             success: function(json) {
                                                                                 var zoneObjs = [];
                                                                                 var items = json.listzonesresponse.zone;
                                                                                 if (items != null) {
                                                                                     for (var i = 0; i < items.length; i++) {
                                                                                         if (args.context.zones[0].zoneid != items[i].id) {
                                                                                             zoneObjs.push({
                                                                                                 id: items[i].id,
                                                                                                 destinationZoneName: items[i].name
                                                                                             });
                                                                                         }
                                                                                     }
                                                                                     args.response.success({
                                                                                         data: zoneObjs
                                                                                     });
                                                                                }else if(args.page == 1) {
							                             args.response.success({
                                                                                         data: []
                                                                                     });
                                                                            } else {
							                             args.response.success({
                                                                                         data: []
                                                                                     });
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            }
                                                        },
                                                        action: function(args) {
                                                            var zoneids = "";
                                                            if (args.context.selectedZone != null &&
                                                                    args.context.selectedZone.length > 0) {
                                                                for (var i = 0; i < args.context.selectedZone.length; i++){
                                                                    if (i != 0 )
                                                                        zoneids += ",";
                                                                    zoneids += args.context.selectedZone[i].id;
                                                                }
                                                            }
                                                            if (zoneids == "")
                                                                return;
                                                            var data = {
                                                                 id: args.context.templates[0].id,
                                                                 destzoneids: zoneids,
                                                                 sourcezoneid: args.context.zones[0].zoneid
                                                            };

                                                             $.ajax({
                                                                 url: createURL('copyTemplate'),
                                                                 data: data,
                                                                 success: function(json) {
                                                                     var jid = json.copytemplateresponse.jobid;
                                                                     args.response.success({
                                                                         _custom: {
                                                                             jobId: jid,
                                                                             getUpdatedItem: function(json) {
                                                                                 return {}; //nothing in this template needs to be updated
                                                                             },
                                                                             getActionFilter: function() {
                                                                                 return templateActionfilter;
                                                                             }
                                                                         }
                                                                     });
                                                                 }
                                                             });
                                                         }
                                                    })
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
                                                    hiddenFields = ["hypervisor", 'xenserverToolsVersion61plus'];
                                                }

                                                if ('templates' in args.context && args.context.templates[0].hypervisor != 'XenServer') {
                                                    hiddenFields.push('xenserverToolsVersion61plus');
                                                }

                                                if ('templates' in args.context && args.context.templates[0].ostypeid != undefined) {
                                                    var ostypeObjs;
                                                    $.ajax({
                                                        url: createURL("listOsTypes"),
                                                        dataType: "json",
                                                        async: false,
                                                        success: function(json) {
                                                            ostypeObjs = json.listostypesresponse.ostype;
                                                        }
                                                    });

                                                    if (ostypeObjs != undefined) {
                                                        var ostypeName;
                                                        for (var i = 0; i < ostypeObjs.length; i++) {
                                                            if (ostypeObjs[i].id == args.context.templates[0].ostypeid) {
                                                                ostypeName = ostypeObjs[i].description;
                                                                break;
                                                            }
                                                        }
                                                        if (ostypeName == undefined || ostypeName.indexOf("Win") == -1) {
                                                            hiddenFields.push('xenserverToolsVersion61plus');
                                                        }
                                                    }
                                                }

                                                return hiddenFields;
                                            },

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
                                                zonename: {
                                                    label: 'label.zone.name'
                                                },
                                                zoneid: {
                                                    label: 'label.zone.id'
                                                },
                                                isready: {
                                                    label: 'state.Ready',
                                                    converter: cloudStack.converters.toBooleanText
                                                },
                                                status: {
                                                    label: 'label.status'
                                                }
                                            }, {
                                                hypervisor: {
                                                    label: 'label.hypervisor'
                                                },
                                                xenserverToolsVersion61plus: {
                                                    label: 'label.xenserver.tools.version.61.plus',
                                                    isBoolean: true,
                                                    isEditable: function () {
                                                        if (isAdmin())
                                                            return true;
                                                        else
                                                            return false;
                                                    },
                                                    converter: cloudStack.converters.toBooleanText
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
                                                isextractable: {
                                                    label: 'label.extractable.lower',
                                                    isBoolean: true,
                                                    isEditable: true,
                                                    converter: cloudStack.converters.toBooleanText
                                                },
                                                passwordenabled: {
                                                    label: 'label.password.enabled',
                                                    isBoolean: true,
                                                    isEditable: true,
                                                    converter: cloudStack.converters.toBooleanText
                                                },
                                                isdynamicallyscalable: {
                                                    label: 'label.dynamically.scalable',
                                                    isBoolean: true,
                                                    isEditable: true,
                                                    converter: cloudStack.converters.toBooleanText
                                                },
                                                ispublic: {
                                                    label: 'label.public',
                                                    isBoolean: true,
                                                    isEditable: function() {
                                                        if (isAdmin()) {
                                                            return true;
                                                        } else {
                                                            if (g_userPublicTemplateEnabled == "true")
                                                                return true;
                                                            else
                                                                return false;
                                                        }
                                                    },
                                                    converter: cloudStack.converters.toBooleanText
                                                },
                                                isfeatured: {
                                                    label: 'label.featured',
                                                    isBoolean: true,
                                                    isEditable: function() {
                                                        if (isAdmin())
                                                            return true;
                                                        else
                                                            return false;
                                                    },
                                                    converter: cloudStack.converters.toBooleanText
                                                },

                                                ostypeid: {
                                                    label: 'label.os.type',
                                                    isEditable: true,
                                                    select: function(args) {
                                                        var ostypeObjs;
                                                        $.ajax({
                                                            url: createURL("listOsTypes"),
                                                            dataType: "json",
                                                            async: false,
                                                            success: function(json) {
                                                                ostypeObjs = json.listostypesresponse.ostype;
                                                            }
                                                        });

                                                        var items = [];
                                                        $(ostypeObjs).each(function() {
                                                            items.push({
                                                                id: this.id,
                                                                description: this.description
                                                            });
                                                        });
                                                        args.response.success({
                                                            data: items
                                                        });
                                                    }
                                                },


                                                displaytext: {
                                                    label: 'label.description',
                                                    isEditable: true,
                                                    validation: {
                                                        required: true
                                                    }
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
                                                },

                                                templatetype: {
                                                    label: 'label.type'
                                                }

                                            }],

                                            tags: cloudStack.api.tags({
                                                resourceType: 'Template',
                                                contextId: 'templates'
                                            }),


                                            dataProvider: function(args) {  // UI > Templates menu (listing) > select a template from listing > Details tab > Zones tab (listing) > select a zone from listing > Details tab
                                                var jsonObj = args.context.zones[0];

                                                if ('details' in jsonObj && 'hypervisortoolsversion' in jsonObj.details) {
                                                    if (jsonObj.details.hypervisortoolsversion == 'xenserver61')
                                                        jsonObj.xenserverToolsVersion61plus = true;
                                                    else
                                                        jsonObj.xenserverToolsVersion61plus = false;
                                                }

                                                args.response.success({
                                                    actionFilter: templateActionfilter,
                                                    data: jsonObj
                                                });
                                            }
                                        }
                                    }}
                                }
                            },
                            /**
							 * Settings tab
							 */
							settings: {
								title: 'label.settings',
								custom: cloudStack.uiCustom.granularDetails({
                                    resourceType: 'Template',
									dataProvider: function(args) {
									    // no paging for listTemplates details
									    if (args.page > 1) {
									        args.response.success({
									            data: []
									        });
									        return;
									    }
										$.ajax({
											url: createURL('listTemplates'),
											data: {
												templatefilter: "self",
												id: args.context.templates[0].id
											},
											success: function(json) {
												var details = json.listtemplatesresponse.template[0].details;
												var listDetails = [];
												for (detail in details){
													var det = {};
													det["name"] = detail;
													det["value"] = details[detail];
													listDetails.push(det);
												}
												args.response.success({
													data: listDetails
												});
											},

											error: function(json) {
												args.response.error(parseXMLHttpResponse(json));
											}
										});

									},
									actions: {
										edit: function(args) {
											var data = {
												name: args.data.jsonObj.name,
												value: args.data.value
											};
											var existingDetails = args.context.templates[0].details;
											var newDetails = '';
											for (d in existingDetails) {
												if (d != data.name) {
													newDetails += 'details[0].' + d + '=' + existingDetails[d] + '&';
												}
											}
											newDetails += 'details[0].' + data.name + '=' + data.value;

											$.ajax({
												url: createURL('updateTemplate&id=' + args.context.templates[0].id + '&' + newDetails),
												success: function(json) {
													var template = json.updatetemplateresponse.template;
													args.context.templates[0].details = template.details;
													args.response.success({
														data: template.details
													});
												},

												error: function(json) {
													args.response.error(parseXMLHttpResponse(json));
												}
											});
										},
										remove: function(args) {
											var existingDetails = args.context.templates[0].details;
											var detailToDelete = args.data.jsonObj.name;
											var newDetails = ''
											for (detail in existingDetails) {
												if (detail != detailToDelete) {
													newDetails += 'details[0].' + detail + '=' + existingDetails[detail] + '&';
												}
											}
											if (newDetails != '') {
												newDetails = newDetails.substring(0, newDetails.length - 1);
											}
											else {
												newDetails += 'cleanupdetails=true';
											}
											$.ajax({
												url: createURL('updateTemplate&id=' + args.context.templates[0].id + '&' + newDetails),
												success: function(json) {
													var template = json.updatetemplateresponse.template;
													args.context.templates[0].details = template.details;
													args.response.success({
														data: template.details
													});
												},
												error: function(json) {
													args.response.error(parseXMLHttpResponse(json));
												}
											});
										},
										add: function(args) {
											var name = args.data.name;
											var value = args.data.value;
											var details = args.context.templates[0].details;
											var detailsFormat = '';
											for (key in details) {
												detailsFormat += "details[0]." + key + "=" + details[key] + "&";
											}
											// Add new detail to the existing ones
											detailsFormat += "details[0]." + name + "=" + value;
											$.ajax({
												url: createURL('updateTemplate&id=' + args.context.templates[0].id + "&" + detailsFormat),
												async: false,
												success: function(json) {
													var template = json.updatetemplateresponse.template;
													args.context.templates[0].details = template.details;
													args.response.success({
														data: template.details
													});
												}
											});
										}
									}
								})
							},

                            /**
                             * OVF properties tab (only displayed when OVF properties are available)
                             */
                            ovfpropertiestab: {
                                title: 'label.ovf.properties',
                                listView: {
                                    id: 'ovfproperties',
                                    fields: {
                                        label: {
                                            label: 'label.label'
                                        },
                                        description: {
                                            label: 'label.description'
                                        },
                                        value: {
                                            label: 'label.value'
                                        }
                                    },
                                    hideSearchBar: true,
                                    dataProvider: function(args) {
                                        $.ajax({
                                            url: createURL("listTemplateOvfProperties"),
                                            data: {
                                                id: args.context.templates[0].id
                                            },
                                            success: function(json) {
                                                var ovfprops = json.listtemplateovfpropertiesresponse.ovfproperty;
                                                var listDetails = [];
                                                for (index in ovfprops){
                                                    var prop = ovfprops[index];
                                                    var det = {};
                                                    det['label'] = prop['label'];
                                                    det['description'] = prop['description'];
                                                    det['value'] = prop['value'];
                                                    listDetails.push(det);
                                                }
                                                args.response.success({
                                                    data: listDetails
                                                });
                                            },

                                            error: function(json) {
                                                args.response.error(parseXMLHttpResponse(json));
                                            }
                                        });

                                    }
                                }
                            }
						}
                    }
                }
            },
            isos: {
                type: 'select',
                title: 'label.iso',
                listView: {
                    label: 'label.iso',
                    filters: {
                        all: {
                            preFilter: function(args) {
                                if (isAdmin()||isDomainAdmin()) //"listIsos&filter=all" works for root-admin and domain-admin. Domain-admin is able to see all Isos in his domain.
                                    return true;
                                else
                                    return false;
                            },
                            label: 'ui.listView.filters.all'
                        },
                        mine: {
                            label: 'ui.listView.filters.mine'
                        },
                        shared: {
                            label: 'label.shared'
                        },
                        featured: {
                            label: 'label.featured'
                        },
                        community: {
                            label: 'label.community'
                        }
                    },
                    preFilter: function() {
                        if (isAdmin()||isDomainAdmin()) {
                            return []
                        }
                        return ['account']
                    },
                    fields: {
                        name: {
                            label: 'label.name'
                        },
                        ostypename: {
                            label: 'label.os.type'
                        },
                        account: {
                            label: 'label.account'
                        }
                    },

                    reorder: cloudStack.api.actions.sort('updateIso', 'isos'),

                    actions: {
                        add: {
                            label: 'label.action.register.iso',
                            messages: {
                                notification: function(args) {
                                    return 'label.action.register.iso';
                                }
                            },
                            createForm: {
                                title: 'label.action.register.iso',
                                preFilter: cloudStack.preFilter.createTemplate,
                                fields: {
                                    name: {
                                        label: 'label.name',
                                        docID: 'helpRegisterISOName',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    description: {
                                        label: 'label.description',
                                        docID: 'helpRegisterISODescription',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    url: {
                                        label: 'label.url',
                                        docID: 'helpRegisterISOURL',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    // For KVM only: Direct Download
                                    directdownload : {
                                        label: 'label.direct.download',
                                        docID: 'helpRegisterTemplateDirectDownload',
                                        isBoolean: true
                                    },
                                    checksum: {
                                        label: 'label.checksum',
                                        dependsOn: 'directdownload',
                                        isHidden: true
                                    },
                                    // Direct Download - End
                                    zone: {
                                        label: 'label.zone',
                                        docID: 'helpRegisterISOZone',
                                        select: function(args) {
                                            if(g_regionsecondaryenabled == true) {
                                                args.response.success({
                                                    data: [{
                                                        id: -1,
                                                        description: "All Zones"
                                                    }]
                                                });
                                            } else {
                                                $.ajax({
                                                    url: createURL("listZones&available=true"),
                                                    dataType: "json",
                                                    async: true,
                                                    success: function(json) {
                                                        var zoneObjs = [];
                                                        var items = json.listzonesresponse.zone;
                                                        if (items != null) {
                                                            for (var i = 0; i < items.length; i++) {
                                                                zoneObjs.push({
                                                                    id: items[i].id,
                                                                    description: items[i].name
                                                                });
                                                            }
                                                        }
                                                        if (isAdmin() && !(cloudStack.context.projects && cloudStack.context.projects[0])) {
                                                            zoneObjs.unshift({
                                                                id: -1,
                                                                description: "All Zones"
                                                            });
                                                        }
                                                        args.response.success({
                                                            data: zoneObjs
                                                        });
                                                    }
                                                });
                                            }
                                        }
                                    },

                                    isBootable: {
                                        label: "label.bootable",
                                        docID: 'helpRegisterISOBootable',
                                        isBoolean: true,
                                        isChecked: true
                                    },

                                    osTypeId: {
                                        label: 'label.os.type',
                                        docID: 'helpRegisterISOOSType',
                                        dependsOn: 'isBootable',
                                        isHidden: false,
                                        validation: {
                                            required: true
                                        },
                                        select: function(args) {
                                            $.ajax({
                                                url: createURL("listOsTypes"),
                                                dataType: "json",
                                                async: true,
                                                success: function(json) {
                                                    var ostypeObjs = json.listostypesresponse.ostype;
                                                    var items = [];
                                                    //items.push({id: "", description: "None"}); //shouldn't have None option when bootable is checked
                                                    $(ostypeObjs).each(function() {
                                                        items.push({
                                                            id: this.id,
                                                            description: this.description
                                                        });
                                                    });
                                                    args.response.success({
                                                        data: items
                                                    });
                                                }
                                            });
                                        }
                                    },

                                    isExtractable: {
                                        label: "label.extractable",
                                        docID: 'helpRegisterISOExtractable',
                                        isBoolean: true
                                    },

                                    isPublic: {
                                        label: "label.public",
                                        docID: 'helpRegisterISOPublic',
                                        isBoolean: true,
                                        isHidden: true
                                    },

                                    isFeatured: {
                                        label: "label.featured",
                                        docID: 'helpRegisterISOFeatured',
                                        isBoolean: true,
                                        isHidden: true
                                    }
                                }
                            },


                            action: function(args) {
                                var data = {
                                    name: args.data.name,
                                    displayText: args.data.description,
                                    url: args.data.url,
                                    zoneid: args.data.zone,
                                    isextractable: (args.data.isExtractable == "on"),
                                    bootable: (args.data.isBootable == "on"),
                                    directdownload: (args.data.directdownload == "on")
                                };

                                if (args.$form.find('.form-item[rel=osTypeId]').css("display") != "none") {
                                    $.extend(data, {
                                        osTypeId: args.data.osTypeId
                                    });
                                }
                                if (args.$form.find('.form-item[rel=isPublic]').css("display") != "none") {
                                    $.extend(data, {
                                        ispublic: (args.data.isPublic == "on")
                                    });
                                }
                                if (args.$form.find('.form-item[rel=isFeatured]').css("display") != "none") {
                                    $.extend(data, {
                                        isfeatured: (args.data.isFeatured == "on")
                                    });
                                }
                                if (args.$form.find('.form-item[rel=checksum]').css("display") != "none") {
                                    $.extend(data, {
                                        checksum: args.data.checksum
                                    });
                                }

                                $.ajax({
                                    url: createURL('registerIso'),
                                    data: data,
                                    success: function(json) {
                                        var items = json.registerisoresponse.iso; //items might have more than one array element if it's create ISOs for all zones.
                                        args.response.success({
                                            data: items[0]
                                        });

                                        /*
                     if(items.length > 1) {
                     for(var i=1; i<items.length; i++) {
                     var $midmenuItem2 = $("#midmenu_item").clone();
                     ISOToMidmenu(items[i], $midmenuItem2);
                     bindClickToMidMenu($midmenuItem2, templateToRightPanel, ISOGetMidmenuId);
                     $("#midmenu_container").append($midmenuItem2.show());              }
                     }
                     */
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
                        uploadISOFromLocal: {
                            isHeader: true,
                            label: 'label.upload.from.local',
                            messages: {
                                notification: function(args) {
                                    return 'label.upload.iso.from.local';
                                }
                            },
                            createForm: {
                                title: 'label.upload.iso.from.local',
                                preFilter: cloudStack.preFilter.createTemplate,
                                fileUpload: {
                                    getURL: function(args) {
                                        args.data = args.formData;

                                        var data = {
                                            name: args.data.name,
                                            displayText: args.data.description,
                                            zoneid: args.data.zone,
                                            format: "ISO",
                                            isextractable: (args.data.isExtractable == "on"),
                                            bootable: (args.data.isBootable == "on"),
                                            ispublic: (args.data.isPublic == "on"),
                                            isfeatured: (args.data.isFeatured == "on")
                                        };

                                        if (args.$form.find('.form-item[rel=osTypeId]').is(':visible')) {
                                            $.extend(data, {
                                                osTypeId: args.data.osTypeId,
                                            });
                                        }

                                        $.ajax({
                                            url: createURL('getUploadParamsForIso'),
                                            data: data,
                                            async: false,
                                            success: function(json) {
                                                var uploadparams = json.postuploadisoresponse.getuploadparams;
                                                var templateId = uploadparams.id;

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
                                                message: "This ISO file has been uploaded. Please check its status at Templates menu > " + args.data.name + " > Zones tab > click a zone > Status field and Ready field."
                                            });
                                            args.response.success();
                                        }
                                    }
                                },
                                fields: {
                                    templateFileUpload: {
                                        label: 'label.local.file',
                                        isFileUpload: true,
                                        validation: {
                                            required: true
                                        }
                                    },
                                    name: {
                                        label: 'label.name',
                                        docID: 'helpRegisterISOName',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    description: {
                                        label: 'label.description',
                                        docID: 'helpRegisterISODescription',
                                        validation: {
                                            required: true
                                        }
                                    },

                                    zone: {
                                        label: 'label.zone',
                                        docID: 'helpRegisterISOZone',
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

                                    isBootable: {
                                        label: "label.bootable",
                                        docID: 'helpRegisterISOBootable',
                                        isBoolean: true,
                                        isChecked: true
                                    },

                                    osTypeId: {
                                        label: 'label.os.type',
                                        docID: 'helpRegisterISOOSType',
                                        dependsOn: 'isBootable',
                                        isHidden: false,
                                        validation: {
                                            required: true
                                        },
                                        select: function(args) {
                                            $.ajax({
                                                url: createURL("listOsTypes"),
                                                dataType: "json",
                                                async: true,
                                                success: function(json) {
                                                    var ostypeObjs = json.listostypesresponse.ostype;
                                                    var items = [];
                                                    $(ostypeObjs).each(function() {
                                                        items.push({
                                                            id: this.id,
                                                            description: this.description
                                                        });
                                                    });
                                                    args.response.success({
                                                        data: items
                                                    });
                                                }
                                            });
                                        }
                                    },

                                    isExtractable: {
                                        label: "label.extractable",
                                        docID: 'helpRegisterISOExtractable',
                                        isBoolean: true
                                    },

                                    isPublic: {
                                        label: "label.public",
                                        docID: 'helpRegisterISOPublic',
                                        isBoolean: true,
                                        isHidden: true
                                    },

                                    isFeatured: {
                                        label: "label.featured",
                                        docID: 'helpRegisterISOFeatured',
                                        isBoolean: true,
                                        isHidden: true
                                    }
                                }
                            },

                            action: function(args) {
                                return;
                            },

                            notification: {
                                poll: function(args) {
                                    args.complete();
                                }
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
                        // Due to zonal grouping, low pagesize can result lower
                        // aggregated items, resulting in no scroll shown
                        // So, use maximum pagesize
                        data.pagesize = 200;

                        var ignoreProject = false;
                        if (args.filterBy != null) { //filter dropdown
                            if (args.filterBy.kind != null) {
                                if (previousFilterType != args.filterBy.kind || args.page == 1) {
                                    previousFilterType = args.filterBy.kind;
                                    previousCollection = [];
                                }
                                switch (args.filterBy.kind) {
                                    case "all":
                                        $.extend(data, {
                                            isofilter: 'all'
                                        });
                                        break;
                                    case "mine":
                                        $.extend(data, {
                                            isofilter: 'self'
                                        });
                                        break;
                                    case "shared":
                                        $.extend(data, {
                                            isofilter: 'shared'
                                        });
                                        break;
                                    case "featured":
                                        ignoreProject = true;
                                        $.extend(data, {
                                            isofilter: 'featured'
                                        });
                                        break;
                                    case "community":
                                        ignoreProject = true;
                                        $.extend(data, {
                                            isofilter: 'community'
                                        });
                                        break;
                                }
                            }
                        }

                        $.ajax({
                            url: createURL('listIsos', {
                                ignoreProject: ignoreProject
                            }),
                            data: data,
                            success: function(json) {
                                var items = json.listisosresponse.iso;

                                var itemsView = [];
                                $(items).each(function(index, item) {
                                    var existing = $.grep(previousCollection, function(it){
                                        return it != null && it.id !=null && it.id == item.id;
                                    });


                                    if (existing.length > 0) {
                                        return true; // skip adding this entry
                                    } else {
                                        var isoItem = {
                                            id: item.id,
                                            name: item.name,
                                            description: item.description,
                                            ostypename: item.ostypename,
                                            ostypeid: item.ostypeid,
                                            account: item.account,
                                            domain: item.domain,
                                            zones: item.zonename,
                                            zoneids: [item.zoneid]
                                        };
                                        itemsView.push(isoItem);
                                        previousCollection.push(isoItem);
                                    }
                                }
);
                                args.response.success({
                                    actionFilter: isoActionfilter,
                                    data: itemsView
                                });
                            }
                        });
                    },

                    detailView: {
                        name: 'label.details',
                        viewAll: {
                            label: 'label.instances',
                            path: 'instances'
                        },
                        actions: {
                            edit: {
                                label: 'label.edit',
                                action: function(args) {
                                    //***** updateIso *****
                                    var data = {
                                        id: args.context.isos[0].id,
                                        //zoneid: args.context.isos[0].zoneid, //can't update template/ISO in only one zone. It always get updated in all zones.
                                        name: args.data.name,
                                        displaytext: args.data.displaytext,
                                        ostypeid: args.data.ostypeid
                                    };
                                    $.ajax({
                                        url: createURL('updateIso'),
                                        data: data,
                                        async: false,
                                        success: function(json) {
                                            //updateIso API returns an incomplete ISO object (isextractable and isfeatured are missing)
                                        }
                                    });


                                    //***** updateIsoPermissions *****
                                    var data = {
                                        id: args.context.isos[0].id
                                        //zoneid: args.context.isos[0].zoneid //can't update template/ISO in only one zone. It always get updated in all zones.
                                    };
                                    //if args.data.ispublic is undefined(i.e. checkbox is hidden), do not pass ispublic to API call.
                                    if (args.data.ispublic == "on") {
                                        $.extend(data, {
                                            ispublic: true
                                        });
                                    } else if (args.data.ispublic == "off") {
                                        $.extend(data, {
                                            ispublic: false
                                        });
                                    }
                                    //if args.data.isfeatured is undefined(i.e. checkbox is hidden), do not pass isfeatured to API call.
                                    if (args.data.isfeatured == "on") {
                                        $.extend(data, {
                                            isfeatured: true
                                        });
                                    } else if (args.data.isfeatured == "off") {
                                        $.extend(data, {
                                            isfeatured: false
                                        });
                                    }
                                    //if args.data.isextractable is undefined(i.e. checkbox is hidden), do not pass isextractable to API call.
                                    if (args.data.isextractable == "on") {
                                        $.extend(data, {
                                            isextractable: true
                                        });
                                    } else if (args.data.isextractable == "off") {
                                        $.extend(data, {
                                            isextractable: false
                                        });
                                    }
                                    $.ajax({
                                        url: createURL('updateIsoPermissions'),
                                        data: data,
                                        async: false,
                                        success: function(json) {
                                            //updateIsoPermissions API doesn't return ISO object
                                        }
                                    });


                                    //***** listIsos *****
                                    //So, we call listIsos API to get a complete ISO object
                                    var data = {
                                        id: args.context.isos[0].id,
                                        zoneid: args.context.isos[0].zoneid,
                                        isofilter: 'self'
                                    };
                                    $.ajax({
                                        url: createURL('listIsos'),
                                        data: data,
                                        async: false,
                                        success: function(json) {
                                            var item = json.listisosresponse.iso;
                                            args.response.success({
                                                data: item
                                            });
                                        }
                                    });
                                }
                            },
                            downloadISO: {
                                label: 'label.action.download.ISO',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.action.download.iso';
                                    },
                                    notification: function(args) {
                                        return 'label.action.download.ISO';
                                    },
                                    complete: function(args) {
                                        var url = args.url;
                                        var htmlMsg = _l('message.download.ISO');
                                        var htmlMsg2 = htmlMsg.replace(/#/, url).replace(/00000/, url);
                                        return htmlMsg2;
                                    }
                                },
                                action: function(args) {
                                    var apiCmd = "extractIso&mode=HTTP_DOWNLOAD&id=" + args.context.isos[0].id;
                                    if (args.context.isos[0].zoneid != null)
                                        apiCmd += "&zoneid=" + args.context.isos[0].zoneid;

                                    $.ajax({
                                        url: createURL(apiCmd),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var jid = json.extractisoresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid,
                                                    getUpdatedItem: function(json) {
                                                        return json.queryasyncjobresultresponse.jobresult.iso;
                                                    },
                                                    getActionFilter: function() {
                                                        return isoActionfilter;
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
                                    directdownload: {
                                        label: 'label.direct.download',
                                        isBoolean: true,
                                        converter: cloudStack.converters.toBooleanText
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
                                    isextractable: {
                                        label: 'label.extractable.lower',
                                        isBoolean: true,
                                        isEditable: function() {
                                            if (isAdmin())
                                                return true;
                                            else
                                                return false;
                                        },
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    bootable: {
                                        label: 'label.bootable',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    ispublic: {
                                        label: 'label.public',
                                        isBoolean: true,
                                        isEditable: true,
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    isfeatured: {
                                        label: 'label.featured',
                                        isBoolean: true,
                                        isEditable: function() {
                                            if (isAdmin())
                                                return true;
                                            else
                                                return false;
                                        },
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    crossZones: {
                                        label: 'label.cross.zones',
                                        converter: cloudStack.converters.toBooleanText
                                    },

                                    ostypeid: {
                                        label: 'label.os.type',
                                        isEditable: true,
                                        select: function(args) {
                                            if (ostypeObjs == undefined) {
                                                $.ajax({
                                                    url: createURL("listOsTypes"),
                                                    dataType: "json",
                                                    async: false,
                                                    success: function(json) {
                                                        ostypeObjs = json.listostypesresponse.ostype;
                                                    }
                                                });
                                            }
                                            var items = [];
                                            $(ostypeObjs).each(function() {
                                                items.push({
                                                    id: this.id,
                                                    description: this.description
                                                });
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
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
                                    resourceType: 'ISO',
                                    contextId: 'isos'
                                }),

                                dataProvider: function(args) {
                                    var jsonObj = args.context.isos[0];
                                    var apiCmd = "listIsos&isofilter=self&id=" + jsonObj.id;
                                    if (jsonObj.zoneid != null)
                                        apiCmd = apiCmd + "&zoneid=" + jsonObj.zoneid;

                                    $.ajax({
                                        url: createURL(apiCmd),
                                        dataType: "json",
                                        success: function(json) {
                                            args.response.success({
                                                actionFilter: isoActionfilter,
                                                data: json.listisosresponse.iso[0]
                                            });
                                        }
                                    });

                                }
                            },
                            zones: {
                                title: 'label.zones',
                                listView: {
                                    id: 'zones',
                                    fields: {
                                        zonename: {
                                            label: 'label.name'
                                        },
                                        status: {
                                            label: 'label.status'
                                        },
                                        isready: {
                                            label: 'state.Ready',
                                            converter: cloudStack.converters.toBooleanText
                                        }
                                    },
                                    hideSearchBar: true,

                                    dataProvider: function(args) {
                                                var data = {
                                                    isofilter: 'self',
                                                    id: args.context.isos[0].id
                                                };
                                                listViewDataProvider(args, data);
                                                $.ajax({
                                                    url: createURL('listIsos'),
                                                    data: data,
                                                    dataType: "json",
                                                    success: function(json) {
                                                            var isos = json.listisosresponse.iso;
                                                            var zones = [];
                                                            zones = isos;

                                                args.response.success({
                                                            actionFilter: isoActionfilter,
                                                            data: zones
                                                });
                                        }
                                    });
                                },

                                detailView: {
                                    actions: {
                                        copyISO: {
                                            label: 'label.action.copy.ISO',
                                            messages: {
                                                notification: function(args) {
                                                    return 'label.copying.iso';
                                                }
                                            },
                                            createForm: {
                                                title: 'label.action.copy.ISO',
                                                desc: 'label.action.copy.ISO',
                                                fields: {
                                                    destinationZoneId: {
                                                        label: 'label.destination.zone',
                                                        validation: {
                                                            required: true
                                                        },
                                                        select: function(args) {
                                                            $.ajax({
                                                                url: createURL("listZones&available=true"),
                                                                dataType: "json",
                                                                async: true,
                                                                success: function(json) {
                                                                    var zoneObjs = [];
                                                                    var items = json.listzonesresponse.zone;
                                                                    if (items != null) {
                                                                        for (var i = 0; i < items.length; i++) {
                                                                            if (items[i].id != args.context.zones[0].zoneid) {
                                                                                zoneObjs.push({
                                                                                    id: items[i].id,
                                                                                    description: items[i].name
                                                                                });
                                                                            }
                                                                        }
                                                                    }
                                                                    args.response.success({
                                                                        data: zoneObjs
                                                                    });
                                                                }
                                                            });
                                                        }
                                                    }
                                                }
                                            },
                                            action: function(args) {
                                                var data = {
                                                    id: args.context.isos[0].id,
                                                    destzoneid: args.data.destinationZoneId
                                                };
                                                if (args.context.zones[0].zoneid != undefined) {
                                                    $.extend(data, {
                                                        sourcezoneid: args.context.zones[0].zoneid
                                                    });
                                                }

                                                $.ajax({
                                                    url: createURL('copyIso'),
                                                    data: data,
                                                    success: function(json) {
                                                        var jid = json.copytemplateresponse.jobid;
                                                        args.response.success({
                                                            _custom: {
                                                                jobId: jid,
                                                                getUpdatedItem: function(json) {
                                                                    return {}; //nothing in this ISO needs to be updated
                                                                },
                                                                getActionFilter: function() {
                                                                    return isoActionfilter;
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

                                        remove: {
                                            label: 'label.action.delete.ISO',
                                            messages: {
                                                confirm: function(args) {
                                                    return 'message.action.delete.ISO';
                                                },
                                                notification: function(args) {
                                                    return 'label.action.delete.ISO';
                                                }
                                            },
                                            action: function(args) {
                                                var queryParams = "deleteIso&id=" + args.context.isos[0].id;
                                                if (!args.context.isos[0].crossZones){
                                                    queryParams += "&zoneid=" + args.context.zones[0].zoneid;
                                                }
                                                $.ajax({
                                                    url: createURL(queryParams),
                                                    dataType: "json",
                                                    async: true,
                                                    success: function(json) {
                                                        var jid = json.deleteisoresponse.jobid;
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
                                                zonename: {
                                                    label: 'label.zone.name'
                                                },
                                                zoneid: {
                                                    label: 'label.zone.id'
                                                },
                                                isready: {
                                                    label: 'state.Ready',
                                                    converter: cloudStack.converters.toBooleanText
                                                },
                                                status: {
                                                    label: 'label.status'
                                                }
                                             },{
                                                displaytext: {
                                                    label: 'label.description',
                                                    isEditable: true,
                                                    validation: {
                                                        required: true
                                                    }
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
                                                isextractable: {
                                                    label: 'label.extractable.lower',
                                                    isBoolean: true,
                                                    isEditable: true,
                                                    converter: cloudStack.converters.toBooleanText
                                                },
                                                bootable: {
                                                    label: 'label.bootable',
                                                    converter: cloudStack.converters.toBooleanText
                                                },
                                                ispublic: {
                                                    label: 'label.public',
                                                    isBoolean: true,
                                                    isEditable: true,
                                                    converter: cloudStack.converters.toBooleanText
                                                },
                                                isfeatured: {
                                                    label: 'label.featured',
                                                    isBoolean: true,
                                                    isEditable: function() {
                                                        if (isAdmin())
                                                            return true;
                                                        else
                                                            return false;
                                                    },
                                                    converter: cloudStack.converters.toBooleanText
                                                },

                                                ostypeid: {
                                                    label: 'label.os.type',
                                                    isEditable: true,
                                                    select: function(args) {
                                                        var ostypeObjs;
                                                        $.ajax({
                                                            url: createURL("listOsTypes"),
                                                            dataType: "json",
                                                            async: false,
                                                            success: function(json) {
                                                                ostypeObjs = json.listostypesresponse.ostype;
                                                            }
                                                        });

                                                        var items = [];
                                                        $(ostypeObjs).each(function() {
                                                            items.push({
                                                                id: this.id,
                                                                description: this.description
                                                            });
                                                        });
                                                        args.response.success({
                                                            data: items
                                                        });
                                                    }
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
                                                resourceType: 'ISO',
                                                contextId: 'isos'
                                            }),

                                            dataProvider: function(args) {
                                                var jsonObj = args.context.isos[0];
                                                var apiCmd = "listIsos&isofilter=self&id=" + jsonObj.id;
                                                if (jsonObj.zoneid != null)
                                                    apiCmd = apiCmd + "&zoneid=" + args.context.zones[0].zoneid;

                                                $.ajax({
                                                    url: createURL(apiCmd),
                                                    dataType: "json",
                                                    success: function(json) {
                                                        args.response.success({
                                                            actionFilter: isoActionfilter,
                                                            data: json.listisosresponse.iso[0]
                                                        });
                                                    }
                                                });

                                            }
                                        }
                                    }
                                }}
                            }
                        }
                    }
                }
            }
        }
    };

    var templateActionfilter = function(args) {
        var jsonObj = args.context.item;
        var allowedActions = [];

        // "Edit Template", "Copy Template", "Create VM"
        if ((isAdmin() == false && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account) && !(jsonObj.domainid == g_domainid && cloudStack.context.projects && jsonObj.projectid == cloudStack.context.projects[0].id)) //if neither root-admin, nor the same account, nor the same project
            || jsonObj.templatetype == "SYSTEM" || jsonObj.isready == false) {
            //do nothing
        } else {
            allowedActions.push("edit");

            allowedActions.push("copyTemplate");
        }

        // "Download Template" , "Update Template Permissions"
        if (((isAdmin() == false && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account) && !(jsonObj.domainid == g_domainid && cloudStack.context.projects && jsonObj.projectid == cloudStack.context.projects[0].id))) //if neither root-admin, nor the same account, nor the same project
            || (jsonObj.isready == false) || jsonObj.templatetype == "SYSTEM") {
            //do nothing
        } else {
            if (jsonObj.isextractable){
                allowedActions.push("downloadTemplate");
            }
            allowedActions.push("shareTemplate");
        }

        // "Delete Template"
        //if (((isUser() && jsonObj.ispublic == true && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account)))
        if (((isAdmin() == false && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account) && !(jsonObj.domainid == g_domainid && cloudStack.context.projects && jsonObj.projectid == cloudStack.context.projects[0].id))) //if neither root-admin, nor the same account, nor the same project
            || (jsonObj.isready == false && jsonObj.status != null && jsonObj.status.indexOf("Downloaded") != -1) || (jsonObj.account == "system")) {
            //do nothing
        } else {
            allowedActions.push("remove");
        }

        return allowedActions;
    }

    var isoActionfilter = function(args) {
        var jsonObj = args.context.item;
        var allowedActions = [];

        if ((isAdmin() == false && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account) && !(jsonObj.domainid == g_domainid && cloudStack.context.projects && jsonObj.projectid == cloudStack.context.projects[0].id)) //if neither root-admin, nor the same account, nor the same project
            || (jsonObj.isready == false) || (jsonObj.domainid == 1 && jsonObj.account == "system")
        ) {
            //do nothing
        } else {
            allowedActions.push("edit");

            allowedActions.push("copyISO");
        }

        // "Create VM"
        // Commenting this out for Beta2 as it does not support the new network.
        /*
     //if (((isUser() && jsonObj.ispublic == true && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account) && !(jsonObj.domainid == g_domainid && cloudStack.context.projects && jsonObj.projectid == cloudStack.context.projects[0].id))  //if neither root-admin, nor the same account, nor the same project
     if (((isAdmin() == false && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account))  //if neither root-admin, nor item owner
     || jsonObj.isready == false)
     || (jsonObj.bootable == false)
     || (jsonObj.domainid ==    1 && jsonObj.account ==    "system")
     ) {
       //do nothing
     }
     else {
       allowedActions.push("createVm");
     }
     */

        // "Download ISO"
        //if (((isUser() && jsonObj.ispublic == true && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account)))
        if (((isAdmin() == false && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account) && !(jsonObj.domainid == g_domainid && cloudStack.context.projects && jsonObj.projectid == cloudStack.context.projects[0].id))) //if neither root-admin, nor the same account, nor the same project
            || (jsonObj.isready == false) || (jsonObj.domainid == 1 && jsonObj.account == "system")
        ) {
            //do nothing
        } else {
            allowedActions.push("downloadISO");
        }

        // "Delete ISO"
        //if (((isUser() && jsonObj.ispublic == true && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account)))
        if (((isAdmin() == false && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account) && !(jsonObj.domainid == g_domainid && cloudStack.context.projects && jsonObj.projectid == cloudStack.context.projects[0].id))) //if neither root-admin, nor the same account, nor the same project
            || (jsonObj.isready == false && jsonObj.status != null && jsonObj.status.indexOf("Downloaded") != -1) || (jsonObj.account == "system")
        ) {
            //do nothing
        } else {
            allowedActions.push("remove");
        }

        return allowedActions;
    }

})(cloudStack, jQuery);
