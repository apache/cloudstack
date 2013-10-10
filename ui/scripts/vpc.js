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
(function($, cloudStack) {
    var aclMultiEdit = {
        noSelect: true,
       
        reorder: {
            moveDrag: {
                action: function(args) {
                    var rule = args.context.multiRule[0];
                    var number = 0;
                    var prevItem = args.prevItem ? args.prevItem.number : null;
                    var nextItem = args.nextItem ? args.nextItem.number : null;

                    if (!nextItem) { // Last item
                        number = prevItem + 100;
                    } else {
                        if (nextItem - prevItem <= 10) {
                            number = nextItem - parseInt(((nextItem - prevItem) / 2));
                        } else {
                            number = nextItem > 1 ? nextItem - 10 : 1;
                        }
                    }

                    $.ajax({
                        url: createURL('updateNetworkACLItem'),
                        data: {
                            id: rule.id,
                            number: number
                        },
                        success: function(json) {
                            var pollTimer = setInterval(function() {
                                pollAsyncJobResult({
                                    _custom: {
                                        jobId: json.createnetworkaclresponse.jobid
                                    },
                                    complete: function() {
                                        clearInterval(pollTimer);
                                        args.response.success();
                                    },
                                    error: function(errorMsg) {
                                        clearInterval(pollTimer);
                                        args.response.error(errorMsg);
                                    }
                                });
                            }, 1000);
                        }
                    });
                }
            }
        },
        fields: {

            'number': {
                label: 'Rule Number',
                edit: true,
                isEditable: true

            },

            'cidrlist': {
                edit: true,
                label: 'label.cidr',
                isEditable: true
            },
            action: {
                label: 'Action',
                isEditable: true,
                select: function(args) {
                    args.response.success({
                        data: [{
                            name: 'Allow',
                            description: 'Allow'
                        }, {
                            name: 'Deny',
                            description: 'Deny'
                        }]
                    });
                }
            },
            'protocol': {
                label: 'label.protocol',
                isEditable: true,
                select: function(args) {
                    var isEditDialog = args.type === 'createForm';

                    args.$select.change(function() {
                        var $inputs, $icmpFields, $otherFields, $portFields, $protocolFields, $protocolinput;

                        //
                        // Editing existing rules in createForm dialog
                        //
                        if (isEditDialog) {
                            $inputs = args.$form.find('.form-item');
                            $icmpFields = $inputs.filter(function() {
                                var name = $(this).attr('rel');

                                return $.inArray(name, [
                                    'icmptype',
                                    'icmpcode'
                                ]) > -1;
                            });
                            $otherFields = $inputs.filter(function() {
                                var name = $(this).attr('rel');

                                return name != 'protocolnumber' &&
                                    name != 'icmptype' &&
                                    name != 'icmpcode' &&
                                    name != 'cidrlist' &&
                                    name != 'number';
                            });
                            $portFields = $inputs.filter(function() {
                                var name = $(this).attr('rel');
                                return $.inArray(name, [
                                    'startport',
                                    'endport'
                                ]) > -1;
                            });
                            $protocolFields = $inputs.filter(function() {
                                var name = $(this).attr('rel');

                                return $.inArray(name, ['protocolnumber']) > -1;
                            });

                            if ($(this).val() == 'protocolnumber') {
                                $icmpFields.show();
                                $portFields.show();
                                $protocolFields.show();
                                $portFields.show();
                            } else if ($(this).val() == 'icmp') {
                                $icmpFields.show();
                                $protocolFields.hide();
                                $portFields.hide();
                            } else if ($(this).val() == 'all') {
                                $portFields.hide();
                            } else {
                                $otherFields.show();
                                $icmpFields.hide();
                                $protocolFields.hide();

                            }
                        } else {
                            //
                            // Add new form
                            //
                            $inputs = args.$form.find('input');
                            $icmpFields = $inputs.filter(function() {
                                var name = $(this).attr('name');

                                return $.inArray(name, [
                                    'icmptype',
                                    'icmpcode'
                                ]) > -1;
                            });
                            $otherFields = $inputs.filter(function() {
                                var name = $(this).attr('name');

                                return name != 'protocolnumber' &&
                                    name != 'icmptype' &&
                                    name != 'icmpcode' &&
                                    name != 'cidrlist' &&
                                    name != 'number';
                            });
                            $portFields = $inputs.filter(function() {
                                var name = $(this).attr('name');
                                return $.inArray(name, [
                                    'startport',
                                    'endport'
                                ]) > -1;
                            });

                            $protocolinput = args.$form.find('td input');
                            $protocolFields = $protocolinput.filter(function() {
                                var name = $(this).attr('name');

                                return $.inArray(name, ['protocolnumber']) > -1;
                            });

                            if ($(this).val() == 'protocolnumber') {
                                $icmpFields.show();
                                $otherFields.hide();
                                $protocolFields.show().addClass('required');
                                $inputs.filter('[name=startport],[name=endport]').show().attr('disabled', false);
                            } else if ($(this).val() == 'icmp') {
                                $icmpFields.show();
                                $icmpFields.attr('disabled', false);
                                $protocolFields.hide().removeClass('required');
                                $otherFields.attr('disabled', 'disabled');
                                $otherFields.hide();
                                $otherFields.parent().find('label.error').hide();
                            } else if ($(this).val() == 'all') {
                                $portFields.attr('disabled', 'disabled');
                                $portFields.hide();
                            } else {
                                $otherFields.show();
                                $otherFields.parent().find('label.error').hide();
                                $otherFields.attr('disabled', false);
                                $icmpFields.attr('disabled', 'disabled');
                                $icmpFields.hide();
                                $icmpFields.parent().find('label.error').hide();
                                $protocolFields.hide().removeClass('required');
                            }
                        }
                    });

                    args.response.success({
                        data: [{
                                name: 'tcp',
                                description: 'TCP'
                            }, {
                                name: 'udp',
                                description: 'UDP'
                            }, {
                                name: 'icmp',
                                description: 'ICMP'
                            }, {
                                name: 'all',
                                description: 'ALL'
                            }, {
                                name: 'protocolnumber',
                                description: 'Protocol Number'
                            }

                        ]
                    });

                    setTimeout(function() {
                        args.$select.trigger('change');
                    }, 100);
                }
            },

            'protocolnumber': {
                label: 'Protocol Number',
                edit: true,
                isEditable: true
            },
            'startport': {
                edit: true,
                label: 'label.start.port',
                isOptional: true,
                isEditable: true
            },
            'endport': {
                edit: true,
                label: 'label.end.port',
                isOptional: true,
                isEditable: true
            },
            'networkid': {
                label: 'Select Tier',
                select: function(args) {
                    var data = {
                        listAll: true,
                        vpcid: args.context.vpc[0].id
                    };

                    // Only show selected tier, if viewing from detail view
                    if (args.context.networks &&
                        args.context.networks[0] &&
                        args.context.networks[0].vpcid) {
                        $.extend(data, {
                            id: args.context.networks[0].id
                        });
                    }

                    //  Ajax Call to display the Tiers
                    $.ajax({
                        url: createURL('listNetworks'),
                        data: data,
                        success: function(json) {
                            var networks = json.listnetworksresponse.network;

                            args.response.success({
                                data: $(networks).map(function(index, network) {
                                    return {
                                        name: network.id,
                                        description: network.name
                                    };
                                })
                            });
                        }
                    });
                }
            },
            'icmptype': {
                edit: true,
                label: 'ICMP.type',
                isDisabled: true,
                desc: 'Please specify -1 if you want to allow all ICMP types',
                defaultValue: '-1',
                isEditable: true
            },
            'icmpcode': {
                edit: true,
                label: 'ICMP.code',
                isDisabled: true,
                desc: 'Please specify -1 if you want to allow all ICMP codes',
                defaultValue: '-1',
                isEditable: true
            },
            'traffictype': {
                label: 'label.traffic.type',
                isEditable: true,
                select: function(args) {
                    args.response.success({
                        data: [{
                            name: 'Ingress',
                            description: 'Ingress'
                        }, {
                            name: 'Egress',
                            description: 'Egress'
                        }]
                    });
                }
            },
            'add-rule': {
                label: 'label.add.rule',
                addButton: true
            }
        },

        tags: cloudStack.api.tags({
            resourceType: 'NetworkACL',
            contextId: 'multiRule'
        }),

        add: {
            label: 'label.add',
            action: function(args) {
                var $multi = args.$multi;
                //Support for Protocol Number between 0 to 255
                if (args.data.protocol === 'protocolnumber') {
                    $.extend(args.data, {
                        protocol: args.data.protocolnumber
                    });
                    delete args.data.protocolnumber;
                    delete args.data.icmptype;
                    delete args.data.icmpcode;
                } else {
                    delete args.data.protocolnumber;
                }

                if ((args.data.protocol == 'tcp' || args.data.protocol == 'udp') && (args.data.startport == "" || args.data.startport == undefined)) {
                    cloudStack.dialog.notice({
                        message: _l('Start Port or End Port value should not be blank')
                    });
                    $(window).trigger('cloudStack.fullRefresh');
                } else if ((args.data.protocol == 'tcp' || args.data.protocol == 'udp') && (args.data.endport == "" || args.data.endport == undefined)) {
                    cloudStack.dialog.notice({
                        message: _l('Start Port or End Port value should not be blank')
                    });
                    $(window).trigger('cloudStack.fullRefresh');
                } else {
                    $.ajax({
                        url: createURL('createNetworkACL'),
                        data: $.extend(args.data, {
                            aclid: args.context.aclLists[0].id
                        }),
                        dataType: 'json',
                        success: function(data) {
                            args.response.success({
                                _custom: {
                                    jobId: data.createnetworkaclresponse.jobid,
                                    getUpdatedItem: function(json) {
                                        $(window).trigger('cloudStack.fullRefresh');

                                        return data;
                                    }
                                },
                                notification: {
                                    label: 'label.add.ACL',
                                    poll: pollAsyncJobResult
                                }
                            });
                        },
                        error: function(data) {
                            args.response.error(parseXMLHttpResponse(data));
                        }
                    });
                }
            }
        },
        actions: {
            edit: {
                label: 'label.edit',
                action: function(args) {
                    var data = {
                        id: args.context.multiRule[0].id,
                        cidrlist: args.data.cidrlist,
                        number: args.data.number,
                        protocol: args.data.protocol,
                        traffictype: args.data.traffictype,
                        action: args.data.action
                    };

                    if (data.protocol === 'tcp' || data.protocol === 'udp') {
                        $.extend(data, {
                            startport: args.data.startport,
                            endport: args.data.endport
                        });
                    } else if (data.protocol === 'icmp') {
                        $.extend(data, {
                            icmptype: args.data.icmptype,
                            icmpcode: args.data.icmpcode
                        });
                    } else if (data.protocol === 'protocolnumber') {
                        $.extend(data, {
                            protocol: args.data.protocolnumber,
                            startport: args.data.startport,
                            endport: args.data.endport
                        });

                        delete args.data.protocolnumber;
                    }

                    $.ajax({
                        url: createURL('updateNetworkACLItem'),
                        data: data,
                        success: function(json) {
                            args.response.success({
                                _custom: {
                                    jobId: json.createnetworkaclresponse.jobid
                                }, // API response obj name needs to be fixed
                                notification: {
                                    label: 'Edit ACL rule',
                                    poll: pollAsyncJobResult
                                }
                            });
                        },
                        error: function(error) {
                            args.response.error(parseXMLHttpResponse(error));
                        }
                    });
                }
            },
            destroy: {
                label: 'label.remove.ACL',
                action: function(args) {
                    $.ajax({
                        url: createURL('deleteNetworkACL'),
                        data: {
                            id: args.context.multiRule[0].id
                        },
                        dataType: 'json',
                        async: true,
                        success: function(data) {
                            var jobID = data.deletenetworkaclresponse.jobid;
                            args.response.success({
                                _custom: {
                                    jobId: jobID,
                                    getUpdateIdtem: function() {
                                        $(window).trigger('cloudStack.fullRefresh');
                                    }
                                },
                                notification: {
                                    label: 'label.remove.ACL',
                                    poll: pollAsyncJobResult
                                }
                            });
                        },
                        error: function(data) {
                            args.response.error(parseXMLHttpResponse(data));
                        }
                    });
                }
            }
        },
        dataProvider: function(args) {
            var $multi = args.$multi;
            var data = {
                vpcid: args.context.vpc[0].id,
                listAll: true
            };

            if (!$multi.data('acl-rules')) {
                $multi.data('acl-rules', []);
            }

            if (args.context.networks &&
                args.context.networks[0] &&
                args.context.networks[0].vpcid) {
                data.networkid = args.context.networks[0].id;

                $.ajax({
                    url: createURL('listNetworkACLs'),
                    data: data,
                    dataType: 'json',
                    async: true,
                    success: function(json) {
                        args.response.success({
                            data: $(json.listnetworkaclsresponse.networkacl).map(function(index, acl) {
                                return $.extend(acl, {
                                    networkid: args.context.networks[0].name
                                });
                            })
                        });
                    },
                    error: function(XMLHttpResponse) {
                        args.response.error(parseXMLHttpResponse(XMLHttpResponse));
                    }
                });
            } else {
                args.response.success({
                    data: $multi.data('acl-rules')
                });
            }
        }
    };

    cloudStack.vpc = {
        // nTier sections
        sections: {
            tierVMs: function() {
                var list = $.extend(true, {}, cloudStack.sections.instances);

                list.listView.actions.add.action.custom = cloudStack.uiCustom.instanceWizard(
                    $.extend(true, {}, cloudStack.instanceWizard, {
                        pluginForm: {
                            name: 'vpcTierInstanceWizard'
                        }
                    })
                );

                return list;
            },

            tierPortForwarders: function() {
                return cloudStack.vpc.ipAddresses.listView();
            },

            tierStaticNATs: function() {
                return cloudStack.vpc.staticNatIpAddresses.listView();
            },

            // Internal load balancers
            internalLoadBalancers: {
                title: 'Internal LB',
                listView: {
                    id: 'internalLoadBalancers',
                    fields: {
                        name: {
                            label: 'label.name'
                        },
                        sourceipaddress: {
                            label: 'Source IP Address'
                        },
                        sourceport: {
                            label: 'Source Port'
                        },
                        instanceport: {
                            label: 'Instance Port'
                        },
                        algorithm: {
                            label: 'label.algorithm'
                        }
                    },
                    dataProvider: function(args) {
                        $.ajax({
                            url: createURL('listLoadBalancers'),
                            data: {
                                networkid: args.context.networks[0].id
                            },
                            success: function(json) {
                                var items = json.listloadbalancerssresponse.loadbalancer;
                                if (items != null) {
                                    for (var i = 0; i < items.length; i++) {
                                        var item = items[i];
                                        //there is only one element in loadbalancerrul array property.
                                        item.sourceport = item.loadbalancerrule[0].sourceport;
                                        item.instanceport = item.loadbalancerrule[0].instanceport;
                                    }
                                }
                                args.response.success({
                                    data: items
                                });
                            }
                        });
                    },
                    actions: {
                        add: {
                            label: 'Add Internal LB',
                            createForm: {
                                title: 'Add Internal LB',
                                fields: {
                                    name: {
                                        label: 'label.name',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    description: {
                                        label: 'label.description',
                                        validation: {
                                            required: false
                                        }
                                    },
                                    sourceipaddress: {
                                        label: 'Source IP Address',
                                        validation: {
                                            required: false
                                        }
                                    },
                                    sourceport: {
                                        label: 'Source Port',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    instanceport: {
                                        label: 'Instance Port',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    algorithm: {
                                        label: 'label.algorithm',
                                        validation: {
                                            required: true
                                        },
                                        select: function(args) {
                                            args.response.success({
                                                data: [{
                                                    id: 'source',
                                                    description: 'source'
                                                }, {
                                                    id: 'roundrobin',
                                                    description: 'roundrobin'
                                                }, {
                                                    id: 'leastconn',
                                                    description: 'leastconn'
                                                }]
                                            });
                                        }
                                    }
                                }
                            },
                            messages: {
                                notification: function(args) {
                                    return 'Add Internal LB';
                                }
                            },
                            action: function(args) {
                                var data = {
                                    name: args.data.name,
                                    sourceport: args.data.sourceport,
                                    instanceport: args.data.instanceport,
                                    algorithm: args.data.algorithm,
                                    networkid: args.context.networks[0].id,
                                    sourceipaddressnetworkid: args.context.networks[0].id,
                                    scheme: 'Internal'
                                };
                                if (args.data.description != null && args.data.description.length > 0) {
                                    $.extend(data, {
                                        description: args.data.description
                                    });
                                }
                                if (args.data.sourceipaddress != null && args.data.sourceipaddress.length > 0) {
                                    $.extend(data, {
                                        sourceipaddress: args.data.sourceipaddress
                                    });
                                }
                                $.ajax({
                                    url: createURL('createLoadBalancer'),
                                    data: data,
                                    success: function(json) {
                                        var jid = json.createloadbalancerresponse.jobid;
                                        args.response.success({
                                            _custom: {
                                                jobId: jid,
                                                getUpdatedItem: function(json) {
                                                    return json.queryasyncjobresultresponse.jobresult.loadbalancer;
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

                    detailView: {
                        isMaximized: true,
                        name: 'Internal LB details',
                        actions: {
                            assignVMs: {
                                label: 'Assign VMs',
                                messages: {
                                    notification: function(args) {
                                        return 'Assign VMs';
                                    }
                                },
                                needsRefresh: true,
                                listView: $.extend(true, {}, cloudStack.sections.instances.listView, {
                                    type: 'checkbox',
                                    filters: false,
                                    dataProvider: function(args) {
                                        var assignedInstances;
                                        $.ajax({
                                            url: createURL('listLoadBalancers'),
                                            data: {
                                                id: args.context.internalLoadBalancers[0].id
                                            },
                                            async: false,
                                            success: function(json) {
                                                assignedInstances = json.listloadbalancerssresponse.loadbalancer[0].loadbalancerinstance;
                                                if (assignedInstances == null)
                                                    assignedInstances = [];
                                            }
                                        });

                                        $.ajax({
                                            url: createURL('listVirtualMachines'),
                                            data: {
                                                networkid: args.context.networks[0].id,
                                                listAll: true
                                            },
                                            success: function(json) {
                                                var instances = json.listvirtualmachinesresponse.virtualmachine;

                                                // Pre-select existing instances in LB rule
                                                $(instances).map(function(index, instance) {
                                                    instance._isSelected = $.grep(assignedInstances,
                                                        function(assignedInstance) {
                                                            return assignedInstance.id == instance.id;
                                                        }
                                                    ).length ? true : false;
                                                });

                                                //remove assigned VMs (i.e. instance._isSelected == true)
                                                var items = [];
                                                if (instances != null) {
                                                    for (var i = 0; i < instances.length; i++) {
                                                        if (instances[i]._isSelected == true)
                                                            continue;
                                                        else
                                                            items.push(instances[i]);
                                                    }
                                                }

                                                args.response.success({
                                                    data: items
                                                });
                                            }
                                        });
                                    }
                                }),
                                action: function(args) {
                                    var vms = args.context.instances;
                                    var array1 = [];
                                    for (var i = 0; i < vms.length; i++) {
                                        array1.push(vms[i].id);
                                    }
                                    var virtualmachineids = array1.join(',');

                                    $.ajax({
                                        url: createURL('assignToLoadBalancerRule'),
                                        data: {
                                            id: args.context.internalLoadBalancers[0].id,
                                            virtualmachineids: virtualmachineids
                                        },
                                        dataType: 'json',
                                        async: true,
                                        success: function(data) {
                                            var jid = data.assigntoloadbalancerruleresponse.jobid;
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
                                label: 'Delete Internal LB',
                                messages: {
                                    confirm: function(args) {
                                        return 'Please confirm you want to delete Internal LB';
                                    },
                                    notification: function(args) {
                                        return 'Delete Internal LB';
                                    }
                                },
                                action: function(args) {
                                    var data = {
                                        id: args.context.internalLoadBalancers[0].id
                                    };
                                    $.ajax({
                                        url: createURL('deleteLoadBalancer'),
                                        data: data,
                                        async: true,
                                        success: function(json) {
                                            var jid = json.deleteloadbalancerresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid
                                                }
                                            });
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
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
                                    description: {
                                        label: 'label.description'
                                    },
                                    sourceipaddress: {
                                        label: 'Source IP Address'
                                    },
                                    sourceport: {
                                        label: 'Source Port'
                                    },
                                    instanceport: {
                                        label: 'Instance Port'
                                    },
                                    algorithm: {
                                        label: 'label.algorithm'
                                    },
                                    loadbalancerinstance: {
                                        label: 'Assigned VMs',
                                        converter: function(objArray) {
                                            var s = '';
                                            if (objArray != null) {
                                                for (var i = 0; i < objArray.length; i++) {
                                                    if (i > 0) {
                                                        s += ', ';
                                                    }
                                                    s += objArray[i].name + ' (' + objArray[i].ipaddress + ')';
                                                }
                                            }
                                            return s;
                                        }
                                    }
                                }],
                                dataProvider: function(args) {
                                    $.ajax({
                                        url: createURL('listLoadBalancers'),
                                        data: {
                                            id: args.context.internalLoadBalancers[0].id
                                        },
                                        success: function(json) {
                                            var item = json.listloadbalancerssresponse.loadbalancer[0];

                                            //remove Rules tab and add sourceport, instanceport at Details tab because there is only one element in loadbalancerrul array property.
                                            item.sourceport = item.loadbalancerrule[0].sourceport;
                                            item.instanceport = item.loadbalancerrule[0].instanceport;

                                            args.response.success({
                                                data: item
                                            });
                                        }
                                    });
                                }
                            },

                            /*
              rules: {
                title: 'label.rules',
                multiple: true,
                fields: [
                  {
                    sourceport: { label: 'Source Port' },
                    instanceport: { label: 'Instance Port' }
                  }
                ],
                dataProvider: function(args) {
                  $.ajax({
                    url: createURL('listLoadBalancers'),
                    data: {
                      id: args.context.internalLoadBalancers[0].id
                    },
                    success: function(json) {
                      var item = json.listloadbalancerssresponse.loadbalancer[0];
                      args.response.success({ data: item.loadbalancerrule });
                    }
                  });
                }
              },
              */

                            assignedVms: {
                                title: 'Assigned VMs',
                                listView: {
                                    id: 'assignedVms',
                                    fields: {
                                        name: {
                                            label: 'label.name'
                                        },
                                        ipaddress: {
                                            label: 'label.ip.address'
                                        }
                                    },
                                    dataProvider: function(args) {
                                        $.ajax({
                                            url: createURL('listLoadBalancers'),
                                            data: {
                                                id: args.context.internalLoadBalancers[0].id
                                            },
                                            success: function(json) {
                                                var item = json.listloadbalancerssresponse.loadbalancer[0];
                                                args.response.success({
                                                    data: item.loadbalancerinstance
                                                });
                                            }
                                        });
                                    },
                                    actions: {
                                        add: {
                                            label: 'Assign VMs',
                                            messages: {
                                                notification: function(args) {
                                                    return 'Assign VMs';
                                                }
                                            },
                                            needsRefresh: true,
                                            listView: $.extend(true, {}, cloudStack.sections.instances.listView, {
                                                type: 'checkbox',
                                                filters: false,
                                                dataProvider: function(args) {
                                                    var assignedInstances;
                                                    $.ajax({
                                                        url: createURL('listLoadBalancers'),
                                                        data: {
                                                            id: args.context.internalLoadBalancers[0].id
                                                        },
                                                        async: false,
                                                        success: function(json) {
                                                            assignedInstances = json.listloadbalancerssresponse.loadbalancer[0].loadbalancerinstance;
                                                            if (assignedInstances == null)
                                                                assignedInstances = [];
                                                        }
                                                    });

                                                    $.ajax({
                                                        url: createURL('listVirtualMachines'),
                                                        data: {
                                                            networkid: args.context.networks[0].id,
                                                            listAll: true
                                                        },
                                                        success: function(json) {
                                                            var instances = json.listvirtualmachinesresponse.virtualmachine;

                                                            // Pre-select existing instances in LB rule
                                                            $(instances).map(function(index, instance) {
                                                                instance._isSelected = $.grep(assignedInstances,
                                                                    function(assignedInstance) {
                                                                        return assignedInstance.id == instance.id;
                                                                    }
                                                                ).length ? true : false;
                                                            });

                                                            //remove assigned VMs (i.e. instance._isSelected == true)
                                                            var items = [];
                                                            if (instances != null) {
                                                                for (var i = 0; i < instances.length; i++) {
                                                                    if (instances[i]._isSelected == true)
                                                                        continue;
                                                                    else
                                                                        items.push(instances[i]);
                                                                }
                                                            }

                                                            args.response.success({
                                                                data: items
                                                            });
                                                        }
                                                    });
                                                }
                                            }),
                                            action: function(args) {
                                                var vms = args.context.instances;
                                                var array1 = [];
                                                for (var i = 0; i < vms.length; i++) {
                                                    array1.push(vms[i].id);
                                                }
                                                var virtualmachineids = array1.join(',');

                                                $.ajax({
                                                    url: createURL('assignToLoadBalancerRule'),
                                                    data: {
                                                        id: args.context.internalLoadBalancers[0].id,
                                                        virtualmachineids: virtualmachineids
                                                    },
                                                    dataType: 'json',
                                                    async: true,
                                                    success: function(data) {
                                                        var jid = data.assigntoloadbalancerruleresponse.jobid;
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
                                    detailView: {
                                        actions: {
                                            remove: {
                                                label: 'remove VM from load balancer',
                                                addRow: 'false',
                                                messages: {
                                                    confirm: function(args) {
                                                        return 'Please confirm you want to remove VM from load balancer';
                                                    },
                                                    notification: function(args) {
                                                        return 'remove VM from load balancer';
                                                    }
                                                },
                                                action: function(args) {
                                                    $.ajax({
                                                        url: createURL('removeFromLoadBalancerRule'),
                                                        data: {
                                                            id: args.context.internalLoadBalancers[0].id,
                                                            virtualmachineids: args.context.assignedVms[0].id
                                                        },
                                                        success: function(json) {
                                                            var jid = json.removefromloadbalancerruleresponse.jobid;
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
                                                    ipaddress: {
                                                        label: 'label.ip.address'
                                                    }
                                                }],
                                                dataProvider: function(args) {
                                                    setTimeout(function() {
                                                        args.response.success({
                                                            data: args.context.assignedVms[0]
                                                        });
                                                    });
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            publicLbIps: {
                title: 'Public LB',
                listView: {
                    id: 'publicLbIps',
                    fields: {
                        ipaddress: {
                            label: 'label.ips',
                            converter: function(text, item) {
                                if (item.issourcenat) {
                                    return text + ' [' + _l('label.source.nat') + ']';
                                }

                                return text;
                            }
                        },
                        zonename: {
                            label: 'label.zone'
                        },
                        virtualmachinedisplayname: {
                            label: 'label.vm.name'
                        },
                        state: {
                            converter: function(str) {
                                // For localization
                                return str;
                            },
                            label: 'label.state',
                            indicator: {
                                'Allocated': 'on',
                                'Released': 'off'
                            }
                        }
                    },
                    dataProvider: function(args) {
                        $.ajax({
                            url: createURL('listPublicIpAddresses'),
                            async: false,
                            data: {
                                associatednetworkid: args.context.networks[0].id,
                                forloadbalancing: true
                            },
                            success: function(json) {
                                var items = json.listpublicipaddressesresponse.publicipaddress;
                                args.response.success({
                                    data: items
                                });
                            }
                        });
                    }
                }
            },

            // Private gateways
            privateGateways: function() {
                return cloudStack.vpc.gateways.listView()
            },

            // Public IP Addresses
            publicIPs: function() {
                return cloudStack.vpc.ipAddresses.listView()
            },

            // Network ACL lists
            networkACLLists: {
                listView: {
                    id: 'aclLists',
                    fields: {
                        name: {
                            label: 'label.name'
                        },
                        description: {
                            label: 'Description'
                        },
                        id: {
                            label: 'id'
                        }
                    },
                    dataProvider: function(args) {
                        $.ajax({
                            url: createURL('listNetworkACLLists&vpcid=' + args.context.vpc[0].id),
                            success: function(json) {
                                var items = json.listnetworkacllistsresponse.networkacllist;

                                args.response.success({
                                    data: items
                                });
                            }
                        });
                    },

                    actions: {
                        add: {
                            label: 'Add ACL List',
                            createForm: {
                                label: 'Add ACL List',
                                fields: {
                                    name: {
                                        label: 'ACL List Name',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    description: {
                                        label: 'Description',
                                        validation: {
                                            required: true
                                        }
                                    }
                                }
                            },
                            messages: {
                                notification: function(args) {
                                    return 'Add Network ACL List';
                                }
                            },
                            action: function(args) {
                                var data = {
                                    name: args.data.name,
                                    description: args.data.description

                                };

                                $.ajax({
                                    url: createURL('createNetworkACLList&vpcid=' + args.context.vpc[0].id),
                                    data: data,
                                    success: function(json) {
                                        var items = json.createnetworkacllistresponse;
                                        args.response.success({
                                            data: items
                                        });
                                    }
                                });
                            }
                        }
                    },

                    detailView: {
                        isMaximized: true,
                        actions: {
                            remove: {
                                label: 'Delete ACL List',
                                messages: {
                                    confirm: function(args) {
                                        return 'Are you sure you want to delete this ACL list ?';
                                    },
                                    notification: function(args) {
                                        return 'Delete ACL list';
                                    }
                                },
                                action: function(args) {
                                    $.ajax({
                                        url: createURL('deleteNetworkACLList&id=' + args.context.aclLists[0].id),
                                        success: function(json) {
                                            var jid = json.deletenetworkacllistresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid
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
                            }
                        },

                        tabs: {
                            details: {
                                title: 'label.details',
                                fields: [{
                                    name: {
                                        label: 'label.name',
                                        isEditable: true
                                    },
                                    description: {
                                        label: 'Description'
                                    },
                                    id: {
                                        label: 'id'
                                    }
                                }],
                                dataProvider: function(args) {
                                    var items = args.context.aclLists[0];
                                    setTimeout(function() {
                                        args.response.success({
                                            data: items,
                                            actionFilter: function(args) {
                                                var allowedActions = [];
                                                if (isAdmin() && items.vpcid) {
                                                    allowedActions.push("remove");

                                                }
                                                return allowedActions;
                                            }
                                        });
                                    });
                                }
                            },

                            aclRules: {
                                title: 'ACL List Rules',
                                custom: function(args) {
                                    return $('<div>').multiEdit($.extend(true, {}, aclMultiEdit, {
                                        context: args.context,
                                        fields: {
                                            networkid: false
                                        },
                                        dataProvider: function(args) {
                                            $.ajax({
                                                url: createURL('listNetworkACLs&aclid=' + args.context.aclLists[0].id),
                                                success: function(json) {
                                                    var items = json.listnetworkaclsresponse.networkacl.sort(function(a, b) {
                                                        return a.number >= b.number;
                                                    }).map(function(acl) {
                                                        if (parseInt(acl.protocol)) { // protocol number
                                                            acl.protocolnumber = acl.protocol;
                                                            acl.protocol = "protocolnumber";
                                                        }

                                                        return acl;
                                                    });

                                                    args.response.success({
                                                        data: items
                                                        /* {
                               cidrlist: '10.1.1.0/24',
                               protocol: 'TCP',
                               startport: 22, endport: 22,
                               networkid: 0,
                               traffictype: 'Egress'
                               },
                               {
                               cidrlist: '10.2.1.0/24',
                               protocol: 'UDP',
                               startport: 56, endport: 72,
                               networkid: 0,
                               trafficType: 'Ingress'
                               }
                               ]*/
                                                    });
                                                }
                                            });
                                        }
                                    }));
                                }
                            }
                        }
                    }
                }
            },
            siteToSiteVPNs: function() {
                return $.extend(true, {}, cloudStack.vpc.siteToSiteVPN, {
                    // siteToSiteVPN is multi-section so doesn't have an explicit
                    // 'listView' block
                    //
                    // -- use this as a flag for VPC chart to render as a list view
                    listView: true,
                    before: {
                        messages: {
                            confirm: 'Please confirm that you would like to create a site-to-site VPN gateway for this VPC.',
                            notification: 'Create site-to-site VPN gateway'
                        },
                        check: function(args) {
                            var items;

                            $.ajax({
                                url: createURL('listVpnGateways&listAll=true'),
                                data: {
                                    vpcid: args.context.vpc[0].id
                                },
                                success: function(json) {
                                    var items = json.listvpngatewaysresponse.vpngateway;

                                    args.response.success(items && items.length);
                                }
                            });
                        },
                        action: function(args) {
                            $.ajax({
                                url: createURL("createVpnGateway"),
                                data: {
                                    vpcid: args.context.vpc[0].id
                                },
                                success: function(json) {
                                    var jid = json.createvpngatewayresponse.jobid;
                                    var pollTimer = setInterval(function() {
                                        pollAsyncJobResult({
                                            _custom: {
                                                jobId: jid
                                            },
                                            complete: function() {
                                                clearInterval(pollTimer);
                                                args.response.success();
                                            }
                                        });
                                    }, g_queryAsyncJobResultInterval);
                                }
                            });
                        }
                    }
                });
            }
        },

        routerDetailView: function() {
            return {
                title: 'VPC router details',
                updateContext: function(args) {
                    var router;

                    $.ajax({
                        url: createURL("listRouters&listAll=true&vpcid=" + args.context.vpc[0].id),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                            router = json.listroutersresponse.router[0];
                        }
                    });

                    return {
                        routers: [router]
                    };
                },
                actions: cloudStack.sections.system.subsections.virtualRouters
                    .listView.detailView.actions,
                tabs: {
                    routerDetails: cloudStack.sections.network.sections.vpc
                        .listView.detailView.tabs.router
                }
            };
        },
        vmListView: {
            id: 'vpcTierInstances',
            listView: {
                filters: {
                    all: {
                        label: 'label.menu.all.instances'
                    },
                    running: {
                        label: 'label.menu.running.instances'
                    },
                    stopped: {
                        label: 'label.menu.stopped.instances'
                    },
                    destroyed: {
                        label: 'label.menu.destroyed.instances'
                    }
                },
                fields: {
                    name: {
                        label: 'label.name',
                        editable: true
                    },
                    account: {
                        label: 'label.account'
                    },
                    zonename: {
                        label: 'label.zone'
                    },
                    state: {
                        label: 'label.status',
                        indicator: {
                            'Running': 'on',
                            'Stopped': 'off',
                            'Destroyed': 'off'
                        }
                    }
                },

                // List view actions
                actions: {
                    start: {
                        label: 'label.action.start.instance',
                        action: function(args) {
                            $.ajax({
                                url: createURL("startVirtualMachine&id=" + args.context.vpcTierInstances[0].id),
                                dataType: "json",
                                async: true,
                                success: function(json) {
                                    var jid = json.startvirtualmachineresponse.jobid;
                                    args.response.success({
                                        _custom: {
                                            jobId: jid,
                                            getUpdatedItem: function(json) {
                                                return json.queryasyncjobresultresponse.jobresult.virtualmachine;
                                            },
                                            getActionFilter: function() {
                                                return cloudStack.actionFilter.vmActionFilter;
                                            }
                                        }
                                    });
                                }
                            });
                        },
                        messages: {
                            confirm: function(args) {
                                return 'message.action.start.instance';
                            },
                            notification: function(args) {
                                return 'label.action.start.instance';
                            },
                            complete: function(args) {
                                if (args.password != null) {
                                    alert('Password of the VM is ' + args.password);
                                }
                                return 'label.action.start.instance';
                            }
                        },
                        notification: {
                            poll: pollAsyncJobResult
                        }
                    },
                    stop: {
                        label: 'label.action.stop.instance',
                        addRow: 'false',
                        createForm: {
                            title: 'label.action.stop.instance',
                            desc: 'message.action.stop.instance',
                            fields: {
                                forced: {
                                    label: 'force.stop',
                                    isBoolean: true,
                                    isChecked: false
                                }
                            }
                        },
                        action: function(args) {
                            var array1 = [];
                            array1.push("&forced=" + (args.data.forced == "on"));
                            $.ajax({
                                url: createURL("stopVirtualMachine&id=" + args.context.vpcTierInstances[0].id + array1.join("")),
                                dataType: "json",
                                async: true,
                                success: function(json) {
                                    var jid = json.stopvirtualmachineresponse.jobid;
                                    args.response.success({
                                        _custom: {
                                            jobId: jid,
                                            getUpdatedItem: function(json) {
                                                return json.queryasyncjobresultresponse.jobresult.virtualmachine;
                                            },
                                            getActionFilter: function() {
                                                return cloudStack.actionFilter.vmActionFilter;
                                            }
                                        }
                                    });
                                }
                            });
                        },
                        messages: {
                            confirm: function(args) {
                                return 'message.action.stop.instance';
                            },

                            notification: function(args) {
                                return 'label.action.stop.instance';
                            }
                        },
                        notification: {
                            poll: pollAsyncJobResult
                        }
                    },
                    restart: {
                        label: 'instances.actions.reboot.label',
                        action: function(args) {
                            $.ajax({
                                url: createURL("rebootVirtualMachine&id=" + args.context.vpcTierInstances[0].id),
                                dataType: "json",
                                async: true,
                                success: function(json) {
                                    var jid = json.rebootvirtualmachineresponse.jobid;
                                    args.response.success({
                                        _custom: {
                                            jobId: jid,
                                            getUpdatedItem: function(json) {
                                                return json.queryasyncjobresultresponse.jobresult.virtualmachine;
                                            },
                                            getActionFilter: function() {
                                                return cloudStack.actionFilter.vmActionFilter;
                                            }
                                        }
                                    });
                                }
                            });
                        },
                        messages: {
                            confirm: function(args) {
                                return 'message.action.reboot.instance';
                            },
                            notification: function(args) {
                                return 'instances.actions.reboot.label';
                            }
                        },
                        notification: {
                            poll: pollAsyncJobResult
                        }
                    },
                    destroy: {
                        label: 'label.action.destroy.instance',
                        messages: {
                            confirm: function(args) {
                                return 'message.action.destroy.instance';
                            },
                            notification: function(args) {
                                return 'label.action.destroy.instance';
                            }
                        },
                        action: function(args) {
                            $.ajax({
                                url: createURL("destroyVirtualMachine&id=" + args.context.vpcTierInstances[0].id),
                                dataType: "json",
                                async: true,
                                success: function(json) {
                                    var jid = json.destroyvirtualmachineresponse.jobid;
                                    args.response.success({
                                        _custom: {
                                            jobId: jid,
                                            getUpdatedItem: function(json) {
                                                return json.queryasyncjobresultresponse.jobresult.virtualmachine;
                                            },
                                            getActionFilter: function() {
                                                return cloudStack.actionFilter.vmActionFilter;
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
                    restore: {
                        label: 'label.action.restore.instance',
                        messages: {
                            confirm: function(args) {
                                return 'message.action.restore.instance';
                            },
                            notification: function(args) {
                                return 'label.action.restore.instance';
                            }
                        },
                        action: function(args) {
                            $.ajax({
                                url: createURL("recoverVirtualMachine&id=" + args.context.vpcTierInstances[0].id),
                                dataType: "json",
                                async: true,
                                success: function(json) {
                                    var item = json.recovervirtualmachineresponse.virtualmachine;
                                    args.response.success({
                                        data: item
                                    });
                                }
                            });
                        }
                    },
                    viewConsole: {
                        label: 'label.view.console',
                        action: {
                            externalLink: {
                                url: function(args) {
                                    return clientConsoleUrl + '?cmd=access&vm=' + args.context.vpcTierInstances[0].id;
                                },
                                title: function(args) {
                                    return args.context.vpcTierInstances[0].id.substr(0, 8); //title in window.open() can't have space nor longer than 8 characters. Otherwise, IE browser will have error.
                                },
                                width: 820,
                                height: 640
                            }
                        }
                    }
                },
                dataProvider: function(args) {
                    var array1 = [];
                    if (args.filterBy != null) {
                        if (args.filterBy.kind != null) {
                            switch (args.filterBy.kind) {
                                case "all":
                                    array1.push("&listAll=true");
                                    break;
                                case "mine":
                                    if (!args.context.projects) array1.push("&domainid=" + g_domainid + "&account=" + g_account);
                                    break;
                                case "running":
                                    array1.push("&listAll=true&state=Running");
                                    break;
                                case "stopped":
                                    array1.push("&listAll=true&state=Stopped");
                                    break;
                                case "destroyed":
                                    array1.push("&listAll=true&state=Destroyed");
                                    break;
                            }
                        }
                        if (args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
                            switch (args.filterBy.search.by) {
                                case "name":
                                    if (args.filterBy.search.value.length > 0)
                                        array1.push("&keyword=" + args.filterBy.search.value);
                                    break;
                            }
                        }
                    }

                    $.ajax({
                        url: createURL('listVirtualMachines' + array1.join("")),
                        data: {
                            networkid: args.context.networks[0].id
                        },
                        success: function(json) {
                            args.response.success({
                                data: json.listvirtualmachinesresponse.virtualmachine,
                                actionFilter: cloudStack.actionFilter.vmActionFilter
                            });
                        }
                    });
                }
            }
        },
        ipAddresses: {
            listView: function() {
                var listView = $.extend(true, {}, cloudStack.sections.network.sections.ipAddresses);

                listView.listView.fields = {
                    ipaddress: listView.listView.fields.ipaddress,
                    zonename: listView.listView.fields.zonename,
                    associatednetworkname: {
                        label: 'label.network.name'
                    },
                    state: listView.listView.fields.state
                };

                return listView;
            }
        },
        staticNatIpAddresses: {
            listView: function() {
                var listView = $.extend(true, {}, cloudStack.sections.network.sections.ipAddresses);

                listView.listView.fields = {
                    ipaddress: listView.listView.fields.ipaddress,
                    zonename: listView.listView.fields.zonename,
                    associatednetworkname: {
                        label: 'label.network.name'
                    },
                    state: listView.listView.fields.state
                };

                listView.listView.dataProvider = function(args) {
                    $.ajax({
                        url: createURL('listPublicIpAddresses'),
                        data: {
                            associatednetworkid: args.context.networks[0].id,
                            isstaticnat: true,
                            listall: true
                        },
                        success: function(json) {
                            args.response.success({
                                data: json.listpublicipaddressesresponse.publicipaddress
                            });
                        },
                        error: function(json) {
                            args.response.error(parseXMLHttpResponse(json));
                        }
                    });
                };

                return listView;
            }
        },
        acl: {
            multiEdit: aclMultiEdit,

            listView: {
                listView: {
                    id: 'networks',
                    fields: {
                        tierName: {
                            label: 'label.tier'
                        },
                        aclTotal: {
                            label: 'label.network.ACL.total'
                        }
                    },
                    dataProvider: function(args) {
                        $.ajax({
                            url: createURL('listNetworks'),
                            data: {
                                listAll: true,
                                vpcid: args.context.vpc[0].id
                            },
                            success: function(json) {
                                var networks = json.listnetworksresponse.network ?
                                    json.listnetworksresponse.network : [];

                                args.response.success({
                                    data: $.map(networks, function(tier) {
                                        var aclTotal = 0;

                                        // Get ACL total
                                        $.ajax({
                                            url: createURL('listNetworkACLs'),
                                            async: false,
                                            data: {
                                                listAll: true,
                                                networkid: tier.id
                                            },
                                            success: function(json) {
                                                aclTotal = json.listnetworkaclsresponse.networkacl ?
                                                    json.listnetworkaclsresponse.networkacl.length : 0;
                                            }
                                        });

                                        return $.extend(tier, {
                                            tierName: tier.name,
                                            aclTotal: aclTotal
                                        });
                                    })
                                });
                            }
                        });
                    }
                }
            }
        },
        gateways: {

            add: {
                preCheck: function(args) {
                    if (isAdmin()) { //root-admin
                        var items;
                        $.ajax({
                            url: createURL('listPrivateGateways'),
                            async: false,
                            data: {
                                vpcid: args.context.vpc[0].id,
                                listAll: true
                            },
                            success: function(json) {
                                items = json.listprivategatewaysresponse.privategateway;
                            }
                        });
                        if (items && items.length) {
                            return true; //show private gateway listView
                        } else {
                            return false; //show create private gateway dialog
                        }
                    } else { //regular-user, domain-admin
                        return true; //show private gateway listView instead of create private gateway dialog because only root-admin is allowed to create private gateway
                    }
                },
                label: 'label.add.new.gateway',
                messages: {
                    notification: function(args) {
                        return 'label.add.new.gateway';
                    }
                },
                createForm: {
                    title: 'label.add.new.gateway',
                    desc: 'message.add.new.gateway.to.vpc',
                    fields: {
                        physicalnetworkid: {
                            docID: 'helpVPCGatewayPhysicalNetwork',
                            label: 'label.physical.network',
                            select: function(args) {
                                $.ajax({
                                    url: createURL("listPhysicalNetworks"),
                                    data: {
                                        zoneid: args.context.vpc[0].zoneid
                                    },
                                    success: function(json) {
                                        var objs = json.listphysicalnetworksresponse.physicalnetwork;
                                        var items = [];
                                        $(objs).each(function() {
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
                        },
                        vlan: {
                            label: 'label.vlan',
                            validation: {
                                required: true
                            },
                            docID: 'helpVPCGatewayVLAN'
                        },
                        ipaddress: {
                            label: 'label.ip.address',
                            validation: {
                                required: true
                            },
                            docID: 'helpVPCGatewayIP'
                        },
                        gateway: {
                            label: 'label.gateway',
                            validation: {
                                required: true
                            },
                            docID: 'helpVPCGatewayGateway'
                        },
                        netmask: {
                            label: 'label.netmask',
                            validation: {
                                required: true
                            },
                            docID: 'helpVPCGatewayNetmask'
                        },
                        sourceNat: {
                            label: 'Source NAT',
                            isBoolean: true,
                            isChecked: false

                        },

                        aclid: {
                            label: 'ACL',
                            select: function(args) {
                                $.ajax({
                                    url: createURL('listNetworkACLLists'),
                                    dataType: 'json',
                                    async: true,
                                    success: function(json) {
                                        var objs = json.listnetworkacllistsresponse.networkacllist;
                                        var items = [];
                                        $(objs).each(function() {
                                            if (this.name == "default_deny")
                                                items.unshift({
                                                    id: this.id,
                                                    description: this.name
                                                });
                                            else
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
                    var array1 = [];
                    if (args.$form.find('.form-item[rel=sourceNat]').find('input[type=checkbox]').is(':Checked') == true) {
                        array1.push("&sourcenatsupported=true");
                    } else
                        array1.push("&sourcenatsupported=false");


                    $.ajax({
                        url: createURL('createPrivateGateway' + array1.join("")),
                        data: {
                            physicalnetworkid: args.data.physicalnetworkid,
                            vpcid: args.context.vpc[0].id,
                            ipaddress: args.data.ipaddress,
                            gateway: args.data.gateway,
                            netmask: args.data.netmask,
                            vlan: args.data.vlan,
                            aclid: args.data.aclid

                        },
                        success: function(json) {
                            var jid = json.createprivategatewayresponse.jobid;
                            args.response.success({
                                _custom: {
                                    jobId: jid,
                                    getUpdatedItem: function(json) {
                                        return json.queryasyncjobresultresponse.jobresult.privategateway;
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
            listView: function() {
                return {
                    listView: {
                        id: 'vpcGateways',
                        fields: {
                            ipaddress: {
                                label: 'label.ip.address',
                                validation: {
                                    required: true
                                }
                            },
                            gateway: {
                                label: 'label.gateway',
                                validation: {
                                    required: true
                                }
                            },
                            netmask: {
                                label: 'label.netmask',
                                validation: {
                                    required: true
                                }
                            },
                            vlan: {
                                label: 'label.vlan',
                                validation: {
                                    required: true
                                }
                            }
                        },

                        actions: {
                            add: {
                                label: 'Add Private Gateway',
                                preFilter: function(args) {
                                    if (isAdmin() || isDomainAdmin())
                                        return true;
                                    else
                                        return false;
                                },
                                createForm: {
                                    title: 'label.add.new.gateway',
                                    desc: 'message.add.new.gateway.to.vpc',
                                    fields: {
                                        physicalnetworkid: {
                                            docID: 'helpVPCGatewayPhysicalNetwork',
                                            label: 'label.physical.network',
                                            select: function(args) {
                                                $.ajax({
                                                    url: createURL("listPhysicalNetworks"),
                                                    data: {
                                                        zoneid: args.context.vpc[0].zoneid
                                                    },
                                                    success: function(json) {
                                                        var objs = json.listphysicalnetworksresponse.physicalnetwork;
                                                        var items = [];
                                                        $(objs).each(function() {
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
                                        },
                                        vlan: {
                                            label: 'label.vlan',
                                            validation: {
                                                required: true
                                            },
                                            docID: 'helpVPCGatewayVLAN'
                                        },
                                        ipaddress: {
                                            label: 'label.ip.address',
                                            validation: {
                                                required: true
                                            },
                                            docID: 'helpVPCGatewayIP'
                                        },
                                        gateway: {
                                            label: 'label.gateway',
                                            validation: {
                                                required: true
                                            },
                                            docID: 'helpVPCGatewayGateway'
                                        },
                                        netmask: {
                                            label: 'label.netmask',
                                            validation: {
                                                required: true
                                            },
                                            docID: 'helpVPCGatewayNetmask'
                                        },

                                        sourceNat: {
                                            label: 'Source NAT',
                                            isBoolean: true,
                                            isChecked: false

                                        },

                                        aclid: {
                                            label: 'ACL',
                                            select: function(args) {
                                                $.ajax({
                                                    url: createURL('listNetworkACLLists'),
                                                    data: {
                                                        vpcid: args.context.vpc[0].id
                                                    },
                                                    success: function(json) {
                                                        var objs = json.listnetworkacllistsresponse.networkacllist;
                                                        var items = [];
                                                        $(objs).each(function() {
                                                            if (this.name == "default_deny")
                                                                items.unshift({
                                                                    id: this.id,
                                                                    description: this.name
                                                                });
                                                            else
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

                                    var array1 = [];
                                    if (args.$form.find('.form-item[rel=sourceNat]').find('input[type=checkbox]').is(':Checked') == true) {
                                        array1.push("&sourcenatsupported=true");
                                    } else
                                        array1.push("&sourcenatsupported=false");


                                    $.ajax({
                                        url: createURL('createPrivateGateway' + array1.join("")),
                                        data: {
                                            physicalnetworkid: args.data.physicalnetworkid,
                                            vpcid: args.context.vpc[0].id,
                                            ipaddress: args.data.ipaddress,
                                            gateway: args.data.gateway,
                                            netmask: args.data.netmask,
                                            vlan: args.data.vlan,
                                            aclid: args.data.aclid

                                        },
                                        success: function(json) {
                                            var jid = json.createprivategatewayresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid,
                                                    getUpdatedItem: function(json) {
                                                        return json.queryasyncjobresultresponse.jobresult.privategateway;
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


                            }
                        },

                        dataProvider: function(args) {
                            $.ajax({
                                url: createURL('listPrivateGateways'),
                                data: {
                                    vpcid: args.context.vpc[0].id,
                                    listAll: true
                                },
                                success: function(json) {
                                    var items = json.listprivategatewaysresponse.privategateway;
                                    args.response.success({
                                        data: items
                                    });
                                }
                            });
                        },
                        detailView: {
                            name: 'label.details',
                            actions: {
                                remove: {
                                    label: 'label.delete.gateway',
                                    messages: {
                                        confirm: function(args) {
                                            return 'message.delete.gateway';
                                        },
                                        notification: function(args) {
                                            return 'label.delete.gateway';
                                        }
                                    },
                                    action: function(args) {
                                        $.ajax({
                                            url: createURL("deletePrivateGateway&id=" + args.context.vpcGateways[0].id),
                                            dataType: "json",
                                            async: true,
                                            success: function(json) {
                                                var jid = json.deleteprivategatewayresponse.jobid;
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

                                replaceACL: {
                                    label: 'Replace ACL',
                                    createForm: {
                                        title: 'Replace ACL',
                                        label: 'Replace ACL',
                                        fields: {
                                            aclid: {
                                                label: 'ACL',
                                                select: function(args) {
                                                    $.ajax({
                                                        url: createURL('listNetworkACLLists'),
                                                        data: {
                                                            vpcid: args.context.vpc[0].id
                                                        },
                                                        dataType: 'json',
                                                        async: true,
                                                        success: function(json) {
                                                            var objs = json.listnetworkacllistsresponse.networkacllist;
                                                            var items = [];

                                                            $(objs).each(function() {
                                                                if (this.id == args.context.vpcGateways[0].aclid) {
                                                                    return true;
                                                                }
                                                                
                                                                items.push({
                                                                    id: this.id,
                                                                    description: this.name
                                                                });

                                                                return true;
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
                                        $.ajax({
                                            url: createURL("replaceNetworkACLList&gatewayid=" + args.context.vpcGateways[0].id + "&aclid=" + args.data.aclid),
                                            dataType: "json",
                                            success: function(json) {
                                                var jid = json.replacenetworkacllistresponse.jobid;
                                                args.response.success(

                                                    {
                                                        _custom: {
                                                            jobId: jid,
                                                            getUpdatedItem: function(json) {
                                                                var item = json.queryasyncjobresultresponse.jobresult.aclid;
                                                                return {
                                                                    aclid: args.data.aclid
                                                                };
                                                            }
                                                        }
                                                    }

                                                )
                                            },

                                            error: function(json) {

                                                args.response.error(parseXMLHttpResponse(json));
                                            }
                                        });
                                    },

                                    notification: {
                                        poll: pollAsyncJobResult
                                    },

                                    messages: {
                                        confirm: function(args) {
                                            return 'Do you want to replace the ACL with a new one ?';
                                        },
                                        notification: function(args) {
                                            return 'ACL replaced';
                                        }
                                    }
                                }
                            },
                            tabs: {
                                details: {
                                    title: 'label.details',
                                    fields: [{
                                        ipaddress: {
                                            label: 'label.ip.address'
                                        }
                                    }, {
                                        gateway: {
                                            label: 'label.gateway'
                                        },
                                        netmask: {
                                            label: 'label.netmask'
                                        },
                                        vlan: {
                                            label: 'label.vlan'
                                        },
                                        state: {
                                            label: 'label.state'
                                        },
                                        id: {
                                            label: 'label.id'
                                        },
                                        zonename: {
                                            label: 'label.zone'
                                        },
                                        domain: {
                                            label: 'label.domain'
                                        },
                                        account: {
                                            label: 'label.account'
                                        },
                                        sourcenatsupported: {
                                            label: 'SourceNAT Supported',
                                            converter: function(str) {
                                                return str ? 'Yes' : 'No';
                                            }
                                        },
                                        aclName: {
                                            label: 'ACL Name'
                                        },
                                        aclid: {
                                            label: 'ACL ID'
                                        }


                                    }],
                                    dataProvider: function(args) {
                                        $.ajax({
                                            url: createURL('listPrivateGateways'),
                                            data: {
                                                id: args.context.vpcGateways[0].id,
                                                listAll: true
                                            },
                                            success: function(json) {
                                                var item = json.listprivategatewaysresponse.privategateway[0];


                                                // Get ACL name
                                                $.ajax({
                                                    url: createURL('listNetworkACLLists'),
                                                    async: false,
                                                    data: {
                                                        vpcid: args.context.vpc[0].id
                                                    },
                                                    success: function(json) {
                                                        var objs = json.listnetworkacllistsresponse.networkacllist;
                                                        var acl = $.grep(objs, function(obj) {
                                                            return obj.id === args.context.vpcGateways[0].aclid;                                                            
                                                        });
                                                        
                                                        item.aclName = acl[0] ? acl[0].name : 'None';
                                                    }
                                                });
                                                
                                                args.response.success({
                                                    data: item,
                                                    actionFilter: function(args) {
                                                        var allowedActions = [];
                                                        if (isAdmin()) {
                                                            allowedActions.push("remove");
                                                            allowedActions.push("replaceACL");

                                                        }
                                                        return allowedActions;
                                                    }
                                                });
                                            }
                                        });
                                    }
                                },
                                staticRoutes: {
                                    title: 'Static Routes',
                                    custom: function(args) {
                                        return $('<div>').multiEdit({
                                            noSelect: true,
                                            context: args.context,
                                            fields: {
                                                cidr: {
                                                    edit: true,
                                                    label: 'label.CIDR.of.destination.network'
                                                },
                                                'add-rule': {
                                                    label: 'label.add.route',
                                                    addButton: true
                                                }
                                            },

                                            tags: cloudStack.api.tags({
                                                resourceType: 'StaticRoute',
                                                contextId: 'multiRule'
                                            }),

                                            add: {
                                                label: 'label.add',
                                                action: function(args) {
                                                    $.ajax({
                                                        url: createURL('createStaticRoute'),
                                                        data: {
                                                            gatewayid: args.context.vpcGateways[0].id,
                                                            cidr: args.data.cidr
                                                        },
                                                        success: function(data) {
                                                            args.response.success({
                                                                _custom: {
                                                                    jobId: data.createstaticrouteresponse.jobid
                                                                },
                                                                notification: {
                                                                    label: 'label.add.static.route',
                                                                    poll: pollAsyncJobResult
                                                                }
                                                            });
                                                        },
                                                        error: function(data) {
                                                            args.response.error(parseXMLHttpResponse(data));
                                                        }
                                                    });
                                                }
                                            },
                                            actions: {
                                                destroy: {
                                                    label: 'label.remove.static.route',
                                                    action: function(args) {
                                                        $.ajax({
                                                            url: createURL('deleteStaticRoute'),
                                                            data: {
                                                                id: args.context.multiRule[0].id
                                                            },
                                                            dataType: 'json',
                                                            async: true,
                                                            success: function(data) {
                                                                var jobID = data.deletestaticrouteresponse.jobid;

                                                                args.response.success({
                                                                    _custom: {
                                                                        jobId: jobID
                                                                    },
                                                                    notification: {
                                                                        label: 'label.remove.static.route',
                                                                        poll: pollAsyncJobResult
                                                                    }
                                                                });
                                                            }
                                                        });
                                                    }
                                                }
                                            },
                                            dataProvider: function(args) {
                                                $.ajax({
                                                    url: createURL('listStaticRoutes'),
                                                    data: {
                                                        gatewayid: args.context.vpcGateways[0].id,
                                                        listAll: true
                                                    },
                                                    success: function(json) {
                                                        var items = json.liststaticroutesresponse.staticroute;
                                                        args.response.success({
                                                            data: items
                                                        });
                                                    }
                                                });
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    }
                };
            }
        },
        siteToSiteVPN: {
            title: 'label.site.to.site.VPN',
            id: 'siteToSiteVpn',
            sectionSelect: {
                preFilter: function(args) {
                    return ["vpnGateway", "vpnConnection"];
                },
                label: 'label.select-view'
            },

            // This is a custom add function -- does not show in list view
            add: {
                // Check if VPN gateways exist
                // -- if false, don't show list view
                preCheck: function(args) {
                    var items;

                    $.ajax({
                        url: createURL('listVpnGateways&listAll=true'),
                        data: {
                            vpcid: args.context.vpc[0].id
                        },
                        async: false,
                        success: function(json) {
                            items = json.listvpngatewaysresponse.vpngateway;
                        }
                    });

                    if (items && items.length) {
                        return true;
                    }

                    return false;
                },
                label: 'label.add.VPN.gateway',
                messages: {
                    notification: function(args) {
                        return 'label.add.VPN.gateway';
                    }
                },
                createForm: {
                    title: 'label.add.VPN.gateway',
                    desc: 'message.add.VPN.gateway',
                    fields: {}
                },
                action: function(args) {
                    $.ajax({
                        url: createURL("createVpnGateway"),
                        data: {
                            vpcid: args.context.vpc[0].id
                        },
                        success: function(json) {
                            var jid = json.createvpngatewayresponse.jobid;
                            args.response.success({
                                _custom: {
                                    jobId: jid,
                                    getUpdatedItem: function(json) {
                                        return json.queryasyncjobresultresponse.jobresult.vpngateway;
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

            sections: {
                vpnGateway: {
                    type: 'select',
                    title: 'label.VPN.gateway',
                    listView: {
                        id: 'vpnGateway',
                        label: 'label.VPN.gateway',
                        fields: {
                            publicip: {
                                label: 'label.ip.address'
                            },
                            account: {
                                label: 'label.account'
                            },
                            domain: {
                                label: 'label.domain'
                            }
                        },
                        dataProvider: function(args) {
                            var array1 = [];
                            if (args.filterBy != null) {
                                if (args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
                                    switch (args.filterBy.search.by) {
                                        case "name":
                                            if (args.filterBy.search.value.length > 0)
                                                array1.push("&keyword=" + args.filterBy.search.value);
                                            break;
                                    }
                                }
                            }
                            $.ajax({
                                url: createURL("listVpnGateways&listAll=true&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
                                data: {
                                    vpcid: args.context.vpc[0].id
                                },
                                async: false,
                                success: function(json) {
                                    var items = json.listvpngatewaysresponse.vpngateway;
                                    args.response.success({
                                        data: items
                                    });
                                }
                            });
                        },
                        detailView: {
                            name: 'label.details',
                            actions: {
                                remove: {
                                    label: 'label.delete.VPN.gateway',
                                    messages: {
                                        confirm: function(args) {
                                            return 'message.delete.VPN.gateway';
                                        },
                                        notification: function(args) {
                                            return 'label.delete.VPN.gateway';
                                        }
                                    },
                                    action: function(args) {
                                        $.ajax({
                                            url: createURL("deleteVpnGateway"),
                                            data: {
                                                id: args.context.vpnGateway[0].id
                                            },
                                            success: function(json) {
                                                var jid = json.deletevpngatewayresponse.jobid;
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
                                        publicip: {
                                            label: 'label.ip.address'
                                        }
                                    }, {
                                        id: {
                                            label: 'label.id'
                                        },
                                        domain: {
                                            label: 'label.domain'
                                        },
                                        account: {
                                            label: 'label.account'
                                        }
                                    }],
                                    dataProvider: function(args) {
                                        $.ajax({
                                            url: createURL("listVpnGateways"),
                                            data: {
                                                id: args.context.vpnGateway[0].id
                                            },
                                            async: true,
                                            success: function(json) {
                                                var item = json.listvpngatewaysresponse.vpngateway[0];
                                                args.response.success({
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
                vpnConnection: {
                    type: 'select',
                    title: 'label.VPN.connection',
                    listView: {
                        id: 'vpnConnection',
                        label: 'label.VPN.connection',
                        fields: {
                            publicip: {
                                label: 'label.ip.address'
                            },
                            gateway: {
                                label: 'label.gateway'
                            },
                            state: {
                                label: 'label.state',
                                indicator: {
                                    'Connected': 'on',
                                    'Disconnected': 'off',
                                    'Error': 'off'
                                }
                            },
                            ipsecpsk: {
                                label: 'label.IPsec.preshared.key'
                            },
                            ikepolicy: {
                                label: 'label.IKE.policy'
                            },
                            esppolicy: {
                                label: 'label.ESP.policy'
                            }
                        },
                        dataProvider: function(args) {
                            var array1 = [];
                            if (args.filterBy != null) {
                                if (args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
                                    switch (args.filterBy.search.by) {
                                        case "name":
                                            if (args.filterBy.search.value.length > 0)
                                                array1.push("&keyword=" + args.filterBy.search.value);
                                            break;
                                    }
                                }
                            }
                            $.ajax({
                                url: createURL("listVpnConnections&listAll=true&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
                                data: {
                                    vpcid: args.context.vpc[0].id
                                },
                                success: function(json) {
                                    var items = json.listvpnconnectionsresponse.vpnconnection;
                                    args.response.success({
                                        data: items
                                    });
                                }
                            });
                        },

                        actions: {
                            add: {
                                label: 'label.create.VPN.connection',
                                messages: {
                                    notification: function(args) {
                                        return 'label.create.VPN.connection';
                                    }
                                },
                                createForm: {
                                    title: 'label.create.VPN.connection',
                                    fields: {
                                        vpncustomergatewayid: {
                                            label: 'label.VPN.customer.gateway',
                                            validation: {
                                                required: true
                                            },
                                            select: function(args) {
                                                $.ajax({
                                                    url: createURL("listVpnCustomerGateways"),
                                                    data: {
                                                        listAll: true
                                                    },
                                                    success: function(json) {
                                                        var items = json.listvpncustomergatewaysresponse.vpncustomergateway ? json.listvpncustomergatewaysresponse.vpncustomergateway : [];
                                                        args.response.success({
                                                            data: $.map(items, function(item) {
                                                                return {
                                                                    id: item.id,
                                                                    description: item.name
                                                                };
                                                            })

                                                        });
                                                    }
                                                });
                                            }
                                        }
                                    }
                                },
                                action: function(args) {
                                    var vpngatewayid = null;
                                    $.ajax({
                                        url: createURL('listVpnGateways'),
                                        data: {
                                            vpcid: args.context.vpc[0].id
                                        },
                                        async: false,
                                        success: function(json) {
                                            var items = json.listvpngatewaysresponse.vpngateway;
                                            if (items != null && items.length > 0) {
                                                vpngatewayid = items[0].id;
                                            }
                                        }
                                    });
                                    if (vpngatewayid == null) {
                                        args.response.error('The selected VPC does not have a VPN gateway. Please create a VPN gateway for the VPC first.');
                                        return;
                                    }

                                    $.ajax({
                                        url: createURL('createVpnConnection'),
                                        data: {
                                            s2svpngatewayid: vpngatewayid,
                                            s2scustomergatewayid: args.data.vpncustomergatewayid
                                        },
                                        success: function(json) {
                                            var jid = json.createvpnconnectionresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid,
                                                    getUpdatedItem: function(json) {
                                                        return json.queryasyncjobresultresponse.jobresult.vpnconnection;
                                                    }
                                                }
                                            });
                                        },
                                        error: function(xmlHttpResponse) {
                                            args.response.error(parseXMLHttpResponse(xmlHttpResponse));
                                        }
                                    });
                                },
                                notification: {
                                    poll: pollAsyncJobResult
                                }
                            }
                        },

                        detailView: {
                            name: 'label.details',
                            tabs: {
                                details: {
                                    title: 'label.details',
                                    fields: [{
                                        id: {
                                            label: 'label.id'
                                        },

                                        //s2svpngatewayid: { label: 'VPN gateway ID' },
                                        publicip: {
                                            label: 'label.ip.address'
                                        },

                                        //s2scustomergatewayid: { label: 'Customer gateway ID' },
                                        gateway: {
                                            label: 'label.gateway'
                                        },
                                        cidrlist: {
                                            label: 'label.CIDR.list'
                                        },
                                        ipsecpsk: {
                                            label: 'label.IPsec.preshared.key'
                                        },
                                        ikepolicy: {
                                            label: 'label.IKE.policy'
                                        },
                                        esppolicy: {
                                            label: 'label.ESP.policy'
                                        },
                                        ikelifetime: {
                                            label: 'label.IKE.lifetime'
                                        },
                                        esplifetime: {
                                            label: 'label.ESP.lifetime'
                                        },
                                        dpd: {
                                            label: 'label.dead.peer.detection',
                                            converter: function(str) {
                                                return str ? 'Yes' : 'No';
                                            }
                                        },
                                        state: {
                                            label: 'label.state'
                                        },
                                        created: {
                                            label: 'label.date',
                                            converter: cloudStack.converters.toLocalDate
                                        }
                                    }],

                                    dataProvider: function(args) {
                                        $.ajax({
                                            url: createURL("listVpnConnections&id=" + args.context.vpnConnection[0].id),
                                            dataType: "json",
                                            async: true,
                                            success: function(json) {
                                                var item = json.listvpnconnectionsresponse.vpnconnection[0];
                                                args.response.success({
                                                    data: item
                                                });
                                            }
                                        });
                                    }
                                }
                            },
                            actions: {
                                restart: {
                                    label: 'label.reset.VPN.connection',
                                    messages: {
                                        confirm: function(args) {
                                            return 'message.reset.VPN.connection';
                                        },
                                        notification: function(args) {
                                            return 'label.reset.VPN.connection';
                                        }
                                    },
                                    action: function(args) {
                                        $.ajax({
                                            url: createURL("resetVpnConnection"),
                                            data: {
                                                id: args.context.vpnConnection[0].id
                                            },
                                            dataType: "json",
                                            async: true,
                                            success: function(json) {
                                                var jid = json.resetvpnconnectionresponse.jobid;
                                                args.response.success({
                                                    _custom: {
                                                        jobId: jid,
                                                        getUpdatedItem: function(json) {
                                                            return json.queryasyncjobresultresponse.jobresult.vpnconnection;
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
                                    label: 'label.delete.VPN.connection',
                                    messages: {
                                        confirm: function(args) {
                                            return 'message.delete.VPN.connection';
                                        },
                                        notification: function(args) {
                                            return 'label.delete.VPN.connection';
                                        }
                                    },
                                    action: function(args) {
                                        $.ajax({
                                            url: createURL("deleteVpnConnection"),
                                            dataType: "json",
                                            data: {
                                                id: args.context.vpnConnection[0].id
                                            },
                                            async: true,
                                            success: function(json) {
                                                var jid = json.deletevpnconnectionresponse.jobid;
                                                args.response.success({
                                                    _custom: {
                                                        jobId: jid,
                                                        getUpdatedItem: function(json) {
                                                            return json.queryasyncjobresultresponse.jobresult.vpnconnection;
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
                            }
                        }
                    }
                }
            }
        },

        tiers: {
            detailView: { //duplicate from cloudStack.sections.network.sections.networks.listView.detailView (begin)
                name: 'Guest network details',
                viewAll: {
                    path: 'network.ipAddresses',
                    label: 'label.menu.ipaddresses',
                    preFilter: function(args) {
                        return false;

                        /// Disabled
                        if (args.context.networks[0].state == 'Destroyed')
                            return false;

                        var services = args.context.networks[0].service;
                        if (services == null)
                            return false;

                        if (args.context.networks[0].type == "Isolated") {
                            for (var i = 0; i < services.length; i++) {
                                var service = services[i];
                                if (service.name == "SourceNat") {
                                    return true;
                                }
                            }
                        } else if (args.context.networks[0].type == "Shared") {
                            var havingSecurityGroupService = false;
                            var havingElasticIpCapability = false;
                            var havingElasticLbCapability = false;

                            for (var i = 0; i < services.length; i++) {
                                var service = services[i];
                                if (service.name == "SecurityGroup") {
                                    havingSecurityGroupService = true;
                                } else if (service.name == "StaticNat") {
                                    $(service.capability).each(function() {
                                        if (this.name == "ElasticIp" && this.value == "true") {
                                            havingElasticIpCapability = true;
                                            return false; //break $.each() loop
                                        }
                                    });
                                } else if (service.name == "Lb") {
                                    $(service.capability).each(function() {
                                        if (this.name == "ElasticLb" && this.value == "true") {
                                            havingElasticLbCapability = true;
                                            return false; //break $.each() loop
                                        }
                                    });
                                }
                            }

                            if (havingSecurityGroupService == true && havingElasticIpCapability == true && havingElasticLbCapability == true)
                                return true;
                            else
                                return false;
                        }

                        return false;
                    }
                },
                actions: {
                    edit: {
                        label: 'label.edit',
                        messages: {
                            notification: function(args) {
                                return 'label.edit.network.details';
                            }
                        },
                        action: function(args) {
                            var array1 = [];
                            array1.push("&name=" + todb(args.data.name));
                            array1.push("&displaytext=" + todb(args.data.displaytext));

                            //args.data.networkdomain is null when networkdomain field is hidden
                            if (args.data.networkdomain != null && args.data.networkdomain != args.context.networks[0].networkdomain)
                                array1.push("&networkdomain=" + todb(args.data.networkdomain));

                            //args.data.networkofferingid is null when networkofferingid field is hidden
                            if (args.data.networkofferingid != null && args.data.networkofferingid != args.context.networks[0].networkofferingid) {
                                array1.push("&networkofferingid=" + todb(args.data.networkofferingid));

                                if (args.context.networks[0].type == "Isolated") { //Isolated network
                                    cloudStack.dialog.confirm({
                                        message: 'Do you want to keep the current guest network CIDR unchanged?',
                                        action: function() { //"Yes"	button is clicked
                                            array1.push("&changecidr=false");
                                            $.ajax({
                                                url: createURL("updateNetwork&id=" + args.context.networks[0].id + array1.join("")),
                                                dataType: "json",
                                                success: function(json) {
                                                    var jid = json.updatenetworkresponse.jobid;
                                                    args.response.success({
                                                        _custom: {
                                                            jobId: jid,
                                                            getUpdatedItem: function(json) {
                                                                var item = json.queryasyncjobresultresponse.jobresult.network;
                                                                return {
                                                                    data: item
                                                                };
                                                            }
                                                        }
                                                    });
                                                },
                                                error: function(json) {
                                                    args.response.error(parseXMLHttpResponse(json));
                                                }
                                            });
                                        },
                                        cancelAction: function() { //"Cancel" button is clicked
                                            array1.push("&changecidr=true");
                                            $.ajax({
                                                url: createURL("updateNetwork&id=" + args.context.networks[0].id + array1.join("")),
                                                dataType: "json",
                                                success: function(json) {
                                                    var jid = json.updatenetworkresponse.jobid;
                                                    args.response.success({
                                                        _custom: {
                                                            jobId: jid,
                                                            getUpdatedItem: function(json) {
                                                                var item = json.queryasyncjobresultresponse.jobresult.network;
                                                                return {
                                                                    data: item
                                                                };
                                                            }
                                                        }
                                                    });
                                                },
                                                error: function(json) {
                                                    args.response.error(parseXMLHttpResponse(json));
                                                }
                                            });
                                        }
                                    });
                                    return;
                                }
                            }

                            $.ajax({
                                url: createURL("updateNetwork&id=" + args.context.networks[0].id + array1.join("")),
                                dataType: "json",
                                success: function(json) {
                                    var jid = json.updatenetworkresponse.jobid;
                                    args.response.success({
                                        _custom: {
                                            jobId: jid,
                                            getUpdatedItem: function(json) {
                                                var item = json.queryasyncjobresultresponse.jobresult.network;
                                                return {
                                                    data: item
                                                };
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

                    'restart': {
                        label: 'label.restart.network',
                        createForm: {
                            title: 'label.restart.network',
                            desc: 'message.restart.network',
                            preFilter: function(args) {
                                var zoneObj;
                                $.ajax({
                                    url: createURL("listZones&id=" + args.context.networks[0].zoneid),
                                    dataType: "json",
                                    async: false,
                                    success: function(json) {
                                        zoneObj = json.listzonesresponse.zone[0];
                                    }
                                });
                                if (zoneObj.networktype == "Basic") {
                                    args.$form.find('.form-item[rel=cleanup]').find('input').removeAttr('checked'); //unchecked
                                    args.$form.find('.form-item[rel=cleanup]').hide(); //hidden
                                } else {
                                    args.$form.find('.form-item[rel=cleanup]').find('input').attr('checked', 'checked'); //checked
                                    args.$form.find('.form-item[rel=cleanup]').css('display', 'inline-block'); //shown
                                }
                            },
                            fields: {
                                cleanup: {
                                    label: 'label.clean.up',
                                    isBoolean: true
                                }
                            }
                        },
                        messages: {
                            notification: function(args) {
                                return 'label.restart.network';
                            }
                        },
                        action: function(args) {
                            var array1 = [];
                            array1.push("&cleanup=" + (args.data.cleanup == "on"));
                            $.ajax({
                                url: createURL("restartNetwork&id=" + args.context.networks[0].id + array1.join("")),
                                dataType: "json",
                                async: true,
                                success: function(json) {
                                    var jid = json.restartnetworkresponse.jobid;
                                    args.response.success({
                                        _custom: {
                                            jobId: jid,
                                            getUpdatedItem: function(json) {
                                                return json.queryasyncjobresultresponse.jobresult.network;
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
                        label: 'label.action.delete.network',
                        messages: {
                            confirm: function(args) {
                                return 'message.action.delete.network';
                            },
                            notification: function(args) {
                                return 'label.action.delete.network';
                            }
                        },
                        action: function(args) {
                            $.ajax({
                                url: createURL("deleteNetwork&id=" + args.context.networks[0].id),
                                dataType: "json",
                                async: true,
                                success: function(json) {
                                    var jid = json.deletenetworkresponse.jobid;
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

                    replaceacllist: {
                        label: 'Replace ACL List',
                        createForm: {
                            title: 'Replace ACL List',
                            label: 'Replace ACL List',
                            fields: {
                                aclid: {
                                    label: 'ACL',
                                    select: function(args) {
                                        $.ajax({
                                            url: createURL('listNetworkACLLists&vpcid=' + args.context.vpc[0].id),
                                            dataType: 'json',
                                            async: true,
                                            success: function(json) {
                                                var objs = json.listnetworkacllistsresponse.networkacllist;
                                                var items = [];

                                                $(objs).each(function() {
                                                    if (this.id == args.context.networks[0].aclid) {
                                                        return true;
                                                    }

                                                    items.push({
                                                        id: this.id,
                                                        description: this.name
                                                    });

                                                    return true;
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
                            $.ajax({
                                url: createURL("replaceNetworkACLList&networkid=" + args.context.networks[0].id + "&aclid=" + args.data.aclid),
                                dataType: "json",
                                success: function(json) {
                                    var jid = json.replacenetworkacllistresponse.jobid;

                                    args.response.success({
                                        _custom: {
                                            jobId: jid,
                                            getUpdatedItem: function(json) {
                                                var network = args.context.networks[0];

                                                network.aclid = args.data.aclid;

                                                return {
                                                    aclid: args.data.aclid
                                                };
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
                        },

                        messages: {
                            confirm: function(args) {
                                return 'Do you want to replace the ACL with a new one ?';
                            },
                            notification: function(args) {
                                return 'ACL replaced';
                            }
                        }
                    }
                },

                tabFilter: function(args) {
                	var hiddenTabs = ['ipAddresses', 'acl']; // Disable IP address tab; it is redundant with 'view all' button
                	
                	var networkOfferingHavingELB = false;                                       
                    var services = args.context.networks[0].service;
                    if(services != null) {
                    	for(var i = 0; i < services.length; i++) {                    		
                    		if (services[i].name == "Lb") {
                    			var capabilities = services[i].capability;
                    			if(capabilities != null) {
                    				for(var k = 0; k < capabilities.length; k++) {
                    					if(capabilities[k].name == "ElasticLb") {
                    						networkOfferingHavingELB = true;
                    						break;                    					
                    					}
                    				}
                    			}  
                                break;
                            }                    		
                    	}
                    }   
                    if (networkOfferingHavingELB == false) {
                        hiddenTabs.push("addloadBalancer");
                    }
                    
                    return hiddenTabs;
                },

                isMaximized: true,
                tabs: {
                    details: {
                        title: 'Network Details',
                        preFilter: function(args) {
                            var hiddenFields = [];
                            var zone;

                            $.ajax({
                                url: createURL('listZones'),
                                data: {
                                    id: args.context.networks[0].zoneid
                                },
                                async: false,
                                success: function(json) {
                                    zone = json.listzonesresponse.zone[0];
                                }
                            });

                            if (zone.networktype == "Basic") {
                                hiddenFields.push("account");
                                hiddenFields.push("gateway");
                                hiddenFields.push("vlan");
                                hiddenFields.push("cidr");
                                //hiddenFields.push("netmask");
                            }

                            if (args.context.networks[0].type == "Isolated") {
                                hiddenFields.push("networkofferingdisplaytext");
                                hiddenFields.push("networkdomaintext");
                                hiddenFields.push("gateway");
                                hiddenFields.push("networkofferingname");
                                //hiddenFields.push("netmask");
                            } else { //selectedGuestNetworkObj.type == "Shared"
                                hiddenFields.push("networkofferingid");
                                hiddenFields.push("networkdomain");
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
                            displaytext: {
                                label: 'label.description',
                                isEditable: true
                            },
                            type: {
                                label: 'label.type'
                            },
                            state: {
                                label: 'label.state'
                            },

                            ispersistent: {
                                label: 'Persistent ',
                                converter: cloudStack.converters.toBooleanText

                            },

                            restartrequired: {
                                label: 'label.restart.required',
                                converter: function(booleanValue) {
                                    if (booleanValue == true)
                                        return "<font color='red'>Yes</font>";
                                    else if (booleanValue == false)
                                        return "No";
                                }
                            },
                            vlan: {
                                label: 'label.vlan.id'
                            },

                            networkofferingname: {
                                label: 'label.network.offering'
                            },

                            networkofferingid: {
                                label: 'label.network.offering',
                                isEditable: true,
                                select: function(args) {
                                    if (args.context.networks[0].state == 'Destroyed') {
                                        args.response.success({
                                            data: []
                                        });
                                        return;
                                    }

                                    var items = [];
                                    $.ajax({
                                        url: createURL("listNetworkOfferings&networkid=" + args.context.networks[0].id),
                                        dataType: "json",
                                        async: false,
                                        success: function(json) {
                                            var networkOfferingObjs = json.listnetworkofferingsresponse.networkoffering;
                                            $(networkOfferingObjs).each(function() {
                                                items.push({
                                                    id: this.id,
                                                    description: this.displaytext
                                                });
                                            });
                                        }
                                    });
                                   
                                    //include currently selected network offeirng to dropdown
                                    items.push({
                                        id: args.context.networks[0].networkofferingid,
                                        description: args.context.networks[0].networkofferingdisplaytext
                                    });                             
                                    
                                    args.response.success({
                                        data: items
                                    });
                                }
                            },

                            gateway: {
                                label: 'label.gateway'
                            },

                            //netmask: { label: 'Netmask' },
                            cidr: {
                                label: 'label.cidr'
                            },

                            networkdomaintext: {
                                label: 'label.network.domain.text'
                            },
                            networkdomain: {
                                label: 'label.network.domain',
                                isEditable: true
                            },

                            aclname: {
                                label: 'ACL name'
                            },
                            //aclid:{label:'ACL id'},

                            domain: {
                                label: 'label.domain'
                            },
                            account: {
                                label: 'label.account'
                            }
                        }],
                        dataProvider: function(args) {
                            $.ajax({
                                url: createURL("listNetworks&id=" + args.context.networks[0].id + "&listAll=true"), //pass "&listAll=true" to "listNetworks&id=xxxxxxxx" for now before API gets fixed.
                                data: {
                                    listAll: true
                                },
                                dataType: "json",
                                async: true,
                                success: function(json) {
                                    var jsonObj = json.listnetworksresponse.network[0];
                                    if (jsonObj.aclid != null) {

                                        $.ajax({
                                            url: createURL("listNetworkACLLists&id=" + jsonObj.aclid),
                                            dataType: "json",
                                            success: function(json) {
                                                var aclObj = json.listnetworkacllistsresponse.networkacllist[0];
                                                args.response.success({
                                                    actionFilter: cloudStack.actionFilter.guestNetwork,
                                                    data: $.extend(jsonObj, {
                                                        aclname: aclObj.name
                                                    })

                                                });
                                            },
                                            error: function(json) {

                                                args.response.error(parseXMLHttpResponse(json));
                                            }
                                        });
                                    } else {
                                        args.response.success({
                                            actionFilter: cloudStack.actionFilter.guestNetwork,
                                            data: $.extend(jsonObj, {
                                                aclname: 'None'
                                            })

                                        });

                                    }
                                }
                            });
                        }
                    },

                    acl: {
                        title: 'label.network.ACL',
                        custom: function(args) {
                            // Widget renders ACL multi-edit, overriding this fn
                            return $('<div>');
                        }
                    },

                    ipAddresses: {
                        title: 'label.menu.ipaddresses',
                        custom: function(args) {
                            // Widget renders IP addresses, overriding this fn
                            return $('<div>');
                        }
                    },

                    addloadBalancer: {
                        title: 'label.add.load.balancer',
                        custom: function(args) {
                            var context = args.context;

                            return $('<div>').multiEdit({
                                context: context,
                                listView: $.extend(true, {}, cloudStack.sections.instances, {
                                    listView: {
                                        dataProvider: function(args) {
                                            var networkid;
                                            if ('vpc' in args.context)
                                                networkid = args.context.multiData.tier;
                                            else
                                                networkid = args.context.ipAddresses[0].associatednetworkid;

                                            var data = {
                                                page: args.page,
                                                pageSize: pageSize,
                                                networkid: networkid,
                                                listAll: true
                                            };

                                            $.ajax({
                                                url: createURL('listVirtualMachines'),
                                                data: data,
                                                dataType: 'json',
                                                async: true,
                                                success: function(data) {
                                                    args.response.success({
                                                        data: $.grep(
                                                            data.listvirtualmachinesresponse.virtualmachine ?
                                                            data.listvirtualmachinesresponse.virtualmachine : [],
                                                            function(instance) {
                                                                return $.inArray(instance.state, [
                                                                    'Destroyed'
                                                                ]) == -1;
                                                            }
                                                        )
                                                    });
                                                },
                                                error: function(data) {
                                                    args.response.error(parseXMLHttpResponse(data));
                                                }
                                            });
                                        }
                                    }
                                }),
                                multipleAdd: true,
                                fields: {
                                    'name': {
                                        edit: true,
                                        label: 'label.name'
                                    },
                                    'publicport': {
                                        edit: true,
                                        label: 'label.public.port'
                                    },
                                    'privateport': {
                                        edit: true,
                                        label: 'label.private.port'
                                    },
                                    'algorithm': {
                                        label: 'label.algorithm',
                                        select: function(args) {
                                            args.response.success({
                                                data: [{
                                                    name: 'roundrobin',
                                                    description: _l('label.round.robin')
                                                }, {
                                                    name: 'leastconn',
                                                    description: _l('label.least.connections')
                                                }, {
                                                    name: 'source',
                                                    description: _l('label.source')
                                                }]
                                            });
                                        }
                                    },
                                    'sticky': {
                                        label: 'label.stickiness',
                                        custom: {
                                            buttonLabel: 'label.configure',
                                            action: cloudStack.lbStickyPolicy.dialog()
                                        }
                                    },
                                    'add-vm': {
                                        label: 'label.add.vm',
                                        addButton: true
                                    }
                                },
                                add: {
                                    label: 'label.add.vm',
                                    action: function(args) {
                                        var data = {
                                            algorithm: args.data.algorithm,
                                            name: args.data.name,
                                            privateport: args.data.privateport,
                                            publicport: args.data.publicport,
                                            openfirewall: false,
                                            domainid: g_domainid,
                                            account: g_account
                                        };

                                        if ('vpc' in args.context) { //from VPC section
                                            if (args.data.tier == null) {
                                                args.response.error('Tier is required');
                                                return;
                                            }
                                            $.extend(data, {
                                                networkid: args.data.tier
                                            });
                                        } else { //from Guest Network section
                                            $.extend(data, {
                                                networkid: args.context.networks[0].id
                                            });
                                        }

                                        var stickyData = $.extend(true, {}, args.data.sticky);

                                        $.ajax({
                                            url: createURL('createLoadBalancerRule'),
                                            data: data,
                                            dataType: 'json',
                                            async: true,
                                            success: function(data) {
                                                var itemData = args.itemData;
                                                var jobID = data.createloadbalancerruleresponse.jobid;

                                                $.ajax({
                                                    url: createURL('assignToLoadBalancerRule'),
                                                    data: {
                                                        id: data.createloadbalancerruleresponse.id,
                                                        virtualmachineids: $.map(itemData, function(elem) {
                                                            return elem.id;
                                                        }).join(',')
                                                    },
                                                    dataType: 'json',
                                                    async: true,
                                                    success: function(data) {
                                                        var lbCreationComplete = false;

                                                        args.response.success({
                                                            _custom: {
                                                                jobId: jobID
                                                            },
                                                            notification: {
                                                                label: 'label.add.load.balancer',
                                                                poll: function(args) {
                                                                    var complete = args.complete;
                                                                    var error = args.error;

                                                                    pollAsyncJobResult({
                                                                        _custom: args._custom,
                                                                        complete: function(args) {
                                                                            if (lbCreationComplete) {
                                                                                return;
                                                                            }

                                                                            lbCreationComplete = true;
                                                                            cloudStack.dialog.notice({
                                                                                message: _l('message.add.load.balancer.under.ip') + args.data.loadbalancer.publicip
                                                                            });

                                                                            if (stickyData &&
                                                                                stickyData.methodname &&
                                                                                stickyData.methodname != 'None') {
                                                                                cloudStack.lbStickyPolicy.actions.add(
                                                                                    args.data.loadbalancer.id,
                                                                                    stickyData,
                                                                                    complete, // Complete
                                                                                    complete // Error
                                                                                );
                                                                            } else {
                                                                                complete();
                                                                            }
                                                                        },
                                                                        error: error
                                                                    });
                                                                }
                                                            }
                                                        });
                                                    },
                                                    error: function(data) {
                                                        args.response.error(parseXMLHttpResponse(data));
                                                    }
                                                });
                                            },
                                            error: function(data) {
                                                args.response.error(parseXMLHttpResponse(data));
                                            }
                                        });
                                    }
                                },
                                dataProvider: function(args) {
                                    args.response.success({ //no LB listing in AddLoadBalancer tab
                                        data: []
                                    });
                                }
                            });
                        }
                    }
                }
            }, //duplicate from cloudStack.sections.network.sections.networks.listView.detailView (begin)

            actionPreFilter: function(args) {
                var tier = args.context.networks[0];
                var state = tier.state;

                return state == 'Running' ? ['start'] : ['stop'];
            },
            actions: {
                add: {
                    label: 'label.add.new.tier',
                    createForm: {
                        title: 'label.add.new.tier',
                        fields: {
                            name: {
                                label: 'label.name',
                                validation: {
                                    required: true
                                },
                                docID: 'helpTierName'
                            },
                            networkOfferingId: {
                                label: 'label.network.offering',
                                docID: 'helpTierNetworkOffering',
                                validation: {
                                    required: true
                                },
                                dependsOn: 'zoneId',
                                select: function(args) {
                                    var publicLbNetworkExists = false;
                                    $.ajax({
                                        url: createURL('listNetworks'),
                                        data: {
                                            vpcid: args.context.vpc[0].id,
                                            supportedservices: 'LB'
                                        },
                                        success: function(json) {
                                            var publicLbNetworkExists = false;

                                            var lbNetworks = json.listnetworksresponse.network;
                                            if (lbNetworks != null) {
                                                for (var i = 0; i < lbNetworks.length; i++) {
                                                    var thisNetworkOfferingIncludesPublicLbService = false;
                                                    $.ajax({
                                                        url: createURL('listNetworkOfferings'),
                                                        data: {
                                                            id: lbNetworks[i].networkofferingid
                                                        },
                                                        async: false,
                                                        success: function(json) {
                                                            var networkOffering = json.listnetworkofferingsresponse.networkoffering[0];
                                                            $(networkOffering.service).each(function() {
                                                                var thisService = this;
                                                                if (thisService.name == "Lb" && lbProviderMap.publicLb.vpc.indexOf(thisService.provider[0].name) != -1) {
                                                                    thisNetworkOfferingIncludesPublicLbService = true;
                                                                    return false; //break $.each() loop
                                                                }
                                                            });
                                                        }
                                                    });
                                                    if (thisNetworkOfferingIncludesPublicLbService == true) {
                                                        publicLbNetworkExists = true;
                                                        break; //break for loop
                                                    }
                                                }
                                            }

                                            $.ajax({
                                                url: createURL('listNetworkOfferings'),
                                                data: {
                                                    forvpc: true,
                                                    zoneid: args.zoneId,
                                                    guestiptype: 'Isolated',
                                                    supportedServices: 'SourceNat',
                                                    state: 'Enabled'
                                                },
                                                success: function(json) {
                                                    var networkOfferings = json.listnetworkofferingsresponse.networkoffering;
                                                    args.$select.change(function() {
                                                        var $vlan = args.$select.closest('form').find('[rel=vlan]');
                                                        var networkOffering = $.grep(
                                                            networkOfferings, function(netoffer) {
                                                                return netoffer.id == args.$select.val();
                                                            }
                                                        )[0];

                                                        if (networkOffering.specifyvlan) {
                                                            $vlan.css('display', 'inline-block');
                                                        } else {
                                                            $vlan.hide();
                                                        }
                                                    });

                                                    //only one network(tier) is allowed to have PublicLb (i.e. provider is PublicLb provider like "VpcVirtualRouter", "Netscaler") in a VPC
                                                    var items;
                                                    if (publicLbNetworkExists == true) { //so, if a PublicLb network(tier) already exists in the vpc, exclude PublicLb network offerings from dropdown
                                                        items = $.grep(networkOfferings, function(networkOffering) {
                                                            var thisNetworkOfferingIncludesPublicLbService = false;
                                                            $(networkOffering.service).each(function() {
                                                                var thisService = this;
                                                                if (thisService.name == "Lb" && lbProviderMap.publicLb.vpc.indexOf(thisService.provider[0].name) != -1) {
                                                                    thisNetworkOfferingIncludesPublicLbService = true;
                                                                    return false; //break $.each() loop
                                                                }
                                                            });
                                                            return !thisNetworkOfferingIncludesPublicLbService;
                                                        });
                                                    } else {
                                                        items = networkOfferings;
                                                    }

                                                    args.response.success({
                                                        data: $.map(items, function(item) {
                                                            return {
                                                                id: item.id,
                                                                description: item.name
                                                            };
                                                        })
                                                    });
                                                }
                                            });
                                        }
                                    });
                                }
                            },
                            vlan: {
                                label: 'VLAN',
                                validation: {
                                    required: true
                                },
                                isHidden: true
                            },
                            gateway: {
                                label: 'label.gateway',
                                docID: 'helpTierGateway',
                                validation: {
                                    required: true
                                }
                            },
                            netmask: {
                                label: 'label.netmask',
                                docID: 'helpTierNetmask',
                                validation: {
                                    required: true
                                }
                            },

                            aclid: {
                                label: 'ACL',
                                select: function(args) {
                                    $.ajax({
                                        url: createURL('listNetworkACLLists&vpcid=' + args.context.vpc[0].id),
                                        dataType: 'json',
                                        async: true,
                                        success: function(json) {
                                            var objs = json.listnetworkacllistsresponse.networkacllist;
                                            var items = [];
                                            items.push({
                                                id: '',
                                                description: ''
                                            });
                                            $(objs).each(function() {

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
                        var dataObj = {
                            zoneId: args.context.vpc[0].zoneid,
                            vpcid: args.context.vpc[0].id,
                            domainid: args.context.vpc[0].domainid,
                            account: args.context.vpc[0].account,
                            networkOfferingId: args.data.networkOfferingId,
                            name: args.data.name,
                            displayText: args.data.name,
                            gateway: args.data.gateway,
                            netmask: args.data.netmask
                        };

                        if (args.data.aclid != '')
                            $.extend(dataObj, {
                                aclid: args.data.aclid
                            });

                        if (args.$form.find('.form-item[rel=vlan]').is(':visible')) {
                            $.extend(dataObj, {
                                vlan: args.data.vlan
                            });
                        }

                        $.ajax({
                            url: createURL('createNetwork'),
                            dataType: 'json',
                            data: dataObj,
                            success: function(json) {
                                args.response.success({
                                    data: json.createnetworkresponse.network
                                });
                            },
                            error: function(XMLHttpResponse) {
                                args.response.error(parseXMLHttpResponse(XMLHttpResponse));
                            }
                        });
                    },
                    messages: {
                        notification: function() {
                            return 'Add new tier';
                        }
                    }
                },

                /*
         start: {
         label: 'Start tier',
         shortLabel: 'Start',
         action: function(args) {
         args.response.success();
         },
         notification: {
         poll: function(args) { args.complete({ data: { state: 'Running' } }); }
         }
         },
         */

                /*
         stop: {
         label: 'Stop tier',
         shortLabel: 'Stop',
         action: function(args) {
         args.response.success();
         },
         notification: {
         poll: function(args) { args.complete({ data: { state: 'Stopped' } }); }
         }
         },
         */

                addVM: {
                    label: 'label.add.VM.to.tier',
                    shortLabel: 'label.add.vm',
                    action: cloudStack.uiCustom.instanceWizard(
                        $.extend(true, {}, cloudStack.instanceWizard, {
                            pluginForm: {
                                name: 'vpcTierInstanceWizard'
                            }
                        })
                    ),
                    notification: {
                        poll: pollAsyncJobResult
                    }
                },

                // Removing ACL buttons from the tier chart
                /* acl: {
          label: 'Configure ACL for tier',
          shortLabel: 'ACL',
         multiEdit: aclMultiEdit
        }, */

                remove: {
                    label: 'label.remove.tier',
                    action: function(args) {
                        $.ajax({
                            url: createURL('deleteNetwork'),
                            dataType: "json",
                            data: {
                                id: args.context.networks[0].id
                            },
                            success: function(json) {
                                var jid = json.deletenetworkresponse.jobid;
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

            // Get tiers
            dataProvider: function(args) {
                $.ajax({
                    url: createURL("listNetworks"),
                    dataType: "json",
                    data: {
                        vpcid: args.context.vpc[0].id,
                        //listAll: true,  //do not pass listAll to listNetworks under VPC
                        domainid: args.context.vpc[0].domainid,
                        account: args.context.vpc[0].account
                    },
                    async: true,
                    success: function(json) {
                        var networks = json.listnetworksresponse.network;
                        var networkACLLists, publicIpAddresses, privateGateways, vpnGateways;
                        var error = false;

                        // Get network ACL lists
                        $.ajax({
                            url: createURL('listNetworkACLLists'),
                            data: {
                                'vpcid': args.context.vpc[0].id
                            },
                            async: false,
                            success: function(json) {
                                networkACLLists = json.listnetworkacllistsresponse;
                            },
                            error: function(json) {
                                error = true;
                            }
                        });

                        // Get public IPs
                        $.ajax({
                            url: createURL('listPublicIpAddresses'),
                            async: false,
                            data: {
                                vpcid: args.context.vpc[0].id,
                                listAll: true
                            },
                            success: function(json) {
                                publicIpAddresses = json.listpublicipaddressesresponse;
                            },
                            error: function(json) {
                                error = true;
                            }
                        });

                        // Get private gateways
                        $.ajax({
                            url: createURL('listPrivateGateways'),
                            async: false,
                            data: {
                                'vpcid': args.context.vpc[0].id,
                                listAll: true
                            },
                            success: function(json) {
                                privateGateways = json.listprivategatewaysresponse;
                            },
                            error: function(json) {
                                error = true;
                            }
                        });

                        // Get VPN gateways
                        $.ajax({
                            url: createURL('listVpnGateways'),
                            async: false,
                            data: {
                                'vpcid': args.context.vpc[0].id
                            },
                            success: function(json) {
                                vpnGateways = json.listvpngatewaysresponse;
                            },
                            error: function(json) {
                                error = true;
                            }
                        });

                        args.response.success({
                            routerDashboard: [{
                                id: 'privateGateways',
                                name: 'Private gateways',
                                total: privateGateways.count
                            }, {
                                id: 'publicIPs',
                                name: 'Public IP addresses',
                                total: publicIpAddresses.count
                            }, {
                                id: 'siteToSiteVPNs',
                                name: 'Site-to-site VPNs',
                                total: vpnGateways.count
                            }, {
                                id: 'networkACLLists',
                                name: 'Network ACL lists',
                                total: networkACLLists.count
                            }],
                            tiers: $(networks).map(function(index, tier) {
                                var internalLoadBalancers, publicLbIps, virtualMachines, staticNatIps;

                                // Get internal load balancers
                                $.ajax({
                                    url: createURL('listLoadBalancers&listAll=true'),
                                    async: false,
                                    data: {
                                        networkid: tier.id
                                    },
                                    success: function(json) {
                                        internalLoadBalancers = json.listloadbalancerssresponse;
                                    },
                                    error: function(json) {
                                        error = true;
                                    }
                                });

                                // Get Public LB IPs
                                $.ajax({
                                    url: createURL('listPublicIpAddresses&listAll=true'),
                                    async: false,
                                    data: {
                                        associatednetworkid: tier.id,
                                        forloadbalancing: true
                                    },
                                    success: function(json) {
                                        publicLbIps = json.listpublicipaddressesresponse;
                                    },
                                    error: function(json) {
                                        error = true;
                                    }
                                });

                                // Get static NAT IPs
                                $.ajax({
                                    url: createURL('listPublicIpAddresses&listAll=true'),
                                    async: false,
                                    data: {
                                        associatednetworkid: tier.id,
                                        isstaticnat: true
                                    },
                                    success: function(json) {
                                        staticNatIps = json.listpublicipaddressesresponse;
                                    },
                                    error: function(json) {
                                        error = true;
                                    }
                                });

                                // Get VMs
                                $.ajax({
                                    url: createURL('listVirtualMachines&listAll=true'),
                                    async: false,
                                    data: {
                                        networkid: tier.id
                                    },
                                    success: function(json) {
                                        virtualMachines = json.listvirtualmachinesresponse;
                                    },
                                    error: function(json) {
                                        error = true;
                                    }
                                });

                                // Highlight if any tier VM contains guest network
                                $.grep(
                                    virtualMachines.virtualmachine ? virtualMachines.virtualmachine : [],
                                    function(vm) {
                                        return $.grep(vm.nic,
                                            function(nic) {
                                                return nic.type == 'Shared';
                                            }).length;
                                    }
                                ).length ? tier._highlighted = true : tier._highlighted = false;

                                // Get LB capabilities

                                var lbSchemes = $.grep(
                                    tier.service,
                                    function(service) {
                                        return service.name == 'Lb';
                                    }
                                ).length ? $.grep($.grep(
                                    tier.service,
                                    function(service) {
                                        return service.name == 'Lb';
                                    }
                                )[0].capability, function(capability) {
                                    return capability.name == 'LbSchemes';
                                }) : [];

                                /*      var lbSchemes = $.grep(
                  $.grep(
                    tier.service,
                    function(service) {
                      return service.name == 'Lb';
                    }
                  )[0].capability,
                  function(capability) {
                    return capability.name == 'LbSchemes';
                  }
                );*/

                                var hasLbScheme = function(schemeVal) {
                                    return $.grep(
                                        lbSchemes,
                                        function(scheme) {
                                            return scheme.value == schemeVal;
                                        }
                                    ).length ? true : false;
                                };

                                return $.extend(tier, {
                                    _dashboardItems: [{
                                        id: 'internalLoadBalancers',
                                        name: 'Internal LB',
                                        total: internalLoadBalancers.count,
                                        _disabled: !hasLbScheme('Internal')
                                    }, {
                                        id: 'publicLbIps',
                                        name: 'Public LB IP',
                                        total: publicLbIps.count,
                                        _disabled: !hasLbScheme('Public')
                                    }, {
                                        id: 'tierStaticNATs',
                                        name: 'Static NATs',
                                        total: staticNatIps.count
                                    }, {
                                        id: 'tierVMs',
                                        name: 'Virtual Machines',
                                        total: virtualMachines.count
                                    }]
                                });
                            })
                        });

                        if (error) {
                            cloudStack.dialog.notice({
                                message: 'Error loading dashboard data.'
                            });
                        }
                    }
                });
            }
        }
    };
}(jQuery, cloudStack));
