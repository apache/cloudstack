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
    var currency;
    cloudStack.plugins.quota = function(plugin) {
        plugin.ui.addSection({
          id: 'quota',
          title: 'Quota',
          preFilter: function(args) {
    	        var retval = $.ajax({
                	url: createURL("quotaIsEnabled"),
                	async: false
                });
    	        var json = JSON.parse(retval.responseText);
    	        return json.quotaisenabledresponse.isenabled.isenabled;
          },
          showOnNavigation: true,
          sectionSelect: {
              label: 'label.select-view',
              preFilter: function(args) {
              if (isAdmin())
                  return ['summary', 'fullSummary', 'tariffEdit', 'emailTemplates'];
              else
                  return  ['summary', 'tariffView'];
              }
          },
          sections: {

              summary: {
                  type: 'select',
                  title: 'label.quota.summary',
                  listView: {
                      label: 'label.quota.summary',
                      disableInfiniteScrolling: true,
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
                              limitcolor: true,
                              limits: {
                                  upperlimit: 'upperlimit',
                                  lowerlimit: 'lowerlimit'
                              },
                              converter: function(args){
                                  return currency + args.toString();
                              }
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

                          // TODO: add logic in mgmt server to filter by account
                          if (args.filterBy.search.value) {
                              data.search = args.filterBy.search.value;
                          }

                          $.ajax({
                              url: createURL('quotaSummary'),
                              data: data,
                              dataType: "json",
                              async: true,
                              success: function(json) {
                                  var items = json.quotasummaryresponse.summary;
                                  if(items){
                                      var array=[];
                                      for (var i = 0; i < items.length; i++) {
                                          if (typeof data.search != 'undefine' && items[i].account.search(data.search) < 0 && items[i].domain.search(data.search) < 0) {
                                              continue;
                                          }
                                          currency = items[i].currency;
                                          items[i].quota = currency + ' ' + items[i].quota;
                                          items[i].lowerlimit = -1;
                                          items[i].upperlimit = 0;
                                          array.push(items[i]);
                                      }
                                      args.response.success({
                                          data: array
                                      });
                                  }
                                  else {
                                      args.response.success({
                                          data: 0
                                      });
                                  }
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
                             add: {
                                label: 'label.quota.add.credits',
                                preFilter: function(args) { return isAdmin(); },
                                messages: {
                                    confirm: function(args) {
                                        return 'label.quota.add.credits';
                                    },
                                    notification: function(args) {
                                        return 'label.quota.add.credits';
                                    }
                                },

                                createForm: {
                                    title: 'label.quota.credits',
                                    desc: '',
                                    fields: {
                                         value: {
                                            label: 'label.quota.credits',
                                            validation: {
                                                required: true
                                            }
                                        },
                                        min_balance: {
                                            label: 'label.quota.minbalance',
                                            validation: {
                                                required: true
                                            }
                                        },
                                        quota_enforce: {
                                            label: 'label.quota.enforcequota',
                                            isBoolean: true,
                                            isChecked: false
                                        },
                                    }

                                },

                                action: function(args) {
                                    var enforce=args.data.quota_enforce == 'on' ? true: false;
                                    $.ajax({
                                        url: createURL('quotaCredits'),
                                        data: {
                                            domainid: args.context.summary[0].domainid,
                                            account: args.context.summary[0].account,
                                            value: args.data.value,
                                            min_balance: args.data.min_balance,
                                            quota_enforce: enforce
                                        },
                                        async: false,
                                        success: function(json) {
                                            args.response.success({
                                                data: json.quotacreditsresponse.totalquota
                                            });
                                        }
                                    });
                                    $(window).trigger('cloudStack.fullRefresh');
                                 }
                            },
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
                                                item.startquota = item.currency + ' ' + item.startquota;
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

                          }
                      }
                  }
              },

              fullSummary: {
                  type: 'select',
                  title: 'label.quota.fullsummary',
                  listView: {
                      label: 'label.quota.fullsummary',
                      disableInfiniteScrolling: true,
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
                              limitcolor: true,
                              limits: {
                                  upperlimit: 'upperlimit',
                                  lowerlimit: 'lowerlimit'
                              },
                              converter: function(args){
                                  return currency + args.toString();
                              }
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

                          // TODO: add logic in mgmt server to filter by account
                          if (args.filterBy.search.value) {
                              data.search = args.filterBy.search.value;
                          }

                          $.ajax({
                              url: createURL('quotaSummary&listall=true'),
                              data: data,
                              dataType: "json",
                              async: true,
                              success: function(json) {
                                  var items = json.quotasummaryresponse.summary;
                                  if(items){
                                      var array=[];
                                      for (var i = 0; i < items.length; i++) {
                                          if (typeof data.search != 'undefine' && items[i].account.search(data.search) < 0 && items[i].domain.search(data.search) < 0) {
                                              continue;
                                          }
                                          currency = items[i].currency;
                                          items[i].quota = currency + ' ' + items[i].quota;
                                          items[i].lowerlimit = -1;
                                          items[i].upperlimit = 0;
                                          array.push(items[i]);
                                      }
                                      args.response.success({
                                          data: array
                                      });
                                  }
                                  else {
                                      args.response.success({
                                          data: 0
                                      });
                                  }
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
                             add: {
                                label: 'label.quota.add.credits',
                                preFilter: function(args) { return isAdmin(); },
                                messages: {
                                    confirm: function(args) {
                                        return 'label.quota.add.credits';
                                    },
                                    notification: function(args) {
                                        return 'label.quota.add.credits';
                                    }
                                },

                                createForm: {
                                    title: 'label.quota.credits',
                                    desc: '',
                                    fields: {
                                         value: {
                                            label: 'label.quota.credits',
                                            validation: {
                                                required: true
                                            }
                                        },
                                        min_balance: {
                                            label: 'label.quota.minbalance',
                                            validation: {
                                                required: true
                                            }
                                        },
                                        quota_enforce: {
                                            label: 'label.quota.enforcequota',
                                            isBoolean: true,
                                            isChecked: false
                                        },
                                    }

                                },

                                action: function(args) {
                                     var enforce=args.data.quota_enforce == 'on' ? true: false;
                                     $.ajax({
                                         url: createURL('quotaCredits'),
                                         data: {
                                             domainid: args.context.fullSummary[0].domainid,
                                             account: args.context.fullSummary[0].account,
                                             value: args.data.value,
                                             min_balance: args.data.min_balance,
                                             quota_enforce: enforce
                                         },
                                        async: false,
                                        success: function(json) {
                                            args.response.success({
                                                data: json.quotacreditsresponse.totalquota
                                            });
                                        }
                                    });
                                    $(window).trigger('cloudStack.fullRefresh');
                                 }
                            },
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
                                        $.ajax({
                                            url: createURL('quotaBalance'),
                                            dataType: 'json',
                                            async: true,
                                            data: {
                                                domainid: args.context.fullSummary[0].domainid,
                                                account: args.context.fullSummary[0].account
                                            },
                                            success: function(json) {
                                                var item = json.quotabalanceresponse.balance;
                                                item.startdate = now.toJSON().slice(0,10);
                                                item.startquota = item.currency + ' ' + item.startquota;
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
                           }
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
                                                maxDate: '+0d',
                                                validation: {
                                                    required: true
                                                }
                                            },
                                            enddate: {
                                                label: 'label.quota.enddate',
                                                isDatepicker: true,
                                                maxDate: '+0d',
                                                validation: {
                                                    required: true
                                                }
                                            }
                                        }
                                      },
                                     action: function(args) {
                                        newstartdate= args.data.startdate.slice(0,10);
                                        newenddate= args.data.enddate.slice(0,10);
                                        $(window).trigger('cloudStack.fullRefresh');
                                    }
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
                                  var usages = json.quotastatementresponse.statement.quotausage;
                                  var currency = json.quotastatementresponse.statement.currency;
                                  $.each(usages, function(idx, item) {
                                      usages[idx].quota = currency + ' ' + usages[idx].quota;
                                  });
                                  usages.push({
                                        name: _l('label.quota.total') + ' : ',
                                        unit:'',
                                        quota: currency + ' ' + json.quotastatementresponse.statement.totalquota
                                    });

                                  usages.unshift({
                                      name: _l('label.quota.startdate')  + ' : ' + json.quotastatementresponse.statement.startdate.slice(0,10),
                                      unit: _l('label.quota.enddate')  + ' : ' + json.quotastatementresponse.statement.enddate.slice(0,10),
                                      quota: ''
                                  });


                                  args.response.success({
                                      data: usages
                                  });
                              },
                              error:function(data) {
                                  cloudStack.dialog.notice({
                                      message: parseXMLHttpResponse(data)
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
                                                maxDate: '+0d',
                                                validation: {
                                                    required: true
                                                }
                                            },
                                            enddate: {
                                                label: 'label.quota.enddate',
                                                isDatepicker: true,
                                                maxDate: '+0d',
                                                validation: {
                                                    required: true
                                                }
                                            }
                                        }
                                      },
                                    action: function(args) {
                                            newstartdate= args.data.startdate.slice(0,10);
                                            newenddate= args.data.enddate.slice(0,10);
                                            $(window).trigger('cloudStack.fullRefresh');
                                        }
                                  }
                           },

                      dataProvider: function(args) {
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
                                  var bal = json.quotabalanceresponse.balance;
                                  var currency = bal.currency;
                                  var array=[{
                                               date: bal.startdate.slice(0,10),
                                               quota: currency + ' ' + bal.startquota,
                                               credit: ''
                                  }];
                                  //now add all credits
                                  for (var i = 0; i < bal.credits.length; i++) {
                                        array.push({
                                            date: bal.credits[i].updated_on.slice(0,10),
                                            quota: '',
                                            credit:  currency + ' ' + bal.credits[i].credits
                                        });
                                    }
                                  array.push({
                                            date: bal.enddate.slice(0,10),
                                            quota:  currency + ' ' + bal.endquota,
                                            credit: ''
                                        });
                                  args.response.success({
                                      data: array
                                  });
                              },
                              error:function(data) {
                  cloudStack.dialog.notice({
                                      message: parseXMLHttpResponse(data)
                                  });
                              }
                          });
                      }
                  } // end list view
              }, // end statement


              tariffEdit: {
                  type: 'select',
                  title: 'label.quota.tariff',
                  listView: {
                      label: 'label.quota.tariff',
                      disableInfiniteScrolling: true,
                      actions: {
                          edit: {
                              label: 'label.change.value',
                              action: function(args) {
                                  if (isAdmin()) {
                                       var data = {
                                            usagetype: args.data.jsonObj.usageType,
                                       };
                                      var tariffVal = args.data.tariffValue.split(' ');
                                      if (tariffVal.length==2){
                                          data.value = tariffVal[1];
                                      }
                                      else {
                                          data.value = tariffVal[0];
                                      }
                                      if (!isNaN(parseFloat(data.value)) && isFinite(data.value)){
                                          var updateTariffForm = cloudStack.dialog.createForm({
                                              form: {
                                                  title: 'label.quota.configuration',
                                                  fields: {
                                                      tariffValue: {
                                                          label: 'label.quota.value',
                                                          number: true,
                                                          validation: {
                                                              required: true
                                                          }
                                                      },
                                                      effectiveDate: {
                                                          label: 'label.quota.tariff.effectivedate',
                                                          isDatepicker: true,
                                                          dependsOn: 'startdate',
                                                          minDate: '+1d',
                                                          validation: {
                                                              required: true
                                                          }
                                                      },
                                                  }
                                              },
                                              after: function(argsLocal) {
                                                  data.startDate = argsLocal.data.effectiveDate;
                                                  data.value = argsLocal.data.tariffValue;
                                                  $.ajax({
                                                      url: createURL('quotaTariffUpdate'),
                                                      data: data,
                                                      type: "POST",
                                                      success: function(json) {
                                                          // Refresh listings on chosen date to reflect new tariff
                                                          $($.find('div.search-bar input')).val(data.startDate);
                                                          $('#basic_search').click();
                                                      }
                                                  });
                                              }
                                          });
                                          updateTariffForm.find('input[name=tariffValue]').val(data.value);
                                          updateTariffForm.find('input[name=effectiveDate]').focus();
                                      }
                                      else {
                                          alert("Bad tariff value, this should be a number " + data.value);
                                          $(window).trigger('cloudStack.fullRefresh');
                                      }//bad data.value - NaN
                                  } // if is ADMIN
                              } // end action
                          }//edits
                      },//actions
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
                              label: 'label.quota.tariff.value',
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

                                  $($.find('.list-view')).data('end-of-table', true);
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


              tariffView: {
                  type: 'select',
                  title: 'label.quota.tariff',
                  listView: {
                      label: 'label.quota.tariff',
                      disableInfiniteScrolling: true,
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
                              label: 'label.quota.tariff.value',
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

                                  $($.find('.list-view')).data('end-of-table', true);
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
