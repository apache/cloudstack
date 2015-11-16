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
	var now = new Date();
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
              summary: {
                  type: 'select',
                  title: 'label.quota.summary',
                  listView: {
                      label: 'label.quota.summary',
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

                          // FIXME: add logic in mgmt server to filter by
							// account
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
                                  
                              }
                          });
                      },
                      detailView: {
                          viewAll: [{
                              path: 'quota.quotastatement',
                              label: 'label.quota.statement.quota'
                          },{
                              path: 'quota.balancestatement',
                              label: 'label.quota.statement.balance'
                          }],
                          actions: {
                              addCredits: {
                                  label: 'label.quota.add.credits',
                                  messages: {
                                      notification: function(args) {
                                          return 'label.quota.add.credits';
                                      }
                                  },
                                  // http://192.168.217.11:8096/?command=quotaCredits&account=batman&domainid=4&value=10&min_balance=30&quota_enforce=true
                                  action: function(args) {
                                      var data = {
                                                  account: args.context.statement[0].account,
                                                  domainid: args.context.statement[0].domainid,
                                                  value: 0,
                                                  min_balance: 0,
                                                  quota_enforce: false
                                              };

                                        var creditForm = cloudStack.dialog.createForm({
                                          form: {
                                              title: 'label.quota.add.credits',
                                              fields: {
                                                  value: {
                                                      label: 'label.quota.credits',
                                                      validation: {
                                                          required: true
                                                      }
                                                  },
                                                  min_balance: {
                                                      label: 'label.quota.min_balance',
                                                      validation: {
                                                          required: true
                                                      }
                                                  },
                                                  quota_enforce: {
                                                      label: 'label.quota.quota_enforce',
                                                      isBoolean: true,
                                                      isChecked: false
                                                  },
                                              }
                                          },
                                          after: function(argsLocal) {
                                              data.value = argsLocal.data.value;
                                              data.min_balance = argsLocal.data.min_balance;
                                              data.quota_enforce = argsLocal.data.quota_enforce;
                                              $.ajax({
                                                  url: createURL('quotaCredits'),
                                                  data: data,
                                                  type: "POST",
                                                  success: function(json) {
                                                      args.response.success({
                                                          data: json.quotacreditsresponse.totalquota
                                                      });
                                                  },
                                                  error: function(data) {
                                                      cloudStack.dialog.notice({
                                                          message: parseXMLHttpResponse(data)
                                                      });
                                                  }
                                              });
                                           }
                                       });
                                  } // function
                              }, // add credits
                          },
                          tabs: {
                             details: {
                                    title: 'label.details',
                                    fields: [{
                                        id: {
                                            label: 'label.quota.statement.balance'
                                        }
                                    }, {
                                        startdate: {
                                            label: 'label.quota.date',
                                        },
                                        startquota: {
                                            label: 'label.quota.value',
                                        }
                                    }],
                                    dataProvider: function(args) {
                                        console.log(args);
                                        $.ajax({
                                            url: createURL('quotaBalance'),
                                            data: {
                                                domainid: args.context.summary[0].domainid,
                                                account: args.context.summary[0].account
                                            },
                                            success: function(json) {
                                            	console.log(json);
                                            	/*
                                            	 * <quotabalanceresponse cloud-stack-version="4.5.2">
                                                    <balance>
                                                        <startquota>10.00</startquota>
                                                        <endquota>-215.07</endquota>
                                                        <credits>
                                                        <credits>10.00</credits>
                                                        <updated_on>2015-10-13T20:38:14+0530</updated_on>
                                                        </credits>
                                                        <startdate>2015-10-01T00:00:00+0530</startdate>
                                                        <enddate>2015-10-23T00:00:00+0530</enddate>
                                                        <currency>R$</currency>
                                                        </balance>
                                                    </quotabalanceresponse>
                                            	 */
                                                var item = json.quotabalanceresponse.balance;
                                                item.startdate = now.toJSON().slice(0,10);
                                                args.response.success({
                                                    data: item
                                                });
                                            }
                                        });
                                    }
                                },
                                
                          }// end tab
                      }

                  }
              },
            
           


          quotastatement:{
                  id: 'statementbalance',
                  title: 'label.quota.statement',
                  preFilter: function(args) {
                      return false; 
                  },
                  listView: {
                      label: 'label.quota.statement.quota',
                      disableInfiniteScrolling: true,
                      fields: {
                            name: {
                                label: 'label.quota.type.name'
                            },
                            unit: {
                                label: 'label.quota.type.unit'
                            },
                            quota: {
                                label: 'label.quota.usage'
                            }
                      },
                      actions: {
                              add: {
                                    label: 'label.quota.dates',
                                    createForm: {
                                          title: 'label.quota.dates',
                                          fields: {
                                            startDate: {
                                                label: 'label.quota.startdate',
                                                validation: {
                                                    required: true
                                                }
                                            },
                                            endDate: {
                                                label: 'label.quota.enddate',
                                                validation: {
                                                    required: true
                                                }
                                            }
                                        }
                                      },
                                    action: function(args) {
                                            var data = {
                                                 domainid: args.context.statement[0].domainid,
                                                 account: args.context.statement[0].account,
                                                 startDate: args.context.statement[0].startdate.slice(0,10),
                                                 endDate: now.toJSON().slice(0,10)
                                            };
                                            $.ajax({
                                                url: createURL('quotaStatement'),
                                                data: {
                                                    domainid: args.context.statement[0].domainid,
                                                    account: args.context.statement[0].account,
                                                    startDate: args.context.statement[0].startdate.slice(0,10),
                                                    endDate: now.toJSON().slice(0,10)
                                                },
                                                //<quotastatementresponse cloud-stack-version="4.5.2"><statement> <quotausage> <type>1</type> <accountid>77</accountid> <domain>35</domain> <name>RUNNING_VM</name>
                                                //<unit>Compute-Month</unit> <quota>450.15</quota> <startdate>2015-10-13T23:31:15+0530</startdate><enddate>2015-10-13T23:46:47+0530</enddate></quotausage>
                                                success: function(json) {
                                                    console.log('quota statement'); /* {type: 1, accountid: 77,// domain:// 35,// name:// "RUNNING_VM",// unit: "Compute-Month",*/
                                                    var usages = json.quotastatementresponse.statement.quotausage;
                                                    var usages = json.quotastatementresponse.statement.quotausage;
                                                    usages.push({
                                                            name: 'TOTAL',
                                                            unit:'',
                                                            quota: json.quotastatementresponse.statement.totalquota
                                                        });
                                                    args.response.success({
                                                        data: usages
                                                    });
                                                }
                                            });
                                        },
                                        notification: {
                                            poll: pollAsyncJobResult
                                        }
                                  }
                           },
    
                      dataProvider: function(args) {
                          console.log(args); /* // args.context = accountid: 77, account: "shapeblue-users", domainid: 35, domain: "sb-domain", balance: "R$ -323.06" */
                          $.ajax({
                              url: createURL('quotaStatement'),
                              data: {
                                  domainid: args.context.summary[0].domainid,
                                  account: args.context.summary[0].account,
                                  startDate: args.context.summary[0].startdate.slice(0,10),
                                  endDate: now.toJSON().slice(0,10)
                              },
                              //<quotastatementresponse cloud-stack-version="4.5.2"><statement> <quotausage> <type>1</type> <accountid>77</accountid> <domain>35</domain> <name>RUNNING_VM</name>
                              //<unit>Compute-Month</unit> <quota>450.15</quota> <startdate>2015-10-13T23:31:15+0530</startdate><enddate>2015-10-13T23:46:47+0530</enddate></quotausage>
                              success: function(json) {
                                  console.log(json); /*// {type:// 1,// accountid:// 77,// domain:// 35,// name:// "RUNNING_VM",// unit:// "Compute-Month", */
                                  var usages = json.quotastatementresponse.statement.quotausage;
                                  usages.push({
                                          name: 'TOTAL',
                                          unit:'',
                                          quota: json.quotastatementresponse.statement.totalquota
                                      });
                                  args.response.success({
                                      data: usages
                                  });
                              }
                          });
                      }
                  } // end list view
              }, // end statement
            
    



          balancestatement:{
                  id: 'balancestatement',
                  title: 'label.quota.statement.balance',
                  listView: {
                      label: 'label.quota.statement.balance',
                      disableInfiniteScrolling: true,
                      /*<balance><startquota>10.00</startquota><endquota>-215.07</endquota><credits><credits>10.00</credits><updated_on>2015-10-13T20:38:14+0530</updated_on></credits>
                      * <startdate>2015-10-01T00:00:00+0530</startdate><enddate>2015-10-23T00:00:00+0530</enddate><currency>R$</currency></balance>*/
                      fields: {
                            date: {
                                label: 'label.quota.date'
                            },
                            quota: {
                                label: 'label.quota.value'
                            },
                            credit: {
                                label: 'label.quota.credit'
                            }
                      },
                        
                      actions: {
                              add: {
                                    label: 'label.quota.dates',
                                    createForm: {
                                          title: 'label.quota.dates',
                                          fields: {
                                            startDate: {
                                                label: 'label.quota.startdate',
                                                validation: {
                                                    required: true
                                                }
                                            },
                                            endDate: {
                                                label: 'label.quota.enddate',
                                                validation: {
                                                    required: true
                                                }
                                            }
                                        }
                                      },
                                    action: function(args) {
                                            var data = {
                                                 domainid: args.context.statement[0].domainid,
                                                 account: args.context.statement[0].account,
                                                 startDate: args.context.statement[0].startdate.slice(0,10),
                                                 endDate: now.toJSON().slice(0,10)
                                            };
                                            $.ajax({
                                                url: createURL('quotaBalance'),
                                                data: {
                                                    domainid: args.context.summary[0].domainid,
                                                    account: args.context.summary[0].account,
                                                    startDate: args.context.summary[0].startdate.slice(0,10),
                                                    endDate: now.toJSON().slice(0,10)
                                                },
                                                success: function(json) {
                                                    console.log('quota balance');
                                                    console.log(json);/*balance: startquota: 0, endquota: 50, credits: [{credits: 50, updated_on: "2015-11-12T16:18:11+0530"}], startdate: "2015-11-01T00:00:00+0530", enddate: "2015-11-17T00:00:00+0530",*/
                                                    var bal = json.quotabalanceresponse.balance;
                                                    var array=[{
                                                                 date: bal.startdate.slice(0,10),
                                                                 quota: bal.startquota,
                                                                 credit: ''
                                                    }];
                                                    //now add all credits
                                                    for (var i = 0; i < bal.credits.length; i++) {
                                                          array.push({
                                                              date: bal.credits[i].updated_on.slice(0,10),
                                                              quota: '',
                                                              credit: bal.credits[i].credits
                                                          });
                                                      }
                                                    array.push({
                                                              date: bal.enddate.slice(0,10),
                                                              quota: bal.endquota,
                                                              credit: ''
                                                          });
                                                    args.response.success({
                                                        data: array
                                                    });
                                                }
                                            });
                                        },
                                        notification: {
                                            poll: pollAsyncJobResult
                                        }
                                  }
                           },
    
                      dataProvider: function(args) {
                          console.log(args);
                          $.ajax({
                              url: createURL('quotaBalance'),
                              data: {
                                  domainid: args.context.summary[0].domainid,
                                  account: args.context.summary[0].account,
                                  startDate: args.context.summary[0].startdate.slice(0,10),
                                  endDate: now.toJSON().slice(0,10)
                              },
                              success: function(json) {
                                  console.log('quota balance');
                                  console.log(json);/*balance: startquota: 0, endquota: 50, credits: [{credits: 50, updated_on: "2015-11-12T16:18:11+0530"}], startdate: "2015-11-01T00:00:00+0530", enddate: "2015-11-17T00:00:00+0530",*/
                                  var bal = json.quotabalanceresponse.balance;
                                  var array=[{
                                               date: bal.startdate.slice(0,10),
                                               quota: bal.startquota,
                                               credit: ''
                                  }];
                                  //now add all credits
                                  for (var i = 0; i < bal.credits.length; i++) {
                                        array.push({
                                            date: bal.credits[i].updated_on.slice(0,10),
                                            quota: '',
                                            credit: bal.credits[i].credits
                                        });
                                    }
                                  array.push({
                                            date: bal.enddate.slice(0,10),
                                            quota: bal.endquota,
                                            credit: ''
                                        });
                                  args.response.success({
                                      data: array
                                  });
                              }
                          });
                      }
                  } // end list view
              }, // end statement
            


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
                                                  // Refresh listings on
													// chosen date to reflect
													// new tariff
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
