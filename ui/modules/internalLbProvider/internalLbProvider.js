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
  cloudStack.modules.internalLbProvider = function(module) {    
    var internalLbDeviceViewAll = [
      {
        label: 'Devices',
        path: '_zone.internalLbDevices'
      }
    ];

    var internalLbListView = {
      id: 'internalLbDevices',
      fields: {
        resourcename: { label: 'Resource Name' },
        provider: { label: 'Provider' }
      },      
      dataProvider: function(args) {        
        args.response.success({ data: [] });  
      },    
      actions: {
        add: {
          label: 'Add internal LB device',

          messages: {           
            notification: function(args) {
              return 'Add internal LB device';
            }
          },

          createForm: {
            title: 'Add internal LB device',
            fields: {
              hostname: {
                label: 'label.host',                
                validation: { required: true }
              },
              username: {
                label: 'label.username',                
                validation: { required: true }
              },
              password: {
                label: 'label.password', 
                isPassword: true,
                validation: { required: true }
              }            
            }
          },

          action: function(args) {
            args.response.success();
          },

          notification: {
            poll: function(args) {
              args.complete();
            }
          }
        }
      },
      
      detailView: {
        name: 'Internal LB resource details',
        actions: {    
          remove: {
            label: 'delete Internal LB resource',
            messages: {
              confirm: function(args) {
                return 'Please confirm you want to delete Internal LB resource';
              },
              notification: function(args) {
                return 'delete Internal LB resource';
              }
            },
            action: function(args) {                               
              args.response.success();
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
               resourcename: { label: 'Resource Name' }
             },
             {   
               resourceid: { label: 'Resource ID'},
               provider: { label: 'Provider' },
               RESOURCE_NAME: { label: 'Resource Name'}
             }
           ],
           dataProvider: function(args) {   
             args.response.success({ data: args.context.internalLbDevices[0] });   
            }    
          }
        }
      }       
    };

    var internalLbProviderDetailView = {
      id: 'internalLbProvider',
      label: 'internal LB',
      viewAll: internalLbDeviceViewAll,
      tabs: {
        details: {
          title: 'label.details',
          fields: [
            {
              name: { label: 'label.name' }
            },
            {
              state: { label: 'label.state' }, 
              id: { label: 'label.id' },
              servicelist: {
                label: 'Services',
                converter: function(args){  
                  if(args)                  
                    return args.join(', ');
                  else
                    return '';
                }
              }
            }
          ],
          dataProvider: function(args) {            
            $.ajax({
              url: createURL('listNetworkServiceProviders'),
              data: {
                name: 'InternalLb',
                physicalnetworkid: args.context.physicalNetworks[0].id   
              },              
              success: function(json){                  
                var items = json.listnetworkserviceprovidersresponse.networkserviceprovider;
                if(items != null && items.length > 0) {   
                  args.response.success({ data: items[0] });                  
                }
                else {
                  args.response.success({ 
                    data: {
                      name: 'InternalLb',
                      state: 'Disabled'
                    }
                  })
                }
              }
            });                 
          }
        }
      }
    };
   
    module.infrastructure.networkServiceProvider({
      id: 'internalLb',
      name: 'Internal LB',
      //state: 'Disabled', //don't know state until log in and visit Infrastructure menu > zone detail > physical network > network service providers
      listView: internalLbListView,

      detailView: internalLbProviderDetailView
    });
  };
}(jQuery, cloudStack));
