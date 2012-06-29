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
  module('Mutli-edit');

  test('Basic', function() {
    var multiEdit = {
      fields: {
        fieldA: { edit: true, label: 'fieldA' },
        fieldB: { edit: true, label: 'fieldB' },
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
    equal($multiEdit.find('div.multi-edit').size(), 1, 'Main container div correct');
    equal($multiEdit.find('.multi-edit form').size(), 1, 'Multi-edit has form');
    equal($multiEdit.find('.multi-edit form table.multi-edit').size(), 1, 'Form has table');
    equal($multiEdit.find('.multi-edit form table thead tr').size(), 1, 'Header present');
    equal($multiEdit.find('.multi-edit form table tbody tr').size(), 1, 'Form body present');
    equal($multiEdit.find('.multi-edit .data .data-body').size(), 1, 'Data body present');

    // Header items
    equal($multiEdit.find('.multi-edit form table thead th.fieldA[rel=fieldA]').html(), 'fieldA', 'fieldA has correct header');
    equal($multiEdit.find('.multi-edit form table thead th.fieldB[rel=fieldB]').html(), 'fieldB', 'fieldB has correct header');
    equal($multiEdit.find('.multi-edit form table thead th.add[rel=add]').html(), 'add', 'Add action column has correct header');

    // Form items
    equal($multiEdit.find('.multi-edit form table tbody td.fieldA[rel=fieldA] input[name=fieldA]').size(), 1, 'fieldA has correct input');
    equal($multiEdit.find('.multi-edit form table tbody td.fieldA[rel=fieldA] input[type=text]').size(), 1, 'fieldA has text-based input');
    equal($multiEdit.find('.multi-edit form table tbody td.fieldB[rel=fieldB] input[name=fieldB]').size(), 1, 'fieldB has correct input');
    equal($multiEdit.find('.multi-edit form table tbody td.fieldB[rel=fieldB] input[type=text]').size(), 1, 'fieldB has text-based input');
    equal($multiEdit.find('.multi-edit form table tbody td.add[rel=add] .button.add-vm').html(), 'addAction', 'Add action column has correct content');
  });
}(jQuery));
