(function(cloudStack, testData) {

  var rootDomainId = 1;

  var systemAccountId = 1;
  var adminAccountId = 2;

  var systemUserId = 1;
  var adminUserId = 2;

  cloudStack.sections.accounts = {
    title: 'Accounts',
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
        title: 'Accounts',
        listView: {
          id: 'accounts',
          fields: {
            name: { label: 'Name' },
            accounttype: {
              label: 'Role',
              converter: function(args){
                return cloudStack.converters.toRole(args);
              }
            },
            domain: { label: 'Domain' },
            state: { label: 'State', indicator: { 'enabled': 'on', 'Destroyed': 'off', 'disabled': 'off' } }
          },

          actions: {
            add: {
              label: 'Create account',
							preFilter: function(args) {							  
								if(isAdmin()) 								
								  return true;
								else
								  return false;
							},							
              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to create an account?';
                },
                notification: function(args) {
                  return 'Creating new account';
                }
              },

              createForm: {
                title: 'Create account',
                desc: 'Please fill in the following data to create a new account.',
                fields: {
                  username: {
                    label: 'Username',
                    validation: { required: true }
                  },
                  password: {
                    label: 'Password',
                    validation: { required: true },
                    isPassword: true
                  },
                  email: {
                    label: 'Email',
                    validation: { required: true }
                  },
                  firstname: {
                    label: 'First name',
                    validation: { required: true }
                  },
                  lastname: {
                    label: 'Last name',
                    validation: { required: true }
                  },
                  domainid: {
                    label: 'Domain',
                    validation: { required: true },
                    select: function(args) {
                      $.ajax({
                        url: createURL("listDomains"),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                          var items = [];
                          var domainObjs = json.listdomainsresponse.domain;
                          $(domainObjs).each(function() {
                            items.push({id: this.id, description: this.name});
                          });
                          args.response.success({data: items});
                        }
                      });
                    }
                  },
                  account: {
                    label: 'Account'
                  },
                  accounttype: {
                    label: 'Account type',
                    validation: { required: true },
                    select: function(args) {
                      var items = [];
                      items.push({id:0, description: "User"});  //regular-user
                      items.push({id:1, description: "Admin"}); //root-admin
                      args.response.success({data: items});
                    }
                  },
                  timezone: {
                    label: 'Timezone',
                    select: function(args) {
                      var items = [];
                      items.push({id: "", description: ""});
                      for(var p in timezoneMap)
                        items.push({id: p, description: timezoneMap[p]});
                      args.response.success({data: items});
                    }
                  },
                  networkdomain: { 
                    label: 'Network domain',
                    validation: { required: false }
                  }
                }
              },

              action: function(args) {
                var array1 = [];
                array1.push("&username=" + todb(args.data.username));

                var password = args.data.password;
                if (md5Hashed)
                  password = $.md5(password);
                array1.push("&password=" + password);

                array1.push("&email=" + todb(args.data.email));
                array1.push("&firstname=" + todb(args.data.firstname));
                array1.push("&lastname=" + todb(args.data.lastname));

                array1.push("&domainid=" + args.data.domainid);

                var account = args.data.account;
                if(account == null || account.length == 0)
                  account = args.data.username;
                array1.push("&account=" + todb(account));

                var accountType = args.data.accounttype;
                if (args.data.accounttype == "1" && parseInt(args.data.domainid) != rootDomainId) //if account type is admin, but domain is not Root domain
                  accountType = "2"; // Change accounttype from root-domain("1") to domain-admin("2")
                array1.push("&accounttype=" + accountType);

                if(args.data.timezone != null && args.data.timezone.length > 0)
                  array1.push("&timezone=" + todb(args.data.timezone));

                if(args.data.networkdomain != null && args.data.networkdomain.length > 0)
                  array1.push("&networkdomain=" + todb(args.data.networkdomain));      
                  
                $.ajax({
                  url: createURL("createAccount" + array1.join("")),
                  dataType: "json",
                  success: function(json) {
                    var item = json.createaccountresponse.account;
                    args.response.success({data:item});
                  },
                  error: function(XMLHttpResponse) {
                    var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                    args.response.error(errorMsg);
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
            var array1 = [];
						if(args.filterBy != null) {          
							if(args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
								switch(args.filterBy.search.by) {
								case "name":
									if(args.filterBy.search.value.length > 0)
										array1.push("&keyword=" + args.filterBy.search.value);
									break;
								}
							}
						}
						
            if("domains" in args.context)
              array1.push("&domainid=" + args.context.domains[0].id);
            $.ajax({
              url: createURL("listAccounts" + "&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
              dataType: "json",
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
            viewAll: { path: 'accounts.users', label: 'Users' },

            actions: {
              edit: {
                label: 'Edit ("-1" indicates no limit to the amount of resources create)',
                action: function(args) {
                  var accountObj = args.context.accounts[0];

                  var array1 = [];                  
                  array1.push("&newname=" + todb(args.data.name));                  
                  array1.push("&networkdomain=" + todb(args.data.networkdomain));
                  $.ajax({
                    url: createURL("updateAccount&domainid=" + accountObj.domainid + "&account=" + accountObj.name + array1.join("")),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      accountObj = json.updateaccountresponse.account;
                    }
                  });

                  $.ajax({
                    url: createURL("updateResourceLimit&resourceType=0&max=" + todb(args.data.vmLimit) + "&account=" + accountObj.name + "&domainid=" + accountObj.domainid),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      accountObj["vmLimit"] = args.data.vmLimit;
                    }
                  });

                  $.ajax({
                    url: createURL("updateResourceLimit&resourceType=1&max=" + todb(args.data.ipLimit) + "&account=" + accountObj.name + "&domainid=" + accountObj.domainid),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      accountObj["ipLimit"] = args.data.ipLimit;
                    }
                  });

                  $.ajax({
                    url: createURL("updateResourceLimit&resourceType=2&max=" + todb(args.data.volumeLimit) + "&account=" + accountObj.name + "&domainid=" + accountObj.domainid),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      accountObj["volumeLimit"] = args.data.volumeLimit;
                    }
                  });

                  $.ajax({
                    url: createURL("updateResourceLimit&resourceType=3&max=" + todb(args.data.snapshotLimit) + "&account=" + accountObj.name + "&domainid=" + accountObj.domainid),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      accountObj["snapshotLimit"] = args.data.snapshotLimit;
                    }
                  });

                  $.ajax({
                    url: createURL("updateResourceLimit&resourceType=4&max=" + todb(args.data.templateLimit) + "&account=" + accountObj.name + "&domainid=" + accountObj.domainid),
                    dataType: "json",
                    async: false,
                    success: function(json) {
                      accountObj["templateLimit"] = args.data.templateLimit;
                    }
                  });

                  args.response.success({data: accountObj});
                }
              },

              updateResourceCount: {
                label: 'Update Resource Count',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to update resource count ?';
                  },
                  notification: function(args) {
                    return 'Updating resource count';
                  }
                },
                action: function(args) {
                  var accountObj = args.context.accounts[0];
                  $.ajax({
                    url: createURL("updateResourceCount&domainid=" + accountObj.domainid + "&account=" + accountObj.name),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      //var resourcecounts= json.updateresourcecountresponse.resourcecount;   //do nothing
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
                label: 'Disable account',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to disable this account?';
                  },
                  notification: function(args) {
                    return 'Disabling account';
                  }
                },
                action: function(args) {
                  var accountObj = args.context.accounts[0];
                  $.ajax({
                    url: createURL("disableAccount&lock=false&domainid=" + accountObj.domainid + "&account=" + accountObj.name),
                    dataType: "json",
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
                label: 'Lock account',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to lock this account?';
                  },
                  notification: function(args) {
                    return 'Locking account';
                  }
                },
                action: function(args) {
                  var accountObj = args.context.accounts[0];
                  $.ajax({
                    url: createURL("disableAccount&lock=true&domainid=" + accountObj.domainid + "&account=" + accountObj.name),
                    dataType: "json",
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
                label: 'Enable account',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to enable this account?';
                  },
                  notification: function(args) {
                    return 'Enabling account';
                  }
                },
                action: function(args) {
                  var accountObj = args.context.accounts[0];
                  $.ajax({
                    url: createURL("enableAccount&domainid=" + accountObj.domainid + "&account=" + accountObj.name),
                    dataType: "json",
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

              destroy: {
                label: 'Delete account',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to delete this account?';
                  },
                  notification: function(args) {
                    return 'Deleting account';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteAccount&id=" + args.context.accounts[0].id),
                    dataType: "json",
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
                title: 'details',

                fields: [
                  {
                    name: {
                      label: 'Name',
                      isEditable: true
                    }
                  },
                  {
                    id: { label: 'ID' },
                    accounttype: {
                      label: 'Role',
                      converter: function(args){
                        return cloudStack.converters.toRole(args);
                      }
                    },
                    domain: { label: 'Domain' },
                    state: { label: 'State' },
                    networkdomain: { 
                     label: 'Network domain',
                     isEditable: true
                    },                    
                    vmLimit: {
                      label: 'Instance limits',
                      isEditable: true
                    },
                    ipLimit: {
                      label: 'Public IP limits',
                      isEditable: true
                    },
                    volumeLimit: {
                      label: 'Volume limits',
                      isEditable: true
                    },
                    snapshotLimit: {
                      label: 'Snapshot limits',
                      isEditable: true
                    },
                    templateLimit: {
                      label: 'Template limits',
                      isEditable: true
                    },

                    vmtotal: { label: 'Total of VM' },
                    iptotal: { label: 'Total of IP Address' },
                    receivedbytes: {
                      label: 'Bytes received',
                      converter: function(args) {
                        if (args == null || args == 0)
                          return "";
                        else
                          return cloudStack.converters.convertBytes(args);
                      }
                    },
                    sentbytes: {
                      label: 'Bytes sent',
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
                  var accountObj = args.context.accounts[0];

                  $.ajax({
                    url: createURL("listResourceLimits&domainid=" + accountObj.domainid + "&account=" + accountObj.name),
                    dataType: "json",
                    async: false,
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
                          }
                        }
                      }
                    }
                  });

                  args.response.success(
                    {
                      actionFilter: accountActionfilter,
                      data: accountObj
                    }
                  );
                }
              }
            }
          }
        }
      },
      users: {
        type: 'select',
        id: 'users',
        title: 'Users',
        listView: {
          id: 'users',
          fields: {
            username: { label: 'Username', editable: true },
            firstname: { label: 'First name' },
            lastname: { label: 'Last name' }
          },
          dataProvider: function(args) {					  
						var array1 = [];  
						if(args.filterBy != null) {          
							if(args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
								switch(args.filterBy.search.by) {
								case "name":
									if(args.filterBy.search.value.length > 0)
										array1.push("&keyword=" + args.filterBy.search.value);
									break;
								}
							}
						}
										
            var accountObj = args.context.accounts[0];
            $.ajax({
              url: createURL("listUsers&domainid=" + accountObj.domainid + "&account=" + accountObj.name + "&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
              dataType: "json",
              success: function(json) {
                args.response.success({
                  actionFilter: userActionfilter,
                  data: json.listusersresponse.user
                });
              }
            })
          },
          actions: {
            add: {
              label: 'Create user',

              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to create an user?';
                },
                notification: function(args) {
                  return 'Creating new user';
                }
              },

              createForm: {
                title: 'Create user',
                fields: {
                  username: {
                    label: 'Username',
                    validation: { required: true }
                  },
                  password: {
                    label: 'Password',
                    isPassword: true,
                    validation: { required: true }
                  },
                  email: {
                    label: 'Email',
                    validation: { required: true }
                  },
                  firstname: {
                    label: 'First name',
                    validation: { required: true }
                  },
                  lastname: {
                    label: 'Last name',
                    validation: { required: true }
                  },
                  timezone: {
                    label: 'Timezone',
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

                var array1 = [];
                array1.push("&username=" + todb(args.data.username));

                var password = args.data.password;
                if (md5Hashed)
                  password = $.md5(password);
                array1.push("&password=" + password);

                array1.push("&email=" + todb(args.data.email));
                array1.push("&firstname=" + todb(args.data.firstname));
                array1.push("&lastname=" + todb(args.data.lastname));
                if(args.data.timezone != null && args.data.timezone.length > 0)
                  array1.push("&timezone=" + todb(args.data.timezone));

                array1.push("&domainid=" + accountObj.domainid);
                array1.push("&account=" + accountObj.name);
                array1.push("&accounttype=" + accountObj.accounttype);

                $.ajax({
                  url: createURL("createUser" + array1.join("")),
                  dataType: "json",
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
            actions: {
              edit: {
                label: 'Edit',
                action: function(args) {
                  var array1 = [];
                  array1.push("&username=" + todb(args.data.username));
                  array1.push("&email=" + todb(args.data.email));
                  array1.push("&firstname=" + todb(args.data.firstname));
                  array1.push("&lastname=" + todb(args.data.lastname));
                  array1.push("&timezone=" + todb(args.data.timezone));
                  $.ajax({
                    url: createURL("updateUser&id=" + args.context.users[0].id + array1.join("")),
                    dataType: "json",
                    success: function(json) {
                      var item = json.updateuserresponse.user;
                      args.response.success({data:item});
                    }
                  });

                }
              },

              changePassword: {
                label: 'Change password',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to change password?';
                  },
                  notification: function(args) {
                    return 'Changing password';
                  }
                },
                createForm: {
                  label: 'Change password',
                  fields: {
                    newPassword: { 
										  label: 'New password',
											isPassword: true, 
											validation: { required: true }
										}
                  }
                },
                action: function(args) {
                  var password = args.data.newPassword;
                  if (md5Hashed)
                    password = $.md5(password);
                  $.ajax({
                    url: createURL("updateUser&id=" + args.context.users[0].id + "&password=" + password),
                    dataType: "json",
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
                label: 'Generate keys',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to generate keys?';
                  },
                  notification: function(args) {
                    return 'Generating keys';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("registerUserKeys&id=" + args.context.users[0].id),
                    dataType: "json",
                    async: true,
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
                label: 'Disable user',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to disable this user?';
                  },
                  notification: function(args) {
                    return 'Disabling user';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("disableUser&id=" + args.context.users[0].id),
                    dataType: "json",
                    async: true,
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
                label: 'Enable user',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to enable this user?';
                  },
                  notification: function(args) {
                    return 'Enabling user';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("enableUser&id=" + args.context.users[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      args.response.success({data: json.enableuserresponse.user});
                    }
                  });
                },
                notification: {
                  poll: function(args) {
                    args.complete();
                  }
                }
              },

              'delete': {
                label: 'Delete user',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to delete this user?';
                  },
                  notification: function(args) {
                    return 'Deleting user';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteUser&id=" + args.context.users[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {}
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
                title: 'details',

                fields: [
                  {
                    username: {
                      label: 'Name',
                      isEditable: true
                    }
                  },
                  {
                    id: { label: 'ID' },
                    state: { label: 'State' },
                    apikey: { label: 'API key' },
                    secretkey: { label: 'Secret key' },
                    account: { label: 'Account name' },
                    accounttype: {
                      label: 'Role',
                      converter: function(args) {
                        return cloudStack.converters.toRole(args);
                      }
                    },
                    domain: { label: 'Domain' },
                    email: {
                      label: 'Email',
                      isEditable: true
                    },
                    firstname: {
                      label: 'First name',
                      isEditable: true
                    },
                    lastname: {
                      label: 'Last name',
                      isEditable: true
                    },
                    timezone: {
                      label: 'Timezone',
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
                  args.response.success(
                    {
                      actionFilter: userActionfilter,
                      data:args.context.users[0]
                    }
                  );
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

    if(isAdmin()) {
      if(jsonObj.id != systemAccountId && jsonObj.id != adminAccountId) {
        //allowedActions.push("edit");
        if (jsonObj.accounttype == roleTypeUser || jsonObj.accounttype == roleTypeDomainAdmin) {
          //allowedActions.push("updateResourceLimits");
          allowedActions.push("edit");
        }
        if(jsonObj.state == "enabled") {
          allowedActions.push("disable");
          allowedActions.push("lock");
        }
        else if(jsonObj.state == "disabled" || jsonObj.state == "locked") {
          allowedActions.push("enable");
        }
        allowedActions.push("destroy");
      }
    }
    allowedActions.push("updateResourceCount");
    return allowedActions;
  }

  var userActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];
    if(isAdmin()) {
      allowedActions.push("edit");
      allowedActions.push("changePassword");
      allowedActions.push("generateKeys");
      if(jsonObj.id != systemUserId && jsonObj.id != adminUserId) {
        if(jsonObj.state == "enabled")
          allowedActions.push("disable");
        if(jsonObj.state == "disabled")
          allowedActions.push("enable");
        allowedActions.push("delete");
      }
    }
    return allowedActions;
  }

})(cloudStack, testData);
