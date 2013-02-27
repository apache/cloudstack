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
  cloudStack.sections.regions = {
    title: 'label.menu.regions',
    id: 'regions',
    regionSelector: {
      dataProvider: function(args) {
        $.ajax({
          url: createURL('listRegions&listAll=true'),
          success: function(json) {
            var regions = json.listregionsresponse.region;

            args.response.success({
              data: regions ? regions : [
                { id: -1, name: '(Default region)' }
              ],
              activeRegionID: cloudStack.context.users.regionid ?
                cloudStack.context.users.regionid : 1
            });
          }
        }); 
      }
    },
    listView: {
      section: 'regions',
      fields: {
        name: { label: 'label.name' },
        id: { label: 'ID' },
        endpoint: { label: 'label.endpoint' }
      },
      actions: {
        add: {
          label: 'label.add.region',
          messages: {
            notification: function() { return 'label.add.region'; }
          },
          createForm: {
            title: 'label.add.region',
            desc: 'message.add.region',
            fields: {
              id: { label: 'label.id', validation: { required: true } },
              name: { label: 'label.name', validation: { required: true } },
              endpoint: { label: 'label.endpoint', validation: { url: true, required: true } },
              userapikey: { label: 'label.api.key' },
              userapisecretkey: { label: 'label.s3.secret_key' }
            }
          },
          action: function(args) {
            $.ajax({
              url: createURL('addRegion'),
              data: args.data,
              success: function(json) {
                var jobID = json.addregionresponse.jobid;

                args.response.success({ _custom: { jobId: jobID }});
                $(window).trigger('cloudStack.refreshRegions');
              },
              error: function(json) {
                args.response.error(parseXMLHttpResponse(json));
              } 
            });
          }
        }
      },
      dataProvider: function(args) {
        $.ajax({
          url: createURL('listRegions&listAll=true'),
          success: function(json) {
            var regions = json.listregionsresponse.region

            args.response.success({
              data: regions ? regions : []
            });
          },
          error: function(json) {
            args.response.error(parseXMLHttpResponse(json));
          } 
        });
      },
      detailView: {
        name: 'Region details',
        actions: {
          edit: {
            label: 'label.edit.region',
            action: function(args) {
              $.ajax({
                url: createURL('updateRegion'),
                data: args.data,
                success: function(json) {
                  args.response.success();
                  $(window).trigger('cloudStack.refreshRegions');
                },
                error: function(json) {
                  args.response.error(parseXMLHttpResponse(json));
                } 
              });
            }
          },
          remove: {
            label: 'label.remove.region',
            messages: {
              notification: function() { return 'label.remove.region'; },
              confirm: function() { return 'message.remove.region'; }
            },
            action: function(args) {
              var region = args.context.regions[0];

              $.ajax({ 
                url: createURL('removeRegion'),
                data: { id: region.id },
                success: function(json) {
                  args.response.success();
                  $(window).trigger('cloudStack.refreshRegions');
                },
                error: function(json) {
                  args.response.error(parseXMLHttpResponse(json));
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
                name: { label: 'label.name', isEditable: true },
              },
              {
                endpoint: { label: 'label.endpoint', isEditable: true },
                id: { label: 'label.id', isEditable: true }
              }
            ],
            dataProvider: function(args) {								  
              $.ajax({
                url: createURL('listRegions&listAll=true'),
                data: { id: args.context.regions[0].id },
                success: function(json) {
                  var region = json.listregionsresponse.region

                  args.response.success({
                    data: region ? region[0] : {}
                  });
                },
                error: function(json) {
                  args.response.error(parseXMLHttpResponse(json));
                } 
              });  
            }
          }
        }
      }
    }
  };
})(cloudStack);
