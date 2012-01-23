(function($, cloudStack, testData) {
  cloudStack.projects = {
    requireInvitation: function(args) {
      return cloudStack.context.users[0].username == 'jdoe';
    },

    dashboard: function(args) {
      var dataFns = {
        instances: function(data) {
          dataFns.storage($.extend(data, {
            runningInstances: 40,
            stoppedInstances: 10,
            totalInstances: 50
          }));
        },

        storage: function(data) {
          dataFns.bandwidth($.extend(data, {
            totalVolumes: 70
          }));
        },

        bandwidth: function(data) {
          dataFns.ipAddresses($.extend(data, {
            totalBandwidth: 1500
          }));
        },

        ipAddresses: function(data) {
          dataFns.loadBalancingRules($.extend(data, {
            totalIPAddresses: 20
          }));
        },

        loadBalancingRules: function(data) {
          dataFns.portForwardingRules($.extend(data, {
            totalLoadBalancers: 12
          }));
        },

        portForwardingRules: function(data) {
          dataFns.users($.extend(data, {
            totalPortForwards: 30
          }));
        },

        users: function(data) {
          dataFns.events($.extend(data, {
            users: $.map(testData.data.users, function(user) {
              return {
                account: user.account
              };
            })
          }));
        },

        events: function(data) {
          complete($.extend(data, {
            events: $.map(testData.data.events, function(event) {
              return {
                date: event.created.substr(5, 2) +
                  '/' + event.created.substr(8, 2) +
                  '/' + event.created.substr(2, 2),
                desc: event.description
              };
            })
          }));
        }
      };

      var complete = function(data) {
        args.response.success({
          data: data
        });
      };

      dataFns.instances({});
    },

    resourceManagement: {
      update: function(args) {
        args.response.success();
      },

      dataProvider: function(args) {
        args.response.success({
          data: $.map(
            testData.data.projectResourceLimits,
            function(resource) {
              var resourceMap = {
                0: {
                  id: 'user_vm',
                  label: 'Max. User VMs'
                },
                1: {
                  id: 'public_ip',
                  label: 'Max. Public IPs'
                },
                2: {
                  id: 'volume',
                  label: 'Max. Volumes'
                },
                3: {
                  id: 'snapshot',
                  label: 'Max. Snapshots'
                },
                4: {
                  id: 'template',
                  label: 'Max. Templates'
                },
                5: {
                  id: 'project',
                  label: 'Max. Projects'
                }
              };
              return {
                id: resourceMap[resource.resourcetype].id,
                label: resourceMap[resource.resourcetype].label,
                type: resource.resourcetype,
                value: resource.max
              };
            }
          )
        });

      }
    },
    
    add: function(args) {
      setTimeout(function() {
        args.response.success({
          data: {
            name: args.data['project-name'],
            displayText: args.data['project-display-text'],
            users: []
          }
        });
      }, 1000);
    },
    inviteForm: {
      noSelect: true,
      fields: {
        'email': { edit: true, label: 'E-mail' },
        'add-user': { addButton: true, label: '' }
      },
      add: {
        label: 'Invite',
        action: function(args) {
          setTimeout(function() {
            args.response.success({
              data: args.data,
              notification: {
                label: 'Invited user to project',
                poll: testData.notifications.testPoll
              }
            });
          }, 100);
        }
      },
      actionPreFilter: function(args) {
        if (cloudStack.context.projects &&
            cloudStack.context.projects[0] &&
            !cloudStack.context.projects[0].isNew) {
          return args.context.actions;
        }

        return ['destroy'];
      },

      actions: {},

      // Project users data provider
      dataProvider: function(args) {
        var data = cloudStack.context.projects ?
              [
                { email: 'brian.federle@citrix.com' },
                { email: 'john.doe@aol.com' },
                { email: 'some.user@gmail.com' }
              ] : [];

        setTimeout(function() {
          args.response.success({
            data: data
          });
        }, 100);
      }
    },
    addUserForm: {
      noSelect: true,
      fields: {
        'username': { edit: true, label: 'Account' },
        'add-user': { addButton: true, label: '' }
      },
      add: {
        label: 'Add user',
        action: function(args) {
          setTimeout(function() {
            args.response.success({
              data: args.data,
              notification: {
                label: 'Added user to project',
                poll: testData.notifications.testPoll
              }
            });
          }, 100);
        }
      },
      actionPreFilter: function(args) {
        if (cloudStack.context.projects &&
            cloudStack.context.projects[0] &&
            !cloudStack.context.projects[0].isNew) {
          return args.context.actions;
        }

        return ['destroy'];
      },
      actions: {
        destroy: {
          label: 'Remove user from project',
          action: function(args) {
            setTimeout(function() {
              args.response.success({
                notification: {
                  label: 'Removed user from project',
                  poll: testData.notifications.testPoll
                }
              });
            }, 500);
          }
        },

        makeOwner: {
          label: 'Make user project owner',
          action: function(args) {
            setTimeout(function() {
              args.response.success({
                notification: {
                  label: 'Assigned new owner to project',
                  poll: testData.notifications.testPoll
                }
              });
            });
          }
        }
      },

      // Project users data provider
      dataProvider: function(args) {
        var data = cloudStack.context.projects ?
              cloudStack.context.projects[0].users : [];

        setTimeout(function() {
          args.response.success({
            data: data
          });
        }, 100);
      }
    },

    // Project listing data provider
    dataProvider: function(args) {
      var user = args.context.users[0];
      setTimeout(function() {
        args.response.success({
          data: user.username == 'bfederle' ? [] : testData.data.projects
        });
      }, 200);
    }
  };

  /**
   * Projects section -- list view
   */
  cloudStack.sections.projects = {
    title: 'Projects',
    id: 'projects',
    listView: {
      fields: {
        name: { label: 'Project Name' },
        displayText: { label: 'Display Text' },
        domain: { label: 'Domain' },
        account: { label: 'Owner' }
      },

      dataProvider: testData.dataProvider.listView('projects'),

      actions: {
        add: {
          label: 'New Project',
          action: {
            custom: function(args) {
              $(window).trigger('cloudStack.newProject');
            }
          },

          messages: {
            confirm: function(args) {
              return 'Are you sure you want to remove ' + args.name + '?';
            },
            notification: function(args) {
              return 'Removed project';
            }
          },

          notification: {
            poll: testData.notifications.testPoll
          }
        },

        destroy: {
          label: 'Remove project',
          action: function(args) {
            args.response.success({});
          },

          messages: {
            confirm: function(args) {
              return 'Are you sure you want to remove ' + args.name + '?';
            },
            notification: function(args) {
              return 'Removed project';
            }
          },

          notification: {
            poll: testData.notifications.testPoll
          }
        }
      }
    }
  };
} (jQuery, cloudStack, testData));
