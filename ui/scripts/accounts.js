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
	var rootDomainId;

  cloudStack.sections.accounts = {
    title: 'label.accounts',
    id: 'accounts',
    sectionSelect: {
      label: 'Select View',
      preFilter: function() {
        return ['accounts'];
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
            name: { label: 'label.name' },
            accounttype: {
              label: 'label.role',
              converter: function(args){
                return cloudStack.converters.toRole(args);
              }
            },
            domain: { label: 'label.domain' },
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
                if(isAdmin())
                  return true;
                else
                  return false;
              },
              messages: {
                notification: function(args) {
                  return 'label.add.account';
                }
              },

              createForm: {
                title: 'label.add.account',
                desc: 'label.add.account',
                fields: {
                  username: {
                    label: 'label.username',
                    validation: { required: true },
                    docID: 'helpAccountUsername'
                  },
                  password: {
                    label: 'label.password',
                    validation: { required: true },
                    isPassword: true,
                    id: 'password',
                    docID: 'helpAccountPassword'
                  },
                  'password-confirm': {
                    label: 'label.confirm.password',
                    validation: {
                      required: true,
                      equalTo: '#password'
                    },
                    isPassword: true,
                    docID: 'helpAccountConfirmPassword'
                  },
                  email: {
                    label: 'label.email',
                    validation: { required: true, email:true },
                    docID: 'helpAccountEmail'
                  },
                  firstname: {
                    label: 'label.first.name',
                    validation: { required: true },
                    docID: 'helpAccountFirstName'
                  },
                  lastname: {
                    label: 'label.last.name',
                    validation: { required: true },
                    docID: 'helpAccountLastName'
                  },                  
                  domainid: {
                    label: 'label.domain',
                    docID: 'helpAccountDomain',
                    validation: { required: true },
                    select: function(args) {
                      var data = {};

                      if (args.context.users) { // In accounts section
                        data.listAll = true;
                      } else if (args.context.domains) { // In domain section (use specific domain)
                        data.id = args.context.domains[0].id;
                      }

                      $.ajax({
                        url: createURL("listDomains"),
                        data: data,
                        dataType: "json",
                        async: false,
                        success: function(json) {
                          var items = [];
                          domainObjs = json.listdomainsresponse.domain;
                          $(domainObjs).each(function() {
                            items.push({ id: this.id, description: this.path });

                            if(this.level == 0)
                              rootDomainId = this.id;
                          });
                          args.response.success({ data: items });
                        }
                      });
                    }
                  },
                  account: {
                    label: 'label.account',
                    docID: 'helpAccountAccount'
                  },
                  accounttype: {
                    label: 'label.type',
                    docID: 'helpAccountType',
                    validation: { required: true },
                    select: function(args) {
                      var items = [];
                      items.push({id:0, description: "User"});  //regular-user
                      items.push({id:1, description: "Admin"}); //root-admin
                      args.response.success({data: items});
                    }
                  },
                  timezone: {
                    label: 'label.timezone',
                    docID: 'helpAccountTimezone',
                    select: function(args) {
                      var items = [];
                      items.push({id: "", description: ""});
                      for(var p in timezoneMap)
                        items.push({id: p, description: timezoneMap[p]});
                      args.response.success({data: items});
                    }
                  },
                  networkdomain: {
                    label: 'label.network.domain',
                    docID: 'helpAccountNetworkDomain',
                    validation: { required: false }
                  }
                }
              },

              action: function(args) {
                var data = {
								  username: args.data.username,
								};															               
               
                var password = args.data.password;
                if (md5Hashed) {
                  password = $.md5(password);		
                }									
								$.extend(data, {
                  password: password
                });								
								
                $.extend(data, {
								  email: args.data.email,
                  firstname: args.data.firstname,
                  lastname: args.data.lastname,                 
                  domainid: args.data.domainid									
								});								              

                var account = args.data.account;
                if(account == null || account.length == 0) {
                  account = args.data.username;
								}
								$.extend(data, {
								  account: account
								});
               
                var accountType = args.data.accounttype;							
                if (args.data.accounttype == "1" && args.data.domainid != rootDomainId) { //if account type is admin, but domain is not Root domain
                  accountType = "2"; // Change accounttype from root-domain("1") to domain-admin("2") 
								}
								$.extend(data, {
								  accounttype: accountType
								});
               
                if(args.data.timezone != null && args.data.timezone.length > 0) {
								  $.extend(data, {
									  timezone: args.data.timezone
									});                  
								}

                if(args.data.networkdomain != null && args.data.networkdomain.length > 0) {
								  $.extend(data, {
									  networkdomain: args.data.networkdomain
									});                  
								}

                $.ajax({
                  url: createURL('createAccount'),
                  type: "POST",
                  data: data,
                  success: function(json) {
                    var item = json.createaccountresponse.account;
                    args.response.success({data:item});
                  },
                  error: function(XMLHttpResponse) {                    
                    args.response.error(parseXMLHttpResponse(XMLHttpResponse));
                  }
                });
              },

              notification: {
                poll: function(args) {
                  args.complete({
                    actionFilter: accountActionfilter
                  });
                }
              }
            }
          },

          dataProvider: function(args) {
            var data = {};
						listViewDataProvider(args, data);			
						
            if("domains" in args.context) {
						  $.extend(data, {
							  domainid: args.context.domains[0].id
							});
						}
						
            $.ajax({
              url: createURL('listAccounts'),
              data: data,
              async: true,
              success: function(json) {
                var items = json.listaccountsresponse.account;
                args.response.success({
                  actionFilter: accountActionfilter,
                  data:items
                });
              }
            });
          },

          detailView: {
            name: 'Account details',
            isMaximized: true,
            viewAll: { path: 'accounts.users', label: 'label.users' },

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

									if(args.data.vmLimit != null) {
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

									if(args.data.ipLimit != null) {
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

									if(args.data.volumeLimit != null) {
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

									if(args.data.snapshotLimit != null) {
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
 
                  if(args.data.templateLimit != null) {
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
									
									if(args.data.vpcLimit != null) {
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

									if(args.data.cpuLimit != null) {
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

									if(args.data.memoryLimit != null) {
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

									if(args.data.primaryStorageLimit != null) {
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

									if(args.data.secondaryStorageLimit != null) {
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
                  args.response.success({data: accountObj});
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
                      //var resourcecounts= json.updateresourcecountresponse.resourcecount;   //do nothing
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
                      args.response.success(
                        {_custom:
                         {jobId: jid,
                          getUpdatedItem: function(json) {
                            return json.queryasyncjobresultresponse.jobresult.account;
                          },
                          getActionFilter: function() {
                            return accountActionfilter;
                          }
                         }
                        }
                      );
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
                      args.response.success(
                        {_custom:
                         {jobId: jid,
                          getUpdatedItem: function(json) {
                            return json.queryasyncjobresultresponse.jobresult.account;
                          },
                          getActionFilter: function() {
                            return accountActionfilter;
                          }
                         }
                        }
                      );
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
                      args.response.success({data: json.enableaccountresponse.account});
                    }
                  });
                },
                notification: {
                  poll: function(args) {
                    args.complete({
                      data: { state: 'enabled' }
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
                      args.response.success(
                        {_custom:
                         {jobId: jid,
                          getUpdatedItem: function(json) {
                            return {}; //nothing in this account needs to be updated, in fact, this whole account has being deleted
                          },
                          getActionFilter: function() {
                            return accountActionfilter;
                          }
                         }
                        }
                      );
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

                fields: [
                  {
                    name: {
                      label: 'label.name',
                      isEditable: true,
					            validation: { required: true }
                    }
                  },
                  {
                    id: { label: 'ID' },
                    accounttype: {
                      label: 'label.role',
                      converter: function(args){
                        return cloudStack.converters.toRole(args);
                      }
                    },                    
                    domain: { label: 'label.domain' },
                    state: { label: 'label.state' },
                    networkdomain: {
                     label: 'label.network.domain',
                     isEditable: true
                    },
                    vmLimit: {
                      label: 'label.instance.limits',
                      isEditable: function(context) {

                                   if(context.accounts == undefined)
                                         return false;
                                               else {											  
											  if (context.accounts[0].accounttype == roleTypeUser || context.accounts[0].accounttype == roleTypeDomainAdmin) //updateResourceLimits is only allowed on account whose type is user or domain-admin
												  return true;
												else
												  return false;
				                        						}	}
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
                      label: 'VPC limits',
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

                    vmtotal: { label: 'label.total.of.vm' },
                    iptotal: { label: 'label.total.of.ip' },
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
                  }
                ],

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
													args.response.success(
														{
															actionFilter: accountActionfilter,
															data: accountObj 
														}
													);
												}
											});
										}
									});
                }
              },

              // Granular settings for account
              settings: {
                title: 'Settings',
                custom: cloudStack.uiCustom.granularSettings({
                  dataProvider: function(args) {
                     $.ajax({
                            url:createURL('listConfigurations&accountid=' + args.context.accounts[0].id),
                             data: { page: args.page, pageSize: pageSize, listAll: true },
                            success:function(json){
                              args.response.success({
                                 data:json.listconfigurationsresponse.configuration

                                 });

                             },

                            error:function(json){
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
                          url:createURL('updateConfiguration&accountid=' + args.context.accounts[0].id),
                          data:data,
                          success:function(json){
                              var item = json.updateconfigurationresponse.configuration;
                              args.response.success({data:item});
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
            username: { label: 'label.username', editable: true },
            firstname: { label: 'label.first.name' },
            lastname: { label: 'label.last.name' }
          },
          dataProvider: function(args) {    
            var accountObj = args.context.accounts[0];
						
						if(isAdmin() || isDomainAdmin()) {
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
						}
						else { //normal user doesn't have access listUsers API until Bug 14127 is fixed.
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
                if(isAdmin())
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
                    validation: { required: true },
                    docID: 'helpUserUsername'
                  },
                  password: {
                    label: 'label.password',
                    isPassword: true,
                    validation: { required: true },
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
                    validation: { required: true, email: true }
                  },
                  firstname: {
                    label: 'label.first.name',
                    docID: 'helpUserFirstName',
                    validation: { required: true }
                  },
                  lastname: {
                    label: 'label.last.name',
                    docID: 'helpUserLastName',
                    validation: { required: true }
                  },
                  timezone: {
                    label: 'label.timezone',
                    docID: 'helpUserTimezone',
                    select: function(args) {
                      var items = [];
                      items.push({id: "", description: ""});
                      for(var p in timezoneMap)
                        items.push({id: p, description: timezoneMap[p]});
                      args.response.success({data: items});
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
                if (md5Hashed) {
                  password = $.md5(password);     
                }									
								$.extend(data, {
                  password: password
                });			
               
								$.extend(data, {
								  email: args.data.email,
									firstname: args.data.firstname,
									lastname: args.data.lastname
								});
								
                if(args.data.timezone != null && args.data.timezone.length > 0) {
								  $.extend(data, {
									  timezone: args.data.timezone
									});								
								}
               
								$.extend(data, {
								  domainid: accountObj.domainid,
                  account: accountObj.name,
									accounttype: accountObj.accounttype
								});
								
                $.ajax({
                  url: createURL('createUser'),
                  type: "POST",
                  data: data,
                  success: function(json) {
                    var item = json.createuserresponse.user;
                    args.response.success({data: item});
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
            name: 'User details',
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
                      args.response.success({data:item});
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
                createForm: {
                  title: 'label.action.change.password',
                  fields: {
                    newPassword: {
                      label: 'label.new.password',
                      isPassword: true,
                      validation: { required: true },
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
                action: function(args) {
                  var password = args.data.newPassword;
                  if (md5Hashed)
                    password = $.md5(password);
                  
									var data = {
									  id: args.context.users[0].id,
										password: password 
									};
									
                  $.ajax({
                    url: createURL('updateUser'),
                    data: data,
                    async: true,
                    success: function(json) {
                      args.response.success({data: json.updateuserresponse.user});
                    }
                  });
                },
                notification: {
                  poll: function(args) {
                    args.complete();
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
                      args.response.success({data: json.registeruserkeysresponse.userkeys});
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
                      args.response.success(
                        {_custom:
                         {jobId: jid,
                          getUpdatedItem: function(json) {
                            return json.queryasyncjobresultresponse.jobresult.user;
                          },
                          getActionFilter: function() {
                            return userActionfilter;
                          }
                         }
                        }
                      );
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
                      args.response.success({data: json.enableuserresponse.user});
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

                fields: [
                  {
                    username: {
                      label: 'label.name',
                      isEditable: true,
					            validation: { required: true }
                    }
                  },
                  {
                    id: { label: 'ID' },
                    state: { label: 'label.state' },
                    apikey: { label: 'label.api.key' },
                    secretkey: { label: 'label.secret.key' },
                    account: { label: 'label.account.name' },
                    accounttype: {
                      label: 'label.role',
                      converter: function(args) {
                        return cloudStack.converters.toRole(args);
                      }
                    },
                    domain: { label: 'label.domain' },
                    email: {
                      label: 'label.email',
                      isEditable: true,
					            validation: { required: true, email: true }
                    },
                    firstname: {
                      label: 'label.first.name',
                      isEditable: true,
					            validation: { required: true }
                    },
                    lastname: {
                      label: 'label.last.name',
                      isEditable: true,
					            validation: { required: true }
                    },
                    timezone: {
                      label: 'label.timezone',
                      converter: function(args) {
                        if(args == null || args.length == 0)
                          return "";
                        else
                          return args;
                      },
                      isEditable: true,
                      select: function(args) {
                        var items = [];
                        items.push({id: "", description: ""});
                        for(var p in timezoneMap)
                          items.push({id: p, description: timezoneMap[p]});
                        args.response.success({data: items});
                      }
                    }
                  }
                ],

                dataProvider: function(args) {
								  if(isAdmin() || isDomainAdmin()) {								
										$.ajax({
											url: createURL('listUsers'),
											data: {
												id: args.context.users[0].id
											},
											success: function(json) {
												args.response.success({
													actionFilter: userActionfilter,
													data: json.listusersresponse.user[0]
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
      }
    }
  };

  var accountActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];

    if (jsonObj.state == 'Destroyed') return [];

    if( isAdmin() && jsonObj.isdefault == false)
       allowedActions.push("remove");

    if(isAdmin()) {
        allowedActions.push("edit"); //updating networkdomain is allowed on any account, including system-generated default admin account 
        if(!(jsonObj.domain == "ROOT" && jsonObj.name == "admin" && jsonObj.accounttype == 1)) { //if not system-generated default admin account    
            if(jsonObj.state == "enabled") {
                allowedActions.push("disable");
                allowedActions.push("lock");
            } else if(jsonObj.state == "disabled" || jsonObj.state == "locked") {
                allowedActions.push("enable");
            }
            allowedActions.push("remove");
        }
        allowedActions.push("updateResourceCount");
    } else if(isDomainAdmin()) {
        allowedActions.push("updateResourceCount");
    }
    return allowedActions;
  }

  var userActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];
   
    if( isAdmin() && jsonObj.isdefault == false)
       allowedActions.push("remove");

    if(isAdmin()) {
      allowedActions.push("edit");
      allowedActions.push("changePassword");
      allowedActions.push("generateKeys");
      if(!(jsonObj.domain == "ROOT" && jsonObj.account == "admin" && jsonObj.accounttype == 1)) { //if not system-generated default admin account user 
        if(jsonObj.state == "enabled")
          allowedActions.push("disable");
        if(jsonObj.state == "disabled")
          allowedActions.push("enable");
        allowedActions.push("remove");
      }
    } else {
        if(isSelfOrChildDomainUser(jsonObj.username, jsonObj.accounttype, jsonObj.domainid, jsonObj.iscallerchilddomain)) {
            allowedActions.push("changePassword");
            allowedActions.push("generateKeys");
        }
    }
    return allowedActions;
  }

})(cloudStack);
