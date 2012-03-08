(function($) {
  module('List view', {
    setup: function() {
      $.fx.off = true;
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
}(jQuery)); 
