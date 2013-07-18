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
    $(window).bind('cloudStack.ready', function() {
        var $header = $('#header .controls');
        var $zoneFilter = $('<div>').addClass('zone-filter');
        var $zoneTypeSelect = $('<select>').append(
            $('<option>').attr('value', '').html(_l('All zones')),
            $('<option>').attr('value', 'Basic').html(_l('Basic')),
            $('<option>').attr('value', 'Advanced').html(_l('Advanced'))
        );
        var $label = $('<label>').html('Zone type:');

        $zoneFilter.append($label, $zoneTypeSelect);
        $zoneFilter.insertAfter($header.find('.project-switcher'));
        $zoneTypeSelect.change(function() {
            cloudStack.context.zoneType = $zoneTypeSelect.val();

            // Go to default/start page (dashboard)
            $('#breadcrumbs .home').click();
        });
    });
}(jQuery, cloudStack));
