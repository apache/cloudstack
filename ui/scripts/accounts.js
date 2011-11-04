(function(cloudStack, testData) {

  var rootDomainId = 1;

  cloudStack.sections.accounts = {
    title: 'Accounts',
    id: 'accounts',
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
            preFilter: cloudStack.preFilter.createTemplate,
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
                  items.push({id: "Etc/GMT+12", description: "[UTC-12:00] GMT-12:00"});
                  items.push({id: "Etc/GMT+11", description: "[UTC-11:00] GMT-11:00"});
                  items.push({id: "Pacific/Samoa", description: "[UTC-11:00] Samoa Standard Time"});
                  items.push({id: "Pacific/Honolulu", description: "[UTC-10:00] Hawaii Standard Time"});
                  items.push({id: "US/Alaska", description: "[UTC-09:00] Alaska Standard Time"});                  
                  items.push({id: "Mexico/BajaNorte", description: "[UTC-08:00] Baja California"});
                  items.push({id: "US/Arizona", description: "[UTC-07:00] Arizona"});
                  items.push({id: "US/Mountain", description: "[UTC-07:00] Mountain Standard Time"});
                  items.push({id: "America/Chihuahua", description: "[UTC-07:00] Chihuahua, La Paz"});
                  items.push({id: "America/Chicago", description: "[UTC-06:00] Central Standard Time"});
                  items.push({id: "America/Costa_Rica", description: "[UTC-06:00] Central America"});
                  items.push({id: "America/Mexico_City", description: "[UTC-06:00] Mexico City, Monterrey"});
                  items.push({id: "Canada/Saskatchewan", description: "[UTC-06:00] Saskatchewan"});
                  items.push({id: "America/Bogota", description: "[UTC-05:00] Bogota, Lima"});
                  items.push({id: "America/New_York", description: "[UTC-05:00] Eastern Standard Time"});
                  items.push({id: "America/Caracas", description: "[UTC-04:00] Venezuela Time"});
                  items.push({id: "America/Asuncion", description: "[UTC-04:00] Paraguay Time"});
                  items.push({id: "America/Cuiaba", description: "[UTC-04:00] Amazon Time"});
                  items.push({id: "America/Halifax", description: "[UTC-04:00] Atlantic Standard Time"});
                  items.push({id: "America/La_Paz", description: "[UTC-04:00] Bolivia Time"});
                  items.push({id: "America/Santiago", description: "[UTC-04:00] Chile Time"});
                  items.push({id: "America/St_Johns", description: "[UTC-03:30] Newfoundland Standard Time"});
                  items.push({id: "America/Araguaina", description: "[UTC-03:00] Brasilia Time"});
                  items.push({id: "America/Argentina/Buenos_Aires", description: "[UTC-03:00] Argentine Time"});
                  items.push({id: "America/Cayenne", description: "[UTC-03:00] French Guiana Time"});
                  items.push({id: "America/Godthab", description: "[UTC-03:00] Greenland Time"});
                  items.push({id: "America/Montevideo", description: "[UTC-03:00] Uruguay Time"});
                  items.push({id: "Etc/GMT+2", description: "[UTC-02:00] GMT-02:00"});
                  items.push({id: "Atlantic/Azores", description: "[UTC-01:00] Azores Time"});
                  items.push({id: "Atlantic/Cape_Verde", description: "[UTC-01:00] Cape Verde Time"});
                  items.push({id: "Africa/Casablanca", description: "[UTC] Casablanca"});
                  items.push({id: "Etc/UTC", description: "[UTC] Coordinated Universal Time"});
                  items.push({id: "Atlantic/Reykjavik", description: "[UTC] Reykjavik"});
                  items.push({id: "Europe/London", description: "[UTC] Western European Time"});
                  items.push({id: "CET", description: "[UTC+01:00] Central European Time"});
                  items.push({id: "Europe/Bucharest", description: "[UTC+02:00] Eastern European Time"});
                  items.push({id: "Africa/Johannesburg", description: "[UTC+02:00] South Africa Standard Time"});
                  items.push({id: "Asia/Beirut", description: "[UTC+02:00] Beirut"});
                  items.push({id: "Africa/Cairo", description: "[UTC+02:00] Cairo"});
                  items.push({id: "Asia/Jerusalem", description: "[UTC+02:00] Israel Standard Time"});
                  items.push({id: "Europe/Minsk", description: "[UTC+02:00] Minsk"});
                  items.push({id: "Europe/Moscow", description: "[UTC+03:00] Moscow Standard Time"});
                  items.push({id: "Africa/Nairobi", description: "[UTC+03:00] Eastern African Time"});
                  items.push({id: "Asia/Karachi", description: "[UTC+05:00] Pakistan Time"});
                  items.push({id: "Asia/Kolkata", description: "[UTC+05:30] India Standard Time"});
                  items.push({id: "Asia/Bangkok", description: "[UTC+05:30] Indochina Time"});
                  items.push({id: "Asia/Shanghai", description: "[UTC+08:00] China Standard Time"});
                  items.push({id: "Asia/Kuala_Lumpur", description: "[UTC+08:00] Malaysia Time"});
                  items.push({id: "Australia/Perth", description: "[UTC+08:00] Western Standard Time (Australia)"});
                  items.push({id: "Asia/Taipei", description: "[UTC+08:00] Taiwan"});
                  items.push({id: "Asia/Tokyo", description: "[UTC+09:00] Japan Standard Time"});
                  items.push({id: "Asia/Seoul", description: "[UTC+09:00] Korea Standard Time"});
                  items.push({id: "Australia/Adelaide", description: "[UTC+09:30] Central Standard Time (South Australia)"});
                  items.push({id: "Australia/Darwin", description: "[UTC+09:30] Central Standard Time (Northern Territory)"});
                  items.push({id: "Australia/Brisbane", description: "[UTC+10:00] Eastern Standard Time (Queensland)"});
                  items.push({id: "Australia/Canberra", description: "[UTC+10:00] Eastern Standard Time (New South Wales)"});
                  items.push({id: "Pacific/Guam", description: "[UTC+10:00] Chamorro Standard Time"});
                  items.push({id: "Pacific/Auckland", description: "[UTC+12:00] New Zealand Standard Time"});                 
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

            /*
             var timezone = $thisDialog.find("#add_user_timezone").val();
             if(timezone != null && timezone.length > 0)
             array1.push("&timezone=" + todb(timezone));
             */

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

        actions: {

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
                }
              }
            ],

            dataProvider: function(args) {
              args.response.success(
                {
                  actionFilter: accountActionfilter,
                  data:args.context.accounts[0]
                }
              );
            }
          }
        }
      }
    }
  };

  var accountActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];
    return allowedActions;
  }

})(cloudStack, testData);
