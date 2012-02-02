(function($, cloudStack) {
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
      zoneDetailView: {
        tabs: {
          resources: {
            title: 'Resources',
            custom: cloudStack.uiCustom.systemChart('resources')
          }
        }
      },
      
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
                    var result = $.grep(capacities, function(capacity) {
                      return capacity.type == id;
                    });
                    return result[0] ? result[0] : {
                      capacityused: 0,
                      capacitytotal: 0,
                      percentused: 0
                    };
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
                var alerts = json.listalertsresponse.alert ?
                  json.listalertsresponse.alert : [];

                dataFns.hostAlerts($.extend(data, {
                  alerts: $.map(alerts, function(alert) {
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
                var hosts = json.listhostsresponse.host ?
                  json.listhostsresponse.host : [];

                dataFns.zoneCapacity($.extend(data, {
                  hostAlerts: $.map(hosts, function(host) {
                    return {
                      name: host.name,
                      description: 'Alert state detected for ' + host.name
                    };
                  })
                }));
              }
            });
          },

          zoneCapacity: function(data) {
            $.ajax({
              url: createURL('listZones'),
              data: {
                showCapacities: true
              },
              success: function(json) {
                var zones = json.listzonesresponse.zone ?
                  json.listzonesresponse.zone : [];

                var zoneCapacities = [];

                $(zones).each(function() {
                  var zone = this;
                  var clusters;

                  // Get cluster-level data
                  $.ajax({
                    url: createURL('listClusters'),
                    data: {
                      zoneId: zone.id,
                      showCapacities: true
                    },
                    async: false,
                    success: function(json) {
                      var cluster = json.listclustersresponse.cluster;

                      // Get cluster-level data
                      $(cluster).each(function() {
                        var cluster = this;

                        $(cluster.capacity).each(function() {
                          var capacity = this;

                          zoneCapacities.push($.extend(capacity, {
                            zoneName: zone.name +
                              '<br/>Pod: ' + cluster.podname +
                              '<br/>Cluster: ' + cluster.name
                          }));
                        });
                      });

                      // Get zone-level data
                      $(zone.capacity).each(function() {
                        var capacity = this;
                        var existingCapacityTypes = $.map(zoneCapacities, function(capacity) {
                          return capacity.type;
                        });
                        var isExistingCapacity = $.inArray(capacity.type, existingCapacityTypes) > -1;

                        if (!isExistingCapacity) {
                          zoneCapacities.push($.extend(capacity, {
                            zoneName: zone.name
                          }));
                        }
                      });
                    }
                  });
                });

                var sortFn = function(a, b) {
                  return parseInt(a.percentused) < parseInt(b.percentused);
                };

                complete($.extend(data, {
                  zoneCapacities: $.map(zoneCapacities.sort(sortFn), function(capacity) {
                    return {
                      zoneID: zones[0].id, // Temporary fix for dashboard
                      zoneName: capacity.zoneName,
                      type: cloudStack.converters.toAlertType(capacity.type),
                      percent: parseInt(capacity.percentused),
                      used: cloudStack.converters.convertByType(capacity.type, capacity.capacityused),
                      total: cloudStack.converters.convertByType(capacity.type, capacity.capacitytotal)
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
})(jQuery, cloudStack);
