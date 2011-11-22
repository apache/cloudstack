(function(cloudStack, testData, $) {
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

          reorder: {
            moveTop: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            },
            moveBottom: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            },
            moveUp: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            },
            moveDown: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            },
            moveDrag: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
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

          reorder: {
            moveTop: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            },
            moveBottom: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            },
            moveUp: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            },
            moveDown: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            },
            moveDrag: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            }
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

          reorder: {
            moveTop: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            },
            moveBottom: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            },
            moveUp: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            },
            moveDown: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            },
            moveDrag: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            }
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

          reorder: {
            moveTop: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            },
            moveBottom: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            },
            moveUp: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            },
            moveDown: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            },
            moveDrag: {
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 500);
              }
            }
          },

          actions: {
            add: {
              label: 'Add network offering',

              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 200);
              },

              createForm: {
                title: 'Add network offering',
                desc: 'Please specify the network offering',
                fields: {
                  name: { label: 'Name', validation: { required: true } },

                  displayText: { label: 'Display Text', validation: { required: true } },

                  maxConnections: { label: 'Max Connections' },

                  networkRate: { label: 'Network Rate' },

                  trafficType: {
                    label: 'Traffic Type', validation: { required: true },
                    select: function(args) {
                      args.response.success({
                        data: [
                          { id: 'GUEST', description: 'Guest' }
                        ]
                      });
                    }
                  },

                  guestType: {
                    label: 'Guest Type',
                    select: function(args) {
                      args.response.success({
                        data: [
                          { id: 'Isolated', description: 'Isolated' },
                          { id: 'Shared', description: 'Shared' }
                        ]
                      });
                    }
                  },

                  availability: {
                    label: 'Availability',
                    select: function(args) {
                      args.response.success({
                        data: [
                          { id: 'Required', description: 'Required' },
                          { id: 'Optional', description: 'Optional' },
                          { id: 'Unavailable', description: 'Unavailable' }
                        ]
                      });
                    }
                  },

                  serviceOfferingId: {
                    label: 'Service Offering',
                    select: function(args) {
                      args.response.success({
                        data: $.map(testData.data.serviceOfferings, function(elem) {
                          return {
                            id: elem.id,
                            description: elem.name
                          };
                        })
                      });
                    }
                  },

                  specifyVlan: { label: 'Specify VLAN', isBoolean: true },

                  vlanId: { label: 'VLAN ID', isHidden: true, dependsOn: 'specifyVlan'},

                  supportedServices: {
                    label: 'Supported Services',

                    dynamic: function(args) {
                      setTimeout(function() {
                        var fields = {};
                        var services = ['Vpn', 'Dhcp', 'Gateway', 'Firewall', 'Lb', 'UserData', 'SourceNat', 'StaticNat', 'PortForwarding', 'SecurityGroup'];

                        $(services).each(function() {
                          var id = {
                            isEnabled: this + '.' + 'isEnabled',
                            capabilities: this + '.' + 'capabilities',
                            provider: this + '.' + 'provider'
                          };

                          fields[id.isEnabled] = { label: this, isBoolean: true };
                          fields[id.provider] = {
                            label: this + ' Provider',
                            isHidden: true,
                            dependsOn: id.isEnabled,
                            select: function(args) {
                              args.response.success({
                                data: [
                                  { id: 'NetScaler', description: 'NetScaler'},
                                  { id: 'SRX', description: 'SRX' }
                                ]
                              });
                            }
                          };
                        });

                        args.response.success({
                          fields: fields
                        });
                      }, 50);
                    }
                  },

                  tags: { label: 'Tags' }
                }
              },

              notification: {
                poll: testData.notifications.testPoll
              },
              messages: {
                notification: function(args) {
                  return 'Added network offering';
                }
              }
            }
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
})(cloudStack, testData, jQuery);
