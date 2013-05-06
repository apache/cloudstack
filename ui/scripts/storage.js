// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
(function(cloudStack) {

  var diskofferingObjs, selectedDiskOfferingObj;

  cloudStack.sections.storage = {
    title: 'label.storage',
    id: 'storage',
    sectionSelect: {
      label: 'label.select-view'
    },
    sections: {
      /**
       * Volumes
       */
      volumes: {
        type: 'select',
        title: 'label.volumes',
        listView: {
          id: 'volumes',
          label: 'label.volumes',
					preFilter: function(args) {
					  var hiddenFields = [];
					  if(isAdmin() != true)
					    hiddenFields.push('hypervisor');
						return hiddenFields;
					},
          fields: {
            name: { label: 'label.name' },
            type: { label: 'label.type' },
            hypervisor: { label: 'label.hypervisor' },	
            vmdisplayname: { label: 'label.vm.display.name' }
            						
						/*
						state: { 
						  label: 'State',
							indicator: {               
                'Ready': 'on'
              }
						}
						*/
          },
					
          // List view actions
          actions: {
            // Add volume
            add: {
              label: 'label.add.volume',

              messages: {
                confirm: function(args) {
                  return 'message.add.volume';
                },
                notification: function(args) {
                  return 'label.add.volume';
                }
              },

              createForm: {
                title: 'label.add.volume',
                desc: 'message.add.volume',
                fields: {
                  name: {
                    docID: 'helpVolumeName',
                    label: 'label.name',
                    validation: { required: true }
                  },
                  availabilityZone: {
                    label: 'label.availability.zone',
                    docID: 'helpVolumeAvailabilityZone',
                    select: function(args) {
                      $.ajax({
                        url: createURL("listZones&available=true"),
                        dataType: "json",
                        async: true,
                        success: function(json) {												 
													var zoneObjs = json.listzonesresponse.zone;		
													args.response.success({descriptionField: 'name', data: zoneObjs});													                      
                        }
                      });
                    }
                  },
                  diskOffering: {
                    label: 'label.disk.offering',
                    docID: 'helpVolumeDiskOffering',
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
                    label: 'label.disk.size.gb',
                    validation: { required: true, number: true },
                    isHidden: true
                  }

                }
              },

              action: function(args) {
							  var data = {
								  name: args.data.name,
									zoneId: args.data.availabilityZone,
									diskOfferingId: args.data.diskOffering
								};
							
                // if(thisDialog.find("#size_container").css("display") != "none") { //wait for Brian to include $form in args
                if (selectedDiskOfferingObj.iscustomized == true) {
								  $.extend(data, {
									  size: args.data.diskSize
									});
                }

                $.ajax({
                  url: createURL('createVolume'),
                  data: data,                 
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
						 
						uploadVolume: {
              isHeader: true,							
              label: 'label.upload.volume',							
              messages: {
                notification: function() { 
								  return 'label.upload.volume'; 
								}
              },
              createForm: {
                title: 'label.upload.volume',                
                fields: {
                  name: {
                    label: 'label.name',
                    validation: { required: true },
                    docID: 'helpUploadVolumeName'
                  },
                  availabilityZone: {
                    label: 'label.availability.zone',
                    docID: 'helpUploadVolumeZone',
                    select: function(args) {                      
											$.ajax({
                        url: createURL("listZones&available=true"),
                        dataType: "json",
                        async: true,
                        success: function(json) {												 
													var zoneObjs = json.listzonesresponse.zone;																													
													args.response.success({descriptionField: 'name', data: zoneObjs});													                      
                        }
                      });											
                    }
                  },
                  format: {
									  label: 'label.format',
                    docID: 'helpUploadVolumeFormat',
										select: function(args) {
										  var items = [];
                      items.push({ id: 'RAW', description: 'RAW' });
											items.push({ id: 'VHD', description: 'VHD' });
											items.push({ id: 'OVA', description: 'OVA' });
											items.push({ id: 'QCOW2', description: 'QCOW2' });
											args.response.success({ data: items });
										}
									},
									url: {
									  label: 'label.url',
                    docID: 'helpUploadVolumeURL',
										validation: { required: true }
									},
                  checksum : {
                    docID: 'helpUploadVolumeChecksum',
                    label: 'label.checksum'
                  }                  
                }
              },

              action: function(args) {
							  var data = {
								  name: args.data.name,
									zoneId: args.data.availabilityZone,
									format: args.data.format,
									url: args.data.url
								};
							                
								if(args.data.checksum != null && args.data.checksum.length > 0) {
								  $.extend(data, {
									  checksum: args.data.checksum
									});
								}
                
                $.ajax({
                  url: createURL('uploadVolume'),
                  data: data,                 
                  success: function(json) {										  
										var jid = json.uploadvolumeresponse.jobid;
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
            }							
          },
					
					advSearchFields: {
					  name: { label: 'Name' },
						zoneid: { 
						  label: 'Zone',							
              select: function(args) {							  					
								$.ajax({
									url: createURL('listZones'),
									data: {
									  listAll: true
									},
									success: function(json) {									  
										var zones = json.listzonesresponse.zone;

										args.response.success({
											data: $.map(zones, function(zone) {
												return {
													id: zone.id,
													description: zone.name
												};
											})
										});
									}
								});
							}						
						},		
						
						domainid: {					
							label: 'Domain',					
							select: function(args) {
								if(isAdmin() || isDomainAdmin()) {
									$.ajax({
										url: createURL('listDomains'),
										data: { 
											listAll: true,
											details: 'min'
										},
										success: function(json) {
											var array1 = [{id: '', description: ''}];
											var domains = json.listdomainsresponse.domain;
											if(domains != null && domains.length > 0) {
												for(var i = 0; i < domains.length; i++) {
													array1.push({id: domains[i].id, description: domains[i].path});
												}
											}
											args.response.success({
												data: array1
											});
										}
									});
								}
								else {
									args.response.success({
										data: null
									});
								}
							},
							isHidden: function(args) {
								if(isAdmin() || isDomainAdmin())
									return false;
								else
									return true;
							}
						},								
						
						account: { 
							label: 'Account',
							isHidden: function(args) {
								if(isAdmin() || isDomainAdmin())
									return false;
								else
									return true;
							}			
						},
						
						tagKey: { label: 'Tag Key' },
						tagValue: { label: 'Tag Value' }						
					},
					
          dataProvider: function(args) {
					  var data = {};
						listViewDataProvider(args, data);						
           
            if(args.context != null) {
              if("instances" in args.context) {
            		$.extend(data, {
            		  virtualMachineId: args.context.instances[0].id
            		});
              }
            }
            
            $.ajax({
              url: createURL('listVolumes'),
              data: data,             
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
            viewAll: { path: 'storage.snapshots', label: 'label.snapshots' },
            actions: {

             migrateVolume:{
                 label:'Migrate Volume',
               messages: {
                  confirm: function(args) {
                    return 'Do you want to migrate this volume ?' ;
                  },
                  notification: function(args) {
                    return 'Volume migrated';
                  }
                },

             createForm: {
              title: 'Migrate Volume',
              desc: '',
              fields: {
                storagePool: {
                  label: 'Storage Pool',
                  validation: { required: true },
                  select: function(args) {
                    $.ajax({
                      url: createURL("findStoragePoolsForMigration&id=" + args.context.volumes[0].id),
                      dataType: "json",
                      async: true,
                      success: function(json) {
                            var pools = json.findstoragepoolsformigrationresponse.storagepool;
                            var items = [];
                            $(pools).each(function() {
                              items.push({id: this.id, description: this.name + " (" + (this.suitableformigration? "Suitable": "Not Suitable")+")"   });
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
                    url: createURL("migrateVolume&livemigrate=true&storageid=" + args.data.storagePool + "&volumeid=" + args.context.volumes[0].id ),
                    dataType: "json",
                    async: true,
                    success: function(json) {
                      var jid = json.migratevolumeresponse.jobid;
                      args.response.success(
                        {_custom:
                         {
                            jobId: jid
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

              takeSnapshot: {
                label: 'label.action.take.snapshot',
                messages: {
                  confirm: function(args) {
                    return 'message.action.take.snapshot' ;
                  },
                  notification: function(args) {
                    return 'label.action.take.snapshot';
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
                         {
												   jobId: jid //take snapshot from a volume doesn't change any property in this volume. So, don't need to specify getUpdatedItem() to return updated volume. Besides, createSnapshot API doesn't return updated volume. 
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

              recurringSnapshot: {
                label: 'label.snapshot.schedule',
                action: {
                  custom: cloudStack.uiCustom.recurringSnapshots({
                    desc: 'message.snapshot.schedule',
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
                            { id: 1, name: 'label.sunday' },
                            { id: 2, name: 'label.monday' },
                            { id: 3, name: 'label.tuesday' },
                            { id: 4, name: 'label.wednesday' },
                            { id: 5, name: 'label.thursday' },
                            { id: 6, name: 'label.friday' },
                            { id: 7, name: 'label.saturday' }
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
                    return 'label.snapshot.schedule';
                  }
                }
              },

              attachDisk: {
                addRow: 'false',
                label: 'label.action.attach.disk',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to attach disk?';
                  },
                  notification: function(args) {
                    return 'label.action.attach.disk';
                  }
                },
                createForm: {
                  title: 'label.action.attach.disk',
                  desc: 'label.action.attach.disk',
                  fields: {
                    virtualMachineId: {
                      label: 'label.instance',
                      select: function(args) {
                        var zoneid = args.context.volumes[0].zoneid;
                        var items = [];
                        var data;

                        if (!args.context.projects) {
                          data = {
                            zoneid: zoneid,
                            domainid: args.context.volumes[0].domainid,
                            account: args.context.volumes[0].account
                          };
                        } else {
                          data = {
                            zoneid: zoneid,
                            projectid: args.context.projects[0].id
                          };
                        }
																
												if(args.context.volumes[0].hypervisor != null && args.context.volumes[0].hypervisor.length > 0 && args.context.volumes[0].hypervisor != 'None') {
												  data = $.extend(data, {
													  hypervisor: args.context.volumes[0].hypervisor
													});
												}
																								
                        $(['Running', 'Stopped']).each(function() {
                          $.ajax({
                            url: createURL('listVirtualMachines'),
                            data: $.extend(data, {
                              state: this.toString()															
                            }),
                            async: false,
                            success: function(json) {
                              var instanceObjs= json.listvirtualmachinesresponse.virtualmachine;
                              $(instanceObjs).each(function() {
                                items.push({
                                  id: this.id, description: this.displayname ?
                                    this.displayname : this.name
                                });
                              });
                            }
                          });
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
                label: 'label.action.detach.disk',
                messages: {
                  confirm: function(args) {
                    return 'message.detach.disk';
                  },
                  notification: function(args) {
                    return 'label.action.detach.disk';
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
                label: 'label.action.download.volume',
                messages: {
                  confirm: function(args) {
                    return 'message.download.volume.confirm';
                  },
                  notification: function(args) {
                    return 'label.action.download.volume';
                  },
                  complete: function(args) {
                    var url = decodeURIComponent(args.url);
                    var htmlMsg = _l('message.download.volume');
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
                label: 'label.create.template',
                messages: {
                  confirm: function(args) {
                    return 'message.create.template';
                  },
                  notification: function(args) {
                    return 'label.create.template';
                  }
                },
                createForm: {
                  title: 'label.create.template',
                  preFilter: cloudStack.preFilter.createTemplate,
                  desc: '',
                  fields: {
                    name: { label: 'label.name', validation: { required: true }},
                    displayText: { label: 'label.description', validation: { required: true }},
                    osTypeId: {
                      label: 'label.os.type',
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
                    isPublic: { label: 'label.public', isBoolean: true },
                    isPasswordEnabled: { label: 'label.password.enabled', isBoolean: true },
                    isFeatured: { label: "label.featured", isBoolean: true }
                  }
                },
                action: function(args) {
                  var data = {
									  volumeId: args.context.volumes[0].id,
									  name: args.data.name,
										displayText: args.data.displayText,
										osTypeId: args.data.osTypeId,
										isPublic: (args.data.isPublic=="on"),
										passwordEnabled: (args.data.isPasswordEnabled=="on")
									};
								                 
                  if(args.$form.find('.form-item[rel=isFeatured]').css("display") != "none") {
									  $.extend(data, {
										  isfeatured: (args.data.isFeatured == "on")
										});
									}

                  $.ajax({
                    url: createURL('createTemplate'),
                    data: data,                    
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
                label: 'label.migrate.volume',
                messages: {
                  confirm: function(args) {
                    return 'message.migrate.volume';
                  },
                  notification: function(args) {
                    return 'label.migrate.volume';
                  }
                },
                createForm: {
                  title: 'label.migrate.volume',
                  desc: '',
                  fields: {
                    storageId: {
                      label: 'label.primary.storage',
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

              remove: {
                label: 'label.action.delete.volume',
                messages: {
                  confirm: function(args) {
                    return 'message.action.delete.volume';
                  },
                  notification: function(args) {
                    return 'label.action.delete.volume';
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
                    args.complete();
                  }
                }
              },
              
              resize: {
                label: 'label.action.resize.volume',
                messages: {
                  notification: function(args) {
                    return 'label.action.resize.volume';
                  }
                },
                createForm: {
                  title: 'label.action.resize.volume',
                  fields: {
                    newdiskoffering: {
                      label: 'label.resize.new.offering.id',
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
                          var $newsize = $form.find('.form-item[rel=newsize]');
                          if (selectedDiskOfferingObj.iscustomized == true) {
                            $newsize.css('display', 'inline-block');
                          }
                          else {
                            $newsize.hide();
                          }
                        });
                      }
                    },
                    newsize: {
                      label: 'label.resize.new.size',
                      validation: { required: true, number: true },
                      isHidden: true
                    },
                    shrinkok: {label: 'label.resize.shrink.ok', isBoolean: true, isChecked: false}
                  }
                },
                action: function(args){
                  var array1 = [];
                  array1.push("&shrinkok=" + (args.data.shrinkok == "on"));
                  var newDiskOffering = args.data.newdiskoffering;
                  var newSize;
                  if (selectedDiskOfferingObj.iscustomized == true) {
                    newSize = args.data.newsize;
                  }
                  if (newDiskOffering != null && newDiskOffering.length > 0){
                    array1.push("&diskofferingid=" + todb(newDiskOffering));
                  }
                  if (newSize != null && newSize.length > 0){
                    array1.push("&size=" + todb(newSize));
                  }
                  $.ajax({
                    url: createURL("resizeVolume&id=" + args.context.volumes[0].id + array1.join("")),
                    dataType: "json",
                    async: true,
                    success: function(json){
                    var jid = json.resizevolumeresponse.jobid;
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
                        });
                    }
                  });
                },
                notification: {
                  poll: pollAsyncJobResult
                }
              }
            },
            tabs: {
              details: {
                title: 'label.details',

                preFilter: function(args) {
                  var hiddenFields;
                  if(isAdmin()) {
                    hiddenFields = [];
                  }
                  else {
                    hiddenFields = ['storage', 'hypervisor'];
                  }
                  return hiddenFields;
                },

                fields: [
                  {
                    name: { label: 'label.name', isEditable: true }
                  },
                  {
                    id: { label: 'ID' },
                    zonename: { label: 'label.zone' },                    
                    state: { 
										  label: 'label.state',
											pollAgainIfValueIsIn: { 
											  'UploadNotStarted': 1
											},
											pollAgainFn: function(context) {  								 
												var toClearInterval = false; 				
												$.ajax({
													url: createURL("listVolumes&id=" + context.volumes[0].id),
													dataType: "json",
													async: false,
													success: function(json) {	
														var jsonObj = json.listvolumesresponse.volume[0];   
														if(jsonObj.state != context.volumes[0].state) {	
															toClearInterval = true;	//to clear interval	
														}
													}
												});		
                        return toClearInterval;												
											}											
										},
		    status: {label: 'label.status'},
                    type: { label: 'label.type' },
                    storagetype: { label: 'label.storage.type' },   
                    hypervisor: { label: 'label.hypervisor' },										
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
                    //vmname: { label: 'label.vm.name' },
                    vmdisplayname: { label: 'label.vm.display.name' },
										vmstate: { label: 'label.vm.state' },
										deviceid: { label: 'label.device.id' },
										storage: { label: 'label.storage' },
                    created: { label: 'label.created', converter: cloudStack.converters.toLocalDate },
                    domain: { label: 'label.domain' },
                    account: { label: 'label.account' }
                  }
                ],

                tags: cloudStack.api.tags({ resourceType: 'Volume', contextId: 'volumes' }),


                dataProvider: function(args) {		
								  $.ajax({
										url: createURL("listVolumes&id=" + args.context.volumes[0].id),
										dataType: "json",
										async: true,
										success: function(json) {								  
											var jsonObj = json.listvolumesresponse.volume[0];   
											args.response.success(
												{
													actionFilter: volumeActionfilter,
													data: jsonObj
												}
											);		
										}
									});								
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
        title: 'label.snapshots',
        listView: {
          id: 'snapshots',
          label: 'label.snapshots',
          fields: {
            volumename: { label: 'label.volume' },
            intervaltype: { label: 'label.interval.type' },
            created: { label: 'label.created', converter: cloudStack.converters.toLocalDate },
            state: {              
              label: 'label.state', 
							indicator: {               
                'BackedUp': 'on', 
								'Destroyed': 'off'
              }
            }
          },

					advSearchFields: {
					  name: { label: 'Name' },	
            
						domainid: {					
							label: 'Domain',					
							select: function(args) {
								if(isAdmin() || isDomainAdmin()) {
									$.ajax({
										url: createURL('listDomains'),
										data: { 
											listAll: true,
											details: 'min'
										},
										success: function(json) {
											var array1 = [{id: '', description: ''}];
											var domains = json.listdomainsresponse.domain;
											if(domains != null && domains.length > 0) {
												for(var i = 0; i < domains.length; i++) {
													array1.push({id: domains[i].id, description: domains[i].path});
												}
											}
											args.response.success({
												data: array1
											});
										}
									});
								}
								else {
									args.response.success({
										data: null
									});
								}
							},
							isHidden: function(args) {
								if(isAdmin() || isDomainAdmin())
									return false;
								else
									return true;
							}
						},		
						
						account: { 
							label: 'Account',
							isHidden: function(args) {
								if(isAdmin() || isDomainAdmin())
									return false;
								else
									return true;
							}			
						},						
						tagKey: { label: 'Tag Key' },
						tagValue: { label: 'Tag Value' }						
					},
					
          dataProvider: function(args) {					  
						var data = {};
            var instanceVolumeIds = [];
						listViewDataProvider(args, data);		
            
            if(args.context != null) {
              if("volumes" in args.context) {
							  $.extend(data, {
								  volumeid: args.context.volumes[0].id
								});                
              } else if (args.context.instances) {
                $.ajax({
                  url: createURL('listVolumes'),
                  data: {
                    virtualmachineid: args.context.instances[0].id,
                    listAll: true
                  },
                  async: false,
                  success: function(json) {
                    instanceVolumeIds = $.map(json.listvolumesresponse.volume, function(volume) {
                      return volume.id;
                    })
                  }
                });
                data.volumeid = instanceVolumeIds.join(',');
              }
            }

            $.ajax({
              url: createURL('listSnapshots'),
              data: data,              
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
                label: 'label.create.template',
                messages: {
                  confirm: function(args) {
                    return 'message.create.template';
                  },
                  notification: function(args) {
                    return 'label.create.template';
                  }
                },
                createForm: {
                  title: 'label.create.template',
                  desc: '',
                  fields: {
                    name: { label: 'label.name', validation: { required: true }},
                    displayText: { label: 'label.description', validation: { required: true }},
                    osTypeId: {
                      label: 'label.os.type',
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
                    isPublic: { label: 'label.public', isBoolean: true },
                    isPasswordEnabled: { label: 'label.password.enabled', isBoolean: true }
                  }
                },
                action: function(args) {
                  var data = {
									  snapshotid: args.context.snapshots[0].id,
										name: args.data.name,
										displayText: args.data.displayText,
										osTypeId: args.data.osTypeId,
										isPublic: (args.data.isPublic=="on"),
										passwordEnabled: (args.data.isPasswordEnabled=="on")
									};
								
                  $.ajax({
                    url: createURL('createTemplate'),
                    data: data,                   
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
                label: 'label.action.create.volume',
                messages: {
                  confirm: function(args) {
                    return 'Are you sure you want to create volume?';
                  },
                  notification: function(args) {
                    return 'label.action.create.volume';
                  }
                },
                createForm: {
                  title: 'label.action.create.volume',
                  desc: '',
                  fields: {
                    name: {
                      label: 'label.name',
                      validation: {
                        required: true
                      }
                    }
                  }
                },
                action: function(args) {
								  var data = {
									  snapshotid: args.context.snapshots[0].id,
										name: args.data.name
									};								                

                  $.ajax({
                    url: createURL('createVolume'),
                    data: data,                    
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

              remove: {
                label: 'label.action.delete.snapshot',
                messages: {
                  confirm: function(args) {
                    return 'message.action.delete.snapshot';
                  },
                  notification: function(args) {
                    return 'label.action.delete.snapshot';
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
                    args.complete();
                  }
                }
              }
            },
            tabs: {
              details: {
                title: 'label.details',
                fields: [
                  {
                    name: { label: 'label.name' }
                  },
                  {
                    id: { label: 'label.id' },
                    volumename: { label: 'label.volume.name' },
                    state: { label: 'label.state' },
                    intervaltype: { label: 'label.interval.type' },
                    domain: { label: 'label.domain' },
                    account: { label: 'label.account' },
                    created: { label: 'label.created', converter: cloudStack.converters.toLocalDate }
                  }
                ],

                tags: cloudStack.api.tags({ resourceType: 'Snapshot', contextId: 'snapshots' }),

                dataProvider: function(args) {
								  $.ajax({
										url: createURL("listSnapshots&id=" + args.context.snapshots[0].id),
										dataType: "json",
										async: true,
										success: function(json) {								  
											var jsonObj = json.listsnapshotsresponse.snapshot[0];   
											args.response.success(
												{
													actionFilter: snapshotActionfilter,
													data: jsonObj
												}
											);		
										}
									});		
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

    
    if (jsonObj.state == 'Destroyed' || jsonObj.state == 'Migrating' || jsonObj.state == 'Uploading') {
      return [];
    }
		if (jsonObj.state == 'UploadError') {
      return ["remove"];
    }

    if(jsonObj.hypervisor != "Ovm" && jsonObj.state == "Ready") {
      allowedActions.push("takeSnapshot");
      allowedActions.push("recurringSnapshot");
      if(jsonObj.type == "DATADISK") {
    	  allowedActions.push("resize");
      }
    }
    if(jsonObj.state != "Allocated") {
      if((jsonObj.vmstate == "Stopped" || jsonObj.virtualmachineid == null) && jsonObj.state == "Ready") {
        allowedActions.push("downloadVolume");
      }
    }

   if(jsonObj.type == "ROOT" || jsonObj.type =="DATADISK"){ 
    if(jsonObj.state == "Ready" && isAdmin() && jsonObj.virtualmachineid != null ){
         allowedActions.push("migrateVolume");
    }
  }

    if(jsonObj.state != "Creating") {
      if(jsonObj.type == "ROOT") {
        if (jsonObj.vmstate == "Stopped") {
          allowedActions.push("createTemplate");
        }
      }
      else { //jsonObj.type == "DATADISK"
        if (jsonObj.virtualmachineid != null) {
          if (jsonObj.vmstate == "Running" || jsonObj.vmstate == "Stopped" || jsonObj.vmstate == "Destroyed") {
            allowedActions.push("detachDisk");
          }
        }
        else { // Disk not attached
          allowedActions.push("remove");
          if(jsonObj.state == "Ready" && isAdmin() && jsonObj.storagetype == "shared") {
            allowedActions.push("migrateToAnotherStorage");
          }
          allowedActions.push("attachDisk");
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
    allowedActions.push("remove");
			
    return allowedActions;
  }

})(cloudStack);
