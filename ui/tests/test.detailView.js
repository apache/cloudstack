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
  module('Detail view', {
    setup: function() {
      cloudStack.dialog.__confirm = cloudStack.dialog.confirm;
      cloudStack.ui.notifications.__add = cloudStack.ui.notifications.add;
      $.fn.__cloudBrowser = $.fn.cloudBrowser;
      $.fn.__listView = $.fn.listView;
      $.fn.__dataTable = $.fn.dataTable;
      $.fn.__is = $.fn.is;

      $.fn.is = function(args) {
        if (args == ':visible')
          return true; // No test elems will ever be shown, so just pretend they are visible

        return true;
      };
    },

    teardown: function() {
      cloudStack.dialog.confirm = cloudStack.dialog.__confirm;
      cloudStack.ui.notifications.add = cloudStack.ui.notifications.__add;
      $.fn.cloudBrowser = $.fn.__cloudBrowser;
      $.fn.listView = $.fn.__listView;
      $.fn.dataTable = $.fn.__dataTable;
      $.fn.is = $.fn.__is;
    }
  });

  test('Basic', function() {
    var detailView = {
      tabs: {
        tabA: {
          title: 'tabA',
          fields: [{}],
          dataProvider: function() {}
        },
        tabB: {
          title: 'tabB',
          fields: [{}],
          dataProvider: function() {}
        }
      }
    };

    var $detailView = $('<div>');

    ok($detailView.detailView(detailView), 'Create detail view');
    equal($detailView.find('.ui-tabs-nav li').size(), 2, 'Detail view has correct tab count');
    equal($detailView.find('.ui-tabs-nav li:first a').html(), 'tabA', 'First tab has correct title');
    equal($detailView.find('.ui-tabs-nav li:last a').html(), 'tabB', 'Last tab has correct title');
  });

  test('Data provider', function() {
    var detailView = {
      tabs: {
        tabA: {
          title: 'tabA',
          fields: [{
            fieldA: { label: 'fieldA' },
            fieldB: { label: 'fieldB' }
          }],
          dataProvider: function(args) {
            start();
            ok(args.response.success({
              data: {
                fieldA: 'dataProviderFieldA',
                fieldB: 'dataProviderFieldB'
              }
            }), 'Call success');
            equal($detailView.find('tr').size(), 2, 'Correct fields rendered');
            equal($detailView.find('tr:first td:first').html(), 'fieldA', 'First field has correct label');
            equal($detailView.find('tr:first td:last').html(), 'dataProviderFieldA', 'First field has correct content');
            equal($detailView.find('tr:last td:first').html(), 'fieldB', 'Last field has correct label');
            equal($detailView.find('tr:last td:last').html(), 'dataProviderFieldB', 'Last field has correct content');
          }
        }
      }
    };
    var $detailView = $('<div>');

    stop();

    // Test first tab
    $detailView = $detailView.detailView(detailView);

    // Test last tab
    $detailView.find('.ui-tabs-nav li:last').click();
  });

  test('Data provider, multiple tabs', function() {
    var detailView = {
      tabs: {
        tabA: {
          title: 'tabA',
          fields: [{
            fieldA: { label: 'fieldA' },
            fieldB: { label: 'fieldB' }
          }],
          dataProvider: function(args) {
            start();
            ok(args.response.success({
              data: {
                fieldA: 'dataProviderFieldA',
                fieldB: 'dataProviderFieldB'
              }
            }), 'Call success');
            equal($detailView.find('tr').size(), 2, 'Correct fields rendered');
            equal($detailView.find('tr:first td:first').html(), 'fieldA', 'First field has correct label');
            equal($detailView.find('tr:first td:last').html(), 'dataProviderFieldA', 'First field has correct content');
            equal($detailView.find('tr:last td:first').html(), 'fieldB', 'Last field has correct label');
            equal($detailView.find('tr:last td:last').html(), 'dataProviderFieldB', 'Last field has correct content');
          }
        },

        tabB: {
          title: 'tabB',
          fields: [{
            fieldC: { label: 'fieldC' },
            fieldD: { label: 'fieldD' },
            fieldC: { label: 'fieldE' },
            fieldD: { label: 'fieldF' }
          }],
          dataProvider: function(args) {
            start();
            ok(args.response.success({
              data: {
                fieldC: 'dataProviderFieldC',
                fieldD: 'dataProviderFieldD',
                fieldE: 'dataProviderFieldE',
                fieldF: 'dataProviderFieldF'
              }
            }), 'Call success');
            equal($detailView.find('tr').size(), 4, 'Correct fields rendered');
            equal($detailView.find('tr:first td:first').html(), 'fieldC', 'First field has correct label');
            equal($detailView.find('tr:first td:last').html(), 'dataProviderFieldC', 'First field has correct content');
            equal($detailView.find('tr:last td:first').html(), 'fieldF', 'Last field has correct label');
            equal($detailView.find('tr:last td:last').html(), 'dataProviderFieldF', 'Last field has correct content');
          }
        }
      }
    };
    var $detailView = $('<div>');

    stop();

    // Test first tab
    $detailView = $detailView.detailView(detailView);

    // Test last tab
    $detailView.find('.ui-tabs-nav li:last').click();
  });

  test('Field pre-filter', function() {
    var detailView = {
      tabs: {
        tabA: {
          title: 'tabA',
          fields: {
            fieldA: { label: 'fieldA' },
            fieldB: { label: 'fieldB' },
            fieldC: { label: 'fieldC' },
            fieldD: { label: 'fieldD' }
          },
          preFilter: function(args) {
            return ['fieldB', 'fieldC'];
          },
          dataProvider: function (args) {
            args.response.success({
              data: {
                fieldA: 'fieldAContent',
                fieldB: 'fieldBContent',
                fieldC: 'fieldCContent',
                fieldD: 'fieldDContent'
              }
            });

            start();
            equal($detailView.find('tr').size(), 2, 'Correct fields rendered');
            equal($detailView.find('tr:first td:first').html(), 'fieldA', 'First field has correct label');
            equal($detailView.find('tr:first td:last').html(), 'fieldAContent', 'First field has correct content');
            equal($detailView.find('tr:last td:first').html(), 'fieldD', 'Last field has correct label');
            equal($detailView.find('tr:last td:last').html(), 'fieldDContent', 'Last field has correct content');
          }
        }
      }
    };
    var $detailView = $('<div>');

    stop();

    $detailView.detailView(detailView);
  });

  test('Action', function() {
    var detailView = {
      actions: {
        actionA: {
          label: 'testActionA',
          action: function(args) {
            start();
            ok(args.response.success(), 'Call success from action A');
          },
          messages: {
            confirm: function() { return 'testActionAConfirm'; },
            notification: function() { return 'testActionANotification'; }
          }
        },
        actionB: {
          label: 'testActionB',
          action: function(args) {
            start();
            ok(args.response.success(), 'Call success from action B');
          },
          messages: {
            confirm: function() { return 'testActionBConfirm'; },
            notification: function() { return 'testActionBNotification'; }
          },
          notification: {
            poll: function(args) {
              start();
              ok(args.complete(), 'Call complete from async action B');
            }
          }
        }
      },
      tabs: {
        tabA: {
          title: 'tabA',
          fields: {
            fieldA: { label: 'fieldA' },
            fieldB: { label: 'fieldB' }
          },
          dataProvider: function(args) {
            args.response.success({
              data: {
                fieldA: 'fieldAContent',
                fieldB: 'fieldBContent'
              }
            });
          }
        }
      }
    };
    var $detailView = $('<div>');

    $detailView.detailView(detailView).appendTo('#qunit-fixture');

    equal($detailView.find('.detail-actions').size(), 1, 'Action container present');
    equal($detailView.find('.detail-actions .action').size(), 2, 'Correct action count');
    equal($detailView.find('.detail-actions .action.actionA').size(), 1, 'actionA present');
    equal($detailView.find('.detail-actions .action.actionB').size(), 1, 'actionB present');

    cloudStack.dialog.confirm = function(args) {
      start();
      equal(args.message, 'testActionAConfirm', 'Correct confirmation message for action A');
      stop();

      args.action(); // Perform action
    };

    cloudStack.ui.notifications.add = function(notification, success, successArgs) {
      stop();
      equal(notification.desc, 'testActionANotification', 'Correct notification message for action A');
      start();
    };

    $detailView.find('.detail-actions .action.actionA a').click(); // <a> triggers action, not action's container

    cloudStack.dialog.confirm = function(args) {
      start();
      equal(args.message, 'testActionBConfirm', 'Correct confirmation message for action B');
      stop();

      args.action(); // Perform action
    };

    cloudStack.ui.notifications.add = function(notification, success, successArgs) {
      start();
      equal(notification.desc, 'testActionBNotification', 'Correct notification message for action B');
      stop();
      notification.poll({ complete: function() { return true; } });
    };

    $detailView.find('.detail-actions .action.actionB a').click(); // <a> triggers action, not action's container
  });

  test('Action filter', function() {
    var detailView = {
      actions: {
        actionA: {
          label: 'testActionA',
          action: function(args) {}
        },
        actionB: {
          label: 'testActionB',
          action: function(args) {}
        }
      },
      tabs: {
        tabA: {
          title: 'tabA',
          fields: {
            fieldA: { label: 'fieldA' },
            fieldB: { label: 'fieldB' }
          },
          dataProvider: function(args) {
            args.response.success({
              actionFilter: function() {
                return ['actionA'];
              },
              data: {
                fieldA: 'fieldAContent',
                fieldB: 'fieldBContent'
              }
            });
          }
        }
      }
    };
    var $detailView = $('<div>');

    $detailView.detailView(detailView).appendTo('#qunit-fixture');

    equal($detailView.find('.detail-actions .action').size(), 1, 'Correct action count');
    equal($detailView.find('.detail-actions .action.actionA').size(), 1, 'actionA present');
    notEqual($detailView.find('.detail-actions .action.actionB').size(), 1, 'actionB not present');
  });

  test('Refresh', function() {
    var dataA = ['dataLoad1A', 'dataLoad2A'];
    var dataB = ['dataLoad1B', 'dataLoad2B'];
    var index = 0;

    var detailView = {
      tabs: {
        tabA: {
          title: 'tabA',
          fields: {
            fieldA: { label: 'fieldA' }
          },
          dataProvider: function(args) {
            args.response.success({ data: { fieldA: dataA[index]  }});
            start();
            equal($detailView.find('tr td:last').html(), dataA[index], 'Tab A data correct for load ' + (index + 1));
            index++;
          }
        },
        tabB: {
          title: 'tabB',
          fields: {
            fieldB: { label: 'fieldB' }
          },
          dataProvider: function(args) {
            args.response.success({ data: { fieldB: dataB[index]  }});
            start();
            equal($detailView.find('tr td:last').html(), dataB[index], 'Tab B data correct for load ' + (index + 1));
            index++;
          }
        }
      }
    };
    var $detailView = $('<div>');

    stop();
    $detailView.detailView(detailView).appendTo('#qunit-fixture');

    stop();
    $detailView.find('.button.refresh').click();

    stop();
    index = 0;
    $detailView.find('.ui-tabs-nav li.last a').click();

    stop();
    $detailView.find('.button.refresh').click();
  });

  test('View all, 1 section', function() {
    var $browser = $('<div>').appendTo('#qunit-fixture');
    var detailView = {
      $browser: $browser,
      context: {},
      viewAll: { label: 'testListView', path: 'testListView' },
      tabs: {
        tabA: {
          title: 'tabA',
          fields: [{ fieldA: { label: 'fieldA' }}],
          dataProvider: function(args) { args.response.success({ data: { fieldA: 'fieldAContent' } }); start(); }
        }
      }
    };
    var testListView = {};
    var $detailView = $('<div>').appendTo('#qunit-fixture');

    $('<div>').attr('cloudStack-container', true).data('cloudStack-args', {
      sections: {
        testListView: testListView
      }
    }).appendTo('#qunit-fixture');

    stop();

    $.fn.cloudBrowser = function(cmd, args) {};
    $browser.cloudBrowser();
    $detailView.detailView(detailView);

    equal($detailView.find('.detail-group.actions td.view-all').size(), 1, 'Detail view has view all button');

    stop();

    $.fn.listView = function(args, options) {
      start();
      ok(true, 'List view called');
      equal(args, testListView, 'Correct list view passed');
      ok(args.$browser.size(), 'Browser passed in args');
      ok($.isPlainObject(args.ref), 'Ref passed in args');
      equal(args.id, 'testListView', 'Correct section ID');

      return this;
    };

    $.fn.cloudBrowser = function(cmd, args) {
      start();
      equal(cmd, 'addPanel', 'Browser add panel called');
      stop();
      args.complete($('<div>'));
    };

    $detailView.find('.view-all a').click();
  });

  test('View all, subsections', function() {
    var $browser = $('<div>').appendTo('#qunit-fixture');
    var detailView = {
      $browser: $browser,
      context: {},
      viewAll: { label: 'testListView', path: 'testSection.listViewB' },
      tabs: {
        tabA: {
          title: 'tabA',
          fields: [{ fieldA: { label: 'fieldA' }}],
          dataProvider: function(args) { args.response.success({ data: { fieldA: 'fieldAContent' } }); start(); }
        }
      }
    };
    var listViewA = {};
    var listViewB = {};
    var $detailView = $('<div>').appendTo('#qunit-fixture');

    $('<div>').attr('cloudStack-container', true).data('cloudStack-args', {
      sections: {
        testSection: {
          sections: {
            listViewA: { listView: listViewA },
            listViewB: { listView: listViewB }
          }
        }
      }
    }).appendTo('#qunit-fixture');

    stop();

    $.fn.cloudBrowser = function(cmd, args) {};
    $browser.cloudBrowser();
    $detailView.detailView(detailView);

    equal($detailView.find('.detail-group.actions td.view-all').size(), 1, 'Detail view has view all button');

    stop();

    $.fn.listView = function(args, options) {
      start();
      ok(true, 'List view called');
      equal(args.listView, listViewB, 'Correct list view passed');
      ok(args.$browser.size(), 'Browser passed in args');
      ok($.isPlainObject(args.ref), 'Ref passed in args');
      equal(args.id, 'testSection', 'Correct section ID');

      return this;
    };

    $.fn.cloudBrowser = function(cmd, args) {
      start();
      equal(cmd, 'addPanel', 'Browser add panel called');
      stop();
      args.complete($('<div>'));
    };

    $detailView.find('.view-all a').click();
  });

  test('Pre-action', function() {
    var detailView = {
      actions: {
        test: {
          label: 'test',
          preAction: function() {
            start();
            ok(true, 'Pre-action called');

            return false;
          },
          action: function() {
            ok(false, 'Action called; pre-action should have blocked it');
          },
          messages: { notification: function() { return 'notification'; }}
        }
      },
      tabs: {
        test: {
          title: 'test',
          label: 'testAction',
          fields: [{
            fieldA: { label: 'fieldA' },
            fieldB: { label: 'fieldB' }
          }],
          dataProvider: function(args) { args.response.success({ data: {} }); }
        }
      }
    };
    var $detailView = $('<div>');

    stop();
    $detailView.detailView(detailView);
    $detailView.find('.action.test a').click();
  });

  test('Update data, from list view row', function() {
    var detailView = {
      section: 'testListView',
      context: {
        testListView: [{
          fieldA: 'fieldA-1',
          fieldB: 'fieldB-1',
          fieldC: 'fieldC-1'
        }]
      },
      actions: {
        updateDataTestSync: {
          label: 'updateDataTestSync',
          preAction: function(args) { return true; },
          action: function(args) {
            args.response.success({
              data: {
                fieldA: 'fieldA-2',
                fieldB: 'fieldB-2'
              }
            });

            start();
            equal($detailView.data('view-args').context.testListView[0].fieldA, 'fieldA-2', 'Correct context value for fieldA');
            equal($detailView.data('view-args').context.testListView[0].fieldB, 'fieldB-2', 'Correct context value for fieldB');
            equal($detailView.data('view-args').context.testListView[0].fieldC, 'fieldC-1', 'Correct context value for fieldC');
            equal($detailView.find('tr.fieldA .value').html(), 'fieldA-2', 'Correct table value for fieldA');
            equal($detailView.find('tr.fieldB .value').html(), 'fieldB-2', 'Correct table value for fieldB');
            equal($detailView.find('tr.fieldC .value').html(), 'fieldC-1', 'Correct table value for fieldC');
          },
          messages: { notification: function() { return 'notification'; }}
        }
      },
      tabs: {
        test: {
          title: 'test',
          fields: [{
            fieldA: { label: 'fieldA' },
            fieldB: { label: 'fieldB' },
            fieldC: { label: 'fieldC' }
          }],
          dataProvider: function(args) {
            args.response.success({
              data: args.context.testListView[0]
            });
          }
        }
      }
    };
    var $detailView = $('<div>');
    var $listView = $('<div>').addClass('list-view');
    var $listViewRow = $('<div>').data('json-obj', detailView.context.testListView[0]).appendTo($listView);
    var $cloudStackContainer = $('<div>').attr('cloudStack-container', true).data('cloudStack-args', {
      sections: {
        testListView: {}
      }
    }).appendTo('#qunit-fixture');

    $.fn.dataTable = function() { return this; };
    $.fn.listView = function(args1, args2) {
      if (args1 == 'replaceItem')
        args2.after(args2.$row.data('json-obj', args2.data));

      return this;
    };
    
    cloudStack.ui.notifications.add = function(notification, complete) {
      complete();
    };
    cloudStack.dialog.confirm = function(args) {
      args.action();
    };
    $detailView.data('list-view-row', $listViewRow);
    $detailView.detailView(detailView);

    stop();
    $detailView.find('.action.updateDataTestSync a').click();
  });


  test('Update data async, from list view row', function() {
    var detailView = {
      section: 'testListView',
      context: {
        testListView: [{
          fieldA: 'fieldA-1',
          fieldB: 'fieldB-1',
          fieldC: 'fieldC-1',
          state: 'on'
        }]
      },
      actions: {
        updateDataTestAsync: {
          label: 'updateDataTestAsync',
          preAction: function(args) {
            start();
            ok(true, 'Pre-action called');
            equal($detailView.data('view-args').context.testListView[0].fieldA, 'fieldA-1', 'Pre-action: Correct context value for fieldA');
            equal($detailView.data('view-args').context.testListView[0].fieldB, 'fieldB-1', 'Pre-action: Correct context value for fieldB');
            equal($detailView.data('view-args').context.testListView[0].fieldC, 'fieldC-1', 'Pre-action: Correct context value for fieldC');
            stop();

            return true;
          },
          action: function(args) {
            args.response.success();
          },
          messages: { notification: function() { return 'notification'; }},
          notification: {
            poll: function(args) {
              args.complete({
                data: {
                  fieldA: 'fieldA-2',
                  fieldB: 'fieldB-2',
                  state: 'off'
                }
              });

              start();
              equal($detailView.data('view-args').context.testListView[0].fieldA, 'fieldA-2', 'Correct context value for fieldA');
              equal($detailView.data('view-args').context.testListView[0].fieldB, 'fieldB-2', 'Correct context value for fieldB');
              equal($detailView.data('view-args').context.testListView[0].fieldC, 'fieldC-1', 'Correct context value for fieldC');
              equal($detailView.find('tr.fieldA .value').html(), 'fieldA-2', 'Correct table value for fieldA');
              equal($detailView.find('tr.fieldB .value').html(), 'fieldB-2', 'Correct table value for fieldB');
              equal($detailView.find('tr.fieldC .value').html(), 'fieldC-1', 'Correct table value for fieldC');

              equal($detailView.find('.action').size(), 1, 'Correct action count');
              equal($detailView.find('.action.updateDataTestAsync').size(), 1, 'updateDataTestAsync present');
              equal($detailView.find('.action.filteredAction').size(), 0, 'filteredAction not present');
              stop();
            }
          }
        },

        filteredAction: {
          label: 'filteredAction',
          action: function() {},
          messages: { notification: function() { return 'notification'; } }
        }
      },
      tabs: {
        test: {
          title: 'test',
          fields: [{
            fieldA: { label: 'fieldA' },
            fieldB: { label: 'fieldB' },
            fieldC: { label: 'fieldC' }
          }],
          dataProvider: function(args) {
            args.response.success({
              data: args.context.testListView[0],
              actionFilter: function(args) {
                if (args.context.testListView[0].state == 'on') {
                  return ['updateDataTestAsync', 'filteredAction'];
                }

                return ['updateDataTestAsync'];
              }
            });
          }
        }
      }
    };
    var $detailView = $('<div>');
    var $listView = $('<div>').addClass('list-view');
    var $listViewRow = $('<div>').data('json-obj', detailView.context.testListView[0]).appendTo($listView);
    var $cloudStackContainer = $('<div>').attr('cloudStack-container', true).data('cloudStack-args', {
      sections: {
        testListView: {}
      }
    }).appendTo('#qunit-fixture');

    $.fn.dataTable = function() { return this; };
    $.fn.listView = function(args1, args2) {
      if (args1 == 'replaceItem')
        args2.after(args2.$row.data('json-obj', args2.data));

      return this;
    };
    
    cloudStack.ui.notifications.add = function(notification, complete) {
      notification.poll({ complete: complete });
    };
    cloudStack.dialog.confirm = function(args) {
      args.action();
    };
    $detailView.data('list-view-row', $listViewRow);
    $detailView.detailView(detailView);

    equal($detailView.find('.action').size(), 2, 'Correct action count');
    equal($detailView.find('.action.updateDataTestAsync').size(), 1, 'updateDataTestAsync present');
    equal($detailView.find('.action.filteredAction').size(), 1, 'filteredAction present');
    
    stop();    
    $detailView.find('.action.updateDataTestAsync a').click();
    $detailView.data('view-args').actions.updateDataTestAsync.preAction = function(args) {
      start();
      equal($detailView.data('view-args').context.testListView[0].fieldA, 'fieldA-2', 'Pre-action: Correct context value for fieldA');
      equal($detailView.data('view-args').context.testListView[0].fieldB, 'fieldB-2', 'Pre-action: Correct context value for fieldB');
      equal($detailView.data('view-args').context.testListView[0].fieldC, 'fieldC-1', 'Pre-action: Correct context value for fieldC');
      ok(true, 'Pre-action called');

      return false;
    };
    $detailView.find('.action.updateDataTestAsync a').click();
  });

  test('Update context', function() {
    var detailView = {
      context: {
        listViewItemA: [
          { fieldA: 'fieldAContent' }
        ]
        // listViewItemB [not stored yet]
      },

      tabFilter: function(args) {
        start();
        ok($.isArray(args.context.listViewItemB), 'updateContext called before tabFilter');
        stop();

        return [];
      },

      // updateContext is executed every time a data provider is called
      updateContext: function(args) {
        start();
        ok(true, 'updateContext called');
        equal(args.context.listViewItemA[0].fieldA, 'fieldAContent', 'updateContext: Item A present in context');
        stop();

        return {
          listViewItemB: [
            { fieldB: 'fieldBContent' }
          ]
        };
      },

      tabs: {
        test: {
          title: 'test',
          fields: { fieldA: { label: 'fieldA'}, fieldB: { label: 'fieldB' }},
          dataProvider: function(args) {
            start();
            equal(args.context.listViewItemA[0].fieldA, 'fieldAContent', 'dataProvider: Item A present in context');
            equal(args.context.listViewItemB[0].fieldB, 'fieldBContent', 'dataProvider: Item B present in context');
          }
        }
      }
    };
    var $detailView = $('<div></div>');

    stop();
    $detailView.detailView(detailView);
  });
}(jQuery));
