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
    $.widget("cloudStack.toolTip", {
        _init: function(args) {
            var context = this.options.context;
            var dataProvider = this.options.dataProvider;
            var actions = this.options.actions;
            var docID = this.options.docID;
            var text = cloudStack.docs[docID].desc;
            var $tooltip = $('<div>').addClass('tooltip-box');
            var $text = $('<p>').html(text).appendTo($tooltip);
            var $container = $('#cloudStack3-container');

            $tooltip.appendTo($container);

            if (this.options.mode == 'hover') {
                $(this.element).hover(hoverHandler, outHandler);
            } else if (this.options.mode == 'focus') {
                $(this.element).focus(hoverHandler);
                $(this.element).blur(outHandler);
            } else if (this.options.mode == 'manual') {}
            
            $(this.element).data('toolTipOptions', this.options);
            $(this.element).data('$tooltip', $tooltip);

            // Add arrow
            $tooltip.append($('<div></div>').addClass('arrow'));

            $tooltip.hide();
        },

        show: function() {
            var o = this.options;

            if (o.mode == 'manual') {
                prepare(this.element, o);
            }

            $(o.toolTip).show();
        },

        hide: function() {
            var o = this.options;
            $(o.toolTip).hide();
        }
    });

    $.extend($.cloudStack.toolTip, {
        defaults: {
            toolTip: '',
            onShow: function(sender) {
                //Flipping arrow and text

                var $tooltip = $('.tooltip-box');

                //Switch styles based on how close to viewport border

                if ($(window).width() - sender.target.offset().left <= $tooltip.width()) {

                    $('.tooltiptextleft', $tooltip).removeClass('tooltiptextleft').addClass('tooltiptextright');
                    $('.tooltiparrowleft', $tooltip).removeClass('tooltiparrowleft').addClass('tooltiparrowright');

                } else {
                    $('.tooltiptextright', $tooltip).removeClass('tooltiptextright').addClass('tooltiptextleft');
                    $('.tooltiparrowright', $tooltip).removeClass('tooltiparrowright').addClass('tooltiparrowleft');
                }

            },
            onHide: undefined,
            mode: 'hover',
            // provide a speed for the animation
            speed: 1000,
            // provide a period for the popup to keep showing
            period: 2000,
            // default the animation algorithm to the basic slide
            animation: 'slide'
        },
        animations: {
            slide: function(e, options) {

            },
            fade: function(e, options) {

            }
        }
    });

    function hoverHandler(event) {
        //Element who raised the event
        var $this = $(this);

        var toolTipOptionObject = $this.data('toolTipOptions');
        

        //Helper functon for Positioning and Calling Callback function
        prepare($this, toolTipOptionObject);

        //Call Show method of the tooltip Widget,
        //Show method should play on any required animations
        $.data(this, '$tooltip').show();
    };

    function outHandler(event) {
        //Element who raised the event
        var $this = $(this);
        
        //Fetch Options
        var toolTipOptionObject = $this.data('toolTipOptions');

        //Get tooptip Element
        var $tooltip = $(toolTipOptionObject.toolTip);

        //If call back method defined, initiate the call
        if (toolTipOptionObject.onHide) {
            toolTipOptionObject.onHide.call(this, {
                target: $(this)
            });
        }

        //Call Hide method of the tooltip Widget,
        //Hide method should play on any required animations
        $.data(this, '$tooltip').hide();
    };

    function prepare(jObj, options) {
        var $tooltip = $(options.tooltip);
        var element = options.attachTo ?
            jObj.closest(options.attachTo) : jObj;
        var offset = element.offset();

        var left = offset.left + element.width();
        var top = offset.top - 5;

        if (options.onShow) {
            options.onShow.call(this, {
                target: jObj
            });
        }

        if ($(window).width() - offset.left <= $tooltip.width()) {
            left = offset.left - $tooltip.width();
        } else {
            left += 35;
        }
        $tooltip.css({
            position: 'absolute',
            top: top + 'px',
            left: left + 'px'
        });

        // Fix overlay
        setTimeout(function() {
            $('.tooltip-box').css( "zIndex", $(':ui-dialog').css("zIndex") + 10);
        });

    };


})(jQuery, cloudStack);
