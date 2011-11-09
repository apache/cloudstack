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
            }
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
                args.response.success({data:items});
              }
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
            traffictype: { label: 'Traffic Type'}
          },
          dataProvider: function(args) {
            $.ajax({
              url: createURL("listNetworkOfferings&guestiptype=Virtual&page=" + args.page + "&pagesize=" + pageSize),
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listnetworkofferingsresponse.networkoffering;
                args.response.success({
                  actionFilter: networkOfferingsActionfilter,
                  data:items
                });
              }
            });
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
                    availability: { 
                      label: 'Availability',
                      isEditable: true,
                      select: function(args) {
                        var items = [];
                        items.push({id: 'Required', description: 'Required'});
                        items.push({id: 'Optional', description: 'Optional'});
                        items.push({id: 'Unavailable', description: 'Unavailable'});
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
                      actionFilter: networkOfferingsActionfilter,
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
  
  var networkOfferingsActionfilter = function(args) {
    var jsonObj = args.context.item;    
    var allowedActions = [];
    allowedActions.push("edit");
    return allowedActions;
  }
  
})(cloudStack, testData);
