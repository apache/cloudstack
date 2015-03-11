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
    cloudStack.installWizard = {
        // Check if install wizard should be invoked
        check: function(args) {
            $.ajax({
                url: createURL('listZones'),
                dataType: 'json',
                async: true,
                success: function(data) {
                    args.response.success({
                        doInstall: !data.listzonesresponse.zone
                    });
                }
            });
        },

        changeUser: function(args) {
            $.ajax({
                url: createURL('updateUser'),
                data: {
                    id: cloudStack.context.users[0].userid,
                    password: md5Hashed ? $.md5(args.data.password) : args.data.password
                },
                type: 'POST',
                dataType: 'json',
                async: true,
                success: function(data) {
                    args.response.success({
                        data: {
                            newUser: data.updateuserresponse.user
                        }
                    });
                }
            });
        },

        // Copy text
        copy: {
            // Tooltips
            'tooltip.addZone.name': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addZone.name'
                });
            },

            'tooltip.addZone.ip4dns1': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addZone.dns1'
                });
            },

            'tooltip.addZone.ip4dns2': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addZone.dns2'
                });
            },

            'tooltip.addZone.internaldns1': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addZone.internaldns1'
                });
            },

            'tooltip.addZone.internaldns2': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addZone.internaldns2'
                });
            },

            'tooltip.configureGuestTraffic.name': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.configureGuestTraffic.name'
                });
            },

            'tooltip.configureGuestTraffic.description': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.configureGuestTraffic.description'
                });
            },

            'tooltip.configureGuestTraffic.guestGateway': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.configureGuestTraffic.guestGateway'
                });
            },

            'tooltip.configureGuestTraffic.guestNetmask': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.configureGuestTraffic.guestNetmask'
                });
            },

            'tooltip.configureGuestTraffic.guestStartIp': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.configureGuestTraffic.guestStartIp'
                });
            },

            'tooltip.configureGuestTraffic.guestEndIp': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.configureGuestTraffic.guestEndIp'
                });
            },

            'tooltip.addPod.name': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addPod.name'
                });
            },

            'tooltip.addPod.reservedSystemGateway': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addPod.reservedSystemGateway'
                });
            },

            'tooltip.addPod.reservedSystemNetmask': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addPod.reservedSystemNetmask'
                });
            },

            'tooltip.addPod.reservedSystemStartIp': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addPod.reservedSystemStartIp'
                });
            },

            'tooltip.addPod.reservedSystemEndIp': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addPod.reservedSystemEndIp'
                });
            },

            'tooltip.addCluster.name': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addCluster.name'
                });
            },

            'tooltip.addHost.hostname': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addHost.hostname'
                });
            },

            'tooltip.addHost.username': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addHost.username'
                });
            },

            'tooltip.addHost.password': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addHost.password'
                });
            },

            'tooltip.addPrimaryStorage.name': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addPrimaryStorage.name'
                });
            },

            'tooltip.addPrimaryStorage.server': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addPrimaryStorage.server'
                });
            },

            'tooltip.addPrimaryStorage.path': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addPrimaryStorage.path'
                });
            },

            'tooltip.addSecondaryStorage.nfsServer': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addSecondaryStorage.nfsServer'
                });
            },

            'tooltip.addSecondaryStorage.path': function(args) {
                args.response.success({
                    text: 'message.installWizard.tooltip.addSecondaryStorage.path'
                });
            },

            // Intro text
            whatIsCloudStack: function(args) {
                args.response.success({
                    text: 'message.installWizard.copy.whatIsCloudStack'
                });
            },

            whatIsAZone: function(args) {
                args.response.success({
                    text: 'message.installWizard.copy.whatIsAZone'
                });
            },

            whatIsAPod: function(args) {
                args.response.success({
                    text: 'message.installWizard.copy.whatIsAPod'
                });
            },

            whatIsACluster: function(args) {
                args.response.success({
                    text: 'message.installWizard.copy.whatIsACluster'
                });
            },

            whatIsAHost: function(args) {
                args.response.success({
                    text: 'message.installWizard.copy.whatIsAHost'
                });
            },

            whatIsPrimaryStorage: function(args) {
                args.response.success({
                    text: 'message.installWizard.copy.whatIsPrimaryStorage'
                });
            },

            whatIsSecondaryStorage: function(args) {
                args.response.success({
                    text: 'message.installWizard.copy.whatIsSecondaryStorage'
                });
            }
        },

        action: function(args) {
            var success = args.response.success;
            var message = args.response.message;

            // Get default network offering
            var selectedNetworkOffering;
            $.ajax({
                url: createURL("listNetworkOfferings&state=Enabled&guestiptype=Shared"),
                dataType: "json",
                async: false,
                success: function(json) {
                    selectedNetworkOffering = $.grep(
                        json.listnetworkofferingsresponse.networkoffering,
                        function(networkOffering) {
                            var services = $.map(networkOffering.service, function(service) {
                                return service.name;
                            });

                            //pick the network offering including SecurityGroup, but excluding Lb and StaticNat. (bug 13665)
                            return (($.inArray('SecurityGroup', services) != -1) && ($.inArray('Lb', services) == -1) && ($.inArray('StaticNat', services) == -1));
                        }
                    )[0];
                }
            });

            cloudStack.zoneWizard.action($.extend(true, {}, args, {
                // Plug in hard-coded values specific to quick install
                data: {
                    zone: {
                        networkType: 'Basic',
                        networkOfferingId: selectedNetworkOffering.id
                    },
                    pluginFrom: {
                        name: 'installWizard',
                        selectedNetworkOffering: selectedNetworkOffering,
                        selectedNetworkOfferingHavingSG: true
                    }
                },
                response: {
                    success: function(args) {
                        var enableZone = function() {
                            message(_l('message.enabling.zone.dots'));
                            cloudStack.zoneWizard.enableZoneAction({
                                data: args.data,
                                formData: args.data,
                                launchData: args.data,
                                response: {
                                    success: function(args) {
                                        pollSystemVMs();
                                    }
                                }
                            });
                        };

                        var pollSystemVMs = function() {
                            // Poll System VMs, then enable zone
                            message(_l('message.creating.systemVM'));
                            var poll = setInterval(function() {
                                $.ajax({
                                    url: createURL('listSystemVms'),
                                    success: function(data) {
                                        var systemVMs = data.listsystemvmsresponse.systemvm;

                                        if (systemVMs && systemVMs.length > 1) {
                                            if (systemVMs.length == $.grep(systemVMs, function(vm) {
                                                return vm.state == 'Running';
                                            }).length) {
                                                clearInterval(poll);
                                                message('message.systems.vms.ready');
                                                setTimeout(pollBuiltinTemplates, 500);
                                            }
                                        }
                                    }
                                });
                            }, 5000);
                        };

                        // Wait for builtin template to be present -- otherwise VMs cannot launch
                        var pollBuiltinTemplates = function() {
                            message('message.waiting.for.builtin.templates.to.load');
                            var poll = setInterval(function() {
                                $.ajax({
                                    url: createURL('listTemplates'),
                                    data: {
                                        templatefilter: 'all'
                                    },
                                    success: function(data) {
                                        var templates = data.listtemplatesresponse.template ?
                                            data.listtemplatesresponse.template : [];
                                        var builtinTemplates = $.grep(templates, function(template) {
                                            return template.templatetype == 'BUILTIN';
                                        });

                                        if (builtinTemplates.length) {
                                            clearInterval(poll);
                                            message('message.your.cloudstack.is.ready');
                                            setTimeout(success, 1000);
                                        }
                                    }
                                });
                            }, 5000);
                        };

                        enableZone();
                    }
                }
            }));
        }
    };
}(jQuery, cloudStack));
