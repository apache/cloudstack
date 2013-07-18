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
    cloudStack.ui.event = {
        // Attach element to specific event type
        elem: function(widget, elem, $elem, extraData) {
            // Setup DOM metadata
            var data = {
                cloudStack: {}
            };
            data.cloudStack[widget] = {
                elem: elem
            };
            if (extraData) $.extend(data.cloudStack[widget], extraData);

            return $elem
                .addClass('cloudStack-elem')
                .addClass(widget)
                .data(data);
        },

        // Create widget-based event
        bind: function(widget, events) {
            return function(event) {
                var $target = $(event.target);
                var $widget, $elem;
                var data, elem;

                $elem = $target.closest('.cloudStack-elem.' + widget);
                if (!$elem.size())
                    return true;

                $widget = $('.cloudStack-widget.' + widget);
                data = $elem.data('cloudStack')[widget];
                elem = data.elem;

                events[elem]($elem, $widget, data);

                return false;
            };
        },

        // Trigger CloudStack UI event (cloudStack.*)
        call: function(eventName, data) {
            $(window).trigger('cloudStack.' + eventName, data);
        }
    };
})(jQuery, cloudStack);
