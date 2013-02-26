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
    listView: {
      section: 'regions',
      fields: {
        name: { label: 'label.name' },
        endpoint: { label: 'label.endpoint' }
      },
      dataProvider: function(args) {
        $.ajax({
          url: createURL('listRegions&listAll=true'),
          success: function(json) {
            var regions = json.listregionsresponse.region

            args.response.success({
              data: regions ? regions : []
            });
          }
        });
      },
      detailView: {
        name: 'Region details',
        tabs: {
          details: {
            title: 'label.details',
            fields: [
              {
                name: { label: 'label.name' },
              },
              {
                endpoint: { label: 'label.endpoint' },
                id: { label: 'label.id' }
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
                }
              });  
            }
          }
        }
      }
    }
  };
})(cloudStack);
