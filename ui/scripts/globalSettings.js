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
  cloudStack.sections['global-settings'] = {
    title: 'label.menu.global.settings',
    id: 'global-settings',
    sectionSelect: {
      label: 'label.select-view'
    },
    sections: {
      globalSettings: {
        type: 'select',
        title: 'label.menu.global.settings',
        listView: {
          label: 'label.menu.global.settings',
          actions: {
            edit: {
              label: 'label.change.value',
              action: function(args) {    
								var data = {
								  name: args.data.jsonObj.name,
									value: args.data.value
								};								
                $.ajax({
                  url: createURL('updateConfiguration'),
                  data: data,                  
                  success: function(json) {                
                    var item = json.updateconfigurationresponse.configuration;
                    if(item.category == "Usage")
                      cloudStack.dialog.notice({ message: _l('message.restart.mgmt.usage.server') });
                    else
                      cloudStack.dialog.notice({ message: _l('message.restart.mgmt.server') });	
                    args.response.success({data: item});
                  },
                  error: function(json) {                
                    args.response.error(parseXMLHttpResponse(json));
                  }
                });
              }
            }
          },
          fields: {
            name: { label: 'label.name', id: true },
            description: { label: 'label.description' },
            value: { label: 'label.value', editable: true }
          },
          dataProvider: function(args) {
            var data = {
              page: args.page,
              pagesize: pageSize
            };

            if (args.filterBy.search.value) {
              data.name = args.filterBy.search.value;
            }

            $.ajax({
              url: createURL('listConfigurations'),
              data: data,
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listconfigurationsresponse.configuration;
                args.response.success({ data: items });
              }
            });
          }
        }
      },

      ldapConfiguration:{
         type:'select',
         title:'LDAP Configuration',
         listView:{
            id:'ldap',
            label:'LDAP Configuration', 
            fields:{
                hostname: {label: 'Hostname'},
                queryfilter: {label: 'Query Filter'},
                searchbase: {label: 'Search Base'},
                port: {label: 'LDAP Port'},
                ssl: {
                  label: 'SSL'


                  }


             },
              dataProvider:function(args){
                   var data = {};
                   listViewDataProvider(args, data);
                    $.ajax({
              url: createURL(''),
              data: data,
                success: function(json) {
               // var items = json.listhypervisorcapabilitiesresponse.hypervisorCapabilities;
                args.response.success({data:items});
                   },
                   error: function(data) {
                args.response.error(parseXMLHttpResponse(data));
                  }
               });
           },

          actions: {
               add:{

               label: 'Configure LDAP',
 
               messages: {
                confirm: function(args) {
                  return 'Do you really want to configure LDAP ? ';
                },
                notification: function(args) {
                  return 'LDAP configured';
                }
              },
         
              createForm: {
                    
                    title: 'Configure LDAP',
                    fields:{
                        name:{label: 'Bind Username' , validation: {required:true} },
                        password: {label: 'Bind Password', validation: {required: true },isPassword:true },
                        hostname: {label:'Hostname' , validation:{required:true}},
                        queryfilter: {label:'Query Filter' , validation: {required:true}},
                        searchbase: {label:'SearchBase',validation:{required:true}},
                        ssl:  {
                               label:'SSL' ,
                               isBoolean:true,
                               isChecked:false
                               // var $form = $(this).closest("form");

                                
                            },
                        port: {  label: 'Port' , defaultValue: '389' },
                        truststore:{ label:'Trust Store' , isHidden:true  },
                        truststorepassword:{ label:'Trust Store Password' ,isHidden:true }
                        
                     }
 

              },
            

                action:function(args) {
                         var $form = $(this).closest("form");
                         if($form.find('.form-item [rel=port]').find('input[ type=checkbox]').is(":checked") ==true) {
                                  $form.find('.form-item[rel=truststore]').attr("isHidden",false);
                                  $form.find('.form-item[rel=truststorepassword]').attr("isHidden",false);
                                        }


                }
             }

           }

 

          }



       },
      hypervisorCapabilities: {
        type: 'select',
        title: 'label.hypervisor.capabilities',
        listView: {
          id: 'hypervisorCapabilities',
          label: 'label.hypervisor.capabilities',
          fields: {
            hypervisor: { label: 'label.hypervisor' },
            hypervisorversion: { label: 'label.hypervisor.version' },
            maxguestslimit: { label: 'label.max.guest.limit' }
          },
          dataProvider: function(args) {					  
						var data = {};
						listViewDataProvider(args, data);					
										  
            $.ajax({
              url: createURL('listHypervisorCapabilities'),
              data: data,              
              success: function(json) {
                var items = json.listhypervisorcapabilitiesresponse.hypervisorCapabilities;
                args.response.success({data:items});
              },
              error: function(data) {
                args.response.error(parseXMLHttpResponse(data));
              }
            });
          },

          detailView: {
            name: 'label.details',
            actions: {
              edit: {
                label: 'label.edit',
                action: function(args) {
                  var data = {
									  id: args.context.hypervisorCapabilities[0].id,
										maxguestslimit: args.data.maxguestslimit
									};
                  
                  $.ajax({
                    url: createURL('updateHypervisorCapabilities'),
                    data: data,
                    success: function(json) {
                      var item = json.updatehypervisorcapabilitiesresponse['null'];
                      args.response.success({data: item});
                    },
                    error: function(data) {
                      args.response.error(parseXMLHttpResponse(data));
                    }
                  });
                }
              }
            },

            tabs: {
              details: {
                title: 'label.details',
                fields: [
                  {
                    id: { label: 'label.id' },
                    hypervisor: { label: 'label.hypervisor' },
                    hypervisorversion: { label: 'label.hypervisor.version' },
                    maxguestslimit: {
                      label: 'label.max.guest.limit',
                      isEditable: true
                    }
                  }
                ],
                dataProvider: function(args) {
                  args.response.success(
                    {
                      data:args.context.hypervisorCapabilities[0]
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
})(cloudStack);
