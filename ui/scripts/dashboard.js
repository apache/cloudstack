(function($, cloudStack, testData) {
  // Admin dashboard
  cloudStack.sections.dashboard = {
    title: 'Dashboard',
    show: cloudStack.uiCustom.dashboard,

    adminCheck: function(args) {
      return isAdmin() ? true : false;
    },

    // User dashboard
    user: {
      dataProvider: function(args) {
        var dataFns = {
          instances: function(data) {
            $.ajax({
              url: createURL('listVirtualMachines'),
              success: function(json) {
                var instances = json.listvirtualmachinesresponse.virtualmachine ? 
                      json.listvirtualmachinesresponse.virtualmachine : [];
                
                dataFns.account($.extend(data, {
                  runningInstances: $.grep(instances, function(instance) {
                    return instance.state == 'Running';
                  }).length,
                  stoppedInstances: $.grep(instances, function(instance) {
                    return instance.state == 'Stopped';
                  }).length,
                  totalInstances: instances.length
                }));
              }
            });
          },

          account: function(data) {
            var user = cloudStack.context.users[0];
            complete($.extend(data, {
              accountID: user.userid,
              accountName: user.account,
              userName: user.username,
              accountType: cloudStack.converters.toRole(user.type),
              accountDomainID: user.domainid
            }));
          }
        };
        
        var complete = function(data) {
          args.response.success({
            data: data
          });
        };

        dataFns.instances({});
      }
    },

    // Admin dashboard
    admin: {
      dataProvider: function(args) {
        var dataFns = {
          zones: function(data) {
            $.ajax({
              url: createURL('listZones'),
              success: function(json) {
                dataFns.capacity({
                  zones: json.listzonesresponse.zone
                });
              }
            });
          },
          capacity: function(data) {
            if (data.zones) {
              $.ajax({
                url: createURL('listCapacity'),
                success: function(json) {
                  var capacities = json.listcapacityresponse.capacity;

                  var capacity = function(id, converter) {
                    return $.grep(capacities, function(capacity) {
                      return capacity.type == id;
                    })[0];
                  };

                  dataFns.alerts($.extend(data, {
                    publicIPAllocated: capacity(8).capacityused,
                    publicIPTotal: capacity(8).capacitytotal,
                    publicIPPercentage: parseInt(capacity(8).percentused),
                    privateIPAllocated: capacity(5).capacityused,
                    privateIPTotal: capacity(5).capacitytotal,
                    privateIPPercentage: parseInt(capacity(8).percentused),
                    memoryAllocated: cloudStack.converters.convertBytes(capacity(0).capacityused),
                    memoryTotal: cloudStack.converters.convertBytes(capacity(0).capacitytotal),
                    memoryPercentage: parseInt(capacity(0).percentused),
                    cpuAllocated: cloudStack.converters.convertHz(capacity(1).capacityused),
                    cpuTotal: cloudStack.converters.convertHz(capacity(1).capacitytotal),
                    cpuPercentage: parseInt(capacity(1).percentused)
                  }));
                }
              });
            } else {
              dataFns.alerts($.extend(data, {
                publicIPAllocated: 0,
                publicIPTotal: 0,
                publicIPPercentage: 0,
                privateIPAllocated: 0,
                privateIPTotal: 0,
                privateIPPercentage: 0,
                memoryAllocated: 0,
                memoryTotal: 0,
                memoryPercentage: 0,
                cpuAllocated: 0,
                cpuTotal: 0,
                cpuPercentage: 0
              }));
            }
          },

          alerts: function(data) {
            $.ajax({
              url: createURL('listAlerts'),
              data: {
                page: 1,
                pageSize: 4
              },
              success: function(json) {
                dataFns.hostAlerts($.extend(data, {
                  alerts: $.map(json.listalertsresponse.alert, function(alert) {
                    return {
                      name: cloudStack.converters.toAlertType(alert.type),
                      description: alert.description
                    };
                  })
                }));
              }
            });
          },

          hostAlerts: function(data) {
            $.ajax({
              url: createURL('listHosts'),
              data: {
                state: 'Alert',
                page: 1,
                pageSize: 4
              },
              success: function(json) {
                complete($.extend(data, {
                  hostAlerts: $.map(json.listhostsresponse.host, function(host) {
                    return {
                      name: host.name,
                      description: 'Alert state detected for ' + host.name
                    };
                  })
                }));
              }
            });
          }
        };

        var complete = function(data) {
          args.response.success({
            data: data
          });
        };

        dataFns.zones({});
      } 
    }
  };
})(jQuery, cloudStack, testData);
