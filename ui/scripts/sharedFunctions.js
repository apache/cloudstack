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

var g_sessionKey = null;
var g_role = null; // roles - root, domain-admin, ro-admin, user
var g_username = null;
var g_userid = null;
var g_account = null;
var g_domainid = null;
var g_hostid = null;
var g_loginCmdText = null;
var g_enableLogging = false;
var g_timezoneoffset = null;
var g_timezone = null;
var g_supportELB = null;
var g_kvmsnapshotenabled =  null;
var g_regionsecondaryenabled = null;
var g_userPublicTemplateEnabled = "true";
var g_allowUserExpungeRecoverVm = "false";
var g_cloudstackversion = null;
var g_queryAsyncJobResultInterval = 3000;
var g_idpList = null;
var g_appendIdpDomain = false;
var g_sortKeyIsAscending = false;
var g_allowUserViewAllDomainAccounts = false;

//keyboard keycode
var keycode_Enter = 13;

//XMLHttpResponse.status
var ERROR_ACCESS_DENIED_DUE_TO_UNAUTHORIZED = 401;
var ERROR_INTERNET_NAME_NOT_RESOLVED = 12007;
var ERROR_INTERNET_CANNOT_CONNECT = 12029;
var ERROR_VMOPS_ACCOUNT_ERROR = 531;

//page size for API call (e.g."listXXXXXXX&pagesize=N" )
var pageSize = 20;
//var pageSize = 1; //for testing only

function to_json_array(str) {
    var simple_array = str.split(",");
    var json_array = [];

    $.each(simple_array, function(index, value) {
        if ($.trim(value).length > 0) {
            var obj = {
                          id: value,
                          name: value
                      };

            json_array.push(obj);
        }
    });

    return json_array;
}

function _tag_equals(tag1, tag2) {
    return (tag1.name == tag2.name) && (tag1.id == tag2.id);
}

function _tag_array_contains(tag, tags)
{
    for (var i = 0; i < tags.length; i++)
    {
        if (_tag_equals(tags[i], tag)) return true;
    }

    return false;
}

function unique_tags(tags)
{
    var unique = [];

    if (tags != null)
    {
        for (var i = 0; i < tags.length; i++)
        {
            if (!_tag_array_contains(tags[i], unique))
            {
                unique.push(tags[i]);
            }
        }
    }

    return unique;
}

//async action
var pollAsyncJobResult = function(args) {
    $.ajax({
        url: createURL("queryAsyncJobResult&jobId=" + args._custom.jobId),
        dataType: "json",
        async: false,
        success: function(json) {
            var result = json.queryasyncjobresultresponse;
            if (result.jobstatus == 0) {
                return; //Job has not completed
            } else {
                if (result.jobstatus == 1) { // Succeeded
                    if (args._custom.getUpdatedItem != null && args._custom.getActionFilter != null) {
                        args.complete({
                            data: args._custom.getUpdatedItem(json),
                            actionFilter: args._custom.getActionFilter()
                        });
                    } else if (args._custom.getUpdatedItem != null && args._custom.getActionFilter == null) {
                        args.complete({
                            data: args._custom.getUpdatedItem(json)
                        });
                    } else {
                        args.complete({
                            data: json.queryasyncjobresultresponse.jobresult
                        });
                    }

                    if (args._custom.fullRefreshAfterComplete == true) {
                        setTimeout(function() {
                            $(window).trigger('cloudStack.fullRefresh');
                        }, 500);
                    }

                    if (args._custom.onComplete) {
                        args._custom.onComplete(json, args._custom);
                    }
                } else if (result.jobstatus == 2) { // Failed
                    var msg = (result.jobresult.errortext == null) ? "" : result.jobresult.errortext;
                    if (args._custom.getUpdatedItemWhenAsyncJobFails != null && args._custom.getActionFilter != null) {
                        args.error({
                            message: msg,
                            updatedData: args._custom.getUpdatedItemWhenAsyncJobFails(),
                            actionFilter: args._custom.getActionFilter()
                        });
                    } else if (args._custom.getUpdatedItemWhenAsyncJobFails != null && args._custom.getActionFilter == null) {
                        args.error({
                            message: msg,
                            updatedData: args._custom.getUpdatedItemWhenAsyncJobFails()
                        });
                    } else {
                        args.error({
                            message: msg
                        });
                    }
                }
            }
        },
        error: function(XMLHttpResponse) {
            args.error({
                message: parseXMLHttpResponse(XMLHttpResponse)
            });
        }
    });
}

//API calls

    function createURL(apiName, options) {
        if (!options) options = {};
        var urlString = clientApiUrl + "?" + "command=" + apiName + "&response=json";
        if (g_sessionKey) {
            urlString += "&sessionkey=" + g_sessionKey;
        }

        if (cloudStack.context && cloudStack.context.projects && !options.ignoreProject) {
            urlString = urlString + '&projectid=' + cloudStack.context.projects[0].id;
        }

        return urlString;
    }


//LB provider map
var lbProviderMap = {
    "publicLb": {
        "non-vpc": ["VirtualRouter", "Netscaler", "F5"],
        "vpc": ["VpcVirtualRouter", "Netscaler"]
    },
    "internalLb": {
        "non-vpc": [],
        "vpc": ["InternalLbVm"]
    }
};

