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
    cloudStack.lbStickyPolicy = {
        dialog: function(args) {
            return function(args) {
                var success = args.response.success;
                var context = args.context;

                var network;
                if ('vpc' in args.context) { //from VPC section
                    var data = {
                        listAll: true,
                        supportedservices: 'Lb'
                    };
                    if (args.context.ipAddresses[0].associatednetworkid == null) {
                        $.extend(data, {
                            vpcid: args.context.vpc[0].id
                        });
                    } else {
                        $.extend(data, {
                            id: args.context.ipAddresses[0].associatednetworkid
                        });
                    }

                    $.ajax({
                        url: createURL("listNetworks"), //check whether the VPC has a network including Lb service
                        data: data,
                        async: false,
                        success: function(json) {
                            var items = json.listnetworksresponse.network;
                            if (items != null && items.length > 0) {
                                network = items[0];
                            }
                        }
                    });
                } else { //from Guest Network section
                    network = args.context.networks[0];
                }

                var $item = args.$item;

                var lbService = $.grep(network.service, function(service) {
                    return service.name == 'Lb';
                })[0];

                var stickinessCapabilities = JSON.parse($.grep(
                    lbService.capability,
                    function(capability) {
                        return capability.name == 'SupportedStickinessMethods';
                    }
                )[0].value);

                var baseFields = {
                    stickyName: {
                        label: 'label.sticky.name',
                        validation: {
                            required: true
                        }
                    }
                };

                $.map(
                    $.map(
                        stickinessCapabilities,
                        function(c) {
                            return c.paramlist;
                        }
                    ),
                    function(p) {
                        baseFields[p.paramname] = {
                            label: _l('label.sticky.' + p.paramname),
                            isHidden: true,
                            isBoolean: p.isflag,
                            validation: {
                                required: p.required
                            }
                        };
                    }
                );

                var conditionalFields = {
                    methodname: {
                        label: 'label.stickiness.method',
                        select: function(args) {
                            var $select = args.$select;
                            var $form = $select.closest('form');
                            var stickyOptions = [];

                            stickinessCapabilities.push({
                                methodname: 'None',
                                paramlist: []
                            });
                            $(stickinessCapabilities).each(function() {
                                var stickyCapability = this;

                                stickyOptions.push({
                                    id: stickyCapability.methodname,
                                    description: stickyCapability.methodname
                                });
                            });

                            stickyOptions = stickyOptions.sort(function() {
                                return this.id != 'None';
                            });

                            args.response.success({
                                data: stickyOptions
                            }, 500);

                            $select.change(function() {
                                var value = $select.val();
                                var showFields = [];
                                var targetMethod = $.grep(stickinessCapabilities, function(stickyCapability) {
                                    return stickyCapability.methodname == value;
                                })[0];
                                var visibleParams = $.map(targetMethod.paramlist, function(param) {
                                    return param.paramname;
                                });

                                $select.closest('.form-item').siblings('.form-item').each(function() {
                                    var $field = $(this);
                                    var id = $field.attr('rel');

                                    if ($.inArray(id, visibleParams) > -1) {
                                        $field.css('display', 'inline-block');
                                        $field.attr('sticky-method', value);
                                    } else {
                                        $field.hide();
                                        $field.attr('sticky-method', null);
                                    }
                                });

                                // Name always is required
                                if ($select.val() != 'None') {
                                    $select.closest('.form-item').siblings('.form-item[rel=stickyName]')
                                        .css('display', 'inline-block');
                                }

                                $select.closest(':ui-dialog').dialog('option', 'position', 'center');
                            });
                        }
                    }
                };

                var fields = $.extend(conditionalFields, baseFields);

                if (args.data) {
                    var populatedFields = $.map(fields, function(field, id) {
                        return id;
                    });

                    $(populatedFields).each(function() {
                        var id = this;
                        var field = fields[id];
                        var dataItem = args.data[id];

                        if (field.isBoolean) {
                            field.isChecked = dataItem ? true : false;
                        } else {
                            field.defaultValue = dataItem;
                        }
                    });
                }

                cloudStack.dialog.createForm({
                    form: {
                        title: 'label.configure.sticky.policy',
                        desc: 'label.please.complete.the.following.fields',
                        fields: fields
                    },
                    after: function(args) {
                        // Remove fields not applicable to sticky method
                        args.$form.find('.form-item:hidden').remove();

                        var data = cloudStack.serializeForm(args.$form);

                        /* $item indicates that this is an existing sticky rule;
               re-create sticky rule with new parameters */
                        if ($item) {
                            var $loading = $('<div>').addClass('loading-overlay');

                            $loading.prependTo($item);
                            cloudStack.lbStickyPolicy.actions.recreate(
                                $item.data('multi-custom-data').id,
                                $item.data('multi-custom-data').lbRuleID,
                                data,
                                function() { // Complete
                                    $(window).trigger('cloudStack.fullRefresh');
                                },
                                function(error) { // Error
                                    $(window).trigger('cloudStack.fullRefresh');
                                }
                            );
                        } else {
                            success({
                                data: data
                            });
                        }
                    }
                });
            };
        },

        actions: {
            add: function(lbRuleID, data, complete, error) {
                var stickyURLData = '';
                var stickyParams = $.map(data, function(value, key) {
                    return key;
                });

                var notParams = ['methodname', 'stickyName'];

                var index = 0;
                $(stickyParams).each(function() {
                    var param = '&param[' + index + ']';
                    var name = this.toString();
                    var value = data[name];

                    if (!value || $.inArray(name, notParams) > -1) return true;
                    if (value == 'on') value = true;

                    stickyURLData += param + '.name=' + name + param + '.value=' + value;

                    index++;

                    return true;
                });

                $.ajax({
                    url: createURL('createLBStickinessPolicy' + stickyURLData),
                    data: {
                        lbruleid: lbRuleID,
                        name: data.stickyName,
                        methodname: data.methodname
                    },
                    success: function(json) {
                        cloudStack.ui.notifications.add({
                                desc: 'message.desc.add.new.lb.sticky.rule',
                                section: 'Network',
                                poll: pollAsyncJobResult,
                                _custom: {
                                    jobId: json.createLBStickinessPolicy.jobid
                                }
                            },
                            complete, {},
                            error, {}
                        );
                    },
                    error: function(json) {
                        complete();
                        cloudStack.dialog.notice({
                            message: parseXMLHttpResponse(json)
                        });
                    }
                });
            },
            'delete': function(stickyRuleID, complete, error) {
                $.ajax({
                    url: createURL('deleteLBStickinessPolicy'),
                    data: {
                        id: stickyRuleID
                    },
                    success: function(json) {
                        cloudStack.ui.notifications.add({
                                desc: 'Remove previous LB sticky rule',
                                section: 'Network',
                                poll: pollAsyncJobResult,
                                _custom: {
                                    jobId: json.deleteLBstickinessrruleresponse.jobid
                                }
                            },
                            complete, {},
                            error, {}
                        );
                    },
                    error: function(json) {
                        complete();
                        cloudStack.dialog.notice({
                            message: parseXMLHttpResponse(json)
                        });
                    }
                });
            },
            recreate: function(stickyRuleID, lbRuleID, data, complete, error) {
                var addStickyPolicy = function() {
                    cloudStack.lbStickyPolicy.actions.add(
                        lbRuleID,
                        data,
                        complete,
                        error
                    );
                };

                // Delete existing rule
                if (data.methodname !== 'None') {
                    addStickyPolicy();
                } else {
                    cloudStack.lbStickyPolicy.actions['delete'](stickyRuleID, complete, error);
                }
            }
        }
    };
}(jQuery, cloudStack));
