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
        // Quota Global Settings
        var gsView = cloudStack.sections['global-settings'].sections;
        gsView.quotaConfiguration = {
                                        type: 'select',
                                        title: 'label.quota.configuration',
                                        listView: {
                                            id: 'quota',
                                            label: 'label.quota.configuration',
                                            fields: {
                                                usageType: {
                                                    label: 'label.usage.type'
                                                },
                                                usageUnit: {
                                                    label: 'label.usage.unit'
                                                },
                                                currencyValue: {
                                                    label: 'label.quota.value'
                                                },
                                                description: {
                                                    label: 'label.quota.description'
                                                }
                                            },
                                            dataProvider: function(args) {
                                                var data = {};
                                                listViewDataProvider(args, data);
                                                if (data.hasOwnProperty('page') && data.page > 1) {
                                                    args.response.success({
                                                        data: []
                                                    });
                                                    return;
                                                }
                                                $.ajax({
                                                    url: createURL('quotaMapping'),
                                                    data: data,
                                                    success: function(json) {
                                                        var items = json.quotaconfigurationresponse.quotamapping;
                                                        args.response.success({
                                                            data: items
                                                        });
                                                    },
                                                    error: function(data) {
                                                        args.response.error(parseXMLHttpResponse(data));
                                                    }
                                                });
                                            },
                                            detailView: {
                                                name: 'label.details',
                                                actions: {
                                                    remove: {
                                                        label: 'label.quota.remove',
                                                        messages: {
                                                            notification: function(args) {
                                                                return 'label.quota.remove';
                                                            },
                                                            confirm: function() {
                                                                return 'label.quota.remove';
                                                            }
                                                        },
                                                        action: function(args) {
                                                            $.ajax({
                                                                url: createURL("deleteQuotaConfiguration&hostname=" + args.context.quotaConfiguration[0].hostname),
                                                                success: function(json) {
                                                                    args.response.success();
                                                                }
                                                            });
                                                            $(window).trigger('cloudStack.fullRefresh');
                                                        }
                                                    },
                                                    edit: {
                                                        label: 'label.quota.configure',
                                                        messages: {
                                                            notification: function(args) {
                                                                return 'label.quota.configure';
                                                            },
                                                            confirm: function() {
                                                                return 'label.quota.configure';
                                                            }
                                                        },
                                                        action: function(args) {
                                                            var data = {
                                                                'type': args.context.quotaConfiguration[0].usageType,
                                                                'usageUnit': args.data.usageUnit,
                                                                'value': args.data.currencyValue,
                                                                'description': args.data.description
                                                            };
                                                            $.ajax({
                                                                url: createURL("editQuotaConfiguration"),
                                                                data: data,
                                                                success: function(json) {
                                                                    args.response.success();
                                                                }
                                                            });
                                                            $(window).trigger('cloudStack.fullRefresh');
                                                        }
                                                    }


                                                },
                                                tabs: {
                                                    details: {
                                                        title: 'label.quota.configuration',
                                                                fields: [{
                                                                usageType: {
                                                                    label: 'label.usage.type'
                                                                },
                                                                usageUnit: {
                                                                    label: 'label.usage.unit',
                                                                    isEditable: true
                                                                },
                                                                currencyValue: {
                                                                    label: 'label.quota.value',
                                                                    isEditable: true
                                                                },
                                                                description: {
                                                                    label: 'label.quota.description',
                                                                    isEditable: true
                                                                }
                                                        }],
                                                        dataProvider: function(args) {
                                                            var items = [];
                                                            $.ajax({
                                                                url: createURL("quotaMapping&type=" + args.context.quotaConfiguration[0].usageType),
                                                                dataType: "json",
                                                                async: true,
                                                                success: function(json) {
                                                                    var item = json.quotaconfigurationresponse.quotamapping;
                                                                    args.response.success({
                                                                        data: item[0]
                                                                    });
                                                                }
                                                            });
                                                        }
                                                    }
                                                }
                                            },
                                            actions: {
                                                add: {
                                                    label: 'label.quota.configure',
                                                    messages: {
                                                        confirm: function(args) {
                                                            return 'message.configure.quota';
                                                        },
                                                        notification: function(args) {
                                                            return 'label.quota.configure';
                                                        }
                                                    },
                                                    createForm: {
                                                        title: 'label.quota.configure',
                                                        fields: {
                                                            usageType: {
                                                                label: 'label.usage.type',
                                                                validation: {
                                                                    required: true
                                                                }
                                                            },
                                                            periodUnit: {
                                                                label: 'label.usage.unit',
                                                                validation: {
                                                                    required: true
                                                                }
                                                            },
                                                            quotaValue: {
                                                                label: 'label.quota.value',
                                                                validation: {
                                                                    required: true
                                                                }
                                                            },
                                                            quotaDescription: {
                                                                label: 'label.quota.description',
                                                                validation: {
                                                                    required: false
                                                                }
                                                            }
                                                        }
                                                    },
                                                    action: function(args) {
                                                        var array = [];
                                                        array.push("&type=" + todb(args.data.usageType));
                                                        array.push("&period=" + todb(args.data.periodUnit));;
                                                        array.push("&value=" + todb(args.data.quotaValue));;
                                                        array.push("&description=" + todb(args.data.quotaDescription));;
                                                        $.ajax({
                                                            url: createURL("addQuotaConfiguration" + array.join("")),
                                                            dataType: "json",
                                                            async: true,
                                                            success: function(json) {
                                                                var items = json.quotaconfigurationresponse.QuotaAddConfiguration;
                                                                args.response.success({
                                                                    data: items
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
                                    };

        // Quota view on user page
        var userView = cloudStack.sections.accounts.sections.users.listView.detailView;
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