//Add Guest Network in Advanced zone (for root-admin only)
var addGuestNetworkDialog = {
    zoneObjs: [],
    physicalNetworkObjs: [],
    networkOfferingObjs: [],
    def: {
        label: 'label.add.guest.network',

        messages: {
            notification: function(args) {
                return 'label.add.guest.network';
            }
        },

        preFilter: function(args) {
            if (isAdmin())
                return true;
            else
                return false;
        },

        createForm: {
            title: 'label.add.guest.network', //Add Shared Network in advanced zone

            preFilter: function(args) {
                if ('zones' in args.context) { //Infrastructure menu > zone detail > guest traffic type > network tab (only shown in advanced zone) > add guest network dialog
                    args.$form.find('.form-item[rel=zoneId]').hide();
                    args.$form.find('.form-item[rel=physicalNetworkId]').hide();
                } else { //Network menu > guest network section > add guest network dialog
                    args.$form.find('.form-item[rel=zoneId]').css('display', 'inline-block');
                    args.$form.find('.form-item[rel=physicalNetworkId]').css('display', 'inline-block');
                }
            },

            fields: {
                name: {
                    docID: 'helpGuestNetworkZoneName',
                    label: 'label.name',
                    validation: {
                        required: true
                    }
                },
                description: {
                    label: 'label.description',
                    docID: 'helpGuestNetworkZoneDescription',
                    validation: {
                        required: true
                    }
                },

                zoneId: {
                    label: 'label.zone',
                    validation: {
                        required: true
                    },
                    docID: 'helpGuestNetworkZone',
                    select: function(args) {
                        if ('zones' in args.context) { //Infrastructure menu > zone detail > guest traffic type > network tab (only shown in advanced zone) > add guest network dialog
                            addGuestNetworkDialog.zoneObjs = args.context.zones; //i.e. only one zone entry
                        } else { //Network menu > guest network section > add guest network dialog
                            $.ajax({
                                url: createURL('listZones'),
                                async: false,
                                success: function(json) {
                                    addGuestNetworkDialog.zoneObjs = []; //reset
                                    var items = json.listzonesresponse.zone;
                                    if (items != null) {
                                        for (var i = 0; i < items.length; i++) {
                                            if (items[i].networktype == 'Advanced') {
                                                addGuestNetworkDialog.zoneObjs.push(items[i]);
                                            }
                                        }
                                    }
                                }
                            });
                        }
                        args.response.success({
                            data: $.map(addGuestNetworkDialog.zoneObjs, function(zone) {
                                return {
                                    id: zone.id,
                                    description: zone.name
                                };
                            })
                        });
                    },
                    isHidden: true
                },

                physicalNetworkId: {
                    label: 'label.physical.network',
                    dependsOn: 'zoneId',
                    select: function(args) {
                        if ('physicalNetworks' in args.context) { //Infrastructure menu > zone detail > guest traffic type > network tab (only shown in advanced zone) > add guest network dialog
                            addGuestNetworkDialog.physicalNetworkObjs = args.context.physicalNetworks;
                        } else { //Network menu > guest network section > add guest network dialog
                            var selectedZoneId = args.$form.find('.form-item[rel=zoneId]').find('select').val();
                            if (selectedZoneId != undefined && selectedZoneId.length > 0) {
                                $.ajax({
                                    url: createURL('listPhysicalNetworks'),
                                    data: {
                                        zoneid: selectedZoneId
                                    },
                                    async: false,
                                    success: function(json) {
                                        var items = [];
                                        var physicalnetworks = json.listphysicalnetworksresponse.physicalnetwork;
                                        if (physicalnetworks != null) {
                                            for (var i = 0; i < physicalnetworks.length; i++) {
                                                $.ajax({
                                                    url: createURL('listTrafficTypes'),
                                                    data: {
                                                        physicalnetworkid: physicalnetworks[i].id
                                                    },
                                                    async: false,
                                                    success: function(json) {
                                                        var traffictypes = json.listtraffictypesresponse.traffictype;
                                                        if (traffictypes != null) {
                                                            for (var k = 0; k < traffictypes.length; k++) {
                                                                if (traffictypes[k].traffictype == 'Guest') {
                                                                    items.push(physicalnetworks[i]);
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    }
                                                });
                                            }
                                        }

                                        addGuestNetworkDialog.physicalNetworkObjs = items;
                                    }
                                });
                            }
                        }
                        var items = [];
                        if (addGuestNetworkDialog.physicalNetworkObjs != null) {
                            for (var i = 0; i < addGuestNetworkDialog.physicalNetworkObjs.length; i++) {
                                items.push({
                                    id: addGuestNetworkDialog.physicalNetworkObjs[i].id,
                                    description: addGuestNetworkDialog.physicalNetworkObjs[i].name
                                });
                            }
                        }
                        args.response.success({
                            data: items
                        });
                    },
                    isHidden: true
                },

                vlanId: {
                    label: 'label.vlan.id',
                    docID: 'helpGuestNetworkZoneVLANID'
                },
                bypassVlanOverlapCheck: {
                    label: 'label.bypass.vlan.overlap.check',
                    isBoolean: true
                },
                isolatedpvlanId: {
                    label: 'label.secondary.isolated.vlan.id'
                },

                scope: {
                    label: 'label.scope',
                    docID: 'helpGuestNetworkZoneScope',
                    select: function(args) {
                        var selectedZoneId = args.$form.find('.form-item[rel=zoneId]').find('select').val();
                        var selectedZoneObj = {};
                        if (addGuestNetworkDialog.zoneObjs != null && selectedZoneId != "") {
                            for (var i = 0; i < addGuestNetworkDialog.zoneObjs.length; i++) {
                                if (addGuestNetworkDialog.zoneObjs[i].id == selectedZoneId) {
                                    selectedZoneObj = addGuestNetworkDialog.zoneObjs[i];
                                    break;
                                }
                            }
                        }

                        var array1 = [];
                        if (selectedZoneObj.networktype == "Advanced" && selectedZoneObj.securitygroupsenabled == true) {
                            array1.push({
                                id: 'zone-wide',
                                description: 'ui.listView.filters.all'
                            });
                        } else {
                            array1.push({
                                id: 'zone-wide',
                                description: 'ui.listView.filters.all'
                            });
                            array1.push({
                                id: 'domain-specific',
                                description: 'label.domain'
                            });
                            array1.push({
                                id: 'account-specific',
                                description: 'label.account'
                            });
                            array1.push({
                                id: 'project-specific',
                                description: 'label.project'
                            });
                        }
                        args.response.success({
                            data: array1
                        });

                        args.$select.change(function() {
                            var $form = $(this).closest('form');
                            if ($(this).val() == "zone-wide") {
                                $form.find('.form-item[rel=domainId]').hide();
                                $form.find('.form-item[rel=subdomainaccess]').hide();
                                $form.find('.form-item[rel=account]').hide();
                                $form.find('.form-item[rel=projectId]').hide();
                            } else if ($(this).val() == "domain-specific") {
                                $form.find('.form-item[rel=domainId]').css('display', 'inline-block');
                                $form.find('.form-item[rel=subdomainaccess]').css('display', 'inline-block');
                                $form.find('.form-item[rel=account]').hide();
                                $form.find('.form-item[rel=projectId]').hide();
                            } else if ($(this).val() == "account-specific") {
                                $form.find('.form-item[rel=domainId]').css('display', 'inline-block');
                                $form.find('.form-item[rel=subdomainaccess]').hide();
                                $form.find('.form-item[rel=account]').css('display', 'inline-block');
                                $form.find('.form-item[rel=projectId]').hide();
                            } else if ($(this).val() == "project-specific") {
                                $form.find('.form-item[rel=domainId]').css('display', 'inline-block');
                                $form.find('.form-item[rel=subdomainaccess]').hide();
                                $form.find('.form-item[rel=account]').hide();
                                $form.find('.form-item[rel=projectId]').css('display', 'inline-block');
                            }
                        });
                    }
                },
                domainId: {
                    label: 'label.domain',
                    validation: {
                        required: true
                    },
                    select: function(args) {
                        var items = [];
                        var selectedZoneId = args.$form.find('.form-item[rel=zoneId]').find('select').val();
                        var selectedZoneObj = {};
                        if (addGuestNetworkDialog.zoneObjs != null && selectedZoneId != "") {
                            for (var i = 0; i < addGuestNetworkDialog.zoneObjs.length; i++) {
                                if (addGuestNetworkDialog.zoneObjs[i].id == selectedZoneId) {
                                    selectedZoneObj = addGuestNetworkDialog.zoneObjs[i];
                                    break;
                                }
                            }
                        }
                        if (selectedZoneObj.domainid != null) { //list only domains under selectedZoneObj.domainid
                            $.ajax({
                                url: createURL("listDomainChildren&id=" + selectedZoneObj.domainid + "&isrecursive=true"),
                                dataType: "json",
                                async: false,
                                success: function(json) {
                                    var domainObjs = json.listdomainchildrenresponse.domain;
                                    $(domainObjs).each(function() {
                                        items.push({
                                            id: this.id,
                                            description: this.path
                                        });
                                    });
                                }
                            });
                            $.ajax({
                                url: createURL("listDomains&id=" + selectedZoneObj.domainid),
                                dataType: "json",
                                async: false,
                                success: function(json) {
                                    var domainObjs = json.listdomainsresponse.domain;
                                    $(domainObjs).each(function() {
                                        items.push({
                                            id: this.id,
                                            description: this.path
                                        });
                                    });
                                }
                            });
                        } else { //list all domains
                            $.ajax({
                                url: createURL("listDomains&listAll=true"),
                                dataType: "json",
                                async: false,
                                success: function(json) {
                                    var domainObjs = json.listdomainsresponse.domain;
                                    $(domainObjs).each(function() {
                                        items.push({
                                            id: this.id,
                                            description: this.path
                                        });
                                    });
                                }
                            });
                        }
                        items.sort(function(a, b) {
                            return a.description.localeCompare(b.description);
                        });
                        args.response.success({
                            data: items
                        });
                    }
                },
                subdomainaccess: {
                    label: 'label.subdomain.access',
                    isBoolean: true,
                    isHidden: true
                },
                account: {
                    label: 'label.account'
                },

                projectId: {
                    label: 'label.project',
                    validation: {
                        required: true
                    },
                    select: function(args) {
                        var items = [];
                        $.ajax({
                            url: createURL("listProjects&listAll=true&details=min"),
                            dataType: "json",
                            async: false,
                            success: function(json) {
                                projectObjs = json.listprojectsresponse.project;
                                $(projectObjs).each(function() {
                                    items.push({
                                        id: this.id,
                                        description: this.name
                                    });
                                });
                            }
                        });
                        args.response.success({
                            data: items
                        });
                    }
                },

                networkOfferingId: {
                    label: 'label.network.offering',
                    docID: 'helpGuestNetworkZoneNetworkOffering',
                    dependsOn: ['zoneId', 'physicalNetworkId', 'scope', 'domainId'],
                    select: function(args) {
                        if(args.$form.find('.form-item[rel=zoneId]').find('select').val() == null || args.$form.find('.form-item[rel=zoneId]').find('select').val().length == 0) {
                            args.response.success({
                                data: null
                            });
                            return;
                        }

                        var data = {
                            state: 'Enabled',
                            zoneid: args.$form.find('.form-item[rel=zoneId]').find('select').val()
                        };

                        var selectedPhysicalNetworkObj = [];
                        var selectedPhysicalNetworkId = args.$form.find('.form-item[rel=physicalNetworkId]').find('select').val();
                        if (addGuestNetworkDialog.physicalNetworkObjs != null) {
                            for (var i = 0; i < addGuestNetworkDialog.physicalNetworkObjs.length; i++) {
                                if (addGuestNetworkDialog.physicalNetworkObjs[i].id == selectedPhysicalNetworkId) {
                                    selectedPhysicalNetworkObj = addGuestNetworkDialog.physicalNetworkObjs[i];
                                    break;
                                }
                            }
                        }
                        if (selectedPhysicalNetworkObj.tags != null && selectedPhysicalNetworkObj.tags.length > 0) {
                            $.extend(data, {
                                tags: selectedPhysicalNetworkObj.tags
                            });
                        }

                        //Network tab in Guest Traffic Type in Infrastructure menu is only available when it's under Advanced zone.
                        //zone dropdown in add guest network dialog includes only Advanced zones.
                        if (args.scope == "zone-wide" || args.scope == "domain-specific") {
                            $.extend(data, {
                                guestiptype: 'Shared'
                            });
                            if (args.scope == "domain-specific") {
                                $.extend(data, {
                                    domainid: args.domainId
                                });
                            }
                        }

                        var items = [];
                        $.ajax({
                            url: createURL('listNetworkOfferings'),
                            data: data,
                            async: false,
                            success: function(json) {
                                addGuestNetworkDialog.networkOfferingObjs = json.listnetworkofferingsresponse.networkoffering;
                                if (addGuestNetworkDialog.networkOfferingObjs != null && addGuestNetworkDialog.networkOfferingObjs.length > 0) {
                                    var selectedZoneId = args.$form.find('.form-item[rel=zoneId]').find('select').val();
                                    var selectedZoneObj = {};
                                    if (addGuestNetworkDialog.zoneObjs != null && selectedZoneId != "") {
                                        for (var i = 0; i < addGuestNetworkDialog.zoneObjs.length; i++) {
                                            if (addGuestNetworkDialog.zoneObjs[i].id == selectedZoneId) {
                                                selectedZoneObj = addGuestNetworkDialog.zoneObjs[i];
                                                break;
                                            }
                                        }
                                    }
                                    for (var i = 0; i < addGuestNetworkDialog.networkOfferingObjs.length; i++) {
                                        //for zone-wide network in Advanced SG-enabled zone, list only SG network offerings
                                        if (selectedZoneObj.networktype == 'Advanced' && selectedZoneObj.securitygroupsenabled == true) {
                                            if (args.scope == "zone-wide") {
                                                var includingSecurityGroup = false;
                                                var serviceObjArray = addGuestNetworkDialog.networkOfferingObjs[i].service;
                                                for (var k = 0; k < serviceObjArray.length; k++) {
                                                    if (serviceObjArray[k].name == "SecurityGroup") {
                                                        includingSecurityGroup = true;
                                                        break;
                                                    }
                                                }
                                                if (includingSecurityGroup == false)
                                                    continue; //skip to next network offering
                                            }
                                        }
                                        items.push({
                                            id: addGuestNetworkDialog.networkOfferingObjs[i].id,
                                            description: addGuestNetworkDialog.networkOfferingObjs[i].displaytext
                                        });
                                    }
                                }
                            }
                        });
                        args.response.success({
                            data: items
                        });

                        args.$select.change(function() {
                            var $form = $(this).closest("form");
                            var selectedNetworkOfferingId = $(this).val();
                            $(addGuestNetworkDialog.networkOfferingObjs).each(function() {
                                if (this.id == selectedNetworkOfferingId) {
                                    if (this.specifyvlan == false) {
                                        $form.find('.form-item[rel=vlanId]').hide();
                                        cloudStack.dialog.createFormField.validation.required.remove($form.find('.form-item[rel=vlanId]')); //make vlanId optional

                                        $form.find('.form-item[rel=isolatedpvlanId]').hide();
                                    } else {
                                        $form.find('.form-item[rel=vlanId]').css('display', 'inline-block');
                                        cloudStack.dialog.createFormField.validation.required.add($form.find('.form-item[rel=vlanId]')); //make vlanId required

                                        $form.find('.form-item[rel=isolatedpvlanId]').css('display', 'inline-block');
                                    }
                                    return false; //break each loop
                                }
                            });
                        });
                    }
                },

                //IPv4 (begin)
                ip4gateway: {
                    label: 'label.ipv4.gateway',
                    docID: 'helpGuestNetworkZoneGateway',
                    validation: {
                        ipv4: true
                    }
                },
                ip4Netmask: {
                    label: 'label.ipv4.netmask',
                    docID: 'helpGuestNetworkZoneNetmask',
                    validation: {
                        netmask: true
                    }
                },
                startipv4: {
                    label: 'label.ipv4.start.ip',
                    docID: 'helpGuestNetworkZoneStartIP',
                    validation: {
                        ipv4: true
                    }
                },
                endipv4: {
                    label: 'label.ipv4.end.ip',
                    docID: 'helpGuestNetworkZoneEndIP',
                    validation: {
                        ipv4: true
                    }
                },
                //IPv4 (end)

                //IPv6 (begin)
                ip6gateway: {
                    label: 'label.ipv6.gateway',
                    docID: 'helpGuestNetworkZoneGateway',
                    validation: {
                    	ipv6CustomJqueryValidator: true
                    }
                },
                ip6cidr: {
                    label: 'label.ipv6.CIDR',
                    validation: {
                        ipv6cidr: true
                    }
                },
                startipv6: {
                    label: 'label.ipv6.start.ip',
                    docID: 'helpGuestNetworkZoneStartIP',
                    validation: {
                    	ipv6CustomJqueryValidator: true
                    }
                },
                endipv6: {
                    label: 'label.ipv6.end.ip',
                    docID: 'helpGuestNetworkZoneEndIP',
                    validation: {
                    	ipv6CustomJqueryValidator: true
                    }
               },
                //IPv6 (end)

                networkdomain: {
                    label: 'label.network.domain',
                    docID: 'helpGuestNetworkZoneNetworkDomain'
                },

                hideipaddressusage: {
                    label: 'label.network.hideipaddressusage',
                    dependsOn: ['zoneId', 'physicalNetworkId', 'scope'],
                    isBoolean: true,
                    isChecked: false,
                    docID: 'helpGuestNetworkHideIpAddressUsage'
                }

            }
        },

        action: function(args) { //Add guest network in advanced zone
            if (
                ((args.data.ip4gateway.length == 0) && (args.data.ip4Netmask.length == 0) && (args.data.startipv4.length == 0) && (args.data.endipv4.length == 0)) &&
                ((args.data.ip6gateway.length == 0) && (args.data.ip6cidr.length == 0) && (args.data.startipv6.length == 0) && (args.data.endipv6.length == 0))
            ) {
                args.response.error("Either IPv4 fields or IPv6 fields need to be filled when adding a guest network");
                return;
            }

            var $form = args.$form;

            var array1 = [];
            array1.push("&zoneId=" + args.data.zoneId);
            array1.push("&networkOfferingId=" + args.data.networkOfferingId);

            //Pass physical network ID to createNetwork API only when network offering's guestiptype is Shared.
            var selectedNetworkOfferingObj;
            if (addGuestNetworkDialog.networkOfferingObjs != null) {
                for (var i = 0; i < addGuestNetworkDialog.networkOfferingObjs.length; i++) {
                    if (addGuestNetworkDialog.networkOfferingObjs[i].id == args.data.networkOfferingId) {
                        selectedNetworkOfferingObj = addGuestNetworkDialog.networkOfferingObjs[i]
                        break;
                    }
                }
            }

            if (selectedNetworkOfferingObj.guestiptype == "Shared")
                array1.push("&physicalnetworkid=" + args.data.physicalNetworkId);
            
            cloudStack.addParameterToCommandUrlParameterArrayIfValueIsNotEmpty(array1, "name", args.data.name);
            cloudStack.addParameterToCommandUrlParameterArrayIfValueIsNotEmpty(array1, "displayText", args.data.description);

            if ($form.find('.form-item[rel=vlanId]').css("display") != "none"){
                cloudStack.addVlanToCommandUrlParameterArrayIfItIsNotNullAndNotEmpty(array1, args.data.vlanId)
            }
            if ($form.find('.form-item[rel=bypassVlanOverlapCheck]').css("display") != "none"){
                array1.push("&bypassVlanOverlapCheck=" + encodeURIComponent((args.data.bypassVlanOverlapCheck == "on")));
            }
            if (($form.find('.form-item[rel=isolatedpvlanId]').css("display") != "none") && (args.data.isolatedpvlanId != null && args.data.isolatedpvlanId.length > 0)){
                array1.push("&isolatedpvlan=" + encodeURIComponent(args.data.isolatedpvlanId));
            }
            if ($form.find('.form-item[rel=domainId]').css("display") != "none") {
                array1.push("&domainId=" + args.data.domainId);

                if ($form.find('.form-item[rel=account]').css("display") != "none") { //account-specific
                    array1.push("&account=" + args.data.account);
                    array1.push("&acltype=account");
                } else if ($form.find('.form-item[rel=projectId]').css("display") != "none") { //project-specific
                    array1.push("&projectid=" + args.data.projectId);
                    array1.push("&acltype=account");
                } else { //domain-specific
                    array1.push("&acltype=domain");

                    if ($form.find('.form-item[rel=subdomainaccess]:visible input:checked').length)
                        array1.push("&subdomainaccess=true");
                    else
                        array1.push("&subdomainaccess=false");
                }
            } else { //zone-wide
                array1.push("&acltype=domain"); //server-side will make it Root domain (i.e. domainid=1)
            }

            //IPv4 (begin)
            if (args.data.ip4gateway != null && args.data.ip4gateway.length > 0)
                array1.push("&gateway=" + args.data.ip4gateway);
            if (args.data.ip4Netmask != null && args.data.ip4Netmask.length > 0)
                array1.push("&netmask=" + args.data.ip4Netmask);
            if (($form.find('.form-item[rel=startipv4]').css("display") != "none") && (args.data.startipv4 != null && args.data.startipv4.length > 0))
                array1.push("&startip=" + args.data.startipv4);
            if (($form.find('.form-item[rel=endipv4]').css("display") != "none") && (args.data.endipv4 != null && args.data.endipv4.length > 0))
                array1.push("&endip=" + args.data.endipv4);
            //IPv4 (end)

            //IPv6 (begin)
            if (args.data.ip6gateway != null && args.data.ip6gateway.length > 0)
                array1.push("&ip6gateway=" + args.data.ip6gateway);
            if (args.data.ip6cidr != null && args.data.ip6cidr.length > 0)
                array1.push("&ip6cidr=" + args.data.ip6cidr);
            if (($form.find('.form-item[rel=startipv6]').css("display") != "none") && (args.data.startipv6 != null && args.data.startipv6.length > 0))
                array1.push("&startipv6=" + args.data.startipv6);
            if (($form.find('.form-item[rel=endipv6]').css("display") != "none") && (args.data.endipv6 != null && args.data.endipv6.length > 0))
                array1.push("&endipv6=" + args.data.endipv6);
            //IPv6 (end)

            if (args.data.networkdomain != null && args.data.networkdomain.length > 0){
                array1.push("&networkdomain=" + encodeURIComponent(args.data.networkdomain));
            }
            if (args.data.hideipaddressusage != null && args.data.hideipaddressusage) {
                array1.push("&hideipaddressusage=true")
            }

            $.ajax({
                url: createURL("createNetwork" + array1.join("")),
                dataType: "json",
                success: function(json) {
                    var item = json.createnetworkresponse.network;
                    args.response.success({
                        data: item
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
    }
}

var addL2GuestNetwork = {
    zoneObjs: [],
    physicalNetworkObjs: [],
    networkOfferingObjs: [],
    def: {
        label: 'label.add.l2.guest.network',

        messages: {
            notification: function(args) {
                return 'label.add.l2.guest.network';
            }
        },

        createForm: {
            title: 'label.add.l2.guest.network',
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
                                    return (zone.networktype == 'Advanced'); //Isolated networks can only be created in Advanced SG-disabled zone (but not in Basic zone nor Advanced SG-enabled zone)
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
                networkOfferingId: {
                    label: 'label.network.offering',
                    validation: {
                        required: true
                    },
                    dependsOn: (isAdmin() || isDomainAdmin()) ? ['zoneId', 'domain'] : 'zoneId', // domain is visible only for admins
                    docID: 'helpGuestNetworkNetworkOffering',
                    select: function(args) {
                        var data = {
                            zoneid: args.zoneId,
                            guestiptype: 'L2',
                            state: 'Enabled'
                        };

                        if ((isAdmin() || isDomainAdmin())) { // domain is visible only for admins
                            $.extend(data, {
                                domainid: args.domain
                            });
                        }

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
                                if(!json.listnetworkofferingsresponse || !json.listnetworkofferingsresponse.networkoffering){
                                    return;
                                }
                                var networkOfferingObjs = json.listnetworkofferingsresponse.networkoffering;
                                args.$select.change(function() {
                                    var $vlan = args.$select.closest('form').find('[rel=vlan]');
                                    var $bypassVlanOverlapCheck = args.$select.closest('form').find('[rel=bypassVlanOverlapCheck]');
                                    var networkOffering = $.grep(
                                        networkOfferingObjs, function(netoffer) {
                                            return netoffer.id == args.$select.val();
                                        }
                                    )[0];

                                    if (networkOffering.specifyvlan) {
                                        $vlan.css('display', 'inline-block');
                                        $bypassVlanOverlapCheck.css('display', 'inline-block');
                                    } else {
                                        $vlan.hide();
                                        $bypassVlanOverlapCheck.hide();
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
                bypassVlanOverlapCheck: {
                    label: 'label.bypass.vlan.overlap.check',
                    isBoolean: true,
                    isHidden: true
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

            if (args.$form.find('.form-item[rel=vlan]').css('display') != 'none') {
                $.extend(dataObj, {
                    vlan: args.data.vlan,
                    bypassVlanOverlapCheck: (args.data.bypassVlanOverlapCheck == "on")
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
        notification: {
            poll: function(args) {
                args.complete();
            }
        }
    }
}

    function isLdapEnabled() {
        var result;
        $.ajax({
            url: createURL("listLdapConfigurations"),
            dataType: "json",
            async: false,
            success: function(json) {
                result = (json.ldapconfigurationresponse.count > 0);
            },
            error: function(json) {
                result = false;
            }
        });
        return result;
    }

    // Role Functions

    function isAdmin() {
        return (g_role == 1);
    }

    function isDomainAdmin() {
        return (g_role == 2);
    }

    function isUser() {
        return (g_role == 0);
    }

    // FUNCTION: Handles AJAX error callbacks.  You can pass in an optional function to
    // handle errors that are not already handled by this method.

    function handleError(XMLHttpResponse, handleErrorCallback) {
        // User Not authenticated
        if (XMLHttpResponse.status == ERROR_ACCESS_DENIED_DUE_TO_UNAUTHORIZED) {
            $("#dialog_session_expired").dialog("open");
        } else if (XMLHttpResponse.status == ERROR_INTERNET_NAME_NOT_RESOLVED) {
            $("#dialog_error_internet_not_resolved").dialog("open");
        } else if (XMLHttpResponse.status == ERROR_INTERNET_CANNOT_CONNECT) {
            $("#dialog_error_management_server_not_accessible").dialog("open");
        } else if (XMLHttpResponse.status == ERROR_VMOPS_ACCOUNT_ERROR && handleErrorCallback != undefined) {
            handleErrorCallback();
        } else if (handleErrorCallback != undefined) {
            handleErrorCallback();
        } else {
            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
            $("#dialog_error").text(_s(errorMsg)).dialog("open");
        }
    }

    function parseXMLHttpResponse(XMLHttpResponse) {
        if (isValidJsonString(XMLHttpResponse.responseText) == false) {
            return "";
        }

        //var json = jQuery.parseJSON(XMLHttpResponse.responseText);
        var json = JSON.parse(XMLHttpResponse.responseText);
        if (json != null) {
            var property;
            for (property in json) {
                var errorObj = json[property];
                if (errorObj.errorcode == 401 && errorObj.errortext == "unable to verify user credentials and/or request signature") {
                    $('#container').hide();

                    return _l('label.session.expired');
                } else {
                    return _s(errorObj.errortext);
                }
            }
        } else {
            return "";
        }
    }

    function isValidJsonString(str) {
        try {
            JSON.parse(str);
        } catch (e) {
            return false;
        }
        return true;
    }

cloudStack.validate = {
    vmHostName: function(args) {
        // 1 ~ 63 characters long
        // ASCII letters 'a' through 'z', 'A' through 'Z', digits '0' through '9', hyphen ('-')
        // must start with a letter
        // must end with a letter or a digit (must not end with a hyphen)
        var regexp = /^[a-zA-Z]{1}[a-zA-Z0-9\-]{0,61}[a-zA-Z0-9]{0,1}$/;
        var b = regexp.test(args); //true or false
        if (b == false)
            cloudStack.dialog.notice({
                message: 'message.validate.instance.name'
            });
        return b;
    }
}

cloudStack.preFilter = {
    createTemplate: function(args) {
        if (isAdmin()) {
            args.$form.find('.form-item[rel=isPublic]').css('display', 'inline-block');
            args.$form.find('.form-item[rel=isFeatured]').css('display', 'inline-block');
            args.$form.find('.form-item[rel=isrouting]').css('display', 'inline-block');
        } else {
            if (g_userPublicTemplateEnabled == "true") {
                args.$form.find('.form-item[rel=isPublic]').css('display', 'inline-block');
            } else {
                args.$form.find('.form-item[rel=isPublic]').hide();
            }
            args.$form.find('.form-item[rel=isFeatured]').hide();
        }
    },
    addLoadBalancerDevice: function(args) { //add netscaler device OR add F5 device
        args.$form.find('.form-item[rel=dedicated]').bind('change', function() {
            var $dedicated = args.$form.find('.form-item[rel=dedicated]');
            var $capacity = args.$form.find('.form-item[rel=capacity]');
            if ($dedicated.find('input[type=checkbox]:checked').length > 0) {
                $capacity.hide();
                $capacity.find('input[type=text]').val('1');
            } else if ($dedicated.find('input[type=checkbox]:unchecked').length > 0) {
                $capacity.css('display', 'inline-block');
                $capacity.find('input[type=text]').val('');
            }
        });
        args.$form.change();
    }
}

cloudStack.actionFilter = {
    guestNetwork: function(args) {
        var jsonObj = args.context.item;
        var allowedActions = [];
        allowedActions.push('replaceacllist');
        if (jsonObj.type == 'Isolated') {
            allowedActions.push('edit'); //only Isolated network is allowed to upgrade to a different network offering (Shared network is not allowed to)
            allowedActions.push('restart');
            allowedActions.push('remove');
        } else if (jsonObj.type == 'Shared') {
            if (isAdmin()) {
                allowedActions.push('restart');
                allowedActions.push('remove');
            }
        }
        return allowedActions;
    }
}

var roleTypeUser = "0";
var roleTypeAdmin = "1";
var roleTypeDomainAdmin = "2";

cloudStack.converters = {
    convertBytes: function(bytes) {
        if (bytes == undefined)
            return '';

        if (bytes < 1024 * 1024) {
            return (bytes / 1024).toFixed(2) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return (bytes / 1024 / 1024).toFixed(2) + " MB";
        } else if (bytes < 1024 * 1024 * 1024 * 1024) {
            return (bytes / 1024 / 1024 / 1024).toFixed(2) + " GB";
        } else {
            return (bytes / 1024 / 1024 / 1024 / 1024).toFixed(2) + " TB";
        }
    },
    toBytes: function(str) {
        if (str === undefined) {
            return "0";
        }

        var res = str.split(" ");

        if (res.length === 1) {
            // assume a number in GB

            return parseInt(str, 10) * 1024 * 1024 * 1024;
        }

        // assume first string is a number and second string is a unit of size

        if (res[1] === "KB") {
            return parseInt(res[0], 10) * 1024;
        }

        if (res[1] === "MB") {
            return parseInt(res[0], 10) * 1024 * 1024;
        }

        if (res[1] === "GB") {
            return parseInt(res[0], 10) * 1024 * 1024 * 1024;
        }

        if (res[1] === "TB") {
            return parseInt(res[0], 10) * 1024 * 1024 * 1024 * 1024;
        }

        // assume GB
        return parseInt(res[0], 10) * 1024 * 1024 * 1024;
    },
    toLocalDate: function(UtcDate) {
        var localDate = "";
        if (UtcDate != null && UtcDate.length > 0) {
            var disconnected = new Date();
            disconnected.setISO8601(UtcDate);

            if (g_timezoneoffset != null && g_timezoneoffset != "null") {
                localDate = disconnected.getTimePlusTimezoneOffset(g_timezoneoffset);
            } else {
                var browserDate = new Date();
                var browserTimezoneoffset = browserDate.getTimezoneOffset();
                if (browserTimezoneoffset == undefined || isNaN(browserTimezoneoffset) ) {
                    localDate = disconnected.toUTCString();
                } else {
                    g_timezoneoffset = (browserTimezoneoffset/60) * (-1);
                    localDate = disconnected.getTimePlusTimezoneOffset(g_timezoneoffset);
                }
            }
        }
        return localDate;
    },
    toBooleanText: function(booleanValue) {
        var text1;
        if (booleanValue == true) {
            text1 = "Yes";
        } else if (booleanValue == false) {
            text1 = "No";
        } else { //booleanValue == undefined
            text1 = "";
        }
        return text1;
    },
    convertHz: function(hz) {
        if (hz == null)
            return "";

        if (hz < 1000) {
            return hz + " MHz";
        } else {
            return (hz / 1000).toFixed(2) + " GHz";
        }
    },
    toDayOfWeekDesp: function(dayOfWeek) {
        if (dayOfWeek == "1")
            return "Sunday";
        else if (dayOfWeek == "2")
            return "Monday";
        else if (dayOfWeek == "3")
            return "Tuesday";
        else if (dayOfWeek == "4")
            return "Wednesday";
        else if (dayOfWeek == "5")
            return "Thursday"
        else if (dayOfWeek == "6")
            return "Friday";
        else if (dayOfWeek == "7")
            return "Saturday";
    },
    toDayOfWeekDesp: function(dayOfWeek) {
        if (dayOfWeek == "1")
            return "Sunday";
        else if (dayOfWeek == "2")
            return "Monday";
        else if (dayOfWeek == "3")
            return "Tuesday";
        else if (dayOfWeek == "4")
            return "Wednesday";
        else if (dayOfWeek == "5")
            return "Thursday"
        else if (dayOfWeek == "6")
            return "Friday";
        else if (dayOfWeek == "7")
            return "Saturday";
    },
    toNetworkType: function(usevirtualnetwork) {
        if (usevirtualnetwork == true || usevirtualnetwork == "true")
            return "Public";
        else
            return "Direct";
    },
    toRole: function(type) {
        if (type == roleTypeUser) {
            return "User";
        } else if (type == roleTypeAdmin) {
            return "Admin";
        } else if (type == roleTypeDomainAdmin) {
            return "Domain-Admin";
        }
    },
    toAccountType: function(roleType) {
        if (roleType == 'User') {
            return 0;
        } else if (roleType == 'Admin') {
            return 1;
        } else if (roleType == 'DomainAdmin') {
            return 2;
        } else if (roleType == 'ResourceAdmin') {
            return 3;
        }
    },
    toAlertType: function(alertCode) {
        switch (alertCode) {
            case 0:
                return _l('label.memory');
            case 1:
                return _l('label.cpu');
            case 2:
                return _l('label.storage');
            case 3:
                return _l('label.primary.storage');
            case 4:
                return _l('label.public.ips');
            case 5:
                return _l('label.management.ips');
            case 6:
                return _l('label.secondary.storage');
            case 7:
                return _l('label.host');
            case 9:
                return _l('label.domain.router');
            case 10:
                return _l('label.console.proxy');

                // These are old values -- can be removed in the future
            case 8:
                return _l('label.user.vm');
            case 11:
                return _l('label.routing.host');
            case 12:
                return _l('label.menu.storage');
            case 13:
                return _l('label.usage.server');
            case 14:
                return _l('label.management.server');
            case 15:
                return _l('label.domain.router');
            case 16:
                return _l('label.console.proxy');
            case 17:
                return _l('label.user.vm');
            case 18:
                return _l('label.vlan');
            case 19:
                return _l('label.secondary.storage.vm');
            case 20:
                return _l('label.usage.server');
            case 21:
                return _l('label.menu.storage');
            case 22:
                return _l('label.action.update.resource.count');
            case 23:
                return _l('label.usage.sanity.result');
            case 24:
                return _l('label.direct.attached.public.ip');
            case 25:
                return _l('label.local.storage');
            case 26:
                return _l('label.resource.limit.exceeded');
        }
    },

    toCapacityCountType: function(capacityCode) {
        switch (capacityCode) {
            case 0:
                return _l('label.memory');
            case 1:
                return _l('label.cpu');
            case 2:
                return _l('label.storage');
            case 3:
                return _l('label.primary.storage');
            case 4:
                return _l('label.public.ips');
            case 5:
                return _l('label.management.ips');
            case 6:
                return _l('label.secondary.storage');
            case 7:
                return _l('label.vlan');
            case 8:
                return _l('label.direct.ips');
            case 9:
                return _l('label.local.storage');
            case 10:
                return _l('label.routing.host');
            case 11:
                return _l('label.menu.storage');
            case 12:
                return _l('label.usage.server');
            case 13:
                return _l('label.management.server');
            case 14:
                return _l('label.domain.router');
            case 15:
                return _l('label.console.proxy');
            case 16:
                return _l('label.user.vm');
            case 17:
                return _l('label.vlan');
            case 18:
                return _l('label.secondary.storage.vm');
            case 19:
                return _l('label.gpu');
            case 90:
                return _l('label.num.cpu.cores');
        }
    },

    convertByType: function(alertCode, value) {
        switch (alertCode) {
            case 0:
                return cloudStack.converters.convertBytes(value);
            case 1:
                return cloudStack.converters.convertHz(value);
            case 2:
                return cloudStack.converters.convertBytes(value);
            case 3:
                return cloudStack.converters.convertBytes(value);
            case 6:
                return cloudStack.converters.convertBytes(value);
            case 9:
                return cloudStack.converters.convertBytes(value);
            case 11:
                return cloudStack.converters.convertBytes(value);
        }

        return value;
    }
}

function isModuleIncluded(moduleName) {
    for(var moduleIndex = 0; moduleIndex < cloudStack.modules.length; moduleIndex++) {
        if (cloudStack.modules[moduleIndex] == moduleName) {
            return true;
            break;
        }
    }
    return false;
}

//data parameter passed to API call in listView

function listViewDataProvider(args, data, options) {
    //search
    if (args.filterBy != null) {
        if (args.filterBy.advSearch != null && typeof(args.filterBy.advSearch) == "object") { //advanced search
            for (var key in args.filterBy.advSearch) {
                if (key == 'tagKey' && args.filterBy.advSearch[key].length > 0) {
                    $.extend(data, {
                        'tags[0].key': args.filterBy.advSearch[key]
                    });
                } else if (key == 'tagValue' && args.filterBy.advSearch[key].length > 0) {
                    $.extend(data, {
                        'tags[0].value': args.filterBy.advSearch[key]
                    });
                } else if (args.filterBy.advSearch[key] != null && args.filterBy.advSearch[key].length > 0) {
                    data[key] = args.filterBy.advSearch[key]; //do NOT use  $.extend(data, { key: args.filterBy.advSearch[key] }); which will treat key variable as "key" string
                }
            }
        } else if (args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) { //basic search
            switch (args.filterBy.search.by) {
                case "name":
                    if (args.filterBy.search.value.length > 0) {
                        if (options && options.searchBy) {
                            data[options.searchBy] = args.filterBy.search.value;
                        } else {
                            $.extend(data, {
                                keyword: args.filterBy.search.value
                            });
                        }
                    }
                    break;
            }
        }
    }

    //pagination
    $.extend(data, {
        listAll: true,
        page: args.page,
        pagesize: pageSize
    });

    return data;
}

//used by infrastructure page and network page
var addExtraPropertiesToGuestNetworkObject = function(jsonObj) {
    jsonObj.networkdomaintext = jsonObj.networkdomain;
    jsonObj.networkofferingidText = jsonObj.networkofferingid;

    if (jsonObj.acltype == "Domain") {
        jsonObj.scope = "Domain (" + jsonObj.domain + ")";
    } else if (jsonObj.acltype == "Account") {
        if (jsonObj.project != null)
            jsonObj.scope = "Account (" + jsonObj.domain + ", " + jsonObj.project + ")";
        else
            jsonObj.scope = "Account (" + jsonObj.domain + ", " + jsonObj.account + ")";
    }

    if (jsonObj.vlan == null && jsonObj.broadcasturi != null && jsonObj.broadcasturi.substring(0,7) == "vlan://") {
        jsonObj.vlan = jsonObj.broadcasturi.replace("vlan://", "");
    }
    if(jsonObj.vxlan == null && jsonObj.broadcasturi != null && jsonObj.broadcasturi.substring(0,8) == "vxlan://") {
        jsonObj.vxlan = jsonObj.broadcasturi.replace("vxlan://", "");
    }
}

//used by infrastructure page
var addExtraPropertiesToUcsBladeObject = function(jsonObj) {
    var array1 = jsonObj.bladedn.split('/');
    jsonObj.chassis = array1[1];
    jsonObj.bladeid = array1[2];
}

var processPropertiesInImagestoreObject = function(jsonObj) {
    if (jsonObj.url != undefined) {
        var url = jsonObj.url; //e.g. 'cifs://10.1.1.1/aaa/aaa2/aaa3?user=bbb&password=ccc&domain=ddd'
        var passwordIndex = url.indexOf('&password='); //38
        var domainIndex = url.indexOf('&domain=');    //51
        if (passwordIndex >= 0) {
            jsonObj.url = url.substring(0, passwordIndex) + url.substring(domainIndex); //remove '&password=ccc' from jsonObj.url
        }
    }
}

//find service object in network object

    function ipFindNetworkServiceByName(pName, networkObj) {
        if (networkObj == null)
            return null;
        if (networkObj.service != null) {
            for (var i = 0; i < networkObj.service.length; i++) {
                var networkServiceObj = networkObj.service[i];
                if (networkServiceObj.name == pName)
                    return networkServiceObj;
            }
        }
        return null;
    }
    //find capability object in service object in network object

    function ipFindCapabilityByName(pName, networkServiceObj) {
        if (networkServiceObj == null)
            return null;
        if (networkServiceObj.capability != null) {
            for (var i = 0; i < networkServiceObj.capability.length; i++) {
                var capabilityObj = networkServiceObj.capability[i];
                if (capabilityObj.name == pName)
                    return capabilityObj;
            }
        }
        return null;
    }

    //compose URL for adding primary storage

    function nfsURL(server, path) {
        var url;

        if (path.substring(0, 1) != "/") {
            path = "/" + path;
        }

        if (server.indexOf("://") == -1)
            url = "nfs://" + server + path;
        else
            url = server + path;
        return url;
    }

    function smbURL(server, path, smbUsername, smbPassword, smbDomain) {
        var url = '';

        if (path.substring(0, 1) != "/") {
            path = "/" + path;
        }

        if (server.indexOf('://') == -1) {
            url += 'cifs://';
        }

        url += (server + path);

        return url;
    }

    function presetupURL(server, path) {
        var url;
        if (server.indexOf("://") == -1)
            url = "presetup://" + server + path;
        else
            url = server + path;
        return url;
    }

    function ocfs2URL(server, path) {
        var url;
        if (server.indexOf("://") == -1)
            url = "ocfs2://" + server + path;
        else
            url = server + path;
        return url;
    }

    function SharedMountPointURL(server, path) {
        var url;
        if (server.indexOf("://") == -1)
            url = "SharedMountPoint://" + server + path;
        else
            url = server + path;
        return url;
    }

    function rbdURL(monitor, pool, id, secret) {
        var url;

        /*
    Replace the + and / symbols by - and _ to have URL-safe base64 going to the API
    It's hacky, but otherwise we'll confuse java.net.URI which splits the incoming URI
    */
        secret = secret.replace("+", "-");
        secret = secret.replace("/", "_");

        if (id != null && secret != null) {
            monitor = id + ":" + secret + "@" + monitor;
        }

        if (pool.substring(0, 1) != "/")
            pool = "/" + pool;

        if (monitor.indexOf("://") == -1)
            url = "rbd://" + monitor + pool;
        else
            url = monitor + pool;

        return url;
    }

    function clvmURL(vgname) {
        var url;
        if (vgname.indexOf("://") == -1)
            url = "clvm://localhost/" + vgname;
        else
            url = vgname;
        return url;
    }

    function vmfsURL(server, path) {
        var url;
        if (server.indexOf("://") == -1)
            url = "vmfs://" + server + path;
        else
            url = server + path;
        return url;
    }

    function iscsiURL(server, iqn, lun) {
        var url;
        if (server.indexOf("://") == -1)
            url = "iscsi://" + server + iqn + "/" + lun;
        else
            url = server + iqn + "/" + lun;
        return url;
    }

    function glusterURL(server, path) {
        var url;
        if (server.indexOf("://") == -1)
            url = "gluster://" + server + path;
        else
            url = server + path;
        return url;
    }


    //VM Instance

    function getVmName(p_vmName, p_vmDisplayname) {
        if (p_vmDisplayname == null)
            return _s(p_vmName);

        var vmName = null;
        if (p_vmDisplayname != p_vmName) {
            vmName = _s(p_vmName) + " (" + _s(p_vmDisplayname) + ")";
        } else {
            vmName = _s(p_vmName);
        }
        return vmName;
    }

var timezoneMap = new Object();
timezoneMap["Etc/GMT+12"] = "Etc/GMT+12 [GMT-12:00]";
timezoneMap["Etc/GMT+11"] = "Etc/GMT+11 [GMT-11:00]";
timezoneMap["Pacific/Midway"] = "Pacific/Midway [Samoa Standard Time]";
timezoneMap["Pacific/Niue"] = "Pacific/Niue [Niue Time]";
timezoneMap["Pacific/Pago_Pago"] = "Pacific/Pago_Pago [Samoa Standard Time]";
timezoneMap["Pacific/Samoa"] = "Pacific/Samoa [Samoa Standard Time]";
timezoneMap["US/Samoa"] = "US/Samoa [Samoa Standard Time]";
timezoneMap["America/Adak"] = "America/Adak [Hawaii-Aleutian Standard Time]";
timezoneMap["America/Atka"] = "America/Atka [Hawaii-Aleutian Standard Time]";
timezoneMap["Etc/GMT+10"] = "Etc/GMT+10 [GMT-10:00]";
timezoneMap["HST"] = "HST [Hawaii Standard Time]";
timezoneMap["Pacific/Honolulu"] = "Pacific/Honolulu [Hawaii Standard Time]";
timezoneMap["Pacific/Johnston"] = "Pacific/Johnston [Hawaii Standard Time]";
timezoneMap["Pacific/Rarotonga"] = "Pacific/Rarotonga [Cook Is. Time]";
timezoneMap["Pacific/Tahiti"] = "Pacific/Tahiti [Tahiti Time]";
timezoneMap["SystemV/HST10"] = "SystemV/HST10 [Hawaii Standard Time]";
timezoneMap["US/Aleutian"] = "US/Aleutian [Hawaii-Aleutian Standard Time]";
timezoneMap["US/Hawaii"] = "US/Hawaii [Hawaii Standard Time]";
timezoneMap["Pacific/Marquesas"] = "Pacific/Marquesas [Marquesas Time]";
timezoneMap["AST"] = "AST [Alaska Standard Time]";
timezoneMap["America/Anchorage"] = "America/Anchorage [Alaska Standard Time]";
timezoneMap["America/Juneau"] = "America/Juneau [Alaska Standard Time]";
timezoneMap["America/Nome"] = "America/Nome [Alaska Standard Time]";
timezoneMap["America/Sitka"] = "America/Sitka [GMT-09:00]";
timezoneMap["America/Yakutat"] = "America/Yakutat [Alaska Standard Time]";
timezoneMap["Etc/GMT+9"] = "Etc/GMT+9 [GMT-09:00]";
timezoneMap["Pacific/Gambier"] = "Pacific/Gambier [Gambier Time]";
timezoneMap["SystemV/YST9"] = "SystemV/YST9 [Alaska Standard Time]";
timezoneMap["SystemV/YST9YDT"] = "SystemV/YST9YDT [Alaska Standard Time]";
timezoneMap["US/Alaska"] = "US/Alaska [Alaska Standard Time]";
timezoneMap["America/Dawson"] = "America/Dawson [Pacific Standard Time]";
timezoneMap["America/Ensenada"] = "America/Ensenada [Pacific Standard Time]";
timezoneMap["America/Los_Angeles"] = "America/Los_Angeles [Pacific Standard Time]";
timezoneMap["America/Metlakatla"] = "America/Metlakatla [GMT-08:00]";
timezoneMap["America/Santa_Isabel"] = "America/Santa_Isabel [Pacific Standard Time]";
timezoneMap["America/Tijuana"] = "America/Tijuana [Pacific Standard Time]";
timezoneMap["America/Vancouver"] = "America/Vancouver [Pacific Standard Time]";
timezoneMap["America/Whitehorse"] = "America/Whitehorse [Pacific Standard Time]";
timezoneMap["Canada/Pacific"] = "Canada/Pacific [Pacific Standard Time]";
timezoneMap["Canada/Yukon"] = "Canada/Yukon [Pacific Standard Time]";
timezoneMap["Etc/GMT+8"] = "Etc/GMT+8 [GMT-08:00]";
timezoneMap["Mexico/BajaNorte"] = "Mexico/BajaNorte [Pacific Standard Time]";
timezoneMap["PST"] = "PST [Pacific Standard Time]";
timezoneMap["PST8PDT"] = "PST8PDT [Pacific Standard Time]";
timezoneMap["Pacific/Pitcairn"] = "Pacific/Pitcairn [Pitcairn Standard Time]";
timezoneMap["SystemV/PST8"] = "SystemV/PST8 [Pacific Standard Time]";
timezoneMap["SystemV/PST8PDT"] = "SystemV/PST8PDT [Pacific Standard Time]";
timezoneMap["US/Pacific"] = "US/Pacific [Pacific Standard Time]";
timezoneMap["US/Pacific-New"] = "US/Pacific-New [Pacific Standard Time]";
timezoneMap["America/Boise"] = "America/Boise [Mountain Standard Time]";
timezoneMap["America/Cambridge_Bay"] = "America/Cambridge_Bay [Mountain Standard Time]";
timezoneMap["America/Chihuahua"] = "America/Chihuahua [Mountain Standard Time]";
timezoneMap["America/Creston"] = "America/Creston [GMT-07:00]";
timezoneMap["America/Dawson_Creek"] = "America/Dawson_Creek [Mountain Standard Time]";
timezoneMap["America/Denver"] = "America/Denver [Mountain Standard Time]";
timezoneMap["America/Edmonton"] = "America/Edmonton [Mountain Standard Time]";
timezoneMap["America/Hermosillo"] = "America/Hermosillo [Mountain Standard Time]";
timezoneMap["America/Inuvik"] = "America/Inuvik [Mountain Standard Time]";
timezoneMap["America/Mazatlan"] = "America/Mazatlan [Mountain Standard Time]";
timezoneMap["America/Ojinaga"] = "America/Ojinaga [Mountain Standard Time]";
timezoneMap["America/Phoenix"] = "America/Phoenix [Mountain Standard Time]";
timezoneMap["America/Shiprock"] = "America/Shiprock [Mountain Standard Time]";
timezoneMap["America/Yellowknife"] = "America/Yellowknife [Mountain Standard Time]";
timezoneMap["Canada/Mountain"] = "Canada/Mountain [Mountain Standard Time]";
timezoneMap["Etc/GMT+7"] = "Etc/GMT+7 [GMT-07:00]";
timezoneMap["MST"] = "MST [Mountain Standard Time]";
timezoneMap["MST7MDT"] = "MST7MDT [Mountain Standard Time]";
timezoneMap["Mexico/BajaSur"] = "Mexico/BajaSur [Mountain Standard Time]";
timezoneMap["Navajo"] = "Navajo [Mountain Standard Time]";
timezoneMap["PNT"] = "PNT [Mountain Standard Time]";
timezoneMap["SystemV/MST7"] = "SystemV/MST7 [Mountain Standard Time]";
timezoneMap["SystemV/MST7MDT"] = "SystemV/MST7MDT [Mountain Standard Time]";
timezoneMap["US/Arizona"] = "US/Arizona [Mountain Standard Time]";
timezoneMap["US/Mountain"] = "US/Mountain [Mountain Standard Time]";
timezoneMap["America/Bahia_Banderas"] = "America/Bahia_Banderas [GMT-06:00]";
timezoneMap["America/Belize"] = "America/Belize [Central Standard Time]";
timezoneMap["America/Cancun"] = "America/Cancun [Central Standard Time]";
timezoneMap["America/Chicago"] = "America/Chicago [Central Standard Time]";
timezoneMap["America/Costa_Rica"] = "America/Costa_Rica [Central Standard Time]";
timezoneMap["America/El_Salvador"] = "America/El_Salvador [Central Standard Time]";
timezoneMap["America/Guatemala"] = "America/Guatemala [Central Standard Time]";
timezoneMap["America/Indiana/Knox"] = "America/Indiana/Knox [Central Standard Time]";
timezoneMap["America/Indiana/Tell_City"] = "America/Indiana/Tell_City [Central Standard Time]";
timezoneMap["America/Knox_IN"] = "America/Knox_IN [Central Standard Time]";
timezoneMap["America/Managua"] = "America/Managua [Central Standard Time]";
timezoneMap["America/Matamoros"] = "America/Matamoros [Central Standard Time]";
timezoneMap["America/Menominee"] = "America/Menominee [Central Standard Time]";
timezoneMap["America/Merida"] = "America/Merida [Central Standard Time]";
timezoneMap["America/Mexico_City"] = "America/Mexico_City [Central Standard Time]";
timezoneMap["America/Monterrey"] = "America/Monterrey [Central Standard Time]";
timezoneMap["America/North_Dakota/Beulah"] = "America/North_Dakota/Beulah [GMT-06:00]";
timezoneMap["America/North_Dakota/Center"] = "America/North_Dakota/Center [Central Standard Time]";
timezoneMap["America/North_Dakota/New_Salem"] = "America/North_Dakota/New_Salem [Central Standard Time]";
timezoneMap["America/Rainy_River"] = "America/Rainy_River [Central Standard Time]";
timezoneMap["America/Rankin_Inlet"] = "America/Rankin_Inlet [Central Standard Time]";
timezoneMap["America/Regina"] = "America/Regina [Central Standard Time]";
timezoneMap["America/Resolute"] = "America/Resolute [Eastern Standard Time]";
timezoneMap["America/Swift_Current"] = "America/Swift_Current [Central Standard Time]";
timezoneMap["America/Tegucigalpa"] = "America/Tegucigalpa [Central Standard Time]";
timezoneMap["America/Winnipeg"] = "America/Winnipeg [Central Standard Time]";
timezoneMap["CST"] = "CST [Central Standard Time]";
timezoneMap["CST6CDT"] = "CST6CDT [Central Standard Time]";
timezoneMap["Canada/Central"] = "Canada/Central [Central Standard Time]";
timezoneMap["Canada/East-Saskatchewan"] = "Canada/East-Saskatchewan [Central Standard Time]";
timezoneMap["Canada/Saskatchewan"] = "Canada/Saskatchewan [Central Standard Time]";
timezoneMap["Chile/EasterIsland"] = "Chile/EasterIsland [Easter Is. Time]";
timezoneMap["Etc/GMT+6"] = "Etc/GMT+6 [GMT-06:00]";
timezoneMap["Mexico/General"] = "Mexico/General [Central Standard Time]";
timezoneMap["Pacific/Easter"] = "Pacific/Easter [Easter Is. Time]";
timezoneMap["Pacific/Galapagos"] = "Pacific/Galapagos [Galapagos Time]";
timezoneMap["SystemV/CST6"] = "SystemV/CST6 [Central Standard Time]";
timezoneMap["SystemV/CST6CDT"] = "SystemV/CST6CDT [Central Standard Time]";
timezoneMap["US/Central"] = "US/Central [Central Standard Time]";
timezoneMap["US/Indiana-Starke"] = "US/Indiana-Starke [Central Standard Time]";
timezoneMap["America/Atikokan"] = "America/Atikokan [Eastern Standard Time]";
timezoneMap["America/Bogota"] = "America/Bogota [Colombia Time]";
timezoneMap["America/Cayman"] = "America/Cayman [Eastern Standard Time]";
timezoneMap["America/Coral_Harbour"] = "America/Coral_Harbour [Eastern Standard Time]";
timezoneMap["America/Detroit"] = "America/Detroit [Eastern Standard Time]";
timezoneMap["America/Fort_Wayne"] = "America/Fort_Wayne [Eastern Standard Time]";
timezoneMap["America/Grand_Turk"] = "America/Grand_Turk [Eastern Standard Time]";
timezoneMap["America/Guayaquil"] = "America/Guayaquil [Ecuador Time]";
timezoneMap["America/Havana"] = "America/Havana [Cuba Standard Time]";
timezoneMap["America/Indiana/Indianapolis"] = "America/Indiana/Indianapolis [Eastern Standard Time]";
timezoneMap["America/Indiana/Marengo"] = "America/Indiana/Marengo [Eastern Standard Time]";
timezoneMap["America/Indiana/Petersburg"] = "America/Indiana/Petersburg [Eastern Standard Time]";
timezoneMap["America/Indiana/Vevay"] = "America/Indiana/Vevay [Eastern Standard Time]";
timezoneMap["America/Indiana/Vincennes"] = "America/Indiana/Vincennes [Eastern Standard Time]";
timezoneMap["America/Indiana/Winamac"] = "America/Indiana/Winamac [Eastern Standard Time]";
timezoneMap["America/Indianapolis"] = "America/Indianapolis [Eastern Standard Time]";
timezoneMap["America/Iqaluit"] = "America/Iqaluit [Eastern Standard Time]";
timezoneMap["America/Jamaica"] = "America/Jamaica [Eastern Standard Time]";
timezoneMap["America/Kentucky/Louisville"] = "America/Kentucky/Louisville [Eastern Standard Time]";
timezoneMap["America/Kentucky/Monticello"] = "America/Kentucky/Monticello [Eastern Standard Time]";
timezoneMap["America/Lima"] = "America/Lima [Peru Time]";
timezoneMap["America/Louisville"] = "America/Louisville [Eastern Standard Time]";
timezoneMap["America/Montreal"] = "America/Montreal [Eastern Standard Time]";
timezoneMap["America/Nassau"] = "America/Nassau [Eastern Standard Time]";
timezoneMap["America/New_York"] = "America/New_York [Eastern Standard Time]";
timezoneMap["America/Nipigon"] = "America/Nipigon [Eastern Standard Time]";
timezoneMap["America/Panama"] = "America/Panama [Eastern Standard Time]";
timezoneMap["America/Pangnirtung"] = "America/Pangnirtung [Eastern Standard Time]";
timezoneMap["America/Port-au-Prince"] = "America/Port-au-Prince [Eastern Standard Time]";
timezoneMap["America/Thunder_Bay"] = "America/Thunder_Bay [Eastern Standard Time]";
timezoneMap["America/Toronto"] = "America/Toronto [Eastern Standard Time]";
timezoneMap["Canada/Eastern"] = "Canada/Eastern [Eastern Standard Time]";
timezoneMap["Cuba"] = "Cuba [Cuba Standard Time]";
timezoneMap["EST"] = "EST [Eastern Standard Time]";
timezoneMap["EST5EDT"] = "EST5EDT [Eastern Standard Time]";
timezoneMap["Etc/GMT+5"] = "Etc/GMT+5 [GMT-05:00]";
timezoneMap["IET"] = "IET [Eastern Standard Time]";
timezoneMap["Jamaica"] = "Jamaica [Eastern Standard Time]";
timezoneMap["SystemV/EST5"] = "SystemV/EST5 [Eastern Standard Time]";
timezoneMap["SystemV/EST5EDT"] = "SystemV/EST5EDT [Eastern Standard Time]";
timezoneMap["US/East-Indiana"] = "US/East-Indiana [Eastern Standard Time]";
timezoneMap["US/Eastern"] = "US/Eastern [Eastern Standard Time]";
timezoneMap["US/Michigan"] = "US/Michigan [Eastern Standard Time]";
timezoneMap["America/Caracas"] = "America/Caracas [Venezuela Time]";
timezoneMap["America/Anguilla"] = "America/Anguilla [Atlantic Standard Time]";
timezoneMap["America/Antigua"] = "America/Antigua [Atlantic Standard Time]";
timezoneMap["America/Argentina/San_Luis"] = "America/Argentina/San_Luis [Western Argentine Time]";
timezoneMap["America/Aruba"] = "America/Aruba [Atlantic Standard Time]";
timezoneMap["America/Asuncion"] = "America/Asuncion [Paraguay Time]";
timezoneMap["America/Barbados"] = "America/Barbados [Atlantic Standard Time]";
timezoneMap["America/Blanc-Sablon"] = "America/Blanc-Sablon [Atlantic Standard Time]";
timezoneMap["America/Boa_Vista"] = "America/Boa_Vista [Amazon Time]";
timezoneMap["America/Campo_Grande"] = "America/Campo_Grande [Amazon Time]";
timezoneMap["America/Cuiaba"] = "America/Cuiaba [Amazon Time]";
timezoneMap["America/Curacao"] = "America/Curacao [Atlantic Standard Time]";
timezoneMap["America/Dominica"] = "America/Dominica [Atlantic Standard Time]";
timezoneMap["America/Eirunepe"] = "America/Eirunepe [Amazon Time]";
timezoneMap["America/Glace_Bay"] = "America/Glace_Bay [Atlantic Standard Time]";
timezoneMap["America/Goose_Bay"] = "America/Goose_Bay [Atlantic Standard Time]";
timezoneMap["America/Grenada"] = "America/Grenada [Atlantic Standard Time]";
timezoneMap["America/Guadeloupe"] = "America/Guadeloupe [Atlantic Standard Time]";
timezoneMap["America/Guyana"] = "America/Guyana [Guyana Time]";
timezoneMap["America/Halifax"] = "America/Halifax [Atlantic Standard Time]";
timezoneMap["America/Kralendijk"] = "America/Kralendijk [GMT-04:00]";
timezoneMap["America/La_Paz"] = "America/La_Paz [Bolivia Time]";
timezoneMap["America/Lower_Princes"] = "America/Lower_Princes [GMT-04:00]";
timezoneMap["America/Manaus"] = "America/Manaus [Amazon Time]";
timezoneMap["America/Marigot"] = "America/Marigot [Atlantic Standard Time]";
timezoneMap["America/Martinique"] = "America/Martinique [Atlantic Standard Time]";
timezoneMap["America/Moncton"] = "America/Moncton [Atlantic Standard Time]";
timezoneMap["America/Montserrat"] = "America/Montserrat [Atlantic Standard Time]";
timezoneMap["America/Port_of_Spain"] = "America/Port_of_Spain [Atlantic Standard Time]";
timezoneMap["America/Porto_Acre"] = "America/Porto_Acre [Amazon Time]";
timezoneMap["America/Porto_Velho"] = "America/Porto_Velho [Amazon Time]";
timezoneMap["America/Puerto_Rico"] = "America/Puerto_Rico [Atlantic Standard Time]";
timezoneMap["America/Rio_Branco"] = "America/Rio_Branco [Amazon Time]";
timezoneMap["America/Santiago"] = "America/Santiago [Chile Time]";
timezoneMap["America/Santo_Domingo"] = "America/Santo_Domingo [Atlantic Standard Time]";
timezoneMap["America/St_Barthelemy"] = "America/St_Barthelemy [Atlantic Standard Time]";
timezoneMap["America/St_Kitts"] = "America/St_Kitts [Atlantic Standard Time]";
timezoneMap["America/St_Lucia"] = "America/St_Lucia [Atlantic Standard Time]";
timezoneMap["America/St_Thomas"] = "America/St_Thomas [Atlantic Standard Time]";
timezoneMap["America/St_Vincent"] = "America/St_Vincent [Atlantic Standard Time]";
timezoneMap["America/Thule"] = "America/Thule [Atlantic Standard Time]";
timezoneMap["America/Tortola"] = "America/Tortola [Atlantic Standard Time]";
timezoneMap["America/Virgin"] = "America/Virgin [Atlantic Standard Time]";
timezoneMap["Antarctica/Palmer"] = "Antarctica/Palmer [Chile Time]";
timezoneMap["Atlantic/Bermuda"] = "Atlantic/Bermuda [Atlantic Standard Time]";
timezoneMap["Brazil/Acre"] = "Brazil/Acre [Amazon Time]";
timezoneMap["Brazil/West"] = "Brazil/West [Amazon Time]";
timezoneMap["Canada/Atlantic"] = "Canada/Atlantic [Atlantic Standard Time]";
timezoneMap["Chile/Continental"] = "Chile/Continental [Chile Time]";
timezoneMap["Etc/GMT+4"] = "Etc/GMT+4 [GMT-04:00]";
timezoneMap["PRT"] = "PRT [Atlantic Standard Time]";
timezoneMap["SystemV/AST4"] = "SystemV/AST4 [Atlantic Standard Time]";
timezoneMap["SystemV/AST4ADT"] = "SystemV/AST4ADT [Atlantic Standard Time]";
timezoneMap["America/St_Johns"] = "America/St_Johns [Newfoundland Standard Time]";
timezoneMap["CNT"] = "CNT [Newfoundland Standard Time]";
timezoneMap["Canada/Newfoundland"] = "Canada/Newfoundland [Newfoundland Standard Time]";
timezoneMap["AGT"] = "AGT [Argentine Time]";
timezoneMap["America/Araguaina"] = "America/Araguaina [Brasilia Time]";
timezoneMap["America/Argentina/Buenos_Aires"] = "America/Argentina/Buenos_Aires [Argentine Time]";
timezoneMap["America/Argentina/Catamarca"] = "America/Argentina/Catamarca [Argentine Time]";
timezoneMap["America/Argentina/ComodRivadavia"] = "America/Argentina/ComodRivadavia [Argentine Time]";
timezoneMap["America/Argentina/Cordoba"] = "America/Argentina/Cordoba [Argentine Time]";
timezoneMap["America/Argentina/Jujuy"] = "America/Argentina/Jujuy [Argentine Time]";
timezoneMap["America/Argentina/La_Rioja"] = "America/Argentina/La_Rioja [Argentine Time]";
timezoneMap["America/Argentina/Mendoza"] = "America/Argentina/Mendoza [Argentine Time]";
timezoneMap["America/Argentina/Rio_Gallegos"] = "America/Argentina/Rio_Gallegos [Argentine Time]";
timezoneMap["America/Argentina/Salta"] = "America/Argentina/Salta [Argentine Time]";
timezoneMap["America/Argentina/San_Juan"] = "America/Argentina/San_Juan [Argentine Time]";
timezoneMap["America/Argentina/Tucuman"] = "America/Argentina/Tucuman [Argentine Time]";
timezoneMap["America/Argentina/Ushuaia"] = "America/Argentina/Ushuaia [Argentine Time]";
timezoneMap["America/Bahia"] = "America/Bahia [Brasilia Time]";
timezoneMap["America/Belem"] = "America/Belem [Brasilia Time]";
timezoneMap["America/Buenos_Aires"] = "America/Buenos_Aires [Argentine Time]";
timezoneMap["America/Catamarca"] = "America/Catamarca [Argentine Time]";
timezoneMap["America/Cayenne"] = "America/Cayenne [French Guiana Time]";
timezoneMap["America/Cordoba"] = "America/Cordoba [Argentine Time]";
timezoneMap["America/Fortaleza"] = "America/Fortaleza [Brasilia Time]";
timezoneMap["America/Godthab"] = "America/Godthab [Western Greenland Time]";
timezoneMap["America/Jujuy"] = "America/Jujuy [Argentine Time]";
timezoneMap["America/Maceio"] = "America/Maceio [Brasilia Time]";
timezoneMap["America/Mendoza"] = "America/Mendoza [Argentine Time]";
timezoneMap["America/Miquelon"] = "America/Miquelon [Pierre & Miquelon Standard Time]";
timezoneMap["America/Montevideo"] = "America/Montevideo [Uruguay Time]";
timezoneMap["America/Paramaribo"] = "America/Paramaribo [Suriname Time]";
timezoneMap["America/Recife"] = "America/Recife [Brasilia Time]";
timezoneMap["America/Rosario"] = "America/Rosario [Argentine Time]";
timezoneMap["America/Santarem"] = "America/Santarem [Brasilia Time]";
timezoneMap["America/Sao_Paulo"] = "America/Sao_Paulo [Brasilia Time]";
timezoneMap["Antarctica/Rothera"] = "Antarctica/Rothera [Rothera Time]";
timezoneMap["Atlantic/Stanley"] = "Atlantic/Stanley [Falkland Is. Time]";
timezoneMap["BET"] = "BET [Brasilia Time]";
timezoneMap["Brazil/East"] = "Brazil/East [Brasilia Time]";
timezoneMap["Etc/GMT+3"] = "Etc/GMT+3 [GMT-03:00]";
timezoneMap["America/Noronha"] = "America/Noronha [Fernando de Noronha Time]";
timezoneMap["Atlantic/South_Georgia"] = "Atlantic/South_Georgia [South Georgia Standard Time]";
timezoneMap["Brazil/DeNoronha"] = "Brazil/DeNoronha [Fernando de Noronha Time]";
timezoneMap["Etc/GMT+2"] = "Etc/GMT+2 [GMT-02:00]";
timezoneMap["America/Scoresbysund"] = "America/Scoresbysund [Eastern Greenland Time]";
timezoneMap["Atlantic/Azores"] = "Atlantic/Azores [Azores Time]";
timezoneMap["Atlantic/Cape_Verde"] = "Atlantic/Cape_Verde [Cape Verde Time]";
timezoneMap["Etc/GMT+1"] = "Etc/GMT+1 [GMT-01:00]";
timezoneMap["Africa/Abidjan"] = "Africa/Abidjan [Greenwich Mean Time]";
timezoneMap["Africa/Accra"] = "Africa/Accra [Ghana Mean Time]";
timezoneMap["Africa/Bamako"] = "Africa/Bamako [Greenwich Mean Time]";
timezoneMap["Africa/Banjul"] = "Africa/Banjul [Greenwich Mean Time]";
timezoneMap["Africa/Bissau"] = "Africa/Bissau [Greenwich Mean Time]";
timezoneMap["Africa/Casablanca"] = "Africa/Casablanca [Western European Time]";
timezoneMap["Africa/Conakry"] = "Africa/Conakry [Greenwich Mean Time]";
timezoneMap["Africa/Dakar"] = "Africa/Dakar [Greenwich Mean Time]";
timezoneMap["Africa/El_Aaiun"] = "Africa/El_Aaiun [Western European Time]";
timezoneMap["Africa/Freetown"] = "Africa/Freetown [Greenwich Mean Time]";
timezoneMap["Africa/Lome"] = "Africa/Lome [Greenwich Mean Time]";
timezoneMap["Africa/Monrovia"] = "Africa/Monrovia [Greenwich Mean Time]";
timezoneMap["Africa/Nouakchott"] = "Africa/Nouakchott [Greenwich Mean Time]";
timezoneMap["Africa/Ouagadougou"] = "Africa/Ouagadougou [Greenwich Mean Time]";
timezoneMap["Africa/Sao_Tome"] = "Africa/Sao_Tome [Greenwich Mean Time]";
timezoneMap["Africa/Timbuktu"] = "Africa/Timbuktu [Greenwich Mean Time]";
timezoneMap["America/Danmarkshavn"] = "America/Danmarkshavn [Greenwich Mean Time]";
timezoneMap["Atlantic/Canary"] = "Atlantic/Canary [Western European Time]";
timezoneMap["Atlantic/Faeroe"] = "Atlantic/Faeroe [Western European Time]";
timezoneMap["Atlantic/Faroe"] = "Atlantic/Faroe [Western European Time]";
timezoneMap["Atlantic/Madeira"] = "Atlantic/Madeira [Western European Time]";
timezoneMap["Atlantic/Reykjavik"] = "Atlantic/Reykjavik [Greenwich Mean Time]";
timezoneMap["Atlantic/St_Helena"] = "Atlantic/St_Helena [Greenwich Mean Time]";
timezoneMap["Eire"] = "Eire [Greenwich Mean Time]";
timezoneMap["Etc/GMT"] = "Etc/GMT [GMT+00:00]";
timezoneMap["Etc/GMT+0"] = "Etc/GMT+0 [GMT+00:00]";
timezoneMap["Etc/GMT-0"] = "Etc/GMT-0 [GMT+00:00]";
timezoneMap["Etc/GMT0"] = "Etc/GMT0 [GMT+00:00]";
timezoneMap["Etc/Greenwich"] = "Etc/Greenwich [Greenwich Mean Time]";
timezoneMap["Etc/UCT"] = "Etc/UCT [Coordinated Universal Time]";
timezoneMap["Etc/UTC"] = "Etc/UTC [Coordinated Universal Time]";
timezoneMap["Etc/Universal"] = "Etc/Universal [Coordinated Universal Time]";
timezoneMap["Etc/Zulu"] = "Etc/Zulu [Coordinated Universal Time]";
timezoneMap["Europe/Belfast"] = "Europe/Belfast [Greenwich Mean Time]";
timezoneMap["Europe/Dublin"] = "Europe/Dublin [Greenwich Mean Time]";
timezoneMap["Europe/Guernsey"] = "Europe/Guernsey [Greenwich Mean Time]";
timezoneMap["Europe/Isle_of_Man"] = "Europe/Isle_of_Man [Greenwich Mean Time]";
timezoneMap["Europe/Jersey"] = "Europe/Jersey [Greenwich Mean Time]";
timezoneMap["Europe/Lisbon"] = "Europe/Lisbon [Western European Time]";
timezoneMap["Europe/London"] = "Europe/London [Greenwich Mean Time]";
timezoneMap["GB"] = "GB [Greenwich Mean Time]";
timezoneMap["GB-Eire"] = "GB-Eire [Greenwich Mean Time]";
timezoneMap["GMT"] = "GMT [Greenwich Mean Time]";
timezoneMap["GMT0"] = "GMT0 [GMT+00:00]";
timezoneMap["Greenwich"] = "Greenwich [Greenwich Mean Time]";
timezoneMap["Iceland"] = "Iceland [Greenwich Mean Time]";
timezoneMap["Portugal"] = "Portugal [Western European Time]";
timezoneMap["UCT"] = "UCT [Coordinated Universal Time]";
timezoneMap["UTC"] = "UTC [Coordinated Universal Time]";
timezoneMap["Universal"] = "Universal [Coordinated Universal Time]";
timezoneMap["WET"] = "WET [Western European Time]";
timezoneMap["Zulu"] = "Zulu [Coordinated Universal Time]";
timezoneMap["Africa/Algiers"] = "Africa/Algiers [Central European Time]";
timezoneMap["Africa/Bangui"] = "Africa/Bangui [Western African Time]";
timezoneMap["Africa/Brazzaville"] = "Africa/Brazzaville [Western African Time]";
timezoneMap["Africa/Ceuta"] = "Africa/Ceuta [Central European Time]";
timezoneMap["Africa/Douala"] = "Africa/Douala [Western African Time]";
timezoneMap["Africa/Kinshasa"] = "Africa/Kinshasa [Western African Time]";
timezoneMap["Africa/Lagos"] = "Africa/Lagos [Western African Time]";
timezoneMap["Africa/Libreville"] = "Africa/Libreville [Western African Time]";
timezoneMap["Africa/Luanda"] = "Africa/Luanda [Western African Time]";
timezoneMap["Africa/Malabo"] = "Africa/Malabo [Western African Time]";
timezoneMap["Africa/Ndjamena"] = "Africa/Ndjamena [Western African Time]";
timezoneMap["Africa/Niamey"] = "Africa/Niamey [Western African Time]";
timezoneMap["Africa/Porto-Novo"] = "Africa/Porto-Novo [Western African Time]";
timezoneMap["Africa/Tripoli"] = "Africa/Tripoli [Eastern European Time]";
timezoneMap["Africa/Tunis"] = "Africa/Tunis [Central European Time]";
timezoneMap["Africa/Windhoek"] = "Africa/Windhoek [Western African Time]";
timezoneMap["Arctic/Longyearbyen"] = "Arctic/Longyearbyen [Central European Time]";
timezoneMap["Atlantic/Jan_Mayen"] = "Atlantic/Jan_Mayen [Central European Time]";
timezoneMap["CET"] = "CET [Central European Time]";
timezoneMap["ECT"] = "ECT [Central European Time]";
timezoneMap["Etc/GMT-1"] = "Etc/GMT-1 [GMT+01:00]";
timezoneMap["Europe/Amsterdam"] = "Europe/Amsterdam [Central European Time]";
timezoneMap["Europe/Andorra"] = "Europe/Andorra [Central European Time]";
timezoneMap["Europe/Belgrade"] = "Europe/Belgrade [Central European Time]";
timezoneMap["Europe/Berlin"] = "Europe/Berlin [Central European Time]";
timezoneMap["Europe/Bratislava"] = "Europe/Bratislava [Central European Time]";
timezoneMap["Europe/Brussels"] = "Europe/Brussels [Central European Time]";
timezoneMap["Europe/Budapest"] = "Europe/Budapest [Central European Time]";
timezoneMap["Europe/Busingen"] = "Europe/Busingen [GMT+01:00]";
timezoneMap["Europe/Copenhagen"] = "Europe/Copenhagen [Central European Time]";
timezoneMap["Europe/Gibraltar"] = "Europe/Gibraltar [Central European Time]";
timezoneMap["Europe/Ljubljana"] = "Europe/Ljubljana [Central European Time]";
timezoneMap["Europe/Luxembourg"] = "Europe/Luxembourg [Central European Time]";
timezoneMap["Europe/Madrid"] = "Europe/Madrid [Central European Time]";
timezoneMap["Europe/Malta"] = "Europe/Malta [Central European Time]";
timezoneMap["Europe/Monaco"] = "Europe/Monaco [Central European Time]";
timezoneMap["Europe/Oslo"] = "Europe/Oslo [Central European Time]";
timezoneMap["Europe/Paris"] = "Europe/Paris [Central European Time]";
timezoneMap["Europe/Podgorica"] = "Europe/Podgorica [Central European Time]";
timezoneMap["Europe/Prague"] = "Europe/Prague [Central European Time]";
timezoneMap["Europe/Rome"] = "Europe/Rome [Central European Time]";
timezoneMap["Europe/San_Marino"] = "Europe/San_Marino [Central European Time]";
timezoneMap["Europe/Sarajevo"] = "Europe/Sarajevo [Central European Time]";
timezoneMap["Europe/Skopje"] = "Europe/Skopje [Central European Time]";
timezoneMap["Europe/Stockholm"] = "Europe/Stockholm [Central European Time]";
timezoneMap["Europe/Tirane"] = "Europe/Tirane [Central European Time]";
timezoneMap["Europe/Vaduz"] = "Europe/Vaduz [Central European Time]";
timezoneMap["Europe/Vatican"] = "Europe/Vatican [Central European Time]";
timezoneMap["Europe/Vienna"] = "Europe/Vienna [Central European Time]";
timezoneMap["Europe/Warsaw"] = "Europe/Warsaw [Central European Time]";
timezoneMap["Europe/Zagreb"] = "Europe/Zagreb [Central European Time]";
timezoneMap["Europe/Zurich"] = "Europe/Zurich [Central European Time]";
timezoneMap["Libya"] = "Libya [Eastern European Time]";
timezoneMap["MET"] = "MET [Middle Europe Time]";
timezoneMap["ART"] = "ART [Eastern European Time]";
timezoneMap["Africa/Blantyre"] = "Africa/Blantyre [Central African Time]";
timezoneMap["Africa/Bujumbura"] = "Africa/Bujumbura [Central African Time]";
timezoneMap["Africa/Cairo"] = "Africa/Cairo [Eastern European Time]";
timezoneMap["Africa/Gaborone"] = "Africa/Gaborone [Central African Time]";
timezoneMap["Africa/Harare"] = "Africa/Harare [Central African Time]";
timezoneMap["Africa/Johannesburg"] = "Africa/Johannesburg [South Africa Standard Time]";
timezoneMap["Africa/Kigali"] = "Africa/Kigali [Central African Time]";
timezoneMap["Africa/Lubumbashi"] = "Africa/Lubumbashi [Central African Time]";
timezoneMap["Africa/Lusaka"] = "Africa/Lusaka [Central African Time]";
timezoneMap["Africa/Maputo"] = "Africa/Maputo [Central African Time]";
timezoneMap["Africa/Maseru"] = "Africa/Maseru [South Africa Standard Time]";
timezoneMap["Africa/Mbabane"] = "Africa/Mbabane [South Africa Standard Time]";
timezoneMap["Asia/Amman"] = "Asia/Amman [Eastern European Time]";
timezoneMap["Asia/Beirut"] = "Asia/Beirut [Eastern European Time]";
timezoneMap["Asia/Damascus"] = "Asia/Damascus [Eastern European Time]";
timezoneMap["Asia/Gaza"] = "Asia/Gaza [Eastern European Time]";
timezoneMap["Asia/Hebron"] = "Asia/Hebron [GMT+02:00]";
timezoneMap["Asia/Istanbul"] = "Asia/Istanbul [Eastern European Time]";
timezoneMap["Asia/Jerusalem"] = "Asia/Jerusalem [Israel Standard Time]";
timezoneMap["Asia/Nicosia"] = "Asia/Nicosia [Eastern European Time]";
timezoneMap["Asia/Tel_Aviv"] = "Asia/Tel_Aviv [Israel Standard Time]";
timezoneMap["CAT"] = "CAT [Central African Time]";
timezoneMap["EET"] = "EET [Eastern European Time]";
timezoneMap["Egypt"] = "Egypt [Eastern European Time]";
timezoneMap["Etc/GMT-2"] = "Etc/GMT-2 [GMT+02:00]";
timezoneMap["Europe/Athens"] = "Europe/Athens [Eastern European Time]";
timezoneMap["Europe/Bucharest"] = "Europe/Bucharest [Eastern European Time]";
timezoneMap["Europe/Chisinau"] = "Europe/Chisinau [Eastern European Time]";
timezoneMap["Europe/Helsinki"] = "Europe/Helsinki [Eastern European Time]";
timezoneMap["Europe/Istanbul"] = "Europe/Istanbul [Eastern European Time]";
timezoneMap["Europe/Kiev"] = "Europe/Kiev [Eastern European Time]";
timezoneMap["Europe/Mariehamn"] = "Europe/Mariehamn [Eastern European Time]";
timezoneMap["Europe/Nicosia"] = "Europe/Nicosia [Eastern European Time]";
timezoneMap["Europe/Riga"] = "Europe/Riga [Eastern European Time]";
timezoneMap["Europe/Simferopol"] = "Europe/Simferopol [Eastern European Time]";
timezoneMap["Europe/Sofia"] = "Europe/Sofia [Eastern European Time]";
timezoneMap["Europe/Tallinn"] = "Europe/Tallinn [Eastern European Time]";
timezoneMap["Europe/Tiraspol"] = "Europe/Tiraspol [Eastern European Time]";
timezoneMap["Europe/Uzhgorod"] = "Europe/Uzhgorod [Eastern European Time]";
timezoneMap["Europe/Vilnius"] = "Europe/Vilnius [Eastern European Time]";
timezoneMap["Europe/Zaporozhye"] = "Europe/Zaporozhye [Eastern European Time]";
timezoneMap["Israel"] = "Israel [Israel Standard Time]";
timezoneMap["Turkey"] = "Turkey [Eastern European Time]";
timezoneMap["Africa/Addis_Ababa"] = "Africa/Addis_Ababa [Eastern African Time]";
timezoneMap["Africa/Asmara"] = "Africa/Asmara [Eastern African Time]";
timezoneMap["Africa/Asmera"] = "Africa/Asmera [Eastern African Time]";
timezoneMap["Africa/Dar_es_Salaam"] = "Africa/Dar_es_Salaam [Eastern African Time]";
timezoneMap["Africa/Djibouti"] = "Africa/Djibouti [Eastern African Time]";
timezoneMap["Africa/Juba"] = "Africa/Juba [GMT+03:00]";
timezoneMap["Africa/Kampala"] = "Africa/Kampala [Eastern African Time]";
timezoneMap["Africa/Khartoum"] = "Africa/Khartoum [Eastern African Time]";
timezoneMap["Africa/Mogadishu"] = "Africa/Mogadishu [Eastern African Time]";
timezoneMap["Africa/Nairobi"] = "Africa/Nairobi [Eastern African Time]";
timezoneMap["Antarctica/Syowa"] = "Antarctica/Syowa [Syowa Time]";
timezoneMap["Asia/Aden"] = "Asia/Aden [Arabia Standard Time]";
timezoneMap["Asia/Baghdad"] = "Asia/Baghdad [Arabia Standard Time]";
timezoneMap["Asia/Bahrain"] = "Asia/Bahrain [Arabia Standard Time]";
timezoneMap["Asia/Kuwait"] = "Asia/Kuwait [Arabia Standard Time]";
timezoneMap["Asia/Qatar"] = "Asia/Qatar [Arabia Standard Time]";
timezoneMap["Asia/Riyadh"] = "Asia/Riyadh [Arabia Standard Time]";
timezoneMap["EAT"] = "EAT [Eastern African Time]";
timezoneMap["Etc/GMT-3"] = "Etc/GMT-3 [GMT+03:00]";
timezoneMap["Europe/Kaliningrad"] = "Europe/Kaliningrad [Eastern European Time]";
timezoneMap["Europe/Minsk"] = "Europe/Minsk [Eastern European Time]";
timezoneMap["Indian/Antananarivo"] = "Indian/Antananarivo [Eastern African Time]";
timezoneMap["Indian/Comoro"] = "Indian/Comoro [Eastern African Time]";
timezoneMap["Indian/Mayotte"] = "Indian/Mayotte [Eastern African Time]";
timezoneMap["Asia/Riyadh87"] = "Asia/Riyadh87 [GMT+03:07]";
timezoneMap["Asia/Riyadh88"] = "Asia/Riyadh88 [GMT+03:07]";
timezoneMap["Asia/Riyadh89"] = "Asia/Riyadh89 [GMT+03:07]";
timezoneMap["Mideast/Riyadh87"] = "Mideast/Riyadh87 [GMT+03:07]";
timezoneMap["Mideast/Riyadh88"] = "Mideast/Riyadh88 [GMT+03:07]";
timezoneMap["Mideast/Riyadh89"] = "Mideast/Riyadh89 [GMT+03:07]";
timezoneMap["Asia/Tehran"] = "Asia/Tehran [Iran Standard Time]";
timezoneMap["Iran"] = "Iran [Iran Standard Time]";
timezoneMap["Asia/Baku"] = "Asia/Baku [Azerbaijan Time]";
timezoneMap["Asia/Dubai"] = "Asia/Dubai [Gulf Standard Time]";
timezoneMap["Asia/Muscat"] = "Asia/Muscat [Gulf Standard Time]";
timezoneMap["Asia/Tbilisi"] = "Asia/Tbilisi [Georgia Time]";
timezoneMap["Asia/Yerevan"] = "Asia/Yerevan [Armenia Time]";
timezoneMap["Etc/GMT-4"] = "Etc/GMT-4 [GMT+04:00]";
timezoneMap["Europe/Moscow"] = "Europe/Moscow [Moscow Standard Time]";
timezoneMap["Europe/Samara"] = "Europe/Samara [Samara Time]";
timezoneMap["Europe/Volgograd"] = "Europe/Volgograd [Volgograd Time]";
timezoneMap["Indian/Mahe"] = "Indian/Mahe [Seychelles Time]";
timezoneMap["Indian/Mauritius"] = "Indian/Mauritius [Mauritius Time]";
timezoneMap["Indian/Reunion"] = "Indian/Reunion [Reunion Time]";
timezoneMap["NET"] = "NET [Armenia Time]";
timezoneMap["W-SU"] = "W-SU [Moscow Standard Time]";
timezoneMap["Asia/Kabul"] = "Asia/Kabul [Afghanistan Time]";
timezoneMap["Antarctica/Mawson"] = "Antarctica/Mawson [Mawson Time]";
timezoneMap["Asia/Aqtau"] = "Asia/Aqtau [Aqtau Time]";
timezoneMap["Asia/Aqtobe"] = "Asia/Aqtobe [Aqtobe Time]";
timezoneMap["Asia/Ashgabat"] = "Asia/Ashgabat [Turkmenistan Time]";
timezoneMap["Asia/Ashkhabad"] = "Asia/Ashkhabad [Turkmenistan Time]";
timezoneMap["Asia/Dushanbe"] = "Asia/Dushanbe [Tajikistan Time]";
timezoneMap["Asia/Karachi"] = "Asia/Karachi [Pakistan Time]";
timezoneMap["Asia/Oral"] = "Asia/Oral [Oral Time]";
timezoneMap["Asia/Samarkand"] = "Asia/Samarkand [Uzbekistan Time]";
timezoneMap["Asia/Tashkent"] = "Asia/Tashkent [Uzbekistan Time]";
timezoneMap["Etc/GMT-5"] = "Etc/GMT-5 [GMT+05:00]";
timezoneMap["Indian/Kerguelen"] = "Indian/Kerguelen [French Southern & Antarctic Lands Time]";
timezoneMap["Indian/Maldives"] = "Indian/Maldives [Maldives Time]";
timezoneMap["PLT"] = "PLT [Pakistan Time]";
timezoneMap["Asia/Calcutta"] = "Asia/Calcutta [India Standard Time]";
timezoneMap["Asia/Colombo"] = "Asia/Colombo [India Standard Time]";
timezoneMap["Asia/Kolkata"] = "Asia/Kolkata [India Standard Time]";
timezoneMap["IST"] = "IST [India Standard Time]";
timezoneMap["Asia/Kathmandu"] = "Asia/Kathmandu [Nepal Time]";
timezoneMap["Asia/Katmandu"] = "Asia/Katmandu [Nepal Time]";
timezoneMap["Antarctica/Vostok"] = "Antarctica/Vostok [Vostok Time]";
timezoneMap["Asia/Almaty"] = "Asia/Almaty [Alma-Ata Time]";
timezoneMap["Asia/Bishkek"] = "Asia/Bishkek [Kirgizstan Time]";
timezoneMap["Asia/Dacca"] = "Asia/Dacca [Bangladesh Time]";
timezoneMap["Asia/Dhaka"] = "Asia/Dhaka [Bangladesh Time]";
timezoneMap["Asia/Qyzylorda"] = "Asia/Qyzylorda [Qyzylorda Time]";
timezoneMap["Asia/Thimbu"] = "Asia/Thimbu [Bhutan Time]";
timezoneMap["Asia/Thimphu"] = "Asia/Thimphu [Bhutan Time]";
timezoneMap["Asia/Yekaterinburg"] = "Asia/Yekaterinburg [Yekaterinburg Time]";
timezoneMap["BST"] = "BST [Bangladesh Time]";
timezoneMap["Etc/GMT-6"] = "Etc/GMT-6 [GMT+06:00]";
timezoneMap["Indian/Chagos"] = "Indian/Chagos [Indian Ocean Territory Time]";
timezoneMap["Asia/Rangoon"] = "Asia/Rangoon [Myanmar Time]";
timezoneMap["Indian/Cocos"] = "Indian/Cocos [Cocos Islands Time]";
timezoneMap["Antarctica/Davis"] = "Antarctica/Davis [Davis Time]";
timezoneMap["Asia/Bangkok"] = "Asia/Bangkok [Indochina Time]";
timezoneMap["Asia/Ho_Chi_Minh"] = "Asia/Ho_Chi_Minh [Indochina Time]";
timezoneMap["Asia/Hovd"] = "Asia/Hovd [Hovd Time]";
timezoneMap["Asia/Jakarta"] = "Asia/Jakarta [West Indonesia Time]";
timezoneMap["Asia/Novokuznetsk"] = "Asia/Novokuznetsk [Novosibirsk Time]";
timezoneMap["Asia/Novosibirsk"] = "Asia/Novosibirsk [Novosibirsk Time]";
timezoneMap["Asia/Omsk"] = "Asia/Omsk [Omsk Time]";
timezoneMap["Asia/Phnom_Penh"] = "Asia/Phnom_Penh [Indochina Time]";
timezoneMap["Asia/Pontianak"] = "Asia/Pontianak [West Indonesia Time]";
timezoneMap["Asia/Saigon"] = "Asia/Saigon [Indochina Time]";
timezoneMap["Asia/Vientiane"] = "Asia/Vientiane [Indochina Time]";
timezoneMap["Etc/GMT-7"] = "Etc/GMT-7 [GMT+07:00]";
timezoneMap["Indian/Christmas"] = "Indian/Christmas [Christmas Island Time]";
timezoneMap["VST"] = "VST [Indochina Time]";
timezoneMap["Antarctica/Casey"] = "Antarctica/Casey [Western Standard Time (Australia)]";
timezoneMap["Asia/Brunei"] = "Asia/Brunei [Brunei Time]";
timezoneMap["Asia/Choibalsan"] = "Asia/Choibalsan [Choibalsan Time]";
timezoneMap["Asia/Chongqing"] = "Asia/Chongqing [China Standard Time]";
timezoneMap["Asia/Chungking"] = "Asia/Chungking [China Standard Time]";
timezoneMap["Asia/Harbin"] = "Asia/Harbin [China Standard Time]";
timezoneMap["Asia/Hong_Kong"] = "Asia/Hong_Kong [Hong Kong Time]";
timezoneMap["Asia/Kashgar"] = "Asia/Kashgar [China Standard Time]";
timezoneMap["Asia/Krasnoyarsk"] = "Asia/Krasnoyarsk [Krasnoyarsk Time]";
timezoneMap["Asia/Kuala_Lumpur"] = "Asia/Kuala_Lumpur [Malaysia Time]";
timezoneMap["Asia/Kuching"] = "Asia/Kuching [Malaysia Time]";
timezoneMap["Asia/Macao"] = "Asia/Macao [China Standard Time]";
timezoneMap["Asia/Macau"] = "Asia/Macau [China Standard Time]";
timezoneMap["Asia/Makassar"] = "Asia/Makassar [Central Indonesia Time]";
timezoneMap["Asia/Manila"] = "Asia/Manila [Philippines Time]";
timezoneMap["Asia/Shanghai"] = "Asia/Shanghai [China Standard Time]";
timezoneMap["Asia/Singapore"] = "Asia/Singapore [Singapore Time]";
timezoneMap["Asia/Taipei"] = "Asia/Taipei [China Standard Time]";
timezoneMap["Asia/Ujung_Pandang"] = "Asia/Ujung_Pandang [Central Indonesia Time]";
timezoneMap["Asia/Ulaanbaatar"] = "Asia/Ulaanbaatar [Ulaanbaatar Time]";
timezoneMap["Asia/Ulan_Bator"] = "Asia/Ulan_Bator [Ulaanbaatar Time]";
timezoneMap["Asia/Urumqi"] = "Asia/Urumqi [China Standard Time]";
timezoneMap["Australia/Perth"] = "Australia/Perth [Western Standard Time (Australia)]";
timezoneMap["Australia/West"] = "Australia/West [Western Standard Time (Australia)]";
timezoneMap["CTT"] = "CTT [China Standard Time]";
timezoneMap["Etc/GMT-8"] = "Etc/GMT-8 [GMT+08:00]";
timezoneMap["Hongkong"] = "Hongkong [Hong Kong Time]";
timezoneMap["PRC"] = "PRC [China Standard Time]";
timezoneMap["Singapore"] = "Singapore [Singapore Time]";
timezoneMap["Australia/Eucla"] = "Australia/Eucla [Central Western Standard Time (Australia)]";
timezoneMap["Asia/Dili"] = "Asia/Dili [Timor-Leste Time]";
timezoneMap["Asia/Irkutsk"] = "Asia/Irkutsk [Irkutsk Time]";
timezoneMap["Asia/Jayapura"] = "Asia/Jayapura [East Indonesia Time]";
timezoneMap["Asia/Pyongyang"] = "Asia/Pyongyang [Korea Standard Time]";
timezoneMap["Asia/Seoul"] = "Asia/Seoul [Korea Standard Time]";
timezoneMap["Asia/Tokyo"] = "Asia/Tokyo [Japan Standard Time]";
timezoneMap["Etc/GMT-9"] = "Etc/GMT-9 [GMT+09:00]";
timezoneMap["JST"] = "JST [Japan Standard Time]";
timezoneMap["Japan"] = "Japan [Japan Standard Time]";
timezoneMap["Pacific/Palau"] = "Pacific/Palau [Palau Time]";
timezoneMap["ROK"] = "ROK [Korea Standard Time]";
timezoneMap["ACT"] = "ACT [Central Standard Time (Northern Territory)]";
timezoneMap["Australia/Adelaide"] = "Australia/Adelaide [Central Standard Time (South Australia)]";
timezoneMap["Australia/Broken_Hill"] = "Australia/Broken_Hill [Central Standard Time (South Australia/New South Wales)]";
timezoneMap["Australia/Darwin"] = "Australia/Darwin [Central Standard Time (Northern Territory)]";
timezoneMap["Australia/North"] = "Australia/North [Central Standard Time (Northern Territory)]";
timezoneMap["Australia/South"] = "Australia/South [Central Standard Time (South Australia)]";
timezoneMap["Australia/Yancowinna"] = "Australia/Yancowinna [Central Standard Time (South Australia/New South Wales)]";
timezoneMap["AET"] = "AET [Eastern Standard Time (New South Wales)]";
timezoneMap["Antarctica/DumontDUrville"] = "Antarctica/DumontDUrville [Dumont-d'Urville Time]";
timezoneMap["Asia/Khandyga"] = "Asia/Khandyga [GMT+10:00]";
timezoneMap["Asia/Yakutsk"] = "Asia/Yakutsk [Yakutsk Time]";
timezoneMap["Australia/ACT"] = "Australia/ACT [Eastern Standard Time (New South Wales)]";
timezoneMap["Australia/Brisbane"] = "Australia/Brisbane [Eastern Standard Time (Queensland)]";
timezoneMap["Australia/Canberra"] = "Australia/Canberra [Eastern Standard Time (New South Wales)]";
timezoneMap["Australia/Currie"] = "Australia/Currie [Eastern Standard Time (New South Wales)]";
timezoneMap["Australia/Hobart"] = "Australia/Hobart [Eastern Standard Time (Tasmania)]";
timezoneMap["Australia/Lindeman"] = "Australia/Lindeman [Eastern Standard Time (Queensland)]";
timezoneMap["Australia/Melbourne"] = "Australia/Melbourne [Eastern Standard Time (Victoria)]";
timezoneMap["Australia/NSW"] = "Australia/NSW [Eastern Standard Time (New South Wales)]";
timezoneMap["Australia/Queensland"] = "Australia/Queensland [Eastern Standard Time (Queensland)]";
timezoneMap["Australia/Sydney"] = "Australia/Sydney [Eastern Standard Time (New South Wales)]";
timezoneMap["Australia/Tasmania"] = "Australia/Tasmania [Eastern Standard Time (Tasmania)]";
timezoneMap["Australia/Victoria"] = "Australia/Victoria [Eastern Standard Time (Victoria)]";
timezoneMap["Etc/GMT-10"] = "Etc/GMT-10 [GMT+10:00]";
timezoneMap["Pacific/Chuuk"] = "Pacific/Chuuk [GMT+10:00]";
timezoneMap["Pacific/Guam"] = "Pacific/Guam [Chamorro Standard Time]";
timezoneMap["Pacific/Port_Moresby"] = "Pacific/Port_Moresby [Papua New Guinea Time]";
timezoneMap["Pacific/Saipan"] = "Pacific/Saipan [Chamorro Standard Time]";
timezoneMap["Pacific/Truk"] = "Pacific/Truk [Truk Time]";
timezoneMap["Pacific/Yap"] = "Pacific/Yap [Truk Time]";
timezoneMap["Australia/LHI"] = "Australia/LHI [Lord Howe Standard Time]";
timezoneMap["Australia/Lord_Howe"] = "Australia/Lord_Howe [Lord Howe Standard Time]";
timezoneMap["Antarctica/Macquarie"] = "Antarctica/Macquarie [Macquarie Island Time]";
timezoneMap["Asia/Sakhalin"] = "Asia/Sakhalin [Sakhalin Time]";
timezoneMap["Asia/Ust-Nera"] = "Asia/Ust-Nera [GMT+11:00]";
timezoneMap["Asia/Vladivostok"] = "Asia/Vladivostok [Vladivostok Time]";
timezoneMap["Etc/GMT-11"] = "Etc/GMT-11 [GMT+11:00]";
timezoneMap["Pacific/Efate"] = "Pacific/Efate [Vanuatu Time]";
timezoneMap["Pacific/Guadalcanal"] = "Pacific/Guadalcanal [Solomon Is. Time]";
timezoneMap["Pacific/Kosrae"] = "Pacific/Kosrae [Kosrae Time]";
timezoneMap["Pacific/Noumea"] = "Pacific/Noumea [New Caledonia Time]";
timezoneMap["Pacific/Pohnpei"] = "Pacific/Pohnpei [GMT+11:00]";
timezoneMap["Pacific/Ponape"] = "Pacific/Ponape [Ponape Time]";
timezoneMap["SST"] = "SST [Solomon Is. Time]";
timezoneMap["Pacific/Norfolk"] = "Pacific/Norfolk [Norfolk Time]";
timezoneMap["Antarctica/McMurdo"] = "Antarctica/McMurdo [New Zealand Standard Time]";
timezoneMap["Antarctica/South_Pole"] = "Antarctica/South_Pole [New Zealand Standard Time]";
timezoneMap["Asia/Anadyr"] = "Asia/Anadyr [Anadyr Time]";
timezoneMap["Asia/Kamchatka"] = "Asia/Kamchatka [Petropavlovsk-Kamchatski Time]";
timezoneMap["Asia/Magadan"] = "Asia/Magadan [Magadan Time]";
timezoneMap["Etc/GMT-12"] = "Etc/GMT-12 [GMT+12:00]";
timezoneMap["Kwajalein"] = "Kwajalein [Marshall Islands Time]";
timezoneMap["NST"] = "NST [New Zealand Standard Time]";
timezoneMap["NZ"] = "NZ [New Zealand Standard Time]";
timezoneMap["Pacific/Auckland"] = "Pacific/Auckland [New Zealand Standard Time]";
timezoneMap["Pacific/Fiji"] = "Pacific/Fiji [Fiji Time]";
timezoneMap["Pacific/Funafuti"] = "Pacific/Funafuti [Tuvalu Time]";
timezoneMap["Pacific/Kwajalein"] = "Pacific/Kwajalein [Marshall Islands Time]";
timezoneMap["Pacific/Majuro"] = "Pacific/Majuro [Marshall Islands Time]";
timezoneMap["Pacific/Nauru"] = "Pacific/Nauru [Nauru Time]";
timezoneMap["Pacific/Tarawa"] = "Pacific/Tarawa [Gilbert Is. Time]";
timezoneMap["Pacific/Wake"] = "Pacific/Wake [Wake Time]";
timezoneMap["Pacific/Wallis"] = "Pacific/Wallis [Wallis & Futuna Time]";
timezoneMap["NZ-CHAT"] = "NZ-CHAT [Chatham Standard Time]";
timezoneMap["Pacific/Chatham"] = "Pacific/Chatham [Chatham Standard Time]";
timezoneMap["Etc/GMT-13"] = "Etc/GMT-13 [GMT+13:00]";
timezoneMap["MIT"] = "MIT [West Samoa Time]";
timezoneMap["Pacific/Apia"] = "Pacific/Apia [West Samoa Time]";
timezoneMap["Pacific/Enderbury"] = "Pacific/Enderbury [Phoenix Is. Time]";
timezoneMap["Pacific/Fakaofo"] = "Pacific/Fakaofo [Tokelau Time]";
timezoneMap["Pacific/Tongatapu"] = "Pacific/Tongatapu [Tonga Time]";
timezoneMap["Etc/GMT-14"] = "Etc/GMT-14 [GMT+14:00]";
timezoneMap["Pacific/Kiritimati"] = "Pacific/Kiritimati [Line Is. Time]";


// CloudStack common API helpers
cloudStack.api = {
    actions: {
        sort: function(updateCommand, objType) {
            var action = function(args) {
                $.ajax({
                    url: createURL(updateCommand),
                    data: {
                        id: args.context[objType].id,
                        sortKey: args.sortKey
                    },
                    success: function(json) {
                        args.response.success();
                    },
                    error: function(json) {
                        args.response.error(parseXMLHttpResponse(json));
                    }
                });

            };

            return {
                moveTop: {
                    action: action
                },
                moveBottom: {
                    action: action
                },
                moveUp: {
                    action: action
                },
                moveDown: {
                    action: action
                },
                moveDrag: {
                    action: action
                }
            }
        }
    },

    tags: function(args) {
        var resourceType = args.resourceType;
        var contextId = args.contextId;

        return {
            actions: {
                add: function(args) {
                    var data = args.data;
                    var resourceId = args.context[contextId][0].id;

                    $.ajax({
                        url: createURL('createTags'),
                        data: {
                            'tags[0].key': data.key,
                            'tags[0].value': data.value,
                            resourceIds: resourceId,
                            resourceType: resourceType
                        },
                        success: function(json) {
                            args.response.success({
                                _custom: {
                                    jobId: json.createtagsresponse.jobid
                                },
                                notification: {
                                    desc: 'Add tag for ' + resourceType,
                                    poll: pollAsyncJobResult
                                }
                            });
                        }
                    });
                },

                remove: function(args) {
                    var data = args.context.tagItems[0];
                    var resourceId = args.context[contextId][0].id;

                    $.ajax({
                        url: createURL('deleteTags'),
                        data: {
                            'tags[0].key': data.key,
                            'tags[0].value': data.value,
                            resourceIds: resourceId,
                            resourceType: resourceType
                        },
                        success: function(json) {
                            args.response.success({
                                _custom: {
                                    jobId: json.deletetagsresponse.jobid
                                },
                                notification: {
                                    desc: 'Remove tag for ' + resourceType,
                                    poll: pollAsyncJobResult
                                }
                            });
                        }
                    });
                }
            },
            dataProvider: function(args) {
                if (args.jsonObj != undefined) {
                    args.response.success({
                        data: args.jsonObj.tags
                    });
                } else {
                    var resourceId = args.context[contextId][0].id;
                    var data = {
                        resourceId: resourceId,
                        resourceType: resourceType
                    };

                    if (isAdmin() || isDomainAdmin()) {
                        data.listAll = true;
                    }

                    if (args.context.projects) {
                        data.projectid = args.context.projects[0].id;
                    }

                    if (args.jsonObj != null && args.jsonObj.projectid != null && data.projectid == null) {
                        data.projectid = args.jsonObj.projectid;
                    }

                    $.ajax({
                        url: createURL('listTags'),
                        data: data,
                        success: function(json) {
                            args.response.success({
                                data: json.listtagsresponse ? json.listtagsresponse.tag : []
                            });
                        },
                        error: function(json) {
                            args.response.error(parseXMLHttpResponse(json));
                        }
                    });
                }
            }
        };
    }
};

function strOrFunc(arg, args) {
    if (typeof arg === 'function')
        return arg(args);
    return arg;
}

function sortArrayByKey(arrayToSort, sortKey, reverse) {

    if(!arrayToSort){
        return;
    }
    // Move smaller items towards the front
    // or back of the array depending on if
    // we want to sort the array in reverse
    // order or not.
    var moveSmaller = reverse ? 1 : -1;

    // Move larger items towards the front
    // or back of the array depending on if
    // we want to sort the array in reverse
    // order or not.
    var moveLarger = reverse ? -1 : 1;

    /**
     * @param  {*} a
     * @param  {*} b
     * @return {Number}
     */
    arrayToSort.sort(function(a, b) {
        if (a[sortKey] < b[sortKey]) {
            return moveSmaller;
        }
        if (a[sortKey] > b[sortKey]) {
            return moveLarger;
        }
        return 0;
    });
}

$.validator.addMethod("netmask", function(value, element) {
    if (this.optional(element) && value.length == 0)
        return true;

    var valid = [ 255, 254, 252, 248, 240, 224, 192, 128, 0 ];
    var octets = value.split('.');
    if (typeof octets == 'undefined' || octets.length != 4) {
        return false;
    }
    var wasAll255 = true;
    for (index = 0; index < octets.length; index++) {
        if (octets[index] != Number(octets[index]).toString()) //making sure that "", " ", "00", "0 ","255  ", etc. will not pass
            return false;
        wasAll255 = wasAll255 && octets[index] == 255;
        if ($.inArray(Number(octets[index]), valid) < 0)
            return false;
        if (!wasAll255 && index > 0 && Number(octets[index]) != 0 && Number(octets[index - 1]) != 255)
            return false;
    }

    return true;
}, "The specified netmask is invalid.");

$.validator.addMethod("ipv6cidr", function(value, element) {
    if (this.optional(element) && value.length == 0)
        return true;

    var parts = value.split('/');
    if (typeof parts == 'undefined' || parts.length != 2) {
        return false;
    }

    if (!$.validator.methods.ipv6CustomJqueryValidator.call(this, parts[0], element))
        return false;

    if (parts[1] != Number(parts[1]).toString()) //making sure that "", " ", "00", "0 ","2  ", etc. will not pass
        return false;

    if (Number(parts[1]) < 0 || Number(parts[1] > 128))
        return false;

    return true;
}, "The specified IPv6 CIDR is invalid.");

$.validator.addMethod("ipv4cidr", function(value, element) {
    if (this.optional(element) && value.length == 0)
        return true;

    var parts = value.split('/');
    if (typeof parts == 'undefined' || parts.length != 2) {
        return false;
    }

    if (!$.validator.methods.ipv4.call(this, parts[0], element))
        return false;

    if (parts[1] != Number(parts[1]).toString()) //making sure that "", " ", "00", "0 ","2  ", etc. will not pass
        return false;

    if (Number(parts[1]) < 0 || Number(parts[1] > 32))
        return false;

    return true;
}, "The specified IPv4 CIDR is invalid.");

$.validator.addMethod("ipv46cidrs", function(value, element) {
    var result = true;
    if (value) {
        var validatorThis = this;
        value.split(',').forEach(function(item){
            if (result && !$.validator.methods.ipv46cidr.call(validatorThis, item.trim(), element)) {
                result = false;
            }
        })
    }
    return result;
}, "The specified IPv4/IPv6 CIDR input is invalid.");

$.validator.addMethod("ipv46cidr", function(value, element) {
    if (this.optional(element) && value.length == 0)
        return true;

    if ($.validator.methods.ipv4cidr.call(this, value, element) || $.validator.methods.ipv6cidr.call(this, value, element))
        return true;

    return false;
}, "The specified IPv4/IPv6 CIDR is invalid.");

jQuery.validator.addMethod("ipv4AndIpv6AddressValidator", function(value, element) {
    if (this.optional(element) && value.length == 0) {
        return true;
	}
    if (jQuery.validator.methods.ipv4.call(this, value, element) || jQuery.validator.methods.ipv6CustomJqueryValidator.call(this, value, element)) {
        return true;
    }
    return false;
}, "The specified IPv4/IPv6 address is invalid.");

jQuery.validator.addMethod("ipv6CustomJqueryValidator", function(value, element) {
    if (value == '::'){
    	return true;
    }
    return jQuery.validator.methods.ipv6.call(this, value, element);
}, "The specified IPv6 address is invalid.");


$.validator.addMethod("allzonesonly", function(value, element){

    if ((value.indexOf("-1") != -1) &&(value.length > 1))
        return false;
    return true;

},
"All Zones cannot be combined with any other zone");

cloudStack.createTemplateMethod = function (isSnapshot){
	return {
        label: 'label.create.template',
        messages: {
            confirm: function(args) {
                return 'message.create.template';
            },
            notification: function(args) {
                return 'label.create.template';
            }
        },
        createForm: {
            title: 'label.create.template',
            preFilter: cloudStack.preFilter.createTemplate,
            desc: '',
            preFilter: function(args) {
                if (args.context.volumes[0].hypervisor == "XenServer") {
                    if (isAdmin()) {
                        args.$form.find('.form-item[rel=xenserverToolsVersion61plus]').css('display', 'inline-block');
                    }
                }
            },
            fields: {
                name: {
                    label: 'label.name',
                    validation: {
                        required: true
                    }
                },
                displayText: {
                    label: 'label.description',
                    validation: {
                        required: true
                    }
                },
                xenserverToolsVersion61plus: {
                    label: 'label.xenserver.tools.version.61.plus',
                    isBoolean: true,
                    isChecked: function (args) {
                        var b = false;
                        var vmObj;
                        $.ajax({
                            url: createURL("listVirtualMachines"),
                            data: {
                                id: args.context.volumes[0].virtualmachineid
                            },
                            async: false,
                            success: function(json) {
                                vmObj = json.listvirtualmachinesresponse.virtualmachine[0];
                            }
                        });
                        if (vmObj == undefined) { //e.g. VM has failed over
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
                        } else {
                             if ('details' in vmObj && 'hypervisortoolsversion' in vmObj.details) {
                                 if (vmObj.details.hypervisortoolsversion == 'xenserver61')
                                     b = true;
                                 else
                                     b = false;
                             }
                        }
                        return b;
                    },
                    isHidden: true
                },
                osTypeId: {
                    label: 'label.os.type',
                    select: function(args) {
                        $.ajax({
                            url: createURL("listOsTypes"),
                            dataType: "json",
                            async: true,
                            success: function(json) {
                                var ostypes = json.listostypesresponse.ostype;
                                var items = [];
                                $(ostypes).each(function() {
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
                isPublic: {
                    label: 'label.public',
                    isBoolean: true
                },
                isPasswordEnabled: {
                    label: 'label.password.enabled',
                    isBoolean: true
                },
                isFeatured: {
                    label: 'label.featured',
                    isBoolean: true
                },
                isdynamicallyscalable: {
                    label: 'label.dynamically.scalable',
                    isBoolean: true
                },
                requireshvm: {
                    label: 'label.hvm',
                    docID: 'helpRegisterTemplateHvm',
                    isBoolean: true,
                    isHidden: false,
                    isChecked: false
                }
            }

        },
        action: function(args) {
            var data = {
                name: args.data.name,
                displayText: args.data.displayText,
                osTypeId: args.data.osTypeId,
                isPublic: (args.data.isPublic == "on"),
                passwordEnabled: (args.data.isPasswordEnabled == "on"),
                isdynamicallyscalable: (args.data.isdynamicallyscalable == "on"),
                requireshvm: (args.data.requireshvm == "on")
            };
            
            if(isSnapshot){
            	data.snapshotid = args.context.snapshots[0].id;
            } else{
            	data.volumeId = args.context.volumes[0].id;
            }
            if (args.$form.find('.form-item[rel=isFeatured]').css("display") != "none") {
                $.extend(data, {
                    isfeatured: (args.data.isFeatured == "on")
                });
            }

            //XenServer only (starts here)
            if (args.$form.find('.form-item[rel=xenserverToolsVersion61plus]').length > 0) {
                if (args.$form.find('.form-item[rel=xenserverToolsVersion61plus]').css("display") != "none") {
                    $.extend(data, {
                        'details[0].hypervisortoolsversion': (args.data.xenserverToolsVersion61plus == "on") ? "xenserver61" : "xenserver56"
                    });
                }
            }
            //XenServer only (ends here)

            $.ajax({
                url: createURL('createTemplate'),
                data: data,
                success: function(json) {
                    var jid = json.createtemplateresponse.jobid;
                    args.response.success({
                        _custom: {
                            jobId: jid,
                            getUpdatedItem: function(json) {
                                return {}; //no properties in this volume needs to be updated
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
    };
};
cloudStack.createTemplateFromSnapshotMethod = function (){
    return {
        label: 'label.create.template',
        messages: {
            confirm: function(args) {
                return 'message.create.template';
            },
            notification: function(args) {
                return 'label.create.template';
            }
        },
        createForm: {
            title: 'label.create.template',
            desc: '',


            fields: {
                name: {
                    label: 'label.name',
                    validation: {
                        required: true
                    }
                },
                displayText: {
                    label: 'label.description',
                    validation: {
                        required: true
                    }
                },
                osTypeId: {
                    label: 'label.os.type',
                    select: function(args) {
                        $.ajax({
                            url: createURL("listOsTypes"),
                            dataType: "json",
                            async: true,
                            success: function(json) {
                                var ostypes = json.listostypesresponse.ostype;
                                var items = [];
                                $(ostypes).each(function() {
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
                isPublic: {
                    label: 'label.public',
                    isBoolean: true
                },
                isPasswordEnabled: {
                    label: 'label.password.enabled',
                    isBoolean: true
                },
                isdynamicallyscalable: {
                    label: 'label.dynamically.scalable',
                    isBoolean: true
                }
            }
        },
        action: function(args) {
            var data = {
                snapshotid: args.context.snapshots[0].id,
                name: args.data.name,
                displayText: args.data.displayText,
                osTypeId: args.data.osTypeId,
                isPublic: (args.data.isPublic == "on"),
                passwordEnabled: (args.data.isPasswordEnabled == "on"),
                isdynamicallyscalable: (args.data.isdynamicallyscalable == "on")
            };

            $.ajax({
                url: createURL('createTemplate'),
                data: data,
                success: function(json) {
                    var jid = json.createtemplateresponse.jobid;
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
                }
            });
        },
        notification: {
            poll: pollAsyncJobResult
        }
    };
};

cloudStack.addParameterToCommandUrlParameterArrayIfValueIsNotEmpty = function(array, parameterName, value){
    if (value != null && value.length > 0) {
        array.push("&" + parameterName + "=" + encodeURIComponent(value));
    }
}

cloudStack.addUsernameAndPasswordToCommandUrlParameterArrayIfItIsNotNullAndNotEmpty = function(array, username, password){
    cloudStack.addParameterToCommandUrlParameterArrayIfValueIsNotEmpty(array, "username", username);
    cloudStack.addPasswordToCommandUrlParameterArray(array, password);
};

cloudStack.addPasswordToCommandUrlParameterArray = function(array, password){
    cloudStack.addParameterToCommandUrlParameterArrayIfValueIsNotEmpty(array, "password", password);
};

/**
 * We will only add the name and description data to the array of parameters if they are not null.
 * Moreover, we expect the name parameter to be a property ('name') of data object. 
 * The description must be a property called 'description' in the data object.   
 */
cloudStack.addNameAndDescriptionToCommandUrlParameterArray = function (array, data){
    cloudStack.addParameterToCommandUrlParameterArrayIfValueIsNotEmpty(array, "name", data.name);
    cloudStack.addParameterToCommandUrlParameterArrayIfValueIsNotEmpty(array, "description", data.description);
};

cloudStack.addNewSizeToCommandUrlParameterArrayIfItIsNotNullAndHigherThanZero = function(array, newSize){
    if(newSize == undefined || newSize == null){
        return;
    }
    var newSizeAsNumber = new Number(newSize);
    if(isNaN(newSizeAsNumber)){
        return;
    }
    if (newSizeAsNumber > 0) {
        array.push("&size=" + encodeURIComponent(newSize));
    }
};

cloudStack.addVlanToCommandUrlParameterArrayIfItIsNotNullAndNotEmpty = function(array, vlan){
    cloudStack.addParameterToCommandUrlParameterArrayIfValueIsNotEmpty(array, "vlan", vlan);
};

cloudStack.createArrayOfParametersForCreatePodCommand = function (zoneId, data){
    var array =[];
    array.push("&zoneId=" + zoneId);
    cloudStack.addParameterToCommandUrlParameterArrayIfValueIsNotEmpty(array, "name", data.podName);
    cloudStack.addParameterToCommandUrlParameterArrayIfValueIsNotEmpty(array, "gateway", data.podGateway);
    cloudStack.addParameterToCommandUrlParameterArrayIfValueIsNotEmpty(array, "netmask", data.podNetmask);
    cloudStack.addParameterToCommandUrlParameterArrayIfValueIsNotEmpty(array, "startIp", data.podStartIp);
    cloudStack.addParameterToCommandUrlParameterArrayIfValueIsNotEmpty(array, "endIp", data.podEndIp);
    return array;
}

cloudStack.listDiskOfferings = function(options){
    var defaultOptions = {
            listAll: false,
            isRecursive: false,
            error: function(data) {
                args.response.error(data);
            }
    };
    var mergedOptions = $.extend({}, defaultOptions, options);
    
    var listDiskOfferingsUrl = "listDiskOfferings";
    if(mergedOptions.listAll){
        listDiskOfferingsUrl = listDiskOfferingsUrl + "&listall=true";
    }
    if(mergedOptions.isRecursive){
        listDiskOfferingsUrl = listDiskOfferingsUrl + "&isrecursive=true";
    }
    var diskOfferings = undefined;
    $.ajax({
        url: createURL(listDiskOfferingsUrl),
        data: mergedOptions.data,
        dataType: "json",
        async: false,
        success: function(json) {
            diskOfferings = json.listdiskofferingsresponse.diskoffering;
        },
        error: mergedOptions.error
    });
    return diskOfferings;
};
