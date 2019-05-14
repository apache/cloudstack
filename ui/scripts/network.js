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
    var ingressEgressDataMap = function(elem) {
        var elemData = {
            id: elem.ruleid,
            protocol: elem.protocol,
            startport: elem.startport,
            endport: elem.endport,
            cidr: elem.cidr ? elem.cidr : ''.concat(elem.account, ' - ', elem.securitygroupname),
            tags: elem.tags
        };

        if (typeof elem.icmptype != 'undefined') {
             var icmptype = elem.icmptype.toString()
        }

        if (typeof elem.icmpcode != 'undefined') {
             var icmpcode = elem.icmpcode.toString()
        }

        if (elemData.startport == 0 && elemData.endport) {
            elemData.startport = '0';
        } else if (icmptype && icmpcode) {
            elemData.startport = icmptype;
            elemData.endport = icmpcode;
        }

        return elemData;
    };

    //value of Primary IP in subselect dropdown is -1, for single VM selection (API parameter virtualmachineid + vmguestip), e.g. enableStaticNat API, createPortForwardingRule API.
    var singleVmSecondaryIPSubselect = function(args) {
        var instance = args.context.instances[0];
        var network = args.context.networks[0];

        if (args.context.ipAddresses[0].isportable) { //portable IP which has multiple NICs. Each NIC has a different network ID.
            $.ajax({
                url: createURL('listNics'),
                data: {
                    virtualmachineid: instance.id
                },
                success: function(json) {
                    var nics = json.listnicsresponse.nic;
                    var ipSelection = [];

                    $(nics).map(function(index, nic) {
                        var primaryIp = nic.ipaddress;
                        var secondaryIps = nic.secondaryip ? nic.secondaryip : [];
                        var prefix = '[NIC ' + (index + 1) + '] ';

                        // Add primary IP as default
                        ipSelection.push({
                            id: nic.networkid + ',-1',
                            description: prefix + primaryIp + ' (Primary)'
                        });

                        // Add secondary IPs
                        $(secondaryIps).map(function(index, secondaryIp) {
                            ipSelection.push({
                                id: nic.networkid + ',' + secondaryIp.ipaddress,
                                description: prefix + secondaryIp.ipaddress
                            });
                        });
                    });

                    args.response.success({
                        data: ipSelection
                    });
                }
            });

        } else { //non-portable IP which has only one NIC
            // Get NIC IPs
            $.ajax({
                url: createURL('listNics'),
                data: {
                    virtualmachineid: instance.id,
                    networkId: network.id
                },
                success: function(json) {
                    var nic = json.listnicsresponse.nic[0];
                    var primaryIp = nic.ipaddress;
                    var secondaryIps = nic.secondaryip ? nic.secondaryip : [];
                    var ipSelection = [];

                    // Add primary IP as default
                    ipSelection.push({
                        id: primaryIp,
                        description: primaryIp + ' (Primary)'
                    });

                    // Add secondary IPs
                    $(secondaryIps).map(function(index, secondaryIp) {
                        ipSelection.push({
                            id: secondaryIp.ipaddress,
                            description: secondaryIp.ipaddress
                        });
                    });

                    args.response.success({
                        data: ipSelection
                    });
                }
            });
        }
    };

    //value of Primary IP in subselect dropdown is itself (not -1), for multiple VM selection (API parameter vmidipmap), e.g. assignToLoadBalancerRule API.
    var multipleVmSecondaryIPSubselect = function(args) {
        var instance = args.context.instances[0];
        var network = args.context.networks[0];

        if (args.context.ipAddresses[0].isportable) { //portable IP which has multiple NICs. Each NIC has a different network ID.
            $.ajax({
                url: createURL('listNics'),
                data: {
                    virtualmachineid: instance.id
                },
                success: function(json) {
                    var nics = json.listnicsresponse.nic;
                    var ipSelection = [];

                    //portable IP has multiple NICs. Each NIC has a different network ID.
                    $(nics).map(function(index, nic) {
                        var primaryIp = nic.ipaddress;
                        var secondaryIps = nic.secondaryip ? nic.secondaryip : [];
                        var prefix = '[NIC ' + (index + 1) + '] ';

                        // Add primary IP as default
                        ipSelection.push({
                            id: nic.networkid + ',' + primaryIp,
                            description: prefix + primaryIp + ' (Primary)'
                        });

                        // Add secondary IPs
                        $(secondaryIps).map(function(index, secondaryIp) {
                            ipSelection.push({
                                id: nic.networkid + ',' + secondaryIp.ipaddress,
                                description: prefix + secondaryIp.ipaddress
                            });
                        });
                    });

                    args.response.success({
                        data: ipSelection
                    });
                }
            });

        } else { //non-portable IP which has only one NIC
            // Get NIC IPs
            $.ajax({
                url: createURL('listNics'),
                data: {
                    virtualmachineid: instance.id,
                    networkid: network.id
                },
                success: function(json) {
                    var nic = json.listnicsresponse.nic[0];
                    var primaryIp = nic.ipaddress;
                    var secondaryIps = nic.secondaryip ? nic.secondaryip : [];
                    var ipSelection = [];
                    var existingIps = $(args.context.subItemData).map(
                        function(index, item) { return item.itemIp; }
                    );

                    // Add primary IP as default
                    if ($.inArray(primaryIp, existingIps) == -1) {
                        ipSelection.push({
                            id: primaryIp,
                            description: primaryIp + ' (Primary)'
                        });
                    }

                    // Add secondary IPs
                    $(secondaryIps).map(function(index, secondaryIp) {
                        if ($.inArray(secondaryIp.ipaddress, existingIps) == -1) {
                            ipSelection.push({
                                id: secondaryIp.ipaddress,
                                description: secondaryIp.ipaddress
                            });
                        }
                    });

                    args.response.success({
                        data: ipSelection
                    });
                }
            });
        }
    };

    var ipChangeNotice = function() {
        cloudStack.dialog.confirm({
            message: 'message.ip.address.changed',
            action: function() {
                $('#browser .container').cloudBrowser('selectPanel', {
                    panel: $('#browser .panel:last').prev(),
                    complete: function() {
                        $(window).trigger('cloudStack.fullRefresh');
                    }
                });
            }
        });
    };

    var zoneObjs = [];

    var actionFilters = {
        ipAddress: function(args) {
            var allowedActions = args.context.actions;
            var disallowedActions = [];
            var ipObj = args.context.item;
            var status = ipObj.state;

            //***** apply to both Isolated Guest Network IP, VPC IP (begin) *****
            if (status == 'Destroyed' ||
                status == 'Releasing' ||
                status == 'Released' ||
                status == 'Creating' ||
                status == 'Allocating' ||
                ipObj.account == 'system' ||
                ipObj.issystem == true) {
                return [];
            }

            if (ipObj.issourcenat) { //sourceNAT IP doesn't support staticNAT
                disallowedActions.push('enableStaticNAT');
                disallowedActions.push('disableStaticNAT');
                disallowedActions.push('remove');
            } else { //non-sourceNAT IP supports staticNAT
                disallowedActions.push('enableVPN');
                if (ipObj.isstaticnat) {
                    disallowedActions.push('enableStaticNAT');
                } else {
                    disallowedActions.push('disableStaticNAT');
                }
            }
            //***** apply to both Isolated Guest Network IP, VPC IP (end) *****


            if (!('vpc' in args.context)) { //***** Guest Network section > Guest Network page > IP Address page *****
                if (args.context.networks[0].networkofferingconservemode == false) {
                    /*
                     (1) If IP is SourceNat, no StaticNat/VPN/PortForwarding/LoadBalancer can be enabled/added.
                     */
                    if (ipObj.issourcenat == true) {
                        disallowedActions.push('enableStaticNAT');
                        disallowedActions.push('enableVPN');
                    }

                    /*
                     (2) If IP is non-SourceNat, show StaticNat/VPN/PortForwarding/LoadBalancer at first.
                     1. Once StaticNat is enabled, hide VPN/PortForwarding/LoadBalancer.
                     2. Once VPN is enabled, hide StaticNat/PortForwarding/LoadBalancer.
                     3. Once a PortForwarding rule is added, hide StaticNat/VPN/LoadBalancer.
                     4. Once a LoadBalancer rule is added, hide StaticNat/VPN/PortForwarding.
                     */
                    else { //ipObj.issourcenat == false
                        if (ipObj.isstaticnat) { //1. Once StaticNat is enabled, hide VPN/PortForwarding/LoadBalancer.
                            disallowedActions.push('enableVPN');
                        }
                        if (ipObj.vpnenabled) { //2. Once VPN is enabled, hide StaticNat/PortForwarding/LoadBalancer.
                            disallowedActions.push('enableStaticNAT');
                        }

                        //3. Once a PortForwarding rule is added, hide StaticNat/VPN/LoadBalancer.
                        $.ajax({
                            url: createURL('listPortForwardingRules'),
                            data: {
                                ipaddressid: ipObj.id,
                                listAll: true
                            },
                            dataType: 'json',
                            async: false,
                            success: function(json) {
                                var rules = json.listportforwardingrulesresponse.portforwardingrule;
                                if (rules != null && rules.length > 0) {
                                    disallowedActions.push('enableVPN');
                                    disallowedActions.push('enableStaticNAT');
                                }
                            }
                        });

                        //4. Once a LoadBalancer rule is added, hide StaticNat/VPN/PortForwarding.
                        $.ajax({
                            url: createURL('listLoadBalancerRules'),
                            data: {
                                publicipid: ipObj.id,
                                listAll: true
                            },
                            dataType: 'json',
                            async: false,
                            success: function(json) {
                                var rules = json.listloadbalancerrulesresponse.loadbalancerrule;
                                if (rules != null && rules.length > 0) {
                                    disallowedActions.push('enableVPN');
                                    disallowedActions.push('enableStaticNAT');
                                }
                            }
                        });
                    }
                }

                if (ipObj.networkOfferingHavingVpnService == true) {
                    if (ipObj.vpnenabled) {
                        disallowedActions.push('enableVPN');
                    } else {
                        disallowedActions.push('disableVPN');
                    }
                } else { //ipObj.networkOfferingHavingVpnService == false
                    disallowedActions.push('disableVPN');
                    disallowedActions.push('enableVPN');
                }
            } else { //***** VPC section > Configuration VPC > Router > Public IP Addresses *****
                if (ipObj.issourcenat) { //VPC sourceNAT IP: supports VPN
                    if (ipObj.vpnenabled) {
                        disallowedActions.push('enableVPN');
                    } else {
                        disallowedActions.push('disableVPN');
                    }
                } else { //VPC non-sourceNAT IP: doesn't support VPN
                    disallowedActions.push('enableVPN');
                    disallowedActions.push('disableVPN');
                }
            }

            allowedActions = $.grep(allowedActions, function(item) {
                return $.inArray(item, disallowedActions) == -1;
            });

            return allowedActions;
        },

        securityGroups: function(args) {
            var allowedActions = [];
            var isSecurityGroupOwner = isAdmin() || isDomainAdmin() ||
                args.context.item.account == args.context.users[0].account;

            if (isSecurityGroupOwner &&
                args.context.item.state != 'Destroyed' &&
                args.context.item.name != 'default') {
                allowedActions.push('remove');
            }

            return allowedActions;
        }
    };

    var networkOfferingObjs = [];
    var advZoneObjs;

    cloudStack.sections.network = {
        title: 'label.network',
        id: 'network',
        sectionSelect: {
            preFilter: function(args) {
                var sectionsToShow = ['networks'];
                var securityGroupsEnabledFound = false; //Until we found a zone where securitygroupsenabled is true.

                //This call to show VPC and VPN Customer Gateway sections, if zone is advanced.
                $.ajax({
                    url: createURL('listZones'),
                    data: {
                        networktype: 'Advanced'
                    },
                    async: false,
                    success: function(json) {
                        advZoneObjs = json.listzonesresponse ? json.listzonesresponse.zone : null;
                        if (advZoneObjs != null && advZoneObjs.length > 0) {
                            sectionsToShow.push('vpc');
                            sectionsToShow.push('vpnCustomerGateway');

                            //At the same time check if any advanced zone has securitygroupsenabled is true.
                            //If so, show Security Group section.
                            for (var i = 0; (i < advZoneObjs.length) && !securityGroupsEnabledFound; i++) {
                                if (advZoneObjs[i].securitygroupsenabled) {
                                    securityGroupsEnabledFound = true;
                                    sectionsToShow.push('securityGroups');
                                }
                            }
                        }
                        //Ajax call to check if VPN is enabled.
                        $.ajax({
                            url: createURL('listRemoteAccessVpns'),
                            data: {
                                listAll: true
                            },
                            async: false,
                            success: function(vpnResponse) {
                                var isVPNEnabled = vpnResponse.listremoteaccessvpnsresponse.count;

                                if (isVPNEnabled) {
                                    sectionsToShow.push('vpnuser');
                                }
                            }
                        });
                    }
                });

                //If we didn't find any advanced zone whose securitygroupsenabled is true.
                //Search in all Basic zones.
                if (!securityGroupsEnabledFound) {
                    $.ajax({
                        url: createURL('listZones'),
                        data: {
                            networktype: 'Basic'
                        },
                        async: false,
                        success: function(json) {
                            var basicZoneObjs = json.listzonesresponse ? json.listzonesresponse.zone : null;
                            if (basicZoneObjs != null && basicZoneObjs.length > 0) {
                                sectionsToShow.push('securityGroups');
                            }
                        }
                    });
                }

                return sectionsToShow;
            },

            label: 'label.select-view'
        },
        sections: {
            networks: {
                id: 'networks',
                type: 'select',
                title: 'label.guest.networks',
                listView: {
                    actions: {
                        add: {
                            label: 'label.add.isolated.network',

                            preFilter: function(args) {
                                if (advZoneObjs != null && advZoneObjs.length > 0) {
                                    for (var i = 0; i < advZoneObjs.length; i++) {
                                        if (advZoneObjs[i].securitygroupsenabled != true) { //'Add Isolated Guest Network with SourceNat' is only supported in Advanced SG-disabled zone
                                            return true;
                                        }
                                    }
                                    return false;
                                } else {
                                    return false;
                                }
                            },

                            createForm: {
                                title: 'label.add.isolated.guest.network.with.sourcenat',
                                fields: {
                                    name: {
                                        label: 'label.name',
                                        validation: {
                                            required: true
                                        },
                                        docID: 'helpGuestNetworkName'
                                    },
                                    displayText: {
                                        label: 'label.display.text',
                                        validation: {
                                            required: true
                                        },
                                        docID: 'helpGuestNetworkDisplayText'
                                    },
                                    zoneId: {
                                        label: 'label.zone',
                                        validation: {
                                            required: true
                                        },
                                        docID: 'helpGuestNetworkZone',

                                        select: function(args) {
                                            $.ajax({
                                                url: createURL('listZones'),
                                                success: function(json) {
                                                    var zones = $.grep(json.listzonesresponse.zone, function(zone) {
                                                        return (zone.networktype == 'Advanced' && zone.securitygroupsenabled != true); //Isolated networks can only be created in Advanced SG-disabled zone (but not in Basic zone nor Advanced SG-enabled zone)
                                                    });

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
                                    networkOfferingId: {
                                        label: 'label.network.offering',
                                        validation: {
                                            required: true
                                        },
                                        dependsOn: 'zoneId',
                                        docID: 'helpGuestNetworkNetworkOffering',
                                        select: function(args) {
                                            var data = {
                                                zoneid: args.zoneId,
                                                guestiptype: 'Isolated',
                                                supportedServices: 'SourceNat',
                                                state: 'Enabled'
                                            };

                                            if ('vpc' in args.context) { //from VPC section
                                                $.extend(data, {
                                                    forVpc: true
                                                });
                                            }
                                            else { //from guest network section
                                                var vpcs;
                                                $.ajax({
                                                    url: createURL('listVPCs'),
                                                    data: {
                                                        listAll: true
                                                    },
                                                    async: false,
                                                    success: function(json) {
                                                        vpcs = json.listvpcsresponse.vpc;
                                                    }
                                                });
                                                if (vpcs == null || vpcs.length == 0) { //if there is no VPC in the system
                                                    $.extend(data, {
                                                        forVpc: false
                                                    });
                                                }
                                            }

                                            if(!isAdmin()) { //normal user is not aware of the VLANs in the system, so normal user is not allowed to create network with network offerings whose specifyvlan = true
                                                $.extend(data, {
                                                    specifyvlan: false
                                                });
                                            }

                                            $.ajax({
                                                url: createURL('listNetworkOfferings'),
                                                data: data,
                                                success: function(json) {
                                                    networkOfferingObjs = json.listnetworkofferingsresponse.networkoffering;
                                                    args.$select.change(function() {
                                                        var $vlan = args.$select.closest('form').find('[rel=vlan]');
                                                        var networkOffering = $.grep(
                                                            networkOfferingObjs, function(netoffer) {
                                                                return netoffer.id == args.$select.val();
                                                            }
                                                        )[0];

                                                        if (networkOffering.specifyvlan) {
                                                            $vlan.css('display', 'inline-block');
                                                        } else {
                                                            $vlan.hide();
                                                        }
                                                    });

                                                    args.response.success({
                                                        data: $.map(networkOfferingObjs, function(zone) {
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

                                    vlan: {
                                        label: 'label.vlan',
                                        validation: {
                                            required: true
                                        },
                                        isHidden: true
                                    },

                                    vpcid: {
                                        label: 'label.vpc',
                                        dependsOn: 'networkOfferingId',
                                        select: function(args) {
                                            var networkOfferingObj;
                                            var $form = args.$select.closest('form');
                                            var data = {
                                                listAll: true,
                                                details: 'min'
                                            };

                                            if (args.context.vpc) {
                                                data.id = args.context.vpc[0].id;
                                            }

                                            $(networkOfferingObjs).each(function(key, value) {
                                                if (value.id == args.networkOfferingId) {
                                                    networkOfferingObj = value;
                                                    return false; //break each loop
                                                }
                                            });

                                            if (networkOfferingObj.forvpc == true) {
                                                args.$select.closest('.form-item').css('display', 'inline-block');
                                                $.ajax({
                                                    url: createURL('listVPCs'),
                                                    data: data,
                                                    success: function(json) {
                                                        var items = json.listvpcsresponse.vpc;
                                                        var data;
                                                        if (items != null && items.length > 0) {
                                                            data = $.map(items, function(item) {
                                                                return {
                                                                    id: item.id,
                                                                    description: item.name
                                                                }
                                                            });
                                                        }
                                                        args.response.success({
                                                            data: data
                                                        });
                                                    }
                                                });
                                                $form.find('.form-item[rel=networkDomain]').hide();
                                            } else {
                                                args.$select.closest('.form-item').hide();
                                                $form.find('.form-item[rel=networkDomain]').show();
                                                args.response.success({
                                                    data: null
                                                });
                                            }
                                        }
                                    },
                                    externalId: {
                                        label: 'label.guest.externalId'
                                    },
                                    guestGateway: {
                                        label: 'label.guest.gateway',
                                        docID: 'helpGuestNetworkGateway'
                                    },
                                    guestNetmask: {
                                        label: 'label.guest.netmask',
                                        docID: 'helpGuestNetworkNetmask'
                                    },
                                    networkDomain: {
                                        label: 'label.network.domain'
                                    },
                                    domain: {
                                        label: 'label.domain',
                                        isHidden: function(args) {
                                            if (isAdmin() || isDomainAdmin())
                                                return false;
                                            else
                                                return true;
                                        },
                                        select: function(args) {
                                            if (isAdmin() || isDomainAdmin()) {
                                                $.ajax({
                                                    url: createURL("listDomains&listAll=true"),
                                                    success: function(json) {
                                                        var items = [];
                                                        items.push({
                                                            id: "",
                                                            description: ""
                                                        });
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
                                                args.$select.change(function() {
                                                    var $form = $(this).closest('form');
                                                    if ($(this).val() == "") {
                                                        $form.find('.form-item[rel=account]').hide();
                                                    } else {
                                                        $form.find('.form-item[rel=account]').css('display', 'inline-block');
                                                    }
                                                });
                                            } else {
                                                args.response.success({
                                                    data: null
                                                });
                                            }
                                        }
                                    },
                                    account: {
                                        label: 'label.account',
                                        validation: {
                                            required: true
                                        },
                                        isHidden: function(args) {
                                            if (isAdmin() || isDomainAdmin())
                                                return false;
                                            else
                                                return true;
                                        }
                                    }
                                }
                            },
                            action: function(args) {
                                var dataObj = {
                                    zoneId: args.data.zoneId,
                                    name: args.data.name,
                                    displayText: args.data.displayText,
                                    networkOfferingId: args.data.networkOfferingId
                                };

                                if (args.data.guestGateway != null && args.data.guestGateway.length > 0) {
                                    $.extend(dataObj, {
                                        gateway: args.data.guestGateway
                                    });
                                }
                                if (args.data.guestNetmask != null && args.data.guestNetmask.length > 0) {
                                    $.extend(dataObj, {
                                        netmask: args.data.guestNetmask
                                    });
                                }
                                if (args.data.externalId != null && args.data.externalId.length > 0) {
                                    $.extend(dataObj, {
                                        externalid: args.data.externalId
                                    });
                                }
                                if (args.$form.find('.form-item[rel=vpcid]').css("display") != "none") {
                                    $.extend(dataObj, {
                                        vpcid: args.data.vpcid
                                    });
                                }

                                if (args.$form.find('.form-item[rel=vlan]').css('display') != 'none') {
                                    $.extend(dataObj, {
                                        vlan: args.data.vlan
                                    });
                                }

                                if (args.data.networkDomain != null && args.data.networkDomain.length > 0 && args.$form.find('.form-item[rel=vpcid]').css("display") == "none") {
                                    $.extend(dataObj, {
                                        networkDomain: args.data.networkDomain
                                    });
                                }

                                if (args.data.domain != null && args.data.domain.length > 0) {
                                    $.extend(dataObj, {
                                        domainid: args.data.domain
                                    });
                                    if (args.data.account != null && args.data.account.length > 0) {
                                        $.extend(dataObj, {
                                            account: args.data.account
                                        });
                                    }
                                }

                                $.ajax({
                                    url: createURL('createNetwork'),
                                    data: dataObj,
                                    success: function(json) {
                                        args.response.success({
                                            data: json.createnetworkresponse.network
                                        });
                                    },
                                    error: function(json) {
                                        args.response.error(parseXMLHttpResponse(json));
                                    }
                                });
                            },
                            messages: {
                                notification: function() {
                                    return 'label.add.isolated.guest.network';
                                }
                            }
                        },

                        rootAdminAddGuestNetwork: $.extend({}, addGuestNetworkDialog.def, {
                            isHeader: true
                        }),

                        AddL2Network: $.extend({}, addL2GuestNetwork.def, {
                            isHeader: true
                        })

                    },
                    id: 'networks',
                    preFilter: function(args) {
                        if (isAdmin() || isDomainAdmin()) {
                            return []
                        }
                        return ['account']
                    },
                    fields: {
                        name: {
                            label: 'label.name'
                        },
                        type: {
                            label: 'label.type'
                        },
                        cidr: {
                            label: 'label.cidr'
                        },
                        ip6cidr: {
                            label: 'label.ipv6.CIDR'
                        },
                        account: {
                            label: 'label.account'
                        },
                        zonename: {
                            label: 'label.zone'
                        },
                        state: {
                            converter: function(str) {
                                // For localization
                                return str;
                            },
                            label: 'label.state',
                            indicator: {
                                'Allocated': 'on',
                                'Released': 'off',
                                'Destroy': 'off',
                                'Shutdown': 'off',
                                'Setup': 'on',
                                'Implemented': 'on'
                            }
                        }
                    },

                    advSearchFields: {
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

                        if ("routers" in args.context) {
                            if ("vpcid" in args.context.routers[0]) {
                                $.extend(data, {
                                    vpcid: args.context.routers[0].vpcid
                                });
                            } else {
                                if ("guestnetworkid" in args.context.routers[0]) {
                                    $.extend(data, {
                                        id: args.context.routers[0].guestnetworkid
                                    });
                                }
                            }
                            if ("projectid" in args.context.routers[0]) {
                                $.extend(data, {
                                    projectid: args.context.routers[0].projectid
                                });
                            }
                        }

                        $.ajax({
                            url: createURL('listNetworks'),
                            data: data,
                            async: false,
                            success: function(data) {
                                args.response.success({
                                    data: data.listnetworksresponse.network
                                });
                            },
                            error: function(data) {
                                args.response.error(parseXMLHttpResponse(data));
                            }
                        });
                    },

                    detailView: {
                        name: 'label.guest.network.details',
                        viewAll: [{
                            path: 'network.ipAddresses',
                            label: 'label.menu.ipaddresses',
                            preFilter: function(args) {
                                if (args.context.networks[0].state == 'Destroyed' ||
                                    args.context.networks[0].type == 'L2')
                                    return false;

                                return true;
                            }
                        }, {
                            label: 'label.instances',
                            path: 'instances'
                        }],
                        actions: {
                            edit: {
                                label: 'label.edit',
                                messages: {
                                    notification: function(args) {
                                        return 'label.edit.network.details';
                                    }
                                },
                                preFilter: function(args) {
                                    if (args.context.networks[0].state == 'Destroyed')
                                        return false;
                                    return true;
                                },
                                action: function(args) {
                                    var data = {
                                        id: args.context.networks[0].id,
                                        name: args.data.name,
                                        displaytext: args.data.displaytext
                                    };

                                    //args.data.networkdomain is null when networkdomain field is hidden
                                    if (args.data.networkdomain != null && args.data.networkdomain.length > 0 && args.data.networkdomain != args.context.networks[0].networkdomain) {
                                        $.extend(data, {
                                            networkdomain: args.data.networkdomain
                                        });
                                    }

                                    var oldcidr;
                                    $.ajax({
                                        url: createURL("listNetworks&id=" + args.context.networks[0].id + "&listAll=true"),
                                        dataType: "json",
                                        async: false,
                                        success: function(json) {
                                            oldcidr = json.listnetworksresponse.network[0].cidr;

                                        }
                                    });


                                    if (args.data.cidr != "" && args.data.cidr != oldcidr) {
                                        $.extend(data, {
                                            guestvmcidr: args.data.cidr
                                        });
                                    }

                                    //args.data.networkofferingid is null when networkofferingid field is hidden
                                    if (args.data.networkofferingid != null && args.data.networkofferingid != args.context.networks[0].networkofferingid) {
                                        $.extend(data, {
                                            networkofferingid: args.data.networkofferingid
                                        });

                                        if (args.context.networks[0].type == "Isolated") { //Isolated network
                                            cloudStack.dialog.confirm({
                                                message: 'message.confirm.current.guest.CIDR.unchanged',
                                                action: function() { //"Yes" button is clicked
                                                    getForcedInfoAndUpdateNetwork(data, args);
                                                },
                                                cancelAction: function() { //"Cancel" button is clicked
                                                    $.extend(data, {
                                                        changecidr: true
                                                    });

                                                    getForcedInfoAndUpdateNetwork(data, args);
                                                }
                                            });
                                            return;
                                        }
                                    }

                                    $.ajax({
                                        url: createURL('updateNetwork'),
                                        data: data,
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
                                        }
                                    });
                                },
                                notification: {
                                    poll: pollAsyncJobResult
                                }
                            },

                            restart: {
                                label: 'label.restart.network',
                                preFilter: function(args) {
                                    if (args.context.networks[0].state == 'Destroyed')
                                        return false;
                                    return true;
                                },
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
                                        args.$form.find('.form-item[rel=cleanup]').find('input').attr('checked', 'checked'); //checked
                                        args.$form.find('.form-item[rel=cleanup]').css('display', 'inline-block'); //shown
                                        args.$form.find('.form-item[rel=makeredundant]').find('input').attr('checked', 'checked'); //checked
                                        args.$form.find('.form-item[rel=makeredundant]').css('display', 'inline-block'); //shown

                                        if (Boolean(args.context.networks[0].redundantrouter)) {
                                            args.$form.find('.form-item[rel=makeredundant]').hide();
                                        } else {
                                            args.$form.find('.form-item[rel=makeredundant]').show();
                                        }
                                    },
                                    fields: {
                                        cleanup: {
                                            label: 'label.clean.up',
                                            isBoolean: true
                                        },
                                        makeredundant: {
                                            label: 'label.make.redundant',
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
                                    $.ajax({
                                        url: createURL("restartNetwork"),
                                        data: {
                                            id: args.context.networks[0].id,
                                            cleanup: (args.data.cleanup == "on"),
                                            makeredundant: (args.data.makeredundant == "on")
                                        },
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
                                preFilter: function(args) {
                                    if (args.context.networks[0].state == 'Destroyed')
                                        return false;
                                    return true;
                                },
                                messages: {
                                    confirm: function(args) {
                                        return 'message.action.delete.network';
                                    },
                                    isWarning: true,
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
                            }
                        },

                        tabFilter: function(args) {
                            var networkHavingELB = false;
                            var hasNetworkACL = false;
                            var hasSRXFirewall = false;
                            var isVPC = false;
                            var isAdvancedSGZone = false;
                            var hiddenTabs = [];
                            var isSharedNetwork;

                            var thisNetwork = args.context.networks[0];
                            if (thisNetwork.vpcid != null) {
                                isVPC = true;
                            }
                            if (thisNetwork.type == 'Shared') {
                                isSharedNetwork = true;
                            }

                            $(thisNetwork.service).each(function() {
                                var thisService = this;

                                if (thisService.name == 'NetworkACL') {
                                    hasNetworkACL = true;
                                } else if (thisService.name == "Lb") {
                                    $(thisService.capability).each(function() {
                                        if (this.name == "ElasticLb" && this.value == "true") {
                                            networkHavingELB = true;
                                        }
                                    });
                                }

                                if (thisService.name == 'Firewall') {
                                    $(thisService.provider).each(function() {
                                        if (this.name == 'JuniperSRX') {
                                            hasSRXFirewall = true;

                                            return false;
                                        }

                                        return true;
                                    });
                                }
                            });

                            // Get zone data
                            $.ajax({
                                url: createURL('listZones'),
                                data: {
                                    id: args.context.networks[0].zoneid
                                },
                                async: false,
                                success: function(json) {
                                    var zone = json.listzonesresponse.zone[0];

                                    isAdvancedSGZone = zone.securitygroupsenabled;
                                }
                            });

                            if (isVPC || isAdvancedSGZone || isSharedNetwork) {
                                hiddenTabs.push('egressRules');
                            }

                            if (!isAdmin()) {
                                hiddenTabs.push("virtualRouters");
                            }

                            return hiddenTabs;
                        },

                        isMaximized: true,
                        tabs: {
                            details: {
                                title: 'label.details',
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

                                    if (!isAdmin()) {
                                        hiddenFields.push("vlan");
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

                                    vpcid: {
                                        label: 'label.vpc.id',
                                        converter: function(args) {
                                            if (args != null)
                                                return args;
                                            else
                                                return _l('label.na');
                                        }
                                    },

                                    ispersistent: {
                                        label: 'label.persistent',
                                        converter: cloudStack.converters.toBooleanText

                                    },
                                    restartrequired: {
                                        label: 'label.restart.required',
                                        converter: function(booleanValue) {
                                            if (booleanValue == true)
                                                return "Yes";
                                            else if (booleanValue == false)
                                                return "No";
                                        }
                                    },
                                    vlan: {
                                        label: 'label.vnet.id'
                                    },

                                    broadcasturi: {
                                        label: 'label.broadcasturi'
                                    },

                                    networkofferingid: {
                                        label: 'label.network.offering',
                                        isEditable: true,
                                        select: function(args) {
                                            if (args.context.networks[0].type == 'Shared') { //Shared network is not allowed to upgrade to a different network offering
                                                args.response.success({
                                                    data: []
                                                });
                                                return;
                                            }

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

                                    //netmask: { label: 'label.netmask' },
                                    cidr: {
                                        label: 'label.cidr',
                                        isEditable: true
                                    },

                                    networkcidr: {
                                        label: 'label.network.cidr'
                                    },

                                    ip6gateway: {
                                        label: 'label.ipv6.gateway'
                                    },

                                    ip6cidr: {
                                        label: 'label.ipv6.CIDR'
                                    },


                                    reservediprange: {
                                        label: 'label.reserved.ip.range'
                                    },

                                    redundantrouter: {
                                        label: 'label.redundant.router',
                                        converter: function(booleanValue) {
                                            if (booleanValue == true) {
                                                return "Yes";
                                            }
                                            return "No";
                                        }
                                    },

                                    networkdomaintext: {
                                        label: 'label.network.domain.text'
                                    },

                                    networkdomain: {
                                        label: 'label.network.domain',
                                        isEditable: true
                                    },

                                    domain: {
                                        label: 'label.domain'
                                    },
                                    account: {
                                        label: 'label.account'
                                    }
                                }],

                                tags: cloudStack.api.tags({
                                    resourceType: 'Network',
                                    contextId: 'networks'
                                }),


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
                                            addExtraPropertiesToGuestNetworkObject(jsonObj);

                                            $(window).trigger('cloudStack.module.sharedFunctions.addExtraProperties', {
                                                obj: jsonObj,
                                                objType: "Network"
                                            });

                                            args.response.success({
                                                actionFilter: cloudStack.actionFilter.guestNetwork,
                                                data: jsonObj
                                            });
                                        }
                                    });
                                }
                            },

                            egressRules: {
                                title: 'label.egress.rules',
                                custom: function(args) {
                                    var context = args.context;
                                    var isConfigRulesMsgShown = false;

                                    return $('<div>').multiEdit({
                                        context: context,
                                        noSelect: true,
                                        noHeaderActionsColumn: true,
                                        fields: {
                                            'cidrlist': {
                                                edit: true,
                                                label: 'label.cidr.list',
                                                isOptional: true
                                            },
                                            'destcidrlist': {
                                                 edit: true,
                                                 label: 'label.cidr.destination.list',
                                                 isOptional: true
                                             },
                                            'protocol': {
                                                label: 'label.protocol',
                                                select: function(args) {
                                                    args.$select.change(function() {
                                                        var $inputs = args.$form.find('th, td');
                                                        var $icmpFields = $inputs.filter(function() {
                                                            var name = $(this).attr('rel');

                                                            return $.inArray(name, [
                                                                'icmptype',
                                                                'icmpcode'
                                                            ]) > -1;
                                                        });
                                                        var $otherFields = $inputs.filter(function() {
                                                            var name = $(this).attr('rel');

                                                            return name != 'cidrlist' &&
                                                                name != 'destcidrlist' &&
                                                                name != 'icmptype' &&
                                                                name != 'icmpcode' &&
                                                                name != 'protocol' &&
                                                                name != 'add-rule';
                                                        });

                                                        if ($(this).val() == 'icmp') {
                                                            $icmpFields.show();
                                                            $otherFields.hide();
                                                        } else if ($(this).val() == 'all') {
                                                            $icmpFields.hide();
                                                            $otherFields.hide();
                                                        } else {
                                                            $icmpFields.hide();
                                                            $otherFields.show();
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
                                                            description: 'All'
                                                        }]
                                                    });
                                                }
                                            },
                                            'startport': {
                                                edit: true,
                                                label: 'label.start.port',
                                                isOptional: true
                                            },
                                            'endport': {
                                                edit: true,
                                                label: 'label.end.port',
                                                isOptional: true
                                            },
                                            'icmptype': {
                                                edit: true,
                                                label: 'ICMP.type',
                                                isHidden: true,
                                                isOptional: true
                                            },
                                            'icmpcode': {
                                                edit: true,
                                                label: 'ICMP.code',
                                                isHidden: true,
                                                isOptional: true
                                            },
                                            'add-rule': {
                                                label: 'label.add',
                                                addButton: true
                                            }
                                        },
                                        add: {
                                            label: 'label.add',
                                            action: function(args) {
                                                var data = {
                                                    protocol: args.data.protocol,
                                                    cidrlist: args.data.cidrlist,
                                                    destcidrlist: args.data.destcidrlist,
                                                    networkid: args.context.networks[0].id
                                                };

                                                if (args.data.icmptype && args.data.icmpcode) { // ICMP
                                                    $.extend(data, {
                                                        icmptype: args.data.icmptype,
                                                        icmpcode: args.data.icmpcode
                                                    });
                                                } else { // TCP/UDP
                                                    $.extend(data, {
                                                        startport: args.data.startport,
                                                        endport: args.data.endport
                                                    });
                                                }

                                                $.ajax({
                                                    url: createURL('createEgressFirewallRule'),
                                                    data: data,
                                                    dataType: 'json',
                                                    async: true,
                                                    success: function(json) {
                                                        var jobId = json.createegressfirewallruleresponse.jobid;

                                                        args.response.success({
                                                            _custom: {
                                                                jobId: jobId
                                                            },
                                                            notification: {
                                                                label: 'label.add.egress.rule',
                                                                poll: pollAsyncJobResult
                                                            }
                                                        });
                                                    },
                                                    error: function(json) {
                                                        args.response.error(parseXMLHttpResponse(json));
                                                    }
                                                });
                                            }
                                        },
                                        actions: {
                                            destroy: {
                                                label: 'label.remove.rule',
                                                action: function(args) {
                                                    $.ajax({
                                                        url: createURL('deleteEgressFirewallRule'),
                                                        data: {
                                                            id: args.context.multiRule[0].id
                                                        },
                                                        dataType: 'json',
                                                        async: true,
                                                        success: function(data) {
                                                            var jobID = data.deleteegressfirewallruleresponse.jobid;

                                                            args.response.success({
                                                                _custom: {
                                                                    jobId: jobID
                                                                },
                                                                notification: {
                                                                    label: 'label.remove.egress.rule',
                                                                    poll: pollAsyncJobResult
                                                                }
                                                            });
                                                        },
                                                        error: function(json) {
                                                            args.response.error(parseXMLHttpResponse(json));
                                                        }
                                                    });
                                                }
                                            }
                                        },
                                        ignoreEmptyFields: true,
                                        dataProvider: function(args) {
                                            $.ajax({
                                                url: createURL('listEgressFirewallRules'),
                                                data: {
                                                    listAll: true,
                                                    networkid: args.context.networks[0].id
                                                },
                                                dataType: 'json',
                                                async: false,
                                                success: function(json) {
                                                    var response = json.listegressfirewallrulesresponse.firewallrule ?
                                                        json.listegressfirewallrulesresponse.firewallrule : [];

                                                    if (response.length > 0) {
                                                        isConfigRulesMsgShown = true;
                                                    }
                                                    args.response.success({
                                                        data: $.map(response, function(rule) {
                                                            if (rule.protocol == 'all') {
                                                                $.extend(rule, {
                                                                    startport: 'All',
                                                                    endport: 'All'
                                                                });
                                                            } else if (rule.protocol == 'tcp' || rule.protocol == 'udp') {
                                                                if (!rule.startport) {
                                                                    rule.startport = ' ';
                                                                }

                                                                if (!rule.endport) {
                                                                    rule.endport = ' ';
                                                                }
                                                            }
                                                            if(!rule.destcidrlist){
                                                                rule.destcidrlist = ' ';
                                                            }
                                                            return rule;
                                                        })
                                                    });
                                                }
                                            });

                                            if (!isConfigRulesMsgShown) {
                                                isConfigRulesMsgShown = true;
                                                $.ajax({
                                                    url: createURL('listNetworkOfferings'),
                                                    data: {
                                                        id: args.context.networks[0].networkofferingid
                                                    },
                                                    dataType: 'json',
                                                    async: true,
                                                    success: function(json) {
                                                        var response = json.listnetworkofferingsresponse.networkoffering ?
                                                            json.listnetworkofferingsresponse.networkoffering[0] : null;

                                                        if (response != null) {
                                                            if (response.egressdefaultpolicy == true) {
                                                                cloudStack.dialog.notice({
                                                                    message: _l('message.configure.firewall.rules.block.traffic')
                                                                });
                                                            } else {
                                                                cloudStack.dialog.notice({
                                                                    message: _l('message.configure.firewall.rules.allow.traffic')
                                                                });
                                                            }
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                            },

                            virtualRouters: {
                                title: "label.virtual.appliances",
                                listView: cloudStack.sections.system.subsections.virtualRouters.sections.routerNoGroup.listView
                            }
                        }
                    }
                }
            },
            secondaryNicIps: {
                title: 'label.menu.ipaddresses',
                listView: {
                    id: 'secondaryNicIps',
                    label: 'label.ips',
                    fields: {
                        virtualmachinedisplayname: {
                            label: 'label.vm.name'
                        },
                        ipaddress: {
                            label: 'label.ips',
                            converter: function(text, item) {
                                if (item.issourcenat) {
                                    return text + ' [' + _l('label.source.nat') + ']';
                                }

                                return text;
                            }
                        }
                    },
                    actions: {
                        add: {
                            label: 'label.acquire.new.secondary.ip',
                            addRow: 'true',
                            createForm: {
                                title: 'label.acquire.new.secondary.ip',
                                desc: 'message.acquire.ip.nic',
                                fields: {
                                    ipaddress: {
                                        label: 'label.ip.address',
                                        validation: {
                                            required: false,
                                            ipv4AndIpv6AddressValidator: true
                                        }
                                    }
                                }
                            },
                            messages: {
                                notification: function(args) {
                                    return _l('label.acquire.new.secondary.ip');
                                }
                            },
                            action: function(args) {
                                var dataObj = {
                                    nicId: args.context.nics[0].id
                                };

                                if (args.data.ipaddress) {
                                    dataObj.ipaddress = args.data.ipaddress;
                                }

                                $.ajax({
                                    url: createURL('addIpToNic'),
                                    data: dataObj,
                                    success: function(json) {
                                        args.response.success({
                                            _custom: {
                                                getUpdatedItem: function(data) {
                                                    return $.extend(
                                                        data.queryasyncjobresultresponse.jobresult.nicsecondaryip, {
                                                            zoneid: args.context.instances[0].zoneid,
                                                            virtualmachinedisplayname: args.context.instances[0].displayname ? args.context.instances[0].displayname : args.context.instances[0].name
                                                        }
                                                    );
                                                },
                                                jobId: json.addiptovmnicresponse.jobid
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

                    dataProvider: function(args) {
                        var data = {};

                        if (args.filterBy.search.value != null) {
                            data.keyword = args.filterBy.search.value;
                        }

                        $.ajax({
                            url: createURL('listNics'),
                            data: {
                                nicId: args.context.nics[0].id,
                                virtualmachineid: args.context.instances[0].id,
                                keyword: args.filterBy.search.value
                            },
                            success: function(json) {
                                var ips = json.listnicsresponse.nic ? json.listnicsresponse.nic[0].secondaryip : [];

                                args.response.success({
                                    data: $(ips).map(function(index, ip) {
                                        return $.extend(ip, {
                                            zoneid: args.context.instances[0].zoneid,
                                            virtualmachinedisplayname: args.context.instances[0].displayname ? args.context.instances[0].displayname : args.context.instances[0].name
                                        });
                                    })
                                });
                            }
                        });
                    },

                    // Detail view
                    detailView: {
                        name: 'Secondary IP address detail',
                        actions: {
                            remove: {
                                label: 'label.action.release.ip',
                                action: function(args) {
                                    $.ajax({
                                        url: createURL('removeIpFromNic'),
                                        data: {
                                            id: args.context.secondaryNicIps[0].id
                                        },
                                        success: function(json) {
                                            args.response.success({
                                                _custom: {
                                                    jobId: json.removeipfromnicresponse.jobid
                                                }
                                            });
                                        }
                                    });
                                },
                                messages: {
                                    confirm: function(args) {
                                        return 'message.action.release.ip';
                                    },
                                    notification: function(args) {
                                        return 'label.action.release.ip';
                                    }
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
                                    ipaddress: {
                                        label: 'label.ip'
                                    }
                                }, {
                                    id: {
                                        label: 'label.id'
                                    },
                                    virtualmachinedisplayname: {
                                        label: 'label.vm.name'
                                    },
                                    zonename: {
                                        label: 'label.zone.name'
                                    }
                                }],

                                dataProvider: function(args) {
                                    $.ajax({
                                        url: createURL('listNics'),
                                        data: {
                                            nicId: args.context.nics[0].id,
                                            virtualmachineid: args.context.instances[0].id
                                        },
                                        success: function(json) {
                                            var ips = json.listnicsresponse.nic[0].secondaryip

                                            args.response.success({
                                                data: $.grep($(ips).map(function(index, ip) {
                                                    return $.extend(ip, {
                                                        zonename: args.context.instances[0].zonename,
                                                        virtualmachinedisplayname: args.context.instances[0].displayname
                                                    });
                                                }), function(ip) {
                                                    return ip.ipaddress == args.context.secondaryNicIps[0].ipaddress;
                                                })[0]
                                            });
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
            },
            ipAddresses: {
                type: 'select',
                title: 'label.menu.ipaddresses',
                listView: {
                    id: 'ipAddresses',
                    label: 'label.ips',
                    preFilter: function(args) {
                        if (isAdmin()) {
                            return ['account']
                        }
                        return []
                    },
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
                        associatednetworkname: {
                            label: 'label.network'
                        },
                        virtualmachinedisplayname: {
                            label: 'label.vm.name'
                        },
                        account: {
                            label: 'label.account'
                        },
                        zonename: {
                            label: 'label.zone'
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
                    actions: {
                        add: {
                            label: 'label.acquire.new.ip',
                            addRow: 'true',
                            preFilter: function(args) {
                                var zoneObj;
                                var dataObj = {};

                                if ('vpc' in args.context) { //from VPC section
                                    $.extend(dataObj, {
                                        id: args.context.vpc[0].zoneid
                                    });
                                } else if ('networks' in args.context) { //from Guest Network section
                                    $.extend(dataObj, {
                                        id: args.context.networks[0].zoneid
                                    });
                                }

                                $.ajax({
                                    url: createURL('listZones'),
                                    data: dataObj,
                                    async: false,
                                    success: function(json) {
                                        zoneObj = json.listzonesresponse.zone[0];
                                    }
                                });

                                if (zoneObj.networktype == 'Advanced' && zoneObj.securitygroupsenabled) {
                                    return false;
                                }

                                if (zoneObj.networktype == 'Basic') {
                                    var havingEIP = false,
                                        havingELB = false;

                                    var services = args.context.networks[0].service;
                                    if(services != null) {
                                        for(var i = 0; i < services.length; i++) {
                                            var thisService = services[i];
                                            var capabilities = thisService.capability;
                                            if (thisService.name == "StaticNat") {
                                                if(capabilities != null) {
                                                    for(var k = 0; k < capabilities.length; k++) {
                                                        if (capabilities[k].name == "ElasticIp" && capabilities[k].value == "true") {
                                                            havingEIP = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                            } else if (thisService.name == "Lb") {
                                                if(capabilities != null) {
                                                    for(var k = 0; k < capabilities.length; k++) {
                                                        if (capabilities[k].name == "ElasticLb" && capabilities[k].value == "true") {
                                                            havingELB = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (havingEIP != true || havingELB != true) { //not EIP-ELB
                                        return false; //acquire new IP is not allowed in non-EIP-ELB basic zone
                                    }
                                }

                                //*** from Guest Network section ***
                                if (!('vpc' in args.context)) {
                                    if (args.context.networks[0].vpcid == null) { //Guest Network section > non-VPC network, show Acquire IP button
                                        return true;
                                    } else { //Guest Network section > VPC network, hide Acquire IP button
                                        return false;
                                    }
                                }
                                //*** from VPC section ***
                                else { //'vpc' in args.context //args.context.networks[0] has only one property => name: 'Router'
                                    return true; //VPC section, show Acquire IP button
                                }
                            },
                            messages: {
                                notification: function(args) {
                                    return 'label.acquire.new.ip';
                                }
                            },
                            createForm: {
                                title: 'label.acquire.new.ip',
                                desc: 'Please confirm that you want to acquire new IP',
                                preFilter: function(args) {
                                    $.ajax({
                                        url: createURL('listRegions'),
                                        success: function(json) {
                                            var selectedRegionName = $(".region-switcher .title").text();
                                            if ( selectedRegionName == undefined || selectedRegionName.length == 0) {
                                                selectedRegionName = "Local";
                                            }
                                            var items = json.listregionsresponse.region;
                                            if(items != null) {
                                                for(var i = 0; i < items.length; i++) {
                                                    if(items[i].name == selectedRegionName) {
                                                        if(items[i].portableipserviceenabled == true) {
                                                            args.$form.find('.form-item[rel=isportable]').css('display', 'inline-block');
                                                        } else {
                                                            args.$form.find('.form-item[rel=isportable]').hide();
                                                        }
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    });
                                },
                                fields: {
                                    isportable: {
                                        label: 'label.cross.zones',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: "false",
                                                description: _l('label.no')
                                            });
                                            items.push({
                                                id: "true",
                                                description: _l('label.yes')
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        },
                                        isHidden: true
                                    }
                                }
                            },
                            action: function(args) {
                                var dataObj = {};
                                if (args.$form.find('.form-item[rel=isportable]').css("display") != "none") {
                                    $.extend(dataObj, {
                                        isportable: args.data.isportable
                                    });
                                }

                                if ('vpc' in args.context) { //from VPC section
                                    $.extend(dataObj, {
                                        vpcid: args.context.vpc[0].id
                                    });
                                } else if ('networks' in args.context) { //from Guest Network section
                                    $.extend(dataObj, {
                                        networkid: args.context.networks[0].id
                                    });

                                    if (args.context.networks[0].type == "Shared" && !args.context.projects) {
                                        $.extend(dataObj, {
                                            domainid: g_domainid,
                                            account: g_account
                                        });
                                    }
                                }

                                $.ajax({
                                    url: createURL('associateIpAddress'),
                                    data: dataObj,
                                    success: function(data) {
                                        args.response.success({
                                            _custom: {
                                                jobId: data.associateipaddressresponse.jobid,
                                                getUpdatedItem: function(data) {
                                                    var newIP = data.queryasyncjobresultresponse.jobresult.ipaddress;
                                                    return $.extend(newIP, {
                                                        state: 'Allocated'
                                                    });
                                                },
                                                getActionFilter: function() {
                                                    return actionFilters.ipAddress;
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
                        var items = [];
                        var data = {};
                        listViewDataProvider(args, data);
                        if (args.context.networks) {
                            $.extend(data, {
                                associatedNetworkId: args.context.networks[0].id
                            });
                        }
                        if ("vpc" in args.context) {
                            $.extend(data, {
                                vpcid: args.context.vpc[0].id
                            });
                        }

                        $.ajax({
                            url: createURL('listPublicIpAddresses'),
                            data: $.extend({}, data, {
                                forvirtualnetwork: true //IPs are allocated on public network
                            }),
                            dataType: "json",
                            async: false,
                            success: function(json) {
                                var ips = json.listpublicipaddressesresponse.publicipaddress;
                                if(ips != null) {
                                    for(var i = 0; i < ips.length; i++) {
                                        getExtaPropertiesForIpObj(ips[i], args);
                                        items.push(ips[i]);
                                    }
                                }
                            }
                        });

                        if (g_supportELB == "guest") {
                            $.ajax({
                                url: createURL('listPublicIpAddresses'),
                                data: $.extend({}, data, {
                                    forvirtualnetwork: false, // ELB IPs are allocated on guest network
                                    forloadbalancing: true
                                }),
                                dataType: "json",
                                async: false,
                                success: function(json) {
                                    var ips = json.listpublicipaddressesresponse.publicipaddress;
                                    if(ips != null) {
                                        for(var i = 0; i < ips.length; i++) {
                                            getExtaPropertiesForIpObj(ips[i], args);
                                            items.push(ips[i]);
                                        }
                                    }
                                }
                            });
                        }

                        args.response.success({
                            actionFilter: actionFilters.ipAddress,
                            data: items
                        });
                    },

                    // Detail view
                    detailView: {
                        name: 'IP address detail',
                        tabFilter: function(args) {
                            var item = args.context.ipAddresses[0];

                            var disabledTabs = [];
                            var ipAddress = args.context.ipAddresses[0];
                            var disableVpn = false,
                                disableIpRules = false;

                            if (!ipAddress.vpnenabled) {
                                disableVpn = true;
                            }

                            if (ipAddress.issystem == true) {
                                disableVpn = true;

                                if (ipAddress.isstaticnat == true || ipAddress.virtualmachineid != null) {
                                    disableIpRules = true;
                                }
                            }

                            if (ipAddress.vpcid != null && ipAddress.issourcenat) { //don't show Configuration(ipRules) tab on VPC sourceNAT IP
                                disableIpRules = true;
                            }

                            if (('vpc' in args.context) == false && ipAddress.vpcid != null) { //from Guest Network section, don't show Configuration(ipRules) tab on VPC IP
                                disableIpRules = true;
                            }

                            if (disableVpn)
                                disabledTabs.push('vpn');
                            if (disableIpRules)
                                disabledTabs.push('ipRules');

                            return disabledTabs;
                        },
                        actions: {
                            enableVPN: {
                                label: 'label.enable.vpn',
                                action: function(args) {
                                    $.ajax({
                                        url: createURL('createRemoteAccessVpn'),
                                        data: {
                                            publicipid: args.context.ipAddresses[0].id,
                                            domainid: args.context.ipAddresses[0].domainid,
                                            account: args.context.ipAddresses[0].account
                                        },
                                        dataType: 'json',
                                        async: true,
                                        success: function(data) {
                                            args.response.success({
                                                _custom: {
                                                    getUpdatedItem: function(json) {
                                                        var vpnenabledAndRunning = false;
                                                        if (json.queryasyncjobresultresponse.jobresult.remoteaccessvpn.state == "Running") {
                                                            vpnenabledAndRunning = true;
                                                        }

                                                        return {
                                                            remoteaccessvpn: json.queryasyncjobresultresponse.jobresult.remoteaccessvpn,
                                                            vpnenabled: vpnenabledAndRunning
                                                        };
                                                    },
                                                    getActionFilter: function() {
                                                        return actionFilters.ipAddress;
                                                    },
                                                    jobId: data.createremoteaccessvpnresponse.jobid
                                                }
                                            });
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                },
                                messages: {
                                    confirm: function(args) {
                                        return 'message.enable.vpn';
                                    },
                                    notification: function(args) {
                                        return 'label.enable.vpn';
                                    },
                                    complete: function(args) {
                                        var msg;
                                        if (args.remoteaccessvpn.state == "Running") {
                                            msg = _l('message.enabled.vpn') + ' ' + args.remoteaccessvpn.publicip + '.' + '<br/>' + _l('message.enabled.vpn.ip.sec') + '<br/>' + args.remoteaccessvpn.presharedkey;
                                        } else {
                                            msg = _l('message.network.remote.access.vpn.configuration');
                                        }
                                        return msg;
                                    }
                                },
                                notification: {
                                    poll: pollAsyncJobResult
                                }
                            },
                            disableVPN: {
                                label: 'label.disable.vpn',
                                action: function(args) {
                                    $.ajax({
                                        url: createURL('deleteRemoteAccessVpn'),
                                        data: {
                                            publicipid: args.context.ipAddresses[0].id,
                                            domainid: args.context.ipAddresses[0].domainid
                                        },
                                        dataType: 'json',
                                        async: true,
                                        success: function(data) {
                                            args.response.success({
                                                _custom: {
                                                    getUpdatedItem: function(data) {
                                                        return {
                                                            vpnenabled: false
                                                        };
                                                    },
                                                    getActionFilter: function() {
                                                        return actionFilters.ipAddress;
                                                    },
                                                    jobId: data.deleteremoteaccessvpnresponse.jobid
                                                }
                                            });
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                },
                                messages: {
                                    confirm: function(args) {
                                        return 'message.disable.vpn';
                                    },
                                    notification: function(args) {
                                        return 'label.disable.vpn';
                                    }
                                },
                                notification: {
                                    poll: pollAsyncJobResult
                                }
                            },
                            enableStaticNAT: {
                                label: 'label.action.enable.static.NAT',

                                action: {
                                    noAdd: true,
                                    custom: cloudStack.uiCustom.enableStaticNAT({
                                        tierSelect: function(args) {
                                            if ('vpc' in args.context) { //from VPC section
                                                args.$tierSelect.show(); //show tier dropdown

                                                $.ajax({ //populate tier dropdown
                                                    url: createURL("listNetworks"),
                                                    async: false,
                                                    data: {
                                                        vpcid: args.context.vpc[0].id,
                                                        //listAll: true,  //do not pass listAll to listNetworks under VPC
                                                        domainid: args.context.vpc[0].domainid,
                                                        account: args.context.vpc[0].account,
                                                        supportedservices: 'StaticNat'
                                                    },
                                                    success: function(json) {
                                                        var networks = json.listnetworksresponse.network;
                                                        var items = [{
                                                            id: -1,
                                                            description: 'Please select a tier'
                                                        }];
                                                        $(networks).each(function() {
                                                            items.push({
                                                                id: this.id,
                                                                description: this.displaytext
                                                            });
                                                        });
                                                        args.response.success({
                                                            data: items
                                                        });
                                                    }
                                                });
                                            } else { //from Guest Network section
                                                args.$tierSelect.hide();
                                            }

                                            args.$tierSelect.change(function() {
                                                args.$tierSelect.closest('.list-view').listView('refresh');
                                            });
                                            args.$tierSelect.closest('.list-view').listView('refresh');
                                        },

                                        listView: $.extend(true, {}, cloudStack.sections.instances, {
                                            listView: {
                                                advSearchFields: null, // Not supported in dialogs right now due to display issues
                                                filters: false,
                                                subselect: {
                                                    label: 'label.use.vm.ip',
                                                    dataProvider: singleVmSecondaryIPSubselect
                                                },
                                                dataProvider: function(args) {
                                                    var data = {
                                                        page: args.page,
                                                        pageSize: pageSize,
                                                        listAll: true
                                                    };

                                                    if (args.filterBy.search.value) {
                                                        data.keyword = args.filterBy.search.value;
                                                    }

                                                    var $tierSelect = $(".ui-dialog-content").find('.tier-select select');

                                                    // if $tierSelect is not initialized, return; tierSelect() will refresh listView and come back here later
                                                    if ($tierSelect.length == 0) {
                                                        args.response.success({
                                                            data: null
                                                        });
                                                        return;
                                                    }

                                                    // if no tier is selected
                                                    if ($tierSelect.val() == '-1') {
                                                        args.response.success({
                                                            data: null
                                                        });
                                                        return;
                                                    }

                                                    if ('vpc' in args.context) {
                                                        $.extend(data, {
                                                            networkid: $tierSelect.val(),
                                                            vpcid: args.context.vpc[0].id
                                                        });
                                                    } else if ('networks' in args.context && !args.context.ipAddresses[0].isportable) {
                                                        $.extend(data, {
                                                            networkid: args.context.networks[0].id
                                                        });
                                                    }

                                                    if (!args.context.projects) {
                                                        $.extend(data, {
                                                            account: args.context.ipAddresses[0].account,
                                                            domainid: args.context.ipAddresses[0].domainid
                                                        });
                                                    }

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
                                                                            'Destroyed', 'Expunging'
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
                                        action: function(args) {
                                            var data = {
                                                ipaddressid: args.context.ipAddresses[0].id,
                                                virtualmachineid: args.context.instances[0].id
                                            };

                                            if (args.context.ipAddresses[0].isportable) {
                                                var subselect = args._subselect.split(',');
                                                var networkid = subselect[0];
                                                var vmguestip = subselect[1];

                                                data.networkid = subselect[0];

                                                if (parseInt(vmguestip) !== -1) {
                                                    data.vmguestip = vmguestip;
                                                }
                                            } else if (args._subselect && args._subselect != -1) {
                                                data.vmguestip = args._subselect;
                                            }

                                            if ('vpc' in args.context) {
                                                if (args.tierID == '-1') {
                                                    args.response.error('Tier is required');
                                                    return;
                                                }
                                                $.extend(data, {
                                                    networkid: args.tierID
                                                });
                                            }

                                            $.ajax({
                                                url: createURL('enableStaticNat'),
                                                data: data,
                                                dataType: 'json',
                                                async: true,
                                                success: function(data) {
                                                    args.response.success({});
                                                },
                                                error: function(data) {
                                                    args.response.error(parseXMLHttpResponse(data));
                                                }
                                            });
                                        }
                                    })
                                },
                                messages: {
                                    notification: function(args) {
                                        return 'label.action.enable.static.NAT';
                                    }
                                },
                                notification: {
                                    poll: function(args) {
                                        args.complete({
                                            data: {
                                                isstaticnat: true
                                            }
                                        });

                                        if (args._custom.$detailView.is(':visible')) {
                                            ipChangeNotice();
                                        }
                                    }
                                }
                            },
                            disableStaticNAT: {
                                label: 'label.action.disable.static.NAT',
                                action: function(args) {
                                    $.ajax({
                                        url: createURL('disableStaticNat'),
                                        data: {
                                            ipaddressid: args.context.ipAddresses[0].id
                                        },
                                        dataType: 'json',
                                        async: true,
                                        success: function(data) {
                                            args.response.success({
                                                _custom: {
                                                    jobId: data.disablestaticnatresponse.jobid,
                                                    getUpdatedItem: function() {
                                                        return {
                                                            isstaticnat: false,
                                                            virtualmachinedisplayname: ""
                                                        };
                                                    },
                                                    getActionFilter: function() {
                                                        return function(args) {
                                                            return ['enableStaticNAT'];
                                                        };
                                                    },
                                                    onComplete: function(args, _custom) {
                                                        if (_custom.$detailView.is(':visible')) {
                                                            ipChangeNotice();
                                                        }
                                                    }
                                                }
                                            });
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                },
                                messages: {
                                    confirm: function(args) {
                                        return 'message.action.disable.static.NAT';
                                    },
                                    notification: function(args) {
                                        return 'label.action.disable.static.NAT';
                                    }
                                },
                                notification: {
                                    poll: pollAsyncJobResult
                                }
                            },
                            remove: {
                                label: 'label.action.release.ip',
                                action: function(args) {
                                    $.ajax({
                                        url: createURL('disassociateIpAddress'),
                                        data: {
                                            id: args.context.ipAddresses[0].id
                                        },
                                        dataType: 'json',
                                        async: true,
                                        success: function(data) {
                                            args.response.success({
                                                _custom: {
                                                    jobId: data.disassociateipaddressresponse.jobid,
                                                    getActionFilter: function() {
                                                        return function(args) {
                                                            var allowedActions = ['enableStaticNAT'];

                                                            return allowedActions;
                                                        };
                                                    },
                                                    getUpdatedItem: function(args) {
                                                        return {
                                                            state: 'Released'
                                                        };
                                                    },
                                                    onComplete: function() {
                                                        $(window).trigger('cloudStack.fullRefresh');
                                                    }
                                                }
                                            });
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                },
                                messages: {
                                    confirm: function(args) {
                                        return 'message.action.release.ip';
                                    },
                                    notification: function(args) {
                                        return 'label.action.release.ip';
                                    }
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
                                    var hiddenFields = [];
                                    var zoneObj;
                                    $.ajax({
                                        url: createURL("listZones&id=" + args.context.ipAddresses[0].zoneid),
                                        dataType: "json",
                                        async: false,
                                        success: function(json) {
                                            zoneObj = json.listzonesresponse.zone[0];
                                        }
                                    });
                                    if (zoneObj.networktype == "Advanced") {
                                        hiddenFields.push("issystem");
                                        hiddenFields.push("purpose");
                                    }

                                    if (!isAdmin()) {
                                        hiddenFields.push("vlanname");
                                    }
                                    return hiddenFields;
                                },
                                fields: [{
                                    ipaddress: {
                                        label: 'label.ip'
                                    }
                                }, {
                                    isportable: {
                                        label: 'label.cross.zones',
                                        converter: function(data) {
                                            return data ? _l('label.yes') : _l('label.no');
                                        }
                                    },
                                    id: {
                                        label: 'label.id'
                                    },
                                    associatednetworkid: {
                                        label: 'label.associated.network.id'
                                    },
                                    associatednetworkname: {
                                        label: 'label.network.name'
                                    },
                                    state: {
                                        label: 'label.state'
                                    },
                                    networkid: {
                                        label: 'label.network.id'
                                    },
                                    issourcenat: {
                                        label: 'label.source.nat',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    isstaticnat: {
                                        label: 'label.static.nat',
                                        converter: cloudStack.converters.toBooleanText
                                    },
                                    vmipaddress: {
                                        label: 'label.vm.ip'
                                    },
                                    issystem: {
                                        label: 'label.is.system',
                                        converter: cloudStack.converters.toBooleanText
                                    }, //(basic zone only)
                                    purpose: {
                                        label: 'label.purpose'
                                    }, //(basic zone only) When an IP is system-generated, the purpose it serves can be Lb or static nat.
                                    virtualmachinedisplayname: {
                                        label: 'label.vm.name'
                                    },
                                    domain: {
                                        label: 'label.domain'
                                    },
                                    account: {
                                        label: 'label.account'
                                    },
                                    zonename: {
                                        label: 'label.zone'
                                    },
                                    vlanname: {
                                        label: 'label.vlan.only'
                                    }
                                }],

                                tags: cloudStack.api.tags({
                                    resourceType: 'PublicIpAddress',
                                    contextId: 'ipAddresses'
                                }),

                                dataProvider: function(args) {
                                    var items = args.context.ipAddresses;

                                    $.ajax({
                                        url: createURL('listPublicIpAddresses'),
                                        data: {
                                            id: args.context.ipAddresses[0].id
                                        },
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var ipObj = json.listpublicipaddressesresponse.publicipaddress[0];
                                            getExtaPropertiesForIpObj(ipObj, args);

                                            var network = $.grep(
                                                args.context.vpc ?
                                                    args.context.vpc[0].network : args.context.networks,
                                                function(network) {
                                                    return network.id = ipObj.associatednetworkid;
                                                })[0];

                                            args.response.success({
                                                actionFilter: actionFilters.ipAddress,
                                                data: ipObj
                                            });
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                }
                            },

                            ipRules: { //Configuration tab
                                title: 'label.configuration',
                                custom: cloudStack.ipRules({
                                    preFilter: function(args) {
                                        var disallowedActions = [];
                                        if (args.context.ipAddresses[0].isstaticnat)
                                            disallowedActions.push("nonStaticNATChart"); //tell ipRules widget to show staticNAT chart instead of non-staticNAT chart.

                                        var havingFirewallService = false;
                                        var havingPortForwardingService = false;
                                        var havingLbService = false;
                                        var havingVpnService = false;

                                        if (!('vpc' in args.context)) { //from Guest Network section
                                            var services = args.context.networks[0].service;
                                            if(services != null) {
                                                for(var i = 0; i < services.length; i++) {
                                                    var thisService = services[i];
                                                    if (thisService.name == "Firewall")
                                                        havingFirewallService = true;
                                                    if (thisService.name == "PortForwarding")
                                                        havingPortForwardingService = true;
                                                    if (thisService.name == "Lb")
                                                        havingLbService = true;
                                                    if (thisService.name == "Vpn")
                                                        havingVpnService = true;
                                                }
                                            }
                                        } else { //from VPC section
                                            //a VPC network from Guest Network section or from VPC section
                                            // Firewall is not supported in IP from VPC section
                                            // (because ACL has already supported in tier from VPC section)
                                            havingFirewallService = false;
                                            disallowedActions.push("firewall");

                                            havingVpnService = false; //VPN is not supported in IP from VPC section

                                            if (args.context.ipAddresses[0].associatednetworkid == null) { //IP is not associated with any tier yet
                                                havingPortForwardingService = true;
                                                havingLbService = true;
                                            } else { //IP is associated with a tier
                                                $.ajax({
                                                    url: createURL('listNetworks'),
                                                    data: {
                                                        listAll: true,
                                                        id: args.context.ipAddresses[0].associatednetworkid
                                                    },
                                                    async: false,
                                                    success: function(json) {
                                                        var networkObj = json.listnetworksresponse.network[0];
                                                        var services = networkObj.service;
                                                        if(services != null) {
                                                            for(var i = 0; i < services.length; i++) {
                                                                if (services[i].name == "PortForwarding")
                                                                    havingPortForwardingService = true;
                                                                if (services[i].name == "Lb")
                                                                    havingLbService = true;
                                                            }
                                                        }

                                                        if (networkObj.networkofferingconservemode == false) {
                                                            /*
                                                             (1) If IP is SourceNat, no StaticNat/VPN/PortForwarding/LoadBalancer can be enabled/added.
                                                             */
                                                            if (args.context.ipAddresses[0].issourcenat) {
                                                                if (havingFirewallService == false) { //firewall is not supported in IP from VPC section (because ACL has already supported in tier from VPC section)
                                                                    disallowedActions.push("firewall");
                                                                }

                                                                disallowedActions.push("portForwarding");
                                                                disallowedActions.push("loadBalancing");
                                                            }

                                                            /*
                                                             (2) If IP is non-SourceNat, show StaticNat/VPN/PortForwarding/LoadBalancer at first.
                                                             1. Once StaticNat is enabled, hide VPN/PortForwarding/LoadBalancer.
                                                             2. If VPN service is supported (i.e. IP comes from Guest Network section, not from VPC section), once VPN is enabled, hide StaticNat/PortForwarding/LoadBalancer.
                                                             3. Once a PortForwarding rule is added, hide StaticNat/VPN/LoadBalancer.
                                                             4. Once a LoadBalancer rule is added, hide StaticNat/VPN/PortForwarding.
                                                             */
                                                            else { //args.context.ipAddresses[0].issourcenat == false
                                                                if (havingFirewallService == false)
                                                                    disallowedActions.push("firewall");
                                                                if (havingPortForwardingService == false)
                                                                    disallowedActions.push("portForwarding");
                                                                if (havingLbService == false)
                                                                    disallowedActions.push("loadBalancing");

                                                                if (args.context.ipAddresses[0].isstaticnat) { //1. Once StaticNat is enabled, hide VPN/PortForwarding/LoadBalancer.
                                                                    disallowedActions.push("portForwarding");
                                                                    disallowedActions.push("loadBalancing");
                                                                }
                                                                if (havingVpnService && args.context.ipAddresses[0].vpnenabled) { //2. If VPN service is supported (i.e. IP comes from Guest Network section, not from VPC section), once VPN is enabled, hide StaticNat/PortForwarding/LoadBalancer.
                                                                    disallowedActions.push("portForwarding");
                                                                    disallowedActions.push("loadBalancing");
                                                                }

                                                                //3. Once a PortForwarding rule is added, hide StaticNat/VPN/LoadBalancer.
                                                                $.ajax({
                                                                    url: createURL('listPortForwardingRules'),
                                                                    data: {
                                                                        ipaddressid: args.context.ipAddresses[0].id,
                                                                        listAll: true
                                                                    },
                                                                    dataType: 'json',
                                                                    async: false,
                                                                    success: function(json) {
                                                                        // Get instance
                                                                        var rules = json.listportforwardingrulesresponse.portforwardingrule;
                                                                        if (rules != null && rules.length > 0) {
                                                                            disallowedActions.push("loadBalancing");
                                                                        }
                                                                    }
                                                                });

                                                                //4. Once a LoadBalancer rule is added, hide StaticNat/VPN/PortForwarding.
                                                                $.ajax({
                                                                    url: createURL('listLoadBalancerRules'),
                                                                    data: {
                                                                        publicipid: args.context.ipAddresses[0].id,
                                                                        listAll: true
                                                                    },
                                                                    dataType: 'json',
                                                                    async: false,
                                                                    success: function(json) {
                                                                        var rules = json.listloadbalancerrulesresponse.loadbalancerrule;
                                                                        if (rules != null && rules.length > 0) {
                                                                            disallowedActions.push("portForwarding");
                                                                        }
                                                                    }
                                                                });
                                                            }
                                                        }
                                                    }
                                                });
                                            }
                                        }

                                        return disallowedActions;
                                    },

                                    // Firewall rules
                                    firewall: {
                                        noSelect: true,
                                        fields: {
                                            'cidrlist': {
                                                edit: true,
                                                label: 'label.cidr.list'
                                            },
                                            'protocol': {
                                                label: 'label.protocol',
                                                select: function(args) {
                                                    args.$select.change(function() {
                                                        var $inputs = args.$form.find('input');
                                                        var $icmpFields = $inputs.filter(function() {
                                                            var name = $(this).attr('name');

                                                            return $.inArray(name, [
                                                                'icmptype',
                                                                'icmpcode'
                                                            ]) > -1;
                                                        });
                                                        var $otherFields = $inputs.filter(function() {
                                                            var name = $(this).attr('name');

                                                            return name != 'icmptype' && name != 'icmpcode' && name != 'cidrlist';
                                                        });

                                                        if ($(this).val() == 'icmp') {
                                                            $icmpFields.show();
                                                            $icmpFields.attr('disabled', false);
                                                            $otherFields.attr('disabled', 'disabled');
                                                            $otherFields.hide();
                                                            $otherFields.parent().find('label.error').hide();
                                                        } else {
                                                            $otherFields.show();
                                                            $otherFields.parent().find('label.error').hide();
                                                            $otherFields.attr('disabled', false);
                                                            $icmpFields.attr('disabled', 'disabled');
                                                            $icmpFields.hide();
                                                            $icmpFields.parent().find('label.error').hide();
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
                                                        }]
                                                    });
                                                }
                                            },
                                            'startport': {
                                                edit: true,
                                                label: 'label.start.port',
                                                isOptional: true
                                            },
                                            'endport': {
                                                edit: true,
                                                label: 'label.end.port',
                                                isOptional: true
                                            },
                                            'icmptype': {
                                                edit: true,
                                                label: 'ICMP.type',
                                                isDisabled: true
                                            },
                                            'icmpcode': {
                                                edit: true,
                                                label: 'ICMP.code',
                                                isDisabled: true
                                            },
                                            'add-rule': {
                                                label: 'label.add.rule',
                                                addButton: true
                                            },
                                            'state' : {
                                                edit: 'ignore',
                                                label: 'label.state'
                                            }
                                        },

                                        tags: cloudStack.api.tags({
                                            resourceType: 'FirewallRule',
                                            contextId: 'multiRule'
                                        }),

                                        add: {
                                            label: 'label.add',
                                            action: function(args) {
                                                $.ajax({
                                                    url: createURL('createFirewallRule'),
                                                    data: $.extend(args.data, {
                                                        ipaddressid: args.context.ipAddresses[0].id
                                                    }),
                                                    dataType: 'json',
                                                    success: function(data) {
                                                        args.response.success({
                                                            _custom: {
                                                                jobId: data.createfirewallruleresponse.jobid
                                                            },
                                                            notification: {
                                                                label: 'label.add.firewall',
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
                                                label: 'label.action.delete.firewall',
                                                action: function(args) {
                                                    $.ajax({
                                                        url: createURL('deleteFirewallRule'),
                                                        data: {
                                                            id: args.context.multiRule[0].id
                                                        },
                                                        dataType: 'json',
                                                        async: true,
                                                        success: function(data) {
                                                            var jobID = data.deletefirewallruleresponse.jobid;

                                                            args.response.success({
                                                                _custom: {
                                                                    jobId: jobID
                                                                },
                                                                notification: {
                                                                    label: 'label.action.delete.firewall',
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
                                            $.ajax({
                                                url: createURL('listFirewallRules'),
                                                data: {
                                                    listAll: true,
                                                    ipaddressid: args.context.ipAddresses[0].id
                                                },
                                                dataType: 'json',
                                                async: true,
                                                success: function(data) {
                                                    args.response.success({
                                                        data: data.listfirewallrulesresponse.firewallrule
                                                    });
                                                },
                                                error: function(data) {
                                                    args.response.error(parseXMLHttpResponse(data));
                                                }
                                            });
                                        }
                                    },

                                    staticNATDataProvider: function(args) {
                                        $.ajax({
                                            url: createURL('listPublicIpAddresses'),
                                            data: {
                                                id: args.context.ipAddresses[0].id,
                                                listAll: true
                                            },
                                            dataType: 'json',
                                            async: true,
                                            success: function(data) {
                                                var ipObj = data.listpublicipaddressesresponse.publicipaddress[0];
                                                getExtaPropertiesForIpObj(ipObj, args);

                                                args.response.success({
                                                    data: ipObj
                                                });
                                            },
                                            error: function(data) {
                                                args.response.error(parseXMLHttpResponse(data));
                                            }
                                        });
                                    },

                                    vmDataProvider: function(args) {
                                        $.ajax({
                                            url: createURL('listVirtualMachines'),
                                            data: {
                                                id: args.context.ipAddresses[0].virtualmachineid,
                                                listAll: true
                                            },
                                            dataType: 'json',
                                            async: true,
                                            success: function(data) {
                                                args.response.success({
                                                    data: data.listvirtualmachinesresponse.virtualmachine[0]
                                                });
                                            },
                                            error: function(data) {
                                                args.response.error(parseXMLHttpResponse(data));
                                            }
                                        });
                                    },

                                    vmDetails: cloudStack.sections.instances.listView.detailView,

                                    // Load balancing rules
                                    loadBalancing: {
                                        listView: $.extend(true, {}, cloudStack.sections.instances, {
                                            listView: {
                                                fields: {
                                                    name: {
                                                        label: 'label.name'
                                                    },
                                                    displayname: {
                                                        label: 'label.display.name'
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
                                                filters: false,

                                                //when server-side change of adding new parameter "vmidipmap" to assignToLoadBalancerRule API is in, uncomment the following commented 4 lines.
                                                subselect: {
                                                    isMultiple: true,
                                                    label: 'label.use.vm.ips',
                                                    dataProvider: multipleVmSecondaryIPSubselect
                                                },

                                                dataProvider: function(args) {
                                                    var itemData = $.isArray(args.context.multiRule) && args.context.subItemData ? args.context.subItemData : [];

                                                    var data = {};
                                                    listViewDataProvider(args, data);

                                                    var networkid;
                                                    if ('vpc' in args.context) {
                                                        networkid = args.context.multiData.tier;
                                                    } else {
                                                        networkid = args.context.ipAddresses[0].associatednetworkid;
                                                    }
                                                    $.extend(data, {
                                                        networkid: networkid
                                                    });

                                                    if (!args.context.projects) {
                                                        $.extend(data, {
                                                            account: args.context.ipAddresses[0].account,
                                                            domainid: args.context.ipAddresses[0].domainid
                                                        });
                                                    }

                                                    $.ajax({
                                                        url: createURL('listVirtualMachines'),
                                                        data: data,
                                                        dataType: 'json',
                                                        async: true,
                                                        success: function(data) {
                                                            var vmData = $.grep(
                                                                data.listvirtualmachinesresponse.virtualmachine ?
                                                                    data.listvirtualmachinesresponse.virtualmachine : [],
                                                                function(instance) {
                                                                    //Hiding the autoScale VMs
                                                                    var nonAutoScale = 0;
                                                                    if (instance.displayname == null)
                                                                        nonAutoScale = 1
                                                                    else {
                                                                        if (instance.displayname.match(/AutoScale-LB-/) == null)
                                                                            nonAutoScale = 1;
                                                                        else {
                                                                            if (instance.displayname.match(/AutoScale-LB-/).length)
                                                                                nonAutoScale = 0;
                                                                        }
                                                                    }
                                                                    var isActiveState = $.inArray(instance.state, ['Destroyed', 'Expunging']) == -1;
                                                                    var notExisting = !$.grep(itemData, function(item) {
                                                                        return item.id == instance.id;
                                                                    }).length;

                                                                    // Check if there are any remaining IPs
                                                                    if (!notExisting) {
                                                                        $.ajax({
                                                                            url: createURL('listNics'),
                                                                            async: false,
                                                                            data: {
                                                                                virtualmachineid: instance.id
                                                                            },
                                                                            success: function(json) {
                                                                                var nics = json.listnicsresponse.nic;

                                                                                $(nics).map(function (index, nic) {
                                                                                    if (nic.secondaryip) {
                                                                                        var targetIPs = $(nic.secondaryip).map(function (index, sip) {
                                                                                            return sip.ipaddress;
                                                                                        });

                                                                                        var lbIPs = $(itemData).map(function(index, item) { return item.itemIp; });

                                                                                        targetIPs.push(nic.ipaddress);

                                                                                        var matchingIPs = $.grep(targetIPs, function(item) {
                                                                                            return $.inArray(item, lbIPs) > -1;
                                                                                        });

                                                                                        if (targetIPs.length - matchingIPs.length) {
                                                                                            notExisting = true;

                                                                                            return false;
                                                                                        }
                                                                                    }
                                                                                });
                                                                            }
                                                                        })
                                                                    }

                                                                    return nonAutoScale && isActiveState && notExisting;
                                                                }
                                                            );

                                                            args.response.success({
                                                                data: vmData
                                                            });
                                                        },
                                                        error: function(data) {
                                                            args.response.error(parseXMLHttpResponse(data));
                                                        }
                                                    });
                                                }
                                            }
                                        }),
                                        headerFields: {
                                            tier: {
                                                label: 'label.tier',
                                                select: function(args) {
                                                    if ('vpc' in args.context) {
                                                        var data = {
                                                            //listAll: true,  //do not pass listAll to listNetworks under VPC
                                                            domainid: args.context.vpc[0].domainid,
                                                            account: args.context.vpc[0].account,
                                                            supportedservices: 'Lb'
                                                        };
                                                        if (args.context.ipAddresses[0].associatednetworkid == null) {
                                                            $.extend(data, {
                                                                vpcid: args.context.vpc[0].id,
                                                                domainid: args.context.vpc[0].domainid,
                                                                account: args.context.vpc[0].account
                                                            });
                                                        } else {
                                                            $.extend(data, {
                                                                id: args.context.ipAddresses[0].associatednetworkid
                                                            });
                                                        }

                                                        $.ajax({
                                                            url: createURL("listNetworks"),
                                                            data: data,
                                                            success: function(json) {
                                                                var networks = json.listnetworksresponse.network;
                                                                var items = [];
                                                                $(networks).each(function() {
                                                                    items.push({
                                                                        id: this.id,
                                                                        description: this.displaytext
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
                                        multipleAdd: true,

                                        fields: {
                                            'name': {
                                                edit: true,
                                                label: 'label.name',
                                                isEditable: true
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
                                                isEditable: true,
                                                select: function(args) {
                                                    var data = [{
                                                        id: 'roundrobin',
                                                        name: 'roundrobin',
                                                        description: _l('label.lb.algorithm.roundrobin')
                                                    }, {
                                                        id: 'leastconn',
                                                        name: 'leastconn',
                                                        description: _l('label.lb.algorithm.leastconn')
                                                    }, {
                                                        id: 'source',
                                                        name: 'source',
                                                        description: _l('label.lb.algorithm.source')
                                                    }];
                                                    if (typeof args.context != 'undefined') {
                                                        var lbAlgs = getLBAlgorithms(args.context.networks[0]);
                                                        data = (lbAlgs.length == 0) ? data : lbAlgs;
                                                    }
                                                    args.response.success({
                                                        data: data
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

                                            'protocol': {
                                                label: 'label.protocol',
                                                isEditable: true,
                                                select: function(args) {
                                                    var data = [{
                                                            id: 'ssl',
                                                            name: 'ssl',
                                                            description: _l('label.lb.protocol.ssl')
                                                        }, {
                                                            id: 'tcp',
                                                            name: 'tcp',
                                                            description: _l('label.lb.protocol.tcp')
                                                        }, {
                                                            id: 'udp',
                                                            name: 'udp',
                                                            description: _l('label.lb.protocol.udp')
                                                        }];
                                                    if (typeof args.context != 'undefined') {
                                                        var lbProtocols = getLBProtocols(args.context.networks[0]);
                                                        data = (lbProtocols.length == 0) ? data : lbProtocols;
                                                    }
                                                    args.response.success({
                                                        data: data
                                                    });
                                                }
                                            },

                                            'sslcertificate': {
                                                label: 'label.update.ssl',
                                                custom: {
                                                    buttonLabel: 'label.configure',
                                                    action: cloudStack.lbCertificatePolicy.dialog()
                                                }
                                            },

                                            'health-check': {
                                                label: 'label.health.check',
                                                custom: {
                                                    requireValidation: true,
                                                    buttonLabel: 'Configure',
                                                    action: cloudStack.uiCustom.healthCheck()
                                                },
                                                isHidden: function(args) {
                                                    if (!('vpc' in args.context)) {  //From Guest Network section
                                                        var lbProviderIsNetscaler = false;
                                                        $.ajax({
                                                            url: createURL('listNetworkOfferings'),
                                                            data: {
                                                                id: args.context.networks[0].networkofferingid
                                                            },
                                                            async: false,
                                                            success: function(json) {
                                                                var networkOffering = json.listnetworkofferingsresponse.networkoffering[0];
                                                                var services = networkOffering.service;
                                                                lbProviderIsNetscaler = checkIfNetScalerProviderIsEnabled(services);
                                                            }
                                                        });
                                                        if (lbProviderIsNetscaler == true) { //Health-Check is only supported on Netscaler (but not on any other provider)
                                                            return false; //Show Health-Check button
                                                        } else {
                                                            return 2; //Hide Health-Check button (Both Header and Form)
                                                        }
                                                    } else { //From VPC section
                                                        var lbProviderIsNetscaler;
                                                        var services = args.context.vpc[0].service;
                                                        lbProviderIsNetscaler = checkIfNetScalerProviderIsEnabled(services);
                                                        if (lbProviderIsNetscaler == true) { //Health-Check is only supported on Netscaler (but not on any other provider)
                                                            return false; //Show Health-Check button
                                                        } else {
                                                            return 2; //Hide Health-Check button (both Header and Form)
                                                        }
                                                    }
                                                }
                                            },

                                            'autoScale': {
                                                label: 'label.autoscale',
                                                custom: {
                                                    requireValidation: true,
                                                    buttonLabel: 'label.configure',
                                                    action: cloudStack.uiCustom.autoscaler(cloudStack.autoscaler)
                                                },
                                                isHidden: function(args) {
                                                    if (!('vpc' in args.context)) {  //from Guest Network section
                                                        var lbProviderIsNetscaler = false;
                                                        $.ajax({
                                                            url: createURL('listNetworkOfferings'),
                                                            data: {
                                                                id: args.context.networks[0].networkofferingid
                                                            },
                                                            async: false,
                                                            success: function(json) {
                                                                var networkOffering = json.listnetworkofferingsresponse.networkoffering[0];
                                                                var services = networkOffering.service;
                                                                lbProviderIsNetscaler = checkIfNetScalerProviderIsEnabled(services);
                                                            }
                                                        });
                                                        if (lbProviderIsNetscaler == true) { //AutoScale is only supported on Netscaler (but not on any other provider like VirtualRouter)
                                                            return false; //show AutoScale button
                                                        } else {
                                                            return 2; //hide Autoscale button (both header and form)
                                                        }
                                                    } else { //from VPC section
                                                        var lbProviderIsNetscaler;
                                                        var services = args.context.vpc[0].service;

                                                        lbProviderIsNetscaler = checkIfNetScalerProviderIsEnabled(services);

                                                        if (lbProviderIsNetscaler == true) { //AutoScale is only supported on Netscaler (but not on any other provider like VirtualRouter)
                                                            return false; //show AutoScale button
                                                        } else {
                                                            return 2; //hide Autoscale button (both header and form)
                                                        }
                                                    }
                                                }
                                            },
                                            'add-vm': {
                                                label: 'label.add.vms',
                                                addButton: true
                                            },
                                            'state' : {
                                                edit: 'ignore',
                                                label: 'label.state'
                                            }
                                        },

                                        tags: cloudStack.api.tags({
                                            resourceType: 'LoadBalancer',
                                            contextId: 'multiRule'
                                        }),

                                        add: {
                                            label: 'label.add.vms',
                                            action: function(args) {
                                                var networkid;
                                                if ('vpc' in args.context) { //from VPC section
                                                    if (args.data.tier == null) {
                                                        args.response.error('Tier is required');
                                                        return;
                                                    }
                                                    networkid = args.data.tier;
                                                } else if ('networks' in args.context) { //from Guest Network section
                                                    networkid = args.context.networks[0].id;
                                                }
                                                var data = {
                                                    algorithm: args.data.algorithm,
                                                    name: args.data.name,
                                                    privateport: args.data.privateport,
                                                    publicport: args.data.publicport,
                                                    openfirewall: false,
                                                    networkid: networkid,
                                                    publicipid: args.context.ipAddresses[0].id,
                                                    protocol: args.data.protocol
                                                };

                                                var stickyData = $.extend(true, {}, args.data.sticky);
                                                var certificateData = $.extend(true, {}, args.data.sslcertificate);

                                                //***** create new LB rule > Add VMs *****
                                                $.ajax({
                                                    url: createURL('createLoadBalancerRule'),
                                                    data: data,
                                                    dataType: 'json',
                                                    async: true,
                                                    success: function(data) {
                                                        var itemData = args.itemData;
                                                        var jobID = data.createloadbalancerruleresponse.jobid;
                                                        var lbID = data.createloadbalancerruleresponse.id;

                                                        var inputData = {
                                                        	id: data.createloadbalancerruleresponse.id
                                                        };

                                                        var selectedVMs = args.itemData;
                                                        if (selectedVMs != null) {
                                                        	var vmidipmapIndex = 0;
                                                    		for (var vmIndex = 0; vmIndex < selectedVMs.length; vmIndex++) {
                                                    			var selectedIPs = selectedVMs[vmIndex]._subselect;
                                                    			for (var ipIndex = 0; ipIndex < selectedIPs.length; ipIndex++) {
                                                    				inputData['vmidipmap[' + vmidipmapIndex + '].vmid'] = selectedVMs[vmIndex].id;

                                                    				if (args.context.ipAddresses[0].isportable) {
                                                        			    inputData['vmidipmap[' + vmidipmapIndex + '].vmip'] = selectedIPs[ipIndex].split(',')[1];
                                                        			} else {
                                                        				inputData['vmidipmap[' + vmidipmapIndex + '].vmip'] = selectedIPs[ipIndex];
                                                        			}

                                                    				vmidipmapIndex++;
                                                    			}
                                                    		}
                                                    	}

                                                        $.ajax({
                                                            url: createURL('assignToLoadBalancerRule'),
                                                            data: inputData,
                                                            success: function(data) {
                                                                var jobID = data.assigntoloadbalancerruleresponse.jobid;
                                                                var lbStickyCreated = false;
                                                                var lbCertificateCreated = false;

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
                                                                                _custom: {
                                                                                    jobId: jobID
                                                                                },
                                                                                complete: function(args) {
                                                                                    if (lbStickyCreated && lbCertificateCreated) {
                                                                                            return;
                                                                                    }

                                                                                    if (!lbStickyCreated) {
                                                                                        lbStickyCreated = true;

                                                                                        if (stickyData && stickyData.methodname && stickyData.methodname != 'None') {
                                                                                            cloudStack.lbStickyPolicy.actions.add(lbID, stickyData, complete, error);
                                                                                        }
                                                                                    }

                                                                                    if (!lbCertificateCreated) {
                                                                                        lbCertificateCreated = true;

                                                                                        if (certificateData && certificateData.certificate && certificateData.certificate != 'None') {
                                                                                            cloudStack.lbCertificatePolicy.actions.add(lbID, certificateData, complete, error);
                                                                                        } else {
                                                                                            complete();
                                                                                        }
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
                                        actions: {
                                            edit: {
                                                label: 'label.edit',
                                                action: function(args) {
                                                    $.ajax({
                                                        url: createURL('updateLoadBalancerRule'),
                                                        data: $.extend(args.data, {
                                                            id: args.context.multiRule[0].id
                                                        }),
                                                        success: function(json) {
                                                            args.response.success({
                                                                _custom: {
                                                                    jobId: json.updateloadbalancerruleresponse.jobid
                                                                },
                                                                notification: {
                                                                    label: 'label.edit.lb.rule',
                                                                    poll: pollAsyncJobResult
                                                                }
                                                            });
                                                        }
                                                    });
                                                }
                                            },
                                            destroy: {
                                                label: 'label.action.delete.load.balancer',
                                                action: function(args) {
                                                    $.ajax({
                                                        url: createURL('deleteLoadBalancerRule'),
                                                        data: {
                                                            id: args.context.multiRule[0].id
                                                        },
                                                        dataType: 'json',
                                                        async: true,
                                                        success: function(data) {
                                                            var jobID = data.deleteloadbalancerruleresponse.jobid;

                                                            args.response.success({
                                                                _custom: {
                                                                    jobId: jobID
                                                                },
                                                                notification: {
                                                                    label: 'label.action.delete.load.balancer',
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

                                        itemActions: {
                                            //***** update existing LB rule > Add VMs *****
                                            add: {
                                                label: 'label.add.vms.to.lb',
                                                action: function(args) {
                                                    var inputData = {
                                                        id: args.multiRule.id
                                                    };

                                                    /*
                                                     * e.g. first VM(xxx) has two IPs(10.1.1.~), second VM(yyy) has three IPs(10.2.2.~):
                                                     * vmidipmap[0].vmid=xxx  vmidipmap[0].vmip=10.1.1.11
                                                     * vmidipmap[1].vmid=xxx  vmidipmap[1].vmip=10.1.1.12
                                                     * vmidipmap[2].vmid=yyy  vmidipmap[2].vmip=10.2.2.77
                                                     * vmidipmap[3].vmid=yyy  vmidipmap[3].vmip=10.2.2.78
                                                     * vmidipmap[4].vmid=yyy  vmidipmap[4].vmip=10.2.2.79
                                                     */
                                                    var selectedVMs = args.data;
                                                    if (selectedVMs != null) {
                                                        var vmidipmapIndex = 0;
                                                        for (var vmIndex = 0; vmIndex < selectedVMs.length; vmIndex++) {
                                                            var selectedIPs = selectedVMs[vmIndex]._subselect;
                                                            for (var ipIndex = 0; ipIndex < selectedIPs.length; ipIndex++) {
                                                                inputData['vmidipmap[' + vmidipmapIndex + '].vmid'] = selectedVMs[vmIndex].id;

                                                                if (args.context.ipAddresses[0].isportable) {
                                                                    inputData['vmidipmap[' + vmidipmapIndex + '].vmip'] = selectedIPs[ipIndex].split(',')[1];
                                                                } else {
                                                                    inputData['vmidipmap[' + vmidipmapIndex + '].vmip'] = selectedIPs[ipIndex];
                                                                }

                                                                vmidipmapIndex++;
                                                            }
                                                        }
                                                    }

                                                    $.ajax({
                                                        url: createURL('assignToLoadBalancerRule'),
                                                        data: inputData,
                                                        success: function(json) {
                                                            args.response.success({
                                                                notification: {
                                                                    _custom: {
                                                                        jobId: json.assigntoloadbalancerruleresponse.jobid
                                                                    },
                                                                    desc: 'label.add.vms.to.lb',
                                                                    poll: pollAsyncJobResult
                                                                }
                                                            });
                                                        },
                                                        error: function(json) {
                                                            args.response.error();
                                                            cloudStack.dialog.notice({
                                                                message: parseXMLHttpResponse(json)
                                                            });
                                                        }
                                                    });
                                                }
                                            },
                                            destroy: {
                                                label: 'label.remove.vm.from.lb',
                                                action: function(args) {
                                                    var inputData;
                                                    if (args.item.itemIp == undefined) {
                                                        inputData = {
                                                            id: args.multiRule.id,
                                                            virtualmachineids: args.item.id
                                                        };
                                                    } else {
                                                        inputData = {
                                                            id: args.multiRule.id,
                                                            "vmidipmap[0].vmid": args.item.id,
                                                            "vmidipmap[0].vmip": args.item.itemIp
                                                        };
                                                    }

                                                    $.ajax({
                                                        url: createURL('removeFromLoadBalancerRule'),
                                                        data: inputData,
                                                        success: function(json) {
                                                            args.response.success({
                                                                notification: {
                                                                    _custom: {
                                                                        jobId: json.removefromloadbalancerruleresponse.jobid
                                                                    },
                                                                    desc: 'label.remove.vm.from.lb',
                                                                    poll: pollAsyncJobResult
                                                                }
                                                            });
                                                        },
                                                        error: function(json) {
                                                            args.response.error();
                                                            cloudStack.dialog.notice({
                                                                message: parseXMLHttpResponse(json)
                                                            });
                                                        }
                                                    });
                                                }
                                            }
                                        },
                                        dataProvider: function(args) {
                                            var $multi = args.$multi;

                                            $.ajax({
                                                url: createURL('listLoadBalancerRules'),
                                                data: {
                                                    publicipid: args.context.ipAddresses[0].id,
                                                    listAll: true
                                                },
                                                dataType: 'json',
                                                async: true,
                                                success: function(data) {
                                                    var loadbalancerrules = data.listloadbalancerrulesresponse.loadbalancerrule;

                                                    $(loadbalancerrules).each(function() {
                                                        var lbRule = this;
                                                        var stickyData = {};
                                                        var sslCertData = {};
                                                        //var lbInstances = [];
                                                        var itemData = [];

                                                        // Passing _hideFields array will disable specified fields for this row
                                                        //lbRule._hideFields = ['autoScale'];

                                                        $.ajax({
                                                            url: createURL('listAutoScaleVmGroups'),
                                                            data: {
                                                                listAll: true,
                                                                lbruleid: lbRule.id
                                                            },
                                                            async: false,
                                                            success: function(json) {
                                                                if (json.listautoscalevmgroupsresponse.autoscalevmgroup != null && json.listautoscalevmgroupsresponse.autoscalevmgroup.length > 0) { //from 'autoScale' button
                                                                    lbRule._hideFields = ['add-vm'];
                                                                } else { //from 'add-vm' button
                                                                    lbRule._hideFields = ['autoScale'];
                                                                }
                                                            }
                                                        });

                                                        // Get sticky data
                                                        $.ajax({
                                                            url: createURL('listLBStickinessPolicies'),
                                                            async: false,
                                                            data: {
                                                                listAll: true,
                                                                lbruleid: lbRule.id
                                                            },
                                                            success: function(json) {
                                                                var stickyPolicy = json.listlbstickinesspoliciesresponse.stickinesspolicies ?
                                                                    json.listlbstickinesspoliciesresponse.stickinesspolicies[0].stickinesspolicy : null;

                                                                if (stickyPolicy && stickyPolicy.length) {
                                                                    stickyPolicy = stickyPolicy[0];

                                                                    if (!stickyPolicy.methodname) stickyPolicy.methodname = 'None';

                                                                    stickyData = {
                                                                        _buttonLabel: stickyPolicy.methodname,
                                                                        methodname: stickyPolicy.methodname,
                                                                        stickyName: stickyPolicy.name,
                                                                        id: stickyPolicy.id,
                                                                        lbRuleID: lbRule.id
                                                                    };
                                                                    $.extend(stickyData, stickyPolicy.params);
                                                                } else {
                                                                    stickyData = {
                                                                        lbRuleID: lbRule.id
                                                                    };
                                                                }
                                                            },
                                                            error: function(json) {
                                                                cloudStack.dialog.notice({
                                                                    message: parseXMLHttpResponse(json)
                                                                });
                                                            }
                                                        });

                                                        // Get SSL Certificate data
                                                        $.ajax({
                                                            url: createURL('listSslCerts'),
                                                            data: {
                                                                listAll: true,
                                                                lbruleid: lbRule.id
                                                            },
                                                            async: false,
                                                            success: function(json) {
                                                                if (json.listsslcertsresponse != null) {
                                                                    lbRule._hideFields.push('sslcertificate');
                                                                }
                                                            }
                                                        });

                                                        // Get instances
                                                        $.ajax({
                                                            url: createURL('listLoadBalancerRuleInstances'),
                                                            dataType: 'json',
                                                            async: false,
                                                            data: {
                                                                listAll: true,
                                                                lbvmips: true,
                                                                id: lbRule.id
                                                            },
                                                            success: function(data) {
                                                                //when "lbvmips: true" is not passed to API
                                                                //lbVMs = data.listloadbalancerruleinstancesresponse.loadbalancerruleinstance;

                                                                //when "lbvmips: true" is passed to API
                                                                lbrulevmidips = data.listloadbalancerruleinstancesresponse.lbrulevmidip;

                                                                if (lbrulevmidips != null) {
                                                                    for (var k = 0; k < lbrulevmidips.length; k++) {
                                                                        var lbrulevmidip = lbrulevmidips[k];
                                                                        var lbVM = lbrulevmidip.loadbalancerruleinstance;
                                                                        if (lbVM.displayname.indexOf('AutoScale-LB-') > -1) //autoscale VM is not allowed to be deleted manually. So, hide destroy button
                                                                            lbVM._hideActions = ['destroy'];

                                                                        if (lbVM.servicestate) {
                                                                            lbVM._itemStateLabel = 'label.service.state';
                                                                            lbVM._itemState = lbVM.servicestate;
                                                                        }

                                                                        if (lbrulevmidip.lbvmipaddresses != null) {
                                                                            for (var m = 0 ; m < lbrulevmidip.lbvmipaddresses.length; m++) {
                                                                                var ip = lbrulevmidip.lbvmipaddresses[m];
                                                                                itemData.push($.extend({}, lbVM, {
                                                                                    itemIp: ip
                                                                                }));
                                                                            }
                                                                        } else {
                                                                            itemData.push(lbVM);
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            error: function(data) {
                                                                args.response.error(parseXMLHttpResponse(data));
                                                            }
                                                        });

                                                        $.extend(lbRule, {
                                                            _itemName: 'name',
                                                            _itemIp: 'itemIp',
                                                            _itemData: itemData,
                                                            _maxLength: {
                                                                name: 7
                                                            },
                                                            sticky: stickyData,
                                                            autoScale: {
                                                                lbRuleID: lbRule.id
                                                            }
                                                        });
                                                    });

                                                    args.response.success({
                                                        data: loadbalancerrules
                                                    });
                                                }
                                            });

                                            // Check if tiers are present; hide/show header drop-down (begin) ***
                                            //dataProvider() is called when a LB rule is added in multiEdit. However, adding a LB rule might change parent object (IP Address object). So, we have to force to refresh args.context.ipAddresses[0] here
                                            $.ajax({
                                                url: createURL('listPublicIpAddresses'),
                                                data: {
                                                    id: args.context.ipAddresses[0].id,
                                                    listAll: true
                                                },
                                                success: function(json) {
                                                    var ipObj = json.listpublicipaddressesresponse.publicipaddress[0];
                                                    getExtaPropertiesForIpObj(ipObj, args);

                                                    args.context.ipAddresses.shift(); //remove the first element in args.context.ipAddresses
                                                    args.context.ipAddresses.push(ipObj);

                                                    var $headerFields = $multi.find('.header-fields');
                                                    if ('vpc' in args.context) {
                                                        if (args.context.ipAddresses[0].associatednetworkid == null) {
                                                            $headerFields.show();
                                                        } else {
                                                            $headerFields.hide();
                                                        }
                                                    } else if ('networks' in args.context) {
                                                        $headerFields.hide();
                                                    }
                                                }
                                            });
                                            // Check if tiers are present; hide/show header drop-down (end) ***
                                        }
                                    },

                                    // Port forwarding rules
                                    portForwarding: {
                                        headerFields: {
                                            tier: {
                                                label: 'label.tier',
                                                select: function(args) {
                                                    if ('vpc' in args.context) {
                                                        var data = {
                                                            //listAll: true,  //do not pass listAll to listNetworks under VPC
                                                            domainid: args.context.vpc[0].domainid,
                                                            account: args.context.vpc[0].account,
                                                            supportedservices: 'PortForwarding'
                                                        };
                                                        if (args.context.ipAddresses[0].associatednetworkid == null) {
                                                            $.extend(data, {
                                                                vpcid: args.context.vpc[0].id,
                                                                domainid: args.context.vpc[0].domainid,
                                                                account: args.context.vpc[0].account
                                                            });
                                                        } else {
                                                            $.extend(data, {
                                                                id: args.context.ipAddresses[0].associatednetworkid
                                                            });
                                                        }
                                                        $.ajax({
                                                            url: createURL("listNetworks"),
                                                            data: data,
                                                            success: function(json) {
                                                                var networks = json.listnetworksresponse.network;
                                                                var items = [];
                                                                $(networks).each(function() {
                                                                    items.push({
                                                                        id: this.id,
                                                                        description: this.displaytext
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
                                        listView: $.extend(true, {}, cloudStack.sections.instances, {
                                            listView: {
                                                filters: false,
                                                subselect: {
                                                    label: 'label.use.vm.ip',
                                                    dataProvider: singleVmSecondaryIPSubselect
                                                },
                                                dataProvider: function(args) {
                                                    var data = {};
                                                    listViewDataProvider(args, data);

                                                    var networkid;
                                                    if ('vpc' in args.context) {
                                                        networkid = args.context.multiData.tier;
                                                    } else {
                                                        networkid = args.context.ipAddresses[0].associatednetworkid;
                                                    }
                                                    $.extend(data, {
                                                        networkid: networkid
                                                    });

                                                    if (!args.context.projects) {
                                                        $.extend(data, {
                                                            account: args.context.ipAddresses[0].account,
                                                            domainid: args.context.ipAddresses[0].domainid
                                                        });
                                                    }

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
                                                                            'Destroyed', 'Expunging'
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
                                        fields: {
                                            //'private-ports': {
                                            privateport: {
                                                edit: true,
                                                label: 'label.private.port',
                                                //range: ['privateport', 'privateendport']  //Bug 13427 - Don't allow port forwarding ranges in the CreatePortForwardingRule API
                                                range: ['privateport', 'privateendport'] //Bug 16344 (restore port range back) (http://bugs.cloudstack.org/browse/CS-16344)
                                            },
                                            //'public-ports': {
                                            publicport: {
                                                edit: true,
                                                label: 'label.public.port',
                                                //range: ['publicport', 'publicendport']  //Bug 13427 - Don't allow port forwarding ranges in the CreatePortForwardingRule API
                                                range: ['publicport', 'publicendport'] //Bug 16344 (restore port range back) (http://bugs.cloudstack.org/browse/CS-16344)
                                            },
                                            'protocol': {
                                                label: 'label.protocol',
                                                select: function(args) {
                                                    args.response.success({
                                                        data: [{
                                                            name: 'tcp',
                                                            description: 'TCP'
                                                        }, {
                                                            name: 'udp',
                                                            description: 'UDP'
                                                        }]
                                                    });
                                                }
                                            },
                                            'state' : {
                                                edit: 'ignore',
                                                label: 'label.state'
                                            },
                                            'add-vm': {
                                                label: 'label.add.vm',
                                                addButton: true
                                            }
                                        },

                                        tags: cloudStack.api.tags({
                                            resourceType: 'PortForwardingRule',
                                            contextId: 'multiRule'
                                        }),

                                        add: {
                                            label: 'label.add.vm',

                                            action: function(args) {
                                                var data = {
                                                    ipaddressid: args.context.ipAddresses[0].id,
                                                    privateport: args.data.privateport,
                                                    privateendport: args.data.privateendport,
                                                    publicport: args.data.publicport,
                                                    publicendport: args.data.publicendport,
                                                    protocol: args.data.protocol,
                                                    virtualmachineid: args.itemData[0].id,
                                                    openfirewall: false
                                                };

                                                if (args.context.ipAddresses[0].isportable) {
                                                    var subselect = args.itemData[0]._subselect.split(',');
                                                    //var networkid = subselect[0];
                                                    var vmguestip = subselect[1];

                                                    //data.networkid = networkid;

                                                    if (parseInt(vmguestip) !== -1) {
                                                        data.vmguestip = vmguestip;
                                                    }
                                                } else if (args.itemData[0]._subselect && args.itemData[0]._subselect != -1) {
                                                    data.vmguestip = args.itemData[0]._subselect;
                                                }

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

                                                $.ajax({
                                                    url: createURL('createPortForwardingRule'),
                                                    data: data,
                                                    success: function(data) {
                                                        args.response.success({
                                                            _custom: {
                                                                jobId: data.createportforwardingruleresponse.jobid,
                                                                getUpdatedItem: function(json) {
                                                                    return json.queryasyncjobresultresponse.jobresult.portforwardingrule;
                                                                }
                                                            },
                                                            notification: {
                                                                label: 'label.add.port.forwarding.rule',
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
                                                label: 'label.remove.pf',
                                                action: function(args) {
                                                    $.ajax({
                                                        url: createURL('deletePortForwardingRule'),
                                                        data: {
                                                            id: args.context.multiRule[0].id
                                                        },
                                                        dataType: 'json',
                                                        async: true,
                                                        success: function(data) {
                                                            var jobID = data.deleteportforwardingruleresponse.jobid;

                                                            args.response.success({
                                                                _custom: {
                                                                    jobId: jobID
                                                                },
                                                                notification: {
                                                                    label: 'label.remove.pf',
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

                                            $.ajax({
                                                url: createURL('listPortForwardingRules'),
                                                data: {
                                                    ipaddressid: args.context.ipAddresses[0].id,
                                                    listAll: true
                                                },
                                                dataType: 'json',
                                                async: true,
                                                success: function(data) {
                                                    // Get instance
                                                    var portForwardingData = data
                                                        .listportforwardingrulesresponse.portforwardingrule;
                                                    var loadTotal = portForwardingData ? portForwardingData.length : 0;
                                                    var loadCurrent = 0;

                                                    $(portForwardingData).each(function() {
                                                        var item = this;

                                                        item._itemName = '_displayName';

                                                        $.ajax({
                                                            url: createURL('listVirtualMachines'),
                                                            dataType: 'json',
                                                            async: true,
                                                            data: {
                                                                listAll: true,
                                                                id: item.virtualmachineid
                                                            },
                                                            success: function(data) {
                                                                loadCurrent++;
                                                                var vms = data.listvirtualmachinesresponse.virtualmachine;

                                                                //if this VM is destroyed, data.listvirtualmachinesresponse.virtualmachine will be undefined for regular-user (CLOUDSTACK-3195)
                                                                if (vms == undefined) {
                                                                    vms = [{
                                                                        "id": item.virtualmachineid,
                                                                        "name": item.virtualmachinename,
                                                                        "displayname": item.virtualmachinedisplayname
                                                                    }];
                                                                }

                                                                $.extend(item, {
                                                                    _itemData: $.map(vms, function(vm) {
                                                                        return $.extend(vm, {
                                                                            _displayName: '<p>VM: ' + vm.name + '</p>' + '<p>IP: ' + item.vmguestip + '</p>' // Also display attached IP
                                                                        });
                                                                    }),
                                                                    _context: {
                                                                        instances: vms
                                                                    }
                                                                });

                                                                if (loadCurrent == loadTotal) {
                                                                    args.response.success({
                                                                        data: portForwardingData
                                                                    });
                                                                }
                                                            }
                                                        });
                                                    });

                                                    // Check if tiers are present; hide/show header drop-down (begin) ***
                                                    //dataProvider() is called when a PF rule is added in multiEdit. However, adding a LB rule might change parent object (IP Address object). So, we have to force to refresh args.context.ipAddresses[0] here
                                                    $.ajax({
                                                        url: createURL('listPublicIpAddresses'),
                                                        data: {
                                                            id: args.context.ipAddresses[0].id,
                                                            listAll: true
                                                        },
                                                        success: function(json) {
                                                            var ipObj = json.listpublicipaddressesresponse.publicipaddress[0];
                                                            getExtaPropertiesForIpObj(ipObj, args);

                                                            args.context.ipAddresses.shift(); //remove the first element in args.context.ipAddresses
                                                            args.context.ipAddresses.push(ipObj);

                                                            var $headerFields = $multi.find('.header-fields');
                                                            if ('vpc' in args.context) {
                                                                if (args.context.ipAddresses[0].associatednetworkid == null) {
                                                                    $headerFields.show();
                                                                } else {
                                                                    $headerFields.hide();
                                                                }
                                                            } else if ('networks' in args.context) {
                                                                $headerFields.hide();
                                                            }
                                                        }
                                                    });
                                                    // Check if tiers are present; hide/show header drop-down (end) ***
                                                },
                                                error: function(data) {
                                                    args.response.error(parseXMLHttpResponse(data));
                                                }
                                            });
                                        }
                                    }
                                })
                            },
                            vpn: {
                                title: 'label.vpn',
                                custom: function(args) {
                                    var ipAddress = args.context.ipAddresses[0].ipaddress;
                                    var psk = "";
                                    if (args.context.ipAddresses[0].remoteaccessvpn != null)
                                        psk = args.context.ipAddresses[0].remoteaccessvpn.presharedkey;

                                    return $('<div>')
                                        .append(
                                        $('<ul>').addClass('info')
                                            .append(
                                            // VPN IP
                                            $('<li>').addClass('ip').html(_l('message.enabled.vpn') + ' ')
                                                .append($('<strong>').html(ipAddress))
                                        )
                                            .append(
                                            // PSK
                                            $('<li>').addClass('psk').html(_l('message.enabled.vpn.ip.sec') + ' ')
                                                .append($('<strong>').html(psk))
                                        )
                                            .append(
                                                //Note
                                                $('<li>').html(_l('message.enabled.vpn.note'))
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            securityGroups: {
                type: 'select',
                title: 'label.menu.security.groups',
                id: 'securityGroups',
                listView: {
                    id: 'securityGroups',
                    label: 'label.menu.security.groups',
                    fields: {
                        name: {
                            label: 'label.name',
                            editable: true
                        },
                        description: {
                            label: 'label.description'
                        },
                        domain: {
                            label: 'label.domain'
                        },
                        account: {
                            label: 'label.account'
                        }
                    },
                    actions: {
                        add: {
                            label: 'label.add.security.group',

                            action: function(args) {
                                $.ajax({
                                    url: createURL('createSecurityGroup'),
                                    data: {
                                        name: args.data.name,
                                        description: args.data.description
                                    },
                                    success: function(data) {
                                        args.response.success({
                                            data: data.createsecuritygroupresponse.securitygroup
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
                                        actionFilter: actionFilters.securityGroups
                                    });
                                }
                            },

                            messages: {
                                confirm: function(args) {
                                    return _l('message.question.are.you.sure.you.want.to.add') + ' ' + args.name + '?';
                                },
                                notification: function(args) {
                                    return 'label.add.security.group';
                                }
                            },

                            createForm: {
                                title: 'label.add.security.group',
                                desc: 'label.add.security.group',
                                fields: {
                                    name: {
                                        label: 'label.name'
                                    },
                                    description: {
                                        label: 'label.description'
                                    }
                                }
                            }
                        }
                    },

                    advSearchFields: {
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

                        $.ajax({
                            url: createURL('listSecurityGroups'),
                            data: data,
                            success: function(json) {
                                var items = json.listsecuritygroupsresponse.securitygroup;
                                args.response.success({
                                    actionFilter: actionFilters.securityGroups,
                                    data: items
                                });
                            }
                        });
                    },

                    detailView: {
                        name: 'Security group details',
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
                                    domain: {
                                        label: 'label.domain'
                                    },
                                    account: {
                                        label: 'label.account'
                                    }
                                }],

                                tags: cloudStack.api.tags({
                                    resourceType: 'SecurityGroup',
                                    contextId: 'securityGroups'
                                }),


                                dataProvider: function(args) {
                                    $.ajax({
                                        url: createURL("listSecurityGroups&id=" + args.id),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            var items = json.listsecuritygroupsresponse.securitygroup;
                                            if (items != null && items.length > 0) {
                                                args.response.success({
                                                    actionFilter: actionFilters.securityGroups,
                                                    data: items[0]
                                                });
                                            }
                                        }
                                    });
                                }
                            },
                            ingressRules: {
                                title: 'label.ingress.rule',
                                custom: cloudStack.uiCustom.securityRules({
                                    noSelect: true,
                                    noHeaderActionsColumn: true,
                                    fields: {
                                        'protocol': {
                                            label: 'label.protocol',
                                            select: function(args) {
                                                args.$select.change(function() {
                                                    var $inputs = args.$form.find('th, td');
                                                    var $icmpFields = $inputs.filter(function() {
                                                        var name = $(this).attr('rel');

                                                        return $.inArray(name, [
                                                            'icmptype',
                                                            'icmpcode'
                                                        ]) > -1;
                                                    });
                                                    var $otherFields = $inputs.filter(function() {
                                                        var name = $(this).attr('rel');

                                                        return name != 'icmptype' &&
                                                            name != 'icmpcode' &&
                                                            name != 'protocol' &&
                                                            name != 'add-rule' &&
                                                            name != 'cidr' &&
                                                            name != 'accountname' &&
                                                            name != 'securitygroup';
                                                    });

                                                    if ($(this).val() == 'icmp') {
                                                        $icmpFields.show();
                                                        $otherFields.hide();
                                                    } else {
                                                        $icmpFields.hide();
                                                        $otherFields.show();
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
                                                    }]
                                                });
                                            }
                                        },
                                        'startport': {
                                            edit: true,
                                            label: 'label.start.port',
                                            validation: {
                                                number: true,
                                                range: [0, 65535]
                                            }
                                        },
                                        'endport': {
                                            edit: true,
                                            label: 'label.end.port',
                                            validation: {
                                                number: true,
                                                range: [0, 65535]
                                            }
                                        },
                                        'icmptype': {
                                            edit: true,
                                            label: 'ICMP.type',
                                            isHidden: true
                                        },
                                        'icmpcode': {
                                            edit: true,
                                            label: 'ICMP.code',
                                            isHidden: true
                                        },
                                        'cidr': {
                                            edit: true,
                                            label: 'label.cidr',
                                            isHidden: true,
                                            validation: {
                                                ipv46cidrs: true
                                            }
                                        },
                                        'accountname': {
                                            edit: true,
                                            label: 'label.account.and.security.group',
                                            isHidden: true,
                                            range: ['accountname', 'securitygroup']
                                        },
                                        'add-rule': {
                                            label: 'label.add',
                                            addButton: true
                                        }
                                    },
                                    add: {
                                        label: 'label.add',
                                        action: function(args) {
                                            var data = {
                                                securitygroupid: args.context.securityGroups[0].id,
                                                protocol: args.data.protocol,
                                                domainid: args.context.securityGroups[0].domainid,
                                                account: args.context.securityGroups[0].account
                                            };

                                            if (args.data.icmptype && args.data.icmpcode) { // ICMP
                                                $.extend(data, {
                                                    icmptype: args.data.icmptype,
                                                    icmpcode: args.data.icmpcode
                                                });
                                            } else { // TCP/UDP
                                                $.extend(data, {
                                                    startport: args.data.startport,
                                                    endport: args.data.endport
                                                });
                                            }

                                            // CIDR / account
                                            if (args.data.cidr) {
                                                data.cidrlist = args.data.cidr;
                                            } else {
                                                data['usersecuritygrouplist[0].account'] = args.data.accountname;
                                                data['usersecuritygrouplist[0].group'] = args.data.securitygroup;
                                            }

                                            $.ajax({
                                                url: createURL('authorizeSecurityGroupIngress'),
                                                data: data,
                                                dataType: 'json',
                                                async: true,
                                                success: function(data) {
                                                    var jobId = data.authorizesecuritygroupingressresponse.jobid;

                                                    args.response.success({
                                                        _custom: {
                                                            jobId: jobId
                                                        },
                                                        notification: {
                                                            label: 'label.add.ingress.rule',
                                                            poll: pollAsyncJobResult
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    },
                                    actions: {
                                        destroy: {
                                            label: 'label.remove.rule',
                                            action: function(args) {
                                                $.ajax({
                                                    url: createURL('revokeSecurityGroupIngress'),
                                                    data: {
                                                        domainid: args.context.securityGroups[0].domainid,
                                                        account: args.context.securityGroups[0].account,
                                                        id: args.context.multiRule[0].id
                                                    },
                                                    dataType: 'json',
                                                    async: true,
                                                    success: function(data) {
                                                        var jobID = data.revokesecuritygroupingressresponse.jobid;

                                                        args.response.success({
                                                            _custom: {
                                                                jobId: jobID
                                                            },
                                                            notification: {
                                                                label: 'label.remove.ingress.rule',
                                                                poll: pollAsyncJobResult
                                                            }
                                                        });
                                                    },
                                                    error: function(json) {
                                                        args.response.error(parseXMLHttpResponse(json));
                                                    }
                                                });
                                            }
                                        }
                                    },
                                    ignoreEmptyFields: true,
                                    tags: cloudStack.api.tags({
                                        resourceType: 'SecurityGroupRule',
                                        contextId: 'multiRule'
                                    }),
                                    dataProvider: function(args) {
                                        $.ajax({
                                            url: createURL('listSecurityGroups'),
                                            data: {
                                                id: args.context.securityGroups[0].id
                                            },
                                            dataType: 'json',
                                            async: true,
                                            success: function(data) {
                                                args.response.success({
                                                    data: $.map(
                                                        data.listsecuritygroupsresponse.securitygroup[0].ingressrule ?
                                                            data.listsecuritygroupsresponse.securitygroup[0].ingressrule : [],
                                                        ingressEgressDataMap
                                                    )
                                                });
                                            }
                                        });
                                    }
                                })
                            },

                            egressRules: {
                                title: 'label.egress.rule',
                                custom: cloudStack.uiCustom.securityRules({
                                    noSelect: true,
                                    noHeaderActionsColumn: true,
                                    fields: {
                                        'protocol': {
                                            label: 'label.protocol',
                                            select: function(args) {
                                                args.$select.change(function() {
                                                    var $inputs = args.$form.find('th, td');
                                                    var $icmpFields = $inputs.filter(function() {
                                                        var name = $(this).attr('rel');

                                                        return $.inArray(name, [
                                                            'icmptype',
                                                            'icmpcode'
                                                        ]) > -1;
                                                    });
                                                    var $otherFields = $inputs.filter(function() {
                                                        var name = $(this).attr('rel');

                                                        return name != 'icmptype' &&
                                                            name != 'icmpcode' &&
                                                            name != 'protocol' &&
                                                            name != 'add-rule' &&
                                                            name != 'cidr' &&
                                                            name != 'accountname' &&
                                                            name != 'securitygroup';
                                                    });

                                                    if ($(this).val() == 'icmp') {
                                                        $icmpFields.show();
                                                        $otherFields.hide();
                                                    } else {
                                                        $icmpFields.hide();
                                                        $otherFields.show();
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
                                                    }]
                                                });
                                            }
                                        },
                                        'startport': {
                                            edit: true,
                                            label: 'label.start.port',
                                            validation: {
                                                number: true,
                                                range: [0, 65535]
                                            }
                                        },
                                        'endport': {
                                            edit: true,
                                            label: 'label.end.port',
                                            validation: {
                                                number: true,
                                                range: [0, 65535]
                                            }
                                        },
                                        'icmptype': {
                                            edit: true,
                                            label: 'ICMP.type',
                                            isHidden: true
                                        },
                                        'icmpcode': {
                                            edit: true,
                                            label: 'ICMP.code',
                                            isHidden: true
                                        },
                                        'cidr': {
                                            edit: true,
                                            label: 'label.cidr',
                                            isHidden: true,
                                            validation: {
                                                ipv46cidrs: true
                                            }
                                        },
                                        'accountname': {
                                            edit: true,
                                            label: 'label.account.and.security.group',
                                            isHidden: true,
                                            range: ['accountname', 'securitygroup']
                                        },
                                        'add-rule': {
                                            label: 'label.add',
                                            addButton: true
                                        }
                                    },
                                    add: {
                                        label: 'label.add',
                                        action: function(args) {
                                            var data = {
                                                securitygroupid: args.context.securityGroups[0].id,
                                                protocol: args.data.protocol,
                                                domainid: args.context.securityGroups[0].domainid,
                                                account: args.context.securityGroups[0].account
                                            };

                                            if (args.data.icmptype && args.data.icmpcode) { // ICMP
                                                $.extend(data, {
                                                    icmptype: args.data.icmptype,
                                                    icmpcode: args.data.icmpcode
                                                });
                                            } else { // TCP/UDP
                                                $.extend(data, {
                                                    startport: args.data.startport,
                                                    endport: args.data.endport
                                                });
                                            }

                                            // CIDR / account
                                            if (args.data.cidr) {
                                                data.cidrlist = args.data.cidr;
                                            } else {
                                                data['usersecuritygrouplist[0].account'] = args.data.accountname;
                                                data['usersecuritygrouplist[0].group'] = args.data.securitygroup;
                                            }

                                            $.ajax({
                                                url: createURL('authorizeSecurityGroupEgress'),
                                                data: data,
                                                dataType: 'json',
                                                async: true,
                                                success: function(data) {
                                                    var jobId = data.authorizesecuritygroupegressresponse.jobid;

                                                    args.response.success({
                                                        _custom: {
                                                            jobId: jobId
                                                        },
                                                        notification: {
                                                            label: 'label.add.egress.rule',
                                                            poll: pollAsyncJobResult
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    },
                                    actions: {
                                        destroy: {
                                            label: 'label.remove.rule',
                                            action: function(args) {
                                                $.ajax({
                                                    url: createURL('revokeSecurityGroupEgress'),
                                                    data: {
                                                        domainid: args.context.securityGroups[0].domainid,
                                                        account: args.context.securityGroups[0].account,
                                                        id: args.context.multiRule[0].id
                                                    },
                                                    dataType: 'json',
                                                    async: true,
                                                    success: function(data) {
                                                        var jobID = data.revokesecuritygroupegressresponse.jobid;

                                                        args.response.success({
                                                            _custom: {
                                                                jobId: jobID
                                                            },
                                                            notification: {
                                                                label: 'label.remove.egress.rule',
                                                                poll: pollAsyncJobResult
                                                            }
                                                        });
                                                    },
                                                    error: function(json) {
                                                        args.response.error(parseXMLHttpResponse(json));
                                                    }
                                                });
                                            }
                                        }
                                    },
                                    ignoreEmptyFields: true,
                                    tags: cloudStack.api.tags({
                                        resourceType: 'SecurityGroupRule',
                                        contextId: 'multiRule'
                                    }),
                                    dataProvider: function(args) {
                                        $.ajax({
                                            url: createURL('listSecurityGroups'),
                                            data: {
                                                id: args.context.securityGroups[0].id
                                            },
                                            dataType: 'json',
                                            async: true,
                                            success: function(data) {
                                                args.response.success({
                                                    data: $.map(
                                                        data.listsecuritygroupsresponse.securitygroup[0].egressrule ?
                                                            data.listsecuritygroupsresponse.securitygroup[0].egressrule : [],
                                                        ingressEgressDataMap
                                                    )
                                                });
                                            }
                                        });
                                    }
                                })
                            }
                        },

                        actions: {
                            remove: {
                                label: 'label.action.delete.security.group',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.action.delete.security.group';
                                    },
                                    notification: function(args) {
                                        return 'label.action.delete.security.group';
                                    }
                                },
                                action: function(args) {
                                    $.ajax({
                                        url: createURL('deleteSecurityGroup'),
                                        data: {
                                            id: args.context.securityGroups[0].id
                                        },
                                        dataType: 'json',
                                        async: true,
                                        success: function(data) {
                                            args.response.success();
                                        },
                                        error: function(json) {
                                            args.response.error(parseXMLHttpResponse(json));
                                        }
                                    });
                                },

                                notification: {
                                    poll: function(args) {
                                        args.complete({
                                            data: {
                                                state: 'Destroyed'
                                            },
                                            actionFilter: actionFilters.securityGroups
                                        });
                                    }
                                }
                            }
                        }
                    }
                }
            },
            vpc: {
                type: 'select',
                title: 'label.vpc',
                id: 'vpc',
                listView: {
                    id: 'vpc',
                    label: 'label.vpc',
                    fields: {
                        name: {
                            label: 'label.name'
                        },
                        displaytext: {
                            label: 'label.description',
                            truncate: true
                        },
                        zonename: {
                            label: 'label.zone',
                            truncate: true
                        },
                        cidr: {
                            label: 'label.cidr'
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

                        $.ajax({
                            url: createURL('listVPCs'),
                            data: data,
                            success: function(json) {
                                var items = json.listvpcsresponse.vpc ? json.listvpcsresponse.vpc : { };

                                //If we are coming from Home > Regions, show only regional vpcs
                                if (args.context.regions)
                                    items = $.grep(
                                        items,
                                        function (vpc, i) {
                                            return vpc.regionlevelvpc;
                                        });

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
                    },
                    actions: {
                        add: {
                            label: 'label.add.vpc',
                            messages: {
                                notification: function(args) {
                                    return 'label.add.vpc';
                                }
                            },
                            createForm: {
                                title: 'label.add.vpc',
                                messages: {
                                    notification: function(args) {
                                        return 'label.add.vpc';
                                    }
                                },
                                fields: {
                                    name: {
                                        label: 'label.name',
                                        docID: 'helpVPCName',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    displaytext: {
                                        label: 'label.description',
                                        docID: 'helpVPCDescription',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    zoneid: {
                                        label: 'label.zone',
                                        docID: 'helpVPCZone',
                                        validation: {
                                            required: true
                                        },
                                        select: function(args) {
                                            var data = {};
                                            $.ajax({
                                                url: createURL('listZones'),
                                                data: data,
                                                success: function(json) {
                                                    var zones = json.listzonesresponse.zone ? json.listzonesresponse.zone : [];
                                                    var advZones = $.grep(zones, function(zone) {
                                                        return zone.networktype == 'Advanced' && !zone.securitygroupsenabled;
                                                    });

                                                    args.response.success({
                                                        data: $.map(advZones, function(zone) {
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
                                    cidr: {
                                        label: 'label.super.cidr.for.guest.networks',
                                        docID: 'helpVPCSuperCIDR',
                                        validation: {
                                            required: true,
                                            ipv4cidr: true
                                        }
                                    },
                                    networkdomain: {
                                        docID: 'helpVPCDomain',
                                        label: 'label.DNS.domain.for.guest.networks'
                                        //format: FQDN
                                    },
                                    vpcoffering: {
                                        label: 'label.vpc.offering',
                                        validation: {
                                            required: true
                                        },
                                        select: function(args) {
                                            var data = {};
                                            $.ajax({
                                                url: createURL('listVPCOfferings'),
                                                data: {},
                                                success: function(json) {
                                                    var offerings  = json.listvpcofferingsresponse.vpcoffering ? json.listvpcofferingsresponse.vpcoffering : [];
                                                    var filteredofferings = $.grep(offerings, function(offering) {
                                                        return offering.state == 'Enabled';
                                                    });
                                                    args.response.success({
                                                        data: $.map(filteredofferings, function(vpco) {
                                                            return {
                                                                id: vpco.id,
                                                                description: vpco.name
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
                                var vpcOfferingName = args.data.vpcoffering;
                                var dataObj = {
                                    name: args.data.name,
                                    displaytext: args.data.displaytext,
                                    zoneid: args.data.zoneid,
                                    cidr: args.data.cidr,
                                    vpcofferingid: args.data.vpcoffering
                                };

                                if (args.data.networkdomain != null && args.data.networkdomain.length > 0)
                                    $.extend(dataObj, {
                                        networkdomain: args.data.networkdomain
                                    });

                                $.ajax({
                                    url: createURL("createVPC"),
                                    dataType: "json",
                                    data: dataObj,
                                    async: true,
                                    success: function(vpcjson) {
                                        var jid = vpcjson.createvpcresponse.jobid;
                                        args.response.success({
                                            _custom: {
                                                jobId: jid,
                                                getUpdatedItem: function(json) {
                                                    return json.queryasyncjobresultresponse.jobresult.vpc;
                                                }
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

                        },
                        configureVpc: {
                            label: 'label.configure.vpc',
                            textLabel: 'label.configure',
                            action: {
                                custom: cloudStack.uiCustom.vpc(cloudStack.vpc)
                            }
                        }
                    },

                    detailView: {
                        name: 'label.details',
                        actions: {
                            configureVpc: {
                                label: 'label.configure',
                                textLabel: 'label.configure',
                                action: {
                                    custom: cloudStack.uiCustom.vpc(cloudStack.vpc)
                                },
                                messages: {
                                    notification: function() {
                                        return '';
                                    }
                                }
                            },

                            edit: {
                                label: 'label.edit',
                                action: function(args) {
                                    $.ajax({
                                        url: createURL('updateVPC'),
                                        data: {
                                            id: args.context.vpc[0].id,
                                            name: args.data.name,
                                            displaytext: args.data.displaytext
                                        },
                                        success: function(json) {
                                            var jid = json.updatevpcresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid,
                                                    getUpdatedItem: function(json) {
                                                        return json.queryasyncjobresultresponse.jobresult.vpc;
                                                    }
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
                            },

                            restart: {
                                label: 'label.restart.vpc',
                                createForm: {
                                    title: 'label.restart.vpc',
                                    desc: function(args) {
                                        if (Boolean(args.context.vpc[0].redundantvpcrouter)) {
                                            return 'message.restart.vpc';
                                        } else {
                                            return 'message.restart.vpc.remark';
                                        }
                                    },

                                    preFilter: function(args) {
                                        var zoneObj;
                                        $.ajax({
                                            url: createURL("listZones&id=" + args.context.vpc[0].zoneid),
                                            dataType: "json",
                                            async: false,
                                            success: function(json) {
                                                zoneObj = json.listzonesresponse.zone[0];
                                            }
                                        });


                                        args.$form.find('.form-item[rel=cleanup]').find('input').attr('checked', 'checked'); //checked
                                        args.$form.find('.form-item[rel=cleanup]').css('display', 'inline-block'); //shown
                                        args.$form.find('.form-item[rel=makeredundant]').find('input').attr('checked', 'checked'); //checked
                                        args.$form.find('.form-item[rel=makeredundant]').css('display', 'inline-block'); //shown

                                        if (Boolean(args.context.vpc[0].redundantvpcrouter)) {
                                            args.$form.find('.form-item[rel=makeredundant]').hide();
                                        } else {
                                            args.$form.find('.form-item[rel=makeredundant]').show();
                                        }
                                    },
                                    fields: {
                                        cleanup: {
                                            label: 'label.clean.up',
                                            isBoolean: true
                                        },
                                        makeredundant: {
                                            label: 'label.make.redundant',
                                            isBoolean: true
                                        }
                                    }
                                },
                                messages: {
                                    confirm: function(args) {
                                        return 'message.restart.vpc';
                                    },
                                    notification: function(args) {
                                        return 'label.restart.vpc';
                                    }
                                },

                                action: function(args) {
                                    $.ajax({
                                        url: createURL("restartVPC"),
                                        data: {
                                            id: args.context.vpc[0].id,
                                            cleanup: (args.data.cleanup == "on"),
                                            makeredundant: (args.data.makeredundant == "on")
                                        },
                                        success: function(json) {
                                            var jid = json.restartvpcresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid,
                                                    getUpdatedItem: function(json) {
                                                        return json.queryasyncjobresultresponse.jobresult.vpc;
                                                    }
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
                            },

                            remove: {
                                label: 'label.remove.vpc',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.remove.vpc';
                                    },
                                    notification: function(args) {
                                        return 'label.remove.vpc';
                                    }
                                },
                                action: function(args) {
                                    $.ajax({
                                        url: createURL("deleteVPC"),
                                        data: {
                                            id: args.context.vpc[0].id
                                        },
                                        success: function(json) {
                                            var jid = json.deletevpcresponse.jobid;
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

                        tabFilter: function(args) {
                            var hiddenTabs = [];
                            var isRouterOwner = isAdmin();
                            if (!isRouterOwner) {
                                hiddenTabs.push("router");
                                hiddenTabs.push("virtualRouters");
                            }
                            return hiddenTabs;
                        },

                        tabs: {
                            details: {
                                title: 'label.details',
                                fields: [{
                                    name: {
                                        label: 'label.name',
                                        isEditable: true
                                    }
                                }, {
                                    displaytext: {
                                        label: 'label.description',
                                        isEditable: true
                                    },
                                    account: {
                                        label: 'label.account'
                                    },
                                    domain: {
                                        label: 'label.domain'
                                    },
                                    zonename: {
                                        label: 'label.zone'
                                    },
                                    cidr: {
                                        label: 'label.cidr'
                                    },
                                    networkdomain: {
                                        label: 'label.network.domain'
                                    },
                                    state: {
                                        label: 'label.state'
                                    },
                                    ispersistent: {
                                        label: 'label.persistent',
                                        converter: function(booleanValue) {
                                            if (booleanValue == true) {
                                                return "Yes";
                                            }

                                            return "No";
                                        }
                                    },
                                    redundantvpcrouter: {
                                        label: 'label.redundant.vpc',
                                        converter: function(booleanValue) {
                                            if (booleanValue == true) {
                                                return "Yes";
                                            }

                                            return "No";
                                        }
                                    },
                                    restartrequired: {
                                        label: 'label.restart.required',
                                        converter: function(booleanValue) {
                                            if (booleanValue == true) {
                                                return "Yes";
                                            }

                                            return "No";
                                        }
                                    },
                                    id: {
                                        label: 'label.id'
                                    }
                                }],

                                tags: cloudStack.api.tags({
                                    resourceType: 'Vpc',
                                    contextId: 'vpc'
                                }),

                                dataProvider: function(args) {
                                    $.ajax({
                                        url: createURL("listVPCs"),
                                        dataType: "json",
                                        data: {
                                            id: args.context.vpc[0].id
                                        },
                                        async: true,
                                        success: function(json) {
                                            var item = json.listvpcsresponse.vpc[0];
                                            args.response.success({
                                                data: item
                                            });
                                        }
                                    });
                                }
                            },
                            router: {
                                title: 'label.vpc.router.details',
                                fields: [{
                                    name: {
                                        label: 'label.name'
                                    }
                                }, {
                                    state: {
                                        label: 'label.state'
                                    },
                                    hostname: {
                                        label: 'label.host'
                                    },
                                    linklocalip: {
                                        label: 'label.linklocal.ip'
                                    },
                                    isredundantrouter: {
                                        label: 'label.redundant.router',
                                        converter: function(booleanValue) {
                                            if (booleanValue == true) {
                                                return "Yes";
                                            }
                                            return "No";
                                        }
                                    },
                                    redundantstate: {
                                        label: 'label.redundant.state'
                                    },
                                    id: {
                                        label: 'label.id'
                                    },
                                    serviceofferingname: {
                                        label: 'label.service.offering'
                                    },
                                    zonename: {
                                        label: 'label.zone'
                                    },
                                    gateway: {
                                        label: 'label.gateway'
                                    },
                                    publicip: {
                                        label: 'label.public.ip'
                                    },
                                    guestipaddress: {
                                        label: 'label.guest.ip'
                                    },
                                    dns1: {
                                        label: 'label.dns'
                                    },
                                    account: {
                                        label: 'label.account'
                                    },
                                    domain: {
                                        label: 'label.domain'
                                    }
                                }],

                                dataProvider: function(args) {
                                    $.ajax({
                                        url: createURL("listRouters&listAll=true&vpcid=" + args.context.vpc[0].id),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            for (var i = 0; i < json.listroutersresponse.router.length; i++) {
                                                var item = json.listroutersresponse.router[i];

                                                args.response.success({
                                                    actionFilter: cloudStack.sections.system.routerActionFilter,
                                                    data: item
                                                });
                                            }
                                        }
                                    });
                                }
                            },
                            virtualRouters: {
                                title: "label.virtual.routers",
                                listView: cloudStack.sections.system.subsections.virtualRouters.sections.routerNoGroup.listView
                            }
                        }
                    }
                }
            },

            vpnCustomerGateway: {
                type: 'select',
                title: 'label.vpn.customer.gateway',
                listView: {
                    id: 'vpnCustomerGateway',
                    label: 'label.vpn.customer.gateway',
                    fields: {
                        name: {
                            label: 'label.name'
                        },
                        gateway: {
                            label: 'label.gateway'
                        },
                        cidrlist: {
                            label: 'label.CIDR.list'
                        },
                        ipsecpsk: {
                            label: 'label.IPsec.preshared.key'
                        }
                    },

                    dataProvider: function(args) {
                        var data = {};
                        listViewDataProvider(args, data);

                        $.ajax({
                            url: createURL('listVpnCustomerGateways'),
                            data: data,
                            async: true,
                            success: function(json) {
                                var items = json.listvpncustomergatewaysresponse.vpncustomergateway;
                                args.response.success({
                                    data: items
                                });
                            }
                        });
                    },

                    actions: {
                        add: {
                            label: 'label.add.vpn.customer.gateway',
                            messages: {
                                notification: function(args) {
                                    return 'label.add.vpn.customer.gateway';
                                }
                            },
                            createForm: {
                                title: 'label.add.vpn.customer.gateway',
                                fields: {
                                    name: {
                                        label: 'label.name',
                                        docID: 'helpVPNGatewayName',
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
                                    cidrlist: {
                                        label: 'label.CIDR.list',
                                        desc: 'message.enter.seperated.list.multiple.cidrs',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    gateway: {
                                        label: 'label.gateway',
                                        docID: 'helpVPNGatewayGateway',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    cidrlist: {
                                        label: 'label.CIDR.list',
                                        desc: 'message.enter.seperated.list.multiple.cidrs',
                                        docID: 'helpVPNGatewayCIDRList',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    ipsecpsk: {
                                        label: 'label.IPsec.preshared.key',
                                        docID: 'helpVPNGatewayIPsecPresharedKey',
                                        validation: {
                                            required: true
                                        }
                                    },

                                    //IKE Policy
                                    ikeEncryption: {
                                        label: 'label.IKE.encryption',
                                        docID: 'helpVPNGatewayIKEEncryption',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: 'aes128',
                                                description: 'aes128'
                                            });
                                            items.push({
                                                id: 'aes192',
                                                description: 'aes192'
                                            });
                                            items.push({
                                                id: 'aes256',
                                                description: 'aes256'
                                            });
                                            items.push({
                                                id: '3des',
                                                description: '3des'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    ikeHash: {
                                        label: 'label.IKE.hash',
                                        docID: 'helpVPNGatewayIKEHash',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: 'sha1',
                                                description: 'sha1'
                                            });
                                            items.push({
                                                id: 'sha256',
                                                description: 'sha256'
                                            });
                                            items.push({
                                                id: 'sha384',
                                                description: 'sha384'
                                            });
                                            items.push({
                                                id: 'sha512',
                                                description: 'sha512'
                                            });
                                            items.push({
                                                id: 'md5',
                                                description: 'md5'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    ikeDh: {
                                        label: 'label.IKE.DH',
                                        docID: 'helpVPNGatewayIKEDH',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: 'modp1536',
                                                description: 'Group 5(modp1536)'
                                            });
                                            items.push({
                                                id: 'modp2048',
                                                description: 'Group 14(modp2048)'
                                            });
                                            items.push({
                                                id: 'modp3072',
                                                description: 'Group 15(modp3072)'
                                            });
                                            items.push({
                                                id: 'modp4096',
                                                description: 'Group 16(modp4096)'
                                            });
                                            items.push({
                                                id: 'modp6144',
                                                description: 'Group 17(modp6144)'
                                            });
                                            items.push({
                                                id: 'modp8192',
                                                description: 'Group 18(modp8192)'
                                            });
                                            items.push({
                                                id: 'modp1024',
                                                description: 'Group 2(modp1024)'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },

                                    //ESP Policy
                                    espEncryption: {
                                        label: 'label.ESP.encryption',
                                        docID: 'helpVPNGatewayESPLifetime',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: 'aes128',
                                                description: 'aes128'
                                            });
                                            items.push({
                                                id: 'aes192',
                                                description: 'aes192'
                                            });
                                            items.push({
                                                id: 'aes256',
                                                description: 'aes256'
                                            });
                                            items.push({
                                                id: '3des',
                                                description: '3des'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    espHash: {
                                        label: 'label.ESP.hash',
                                        docID: 'helpVPNGatewayESPHash',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: 'sha1',
                                                description: 'sha1'
                                            });
                                            items.push({
                                                id: 'sha256',
                                                description: 'sha256'
                                            });
                                            items.push({
                                                id: 'sha384',
                                                description: 'sha384'
                                            });
                                            items.push({
                                                id: 'sha512',
                                                description: 'sha512'
                                            });
                                            items.push({
                                                id: 'md5',
                                                description: 'md5'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    perfectForwardSecrecy: {
                                        label: 'label.perfect.forward.secrecy',
                                        docID: 'helpVPNGatewayPerfectForwardSecrecy',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: '',
                                                description: _l('label.none')
                                            });
                                            items.push({
                                                id: 'modp1536',
                                                description: 'Group 5(modp1536)'
                                            });
                                            items.push({
                                                id: 'modp2048',
                                                description: 'Group 14(modp2048)'
                                            });
                                            items.push({
                                                id: 'modp3072',
                                                description: 'Group 15(modp3072)'
                                            });
                                            items.push({
                                                id: 'modp4096',
                                                description: 'Group 16(modp4096)'
                                            });
                                            items.push({
                                                id: 'modp6144',
                                                description: 'Group 17(modp6144)'
                                            });
                                            items.push({
                                                id: 'modp8192',
                                                description: 'Group 18(modp8192)'
                                            });
                                            items.push({
                                                id: 'modp1024',
                                                description: 'Group 2(modp1024)'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },

                                    ikelifetime: {
                                        label: 'label.IKE.lifetime',
                                        docID: 'helpVPNGatewayIKELifetime',
                                        defaultValue: '86400',
                                        validation: {
                                            required: false,
                                            number: true
                                        }
                                    },
                                    esplifetime: {
                                        label: 'label.ESP.lifetime',
                                        docID: 'helpVPNGatewayESPLifetime',
                                        defaultValue: '3600',
                                        validation: {
                                            required: false,
                                            number: true
                                        }
                                    },

                                    dpd: {
                                        label: 'label.dead.peer.detection',
                                        docID: 'helpVPNGatewayDeadPeerDetection',
                                        isBoolean: true,
                                        isChecked: false
                                    },

                                    forceencap: {
                                        label: 'label.vpn.force.encapsulation',
                                        docID: 'helpVPNGatewayForceEncapsulation',
                                        docID: 'helpVPNGatewayForceEncapsulation',
                                        isBoolean: true,
                                        isChecked: false
                                    }
                                }
                            },
                            action: function(args) {
                                var data = {
                                    name: args.data.name,
                                    gateway: args.data.gateway,
                                    cidrlist: args.data.cidrlist,
                                    ipsecpsk: args.data.ipsecpsk,
                                    ikelifetime: args.data.ikelifetime,
                                    esplifetime: args.data.esplifetime,
                                    dpd: (args.data.dpd == "on"),
                                    forceencap: (args.data.forceencap == "on")
                                };

                                var ikepolicy = args.data.ikeEncryption + '-' + args.data.ikeHash;
                                if (args.data.ikeDh != null && args.data.ikeDh.length > 0)
                                    ikepolicy += ';' + args.data.ikeDh;

                                $.extend(data, {
                                    ikepolicy: ikepolicy
                                });

                                var esppolicy = args.data.espEncryption + '-' + args.data.espHash;
                                if (args.data.perfectForwardSecrecy != null && args.data.perfectForwardSecrecy.length > 0)
                                    esppolicy += ';' + args.data.perfectForwardSecrecy;

                                $.extend(data, {
                                    esppolicy: esppolicy
                                });

                                $.ajax({
                                    url: createURL('createVpnCustomerGateway'),
                                    data: data,
                                    dataType: 'json',
                                    success: function(json) {
                                        var jid = json.createvpncustomergatewayresponse.jobid;
                                        args.response.success({
                                            _custom: {
                                                jobId: jid,
                                                getUpdatedItem: function(json) {
                                                    return json.queryasyncjobresultresponse.jobresult.vpncustomergateway;
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
                        name: 'label.details',
                        actions: {
                            edit: {
                                label: 'label.edit',
                                action: function(args) {
                                    var data = {
                                        id: args.context.vpnCustomerGateway[0].id,
                                        name: args.data.name,
                                        gateway: args.data.gateway,
                                        cidrlist: args.data.cidrlist,
                                        ipsecpsk: args.data.ipsecpsk,
                                        ikelifetime: args.data.ikelifetime,
                                        esplifetime: args.data.esplifetime,
                                        dpd: (args.data.dpd == "on"),
                                        forceencap: (args.data.forceencap == "on")
                                    };

                                    var ikepolicy = args.data.ikeEncryption + '-' + args.data.ikeHash;
                                    if (args.data.ikeDh != null && args.data.ikeDh.length > 0)
                                        ikepolicy += ';' + args.data.ikeDh;

                                    $.extend(data, {
                                        ikepolicy: ikepolicy
                                    });

                                    var esppolicy = args.data.espEncryption + '-' + args.data.espHash;
                                    if (args.data.perfectForwardSecrecy != null && args.data.perfectForwardSecrecy.length > 0)
                                        esppolicy += ';' + args.data.perfectForwardSecrecy;

                                    $.extend(data, {
                                        esppolicy: esppolicy
                                    });

                                    $.ajax({
                                        url: createURL('updateVpnCustomerGateway'),
                                        data: data,
                                        success: function(json) {
                                            var jobId = json.updatevpncustomergatewayresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jobId,
                                                    getUpdatedItem: function(json) {
                                                        var item = json.queryasyncjobresultresponse.jobresult.vpncustomergateway;
                                                        args.response.success({
                                                            data: item
                                                        });
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

                            remove: {
                                label: 'label.delete.VPN.customer.gateway',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.delete.VPN.customer.gateway';
                                    },
                                    notification: function(args) {
                                        return 'label.delete.VPN.customer.gateway';
                                    }
                                },
                                action: function(args) {
                                    $.ajax({
                                        url: createURL("deleteVpnCustomerGateway"),
                                        data: {
                                            id: args.context.vpnCustomerGateway[0].id
                                        },
                                        success: function(json) {
                                            var jid = json.deletevpncustomergatewayresponse.jobid;
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
                                    gateway: {
                                        label: 'label.gateway',
                                        isEditable: true,
                                        validation: {
                                            required: true
                                        }
                                    },
                                    cidrlist: {
                                        label: 'label.CIDR.list',
                                        isEditable: true,
                                        validation: {
                                            required: true
                                        }
                                    },
                                    ipsecpsk: {
                                        label: 'label.IPsec.preshared.key',
                                        isEditable: true,
                                        validation: {
                                            required: true
                                        }
                                    },

                                    //IKE Policy
                                    ikeEncryption: {
                                        label: 'label.IKE.encryption',
                                        isEditable: true,
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: '3des',
                                                description: '3des'
                                            });
                                            items.push({
                                                id: 'aes128',
                                                description: 'aes128'
                                            });
                                            items.push({
                                                id: 'aes192',
                                                description: 'aes192'
                                            });
                                            items.push({
                                                id: 'aes256',
                                                description: 'aes256'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    ikeHash: {
                                        label: 'label.IKE.hash',
                                        isEditable: true,
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: 'md5',
                                                description: 'md5'
                                            });
                                            items.push({
                                                id: 'sha1',
                                                description: 'sha1'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    ikeDh: {
                                        label: 'label.IKE.DH',
                                        isEditable: true,
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: '',
                                                description: _l('label.none')
                                            });
                                            items.push({
                                                id: 'modp1024',
                                                description: 'Group 2(modp1024)'
                                            });
                                            items.push({
                                                id: 'modp1536',
                                                description: 'Group 5(modp1536)'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },

                                    //ESP Policy
                                    espEncryption: {
                                        label: 'label.ESP.encryption',
                                        isEditable: true,
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: '3des',
                                                description: '3des'
                                            });
                                            items.push({
                                                id: 'aes128',
                                                description: 'aes128'
                                            });
                                            items.push({
                                                id: 'aes192',
                                                description: 'aes192'
                                            });
                                            items.push({
                                                id: 'aes256',
                                                description: 'aes256'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    espHash: {
                                        label: 'label.ESP.hash',
                                        isEditable: true,
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: 'md5',
                                                description: 'md5'
                                            });
                                            items.push({
                                                id: 'sha1',
                                                description: 'sha1'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    perfectForwardSecrecy: {
                                        label: 'label.perfect.forward.secrecy',
                                        isEditable: true,
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: '',
                                                description: _l('label.none')
                                            });
                                            items.push({
                                                id: 'modp1024',
                                                description: 'Group 2(modp1024)'
                                            });
                                            items.push({
                                                id: 'modp1536',
                                                description: 'Group 5(modp1536)'
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },

                                    ikelifetime: {
                                        label: 'label.IKE.lifetime',
                                        isEditable: true,
                                        validation: {
                                            required: false,
                                            number: true
                                        }
                                    },
                                    esplifetime: {
                                        label: 'label.ESP.lifetime',
                                        isEditable: true,
                                        validation: {
                                            required: false,
                                            number: true
                                        }
                                    },

                                    dpd: {
                                        label: 'label.dead.peer.detection',
                                        isBoolean: true,
                                        isEditable: true,
                                        converter: cloudStack.converters.toBooleanText
                                    },

                                    forceencap: {
                                        label: 'label.vpn.force.encapsulation',
                                        isBoolean: true,
                                        isEditable: true,
                                        converter: cloudStack.converters.toBooleanText
                                    },

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
                                        url: createURL("listVpnCustomerGateways"),
                                        data: {
                                            id: args.context.vpnCustomerGateway[0].id
                                        },
                                        success: function(json) {
                                            var item = json.listvpncustomergatewaysresponse.vpncustomergateway[0];

                                            //IKE Policy
                                            var a1 = item.ikepolicy.split('-'); //e.g. item.ikepolicy == '3des-md5;modp1024'
                                            item.ikeEncryption = a1[0];
                                            if (a1[1].indexOf(';') == -1) {
                                                item.ikeHash = a1[1];
                                            } else {
                                                var a2 = a1[1].split(';');
                                                item.ikeHash = a2[0];
                                                item.ikeDh = a2[1];
                                            }

                                            //ESP Policy
                                            var a1 = item.esppolicy.split('-'); //e.g. item.esppolicy == '3des-md5' or '3des-md5;modp1024'
                                            item.espEncryption = a1[0];
                                            if (a1[1].indexOf(';') == -1) {
                                                item.espHash = a1[1];
                                            } else {
                                                var a2 = a1[1].split(';');
                                                item.espHash = a2[0];
                                                item.perfectForwardSecrecy = a2[1];
                                            }

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
            vpnuser: {
                type: 'select',
                title: 'label.vpn.users',
                listView: {
                    id: 'vpnUsers',
                    label: 'label.vpn.users',
                    fields: {
                        username: {
                            label: 'label.name'
                        },
                        domain: {
                            label: 'label.domain'
                        },
                        state: {
                            label: 'label.state'
                        }
                    },

                    dataProvider: function(args) {
                        var data = {};
                        listViewDataProvider(args, data);

                        $.ajax({
                            url: createURL('listVpnUsers'),
                            data: data,
                            dataType: 'json',
                            success: function(json) {
                                var items = json.listvpnusersresponse.vpnuser;
                                args.response.success({
                                    data: items
                                });
                            }
                        });
                    },

                    actions:{
                        destroy: {
                            label: 'label.action.delete.user',
                            messages: {
                                confirm: function(args) {
                                    return 'message.action.delete.vpn.user'
                                },
                                notification: function(args) {
                                    return 'label.delete.vpn.user'
                                }
                            },
                            action: function(args) {
                                $.ajax({
                                    url: createURL('removeVpnUser'),
                                    data: {
                                        domainid: args.context.vpnuser[0].domainid,
                                        account: args.context.vpnuser[0].account,
                                        username: args.context.vpnuser[0].username
                                    },
                                    dataType: 'json',
                                    async: true,
                                    success: function(json) {
                                        var jobID = json.removevpnuserresponse.jobid;
                                        args.response.success({
                                            _custom: {
                                                jobId: jobID
                                            }
                                        });
                                    }
                                });
                            },
                            notification: {
                                poll: pollAsyncJobResult
                            }
                        },
                        add: {
                            label: 'label.add.user',
                            messages: {
                                notification: function(args) {
                                    return 'label.add.vpn.user';
                                }
                            },
                            createForm:{
                                title: 'label.add.vpn.user',
                                fields: {
                                    username: {
                                        edit: true,
                                        label: 'label.username',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    password: {
                                        edit: true,
                                        isPassword: true,
                                        label: 'label.password',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    domain: {
                                        label: 'label.domain',
                                        isHidden: function(args) {
                                            if (isAdmin() || isDomainAdmin())
                                                return false;
                                            else
                                                return true;
                                        },
                                        select: function(args) {
                                            if (isAdmin() || isDomainAdmin()) {
                                                $.ajax({
                                                    url: createURL("listDomains&listAll=true"),
                                                    success: function(json) {
                                                        var items = [];
                                                        items.push({
                                                            id: "",
                                                            description: ""
                                                        });
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
                                                args.$select.change(function() {
                                                    var $form = $(this).closest('form');
                                                    if ($(this).val() == "") {
                                                        $form.find('.form-item[rel=account]').hide();
                                                    } else {
                                                        $form.find('.form-item[rel=account]').css('display', 'inline-block');
                                                    }
                                                });
                                            } else {
                                                args.response.success({
                                                    data: null
                                                });
                                            }
                                        }
                                    },
                                    account: {
                                        label: 'label.account',
                                        validation: {
                                            required: true
                                        },
                                        isHidden: function(args) {
                                            if (isAdmin() || isDomainAdmin()) {
                                                return false;
                                            } else {
                                                return true;
                                            }
                                        }
                                    }
                                }
                            },
                            action: function(args) {
                                var data = {
                                    username: args.data.username,
                                    password: args.data.password
                                };

                                if (args.data.domain != null && args.data.domain.length > 0) {
                                    $.extend(data, {
                                        domainid: args.data.domain
                                    });
                                    if (args.data.account != null && args.data.account.length > 0) {
                                        $.extend(data, {
                                            account: args.data.account
                                        });
                                    }
                                }

                                $.ajax({
                                    url: createURL('addVpnUser'),
                                    data: data,
                                    dataType: 'json',
                                    async: true,
                                    success: function(json) {
                                        var jid = json.addvpnuserresponse.jobid;
                                        args.response.success({
                                            _custom: {
                                                jobId: jid,
                                                getUpdatedItem: function(json) {
                                                    return json.queryasyncjobresultresponse.jobresult.vpnuser;
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
                        name: 'label.details',
                        actions: {
                            destroy: {
                                label: 'label.action.delete.user',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.action.delete.vpn.user';
                                    },
                                    notification: function(args) {
                                        return 'label.delete.vpn.user';
                                    }
                                },
                                action: function(args) {
                                    $.ajax({
                                        url: createURL("removeVpnUser"),
                                        data: {
                                            domainid: args.context.vpnuser[0].domainid,
                                            account: args.context.vpnuser[0].account,
                                            username: args.context.vpnuser[0].username
                                        },
                                        dataType: 'json',
                                        async: true,
                                        success: function(json) {
                                            var jid = json.removevpnuserresponse.jobid;
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
                                    username: {
                                        label: 'label.name',
                                        validation: {
                                            required: true
                                        }
                                    }
                                }, {
                                    domain: {
                                        label: 'label.domain'
                                    },
                                    state: {
                                        label: 'label.state'
                                    },
                                }],

                                dataProvider: function(args) {
                                    $.ajax({
                                        url: createURL("listVpnUsers"),
                                        data: {
                                            id: args.context.vpnuser[0].id
                                        },
                                        success: function(json) {
                                            var item = json.listvpnusersresponse.vpnuser[0];

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
            }
        }
    };

    function checkIfNetScalerProviderIsEnabled(services) {
        if (services != null) {
            for (var i = 0; i < services.length; i++) {
                if (services[i].name == 'Lb') {
                    var providers = services[i].provider;
                    if (providers != null) {
                        for (var k = 0; k < providers.length; k++) {
                            if (providers[k].name == 'Netscaler') {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            }
        }

        return false;
    }

    function getExtaPropertiesForIpObj(ipObj, args) {
        if (!('vpc' in args.context)) { //***** Guest Network section > Guest Network page > IP Address page *****
            var services = args.context.networks[0].service;
            if(services != null) {
                for(var i = 0; i < services.length; i++) {
                    var thisService = services[i];
                    if (thisService.name == "Vpn") {
                        ipObj.networkOfferingHavingVpnService = true;
                        break;
                    }
                }
            }
            if (ipObj.networkOfferingHavingVpnService == true) {
                $.ajax({
                    url: createURL('listRemoteAccessVpns'),
                    data: {
                        listAll: true,
                        publicipid: ipObj.id
                    },
                    async: false,
                    success: function(vpnResponse) {
                        var isVPNEnabled = vpnResponse.listremoteaccessvpnsresponse.count;
                        if (isVPNEnabled) {
                            ipObj.vpnenabled = true;
                            ipObj.remoteaccessvpn = vpnResponse.listremoteaccessvpnsresponse.remoteaccessvpn[0];
                        } else {
                            ipObj.vpnenabled = false;
                        }
                    }
                });
            }
        } else { //***** VPC section > Configuration VPC > Router > Public IP Addresses *****
            if (ipObj.issourcenat) { //VPC sourceNAT IP: supports VPN
                $.ajax({
                    url: createURL('listRemoteAccessVpns'),
                    data: {
                        listAll: true,
                        publicipid: ipObj.id
                    },
                    async: false,
                    success: function(vpnResponse) {
                        var isVPNEnabled = vpnResponse.listremoteaccessvpnsresponse.count;
                        if (isVPNEnabled) {
                            ipObj.vpnenabled = true;
                            ipObj.remoteaccessvpn = vpnResponse.listremoteaccessvpnsresponse.remoteaccessvpn[0];
                        } else {
                            ipObj.vpnenabled = false;
                        }
                    }
                });
            }
        }
    };

    var getLBAlgorithms = function(networkObj) {
        if (!networkObj || !networkObj.service) {
            return [];
        }

        var lbService = $.grep(networkObj.service, function(service) {
            return service.name == 'Lb';
        })[0];

        if (!lbService || !lbService.capability) {
            return [];
        }

        var algorithmCapabilities = $.grep(
            lbService.capability,
            function(capability) {
                return capability.name == 'SupportedLbAlgorithms';
            }
        )[0];

        if (!algorithmCapabilities) {
            return [];
        }

        var algorithms = algorithmCapabilities.value.split(',');

        if (!algorithms) {
            return [];
        }

        var data = [];
        $(algorithms).each(function() {
            data.push({id: this.valueOf(), name: this.valueOf(), description: _l('label.lb.algorithm.' + this.valueOf())});
        });

        return data;
    }

    function getForcedInfoAndUpdateNetwork(data, args) {
        if (isAdmin()) {
            cloudStack.dialog.confirm({
                message: "message.confirm.force.update",
                action: function() {
                    $.extend(data, {
                        forced: true
                    });

                    $.ajax({
                        url: createURL('updateNetwork'),
                        async: false,
                        data: data,
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
                        }
                    });
                },
                cancelAction: function() {
                    $.ajax({
                        url: createURL('updateNetwork'),
                        async: false,
                        data: data,
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
                        }
                    });
                }
            });
        }
        else {
            $.ajax({
                url: createURL('updateNetwork'),
                async: false,
                data: data,
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
                }
            });
        }
    }

    var getLBProtocols = function(networkObj) {
        if (!networkObj || !networkObj.service) {
            return [];
        }

        var lbService = $.grep(networkObj.service, function(service) {
            return service.name == 'Lb';
        })[0];

        if (!lbService || !lbService.capability) {
            return [];
        }

        var protocolCapabilities = $.grep(
            lbService.capability,
            function(capability) {
                return (capability.name == 'SupportedProtocols');
            }
        )[0];

        if (!protocolCapabilities) {
            return [];
        }

        var protocols = protocolCapabilities.value.split(',');

        if (!protocols) {
            return [];
        }

        var data = [];
        $(protocols).each(function() {
            data.push({id: this.valueOf(), name: this.valueOf(), description: _l('label.lb.protocol.' + this.valueOf())});
        });

        protocolCapabilities = $.grep(
            lbService.capability,
            function(capability) {
                return (capability.name == 'SslTermination' && (capability.value == 'true' || capability.value == true));
            }
        )[0];

        if (!protocolCapabilities) {
            return data;
        }

        var protocols = protocolCapabilities.value.split(',');

        if (!protocols) {
            return data;
        }

        $(protocols).each(function() {
                data.push({id: 'ssl', name: 'ssl', description: _l('label.lb.protocol.ssl')});
        });

        return data;
    }

})(cloudStack, jQuery);
