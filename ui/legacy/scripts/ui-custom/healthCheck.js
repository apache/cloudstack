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

    cloudStack.uiCustom.healthCheck = function(args) {

        // Place outer args here as local variables
        // i.e, -- var dataProvider = args.dataProvider

        return function(args) {
            if (args.context.multiRules == undefined) { //LB rule is not created yet
                cloudStack.dialog.notice({
                    message: _l('Health Check can only be configured on a created LB rule')
                });
                return;
            }

            var formData = args.formData;
            var forms = $.extend(true, {}, args.forms);
            var topFieldForm, bottomFieldForm, $topFieldForm, $bottomFieldForm;
            var topfields = forms.topFields;

            var $healthCheckDesc = $('<div>' + _l('label.health.check.message.desc') + '</div>').addClass('health-check-description');
            var $healthCheckConfigTitle = $('<div><br><br>' + _l('label.health.check.configurations.options') + '</div>').addClass('health-check-config-title');
            var $healthCheckAdvancedTitle = $('<div><br><br>' + _l('label.health.check.advanced.options') + '</div>').addClass('health-check-advanced-title');

            var $healthCheckDialog = $('<div>').addClass('health-check');
            $healthCheckDialog.append($healthCheckDesc);
            $healthCheckDialog.append($healthCheckConfigTitle);
            var $loadingOnDialog = $('<div>').addClass('loading-overlay');

            var policyObj = null;
            var pingpath1 = '/';
            var responsetimeout1 = '2';
            var healthinterval1 = '5';
            var healthythreshold1 = '2';
            var unhealthythreshold1 = '1';

            $.ajax({
                url: createURL('listLBHealthCheckPolicies'),
                data: {
                    lbruleid: args.context.multiRules[0].id
                },
                async: false,
                success: function(json) {
                    if (json.listlbhealthcheckpoliciesresponse.healthcheckpolicies[0].healthcheckpolicy[0] != undefined) {
                        policyObj = json.listlbhealthcheckpoliciesresponse.healthcheckpolicies[0].healthcheckpolicy[0];
                        pingpath1 = policyObj.pingpath; //API bug: API doesn't return it
                        responsetimeout1 = policyObj.responsetime;
                        healthinterval1 = policyObj.healthcheckinterval;
                        healthythreshold1 = policyObj.healthcheckthresshold;
                        unhealthythreshold1 = policyObj.unhealthcheckthresshold;
                    }
                }
            });

            topFieldForm = cloudStack.dialog.createForm({
                context: args.context,
                noDialog: true, // Don't render a dialog, just return $formContainer
                form: {
                    title: '',
                    fields: {
                        pingpath: {
                            label: 'label.ping.path',
                            validation: {
                                required: false
                            },
                            defaultValue: pingpath1
                        }
                    }
                }
            });

            $topFieldForm = topFieldForm.$formContainer;
            $topFieldForm.appendTo($healthCheckDialog);

            $healthCheckDialog.append($healthCheckAdvancedTitle);

            bottomFieldForm = cloudStack.dialog.createForm({
                context: args.context,
                noDialog: true,
                form: {
                    title: '',
                    fields: {
                        responsetimeout: {
                            label: 'label.response.timeout.in.sec',
                            validation: {
                                required: false
                            },
                            defaultValue: responsetimeout1
                        },
                        healthinterval: {
                            label: 'label.health.check.interval.in.sec',
                            validation: {
                                required: false
                            },
                            defaultValue: healthinterval1
                        },
                        healthythreshold: {
                            label: 'label.healthy.threshold',
                            validation: {
                                required: false
                            },
                            defaultValue: healthythreshold1
                        },
                        unhealthythreshold: {
                            label: 'label.unhealthy.threshold',
                            validation: {
                                required: false
                            },
                            defaultValue: unhealthythreshold1
                        }
                    }
                }
            });

            $bottomFieldForm = bottomFieldForm.$formContainer;
            $bottomFieldForm.appendTo($healthCheckDialog);


            var buttons = [{
                text: _l('label.cancel'),
                'class': 'cancel',
                click: function() {
                    $healthCheckDialog.dialog('destroy');
                    $('.overlay').remove();
                }
            }];

            if (policyObj == null) { //policy is not created yet
                buttons.push({
                    text: _l('Create'),
                    'class': 'ok',
                    click: function() {
                        $loadingOnDialog.appendTo($healthCheckDialog);
                        var formData = cloudStack.serializeForm($healthCheckDialog.find('form'));
                        var data = {
                            lbruleid: args.context.multiRules[0].id,
                            pingpath: formData.pingpath,
                            responsetimeout: formData.responsetimeout,
                            intervaltime: formData.healthinterval,
                            healthythreshold: formData.healthythreshold,
                            unhealthythreshold: formData.unhealthythreshold
                        };

                        $.ajax({
                            url: createURL('createLBHealthCheckPolicy'),
                            data: data,
                            success: function(json) {
                                var jobId = json.createlbhealthcheckpolicyresponse.jobid;
                                var createLBHealthCheckPolicyIntervalId = setInterval(function() {
                                    $.ajax({
                                        url: createURL('queryAsyncJobResult'),
                                        data: {
                                            jobid: jobId
                                        },
                                        success: function(json) {
                                            var result = json.queryasyncjobresultresponse;
                                            if (result.jobstatus == 0) {
                                                return; //Job has not completed
                                            } else {
                                                clearInterval(createLBHealthCheckPolicyIntervalId);

                                                if (result.jobstatus == 1) {
                                                    cloudStack.dialog.notice({
                                                        message: _l('Health Check Policy has been created')
                                                    });
                                                    $loadingOnDialog.remove();
                                                    $healthCheckDialog.dialog('destroy');
                                                    $('.overlay').remove();
                                                } else if (result.jobstatus == 2) {
                                                    cloudStack.dialog.notice({
                                                        message: _s(result.jobresult.errortext)
                                                    });
                                                    $loadingOnDialog.remove();
                                                    $healthCheckDialog.dialog('destroy');
                                                    $('.overlay').remove();
                                                }
                                            }
                                        }
                                    });
                                }, g_queryAsyncJobResultInterval);
                            },

                            error: function(json) {

                                cloudStack.dialog.notice({
                                    message: parseXMLHttpResponse(json)
                                }); //Error message in the API needs to be improved
                                $healthCheckDialog.dialog('close');
                                $('.overlay').remove();
                            }

                        });
                    }
                });
            } else { //policy exists already
                buttons.push(
                    //Update Button (begin) - call delete API first, then create API
                    {
                        text: _l('Update'),
                        'class': 'ok',
                        click: function() {
                            $loadingOnDialog.appendTo($healthCheckDialog);

                            $.ajax({
                                url: createURL('deleteLBHealthCheckPolicy'),
                                data: {
                                    id: policyObj.id
                                },
                                success: function(json) {
                                    var jobId = json.deletelbhealthcheckpolicyresponse.jobid;
                                    var deleteLBHealthCheckPolicyIntervalId = setInterval(function() {
                                        $.ajax({
                                            url: createURL('queryAsyncJobResult'),
                                            data: {
                                                jobid: jobId
                                            },
                                            success: function(json) {
                                                var result = json.queryasyncjobresultresponse;
                                                if (result.jobstatus == 0) {
                                                    return; //Job has not completed
                                                } else {
                                                    clearInterval(deleteLBHealthCheckPolicyIntervalId);

                                                    if (result.jobstatus == 1) {
                                                        var formData = cloudStack.serializeForm($healthCheckDialog.find('form'));
                                                        var data = {
                                                            lbruleid: args.context.multiRules[0].id,
                                                            pingpath: formData.pingpath,
                                                            responsetimeout: formData.responsetimeout,
                                                            intervaltime: formData.healthinterval,
                                                            healthythreshold: formData.healthythreshold,
                                                            unhealthythreshold: formData.unhealthythreshold
                                                        };

                                                        $.ajax({
                                                            url: createURL('createLBHealthCheckPolicy'),
                                                            data: data,
                                                            success: function(json) {
                                                                var jobId = json.createlbhealthcheckpolicyresponse.jobid;
                                                                var createLBHealthCheckPolicyIntervalId = setInterval(function() {
                                                                    $.ajax({
                                                                        url: createURL('queryAsyncJobResult'),
                                                                        data: {
                                                                            jobid: jobId
                                                                        },
                                                                        success: function(json) {
                                                                            var result = json.queryasyncjobresultresponse;
                                                                            if (result.jobstatus == 0) {
                                                                                return; //Job has not completed
                                                                            } else {
                                                                                clearInterval(createLBHealthCheckPolicyIntervalId);

                                                                                if (result.jobstatus == 1) {
                                                                                    cloudStack.dialog.notice({
                                                                                        message: _l('Health Check Policy has been updated')
                                                                                    });
                                                                                    $loadingOnDialog.remove();
                                                                                    $healthCheckDialog.dialog('destroy');
                                                                                    $('.overlay').remove();
                                                                                } else if (result.jobstatus == 2) {
                                                                                    cloudStack.dialog.notice({
                                                                                        message: _s(result.jobresult.errortext)
                                                                                    });
                                                                                    $loadingOnDialog.remove();
                                                                                    $healthCheckDialog.dialog('destroy');
                                                                                    $('.overlay').remove();
                                                                                }
                                                                            }
                                                                        }
                                                                    });
                                                                }, g_queryAsyncJobResultInterval);
                                                            }
                                                        });
                                                    } else if (result.jobstatus == 2) {
                                                        cloudStack.dialog.notice({
                                                            message: _s(result.jobresult.errortext)
                                                        });
                                                        $loadingOnDialog.remove();
                                                        $healthCheckDialog.dialog('destroy');
                                                        $('.overlay').remove();
                                                    }
                                                }
                                            }
                                        });
                                    }, g_queryAsyncJobResultInterval);
                                }
                            });
                        }
                    }
                    //Update Button (end)
                    ,
                    //Delete Button (begin) - call delete API
                    {
                        text: _l('Delete'),
                        'class': 'delete',
                        click: function() {
                            $loadingOnDialog.appendTo($healthCheckDialog);

                            $.ajax({
                                url: createURL('deleteLBHealthCheckPolicy'),
                                data: {
                                    id: policyObj.id
                                },
                                success: function(json) {
                                    var jobId = json.deletelbhealthcheckpolicyresponse.jobid;
                                    var deleteLBHealthCheckPolicyIntervalId = setInterval(function() {
                                        $.ajax({
                                            url: createURL('queryAsyncJobResult'),
                                            data: {
                                                jobid: jobId
                                            },
                                            success: function(json) {
                                                var result = json.queryasyncjobresultresponse;
                                                if (result.jobstatus == 0) {
                                                    return; //Job has not completed
                                                } else {
                                                    clearInterval(deleteLBHealthCheckPolicyIntervalId);

                                                    if (result.jobstatus == 1) {
                                                        cloudStack.dialog.notice({
                                                            message: _l('Health Check Policy has been deleted')
                                                        });
                                                        $loadingOnDialog.remove();
                                                        $healthCheckDialog.dialog('destroy');
                                                        $('.overlay').remove();
                                                    } else if (result.jobstatus == 2) {
                                                        cloudStack.dialog.notice({
                                                            message: _s(result.jobresult.errortext)
                                                        });
                                                        $loadingOnDialog.remove();
                                                        $healthCheckDialog.dialog('destroy');
                                                        $('.overlay').remove();
                                                    }
                                                }
                                            }
                                        });
                                    }, g_queryAsyncJobResultInterval);
                                }
                            });
                        }
                    }
                    //Delete Button (end)
                );
            }

            $healthCheckDialog.dialog({
                title: _l('label.health.check.wizard'),
                width: 630,
                height: 600,
                draggable: true,
                closeonEscape: false,
                overflow: 'auto',
                open: function() {
                    $("button").each(function() {
                        $(this).attr("style", "left: 400px; position: relative; margin-right: 5px; ");
                    });

                    $('.ui-dialog .delete').css('left', '140px');

                },
                buttons: buttons
            }).closest('.ui-dialog').overlay();

        }
    }
}(jQuery, cloudStack));
