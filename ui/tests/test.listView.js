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
(function($) {
  module('List view', {
    setup: function() {
      $.fx.off = true;
      cloudStack.debug = true;
    },
    teardown: function() {
      // Cleanup notification box
      $('.notification-box').remove();
    }
  });

  test('Basic', function() {
    var listView = {
      listView: {
        section: 'test123',
        fields: {
          fieldA: { label: 'testFieldA' },
          fieldB: { label: 'testFieldB' }
        },
        dataProvider: function(args) {
          args.response.success({
            data: []
          });
        }
      }
    };
    var $listView;

    ok($listView = $('<div>').listView(listView), 'Initialize list view');
    equal($listView.find('.list-view').size(), 1, 'List view has container div');
    equal($listView.find('.list-view.test123').size(), 1, 'Container div has section ID as CSS class');
    equal($listView.find('.list-view table').size(), 2, 'List view has split tables');
    equal($listView.find('.list-view .fixed-header table thead tr').size(), 1, 'List view has fixed table header');
    equal($listView.find('.list-view .fixed-header table thead th').size(), 2, 'List view has correct column headers');
    equal($listView.find('.list-view .fixed-header table thead th:first').html(), 'testFieldA', 'First header has correct label');
    ok($listView.find('.list-view .fixed-header table thead th:first').hasClass('fieldA'), 'First header has correct class');
    ok($listView.find('.list-view .fixed-header table thead th:last').hasClass('fieldB'), 'First header has correct class');
    equal($listView.find('.list-view .fixed-header table thead th:last').html(), 'testFieldB', 'First header has correct label');
    equal($listView.find('.list-view table tbody tr').size(), 1, 'List view has table body');
    equal($listView.find('.toolbar').size(), 1, 'List view has toolbar');
    equal($listView.find('.toolbar .text-search .search-bar input[type=text]').size(), 1, 'Toolbar has search box');
    equal($listView.find('.toolbar .text-search .search-bar input[type=text]').size(), 1, 'Toolbar has search box');
    equal($listView.find('.toolbar .text-search .button.search').size(), 1, 'Toolbar has search button');
    ok(!$listView.find('.toolbar .filters').size(), 'Toolbar doesn\'t have filters');
  });

  test('Data provider', function() {
    var $listView = $('<div>');
    
    stop();
    $listView.listView({
      context: {
        tests: []
      },
      listView: {
        section: 'test',
        fields: {
          fieldA: { label: 'testFieldA' },
          fieldB: { label: 'testFieldB' }
        },
        dataProvider: function(args) {
          start();
          equal(args.page, 1, 'Correct page # passed');
          equal(args.filterBy.search.value, '', 'No search params specified');
          ok($.isArray(args.context.tests), 'Context passed');
          args.response.success({
            data: [
              {
                fieldA: '1A',
                fieldB: '1B'
              },
              {
                fieldA: '2A',
                fieldB: '2B'
              },
              {
                fieldA: '3A',
                fieldB: '3B'
              }
            ]
          });
          stop();
          setTimeout(function() {
            start();
            equal($listView.find('tbody tr').size(), 3, 'Data row count is correct');
            equal($listView.find('tbody tr:first td.fieldA span').html(), '1A', 'Correct table item value for first row');
            equal($listView.find('tbody tr:first td.fieldB span').html(), '1B', 'Correct table item value for first row');
            equal($listView.find('tbody tr:last td.fieldA span').html(), '3A', 'Correct table item value for last row');
            equal($listView.find('tbody tr:last td.fieldB span').html(), '3B', 'Correct table item value for last row');
          });
        }
      }
    });
  });

  test('Pre-filter', function() {
    var $listView = $('<div>');
    
    stop();
    $listView.listView({
      listView: {
        section: 'test',
        fields: {
          fieldA: { label: 'testFieldA' },
          fieldB: { label: 'testFieldB' },
          fieldC: { label: 'testFieldC' }
        },
        preFilter: function(args) {
          return ['fieldC'];
        },
        dataProvider: function(args) {
          args.response.success({
            data: [
              {
                fieldA: '1A',
                fieldB: '1B',
                fieldC: '1C'
              }
            ]
          });
          setTimeout(function() {
            start();
            equal($listView.find('thead th').size(), 2, 'Correct # of filtered columns present');
            equal($listView.find('tbody tr:first td').size(), 2, 'Correct # of body columns present');
            equal($listView.find('tbody tr:first td.fieldA span').html(), '1A', 'Correct table item value for data row');
            equal($listView.find('tbody tr:first td.fieldB span').html(), '1B', 'Correct table item value for data row');
          });
        }
      }
    });
  });

  test('Section select', function() {
    var $listView = $('<div>');
    
    ok($listView.listView({
      sectionSelect: {
        label: 'testSectionSelect'
      },
      sections: {
        sectionA: {
          type: 'select',
          title: 'sectionA',
          listView: {
            fields: {
              fieldA: { label: 'testFieldA' },
              fieldB: { label: 'testFieldB' }
            },
            dataProvider: function(args) {
              args.response.success({
                data: [
                  {
                    fieldA: '1A',
                    fieldB: '1B'
                  },
                  {
                    fieldA: '2A',
                    fieldB: '2B'
                  }
                ]
              });
            }
          }  
        },
        
        sectionB: {
          type: 'select',
          title: 'sectionB',
          listView: {
            fields: {
              fieldC: { label: 'testFieldC' },
              fieldD: { label: 'testFieldD' },
              fieldE: { label: 'testFieldE' }
            },
            dataProvider: function(args) {
              args.response.success({
                data: [
                  {
                    fieldC: '1C',
                    fieldD: '1D',
                    fieldE: '1E'
                  },
                  {
                    fieldC: '2C',
                    fieldD: '2D',
                    fieldE: '2E'
                  },
                  {
                    fieldC: '3C',
                    fieldD: '3D',
                    fieldE: '3E'
                  },
                  {
                    fieldC: '4C',
                    fieldD: '4D',
                    fieldE: '4E'
                  }
                ]
              });
            }
          }
        }
      }
    }));

    equal($listView.find('.toolbar .section-switcher').size(), 1,
          'Section switcher present in toolbar');
    equal($listView.find('.toolbar .section-switcher .section-select select').size(), 1,
          'Section select present');
    equal($listView.find('.toolbar .section-switcher .section-select label').html(),
          'testSectionSelect' + ':',
          'Section select label is correct');
    equal($listView.find('.toolbar .section-switcher .section-select select option').size(), 2,
          'Selectable sections present as options');
    equal($listView.find('.toolbar .section-switcher .section-select select option:first').html(), 'sectionA',
          'First select has correct title');
    equal($listView.find('.toolbar .section-switcher .section-select select option:selected')[0],
          $listView.find('.toolbar .section-switcher .section-select select option:first')[0],
          'First section option is selected by default');
    equal($listView.find('.toolbar .section-switcher .section-select select option:last').html(), 'sectionB', 'Last select has correct title');
    equal($listView.find('.list-view thead th').size(), 2, 'Correct list view column count present');
    equal($listView.find('.list-view thead th:first').html(), 'testFieldA', 'Column 1 is labeled correctly');
    equal($listView.find('.list-view thead th:last').html(), 'testFieldB', 'Column 2 is labeled correctly');
    equal($listView.find('.list-view tbody tr').size(), 2, 'Correct # of data rows present');
    equal($listView.find('.list-view tbody tr:first td').size(), 2, 'Correct # of data columns present');
    
    $listView.find('.toolbar .section-switcher select').val('sectionB');
    stop();
    ok($listView.find('.toolbar .section-switcher select').change(), 'Change section');
    start();
    
    equal($listView.find('.list-view thead th').size(), 3, 'Correct list view column count present');
    equal($listView.find('.list-view thead th:first').html(), 'testFieldC', 'Column 1 is labeled correctly');
    equal($($listView.find('.list-view thead th')[1]).html(), 'testFieldD', 'Column 2 is labeled correctly');
    equal($listView.find('.list-view thead th:last').html(), 'testFieldE', 'Column 3 is labeled correctly');
    equal($listView.find('.list-view tbody tr').size(), 4, 'Correct # of data rows present');
    equal($listView.find('.list-view tbody tr:first td').size(), 3, 'Correct # of data columns present');
    equal($listView.find('.list-view tbody tr:first td:first span').html(), '1C', 'First table cell has correct data');
    equal($listView.find('.list-view tbody tr:last td:last span').html(), '4E', 'Last table cell has correct data');

    $listView.find('.toolbar .section-switcher select').val('sectionA');
    stop();
    ok($listView.find('.toolbar .section-switcher select').change(), 'Change section');
    start();
    equal($listView.find('.toolbar .section-switcher .section-select select option:last').html(), 'sectionB', 'Last select has correct title');
    equal($listView.find('.list-view thead th').size(), 2, 'Correct list view column count present');
    equal($listView.find('.list-view thead th:first').html(), 'testFieldA', 'Column 1 is labeled correctly');
    equal($listView.find('.list-view thead th:last').html(), 'testFieldB', 'Column 2 is labeled correctly');
    equal($listView.find('.list-view tbody tr').size(), 2, 'Correct # of data rows present');
    equal($listView.find('.list-view tbody tr:first td').size(), 2, 'Correct # of data columns present');
    equal($listView.find('.list-view tbody tr:first td:first span').html(), '1A', 'First table cell has correct data');
    equal($listView.find('.list-view tbody tr:last td:last span').html(), '2B', 'Last table cell has correct data');
  });

  test('Basic sync action', function() {
    var $cloudStack = $('<div>').appendTo('#qunit-fixture');
    var listView = {
      listView: {
        section: 'test123',
        fields: {
          fieldA: { label: 'testFieldA' },
          fieldB: { label: 'testFieldB' }
        },
        actions: {
          basicSync: {
            label: 'basicAction',
            messages: {
              confirm: function() {
                return 'basicActionConfirm';
              },
              notification: function() {
                return 'basicActionNotification';
              }
            },
            action: function(args) {
              args.response.success();
            }
          }
        },
        dataProvider: function(args) {
          args.response.success({
            data: [
              {
                fieldA: '1A',
                fieldB: '1B',
                fieldC: '1C'
              },
              {
                fieldA: '2A',
                fieldB: '2B',
                fieldC: '2C'
              }
            ]
          });
        }
      }
    };

    // CloudStack object is needed for notification handling for actions
    var cloudStack = {
      sections: {
        testActions: listView
      },

      home: 'testActions'
    };
    
    ok($cloudStack.cloudStack(cloudStack), 'Initialize cloudStack widget w/ list view');

    var $listView = $cloudStack.find('.list-view');
    
    equal($listView.find('table thead th').size(), 3, 'Correct header column count');
    equal($listView.find('table thead th.actions').size(), 1, 'Action header column present');
    equal($listView.find('table tbody tr:first td').size(), 3, 'Correct data column count');
    equal($listView.find('table tbody tr:first td.actions').size(), 1, 'Action data column present');
    equal($listView.find('table tbody tr:first td.actions .action').size(), 1, 'Correct action count');
    equal($listView.find('table tbody tr:first td.actions .action:first .icon').size(), 1, 'Action has icon');
    ok($listView.find('table tbody tr:first td.actions .action:first').hasClass('basicSync'),
       'First action has ID as CSS class');
    ok($listView.find('td.actions .action:first').click(), 'Click first action');
    equal($('.ui-dialog.confirm .message').html(), 'basicActionConfirm', 'Confirmation message present');

    // Make sure dialog is cleaned up -- always put below all confirm tests
    $(':ui-dialog, .overlay').appendTo('#qunit-fixture');

    ok($('.ui-dialog.confirm .ui-button.ok').click(), 'Confirm action');
    equal($cloudStack.find('.notifications .total span').html(), '1', 'Notification total increased');
    equal($('.notification-box ul li').size(), 1, 'Notification list has 1 item');
    equal($('.notification-box ul li span').html(), 'basicActionNotification', 'Notification list item has correct label');
    ok($('.notification-box ul li').hasClass('pending'), 'Notification has pending state');
    stop();
    setTimeout(function() {
      start();
      ok(!$('.notification-box ul li').hasClass('pending'), 'Notification has completed state');
    });
  });

  test('Async action', function() {
    var $cloudStack = $('<div>').appendTo('#qunit-fixture');
    var listView = {
      listView: {
        id: 'testAsyncListView',
        fields: {
          fieldA: { label: 'testFieldA' },
          fieldB: { label: 'testFieldB' }
        },
        actions: {
          asyncTest: {
            label: 'asyncAction',
            messages: {
              confirm: function() {
                return 'asyncActionConfirm';
              },
              notification: function() {
                return 'asyncActionNotification';
              }
            },
            action: function(args) {
              ok($.isArray(args.context.testAsyncListView), 'List view context passed');
              args.response.success({
                _custom: {
                  jobId: 'job123'
                }
              });
            },
            notification: {
              interval: 0,
              poll: function(args) {
                start();
                equal(args._custom.jobId, 'job123', 'Job ID passed correctly');
                args.complete();
              }
            }
          }
        },
        dataProvider: function(args) {
          args.response.success({
            data: [
              {
                fieldA: '1A',
                fieldB: '1B',
                fieldC: '1C'
              },
              {
                fieldA: '2A',
                fieldB: '2B',
                fieldC: '2C'
              }
            ]
          });
        }
      }
    };

    // CloudStack object is needed for notification handling for actions
    var cloudStack = {
      sections: {
        testActions: listView
      },

      home: 'testActions'
    };
    
    ok($cloudStack.cloudStack(cloudStack), 'Initialize cloudStack widget w/ list view');

    var $listView = $cloudStack.find('.list-view');
    
    equal($listView.find('table thead th').size(), 3, 'Correct header column count');
    equal($listView.find('table thead th.actions').size(), 1, 'Action header column present');
    equal($listView.find('table tbody tr:first td').size(), 3, 'Correct data column count');
    equal($listView.find('table tbody tr:first td.actions').size(), 1, 'Action data column present');
    equal($listView.find('table tbody tr:first td.actions .action').size(), 1, 'Correct action count');
    equal($listView.find('table tbody tr:first td.actions .action:first .icon').size(), 1, 'Action has icon');
    ok($listView.find('table tbody tr:first td.actions .action:first').hasClass('asyncTest'),
       'First action has ID as CSS class');
    ok($listView.find('td.actions .action:first').click(), 'Click first action');
    equal($('.ui-dialog.confirm .message').html(), 'asyncActionConfirm', 'Confirmation message present');

    // Make sure dialog is cleaned up -- always put below all confirm tests
    $(':ui-dialog, .overlay').appendTo('#qunit-fixture');

    ok($('.ui-dialog.confirm .ui-button.ok').click(), 'Confirm action');
    equal($cloudStack.find('.notifications .total span').html(), '1', 'Notification total increased');
    equal($('.notification-box ul li').size(), 1, 'Notification list has 1 item');
    equal($('.notification-box ul li span').html(), 'asyncActionNotification', 'Notification list item has correct label');
    ok($('.notification-box ul li').hasClass('pending'), 'Notification has pending state');
    stop();
  });

  test('Action filter', function() {
    var $cloudStack = $('<div>').appendTo('#qunit-fixture');
    var listView = {
      listView: {
        id: 'testAsyncListView',
        fields: {
          fieldA: { label: 'testFieldA' },
          fieldB: { label: 'testFieldB' }
        },
        actions: {
          actionA: {
            label: 'actionA',
            messages: {
              confirm: function() { return ''; },
              notification: function() { return ''; }
            },
            action: function(args) { args.response.success(); }
          },
          actionB: {
            label: 'actionB [HIDDEN]',
            messages: {
              confirm: function() { return ''; },
              notification: function() { return ''; }
            },
            action: function(args) { args.response.success(); }
          },
          actionC: {
            label: 'actionC',
            messages: {
              confirm: function() { return ''; },
              notification: function() { return ''; }
            },
            action: function(args) { args.response.success(); }
          }
        },
        dataProvider: function(args) {
          args.response.success({
            actionFilter: function() {
              return ['actionA', 'actionC'];
            },
            data: [
              {
                fieldA: '1A',
                fieldB: '1B',
                fieldC: '1C'
              },
              {
                fieldA: '2A',
                fieldB: '2B',
                fieldC: '2C'
              }
            ]
          });
        }
      }
    };

    // CloudStack object is needed for notification handling for actions
    var cloudStack = {
      sections: {
        testActions: listView
      },

      home: 'testActions'
    };
    
    ok($cloudStack.cloudStack(cloudStack), 'Initialize cloudStack widget w/ list view');

    var $listView = $cloudStack.find('.list-view');
    
    equal($listView.find('table thead th').size(), 3, 'Correct header column count');
    equal($listView.find('table thead th.actions').size(), 1, 'Action header column present');
    equal($listView.find('table tbody tr:first td.actions').size(), 1, 'Action data column present');
    equal($listView.find('table tbody tr:first td.actions .action').size(), 3, 'Correct action count (all)');
    equal($listView.find('table tbody tr:first td.actions .action:not(.disabled)').size(), 2, 'Correct action count (enabled)');
    ok($listView.find('table tbody tr:first td.actions .action:first').hasClass('actionA'),
       'First action has correct ID');
    ok($listView.find('table tbody tr:first td.actions .action:last').hasClass('actionC'),
       'Last action has correct ID');
  });
}(jQuery)); 
