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
    cloudStack.sections.domains = {
        title: 'label.menu.domains',
        id: 'domains',

        // Domain tree
        treeView: {
        overflowScroll: true,
            // Details
            detailView: {
                name: 'label.domain.details',
                viewAll: {
                    label: 'label.accounts',
                    path: 'accounts'
                },

                // Detail actions
                actions: {
                    'delete': {
                        label: 'label.action.delete.domain',
                        messages: {
                            notification: function(args) {
                                return 'label.action.delete.domain';
                            }
                        },

                        createForm: {
                            title: 'label.action.delete.domain',
                            desc: 'message.action.delete.domain',
                            createLabel: 'label.delete',
                            preFilter: function(args) {
                                if (isAdmin()) {
                                    args.$form.find('.form-item[rel=isForced]').css('display', 'inline-block');
                                }
                            },
                            fields: {
                                isForced: {
                                    label: 'force.delete',
                                    isBoolean: true,
                                    isHidden: true
                                }
                            }
                        },

                        action: function(args) {
                            var array1 = [];
                            if (args.$form.find('.form-item[rel=isForced]').css("display") != "none") //uncomment after Brian fix it to include $form in args
                                array1.push("&cleanup=" + (args.data.isForced == "on"));

                            $.ajax({
                                url: createURL("deleteDomain&id=" + args.context.domains[0].id + array1.join("")),
                                dataType: "json",
                                async: false,
                                success: function(json) {
                                    var jid = json.deletedomainresponse.jobid;
                                    args.response.success({
                                        _custom: {
                                            jobId: jid
                                        }
                                    });

                                    // Quick fix for proper UI reaction to delete domain
                                    var $item = $('.name.selected').closest('li');
                                    var $itemParent = $item.closest('li');
                                    $itemParent.parent().parent().find('.name:first').click();
                                    $item.remove();
                                }
                            });
                        },
                        notification: {
                            poll: pollAsyncJobResult
                        }
                    },

                    // Edit domain
                    edit: {
                        label: 'label.action.edit.domain',
                        messages: {
                            notification: function(args) {
                                return 'label.action.edit.domain';
                            }
                        },
                        action: function(args) {
                            var domainObj;

                            var data = {
                                id: args.context.domains[0].id
                            };

                            if (args.data.name != null) { //args.data.name == undefined means name field is not editable (when log in as normal user or domain admin)
                                $.extend(data, {
                                    name: args.data.name
                                });
                            }

                            if (args.data.networkdomain != null) { //args.data.networkdomain == undefined means networkdomain field is not editable (when log in as normal user or domain admin)
                                $.extend(data, {
                                    networkdomain: args.data.networkdomain
                                });
                            }

                            if('name' in data || 'networkdomain' in data)  {
                                $.ajax({
                                    url: createURL("updateDomain"),
                                    async: false,
                                    data: data,
                                    success: function(json) {
                                        domainObj = json.updatedomainresponse.domain;
                                    }
                                });
                            }

                            if (args.data.vmLimit != null) {
                                $.ajax({
                                    url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=0&max=" + args.data.vmLimit),
                                    dataType: "json",
                                    async: false,
                                    success: function(json) {
                                        domainObj["vmLimit"] = args.data.vmLimit;
                                    }
                                });
                            }

                            if (args.data.ipLimit != null) {
                                $.ajax({
                                    url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=1&max=" + args.data.ipLimit),
                                    dataType: "json",
                                    async: false,
                                    success: function(json) {
                                        domainObj["ipLimit"] = args.data.ipLimit;
                                    }
                                });
                            }

                            if (args.data.volumeLimit != null) {
                                $.ajax({
                                    url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=2&max=" + args.data.volumeLimit),
                                    dataType: "json",
                                    async: false,
                                    success: function(json) {
                                        domainObj["volumeLimit"] = args.data.volumeLimit;
                                    }
                                });
                            }

                            if (args.data.snapshotLimit != null) {
                                $.ajax({
                                    url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=3&max=" + args.data.snapshotLimit),
                                    dataType: "json",
                                    async: false,
                                    success: function(json) {
                                        domainObj["snapshotLimit"] = args.data.snapshotLimit;
                                    }
                                });
                            }

                            if (args.data.templateLimit != null) {
                                $.ajax({
                                    url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=4&max=" + args.data.templateLimit),
                                    dataType: "json",
                                    async: false,
                                    success: function(json) {
                                        domainObj["templateLimit"] = args.data.templateLimit;
                                    }
                                });
                            }

                            if (args.data.vpcLimit != null) {
                                $.ajax({
                                    url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=7&max=" + args.data.vpcLimit),
                                    dataType: "json",
                                    async: false,
                                    success: function(json) {
                                        domainObj["vpcLimit"] = args.data.vpcLimit;
                                    }
                                });
                            }

                            if (args.data.cpuLimit != null) {
                                $.ajax({
                                    url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=8&max=" + args.data.cpuLimit),
                                    dataType: "json",
                                    async: false,
                                    success: function(json) {
                                        domainObj["cpuLimit"] = args.data.cpuLimit;
                                    }
                                });
                            }

                            if (args.data.memoryLimit != null) {
                                $.ajax({
                                    url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=9&max=" + args.data.memoryLimit),
                                    dataType: "json",
                                    async: false,
                                    success: function(json) {
                                        domainObj["memoryLimit"] = args.data.memoryLimit;
                                    }
                                });
                            }

                            if (args.data.networkLimit != null) {
                                $.ajax({
                                    url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=6&max=" + args.data.networkLimit),
                                    dataType: "json",
                                    async: false,
                                    success: function(json) {
                                        domainObj["networkLimit"] = args.data.networkLimit;
                                    }
                                });
                            }

                            if (args.data.primaryStorageLimit != null) {
                                $.ajax({
                                    url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=10&max=" + args.data.primaryStorageLimit),
                                    dataType: "json",
                                    async: false,
                                    success: function(json) {
                                        domainObj["primaryStorageLimit"] = args.data.primaryStorageLimit;
                                    }
                                });
                            }

                            if (args.data.secondaryStorageLimit != null) {
                                $.ajax({
                                    url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=11&max=" + args.data.secondaryStorageLimit),
                                    dataType: "json",
                                    async: false,
                                    success: function(json) {
                                        domainObj["secondaryStorageLimit"] = args.data.secondaryStorageLimit;
                                    }
                                });
                            }

                            args.response.success({
                                data: domainObj
                            });
                        }
                    },

                    // Add domain
                    create: {
                        label: 'label.add.domain',

                        action: function(args) {
                            var data = {
                                parentdomainid: args.context.domains[0].id,
                                name: args.data.name
                            };

                            if (args.data.networkdomain != null && args.data.networkdomain.length > 0) {
                                $.extend(data, {
                                    networkdomain: args.data.networkdomain
                                });
                            }

                            $.ajax({
                                url: createURL('createDomain'),
                                data: data,
                                success: function(json) {
                                    var item = json.createdomainresponse.domain;
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

                        messages: {
                            notification: function(args) {
                                return 'label.add.domain';
                            }
                        },

                        createForm: {
                            title: 'label.add.domain',
                            desc: 'message.add.domain',
                            fields: {
                                name: {
                                    label: 'label.name',
                                    docID: 'helpDomainName',
                                    validation: {
                                        required: true
                                    }
                                },
                                networkdomain: {
                                    label: 'label.network.domain',
                                    docID: 'helpDomainNetworkDomain',
                                    validation: {
                                        required: false
                                    }
                                }
                            }
                        }
                    },

                    linktoldap: {
                            label: 'label.link.domain.to.ldap',

                            action: function(args) {
                                var data = {
                                    domainid: args.context.domains[0].id,
                                    type: args.data.type,
                                    name: args.data.name,
                                    accounttype: args.data.accounttype
                                };

                                if (args.data.admin != null && args.data.admin.length > 0) {
                                    $.extend(data, {
                                        admin: args.data.admin
                                    });
                                }

                                $.ajax({
                                    url: createURL('linkDomainToLdap'),
                                    data: data,
                                    success: function(json) {
                                        var item = json.linkdomaintoldapresponse.LinkDomainToLdap.domainid;
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

                            messages: {
                                notification: function(args) {
                                    return 'label.link.domain.to.ldap';
                                }
                            },

                            createForm: {
                                title: 'label.link.domain.to.ldap',
                                desc: 'message.link.domain.to.ldap',
                                fields: {
                                    type: {
                                        label: 'label.ldap.link.type',
                                        docID: 'helpLdapGroupType',
                                        validation: {
                                            required: true
                                        },
                                        select: function(args) {
                                             var items = [];
                                             items.push({
                                                 id: "GROUP",
                                                 description: "GROUP"
                                             }); //regular-user
                                             items.push({
                                                 id: "OU",
                                                 description: "OU"
                                             }); //root-admin
                                             args.response.success({
                                                 data: items
                                             });
                                        }
                                    },
                                    name: {
                                        label: 'label.name',
                                        docID: 'helpLdapGroupName',
                                        validation: {
                                            required: true
                                        }
                                    },
                                    accounttype: {
                                        label: 'label.account.type',
                                        docID: 'helpAccountType',
                                        validation: {
                                            required: true
                                        },
                                        select: function(args) {
                                             var items = [];
                                             items.push({
                                                 id: 0,
                                                 description: "Normal User"
                                             }); //regular-user
                                             items.push({
                                                 id: 2,
                                                 description: "Domain Admin"
                                             }); //root-admin
                                             args.response.success({
                                                 data: items
                                             });
                                        }
                                    },
                                    admin: {
                                        label: 'label.domain.admin',
                                        docID: 'helpLdapLinkDomainAdmin',
                                        validation: {
                                            required: false
                                        }
                                    }
                                }
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
                            $.ajax({
                                url: createURL("updateResourceCount&domainid=" + args.context.domains[0].id),
                                dataType: "json",
                                async: true,
                                success: function(json) {
                                    //var resourcecounts= json.updateresourcecountresponse.resourcecount;   //do nothing
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
                            name: {
                                label: 'label.name',
                                isEditable: function(args) {
                                    if (isAdmin() && args.domains[0].level != 0) //ROOT domain (whose level is 0) is not allowed to change domain name
                                        return true;
                                    else
                                        return false;
                                }
                            }
                        }, {
                            id: {
                                label: 'label.id'
                            },

                            path: {
                                label: 'label.full.path'
                            },

                            networkdomain: {
                                label: 'label.network.domain',
                                isEditable: function(args) {
                                    if (isAdmin())
                                        return true;
                                    else
                                        return false;
                                }
                            },
                            vmLimit: {
                                label: 'label.instance.limits',
                                isEditable: function(args) {
                                    if (args.domains[0].id == g_domainid) //disallow to update the field on the domain you log in as
                                        return false;
                                    else
                                        return true;
                                }
                            },
                            ipLimit: {
                                label: 'label.ip.limits',
                                isEditable: function(args) {
                                    if (args.domains[0].id == g_domainid) //disallow to update the field on the domain you log in as
                                        return false;
                                    else
                                        return true;
                                }
                            },
                            volumeLimit: {
                                label: 'label.volume.limits',
                                isEditable: function(args) {
                                    if (args.domains[0].id == g_domainid) //disallow to update the field on the domain you log in as
                                        return false;
                                    else
                                        return true;
                                }
                            },
                            snapshotLimit: {
                                label: 'label.snapshot.limits',
                                isEditable: function(args) {
                                    if (args.domains[0].id == g_domainid) //disallow to update the field on the domain you log in as
                                        return false;
                                    else
                                        return true;
                                }
                            },
                            templateLimit: {
                                label: 'label.template.limits',
                                isEditable: function(args) {
                                    if (args.domains[0].id == g_domainid) //disallow to update the field on the domain you log in as
                                        return false;
                                    else
                                        return true;
                                }
                            },
                            vpcLimit: {
                                label: 'label.VPC.limits',
                                isEditable: function(args) {
                                    if (args.domains[0].id == g_domainid) //disallow to update the field on the domain you log in as
                                        return false;
                                    else
                                        return true;
                                }
                            },
                            cpuLimit: {
                                label: 'label.cpu.limits',
                                isEditable: function(args) {
                                    if (args.domains[0].id == g_domainid) //disallow to update the field on the domain you log in as
                                        return false;
                                    else
                                        return true;
                                }
                            },
                            memoryLimit: {
                                label: 'label.memory.limits',
                                isEditable: function(args) {
                                    if (args.domains[0].id == g_domainid) //disallow to update the field on the domain you log in as
                                        return false;
                                    else
                                        return true;
                                }
                            },
                            networkLimit: {
                                label: 'label.network.limits',
                                isEditable: function(args) {
                                    if (args.domains[0].id == g_domainid) //disallow to update the field on the domain you log in as
                                        return false;
                                    else
                                        return true;
                                }
                            },
                            primaryStorageLimit: {
                                label: 'label.primary.storage.limits',
                                isEditable: function(args) {
                                    if (args.domains[0].id == g_domainid) //disallow to update the field on the domain you log in as
                                        return false;
                                    else
                                        return true;
                                }
                            },
                            secondaryStorageLimit: {
                                label: 'label.secondary.storage.limits',
                                isEditable: function(args) {
                                    if (args.domains[0].id == g_domainid) //disallow to update the field on the domain you log in as
                                        return false;
                                    else
                                        return true;
                                }
                            },
                            accountTotal: {
                                label: 'label.accounts'
                            },
                            vmTotal: {
                                label: 'label.instances'
                            },
                            volumeTotal: {
                                label: 'label.volumes'
                            }
                        }],
                        dataProvider: function(args) {
                            var domainObj = args.context.domains[0];
                            var totalVMs = 0;
                            var totalVolumes = 0;

                            $.ajax({
                                url: createURL("listAccounts&domainid=" + domainObj.id),
                                async: false,
                                data: {},
                                success: function(json) {
                                    var items = json.listaccountsresponse.account;
                                    if (items != null) {
                                        domainObj["accountTotal"] = items.length;
                                        for (var i = 0; i < items.length; i++) {
                                            totalVMs += items[i].vmtotal;
                                            totalVolumes += items[i].volumetotal;
                                        }
                                    } else {
                                        domainObj["accountTotal"] = 0;
                                    }
                                }
                            });

                            $.ajax({
                                url: createURL("listProjects&domainid=" + domainObj.id),
                                async: false,
                                data: {},
                                success: function(json) {
                                    var items = json.listprojectsresponse.project;
                                    if (items != null) {
                                        for (var i = 0; i < items.length; i++) {
                                            totalVMs += items[i].vmtotal;
                                            totalVolumes += items[i].volumetotal;
                                        }
                                    }
                                }
                            });

                            domainObj["vmTotal"] = totalVMs;
                            domainObj["volumeTotal"] = totalVolumes;

                            /* $.ajax({
                url: createURL("listVirtualMachines&details=min&domainid=" + domainObj.id),
                async: false,
                dataType: "json",
                success: function(json) {
                  var items = json.listvirtualmachinesresponse.virtualmachine;
                  var total;
                  if (items != null)
                    total = items.length;
                  else
                    total = 0;
                  domainObj["vmTotal"] = total;
                }
              });

              $.ajax({
                url: createURL("listVolumes&domainid=" + domainObj.id),
                async: false,
                dataType: "json",
                success: function(json) {
                  var items = json.listvolumesresponse.volume;
                  var total;
                  if (items != null)
                    total = items.length;
                  else
                    total = 0;
                  domainObj["volumeTotal"] = total;
                }
              });*/

                            $.ajax({
                                url: createURL("listResourceLimits&domainid=" + domainObj.id),
                                async: false,
                                dataType: "json",
                                success: function(json) {
                                    var limits = json.listresourcelimitsresponse.resourcelimit;
                                    if (limits != null) {
                                        for (var i = 0; i < limits.length; i++) {
                                            var limit = limits[i];
                                            switch (limit.resourcetype) {
                                                case "0":
                                                    domainObj["vmLimit"] = limit.max;
                                                    break;
                                                case "1":
                                                    domainObj["ipLimit"] = limit.max;
                                                    break;
                                                case "2":
                                                    domainObj["volumeLimit"] = limit.max;
                                                    break;
                                                case "3":
                                                    domainObj["snapshotLimit"] = limit.max;
                                                    break;
                                                case "4":
                                                    domainObj["templateLimit"] = limit.max;
                                                    break;
                                                case "6":
                                                    domainObj["networkLimit"] = limit.max;
                                                    break;
                                                case "7":
                                                    domainObj["vpcLimit"] = limit.max;
                                                    break;
                                                case "8":
                                                    domainObj["cpuLimit"] = limit.max;
                                                    break;
                                                case "9":
                                                    domainObj["memoryLimit"] = limit.max;
                                                    break;
                                                case "10":
                                                    domainObj["primaryStorageLimit"] = limit.max;
                                                    break;
                                                case "11":
                                                    domainObj["secondaryStorageLimit"] = limit.max;
                                                    break;
                                            }
                                        }
                                    }
                                }
                            });

                            args.response.success({
                                data: domainObj,
                                actionFilter: domainActionfilter
                            });
                        }
                    }
                }
            },
            labelField: 'name',
            dataProvider: function(args) {
                var parentDomain = args.context.parentDomain;
                if (parentDomain == null) { //draw root node
                    $.ajax({
                        url: createURL("listDomains&id=" + g_domainid + '&listAll=true'),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                            var domainObjs = json.listdomainsresponse.domain;
                            if (domainObjs != null && domainObjs.length > 0) {
                                domainObjs.sort(function(a, b) {
                                    return a.name.localeCompare(b.name);
                                });
                            }
                            args.response.success({
                                actionFilter: domainActionfilter,
                                data: domainObjs
                            });
                        }
                    });
                } else {
                    $.ajax({
                        url: createURL("listDomainChildren&id=" + parentDomain.id),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                            var domainObjs = json.listdomainchildrenresponse.domain;
                            if (domainObjs != null && domainObjs.length > 0) {
                                domainObjs.sort(function(a, b) {
                                    return a.name.localeCompare(b.name);
                                });
                            }
                            args.response.success({
                                actionFilter: domainActionfilter,
                                data: domainObjs
                            });
                        }
                    });
                }
            }
        }
    };

    var domainActionfilter = function(args) {
        var jsonObj = args.context.item;
        var allowedActions = [];
        if (isAdmin()) {
            allowedActions.push("create");
            allowedActions.push("edit"); //merge updateResourceLimit into edit
            if (jsonObj.level != 0) { //ROOT domain (whose level is 0) is not allowed to delete
                allowedActions.push("delete");
            }
            if(isLdapEnabled()) {
                allowedActions.push("linktoldap")
            }
        } else if (isDomainAdmin()) {
            if (args.context.domains[0].id != g_domainid) {
                allowedActions.push("edit"); //merge updateResourceLimit into edit
            }
        }
        allowedActions.push("updateResourceCount");
        return allowedActions;
    }

})(cloudStack);
