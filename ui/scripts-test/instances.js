(function($, cloudStack) {
  cloudStack.sections.instances = {
    title: 'Instances',
    id: 'instances',
    listView: {
      section: 'instances',
      filters: {
        mine: { label: 'My instances' },
        all: { label: 'All instances' },
        running: { label: 'Running instances' },
        destroyed: { label: 'Destroyed instances' }
      },
      fields: {
        name: { label: 'Name', editable: true },
        account: { label: 'Account' },
        zonename: { label: 'Zone' },
        state: {
          label: 'Status',
          indicator: {
            'Running': 'on',
            'Stopped': 'off',
            'Destroyed': 'off'
          }
        }
      },

      // List view actions
      actions: {
        // Add instance wizard
        add: {
          label: 'Add instance',

          action: {
            custom: cloudStack.instanceWizard({
              steps: [
                // Step 1: Setup
                function(args) {
                  args.response.success({
                    data: {
                      zones: testData.data.zones
                    }
                  });
                },

                // Step 2: Select template
                function(args) {
                  args.response.success({
                    hypervisor: {
                      idField: 'id',
                      nameField: 'displayname'
                    },
                    data: {
                      templates: {
                        featuredtemplates: $.grep(testData.data.isos, function(elem) {
                          return elem.isfeatured === true;
                        }),
                        communitytemplates: [],
                        mytemplates: $.grep(testData.data.isos, function(elem) {
                          return elem.isfeatured === true;
                        }),
                        isos: $.grep(testData.data.isos, function(elem) {
                          return elem.isfeatured === false;
                        })
                      },
                      hypervisors: [
                        { id: 123, displayname: 'KVM' },
                        { id: 124, displayname: 'Xen' },
                        { id: 125, displayname: 'VMWare' }
                      ]
                    }
                  });
                },

                // Step 3: Service offering
                function(args) {
                  args.response.success({
                    data: {
                      serviceOfferings: testData.data.serviceOfferings
                    }
                  });
                },

                // Step 4: Data disk offering
                function(args) {
                  args.response.success({
                    required: true,
                    customFlag: 'iscustomized', // Field determines if custom slider is shown
                    data: {
                      diskOfferings: testData.data.diskOfferings
                    }
                  });
                },

                // Step 5: Network
                function(args) {
                  args.response.success({
                    type: 'select-network',
                    data: {
                      myNetworks: $.grep(testData.data.networks, function(elem) {
                        return elem.isdefault === true;
                      }),
                      sharedNetworks: $.grep(testData.data.networks, function(elem) {
                        return elem.isdefault === false;
                      }),
                      securityGroups: testData.data.securityGroups,
                      networkOfferings: testData.data.networkOfferings
                    }
                  });
                },

                // Step 6: Review
                function(args) {
                  args.response.success({});
                }
              ],
              action: function(args) {
                args.response.success({
                  _custom: { jobID: 12345 }
                });
              }
            })
          },

          messages: {
            confirm: function(args) {
              return 'Are you sure you want to add ' + args.name + '?';
            },
            notification: function(args) {
              return 'Creating new VM: ' + args.name;
            }
          },
          notification: {
            poll: testData.notifications.customPoll(
              testData.data.instances[1]
            )
          }
        },

        edit: {
          label: 'Edit instance name',
          action: function(args) {
            if ((args.data.name) == '') {
              args.response.error({ message: 'Instance name cannot be blank.' });
            } else {
              args.response.success();
            }
          }
        },

        restart: {
          label: 'Restart instance',
          action: function(args) {
            setTimeout(function() {
              args.response.success({
                data: {
                  state: 'Restarting'
                }
              });
            }, 1000);
          },
          messages: {
            confirm: function(args) {
              return 'Are you sure you want to restart ' + args.name + '?';
            },
            notification: function(args) {
              return 'Rebooting VM: ' + args.name;
            }
          },
          notification: {
            poll: testData.notifications.customPoll({
              state: 'Running'
            })
          }
        },
        stop: {
          label: 'Stop instance',
          action: function(args) {
            setTimeout(function() {
              args.response.success({
                data: { state: 'Stopping' }
              });
            }, 500);
          },
          messages: {
            confirm: function(args) {
              return 'Are you sure you want to shutdown ' + args.name + '?';
            },
            notification: function(args) {
              return 'Rebooting VM: ' + args.name;
            }
          },
          notification: {
            poll: testData.notifications.customPoll({
              state: 'Stopped'
            })
          }
        },
        start: {
          label: 'Start instance',
          action: function(args) {
            setTimeout(function() {
              args.response.success({
                data: { state: 'Starting' }
              });
            }, 500);
          },
          messages: {
            confirm: function(args) {
              return 'Are you sure you want to start ' + args.name + '?';
            },
            notification: function(args) {
              return 'Starting VM: ' + args.name;
            }
          },
          notification: {
            poll: testData.notifications.customPoll({
              state: 'Running'
            })
          }
        },
        destroy: {
          label: 'Destroy instance',
          messages: {
            confirm: function(args) {
              return 'Are you sure you want to destroy ' + args.name + '?';
            },
            notification: function(args) {
              return 'Destroyed VM: ' + args.name;
            }
          },
          action: function(args) {
            setTimeout(function() {
              args.response.success({ data: { state: 'Destroying' }});
            }, 200);
          },
          notification: {
            poll: testData.notifications.customPoll({
              state: 'Destroyed'
            })
          }
        }
      },
      dataProvider: testData.dataProvider.listView('instances'),
      detailView: {
        name: 'Instance details',
        viewAll: { path: 'storage.volumes', label: 'Volumes' },

        // Detail view actions
        actions: {
          edit: {
            label: 'Edit VM details', action: function(args) {
              setTimeout(function() {
                args.response.success();
              }, 500);
            },
            notification: {
              poll: testData.notifications.testPoll
            }
          },
          viewConsole: {
            label: 'View VM console',
            action: {
              externalLink: {
                url: function(args) {
                  return 'http://localhost:8080/client/console?cmd=access&vm=' +
                    args.context.instances[0].id;
                },
                title: function(args) {
                  return args.context.instances[0].displayname + ' console';
                },
                width: 800,
                height: 600
              }
            }
          },
          stop: {
            label: 'Stop VM',
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to stop ' + args.name + '?';
              },
              notification: function(args) {
                return 'Stopping VM: ' + args.name;
              }
            },
            notification: {
              poll: testData.notifications.customPoll({
                state: 'Stopped'
              })
            },
            action: function(args) {
              setTimeout(function() {
                args.response.success({
                  _custom: { jobID: args.data.id }
                });
              }, 1000);
            }
          },
          start: {
            label: 'Start VM',
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to start ' + args.name + '?';
              },
              notification: function(args) {
                return 'Starting VM: ' + args.name;
              }
            },
            notification: {
              poll: testData.notifications.customPoll({
                state: 'Running'
              })
            },
            action: function(args) {
              setTimeout(function() {
                args.response.success({
                  _custom: { jobID: args.data.id }
                });
              }, 1000);
            }
          },
          restart: {
            label: 'Restart VM',
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to restart ' + args.name + '?';
              },
              notification: function(args) {
                return 'Rebooting VM: ' + args.name;
              }
            },
            notification: {
              poll: testData.notifications.testPoll
            },
            action: function(args) {
              setTimeout(function() {
                args.response.success({
                  _custom: { jobID: args.data.id }
                });
              }, 1000);
            }
          },
          destroy: {
            label: 'Destroy VM',
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to destroy ' + args.name + '?';
              },
              notification: function(args) {
                return 'Destroying VM: ' + args.name;
              }
            },
            notification: {
              poll: testData.notifications.customPoll({
                state: 'Destroyed'
              })
            },
            action: function(args) {
              setTimeout(function() {
                args.response.success();
              }, 1000);
            }
          },
          migrate: {
            notification: {
              desc: 'Migrated VM',
              poll: testData.notifications.testPoll
            },
            label: 'Migrate VM', action: function(args) {
              args.response.success();
            }
          },
          attachISO: {
            label: 'Attach ISO', action: function(args) {
              args.response.success();
            }
          },
          resetPassword: {
            label: 'Reset password',
            action: function(args) {
              args.response.success({});
            },
            messages: {
              confirm: function(args) {
                return 'Do you really want to reset your password?';
              },
              notification: function(args) {
                return 'Resetting VM password';
              },
              complete: function(args) {
                return 'VM password reset. New password is: ' + args.password;
              }
            },
            notification: {
              poll: testData.notifications.customPoll({
                password: '1284018jaj#'
              })
            }
          },
          changeService: {
            label: 'Change Service', action: function(args) {
              args.response.success();
            }
          }
        },
        tabs: {
          // Details tab
          details: {
            title: 'Details',
            fields: [
              {
                name: {
                  label: 'Name', isEditable: true
                }
              },
              {
                id: { label: 'ID', isEditable: false },
                zonename: { label: 'Zone', isEditable: false },
                templateid: {
                  label: 'Template type',
                  isEditable: true,
                  select: function(args) {
                    var items = [];

                    $(testData.data.templates).each(function() {
                      items.push({ id: this.id, description: this.name });
                    });
                    setTimeout(function() {
                      args.response.success({ data: items });
                    }, 500);
                  }
                },
                serviceofferingname: { label: 'Service offering', isEditable: false },
                group: { label: 'Group', isEditable: true }
              }
            ],
            dataProvider: testData.dataProvider.detailView('instances')
          },

          /**
           * NICs tab
           */
          nics: {
            title: 'NICs',
            isMultiple: true,
            preFilter: function(args) {
              return ['type'];
            },
            fields: [
              {
                name: { label: 'Name', header: true },
                ipaddress: { label: 'IP Address' },
                gateway: { label: 'Default gateway' },
                netmask: { label: 'Netmask' },
                type: { label: 'Type' }
              }
            ],
            dataProvider: function(args) {
              setTimeout(function() {
                var instance = $.grep(testData.data.instances, function(elem) {
                  return elem.id == args.id;
                });
                args.response.success({
                  data: $.map(instance[0].nic, function(item, index) {
                    item.name = 'NIC ' + (index + 1);
                    return item;
                  })
                });
              }, 500);
            }
          },

          /**
           * Statistics tab
           */
          stats: {
            title: 'Statistics',
            fields: {
              cpuspeed: { label: 'Total CPU' },
              cpuused: { label: 'CPU Utilized' },
              networkkbsread: { label: 'Network Read' },
              networkkbswrite: { label: 'Network Write' }
            },
            dataProvider: testData.dataProvider.detailView('instances')
          }
        }
      }
    }
  };
})(jQuery, cloudStack);
