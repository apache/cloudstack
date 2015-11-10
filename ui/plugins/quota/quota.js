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
        plugin.ui.addSection({
          id: 'quota',
          title: 'Quota',
          showOnNavigation: true,
          preFilter: function(args) {
              return true;
          },
          sectionSelect: {
              label: 'label.select-view'
          },
          sections: {
              statement: {
                  type: 'select',
                  title: 'label.quota.statement',
                  listView: {
                      label: 'label.quota.statement',
                      fields: {
                          account: {
                              label: 'label.account',
                              truncate: true
                          },
                          domain: {
                              label: 'label.domain'
                          },
                          balance: {
                              label: 'label.quota.balance',
                          },
                          quota: {
                              label: 'label.quota.totalusage',
                              editable: true,
                              truncate: true
                          },
                          state: {
                              label: 'label.quota.state',
                              indicator: {
                                  'enabled': 'on',
                                  'disabled': 'off',
                                  'locked': 'off',
                              }
                          },
                      },
                      dataProvider: function(args) {
                          var data = {
                              page: args.page,
                              pagesize: pageSize
                          };

                          // FIXME: add logic in mgmt server to filter by account
                          if (args.filterBy.search.value) {
                              data.account = args.filterBy.search.value;
                          }

                          $.ajax({
                              url: createURL('quotaSummary'),
                              data: data,
                              dataType: "json",
                              async: true,
                              success: function(json) {
                                  var items = json.quotasummaryresponse.summary;
                                  $.each(items, function(idx, item) {
                                      items[idx].quota = items[idx].currency + ' ' + items[idx].quota;
                                      items[idx].balance = items[idx].currency + ' ' + items[idx].balance;
                                  });
                                  args.response.success({
                                      data: items
                                  });
                              },
                              error: function(data) {
                                  cloudStack.dialog.notice({
                                      message: parseXMLHttpResponse(data)
                                  });
                              }
                          });
                      },
                      detailView: {
                          actions: {
                              addCredits: {
                                  label: 'label.quota.add.credits',
                                  messages: {
                                      notification: function(args) {
                                          return 'label.quota.add.credits';
                                      }
                                  },
                                  action: function(args) {
                                      // FIX add code to show add credits form and fix css to show an icon
                                  }
                              },

                              quotaStatementByDates: {
                                  label: 'label.quota.statement.bydates',
                                  messages: {
                                      notification: function(args) {
                                          return 'label.quota.statement.bydates';
                                      }
                                  },
                                  action: function(args) {
                                      // FIX add code to show form start/end date with datepicker injected, fix css
                                  }
                              }

                          },

                          tabs: {
                              details: {
                                  title: 'label.details',
                                  fields: [{
                                      name: {
                                          label: 'label.quota.statement.balance'
                                      }
                                  }, {
                                      balance: {
                                          label: 'label.quota.statement.balance',
                                      },
                                      startdate: {
                                          label: 'label.quota.statement.date',
                                      },
                                  }],

                                  dataProvider: function(args) {
                                      console.log('staetment view');
                                      console.log(args);
                                      $.ajax({
                                          url: createURL('quotaBalance'),
                                          data: {
                                              domainid: args.context.statement[0].domainid,
                                              account: args.context.statement[0].account
                                          },
                                          success: function(json) {
                                              var item = json.quotabalanceresponse.balance;
                                              item.name = args.context.statement[0].account;
                                              item.balance = item.currency + ' ' + item.startquota;
                                              args.response.success({
                                                  data: item
                                              });
                                          }
                                      });
                                  }
                              }
                          }
                      }

                  }
              },

              tariff: {
                  type: 'select',
                  title: 'label.quota.tariff',
                  listView: {
                      label: 'label.quota.tariff',
                      disableInfiniteScrolling: true,
                      actions: {
                          edit: {
                              label: 'label.change.value',
                              action: function(args) {
                                  var data = {
                                      usagetype: args.data.jsonObj.usageType,
                                      value: args.data.tariffValue.split(' ')[1]
                                  };
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
                                      after: function(argsLocal) {
                                          data.startDate = argsLocal.data.effectiveDate;
                                          $.ajax({
                                              url: createURL('quotaTariffUpdate'),
                                              data: data,
                                              type: "POST",
                                              success: function(json) {
                                                  args.response.success({
                                                      data: json.quotatariffupdateresponse.quotatariff
                                                  });
                                                  // Refresh listings on chosen date to reflect new tariff
                                                  $($.find('div.search-bar input')).val(data.startDate);
                                                  $('#basic_search').click();
                                              },
                                              error: function(data) {
                                                  cloudStack.dialog.notice({
                                                      message: parseXMLHttpResponse(data)
                                                  });
                                              }
                                          });
                                      }
                                  });
                                  updateTariffForm.find('input[name=quotaValue]').val(data.value);
                                  updateTariffForm.find('input[name=effectiveDate]').datepicker({
                                      defaultDate: new Date(),
                                      changeMonth: true,
                                      dateFormat: "yy-mm-dd",
                                  }).focus();
                              }
                          }
                      },
                      fields: {
                          usageName: {
                              label: 'label.usage.type',
                              id: true,
                              truncate: true
                          },
                          usageUnit: {
                              label: 'label.usage.unit'
                          },
                          tariffValue: {
                              label: 'label.quota.tariffvalue',
                              editable: true
                          },
                          description: {
                              label: 'label.quota.description',
                              truncate: true
                          }
                      },
                      dataProvider: function(args) {
                          var data = {};
                          if (args.filterBy.search.value) {
                              data.startdate = args.filterBy.search.value;
                          }
                          $.ajax({
                              url: createURL('quotaTariffList'),
                              data: data,
                              dataType: "json",
                              async: true,
                              success: function(json) {
                                  var items = json.quotatarifflistresponse.quotatariff;
                                  $.each(items, function(idx, item) {
                                      items[idx].tariffValue =  items[idx].currency + ' ' + items[idx].tariffValue;
                                  });
                                  args.response.success({
                                      data: items
                                  });

                                  // Hook up date picker
                                  var input = $($.find('div.search-bar input'));
                                  input.datepicker({
                                      defaultDate: new Date(),
                                      changeMonth: true,
                                      dateFormat: "yy-mm-dd",
                                  });
                                  input.parent().attr('title', _l('label.quota.effectivedate'));
                              },
                              error: function(data) {
                                  cloudStack.dialog.notice({
                                      message: parseXMLHttpResponse(data)
                                  });
                              }
                          });
                      }
                  }
              },

              emailTemplates: {
                  type: 'select',
                  title: 'label.quota.email.template',
                  listView: {
                      label: 'label.quota.email.template',
                      disableInfiniteScrolling: true,
                      fields: {
                          templatetype: {
                              label: 'label.quota.email.template',
                          },
                          templatesubject: {
                              label: 'label.quota.email.subject',
                              truncate: true
                          },
                          templatebody: {
                              label: 'label.quota.email.body',
                              truncate: true
                          },
                          last_updated: {
                              label: 'label.quota.email.lastupdated',
                              truncate: true
                          },
                      },
                      dataProvider: function(args) {
                          var data = {};
                          if (args.filterBy.search.value) {
                              data.templatetype = args.filterBy.search.value;
                          }

                          $.ajax({
                              url: createURL('quotaEmailTemplateList'),
                              data: data,
                              dataType: "json",
                              async: true,
                              success: function(json) {
                                  if (!json.hasOwnProperty('quotaemailtemplatelistresponse') || !json.quotaemailtemplatelistresponse.hasOwnProperty('quotaemailtemplate')) {
                                      return;
                                  }
                                  var items = json.quotaemailtemplatelistresponse.quotaemailtemplate;
                                  args.response.success({
                                      data: items
                                  });
                              },
                              error: function(data) {
                                  cloudStack.dialog.notice({
                                      message: parseXMLHttpResponse(data)
                                  });
                              }
                          });
                      },
                      detailView: {
                          actions: {
                              edit: {
                                  label: 'label.quota.email.edit',
                                  messages: {
                                      notification: function(args) {
                                          return 'label.quota.email.edit';
                                      }
                                  },
                                  action: function(args) {
                                      console.log(args);
                                      args.data.templatetype = args.context.emailTemplates[0].templatetype;
                                      $.ajax({
                                          url: createURL('quotaEmailTemplateUpdate'),
                                          data: args.data,
                                          success: function(json) {
                                              args.response.success({
                                                  _custom: {
                                                      success: true
                                                  }
                                              });
                                          }
                                      });
                                  }
                              }
                          },

                          tabs: {
                              details: {
                                  title: 'label.details',
                                  fields: [{
                                      templatetype: {
                                          label: 'label.quota.email.template'
                                      }
                                  }, {
                                      templatesubject: {
                                          label: 'label.quota.email.subject',
                                          isEditable: true,
                                          textArea: true
                                      },
                                      templatebody: {
                                          label: 'label.quota.email.body',
                                          isEditable: true,
                                          textArea: true
                                      },
                                      last_updated: {
                                          label: 'label.quota.email.lastupdated',
                                      },
                                  }],

                                  dataProvider: function(args) {
                                      $.ajax({
                                          url: createURL('quotaEmailTemplateList'),
                                          data: {
                                              templatetype: args.context.emailTemplates[0].templatetype
                                          },
                                          success: function(json) {
                                              var item = json.quotaemailtemplatelistresponse.quotaemailtemplate[0];
                                              args.response.success({
                                                  data: item
                                              });
                                          }
                                      });
                                  }
                              }
                          }
                      }
                  }
              }

          }
      });
  };
}(cloudStack));
