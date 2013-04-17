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
        description: { label: 'label.description' }
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
						if(args.data.domainid != null && args.data.domainid.length > 0)
							$.extend(data, { domainid: args.data.domainid });  								  		
						if(args.data.account != null && args.data.account.length > 0)
							$.extend(data, { account: args.data.account });   
					
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
          edit: {
            label: 'label.edit',
            action: function(args) {
              args.response.success();
            },
            messages: {
              notification: function(args) { return 'label.edit.affinity.group'; }
            }
          },
          remove: {
            label: 'label.delete.affinity.group',
            action: function(args) {
              args.response.success();
            },
            messages: {
              confirm: function(args) {
                return 'message.delete.affinity.group';
              },
              notification: function(args) {
                return 'label.delete.affinity.group';
              }
            },
            notification: {
              // poll: pollAsyncJobResult,
              poll: function(args) { args.complete(); }
            }
          }
        },

        viewAll: { path: 'instances', label: 'label.instances' },
        
        tabs: {
          details: {
            title: 'label.details',
            fields: [
              {
                name: { label: 'label.name', isEditable: true }
              },
              {
                type: { label: 'label.type', isCompact: true }
              }
            ],

            dataProvider: function(args) {
              setTimeout(function() {
                args.response.success({ data: args.context.affinityGroups[0] });
              }, 20);
            }
          }
        }
      }
    }
  };
})(cloudStack);
