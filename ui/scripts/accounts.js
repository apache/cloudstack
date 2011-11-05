(function(cloudStack, testData) {

  var rootDomainId = 1;

  var systemAccountId = 1;
  var adminAccountId = 2;

  var systemUserId = 1;
  var adminUserId = 2;
  
  var selectedAccountResourceLimits;
  
  cloudStack.sections.accounts = {
    title: 'Accounts',
    id: 'accounts',
    
    sections: {
      accounts: {        
        listView: {
          fields: {
            name: { label: 'Name' },
            accounttype: {
              label: 'Role',
              converter: function(args){
                return cloudStack.converters.toRole(args);
              }
            },
            domain: { label: 'Domain' },
            state: { label: 'State' }
          },

          actions: {
            add: {
              label: 'Create account',

              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to create an account?';
                },
                success: function(args) {
                  return 'Your new account is being created.';
                },
                notification: function(args) {
                  return 'Creating new account';
                },
                complete: function(args) {
                  return 'Account has been created successfully!';
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
                  args.complete();
                }
              }
            }
          },

          dataProvider: function(args) {
            $.ajax({
              url: createURL("listAccounts&page=" + args.page + "&pagesize=" + pageSize),
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
                  $.ajax({
                    url: createURL("updateAccount&newname=" + todb(args.data.name) + "&account=" + accountObj.name + "&domainid=" + accountObj.domainid),
                    dataType: "json",
                    async: false,
                    success: function(json) {                                        
                      accountObj = json.updateaccountresponse.account;
                      args.response.success({data: accountObj});
                    }
                  });
                                   
                  $.ajax({
                    url: createURL("updateResourceLimit&resourceType=0&max=" + todb(args.data.instanceLimits) + "&account=" + accountObj.name + "&domainid=" + accountObj.domainid),
                    dataType: "json",
                    async: false,
                    success: function(json) {		
                      selectedAccountResourceLimits = json.updateresourcelimitresponse.resourcelimit;                      
                    }
                  });          
                 
                  $.ajax({
                    url: createURL("updateResourceLimit&resourceType=1&max=" + todb(args.data.ipLimits) + "&account=" + accountObj.name + "&domainid=" + accountObj.domainid),
                    dataType: "json",
                    async: false,
                    success: function(json) {		
                      selectedAccountResourceLimits = json.updateresourcelimitresponse.resourcelimit;                      
                    }
                  });       
                              
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
                    },
                    instanceLimits: {
                      label: 'Instance limits',
                      isEditable: true,
                      converter: function(args) {                        
                        if (selectedAccountResourceLimits != null) {	
                          for (var i = 0; i < selectedAccountResourceLimits.length; i++) {
                            if(selectedAccountResourceLimits[i].resourcetype == "0") {
                              return selectedAccountResourceLimits[i].max;
                              break;
                            }
                          }
                        }  
                      }
                    },
                                        
                    ipLimits: {
                      label: 'Public IP limits',
                      isEditable: true,
                      converter: function(args) {                        
                        if (selectedAccountResourceLimits != null) {	
                          for (var i = 0; i < selectedAccountResourceLimits.length; i++) {
                            if(selectedAccountResourceLimits[i].resourcetype == "1") {
                              return selectedAccountResourceLimits[i].max;
                              break;
                            }
                          }
                        }  
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
                      selectedAccountResourceLimits = json.listresourcelimitsresponse.resourcelimit;		                      
                    }
                  });
                 
                  args.response.success(
                    {
                      actionFilter: accountActionfilter,
                      data: args.context.accounts[0]
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
        title: 'Users',
        listView: {
          id: 'users',
          fields: {
            username: { label: 'Username', editable: true },
            firstname: { label: 'First name' },
            lastname: { label: 'Last name' }
          },          
          dataProvider: function(args) {            
            var accountObj = args.context.accounts[0];
            $.ajax({
              url: createURL("listUsers&domainid=" + fromdb(accountObj.domainid) + "&account=" + todb(fromdb(accountObj.name))),
              dataType: "json",
              success: function(json) {                
                args.response.success({data: json.listusersresponse.user});                
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
                success: function(args) {
                  return 'Your new user is being created.';
                },
                notification: function(args) {
                  return 'Creating new user';
                },
                complete: function(args) {
                  return 'User has been created successfully!';
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
            tabs: {
              details: {
                title: 'details',

                fields: [
                  {
                    username: { label: 'Name' }
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
                    email: { label: 'Email' },
                    firstname: { label: 'First name' },
                    lastname: { label: 'Last name' },
                    timezone: { 
                      label: 'Timezone',
                      converter: function(args) {                        
                        if(args == null || args.length == 0)
                          return "";
                        else                        
                          return timezoneMap[args];
                      }
                    }                    
                  }
                ],

                dataProvider: function(args) {
                  args.response.success(
                    {
                      actionFilter: accountActionfilter,
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
        allowedActions.push("delete");                 
      }  
    }    
    allowedActions.push("updateResourceCount");        
    return allowedActions;
  }

})(cloudStack, testData);
