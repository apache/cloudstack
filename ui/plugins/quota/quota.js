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
(function (cloudStack) {
    cloudStack.plugins.quota = function(plugin) {
        var userView = cloudStack.sections.accounts.sections.users.listView.detailView;
        // Add view to show quota information
        userView.tabs.quota = {
                                  title: 'Quota',
                                  fields: [{
                                      username: {
                                          label: 'label.name',
                                          isEditable: true,
                                          validation: {
                                              required: true
                                          }
                                      }
                                  }, {
                                      id: {
                                          label: 'label.id'
                                      },
                                      state: {
                                          label: 'label.state'
                                      },
                                      account: {
                                          label: 'label.account.name'
                                      }
                                  }],
                                  dataProvider: function(args) {
                                      $.ajax({
                                          url: createURL('listUsers'),
                                          data: {
                                              id: args.context.users[0].id
                                          },
                                          success: function(json) {
                                              args.response.success({
                                                  data: json.listusersresponse.user[0]
                                              });
                                          }
                                      });
                                  }
                              };

        plugin.ui.addSection({
          id: 'quota',
          title: 'Quota',
          showOnNavigation: true,
          preFilter: function(args) {
              return isAdmin() || isDomainAdmin();
          },
          show: function() {
            return $('<div style="width:100%;height:100%">').html('Hello World');
          }

        });
  };
}(cloudStack));
