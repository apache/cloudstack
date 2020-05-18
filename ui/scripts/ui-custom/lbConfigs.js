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

    cloudStack.uiCustom.lbConfigs = function(args) {

        // Place outer args here as local variables
        // i.e, -- var dataProvider = args.dataProvider

        return function(args) {
            var context = args.context;

            if (args.context.multiRules == undefined) { //LB rule is not created yet
                cloudStack.dialog.notice({
                    message: _l('LB configs can only be configured on a created LB rule')
                });
                return;
            }

            var lbId = args.context.multiRules[0].id;

            var portMultiEdit = function(args) {
                return $('<div>').multiEdit({
                    context: context,
                    noSelect: true,
                    noHeaderActionsColumn: true,
                    fields: {
                        'displayname': {
                            label: 'label.name',
                            select: function(args) {
                                var config_list = [];
                                $.ajax({
                                    url: createURL('listLoadBalancerConfigs'),
                                    data: {
                                        listAll: true,
                                        scope: 'LoadBalancerRule',
                                        loadbalancerid: lbId
                                    },
                                    dataType: 'json',
                                    async: false,
                                    success: function(json) {
                                        var configs = json.listloadbalancerconfigsresponse.loadbalancerconfig ?
                                            json.listloadbalancerconfigsresponse.loadbalancerconfig : [];
                                        $(configs).each(function() {
                                            config_list[this.name] = {
                                                desc: this.description,
                                                defaultvalue: this.defaultvalue
                                            }
                                        });

                                        args.response.success({
                                            data: $.map(configs, function(config) {
                                                return {
                                                    name: config.name,
                                                    description: config.name
                                                };
                                            })
                                        });
                                    }
                                });
                                args.$select.change(function() {
                                    var name = $(this).children(':selected').val();
                                    var desc = config_list[name].desc;
                                    $(this).parent().parent().find('.description').html(desc);
                                    var dvalue = config_list[name].defaultvalue;
                                    $(this).parent().parent().find('.defaultvalue').html(dvalue);
                                });
                            }
                        },
                        'description': {
                            edit: false,
                            display: true,
                            label: 'label.description',
                            isOptional: true
                        },
                        'defaultvalue': {
                            edit: false,
                            display: true,
                            label: 'default value',
                            isOptional: true
                        },
                        'value': {
                            edit: true,
                            label: 'value',
                            isOptional: false
                        },
                        'add-rule': {
                            label: 'label.add',
                            addButton: true
                        }
                    },
                    add: {
                        label: 'label.add',
                        action: function(args) {
                            var data = {
                                scope: 'LoadBalancerRule',
                                name: args.data.displayname,
                                value: args.data.value,
                                forced: true,
                                loadbalancerid: lbId
                            };

                            $.ajax({
                                url: createURL('createLoadBalancerConfig'),
                                data: data,
                                dataType: 'json',
                                async: true,
                                success: function(json) {
                                    var jobId = json.createloadbalancerconfigresponse.jobid;

                                    args.response.success({
                                        _custom: {
                                            jobId: jobId
                                        },
                                        notification: {
                                            label: 'Added load balancer config ' + args.data.displayname,
                                            poll: pollAsyncJobResult
                                        }
                                    });
                                },
                                error: function(json) {
                                    args.response.error(parseXMLHttpResponse(json));
                                }
                            });
                        }
                    },
                    actions: {
                        destroy: {
                            label: 'Remove load balancer config',
                            action: function(args) {
                                $.ajax({
                                    url: createURL('deleteLoadBalancerConfig'),
                                    data: {
                                        id: args.context.multiRule[0].id
                                    },
                                    dataType: 'json',
                                    async: true,
                                    success: function(data) {
                                        var jobID = data.deleteloadbalancerconfigresponse.jobid;

                                        args.response.success({
                                            _custom: {
                                                jobId: jobID
                                            },
                                            notification: {
                                                label: 'Removed load balancer config ' + args.context.multiRule[0].displayname,
                                                poll: pollAsyncJobResult
                                            }
                                        });
                                    },
                                    error: function(json) {
                                        args.response.error(parseXMLHttpResponse(json));
                                    }
                                });
                            }
                        }
                    },
                    ignoreEmptyFields: true,
                    dataProvider: function(args) {
                        $.ajax({
                            url: createURL('listLoadBalancerConfigs'),
                            data: {
                                scope: 'LoadBalancerRule',
                                loadbalancerid: lbId
                            },
                            dataType: 'json',
                            async: false,
                            success: function(json) {
                                var configs = json.listloadbalancerconfigsresponse.loadbalancerconfig ?
                                    json.listloadbalancerconfigsresponse.loadbalancerconfig : [];


                                args.response.success({
                                    data: $.map(configs, function(config) {
                                        $.extend(config, {
                                            displayname: config.name
                                        });
                                        if (config.defaultvalue == "") {
                                            $.extend(config, {
                                                defaultvalue: "<null>"
                                            });
                                        }
                                        return config;
                                    })
                                });
                            }
                        });
                    }
                });
            };

            var $lbConfigsDesc = $('<div>' + _l('label.load.balancer') + ' ' + _l('label.configuration') + '</div>');

            var $lbConfigsDialog = $('<div>').addClass('lb-configs');
            $lbConfigsDialog.append($lbConfigsDesc);
            var $loadingOnDialog = $('<div>').addClass('loading-overlay');
            $lbConfigsDialog.append(portMultiEdit(context));

            var lbConfigsDialog = $lbConfigsDialog.dialog({
                title: _l('label.load.balancer') + ' ' + _l('label.configuration'),
                width: 1000,
                height: 600,
                draggable: true,
                closeonEscape: false,
                overflow: 'auto',
                open: function() {
                    $("button").each(function() {
                        $(this).attr("style", "left: 400px; position: relative; margin-right: 5px; ");
                    });
                },
                buttons: [{
                    text: _l('label.done'),
                    'class': 'ok',
                    click: function() {
                        $lbConfigsDialog.dialog('destroy');
                        $('.overlay').remove();
                    }
                }]
            });

            cloudStack.applyDefaultZindexAndOverlayOnJqueryDialogAndRemoveCloseButton($lbConfigsDialog);
        }
    }
}(jQuery, cloudStack));
