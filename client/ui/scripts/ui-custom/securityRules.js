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
(function($, cloudStack) {
    cloudStack.uiCustom.securityRules = function(args) {
        var multiEdit = args;

        return function(args) {
            var context = args.context;
            var $multi = $('<div>').addClass('security-rules').multiEdit(
                $.extend(true, {}, multiEdit, {
                    context: context
                })
            );
            var $fields = $multi.find('form table').find('th, td');
            var $accountFields = $fields.filter(function() {
                return $(this).hasClass('accountname') ||
                    $(this).hasClass('securitygroupname');
            });
            var $cidrFields = $fields.filter(function() {
                return $(this).hasClass('cidr');
            });

            $multi.prepend(
                $('<div>').addClass('add-by')
                .append($('<span>').html('Add by:'))
                .append(
                    $('<div>').addClass('selection')
                    .append(
                        $('<input>').attr({
                            type: 'radio',
                            name: 'add-by',
                            checked: 'checked'
                        }).click(function() {
                            $accountFields.hide();
                            $cidrFields.show();

                            return true;
                        }).click()
                    )
                    .append($('<label>').html('CIDR'))
                    .append(
                        $('<input>').attr({
                            type: 'radio',
                            name: 'add-by'
                        }).click(function() {
                            $accountFields.show();
                            $cidrFields.hide();

                            return true;
                        })
                    )
                    .append($('<label>').html('Account'))
                )
            );

            return $multi;
        };
    };
})(jQuery, cloudStack);
