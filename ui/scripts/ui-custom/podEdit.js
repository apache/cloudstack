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
(function(cloudStack, $) {
  cloudStack.uiCustom.podEdit = function(args) {
    var $podEdit = $('<div></div>').addClass('pod-edit');
    var multiEditData = [];
    var totalIndex = 0;
    var form = cloudStack.dialog.createForm({
      noDialog: true,
      desc: '',
      form: {
        desc: '',
        fields: args.form.fields
      }
    });
    var $multi = $('<div></div>').multiEdit($.extend(true, {}, args.multiEdit, {
      context: args.context,
      noSelect: true,
      add: {
        label: 'label.add',
        action: function(args) {
          multiEditData.push($.extend(args.data, {
            index: totalIndex
          }));

          totalIndex++;
          args.response.success();
        }
      },
      actions: {
        destroy: {
          label: 'label.remove.rule',
          action: function(args) {
            multiEditData = $.grep(multiEditData, function(item) {
              return item.index != args.context.multiRule[0].index;
            });
            args.response.success();
          }
        }
      },
      dataProvider: function(args) {
        args.response.success({
          data: multiEditData
        });
      }
    }));

    $podEdit.append(form.$formContainer, $multi);

    return $podEdit;
  };
}(window.cloudStack, window.jQuery));