(function(cloudStack, testData, $) {
  cloudStack.sections.network = {
    title: 'Network',
    id: 'network',
    sectionSelect: {
      label: 'Select view'
    },
    sections: {
      networks: {
        type: 'select',
        title: 'Networks',
        listView: {
          filters: {
            all: { label: 'All' },
            mine: { label: 'My network' }
          },
          fields: {
            displaytext: { label: 'Name' },
            traffictype: { label: 'Traffic Type' },
            gateway: { label: 'Gateway' },
            vlan: { label: 'VLAN' }
          },
          dataProvider: testData.dataProvider.listView('networks'),

          detailView: {
            name: 'Network details',
            viewAll: { path: 'network.ipAddresses', label: 'IP Addresses' },
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    displaytext: { label: 'Name' }
                  },
                  {
                    name: { label: 'Short name' },
                    traffictype: { label: 'Traffic Type' },
                    gateway: { label: 'Gateway' },
                    vlan: { label: 'VLAN' }
                  },
                  {
                    startip: { label: 'Start IP' },
                    endip: { label: 'End IP' }
                  }
                ],
                dataProvider: testData.dataProvider.detailView('networks')
              }
            }
          }
        }
      },
      ipAddresses: {
        type: 'select',
        title: 'IP Addresses',
        listView: {
          id: 'ipAddresses',
          label: 'IPs',
          filters: {
            allocated: { label: 'Allocated ' },
            mine: { label: 'My network' }
          },
          fields: {
            ipaddress: {
              label: 'IP',
              converter: function(text, item) {
                if (item.issourcenat) {
                  return text + ' [Source NAT]';
                }

                return text;
              }
            },
            zonename: { label: 'Zone' },
            vlanname: { label: 'VLAN' },
            networkid: { label: 'Network Type' },
            state: { label: 'State', indicator: { 'Allocated': 'on' } }
          },

          actions: {
            add: {
              label: 'Acquire new IP',

              action: function(args) {
                args.response.success();
              },

              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to add this new IP?';
                },
                notification: function(args) {
                  return 'Allocated IP';
                }
              },

              createForm: {
                title: 'Acquire new IP',
                desc: 'Please select a zone from which you want to acquire your new IP from.',
                fields: {
                  zonename: {
                    label: 'Zone',
                    select: function(args) {
                      setTimeout(function() {
                        args.response.success({
                          data: [
                            { id: 'San Jose', description: 'San Jose' },
                            { id: 'Chicago', description: 'Chicago' }
                          ]
                        });
                      }, 10);
                    }
                  }
                }
              },

              notification: {
                poll: testData.notifications.customPoll(testData.data.network[0])
              }
            },
            enableStaticNAT: {
              label: 'Enable static NAT',
              action: {
                noAdd: true,
                custom: cloudStack.uiCustom.enableStaticNAT({
                  listView: cloudStack.sections.instances,
                  action: function(args) {
                    args.response.success();
                  }
                })
              },
              messages: {
                notification: function(args) {
                  return 'Enabled Static NAT';
                }
              },
              notification: {
                poll: testData.notifications.customPoll({ isstaticnat: true })
              }
            },
            disableStaticNAT: {
              label: 'Disable static NAT',
              action: function(args) {
                args.response.success();
              },
              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to disable static NAT?';
                },
                notification: function(args) {
                  return 'Disabled Static NAT';
                }
              },
              notification: {
                poll: testData.notifications.customPoll({ isstaticnat: false })
              }
            }
          },
          dataProvider: testData.dataProvider.listView('network'),

          // Detail view
          detailView: {
            name: 'IP address detail',
            // Example tab filter
            tabFilter: function(args) {
              var disabledTabs = [];
              var ipAddress = args.context.ipAddresses[0];

              if (!ipAddress.issourcenat ||
                  (ipAddress.issourcenat && !ipAddress.vpnenabled)) {
                disabledTabs.push('vpn');
              }

              return disabledTabs;
            },
            actions: {
              enableStaticNAT: {
                label: 'Enable static NAT',
                action: {
                  noAdd: true,
                  custom: cloudStack.uiCustom.enableStaticNAT({
                    listView: cloudStack.sections.instances,
                    action: function(args) {
                      args.response.success();
                    }
                  })
                },
                messages: {
                  notification: function(args) {
                    return 'Enabled Static NAT';
                  }
                },
                notification: {
                  poll: testData.notifications.customPoll({ isstaticnat: true })
                }
              },
              disableStaticNAT: {
                label: 'Disable static NAT',
                action: function(args) {
                  args.response.success();
                },
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to disable static NAT?';
                  },
                  notification: function(args) {
                    return 'Disabled Static NAT';
                  }
                },
                notification: {
                  poll: testData.notifications.customPoll({ isstaticnat: false })
                }
              },
              enableVPN: {
                label: 'Enable VPN',
                action: function(args) {
                  args.response.success();
                },
                messages: {
                  confirm: function(args) {
                    return 'Please confirm that you want VPN enabled for this IP address.';
                  },
                  notification: function(args) {
                    return 'Enabled VPN';
                  },
                  complete: function(args) {
                    return 'VPN is now enabled for IP ' + args.publicip + '.'
                      + '<br/>Your IPsec pre-shared key is:<br/>' + args.presharedkey;
                  }
                },
                notification: {
                  poll: testData.notifications.customPoll({
                    publicip: '10.2.2.1',
                    presharedkey: '23fudh881ssx88199488PP!#Dwdw',
                    vpnenabled: true
                  })
                }
              },
              disableVPN: {
                label: 'Disable VPN',
                action: function(args) {
                  args.response.success();
                },
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to disable VPN?';
                  },
                  notification: function(args) {
                    return 'Disabled VPN';
                  }
                },
                notification: {
                  poll: testData.notifications.customPoll({ vpnenabled: false })
                }
              }
            },
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    ipaddress: { label: 'IP' }
                  },
                  {
                    state: { label: 'State' },
                    zonename: { label: 'Zone' },
                    vlanname: { label: 'VLAN' },
                    issourcenat: { label: 'Source NAT' }
                  }
                ],
                dataProvider: testData.dataProvider.detailView('network')
              },

              ipRules: {
                title: 'Configuration',
                custom: cloudStack.ipRules({
                  preFilter: function(args) {
                    if (args.context.ipAddresses[0].isstaticnat) {
                      return args.items; // All items filtered means static NAT
                    }

                    return [];
                  },

                  // Firewall rules
                  firewall: {
                    noSelect: true,
                    fields: {
                      'cidrlist': { edit: true, label: 'Source CIDR' },
                      'protocol': {
                        label: 'Protocol',
                        select: function(args) {
                          args.$select.change(function() {
                            var $inputs = args.$form.find('input');
                            var $icmpFields = $inputs.filter(function() {
                              var name = $(this).attr('name');

                              return $.inArray(name, [
                                'icmptype',
                                'icmpcode'
                              ]) > -1;
                            });
                            var $otherFields = $inputs.filter(function() {
                              var name = $(this).attr('name');

                              return name != 'icmptype' && name != 'icmpcode' && name != 'cidrlist';
                            });

                            if ($(this).val() == 'icmp') {
                              $icmpFields.attr('disabled', false);
                              $otherFields.attr('disabled', 'disabled');
                            } else {
                              $otherFields.attr('disabled', false);
                              $icmpFields.attr('disabled', 'disabled');
                            }
                          });

                          args.response.success({
                            data: [
                              { name: 'tcp', description: 'TCP' },
                              { name: 'udp', description: 'UDP' },
                              { name: 'icmp', description: 'ICMP' }
                            ]
                          });
                        }
                      },
                      'startport': { edit: true, label: 'Start Port' },
                      'endport': { edit: true, label: 'End Port' },
                      'icmptype': { edit: true, label: 'ICMP Type', isDisabled: true },
                      'icmpcode': { edit: true, label: 'ICMP Code', isDisabled: true },
                      'add-rule': {
                        label: 'Add',
                        addButton: true
                      }
                    },
                    add: {
                      label: 'Add',
                      action: function(args) {
                        setTimeout(function() {
                          args.response.success({
                            notification: {
                              label: 'Add firewall rule',
                              poll: testData.notifications.testPoll
                            }
                          });
                        }, 500);
                      }
                    },
                    actions: {
                      destroy: {
                        label: 'Remove Rule',
                        action: function(args) {
                          setTimeout(function() {
                            args.response.success({
                              notification: {
                                label: 'Remove firewall rule',
                                poll: testData.notifications.testPoll
                              }
                            });
                          }, 500);
                        }
                      }
                    },
                    dataProvider: function(args) {
                      setTimeout(function() {
                        args.response.success({
                          data: [
                            {
                              "id": 11,
                              "protocol": "icmp",
                              "ipaddressid": 4,
                              "ipaddress": "10.223.71.23",
                              "state": "Active",
                              "cidrlist": "0.0.0.0/0",
                              "icmptype": 2,
                              "icmpcode": 22
                            },
                            {
                              "id": 10,
                              "protocol": "udp",
                              "startport": "500",
                              "endport": "10000",
                              "ipaddressid": 4,
                              "ipaddress": "10.223.71.23",
                              "state": "Active",
                              "cidrlist": "0.0.0.0/24"
                            },
                            {
                              "id": 9,
                              "protocol": "tcp",
                              "startport": "20",
                              "endport": "200",
                              "ipaddressid": 4,
                              "ipaddress": "10.223.71.23",
                              "state": "Active",
                              "cidrlist": "0.0.0.0/24"
                            }
                          ]
                        });
                      }, 100);
                    }
                  },

                  staticNATDataProvider: function(args) {
                    args.response.success({
                      data: testData.data.networks[0]
                    });
                  },

                  vmDataProvider: function(args) {
                    args.response.success({
                      data: testData.data.instances[1]
                    });
                  },

                  vmDetails: cloudStack.sections.instances.listView.detailView,

                  staticNAT: {
                    noSelect: true,
                    fields: {
                      'protocol': {
                        label: 'Protocol',
                        select: function(args) {
                          args.response.success({
                            data: [
                              { name: 'tcp', description: 'TCP' },
                              { name: 'udp', description: 'UDP' }
                            ]
                          });
                        }
                      },
                      'startport': { edit: true, label: 'Start Port' },
                      'endport': { edit: true, label: 'End Port' },
                      'add-rule': {
                        label: 'Add',
                        addButton: true
                      }
                    },
                    add: {
                      label: 'Add',
                      action: function(args) {
                        setTimeout(function() {
                          args.response.success({
                            notification: {
                              label: 'Add static NAT rule',
                              poll: testData.notifications.testPoll
                            }
                          });
                        }, 500);
                      }
                    },
                    actions: {
                      destroy: {
                        label: 'Remove Rule',
                        action: function(args) {
                          setTimeout(function() {
                            args.response.success({
                              notification: {
                                label: 'Remove static NAT rule',
                                poll: testData.notifications.testPoll
                              }
                            });
                          }, 500);
                        }
                      }
                    },
                    dataProvider: function(args) {
                      setTimeout(function() {
                        args.response.success({
                          data: [
                            {
                              "id": 10,
                              "protocol": "udp",
                              "startport": "500",
                              "endport": "10000",
                              "ipaddressid": 4,
                              "ipaddress": "10.223.71.23",
                              "state": "Active",
                              "cidrlist": "0.0.0.0/24"
                            },
                            {
                              "id": 9,
                              "protocol": "tcp",
                              "startport": "20",
                              "endport": "200",
                              "ipaddressid": 4,
                              "ipaddress": "10.223.71.23",
                              "state": "Active",
                              "cidrlist": "0.0.0.0/24"
                            }
                          ]
                        });
                      }, 100);
                    }
                  },

                  // Load balancing rules
                  loadBalancing: {
                    listView: cloudStack.sections.instances,
                    multipleAdd: true,
                    fields: {
                      'name': { edit: true, label: 'Name' },
                      'publicport': { edit: true, label: 'Public Port' },
                      'privateport': { edit: true, label: 'Private Port' },
                      'algorithm': {
                        label: 'Algorithm',
                        select: function(args) {
                          args.response.success({
                            data: [
                              { name: 'roundrobin', description: 'Round-robin' },
                              { name: 'leastconn', description: 'Least connections' },
                              { name: 'source', description: 'Source' }
                            ]
                          });
                        }
                      },
                      'sticky': {
                        label: 'Sticky Policy',
                        custom: {
                          buttonLabel: 'Configure',
                          action: function(args) {
                            var success = args.response.success;
                            var fields = {
                              method: {
                                label: 'Stickiness method',
                                select: function(args) {
                                  var $select = args.$select;
                                  var $form = $select.closest('form');
                                  
                                  args.response.success({
                                    data: [
                                      {
                                        id: 'none',
                                        description: 'None'
                                      },
                                      {
                                        id: 'lb',
                                        description: 'LB-based'
                                      },
                                      {
                                        id: 'cookie',
                                        description: 'Cookie-based'
                                      },
                                      {
                                        id: 'source',
                                        description: 'Source-based'
                                      }
                                    ]
                                  }, 500);

                                  $select.change(function() {
                                    var value = $select.val();
                                    var showFields = [];

                                    switch (value) {
                                    case 'none':
                                      showFields = [];
                                      break;
                                    case 'lb':
                                      showFields = ['name', 'mode', 'nocache', 'indirect', 'postonly', 'domain'];
                                      break;
                                    case 'cookie':
                                      showFields = ['name', 'length', 'holdtime', 'request-learn', 'prefix', 'mode'];
                                      break;
                                    case 'source':
                                      showFields = ['tablesize', 'expire'];
                                      break;
                                    }

                                    $select.closest('.form-item').siblings('.form-item').each(function() {
                                      var $field = $(this);
                                      var id = $field.attr('rel');

                                      if ($.inArray(id, showFields) > -1) {
                                        $field.css('display', 'inline-block');
                                      } else {
                                        $field.hide();
                                      }
                                    });

                                    $select.closest(':ui-dialog').dialog('option', 'position', 'center');
                                  });
                                }
                              },
                              name: { label: 'Name', validation: { required: true }, isHidden: true },
                              mode: { label: 'Mode', isHidden: true },
                              length: { label: 'Length', validation: { required: true }, isHidden: true },
                              holdtime: { label: 'Hold Time', validation: { required: true }, isHidden: true },
                              tablesize: { label: 'Table size', isHidden: true },
                              expire: { label: 'Expire', isHidden: true },
                              requestlearn: { label: 'Request-Learn', isBoolean: true, isHidden: true },
                              prefix: { label: 'Prefix', isBoolean: true, isHidden: true },
                              nocache: { label: 'No cache', isBoolean: true, isHidden: true },
                              indirect: { label: 'Indirect', isBoolean: true, isHidden: true },
                              postonly: { label: 'Is post-only', isBoolean: true, isHidden: true },
                              domain: { label: 'Domain', isBoolean: true, isHidden: true }
                            };

                            if (args.data) {
                              var populatedFields = $.map(fields, function(field, id) {
                                return id;
                              });
                              
                              $(populatedFields).each(function() {
                                var id = this;
                                var field = fields[id];
                                var dataItem = args.data[id];

                                if (field.isBoolean) {
                                  field.isChecked = dataItem ? true : false;
                                } else {
                                  field.defaultValue = dataItem;
                                }
                              });
                            }

                            cloudStack.dialog.createForm({
                              form: {
                                title: 'Configure Sticky Policy',
                                desc: 'Please complete the following fields',
                                fields: fields
                              },
                              after: function(args) {
                                var data = cloudStack.serializeForm(args.$form);
                                success({
                                  data: $.extend(data, {
                                    _buttonLabel: data.method.toUpperCase()
                                  })
                                });
                              }
                            });
                          }
                        }
                      },
                      'add-vm': {
                        label: 'Add VMs',
                        addButton: true
                      }
                    },
                    add: {
                      label: 'Add VMs',
                      action: function(args) {
                        setTimeout(function() {
                          args.response.success({
                            notification: {
                              label: 'Add load balancing rule',
                              poll: testData.notifications.testPoll
                            }
                          });
                        }, 500);
                      }
                    },
                    actions: {
                      destroy:  {
                        label: 'Remove load balancing rule',
                        action: function(args) {
                          setTimeout(function() {
                            args.response.success({
                              notification: {
                                label: 'Remove load balancing rule',
                                poll: testData.notifications.testPoll
                              }
                            });
                          }, 500);
                        }
                      }
                    },
                    dataProvider: function(args) {
                      setTimeout(function() {
                        args.response.success({
                          data: [
                            {
                              "id": 13,
                              "name": "HTTP",
                              "publicipid": 4,
                              "publicip": "10.223.71.23",
                              "publicport": "80",
                              "privateport": "80",
                              "algorithm": "roundrobin",
                              "cidrlist": "",
                              "account": "admin",
                              "domainid": 1,
                              "domain": "ROOT",
                              "state": "Active",
                              "zoneid": 1,
                              _itemData: [
                                testData.data.instances[0],
                                testData.data.instances[1],
                                testData.data.instances[2],
                                testData.data.instances[3]
                              ],
                              sticky: {
                                _buttonLabel: 'lb'.toUpperCase(),
                                method: 'lb',
                                name: 'StickyTest',
                                mode: '123',
                                nocache: true,
                                indirect: false,
                                postonly: true,
                                domain: false
                              }
                            }
                          ]
                        });
                      }, 100);
                    }
                  },

                  // Port forwarding rules
                  portForwarding: {
                    listView: cloudStack.sections.instances,
                    fields: {
                      'private-ports': {
                        edit: true,
                        label: 'Private Ports',
                        range: ['privateport', 'privateendport']
                      },
                      'public-ports': {
                        edit: true,
                        label: 'Public Ports',
                        range: ['publicport', 'publicendport']
                      },
                      'protocol': {
                        label: 'Protocol',
                        select: function(args) {
                          args.response.success({
                            data: [
                              { name: 'tcp', description: 'TCP' },
                              { name: 'udp', description: 'UDP' }
                            ]
                          });
                        }
                      },
                      'add-vm': {
                        label: 'Add VM',
                        addButton: true
                      }
                    },
                    add: {
                      label: 'Add VM',
                      action: function(args) {
                        setTimeout(function() {
                          args.response.success({
                            notification: {
                              label: 'Add port forwarding rule',
                              poll: testData.notifications.testPoll
                            }
                          });
                        }, 500);
                      }
                    },
                    actions: {
                      destroy: {
                        label: 'Remove port forwarding rule',
                        action: function(args) {
                          setTimeout(function() {
                            args.response.success({
                              notification: {
                                label: 'Remove port forwarding rule',
                                poll: testData.notifications.testPoll
                              }
                            });
                          }, 500);
                        }
                      }
                    },
                    dataProvider: function(args) {
                      setTimeout(function() {
                        args.response.success({
                          data: [
                            {
                              "id": 12,
                              "privateport": "22",
                              "privateendport": "22",
                              "protocol": "tcp",
                              "publicport": "22",
                              "publicendport": "22",
                              "virtualmachineid": 10,
                              "virtualmachinename": "i-2-10-TEST",
                              "virtualmachinedisplayname": "i-2-10-TEST",
                              "ipaddressid": 4,
                              "ipaddress": "10.223.71.23",
                              "state": "Active",
                              "cidrlist": "",
                              _itemData: [
                                testData.data.instances[5]
                              ]
                            }
                          ]
                        });
                      }, 100);
                    }
                  }
                })
              },
              vpn: {
                title: 'VPN',
                custom: function(args) {
                  var ipAddress = args.context.networks[0].ipaddress;
                  var psk = '081XufGFmEDBAEfsfdXTNpramSZ';

                  return $('<div>').append(
                    $('<ul>').addClass('info')
                      .append(
                        // VPN IP
                        $('<li>').addClass('ip').html('Your VPN access is currently enabled and can be accessed via the IP: ')
                          .append($('<strong>').html(ipAddress))
                      )
                      .append(
                        // PSK
                        $('<li>').addClass('psk').html('Your IPSec pre-shared key is: ')
                          .append($('<strong>').html(psk))
                      )
                  ).multiEdit({
                    noSelect: true,
                    fields: {
                      'username': { edit: true, label: 'Username' },
                      'password': { edit: true, label: 'Password' },
                      'add-user': { addButton: true, label: 'Add user' }
                    },
                    add: {
                      label: 'Add user',
                      action: function(args) {
                        setTimeout(function() {
                          args.response.success({
                            notification: {
                              label: 'Add user to VPN',
                              poll: testData.notifications.testPoll
                            }
                          });
                        }, 500);
                      }
                    },
                    actions: {
                      destroy: {
                        label: 'Remove user',
                        action: function(args) {
                          setTimeout(function() {
                            args.response.success({
                              notification: {
                                label: 'Remove user from VPN',
                                poll: testData.notifications.testPoll
                              }
                            });
                          }, 500);
                        }
                      }
                    },
                    dataProvider: function(args) {
                      setTimeout(function() {
                        args.response.success({
                          data: []
                        });
                      }, 100);
                    }
                  });
                }
              }
            }
          }
        }
      },
      securityGroups: {
        type: 'select',
        title: 'Security Groups',
        listView: {
          id: 'securityGroups',
          label: 'Security Groups',
          fields: {
            name: { label: 'Name', editable: true },
            description: { label: 'Description' },
            domain: { label: 'Domain' },
            account: { label: 'Account' }
          },
          actions: {
            add: {
              label: 'Add security group',

              action: function(args) {
                args.response.success();
              },

              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to add ' + args.name + '?';
                },
                notification: function(args) {
                  return 'Created security group';
                }
              },

              createForm: {
                title: 'New security group',
                desc: 'Please name your security group.',
                fields: {
                  name: { label: 'Name' },
                  description: { label: 'Description' }
                }
              },

              notification: {
                poll: testData.notifications.testPoll
              }
            },
            destroy: {
              label: 'Delete security group',
              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to delete ' + args.name + '?';
                },
                notification: function(args) {
                  return 'Deleted security group: ' + args.name;
                }
              },
              action: function(args) {
                setTimeout(function() {
                  args.response.success();
                }, 200);
              },
              notification: {
                poll: testData.notifications.testPoll
              }
            }
          },
          dataProvider: testData.dataProvider.listView('securityGroups'),
          detailView: {
            name: 'Security group details',
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    name: { label: 'Name' }
                  },
                  {
                    domain: { label: 'Domain' },
                    account: { label: 'Account' }
                  }
                ],
                dataProvider: testData.dataProvider.detailView('securityGroups')
              },
              ingressRules: {
                title: 'Ingress Rules',
                custom: cloudStack.uiCustom.securityRules({
                  noSelect: true,
                  noHeaderActionsColumn: true,
                  fields: {
                    'protocol': {
                      label: 'Protocol',
                      select: function(args) {
                        args.$select.change(function() {
                          var $inputs = args.$form.find('th, td');
                          var $icmpFields = $inputs.filter(function() {
                            var name = $(this).attr('rel');

                            return $.inArray(name, [
                              'icmptype',
                              'icmpcode'
                            ]) > -1;
                          });
                          var $otherFields = $inputs.filter(function() {
                            var name = $(this).attr('rel');

                            return name != 'icmptype' &&
                              name != 'icmpcode' &&
                              name != 'protocol' &&
                              name != 'add-rule' &&
                              name != 'cidr' &&
                              name != 'accountname' &&
                              name != 'securitygroupname';
                          });

                          if ($(this).val() == 'icmp') {
                            $icmpFields.show();
                            $otherFields.hide();
                          } else {
                            $icmpFields.hide();
                            $otherFields.show();
                          }
                        });

                        args.response.success({
                          data: [
                            { name: 'tcp', description: 'TCP' },
                            { name: 'udp', description: 'UDP' },
                            { name: 'icmp', description: 'ICMP' }
                          ]
                        });
                      }
                    },
                    'startport': { edit: true, label: 'Start Port' },
                    'endport': { edit: true, label: 'End Port' },
                    'icmptype': { edit: true, label: 'ICMP Type', isHidden: true },
                    'icmpcode': { edit: true, label: 'ICMP Code', isHidden: true },
                    'cidr': { edit: true, label: 'CIDR', isHidden: true },
                    'accountname': {
                      edit: true,
                      label: 'Account, Security Group',
                      isHidden: true,
                      range: ['accountname', 'securitygroupname']
                    },
                    'add-rule': {
                      label: 'Add',
                      addButton: true
                    }
                  },
                  add: {
                    label: 'Add',
                    action: function(args) {
                      setTimeout(function() {
                        args.response.success({
                          notification: {
                            label: 'Add ingress rule',
                            poll: testData.notifications.testPoll
                          }
                        });
                      }, 500);
                    }
                  },
                  actions: {
                    destroy: {
                      label: 'Remove Rule',
                      action: function(args) {
                        setTimeout(function() {
                          args.response.success({
                            notification: {
                              label: 'Remove ingress rule',
                              poll: testData.notifications.testPoll
                            }
                          });
                        }, 500);
                      }
                    }
                  },
                  ignoreEmptyFields: true,
                  dataProvider: function(args) {
                    setTimeout(function() {
                      args.response.success({
                        data: []
                      });
                    }, 100);
                  }
                })
              }
            }
          }
        }
      }
    }
  };
})(cloudStack, testData, jQuery);
