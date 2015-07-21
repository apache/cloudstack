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

var g_quotaCurrency = '';
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
            $.ajax({
                url: createURL('listConfigurations'),
                data: {
                    name: 'quota.currency.symbol'
                },
                success: function(json) {
                    if (json.hasOwnProperty('listconfigurationsresponse') && json.listconfigurationsresponse.hasOwnProperty('configuration')) {
                        g_quotaCurrency = json.listconfigurationsresponse.configuration[0].value + ' ';
                    }
                },
                error: function(data) {
                    // FIXME: what to do on error?
                }
            });

            var $quotaView = $('<div class="quota-container detail-view ui-tabs ui-widget ui-widget-content ui-corner-all">');
            var $toolbar = $('<div class="toolbar"><div class="section-switcher reduced-hide"><div class="section-select"><label>Quota Management</label></div></div></div>');
            var $tabs = $('<ul class="ui-tabs-nav ui-helper-reset ui-helper-clearfix ui-widget-header ui-corner-all">');
            var $tabViews = [];

            var sections = [{'id': 'quota-statement',
                             'name': 'Statement',
                             'render': function ($node) {
                                  var statementView = $('<div class="details" style="padding: 10px">');
                                  var generatedStatement = $('<div class="quota-generated-statement">');

                                  var statementForm = $('<div class="quota-statement">');
                                  var domainDropdown = $('<div class="quota-domain-dropdown">');
                                  var accountDropdown = $('<div class="quota-account-dropdown">');
                                  var startDateInput = $('<input type="text" id="quota-statement-start-date">');
                                  var endDateInput = $('<input type="text" id="quota-statement-end-date">');
                                  var generateStatementButton = $('<button id="quota-get-statement-button">').html("Generate Statement");

                                  startDateInput.datepicker({
                                      defaultDate: "+1w",
                                      changeMonth: true,
                                      dateFormat: "yy-mm-dd",
                                      onClose: function (selectedDate) {
                                          endDateInput.datepicker("option", "minDate", selectedDate);
                                      }
                                  });

                                  endDateInput.datepicker({
                                      defaultDate: "+1w",
                                      changeMonth: true,
                                      dateFormat: "yy-mm-dd",
                                      onClose: function (selectedDate) {
                                          startDateInput.datepicker("option", "maxDate", selectedDate);
                                      }
                                  });

                                  generateStatementButton.click(function() {
                                      var domainId = g_domainid;
                                      if (isAdmin() || isDomainAdmin()) {
                                          domainId = domainDropdown.find("select :selected").val();
                                      }
                                      var accountId = accountDropdown.find("select :selected").val();
                                      var account = accountDropdown.find("select :selected").html();
                                      var startDate = startDateInput.val();
                                      var endDate = endDateInput.val();

                                      $.ajax({
                                          url: createURL('quotaStatement'),
                                          data: {
                                              domainid: domainId,
                                              accountid: accountId,
                                              account: account,
                                              startdate: startDate,
                                              enddate: endDate
                                          },
                                          success: function(json) {
                                              var statement = json.quotastatementresponse.statement;
                                              var totalQuota = statement.totalquota;
                                              var quotaUsage = statement.quotausage;

                                              generatedStatement.empty();
                                              $("<br><hr>").appendTo(generatedStatement);
                                              $("<p>").html("Total Quota: " + g_quotaCurrency + totalQuota).appendTo(generatedStatement);

                                              if (quotaUsage.length < 1) {
                                                  return;
                                              }

                                              $("<p>").html("<br>Quota Usage:").appendTo(generatedStatement);
                                              var statementTable = $('<table>');
                                              statementTable.appendTo($('<div class="data-table">').appendTo(generatedStatement));

                                              var statementTableHead = $('<tr>');
                                              $('<th>').html(_l('label.usage.type')).appendTo(statementTableHead);
                                              $('<th>').html(_l('label.quota.description')).appendTo(statementTableHead);
                                              $('<th>').html(_l('label.quota.value')).appendTo(statementTableHead);
                                              $('<th>').html(_l('label.usage.unit')).appendTo(statementTableHead);
                                              $('<th>').html("Start Date").appendTo(statementTableHead);
                                              $('<th>').html("End Date").appendTo(statementTableHead);
                                              statementTableHead.appendTo($('<thead>').appendTo(statementTable));

                                              var statementTableBody = $('<tbody>');
                                              for (var i = 0; i < quotaUsage.length; i++) {
                                                  var statementTableBodyRow = $('<tr>');
                                                  if (i % 2 == 0) {
                                                      statementTableBodyRow.addClass('even');
                                                  } else {
                                                      statementTableBodyRow.addClass('odd');
                                                  }
                                                  $('<td>').html(quotaUsage[i].type).appendTo(statementTableBodyRow);
                                                  $('<td>').html(quotaUsage[i].name).appendTo(statementTableBodyRow);
                                                  $('<td>').html(g_quotaCurrency + quotaUsage[i].quota).appendTo(statementTableBodyRow);
                                                  $('<td>').html(quotaUsage[i].unit).appendTo(statementTableBodyRow);
                                                  $('<td>').html(quotaUsage[i].startdate).appendTo(statementTableBodyRow);
                                                  $('<td>').html(quotaUsage[i].enddate).appendTo(statementTableBodyRow);
                                                  statementTableBodyRow.appendTo(statementTableBody);
                                              }
                                              statementTableBody.appendTo(statementTable);
                                          },
                                          error: function(json) {
                                              generatedStatement.empty();
                                              var error = JSON.parse(json.responseText);
                                              if (error.hasOwnProperty('quotastatementresponse')) {
                                                  $("<br><hr>").appendTo(generatedStatement);
                                                  $("<p>").html("Error: " + error.quotastatementresponse.errortext).appendTo(generatedStatement);
                                              }
                                          }
                                      });
                                  });

                                  domainDropdown.appendTo(statementForm);
                                  accountDropdown.appendTo(statementForm);
                                  startDateInput.appendTo($("<p>Start Date: </p>").appendTo(statementForm));
                                  endDateInput.appendTo($("<p>End Date: </p>").appendTo(statementForm));

                                  generateStatementButton.appendTo(statementForm);

                                  var accountLister = function(selectedDomainId) {
                                      var data = {listall: true};
                                      if (selectedDomainId) {
                                          data.domainid = selectedDomainId;
                                      }
                                      $.ajax({
                                          url: createURL('listAccounts'),
                                          data : data,
                                          success: function(json) {
                                              accountDropdown.empty();
                                              if (json.hasOwnProperty('listaccountsresponse') && json.listaccountsresponse.hasOwnProperty('account')) {
                                                  var accounts = json.listaccountsresponse.account;
                                                  var dropdown = $('<select>');
                                                  for (var i = 0; i < accounts.length; i++) {
                                                      $('<option value="' + accounts[i].id + '">' + accounts[i].name + '</option>').appendTo(dropdown);
                                                  }
                                                  $('<span>Account: </span>').appendTo(accountDropdown);
                                                  dropdown.appendTo(accountDropdown);
                                              } else {
                                                  $('<span>Accounts: No accounts found in the selected domain</span>').appendTo(accountDropdown);
                                              }
                                          },
                                          error: function(data) {
                                              // TODO: Add error dialog?
                                          }
                                      });
                                  };

                                  var domainLister = function() {
                                      $.ajax({
                                          url: createURL('listDomains'),
                                          data: {
                                              listall: true
                                          },
                                          success: function(json) {
                                              var domains = json.listdomainsresponse.domain;
                                              var dropdown = $('<select>');
                                              if (domains.length > 1) {
                                                  $('<option value="">--- Select Domain ---</option>').appendTo(dropdown);
                                              }
                                              for (var i = 0; i < domains.length; i++) {
                                                  $('<option value="' + domains[i].id + '">' + domains[i].name + '</option>').appendTo(dropdown);
                                              }
                                              $('<span>Domain: </span>').appendTo(domainDropdown);
                                              dropdown.appendTo(domainDropdown);

                                              dropdown.change(function() {
                                                  var selectedDomainId = $(this).find(':selected').val();
                                                  if (!selectedDomainId) {
                                                      accountDropdown.empty();
                                                      $('<span>Accounts: Select a valid domain to start with</span>').appendTo(accountDropdown);
                                                      return;
                                                  }
                                                  accountLister(selectedDomainId);
                                              });
                                              dropdown.change();
                                          },
                                          error: function(data) {
                                              // FIXME: what to do on error?
                                          }
                                      });
                                  };

                                  if (isAdmin() || isDomainAdmin()) {
                                      domainLister();
                                  } else {
                                      accountLister(g_domainid);
                                  }

                                  statementForm.appendTo(statementView);
                                  generatedStatement.appendTo(statementView);
                                  statementView.appendTo($node);
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
                                                  var value = $('<span>').html(g_quotaCurrency + items[i].tariffValue);
                                                  value.appendTo(valueCell);
                                                  valueCell.appendTo(tariffTableBodyRow);

                                                  var usageType = items[i].usageType;
                                                  var editButton = $('<div class="action edit quota-tariff-edit" alt="Change value" title="Change value"><span class="icon">&nbsp;</span></div>');
                                                  editButton.appendTo(valueCell);
                                                  editButton.attr('id', 'quota-tariff-edit-' + items[i].usageType);
                                                  editButton.click(function() {
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
                            {'id': 'quota-credit',
                             'name': 'Manage Credits',
                             'render': function($node) {
                                  var manageCreditView = $('<div class="details" style="padding: 10px">');
                                  var creditStatement = $('<div class="quota-credit-statement">');

                                  var creditForm = $('<div class="quota-credit">');
                                  var domainDropdown = $('<div class="quota-domain-dropdown">');
                                  var accountDropdown = $('<div class="quota-account-dropdown">');
                                  var quotaValueInput = $('<input type="text">');
                                  var addCreditButton = $('<button id="quota-add-credit-button">').html("Add Credit");

                                  addCreditButton.click(function() {
                                      if (isAdmin() || isDomainAdmin()) {
                                          domainId = domainDropdown.find("select :selected").val();
                                      } else {
                                          return;
                                      }
                                      var account = accountDropdown.find("select :selected").val();
                                      var quotaValue = quotaValueInput.val();

                                      $.ajax({
                                          url: createURL('quotaCredits'),
                                          data: {
                                              account: account,
                                              domainid: domainId,
                                              value: quotaValue
                                          },
                                          success: function(json) {
                                              quotaValueInput.val('');
                                              $('<hr>').appendTo(creditStatement);
                                              $('<p>').html('Credit amount ' + g_quotaCurrency + json.quotacreditsresponse.quotacredits.credits + ' added to the account').appendTo(creditStatement);
                                          },
                                          error: function(json) {
                                          }
                                      });
                                  });

                                  domainDropdown.appendTo(creditForm);
                                  accountDropdown.appendTo(creditForm);
                                  quotaValueInput.appendTo($("<p>Quota Credit Value: </p>").appendTo(creditForm));

                                  addCreditButton.appendTo(creditForm);

                                  var accountLister = function(selectedDomainId) {
                                      var data = {listall: true};
                                      if (selectedDomainId) {
                                          data.domainid = selectedDomainId;
                                      }
                                      $.ajax({
                                          url: createURL('listAccounts'),
                                          data : data,
                                          success: function(json) {
                                              accountDropdown.empty();
                                              if (json.hasOwnProperty('listaccountsresponse') && json.listaccountsresponse.hasOwnProperty('account')) {
                                                  var accounts = json.listaccountsresponse.account;
                                                  var dropdown = $('<select>');
                                                  for (var i = 0; i < accounts.length; i++) {
                                                      $('<option value="' + accounts[i].name + '">' + accounts[i].name + '</option>').appendTo(dropdown);
                                                  }
                                                  $('<span>Account: </span>').appendTo(accountDropdown);
                                                  dropdown.appendTo(accountDropdown);
                                              } else {
                                                  $('<span>Accounts: No accounts found in the selected domain</span>').appendTo(accountDropdown);
                                              }
                                          },
                                          error: function(data) {
                                              // TODO: Add error dialog?
                                          }
                                      });
                                  };

                                  var domainLister = function() {
                                      $.ajax({
                                          url: createURL('listDomains'),
                                          data: {
                                              listall: true
                                          },
                                          success: function(json) {
                                              var domains = json.listdomainsresponse.domain;
                                              var dropdown = $('<select>');
                                              if (domains.length > 1) {
                                                  $('<option value="">--- Select Domain ---</option>').appendTo(dropdown);
                                              }
                                              for (var i = 0; i < domains.length; i++) {
                                                  $('<option value="' + domains[i].id + '">' + domains[i].name + '</option>').appendTo(dropdown);
                                              }
                                              $('<span>Domain: </span>').appendTo(domainDropdown);
                                              dropdown.appendTo(domainDropdown);

                                              dropdown.change(function() {
                                                  var selectedDomainId = $(this).find(':selected').val();
                                                  if (!selectedDomainId) {
                                                      accountDropdown.empty();
                                                      $('<span>Accounts: Select a valid domain to start with</span>').appendTo(accountDropdown);
                                                      return;
                                                  }
                                                  accountLister(selectedDomainId);
                                              });
                                              dropdown.change();
                                          },
                                          error: function(data) {
                                              // FIXME: what to do on error?
                                          }
                                      });
                                  };

                                  domainLister();

                                  creditForm.appendTo(manageCreditView);
                                  creditStatement.appendTo(manageCreditView);
                                  manageCreditView.appendTo($node);
                             }
                            },
                            {'id': 'quota-email',
                             'name': 'Email Templates',
                             'render': function($node) {
                                  $node.html("<p>EMAIL TEMPLATE STUB</p>");
                             }
                            }];

            if (isAdmin()) {
            } else if (isDomainAdmin()) {
                sections = $.grep(sections, function(item) {
                    return ['quota-credit', 'quota-email'].indexOf(item.id) < 0;
                });
            } else {
                sections = $.grep(sections, function(item) {
                    return ['quota-credit', 'quota-email'].indexOf(item.id) < 0;
                });
            }

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
