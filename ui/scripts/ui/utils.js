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
    // General utils
    cloudStack.serializeForm = function($form, options) {
        if (!options) options = {};

        var data = {};

        $($form.serializeArray()).each(function() {
            var dataItem = data[this.name];
            var value = _s(this.value.toString());

            if (options.escapeSlashes) {
                value = value.replace(/\//g, '__forwardSlash__');
            }

            if (!dataItem) {
                data[this.name] = value;
            } else if (dataItem && !$.isArray(dataItem)) {
                data[this.name] = [dataItem, value];
            } else if ($.isArray(dataItem)) {
                dataItem.push(value);
            }
        });

        return data;
    };

    // Even/odd row handling
    cloudStack.evenOdd = function($container, itemSelector, args) {
        var even = false;

        $container.find(itemSelector).each(function() {
            var $elem = $(this);

            if (even) {
                even = false;
                args.odd($elem);
            } else {
                even = true;
                args.even($elem);
            }
        });
    };

    /**
     * Localization -- shortcut _l
     *
     * Takes string and runs through localization function -- if no code
     * exists or function isn't present, return string as-is
     */
    cloudStack.localize = window._l = function(str) {
        var localized = cloudStack.localizationFn ?
            cloudStack.localizationFn(str) : null;

        return localized ? localized : str;
    };

    /**
     * Sanitize user input (HTML Encoding) -- shortcut _s
     *
     * Strip unwanted characters from user-based input
     */
    cloudStack.sanitize = window._s = function(value) {
        if (typeof(value) == "number") {
            //alert("number does not need to be sanitized. Only string needs to be sanitized.");
            return value;
        } else if (typeof(value) == "boolean") {
            //alert("boolean does not need to be sanitized. Only string needs to be sanitized.");
            return value;
        } else if (typeof(value) == "object") {
            //alert("object cant not be sanitized. Only string can be sanitized.");
            return value;
        } else if (typeof(value) == null || typeof(value) == "undefined") {
            return '';
        }

        var sanitized = value
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");

        return sanitized;
    };

    /**
     * Reverse sanitization (HTML Decoding)
     */
    cloudStack.sanitizeReverse = function(value) {
        var reversedValue = value
            .replace(/&amp;/g, "&")
            .replace(/&lt;/g, "<")
            .replace(/&gt;/g, ">");

        return reversedValue;
    };

    /**
     * If the str.length is > maxLen,
     * then concatenate and add '...' to the end of the string
     */
    cloudStack.concat = function(str, maxLen) {
        if (str.length > maxLen) {
            return str.substr(0, maxLen) + '...';
        } else {
            return str;
        }
    };

    /**
     * Localize validator messages
     */
    cloudStack.localizeValidatorMessages = function() {
        $.extend($.validator.messages, {
            required: _l('message.validate.fieldrequired'),
            remote: _l('message.validate.fixfield'),
            email: _l('message.validate.email.address'),
            url: _l('message.validate.URL'),
            date: _l('message.validate.date'),
            dateISO: _l('message.validate.date.ISO'),
            number: _l('message.validate.number'),
            digits: _l('message.validate.digits'),
            creditcard: _l('message.validate.creditcard'),
            equalTo: _l('message.validate.equalto'),
            accept: _l('message.validate.accept'),
            maxlength: $.validator.format(_l('message.validate.maxlength')),
            minlength: $.validator.format(_l('message.validate.minlength')),
            rangelength: $.validator.format(_l('message.validate.range.length')),
            range: $.validator.format(_l('message.validate.range')),
            max: $.validator.format(_l('message.validate.max')),
            min: $.validator.format(_l('messgae.validate.min'))
        });
    };
})(jQuery, cloudStack);
