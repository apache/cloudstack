(function(cloudStack, testData) {
  cloudStack.sections.configuration = {
    title: 'Configuration',
    id: 'configuration',
    sectionSelect: {
      label: 'Select Offering'
    },
    sections: {
      serviceOfferings: {
        type: 'select',
        title: 'Service',
        listView: {
          label: 'Service Offerings',
          fields: {
            name: { label: 'Name', editable: true },
            storagetype: { label: 'Storage Type' },
            cpuspeed: { label: 'CPU' },
            memory: { label: 'Memory' },
            domain: { label: 'Domain'}
          },
          actions: {
            add: {
              label: 'Add service offering',

              action: function(args) {
                args.response.success();
              },

              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to add ' + args.name + '?';
                },
                success: function(args) {
                  return 'Your new service offering is being created.';
                },
                notification: function(args) {
                  return 'Created service offering';
                },
                complete: function(args) {
                  return 'Service offering has been created';
                }
              },

              createForm: {
                title: 'New service offering',
                desc: 'Please fill in the following data to add a new service offering.',
                fields: {
                  name: { label: 'Name', editable: true },
                  displayText: { label: 'Display Text' },
                  storageType: {
                    label: 'Storage Type',
                    select: [
                      { id: 'shared', description: 'Shared' },
                      { id: 'local', description: 'Local' }
                    ]
                  },
                  cpuCores: { label: '# of CPU cores' },
                  cpuSpeed: { label: 'CPU Speed (in MHz)'},
                  memory: { label: 'Memory (in MB)' },
                  tags: { label: 'Tags' },
                  offerHA: { label: 'Offer HA', isBoolean: true },
                  isPublic: { label: 'Public', isBoolean: true }
                }
              },

              notification: {
                poll: testData.notifications.testPoll
              }
            }
          },
          dataProvider: function(args) {
            setTimeout(function() {
              args.response.success({
                data: testData.data.serviceOfferings
              });              
            });
          }
        }
      },

      systemServiceOfferings: {
        type: 'select',
        title: 'System Service',
        listView: {
          label: 'System Service Offerings',
          fields: {
            name: { label: 'Name', editable: true },
            storagetype: { label: 'Storage Type' },
            cpuspeed: { label: 'CPU' },
            memory: { label: 'Memory' },
            domain: { label: 'Domain'}
          },
          actions: {
            add: {
              label: 'Add system service offering',

              action: function(args) {
                args.response.success();
              },

              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to add ' + args.name + '?';
                },
                success: function(args) {
                  return 'Your new system service offering is being created.';
                },
                notification: function(args) {
                  return 'Created system service offering';
                },
                complete: function(args) {
                  return 'Sysmte service offering has been created';
                }
              },

              createForm: {
                title: 'New system service offering',
                desc: 'Please fill in the following data to add a new service offering.',
                fields: {
                  name: { label: 'Name', editable: true },
                  displayText: { label: 'Display Text' },
                  storageType: {
                    label: 'Storage Type',
                    select: [
                      { id: 'shared', description: 'Shared' },
                      { id: 'local', description: 'Local' }
                    ]
                  },
                  cpuCores: { label: '# of CPU cores' },
                  cpuSpeed: { label: 'CPU Speed (in MHz)'},
                  memory: { label: 'Memory (in MB)' },
                  tags: { label: 'Tags' },
                  offerHA: { label: 'Offer HA', isBoolean: true },
                  isPublic: { label: 'Public', isBoolean: true }
                }
              },

              notification: {
                poll: testData.notifications.testPoll
              }
            }
          },
          dataProvider: function(args) {
            setTimeout(function() {
              args.response.success({
                data: testData.data.systemServiceOfferings
              });              
            });
          }
        }
      },

      diskOfferings: {
        type: 'select',
        title: 'Disk',
        listView: {
          label: 'Disk Offerings',
          fields: {
            displaytext: { label: 'Name' },
            disksize: { label: 'Disk Size' },
            domain: { label: 'Domain'}
          },
          dataProvider: function(args) {
            setTimeout(function() {
              args.response.success({
                data: testData.data.diskOfferings
              });
            });
          },

          actions: {
            add: {
              label: 'Add disk offering',

              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to add a disk offering?';
                },
                success: function(args) {
                  return 'Your new disk offering is being created.';
                },
                notification: function(args) {
                  return 'Creating new disk offering';
                },
                complete: function(args) {
                  return 'Disk offering has been created successfully!';
                }
              },

              createForm: {
                title: 'Add disk offering',
                fields: {
                  name: {
                    label: 'Name',
                    validation: { required: true }
                  },
                  description: {
                    label: 'Description',
                    validation: { required: true }
                  },
                  isCustomized: {
                    label: 'Custom disk size',
                    isBoolean: true,
                    isReverse: true,
                    isChecked: true
                  },
                  disksize: {
                    label: 'Disk size (in GB)',
                    dependsOn: 'isCustomized',
                    validation: { required: true, number: true },
                    isHidden: true
                  },
                  tags: {
                    label: 'Storage tags'
                  },
                  isDomainSpecific: {
                    label: 'Domain specific',
                    isBoolean: true
                  },
                  domainId: {
                    label: 'Domain',
                    dependsOn: 'isDomainSpecific',
                    select: function(args) {
                      setTimeout(function() {
                        args.response.success({
                          descriptionField: 'name',
                          data: testData.data.domains
                        });
                      });
                    },
                    isHidden: true
                  }
                }
              },

              action: function(args) {
                args.response.success();
              }
            }
          }
        }
      },
      networkOfferings: {
        type: 'select',
        title: 'Network',
        listView: {
          label: 'Network Offerings',
          fields: {
            name: { label: 'Name', editable: true },
            networkrate: { label: 'Network Rate' },
            traffictype: { label: 'Traffic Type'}
          },
          dataProvider: function(args) {
            setTimeout(function() {
              args.response.success({
                data: testData.data.networkOfferings
              });
            });
          }
        }
      }
    }
  };
})(cloudStack, testData);
