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
        args.response.success({
          data: [
            { id: 1, name: 'Affinity Group 1', type: 'Affinity' },
            { id: 2, name: 'Affinity Group 2', type: 'Anti-affinity' },
            { id: 3, name: 'Anti-affinity Group', type: 'Anti-affinity' }
          ]
        });
      },
      actions: {
        add: {
          label: 'label.add.affinity.group',

          messages: {
            confirm: function(args) {
              return 'message.add.volume';
            },
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
              type: {
                label: 'label.availability.zone',
                select: function(args) {
                  args.response.success({
                    data: [
                      { id: 'Affinity', description: 'Affinity' },
                      { id: 'AntiAffinity', description: 'Anti-Affinity' }
                    ]
                  });
                }
              },              
              availabilityZone: {
                label: 'label.availability.zone',
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
            }
          },

          action: function(args) {
            args.response.success();
          },

          notification: {
            poll: function(args) { args.complete(); }
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
