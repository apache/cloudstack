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

(function() {
    var listView = function(args) {
        var basicArgs = {
            listView: {
                fields: {},
                dataProvider: function() {}
            }
        };

        return $('<div>').listView(
            $.extend(true, {}, basicArgs, args)
        ).find('.list-view');
    };

    module('List view', {
        setup: function() {
            window.pageSize = 20;
        },
        teardown: function() {
            delete window.pageSize;
        }
    });

    test('Basic', function() {
        var $listView = listView();
        var $toolbar = $listView.find('> .toolbar');
        var $table = $listView.find('> .data-table');

        equal($listView.size(), 1, 'List view present');
        equal($toolbar.size(), 1, 'Toolbar present');
        equal($table.size(), 1, 'Data table div present');
        equal($table.find('> .fixed-header table thead tr').size(), 1, 'Fixed header present');
        equal($table.find('> table.body tbody').size(), 1, 'Body table present');
    });

    test('Fields: basic', function() {
        var $listView = listView({
            listView: {
                fields: {
                    fieldA: { label: 'TestFieldA' }
                }
            }
        });
        var $fields = $listView.find('.fixed-header table thead tr th');

        equal($fields.size(), 1, 'Column present');
        ok($fields.hasClass('fieldA'), 'Has ID as classname');
        equal($fields.html(), 'TestFieldA', 'Has correct label');
    });

    test('Fields: n columns', function() {
        var testFields = {
            fieldA: { label: 'TestFieldA' },
            fieldB: { label: 'TestFieldB' },
            fieldC: { label: 'TestFieldC' }
        };

        var $listView = listView({
            listView: {
                fields: testFields
            }
        });
        var $fields = $listView.find('.fixed-header table thead tr th');

        $.each(testFields, function(k, v) {
            var $field = $fields.filter('.' + k);

            equal($field.size(), 1, k + '-> Column present');
            equal($field.html(), v.label, k + '-> Has correct label');
        });
    });
}());
