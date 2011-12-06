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
            displaytext: { label: 'Description' }
          },

          actions: {
            add: {
              label: 'Add service offering',

              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to add a service offering?';
                },
                success: function(args) {
                  return 'Your new service offering is being created.';
                },
                notification: function(args) {
                  return 'Creating new service offering';
                },
                complete: function(args) {
                  return 'Service offering has been created successfully!';
                }
              },

              createForm: {
                title: 'Add service offering',
                fields: {
                  name: {
                    label: 'Name',
                    validation: { required: true }
                  },
                  description: {
                    label: 'Description',
                    validation: { required: true }
                  },
                  storageType: {
                    label: 'Storage type',
                    select: function(args) {
                      var items = [];
                      items.push({id: 'shared', description: 'shared'});
                      items.push({id: 'local', description: 'local'});
                      args.response.success({data: items});
                    }
                  },
                  cpuNumber: {
                    label: '# of CPU cores',
                    validation: {
                      required: true,
                      number: true
                    }
                  },
                  cpuSpeed: {
                    label: 'CPU (in MHz)',
                    validation: {
                      required: true,
                      number: true
                    }
                  },
                  memory: {
                    label: 'Memory (in MB)',
                    validation: {
                      required: true,
                      number: true
                    }
                  },
                  networkRate: {
                    label: 'Network rate',
                    validation: {
                      required: false, //optional
                      number: true
                    }
                  },
                  offerHA: {
                    label: 'Offer HA',
                    isBoolean: true,
                    isChecked: false
                  },
                  storageTags: {
                    label: 'Storage tags'
                  },
                  hostTags: {
                    label: 'Host tags'
                  },
                  cpuCap: {
                    label: 'CPU cap',
                    isBoolean: true,
                    isChecked: false
                  },
                  isPublic: {
                    label: 'Public',
                    isBoolean: true,
                    isReverse: true,
                    isChecked: true
                  },
                  domainId: {
                    label: 'Domain',
                    dependsOn: 'isPublic',
                    select: function(args) {
                      $.ajax({
                        url: createURL("listDomains"),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                          var items = [];
                          var domainObjs = json.listdomainsresponse.domain;
                          $(domainObjs).each(function(){
                            items.push({id: this.id, description: this.name});
                          });
                          args.response.success({data: items});
                        }
                      });
                    },
                    isHidden: true
                  }
                }
              },

              action: function(args) {
                var array1 = [];
                array1.push("&name=" + args.data.name);
                array1.push("&displaytext=" + todb(args.data.description));
                array1.push("&storageType=" + todb(args.data.storageType));
                array1.push("&cpuNumber=" + args.data.cpuNumber);
                array1.push("&cpuSpeed="+ args.data.cpuSpeed);
                array1.push("&memory=" + args.data.memory);

                if(args.data.networkRate != null && args.data.networkRate.length > 0)
                  array1.push("&networkrate=" + args.data.networkRate);

                array1.push("&offerha=" + (args.data.offerHA == "on"));

                if(args.data.storageTags != null && args.data.storageTags.length > 0)
                  array1.push("&tags=" + todb(args.data.storageTags));

                if(args.data.hostTags != null && args.data.hostTags.length > 0)
                  array1.push("&hosttags=" + todb(args.data.hostTags));

                array1.push("&limitcpuuse=" + (args.data.cpuCap == "on"));

                if(args.$form.find('.form-item[rel=domainId]').css("display") != "none")
                  array1.push("&domainid=" + args.data.domainId);

                $.ajax({
                  url: createURL("createServiceOffering&issystem=false"+array1.join("")),
                  dataType: "json",
                  async: true,
                  success: function(json) {
                    var item = json.createserviceofferingresponse.serviceoffering;
                    args.response.success({data: item});
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
              url: createURL("listServiceOfferings&issystem=false&page=" + args.page + "&pagesize=" + pageSize),
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listserviceofferingsresponse.serviceoffering;
                args.response.success({
                  actionFitler: serviceOfferingActionfilter,
                  data:items
                });
              }
            });
          },

          detailView: {
            name: 'Service offering details',
            actions: {
              edit: {
                label: 'Edit',
                action: function(args) {
                  var array1 = [];
                  array1.push("&name=" + todb(args.data.name));
                  array1.push("&displaytext=" + todb(args.data.displaytext));
                  $.ajax({
                    url: createURL("updateServiceOffering&id=" + args.context.serviceOfferings[0].id + array1.join("")),
                    dataType: "json",
                    success: function(json) {
                      var item = json.updateserviceofferingresponse.serviceoffering;
                      args.response.success({data: item});
                    }
                  });
                }
              },

              'delete': {
                label: 'Delete service offering',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to delete this service offering?';
                  },
                  success: function(args) {
                    return 'Service offering is being deleted.';
                  },
                  notification: function(args) {
                    return 'Deleting service offering';
                  },
                  complete: function(args) {
                    return 'Service offering has been deleted.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteServiceOffering&id=" + args.context.serviceOfferings[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      args.response.success();
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

            tabs: {
              details: {
                title: 'Details',

                fields: [
                  {
                    name: {
                      label: 'Name',
                      isEditable: true
                    }
                  },
                  {
                    id: { label: 'ID' },
                    displaytext: {
                      label: 'Description',
                      isEditable: true
                    },
                    storagetype: { label: 'Storage Type' },
                    cpunumber: { label: 'CPU number' },
                    cpuspeed: {
                      label: 'CPU speed',
                      converter: function(args) {
                        return cloudStack.converters.convertHz(args);
                      }
                    },
                    memory: {
                      label: 'Memory',
                      converter: function(args) {
                        return cloudStack.converters.convertBytes(args*1024*1024);
                      }
                    },
                    networkrate: { label: 'Network rate' },
                    offerha: {
                      label: 'Offer HA',
                      converter: cloudStack.converters.toBooleanText
                    },
                    limitcpuuse: {
                      label: 'CPU cap',
                      converter: cloudStack.converters.toBooleanText
                    },
                    tags: { label: 'Storage tags' },
                    hosttags: { label: 'Host tags' },
                    domain: { label: 'Domain' },
                    created: { label: 'Created' }
                  }
                ],

                dataProvider: function(args) {
                  args.response.success(
                    {
                      actionFilter: serviceOfferingActionfilter,
                      data:args.context.serviceOfferings[0]
                    }
                  );
                }
              }
            }
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
            displaytext: { label: 'Description' }
          },

          actions: {
            add: {
              label: 'Add system service offering',

              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to add a system service offering?';
                },
                success: function(args) {
                  return 'Your new system service offering is being created.';
                },
                notification: function(args) {
                  return 'Creating new system service offering';
                },
                complete: function(args) {
                  return 'System service offering has been created successfully!';
                }
              },

              createForm: {
                title: 'Add system service offering',
                fields: {
                  name: {
                    label: 'Name',
                    validation: { required: true }
                  },
                  description: {
                    label: 'Description',
                    validation: { required: true }
                  },
                  storageType: {
                    label: 'Storage type',
                    select: function(args) {
                      var items = [];
                      items.push({id: 'shared', description: 'shared'});
                      items.push({id: 'local', description: 'local'});
                      args.response.success({data: items});
                    }
                  },
                  cpuNumber: {
                    label: '# of CPU cores',
                    validation: {
                      required: true,
                      number: true
                    }
                  },
                  cpuSpeed: {
                    label: 'CPU (in MHz)',
                    validation: {
                      required: true,
                      number: true
                    }
                  },
                  memory: {
                    label: 'Memory (in MB)',
                    validation: {
                      required: true,
                      number: true
                    }
                  },
                  networkRate: {
                    label: 'Network rate',
                    validation: {
                      required: false, //optional
                      number: true
                    }
                  },
                  offerHA: {
                    label: 'Offer HA',
                    isBoolean: true,
                    isChecked: false
                  },
                  storageTags: {
                    label: 'Storage tags'
                  },
                  hostTags: {
                    label: 'Host tags'
                  },
                  cpuCap: {
                    label: 'CPU cap',
                    isBoolean: true,
                    isChecked: false
                  },
                  isPublic: {
                    label: 'Public',
                    isBoolean: true,
                    isReverse: true,
                    isChecked: true
                  },
                  domainId: {
                    label: 'Domain',
                    dependsOn: 'isPublic',
                    select: function(args) {
                      $.ajax({
                        url: createURL("listDomains"),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                          var items = [];
                          var domainObjs = json.listdomainsresponse.domain;
                          $(domainObjs).each(function(){
                            items.push({id: this.id, description: this.name});
                          });
                          args.response.success({data: items});
                        }
                      });
                    },
                    isHidden: true
                  }
                }
              },

              action: function(args) {
                var array1 = [];
                array1.push("&name=" + args.data.name);
                array1.push("&displaytext=" + todb(args.data.description));
                array1.push("&storageType=" + todb(args.data.storageType));
                array1.push("&cpuNumber=" + args.data.cpuNumber);
                array1.push("&cpuSpeed="+ args.data.cpuSpeed);
                array1.push("&memory=" + args.data.memory);

                if(args.data.networkRate != null && args.data.networkRate.length > 0)
                  array1.push("&networkrate=" + args.data.networkRate);

                array1.push("&offerha=" + (args.data.offerHA == "on"));

                if(args.data.storageTags != null && args.data.storageTags.length > 0)
                  array1.push("&tags=" + todb(args.data.storageTags));

                if(args.data.hostTags != null && args.data.hostTags.length > 0)
                  array1.push("&hosttags=" + todb(args.data.hostTags));

                array1.push("&limitcpuuse=" + (args.data.cpuCap == "on"));

                if(args.$form.find('.form-item[rel=domainId]').css("display") != "none")
                  array1.push("&domainid=" + args.data.domainId);

                $.ajax({
                  url: createURL("createServiceOffering&issystem=true"+array1.join("")),
                  dataType: "json",
                  async: true,
                  success: function(json) {
                    var item = json.createserviceofferingresponse.serviceoffering;
                    args.response.success({data: item});
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
              url: createURL("listServiceOfferings&issystem=true&page="+args.page+"&pagesize="+pageSize),
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listserviceofferingsresponse.serviceoffering;
                args.response.success({data:items});
              }
            });
          },

          detailView: {
            name: 'System service offering details',
            actions: {
              edit: {
                label: 'Edit',
                action: function(args) {
                  var array1 = [];
                  array1.push("&name=" + todb(args.data.name));
                  array1.push("&displaytext=" + todb(args.data.displaytext));
                  $.ajax({
                    url: createURL("updateServiceOffering&id=" + args.context.systemServiceOfferings[0].id + array1.join("")),
                    dataType: "json",
                    success: function(json) {
                      var item = json.updateserviceofferingresponse.serviceoffering;
                      args.response.success({data: item});
                    }
                  });
                }
              },

              'delete': {
                label: 'Delete system service offering',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to delete this system service offering?';
                  },
                  success: function(args) {
                    return 'System service offering is being deleted.';
                  },
                  notification: function(args) {
                    return 'Deleting system service offering';
                  },
                  complete: function(args) {
                    return 'System service offering has been deleted.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteServiceOffering&id=" + args.context.systemServiceOfferings[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      args.response.success();
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

            tabs: {
              details: {
                title: 'Details',

                fields: [
                  {
                    name: {
                      label: 'Name',
                      isEditable: true
                    }
                  },
                  {
                    id: { label: 'ID' },
                    displaytext: {
                      label: 'Description',
                      isEditable: true
                    },
                    storagetype: { label: 'Storage Type' },
                    cpunumber: { label: 'CPU number' },
                    cpuspeed: {
                      label: 'CPU speed',
                      converter: function(args) {
                        return cloudStack.converters.convertHz(args);
                      }
                    },
                    memory: {
                      label: 'Memory',
                      converter: function(args) {
                        return cloudStack.converters.convertBytes(args*1024*1024);
                      }
                    },
                    networkrate: { label: 'Network rate' },
                    offerha: {
                      label: 'Offer HA',
                      converter: cloudStack.converters.toBooleanText
                    },
                    limitcpuuse: {
                      label: 'CPU cap',
                      converter: cloudStack.converters.toBooleanText
                    },
                    tags: { label: 'Storage tags' },
                    hosttags: { label: 'Host tags' },
                    domain: { label: 'Domain' },
                    created: { label: 'Created' }
                  }
                ],

                dataProvider: function(args) {
                  args.response.success(
                    {
                      actionFilter: systemServiceOfferingActionfilter,
                      data:args.context.systemServiceOfferings[0]
                    }
                  );
                }
              }
            }
          }
        }
      },

      diskOfferings: {
        type: 'select',
        title: 'Disk',
        listView: {
          label: 'Disk Offerings',
          fields: {
            name: { label: 'Name' },
            displaytext: { label: 'Description' },
            disksize: {
              label: 'Disk Size',
              converter: function(args) {
                return args + " GB";
              }
            }
          },
          dataProvider: function(args) {
            $.ajax({
              url: createURL("listDiskOfferings&page="+args.page+"&pagesize="+pageSize),
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listdiskofferingsresponse.diskoffering;
                args.response.success({data:items});
              }
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
                    isChecked: false
                  },
                  disksize: {
                    label: 'Disk size (in GB)',
                    dependsOn: 'isCustomized',
                    validation: { required: true, number: true }
                  },
                  tags: {
                    label: 'Storage tags'
                  },
                  isPublic: {
                    label: 'Public',
                    isBoolean: true,
                    isReverse: true,
                    isChecked: true
                  },
                  domainId: {
                    label: 'Domain',
                    dependsOn: 'isPublic',
                    select: function(args) {
                      $.ajax({
                        url: createURL("listDomains"),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                          var items = [];
                          var domainObjs = json.listdomainsresponse.domain;
                          $(domainObjs).each(function(){
                            items.push({id: this.id, description: this.name});
                          });
                          args.response.success({data: items});
                        }
                      });
                    },
                    isHidden: true
                  }
                }
              },

              action: function(args) {
                var array1 = [];
                array1.push("&name=" + args.data.name);
                array1.push("&displaytext=" + todb(args.data.description));

                array1.push("&customized=" + (args.data.isCustomized=="on"));
                if(args.$form.find('.form-item[rel=disksize]').css("display") != "none")
                  array1.push("&disksize=" + args.data.disksize);

                if(args.data.tags != null && args.data.tags.length > 0)
                  array1.push("&tags=" + todb(args.data.tags));

                if(args.$form.find('.form-item[rel=domainId]').css("display") != "none")
                  array1.push("&domainid=" + args.data.domainId);

                $.ajax({
                  url: createURL("createDiskOffering&isMirrored=false" + array1.join("")),
                  dataType: "json",
                  async: true,
                  success: function(json) {
                    var item = json.creatediskofferingresponse.diskoffering;
                    args.response.success({data: item});
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
            name: 'Disk offering details',
            actions: {
              edit: {
                label: 'Edit',
                action: function(args) {
                  var array1 = [];
                  array1.push("&name=" + todb(args.data.name));
                  array1.push("&displaytext=" + todb(args.data.displaytext));
                  $.ajax({
                    url: createURL("updateDiskOffering&id=" + args.context.diskOfferings[0].id + array1.join("")),
                    dataType: "json",
                    success: function(json) {
                      var item = json.updatediskofferingresponse.diskoffering;
                      args.response.success({data: item});
                    }
                  });
                }
              },

              'delete': {
                label: 'Delete disk offering',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to delete this disk offering?';
                  },
                  success: function(args) {
                    return 'Disk offering is being deleted.';
                  },
                  notification: function(args) {
                    return 'Deleting disk offering';
                  },
                  complete: function(args) {
                    return 'Disk offering has been deleted.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteDiskOffering&id=" + args.context.diskOfferings[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      args.response.success();
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

            tabs: {
              details: {
                title: 'Details',

                fields: [
                  {
                    name: {
                      label: 'Name',
                      isEditable: true
                    }
                  },
                  {
                    id: { label: 'ID' },
                    displaytext: {
                      label: 'Description',
                      isEditable: true
                    },
                    iscustomized: {
                      label: 'Customized',
                      converter: cloudStack.converters.toBooleanText
                    },
                    disksize: {
                      label: 'Disk Size',
                      converter: function(args) {
                        return args + " GB";
                      }
                    },
                    tags: { label: 'Storage tags' },
                    domain: { label: 'Domain' }
                  }
                ],

                dataProvider: function(args) {
                  args.response.success(
                    {
                      actionFilter: diskOfferingActionfilter,
                      data:args.context.diskOfferings[0]
                    }
                  );
                }
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
            name: { label: 'Name' },
            displaytext: { label: 'Description' },
            traffictype: { label: 'Traffic Type'}
          },
          dataProvider: function(args) {
            $.ajax({
              url: createURL('listNetworkOfferings'),
              data: {
                page: args.page,
                pagesize: pageSize
              },
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listnetworkofferingsresponse.networkoffering;
                args.response.success({
                  actionFilter: networkOfferingActionfilter,
                  data:items
                });
              }
            });
          },

          actions: {
            add: {
              label: 'Add network offering',

              action: function(args) {
                var formData = args.data;
                var inputData = {};
                var services = {};

                $.each(formData, function(key, value) {
                  var serviceData = key.split('.');

                  if (serviceData.length > 1) {
                    if (serviceData[0] == 'service' &&
                        serviceData[2] == 'isEnabled' &&
                        value == 'on') { // Services field
                        services[serviceData[1]] = formData[
                          'service.' + serviceData[1] + '.provider'
                        ];
                    } 
                  } else if (value != '') { // Normal data
                    inputData[key] = value;
                  }
                });

                // Make supported services list
                inputData['supportedServices'] = $.map(services, function(value, key) {
                  return key;
                }).join(',');

                if (inputData['specifyVlan'] == 'on') {
                  inputData['specifyVlan'] = true;
                } else {
                  inputData['specifyVlan'] = false;
                }

                // Make service provider map
                var serviceProviderIndex = 0;
                $.each(services, function(key, value) {
                  inputData['serviceProviderList[' + serviceProviderIndex + '].service'] = key;
                  inputData['serviceProviderList[' + serviceProviderIndex + '].provider'] = value;
                  serviceProviderIndex++;
                });

                $.ajax({
                  url: createURL('createNetworkOffering'),
                  data: inputData,
                  dataType: 'json',
                  async: true,
                  success: function(data) {
                    args.response.success({ data: data.createnetworkofferingresponse.networkoffering });
                  }
                });
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

                  guestIpType: {
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
                          { id: 'Optional', description: 'Optional' },
                          { id: 'Required', description: 'Required' },
                          { id: 'Unavailable', description: 'Unavailable' }
                        ]
                      });
                    }
                  },

                  serviceOfferingId: {
                    label: 'Service Offering',
                    select: function(args) {
                      $.ajax({
                        url: createURL('listServiceOfferings&issystem=true'),
                        dataType: 'json',
                        async: true,
                        success: function(data) {
                          var serviceOfferings = data.listserviceofferingsresponse.serviceoffering;

                          args.response.success({
                            data: $.merge(
                              [{
                                id: null,
                                description: 'None'
                              }],
                              $.map(serviceOfferings, function(elem) {
                                return {
                                  id: elem.id,
                                  description: elem.name
                                };
                              })
                            )
                          });                          
                        }
                      });
                    }
                  },

                  specifyVlan: { label: 'Specify VLAN', isBoolean: true },

                  vlanId: { label: 'VLAN ID', isHidden: true, dependsOn: 'specifyVlan'},

                  supportedServices: { 
                    label: 'Supported Services',

                    dynamic: function(args) {
                      $.ajax({
                        url: createURL('listSupportedNetworkServices'),
                        dataType: 'json',
                        async: true,
                        success: function(data) {
                          var networkServices = data.listsupportednetworkservicesresponse.networkservice;
                          var fields = {};
                          $(networkServices).each(function() {
                            var name = this.name;
                            var providers = this.provider;
                            
                            var id = {
                              isEnabled: 'service' + '.' + name + '.' + 'isEnabled',
                              capabilities: 'service' + '.' + name + '.' + 'capabilities',
                              provider: 'service' + '.' + name + '.' + 'provider'
                            };

                            fields[id.isEnabled] = { label: name, isBoolean: true };
                            fields[id.provider] = { 
                              label: name + ' Provider',
                              isHidden: true,
                              dependsOn: id.isEnabled,
                              select: function(args) {
                                args.response.success({
                                  data: $.map(providers, function(provider) {
                                    return {
                                      id: provider.name,
                                      description: provider.name
                                    };
                                  })
                                });
                              }
                            };
                          });

                          args.response.success({
                            fields: fields
                          });
                        }
                      });
                    }
                  },

                  tags: { label: 'Tags' }
                }
              },
              
              messages: {
                notification: function(args) {
                  return 'Added network offering';
                }
              }
            }
          },

          detailView: {
            name: 'Network offering details',
            actions: {
              edit: {
                label: 'Edit',
                action: function(args) {
                  var array1 = [];
                  array1.push("&displayText=" + todb(args.data.displaytext));
                  array1.push("&availability=" + todb(args.data.availability));
                  $.ajax({
                    url: createURL("updateNetworkOffering&id=" + args.context.networkOfferings[0].id + array1.join("")),
                    dataType: "json",
                    success: function(json) {
                      var item = json.updatenetworkofferingresponse.networkoffering;
                      args.response.success({ data:item });
                    }
                  });
                }
              },
              //???
              enable: {
                label: 'Enable network offering',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to enable this network offering?';
                  },
                  success: function(args) {
                    return 'This network offering is being enabled.';
                  },
                  notification: function(args) {
                    return 'Enabling network offering';
                  },
                  complete: function(args) {
                    return 'Network offering has been enabled.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkOffering&id=" + args.context.networkOfferings[0].id + "&state=Enabled"),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var item = json.updatenetworkofferingresponse.networkoffering;
                      args.response.success({
                        actionFilter: networkOfferingActionfilter,
                        data:item
                      });
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
                label: 'Disable network offering',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to disable this network offering?';
                  },
                  success: function(args) {
                    return 'This network offering is being disabled.';
                  },
                  notification: function(args) {
                    return 'Disabling network offering';
                  },
                  complete: function(args) {
                    return 'Network offering has been disabled.';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("updateNetworkOffering&id=" + args.context.networkOfferings[0].id + "&state=Disabled"),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var item = json.updatenetworkofferingresponse.networkoffering;
                      args.response.success({
                        actionFilter: networkOfferingActionfilter,
                        data:item
                      });
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
            tabs: {
              details: {
                title: 'Details',

                fields: [
                  {
                    name: {
                      label: 'Name'
                    }
                  },
                  {
                    id: { label: 'ID' },
                    displaytext: {
                      label: 'Description',
                      isEditable: true
                    },
                    state: { label: 'State' },
                    availability: {
                      label: 'Availability',
                      isEditable: true,
                      select: function(args) {
                        var items = [];
                        items.push({id: 'Required', description: 'Required'});
                        items.push({id: 'Optional', description: 'Optional'});
                        //items.push({id: 'Unavailable', description: 'Unavailable'});
                        args.response.success({data: items});
                      }
                    },
                    isdefault: {
                      label: 'Default',
                      converter:cloudStack.converters.toBooleanText
                    },
                    specifyvlan: {
                      label: 'Specify VLAN',
                      converter:cloudStack.converters.toBooleanText
                    },
                    networkrate: {
                      label: 'Network rate',
                      converter: function(args) {
                        var networkRate = args;
                        if (args == null || args == -1) {
                          return "Unlimited";
                        }
                        else {
                          return fromdb(args) + " Mb/s";

                        }
                      }
                    },
                    traffictype: {
                      label: 'Traffic type'
                    }
                  }
                ],

                dataProvider: function(args) {
                  args.response.success(
                    {
                      actionFilter: networkOfferingActionfilter,
                      data:args.context.networkOfferings[0]
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

  var serviceOfferingActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];
    allowedActions.push("edit");
    allowedActions.push("delete");
    return allowedActions;
  };

  var systemServiceOfferingActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];
    allowedActions.push("edit");
    allowedActions.push("delete");
    return allowedActions;
  };

  var diskOfferingActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];
    allowedActions.push("edit");
    allowedActions.push("delete");
    return allowedActions;
  };

  var networkOfferingActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];
    allowedActions.push("edit");
    if(jsonObj.state == "Enabled")
      allowedActions.push("disable");
    else if(jsonObj.state == "Disabled")
      allowedActions.push("enable");
    return allowedActions;
  };

})(cloudStack, testData, jQuery);
