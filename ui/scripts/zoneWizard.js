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
    var selectedNetworkOfferingHavingSG = false;
    var selectedNetworkOfferingHavingEIP = false;
    var selectedNetworkOfferingHavingELB = false;
    var selectedNetworkOfferingHavingNetscaler = false;
    var returnedPublicVlanIpRanges = []; //public VlanIpRanges returned by API
    var configurationUseLocalStorage = false;
    var skipGuestTrafficStep = false;
    var selectedNetworkOfferingObj = {};
    var baremetalProviders = ["BaremetalDhcpProvider", "BaremetalPxeProvider", "BaremetalUserdataProvider"];
    var selectedBaremetalProviders = [];

    // Makes URL string for traffic label
    var trafficLabelParam = function(trafficTypeID, data, physicalNetworkID) {
        var zoneType = data.zone.networkType;
        var hypervisor = data.zone.hypervisor;
        physicalNetworkID = zoneType == 'Advanced' ? physicalNetworkID : 0;
        var physicalNetwork = data.physicalNetworks ? data.physicalNetworks[physicalNetworkID] : null;
        var trafficConfig = physicalNetwork ? physicalNetwork.trafficTypeConfiguration[trafficTypeID] : null;
                
        var trafficLabel;       
        if (trafficConfig != null) {
        	if ('label' in trafficConfig) {
        		trafficLabel = trafficConfig.label;
        	}
        	else {
        		trafficLabel = '';
        		
        		if ('vSwitchName' in trafficConfig) {
        			trafficLabel += trafficConfig.vSwitchName;
        		}
        		if ('vlanId' in trafficConfig) {
        			if (trafficLabel.length > 0)
        				trafficLabel += ',';
        			trafficLabel += trafficConfig.vlanId;
        		}
        		if ('vSwitchType' in trafficConfig) {
        			if (trafficLabel.length > 0)
        				trafficLabel += ',';
        			trafficLabel += trafficConfig.vSwitchType;
        		}
        		
        		if (trafficLabel.length == 0) { //trafficLabel == ''
        			trafficLabel = null;
        		} else if (trafficLabel.length >= 1) {            		
            		if (trafficLabel.charAt(trafficLabel.length-1) == ',') { //if last character is comma  
            			trafficLabel = trafficLabel.substring(0, trafficLabel.length - 1); //remove the last character (which is comma)  
            		}
        		}        		
        	}
        }
        
        var hypervisorAttr, trafficLabelStr;

        switch (hypervisor) {
            case 'XenServer':
                hypervisorAttr = 'xennetworklabel';
                break;
            case 'KVM':
                hypervisorAttr = 'kvmnetworklabel';
                break;
            case 'VMware':
                hypervisorAttr = 'vmwarenetworklabel';
                break;
            case 'BareMetal':
                hypervisorAttr = 'baremetalnetworklabel';
                break;
            case 'Ovm':
                hypervisorAttr = 'ovmnetworklabel';
                break;
            case 'LXC':
                hypervisorAttr = 'lxcnetworklabel';
                break;
        }

        trafficLabelStr = trafficLabel ? '&' + hypervisorAttr + '=' + trafficLabel : '';

        return trafficLabelStr;
    };

    cloudStack.zoneWizard = {
        // Return required traffic types, for configure physical network screen
        requiredTrafficTypes: function(args) {
            if (args.data.zone.networkType == 'Basic') {
                if (selectedNetworkOfferingHavingEIP || selectedNetworkOfferingHavingELB) {
                    return [
                        'management',
                        'guest',
                        'public'
                    ];
                } else {
                    return [
                        'management',
                        'guest'
                    ];
                }
            } else { // args.data.zone.networkType == 'Advanced'
                if (args.data.zone.sgEnabled != true) {
                    return [
                        'management',
                        'public',
                        'guest'
                    ];
                } else {
                    return [
                        'management',
                        'guest'
                    ];
                }
            }
        },

        disabledTrafficTypes: function(args) {
            if (args.data.zone.networkType == 'Basic') {
                if (selectedNetworkOfferingHavingEIP || selectedNetworkOfferingHavingELB)
                    return [];
                else
                    return ['public'];
            } else { // args.data.zone.networkType == 'Advanced'
                if (args.data.zone.sgEnabled != true) {
                    return [];
                } else {
                    return ['public'];
                }
            }
        },

        cloneTrafficTypes: function(args) {
            if (args.data.zone.networkType == 'Advanced') {
                return ['guest'];
            } else {
                return [];
            }
        },

        customUI: {
            publicTrafficIPRange: function(args) {
                var multiEditData = [];
                var totalIndex = 0;

                return $('<div>').multiEdit({
                    context: args.context,
                    noSelect: true,
                    fields: {
                        'gateway': {
                            edit: true,
                            label: 'label.gateway'
                        },
                        'netmask': {
                            edit: true,
                            label: 'label.netmask'
                        },
                        'vlanid': {
                            edit: true,
                            label: 'label.vlan',
                            isOptional: true
                        },
                        'startip': {
                            edit: true,
                            label: 'label.start.IP'
                        },
                        'endip': {
                            edit: true,
                            label: 'label.end.IP'
                        },
                        'add-rule': {
                            label: 'label.add',
                            addButton: true
                        }
                    },
                    add: {
                        label: 'label.add',
                        action: function(args) {
                            multiEditData.push($.extend(args.data, {
                                index: totalIndex
                            }));

                            totalIndex++;
                            args.response.success();
                        }
                    },
                    actions: {
                        destroy: {
                            label: 'label.remove.rule',
                            action: function(args) {
                                multiEditData = $.grep(multiEditData, function(item) {
                                    return item.index != args.context.multiRule[0].index;
                                });
                                args.response.success();
                            }
                        }
                    },
                    dataProvider: function(args) {
                        args.response.success({
                            data: multiEditData
                        });
                    }
                });
            },

            storageTrafficIPRange: function(args) {
                var multiEditData = [];
                var totalIndex = 0;

                return $('<div>').multiEdit({
                    context: args.context,
                    noSelect: true,
                    fields: {
                        'gateway': {
                            edit: true,
                            label: 'label.gateway'
                        },
                        'netmask': {
                            edit: true,
                            label: 'label.netmask'
                        },
                        'vlan': {
                            edit: true,
                            label: 'label.vlan',
                            isOptional: true
                        },
                        'startip': {
                            edit: true,
                            label: 'label.start.IP'
                        },
                        'endip': {
                            edit: true,
                            label: 'label.end.IP'
                        },
                        'add-rule': {
                            label: 'label.add',
                            addButton: true
                        }
                    },
                    add: {
                        label: 'label.add',
                        action: function(args) {
                            multiEditData.push($.extend(args.data, {
                                index: totalIndex
                            }));

                            totalIndex++;
                            args.response.success();
                        }
                    },
                    actions: {
                        destroy: {
                            label: 'label.remove.rule',
                            action: function(args) {
                                multiEditData = $.grep(multiEditData, function(item) {
                                    return item.index != args.context.multiRule[0].index;
                                });
                                args.response.success();
                            }
                        }
                    },
                    dataProvider: function(args) {
                        args.response.success({
                            data: multiEditData
                        });
                    }
                });
            }
        },

        preFilters: {
            addNetscalerDevice: function(args) { //add Netscaler
                var isShown;
                if (selectedNetworkOfferingHavingNetscaler == true) {
                    isShown = true;
                    $('.conditional.netscaler').show();
                } else {
                    isShown = false;
                    $('.conditional.netscaler').hide();
                }
                return isShown;
            },

            addPublicNetwork: function(args) {
                var isShown;
                var $publicTrafficDesc = $('.zone-wizard:visible').find('#add_zone_public_traffic_desc');
                if (args.data['network-model'] == 'Basic') {
                    if (selectedNetworkOfferingHavingSG == true && selectedNetworkOfferingHavingEIP == true && selectedNetworkOfferingHavingELB == true) {
                        isShown = true;
                    } else {
                        isShown = false;
                    }

                    $publicTrafficDesc.find('#for_basic_zone').css('display', 'inline');
                    $publicTrafficDesc.find('#for_advanced_zone').hide();
                } else { //args.data['network-model'] == 'Advanced'
                    if (args.data["zone-advanced-sg-enabled"] != "on")
                        isShown = true;
                    else
                        isShown = false;

                    $publicTrafficDesc.find('#for_advanced_zone').css('display', 'inline');
                    $publicTrafficDesc.find('#for_basic_zone').hide();
                }
                return isShown;
            },

            setupPhysicalNetwork: function(args) {
                if (args.data['network-model'] == 'Basic' && !(selectedNetworkOfferingHavingELB && selectedNetworkOfferingHavingEIP)) {
                    $('.setup-physical-network .info-desc.conditional.basic').show();
                    $('.setup-physical-network .info-desc.conditional.advanced').hide();
                    $('.subnav li.public-network').hide();
                } else {
                    $('.setup-physical-network .info-desc.conditional.basic').hide();
                    $('.setup-physical-network .info-desc.conditional.advanced').show();
                    $('.subnav li.public-network').show();
                }
                return true; // Both basic & advanced zones show physical network UI
            },

            configureGuestTraffic: function(args) {
                if ((args.data['network-model'] == 'Basic') || (args.data['network-model'] == 'Advanced' && args.data["zone-advanced-sg-enabled"] == "on")) {
                    $('.setup-guest-traffic').addClass('basic');
                    $('.setup-guest-traffic').removeClass('advanced');
                    skipGuestTrafficStep = false; //set value
                } else { //args.data['network-model'] == 'Advanced' && args.data["zone-advanced-sg-enabled"] !=	"on"
                    $('.setup-guest-traffic').removeClass('basic');
                    $('.setup-guest-traffic').addClass('advanced');

                    //skip the step if OVS tunnel manager is enabled
                    skipGuestTrafficStep = false; //reset it before ajax call
                    $.ajax({
                        url: createURL('listConfigurations'),
                        data: {
                            name: 'sdn.ovs.controller'
                        },
                        dataType: "json",
                        async: false,
                        success: function(json) {
                            var items = json.listconfigurationsresponse.configuration; //2 entries returned: 'sdn.ovs.controller', 'sdn.ovs.controller.default.label'
                            $(items).each(function() {
                                if (this.name == 'sdn.ovs.controller') {
                                    if (this.value == 'true' || this.value == true) {
                                        skipGuestTrafficStep = true;
                                    }
                                    return false; //break each loop
                                }
                            });
                        }
                    });
                }
                return !skipGuestTrafficStep;
            },

            configureStorageTraffic: function(args) {
                return $.grep(args.groupedData.physicalNetworks, function(network) {
                    return $.inArray('storage', network.trafficTypes) > -1;
                }).length;
            },

            addHost: function(args) {
                return (args.groupedData.zone.hypervisor != "VMware");
            },

            addPrimaryStorage: function(args) {
                return args.data.localstorageenabled != 'on';
            }
        },

        forms: {
            zone: {
                preFilter: function(args) {
                    var $form = args.$form;

                    if (args.data['network-model'] == 'Basic') { //Basic zone
                        args.$form.find('[rel=networkOfferingId]').show(); //will be used to create a guest network during zone creation
                        args.$form.find('[rel=guestcidraddress]').hide();

                        args.$form.find('[rel=ip6dns1]').hide();
                        args.$form.find('[rel=ip6dns2]').hide();
                    } else { //Advanced zone
                        if (args.data["zone-advanced-sg-enabled"] != "on") { //Advanced SG-disabled zone
                            args.$form.find('[rel=networkOfferingId]').hide();
                            args.$form.find('[rel=guestcidraddress]').show();

                            args.$form.find('[rel=ip6dns1]').show();
                            args.$form.find('[rel=ip6dns2]').show();
                        } else { //Advanced SG-enabled zone
                            args.$form.find('[rel=networkOfferingId]').show(); //will be used to create a guest network during zone creation
                            args.$form.find('[rel=guestcidraddress]').hide();

                            args.$form.find('[rel=ip6dns1]').hide();
                            args.$form.find('[rel=ip6dns2]').hide();
                        }
                    }
                },
                fields: {
                    name: {
                        label: 'label.name',
                        validation: {
                            required: true
                        },
                        desc: 'message.tooltip.zone.name'
                    },
                    ip4dns1: {
                        label: 'IPv4 DNS1',
                        validation: {
                            required: true
                        },
                        desc: 'message.tooltip.dns.1'
                    },
                    ip4dns2: {
                        label: 'IPv4 DNS2',
                        desc: 'message.tooltip.dns.2'
                    },

                    ip6dns1: {
                        label: 'IPv6 DNS1',
                        desc: 'message.tooltip.dns.1'
                    },
                    ip6dns2: {
                        label: 'IPv6 DNS2',
                        desc: 'message.tooltip.dns.2'
                    },

                    internaldns1: {
                        label: 'label.internal.dns.1',
                        validation: {
                            required: true
                        },
                        desc: 'message.tooltip.internal.dns.1'
                    },
                    internaldns2: {
                        label: 'label.internal.dns.2',
                        desc: 'message.tooltip.internal.dns.2'
                    },
                    hypervisor: {
                        label: 'label.hypervisor',
                        validation: {
                            required: true
                        },
                        select: function(args) {
                            $.ajax({
                                url: createURL('listHypervisors'),
                                async: false,
                                data: {
                                    listAll: true
                                },
                                success: function(json) {
                                    var items = json.listhypervisorsresponse.hypervisor;
                                    var array1 = [];

                                    var firstOption = "XenServer";
                                    var nonSupportedHypervisors = {};
                                    if (args.context.zones[0]['network-model'] == "Advanced" && args.context.zones[0]['zone-advanced-sg-enabled'] == "on") {
                                        firstOption = "KVM";
                                        nonSupportedHypervisors["VMware"] = 1;
                                        nonSupportedHypervisors["BareMetal"] = 1;
                                        nonSupportedHypervisors["Hyperv"] = 1;
                                        nonSupportedHypervisors["Ovm"] = 1;
                                        nonSupportedHypervisors["LXC"] = 1;
                                    }

                                    if (items != null) {
                                        for (var i = 0; i < items.length; i++) {
                                            if (items[i].name in nonSupportedHypervisors)
                                                continue;

                                            if (items[i].name == firstOption)
                                                array1.unshift({
                                                    id: items[i].name,
                                                    description: items[i].name
                                                });
                                            else
                                                array1.push({
                                                    id: items[i].name,
                                                    description: items[i].name
                                                });
                                        }
                                    }
                                    args.response.success({
                                        data: array1
                                    });
                                }
                            });
                        }
                    },
                    networkOfferingId: {
                        label: 'label.network.offering',
                        dependsOn: 'hypervisor',
                        select: function(args) {
                            var selectedNetworkOfferingObj = {};
                            var networkOfferingObjs = [];

                            args.$select.unbind("change").bind("change", function() {
                                //reset when different network offering is selected
                                selectedNetworkOfferingHavingSG = false;
                                selectedNetworkOfferingHavingEIP = false;
                                selectedNetworkOfferingHavingELB = false;
                                selectedNetworkOfferingHavingNetscaler = false;

                                var selectedNetworkOfferingId = $(this).val();
                                $(networkOfferingObjs).each(function() {
                                    if (this.id == selectedNetworkOfferingId) {
                                        selectedNetworkOfferingObj = this;
                                        return false; //break $.each() loop
                                    }
                                });

                                if (selectedNetworkOfferingObj.havingNetscaler == true)
                                    selectedNetworkOfferingHavingNetscaler = true;
                                if (selectedNetworkOfferingObj.havingSG == true)
                                    selectedNetworkOfferingHavingSG = true;
                                if (selectedNetworkOfferingObj.havingEIP == true)
                                    selectedNetworkOfferingHavingEIP = true;
                                if (selectedNetworkOfferingObj.havingELB == true)
                                    selectedNetworkOfferingHavingELB = true;
                            });


                            $.ajax({
                                url: createURL("listNetworkOfferings&state=Enabled&guestiptype=Shared"),
                                dataType: "json",
                                async: false,
                                success: function(json) {
                                    networkOfferingObjs = json.listnetworkofferingsresponse.networkoffering;
                                    var availableNetworkOfferingObjs = [];
                                    $(networkOfferingObjs).each(function() {
                                        var thisNetworkOffering = this;
                                        $(this.service).each(function() {
                                            var thisService = this;

                                            $(thisService.provider).each(function() {
                                                if (this.name == "Netscaler") {
                                                    thisNetworkOffering.havingNetscaler = true;
                                                } else if ($.inArray(this.name, baremetalProviders) != -1) {
                                                    selectedBaremetalProviders.push(this.name);
                                                }
                                            });

                                            if (thisService.name == "SecurityGroup") {
                                                thisNetworkOffering.havingSG = true;
                                            } else if (thisService.name == "StaticNat") {
                                                $(thisService.capability).each(function() {
                                                    if (this.name == "ElasticIp" && this.value == "true") {
                                                        thisNetworkOffering.havingEIP = true;
                                                        return false; //break $.each() loop
                                                    }
                                                });
                                            } else if (thisService.name == "Lb") {
                                                $(thisService.capability).each(function() {
                                                    if (this.name == "ElasticLb" && this.value == "true") {
                                                        thisNetworkOffering.havingELB = true;
                                                        return false; //break $.each() loop
                                                    }
                                                });
                                            }
                                        });

                                        if (thisNetworkOffering.havingEIP == true && thisNetworkOffering.havingELB == true) { //EIP ELB
                                            if (args.hypervisor == "VMware" || args.hypervisor == "BareMetal") { //VMware, BareMetal don't support EIP ELB
                                                return true; //move to next item in $.each() loop
                                            }
                                            if (args.context.zones[0]["network-model"] == "Advanced" && args.context.zones[0]["zone-advanced-sg-enabled"] == "on") { // Advanced SG-enabled zone doesn't support EIP ELB
                                                return true; //move to next item in $.each() loop
                                            }
                                        }

                                        if (args.context.zones[0]["network-model"] == "Advanced" && args.context.zones[0]["zone-advanced-sg-enabled"] == "on") { // Advanced SG-enabled zone
                                            if (thisNetworkOffering.havingSG != true) {
                                                return true; //move to next item in $.each() loop
                                            }
                                        }

                                        availableNetworkOfferingObjs.push(thisNetworkOffering);
                                    });

                                    args.response.success({
                                        data: $.map(availableNetworkOfferingObjs, function(offering) {
                                            return {
                                                id: offering.id,
                                                description: offering.name
                                            };
                                        })
                                    });

                                }
                            });
                        }
                    },
                    networkdomain: {
                        label: 'label.network.domain',
                        desc: 'message.tooltip.network.domain'
                    },
                    guestcidraddress: {
                        label: 'label.guest.cidr',
                        defaultValue: '10.1.1.0/24',
                        validation: {
                            required: false
                        }
                    },
                    isdedicated: {                        
                        isBoolean: true,
                        label: 'Dedicated',
                        isChecked: false 
                    },
                    domain: {
                        label: 'label.domain',
                        dependsOn: 'isdedicated',
                        isHidden: true,
                        select: function(args) {
                            $.ajax({
                                url: createURL("listDomains&listAll=true"),
                                data: {
                                    viewAll: true
                                },
                                dataType: "json",
                                async: false,
                                success: function(json) {
                                    domainObjs = json.listdomainsresponse.domain;
                                    args.response.success({
                                        data: $.map(domainObjs, function(domain) {
                                            return {
                                                id: domain.id,
                                                description: domain.path
                                            };
                                        })
                                    });
                                }
                            });
                        }
                    },

                    account: {
                        label: 'Account',
                        isHidden: true,
                        dependsOn: 'isdedicated',
                        //docID:'helpAccountForDedication',
                        validation: {
                            required: false
                        }

                    },

                    localstorageenabled: {
                        label: 'label.local.storage.enabled',
                        isBoolean: true,
                        onChange: function(args) {
                            var $checkbox = args.$checkbox;

                            if ($checkbox.is(':checked')) {
                                cloudStack.dialog.confirm({
                                    message: 'message.zoneWizard.enable.local.storage',
                                    action: function() {
                                        $checkbox.attr('checked', true);
                                    },
                                    cancelAction: function() {
                                        $checkbox.attr('checked', false);
                                    }
                                });

                                return false;
                            }

                            return true;
                        }
                    }
                }
            },

            pod: {
                fields: {
                    name: {
                        label: 'label.pod.name',
                        validation: {
                            required: true
                        },
                        desc: 'message.tooltip.pod.name'
                    },
                    reservedSystemGateway: {
                        label: 'label.reserved.system.gateway',
                        validation: {
                            required: true
                        },
                        desc: 'message.tooltip.reserved.system.gateway'
                    },
                    reservedSystemNetmask: {
                        label: 'label.reserved.system.netmask',
                        validation: {
                            required: true
                        },
                        desc: 'message.tooltip.reserved.system.netmask'
                    },
                    reservedSystemStartIp: {
                        label: 'label.start.reserved.system.IP',
                        validation: {
                            required: true
                        }
                    },
                    reservedSystemEndIp: {
                        label: 'label.end.reserved.system.IP',
                        validation: {
                            required: false
                        }
                    }
                }
            },

            basicPhysicalNetwork: { //"Netscaler" now
                preFilter: function(args) {
                    if (args.data['network-model'] == 'Basic' && (selectedNetworkOfferingHavingELB || selectedNetworkOfferingHavingEIP)) {
                        args.$form.find('[rel=dedicated]').hide();
                    } else {
                        args.$form.find('[rel=dedicated]').show();
                    };
                    cloudStack.preFilter.addLoadBalancerDevice
                },
                fields: {
                    ip: {
                        label: 'label.ip.address'
                    },
                    username: {
                        label: 'label.username'
                    },
                    password: {
                        label: 'label.password',
                        isPassword: true
                    },
                    networkdevicetype: {
                        label: 'label.type',
                        select: function(args) {
                            var items = [];
                            items.push({
                                id: "NetscalerMPXLoadBalancer",
                                description: "NetScaler MPX LoadBalancer"
                            });
                            items.push({
                                id: "NetscalerVPXLoadBalancer",
                                description: "NetScaler VPX LoadBalancer"
                            });
                            items.push({
                                id: "NetscalerSDXLoadBalancer",
                                description: "NetScaler SDX LoadBalancer"
                            });
                            args.response.success({
                                data: items
                            });
                        }
                    },
                    publicinterface: {
                        label: 'label.public.interface'
                    },
                    privateinterface: {
                        label: 'label.private.interface'
                    },
                    gslbprovider: {
                        label: 'GSLB service',
                        isBoolean: true,
                        isChecked: false
                    },
                    gslbproviderpublicip: {
                        label: 'GSLB service Public IP'
                    },
                    gslbproviderprivateip: {
                        label: 'GSLB service Private IP'
                    },
                    numretries: {
                        label: 'label.numretries',
                        defaultValue: '2'
                    },
                    dedicated: {
                        label: 'label.dedicated',
                        isBoolean: true,
                        isChecked: false
                    },
                    capacity: {
                        label: 'label.capacity',
                        validation: {
                            required: false,
                            number: true
                        }
                    }
                }
            },

            guestTraffic: {
                preFilter: function(args) {
                    var $guestTrafficDesc = $('.zone-wizard:visible').find('#add_zone_guest_traffic_desc');
                    if ((args.data['network-model'] == 'Basic') || (args.data['network-model'] == 'Advanced' && args.data["zone-advanced-sg-enabled"] == "on")) {
                        $guestTrafficDesc.find('#for_basic_zone').css('display', 'inline');
                        $guestTrafficDesc.find('#for_advanced_zone').hide();
                    } else { //args.data['network-model'] == 'Advanced' && args.data["zone-advanced-sg-enabled"] !=	"on"
                        $guestTrafficDesc.find('#for_advanced_zone').css('display', 'inline');
                        $guestTrafficDesc.find('#for_basic_zone').hide();
                    }

                    var selectedZoneObj = {
                        networktype: args.data['network-model']
                    };

                    if (selectedZoneObj.networktype == 'Basic') {
                        args.$form.find('[rel="guestGateway"]').show();
                        args.$form.find('[rel="guestNetmask"]').show();
                        args.$form.find('[rel="guestStartIp"]').show();
                        args.$form.find('[rel="guestEndIp"]').show();
                        args.$form.find('[rel="vlanId"]').hide();
                        args.$form.find('[rel="vlanRange"]').hide();
                    } else if (selectedZoneObj.networktype == 'Advanced' && args.data["zone-advanced-sg-enabled"] == "on") {
                        args.$form.find('[rel="guestGateway"]').show();
                        args.$form.find('[rel="guestNetmask"]').show();
                        args.$form.find('[rel="guestStartIp"]').show();
                        args.$form.find('[rel="guestEndIp"]').show();
                        args.$form.find('[rel="vlanId"]').show();
                        args.$form.find('[rel="vlanRange"]').hide();
                    } else if (selectedZoneObj.networktype == 'Advanced' && args.data["zone-advanced-sg-enabled"] != "on") { //this conditional statement is useless because the div will be replaced with other div(multiple tabs in Advanced zone without SG) later
                        args.$form.find('[rel="guestGateway"]').hide();
                        args.$form.find('[rel="guestNetmask"]').hide();
                        args.$form.find('[rel="guestStartIp"]').hide();
                        args.$form.find('[rel="guestEndIp"]').hide();
                        args.$form.find('[rel="vlanId"]').hide();
                        args.$form.find('[rel="vlanRange"]').show();
                    }
                },

                fields: {
                    guestGateway: {
                        label: 'label.guest.gateway'
                    }, //Basic, Advanced with SG
                    guestNetmask: {
                        label: 'label.guest.netmask'
                    }, //Basic, Advanced with SG
                    guestStartIp: {
                        label: 'label.guest.start.ip'
                    }, //Basic, Advanced with SG
                    guestEndIp: {
                        label: 'label.guest.end.ip'
                    }, //Basic, Advanced with SG
                    vlanId: {
                        label: 'VLAN ID'
                    }, //Advanced with SG

                    vlanRange: { //in multiple tabs (tabs is as many as Guest Traffic types in multiple physical networks in Advanced Zone without SG)
                        label: 'label.vlan.range',
                        range: ['vlanRangeStart', 'vlanRangeEnd'],
                        validation: {
                            required: false,
                            digits: true
                        } //Bug 13517 - AddZone wizard->Configure guest traffic: Vlan Range is optional
                    }
                }
            },
            cluster: {
                fields: {
                    hypervisor: {
                        label: 'label.hypervisor',
                        select: function(args) {
                            // Disable select -- selection is made on zone setup step
                            args.$select.attr('disabled', 'disabled');

                            $.ajax({
                                url: createURL("listHypervisors"),
                                dataType: "json",
                                async: false,
                                success: function(json) {
                                    var hypervisors = json.listhypervisorsresponse.hypervisor;
                                    var items = [];
                                    $(hypervisors).each(function() {
                                        items.push({
                                            id: this.name,
                                            description: this.name
                                        })
                                    });
                                    args.response.success({
                                        data: items
                                    });
                                    args.$select.val(args.context.zones[0].hypervisor);
                                    args.$select.change();
                                }
                            });

                            var vSwitchEnabled = false;
                            var dvSwitchEnabled = false;
                            // Check whether vSwitch capability is enabled
                            $.ajax({
                                url: createURL('listConfigurations'),
                                data: {
                                    name: 'vmware.use.nexus.vswitch'
                                },
                                async: false,
                                success: function(json) {
                                    if (json.listconfigurationsresponse.configuration[0].value == 'true') {
                                        vSwitchEnabled = true;
                                    }
                                }
                            });

                            //Check whether dvSwitch is enabled or not
                            $.ajax({
                                url: createURL('listConfigurations'),
                                data: {
                                    name: 'vmware.use.dvswitch'
                                },
                                async: false,
                                success: function(json) {
                                    if (json.listconfigurationsresponse.configuration[0].value == 'true') {
                                        dvSwitchEnabled = true;
                                    }
                                }
                            });

                            args.$select.bind("change", function(event) {
                                var $form = $(this).closest('form');
                                var $vsmFields = $form.find('[rel]').filter(function() {
                                    var vsmFields = [
                                        'vsmipaddress',
                                        'vsmusername',
                                        'vsmpassword'
                                    ];

                                    return $.inArray($(this).attr('rel'), vsmFields) > -1;
                                });

                                if ($(this).val() == "VMware") {
                                    //$('li[input_sub_group="external"]', $dialogAddCluster).show();
                                    if (dvSwitchEnabled) {
                                        /*   
										$fields.filter('[rel=vSwitchPublicType]').css('display', 'inline-block');
										$form.find('.form-item[rel=vSwitchGuestType]').css('display', 'inline-block');

										$form.find('.form-item[rel=vSwitchPublicName]').css('display','inline-block');
										$form.find('.form-item[rel=vSwitchGuestName]').css('display','inline-block');

										$form.find('.form-item[rel=overridepublictraffic]').find('input[type=checkbox]').css('display','inline-block');
										$form.find('.form-item[rel=overrideguesttraffic]').find('input[type=checkbox]').css('display','inline-block');
						                */

                                        $form.find('.form-item[rel=overridepublictraffic]').css('display', 'inline-block');
                                        $form.find('.form-item[rel=overridepublictraffic]').find('input[type=checkbox]').removeAttr('checked');

                                        $form.find('.form-item[rel=overrideguesttraffic]').css('display', 'inline-block');
                                        $form.find('.form-item[rel=overrideguesttraffic]').find('input[type=checkbox]').removeAttr('checked');



                                    } else {
                                        /*    
										$form.find('.form-item[rel=vSwitchPublicType]').css('display', 'none');
									    $form.find('.form-item[rel=vSwitchGuestType]').css('display', 'none');
									    $form.find('.form-item[rel=vSwitchPublicName]').css('display','none');
									    $form.find('.form-item[rel=vSwitchGuestName]').css('display','none');
						                */
                                        $form.find('.form-item[rel=overridepublictraffic]').css('display', 'none');
                                        $form.find('.form-item[rel=overrideguesttraffic]').css('display', 'none');

                                    }

                                    $form.find('[rel=vCenterHost]').css('display', 'block');
                                    $form.find('[rel=vCenterUsername]').css('display', 'block');
                                    $form.find('[rel=vCenterPassword]').css('display', 'block');
                                    $form.find('[rel=vCenterDatacenter]').css('display', 'block');

                                    if (vSwitchEnabled) {
                                        $vsmFields.css('display', 'block');
                                    } else {
                                        $vsmFields.css('display', 'none');
                                    }
                                    //$("#cluster_name_label", $dialogAddCluster).text("vCenter Cluster:");
                                } else {
                                    //$('li[input_group="vmware"]', $dialogAddCluster).hide();

                                    $form.find('.form-item[rel=overridepublictraffic]').css('display', 'none');
                                    $form.find('.form-item[rel=overrideguesttraffic]').css('display', 'none');
                                    //$form.find('.form-item[rel=vSwitchPublicType]').css('display', 'none');
                                    //$form.find('.form-item[rel=vSwitchGuestType]').css('display', 'none');
                                    //$form.find('.form-item[rel=vSwitchPublicName]').css('display', 'none');
                                    //$form.find('.form-item[rel=vSwitchGuestName]').css('display', 'none');


                                    $form.find('[rel=vCenterHost]').css('display', 'none');
                                    $form.find('[rel=vCenterUsername]').css('display', 'none');
                                    $form.find('[rel=vCenterPassword]').css('display', 'none');
                                    $form.find('[rel=vCenterDatacenter]').css('display', 'none');

                                    //$("#cluster_name_label", $dialogAddCluster).text("Cluster:");
                                }
                            });
                        }
                    },
                    name: {
                        label: 'label.cluster.name',
                        validation: {
                            required: true
                        }
                    },

                    //hypervisor==VMWare begins here

                    vCenterHost: {
                        label: 'label.vcenter.host',
                        validation: {
                            required: true
                        } //required, for firing addVmwareDc API
                    },
                    vCenterUsername: {
                        label: 'label.vcenter.username',
                        validation: {
                            required: true
                        } //required, for firing addVmwareDc API
                    },
                    vCenterPassword: {
                        label: 'label.vcenter.password',
                        isPassword: true,
                        validation: {
                            required: true
                        } //required, for firing addVmwareDc API
                    },
                    vCenterDatacenter: {
                        label: 'label.vcenter.datacenter',
                        validation: {
                            required: true
                        } //required, for firing addVmwareDc API
                    },

                    overridepublictraffic: {
                        label: 'Override Public-Traffic',
                        isBoolean: true,
                        isHidden: true

                    },

                    /*
                    vSwitchPublicType: {
                        label: 'Public Traffic vSwitch Type',
                        select: function(args) {
                            var items = []
                            var vSwitchEnabled = false;
                            $.ajax({
                                url: createURL('listConfigurations'),
                                data: {
                                    name: 'vmware.use.nexus.vswitch'
                                },
                                async: false,
                                success: function(json) {
                                    if (json.listconfigurationsresponse.configuration[0].value == 'true') {
                                        vSwitchEnabled = true;
                                    }
                                }
                            });

                            if (vSwitchEnabled) {

                                items.push({
                                    id: "nexusdvs",
                                    description: "Cisco Nexus 1000v Distributed Virtual Switch"
                                });

                                items.push({
                                    id: "vmwaresvs",
                                    description: "VMware vNetwork Standard Virtual Switch"
                                });
                                items.push({
                                    id: "vmwaredvs",
                                    description: "VMware vNetwork Distributed Virtual Switch"
                                });




                            }

                            //  items.push({id: " ", description: " "});
                            else {
                                items.push({
                                    id: "vmwaredvs",
                                    description: "VMware vNetwork Distributed Virtual Switch"
                                });
                                items.push({
                                    id: "vmwaresvs",
                                    description: "VMware vNetwork Standard Virtual Switch"
                                });
                                items.push({
                                    id: "nexusdvs",
                                    description: "Cisco Nexus 1000v Distributed Virtual Switch"
                                });

                            }
                            args.response.success({
                                data: items
                            });
                        },
                        isHidden: true,
                        dependsOn: 'overridepublictraffic'
                    },
                    */
					
					/*
                    vSwitchPublicName: {
                        label: 'Public Traffic vSwitch Name',
                        dependsOn: 'overridepublictraffic',
                        isHidden: true
                    },
                    */
					
                    overrideguesttraffic: {
                        label: 'Override Guest-Traffic',
                        isBoolean: true,
                        isHidden: true

                    },

                    /*
                    vSwitchGuestType: {
                        label: 'Guest Traffic vSwitch Type',
                        select: function(args) {
                            var items = []
                            //items.push({ id:" ", description:" "});

                            var vSwitchEnabled = false;
                            var items = []
                            $.ajax({
                                url: createURL('listConfigurations'),
                                data: {
                                    name: 'vmware.use.nexus.vswitch'
                                },
                                async: false,
                                success: function(json) {
                                    if (json.listconfigurationsresponse.configuration[0].value == 'true') {
                                        vSwitchEnabled = true;
                                    }
                                }
                            });

                            if (vSwitchEnabled) {

                                items.push({
                                    id: "nexusdvs",
                                    description: "Cisco Nexus 1000v Distributed Virtual Switch"
                                });
                                items.push({
                                    id: "vmwaresvs",
                                    description: "VMware vNetwork Standard Virtual Switch"
                                });
                                items.push({
                                    id: "vmwaredvs",
                                    description: "VMware vNetwork Distributed Virtual Switch"
                                });




                            } else {

                                items.push({
                                    id: "vmwaredvs",
                                    description: "VMware vNetwork Distributed Virtual Switch"
                                });
                                items.push({
                                    id: "vmwaresvs",
                                    description: "VMware vNetwork Standard Virtual Switch"
                                });
                                items.push({
                                    id: "nexusdvs",
                                    description: "Cisco Nexus 1000v Distributed Virtual Switch"
                                });

                            }
                            args.response.success({
                                data: items
                            });

                        },
                        isHidden: true,
                        dependsOn: 'overrideguesttraffic'

                    },
                    */
					
					/*
                    vSwitchGuestName: {
                        label: 'Guest Traffic vSwitch Name',
                        dependsOn: 'overrideguesttraffic',
                        isHidden: true
                    },
					*/

                    //Cisco Nexus Vswitch
                    vsmipaddress: {
                        label: 'Nexus 1000v IP Address',
                        validation: {
                            required: false
                        },
                        isHidden: true
                    },
                    vsmusername: {
                        label: 'Nexus 1000v Username',
                        validation: {
                            required: false
                        },
                        isHidden: true
                    },
                    vsmpassword: {
                        label: 'Nexus 1000v Password',
                        validation: {
                            required: false
                        },
                        isPassword: true,
                        isHidden: true
                    }
                    //hypervisor==VMWare ends here
                }
            },
            host: {
                preFilter: function(args) {
                    var selectedClusterObj = {
                        hypervisortype: args.data.hypervisor
                    };

                    var $form = args.$form;

                    if (selectedClusterObj.hypervisortype == "VMware") {
                        //$('li[input_group="general"]', $dialogAddHost).hide();
                        $form.find('[rel=hostname]').hide();
                        $form.find('[rel=username]').hide();
                        $form.find('[rel=password]').hide();

                        //$('li[input_group="vmware"]', $dialogAddHost).show();
                        $form.find('[rel=vcenterHost]').css('display', 'block');

                        //$('li[input_group="baremetal"]', $dialogAddHost).hide();
                        $form.find('[rel=baremetalCpuCores]').hide();
                        $form.find('[rel=baremetalCpu]').hide();
                        $form.find('[rel=baremetalMemory]').hide();
                        $form.find('[rel=baremetalMAC]').hide();

                        //$('li[input_group="Ovm"]', $dialogAddHost).hide();
                        $form.find('[rel=agentUsername]').hide();
                        $form.find('[rel=agentPassword]').hide();
                    } else if (selectedClusterObj.hypervisortype == "BareMetal") {
                        //$('li[input_group="general"]', $dialogAddHost).show();
                        $form.find('[rel=hostname]').css('display', 'block');
                        $form.find('[rel=username]').css('display', 'block');
                        $form.find('[rel=password]').css('display', 'block');

                        //$('li[input_group="baremetal"]', $dialogAddHost).show();
                        $form.find('[rel=baremetalCpuCores]').css('display', 'block');
                        $form.find('[rel=baremetalCpu]').css('display', 'block');
                        $form.find('[rel=baremetalMemory]').css('display', 'block');
                        $form.find('[rel=baremetalMAC]').css('display', 'block');

                        //$('li[input_group="vmware"]', $dialogAddHost).hide();
                        $form.find('[rel=vcenterHost]').hide();

                        //$('li[input_group="Ovm"]', $dialogAddHost).hide();
                        $form.find('[rel=agentUsername]').hide();
                        $form.find('[rel=agentPassword]').hide();
                    } else if (selectedClusterObj.hypervisortype == "Ovm") {
                        //$('li[input_group="general"]', $dialogAddHost).show();
                        $form.find('[rel=hostname]').css('display', 'block');
                        $form.find('[rel=username]').css('display', 'block');
                        $form.find('[rel=password]').css('display', 'block');

                        //$('li[input_group="vmware"]', $dialogAddHost).hide();
                        $form.find('[rel=vcenterHost]').hide();

                        //$('li[input_group="baremetal"]', $dialogAddHost).hide();
                        $form.find('[rel=baremetalCpuCores]').hide();
                        $form.find('[rel=baremetalCpu]').hide();
                        $form.find('[rel=baremetalMemory]').hide();
                        $form.find('[rel=baremetalMAC]').hide();

                        //$('li[input_group="Ovm"]', $dialogAddHost).show();
                        $form.find('[rel=agentUsername]').css('display', 'block');
                        $form.find('[rel=agentUsername]').find('input').val("oracle");
                        $form.find('[rel=agentPassword]').css('display', 'block');
                    } else {
                        //$('li[input_group="general"]', $dialogAddHost).show();
                        $form.find('[rel=hostname]').css('display', 'block');
                        $form.find('[rel=username]').css('display', 'block');
                        $form.find('[rel=password]').css('display', 'block');

                        //$('li[input_group="vmware"]', $dialogAddHost).hide();
                        $form.find('[rel=vcenterHost]').hide();

                        //$('li[input_group="baremetal"]', $dialogAddHost).hide();
                        $form.find('[rel=baremetalCpuCores]').hide();
                        $form.find('[rel=baremetalCpu]').hide();
                        $form.find('[rel=baremetalMemory]').hide();
                        $form.find('[rel=baremetalMAC]').hide();

                        //$('li[input_group="Ovm"]', $dialogAddHost).hide();
                        $form.find('[rel=agentUsername]').hide();
                        $form.find('[rel=agentPassword]').hide();
                    }
                },
                fields: {
                    hostname: {
                        label: 'label.host.name',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },

                    username: {
                        label: 'label.username',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },

                    password: {
                        label: 'label.password',
                        validation: {
                            required: true
                        },
                        isHidden: true,
                        isPassword: true
                    },
                    //input_group="general" ends here

                    //input_group="VMWare" starts here
                    vcenterHost: {
                        label: 'label.esx.host',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },
                    //input_group="VMWare" ends here

                    //input_group="BareMetal" starts here
                    baremetalCpuCores: {
                        label: 'label.num.cpu.cores',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },
                    baremetalCpu: {
                        label: 'label.cpu.mhz',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },
                    baremetalMemory: {
                        label: 'label.memory.mb',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },
                    baremetalMAC: {
                        label: 'label.host.MAC',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },
                    //input_group="BareMetal" ends here

                    //input_group="OVM" starts here
                    agentUsername: {
                        label: 'label.agent.username',
                        validation: {
                            required: false
                        },
                        isHidden: true
                    },
                    agentPassword: {
                        label: 'label.agent.password',
                        validation: {
                            required: true
                        },
                        isHidden: true,
                        isPassword: true
                    },
                    //input_group="OVM" ends here

                    //always appear (begin)
                    hosttags: {
                        label: 'label.host.tags',
                        validation: {
                            required: false
                        }
                    }
                    //always appear (end)
                }
            },
            primaryStorage: {
                preFilter: function(args) {},

                fields: {
                    name: {
                        label: 'label.name',
                        validation: {
                            required: true
                        }
                    },

                    scope: {
                        label: 'label.scope',
                        select: function(args) {

                            var selectedHypervisorObj = {
                                hypervisortype: $.isArray(args.context.zones[0].hypervisor) ?
                                // We want the cluster's hypervisor type
                                args.context.zones[0].hypervisor[1] : args.context.zones[0].hypervisor
                            };

                            if (selectedHypervisorObj == null) {
                                return;
                            }

                            //zone-wide-primary-storage is supported only for KVM and VMWare
                            if (selectedHypervisorObj.hypervisortype == "KVM" || selectedHypervisorObj.hypervisortype == "VMware") {
                                var scope = [];
                                scope.push({
                                    id: 'zone',
                                    description: _l('label.zone.wide')
                                });
                                scope.push({
                                    id: 'cluster',
                                    description: _l('label.cluster')
                                });
                                args.response.success({
                                    data: scope
                                });
                            } else {
                                var scope = [];
                                scope.push({
                                    id: 'cluster',
                                    description: _l('label.cluster')
                                });
                                args.response.success({
                                    data: scope
                                });
                            }
                        }
                    },

                    protocol: {
                        label: 'label.protocol',
                        validation: {
                            required: true
                        },
                        select: function(args) {
                            var selectedClusterObj = {
                                hypervisortype: $.isArray(args.context.zones[0].hypervisor) ?
                                // We want the cluster's hypervisor type
                                args.context.zones[0].hypervisor[1] : args.context.zones[0].hypervisor
                            };

                            if (selectedClusterObj == null) {
                                return;
                            }

                            if (selectedClusterObj.hypervisortype == "KVM") {
                                var items = [];
                                items.push({
                                    id: "nfs",
                                    description: "nfs"
                                });
                                items.push({
                                    id: "SharedMountPoint",
                                    description: "SharedMountPoint"
                                });
                                items.push({
                                    id: "clvm",
                                    description: "CLVM"
                                });
                                args.response.success({
                                    data: items
                                });
                            } else if (selectedClusterObj.hypervisortype == "XenServer") {
                                var items = [];
                                items.push({
                                    id: "nfs",
                                    description: "nfs"
                                });
                                items.push({
                                    id: "PreSetup",
                                    description: "PreSetup"
                                });
                                items.push({
                                    id: "iscsi",
                                    description: "iscsi"
                                });
                                args.response.success({
                                    data: items
                                });
                            } else if (selectedClusterObj.hypervisortype == "VMware") {
                                var items = [];
                                items.push({
                                    id: "nfs",
                                    description: "nfs"
                                });
                                items.push({
                                    id: "vmfs",
                                    description: "vmfs"
                                });
                                args.response.success({
                                    data: items
                                });
                            } else if (selectedClusterObj.hypervisortype == "Ovm") {
                                var items = [];
                                items.push({
                                    id: "nfs",
                                    description: "nfs"
                                });
                                items.push({
                                    id: "ocfs2",
                                    description: "ocfs2"
                                });
                                args.response.success({
                                    data: items
                                });
                            } else if (selectedClusterObj.hypervisortype == "LXC") {
                                var items = [];
                                items.push({
                                    id: "nfs",
                                    description: "nfs"
                                });
                                items.push({
                                    id: "SharedMountPoint",
                                    description: "SharedMountPoint"
                                });
                                args.response.success({
                                    data: items
                                });
                            } else {
                                args.response.success({
                                    data: []
                                });
                            }

                            args.$select.change(function() {
                                var $form = $(this).closest('form');

                                var protocol = $(this).val();

                                $form.find('[rel=path]').find(".name").find("label").html('<span class=\"field-required\">*</span>Path:');

                                if (protocol == null)
                                    return;

                                if (protocol == "nfs") {
                                    //$("#add_pool_server_container", $dialogAddPool).show();
                                    $form.find('[rel=server]').css('display', 'block');
                                    //$dialogAddPool.find("#add_pool_nfs_server").val("");
                                    $form.find('[rel=server]').find(".value").find("input").val("");

                                    //$('li[input_group="nfs"]', $dialogAddPool).show();
                                    $form.find('[rel=path]').css('display', 'block');
                                    //$dialogAddPool.find("#add_pool_path_container").find("label").text(g_dictionary["label.path"]+":");
                                    //$form.find('[rel=path]').find(".name").find("label").text("Path:");

                                    //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                                    $form.find('[rel=iqn]').hide();
                                    $form.find('[rel=lun]').hide();

                                    //$('li[input_group="clvm"]', $dialogAddPool).hide();
                                    $form.find('[rel=volumegroup]').hide();

                                    //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                                    $form.find('[rel=vCenterDataCenter]').hide();
                                    $form.find('[rel=vCenterDataStore]').hide();
                                } else if (protocol == "ocfs2") { //ocfs2 is the same as nfs, except no server field.
                                    //$dialogAddPool.find("#add_pool_server_container").hide();
                                    $form.find('[rel=server]').hide();
                                    //$dialogAddPool.find("#add_pool_nfs_server").val("");
                                    $form.find('[rel=server]').find(".value").find("input").val("");

                                    //$('li[input_group="nfs"]', $dialogAddPool).show();
                                    $form.find('[rel=path]').css('display', 'block');
                                    //$dialogAddPool.find("#add_pool_path_container").find("label").text(g_dictionary["label.path"]+":");
                                    //$form.find('[rel=path]').find(".name").find("label").text("Path:");

                                    //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                                    $form.find('[rel=iqn]').hide();
                                    $form.find('[rel=lun]').hide();

                                    //$('li[input_group="clvm"]', $dialogAddPool).hide();
                                    $form.find('[rel=volumegroup]').hide();

                                    //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                                    $form.find('[rel=vCenterDataCenter]').hide();
                                    $form.find('[rel=vCenterDataStore]').hide();
                                } else if (protocol == "PreSetup") {
                                    //$dialogAddPool.find("#add_pool_server_container").hide();
                                    $form.find('[rel=server]').hide();
                                    //$dialogAddPool.find("#add_pool_nfs_server").val("localhost");
                                    $form.find('[rel=server]').find(".value").find("input").val("localhost");

                                    //$('li[input_group="nfs"]', $dialogAddPool).show();
                                    $form.find('[rel=path]').css('display', 'block');
                                    //$dialogAddPool.find("#add_pool_path_container").find("label").text(g_dictionary["label.SR.name"]+":");
                                    $form.find('[rel=path]').find(".name").find("label").html("<span class=\"field-required\">*</span>SR Name-Label:");

                                    //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                                    $form.find('[rel=iqn]').hide();
                                    $form.find('[rel=lun]').hide();

                                    //$('li[input_group="clvm"]', $dialogAddPool).hide();
                                    $form.find('[rel=volumegroup]').hide();

                                    //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                                    $form.find('[rel=vCenterDataCenter]').hide();
                                    $form.find('[rel=vCenterDataStore]').hide();
                                } else if (protocol == "iscsi") {
                                    //$dialogAddPool.find("#add_pool_server_container").show();
                                    $form.find('[rel=server]').css('display', 'block');
                                    //$dialogAddPool.find("#add_pool_nfs_server").val("");
                                    $form.find('[rel=server]').find(".value").find("input").val("");

                                    //$('li[input_group="nfs"]', $dialogAddPool).hide();
                                    $form.find('[rel=path]').hide();

                                    //$('li[input_group="iscsi"]', $dialogAddPool).show();
                                    $form.find('[rel=iqn]').css('display', 'block');
                                    $form.find('[rel=lun]').css('display', 'block');

                                    //$('li[input_group="clvm"]', $dialogAddPool).hide();
                                    $form.find('[rel=volumegroup]').hide();

                                    //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                                    $form.find('[rel=vCenterDataCenter]').hide();
                                    $form.find('[rel=vCenterDataStore]').hide();
                                } else if ($(this).val() == "clvm") {
                                    //$("#add_pool_server_container", $dialogAddPool).hide();
                                    $form.find('[rel=server]').hide();
                                    //$dialogAddPool.find("#add_pool_nfs_server").val("localhost");
                                    $form.find('[rel=server]').find(".value").find("input").val("localhost");

                                    //$('li[input_group="nfs"]', $dialogAddPool).hide();
                                    $form.find('[rel=path]').hide();

                                    //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                                    $form.find('[rel=iqn]').hide();
                                    $form.find('[rel=lun]').hide();

                                    //$('li[input_group="clvm"]', $dialogAddPool).show();
                                    $form.find('[rel=volumegroup]').css('display', 'inline-block');

                                    //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                                    $form.find('[rel=vCenterDataCenter]').hide();
                                    $form.find('[rel=vCenterDataStore]').hide();
                                } else if (protocol == "vmfs") {
                                    //$dialogAddPool.find("#add_pool_server_container").show();
                                    $form.find('[rel=server]').css('display', 'block');
                                    //$dialogAddPool.find("#add_pool_nfs_server").val("");
                                    $form.find('[rel=server]').find(".value").find("input").val("");

                                    //$('li[input_group="nfs"]', $dialogAddPool).hide();
                                    $form.find('[rel=path]').hide();

                                    //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                                    $form.find('[rel=iqn]').hide();
                                    $form.find('[rel=lun]').hide();

                                    //$('li[input_group="clvm"]', $dialogAddPool).hide();
                                    $form.find('[rel=volumegroup]').hide();

                                    //$('li[input_group="vmfs"]', $dialogAddPool).show();
                                    $form.find('[rel=vCenterDataCenter]').css('display', 'block');
                                    $form.find('[rel=vCenterDataStore]').css('display', 'block');
                                } else if (protocol == "SharedMountPoint") { //"SharedMountPoint" show the same fields as "nfs" does.
                                    //$dialogAddPool.find("#add_pool_server_container").hide();
                                    $form.find('[rel=server]').hide();
                                    //$dialogAddPool.find("#add_pool_nfs_server").val("localhost");
                                    $form.find('[rel=server]').find(".value").find("input").val("localhost");

                                    //$('li[input_group="nfs"]', $dialogAddPool).show();
                                    $form.find('[rel=path]').css('display', 'block');
                                    //$form.find('[rel=path]').find(".name").find("label").text("Path:");

                                    //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                                    $form.find('[rel=iqn]').hide();
                                    $form.find('[rel=lun]').hide();

                                    //$('li[input_group="clvm"]', $dialogAddPool).hide();
                                    $form.find('[rel=volumegroup]').hide();

                                    //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                                    $form.find('[rel=vCenterDataCenter]').hide();
                                    $form.find('[rel=vCenterDataStore]').hide();
                                } else {
                                    //$dialogAddPool.find("#add_pool_server_container").show();
                                    $form.find('[rel=server]').css('display', 'block');
                                    //$dialogAddPool.find("#add_pool_nfs_server").val("");
                                    $form.find('[rel=server]').find(".value").find("input").val("");

                                    //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                                    $form.find('[rel=iqn]').hide();
                                    $form.find('[rel=lun]').hide();

                                    //$('li[input_group="clvm"]', $dialogAddPool).hide();
                                    $form.find('[rel=volumegroup]').hide();

                                    //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                                    $form.find('[rel=vCenterDataCenter]').hide();
                                    $form.find('[rel=vCenterDataStore]').hide();
                                }
                            });

                            args.$select.trigger("change");
                        }
                    },
                    server: {
                        label: 'label.server',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },

                    //nfs
                    path: {
                        label: 'label.path',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },

                    //iscsi
                    iqn: {
                        label: 'label.target.iqn',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },
                    lun: {
                        label: 'label.LUN.number',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },

                    //clvm
                    volumegroup: {
                        label: 'label.volgroup',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },

                    //vmfs
                    vCenterDataCenter: {
                        label: 'label.vcenter.datacenter',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },
                    vCenterDataStore: {
                        label: 'label.vcenter.datastore',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },

                    //always appear (begin)
                    storageTags: {
                        label: 'label.storage.tags',
                        validation: {
                            required: false
                        }
                    }
                    //always appear (end)
                }
            },
            secondaryStorage: {
                fields: {                    
                    provider: {
                        label: 'Provider',
                        select: function(args) {
                        	var storageproviders = [];      
                        	$.ajax({
                        		url: createURL('listImageStores'),
                        		data: {
                        			provider: 'S3'
                        		},
                        		async: true,
                        		success: function(json) {                        			
                        			var s3stores = json.listimagestoresresponse.imagestore;
                        			if(s3stores != null && s3stores.length > 0) {                        				
                        				storageproviders.push({ id: 'S3', description: 'S3'});
                        			} else {
                        				$.ajax({
                                            url: createURL('listStorageProviders'),
                                            data: {
                                                type: 'image'
                                            },
                                            async: false,
                                            success: function(json) {
                                                var objs = json.liststorageprovidersresponse.dataStoreProvider;
                                                storageproviders.push({ id: '', description: ''}); 
                                                if (objs != null) {
                                                    for (var i = 0; i < objs.length; i++) {    
                                                    	storageproviders.push({
                                                            id: objs[i].name,
                                                            description: objs[i].name
                                                        });
                                                    }
                                                }                                    
                                            }
                                        });
                        			}                        		                    			
                                    args.response.success({
                                        data: storageproviders
                                    });                                                      
                                    
                                    args.$select.change(function() {                                    
                                        var $form = $(this).closest('form');
                                        var $fields = $form.find('.field');

                                        if ($(this).val() == "") {
                                        	$fields.filter('[rel=name]').hide();
                                        	
                                            //NFS
                                            $fields.filter('[rel=zoneid]').hide();
                                            $fields.filter('[rel=nfsServer]').hide();
                                            $fields.filter('[rel=path]').hide();

                                            //S3
                                            $fields.filter('[rel=accesskey]').hide();
                                            $fields.filter('[rel=secretkey]').hide();
                                            $fields.filter('[rel=bucket]').hide();
                                            $fields.filter('[rel=endpoint]').hide();
                                            $fields.filter('[rel=usehttps]').hide();
                                            $fields.filter('[rel=connectiontimeout]').hide();
                                            $fields.filter('[rel=maxerrorretry]').hide();
                                            $fields.filter('[rel=sockettimeout]').hide();

                                            $fields.filter('[rel=createNfsCache]').hide();
                                            $fields.filter('[rel=createNfsCache]').find('input').removeAttr('checked');
                                            $fields.filter('[rel=nfsCacheNfsServer]').hide();
                                            $fields.filter('[rel=nfsCachePath]').hide();

                                            //Swift
                                            $fields.filter('[rel=url]').hide();
                                            $fields.filter('[rel=account]').hide();
                                            $fields.filter('[rel=username]').hide();
                                            $fields.filter('[rel=key]').hide();
                                        } else if ($(this).val() == "NFS") {
                                        	$fields.filter('[rel=name]').css('display', 'inline-block');
                                        	
                                        	//NFS
                                            $fields.filter('[rel=zoneid]').css('display', 'inline-block');
                                            $fields.filter('[rel=nfsServer]').css('display', 'inline-block');
                                            $fields.filter('[rel=path]').css('display', 'inline-block');

                                            //S3
                                            $fields.filter('[rel=accesskey]').hide();
                                            $fields.filter('[rel=secretkey]').hide();
                                            $fields.filter('[rel=bucket]').hide();
                                            $fields.filter('[rel=endpoint]').hide();
                                            $fields.filter('[rel=usehttps]').hide();
                                            $fields.filter('[rel=connectiontimeout]').hide();
                                            $fields.filter('[rel=maxerrorretry]').hide();
                                            $fields.filter('[rel=sockettimeout]').hide();

                                            $fields.filter('[rel=createNfsCache]').hide();
                                            $fields.filter('[rel=createNfsCache]').find('input').removeAttr('checked');
                                            $fields.filter('[rel=nfsCacheNfsServer]').hide();
                                            $fields.filter('[rel=nfsCachePath]').hide();

                                            //Swift
                                            $fields.filter('[rel=url]').hide();
                                            $fields.filter('[rel=account]').hide();
                                            $fields.filter('[rel=username]').hide();
                                            $fields.filter('[rel=key]').hide();
                                        } else if ($(this).val() == "S3") {
                                        	$fields.filter('[rel=name]').css('display', 'inline-block');
                                        	                                        	
                                        	if(s3stores != null && s3stores.length > 0) {
                                        		$fields.filter('[rel=name]').find('input').val(s3stores[0].name);
                                        		$fields.filter('[rel=name]').find('input').attr("disabled", "disabled");
                                        	} else {
                                        		//$fields.filter('[rel=name]').find('input').val("");
                                        		$fields.filter('[rel=name]').find('input').removeAttr("disabled");
                                        	}                                        	
                                        	
                                        	//NFS
                                            $fields.filter('[rel=zoneid]').hide();
                                            $fields.filter('[rel=nfsServer]').hide();
                                            $fields.filter('[rel=path]').hide();

                                            //S3
                                            if(s3stores != null && s3stores.length > 0) {
                                            	 $fields.filter('[rel=accesskey]').hide();
                                                 $fields.filter('[rel=secretkey]').hide();
                                                 $fields.filter('[rel=bucket]').hide();
                                                 $fields.filter('[rel=endpoint]').hide();
                                                 $fields.filter('[rel=usehttps]').hide();
                                                 $fields.filter('[rel=connectiontimeout]').hide();
                                                 $fields.filter('[rel=maxerrorretry]').hide();
                                                 $fields.filter('[rel=sockettimeout]').hide();   
                                            } else {
                                            	 $fields.filter('[rel=accesskey]').css('display', 'inline-block');
                                                 $fields.filter('[rel=secretkey]').css('display', 'inline-block');
                                                 $fields.filter('[rel=bucket]').css('display', 'inline-block');
                                                 $fields.filter('[rel=endpoint]').css('display', 'inline-block');
                                                 $fields.filter('[rel=usehttps]').css('display', 'inline-block');
                                                 $fields.filter('[rel=connectiontimeout]').css('display', 'inline-block');
                                                 $fields.filter('[rel=maxerrorretry]').css('display', 'inline-block');
                                                 $fields.filter('[rel=sockettimeout]').css('display', 'inline-block');
                                            }      
                                            $fields.filter('[rel=createNfsCache]').find('input').attr('checked', 'checked');
                                            $fields.filter('[rel=createNfsCache]').find('input').attr("disabled", "disabled");  //Create NFS staging is required for S3 at this moment. So, disallow user to uncheck "Create NFS Secondary Staging" checkbox
                                            $fields.filter('[rel=createNfsCache]').css('display', 'inline-block');
                                            $fields.filter('[rel=nfsCacheNfsServer]').css('display', 'inline-block');
                                            $fields.filter('[rel=nfsCachePath]').css('display', 'inline-block');
                                            
                                            //Swift
                                            $fields.filter('[rel=url]').hide();
                                            $fields.filter('[rel=account]').hide();
                                            $fields.filter('[rel=username]').hide();
                                            $fields.filter('[rel=key]').hide();
                                        } else if ($(this).val() == "Swift") {
                                        	$fields.filter('[rel=name]').css('display', 'inline-block');
                                        	
                                        	//NFS
                                            $fields.filter('[rel=zoneid]').hide();
                                            $fields.filter('[rel=nfsServer]').hide();
                                            $fields.filter('[rel=path]').hide();

                                            //S3
                                            $fields.filter('[rel=accesskey]').hide();
                                            $fields.filter('[rel=secretkey]').hide();
                                            $fields.filter('[rel=bucket]').hide();
                                            $fields.filter('[rel=endpoint]').hide();
                                            $fields.filter('[rel=usehttps]').hide();
                                            $fields.filter('[rel=connectiontimeout]').hide();
                                            $fields.filter('[rel=maxerrorretry]').hide();
                                            $fields.filter('[rel=sockettimeout]').hide();

                                            $fields.filter('[rel=createNfsCache]').hide();
                                            $fields.filter('[rel=createNfsCache]').find('input').removeAttr('checked');
                                            $fields.filter('[rel=nfsCacheNfsServer]').hide();
                                            $fields.filter('[rel=nfsCachePath]').hide();

                                            //Swift
                                            $fields.filter('[rel=url]').css('display', 'inline-block');
                                            $fields.filter('[rel=account]').css('display', 'inline-block');
                                            $fields.filter('[rel=username]').css('display', 'inline-block');
                                            $fields.filter('[rel=key]').css('display', 'inline-block');
                                        }
                                    });                                  
                                    args.$select.change();     
                        		}
                        	});                         	
                        }
                    },

                    name: {
                        label: 'label.name',
                        isHidden: true
                    },
                    
                    //NFS (begin)
                    nfsServer: {
                        label: 'label.nfs.server',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },
                    path: {
                        label: 'label.path',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },
                    //NFS (end)


                    //S3 (begin)
                    accesskey: {
                        label: 'label.s3.access_key',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },
                    secretkey: {
                        label: 'label.s3.secret_key',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },
                    bucket: {
                        label: 'label.s3.bucket',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },
                    endpoint: {
                        label: 'label.s3.endpoint',
                        isHidden: true
                    },
                    usehttps: {
                        label: 'label.s3.use_https',                        
                        isBoolean: true,
                        isChecked: true,
                        isHidden: true
                    },
                    connectiontimeout: {
                        label: 'label.s3.connection_timeout',
                        isHidden: true
                    },
                    maxerrorretry: {
                        label: 'label.s3.max_error_retry',
                        isHidden: true
                    },
                    sockettimeout: {
                        label: 'label.s3.socket_timeout',
                        isHidden: true
                    },

                    createNfsCache: {
                        label: 'Create NFS Secondary Staging Store',
                        isBoolean: true,
                        isChecked: true,
                        isHidden: true
                    },
                    nfsCacheNfsServer: {
                        dependsOn: 'createNfsCache',
                        label: 'label.s3.nfs.server',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },
                    nfsCachePath: {
                        dependsOn: 'createNfsCache',
                        label: 'label.s3.nfs.path',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },
                    //S3 (end)


                    //Swift (begin)
                    url: {
                        label: 'label.url',
                        validation: {
                            required: true
                        },
                        isHidden: true
                    },
                    account: {
                        label: 'label.account',
                        isHidden: true
                    },
                    username: {
                        label: 'label.username',
                        isHidden: true
                    },
                    key: {
                        label: 'label.key',
                        isHidden: true
                    }
                    //Swift (end)
                }
            }
        },

        action: function(args) {      	
        	var $wizard = args.wizard;
        	        	
            var advZoneConfiguredVirtualRouterCount = 0; //for multiple physical networks in advanced zone. Each physical network has 2 virtual routers: regular one and VPC one.

            var success = args.response.success;
            var error = args.response.error;
            var message = args.response.message;
            //var data = args.data;
            var startFn = args.startFn;
            var data = args.data;

            var stepFns = {
                addZone: function() {
                    message(dictionary['message.creating.zone']);

                    var array1 = [];
                    var networkType = args.data.zone.networkType; //"Basic", "Advanced"
                    array1.push("&networktype=" + todb(networkType));

                    if (networkType == "Basic") {
                        if (selectedNetworkOfferingHavingSG == true)
                            array1.push("&securitygroupenabled=true");
                        else
                            array1.push("&securitygroupenabled=false");
                    } else { // networkType == "Advanced"
                        if (args.data.zone.sgEnabled != true) {
                            array1.push("&securitygroupenabled=false");

                            if (args.data.zone.guestcidraddress != null && args.data.zone.guestcidraddress.length > 0)
                                array1.push("&guestcidraddress=" + todb(args.data.zone.guestcidraddress));
                        } else { // args.data.zone.sgEnabled	== true
                            array1.push("&securitygroupenabled=true");
                        }
                    }

                    array1.push("&name=" + todb(args.data.zone.name));

                    if (args.data.zone.localstorageenabled == 'on') {
                        array1.push("&localstorageenabled=true");
                    }

                    //IPv4
                    if (args.data.zone.ip4dns1 != null && args.data.zone.ip4dns1.length > 0)
                        array1.push("&dns1=" + todb(args.data.zone.ip4dns1));
                    if (args.data.zone.ip4dns2 != null && args.data.zone.ip4dns2.length > 0)
                        array1.push("&dns2=" + todb(args.data.zone.ip4dns2));

                    //IPv6
                    if (args.data.zone.ip6dns1 != null && args.data.zone.ip6dns1.length > 0)
                        array1.push("&ip6dns1=" + todb(args.data.zone.ip6dns1));
                    if (args.data.zone.ip6dns2 != null && args.data.zone.ip6dns2.length > 0)
                        array1.push("&ip6dns2=" + todb(args.data.zone.ip6dns2));


                    array1.push("&internaldns1=" + todb(args.data.zone.internaldns1));

                    var internaldns2 = args.data.zone.internaldns2;
                    if (internaldns2 != null && internaldns2.length > 0)
                        array1.push("&internaldns2=" + todb(internaldns2));

                    if (args.data.zone.networkdomain != null && args.data.zone.networkdomain.length > 0)
                        array1.push("&domain=" + todb(args.data.zone.networkdomain));

                    $.ajax({
                        url: createURL("createZone" + array1.join("")),
                        dataType: "json",
                        async: false,
                        success: function(json) {                        	
                            if (args.data.pluginFrom == null) { //from zone wizard, not from quick instsaller(args.data.pluginFrom != null && args.data.pluginFrom.name == 'installWizard') who doesn't have public checkbox
                                if(args.data.zone.isdedicated == 'on'){ //dedicated checkbox in zone wizard is checked    
                            	    message(dictionary['message.dedicate.zone']);                            	    
                            	    var data = {
                            	    	zoneid: json.createzoneresponse.zone.id	
                            	    };    
                                    if (args.data.zone.domain != null) 
                                    	$.extend(data, {
                                    		domainid: args.data.zone.domain
                                    	});  
                                    if (args.data.zone.account != "") 
                                    	$.extend(data, {
                                    		account: args.data.zone.account
                                    	});   
                                    $.ajax({
                                        url: createURL('dedicateZone'),
                                        data: data,
                                        success: function(json) {}
                                    });                                    
                                }
                            }                        	                       	
                        	
                            stepFns.addPhysicalNetworks({
                                data: $.extend(args.data, {
                                    returnedZone: json.createzoneresponse.zone
                                })
                            });
                        },
                        error: function(XMLHttpResponse) {
                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                            error('addZone', errorMsg, {
                                fn: 'addZone',
                                args: args
                            });
                        }
                    });
                },

                addPhysicalNetworks: function(args) {
                    message(dictionary['message.creating.physical.networks']);

                    var returnedPhysicalNetworks = [];

                    if (args.data.zone.networkType == "Basic") { //Basic zone ***
                        var requestedTrafficTypeCount = 2; //request guest traffic type, management traffic type
                        if (selectedNetworkOfferingHavingSG == true && selectedNetworkOfferingHavingEIP == true && selectedNetworkOfferingHavingELB == true)
                            requestedTrafficTypeCount++; //request public traffic type

                        //Basic zone has only one physical network
                        var array1 = [];
                        if ("physicalNetworks" in args.data) { //from add-zone-wizard
                            array1.push("&name=" + todb(args.data.physicalNetworks[0].name));
                        } else { //from quick-install-wizard
                            array1.push("&name=PhysicalNetworkInBasicZone");
                        }

                        $.ajax({
                            url: createURL("createPhysicalNetwork&zoneid=" + args.data.returnedZone.id + array1.join("")),
                            dataType: "json",
                            success: function(json) {
                                var jobId = json.createphysicalnetworkresponse.jobid;
                                var createPhysicalNetworkIntervalID = setInterval(function() {
                                    $.ajax({
                                        url: createURL("queryAsyncJobResult&jobid=" + jobId),
                                        dataType: "json",
                                        success: function(json) {
                                            var result = json.queryasyncjobresultresponse;
                                            if (result.jobstatus == 0) {
                                                return; //Job has not completed
                                            } else {
                                                clearInterval(createPhysicalNetworkIntervalID);

                                                if (result.jobstatus == 1) {
                                                    var returnedBasicPhysicalNetwork = result.jobresult.physicalnetwork;
                                                    var label = returnedBasicPhysicalNetwork.id + trafficLabelParam('guest', data);
                                                    var returnedTrafficTypes = [];

                                                    $.ajax({
                                                        url: createURL("addTrafficType&trafficType=Guest&physicalnetworkid=" + label),
                                                        dataType: "json",
                                                        success: function(json) {
                                                            var jobId = json.addtraffictyperesponse.jobid;
                                                            var addGuestTrafficTypeIntervalID = setInterval(function() {
                                                                $.ajax({
                                                                    url: createURL("queryAsyncJobResult&jobid=" + jobId),
                                                                    dataType: "json",
                                                                    success: function(json) {
                                                                        var result = json.queryasyncjobresultresponse;
                                                                        if (result.jobstatus == 0) {
                                                                            return; //Job has not completed
                                                                        } else {
                                                                            clearInterval(addGuestTrafficTypeIntervalID);

                                                                            if (result.jobstatus == 1) {
                                                                                returnedTrafficTypes.push(result.jobresult.traffictype);

                                                                                if (returnedTrafficTypes.length == requestedTrafficTypeCount) { //all requested traffic types have been added
                                                                                    returnedBasicPhysicalNetwork.returnedTrafficTypes = returnedTrafficTypes;

                                                                                    stepFns.configurePhysicalNetwork({
                                                                                        data: $.extend(args.data, {
                                                                                            returnedBasicPhysicalNetwork: returnedBasicPhysicalNetwork
                                                                                        })
                                                                                    });
                                                                                }
                                                                            } else if (result.jobstatus == 2) {
                                                                                alert("Failed to add Guest traffic type to basic zone. Error: " + _s(result.jobresult.errortext));
                                                                            }
                                                                        }
                                                                    },
                                                                    error: function(XMLHttpResponse) {
                                                                        var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                                        alert("Failed to add Guest traffic type to basic zone. Error: " + errorMsg);
                                                                    }
                                                                });
                                                            }, g_queryAsyncJobResultInterval);
                                                        }
                                                    });

                                                    label = trafficLabelParam('management', data);

                                                    $.ajax({
                                                        url: createURL("addTrafficType&trafficType=Management&physicalnetworkid=" + returnedBasicPhysicalNetwork.id + label),
                                                        dataType: "json",
                                                        success: function(json) {
                                                            var jobId = json.addtraffictyperesponse.jobid;
                                                            var addManagementTrafficTypeIntervalID = setInterval(function() {
                                                                $.ajax({
                                                                    url: createURL("queryAsyncJobResult&jobid=" + jobId),
                                                                    dataType: "json",
                                                                    success: function(json) {
                                                                        var result = json.queryasyncjobresultresponse;
                                                                        if (result.jobstatus == 0) {
                                                                            return; //Job has not completed
                                                                        } else {
                                                                            clearInterval(addManagementTrafficTypeIntervalID);

                                                                            if (result.jobstatus == 1) {
                                                                                returnedTrafficTypes.push(result.jobresult.traffictype);

                                                                                if (returnedTrafficTypes.length == requestedTrafficTypeCount) { //all requested traffic types have been added
                                                                                    returnedBasicPhysicalNetwork.returnedTrafficTypes = returnedTrafficTypes;

                                                                                    stepFns.configurePhysicalNetwork({
                                                                                        data: $.extend(args.data, {
                                                                                            returnedBasicPhysicalNetwork: returnedBasicPhysicalNetwork
                                                                                        })
                                                                                    });
                                                                                }
                                                                            } else if (result.jobstatus == 2) {
                                                                                alert("Failed to add Management traffic type to basic zone. Error: " + _s(result.jobresult.errortext));
                                                                            }
                                                                        }
                                                                    },
                                                                    error: function(XMLHttpResponse) {
                                                                        var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                                        alert("Failed to add Management traffic type to basic zone. Error: " + errorMsg);
                                                                    }
                                                                });
                                                            }, g_queryAsyncJobResultInterval);
                                                        }
                                                    });

                                                    // Storage traffic
                                                    if (data.physicalNetworks &&
                                                        $.inArray('storage', data.physicalNetworks[0].trafficTypes) > -1) {
                                                        label = trafficLabelParam('storage', data);
                                                        $.ajax({
                                                            url: createURL('addTrafficType&physicalnetworkid=' + returnedBasicPhysicalNetwork.id + '&trafficType=Storage' + label),
                                                            dataType: "json",
                                                            success: function(json) {
                                                                var jobId = json.addtraffictyperesponse.jobid;
                                                                var addStorageTrafficTypeIntervalID = setInterval(function() {
                                                                    $.ajax({
                                                                        url: createURL("queryAsyncJobResult&jobid=" + jobId),
                                                                        dataType: "json",
                                                                        success: function(json) {
                                                                            var result = json.queryasyncjobresultresponse;
                                                                            if (result.jobstatus == 0) {
                                                                                return; //Job has not completed
                                                                            } else {
                                                                                clearInterval(addStorageTrafficTypeIntervalID);

                                                                                if (result.jobstatus == 1) {
                                                                                    returnedTrafficTypes.push(result.jobresult.traffictype);

                                                                                    if (returnedTrafficTypes.length == requestedTrafficTypeCount) { //all requested traffic types have been added
                                                                                        returnedBasicPhysicalNetwork.returnedTrafficTypes = returnedTrafficTypes;

                                                                                        stepFns.configurePhysicalNetwork({
                                                                                            data: $.extend(args.data, {
                                                                                                returnedBasicPhysicalNetwork: returnedBasicPhysicalNetwork
                                                                                            })
                                                                                        });
                                                                                    }
                                                                                } else if (result.jobstatus == 2) {
                                                                                    alert("Failed to add Management traffic type to basic zone. Error: " + _s(result.jobresult.errortext));
                                                                                }
                                                                            }
                                                                        },
                                                                        error: function(XMLHttpResponse) {
                                                                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                                            alert("Failed to add Management traffic type to basic zone. Error: " + errorMsg);
                                                                        }
                                                                    });
                                                                }, g_queryAsyncJobResultInterval);
                                                            }
                                                        });
                                                    }

                                                    if (selectedNetworkOfferingHavingSG == true && selectedNetworkOfferingHavingEIP == true && selectedNetworkOfferingHavingELB == true) {
                                                        label = trafficLabelParam('public', data);
                                                        $.ajax({
                                                            url: createURL("addTrafficType&trafficType=Public&physicalnetworkid=" + returnedBasicPhysicalNetwork.id + label),
                                                            dataType: "json",
                                                            success: function(json) {
                                                                var jobId = json.addtraffictyperesponse.jobid;
                                                                var addPublicTrafficTypeIntervalID = setInterval(function() {
                                                                    $.ajax({
                                                                        url: createURL("queryAsyncJobResult&jobid=" + jobId),
                                                                        dataType: "json",
                                                                        success: function(json) {
                                                                            var result = json.queryasyncjobresultresponse;
                                                                            if (result.jobstatus == 0) {
                                                                                return; //Job has not completed
                                                                            } else {
                                                                                clearInterval(addPublicTrafficTypeIntervalID);

                                                                                if (result.jobstatus == 1) {
                                                                                    returnedTrafficTypes.push(result.jobresult.traffictype);

                                                                                    if (returnedTrafficTypes.length == requestedTrafficTypeCount) { //all requested traffic types have been added
                                                                                        returnedBasicPhysicalNetwork.returnedTrafficTypes = returnedTrafficTypes;

                                                                                        stepFns.configurePhysicalNetwork({
                                                                                            data: $.extend(args.data, {
                                                                                                returnedBasicPhysicalNetwork: returnedBasicPhysicalNetwork
                                                                                            })
                                                                                        });
                                                                                    }
                                                                                } else if (result.jobstatus == 2) {
                                                                                    alert("Failed to add Public traffic type to basic zone. Error: " + _s(result.jobresult.errortext));
                                                                                }
                                                                            }
                                                                        },
                                                                        error: function(XMLHttpResponse) {
                                                                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                                            alert("Failed to add Public traffic type to basic zone. Error: " + errorMsg);
                                                                        }
                                                                    });
                                                                }, g_queryAsyncJobResultInterval);
                                                            }
                                                        });
                                                    }
                                                } else if (result.jobstatus == 2) {
                                                    alert("createPhysicalNetwork failed. Error: " + _s(result.jobresult.errortext));
                                                }
                                            }
                                        },
                                        error: function(XMLHttpResponse) {
                                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                            alert("createPhysicalNetwork failed. Error: " + errorMsg);
                                        }
                                    });
                                }, g_queryAsyncJobResultInterval);

                            }
                        });
                    } else if (args.data.zone.networkType == "Advanced") {
                        $(args.data.physicalNetworks).each(function(index) {
                            var thisPhysicalNetwork = this;
                            var array1 = [];
                            array1.push("&name=" + todb(thisPhysicalNetwork.name));
                            if (thisPhysicalNetwork.isolationMethod != null && thisPhysicalNetwork.isolationMethod.length > 0)
                                array1.push("&isolationmethods=" + todb(thisPhysicalNetwork.isolationMethod));
                            $.ajax({
                                url: createURL("createPhysicalNetwork&zoneid=" + args.data.returnedZone.id + array1.join("")),
                                dataType: "json",
                                success: function(json) {
                                    var jobId = json.createphysicalnetworkresponse.jobid;
                                    var createPhysicalNetworkIntervalID = setInterval(function() {
                                        $.ajax({
                                            url: createURL("queryAsyncJobResult&jobid=" + jobId),
                                            dataType: "json",
                                            success: function(json) {
                                                var result = json.queryasyncjobresultresponse;
                                                if (result.jobstatus == 0) {
                                                    return; //Job has not completed
                                                } else {
                                                    clearInterval(createPhysicalNetworkIntervalID);

                                                    if (result.jobstatus == 1) {
                                                        var returnedPhysicalNetwork = result.jobresult.physicalnetwork;
                                                        returnedPhysicalNetwork.originalId = thisPhysicalNetwork.id;

                                                        var returnedTrafficTypes = [];
                                                        var label; // Traffic type label
                                                        $(thisPhysicalNetwork.trafficTypes).each(function() {
                                                            var thisTrafficType = this;
                                                            var apiCmd = "addTrafficType&physicalnetworkid=" + returnedPhysicalNetwork.id;
                                                            if (thisTrafficType == "public") {
                                                                apiCmd += "&trafficType=Public";
                                                                label = trafficLabelParam('public', data, index);
                                                            } else if (thisTrafficType == "management") {
                                                                apiCmd += "&trafficType=Management";
                                                                label = trafficLabelParam('management', data, index);
                                                            } else if (thisTrafficType == "guest") {
                                                                apiCmd += "&trafficType=Guest";
                                                                label = trafficLabelParam('guest', data, index);
                                                            } else if (thisTrafficType == "storage") {
                                                                apiCmd += "&trafficType=Storage";
                                                                label = trafficLabelParam('storage', data, index);
                                                            }

                                                            $.ajax({
                                                                url: createURL(apiCmd + label),
                                                                dataType: "json",
                                                                success: function(json) {
                                                                    var jobId = json.addtraffictyperesponse.jobid;
                                                                    var addTrafficTypeIntervalID = setInterval(function() {
                                                                        $.ajax({
                                                                            url: createURL("queryAsyncJobResult&jobid=" + jobId),
                                                                            dataType: "json",
                                                                            success: function(json) {
                                                                                var result = json.queryasyncjobresultresponse;
                                                                                if (result.jobstatus == 0) {
                                                                                    return; //Job has not completed
                                                                                } else {
                                                                                    clearInterval(addTrafficTypeIntervalID);

                                                                                    if (result.jobstatus == 1) {
                                                                                        returnedTrafficTypes.push(result.jobresult.traffictype);

                                                                                        if (returnedTrafficTypes.length == thisPhysicalNetwork.trafficTypes.length) { //this physical network is complete (specified traffic types are added)
                                                                                            returnedPhysicalNetwork.returnedTrafficTypes = returnedTrafficTypes;
                                                                                            returnedPhysicalNetworks.push(returnedPhysicalNetwork);

                                                                                            if (returnedPhysicalNetworks.length == args.data.physicalNetworks.length) { //all physical networks are complete
                                                                                                stepFns.configurePhysicalNetwork({
                                                                                                    data: $.extend(args.data, {
                                                                                                        returnedPhysicalNetworks: returnedPhysicalNetworks
                                                                                                    })
                                                                                                });
                                                                                            }
                                                                                        }
                                                                                    } else if (result.jobstatus == 2) {
                                                                                        alert(apiCmd + " failed. Error: " + _s(result.jobresult.errortext));
                                                                                    }
                                                                                }
                                                                            },
                                                                            error: function(XMLHttpResponse) {
                                                                                var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                                                alert(apiCmd + " failed. Error: " + errorMsg);
                                                                            }
                                                                        });
                                                                    }, g_queryAsyncJobResultInterval);
                                                                }
                                                            });
                                                        });
                                                    } else if (result.jobstatus == 2) {
                                                        alert("createPhysicalNetwork failed. Error: " + _s(result.jobresult.errortext));
                                                    }
                                                }
                                            },
                                            error: function(XMLHttpResponse) {
                                                var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                alert("createPhysicalNetwork failed. Error: " + errorMsg);
                                            }
                                        });
                                    }, g_queryAsyncJobResultInterval);
                                }
                            });
                        });
                    }
                },

                //afterCreateZonePhysicalNetworkTrafficTypes: enable physical network, enable virtual router element, enable network service provider
                configurePhysicalNetwork: function(args) {
                    message(dictionary['message.configuring.physical.networks']);

                    if (args.data.zone.networkType == "Basic") {
                        $.ajax({
                            url: createURL("updatePhysicalNetwork&state=Enabled&id=" + args.data.returnedBasicPhysicalNetwork.id),
                            dataType: "json",
                            success: function(json) {
                                var enablePhysicalNetworkIntervalID = setInterval(function() {
                                    $.ajax({
                                        url: createURL("queryAsyncJobResult&jobId=" + json.updatephysicalnetworkresponse.jobid),
                                        dataType: "json",
                                        success: function(json) {
                                            var result = json.queryasyncjobresultresponse;
                                            if (result.jobstatus == 0) {
                                                return; //Job has not completed
                                            } else {
                                                clearInterval(enablePhysicalNetworkIntervalID);

                                                if (result.jobstatus == 1) {
                                                    //alert("updatePhysicalNetwork succeeded.");

                                                    // get network service provider ID of Virtual Router
                                                    var virtualRouterProviderId;
                                                    $.ajax({
                                                        url: createURL("listNetworkServiceProviders&name=VirtualRouter&physicalNetworkId=" + args.data.returnedBasicPhysicalNetwork.id),
                                                        dataType: "json",
                                                        async: false,
                                                        success: function(json) {
                                                            var items = json.listnetworkserviceprovidersresponse.networkserviceprovider;
                                                            if (items != null && items.length > 0) {
                                                                virtualRouterProviderId = items[0].id;
                                                            }
                                                        }
                                                    });
                                                    if (virtualRouterProviderId == null) {
                                                        alert("error: listNetworkServiceProviders API doesn't return VirtualRouter provider ID");
                                                        return;
                                                    }

                                                    var virtualRouterElementId;
                                                    $.ajax({
                                                        url: createURL("listVirtualRouterElements&nspid=" + virtualRouterProviderId),
                                                        dataType: "json",
                                                        async: false,
                                                        success: function(json) {
                                                            var items = json.listvirtualrouterelementsresponse.virtualrouterelement;
                                                            if (items != null && items.length > 0) {
                                                                virtualRouterElementId = items[0].id;
                                                            }
                                                        }
                                                    });
                                                    if (virtualRouterElementId == null) {
                                                        alert("error: listVirtualRouterElements API doesn't return Virtual Router Element Id");
                                                        return;
                                                    }

                                                    $.ajax({
                                                        url: createURL("configureVirtualRouterElement&enabled=true&id=" + virtualRouterElementId),
                                                        dataType: "json",
                                                        async: false,
                                                        success: function(json) {
                                                            var enableVirtualRouterElementIntervalID = setInterval(function() {
                                                                $.ajax({
                                                                    url: createURL("queryAsyncJobResult&jobId=" + json.configurevirtualrouterelementresponse.jobid),
                                                                    dataType: "json",
                                                                    success: function(json) {
                                                                        var result = json.queryasyncjobresultresponse;
                                                                        if (result.jobstatus == 0) {
                                                                            return; //Job has not completed
                                                                        } else {
                                                                            clearInterval(enableVirtualRouterElementIntervalID);

                                                                            if (result.jobstatus == 1) {
                                                                                //alert("configureVirtualRouterElement succeeded.");

                                                                                if (args.data.pluginFrom != null && args.data.pluginFrom.name == "installWizard") {
                                                                                    selectedNetworkOfferingObj = args.data.pluginFrom.selectedNetworkOffering;
                                                                                }

                                                                                var data = {
                                                                                    id: virtualRouterProviderId,
                                                                                    state: 'Enabled'
                                                                                };

                                                                                $.ajax({
                                                                                    url: createURL("updateNetworkServiceProvider"),
                                                                                    data: data,
                                                                                    async: false,
                                                                                    success: function(json) {
                                                                                        var enableVirtualRouterProviderIntervalID = setInterval(function() {
                                                                                            $.ajax({
                                                                                                url: createURL("queryAsyncJobResult&jobId=" + json.updatenetworkserviceproviderresponse.jobid),
                                                                                                dataType: "json",
                                                                                                success: function(json) {
                                                                                                    var result = json.queryasyncjobresultresponse;
                                                                                                    if (result.jobstatus == 0) {
                                                                                                        return; //Job has not completed
                                                                                                    } else {
                                                                                                        clearInterval(enableVirtualRouterProviderIntervalID);

                                                                                                        if (result.jobstatus == 1) {
                                                                                                            //alert("Virtual Router Provider is enabled");
                                                                                                            for (var i = 0; i < selectedBaremetalProviders.length; i++) {
                                                                                                                $.ajax({
                                                                                                                    url: createURL("listNetworkServiceProviders"),
                                                                                                                    data: {
                                                                                                                        name: selectedBaremetalProviders[i],
                                                                                                                        physicalNetworkId: args.data.returnedBasicPhysicalNetwork.id
                                                                                                                    },
                                                                                                                    async: false,
                                                                                                                    success: function(json) {
                                                                                                                        var items = json.listnetworkserviceprovidersresponse.networkserviceprovider;
                                                                                                                        if (items != null && items.length > 0) {
                                                                                                                            var providerId = items[0].id;
                                                                                                                            $.ajax({
                                                                                                                                url: createURL("updateNetworkServiceProvider"),
                                                                                                                                data: {
                                                                                                                                    id: providerId,
                                                                                                                                    state: 'Enabled'
                                                                                                                                },
                                                                                                                                async: false,
                                                                                                                                success: function(json) {
                                                                                                                                    var updateNetworkServiceProviderIntervalID = setInterval(function() {
                                                                                                                                        $.ajax({
                                                                                                                                            url: createURL("queryAsyncJobResult&jobId=" + json.updatenetworkserviceproviderresponse.jobid),
                                                                                                                                            dataType: "json",
                                                                                                                                            success: function(json) {
                                                                                                                                                var result = json.queryasyncjobresultresponse;
                                                                                                                                                if (result.jobstatus == 0) {
                                                                                                                                                    return; //Job has not completed
                                                                                                                                                } else {
                                                                                                                                                    clearInterval(updateNetworkServiceProviderIntervalID);
                                                                                                                                                    if (result.jobstatus == 1) { //baremetal provider has been enabled successfully

                                                                                                                                                    } else if (result.jobstatus == 2) {
                                                                                                                                                        alert(_s(result.jobresult.errortext));
                                                                                                                                                    }
                                                                                                                                                }
                                                                                                                                            },
                                                                                                                                            error: function(XMLHttpResponse) {
                                                                                                                                                alert(parseXMLHttpResponse(XMLHttpResponse));
                                                                                                                                            }
                                                                                                                                        });
                                                                                                                                    }, g_queryAsyncJobResultInterval);
                                                                                                                                }
                                                                                                                            });
                                                                                                                        }
                                                                                                                    }
                                                                                                                });
                                                                                                            }

                                                                                                            if (args.data.pluginFrom != null && args.data.pluginFrom.name == "installWizard") {
                                                                                                                selectedNetworkOfferingHavingSG = args.data.pluginFrom.selectedNetworkOfferingHavingSG;
                                                                                                            }
                                                                                                            if (selectedNetworkOfferingHavingSG == true) { //need to Enable security group provider first
                                                                                                                message(dictionary['message.enabling.security.group.provider']);

                                                                                                                // get network service provider ID of Security Group
                                                                                                                var securityGroupProviderId;
                                                                                                                $.ajax({
                                                                                                                    url: createURL("listNetworkServiceProviders&name=SecurityGroupProvider&physicalNetworkId=" + args.data.returnedBasicPhysicalNetwork.id),
                                                                                                                    dataType: "json",
                                                                                                                    async: false,
                                                                                                                    success: function(json) {
                                                                                                                        var items = json.listnetworkserviceprovidersresponse.networkserviceprovider;
                                                                                                                        if (items != null && items.length > 0) {
                                                                                                                            securityGroupProviderId = items[0].id;
                                                                                                                        }
                                                                                                                    }
                                                                                                                });
                                                                                                                if (securityGroupProviderId == null) {
                                                                                                                    alert("error: listNetworkServiceProviders API doesn't return security group provider ID");
                                                                                                                    return;
                                                                                                                }

                                                                                                                $.ajax({
                                                                                                                    url: createURL("updateNetworkServiceProvider&state=Enabled&id=" + securityGroupProviderId),
                                                                                                                    dataType: "json",
                                                                                                                    async: false,
                                                                                                                    success: function(json) {
                                                                                                                        var enableSecurityGroupProviderIntervalID = setInterval(function() {
                                                                                                                            $.ajax({
                                                                                                                                url: createURL("queryAsyncJobResult&jobId=" + json.updatenetworkserviceproviderresponse.jobid),
                                                                                                                                dataType: "json",
                                                                                                                                success: function(json) {
                                                                                                                                    var result = json.queryasyncjobresultresponse;
                                                                                                                                    if (result.jobstatus == 0) {
                                                                                                                                        return; //Job has not completed
                                                                                                                                    } else {
                                                                                                                                        clearInterval(enableSecurityGroupProviderIntervalID);

                                                                                                                                        if (result.jobstatus == 1) { //Security group provider has been enabled successfully
                                                                                                                                            stepFns.addNetscalerProvider({
                                                                                                                                                data: args.data
                                                                                                                                            });
                                                                                                                                        } else if (result.jobstatus == 2) {
                                                                                                                                            alert("failed to enable security group provider. Error: " + _s(result.jobresult.errortext));
                                                                                                                                        }
                                                                                                                                    }
                                                                                                                                },
                                                                                                                                error: function(XMLHttpResponse) {
                                                                                                                                    var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                                                                                                    alert("failed to enable security group provider. Error: " + errorMsg);
                                                                                                                                }
                                                                                                                            });
                                                                                                                        }, g_queryAsyncJobResultInterval);
                                                                                                                    }
                                                                                                                });
                                                                                                            } else { //selectedNetworkOfferingHavingSG == false
                                                                                                                stepFns.addNetscalerProvider({
                                                                                                                    data: args.data
                                                                                                                });
                                                                                                            }
                                                                                                        } else if (result.jobstatus == 2) {
                                                                                                            alert("failed to enable Virtual Router Provider. Error: " + _s(result.jobresult.errortext));
                                                                                                        }
                                                                                                    }
                                                                                                },
                                                                                                error: function(XMLHttpResponse) {
                                                                                                    var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                                                                    alert("failed to enable Virtual Router Provider. Error: " + errorMsg);
                                                                                                }
                                                                                            });
                                                                                        }, g_queryAsyncJobResultInterval);
                                                                                    }
                                                                                });
                                                                            } else if (result.jobstatus == 2) {
                                                                                alert("configureVirtualRouterElement failed. Error: " + _s(result.jobresult.errortext));
                                                                            }
                                                                        }
                                                                    },
                                                                    error: function(XMLHttpResponse) {
                                                                        var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                                        alert("configureVirtualRouterElement failed. Error: " + errorMsg);
                                                                    }
                                                                });
                                                            }, g_queryAsyncJobResultInterval);
                                                        }
                                                    });
                                                } else if (result.jobstatus == 2) {
                                                    alert("updatePhysicalNetwork failed. Error: " + _s(result.jobresult.errortext));
                                                }
                                            }
                                        },
                                        error: function(XMLHttpResponse) {
                                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                            alert("updatePhysicalNetwork failed. Error: " + errorMsg);
                                        }
                                    });
                                }, g_queryAsyncJobResultInterval);
                            }
                        });
                    } else if (args.data.zone.networkType == "Advanced") {
                        $(args.data.returnedPhysicalNetworks).each(function() {
                            var thisPhysicalNetwork = this;
                            $.ajax({
                                url: createURL("updatePhysicalNetwork&state=Enabled&id=" + thisPhysicalNetwork.id),
                                dataType: "json",
                                success: function(json) {
                                    var jobId = json.updatephysicalnetworkresponse.jobid;
                                    var enablePhysicalNetworkIntervalID = setInterval(function() {
                                        $.ajax({
                                            url: createURL("queryAsyncJobResult&jobId=" + jobId),
                                            dataType: "json",
                                            success: function(json) {
                                                var result = json.queryasyncjobresultresponse;
                                                if (result.jobstatus == 0) {
                                                    return; //Job has not completed
                                                } else {
                                                    clearInterval(enablePhysicalNetworkIntervalID);

                                                    if (result.jobstatus == 1) {
                                                        //alert("enable physical network succeeded.");

                                                        // ***** Virtual Router ***** (begin) *****
                                                        var virtualRouterProviderId;
                                                        $.ajax({
                                                            url: createURL("listNetworkServiceProviders&name=VirtualRouter&physicalNetworkId=" + thisPhysicalNetwork.id),
                                                            dataType: "json",
                                                            async: false,
                                                            success: function(json) {
                                                                var items = json.listnetworkserviceprovidersresponse.networkserviceprovider;
                                                                if (items != null && items.length > 0) {
                                                                    virtualRouterProviderId = items[0].id;
                                                                }
                                                            }
                                                        });
                                                        if (virtualRouterProviderId == null) {
                                                            alert("error: listNetworkServiceProviders API doesn't return VirtualRouter provider ID");
                                                            return;
                                                        }

                                                        var virtualRouterElementId;
                                                        $.ajax({
                                                            url: createURL("listVirtualRouterElements&nspid=" + virtualRouterProviderId),
                                                            dataType: "json",
                                                            async: false,
                                                            success: function(json) {
                                                                var items = json.listvirtualrouterelementsresponse.virtualrouterelement;
                                                                if (items != null && items.length > 0) {
                                                                    virtualRouterElementId = items[0].id;
                                                                }
                                                            }
                                                        });
                                                        if (virtualRouterElementId == null) {
                                                            alert("error: listVirtualRouterElements API doesn't return Virtual Router Element Id");
                                                            return;
                                                        }

                                                        $.ajax({
                                                            url: createURL("configureVirtualRouterElement&enabled=true&id=" + virtualRouterElementId),
                                                            dataType: "json",
                                                            async: false,
                                                            success: function(json) {
                                                                var jobId = json.configurevirtualrouterelementresponse.jobid;
                                                                var enableVirtualRouterElementIntervalID = setInterval(function() {
                                                                    $.ajax({
                                                                        url: createURL("queryAsyncJobResult&jobId=" + jobId),
                                                                        dataType: "json",
                                                                        success: function(json) {
                                                                            var result = json.queryasyncjobresultresponse;
                                                                            if (result.jobstatus == 0) {
                                                                                return; //Job has not completed
                                                                            } else {
                                                                                clearInterval(enableVirtualRouterElementIntervalID);

                                                                                if (result.jobstatus == 1) { //configureVirtualRouterElement succeeded
                                                                                    $.ajax({
                                                                                        url: createURL("updateNetworkServiceProvider&state=Enabled&id=" + virtualRouterProviderId),
                                                                                        dataType: "json",
                                                                                        async: false,
                                                                                        success: function(json) {
                                                                                            var jobId = json.updatenetworkserviceproviderresponse.jobid;
                                                                                            var enableVirtualRouterProviderIntervalID = setInterval(function() {
                                                                                                $.ajax({
                                                                                                    url: createURL("queryAsyncJobResult&jobId=" + jobId),
                                                                                                    dataType: "json",
                                                                                                    success: function(json) {
                                                                                                        var result = json.queryasyncjobresultresponse;
                                                                                                        if (result.jobstatus == 0) {
                                                                                                            return; //Job has not completed
                                                                                                        } else {
                                                                                                            clearInterval(enableVirtualRouterProviderIntervalID);

                                                                                                            if (result.jobstatus == 1) { //Virtual Router Provider has been enabled successfully
                                                                                                                advZoneConfiguredVirtualRouterCount++;

                                                                                                                if (advZoneConfiguredVirtualRouterCount == args.data.returnedPhysicalNetworks.length) { //not call next stepFns.addXXX() until virtualRouter of all physical networks get configured
                                                                                                                    if (args.data.zone.sgEnabled != true) { //Advanced SG-disabled zone
                                                                                                                        stepFns.addPod({
                                                                                                                            data: args.data
                                                                                                                        });
                                                                                                                    } else { //args.data.zone.sgEnabled	== true  //Advanced SG-enabled zone
                                                                                                                        stepFns.addGuestNetwork({
                                                                                                                            data: args.data
                                                                                                                        });
                                                                                                                    }
                                                                                                                }
                                                                                                            } else if (result.jobstatus == 2) {
                                                                                                                alert("failed to enable Virtual Router Provider. Error: " + _s(result.jobresult.errortext));
                                                                                                            }
                                                                                                        }
                                                                                                    },
                                                                                                    error: function(XMLHttpResponse) {
                                                                                                        var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                                                                        alert("updateNetworkServiceProvider failed. Error: " + errorMsg);
                                                                                                    }
                                                                                                });
                                                                                            }, g_queryAsyncJobResultInterval);
                                                                                        }
                                                                                    });
                                                                                } else if (result.jobstatus == 2) {
                                                                                    alert("configureVirtualRouterElement failed. Error: " + _s(result.jobresult.errortext));
                                                                                }
                                                                            }
                                                                        },
                                                                        error: function(XMLHttpResponse) {
                                                                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                                            alert("configureVirtualRouterElement failed. Error: " + errorMsg);
                                                                        }
                                                                    });
                                                                }, g_queryAsyncJobResultInterval);
                                                            }
                                                        });
                                                        // ***** Virtual Router ***** (end) *****

                                                        // ***** Internal LB ***** (begin) *****
                                                        var internalLbProviderId;
                                                        $.ajax({
                                                            url: createURL("listNetworkServiceProviders&name=Internallbvm&physicalNetworkId=" + thisPhysicalNetwork.id),
                                                            dataType: "json",
                                                            async: false,
                                                            success: function(json) {
                                                                var items = json.listnetworkserviceprovidersresponse.networkserviceprovider;
                                                                if (items != null && items.length > 0) {
                                                                    internalLbProviderId = items[0].id;
                                                                }
                                                            }
                                                        });
                                                        if (internalLbProviderId == null) {
                                                            alert("error: listNetworkServiceProviders API doesn't return internalLb provider ID");
                                                            return;
                                                        }

                                                        var internalLbElementId;
                                                        $.ajax({
                                                            url: createURL("listInternalLoadBalancerElements&nspid=" + internalLbProviderId),
                                                            dataType: "json",
                                                            async: false,
                                                            success: function(json) {
                                                                var items = json.listinternalloadbalancerelementsresponse.internalloadbalancerelement;
                                                                if (items != null && items.length > 0) {
                                                                    internalLbElementId = items[0].id;
                                                                }
                                                            }
                                                        });
                                                        if (internalLbElementId == null) {
                                                            alert("error: listInternalLoadBalancerElements API doesn't return Internal LB Element Id");
                                                            return;
                                                        }

                                                        $.ajax({
                                                            url: createURL("configureInternalLoadBalancerElement&enabled=true&id=" + internalLbElementId),
                                                            dataType: "json",
                                                            async: false,
                                                            success: function(json) {
                                                                var jobId = json.configureinternalloadbalancerelementresponse.jobid;
                                                                var enableInternalLbElementIntervalID = setInterval(function() {
                                                                    $.ajax({
                                                                        url: createURL("queryAsyncJobResult&jobId=" + jobId),
                                                                        dataType: "json",
                                                                        success: function(json) {
                                                                            var result = json.queryasyncjobresultresponse;
                                                                            if (result.jobstatus == 0) {
                                                                                return; //Job has not completed
                                                                            } else {
                                                                                clearInterval(enableInternalLbElementIntervalID);

                                                                                if (result.jobstatus == 1) { //configureVirtualRouterElement succeeded
                                                                                    $.ajax({
                                                                                        url: createURL("updateNetworkServiceProvider&state=Enabled&id=" + internalLbProviderId),
                                                                                        dataType: "json",
                                                                                        async: false,
                                                                                        success: function(json) {
                                                                                            var jobId = json.updatenetworkserviceproviderresponse.jobid;
                                                                                            var enableInternalLbProviderIntervalID = setInterval(function() {
                                                                                                $.ajax({
                                                                                                    url: createURL("queryAsyncJobResult&jobId=" + jobId),
                                                                                                    dataType: "json",
                                                                                                    success: function(json) {
                                                                                                        var result = json.queryasyncjobresultresponse;
                                                                                                        if (result.jobstatus == 0) {
                                                                                                            return; //Job has not completed
                                                                                                        } else {
                                                                                                            clearInterval(enableInternalLbProviderIntervalID);

                                                                                                            if (result.jobstatus == 1) { //Internal LB has been enabled successfully
                                                                                                                //don't need to do anything here
                                                                                                            } else if (result.jobstatus == 2) {
                                                                                                                alert("failed to enable Internal LB Provider. Error: " + _s(result.jobresult.errortext));
                                                                                                            }
                                                                                                        }
                                                                                                    },
                                                                                                    error: function(XMLHttpResponse) {
                                                                                                        var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                                                                        alert("failed to enable Internal LB Provider. Error: " + errorMsg);
                                                                                                    }
                                                                                                });
                                                                                            }, g_queryAsyncJobResultInterval);
                                                                                        }
                                                                                    });
                                                                                } else if (result.jobstatus == 2) {
                                                                                    alert("configureVirtualRouterElement failed. Error: " + _s(result.jobresult.errortext));
                                                                                }
                                                                            }
                                                                        },
                                                                        error: function(XMLHttpResponse) {
                                                                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                                            alert("configureVirtualRouterElement failed. Error: " + errorMsg);
                                                                        }
                                                                    });
                                                                }, g_queryAsyncJobResultInterval);
                                                            }
                                                        });
                                                        // ***** Internal LB ***** (end) *****

                                                        if (args.data.zone.sgEnabled != true) { //Advanced SG-disabled zone
                                                            // ***** VPC Virtual Router ***** (begin) *****
                                                            var vpcVirtualRouterProviderId;
                                                            $.ajax({
                                                                url: createURL("listNetworkServiceProviders&name=VpcVirtualRouter&physicalNetworkId=" + thisPhysicalNetwork.id),
                                                                dataType: "json",
                                                                async: false,
                                                                success: function(json) {
                                                                    var items = json.listnetworkserviceprovidersresponse.networkserviceprovider;
                                                                    if (items != null && items.length > 0) {
                                                                        vpcVirtualRouterProviderId = items[0].id;
                                                                    }
                                                                }
                                                            });
                                                            if (vpcVirtualRouterProviderId == null) {
                                                                alert("error: listNetworkServiceProviders API doesn't return VpcVirtualRouter provider ID");
                                                                return;
                                                            }

                                                            var vpcVirtualRouterElementId;
                                                            $.ajax({
                                                                url: createURL("listVirtualRouterElements&nspid=" + vpcVirtualRouterProviderId),
                                                                dataType: "json",
                                                                async: false,
                                                                success: function(json) {
                                                                    var items = json.listvirtualrouterelementsresponse.virtualrouterelement;
                                                                    if (items != null && items.length > 0) {
                                                                        vpcVirtualRouterElementId = items[0].id;
                                                                    }
                                                                }
                                                            });
                                                            if (vpcVirtualRouterElementId == null) {
                                                                alert("error: listVirtualRouterElements API doesn't return VPC Virtual Router Element Id");
                                                                return;
                                                            }

                                                            $.ajax({
                                                                url: createURL("configureVirtualRouterElement&enabled=true&id=" + vpcVirtualRouterElementId),
                                                                dataType: "json",
                                                                async: false,
                                                                success: function(json) {
                                                                    var jobId = json.configurevirtualrouterelementresponse.jobid;
                                                                    var enableVpcVirtualRouterElementIntervalID = setInterval(function() {
                                                                        $.ajax({
                                                                            url: createURL("queryAsyncJobResult&jobId=" + jobId),
                                                                            dataType: "json",
                                                                            success: function(json) {
                                                                                var result = json.queryasyncjobresultresponse;
                                                                                if (result.jobstatus == 0) {
                                                                                    return; //Job has not completed
                                                                                } else {
                                                                                    clearInterval(enableVpcVirtualRouterElementIntervalID);

                                                                                    if (result.jobstatus == 1) { //configureVirtualRouterElement succeeded
                                                                                        $.ajax({
                                                                                            url: createURL("updateNetworkServiceProvider&state=Enabled&id=" + vpcVirtualRouterProviderId),
                                                                                            dataType: "json",
                                                                                            async: false,
                                                                                            success: function(json) {
                                                                                                var jobId = json.updatenetworkserviceproviderresponse.jobid;
                                                                                                var enableVpcVirtualRouterProviderIntervalID = setInterval(function() {
                                                                                                    $.ajax({
                                                                                                        url: createURL("queryAsyncJobResult&jobId=" + jobId),
                                                                                                        dataType: "json",
                                                                                                        success: function(json) {
                                                                                                            var result = json.queryasyncjobresultresponse;
                                                                                                            if (result.jobstatus == 0) {
                                                                                                                return; //Job has not completed
                                                                                                            } else {
                                                                                                                clearInterval(enableVpcVirtualRouterProviderIntervalID);

                                                                                                                if (result.jobstatus == 1) { //VPC Virtual Router has been enabled successfully
                                                                                                                    //don't need to do anything here
                                                                                                                } else if (result.jobstatus == 2) {
                                                                                                                    alert("failed to enable VPC Virtual Router Provider. Error: " + _s(result.jobresult.errortext));
                                                                                                                }
                                                                                                            }
                                                                                                        },
                                                                                                        error: function(XMLHttpResponse) {
                                                                                                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                                                                            alert("failed to enable VPC Virtual Router Provider. Error: " + errorMsg);
                                                                                                        }
                                                                                                    });
                                                                                                }, g_queryAsyncJobResultInterval);
                                                                                            }
                                                                                        });
                                                                                    } else if (result.jobstatus == 2) {
                                                                                        alert("configureVirtualRouterElement failed. Error: " + _s(result.jobresult.errortext));
                                                                                    }
                                                                                }
                                                                            },
                                                                            error: function(XMLHttpResponse) {
                                                                                var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                                                alert("configureVirtualRouterElement failed. Error: " + errorMsg);
                                                                            }
                                                                        });
                                                                    }, g_queryAsyncJobResultInterval);
                                                                }
                                                            });
                                                            // ***** VPC Virtual Router ***** (end) *****
                                                        } else { //args.data.zone.sgEnabled == true  //Advanced SG-enabled zone
                                                            message(dictionary['message.enabling.security.group.provider']);

                                                            // get network service provider ID of Security Group
                                                            var securityGroupProviderId;
                                                            $.ajax({
                                                                url: createURL("listNetworkServiceProviders&name=SecurityGroupProvider&physicalNetworkId=" + thisPhysicalNetwork.id),
                                                                dataType: "json",
                                                                async: false,
                                                                success: function(json) {
                                                                    var items = json.listnetworkserviceprovidersresponse.networkserviceprovider;
                                                                    if (items != null && items.length > 0) {
                                                                        securityGroupProviderId = items[0].id;
                                                                    }
                                                                }
                                                            });
                                                            if (securityGroupProviderId == null) {
                                                                alert("error: listNetworkServiceProviders API doesn't return security group provider ID");
                                                                return;
                                                            }

                                                            $.ajax({
                                                                url: createURL("updateNetworkServiceProvider&state=Enabled&id=" + securityGroupProviderId),
                                                                dataType: "json",
                                                                async: false,
                                                                success: function(json) {
                                                                    var enableSecurityGroupProviderIntervalID = setInterval(function() {
                                                                        $.ajax({
                                                                            url: createURL("queryAsyncJobResult&jobId=" + json.updatenetworkserviceproviderresponse.jobid),
                                                                            dataType: "json",
                                                                            success: function(json) {
                                                                                var result = json.queryasyncjobresultresponse;
                                                                                if (result.jobstatus == 0) {
                                                                                    return; //Job has not completed
                                                                                } else {
                                                                                    clearInterval(enableSecurityGroupProviderIntervalID);

                                                                                    if (result.jobstatus == 1) { //Security group provider has been enabled successfully
                                                                                        //don't need to do anything here
                                                                                    } else if (result.jobstatus == 2) {
                                                                                        alert("failed to enable security group provider. Error: " + _s(result.jobresult.errortext));
                                                                                    }
                                                                                }
                                                                            },
                                                                            error: function(XMLHttpResponse) {
                                                                                var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                                                alert("failed to enable security group provider. Error: " + errorMsg);
                                                                            }
                                                                        });
                                                                    }, g_queryAsyncJobResultInterval);
                                                                }
                                                            });
                                                        }
                                                    } else if (result.jobstatus == 2) {
                                                        alert("failed to enable physical network. Error: " + _s(result.jobresult.errortext));
                                                    }
                                                }
                                            },
                                            error: function(XMLHttpResponse) {
                                                alert("failed to enable physical network. Error: " + parseXMLHttpResponse(XMLHttpResponse));
                                            }
                                        });
                                    }, g_queryAsyncJobResultInterval);
                                }
                            });
                        });
                    }
                },

                addNetscalerProvider: function(args) {
                    if (selectedNetworkOfferingHavingNetscaler == true) {
                        message(dictionary['message.adding.Netscaler.provider']);

                        $.ajax({
                            url: createURL("addNetworkServiceProvider&name=Netscaler&physicalnetworkid=" + args.data.returnedBasicPhysicalNetwork.id),
                            dataType: "json",
                            async: false,
                            success: function(json) {
                                var addNetscalerProviderIntervalID = setInterval(function() {
                                    $.ajax({
                                        url: createURL("queryAsyncJobResult&jobId=" + json.addnetworkserviceproviderresponse.jobid),
                                        dataType: "json",
                                        success: function(json) {
                                            var result = json.queryasyncjobresultresponse;
                                            if (result.jobstatus == 0) {
                                                return; //Job has not completed
                                            } else {
                                                clearInterval(addNetscalerProviderIntervalID);

                                                if (result.jobstatus == 1) {
                                                    args.data.returnedNetscalerProvider = result.jobresult.networkserviceprovider;
                                                    stepFns.addNetscalerDevice({
                                                        data: args.data
                                                    });
                                                } else if (result.jobstatus == 2) {
                                                    alert("addNetworkServiceProvider&name=Netscaler failed. Error: " + _s(result.jobresult.errortext));
                                                }
                                            }
                                        },
                                        error: function(XMLHttpResponse) {
                                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                            alert("addNetworkServiceProvider&name=Netscaler failed. Error: " + errorMsg);
                                        }
                                    });
                                }, g_queryAsyncJobResultInterval);
                            }
                        });
                        //add netscaler provider (end)
                    } else { //selectedNetworkOfferingHavingNetscaler == false
                        //create a guest network for basic zone
                        stepFns.addGuestNetwork({
                            data: args.data
                        });
                    }
                },


                addNetscalerDevice: function(args) {
                    message(dictionary['message.adding.Netscaler.device']);

                    var array1 = [];
                    array1.push("&physicalnetworkid=" + args.data.returnedBasicPhysicalNetwork.id);
                    array1.push("&username=" + todb(args.data.basicPhysicalNetwork.username));
                    array1.push("&password=" + todb(args.data.basicPhysicalNetwork.password));
                    array1.push("&networkdevicetype=" + todb(args.data.basicPhysicalNetwork.networkdevicetype));
                    array1.push("&gslbprovider=" + (args.data.basicPhysicalNetwork.gslbprovider == "on"));
                    array1.push("&gslbproviderpublicip=" + todb(args.data.basicPhysicalNetwork.gslbproviderpublicip));
                    array1.push("&gslbproviderprivateip=" + todb(args.data.basicPhysicalNetwork.gslbproviderprivateip));

                    //construct URL starts here
                    var url = [];

                    var ip = args.data.basicPhysicalNetwork.ip;
                    url.push("https://" + ip);

                    var isQuestionMarkAdded = false;

                    var publicInterface = args.data.basicPhysicalNetwork.publicinterface;
                    if (publicInterface != null && publicInterface.length > 0) {
                        if (isQuestionMarkAdded == false) {
                            url.push("?");
                            isQuestionMarkAdded = true;
                        } else {
                            url.push("&");
                        }
                        url.push("publicinterface=" + publicInterface);
                    }

                    var privateInterface = args.data.basicPhysicalNetwork.privateinterface;
                    if (privateInterface != null && privateInterface.length > 0) {
                        if (isQuestionMarkAdded == false) {
                            url.push("?");
                            isQuestionMarkAdded = true;
                        } else {
                            url.push("&");
                        }
                        url.push("privateinterface=" + privateInterface);
                    }

                    var numretries = args.data.basicPhysicalNetwork.numretries;
                    if (numretries != null && numretries.length > 0) {
                        if (isQuestionMarkAdded == false) {
                            url.push("?");
                            isQuestionMarkAdded = true;
                        } else {
                            url.push("&");
                        }
                        url.push("numretries=" + numretries);
                    }

                    var isInline = args.data.basicPhysicalNetwork.inline;
                    if (isInline != null && isInline.length > 0) {
                        if (isQuestionMarkAdded == false) {
                            url.push("?");
                            isQuestionMarkAdded = true;
                        } else {
                            url.push("&");
                        }
                        url.push("inline=" + isInline);
                    }

                    var capacity = args.data.basicPhysicalNetwork.capacity;
                    if (capacity != null && capacity.length > 0) {
                        if (isQuestionMarkAdded == false) {
                            url.push("?");
                            isQuestionMarkAdded = true;
                        } else {
                            url.push("&");
                        }
                        url.push("lbdevicecapacity=" + capacity);
                    }

                    var dedicated = (args.data.basicPhysicalNetwork.dedicated == "on"); //boolean	(true/false)
                    if (isQuestionMarkAdded == false) {
                        url.push("?");
                        isQuestionMarkAdded = true;
                    } else {
                        url.push("&");
                    }
                    url.push("lbdevicededicated=" + dedicated.toString());


                    array1.push("&url=" + todb(url.join("")));
                    //construct URL ends here

                    $.ajax({
                        url: createURL("addNetscalerLoadBalancer" + array1.join("")),
                        type: "POST",
                        dataType: "json",
                        success: function(json) {
                            var addNetscalerLoadBalancerIntervalID = setInterval(function() {
                                $.ajax({
                                    url: createURL("queryAsyncJobResult&jobid=" + json.addnetscalerloadbalancerresponse.jobid),
                                    dataType: "json",
                                    success: function(json) {
                                        var result = json.queryasyncjobresultresponse;
                                        if (result.jobstatus == 0) {
                                            return;
                                        } else {
                                            clearInterval(addNetscalerLoadBalancerIntervalID);

                                            if (result.jobstatus == 1) {
                                                args.data.returnedNetscalerProvider.returnedNetscalerloadbalancer = result.jobresult.netscalerloadbalancer;

                                                $.ajax({
                                                    url: createURL("updateNetworkServiceProvider&state=Enabled&id=" + args.data.returnedNetscalerProvider.id),
                                                    dataType: "json",
                                                    success: function(json) {
                                                        var enableNetscalerProviderIntervalID = setInterval(function() {
                                                            $.ajax({
                                                                url: createURL("queryAsyncJobResult&jobid=" + json.updatenetworkserviceproviderresponse.jobid),
                                                                dataType: "json",
                                                                success: function(json) {
                                                                    var result = json.queryasyncjobresultresponse;
                                                                    if (result.jobstatus == 0) {
                                                                        return;
                                                                    } else {
                                                                        clearInterval(enableNetscalerProviderIntervalID);

                                                                        if (result.jobstatus == 1) {
                                                                            stepFns.addGuestNetwork({
                                                                                data: args.data
                                                                            });
                                                                        } else if (result.jobstatus == 2) {
                                                                            alert("failed to enable Netscaler provider. Error: " + _s(result.jobresult.errortext));
                                                                        }
                                                                    }
                                                                }
                                                            });
                                                        }, g_queryAsyncJobResultInterval);
                                                    },
                                                    error: function(XMLHttpResponse) {
                                                        var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                        alert("failed to enable Netscaler provider. Error: " + errorMsg);
                                                    }
                                                });
                                            } else if (result.jobstatus == 2) { //addNetscalerLoadBalancer failed
                                                error('addNetscalerDevice', _s(result.jobresult.errortext), {
                                                    fn: 'addNetscalerDevice',
                                                    args: args
                                                });
                                            }
                                        }
                                    }
                                });
                            }, g_queryAsyncJobResultInterval);
                        },
                        error: function(XMLHttpResponse) {
                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                            error('addNetscalerDevice', errorMsg, {
                                fn: 'addNetscalerDevice',
                                args: args
                            });
                        }
                    });
                },

                addGuestNetwork: function(args) { //create a guest network for Basic zone or Advanced zone with SG
                    message(dictionary['message.creating.guest.network']);

                    var data = {
                        zoneid: args.data.returnedZone.id,
                        name: 'defaultGuestNetwork',
                        displaytext: 'defaultGuestNetwork',
                        networkofferingid: args.data.zone.networkOfferingId
                    };

                    //Advanced zone with SG
                    if (args.data.zone.networkType == "Advanced" && args.data.zone.sgEnabled == true) {
                        $.extend(data, {
                            gateway: args.data.guestTraffic.guestGateway,
                            netmask: args.data.guestTraffic.guestNetmask,
                            startip: args.data.guestTraffic.guestStartIp,
                            vlan: args.data.guestTraffic.vlanId
                        });
                        if (args.data.guestTraffic.guestEndIp != null && args.data.guestTraffic.guestEndIp.length > 0) {
                            $.extend(data, {
                                endip: args.data.guestTraffic.guestEndIp
                            });
                        }
                    }

                    $.ajax({
                        url: createURL('createNetwork'),
                        data: data,
                        async: false,
                        success: function(json) {
                            //basic zone has only one physical network => addPod() will be called only once => so don't need to double-check before calling addPod()
                            stepFns.addPod({
                                data: $.extend(args.data, {
                                    returnedGuestNetwork: json.createnetworkresponse.network
                                })
                            });
                        },
                        error: function(XMLHttpResponse) {
                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                            alert("failed to create a guest network for basic zone. Error: " + errorMsg);
                        }
                    });
                },

                addPod: function(args) {
                    message(dictionary['message.creating.pod']);

                    var array3 = [];
                    array3.push("&zoneId=" + args.data.returnedZone.id);
                    array3.push("&name=" + todb(args.data.pod.name));
                    array3.push("&gateway=" + todb(args.data.pod.reservedSystemGateway));
                    array3.push("&netmask=" + todb(args.data.pod.reservedSystemNetmask));
                    array3.push("&startIp=" + todb(args.data.pod.reservedSystemStartIp));

                    var endip = args.data.pod.reservedSystemEndIp; //optional
                    if (endip != null && endip.length > 0)
                        array3.push("&endIp=" + todb(endip));

                    $.ajax({
                        url: createURL("createPod" + array3.join("")),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                            stepFns.configurePublicTraffic({
                                data: $.extend(args.data, {
                                    returnedPod: json.createpodresponse.pod
                                })
                            });
                        },
                        error: function(XMLHttpResponse) {
                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                            error('addPod', errorMsg, {
                                fn: 'addPod',
                                args: args
                            });
                        }
                    });
                },

                configurePublicTraffic: function(args) {
                    if ((args.data.zone.networkType == "Basic" && (selectedNetworkOfferingHavingSG == true && selectedNetworkOfferingHavingEIP == true && selectedNetworkOfferingHavingELB == true)) || (args.data.zone.networkType == "Advanced" && args.data.zone.sgEnabled != true)) {

                        message(dictionary['message.configuring.public.traffic']);

                        var stopNow = false;

                        $(args.data.publicTraffic).each(function() {
                            var thisPublicVlanIpRange = this;

                            //check whether the VlanIpRange exists or not (begin)
                            var isExisting = false;
                            $(returnedPublicVlanIpRanges).each(function() {
                                if (this.vlan == thisPublicVlanIpRange.vlanid && this.startip == thisPublicVlanIpRange.startip && this.netmask == thisPublicVlanIpRange.netmask && this.gateway == thisPublicVlanIpRange.gateway) {
                                    isExisting = true;
                                    return false; //break each loop
                                }
                            });
                            if (isExisting == true)
                                return; //skip current item to next item (continue each loop)

                            //check whether the VlanIpRange exists or not (end)

                            var array1 = [];
                            array1.push("&zoneId=" + args.data.returnedZone.id);

                            if (this.vlanid != null && this.vlanid.length > 0)
                                array1.push("&vlan=" + todb(this.vlanid));
                            else
                                array1.push("&vlan=untagged");

                            array1.push("&gateway=" + this.gateway);
                            array1.push("&netmask=" + this.netmask);
                            array1.push("&startip=" + this.startip);
                            if (this.endip != null && this.endip.length > 0)
                                array1.push("&endip=" + this.endip);

                            if (args.data.zone.networkType == "Basic") {
                                array1.push("&forVirtualNetwork=true");
                            } else if (args.data.zone.networkType == "Advanced") {
                                if (args.data.zone.sgEnabled != true) {
                                    array1.push("&forVirtualNetwork=true");
                                } else { //args.data.zone.sgEnabled	== true
                                    array1.push("&forVirtualNetwork=false");
                                }
                            }

                            $.ajax({
                                url: createURL("createVlanIpRange" + array1.join("")),
                                dataType: "json",
                                async: false,
                                success: function(json) {
                                    var item = json.createvlaniprangeresponse.vlan;
                                    returnedPublicVlanIpRanges.push(item);
                                },
                                error: function(XMLHttpResponse) {
                                    var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                    error('configurePublicTraffic', errorMsg, {
                                        fn: 'configurePublicTraffic',
                                        args: args
                                    });
                                    stopNow = true;
                                }
                            });

                            if (stopNow == true)
                                return false; //break each loop, don't create next VlanIpRange
                        });

                        if (stopNow == true)
                            return; //stop the whole process

                        stepFns.configureStorageTraffic({
                            data: $.extend(args.data, {
                                returnedPublicTraffic: returnedPublicVlanIpRanges
                            })
                        });
                    } else if (args.data.zone.networkType == "Advanced" && args.data.zone.sgEnabled == true) { // Advanced SG-enabled zone doesn't have public traffic type
                        stepFns.configureStorageTraffic({
                            data: args.data
                        });
                    } else { //basic zone without public traffic type , skip to next step
                        if (data.physicalNetworks && $.inArray('storage', data.physicalNetworks[0].trafficTypes) > -1) {
                            stepFns.configureStorageTraffic({
                                data: args.data
                            });
                        } else {
                            stepFns.configureGuestTraffic({
                                data: args.data
                            });
                        }
                    }
                },

                configureStorageTraffic: function(args) {
                    var complete = function(data) {
                        stepFns.configureGuestTraffic({
                            data: $.extend(args.data, data)
                        });
                    };

                    var targetNetwork = $.grep(args.data.physicalNetworks, function(net) {
                        return $.inArray('storage', net.trafficTypes) > -1;
                    });

                    if (!targetNetwork.length) {
                        return complete({});
                    }

                    message(dictionary['message.configuring.storage.traffic']);

                    var storageIPRanges = args.data.storageTraffic;
                    var tasks = [];
                    var taskTimer;

                    $(storageIPRanges).each(function() {
                        var item = this;
                        if ('vlan' in item && (item.vlan == null || item.vlan.length == 0))
                            delete item.vlan;
                        $.ajax({
                            url: createURL('createStorageNetworkIpRange'),
                            data: $.extend(true, {}, item, {
                                zoneid: args.data.returnedZone.id,
                                podid: args.data.returnedPod.id
                            }),
                            success: function(json) {
                                tasks.push({
                                    jobid: json.createstoragenetworkiprangeresponse.jobid,
                                    complete: false
                                });
                            },
                            error: function(json) {
                                tasks.push({
                                    error: true,
                                    message: parseXMLHttpResponse(json)
                                });
                            }
                        });
                    });

                    taskTimer = setInterval(function() {
                        var completedTasks = $.grep(tasks, function(task) {
                            return task.complete || task.error;
                        });

                        var errorTasks = $.grep(tasks, function(task) {
                            return task.error;
                        });

                        if (completedTasks.length == storageIPRanges.length) {
                            clearInterval(taskTimer);

                            if (errorTasks.length) {
                                return error('configureStorageTraffic', errorTasks[0].message, {
                                    fn: 'configureStorageTraffic',
                                    args: args
                                });
                            }

                            return complete({});
                        }

                        if (tasks.length == storageIPRanges.length) {
                            $(tasks).each(function() {
                                var task = this;

                                if (task.error) return true;

                                pollAsyncJobResult({
                                    _custom: {
                                        jobId: task.jobid
                                    },
                                    complete: function() {
                                        task.complete = true;
                                    },
                                    error: function(args) {
                                        task.error = true;
                                        task.message = args.message;
                                    }
                                });

                                return true;
                            });
                        }

                        return true;
                    }, 1000);

                    return true;
                },

                configureGuestTraffic: function(args) {
                    if (skipGuestTrafficStep == true) {
                        stepFns.addCluster({
                            data: args.data
                        });
                        return;
                    }

                    message(dictionary['message.configuring.guest.traffic']);

                    if (args.data.returnedZone.networktype == "Basic") { //create an VlanIpRange for guest network in basic zone
                        var array1 = [];
                        array1.push("&podid=" + args.data.returnedPod.id);
                        array1.push("&networkid=" + args.data.returnedGuestNetwork.id);
                        array1.push("&gateway=" + args.data.guestTraffic.guestGateway);
                        array1.push("&netmask=" + args.data.guestTraffic.guestNetmask);
                        array1.push("&startip=" + args.data.guestTraffic.guestStartIp);
                        if (args.data.guestTraffic.guestEndIp != null && args.data.guestTraffic.guestEndIp.length > 0)
                            array1.push("&endip=" + args.data.guestTraffic.guestEndIp);
                        array1.push("&forVirtualNetwork=false"); //indicates this new IP range is for guest network, not public network

                        $.ajax({
                            url: createURL("createVlanIpRange" + array1.join("")),
                            dataType: "json",
                            success: function(json) {
                                args.data.returnedGuestNetwork.returnedVlanIpRange = json.createvlaniprangeresponse.vlan;

                                if (args.data.zone.hypervisor == "BareMetal") { //if hypervisor is BareMetal, zone creation is completed at this point.
                                    complete({
                                        data: args.data
                                    });
                                } else {
                                    stepFns.addCluster({
                                        data: args.data
                                    });
                                }
                            },
                            error: function(XMLHttpResponse) {
                                var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                error('configureGuestTraffic', errorMsg, {
                                    fn: 'configureGuestTraffic',
                                    args: args
                                });
                            }
                        });
                    } else if (args.data.returnedZone.networktype == "Advanced") { //update VLAN in physical network(s) in advanced zone
                        var physicalNetworksHavingGuestIncludingVlan = [];
                        $(args.data.physicalNetworks).each(function() {
                            if (this.guestConfiguration != null && this.guestConfiguration.vlanRangeStart != null && this.guestConfiguration.vlanRangeStart.length > 0) {
                                physicalNetworksHavingGuestIncludingVlan.push(this);
                            }
                        });

                        if (physicalNetworksHavingGuestIncludingVlan.length == 0) {
                            stepFns.addCluster({
                                data: args.data
                            });
                        } else {
                            var updatedCount = 0;
                            $(physicalNetworksHavingGuestIncludingVlan).each(function() {
                                var vlan;
                                if (this.guestConfiguration.vlanRangeEnd == null || this.guestConfiguration.vlanRangeEnd.length == 0)
                                    vlan = this.guestConfiguration.vlanRangeStart;
                                else
                                    vlan = this.guestConfiguration.vlanRangeStart + "-" + this.guestConfiguration.vlanRangeEnd;

                                var originalId = this.id;
                                var returnedId;
                                $(args.data.returnedPhysicalNetworks).each(function() {
                                    if (this.originalId == originalId) {
                                        returnedId = this.id;
                                        return false; //break the loop
                                    }
                                });

                                $.ajax({
                                    url: createURL("updatePhysicalNetwork&id=" + returnedId + "&vlan=" + todb(vlan)),
                                    dataType: "json",
                                    success: function(json) {
                                        var jobId = json.updatephysicalnetworkresponse.jobid;
                                        var updatePhysicalNetworkVlanIntervalID = setInterval(function() {
                                            $.ajax({
                                                url: createURL("queryAsyncJobResult&jobid=" + jobId),
                                                dataType: "json",
                                                success: function(json) {
                                                    var result = json.queryasyncjobresultresponse;
                                                    if (result.jobstatus == 0) {
                                                        return;
                                                    } else {
                                                        clearInterval(updatePhysicalNetworkVlanIntervalID);

                                                        if (result.jobstatus == 1) {
                                                            updatedCount++;
                                                            if (updatedCount == physicalNetworksHavingGuestIncludingVlan.length) {
                                                                stepFns.addCluster({
                                                                    data: args.data
                                                                });
                                                            }
                                                        } else if (result.jobstatus == 2) {
                                                            alert("error: " + _s(result.jobresult.errortext));
                                                            error('configureGuestTraffic', result.jobresult.errortext, {
                                                                fn: 'configureGuestTraffic',
                                                                args: args
                                                            });

                                                        }
                                                    }
                                                },
                                                error: function(XMLHttpResponse) {
                                                    var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                    error('configureGuestTraffic', errorMsg, {
                                                        fn: 'configureGuestTraffic',
                                                        args: args
                                                    });
                                                }
                                            });
                                        }, g_queryAsyncJobResultInterval);
                                    }
                                });
                            });
                        }
                    }
                },

                addCluster: function(args) {
                    message(dictionary['message.creating.cluster']);

                    // Have cluster use zone's hypervisor
                    args.data.cluster.hypervisor = args.data.zone.hypervisor ?
                        args.data.zone.hypervisor : args.data.cluster.hypervisor;

                    var array1 = [];
                    array1.push("&zoneId=" + args.data.returnedZone.id);
                    array1.push("&hypervisor=" + args.data.cluster.hypervisor);

                    var clusterType;
                    if (args.data.cluster.hypervisor == "VMware")
                        clusterType = "ExternalManaged";
                    else
                        clusterType = "CloudManaged";
                    array1.push("&clustertype=" + clusterType);

                    array1.push("&podId=" + args.data.returnedPod.id);

                    var clusterName = args.data.cluster.name;

                    if (args.data.cluster.hypervisor == "VMware") {
                        array1.push("&username=" + todb(args.data.cluster.vCenterUsername));
                        array1.push("&password=" + todb(args.data.cluster.vCenterPassword));

                        //dvswitch is enabled
						/*
                        if (args.data.cluster.vSwitchPublicType != "")
                            array1.push('&publicvswitchtype=' + args.data.cluster.vSwitchPublicType);
						*/

						/*
                        if (args.data.cluster.vSwitchPublicName != "")
                            array1.push("&publicvswitchname=" + args.data.cluster.vSwitchPublicName);
                        */

                        /*
                        if (args.data.cluster.vSwitchGuestType != "")
                            array1.push('&guestvswitchtype=' + args.data.cluster.vSwitchGuestType);
                        */
						
						/*
                        if (args.data.cluster.vSwitchGuestName != "")
                            array1.push("&guestvswitchname=" + args.data.cluster.vSwitchGuestName);
						*/

                        if (args.data.cluster.vsmipaddress != null && args.data.cluster.vsmipaddress.length > 0) { 
                            array1.push('&vsmipaddress=' + args.data.cluster.vsmipaddress);
                        }
                        
                        if(args.data.cluster.vsmusername != null && args.data.cluster.vsmusername.length > 0) {
                        	array1.push('&vsmusername=' + args.data.cluster.vsmusername);
                        }
                          
                        if(args.data.cluster.vsmpassword != null && args.data.cluster.vsmpassword.length > 0) {
                        	array1.push('&vsmpassword=' + args.data.cluster.vsmpassword);
                        }  

                        var hostname = args.data.cluster.vCenterHost;
                        var dcName = args.data.cluster.vCenterDatacenter;

                        var url;
                        if (hostname.indexOf("http://") == -1)
                            url = "http://" + hostname;
                        else
                            url = hostname;
                        url += "/" + dcName + "/" + clusterName;
                        array1.push("&url=" + todb(url));

                        clusterName = hostname + "/" + dcName + "/" + clusterName; //override clusterName
                    }
                    array1.push("&clustername=" + todb(clusterName));

                    if (args.data.cluster.hypervisor == "VMware") {
                        var vmwareData = {
                            zoneId: args.data.returnedZone.id,
                            username: args.data.cluster.vCenterUsername,
                            password: args.data.cluster.vCenterPassword,
                            name: args.data.cluster.vCenterDatacenter,
                            vcenter: args.data.cluster.vCenterHost
                        };
                        $.ajax({
                            url: createURL('addVmwareDc'),
                            data: vmwareData,
                            type: "POST",
                            success: function(json) {
                                var item = json.addvmwaredcresponse.vmwaredc;
                                if (item.id != null) {
                                    $.ajax({
                                        url: createURL("addCluster" + array1.join("")),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            stepFns.addPrimaryStorage({ //skip "add host step" when hypervisor is VMware
                                                data: $.extend(args.data, {
                                                    returnedCluster: json.addclusterresponse.cluster[0]
                                                })
                                            });
                                        },
                                        error: function(XMLHttpResponse) {
                                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                            error('addCluster', errorMsg, {
                                                fn: 'addCluster',
                                                args: args
                                            });
                                        }
                                    });
                                }
                            },

                            error: function(XMLHttpResponse) {
                                var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                error('addCluster', errorMsg, {
                                    fn: 'addCluster',
                                    args: args
                                });
                            }

                        });
                    } else {
                        $.ajax({
                            url: createURL("addCluster" + array1.join("")),
                            dataType: "json",
                            type: "POST",                            
                            success: function(json) {
                                stepFns.addHost({
                                    data: $.extend(args.data, {
                                        returnedCluster: json.addclusterresponse.cluster[0]
                                    })
                                });
                            },
                            error: function(XMLHttpResponse) {
                                var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                error('addCluster', errorMsg, {
                                    fn: 'addCluster',
                                    args: args
                                });
                            }
                        });
                    }
                },

                addHost: function(args) {
                    message(dictionary['message.adding.host']);

                    var data = {
                        zoneid: args.data.returnedZone.id,
                        podid: args.data.returnedPod.id,
                        clusterid: args.data.returnedCluster.id,
                        hypervisor: args.data.returnedCluster.hypervisortype,
                        clustertype: args.data.returnedCluster.clustertype,
                        hosttags: args.data.host.hosttags,
                        username: args.data.host.username,
                        password: args.data.host.password
                    };

                    var hostname = args.data.host.hostname;
                    var url;
                    if (hostname.indexOf("http://") == -1) {
                        url = "http://" + hostname;
                    } else {
                        url = hostname;
                    }
                    $.extend(data, {
                        url: url
                    });

                    if (args.data.cluster.hypervisor == "BareMetal") {
                        $.extend(data, {
                            cpunumber: args.data.host.baremetalCpuCores,
                            cpuspeed: args.data.host.baremetalCpu,
                            memory: args.data.host.baremetalMemory,
                            hostmac: args.data.host.baremetalMAC
                        });
                    } else if (args.data.cluster.hypervisor == "Ovm") {
                        $.extend(data, {
                            agentusername: args.data.host.agentUsername,
                            agentpassword: args.data.host.agentPassword
                        });
                    }

                    $.ajax({
                        url: createURL("addHost"),
                        type: "POST",
                        data: data,
                        success: function(json) {
                            stepFns.addPrimaryStorage({
                                data: $.extend(args.data, {
                                    returnedHost: json.addhostresponse.host[0]
                                })
                            });
                        },
                        error: function(XMLHttpResponse) {
                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                            error('addHost', errorMsg, {
                                fn: 'addHost',
                                args: args
                            });
                        }
                    });
                },

                addPrimaryStorage: function(args) {
                    if (args.data.zone.localstorageenabled == 'on') { //use local storage, don't need primary storage. So, skip this step.
                        stepFns.addSecondaryStorage({
                            data: args.data
                        });
                        return;
                    }

                    message(dictionary['message.creating.primary.storage']);

                    var array1 = [];
                    array1.push("&zoneid=" + args.data.returnedZone.id);
                    array1.push("&podId=" + args.data.returnedPod.id);
                    array1.push("&clusterid=" + args.data.returnedCluster.id);
                    array1.push("&name=" + todb(args.data.primaryStorage.name));
                    array1.push("&scope=" + todb(args.data.primaryStorage.scope));

                    //zone-wide-primary-storage is supported only for KVM and VMWare
                    if (args.data.primaryStorage.scope == "zone") {
                        array1.push("&hypervisor=" + todb(args.data.cluster.hypervisor)); //hypervisor type of the hosts in zone that will be attached to this storage pool. KVM, VMware supported as of now.
                    }

                    var server = args.data.primaryStorage.server;
                    var url = null;
                    if (args.data.primaryStorage.protocol == "nfs") {
                        //var path = trim($thisDialog.find("#add_pool_path").val());
                        var path = args.data.primaryStorage.path;

                        if (path.substring(0, 1) != "/")
                            path = "/" + path;
                        url = nfsURL(server, path);
                    } else if (args.data.primaryStorage.protocol == "PreSetup") {
                        //var path = trim($thisDialog.find("#add_pool_path").val());
                        var path = args.data.primaryStorage.path;

                        if (path.substring(0, 1) != "/")
                            path = "/" + path;
                        url = presetupURL(server, path);
                    } else if (args.data.primaryStorage.protocol == "ocfs2") {
                        //var path = trim($thisDialog.find("#add_pool_path").val());
                        var path = args.data.primaryStorage.path;

                        if (path.substring(0, 1) != "/")
                            path = "/" + path;
                        url = ocfs2URL(server, path);
                    } else if (args.data.primaryStorage.protocol == "SharedMountPoint") {
                        //var path = trim($thisDialog.find("#add_pool_path").val());
                        var path = args.data.primaryStorage.path;

                        if (path.substring(0, 1) != "/")
                            path = "/" + path;
                        url = SharedMountPointURL(server, path);
                    } else if (args.data.primaryStorage.protocol == "clvm") {
                        //var vg = trim($thisDialog.find("#add_pool_clvm_vg").val());
                        var vg = args.data.primaryStorage.volumegroup;

                        if (vg.substring(0, 1) != "/")
                            vg = "/" + vg;
                        url = clvmURL(vg);
                    } else if (args.data.primaryStorage.protocol == "vmfs") {
                        //var path = trim($thisDialog.find("#add_pool_vmfs_dc").val());
                        var path = args.data.primaryStorage.vCenterDataCenter;

                        if (path.substring(0, 1) != "/")
                            path = "/" + path;
                        path += "/" + args.data.primaryStorage.vCenterDataStore;
                        url = vmfsURL("dummy", path);
                    } else {
                        //var iqn = trim($thisDialog.find("#add_pool_iqn").val());
                        var iqn = args.data.primaryStorage.iqn;

                        if (iqn.substring(0, 1) != "/")
                            iqn = "/" + iqn;
                        var lun = args.data.primaryStorage.lun;
                        url = iscsiURL(server, iqn, lun);
                    }
                    array1.push("&url=" + todb(url));

                    if (args.data.primaryStorage.storageTags != null && args.data.primaryStorage.storageTags.length > 0)
                        array1.push("&tags=" + todb(args.data.primaryStorage.storageTags));

                    $.ajax({
                        url: createURL("createStoragePool" + array1.join("")),
                        dataType: "json",
                        success: function(json) {
                            stepFns.addSecondaryStorage({
                                data: $.extend(args.data, {
                                    returnedPrimaryStorage: json.createstoragepoolresponse.storagepool
                                })
                            });
                        },
                        error: function(XMLHttpResponse) {
                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                            error('addPrimaryStorage', errorMsg, {
                                fn: 'addPrimaryStorage',
                                args: args
                            });
                        }
                    });
                },

                addSecondaryStorage: function(args) {
                	if (args.data.secondaryStorage.provider == '') {
                        complete({
                            data: args.data
                        });                        
                        return; //skip addSecondaryStorage if provider dropdown is blank
                	}
                	
                	
                	message(dictionary['message.creating.secondary.storage']);

                    var data = {};
                    if (args.data.secondaryStorage.name != null && args.data.secondaryStorage.name.length > 0) {
                        $.extend(data, {
                            name: args.data.secondaryStorage.name
                        });
                    }

                    if (args.data.secondaryStorage.provider == 'NFS') {
                        var nfs_server = args.data.secondaryStorage.nfsServer;
                        var path = args.data.secondaryStorage.path;
                        var url = nfsURL(nfs_server, path);

                        $.extend(data, {
                            provider: 'NFS',
                            zoneid: args.data.returnedZone.id,
                            url: url
                        });

                        $.ajax({
                            url: createURL('addImageStore'),
                            data: data,
                            success: function(json) {
                                complete({
                                    data: $.extend(args.data, {
                                        returnedSecondaryStorage: json.addimagestoreresponse.secondarystorage
                                    })
                                });
                            },
                            error: function(XMLHttpResponse) {
                                var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                error('addSecondaryStorage', errorMsg, {
                                    fn: 'addSecondaryStorage',
                                    args: args
                                });
                            }
                        });
                    } else if (args.data.secondaryStorage.provider == 'S3') {           	               	                	
                    	if($wizard.find('form[rel=secondaryStorage]').find('div[rel=name]').find('input').attr("disabled") == "disabled") { //Name textbox is disabled (and populated with S3 image setore name) when S3 image store exists. In this case, do not call addImageStore to create S3 image store.                    		
                            complete({
                                data: args.data
                            });        		
                    	} else { //Name textbox is not disabled when S3 image store does not exist. In this case, call addImageStore to create S3 image store.                    	
                    		$.extend(data, {
                                provider: args.data.secondaryStorage.provider,
                                'details[0].key': 'accesskey',
                                'details[0].value': args.data.secondaryStorage.accesskey,
                                'details[1].key': 'secretkey',
                                'details[1].value': args.data.secondaryStorage.secretkey,
                                'details[2].key': 'bucket',
                                'details[2].value': args.data.secondaryStorage.bucket,
                                'details[3].key': 'usehttps',
                                'details[3].value': (args.data.secondaryStorage.usehttps != null && args.data.secondaryStorage.usehttps == 'on' ? 'true' : 'false')
                            });

                            var index = 4;
                            if (args.data.secondaryStorage.endpoint != null && args.data.secondaryStorage.endpoint.length > 0) {
                                data['details[' + index.toString() + '].key'] = 'endpoint';
                                data['details[' + index.toString() + '].value'] = args.data.secondaryStorage.endpoint;
                                index++;
                            }
                            if (args.data.secondaryStorage.connectiontimeout != null && args.data.secondaryStorage.connectiontimeout.length > 0) {
                                data['details[' + index.toString() + '].key'] = 'connectiontimeout';
                                data['details[' + index.toString() + '].value'] = args.data.secondaryStorage.connectiontimeout;
                                index++;
                            }
                            if (args.data.secondaryStorage.maxerrorretry != null && args.data.secondaryStorage.maxerrorretry.length > 0) {
                                data['details[' + index.toString() + '].key'] = 'maxerrorretry';
                                data['details[' + index.toString() + '].value'] = args.data.secondaryStorage.maxerrorretry;
                                index++;
                            }
                            if (args.data.secondaryStorage.sockettimeout != null && args.data.secondaryStorage.sockettimeout.length > 0) {
                                data['details[' + index.toString() + '].key'] = 'sockettimeout';
                                data['details[' + index.toString() + '].value'] = args.data.secondaryStorage.sockettimeout;
                                index++;
                            }
                            $.ajax({
                                url: createURL('addImageStore'),
                                data: data,
                                success: function(json) {   
                                	g_regionsecondaryenabled = true;
                                	
                                    complete({
                                        data: $.extend(args.data, {
                                            returnedSecondaryStorage: json.addimagestoreresponse.secondarystorage
                                        })
                                    });
                                },
                                error: function(XMLHttpResponse) {
                                    var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                    error('addSecondaryStorage', errorMsg, {
                                        fn: 'addSecondaryStorage',
                                        args: args
                                    });
                                }
                            });                 
                    	}
                    	                     
                        //NFS Cache
                        if ($wizard.find('form[rel=secondaryStorage]').find('div[rel=createNfsCache]').find("input[type=checkbox]").is(':checked') == true) {
                            var zoneid = args.data.secondaryStorage.nfsCacheZoneid;
                            var nfs_server = args.data.secondaryStorage.nfsCacheNfsServer;
                            var path = args.data.secondaryStorage.nfsCachePath;
                            var url = nfsURL(nfs_server, path);

                            var nfsCacheData = {
                                provider: 'NFS',
                                zoneid: args.data.returnedZone.id,
                                url: url
                            };

                            $.ajax({
                                url: createURL('createSecondaryStagingStore'),
                                data: nfsCacheData,
                                success: function(json) {
                                    //do nothing
                                },
                                error: function(XMLHttpResponse) {
                                    var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                    error('addSecondaryStorage', errorMsg, {
                                        fn: 'addSecondaryStorage',
                                        args: args
                                    });
                                }
                            });
                        }
                    } else if (args.data.secondaryStorage.provider == 'Swift') {
                        $.extend(data, {
                            provider: args.data.secondaryStorage.provider,
                            url: args.data.secondaryStorage.url
                        });

                        var index = 0;
                        if (args.data.secondaryStorage.account != null && args.data.secondaryStorage.account.length > 0) {
                            data['details[' + index.toString() + '].key'] = 'account';
                            data['details[' + index.toString() + '].value'] = args.data.secondaryStorage.account;
                            index++;
                        }
                        if (args.data.secondaryStorage.username != null && args.data.secondaryStorage.username.length > 0) {
                            data['details[' + index.toString() + '].key'] = 'username';
                            data['details[' + index.toString() + '].value'] = args.data.secondaryStorage.username;
                            index++;
                        }
                        if (args.data.secondaryStorage.key != null && args.data.secondaryStorage.key.length > 0) {
                            data['details[' + index.toString() + '].key'] = 'key';
                            data['details[' + index.toString() + '].value'] = args.data.secondaryStorage.key;
                            index++;
                        }
                        $.ajax({
                            url: createURL('addImageStore'),
                            data: data,
                            success: function(json) {  
                            	g_regionsecondaryenabled = true;
                            	
                                complete({
                                    data: $.extend(args.data, {
                                        returnedSecondaryStorage: json.addimagestoreresponse.secondarystorage
                                    })
                                });                                
                            },
                            error: function(XMLHttpResponse) {
                                var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                error('addSecondaryStorage', errorMsg, {
                                    fn: 'addSecondaryStorage',
                                    args: args
                                });
                            }
                        });
                    }
                }                
            };

            var complete = function(args) {
                message(dictionary['message.Zone.creation.complete']);
                success(args);
            };

            if (startFn) {
                stepFns[startFn.fn]({
                    data: $.extend(startFn.args.data, data)
                });
            } else {
                stepFns.addZone({});
            }
        },

        enableZoneAction: function(args) {
            $.ajax({
                url: createURL("updateZone&allocationstate=Enabled&id=" + args.launchData.returnedZone.id),
                dataType: "json",
                success: function(json) {
                    args.formData.returnedZone = json.updatezoneresponse.zone;
                    args.response.success();
                }
            });
        }
    };
}(cloudStack, jQuery));
