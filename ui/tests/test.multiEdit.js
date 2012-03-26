(function($) {
  module('Mutli-edit');

  test('Basic', function() {
    var multiEdit = {
      fields: {
        fieldA: { label: 'fieldA' },
        fieldB: { label: 'fieldB' },
        add: { label: 'add', addButton: true }
      },
      add: {
        label: 'addAction',
        action: function() {}
      },
      dataProvider: function() {}
    };
    var $multiEdit = $('<div>');
    
    ok($multiEdit.multiEdit(multiEdit), 'Initialize multi-edit');
  });
}(jQuery));
