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
                                  
                                
                                      $.ajax({
                                          url: createURL('quotaSummary'),
                                          success: function(json) {
                                              var quotaSummary = json.quotasummaryresponse.summary;
                                              
                                              generatedStatement.empty();
                                              $("<hr>").appendTo(generatedStatement);

                                              if (quotaSummary.length < 1) {
                                                  return;
                                              }
                                              
                                              var statementTable = $('<table>');
                                              statementTable.appendTo($('<div class="data-table">').appendTo(generatedStatement));

                                              var statementTableHead = $('<tr>');
                                              $('<th>').html(_l('label.name')).appendTo(statementTableHead);
                                              $('<th>').html(_l('label.domain')).appendTo(statementTableHead);
                                              $('<th>').html(_l('label.balance')).appendTo(statementTableHead);
                                              $('<th>').html(_l('label.totalusage')).appendTo(statementTableHead);
                                              $('<th>').html(_l('label.state')).appendTo(statementTableHead);
                                              $('<th>').html(_l('label.quickview')).appendTo(statementTableHead);
                                              statementTableHead.appendTo($('<thead>').appendTo(statementTable));

                                              var statementTableBody = $('<tbody>');
                                              for (var i = 0; i < quotaSummary.length; i++) {
                                                  var statementTableBodyRow = $('<tr>');
                                                  if (i % 2 == 0) {
                                                      statementTableBodyRow.addClass('even');
                                                  } else {
                                                      statementTableBodyRow.addClass('odd');
                                                  }
                                                  var g_quotaCurrency = quotaSummary[i].currency.trim() + ' ';
                                                  $('<td>').html(quotaSummary[i].account).appendTo(statementTableBodyRow);
                                                  $('<td>').html(quotaSummary[i].domain).appendTo(statementTableBodyRow);
                                                  $('<td>').html(g_quotaCurrency + quotaSummary[i].quota).appendTo(statementTableBodyRow);
                                                  $('<td>').html(g_quotaCurrency + quotaSummary[i].balance).appendTo(statementTableBodyRow);
                                                  $('<td>').html(quotaSummary[i].state).appendTo(statementTableBodyRow);
                                                  $('<td>').html(_l('label.quickview')).addClass('quick-view reduced-hide').appendTo(statementTableBodyRow);
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


                                  generatedStatement.appendTo(statementView);
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
                            
                            {'id': 'quota-email',
                             'name': 'Email Templates',
                             'render': function($node) {
                                  var manageTemplatesView = $('<div class="details quota-text" style="padding: 10px">');

                                  var emailTemplateForm = $('<div class="quota-email-form">');
                                  var templateDropdown = $('<div class="quota-template-dropdown">');
                                  var templateOptions = $('<select class="quota-input" style="margin: 0px 0px 5px 11px"><option value="QUOTA_LOW">Template for accounts with low quota balance</option><option value="QUOTA_EMPTY">Template for accounts with no quota balance that will be locked</option><option value="QUOTA_UNLOCK_ACCOUNT">Template for accounts with enough credits getting unlocked</option><option value="QUOTA_STATEMENT">Template for quota statement</option><</select>');
                                  $('<p class="quota-bold quota-element">').html('Email Template').appendTo(templateDropdown);
                                  templateOptions.appendTo(templateDropdown);
                                  // templateOptions.appendTo($('<p
									// class="quota-bold quota-element">Email
									// Template</p>').appendTo(templateDropdown));

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
                                              // handle error here
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
