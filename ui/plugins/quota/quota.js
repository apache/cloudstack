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
              //return isAdmin() || isDomainAdmin();
              return true;
          },
          show: function() {
            var $quotaView = $('<div class="quota-container detail-view ui-tabs ui-widget ui-widget-content ui-corner-all">');
            var $toolbar = $('<div class="toolbar"><div class="section-switcher reduced-hide"><div class="section-select"><label>Quota Management</label></div></div></div>');
            var $tabs = $('<ul class="ui-tabs-nav ui-helper-reset ui-helper-clearfix ui-widget-header ui-corner-all">');
            var $tabViews = [];

            var sections = [{'id': 'quota-reports',
                             'name': 'Reports',
                             'render': function ($node) {
                                  $node.html("<p>REPORT View Stub</p>");
                              }
                            },
                            {
                             'id': 'quota-tariff',
                             'name' : 'Tariff Plan',
                             'render': function($node) {
                                  var tariffView = $('<div class="details" style="margin-top: -30px">');
                                  var tariffTable = $('<table style="margin-top: 15px">');

                                  tariffTable.appendTo($('<div class="data-table">').appendTo($('<div class="view list-view">').appendTo(tariffView)));

                                  var tariffTableHead = $('<tr>');
                                  $('<th>ID</th>').appendTo(tariffTableHead);
                                  $('<th>').html(_l('label.usage.type')).appendTo(tariffTableHead);
                                  $('<th>').html(_l('label.usage.unit')).appendTo(tariffTableHead);
                                  $('<th>').html(_l('label.quota.value')).appendTo(tariffTableHead);
                                  $('<th>').html(_l('label.quota.description')).appendTo(tariffTableHead);
                                  tariffTableHead.appendTo($('<thead>').appendTo(tariffTable));

                                  $.ajax({
                                      url: createURL('quotaTariffList'),
                                      success: function(json) {
                                          var items = json.quotatarifflistresponse.quotatariff;
                                          var tariffTableBody = $('<tbody>');

                                          for (var i = 0; i < items.length; i++) {
                                              var tariffTableBodyRow = $('<tr>');
                                              if (i % 2 == 0) {
                                                  tariffTableBodyRow.addClass('even');
                                              } else {
                                                  tariffTableBodyRow.addClass('odd');
                                              }
                                              $('<td>').html(items[i].usageType).appendTo(tariffTableBodyRow);
                                              $('<td>').html(items[i].usageName).appendTo(tariffTableBodyRow);
                                              $('<td>').html(items[i].usageUnit).appendTo(tariffTableBodyRow);

                                              if (isAdmin()) {
                                                  var valueCell = $('<td class="value actions">');
                                                  var value = $('<span>').html(items[i].tariffValue);
                                                  value.appendTo(valueCell);
                                                  valueCell.appendTo(tariffTableBodyRow);

                                                  var usageType = items[i].usageType;
                                                  var editButton = $('<div class="action edit quota-tariff-edit" alt="Change value" title="Change value"><span class="icon">&nbsp;</span></div>');
                                                  editButton.appendTo(valueCell);
                                                  editButton.attr('id', 'quota-tariff-edit-' + items[i].usageType);
                                                  editButton.click(function() {
                                                      console.log($(this));
                                                      var usageTypeId = $(this).context.id.replace('quota-tariff-edit-', '');
                                                      cloudStack.dialog.createForm({
                                                          form: {
                                                              title: 'label.quota.configuration',
                                                              fields: {
                                                                  quotaValue: {
                                                                      label: 'label.quota.value',
                                                                      validation: {
                                                                          required: true
                                                                      }
                                                                  }
                                                              }
                                                          },
                                                          after: function(args) {
                                                              $.ajax({
                                                                  url: createURL('quotaTariffUpdate'),
                                                                  data: {
                                                                      usagetype: usageTypeId,
                                                                      value: args.data.quotaValue
                                                                  },
                                                                  type: "POST",
                                                                  success: function(json) {
                                                                      $('#quota-tariff').click();
                                                                  },
                                                                  error: function(json) {
                                                                      // TODO: handle error?
                                                                  }
                                                              });
                                                          }
                                                      });
                                                  });
                                              } else {
                                                  $('<td>').html(items[i].tariffValue).appendTo(tariffTableBodyRow);
                                              }
                                              $('<td>').html(items[i].description).appendTo(tariffTableBodyRow);
                                              tariffTableBodyRow.appendTo(tariffTableBody);
                                          }
                                          tariffTableBody.appendTo(tariffTable);
                                      },
                                      error: function(data) {
                                          // TODO: Add error dialog?
                                      }
                                  });

                                  tariffView.appendTo($node);
                             }
                            },
                            {'id': 'quota-email',
                             'name': 'Email Templates',
                             'render': function($node) {
                                  $node.html("<p>EMAIL TEMPLATE STUB</p>");
                             }
                            }];


            for (idx in sections) {
                var tabLi = $('<li detail-view-tab="true" class="first ui-state-default ui-corner-top"><a href="#">' +  sections[idx].name+ '</a></li>');
                var tabView = $('<div class="detail-group ui-tabs-panel ui-widget-content ui-corner-bottom ui-tabs-hide">');

                tabLi.attr('id', sections[idx].id);
                tabView.attr('id', 'details-tab-' + sections[idx].id);

                tabLi.click(function() {
                    var tabIdx = 0;
                    for (sidx in sections) {
                        $('#' + sections[sidx].id).removeClass('ui-tabs-selected ui-state-active');
                        $('#details-tab-' + sections[sidx].id).addClass('ui-tabs-hide');
                        $('#details-tab-' + sections[sidx].id).empty();
                        if (sections[sidx].id === $(this).context.id) {
                            tabIdx = sidx;
                        }
                    }
                    $(this).addClass('ui-tabs-selected ui-state-active');
                    var tabDetails = $('#details-tab-' + $(this).context.id);
                    tabDetails.removeClass('ui-tabs-hide');
                    sections[tabIdx].render(tabDetails);
                });

                if (idx == 0) {
                    tabLi.addClass('ui-tabs-selected ui-state-active');
                    tabView.removeClass('ui-tabs-hide');
                    sections[idx].render(tabView);
                }

                tabLi.appendTo($tabs);
                $tabViews.push(tabView);
            }

            $toolbar.appendTo($quotaView);
            $tabs.appendTo($quotaView);
            for (idx in $tabViews) {
                $tabViews[idx].appendTo($quotaView);
            }
            return $quotaView;
          }

        });
  };
}(cloudStack));
