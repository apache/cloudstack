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
(function ($, cloudStack) {
    cloudStack.modules.vnmcNetworkProvider = function (module) {
        var vnmcDeviceViewAll = window._m = [{
            label: 'VNMC Devices',
            path: '_zone.vnmcDevices'
        }];

        var vnmcListView = {
            id: 'vnmcDevices',
            fields: {
                resourcename: {
                    label: 'Resource Name'
                },
                provider: {
                    label: 'Provider'
                }
            },
            dataProvider: function (args) {
                $.ajax({
                    url: createURL('listCiscoVnmcResources'),
                    data: {
                        physicalnetworkid: args.context.physicalNetworks[0].id
                    },
                    success: function (json) {
                        var items = json.listCiscoVnmcResources.CiscoVnmcResource;
                        args.response.success({
                            data: items
                        });
                    }
                });
            },
            actions: {
                add: {
                    label: 'Add VNMC device',

                    messages: {
                        notification: function (args) {
                            return 'Add VNMC device';
                        }
                    },

                    createForm: {
                        title: 'Add VNMC device',
                        fields: {
                            hostname: {
                                label: 'label.host',
                                validation: {
                                    required: true
                                }
                            },
                            username: {
                                label: 'label.username',
                                validation: {
                                    required: true
                                }
                            },
                            password: {
                                label: 'label.password',
                                isPassword: true,
                                validation: {
                                    required: true
                                }
                            }
                        }
                    },

                    action: function (args) {
                        $.ajax({
                            url: createURL('listNetworkServiceProviders'),
                            data: {
                                name: 'CiscoVnmc',
                                physicalnetworkid: args.context.physicalNetworks[0].id
                            },
                            success: function (json) {
                                var items = json.listnetworkserviceprovidersresponse.networkserviceprovider;
                                if (items != null && items.length > 0) {
                                    var ciscoVnmcProvider = items[0];
                                    if (ciscoVnmcProvider.state == 'Enabled') {
                                        addCiscoVnmcResourceFn();
                                    } else {
                                        enableCiscoVnmcProviderFn(ciscoVnmcProvider);
                                    }
                                } else {
                                    $.ajax({
                                        url: createURL("addNetworkServiceProvider"),
                                        data: {
                                            name: 'CiscoVnmc',
                                            physicalnetworkid: args.context.physicalNetworks[0].id
                                        },
                                        success: function (json) {
                                            var jobId = json.addnetworkserviceproviderresponse.jobid;
                                            var addVnmcProviderIntervalID = setInterval(function () {
                                                $.ajax({
                                                    url: createURL("queryAsyncJobResult&jobId=" + jobId),
                                                    dataType: "json",
                                                    success: function (json) {
                                                        var result = json.queryasyncjobresultresponse;
                                                        if (result.jobstatus == 0) {
                                                            return; //Job has not completed
                                                        } else {
                                                            clearInterval(addVnmcProviderIntervalID);
                                                            if (result.jobstatus == 1) {
                                                                //nspMap["CiscoVnmc"] = json.queryasyncjobresultresponse.jobresult.networkserviceprovider;
                                                                var ciscoVnmcProvider = json.queryasyncjobresultresponse.jobresult.networkserviceprovider;
                                                                enableCiscoVnmcProviderFn(ciscoVnmcProvider);
                                                            } else if (result.jobstatus == 2) {
                                                                args.response.error(_s(result.jobresult.errortext));
                                                            }
                                                        }
                                                    },
                                                    error: function (XMLHttpResponse) {
                                                        args.response.error(parseXMLHttpResponse(data));
                                                    }
                                                });
                                            }, g_queryAsyncJobResultInterval);
                                        }
                                    });
                                }
                            }
                        });

                        var enableCiscoVnmcProviderFn = function (ciscoVnmcProvider) {
                            $.ajax({
                                url: createURL('updateNetworkServiceProvider'),
                                data: {
                                    id: ciscoVnmcProvider.id,
                                    state: 'Enabled'
                                },
                                success: function (json) {
                                    var jid = json.updatenetworkserviceproviderresponse.jobid;
                                    var enableVnmcProviderIntervalID = setInterval(function () {
                                        $.ajax({
                                            url: createURL('queryAsyncJobResult'),
                                            data: {
                                                jobid: jid
                                            },
                                            success: function (json) {
                                                var result = json.queryasyncjobresultresponse;
                                                if (result.jobstatus == 0) {
                                                    return; //Job has not completed
                                                } else {
                                                    clearInterval(enableVnmcProviderIntervalID);
                                                    if (result.jobstatus == 1) {
                                                        addCiscoVnmcResourceFn();
                                                    } else if (result.jobstatus == 2) {
                                                        args.response.error(_s(result.jobresult.errortext));
                                                    }
                                                }
                                            }
                                        });
                                    }, g_queryAsyncJobResultInterval);
                                }
                            });
                        };

                        var addCiscoVnmcResourceFn = function () {
                            var data = {
                                physicalnetworkid: args.context.physicalNetworks[0].id,
                                hostname: args.data.hostname,
                                username: args.data.username,
                                password: args.data.password
                            };

                            $.ajax({
                                url: createURL('addCiscoVnmcResource'),
                                type: "POST",
                                data: data,
                                success: function (json) {
                                    var item = json.addCiscoVnmcResource.CiscoVnmcResource;
                                    args.response.success({
                                        data: item
                                    });
                                },
                                error: function (data) {
                                    args.response.error(parseXMLHttpResponse(data));
                                }
                            });
                        }
                    },

                    notification: {
                        poll: function (args) {
                            args.complete();
                        }
                    }
                }
            },

            detailView: {
                name: 'CiscoVNMC resource details',
                actions: {
                    remove: {
                        label: 'delete CiscoVNMC resource',
                        messages: {
                            confirm: function (args) {
                                return 'Please confirm you want to delete CiscoVNMC resource';
                            },
                            notification: function (args) {
                                return 'delete CiscoVNMC resource';
                            }
                        },
                        action: function (args) {
                            $.ajax({
                                url: createURL('deleteCiscoVnmcResource'),
                                data: {
                                    resourceid: args.context.vnmcDevices[0].resourceid
                                },
                                success: function (json) {
                                    args.response.success();
                                },
                                error: function (data) {
                                    args.response.error(parseXMLHttpResponse(data));
                                }
                            });
                        },
                        notification: {
                            poll: function (args) {
                                args.complete();
                            }
                        }
                    }
                },

                tabs: {
                    details: {
                        title: 'label.details',
                        fields: [{
                            resourcename: {
                                label: 'Resource Name'
                            }
                        }, {
                            resourceid: {
                                label: 'Resource ID'
                            },
                            provider: {
                                label: 'Provider'
                            },
                            RESOURCE_NAME: {
                                label: 'Resource Name'
                            }
                        }],
                        dataProvider: function (args) {
                            $.ajax({
                                url: createURL('listCiscoVnmcResources'),
                                data: {
                                    resourceid: args.context.vnmcDevices[0].resourceid
                                },
                                success: function (json) {
                                    var item = json.listCiscoVnmcResources.CiscoVnmcResource[0];
                                    args.response.success({
                                        data: item
                                    });
                                }
                            });
                        }
                    }
                }
            }
        };

        var vnmcProviderDetailView = {
            id: 'vnmcProvider',
            label: 'VNMC',
            viewAll: vnmcDeviceViewAll,
            actions: {
            	add: {
            		label: 'Add VNMC provider',

                    messages: {
                        confirm: function (args) {
                            return 'Please confirm you would like to add the VNMC provider.';
                        },
                        notification: function (args) {
                            return 'Add VNMC device';
                        }
                    },

                    action: function (args) {      
                    	$.ajax({
                            url: createURL("addNetworkServiceProvider"),
                            data: {
                                name: 'CiscoVnmc',
                                physicalnetworkid: args.context.physicalNetworks[0].id
                            },
                            success: function (json) {                            	
                                var jobId = json.addnetworkserviceproviderresponse.jobid;
                                var addVnmcProviderIntervalID = setInterval(function () {
                                    $.ajax({
                                        url: createURL("queryAsyncJobResult&jobId=" + jobId),
                                        dataType: "json",
                                        success: function (json) {
                                            var result = json.queryasyncjobresultresponse;
                                            if (result.jobstatus == 0) {
                                                return; //Job has not completed
                                            } else {
                                                clearInterval(addVnmcProviderIntervalID);
                                                if (result.jobstatus == 1) {                                                    
                                                    var item = json.queryasyncjobresultresponse.jobresult.networkserviceprovider;        
                                                    args.response.success({
                                                        data: item
                                                    });                                                    
                                                } else if (result.jobstatus == 2) {
                                                    args.response.error(_s(result.jobresult.errortext));
                                                }
                                            }
                                        },
                                        error: function (XMLHttpResponse) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });
                                }, g_queryAsyncJobResultInterval);
                            }
                        });                    	
                    }
            	},
                enable: {
                    label: 'Enable VNMC provider',

                    messages: {
                        confirm: function (args) {
                            return 'Please confirm you would like to enable the VNMC provider.';
                        },
                        notification: function (args) {
                            return 'Enable VNMC device';
                        }
                    },

                    action: function (args) {
                        $.ajax({
                            url: createURL('listNetworkServiceProviders'),
                            data: {
                                name: 'CiscoVnmc',
                                physicalnetworkid: args.context.physicalNetworks[0].id
                            },
                            success: function (json) {
                                var items = json.listnetworkserviceprovidersresponse.networkserviceprovider;
                                if (items != null && items.length > 0) {
                                    var ciscoVnmcProvider = items[0];

                                    enableCiscoVnmcProviderFn(ciscoVnmcProvider);
                                } else {
                                    args.response.error('VNMC is not available from provider list.');
                                }
                            }
                        });

                        var enableCiscoVnmcProviderFn = function (ciscoVnmcProvider) {
                            $.ajax({
                                url: createURL('updateNetworkServiceProvider'),
                                data: {
                                    id: ciscoVnmcProvider.id,
                                    state: 'Enabled'
                                },
                                success: function (json) {
                                    var jid = json.updatenetworkserviceproviderresponse.jobid;
                                    var enableVnmcProviderIntervalID = setInterval(function () {
                                        $.ajax({
                                            url: createURL('queryAsyncJobResult'),
                                            data: {
                                                jobid: jid
                                            },
                                            success: function (json) {
                                                var result = json.queryasyncjobresultresponse;                                                
                                                if (result.jobstatus == 0) {
                                                    return; //Job has not completed
                                                } else {
                                                	clearInterval(enableVnmcProviderIntervalID);
                                                    if (result.jobstatus == 1) {
                                                        args.response.success({
                                                            data: result.jobresult.networkserviceprovider
                                                        });
                                                    } else if (result.jobstatus == 2) {
                                                        args.response.error(_s(result.jobresult.errortext));
                                                    }
                                                }
                                            }
                                        });
                                    }, g_queryAsyncJobResultInterval);
                                }
                            });
                        }

                        var addCiscoVnmcResourceFn = function () {
                            var data = {
                                physicalnetworkid: args.context.physicalNetworks[0].id,
                                hostname: args.data.hostname,
                                username: args.data.username,
                                password: args.data.password
                            };

                            $.ajax({
                                url: createURL('addCiscoVnmcResource'),
                                data: data,
                                success: function (json) {
                                    var item = json.addCiscoVnmcResource.CiscoVnmcResource;
                                    args.response.success({
                                        data: item
                                    });
                                },
                                error: function (data) {
                                    args.response.error(parseXMLHttpResponse(data));
                                }
                            });
                        }
                    }                    
                },

                disable: {
                    label: 'Disable VNMC provider',

                    messages: {
                        confirm: function (args) {
                            return 'Please confirm you would like to disable the VNMC provider.';
                        },
                        notification: function (args) {
                            return 'Disable VNMC device';
                        }
                    },

                    action: function (args) {
                        $.ajax({
                            url: createURL('listNetworkServiceProviders'),
                            data: {
                                name: 'CiscoVnmc',
                                physicalnetworkid: args.context.physicalNetworks[0].id
                            },
                            success: function (json) {
                                var items = json.listnetworkserviceprovidersresponse.networkserviceprovider;
                                if (items != null && items.length > 0) {
                                    var ciscoVnmcProvider = items[0];

                                    disableCiscoVnmcProviderFn(ciscoVnmcProvider);
                                } else {
                                    args.response.error('VNMC is not available from provider list.');
                                }
                            }
                        });

                        var disableCiscoVnmcProviderFn = function (ciscoVnmcProvider) {
                            $.ajax({
                                url: createURL('updateNetworkServiceProvider'),
                                data: {
                                    id: ciscoVnmcProvider.id,
                                    state: 'Disabled'
                                },
                                success: function (json) {
                                    var jid = json.updatenetworkserviceproviderresponse.jobid;
                                    var disableVnmcProviderIntervalID = setInterval(function () {
                                        $.ajax({
                                            url: createURL('queryAsyncJobResult'),
                                            data: {
                                                jobid: jid
                                            },
                                            success: function (json) {
                                                var result = json.queryasyncjobresultresponse;                                             
                                                if (result.jobstatus == 0) {
                                                    return; //Job has not completed
                                                } else {
                                                    clearInterval(disableVnmcProviderIntervalID);
                                                    if (result.jobstatus == 1) {
                                                        args.response.success({
                                                            data: result.jobresult.networkserviceprovider
                                                        });
                                                    } else if (result.jobstatus == 2) {
                                                        args.response.error(_s(result.jobresult.errortext));
                                                    }
                                                }
                                            }
                                        });
                                    }, g_queryAsyncJobResultInterval);
                                }
                            });
                        };

                        var addCiscoVnmcResourceFn = function () {
                            var data = {
                                physicalnetworkid: args.context.physicalNetworks[0].id,
                                hostname: args.data.hostname,
                                username: args.data.username,
                                password: args.data.password
                            };

                            $.ajax({
                                url: createURL('addCiscoVnmcResource'),
                                data: data,
                                success: function (json) {
                                    var item = json.addCiscoVnmcResource.CiscoVnmcResource;
                                    args.response.success({
                                        data: item
                                    });
                                },
                                error: function (data) {
                                    args.response.error(parseXMLHttpResponse(data));
                                }
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
                            label: 'label.name'
                        }
                    }, {
                        state: {
                            label: 'label.state'
                        },
                        id: {
                            label: 'label.id'
                        },
                        servicelist: {
                            label: 'Services',
                            converter: function (args) {
                                if (args)
                                    return args.join(', ');
                                else
                                    return '';
                            }
                        }
                    }],
                    dataProvider: function (args) {
                        $.ajax({
                            url: createURL('listNetworkServiceProviders'),
                            data: {
                                name: 'CiscoVnmc',
                                physicalnetworkid: args.context.physicalNetworks[0].id
                            },
                            success: function (json) {
                                var items = json.listnetworkserviceprovidersresponse.networkserviceprovider;
                                if (items != null && items.length > 0) {
                                    args.response.success({
                                        data: items[0],
                                        actionFilter: vnmcActionFilter
                                    });
                                } else {
                                    args.response.success({
                                        data: {
                                            name: 'CiscoVnmc',
                                            state: 'Absent'
                                        },
                                        actionFilter: vnmcActionFilter
                                    });
                                }
                            }
                        });
                    }
                }
            }
        };

        var vnmcActionFilter = function (args) {
        	var allowedActions = [];
            $.ajax({
                url: createURL('listNetworkServiceProviders'),                
                data: {
                    name: 'CiscoVnmc',
                    physicalnetworkid: args.context.physicalNetworks[0].id
                },
                async: false,
                success: function (json) {
                    var items = json.listnetworkserviceprovidersresponse.networkserviceprovider;                    
                    if (items != null && items.length > 0) {
                        var ciscoVnmcProvider = items[0];
                        if (ciscoVnmcProvider.state == 'Enabled') {
                        	allowedActions.push('disable');
                        } else if (ciscoVnmcProvider.state == 'Disabled') {
                        	allowedActions.push('enable');
                        }
                    } else {
                    	allowedActions.push('add');
                    }
                }
            });
            return allowedActions;
        };

        module.pluginAPI.extend({
            addDevice: function (device) {
                cloudStack.sections.system.subsections[device.id] = device;
                vnmcDeviceViewAll.push({
                    label: device.title,
                    path: '_zone.' + device.id
                });
            }
        });

        module.infrastructure.networkServiceProvider({
            id: 'CiscoVnmc',
            name: 'Cisco VNMC',
            //state: 'Disabled', //don't know state until log in and visit Infrastructure menu > zone detail > physical network > network service providers
            listView: vnmcListView,

            detailView: vnmcProviderDetailView
        });
    };
}(jQuery, cloudStack));