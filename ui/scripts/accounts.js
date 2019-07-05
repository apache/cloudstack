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

    var domainObjs;
    var roleObjs;

    cloudStack.sections.accounts = {
        title: 'label.accounts',
        id: 'accounts',
        sectionSelect: {
            label: 'label.select-view',
            preFilter: function() {
                return ['accounts', 'sshkeypairs'];
            }
        },
        sections: {
            accounts: {
                type: 'select',
                id: 'accounts',
                title: 'label.accounts',
                listView: {
                    id: 'accounts',
                    fields: {
                        name: {
                            label: 'label.name'
                        },
                        rolename: {
                            label: 'label.role'
                        },
                        roletype: {
                            label: 'label.roletype'
                        },
                        domainpath: {
                            label: 'label.domain'
                        },
                        state: {
                            converter: function(str) {
                                // For localization
                                return str;
                            },
                            label: 'label.state',
                            indicator: {
                                'enabled': 'on',
                                'Destroyed': 'off',
                                'disabled': 'off'
                            }
                        }
                    },

                    actions: {
                        add: {
                            label: 'label.add.account',
                            preFilter: function(args) {
                                if (isAdmin() || isDomainAdmin())
                                    return true;
                                else
                                    return false;
                            },
                            messages: {
                                notification: function(args) {
                                    return 'label.add.account';
                                }
                            },
                            notification: {
                                poll: function(args) {
                                    args.complete({
                                        actionFilter: accountActionfilter
                                    });
                                }
                            },

                            action: {
                                custom: cloudStack.uiCustom.accountsWizard(
                                    cloudStack.accountsWizard,
                                    false
                                )
                            }

                        },

                        addLdapAccount: {
                            label: 'label.add.ldap.account',
                            isHeader: true,
                            preFilter: function(args) {
                                if (isAdmin() && isLdapEnabled()) {
                                    return true;
                                } else {
                                    return false;
                                }
                            },
                            messages: {
                                notification: function(args) {
                                    return 'label.add.ldap.account';
                                }
                            },
                            notification: {
                                poll: function(args) {
                                    args.complete({
                                        actionFilter: accountActionfilter
                                    });
                                }
                            },

                            action: {
                                custom: cloudStack.uiCustom.accountsWizard(
                                    cloudStack.accountsWizard,
                                    true
                                )
                            }

                        }
                    },

                    dataProvider: function(args) {
                        var data = {};
                        listViewDataProvider(args, data);

                        if ("domains" in args.context) {
                            $.extend(data, {
                                domainid: args.context.domains[0].id
                            });
                        }

                        if ("routers" in args.context) {
                            if ("account" in args.context.routers[0]) {
                                $.extend(data, {
                                    name: args.context.routers[0].account
                                });
                            }
                            if ("domainid" in args.context.routers[0]) {
                                $.extend(data, {
                                    domainid: args.context.routers[0].domainid
                                });
                            }
                        }

                        $.extend(data, {
                            details: 'min'
                        });

                        $.ajax({
                            url: createURL('listAccounts'),
                            data: data,
                            async: true,
                            success: function(json) {
                                var items = json.listaccountsresponse.account;
                                args.response.success({
                                    actionFilter: accountActionfilter,
                                    data: items
                                });
                            }
                        });

                        // SAML: Check Append Domain Setting
                        if (g_idpList) {
                            $.ajax({
                                type: 'GET',
                                url: createURL('listConfigurations&name=saml2.append.idpdomain'),
                                dataType: 'json',
                                async: false,
                                success: function(data, textStatus, xhr) {
                                    if (data && data.listconfigurationsresponse && data.listconfigurationsresponse.configuration) {
                                        g_appendIdpDomain = (data.listconfigurationsresponse.configuration[0].value === 'true');
                                    }
                                },
                                error: function(xhr) {
                                }
                            });
                        }
                    },

                    detailView: {
                        name: 'label.account.details',
                        isMaximized: true,
                        viewAll: {
                            path: 'accounts.users',
                            label: 'label.users'
                        },

                        actions: {
                            edit: {
                                label: 'message.edit.account',
                                compactLabel: 'label.edit',
                                action: function(args) {
                                    var accountObj = args.context.accounts[0];

                                    var data = {
                                        domainid: accountObj.domainid,
                                        account: accountObj.name,
                                        newname: args.data.name,
                                        networkdomain: args.data.networkdomain
                                    };

                                    $.ajax({
                                        url: createURL('updateAccount'),
                                        data: data,
                                        async: false,
                                        success: function(json) {
                                            accountObj = json.updateaccountresponse.account;
                                        },
                                        error: function(json) {
                                            var errorMsg = parseXMLHttpResponse(json);
                                            args.response.error(errorMsg);
                                        }
                                    });

                                    if (args.data.vmLimit != null) {
                                        var data = {
                                            resourceType: 0,
                                            max: args.data.vmLimit,
                                            domainid: accountObj.domainid,
                                            account: accountObj.name
                                        };
                                        $.ajax({
                                            url: createURL('updateResourceLimit'),
                                            data: data,
                                            async: false,
                                            success: function(json) {
                                                accountObj["vmLimit"] = args.data.vmLimit;
                                            }
                                        });
                                    }

                                    if (args.data.ipLimit != null) {
                                        var data = {
                                            resourceType: 1,
                                            max: args.data.ipLimit,
                                            domainid: accountObj.domainid,
                                            account: accountObj.name
                                        };
                                        $.ajax({
                                            url: createURL('updateResourceLimit'),
                                            data: data,
                                            async: false,
                                            success: function(json) {
                                                accountObj["ipLimit"] = args.data.ipLimit;
                                            }
                                        });
                                    }

                                    if (args.data.volumeLimit != null) {
                                        var data = {
                                            resourceType: 2,
                                            max: args.data.volumeLimit,
                                            domainid: accountObj.domainid,
                                            account: accountObj.name
                                        };
                                        $.ajax({
                                            url: createURL('updateResourceLimit'),
                                            data: data,
                                            async: false,
                                            success: function(json) {
                                                accountObj["volumeLimit"] = args.data.volumeLimit;
                                            }
                                        });
                                    }

                                    if (args.data.snapshotLimit != null) {
                                        var data = {
                                            resourceType: 3,
                                            max: args.data.snapshotLimit,
                                            domainid: accountObj.domainid,
                                            account: accountObj.name
                                        };
                                        $.ajax({
                                            url: createURL('updateResourceLimit'),
                                            data: data,
                                            async: false,
                                            success: function(json) {
                                                accountObj["snapshotLimit"] = args.data.snapshotLimit;
                                            }
                                        });
                                    }

                                    if (args.data.templateLimit != null) {
                                        var data = {
                                            resourceType: 4,
                                            max: args.data.templateLimit,
                                            domainid: accountObj.domainid,
                                            account: accountObj.name
                                        };
                                        $.ajax({
                                            url: createURL('updateResourceLimit'),
                                            data: data,
                                            async: false,
                                            success: function(json) {
                                                accountObj["templateLimit"] = args.data.templateLimit;
                                            }
                                        });
                                    }

                                    if (args.data.vpcLimit != null) {
                                        var data = {
                                            resourceType: 7,
                                            max: args.data.vpcLimit,
                                            domainid: accountObj.domainid,
                                            account: accountObj.name
                                        };

                                        $.ajax({
                                            url: createURL('updateResourceLimit'),
                                            data: data,
                                            async: false,
                                            success: function(json) {
                                                accountObj["vpcLimit"] = args.data.vpcLimit;
                                            }
                                        });
                                    }

                                    if (args.data.cpuLimit != null) {
                                        var data = {
                                            resourceType: 8,
                                            max: args.data.cpuLimit,
                                            domainid: accountObj.domainid,
                                            account: accountObj.name
                                        };

                                        $.ajax({
                                            url: createURL('updateResourceLimit'),
                                            data: data,
                                            async: false,
                                            success: function(json) {
                                                accountObj["cpuLimit"] = args.data.cpuLimit;
                                            }
                                        });
                                    }

                                    if (args.data.memoryLimit != null) {
                                        var data = {
                                            resourceType: 9,
                                            max: args.data.memoryLimit,
                                            domainid: accountObj.domainid,
                                            account: accountObj.name
                                        };

                                        $.ajax({
                                            url: createURL('updateResourceLimit'),
                                            data: data,
                                            async: false,
                                            success: function(json) {
                                                accountObj["memoryLimit"] = args.data.memoryLimit;
                                            }
                                        });
                                    }

                                    if (args.data.networkLimit != null) {
                                        var data = {
                                            resourceType: 6,
                                            max: args.data.networkLimit,
                                            domainid: accountObj.domainid,
                                            account: accountObj.name
                                        };

                                        $.ajax({
                                            url: createURL('updateResourceLimit'),
                                            data: data,
                                            async: false,
                                            success: function(json) {
                                                accountObj["networkLimit"] = args.data.networkLimit;
                                            }
                                        });
                                    }

                                    if (args.data.primaryStorageLimit != null) {
                                        var data = {
                                            resourceType: 10,
                                            max: args.data.primaryStorageLimit,
                                            domainid: accountObj.domainid,
                                            account: accountObj.name
                                        };

                                        $.ajax({
                                            url: createURL('updateResourceLimit'),
                                            data: data,
                                            async: false,
                                            success: function(json) {
                                                accountObj["primaryStorageLimit"] = args.data.primaryStorageLimit;
                                            }
                                        });
                                    }

                                    if (args.data.secondaryStorageLimit != null) {
                                        var data = {
                                            resourceType: 11,
                                            max: args.data.secondaryStorageLimit,
                                            domainid: accountObj.domainid,
                                            account: accountObj.name
                                        };

                                        $.ajax({
                                            url: createURL('updateResourceLimit'),
                                            data: data,
                                            async: false,
                                            success: function(json) {
                                                accountObj["secondaryStorageLimit"] = args.data.secondaryStorageLimit;
                                            }
                                        });
                                    }
                                    args.response.success({
                                        data: accountObj
                                    });
                                }
                            },

                            updateResourceCount: {
                                label: 'label.action.update.resource.count',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.update.resource.count';
                                    },
                                    notification: function(args) {
                                        return 'label.action.update.resource.count';
                                    }
                                },
                                action: function(args) {
                                    var accountObj = args.context.accounts[0];
                                    var data = {
                                        domainid: accountObj.domainid,
                                        account: accountObj.name
                                    };

                                    $.ajax({
                                        url: createURL('updateResourceCount'),
                                        data: data,
                                        async: true,
                                        success: function(json) {
                                            var resourcecounts= json.updateresourcecountresponse.resourcecount;
                                            //pop up API response in a dialog box since only updateResourceCount API returns resourcecount (listResourceLimits API does NOT return resourcecount)
                                            var msg = '';
                                            if (resourcecounts != null) {
                                                for (var i = 0; i < resourcecounts.length; i++) {
                                                    switch (resourcecounts[i].resourcetype) {
                                                    case '0':
                                                        msg += 'Instance'; //vmLimit
                                                        break;
                                                    case '1':
                                                        msg += 'Public IP'; //ipLimit
                                                        break;
                                                    case '2':
                                                        msg += 'Volume'; //volumeLimit
                                                        break;
                                                    case '3':
                                                        msg += 'Snapshot'; //snapshotLimit
                                                        break;
                                                    case '4':
                                                        msg += 'Template'; //templateLimit
                                                        break;
                                                    case '5':
                                                        continue; //resourcetype 5 is not in use. so, skip to next item.
                                                        break;
                                                    case '6':
                                                        msg += 'Network'; //networkLimit
                                                        break;
                                                    case '7':
                                                        msg += 'VPC'; //vpcLimit
                                                        break;
                                                    case '8':
                                                        msg += 'CPU'; //cpuLimit
                                                        break;
                                                    case '9':
                                                        msg += 'Memory'; //memoryLimit
                                                        break;
                                                    case '10':
                                                        msg += 'Primary Storage'; //primaryStorageLimit
                                                        break;
                                                    case '11':
                                                        msg += 'Secondary Storage'; //secondaryStorageLimit
                                                        break;
                                                    }

                                                    msg += ' Count: ' + resourcecounts[i].resourcecount + ' <br> ';
                                                }
                                            }


                                            cloudStack.dialog.notice({
                                                message: msg
                                            });

                                            args.response.success();
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
                            },

                            disable: {
                                label: 'label.action.disable.account',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.disable.account';
                                    },
                                    notification: function(args) {
                                        return 'label.action.disable.account';
                                    }
                                },
                                action: function(args) {
                                    var accountObj = args.context.accounts[0];
                                    var data = {
                                        lock: false,
                                        domainid: accountObj.domainid,
                                        account: accountObj.name
                                    };

                                    $.ajax({
                                        url: createURL('disableAccount'),
                                        data: data,
                                        async: true,
                                        success: function(json) {
                                            var jid = json.disableaccountresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid,
                                                    getUpdatedItem: function(json) {
                                                        return json.queryasyncjobresultresponse.jobresult.account;
                                                    },
                                                    getActionFilter: function() {
                                                        return accountActionfilter;
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

                            lock: {
                                label: 'label.action.lock.account',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.lock.account';
                                    },
                                    notification: function(args) {
                                        return 'label.action.lock.account';
                                    }
                                },
                                action: function(args) {
                                    var accountObj = args.context.accounts[0];
                                    var data = {
                                        lock: true,
                                        domainid: accountObj.domainid,
                                        account: accountObj.name
                                    };

                                    $.ajax({
                                        url: createURL('disableAccount'),
                                        data: data,
                                        async: true,
                                        success: function(json) {
                                            var jid = json.disableaccountresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid,
                                                    getUpdatedItem: function(json) {
                                                        return json.queryasyncjobresultresponse.jobresult.account;
                                                    },
                                                    getActionFilter: function() {
                                                        return accountActionfilter;
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

                            enable: {
                                label: 'label.action.enable.account',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.enable.account';
                                    },
                                    notification: function(args) {
                                        return 'label.action.enable.account';
                                    }
                                },
                                action: function(args) {
                                    var accountObj = args.context.accounts[0];
                                    var data = {
                                        domainid: accountObj.domainid,
                                        account: accountObj.name
                                    };
                                    $.ajax({
                                        url: createURL('enableAccount'),
                                        data: data,
                                        async: true,
                                        success: function(json) {
                                            args.response.success({
                                                data: json.enableaccountresponse.account
                                            });
                                        }
                                    });
                                },
                                notification: {
                                    poll: function(args) {
                                        args.complete({
                                            data: {
                                                state: 'enabled'
                                            }
                                        });
                                    }
                                }
                            },

                            remove: {
                                label: 'label.action.delete.account',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.delete.account';
                                    },
                                    notification: function(args) {
                                        return 'label.action.delete.account';
                                    }
                                },
                                action: function(args) {
                                    var data = {
                                        id: args.context.accounts[0].id
                                    };
                                    $.ajax({
                                        url: createURL('deleteAccount'),
                                        data: data,
                                        async: true,
                                        success: function(json) {
                                            var jid = json.deleteaccountresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid,
                                                    getUpdatedItem: function(json) {
                                                        return {}; //nothing in this account needs to be updated, in fact, this whole account has being deleted
                                                    },
                                                    getActionFilter: function() {
                                                        return accountActionfilter;
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

                        tabFilter: function(args) {
                            var hiddenTabs = [];
                            if(!isAdmin()) {
                                hiddenTabs.push('settings');
                            }
                            return hiddenTabs;
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
                                    rolename: {
                                        label: 'label.role'
                                    },
                                    roletype: {
                                        label: 'label.roletype'
                                    },
                                    domainpath: {
                                        label: 'label.domain'
                                    },
                                    state: {
                                        label: 'label.state'
                                    },
                                    networkdomain: {
                                        label: 'label.network.domain',
                                        isEditable: true
                                    },
                                    vmLimit: {
                                        label: 'label.instance.limits',
                                        isEditable: function(context) {

                                            if (context.accounts == undefined)
                                                return false;
                                            else {
                                                if (context.accounts[0].accounttype == roleTypeUser || context.accounts[0].accounttype == roleTypeDomainAdmin) //updateResourceLimits is only allowed on account whose type is user or domain-admin
                                                    return true;
                                                else
                                                    return false;
                                            }
                                        }
                                    },
                                    ipLimit: {
                                        label: 'label.ip.limits',
                                        isEditable: function(context) {
                                            if (context.accounts[0].accounttype == roleTypeUser || context.accounts[0].accounttype == roleTypeDomainAdmin) //updateResourceLimits is only allowed on account whose type is user or domain-admin
                                                return true;
                                            else
                                                return false;
                                        }
                                    },
                                    volumeLimit: {
                                        label: 'label.volume.limits',
                                        isEditable: function(context) {
                                            if (context.accounts[0].accounttype == roleTypeUser || context.accounts[0].accounttype == roleTypeDomainAdmin) //updateResourceLimits is only allowed on account whose type is user or domain-admin
                                                return true;
                                            else
                                                return false;
                                        }
                                    },
                                    snapshotLimit: {
                                        label: 'label.snapshot.limits',
                                        isEditable: function(context) {
                                            if (context.accounts[0].accounttype == roleTypeUser || context.accounts[0].accounttype == roleTypeDomainAdmin) //updateResourceLimits is only allowed on account whose type is user or domain-admin
                                                return true;
                                            else
                                                return false;
                                        }
                                    },
                                    templateLimit: {
                                        label: 'label.template.limits',
                                        isEditable: function(context) {
                                            if (context.accounts[0].accounttype == roleTypeUser || context.accounts[0].accounttype == roleTypeDomainAdmin) //updateResourceLimits is only allowed on account whose type is user or domain-admin
                                                return true;
                                            else
                                                return false;
                                        }
                                    },
                                    vpcLimit: {
                                        label: 'label.VPC.limits',
                                        isEditable: function(context) {
                                            if (context.accounts[0].accounttype == roleTypeUser || context.accounts[0].accounttype == roleTypeDomainAdmin) //updateResourceLimits is only allowed on account whose type is user or domain-admin
                                                return true;
                                            else
                                                return false;
                                        }
                                    },
                                    cpuLimit: {
                                        label: 'label.cpu.limits',
                                        isEditable: function(context) {
                                            if (context.accounts[0].accounttype == roleTypeUser || context.accounts[0].accounttype == roleTypeDomainAdmin) //updateResourceLimits is only allowed on account whose type is user or domain-admin
                                                return true;
                                            else
                                                return false;
                                        }
                                    },
                                    memoryLimit: {
                                        label: 'label.memory.limits',
                                        isEditable: function(context) {
                                            if (context.accounts[0].accounttype == roleTypeUser || context.accounts[0].accounttype == roleTypeDomainAdmin) //updateResourceLimits is only allowed on account whose type is user or domain-admin
                                                return true;
                                            else
                                                return false;
                                        }
                                    },
                                    networkLimit: {
                                        label: 'label.network.limits',
                                        isEditable: function(context) {
                                            if (context.accounts[0].accounttype == roleTypeUser || context.accounts[0].accounttype == roleTypeDomainAdmin) //updateResourceLimits is only allowed on account whose type is user or domain-admin
                                                return true;
                                            else
                                                return false;
                                        }
                                    },
                                    primaryStorageLimit: {
                                        label: 'label.primary.storage.limits',
                                        isEditable: function(context) {
                                            if (context.accounts[0].accounttype == roleTypeUser || context.accounts[0].accounttype == roleTypeDomainAdmin) //updateResourceLimits is only allowed on account whose type is user or domain-admin
                                                return true;
                                            else
                                                return false;
                                        }
                                    },
                                    secondaryStorageLimit: {
                                        label: 'label.secondary.storage.limits',
                                        isEditable: function(context) {
                                            if (context.accounts[0].accounttype == roleTypeUser || context.accounts[0].accounttype == roleTypeDomainAdmin) //updateResourceLimits is only allowed on account whose type is user or domain-admin
                                                return true;
                                            else
                                                return false;
                                        }
                                    },

                                    vmtotal: {
                                        label: 'label.total.of.vm'
                                    },
                                    iptotal: {
                                        label: 'label.total.of.ip'
                                    },
                                    receivedbytes: {
                                        label: 'label.bytes.received',
                                        converter: function(args) {
                                            if (args == null || args == 0)
                                                return "";
                                            else
                                                return cloudStack.converters.convertBytes(args);
                                        }
                                    },
                                    sentbytes: {
                                        label: 'label.bytes.sent',
                                        converter: function(args) {
                                            if (args == null || args == 0)
                                                return "";
                                            else
                                                return cloudStack.converters.convertBytes(args);
                                        }
                                    }
                                }],

                                dataProvider: function(args) {
                                    var data = {
                                        id: args.context.accounts[0].id
                                    };
                                    $.ajax({
                                        url: createURL('listAccounts'),
                                        data: data,
                                        success: function(json) {
                                            var accountObj = json.listaccountsresponse.account[0];
                                            var data = {
                                                domainid: accountObj.domainid,
                                                account: accountObj.name
                                            };
                                            $.ajax({
                                                url: createURL('listResourceLimits'),
                                                data: data,
                                                success: function(json) {
                                                    var limits = json.listresourcelimitsresponse.resourcelimit;
                                                    if (limits != null) {
                                                        for (var i = 0; i < limits.length; i++) {
                                                            var limit = limits[i];
                                                            switch (limit.resourcetype) {
                                                                case "0":
                                                                    accountObj["vmLimit"] = limit.max;
                                                                    break;
                                                                case "1":
                                                                    accountObj["ipLimit"] = limit.max;
                                                                    break;
                                                                case "2":
                                                                    accountObj["volumeLimit"] = limit.max;
                                                                    break;
                                                                case "3":
                                                                    accountObj["snapshotLimit"] = limit.max;
                                                                    break;
                                                                case "4":
                                                                    accountObj["templateLimit"] = limit.max;
                                                                    break;
                                                                case "6":
                                                                    accountObj["networkLimit"] = limit.max;
                                                                    break;
                                                                case "7":
                                                                    accountObj["vpcLimit"] = limit.max;
                                                                    break;
                                                                case "8":
                                                                    accountObj["cpuLimit"] = limit.max;
                                                                    break;
                                                                case "9":
                                                                    accountObj["memoryLimit"] = limit.max;
                                                                    break;
                                                                case "10":
                                                                    accountObj["primaryStorageLimit"] = limit.max;
                                                                    break;
                                                                case "11":
                                                                    accountObj["secondaryStorageLimit"] = limit.max;
                                                                    break;
                                                            }
                                                        }
                                                    }
                                                    args.response.success({
                                                        actionFilter: accountActionfilter,
                                                        data: accountObj
                                                    });
                                                }
                                            });
                                        }
                                    });
                                }
                            },

                            sslCertificates: {
                                title: 'label.sslcertificates',
                                listView: {
                                    id: 'sslCertificates',
                                    
                                    fields: {
                                        name: {
                                            label: 'label.name'
                                        },
                                        id: {
                                            label: 'label.certificateid'
                                        }
                                    },
                                    
                                    dataProvider: function(args) {
                                        var data = {};
                                        listViewDataProvider(args, data);
                                        if (args.context != null) {
                                            if ("accounts" in args.context) {
                                                $.extend(data, {
                                                    accountid: args.context.accounts[0].id
                                                });
                                            }
                                        }
                                        $.ajax({
                                            url: createURL('listSslCerts'),
                                            data: data,
                                            success: function(json) {
                                                var items = json.listsslcertsresponse.sslcert;
                                                args.response.success({
                                                    data: items
                                                });
                                            }
                                        });
                                    },
                                    
                                    actions: {
                                        add: {
                                            label: 'label.add.certificate',

                                            messages: {
                                                notification: function(args) {
                                                    return 'label.add.certificate';
                                                }
                                            },

                                            createForm: {
                                                title: 'label.add.certificate',
                                                fields: {
                                                    name: {
                                                        label: 'label.name',
                                                        validation: {
                                                            required: true
                                                        }
                                                    },
                                                    certificate: {
                                                        label: 'label.certificate.name',
                                                        isTextarea: true,
                                                        validation: {
                                                            required: true
                                                        },
                                                    },
                                                    privatekey: {
                                                        label: 'label.privatekey.name',
                                                        isTextarea: true,
                                                        validation: {
                                                            required: true
                                                        }
                                                    },
                                                    certchain: {
                                                        label: "label.chain",
                                                        isTextarea: true,
                                                        validation: {
                                                            required: false
                                                        }
                                                    },
                                                    password: {
                                                        label: "label.privatekey.password",
                                                        isPassword: true,
                                                        validation: {
                                                            required: false
                                                        }
                                                    }
                                                }
                                            },

                                            action: function(args) {
                                                var data = {
                                                    name: args.data.name,
                                                    certificate: args.data.certificate,
                                                    privatekey: args.data.privatekey
                                                };

                                                if (args.data.certchain != null && args.data.certchain.length > 0) {
                                                    $.extend(data, {
                                                        certchain: args.data.certchain
                                                    });
                                                }

                                                if (args.data.password != null && args.data.password.length > 0) {
                                                    $.extend(data, {
                                                        password: args.data.password
                                                    });
                                                }

                                                $.ajax({
                                                    url: createURL('uploadSslCert'),
                                                    type: "POST",
                                                    data: data,
                                                    success: function(json) {
                                                        var item = json.uploadsslcertresponse.sslcert;
                                                        args.response.success({
                                                            data: item
                                                        });
                                                    },
                                                    error: function(json) {
                                                        args.response.error(parseXMLHttpResponse(json));
                                                    }
                                                });
                                            }
                                        }
                                    },
                                    
                                    detailView: {
                                        actions: {
                                            remove: {
                                                label: 'label.delete.sslcertificate',
                                                messages: {
                                                    confirm: function(args) {
                                                        return 'message.delete.sslcertificate';
                                                    },
                                                    notification: function(args) {
                                                        return 'label.delete.sslcertificate';
                                                    }
                                                },
                                                action: function(args) {
                                                    $.ajax({
                                                        url: createURL('deleteSslCert'),
                                                        data: {
                                                            id: args.context.sslCertificates[0].id
                                                        },
                                                        success: function(json) {
                                                            var items = json.deletesslcertresponse.sslcert;
                                                            args.response.success({
                                                                data: items
                                                            });
                                                        }
                                                    });
                                                }
                                            }
                                        },

                                        tabs: {
                                            details: {
                                                title: 'label.certificate.details',
                                                fields: {
                                                    name: {
                                                        label: 'label.name'
                                                    },
                                                    certificate: {
                                                        label: 'label.certificate.name'
                                                    },
                                                    certchain: {
                                                        label: 'label.chain'
                                                    }
                                                },

                                                dataProvider: function(args) {
                                                    var data = {};
                                                
                                                    if (args.context != null) {
                                                        if ("sslCertificates" in args.context) {
                                                            $.extend(data, {
                                                                certid: args.context.sslCertificates[0].id
                                                            });
                                                        }
                                                    }
                                                    $.ajax({
                                                        url: createURL('listSslCerts'),
                                                        data: data,
                                                        success: function(json) {
                                                            var items = json.listsslcertsresponse.sslcert[0];
                                                            args.response.success({
                                                                data: items
                                                            });
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    }
                                }
                            },

                            // Granular settings for account
                            settings: {
                                title: 'label.settings',
                                custom: cloudStack.uiCustom.granularSettings({
                                    dataProvider: function(args) {
                                        $.ajax({
                                            url: createURL('listConfigurations&accountid=' + args.context.accounts[0].id),
                                            data: listViewDataProvider(args, {}, { searchBy: 'name' }),
                                            success: function(json) {
                                                args.response.success({
                                                    data: json.listconfigurationsresponse.configuration
                                                });

                                            },

                                            error: function(json) {
                                                args.response.error(parseXMLHttpResponse(json));

                                            }
                                        });

                                    },
                                    actions: {
                                        edit: function(args) {
                                            // call updateAccountLevelParameters
                                            var data = {
                                                name: args.data.jsonObj.name,
                                                value: args.data.value
                                            };

                                            $.ajax({
                                                url: createURL('updateConfiguration&accountid=' + args.context.accounts[0].id),
                                                data: data,
                                                success: function(json) {
                                                    var item = json.updateconfigurationresponse.configuration;
                                                    args.response.success({
                                                        data: item
                                                    });
                                                },

                                                error: function(json) {
                                                    args.response.error(parseXMLHttpResponse(json));
                                                }

                                            });

                                        }
                                    }
                                })
                            }
                        }
                    }
                }
            },
            users: {
                type: 'select',
                id: 'users',
                title: 'label.users',
                listView: {
                    id: 'users',
                    fields: {
                        username: {
                            label: 'label.username',
                            editable: true
                        },
                        firstname: {
                            label: 'label.first.name'
                        },
                        lastname: {
                            label: 'label.last.name'
                        }
                    },
                    dataProvider: function(args) {
                        var accountObj = args.context.accounts[0];

                        if (isAdmin() || isDomainAdmin()) {
                            var data = {
                                domainid: accountObj.domainid,
                                account: accountObj.name
                            };
                            listViewDataProvider(args, data);

                            $.ajax({
                                url: createURL('listUsers'),
                                data: data,
                                success: function(json) {
                                    args.response.success({
                                        actionFilter: userActionfilter,
                                        data: json.listusersresponse.user
                                    });
                                }
                            })
                        } else { //normal user doesn't have access listUsers API until Bug 14127 is fixed.
                            args.response.success({
                                actionFilter: userActionfilter,
                                data: accountObj.user
                            });
                        }
                    },
                    actions: {
                        add: {
                            label: 'label.add.user',

                            preFilter: function(args) {
                                if (isAdmin() || isDomainAdmin())
                                    return true;
                                else
                                    return false;
                            },

                            messages: {
                                notification: function(args) {
                                    return 'label.add.user';
                                }
                            },

                            createForm: {
                                title: 'label.add.user',
                                fields: {
                                    username: {
                                        label: 'label.username',
                                        validation: {
                                            required: true
                                        },
                                        docID: 'helpUserUsername'
                                    },
                                    password: {
                                        label: 'label.password',
                                        isPassword: true,
                                        validation: {
                                            required: true
                                        },
                                        id: 'password',
                                        docID: 'helpUserPassword'
                                    },
                                    'password-confirm': {
                                        label: 'label.confirm.password',
                                        docID: 'helpUserConfirmPassword',
                                        validation: {
                                            required: true,
                                            equalTo: '#password'
                                        },
                                        isPassword: true
                                    },
                                    email: {
                                        label: 'label.email',
                                        docID: 'helpUserEmail',
                                        validation: {
                                            required: true,
                                            email: true
                                        }
                                    },
                                    firstname: {
                                        label: 'label.first.name',
                                        docID: 'helpUserFirstName',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    lastname: {
                                        label: 'label.last.name',
                                        docID: 'helpUserLastName',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    timezone: {
                                        label: 'label.timezone',
                                        docID: 'helpUserTimezone',
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: "",
                                                description: ""
                                            });
                                            for (var p in timezoneMap)
                                                items.push({
                                                    id: p,
                                                    description: timezoneMap[p]
                                                });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    },
                                    samlEnable: {
                                        label: 'label.saml.enable',
                                        docID: 'helpSamlEnable',
                                        isBoolean: true,
                                        validation: {
                                            required: false
                                        },
                                        isHidden: function (args) {
                                            if (g_idpList) return false;
                                            return true;
                                        }
                                    },
                                    samlEntity: {
                                        label: 'label.saml.entity',
                                        docID: 'helpSamlEntity',
                                        validation: {
                                            required: false
                                        },
                                        select: function(args) {
                                            var samlChecked = false;
                                            var idpUrl = args.$form.find('select[name=samlEntity]').children(':selected').val();
                                            var appendDomainToUsername = function() {
                                                if (!g_appendIdpDomain) {
                                                    return;
                                                }
                                                var username = args.$form.find('input[name=username]').val();
                                                if (username) {
                                                    username = username.split('@')[0];
                                                }
                                                if (samlChecked) {
                                                    var link = document.createElement('a');
                                                    link.setAttribute('href', idpUrl);
                                                    args.$form.find('input[name=username]').val(username + "@" + link.host.split('.').splice(-2).join('.'));
                                                } else {
                                                    args.$form.find('input[name=username]').val(username);
                                                }
                                            };
                                            args.$form.find('select[name=samlEntity]').change(function() {
                                                idpUrl = $(this).children(':selected').val();
                                                appendDomainToUsername();
                                            });
                                            args.$form.find('input[name=samlEnable]').change(function() {
                                                samlChecked = $(this).context.checked;
                                                appendDomainToUsername();
                                            });

                                            var items = [];
                                            $(g_idpList).each(function() {
                                                items.push({
                                                    id: this.id,
                                                    description: this.orgName
                                                });
                                            });
                                            args.response.success({
                                                data: items
                                            });
                                        },
                                        isHidden: function (args) {
                                            if (g_idpList) return false;
                                            return true;
                                        }
                                    }
                                }
                            },

                            action: function(args) {
                                var accountObj = args.context.accounts[0];

                                var data = {
                                    username: args.data.username
                                };

                                var password = args.data.password;
                                $.extend(data, {
                                    password: password
                                });

                                $.extend(data, {
                                    email: args.data.email,
                                    firstname: args.data.firstname,
                                    lastname: args.data.lastname
                                });

                                if (args.data.timezone != null && args.data.timezone.length > 0) {
                                    $.extend(data, {
                                        timezone: args.data.timezone
                                    });
                                }

                                $.extend(data, {
                                    domainid: accountObj.domainid,
                                    account: accountObj.name,
                                    accounttype: accountObj.accounttype
                                });


                                var authorizeUsersForSamlSSO = function (users, entity) {
                                    for (var i = 0; i < users.length; i++) {
                                        $.ajax({
                                            url: createURL('authorizeSamlSso&enable=true&userid=' + users[i].id + "&entityid=" + entity),
                                            error: function(XMLHttpResponse) {
                                                args.response.error(parseXMLHttpResponse(XMLHttpResponse));
                                            }
                                        });
                                    }
                                    return;
                                };

                                $.ajax({
                                    url: createURL('createUser'),
                                    type: "POST",
                                    data: data,
                                    success: function(json) {
                                        var item = json.createuserresponse.user;
                                        if (args.data.samlEnable && args.data.samlEnable === 'on') {
                                            var entity = args.data.samlEntity;
                                            if (item && entity)
                                                authorizeUsersForSamlSSO([item], entity);
                                        }
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
                    },

                    detailView: {
                        name: 'label.user.details',
                        isMaximized: true,
                        actions: {
                            edit: {
                                label: 'label.edit',
                                action: function(args) {
                                    var data = {
                                        id: args.context.users[0].id,
                                        username: args.data.username,
                                        email: args.data.email,
                                        firstname: args.data.firstname,
                                        lastname: args.data.lastname,
                                        timezone: args.data.timezone
                                    };
                                    $.ajax({
                                        url: createURL('updateUser'),
                                        data: data,
                                        success: function(json) {
                                            var item = json.updateuserresponse.user;
                                            args.response.success({
                                                data: item
                                            });
                                        },
                                        error: function(data) {
                                            args.response.error(parseXMLHttpResponse(data));
                                        }
                                    });

                                }
                            },

                            changePassword: {
                                label: 'label.action.change.password',
                                messages: {
                                    notification: function(args) {
                                        return 'label.action.change.password';
                                    }
                                },

                                action: {
                                    custom: function(args) {
                                        var start = args.start;
                                        var complete = args.complete;
                                        var context = args.context;

                                        var userSource = context.users[0].usersource;
                                        if (userSource == "native") {
                                            cloudStack.dialog.createForm({
                                                form: {
                                                    title: 'label.action.change.password',
                                                    fields: {
                                                        currentPassword: {
                                                            label: 'label.current.password',
                                                            isPassword: true,
                                                            validation: {
                                                                required: !(isAdmin() || isDomainAdmin())
                                                            },
                                                            id: 'currentPassword'
                                                        },
                                                        newPassword: {
                                                            label: 'label.new.password',
                                                            isPassword: true,
                                                            validation: {
                                                                required: true
                                                            },
                                                            id: 'newPassword'
                                                        },
                                                        'password-confirm': {
                                                            label: 'label.confirm.password',
                                                            validation: {
                                                                required: true,
                                                                equalTo: '#newPassword'
                                                            },
                                                            isPassword: true
                                                        }
                                                    }
                                                },
                                                after: function(args) {
                                                    start();
                                                    var currentPassword = args.data.currentPassword;
                                                    var password = args.data.newPassword;
                                                    $.ajax({
                                                        url: createURL('updateUser'),
                                                        data: {
                                                            id: context.users[0].id,
                                                            currentPassword: currentPassword,
                                                            password: password
                                                        },
                                                        type: "POST",
                                                        success: function(json) {
                                                            complete();
                                                        },
                                                        error: function(json) {
                                                            complete({ error: parseXMLHttpResponse(json) });
                                                        }
                                                    });
                                                }
                                            });
                                            if(isAdmin() || isDomainAdmin()){
                                                $('div[rel=currentPassword]').hide();
                                            }
                                        } else {
                                            cloudStack.dialog.notice({ message: _l('error.could.not.change.your.password.because.non.native.user') });
                                        }
                                    }
                                }
                            },

                            configureSamlAuthorization: {
                                label: 'label.action.configure.samlauthorization',
                                messages: {
                                    notification: function(args) {
                                        return 'label.action.configure.samlauthorization';
                                    }
                                },
                                action: {
                                    custom: function(args) {
                                        var start = args.start;
                                        var complete = args.complete;
                                        var context = args.context;

                                        if (g_idpList) {
                                            $.ajax({
                                                url: createURL('listSamlAuthorization'),
                                                data: {
                                                    userid: context.users[0].id
                                                },
                                                success: function(json) {
                                                    var authorization = json.listsamlauthorizationsresponse.samlauthorization[0];
                                                    cloudStack.dialog.createForm({
                                                        form: {
                                                            title: 'label.action.configure.samlauthorization',
                                                            fields: {
                                                                samlEnable: {
                                                                    label: 'label.saml.enable',
                                                                    docID: 'helpSamlEnable',
                                                                    isBoolean: true,
                                                                    isChecked: authorization.status,
                                                                    validation: {
                                                                        required: false
                                                                    }
                                                                },
                                                                samlEntity: {
                                                                    label: 'label.saml.entity',
                                                                    docID: 'helpSamlEntity',
                                                                    validation: {
                                                                        required: false
                                                                    },
                                                                    select: function(args) {
                                                                        var items = [];
                                                                        $(g_idpList).each(function() {
                                                                            items.push({
                                                                                id: this.id,
                                                                                description: this.orgName
                                                                            });
                                                                        });
                                                                        args.response.success({
                                                                            data: items
                                                                        });
                                                                        args.$select.change(function() {
                                                                            $('select[name="samlEntity"] option[value="' + authorization.idpid  + '"]').attr("selected", "selected");
                                                                        });
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        after: function(args) {
                                                            start();
                                                            var enableSaml = false;
                                                            var idpId = '';
                                                            if (args.data.hasOwnProperty('samlEnable')) {
                                                                enableSaml = (args.data.samlEnable === 'on');
                                                            }
                                                            if (args.data.hasOwnProperty('samlEntity')) {
                                                                idpId = args.data.samlEntity;
                                                            }
                                                            $.ajax({
                                                                url: createURL('authorizeSamlSso'),
                                                                data: {
                                                                    userid: context.users[0].id,
                                                                    enable: enableSaml,
                                                                    entityid: idpId
                                                                },
                                                                type: "POST",
                                                                success: function(json) {
                                                                    complete();
                                                                },
                                                                error: function(json) {
                                                                    complete({ error: parseXMLHttpResponse(json) });
                                                                }
                                                            });
                                                        }
                                                    });
                                                },
                                                error: function(json) {
                                                    complete({ error: parseXMLHttpResponse(json) });
                                                }
                                            });

                                        }
                                    }
                                }
                            },

                            generateKeys: {
                                label: 'label.action.generate.keys',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.generate.keys';
                                    },
                                    notification: function(args) {
                                        return 'label.action.generate.keys';
                                    }
                                },
                                action: function(args) {
                                    var data = {
                                        id: args.context.users[0].id
                                    };
                                    $.ajax({
                                        url: createURL('registerUserKeys'),
                                        data: data,
                                        success: function(json) {
                                            args.response.success({
                                                data: json.registeruserkeysresponse.userkeys
                                            });
                                        }
                                    });
                                },
                                notification: {
                                    poll: function(args) {
                                        args.complete();
                                    }
                                }
                            },

                            disable: {
                                label: 'label.action.disable.user',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.disable.user';
                                    },
                                    notification: function(args) {
                                        return 'label.action.disable.user';
                                    }
                                },
                                action: function(args) {
                                    var data = {
                                        id: args.context.users[0].id
                                    };
                                    $.ajax({
                                        url: createURL('disableUser'),
                                        data: data,
                                        success: function(json) {
                                            var jid = json.disableuserresponse.jobid;
                                            args.response.success({
                                                _custom: {
                                                    jobId: jid,
                                                    getUpdatedItem: function(json) {
                                                        return json.queryasyncjobresultresponse.jobresult.user;
                                                    },
                                                    getActionFilter: function() {
                                                        return userActionfilter;
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

                            enable: {
                                label: 'label.action.enable.user',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.enable.user';
                                    },
                                    notification: function(args) {
                                        return 'label.action.enable.user';
                                    }
                                },
                                action: function(args) {
                                    var data = {
                                        id: args.context.users[0].id
                                    };
                                    $.ajax({
                                        url: createURL('enableUser'),
                                        data: data,
                                        success: function(json) {
                                            args.response.success({
                                                data: json.enableuserresponse.user
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
                            },

                            remove: {
                                label: 'label.action.delete.user',
                                messages: {
                                    confirm: function(args) {
                                        return 'message.delete.user';
                                    },
                                    notification: function(args) {
                                        return 'label.action.delete.user';
                                    }
                                },
                                action: function(args) {
                                    var data = {
                                        id: args.context.users[0].id
                                    };
                                    $.ajax({
                                        url: createURL('deleteUser'),
                                        data: data,
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
                            }
                        },
                        tabs: {
                            details: {
                                title: 'label.details',

                                fields: [{
                                    username: {
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
                                    state: {
                                        label: 'label.state'
                                    },
                                    account: {
                                        label: 'label.account.name'
                                    },
                                    rolename: {
                                        label: 'label.role'
                                    },
                                    roletype: {
                                        label: 'label.roletype'
                                    },
                                    domain: {
                                        label: 'label.domain'
                                    },
                                    apikey: {
                                        label: 'label.api.key',
                                        isCopyPaste: true
                                    },
                                    secretkey: {
                                        label: 'label.secret.key',
                                        isCopyPaste: true
                                    },
                                    email: {
                                        label: 'label.email',
                                        isEditable: true,
                                        validation: {
                                            required: true,
                                            email: true
                                        }
                                    },
                                    firstname: {
                                        label: 'label.first.name',
                                        isEditable: true,
                                        validation: {
                                            required: true
                                        }
                                    },
                                    lastname: {
                                        label: 'label.last.name',
                                        isEditable: true,
                                        validation: {
                                            required: true
                                        }
                                    },
                                    timezone: {
                                        label: 'label.timezone',
                                        converter: function(args) {
                                            if (args == null || args.length == 0)
                                                return "";
                                            else
                                                return args;
                                        },
                                        isEditable: true,
                                        select: function(args) {
                                            var items = [];
                                            items.push({
                                                id: "",
                                                description: ""
                                            });
                                            for (var p in timezoneMap)
                                                items.push({
                                                    id: p,
                                                    description: timezoneMap[p]
                                                });
                                            args.response.success({
                                                data: items
                                            });
                                        }
                                    }
                                }],

                                dataProvider: function(args) {
                                if (isAdmin() || isDomainAdmin()) {
                                    $.ajax({
                                        url: createURL('listUsers'),
                                            data: {
                                                id: args.context.users[0].id
                                            },
                                            success: function(json) {
                                                var items = json.listusersresponse.user[0];
                                                    $.ajax({
                                                        url: createURL('getUserKeys'),//change
                                                        data: {
                                                            id: args.context.users[0].id//change
                                                        },
                                                        success: function(json) {
                                                            $.extend(items, {
                                                                secretkey: json.getuserkeysresponse.userkeys.secretkey//change
                                                            });
                                                        args.response.success({
                                                            actionFilter: userActionfilter,
                                                                data: items
                                                        });
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    else { //normal user doesn't have access listUsers API until Bug 14127 is fixed.
                                        args.response.success({
                                            actionFilter: userActionfilter,
                                            data: args.context.users[0]
                                        });
                                    }
                                }
                            }
                        }
                    }
                }
            },
            sshkeypairs: {
                type: 'select',
                id: 'sshkeypairs',
                title: 'label.ssh.key.pairs',
                listView: {
                    name: 'sshkeypairs',
                    fields: {
                        name: {
                            label: 'label.name'
                        },
                        domain: {
                           label: 'label.domain'
                        },
                        account: {
                           label: 'label.account'
                        },
                        privatekey: {
                            label: 'label.private.key',
                            span: false
                        }
                    },
                    dataProvider: function(args) {
                        var data = {
//                            domainid: g_domainid,
//                            account: g_account
                        };

                        listViewDataProvider(args, data);

                        $.ajax({
                            url: createURL('listSSHKeyPairs'),
                            data: data,
                            success: function(json) {
                                var items = json.listsshkeypairsresponse.sshkeypair;
                                args.response.success({
                                    data: items
                                });
                            }
                        });
                    },
                    actions: {
                        add: {
                            label: 'label.create.ssh.key.pair',

                            preFilter: function(args) {
                                return true;
                            },

                            messages: {
                                notification: function(args) {
                                    return _l('message.desc.created.ssh.key.pair');
                                }
                            },

                            createForm: {
                                title: 'label.create.ssh.key.pair',
                                desc: 'message.desc.create.ssh.key.pair',
                                fields: {
                                    name: {
                                        label: 'label.name',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    publickey: {
                                        label: 'label.public.key'
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
                                                    url: createURL('listDomains'),
                                                    data: {
                                                        listAll: true,
                                                        details: 'min'
                                                    },
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
                                                var items = [];
                                                items.push({
                                                    id: "",
                                                    description: ""
                                                });
                                                args.response.success({
                                                    data: items
                                                });
                                            }
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
                                    }
                                }
                            },

                            action: function(args) {

                                var data = {
                                    name: args.data.name
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

                                if (args.data.publickey != null && args.data.publickey.length > 0) {
                                    $.extend(data, {
                                        publickey: args.data.publickey
                                    });
                                    $.ajax({
                                        url: createURL('registerSSHKeyPair'),
                                        data: data,
                                        type: "POST",
                                        success: function(json) {
                                            var item = json.registersshkeypairresponse.keypair;
                                            args.response.success({
                                                data: item
                                            });
                                        },
                                        error: function(XMLHttpResponse) {
                                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                            args.response.error(errorMsg);
                                        }
                                    });
                                } else {
                                    $.ajax({
                                        url: createURL('createSSHKeyPair'),
                                        data: data,
                                        success: function(json) {
                                            var item = json.createsshkeypairresponse.keypair;
                                            args.response.success({
                                                data: item
                                            });
                                        },
                                        error: function(XMLHttpResponse) {
                                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                            args.response.error(errorMsg);
                                        }
                                    });
                                }
                            },

                            notification: {
                                poll: function(args) {
                                    args.complete();
                                }
                            }
                        }
                    },

                    detailView: {
                        name: 'label.ssh.key.pair.details',
                        isMaximized: true,
                        viewAll: {
                            label: 'label.instances',
                            path: 'instances'
                        },
                        actions: {
                            remove: {
                                label: 'label.remove.ssh.key.pair',
                                messages: {
                                    confirm: function(args) {
                                        return _l('message.please.confirm.remove.ssh.key.pair');
                                    },
                                    notification: function(args) {
                                        return _l('message.removed.ssh.key.pair');
                                    }
                                },
                                action: function(args) {
                                    var data = {
                                        name: args.context.sshkeypairs[0].name
                                    };
                                    if (!args.context.projects) {
                                        $.extend(data, {
                                            domainid: args.context.sshkeypairs[0].domainid,
                                            account: args.context.sshkeypairs[0].account
                                        });
                                    }
                                    $.ajax({
                                        url: createURL('deleteSSHKeyPair'),
                                        data: data,
                                        success: function(json) {
                                            args.response.success();
                                            $(window).trigger('cloudStack.fullRefresh');
                                        }
                                    });
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
                                    domain: {
                                        label: 'label.domain'
                                    },
                                    account: {
                                        label: 'label.account'
                                    },
                                    privatekey: {
                                        label: 'label.private.key',
                                        span: false
                                    },
                                    fingerprint: {
                                        label: 'label.fingerprint'
                                    }
                                }],

                                dataProvider: function(args) {
                                    var data = {
                                        name: args.context.sshkeypairs[0].name
                                    };
                                    $.ajax({
                                        url: createURL('listSSHKeyPairs&listAll=true'),
                                        data: data,
                                        success: function(json) {
                                            args.response.success({
                                                actionFilter: sshkeypairActionfilter,
                                                data: json.listsshkeypairsresponse.sshkeypair[0]
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

    var accountActionfilter = function(args) {
        var jsonObj = args.context.item;
        var allowedActions = [];

        if (jsonObj.state == 'Destroyed') return [];

        if (isAdmin() && jsonObj.isdefault == false)
            allowedActions.push("remove");

        if (isAdmin()) {
            allowedActions.push("edit"); //updating networkdomain is allowed on any account, including system-generated default admin account
            if (!(jsonObj.domain == "ROOT" && jsonObj.name == "admin" && jsonObj.accounttype == 1)) { //if not system-generated default admin account
                if (jsonObj.state == "enabled") {
                    allowedActions.push("disable");
                    allowedActions.push("lock");
                } else if (jsonObj.state == "disabled" || jsonObj.state == "locked") {
                    allowedActions.push("enable");
                }
                allowedActions.push("remove");
            }
            allowedActions.push("updateResourceCount");
        } else if (isDomainAdmin()) {
            if (jsonObj.name != g_account) {
                allowedActions.push("edit"); //updating networkdomain is allowed on any account, including system-generated default admin account
                if (jsonObj.state == "enabled") {
                    allowedActions.push("disable");
                    allowedActions.push("lock");
                } else if (jsonObj.state == "disabled" || jsonObj.state == "locked") {
                    allowedActions.push("enable");
                }
                allowedActions.push("remove");
            }
            allowedActions.push("updateResourceCount");
        }
        return allowedActions;
    }

    var userActionfilter = function(args) {
        var jsonObj = args.context.item;
        var allowedActions = [];

        if (isAdmin() && jsonObj.isdefault == false)
            allowedActions.push("remove");

        if (isAdmin()) {
            allowedActions.push("edit");
            allowedActions.push("changePassword");
            allowedActions.push("generateKeys");
            if (g_idpList) {
                allowedActions.push("configureSamlAuthorization");
            }
            if (!(jsonObj.domain == "ROOT" && jsonObj.account == "admin" && jsonObj.accounttype == 1)) { //if not system-generated default admin account user
                if (jsonObj.state == "enabled")
                    allowedActions.push("disable");
                if (jsonObj.state == "disabled")
                    allowedActions.push("enable");
                allowedActions.push("remove");
            }
        } else { //domain-admin, regular-user
            if (jsonObj.username == g_username) { //selected user is self
                allowedActions.push("changePassword");
                allowedActions.push("generateKeys");
            } else if (isDomainAdmin()) { //if selected user is not self, and the current login is domain-admin
                allowedActions.push("edit");
                if (jsonObj.state == "enabled")
                    allowedActions.push("disable");
                if (jsonObj.state == "disabled")
                    allowedActions.push("enable");
                allowedActions.push("remove");

                allowedActions.push("changePassword");
                allowedActions.push("generateKeys");
                if (g_idpList) {
                    allowedActions.push("configureSamlAuthorization");
                }
            }
        }
        return allowedActions;
    }

    var sshkeypairActionfilter = function(args) {
        var allowedActions = [];
        allowedActions.push("remove");
        return allowedActions;
    }
})(cloudStack);
