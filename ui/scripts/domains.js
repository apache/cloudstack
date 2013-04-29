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
  cloudStack.sections.domains = {
    title: 'label.menu.domains',
    id: 'domains',

    // Domain tree
    treeView: {
      // Details
      detailView: {
        name: 'Domain details',
        viewAll: {
          label: 'label.accounts',
          path: 'accounts'
        },

        // Detail actions
        actions: {
          'delete': {
            label: 'label.action.delete.domain',
            messages: {
              notification: function(args) {
                return 'label.action.delete.domain';
              }
            },

            createForm: {
              title: 'label.action.delete.domain',
              desc: 'message.action.delete.domain',
              createLabel: 'label.delete',
              preFilter: function(args) {
                if(isAdmin()) {
                  args.$form.find('.form-item[rel=isForced]').css('display', 'inline-block');
                }
              },
              fields: {
                isForced: {
                  label: 'force.delete',
                  isBoolean: true,
                  isHidden: true
                }
              }
            },

            action: function(args) {
              var array1 = [];
              if(args.$form.find('.form-item[rel=isForced]').css("display") != "none") //uncomment after Brian fix it to include $form in args
                array1.push("&cleanup=" + (args.data.isForced == "on"));

              $.ajax({
                url: createURL("deleteDomain&id=" + args.context.domains[0].id + array1.join("")),
                dataType: "json",
                async: false,
                success: function(json) {
                  var jid = json.deletedomainresponse.jobid;
                  args.response.success(
                    {_custom:
                     {jobId: jid}
                    }
                  );

                  // Quick fix for proper UI reaction to delete domain
                  var $item = $('.name.selected').closest('li');
                  var $itemParent = $item.closest('li');
                  $itemParent.parent().parent().find('.name:first').click();
                  $item.remove();
                }
              });
            },
            notification: {
              poll: pollAsyncJobResult
            }
          },

          // Edit domain
          edit: {
            label: 'label.action.edit.domain',
            messages: {
              notification: function(args) {
                return 'label.action.edit.domain';
              }
            },
            action: function(args) {
              var domainObj;
							
							var data = {
							  id: args.context.domains[0].id,
							  networkdomain: args.data.networkdomain
							};
														
							if(args.data.name != null) {
							  $.extend(data, {
								  name: args.data.name
								});
							}							             
							
              $.ajax({
                url: createURL("updateDomain"),
                async: false,
                data: data,
                success: function(json) {
                  domainObj = json.updatedomainresponse.domain;
                }
              });

							if(args.data.vmLimit != null) {
								$.ajax({
									url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=0&max=" + args.data.vmLimit),
									dataType: "json",
									async: false,
									success: function(json) {
										domainObj["vmLimit"] = args.data.vmLimit;
									}
								});
							}
							
							if(args.data.ipLimit != null) {
								$.ajax({
									url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=1&max=" + args.data.ipLimit),
									dataType: "json",
									async: false,
									success: function(json) {
										domainObj["ipLimit"] = args.data.ipLimit;
									}
								});
							}

              if(args.data.volumeLimit != null) {							
								$.ajax({
									url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=2&max=" + args.data.volumeLimit),
									dataType: "json",
									async: false,
									success: function(json) {
										domainObj["volumeLimit"] = args.data.volumeLimit;
									}
								});
							}

              if(args.data.snapshotLimit != null) {						
								$.ajax({
									url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=3&max=" + args.data.snapshotLimit),
									dataType: "json",
									async: false,
									success: function(json) {
										domainObj["snapshotLimit"] = args.data.snapshotLimit;
									}
								});
							}

              if(args.data.templateLimit != null) {							
								$.ajax({
									url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=4&max=" + args.data.templateLimit),
									dataType: "json",
									async: false,
									success: function(json) {
										domainObj["templateLimit"] = args.data.templateLimit;
									}
								});
							}

              if(args.data.vpcLimit != null) {						
								$.ajax({
									url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=7&max=" + args.data.vpcLimit),
									dataType: "json",
									async: false,
									success: function(json) {
										domainObj["vpcLimit"] = args.data.vpcLimit;
									}
								});
							}

              if(args.data.cpuLimit != null) {
								$.ajax({
									url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=8&max=" + args.data.cpuLimit),
									dataType: "json",
									async: false,
									success: function(json) {
										domainObj["cpuLimit"] = args.data.cpuLimit;
									}
								});
							}

              if(args.data.memoryLimit != null) {
								$.ajax({
									url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=9&max=" + args.data.memoryLimit),
									dataType: "json",
									async: false,
									success: function(json) {
										domainObj["memoryLimit"] = args.data.memoryLimit;
									}
								});
							}

              if(args.data.primaryStorageLimit != null) {
								$.ajax({
									url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=10&max=" + args.data.primaryStorageLimit),
									dataType: "json",
									async: false,
									success: function(json) {
										domainObj["primaryStorageLimit"] = args.data.primaryStorageLimit;
									}
								});
							}

              if(args.data.secondaryStorageLimit != null) {
								$.ajax({
									url: createURL("updateResourceLimit&domainid=" + args.context.domains[0].id + "&resourceType=11&max=" + args.data.secondaryStorageLimit),
									dataType: "json",
									async: false,
									success: function(json) {
										domainObj["secondaryStorageLimit"] = args.data.secondaryStorageLimit;
									}
								});
							}

              args.response.success({data: domainObj});
            }
          },

          // Add domain
          create: {
            label: 'label.add.domain',

            action: function(args) {
              var data = {
							  parentdomainid: args.context.domains[0].id,
								name: args.data.name
							};
                 
              if(args.data.networkdomain != null && args.data.networkdomain.length > 0) {    
                $.extend(data, {
                  networkdomain: args.data.networkdomain
                });			
							}
                
              $.ajax({
                url: createURL('createDomain'),
                data: data,               
                success: function(json) {
                  var item = json.createdomainresponse.domain;
                  args.response.success({data: item});
                },
                error: function(XMLHttpResponse) {
                  var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                  args.response.error(errorMsg);
                }
              });
            },

            messages: {
              notification: function(args) {
                return 'label.add.domain';
              }
            },

            createForm: {
              title: 'label.add.domain',
              desc: 'message.add.domain',
              fields: {
                name: {
                  label: 'label.name',
                  docID: 'helpDomainName',
                  validation: { required: true }
                },
                networkdomain: {
                  label: 'label.network.domain',
                  docID: 'helpDomainNetworkDomain',
                  validation: { required: false }
                }
              }
            }
          },
					
					updateResourceCount: {
						label: 'label.action.update.resource.count',
						messages: {
							confirm: function(args) {
								return 'message.update.resource.count';
							},
							notification: function(args) {
								return 'label.action.update.resource.count';
							}
						},
						action: function(args) {						  		
							$.ajax({
								url: createURL("updateResourceCount&domainid=" + args.context.domains[0].id),
								dataType: "json",
								async: true,
								success: function(json) {
									//var resourcecounts= json.updateresourcecountresponse.resourcecount;   //do nothing
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
            title: 'label.details',
            fields: [
              {
                name: { 
								  label: 'label.name', 
									isEditable: function(context) {		                    
                    if(context.domains[0].level != 0) //ROOT domain (whose level is 0) is not allowed to change domain name 												
										  return true;
										else
											return false;
									}
								}
              },
              {
                id: { label: 'ID' },
								
								path: { label: 'label.full.path' },
								
                networkdomain: { 
                  label: 'label.network.domain',
                  isEditable: true
                },
                vmLimit: {
                  label: 'label.instance.limits',
                  isEditable: function(context) {		                    
                    if(context.domains[0].level != 0) //ROOT domain (whose level is 0) is not allowed to updateResourceLimits 												
										  return true;
										else
											return false;
									}
                },
                ipLimit: {
                  label: 'label.ip.limits',
                  isEditable: function(context) {		                    
                    if(context.domains[0].level != 0) //ROOT domain (whose level is 0) is not allowed to updateResourceLimits 												
										  return true;
										else
											return false;
									}
                },
                volumeLimit: {
                  label: 'label.volume.limits',
                  isEditable: function(context) {		                    
                    if(context.domains[0].level != 0) //ROOT domain (whose level is 0) is not allowed to updateResourceLimits 												
										  return true;
										else
											return false;
									}
                },
                snapshotLimit: {
                  label: 'label.snapshot.limits',
                  isEditable: function(context) {		                    
                    if(context.domains[0].level != 0) //ROOT domain (whose level is 0) is not allowed to updateResourceLimits 												
										  return true;
										else
											return false;
									}
                },
                templateLimit: {
                  label: 'label.template.limits',
                  isEditable: function(context) {		                    
                    if(context.domains[0].level != 0) //ROOT domain (whose level is 0) is not allowed to updateResourceLimits 												
										  return true;
										else
											return false;
									}
                },
                vpcLimit: {
                  label: 'VPC limits',
                  isEditable: function(context) {		                    
                    if(context.domains[0].level != 0) //ROOT domain (whose level is 0) is not allowed to updateResourceLimits 												
										  return true;
										else
											return false;
									}
                },
                cpuLimit: {
                  label: 'label.cpu.limits',
                  isEditable: function(context) {
                    if(context.domains[0].level != 0) //ROOT domain (whose level is 0) is not allowed to updateResourceLimits 												
  										  return true;
  										else
  											return false;
  									}
                },
                memoryLimit: {
                  label: 'label.memory.limits',
                  isEditable: function(context) {
                    if(context.domains[0].level != 0) //ROOT domain (whose level is 0) is not allowed to updateResourceLimits 												
										  return true;
										else
											return false;
									}
                },
                primaryStorageLimit: {
                  label: 'label.primary.storage.limits',
                  isEditable: function(context) {
                    if(context.domains[0].level != 0) //ROOT domain (whose level is 0) is not allowed to updateResourceLimits 												
  										  return true;
  										else
  											return false;
  									}
                },
                secondaryStorageLimit: {
                  label: 'label.secondary.storage.limits',
                  isEditable: function(context) {
                    if(context.domains[0].level != 0) //ROOT domain (whose level is 0) is not allowed to updateResourceLimits 												
  										  return true;
  										else
  											return false;
  									}
                },
                accountTotal: { label: 'label.accounts' },
                vmTotal: { label: 'label.instances' },
                volumeTotal: { label: 'label.volumes' }
              }
            ],
            dataProvider: function(args) {
              var domainObj = args.context.domains[0];
              $.ajax({
                url: createURL("listAccounts&domainid=" + domainObj.id),
                async: false,
                data: {
								  details: 'min'
								},
                success: function(json) {
                  var items = json.listaccountsresponse.account;
                  var total;
                  if (items != null)
                    total = items.length;
                  else
                    total = 0;
                  domainObj["accountTotal"] = total;
                  var itemsAcc;
                  var totalVMs=0;
                  var totalVolumes=0;
                  for(var i=0;i<total;i++) {
                        itemsAcc = json.listaccountsresponse.account[i];
                        totalVMs = totalVMs + itemsAcc.vmtotal;
                        totalVolumes = totalVolumes + itemsAcc.volumetotal;
                  }
                  domainObj["vmTotal"] = totalVMs;
                  domainObj["volumeTotal"] = totalVolumes;

                }
              });

              /* $.ajax({
                url: createURL("listVirtualMachines&details=min&domainid=" + domainObj.id),
                async: false,
                dataType: "json",
                success: function(json) {
                  var items = json.listvirtualmachinesresponse.virtualmachine;
                  var total;
                  if (items != null)
                    total = items.length;
                  else
                    total = 0;
                  domainObj["vmTotal"] = total;
                }
              });

              $.ajax({
                url: createURL("listVolumes&domainid=" + domainObj.id),
                async: false,
                dataType: "json",
                success: function(json) {
                  var items = json.listvolumesresponse.volume;
                  var total;
                  if (items != null)
                    total = items.length;
                  else
                    total = 0;
                  domainObj["volumeTotal"] = total;
                }
              });*/

              $.ajax({
                url: createURL("listResourceLimits&domainid=" + domainObj.id),
                async: false,
                dataType: "json",
                success: function(json) {
                  var limits = json.listresourcelimitsresponse.resourcelimit;
                  if (limits != null) {
                    for (var i = 0; i < limits.length; i++) {
                      var limit = limits[i];
                      switch (limit.resourcetype) {
                      case "0":
                        domainObj["vmLimit"] = limit.max;
                        break;
                      case "1":
                        domainObj["ipLimit"] = limit.max;
                        break;
                      case "2":
                        domainObj["volumeLimit"] = limit.max;
                        break;
                      case "3":
                        domainObj["snapshotLimit"] = limit.max;
                        break;
                      case "4":
                        domainObj["templateLimit"] = limit.max;
                        break;
                      case "7":
                        domainObj["vpcLimit"] = limit.max;
                        break;
                      case "8":
                        domainObj["cpuLimit"] = limit.max;
                        break;
                      case "9":
                        domainObj["memoryLimit"] = limit.max;
                        break;
                      case "10":
                        domainObj["primaryStorageLimit"] = limit.max;
                        break;
                      case "11":
                        domainObj["secondaryStorageLimit"] = limit.max;
                        break;
                      }
                    }
                  }
                }
              });

              args.response.success({
                data: domainObj,
                actionFilter: domainActionfilter
              });
            }
          }
        }
      },
      labelField: 'name',
      dataProvider: function(args) {
        var parentDomain = args.context.parentDomain;
        if(parentDomain == null) { //draw root node
          $.ajax({
            url: createURL("listDomains&id=" + g_domainid + '&listAll=true'),
            dataType: "json",
            async: false,
            success: function(json) {
              var domainObjs = json.listdomainsresponse.domain;
              args.response.success({
                actionFilter: domainActionfilter,
                data: domainObjs
              });
            }
          });
        }
        else {
          $.ajax({
            url: createURL("listDomainChildren&id=" + parentDomain.id),
            dataType: "json",
            async: false,
            success: function(json) {
              var domainObjs = json.listdomainchildrenresponse.domain;
              args.response.success({
                actionFilter: domainActionfilter,
                data: domainObjs
              });
            }
          });
        }
      }
    }
  };

  var domainActionfilter = function(args) {
    var jsonObj = args.context.item;
    var allowedActions = [];
    if(isAdmin()) {
      allowedActions.push("create");     
			allowedActions.push("edit"); //merge updateResourceLimit into edit
			if(jsonObj.level != 0) { //ROOT domain (whose level is 0) is not allowed to delete         
        allowedActions.push("delete");
      }
    }
    allowedActions.push("updateResourceCount");
    return allowedActions;
  }

})(cloudStack);
