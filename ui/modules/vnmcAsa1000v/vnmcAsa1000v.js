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
(function($, cloudStack) {
  cloudStack.modules.vnmcAsa1000v = function(module) {
    module.vnmcNetworkProvider.addDevice({
      id: 'asa1000v',
      title: 'ASA 1000v',
      listView: {
        id: 'asa1000vDevices',
        fields: {
          hostname: { label: 'label.host' },
          insideportprofile: { label: 'Inside Port Profile' }
        },
        dataProvider: function(args) {          
          $.ajax({
            url: createURL('listCiscoAsa1000vResources'),
            data: {
              physicalnetworkid: args.context.physicalNetworks[0].id
            },
            success: function(json){   
              var items = json.listCiscoAsa1000vResources.CiscoAsa1000vResource; 
              args.response.success({ data: items });            
            }
          }); 
        },
        
        actions: {
          add: {
            label: 'Add CiscoASA1000v Resource',
            messages: {             
              notification: function(args) {
                return 'Add CiscoASA1000v Resource';
              }
            },
            createForm: {
              title: 'Add CiscoASA1000v Resource',
              fields: {
                hostname: {
                  label: 'label.host',                
                  validation: { required: true }
                },
                insideportprofile: {
                  label: 'Inside Port Profile',                
                  validation: { required: true }
                },
                clusterid: {
                  label: 'label.cluster',                   
                  validation: { required: true },
                  select: function(args){                    
                    $.ajax({
                      url: createURL('listClusters'),
                      data: {
                        zoneid: args.context.zones[0].id
                      },                      
                      success: function(json) {                        
                        var objs = json.listclustersresponse.cluster;
                        var items = [];
                        if(objs != null) {
                          for(var i = 0; i < objs.length; i++){
                            items.push({id: objs[i].id, description: objs[i].name});
                          }
                        }     
                        args.response.success({data: items});
                      }
                    });
                  }
                }            
              }
            },
            action: function(args) {              
              var data = {
                physicalnetworkid: args.context.physicalNetworks[0].id,
                hostname: args.data.hostname,
                insideportprofile: args.data.insideportprofile,
                clusterid: args.data.clusterid
              };
              
              $.ajax({
                url: createURL('addCiscoAsa1000vResource'),
                data: data,
                success: function(json){
                  var item = json.addCiscoAsa1000vResource.CiscoAsa1000vResource;
                  args.response.success({data: item});
                },
                error: function(data) {
                  args.response.error(parseXMLHttpResponse(data));
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
          name: 'CiscoASA1000v details',
          actions: {    
            remove: {
              label: 'delete CiscoASA1000v',
              messages: {
                confirm: function(args) {
                  return 'Please confirm you want to delete CiscoASA1000v';
                },
                notification: function(args) {
                  return 'delete CiscoASA1000v';
                }
              },
              action: function(args) {                               
                $.ajax({
                  url: createURL('deleteCiscoAsa1000vResource'),
                  data: {
                    resourceid: args.context.asa1000vDevices[0].resourceid
                  },                   
                  success: function(json) {
                    args.response.success();
                  },
                  error: function(data) {
                    args.response.error(parseXMLHttpResponse(data));
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
                  hostname: {
                    label: 'label.host'                    
                  }
                },
                {
                  insideportprofile: { label: 'Inside Port Profile' },
                  RESOURCE_NAME: { label: 'Resource Name' },
                  resourceid: { label: 'Resource ID' }                    
                }
              ],

              dataProvider: function(args) {               
                $.ajax({
                  url: createURL('listCiscoAsa1000vResources'),
                  data: {
                    resourceid: args.context.asa1000vDevices[0].resourceid
                  },  
                  success: function(json) {                     
                    var item = json.listCiscoAsa1000vResources.CiscoAsa1000vResource[0];  
                    args.response.success({ data: item });            
                  }
                });                 
              }
            }
          }
        }              
      }
    });
  };
}(jQuery, cloudStack));
