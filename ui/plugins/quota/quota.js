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
        plugin.ui.addSection({
          id: 'quota',
          title: 'Quota',
          showOnNavigation: true,
          preFilter: function(args) {
              return true;
          },
          show: function() {
            var $quotaView = $('<div class="quota-container detail-view ui-tabs ui-widget ui-widget-content ui-corner-all">');
            var $toolbar = $('<div class="toolbar"><div class="section-switcher reduced-hide"><div class="section-select"><label>Quota Management</label></div></div></div>');
            var $tabs = $('<ul class="ui-tabs-nav ui-helper-reset ui-helper-clearfix ui-widget-header ui-corner-all">');
            var $tabViews = [];

            var sections = [{'id': 'quota-statement',
                             'name': 'Statement',
                             'render': function ($node) {
                                  var statementView = $('<div class="details quota-text" style="padding: 10px">');
                                  var generatedStatement = $('<div class="quota-generated-statement">');
                                  var generatedBalanceStatement = $('<div class="quota-generated-balance-statement">');

                                  var statementForm = $('<div class="quota-statement select-container">');
                                  var domainDropdown = $('<div class="quota-domain-dropdown quota-element">');
                                  var accountDropdown = $('<div class="quota-account-dropdown quota-element">');
                                  var startDateInput = $('<input type="text" class="quota-input" id="quota-statement-start-date" style="margin-left: 13px">');
                                  var endDateInput = $('<input type="text" class="quota-input" id="quota-statement-end-date" style="margin-left: 13px">');
                                  var generateStatementButton = $('<button class="quota-element quota-button" id="quota-get-statement-button">').html("Generate Statement");

                                  startDateInput.datepicker({
                                      defaultDate: new Date(),
                                      changeMonth: true,
                                      dateFormat: "yy-mm-dd",
                                      onClose: function (selectedDate) {
                                          endDateInput.datepicker("option", "minDate", selectedDate);
                                      }
                                  });

                                  endDateInput.datepicker({
                                      defaultDate: new Date(),
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

                                      if (!domainId || !account) {
                                          generatedStatement.empty();
                                          $("<br><hr>").appendTo(generatedStatement);
                                          $("<p class='quota-text'>").html("Error: Please select valid domain and account").appendTo(generatedStatement);
                                          return;
                                      }

                                      if (!startDate || !endDate) {
                                          generatedStatement.empty();
                                          $("<br><hr>").appendTo(generatedStatement);
                                          $("<p class='quota-text'>").html("Error: Please select start and end dates").appendTo(generatedStatement);
                                          return;
                                      }

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
                                              if (statement.currency) {
                                                  g_quotaCurrency = statement.currency.trim() + ' ';
                                              }

                                              generatedStatement.empty();
                                              $("<hr>").appendTo(generatedStatement);

                                              if (quotaUsage.length < 1) {
                                                  return;
                                              }

                                              $('<h1 class="quota-bold">').html("<br>Usage Statement").appendTo(generatedStatement);
                                              var statementTable = $('<table>');
                                              statementTable.appendTo($('<div class="data-table">').appendTo(generatedStatement));

                                              var statementTableHead = $('<tr>');
                                              $('<th>').html('Usage Description').appendTo(statementTableHead);
                                              $('<th>').html('Usage Unit').appendTo(statementTableHead);
                                              $('<th>').html('Amount').appendTo(statementTableHead);
                                              statementTableHead.appendTo($('<thead>').appendTo(statementTable));

                                              // Add total usage
                                              quotaUsage.push({type: '', unit: '', name: '<span class="quota-bold">Total Quota Usage</span>', quota: totalQuota, });

                                              var statementTableBody = $('<tbody>');
                                              for (var i = 0; i < quotaUsage.length; i++) {
                                                  var statementTableBodyRow = $('<tr>');
                                                  if (i % 2 == 0) {
                                                      statementTableBodyRow.addClass('even');
                                                  } else {
                                                      statementTableBodyRow.addClass('odd');
                                                  }
                                                  $('<td>').html(quotaUsage[i].name).appendTo(statementTableBodyRow);
                                                  $('<td>').html(quotaUsage[i].unit).appendTo(statementTableBodyRow);
                                                  $('<td>').html(g_quotaCurrency + quotaUsage[i].quota).appendTo(statementTableBodyRow);
                                                  statementTableBodyRow.appendTo(statementTableBody);
                                              }
                                              statementTableBody.appendTo(statementTable);
                                          },
                                          error: function(data) {
                                              generatedStatement.empty();
                                              cloudStack.dialog.notice({
                                                  message: parseXMLHttpResponse(data)
                                              });
                                          }
                                      });

                                      $.ajax({
                                          url: createURL('quotaBalance'),
                                          data: {
                                              domainid: domainId,
                                              account: account,
                                              startdate: startDate,
                                              enddate: endDate
                                          },
                                          success: function(json) {
                                              var statement = json.quotabalanceresponse.balance;
                                              var credits = statement.credits;
                                              var startBalance = statement.startquota;
                                              var startBalanceDate = statement.startdate;
                                              var endBalance = statement.endquota;
                                              var endBalanceDate = statement.enddate;
                                              if (statement.currency) {
                                                  g_quotaCurrency = statement.currency.trim() + ' ';
                                              }

                                              generatedBalanceStatement.empty();
                                              $("<br>").appendTo(generatedBalanceStatement);

                                              $('<p class="quota-bold">').html("<br>Quota Balance Statement").appendTo(generatedBalanceStatement);
                                              var statementTable = $('<table>');
                                              statementTable.appendTo($('<div class="data-table">').appendTo(generatedBalanceStatement));

                                              var statementTableHead = $('<tr>');
                                              $('<th>').html('Description').appendTo(statementTableHead);
                                              $('<th>').html("Date").appendTo(statementTableHead);
                                              $('<th>').html('Amount').appendTo(statementTableHead);
                                              statementTableHead.appendTo($('<thead>').appendTo(statementTable));

                                              var statementTableBody = $('<tbody>');
                                              if (startBalance) {
                                                  var statementTableBodyRow = $('<tr>');
                                                  $('<td>').html("Start Quota Balance").appendTo(statementTableBodyRow);
                                                  $('<td>').html(startBalanceDate).appendTo(statementTableBodyRow);
                                                  $('<td>').html(g_quotaCurrency + startBalance).appendTo(statementTableBodyRow);
                                                  statementTableBodyRow.appendTo(statementTableBody);
                                              }

                                              for (var i = 0; i < credits.length; i++) {
                                                  var statementTableBodyRow = $('<tr>');
                                                  if (i % 2 == 0) {
                                                      statementTableBodyRow.addClass('even');
                                                  } else {
                                                      statementTableBodyRow.addClass('odd');
                                                  }
                                                  $('<td>').html("Credit").appendTo(statementTableBodyRow);
                                                  $('<td>').html(credits[i].updated_on).appendTo(statementTableBodyRow);
                                                  $('<td>').html(g_quotaCurrency + credits[i].credits).appendTo(statementTableBodyRow);
                                                  statementTableBodyRow.appendTo(statementTableBody);
                                              }

                                              if (endBalance) {
                                                  var statementTableBodyRow = $('<tr>');
                                                  $('<td class="quota-bold">').html("Final Quota Balance").appendTo(statementTableBodyRow);
                                                  $('<td>').html(endBalanceDate).appendTo(statementTableBodyRow);
                                                  $('<td>').html(g_quotaCurrency + endBalance).appendTo(statementTableBodyRow);
                                                  statementTableBodyRow.appendTo(statementTableBody);
                                              }

                                              statementTableBody.appendTo(statementTable);
                                          },
                                          error: function(data) {
                                              generatedBalanceStatement.empty();
                                              cloudStack.dialog.notice({
                                                  message: parseXMLHttpResponse(data)
                                              });
                                          }
                                      });
                                  });

                                  domainDropdown.appendTo(statementForm);
                                  accountDropdown.appendTo(statementForm);
                                  $('<p class="quota-bold quota-element">').html('Start Date').appendTo(statementForm);
                                  startDateInput.appendTo(statementForm);
                                  $('<p class="quota-bold quota-element">').html('End Date').appendTo(statementForm);
                                  endDateInput.appendTo(statementForm);
                                  $('<br>').appendTo(statementForm);

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
                                              $('<p class="quota-bold" style="margin-bottom: 5px">').html('Account').appendTo(accountDropdown);
                                              if (json.hasOwnProperty('listaccountsresponse') && json.listaccountsresponse.hasOwnProperty('account')) {
                                                  var accounts = json.listaccountsresponse.account;
                                                  var dropdown = $('<select class="quota-input">');
                                                  for (var i = 0; i < accounts.length; i++) {
                                                      $('<option value="' + accounts[i].id + '">' + accounts[i].name + '</option>').appendTo(dropdown);
                                                  }
                                                  dropdown.appendTo(accountDropdown);
                                              } else {
                                                  $('<span class="quota-text">No accounts found in the selected domain</span>').appendTo(accountDropdown);
                                              }
                                          },
                                          error: function(data) {
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
                                              $('<p class="quota-bold" style="margin-bottom: 5px">').html('Domain').appendTo(domainDropdown);
                                              var dropdown = $('<select class="quota-input">');
                                              if (domains.length > 1) {
                                                  $('<option value="">--- Select Domain ---</option>').appendTo(dropdown);
                                              }
                                              for (var i = 0; i < domains.length; i++) {
                                                  $('<option value="' + domains[i].id + '">' + domains[i].name + '</option>').appendTo(dropdown);
                                              }
                                              dropdown.appendTo(domainDropdown);

                                              dropdown.change(function() {
                                                  var selectedDomainId = $(this).find(':selected').val();
                                                  if (!selectedDomainId) {
                                                      accountDropdown.empty();
                                                      return;
                                                  }
                                                  accountLister(selectedDomainId);
                                              });
                                              dropdown.change();
                                          },
                                          error: function(data) {
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
                                  generatedBalanceStatement.appendTo(statementView);
                                  statementView.appendTo($node);
                              }
                            },
                            {
                             'id': 'quota-tariff',
                             'name' : 'Tariff Plan',
                             'render': function($node) {
                                  var tariffView = $('<div class="details quota-text" style="margin-top: -30px">');
                                  var tariffViewList = $('<div class="view list-view">');
                                  tariffViewList.appendTo(tariffView);

                                  var renderDateForm = function(lastDate) {
                                      var startDateInput = $('<input type="text" class="quota-input" id="quota-tariff-startdate">');
                                      if (lastDate) {
                                          startDateInput.val(lastDate);
                                      }

                                      startDateInput.datepicker({
                                          defaultDate: new Date(),
                                          changeMonth: true,
                                          dateFormat: "yy-mm-dd",
                                          onClose: function (selectedDate) {
                                              if (!selectedDate) {
                                                  return;
                                              }
                                              tariffViewList.empty();
                                              renderDateForm(selectedDate);
                                              renderTariffTable(selectedDate);
                                          }
                                      });
                                      startDateInput.appendTo($('<br><span class="quota-element quota-bold">').html('Effective Date ').appendTo(tariffViewList));
                                  };

                                  var renderTariffTable = function(startDate) {
                                      var tariffTable = $('<table style="margin-top: 15px">');
                                      tariffTable.appendTo(tariffViewList);

                                      var tariffTableHead = $('<tr>');
                                      $('<th>').html(_l('label.usage.type')).appendTo(tariffTableHead);
                                      $('<th>').html(_l('label.usage.unit')).appendTo(tariffTableHead);
                                      $('<th>').html(_l('label.quota.value')).appendTo(tariffTableHead);
                                      $('<th>').html(_l('label.quota.description')).appendTo(tariffTableHead);
                                      tariffTableHead.appendTo($('<thead>').appendTo(tariffTable));

                                      $.ajax({
                                          url: createURL('quotaTariffList'),
                                          data: {startdate: startDate },
                                          success: function(json) {
                                              var items = json.quotatarifflistresponse.quotatariff;
                                              if (items.constructor === Array && items[0].currency) {
                                                  g_quotaCurrency = items[0].currency.trim() + ' ';
                                              }

                                              var tariffTableBody = $('<tbody>');

                                              for (var i = 0; i < items.length; i++) {
                                                  var tariffTableBodyRow = $('<tr>');
                                                  if (i % 2 == 0) {
                                                      tariffTableBodyRow.addClass('even');
                                                  } else {
                                                      tariffTableBodyRow.addClass('odd');
                                                  }
                                                  $('<td>').html(items[i].usageName).appendTo(tariffTableBodyRow);
                                                  $('<td>').html(items[i].usageUnit).appendTo(tariffTableBodyRow);

                                                  if (isAdmin()) {
                                                      var valueCell = $('<td class="value actions">');
                                                      var value = $('<span class="quota-element">').html(g_quotaCurrency + items[i].tariffValue);
                                                      value.appendTo(valueCell);
                                                      valueCell.appendTo(tariffTableBodyRow);

                                                      var usageType = items[i].usageType;
                                                      var editButton = $('<div class="action edit quota-tariff-edit" alt="Change value" title="Change value"><span class="icon">&nbsp;</span></div>');
                                                      editButton.appendTo(valueCell);
                                                      editButton.attr('id', 'quota-tariff-edit-' + items[i].usageType);
                                                      editButton.click(function() {
                                                          var usageTypeId = $(this).context.id.replace('quota-tariff-edit-', '');
                                                          var updateTariffForm = cloudStack.dialog.createForm({
                                                              form: {
                                                                  title: 'label.quota.configuration',
                                                                  fields: {
                                                                      quotaValue: {
                                                                          label: 'label.quota.value',
                                                                          validation: {
                                                                              required: true
                                                                          }
                                                                      },
                                                                      effectiveDate: {
                                                                          label: 'Effective Date',
                                                                          validation: {
                                                                              required: true
                                                                          }
                                                                      },
                                                                  }
                                                              },
                                                              after: function(args) {
                                                                  $.ajax({
                                                                      url: createURL('quotaTariffUpdate'),
                                                                      data: {
                                                                          usagetype: usageTypeId,
                                                                          value: args.data.quotaValue,
                                                                          startDate: args.data.effectiveDate
                                                                      },
                                                                      type: "POST",
                                                                      success: function(json) {
                                                                          $('#quota-tariff').click();
                                                                      },
                                                                      error: function(data) {
                                                                          cloudStack.dialog.notice({
                                                                              message: parseXMLHttpResponse(data)
                                                                          });
                                                                      }
                                                                  });
                                                              }
                                                          });
                                                          updateTariffForm.find('input[name=effectiveDate]').datepicker({
                                                              defaultDate: new Date(),
                                                              changeMonth: true,
                                                              dateFormat: "yy-mm-dd",
                                                          });
                                                      });
                                                  } else {
                                                      $('<td>').html(g_quotaCurrency + items[i].tariffValue).appendTo(tariffTableBodyRow);
                                                  }
                                                  $('<td>').html(items[i].description).appendTo(tariffTableBodyRow);
                                                  tariffTableBodyRow.appendTo(tariffTableBody);
                                              }
                                              tariffTableBody.appendTo(tariffTable);
                                          },
                                          error: function(data) {
                                              cloudStack.dialog.notice({
                                                  message: parseXMLHttpResponse(data)
                                              });
                                          }
                                      });
                                  };

                                  renderDateForm();
                                  renderTariffTable();
                                  tariffView.appendTo($node);
                             }
                            },
                            {'id': 'quota-credit',
                             'name': 'Manage Credits',
                             'render': function($node) {
                                  var manageCreditView = $('<div class="details quota-text" style="padding: 10px">');
                                  var creditStatement = $('<div class="quota-credit-statement">');

                                  var creditForm = $('<div class="quota-credit">');
                                  var domainDropdown = $('<div class="quota-domain-dropdown quota-element">');
                                  var accountDropdown = $('<div class="quota-account-dropdown quota-element">');
                                  var quotaValueInput = $('<input class="quota-input" type="text" style="margin-left: 12px">');
                                  var quotaThresholdInput = $('<input class="quota-input" type="text" style="margin-left: 12px">');
                                  var quotaThresholdLockCheckbox = $('<input class="quota-input quota-element" type="checkbox" style="margin-left: 12px" checked>');
                                  var addCreditButton = $('<button class="quota-element quota-button" id="quota-add-credit-button">').html("Add Credit");

                                  addCreditButton.click(function() {
                                      if (isAdmin() || isDomainAdmin()) {
                                          domainId = domainDropdown.find("select :selected").val();
                                      } else {
                                          return;
                                      }
                                      var account = accountDropdown.find("select :selected").val();
                                      var quotaValue = quotaValueInput.val();
                                      var quotaThreshold = quotaThresholdInput.val();
                                      var isLockedOptionSelected = quotaThresholdLockCheckbox.is(':checked');

                                      if (!quotaValue || !account) {
                                          creditStatement.empty();
                                          $('<br>').appendTo(creditStatement);
                                          $('<p class="quota-element">').html('Please select domain/account and enter valid credit/debit value').appendTo(creditStatement);
                                          return;
                                      }

                                      $.ajax({
                                          url: createURL('quotaCredits'),
                                          data: {
                                              account: account,
                                              domainid: domainId,
                                              value: quotaValue,
                                              quota_enforce: isLockedOptionSelected,
                                              min_balance: quotaThreshold
                                          },
                                          success: function(json) {
                                              quotaValueInput.val('');
                                              creditStatement.empty();
                                              if (json.quotacreditsresponse.quotacredits.currency) {
                                                  g_quotaCurrency = json.quotacreditsresponse.quotacredits.currency.trim() + ' ';
                                              }
                                              $('<hr>').appendTo(creditStatement);
                                              $('<p class="quota-bold">').html('Credit amount ' + g_quotaCurrency + json.quotacreditsresponse.quotacredits.credits + ' added to the account ' + account).appendTo(creditStatement);
                                              $.ajax({
                                                  url: createURL('quotaBalance'),
                                                  data: {
                                                      account: account,
                                                      domainid: domainId,
                                                  },
                                                  success: function(json) {
                                                      if (json.hasOwnProperty('quotabalanceresponse') && json.quotabalanceresponse.hasOwnProperty('balance')) {
                                                          $('<p class="quota-bold">').html('Current Quota Balance of "' + account + '": ' + g_quotaCurrency + json.quotabalanceresponse.balance.startquota).appendTo(creditStatement);
                                                      }
                                                  },
                                                  error: function(json) {
                                                  }
                                              });
                                          },
                                          error: function(json) {
                                              cloudStack.dialog.notice({
                                                  message: parseXMLHttpResponse(json)
                                              });
                                          }
                                      });
                                  });

                                  domainDropdown.appendTo(creditForm);
                                  accountDropdown.appendTo(creditForm);
                                  $('<p class="quota-bold quota-element">').html('Credit Value').appendTo(creditForm);
                                  quotaValueInput.appendTo(creditForm);
                                  $('<p class="quota-bold quota-element">').html('Account Minimum Threshold').appendTo(creditForm);
                                  quotaThresholdInput.appendTo(creditForm);
                                  $('<br>').appendTo(creditForm);
                                  quotaThresholdLockCheckbox.appendTo(creditForm);
                                  $('<span class="quota-bold">').html('Lock account when quota balance is less than minimum threshold').appendTo(creditForm);
                                  $('<br>').appendTo(creditForm);
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
                                              $('<p class="quota-bold">').html('Account').appendTo(accountDropdown);
                                              if (json.hasOwnProperty('listaccountsresponse') && json.listaccountsresponse.hasOwnProperty('account')) {
                                                  var accounts = json.listaccountsresponse.account;
                                                  var dropdown = $('<select class="quota-input">');
                                                  for (var i = 0; i < accounts.length; i++) {
                                                      $('<option value="' + accounts[i].name + '">' + accounts[i].name + '</option>').appendTo(dropdown);
                                                  }
                                                  dropdown.appendTo(accountDropdown);
                                              } else {
                                                  $('<span>No accounts found in the selected domain</span>').appendTo(accountDropdown);
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
                                              $('<p class="quota-bold">').html('Domain').appendTo(domainDropdown);
                                              var domains = json.listdomainsresponse.domain;
                                              var dropdown = $('<select class="quota-input">');
                                              if (domains.length > 1) {
                                                  $('<option value="">--- Select Domain ---</option>').appendTo(dropdown);
                                              }
                                              for (var i = 0; i < domains.length; i++) {
                                                  $('<option value="' + domains[i].id + '">' + domains[i].name + '</option>').appendTo(dropdown);
                                              }
                                              dropdown.appendTo(domainDropdown);

                                              dropdown.change(function() {
                                                  var selectedDomainId = $(this).find(':selected').val();
                                                  if (!selectedDomainId) {
                                                      accountDropdown.empty();
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
                                  var manageTemplatesView = $('<div class="details quota-text" style="padding: 10px">');

                                  var emailTemplateForm = $('<div class="quota-email-form">');
                                  var templateDropdown = $('<div class="quota-template-dropdown">');
                                  var templateOptions = $('<select class="quota-input" style="margin: 0px 0px 5px 11px"><option value="QUOTA_LOW">Template for accounts with low quota balance</option><option value="QUOTA_EMPTY">Template for accounts with no quota balance that will be locked</option><option value="QUOTA_UNLOCK_ACCOUNT">Template for accounts with enough credits getting unlocked</option><option value="QUOTA_STATEMENT">Template for quota statement</option><</select>');
                                  $('<p class="quota-bold quota-element">').html('Email Template').appendTo(templateDropdown);
                                  templateOptions.appendTo(templateDropdown);
                                  //templateOptions.appendTo($('<p class="quota-bold quota-element">Email Template</p>').appendTo(templateDropdown));

                                  var templateSubjectTextArea = $('<textarea id="quota-template-subjectarea" class="quota-input" style="margin: 0px 0px 10px 12px; font-size: 12px">');
                                  var templateBodyTextArea = $('<textarea id="quota-template-bodyarea" class="quota-input" style="height: 250px; margin: 0px 0px 10px 12px; font-size: 12px"></textarea>');
                                  var saveTemplateButton = $('<button class="quota-button quota-element" id="quota-save-template-button">').html("Save Template");

                                  templateOptions.change(function() {
                                      var templateName = $(this).find(':selected').val();
                                      templateSubjectTextArea.val('');
                                      templateBodyTextArea.val('');
                                      $.ajax({
                                          url: createURL('quotaEmailTemplateList'),
                                          data: {
                                              templatetype: templateName
                                          },
                                          success: function(json) {
                                              if (!json.hasOwnProperty('quotaemailtemplatelistresponse') || !json.quotaemailtemplatelistresponse.hasOwnProperty('quotaemailtemplate')) {
                                                  return;
                                              }
                                              var template = json.quotaemailtemplatelistresponse.quotaemailtemplate[0];
                                              templateSubjectTextArea.val(template.templatesubject.replace(/\\n/g, '\n').replace(/\\"/g, '"').replace(/<br>/g, '\n'));
                                              templateBodyTextArea.val(template.templatebody.replace(/\\n/g, '\n').replace(/\\"/g, '"').replace(/<br>/g, '\n'));
                                          },
                                          error: function(data) {
                                          }
                                      });
                                  });
                                  templateOptions.change();

                                  saveTemplateButton.click(function() {
                                      var templateName = templateOptions.find(':selected').val();
                                      var templateSubject = templateSubjectTextArea.val().replace(/\n/g, '<br>');
                                      var templateBody = templateBodyTextArea.val().replace(/\n/g, '<br>');

                                      $.ajax({
                                          url: createURL('quotaEmailTemplateUpdate'),
                                          type: "POST",
                                          data: {
                                              templatetype: templateName,
                                              templatesubject: templateSubject,
                                              templatebody: unescape(templateBody),
                                          },
                                          success: function(json) {
                                              templateOptions.change();
                                          },
                                          error: function(data) {
                                              //handle error here
                                          }
                                      });
                                  });

                                  templateDropdown.appendTo(emailTemplateForm);
                                  $('<p class="quota-bold quota-element">').html('Email Template Subject').appendTo(emailTemplateForm);
                                  templateSubjectTextArea.appendTo(emailTemplateForm);
                                  $('<p class="quota-bold quota-element">').html('Email Template Body').appendTo(emailTemplateForm);
                                  templateBodyTextArea.appendTo(emailTemplateForm);
                                  saveTemplateButton.appendTo(emailTemplateForm);
                                  $('<hr>').appendTo(emailTemplateForm);
                                  $('<p>').html("These options can be used in template as ${variable}: quotaBalance, quotaUsage, accountName, accountID, accountUsers, domainName, domainID").appendTo(emailTemplateForm);

                                  emailTemplateForm.appendTo(manageTemplatesView);
                                  manageTemplatesView.appendTo($node);
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
