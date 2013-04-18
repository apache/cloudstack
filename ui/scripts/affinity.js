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
  cloudStack.sections.affinityGroups = {
    title: 'label.affinity.groups',
    listView: {
      id: 'affinityGroups',
      fields: {
        name: { label: 'label.name' },
        type: { label: 'label.type' }
      },
      dataProvider: function(args) {
        $.ajax({
				  url: createURL('listAffinityGroups'),
					success: function(json) {					 
					  var items = json.listaffinitygroupsresponse.affinitygroup;
						args.response.success({data: items});
					}
				});				
      },
      actions: {
        add: {
          label: 'label.add.affinity.group',

          messages: {            
            notification: function(args) {
              return 'label.add.affinity.group';
            }
          },

          createForm: {
            title: 'label.add.affinity.group',
            fields: {
              name: {
                label: 'label.name',
                validation: { required: true }
              },
							description: {
                label: 'label.description'                
              },
              type: {
                label: 'label.type',
                select: function(args) {
								  $.ajax({
									  url: createURL('listAffinityGroupTypes'),
										success: function(json) {
										  var types = [];											
											var items = json.listaffinitygrouptypesresponse.affinityGroupType;
											if(items != null) {
											  for(var i = 0; i < items.length; i++) {
												  types.push({id: items[i].type, description: items[i].type});
												}												
											}
											args.response.success({data: types})
										}
									});								
                }
              }     
            }
          },

          action: function(args) {					 
						var data = {
						  name: args.data.name,
							type: args.data.type							
						};						
						if(args.data.description != null && args.data.description.length > 0)
						  $.extend(data, {description: args.data.description});						
					
					  $.ajax({
						  url: createURL('createAffinityGroup'),
							data: data,
							success: function(json) {							 							
								var jid = json.createaffinitygroupresponse.jobid;
								args.response.success(
									{_custom:
									 {jobId: jid,
										getUpdatedItem: function(json) {												  
											return json.queryasyncjobresultresponse.jobresult.affinitygroup;
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
        }
      },
      detailView: {
        actions: {          
          remove: {
            label: 'label.delete.affinity.group',            
            messages: {
              confirm: function(args) {
                return 'message.delete.affinity.group';
              },
              notification: function(args) {
                return 'label.delete.affinity.group';
              }
            },
						action: function(args) {						  
							$.ajax({
							  url: createURL('deleteAffinityGroup'),
								data: {
								  id: args.context.affinityGroups[0].id
								},
								success: function(json) {			
									var jid = json.deleteaffinitygroupresponse.jobid;
									args.response.success({
									  _custom:{
										  jobId: jid
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

        viewAll: { path: 'instances', label: 'label.instances' },
        
        tabs: {
          details: {
            title: 'label.details',
            fields: [
              {
                name: { label: 'label.name' }
              },
              {                
								description: { label: 'label.description' },
								type: { label: 'label.type' },
								id: { label: 'label.id' }								
              }
            ],

            dataProvider: function(args) {						  
							$.ajax({
								url: createURL('listAffinityGroups'),
								data: {
								  id: args.context.affinityGroups[0].id
								},
								success: function(json) {					 
									var item = json.listaffinitygroupsresponse.affinitygroup[0];
									args.response.success({data: item});
								}
							});	              
            }
          },
										
					/**
           * VMs tab
           */
          vms: {
            title: 'label.instances',
            multiple: true,
            fields: [
              {
                id: { label: 'ID' },
                displayname: { label: 'label.display.name' },
                state: { label: 'label.state' }							
              }
            ],
            dataProvider: function(args) {		
						  var vmIds = args.context.affinityGroups[0].virtualmachineIds;
							if(vmIds == null || vmIds.length == 0) {
							  args.response.success({data: null});		
							  return;
							}
						
              $.ajax({
							  url: createURL('listVirtualMachines'),
								success: function(json) {								  
									var firstPageVms = json.listvirtualmachinesresponse.virtualmachine;									
									var items = [];									
									if(vmIds != null) {
										for(var i = 0; i < vmIds.length; i++) {										  
											var item = {id: vmIds[i]};	
                      var matchFound = false;											
											if(firstPageVms != null) {
											  for(var k = 0; k < firstPageVms.length; k++) {
												  if(firstPageVms[k].id == vmIds[i]) {
													  matchFound = true;
													  item = firstPageVms[k];
														break; //break for looup
													}
												}
											}		
                      if(matchFound == false) { //the VM is not in API response of "listVirtualMachines&page=1&pagesize=500"
                        $.ajax({
												  url: createURL('listVirtualMachines'),
													async: false,
													data: {id: vmIds[i]},
								          success: function(json) {	
													  item = json.listvirtualmachinesresponse.virtualmachine[0];														
													}
												});
                      }											
											items.push(item);								  
										}
									}			                  							
									args.response.success({data: items});																	
								}
							});					  
            }
          }					
        }
      }
    }
  };
})(cloudStack);
