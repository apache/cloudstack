(function($, cloudStack) {
    /**
     * Serialize form data as object
     */
    var getData = function($wizard, options) {
        if (!options) options = {};

        var $forms = $wizard.find('form').filter(function() {
            return !options.all ? !$(this).closest('.multi-edit').length : true;
        });

        var $addRoutingPolicyFromTerm = $wizard.find(
            '.steps .add-routing-policy-from-term .data-body .data-item');
        var $addRoutingPolicyThenTerm = $wizard.find(
            '.steps .add-routing-policy-then-term .data-body .data-item');
        var groupedForms = {};

        if (options.all) {
            return cloudStack.serializeForm($forms, {
                escapeSlashes: true
            });
        }

        // Group form fields together, by form ID
        $forms.each(function() {
            var $form = $(this);
            var id = $form.attr('rel');

            if (!id) return true;

            groupedForms[id] = cloudStack.serializeForm($form, {
                escapeSlashes: true
            });

            return true;
        });

        groupedForms.routingPolicyFromTerm = $.map(
            $addRoutingPolicyFromTerm,
            function(routingPolicyFromTermItem) {
                var $routingPolicyFromTermItem = $(routingPolicyFromTermItem);
                var routingPolicyFromTermData = {};
                var fields = [
                    'prefix',
                    'prefixType'
                ];

                $(fields).each(function() {
                    routingPolicyFromTermData[this] =
                        $routingPolicyFromTermItem.find('td.' + this + ' span').html();
                });

                return routingPolicyFromTermData;
            }
        );

        groupedForms.routingPolicyThenTerm = $.map(
            $addRoutingPolicyThenTerm,
            function(routingPolicyThenTermItem) {
                var $routingPolicyThenTermItem = $(routingPolicyThenTermItem);
                var routingPolicyThenTermData = {};
                var fields = [
                    'termType',
                    'termValue',
                    'termAction'
                ];

                $(fields).each(function() {
                    routingPolicyThenTermData[this] =
                        $routingPolicyThenTermItem.find('td.' + this + ' span').html();
                });

                return routingPolicyThenTermData;
            }
        );
        // Hack to fix forward slash JS error
        $.each(groupedForms, function(key1, value1) {
            $.each(value1, function(key2, value2) {
                if (typeof value2 == 'string') {
                    groupedForms[key1][key2] = value2.replace(/__forwardSlash__/g, '\/');
                }
            });
        });

        return groupedForms;
    };

    /**
     * Generate dynamic form, based on ID of form object given
     */
    var makeForm = function(args, id, formState) {
        var form = cloudStack.dialog.createForm({
            noDialog: true,
            context: $.extend(true, {}, cloudStack.context, {
                terms: [formState]
            }),
            form: {
                title: '',
                desc: '',
                fields: args.forms[id].fields
            },
            after: function(args) {}
        });

        var $form = form.$formContainer.find('form');

        // Cleanup form to follow routing policy wizard CSS naming
        $form.attr('rel', id);
        $form.find('input[type=submit]').remove();
        $form.find('.form-item').addClass('field').removeClass('form-item');
        $form.find('label.error').hide();
        $form.find('.form-item .name').each(function() {
            $(this).html($(this).find('label'));
        });
        $form.find('label[for]').each(function() {
            var forAttr = $(this).attr('for');
            $form.find('#' + forAttr).attr('id', id + '_' + forAttr);
            $(this).attr('for', id + '_' + forAttr)
        });

        $form.find('select, input').change(function() {
            cloudStack.evenOdd($form, '.field:visible', {
                even: function($row) {
                    $row.removeClass('odd');
                },
                odd: function($row) {
                    $row.addClass('odd');
                }
            });
        });

        return $form;
    };

    cloudStack.uiCustom.routingPolicyTermWizard = function(args) {
        return function() {
            var $wizard = $('#template').find('div.routing-policy-term-wizard').clone();
            var $progress = $wizard.find('div.progress ul li');
            var $steps = $wizard.find('div.steps').children().hide().filter(':not(.disabled)');

            $wizard.data('startfn', null);

            // Close wizard
            var close = function() {
                $wizard.dialog('destroy');
                $('div.overlay').fadeOut(function() {
                    $('div.overlay').remove();
                });
            };

            // Go to specified step in wizard,
            // updating nav items and diagram
            var showStep = function(index, goBack, options) {
                if (!options) options = {};

                if (typeof index == 'string') {
                    index = $wizard.find('[routing-policy-term-wizard-step-id=' + index + ']').index() + 1;
                }

                var targetIndex = index - 1;

                if (index <= 1) targetIndex = 0;
                if (targetIndex == $steps.length) {
                    completeAction();
                }

                $steps.hide();
                $wizard.find('.buttons').show();

                var $targetStep = $($steps[targetIndex]).show();
                var $uiCustom = $targetStep.find('[ui-custom]');
                var formState = getData($wizard, {
                    all: true
                });
                var groupedFormState = getData($wizard);
                var formID = $targetStep.attr('routing-policy-term-wizard-form');
                var stepPreFilter = cloudStack.routingPolicyTermWizard.preFilters[
                    $targetStep.attr('routing-policy-term-wizard-prefilter')
                    ];

                // Bypass step check
                if (stepPreFilter && !stepPreFilter({
                    data: formState,
                    groupedData: groupedFormState
                })) {
                    return showStep(!goBack ? index + 1 : index - 1,
                        goBack
                    );
                }

                if (formID) {
                    if (!$targetStep.find('form').length) {
                        makeForm(args, formID, formState).appendTo($targetStep.find('.content.input-area .select-container'));

                        setTimeout(function() {
                            cloudStack.evenOdd($targetStep, '.field:visible', {
                                even: function() {},
                                odd: function($row) {
                                    $row.addClass('odd');
                                }
                            });
                        }, 50);
                    } else {
                        // Always re-activate selects
                        $targetStep.find('form select').each(function() {
                            var $select = $(this);
                            var selectFn = $select.data('dialog-select-fn');
                            var originalVal = $select.val();

                            if (selectFn) {
                                $select.html('');
                                selectFn({
                                    context: {
                                        terms: [formState]
                                    }
                                });
                                $select.val(originalVal);
                                $select.trigger('change');
                            }
                        });
                    }

                    if (args.forms[formID].preFilter) {
                        var preFilter = args.forms[formID].preFilter({
                            $form: $targetStep.find('form'),
                            data: formState
                        });
                    }
                }

                if ($uiCustom.length) {
                    $uiCustom.each(function() {
                        var $item = $(this);
                        var id = $item.attr('ui-custom');

                        $item.replaceWith(
                            args.customUI[id]({
                                data: formState,
                                context: cloudStack.context
                            })
                        )
                    });
                }

                if (!targetIndex) {
                    $wizard.find('.button.previous').hide();
                } else {
                    $wizard.find('.button.previous').show();
                }

                var $nextButton = $wizard.find('.button.next');
                $nextButton.find('span').html(_l('label.next'));
                $nextButton.removeClass('final post-launch');

                // Show launch button if last step
                if ($targetStep.index() == $steps.length - 1 || options.nextStep) {
                    $nextButton.find('span').html(options.nextStep ? _l('label.save.changes') : _l('label.tungsten.add.routing.policy.term'));
                    $nextButton.addClass('final');

                    if (options.nextStep) {
                        $nextButton.addClass('post-launch');
                    }
                }

                // Update progress bar
                var $targetProgress = $progress.removeClass('active').filter(function() {
                    return $(this).index() <= targetIndex;
                }).toggleClass('active');

                setTimeout(function() {
                    if (!$targetStep.find('input[type=radio]:checked').length) {
                        $targetStep.find('input[type=radio]:first').click();
                    }
                }, 50);

                $targetStep.find('form').validate();
            };

            var buildTermsList = function () {
                var fromTerms = getData($wizard, {
                    all: true
                });
                var prefixList = getData($wizard);
                var terms = [];
                var term = [];
                term.push({
                    name: "community",
                    value: fromTerms.community
                });
                term.push({
                    name: "matchAll",
                    value: fromTerms.matchAllCommunities
                });
                term.push({
                    name: "protocol",
                    value: fromTerms.protocol
                });
                prefixList.routingPolicyFromTerm.forEach(function (item) {
                    term.push({
                        name: "fromTermPrefix",
                        value: item
                    })
                });
                prefixList.routingPolicyThenTerm.forEach(function (item) {
                    term.push({
                        name: "thenTermItem",
                        value: item
                    })
                });
                terms.push({
                    terms: term
                });
                return terms;
            };

            var addRoutingPolicy = function (args, terms) {
                var prefixList = getData($wizard);
                var tungstenProviders = [];
                $.ajax({
                    url: createURL('listTungstenFabricProviders'),
                    async: false,
                    success: function(json) {
                        var items = json.listtungstenfabricprovidersresponse.tungstenProvider ? json.listtungstenfabricprovidersresponse.tungstenProvider : [];
                        tungstenProviders = items;
                    }
                });
                addRoutingPolicyTerm(args.routingPolicyUuid, args.zoneId, terms);
            };

            var buildPrefix = function (prefixResponse, prefix, prefixType) {
                if(prefixResponse.length > 0) {
                    prefixResponse = prefixResponse + ","
                }
                return prefixResponse + prefix + "&" + prefixType;
            };

            var buildTerm = function (termResponse, termAction, termType, termValue) {
                if(termResponse.length > 0) {
                    termResponse = termResponse + ","
                }
                if(termAction.length == 0){
                    termAction = " ";
                }
                if(termValue.length == 0){
                    termValue = " ";
                }
                return termResponse + termAction + "&" + termType + "&" + termValue;
            };

            var addRoutingPolicyTerm = function (routingPolicyUuid, zoneid, terms) {
                for ( var i = 0; i < terms.length; i++) {
                    var communities = [];
                    var matchAll = undefined;
                    var protocol = [];
                    var prefixes = [];
                    var prefixRespone = "";
                    var thenTermsList = [];
                    var termResponse = "";
                    var term = terms[i];
                    for ( var j = 0; j < terms[i].terms.length; j++) {
                        var termItem = term.terms[j];
                        if(termItem.name == "community") {
                            communities = termItem.value.split(",");
                        }
                        if(termItem.name == "matchAll") {
                            if(termItem.value == "on"){
                                matchAll = true;
                            } else {
                                matchAll = false;
                            }
                        }
                        if(termItem.name == "protocol") {
                            protocol = termItem.value.split(",");
                        }
                        if(termItem.name == "fromTermPrefix") {
                            prefixRespone = buildPrefix(prefixRespone, termItem.value.prefix, termItem.value.prefixType)
                        }
                        if(termItem.name == "thenTermItem") {
                            termResponse = buildTerm(termResponse, termItem.value.termAction, termItem.value.termType, termItem.value.termValue)
                        }
                        prefixes = prefixRespone.split(',');
                        thenTermsList = termResponse.split(",");
                    }
                    var dataObj = {
                        zoneid: zoneid,
                        tungstenroutingpolicyuuid: routingPolicyUuid,
                        tungstenroutingpolicyfromtermcommunities: communities.join(),
                        tungstenroutingpolicymatchall: matchAll,
                        tungstenroutingpolicyprotocol: protocol.join(),
                        tungstenroutingpolicyfromtermprefixlist: prefixes.join(),
                        tungstenroutingpolicythentermlist: thenTermsList.join()
                    };
                    $.ajax({
                        url: createURL('addTungstenFabricRoutingPolicyTerm'),
                        dataType: "json",
                        data: dataObj,
                        async: false,
                        success: function (json) {
                        }
                    });
                }
            };

            $wizard.click(function(event) {
                var $target = $(event.target);
                // Next button
                if ($target.closest('div.button.next').length) {
                    var $step = $steps.filter(':visible');
                    // Validation
                    var $form = $('form:visible').filter(function() {
                        // Don't include multi-edit (validation happens separately)
                        return !$(this).closest('.multi-edit').length;
                    });

                    if (!$target.closest('.button.next.final').length)
                        showStep($steps.filter(':visible').index() + 2);
                    else {
                        if ($target.closest('.button.next.final.post-launch').length) {
                            showStep('launch');
                        }
                        var terms = buildTermsList();
                        addRoutingPolicy(args, terms);
                        close();
                        $(window).trigger('cloudStack.fullRefresh');
                    }

                    return false;
                }

                // Previous button
                if ($target.closest('div.button.previous').length) {
                    showStep($steps.filter(':visible').index(), true);

                    return false;
                }

                // Close button
                if ($target.closest('div.button.cancel').length) {
                    close();

                    return false;
                }

                // Edit link
                if ($target.closest('div.edit').length) {
                    var $edit = $target.closest('div.edit');

                    showStep($edit.find('a').attr('href'));

                    return false;
                }

                return true;
            });


            showStep(1);

            var $dialog = $wizard.dialog({
                title: _l('label.installWizard.createRoutingPolicy.title'),
                closeOnEscape: false,
                width: 750,
                height: 665,
                resizable: false
            });

            return cloudStack.applyDefaultZindexAndOverlayOnJqueryDialogAndRemoveCloseButton($dialog);
        };
    };
})(jQuery, cloudStack);
