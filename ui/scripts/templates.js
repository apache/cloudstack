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
                                if (isAdmin()) //"listTemplates&templatefilter=all" only works for root-admin, but no domain-admin. Domain-admin is unable to see all templates until listTemplates API supports a new type of templatefilter for domain-admin to see all templates in his domain.
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
                    fields: {
                        name: {
                            label: 'label.name'
                        },
                        hypervisor: {
                            label: 'label.hypervisor'
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

                                            var apiCmd;
                                            if (args.zone == -1) { //All Zones
                                                //apiCmd = "listHypervisors&zoneid=-1"; //"listHypervisors&zoneid=-1" has been changed to return only hypervisors available in all zones (bug 8809)
                                                apiCmd = "listHypervisors";
                                            }
                                            else {
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
                                                } else if ($(this).val() == "XenServer") {
                                                    $form.find('.form-item[rel=rootDiskControllerType]').hide();
                                                    $form.find('.form-item[rel=nicAdapterType]').hide();
                                                    $form.find('.form-item[rel=keyboardType]').hide();

                                                    if (isAdmin())
                                                        $form.find('.form-item[rel=xenserverToolsVersion61plus]').css('display', 'inline-block');
                                                } else {
                                                    $form.find('.form-item[rel=rootDiskControllerType]').hide();
                                                    $form.find('.form-item[rel=nicAdapterType]').hide();
                                                    $form.find('.form-item[rel=keyboardType]').hide();

                                                    $form.find('.form-item[rel=xenserverToolsVersion61plus]').hide();
                                                }
                                            });

                                            args.$select.trigger('change');
                                        }
                                    },

                                    xenserverToolsVersion61plus: {
                                        label: 'label.xenserver.tools.version.61.plus',
                                        isBoolean: true,
                                        isChecked: function (args) {
                                            var b = false;
                                            if (isAdmin()) {
                                                $.ajax({
                                                    url: createURL('listConfigurations'),
                                                    data: {
                                                        name: 'xenserver.pvdriver.version'
                                                    },
                                                    async: false,
                                                    success: function (json) {
                                                        if (json.listconfigurationsresponse.configuration != null && json.listconfigurationsresponse.configuration[0].value == 'xenserver61') {
                                                            b = true;
                                                        }
                                                    }
                                                });
                                            }
                                            return b;
                                        },
                                        isHidden: true
                                    },

                                    //fields for hypervisor == "VMware" (starts here)
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
                                            items.push({
                                                id: "us",
                                                description: "US Keboard"
                                            });
                                            items.push({
                                                id: "uk",
                                                description: "UK Keyboard"
                                            });
                                            items.push({
                                                id: "jp",
                                                description: "Japanese Keyboard"
                                            });
                                            items.push({
                                                id: "sc",
                                                description: "Simplified Chinese"
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    //fields for hypervisor == "VMware" (ends here)

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
                                var data = {
                                    name: args.data.name,
                                    displayText: args.data.description,
                                    url: args.data.url,
                                    zoneid: args.data.zone,
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

                                //XenServer only (starts here)
                                if (args.$form.find('.form-item[rel=xenserverToolsVersion61plus]').css("display") != "none") {
                                    $.extend(data, {
                                        'details[0].hypervisortoolsversion': (args.data.xenserverToolsVersion61plus == "on") ? "xenserver61" : "xenserver56"
                                    });
                                }
                                //XenServer only (ends here)

                                //VMware only (starts here)
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
                                //VMware only (ends here)

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
                                        }
                                    },

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

                        var ignoreProject = false;
                        if (args.filterBy != null) { //filter dropdown
                            if (args.filterBy.kind != null) {
                                if (previousFilterType != args.filterBy.kind || args.page == 1) {
                                    previousFilterType = args.filterBy.kind;
                                    previousCollection = [];
                                }
                                switch (args.filterBy.kind) {
                                    case "all":
                                        ignoreProject = true;
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
                                                 messages: {
                                                     confirm: function(args) {
                                                         return 'message.action.delete.template';
                                                     },
                                                     notification: function(args) {
                                                         return 'label.action.delete.template';
                                                     }
                                                 },
                                                 action: function(args) {
                                                     $.ajax({
                                                         url: createURL("deleteTemplate&id=" + args.context.templates[0].id + "&zoneid=" + args.context.zones[0].zoneid),
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
                                                     confirm: function(args) {
                                                         return 'message.copy.template.confirm';
                                                     },
                                                     success: function(args) {
                                                         return 'message.template.copying';
                                                     },
                                                     notification: function(args) {
                                                         return 'label.action.copy.template';
                                                     }
                                                 },
                                                 createForm: {
                                                     title: 'label.action.copy.template',
                                                     desc: '',
                                                     fields: {
                                                         destinationZoneId: {
                                                             label: 'label.destination.zone',
                                                             docID: 'helpCopyTemplateDestination',
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
                                                                                 if (args.context.zones[0].zoneid != items[i].id) {
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
                                                         id: args.context.templates[0].id,
                                                         destzoneid: args.data.destinationZoneId
                                                     };
                                                     $.extend(data, {
                                                         sourcezoneid: args.context.zones[0].zoneid
                                                     });

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
                                                    isEditable: function() {
                                                        if (isAdmin())
                                                            return true;
                                                        else
                                                            return false;
                                                    },
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
                                if (isAdmin()) //"listIsos&filter=all" only works for root-admin, but no domain-admin. Domain-admin is unable to see all Isos until listIsos API supports a new type of isofilter for domain-admin to see all Isos in his domain.
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
                    fields: {
                        name: {
                            label: 'label.name'
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
                                    bootable: (args.data.isBootable == "on")
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

                        var ignoreProject = false;
                        if (args.filterBy != null) { //filter dropdown
                            if (args.filterBy.kind != null) {
                                if (previousFilterType != args.filterBy.kind || args.page == 1) {
                                    previousFilterType = args.filterBy.kind;
                                    previousCollection = [];
                                }
                                switch (args.filterBy.kind) {
                                    case "all":
                                        ignoreProject = true;
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
                                            ostypeid: item.ostypeid,
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
                                                var array1 = [];
                                                if (args.context.zones[0].zoneid != null)
                                                    array1.push("&zoneid=" + args.context.zones[0].zoneid);

                                                $.ajax({
                                                    url: createURL("deleteIso&id=" + args.context.isos[0].id + "&zoneid=" + args.context.zones[0].zoneid),
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

        // "Download Template"
        if (((isAdmin() == false && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account) && !(jsonObj.domainid == g_domainid && cloudStack.context.projects && jsonObj.projectid == cloudStack.context.projects[0].id))) //if neither root-admin, nor the same account, nor the same project
            || (jsonObj.isready == false) || jsonObj.templatetype == "SYSTEM") {
            //do nothing
        } else {
            allowedActions.push("downloadTemplate");
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
