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
	var newstartdate;
	var newenddate;
    cloudStack.plugins.quota = function(plugin) {
        plugin.ui.addSection({
          id: 'quota',
          title: 'Quota',
          preFilter: function(args) {
              return true; 
          },
          showOnNavigation: true,
          sectionSelect: {
              label: 'label.select-view',
              preFilter: function(args) {
            	  console.log(args);
                  return ['summary', 'tariff', 'emailTemplates']; 
              }
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
                                            dataType: 'json',
                                            async: true,
                                            data: {
                                                domainid: args.context.summary[0].domainid,
                                                account: args.context.summary[0].account
                                            },
                                            success: function(json) {
                                                var item = json.quotabalanceresponse.balance;
                                                item.startdate = now.toJSON().slice(0,10);
                                                args.response.success({
                                                    data: item
                                                });
                                            } ,
                                            error:function(data) {
                                                args.response.error(parseXMLHttpResponse(data));
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
                                            startdate: {
                                                label: 'label.quota.startdate',
                                                isDatepicker: true,
                                                validation: {
                                                    required: true
                                                }
                                            },
                                            enddate: {
                                                label: 'label.quota.enddate',
                                                isDatepicker: true,
                                                validation: {
                                                    required: true
                                                }
                                            }
                                        }
                                      },
                                     action: function(args) {
                                	    console.log(args); 
                                	    newstartdate= args.data.startdate.slice(0,10);
                                	    newenddate= args.data.enddate.slice(0,10);
                                	    $(window).trigger('cloudStack.fullRefresh');
                                    }//end action
                            }
                       },
                      dataProvider: function(args) {
                          $.ajax({
                              url: createURL('quotaStatement'),
                              dataType: 'json',
                              async: true,
                              data: {
                                  domainid: args.context.summary[0].domainid,
                                  account: args.context.summary[0].account,
                                  startdate: function() {
             	                                 if (typeof newstartdate == 'undefined') { 
             	                                     return  args.context.summary[0].startdate.slice(0,10);
             	                                 } else {
             	                                     return newstartdate; 
             	                                 }
           	                        },
                                  enddate: function() {
                        	                     if (typeof newenddate == 'undefined') { 
                        	                          return  now.toJSON().slice(0,10);
                                    	          } else {
                                                      return newenddate; 
                                    	          }
                            	   }
                              },
                              success: function(json) {
                                  console.log(json);
                                  var usages = json.quotastatementresponse.statement.quotausage;
                                  usages.push({
                                        name: 'TOTAL',
                                        unit:'',
                                        quota: json.quotastatementresponse.statement.totalquota
                                    });
                                  
                                  usages.unshift({
                                      name: 'Start Date: ' + json.quotastatementresponse.statement.startdate.slice(0,10),
                                      unit: 'End Date: ' + json.quotastatementresponse.statement.enddate.slice(0,10),
                                      quota: ''
                                  });                                  


                                  args.response.success({
                                      data: usages
                                  });
                              },
                              error:function(data) {
                                  args.response.error(parseXMLHttpResponse(data));
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
                                            startdate: {
                                                label: 'label.quota.startdate',
                                                isDatepicker: true,
                                                validation: {
                                                    required: true
                                                }
                                            },
                                            enddate: {
                                                label: 'label.quota.enddate',
                                                isDatepicker: true,
                                                validation: {
                                                    required: true
                                                }
                                            }
                                        }
                                      },
                                    action: function(args) {
                                    	    console.log(args);
                                            newstartdate= args.data.startdate.slice(0,10);
                                            newenddate= args.data.enddate.slice(0,10);
                                            $(window).trigger('cloudStack.fullRefresh');
                                        }
                                  }
                           },
    
                      dataProvider: function(args) {
                          console.log(args);
                          $.ajax({
                              url: createURL('quotaBalance'),
                              dataType: 'json',
                              async: true,
                              data: {
                                  domainid: args.context.summary[0].domainid,
                                  account: args.context.summary[0].account,
                                  startdate: function() {
                                       if (typeof newstartdate == 'undefined') { 
                                           return  args.context.summary[0].startdate.slice(0,10);
                                       } else {
                                           return newstartdate; 
                                       }
                                    },
                                   enddate: function() {
             	                     if (typeof newenddate == 'undefined') { 
             	                          return  now.toJSON().slice(0,10);
                         	          } else {
                                           return newenddate; 
                         	          }
                                  }
                              },
                              success: function(json) {
                                  console.log('quota balance');
                                  console.log(json);
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
                              },
                              error:function(data) {
                                  args.response.error(parseXMLHttpResponse(data));
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
