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
    /**
     * Create a dark overlay, for modal dialogs, etc.
     */
    $.fn.overlay = function(args) {
        var $topElem = this;
        var $overlay = $('<div class="overlay">').hide().appendTo('html body').css({
            position: 'absolute',
            background: 'black',
            opacity: 0.5,
            width: '100%',
            height: '100%',
            top: $(window).scrollTop(),
            left: 0,
            zIndex: $topElem.css('z-index') - 1
        }).show();

        // Events
        $overlay.click(function(event) {
            if (!args || !args.closeAction) return false;

            args.closeAction();
            $overlay.fadeOut(function() {
                $overlay.remove();
            });
        });

        return this;
    };

    $(window).bind('resize scroll', function() {
        $('.overlay').css('top', $(window).scrollTop());
    });
})(window.jQuery);
