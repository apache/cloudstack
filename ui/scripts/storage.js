(function(cloudStack, testData) {

  var diskofferingObjs, selectedDiskOfferingObj;

  cloudStack.sections.storage = {
    title: 'Storage',
    id: 'storage',
    sectionSelect: {
      label: 'Select view'
    },
    sections: {
      /**
       * Volumes
       */
      volumes: {
        type: 'select',
        title: 'Volumes',
        listView: {
          id: 'volumes',
          label: 'Volumes',
          fields: {
            name: { label: 'Name' },
            type: { label: 'Type' },
            storagetype: { label: 'Storage Type' },
            vmdisplayname: { label: 'VM Display Name' },
            state: { label: 'State', indicator: { 'Ready': 'on' } }
          },

          // List view actions
          actions: {
            // Add volume
            add: {
              label: 'Add volume',

              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to add a volume?';
                },
                notification: function(args) {
                  return 'create volume';
                }
              },

              createForm: {
                title: 'Add volume',
                desc: 'Please fill in the following data to add a new volume.',
                fields: {
                  name: {
                    label: 'Name',
                    validation: { required: true }
                  },
                  availabilityZone: {
                    label: 'Availability Zone',
                    select: function(args) {
                      $.ajax({
                        url: createURL("listZones&available=true"),
                        dataType: "json",
                        async: true,
                        success: function(json) {
                          var items = json.listzonesresponse.zone;
                          args.response.success({descriptionField: 'name', data: items});
                        }
                      });
                    }
                  },
                  diskOffering: {
                    label: 'Disk Offering',
                    select: function(args) {
                      $.ajax({
                        url: createURL("listDiskOfferings"),
                        dataType: "json",
                        async: false,
                        success: function(json) {
                          diskofferingObjs = json.listdiskofferingsresponse.diskoffering;
                          var items = [];
                          $(diskofferingObjs).each(function(){
                            items.push({id: this.id, description: this.displaytext});
                          });
                          args.response.success({data: items});
                        }
                      });

                      args.$select.change(function() {
                        var diskOfferingId = $(this).val();
                        $(diskofferingObjs).each(function(){
                          if(this.id == diskOfferingId) {
                            selectedDiskOfferingObj = this;
                            return false; //break the $.each() loop
                          }
                        });
                        if(selectedDiskOfferingObj == null)
                          return;

                        var $form = $(this).closest('form');
                        var $diskSize = $form.find('.form-item[rel=diskSize]');
                        if (selectedDiskOfferingObj.iscustomized == true) {
                          $diskSize.css('display', 'inline-block');
                        }
                        else {
                          $diskSize.hide();
                        }
                      });
                    }
                  }

                  ,
                  diskSize: {
                    label: 'Disk size (in GB)',
                    validation: { required: true, number: true },
                    isHidden: true
                  }

                }
              },

              action: function(args) {
                var array1 = [];
                array1.push("&name=" + args.data.name);
                array1.push("&zoneId=" + args.data.availabilityZone);
                array1.push("&diskOfferingId=" + args.data.diskOffering);

                // if(thisDialog.find("#size_container").css("display") != "none") { //wait for Brian to include $form in args
                if (selectedDiskOfferingObj.iscustomized == true) {
                  array1.push("&size=" + args.data.diskSize);
                }

                $.ajax({
                  url: createURL("createVolume" + array1.join("")),
                  dataType: "json",
                  async: true,
                  success: function(json) {
                    var jid = json.createvolumeresponse.jobid;
                    args.response.success(
                      {_custom:
                       {jobId: jid,
                        getUpdatedItem: function(json) {
                          return json.queryasyncjobresultresponse.jobresult.volume;
                        },
                        getActionFilter: function() {
                          return volumeActionfilter;
                        }
                       }
                      }
                    );
                  },
                  error: function(json) {
                    args.response.error(parseXMLHttpResponse(json));
                  }
                });
              },

              notification: {
                poll: pollAsyncJobResult
              }
            },
            
            recurringSnapshot: {
              label: 'Setup recurring snapshots',
              action: {
                custom: cloudStack.uiCustom.recurringSnapshots({
                  desc: 'You can setup recurring snapshot schedules by selecting from the available options below and applying your policy preference.',
                  dataProvider: function(args) {
                    $.ajax({
                      url: createURL('listSnapshotPolicies'),
                      data: {
                        volumeid: args.context.volumes[0].id
                      },
                      async: true,
                      dataType: 'json',
                      success: function(data) {
                        args.response.success({
                          data: $.map(
                            data.listsnapshotpoliciesresponse.snapshotpolicy ? data.listsnapshotpoliciesresponse.snapshotpolicy : [],
                            function(snapshot, index) {
                              return {
                                id: snapshot.id,
                                type: snapshot.intervaltype,
                                time: snapshot.intervaltype > 0 ?
                                  snapshot.schedule.split(':')[1] + ':' + snapshot.schedule.split(':')[0] :
                                  snapshot.schedule,
                                timezone: snapshot.timezone,
                                keep: snapshot.maxsnaps,
                                'day-of-week': snapshot.intervaltype == 2 ?
                                  snapshot.schedule.split(':')[2] : null,
                                'day-of-month': snapshot.intervaltype == 3 ?
                                  snapshot.schedule.split(':')[2] : null
                              };
                            }
                          )
                        });
                      }
                    });
                  },
                  actions: {
                    add: function(args) {
                      var snap = args.snapshot;

                      var data = {
                        keep: snap.maxsnaps,
                        timezone: snap.timezone
                      };

                      var convertTime = function(minute, hour, meridiem, extra) {
                        var convertedHour = meridiem == 'PM' ?
                              (hour != 12 ? parseInt(hour) + 12 : 12) : (hour != 12 ? hour : '00');
                        var time = minute + ':' + convertedHour;
                        if (extra) time += ':' + extra;

                        return time;
                      };

                      switch (snap['snapshot-type']) {
                      case 'hourly': // Hourly
                        $.extend(data, {
                          schedule: snap.schedule
                        }); break;

                      case 'daily': // Daily
                        $.extend(data, {
                          schedule: convertTime(
                            snap['time-minute'],
                            snap['time-hour'],
                            snap['time-meridiem']
                          )
                        }); break;

                      case 'weekly': // Weekly
                        $.extend(data, {
                          schedule: convertTime(
                            snap['time-minute'],
                            snap['time-hour'],
                            snap['time-meridiem'],
                            snap['day-of-week']
                          )
                        }); break;

                      case 'monthly': // Monthly
                        $.extend(data, {
                          schedule: convertTime(
                            snap['time-minute'],
                            snap['time-hour'],
                            snap['time-meridiem'],
                            snap['day-of-month']
                          )
                        }); break;
                      }

                      $.ajax({
                        url: createURL('createSnapshotPolicy'),
                        data: {
                          volumeid: args.context.volumes[0].id,
                          intervaltype: snap['snapshot-type'],
                          maxsnaps: snap.maxsnaps,
                          schedule: data.schedule,
                          timezone: snap.timezone
                        },
                        dataType: 'json',
                        async: true,
                        success: function(successData) {
                          var snapshot = successData.createsnapshotpolicyresponse.snapshotpolicy;

                          args.response.success({
                            data: {
                              id: snapshot.id,
                              type: snapshot.intervaltype,
                              time: snapshot.intervaltype > 0 ?
                                snapshot.schedule.split(':')[1] + ':' + snapshot.schedule.split(':')[0] :
                                snapshot.schedule,
                              timezone: snapshot.timezone,
                              keep: snapshot.maxsnaps,
                              'day-of-week': snapshot.intervaltype == 2 ?
                                snapshot.schedule.split(':')[2] : null,
                              'day-of-month': snapshot.intervaltype == 3 ?
                                snapshot.schedule.split(':')[2] : null
                            }
                          });
                        }
                      });
                    },
                    remove: function(args) {
                      $.ajax({
                        url: createURL('deleteSnapshotPolicies'),
                        data: {
                          id: args.snapshot.id
                        },
                        dataType: 'json',
                        async: true,
                        success: function(data) {
                          args.response.success();
                        }
                      });
                    }
                  },

                  // Select data
                  selects: {
                    schedule: function(args) {
                      var time = [];

                      for (var i = 1; i <= 59; i++) {
                        time.push({
                          id: i,
                          name: i
                        });
                      }

                      args.response.success({
                        data: time
                      });
                    },
                    timezone: function(args) {
                      args.response.success({
                        data: $.map(timezoneMap, function(value, key) {
                          return {
                            id: key,
                            name: value
                          };
                        })
                      });
                    },
                    'day-of-week': function(args) {
                      args.response.success({
                        data: [
                          { id: 1, name: 'Sunday' },
                          { id: 2, name: 'Monday' },
                          { id: 3, name: 'Tuesday' },
                          { id: 4, name: 'Wednesday' },
                          { id: 5, name: 'Thursday' },
                          { id: 6, name: 'Friday' },
                          { id: 7, name: 'Saturday' }
                        ]
                      });
                    },

                    'day-of-month': function(args) {
                      var time = [];

                      for (var i = 1; i <= 31; i++) {
                        time.push({
                          id: i,
                          name: i
                        });
                      }

                      args.response.success({
                        data: time
                      });
                    },

                    'time-hour': function(args) {
                      var time = [];

                      for (var i = 1; i <= 12; i++) {
                        time.push({
                          id: i,
                          name: i
                        });
                      }

                      args.response.success({
                        data: time
                      });
                    },

                    'time-minute': function(args) {
                      var time = [];

                      for (var i = 0; i <= 59; i++) {
                        time.push({
                          id: i < 10 ? '0' + i : i,
                          name: i < 10 ? '0' + i : i
                        });
                      }

                      args.response.success({
                        data: time
                      });
                    },

                    'time-meridiem': function(args) {
                      args.response.success({
                        data: [
                          { id: 'AM', name: 'AM' },
                          { id: 'PM', name: 'PM' }
                        ]
                      });
                    }
                  }
                })
              },
              messages: {
                notification: function(args) {
                  return 'Setup recurring snapshot';
                }
              },
              notification: {
                poll: testData.notifications.testPoll
              }
            }            

          },

          dataProvider: function(args) {					  
						var array1 = [];  
						if(args.filterBy != null) {          
							if(args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
								switch(args.filterBy.search.by) {
								case "name":
									if(args.filterBy.search.value.length > 0)
										array1.push("&keyword=" + args.filterBy.search.value);
									break;
								}
							}
						}
											
            var apiCmd = "listVolumes&page=" + args.page + "&pagesize=" + pageSize+ array1.join("");
            if(args.context != null) {
              if("instances" in args.context) {
                apiCmd += "&virtualMachineId=" + args.context.instances[0].id;
              }
            }

            $.ajax({
              url: createURL(apiCmd),
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listvolumesresponse.volume;
                args.response.success({
                  actionFilter: volumeActionfilter,
                  data: items
                });
              }
            });
          },

          detailView: {
            name: 'Volume details',
            viewAll: { path: 'storage.snapshots', label: 'Snapshots' },
            actions: {
              takeSnapshot: {
                label: 'Take snapshot',
                messages: {
                  confirm: function(args) {
                    return 'Please confirm that you want to take a snapshot' ;
                  },
                  notification: function(args) {
                    return 'Take snapshot';
                  }
                },
                action: function(args) {								  
                  $.ajax({
                    url: createURL("createSnapshot&volumeid=" + args.context.volumes[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.createsnapshotresponse.jobid;
                      args.response.success(
                        {_custom:
                         {jobId: jid,
                          getUpdatedItem: function(json) {
                            return json.queryasyncjobresultresponse.jobresult.snapshot;
                          },
                          getActionFilter: function() {
                            return volumeActionfilter;
                          }
                         }
                        }
                      );
                    }
                  });
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },

              attachDisk: {
                addRow: 'false',
                label: 'Attach Disk',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to attach disk?';
                  },
                  notification: function(args) {
                    return 'Attach disk';
                  }
                },
                createForm: {
                  title: 'Attach Disk',
                  desc: 'Attach Disk to Instance',
                  fields: {
                    virtualMachineId: {
                      label: 'Instance',
                      select: function(args) {
                        var items = [];
                        $.ajax({
                          url: createURL("listVirtualMachines&state=Running&zoneid=" + args.context.volumes[0].zoneid + "&domainid=" + args.context.volumes[0].domainid + "&account=" + args.context.volumes[0].account),
                          dataType: "json",
                          async: false,
                          success: function(json) {
                            var instanceObjs= json.listvirtualmachinesresponse.virtualmachine;
                            $(instanceObjs).each(function() {
                              items.push({id: this.id, description: this.displayname});
                            });
                          }
                        });
                        $.ajax({
                          url: createURL("listVirtualMachines&state=Stopped&zoneid=" + args.context.volumes[0].zoneid + "&domainid=" + args.context.volumes[0].domainid + "&account=" + args.context.volumes[0].account),
                          dataType: "json",
                          async: false,
                          success: function(json) {
                            var instanceObjs= json.listvirtualmachinesresponse.virtualmachine;
                            $(instanceObjs).each(function() {
                              items.push({id: this.id, description: this.displayname});
                            });
                          }
                        });
                        args.response.success({data: items});
                      }
                    }
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("attachVolume&id=" + args.context.volumes[0].id + '&virtualMachineId=' + args.data.virtualMachineId),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.attachvolumeresponse.jobid;
                      args.response.success(
                        {_custom:
                         {jobId: jid,
                          getUpdatedItem: function(json) {
                            return json.queryasyncjobresultresponse.jobresult.volume;
                          },
                          getActionFilter: function() {
                            return volumeActionfilter;
                          }
                         }
                        }
                      );
                    }
                  });
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },
              detachDisk: {
                label: 'Detach disk',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to detach disk?';
                  },
                  notification: function(args) {
                    return 'Detach disk';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("detachVolume&id=" + args.context.volumes[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.detachvolumeresponse.jobid;
                      args.response.success(
                        {_custom:
                         {jobId: jid,
                          getUpdatedItem: function(json) {
                            return {
                              virtualmachineid: null,
                              vmname: null
                            };
                          },
                          getActionFilter: function() {
                            return volumeActionfilter;
                          }
                         }
                        }
                      );
                    }
                  });
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },

              downloadVolume: {
                label: 'Download volume',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to download volume?';
                  },
                  notification: function(args) {
                    return 'Downloading volume';
                  },
                  complete: function(args) {
                    var url = decodeURIComponent(args.url);
                    var htmlMsg = 'Please click <a href="#">00000</a> to download volume';
                    var htmlMsg2 = htmlMsg.replace(/#/, url).replace(/00000/, url);
                    //$infoContainer.find("#info").html(htmlMsg2);
                    return htmlMsg2;
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("extractVolume&id=" + args.context.volumes[0].id + "&zoneid=" + args.context.volumes[0].zoneid + "&mode=HTTP_DOWNLOAD"),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.extractvolumeresponse.jobid;
                      args.response.success(
                        {_custom:
                         {jobId: jid,
                          getUpdatedItem: function(json) {
                            return json.queryasyncjobresultresponse.jobresult.volume;
                          },
                          getActionFilter: function() {
                            return volumeActionfilter;
                          }
                         }
                        }
                      );
                    }
                  });
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },

              createTemplate: {
                label: 'Create template',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to create template?';
                  },
                  notification: function(args) {
                    return 'Create template';
                  }
                },
                createForm: {
                  title: 'Create Template',
                  desc: '',
                  fields: {
                    name: { label: 'Name', validation: { required: true }},
                    displayText: { label: 'Description', validation: { required: true }},
                    osTypeId: {
                      label: 'OS Type',
                      select: function(args) {
                        $.ajax({
                          url: createURL("listOsTypes"),
                          dataType: "json",
                          async: true,
                          success: function(json) {
                            var ostypes = json.listostypesresponse.ostype;
                            var items = [];
                            $(ostypes).each(function() {
                              items.push({id: this.id, description: this.description});
                            });
                            args.response.success({data: items});
                          }
                        });
                      }
                    },
                    isPublic: { label: 'Public', isBoolean: true },
                    isPasswordEnabled: { label: 'Password enabled', isBoolean: true }
                  }
                },
                action: function(args) {
                  /*
                   var isValid = true;
                   isValid &= validateString("Name", $thisDialog.find("#create_template_name"), $thisDialog.find("#create_template_name_errormsg"));
                   isValid &= validateString("Display Text", $thisDialog.find("#create_template_desc"), $thisDialog.find("#create_template_desc_errormsg"));
                   if (!isValid)
                   return;
                   $thisDialog.dialog("close");
                   */

                  var array1 = [];
                  array1.push("&name=" + todb(args.data.name));
                  array1.push("&displayText=" + todb(args.data.displayText));
                  array1.push("&osTypeId=" + args.data.osTypeId);
                  array1.push("&isPublic=" + (args.data.isPublic=="on"));
                  array1.push("&passwordEnabled=" + (args.data.isPasswordEnabled=="on"));

                  $.ajax({
                    url: createURL("createTemplate&volumeId=" + args.context.volumes[0].id + array1.join("")),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.createtemplateresponse.jobid;
                      args.response.success(
                        {_custom:
                         {jobId: jid,
                          getUpdatedItem: function(json) {
                            return {}; //no properties in this volume needs to be updated
                          },
                          getActionFilter: function() {
                            return volumeActionfilter;
                          }
                         }
                        }
                      );
                    }
                  });
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },

              migrateToAnotherStorage: {
                label: 'Migrate volume to another primary storage',
                messages: {
                  confirm: function(args) {
                    return 'Please confirm that you want to migrate volume to another primary storage.';
                  },
                  success: function(args) {
                    return 'Volume is being migrated to another primary storage.';
                  },
                  notification: function(args) {
                    return 'Migrating volume to another primary storage.';
                  },
                  complete: function(args) {
                    return 'Volume has been migrated to another primary storage.';
                  }
                },
                createForm: {
                  title: 'Migrate volumeto another primary storage',
                  desc: '',
                  fields: {
                    storageId: {
                      label: 'Primary storage',
                      validation: { required: true },
                      select: function(args) {
                        $.ajax({
                          url: createURL("listStoragePools&zoneid=" + args.context.volumes[0].zoneid),
                          dataType: "json",
                          async: true,
                          success: function(json) {
                            var pools = json.liststoragepoolsresponse.storagepool;
                            var items = [];
                            $(pools).each(function() {
                              items.push({id: this.id, description: this.name});
                            });
                            args.response.success({data: items});
                          }
                        });
                      }
                    }
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("migrateVolume&storageid=" + args.data.storageId + "&volumeid=" + args.context.volumes[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.migratevolumeresponse.jobid;
                      args.response.success(
                        {_custom:
                         {jobId: jid,
                          getUpdatedItem: function(json) {
                            return json.queryasyncjobresultresponse.jobresult.volume;
                          },
                          getActionFilter: function() {
                            return volumeActionfilter;
                          }
                         }
                        }
                      );
                    }
                  });
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },

              'destroy': {
                label: 'Delete volume',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to delete volume?';
                  },
                  notification: function(args) {
                    return 'Delete volume';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteVolume&id=" + args.context.volumes[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      args.response.success();
                    }
                  });
                },
                notification: {
                  poll: function(args) {
                    args.complete({
                      data: {
                        state: 'Destroyed'
                      }
                    });
                  }
                }
              }
            },
            tabs: {
              details: {
                title: 'Details',

                preFilter: function(args) {
                  var hiddenFields;
                  if(isAdmin()) {
                    hiddenFields = [];
                  }
                  else {
                    hiddenFields = ["storage"];
                  }
                  return hiddenFields;
                },

                fields: [
                  {
                    name: { label: 'Name', isEditable: true }
                  },
                  {
                    id: { label: 'ID' },
                    zonename: { label: 'Zone' },
                    deviceid: { label: 'Device ID' },
                    state: { label: 'State' },
                    type: { label: 'Type' },
                    storagetype: { label: 'Storage Type' },
                    storage: { label: 'Storage' },
                    size : {
                      label: 'Size ',
                      converter: function(args) {
                        if (args == null || args == 0)
                          return "";
                        else
                          return cloudStack.converters.convertBytes(args);
                      }
                    },
                    virtualmachineid: {
                      label: 'VM ID',
                      converter: function(args) {
                        if (args == null)
                          return "detached";
                        else
                          return args;
                      }
                    },
                    vmname: { label: 'VM Name' },
                    vmdisplayname: { label: 'VM Display Name' },
                    created: { label: 'Created', converter: cloudStack.converters.toLocalDate },
                    domain: { label: 'Domain' },
                    account: { label: 'Account' }
                  }
                ],

                dataProvider: function(args) {
                  args.response.success(
                    {
                      actionFilter: volumeActionfilter,
                      data: args.context.volumes[0]
                    }
                  );
                }

              }
            }
          }
        }
      },

      /**
       * Snapshots
       */
      snapshots: {
        type: 'select',
        title: 'Snapshots',
        listView: {
          id: 'snapshots',
          label: 'Snapshots',
          fields: {
            volumename: { label: 'Volume' },
            intervaltype: { label: 'Interval Type' },
            created: { label: 'Date', converter: cloudStack.converters.toLocalDate },
            state: { label: 'State', indicator: { 'BackedUp': 'on', 'Destroyed': 'off' } }
          },

          actions: {
            createTemplate: {
              addRow: 'false',
              label: 'Create template',
              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to create template?';
                },
                notification: function(args) {
                  return 'Create template';
                }
              },
              createForm: {
                title: 'Create Template',
                desc: '',
                fields: {
                  name: { label: 'Name', validation: { required: true }},
                  displayText: { label: 'Description', validation: { required: true }},
                  osTypeId: {
                    label: 'OS Type',
                    select: function(args) {
                      $.ajax({
                        url: createURL("listOsTypes"),
                        dataType: "json",
                        async: true,
                        success: function(json) {
                          var ostypes = json.listostypesresponse.ostype;
                          var items = [];
                          $(ostypes).each(function() {
                            items.push({id: this.id, description: this.description});
                          });
                          args.response.success({data: items});
                        }
                      });
                    }
                  },
                  isPublic: { label: 'Public', isBoolean: true },
                  isPasswordEnabled: { label: 'Password enabled', isBoolean: true }
                }
              },
              action: function(args) {
                /*
                 var isValid = true;
                 isValid &= validateString("Name", $thisDialog.find("#create_template_name"), $thisDialog.find("#create_template_name_errormsg"));
                 isValid &= validateString("Display Text", $thisDialog.find("#create_template_desc"), $thisDialog.find("#create_template_desc_errormsg"));
                 if (!isValid)
                 return;
                 $thisDialog.dialog("close");
                 */

                var array1 = [];
                array1.push("&name=" + todb(args.data.name));
                array1.push("&displayText=" + todb(args.data.displayText));
                array1.push("&osTypeId=" + args.data.osTypeId);
                array1.push("&isPublic=" + (args.data.isPublic=="on"));
                array1.push("&passwordEnabled=" + (args.data.isPasswordEnabled=="on"));

                $.ajax({
                  url: createURL("createTemplate&snapshotid=" + args.context.snapshots[0].id + array1.join("")),
                  dataType: "json",
                  async: true,
                  success: function(json) {
                    var jid = json.createtemplateresponse.jobid;
                    args.response.success(
                      {_custom:
                       {jobId: jid,
                        getUpdatedItem: function(json) {
                          return {}; //nothing in this snapshot needs to be updated
                        },
                        getActionFilter: function() {
                          return snapshotActionfilter;
                        }
                       }
                      }
                    );
                  }
                });
              },
              notification: {
                poll: pollAsyncJobResult
              }
            },

            createVolume: {
              label: 'Create volume',
              addRow: 'false',
              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to create volume?';
                },
                notification: function(args) {
                  return 'Create volume';
                }
              },
              createForm: {
                title: 'Create volume',
                desc: 'Please name your volume.',
                fields: {
                  name: { label: 'Name', validation: { required: true }}
                }
              },
              action: function(args) {
                /*
                 var isValid = true;
                 isValid &= validateString("Name", $thisDialog.find("#create_volume_name"), $thisDialog.find("#create_volume_name_errormsg"));
                 if (!isValid)
                 return;
                 */

                var array1 = [];
                array1.push("&name=" + todb(args.data.name));

                $.ajax({
                  url: createURL("createVolume&snapshotid=" + args.context.snapshots[0].id + array1.join("")),
                  dataType: "json",
                  async: true,
                  success: function(json) {
                    var jid = json.createvolumeresponse.jobid;
                    args.response.success(
                      {_custom:
                       {jobId: jid,
                        getUpdatedItem: function(json) {
                          return {}; //nothing in this snapshot needs to be updated
                        },
                        getActionFilter: function() {
                          return snapshotActionfilter;
                        }
                       }
                      }
                    );
                  }
                });
              },
              notification: {
                poll: pollAsyncJobResult
              }
            },

            'destroy': {
              label: 'Delete snapshot',
              messages: {
                confirm: function(args) {
                  return 'Are you sure you want to delete snapshot?';
                },
                notification: function(args) {
                  return 'Delete snapshot';
                }
              },
              action: function(args) {
                $.ajax({
                  url: createURL("deleteSnapshot&id=" + args.context.snapshots[0].id),
                  dataType: "json",
                  async: true,
                  success: function(json) {
                    var jid = json.deletesnapshotresponse.jobid;
                    args.response.success(
                      {_custom:
                       {jobId: jid
                       }
                      }
                    );
                  }
                });
              },
              notification: {
                poll: function(args) {args.complete({
                  data: { state: 'Destroyed' }
                });}
              }
            }
          },

          dataProvider: function(args) {					  
						var array1 = [];  
						if(args.filterBy != null) {          
							if(args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
								switch(args.filterBy.search.by) {
								case "name":
									if(args.filterBy.search.value.length > 0)
										array1.push("&keyword=" + args.filterBy.search.value);
									break;
								}
							}
						}
											
            var apiCmd = "listSnapshots&page=" + args.page + "&pagesize=" + pageSize + array1.join("");
            if(args.context != null) {
              if("volumes" in args.context) {
                apiCmd += "&volumeid=" + args.context.volumes[0].id;
              }
            }

            $.ajax({
              url: createURL(apiCmd),
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listsnapshotsresponse.snapshot;
                args.response.success({
                  actionFilter: snapshotActionfilter,
                  data: items
                });
              }
            });
          },

          detailView: {
            name: 'Snapshot detail',
            actions: {
              createTemplate: {
                label: 'Create template',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to create template?';
                  },
                  notification: function(args) {
                    return 'Create template';
                  }
                },
                createForm: {
                  title: 'Create Template',
                  desc: '',
                  fields: {
                    name: { label: 'Name', validation: { required: true }},
                    displayText: { label: 'Description', validation: { required: true }},
                    osTypeId: {
                      label: 'OS Type',
                      select: function(args) {
                        $.ajax({
                          url: createURL("listOsTypes"),
                          dataType: "json",
                          async: true,
                          success: function(json) {
                            var ostypes = json.listostypesresponse.ostype;
                            var items = [];
                            $(ostypes).each(function() {
                              items.push({id: this.id, description: this.description});
                            });
                            args.response.success({data: items});
                          }
                        });
                      }
                    },
                    isPublic: { label: 'Public', isBoolean: true },
                    isPasswordEnabled: { label: 'Password enabled', isBoolean: true }
                  }
                },
                action: function(args) {
                  /*
                   var isValid = true;
                   isValid &= validateString("Name", $thisDialog.find("#create_template_name"), $thisDialog.find("#create_template_name_errormsg"));
                   isValid &= validateString("Display Text", $thisDialog.find("#create_template_desc"), $thisDialog.find("#create_template_desc_errormsg"));
                   if (!isValid)
                   return;
                   $thisDialog.dialog("close");
                   */

                  var array1 = [];
                  array1.push("&name=" + todb(args.data.name));
                  array1.push("&displayText=" + todb(args.data.displayText));
                  array1.push("&osTypeId=" + args.data.osTypeId);
                  array1.push("&isPublic=" + (args.data.isPublic=="on"));
                  array1.push("&passwordEnabled=" + (args.data.isPasswordEnabled=="on"));

                  $.ajax({
                    url: createURL("createTemplate&snapshotid=" + args.context.snapshots[0].id + array1.join("")),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.createtemplateresponse.jobid;
                      args.response.success(
                        {_custom:
                         {jobId: jid,
                          getUpdatedItem: function(json) {
                            return {}; //nothing in this snapshot needs to be updated
                          },
                          getActionFilter: function() {
                            return snapshotActionfilter;
                          }
                         }
                        }
                      );
                    }
                  });
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },

              createVolume: {
                label: 'Create volume',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to create volume?';
                  },
                  notification: function(args) {
                    return 'Create volume';
                  }
                },
                createForm: {
                  title: 'Create volume',
                  desc: '',
                  fields: {
                    name: {
                      label: 'Name',
                      validation: {
                        required: true
                      }
                    }
                  }
                },
                action: function(args) {
                  var array1 = [];
                  array1.push("&name=" + todb(args.data.name));

                  $.ajax({
                    url: createURL("createVolume&snapshotid=" + args.context.snapshots[0].id + array1.join("")),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.createvolumeresponse.jobid;
                      args.response.success(
                        {_custom:
                         {jobId: jid,
                          getUpdatedItem: function(json) {
                            return {}; //nothing in this snapshot needs to be updated
                          },
                          getActionFilter: function() {
                            return snapshotActionfilter;
                          }
                         }
                        }
                      );
                    }
                  });
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              },

              'destroy': {
                label: 'Delete snapshot',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to delete snapshot?';
                  },
                  notification: function(args) {
                    return 'Delete snapshot';
                  }
                },
                action: function(args) {
                  $.ajax({
                    url: createURL("deleteSnapshot&id=" + args.context.snapshots[0].id),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.deletesnapshotresponse.jobid;
                      args.response.success(
                        {_custom:
                         {jobId: jid
                         }
                        }
                      );
                    }
                  });
                },
                notification: {
                  poll: function(args) {
                    args.complete({ data: { state: 'Destroyed' } });
                  }
                }
              }
            },
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    name: { label: 'Name' }
                  },
                  {
                    id: { label: 'ID' },
                    name: { label: 'Name' },
                    volumename: { label: 'Volume Name' },
                    state: { label: 'State' },
                    intervaltype: { label: 'Interval Type' },
                    domain: { label: 'Domain' },
                    account: { label: 'Account' },
                    created: { label: 'Created', converter: cloudStack.converters.toLocalDate }
                  }
                ],

                dataProvider: function(args) {
                  args.response.success(
                    {
                      actionFilter: snapshotActionfilter,
                      data: args.context.snapshots[0]
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


  var volumeActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];

    if (jsonObj.state == 'Destroyed') {
      return [];
    }

    if(jsonObj.hypervisor != "Ovm") {
      allowedActions.push("takeSnapshot");
      allowedActions.push("recurringSnapshot");
    }
    if(jsonObj.state != "Allocated") {
      if(jsonObj.hypervisor != "Ovm") {
        allowedActions.push("downloadVolume");
      }
    }
    if(jsonObj.state != "Creating" && jsonObj.state != "Corrupted" && jsonObj.name != "attaching") {
      if(jsonObj.type == "ROOT") {
        if (jsonObj.vmstate == "Stopped") {
          allowedActions.push("createTemplate");
        }
      }
      else {
        if (jsonObj.virtualmachineid != null) {
          if (jsonObj.storagetype == "shared" && (jsonObj.vmstate == "Running" || jsonObj.vmstate == "Stopped" || jsonObj.vmstate == "Destroyed")) {
            allowedActions.push("detachDisk");
          }
        }
        else { // Disk not attached
				  allowedActions.push("destroy");
          allowedActions.push("migrateToAnotherStorage");
          if (jsonObj.storagetype == "shared") {
            allowedActions.push("attachDisk");
          }
        }
      }
    }
    return allowedActions;
  };

  var snapshotActionfilter = function(args) {
    var jsonObj = args.context.item;

    if (jsonObj.state == 'Destroyed') {
      return [];
    }

    var allowedActions = [];
    if(jsonObj.state == "BackedUp") {
      allowedActions.push("createTemplate");
      allowedActions.push("createVolume");
    }
    allowedActions.push("destroy");
    return allowedActions;
  }

})(cloudStack, testData);