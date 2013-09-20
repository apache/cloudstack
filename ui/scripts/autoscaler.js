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

(function($, cloudstack) {
    var scaleUpData = [];
    var totalScaleUpCondition = 0;
    var scaleDownData = [];
    var totalScaleDownCondition = 0;

    cloudStack.autoscaler = {
        // UI actions to appear in dialog
        autoscaleActions: {
            enable: {
                label: 'Enable Autoscale',
                action: function(args) {
                    $.ajax({
                        url: createURL('enableAutoScaleVmGroup'),
                        data: {
                            id: args.context.originalAutoscaleData.context.autoscaleVmGroup.id
                        },
                        success: function(json) {
                            var jid = json.enableautoscalevmGroupresponse.jobid;
                            args.response.success({
                                _custom: {
                                    jobId: jid,
                                    getUpdatedItem: function(json) {
                                        return json.queryasyncjobresultresponse.jobresult.autoscalevmgroup;
                                    },
                                    getActionFilter: function() {
                                        return cloudStack.autoscaler.actionFilter;
                                    }
                                },
                                notification: {
                                    poll: pollAsyncJobResult
                                }
                            });
                        }
                    });
                }
            },
            disable: {
                label: 'Disable Autoscale',
                action: function(args) {
                    $.ajax({
                        url: createURL('disableAutoScaleVmGroup'),
                        data: {
                            id: args.context.originalAutoscaleData.context.autoscaleVmGroup.id
                        },
                        success: function(json) {
                            var jid = json.disableautoscalevmGroupresponse.jobid;
                            args.response.success({
                                _custom: {
                                    jobId: jid,
                                    getUpdatedItem: function(json) {
                                        return json.queryasyncjobresultresponse.jobresult.autoscalevmgroup;
                                    },
                                    getActionFilter: function() {
                                        return cloudStack.autoscaler.actionFilter;
                                    }
                                },
                                notification: {
                                    poll: pollAsyncJobResult
                                }
                            });
                        }
                    });
                }
            }
        },
        actionFilter: function(args) {
            var allowedActions = [];
            if (args.context.originalAutoscaleData == null) { //new LB rule
                //no actions  for new LB rule
            } else { //existing LB rule
                if (args.context.originalAutoscaleData[0].afterActionIsComplete == null) {
                    if (args.context.originalAutoscaleData[0].context.autoscaleVmGroup.state == 'disabled')
                        allowedActions.push('enable');
                    else if (args.context.originalAutoscaleData[0].context.autoscaleVmGroup.state == 'enabled')
                        allowedActions.push('disable');
                } else {
                    if (args.context.originalAutoscaleData[0].afterActionIsComplete.state == 'disabled')
                        allowedActions.push('enable');
                    else if (args.context.originalAutoscaleData[0].afterActionIsComplete.state == 'enabled')
                        allowedActions.push('disable');
                }
            }
            return allowedActions;
        },
        dataProvider: function(args) {
            // Reset data
            scaleUpData = [];
            totalScaleUpCondition = 0;
            scaleDownData = [];
            totalScaleDownCondition = 0;

            if (!('multiRules' in args.context)) { //from a new LB
                args.response.success({
                    data: null
                });
            } else { //from an existing LB
                $.ajax({
                    url: createURL('listAutoScaleVmGroups'),
                    data: {
                        listAll: true,
                        lbruleid: args.context.multiRules[0].id
                    },
                    success: function(json) {
                        var autoscaleVmGroup = json.listautoscalevmgroupsresponse.autoscalevmgroup[0];

                        $.ajax({
                            url: createURL('listAutoScaleVmProfiles'),
                            data: {
                                listAll: true,
                                id: autoscaleVmGroup.vmprofileid
                            },
                            success: function(json) {
                                var autoscaleVmProfile = json.listautoscalevmprofilesresponse.autoscalevmprofile[0];

                                var scaleUpPolicy = {
                                    id: autoscaleVmGroup.scaleuppolicies[0].id,
                                    duration: autoscaleVmGroup.scaleuppolicies[0].duration,
                                    conditions: []
                                };
                                $(autoscaleVmGroup.scaleuppolicies[0].conditions).each(function() {
                                    var condition = {
                                        id: this.id,
                                        counterid: this.counter[0].id,
                                        relationaloperator: this.relationaloperator,
                                        threshold: this.threshold
                                    };
                                    scaleUpPolicy.conditions.push(condition);
                                });

                                var scaleDownPolicy = {
                                    id: autoscaleVmGroup.scaledownpolicies[0].id,
                                    duration: autoscaleVmGroup.scaledownpolicies[0].duration,
                                    conditions: []
                                };
                                $(autoscaleVmGroup.scaledownpolicies[0].conditions).each(function() {
                                    var condition = {
                                        id: this.id,
                                        counterid: this.counter[0].id,
                                        relationaloperator: this.relationaloperator,
                                        threshold: this.threshold.toString()
                                    };
                                    scaleDownPolicy.conditions.push(condition);
                                });

                                var diskOfferingId, securityGroups;
                                var otherdeployparams = autoscaleVmProfile.otherdeployparams;
                                if (otherdeployparams != null && otherdeployparams.length > 0) {
                                    var array1 = otherdeployparams.split('&');
                                    $(array1).each(function() {
                                        var array2 = this.split('=');
                                        if (array2[0] == 'diskofferingid')
                                            diskOfferingId = array2[1];
                                        if (array2[0] == 'securitygroupids')
                                            securityGroups = array2[1];
                                    });
                                }

                                var originalAutoscaleData = {
                                    templateNames: autoscaleVmProfile.templateid,
                                    serviceOfferingId: autoscaleVmProfile.serviceofferingid,
                                    minInstance: autoscaleVmGroup.minmembers,
                                    maxInstance: autoscaleVmGroup.maxmembers,
                                    scaleUpPolicy: scaleUpPolicy,
                                    scaleDownPolicy: scaleDownPolicy,
                                    interval: autoscaleVmGroup.interval,
                                    quietTime: autoscaleVmGroup.scaleuppolicies[0].quiettime,
                                    destroyVMgracePeriod: autoscaleVmProfile.destroyvmgraceperiod,
                                    securityGroups: securityGroups,
                                    diskOfferingId: diskOfferingId,
                                    snmpCommunity: autoscaleVmProfile.counterparam.snmpcommunity,
                                    snmpPort: autoscaleVmProfile.counterparam.snmpport,
                                    username: autoscaleVmProfile.autoscaleuserid,
                                    context: {
                                        autoscaleVmGroup: autoscaleVmGroup,
                                        autoscaleVmProfile: autoscaleVmProfile
                                    }
                                    //isAdvanced: false // Set this to true if any advanced field data is present
                                };

                                args.response.success({
                                    data: originalAutoscaleData
                                });
                            }
                        });
                    }
                });
            }
        },

        // --
        // Add the following object blocks:
        //
        // topFields: { <standard createForm field format> }
        // bottomFields: { <standard createForm field format> },
        // scaleUpPolicy: { <standard multiEdit field format> },
        // scaleDownPolicy: { <standard multiEdit field format> }
        // --
        //
        forms: {
            topFields: {
                //**
                //** Disabled due to UI issues
                //**
                // templateCategory: {
                //   label: 'Template',
                //   id: 'templatecategory',
                //   select: function(args) {
                //     args.response.success({
                //       data: [
                //         { id: 'all', description: _l('ui.listView.filters.all') },
                //         { id: 'featured', description: _l('label.featured') },
                //         { id: 'Community', description: _l('label.menu.community.templates') },
                //         { id: 'self', description: _l('ui.listView.filters.mine') }
                //       ]
                //     });
                //   }
                // },
                //**

                templateNames: {
                    label: 'label.template',
                    id: 'templatename',
                    select: function(args) {
                        var templates;
                        var templateIdMap = {};
                        $.ajax({
                            url: createURL('listTemplates'),
                            data: {
                                templatefilter: 'featured',
                                zoneid: args.context.networks[0].zoneid
                            },
                            async: false,
                            success: function(json) {
                                templates = json.listtemplatesresponse.template;
                                if (templates == null)
                                    templates = [];
                                $(templates).each(function() {
                                    templateIdMap[this.id] = 1;
                                });
                            }
                        });

                        $.ajax({
                            url: createURL('listTemplates'),
                            data: {
                                templatefilter: 'community',
                                zoneid: args.context.networks[0].zoneid
                            },
                            async: false,
                            success: function(json) {
                                var items = json.listtemplatesresponse.template;
                                $(items).each(function() {
                                    if (!(this.id in templateIdMap)) {
                                        templates.push(this);
                                        templateIdMap[this.id] = 1;
                                    }
                                });
                            }
                        });

                        $.ajax({
                            url: createURL('listTemplates'),
                            data: {
                                templatefilter: 'selfexecutable',
                                zoneid: args.context.networks[0].zoneid
                            },
                            async: false,
                            success: function(json) {
                                var items = json.listtemplatesresponse.template;
                                $(items).each(function() {
                                    if (!(this.id in templateIdMap)) {
                                        templates.push(this);
                                        templateIdMap[this.id] = 1;
                                    }
                                });
                            }
                        });

                        args.response.success({
                            data: $.map(templates, function(template) {
                                return {
                                    id: template.id,
                                    description: template.name
                                };
                            })
                        });
                    }
                },

                serviceOfferingId: {
                    label: 'label.compute.offering',
                    select: function(args) {
                        $.ajax({
                            url: createURL("listServiceOfferings&issystem=false"),
                            dataType: "json",
                            async: true,
                            success: function(json) {
                                var serviceofferings = json.listserviceofferingsresponse.serviceoffering;
                                args.response.success({
                                    data: $.map(serviceofferings, function(serviceoffering) {
                                        return {
                                            id: serviceoffering.id,
                                            description: serviceoffering.name
                                        };
                                    })
                                });
                            }
                        });
                    }
                },

                minInstance: {
                    label: 'Min Instances',
                    defaultValue: '3',
                    validation: {
                        required: true,
                        number: true
                    }
                },

                maxInstance: {
                    label: 'Max Instances',
                    defaultValue: '10',
                    validation: {
                        required: true,
                        number: true
                    }
                }
            },

            bottomFields: {
                isAdvanced: {
                    isBoolean: true,
                    label: 'Show advanced settings'
                },
                interval: {
                    label: 'Polling Interval (in sec)',
                    defaultValue: '30',
                    validation: {
                        required: true,
                        number: true
                    }
                },

                quietTime: {
                    label: 'Quiet Time (in sec)',
                    defaultValue: '300',
                    validation: {
                        required: true,
                        number: true
                    }
                },

                destroyVMgracePeriod: {
                    label: 'Destroy VM Grace Period',
                    defaultValue: '30',
                    isHidden: true,
                    dependsOn: 'isAdvanced',
                    validation: {
                        required: true,
                        number: true
                    }
                },
                securityGroups: {
                    label: 'label.menu.security.groups',
                    isHidden: true,
                    dependsOn: 'isAdvanced',
                    select: function(args) {
                        $.ajax({
                            url: createURL("listSecurityGroups&listAll=true"),
                            dataType: "json",
                            async: true,
                            success: function(json) {
                                var securitygroups = json.listsecuritygroupsresponse.securitygroup;
                                var items = [];
                                items.push({
                                    id: "",
                                    description: ""
                                });
                                $(securitygroups).each(function() {
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

                diskOfferingId: {
                    label: 'label.menu.disk.offerings',
                    isHidden: true,
                    dependsOn: 'isAdvanced',
                    select: function(args) {
                        $.ajax({
                            url: createURL("listDiskOfferings&listAll=true"),
                            dataType: "json",
                            async: true,
                            success: function(json) {
                                var diskofferings = json.listdiskofferingsresponse.diskoffering;
                                var items = [];
                                items.push({
                                    id: "",
                                    description: ""
                                });
                                $(diskofferings).each(function() {
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

                snmpCommunity: {
                    isHidden: true,
                    dependsOn: 'isAdvanced',
                    label: 'SNMP Community',
                    defaultValue: 'public',
                    validation: {
                        required: true
                    }
                },

                snmpPort: {
                    isHidden: true,
                    dependsOn: 'isAdvanced',
                    label: 'SNMP Port',
                    defaultValue: '161',
                    validation: {
                        required: true,
                        number: true
                    }
                },

                username: {
                    isHidden: true,
                    dependsOn: 'isAdvanced',
                    label: 'User',
                    select: function(args) {
                        var items = [];
                        if (args.context.originalAutoscaleData == null) { //new LB rule
                            if (isAdmin() || isDomainAdmin()) {
                                $.ajax({
                                    url: createURL('listUsers'),
                                    data: {
                                        domainid: g_domainid,
                                        account: g_account
                                    },
                                    success: function(json) {
                                        var users = json.listusersresponse.user;
                                        $(users).each(function() {
                                            items.push({
                                                id: this.id,
                                                description: this.username
                                            });
                                        });
                                        args.response.success({
                                            data: items
                                        });
                                    }
                                });
                            } else { //regular user doesn't have access to listUers API call.
                                items.push({
                                    id: "",
                                    description: ""
                                });
                                args.response.success({
                                    data: items
                                });
                            }
                        } else { //existing LB rule
                            if (isAdmin() || isDomainAdmin()) {
                                $.ajax({
                                    url: createURL('listUsers'),
                                    data: {
                                        domainid: args.context.originalAutoscaleData.context.autoscaleVmProfile.domainid,
                                        account: args.context.originalAutoscaleData.context.autoscaleVmProfile.account
                                    },
                                    success: function(json) {
                                        var users = json.listusersresponse.user;
                                        $(users).each(function() {
                                            items.push({
                                                id: this.id,
                                                description: this.username
                                            });
                                        });
                                        args.response.success({
                                            data: items
                                        });
                                    }
                                });
                            } else { //regular user doesn't have access to listUers API call.
                                items.push({
                                    id: "",
                                    description: ""
                                });
                                args.response.success({
                                    data: items
                                });
                            }
                        }
                    }
                }
            },
            scaleUpPolicy: {
                title: 'ScaleUp Policy',
                label: 'SCALE UP POLICY',
                noSelect: true,
                noHeaderActionsColumn: true,
                ignoreEmptyFields: true,
                fields: {
                    'counterid': {
                        label: 'Counter',
                        select: function(args) {
                            $.ajax({
                                url: createURL("listCounters"),
                                dataType: "json",
                                async: false,
                                success: function(json) {
                                    var counters = json.counterresponse.counter;

                                    args.response.success({
                                        data: $.map(counters, function(counter) {
                                            return {
                                                name: counter.id,
                                                description: counter.name
                                            };
                                        })
                                    });
                                }
                            });
                        }
                    },
                    'relationaloperator': {
                        label: 'Operator',
                        select: function(args) {
                            args.response.success({
                                data: [{
                                    name: 'GT',
                                    description: 'greater-than'
                                }, {
                                    name: 'GE',
                                    description: 'greater-than or equals to'
                                }, {
                                    name: 'LT',
                                    description: 'less-than'
                                }, {
                                    name: 'LE',
                                    description: 'less-than or equals to'
                                }, {
                                    name: 'EQ',
                                    description: 'equals-to'
                                }]
                            });
                        }
                    },
                    'threshold': {
                        edit: true,
                        label: 'Threshold'
                    },
                    'add-scaleUpcondition': {
                        label: 'label.add',
                        addButton: true
                    }
                },
                add: {
                    label: 'label.add',
                    action: function(args) {
                        scaleUpData.push($.extend(args.data, {
                            index: totalScaleUpCondition
                        }));

                        totalScaleUpCondition++;
                        args.response.success();
                    }
                },
                actions: {
                    destroy: {
                        label: '',
                        action: function(args) {
                            scaleUpData = $.grep(scaleUpData, function(item) {
                                return item.index != args.context.multiRule[0].index;
                            });
                            totalScaleUpCondition--;
                            args.response.success();
                        }
                    }
                },
                dataProvider: function(args) {
                    var data = scaleUpData;
                    var $autoscaler = $('.ui-dialog .autoscaler');
                    var initialData = $autoscaler.data('autoscaler-scale-up-data');

                    if ($.isArray(initialData)) {
                        $(initialData).each(function() {
                            this.index = totalScaleUpCondition;
                            totalScaleUpCondition++;
                            scaleUpData.push(this);
                        });

                        $autoscaler.data('autoscaler-scale-up-data', null);
                    }

                    args.response.success({
                        data: scaleUpData
                    });
                }
            },

            scaleDownPolicy: {
                title: 'ScaleDown Policy',
                noSelect: true,
                noHeaderActionsColumn: true,
                ignoreEmptyFields: true,
                fields: {
                    'counterid': {
                        label: 'Counter',
                        select: function(args) {
                            $.ajax({
                                url: createURL("listCounters"),
                                dataType: "json",
                                async: false,
                                success: function(json) {
                                    var counters = json.counterresponse.counter;

                                    args.response.success({
                                        data: $.map(counters, function(counter) {
                                            return {
                                                name: counter.id,
                                                description: counter.name
                                            };
                                        })
                                    });
                                }
                            });
                        }
                    },
                    'relationaloperator': {
                        label: 'Operator',
                        select: function(args) {
                            args.response.success({
                                data: [{
                                    name: 'GT',
                                    description: 'greater-than'
                                }, {
                                    name: 'GE',
                                    description: 'greater-than or equals to'
                                }, {
                                    name: 'LT',
                                    description: 'less-than'
                                }, {
                                    name: 'LE',
                                    description: 'less-than or equals to'
                                }, {
                                    name: 'EQ',
                                    description: 'equals-to'
                                }]
                            });
                        }
                    },
                    'threshold': {
                        edit: true,
                        label: 'Threshold'
                    },
                    'add-scaleDowncondition': {
                        label: 'label.add',
                        addButton: true
                    }
                },
                add: {
                    label: 'label.add',
                    action: function(args) {
                        scaleDownData.push($.extend(args.data, {
                            index: totalScaleDownCondition
                        }));
                        totalScaleDownCondition++;
                        args.response.success();
                    }
                },
                actions: {
                    destroy: {
                        label: '',
                        action: function(args) {
                            scaleDownData = $.grep(scaleDownData, function(item) {
                                return item.index != args.context.multiRule[0].index;
                            });
                            totalScaleDownCondition--;
                            args.response.success();
                        }
                    }
                },
                dataProvider: function(args) {
                    var data = scaleDownData;
                    var $autoscaler = $('.ui-dialog .autoscaler');
                    var initialData = $autoscaler.data('autoscaler-scale-down-data');

                    if ($.isArray(initialData)) {
                        $(initialData).each(function() {
                            this.index = totalScaleDownCondition;
                            totalScaleDownCondition++;
                            scaleDownData.push(this);
                        });

                        $autoscaler.data('autoscaler-scale-down-data', null);
                    }

                    args.response.success({
                        data: scaleDownData
                    });
                }
            }
        },

        actions: {
            apply: function(args) {
                //validation (begin) *****
                if (!('multiRules' in args.context)) { //from a new LB
                    if (args.formData.name == '' || args.formData.publicport == '' || args.formData.privateport == '') {
                        args.response.error('Name, Public Port, Private Port of Load Balancing are required. Please close this dialog box and fill Name, Public Port, Private Port first.');
                        return;
                    }
                } else { //from an existing LB
                    if (args.context.originalAutoscaleData.afterActionIsComplete == null) {
                        if (args.context.originalAutoscaleData.context.autoscaleVmGroup.state != 'disabled') {
                            args.response.error('An Autoscale VM Group can be updated only if it is in disabled state. Please disable the Autoscale VM Group first.');
                            return;
                        }
                    } else {
                        if (args.context.originalAutoscaleData.afterActionIsComplete.state != 'disabled') {
                            args.response.error('An Autoscale VM Group can be updated only if it is in disabled state. Please disable the Autoscale VM Group first.');
                            return;
                        }
                    }
                }

                if (isAdmin() || isDomainAdmin()) { //only admin and domain-admin has access to listUers API
                    var havingApiKeyAndSecretKey = false;
                    $.ajax({
                        url: createURL('listUsers'),
                        data: {
                            id: args.data.username
                        },
                        async: false,
                        success: function(json) {
                            if (json.listusersresponse.user[0].apikey != null && json.listusersresponse.user[0].secretkey != null) {
                                havingApiKeyAndSecretKey = true;
                            }
                        }
                    });
                    if (havingApiKeyAndSecretKey == false) {
                        args.response.error('The selected user in advanced settings does not have API key or secret key');
                        return;
                    }
                }

                if (isAdmin()) { //only admin has access to listConfigurations API
                    var hasValidEndpointeUrl = false;
                    $.ajax({
                        url: createURL('listConfigurations'),
                        data: {
                            name: 'endpointe.url'
                        },
                        async: false,
                        success: function(json) {
                            if (json.listconfigurationsresponse.configuration != null) {
                                if (json.listconfigurationsresponse.configuration[0].value.indexOf('localhost') == -1) {
                                    hasValidEndpointeUrl = true;
                                }
                            }
                        }
                    });
                    if (hasValidEndpointeUrl == false) {
                        args.response.error("Global setting endpointe.url has to be set to the Management Server's API end point");
                        return;
                    }
                }

                //Scale Up Policy
                if (args.data.scaleUpDuration == null || args.data.scaleUpDuration.length == 0) {
                    args.response.error("Duration of Scale Up Policy is required.");
                    return;
                }
                if (isNaN(args.data.scaleUpDuration)) {
                    args.response.error("Duration of Scale Up Policy should be a number.");
                    return;
                }
                if (parseInt(args.data.scaleUpDuration) < parseInt(args.data.interval)) {
                    args.response.error("Duration of Scale Up Policy must be greater than or equal to Polling Interval.");
                    return;
                }
                if (scaleUpData.length == 0) {
                    args.response.error("At least one condition is required in Scale Up Policy.");
                    return;
                }

                //Scale Down Policy
                if (args.data.scaleDownDuration == null || args.data.scaleDownDuration.length == 0) {
                    args.response.error("Duration of Scale Down Policy is required.");
                    return;
                }
                if (isNaN(args.data.scaleDownDuration)) {
                    args.response.error("Duration of Scale Down Policy should be a number.");
                    return;
                }
                if (parseInt(args.data.scaleDownDuration) < parseInt(args.data.interval)) {
                    args.response.error("Duration of Scale Down Policy must be greater than or equal to Polling Interval.");
                    return;
                }
                if (scaleDownData.length == 0) {
                    args.response.error("At least one condition is required in Scale Down Policy.");
                    return;
                }
                //validation (end) *****

                var scaleVmProfileResponse = [];
                var loadBalancerResponse = [];
                var scaleVmGroupResponse = [];
                var scaleUpConditionIds = [];
                var scaleDownConditionIds = [];

                var scaleUp = function(args) {
                    var scaleUpConditionIds = [];
                    $(scaleUpData).each(function() {
                        var data = {
                            counterid: this.counterid,
                            relationaloperator: this.relationaloperator,
                            threshold: this.threshold
                        };
                        $.ajax({
                            url: createURL('createCondition'),
                            data: data,
                            success: function(json) {
                                var createConditionIntervalID = setInterval(function() {
                                    $.ajax({
                                        url: createURL("queryAsyncJobResult&jobid=" + json.conditionresponse.jobid),
                                        dataType: "json",
                                        success: function(json) {
                                            var result = json.queryasyncjobresultresponse;
                                            if (result.jobstatus == 0) {
                                                return;
                                            } else {
                                                clearInterval(createConditionIntervalID);
                                                if (result.jobstatus == 1) {
                                                    var item = json.queryasyncjobresultresponse.jobresult.condition;
                                                    scaleUpConditionIds.push(item.id);
                                                    if (scaleUpConditionIds.length == scaleUpData.length) {
                                                        if (!('multiRules' in args.context)) { //from a new LB
                                                            var data = {
                                                                action: 'scaleup',
                                                                conditionids: scaleUpConditionIds.join(","),
                                                                duration: args.data.scaleUpDuration,
                                                                quiettime: args.data.quietTime
                                                            };
                                                            $.ajax({
                                                                url: createURL('createAutoScalePolicy'),
                                                                data: data,
                                                                success: function(json) {
                                                                    var jobId = json.autoscalepolicyresponse.jobid;
                                                                    var createAutoScalePolicyInterval = setInterval(function() {
                                                                        $.ajax({
                                                                            url: createURL("queryAsyncJobResult&jobId=" + jobId),
                                                                            dataType: "json",
                                                                            success: function(json) {
                                                                                var result = json.queryasyncjobresultresponse;
                                                                                if (result.jobstatus == 0) {
                                                                                    return; //Job has not completed
                                                                                } else {
                                                                                    clearInterval(createAutoScalePolicyInterval);
                                                                                    if (result.jobstatus == 1) { //AutoScalePolicy successfully created
                                                                                        var item = result.jobresult.autoscalepolicy;
                                                                                        scaleDown($.extend(args, {
                                                                                            scaleUpPolicyResponse: item
                                                                                        }));
                                                                                    } else if (result.jobstatus == 2) {
                                                                                        args.response.error(_s(result.jobresult.errortext));
                                                                                    }
                                                                                }
                                                                            }
                                                                        });
                                                                    }, g_queryAsyncJobResultInterval);
                                                                },
                                                                error: function(XMLHttpResponse) {
                                                                    args.response.error(parseXMLHttpResponse(XMLHttpResponse));
                                                                }
                                                            });
                                                        } else { //from an existing LB
                                                            var data = {
                                                                id: args.context.originalAutoscaleData.scaleUpPolicy.id,
                                                                conditionids: scaleUpConditionIds.join(","),
                                                                duration: args.data.scaleUpDuration,
                                                                quiettime: args.data.quietTime
                                                            };

                                                            $.ajax({
                                                                url: createURL('updateAutoScalePolicy'),
                                                                data: data,
                                                                success: function(json) {
                                                                    var updateAutoScalePolicyInterval = setInterval(function() {
                                                                        $.ajax({
                                                                            url: createURL("queryAsyncJobResult&jobId=" + json.updateautoscalepolicyresponse.jobid),
                                                                            dataType: "json",
                                                                            success: function(json) {
                                                                                var result = json.queryasyncjobresultresponse;
                                                                                if (result.jobstatus == 0) {
                                                                                    return; //Job has not completed
                                                                                } else {
                                                                                    clearInterval(updateAutoScalePolicyInterval);
                                                                                    if (result.jobstatus == 1) {
                                                                                        var item = result.jobresult.autoscalepolicy;

                                                                                        //delete old conditions which are orphans now. Don't need to call queryAsyncJobResult because subsequent API calls have no dependency on deleteCondition.
                                                                                        $(args.context.originalAutoscaleData.scaleUpPolicy.conditions).each(function() {
                                                                                            $.ajax({
                                                                                                url: createURL('deleteCondition'),
                                                                                                data: {
                                                                                                    id: this.id
                                                                                                }
                                                                                            });
                                                                                        });

                                                                                        scaleDown($.extend(args, {
                                                                                            scaleUpPolicyResponse: item
                                                                                        }));
                                                                                    } else if (result.jobstatus == 2) {
                                                                                        args.response.error(_s(result.jobresult.errortext));
                                                                                    }
                                                                                }
                                                                            }
                                                                        });
                                                                    }, g_queryAsyncJobResultInterval);
                                                                },
                                                                error: function(XMLHttpResponse) {
                                                                    args.response.error(parseXMLHttpResponse(XMLHttpResponse));
                                                                }
                                                            });
                                                        }
                                                    }
                                                } else if (result.jobstatus == 2) {
                                                    args.response.error(_s(result.jobresult.errortext));
                                                }
                                            }
                                        }
                                    });
                                }, g_queryAsyncJobResultInterval);
                            },
                            error: function(XMLHttpResponse) {
                                args.response.error(parseXMLHttpResponse(XMLHttpResponse));
                            }
                        });
                    });
                };

                var scaleDown = function(args) {
                    var scaleDownConditionIds = [];
                    $(scaleDownData).each(function() {
                        var data = {
                            counterid: this.counterid,
                            relationaloperator: this.relationaloperator,
                            threshold: this.threshold
                        };
                        $.ajax({
                            url: createURL('createCondition'),
                            data: data,
                            success: function(json) {
                                var createConditionIntervalID = setInterval(function() {
                                    $.ajax({
                                        url: createURL("queryAsyncJobResult&jobid=" + json.conditionresponse.jobid),
                                        dataType: "json",
                                        success: function(json) {
                                            var result = json.queryasyncjobresultresponse;
                                            if (result.jobstatus == 0) {
                                                return;
                                            } else {
                                                clearInterval(createConditionIntervalID);
                                                if (result.jobstatus == 1) {
                                                    var item = json.queryasyncjobresultresponse.jobresult.condition;
                                                    scaleDownConditionIds.push(item.id);
                                                    if (scaleDownConditionIds.length == scaleDownData.length) {
                                                        if (!('multiRules' in args.context)) { //from a new LB
                                                            var data = {
                                                                action: 'scaledown',
                                                                conditionids: scaleDownConditionIds.join(","),
                                                                duration: args.data.scaleDownDuration,
                                                                quiettime: args.data.quietTime
                                                            };
                                                            $.ajax({
                                                                url: createURL('createAutoScalePolicy'),
                                                                data: data,
                                                                success: function(json) {
                                                                    var jobId = json.autoscalepolicyresponse.jobid;
                                                                    var createAutoScalePolicyInterval = setInterval(function() {
                                                                        $.ajax({
                                                                            url: createURL("queryAsyncJobResult&jobId=" + jobId),
                                                                            dataType: "json",
                                                                            success: function(json) {
                                                                                var result = json.queryasyncjobresultresponse;
                                                                                if (result.jobstatus == 0) {
                                                                                    return; //Job has not completed
                                                                                } else {
                                                                                    clearInterval(createAutoScalePolicyInterval);
                                                                                    if (result.jobstatus == 1) { //AutoScalePolicy successfully created
                                                                                        var item = result.jobresult.autoscalepolicy;
                                                                                        createOrUpdateVmProfile($.extend(args, {
                                                                                            scaleDownPolicyResponse: item
                                                                                        }));
                                                                                    } else if (result.jobstatus == 2) {
                                                                                        args.response.error(_s(result.jobresult.errortext));
                                                                                    }
                                                                                }
                                                                            }
                                                                        });
                                                                    }, g_queryAsyncJobResultInterval);
                                                                },
                                                                error: function(XMLHttpResponse) {
                                                                    args.response.error(parseXMLHttpResponse(XMLHttpResponse));
                                                                }
                                                            });
                                                        } else { //from an existing LB
                                                            var data = {
                                                                id: args.context.originalAutoscaleData.scaleDownPolicy.id,
                                                                conditionids: scaleDownConditionIds.join(","),
                                                                duration: args.data.scaleDownDuration,
                                                                quiettime: args.data.quietTime
                                                            };

                                                            $.ajax({
                                                                url: createURL('updateAutoScalePolicy'),
                                                                data: data,
                                                                success: function(json) {
                                                                    var jobId = json.updateautoscalepolicyresponse.jobid;
                                                                    var updateAutoScalePolicyInterval = setInterval(function() {
                                                                        $.ajax({
                                                                            url: createURL("queryAsyncJobResult&jobId=" + jobId),
                                                                            dataType: "json",
                                                                            success: function(json) {
                                                                                var result = json.queryasyncjobresultresponse;
                                                                                if (result.jobstatus == 0) {
                                                                                    return; //Job has not completed
                                                                                } else {
                                                                                    clearInterval(updateAutoScalePolicyInterval);
                                                                                    if (result.jobstatus == 1) {
                                                                                        var item = result.jobresult.autoscalepolicy;

                                                                                        //delete old conditions which are orphans now. Don't need to call queryAsyncJobResult because subsequent API calls have no dependency on deleteCondition.
                                                                                        $(args.context.originalAutoscaleData.scaleDownPolicy.conditions).each(function() {
                                                                                            $.ajax({
                                                                                                url: createURL('deleteCondition'),
                                                                                                data: {
                                                                                                    id: this.id
                                                                                                }
                                                                                            });
                                                                                        });

                                                                                        createOrUpdateVmProfile($.extend(args, {
                                                                                            scaleDownPolicyResponse: item
                                                                                        }));
                                                                                    } else if (result.jobstatus == 2) {
                                                                                        args.response.error(_s(result.jobresult.errortext));
                                                                                    }
                                                                                }
                                                                            }
                                                                        });
                                                                    }, g_queryAsyncJobResultInterval);
                                                                },
                                                                error: function(XMLHttpResponse) {
                                                                    args.response.error(parseXMLHttpResponse(XMLHttpResponse));
                                                                }
                                                            });
                                                        }
                                                    }
                                                } else if (result.jobstatus == 2) {
                                                    args.response.error(_s(result.jobresult.errortext));
                                                }
                                            }
                                        },
                                        error: function(XMLHttpResponse) {
                                            args.response.error(parseXMLHttpResponse(XMLHttpResponse));
                                        }
                                    });
                                }, g_queryAsyncJobResultInterval);
                            }
                        });
                    });
                };

                var createOrUpdateVmProfile = function(args) {
                    var array1 = [];
                    var apiCmd, apiCmdRes;
                    if (!('multiRules' in args.context)) { //from a new LB
                        var data = {
                            zoneid: args.context.networks[0].zoneid, //get zoneid from args.context.networks[0] instead of args.context.ipAddresses[0] because args.context.ipAddresses is null when adding AutoScale rule from Add Load Balancer tab in Network page
                            serviceofferingid: args.data.serviceOfferingId,
                            templateid: args.data.templateNames,
                            destroyvmgraceperiod: args.data.destroyVMgracePeriod,
                            snmpcommunity: args.data.snmpCommunity,
                            snmpport: args.data.snmpPort
                        };

                        var allParamNames = $.map(data, function(value, key) {
                            return key;
                        });

                        var notParams = ['zoneid', 'serviceofferingid', 'templateid', 'destroyvmgraceperiod'];
                        var index = 0;
                        $(allParamNames).each(function() {
                            var param = 'counterparam[' + index + ']';
                            var name = this.toString();
                            var value = data[name];
                            if (!value || $.inArray(name, notParams) > -1) return true;
                            data[param + '.name'] = name;
                            data[param + '.value'] = value;
                            index++;
                            delete data[name];

                            return true;
                        });


                        if (args.data.username != null && args.data.username.length > 0) {
                            $.extend(data, {
                                autoscaleuserid: args.data.username
                            });
                        }

                        var array2 = [];
                        if (args.data.diskOfferingId != null && args.data.diskOfferingId.length > 0)
                            array2.push("diskofferingid=" + args.data.diskOfferingId);
                        if (args.data.securityGroups != null && args.data.securityGroups.length > 0) {
                            if (array2.length > 0)
                                array2.push("&securitygroupids=" + args.data.securityGroups);
                            else
                                array2.push("securitygroupids=" + args.data.securityGroups);
                        }
                        if (array2.length > 0) {
                            $.extend(data, {
                                otherdeployparams: array2.join("")
                            });
                        }

                        $.ajax({
                            url: createURL('createAutoScaleVmProfile'),
                            data: data,
                            success: function(json) {
                                var jobId = json.autoscalevmprofileresponse.jobid;
                                var autoscaleVmProfileTimer = setInterval(function() {
                                    $.ajax({
                                        url: createURL("queryAsyncJobResult&jobId=" + jobId),
                                        dataType: "json",
                                        success: function(json) {
                                            var result = json.queryasyncjobresultresponse;
                                            if (result.jobstatus == 0) {
                                                return; //Job has not completed
                                            } else {
                                                clearInterval(autoscaleVmProfileTimer);
                                                if (result.jobstatus == 1) {
                                                    scaleVmProfileResponse = result.jobresult.autoscalevmprofile;
                                                    loadBalancer(args); //create a load balancer rule
                                                } else if (result.jobstatus == 2) {
                                                    args.response.error(_s(result.jobresult.errortext));
                                                }
                                            }
                                        }
                                    });
                                }, g_queryAsyncJobResultInterval);
                            },
                            error: function(XMLHttpResponse) {
                                args.response.error(parseXMLHttpResponse(XMLHttpResponse));
                            }
                        });
                    } else { //from an existing LB
                        var data = {
                            id: args.context.originalAutoscaleData.context.autoscaleVmProfile.id,
                            templateid: args.data.templateNames,
                            destroyvmgraceperiod: args.data.destroyVMgracePeriod,
                            snmpcommunity: args.data.snmpCommunity,
                            snmpport: args.data.snmpPort
                        };

                        var allParamNames = $.map(data, function(value, key) {
                            return key;
                        });

                        var notParams = ['id', 'templateid', 'destroyvmgraceperiod'];
                        var index = 0;
                        $(allParamNames).each(function() {
                            var param = 'counterparam[' + index + ']';
                            var name = this.toString();
                            var value = data[name];
                            if (!value || $.inArray(name, notParams) > -1) return true;
                            data[param + '.name'] = name;
                            data[param + '.value'] = value;
                            index++;
                            delete data[name];

                            return true;
                        });




                        if (args.data.username != null && args.data.username.length > 0) {
                            $.extend(data, {
                                autoscaleuserid: args.data.username
                            });
                        }

                        $.ajax({
                            url: createURL('updateAutoScaleVmProfile'),
                            data: data,
                            success: function(json) {
                                var jobId = json.updateautoscalevmprofileresponse.jobid;
                                var autoscaleVmProfileTimer = setInterval(function() {
                                    $.ajax({
                                        url: createURL("queryAsyncJobResult&jobId=" + jobId),
                                        dataType: "json",
                                        success: function(json) {
                                            var result = json.queryasyncjobresultresponse;
                                            if (result.jobstatus == 0) {
                                                return; //Job has not completed
                                            } else {
                                                clearInterval(autoscaleVmProfileTimer);
                                                if (result.jobstatus == 1) {
                                                    scaleVmProfileResponse = result.jobresult.autoscalevmprofile;
                                                    autoScaleVmGroup(args); //update autoScaleVmGroup
                                                } else if (result.jobstatus == 2) {
                                                    args.response.error(_s(result.jobresult.errortext));
                                                }
                                            }
                                        }
                                    });
                                }, g_queryAsyncJobResultInterval);
                            },
                            error: function(XMLHttpResponse) {
                                args.response.error(parseXMLHttpResponse(XMLHttpResponse));
                            }
                        });
                    }
                };

                var loadBalancer = function(args) {
                    var networkid;
                    if ('vpc' in args.context) { //from VPC section
                        if (args.data.tier == null) {
                            cloudStack.dialog.notice({
                                message: 'Tier is required'
                            });
                            return;
                        }
                        networkid = args.data.tier;
                    } else if ('networks' in args.context) { //from Guest Network section
                        networkid = args.context.networks[0].id;
                    }
                    var data = {
                        algorithm: args.formData.algorithm,
                        name: args.formData.name,
                        privateport: args.formData.privateport,
                        publicport: args.formData.publicport,
                        openfirewall: false,
                        networkid: networkid
                    };
                    if (args.context.ipAddresses != null) {
                        data = $.extend(data, {
                            publicipid: args.context.ipAddresses[0].id
                        });
                    } else {
                        data = $.extend(data, {
                            domainid: g_domainid,
                            account: g_account
                        });
                    }

                    $.ajax({
                        url: createURL('createLoadBalancerRule'),
                        dataType: 'json',
                        data: data,
                        async: true,
                        success: function(json) {
                            var jobId = json.createloadbalancerruleresponse.jobid;
                            var loadBalancerTimer = setInterval(function() {
                                $.ajax({
                                    url: createURL("queryAsyncJobResult&jobId=" + jobId),
                                    dataType: "json",
                                    success: function(json) {
                                        var result = json.queryasyncjobresultresponse;
                                        if (result.jobstatus == 0) {
                                            return; //Job has not completed
                                        } else {
                                            clearInterval(loadBalancerTimer);
                                            if (result.jobstatus == 1) { //LoadBalancerRule successfully created
                                                loadBalancerResponse = result.jobresult.loadbalancer;
                                                autoScaleVmGroup(args);
                                            } else if (result.jobstatus == 2) {
                                                args.response.error(_s(result.jobresult.errortext));
                                            }
                                        }
                                    }
                                });
                            }, g_queryAsyncJobResultInterval);
                        },
                        error: function(XMLHttpResponse) {
                            args.response.error(parseXMLHttpResponse(XMLHttpResponse));
                        }
                    });
                };

                var autoScaleVmGroup = function(args) {
                    if (!('multiRules' in args.context)) { //from a new LB
                        var array1 = [];
                        array1.push("&lbruleid=" + loadBalancerResponse.id);
                        array1.push("&minMembers=" + args.data.minInstance);
                        array1.push("&maxMembers=" + args.data.maxInstance);
                        array1.push("&vmprofileid=" + scaleVmProfileResponse.id);
                        array1.push("&interval=" + args.data.interval);
                        array1.push("&scaleuppolicyids=" + args.scaleUpPolicyResponse.id);
                        array1.push("&scaledownpolicyids=" + args.scaleDownPolicyResponse.id);
                        
                        $.ajax({
                            url: createURL('createAutoScaleVmGroup' + array1.join("")),
                            dataType: 'json',
                            async: true,
                            success: function(json) {
                                var jobId = json.autoscalevmgroupresponse.jobid;
                                var scaleVmGroupTimer = setInterval(function() {
                                    $.ajax({
                                        url: createURL("queryAsyncJobResult&jobId=" + jobId),
                                        dataType: "json",
                                        success: function(json) {
                                            var result = json.queryasyncjobresultresponse;
                                            if (result.jobstatus == 0) {
                                                return; //Job has not completed
                                            } else {
                                                clearInterval(scaleVmGroupTimer);
                                                if (result.jobstatus == 1) { //autoscale Vm group successfully created
                                                    scaleVmGroupResponse = result.jobresult.autoscalevmgroup;
                                                    args.response.success();
                                                } else if (result.jobstatus == 2) {
                                                    args.response.error(_s(result.jobresult.errortext));
                                                }
                                            }
                                        }
                                    });
                                }, g_queryAsyncJobResultInterval);
                            },
                            error: function(XMLHttpResponse) {
                                args.response.error(parseXMLHttpResponse(XMLHttpResponse));
                            }
                        });
                    } else { //from an existing LB
                        var data = {
                            id: args.context.originalAutoscaleData.context.autoscaleVmGroup.id,
                            minmembers: args.data.minInstance,
                            maxmembers: args.data.maxInstance,
                            interval: args.data.interval,
                            scaleuppolicyids: args.context.originalAutoscaleData.scaleUpPolicy.id,
                            scaledownpolicyids: args.context.originalAutoscaleData.scaleDownPolicy.id
                        };

                        $.ajax({
                            url: createURL('updateAutoScaleVmGroup'),
                            data: data,
                            success: function(json) {
                                var jobId = json.updateautoscalevmgroupresponse.jobid;
                                var updateAutoScaleVmGroupTimer = setInterval(function() {
                                    $.ajax({
                                        url: createURL("queryAsyncJobResult&jobId=" + jobId),
                                        dataType: "json",
                                        success: function(json) {
                                            var result = json.queryasyncjobresultresponse;
                                            if (result.jobstatus == 0) {
                                                return; //Job has not completed
                                            } else {
                                                clearInterval(updateAutoScaleVmGroupTimer);
                                                if (result.jobstatus == 1) { //autoscale Vm group successfully created
                                                    args.response.success();
                                                } else if (result.jobstatus == 2) {
                                                    args.response.error(_s(result.jobresult.errortext));
                                                }
                                            }
                                        }
                                    });
                                }, g_queryAsyncJobResultInterval);
                            },
                            error: function(XMLHttpResponse) {
                                args.response.error(parseXMLHttpResponse(XMLHttpResponse));
                            }
                        });
                    }
                };

                // Get hypervisor;
                // if VMware, show notification to user about additional configuration required
                $.ajax({
                    url: createURL('listTemplates'),
                    data: {
                        id: args.data.templateNames,
                        templatefilter: 'executable'
                    },
                    async: false,
                    success: function(json) {
                        var template = json.listtemplatesresponse.template;

                        if (template && template[0].hypervisor === 'VMware') {
                            cloudStack.dialog.confirm({
                                message: 'For VMware-based VMs, please read the dynamic scaling section in the admin guide before scaling. Would you like to continue?,',
                                action: function() {
                                    //*** API calls start!!! ********
                                    scaleUp(args);
                                },
                                cancelAction: function() {
                                    $('.loading-overlay').remove();
                                }
                            });
                        } else {
                            //*** API calls start!!! ********
                            scaleUp(args);
                        }
                    }
                });

            },
            destroy: function(args) {
                $.ajax({
                    url: createURL('')
                });
            }
        },

        dialog: function(args) {
            return function(args) {
                var context = args.context;

                var $dialog = $('<div>');
                $dialog.dialog({
                    title: 'AutoScale Configuration Wizard',
                    closeonEscape: false,

                    draggable: true,
                    width: 825,
                    height: 600,
                    buttons: {
                        'Cancel': function() {
                            $(this).dialog("close");
                            $('.overlay').remove();
                        },


                        'Apply': function() {
                            $(':ui-dialog').remove();
                            $('.overlay').remove();
                        }
                    }
                }).closest('.ui-dialog').overlay();

                $("buttons").each(function() {
                    $(this).attr('style', 'float: right');
                });
                var $field = $('<div>').addClass('field username');
                var $input = $('<input>').attr({
                    name: 'username'
                });
                var $inputLabel = $('<label>').html('Username');

                $field.append($input, $inputLabel);
                $field.appendTo($dialog);
            }
        }
    }
}(jQuery, cloudStack));
