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
    equal($listView.find('.toolbar .text-search .button.search').size(), 1, 'Toolbar has search button');
  });
}(jQuery)); 
