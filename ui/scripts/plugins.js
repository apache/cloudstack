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
(function($, cloudStack, require) {
    if (!cloudStack.pluginAPI) {
        cloudStack.pluginAPI = {};
    }

    var loadCSS = function(path) {
        if (document.createStyleSheet) {
            // IE-compatible CSS loading
            document.createStyleSheet(path);
        } else {
            var $link = $('<link>');

            $link.attr({
                rel: 'stylesheet',
                type: 'text/css',
                href: path
            });

            $('html head').append($link);
        }
    };

    $.extend(cloudStack.pluginAPI, {
        ui: {
            pollAsyncJob: pollAsyncJobResult,
            apiCall: function(command, args) {
                $.ajax({
                    url: createURL(command),
                    data: args.data,
                    success: args.success,
                    error: function(json) {
                        args.error(parseXMLHttpResponse(json));
                    }
                });
            },
            addSection: function(section) {
                cloudStack.sections[section.id] = $.extend(section, {
                    customIcon: 'plugins/' + section.id + '/icon.png',
                    isPlugin: true
                });
            },
            extend: function(obj) {
                $.extend(true, cloudStack, obj);
            }
        }
    });

    cloudStack.sections.plugins = {
        title: 'label.plugins',
        show: cloudStack.uiCustom.pluginListing
    };

    var loadedPlugins = 0;
    var pluginTotal = cloudStack.plugins.length + cloudStack.modules.length;

    // Load
    $(['modules', 'plugins']).each(function() {
        var type = this;
        var paths = $(cloudStack[type]).map(function(index, id) {
            return type + '/' + id + '/' + id;
        }).toArray();

        // Load modules
        require(
            paths,
            function() {
                $(cloudStack[type]).map(function(index, id) {
                    var basePath = type + '/' + id + '/';
                    var css = basePath + id + '.css';
                    var configJS = type == 'plugins' ? basePath + 'config' : null;

                    var pluginDictionary;
                    var locale;
                    if (type == 'plugins') {
                        pluginDictionary = basePath + 'dictionary';
                        locale = $.cookie('lang');
                    }

                    if (configJS) {
                        // Load config metadata
                        require([configJS]);
                    }

                    // Execute module
                    var target = cloudStack[type][id];

                    if ($.isArray(target)) { // Load additional JS includes
                        pluginTotal += target[0].length;
                        require(
                            $(target[0]).map(function(index, val) {
                                return basePath + val;
                            }).toArray(),
                            function() {
                                loadedPlugins = loadedPlugins + target[0].length;
                                target[1](
                                    $.extend(true, {}, cloudStack.pluginAPI, {
                                        pluginAPI: {
                                            extend: function(api) {
                                                cloudStack.pluginAPI[id] = api;
                                            }
                                        }
                                    })
                                );
                            });
                    } else {
                        target(
                            $.extend(true, {}, cloudStack.pluginAPI, {
                                pluginAPI: {
                                    extend: function(api) {
                                        cloudStack.pluginAPI[id] = api;
                                    }
                                }
                            })
                        );
                    }

                    if (pluginDictionary) {
                        // Callback for extending the dictionary with new entries
                        var addToDictionary = function() {
                            var additions = cloudStack[type][id].dictionary;
                            $.extend(dictionary, additions);
                        };

                        // Only add a suffix if the locale is defined and not english/US
                        var suffix = '';
                        if (locale && (locale != '' && locale != 'en' & locale != 'en_US'))
                            suffix = '_'+locale;

                        // Attempt to load the locale's dictionary
                        require([pluginDictionary+suffix], addToDictionary, function() {
                            // If the load failed and there was no suffix, don't try again
                            if (!suffix)
                                return;

                            // Otherwise try to load the default dictionary
                            require([pluginDictionary], addToDictionary);
                        });
                    }

                    loadedPlugins = loadedPlugins + 1;

                    loadCSS(css);
                });
            }
        );
    });

    var loadTimer = setInterval(function() {
        if (loadedPlugins === pluginTotal) {
            clearInterval(loadTimer);
            $(window).trigger('cloudStack.pluginReady');
        }
    }, 100);
}(jQuery, cloudStack, require));
